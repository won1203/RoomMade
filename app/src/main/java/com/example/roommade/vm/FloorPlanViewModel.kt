package com.example.roommade.vm

import android.graphics.Bitmap
import android.graphics.RectF
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.roommade.model.CatalogItem
import com.example.roommade.model.FloorPlan
import com.example.roommade.model.FurnCategory
import com.example.roommade.model.Furniture
import com.example.roommade.model.Opening
import com.example.roommade.model.OpeningType
import com.example.roommade.model.Recommendation
import com.example.roommade.model.RoomSpec
import com.example.roommade.model.ShopLink
import com.example.roommade.model.StyleCatalog
import kotlin.math.max

class FloorPlanViewModel : ViewModel() {

    // ---------- 기본 상태 ----------
    var roomSpec by mutableStateOf(RoomSpec())
        private set

    var floorPlan by mutableStateOf(FloorPlan())
        private set

    var selectedFurnitureIndex by mutableStateOf<Int?>(null)
        private set

    // ---------- 스타일/카탈로그/추천 ----------
    var styleTags by mutableStateOf(setOf<String>())
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

    // 인벤토리(보유 가구 수량)
    var inventory by mutableStateOf<Map<FurnCategory, Int>>(emptyMap())
        private set

    // ---------- 입력 단계 ----------
    fun setRoomAreaPyeong(v: Float) {
        roomSpec = roomSpec.copy(areaPyeong = v.coerceIn(2f, 80f))
        recomputeRoom()
    }

    fun setRoomAspect(v: Float) {
        roomSpec = roomSpec.copy(aspect = v.coerceIn(0.5f, 2.0f))
        recomputeRoom()
    }

    fun setInventoryCounts(map: Map<FurnCategory, Int>) {
        inventory = map
    }

    private fun recomputeRoom() {
        val targetWidthPx = 900f
        val mmPerPx = (roomSpec.widthMm / targetWidthPx).coerceAtLeast(1f)
        val widthPx = roomSpec.widthMm / mmPerPx
        val heightPx = roomSpec.heightMm / mmPerPx
        floorPlan = floorPlan.copy(
            bounds = RectF(0f, 0f, widthPx, heightPx),
            scaleMmPerPx = mmPerPx
        )
    }

    // ---------- 편집 단계 ----------
    fun addOpening(type: OpeningType, rect: RectF) {
        floorPlan = when (type) {
            OpeningType.DOOR ->
                floorPlan.copy(doors = floorPlan.doors + Opening(type, rect))
            OpeningType.WINDOW ->
                floorPlan.copy(windows = floorPlan.windows + Opening(type, rect))
        }
    }

    fun addFurniture(cat: FurnCategory, rect: RectF) {
        floorPlan = floorPlan.copy(
            furnitures = floorPlan.furnitures + Furniture(cat, rect)
        )
    }

    // 가구 이동: 방(bounds) 바깥으로 못 나가도록 클램프
    fun moveFurniture(index: Int, dx: Float, dy: Float) {
        val list = floorPlan.furnitures.toMutableList()
        val f = list.getOrNull(index) ?: return

        val b = floorPlan.bounds
        val r = f.rect
        val w = r.width()
        val h = r.height()

        val nl = (r.left + dx).coerceIn(b.left, b.right - w)
        val nt = (r.top  + dy).coerceIn(b.top,  b.bottom - h)

        list[index] = f.copy(rect = android.graphics.RectF(nl, nt, nl + w, nt + h))
        floorPlan = floorPlan.copy(furnitures = list)
    }

    // 문/창 이동: 동일하게 클램프
    fun moveOpening(isDoor: Boolean, index: Int, dx: Float, dy: Float) {
        val b = floorPlan.bounds

        if (isDoor) {
            val list = floorPlan.doors.toMutableList()
            val o = list.getOrNull(index) ?: return
            val r = o.rect
            val w = r.width()
            val h = r.height()

            val nl = (r.left + dx).coerceIn(b.left, b.right - w)
            val nt = (r.top  + dy).coerceIn(b.top,  b.bottom - h)

            list[index] = o.copy(rect = android.graphics.RectF(nl, nt, nl + w, nt + h))
            floorPlan = floorPlan.copy(doors = list)
        } else {
            val list = floorPlan.windows.toMutableList()
            val o = list.getOrNull(index) ?: return
            val r = o.rect
            val w = r.width()
            val h = r.height()

            val nl = (r.left + dx).coerceIn(b.left, b.right - w)
            val nt = (r.top  + dy).coerceIn(b.top,  b.bottom - h)

            list[index] = o.copy(rect = android.graphics.RectF(nl, nt, nl + w, nt + h))
            floorPlan = floorPlan.copy(windows = list)
        }
    }

