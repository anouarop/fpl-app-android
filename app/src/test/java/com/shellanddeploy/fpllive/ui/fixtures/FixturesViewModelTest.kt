package com.shellanddeploy.fpllive.ui.fixtures

import com.shellanddeploy.fpllive.data.api.FetchResult
import com.shellanddeploy.fpllive.domain.model.Bootstrap
import com.shellanddeploy.fpllive.domain.model.Fixture
import com.shellanddeploy.fpllive.domain.model.Gameweek
import com.shellanddeploy.fpllive.domain.model.Position
import com.shellanddeploy.fpllive.domain.model.Team
import com.shellanddeploy.fpllive.testutil.FakeFplRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FixturesViewModelTest {

    private val gw1 = Gameweek(id = 1, name = "Gameweek 1", finished = false, isCurrent = true, isNext = false, deadlineTime = "2026-08-15T10:00:00Z", deadlineTimeEpoch = 0, averageEntryScore = 0, highestScore = null)
    private val gw2 = Gameweek(id = 2, name = "Gameweek 2", finished = false, isCurrent = false, isNext = true, deadlineTime = "2026-08-22T10:00:00Z", deadlineTimeEpoch = 0, averageEntryScore = 0, highestScore = null)

    private val bootstrap = Bootstrap(
        players = emptyList(),
        teams = listOf(Team(id = 1, name = "Arsenal", shortName = "ARS", code = 3)),
        positions = listOf(Position(id = 1, singularName = "Goalkeeper", singularNameShort = "GKP", pluralName = "Goalkeepers")),
        gameweeks = listOf(gw1, gw2),
    )

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loads bootstrap and fixtures for current gameweek`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repo = FakeFplRepository().apply {
            bootstrapResult = FetchResult.Success(bootstrap)
            fixturesResult = FetchResult.Success(
                listOf(Fixture(id = 100, event = 1, teamH = 1, teamA = 2, teamHDifficulty = 2, teamADifficulty = 3, teamHScore = null, teamAScore = null, started = false, finished = false, finishedProvisional = false, minutes = 0, kickoffTime = "2026-08-15T12:00:00Z", pulseId = 0)),
            )
        }

        val vm = FixturesViewModel(repo)
        advanceUntilIdle()

        val state = vm.state.value
        assertEquals(1, state.selectedEventId)
        assertEquals(listOf("Gameweek 1", "Gameweek 2"), state.events.map { it.name })
        assertEquals("ARS", state.teamShorts[1])
        assertEquals(1, state.fixtures.size)
        assertFalse(state.loading)
    }

    @Test
    fun `selecting an event loads its fixtures`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repo = FakeFplRepository().apply {
            bootstrapResult = FetchResult.Success(bootstrap)
            fixturesResult = FetchResult.Success(emptyList())
        }

        val vm = FixturesViewModel(repo)
        advanceUntilIdle()

        vm.selectEvent(2)
        advanceUntilIdle()

        assertEquals(2, vm.state.value.selectedEventId)
        assertTrue(vm.state.value.fixtures.isEmpty())
    }

    @Test
    fun `error result surfaces error state`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repo = FakeFplRepository().apply {
            bootstrapResult = FetchResult.Error(null, "offline")
        }

        val vm = FixturesViewModel(repo)
        advanceUntilIdle()

        assertEquals(0, vm.state.value.selectedEventId)
        assertFalse(vm.state.value.loading)
    }
}
