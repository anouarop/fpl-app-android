package com.shellanddeploy.fpllive.data.mapper

import com.shellanddeploy.fpllive.data.model.BootstrapDto
import com.shellanddeploy.fpllive.data.model.ChipDto
import com.shellanddeploy.fpllive.data.model.ElementDto
import com.shellanddeploy.fpllive.data.model.ElementSummaryDto
import com.shellanddeploy.fpllive.data.model.ElementTypeDto
import com.shellanddeploy.fpllive.data.model.EntryDto
import com.shellanddeploy.fpllive.data.model.EntryHistoryDto
import com.shellanddeploy.fpllive.data.model.EventDto
import com.shellanddeploy.fpllive.data.model.FixtureDto
import com.shellanddeploy.fpllive.data.model.GwHistoryDto
import com.shellanddeploy.fpllive.data.model.HistoryDto
import com.shellanddeploy.fpllive.data.model.LiveElementDto
import com.shellanddeploy.fpllive.data.model.LiveEventDto
import com.shellanddeploy.fpllive.data.model.LiveStatsDto
import com.shellanddeploy.fpllive.data.model.LeagueRowDto
import com.shellanddeploy.fpllive.data.model.LeagueStandingsDto
import com.shellanddeploy.fpllive.data.model.PickDto
import com.shellanddeploy.fpllive.data.model.PicksDto
import com.shellanddeploy.fpllive.data.model.PlayerFixtureDto
import com.shellanddeploy.fpllive.data.model.PlayerHistoryDto
import com.shellanddeploy.fpllive.data.model.PlayerHistoryPastDto
import com.shellanddeploy.fpllive.data.model.SeasonHistoryDto
import com.shellanddeploy.fpllive.data.model.TeamDto
import com.shellanddeploy.fpllive.data.model.TransferDto
import com.shellanddeploy.fpllive.domain.model.Bootstrap
import com.shellanddeploy.fpllive.domain.model.Chip
import com.shellanddeploy.fpllive.domain.model.Entry
import com.shellanddeploy.fpllive.domain.model.EntryHistory
import com.shellanddeploy.fpllive.domain.model.Fixture
import com.shellanddeploy.fpllive.domain.model.Gameweek
import com.shellanddeploy.fpllive.domain.model.GameweekScore
import com.shellanddeploy.fpllive.domain.model.LiveElement
import com.shellanddeploy.fpllive.domain.model.LiveEvent
import com.shellanddeploy.fpllive.domain.model.LiveStats
import com.shellanddeploy.fpllive.domain.model.League
import com.shellanddeploy.fpllive.domain.model.LeagueRow
import com.shellanddeploy.fpllive.domain.model.LeagueStandings
import com.shellanddeploy.fpllive.domain.model.Pick
import com.shellanddeploy.fpllive.domain.model.Picks
import com.shellanddeploy.fpllive.domain.model.Player
import com.shellanddeploy.fpllive.domain.model.PlayerFixture
import com.shellanddeploy.fpllive.domain.model.PlayerHistory
import com.shellanddeploy.fpllive.domain.model.PlayerHistoryPast
import com.shellanddeploy.fpllive.domain.model.PlayerSummary
import com.shellanddeploy.fpllive.domain.model.Position
import com.shellanddeploy.fpllive.domain.model.SeasonHistory
import com.shellanddeploy.fpllive.domain.model.Team
import com.shellanddeploy.fpllive.domain.model.TeamHistory
import com.shellanddeploy.fpllive.domain.model.Transfer

private fun String.toDoubleSafe(): Double = toDoubleOrNull() ?: 0.0

fun BootstrapDto.toDomain(): Bootstrap = Bootstrap(
    players = elements.map { it.toDomain() },
    teams = teams.map { it.toDomain() },
    positions = elementTypes.map { it.toDomain() },
    gameweeks = events.map { it.toDomain() },
)

fun ElementDto.toDomain(): Player = Player(
    id = id,
    code = code,
    webName = webName,
    firstName = firstName,
    secondName = secondName,
    elementTypeId = elementType,
    teamId = team,
    nowCost = nowCost,
    form = form.toDoubleSafe(),
    totalPoints = totalPoints,
    pointsPerGame = pointsPerGame.toDoubleSafe(),
    selectedByPercent = selectedByPercent.toDoubleSafe(),
    status = status,
    epNext = epNext.toDoubleSafe(),
    epThis = epThis.toDoubleSafe(),
    chanceOfPlayingNextRound = chanceOfPlayingNextRound,
    goalsScored = goalsScored,
    assists = assists,
    cleanSheets = cleanSheets,
    bonus = bonus,
    minutes = minutes,
    saves = saves,
    yellowCards = yellowCards,
    redCards = redCards,
    ictIndex = ictIndex.toDoubleSafe(),
    news = news,
)

fun TeamDto.toDomain(): Team = Team(
    id = id,
    name = name,
    shortName = shortName,
    code = code,
)

fun ElementTypeDto.toDomain(): Position = Position(
    id = id,
    singularName = singularName,
    singularNameShort = singularNameShort,
    pluralName = pluralName,
)

fun EventDto.toDomain(): Gameweek = Gameweek(
    id = id,
    name = name,
    finished = finished,
    isCurrent = isCurrent,
    isNext = isNext,
    deadlineTime = deadlineTime,
    deadlineTimeEpoch = deadlineTimeEpoch,
    averageEntryScore = averageEntryScore,
    highestScore = highestScore,
)

