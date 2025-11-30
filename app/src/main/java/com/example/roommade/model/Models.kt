package com.example.roommade.model

import android.graphics.RectF

enum class OpeningType { DOOR, WINDOW }
enum class FurnCategory { BED, DESK, SOFA, WARDROBE, TABLE, CHAIR, LIGHTING, RUG, OTHER }

enum class FurnOrigin { INVENTORY, CATALOG, MANUAL }

data class Furniture(
    val category: FurnCategory,
    val rect: RectF,
    val origin: FurnOrigin = FurnOrigin.MANUAL,
    val tag: String? = null
)

data class Opening(val type: OpeningType, val rect: RectF)

data class FloorPlan(
    val bounds: RectF = RectF(0f, 0f, 600f, 400f),
    val doors: List<Opening> = emptyList(),
    val windows: List<Opening> = emptyList(),
    val furnitures: List<Furniture> = emptyList(),
    val scaleMmPerPx: Float = 2.0f // mm/px
)

data class RoomSpec(
    val areaPyeong: Float = 6f,
    val aspect: Float = 1.2f
) {
    val areaM2: Float get() = areaPyeong * 3.305785f
    val widthM: Float get() = kotlin.math.sqrt(areaM2 * aspect)
    val heightM: Float get() = widthM / aspect
    val widthMm: Float get() = widthM * 1000f
    val heightMm: Float get() = heightM * 1000f
}

data class Recommendation(
    val id: String,
    val title: String,
    val rationale: String,
    val plan: FloorPlan
)

data class ShopLink(val vendor: String, val url: String)

data class CatalogItem(
    val id: String,
    val name: String,
    val category: FurnCategory,
    val styleTags: Set<String>,
    val defaultWidthMm: Int,
    val defaultHeightMm: Int,
    val priceKRW: Int,
    val shopLinks: List<ShopLink> = emptyList()
)

data class StyleCatalog(val items: List<CatalogItem>)
