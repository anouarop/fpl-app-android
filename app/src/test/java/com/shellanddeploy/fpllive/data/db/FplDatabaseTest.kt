package com.shellanddeploy.fpllive.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.shellanddeploy.fpllive.domain.model.Gameweek
import com.shellanddeploy.fpllive.domain.model.GameweekScore
import com.shellanddeploy.fpllive.domain.model.Player
import com.shellanddeploy.fpllive.domain.model.Team
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FplDatabaseTest {

    private lateinit var db: FplDatabase

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), FplDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun teardown() {
        db.close()
    }

    private val player = Player(
        id = 1, code = 1, webName = "Saka", firstName = "Bukayo", secondName = "Saka",
        elementTypeId = 3, teamId = 1, nowCost = 85, form = 7.5, totalPoints = 100,
        pointsPerGame = 5.0, selectedByPercent = 40.0, status = "a", epNext = 0.0, epThis = 0.0,
        chanceOfPlayingNextRound = null, goalsScored = 5, assists = 4, cleanSheets = 2, bonus = 3,
        minutes = 800, saves = 0, yellowCards = 1, redCards = 0, ictIndex = 100.0, news = "",
    )

    @Test
    fun `players insert and read back`() = runBlocking {
        db.playerDao().insertAll(listOf(player.toEntity()))
        assertEquals("Saka", db.playerDao().getAll().single().webName)
    }

    @Test
    fun `player flow emits inserted rows`() = runBlocking {
        db.playerDao().insertAll(listOf(player.toEntity()))
        assertEquals(1, db.playerDao().observeAll().first().size)
    }

    @Test
    fun `teams and gameweeks persist`() = runBlocking {
        db.teamDao().insertAll(listOf(Team(id = 1, name = "Arsenal", shortName = "ARS", code = 3).toEntity()))
        db.gameweekDao().insertAll(
            listOf(Gameweek(id = 1, name = "Gameweek 1", finished = false, isCurrent = true, isNext = false, deadlineTime = "2026-08-15T10:00:00Z", deadlineTimeEpoch = 0, averageEntryScore = 0, highestScore = null).toEntity()),
        )
        assertEquals(1, db.teamDao().getAll().size)
        assertEquals(1, db.gameweekDao().getAll().size)
    }

    @Test
    fun `fixtures are scoped by event`() = runBlocking {
        db.fixtureDao().insertAll(
            listOf(
                com.shellanddeploy.fpllive.domain.model.Fixture(id = 1, event = 1, teamH = 1, teamA = 2, teamHDifficulty = 2, teamADifficulty = 3, teamHScore = null, teamAScore = null, started = false, finished = false, finishedProvisional = false, minutes = 0, kickoffTime = "2026-08-15T12:00:00Z", pulseId = 0).toEntity(),
                com.shellanddeploy.fpllive.domain.model.Fixture(id = 2, event = 2, teamH = 3, teamA = 4, teamHDifficulty = 2, teamADifficulty = 2, teamHScore = null, teamAScore = null, started = false, finished = false, finishedProvisional = false, minutes = 0, kickoffTime = "2026-08-22T12:00:00Z", pulseId = 0).toEntity(),
            ),
        )
        assertEquals(1, db.fixtureDao().getForEvent(1).size)
        assertEquals(2, db.fixtureDao().getForEvent(2).first().id)
    }

    @Test
    fun `gameweek scores round-trip with entry scoping`() = runBlocking {
        val score = GameweekScore(event = 1, points = 72, totalPoints = 72, rank = 10, overallRank = 20, eventTransfers = 1, eventTransfersCost = 0, pointsOnBench = 5, chip = "wildcard")
        db.gameweekScoreDao().insertAll(listOf(score.toEntity(entryId = 7)))
        assertEquals(score, db.gameweekScoreDao().getForEntry(7).single().toDomain())
        assertNull(db.gameweekScoreDao().getForEntry(8).singleOrNull())
    }
}
