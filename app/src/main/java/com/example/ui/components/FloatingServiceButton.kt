package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AutomationState
import com.example.ui.theme.CoralReject
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.NeonGreenPrimary
import com.example.ui.theme.StatusManualYellow
import com.example.ui.theme.StatusRunningGreen
import com.example.ui.theme.StatusStoppedRed

/**
 * High-visibility Floating Button to start or stop the RidePilot automation service.
 * Supports quick 1-tap Start/Stop, expandable menu for floating screen overlay
 * and simulation shortcuts, with live pulsing beacon when the service is active.
 */
@Composable
fun FloatingServiceButton(
  automationState: AutomationState,
  isOverlayActive: Boolean,
  onToggleService: () -> Unit,
  onToggleOverlay: () -> Unit,
  onQuickSimulate: () -> Unit,
  onPauseService: () -> Unit,
  modifier: Modifier = Modifier
) {
  var isExpanded by remember { mutableStateOf(false) }

  val isRunning = automationState == AutomationState.RUNNING
  val isPaused = automationState == AutomationState.PAUSED

  // Pulsing animation for the active service beacon
  val infiniteTransition = rememberInfiniteTransition(label = "service_beacon_pulse")
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 1.0f,
    targetValue = 1.25f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 900, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "beacon_scale"
  )
  val pulseAlpha by infiniteTransition.animateFloat(
    initialValue = 0.8f,
    targetValue = 0.2f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 900, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "beacon_alpha"
  )

  // Dynamic colors based on active automation state
  val primaryBgColor by animateColorAsState(
    targetValue = when {
      isRunning -> CoralReject
      isPaused -> StatusManualYellow
      else -> NeonGreenPrimary
    },
    label = "fab_bg_color"
  )

  val contentColor = when {
    isRunning -> Color.White
    isPaused -> Color(0xFF1E1400)
    else -> Color(0xFF041C0C)
  }

  Column(
    horizontalAlignment = Alignment.End,
    verticalArrangement = Arrangement.spacedBy(10.dp),
    modifier = modifier.padding(bottom = 6.dp, end = 4.dp)
  ) {
    // Expandable Speed Dial Options
    AnimatedVisibility(
      visible = isExpanded,
      enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
      exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom)
    ) {
      Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        // Option 1: Toggle Floating Screen Overlay Service (Ola/Uber Bubble)
        FloatingSubActionButton(
          label = if (isOverlayActive) "Hide Screen Bubble" else "Show Screen Bubble",
          badge = if (isOverlayActive) "OVERLAY ON" else "OVERLAY OFF",
          icon = Icons.Default.Layers,
          accentColor = if (isOverlayActive) NeonGreenPrimary else Color(0xFF94A3B8),
          testTag = "floating_sub_toggle_overlay",
          onClick = {
            onToggleOverlay()
            isExpanded = false
          }
        )

        // Option 2: Pause Service (if running) or Resume
        if (isRunning) {
          FloatingSubActionButton(
            label = "Pause Service (Manual Mode)",
            badge = "PAUSE",
            icon = Icons.Default.Pause,
            accentColor = StatusManualYellow,
            testTag = "floating_sub_pause_service",
            onClick = {
              onPauseService()
              isExpanded = false
            }
          )
        }

        // Option 3: Quick Test Ride Simulation
        FloatingSubActionButton(
          label = "Test Ride Simulation",
          badge = "DEMO",
          icon = Icons.Default.AutoAwesome,
          accentColor = Color(0xFF00E5FF),
          testTag = "floating_sub_simulate_ride",
          onClick = {
            onQuickSimulate()
            isExpanded = false
          }
        )

        Spacer(modifier = Modifier.height(2.dp))
      }
    }

    // Main Floating Action Button Row
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      // Expand/Collapse toggle button
      Box(
        modifier = Modifier
          .size(42.dp)
          .shadow(elevation = 6.dp, shape = CircleShape)
          .clip(CircleShape)
          .background(DarkSurfaceElevated)
          .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
          .clickable { isExpanded = !isExpanded }
          .testTag("floating_service_expand_menu_button"),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
          contentDescription = if (isExpanded) "Collapse options" else "Expand service options",
          tint = Color.White,
          modifier = Modifier.size(24.dp)
        )
      }

      // Primary Start / Stop Service Button
      Surface(
        modifier = Modifier
          .shadow(
            elevation = if (isRunning) 12.dp else 8.dp,
            shape = RoundedCornerShape(28.dp),
            spotColor = primaryBgColor
          )
          .clip(RoundedCornerShape(28.dp))
          .clickable { onToggleService() }
          .testTag("floating_service_start_stop_button"),
        color = primaryBgColor,
        shape = RoundedCornerShape(28.dp)
      ) {
        Row(
          modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 14.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          // Dynamic Icon with Pulse beacon if running
          Box(contentAlignment = Alignment.Center) {
            if (isRunning) {
              Box(
                modifier = Modifier
                  .size(28.dp)
                  .scale(pulseScale)
                  .clip(CircleShape)
                  .background(Color.White.copy(alpha = pulseAlpha))
              )
            }
            Icon(
              imageVector = when {
                isRunning -> Icons.Default.Stop
                isPaused -> Icons.Default.PlayArrow
                else -> Icons.Default.PlayArrow
              },
              contentDescription = if (isRunning) "Stop Automation Service" else "Start Automation Service",
              tint = contentColor,
              modifier = Modifier.size(24.dp)
            )
          }

          Column {
            Text(
              text = when {
                isRunning -> "STOP SERVICE"
                isPaused -> "RESUME SERVICE"
                else -> "START SERVICE"
              },
              fontSize = 14.sp,
              fontWeight = FontWeight.Black,
              letterSpacing = 0.8.sp,
              color = contentColor
            )
            Text(
              text = when {
                isRunning -> "Auto-pilot active • Tap to halt"
                isPaused -> "Manual mode • Tap to run"
                else -> "Offline • Tap to start auto-accept"
              },
              fontSize = 10.sp,
              fontWeight = FontWeight.Medium,
              color = contentColor.copy(alpha = 0.85f)
            )
          }
        }
      }
    }
  }
}

/**
 * Sub-action button in the speed dial menu for the floating service button.
 */
@Composable
private fun FloatingSubActionButton(
  label: String,
  badge: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  accentColor: Color,
  testTag: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    modifier = modifier
      .shadow(elevation = 6.dp, shape = RoundedCornerShape(20.dp))
      .clip(RoundedCornerShape(20.dp))
      .clickable { onClick() }
      .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
      .testTag(testTag),
    color = DarkSurface,
    shape = RoundedCornerShape(20.dp)
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Box(
        modifier = Modifier
          .size(30.dp)
          .clip(CircleShape)
          .background(accentColor.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = accentColor,
          modifier = Modifier.size(16.dp)
        )
      }

      Column {
        Text(
          text = label,
          fontSize = 12.sp,
          fontWeight = FontWeight.SemiBold,
          color = Color.White
        )
        Text(
          text = badge,
          fontSize = 9.sp,
          fontWeight = FontWeight.Bold,
          color = accentColor,
          letterSpacing = 0.5.sp
        )
      }
    }
  }
}
