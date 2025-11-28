package com.example.roommade.ui

import com.example.roommade.model.FloorPlan
import com.example.roommade.model.FurnCategory
import com.example.roommade.model.korLabel

fun planLabelsFor(plan: FloorPlan): List<String> {
    val counters = mutableMapOf<FurnCategory, Int>()
    val out = ArrayList<String>(plan.furnitures.size)
    plan.furnitures.forEach { f ->
        val next = (counters[f.category] ?: 0) + 1
        counters[f.category] = next
        out += f.category.korLabel() + next.toString()
    }
    return out
}
