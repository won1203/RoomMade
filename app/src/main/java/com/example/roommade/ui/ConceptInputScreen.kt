package com.example.roommade.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.roommade.vm.FloorPlanViewModel

@Composable
fun ConceptInputScreen(
    onNext: () -> Unit,
    onBack: () -> Unit,
    vm: FloorPlanViewModel
) {
    var concept by remember { mutableStateOf("") }

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

        if (vm.isAnalyzingConcept) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        Button(
            onClick = {
                vm.analyzeConcept(concept)
                onNext()
            },
            enabled = concept.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) { Text("감성 분석") }
    }
}
