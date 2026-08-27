package com.shellanddeploy.fpllive.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shellanddeploy.fpllive.data.chat.ChatRepository
import com.shellanddeploy.fpllive.data.datastore.SettingsRepository
import com.shellanddeploy.fpllive.domain.model.ChatMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val teamId: Int = 0,
    val teamName: String = "",
    val loading: Boolean = true,
    val error: String? = null,
    val configured: Boolean = true,
)

class ChatViewModel(
    private val repository: ChatRepository,
    private val settings: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ChatUiState(configured = repository.isConfigured))
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    private var pollJob: Job? = null
    private var foreground = false
    private var lastIso = ""

    init {
        viewModelScope.launch {
            settings.settings.collect { s ->
                _state.update { it.copy(teamId = s.defaultTeamId) }
            }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = it.messages.isEmpty(), error = null) }
            runCatching { repository.recent() }
                .onSuccess { list ->
                    val sorted = list.sortedBy { m -> m.createdAt }
                    lastIso = sorted.lastOrNull()?.createdAt ?: ""
                    _state.update { it.copy(messages = sorted, loading = false, error = null) }
                }
                .onFailure { e ->
                    _state.update { it.copy(loading = false, error = e.message ?: "Can't reach chat") }
                }
        }
    }

    fun send(text: String) {
        val body = text.trim()
        val teamId = _state.value.teamId
        if (body.isEmpty() || teamId <= 0) return
        viewModelScope.launch {
            runCatching { repository.send(teamId, _state.value.teamName, body) }
                .onFailure { e -> _state.update { it.copy(error = e.message ?: "Send failed") } }
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
        if (!foreground || pollJob?.isActive == true) return
        pollJob = viewModelScope.launch {
            while (isActive) {
                delay(5000)
                if (lastIso.isNotEmpty()) {
                    runCatching { repository.since(lastIso) }
                        .onSuccess { newOnes ->
                            if (newOnes.isNotEmpty()) {
                                val merged =
                                    (_state.value.messages + newOnes).distinctBy { it.id }
                                        .sortedBy { it.createdAt }
                                lastIso = merged.lastOrNull()?.createdAt ?: lastIso
                                _state.update { it.copy(messages = merged) }
                            }
                        }
                }
            }
        }
    }
}
