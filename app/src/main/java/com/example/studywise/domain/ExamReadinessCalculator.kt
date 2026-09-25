package com.example.studywise.domain

import com.example.studywise.data.entity.QuizAttempt
import com.example.studywise.data.entity.StudySession
import com.example.studywise.data.entity.Subject
import com.example.studywise.data.entity.Topic
import kotlin.math.roundToInt

data class ExamReadinessResult(
    val overallPercentage: Int,
    val status: ReadinessStatus,
    val preparationScore: Int,
    val topicScore: Int,
    val quizScore: Int,
    val consistencyScore: Int
)

enum class ReadinessStatus(val label: String) {
    READY("Ready"),
    ON_TRACK("On Track"),
    NEEDS_WORK("Needs Work"),
    AT_RISK("At Risk")
}

object ExamReadinessCalculator {

    /**
     * Exam Readiness Score:
     * Preparation = 35%
     * Topic completion = 30%
     * Quiz performance = 20%
     * Study consistency = 15%
     */
    fun calculateReadiness(
        subject: Subject,
        topics: List<Topic>,
        quizAttempts: List<QuizAttempt>,
        studySessions: List<StudySession>
    ): ExamReadinessResult {
        // 1. Preparation factor (0-100)
        val prepScore = subject.preparationPercentage.coerceIn(0, 100)

        // 2. Topic completion factor (0-100)
        val topicScore = if (topics.isEmpty()) {
            prepScore // Fallback to subjective preparation if no individual topics are logged
        } else {
            topics.map { it.completionPercentage }.average().roundToInt().coerceIn(0, 100)
        }

        // 3. Quiz performance factor (0-100)
        val quizScore = if (quizAttempts.isEmpty()) {
            70 // Neutral baseline if no quizzes taken yet
        } else {
            quizAttempts.map { it.percentage }.average().roundToInt().coerceIn(0, 100)
        }

        // 4. Study consistency factor (0-100)
        // Calculated by evaluating sessions logged in the last 14 days
        val consistencyScore = calculateConsistencyScore(studySessions)

        val totalScore = (prepScore * 0.35) +
                (topicScore * 0.30) +
                (quizScore * 0.20) +
                (consistencyScore * 0.15)

        val finalPercentage = totalScore.roundToInt().coerceIn(0, 100)

        val status = when {
            finalPercentage >= 80 -> ReadinessStatus.READY
            finalPercentage >= 60 -> ReadinessStatus.ON_TRACK
            finalPercentage >= 40 -> ReadinessStatus.NEEDS_WORK
            else -> ReadinessStatus.AT_RISK
        }

        return ExamReadinessResult(
            overallPercentage = finalPercentage,
            status = status,
            preparationScore = prepScore,
            topicScore = topicScore,
            quizScore = quizScore,
            consistencyScore = consistencyScore
        )
    }

    private fun calculateConsistencyScore(sessions: List<StudySession>): Int {
        if (sessions.isEmpty()) return 20
        val daysWithSessions = sessions.map { session ->
            session.date / (1000 * 60 * 60 * 24)
        }.distinct().size

        // Having studied on 5 or more distinct days in recent history gives 100% consistency
        return (daysWithSessions * 20).coerceIn(20, 100)
    }
}
