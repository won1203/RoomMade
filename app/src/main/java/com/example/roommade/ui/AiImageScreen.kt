package com.example.roommade.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
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
    val plan = vm.floorPlan
    val concept = vm.conceptText
    val styles = vm.styleTags
    val roomCategory = vm.roomCategory

    // 화면 진입 시 자동으로 한 번 생성 시도
    LaunchedEffect(plan, concept, styles, roomCategory) {
        if (uiState is AiImageUiState.Idle) {
            aiVm.generate(plan, concept, styles, roomCategory)
        }
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
                    text = "완성된 배치도를 기반으로 ControlNet-Seg 모델로 방 이미지를 생성합니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(onClick = onBack) {
                Text("뒤로")
            }
        }

        when (val state = uiState) {
            AiImageUiState.Idle -> {
                Text(
                    text = "배치도를 전송해 이미지를 생성하세요.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            AiImageUiState.Generating -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator()
                    Text("이미지 생성 중...", style = MaterialTheme.typography.bodyMedium)
                }
            }

            is AiImageUiState.Success -> {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp),
                    tonalElevation = 4.dp
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        AsyncImage(
                            model = state.imageUrl,
                            contentDescription = "생성된 방 이미지"
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { aiVm.saveToGallery(context) },
                        enabled = !aiVm.isSaving
                    ) {
                        Text(if (aiVm.isSaving) "저장 중..." else "갤러리에 저장")
                    }
                    Button(
                        onClick = { aiVm.share(context) },
                        enabled = aiVm.latestSavedUri != null
                    ) {
                        Text("공유")
                    }
                }
                Text(
                    text = "공유는 저장 후 가능합니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        Text("메시지 닫기")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f)
            ) { Text("뒤로") }
            Button(
                onClick = {
                    aiVm.generate(
                        plan = plan,
                        concept = concept,
                        styleTags = styles,
                        roomCategory = roomCategory
                    )
                },
                modifier = Modifier.weight(1f)
            ) { Text("다시 생성") }
        }
    }
}
