package com.shellanddeploy.fpllive.ui.fixtures

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shellanddeploy.fpllive.data.api.FetchResult
import com.shellanddeploy.fpllive.data.api.FplRepository
import com.shellanddeploy.fpllive.domain.model.Fixture
import com.shellanddeploy.fpllive.domain.model.Gameweek
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FixturesUiState(
    val events: List<Gameweek> = emptyList(),
    val selectedEventId: Int = 0,
    val fixtures: List<Fixture> = emptyList(),
    val teamShorts: Map<Int, String> = emptyMap(),
    val teamNames: Map<Int, String> = emptyMap(),
    val loading: Boolean = true,
    val error: String? = null,
    val stale: Boolean = false,
    val lastUpdated: Long? = null,
)

class FixturesViewModel(
    private val repository: FplRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(FixturesUiState())
    val state: StateFlow<FixturesUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            when (val r = repository.bootstrap()) {
                is FetchResult.Success -> {
                    val bootstrap = r.data
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
                }
                is FetchResult.Error -> {
                    _state.update { it.copy(loading = false, error = r.message, stale = true) }
                }
            }
        }
    }

    fun selectEvent(id: Int) {
        if (id == _state.value.selectedEventId) return
        _state.update { it.copy(selectedEventId = id, loading = true, error = null) }
        loadFixtures(id)
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
}
