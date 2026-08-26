package com.shellanddeploy.fpllive.ui.players

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shellanddeploy.fpllive.data.api.FplRepository
import com.shellanddeploy.fpllive.domain.model.Bootstrap
import com.shellanddeploy.fpllive.domain.model.Player
import com.shellanddeploy.fpllive.util.PlayerListLogic
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlayersUiState(
    val bootstrap: Bootstrap? = null,
    val loading: Boolean = true,
    val error: String? = null,
    val query: String = "",
    val positionId: Int? = null,
    val sort: PlayerListLogic.Sort = PlayerListLogic.Sort.POINTS,
) {
    val positions get() = bootstrap?.positions ?: emptyList()
    val teamNames: Map<Int, String> get() = bootstrap?.teams?.associate { it.id to it.shortName } ?: emptyMap()
    val players: List<Player> get() = PlayerListLogic.apply(bootstrap?.players ?: emptyList(), query, positionId, sort)
}

class PlayersViewModel(
    private val repository: FplRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(PlayersUiState())
    val state: StateFlow<PlayersUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeBootstrap().collect { b ->
                _state.update { it.copy(bootstrap = b, loading = b == null) }
            }
        }
    }

    fun setQuery(q: String) = _state.update { it.copy(query = q) }

    fun setPosition(positionId: Int?) = _state.update { it.copy(positionId = positionId) }

    fun setSort(sort: PlayerListLogic.Sort) = _state.update { it.copy(sort = sort) }

    fun retry() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            repository.bootstrap()
        }
    }
}
