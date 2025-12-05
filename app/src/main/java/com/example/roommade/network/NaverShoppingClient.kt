package com.example.roommade.network

import androidx.core.text.HtmlCompat
import com.example.roommade.BuildConfig
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

data class NaverShoppingItem(
    val id: String,
    val title: String,
    val mallName: String?,
    val price: Int,
    val link: String,
    val imageUrl: String?
)

/**
    * 네이버 쇼핑 검색을 백엔드(Firebase Functions/Run) 프록시로 호출하여
    * 클라이언트에 ClientId/Secret을 보관하지 않는다.
    */
class NaverShoppingClient(
    private val endpoint: String = BuildConfig.NAVER_SHOPPING_FUNCTION_URL,
    private val httpClient: OkHttpClient = defaultClient()
) {

    suspend fun search(query: String, display: Int = 30): List<NaverShoppingItem> = withContext(Dispatchers.IO) {
        if (endpoint.isBlank()) {
            error("네이버 쇼핑 백엔드 URL이 설정되지 않았습니다. local.properties에 NAVER_SHOPPING_FUNCTION_URL을 추가하세요.")
        }
        val sanitized = query.trim().ifBlank {
            error("검색어가 비어 있습니다.")
        }
        val body = JSONObject().apply {
            put("query", sanitized)
            put("display", display.coerceIn(1, 100))
        }

        val request = Request.Builder()
            .url(endpoint)
            .post(body.toString().toRequestBody(JSON))
            .addHeader("Content-Type", "application/json")
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("네이버 쇼핑 백엔드 호출 실패: HTTP ${response.code}")
            }
            val bodyStr = response.body?.string().orEmpty()
            if (bodyStr.isBlank()) error("응답 본문이 비어 있습니다.")
            parseItems(JSONObject(bodyStr))
        }
    }

    private fun parseItems(json: JSONObject): List<NaverShoppingItem> {
        val items = json.optJSONArray("items")
            ?: json.optJSONObject("data")?.optJSONArray("items")
            ?: JSONArray()
        if (items.length() == 0) return emptyList()
        val results = mutableListOf<NaverShoppingItem>()
        for (index in 0 until items.length()) {
            val obj = items.optJSONObject(index) ?: continue
            val rawTitle = obj.optString("title")
            val decodedTitle = HtmlCompat.fromHtml(rawTitle, HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
            val price = obj.optString("lprice").toIntOrNull() ?: 0
            results += NaverShoppingItem(
                id = obj.optString("productId", obj.optString("productNo", index.toString())),
                title = decodedTitle.ifBlank { obj.optString("brand").ifBlank { "이름 미상" } },
                mallName = obj.optString("mallName").ifBlank { null },
                price = price,
                link = obj.optString("link"),
                imageUrl = obj.optString("image").ifBlank { null }
            )
        }
        return results
    }

    fun hasCredentials(): Boolean = endpoint.isNotBlank()

    companion object {
        private val JSON = "application/json".toMediaType()
        private val TIMEOUT = 30.seconds

        private fun defaultClient(): OkHttpClient =
            OkHttpClient.Builder()
                .callTimeout(TIMEOUT.toJavaDuration())
                .build()
    }
}
