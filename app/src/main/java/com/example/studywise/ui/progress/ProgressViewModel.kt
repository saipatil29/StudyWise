package com.example.studywise.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studywise.data.entity.Subject
import com.example.studywise.domain.StreakCalculator
import com.example.studywise.repository.StudyWiseRepository
import com.example.studywise.utils.DateTimeUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar

data class DayActivity(
    val dayName: String,
    val minutesStudied: Int,
    val isToday: Boolean
)

data class SubjectPrepItem(
    val subjectName: String,
    val preparationPercentage: Int,
    val difficulty: String
)

data class ProgressUiState(
    val totalStudyMinutes: Int = 0,
    val todayStudyMinutes: Int = 0,
    val weeklyStudyMinutes: Int = 0,
    val totalSessionsCount: Int = 0,
    val avgSessionDurationMinutes: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val avgQuizScorePercentage: Int = 0,
    val overallPreparationPercentage: Int = 0,
    val weekDaysActivity: List<DayActivity> = emptyList(),
    val subjectPreparations: List<SubjectPrepItem> = emptyList()
)

class ProgressViewModel(
    private val repository: StudyWiseRepository
) : ViewModel() {

    val uiState: StateFlow<ProgressUiState> = combine(
        repository.allSessions,
        repository.allSubjects,
        repository.allAttempts
    ) { sessions, subjects, attempts ->
        val now = System.currentTimeMillis()
        val completedSessions = sessions.filter { it.completed }

        // 1. Total minutes & sessions
        val totalMinutes = completedSessions.sumOf { it.durationMinutes }
        val totalSessions = completedSessions.size
        val avgDuration = if (totalSessions > 0) totalMinutes / totalSessions else 0

        // 2. Today's minutes
        val startOfToday = DateTimeUtils.getStartOfToday()
        val endOfToday = DateTimeUtils.getEndOfToday()
        val todayMinutes = completedSessions
            .filter { it.date in startOfToday..endOfToday }
            .sumOf { it.durationMinutes }

        // 3. Weekly minutes (last 7 days)
        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.DAY_OF_YEAR, -6)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOf7DaysAgo = cal.timeInMillis
        val weeklyMinutes = completedSessions
            .filter { it.date >= startOf7DaysAgo }
            .sumOf { it.durationMinutes }

        // 4. Streaks
        val streakResult = StreakCalculator.calculateStreak(completedSessions, now)

        // 5. Average Quiz Score
        val avgQuiz = if (attempts.isNotEmpty()) {
            attempts.map { it.percentage }.average().toInt()
        } else 0

        // 6. Overall Preparation
        val overallPrep = if (subjects.isNotEmpty()) {
            subjects.map { it.preparationPercentage }.average().toInt()
        } else 0

        // 7. 7-Day Activity Breakdown (Mon..Sun or last 7 days)
        val dayActivities = mutableListOf<DayActivity>()
        val dayCal = Calendar.getInstance()
        val todayDayOfYear = dayCal.get(Calendar.DAY_OF_YEAR)

        for (i in 6 downTo 0) {
            dayCal.timeInMillis = now
            dayCal.add(Calendar.DAY_OF_YEAR, -i)
            dayCal.set(Calendar.HOUR_OF_DAY, 0)
            dayCal.set(Calendar.MINUTE, 0)
            dayCal.set(Calendar.SECOND, 0)
            val dayStart = dayCal.timeInMillis
            dayCal.set(Calendar.HOUR_OF_DAY, 23)
            dayCal.set(Calendar.MINUTE, 59)
            val dayEnd = dayCal.timeInMillis

            val dayMinutes = completedSessions
                .filter { it.date in dayStart..dayEnd }
                .sumOf { it.durationMinutes }

            val dayName = DateTimeUtils.formatDayOfWeek(dayStart)
            val isToday = (dayCal.get(Calendar.DAY_OF_YEAR) == todayDayOfYear)

            dayActivities.add(DayActivity(dayName = dayName, minutesStudied = dayMinutes, isToday = isToday))
        }

        // 8. Subject Preparation list
        val subPreps = subjects.map {
            SubjectPrepItem(
                subjectName = it.name,
                preparationPercentage = it.preparationPercentage.coerceIn(0, 100),
                difficulty = it.difficulty
            )
        }.sortedByDescending { it.preparationPercentage }

        ProgressUiState(
            totalStudyMinutes = totalMinutes,
            todayStudyMinutes = todayMinutes,
            weeklyStudyMinutes = weeklyMinutes,
            totalSessionsCount = totalSessions,
            avgSessionDurationMinutes = avgDuration,
            currentStreak = streakResult.currentStreak,
            longestStreak = streakResult.longestStreak,
            avgQuizScorePercentage = avgQuiz,
            overallPreparationPercentage = overallPrep,
            weekDaysActivity = dayActivities,
            subjectPreparations = subPreps
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ProgressUiState()
    )
}
