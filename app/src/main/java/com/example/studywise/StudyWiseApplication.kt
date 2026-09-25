package com.example.studywise

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.example.studywise.data.database.StudyWiseDatabase
import com.example.studywise.domain.GamificationEngine
import com.example.studywise.repository.StudyWiseRepository
import com.example.studywise.utils.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class StudyWiseApplication : Application() {

    val database by lazy { StudyWiseDatabase.getDatabase(this) }
    val repository by lazy {
        StudyWiseRepository(
            database.subjectDao(),
            database.topicDao(),
            database.studySessionDao(),
            database.quizDao(),
            database.userPreferencesDao(),
            database.badgeDao()
        )
    }

    private val applicationScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannels(this)

        // Initialize default user preferences and badges if first run
        applicationScope.launch {
            val prefs = database.userPreferencesDao().getUserPreferencesSync()
            if (prefs == null) {
                database.userPreferencesDao().insertOrUpdatePreferences(
                    com.example.studywise.data.entity.UserPreferences()
                )
                database.badgeDao().insertBadges(GamificationEngine.getDefaultBadges())
            } else {
                // Apply saved theme mode
                when (prefs.themeMode) {
                    "LIGHT" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    "DARK" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                }
            }
        }
    }
}
