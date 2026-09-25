package com.example.studywise.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studywise.data.entity.Subject
import com.example.studywise.domain.AdaptiveScheduler
import com.example.studywise.domain.PlannedStudySlot
import com.example.studywise.domain.PriorityCalculator
import com.example.studywise.domain.PriorityLevel
import com.example.studywise.domain.StreakCalculator
import com.example.studywise.domain.StreakResult
import com.example.studywise.repository.StudyWiseRepository
import com.example.studywise.utils.DateTimeUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class RecommendedSubjectUi(
    val subject: Subject,
    val priorityScore: Int,
    val priorityLevel: PriorityLevel,
    val recommendedDurationMinutes: Int,
    val reason: String
)

data class HomeUiState(
    val isLoading: Boolean = true,
    val studentName: String = "Student",
    val streak: StreakResult = StreakResult(0, 0, false),
    val dailyGoalMinutes: Int = 120,
    val completedTodayMinutes: Int = 0,
    val subjectsCount: Int = 0,
    val totalPlannedMinutes: Int = 0,
    val topRecommendation: RecommendedSubjectUi? = null,
    val upcomingExams: List<Subject> = emptyList(),
    val todaysPlan: List<PlannedStudySlot> = emptyList()
)

class HomeViewModel(
    private val repository: StudyWiseRepository
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        repository.allSubjects,
        repository.allTopics,
        repository.allSessions,
        repository.userPreferences
    ) { subjects, topics, sessions, userPreferences ->
        val now = System.currentTimeMillis()
        val dailyGoal = userPreferences?.dailyGoalMinutes ?: 120
        val studentName = userPreferences?.studentName ?: "Student"

        // Streak
        val streakResult = StreakCalculator.calculateStreak(sessions, now)

        // Today's completed study time
        val startOfToday = DateTimeUtils.getStartOfToday()
        val endOfToday = DateTimeUtils.getEndOfToday()
        val todaySessions = sessions.filter { it.date in startOfToday..endOfToday && it.completed }
        val completedTodayMinutes = todaySessions.sumOf { it.durationMinutes }

        // Yesterday's sessions for adaptive scheduler
        val startOfYesterday = DateTimeUtils.getStartOfYesterday()
        val endOfYesterday = DateTimeUtils.getEndOfYesterday()
        val yesterdaySessions = sessions.filter { it.date in startOfYesterday..endOfYesterday && it.completed }

        // Group topics and sessions by subject
        val topicsBySubject = topics.groupBy { it.subjectId }
        val sessionsBySubject = sessions.groupBy { it.subjectId }

        // Calculate priority for each subject
        val priorityMap = subjects.associate { subject ->
            val subTopics = topicsBySubject[subject.id] ?: emptyList()
            val subSessions = sessionsBySubject[subject.id] ?: emptyList()
            val score = PriorityCalculator.calculatePriorityScore(subject, subTopics, subSessions, now)
            subject.id to score
        }

        // Top recommendation
        val topRecommendation = if (subjects.isNotEmpty()) {
            val topSubject = subjects.maxByOrNull { priorityMap[it.id] ?: 0 }!!
            val topScore = priorityMap[topSubject.id] ?: 0
            val subTopics = topicsBySubject[topSubject.id] ?: emptyList()
            val subSessions = sessionsBySubject[topSubject.id] ?: emptyList()
            val reason = PriorityCalculator.getRecommendationReason(topSubject, subTopics, subSessions, now)
            val duration = PriorityCalculator.getRecommendedStudyMinutes(topScore, topSubject.dailyGoalMinutes)

            RecommendedSubjectUi(
                subject = topSubject,
                priorityScore = topScore,
                priorityLevel = PriorityCalculator.getPriorityLevel(topScore),
                recommendedDurationMinutes = duration,
                reason = reason
            )
        } else null

        // Upcoming exams sorted by exam date
        val upcomingExams = subjects.sortedBy { it.examDate }

        // Generate Today's Adaptive Plan
        val plannedSlots = AdaptiveScheduler.generateDailySchedule(
            subjects = subjects,
            subjectPriorities = priorityMap,
            availableMinutes = dailyGoal,
            yesterdaySessions = yesterdaySessions
        )
        val totalPlannedMinutes = plannedSlots.sumOf { it.durationMinutes }

        HomeUiState(
            isLoading = false,
            studentName = studentName,
            streak = streakResult,
            dailyGoalMinutes = dailyGoal,
            completedTodayMinutes = completedTodayMinutes,
            subjectsCount = subjects.size,
            totalPlannedMinutes = totalPlannedMinutes,
            topRecommendation = topRecommendation,
            upcomingExams = upcomingExams,
            todaysPlan = plannedSlots
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )
}
