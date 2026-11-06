package com.example.automation

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.model.AutomationState
import com.example.model.DecisionType
import com.example.model.RidePlatform
import com.example.model.RideRequest
import com.example.model.RideStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale

class FloatingWidgetService : Service() {

  private var windowManager: WindowManager? = null
  private var rootContainer: FrameLayout? = null
  private var layoutParams: WindowManager.LayoutParams? = null

  private var bubbleView: LinearLayout? = null
  private var cardView: LinearLayout? = null

  // Bubble Views
  private var tvBubbleTitle: TextView? = null
  private var tvBubbleStatus: TextView? = null
  private var bubbleBorderDrawable: GradientDrawable? = null

  // Card Views
  private var tvCardPlatform: TextView? = null
  private var tvCardVehicle: TextView? = null
  private var tvCardFare: TextView? = null
  private var tvCardPerKm: TextView? = null
  private var tvCardPickup: TextView? = null
  private var tvCardDrop: TextView? = null
  private var tvCardDecision: TextView? = null
  private var tvCardReason: TextView? = null
  private var layoutCountdown: LinearLayout? = null
  private var tvCountdownText: TextView? = null
  private var btnAbortCountdown: TextView? = null

  // Action Buttons
  private var layoutActions: LinearLayout? = null
  private var btnCardReject: Button? = null
  private var btnCardAccept: Button? = null
  private var btnCardOpenApp: Button? = null

  // Idle Cockpit Container
  private var layoutIdleCockpit: LinearLayout? = null
  private var tvCockpitStats: TextView? = null
  private var btnTogglePause: Button? = null
  private var btnQuickSimulate: Button? = null

  private var isExpanded = false
  private var currentRequestId: String? = null

  private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

  companion object {
    const val CHANNEL_ID = "ridepilot_floating_service_channel"
    const val NOTIFICATION_ID = 4042

    fun start(context: Context) {
      val intent = Intent(context, FloatingWidgetService::class.java)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
      } else {
        context.startService(intent)
      }
    }

