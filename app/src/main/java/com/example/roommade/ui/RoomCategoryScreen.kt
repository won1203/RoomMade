package com.example.roommade.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.roommade.R
import com.example.roommade.model.RoomCategory
import com.example.roommade.model.korLabel
import com.example.roommade.vm.FloorPlanViewModel

@Composable
fun RoomCategoryScreen(
    onNext: () -> Unit,
    onBack: () -> Unit,
    vm: FloorPlanViewModel
) {
    val categories = listOf(RoomCategory.MASTER_BEDROOM, RoomCategory.LIVING_ROOM)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("방 카테고리 선택", style = MaterialTheme.typography.headlineSmall)
        Text(
            text = "원하는 공간을 선택해 바로 다음 단계로 이동하세요.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            categories.forEach { category ->
                val selected = vm.roomCategory == category
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
                val imageRes = when (category) {
                    RoomCategory.MASTER_BEDROOM -> R.drawable.example_bedroom_1
                    RoomCategory.LIVING_ROOM -> R.drawable.example_living_1
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            vm.selectRoomCategory(category)
                            onNext()
                        },
                    color = containerColor,
                    tonalElevation = if (selected) 2.dp else 0.dp,
                    shape = RoundedCornerShape(18.dp),
                    border = border
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Image(
                            painter = painterResource(id = imageRes),
                            contentDescription = category.korLabel(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentScale = ContentScale.Crop
                        )
                        Text(
                            text = category.korLabel(),
                            style = MaterialTheme.typography.titleMedium,
                            color = if (selected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }
            }
        }
    }
}
