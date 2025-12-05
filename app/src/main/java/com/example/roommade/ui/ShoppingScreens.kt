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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
    onBack: () -> Unit,
    onGoPlacement: () -> Unit
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
    val cartCount = vm.cartCount()
    val cartItems = vm.cartItems
    var cartExpanded by remember { mutableStateOf(false) }

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

    LaunchedEffect(cartCount) {
        if (cartCount == 0) cartExpanded = false
    }

    ScreenContainer(
        title = "추천 가구",
        subtitle = "카테고리와 검색어로 원하는 가구를 찾아보세요"
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "추천 결과를 확인하고 필요한 가구를 찾아보세요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SectionCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("카테고리 필터", style = MaterialTheme.typography.titleMedium)
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("장바구니: ${cartCount}개", style = MaterialTheme.typography.bodyMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { cartExpanded = !cartExpanded },
                                enabled = cartCount > 0,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(if (cartExpanded) "장바구니 닫기" else "장바구니 목록")
                            }
                            Button(
                                onClick = onGoPlacement,
                                enabled = cartCount > 0,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("배치 생성")
                            }
                        }
                    }
                    if (cartExpanded) {
                        CartItemsSummary(
                            items = cartItems.map { it.item },
                            formatter = formatter,
                            onRemove = { vm.toggleCartItem(it) }
                        )
                    }
                    Button(
                        onClick = { vm.searchShoppingItems(currentQuery) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = hasCredential && !isLoading
                    ) {
                        Text("추천 결과 불러오기")
                    }
                    if (!hasCredential) {
                        Text(
                            text = "네이버 쇼핑 함수 URL을 local.properties의 NAVER_SHOPPING_FUNCTION_URL로 설정하세요.",
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
                }
            }

            when {
                isLoading -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                items.isEmpty() -> {
                    SectionCard {
                        Text(
                            text = "검색 결과가 없습니다. 다른 감성 또는 카테고리를 선택해 보세요.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(items, key = { it.id }) { item ->
                            ShoppingResultCard(
                                item = item,
                                formatter = formatter,
                                isChecked = vm.isInCart(item.id),
                                onToggle = { vm.toggleCartItem(item) },
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
}

@Composable
private fun ShoppingResultCard(
    item: NaverShoppingItem,
    formatter: NumberFormat,
    isChecked: Boolean,
    onToggle: () -> Unit,
    onOpenLink: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        onClick = { onOpenLink(item.link) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "체크",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Checkbox(
                    checked = isChecked,
                    onCheckedChange = { onToggle() }
                )
            }
            item.imageUrl?.let { imageUrl ->
                AsyncImage(
                    model = imageUrl,
                    contentDescription = item.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
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
            val mallInfo = item.mallName ?: "판매처 정보 없음"
            Text(
                text = "판매처: $mallInfo",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "가격 ${formatter.format(item.price)} 원",
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

@Composable
private fun CartItemsSummary(
    items: List<NaverShoppingItem>,
    formatter: NumberFormat,
    onRemove: (NaverShoppingItem) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("장바구니 목록", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${items.size}개",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (items.isEmpty()) {
                Text(
                    text = "선택한 가구가 없습니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "가격 ${formatter.format(item.price)} 원",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(onClick = { onRemove(item) }) {
                                Text("제거")
                            }
                        }
                    }
                }
            }
        }
    }
}

private val furnitureCategoryFilters = listOf(
    FurnitureCategoryFilter("전체", null),
    FurnitureCategoryFilter("침대", "침대"),
    FurnitureCategoryFilter("소파", "소파"),
    FurnitureCategoryFilter("의자", "의자"),
    FurnitureCategoryFilter("책상", "책상"),
    FurnitureCategoryFilter("수납", "수납"),
    FurnitureCategoryFilter("조명", "조명"),
    FurnitureCategoryFilter("카펫", "카펫")
)