    fun stop(context: Context) {
      context.stopService(Intent(context, FloatingWidgetService::class.java))
    }
  }

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onCreate() {
    super.onCreate()
    createNotificationChannel()
    startForeground(NOTIFICATION_ID, buildForegroundNotification())

    AutomationManager.getInstance(this).setOverlayServiceActive(true)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
      // Overlay permission not granted; notification remains active
      return
    }

    initFloatingOverlay()
    observeAutomationState()
  }

  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(
        CHANNEL_ID,
        "RidePilot Floating Overlay Service",
        NotificationManager.IMPORTANCE_LOW
      ).apply {
        description = "Provides draggable cockpit widget and live ride alerts on top of navigation apps"
      }
      val manager = getSystemService(NotificationManager::class.java)
      manager?.createNotificationChannel(channel)
    }
  }

  private fun buildForegroundNotification(): Notification {
    val pendingIntent = PendingIntent.getActivity(
      this,
      0,
      Intent(this, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
      },
      PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    return NotificationCompat.Builder(this, CHANNEL_ID)
      .setContentTitle("RidePilot Floating Overlay Active")
      .setContentText("Monitoring rides over Ola, Uber, Rapido & Porter")
      .setSmallIcon(android.R.drawable.ic_menu_compass)
      .setContentIntent(pendingIntent)
      .setOngoing(true)
      .build()
  }

  @SuppressLint("ClickableViewAccessibility")
  private fun initFloatingOverlay() {
    try {
      windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

      val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
      } else {
        @Suppress("DEPRECATION")
        WindowManager.LayoutParams.TYPE_PHONE
      }

      layoutParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        windowType,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT
      ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = dpToPx(16)
        y = dpToPx(120)
      }

      rootContainer = FrameLayout(this).apply {
        layoutParams = ViewGroup.LayoutParams(
          ViewGroup.LayoutParams.WRAP_CONTENT,
          ViewGroup.LayoutParams.WRAP_CONTENT
        )
      }

      buildBubbleView()
      buildCardView()

      rootContainer?.addView(cardView)
      rootContainer?.addView(bubbleView)

      windowManager?.addView(rootContainer, layoutParams)
    } catch (_: Exception) {
      // Catch any window manager attachment failure
    }
  }

  @SuppressLint("ClickableViewAccessibility")
  private fun buildBubbleView() {
    bubbleBorderDrawable = GradientDrawable().apply {
      shape = GradientDrawable.RECTANGLE
      cornerRadius = dpToPx(24).toFloat()
      setColor(Color.parseColor("#091624"))
      setStroke(dpToPx(2), Color.parseColor("#00E676"))
    }

    bubbleView = LinearLayout(this).apply {
      orientation = LinearLayout.HORIZONTAL
      background = bubbleBorderDrawable
      gravity = Gravity.CENTER_VERTICAL
      setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8))
      elevation = dpToPx(10).toFloat()
    }

    // Emblem
    val emblemView = TextView(this).apply {
      text = "RP"
      setTextColor(Color.parseColor("#041C0C"))
      textSize = 11f
      setTypeface(null, Typeface.BOLD)
      gravity = Gravity.CENTER
      background = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(Color.parseColor("#00E676"))
      }
      layoutParams = LinearLayout.LayoutParams(dpToPx(24), dpToPx(24)).apply {
        marginEnd = dpToPx(8)
      }
    }

    tvBubbleTitle = TextView(this).apply {
      text = "RidePilot"
      setTextColor(Color.WHITE)
      textSize = 12f
      setTypeface(null, Typeface.BOLD)
      layoutParams = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      ).apply {
        marginEnd = dpToPx(6)
      }
    }

    tvBubbleStatus = TextView(this).apply {
      text = "ACTIVE"
      setTextColor(Color.parseColor("#00E676"))
      textSize = 10f
      setTypeface(null, Typeface.BOLD)
      background = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = dpToPx(4).toFloat()
        setColor(Color.parseColor("#00391A"))
      }
      setPadding(dpToPx(6), dpToPx(2), dpToPx(6), dpToPx(2))
    }

    bubbleView?.addView(emblemView)
    bubbleView?.addView(tvBubbleTitle)
    bubbleView?.addView(tvBubbleStatus)

    // Touch & Drag Handling on Bubble
    bubbleView?.setOnTouchListener(object : View.OnTouchListener {
      private var initialX = 0
      private var initialY = 0
      private var initialTouchX = 0f
      private var initialTouchY = 0f
      private var isDragging = false

      override fun onTouch(v: View, event: MotionEvent): Boolean {
        val params = layoutParams ?: return false
        when (event.action) {
          MotionEvent.ACTION_DOWN -> {
            initialX = params.x
            initialY = params.y
            initialTouchX = event.rawX
            initialTouchY = event.rawY
            isDragging = false
            return true
          }
          MotionEvent.ACTION_MOVE -> {
            val dx = (event.rawX - initialTouchX).toInt()
            val dy = (event.rawY - initialTouchY).toInt()
            if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
              isDragging = true
            }
            if (isDragging) {
              params.x = initialX + dx
              params.y = initialY + dy
              try {
                windowManager?.updateViewLayout(rootContainer, params)
              } catch (_: Exception) {}
            }
            return true
          }
          MotionEvent.ACTION_UP -> {
            if (!isDragging) {
              // Click detected -> toggle expanded card
              toggleExpandedState()
            }
            return true
          }
        }
        return false
      }
    })
  }

  private fun buildCardView() {
    cardView = LinearLayout(this).apply {
      orientation = LinearLayout.VERTICAL
      visibility = View.GONE
      background = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = dpToPx(16).toFloat()
        setColor(Color.parseColor("#081426"))
        setStroke(dpToPx(1), Color.parseColor("#1E3A5F"))
      }
      setPadding(dpToPx(14), dpToPx(12), dpToPx(14), dpToPx(12))
      elevation = dpToPx(12).toFloat()
      layoutParams = FrameLayout.LayoutParams(dpToPx(320), ViewGroup.LayoutParams.WRAP_CONTENT).apply {
        topMargin = dpToPx(52) // Placed right under the bubble
      }
    }

    // Header (Platform & Close X)
    val headerLayout = LinearLayout(this).apply {
      orientation = LinearLayout.HORIZONTAL
      gravity = Gravity.CENTER_VERTICAL
      layoutParams = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
    }

    tvCardPlatform = TextView(this).apply {
      text = "OLA MINI"
      setTextColor(Color.WHITE)
      textSize = 13f
      setTypeface(null, Typeface.BOLD)
      layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
    }

    tvCardVehicle = TextView(this).apply {
      text = "4 min away"
      setTextColor(Color.parseColor("#38BDF8"))
      textSize = 11f
      layoutParams = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      ).apply {
        marginEnd = dpToPx(10)
      }
    }

    val btnClose = TextView(this).apply {
      text = "✕"
      setTextColor(Color.parseColor("#94A3B8"))
      textSize = 14f
      setTypeface(null, Typeface.BOLD)
      setPadding(dpToPx(6), dpToPx(2), dpToPx(6), dpToPx(2))
      setOnClickListener { toggleExpandedState(false) }
    }

    headerLayout.addView(tvCardPlatform)
    headerLayout.addView(tvCardVehicle)
    headerLayout.addView(btnClose)
    cardView?.addView(headerLayout)

    // Fare & Per-KM Row
    val fareLayout = LinearLayout(this).apply {
      orientation = LinearLayout.HORIZONTAL
      gravity = Gravity.CENTER_VERTICAL
      setPadding(0, dpToPx(8), 0, dpToPx(4))
      layoutParams = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
    }

    tvCardFare = TextView(this).apply {
      text = "₹320"
      setTextColor(Color.parseColor("#00E676"))
      textSize = 22f
      setTypeface(null, Typeface.BOLD)
    }

    tvCardPerKm = TextView(this).apply {
      text = "  (₹21.3 / km)"
      setTextColor(Color.parseColor("#00E5FF"))
      textSize = 13f
      setTypeface(null, Typeface.BOLD)
    }

    fareLayout.addView(tvCardFare)
    fareLayout.addView(tvCardPerKm)
    cardView?.addView(fareLayout)

    // Pickup & Drop Locations
    tvCardPickup = TextView(this).apply {
      text = "📍 Connaught Place Gate 4"
      setTextColor(Color.parseColor("#E2E8F0"))
      textSize = 11f
      maxLines = 1
      setPadding(0, dpToPx(2), 0, dpToPx(2))
    }

    tvCardDrop = TextView(this).apply {
      text = "🏁 IGI Airport Terminal 3"
      setTextColor(Color.parseColor("#94A3B8"))
      textSize = 11f
      maxLines = 1
      setPadding(0, dpToPx(2), 0, dpToPx(4))
    }

    cardView?.addView(tvCardPickup)
    cardView?.addView(tvCardDrop)

    // Decision Badge & Reason
    tvCardDecision = TextView(this).apply {
      text = "AUTO-ACCEPT: MATCHES ALL RULES"
      setTextColor(Color.parseColor("#00E676"))
      textSize = 11f
      setTypeface(null, Typeface.BOLD)
      background = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = dpToPx(4).toFloat()
        setColor(Color.parseColor("#00391A"))
      }
      setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))
    }

    tvCardReason = TextView(this).apply {
      text = "Fare ₹21.3/km ≥ Target ₹16/km • Within Airport zone"
      setTextColor(Color.parseColor("#94A3B8"))
      textSize = 10f
      setPadding(0, dpToPx(4), 0, dpToPx(4))
    }

    cardView?.addView(tvCardDecision)
    cardView?.addView(tvCardReason)

    // Countdown Abort Layout
    layoutCountdown = LinearLayout(this).apply {
      orientation = LinearLayout.HORIZONTAL
      gravity = Gravity.CENTER_VERTICAL
      visibility = View.GONE
      background = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = dpToPx(6).toFloat()
        setColor(Color.parseColor("#00391A"))
        setStroke(dpToPx(1), Color.parseColor("#00E676"))
      }
      setPadding(dpToPx(8), dpToPx(6), dpToPx(8), dpToPx(6))
      layoutParams = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      ).apply {
        topMargin = dpToPx(6)
      }
    }

    tvCountdownText = TextView(this).apply {
      text = "Auto-accepting in 1.2s..."
      setTextColor(Color.parseColor("#00E676"))
      textSize = 11f
      setTypeface(null, Typeface.BOLD)
      layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
    }

    btnAbortCountdown = TextView(this).apply {
      text = "ABORT"
      setTextColor(Color.WHITE)
      textSize = 10f
      setTypeface(null, Typeface.BOLD)
      background = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = dpToPx(4).toFloat()
        setColor(Color.parseColor("#FF5252"))
      }
      setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))
      setOnClickListener {
        AutomationManager.getInstance(this@FloatingWidgetService).abortPendingAutoAccept()
      }
    }

    layoutCountdown?.addView(tvCountdownText)
    layoutCountdown?.addView(btnAbortCountdown)
    cardView?.addView(layoutCountdown)

    // Action Buttons Row (Skip, Accept, Open App)
    layoutActions = LinearLayout(this).apply {
      orientation = LinearLayout.HORIZONTAL
      setPadding(0, dpToPx(8), 0, 0)
      layoutParams = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
    }

    btnCardReject = Button(this).apply {
      text = "SKIP"
      setTextColor(Color.parseColor("#FF5252"))
      textSize = 11f
      background = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = dpToPx(8).toFloat()
        setColor(Color.parseColor("#2E1010"))
        setStroke(dpToPx(1), Color.parseColor("#FF5252"))
      }
      layoutParams = LinearLayout.LayoutParams(0, dpToPx(38), 1f).apply {
        marginEnd = dpToPx(6)
      }
      setOnClickListener {
        currentRequestId?.let { id ->
          AutomationManager.getInstance(this@FloatingWidgetService).manualRejectRide(id)
        }
      }
    }

    btnCardAccept = Button(this).apply {
      text = "ACCEPT"
      setTextColor(Color.parseColor("#041C0C"))
      textSize = 11f
      setTypeface(null, Typeface.BOLD)
      background = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = dpToPx(8).toFloat()
        setColor(Color.parseColor("#00E676"))
      }
      layoutParams = LinearLayout.LayoutParams(0, dpToPx(38), 1f).apply {
        marginEnd = dpToPx(6)
      }
      setOnClickListener {
        currentRequestId?.let { id ->
          AutomationManager.getInstance(this@FloatingWidgetService).manualAcceptRide(id)
        }
      }
    }

    btnCardOpenApp = Button(this).apply {
      text = "APP"
      setTextColor(Color.parseColor("#38BDF8"))
      textSize = 11f
      background = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = dpToPx(8).toFloat()
        setColor(Color.parseColor("#0A1E35"))
        setStroke(dpToPx(1), Color.parseColor("#38BDF8"))
      }
      layoutParams = LinearLayout.LayoutParams(0, dpToPx(38), 0.8f)
      setOnClickListener { openApp() }
    }

    layoutActions?.addView(btnCardReject)
    layoutActions?.addView(btnCardAccept)
    layoutActions?.addView(btnCardOpenApp)
    cardView?.addView(layoutActions)

    // Build Idle Cockpit Panel (Shown when no active ride)
    buildIdleCockpitPanel()
  }

  private fun buildIdleCockpitPanel() {
    layoutIdleCockpit = LinearLayout(this).apply {
      orientation = LinearLayout.VERTICAL
      visibility = View.GONE
      setPadding(0, dpToPx(6), 0, 0)
    }

    tvCockpitStats = TextView(this).apply {
      text = "Today: ₹1,840 • 11 Rides • 58% Accept"
      setTextColor(Color.parseColor("#94A3B8"))
      textSize = 11f
      setPadding(0, dpToPx(2), 0, dpToPx(6))
    }

    val buttonsRow = LinearLayout(this).apply {
      orientation = LinearLayout.HORIZONTAL
      layoutParams = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
    }

    btnTogglePause = Button(this).apply {
      text = "PAUSE"
      setTextColor(Color.WHITE)
      textSize = 11f
      background = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = dpToPx(8).toFloat()
        setColor(Color.parseColor("#1E293B"))
      }
      layoutParams = LinearLayout.LayoutParams(0, dpToPx(36), 1f).apply {
        marginEnd = dpToPx(6)
      }
      setOnClickListener {
        val manager = AutomationManager.getInstance(this@FloatingWidgetService)
        val current = manager.settings.value.state
        if (current == AutomationState.RUNNING) {
          manager.updateState(AutomationState.PAUSED)
        } else {
          manager.updateState(AutomationState.RUNNING)
        }
      }
    }

    btnQuickSimulate = Button(this).apply {
      text = "TEST RIDE"
      setTextColor(Color.parseColor("#00E5FF"))
      textSize = 11f
      background = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = dpToPx(8).toFloat()
        setColor(Color.parseColor("#0A2239"))
        setStroke(dpToPx(1), Color.parseColor("#00E5FF"))
      }
      layoutParams = LinearLayout.LayoutParams(0, dpToPx(36), 1f).apply {
        marginEnd = dpToPx(6)
      }
      setOnClickListener {
        AutomationManager.getInstance(this@FloatingWidgetService).triggerSimulationRide()
      }
    }

    val btnCockpitOpenApp = Button(this).apply {
      text = "DASHBOARD"
      setTextColor(Color.WHITE)
      textSize = 11f
      background = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = dpToPx(8).toFloat()
        setColor(Color.parseColor("#1E3A5F"))
      }
      layoutParams = LinearLayout.LayoutParams(0, dpToPx(36), 1.2f)
      setOnClickListener { openApp() }
    }

    buttonsRow.addView(btnTogglePause)
    buttonsRow.addView(btnQuickSimulate)
    buttonsRow.addView(btnCockpitOpenApp)

    layoutIdleCockpit?.addView(tvCockpitStats)
    layoutIdleCockpit?.addView(buttonsRow)
    cardView?.addView(layoutIdleCockpit)
  }

  private fun toggleExpandedState(expand: Boolean? = null) {
    isExpanded = expand ?: !isExpanded
    cardView?.visibility = if (isExpanded) View.VISIBLE else View.GONE
  }

  private fun openApp() {
    val intent = Intent(this, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
    }
    startActivity(intent)
  }

  private fun observeAutomationState() {
    val manager = AutomationManager.getInstance(this)

    // Observe Automation Settings
    serviceScope.launch {
      manager.settings.collectLatest { settings ->
        val (colorHex, statusText) = when (settings.state) {
          AutomationState.RUNNING -> Pair("#00E676", "RUNNING")
          AutomationState.PAUSED -> Pair("#FFB300", "MANUAL")
          AutomationState.STOPPED -> Pair("#FF5252", "STOPPED")
        }

        bubbleBorderDrawable?.setStroke(dpToPx(2), Color.parseColor(colorHex))
        tvBubbleStatus?.text = statusText
        tvBubbleStatus?.setTextColor(Color.parseColor(colorHex))
        btnTogglePause?.text = if (settings.state == AutomationState.RUNNING) "PAUSE" else "RESUME"
      }
    }

    // Observe Live Active Ride Request
    serviceScope.launch {
      manager.currentActiveRequest.collectLatest { request ->
        currentRequestId = request?.id
        if (request != null) {
          // A real or simulated ride request has appeared! Automatically expand card!
          updateRideCard(request)
          toggleExpandedState(true)
        } else {
          // No active ride request; switch to cockpit idle view
          showIdleCockpitView()
        }
      }
    }

    // Observe Reaction Countdown Timer
    serviceScope.launch {
      manager.pendingCountdownSeconds.collectLatest { secondsRemaining ->
        if (secondsRemaining != null && secondsRemaining > 0f) {
          layoutCountdown?.visibility = View.VISIBLE
          tvCountdownText?.text = String.format(Locale.US, "Auto-accepting in %.1fs...", secondsRemaining)
        } else {
          layoutCountdown?.visibility = View.GONE
        }
      }
    }

    // Observe Stats
    serviceScope.launch {
      manager.stats.collectLatest { stats ->
        tvCockpitStats?.text = "Today: ₹${stats.todayEarningsInr.toInt()} • ${stats.todayAcceptedRides} Accepted (${stats.acceptanceRatePercent}%)"
      }
    }
  }

  private fun updateRideCard(request: RideRequest) {
    tvCardPlatform?.text = "${request.platform.displayName.uppercase()} ${request.vehicleCategory.uppercase()}"
    tvCardVehicle?.text = "${request.pickupDistanceKm} km pickup"
    tvCardFare?.text = "₹${request.fare.toInt()}"

    val perKm = if (request.tripDistanceKm > 0) request.fare / request.tripDistanceKm else 0.0
    tvCardPerKm?.text = String.format(Locale.US, "  (₹%.1f / km)", perKm)

    tvCardPickup?.text = "📍 ${request.pickupLocation}"
    tvCardDrop?.text = "🏁 ${request.dropLocation} (${request.tripDistanceKm} km)"

    val (decisionText, decisionColor) = when (request.status) {
      RideStatus.ACCEPTED -> Pair("🟢 AUTO-ACCEPT: RULES MATCHED", "#00E676")
      RideStatus.REJECTED -> Pair("🔴 AUTO-REJECT: THRESHOLD EXCEEDED", "#FF5252")
      RideStatus.MANUAL_REVIEW -> Pair("🟡 MANUAL REVIEW REQUIRED", "#FFB300")
      else -> Pair("EVALUATING RIDE...", "#38BDF8")
    }

    tvCardDecision?.text = decisionText
    tvCardDecision?.setTextColor(Color.parseColor(decisionColor))
    tvCardReason?.text = request.decisionReason

    // Show ride content, hide idle cockpit
    tvCardFare?.visibility = View.VISIBLE
    tvCardPerKm?.visibility = View.VISIBLE
    tvCardPickup?.visibility = View.VISIBLE
    tvCardDrop?.visibility = View.VISIBLE
    tvCardDecision?.visibility = View.VISIBLE
    tvCardReason?.visibility = View.VISIBLE
    layoutActions?.visibility = View.VISIBLE
    layoutIdleCockpit?.visibility = View.GONE

    // Update bubble title
    tvBubbleTitle?.text = "₹${request.fare.toInt()} • ${request.platform.displayName.split(" ").first()}"
  }

  private fun showIdleCockpitView() {
    tvBubbleTitle?.text = "RidePilot"
    tvCardPlatform?.text = "RIDEPILOT COCKPIT"
    tvCardVehicle?.text = "Ready"

    tvCardFare?.visibility = View.GONE
    tvCardPerKm?.visibility = View.GONE
    tvCardPickup?.visibility = View.GONE
    tvCardDrop?.visibility = View.GONE
    tvCardDecision?.visibility = View.GONE
    tvCardReason?.visibility = View.GONE
    layoutActions?.visibility = View.GONE
    layoutCountdown?.visibility = View.GONE
    layoutIdleCockpit?.visibility = View.VISIBLE
  }

  private fun dpToPx(dp: Int): Int {
    return TypedValue.applyDimension(
      TypedValue.COMPLEX_UNIT_DIP,
      dp.toFloat(),
      resources.displayMetrics
    ).toInt()
  }

  override fun onDestroy() {
    super.onDestroy()
    serviceScope.cancel()
    AutomationManager.getInstance(this).setOverlayServiceActive(false)
    rootContainer?.let {
      try {
        windowManager?.removeView(it)
      } catch (_: Exception) {}
    }
  }
}
