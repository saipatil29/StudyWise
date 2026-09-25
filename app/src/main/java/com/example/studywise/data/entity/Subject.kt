package com.example.studywise.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subjects")
data class Subject(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val examDate: Long,
    val preparationPercentage: Int,
    val difficulty: String, // EASY, MEDIUM, HARD
    val dailyGoalMinutes: Int = 45,
    val description: String = "",
    val colorHex: String = "#4F46E5",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
