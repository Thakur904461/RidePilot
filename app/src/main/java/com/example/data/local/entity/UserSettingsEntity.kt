package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.AutomationSettings
import com.example.model.AutomationState
import com.example.model.PresetMode

@Entity(tableName = "user_settings")
data class UserSettingsEntity(
  @PrimaryKey
  val id: Int = 1, // Single-row singleton configuration
  val automationState: String = "RUNNING",
  val activePreset: String = "HIGH_EARNING",
  val minFare: Double = 120.0,
  val maxFare: Double = 1200.0,
  val minFarePerKm: Double = 16.0,
  val maxPickupDistanceKm: Double = 2.5,
  val minTripDistanceKm: Double = 2.0,
  val maxTripDistanceKm: Double = 30.0,
  val minConfidenceThreshold: Float = 0.70f,
  val autoAcceptEnabled: Boolean = true,
  val autoRejectEnabled: Boolean = true,
  val manualReviewMargin: Double = 15.0,
  val locationFilterEnabled: Boolean = true,
  val voiceAnnouncementsEnabled: Boolean = true,
  val voiceLanguage: String = "hi",
  val emergencyStopActive: Boolean = false,
  val verifyStateInApp: Boolean = true,
  val autoAcceptDelaySeconds: Float = 1.2f,
  val humanizeJitterEnabled: Boolean = true,
  val showReactionCountdownOverlay: Boolean = true,
  val preferredVehicleTypes: String = "Bike Taxi,Auto Rickshaw,Economy Cab,Sedan Premier,Parcel Delivery,Mini Truck / Tata Ace",
  val updatedAt: Long = System.currentTimeMillis()
) {
  fun toDomain(): AutomationSettings {
    val state = try {
      AutomationState.valueOf(automationState.uppercase())
    } catch (_: Exception) {
      AutomationState.RUNNING
    }

    val preset = try {
      PresetMode.valueOf(activePreset.uppercase())
    } catch (_: Exception) {
      PresetMode.HIGH_EARNING
    }

    val vehicleCategories = if (preferredVehicleTypes.isBlank()) {
      listOf("Bike Taxi", "Auto Rickshaw", "Economy Cab", "Sedan Premier", "Parcel Delivery", "Mini Truck / Tata Ace")
    } else {
      preferredVehicleTypes.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    return AutomationSettings(
      state = state,
      activePreset = preset,
      minFare = minFare,
      maxFare = maxFare,
      minFarePerKm = minFarePerKm,
      maxPickupDistanceKm = maxPickupDistanceKm,
      minTripDistanceKm = minTripDistanceKm,
      maxTripDistanceKm = maxTripDistanceKm,
      minConfidenceThreshold = minConfidenceThreshold,
      autoAcceptEnabled = autoAcceptEnabled,
      autoRejectEnabled = autoRejectEnabled,
      manualReviewMargin = manualReviewMargin,
      locationFilterEnabled = locationFilterEnabled,
      voiceAnnouncementsEnabled = voiceAnnouncementsEnabled,
      voiceLanguage = voiceLanguage,
      emergencyStopActive = emergencyStopActive,
      verifyStateInApp = verifyStateInApp,
      autoAcceptDelaySeconds = autoAcceptDelaySeconds,
      humanizeJitterEnabled = humanizeJitterEnabled,
      showReactionCountdownOverlay = showReactionCountdownOverlay,
      preferredVehicleCategories = vehicleCategories
    )
  }

  companion object {
    fun fromDomain(settings: AutomationSettings): UserSettingsEntity {
      return UserSettingsEntity(
        id = 1,
        automationState = settings.state.name,
        activePreset = settings.activePreset.name,
        minFare = settings.minFare,
        maxFare = settings.maxFare,
        minFarePerKm = settings.minFarePerKm,
        maxPickupDistanceKm = settings.maxPickupDistanceKm,
        minTripDistanceKm = settings.minTripDistanceKm,
        maxTripDistanceKm = settings.maxTripDistanceKm,
        minConfidenceThreshold = settings.minConfidenceThreshold,
        autoAcceptEnabled = settings.autoAcceptEnabled,
        autoRejectEnabled = settings.autoRejectEnabled,
        manualReviewMargin = settings.manualReviewMargin,
        locationFilterEnabled = settings.locationFilterEnabled,
        voiceAnnouncementsEnabled = settings.voiceAnnouncementsEnabled,
        voiceLanguage = settings.voiceLanguage,
        emergencyStopActive = settings.emergencyStopActive,
        verifyStateInApp = settings.verifyStateInApp,
        autoAcceptDelaySeconds = settings.autoAcceptDelaySeconds,
        humanizeJitterEnabled = settings.humanizeJitterEnabled,
        showReactionCountdownOverlay = settings.showReactionCountdownOverlay,
        preferredVehicleTypes = settings.preferredVehicleCategories.joinToString(","),
        updatedAt = System.currentTimeMillis()
      )
    }
  }
}
