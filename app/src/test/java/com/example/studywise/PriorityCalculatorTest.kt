package com.example.studywise

import com.example.studywise.data.entity.StudySession
import com.example.studywise.data.entity.Subject
import com.example.studywise.data.entity.Topic
import com.example.studywise.domain.PriorityCalculator
import com.example.studywise.domain.PriorityLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class PriorityCalculatorTest {

    private val now = System.currentTimeMillis()

    @Test
    fun testExamUrgencyScoreLevels() {
        // Today or tomorrow (<= 1 day)
        val scoreToday = PriorityCalculator.calculateExamUrgencyScore(now + TimeUnit.HOURS.toMillis(12), now)
        assertEquals(100.0, scoreToday, 0.01)

        // 3 days
        val score3Days = PriorityCalculator.calculateExamUrgencyScore(now + TimeUnit.DAYS.toMillis(3), now)
        assertEquals(85.0, score3Days, 0.01)

        // 6 days
        val score6Days = PriorityCalculator.calculateExamUrgencyScore(now + TimeUnit.DAYS.toMillis(6), now)
        assertEquals(70.0, score6Days, 0.01)

        // 10 days
        val score10Days = PriorityCalculator.calculateExamUrgencyScore(now + TimeUnit.DAYS.toMillis(10), now)
        assertEquals(50.0, score10Days, 0.01)

        // Passed
        val scorePassed = PriorityCalculator.calculateExamUrgencyScore(now - TimeUnit.DAYS.toMillis(2), now)
        assertEquals(10.0, scorePassed, 0.01)
    }

    @Test
    fun testDifficultyScore() {
        assertEquals(100.0, PriorityCalculator.calculateDifficultyScore("HARD"), 0.01)
        assertEquals(60.0, PriorityCalculator.calculateDifficultyScore("MEDIUM"), 0.01)
        assertEquals(30.0, PriorityCalculator.calculateDifficultyScore("EASY"), 0.01)
    }

    @Test
    fun testPriorityClampingAndLevels() {
        // Critical subject: exam in 2 days, 20% prep, Hard, never studied
        val criticalSubject = Subject(
            name = "Computer Networks",
            examDate = now + TimeUnit.DAYS.toMillis(2),
            preparationPercentage = 20,
            difficulty = "HARD",
            dailyGoalMinutes = 60
        )
        val score = PriorityCalculator.calculatePriorityScore(criticalSubject, emptyList(), emptyList(), now)
        assertTrue("Critical score should be >= 80, actual: $score", score >= 80)
        assertEquals(PriorityLevel.CRITICAL, PriorityCalculator.getPriorityLevel(score))

        // Low priority subject: exam in 45 days, 90% prep, Easy, studied today
        val lowSubject = Subject(
            name = "Environmental Science",
            examDate = now + TimeUnit.DAYS.toMillis(45),
            preparationPercentage = 90,
            difficulty = "EASY",
            dailyGoalMinutes = 30
        )
        val todaySession = listOf(StudySession(subjectId = 0, durationMinutes = 30, date = now))
        val lowScore = PriorityCalculator.calculatePriorityScore(lowSubject, emptyList(), todaySession, now)
        assertTrue("Low score should be < 40, actual: $lowScore", lowScore < 40)
        assertEquals(PriorityLevel.LOW, PriorityCalculator.getPriorityLevel(lowScore))
    }

    @Test
    fun testRecommendationReasonContainsContext() {
        val subject = Subject(
            name = "Operating Systems",
            examDate = now + TimeUnit.DAYS.toMillis(2),
            preparationPercentage = 30,
            difficulty = "HARD"
        )
        val reason = PriorityCalculator.getRecommendationReason(subject, emptyList(), emptyList(), now)
        assertTrue(reason.contains("exam is approaching"))
        assertTrue(reason.contains("preparation is below 50%"))
        assertTrue(reason.contains("high-difficulty"))
    }
}
