package com.shellanddeploy.fpllive.domain.model

/** The real-time live feed (from the companion live-events service). */
data class LiveFeed(
    val event: Int,
    val updated: Long,
    val matches: List<LiveMatch>,
)

/** One fixture with its live score and real-time events. */
data class LiveMatch(
    val id: Int,
    val kickoff: String,
    val homeShort: String,
    val awayShort: String,
    val homeScore: Int,
    val awayScore: Int,
    val status: String,
    val live: Boolean,
    val events: List<LiveMatchEvent>,
)

/** A single live event (goal, card, assist…) mapped to an FPL player. */
data class LiveMatchEvent(
    val minute: Int,
    val type: String,
    val isHome: Boolean,
    val playerName: String,
    val playerPoints: Int,
    val assistName: String?,
    val assistPoints: Int?,
    val detail: String?,
)
