package com.example.roommade.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.roommade.model.RoomCategory
import com.example.roommade.model.defaults
import com.example.roommade.model.korLabel
import com.example.roommade.vm.FloorPlanViewModel

@Composable
fun RoomCategoryScreen(
    onNext: () -> Unit,
    onBack: () -> Unit,
    vm: FloorPlanViewModel
) {
    // 현재는 주방을 제외한 2개 카테고리만 노출
    val categories = listOf(RoomCategory.MASTER_BEDROOM, RoomCategory.LIVING_ROOM)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("방 카테고리 선택", style = MaterialTheme.typography.headlineSmall)
        Text(
            text = "안방, 거실, 주방 중 공간을 먼저 선택해 주세요. 선택 시 해당 공간에 맞춘 기본 평수와 가로세로 비율이 적용됩니다.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            categories.forEach { category ->
                val selected = vm.roomCategory == category
                val defaults = category.defaults()
                val containerColor = if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
                val border = if (selected) {
                    BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                } else {
                    BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { vm.selectRoomCategory(category) },
                    color = containerColor,
                    tonalElevation = if (selected) 2.dp else 0.dp,
                    shape = RoundedCornerShape(14.dp),
                    border = border
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = category.korLabel(),
                            style = MaterialTheme.typography.titleMedium,
                            color = if (selected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                        Text(
                            text = "기본 평수 ${"%.1f".format(defaults.areaPyeong)} · 비율 ${"%.2f".format(defaults.aspect)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (selected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                Text("이전")
            }
            Button(onClick = onNext, modifier = Modifier.weight(1f)) {
                Text("다음")
            }
        }
    }
}
