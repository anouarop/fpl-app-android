package com.shellanddeploy.fpllive.data.db

import com.shellanddeploy.fpllive.domain.model.Entry
import com.shellanddeploy.fpllive.domain.model.Fixture
import com.shellanddeploy.fpllive.domain.model.Gameweek
import com.shellanddeploy.fpllive.domain.model.GameweekScore
import com.shellanddeploy.fpllive.domain.model.Player
import com.shellanddeploy.fpllive.domain.model.Position
import com.shellanddeploy.fpllive.domain.model.Team

fun Player.toEntity(): PlayerEntity = PlayerEntity(
    id = id,
    code = code,
    webName = webName,
    firstName = firstName,
    secondName = secondName,
    elementTypeId = elementTypeId,
    teamId = teamId,
    nowCost = nowCost,
    form = form,
    totalPoints = totalPoints,
    pointsPerGame = pointsPerGame,
    selectedByPercent = selectedByPercent,
    status = status,
    epNext = epNext,
    epThis = epThis,
    chanceOfPlayingNextRound = chanceOfPlayingNextRound,
    goalsScored = goalsScored,
    assists = assists,
    cleanSheets = cleanSheets,
    bonus = bonus,
    minutes = minutes,
    saves = saves,
    yellowCards = yellowCards,
    redCards = redCards,
    ictIndex = ictIndex,
    news = news,
)

fun PlayerEntity.toDomain(): Player = Player(
    id = id,
    code = code,
    webName = webName,
    firstName = firstName,
    secondName = secondName,
    elementTypeId = elementTypeId,
    teamId = teamId,
    nowCost = nowCost,
    form = form,
    totalPoints = totalPoints,
    pointsPerGame = pointsPerGame,
    selectedByPercent = selectedByPercent,
    status = status,
    epNext = epNext,
    epThis = epThis,
    chanceOfPlayingNextRound = chanceOfPlayingNextRound,
    goalsScored = goalsScored,
    assists = assists,
    cleanSheets = cleanSheets,
    bonus = bonus,
    minutes = minutes,
    saves = saves,
    yellowCards = yellowCards,
    redCards = redCards,
    ictIndex = ictIndex,
    news = news,
)

fun Team.toEntity(): TeamEntity = TeamEntity(id = id, name = name, shortName = shortName, code = code)

fun TeamEntity.toDomain(): Team = Team(id = id, name = name, shortName = shortName, code = code)

fun Position.toEntity(): PositionEntity = PositionEntity(
    id = id,
    singularName = singularName,
    singularNameShort = singularNameShort,
    pluralName = pluralName,
)

fun PositionEntity.toDomain(): Position = Position(
    id = id,
    singularName = singularName,
    singularNameShort = singularNameShort,
    pluralName = pluralName,
)

fun Gameweek.toEntity(): GameweekEntity = GameweekEntity(
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

fun GameweekEntity.toDomain(): Gameweek = Gameweek(
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

fun Fixture.toEntity(): FixtureEntity = FixtureEntity(
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

fun FixtureEntity.toDomain(): Fixture = Fixture(
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

fun Entry.toEntity(): EntryEntity = EntryEntity(
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

fun EntryEntity.toDomain(): Entry = Entry(
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

fun GameweekScore.toEntity(entryId: Int): GameweekScoreEntity = GameweekScoreEntity(
    entryId = entryId,
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

fun GameweekScoreEntity.toDomain(): GameweekScore = GameweekScore(
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
