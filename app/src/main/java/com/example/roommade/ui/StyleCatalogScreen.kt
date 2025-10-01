package com.example.roommade.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.roommade.model.CatalogItem
import com.example.roommade.vm.FloorPlanViewModel

@Composable
fun StyleCatalogScreen(onNext: () -> Unit, onBack: () -> Unit, vm: FloorPlanViewModel) {
    LaunchedEffect(vm.styleTags) { vm.buildStyleCatalog() }
    val catalog = vm.recommendedCatalog
    val chosen = vm.chosenCatalog

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("스타일 기반 가구 추천", style = MaterialTheme.typography.headlineSmall)
        Text("선택된 태그: ${vm.styleTags.joinToString()}")

        LazyColumn(Modifier.weight(1f)) {
            items(catalog) { item ->
                CatalogRow(item, chosen.contains(item.id)) { vm.toggleChooseCatalogItem(item.id) }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onBack) { Text("뒤로") }
            Button(onClick = { vm.spawnChosenCatalogToPlan(); onNext() }, enabled = chosen.isNotEmpty()) {
                Text("선택한 가구로 A/B/C 보기")
            }
        }
    }
}

@Composable
private fun CatalogRow(item: CatalogItem, selected: Boolean, onToggle: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onToggle() }) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(if (selected) "✅ ${item.name}" else item.name, style = MaterialTheme.typography.titleMedium)
                Text("카테고리: ${item.category}", style = MaterialTheme.typography.bodySmall)
                Text("스타일: ${item.styleTags.joinToString()}", style = MaterialTheme.typography.bodySmall)
            }
            Text("${"%,d".format(item.priceKRW)}원", style = MaterialTheme.typography.titleSmall)
        }
    }
}
