package com.example.model

enum class TimerStatus {
    IDLE,
    RUNNING,
    PAUSED
}

data class TimerState(
    val sessionType: SessionType = SessionType.FOCUS,
    val status: TimerStatus = TimerStatus.IDLE,
    val totalDurationSeconds: Int = 25 * 60,
    val remainingSeconds: Int = 25 * 60,
    val completedRoundsInCycle: Int = 0,
    val sessionsUntilLongBreak: Int = 4,
    val sessionTag: String = "Deep Work",
    val isCustomDurationActive: Boolean = false,
    val distractionFreeMode: Boolean = false
) {
    val progress: Float
        get() = if (totalDurationSeconds <= 0) 0f
        else ((totalDurationSeconds - remainingSeconds).toFloat() / totalDurationSeconds.toFloat())
            .coerceIn(0f, 1f)

    val formattedTime: String
        get() {
            val mins = remainingSeconds / 60
            val secs = remainingSeconds % 60
            return "%02d:%02d".format(mins, secs)
        }
}
