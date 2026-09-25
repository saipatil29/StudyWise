package com.example.studywise.domain

import com.example.studywise.data.entity.Badge
import kotlin.math.floor

data class UserLevelInfo(
    val level: Int,
    val currentLevelXp: Int,
    val xpRequiredForNextLevel: Int,
    val progressPercentage: Int,
    val totalXp: Int
)

object GamificationEngine {

    const val XP_PER_LEVEL = 300

    const val XP_SESSION_25_MIN = 25
    const val XP_DAILY_GOAL_MET = 50
    const val XP_QUIZ_COMPLETED = 20
    const val XP_QUIZ_EXCELLENCE_BONUS = 15
    const val XP_STREAK_7_DAYS = 100

    fun calculateLevelInfo(totalXp: Int): UserLevelInfo {
        val level = (totalXp / XP_PER_LEVEL) + 1
        val currentLevelXp = totalXp % XP_PER_LEVEL
        val progressPercentage = (currentLevelXp * 100) / XP_PER_LEVEL

        return UserLevelInfo(
            level = level,
            currentLevelXp = currentLevelXp,
            xpRequiredForNextLevel = XP_PER_LEVEL,
            progressPercentage = progressPercentage,
            totalXp = totalXp
        )
    }

    fun getDefaultBadges(): List<Badge> {
        return listOf(
            Badge(
                id = "first_session",
                name = "First Step",
                description = "Complete your first focus study session",
                isUnlocked = false
            ),
            Badge(
                id = "streak_7",
                name = "Consistency Champion",
                description = "Maintain a 7-day study streak",
                isUnlocked = false
            ),
            Badge(
                id = "study_10h",
                name = "Dedicated Scholar",
                description = "Log over 10 hours of total focus study time",
                isUnlocked = false
            ),
            Badge(
                id = "quiz_master",
                name = "Knowledge Seeker",
                description = "Score 80% or higher on 3 subject quizzes",
                isUnlocked = false
            ),
            Badge(
                id = "goal_crusher",
                name = "Goal Crusher",
                description = "Reach your daily study goal 5 times",
                isUnlocked = false
            )
        )
    }
}
