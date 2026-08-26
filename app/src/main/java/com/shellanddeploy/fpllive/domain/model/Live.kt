package com.shellanddeploy.fpllive.domain.model

/** Live gameweek data (network-only, never persisted). */
data class LiveEvent(
    val elements: List<LiveElement>,
)

data class LiveElement(
    val id: Int,
    val stats: LiveStats,
)

data class LiveStats(
    val minutes: Int,
    val goalsScored: Int,
    val assists: Int,
    val cleanSheets: Int,
    val goalsConceded: Int,
    val ownGoals: Int,
    val penaltiesSaved: Int,
    val penaltiesMissed: Int,
    val yellowCards: Int,
    val redCards: Int,
    val saves: Int,
    val bonus: Int,
    val bps: Int,
    val totalPoints: Int,
    val inDreamteam: Boolean,
    val played: Boolean,
)