    fun selectFurniture(index: Int?) { selectedFurnitureIndex = index }

    fun resizeSelected(widthMm: Float, heightMm: Float) {
        val idx = selectedFurnitureIndex ?: return
        val list = floorPlan.furnitures.toMutableList()
        val f = list[idx]
        val pxW = (widthMm / floorPlan.scaleMmPerPx).coerceAtLeast(20f)
        val pxH = (heightMm / floorPlan.scaleMmPerPx).coerceAtLeast(20f)
        val left = f.rect.left
        val top = f.rect.top
        list[idx] = f.copy(rect = RectF(left, top, left + pxW, top + pxH))
        floorPlan = floorPlan.copy(furnitures = list)
    }

    /**
     * 📸 사진 기반 간이 자동 배치(문/창 1개씩 제안). 추후 ML 로직으로 대체 예정.
     */
    fun autoDetectFrom(bitmap: Bitmap) {
        val b = floorPlan.bounds
        val w = b.width()
        val h = b.height()

        val newDoors = if (floorPlan.doors.isEmpty())
            floorPlan.doors + Opening(
                OpeningType.DOOR,
                RectF(16f, h - 60f, 76f, h - 16f)
            ) else floorPlan.doors

        val newWindows = if (floorPlan.windows.isEmpty())
            floorPlan.windows + Opening(
                OpeningType.WINDOW,
                RectF(w / 2f - 60f, 16f, w / 2f + 60f, 46f)
            ) else floorPlan.windows

        floorPlan = floorPlan.copy(doors = newDoors, windows = newWindows)
    }

    /**
     * ✅ 인벤토리 수량을 기반으로 편집 화면 입장 시 가구를 자동 배치.
     * - 기본: 기존 가구 전체를 비우고 격자처럼 좌→우, 줄바꿈하며 놓음.
     * - 나중에 '출처(origin)'을 모델에 추가하면 INVENTORY만 교체하는 방식으로 변경 가능.
     */
    fun spawnInventoryToPlan(resetPrevious: Boolean = true) {
        val mmPerPx = floorPlan.scaleMmPerPx.coerceAtLeast(1f)
        val b = floorPlan.bounds

        // 0) 기존 가구 초기화 정책
        val baseList = if (resetPrevious) emptyList() else floorPlan.furnitures

        // 1) 카테고리별 기본 크기(mm)
        fun defaultSizeMm(cat: FurnCategory): Pair<Int, Int> = when (cat) {
            FurnCategory.BED -> 1500 to 2000
            FurnCategory.DESK -> 1200 to 600
            FurnCategory.SOFA -> 1600 to 800
            FurnCategory.WARDROBE -> 1200 to 600
            FurnCategory.TABLE -> 800 to 800
            else -> 800 to 600
        }

        // 2) 격자 배치
        var x = 16f
        var y = 16f
        val gap = 12f
        val maxRight = b.right - 16f

        val spawned = mutableListOf<Furniture>()
        inventory.forEach { (cat, qty) ->
            repeat(qty.coerceAtLeast(0)) {
                val (wMm, hMm) = defaultSizeMm(cat)
                val wPx = wMm / mmPerPx
                val hPx = hMm / mmPerPx

                if (x + wPx > maxRight) {
                    x = 16f
                    y += hPx + gap
                }

                val r = RectF(x, y, x + wPx, y + hPx)
                spawned += Furniture(cat, r)
                x += wPx + gap
            }
        }

        floorPlan = floorPlan.copy(furnitures = baseList + spawned)
    }

    // ---------- 스타일/카탈로그 ----------
    fun toggleStyle(tag: String) {
        styleTags = if (tag in styleTags) styleTags - tag else styleTags + tag
    }

    fun buildStyleCatalog() {
        val base = demoCatalog()
        recommendedCatalog = base.items.sortedByDescending {
            it.styleTags.intersect(styleTags).size
        }
    }

    fun toggleChooseCatalogItem(id: String) {
        chosenCatalog = if (id in chosenCatalog) chosenCatalog - id else chosenCatalog + id
    }

