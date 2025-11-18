package com.example.roommade.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.roommade.network.NaverShoppingItem
import com.example.roommade.vm.FloorPlanViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingWebViewScreen(
    query: String,
    vm: FloorPlanViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val currentQuery = vm.shoppingQueryInput
    val items = vm.shoppingResults
    val isLoading = vm.shoppingIsLoading
    val errorMessage = vm.shoppingErrorMessage
    val hasCredential = vm.hasShoppingCredentials()
    val formatter = remember { NumberFormat.getNumberInstance(Locale.KOREA) }
    var lastAutoQuery by remember { mutableStateOf("") }
    val categoryFilters = remember { furnitureCategoryFilters }
    val selectedCategory = vm.shoppingCategoryFilter

    LaunchedEffect(query) {
        if (query.isNotBlank()) {
            vm.updateShoppingQueryInput(query)
        } else {
            vm.ensureShoppingQueryPrefilled()
        }
    }

    LaunchedEffect(currentQuery, hasCredential) {
        if (hasCredential && currentQuery.isNotBlank() && currentQuery != lastAutoQuery) {
            lastAutoQuery = currentQuery
            vm.searchShoppingItems(currentQuery)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "AI 감성 기반 가구 추천",
                style = MaterialTheme.typography.headlineSmall
            )
            OutlinedButton(onClick = onBack) {
                Text("뒤로")
            }
        }
        Text(
            text = "AI가 분석한 감성과 선택한 가구 카테고리를 조합해 네이버 쇼핑을 자동으로 검색합니다.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "원하는 가구 종류를 선택하세요.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categoryFilters) { filter ->
                val isSelected = if (filter.query == null) {
                    selectedCategory == null
                } else {
                    selectedCategory == filter.query
                }
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        if (filter.query == null) {
                            vm.updateShoppingCategoryFilter(null)
                        } else {
                            val next = if (isSelected) null else filter.query
                            vm.updateShoppingCategoryFilter(next)
                        }
                    },
                    label = { Text(filter.label) }
                )
            }
        }
        Button(
            onClick = { vm.searchShoppingItems(currentQuery) },
            modifier = Modifier.fillMaxWidth(),
            enabled = hasCredential && !isLoading
        ) {
            Text("추천 결과 새로고침")
        }
        if (!hasCredential) {
            Text(
                text = "NAVER API 키가 설정되지 않았습니다. local.properties 또는 gradle.properties에 NAVER_CLIENT_ID/NAVER_CLIENT_SECRET 값을 입력하세요.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        when {
            isLoading -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            items.isEmpty() -> {
                Text(
                    text = "검색 결과가 없습니다. 다른 감성 또는 카테고리를 선택해 보세요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items, key = { it.id }) { item ->
                        ShoppingResultCard(
                            item = item,
                            formatter = formatter,
                            onOpenLink = { link ->
                                if (link.isNotBlank()) {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
                                    runCatching { context.startActivity(intent) }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ShoppingResultCard(
    item: NaverShoppingItem,
    formatter: NumberFormat,
    onOpenLink: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { onOpenLink(item.link) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item.imageUrl?.let { imageUrl ->
                AsyncImage(
                    model = imageUrl,
                    contentDescription = item.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            val mallInfo = item.mallName ?: "제휴 쇼핑몰"
            Text(
                text = "판매처: $mallInfo",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "가격: ${formatter.format(item.price)} 원",
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(4.dp))
            TextButton(onClick = { onOpenLink(item.link) }) {
                Text("구매 페이지 열기")
            }
        }
    }
}

private data class FurnitureCategoryFilter(val label: String, val query: String?)

private val furnitureCategoryFilters = listOf(
    FurnitureCategoryFilter("전체", null),
    FurnitureCategoryFilter("침대", "침대"),
    FurnitureCategoryFilter("소파", "소파"),
    FurnitureCategoryFilter("테이블", "테이블"),
    FurnitureCategoryFilter("책상", "책상"),
    FurnitureCategoryFilter("수납장", "수납장"),
    FurnitureCategoryFilter("옷장", "옷장"),
    FurnitureCategoryFilter("의자", "의자"),
    FurnitureCategoryFilter("조명", "조명"),
    FurnitureCategoryFilter("카페트", "카페트")
)
