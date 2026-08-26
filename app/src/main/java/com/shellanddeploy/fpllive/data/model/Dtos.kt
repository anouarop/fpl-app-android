package com.shellanddeploy.fpllive.data.model

import kotlinx.serialization.Serializable

@Serializable
data class BootstrapDto(
    val elements: List<ElementDto> = emptyList(),
    val teams: List<TeamDto> = emptyList(),
    val elementTypes: List<ElementTypeDto> = emptyList(),
    val events: List<EventDto> = emptyList(),
)

@Serializable
data class ElementDto(
    val id: Int,
    val code: Int = 0,
    val webName: String = "",
    val firstName: String = "",
    val secondName: String = "",
    val elementType: Int = 0,
    val team: Int = 0,
    val nowCost: Int = 0,
    val form: String = "0.0",
    val totalPoints: Int = 0,
    val pointsPerGame: String = "0.0",
    val selectedByPercent: String = "0.0",
    val status: String = "a",
    val epNext: String = "0.0",
    val epThis: String = "0.0",
    val chanceOfPlayingNextRound: Int? = null,
    val goalsScored: Int = 0,
    val assists: Int = 0,
    val cleanSheets: Int = 0,
    val bonus: Int = 0,
    val minutes: Int = 0,
    val saves: Int = 0,
    val yellowCards: Int = 0,
    val redCards: Int = 0,
    val ictIndex: String = "0.0",
    val news: String = "",
)

@Serializable
data class TeamDto(
    val id: Int,
    val name: String = "",
    val shortName: String = "",
    val code: Int = 0,
)

@Serializable
data class ElementTypeDto(
    val id: Int,
    val singularName: String = "",
    val singularNameShort: String = "",
    val pluralName: String = "",
)

@Serializable
data class EventDto(
    val id: Int,
    val name: String = "",
    val finished: Boolean = false,
    val isCurrent: Boolean = false,
    val isNext: Boolean = false,
    val deadlineTime: String = "",
    val deadlineTimeEpoch: Long = 0,
    val averageEntryScore: Int = 0,
    val highestScore: Int? = null,
)

@Serializable
data class LiveEventDto(
    val elements: List<LiveElementDto> = emptyList(),
)

@Serializable
data class LiveElementDto(
    val id: Int,
    val stats: LiveStatsDto = LiveStatsDto(),
    val explain: List<LiveExplainDto> = emptyList(),
)

@Serializable
data class LiveStatsDto(
    val minutes: Int = 0,
    val goalsScored: Int = 0,
    val assists: Int = 0,
    val cleanSheets: Int = 0,
    val goalsConceded: Int = 0,
    val ownGoals: Int = 0,
    val penaltiesSaved: Int = 0,
    val penaltiesMissed: Int = 0,
    val yellowCards: Int = 0,
    val redCards: Int = 0,
    val saves: Int = 0,
    val bonus: Int = 0,
    val bps: Int = 0,
    val totalPoints: Int = 0,
    val inDreamteam: Boolean = false,
    val played: Boolean = false,
)

@Serializable
data class LiveExplainDto(
    val fixture: Int = 0,
    val stats: List<ExplainStatDto> = emptyList(),
)

@Serializable
data class ExplainStatDto(
    val identifier: String = "",
    val points: Int = 0,
    val value: Int = 0,
)

@Serializable
data class FixtureDto(
    val id: Int,
    val event: Int? = null,
    val teamH: Int = 0,
    val teamA: Int = 0,
    val teamHDifficulty: Int = 0,
    val teamADifficulty: Int = 0,
    val teamHScore: Int? = null,
    val teamAScore: Int? = null,
    val started: Boolean = false,
    val finished: Boolean = false,
    val finishedProvisional: Boolean = false,
    val minutes: Int = 0,
    val kickoffTime: String = "",
    val pulseId: Int = 0,
    val stats: List<FixtureStatDto> = emptyList(),
)

@Serializable
data class FixtureStatDto(
    val identifier: String = "",
    val h: List<FixtureStatValueDto> = emptyList(),
    val a: List<FixtureStatValueDto> = emptyList(),
)

@Serializable
data class FixtureStatValueDto(
    val value: Int = 0,
    val element: Int = 0,
)

@Serializable
data class EntryDto(
    val id: Int,
    val name: String = "",
    val playerFirstName: String = "",
    val playerLastName: String = "",
    val summaryOverallPoints: Int = 0,
    val summaryOverallRank: Int = 0,
    val summaryEventPoints: Int = 0,
    val currentEvent: Int = 0,
    val startedEvent: Int = 0,
    val lastDeadlineBank: Int? = null,
    val lastDeadlineValue: Int? = null,
)

