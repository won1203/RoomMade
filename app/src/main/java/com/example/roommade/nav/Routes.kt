package com.example.roommade.nav

sealed class Route(val path: String) {
    data object Start : Route("start")
    data object RoomCategory : Route("room_category")
    data object Concept : Route("concept")
    data object ConceptAnalyzing : Route("concept_analyzing")
    data object ConceptResult : Route("concept_result")
    data object ShoppingWeb : Route("shopping_web")
    data object ExampleRoom : Route("example_room")
    data object AiImage : Route("ai_image")
}
