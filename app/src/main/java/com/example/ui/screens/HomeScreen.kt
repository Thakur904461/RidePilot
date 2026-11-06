package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.automation.AutomationStats
import com.example.model.AutomationSettings
import com.example.model.AutomationState
import com.example.model.PresetMode
import com.example.model.RidePlatform
import com.example.model.RideRequest
import com.example.ui.components.ActiveRideAlertCard
import com.example.ui.components.MetricCard
import com.example.ui.components.StatusHeader
import com.example.ui.theme.CoralReject
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.NeonGreenPrimary

@Composable
fun HomeScreen(
  settings: AutomationSettings,
  stats: AutomationStats,
  currentRequest: RideRequest?,
  isAccessibilityActive: Boolean,
  onStateChange: (AutomationState) -> Unit,
  onEmergencyStop: () -> Unit,
  onSimulateRide: (RidePlatform) -> Unit,
  onManualAccept: (String) -> Unit,
  onManualReject: (String) -> Unit,
  onPresetSelect: (PresetMode) -> Unit,
  onPlatformToggle: (RidePlatform, Boolean) -> Unit,
  onNavigateToFilters: () -> Unit,
  countdownRemainingSeconds: Float? = null,
  onAbortAutoAccept: (() -> Unit)? = null,
  isOverlayActive: Boolean = false,
  onToggleOverlay: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    item {
      Spacer(modifier = Modifier.height(8.dp))
      // Cockpit Automation Status Header
      StatusHeader(
        settings = settings,
        isAccessibilityActive = isAccessibilityActive,
        onStateChange = onStateChange,
        onEmergencyStop = onEmergencyStop,
        isOverlayActive = isOverlayActive,
        onToggleOverlay = onToggleOverlay
      )
    }

    // Active Live / Simulated Ride Request Notification
    if (currentRequest != null) {
      item {
        ActiveRideAlertCard(
          request = currentRequest,
          onManualAccept = onManualAccept,
          onManualReject = onManualReject,
          countdownRemainingSeconds = countdownRemainingSeconds,
          onAbortAutoAccept = onAbortAutoAccept
        )
      }
    }

    // Interactive Simulation Bar (Test Acceptance Engine Instantly)
    item {
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(16.dp))
          .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
        color = Color(0xFF071B2D)
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = Color(0xFF00E5FF),
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "TEST SIMULATION SANDBOX",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00E5FF)
              )
            }
            Text(
              text = "Tap to trigger",
              fontSize = 11.sp,
              color = Color(0xFF7DD3FC)
            )
          }

          Spacer(modifier = Modifier.height(10.dp))
          Text(
            text = "Simulate real Indian driver request popups to verify your auto-accept filters & voice alerts in real time.",
            fontSize = 11.sp,
            color = Color(0xFFBAE6FD),
            lineHeight = 15.sp
          )

          Spacer(modifier = Modifier.height(12.dp))

          // 4 platform simulator buttons
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            listOf(
              RidePlatform.OLA,
              RidePlatform.UBER,
              RidePlatform.RAPIDO,
              RidePlatform.PORTER
            ).forEach { platform ->
              Box(
                modifier = Modifier
                  .weight(1f)
                  .height(38.dp)
                  .clip(RoundedCornerShape(8.dp))
                  .background(DarkSurfaceElevated)
                  .border(1.dp, platform.brandColor.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                  .clickable { onSimulateRide(platform) }
                  .testTag("simulate_button_${platform.id}"),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = platform.displayName.split(" ").first(),
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color.White
                )
              }
            }
          }
        }
      }
    }

    // 4 Metrics Grid
    item {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          MetricCard(
            title = "Today's Earnings",
            value = "₹${stats.todayEarningsInr.toInt()}",
            subtitle = "+₹320 last hour",
            icon = Icons.Default.CurrencyRupee,
            accentColor = NeonGreenPrimary,
            modifier = Modifier.weight(1f)
          )
          MetricCard(
            title = "Accepted Rides",
            value = "${stats.todayAcceptedRides}",
            subtitle = "${stats.acceptanceRatePercent}% success rate",
            icon = Icons.Default.CheckCircle,
            accentColor = Color(0xFF00E5FF),
            modifier = Modifier.weight(1f)
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          MetricCard(
            title = "Avg Earning / Km",
            value = "₹${String.format("%.1f", stats.averageFarePerKm)}",
            subtitle = "Target: ≥₹${settings.minFarePerKm.toInt()}/km",
            icon = Icons.Default.Speed,
            accentColor = Color(0xFFFFB300),
            modifier = Modifier.weight(1f)
          )
          MetricCard(
            title = "Rejected / Skipped",
            value = "${stats.todayRejectedRides}",
            subtitle = "Saved ~${(stats.todayRejectedRides * 2.8).toInt()} deadhead km",
            icon = Icons.Default.Close,
            accentColor = CoralReject,
            modifier = Modifier.weight(1f)
          )
        }
      }
    }

    // Active Preset Card
    item {
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(16.dp))
          .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = null,
                tint = NeonGreenPrimary,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "ACTIVE PRESET: ${settings.activePreset.displayName.uppercase()}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
            }
            Text(
              text = "Edit Rules",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = NeonGreenPrimary,
              modifier = Modifier.clickable { onNavigateToFilters() }
            )
          }

          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = settings.activePreset.description,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          Spacer(modifier = Modifier.height(10.dp))

          // Filter Summary Chips
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            FilterSummaryChip(label = "Min ₹${settings.minFare.toInt()}", modifier = Modifier.weight(1f))
            FilterSummaryChip(label = "≥₹${settings.minFarePerKm.toInt()}/km", modifier = Modifier.weight(1f))
            FilterSummaryChip(label = "Pickup ≤${settings.maxPickupDistanceKm}km", modifier = Modifier.weight(1f))
          }
        }
      }
    }

    // Weekly Earnings Bar Visualization
    item {
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(16.dp))
          .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(
            text = "THIS WEEK'S EARNINGS",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "₹12,480 Total Gross",
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
          )

          Spacer(modifier = Modifier.height(14.dp))

          // Simple Custom Canvas-like Row Chart for Days of Week
          val weekData = listOf(
            Pair("Mon", 1420),
            Pair("Tue", 1680),
            Pair("Wed", 1540),
            Pair("Thu", 1920),
            Pair("Fri", 2150),
            Pair("Sat", 2430),
            Pair("Today", 1840)
          )
          val maxVal = 2600

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .height(110.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
          ) {
            weekData.forEach { (day, amount) ->
              val barRatio = (amount.toFloat() / maxVal).coerceIn(0.1f, 1f)
              val isToday = day == "Today"

              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.weight(1f)
              ) {
                Text(
                  text = "₹${(amount / 1000.0).toString().take(3)}k",
                  fontSize = 9.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (isToday) NeonGreenPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                  modifier = Modifier
                    .width(16.dp)
                    .height((80 * barRatio).dp)
                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                    .background(if (isToday) NeonGreenPrimary else Color(0xFF1E3A5F))
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                  text = day,
                  fontSize = 10.sp,
                  fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                  color = if (isToday) NeonGreenPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }
        }
      }
    }

    item {
      Spacer(modifier = Modifier.height(96.dp))
    }
  }
}

@Composable
private fun FilterSummaryChip(label: String, modifier: Modifier = Modifier) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(6.dp))
      .background(DarkSurfaceElevated)
      .padding(vertical = 5.dp),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = label,
      fontSize = 11.sp,
      fontWeight = FontWeight.Medium,
      color = Color.White
    )
  }
}
