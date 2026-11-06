package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.RidePlatform
import com.example.model.RideRequest
import com.example.model.RideStatus

@Entity(tableName = "ride_history")
data class RideHistoryEntity(
  @PrimaryKey
  val id: String,
  val platformId: String,
  val fare: Double,
  val pickupDistanceKm: Double,
  val tripDistanceKm: Double,
  val pickupLocation: String,
  val dropLocation: String,
  val estimatedMinutes: Int,
  val vehicleCategory: String,
  val timestamp: Long = System.currentTimeMillis(),
  val rawText: String = "",
  val status: String = "PENDING",
  val decisionReason: String = "",
  val confidence: Float = 0.95f,
  val isVerified: Boolean = false,
  val isSimulated: Boolean = false
) {
  fun toDomain(): RideRequest {
    val matchedPlatform = RidePlatform.entries.firstOrNull {
      it.id.equals(platformId, ignoreCase = true) || it.name.equals(platformId, ignoreCase = true)
    } ?: RidePlatform.OTHER

    val matchedStatus = try {
      RideStatus.valueOf(status.uppercase())
    } catch (_: Exception) {
      RideStatus.PENDING
    }

    return RideRequest(
      id = id,
      platform = matchedPlatform,
      fare = fare,
      pickupDistanceKm = pickupDistanceKm,
      tripDistanceKm = tripDistanceKm,
      pickupLocation = pickupLocation,
      dropLocation = dropLocation,
      estimatedMinutes = estimatedMinutes,
      vehicleCategory = vehicleCategory,
      timestamp = timestamp,
      rawText = rawText,
      status = matchedStatus,
      decisionReason = decisionReason,
      confidence = confidence,
      isVerified = isVerified,
      isSimulated = isSimulated
    )
  }

  companion object {
    fun fromDomain(request: RideRequest): RideHistoryEntity {
      return RideHistoryEntity(
        id = request.id,
        platformId = request.platform.id,
        fare = request.fare,
        pickupDistanceKm = request.pickupDistanceKm,
        tripDistanceKm = request.tripDistanceKm,
        pickupLocation = request.pickupLocation,
        dropLocation = request.dropLocation,
        estimatedMinutes = request.estimatedMinutes,
        vehicleCategory = request.vehicleCategory,
        timestamp = request.timestamp,
        rawText = request.rawText,
        status = request.status.name,
        decisionReason = request.decisionReason,
        confidence = request.confidence,
        isVerified = request.isVerified,
        isSimulated = request.isSimulated
      )
    }
  }
}
