package com.shellanddeploy.fpllive.testutil

import com.shellanddeploy.fpllive.data.api.FetchResult
import com.shellanddeploy.fpllive.data.api.FplRepository
import com.shellanddeploy.fpllive.domain.model.Bootstrap
import com.shellanddeploy.fpllive.domain.model.Entry
import com.shellanddeploy.fpllive.domain.model.Fixture
import com.shellanddeploy.fpllive.domain.model.LiveEvent
import com.shellanddeploy.fpllive.domain.model.LeagueStandings
import com.shellanddeploy.fpllive.domain.model.Picks
import com.shellanddeploy.fpllive.domain.model.PlayerSummary
import com.shellanddeploy.fpllive.domain.model.TeamHistory
import com.shellanddeploy.fpllive.domain.model.Transfer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeFplRepository : FplRepository {

    var bootstrapResult: FetchResult<Bootstrap> = FetchResult.Error(null, "no data")
    var fixturesResult: FetchResult<List<Fixture>> = FetchResult.Error(null, "no data")
    var entryResult: FetchResult<Entry> = FetchResult.Error(null, "no data")
    var historyResult: FetchResult<TeamHistory> = FetchResult.Error(null, "no data")
    var transfersResult: FetchResult<List<Transfer>> = FetchResult.Error(null, "no data")
    var leagueStandingsResult: FetchResult<LeagueStandings> = FetchResult.Error(null, "no data")

    private val bootstrapFlow = MutableStateFlow<Bootstrap?>(null)

    override fun observeBootstrap(): Flow<Bootstrap?> = bootstrapFlow

    override suspend fun bootstrap(): FetchResult<Bootstrap> = bootstrapResult

    override suspend fun live(eventId: Int): FetchResult<LiveEvent> = FetchResult.Error(null, "no data")

    override suspend fun fixtures(eventId: Int): FetchResult<List<Fixture>> = fixturesResult

    override suspend fun liveFixtures(eventId: Int): FetchResult<List<Fixture>> = fixturesResult

    override suspend fun entry(teamId: Int): FetchResult<Entry> = entryResult

    override suspend fun picks(teamId: Int, eventId: Int): FetchResult<Picks> = FetchResult.Error(null, "no data")

    override suspend fun history(teamId: Int): FetchResult<TeamHistory> = historyResult

    override suspend fun elementSummary(playerId: Int): FetchResult<PlayerSummary> = FetchResult.Error(null, "no data")

    override suspend fun transfers(teamId: Int): FetchResult<List<Transfer>> = transfersResult

    override suspend fun leagueStandings(leagueId: Int): FetchResult<LeagueStandings> = leagueStandingsResult

    override suspend fun clearCache() = Unit
}
