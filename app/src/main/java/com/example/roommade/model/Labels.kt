package com.example.roommade.model

fun FurnCategory.korLabel(): String = when (this) {
    FurnCategory.BED      -> "침대"
    FurnCategory.DESK     -> "책상"
    FurnCategory.SOFA     -> "소파"
    FurnCategory.WARDROBE -> "옷장"
    FurnCategory.TABLE    -> "테이블"
    FurnCategory.CHAIR    -> "의자"
    FurnCategory.LIGHTING -> "조명"
    FurnCategory.RUG      -> "러그"
    FurnCategory.OTHER    -> "기타"
}
