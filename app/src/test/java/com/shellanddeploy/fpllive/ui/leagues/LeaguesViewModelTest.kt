package com.shellanddeploy.fpllive.ui.leagues

import com.shellanddeploy.fpllive.data.api.FetchResult
import com.shellanddeploy.fpllive.domain.model.League
import com.shellanddeploy.fpllive.domain.model.LeagueRow
import com.shellanddeploy.fpllive.domain.model.LeagueStandings
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LeaguesViewModelTest {

    private val standings = LeagueStandings(
        league = League(id = 313, name = "Overall", isPrivate = false),
        rows = listOf(LeagueRow(rank = 1, entry = 9166708, entryName = "My Team", playerName = "John", total = 100)),
    )

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loads league standings`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repo = FakeFplRepository().apply { leagueStandingsResult = FetchResult.Success(standings) }

        val vm = LeaguesViewModel(repo)
        vm.loadLeague(313)
        advanceUntilIdle()

        val state = vm.state.value
        assertEquals("Overall", state.standings?.league?.name)
        assertEquals(1, state.standings?.rows?.size)
        assertFalse(state.loading)
    }

    @Test
    fun `ignores invalid league id`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val vm = LeaguesViewModel(FakeFplRepository())
        vm.loadLeague(0)
        advanceUntilIdle()

        assertNull(vm.state.value.standings)
    }

    @Test
    fun `error surfaces message`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repo = FakeFplRepository().apply { leagueStandingsResult = FetchResult.Error(null, "offline") }

        val vm = LeaguesViewModel(repo)
        vm.loadLeague(313)
        advanceUntilIdle()

        assertNull(vm.state.value.standings)
        assertTrue(vm.state.value.error == "offline")
        assertFalse(vm.state.value.loading)
    }
}
