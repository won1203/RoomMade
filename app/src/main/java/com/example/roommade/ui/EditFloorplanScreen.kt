@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.example.roommade.ui

import android.graphics.RectF
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.roommade.model.OpeningType
import com.example.roommade.vm.FloorPlanViewModel

@Composable
fun EditFloorplanScreen(
    onNext: () -> Unit,
    vm: FloorPlanViewModel
) {
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("편집 모드") },
            actions = { Button(onClick = onNext) { Text("다음") } }
        )

        // 문/창 추가 액션 바
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    // 기본 크기 60x30px, 좌→우, 상→하 배치
                    val plan = vm.floorPlan
                    val pad = 16f
                    val w = 60f; val h = 30f
                    val count = plan.doors.size
                    val perRow = ((plan.bounds.width() - pad * 2) / (w + 8f)).toInt().coerceAtLeast(1)
                    val row = count / perRow
                    val col = count % perRow
                    val left = (plan.bounds.left + pad) + col * (w + 8f)
                    val top  = (plan.bounds.top + pad) + row * (h + 8f)
                    vm.addOpening(OpeningType.DOOR, RectF(left, top, left + w, top + h))
                }
            ) { Text("문") }

            Button(
                onClick = {
                    // 기본 크기 100x24px, 좌→우, 상→하 배치
                    val plan = vm.floorPlan
                    val pad = 16f
                    val w = 100f; val h = 24f
                    val count = plan.windows.size
                    val perRow = ((plan.bounds.width() - pad * 2) / (w + 8f)).toInt().coerceAtLeast(1)
                    val row = count / perRow
                    val col = count % perRow
                    val left = (plan.bounds.left + pad) + col * (w + 8f)
                    val top  = (plan.bounds.top + pad) + row * (h + 8f)
                    vm.addOpening(OpeningType.WINDOW, RectF(left, top, left + w, top + h))
                }
            ) { Text("창") }

            Spacer(Modifier.weight(1f))
            Text(
                "해상도(mm/px): ${"%.1f".format(vm.floorPlan.scaleMmPerPx)}",
                modifier = Modifier.align(Alignment.CenterVertically),
                style = MaterialTheme.typography.bodySmall
            )
        }

        // 캔버스 영역: 흰 배경 + 드래그 이동 가능
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            PlanCanvasInteractive(
                vm = vm,
                modifier = Modifier.fillMaxSize(),
                backgroundColor = Color.White
            )
        }
    }
}
