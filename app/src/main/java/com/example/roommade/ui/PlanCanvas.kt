package com.example.roommade.ui

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.sp
import com.example.roommade.model.FloorPlan
import com.example.roommade.model.FurnCategory
import com.example.roommade.model.korLabel
import com.example.roommade.vm.FloorPlanViewModel
import kotlin.math.min

private sealed interface DragTarget {
    data class Furniture(val index: Int) : DragTarget
    data class Door(val index: Int) : DragTarget
    data class Window(val index: Int) : DragTarget
}

@Composable
fun PlanCanvasStatic(
    plan: FloorPlan,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.White,
    showLabels: Boolean = true,
    fitToHeight: Boolean = false,
    alignLeftInCanvas: Boolean = false,
    alignXFraction: Float = 0.5f,
    alignYFraction: Float = 0.5f,
) {
    val density = LocalDensity.current
    val labels = remember(plan.furnitures) { labelsForPlan(plan) }
    Canvas(modifier = modifier) {
        val info0 = computeScaleInfo(plan.bounds, size, preferHeight = fitToHeight)
        val width = plan.bounds.width().coerceAtLeast(1f)
        val height = plan.bounds.height().coerceAtLeast(1f)
        val scaledWidth = width * info0.scale
        val scaledHeight = height * info0.scale
        val fx = (if (alignLeftInCanvas) 0f else alignXFraction).coerceIn(0f, 1f)
        val fy = alignYFraction.coerceIn(0f, 1f)
        val finalOffset = Offset(
            x = (size.width - scaledWidth) * fx,
            y = (size.height - scaledHeight) * fy
        )
        val info = info0.copy(offset = finalOffset)
        withTransform({
            translate(info.offset.x, info.offset.y)
            scale(info.scale, info.scale)
            translate(-plan.bounds.left, -plan.bounds.top)
        } ) {
            // 정규화 이후에는 (0,0)~(width,height)로 클리핑
            clipRect(
                left = 0f,
                top = 0f,
                right = plan.bounds.width(),
                bottom = plan.bounds.height()
            ) {
                // 諛곌꼍? 諛??대?留??곗깋?쇰줈 梨꾩슫??(罹붾쾭???꾩껜 諛곌꼍? 移좏븯吏 ?딆쓬)
                drawPlan(plan, showLabels, labels, density)
            }
        }
    }
}

@Composable
fun PlanCanvasInteractive(
    vm: FloorPlanViewModel,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.White,
) {
    val density = LocalDensity.current
    val plan = vm.floorPlan
    var dragTarget by remember { mutableStateOf<DragTarget?>(null) }
    data class DragContext(val startPointer: Offset, val index: Int, val startLeft: Float, val startTop: Float)
    var furnitureDrag by remember { mutableStateOf<DragContext?>(null) }
    val planState by rememberUpdatedState(plan)
    // Ensure inventory is spawned if needed (mirrors EditFloorplanScreen behavior)
    LaunchedEffect(plan.furnitures.size, vm.inventory) {
        if (plan.furnitures.isEmpty() && vm.inventory.isNotEmpty()) {
            vm.spawnInventoryToPlan(resetPrevious = true)
        }
        dragTarget = null
        furnitureDrag = null
    }
    val labels = remember(plan.furnitures) { labelsForPlan(plan) }
    val widthPx = plan.bounds.width().coerceAtLeast(1f)
    val heightPx = plan.bounds.height().coerceAtLeast(1f)
    val widthDp = with(density) { widthPx.toDp() }
    val heightDp = with(density) { heightPx.toDp() }

    Canvas(
        modifier = modifier
            .requiredSize(widthDp, heightDp)
            .background(backgroundColor)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val target = hitTest(planState, offset)
                        dragTarget = target
                        if (target is DragTarget.Furniture) {
                            val rect = planState.furnitures.getOrNull(target.index)?.rect
                            if (rect != null) {
                                furnitureDrag = DragContext(
                                    startPointer = offset,
                                    index = target.index,
                                    startLeft = rect.left,
                                    startTop = rect.top
                                )
                            }
                        }
                    },
                    onDragEnd = { dragTarget = null; furnitureDrag = null },
                    onDragCancel = { dragTarget = null; furnitureDrag = null },
                    onDrag = { change, drag ->
                        change.consume()
                        if (dragTarget == null) {
                            val startPos = change.position - drag
                            val candidate = hitTest(planState, startPos) ?: hitTest(planState, change.position)
                            if (candidate is DragTarget.Furniture) {
                                val rect = planState.furnitures.getOrNull(candidate.index)?.rect
                                if (rect != null) {
                                    dragTarget = candidate
                                    furnitureDrag = DragContext(
                                        startPointer = startPos,
                                        index = candidate.index,
                                        startLeft = rect.left,
                                        startTop = rect.top
                                    )
                                }
                            } else if (candidate is DragTarget.Door || candidate is DragTarget.Window) {
                                dragTarget = candidate
                            }
                        }
                        when (val t = dragTarget) {
                            is DragTarget.Furniture -> {
                                val ctx = furnitureDrag
                                if (ctx != null) {
                                    val dxAbs = change.position.x - ctx.startPointer.x
                                    val dyAbs = change.position.y - ctx.startPointer.y
                                    val newLeft = ctx.startLeft + dxAbs
                                    val newTop = ctx.startTop + dyAbs
                                    vm.moveFurnitureTo(t.index, newLeft, newTop)
                                }
                            }
                            is DragTarget.Door -> vm.moveOpening(true, t.index, drag.x, drag.y)
                            is DragTarget.Window -> vm.moveOpening(false, t.index, drag.x, drag.y)
                            null -> Unit
                        }
                    }
                )
            }
     ) {
        clipRect(
            left = plan.bounds.left,
            top = plan.bounds.top,
            right = plan.bounds.right,
            bottom = plan.bounds.bottom
        ) {
            drawPlan(planState, true, labels, density)
        }
    }
}

