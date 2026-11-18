package com.example.service

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.example.data.local.FocusRepository
import com.example.data.local.SettingsDataStore
import com.example.model.SessionType
import com.example.model.TimerSettings
import com.example.model.TimerState
import com.example.model.TimerStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class TimerEngine(
    private val context: Context,
    private val repository: FocusRepository,
    private val settingsDataStore: SettingsDataStore,
    private val feedbackHelper: NotificationFeedbackHelper,
    private val manageForegroundService: Boolean = true,
    private val elapsedTimeProvider: () -> Long = { SystemClock.elapsedRealtime() }
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _settings = MutableStateFlow(TimerSettings())
    val settings: StateFlow<TimerSettings> = _settings.asStateFlow()

    private val _timerState = MutableStateFlow(TimerState())
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    private var tickerJob: Job? = null
    private var endElapsedMillis: Long = 0L

    init {
        scope.launch {
            settingsDataStore.settingsFlow.collect { latest ->
                val previous = _settings.value
                _settings.value = latest
                _timerState.update { state ->
                    val shouldUpdateDuration = state.status == TimerStatus.IDLE &&
                        !state.isCustomDurationActive &&
                        previous.durationMinutesFor(state.sessionType) != latest.durationMinutesFor(state.sessionType)
                    val newSeconds = if (shouldUpdateDuration) {
                        latest.durationMinutesFor(state.sessionType) * 60
                    } else {
                        state.remainingSeconds
                    }
                    val newTotal = if (shouldUpdateDuration) {
                        latest.durationMinutesFor(state.sessionType) * 60
                    } else {
                        state.totalDurationSeconds
                    }
                    state.copy(
                        totalDurationSeconds = newTotal,
                        remainingSeconds = newSeconds,
                        sessionsUntilLongBreak = latest.sessionsUntilLongBreak,
                        sessionTag = latest.selectedTag
                    )
                }
            }
        }
    }

    fun startOrResume() {
        val current = _timerState.value
        if (current.status == TimerStatus.RUNNING) return

        feedbackHelper.cancelCompletionNotification()
        val remainingMillis = current.remainingSeconds.coerceAtLeast(1) * 1000L
        endElapsedMillis = elapsedTimeProvider() + remainingMillis

        _timerState.update {
            it.copy(status = TimerStatus.RUNNING)
        }
        startTicker()
        syncForegroundService(startIfNeeded = true)
    }

    fun pause() {
        val current = _timerState.value
        if (current.status != TimerStatus.RUNNING) return

        tickerJob?.cancel()
        tickerJob = null

        val remainingSec = ((endElapsedMillis - elapsedTimeProvider()) + 999L)
            .coerceAtLeast(0L)
            .div(1000L)
            .toInt()
            .coerceAtLeast(1)

        _timerState.update {
            it.copy(
                status = TimerStatus.PAUSED,
                remainingSeconds = remainingSec
            )
        }
        syncForegroundService(startIfNeeded = false)
    }

    fun reset() {
        tickerJob?.cancel()
        tickerJob = null

        val currentType = _timerState.value.sessionType
        val defaultSecs = _settings.value.durationMinutesFor(currentType) * 60
        _timerState.update {
            it.copy(
                status = TimerStatus.IDLE,
                totalDurationSeconds = defaultSecs,
                remainingSeconds = defaultSecs,
                isCustomDurationActive = false
            )
        }
        stopForegroundService()
    }

    fun selectSessionType(type: SessionType) {
        if (_timerState.value.sessionType == type && _timerState.value.status == TimerStatus.IDLE) {
            return
        }
        tickerJob?.cancel()
        tickerJob = null

        val secs = _settings.value.durationMinutesFor(type) * 60
        _timerState.update {
            it.copy(
                sessionType = type,
                status = TimerStatus.IDLE,
                totalDurationSeconds = secs,
                remainingSeconds = secs,
                isCustomDurationActive = false
            )
        }
        stopForegroundService()
    }

    fun applyCustomDuration(minutes: Int) {
        val clamped = minutes.coerceIn(1, 180)
        tickerJob?.cancel()
        tickerJob = null

        val secs = clamped * 60
        _timerState.update {
            it.copy(
                status = TimerStatus.IDLE,
                totalDurationSeconds = secs,
                remainingSeconds = secs,
                isCustomDurationActive = true
            )
        }
        stopForegroundService()
    }

    fun skipToNextPhase() {
        tickerJob?.cancel()
        tickerJob = null

        val current = _timerState.value
        val nextType = when (current.sessionType) {
            SessionType.FOCUS -> {
                if (current.completedRoundsInCycle + 1 >= _settings.value.sessionsUntilLongBreak) {
                    SessionType.LONG_BREAK
                } else {
                    SessionType.SHORT_BREAK
                }
            }
            SessionType.SHORT_BREAK, SessionType.LONG_BREAK -> SessionType.FOCUS
        }

        val nextCycleCount = when (current.sessionType) {
            SessionType.FOCUS -> current.completedRoundsInCycle
            SessionType.SHORT_BREAK -> current.completedRoundsInCycle
            SessionType.LONG_BREAK -> 0
        }

        val nextSecs = _settings.value.durationMinutesFor(nextType) * 60
        _timerState.update {
            it.copy(
                sessionType = nextType,
                status = TimerStatus.IDLE,
                totalDurationSeconds = nextSecs,
                remainingSeconds = nextSecs,
                completedRoundsInCycle = nextCycleCount,
                isCustomDurationActive = false
            )
        }
        stopForegroundService()
    }

    fun updateSessionTag(tag: String) {
        val sanitized = tag.trim().take(28).ifEmpty { "Deep Work" }
        _timerState.update { it.copy(sessionTag = sanitized) }
        scope.launch {
            settingsDataStore.updateSettings { it.copy(selectedTag = sanitized) }
        }
    }

    fun toggleDistractionFreeMode() {
        _timerState.update { it.copy(distractionFreeMode = !it.distractionFreeMode) }
    }

    fun setDistractionFreeMode(enabled: Boolean) {
        _timerState.update { it.copy(distractionFreeMode = enabled) }
    }

    fun testAlertFeedback() {
        val currentSettings = _settings.value
        if (currentSettings.soundEnabled) {
            feedbackHelper.playCompletionChime()
        }
        if (currentSettings.vibrationEnabled) {
            feedbackHelper.triggerCompletionVibration()
        }
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (isActive) {
                val remainingMillis = endElapsedMillis - elapsedTimeProvider()
                if (remainingMillis <= 0L) {
                    onSessionCompleted()
                    break
                }
                val remainingSeconds = ((remainingMillis + 999L) / 1000L).toInt()
                _timerState.update { state ->
                    if (state.remainingSeconds != remainingSeconds) {
                        state.copy(remainingSeconds = remainingSeconds)
                    } else {
                        state
                    }
                }
                delay(250L)
            }
        }
    }

    private suspend fun onSessionCompleted() {
        val completedState = _timerState.value
        val currentSettings = _settings.value

        val completedMinutes = (completedState.totalDurationSeconds / 60).coerceAtLeast(1)
        repository.recordCompletedSession(
            sessionType = completedState.sessionType,
            durationMinutes = completedMinutes,
            tag = completedState.sessionTag
        )

        if (currentSettings.soundEnabled) {
            feedbackHelper.playCompletionChime()
        }
        if (currentSettings.vibrationEnabled) {
            feedbackHelper.triggerCompletionVibration()
        }
        feedbackHelper.postCompletionNotification(completedState.sessionType)

        val updatedRounds = when (completedState.sessionType) {
            SessionType.FOCUS -> completedState.completedRoundsInCycle + 1
            SessionType.SHORT_BREAK -> completedState.completedRoundsInCycle
            SessionType.LONG_BREAK -> 0
        }

        val nextType = when (completedState.sessionType) {
            SessionType.FOCUS -> {
                if (updatedRounds >= currentSettings.sessionsUntilLongBreak) {
                    SessionType.LONG_BREAK
                } else {
                    SessionType.SHORT_BREAK
                }
            }
            SessionType.SHORT_BREAK, SessionType.LONG_BREAK -> SessionType.FOCUS
        }

        val normalizedRounds = if (completedState.sessionType == SessionType.LONG_BREAK) 0 else updatedRounds
        val nextDurationSecs = currentSettings.durationMinutesFor(nextType) * 60
        val shouldAutoStart = if (nextType.isBreak) {
            currentSettings.autoStartBreaks
        } else {
            currentSettings.autoStartFocus
        }

        if (shouldAutoStart) {
            endElapsedMillis = elapsedTimeProvider() + (nextDurationSecs * 1000L)
            _timerState.update {
                it.copy(
                    sessionType = nextType,
                    status = TimerStatus.RUNNING,
                    totalDurationSeconds = nextDurationSecs,
                    remainingSeconds = nextDurationSecs,
                    completedRoundsInCycle = normalizedRounds,
                    isCustomDurationActive = false
                )
            }
            startTicker()
            syncForegroundService(startIfNeeded = true)
        } else {
            _timerState.update {
                it.copy(
                    sessionType = nextType,
                    status = TimerStatus.IDLE,
                    totalDurationSeconds = nextDurationSecs,
                    remainingSeconds = nextDurationSecs,
                    completedRoundsInCycle = normalizedRounds,
                    isCustomDurationActive = false
                )
            }
            stopForegroundService()
        }
    }

    private fun syncForegroundService(startIfNeeded: Boolean) {
        if (!manageForegroundService) return
        if (startIfNeeded) {
            runCatching {
                val intent = Intent(context, TimerForegroundService::class.java).apply {
                    action = TimerForegroundService.ACTION_START_OR_SYNC
                }
                ContextCompat.startForegroundService(context, intent)
            }
        }
    }

    private fun stopForegroundService() {
        if (!manageForegroundService) return
        runCatching {
            val intent = Intent(context, TimerForegroundService::class.java).apply {
                action = TimerForegroundService.ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
