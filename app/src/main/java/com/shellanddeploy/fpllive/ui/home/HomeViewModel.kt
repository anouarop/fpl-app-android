package com.shellanddeploy.fpllive.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shellanddeploy.fpllive.data.api.FetchResult
import com.shellanddeploy.fpllive.data.api.FplRepository
import com.shellanddeploy.fpllive.data.datastore.Settings
import com.shellanddeploy.fpllive.data.datastore.SettingsRepository
import com.shellanddeploy.fpllive.domain.model.Bootstrap
import com.shellanddeploy.fpllive.domain.model.Entry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val bootstrap: Bootstrap? = null,
    val entry: Entry? = null,
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: String? = null,
    val stale: Boolean = false,
    val lastUpdated: Long? = null,
    val liveFixtures: Int = 0,
) {
    val currentGameweek get() = bootstrap?.currentGameweek
    val topPlayers get() = bootstrap?.players?.sortedByDescending { it.totalPoints }?.take(5) ?: emptyList()
}

class HomeViewModel(
    private val repository: FplRepository,
    private val settings: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private var defaultTeamId = Settings.DEFAULT_TEAM_ID

    init {
        viewModelScope.launch {
            settings.settings.collect { defaultTeamId = it.defaultTeamId }
        }
        viewModelScope.launch {
            repository.observeBootstrap().collect { b ->
                _state.update { it.copy(bootstrap = b, loading = b == null) }
            }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(refreshing = true, error = null) }
            var bootstrap: Bootstrap? = null
            when (val r = repository.bootstrap()) {
                is FetchResult.Success -> {
                    bootstrap = r.data
                    _state.update { it.copy(stale = r.stale, lastUpdated = System.currentTimeMillis()) }
                }
                is FetchResult.Error -> _state.update { it.copy(error = r.message, stale = true) }
            }
            loadEntry()
            loadLiveFixtures(bootstrap?.currentGameweek?.id)
            _state.update { it.copy(refreshing = false) }
        }
    }

    private suspend fun loadEntry() {
        when (val r = repository.entry(defaultTeamId)) {
            is FetchResult.Success -> _state.update { it.copy(entry = r.data, stale = it.stale || r.stale) }
            else -> Unit
        }
    }

    private suspend fun loadLiveFixtures(eventId: Int?) {
        if (eventId == null) return
        when (val r = repository.fixtures(eventId)) {
            is FetchResult.Success -> {
                val live = r.data.count { it.started && !it.finished && !it.finishedProvisional }
                _state.update { it.copy(liveFixtures = live) }
            }
            else -> Unit
        }
    }
}
