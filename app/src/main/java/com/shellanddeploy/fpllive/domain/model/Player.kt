package com.shellanddeploy.fpllive.domain.model

/** A player (FPL "element") exposed to the UI. Values are already typed (no raw API strings). */
data class Player(
    val id: Int,
    val code: Int,
    val webName: String,
    val firstName: String,
    val secondName: String,
    val elementTypeId: Int,
    val teamId: Int,
    val nowCost: Int,
    val form: Double,
    val totalPoints: Int,
    val pointsPerGame: Double,
    val selectedByPercent: Double,
    val status: String,
    val epNext: Double,
    val epThis: Double,
    val chanceOfPlayingNextRound: Int?,
    val goalsScored: Int,
    val assists: Int,
    val cleanSheets: Int,
    val bonus: Int,
    val minutes: Int,
    val saves: Int,
    val yellowCards: Int,
    val redCards: Int,
    val ictIndex: Double,
    val news: String,
)

/** A Premier League club. */
data class Team(
    val id: Int,
    val name: String,
    val shortName: String,
    val code: Int,
)

/** A player position (FPL "element_type"). */
data class Position(
    val id: Int,
    val singularName: String,
    val singularNameShort: String,
    val pluralName: String,
)
