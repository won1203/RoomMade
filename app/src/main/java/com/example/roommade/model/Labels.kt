package com.example.roommade.model

data class EnFurnitureLabel(val singular: String, val plural: String)

fun FurnCategory.korLabel(): String = when (this) {
    FurnCategory.BED -> "침대"
    FurnCategory.DESK -> "책상"
    FurnCategory.SOFA -> "소파"
    FurnCategory.WARDROBE -> "옷장"
    FurnCategory.TABLE -> "테이블"
    FurnCategory.CHAIR -> "의자"
    FurnCategory.LIGHTING -> "조명"
    FurnCategory.RUG -> "러그"
    FurnCategory.OTHER -> "기타"
}

fun FurnCategory.enLabel(): EnFurnitureLabel = when (this) {
    FurnCategory.BED -> EnFurnitureLabel("bed", "beds")
    FurnCategory.DESK -> EnFurnitureLabel("desk", "desks")
    FurnCategory.SOFA -> EnFurnitureLabel("sofa", "sofas")
    FurnCategory.WARDROBE -> EnFurnitureLabel("wardrobe", "wardrobes")
    FurnCategory.TABLE -> EnFurnitureLabel("table", "tables")
    FurnCategory.CHAIR -> EnFurnitureLabel("chair", "chairs")
    FurnCategory.LIGHTING -> EnFurnitureLabel("light", "lights")
    FurnCategory.RUG -> EnFurnitureLabel("rug", "rugs")
    FurnCategory.OTHER -> EnFurnitureLabel("furniture", "furniture")
}
