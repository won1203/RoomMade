package com.example.roommade.domain

import com.example.roommade.model.Furniture
import com.example.roommade.model.RoomCategory
import com.example.roommade.model.enLabel

/**
 * Single responsibility: build the textual prompt for Replicate.
 */
object GenerationPromptBuilder {
    fun build(
        concept: String,
        styleTags: Set<String>,
        roomCategory: RoomCategory,
        furnitures: List<Furniture>
    ): String {
        val furnitureList = buildFurnitureList(furnitures)
        val tokens = buildList {
            addAll(styleTokens(styleTags))
            add(roomLabel(roomCategory))
            add("neutral palette")
            add("follow the sketch layout")
            add("match positions and sizes of $furnitureList")
            add("interior design")
            add("photorealistic")
            add("soft lighting")
            add("high quality")
        }
        return tokens.joinToString(", ")
    }

    private fun styleTokens(styleTags: Set<String>): List<String> =
        styleTags
            .filter { it.isNotBlank() }
            .map { it.trim() }

    private fun roomLabel(roomCategory: RoomCategory): String = when (roomCategory) {
        RoomCategory.MASTER_BEDROOM -> "master bedroom"
        RoomCategory.LIVING_ROOM -> "living room"
    }

    private fun buildFurnitureList(items: List<Furniture>): String {
        if (items.isEmpty()) return "no furniture"
        return items
            .groupBy { it.category }
            .entries
            .joinToString(", ") { (category, list) ->
                val label = category.enLabel()
                val count = list.size
                if (count > 1) "$count ${label.plural}" else label.singular
            }
    }
}
