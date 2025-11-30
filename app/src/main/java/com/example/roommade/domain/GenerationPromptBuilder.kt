package com.example.roommade.domain

import com.example.roommade.model.FurnCategory
import com.example.roommade.model.Furniture
import com.example.roommade.model.RoomCategory

/**
 * 단일 책임: 프롬프트 생성. 다른 곳에서 문자열을 조합하지 않는다.
 */
object GenerationPromptBuilder {
    fun build(
        concept: String,
        styleTags: Set<String>,
        roomCategory: RoomCategory,
        furnitures: List<Furniture>
    ): String {
        val styles = if (styleTags.isEmpty()) "minimal" else styleTags.joinToString(", ")
        val furnitureSummary = summarizeFurniture(furnitures)
        val base = "A photorealistic ${roomCategory.name.lowercase()} interior, $styles style, with $furnitureSummary."
        return if (concept.isBlank()) base else "$base Details: ${concept.trim()}"
    }

    private fun summarizeFurniture(items: List<Furniture>): String {
        if (items.isEmpty()) return "no furniture placed"
        return items
            .groupBy { it.category }
            .entries
            .joinToString { (category, list) ->
                "${list.size} ${categoryLabel(category)}"
            }
    }

    private fun categoryLabel(category: FurnCategory): String = when (category) {
        FurnCategory.BED -> "beds"
        FurnCategory.DESK -> "desks"
        FurnCategory.SOFA -> "sofas"
        FurnCategory.WARDROBE -> "wardrobes"
        FurnCategory.TABLE -> "tables"
        FurnCategory.CHAIR -> "chairs"
        FurnCategory.LIGHTING -> "lights"
        FurnCategory.RUG -> "rugs"
        FurnCategory.OTHER -> "furniture"
    }
}
