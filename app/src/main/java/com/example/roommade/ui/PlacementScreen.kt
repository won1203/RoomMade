package com.example.roommade.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.roommade.ui.planLabelsFor
import com.example.roommade.vm.FloorPlanViewModel

@Composable
fun PlacementScreen(
    onBack: () -> Unit,
    onRender: () -> Unit,
    onGenerateAi: () -> Unit,
    vm: FloorPlanViewModel
) {
    val cartCount = vm.cartCount()
    val plan = vm.floorPlan
    val selectedIndex = vm.selectedFurnitureIndex
    val selectedFurniture = plan.furnitures.getOrNull(selectedIndex ?: -1)
    val labels = remember(plan.furnitures) { planLabelsFor(plan) }
    val scale = plan.scaleMmPerPx.coerceAtLeast(1f)

    var widthMm by remember { mutableFloatStateOf(0f) }
    var heightMm by remember { mutableFloatStateOf(0f) }
    var showSizeDialog by remember { mutableStateOf(false) }

    LaunchedEffect(cartCount) { vm.syncCartFurniture() }

    LaunchedEffect(selectedIndex, plan.furnitures) {
        widthMm = (selectedFurniture?.rect?.width() ?: 0f) * scale
        heightMm = (selectedFurniture?.rect?.height() ?: 0f) * scale
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("가구 배치", style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = "쇼핑 바구니에서 가져온 가구를 공간에 배치하세요.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(onClick = onBack, shape = RoundedCornerShape(12.dp)) {
                Text("뒤로")
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp),
            tonalElevation = 2.dp,
            shape = RoundedCornerShape(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                PlanCanvasInteractive(
                    vm = vm,
                    modifier = Modifier.fillMaxSize(),
                    backgroundColor = MaterialTheme.colorScheme.surface
                )
            }
        }

        Text(
            text = if (cartCount > 0) "카트에 가구가 ${cartCount}개 있습니다. AI 이미지 생성을 시작하세요." else "카트가 비어 있습니다. 쇼핑 화면에서 가구를 추가하세요.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Button(
            onClick = onGenerateAi,
            enabled = plan.furnitures.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("이미지 생성")
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("선택 및 크기 조절", style = MaterialTheme.typography.titleMedium)
            if (plan.furnitures.isEmpty()) {
                Text("배치된 가구가 없습니다. 쇼핑 화면에서 가구를 추가하세요.", style = MaterialTheme.typography.bodySmall)
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(plan.furnitures) { index, _ ->
                        val label = labels.getOrNull(index) ?: "가구 ${index + 1}"
                        FilterChip(
                            selected = selectedIndex == index,
                            onClick = {
                                vm.selectFurniture(index)
                                showSizeDialog = true
                            },
                            label = { Text(label) }
                        )
                    }
                }
                val hasSelection = selectedFurniture != null
                Text(
                    text = if (hasSelection)
                        "선택된 가구: ${labels.getOrNull(selectedIndex ?: -1) ?: "가구"}"
                    else "가구를 터치하면 크기 조절 창이 열립니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showSizeDialog) {
        val hasSelection = selectedFurniture != null
        val clampedW = widthMm.coerceAtLeast(200f)
        val clampedH = heightMm.coerceAtLeast(200f)
        AlertDialog(
            onDismissRequest = { showSizeDialog = false },
            confirmButton = {
                Button(
                    onClick = { showSizeDialog = false },
                    enabled = hasSelection
                ) { Text("닫기") }
            },
            title = { Text("가구 크기 조절") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (hasSelection)
                            "현재 크기: ${"%.0f".format(clampedW)} x ${"%.0f".format(clampedH)} mm"
                        else "가구를 선택하세요.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Slider(
                        value = clampedW,
                        onValueChange = { newVal ->
                            if (hasSelection) {
                                widthMm = newVal
                                vm.resizeSelected(newVal, clampedH)
                            }
                        },
                        valueRange = 400f..2600f,
                        enabled = hasSelection
                    )
                    Slider(
                        value = clampedH,
                        onValueChange = { newVal ->
                            if (hasSelection) {
                                heightMm = newVal
                                vm.resizeSelected(clampedW, newVal)
                            }
                        },
                        valueRange = 400f..2600f,
                        enabled = hasSelection
                    )
                }
            }
        )
    }
}
