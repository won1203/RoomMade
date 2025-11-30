package com.example.roommade.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ImageSaver {
    suspend fun savePngToGallery(
        context: Context,
        bitmap: android.graphics.Bitmap,
        displayName: String
    ): Uri {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/RoomMade")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw IOException("MediaStore에 이미지를 생성하지 못했습니다.")
        try {
            resolver.openOutputStream(uri)?.use { stream ->
                val ok = withContext(Dispatchers.IO) {
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
                }
                if (!ok) throw IOException("이미지 압축에 실패했습니다.")
            } ?: throw IOException("이미지 스트림을 열지 못했습니다.")
        } catch (t: Throwable) {
            resolver.delete(uri, null, null)
            throw t
        } finally {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val pendingClear = ContentValues().apply {
                    put(MediaStore.Images.Media.IS_PENDING, 0)
                }
                resolver.update(uri, pendingClear, null, null)
            }
        }
        return uri
    }
}

class ImageSharer {
    fun shareImage(context: Context, uri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "이미지 공유")
        // Activity context 필요: Compose 호스트에서 호출되므로 new task 플래그는 불필요.
        context.startActivity(chooser)
    }
}
