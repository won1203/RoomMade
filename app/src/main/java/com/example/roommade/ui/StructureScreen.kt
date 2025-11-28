package com.example.roommade.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.roommade.model.FurnCategory
import com.example.roommade.model.korLabel
import com.example.roommade.vm.FloorPlanViewModel

@Composable
fun StructureAreaScreen(
    onNext: () -> Unit,
    onBack: () -> Unit,
    vm: FloorPlanViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("공간 정보 입력", style = MaterialTheme.typography.headlineSmall)
        Text(
            text = "카테고리: ${vm.roomCategory.korLabel()}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "면적: ${"%.1f".format(vm.roomSpec.areaPyeong)}평",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "가로 ${"%.1f".format(vm.roomSpec.widthM)}m × ${"%.1f".format(vm.roomSpec.heightM)}m",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Slider(
                value = vm.roomSpec.areaPyeong,
                onValueChange = { vm.setRoomAreaPyeong(it) },
                valueRange = 2f..40f
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "가로세로 비율: ${"%.2f".format(vm.roomSpec.aspect)}",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "비율 1은 정사각형, 클수록 가로가 긴 방입니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Slider(
                value = vm.roomSpec.aspect,
                onValueChange = { vm.setRoomAspect(it) },
                valueRange = 0.5f..2.0f
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                Text("이전")
            }
            Button(onClick = onNext, modifier = Modifier.weight(1f)) {
                Text("다음")
            }
        }
    }
}

@Composable
fun StructureInventoryScreen(
    onNext: () -> Unit,
    onBack: () -> Unit,
    vm: FloorPlanViewModel
) {
    val categoryStates = remember(vm.inventory) {
        listOf(
            CategoryState(FurnCategory.BED, vm.inventory[FurnCategory.BED] ?: 1),
            CategoryState(FurnCategory.DESK, vm.inventory[FurnCategory.DESK] ?: 1),
            CategoryState(FurnCategory.WARDROBE, vm.inventory[FurnCategory.WARDROBE] ?: 1),
            CategoryState(FurnCategory.SOFA, vm.inventory[FurnCategory.SOFA] ?: 0)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("보유 가구 선택", style = MaterialTheme.typography.headlineSmall)
        Text(
            text = "카테고리: ${vm.roomCategory.korLabel()}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = "지금 방에 있는 가구 개수를 선택해 주세요. 0개도 설정 가능합니다.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            categoryStates.forEach { state ->
                FurnitureCounter(state)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                Text("이전")
            }
            Button(
                onClick = {
                    val counts = categoryStates.associate { it.category to it.count }
                    vm.prepareStructure(
                        areaPyeong = vm.roomSpec.areaPyeong,
                        aspectRatio = vm.roomSpec.aspect,
                        inventoryCounts = counts
                    )
                    onNext()
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("다음")
            }
        }
    }
}

@Composable
fun StructureLayoutScreen(
    onNext: () -> Unit,
    onBack: () -> Unit,
    vm: FloorPlanViewModel
) {
    val currentParams = StructureInputsSnapshot(
        area = vm.roomSpec.areaPyeong,
        aspect = vm.roomSpec.aspect,
        inventory = vm.inventory
    )
    var lastGeneratedParams by remember {
        mutableStateOf<StructureInputsSnapshot?>(
            if (vm.floorPlan.furnitures.isNotEmpty()) currentParams else null
        )
    }
    var generated by remember { mutableStateOf(lastGeneratedParams != null) }

    val inputsChanged = lastGeneratedParams != currentParams

    LaunchedEffect(inputsChanged) {
        if (inputsChanged) {
            vm.prepareStructure(
                areaPyeong = vm.roomSpec.areaPyeong,
                aspectRatio = vm.roomSpec.aspect,
                inventoryCounts = vm.inventory
            )
            generated = true
            lastGeneratedParams = currentParams
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("기본 배치 생성", style = MaterialTheme.typography.headlineSmall)

        Text(
            text = "입력한 면적과 가구 정보를 바탕으로 기본 평면도를 생성합니다.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = "면적 ${"%.1f".format(vm.roomSpec.areaPyeong)}평 · 비율 ${"%.2f".format(vm.roomSpec.aspect)}",
            style = MaterialTheme.typography.titleMedium
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                Text("이전")
            }
            Button(
                onClick = {
                    vm.prepareStructure(
                        areaPyeong = vm.roomSpec.areaPyeong,
                        aspectRatio = vm.roomSpec.aspect,
                        inventoryCounts = vm.inventory
                    )
                    generated = true
                    lastGeneratedParams = StructureInputsSnapshot(
                        area = vm.roomSpec.areaPyeong,
                        aspect = vm.roomSpec.aspect,
                        inventory = vm.inventory
                    )
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("기본 배치 생성")
            }
            Button(
                enabled = generated,
                onClick = onNext,
                modifier = Modifier.weight(1f)
            ) {
                Text("다음")
            }
        }

        if (generated) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "생성된 기본 평면도",
                    style = MaterialTheme.typography.titleMedium
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF1E9FF), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    PlanCanvasInteractive(
                        vm = vm,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White)
                            .border(1.dp, Color(0xFFADB5BD), RoundedCornerShape(10.dp)),
                        backgroundColor = Color.Transparent
                    )
                }
            }
        }
    }
}

private class CategoryState(val category: FurnCategory, initial: Int) {
    var count by mutableIntStateOf(initial)
}

@Composable
private fun FurnitureCounter(state: CategoryState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = state.category.korLabel(),
            style = MaterialTheme.typography.bodyLarge
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { if (state.count > 0) state.count-- }) { Text("-") }
            Text("${state.count}")
            Button(onClick = { state.count++ }) { Text("+") }
        }
    }
}

private data class StructureInputsSnapshot(
    val area: Float,
    val aspect: Float,
    val inventory: Map<FurnCategory, Int>
)
