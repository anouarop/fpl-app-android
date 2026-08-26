package com.shellanddeploy.fpllive.ui.standings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shellanddeploy.fpllive.data.api.FetchResult
import com.shellanddeploy.fpllive.data.api.FplRepository
import com.shellanddeploy.fpllive.data.datastore.Settings
import com.shellanddeploy.fpllive.data.datastore.SettingsRepository
import com.shellanddeploy.fpllive.domain.model.Entry
import com.shellanddeploy.fpllive.domain.model.GameweekScore
import com.shellanddeploy.fpllive.domain.model.SeasonHistory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StandingsUiState(
    val teamId: Int = 0,
    val entry: Entry? = null,
    val gameweeks: List<GameweekScore> = emptyList(),
    val seasons: List<SeasonHistory> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
    val stale: Boolean = false,
)

class StandingsViewModel(
    private val repository: FplRepository,
    private val settings: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(StandingsUiState())
    val state: StateFlow<StandingsUiState> = _state.asStateFlow()

    private var teamId = Settings.DEFAULT_TEAM_ID

    init {
        viewModelScope.launch {
            teamId = settings.settings.first().defaultTeamId
            _state.update { it.copy(teamId = teamId) }
            load()
        }
    }

    fun loadTeam(id: Int) {
        if (id <= 0) return
        teamId = id
        _state.update { it.copy(teamId = id, loading = true, error = null) }
        load()
    }

    fun retry() {
        _state.update { it.copy(loading = true, error = null) }
        load()
    }

    private fun load() {
        viewModelScope.launch {
            when (val r = repository.entry(teamId)) {
                is FetchResult.Success -> _state.update { it.copy(entry = r.data, stale = r.stale) }
                is FetchResult.Error -> _state.update { it.copy(error = r.message, stale = true) }
            }
            when (val r = repository.history(teamId)) {
                is FetchResult.Success -> _state.update {
                    it.copy(gameweeks = r.data.gameweeks, seasons = r.data.seasons, loading = false, stale = it.stale || r.stale)
                }
                is FetchResult.Error -> _state.update { it.copy(loading = false, error = r.message, stale = true) }
            }
        }
    }
}
