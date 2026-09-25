package com.example.studywise

import com.example.studywise.data.entity.StudySession
import com.example.studywise.data.entity.Subject
import com.example.studywise.domain.AdaptiveScheduler
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class AdaptiveSchedulerTest {

    private val now = System.currentTimeMillis()

    @Test
    fun testScheduleDoesNotExceedAvailableMinutes() {
        val subjects = listOf(
            Subject(id = 1, name = "Computer Networks", examDate = now + TimeUnit.DAYS.toMillis(3), preparationPercentage = 40, difficulty = "HARD"),
            Subject(id = 2, name = "DBMS", examDate = now + TimeUnit.DAYS.toMillis(7), preparationPercentage = 70, difficulty = "MEDIUM"),
            Subject(id = 3, name = "AI", examDate = now + TimeUnit.DAYS.toMillis(14), preparationPercentage = 50, difficulty = "MEDIUM")
        )
        val priorityMap = mapOf(1L to 85, 2L to 60, 3L to 45)
        val dailyGoal = 120

        val schedule = AdaptiveScheduler.generateDailySchedule(
            subjects = subjects,
            subjectPriorities = priorityMap,
            availableMinutes = dailyGoal
        )

        assertTrue(schedule.isNotEmpty())
        val totalScheduledMinutes = schedule.sumOf { it.durationMinutes }
        assertTrue("Total scheduled ($totalScheduledMinutes) should not exceed available ($dailyGoal)", totalScheduledMinutes <= dailyGoal)
    }

    @Test
    fun testAdaptiveReschedulingWhenSessionMissed() {
        val subjects = listOf(
            Subject(id = 1, name = "Computer Networks", examDate = now + TimeUnit.DAYS.toMillis(3), preparationPercentage = 40, difficulty = "HARD", dailyGoalMinutes = 50),
            Subject(id = 2, name = "DBMS", examDate = now + TimeUnit.DAYS.toMillis(7), preparationPercentage = 70, difficulty = "MEDIUM", dailyGoalMinutes = 45)
        )
        val priorityMap = mapOf(1L to 70, 2L to 65)

        // Yesterday user only studied DBMS (45m), missed Computer Networks (0m)
        val yesterdaySession = listOf(
            StudySession(subjectId = 2, durationMinutes = 45, date = now - TimeUnit.DAYS.toMillis(1))
        )

        val schedule = AdaptiveScheduler.generateDailySchedule(
            subjects = subjects,
            subjectPriorities = priorityMap,
            availableMinutes = 90,
            yesterdaySessions = yesterdaySession
        )

        val cnSlot = schedule.find { it.subjectId == 1L }
        assertTrue("Computer Networks should be rescheduled due to missed session yesterday", cnSlot?.isRescheduled == true)
    }
}
