package com.example.roommade.domain

import com.example.roommade.model.FloorPlan
import com.example.roommade.model.RoomCategory
import com.example.roommade.network.ReplicateClient
import com.example.roommade.util.toBase64PngDataUri

class GenerateRoomImageUseCase(
    private val replicateClient: ReplicateClient
) {
    suspend operator fun invoke(
        plan: FloorPlan,
        concept: String,
        styleTags: Set<String>,
        roomCategory: RoomCategory,
        baseRoomImage: String? = null
    ): String {
        val prompt = GenerationPromptBuilder.build(concept, styleTags, roomCategory, plan.furnitures)
        // 선택된 빈 방 이미지(예시)와 프롬프트를 조합해 img2img로 생성. 없으면 텍스트만 사용.
        return replicateClient.generate(
            prompt = prompt,
            image = baseRoomImage
        )
    }
}
