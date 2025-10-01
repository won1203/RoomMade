@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.example.roommade.ui

import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roommade.model.FloorPlan
import com.example.roommade.model.FurnCategory
import com.example.roommade.model.OpeningType
import com.example.roommade.vm.FloorPlanViewModel
import com.example.roommade.model.korLabel

private sealed interface DragTarget {
    data class Furniture(val index: Int) : DragTarget
    data class Door(val index: Int) : DragTarget
    data class Window(val index: Int) : DragTarget
}

@Composable
fun EditFloorplanScreen(
    onNext: () -> Unit,
    vm: FloorPlanViewModel
) {
    val fp = vm.floorPlan

    // 인벤토리 스폰 누락 대비
    LaunchedEffect(fp.furnitures.size, vm.inventory) {
        if (fp.furnitures.isEmpty() && vm.inventory.isNotEmpty()) {
            vm.spawnInventoryToPlan(resetPrevious = true)
        }
    }

    var dragTarget by remember { mutableStateOf<DragTarget?>(null) }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("평면도 편집") },
            actions = { Button(onClick = onNext) { Text("다음") } }
        )

        // 수동 추가는 문/창만
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    // 방 내부에 60x30px 크기로 좌->우로 타일링
                    val pad = 16f
                    val w = 60f; val h = 30f
                    val count = fp.doors.size
                    val maxRight = fp.bounds.right - pad
                    val perRow = ((fp.bounds.width() - pad * 2) / (w + 8f)).toInt().coerceAtLeast(1)
                    val row = count / perRow
                    val col = count % perRow
                    val left = (fp.bounds.left + pad) + col * (w + 8f)
                    val top  = (fp.bounds.top + pad) + row * (h + 8f)
                    vm.addOpening(OpeningType.DOOR, RectF(left, top, left + w, top + h))
                }
            ) { Text("문") }

            Button(
                onClick = {
                    // 창은 상단 벽을 따라 100x24px로 타일링
                    val pad = 16f
                    val w = 100f; val h = 24f
                    val count = fp.windows.size
                    val perRow = ((fp.bounds.width() - pad * 2) / (w + 8f)).toInt().coerceAtLeast(1)
                    val row = count / perRow
                    val col = count % perRow
                    val left = (fp.bounds.left + pad) + col * (w + 8f)
                    val top  = (fp.bounds.top + pad) + row * (h + 8f)
                    vm.addOpening(OpeningType.WINDOW, RectF(left, top, left + w, top + h))
                }
            ) { Text("창") }

            Spacer(Modifier.weight(1f))
            Text(
                "스케일(mm/px): ${"%.1f".format(fp.scaleMmPerPx)}",
                modifier = Modifier.align(Alignment.CenterVertically),
                style = MaterialTheme.typography.bodySmall
            )
        }

        // 캔버스
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .background(Color(0xFFF6F2F8))
        ) {
            val density = LocalDensity.current

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    // ✅ fp를 키로 쓰지 말고 고정 → 드래그 중 재생성 방지(끊김 해결)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                dragTarget = hitTest(vm.floorPlan, offset)
                            },
                            onDragEnd = { dragTarget = null },
                            onDragCancel = { dragTarget = null },
                            onDrag = { change, drag ->
                                change.consume()
                                when (val t = dragTarget) {
                                    is DragTarget.Furniture ->
                                        vm.moveFurniture(t.index, drag.x, drag.y)
                                    is DragTarget.Door ->
                                        vm.moveOpening(true, t.index, drag.x, drag.y)
                                    is DragTarget.Window ->
                                        vm.moveOpening(false, t.index, drag.x, drag.y)
                                    null -> Unit
                                }
                            }
                        )
                    }
            ) {
                val cur = vm.floorPlan // 드로잉은 최신 상태 기준

                // 방 외곽
                drawRect(
                    color = Color(0xFF495057),
                    topLeft = Offset(cur.bounds.left, cur.bounds.top),
                    size = androidx.compose.ui.geometry.Size(cur.bounds.width(), cur.bounds.height()),
                    style = Stroke(width = 2.5f)
                )

                // 문/창
                cur.doors.forEach {
                    drawRect(
                        color = Color(0xFF8CE99A),
                        topLeft = Offset(it.rect.left, it.rect.top),
                        size = androidx.compose.ui.geometry.Size(it.rect.width(), it.rect.height())
                    )
                }
                cur.windows.forEach {
                    drawRect(
                        color = Color(0xFF74C0FC),
                        topLeft = Offset(it.rect.left, it.rect.top),
                        size = androidx.compose.ui.geometry.Size(it.rect.width(), it.rect.height())
                    )
                }

                // 가구(색 + 라벨)
                cur.furnitures.forEach { f ->
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

                    drawContext.canvas.nativeCanvas.apply {
                        val paint = Paint().apply {
                            isAntiAlias = true
                            textAlign = Paint.Align.CENTER
                            textSize = with(density) { 12.sp.toPx() }
                            color = android.graphics.Color.BLACK
                        }
                        val cx = f.rect.centerX()
                        val cy = f.rect.centerY() - (paint.ascent() + paint.descent()) / 2f
                        // 한글 라벨 원하면 map으로 바꿔도 됨
                        drawText(f.category.korLabel(), cx, cy, paint)
                    }
                }
            }
        }
    }
}

/** 터치 지점에서 최상단 객체 탐색(가구 → 문 → 창) */
private fun hitTest(fp: FloorPlan, p: Offset): DragTarget? {
    for (i in fp.furnitures.indices.reversed()) {
        if (fp.furnitures[i].rect.contains(p.x, p.y)) return DragTarget.Furniture(i)
    }
    for (i in fp.doors.indices.reversed()) {
        if (fp.doors[i].rect.contains(p.x, p.y)) return DragTarget.Door(i)
    }
    for (i in fp.windows.indices.reversed()) {
        if (fp.windows[i].rect.contains(p.x, p.y)) return DragTarget.Window(i)
    }
    return null
}
