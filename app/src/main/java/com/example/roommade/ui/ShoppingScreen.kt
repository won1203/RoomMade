package com.example.roommade.ui

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
fun ShoppingScreen(onDone: () -> Unit, onBack: () -> Unit, vm: FloorPlanViewModel) {
    val list = remember { mutableStateListOf<CatalogItem>() }
    LaunchedEffect(Unit) { list.clear(); list.addAll(vm.buildShoppingForSelection()) }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("구매 추천 리스트", style = MaterialTheme.typography.headlineSmall)
        if (list.isEmpty()) Text("추천할 품목이 없습니다.")
        else {
            LazyColumn(Modifier.weight(1f)) {
                items(list) { item ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Text(item.name, style = MaterialTheme.typography.titleMedium)
                            Text("카테고리: ${item.category}", style = MaterialTheme.typography.bodySmall)
                            Text("스타일: ${item.styleTags.joinToString()}", style = MaterialTheme.typography.bodySmall)
                            Text("가격: ${"%,d".format(item.priceKRW)}원", style = MaterialTheme.typography.titleSmall)
                            if (item.shopLinks.isNotEmpty()) {
                                Spacer(Modifier.height(4.dp))
                                item.shopLinks.forEach { link ->
                                    Text("• ${link.vendor}", color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onBack) { Text("뒤로") }
            Button(onClick = onDone) { Text("완료") }
        }
    }
}
