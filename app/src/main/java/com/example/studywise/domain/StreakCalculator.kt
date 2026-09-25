package com.example.studywise.domain

import com.example.studywise.data.entity.StudySession
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

data class StreakResult(
    val currentStreak: Int,
    val longestStreak: Int,
    val studiedToday: Boolean
)

object StreakCalculator {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    /**
     * Calculates the real study streak and longest streak from completed sessions.
     */
    fun calculateStreak(
        sessions: List<StudySession>,
        currentDateMillis: Long = System.currentTimeMillis()
    ): StreakResult {
        val completedSessions = sessions.filter { it.completed }
        if (completedSessions.isEmpty()) {
            return StreakResult(currentStreak = 0, longestStreak = 0, studiedToday = false)
        }

        // Normalize session timestamps to epoch days
        val sessionDays = completedSessions.map { session ->
            epochDay(session.date)
        }.distinct().sorted()

        val todayEpochDay = epochDay(currentDateMillis)
        val studiedToday = sessionDays.contains(todayEpochDay)

        // 1. Calculate Current Streak
        var currentStreak = 0
        var checkDay = if (studiedToday) todayEpochDay else todayEpochDay - 1

        while (sessionDays.contains(checkDay)) {
            currentStreak++
            checkDay--
        }

        // 2. Calculate Longest Streak in history
        var longestStreak = 0
        var tempStreak = 0
        var previousDay: Long? = null

        for (day in sessionDays) {
            if (previousDay == null || day == previousDay + 1) {
                tempStreak++
            } else {
                tempStreak = 1
            }
            if (tempStreak > longestStreak) {
                longestStreak = tempStreak
            }
            previousDay = day
        }

        return StreakResult(
            currentStreak = currentStreak,
            longestStreak = longestStreak.coerceAtLeast(currentStreak),
            studiedToday = studiedToday
        )
    }

    private fun epochDay(millis: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return TimeUnit.MILLISECONDS.toDays(cal.timeInMillis)
    }
}
