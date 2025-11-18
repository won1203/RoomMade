package com.example.roommade.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.roommade.vm.FloorPlanViewModel

@Composable
fun ResultScreen(onSearch: () -> Unit, onBack: () -> Unit, vm: FloorPlanViewModel) {
    val before = vm.beforePlan
    val rec = vm.selectedRec

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
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
                    Text("No data")
                }
            }
            Column(Modifier.weight(1f)) {
                Text("After", style = MaterialTheme.typography.titleMedium)
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
                    Text("No data")
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Back") }
            Button(onClick = onSearch, enabled = rec != null, modifier = Modifier.weight(1f)) {
                Text("Search furniture")
            }
        }
    }
}
