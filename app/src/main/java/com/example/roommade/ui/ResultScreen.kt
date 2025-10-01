package com.example.roommade.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.roommade.vm.FloorPlanViewModel

@Composable
fun ResultScreen(onShop: () -> Unit, onBack: () -> Unit, vm: FloorPlanViewModel) {
    val before = vm.beforePlan
    val rec = vm.selectedRec

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Before / After", style = MaterialTheme.typography.headlineSmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                Text("Before", style = MaterialTheme.typography.titleMedium)
                if (before != null) PlanPreview(before, Modifier.fillMaxWidth().height(160.dp)) else Text("없음")
            }
            Column(Modifier.weight(1f)) {
                Text("After (${rec?.id ?: "-"})", style = MaterialTheme.typography.titleMedium)
                if (rec != null) PlanPreview(rec.plan, Modifier.fillMaxWidth().height(160.dp)) else Text("없음")
            }
        }
        Text(rec?.rationale ?: "", style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onBack) { Text("뒤로") }
            Button(onClick = onShop, enabled = rec != null) { Text("구매 추천 보기") }
        }
    }
}

