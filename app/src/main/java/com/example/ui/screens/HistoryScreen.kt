package com.example.ui.screens

import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.RidePlatform
import com.example.model.RideRequest
import com.example.model.RideStatus
import com.example.ui.theme.CoralReject
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.NeonGreenPrimary
import com.example.ui.theme.StatusManualYellow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
  rides: List<RideRequest>,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var searchQuery by remember { mutableStateOf("") }
  var selectedStatusFilter by remember { mutableStateOf<RideStatus?>(null) }
  var selectedPlatformFilter by remember { mutableStateOf<RidePlatform?>(null) }

  val filteredRides = rides.filter { req ->
    val matchesSearch = searchQuery.isBlank() ||
      req.pickupLocation.contains(searchQuery, ignoreCase = true) ||
      req.dropLocation.contains(searchQuery, ignoreCase = true) ||
      req.platform.displayName.contains(searchQuery, ignoreCase = true)

    val matchesStatus = selectedStatusFilter == null || req.status == selectedStatusFilter
    val matchesPlatform = selectedPlatformFilter == null || req.platform == selectedPlatformFilter

    matchesSearch && matchesStatus && matchesPlatform
  }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    item {
      Spacer(modifier = Modifier.height(6.dp))

      // Header and CSV Export Button
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "RIDE AUDIT LOG",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            text = "${rides.size} total requests evaluated",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
          )
        }

        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurfaceElevated)
            .border(1.dp, NeonGreenPrimary.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .clickable { exportHistoryCsv(context, rides) }
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag("export_csv_button"),
          contentAlignment = Alignment.Center
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.FileDownload,
              contentDescription = "Export CSV",
              tint = NeonGreenPrimary,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "EXPORT CSV",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = NeonGreenPrimary
            )
          }
        }
      }
    }

    // Search bar
    item {
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = { Text("Search location, terminal, station...", fontSize = 13.sp) },
        leadingIcon = {
          Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Color(0xFF94A3B8))
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedContainerColor = DarkSurfaceElevated,
          unfocusedContainerColor = DarkSurfaceElevated,
          focusedBorderColor = NeonGreenPrimary,
          unfocusedBorderColor = Color(0xFF22335C)
        ),
        singleLine = true
      )
    }

    // Filter Chips Row
    item {
      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
          FilterChipItem(
            label = "All Status",
            isSelected = selectedStatusFilter == null,
            onClick = { selectedStatusFilter = null }
          )
        }
        item {
          FilterChipItem(
            label = "Accepted",
            isSelected = selectedStatusFilter == RideStatus.ACCEPTED,
            activeColor = NeonGreenPrimary,
            onClick = { selectedStatusFilter = RideStatus.ACCEPTED }
          )
        }
        item {
          FilterChipItem(
            label = "Rejected / Skipped",
            isSelected = selectedStatusFilter == RideStatus.REJECTED,
            activeColor = CoralReject,
            onClick = { selectedStatusFilter = RideStatus.REJECTED }
          )
        }
        item {
          FilterChipItem(
            label = "Manual Review",
            isSelected = selectedStatusFilter == RideStatus.MANUAL_REVIEW,
            activeColor = StatusManualYellow,
            onClick = { selectedStatusFilter = RideStatus.MANUAL_REVIEW }
          )
        }
      }
    }

    // Ride History Cards
    if (filteredRides.isEmpty()) {
      item {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
              imageVector = Icons.Default.History,
              contentDescription = null,
              tint = Color(0xFF64748B),
              modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "No ride records match your filter",
              fontSize = 14.sp,
              color = Color(0xFF94A3B8)
            )
          }
        }
      }
    } else {
      items(filteredRides) { request ->
        HistoryRideCard(request = request)
      }
    }

    item {
      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}

