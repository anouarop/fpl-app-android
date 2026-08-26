package com.shellanddeploy.fpllive.domain.model

/** A player's per-gameweek + fixture history (element-summary). */
data class PlayerSummary(
    val fixtures: List<PlayerFixture>,
    val history: List<PlayerHistory>,
    val historyPast: List<PlayerHistoryPast>,
)

/** An upcoming fixture from a player's perspective. */
data class PlayerFixture(
    val id: Int,
    val event: Int?,
    val teamH: Int,
    val teamA: Int,
    val isHome: Boolean,
    val difficulty: Int,
    val eventName: String,
    val kickoffTime: String,
    val finished: Boolean,
)

/** One finished gameweek from a player's history. */
data class PlayerHistory(
    val fixture: Int,
    val opponentTeam: Int,
    val totalPoints: Int,
    val wasHome: Boolean,
    val round: Int,
    val minutes: Int,
    val goalsScored: Int,
    val assists: Int,
    val cleanSheets: Int,
    val bonus: Int,
)

/** A finished season's summary for a player. */
data class PlayerHistoryPast(
    val seasonName: String,
    val totalPoints: Int,
    val startCost: Int,
    val endCost: Int,
    val minutes: Int,
    val goalsScored: Int,
    val assists: Int,
    val cleanSheets: Int,
    val bonus: Int,
)
