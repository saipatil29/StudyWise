package com.example.studywise.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studywise.data.database.StudyWiseDatabase
import com.example.studywise.data.entity.Badge
import com.example.studywise.data.entity.UserPreferences
import com.example.studywise.domain.GamificationEngine
import com.example.studywise.domain.UserLevelInfo
import com.example.studywise.repository.StudyWiseRepository
import com.example.studywise.utils.DemoDataSeeder
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProfileUiState(
    val preferences: UserPreferences = UserPreferences(),
    val levelInfo: UserLevelInfo = GamificationEngine.calculateLevelInfo(0),
    val badges: List<Badge> = emptyList()
)

class ProfileViewModel(
    private val repository: StudyWiseRepository
) : ViewModel() {

    val uiState: StateFlow<ProfileUiState> = combine(
        repository.userPreferences,
        repository.allBadges
    ) { prefs, badges ->
        val safePrefs = prefs ?: UserPreferences()
        val levelInfo = GamificationEngine.calculateLevelInfo(safePrefs.totalXp)
        ProfileUiState(
            preferences = safePrefs,
            levelInfo = levelInfo,
            badges = badges
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProfileUiState())

    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            val current = repository.getUserPreferencesSync()
            repository.updateUserPreferences(current.copy(themeMode = mode))
        }
    }

    fun setDailyGoal(minutes: Int) {
        viewModelScope.launch {
            val current = repository.getUserPreferencesSync()
            repository.updateUserPreferences(current.copy(dailyGoalMinutes = minutes))
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val current = repository.getUserPreferencesSync()
            repository.updateUserPreferences(current.copy(notificationsEnabled = enabled))
        }
    }

    fun loadDemoData(database: StudyWiseDatabase) {
        viewModelScope.launch {
            DemoDataSeeder.seedDemoData(database)
            _toastEvent.emit("Realistic CS curriculum, topics, and quiz questions loaded!")
        }
    }

    fun resetAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            _toastEvent.emit("All data has been reset.")
        }
    }
}
