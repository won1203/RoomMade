package com.example.roommade.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.roommade.ml.StyleAnalyzer
import com.example.roommade.vm.FloorPlanViewModel
import kotlin.math.roundToInt

@Composable
fun ConceptResultScreen(
    onSearch: () -> Unit,
    onBack: () -> Unit,
    vm: FloorPlanViewModel
) {
    val probabilities = vm.styleProbabilities
    val conceptText = vm.conceptText.ifBlank { "입력한 감성 문장 없음" }
    val topResult = probabilities.firstOrNull()
    val topLabel = topResult?.label ?: vm.styleTags.firstOrNull() ?: "기본 스타일"
    val secondary = probabilities.drop(1).take(4)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("감성 분석 결과", style = MaterialTheme.typography.labelLarge)
            Text(
                text = "\"$conceptText\"",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 3.dp,
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("주요 감성", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = topLabel,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold)
                )
                if (topResult != null) {
                    val percent = (topResult.probability * 100f).roundToInt()
                    Text(
                        text = "모델 신뢰도 $percent%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (secondary.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("다른 후보 스타일", style = MaterialTheme.typography.titleMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(secondary) { score ->
                        StyleProbabilityChip(score)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onSearch
            ) {
                Text("추천 가구 검색")
            }
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onBack
            ) {
                Text("감성 다시 입력")
            }
        }
    }
}

@Composable
private fun StyleProbabilityChip(score: StyleAnalyzer.StyleProbability) {
    val percent = (score.probability * 100f).roundToInt().coerceIn(0, 100)
    Surface(
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(score.label, style = MaterialTheme.typography.bodyMedium)
            Text(
                "$percent%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