    fun spawnChosenCatalogToPlan() {
        val mmPerPx = floorPlan.scaleMmPerPx.coerceAtLeast(1f)
        val pick = recommendedCatalog.filter { it.id in chosenCatalog }
        var x = 16f; var y = 16f
        pick.forEach { item ->
            val w = item.defaultWidthMm / mmPerPx
            val h = item.defaultHeightMm / mmPerPx
            addFurniture(item.category, RectF(x, y, x + w, y + h))
            x += w + 12f
            if (x + w > floorPlan.bounds.right - 16f) {
                x = 16f; y += h + 12f
            }
        }
    }

    // ---------- 추천(A/B/C) ----------
    fun generateRecommendations() {
        beforePlan = floorPlan
        val allowedCats = recommendedCatalog
            .filter { it.id in chosenCatalog }
            .map { it.category }
            .toSet()
        fun fit(fp: FloorPlan) =
            fp.copy(furnitures = fp.furnitures.filter { it.category in allowedCats })

        val base = fit(floorPlan)
        val a = base.copy(furnitures = base.furnitures.map { f ->
            when (f.category) {
                FurnCategory.BED -> placeAwayFromDoor(f, base)
                FurnCategory.DESK -> placeNearWindow(f, base)
                else -> snapToWall(f, base)
            }
        })
        val b = base.copy(furnitures = base.furnitures.map { f ->
            when (f.category) {
                FurnCategory.BED -> centerOnWall(f, base)
                FurnCategory.DESK -> sideWall(f, base)
                else -> snapToWall(f, base)
            }
        })
        val c = tidyLayout(base)

        recommendations = listOf(
            Recommendation("A", "A안 · Cozy", "침대는 문에서 멀리, 책상은 창가. 휴식/업무 분리.", a),
            Recommendation("B", "B안 · Minimal", "큰 가구 중앙 정렬로 정돈감.", b),
            Recommendation("C", "C안 · Tidy", "겹침 제거 + 벽 스냅.", c)
        )
    }

    fun chooseRecommendation(rec: Recommendation) {
        selectedRec = rec
        floorPlan = rec.plan
    }

    fun buildShoppingForSelection(): List<CatalogItem> {
        val rec = selectedRec ?: return emptyList()
        val cats = rec.plan.furnitures.map { it.category }.toSet()
        val pool = demoCatalog().items.filter { it.category in cats }
        return pool
            .sortedByDescending { it.styleTags.intersect(styleTags).size }
            .groupBy { it.category }
            .flatMap { (_, lst) -> lst.take(3) }
    }

    // ---------- 배치 휴리스틱 ----------
    private fun snapToWall(f: Furniture, fp: FloorPlan, padding: Float = 8f): Furniture {
        val r = f.rect; val b = fp.bounds
        val gaps = listOf(
            r.left - b.left to "L", b.right - r.right to "R",
            r.top - b.top to "T", b.bottom - r.bottom to "B"
        )
        val (minGap, side) = gaps.minBy { it.first }
        val dx: Float; val dy: Float
        when (side) {
            "L" -> { dx = -(minGap - padding); dy = 0f }
            "R" -> { dx = (minGap - padding); dy = 0f }
            "T" -> { dx = 0f; dy = -(minGap - padding) }
            else -> { dx = 0f; dy = (minGap - padding) }
        }
        return f.copy(rect = RectF(r.left + dx, r.top + dy, r.right + dx, r.bottom + dy))
    }

    private fun placeNearWindow(f: Furniture, fp: FloorPlan): Furniture {
        val win = fp.windows.firstOrNull() ?: return snapToWall(f, fp)
        val r = f.rect; val b = fp.bounds
        val tx = (win.rect.centerX() - r.width()/2f)
            .coerceIn(b.left + 8f, b.right - r.width() - 8f)
        val ty = (win.rect.bottom + 16f)
            .coerceIn(b.top + 8f, b.bottom - r.height() - 8f)
        return f.copy(rect = RectF(tx, ty, tx + r.width(), ty + r.height()))
    }

