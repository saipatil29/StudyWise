package com.example.studywise

import com.example.studywise.data.entity.StudySession
import com.example.studywise.domain.StreakCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class StreakCalculatorTest {

    private val now = System.currentTimeMillis()

    @Test
    fun testEmptySessionsProducesZeroStreak() {
        val result = StreakCalculator.calculateStreak(emptyList(), now)
        assertEquals(0, result.currentStreak)
        assertEquals(0, result.longestStreak)
        assertFalse(result.studiedToday)
    }

    @Test
    fun testConsecutiveDaysStreak() {
        // Studied today, yesterday, 2 days ago, 3 days ago (4-day streak)
        val sessions = listOf(
            StudySession(subjectId = 1, durationMinutes = 25, date = now),
            StudySession(subjectId = 1, durationMinutes = 30, date = now - TimeUnit.DAYS.toMillis(1)),
            StudySession(subjectId = 1, durationMinutes = 40, date = now - TimeUnit.DAYS.toMillis(2)),
            StudySession(subjectId = 1, durationMinutes = 25, date = now - TimeUnit.DAYS.toMillis(3))
        )
        val result = StreakCalculator.calculateStreak(sessions, now)
        assertEquals(4, result.currentStreak)
        assertEquals(4, result.longestStreak)
        assertTrue(result.studiedToday)
    }

    @Test
    fun testBrokenStreakReturnsCorrectCurrentStreak() {
        // Studied today and yesterday, but missed 2 days ago. Studied 3, 4, 5, 6 days ago (longest 4)
        val sessions = listOf(
            StudySession(subjectId = 1, durationMinutes = 25, date = now),
            StudySession(subjectId = 1, durationMinutes = 30, date = now - TimeUnit.DAYS.toMillis(1)),
            // gap at day 2
            StudySession(subjectId = 1, durationMinutes = 30, date = now - TimeUnit.DAYS.toMillis(3)),
            StudySession(subjectId = 1, durationMinutes = 30, date = now - TimeUnit.DAYS.toMillis(4)),
            StudySession(subjectId = 1, durationMinutes = 30, date = now - TimeUnit.DAYS.toMillis(5)),
            StudySession(subjectId = 1, durationMinutes = 30, date = now - TimeUnit.DAYS.toMillis(6))
        )
        val result = StreakCalculator.calculateStreak(sessions, now)
        assertEquals(2, result.currentStreak)
        assertEquals(4, result.longestStreak)
    }
}
