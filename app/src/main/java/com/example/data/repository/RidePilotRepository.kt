package com.example.data.repository

import com.example.data.local.RidePilotDatabase
import com.example.data.local.entity.LocationZoneEntity
import com.example.data.local.entity.PlatformConfigurationEntity
import com.example.data.local.entity.RideHistoryEntity
import com.example.data.local.entity.UserSettingsEntity
import com.example.model.AutomationSettings
import com.example.model.LocationZone
import com.example.model.PlatformConfig
import com.example.model.RidePlatform
import com.example.model.RideRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RidePilotRepository(private val database: RidePilotDatabase) {

  private val rideHistoryDao = database.rideHistoryDao()
  private val userSettingsDao = database.userSettingsDao()
  private val platformConfigDao = database.platformConfigDao()
  private val locationZoneDao = database.locationZoneDao()

  val allRides: Flow<List<RideRequest>> = rideHistoryDao.getAllRides()
    .map { list -> list.map { it.toDomain() } }

  val userSettings: Flow<AutomationSettings?> = userSettingsDao.getUserSettings()
    .map { it?.toDomain() }

  val platformConfigs: Flow<Map<RidePlatform, PlatformConfig>> = platformConfigDao.getAllPlatformConfigs()
    .map { list ->
      val map = mutableMapOf<RidePlatform, PlatformConfig>()
      list.forEach { entity ->
        val config = entity.toDomain()
        map[config.platform] = config
      }
      map
    }

  val allZones: Flow<List<LocationZone>> = locationZoneDao.getAllZones()
    .map { list -> list.map { it.toDomain() } }

  val activeZones: Flow<List<LocationZone>> = locationZoneDao.getActiveZones()
    .map { list -> list.map { it.toDomain() } }

  suspend fun insertRide(ride: RideRequest) {
    rideHistoryDao.insertRide(RideHistoryEntity.fromDomain(ride))
  }

  suspend fun insertRides(rides: List<RideRequest>) {
    rideHistoryDao.insertRides(rides.map { RideHistoryEntity.fromDomain(it) })
  }

  suspend fun deleteRide(rideId: String) {
    rideHistoryDao.deleteRideById(rideId)
  }

  suspend fun clearHistory() {
    rideHistoryDao.clearAllRides()
  }

  suspend fun saveUserSettings(settings: AutomationSettings) {
    userSettingsDao.insertOrUpdate(UserSettingsEntity.fromDomain(settings))
  }

  suspend fun updateAutomationState(stateName: String) {
    userSettingsDao.updateAutomationState(stateName)
  }

  suspend fun updateEmergencyStop(stopped: Boolean) {
    userSettingsDao.updateEmergencyStop(stopped)
  }

  suspend fun savePlatformConfig(config: PlatformConfig) {
    platformConfigDao.insertOrUpdate(PlatformConfigurationEntity.fromDomain(config))
  }

  suspend fun setPlatformEnabled(platform: RidePlatform, isEnabled: Boolean) {
    platformConfigDao.updatePlatformEnabled(platform.id, isEnabled)
  }

  suspend fun insertZone(zone: LocationZone) {
    locationZoneDao.insertZone(LocationZoneEntity.fromDomain(zone))
  }

  suspend fun insertAllZones(zones: List<LocationZone>) {
    locationZoneDao.insertAllZones(zones.map { LocationZoneEntity.fromDomain(it) })
  }

  suspend fun toggleZoneActive(zoneId: String, isActive: Boolean) {
    locationZoneDao.toggleZoneActive(zoneId, isActive)
  }

  suspend fun deleteZone(zoneId: String) {
    locationZoneDao.deleteZoneById(zoneId)
  }
}
