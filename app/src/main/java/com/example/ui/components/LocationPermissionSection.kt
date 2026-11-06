package com.example.ui.components

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.location.GpsLocationData
import com.example.location.GpsLocationProvider
import com.example.ui.theme.CoralReject
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.NeonGreenPrimary
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Accompanist Permissions handler for Fine & Coarse Location.
 * Manages permission requests, rationales, and GPS telemetry for RidePilot's Location Zones feature.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LocationPermissionSection(
  modifier: Modifier = Modifier,
  onLocationUpdated: ((GpsLocationData) -> Unit)? = null
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val gpsProvider = remember { GpsLocationProvider.getInstance(context) }

  val locationPermissionsState: MultiplePermissionsState = rememberMultiplePermissionsState(
    permissions = listOf(
      Manifest.permission.ACCESS_FINE_LOCATION,
      Manifest.permission.ACCESS_COARSE_LOCATION
    )
  )

  var locationData by remember { mutableStateOf(GpsLocationData()) }
  var isFetchingLocation by remember { mutableStateOf(false) }

  fun refreshLocation() {
    if (locationPermissionsState.allPermissionsGranted) {
      isFetchingLocation = true
      coroutineScope.launch {
        val fetched = gpsProvider.fetchCurrentLocation()
        locationData = fetched
        isFetchingLocation = false
        onLocationUpdated?.invoke(fetched)
      }
    }
  }

  LaunchedEffect(locationPermissionsState.allPermissionsGranted) {
    if (locationPermissionsState.allPermissionsGranted) {
      refreshLocation()
    }
  }

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .border(
        width = 1.dp,
        color = if (locationPermissionsState.allPermissionsGranted) NeonGreenPrimary.copy(alpha = 0.5f) else CoralReject.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp)
      ),
    color = MaterialTheme.colorScheme.surfaceVariant
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(32.dp)
              .clip(CircleShape)
              .background(
                if (locationPermissionsState.allPermissionsGranted) Color(0xFF00391A) else Color(0xFF3B1214)
              ),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = if (locationPermissionsState.allPermissionsGranted) Icons.Default.GpsFixed else Icons.Default.GpsOff,
              contentDescription = "GPS Status",
              tint = if (locationPermissionsState.allPermissionsGranted) NeonGreenPrimary else CoralReject,
              modifier = Modifier.size(18.dp)
            )
          }

          Spacer(modifier = Modifier.width(10.dp))

          Column {
            Text(
              text = if (locationPermissionsState.allPermissionsGranted) "GPS TELEMETRY ACTIVE" else "GPS PERMISSION REQUIRED",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = if (locationPermissionsState.allPermissionsGranted) NeonGreenPrimary else CoralReject,
              letterSpacing = 0.5.sp
            )
            Text(
              text = if (locationPermissionsState.allPermissionsGranted)
                "Real-time geofence validation enabled"
              else
                "Accompanist Location Permission gate",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        if (locationPermissionsState.allPermissionsGranted) {
          IconButton(
            onClick = { refreshLocation() },
            enabled = !isFetchingLocation,
            modifier = Modifier
              .size(32.dp)
              .testTag("refresh_gps_button")
          ) {
            if (isFetchingLocation) {
              CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = NeonGreenPrimary,
                strokeWidth = 2.dp
              )
            } else {
              Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Refresh GPS",
                tint = NeonGreenPrimary,
                modifier = Modifier.size(18.dp)
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      if (locationPermissionsState.allPermissionsGranted) {
        // Location telemetry details
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(8.dp),
          color = Color(0xFF051B0F)
        ) {
          Column(modifier = Modifier.padding(10.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                  modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(NeonGreenPrimary)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = locationData.localityName,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color.White
                )
              }

              Text(
                text = "±${locationData.accuracyMeters.toInt()}m accuracy",
                fontSize = 10.sp,
                color = Color(0xFF94A3B8)
              )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = String.format(Locale.US, "Coordinates: %.4f° N, %.4f° E", locationData.latitude, locationData.longitude),
              fontSize = 11.sp,
              color = NeonGreenPrimary.copy(alpha = 0.9f)
            )
          }
        }
      } else {
        // Permission rationale or request actions
        Text(
          text = if (locationPermissionsState.shouldShowRationale) {
            "RidePilot requires GPS location access to automatically verify if incoming ride pickups or drop locations match your geofenced zones (Airports, Railway Stations, Tollways) and calculate accurate deadhead distances."
          } else {
            "Grant location permission so RidePilot can calculate accurate pickup distances and trigger auto-accept rules based on your current geofence boundary."
          },
          fontSize = 11.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          lineHeight = 16.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Button(
            onClick = {
              locationPermissionsState.launchMultiplePermissionRequest()
            },
            colors = ButtonDefaults.buttonColors(
              containerColor = NeonGreenPrimary,
              contentColor = Color(0xFF041C0C)
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
              .weight(1f)
              .testTag("request_location_permission_button")
          ) {
            Icon(
              imageVector = Icons.Default.LocationOn,
              contentDescription = null,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (locationPermissionsState.shouldShowRationale) "GRANT PERMISSION" else "ENABLE GPS ACCESS",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold
            )
          }

          if (locationPermissionsState.shouldShowRationale) {
            OutlinedButton(
              onClick = {
                val intent = Intent(
                  Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                  Uri.fromParts("package", context.packageName, null)
                )
                context.startActivity(intent)
              },
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.testTag("open_settings_button")
            ) {
              Icon(
                imageVector = Icons.Default.OpenInNew,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text("SETTINGS", fontSize = 11.sp)
            }
          }
        }
      }
    }
  }
}
