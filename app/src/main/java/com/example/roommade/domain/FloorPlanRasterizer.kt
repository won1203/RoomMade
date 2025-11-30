package com.example.roommade.domain

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.example.roommade.model.FloorPlan
import com.example.roommade.model.FurnCategory

/**
 * 단일 책임: FloorPlan -> ControlNet-Seg 세그멘테이션 비트맵 변환.
 * 팔레트는 서버와 반드시 동일하게 유지해야 한다.
 */
object FloorPlanRasterizer {
    private const val DEFAULT_SIZE = 768
    private const val BACKGROUND_COLOR = Color.BLACK
    private const val WALL_COLOR = 0xFF1A1A1A.toInt()
    private const val WINDOW_COLOR = 0xFF00FFFF.toInt()
    private const val DOOR_COLOR = 0xFFFF00FF.toInt()

    // ControlNet-Seg 클래스 팔레트 (서버와 동기화 필수)
    private val categoryColors: Map<FurnCategory, Int> = mapOf(
        FurnCategory.BED to 0xFF1E90FF.toInt(),
        FurnCategory.DESK to 0xFF32CD32.toInt(),
        FurnCategory.SOFA to 0xFFFFA500.toInt(),
        FurnCategory.WARDROBE to 0xFF8A2BE2.toInt(),
        FurnCategory.TABLE to 0xFF32CD32.toInt(),
        FurnCategory.CHAIR to 0xFF6495ED.toInt(),
        FurnCategory.LIGHTING to 0xFFFFFF00.toInt(),
        FurnCategory.RUG to 0xFFCD5C5C.toInt(),
        FurnCategory.OTHER to 0xFF808080.toInt()
    )

    fun toSegBitmap(plan: FloorPlan, targetSize: Int = DEFAULT_SIZE): Bitmap {
        val bounds = plan.bounds
        val widthPx = bounds.width().coerceAtLeast(1f)
        val heightPx = bounds.height().coerceAtLeast(1f)
        val aspect = widthPx / heightPx

        val outWidth = if (aspect >= 1f) targetSize else (targetSize * aspect).toInt().coerceAtLeast(1)
        val outHeight = if (aspect >= 1f) (targetSize / aspect).toInt().coerceAtLeast(1) else targetSize

        val bitmap = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(BACKGROUND_COLOR)

        val scaleX = outWidth / widthPx
        val scaleY = outHeight / heightPx
        fun RectF.scaled(): RectF = RectF(left * scaleX, top * scaleY, right * scaleX, bottom * scaleY)

        val wallPaint = Paint().apply {
            color = WALL_COLOR
            style = Paint.Style.STROKE
            strokeWidth = 10f
            isAntiAlias = false
        }
        val strokePaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 14f
            isAntiAlias = false
        }
        val fillPaint = Paint().apply {
            style = Paint.Style.FILL
            isAntiAlias = false
        }

        // 외곽 벽
        canvas.drawRect(bounds.scaled(), wallPaint)

        // 창문
        strokePaint.color = WINDOW_COLOR
        plan.windows.forEach { window ->
            canvas.drawRect(window.rect.scaled(), strokePaint)
        }

        // 문
        strokePaint.color = DOOR_COLOR
        plan.doors.forEach { door ->
            canvas.drawRect(door.rect.scaled(), strokePaint)
        }

        // 가구
        plan.furnitures.forEach { furniture ->
            fillPaint.color = categoryColors[furniture.category] ?: categoryColors[FurnCategory.OTHER]!!
            canvas.drawRect(furniture.rect.scaled(), fillPaint)
        }

        return bitmap
    }
}
