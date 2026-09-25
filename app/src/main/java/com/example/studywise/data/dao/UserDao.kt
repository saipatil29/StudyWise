package com.example.studywise.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.studywise.data.entity.Badge
import com.example.studywise.data.entity.UserPreferences
import kotlinx.coroutines.flow.Flow

@Dao
interface UserPreferencesDao {
    @Query("SELECT * FROM user_preferences WHERE id = 1 LIMIT 1")
    fun getUserPreferences(): Flow<UserPreferences?>

    @Query("SELECT * FROM user_preferences WHERE id = 1 LIMIT 1")
    suspend fun getUserPreferencesSync(): UserPreferences?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdatePreferences(preferences: UserPreferences)
}

@Dao
interface BadgeDao {
    @Query("SELECT * FROM badges ORDER BY id ASC")
    fun getAllBadges(): Flow<List<Badge>>

    @Query("SELECT * FROM badges ORDER BY id ASC")
    suspend fun getAllBadgesSync(): List<Badge>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBadges(badges: List<Badge>)

    @Update
    suspend fun updateBadge(badge: Badge)

    @Query("DELETE FROM badges")
    suspend fun clearAll()
}
