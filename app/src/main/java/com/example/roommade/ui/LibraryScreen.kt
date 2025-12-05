package com.example.roommade.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.roommade.util.ImageFetcher
import com.example.roommade.util.ImageSaver
import com.example.roommade.util.ImageSharer
import com.example.roommade.vm.FloorPlanViewModel
import kotlinx.coroutines.launch

@Composable
fun LibraryScreen(
    vm: FloorPlanViewModel,
    onOpenGenerate: () -> Unit
) {
    val boards = vm.generatedBoards
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val imageSaver = remember { ImageSaver() }
    val imageSharer = remember { ImageSharer() }
    val imageFetcher = remember { ImageFetcher() }
    val savedUris = remember { mutableStateMapOf<String, Uri>() }

    var selected by remember { mutableStateOf<FloorPlanViewModel.GeneratedBoard?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(selected?.id) {
        statusMessage = null
        errorMessage = null
    }

    suspend fun persistToGallery(board: FloorPlanViewModel.GeneratedBoard): Uri {
        savedUris[board.id]?.let { return it }
        val bitmap = imageFetcher.fetchBitmap(board.imageUrl)
        val uri = imageSaver.savePngToGallery(
            context = context,
            bitmap = bitmap,
            displayName = "roommade_${board.id}.png"
        )
        savedUris[board.id] = uri
        return uri
    }

    fun handleSave(board: FloorPlanViewModel.GeneratedBoard, shareAfter: Boolean = false) {
        if (isSaving) return
        scope.launch {
            isSaving = true
            statusMessage = null
            errorMessage = null
            try {
                val uri = persistToGallery(board)
                statusMessage = if (shareAfter) {
                    "갤러리에 저장 후 공유했어요."
                } else {
                    "갤러리에 저장했어요."
                }
                if (shareAfter) {
                    imageSharer.shareImage(context, uri)
                }
            } catch (t: Throwable) {
                errorMessage = t.message ?: "이미지를 처리하는 중 오류가 발생했습니다."
            } finally {
                isSaving = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("보관함", style = MaterialTheme.typography.headlineSmall)
        if (boards.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("아직 저장된 이미지가 없습니다.", style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = onOpenGenerate) {
                        Text("다시 생성하러 가기")
                    }
                }
            }
            return
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(boards, key = { it.id }) { item ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.clickable { selected = item }
                ) {
                    val decoded = remember(item.imageUrl) { decodeDataUri(item.imageUrl) }
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (decoded != null) {
                            Image(
                                bitmap = decoded.asImageBitmap(),
                                contentDescription = item.styleLabel,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(item.imageUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = item.styleLabel,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Text(
                            item.styleLabel.ifBlank { "스타일 미분석" },
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            item.roomCategory,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    selected?.let { board ->
        Dialog(onDismissRequest = { if (!isSaving) selected = null }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        board.styleLabel.ifBlank { "스타일 미분석" },
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        board.roomCategory,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val decoded = remember(board.imageUrl) { decodeDataUri(board.imageUrl) }
                    if (decoded != null) {
                        Image(
                            bitmap = decoded.asImageBitmap(),
                            contentDescription = board.styleLabel,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(340.dp)
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(board.imageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = board.styleLabel,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(340.dp)
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { handleSave(board, shareAfter = false) },
                            enabled = !isSaving
                        ) {
                            Icon(Icons.Filled.Save, contentDescription = "저장")
                        }
                        IconButton(
                            onClick = { handleSave(board, shareAfter = true) },
                            enabled = !isSaving
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = "공유")
                        }
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }
                    statusMessage?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    errorMessage?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    TextButton(
                        onClick = { selected = null },
                        enabled = !isSaving,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("닫기")
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