    private fun placeAwayFromDoor(f: Furniture, fp: FloorPlan): Furniture {
        val door = fp.doors.firstOrNull(); val r = f.rect; val b = fp.bounds
        val corners = listOf(
            RectF(b.left+8f, b.top+8f, b.left+8f + r.width(), b.top+8f + r.height()),
            RectF(b.right - r.width()-8f, b.top+8f, b.right-8f, b.top+8f + r.height()),
            RectF(b.left+8f, b.bottom - r.height()-8f, b.left+8f + r.width(), b.bottom-8f),
            RectF(b.right - r.width()-8f, b.bottom - r.height()-8f, b.right-8f, b.bottom-8f)
        )
        if (door == null) return f.copy(rect = corners.last())
        val best = corners.maxBy { c ->
            val cx = c.centerX(); val cy = c.centerY()
            val dx = (door.rect.centerX() - cx); val dy = (door.rect.centerY() - cy)
            dx*dx + dy*dy
        }
        return f.copy(rect = best)
    }

    private fun centerOnWall(f: Furniture, fp: FloorPlan): Furniture {
        val r = f.rect; val b = fp.bounds
        return if (b.width() >= b.height()) {
            val x = b.centerX() - r.width()/2f
            f.copy(rect = RectF(x, b.top+8f, x + r.width(), b.top+8f + r.height()))
        } else {
            val y = b.centerY() - r.height()/2f
            f.copy(rect = RectF(b.left+8f, y, b.left+8f + r.width(), y + r.height()))
        }
    }

    private fun sideWall(f: Furniture, fp: FloorPlan): Furniture {
        val r = f.rect; val b = fp.bounds
        val y = (b.centerY() - r.height()/2f)
            .coerceIn(b.top+8f, b.bottom - r.height() - 8f)
        return f.copy(rect = RectF(b.right - r.width() - 8f, y, b.right - 8f, y + r.height()))
    }

    private fun tidyLayout(fp: FloorPlan): FloorPlan {
        var current = fp.copy(furnitures = fp.furnitures.map { snapToWall(it, fp, 6f) })
        repeat(6) {
            val list = current.furnitures.toMutableList()
            var changed = false
            for (i in 0 until list.size) for (j in i + 1 until list.size) {
                val a = list[i].rect; val b = list[j].rect
                if (RectF.intersects(a, b)) {
                    val dy = max(6f, (a.bottom - b.top) + 6f)
                    list[j] = list[j].copy(
                        rect = RectF(b.left, b.top + dy, b.right, b.bottom + dy)
                    )
                    changed = true
                }
            }
            current = current.copy(furnitures = list)
            if (!changed) return@repeat
        }
        return current
    }

    // ---------- 더미 카탈로그 ----------
    private fun demoCatalog() = StyleCatalog(
        items = listOf(
            CatalogItem(
                "bed_min_01", "로우프레임 침대", FurnCategory.BED,
                setOf("미니멀", "밝은"), 1500, 2000, 299000,
                listOf(ShopLink("N스탠드", "https://example.com/bed_min_01"))
            ),
            CatalogItem(
                "bed_warm_01", "우드톤 침대", FurnCategory.BED,
                setOf("우드톤", "아늑"), 1600, 2000, 459000,
                listOf(ShopLink("W몰", "https://example.com/bed_warm_01"))
            ),
            CatalogItem(
                "desk_min_01", "화이트 데스크 1200", FurnCategory.DESK,
                setOf("미니멀", "밝은"), 1200, 600, 149000,
                listOf(ShopLink("A샵", "https://example.com/desk_min_01"))
            ),
            CatalogItem(
                "desk_walnut_01", "월넛 데스크 1400", FurnCategory.DESK,
                setOf("우드톤", "모던"), 1400, 700, 259000,
                listOf(ShopLink("B샵", "https://example.com/desk_walnut_01"))
            ),
            CatalogItem(
                "sofa_min_01", "2인 미니멀 소파", FurnCategory.SOFA,
                setOf("미니멀", "밝은"), 1600, 800, 329000,
                listOf(ShopLink("C샵", "https://example.com/sofa_min_01"))
            ),
            CatalogItem(
                "ward_min_01", "미닫이 옷장 1200", FurnCategory.WARDROBE,
                setOf("미니멀"), 1200, 600, 279000,
                listOf(ShopLink("D샵", "https://example.com/ward_min_01"))
            ),
            CatalogItem(
                "table_light_01", "원형 테이블 800", FurnCategory.TABLE,
                setOf("밝은", "캐주얼"), 800, 800, 99000,
                listOf(ShopLink("E샵", "https://example.com/table_light_01"))
            )
        )
    )
}
