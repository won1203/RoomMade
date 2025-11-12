package com.example.roommade.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.roommade.ml.StyleAnalyzer
import com.example.roommade.vm.FloorPlanViewModel
import kotlin.math.roundToInt

@Composable
fun ConceptInputScreen(
    onNext: () -> Unit,
    onBack: () -> Unit,
    vm: FloorPlanViewModel
) {
    var concept by remember { mutableStateOf(vm.conceptText) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("원하시는 감성을 입력해주세요", style = MaterialTheme.typography.headlineSmall)

        OutlinedTextField(
            value = concept,
            onValueChange = { concept = it },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            placeholder = { Text("Ex: 깔끔하고 세련된 느낌") },
            minLines = 4
        )


        when {
            vm.isAnalyzingConcept -> {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            vm.styleProbabilities.isNotEmpty() -> {
                StylePredictionRow(scores = vm.styleProbabilities)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onBack) { Text("이전") }
            Button(
                onClick = {
                    vm.analyzeConcept(concept)
                    onNext()
                },
                enabled = concept.isNotBlank()
            ) { Text("감성 분석") }
        }
    }
}

@Composable
private fun StylePredictionRow(scores: List<StyleAnalyzer.StyleProbability>) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("AI 감성 예측", style = MaterialTheme.typography.titleMedium)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(scores) { score -> StyleProbabilityChip(score) }
        }
    }
}

@Composable
private fun StyleProbabilityChip(score: StyleAnalyzer.StyleProbability) {
    val percent = (score.probability * 100f).roundToInt().coerceIn(0, 100)
    Surface(shape = MaterialTheme.shapes.small, tonalElevation = 3.dp) {
        Text(
            text = "${score.label} ${percent}%",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
