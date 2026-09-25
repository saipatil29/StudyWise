package com.example.studywise.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.studywise.data.entity.StudySession
import kotlinx.coroutines.flow.Flow

@Dao
interface StudySessionDao {
    @Query("SELECT * FROM study_sessions ORDER BY date DESC")
    fun getAllSessions(): Flow<List<StudySession>>

    @Query("SELECT * FROM study_sessions ORDER BY date DESC")
    suspend fun getAllSessionsSync(): List<StudySession>

    @Query("SELECT * FROM study_sessions WHERE subjectId = :subjectId ORDER BY date DESC")
    fun getSessionsForSubject(subjectId: Long): Flow<List<StudySession>>

    @Query("SELECT * FROM study_sessions WHERE subjectId = :subjectId ORDER BY date DESC")
    suspend fun getSessionsForSubjectSync(subjectId: Long): List<StudySession>

    @Query("SELECT * FROM study_sessions WHERE date >= :startMillis AND date <= :endMillis ORDER BY date DESC")
    fun getSessionsForDateRange(startMillis: Long, endMillis: Long): Flow<List<StudySession>>

    @Query("SELECT * FROM study_sessions WHERE date >= :startMillis AND date <= :endMillis ORDER BY date DESC")
    suspend fun getSessionsForDateRangeSync(startMillis: Long, endMillis: Long): List<StudySession>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: StudySession): Long

    @Delete
    suspend fun deleteSession(session: StudySession)

    @Query("DELETE FROM study_sessions WHERE subjectId = :subjectId")
    suspend fun deleteSessionsForSubject(subjectId: Long)

    @Query("DELETE FROM study_sessions")
    suspend fun clearAll()
}
