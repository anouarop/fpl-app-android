package com.shellanddeploy.fpllive.data.api

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

/** Result of a fetch that distinguishes fresh data, stale (cached) data and errors. */
sealed class FetchResult<out T> {
    data class Success<T>(val data: T, val stale: Boolean = false) : FetchResult<T>()
    data class Error<T>(val cached: T?, val message: String) : FetchResult<T>()
}

/**
 * Read-only data access for the FPL API. Implementations own caching (persistent Room cache +
 * an in-memory TTL layer) and return [domain model]s — never raw API DTOs.
 *
 * The public FPL API is unauthenticated and read-only. Anything that needs writes or
 * authentication (making transfers, private leagues, sign-in) is not available here and is
 * marked TODO/VERIFY at the feature layer.
 */
interface FplRepository {

    /** Reactively observes the cached bootstrap index (players, teams, positions, gameweeks). */
    fun observeBootstrap(): Flow<Bootstrap?>

    /** Cache-first bootstrap: returns fresh cached data or refreshes from the network. */
    suspend fun bootstrap(): FetchResult<Bootstrap>

    suspend fun live(eventId: Int): FetchResult<LiveEvent>

    suspend fun fixtures(eventId: Int): FetchResult<List<Fixture>>

    /** Fixtures fetched on the short live cadence (in-memory only, never persisted). */
    suspend fun liveFixtures(eventId: Int): FetchResult<List<Fixture>>

    suspend fun entry(teamId: Int): FetchResult<Entry>

    suspend fun picks(teamId: Int, eventId: Int): FetchResult<Picks>

    suspend fun history(teamId: Int): FetchResult<TeamHistory>

    suspend fun elementSummary(playerId: Int): FetchResult<PlayerSummary>

    suspend fun transfers(teamId: Int): FetchResult<List<Transfer>>

    /** Standings for a classic league by id (public leagues only; private leagues need auth). */
    suspend fun leagueStandings(leagueId: Int): FetchResult<LeagueStandings>

    /** Clears both the in-memory TTL cache and the persistent Room cache. */
    suspend fun clearCache()
}
