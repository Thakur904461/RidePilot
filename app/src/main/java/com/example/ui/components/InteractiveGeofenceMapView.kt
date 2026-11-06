package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Polyline
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.LocationZone
import com.example.model.ZoneType
import com.example.ui.theme.CoralReject
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.NeonGreenPrimary
import java.util.Locale

enum class MapBoundaryMode {
  RADIUS_CIRCLE,
  CUSTOM_POLYGON
}

/**
 * Interactive Geofence Map Visualizer:
 * Renders an interactive radar map canvas where drivers can:
 * 1. Visually see all active zones plotted relative to their center.
 * 2. Drag a center pin or tap on the map to define custom circular boundaries (with radius slider).
 * 3. Switch to Polygon mode and tap to define custom polygonal service boundaries.
 * 4. Save the visually defined geofence directly into Room database.
 */
@Composable
fun InteractiveGeofenceMapView(
  existingZones: List<LocationZone>,
  currentDriverLat: Double = 28.5562,
  currentDriverLng: Double = 77.1000,
  onSaveNewZone: (name: String, type: ZoneType, keywords: List<String>, radiusKm: Double, lat: Double, lng: Double, polygonPoints: List<Pair<Double, Double>>) -> Unit,
  modifier: Modifier = Modifier
) {
  var boundaryMode by remember { mutableStateOf(MapBoundaryMode.RADIUS_CIRCLE) }
  var centerLat by remember { mutableStateOf(currentDriverLat) }
  var centerLng by remember { mutableStateOf(currentDriverLng) }
  var radiusKm by remember { mutableFloatStateOf(6.0f) }

  // Canvas pan offset & scale
  var panOffset by remember { mutableStateOf(Offset.Zero) }
  var canvasScale by remember { mutableFloatStateOf(1.0f) }

  // Polygon vertices stored as offsets relative to canvas center
  val polygonPoints = remember { mutableStateListOf<Offset>() }

  var showSaveDialog by remember { mutableStateOf(false) }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(DarkSurface)
      .border(1.dp, Color(0xFF1E3A8A).copy(alpha = 0.6f), RoundedCornerShape(16.dp))
      .padding(14.dp)
  ) {
    // Header & Mode Controls
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
            .background(Color(0xFF0F2B48)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Layers,
            contentDescription = null,
            tint = Color(0xFF38BDF8),
            modifier = Modifier.size(18.dp)
          )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column {
          Text(
            text = "GEOFENCE MAP VISUALIZER",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF38BDF8),
            letterSpacing = 0.5.sp
          )
          Text(
            text = if (boundaryMode == MapBoundaryMode.RADIUS_CIRCLE) "Radius Circle Mode" else "Custom Polygon Boundary (${polygonPoints.size} vertices)",
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      // Mode Selector Pills
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (boundaryMode == MapBoundaryMode.RADIUS_CIRCLE) NeonGreenPrimary else DarkSurfaceElevated)
            .clickable { boundaryMode = MapBoundaryMode.RADIUS_CIRCLE }
            .padding(horizontal = 8.dp, vertical = 5.dp)
            .testTag("mode_radius_circle"),
          contentAlignment = Alignment.Center
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.RadioButtonChecked,
              contentDescription = null,
              tint = if (boundaryMode == MapBoundaryMode.RADIUS_CIRCLE) Color(0xFF041C0C) else Color.White,
              modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
              "Radius",
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              color = if (boundaryMode == MapBoundaryMode.RADIUS_CIRCLE) Color(0xFF041C0C) else Color.White
            )
          }
        }

        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (boundaryMode == MapBoundaryMode.CUSTOM_POLYGON) Color(0xFF00E5FF) else DarkSurfaceElevated)
            .clickable { boundaryMode = MapBoundaryMode.CUSTOM_POLYGON }
            .padding(horizontal = 8.dp, vertical = 5.dp)
            .testTag("mode_polygon"),
          contentAlignment = Alignment.Center
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Polyline,
              contentDescription = null,
              tint = if (boundaryMode == MapBoundaryMode.CUSTOM_POLYGON) Color(0xFF041C0C) else Color.White,
              modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
              "Polygon",
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              color = if (boundaryMode == MapBoundaryMode.CUSTOM_POLYGON) Color(0xFF041C0C) else Color.White
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Interactive Map Canvas Container
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(280.dp)
        .clip(RoundedCornerShape(12.dp))
        .background(Color(0xFF070F1E))
        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
        .pointerInput(boundaryMode) {
          detectTapGestures { tapOffset ->
            if (boundaryMode == MapBoundaryMode.CUSTOM_POLYGON) {
              polygonPoints.add(tapOffset)
            } else {
              // Convert tap offset to approximate delta lat/lng
              val centerX = size.width / 2f + panOffset.x
              val centerY = size.height / 2f + panOffset.y
              val dx = (tapOffset.x - centerX) / (size.width / 2f)
              val dy = (tapOffset.y - centerY) / (size.height / 2f)
              // 0.1 degree roughly corresponds to ~11 km in Delhi
              centerLat -= dy * 0.08
              centerLng += dx * 0.08
            }
          }
        }
        .pointerInput(Unit) {
          detectDragGestures { _, dragAmount ->
            panOffset = Offset(panOffset.x + dragAmount.x, panOffset.y + dragAmount.y)
          }
        }
        .testTag("interactive_geofence_canvas")
    ) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val center = Offset(width / 2f + panOffset.x, height / 2f + panOffset.y)

        // Draw dark futuristic radar grid lines
        val gridStep = 40f * canvasScale
        var x = (panOffset.x % gridStep)
        while (x < width) {
          drawLine(
            color = Color(0xFF11223A),
            start = Offset(x, 0f),
            end = Offset(x, height),
            strokeWidth = 1f
          )
          x += gridStep
        }
        var y = (panOffset.y % gridStep)
        while (y < height) {
          drawLine(
            color = Color(0xFF11223A),
            start = Offset(0f, y),
            end = Offset(width, y),
            strokeWidth = 1f
          )
          y += gridStep
        }

        // Concentric distance rings (5km, 10km, 15km, 20km)
        val kmToPxRatio = (width / 50f) * canvasScale
        listOf(5f, 10f, 15f, 20f).forEach { distKm ->
          val rPx = distKm * kmToPxRatio
          drawCircle(
            color = Color(0xFF1E3A5F).copy(alpha = 0.5f),
            radius = rPx,
            center = center,
            style = Stroke(width = 1f, cap = StrokeCap.Round)
          )
        }

        // Crosshairs
        drawLine(
          color = Color(0xFF38BDF8).copy(alpha = 0.3f),
          start = Offset(center.x - 20f, center.y),
          end = Offset(center.x + 20f, center.y),
          strokeWidth = 1.5f
        )
        drawLine(
          color = Color(0xFF38BDF8).copy(alpha = 0.3f),
          start = Offset(center.x, center.y - 20f),
          end = Offset(center.x, center.y + 20f),
          strokeWidth = 1.5f
        )

        // Render Existing Saved Zones
        existingZones.forEach { zone ->
          val zoneColor = when {
            zone.zoneType.isExclusion -> CoralReject
            zone.name.contains("Airport", ignoreCase = true) -> Color(0xFF00E5FF)
            else -> NeonGreenPrimary
          }
          // Offset relative to current center
          val dx = ((zone.lng - centerLng) / 0.08) * (width / 2f)
          val dy = -((zone.lat - centerLat) / 0.08) * (height / 2f)
          val zoneCenter = Offset(center.x + dx.toFloat(), center.y + dy.toFloat())
          val zoneRadiusPx = (zone.radiusKm.toFloat() * kmToPxRatio).coerceIn(10f, 300f)

          // Draw existing zone boundary circle
          drawCircle(
            color = zoneColor.copy(alpha = if (zone.isActive) 0.18f else 0.06f),
            radius = zoneRadiusPx,
            center = zoneCenter
          )
          drawCircle(
            color = zoneColor.copy(alpha = if (zone.isActive) 0.8f else 0.3f),
            radius = zoneRadiusPx,
            center = zoneCenter,
            style = Stroke(width = 1.5f)
          )
          // Draw zone center marker
          drawCircle(
            color = zoneColor,
            radius = 4f,
            center = zoneCenter
          )
        }

        // Render Active Interactive Geofence
        if (boundaryMode == MapBoundaryMode.RADIUS_CIRCLE) {
          val activeRadiusPx = radiusKm * kmToPxRatio

          // Semi-transparent fill
          drawCircle(
            color = NeonGreenPrimary.copy(alpha = 0.22f),
            radius = activeRadiusPx,
            center = center
          )
          // Glowing border
          drawCircle(
            color = NeonGreenPrimary,
            radius = activeRadiusPx,
            center = center,
            style = Stroke(width = 2.5f)
          )
          // Center GPS pin marker
          drawCircle(
            color = Color(0xFF041C0C),
            radius = 7f,
            center = center
          )
          drawCircle(
            color = NeonGreenPrimary,
            radius = 5f,
            center = center
          )
        } else {
          // CUSTOM POLYGON MODE: Connect vertices
          if (polygonPoints.isNotEmpty()) {
            val path = Path().apply {
              moveTo(polygonPoints.first().x, polygonPoints.first().y)
              for (i in 1 until polygonPoints.size) {
                lineTo(polygonPoints[i].x, polygonPoints[i].y)
              }
              if (polygonPoints.size >= 3) {
                close()
              }
            }

            if (polygonPoints.size >= 3) {
              drawPath(
                path = path,
                color = Color(0xFF00E5FF).copy(alpha = 0.25f)
              )
            }

            drawPath(
              path = path,
              color = Color(0xFF00E5FF),
              style = Stroke(width = 2.5f)
            )

            // Draw vertex handles
            drawPoints(
              points = polygonPoints,
              pointMode = PointMode.Points,
              color = Color.White,
              strokeWidth = 10f,
              cap = StrokeCap.Round
            )
          }
        }
      }

      // Compass & Presets HUD Overlay on Canvas
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
      ) {
        // Location Coordinates Pill
        Surface(
          shape = RoundedCornerShape(6.dp),
          color = Color(0xFF030A14).copy(alpha = 0.85f),
          border = BorderStroke(1.dp, Color(0xFF1E293B))
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(NeonGreenPrimary)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
              text = String.format(Locale.US, "%.4f° N, %.4f° E", centerLat, centerLng),
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              color = Color.White
            )
          }
        }

        // Quick Center Buttons
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          IconButton(
            onClick = {
              centerLat = currentDriverLat
              centerLng = currentDriverLng
              panOffset = Offset.Zero
            },
            modifier = Modifier
              .size(28.dp)
              .background(Color(0xFF0A1E35), RoundedCornerShape(6.dp))
          ) {
            Icon(
              imageVector = Icons.Default.MyLocation,
              contentDescription = "My Location",
              tint = NeonGreenPrimary,
              modifier = Modifier.size(16.dp)
            )
          }

          IconButton(
            onClick = {
              panOffset = Offset.Zero
              canvasScale = 1.0f
            },
            modifier = Modifier
              .size(28.dp)
              .background(Color(0xFF0A1E35), RoundedCornerShape(6.dp))
          ) {
            Icon(
              imageVector = Icons.Default.CenterFocusStrong,
              contentDescription = "Recenter",
              tint = Color(0xFF38BDF8),
              modifier = Modifier.size(16.dp)
            )
          }
        }
      }

      // Canvas Instruction Badge at Bottom
      Box(
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .padding(8.dp)
          .clip(RoundedCornerShape(6.dp))
          .background(Color(0xFF030A14).copy(alpha = 0.85f))
          .padding(horizontal = 8.dp, vertical = 3.dp)
      ) {
        Text(
          text = if (boundaryMode == MapBoundaryMode.RADIUS_CIRCLE)
            "Tap map to relocate center • Drag slider below to adjust radius"
          else
            "Tap map to place polygon boundary points (need ≥3)",
          fontSize = 9.sp,
          color = Color(0xFF94A3B8)
        )
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Dynamic Controls depending on mode
    if (boundaryMode == MapBoundaryMode.RADIUS_CIRCLE) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Radius: ${radiusKm.toInt()} km (${(radiusKm * 2).toInt()} km diameter)",
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          color = Color.White
        )
        Text(
          text = "Est. Area: ${(3.14159 * radiusKm * radiusKm).toInt()} km²",
          fontSize = 10.sp,
          color = NeonGreenPrimary
        )
      }

      Slider(
        value = radiusKm,
        onValueChange = { radiusKm = it },
        valueRange = 2f..25f,
        steps = 22,
        modifier = Modifier.fillMaxWidth()
      )
    } else {
      // Polygon Mode Tools: Undo / Clear / Points counter
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "${polygonPoints.size} Vertices Placed ${if (polygonPoints.size >= 3) "• Boundary Closed" else "(Min 3 needed)"}",
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          color = if (polygonPoints.size >= 3) Color(0xFF00E5FF) else Color(0xFF94A3B8)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          TextButton(
            onClick = {
              if (polygonPoints.isNotEmpty()) polygonPoints.removeAt(polygonPoints.size - 1)
            },
            enabled = polygonPoints.isNotEmpty()
          ) {
            Icon(Icons.Default.Undo, contentDescription = "Undo", modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(3.dp))
            Text("Undo", fontSize = 11.sp)
          }

          TextButton(
            onClick = { polygonPoints.clear() },
            enabled = polygonPoints.isNotEmpty()
          ) {
            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear", modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(3.dp))
            Text("Clear", fontSize = 11.sp)
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Action Button: Save this Geofence Zone
    Button(
      onClick = { showSaveDialog = true },
      enabled = boundaryMode == MapBoundaryMode.RADIUS_CIRCLE || polygonPoints.size >= 3,
      colors = ButtonDefaults.buttonColors(
        containerColor = if (boundaryMode == MapBoundaryMode.RADIUS_CIRCLE) NeonGreenPrimary else Color(0xFF00E5FF),
        contentColor = Color(0xFF041C0C)
      ),
      shape = RoundedCornerShape(10.dp),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("save_visual_geofence_button")
    ) {
      Icon(
        imageVector = Icons.Default.AddLocation,
        contentDescription = null,
        modifier = Modifier.size(18.dp)
      )
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = if (boundaryMode == MapBoundaryMode.RADIUS_CIRCLE)
          "SAVE CIRCLE GEOFENCE (${radiusKm.toInt()} KM)"
        else
          "SAVE CUSTOM POLYGON ZONE",
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp
      )
    }
  }

  // Save Modal Dialog
  if (showSaveDialog) {
    var zoneName by remember { mutableStateOf("") }
    var zoneType by remember { mutableStateOf(ZoneType.PREFERRED_DROP) }
    var keywords by remember { mutableStateOf("") }

    AlertDialog(
      onDismissRequest = { showSaveDialog = false },
      title = { Text("Save Geofenced Zone", fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = zoneName,
            onValueChange = { zoneName = it },
            label = { Text("Zone Name (e.g. West Delhi Sector)") },
            modifier = Modifier.fillMaxWidth()
          )

          Text("Zone Type:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
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
            placeholder = { Text("Sector, Metro, Toll, Highway") },
            modifier = Modifier.fillMaxWidth()
          )

          Text(
            text = if (boundaryMode == MapBoundaryMode.RADIUS_CIRCLE)
              "Center: ${String.format(Locale.US, "%.4f", centerLat)}, ${String.format(Locale.US, "%.4f", centerLng)} • Radius: ${radiusKm.toInt()} km"
            else
              "Polygon boundary with ${polygonPoints.size} custom vertices saved.",
            fontSize = 11.sp,
            color = Color(0xFF94A3B8)
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (zoneName.isNotBlank()) {
              val kwList = keywords.split(",").map { it.trim() }.filter { it.isNotBlank() }
              // Convert polygon offset points to approximate relative coordinates
              val pointsGeo = polygonPoints.map { pt ->
                val dLat = -(pt.y - 140f) / 140f * 0.08
                val dLng = (pt.x - 175f) / 175f * 0.08
                Pair(centerLat + dLat, centerLng + dLng)
              }

              onSaveNewZone(
                zoneName,
                zoneType,
                if (kwList.isEmpty()) listOf(zoneName) else kwList,
                radiusKm.toDouble(),
                centerLat,
                centerLng,
                pointsGeo
              )
              showSaveDialog = false
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = NeonGreenPrimary)
        ) {
          Text("Save", color = Color(0xFF041C0C), fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showSaveDialog = false }) {
          Text("Cancel", color = Color(0xFF94A3B8))
        }
      }
    )
  }
}

private fun BorderStroke(width: androidx.compose.ui.unit.Dp, color: Color) =
  androidx.compose.foundation.BorderStroke(width, color)
