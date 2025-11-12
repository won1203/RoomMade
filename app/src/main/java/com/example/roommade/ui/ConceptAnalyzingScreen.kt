package com.example.roommade.ui

import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.roommade.vm.AnalysisState
import com.example.roommade.vm.FloorPlanViewModel
import kotlinx.coroutines.delay

@Composable
fun ConceptAnalyzingScreen(
    onAnalyzed: () -> Unit,
    onCancel: () -> Unit,
    vm: FloorPlanViewModel
) {
    val analysisState = vm.analysisState
    val errorMessage = vm.analysisErrorMessage
    val minDisplayMillis = 800L
    val analysisStart = remember { mutableStateOf(SystemClock.elapsedRealtime()) }

    LaunchedEffect(analysisState) {
        when (analysisState) {
            AnalysisState.Running -> analysisStart.value = SystemClock.elapsedRealtime()
            AnalysisState.Success -> {
                val elapsed = SystemClock.elapsedRealtime() - analysisStart.value
                if (elapsed < minDisplayMillis) {
                    delay(minDisplayMillis - elapsed)
                }
                vm.markAnalysisHandled()
                onAnalyzed()
            }
            else -> Unit
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (analysisState) {
            AnalysisState.Running, AnalysisState.Idle -> {
                CircularProgressIndicator()
                Text(
                    text = "감성 분석 중입니다.",
                    style = MaterialTheme.typography.titleMedium
                )
                OutlinedButton(onClick = {
                    vm.cancelAnalysis()
                    onCancel()
                }) {
                    Text("취소")
                }
            }
            AnalysisState.Failed -> {
                Text(
                    text = "감성 분석에 실패했습니다.",
                    style = MaterialTheme.typography.titleMedium
                )
                if (!errorMessage.isNullOrBlank()) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        vm.cancelAnalysis()
                        onCancel()
                    }
                ) {
                    Text("다시 입력")
                }
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        vm.markAnalysisHandled()
                        onAnalyzed()
                    }
                ) {
                    Text("기본 추천 보기")
                }
            }
            AnalysisState.Success -> {
                CircularProgressIndicator()
                Text(
                    text = "결과를 준비 중입니다...",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}
