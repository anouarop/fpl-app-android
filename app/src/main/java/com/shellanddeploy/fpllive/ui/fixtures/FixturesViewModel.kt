package com.shellanddeploy.fpllive.ui.fixtures

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shellanddeploy.fpllive.data.api.FetchResult
import com.shellanddeploy.fpllive.data.api.FplRepository
import com.shellanddeploy.fpllive.data.datastore.Settings
import com.shellanddeploy.fpllive.domain.model.Fixture
import com.shellanddeploy.fpllive.domain.model.Gameweek
import com.shellanddeploy.fpllive.domain.model.Player
import com.shellanddeploy.fpllive.ui.components.OwnedPlayer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FixturesUiState(
    val events: List<Gameweek> = emptyList(),
    val selectedEventId: Int = 0,
    val fixtures: List<Fixture> = emptyList(),
    val teamShorts: Map<Int, String> = emptyMap(),
    val teamNames: Map<Int, String> = emptyMap(),
    val ownedPlayersByTeam: Map<Int, List<OwnedPlayer>> = emptyMap(),
    val squadNote: String? = null,
    val loading: Boolean = true,
    val error: String? = null,
    val stale: Boolean = false,
    val lastUpdated: Long? = null,
)

class FixturesViewModel(
    private val repository: FplRepository,
    settingsFlow: Flow<Settings>,
) : ViewModel() {

    private val _state = MutableStateFlow(FixturesUiState())
    val state: StateFlow<FixturesUiState> = _state.asStateFlow()

    private var defaultTeamId = 0
    private var players: Map<Int, Player> = emptyMap()

    init {
        viewModelScope.launch {
            defaultTeamId = settingsFlow.first().defaultTeamId
            when (val r = repository.bootstrap()) {
                is FetchResult.Success -> {
                    val bootstrap = r.data
                    players = bootstrap.players.associate { it.id to it }
                    val current = bootstrap.currentGameweek
                    _state.update {
                        it.copy(
                            events = bootstrap.gameweeks,
                            teamShorts = bootstrap.teams.associate { t -> t.id to t.shortName },
                            teamNames = bootstrap.teams.associate { t -> t.id to t.name },
                            selectedEventId = current?.id ?: (bootstrap.gameweeks.firstOrNull()?.id ?: 0),
                        )
                    }
                    loadFixtures(_state.value.selectedEventId)
                    loadSquad(_state.value.selectedEventId)
                }
                is FetchResult.Error -> {
                    _state.update { it.copy(loading = false, error = r.message, stale = true) }
                }
            }
        }
    }

    fun selectEvent(id: Int) {
        if (id == _state.value.selectedEventId) return
        _state.update {
            it.copy(
                selectedEventId = id,
                loading = true,
                error = null,
                ownedPlayersByTeam = emptyMap(),
                squadNote = null,
            )
        }
        loadFixtures(id)
        loadSquad(id)
    }

    private fun loadFixtures(eventId: Int) {
        if (eventId == 0) return
        viewModelScope.launch {
            when (val r = repository.fixtures(eventId)) {
                is FetchResult.Success -> _state.update {
                    it.copy(fixtures = r.data, loading = false, stale = r.stale, lastUpdated = System.currentTimeMillis())
                }
                is FetchResult.Error -> _state.update {
                    it.copy(loading = false, error = r.message, stale = true)
                }
            }
        }
    }

    private fun loadSquad(eventId: Int) {
        if (defaultTeamId <= 0 || eventId == 0) return
        viewModelScope.launch {
            when (val r = repository.picks(defaultTeamId, eventId)) {
                is FetchResult.Success -> {
                    val byTeam = r.data.picks
                        .mapNotNull { pick ->
                            players[pick.element]?.let { player ->
                                player.teamId to OwnedPlayer(player.webName, pick.isCaptain, pick.isViceCaptain)
                            }
                        }
                        .groupBy({ it.first }, { it.second })
                    _state.update {
                        it.copy(
                            ownedPlayersByTeam = byTeam,
                            squadNote = if (byTeam.isEmpty()) SQUAD_UNAVAILABLE_NOTE else null,
                        )
                    }
                }
                else -> _state.update {
                    it.copy(ownedPlayersByTeam = emptyMap(), squadNote = SQUAD_UNAVAILABLE_NOTE)
                }
            }
        }
    }

    companion object {
        private const val SQUAD_UNAVAILABLE_NOTE =
            "Squad players appear here once this gameweek's deadline passes."
    }
}
