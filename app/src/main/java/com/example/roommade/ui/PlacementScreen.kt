package com.example.roommade.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.roommade.ui.planLabelsFor
import com.example.roommade.vm.FloorPlanViewModel

@Composable
fun PlacementScreen(
    onBack: () -> Unit,
    onRender: () -> Unit,
    vm: FloorPlanViewModel
) {
    val cartCount = vm.cartCount()
    val plan = vm.floorPlan
    val selectedIndex = vm.selectedFurnitureIndex
    val selectedFurniture = plan.furnitures.getOrNull(selectedIndex ?: -1)
    val labels = remember(plan.furnitures) { planLabelsFor(plan) }
    val scale = plan.scaleMmPerPx.coerceAtLeast(1f)

    var widthMmState = remember { mutableFloatStateOf(0f) }
    var heightMmState = remember { mutableFloatStateOf(0f) }
    var areaState = remember { mutableFloatStateOf(vm.roomSpec.areaPyeong) }
    var aspectState = remember { mutableFloatStateOf(vm.roomSpec.aspect) }

    LaunchedEffect(cartCount) {
        vm.syncCartFurniture()
    }

    LaunchedEffect(selectedIndex, plan.furnitures) {
        val w = selectedFurniture?.rect?.width()?.times(scale) ?: 0f
        val h = selectedFurniture?.rect?.height()?.times(scale) ?: 0f
        widthMmState.floatValue = w
        heightMmState.floatValue = h
    }

    LaunchedEffect(vm.roomSpec.areaPyeong, vm.roomSpec.aspect) {
        areaState.floatValue = vm.roomSpec.areaPyeong
        aspectState.floatValue = vm.roomSpec.aspect
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
                    text = "장바구니에서 담은 가구를 공간 위에 직접 배치해 보세요.",
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

        // 평면도 크기 조절
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("평면도 크기", style = MaterialTheme.typography.titleMedium)
            Text("면적: ${"%.1f".format(areaState.floatValue)}평", style = MaterialTheme.typography.bodySmall)
            Slider(
                value = areaState.floatValue,
                onValueChange = {
                    areaState.floatValue = it
                    vm.setRoomAreaPyeong(it)
                },
                valueRange = 2f..40f
            )
            Text("가로세로 비율: ${"%.2f".format(aspectState.floatValue)}", style = MaterialTheme.typography.bodySmall)
            Slider(
                value = aspectState.floatValue,
                onValueChange = {
                    aspectState.floatValue = it
                    vm.setRoomAspect(it)
                },
                valueRange = 0.5f..2.0f
            )
        }

        // 가구 선택 및 크기/방향 조절
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("가구 선택 · 크기 조절", style = MaterialTheme.typography.titleMedium)
            if (plan.furnitures.isEmpty()) {
                Text("배치할 가구가 없습니다. 쇼핑 화면에서 담아주세요.", style = MaterialTheme.typography.bodySmall)
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(plan.furnitures) { index, _ ->
                        val label = labels.getOrNull(index) ?: "가구${index + 1}"
                        FilterChip(
                            selected = selectedIndex == index,
                            onClick = { vm.selectFurniture(index) },
                            label = { Text(label) }
                        )
                    }
                }
                val hasSelection = selectedFurniture != null
                val widthMm = widthMmState.floatValue.coerceAtLeast(200f)
                val heightMm = heightMmState.floatValue.coerceAtLeast(200f)

                Text(
                    text = if (hasSelection)
                        "현재 크기: ${"%.0f".format(widthMm)} x ${"%.0f".format(heightMm)} mm"
                    else "가구를 선택하면 크기를 조절할 수 있습니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = widthMm,
                    onValueChange = { newVal ->
                        if (hasSelection) {
                            widthMmState.floatValue = newVal
                            vm.resizeSelected(newVal, heightMm)
                        }
                    },
                    valueRange = 400f..2600f,
                    enabled = hasSelection
                )
                Slider(
                    value = heightMm,
                    onValueChange = { newVal ->
                        if (hasSelection) {
                            heightMmState.floatValue = newVal
                            vm.resizeSelected(widthMm, newVal)
                        }
                    },
                    valueRange = 400f..2600f,
                    enabled = hasSelection
                )
                Button(
                    onClick = {
                        if (hasSelection) {
                            val newW = heightMm
                            val newH = widthMm
                            widthMmState.floatValue = newW
                            heightMmState.floatValue = newH
                            vm.resizeSelected(newW, newH)
                        }
                    },
                    enabled = hasSelection,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("가로/세로 뒤집기")
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = if (cartCount > 0) "선택한 가구 ${cartCount}개가 배치에 추가되었습니다." else "장바구니가 비어 있습니다. 쇼핑 화면에서 가구를 선택해 주세요.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                Text("뒤로")
            }
            Button(
                onClick = {
                    vm.markPlacementRendered()
                    onRender()
                },
                enabled = cartCount > 0,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("렌더링 하기")
            }
        }
        if (vm.placementRendered) {
            Text(
                text = "렌더링 준비 완료! 원하는 위치에 배치되었습니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
