package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.PlatformConfigurationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlatformConfigDao {

  @Query("SELECT * FROM platform_configurations")
  fun getAllPlatformConfigs(): Flow<List<PlatformConfigurationEntity>>

  @Query("SELECT * FROM platform_configurations WHERE platformId = :platformId LIMIT 1")
  fun getConfigForPlatform(platformId: String): Flow<PlatformConfigurationEntity?>

  @Query("SELECT * FROM platform_configurations WHERE platformId = :platformId LIMIT 1")
  suspend fun getConfigForPlatformDirect(platformId: String): PlatformConfigurationEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrUpdate(config: PlatformConfigurationEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(configs: List<PlatformConfigurationEntity>)

  @Query("UPDATE platform_configurations SET isEnabled = :isEnabled WHERE platformId = :platformId")
  suspend fun updatePlatformEnabled(platformId: String, isEnabled: Boolean)

  @Query("UPDATE platform_configurations SET totalAccepted = totalAccepted + :acceptedDelta, totalRejected = totalRejected + :rejectedDelta, lastDetectedTime = :lastDetectedTime WHERE platformId = :platformId")
  suspend fun recordPlatformActivity(platformId: String, acceptedDelta: Int, rejectedDelta: Int, lastDetectedTime: Long)
}
