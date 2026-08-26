package com.shellanddeploy.fpllive.util

import com.shellanddeploy.fpllive.domain.model.Player

/**
 * Pure, unit-testable player browsing logic (no Android dependencies).
 * Filters the full player pool by position and a name query, then sorts.
 */
object PlayerListLogic {

    enum class Sort { POINTS, PRICE, FORM, SELECTED }

    fun apply(
        players: List<Player>,
        query: String,
        positionId: Int?,
        sort: Sort,
    ): List<Player> {
        val q = query.trim().lowercase()
        val filtered = players.filter { p ->
            (positionId == null || p.elementTypeId == positionId) &&
                (q.isEmpty() || p.webName.lowercase().contains(q))
        }
        return when (sort) {
            Sort.POINTS -> filtered.sortedByDescending { it.totalPoints }
            Sort.PRICE -> filtered.sortedByDescending { it.nowCost }
            Sort.FORM -> filtered.sortedByDescending { it.form }
            Sort.SELECTED -> filtered.sortedByDescending { it.selectedByPercent }
        }
    }
}
