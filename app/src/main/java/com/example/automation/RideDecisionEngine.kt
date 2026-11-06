package com.example.automation

import com.example.model.AutomationSettings
import com.example.model.AutomationState
import com.example.model.DecisionType
import com.example.model.LocationZone
import com.example.model.RideEvaluationResult
import com.example.model.RideRequest
import com.example.model.ZoneType

object RideDecisionEngine {

  /**
   * Evaluates an incoming ride request against driver's rules, active preset, and geofence zones.
   */
  fun evaluateRide(
    request: RideRequest,
    settings: AutomationSettings,
    zones: List<LocationZone>
  ): RideEvaluationResult {
    // 1. Emergency stop / Automation state check
    if (settings.emergencyStopActive || settings.state == AutomationState.STOPPED) {
      return RideEvaluationResult(
        decision = DecisionType.REJECT,
        reason = "Automation is stopped or emergency stop is engaged",
        score = 0.0f
      )
    }

    if (settings.state == AutomationState.PAUSED) {
      return RideEvaluationResult(
        decision = DecisionType.MANUAL_REVIEW,
        reason = "Automation paused by driver (Manual Mode active)",
        score = 0.5f
      )
    }

    // 2. Platform connection and enable check
    val platformConfig = settings.platformConfigs[request.platform]
    if (platformConfig != null && !platformConfig.isEnabled) {
      return RideEvaluationResult(
        decision = DecisionType.REJECT,
        reason = "${request.platform.displayName} is toggled OFF in settings",
        score = 0.0f
      )
    }

    // 3. Confidence threshold
    if (request.confidence < settings.minConfidenceThreshold) {
      return RideEvaluationResult(
        decision = DecisionType.UNKNOWN,
        reason = "Confidence ${(request.confidence * 100).toInt()}% is below threshold ${(settings.minConfidenceThreshold * 100).toInt()}%",
        score = request.confidence,
        warnings = listOf("Card information might be partially obscured or fast-fading")
      )
    }

    // 3b. Preferred Vehicle Type Filter Check
    if (settings.preferredVehicleCategories.isNotEmpty() && request.vehicleCategory.isNotBlank()) {
      val isVehicleMatched = settings.preferredVehicleCategories.any { pref ->
        request.vehicleCategory.contains(pref, ignoreCase = true) ||
        pref.contains(request.vehicleCategory, ignoreCase = true) ||
        (pref.contains("Bike", ignoreCase = true) && (request.vehicleCategory.contains("Moto", ignoreCase = true) || request.vehicleCategory.contains("Bike", ignoreCase = true))) ||
        (pref.contains("Auto", ignoreCase = true) && request.vehicleCategory.contains("Auto", ignoreCase = true)) ||
        (pref.contains("Cab", ignoreCase = true) && (request.vehicleCategory.contains("Mini", ignoreCase = true) || request.vehicleCategory.contains("Go", ignoreCase = true) || request.vehicleCategory.contains("Sedan", ignoreCase = true) || request.vehicleCategory.contains("Prime", ignoreCase = true))) ||
        (pref.contains("Delivery", ignoreCase = true) && (request.vehicleCategory.contains("Delivery", ignoreCase = true) || request.vehicleCategory.contains("Parcel", ignoreCase = true) || request.vehicleCategory.contains("Wheeler", ignoreCase = true) || request.vehicleCategory.contains("Ace", ignoreCase = true)))
      }
      if (!isVehicleMatched) {
        return RideEvaluationResult(
          decision = DecisionType.REJECT,
          reason = "Vehicle '${request.vehicleCategory}' not in driver's preferred vehicle types",
          score = 0.15f
        )
      }
    }

    // 4. Excluded Location Zone check (CRITICAL)
    if (settings.locationFilterEnabled) {
      val matchedExcludedDrop = zones.firstOrNull { zone ->
        zone.isActive && zone.zoneType == ZoneType.EXCLUDED_DROP && zone.matchesAddress(request.dropLocation)
      }
      if (matchedExcludedDrop != null) {
        return RideEvaluationResult(
          decision = DecisionType.REJECT,
          reason = "Destination matches excluded zone: ${matchedExcludedDrop.name}",
          score = 0.1f
        )
      }

      val matchedExcludedPickup = zones.firstOrNull { zone ->
        zone.isActive && zone.zoneType == ZoneType.EXCLUDED_PICKUP && zone.matchesAddress(request.pickupLocation)
      }
      if (matchedExcludedPickup != null) {
        return RideEvaluationResult(
          decision = DecisionType.REJECT,
          reason = "Pickup matches excluded zone: ${matchedExcludedPickup.name}",
          score = 0.1f
        )
      }
    }

    // 5. Pickup distance check
    if (request.pickupDistanceKm > settings.maxPickupDistanceKm) {
      return RideEvaluationResult(
        decision = DecisionType.REJECT,
        reason = "Pickup distance (${String.format("%.1f", request.pickupDistanceKm)} km) exceeds max limit (${String.format("%.1f", settings.maxPickupDistanceKm)} km)",
        score = 0.2f
      )
    }

    // 6. Minimum fare check
    val effectiveMinFare = platformConfig?.minFare ?: settings.minFare
    if (request.fare < effectiveMinFare) {
      // Check if it's within manual review margin
      val marginAmount = effectiveMinFare * (settings.manualReviewMargin / 100.0)
      if (request.fare >= (effectiveMinFare - marginAmount)) {
        return RideEvaluationResult(
          decision = DecisionType.MANUAL_REVIEW,
          reason = "Fare ₹${request.fare.toInt()} is slightly below min ₹${effectiveMinFare.toInt()} (Manual Review)",
          score = 0.45f
        )
      }
      return RideEvaluationResult(
        decision = DecisionType.REJECT,
        reason = "Fare ₹${request.fare.toInt()} is below minimum requirement ₹${effectiveMinFare.toInt()}",
        score = 0.25f
      )
    }

    // 7. Maximum fare check
    if (request.fare > settings.maxFare) {
      return RideEvaluationResult(
        decision = DecisionType.MANUAL_REVIEW,
        reason = "High fare ₹${request.fare.toInt()} exceeds standard safety cap ₹${settings.maxFare.toInt()} (Manual Check)",
        score = 0.6f
      )
    }

    // 8. Trip distance checks
    if (request.tripDistanceKm < settings.minTripDistanceKm) {
      return RideEvaluationResult(
        decision = DecisionType.REJECT,
        reason = "Trip distance (${String.format("%.1f", request.tripDistanceKm)} km) is below minimum (${String.format("%.1f", settings.minTripDistanceKm)} km)",
        score = 0.3f
      )
    }

    if (request.tripDistanceKm > settings.maxTripDistanceKm) {
      return RideEvaluationResult(
        decision = DecisionType.REJECT,
        reason = "Trip distance (${String.format("%.1f", request.tripDistanceKm)} km) exceeds maximum (${String.format("%.1f", settings.maxTripDistanceKm)} km)",
        score = 0.3f
      )
    }

    // 9. Earning per km check
    val effectiveMinPerKm = platformConfig?.minPerKm ?: settings.minFarePerKm
    val calculatedPerKm = request.farePerKm
    if (calculatedPerKm < effectiveMinPerKm) {
      return RideEvaluationResult(
        decision = DecisionType.REJECT,
        reason = "Earning ₹${String.format("%.1f", calculatedPerKm)}/km is below minimum target ₹${String.format("%.1f", effectiveMinPerKm)}/km",
        score = 0.35f
      )
    }

    // 10. Check if it matches preferred zones for bonus boost
    val isPreferredDrop = zones.any { zone ->
      zone.isActive && zone.zoneType == ZoneType.PREFERRED_DROP && zone.matchesAddress(request.dropLocation)
    }
    val isPreferredPickup = zones.any { zone ->
      zone.isActive && zone.zoneType == ZoneType.PREFERRED_PICKUP && zone.matchesAddress(request.pickupLocation)
    }

    val preferredBonus = if (isPreferredDrop || isPreferredPickup) " [Matches Preferred Zone!]" else ""

    // All mandatory criteria satisfied -> ACCEPT
    val successReason = "Matches all criteria: Fare ₹${request.fare.toInt()} (₹${String.format("%.1f", calculatedPerKm)}/km), Pickup ${String.format("%.1f", request.pickupDistanceKm)} km, Trip ${String.format("%.1f", request.tripDistanceKm)} km$preferredBonus"

    return RideEvaluationResult(
      decision = DecisionType.ACCEPT,
      reason = successReason,
      score = if (isPreferredDrop || isPreferredPickup) 0.98f else 0.88f
    )
  }
}
