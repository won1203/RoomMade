package com.example.roommade.domain

import com.example.roommade.model.FloorPlan
import com.example.roommade.model.RoomCategory
import com.example.roommade.network.FirebaseImageGenClient

class GenerateRoomImageUseCase(
    private val backendClient: FirebaseImageGenClient
) {
    suspend operator fun invoke(
        plan: FloorPlan,
        concept: String,
        styleTags: Set<String>,
        roomCategory: RoomCategory,
        baseRoomImage: String? = null
    ): String {
        val prompts = GenerationPromptBuilder.build(concept, styleTags, roomCategory, plan.furnitures)
        return backendClient.generateImage(
            prompt = prompts.positive,
            negativePrompt = prompts.negative,
            baseImageDataUri = baseRoomImage
        )
    }
}