private fun hitTest(fp: FloorPlan, p: Offset): DragTarget? {
    for (i in fp.furnitures.indices.reversed()) {
        if (fp.furnitures[i].rect.contains(p.x, p.y)) return DragTarget.Furniture(i)
    }
    return null
}

private data class ScaleInfo(val scale: Float, val offset: Offset)

private fun computeScaleInfo(
    bounds: android.graphics.RectF,
    canvasSize: Size,
    preferHeight: Boolean = false
): ScaleInfo {
    if (canvasSize.width <= 0f || canvasSize.height <= 0f) return ScaleInfo(1f, Offset.Zero)
    val width = bounds.width().coerceAtLeast(1f)
    val height = bounds.height().coerceAtLeast(1f)
    val scaleX = canvasSize.width / width
    val scaleY = canvasSize.height / height
    val scale = if (preferHeight) scaleY else min(scaleX, scaleY)
    val scaledWidth = width * scale
    val scaledHeight = height * scale
    val offsetX = (canvasSize.width - scaledWidth) / 2f
    val offsetY = (canvasSize.height - scaledHeight) / 2f
    return ScaleInfo(scale = scale, offset = Offset(offsetX, offsetY))
}

private fun DrawScope.drawPlan(
    plan: FloorPlan,
    showLabels: Boolean,
    labels: List<String>,
    density: androidx.compose.ui.unit.Density,
) {
    val roomWidth = plan.bounds.width()
    val roomHeight = plan.bounds.height()
    val roomLeft = plan.bounds.left
    val roomTop = plan.bounds.top
    // Fill room background
    drawRect(
        color = Color.White,
        topLeft = Offset(roomLeft, roomTop),
        size = Size(roomWidth, roomHeight)
    )
    // Room bounds (wall) - stroke drawn inside the fill area
    val strokeWidth = 2.5f
    clipRect(
        left = roomLeft,
        top = roomTop,
        right = roomLeft + roomWidth,
        bottom = roomTop + roomHeight
    ) {
        drawRect(
            color = Color(0xFF495057),
            topLeft = Offset(roomLeft, roomTop),
            size = Size(roomWidth, roomHeight),
            style = Stroke(width = strokeWidth)
        )
    }

    // Furnitures
    plan.furnitures.forEachIndexed { index, f ->
        val fill = when (f.category) {
            FurnCategory.BED -> Color(0xFFE5DEFF)
            FurnCategory.DESK -> Color(0xFFFFF1B6)
            FurnCategory.SOFA -> Color(0xFFC7F2FF)
            FurnCategory.WARDROBE -> Color(0xFFD0F5CB)
            FurnCategory.TABLE -> Color(0xFFEFD3D7)
            else -> Color(0xFFE9ECEF)
        }
        val topLeft = Offset(f.rect.left, f.rect.top)
        val size = androidx.compose.ui.geometry.Size(f.rect.width(), f.rect.height())

        drawRect(color = fill, topLeft = topLeft, size = size)
        drawRect(color = Color.Black, topLeft = topLeft, size = size, style = Stroke(1.5f))

        if (showLabels) {
            val label = labels.getOrNull(index) ?: f.category.korLabel()
            drawContext.canvas.nativeCanvas.apply {
                val paint = Paint().apply {
                    isAntiAlias = true
                    textAlign = Paint.Align.CENTER
                    textSize = with(density) { 12.sp.toPx() }
                    color = android.graphics.Color.BLACK
                }
                val cx = f.rect.centerX()
                val cy = f.rect.centerY() - (paint.ascent() + paint.descent()) / 2f
                drawText(label, cx, cy, paint)
            }
        }
    }
}

private fun labelsForPlan(plan: FloorPlan): List<String> {
    val counters = mutableMapOf<FurnCategory, Int>()
    val out = ArrayList<String>(plan.furnitures.size)
    plan.furnitures.forEach { f ->
        val next = (counters[f.category] ?: 0) + 1
        counters[f.category] = next
        out += f.category.korLabel() + next.toString()
    }
    return out
}
