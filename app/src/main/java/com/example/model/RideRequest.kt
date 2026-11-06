package com.example.model

enum class RideStatus {
  PENDING,
  ACCEPTED,
  REJECTED,
  MANUAL_REVIEW,
  EXPIRED,
  MISSED
}

enum class DecisionType {
  ACCEPT,
  REJECT,
  MANUAL_REVIEW,
  UNKNOWN
}

data class RideRequest(
  val id: String,
  val platform: RidePlatform,
  val fare: Double, // in INR
  val pickupDistanceKm: Double,
  val tripDistanceKm: Double,
  val pickupLocation: String,
  val dropLocation: String,
  val estimatedMinutes: Int,
  val vehicleCategory: String,
  val timestamp: Long = System.currentTimeMillis(),
  val rawText: String = "",
  val status: RideStatus = RideStatus.PENDING,
  val decisionReason: String = "",
  val confidence: Float = 0.95f,
  val isVerified: Boolean = false,
  val isSimulated: Boolean = false
) {
  val farePerKm: Double
    get() = if (tripDistanceKm > 0.1) fare / tripDistanceKm else 0.0

  val totalDistanceKm: Double
    get() = pickupDistanceKm + tripDistanceKm

  val estimatedGrossPerMinute: Double
    get() = if (estimatedMinutes > 0) fare / estimatedMinutes else 0.0
}

data class RideEvaluationResult(
  val decision: DecisionType,
  val reason: String,
  val score: Float,
  val warnings: List<String> = emptyList()
)
