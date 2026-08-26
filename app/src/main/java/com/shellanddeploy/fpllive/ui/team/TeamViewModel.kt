package com.shellanddeploy.fpllive.ui.team

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shellanddeploy.fpllive.data.api.FetchResult
import com.shellanddeploy.fpllive.data.api.FplRepository
import com.shellanddeploy.fpllive.data.datastore.SettingsRepository
import com.shellanddeploy.fpllive.domain.model.Bootstrap
import com.shellanddeploy.fpllive.domain.model.Entry
import com.shellanddeploy.fpllive.domain.model.EntryHistory
import com.shellanddeploy.fpllive.domain.model.Fixture
import com.shellanddeploy.fpllive.domain.model.GameweekScore
import com.shellanddeploy.fpllive.domain.model.Pick
import com.shellanddeploy.fpllive.domain.model.Player
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

data class TeamUiState(
    val teamId: Int = 0,
    val entry: Entry? = null,
    val loading: Boolean = true,
    val error: String? = null,
    val stale: Boolean = false,
    val lastUpdated: Long? = null,
    val picks: List<Pick> = emptyList(),
    val entryHistory: EntryHistory? = null,
    val history: List<GameweekScore> = emptyList(),
    val players: Map<Int, Player> = emptyMap(),
    val teamShorts: Map<Int, String> = emptyMap(),
    val teamNames: Map<Int, String> = emptyMap(),
    val positionNames: Map<Int, String> = emptyMap(),
    val livePoints: Map<Int, Int> = emptyMap(),
    val nextFixturesByTeam: Map<Int, Fixture> = emptyMap(),
    val currentEventId: Int = 0,
    val currentEventName: String = "",
    val liveNow: Boolean = false,
) {
    val xi: List<Pick> get() = picks.filter { it.position in 1..11 }.sortedBy { it.position }
    val bench: List<Pick> get() = picks.filter { it.position in 12..15 }.sortedBy { it.position }
    val liveTotal: Int
        get() = xi.sumOf { (livePoints[it.element] ?: 0) * it.multiplier }
}

class TeamViewModel(
    private val repository: FplRepository,
    private val settings: SettingsRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val teamIdArg: Int = savedStateHandle.get<Int>("teamId") ?: -1
    private var resolvedTeamId = 0

    private val _state = MutableStateFlow(TeamUiState())
    val state: StateFlow<TeamUiState> = _state.asStateFlow()

    private var bootstrap: Bootstrap? = null
    private var pollIntervalMs = 20_000L
    private var pollJob: Job? = null
    private var foreground = false
    private val inFlight = AtomicBoolean(false)

    init {
        viewModelScope.launch {
            val s = settings.settings.first()
            pollIntervalMs = s.pollIntervalSeconds * 1000L
            resolvedTeamId = if (teamIdArg > 0) teamIdArg else s.defaultTeamId
            _state.update { it.copy(teamId = resolvedTeamId) }
            load()
        }
    }

    fun loadTeam(id: Int) {
        if (id <= 0) return
        resolvedTeamId = id
        _state.update { it.copy(teamId = id, loading = true, error = null) }
        load()
    }

    private fun load() {
        viewModelScope.launch {
            if (bootstrap == null) {
                bootstrap = when (val r = repository.bootstrap()) {
                    is FetchResult.Success -> r.data
                    else -> null
                }
            }
            val b = bootstrap
            if (b == null) {
                _state.update { it.copy(loading = false, error = "Could not load data") }
                return@launch
            }
            val currentEvent = b.currentGameweek
            val nextEvent = b.gameweeks.firstOrNull { it.isNext }
            _state.update {
                it.copy(
                    players = b.players.associate { p -> p.id to p },
                    teamShorts = b.teams.associate { t -> t.id to t.shortName },
                    teamNames = b.teams.associate { t -> t.id to t.name },
                    positionNames = b.positions.associate { t -> t.id to t.singularNameShort },
                    currentEventId = currentEvent?.id ?: 0,
                    currentEventName = currentEvent?.name ?: "",
                )
            }

            loadEntry()
            loadHistory()
            loadPicks(currentEvent?.id ?: 0)
            loadFixtures(currentEvent?.id ?: 0, nextEvent?.id ?: 0)
        }
    }

    private fun loadEntry() {
        viewModelScope.launch {
            when (val r = repository.entry(resolvedTeamId)) {
                is FetchResult.Success -> _state.update { it.copy(entry = r.data, loading = false, stale = r.stale, lastUpdated = System.currentTimeMillis()) }
                is FetchResult.Error -> _state.update { it.copy(loading = false, error = r.message, stale = true) }
            }
        }
    }

    private fun loadHistory() {
        viewModelScope.launch {
            when (val r = repository.history(resolvedTeamId)) {
                is FetchResult.Success -> _state.update { it.copy(history = r.data.gameweeks.takeLast(5)) }
                else -> Unit
            }
        }
    }

    private fun loadPicks(eventId: Int) {
        if (eventId == 0) return
        viewModelScope.launch {
            when (val r = repository.picks(resolvedTeamId, eventId)) {
                is FetchResult.Success -> _state.update {
                    it.copy(picks = r.data.picks, entryHistory = r.data.entryHistory)
                }
                is FetchResult.Error -> _state.update { it.copy(picks = emptyList()) }
            }
        }
    }

    private fun loadFixtures(currentEventId: Int, nextEventId: Int) {
        viewModelScope.launch {
            if (currentEventId > 0) {
                when (val r = repository.fixtures(currentEventId)) {
                    is FetchResult.Success -> {
                        val liveNow = r.data.any { it.started && !it.finished && !it.finishedProvisional }
                        _state.update { it.copy(liveNow = liveNow) }
                    }
                    else -> Unit
                }
            }
            if (nextEventId > 0) {
                when (val r = repository.fixtures(nextEventId)) {
                    is FetchResult.Success -> {
                        val byTeam = buildMap {
                            r.data.forEach { f ->
                                put(f.teamH, f)
                                put(f.teamA, f)
                            }
                        }
                        _state.update { it.copy(nextFixturesByTeam = byTeam) }
                    }
                    else -> Unit
                }
            }
            refreshLive()
        }
    }

    private suspend fun refreshLive() {
        if (!inFlight.compareAndSet(false, true)) return
        try {
            val eventId = _state.value.currentEventId
            if (eventId == 0) return
            when (val r = repository.live(eventId)) {
                is FetchResult.Success -> {
                    val points = r.data.elements.associate { it.id to it.stats.totalPoints }
                    _state.update { it.copy(livePoints = points, stale = r.stale, lastUpdated = System.currentTimeMillis()) }
                }
                is FetchResult.Error -> _state.update { it.copy(stale = true) }
            }
        } finally {
            inFlight.set(false)
        }
    }

    fun startPolling() {
        foreground = true
        ensurePolling()
    }

    fun stopPolling() {
        foreground = false
        pollJob?.cancel()
        pollJob = null
    }

    private fun ensurePolling() {
        if (!foreground) return
        if (pollJob?.isActive == true) return
        pollJob = viewModelScope.launch {
            while (isActive) {
                if (_state.value.liveNow) refreshLive()
                delay(pollIntervalMs)
            }
        }
    }
}
