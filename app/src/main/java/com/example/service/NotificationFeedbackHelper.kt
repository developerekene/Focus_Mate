package com.example.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.model.SessionType
import com.example.model.TimerState
import com.example.model.TimerStatus

class NotificationFeedbackHelper(private val context: Context) {

    companion object {
        const val CHANNEL_TIMER_ONGOING = "focusmate_timer_ongoing"
        const val CHANNEL_SESSION_ALERTS = "focusmate_session_alerts"
        const val ONGOING_NOTIFICATION_ID = 1001
        const val COMPLETION_NOTIFICATION_ID = 1002
    }

    fun ensureChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java) ?: return

            val ongoingChannel = NotificationChannel(
                CHANNEL_TIMER_ONGOING,
                context.getString(R.string.notif_channel_timer_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.notif_channel_timer_desc)
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            }

            val alertsChannel = NotificationChannel(
                CHANNEL_SESSION_ALERTS,
                context.getString(R.string.notif_channel_alert_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notif_channel_alert_desc)
                enableVibration(false)
                setSound(null, null) // Sound & vibration are explicitly controlled by user preferences
            }

            manager.createNotificationChannel(ongoingChannel)
            manager.createNotificationChannel(alertsChannel)
        }
    }

    fun buildOngoingNotification(state: TimerState): Notification {
        ensureChannels()

        val openAppIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val phaseName = when (state.sessionType) {
            SessionType.FOCUS -> context.getString(R.string.session_focus)
            SessionType.SHORT_BREAK -> context.getString(R.string.session_short_break)
            SessionType.LONG_BREAK -> context.getString(R.string.session_long_break)
        }

        val title = if (state.sessionType == SessionType.FOCUS && state.sessionTag.isNotBlank()) {
            "$phaseName · ${state.sessionTag}"
        } else {
            phaseName
        }

        val statusSuffix = if (state.status == TimerStatus.PAUSED) " (Paused)" else ""
        val contentText = "${state.formattedTime} remaining$statusSuffix"

        val toggleAction = if (state.status == TimerStatus.RUNNING) {
            NotificationCompat.Action.Builder(
                android.R.drawable.ic_media_pause,
                context.getString(R.string.notif_action_pause),
                servicePendingIntent(TimerForegroundService.ACTION_PAUSE, 11)
            ).build()
        } else {
            NotificationCompat.Action.Builder(
                android.R.drawable.ic_media_play,
                context.getString(R.string.notif_action_resume),
                servicePendingIntent(TimerForegroundService.ACTION_RESUME, 12)
            ).build()
        }

        val resetAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_revert,
            context.getString(R.string.notif_action_reset),
            servicePendingIntent(TimerForegroundService.ACTION_RESET, 13)
        ).build()

        val elapsed = (state.totalDurationSeconds - state.remainingSeconds).coerceAtLeast(0)

        return NotificationCompat.Builder(context, CHANNEL_TIMER_ONGOING)
            .setSmallIcon(R.drawable.ic_timer_notif)
            .setContentTitle(title)
            .setContentText(contentText)
            .setContentIntent(openAppIntent)
            .setOngoing(state.status == TimerStatus.RUNNING)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setProgress(state.totalDurationSeconds.coerceAtLeast(1), elapsed, false)
            .addAction(toggleAction)
            .addAction(resetAction)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    @SuppressLint("MissingPermission")
    fun postCompletionNotification(completedType: SessionType) {
        if (!hasNotificationPermission()) return
        ensureChannels()

        val openAppIntent = PendingIntent.getActivity(
            context,
            1,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (completedType == SessionType.FOCUS) {
            context.getString(R.string.notif_focus_complete_title)
        } else {
            context.getString(R.string.notif_break_complete_title)
        }

        val body = if (completedType == SessionType.FOCUS) {
            context.getString(R.string.notif_focus_complete_body)
        } else {
            context.getString(R.string.notif_break_complete_body)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_SESSION_ALERTS)
            .setSmallIcon(R.drawable.ic_timer_notif)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(openAppIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .build()

        NotificationManagerCompat.from(context).notify(COMPLETION_NOTIFICATION_ID, notification)
    }

    fun cancelCompletionNotification() {
        NotificationManagerCompat.from(context).cancel(COMPLETION_NOTIFICATION_ID)
    }

    fun playCompletionChime() {
        runCatching {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val ringtone = RingtoneManager.getRingtone(context, uri)
            if (ringtone != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    ringtone.audioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                }
                ringtone.play()
                return
            }
        }
        runCatching {
            val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 85)
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 350)
        }
    }

    fun triggerCompletionVibration() {
        val pattern = longArrayOf(0, 180, 110, 240)
        runCatching {
            val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (vibrator == null || !vibrator.hasVibrator()) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, -1)
            }
        }
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun servicePendingIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, TimerForegroundService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
