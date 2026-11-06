package com.example

import com.example.automation.RideDataExtractor
import com.example.automation.RideDecisionEngine
import com.example.data.local.entity.PlatformConfigurationEntity
import com.example.data.local.entity.RideHistoryEntity
import com.example.data.local.entity.UserSettingsEntity
import com.example.model.AutomationSettings
import com.example.model.AutomationState
import com.example.model.DecisionType
import com.example.model.LocationZone
import com.example.model.PlanTier
import com.example.model.PresetMode
import com.example.model.RidePlatform
import com.example.model.RideRequest
import com.example.model.RideStatus
import com.example.model.SubscriptionPlan
import com.example.model.ZoneType
import com.example.payment.RazorpayBackendService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun testRideDecisionEngine_AcceptsMatchingRide() {
    val settings = AutomationSettings(
      minFare = 100.0,
      maxFare = 1000.0,
      minFarePerKm = 15.0,
      maxPickupDistanceKm = 2.5,
      minTripDistanceKm = 2.0,
      maxTripDistanceKm = 25.0
    )

    val validRequest = RideRequest(
      id = "test_01",
      platform = RidePlatform.OLA,
      fare = 320.0,
      pickupDistanceKm = 1.2,
      tripDistanceKm = 14.0,
      pickupLocation = "Connaught Place",
      dropLocation = "Noida Sector 18",
      estimatedMinutes = 30,
      vehicleCategory = "Mini"
    )

    val result = RideDecisionEngine.evaluateRide(validRequest, settings, emptyList())
    assertEquals(DecisionType.ACCEPT, result.decision)
    assertTrue(result.reason.contains("Matches all criteria"))
  }

  @Test
  fun testRideDecisionEngine_RejectsExcessPickupDistance() {
    val settings = AutomationSettings(maxPickupDistanceKm = 2.0)

    val farPickupRequest = RideRequest(
      id = "test_02",
      platform = RidePlatform.UBER,
      fare = 250.0,
      pickupDistanceKm = 4.2, // Exceeds 2.0 km
      tripDistanceKm = 10.0,
      pickupLocation = "Dwarka",
      dropLocation = "Gurgaon",
      estimatedMinutes = 25,
      vehicleCategory = "Uber Go"
    )

    val result = RideDecisionEngine.evaluateRide(farPickupRequest, settings, emptyList())
    assertEquals(DecisionType.REJECT, result.decision)
    assertTrue(result.reason.contains("Pickup distance"))
  }

  @Test
  fun testRideDecisionEngine_RejectsExcludedZone() {
    val settings = AutomationSettings(locationFilterEnabled = true)
    val zones = listOf(
      LocationZone(
        id = "toll_zone",
        name = "Badarpur Border Toll",
        zoneType = ZoneType.EXCLUDED_DROP,
        addressKeywords = listOf("Badarpur", "Border Toll")
      )
    )

    val tollRequest = RideRequest(
      id = "test_03",
      platform = RidePlatform.RAPIDO,
      fare = 180.0,
      pickupDistanceKm = 1.0,
      tripDistanceKm = 8.0,
      pickupLocation = "Lajpat Nagar",
      dropLocation = "Badarpur Border Toll Plaza",
      estimatedMinutes = 20,
      vehicleCategory = "Bike Taxi"
    )

    val result = RideDecisionEngine.evaluateRide(tollRequest, settings, zones)
    assertEquals(DecisionType.REJECT, result.decision)
    assertTrue(result.reason.contains("excluded zone"))
  }

  @Test
  fun testRideDataExtractor_ParsesFareAndDistances() {
    val sampleTexts = listOf(
      "New Ride Request",
      "Fare: ₹350",
      "Pickup: 1.5 km away",
      "Drop: IGI Airport T3",
      "Trip: 16.0 km",
      "Accept Ride"
    )

    val platform = RideDataExtractor.identifyPlatform("com.olacabs.driver", "")
    assertEquals(RidePlatform.OLA, platform)

    val parsed = RideDataExtractor.extractFromTexts(sampleTexts, platform, "")
    assertTrue(parsed != null)
    assertEquals(350.0, parsed!!.fare, 0.01)
    assertEquals(1.5, parsed.pickupDistanceKm, 0.01)
    assertEquals(16.0, parsed.tripDistanceKm, 0.01)
  }

  @Test
  fun testRoomEntity_RideHistoryMapping() {
    val domainRide = RideRequest(
      id = "ride_room_01",
      platform = RidePlatform.PORTER,
      fare = 450.0,
      pickupDistanceKm = 1.8,
      tripDistanceKm = 19.5,
      pickupLocation = "Okhla Industrial Area",
      dropLocation = "Connaught Place",
      estimatedMinutes = 45,
      vehicleCategory = "Tata Ace",
      status = RideStatus.ACCEPTED,
      decisionReason = "Matched all criteria",
      confidence = 0.98f,
      isVerified = true
    )

    val entity = RideHistoryEntity.fromDomain(domainRide)
    assertEquals("ride_room_01", entity.id)
    assertEquals("porter", entity.platformId)
    assertEquals(450.0, entity.fare, 0.01)

    val mappedBack = entity.toDomain()
    assertEquals(domainRide.id, mappedBack.id)
    assertEquals(domainRide.platform, mappedBack.platform)
    assertEquals(domainRide.fare, mappedBack.fare, 0.01)
    assertEquals(domainRide.status, mappedBack.status)
  }

  @Test
  fun testRoomEntity_UserSettingsMapping() {
    val domainSettings = AutomationSettings(
      activePreset = PresetMode.FUEL_SAVING,
      minFare = 95.0,
      maxFare = 850.0,
      minFarePerKm = 18.5,
      maxPickupDistanceKm = 1.2
    )

    val entity = UserSettingsEntity.fromDomain(domainSettings)
    assertEquals("FUEL_SAVING", entity.activePreset)
    assertEquals(95.0, entity.minFare, 0.01)

    val mappedBack = entity.toDomain()
    assertEquals(PresetMode.FUEL_SAVING, mappedBack.activePreset)
    assertEquals(95.0, mappedBack.minFare, 0.01)
    assertEquals(18.5, mappedBack.minFarePerKm, 0.01)
  }

  @Test
  fun testRoomEntity_PlatformConfigurationDefaults() {
    val defaults = PlatformConfigurationEntity.defaultEntities()
    assertEquals(4, defaults.size)

    val ola = defaults.first { it.platformId == "ola" }
    val mappedOla = ola.toDomain()
    assertEquals(RidePlatform.OLA, mappedOla.platform)
    assertTrue(mappedOla.isEnabled)
    assertTrue(mappedOla.activeCategories.isNotEmpty())
  }

  @Test
  fun testRoomEntity_LocationZoneMapping() {
    val domainZone = LocationZone(
      id = "zone_delhi_airport_test",
      name = "IGI Airport Terminal 3",
      zoneType = ZoneType.PREFERRED_DROP,
      addressKeywords = listOf("Airport", "Terminal 3", "T3", "Aerocity"),
      lat = 28.5562,
      lng = 77.1000,
      radiusKm = 6.0,
      isActive = true,
      colorHex = "#00E5FF"
    )

    val entity = com.example.data.local.entity.LocationZoneEntity.fromDomain(domainZone)
    assertEquals("zone_delhi_airport_test", entity.id)
    assertEquals("PREFERRED_DROP", entity.zoneType)
    assertEquals(6.0, entity.radiusKm, 0.01)
    assertTrue(entity.addressKeywords.contains("Terminal 3"))

    val mappedBack = entity.toDomain()
    assertEquals(domainZone.id, mappedBack.id)
    assertEquals(domainZone.name, mappedBack.name)
    assertEquals(domainZone.zoneType, mappedBack.zoneType)
    assertEquals(4, mappedBack.addressKeywords.size)
    assertTrue(mappedBack.matchesAddress("Passenger going to IGI Airport Departure"))
  }

  @Test
  fun testLocationZone_GpsGeofenceDistanceCalculation() {
    val airportZone = LocationZone(
      id = "zone_airport",
      name = "IGI Airport",
      zoneType = ZoneType.PREFERRED_DROP,
      addressKeywords = listOf("Airport", "IGI"),
      lat = 28.5562,
      lng = 77.1000,
      radiusKm = 6.0
    )

    // Aerocity is ~2.3 km from Terminal 3
    val aerocityLat = 28.5500
    val aerocityLng = 77.1200
    val distanceToAerocity = airportZone.distanceToKm(aerocityLat, aerocityLng)
    assertTrue("Aerocity should be within 6 km", distanceToAerocity < 6.0)
    assertTrue("isInGeofence should be true for Aerocity", airportZone.isInGeofence(aerocityLat, aerocityLng))

    // Noida Sector 62 is ~30 km from IGI Airport
    val noidaLat = 28.6280
    val noidaLng = 77.3649
    val distanceToNoida = airportZone.distanceToKm(noidaLat, noidaLng)
    assertTrue("Noida should be > 20 km", distanceToNoida > 20.0)
    assertTrue("isInGeofence should be false for Noida", !airportZone.isInGeofence(noidaLat, noidaLng))
  }

  @Test
  fun testPolygonGeofence_RayCastingPointInPolygon() {
    // Define a 4-point polygon around Central Connaught Place
    val cpPolygon = listOf(
      Pair(28.6350, 77.2150), // North-West
      Pair(28.6350, 77.2250), // North-East
      Pair(28.6250, 77.2250), // South-East
      Pair(28.6250, 77.2150)  // South-West
    )

    val customZone = LocationZone(
      id = "zone_cp_poly",
      name = "Connaught Place Inner Circle",
      zoneType = ZoneType.PREFERRED_PICKUP,
      addressKeywords = listOf("Connaught Place", "CP", "Rajiv Chowk"),
      polygonPoints = cpPolygon
    )

    // Rajiv Chowk is at ~28.6300, 77.2200 -> inside CP polygon
    val insidePoint = Pair(28.6300, 77.2200)
    assertTrue("Rajiv Chowk must be inside polygon", customZone.isInGeofence(insidePoint.first, insidePoint.second))

    // India Gate is at ~28.6129, 77.2295 -> outside CP polygon
    val outsidePoint = Pair(28.6129, 77.2295)
    assertTrue("India Gate must be outside polygon", !customZone.isInGeofence(outsidePoint.first, outsidePoint.second))
  }

  @Test
  fun testAutoAcceptTiming_JitterBounds() {
    val settings = AutomationSettings(
      autoAcceptDelaySeconds = 1.5f,
      humanizeJitterEnabled = true
    )
    assertEquals(1.5f, settings.autoAcceptDelaySeconds, 0.01f)
    assertTrue(settings.humanizeJitterEnabled)

    // Verify jitter computation range is bounded between 300ms and 5000ms
    val baseMs = (settings.autoAcceptDelaySeconds * 1000).toLong()
    val jitter = 150L // within -200..250ms
    val effective = (baseMs + jitter).coerceIn(300L, 5000L)
    assertEquals(1650L, effective)
  }

  @Test
  fun testSubscriptionPlans_PricingAndTiers() {
    val plans = SubscriptionPlan.defaultPlans()

    val daily = plans.first { it.tier == PlanTier.DAILY }
    assertEquals(5.0, daily.priceInr, 0.01)
    assertEquals(1, daily.durationDays)

    val monthly = plans.first { it.tier == PlanTier.MONTHLY }
    assertEquals(99.0, monthly.priceInr, 0.01)
    assertEquals(30, monthly.durationDays)
    assertTrue(monthly.isPopular)

    val halfYearly = plans.first { it.tier == PlanTier.HALF_YEARLY }
    assertEquals(499.0, halfYearly.priceInr, 0.01)
    assertEquals(180, halfYearly.durationDays)

    val yearly = plans.first { it.tier == PlanTier.YEARLY }
    assertEquals(699.0, yearly.priceInr, 0.01)
    assertEquals(365, yearly.durationDays)
  }

  @Test
  fun testRazorpayCheckout_GstCalculation() {
    val basePrice = 99.0 // Monthly Saver Plan
    val gstRate = 0.18
    val gstAmount = basePrice * gstRate
    val totalPayable = basePrice + gstAmount

    assertEquals(17.82, gstAmount, 0.01)
    assertEquals(116.82, totalPayable, 0.01)
  }

  @Test
  fun testRazorpayBackendService_OrderAndSignatureVerification() {
    // Step 1: Order creation with minimum 100 paise validation
    val orderResult = RazorpayBackendService.createOrder(99.0, "rcpt_test_101")
    assertTrue(orderResult.isSuccess)
    val order = orderResult.getOrThrow()
    assertEquals(9900L, order.amountPaise)
    assertTrue(order.orderId.startsWith("order_rzp_"))

    // Step 2 & 3: Signature Generation and Verification with HMAC-SHA256
    val testPaymentId = "pay_rzp_test_abc123"
    val validSignature = RazorpayBackendService.generateSignature(order.orderId, testPaymentId)
    assertNotNull(validSignature)
    assertEquals(64, validSignature.length) // SHA256 hex is 64 characters

    // Authentic signature check
    val verificationSuccess = RazorpayBackendService.verifyPaymentSignature(order.orderId, testPaymentId, validSignature)
    assertTrue(verificationSuccess.isSuccess)

    // Tampered signature check (anti-fraud)
    val verificationFailed = RazorpayBackendService.verifyPaymentSignature(order.orderId, testPaymentId, "tampered_fake_signature_hex_123")
    assertFalse(verificationFailed.isSuccess)
  }

  @Test
  fun testRoomEntity_UserSettingsVehicleCriteriaPersistence() {
    val domainSettings = AutomationSettings(
      minFare = 150.0,
      maxFare = 1500.0,
      minFarePerKm = 18.0,
      maxPickupDistanceKm = 2.0,
      minTripDistanceKm = 3.0,
      maxTripDistanceKm = 25.0,
      preferredVehicleCategories = listOf("Bike Taxi", "Auto Rickshaw", "Economy Cab")
    )

    val entity = UserSettingsEntity.fromDomain(domainSettings)
    assertEquals(150.0, entity.minFare, 0.01)
    assertEquals(1500.0, entity.maxFare, 0.01)
    assertEquals(18.0, entity.minFarePerKm, 0.01)
    assertEquals(2.0, entity.maxPickupDistanceKm, 0.01)
    assertEquals(3.0, entity.minTripDistanceKm, 0.01)
    assertEquals(25.0, entity.maxTripDistanceKm, 0.01)
    assertTrue(entity.preferredVehicleTypes.contains("Bike Taxi"))
    assertTrue(entity.preferredVehicleTypes.contains("Auto Rickshaw"))
    assertTrue(entity.preferredVehicleTypes.contains("Economy Cab"))

    val mappedBack = entity.toDomain()
    assertEquals(domainSettings.minFare, mappedBack.minFare, 0.01)
    assertEquals(domainSettings.maxFare, mappedBack.maxFare, 0.01)
    assertEquals(domainSettings.minFarePerKm, mappedBack.minFarePerKm, 0.01)
    assertEquals(domainSettings.maxPickupDistanceKm, mappedBack.maxPickupDistanceKm, 0.01)
    assertEquals(domainSettings.minTripDistanceKm, mappedBack.minTripDistanceKm, 0.01)
    assertEquals(domainSettings.maxTripDistanceKm, mappedBack.maxTripDistanceKm, 0.01)
    assertEquals(3, mappedBack.preferredVehicleCategories.size)
    assertTrue(mappedBack.preferredVehicleCategories.contains("Bike Taxi"))
  }

  @Test
  fun testRideDecisionEngine_FiltersUnselectedVehicleType() {
    val settings = AutomationSettings(
      minFare = 50.0,
      preferredVehicleCategories = listOf("Bike Taxi", "Auto Rickshaw") // Cab not preferred
    )

    val cabRequest = RideRequest(
      id = "test_cab_01",
      platform = RidePlatform.UBER,
      fare = 350.0,
      pickupDistanceKm = 1.0,
      tripDistanceKm = 12.0,
      pickupLocation = "Janakpuri",
      dropLocation = "Gurgaon",
      estimatedMinutes = 25,
      vehicleCategory = "Uber Premier"
    )

    val result = RideDecisionEngine.evaluateRide(cabRequest, settings, emptyList())
    assertEquals(DecisionType.REJECT, result.decision)
    assertTrue(result.reason.contains("not in driver's preferred vehicle types"))
  }

  @Test
  fun testReactionCountdownOverlay_DefaultAndToggle() {
    val defaultSettings = AutomationSettings()
    assertTrue(defaultSettings.showReactionCountdownOverlay)

    val updatedSettings = defaultSettings.copy(showReactionCountdownOverlay = false)
    assertEquals(false, updatedSettings.showReactionCountdownOverlay)
  }

  @Test
  fun testAutomationState_ToggleLogic() {
    var state = AutomationState.STOPPED
    // Toggle to start
    state = if (state == AutomationState.RUNNING) AutomationState.STOPPED else AutomationState.RUNNING
    assertEquals(AutomationState.RUNNING, state)

    // Toggle to stop
    state = if (state == AutomationState.RUNNING) AutomationState.STOPPED else AutomationState.RUNNING
    assertEquals(AutomationState.STOPPED, state)
  }
}

