package com.example.roommade.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.roommade.vm.FloorPlanViewModel

@Composable
fun ResultScreen(onShop: () -> Unit, onBack: () -> Unit, vm: FloorPlanViewModel) {
    val before = vm.beforePlan
    val rec = vm.selectedRec

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Before / After", style = MaterialTheme.typography.headlineSmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                Text("Before", style = MaterialTheme.typography.titleMedium)
                if (before != null) {
                    PlanPreview(
                        plan = before,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .offset(x = (-50).dp)
                            .offset(y = (-18).dp),
                        alignLeftInCanvas = false,
                        alignXFraction = 0f,
                        alignYFraction = 0f
                    )
                } else {
                    Text("다음")
                }
            }
            Column(Modifier.weight(1f)) {
                Text("After (${rec?.id ?: "-"})", style = MaterialTheme.typography.titleMedium)
                if (rec != null) {
                    PlanPreview(
                        plan = rec.plan,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .offset(x = (-50).dp)
                            .offset(y = (-18).dp),
                        alignLeftInCanvas = false,
                        alignXFraction = 0.2f,
                        alignYFraction = 0f
                    )
                } else {
                    Text("다음")
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedButton(onClick = onBack) { Text("이전") }
            Button(onClick = onShop, enabled = rec != null) { Text("구매 추천 보기") }
        }
    }
}

