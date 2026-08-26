package com.shellanddeploy.fpllive.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shellanddeploy.fpllive.data.api.FplRepository
import com.shellanddeploy.fpllive.data.datastore.Settings
import com.shellanddeploy.fpllive.data.datastore.SettingsRepository
import com.shellanddeploy.fpllive.notifications.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: FplRepository,
    private val settings: SettingsRepository,
    private val reminderScheduler: ReminderScheduler,
) : ViewModel() {

    private val _state = MutableStateFlow(Settings())
    val state: StateFlow<Settings> = _state.asStateFlow()

    private val _cleared = MutableStateFlow(false)
    val cleared: StateFlow<Boolean> = _cleared.asStateFlow()

    init {
        viewModelScope.launch {
            settings.settings.collect { _state.value = it }
        }
    }

    fun setDefaultTeamId(id: Int) {
        if (id <= 0) return
        viewModelScope.launch { settings.setDefaultTeamId(id) }
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
}
