package com.shellanddeploy.fpllive.data.api

import androidx.room.withTransaction
import com.shellanddeploy.fpllive.data.db.CacheMetaEntity
import com.shellanddeploy.fpllive.data.db.FplDatabase
import com.shellanddeploy.fpllive.data.db.toDomain
import com.shellanddeploy.fpllive.data.db.toEntity
import com.shellanddeploy.fpllive.data.mapper.toDomain
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/**
 * Cache-aside repository backed by a persistent Room cache and an in-memory TTL layer.
 *
 * Read path: memory TTL -> Room (if fresh) -> network (persist to Room). Network failures fall
 * back to the most recent cached copy (marked stale). A per-key mutex coalesces concurrent
 * requests so a given resource is only fetched from the network once at a time.
 */
class FplRepositoryImpl(
    private val api: FplApi,
    private val db: FplDatabase,
) : FplRepository {

    private val ttl = TtlCache()
    private val locks = ConcurrentHashMap<String, Mutex>()

    private val playerDao get() = db.playerDao()
    private val teamDao get() = db.teamDao()
    private val positionDao get() = db.positionDao()
    private val gameweekDao get() = db.gameweekDao()
    private val fixtureDao get() = db.fixtureDao()
    private val entryDao get() = db.entryDao()
    private val gameweekScoreDao get() = db.gameweekScoreDao()
    private val cacheMetaDao get() = db.cacheMetaDao()

    companion object {
        const val TTL_BOOTSTRAP = 10 * 60 * 1000L
        const val TTL_FIXTURES = 10 * 60 * 1000L
        const val TTL_ENTRY = 10 * 60 * 1000L
        const val TTL_LIVE = 20 * 1000L
        const val TTL_SUMMARY = 10 * 60 * 1000L
        const val TTL_HISTORY = 10 * 60 * 1000L
        const val TTL_TRANSFERS = 10 * 60 * 1000L

        private const val KEY_BOOTSTRAP = "bootstrap"
        private const val KEY_LIVE = "live"
        private const val KEY_LIVE_FIXTURES = "livefixtures"
        private const val KEY_FIXTURES = "fixtures"
        private const val KEY_ENTRY = "entry"
        private const val KEY_PICKS = "picks"
        private const val KEY_HISTORY = "history"
        private const val KEY_SUMMARY = "summary"
        private const val KEY_TRANSFERS = "transfers"
        private const val KEY_LEAGUE = "league"
    }

    override fun observeBootstrap(): Flow<Bootstrap?> =
        combine(
            playerDao.observeAll(),
            teamDao.observeAll(),
            positionDao.observeAll(),
            gameweekDao.observeAll(),
        ) { players, teams, positions, gameweeks ->
            if (players.isEmpty()) {
                null
            } else {
                Bootstrap(
                    players = players.map { it.toDomain() },
                    teams = teams.map { it.toDomain() },
                    positions = positions.map { it.toDomain() },
                    gameweeks = gameweeks.map { it.toDomain() },
                )
            }
        }

    override suspend fun bootstrap(): FetchResult<Bootstrap> {
        ttl.get<Bootstrap>(KEY_BOOTSTRAP)?.let { return FetchResult.Success(it) }
        val cached = readBootstrapFromRoom()
        if (cached != null && isFresh(KEY_BOOTSTRAP, TTL_BOOTSTRAP)) {
            ttl.put(KEY_BOOTSTRAP, cached, TTL_BOOTSTRAP)
            return FetchResult.Success(cached)
        }
        return withKeyLock(KEY_BOOTSTRAP) {
            ttl.get<Bootstrap>(KEY_BOOTSTRAP)?.let { return@withKeyLock FetchResult.Success(it) }
            val current = readBootstrapFromRoom()
            if (current != null && isFresh(KEY_BOOTSTRAP, TTL_BOOTSTRAP)) {
                ttl.put(KEY_BOOTSTRAP, current, TTL_BOOTSTRAP)
                return@withKeyLock FetchResult.Success(current)
            }
            try {
                val domain = api.bootstrap().toDomain()
                db.withTransaction {
                    playerDao.clear()
                    playerDao.insertAll(domain.players.map { it.toEntity() })
                    teamDao.clear()
                    teamDao.insertAll(domain.teams.map { it.toEntity() })
                    positionDao.clear()
                    positionDao.insertAll(domain.positions.map { it.toEntity() })
                    gameweekDao.clear()
                    gameweekDao.insertAll(domain.gameweeks.map { it.toEntity() })
                    cacheMetaDao.upsert(CacheMetaEntity(KEY_BOOTSTRAP, System.currentTimeMillis()))
                }
                ttl.put(KEY_BOOTSTRAP, domain, TTL_BOOTSTRAP)
                FetchResult.Success(domain)
            } catch (e: Exception) {
                current?.let {
                    ttl.put(KEY_BOOTSTRAP, it, TTL_BOOTSTRAP)
                    FetchResult.Success(it, stale = true)
                } ?: FetchResult.Error(null, e.message ?: "Network error")
            }
        }
    }

    override suspend fun live(eventId: Int): FetchResult<LiveEvent> =
        fetchWithMemoryTtl("$KEY_LIVE:$eventId", TTL_LIVE) { api.eventLive(eventId).toDomain() }

    override suspend fun fixtures(eventId: Int): FetchResult<List<Fixture>> {
        val key = "$KEY_FIXTURES:$eventId"
        ttl.get<List<Fixture>>(key)?.let { return FetchResult.Success(it) }
        val cached = fixtureDao.getForEvent(eventId).map { it.toDomain() }
        if (cached.isNotEmpty() && isFresh(key, TTL_FIXTURES)) {
            ttl.put(key, cached, TTL_FIXTURES)
            return FetchResult.Success(cached)
        }
        return withKeyLock(key) {
            ttl.get<List<Fixture>>(key)?.let { return@withKeyLock FetchResult.Success(it) }
            val current = fixtureDao.getForEvent(eventId).map { it.toDomain() }
            if (current.isNotEmpty() && isFresh(key, TTL_FIXTURES)) {
                ttl.put(key, current, TTL_FIXTURES)
                return@withKeyLock FetchResult.Success(current)
            }
            try {
                val domain = api.fixtures(eventId).map { it.toDomain() }
                db.withTransaction {
                    fixtureDao.clearForEvent(eventId)
                    fixtureDao.insertAll(domain.map { it.toEntity() })
                    cacheMetaDao.upsert(CacheMetaEntity(key, System.currentTimeMillis()))
                }
                ttl.put(key, domain, TTL_FIXTURES)
                FetchResult.Success(domain)
            } catch (e: Exception) {
                if (current.isNotEmpty()) {
                    ttl.put(key, current, TTL_FIXTURES)
                    FetchResult.Success(current, stale = true)
                } else {
                    FetchResult.Error(null, e.message ?: "Network error")
                }
            }
        }
    }

    override suspend fun liveFixtures(eventId: Int): FetchResult<List<Fixture>> =
        fetchWithMemoryTtl("$KEY_LIVE_FIXTURES:$eventId", TTL_LIVE) { api.fixtures(eventId).map { it.toDomain() } }

    override suspend fun entry(teamId: Int): FetchResult<Entry> {
        val key = "$KEY_ENTRY:$teamId"
        ttl.get<Entry>(key)?.let { return FetchResult.Success(it) }
        val cached = entryDao.getById(teamId)?.toDomain()
        if (cached != null && isFresh(key, TTL_ENTRY)) {
            ttl.put(key, cached, TTL_ENTRY)
            return FetchResult.Success(cached)
        }
        return withKeyLock(key) {
            ttl.get<Entry>(key)?.let { return@withKeyLock FetchResult.Success(it) }
            val current = entryDao.getById(teamId)?.toDomain()
            if (current != null && isFresh(key, TTL_ENTRY)) {
                ttl.put(key, current, TTL_ENTRY)
                return@withKeyLock FetchResult.Success(current)
            }
            try {
                val domain = api.entry(teamId).toDomain()
                db.withTransaction {
                    entryDao.upsert(domain.toEntity())
                    cacheMetaDao.upsert(CacheMetaEntity(key, System.currentTimeMillis()))
                }
                ttl.put(key, domain, TTL_ENTRY)
                FetchResult.Success(domain)
            } catch (e: Exception) {
                current?.let {
                    ttl.put(key, it, TTL_ENTRY)
                    FetchResult.Success(it, stale = true)
                } ?: FetchResult.Error(null, e.message ?: "Network error")
            }
        }
    }

    override suspend fun picks(teamId: Int, eventId: Int): FetchResult<Picks> =
        fetchWithMemoryTtl("$KEY_PICKS:$teamId:$eventId", TTL_LIVE) { api.picks(teamId, eventId).toDomain() }

    override suspend fun history(teamId: Int): FetchResult<TeamHistory> {
        val key = "$KEY_HISTORY:$teamId"
        ttl.get<TeamHistory>(key)?.let { return FetchResult.Success(it) }
        val cached = readHistoryFromRoom(teamId)
        if (cached != null && isFresh(key, TTL_HISTORY)) {
            ttl.put(key, cached, TTL_HISTORY)
            return FetchResult.Success(cached)
        }
        return withKeyLock(key) {
            ttl.get<TeamHistory>(key)?.let { return@withKeyLock FetchResult.Success(it) }
            val current = readHistoryFromRoom(teamId)
            if (current != null && isFresh(key, TTL_HISTORY)) {
                ttl.put(key, current, TTL_HISTORY)
                return@withKeyLock FetchResult.Success(current)
            }
            try {
                val domain = api.history(teamId).toDomain()
                db.withTransaction {
                    gameweekScoreDao.clearForEntry(teamId)
                    gameweekScoreDao.insertAll(domain.gameweeks.map { it.toEntity(teamId) })
                    cacheMetaDao.upsert(CacheMetaEntity(key, System.currentTimeMillis()))
                }
                ttl.put(key, domain, TTL_HISTORY)
                FetchResult.Success(domain)
            } catch (e: Exception) {
                current?.let {
                    ttl.put(key, it, TTL_HISTORY)
                    FetchResult.Success(it, stale = true)
                } ?: FetchResult.Error(null, e.message ?: "Network error")
            }
        }
    }

    override suspend fun elementSummary(playerId: Int): FetchResult<PlayerSummary> =
        fetchWithMemoryTtl("$KEY_SUMMARY:$playerId", TTL_SUMMARY) { api.elementSummary(playerId).toDomain() }

    override suspend fun transfers(teamId: Int): FetchResult<List<Transfer>> =
        fetchWithMemoryTtl("$KEY_TRANSFERS:$teamId", TTL_TRANSFERS) { api.transfers(teamId).map { it.toDomain() } }

    override suspend fun leagueStandings(leagueId: Int): FetchResult<LeagueStandings> =
        fetchWithMemoryTtl("$KEY_LEAGUE:$leagueId", TTL_TRANSFERS) { api.leaguesClassicStandings(leagueId).toDomain() }

    override suspend fun clearCache() {
        db.withTransaction {
            cacheMetaDao.clear()
            playerDao.clear()
            teamDao.clear()
            positionDao.clear()
            gameweekDao.clear()
            fixtureDao.clearAll()
            entryDao.clearAll()
            gameweekScoreDao.clearAll()
        }
        ttl.clear()
    }

    private suspend fun readBootstrapFromRoom(): Bootstrap? {
        val players = playerDao.getAll()
        if (players.isEmpty()) return null
        return Bootstrap(
            players = players.map { it.toDomain() },
            teams = teamDao.getAll().map { it.toDomain() },
            positions = positionDao.getAll().map { it.toDomain() },
            gameweeks = gameweekDao.getAll().map { it.toDomain() },
        )
    }

    private suspend fun readHistoryFromRoom(teamId: Int): TeamHistory? {
        val gameweeks = gameweekScoreDao.getForEntry(teamId).map { it.toDomain() }
        if (gameweeks.isEmpty()) return null
        return TeamHistory(gameweeks = gameweeks, seasons = emptyList(), chips = emptyList())
    }

    private suspend fun isFresh(key: String, ttlMillis: Long): Boolean {
        val updated = cacheMetaDao.get(key) ?: return false
        return System.currentTimeMillis() - updated < ttlMillis
    }

    private suspend fun <T> withKeyLock(key: String, block: suspend () -> T): T {
        val mutex = locks.computeIfAbsent(key) { Mutex() }
        return mutex.withLock { block() }
    }

    private suspend fun <T : Any> fetchWithMemoryTtl(key: String, ttlMillis: Long, fetch: suspend () -> T): FetchResult<T> {
        val cached = ttl.get<T>(key)
        return withKeyLock(key) {
            ttl.get<T>(key)?.let { return@withKeyLock FetchResult.Success(it) }
            try {
                val value = fetch()
                ttl.put(key, value, ttlMillis)
                FetchResult.Success(value)
            } catch (e: Exception) {
                if (cached != null) FetchResult.Success(cached, stale = true)
                else FetchResult.Error(null, e.message ?: "Network error")
            }
        }
    }
}
