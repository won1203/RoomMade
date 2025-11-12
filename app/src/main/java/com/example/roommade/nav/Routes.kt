package com.example.roommade.nav

sealed class Route(val path: String) {
    data object Start : Route("start")
    data object StructureArea : Route("structure_area")
    data object StructureInventory : Route("structure_inventory")
    data object StructureLayout : Route("structure_layout")
    data object Concept : Route("concept")
    data object ConceptAnalyzing : Route("concept_analyzing")
    data object ConceptResult : Route("concept_result")
    data object Catalog : Route("catalog")
    data object Plans : Route("plans")
    data object Result : Route("result")
    data object Shopping : Route("shopping")
}

