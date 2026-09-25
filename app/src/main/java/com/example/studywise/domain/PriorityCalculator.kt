package com.example.studywise.domain

import com.example.studywise.data.entity.StudySession
import com.example.studywise.data.entity.Subject
import com.example.studywise.data.entity.Topic
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

enum class PriorityLevel {
    CRITICAL,
    HIGH,
    MEDIUM,
    LOW
}

object PriorityCalculator {

    /**
     * Calculates the smart priority score (0-100) for a subject.
     * Formula:
     * priority = examUrgency * 0.35 +
     *            preparationGap * 0.30 +
     *            difficultyScore * 0.15 +
     *            recentStudyScore * 0.10 +
     *            topicGap * 0.10
     */
    fun calculatePriorityScore(
        subject: Subject,
        topics: List<Topic> = emptyList(),
        recentSessions: List<StudySession> = emptyList(),
        currentDateMillis: Long = System.currentTimeMillis()
    ): Int {
        val urgencyScore = calculateExamUrgencyScore(subject.examDate, currentDateMillis)
        val prepGapScore = (100 - subject.preparationPercentage.coerceIn(0, 100)).toDouble()
        val diffScore = calculateDifficultyScore(subject.difficulty)
        val recentStudyScore = calculateRecentStudyScore(recentSessions, currentDateMillis)
        val topicGapScore = calculateTopicGapScore(topics)

        val rawScore = (urgencyScore * 0.35) +
                (prepGapScore * 0.30) +
                (diffScore * 0.15) +
                (recentStudyScore * 0.10) +
                (topicGapScore * 0.10)

        return rawScore.roundToInt().coerceIn(0, 100)
    }

    /**
     * Exam Urgency Score:
     * Passed: 10
     * Today/Tomorrow (<= 1 day): 100
     * 2-3 days: 85
     * 4-7 days: 70
     * 8-14 days: 50
     * 15-30 days: 30
     * 31+ days: 10
     */
    fun calculateExamUrgencyScore(examDateMillis: Long, currentDateMillis: Long): Double {
        val diffMillis = examDateMillis - currentDateMillis
        val daysUntilExam = TimeUnit.MILLISECONDS.toDays(diffMillis)

        return when {
            diffMillis < 0 && daysUntilExam < 0 -> 10.0 // Exam already passed
            daysUntilExam <= 1 -> 100.0 // Exam today or tomorrow
            daysUntilExam in 2..3 -> 85.0
            daysUntilExam in 4..7 -> 70.0
            daysUntilExam in 8..14 -> 50.0
            daysUntilExam in 15..30 -> 30.0
            else -> 10.0
        }
    }

    /**
     * Difficulty Score:
     * Easy = 30
     * Medium = 60
     * Hard = 100
     */
    fun calculateDifficultyScore(difficulty: String): Double {
        return when (difficulty.uppercase().trim()) {
            "HARD" -> 100.0
            "MEDIUM" -> 60.0
            "EASY" -> 30.0
            else -> 60.0
        }
    }

    /**
     * Recent Study Score:
     * If not studied recently, score increases (higher priority to catch up).
     */
    fun calculateRecentStudyScore(
        sessions: List<StudySession>,
        currentDateMillis: Long = System.currentTimeMillis()
    ): Double {
        val latestSession = sessions.maxByOrNull { it.date } ?: return 100.0 // Never studied
        val diffMillis = currentDateMillis - latestSession.date
        val daysSinceStudy = TimeUnit.MILLISECONDS.toDays(diffMillis)

        return when {
            daysSinceStudy >= 7 -> 90.0
            daysSinceStudy in 4..6 -> 70.0
            daysSinceStudy in 2..3 -> 40.0
            daysSinceStudy == 1L -> 20.0
            else -> 0.0 // Studied today
        }
    }

    /**
     * Topic Gap: 100 - average topic completion percentage.
     * If no topics exist, returns 50.0 as neutral fallback.
     */
    fun calculateTopicGapScore(topics: List<Topic>): Double {
        if (topics.isEmpty()) return 50.0
        val avgCompletion = topics.map { it.completionPercentage }.average()
        return (100.0 - avgCompletion).coerceIn(0.0, 100.0)
    }

    fun getPriorityLevel(score: Int): PriorityLevel {
        return when {
            score >= 80 -> PriorityLevel.CRITICAL
            score >= 60 -> PriorityLevel.HIGH
            score >= 40 -> PriorityLevel.MEDIUM
            else -> PriorityLevel.LOW
        }
    }

    /**
     * Generates a dynamic human-readable explanation of why this subject has high priority.
     */
    fun getRecommendationReason(
        subject: Subject,
        topics: List<Topic> = emptyList(),
        recentSessions: List<StudySession> = emptyList(),
        currentDateMillis: Long = System.currentTimeMillis()
    ): String {
        val diffMillis = subject.examDate - currentDateMillis
        val daysUntil = TimeUnit.MILLISECONDS.toDays(diffMillis)
        val reasons = mutableListOf<String>()

        if (daysUntil in 0..3) {
            val dayText = if (daysUntil == 0L) "today" else if (daysUntil == 1L) "tomorrow" else "$daysUntil days"
            reasons.add("your exam is approaching in $dayText")
        } else if (daysUntil in 4..7) {
            reasons.add("your exam is only $daysUntil days away")
        }

        if (subject.preparationPercentage < 50) {
            reasons.add("your preparation is below 50% (${subject.preparationPercentage}%)")
        }

        if (subject.difficulty.equals("HARD", ignoreCase = true)) {
            reasons.add("it is marked as a high-difficulty subject")
        }

        val latestSession = recentSessions.maxByOrNull { it.date }
        if (latestSession == null) {
            reasons.add("you have not logged any study sessions for it yet")
        } else {
            val daysSince = TimeUnit.MILLISECONDS.toDays(currentDateMillis - latestSession.date)
            if (daysSince >= 3) {
                reasons.add("you haven't reviewed it in $daysSince days")
            }
        }

        return if (reasons.isNotEmpty()) {
            "Prioritized because " + reasons.joinToString(", ") + "."
        } else {
            "Regular maintenance review recommended to retain syllabus mastery before the exam."
        }
    }

    /**
     * Computes recommended study session duration in minutes based on priority and subject daily goal.
     */
    fun getRecommendedStudyMinutes(priorityScore: Int, dailyGoalMinutes: Int): Int {
        val baseMinutes = if (dailyGoalMinutes > 0) dailyGoalMinutes else 45
        return when (getPriorityLevel(priorityScore)) {
            PriorityLevel.CRITICAL -> (baseMinutes * 1.33).roundToInt().coerceIn(45, 90)
            PriorityLevel.HIGH -> baseMinutes.coerceIn(30, 60)
            PriorityLevel.MEDIUM -> (baseMinutes * 0.8).roundToInt().coerceIn(25, 45)
            PriorityLevel.LOW -> (baseMinutes * 0.6).roundToInt().coerceIn(20, 30)
        }
    }
}
