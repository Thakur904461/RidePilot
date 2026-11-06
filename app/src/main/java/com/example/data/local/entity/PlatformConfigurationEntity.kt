package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.PlatformConfig
import com.example.model.RidePlatform

@Entity(tableName = "platform_configurations")
data class PlatformConfigurationEntity(
  @PrimaryKey
  val platformId: String,
  val displayName: String,
  val packageName: String,
  val isEnabled: Boolean = true,
  val minFare: Double = 100.0,
  val minPerKm: Double = 15.0,
  val maxPickupKm: Double = 3.0,
  val activeCategories: String = "", // Comma-separated categories
  val isConnected: Boolean = true,
  val lastDetectedTime: Long? = null,
  val totalAccepted: Int = 0,
  val totalRejected: Int = 0
) {
  fun toDomain(): PlatformConfig {
    val platform = RidePlatform.entries.firstOrNull {
      it.id.equals(platformId, ignoreCase = true) || it.name.equals(platformId, ignoreCase = true)
    } ?: RidePlatform.OTHER

    val categories = if (activeCategories.isBlank()) {
      platform.defaultCategories
    } else {
      activeCategories.split(",").map { it.trim() }.filter { it.isNotBlank() }
    }

    return PlatformConfig(
      platform = platform,
      isEnabled = isEnabled,
      minFare = minFare,
      minPerKm = minPerKm,
      maxPickupKm = maxPickupKm,
      activeCategories = categories,
      isConnected = isConnected,
      lastDetectedTime = lastDetectedTime,
      totalAccepted = totalAccepted,
      totalRejected = totalRejected
    )
  }

  companion object {
    fun fromDomain(config: PlatformConfig): PlatformConfigurationEntity {
      return PlatformConfigurationEntity(
        platformId = config.platform.id,
        displayName = config.platform.displayName,
        packageName = config.platform.packageName,
        isEnabled = config.isEnabled,
        minFare = config.minFare,
        minPerKm = config.minPerKm,
        maxPickupKm = config.maxPickupKm,
        activeCategories = config.activeCategories.joinToString(","),
        isConnected = config.isConnected,
        lastDetectedTime = config.lastDetectedTime,
        totalAccepted = config.totalAccepted,
        totalRejected = config.totalRejected
      )
    }

    fun defaultEntities(): List<PlatformConfigurationEntity> {
      return listOf(
        PlatformConfigurationEntity(
          platformId = RidePlatform.OLA.id,
          displayName = RidePlatform.OLA.displayName,
          packageName = RidePlatform.OLA.packageName,
          minFare = 110.0,
          minPerKm = 16.0,
          maxPickupKm = 2.5,
          activeCategories = RidePlatform.OLA.defaultCategories.joinToString(",")
        ),
        PlatformConfigurationEntity(
          platformId = RidePlatform.UBER.id,
          displayName = RidePlatform.UBER.displayName,
          packageName = RidePlatform.UBER.packageName,
          minFare = 120.0,
          minPerKm = 17.0,
          maxPickupKm = 2.5,
          activeCategories = RidePlatform.UBER.defaultCategories.joinToString(",")
        ),
        PlatformConfigurationEntity(
          platformId = RidePlatform.RAPIDO.id,
          displayName = RidePlatform.RAPIDO.displayName,
          packageName = RidePlatform.RAPIDO.packageName,
          minFare = 60.0,
          minPerKm = 18.0,
          maxPickupKm = 1.8,
          activeCategories = RidePlatform.RAPIDO.defaultCategories.joinToString(",")
        ),
        PlatformConfigurationEntity(
          platformId = RidePlatform.PORTER.id,
          displayName = RidePlatform.PORTER.displayName,
          packageName = RidePlatform.PORTER.packageName,
          minFare = 200.0,
          minPerKm = 20.0,
          maxPickupKm = 3.0,
          activeCategories = RidePlatform.PORTER.defaultCategories.joinToString(",")
        )
      )
    }
  }
}
