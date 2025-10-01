package com.example.roommade.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.roommade.model.FloorPlan
import com.example.roommade.vm.FloorPlanViewModel

@Composable
fun RecommendScreen(onSelectPlan: () -> Unit, onBack: () -> Unit, vm: FloorPlanViewModel) {
    LaunchedEffect(Unit) { vm.generateRecommendations() }
    val recs = vm.recommendations

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("AI 배치 추천", style = MaterialTheme.typography.headlineSmall)
        if (recs.isEmpty()) Text("추천 생성 중…")
        else {
            recs.forEach { rec ->
                Card(Modifier.fillMaxWidth().clickable { vm.chooseRecommendation(rec); onSelectPlan() }.padding(vertical = 4.dp)) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        PlanPreview(rec.plan, Modifier.size(120.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(rec.title, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(4.dp))
                            Text(rec.rationale, style = MaterialTheme.typography.bodyMedium, maxLines = 3)
                        }
                    }
                }
            }
        }
        TextButton(onClick = onBack) { Text("뒤로") }
    }
}

@Composable
fun PlanPreview(plan: FloorPlan, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawRect(Color(0xFF111418))
        drawRect(Color.White, topLeft = Offset(plan.bounds.left, plan.bounds.top),
            size = androidx.compose.ui.geometry.Size(plan.bounds.width(), plan.bounds.height()), style = Stroke(2f))
        plan.doors.forEach {
            drawRect(Color(0xFF8CE99A), Offset(it.rect.left, it.rect.top),
                androidx.compose.ui.geometry.Size(it.rect.width(), it.rect.height()))
        }
        plan.windows.forEach {
            drawRect(Color(0xFF74C0FC), Offset(it.rect.left, it.rect.top),
                androidx.compose.ui.geometry.Size(it.rect.width(), it.rect.height()))
        }
        plan.furnitures.forEach { f ->
            drawRect(Color(0xFFE9ECEF), Offset(f.rect.left, f.rect.top),
                androidx.compose.ui.geometry.Size(f.rect.width(), f.rect.height()), style = Stroke(1.5f))
        }
    }
}

