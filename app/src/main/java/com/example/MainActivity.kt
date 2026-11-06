package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.automation.AutomationManager
import com.example.model.AutomationState
import androidx.compose.runtime.DisposableEffect
import com.example.ui.components.FloatingServiceButton
import com.example.ui.screens.FiltersScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.PlatformsScreen
import com.example.ui.screens.ProfileScreen
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.NeonGreenPrimary
import com.example.ui.theme.RidePilotTheme
import com.example.ui.theme.StatusManualYellow
import com.example.ui.theme.StatusRunningGreen
import com.example.ui.theme.StatusStoppedRed

enum class NavTab(val title: String, val icon: ImageVector) {
  HOME("Home", Icons.Default.DirectionsCar),
  APPS("Apps", Icons.Default.Apps),
  FILTERS("Filters", Icons.Default.Tune),
  HISTORY("History", Icons.Default.History),
  PROFILE("Profile", Icons.Default.Person)
}

class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    setContent {
      RidePilotTheme {
        var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
        var isDemoBypassed by remember { mutableStateOf(false) }

        DisposableEffect(Unit) {
          val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
          }
          Firebase.auth.addAuthStateListener(listener)
          onDispose {
            Firebase.auth.removeAuthStateListener(listener)
          }
        }

        if (currentUser == null && !isDemoBypassed) {
          LoginScreen(
            onAuthSuccess = {
              currentUser = Firebase.auth.currentUser
            },
            onExploreDemo = {
              isDemoBypassed = true
            }
          )
        } else {
          RidePilotApp(
            onSignOutRequested = {
              isDemoBypassed = false
              currentUser = null
            }
          )
        }
      }
    }
  }
}

@Composable
fun RidePilotApp(
  onSignOutRequested: () -> Unit = {}
) {
  val context = LocalContext.current
  val manager = remember { AutomationManager.getInstance(context) }

  val settings by manager.settings.collectAsStateWithLifecycle()
  val stats by manager.stats.collectAsStateWithLifecycle()
  val zones by manager.locationZones.collectAsStateWithLifecycle()
  val rides by manager.rideHistory.collectAsStateWithLifecycle()
  val currentRequest by manager.currentActiveRequest.collectAsStateWithLifecycle()
  val profile by manager.driverProfile.collectAsStateWithLifecycle()
  val isSubscribed by manager.isSubscribed.collectAsStateWithLifecycle()
  val activePlan by manager.activePlan.collectAsStateWithLifecycle()
  val paymentHistory by manager.paymentHistory.collectAsStateWithLifecycle()
  val isAccessibilityActive by manager.isAccessibilityServiceConnected.collectAsStateWithLifecycle()
  val countdownSeconds by manager.pendingCountdownSeconds.collectAsStateWithLifecycle()
  val isOverlayActive by manager.isOverlayServiceActive.collectAsStateWithLifecycle()

  var currentTab by remember { mutableStateOf(NavTab.HOME) }

  // Handle system back button: return to Home if on secondary tab
  BackHandler(enabled = currentTab != NavTab.HOME) {
    currentTab = NavTab.HOME
  }

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    topBar = {
      TopAppBarHeader(
        settingsState = settings.state,
        onQuickSimulate = { manager.triggerSimulationRide() }
      )
    },
    bottomBar = {
      BottomNavBar(
        currentTab = currentTab,
        onTabSelect = { currentTab = it }
      )
    },
    floatingActionButton = {
      FloatingServiceButton(
        automationState = settings.state,
        isOverlayActive = isOverlayActive,
        onToggleService = {
          if (settings.state == AutomationState.RUNNING) {
            manager.updateState(AutomationState.STOPPED)
          } else {
            manager.updateState(AutomationState.RUNNING)
            if (manager.hasOverlayPermission() && !isOverlayActive) {
              manager.toggleFloatingWidget()
            }
          }
        },
        onToggleOverlay = {
          if (manager.hasOverlayPermission()) {
            manager.toggleFloatingWidget()
          } else {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
              val intent = android.content.Intent(
                android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                android.net.Uri.parse("package:${context.packageName}")
              )
              context.startActivity(intent)
            }
          }
        },
        onQuickSimulate = {
          manager.triggerSimulationRide()
        },
        onPauseService = {
          manager.updateState(AutomationState.PAUSED)
        }
      )
    },
    containerColor = MaterialTheme.colorScheme.background
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      when (currentTab) {
        NavTab.HOME -> HomeScreen(
          settings = settings,
          stats = stats,
          currentRequest = currentRequest,
          isAccessibilityActive = isAccessibilityActive,
          onStateChange = { manager.updateState(it) },
          onEmergencyStop = { manager.toggleEmergencyStop() },
          onSimulateRide = { platform -> manager.triggerSimulationRide(platform) },
          onManualAccept = { id -> manager.manualAcceptRide(id) },
          onManualReject = { id -> manager.manualRejectRide(id) },
          onPresetSelect = { manager.selectPreset(it) },
          onPlatformToggle = { platform, enabled -> manager.togglePlatform(platform, enabled) },
          onNavigateToFilters = { currentTab = NavTab.FILTERS },
          countdownRemainingSeconds = countdownSeconds,
          onAbortAutoAccept = { manager.abortPendingAutoAccept() },
          isOverlayActive = isOverlayActive,
          onToggleOverlay = {
            if (manager.hasOverlayPermission()) {
              manager.toggleFloatingWidget()
            } else {
              if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                val intent = android.content.Intent(
                  android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                  android.net.Uri.parse("package:${context.packageName}")
                )
                context.startActivity(intent)
              }
            }
          }
        )

        NavTab.APPS -> PlatformsScreen(
          settings = settings,
          onTogglePlatform = { platform, enabled -> manager.togglePlatform(platform, enabled) },
          onSimulatePlatform = { platform ->
            manager.triggerSimulationRide(platform)
            currentTab = NavTab.HOME
          }
        )

        NavTab.FILTERS -> FiltersScreen(
          settings = settings,
          zones = zones,
          onPresetSelect = { manager.selectPreset(it) },
          onUpdateFareFilters = { minF, maxF, minKm -> manager.updateFareFilters(minF, maxF, minKm) },
          onUpdateDistanceFilters = { maxP, minT, maxT -> manager.updateDistanceFilters(maxP, minT, maxT) },
          onUpdateTimingSettings = { delayS, jitter, showCd -> manager.updateTimingSettings(delayS, jitter, showCd) },
          onAddZone = { name, type, kw, rad, lat, lng, poly -> manager.addLocationZone(name, type, kw, rad, lat, lng, poly) },
          onToggleZone = { manager.toggleLocationZone(it) },
          onDeleteZone = { manager.deleteLocationZone(it) },
          onToggleVehicleCategory = { cat, selected -> manager.toggleVehicleCategory(cat, selected) },
          onSaveCriteriaToRoom = { minF, maxF, minKm, maxP, minT, maxT, vehs ->
            manager.updateRideCriteria(minF, maxF, minKm, maxP, minT, maxT, vehs)
          }
        )

        NavTab.HISTORY -> HistoryScreen(
          rides = rides
        )

        NavTab.PROFILE -> ProfileScreen(
          driverProfile = profile,
          settings = settings,
          isSubscribed = isSubscribed,
          activePlan = activePlan,
          paymentHistory = paymentHistory,
          isAccessibilityActive = isAccessibilityActive,
          onSubscribePlan = { manager.subscribePlan(it) },
          onRecordPayment = { plan, record -> manager.recordPayment(plan, record) },
          onUpdateVoiceSettings = { en, lang -> manager.updateVoiceSettings(en, lang) },
          onTestVoice = {
            manager.triggerSimulationRide()
            currentTab = NavTab.HOME
          },
          onSignOutRequested = onSignOutRequested
        )
      }
    }
  }
}

