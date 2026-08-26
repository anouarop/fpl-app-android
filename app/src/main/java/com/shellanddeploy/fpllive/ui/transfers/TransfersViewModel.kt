package com.shellanddeploy.fpllive.ui.transfers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shellanddeploy.fpllive.data.api.FetchResult
import com.shellanddeploy.fpllive.data.api.FplRepository
import com.shellanddeploy.fpllive.data.datastore.Settings
import com.shellanddeploy.fpllive.data.datastore.SettingsRepository
import com.shellanddeploy.fpllive.domain.model.Transfer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TransfersUiState(
    val teamId: Int = 0,
    val transfers: List<Transfer> = emptyList(),
    val playerNames: Map<Int, String> = emptyMap(),
    val loading: Boolean = true,
    val error: String? = null,
    val stale: Boolean = false,
    val lastUpdated: Long? = null,
)

class TransfersViewModel(
    private val repository: FplRepository,
    private val settings: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(TransfersUiState())
    val state: StateFlow<TransfersUiState> = _state.asStateFlow()

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
            val bootstrap = when (val r = repository.bootstrap()) {
                is FetchResult.Success -> r.data
                else -> null
            }
            _state.update {
                it.copy(playerNames = bootstrap?.players?.associate { p -> p.id to p.webName } ?: emptyMap())
            }
            when (val r = repository.transfers(teamId)) {
                is FetchResult.Success -> _state.update {
                    it.copy(transfers = r.data, loading = false, stale = r.stale, lastUpdated = System.currentTimeMillis())
                }
                is FetchResult.Error -> _state.update {
                    it.copy(loading = false, error = r.message, stale = true)
                }
            }
        }
    }
}
