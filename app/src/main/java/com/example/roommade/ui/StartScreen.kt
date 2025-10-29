package com.example.roommade.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun StartScreen(
    onStartManual: () -> Unit  // 권장: 가이드대로 시작 (RoomSize)
) {
    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 타이틀
            Text(
                text = "RoomMade",
                style = MaterialTheme.typography.headlineLarge
            )
            Spacer(Modifier.height(8.dp))
            // 서브타이틀
            Text(
                text = "방 크기 입력부터 스타일 추천까지\n한 번에 완성하는 인테리어 플래너",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))

            // 권장 시작(메인 플로우)
            Button(
                onClick = onStartManual,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text("🚀 가이드대로 시작 (권장)")
            }

            Spacer(Modifier.height(24.dp))

            // 작은 도움말
            Text(
                text = "가이드 시작은 평수·비율 → 보유 가구 → 평면도 편집 → 스타일 → 추천 순서로 진행돼요.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        // 하단 푸터(선택)
        Text(
            text = "v0.2 • Kotlin + Compose",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }
}
