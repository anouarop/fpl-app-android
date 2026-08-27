package com.shellanddeploy.fpllive.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shellanddeploy.fpllive.data.api.FplRepository
import com.shellanddeploy.fpllive.data.datastore.Settings
import com.shellanddeploy.fpllive.data.datastore.SettingsRepository
import com.shellanddeploy.fpllive.data.namesearch.ManagerMatch
import com.shellanddeploy.fpllive.data.namesearch.NameSearchRepository
import com.shellanddeploy.fpllive.notifications.ReminderScheduler
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NameSearchUiState(
    val query: String = "",
    val loading: Boolean = false,
    val results: List<ManagerMatch> = emptyList(),
    val available: Boolean = true,
    val error: String? = null,
)

class SettingsViewModel(
    private val repository: FplRepository,
    private val settings: SettingsRepository,
    private val reminderScheduler: ReminderScheduler,
    private val nameSearch: NameSearchRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(Settings())
    val state: StateFlow<Settings> = _state.asStateFlow()

    private val _nameSearch = MutableStateFlow(NameSearchUiState(available = nameSearch.isConfigured))
    val nameSearchState: StateFlow<NameSearchUiState> = _nameSearch.asStateFlow()

    private val _cleared = MutableStateFlow(false)
    val cleared: StateFlow<Boolean> = _cleared.asStateFlow()

    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            settings.settings.collect { _state.value = it }
        }
    }

    fun setDefaultTeamId(id: Int) {
        if (id <= 0) return
        viewModelScope.launch { settings.setDefaultTeamId(id) }
    }

    fun setNameQuery(value: String) {
        _nameSearch.update { it.copy(query = value, error = null, results = emptyList()) }
        searchJob?.cancel()
        if (value.trim().length < 2) return
        searchJob = viewModelScope.launch {
            delay(300)
            val query = _nameSearch.value.query.trim()
            if (query.length < 2) return@launch
            _nameSearch.update { it.copy(loading = true) }
            runCatching { nameSearch.search(query) }
                .onSuccess { results ->
                    _nameSearch.update {
                        it.copy(
                            loading = false,
                            results = results,
                            error = if (results.isEmpty()) "No matches for \"$query\"." else null,
                        )
                    }
                }
                .onFailure {
                    _nameSearch.update {
                        it.copy(
                            loading = false,
                            results = emptyList(),
                            error = "Can't reach the search service. Check your connection and try again.",
                        )
                    }
                }
        }
    }

    fun selectMatch(match: ManagerMatch) {
        viewModelScope.launch {
            settings.setDefaultTeamId(match.teamId)
            nameSearch.register(match.teamId, match.managerName, match.teamName)
            _nameSearch.update { it.copy(query = "", results = emptyList(), error = null) }
        }
    }

    fun setPollInterval(seconds: Int) {
        viewModelScope.launch { settings.setPollInterval(seconds) }
    }

    fun setDarkTheme(dark: Boolean) {
        viewModelScope.launch { settings.setDarkTheme(dark) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        reminderScheduler.setEnabled(enabled)
        viewModelScope.launch { settings.setNotificationsEnabled(enabled) }
    }

    fun clearCache() {
        viewModelScope.launch {
            repository.clearCache()
            _cleared.value = true
        }
    }

    fun logout() {
        viewModelScope.launch {
            settings.logout()
        }
    }
}
