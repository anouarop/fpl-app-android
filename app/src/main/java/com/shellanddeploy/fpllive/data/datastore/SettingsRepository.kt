package com.shellanddeploy.fpllive.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "fpllive_settings")

data class Settings(
    val defaultTeamId: Int = DEFAULT_TEAM_ID,
    val pollIntervalSeconds: Int = 20,
    val darkTheme: Boolean = true,
    val notificationsEnabled: Boolean = false,
    val onboardingComplete: Boolean = false,
) {
    companion object {
        const val DEFAULT_TEAM_ID = 9166708
    }
}

class SettingsRepository(private val context: Context) {

    private object Keys {
        val DEFAULT_TEAM_ID = intPreferencesKey("default_team_id")
        val POLL_INTERVAL = intPreferencesKey("poll_interval_seconds")
        val DARK_THEME = stringPreferencesKey("theme")
        val NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
    }

    val settings: Flow<Settings> = context.dataStore.data.map { prefs ->
        Settings(
            defaultTeamId = prefs[Keys.DEFAULT_TEAM_ID] ?: Settings.DEFAULT_TEAM_ID,
            pollIntervalSeconds = prefs[Keys.POLL_INTERVAL] ?: 20,
            darkTheme = (prefs[Keys.DARK_THEME] ?: "dark") == "dark",
            notificationsEnabled = prefs[Keys.NOTIFICATIONS] ?: false,
            onboardingComplete = prefs[Keys.ONBOARDING_COMPLETE] ?: false,
        )
    }

    suspend fun setDefaultTeamId(id: Int) {
        context.dataStore.edit { it[Keys.DEFAULT_TEAM_ID] = id }
    }

    suspend fun setPollInterval(seconds: Int) {
        context.dataStore.edit { it[Keys.POLL_INTERVAL] = seconds }
    }

    suspend fun setDarkTheme(dark: Boolean) {
        context.dataStore.edit { it[Keys.DARK_THEME] = if (dark) "dark" else "light" }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFICATIONS] = enabled }
    }

    suspend fun completeOnboarding(teamId: Int) {
        context.dataStore.edit {
            it[Keys.DEFAULT_TEAM_ID] = teamId
            it[Keys.ONBOARDING_COMPLETE] = true
        }
    }
}
