package com.example.roommade.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.roommade.model.FurnCategory
import com.example.roommade.vm.FloorPlanViewModel

@Composable
fun InventoryScreen(onNext: () -> Unit, vm: FloorPlanViewModel) {
    class RowState(val cat: FurnCategory, qty: Int) {
        var qty by mutableIntStateOf(qty)
    }

    // VM에 저장된 값으로 초기화(없으면 기본)
    val initial = remember(vm.inventory) {
        mapOf(
            FurnCategory.BED to (vm.inventory[FurnCategory.BED] ?: 1),
            FurnCategory.DESK to (vm.inventory[FurnCategory.DESK] ?: 1),
            FurnCategory.WARDROBE to (vm.inventory[FurnCategory.WARDROBE] ?: 0),
            FurnCategory.SOFA to (vm.inventory[FurnCategory.SOFA] ?: 0),
            FurnCategory.TABLE to (vm.inventory[FurnCategory.TABLE] ?: 0),
        )
    }

    val rows = remember(initial) {
        mutableStateListOf(
            RowState(FurnCategory.BED, initial[FurnCategory.BED] ?: 1),
            RowState(FurnCategory.DESK, initial[FurnCategory.DESK] ?: 1),
            RowState(FurnCategory.WARDROBE, initial[FurnCategory.WARDROBE] ?: 0),
            RowState(FurnCategory.SOFA, initial[FurnCategory.SOFA] ?: 0),
            RowState(FurnCategory.TABLE, initial[FurnCategory.TABLE] ?: 0),
        )
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("보유 가구 수량", style = MaterialTheme.typography.headlineSmall)
        rows.forEach { r ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(r.cat.name)
                Row {
                    OutlinedButton(onClick = { if (r.qty > 0) r.qty-- }) { Text("-") }
                    Text("${r.qty}", modifier = Modifier.padding(horizontal = 12.dp))
                    Button(onClick = { r.qty++ }) { Text("+") }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = {
            // 1) VM에 인벤토리 수량 저장
            vm.setInventoryCounts(rows.associate { it.cat to it.qty })

            // 2) 평면도에 스폰(임시 배치)
            vm.spawnInventoryToPlan(resetPrevious = true)

            // 3) 편집 화면으로 이동
            onNext()
        }) { Text("평면도 편집으로") }
    }
}
