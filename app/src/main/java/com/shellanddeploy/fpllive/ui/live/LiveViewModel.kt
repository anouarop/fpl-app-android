package com.shellanddeploy.fpllive.ui.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shellanddeploy.fpllive.data.datastore.SettingsRepository
import com.shellanddeploy.fpllive.data.live.LiveRepository
import com.shellanddeploy.fpllive.domain.model.LiveFeed
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class LiveUiState(
    val feed: LiveFeed? = null,
    val loading: Boolean = true,
    val error: String? = null,
    val lastUpdated: Long? = null,
    val configured: Boolean = true,
)

class LiveViewModel(
    private val repository: LiveRepository,
    private val settings: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(LiveUiState(configured = repository.isConfigured))
    val state: StateFlow<LiveUiState> = _state.asStateFlow()

    private var pollIntervalMs = 15_000L
    private var pollJob: Job? = null
    private var foreground = false

    init {
        viewModelScope.launch {
            settings.settings.collect { s ->
                pollIntervalMs = (s.pollIntervalSeconds * 1000L).coerceAtLeast(15_000L)
            }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = it.feed == null, error = null) }
            runCatching { repository.live() }
                .onSuccess { feed ->
                    _state.update {
                        it.copy(feed = feed, loading = false, error = null, lastUpdated = System.currentTimeMillis())
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(loading = false, error = e.message ?: "Can't reach the live service")
                    }
                }
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
                refresh()
                delay(pollIntervalMs)
            }
        }
    }
}
