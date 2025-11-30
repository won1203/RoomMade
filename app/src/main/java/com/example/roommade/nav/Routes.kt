package com.example.roommade.nav

sealed class Route(val path: String) {
    data object Start : Route("start")
    data object RoomCategory : Route("room_category")
    data object StructureArea : Route("structure_area")
    data object StructureInventory : Route("structure_inventory")
    data object StructureLayout : Route("structure_layout")
    data object Concept : Route("concept")
    data object ConceptAnalyzing : Route("concept_analyzing")
    data object ConceptResult : Route("concept_result")
    data object Plans : Route("plans")
    data object Result : Route("result")
    data object ShoppingWeb : Route("shopping_web")
    data object Placement : Route("placement")
    data object AiImage : Route("ai_image")
}
