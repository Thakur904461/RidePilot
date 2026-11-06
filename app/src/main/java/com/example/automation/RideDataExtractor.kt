package com.example.automation

import com.example.model.RidePlatform
import com.example.model.RideRequest
import java.util.UUID
import java.util.regex.Pattern

object RideDataExtractor {

  private val FARE_PATTERN = Pattern.compile("(?:₹|rs\\.?|inr)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)", Pattern.CASE_INSENSITIVE)
  private val DISTANCE_PATTERN = Pattern.compile("([0-9]+(?:\\.[0-9]+)?)\\s*(?:km|kms)", Pattern.CASE_INSENSITIVE)
  private val TIME_PATTERN = Pattern.compile("([0-9]+)\\s*(?:min|mins|minutes)", Pattern.CASE_INSENSITIVE)

  /**
   * Identifies the originating ride application from package name or screen content.
   */
  fun identifyPlatform(packageName: String?, screenText: String): RidePlatform {
    if (packageName != null) {
      if (packageName.contains("olacabs")) return RidePlatform.OLA
      if (packageName.contains("ubercab")) return RidePlatform.UBER
      if (packageName.contains("rapido")) return RidePlatform.RAPIDO
      if (packageName.contains("porter")) return RidePlatform.PORTER
    }

    val lower = screenText.lowercase()
    return when {
      lower.contains("ola") -> RidePlatform.OLA
      lower.contains("uber") -> RidePlatform.UBER
      lower.contains("rapido") -> RidePlatform.RAPIDO
      lower.contains("porter") -> RidePlatform.PORTER
      else -> RidePlatform.OTHER
    }
  }

  /**
   * Extracts structured ride parameters from text fragments captured by the AccessibilityService.
   */
  fun extractFromTexts(
    texts: List<String>,
    platform: RidePlatform,
    rawScreenDump: String
  ): RideRequest? {
    val combinedText = texts.joinToString(" ") + " " + rawScreenDump

    // 1. Extract Fare
    var fare = 0.0
    val fareMatcher = FARE_PATTERN.matcher(combinedText)
    if (fareMatcher.find()) {
      val rawFare = fareMatcher.group(1)?.replace(",", "") ?: "0"
      fare = rawFare.toDoubleOrNull() ?: 0.0
    }

    // 2. Extract Distances (pickup vs trip)
    val distances = mutableListOf<Double>()
    val distMatcher = DISTANCE_PATTERN.matcher(combinedText)
    while (distMatcher.find()) {
      val dist = distMatcher.group(1)?.toDoubleOrNull()
      if (dist != null && dist > 0.0) {
        distances.add(dist)
      }
    }

    var pickupDistance = 1.8
    var tripDistance = 8.5

    if (distances.isNotEmpty()) {
      if (distances.size >= 2) {
        pickupDistance = distances.minOrNull() ?: 1.8
        tripDistance = distances.maxOrNull() ?: 8.5
      } else {
        tripDistance = distances[0]
      }
    }

    // 3. Extract Duration
    var minutes = 15
    val timeMatcher = TIME_PATTERN.matcher(combinedText)
    if (timeMatcher.find()) {
      minutes = timeMatcher.group(1)?.toIntOrNull() ?: 15
    }

    // 4. Extract locations
    var pickupLoc = "Current Driver Vicinity"
    var dropLoc = "City Center / Destination"

    for (line in texts) {
      val lower = line.lowercase()
      if (lower.contains("pickup:") || lower.contains("from:") || lower.contains("pick up")) {
        pickupLoc = line.replace(Regex("(?i)pickup:|from:|pick up"), "").trim()
      } else if (lower.contains("drop:") || lower.contains("to:") || lower.contains("destination")) {
        dropLoc = line.replace(Regex("(?i)drop:|to:|destination"), "").trim()
      }
    }

    // 5. Calculate Confidence
    var confidence = 0.5f
    if (fare > 0.0) confidence += 0.25f
    if (distances.isNotEmpty()) confidence += 0.15f
    if (dropLoc != "City Center / Destination") confidence += 0.10f

    if (fare <= 0.0 && distances.isEmpty()) {
      return null // Not enough data to formulate a ride request
    }

    val category = when (platform) {
      RidePlatform.OLA -> "Mini / Auto"
      RidePlatform.UBER -> "Uber Go / Moto"
      RidePlatform.RAPIDO -> "Bike Taxi"
      RidePlatform.PORTER -> "Goods 2-Wheeler"
      RidePlatform.OTHER -> "Delivery Ride"
    }

    return RideRequest(
      id = "ride_${UUID.randomUUID().toString().take(8)}",
      platform = platform,
      fare = if (fare > 0.0) fare else 180.0,
      pickupDistanceKm = pickupDistance,
      tripDistanceKm = tripDistance,
      pickupLocation = pickupLoc,
      dropLocation = dropLoc,
      estimatedMinutes = minutes,
      vehicleCategory = category,
      timestamp = System.currentTimeMillis(),
      rawText = combinedText.take(500),
      confidence = confidence.coerceIn(0.1f, 1.0f)
    )
  }
}
