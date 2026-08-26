package com.shellanddeploy.fpllive.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "players")
data class PlayerEntity(
    @PrimaryKey val id: Int,
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

@Entity(tableName = "teams")
data class TeamEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val shortName: String,
    val code: Int,
)

@Entity(tableName = "positions")
data class PositionEntity(
    @PrimaryKey val id: Int,
    val singularName: String,
    val singularNameShort: String,
    val pluralName: String,
)

@Entity(tableName = "gameweeks")
data class GameweekEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val finished: Boolean,
    val isCurrent: Boolean,
    val isNext: Boolean,
    val deadlineTime: String,
    val deadlineTimeEpoch: Long,
    val averageEntryScore: Int,
    val highestScore: Int?,
)

@Entity(
    tableName = "fixtures",
    indices = [Index("event")],
)
data class FixtureEntity(
    @PrimaryKey val id: Int,
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

@Entity(tableName = "entries")
data class EntryEntity(
    @PrimaryKey val id: Int,
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

@Entity(
    tableName = "gameweek_scores",
    primaryKeys = ["entryId", "event"],
)
data class GameweekScoreEntity(
    val entryId: Int,
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

/** Tracks when a cache key was last refreshed from the network (for staleness decisions). */
@Entity(tableName = "cache_meta")
data class CacheMetaEntity(
    @PrimaryKey val key: String,
    val updatedAt: Long,
)
