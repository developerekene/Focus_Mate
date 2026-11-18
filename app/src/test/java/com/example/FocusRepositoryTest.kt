package com.example

import com.example.data.local.FocusRepository
import com.example.data.local.FocusSessionEntity
import com.example.model.SessionType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class FocusRepositoryTest {

    @Test
    fun `computeSummary aggregates today focus, breaks, streak, and 7-day chart`() {
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 18, 14, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayMillis = now.timeInMillis
        val yesterdayMillis = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }.timeInMillis
        val twoDaysAgoMillis = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -2) }.timeInMillis
        val fourDaysAgoMillis = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -4) }.timeInMillis

        val sessions = listOf(
            FocusSessionEntity(
                id = 1,
                sessionType = SessionType.FOCUS.storageKey,
                durationMinutes = 25,
                tag = "Deep Work",
                completedAtMillis = todayMillis,
                dateKey = FocusRepository.formatDateKey(todayMillis)
            ),
            FocusSessionEntity(
                id = 2,
                sessionType = SessionType.FOCUS.storageKey,
                durationMinutes = 50,
                tag = "Coding",
                completedAtMillis = todayMillis - 60_000L,
                dateKey = FocusRepository.formatDateKey(todayMillis)
            ),
            FocusSessionEntity(
                id = 3,
                sessionType = SessionType.SHORT_BREAK.storageKey,
                durationMinutes = 5,
                tag = "Deep Work",
                completedAtMillis = todayMillis - 120_000L,
                dateKey = FocusRepository.formatDateKey(todayMillis)
            ),
            FocusSessionEntity(
                id = 4,
                sessionType = SessionType.FOCUS.storageKey,
                durationMinutes = 25,
                tag = "Study",
                completedAtMillis = yesterdayMillis,
                dateKey = FocusRepository.formatDateKey(yesterdayMillis)
            ),
            FocusSessionEntity(
                id = 5,
                sessionType = SessionType.FOCUS.storageKey,
                durationMinutes = 25,
                tag = "Study",
                completedAtMillis = twoDaysAgoMillis,
                dateKey = FocusRepository.formatDateKey(twoDaysAgoMillis)
            ),
            FocusSessionEntity(
                id = 6,
                sessionType = SessionType.FOCUS.storageKey,
                durationMinutes = 30,
                tag = "Writing",
                completedAtMillis = fourDaysAgoMillis,
                dateKey = FocusRepository.formatDateKey(fourDaysAgoMillis)
            )
        )

        val summary = FocusRepository.computeSummary(sessions, nowMillis = todayMillis)

        assertEquals(75, summary.todayFocusMinutes)
        assertEquals(2, summary.todayCompletedFocusSessions)
        assertEquals(5, summary.todayBreakMinutes)
        assertEquals(155, summary.totalFocusMinutesAllTime)
        assertEquals(5, summary.totalCompletedFocusSessionsAllTime)
        assertEquals(3, summary.currentStreakDays)
        assertEquals(7, summary.last7Days.size)
        assertEquals(75, summary.last7Days.last().focusMinutes)
    }
}
