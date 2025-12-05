package com.example.roommade.network

import com.example.roommade.BuildConfig
import java.io.IOException
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * Firebase(Cloud Functions/Run) 백엔드로 AI 이미지 생성을 위임하는 클라이언트.
 * - 안드로이드에 Vertex/모델 키를 저장하지 않고, 백엔드를 통해 호출한다.
 * - 백엔드 엔드포인트는 BuildConfig.IMAGE_GEN_FUNCTION_URL 로 주입.
 */
class FirebaseImageGenClient(
    private val httpClient: OkHttpClient = defaultClient()
) {
    suspend fun generateImage(
        prompt: String,
        negativePrompt: String?,
        baseImageDataUri: String?
    ): String {
        val endpoint = BuildConfig.IMAGE_GEN_FUNCTION_URL
        require(endpoint.isNotBlank()) {
            "IMAGE_GEN_FUNCTION_URL이 설정되지 않았습니다. local.properties나 환경 변수에 추가하세요."
        }

        val body = buildRequest(prompt, negativePrompt, baseImageDataUri)
        val request = Request.Builder()
            .url(endpoint)
            .post(body.toString().toRequestBody(JSON))
            .addHeader("Content-Type", "application/json")
            .build()

        val responseJson = requestJson(request)
        val imageUrl = responseJson.optString("imageUrl").ifBlank {
            responseJson.optString("image_url").ifBlank {
                responseJson.optJSONObject("data")?.optString("imageUrl").orEmpty()
            }
        }
        if (imageUrl.isBlank()) {
            throw IllegalStateException("백엔드 응답에 imageUrl이 없습니다.")
        }
        return imageUrl
    }

    private fun buildRequest(
        prompt: String,
        negativePrompt: String?,
        baseImageDataUri: String?
    ): JSONObject {
        return JSONObject().apply {
            put("prompt", prompt)
            negativePrompt?.takeIf { it.isNotBlank() }?.let { put("negative_prompt", it) }
            baseImageDataUri?.takeIf { it.isNotBlank() }?.let { put("base_image", it) }
        }
    }

    private suspend fun requestJson(request: Request): JSONObject {
        val response = withContext(Dispatchers.IO) { httpClient.newCall(request).execute() }
        val bodyStr = response.use { it.body?.string() }
        if (!response.isSuccessful || bodyStr == null) {
            throw IOException("이미지 백엔드 요청 실패 (${response.code}): ${response.message} / ${bodyStr ?: "empty body"}")
        }
        val json = JSONObject(bodyStr)
        val error = json.optString("error").takeIf { it.isNotBlank() }
            ?: json.optJSONObject("error")?.optString("message")?.takeIf { it.isNotBlank() }
        if (error != null) {
            throw IllegalStateException("이미지 백엔드 오류: $error")
        }
        return json
    }

    companion object {
        private val JSON = "application/json".toMediaType()
        private val TIMEOUT = 120.seconds

        private fun defaultClient(): OkHttpClient =
            OkHttpClient.Builder()
                .connectTimeout(TIMEOUT.toJavaDuration())
                .readTimeout(TIMEOUT.toJavaDuration())
                .writeTimeout(TIMEOUT.toJavaDuration())
                .callTimeout(TIMEOUT.toJavaDuration())
                .build()
    }
}
