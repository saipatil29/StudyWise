package com.example.studywise.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.studywise.data.entity.Topic
import kotlinx.coroutines.flow.Flow

@Dao
interface TopicDao {
    @Query("SELECT * FROM topics WHERE subjectId = :subjectId ORDER BY id ASC")
    fun getTopicsForSubject(subjectId: Long): Flow<List<Topic>>

    @Query("SELECT * FROM topics WHERE subjectId = :subjectId ORDER BY id ASC")
    suspend fun getTopicsForSubjectSync(subjectId: Long): List<Topic>

    @Query("SELECT * FROM topics ORDER BY id ASC")
    fun getAllTopics(): Flow<List<Topic>>

    @Query("SELECT * FROM topics ORDER BY id ASC")
    suspend fun getAllTopicsSync(): List<Topic>

    @Query("SELECT * FROM topics WHERE nextReviewDate IS NOT NULL AND nextReviewDate <= :cutoffDateMillis ORDER BY nextReviewDate ASC")
    fun getTopicsDueForRevision(cutoffDateMillis: Long): Flow<List<Topic>>

    @Query("SELECT * FROM topics WHERE id = :id LIMIT 1")
    suspend fun getTopicById(id: Long): Topic?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopic(topic: Topic): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopics(topics: List<Topic>)

    @Update
    suspend fun updateTopic(topic: Topic)

    @Delete
    suspend fun deleteTopic(topic: Topic)

    @Query("DELETE FROM topics WHERE subjectId = :subjectId")
    suspend fun deleteTopicsForSubject(subjectId: Long)

    @Query("DELETE FROM topics")
    suspend fun clearAll()
}
