package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.R
import com.example.model.AutomationSettings
import com.example.model.RidePlatform
import com.example.model.RideRequest
import com.example.model.RideStatus
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirestoreSyncService(private val context: Context) {

  private val TAG = "FirestoreSyncService"

  // CRITICAL: Always initialize with R.string.firestore_database_id
  val db: FirebaseFirestore by lazy {
    val dbId = context.getString(R.string.firestore_database_id)
    FirebaseFirestore.getInstance(dbId)
  }

  private val auth get() = Firebase.auth

  fun currentUserId(): String? = auth.currentUser?.uid

  suspend fun syncRideToCloud(ride: RideRequest) {
    val uid = currentUserId() ?: return
    try {
      val rideRef = db.collection("users")
        .document(uid)
        .collection("rides")
        .document(ride.id)

      val payload = hashMapOf<String, Any>(
        "id" to ride.id,
        "userId" to uid,
        "platformId" to ride.platform.id,
        "fare" to ride.fare,
        "pickupDistanceKm" to ride.pickupDistanceKm,
        "tripDistanceKm" to ride.tripDistanceKm,
        "pickupLocation" to ride.pickupLocation,
        "dropLocation" to ride.dropLocation,
        "estimatedMinutes" to ride.estimatedMinutes,
        "vehicleCategory" to ride.vehicleCategory,
        "timestamp" to ride.timestamp,
        "status" to ride.status.name,
        "decisionReason" to ride.decisionReason,
        "confidence" to ride.confidence,
        "isVerified" to ride.isVerified,
        "isSimulated" to ride.isSimulated,
        "updatedAt" to FieldValue.serverTimestamp()
      )

      rideRef.set(payload, SetOptions.merge()).await()
      Log.d(TAG, "Successfully synced ride ${ride.id} to Firestore")
    } catch (e: Exception) {
      Log.e(TAG, "Failed to sync ride ${ride.id} to Firestore", e)
    }
  }

  suspend fun syncSettingsToCloud(settings: AutomationSettings) {
    val uid = currentUserId() ?: return
    try {
      val settingsRef = db.collection("users")
        .document(uid)
        .collection("settings")
        .document("active_config")

      val payload = hashMapOf<String, Any>(
        "userId" to uid,
        "automationState" to settings.state.name,
        "activePreset" to settings.activePreset.name,
        "minFare" to settings.minFare,
        "maxFare" to settings.maxFare,
        "minFarePerKm" to settings.minFarePerKm,
        "maxPickupDistanceKm" to settings.maxPickupDistanceKm,
        "minTripDistanceKm" to settings.minTripDistanceKm,
        "maxTripDistanceKm" to settings.maxTripDistanceKm,
        "minConfidenceThreshold" to settings.minConfidenceThreshold,
        "autoAcceptEnabled" to settings.autoAcceptEnabled,
        "autoRejectEnabled" to settings.autoRejectEnabled,
        "manualReviewMargin" to settings.manualReviewMargin,
        "locationFilterEnabled" to settings.locationFilterEnabled,
        "voiceAnnouncementsEnabled" to settings.voiceAnnouncementsEnabled,
        "voiceLanguage" to settings.voiceLanguage,
        "emergencyStopActive" to settings.emergencyStopActive,
        "updatedAt" to FieldValue.serverTimestamp()
      )

      settingsRef.set(payload, SetOptions.merge()).await()
      Log.d(TAG, "Successfully synced automation settings to Firestore")
    } catch (e: Exception) {
      Log.e(TAG, "Failed to sync settings to Firestore", e)
    }
  }

  fun observeCloudRides(): Flow<List<RideRequest>> = flow {
    val uid = currentUserId()
    if (uid == null) {
      emit(emptyList())
      return@flow
    }

    db.collection("users")
      .document(uid)
      .collection("rides")
      .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
      .snapshots()
      .map { snapshot ->
        snapshot.documents.mapNotNull { doc ->
          try {
            val id = doc.getString("id") ?: doc.id
            val platformStr = doc.getString("platformId") ?: "other"
            val platform = RidePlatform.entries.firstOrNull { it.id.equals(platformStr, ignoreCase = true) } ?: RidePlatform.OTHER
            val fare = doc.getDouble("fare") ?: 0.0
            val pickupDist = doc.getDouble("pickupDistanceKm") ?: 0.0
            val tripDist = doc.getDouble("tripDistanceKm") ?: 0.0
            val pickupLoc = doc.getString("pickupLocation") ?: ""
            val dropLoc = doc.getString("dropLocation") ?: ""
            val estMin = doc.getLong("estimatedMinutes")?.toInt() ?: 15
            val category = doc.getString("vehicleCategory") ?: "Standard"
            val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
            val statusStr = doc.getString("status") ?: "PENDING"
            val status = try { RideStatus.valueOf(statusStr) } catch (_: Exception) { RideStatus.PENDING }
            val reason = doc.getString("decisionReason") ?: ""
            val confidence = doc.getDouble("confidence")?.toFloat() ?: 0.9f
            val isVerified = doc.getBoolean("isVerified") ?: false
            val isSimulated = doc.getBoolean("isSimulated") ?: false

            RideRequest(
              id = id,
              platform = platform,
              fare = fare,
              pickupDistanceKm = pickupDist,
              tripDistanceKm = tripDist,
              pickupLocation = pickupLoc,
              dropLocation = dropLoc,
              estimatedMinutes = estMin,
              vehicleCategory = category,
              timestamp = timestamp,
              status = status,
              decisionReason = reason,
              confidence = confidence,
              isVerified = isVerified,
              isSimulated = isSimulated
            )
          } catch (e: Exception) {
            Log.e(TAG, "Failed to parse cloud ride document", e)
            null
          }
        }
      }
      .catch { error ->
        Log.e(TAG, "Error in observeCloudRides listener", error)
        emit(emptyList())
      }
      .collect { emit(it) }
  }
}
