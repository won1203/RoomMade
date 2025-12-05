package com.example.roommade.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.roommade.R
import com.example.roommade.model.RoomCategory
import com.example.roommade.model.korLabel
import com.example.roommade.util.toBase64PngDataUri
import com.example.roommade.vm.FloorPlanViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ExampleRoomSelectionScreen(
    vm: FloorPlanViewModel,
    onBack: () -> Unit,
    onGenerate: () -> Unit
) {
    val category = vm.roomCategory
    val context = LocalContext.current
    val examples = remember(category) { ExampleRoomPresets.forCategory(context, category) }
    var selectedId by remember(examples) { mutableStateOf<String?>(null) }
    val selected = remember(selectedId, examples) { examples.firstOrNull { it.id == selectedId } }
    var prepareError by remember { mutableStateOf<String?>(null) }
    var isPreparing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // 선택을 해제했을 때만 즉시 클리어 (선택 후 화면 이동 시 취소되는 문제 방지)
    LaunchedEffect(selectedId) {
        if (selected == null) {
            vm.selectBaseRoomImage(null)
        }
    }

    LaunchedEffect(Unit) {
        vm.syncCartFurniture()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            Button(
                onClick = {
                    val target = selected ?: return@Button
                    scope.launch {
                        isPreparing = true
                        prepareError = null
                        try {
                            val dataUri = withContext(Dispatchers.IO) {
                                BitmapFactory.decodeResource(context.resources, target.resId)
                                    ?.toBase64PngDataUri()
                                    ?: throw IllegalStateException("예시 이미지를 불러오지 못했습니다.")
                            }
                            vm.selectBaseRoomImage(dataUri)
                            onGenerate()
                        } catch (t: Throwable) {
                            prepareError = t.message ?: "예시 이미지를 준비하는 중 오류가 발생했습니다."
                        } finally {
                            isPreparing = false
                        }
                    }
                },
                enabled = selectedId != null && !isPreparing,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(if (isPreparing) "준비 중..." else "AI 이미지 생성")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("샘플 방 이미지", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "${category.korLabel()}에 맞는 샘플 이미지를 고른 뒤 감성 프롬프트와 함께 AI 이미지를 생성합니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .border(
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        RoundedCornerShape(16.dp)
                    )
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                if (selected != null) {
                    AsyncImage(
                        model = selected.resId,
                        contentDescription = selected.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp)),
                        alignment = Alignment.Center,
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("예시 이미지를 선택해주세요", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "선택한 예시 이미지가 함께 보내져야 합니다.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Text("예시 이미지", style = MaterialTheme.typography.titleMedium)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(examples, key = { it.id }) { room ->
                    val selectedCard = room.id == selectedId
                    Card(
                        modifier = Modifier
                            .width(140.dp)
                            .height(110.dp)
                            .clickable { selectedId = room.id },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (selectedCard) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedCard) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        )
                    ) {
                        AsyncImage(
                            model = room.resId,
                            contentDescription = room.title,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                                .clip(RoundedCornerShape(10.dp)),
                            alignment = Alignment.Center,
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            prepareError?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

private data class ExampleRoom(
    val id: String,
    val title: String,
    val description: String,
    val resId: Int
)

private object ExampleRoomPresets {
    fun forCategory(context: android.content.Context, category: RoomCategory): List<ExampleRoom> {
        val (resIds, titlePrefix) = when (category) {
            RoomCategory.MASTER_BEDROOM -> bedroomRes to "안방 샘플"
            RoomCategory.LIVING_ROOM -> livingRes to "거실 샘플"
        }
        return resIds.mapIndexed { idx, resId ->
            val label = "$titlePrefix ${idx + 1}"
            ExampleRoom(
                id = "${category.name}_$idx",
                title = label,
                description = "$label 이미지",
                resId = resId
            )
        }
    }

    private val bedroomRes = listOf(
        R.drawable.example_bedroom_1,
        R.drawable.example_bedroom_2,
        R.drawable.example_bedroom_3,
        R.drawable.example_bedroom_4,
        R.drawable.example_bedroom_5
    )

    private val livingRes = listOf(
        R.drawable.example_living_1,
        R.drawable.example_living_2,
        R.drawable.example_living_3
    )
}
