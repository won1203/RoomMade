package com.example.roommade.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.roommade.model.CatalogItem
import com.example.roommade.vm.FloorPlanViewModel

@Composable
fun RecommendationCatalogScreen(
    onNext: () -> Unit,
    onBack: () -> Unit,
    vm: FloorPlanViewModel
) {
    LaunchedEffect(vm.styleTags) { vm.buildRecommendationCatalog() }
    val catalog = vm.recommendedCatalog
    val chosen = vm.chosenCatalog

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("감성에 맞는 추천 가구", style = MaterialTheme.typography.headlineSmall)
        val tags = if (vm.styleTags.isEmpty()) "감지된 태그 없음" else vm.styleTags.joinToString()
        Text("적용된 스타일: $tags", style = MaterialTheme.typography.bodyMedium)

        LazyColumn(Modifier.weight(1f)) {
            items(catalog) { item ->
                CatalogRow(
                    item = item,
                    selected = chosen.contains(item.id),
                    onToggle = { vm.toggleChooseCatalogItem(item.id) }
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onBack) { Text("뒤로") }
            Button(onClick = onNext) { Text("AI 배치 보기") }
        }
    }
}

@Composable
private fun CatalogRow(item: CatalogItem, selected: Boolean, onToggle: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onToggle() }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                val title = if (selected) "[선택] ${item.name}" else item.name
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text("카테고리: ${item.category}", style = MaterialTheme.typography.bodySmall)
                Text("스타일 태그: ${item.styleTags.joinToString()}", style = MaterialTheme.typography.bodySmall)
            }
            Text(
                "${"%,d".format(item.priceKRW)}원",
                style = MaterialTheme.typography.titleSmall
            )
        }
    }
}
