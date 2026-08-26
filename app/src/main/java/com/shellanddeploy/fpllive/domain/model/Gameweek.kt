package com.shellanddeploy.fpllive.domain.model

/** A gameweek (FPL "event"). */
data class Gameweek(
    val id: Int,
    val name: String,
    val finished: Boolean,
    val isCurrent: Boolean,
    val isNext: Boolean,
    val deadlineTime: String,
    val deadlineTimeEpoch: Long,
    val averageEntryScore: Int,
    val highestScore: Int?,
)

/** A fixture between two teams, optionally scoped to a gameweek. */
data class Fixture(
    val id: Int,
    val event: Int?,
    val teamH: Int,
    val teamA: Int,
    val teamHDifficulty: Int,
    val teamADifficulty: Int,
    val teamHScore: Int?,
    val teamAScore: Int?,
    val started: Boolean,
    val finished: Boolean,
    val finishedProvisional: Boolean,
    val minutes: Int,
    val kickoffTime: String,
    val pulseId: Int,
)
