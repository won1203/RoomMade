package com.example.roommade.ui

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.roommade.vm.AiImageUiState
import com.example.roommade.vm.AiImageViewModel
import com.example.roommade.vm.FloorPlanViewModel

@Composable
fun AiImageScreen(
    vm: FloorPlanViewModel,
    onBack: () -> Unit
) {
    val aiVm: AiImageViewModel = viewModel(factory = AiImageViewModel.provideFactory())
    val uiState = aiVm.uiState
    val context = LocalContext.current
    val concept = vm.conceptText
    val styles = vm.styleTags
    val roomCategory = vm.roomCategory
    val baseRoomImage = vm.selectedBaseRoomImage

    var lastStoredUrl by remember { mutableStateOf<String?>(null) }
    var inputError by remember { mutableStateOf<String?>(null) }
    var imageLoadError by remember { mutableStateOf<String?>(null) }
    var lastRequestKey by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(vm.floorPlan, concept, styles, roomCategory, baseRoomImage) {
        if (baseRoomImage.isNullOrBlank()) {
            inputError = "예시 이미지를 먼저 선택해주세요"
            return@LaunchedEffect
        }
        val requestKey =
            listOf(vm.floorPlan, concept, styles.joinToString(","), roomCategory, baseRoomImage)
                .joinToString("|")
        if (lastRequestKey != requestKey) {
            lastRequestKey = requestKey
            inputError = null
            vm.syncCartFurniture()
            val latestPlan = vm.floorPlan
            aiVm.generate(
                plan = latestPlan,
                concept = concept,
                styleTags = styles,
                roomCategory = roomCategory,
                baseRoomImage = baseRoomImage
            )
        }
    }

    when (uiState) {
        AiImageUiState.Generating -> {
            val infiniteTransition = rememberInfiniteTransition(label = "ai_generating")
            val textAlpha by infiniteTransition.animateFloat(
                initialValue = 0.35f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "ai_generating_alpha"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "AI 이미지 생성 중...",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.alpha(textAlpha)
                    )
                    CircularProgressIndicator(modifier = Modifier.size(56.dp))
                }
            }
            return
        }

        else -> Unit
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("AI 이미지 생성", style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = "선택한 예시와 감성 프롬프트를 조합해 AI 이미지를 생성합니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        when (val state = uiState) {
            AiImageUiState.Idle -> {
                Text("생성 대기 중입니다.", style = MaterialTheme.typography.bodyMedium)
            }

            AiImageUiState.Generating -> Unit

            is AiImageUiState.Success -> {
                if (state.imageUrl != lastStoredUrl) {
                    vm.saveGeneratedBoard(state.imageUrl)
                    lastStoredUrl = state.imageUrl
                }

                val decodedBitmap = remember(state.imageUrl) { decodeDataUri(state.imageUrl) }
                if (decodedBitmap != null) {
                    imageLoadError = null
                    Image(
                        bitmap = decodedBitmap.asImageBitmap(),
                        contentDescription = "생성된 AI 이미지",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(420.dp),
                        alignment = Alignment.Center
                    )
                } else {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(state.imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "생성된 AI 이미지",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(420.dp),
                        alignment = Alignment.Center,
                        onSuccess = { imageLoadError = null },
                        onError = { imageLoadError = it.result.throwable.message }
                    )
                }

                imageLoadError?.let { err ->
                    Text(
                        text = "이미지 로드 실패: ${err.take(120)}",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                ActionRow(
                    isSaving = aiVm.isSaving,
                    canGenerate = !baseRoomImage.isNullOrBlank(),
                    onRegenerate = {
                        if (baseRoomImage.isNullOrBlank()) {
                            inputError = "예시 이미지를 먼저 선택해주세요"
                        } else {
                            inputError = null
                            vm.syncCartFurniture()
                            val latestPlan = vm.floorPlan
                            aiVm.generate(
                                plan = latestPlan,
                                concept = concept,
                                styleTags = styles,
                                roomCategory = roomCategory,
                                baseRoomImage = baseRoomImage
                            )
                        }
                    },
                    onSave = { aiVm.saveToGallery(context) },
                    onShare = { aiVm.shareOrSave(context) }
                )
            }

            is AiImageUiState.Error -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    OutlinedButton(onClick = { aiVm.clearError() }) {
                        Text("다시 시도")
                    }
                }
            }
        }

        inputError?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

private fun decodeDataUri(data: String?): android.graphics.Bitmap? {
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

@Composable
private fun ActionRow(
    isSaving: Boolean,
    canGenerate: Boolean,
    onRegenerate: () -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ActionIconButton(
            label = "다시 생성",
            enabled = !isSaving && canGenerate,
            onClick = onRegenerate
        ) { Icon(Icons.Filled.Refresh, contentDescription = "다시 생성") }

        ActionIconButton(
            label = if (isSaving) "저장 중..." else "저장",
            enabled = !isSaving,
            onClick = onSave
        ) { Icon(Icons.Filled.Save, contentDescription = "저장") }

        ActionIconButton(
            label = "공유",
            enabled = !isSaving,
            onClick = onShare
        ) { Icon(Icons.Filled.Share, contentDescription = "공유") }
    }
}

@Composable
private fun ActionIconButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Button(
            onClick = onClick,
            enabled = enabled,
            shape = CircleShape,
            contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.buttonColors(),
            modifier = Modifier.size(68.dp)
        ) {
            content()
        }
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}
