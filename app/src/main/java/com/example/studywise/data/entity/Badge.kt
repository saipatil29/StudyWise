package com.example.studywise.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "badges")
data class Badge(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val isUnlocked: Boolean = false,
    val unlockedAt: Long? = null
)
