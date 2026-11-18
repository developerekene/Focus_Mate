package com.example.model

data class DailyFocusDay(
    val dateKey: String,
    val dayLabel: String,
    val focusMinutes: Int,
    val completedSessions: Int
)

data class ProductivitySummary(
    val todayFocusMinutes: Int = 0,
    val todayCompletedFocusSessions: Int = 0,
    val todayBreakMinutes: Int = 0,
    val totalFocusMinutesAllTime: Int = 0,
    val totalCompletedFocusSessionsAllTime: Int = 0,
    val currentStreakDays: Int = 0,
    val last7Days: List<DailyFocusDay> = emptyList()
)
