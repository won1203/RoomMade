package com.example.roommade.network

import com.example.roommade.BuildConfig
import java.io.IOException
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class ReplicateClient(
    private val httpClient: OkHttpClient = defaultClient()
) {
    suspend fun generate(
        prompt: String,
        controlImage: String,
        steps: Int = 20,
        guidanceScale: Double = 7.5
    ): String {
        requireConfig()
        val version = BuildConfig.REPLICATE_CONTROLNET_VERSION
        val createBody = JSONObject().apply {
            put("version", version)
            put(
                "input",
                JSONObject().apply {
                    put("prompt", prompt)
                    put("control_image", controlImage)
                    put("num_inference_steps", steps)
                    put("guidance_scale", guidanceScale)
                }
            )
        }

        val created = createPrediction(createBody)
        var current = created
        repeat(MAX_POLL) {
            if (current.status == "succeeded") return@repeat
            if (current.status == "failed" || current.status == "canceled") {
                throw IllegalStateException(current.error ?: "Replicate 요청이 실패했습니다: ${current.status}")
            }
            delay(POLL_DELAY.inWholeMilliseconds)
            current = fetchPrediction(current.id)
        }

        if (current.status != "succeeded") {
            throw IllegalStateException("AI 생성이 완료되지 않았습니다: ${current.status}")
        }

        val url = current.output.firstOrNull()
            ?: throw IllegalStateException(current.error ?: "생성된 이미지 URL이 비어 있습니다.")
        return url
    }

    private suspend fun createPrediction(body: JSONObject): PredictionResponse {
        val mediaType = "application/json".toMediaType()
        val request = Request.Builder()
            .url("$BASE_URL/predictions")
            .post(body.toString().toRequestBody(mediaType))
            .build()
        val json = requestJson(request)
        return parsePrediction(json)
    }

    private suspend fun fetchPrediction(id: String): PredictionResponse {
        val request = Request.Builder()
            .url("$BASE_URL/predictions/$id")
            .get()
            .build()
        val json = requestJson(request)
        return parsePrediction(json)
    }

    private suspend fun requestJson(request: Request): JSONObject {
        val responseBody = withContext(Dispatchers.IO) {
            httpClient.newCall(request).execute()
        }.use { response ->
            val body = response.body?.string()
            if (!response.isSuccessful || body == null) {
                throw IOException("Replicate API 호출 실패 (${response.code}): ${response.message}")
            }
            body
        }
        return JSONObject(responseBody)
    }

    private fun parsePrediction(json: JSONObject): PredictionResponse {
        val output = when (val value = json.opt("output")) {
            is JSONArray -> (0 until value.length()).mapNotNull { idx -> value.optString(idx) }
            is String -> listOf(value)
            else -> emptyList()
        }
        return PredictionResponse(
            id = json.getString("id"),
            status = json.getString("status"),
            output = output,
            error = json.optString("error").takeIf { it.isNotBlank() }
        )
    }

    private fun requireConfig() {
        if (BuildConfig.REPLICATE_API_KEY.isBlank()) {
            throw IllegalStateException("REPLICATE_API_KEY가 설정되지 않았습니다. local.properties 또는 환경 변수에 키를 추가하세요.")
        }
        if (BuildConfig.REPLICATE_CONTROLNET_VERSION.isBlank()) {
            throw IllegalStateException("REPLICATE_CONTROLNET_VERSION이 설정되지 않았습니다. Replicate 버전 ID를 설정하세요.")
        }
    }

    data class PredictionResponse(
        val id: String,
        val status: String,
        val output: List<String>,
        val error: String?
    )

    companion object {
        private const val BASE_URL = "https://api.replicate.com/v1"
        private val POLL_DELAY = 1.5.seconds
        private const val MAX_POLL = 30

        private fun defaultClient(): OkHttpClient =
            OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .addHeader("Authorization", "Token ${BuildConfig.REPLICATE_API_KEY}")
                        .addHeader("Content-Type", "application/json")
                        .build()
                    chain.proceed(request)
                }
                .build()
    }
}
