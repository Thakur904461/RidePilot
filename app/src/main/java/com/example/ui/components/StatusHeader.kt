package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AutomationSettings
import com.example.model.AutomationState
import com.example.ui.theme.CoralReject
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.NeonGreenPrimary
import com.example.ui.theme.StatusManualYellow
import com.example.ui.theme.StatusRunningGreen
import com.example.ui.theme.StatusStoppedRed

@Composable
fun StatusHeader(
  settings: AutomationSettings,
  isAccessibilityActive: Boolean,
  onStateChange: (AutomationState) -> Unit,
  onEmergencyStop: () -> Unit,
  isOverlayActive: Boolean = false,
  onToggleOverlay: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val (statusColor, statusTitle, statusSubtitle) = when (settings.state) {
    AutomationState.RUNNING -> Triple(
      StatusRunningGreen,
      "AUTO-ACCEPT RUNNING",
      "Scanning Ola, Uber, Rapido & Porter"
    )
    AutomationState.PAUSED -> Triple(
      StatusManualYellow,
      "MANUAL REVIEW MODE",
      "Rides will alert you without auto-clicking"
    )
    AutomationState.STOPPED -> Triple(
      StatusStoppedRed,
      "AUTOMATION STOPPED",
      "Acceptor is inactive. Tap Start to resume."
    )
  }

  val animatedBorder by animateColorAsState(targetValue = statusColor, label = "statusBorder")

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(20.dp))
      .border(1.5.dp, animatedBorder.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
    color = MaterialTheme.colorScheme.surfaceVariant,
    tonalElevation = 4.dp
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .background(
          Brush.verticalGradient(
            listOf(
              animatedBorder.copy(alpha = 0.12f),
              Color.Transparent
            )
          )
        )
        .padding(16.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          // Pulsing status dot
          Box(
            modifier = Modifier
              .size(14.dp)
              .clip(CircleShape)
              .background(statusColor)
              .border(2.dp, Color.White.copy(alpha = 0.8f), CircleShape)
          )
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = statusTitle,
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
              color = statusColor,
              letterSpacing = 0.5.sp
            )
            Text(
              text = statusSubtitle,
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        // Emergency Stop Button
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (settings.emergencyStopActive) CoralReject else DarkSurfaceElevated)
            .border(1.dp, CoralReject.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
            .clickable { onEmergencyStop() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag("emergency_stop_button"),
          contentAlignment = Alignment.Center
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Warning,
              contentDescription = "Emergency Stop",
              tint = if (settings.emergencyStopActive) Color.White else CoralReject,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = if (settings.emergencyStopActive) "HALTED" else "E-STOP",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = if (settings.emergencyStopActive) Color.White else CoralReject
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Control Action Buttons Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        // Start / Resume Button
        ControlButton(
          label = "START",
          icon = Icons.Default.PlayArrow,
          isSelected = settings.state == AutomationState.RUNNING,
          accentColor = StatusRunningGreen,
          modifier = Modifier.weight(1f),
          onClick = { onStateChange(AutomationState.RUNNING) }
        )

        // Pause Button
        ControlButton(
          label = "PAUSE",
          icon = Icons.Default.Pause,
          isSelected = settings.state == AutomationState.PAUSED,
          accentColor = StatusManualYellow,
          modifier = Modifier.weight(1f),
          onClick = { onStateChange(AutomationState.PAUSED) }
        )

        // Stop Button
        ControlButton(
          label = "STOP",
          icon = Icons.Default.Stop,
          isSelected = settings.state == AutomationState.STOPPED,
          accentColor = StatusStoppedRed,
          modifier = Modifier.weight(1f),
          onClick = { onStateChange(AutomationState.STOPPED) }
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Floating Cockpit Overlay Service Toggle
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(10.dp))
          .clickable { onToggleOverlay() }
          .border(
            1.dp,
            if (isOverlayActive) NeonGreenPrimary.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.15f),
            RoundedCornerShape(10.dp)
          )
          .testTag("floating_overlay_toggle_card"),
        color = if (isOverlayActive) Color(0xFF00391A) else DarkSurface
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Layers,
              contentDescription = "Floating Overlay",
              tint = if (isOverlayActive) NeonGreenPrimary else Color(0xFF94A3B8),
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                text = "FLOATING COCKPIT WIDGET",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isOverlayActive) NeonGreenPrimary else Color.White
              )
              Text(
                text = if (isOverlayActive)
                  "Active on top of Ola, Uber & Google Maps"
                else
                  "Tap to show floating bubble on top of other apps",
                fontSize = 10.sp,
                color = Color(0xFF94A3B8)
              )
            }
          }

          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .background(if (isOverlayActive) NeonGreenPrimary else DarkSurfaceElevated)
              .padding(horizontal = 8.dp, vertical = 4.dp)
          ) {
            Text(
              text = if (isOverlayActive) "ACTIVE" else "ENABLE",
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              color = if (isOverlayActive) Color(0xFF041C0C) else Color.White
            )
          }
        }
      }

      if (!isAccessibilityActive) {
        Spacer(modifier = Modifier.height(10.dp))
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF332000))
            .padding(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = StatusManualYellow,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Accessibility Service is not active. Enable in Settings to auto-click apps.",
            fontSize = 11.sp,
            color = StatusManualYellow
          )
        }
      }
    }
  }
}

@Composable
private fun ControlButton(
  label: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  isSelected: Boolean,
  accentColor: Color,
  modifier: Modifier = Modifier,
  onClick: () -> Unit
) {
  Box(
    modifier = modifier
      .height(42.dp)
      .clip(RoundedCornerShape(10.dp))
      .background(if (isSelected) accentColor else DarkSurface)
      .border(
        1.dp,
        if (isSelected) accentColor else Color.White.copy(alpha = 0.15f),
        RoundedCornerShape(10.dp)
      )
      .clickable { onClick() }
      .testTag("control_button_${label.lowercase()}"),
    contentAlignment = Alignment.Center
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = if (isSelected) Color(0xFF041C0C) else Color.White,
        modifier = Modifier.size(18.dp)
      )
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = label,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = if (isSelected) Color(0xFF041C0C) else Color.White
      )
    }
  }
}
