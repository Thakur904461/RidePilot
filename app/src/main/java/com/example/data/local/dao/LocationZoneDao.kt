package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.LocationZoneEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationZoneDao {

  @Query("SELECT * FROM location_zones ORDER BY createdAt DESC")
  fun getAllZones(): Flow<List<LocationZoneEntity>>

  @Query("SELECT * FROM location_zones WHERE isActive = 1 ORDER BY createdAt DESC")
  fun getActiveZones(): Flow<List<LocationZoneEntity>>

  @Query("SELECT * FROM location_zones WHERE zoneType = :zoneType ORDER BY createdAt DESC")
  fun getZonesByType(zoneType: String): Flow<List<LocationZoneEntity>>

  @Query("SELECT * FROM location_zones WHERE id = :id LIMIT 1")
  fun getZoneById(id: String): Flow<LocationZoneEntity?>

  @Query("SELECT * FROM location_zones WHERE id = :id LIMIT 1")
  suspend fun getZoneByIdDirect(id: String): LocationZoneEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertZone(zone: LocationZoneEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAllZones(zones: List<LocationZoneEntity>)

  @Update
  suspend fun updateZone(zone: LocationZoneEntity)

  @Query("UPDATE location_zones SET isActive = :isActive WHERE id = :id")
  suspend fun toggleZoneActive(id: String, isActive: Boolean)

  @Query("DELETE FROM location_zones WHERE id = :id")
  suspend fun deleteZoneById(id: String)

  @Query("DELETE FROM location_zones")
  suspend fun clearAllZones()
}