@Serializable
data class PicksDto(
    val picks: List<PickDto> = emptyList(),
    val entryHistory: EntryHistoryDto? = null,
    val activeChip: String? = null,
)

@Serializable
data class PickDto(
    val element: Int,
    val position: Int,
    val multiplier: Int = 1,
    val isCaptain: Boolean = false,
    val isViceCaptain: Boolean = false,
    val elementType: Int? = null,
)

@Serializable
data class EntryHistoryDto(
    val event: Int = 0,
    val points: Int = 0,
    val totalPoints: Int = 0,
    val rank: Int = 0,
    val overallRank: Int = 0,
    val bank: Int = 0,
    val value: Int = 0,
    val eventTransfers: Int = 0,
    val eventTransfersCost: Int = 0,
    val pointsOnBench: Int = 0,
)

@Serializable
data class HistoryDto(
    val current: List<GwHistoryDto> = emptyList(),
    val past: List<SeasonHistoryDto> = emptyList(),
    val chips: List<ChipDto> = emptyList(),
)

@Serializable
data class GwHistoryDto(
    val event: Int = 0,
    val points: Int = 0,
    val totalPoints: Int = 0,
    val rank: Int = 0,
    val overallRank: Int = 0,
    val eventTransfers: Int = 0,
    val eventTransfersCost: Int = 0,
    val pointsOnBench: Int = 0,
    val chip: String? = null,
)

@Serializable
data class SeasonHistoryDto(
    val seasonName: String = "",
    val totalPoints: Int = 0,
    val rank: Int = 0,
)

@Serializable
data class ChipDto(
    val name: String = "",
    val time: String = "",
    val event: Int = 0,
)

@Serializable
data class ElementSummaryDto(
    val fixtures: List<PlayerFixtureDto> = emptyList(),
    val history: List<PlayerHistoryDto> = emptyList(),
    val historyPast: List<PlayerHistoryPastDto> = emptyList(),
)

@Serializable
data class PlayerFixtureDto(
    val id: Int,
    val event: Int? = null,
    val teamH: Int = 0,
    val teamA: Int = 0,
    val isHome: Boolean = false,
    val difficulty: Int = 0,
    val eventName: String = "",
    val kickoffTime: String = "",
    val teamHScore: Int? = null,
    val teamAScore: Int? = null,
    val finished: Boolean = false,
)

@Serializable
data class PlayerHistoryDto(
    val element: Int = 0,
    val fixture: Int = 0,
    val opponentTeam: Int = 0,
    val totalPoints: Int = 0,
    val wasHome: Boolean = false,
    val kickoffTime: String = "",
    val teamHScore: Int? = null,
    val teamAScore: Int? = null,
    val round: Int = 0,
    val minutes: Int = 0,
    val goalsScored: Int = 0,
    val assists: Int = 0,
    val cleanSheets: Int = 0,
    val goalsConceded: Int = 0,
    val ownGoals: Int = 0,
    val penaltiesSaved: Int = 0,
    val penaltiesMissed: Int = 0,
    val yellowCards: Int = 0,
    val redCards: Int = 0,
    val saves: Int = 0,
    val bonus: Int = 0,
    val bps: Int = 0,
    val value: Int = 0,
    val starts: Int = 0,
    val selected: Int = 0,
)

@Serializable
data class PlayerHistoryPastDto(
    val seasonName: String = "",
    val totalPoints: Int = 0,
    val startCost: Int = 0,
    val endCost: Int = 0,
    val minutes: Int = 0,
    val goalsScored: Int = 0,
    val assists: Int = 0,
    val cleanSheets: Int = 0,
    val bonus: Int = 0,
)

@Serializable
data class TransferDto(
    val elementIn: Int = 0,
    val elementOut: Int = 0,
    val entry: Int = 0,
    val event: Int = 0,
    val time: String = "",
)

@Serializable
data class LeagueStandingsDto(
    val league: LeagueInfoDto = LeagueInfoDto(),
    val standings: LeaguePageDto = LeaguePageDto(),
)

@Serializable
data class LeagueInfoDto(
    val id: Int = 0,
    val name: String = "",
    val leagueType: String = "",
    val codePrivacy: String = "",
)

@Serializable
data class LeaguePageDto(
    val hasNext: Boolean = false,
    val page: Int = 0,
    val results: List<LeagueRowDto> = emptyList(),
)

@Serializable
data class LeagueRowDto(
    val rank: Int = 0,
    val lastRank: Int = 0,
    val entry: Int = 0,
    val entryName: String = "",
    val playerName: String = "",
    val total: Int = 0,
    val eventTotal: Int = 0,
)
