package com.shellanddeploy.fpllive.ui.gameweeks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shellanddeploy.fpllive.data.api.FplRepository
import com.shellanddeploy.fpllive.domain.model.Gameweek
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GameweeksUiState(
    val gameweeks: List<Gameweek> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
)

class GameweeksViewModel(
    private val repository: FplRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(GameweeksUiState())
    val state: StateFlow<GameweeksUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeBootstrap().collect { b ->
                _state.update { it.copy(gameweeks = b?.gameweeks ?: emptyList(), loading = b == null) }
            }
        }
    }

    fun retry() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch { repository.bootstrap() }
    }
}
