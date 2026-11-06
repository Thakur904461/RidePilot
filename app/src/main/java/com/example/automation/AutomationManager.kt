package com.example.automation

import android.content.Context
import com.example.data.local.RidePilotDatabase
import com.example.data.repository.RidePilotRepository
import com.example.model.AutomationSettings
import com.example.model.AutomationState
import com.example.model.DecisionType
import com.example.model.DriverProfile
import com.example.model.LocationZone
import com.example.model.PaymentRecord
import com.example.model.PlanTier
import com.example.model.PresetMode
import com.example.model.RidePlatform
import com.example.model.RideRequest
import com.example.model.RideStatus
import com.example.model.SubscriptionPlan
import com.example.model.ZoneType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import android.os.Build
import android.provider.Settings
import java.util.UUID

data class AutomationStats(
  val todayEarningsInr: Double = 1840.0,
  val todayAcceptedRides: Int = 11,
  val todayRejectedRides: Int = 8,
  val todayManualRides: Int = 2,
  val todayTotalKm: Double = 94.6,
  val activeAppsCount: Int = 4,
  val averageFarePerKm: Double = 19.4,
  val acceptanceRatePercent: Int = 58
)

class AutomationManager private constructor(context: Context) {

  private val appContext = context.applicationContext
  private val scope = CoroutineScope(Dispatchers.Main + Job())
  private val voiceAssistant = VoiceAssistant(appContext)

  // Room Persistence Layer
  val database = RidePilotDatabase.getInstance(appContext)
  val repository = RidePilotRepository(database)
  val firestoreSync = com.example.data.remote.FirestoreSyncService(appContext)

  private val _settings = MutableStateFlow(AutomationSettings())
  val settings: StateFlow<AutomationSettings> = _settings.asStateFlow()

  private val _stats = MutableStateFlow(AutomationStats())
  val stats: StateFlow<AutomationStats> = _stats.asStateFlow()

  private val _locationZones = MutableStateFlow(LocationZone.defaultZones())
  val locationZones: StateFlow<List<LocationZone>> = _locationZones.asStateFlow()

  private val _rideHistory = MutableStateFlow<List<RideRequest>>(emptyList())
  val rideHistory: StateFlow<List<RideRequest>> = _rideHistory.asStateFlow()

  private val _currentActiveRequest = MutableStateFlow<RideRequest?>(null)
  val currentActiveRequest: StateFlow<RideRequest?> = _currentActiveRequest.asStateFlow()

  private val _driverProfile = MutableStateFlow(DriverProfile())
  val driverProfile: StateFlow<DriverProfile> = _driverProfile.asStateFlow()

  private val _isSubscribed = MutableStateFlow(true)
  val isSubscribed: StateFlow<Boolean> = _isSubscribed.asStateFlow()

  private val _activePlan = MutableStateFlow(SubscriptionPlan.defaultPlans().first { it.tier == PlanTier.MONTHLY }) // Monthly Saver ₹99
  val activePlan: StateFlow<SubscriptionPlan> = _activePlan.asStateFlow()

  private val _paymentHistory = MutableStateFlow(
    listOf(
      PaymentRecord(
        paymentId = "pay_RP_789102",
        orderId = "order_RP_991823",
        planName = "Monthly Saver Auto-Pilot",
        amountInr = 99.0,
        timestamp = System.currentTimeMillis() - 86400000L * 12,
        status = "SUCCESS",
        method = "UPI (Google Pay)",
        invoiceNumber = "INV-2026-RP-0418"
      )
    )
  )
  val paymentHistory: StateFlow<List<PaymentRecord>> = _paymentHistory.asStateFlow()

  private val _isAccessibilityServiceConnected = MutableStateFlow(false)
  val isAccessibilityServiceConnected: StateFlow<Boolean> = _isAccessibilityServiceConnected.asStateFlow()

  private val _isOverlayPermissionGranted = MutableStateFlow(true)
  val isOverlayPermissionGranted: StateFlow<Boolean> = _isOverlayPermissionGranted.asStateFlow()

  private val _isOverlayServiceActive = MutableStateFlow(false)
  val isOverlayServiceActive: StateFlow<Boolean> = _isOverlayServiceActive.asStateFlow()

