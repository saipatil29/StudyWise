package com.example.studywise.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "topics",
    foreignKeys = [
        ForeignKey(
            entity = Subject::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["subjectId"])]
)
data class Topic(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long,
    val name: String,
    val completionPercentage: Int = 0,
    val masteryLevel: String = "UNSTUDIED", // UNSTUDIED, FAMILIAR, MASTERED
    val lastReviewed: Long? = null,
    val nextReviewDate: Long? = null
)
