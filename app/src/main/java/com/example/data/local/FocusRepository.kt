package com.example.data.local

import com.example.model.DailyFocusDay
import com.example.model.ProductivitySummary
import com.example.model.SessionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class FocusRepository(private val dao: FocusSessionDao) {

    val allSessions: Flow<List<FocusSessionEntity>> = dao.observeAllSessions()

    fun observeSummary(): Flow<ProductivitySummary> = allSessions.map { sessions ->
        computeSummary(sessions, nowMillis = System.currentTimeMillis())
    }

    suspend fun recordCompletedSession(
        sessionType: SessionType,
        durationMinutes: Int,
        tag: String,
        completedAtMillis: Long = System.currentTimeMillis()
    ) {
        val dateKey = formatDateKey(completedAtMillis)
        dao.insertSession(
            FocusSessionEntity(
                sessionType = sessionType.storageKey,
                durationMinutes = durationMinutes.coerceAtLeast(1),
                tag = tag.ifBlank { "Deep Work" },
                completedAtMillis = completedAtMillis,
                dateKey = dateKey
            )
        )
    }

    suspend fun deleteSession(id: Long) {
        dao.deleteSessionById(id)
    }

    suspend fun clearAllHistory() {
        dao.clearAllSessions()
    }

    companion object {
        fun formatDateKey(timeMillis: Long): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            return sdf.format(Date(timeMillis))
        }

        fun formatTimeLabel(timeMillis: Long): String {
            val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
            return sdf.format(Date(timeMillis))
        }

        fun formatShortDateLabel(dateKey: String, todayMillis: Long = System.currentTimeMillis()): String {
            val todayKey = formatDateKey(todayMillis)
            val cal = Calendar.getInstance().apply {
                timeInMillis = todayMillis
                add(Calendar.DAY_OF_YEAR, -1)
            }
            val yesterdayKey = formatDateKey(cal.timeInMillis)
            return when (dateKey) {
                todayKey -> "Today"
                yesterdayKey -> "Yesterday"
                else -> {
                    val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    val formatter = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
                    runCatching {
                        val parsed = parser.parse(dateKey)
                        if (parsed != null) formatter.format(parsed) else dateKey
                    }.getOrDefault(dateKey)
                }
            }
        }

        fun computeSummary(
            sessions: List<FocusSessionEntity>,
            nowMillis: Long = System.currentTimeMillis()
        ): ProductivitySummary {
            val todayKey = formatDateKey(nowMillis)
            val focusSessions = sessions.filter { SessionType.fromKey(it.sessionType) == SessionType.FOCUS }
            val breakSessions = sessions.filter { SessionType.fromKey(it.sessionType) != SessionType.FOCUS }

            val todayFocus = focusSessions.filter { it.dateKey == todayKey }
            val todayBreak = breakSessions.filter { it.dateKey == todayKey }

            val dayFormatter = SimpleDateFormat("EEE", Locale.getDefault())
            val last7Days = (6 downTo 0).map { daysAgo ->
                val cal = Calendar.getInstance().apply {
                    timeInMillis = nowMillis
                    add(Calendar.DAY_OF_YEAR, -daysAgo)
                }
                val key = formatDateKey(cal.timeInMillis)
                val label = dayFormatter.format(Date(cal.timeInMillis)).take(3)
                val dayFocus = focusSessions.filter { it.dateKey == key }
                DailyFocusDay(
                    dateKey = key,
                    dayLabel = label,
                    focusMinutes = dayFocus.sumOf { it.durationMinutes },
                    completedSessions = dayFocus.size
                )
            }

            // Compute consecutive focus days ending today (or yesterday if none yet today)
            val activeFocusDays = focusSessions.map { it.dateKey }.toSet()
            var streak = 0
            val streakCal = Calendar.getInstance().apply { timeInMillis = nowMillis }
            if (!activeFocusDays.contains(formatDateKey(streakCal.timeInMillis))) {
                streakCal.add(Calendar.DAY_OF_YEAR, -1)
            }
            while (activeFocusDays.contains(formatDateKey(streakCal.timeInMillis))) {
                streak++
                streakCal.add(Calendar.DAY_OF_YEAR, -1)
            }

            return ProductivitySummary(
                todayFocusMinutes = todayFocus.sumOf { it.durationMinutes },
                todayCompletedFocusSessions = todayFocus.size,
                todayBreakMinutes = todayBreak.sumOf { it.durationMinutes },
                totalFocusMinutesAllTime = focusSessions.sumOf { it.durationMinutes },
                totalCompletedFocusSessionsAllTime = focusSessions.size,
                currentStreakDays = streak,
                last7Days = last7Days
            )
        }
    }
}