  fun setOverlayServiceActive(active: Boolean) {
    _isOverlayServiceActive.value = active
  }

  fun hasOverlayPermission(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      Settings.canDrawOverlays(appContext)
    } else true
  }

  fun checkOverlayPermission() {
    _isOverlayPermissionGranted.value = hasOverlayPermission()
  }

  fun toggleFloatingWidget() {
    val permitted = hasOverlayPermission()
    _isOverlayPermissionGranted.value = permitted
    if (!permitted) return

    if (_isOverlayServiceActive.value) {
      FloatingWidgetService.stop(appContext)
      _isOverlayServiceActive.value = false
    } else {
      FloatingWidgetService.start(appContext)
      _isOverlayServiceActive.value = true
    }
  }

  private var pendingCountdownJob: Job? = null
  private val _pendingCountdownSeconds = MutableStateFlow<Float?>(null)
  val pendingCountdownSeconds: StateFlow<Float?> = _pendingCountdownSeconds.asStateFlow()

  init {
    loadSeedHistory()
    observeRoomDatabase()
  }

  private fun observeRoomDatabase() {
    scope.launch(Dispatchers.IO) {
      repository.allRides.collectLatest { persistedRides ->
        if (persistedRides.isNotEmpty()) {
          _rideHistory.value = persistedRides
        }
      }
    }

    scope.launch(Dispatchers.IO) {
      repository.userSettings.collectLatest { persistedSettings ->
        if (persistedSettings != null) {
          _settings.update { current ->
            persistedSettings.copy(platformConfigs = current.platformConfigs)
          }
        }
      }
    }

    scope.launch(Dispatchers.IO) {
      repository.allZones.collectLatest { persistedZones ->
        if (persistedZones.isNotEmpty()) {
          _locationZones.value = persistedZones
        } else {
          repository.insertAllZones(LocationZone.defaultZones())
        }
      }
    }
  }

  fun setAccessibilityConnected(connected: Boolean) {
    _isAccessibilityServiceConnected.value = connected
  }

  fun updateState(newState: AutomationState) {
    _settings.update { it.copy(state = newState) }
    scope.launch(Dispatchers.IO) {
      repository.saveUserSettings(_settings.value)
    }
    val msg = when (newState) {
      AutomationState.RUNNING -> "RidePilot automation started"
      AutomationState.PAUSED -> "RidePilot paused"
      AutomationState.STOPPED -> "RidePilot stopped"
    }
    voiceAssistant.speak(msg, _settings.value.voiceLanguage == "hi")
  }

  fun toggleEmergencyStop() {
    _settings.update {
      val newStop = !it.emergencyStopActive
      it.copy(
        emergencyStopActive = newStop,
        state = if (newStop) AutomationState.STOPPED else AutomationState.RUNNING
      )
    }
    scope.launch(Dispatchers.IO) {
      repository.saveUserSettings(_settings.value)
    }
  }

  fun selectPreset(preset: PresetMode) {
    _settings.update { current ->
      val newPresetSettings = AutomationSettings.fromPreset(preset)
      current.copy(
        activePreset = preset,
        minFare = newPresetSettings.minFare,
        maxFare = newPresetSettings.maxFare,
        minFarePerKm = newPresetSettings.minFarePerKm,
        maxPickupDistanceKm = newPresetSettings.maxPickupDistanceKm,
        minTripDistanceKm = newPresetSettings.minTripDistanceKm,
        maxTripDistanceKm = newPresetSettings.maxTripDistanceKm
      )
    }
    scope.launch(Dispatchers.IO) {
      repository.saveUserSettings(_settings.value)
    }
  }

  fun updateFareFilters(minFare: Double, maxFare: Double, minPerKm: Double) {
    _settings.update {
      it.copy(
        activePreset = PresetMode.CUSTOM,
        minFare = minFare,
        maxFare = maxFare,
        minFarePerKm = minPerKm
      )
    }
    scope.launch(Dispatchers.IO) {
      repository.saveUserSettings(_settings.value)
    }
  }

  fun updateDistanceFilters(maxPickup: Double, minTrip: Double, maxTrip: Double) {
    _settings.update {
      it.copy(
        activePreset = PresetMode.CUSTOM,
        maxPickupDistanceKm = maxPickup,
        minTripDistanceKm = minTrip,
        maxTripDistanceKm = maxTrip
      )
    }
    scope.launch(Dispatchers.IO) {
      repository.saveUserSettings(_settings.value)
    }
  }

  fun updateRideCriteria(
    minFare: Double,
    maxFare: Double,
    minPerKm: Double,
    maxPickup: Double,
    minTrip: Double,
    maxTrip: Double,
    preferredVehicles: List<String>
  ) {
    _settings.update {
      it.copy(
        activePreset = PresetMode.CUSTOM,
        minFare = minFare,
        maxFare = maxFare,
        minFarePerKm = minPerKm,
        maxPickupDistanceKm = maxPickup,
        minTripDistanceKm = minTrip,
        maxTripDistanceKm = maxTrip,
        preferredVehicleCategories = preferredVehicles
      )
    }
    scope.launch(Dispatchers.IO) {
      repository.saveUserSettings(_settings.value)
    }
  }

  fun toggleVehicleCategory(category: String, isSelected: Boolean) {
    _settings.update { current ->
      val updated = if (isSelected) {
        if (!current.preferredVehicleCategories.contains(category)) current.preferredVehicleCategories + category else current.preferredVehicleCategories
      } else {
        current.preferredVehicleCategories - category
      }
      current.copy(preferredVehicleCategories = updated, activePreset = PresetMode.CUSTOM)
    }
    scope.launch(Dispatchers.IO) {
      repository.saveUserSettings(_settings.value)
    }
  }

  fun togglePlatform(platform: RidePlatform, isEnabled: Boolean) {
    _settings.update { current ->
      val currentConfigs = current.platformConfigs.toMutableMap()
      val existing = currentConfigs[platform] ?: com.example.model.PlatformConfig(platform = platform)
      currentConfigs[platform] = existing.copy(isEnabled = isEnabled)
      current.copy(platformConfigs = currentConfigs)
    }
    scope.launch(Dispatchers.IO) {
      repository.setPlatformEnabled(platform, isEnabled)
    }
  }

  fun updateVoiceSettings(enabled: Boolean, language: String) {
    _settings.update {
      it.copy(voiceAnnouncementsEnabled = enabled, voiceLanguage = language)
    }
    scope.launch(Dispatchers.IO) {
      repository.saveUserSettings(_settings.value)
    }
  }

  fun updateTimingSettings(delaySeconds: Float, jitterEnabled: Boolean, showCountdown: Boolean) {
    _settings.update {
      it.copy(
        autoAcceptDelaySeconds = delaySeconds,
        humanizeJitterEnabled = jitterEnabled,
        showReactionCountdownOverlay = showCountdown
      )
    }
    scope.launch(Dispatchers.IO) {
      repository.saveUserSettings(_settings.value)
    }
  }

  fun abortPendingAutoAccept() {
    pendingCountdownJob?.cancel()
    pendingCountdownJob = null
    _pendingCountdownSeconds.value = null
    val current = _currentActiveRequest.value
    if (current != null && current.status == RideStatus.ACCEPTED) {
      val aborted = current.copy(
        status = RideStatus.MANUAL_REVIEW,
        decisionReason = "Auto-accept cancelled by driver during reaction window"
      )
      _currentActiveRequest.value = aborted
      _rideHistory.update { list -> list.map { if (it.id == aborted.id) aborted else it } }
      scope.launch(Dispatchers.IO) {
        repository.insertRide(aborted)
      }
      voiceAssistant.speak("Auto accept aborted", _settings.value.voiceLanguage == "hi")
    }
  }

  fun addLocationZone(
    name: String,
    type: ZoneType,
    keywords: List<String>,
    radiusKm: Double,
    lat: Double = 0.0,
    lng: Double = 0.0,
    polygonPoints: List<Pair<Double, Double>> = emptyList()
  ) {
    val newZone = LocationZone(
      id = "zone_${UUID.randomUUID().toString().take(6)}",
      name = name,
      zoneType = type,
      addressKeywords = keywords,
      lat = lat,
      lng = lng,
      radiusKm = radiusKm,
      polygonPoints = polygonPoints,
      isActive = true
    )
    _locationZones.update { listOf(newZone) + it }
    scope.launch(Dispatchers.IO) {
      repository.insertZone(newZone)
    }
  }

  fun toggleLocationZone(id: String) {
    var newActive = true
    _locationZones.update { list ->
      list.map {
        if (it.id == id) {
          newActive = !it.isActive
          it.copy(isActive = newActive)
        } else it
      }
    }
    scope.launch(Dispatchers.IO) {
      repository.toggleZoneActive(id, newActive)
    }
  }

  fun deleteLocationZone(id: String) {
    _locationZones.update { list -> list.filter { it.id != id } }
    scope.launch(Dispatchers.IO) {
      repository.deleteZone(id)
    }
  }

  /**
   * Processes a detected or simulated ride request through the decision engine and saves to Room.
   */
  fun processRideRequest(request: RideRequest, onActionTriggered: ((DecisionType) -> Unit)? = null) {
    val result = RideDecisionEngine.evaluateRide(request, _settings.value, _locationZones.value)

    val finalStatus = when (result.decision) {
      DecisionType.ACCEPT -> RideStatus.ACCEPTED
      DecisionType.REJECT -> RideStatus.REJECTED
      DecisionType.MANUAL_REVIEW -> RideStatus.MANUAL_REVIEW
      DecisionType.UNKNOWN -> RideStatus.PENDING
    }

    val updatedRequest = request.copy(
      status = finalStatus,
      decisionReason = result.reason,
      isVerified = result.decision == DecisionType.ACCEPT
    )

    _currentActiveRequest.value = updatedRequest
    _rideHistory.update { listOf(updatedRequest) + it }

    // Persist to Room Database and Firestore Cloud
    scope.launch(Dispatchers.IO) {
      repository.insertRide(updatedRequest)
      firestoreSync.syncRideToCloud(updatedRequest)
    }

    // Voice announcement
    if (_settings.value.voiceAnnouncementsEnabled) {
      voiceAssistant.announceRideDecision(
        request = updatedRequest,
        decision = result.decision,
        isHindi = _settings.value.voiceLanguage == "hi"
      )
    }

    // Update statistics
    if (result.decision == DecisionType.ACCEPT) {
      _stats.update {
        val newEarnings = it.todayEarningsInr + updatedRequest.fare
        val newAccepted = it.todayAcceptedRides + 1
        val newKm = it.todayTotalKm + updatedRequest.tripDistanceKm
        val totalDecided = newAccepted + it.todayRejectedRides
        val rate = if (totalDecided > 0) ((newAccepted.toDouble() / totalDecided) * 100).toInt() else 0
        it.copy(
          todayEarningsInr = newEarnings,
          todayAcceptedRides = newAccepted,
          todayTotalKm = newKm,
          averageFarePerKm = if (newKm > 0) newEarnings / newKm else 0.0,
          acceptanceRatePercent = rate
        )
      }
    } else if (result.decision == DecisionType.REJECT) {
      _stats.update {
        val newRejected = it.todayRejectedRides + 1
        val totalDecided = it.todayAcceptedRides + newRejected
        val rate = if (totalDecided > 0) ((it.todayAcceptedRides.toDouble() / totalDecided) * 100).toInt() else 0
        it.copy(
          todayRejectedRides = newRejected,
          acceptanceRatePercent = rate
        )
      }
    }

    if (result.decision == DecisionType.ACCEPT && _settings.value.autoAcceptDelaySeconds > 0.1f) {
      val baseDelayMs = (_settings.value.autoAcceptDelaySeconds * 1000).toLong()
      val jitterMs = if (_settings.value.humanizeJitterEnabled) {
        java.util.concurrent.ThreadLocalRandom.current().nextLong(-200L, 250L)
      } else 0L
      val effectiveDelayMs = (baseDelayMs + jitterMs).coerceIn(300L, 5000L)
      val interval = 100L
      val steps = (effectiveDelayMs / interval).toInt()

      pendingCountdownJob?.cancel()
      pendingCountdownJob = scope.launch {
        for (s in steps downTo 0) {
          _pendingCountdownSeconds.value = s * 0.1f
          delay(interval)
        }
        _pendingCountdownSeconds.value = null
        onActionTriggered?.invoke(DecisionType.ACCEPT)

        // Clear active card after brief showcase window
        delay(6000)
        if (_currentActiveRequest.value?.id == updatedRequest.id) {
          _currentActiveRequest.value = null
        }
      }
    } else {
      onActionTriggered?.invoke(result.decision)

      // Clear active card after brief showcase window
      scope.launch {
        delay(6000)
        if (_currentActiveRequest.value?.id == updatedRequest.id) {
          _currentActiveRequest.value = null
        }
      }
    }
  }

  fun triggerSimulationRide(
    platform: RidePlatform = RidePlatform.entries.filter { it != RidePlatform.OTHER }.random(),
    forcedDecision: DecisionType? = null
  ) {
    val sampleLocations = listOf(
      Pair("Rajiv Chowk Metro Stn Gate 4", "IGI Airport Terminal 3 Departure"),
      Pair("Noida City Centre Sector 39", "DLF Cyber City Phase 2 Gurgaon"),
      Pair("Lajpat Nagar Central Market", "New Delhi Railway Station Paharganj"),
      Pair("Dwarka Mor Metro Station", "Janakpuri District Centre"),
      Pair("Saket Select Citywalk Mall", "Badarpur Border Tollway Outskirts"),
      Pair("Indiranagar 100ft Road Bangalore", "Kempegowda Int'l Airport Devanahalli"),
      Pair("Koramangala 5th Block Sony Signal", "Electronic City Phase 1 Infosys")
    )

    val locPair = sampleLocations.random()
    val pickupDist = when (forcedDecision) {
      DecisionType.REJECT -> 4.8 // exceeds max pickup
      else -> (0.8 + Math.random() * 2.0)
    }

    val tripDist = when (forcedDecision) {
      DecisionType.REJECT -> 2.2
      else -> (6.0 + Math.random() * 18.0)
    }

    val calculatedFare = when (forcedDecision) {
      DecisionType.REJECT -> 65.0 // low fare
      DecisionType.ACCEPT -> tripDist * (19.0 + Math.random() * 7.0) // high ₹/km
      else -> tripDist * 16.5
    }

    val simRequest = RideRequest(
      id = "sim_${UUID.randomUUID().toString().take(6)}",
      platform = platform,
      fare = calculatedFare,
      pickupDistanceKm = pickupDist,
      tripDistanceKm = tripDist,
      pickupLocation = locPair.first,
      dropLocation = locPair.second,
      estimatedMinutes = (tripDist * 2.5).toInt(),
      vehicleCategory = when (platform) {
        RidePlatform.OLA -> "Mini Cab / Auto"
        RidePlatform.UBER -> "Uber Go / Auto"
        RidePlatform.RAPIDO -> "Bike Taxi"
        RidePlatform.PORTER -> "Goods 2-Wheeler"
        RidePlatform.OTHER -> "Delivery Ride"
      },
      confidence = 0.94f,
      isSimulated = true
    )

    processRideRequest(simRequest)
  }

  fun manualAcceptRide(requestId: String) {
    _rideHistory.update { list ->
      list.map { req ->
        if (req.id == requestId) req.copy(status = RideStatus.ACCEPTED, decisionReason = "Manually accepted by driver")
        else req
      }
    }
    _currentActiveRequest.value = null
    voiceAssistant.speak("Ride manually accepted", _settings.value.voiceLanguage == "hi")
  }

  fun manualRejectRide(requestId: String) {
    _rideHistory.update { list ->
      list.map { req ->
        if (req.id == requestId) req.copy(status = RideStatus.REJECTED, decisionReason = "Manually rejected by driver")
        else req
      }
    }
    _currentActiveRequest.value = null
    voiceAssistant.speak("Ride rejected", _settings.value.voiceLanguage == "hi")
  }

  fun subscribePlan(plan: SubscriptionPlan) {
    _activePlan.value = plan
    _isSubscribed.value = true
    val record = PaymentRecord(
      paymentId = "pay_RP_${UUID.randomUUID().toString().take(8)}",
      orderId = "order_RP_${UUID.randomUUID().toString().take(8)}",
      planName = plan.tier.displayName,
      amountInr = plan.priceInr,
      timestamp = System.currentTimeMillis(),
      status = "SUCCESS",
      method = "UPI (Instant)",
      invoiceNumber = "INV-2026-RP-${(1000..9999).random()}"
    )
    _paymentHistory.update { listOf(record) + it }
    voiceAssistant.speak("${plan.tier.displayName} activated successfully", _settings.value.voiceLanguage == "hi")
  }

  fun recordPayment(plan: SubscriptionPlan, record: PaymentRecord) {
    _activePlan.value = plan
    _isSubscribed.value = true
    _paymentHistory.update { listOf(record) + it }
    voiceAssistant.speak("${plan.tier.displayName} activated with Razorpay", _settings.value.voiceLanguage == "hi")
  }

  private fun loadSeedHistory() {
    val seed = listOf(
      RideRequest(
        id = "ride_01",
        platform = RidePlatform.UBER,
        fare = 420.0,
        pickupDistanceKm = 1.4,
        tripDistanceKm = 18.2,
        pickupLocation = "Hauz Khas Market, Delhi",
        dropLocation = "IGI Airport Terminal 3, Delhi",
        estimatedMinutes = 35,
        vehicleCategory = "Uber Go",
        timestamp = System.currentTimeMillis() - 1000L * 60 * 25,
        status = RideStatus.ACCEPTED,
        decisionReason = "Matches criteria: ₹23.0/km (min ₹16), pickup 1.4km (max 2.5km) [Preferred Drop Zone]",
        confidence = 0.96f,
        isVerified = true
      ),
      RideRequest(
        id = "ride_02",
        platform = RidePlatform.RAPIDO,
        fare = 85.0,
        pickupDistanceKm = 0.9,
        tripDistanceKm = 4.1,
        pickupLocation = "Saket Metro Station Gate 2",
        dropLocation = "Malviya Nagar Shivalik Colony",
        estimatedMinutes = 12,
        vehicleCategory = "Bike Taxi",
        timestamp = System.currentTimeMillis() - 1000L * 60 * 65,
        status = RideStatus.ACCEPTED,
        decisionReason = "Matches criteria: ₹20.7/km, pickup 0.9km",
        confidence = 0.92f,
        isVerified = true
      ),
      RideRequest(
        id = "ride_03",
        platform = RidePlatform.OLA,
        fare = 110.0,
        pickupDistanceKm = 4.2,
        tripDistanceKm = 7.0,
        pickupLocation = "Outer Ring Road Nehru Place",
        dropLocation = "Okhla Phase 3 Industrial Area",
        estimatedMinutes = 20,
        vehicleCategory = "Auto",
        timestamp = System.currentTimeMillis() - 1000L * 60 * 110,
        status = RideStatus.REJECTED,
        decisionReason = "Pickup distance (4.2 km) exceeds max limit (2.5 km)",
        confidence = 0.88f
      ),
      RideRequest(
        id = "ride_04",
        platform = RidePlatform.PORTER,
        fare = 310.0,
        pickupDistanceKm = 1.8,
        tripDistanceKm = 13.5,
        pickupLocation = "Chandni Chowk Wholesale Hub",
        dropLocation = "Laxmi Nagar Vikas Marg",
        estimatedMinutes = 40,
        vehicleCategory = "2 Wheeler Delivery",
        timestamp = System.currentTimeMillis() - 1000L * 60 * 175,
        status = RideStatus.ACCEPTED,
        decisionReason = "Matches criteria: ₹22.9/km, pickup 1.8km",
        confidence = 0.95f,
        isVerified = true
      ),
      RideRequest(
        id = "ride_05",
        platform = RidePlatform.UBER,
        fare = 95.0,
        pickupDistanceKm = 1.1,
        tripDistanceKm = 11.0,
        pickupLocation = "Mayur Vihar Phase 1",
        dropLocation = "Badarpur Border Tollway Outskirts",
        estimatedMinutes = 30,
        vehicleCategory = "Uber Moto",
        timestamp = System.currentTimeMillis() - 1000L * 60 * 220,
        status = RideStatus.REJECTED,
        decisionReason = "Destination matches excluded zone: Heavy Toll / No-Go Border Zone",
        confidence = 0.94f
      )
    )
    _rideHistory.value = seed

    scope.launch(Dispatchers.IO) {
      repository.insertRides(seed)
    }
  }

  companion object {
    @Volatile
    private var instance: AutomationManager? = null

    fun getInstance(context: Context): AutomationManager {
      return instance ?: synchronized(this) {
        instance ?: AutomationManager(context).also { instance = it }
      }
    }
  }
}
