package com.example.roommade.network

import androidx.core.text.HtmlCompat
import com.example.roommade.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
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

class NaverShoppingClient(
    private val clientId: String = BuildConfig.NAVER_CLIENT_ID,
    private val clientSecret: String = BuildConfig.NAVER_CLIENT_SECRET,
    private val httpClient: OkHttpClient = OkHttpClient()
) {

    suspend fun search(query: String, display: Int = 30): List<NaverShoppingItem> = withContext(Dispatchers.IO) {
        if (clientId.isBlank() || clientSecret.isBlank()) {
            error("네이버 쇼핑 API 키가 설정되지 않았습니다. local.properties 또는 gradle.properties에 NAVER_CLIENT_ID/NAVER_CLIENT_SECRET 값을 입력하세요.")
        }
        val sanitized = query.trim().ifBlank {
            error("검색어가 비어 있습니다.")
        }
        val url = HttpUrl.Builder()
            .scheme("https")
            .host("openapi.naver.com")
            .addPathSegments("v1/search/shop.json")
            .addQueryParameter("query", sanitized)
            .addQueryParameter("display", display.coerceIn(1, 100).toString())
            .addQueryParameter("sort", "sim")
            .build()
        val request = Request.Builder()
            .url(url)
            .get()
            .addHeader("X-Naver-Client-Id", clientId)
            .addHeader("X-Naver-Client-Secret", clientSecret)
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("네이버 쇼핑 API 오류: HTTP ${response.code}")
            }
            val body = response.body?.string().orEmpty()
            parseItems(JSONObject(body))
        }
    }

    private fun parseItems(json: JSONObject): List<NaverShoppingItem> {
        val items = json.optJSONArray("items") ?: JSONArray()
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

    fun hasCredentials(): Boolean = clientId.isNotBlank() && clientSecret.isNotBlank()
}