@Composable
fun TopAppBarHeader(
  settingsState: AutomationState,
  onQuickSimulate: () -> Unit
) {
  val (statusDotColor, statusLabel) = when (settingsState) {
    AutomationState.RUNNING -> Pair(StatusRunningGreen, "ACTIVE")
    AutomationState.PAUSED -> Pair(StatusManualYellow, "MANUAL")
    AutomationState.STOPPED -> Pair(StatusStoppedRed, "OFF")
  }

  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .windowInsetsPadding(WindowInsets.statusBars),
    color = MaterialTheme.colorScheme.background
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 10.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        // App emblem
        Box(
          modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(NeonGreenPrimary),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.DirectionsCar,
            contentDescription = null,
            tint = Color(0xFF041C0C),
            modifier = Modifier.size(22.dp)
          )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = "RidePilot",
              fontSize = 18.sp,
              fontWeight = FontWeight.Black,
              color = Color.White,
              letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF00391A))
                .padding(horizontal = 5.dp, vertical = 1.dp)
            ) {
              Text(
                text = "INDIA",
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                color = NeonGreenPrimary
              )
            }
          }
          Text(
            text = "Smart Auto Ride Acceptor",
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Row(verticalAlignment = Alignment.CenterVertically) {
        // Simulation Shortcut Pill
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0A2239))
            .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .clickable { onQuickSimulate() }
            .padding(horizontal = 8.dp, vertical = 5.dp)
            .testTag("quick_simulate_top_button"),
          contentAlignment = Alignment.Center
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.AutoAwesome,
              contentDescription = "Simulate",
              tint = Color(0xFF00E5FF),
              modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "TEST RIDE",
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFF00E5FF)
            )
          }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Status Indicator Pill
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurface)
            .border(1.dp, statusDotColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(statusDotColor)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
              text = statusLabel,
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              color = statusDotColor
            )
          }
        }
      }
    }
  }
}

@Composable
fun BottomNavBar(
  currentTab: NavTab,
  onTabSelect: (NavTab) -> Unit
) {
  NavigationBar(
    modifier = Modifier
      .fillMaxWidth()
      .windowInsetsPadding(WindowInsets.navigationBars),
    containerColor = DarkSurface,
    tonalElevation = 8.dp
  ) {
    NavTab.entries.forEach { tab ->
      val isSelected = currentTab == tab
      NavigationBarItem(
        selected = isSelected,
        onClick = { onTabSelect(tab) },
        icon = {
          Icon(
            imageVector = tab.icon,
            contentDescription = tab.title,
            modifier = Modifier.size(22.dp)
          )
        },
        label = {
          Text(
            text = tab.title,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
          )
        },
        colors = NavigationBarItemDefaults.colors(
          selectedIconColor = Color(0xFF041C0C),
          selectedTextColor = NeonGreenPrimary,
          indicatorColor = NeonGreenPrimary,
          unselectedIconColor = Color(0xFF94A3B8),
          unselectedTextColor = Color(0xFF94A3B8)
        ),
        modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
      )
    }
  }
}
