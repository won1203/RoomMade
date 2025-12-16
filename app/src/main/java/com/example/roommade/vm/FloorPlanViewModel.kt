package com.example.roommade.vm

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RectF
import android.util.Log
import android.util.Base64
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.roommade.data.BoardRepository
import com.example.roommade.data.FirestoreBoardRepository
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
import com.example.roommade.model.RoomCategory
import com.example.roommade.model.Recommendation
import com.example.roommade.model.RoomSpec
import com.example.roommade.model.ShopLink
import com.example.roommade.model.defaults
import com.example.roommade.model.korLabel
import com.example.roommade.network.NaverShoppingClient
import com.example.roommade.network.NaverShoppingItem
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.io.ByteArrayOutputStream
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

enum class AnalysisState { Idle, Running, Success, Failed }

class FloorPlanViewModel(
    private val styleAnalyzer: StyleAnalyzer? = null,
    private val shoppingClient: NaverShoppingClient = NaverShoppingClient(),
    private val boardRepo: BoardRepository = FirestoreBoardRepository()
) : ViewModel() {

    var roomCategory by mutableStateOf(RoomCategory.MASTER_BEDROOM)
        private set

    var roomSpec by mutableStateOf(RoomSpec())
        private set

    var floorPlan by mutableStateOf(FloorPlan())
        private set

    var selectedFurnitureIndex by mutableStateOf<Int?>(null)
        private set

    var selectedBaseRoomImage by mutableStateOf<String?>(null)
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

    var scrapedItems by mutableStateOf<List<CatalogItem>>(emptyList())
        private set

    var recommendations by mutableStateOf<List<Recommendation>>(emptyList())
        private set

    var selectedRec by mutableStateOf<Recommendation?>(null)
        private set

    var beforePlan by mutableStateOf<FloorPlan?>(null)
        private set

    var inventory by mutableStateOf<Map<FurnCategory, Int>>(emptyMap())
        private set

    var shoppingQueryInput by mutableStateOf("")
        private set

    var shoppingResults by mutableStateOf<List<NaverShoppingItem>>(emptyList())
        private set

    var shoppingIsLoading by mutableStateOf(false)
        private set

    var shoppingErrorMessage by mutableStateOf<String?>(null)
        private set

    var shoppingCategoryFilter by mutableStateOf<String?>(null)
        private set

    data class CartEntry(
        val sessionId: String?,
        val item: NaverShoppingItem
    )

    var cartItems by mutableStateOf<List<CartEntry>>(emptyList())
        private set

    var placementRendered by mutableStateOf(false)
        private set

    data class GeneratedBoard(
        val id: String,
        val imageUrl: String,
        val concept: String,
        val styleLabel: String,
        val roomCategory: String,
        val createdAt: Long = System.currentTimeMillis()
    )

    var generatedBoards by mutableStateOf<List<GeneratedBoard>>(emptyList())
        private set

    private var analyzeJob: Job? = null
    private var shoppingJob: Job? = null
    private var boardsJob: Job? = null
    private var currentUserId: String? = null
    private var currentSessionId: String? = null
    private var lastSavedImageUrl: String? = null

    fun beginNewCartSession() {
        currentSessionId = "session_${System.currentTimeMillis()}"
    }

    /**
     * 추천 가구 화면 전용: 항상 새 세션을 만들고, 새 세션과 충돌하는 잔여 아이템을 정리한다.
     * 기존 세션(보드 ID나 이전 세션 ID)에 담긴 장바구니는 그대로 유지된다.
     */
    fun startShoppingSession() {
        val newSession = "session_${System.currentTimeMillis()}"
        currentSessionId = newSession
        // 혹시 동일 ID가 이미 있었다면 제거 (실제론 없겠지만 안전)
        cartItems = cartItems.filterNot { it.sessionId == newSession }
    }

    fun currentSessionCartItems(): List<CartEntry> {
        val sid = currentSessionId
        return if (sid != null) cartItems.filter { it.sessionId == sid } else emptyList()
    }

    fun currentSessionCartCount(): Int = currentSessionCartItems().size

    private val firestore by lazy { FirebaseFirestore.getInstance() }

    // ---------------------------------------------------------------------
    // Auth and library sync
    // ---------------------------------------------------------------------

    fun attachUser(uid: String?) {
        if (uid.isNullOrBlank()) {
            currentUserId = null
            boardsJob?.cancel()
            generatedBoards = emptyList()
            return
        }
        if (uid == currentUserId) return
        currentUserId = uid
        boardsJob?.cancel()
        boardsJob = viewModelScope.launch {
            boardRepo.observeBoards(uid).collectLatest { boards ->
                // 원격 동기화가 실패해 빈 리스트가 내려와도, 로컬에 이미 생성된 보드는 유지한 채 병합
                val current = generatedBoards.associateBy { it.id }.toMutableMap()
                boards.forEach { current[it.id] = it }
                generatedBoards = current.values.sortedByDescending { it.createdAt }
                if (currentSessionId == null || currentSessionId?.startsWith("session_") == true) {
                    currentSessionId = generatedBoards.firstOrNull()?.id
                }
                pruneCartForExistingBoards()
            }
        }
        // Cart 로드
        viewModelScope.launch {
            loadCart(uid)
        }
    }

    // ---------------------------------------------------------------------
    // Room specification and base layout
    // ---------------------------------------------------------------------

    private fun normalizeRoomSpec(areaPyeong: Float, aspectRatio: Float): RoomSpec {
        val clampedArea = areaPyeong.coerceIn(2f, 80f)
        val clampedAspect = aspectRatio.coerceIn(0.5f, 2.0f)
        return roomSpec.copy(areaPyeong = clampedArea, aspect = clampedAspect)
    }

    private fun refreshPlanScale() {
        floorPlan = floorPlan.copy(bounds = buildBounds(), scaleMmPerPx = computeMmPerPx())
    }

    fun prepareStructure(
        areaPyeong: Float,
        aspectRatio: Float,
        inventoryCounts: Map<FurnCategory, Int>
    ) {
        roomSpec = normalizeRoomSpec(areaPyeong, aspectRatio)
        setInventoryCounts(inventoryCounts)
        cartItems = emptyList()
        placementRendered = false
        scrapedItems = emptyList()
        chosenCatalog = emptySet()
        shoppingJob?.cancel()
        shoppingQueryInput = ""
        shoppingResults = emptyList()
        shoppingErrorMessage = null
        shoppingIsLoading = false
        shoppingCategoryFilter = null

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
        roomSpec = normalizeRoomSpec(value, roomSpec.aspect)
        refreshPlanScale()
    }

    fun setRoomAspect(value: Float) {
        roomSpec = normalizeRoomSpec(roomSpec.areaPyeong, value)
        refreshPlanScale()
    }

    fun selectRoomCategory(category: RoomCategory) {
        if (roomCategory == category) return
        roomCategory = category
        selectedBaseRoomImage = null
        val defaults = category.defaults()
        roomSpec = normalizeRoomSpec(defaults.areaPyeong, defaults.aspect)
        refreshPlanScale()
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
        FurnCategory.CHAIR -> 520 to 520
        FurnCategory.LIGHTING -> 360 to 360
        FurnCategory.RUG -> 1600 to 2000
        FurnCategory.OTHER -> 700 to 700
    }

    private fun cartTag(id: String): String = "$CART_TAG_PREFIX$id"

    private val categoryKeywords: List<Pair<FurnCategory, List<String>>> = listOf(
        FurnCategory.BED to listOf("\uCE68\uB300", "bed", "\uB9E4\uD2B8\uB9AC\uC2A4", "mattress"),
        FurnCategory.SOFA to listOf("\uC18C\uD30C", "sofa", "\uCE74\uC6B0\uCE58"),
        FurnCategory.TABLE to listOf("\uD14C\uC774\uBE14", "\uC2DD\uD0D5", "table", "dining"),
        FurnCategory.DESK to listOf("\uCC45\uC0C1", "desk"),
        FurnCategory.WARDROBE to listOf("\uC637\uC7A5", "\uBD99\uBC15\uC774\uC7A5", "wardrobe", "closet", "\uC218\uB0A9\uC7A5"),
        FurnCategory.CHAIR to listOf("\uC758\uC790", "\uCCB4\uC5B4", "chair", "stool", "\uBCA4\uCE58", "\uC555\uCCB4\uC5B4"),
        FurnCategory.LIGHTING to listOf("\uC870\uBA85", "\uB7A8\uD504", "\uC2A4\uD0E4\uB4DC", "\uBB34\uB4DC\uB4F1", "\uBB34\uB4DC", "light", "lamp"),
        FurnCategory.RUG to listOf("\uB7EC\uADF8", "\uCE74\uD3A0", "\uCE74\uD398\uD2B8", "\uCE74\uD3A0\uD2B8", "\uB9E4\uD2B8", "\uB7EC\uADF8\uB9E4\uD2B8", "carpet", "rug")
    )

    private fun guessCategoryFromText(text: String): FurnCategory {
        val normalized = text.lowercase(Locale.ROOT)
        for ((category, keywords) in categoryKeywords) {
            if (keywords.any { keyword -> normalized.contains(keyword) }) {
                return category
            }
        }
        return FurnCategory.OTHER
    }
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
        val catalog = inventoryBackedCatalog()
        val prioritized = if (styleTags.isEmpty()) {
            catalog
        } else {
            val filtered = catalog.filter { it.styleTags.intersect(styleTags).isNotEmpty() }
            if (filtered.isEmpty()) catalog else filtered
        }.sortedWith(
            compareByDescending<CatalogItem> { item ->
                item.styleTags.count { tag -> tag in styleTags }
            }.thenBy { it.priceKRW }
        )

        val combined = prioritized + scrapedItems
        recommendedCatalog = combined
        val availableIds = combined.map { it.id }.toSet()
        val keep = chosenCatalog.filter { it in availableIds }.toSet()
        chosenCatalog = if (keep.isNotEmpty()) keep else pickDiverseDefaults(prioritized, 4)
        shoppingQueryInput = shoppingSearchQuery()
    }

    fun toggleChooseCatalogItem(id: String) {
        chosenCatalog = if (id in chosenCatalog) chosenCatalog - id else chosenCatalog + id
    }

    fun addScrapedItem(name: String, category: FurnCategory, price: Int?, sourceUrl: String?) {
        val safeName = name.ifBlank { category.korLabel() }
        val tags = if (styleTags.isNotEmpty()) styleTags else defaultStyleTags()
        val (defaultWidth, defaultHeight) = defaultSizeMm(category)
        val item = CatalogItem(
            id = "$SCRAP_ITEM_PREFIX${System.currentTimeMillis()}",
            name = safeName,
            category = category,
            styleTags = tags,
            defaultWidthMm = defaultWidth,
            defaultHeightMm = defaultHeight,
            priceKRW = price?.coerceAtLeast(0) ?: 0,
            shopLinks = sourceUrl?.takeIf { it.isNotBlank() }
                ?.let { listOf(ShopLink("Scrap", it)) }
                ?: emptyList()
        )
        scrapedItems = scrapedItems + item
        chosenCatalog = chosenCatalog + item.id
        buildRecommendationCatalog()
    }

    fun shoppingSearchQuery(): String {
        val keyword = styleKeyword()
        val category = shoppingCategoryFilter
        return when {
            keyword != null && category != null -> "$keyword $category"
            keyword != null -> "$keyword 인테리어 가구"
            category != null -> "$category 추천"
            else -> "인테리어 가구 추천"
        }
    }

    fun updateShoppingQueryInput(value: String) {
        shoppingQueryInput = value
    }

    fun ensureShoppingQueryPrefilled() {
        if (shoppingQueryInput.isBlank()) {
            shoppingQueryInput = shoppingSearchQuery()
        }
    }

    fun hasShoppingCredentials(): Boolean = shoppingClient.hasCredentials()

    fun searchShoppingItems(queryOverride: String? = null) {
        val target = queryOverride
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: shoppingQueryInput.trim().ifBlank { shoppingSearchQuery() }
        if (target.isBlank()) return

        shoppingQueryInput = target
        shoppingJob?.cancel()
        shoppingJob = viewModelScope.launch {
            shoppingIsLoading = true
            shoppingErrorMessage = null
            try {
                val result = shoppingClient.search(target)
                shoppingResults = result
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                Log.e(LOG_TAG, "Failed to search Naver shopping", e)
                shoppingErrorMessage = e.message ?: "추천 가구 검색 중 오류가 발생했습니다."
                shoppingResults = emptyList()
            } finally {
                shoppingIsLoading = false
            }
        }
    }

    fun updateShoppingCategoryFilter(categoryLabel: String?) {
        val normalized = categoryLabel?.takeIf { it.isNotBlank() }
        if (shoppingCategoryFilter == normalized) return
        shoppingCategoryFilter = normalized
        shoppingQueryInput = shoppingSearchQuery()
    }

    // ---------------------------------------------------------------------
    // Shopping cart and placement
    // ---------------------------------------------------------------------

    fun toggleCartItem(item: NaverShoppingItem) {
        if (currentSessionId == null) {
            currentSessionId = generatedBoards.firstOrNull()?.id ?: "session_${System.currentTimeMillis()}"
        }
        val currentSession = currentSessionId
        val existsInSession = cartItems.any { it.item.id == item.id && it.sessionId == currentSession }
        cartItems = if (existsInSession) {
            // 현재 세션의 동일 상품만 제거하고, 다른 세션의 데이터는 유지
            cartItems.filterNot { it.item.id == item.id && it.sessionId == currentSession }
        } else {
            cartItems + CartEntry(sessionId = currentSession, item = item)
        }
        placementRendered = false
        syncCartFurniture()
        persistCartRemote()
    }

    private fun normalizeImageForSync(original: String): String {
        if (!original.startsWith("data:image")) return original
        val bitmap = decodeDataUri(original) ?: return original
        val maxDim = 800
        val scaled = if (bitmap.width > maxDim || bitmap.height > maxDim) {
            val scale = min(
                maxDim.toFloat() / bitmap.width.toFloat(),
                maxDim.toFloat() / bitmap.height.toFloat()
            )
            val w = (bitmap.width * scale).toInt().coerceAtLeast(1)
            val h = (bitmap.height * scale).toInt().coerceAtLeast(1)
            Bitmap.createScaledBitmap(bitmap, w, h, true)
        } else {
            bitmap
        }
        return encodeToDataUri(scaled)
    }

    fun deleteBoard(board: GeneratedBoard) {
        generatedBoards = generatedBoards.filterNot { it.id == board.id }
        // 세션과 매핑된 장바구니 항목도 제거
        cartItems = cartItems.filterNot { it.sessionId == board.id }
        if (currentSessionId == board.id) {
            currentSessionId = generatedBoards.firstOrNull()?.id
        }
        persistCartRemote()
        currentUserId?.let { uid ->
            viewModelScope.launch {
                try {
                    boardRepo.delete(uid, board.id)
                } catch (t: Throwable) {
                    Log.e(LOG_TAG, "Failed to delete board in Firestore", t)
                }
            }
        }
    }

    private fun persistCartRemote() {
        val uid = currentUserId ?: return
        val snapshot = cartItems.map { it.toMap() }
        viewModelScope.launch {
            try {
                firestore.collection("users")
                    .document(uid)
                    .collection("meta")
                    .document("cart")
                    .set(mapOf("items" to snapshot, "updatedAt" to System.currentTimeMillis()), SetOptions.merge())
            } catch (t: Throwable) {
                Log.e(LOG_TAG, "Failed to persist cart", t)
            }
        }
    }

    private suspend fun loadCart(uid: String) {
        try {
            val doc = firestore.collection("users")
                .document(uid)
                .collection("meta")
                .document("cart")
                .get()
                .await()
            val items = doc.get("items") as? List<*> ?: emptyList<Any>()
            val restored = items.mapNotNull { (it as? Map<*, *>)?.toCartEntry() }
            cartItems = restored
            syncCartFurniture()
            pruneCartForExistingBoards()
        } catch (t: Throwable) {
            Log.e(LOG_TAG, "Failed to load cart", t)
        }
    }

    private fun pruneCartForExistingBoards() {
        // 보관함에 없는 세션은 제거하되, 현재 진행 중인 세션은 예외로 둔다.
        val validSessions = generatedBoards.map { it.id }.toMutableSet()
        currentSessionId?.let { validSessions += it }
        val filtered = cartItems.filter { it.sessionId != null && it.sessionId in validSessions }
        val removed = filtered.size != cartItems.size
        cartItems = filtered
        if (currentSessionId != null && currentSessionId !in validSessions) {
            currentSessionId = validSessions.firstOrNull()
        }
        if (removed) {
            syncCartFurniture()
            persistCartRemote()
        }
    }

    private fun CartEntry.toMap(): Map<String, Any?> = mapOf(
        "sessionId" to sessionId,
        "item" to mapOf(
            "id" to item.id,
            "title" to item.title,
            "mallName" to item.mallName,
            "price" to item.price,
            "link" to item.link,
            "imageUrl" to item.imageUrl
        )
    )

    private fun Map<*, *>.toCartEntry(): CartEntry? {
        val sessionId = this["sessionId"] as? String
        val itemMap = this["item"] as? Map<*, *> ?: return null
        val id = itemMap["id"] as? String ?: return null
        val title = itemMap["title"] as? String ?: return null
        val mallName = itemMap["mallName"] as? String
        val price = (itemMap["price"] as? Number)?.toInt() ?: 0
        val link = itemMap["link"] as? String ?: ""
        val imageUrl = itemMap["imageUrl"] as? String
        val item = NaverShoppingItem(
            id = id,
            title = title,
            mallName = mallName,
            price = price,
            link = link,
            imageUrl = imageUrl
        )
        return CartEntry(sessionId = sessionId, item = item)
    }

    private fun decodeDataUri(data: String?): Bitmap? {
        if (data.isNullOrBlank()) return null
        if (!data.startsWith("data:image")) return null
        val comma = data.indexOf(',')
        if (comma <= 0) return null
        val base64Part = data.substring(comma + 1)
        return try {
            val bytes = Base64.decode(base64Part, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: Throwable) {
            null
        }
    }

    private fun encodeToDataUri(bitmap: Bitmap): String {
        return try {
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
            val encoded = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
            "data:image/jpeg;base64,$encoded"
        } catch (_: Throwable) {
            // fallback to original bitmap if encoding fails
            "data:image/jpeg;base64,"
        }
    }

    fun selectBaseRoomImage(imageDataUri: String?) {
        selectedBaseRoomImage = imageDataUri
    }

    fun isInCart(itemId: String): Boolean = cartItems.any { it.item.id == itemId }

    fun cartCount(): Int = cartItems.size
    fun syncCartFurniture() {
        val mmPerPx = floorPlan.scaleMmPerPx.coerceAtLeast(1f)
        val bounds = floorPlan.bounds
        val cursor = PlacementCursor(bounds)
        val existingCart = floorPlan.furnitures.filter { it.tag?.startsWith(CART_TAG_PREFIX) == true }
            .associateBy { it.tag }
        val preserved = floorPlan.furnitures.filterNot { it.tag?.startsWith(CART_TAG_PREFIX) == true }

        val additions = currentSessionCartItems().map { entry ->
            val item = entry.item
            val tag = cartTag(item.id)
            val keptRect = existingCart[tag]?.rect
            val category = guessCategoryFromText(item.title)
            val (widthMm, heightMm) = defaultSizeMm(category)
            val widthPx = widthMm / mmPerPx
            val heightPx = heightMm / mmPerPx
            val rect = keptRect ?: run {
                val (left, top) = cursor.next(widthPx, heightPx)
                RectF(left, top, left + widthPx, top + heightPx)
            }
            Furniture(
                category = category,
                rect = rect,
                origin = FurnOrigin.CATALOG,
                tag = tag
            )
        }

        floorPlan = floorPlan.copy(furnitures = preserved + additions)
    }

    fun saveGeneratedBoard(imageUrl: String) {
        val safeUrl = normalizeImageForSync(imageUrl)
        // 동일 이미지가 이미 보관함에 있으면 중복 생성 대신 해당 세션에 연결만 갱신
        val existing = generatedBoards.firstOrNull { it.imageUrl == safeUrl }
        val previousSession = currentSessionId
        if (existing != null) {
            currentSessionId = existing.id
            cartItems = cartItems.map {
                if (it.sessionId == previousSession || (it.sessionId == null && previousSession != null)) {
                    CartEntry(sessionId = existing.id, item = it.item)
                } else it
            }
            syncCartFurniture()
            persistCartRemote()
            return
        }
        if (lastSavedImageUrl == safeUrl) return
        val id = "gen_${System.currentTimeMillis()}"
        val entry = GeneratedBoard(
            id = id,
            imageUrl = safeUrl,
            concept = conceptText,
            styleLabel = primaryStyleLabel(),
            roomCategory = roomCategory.korLabel()
        )
        lastSavedImageUrl = safeUrl
        generatedBoards = listOf(entry) + generatedBoards
        currentSessionId = id
        // 임시 세션(session_*)으로 담긴 항목만 새 보드 ID로 갱신 (기존 보드 세션은 유지)
        if (previousSession != null && previousSession.startsWith("session_")) {
            cartItems = cartItems.map {
                if (it.sessionId == previousSession || (it.sessionId == null && previousSession != null)) {
                    CartEntry(sessionId = id, item = it.item)
                } else it
            }
        }
        persistCartRemote()
        currentUserId?.let { uid ->
            viewModelScope.launch {
                try {
                    boardRepo.upsert(uid, entry)
                } catch (t: Throwable) {
                    Log.e(LOG_TAG, "Failed to sync board to Firestore", t)
                }
            }
        }
    }

    private fun primaryStyleLabel(): String {
        val keyword = styleKeyword()?.ifBlank { null }
        if (keyword != null) return keyword
        val tag = styleTags.firstOrNull()?.ifBlank { null }
        if (tag != null) return tag
        if (conceptText.isNotBlank()) return conceptText
        return "스타일 미분석"
    }

    fun markPlacementRendered() {
        placementRendered = true
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

    private fun inventoryBackedCatalog(): List<CatalogItem> {
        if (inventory.isEmpty()) return emptyList()
        val tags = styleTags.ifEmpty { defaultStyleTags() }
        return inventory.flatMap { (category, count) ->
            val (widthMm, heightMm) = defaultSizeMm(category)
            (0 until count).map { index ->
                CatalogItem(
                    id = "inventory:${category.name.lowercase(Locale.ROOT)}_$index",
                    name = "${category.korLabel()} ${index + 1}",
                    category = category,
                    styleTags = tags,
                    defaultWidthMm = widthMm,
                    defaultHeightMm = heightMm,
                    priceKRW = 0,
                    shopLinks = emptyList()
                )
            }
        }
    }

    private fun styleKeyword(): String? {
        val primary = styleTags.firstOrNull()?.trim().orEmpty()
        if (primary.isBlank()) return null
        val normalized = primary.lowercase(Locale.ROOT)
        val mapped = when (normalized) {
            "modern", "모던" -> "모던"
            "classic", "클래식" -> "클래식"
            "cozy", "코지" -> "코지"
            "natural", "내추럴" -> "내추럴"
            "minimal", "미니멀",
            "minimalist" -> "미니멀"
            else -> null
        }
        if (mapped != null) return mapped
        val containsLatin = primary.any { it.code in 0x41..0x5A || it.code in 0x61..0x7A }
        return if (containsLatin) null else primary
    }

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

    override fun onCleared() {
        super.onCleared()
        analyzeJob?.cancel()
        shoppingJob?.cancel()
        boardsJob?.cancel()
    }

    companion object {
        private const val LOG_TAG = "FloorPlanViewModel"
        private const val SCRAP_ITEM_PREFIX = "scrap:"
        private const val CART_TAG_PREFIX = "cart:"


        fun provideFactory(context: Context): ViewModelProvider.Factory {
            val appContext = context.applicationContext
            return object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(FloorPlanViewModel::class.java)) {
                        val analyzer = StyleAnalyzerProvider.getOrNull(appContext)
                        val shoppingClient = NaverShoppingClient()
                        @Suppress("UNCHECKED_CAST")
                        return FloorPlanViewModel(
                            analyzer,
                            shoppingClient,
                            FirestoreBoardRepository()
                        ) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: $modelClass")
                }
            }
        }
    }
}
