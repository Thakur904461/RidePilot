package com.example.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class GpsLocationData(
  val latitude: Double = 28.5562, // Default Delhi NCR
  val longitude: Double = 77.1000,
  val accuracyMeters: Float = 15.0f,
  val timestamp: Long = System.currentTimeMillis(),
  val isRealGpsFix: Boolean = false,
  val localityName: String = "Delhi NCR"
)

class GpsLocationProvider private constructor(private val context: Context) {

  private val fusedLocationClient: FusedLocationProviderClient =
    LocationServices.getFusedLocationProviderClient(context)

  private val _currentLocation = MutableStateFlow(GpsLocationData())
  val currentLocation: StateFlow<GpsLocationData> = _currentLocation.asStateFlow()

  fun hasLocationPermission(): Boolean {
    val fineGranted = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val coarseGranted = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    return fineGranted || coarseGranted
  }

  fun isGpsEnabled(): Boolean {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    return locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true ||
      locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true
  }

  @SuppressLint("MissingPermission")
  suspend fun fetchCurrentLocation(): GpsLocationData {
    if (!hasLocationPermission()) {
      return _currentLocation.value.copy(isRealGpsFix = false)
    }

    return try {
      val cancellationTokenSource = CancellationTokenSource()
      val location: Location? = suspendCancellableCoroutine { continuation ->
        fusedLocationClient.getCurrentLocation(
          Priority.PRIORITY_HIGH_ACCURACY,
          cancellationTokenSource.token
        ).addOnSuccessListener { loc ->
          if (continuation.isActive) {
            continuation.resume(loc)
          }
        }.addOnFailureListener {
          if (continuation.isActive) {
            continuation.resume(null)
          }
        }

        continuation.invokeOnCancellation {
          cancellationTokenSource.cancel()
        }
      }

      if (location != null) {
        val locality = deriveLocality(location.latitude, location.longitude)
        val data = GpsLocationData(
          latitude = location.latitude,
          longitude = location.longitude,
          accuracyMeters = location.accuracy,
          timestamp = location.time,
          isRealGpsFix = true,
          localityName = locality
        )
        _currentLocation.value = data
        data
      } else {
        // Fallback to last known location if getCurrentLocation returned null (e.g. timeout on emulator)
        getLastKnownLocation()
      }
    } catch (e: Exception) {
      _currentLocation.value.copy(isRealGpsFix = false)
    }
  }

  @SuppressLint("MissingPermission")
  private suspend fun getLastKnownLocation(): GpsLocationData {
    if (!hasLocationPermission()) return _currentLocation.value

    return suspendCancellableCoroutine { continuation ->
      fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
        if (loc != null) {
          val data = GpsLocationData(
            latitude = loc.latitude,
            longitude = loc.longitude,
            accuracyMeters = loc.accuracy,
            timestamp = loc.time,
            isRealGpsFix = true,
            localityName = deriveLocality(loc.latitude, loc.longitude)
          )
          _currentLocation.value = data
          if (continuation.isActive) continuation.resume(data)
        } else {
          if (continuation.isActive) continuation.resume(_currentLocation.value)
        }
      }.addOnFailureListener {
        if (continuation.isActive) continuation.resume(_currentLocation.value)
      }
    }
  }

  /**
   * Fast offline reverse lookup for common major Indian metro zones (avoids network blocking geocoders)
   */
  fun deriveLocality(lat: Double, lng: Double): String {
    // Delhi NCR zones
    if (lat in 28.4..28.8 && lng in 76.9..77.4) {
      return when {
        lat in 28.53..28.58 && lng in 77.07..77.12 -> "IGI Airport / Aerocity, Delhi"
        lat in 28.62..28.66 && lng in 77.20..77.24 -> "Connaught Place / NDLS, Delhi"
        lat in 28.43..28.51 && lng in 77.01..77.10 -> "Cyber City / DLF, Gurugram"
        lat in 28.54..28.62 && lng in 77.32..77.40 -> "Sector 18 / Expressway, Noida"
        else -> "Delhi NCR, India"
      }
    }
    // Bengaluru zones
    if (lat in 12.8..13.2 && lng in 77.4..77.8) {
      return when {
        lat in 13.18..13.22 && lng in 77.68..77.72 -> "Kempegowda Airport, Bengaluru"
        lat in 12.92..12.98 && lng in 77.60..77.66 -> "Koramangala / Indiranagar, Bengaluru"
        else -> "Bengaluru Urban, Karnataka"
      }
    }
    // Mumbai zones
    if (lat in 18.9..19.3 && lng in 72.7..73.1) {
      return when {
        lat in 19.08..19.11 && lng in 72.86..72.89 -> "Chhatrapati Shivaji Maharaj Airport, Mumbai"
        lat in 19.05..19.08 && lng in 72.82..72.85 -> "Bandra Kurla Complex (BKC), Mumbai"
        else -> "Mumbai Metropolitan Region"
      }
    }
    return String.format(java.util.Locale.US, "GPS: %.4f° N, %.4f° E", lat, lng)
  }

  companion object {
    @Volatile
    private var INSTANCE: GpsLocationProvider? = null

    fun getInstance(context: Context): GpsLocationProvider {
      return INSTANCE ?: synchronized(this) {
        INSTANCE ?: GpsLocationProvider(context.applicationContext).also { INSTANCE = it }
      }
    }
  }
}
