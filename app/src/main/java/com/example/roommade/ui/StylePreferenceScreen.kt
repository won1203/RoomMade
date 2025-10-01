package com.example.roommade.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.roommade.vm.FloorPlanViewModel
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow

@OptIn(ExperimentalLayoutApi::class) // 실험적 API를 의도적으로 허용하는 애노테이션
@Composable
fun StylePreferenceScreen(onNext: () -> Unit, onBack: () -> Unit, vm: FloorPlanViewModel) {
    val tags = listOf("미니멀","캐주얼","밝은","어두운","우드톤","모던","빈티지","아늑")
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("원하는 분위기/스타일", style = MaterialTheme.typography.headlineSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            tags.forEach { t ->
                val selected = vm.styleTags.contains(t)
                FilterChip(selected, { vm.toggleStyle(t) }, label = { Text(t) })
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onBack) { Text("뒤로") }
            Button(onClick = onNext, enabled = vm.styleTags.isNotEmpty()) { Text("가구 추천 보기") }
        }
    }
}
