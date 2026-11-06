package com.example.model

enum class AutomationState {
  RUNNING, // Green: auto-evaluating and auto-accepting
  PAUSED,  // Yellow: manual review / paused
  STOPPED  // Red: automation disabled
}

enum class PresetMode(val displayName: String, val description: String) {
  HIGH_EARNING(
    displayName = "High Earning",
    description = "Prioritize high-value rides above ₹250 with ≥₹20/km fare"
  ),
  SHORT_DISTANCE(
    displayName = "Short Distance",
    description = "Quick turnaround trips (<8 km) with low pickup wait"
  ),
  LONG_DISTANCE(
    displayName = "Long Distance",
    description = "Long inter-city or airport highway routes (≥15 km, ≥₹400)"
  ),
  FUEL_SAVING(
    displayName = "Fuel Saving",
    description = "Minimal pickup deadheading (<1.5 km) with optimal ₹/km"
  ),
  CUSTOM(
    displayName = "Custom Rules",
    description = "Fully personalized fare, distance, and zone parameters"
  )
}

data class AutomationSettings(
  val state: AutomationState = AutomationState.RUNNING,
  val activePreset: PresetMode = PresetMode.HIGH_EARNING,
  val minFare: Double = 120.0,
  val maxFare: Double = 1200.0,
  val minFarePerKm: Double = 16.0,
  val maxPickupDistanceKm: Double = 2.5,
  val minTripDistanceKm: Double = 2.0,
  val maxTripDistanceKm: Double = 30.0,
  val minConfidenceThreshold: Float = 0.70f,
  val autoAcceptEnabled: Boolean = true,
  val autoRejectEnabled: Boolean = true,
  val manualReviewMargin: Double = 15.0, // percent
  val locationFilterEnabled: Boolean = true,
  val voiceAnnouncementsEnabled: Boolean = true,
  val voiceLanguage: String = "hi", // "hi" for Hinglish/Hindi, "en" for English
  val emergencyStopActive: Boolean = false,
  val verifyStateInApp: Boolean = true,
  val autoAcceptDelaySeconds: Float = 1.2f, // Customizable humanized delay (0.3s to 5.0s)
  val humanizeJitterEnabled: Boolean = true, // Adds ±0.2s natural human variance to avoid bot detection
  val showReactionCountdownOverlay: Boolean = true, // Shows countdown overlay before tap
  val preferredVehicleCategories: List<String> = listOf(
    "Bike Taxi",
    "Auto Rickshaw",
    "Economy Cab",
    "Sedan Premier",
    "Parcel Delivery",
    "Mini Truck / Tata Ace"
  ),
  val platformConfigs: Map<RidePlatform, PlatformConfig> = RidePlatform.entries.associateWith {
    PlatformConfig(platform = it)
  }
) {
  companion object {
    fun fromPreset(preset: PresetMode): AutomationSettings {
      return when (preset) {
        PresetMode.HIGH_EARNING -> AutomationSettings(
          activePreset = PresetMode.HIGH_EARNING,
          minFare = 250.0,
          maxFare = 2500.0,
          minFarePerKm = 22.0,
          maxPickupDistanceKm = 2.5,
          minTripDistanceKm = 5.0,
          maxTripDistanceKm = 40.0
        )
        PresetMode.SHORT_DISTANCE -> AutomationSettings(
          activePreset = PresetMode.SHORT_DISTANCE,
          minFare = 80.0,
          maxFare = 250.0,
          minFarePerKm = 20.0,
          maxPickupDistanceKm = 1.5,
          minTripDistanceKm = 1.0,
          maxTripDistanceKm = 8.0
        )
        PresetMode.LONG_DISTANCE -> AutomationSettings(
          activePreset = PresetMode.LONG_DISTANCE,
          minFare = 400.0,
          maxFare = 4000.0,
          minFarePerKm = 17.0,
          maxPickupDistanceKm = 3.5,
          minTripDistanceKm = 15.0,
          maxTripDistanceKm = 70.0
        )
        PresetMode.FUEL_SAVING -> AutomationSettings(
          activePreset = PresetMode.FUEL_SAVING,
          minFare = 100.0,
          maxFare = 800.0,
          minFarePerKm = 19.0,
          maxPickupDistanceKm = 1.0,
          minTripDistanceKm = 2.0,
          maxTripDistanceKm = 12.0
        )
        PresetMode.CUSTOM -> AutomationSettings(
          activePreset = PresetMode.CUSTOM,
          minFare = 120.0,
          maxFare = 1000.0,
          minFarePerKm = 15.0,
          maxPickupDistanceKm = 3.0,
          minTripDistanceKm = 2.0,
          maxTripDistanceKm = 25.0
        )
      }
    }
  }
}
