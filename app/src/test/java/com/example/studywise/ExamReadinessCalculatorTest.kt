package com.example.studywise

import com.example.studywise.data.entity.QuizAttempt
import com.example.studywise.data.entity.StudySession
import com.example.studywise.data.entity.Subject
import com.example.studywise.data.entity.Topic
import com.example.studywise.domain.ExamReadinessCalculator
import com.example.studywise.domain.ReadinessStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class ExamReadinessCalculatorTest {

    private val now = System.currentTimeMillis()

    @Test
    fun testHighReadinessScore() {
        val subject = Subject(
            id = 1,
            name = "DBMS",
            examDate = now + TimeUnit.DAYS.toMillis(10),
            preparationPercentage = 85,
            difficulty = "MEDIUM"
        )
        val topics = listOf(
            Topic(subjectId = 1, name = "SQL", completionPercentage = 90, masteryLevel = "MASTERED"),
            Topic(subjectId = 1, name = "Normalization", completionPercentage = 85, masteryLevel = "MASTERED")
        )
        val attempts = listOf(
            QuizAttempt(quizId = 1, subjectId = 1, score = 9, totalQuestions = 10, percentage = 90)
        )
        val sessions = (0..5).map { day ->
            StudySession(subjectId = 1, durationMinutes = 30, date = now - TimeUnit.DAYS.toMillis(day.toLong()))
        }

        val result = ExamReadinessCalculator.calculateReadiness(subject, topics, attempts, sessions)
        assertTrue("Readiness should be >= 80%, actual: ${result.overallPercentage}%", result.overallPercentage >= 80)
        assertEquals(ReadinessStatus.READY, result.status)
    }

    @Test
    fun testAtRiskReadinessScore() {
        val subject = Subject(
            id = 2,
            name = "Calculus",
            examDate = now + TimeUnit.DAYS.toMillis(5),
            preparationPercentage = 20,
            difficulty = "HARD"
        )
        val topics = listOf(
            Topic(subjectId = 2, name = "Integrals", completionPercentage = 15, masteryLevel = "UNSTUDIED")
        )
        val attempts = listOf(
            QuizAttempt(quizId = 2, subjectId = 2, score = 3, totalQuestions = 10, percentage = 30)
        )

        val result = ExamReadinessCalculator.calculateReadiness(subject, topics, attempts, emptyList())
        assertTrue("Readiness should be < 40%, actual: ${result.overallPercentage}%", result.overallPercentage < 40)
        assertEquals(ReadinessStatus.AT_RISK, result.status)
    }
}
