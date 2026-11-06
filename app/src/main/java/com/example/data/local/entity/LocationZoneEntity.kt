package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.LocationZone
import com.example.model.ZoneType

@Entity(tableName = "location_zones")
data class LocationZoneEntity(
  @PrimaryKey
  val id: String,
  val name: String,
  val zoneType: String, // PREFERRED_PICKUP, PREFERRED_DROP, EXCLUDED_DROP, EXCLUDED_PICKUP
  val addressKeywords: String, // Comma-separated address keywords
  val latitude: Double = 0.0,
  val longitude: Double = 0.0,
  val radiusKm: Double = 5.0,
  val polygonCoordinates: String = "", // "lat,lng;lat,lng;..."
  val isActive: Boolean = true,
  val colorHex: String = "#00E676",
  val createdAt: Long = System.currentTimeMillis()
) {
  fun toDomain(): LocationZone {
    val type = try {
      ZoneType.valueOf(zoneType.uppercase())
    } catch (_: Exception) {
      ZoneType.PREFERRED_DROP
    }

    val keywordsList = if (addressKeywords.isBlank()) {
      listOf(name)
    } else {
      addressKeywords.split(",").map { it.trim() }.filter { it.isNotBlank() }
    }

    val polygonPointsList = if (polygonCoordinates.isNotBlank()) {
      polygonCoordinates.split(";").mapNotNull { pairStr ->
        val parts = pairStr.split(",")
        if (parts.size == 2) {
          val pLat = parts[0].toDoubleOrNull()
          val pLng = parts[1].toDoubleOrNull()
          if (pLat != null && pLng != null) Pair(pLat, pLng) else null
        } else null
      }
    } else emptyList()

    return LocationZone(
      id = id,
      name = name,
      zoneType = type,
      addressKeywords = keywordsList,
      lat = latitude,
      lng = longitude,
      radiusKm = radiusKm,
      polygonPoints = polygonPointsList,
      isActive = isActive,
      colorHex = colorHex
    )
  }

  companion object {
    fun fromDomain(zone: LocationZone): LocationZoneEntity {
      return LocationZoneEntity(
        id = zone.id,
        name = zone.name,
        zoneType = zone.zoneType.name,
        addressKeywords = zone.addressKeywords.joinToString(","),
        latitude = zone.lat,
        longitude = zone.lng,
        radiusKm = zone.radiusKm,
        polygonCoordinates = zone.polygonPoints.joinToString(";") { "${it.first},${it.second}" },
        isActive = zone.isActive,
        colorHex = zone.colorHex
      )
    }

    fun defaultEntities(): List<LocationZoneEntity> {
      return LocationZone.defaultZones().map { fromDomain(it) }
    }
  }
}
