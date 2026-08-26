package com.shellanddeploy.fpllive.domain.model

/** A classic league (public or private). */
data class League(
    val id: Int,
    val name: String,
    val isPrivate: Boolean,
)

/** One ranked entry in a league table. */
data class LeagueRow(
    val rank: Int,
    val entry: Int,
    val entryName: String,
    val playerName: String,
    val total: Int,
)

/** A league's standings. */
data class LeagueStandings(
    val league: League,
    val rows: List<LeagueRow>,
)
