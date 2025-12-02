package com.example.roommade.model

data class RoomCategoryDefaults(
    val areaPyeong: Float,
    val aspect: Float
)

enum class RoomCategory {
    MASTER_BEDROOM,
    LIVING_ROOM
}

fun RoomCategory.korLabel(): String = when (this) {
    RoomCategory.MASTER_BEDROOM -> "안방"
    RoomCategory.LIVING_ROOM -> "거실"
}

fun RoomCategory.defaults(): RoomCategoryDefaults = when (this) {
    RoomCategory.MASTER_BEDROOM -> RoomCategoryDefaults(areaPyeong = 6f, aspect = 1.2f)
    RoomCategory.LIVING_ROOM -> RoomCategoryDefaults(areaPyeong = 12f, aspect = 1.3f)
}
