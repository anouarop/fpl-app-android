package com.shellanddeploy.fpllive.ui.playerdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shellanddeploy.fpllive.data.api.FetchResult
import com.shellanddeploy.fpllive.data.api.FplRepository
import com.shellanddeploy.fpllive.data.datastore.SettingsRepository
import com.shellanddeploy.fpllive.domain.model.Bootstrap
import com.shellanddeploy.fpllive.domain.model.Fixture
import com.shellanddeploy.fpllive.domain.model.LiveStats
import com.shellanddeploy.fpllive.domain.model.Player
import com.shellanddeploy.fpllive.domain.model.PlayerFixture
import com.shellanddeploy.fpllive.domain.model.PlayerSummary
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

data class PlayerDetailUiState(
    val playerId: Int = 0,
    val player: Player? = null,
    val teamName: String = "",
    val teamShort: String = "",
    val position: String = "",
    val loading: Boolean = true,
    val error: String? = null,
    val stale: Boolean = false,
    val lastUpdated: Long? = null,
    val liveStats: LiveStats? = null,
    val liveFixture: Fixture? = null,
    val currentEventId: Int = 0,
    val currentEventName: String = "",
    val liveNow: Boolean = false,
    val matchInProgress: Boolean = false,
    val summary: PlayerSummary? = null,
    val nextFixture: PlayerFixture? = null,
    val teamShorts: Map<Int, String> = emptyMap(),
    val teamNames: Map<Int, String> = emptyMap(),
)

class PlayerDetailViewModel(
    private val repository: FplRepository,
    private val settings: SettingsRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val playerId: Int = savedStateHandle.get<Int>("playerId") ?: 0

    private val _state = MutableStateFlow(PlayerDetailUiState(playerId = playerId))
    val state: StateFlow<PlayerDetailUiState> = _state.asStateFlow()

    private var bootstrap: Bootstrap? = null
    private var pollIntervalMs = 20_000L
    private var pollJob: Job? = null
    private var foreground = false
    private val inFlight = AtomicBoolean(false)

    init {
        viewModelScope.launch {
            settings.settings.collect { s ->
                pollIntervalMs = s.pollIntervalSeconds * 1000L
            }
        }
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val b = when (val r = repository.bootstrap()) {
                is FetchResult.Success -> r.data
                else -> null
            }
            if (b == null) {
                _state.update { it.copy(loading = false, error = "Could not load player data") }
                return@launch
            }
            bootstrap = b
            val player = b.players.firstOrNull { it.id == playerId }
            if (player == null) {
                _state.update { it.copy(loading = false, error = "Player $playerId not found") }
                return@launch
            }
            val team = b.teams.firstOrNull { it.id == player.teamId }
            val type = b.positions.firstOrNull { it.id == player.elementTypeId }
            val currentEvent = b.currentGameweek

            _state.update {
                it.copy(
                    player = player,
                    teamName = team?.name ?: "",
                    teamShort = team?.shortName ?: "",
                    position = type?.singularNameShort ?: "",
                    currentEventId = currentEvent?.id ?: 0,
                    currentEventName = currentEvent?.name ?: "",
                    teamShorts = b.teams.associate { t -> t.id to t.shortName },
                    teamNames = b.teams.associate { t -> t.id to t.name },
                    loading = false,
                )
            }

            loadSummary(playerId)
            refreshStaticFixtures(player.teamId)
        }
    }

    private fun loadSummary(id: Int) {
        viewModelScope.launch {
            when (val r = repository.elementSummary(id)) {
                is FetchResult.Success -> {
                    val next = r.data.fixtures.firstOrNull { !it.finished }
                    _state.update { it.copy(summary = r.data, nextFixture = next) }
                }
                else -> Unit
            }
        }
    }

    private fun refreshStaticFixtures(teamId: Int) {
        val eventId = _state.value.currentEventId
        if (eventId == 0) return
        viewModelScope.launch {
            when (val r = repository.fixtures(eventId)) {
                is FetchResult.Success -> applyFixtures(teamId, r.data)
                else -> Unit
            }
            refreshLive()
        }
    }

    private fun applyFixtures(teamId: Int, fixtures: List<Fixture>) {
        val fixture = fixtures.firstOrNull { it.teamH == teamId || it.teamA == teamId }
        val matchInProgress = fixture?.started == true && fixture?.finished != true
        val liveNow = fixture?.started == true && fixture?.finished != true && fixture?.finishedProvisional != true
        _state.update {
            it.copy(
                liveFixture = fixture,
                matchInProgress = matchInProgress,
                liveNow = liveNow,
            )
        }
    }

    private suspend fun refreshLive() {
        if (!inFlight.compareAndSet(false, true)) return
        try {
            val eventId = _state.value.currentEventId
            val teamId = _state.value.player?.teamId ?: 0
            if (eventId > 0) {
                when (val r = repository.live(eventId)) {
                    is FetchResult.Success -> {
                        val stats = r.data.elements.firstOrNull { it.id == playerId }?.stats
                        _state.update {
                            it.copy(liveStats = stats, stale = r.stale, lastUpdated = System.currentTimeMillis())
                        }
                    }
                    is FetchResult.Error -> {
                        _state.update { it.copy(stale = true) }
                    }
                }
            }
            if (teamId > 0 && eventId > 0) {
                when (val r = repository.liveFixtures(eventId)) {
                    is FetchResult.Success -> applyFixtures(teamId, r.data)
                    else -> Unit
                }
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
                if (_state.value.liveNow) {
                    refreshLive()
                }
                delay(pollIntervalMs)
            }
        }
    }

    fun retry() {
        _state.update { it.copy(loading = true, error = null) }
        load()
    }
}
