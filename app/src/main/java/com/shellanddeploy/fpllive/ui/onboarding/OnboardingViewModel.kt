package com.shellanddeploy.fpllive.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shellanddeploy.fpllive.data.api.FetchResult
import com.shellanddeploy.fpllive.data.api.FplRepository
import com.shellanddeploy.fpllive.data.datastore.SettingsRepository
import com.shellanddeploy.fpllive.data.namesearch.ManagerMatch
import com.shellanddeploy.fpllive.data.namesearch.NameSearchRepository
import com.shellanddeploy.fpllive.domain.model.Entry
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class OnboardingMode { TeamId, Name }

data class OnboardingUiState(
    val mode: OnboardingMode = OnboardingMode.TeamId,
    val teamIdText: String = "",
    val nameQuery: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val preview: Entry? = null,
    val searchResults: List<ManagerMatch> = emptyList(),
    val nameSearchAvailable: Boolean = true,
)

class OnboardingViewModel(
    private val repository: FplRepository,
    private val settings: SettingsRepository,
    private val nameSearch: NameSearchRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(
        OnboardingUiState(nameSearchAvailable = nameSearch.isConfigured),
    )
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    private var searchJob: Job? = null

    fun setMode(mode: OnboardingMode) {
        _state.update { it.copy(mode = mode, error = null, searchResults = emptyList(), preview = null) }
    }

    fun setTeamId(value: String) {
        _state.update { it.copy(teamIdText = value, error = null, preview = null) }
    }

    fun setNameQuery(value: String) {
        _state.update { it.copy(nameQuery = value, error = null, searchResults = emptyList()) }
        searchJob?.cancel()
        if (value.trim().length < 2) return
        searchJob = viewModelScope.launch {
            delay(300)
            val query = _state.value.nameQuery.trim()
            if (query.length < 2) return@launch
            _state.update { it.copy(loading = true) }
            runCatching { nameSearch.search(query) }
                .onSuccess { results ->
                    _state.update {
                        it.copy(
                            loading = false,
                            searchResults = results,
                            error = if (results.isEmpty()) "No matches for \"$query\". Try your team ID instead." else null,
                        )
                    }
                }
                .onFailure {
                    _state.update {
                        it.copy(
                            loading = false,
                            searchResults = emptyList(),
                            error = "Can't reach the search service. Check your connection and try again.",
                        )
                    }
                }
        }
    }

    fun lookup() {
        val id = extractTeamId(_state.value.teamIdText)
        if (id == null || id <= 0) {
            _state.update { it.copy(error = "Enter a valid team ID, or paste your team's URL from the official site.") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            when (val r = repository.entry(id)) {
                is FetchResult.Success -> _state.update { it.copy(loading = false, preview = r.data) }
                is FetchResult.Error ->
                    _state.update {
                        it.copy(
                            loading = false,
                            error = "No team found for that ID. Double-check the number and try again.",
                        )
                    }
            }
        }
    }

    fun selectMatch(match: ManagerMatch) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            when (val r = repository.entry(match.teamId)) {
                is FetchResult.Success -> _state.update { it.copy(loading = false, preview = r.data) }
                is FetchResult.Error ->
                    _state.update { it.copy(loading = false, error = "Could not load that team. Try again.") }
            }
        }
    }

    fun confirm() {
        val preview = _state.value.preview ?: return
        viewModelScope.launch {
            settings.completeOnboarding(preview.id)
            nameSearch.register(preview.id, "${preview.playerFirstName} ${preview.playerLastName}", preview.name)
        }
    }

    companion object {
        /** Pulls a numeric team ID out of either a bare ID or a full FPL team URL. */
        fun extractTeamId(raw: String): Int? {
            Regex("""entry/(\d+)""").find(raw)?.groupValues?.get(1)?.toIntOrNull()?.let { return it }
            return raw.filter { it.isDigit() }.toIntOrNull()
        }
    }
}
