package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.FocusRepository
import com.example.data.local.FocusSessionEntity
import com.example.data.local.SettingsDataStore
import com.example.model.ProductivitySummary
import com.example.model.SessionType
import com.example.model.ThemeMode
import com.example.model.TimerSettings
import com.example.model.TimerState
import com.example.service.TimerEngine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FocusViewModel(
    private val timerEngine: TimerEngine,
    private val repository: FocusRepository,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    val timerState: StateFlow<TimerState> = timerEngine.timerState

    val settings: StateFlow<TimerSettings> = settingsDataStore.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TimerSettings()
        )

    val summary: StateFlow<ProductivitySummary> = repository.observeSummary()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ProductivitySummary()
        )

    val sessionHistory: StateFlow<List<FocusSessionEntity>> = repository.allSessions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun startOrPause() {
        when (timerState.value.status) {
            com.example.model.TimerStatus.RUNNING -> timerEngine.pause()
            com.example.model.TimerStatus.IDLE,
            com.example.model.TimerStatus.PAUSED -> timerEngine.startOrResume()
        }
    }

    fun resetTimer() {
        timerEngine.reset()
    }

    fun skipPhase() {
        timerEngine.skipToNextPhase()
    }

    fun selectSessionType(type: SessionType) {
        timerEngine.selectSessionType(type)
    }

    fun applyCustomDuration(minutes: Int) {
        timerEngine.applyCustomDuration(minutes)
    }

    fun updateSessionTag(tag: String) {
        timerEngine.updateSessionTag(tag)
    }

    fun toggleDistractionFreeMode() {
        timerEngine.toggleDistractionFreeMode()
    }

    fun setDistractionFreeMode(enabled: Boolean) {
        timerEngine.setDistractionFreeMode(enabled)
    }

    fun deleteSession(id: Long) {
        viewModelScope.launch {
            repository.deleteSession(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAllHistory()
        }
    }

    fun updateFocusMinutes(minutes: Int) {
        viewModelScope.launch {
            settingsDataStore.updateSettings { it.copy(focusMinutes = minutes) }
        }
    }

    fun updateShortBreakMinutes(minutes: Int) {
        viewModelScope.launch {
            settingsDataStore.updateSettings { it.copy(shortBreakMinutes = minutes) }
        }
    }

    fun updateLongBreakMinutes(minutes: Int) {
        viewModelScope.launch {
            settingsDataStore.updateSettings { it.copy(longBreakMinutes = minutes) }
        }
    }

    fun updateSessionsUntilLongBreak(count: Int) {
        viewModelScope.launch {
            settingsDataStore.updateSettings { it.copy(sessionsUntilLongBreak = count) }
        }
    }

    fun updateDailyGoalSessions(count: Int) {
        viewModelScope.launch {
            settingsDataStore.updateSettings { it.copy(dailyGoalSessions = count) }
        }
    }

    fun updateAutoStartBreaks(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.updateSettings { it.copy(autoStartBreaks = enabled) }
        }
    }

    fun updateAutoStartFocus(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.updateSettings { it.copy(autoStartFocus = enabled) }
        }
    }

    fun updateSoundEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.updateSettings { it.copy(soundEnabled = enabled) }
        }
    }

    fun updateVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.updateSettings { it.copy(vibrationEnabled = enabled) }
        }
    }

    fun updateKeepScreenOn(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.updateSettings { it.copy(keepScreenOn = enabled) }
        }
    }

    fun updateThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsDataStore.updateSettings { it.copy(themeMode = mode) }
        }
    }

    fun testAlertFeedback() {
        timerEngine.testAlertFeedback()
    }

    class Factory(
        private val timerEngine: TimerEngine,
        private val repository: FocusRepository,
        private val settingsDataStore: SettingsDataStore
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FocusViewModel(timerEngine, repository, settingsDataStore) as T
        }
    }
}
