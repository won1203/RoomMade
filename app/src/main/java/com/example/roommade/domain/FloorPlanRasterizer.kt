package com.example.roommade.domain

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.example.roommade.model.FloorPlan
import com.example.roommade.model.FurnCategory
import com.example.roommade.model.enLabel
import kotlin.math.max

object FloorPlanRasterizer {
    private const val DEFAULT_SIZE = 768

    // Scribble (ControlNet-Scribble) palette
    private const val SCRIBBLE_BG = Color.WHITE
    private const val SCRIBBLE_STROKE = Color.BLACK
    private const val SCRIBBLE_STROKE_WIDTH = 6f
    private const val SCRIBBLE_TEXT_SIZE = 28f

    /**
     * FloorPlan -> scribble bitmap (white background + black outlines) for ControlNet-Scribble.
     */
    fun toScribbleBitmap(plan: FloorPlan, targetSize: Int = DEFAULT_SIZE): Bitmap {
        val ctx = rasterContext(plan, targetSize)
        val bitmap = ctx.bitmap
        val canvas = Canvas(bitmap).apply { drawColor(SCRIBBLE_BG) }
        val strokePaint = Paint().apply {
            color = SCRIBBLE_STROKE
            style = Paint.Style.STROKE
            strokeWidth = SCRIBBLE_STROKE_WIDTH
            isAntiAlias = false
        }
        val textPaint = Paint().apply {
            color = SCRIBBLE_STROKE
            textSize = SCRIBBLE_TEXT_SIZE
            textAlign = Paint.Align.CENTER
            isAntiAlias = false
        }
        val counts = mutableMapOf<FurnCategory, Int>()

        // Outer boundary
        canvas.drawRect(ctx.scaled(plan.bounds), strokePaint)

        // Furniture outlines with labels to preserve semantics
        plan.furnitures.forEach { furniture ->
            val rect = ctx.scaled(furniture.rect)
            canvas.drawRect(rect, strokePaint)

            val labelIndex = (counts[furniture.category] ?: 0) + 1
            counts[furniture.category] = labelIndex
            val base = furniture.category.enLabel().singular
            val label = if (labelIndex > 1) "$base $labelIndex" else base
            val baselineY = rect.centerY() - ((textPaint.descent() + textPaint.ascent()) / 2f)
            canvas.drawText(label, rect.centerX(), baselineY, textPaint)
        }

        return bitmap
    }

    private data class RasterContext(
        val bitmap: Bitmap,
        val scaled: (RectF) -> RectF
    )

    private fun rasterContext(plan: FloorPlan, targetSize: Int): RasterContext {
        val bounds = plan.bounds
        val widthPx = max(bounds.width(), 1f)
        val heightPx = max(bounds.height(), 1f)
        val aspect = widthPx / heightPx

        val outWidth = if (aspect >= 1f) targetSize else (targetSize * aspect).toInt().coerceAtLeast(1)
        val outHeight = if (aspect >= 1f) (targetSize / aspect).toInt().coerceAtLeast(1) else targetSize

        val bitmap = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
        val scaleX = outWidth / widthPx
        val scaleY = outHeight / heightPx
        val scaler: (RectF) -> RectF = { rect ->
            RectF(
                rect.left * scaleX,
                rect.top * scaleY,
                rect.right * scaleX,
                rect.bottom * scaleY
            )
        }
        return RasterContext(bitmap, scaler)
    }
}
