package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AutomationSettings
import com.example.model.PlatformConfig
import com.example.model.RidePlatform
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.NeonGreenPrimary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlatformsScreen(
  settings: AutomationSettings,
  onTogglePlatform: (RidePlatform, Boolean) -> Unit,
  onSimulatePlatform: (RidePlatform) -> Unit,
  modifier: Modifier = Modifier
) {
  val platforms = listOf(
    RidePlatform.OLA,
    RidePlatform.UBER,
    RidePlatform.RAPIDO,
    RidePlatform.PORTER,
    RidePlatform.OTHER
  )

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    item {
      Spacer(modifier = Modifier.height(6.dp))
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(16.dp))
          .background(Color(0xFF0F1E36))
          .padding(14.dp)
      ) {
        Column {
          Text(
            text = "MULTI-PLATFORM COMPATIBILITY",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF38BDF8)
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "RidePilot operates natively alongside Indian driver applications through Android's Accessibility framework. Configure individual vehicle categories and fare rules per platform.",
            fontSize = 11.sp,
            color = Color(0xFF94A3B8),
            lineHeight = 15.sp
          )
        }
      }
    }

    items(platforms) { platform ->
      val config = settings.platformConfigs[platform] ?: PlatformConfig(platform = platform)
      PlatformIntegrationCard(
        platform = platform,
        config = config,
        onToggle = { isEnabled -> onTogglePlatform(platform, isEnabled) },
        onSimulate = { onSimulatePlatform(platform) }
      )
    }

    item {
      Spacer(modifier = Modifier.height(96.dp))
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlatformIntegrationCard(
  platform: RidePlatform,
  config: PlatformConfig,
  onToggle: (Boolean) -> Unit,
  onSimulate: () -> Unit
) {
  var isExpanded by remember { mutableStateOf(false) }

  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .border(
        1.dp,
        if (config.isEnabled) platform.brandColor.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
        RoundedCornerShape(16.dp)
      )
      .testTag("platform_card_${platform.id}"),
    color = MaterialTheme.colorScheme.surfaceVariant,
    tonalElevation = 2.dp
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(40.dp)
              .clip(RoundedCornerShape(10.dp))
              .background(platform.brandColor),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = platform.displayName.take(2).uppercase(),
              fontSize = 14.sp,
              fontWeight = FontWeight.Black,
              color = if (platform == RidePlatform.UBER) Color.White else Color.Black
            )
          }

          Spacer(modifier = Modifier.width(12.dp))

          Column {
            Text(
              text = platform.displayName,
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(6.dp)
                  .clip(CircleShape)
                  .background(if (config.isConnected) NeonGreenPrimary else Color(0xFF94A3B8))
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = if (config.isConnected) "Detection Ready" else "App Not Detected",
                fontSize = 11.sp,
                color = if (config.isConnected) NeonGreenPrimary else Color(0xFF94A3B8)
              )
            }
          }
        }

        // Toggle Switch
        Switch(
          checked = config.isEnabled,
          onCheckedChange = onToggle,
          colors = SwitchDefaults.colors(
            checkedThumbColor = Color(0xFF041C0C),
            checkedTrackColor = NeonGreenPrimary,
            uncheckedThumbColor = Color(0xFF94A3B8),
            uncheckedTrackColor = DarkSurfaceElevated
          ),
          modifier = Modifier.testTag("platform_toggle_${platform.id}")
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Supported Vehicle Categories
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        platform.defaultCategories.forEach { category ->
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .background(DarkSurface)
              .padding(horizontal = 8.dp, vertical = 4.dp)
          ) {
            Text(
              text = category,
              fontSize = 10.sp,
              fontWeight = FontWeight.Medium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Expand / Collapse Settings & Actions
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          modifier = Modifier.clickable { isExpanded = !isExpanded },
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = NeonGreenPrimary,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = if (isExpanded) "Hide Rules" else "Individual Rules",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = NeonGreenPrimary
          )
        }

        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(DarkSurfaceElevated)
            .clickable { onSimulate() }
            .padding(horizontal = 10.dp, vertical = 5.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "Test ${platform.displayName.split(" ").first()}",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
          )
        }
      }

      AnimatedVisibility(visible = isExpanded) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
        ) {
          Text(
            text = "Platform Minimum Fare: ₹${config.minFare.toInt()}",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "Platform Minimum ₹/Km: ₹${config.minPerKm.toInt()}/km",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "Max Allowed Pickup: ${config.maxPickupKm} km",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Package: ${platform.packageName}",
            fontSize = 10.sp,
            color = Color(0xFF64748B)
          )
        }
      }
    }
  }
}
