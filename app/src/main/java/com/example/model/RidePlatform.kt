package com.example.model

import androidx.compose.ui.graphics.Color

enum class RidePlatform(
  val id: String,
  val displayName: String,
  val packageName: String,
  val brandColor: Color,
  val defaultCategories: List<String>
) {
  OLA(
    id = "ola",
    displayName = "Ola Driver",
    packageName = "com.olacabs.driver",
    brandColor = Color(0xFFC6D92C),
    defaultCategories = listOf("Bike", "Auto", "Mini", "Sedan", "Prime")
  ),
  UBER(
    id = "uber",
    displayName = "Uber Driver",
    packageName = "com.ubercab.driver",
    brandColor = Color(0xFF000000),
    defaultCategories = listOf("Uber Moto", "Uber Auto", "Uber Go", "Premier", "XL")
  ),
  RAPIDO(
    id = "rapido",
    displayName = "Rapido Captain",
    packageName = "com.rapido.captain",
    brandColor = Color(0xFFFFC72C),
    defaultCategories = listOf("Bike Taxi", "Auto", "Cab Economy")
  ),
  PORTER(
    id = "porter",
    displayName = "Porter Partner",
    packageName = "com.theporter.partner",
    brandColor = Color(0xFF0052CC),
    defaultCategories = listOf("2 Wheeler", "3 Wheeler", "Tata Ace", "Pickup 8ft")
  ),
  OTHER(
    id = "other",
    displayName = "Custom Platform",
    packageName = "custom.delivery.app",
    brandColor = Color(0xFF7C4DFF),
    defaultCategories = listOf("Standard Ride", "Delivery Parcel")
  )
}

data class PlatformConfig(
  val platform: RidePlatform,
  val isEnabled: Boolean = true,
  val minFare: Double = 100.0,
  val minPerKm: Double = 15.0,
  val maxPickupKm: Double = 3.0,
  val activeCategories: List<String> = platform.defaultCategories,
  val isConnected: Boolean = true,
  val lastDetectedTime: Long? = null,
  val totalAccepted: Int = 0,
  val totalRejected: Int = 0
)
