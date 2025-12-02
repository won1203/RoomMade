package com.example.roommade.util

import android.graphics.Bitmap
import android.util.Base64
import java.io.ByteArrayOutputStream
import kotlin.math.max

private const val MAX_BASE64_LONG_EDGE = 1280

fun Bitmap.toBase64PngDataUri(): String {
    // Downscale by long edge to avoid large Base64 allocations
    val scale = MAX_BASE64_LONG_EDGE.toFloat() / max(width, height).toFloat()
    val bitmapForEncode =
        if (scale < 1f) Bitmap.createScaledBitmap(this, (width * scale).toInt(), (height * scale).toInt(), true)
        else this

    val buffer = ByteArrayOutputStream()
    val ok = bitmapForEncode.compress(Bitmap.CompressFormat.PNG, 100, buffer)
    if (!ok) throw IllegalStateException("Failed to convert bitmap to PNG.")
    val encoded = Base64.encodeToString(buffer.toByteArray(), Base64.NO_WRAP)
    return "data:image/png;base64,$encoded"
}
