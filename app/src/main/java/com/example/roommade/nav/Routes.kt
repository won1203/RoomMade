package com.example.roommade.nav

sealed class Route(val path: String) {
    data object Start : Route("start")
    data object RoomSize : Route("room_size")
    data object Inventory : Route("inventory")
    data object Edit : Route("edit")
    data object Style : Route("style")
    data object StyleCatalog : Route("style_catalog")
    data object Recommend : Route("recommend")
    data object Result : Route("result")
    data object Shopping : Route("shopping")
}

