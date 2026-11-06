package com.example.model

enum class ZoneType(val displayName: String, val isExclusion: Boolean) {
  PREFERRED_PICKUP("Preferred Pickup Zone", false),
  PREFERRED_DROP("Preferred Destination", false),
  EXCLUDED_DROP("Avoid / Excluded Destination", true),
  EXCLUDED_PICKUP("Avoid / Excluded Pickup", true)
}

data class LocationZone(
  val id: String,
  val name: String,
  val zoneType: ZoneType,
  val addressKeywords: List<String>,
  val lat: Double = 0.0,
  val lng: Double = 0.0,
  val radiusKm: Double = 5.0,
  val polygonPoints: List<Pair<Double, Double>> = emptyList(),
  val isActive: Boolean = true,
  val colorHex: String = "#00E676"
) {
  fun matchesAddress(address: String): Boolean {
    val lower = address.lowercase()
    return addressKeywords.any { keyword -> lower.contains(keyword.lowercase()) }
  }

  fun distanceToKm(targetLat: Double, targetLng: Double): Double {
    if (lat == 0.0 && lng == 0.0) return Double.MAX_VALUE
    val earthRadiusKm = 6371.0
    val dLat = Math.toRadians(targetLat - lat)
    val dLon = Math.toRadians(targetLng - lng)
    val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
      Math.cos(Math.toRadians(lat)) * Math.cos(Math.toRadians(targetLat)) *
      Math.sin(dLon / 2) * Math.sin(dLon / 2)
    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    return earthRadiusKm * c
  }

  fun isPointInPolygon(pointLat: Double, pointLng: Double): Boolean {
    if (polygonPoints.size < 3) return false
    var inside = false
    var j = polygonPoints.size - 1
    for (i in polygonPoints.indices) {
      val xi = polygonPoints[i].first
      val yi = polygonPoints[i].second
      val xj = polygonPoints[j].first
      val yj = polygonPoints[j].second
      val intersect = ((yi > pointLng) != (yj > pointLng)) &&
        (pointLat < (xj - xi) * (pointLng - yi) / (yj - yi) + xi)
      if (intersect) inside = !inside
      j = i
    }
    return inside
  }

  fun isInGeofence(targetLat: Double, targetLng: Double): Boolean {
    if (polygonPoints.size >= 3) {
      return isPointInPolygon(targetLat, targetLng)
    }
    if (lat == 0.0 && lng == 0.0) return false
    return distanceToKm(targetLat, targetLng) <= radiusKm
  }

  companion object {
    fun defaultZones(): List<LocationZone> = listOf(
      LocationZone(
        id = "zone_delhi_airport",
        name = "IGI Airport (T3/T2/T1)",
        zoneType = ZoneType.PREFERRED_DROP,
        addressKeywords = listOf("Airport", "Terminal 3", "Terminal 2", "Terminal 1", "IGI", "Aerocity"),
        lat = 28.5562,
        lng = 77.1000,
        radiusKm = 6.0,
        colorHex = "#00E5FF"
      ),
      LocationZone(
        id = "zone_railway",
        name = "Central Railway Station",
        zoneType = ZoneType.PREFERRED_PICKUP,
        addressKeywords = listOf("Railway Station", "NDLS", "Old Delhi", "Hazrat Nizamuddin", "Anand Vihar"),
        lat = 28.6429,
        lng = 77.2195,
        radiusKm = 4.0,
        colorHex = "#00E676"
      ),
      LocationZone(
        id = "zone_home",
        name = "Home Zone (Dwarka / Janakpuri)",
        zoneType = ZoneType.PREFERRED_DROP,
        addressKeywords = listOf("Dwarka", "Janakpuri", "Uttam Nagar", "Palam"),
        lat = 28.5921,
        lng = 77.0460,
        radiusKm = 8.0,
        colorHex = "#FFB300"
      ),
      LocationZone(
        id = "zone_excluded_toll",
        name = "Heavy Toll / No-Go Border Zone",
        zoneType = ZoneType.EXCLUDED_DROP,
        addressKeywords = listOf("Badarpur Border", "Kundli Border", "Toll Plaza Outer"),
        lat = 28.4900,
        lng = 77.3000,
        radiusKm = 5.0,
        colorHex = "#FF5252"
      )
    )
  }
}
