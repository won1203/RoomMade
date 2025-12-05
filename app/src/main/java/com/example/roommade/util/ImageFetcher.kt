package com.example.roommade.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class ImageFetcher(
    private val httpClient: OkHttpClient = defaultClient()
) {
    suspend fun fetchBitmap(url: String): Bitmap {
        val normalized = url.trim()
        if (normalized.startsWith("data:", ignoreCase = true)) {
            return decodeDataUri(normalized)
        }

        val request = Request.Builder().url(normalized).build()
        val bytes = withContext(Dispatchers.IO) {
            httpClient.newCall(request).execute()
        }.use { response ->
            if (!response.isSuccessful) {
                throw IOException("이미지 요청 실패 (${response.code})")
            }
            response.body?.bytes() ?: throw IOException("이미지 응답이 비어 있습니다.")
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: throw IOException("이미지를 비트맵으로 변환할 수 없습니다.")
    }

    private fun decodeDataUri(dataUri: String): Bitmap {
        val commaIndex = dataUri.indexOf(',')
        if (commaIndex <= 0) throw IOException("data URI 형식이 올바르지 않습니다.")
        val base64 = dataUri.substring(commaIndex + 1)
        val bytes = try {
            Base64.decode(base64, Base64.DEFAULT)
        } catch (t: Throwable) {
            throw IOException("data URI를 디코딩할 수 없습니다.", t)
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: throw IOException("data URI를 비트맵으로 변환할 수 없습니다.")
    }

    companion object {
        private fun defaultClient(): OkHttpClient =
            OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()
    }
}
