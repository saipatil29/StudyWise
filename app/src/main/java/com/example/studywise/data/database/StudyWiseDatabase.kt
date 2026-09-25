package com.example.studywise.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
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

@Database(
    entities = [
        Subject::class,
        Topic::class,
        StudySession::class,
        Quiz::class,
        QuizQuestion::class,
        QuizAttempt::class,
        UserPreferences::class,
        Badge::class
    ],
    version = 1,
    exportSchema = false
)
abstract class StudyWiseDatabase : RoomDatabase() {

    abstract fun subjectDao(): SubjectDao
    abstract fun topicDao(): TopicDao
    abstract fun studySessionDao(): StudySessionDao
    abstract fun quizDao(): QuizDao
    abstract fun userPreferencesDao(): UserPreferencesDao
    abstract fun badgeDao(): BadgeDao

    companion object {
        @Volatile
        private var INSTANCE: StudyWiseDatabase? = null

        fun getDatabase(context: Context): StudyWiseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StudyWiseDatabase::class.java,
                    "studywise_database"
                )
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            db.execSQL("PRAGMA foreign_keys = ON;")
                        }
                    })
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
