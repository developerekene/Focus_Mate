package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.ThemeMode
import com.example.model.TimerSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "focusmate_settings")

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val FOCUS_MINUTES = intPreferencesKey("focus_minutes")
        val SHORT_BREAK_MINUTES = intPreferencesKey("short_break_minutes")
        val LONG_BREAK_MINUTES = intPreferencesKey("long_break_minutes")
        val SESSIONS_UNTIL_LONG_BREAK = intPreferencesKey("sessions_until_long_break")
        val DAILY_GOAL_SESSIONS = intPreferencesKey("daily_goal_sessions")
        val AUTO_START_BREAKS = booleanPreferencesKey("auto_start_breaks")
        val AUTO_START_FOCUS = booleanPreferencesKey("auto_start_focus")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val SELECTED_TAG = stringPreferencesKey("selected_tag")
    }

    val settingsFlow: Flow<TimerSettings> = context.settingsDataStore.data.map { prefs ->
        TimerSettings(
            focusMinutes = (prefs[Keys.FOCUS_MINUTES] ?: 25).coerceIn(1, 120),
            shortBreakMinutes = (prefs[Keys.SHORT_BREAK_MINUTES] ?: 5).coerceIn(1, 60),
            longBreakMinutes = (prefs[Keys.LONG_BREAK_MINUTES] ?: 15).coerceIn(1, 90),
            sessionsUntilLongBreak = (prefs[Keys.SESSIONS_UNTIL_LONG_BREAK] ?: 4).coerceIn(2, 8),
            dailyGoalSessions = (prefs[Keys.DAILY_GOAL_SESSIONS] ?: 8).coerceIn(1, 20),
            autoStartBreaks = prefs[Keys.AUTO_START_BREAKS] ?: false,
            autoStartFocus = prefs[Keys.AUTO_START_FOCUS] ?: false,
            soundEnabled = prefs[Keys.SOUND_ENABLED] ?: true,
            vibrationEnabled = prefs[Keys.VIBRATION_ENABLED] ?: true,
            keepScreenOn = prefs[Keys.KEEP_SCREEN_ON] ?: false,
            themeMode = ThemeMode.fromKey(prefs[Keys.THEME_MODE]),
            selectedTag = prefs[Keys.SELECTED_TAG] ?: "Deep Work"
        )
    }

    suspend fun updateSettings(transform: (TimerSettings) -> TimerSettings) {
        context.settingsDataStore.edit { prefs ->
            val current = TimerSettings(
                focusMinutes = prefs[Keys.FOCUS_MINUTES] ?: 25,
                shortBreakMinutes = prefs[Keys.SHORT_BREAK_MINUTES] ?: 5,
                longBreakMinutes = prefs[Keys.LONG_BREAK_MINUTES] ?: 15,
                sessionsUntilLongBreak = prefs[Keys.SESSIONS_UNTIL_LONG_BREAK] ?: 4,
                dailyGoalSessions = prefs[Keys.DAILY_GOAL_SESSIONS] ?: 8,
                autoStartBreaks = prefs[Keys.AUTO_START_BREAKS] ?: false,
                autoStartFocus = prefs[Keys.AUTO_START_FOCUS] ?: false,
                soundEnabled = prefs[Keys.SOUND_ENABLED] ?: true,
                vibrationEnabled = prefs[Keys.VIBRATION_ENABLED] ?: true,
                keepScreenOn = prefs[Keys.KEEP_SCREEN_ON] ?: false,
                themeMode = ThemeMode.fromKey(prefs[Keys.THEME_MODE]),
                selectedTag = prefs[Keys.SELECTED_TAG] ?: "Deep Work"
            )
            val updated = transform(current)
            prefs[Keys.FOCUS_MINUTES] = updated.focusMinutes.coerceIn(1, 120)
            prefs[Keys.SHORT_BREAK_MINUTES] = updated.shortBreakMinutes.coerceIn(1, 60)
            prefs[Keys.LONG_BREAK_MINUTES] = updated.longBreakMinutes.coerceIn(1, 90)
            prefs[Keys.SESSIONS_UNTIL_LONG_BREAK] = updated.sessionsUntilLongBreak.coerceIn(2, 8)
            prefs[Keys.DAILY_GOAL_SESSIONS] = updated.dailyGoalSessions.coerceIn(1, 20)
            prefs[Keys.AUTO_START_BREAKS] = updated.autoStartBreaks
            prefs[Keys.AUTO_START_FOCUS] = updated.autoStartFocus
            prefs[Keys.SOUND_ENABLED] = updated.soundEnabled
            prefs[Keys.VIBRATION_ENABLED] = updated.vibrationEnabled
            prefs[Keys.KEEP_SCREEN_ON] = updated.keepScreenOn
            prefs[Keys.THEME_MODE] = updated.themeMode.storageKey
            prefs[Keys.SELECTED_TAG] = updated.selectedTag
        }
    }
}
