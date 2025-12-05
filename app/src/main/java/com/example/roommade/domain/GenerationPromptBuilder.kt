package com.example.roommade.domain

import com.example.roommade.model.Furniture
import com.example.roommade.model.RoomCategory
import com.example.roommade.model.enLabel

data class GenerationPrompts(
    val positive: String,
    val negative: String
)

/**
 * Single responsibility: build the textual prompts (positive/negative) for image generation.
 */
object GenerationPromptBuilder {
    fun build(
        concept: String,
        styleTags: Set<String>,
        roomCategory: RoomCategory,
        furnitures: List<Furniture>
    ): GenerationPrompts {
        val furnitureLine = buildFurnitureLine(furnitures)
        val styleLine = buildStyleLine(concept, styleTags)

        val positive = buildList {
            add("professional interior render of a ${roomLabel(roomCategory)}")
            if (styleLine.isNotBlank()) add("in $styleLine style")
            add("follow the provided hough/sketch lines exactly for layout and perspective")
            add("even if reference is empty, render all listed furniture")
            add("place every furniture item with correct count, position, and scale: $furnitureLine")
            add("do not add furniture not listed")
            add("accurate proportions, realistic materials, soft natural light")
            add("neutral palette when appropriate")
            add("best quality, extremely detailed, photorealistic, high resolution")
        }.joinToString(", ")

        val negative = NEGATIVE_TOKENS.joinToString(", ")

        return GenerationPrompts(
            positive = positive,
            negative = negative
        )
    }

    private fun buildStyleLine(concept: String, styleTags: Set<String>): String =
        (listOf(concept) + styleTags)
            .mapNotNull { it.trim().takeIf { token -> token.isNotBlank() } }
            .distinct()
            .joinToString(", ")

    private fun roomLabel(roomCategory: RoomCategory): String = when (roomCategory) {
        RoomCategory.MASTER_BEDROOM -> "master bedroom"
        RoomCategory.LIVING_ROOM -> "living room"
    }

    private fun buildFurnitureLine(items: List<Furniture>): String {
        if (items.isEmpty()) return "no furniture"
        return items
            .groupBy { it.category }
            .entries
            .joinToString(", ") { (category, list) ->
                val label = category.enLabel()
                val count = list.size
                val tagHint = list
                    .asSequence()
                    .mapNotNull { it.tag?.trim()?.takeIf(String::isNotBlank) }
                    .firstOrNull()
                    ?.let { " ($it)" }
                    .orEmpty()
                if (count > 1) "$count ${label.plural}$tagHint" else "${label.singular}$tagHint"
            }
    }

    private val NEGATIVE_TOKENS = listOf(
        "extra furniture",
        "missing furniture",
        "empty room",
        "misplaced furniture",
        "duplicate furniture",
        "wrong scale",
        "warped geometry",
        "distorted perspective",
        "cluttered layout",
        "lowres",
        "blurry",
        "noisy",
        "watermark",
        "text",
        "logo",
        "cropped"
    )
}
