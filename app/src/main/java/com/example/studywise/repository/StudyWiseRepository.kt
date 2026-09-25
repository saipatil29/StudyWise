package com.example.studywise.repository

import com.example.studywise.data.dao.BadgeDao
import com.example.studywise.data.dao.QuizDao
import com.example.studywise.data.dao.StudySessionDao
import com.example.studywise.data.dao.SubjectDao
import com.example.studywise.data.dao.TopicDao
import com.example.studywise.data.dao.UserPreferencesDao
import com.example.studywise.data.entity.Badge
import com.example.studywise.data.entity.Quiz
import com.example.studywise.data.entity.QuizAttempt
import com.example.studywise.data.entity.QuizQuestion
import com.example.studywise.data.entity.StudySession
import com.example.studywise.data.entity.Subject
import com.example.studywise.data.entity.Topic
import com.example.studywise.data.entity.UserPreferences
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class StudyWiseRepository(
    private val subjectDao: SubjectDao,
    private val topicDao: TopicDao,
    private val studySessionDao: StudySessionDao,
    private val quizDao: QuizDao,
    private val userPreferencesDao: UserPreferencesDao,
    private val badgeDao: BadgeDao
) {
    val allSubjects: Flow<List<Subject>> = subjectDao.getAllSubjects()
    val allTopics: Flow<List<Topic>> = topicDao.getAllTopics()
    val allSessions: Flow<List<StudySession>> = studySessionDao.getAllSessions()
    val allQuizzes: Flow<List<Quiz>> = quizDao.getAllQuizzes()
    val allAttempts: Flow<List<QuizAttempt>> = quizDao.getAllAttempts()
    val userPreferences: Flow<UserPreferences?> = userPreferencesDao.getUserPreferences()
    val allBadges: Flow<List<Badge>> = badgeDao.getAllBadges()

    suspend fun getAllSubjectsSync(): List<Subject> = subjectDao.getAllSubjectsSync()
    suspend fun getSubjectById(id: Long): Subject? = subjectDao.getSubjectById(id)
    fun getSubjectByIdFlow(id: Long): Flow<Subject?> = subjectDao.getSubjectByIdFlow(id)
    suspend fun insertSubject(subject: Subject): Long = subjectDao.insertSubject(subject)
    suspend fun updateSubject(subject: Subject) = subjectDao.updateSubject(subject)
    suspend fun deleteSubject(subject: Subject) {
        deleteSubjectById(subject.id)
    }

    suspend fun deleteSubjectById(id: Long) {
        quizDao.deleteQuestionsForSubject(id)
        quizDao.deleteAttemptsForSubject(id)
        quizDao.deleteQuizzesForSubject(id)
        topicDao.deleteTopicsForSubject(id)
        studySessionDao.deleteSessionsForSubject(id)
        subjectDao.deleteSubjectById(id)
    }

    fun getTopicsForSubject(subjectId: Long): Flow<List<Topic>> = topicDao.getTopicsForSubject(subjectId)
    suspend fun getTopicsForSubjectSync(subjectId: Long): List<Topic> = topicDao.getTopicsForSubjectSync(subjectId)
    fun getTopicsDueForRevision(cutoffDateMillis: Long): Flow<List<Topic>> = topicDao.getTopicsDueForRevision(cutoffDateMillis)
    suspend fun insertTopic(topic: Topic): Long = topicDao.insertTopic(topic)
    suspend fun updateTopic(topic: Topic) = topicDao.updateTopic(topic)
    suspend fun deleteTopic(topic: Topic) = topicDao.deleteTopic(topic)

    fun getSessionsForSubject(subjectId: Long): Flow<List<StudySession>> = studySessionDao.getSessionsForSubject(subjectId)
    suspend fun getSessionsForSubjectSync(subjectId: Long): List<StudySession> = studySessionDao.getSessionsForSubjectSync(subjectId)

    fun getTodaySessions(): Flow<List<StudySession>> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startMillis = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, 1)
        val endMillis = cal.timeInMillis - 1
        return studySessionDao.getSessionsForDateRange(startMillis, endMillis)
    }

    suspend fun getTodaySessionsSync(): List<StudySession> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startMillis = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, 1)
        val endMillis = cal.timeInMillis - 1
        return studySessionDao.getSessionsForDateRangeSync(startMillis, endMillis)
    }

    suspend fun recordStudySession(subjectId: Long, durationMinutes: Int, notes: String = ""): Long {
        val session = StudySession(
            subjectId = subjectId,
            durationMinutes = durationMinutes,
            date = System.currentTimeMillis(),
            completed = true,
            notes = notes
        )
        val sessionId = studySessionDao.insertSession(session)
        // Award XP (1 minute = 1 XP, minimum 10 XP)
        val xpToAward = (durationMinutes * 1).coerceAtLeast(10)
        awardXp(xpToAward)
        return sessionId
    }

    fun getQuizzesForSubject(subjectId: Long): Flow<List<Quiz>> = quizDao.getQuizzesForSubject(subjectId)
    fun getQuestionsForQuiz(quizId: Long): Flow<List<QuizQuestion>> = quizDao.getQuestionsForQuiz(quizId)
    suspend fun getQuestionsForQuizSync(quizId: Long): List<QuizQuestion> = quizDao.getQuestionsForQuizSync(quizId)
    suspend fun getQuizById(id: Long): Quiz? = quizDao.getQuizById(id)

    suspend fun recordQuizAttempt(quizId: Long, subjectId: Long, score: Int, total: Int): Long {
        val percentage = if (total > 0) (score * 100) / total else 0
        val attempt = QuizAttempt(
            quizId = quizId,
            subjectId = subjectId,
            score = score,
            totalQuestions = total,
            percentage = percentage,
            attemptedAt = System.currentTimeMillis()
        )
        val id = quizDao.insertAttempt(attempt)
        // Award XP: +20 for quiz completion, +10 bonus for >= 80%
        val xp = if (percentage >= 80) 30 else 20
        awardXp(xp)
        return id
    }

    suspend fun getUserPreferencesSync(): UserPreferences {
        return userPreferencesDao.getUserPreferencesSync() ?: UserPreferences().also {
            userPreferencesDao.insertOrUpdatePreferences(it)
        }
    }

    suspend fun updateUserPreferences(preferences: UserPreferences) {
        userPreferencesDao.insertOrUpdatePreferences(preferences)
    }

    suspend fun awardXp(points: Int) {
        val current = getUserPreferencesSync()
        val updated = current.copy(totalXp = current.totalXp + points)
        userPreferencesDao.insertOrUpdatePreferences(updated)
        evaluateBadges()
    }

    suspend fun evaluateBadges() {
        val prefs = getUserPreferencesSync()
        val allSessions = studySessionDao.getAllSessionsSync()
        val allAttempts = quizDao.getAllAttemptsSync()
        val existingBadges = badgeDao.getAllBadgesSync().associateBy { it.id }.toMutableMap()

        // 1. First Session
        if (allSessions.isNotEmpty()) {
            val badge = existingBadges["first_session"] ?: Badge("first_session", "First Step", "Completed your first focus study session")
            if (!badge.isUnlocked) {
                badgeDao.updateBadge(badge.copy(isUnlocked = true, unlockedAt = System.currentTimeMillis()))
            }
        }

        // 2. 10 Hours Studied (600 minutes)
        val totalMinutes = allSessions.sumOf { it.durationMinutes }
        if (totalMinutes >= 600) {
            val badge = existingBadges["study_10h"] ?: Badge("study_10h", "Dedicated Scholar", "Logged over 10 hours of total study time")
            if (!badge.isUnlocked) {
                badgeDao.updateBadge(badge.copy(isUnlocked = true, unlockedAt = System.currentTimeMillis()))
            }
        }

        // 3. Quiz Master (3 quizzes with >= 80%)
        val highScores = allAttempts.count { it.percentage >= 80 }
        if (highScores >= 3) {
            val badge = existingBadges["quiz_master"] ?: Badge("quiz_master", "Knowledge Seeker", "Scored 80% or higher on 3 quizzes")
            if (!badge.isUnlocked) {
                badgeDao.updateBadge(badge.copy(isUnlocked = true, unlockedAt = System.currentTimeMillis()))
            }
        }

        // 4. Streak 7
        if (prefs.streakDays >= 7) {
            val badge = existingBadges["streak_7"] ?: Badge("streak_7", "Consistency Champion", "Maintained a 7-day study streak")
            if (!badge.isUnlocked) {
                badgeDao.updateBadge(badge.copy(isUnlocked = true, unlockedAt = System.currentTimeMillis()))
            }
        }
    }

    suspend fun clearAllData() {
        subjectDao.clearAll()
        topicDao.clearAll()
        studySessionDao.clearAll()
        quizDao.clearQuizzes()
        quizDao.clearAttempts()
        badgeDao.clearAll()
        userPreferencesDao.insertOrUpdatePreferences(UserPreferences())
    }
}
