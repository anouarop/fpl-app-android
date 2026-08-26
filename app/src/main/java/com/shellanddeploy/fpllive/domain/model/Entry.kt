package com.shellanddeploy.fpllive.domain.model

/** A manager's FPL team summary. */
data class Entry(
    val id: Int,
    val name: String,
    val playerFirstName: String,
    val playerLastName: String,
    val summaryOverallPoints: Int,
    val summaryOverallRank: Int,
    val summaryEventPoints: Int,
    val currentEvent: Int,
    val startedEvent: Int,
    val lastDeadlineBank: Int?,
    val lastDeadlineValue: Int?,
)

/** A single squad pick (player) for a gameweek. */
data class Pick(
    val element: Int,
    val position: Int,
    val multiplier: Int,
    val isCaptain: Boolean,
    val isViceCaptain: Boolean,
    val elementType: Int?,
)

/** A team's picks + their gameweek entry history. */
data class Picks(
    val picks: List<Pick>,
    val entryHistory: EntryHistory?,
    val activeChip: String?,
)

/** Per-gameweek result for a team (entry history). */
data class EntryHistory(
    val event: Int,
    val points: Int,
    val totalPoints: Int,
    val rank: Int,
    val overallRank: Int,
    val bank: Int,
    val value: Int,
    val eventTransfers: Int,
    val eventTransfersCost: Int,
    val pointsOnBench: Int,
)

/** One gameweek row from a team's full season history. */
data class GameweekScore(
    val event: Int,
    val points: Int,
    val totalPoints: Int,
    val rank: Int,
    val overallRank: Int,
    val eventTransfers: Int,
    val eventTransfersCost: Int,
    val pointsOnBench: Int,
    val chip: String?,
)

/** A finished season's summary from a team's history. */
data class SeasonHistory(
    val seasonName: String,
    val totalPoints: Int,
    val rank: Int,
)

/** A chip (wildcard, bench boost, etc.) used in a past gameweek. */
data class Chip(
    val name: String,
    val time: String,
    val event: Int,
)

/** A transfer made by a manager (read-only history). */
data class Transfer(
    val elementIn: Int,
    val elementOut: Int,
    val entry: Int,
    val event: Int,
    val time: String,
)

/** A team's full season history (current gameweeks + past seasons + chips). */
data class TeamHistory(
    val gameweeks: List<GameweekScore>,
    val seasons: List<SeasonHistory>,
    val chips: List<Chip>,
)