@Composable
private fun HistoryRideCard(request: RideRequest) {
  val dateFormat = remember { SimpleDateFormat("hh:mm a, dd MMM", Locale.ENGLISH) }
  val formattedTime = remember(request.timestamp) { dateFormat.format(Date(request.timestamp)) }

  val (statusBg, statusText, statusColor) = when (request.status) {
    RideStatus.ACCEPTED -> Triple(Color(0xFF00391A), "ACCEPTED", NeonGreenPrimary)
    RideStatus.REJECTED -> Triple(Color(0xFF3B0B0B), "REJECTED", CoralReject)
    RideStatus.MANUAL_REVIEW -> Triple(Color(0xFF3B2900), "MANUAL", StatusManualYellow)
    else -> Triple(Color(0xFF1E293B), "EXPIRED", Color(0xFF94A3B8))
  }

  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
    color = MaterialTheme.colorScheme.surfaceVariant,
    tonalElevation = 2.dp
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      // Top platform + time + status
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
              .background(request.platform.brandColor)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = request.platform.displayName,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = formattedTime,
            fontSize = 11.sp,
            color = Color(0xFF94A3B8)
          )
        }

        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(statusBg)
            .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
          Text(
            text = statusText,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = statusColor
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Fare and Distance Details
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = "₹${request.fare.toInt()}",
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            color = NeonGreenPrimary
          )
          Spacer(modifier = Modifier.width(8.dp))
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(4.dp))
              .background(Color(0xFF003049))
              .padding(horizontal = 6.dp, vertical = 2.dp)
          ) {
            Text(
              text = "₹${String.format("%.1f", request.farePerKm)}/km",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFF00E5FF)
            )
          }
        }

        Text(
          text = "Pickup: ${String.format("%.1f", request.pickupDistanceKm)} km | Trip: ${String.format("%.1f", request.tripDistanceKm)} km",
          fontSize = 11.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Locations
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(8.dp))
          .background(DarkSurface)
          .padding(8.dp)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Navigation,
            contentDescription = null,
            tint = NeonGreenPrimary,
            modifier = Modifier.size(12.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = request.pickupLocation,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
          )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = null,
            tint = CoralReject,
            modifier = Modifier.size(12.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = request.dropLocation,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
          )
        }
      }

      // Reason breakdown
      if (request.decisionReason.isNotEmpty()) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = request.decisionReason,
          fontSize = 10.sp,
          color = Color(0xFF94A3B8),
          lineHeight = 14.sp
        )
      }
    }
  }
}

@Composable
private fun FilterChipItem(
  label: String,
  isSelected: Boolean,
  activeColor: Color = NeonGreenPrimary,
  onClick: () -> Unit
) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(20.dp))
      .background(if (isSelected) activeColor else DarkSurfaceElevated)
      .border(1.dp, if (isSelected) activeColor else Color(0xFF22335C), RoundedCornerShape(20.dp))
      .clickable { onClick() }
      .padding(horizontal = 12.dp, vertical = 6.dp),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = label,
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold,
      color = if (isSelected) Color(0xFF041C0C) else Color.White
    )
  }
}

private fun exportHistoryCsv(context: Context, rides: List<RideRequest>) {
  val sb = StringBuilder()
  sb.append("Ride_ID,Platform,Time,Fare_INR,Pickup_KM,Trip_KM,Fare_Per_KM,Pickup_Location,Drop_Location,Status,Reason\n")
  val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
  for (r in rides) {
    sb.append("\"${r.id}\",")
    sb.append("\"${r.platform.displayName}\",")
    sb.append("\"${sdf.format(Date(r.timestamp))}\",")
    sb.append("${r.fare},")
    sb.append("${r.pickupDistanceKm},")
    sb.append("${r.tripDistanceKm},")
    sb.append("${String.format(Locale.ENGLISH, "%.2f", r.farePerKm)},")
    sb.append("\"${r.pickupLocation.replace("\"", "\"\"")}\",")
    sb.append("\"${r.dropLocation.replace("\"", "\"\"")}\",")
    sb.append("\"${r.status}\",")
    sb.append("\"${r.decisionReason.replace("\"", "\"\"")}\"\n")
  }

  val sendIntent = Intent(Intent.ACTION_SEND).apply {
    type = "text/csv"
    putExtra(Intent.EXTRA_SUBJECT, "RidePilot_Earnings_History_${System.currentTimeMillis()}.csv")
    putExtra(Intent.EXTRA_TEXT, sb.toString())
  }
  val chooser = Intent.createChooser(sendIntent, "Export Ride History CSV")
  context.startActivity(chooser)
}
