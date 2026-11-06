package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.UserSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserSettingsDao {

  @Query("SELECT * FROM user_settings WHERE id = 1 LIMIT 1")
  fun getUserSettings(): Flow<UserSettingsEntity?>

  @Query("SELECT * FROM user_settings WHERE id = 1 LIMIT 1")
  suspend fun getUserSettingsDirect(): UserSettingsEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrUpdate(settings: UserSettingsEntity)

  @Query("UPDATE user_settings SET automationState = :state, updatedAt = :timestamp WHERE id = 1")
  suspend fun updateAutomationState(state: String, timestamp: Long = System.currentTimeMillis())

  @Query("UPDATE user_settings SET emergencyStopActive = :stopped, automationState = CASE WHEN :stopped THEN 'STOPPED' ELSE automationState END, updatedAt = :timestamp WHERE id = 1")
  suspend fun updateEmergencyStop(stopped: Boolean, timestamp: Long = System.currentTimeMillis())

  @Query("UPDATE user_settings SET activePreset = :preset, updatedAt = :timestamp WHERE id = 1")
  suspend fun updateActivePreset(preset: String, timestamp: Long = System.currentTimeMillis())
}
