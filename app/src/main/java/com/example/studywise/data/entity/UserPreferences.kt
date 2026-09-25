package com.example.studywise.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_preferences")
data class UserPreferences(
    @PrimaryKey val id: Int = 1,
    val studentName: String = "Engineering Student",
    val studentEmail: String = "student@university.edu",
    val dailyGoalMinutes: Int = 120,
    val defaultFocusDurationMinutes: Int = 25,
    val notificationsEnabled: Boolean = true,
    val themeMode: String = "SYSTEM",
    val totalXp: Int = 0,
    val streakDays: Int = 0,
    val lastStudyDateMillis: Long = 0L
)
