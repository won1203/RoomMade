package com.example.roommade.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.roommade.model.FloorPlan
import com.example.roommade.vm.FloorPlanViewModel

@Composable
fun PlanSelectionScreen(
    onSelectPlan: () -> Unit,
    onBack: () -> Unit,
    vm: FloorPlanViewModel
) {
    LaunchedEffect(Unit) { vm.generateRecommendations() }
    val recs = vm.recommendations

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("AI layout concepts", style = MaterialTheme.typography.headlineSmall)
        if (recs.isEmpty()) {
            Text("Preparing layout options...", style = MaterialTheme.typography.bodyMedium)
        } else {
            recs.forEach { rec ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            vm.chooseRecommendation(rec)
                            onSelectPlan()
                        }
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(100.dp)
                    ) {
                        PlanPreview(
                            rec.plan,
                            Modifier
                                .width(110.dp)
                                .height(150.dp)
                                .offset(y = (-18).dp) // 여기를 추가/조절해서 더 위로
                                .align(Alignment.Top),
                        alignLeftInCanvas = true,
                        alignXFraction = 0f,
                        alignYFraction = 0f
                        )

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(rec.title, style = MaterialTheme.typography.titleMedium)
                            Text(
                                rec.rationale,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 3
                            )
                        }
                    }
                }
            }
        }
        TextButton(onClick = onBack) { Text("Back") }
    }
}

@Composable
fun PlanPreview(
    plan: FloorPlan,
    modifier: Modifier = Modifier,
    fitToHeight: Boolean = false,
    alignLeftInCanvas: Boolean = false,
    alignXFraction: Float = 0.5f,
    alignYFraction: Float = 0.5f
) {

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        PlanCanvasStatic(
            plan = plan,
            modifier = Modifier.fillMaxSize(),
            fitToHeight = fitToHeight,
            alignLeftInCanvas = alignLeftInCanvas,
            alignXFraction = alignXFraction,
            alignYFraction = alignYFraction
        )
    }
}
