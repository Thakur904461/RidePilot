package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.LocationZoneDao
import com.example.data.local.dao.PlatformConfigDao
import com.example.data.local.dao.RideHistoryDao
import com.example.data.local.dao.UserSettingsDao
import com.example.data.local.entity.LocationZoneEntity
import com.example.data.local.entity.PlatformConfigurationEntity
import com.example.data.local.entity.RideHistoryEntity
import com.example.data.local.entity.UserSettingsEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
  entities = [
    RideHistoryEntity::class,
    UserSettingsEntity::class,
    PlatformConfigurationEntity::class,
    LocationZoneEntity::class
  ],
  version = 3,
  exportSchema = false
)
abstract class RidePilotDatabase : RoomDatabase() {

  abstract fun rideHistoryDao(): RideHistoryDao
  abstract fun userSettingsDao(): UserSettingsDao
  abstract fun platformConfigDao(): PlatformConfigDao
  abstract fun locationZoneDao(): LocationZoneDao

  companion object {
    private const val DATABASE_NAME = "ridepilot.db"

    @Volatile
    private var INSTANCE: RidePilotDatabase? = null

    fun getInstance(context: Context): RidePilotDatabase {
      return INSTANCE ?: synchronized(this) {
        INSTANCE ?: buildDatabase(context.applicationContext).also { INSTANCE = it }
      }
    }

    private fun buildDatabase(appContext: Context): RidePilotDatabase {
      return Room.databaseBuilder(
        appContext,
        RidePilotDatabase::class.java,
        DATABASE_NAME
      )
        .fallbackToDestructiveMigration()
        .addCallback(object : Callback() {
          override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            // Prepopulate initial settings, platform integrations, and location zones asynchronously
            CoroutineScope(Dispatchers.IO).launch {
              val database = getInstance(appContext)
              database.userSettingsDao().insertOrUpdate(UserSettingsEntity())
              database.platformConfigDao().insertAll(PlatformConfigurationEntity.defaultEntities())
              database.locationZoneDao().insertAllZones(LocationZoneEntity.defaultEntities())
            }
          }
        })
        .build()
    }
  }
}
