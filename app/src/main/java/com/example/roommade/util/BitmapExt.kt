package com.example.roommade.util

import android.graphics.Bitmap
import android.util.Base64
import java.io.ByteArrayOutputStream

fun Bitmap.toBase64PngDataUri(): String {
    val buffer = ByteArrayOutputStream()
    val ok = this.compress(Bitmap.CompressFormat.PNG, 100, buffer)
    if (!ok) throw IllegalStateException("비트맵을 PNG로 변환하지 못했습니다.")
    val encoded = Base64.encodeToString(buffer.toByteArray(), Base64.NO_WRAP)
    return "data:image/png;base64,$encoded"
}
