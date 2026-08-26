package com.shellanddeploy.fpllive.ui.gameweeks

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shellanddeploy.fpllive.data.api.FetchResult
import com.shellanddeploy.fpllive.data.api.FplRepository
import com.shellanddeploy.fpllive.domain.model.Fixture
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GameweekFixturesUiState(
    val gameweekId: Int = 0,
    val gameweekName: String = "",
    val deadlineTime: String = "",
    val fixtures: List<Fixture> = emptyList(),
    val teamShorts: Map<Int, String> = emptyMap(),
    val loading: Boolean = true,
    val error: String? = null,
    val stale: Boolean = false,
    val lastUpdated: Long? = null,
)

class GameweekFixturesViewModel(
    private val repository: FplRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val gameweekId: Int = savedStateHandle.get<Int>("gameweekId") ?: 0

    private val _state = MutableStateFlow(GameweekFixturesUiState(gameweekId = gameweekId))
    val state: StateFlow<GameweekFixturesUiState> = _state.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val bootstrap = when (val r = repository.bootstrap()) {
                is FetchResult.Success -> r.data
                else -> null
            }
            val gw = bootstrap?.gameweeks?.firstOrNull { it.id == gameweekId }
            _state.update {
                it.copy(
                    teamShorts = bootstrap?.teams?.associate { t -> t.id to t.shortName } ?: emptyMap(),
                    gameweekName = gw?.name ?: "Gameweek $gameweekId",
                    deadlineTime = gw?.deadlineTime ?: "",
                )
            }
            when (val r = repository.fixtures(gameweekId)) {
                is FetchResult.Success -> _state.update {
                    it.copy(fixtures = r.data, loading = false, stale = r.stale, lastUpdated = System.currentTimeMillis())
                }
                is FetchResult.Error -> _state.update {
                    it.copy(loading = false, error = r.message, stale = true)
                }
            }
        }
    }

    fun retry() {
        _state.update { it.copy(loading = true, error = null) }
        load()
    }
}
