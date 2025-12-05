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
import org.json.JSONArray
import org.json.JSONObject

class VertexAiClient(
    private val httpClient: OkHttpClient = defaultClient()
) {
    suspend fun generateImage(
        prompt: String,
        negativePrompt: String?,
        baseImageDataUri: String? = null
    ): String {
        ensureConfig()
        val requestBody = buildRequestBody(prompt, negativePrompt, baseImageDataUri)
        val request = Request.Builder()
            .url(generateUrl())
            .addHeader("Content-Type", "application/json")
            .addHeader("x-goog-api-key", BuildConfig.VERTEX_API_KEY)
            .post(requestBody.toString().toRequestBody(JSON.toMediaType()))
            .build()

        val responseJson = requestJson(request)
        val inlineData = extractImageInlineData(responseJson)
        return "data:${inlineData.mimeType};base64,${inlineData.data}"
    }

    private fun ensureConfig() {
        if (BuildConfig.VERTEX_API_KEY.isBlank()) {
            throw IllegalStateException("VERTEX_API_KEY가 설정되지 않았습니다. local.properties 또는 환경 변수에 추가하세요.")
        }
        if (BuildConfig.VERTEX_PROJECT_ID.isBlank()) {
            throw IllegalStateException("VERTEX_PROJECT_ID가 설정되지 않았습니다.")
        }
        if (BuildConfig.VERTEX_LOCATION.isBlank()) {
            throw IllegalStateException("VERTEX_LOCATION이 설정되지 않았습니다.")
        }
        if (BuildConfig.VERTEX_GEMINI_IMAGE_MODEL.isBlank()) {
            throw IllegalStateException("VERTEX_GEMINI_IMAGE_MODEL이 설정되지 않았습니다.")
        }
    }

    private fun buildRequestBody(
        prompt: String,
        negativePrompt: String?,
        baseImageDataUri: String?
    ): JSONObject {
        val combinedPrompt = if (negativePrompt.isNullOrBlank()) {
            prompt
        } else {
            "$prompt\n\nAvoid: $negativePrompt"
        }

        val parts = JSONArray().apply {
            put(JSONObject().put("text", combinedPrompt))
            baseImageDataUri
                ?.takeIf { it.isNotBlank() }
                ?.let { put(JSONObject().put("inline_data", parseDataUri(it))) }
        }

        val safety = SAFETY_SETTINGS.fold(JSONArray()) { acc, setting ->
            acc.put(setting)
        }

        return JSONObject().apply {
            put(
                "contents",
                JSONArray().put(
                    JSONObject()
                        .put("role", "user")
                        .put("parts", parts)
                )
            )
            put(
                "generation_config",
                JSONObject()
                    .put("response_mime_type", "image/png")
                    .put("temperature", 0.8)
            )
            put("safety_settings", safety)
        }
    }

    private fun parseDataUri(dataUri: String): JSONObject {
        val commaIndex = dataUri.indexOf(',')
        if (commaIndex <= 0) {
            throw IllegalArgumentException("base image는 data URI 형태여야 합니다.")
        }
        val header = dataUri.substring(0, commaIndex)
        val data = dataUri.substring(commaIndex + 1)
        val mimeType = header.substringAfter("data:", missingDelimiterValue = "")
            .substringBefore(';')
            .ifBlank { "image/png" }
        return JSONObject()
            .put("mime_type", mimeType)
            .put("data", data)
    }

    private suspend fun requestJson(request: Request): JSONObject {
        val response = withContext(Dispatchers.IO) { httpClient.newCall(request).execute() }
        val bodyStr = response.use { it.body?.string() }
        if (!response.isSuccessful || bodyStr == null) {
            throw IOException("Vertex AI 요청 실패 (${response.code}): ${response.message} / ${bodyStr ?: "empty body"}")
        }
        val root = JSONObject(bodyStr)
        val error = root.optJSONObject("error")?.optString("message")?.takeIf { it.isNotBlank() }
        if (error != null) {
            throw IllegalStateException("Vertex AI 오류: $error")
        }
        return root
    }

    private fun extractImageInlineData(json: JSONObject): InlineImageData {
        val candidates = json.optJSONArray("candidates") ?: JSONArray()
        for (i in 0 until candidates.length()) {
            val candidate = candidates.optJSONObject(i) ?: continue
            val content = candidate.optJSONObject("content") ?: continue
            val parts = content.optJSONArray("parts") ?: continue
            for (j in 0 until parts.length()) {
                val part = parts.optJSONObject(j) ?: continue
                val inline = part.optJSONObject("inline_data")
                val mime = inline?.optString("mime_type").orEmpty()
                val data = inline?.optString("data").orEmpty()
                if (mime.isNotBlank() && data.isNotBlank()) {
                    return InlineImageData(mime, data)
                }
            }
        }
        throw IllegalStateException("Vertex AI가 이미지 응답을 반환하지 않았습니다.")
    }

    private fun generateUrl(): String {
        val base =
            "https://${BuildConfig.VERTEX_LOCATION}-aiplatform.googleapis.com/v1beta1/projects/${BuildConfig.VERTEX_PROJECT_ID}/locations/${BuildConfig.VERTEX_LOCATION}/publishers/google/models/${BuildConfig.VERTEX_GEMINI_IMAGE_MODEL}"
        return "$base:generateContent?key=${BuildConfig.VERTEX_API_KEY}"
    }

    data class InlineImageData(
        val mimeType: String,
        val data: String
    )

    companion object {
        private const val JSON = "application/json"
        private val SAFETY_SETTINGS = listOf(
            safetySetting("HARM_CATEGORY_HATE_SPEECH"),
            safetySetting("HARM_CATEGORY_DANGEROUS_CONTENT"),
            safetySetting("HARM_CATEGORY_SEXUALLY_EXPLICIT"),
            safetySetting("HARM_CATEGORY_HARASSMENT")
        )
        private val TIMEOUT = 120.seconds

        private fun safetySetting(category: String): JSONObject =
            JSONObject()
                .put("category", category)
                .put("threshold", "BLOCK_MEDIUM_AND_ABOVE")

        private fun defaultClient(): OkHttpClient =
            OkHttpClient.Builder()
                .callTimeout(TIMEOUT.toJavaDuration())
                .build()
    }
}
