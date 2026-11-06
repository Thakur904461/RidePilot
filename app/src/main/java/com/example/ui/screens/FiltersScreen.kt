package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.example.model.LocationZone
import com.example.model.PresetMode
import com.example.model.ZoneType
import android.Manifest
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.example.location.GpsLocationProvider
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Timer
import com.example.ui.components.InteractiveGeofenceMapView
import com.example.ui.components.LocationPermissionSection
import com.example.ui.theme.CoralReject
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.NeonGreenPrimary
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class, ExperimentalPermissionsApi::class)
@Composable
fun FiltersScreen(
  settings: AutomationSettings,
  zones: List<LocationZone>,
  onPresetSelect: (PresetMode) -> Unit,
  onUpdateFareFilters: (Double, Double, Double) -> Unit,
  onUpdateDistanceFilters: (Double, Double, Double) -> Unit,
  onUpdateTimingSettings: (Float, Boolean, Boolean) -> Unit,
  onAddZone: (String, ZoneType, List<String>, Double, Double, Double, List<Pair<Double, Double>>) -> Unit,
  onToggleZone: (String) -> Unit,
  onDeleteZone: (String) -> Unit,
  onToggleVehicleCategory: ((String, Boolean) -> Unit)? = null,
  onSaveCriteriaToRoom: ((Double, Double, Double, Double, Double, Double, List<String>) -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  var minFare by remember(settings.minFare) { mutableFloatStateOf(settings.minFare.toFloat()) }
  var maxFare by remember(settings.maxFare) { mutableFloatStateOf(settings.maxFare.toFloat()) }
  var minPerKm by remember(settings.minFarePerKm) { mutableFloatStateOf(settings.minFarePerKm.toFloat()) }

  var maxPickup by remember(settings.maxPickupDistanceKm) { mutableFloatStateOf(settings.maxPickupDistanceKm.toFloat()) }
  var minTrip by remember(settings.minTripDistanceKm) { mutableFloatStateOf(settings.minTripDistanceKm.toFloat()) }
  var maxTrip by remember(settings.maxTripDistanceKm) { mutableFloatStateOf(settings.maxTripDistanceKm.toFloat()) }

  var selectedVehicles by remember(settings.preferredVehicleCategories) {
    mutableStateOf(settings.preferredVehicleCategories.toSet())
  }
  var showSavedRoomBanner by remember { mutableStateOf(false) }

  var reactionDelay by remember(settings.autoAcceptDelaySeconds) { mutableFloatStateOf(settings.autoAcceptDelaySeconds) }
  var jitterEnabled by remember(settings.humanizeJitterEnabled) { mutableStateOf(settings.humanizeJitterEnabled) }
  var showCountdown by remember(settings.showReactionCountdownOverlay) { mutableStateOf(settings.showReactionCountdownOverlay) }
  var showInteractiveMap by remember { mutableStateOf(true) }

  var showAddZoneDialog by remember { mutableStateOf(false) }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    item {
      Spacer(modifier = Modifier.height(6.dp))
      // Presets Selector Section
      Text(
        text = "DRIVER PRESET MODES",
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Spacer(modifier = Modifier.height(8.dp))

      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PresetMode.entries.forEach { preset ->
          val isSelected = settings.activePreset == preset
          Surface(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .border(
                1.5.dp,
                if (isSelected) NeonGreenPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                RoundedCornerShape(12.dp)
              )
              .clickable { onPresetSelect(preset) }
              .testTag("preset_${preset.name.lowercase()}"),
            color = if (isSelected) Color(0xFF072414) else MaterialTheme.colorScheme.surfaceVariant
          ) {
            Row(
              modifier = Modifier.padding(14.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(
                modifier = Modifier
                  .size(16.dp)
                  .clip(CircleShape)
                  .border(2.dp, if (isSelected) NeonGreenPrimary else Color(0xFF94A3B8), CircleShape)
                  .background(if (isSelected) NeonGreenPrimary else Color.Transparent)
              )
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = preset.displayName,
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (isSelected) NeonGreenPrimary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = preset.description,
                  fontSize = 11.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }
        }
      }
    }

    // Advanced Fare Filters Card
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
            text = "ADVANCED FARE LIMITS (INR ₹)",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )

          Spacer(modifier = Modifier.height(14.dp))

          // Minimum Fare
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("Minimum Fare", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("₹${minFare.toInt()}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NeonGreenPrimary)
          }
          Slider(
            value = minFare,
            onValueChange = {
              minFare = it
              onUpdateFareFilters(it.toDouble(), maxFare.toDouble(), minPerKm.toDouble())
            },
            valueRange = 50f..500f,
            steps = 9,
            colors = SliderDefaults.colors(thumbColor = NeonGreenPrimary, activeTrackColor = NeonGreenPrimary)
          )

          Spacer(modifier = Modifier.height(8.dp))

          // Minimum Per KM Earning
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("Minimum Earning / Km", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("₹${String.format("%.1f", minPerKm)}/km", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E5FF))
          }
          Slider(
            value = minPerKm,
            onValueChange = {
              minPerKm = it
              onUpdateFareFilters(minFare.toDouble(), maxFare.toDouble(), it.toDouble())
            },
            valueRange = 12f..35f,
            steps = 23,
            colors = SliderDefaults.colors(thumbColor = Color(0xFF00E5FF), activeTrackColor = Color(0xFF00E5FF))
          )

          Spacer(modifier = Modifier.height(8.dp))

          // Maximum Fare Cap
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("Maximum Fare Limit", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("₹${maxFare.toInt()}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
          }
          Slider(
            value = maxFare,
            onValueChange = {
              maxFare = it
              onUpdateFareFilters(minFare.toDouble(), it.toDouble(), minPerKm.toDouble())
            },
            valueRange = 500f..4000f,
            steps = 14,
            colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White)
          )
        }
      }
    }

    // Distance Filters Card
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
            text = "DISTANCE & PICKUP LIMITS",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )

          Spacer(modifier = Modifier.height(14.dp))

          // Max Pickup Distance
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("Maximum Pickup Distance", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${String.format("%.1f", maxPickup)} km", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NeonGreenPrimary)
          }
          Slider(
            value = maxPickup,
            onValueChange = {
              maxPickup = it
              onUpdateDistanceFilters(it.toDouble(), minTrip.toDouble(), maxTrip.toDouble())
            },
            valueRange = 0.8f..6.0f,
            steps = 26,
            colors = SliderDefaults.colors(thumbColor = NeonGreenPrimary, activeTrackColor = NeonGreenPrimary)
          )

          Spacer(modifier = Modifier.height(8.dp))

          // Max Trip Distance
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("Maximum Trip Distance", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${maxTrip.toInt()} km", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
          }
          Slider(
            value = maxTrip,
            onValueChange = {
              maxTrip = it
              onUpdateDistanceFilters(maxPickup.toDouble(), minTrip.toDouble(), it.toDouble())
            },
            valueRange = 5f..60f,
            steps = 11,
            colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White)
          )
        }
      }
    }

    // Preferred Vehicle Type Selection Card (Room Persistence)
    item {
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(16.dp))
          .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.DirectionsCar,
                contentDescription = null,
                tint = NeonGreenPrimary,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "PREFERRED VEHICLE TYPES",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
            }

            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF00391A))
                .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Text(
                text = "ROOM DB",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = NeonGreenPrimary
              )
            }
          }

          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Select vehicle categories to accept. Unchecked types will be rejected by the auto-pilot engine.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          Spacer(modifier = Modifier.height(14.dp))

          // Vehicle categories list
          val availableCategories = listOf(
            Pair("Bike Taxi", "Rapido Bike, Uber Moto, Ola Bike"),
            Pair("Auto Rickshaw", "Ola Auto, Uber Auto, Rapido Auto"),
            Pair("Economy Cab", "Uber Go, Ola Mini, WagonR"),
            Pair("Sedan Premier", "Ola Prime, Uber Premier, Dzire"),
            Pair("Parcel Delivery", "Porter 2W, Delivery Express"),
            Pair("Mini Truck / Tata Ace", "Porter 3W, Tata Ace, Pickup")
          )

          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            availableCategories.forEach { (categoryName, categoryDesc) ->
              val isSelected = selectedVehicles.contains(categoryName)
              Surface(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(10.dp))
                  .border(
                    1.dp,
                    if (isSelected) NeonGreenPrimary.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.1f),
                    RoundedCornerShape(10.dp)
                  )
                  .clickable {
                    val updated = if (isSelected) {
                      selectedVehicles - categoryName
                    } else {
                      selectedVehicles + categoryName
                    }
                    selectedVehicles = updated
                    onToggleVehicleCategory?.invoke(categoryName, !isSelected)
                  }
                  .testTag("vehicle_chip_${categoryName.lowercase().replace(" ", "_").replace("/", "_")}"),
                color = if (isSelected) Color(0xFF072414) else DarkSurface
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                      modifier = Modifier
                        .size(18.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .border(
                          1.5.dp,
                          if (isSelected) NeonGreenPrimary else Color(0xFF94A3B8),
                          RoundedCornerShape(4.dp)
                        )
                        .background(if (isSelected) NeonGreenPrimary else Color.Transparent),
                      contentAlignment = Alignment.Center
                    ) {
                      if (isSelected) {
                        Icon(
                          imageVector = Icons.Default.Check,
                          contentDescription = null,
                          tint = Color(0xFF041C0C),
                          modifier = Modifier.size(14.dp)
                        )
                      }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                      Text(
                        text = categoryName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) NeonGreenPrimary else Color.White
                      )
                      Text(
                        text = categoryDesc,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                      )
                    }
                  }

                  Text(
                    text = if (isSelected) "ACCEPTED" else "SKIP",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) NeonGreenPrimary else Color(0xFF94A3B8)
                  )
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Save All Criteria to Room Database Button
          Button(
            onClick = {
              onSaveCriteriaToRoom?.invoke(
                minFare.toDouble(),
                maxFare.toDouble(),
                minPerKm.toDouble(),
                maxPickup.toDouble(),
                minTrip.toDouble(),
                maxTrip.toDouble(),
                selectedVehicles.toList()
              )
              showSavedRoomBanner = true
            },
            colors = ButtonDefaults.buttonColors(
              containerColor = NeonGreenPrimary,
              contentColor = Color(0xFF041C0C)
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("save_criteria_to_room_button")
          ) {
            Icon(
              imageVector = Icons.Default.Save,
              contentDescription = null,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "SAVE CRITERIA TO ROOM DB",
              fontSize = 12.sp,
              fontWeight = FontWeight.Black,
              letterSpacing = 0.5.sp
            )
          }

          if (showSavedRoomBanner) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF00391A))
                .padding(horizontal = 10.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = NeonGreenPrimary,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Fare, distance & vehicle criteria saved to Room SQLite!",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = NeonGreenPrimary
              )
            }
          }
        }
      }
    }

    // Auto-Accept Timing & Delay Customization (Anti-Bot Protection)
    item {
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(16.dp))
          .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = null,
                tint = NeonGreenPrimary,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "AUTO-ACCEPT TIMING & DELAY",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
            }

            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF00391A))
                .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Text(
                text = "${String.format(Locale.US, "%.1f", reactionDelay)}s DELAY",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = NeonGreenPrimary
              )
            }
          }

          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "Humanized reaction delay prevents ride platforms (Ola, Uber, Rapido) from detecting robotic or instantaneous automated taps.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 15.sp
          )

          Spacer(modifier = Modifier.height(12.dp))

          // Reaction Delay Slider
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("Reaction Delay Window", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${String.format(Locale.US, "%.1f", reactionDelay)} seconds", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
          }

          Slider(
            value = reactionDelay,
            onValueChange = {
              reactionDelay = it
              onUpdateTimingSettings(it, jitterEnabled, showCountdown)
            },
            valueRange = 0.3f..4.0f,
            steps = 36,
            colors = SliderDefaults.colors(thumbColor = NeonGreenPrimary, activeTrackColor = NeonGreenPrimary)
          )

          // Quick Delay Preset Chips
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            listOf(
              Triple("Instant (0.3s)", 0.3f, false),
              Triple("Human (1.2s)", 1.2f, true),
              Triple("Cautious (2.5s)", 2.5f, true)
            ).forEach { (label, value, hasJitter) ->
              val isSelected = Math.abs(reactionDelay - value) < 0.15f
              Box(
                modifier = Modifier
                  .weight(1f)
                  .clip(RoundedCornerShape(8.dp))
                  .background(if (isSelected) NeonGreenPrimary else DarkSurfaceElevated)
                  .clickable {
                    reactionDelay = value
                    jitterEnabled = hasJitter
                    onUpdateTimingSettings(value, hasJitter, showCountdown)
                  }
                  .padding(vertical = 7.dp),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = label,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (isSelected) Color(0xFF041C0C) else Color.White
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Jitter toggle
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text("Anti-Bot Reaction Jitter (±0.25s)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
              Text("Adds natural human variance to acceptance timing", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
              checked = jitterEnabled,
              onCheckedChange = {
                jitterEnabled = it
                onUpdateTimingSettings(reactionDelay, it, showCountdown)
              },
              colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF041C0C), checkedTrackColor = NeonGreenPrimary)
            )
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Countdown overlay toggle
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text("Reaction Countdown & Abort Window", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
              Text("Allows driver to cancel auto-accept before the tap executes", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
              checked = showCountdown,
              onCheckedChange = {
                showCountdown = it
                onUpdateTimingSettings(reactionDelay, jitterEnabled, it)
              },
              colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF041C0C), checkedTrackColor = NeonGreenPrimary)
            )
          }
        }
      }
    }

    // Accompanist Location Permission Card for GPS Geofencing
    item {
      LocationPermissionSection()
    }

    // Interactive Map Visualizer Toggle Header
    item {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(8.dp))
          .background(Color(0xFF0F1E36))
          .clickable { showInteractiveMap = !showInteractiveMap }
          .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Map, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = if (showInteractiveMap) "HIDE INTERACTIVE MAP VISUALIZER" else "OPEN INTERACTIVE MAP VISUALIZER",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF38BDF8)
          )
        }
        Text(
          text = if (showInteractiveMap) "▲" else "▼",
          fontSize = 12.sp,
          color = Color(0xFF38BDF8)
        )
      }
    }

    if (showInteractiveMap) {
      item {
        InteractiveGeofenceMapView(
          existingZones = zones,
          onSaveNewZone = { name, type, kw, rad, lat, lng, poly ->
            onAddZone(name, type, kw, rad, lat, lng, poly)
          }
        )
      }
    }

    // Location Zones System Header & Add Button
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "LOCATION & GEOFENCE ZONES",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            text = "Prioritize airports/stations or exclude tollways",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
          )
        }

        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(NeonGreenPrimary)
            .clickable { showAddZoneDialog = true }
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag("add_zone_button"),
          contentAlignment = Alignment.Center
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Add,
              contentDescription = "Add Zone",
              tint = Color(0xFF041C0C),
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "ADD ZONE",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFF041C0C)
            )
          }
        }
      }
    }

    // List of Location Zones
    items(zones) { zone ->
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .border(
            1.dp,
            if (zone.zoneType.isExclusion) CoralReject.copy(alpha = 0.4f) else NeonGreenPrimary.copy(alpha = 0.4f),
            RoundedCornerShape(12.dp)
          ),
        color = MaterialTheme.colorScheme.surfaceVariant
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.LocationOn,
              contentDescription = null,
              tint = if (zone.zoneType.isExclusion) CoralReject else NeonGreenPrimary,
              modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = zone.name,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "${zone.zoneType.displayName} • Radius ${zone.radiusKm.toInt()} km",
                fontSize = 11.sp,
                color = if (zone.zoneType.isExclusion) CoralReject else NeonGreenPrimary
              )
              if (zone.lat != 0.0 && zone.lng != 0.0) {
                Text(
                  text = String.format(Locale.US, "GPS: %.4f° N, %.4f° E", zone.lat, zone.lng),
                  fontSize = 10.sp,
                  color = NeonGreenPrimary.copy(alpha = 0.85f)
                )
              }
              Text(
                text = "Keywords: ${zone.addressKeywords.joinToString(", ")}",
                fontSize = 10.sp,
                color = Color(0xFF94A3B8)
              )
            }
          }

          Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(
              checked = zone.isActive,
              onCheckedChange = { onToggleZone(zone.id) },
              colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF041C0C),
                checkedTrackColor = if (zone.zoneType.isExclusion) CoralReject else NeonGreenPrimary
              )
            )
            IconButton(onClick = { onDeleteZone(zone.id) }) {
              Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete Zone",
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(18.dp)
              )
            }
          }
        }
      }
    }

    item {
      Spacer(modifier = Modifier.height(16.dp))
    }
  }

  // Add Zone Modal Dialog
  if (showAddZoneDialog) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val gpsProvider = remember { GpsLocationProvider.getInstance(context) }
    val locationPermissionsState = rememberMultiplePermissionsState(
      listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    )

    var zoneName by remember { mutableStateOf("") }
    var zoneType by remember { mutableStateOf(ZoneType.PREFERRED_DROP) }
    var keywords by remember { mutableStateOf("") }
    var latText by remember { mutableStateOf("") }
    var lngText by remember { mutableStateOf("") }
    var isPinningGps by remember { mutableStateOf(false) }
    var radius by remember { mutableFloatStateOf(5.0f) }

    AlertDialog(
      onDismissRequest = { showAddZoneDialog = false },
      title = { Text("Add Location Zone", fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          // GPS Location Pinning Button using Accompanist Permissions
          Surface(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(8.dp))
              .border(
                1.dp,
                if (latText.isNotBlank()) NeonGreenPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                RoundedCornerShape(8.dp)
              )
              .clickable {
                if (locationPermissionsState.allPermissionsGranted) {
                  isPinningGps = true
                  coroutineScope.launch {
                    val loc = gpsProvider.fetchCurrentLocation()
                    latText = String.format(Locale.US, "%.5f", loc.latitude)
                    lngText = String.format(Locale.US, "%.5f", loc.longitude)
                    if (zoneName.isBlank()) {
                      zoneName = loc.localityName
                    }
                    if (keywords.isBlank()) {
                      keywords = loc.localityName.split("/").first().trim()
                    }
                    isPinningGps = false
                  }
                } else {
                  locationPermissionsState.launchMultiplePermissionRequest()
                }
              }
              .testTag("pin_current_gps_button"),
            color = if (latText.isNotBlank()) Color(0xFF00391A) else DarkSurfaceElevated
          ) {
            Row(
              modifier = Modifier.padding(10.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = "Pin GPS",
                tint = NeonGreenPrimary,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Column {
                Text(
                  text = if (latText.isNotBlank()) "GPS COORDINATES PINNED" else "USE CURRENT GPS LOCATION",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = NeonGreenPrimary
                )
                Text(
                  text = if (latText.isNotBlank())
                    "$latText° N, $lngText° E (Geofence: ${radius.toInt()} km)"
                  else if (locationPermissionsState.allPermissionsGranted)
                    "Tap to autofill coordinates from device GPS"
                  else
                    "Tap to grant location permission & detect GPS",
                  fontSize = 10.sp,
                  color = Color(0xFF94A3B8)
                )
              }
            }
          }

          OutlinedTextField(
            value = zoneName,
            onValueChange = { zoneName = it },
            label = { Text("Zone Name (e.g. Noida Toll Plaza)") },
            modifier = Modifier.fillMaxWidth()
          )

          Text("Zone Action:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (zoneType == ZoneType.PREFERRED_DROP) NeonGreenPrimary else DarkSurfaceElevated)
                .clickable { zoneType = ZoneType.PREFERRED_DROP }
                .padding(8.dp)
            ) {
              Text(
                "Preferred Drop",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (zoneType == ZoneType.PREFERRED_DROP) Color(0xFF041C0C) else Color.White
              )
            }

            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (zoneType == ZoneType.EXCLUDED_DROP) CoralReject else DarkSurfaceElevated)
                .clickable { zoneType = ZoneType.EXCLUDED_DROP }
                .padding(8.dp)
            ) {
              Text(
                "Avoid / Exclude",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
              )
            }
          }

          OutlinedTextField(
            value = keywords,
            onValueChange = { keywords = it },
            label = { Text("Keywords (comma separated)") },
            placeholder = { Text("Airport, T3, Aerocity") },
            modifier = Modifier.fillMaxWidth()
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedTextField(
              value = latText,
              onValueChange = { latText = it },
              label = { Text("Latitude") },
              placeholder = { Text("28.5562") },
              modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
              value = lngText,
              onValueChange = { lngText = it },
              label = { Text("Longitude") },
              placeholder = { Text("77.1000") },
              modifier = Modifier.weight(1f)
            )
          }

          Text("Geofence Radius: ${radius.toInt()} km", fontSize = 12.sp)
          Slider(
            value = radius,
            onValueChange = { radius = it },
            valueRange = 2f..15f,
            steps = 13
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (zoneName.isNotBlank()) {
              val list = keywords.split(",").map { it.trim() }.filter { it.isNotBlank() }
              val latVal = latText.toDoubleOrNull() ?: 0.0
              val lngVal = lngText.toDoubleOrNull() ?: 0.0
              onAddZone(
                zoneName,
                zoneType,
                if (list.isEmpty()) listOf(zoneName) else list,
                radius.toDouble(),
                latVal,
                lngVal,
                emptyList()
              )
              showAddZoneDialog = false
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = NeonGreenPrimary),
          modifier = Modifier.testTag("save_zone_confirm_button")
        ) {
          Text("Save Zone", color = Color(0xFF041C0C), fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showAddZoneDialog = false }) {
          Text("Cancel", color = Color(0xFF94A3B8))
        }
      }
    )
  }
}
