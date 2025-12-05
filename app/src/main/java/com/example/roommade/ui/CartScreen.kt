package com.example.roommade.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.roommade.vm.FloorPlanViewModel
import com.example.roommade.vm.FloorPlanViewModel.GeneratedBoard

@Composable
fun CartScreen(
    vm: FloorPlanViewModel,
    onSearchMore: () -> Unit
) {
    val cart = vm.cartItems
    val grouped = cart.groupBy { it.sessionId }
    val boards = vm.generatedBoards.associateBy { it.id }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("장바구니", style = MaterialTheme.typography.headlineSmall)
        Text(
            "세션별로 담긴 가구를 확인하세요.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (cart.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("장바구니가 비어 있습니다.")
            }
            return
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            grouped.forEach { (sessionId, itemsInSession) ->
                val board: GeneratedBoard? = sessionId?.let { boards[it] }
                val headerTitle = board?.styleLabel ?: "세션 구분 없음"
                val headerSubtitle = when {
                    board != null -> board.roomCategory ?: "방 카테고리 없음"
                    sessionId != null -> "세션 ID: ${sessionId.take(8)}"
                    else -> "공통 장바구니"
                }
                item(key = "header_$sessionId") {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            headerTitle,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            headerSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        board?.let { b ->
                            decodeDataUri(b.imageUrl)?.let { bmp ->
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = b.styleLabel,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 6.dp)
                                        .height(120.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }
                items(itemsInSession, key = { it.item.id }) { entry ->
                    val item = entry.item
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val thumb = remember(item.imageUrl) { decodeDataUri(item.imageUrl) }
                            if (thumb != null) {
                                Image(
                                    bitmap = thumb.asImageBitmap(),
                                    contentDescription = item.title,
                                    modifier = Modifier
                                        .size(96.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(item.imageUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = item.title,
                                    modifier = Modifier
                                        .size(96.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.title, style = MaterialTheme.typography.titleMedium, maxLines = 2)
                                Text(
                                    text = "가격 ${item.price}원",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = item.mallName ?: "판매처 정보 없음",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(onClick = { vm.toggleCartItem(item) }) { Text("제거") }
                                    if (item.link.isNotBlank()) {
                                        TextButton(onClick = {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.link))
                                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            context.startActivity(intent)
                                        }) { Text("구매 페이지") }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun decodeDataUri(data: String?): Bitmap? {
    if (data.isNullOrBlank()) return null
    if (!data.startsWith("data:image")) return null
    val comma = data.indexOf(',')
    if (comma <= 0) return null
    val base64Part = data.substring(comma + 1)
    return try {
        val bytes = Base64.decode(base64Part, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (_: Throwable) {
        null
    }
}
