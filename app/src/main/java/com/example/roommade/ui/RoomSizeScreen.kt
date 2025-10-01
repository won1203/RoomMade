package com.example.roommade.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.roommade.vm.FloorPlanViewModel

@Composable
fun RoomSizeScreen(onNext: () -> Unit, vm: FloorPlanViewModel) {
    var pyeong by remember { mutableStateOf(vm.roomSpec.areaPyeong) }
    var aspect by remember { mutableStateOf(vm.roomSpec.aspect) }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("방 크기 입력", style = MaterialTheme.typography.headlineSmall)
        Text("평수: ${"%.1f".format(pyeong)} 평")
        Slider(value = pyeong, onValueChange = { pyeong = it }, valueRange = 2f..40f)
        Text("가로:세로 비율: ${"%.2f".format(aspect)}")
        Slider(value = aspect, onValueChange = { aspect = it }, valueRange = 0.5f..2.0f)
        Spacer(Modifier.height(8.dp))
        Button(onClick = {
            vm.setRoomAreaPyeong(pyeong)
            vm.setRoomAspect(aspect)
            onNext()
        }) { Text("다음") }
    }
}
