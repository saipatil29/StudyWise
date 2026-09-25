package com.example.studywise.domain

import java.util.Calendar
import java.util.concurrent.TimeUnit

enum class RecallQuality {
    HARD,   // 1 day
    OKAY,   // 3 days
    GOOD,   // 7 days
    EASY    // 14 days
}

object SpacedRepetitionEngine {

    /**
     * Calculates the next review date in epoch milliseconds
     * based on user recall self-assessment.
     */
    fun calculateNextReviewDate(quality: RecallQuality, baseDateMillis: Long = System.currentTimeMillis()): Long {
        val daysToAdd = when (quality) {
            RecallQuality.HARD -> 1
            RecallQuality.OKAY -> 3
            RecallQuality.GOOD -> 7
            RecallQuality.EASY -> 14
        }
        val cal = Calendar.getInstance().apply {
            timeInMillis = baseDateMillis
            add(Calendar.DAY_OF_YEAR, daysToAdd)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }
        return cal.timeInMillis
    }

    /**
     * Checks if a topic's next review date is due (today or earlier).
     */
    fun isRevisionDue(nextReviewDateMillis: Long?, currentDateMillis: Long = System.currentTimeMillis()): Boolean {
        if (nextReviewDateMillis == null) return false
        val cal = Calendar.getInstance().apply {
            timeInMillis = currentDateMillis
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }
        return nextReviewDateMillis <= cal.timeInMillis
    }

    /**
     * Maps recall quality to mastery progression:
     * HARD -> UNSTUDIED / FAMILIAR
     * OKAY -> FAMILIAR
     * GOOD/EASY -> MASTERED
     */
    fun getUpdatedMasteryLevel(quality: RecallQuality, currentMastery: String): String {
        return when (quality) {
            RecallQuality.HARD -> "FAMILIAR"
            RecallQuality.OKAY -> "FAMILIAR"
            RecallQuality.GOOD, RecallQuality.EASY -> "MASTERED"
        }
    }
}
