package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.RideHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RideHistoryDao {

  @Query("SELECT * FROM ride_history ORDER BY timestamp DESC")
  fun getAllRides(): Flow<List<RideHistoryEntity>>

  @Query("SELECT * FROM ride_history WHERE status = :status ORDER BY timestamp DESC")
  fun getRidesByStatus(status: String): Flow<List<RideHistoryEntity>>

  @Query("SELECT * FROM ride_history WHERE platformId = :platformId ORDER BY timestamp DESC")
  fun getRidesByPlatform(platformId: String): Flow<List<RideHistoryEntity>>

  @Query("SELECT * FROM ride_history ORDER BY timestamp DESC LIMIT :limit")
  fun getRecentRides(limit: Int): Flow<List<RideHistoryEntity>>

  @Query("SELECT SUM(fare) FROM ride_history WHERE status = 'ACCEPTED' AND timestamp >= :startOfDayTimestamp")
  fun getTodayTotalEarnings(startOfDayTimestamp: Long): Flow<Double?>

  @Query("SELECT COUNT(*) FROM ride_history WHERE status = 'ACCEPTED' AND timestamp >= :startOfDayTimestamp")
  fun getTodayAcceptedCount(startOfDayTimestamp: Long): Flow<Int>

  @Query("SELECT COUNT(*) FROM ride_history WHERE status = 'REJECTED' AND timestamp >= :startOfDayTimestamp")
  fun getTodayRejectedCount(startOfDayTimestamp: Long): Flow<Int>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertRide(ride: RideHistoryEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertRides(rides: List<RideHistoryEntity>)

  @Update
  suspend fun updateRide(ride: RideHistoryEntity)

  @Query("DELETE FROM ride_history WHERE id = :id")
  suspend fun deleteRideById(id: String)

  @Query("DELETE FROM ride_history")
  suspend fun clearAllRides()
}
