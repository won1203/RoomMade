package com.example.roommade.vm

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.roommade.ml.StyleAnalyzer
import com.example.roommade.ml.StyleAnalyzer.StyleProbability
import com.example.roommade.ml.StyleAnalyzerProvider
import com.example.roommade.model.CatalogItem
import com.example.roommade.model.FloorPlan
import com.example.roommade.model.FurnCategory
import com.example.roommade.model.FurnOrigin
import com.example.roommade.model.Furniture
import com.example.roommade.model.Opening
import com.example.roommade.model.OpeningType
import com.example.roommade.model.Recommendation
import com.example.roommade.model.RoomSpec
import com.example.roommade.model.ShopLink
import com.example.roommade.model.StyleCatalog
import kotlin.math.max
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

enum class AnalysisState { Idle, Running, Success, Failed }

class FloorPlanViewModel(
    private val styleAnalyzer: StyleAnalyzer? = null
) : ViewModel() {

    var roomSpec by mutableStateOf(RoomSpec())
        private set

    var floorPlan by mutableStateOf(FloorPlan())
        private set

    var selectedFurnitureIndex by mutableStateOf<Int?>(null)
        private set

    var conceptText by mutableStateOf("")
        private set

    var styleTags by mutableStateOf(setOf<String>())
        private set

    var styleProbabilities by mutableStateOf<List<StyleProbability>>(emptyList())
        private set

    var isAnalyzingConcept by mutableStateOf(false)
        private set

    var analysisState by mutableStateOf(AnalysisState.Idle)
        private set

    var analysisErrorMessage by mutableStateOf<String?>(null)
        private set

    var recommendedCatalog by mutableStateOf<List<CatalogItem>>(emptyList())
        private set

    var chosenCatalog by mutableStateOf<Set<String>>(emptySet())
        private set

    var recommendations by mutableStateOf<List<Recommendation>>(emptyList())
        private set

    var selectedRec by mutableStateOf<Recommendation?>(null)
        private set

    var beforePlan by mutableStateOf<FloorPlan?>(null)
        private set

    var inventory by mutableStateOf<Map<FurnCategory, Int>>(emptyMap())
        private set

    private var analyzeJob: Job? = null

    // ---------------------------------------------------------------------
    // Room specification and base layout
    // ---------------------------------------------------------------------

    fun prepareStructure(
        areaPyeong: Float,
        aspectRatio: Float,
        inventoryCounts: Map<FurnCategory, Int>
    ) {
        val clampedArea = areaPyeong.coerceIn(2f, 80f)
        val clampedAspect = aspectRatio.coerceIn(0.5f, 2.0f)
        roomSpec = roomSpec.copy(areaPyeong = clampedArea, aspect = clampedAspect)
        inventory = inventoryCounts.filterValues { it > 0 }

        floorPlan = buildBaseFloorPlan()
        floorPlan = ensureDefaultOpenings(floorPlan)
        spawnInventoryToPlan(resetPrevious = true)

        beforePlan = null
        selectedRec = null
        recommendations = emptyList()
        analysisState = AnalysisState.Idle
        analysisErrorMessage = null
    }

    fun setRoomAreaPyeong(value: Float) {
        roomSpec = roomSpec.copy(areaPyeong = value.coerceIn(2f, 80f))
        floorPlan = floorPlan.copy(bounds = buildBounds(), scaleMmPerPx = computeMmPerPx())
    }

    fun setRoomAspect(value: Float) {
        roomSpec = roomSpec.copy(aspect = value.coerceIn(0.5f, 2.0f))
        floorPlan = floorPlan.copy(bounds = buildBounds(), scaleMmPerPx = computeMmPerPx())
    }

    fun setInventoryCounts(map: Map<FurnCategory, Int>) {
        inventory = map.filterValues { it > 0 }
    }

    private fun buildBaseFloorPlan(): FloorPlan {
        return FloorPlan(
            bounds = buildBounds(),
            scaleMmPerPx = computeMmPerPx()
        )
    }

    private fun buildBounds(): RectF {
        val mmPerPx = computeMmPerPx()
        val widthPx = roomSpec.widthMm / mmPerPx
        val heightPx = roomSpec.heightMm / mmPerPx
        return RectF(0f, 0f, widthPx, heightPx)
    }

    private fun computeMmPerPx(): Float {
        val targetWidthPx = 900f
        return (roomSpec.widthMm / targetWidthPx).coerceAtLeast(1f)
    }

    private fun ensureDefaultOpenings(plan: FloorPlan): FloorPlan {
        val bounds = plan.bounds
        val door = if (plan.doors.isNotEmpty()) plan.doors else listOf(
            Opening(
                OpeningType.DOOR,
                RectF(
                    bounds.left + 16f,
                    bounds.bottom - 76f,
                    bounds.left + 76f,
                    bounds.bottom - 16f
                )
            )
        )
        val window = if (plan.windows.isNotEmpty()) plan.windows else listOf(
            Opening(
                OpeningType.WINDOW,
                RectF(
                    bounds.centerX() - 60f,
                    bounds.top + 16f,
                    bounds.centerX() + 60f,
                    bounds.top + 46f
                )
            )
        )
        return plan.copy(doors = door, windows = window)
    }

    // ---------------------------------------------------------------------
    // Floor plan editing helpers
    // ---------------------------------------------------------------------

    fun addOpening(type: OpeningType, rect: RectF) {
        floorPlan = when (type) {
            OpeningType.DOOR -> floorPlan.copy(doors = floorPlan.doors + Opening(type, rect))
            OpeningType.WINDOW -> floorPlan.copy(windows = floorPlan.windows + Opening(type, rect))
        }
    }

    fun addFurniture(cat: FurnCategory, rect: RectF) {
        floorPlan = floorPlan.copy(
            furnitures = floorPlan.furnitures + Furniture(cat, rect)
        )
    }

    fun moveFurniture(index: Int, dx: Float, dy: Float) {
        val list = floorPlan.furnitures.toMutableList()
        val furniture = list.getOrNull(index) ?: return
        val bounds = floorPlan.bounds
        val rect = furniture.rect
        val width = rect.width()
        val height = rect.height()

        val left = (rect.left + dx).coerceIn(bounds.left, bounds.right - width)
        val top = (rect.top + dy).coerceIn(bounds.top, bounds.bottom - height)
        list[index] = furniture.copy(rect = RectF(left, top, left + width, top + height))
        floorPlan = floorPlan.copy(furnitures = list)
    }

    fun moveFurnitureTo(index: Int, left: Float, top: Float) {
        val list = floorPlan.furnitures.toMutableList()
        val furniture = list.getOrNull(index) ?: return
        val bounds = floorPlan.bounds
        val rect = furniture.rect
        val width = rect.width()
        val height = rect.height()
        val clampedLeft = left.coerceIn(bounds.left, bounds.right - width)
        val clampedTop = top.coerceIn(bounds.top, bounds.bottom - height)
        list[index] = furniture.copy(rect = RectF(clampedLeft, clampedTop, clampedLeft + width, clampedTop + height))
        floorPlan = floorPlan.copy(furnitures = list)
    }

    fun moveOpening(isDoor: Boolean, index: Int, dx: Float, dy: Float) {
        val bounds = floorPlan.bounds
        if (isDoor) {
            val list = floorPlan.doors.toMutableList()
            val opening = list.getOrNull(index) ?: return
            val rect = opening.rect
            val width = rect.width()
            val height = rect.height()
            val left = (rect.left + dx).coerceIn(bounds.left, bounds.right - width)
            val top = (rect.top + dy).coerceIn(bounds.top, bounds.bottom - height)
            list[index] = opening.copy(rect = RectF(left, top, left + width, top + height))
            floorPlan = floorPlan.copy(doors = list)
        } else {
            val list = floorPlan.windows.toMutableList()
            val opening = list.getOrNull(index) ?: return
            val rect = opening.rect
            val width = rect.width()
            val height = rect.height()
            val left = (rect.left + dx).coerceIn(bounds.left, bounds.right - width)
            val top = (rect.top + dy).coerceIn(bounds.top, bounds.bottom - height)
            list[index] = opening.copy(rect = RectF(left, top, left + width, top + height))
            floorPlan = floorPlan.copy(windows = list)
        }
    }

    fun selectFurniture(index: Int?) {
        selectedFurnitureIndex = index
    }

    fun resizeSelected(widthMm: Float, heightMm: Float) {
        val idx = selectedFurnitureIndex ?: return
        val list = floorPlan.furnitures.toMutableList()
        val furniture = list.getOrNull(idx) ?: return
        val pxWidth = (widthMm / floorPlan.scaleMmPerPx).coerceAtLeast(20f)
        val pxHeight = (heightMm / floorPlan.scaleMmPerPx).coerceAtLeast(20f)
        val left = furniture.rect.left
        val top = furniture.rect.top
        list[idx] = furniture.copy(rect = RectF(left, top, left + pxWidth, top + pxHeight))
        floorPlan = floorPlan.copy(furnitures = list)
    }

    fun autoDetectFrom(bitmap: Bitmap) {
        // Placeholder heuristic: ensure at least one door/window exists.
        floorPlan = ensureDefaultOpenings(floorPlan)
    }

    fun spawnInventoryToPlan(resetPrevious: Boolean = true) {
        val mmPerPx = floorPlan.scaleMmPerPx.coerceAtLeast(1f)
        val bounds = floorPlan.bounds
        val base = if (resetPrevious) emptyList() else floorPlan.furnitures
        val additions = mutableListOf<Furniture>()
        val cursor = PlacementCursor(bounds)

        inventory.forEach { (category, qty) ->
            val count = qty.coerceAtLeast(0)
            repeat(count) {
                val (widthMm, heightMm) = defaultSizeMm(category)
                val widthPx = widthMm / mmPerPx
                val heightPx = heightMm / mmPerPx
                val (left, top) = cursor.next(widthPx, heightPx)
                val rect = RectF(left, top, left + widthPx, top + heightPx)
                additions += Furniture(category, rect, FurnOrigin.INVENTORY)
            }
        }

        floorPlan = floorPlan.copy(furnitures = base + additions)
    }

    private fun defaultSizeMm(category: FurnCategory): Pair<Int, Int> = when (category) {
        FurnCategory.BED -> 1500 to 2000
        FurnCategory.DESK -> 1200 to 600
        FurnCategory.SOFA -> 1800 to 900
        FurnCategory.WARDROBE -> 1200 to 600
        FurnCategory.TABLE -> 800 to 800
        FurnCategory.OTHER -> 700 to 700
    }

    // ---------------------------------------------------------------------
    // Style analysis and catalog selection
    // ---------------------------------------------------------------------

    fun analyzeConcept(text: String) {
        conceptText = text.trim()
        analyzeJob?.cancel()

        if (conceptText.isBlank()) {
            styleProbabilities = emptyList()
            styleTags = defaultStyleTags()
            analysisState = AnalysisState.Success
            analysisErrorMessage = null
            buildRecommendationCatalog()
            return
        }

        val fallbackTags = deriveStyleTags(conceptText).ifEmpty { defaultStyleTags() }
        val analyzer = styleAnalyzer
        if (analyzer == null) {
            styleProbabilities = emptyList()
            styleTags = fallbackTags
            analysisState = AnalysisState.Failed
            analysisErrorMessage = "AI 모델 초기화에 실패했습니다."
            buildRecommendationCatalog()
            return
        }

        isAnalyzingConcept = true
        analysisState = AnalysisState.Running
        analysisErrorMessage = null
        analyzeJob = viewModelScope.launch {
            try {
                val scores = analyzer.analyze(conceptText)
                styleProbabilities = scores
                val picked = pickTagsFromScores(scores, analyzer)
                styleTags = if (picked.isEmpty()) fallbackTags else picked
                analysisState = AnalysisState.Success
            } catch (c: CancellationException) {
                throw c
            } catch (t: Throwable) {
                Log.e(LOG_TAG, "Style analyzer failed", t)
                styleProbabilities = emptyList()
                styleTags = fallbackTags
                analysisState = AnalysisState.Failed
                analysisErrorMessage = t.message ?: "AI 분석 중 오류가 발생했습니다."
            } finally {
                isAnalyzingConcept = false
                buildRecommendationCatalog()
            }
        }
    }

    fun buildRecommendationCatalog() {
        val catalog = demoCatalog().items
        val prioritized = if (styleTags.isEmpty()) {
            catalog
        } else {
            val filtered = catalog.filter { it.styleTags.intersect(styleTags).isNotEmpty() }
            if (filtered.isEmpty()) catalog else filtered
        }.sortedWith(
            compareByDescending<CatalogItem> { it.styleTags.count { tag -> tag in styleTags } }
                .thenBy { it.priceKRW }
        )

        recommendedCatalog = prioritized
        val availableIds = prioritized.map { it.id }.toSet()
        val keep = chosenCatalog.filter { it in availableIds }.toSet()
        chosenCatalog = if (keep.isNotEmpty()) {
            keep
        } else {
            pickDiverseDefaults(prioritized, 4)
        }
    }

    fun toggleChooseCatalogItem(id: String) {
        chosenCatalog = if (id in chosenCatalog) chosenCatalog - id else chosenCatalog + id
    }

    // ---------------------------------------------------------------------
    // Recommendation generation and selection
    // ---------------------------------------------------------------------

    fun generateRecommendations() {
        beforePlan = floorPlan
        val base = applyCatalogSelections(floorPlan)
        val variantA = base.copy(
            furnitures = base.furnitures.map { furniture ->
                when (furniture.category) {
                    FurnCategory.BED -> placeAwayFromDoor(furniture, base)
                    FurnCategory.DESK -> placeNearWindow(furniture, base)
                    else -> snapToWall(furniture, base)
                }
            }
        )
        val variantB = base.copy(
            furnitures = base.furnitures.map { furniture ->
                when (furniture.category) {
                    FurnCategory.BED -> centerOnWall(furniture, base)
                    FurnCategory.DESK -> sideWall(furniture, base)
                    FurnCategory.TABLE -> centerOnWall(furniture, base)
                    else -> snapToWall(furniture, base)
                }
            }
        )
        val variantC = tidyLayout(base)

        recommendations = listOf(
            Recommendation(
                id = "A",
                title = "Plan A · Cozy flow",
                rationale = "Keeps the bed away from the door and places the desk near daylight.",
                plan = variantA
            ),
            Recommendation(
                id = "B",
                title = "Plan B · Minimal balance",
                rationale = "Aligns major furniture along walls for maximum open center space.",
                plan = variantB
            ),
            Recommendation(
                id = "C",
                title = "Plan C · Tidy grid",
                rationale = "Uses collision-free snapping to keep circulation clear.",
                plan = variantC
            )
        )
    }

    fun chooseRecommendation(rec: Recommendation) {
        selectedRec = rec
        floorPlan = rec.plan
    }

    fun buildShoppingForSelection(): List<CatalogItem> {
        val rec = selectedRec ?: return emptyList()
        val categories = rec.plan.furnitures.map { it.category }.toSet()
        val source = if (recommendedCatalog.isNotEmpty()) {
            recommendedCatalog
        } else {
            demoCatalog().items
        }
        return source
            .filter { it.category in categories }
            .sortedByDescending { it.styleTags.count { tag -> tag in styleTags } }
            .groupBy { it.category }
            .flatMap { (_, items) -> items.take(3) }
    }

    // ---------------------------------------------------------------------
    // Internal helpers for layout and style rules
    // ---------------------------------------------------------------------

    private fun applyCatalogSelections(plan: FloorPlan): FloorPlan {
        val selectedItems = recommendedCatalog.filter { it.id in chosenCatalog }
        if (selectedItems.isEmpty()) return plan

        val mmPerPx = plan.scaleMmPerPx.coerceAtLeast(1f)
        val bounds = plan.bounds
        val cursor = PlacementCursor(bounds)
        val updated = plan.furnitures.toMutableList()

        selectedItems.forEach { item ->
            val widthPx = item.defaultWidthMm / mmPerPx
            val heightPx = item.defaultHeightMm / mmPerPx
            val index = updated.indexOfFirst { it.category == item.category }
            val rect = if (index >= 0) {
                val baseRect = updated[index].rect
                val cx = baseRect.centerX()
                val cy = baseRect.centerY()
                RectF(
                    (cx - widthPx / 2f).coerceIn(bounds.left, bounds.right - widthPx),
                    (cy - heightPx / 2f).coerceIn(bounds.top, bounds.bottom - heightPx),
                    0f,
                    0f
                ).apply {
                    right = left + widthPx
                    bottom = top + heightPx
                }
            } else {
                val (left, top) = cursor.next(widthPx, heightPx)
                RectF(left, top, left + widthPx, top + heightPx)
            }
            val replacement = Furniture(item.category, rect, FurnOrigin.CATALOG)
            if (index >= 0) {
                updated[index] = replacement
            } else {
                updated += replacement
            }
        }

        return plan.copy(furnitures = updated)
    }

    private fun pickTagsFromScores(
        scores: List<StyleProbability>,
        analyzer: StyleAnalyzer,
        minScore: Float = 0.2f,
        maxCount: Int = 3
    ): Set<String> {
        if (scores.isEmpty()) return emptySet()
        val filtered = scores.filter { it.probability >= minScore }
        val source = if (filtered.isEmpty()) scores else filtered
        val collected = linkedSetOf<String>()
        source.take(maxCount).forEach { probability ->
            val mapped = analyzer.styleTagsFor(probability.label)
            if (mapped.isNotEmpty()) {
                collected += mapped
            }
        }
        return collected
    }

    fun cancelAnalysis() {
        analyzeJob?.cancel()
        analyzeJob = null
        isAnalyzingConcept = false
        analysisState = AnalysisState.Idle
        analysisErrorMessage = null
    }

    fun markAnalysisHandled() {
        analysisState = AnalysisState.Idle
        analysisErrorMessage = null
    }

    private fun deriveStyleTags(text: String): Set<String> {
        if (text.isBlank()) return emptySet()
        val normalized = text.lowercase()
        val hits = mutableSetOf<String>()
        for (rule in styleRules) {
            if (rule.keywords.any { normalized.contains(it) }) {
                hits += rule.tags
            }
        }
        return hits
    }

    private fun defaultStyleTags(): Set<String> = setOf("Minimal")

    private fun pickDiverseDefaults(items: List<CatalogItem>, limit: Int): Set<String> {
        val result = mutableListOf<String>()
        val usedCategories = mutableSetOf<FurnCategory>()
        for (item in items) {
            if (result.size >= limit) break
            if (item.category !in usedCategories || usedCategories.size >= limit) {
                result += item.id
                usedCategories += item.category
            }
        }
        return result.toSet()
    }

    private fun snapToWall(furniture: Furniture, plan: FloorPlan, padding: Float = 8f): Furniture {
        val rect = furniture.rect
        val bounds = plan.bounds
        val gaps = listOf(
            rect.left - bounds.left to Direction.LEFT,
            bounds.right - rect.right to Direction.RIGHT,
            rect.top - bounds.top to Direction.TOP,
            bounds.bottom - rect.bottom to Direction.BOTTOM
        )
        val (_, side) = gaps.minBy { it.first }
        val dx: Float
        val dy: Float
        when (side) {
            Direction.LEFT -> {
                dx = -(rect.left - bounds.left - padding)
                dy = 0f
            }
            Direction.RIGHT -> {
                dx = bounds.right - rect.right - padding
                dy = 0f
            }
            Direction.TOP -> {
                dx = 0f
                dy = -(rect.top - bounds.top - padding)
            }
            Direction.BOTTOM -> {
                dx = 0f
                dy = bounds.bottom - rect.bottom - padding
            }
        }
        val moved = RectF(rect.left + dx, rect.top + dy, rect.right + dx, rect.bottom + dy)
        return furniture.copy(rect = moved)
    }

    private fun placeNearWindow(furniture: Furniture, plan: FloorPlan): Furniture {
        val window = plan.windows.firstOrNull() ?: return snapToWall(furniture, plan)
        val rect = furniture.rect
        val bounds = plan.bounds
        val width = rect.width()
        val height = rect.height()
        val left = (window.rect.centerX() - width / 2f).coerceIn(bounds.left + 8f, bounds.right - width - 8f)
        val top = (window.rect.bottom + 16f).coerceIn(bounds.top + 8f, bounds.bottom - height - 8f)
        return furniture.copy(rect = RectF(left, top, left + width, top + height))
    }

    private fun placeAwayFromDoor(furniture: Furniture, plan: FloorPlan): Furniture {
        val door = plan.doors.firstOrNull()
        val rect = furniture.rect
        val bounds = plan.bounds
        val candidates = listOf(
            RectF(bounds.left + 8f, bounds.top + 8f, bounds.left + 8f + rect.width(), bounds.top + 8f + rect.height()),
            RectF(bounds.right - rect.width() - 8f, bounds.top + 8f, bounds.right - 8f, bounds.top + 8f + rect.height()),
            RectF(bounds.left + 8f, bounds.bottom - rect.height() - 8f, bounds.left + 8f + rect.width(), bounds.bottom - 8f),
            RectF(bounds.right - rect.width() - 8f, bounds.bottom - rect.height() - 8f, bounds.right - 8f, bounds.bottom - 8f)
        )
        if (door == null) return furniture.copy(rect = candidates.last())
        val farthest = candidates.maxBy { candidate ->
            val dx = door.rect.centerX() - candidate.centerX()
            val dy = door.rect.centerY() - candidate.centerY()
            dx * dx + dy * dy
        }
        return furniture.copy(rect = farthest)
    }

    private fun centerOnWall(furniture: Furniture, plan: FloorPlan): Furniture {
        val rect = furniture.rect
        val bounds = plan.bounds
        return if (bounds.width() >= bounds.height()) {
            val left = bounds.centerX() - rect.width() / 2f
            val top = bounds.top + 8f
            furniture.copy(rect = RectF(left, top, left + rect.width(), top + rect.height()))
        } else {
            val left = bounds.left + 8f
            val top = bounds.centerY() - rect.height() / 2f
            furniture.copy(rect = RectF(left, top, left + rect.width(), top + rect.height()))
        }
    }

    private fun sideWall(furniture: Furniture, plan: FloorPlan): Furniture {
        val rect = furniture.rect
        val bounds = plan.bounds
        val left = bounds.right - rect.width() - 8f
        val top = (bounds.centerY() - rect.height() / 2f).coerceIn(bounds.top + 8f, bounds.bottom - rect.height() - 8f)
        return furniture.copy(rect = RectF(left, top, left + rect.width(), top + rect.height()))
    }

    private fun tidyLayout(plan: FloorPlan): FloorPlan {
        var current = plan.copy(furnitures = plan.furnitures.map { snapToWall(it, plan, 6f) })
        repeat(6) {
            val list = current.furnitures.toMutableList()
            var changed = false
            for (i in list.indices) {
                for (j in i + 1 until list.size) {
                    val a = list[i].rect
                    val b = list[j].rect
                    if (RectF.intersects(a, b)) {
                        val dy = max(6f, (a.bottom - b.top) + 6f)
                        list[j] = list[j].copy(
                            rect = RectF(b.left, b.top + dy, b.right, b.bottom + dy)
                        )
                        changed = true
                    }
                }
            }
            current = current.copy(furnitures = list.map { fitInside(it, current.bounds) })
            if (!changed) return current
        }
        return current
    }

    private fun fitInside(furniture: Furniture, bounds: RectF): Furniture {
        val rect = furniture.rect
        val width = rect.width()
        val height = rect.height()
        val left = rect.left.coerceIn(bounds.left, bounds.right - width)
        val top = rect.top.coerceIn(bounds.top, bounds.bottom - height)
        return furniture.copy(rect = RectF(left, top, left + width, top + height))
    }

    private val styleRules = listOf(
        StyleRule(
            keywords = listOf("natural", "nature", "wood", "linen", "내추럴", "우드"),
            tags = setOf("Natural", "Warm")
        ),
        StyleRule(
            keywords = listOf("warm", "cozy", "comfort", "아늑", "따뜻"),
            tags = setOf("Warm", "Cozy")
        ),
        StyleRule(
            keywords = listOf("minimal", "clean", "simple", "미니멀"),
            tags = setOf("Minimal")
        ),
        StyleRule(
            keywords = listOf("modern", "sleek", "contemporary", "모던"),
            tags = setOf("Modern")
        ),
        StyleRule(
            keywords = listOf("vintage", "retro", "빈티지"),
            tags = setOf("Vintage")
        ),
        StyleRule(
            keywords = listOf("industrial", "metal", "콘크리트", "인더스트리얼"),
            tags = setOf("Industrial")
        ),
        StyleRule(
            keywords = listOf("light", "bright", "white"),
            tags = setOf("Bright")
        )
    )

    private fun demoCatalog() = StyleCatalog(
        items = listOf(
            CatalogItem(
                id = "bed_natural_01",
                name = "Oak frame platform bed",
                category = FurnCategory.BED,
                styleTags = setOf("Natural", "Warm"),
                defaultWidthMm = 1600,
                defaultHeightMm = 2000,
                priceKRW = 459_000,
                shopLinks = listOf(ShopLink("Nordico", "https://example.com/bed_natural_01"))
            ),
            CatalogItem(
                id = "bed_minimal_01",
                name = "Low profile fabric bed",
                category = FurnCategory.BED,
                styleTags = setOf("Minimal", "Bright"),
                defaultWidthMm = 1500,
                defaultHeightMm = 2000,
                priceKRW = 329_000,
                shopLinks = listOf(ShopLink("CalmLiving", "https://example.com/bed_minimal_01"))
            ),
            CatalogItem(
                id = "desk_modern_01",
                name = "Walnut study desk 1400",
                category = FurnCategory.DESK,
                styleTags = setOf("Modern", "Warm"),
                defaultWidthMm = 1400,
                defaultHeightMm = 700,
                priceKRW = 259_000,
                shopLinks = listOf(ShopLink("BeamDesk", "https://example.com/desk_modern_01"))
            ),
            CatalogItem(
                id = "desk_minimal_01",
                name = "White steel desk 1200",
                category = FurnCategory.DESK,
                styleTags = setOf("Minimal", "Bright"),
                defaultWidthMm = 1200,
                defaultHeightMm = 600,
                priceKRW = 159_000,
                shopLinks = listOf(ShopLink("SimpleLine", "https://example.com/desk_minimal_01"))
            ),
            CatalogItem(
                id = "sofa_cozy_01",
                name = "Two-seat boucle sofa",
                category = FurnCategory.SOFA,
                styleTags = setOf("Cozy", "Warm"),
                defaultWidthMm = 1700,
                defaultHeightMm = 900,
                priceKRW = 389_000,
                shopLinks = listOf(ShopLink("SoftNest", "https://example.com/sofa_cozy_01"))
            ),
            CatalogItem(
                id = "wardrobe_modern_01",
                name = "Sliding wardrobe 1200",
                category = FurnCategory.WARDROBE,
                styleTags = setOf("Modern", "Minimal"),
                defaultWidthMm = 1200,
                defaultHeightMm = 600,
                priceKRW = 299_000,
                shopLinks = listOf(ShopLink("ClosetLab", "https://example.com/wardrobe_modern_01"))
            ),
            CatalogItem(
                id = "table_natural_01",
                name = "Round dining table 900",
                category = FurnCategory.TABLE,
                styleTags = setOf("Natural", "Bright"),
                defaultWidthMm = 900,
                defaultHeightMm = 900,
                priceKRW = 189_000,
                shopLinks = listOf(ShopLink("Oak&Co", "https://example.com/table_natural_01"))
            ),
            CatalogItem(
                id = "table_industrial_01",
                name = "Metal cafe table 800",
                category = FurnCategory.TABLE,
                styleTags = setOf("Industrial", "Modern"),
                defaultWidthMm = 800,
                defaultHeightMm = 800,
                priceKRW = 210_000,
                shopLinks = listOf(ShopLink("SteelCraft", "https://example.com/table_industrial_01"))
            )
        )
    )

    private data class StyleRule(
        val keywords: List<String>,
        val tags: Set<String>
    )

    private class PlacementCursor(
        private val bounds: RectF,
        private val padding: Float = 16f,
        private val gap: Float = 12f
    ) {
        private var cursorX = bounds.left + padding
        private var cursorY = bounds.top + padding
        private val maxRight = bounds.right - padding

        fun next(width: Float, height: Float): Pair<Float, Float> {
            if (cursorX + width > maxRight) {
                cursorX = bounds.left + padding
                cursorY += height + gap
            }
            val maxTop = (bounds.bottom - height - padding).coerceAtLeast(bounds.top + padding)
            val yClamped = cursorY.coerceIn(bounds.top + padding, maxTop)
            val x = cursorX
            val y = yClamped
            cursorX += width + gap
            cursorY = (yClamped + height + gap).coerceAtMost(maxTop)
            return x to y
        }
    }

    private enum class Direction { LEFT, RIGHT, TOP, BOTTOM }

    companion object {
        private const val LOG_TAG = "FloorPlanViewModel"


        fun provideFactory(context: Context): ViewModelProvider.Factory {
            val appContext = context.applicationContext
            return object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(FloorPlanViewModel::class.java)) {
                        val analyzer = StyleAnalyzerProvider.getOrNull(appContext)
                        @Suppress("UNCHECKED_CAST")
                        return FloorPlanViewModel(analyzer) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: $modelClass")
                }
            }
        }
    }
}
