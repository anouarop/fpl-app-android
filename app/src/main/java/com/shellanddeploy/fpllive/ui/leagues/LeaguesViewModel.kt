package com.shellanddeploy.fpllive.ui.leagues

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shellanddeploy.fpllive.data.api.FetchResult
import com.shellanddeploy.fpllive.data.api.FplRepository
import com.shellanddeploy.fpllive.domain.model.LeagueStandings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LeaguesUiState(
    val leagueId: Int = 0,
    val standings: LeagueStandings? = null,
    val loading: Boolean = false,
    val error: String? = null,
    val stale: Boolean = false,
    val lastUpdated: Long? = null,
)

class LeaguesViewModel(
    private val repository: FplRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(LeaguesUiState())
    val state: StateFlow<LeaguesUiState> = _state.asStateFlow()

    fun loadLeague(id: Int) {
        if (id <= 0) return
        _state.update { it.copy(leagueId = id, loading = true, error = null, standings = null) }
        viewModelScope.launch {
            when (val r = repository.leagueStandings(id)) {
                is FetchResult.Success -> _state.update {
                    it.copy(standings = r.data, loading = false, stale = r.stale, lastUpdated = System.currentTimeMillis())
                }
                is FetchResult.Error -> _state.update {
                    it.copy(loading = false, error = r.message, stale = true)
                }
            }
        }
    }
}
