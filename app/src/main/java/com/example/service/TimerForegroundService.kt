package com.example.service

import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.example.FocusMateApplication
import com.example.model.TimerStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class TimerForegroundService : Service() {

    companion object {
        const val ACTION_START_OR_SYNC = "com.example.action.START_OR_SYNC"
        const val ACTION_PAUSE = "com.example.action.PAUSE"
        const val ACTION_RESUME = "com.example.action.RESUME"
        const val ACTION_RESET = "com.example.action.RESET"
        const val ACTION_STOP = "com.example.action.STOP"
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var stateObserverJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val app = application as FocusMateApplication
        val engine = app.timerEngine
        val feedback = app.feedbackHelper

        when (intent?.action) {
            ACTION_PAUSE -> engine.pause()
            ACTION_RESUME -> engine.startOrResume()
            ACTION_RESET -> {
                engine.reset()
                stopSelfAndClear()
                return START_NOT_STICKY
            }
            ACTION_STOP -> {
                stopSelfAndClear()
                return START_NOT_STICKY
            }
        }

        val currentState = engine.timerState.value
        if (currentState.status == TimerStatus.IDLE) {
            stopSelfAndClear()
            return START_NOT_STICKY
        }

        val notification = feedback.buildOngoingNotification(currentState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NotificationFeedbackHelper.ONGOING_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(
                NotificationFeedbackHelper.ONGOING_NOTIFICATION_ID,
                notification
            )
        }

        observeTimerUpdates(app)
        return START_STICKY
    }

    private fun observeTimerUpdates(app: FocusMateApplication) {
        if (stateObserverJob?.isActive == true) return
        val manager = getSystemService(NotificationManager::class.java)

        stateObserverJob = serviceScope.launch {
            app.timerEngine.timerState.collect { state ->
                if (state.status == TimerStatus.IDLE) {
                    stopSelfAndClear()
                } else {
                    val updatedNotification = app.feedbackHelper.buildOngoingNotification(state)
                    manager?.notify(
                        NotificationFeedbackHelper.ONGOING_NOTIFICATION_ID,
                        updatedNotification
                    )
                }
            }
        }
    }

    private fun stopSelfAndClear() {
        stateObserverJob?.cancel()
        stateObserverJob = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    override fun onDestroy() {
        stateObserverJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }
}
