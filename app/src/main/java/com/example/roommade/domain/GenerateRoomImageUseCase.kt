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
        roomCategory: RoomCategory
    ): String {
        val controlBitmap = FloorPlanRasterizer.toSegBitmap(plan)
        val controlImage = controlBitmap.toBase64PngDataUri()
        val prompt = GenerationPromptBuilder.build(concept, styleTags, roomCategory, plan.furnitures)
        return replicateClient.generate(prompt = prompt, controlImage = controlImage)
    }
}
