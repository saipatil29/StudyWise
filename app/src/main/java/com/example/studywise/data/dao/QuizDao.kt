package com.example.studywise.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.studywise.data.entity.Quiz
import com.example.studywise.data.entity.QuizAttempt
import com.example.studywise.data.entity.QuizQuestion
import kotlinx.coroutines.flow.Flow

@Dao
interface QuizDao {
    @Query("SELECT * FROM quizzes ORDER BY id ASC")
    fun getAllQuizzes(): Flow<List<Quiz>>

    @Query("SELECT * FROM quizzes WHERE subjectId = :subjectId ORDER BY id ASC")
    fun getQuizzesForSubject(subjectId: Long): Flow<List<Quiz>>

    @Query("SELECT * FROM quizzes WHERE id = :id LIMIT 1")
    suspend fun getQuizById(id: Long): Quiz?

    @Query("SELECT * FROM quiz_questions WHERE quizId = :quizId ORDER BY id ASC")
    fun getQuestionsForQuiz(quizId: Long): Flow<List<QuizQuestion>>

    @Query("SELECT * FROM quiz_questions WHERE quizId = :quizId ORDER BY id ASC")
    suspend fun getQuestionsForQuizSync(quizId: Long): List<QuizQuestion>

    @Query("SELECT * FROM quiz_attempts ORDER BY attemptedAt DESC")
    fun getAllAttempts(): Flow<List<QuizAttempt>>

    @Query("SELECT * FROM quiz_attempts ORDER BY attemptedAt DESC")
    suspend fun getAllAttemptsSync(): List<QuizAttempt>

    @Query("SELECT * FROM quiz_attempts WHERE subjectId = :subjectId ORDER BY attemptedAt DESC")
    fun getAttemptsForSubject(subjectId: Long): Flow<List<QuizAttempt>>

    @Query("SELECT * FROM quiz_attempts WHERE subjectId = :subjectId ORDER BY attemptedAt DESC")
    suspend fun getAttemptsForSubjectSync(subjectId: Long): List<QuizAttempt>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuiz(quiz: Quiz): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<QuizQuestion>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttempt(attempt: QuizAttempt): Long

    @Query("DELETE FROM quiz_questions WHERE quizId IN (SELECT id FROM quizzes WHERE subjectId = :subjectId)")
    suspend fun deleteQuestionsForSubject(subjectId: Long)

    @Query("DELETE FROM quiz_attempts WHERE subjectId = :subjectId")
    suspend fun deleteAttemptsForSubject(subjectId: Long)

    @Query("DELETE FROM quizzes WHERE subjectId = :subjectId")
    suspend fun deleteQuizzesForSubject(subjectId: Long)

    @Query("DELETE FROM quizzes")
    suspend fun clearQuizzes()

    @Query("DELETE FROM quiz_attempts")
    suspend fun clearAttempts()
}