fun FixtureDto.toDomain(): Fixture = Fixture(
    id = id,
    event = event,
    teamH = teamH,
    teamA = teamA,
    teamHDifficulty = teamHDifficulty,
    teamADifficulty = teamADifficulty,
    teamHScore = teamHScore,
    teamAScore = teamAScore,
    started = started,
    finished = finished,
    finishedProvisional = finishedProvisional,
    minutes = minutes,
    kickoffTime = kickoffTime,
    pulseId = pulseId,
)

fun LiveEventDto.toDomain(): LiveEvent = LiveEvent(
    elements = elements.map { it.toDomain() },
)

fun LiveElementDto.toDomain(): LiveElement = LiveElement(
    id = id,
    stats = stats.toDomain(),
)

fun LiveStatsDto.toDomain(): LiveStats = LiveStats(
    minutes = minutes,
    goalsScored = goalsScored,
    assists = assists,
    cleanSheets = cleanSheets,
    goalsConceded = goalsConceded,
    ownGoals = ownGoals,
    penaltiesSaved = penaltiesSaved,
    penaltiesMissed = penaltiesMissed,
    yellowCards = yellowCards,
    redCards = redCards,
    saves = saves,
    bonus = bonus,
    bps = bps,
    totalPoints = totalPoints,
    inDreamteam = inDreamteam,
    played = played,
)

fun EntryDto.toDomain(): Entry = Entry(
    id = id,
    name = name,
    playerFirstName = playerFirstName,
    playerLastName = playerLastName,
    summaryOverallPoints = summaryOverallPoints,
    summaryOverallRank = summaryOverallRank,
    summaryEventPoints = summaryEventPoints,
    currentEvent = currentEvent,
    startedEvent = startedEvent,
    lastDeadlineBank = lastDeadlineBank,
    lastDeadlineValue = lastDeadlineValue,
)

fun PickDto.toDomain(): Pick = Pick(
    element = element,
    position = position,
    multiplier = multiplier,
    isCaptain = isCaptain,
    isViceCaptain = isViceCaptain,
    elementType = elementType,
)

fun PicksDto.toDomain(): Picks = Picks(
    picks = picks.map { it.toDomain() },
    entryHistory = entryHistory?.toDomain(),
    activeChip = activeChip,
)

fun EntryHistoryDto.toDomain(): EntryHistory = EntryHistory(
    event = event,
    points = points,
    totalPoints = totalPoints,
    rank = rank,
    overallRank = overallRank,
    bank = bank,
    value = value,
    eventTransfers = eventTransfers,
    eventTransfersCost = eventTransfersCost,
    pointsOnBench = pointsOnBench,
)

fun GwHistoryDto.toDomain(): GameweekScore = GameweekScore(
    event = event,
    points = points,
    totalPoints = totalPoints,
    rank = rank,
    overallRank = overallRank,
    eventTransfers = eventTransfers,
    eventTransfersCost = eventTransfersCost,
    pointsOnBench = pointsOnBench,
    chip = chip,
)

fun SeasonHistoryDto.toDomain(): SeasonHistory = SeasonHistory(
    seasonName = seasonName,
    totalPoints = totalPoints,
    rank = rank,
)

fun ChipDto.toDomain(): Chip = Chip(
    name = name,
    time = time,
    event = event,
)

fun TransferDto.toDomain(): Transfer = Transfer(
    elementIn = elementIn,
    elementOut = elementOut,
    entry = entry,
    event = event,
    time = time,
)

fun HistoryDto.toDomain(): TeamHistory = TeamHistory(
    gameweeks = current.map { it.toDomain() },
    seasons = past.map { it.toDomain() },
    chips = chips.map { it.toDomain() },
)

fun LeagueStandingsDto.toDomain(): LeagueStandings = LeagueStandings(
    league = League(
        id = league.id,
        name = league.name,
        isPrivate = league.codePrivacy == "p",
    ),
    rows = standings.results.map { it.toDomain() },
)

fun LeagueRowDto.toDomain(): LeagueRow = LeagueRow(
    rank = rank,
    entry = entry,
    entryName = entryName,
    playerName = playerName,
    total = total,
)

fun ElementSummaryDto.toDomain(): PlayerSummary = PlayerSummary(
    fixtures = fixtures.map { it.toDomain() },
    history = history.map { it.toDomain() },
    historyPast = historyPast.map { it.toDomain() },
)

fun PlayerFixtureDto.toDomain(): PlayerFixture = PlayerFixture(
    id = id,
    event = event,
    teamH = teamH,
    teamA = teamA,
    isHome = isHome,
    difficulty = difficulty,
    eventName = eventName,
    kickoffTime = kickoffTime,
    finished = finished,
)

fun PlayerHistoryDto.toDomain(): PlayerHistory = PlayerHistory(
    fixture = fixture,
    opponentTeam = opponentTeam,
    totalPoints = totalPoints,
    wasHome = wasHome,
    round = round,
    minutes = minutes,
    goalsScored = goalsScored,
    assists = assists,
    cleanSheets = cleanSheets,
    bonus = bonus,
)

fun PlayerHistoryPastDto.toDomain(): PlayerHistoryPast = PlayerHistoryPast(
    seasonName = seasonName,
    totalPoints = totalPoints,
    startCost = startCost,
    endCost = endCost,
    minutes = minutes,
    goalsScored = goalsScored,
    assists = assists,
    cleanSheets = cleanSheets,
    bonus = bonus,
)
