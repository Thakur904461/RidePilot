package com.example.ui.components

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.RideRequest
import com.example.model.RideStatus
import com.example.ui.theme.CoralReject
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.NeonGreenPrimary
import com.example.ui.theme.StatusManualYellow

@Composable
fun ActiveRideAlertCard(
  request: RideRequest?,
  onManualAccept: (String) -> Unit,
  onManualReject: (String) -> Unit,
  countdownRemainingSeconds: Float? = null,
  onAbortAutoAccept: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  AnimatedVisibility(
    visible = request != null,
    enter = fadeIn(),
    exit = fadeOut()
  ) {
    if (request == null) return@AnimatedVisibility

    val (badgeBg, badgeText, badgeColor) = when (request.status) {
      RideStatus.ACCEPTED -> Triple(Color(0xFF00391A), "AUTO-ACCEPTED", NeonGreenPrimary)
      RideStatus.REJECTED -> Triple(Color(0xFF3B0B0B), "AUTO-REJECTED", CoralReject)
      RideStatus.MANUAL_REVIEW -> Triple(Color(0xFF3B2900), "MANUAL REVIEW", StatusManualYellow)
      else -> Triple(Color(0xFF1E293B), "EVALUATING...", Color(0xFF38BDF8))
    }

    Surface(
      modifier = modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(20.dp))
        .border(2.dp, badgeColor, RoundedCornerShape(20.dp))
        .testTag("active_ride_card"),
      color = MaterialTheme.colorScheme.surfaceVariant,
      tonalElevation = 6.dp
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp)
      ) {
        // Platform Badge and Decision Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(request.platform.brandColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = request.platform.displayName.uppercase(),
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            if (request.isSimulated) {
              Spacer(modifier = Modifier.width(8.dp))
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(4.dp))
                  .background(Color(0xFF334155))
                  .padding(horizontal = 6.dp, vertical = 2.dp)
              ) {
                Text(
                  text = "SIMULATION",
                  fontSize = 9.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFF94A3B8)
                )
              }
            }
          }

          // Status Badge Pill
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .background(badgeBg)
              .border(1.dp, badgeColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
              .padding(horizontal = 10.dp, vertical = 4.dp)
          ) {
            Text(
              text = badgeText,
              fontSize = 11.sp,
              fontWeight = FontWeight.ExtraBold,
              color = badgeColor
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Fare and Earnings Highlights
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.Bottom
        ) {
          Column {
            Text(
              text = "₹${request.fare.toInt()}",
              fontSize = 32.sp,
              fontWeight = FontWeight.Black,
              color = NeonGreenPrimary
            )
            Text(
              text = "${request.vehicleCategory} • ${(request.confidence * 100).toInt()}% Confidence",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          Column(horizontalAlignment = Alignment.End) {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF003049))
                .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Default.Speed,
                  contentDescription = null,
                  tint = Color(0xFF00E5FF),
                  modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "₹${String.format("%.1f", request.farePerKm)}/km",
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFF00E5FF)
                )
              }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Pickup: ${String.format("%.1f", request.pickupDistanceKm)} km | Trip: ${String.format("%.1f", request.tripDistanceKm)} km",
              fontSize = 11.sp,
              fontWeight = FontWeight.Medium,
              color = MaterialTheme.colorScheme.onSurface
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Locations Section
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurface)
            .padding(10.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Navigation,
              contentDescription = "Pickup",
              tint = NeonGreenPrimary,
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = request.pickupLocation,
              fontSize = 12.sp,
              fontWeight = FontWeight.Medium,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              color = MaterialTheme.colorScheme.onSurface
            )
          }

          Spacer(modifier = Modifier.height(6.dp))

          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.LocationOn,
              contentDescription = "Drop",
              tint = CoralReject,
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = request.dropLocation,
              fontSize = 12.sp,
              fontWeight = FontWeight.Medium,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              color = MaterialTheme.colorScheme.onSurface
            )
          }
        }

        // Live Reaction Delay Countdown Bar
        if (request.status == RideStatus.ACCEPTED && countdownRemainingSeconds != null) {
          Spacer(modifier = Modifier.height(10.dp))
          Surface(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(8.dp)),
            color = Color(0xFF00391A),
            border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreenPrimary.copy(alpha = 0.5f))
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.CircularProgressIndicator(
                  modifier = Modifier.size(16.dp),
                  color = NeonGreenPrimary,
                  strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "Auto-Accepting in ${String.format(java.util.Locale.US, "%.1f", countdownRemainingSeconds)}s...",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = NeonGreenPrimary
                )
              }

              if (onAbortAutoAccept != null) {
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(CoralReject)
                    .clickable { onAbortAutoAccept() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .testTag("abort_auto_accept_button"),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = "ABORT",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                  )
                }
              }
            }
          }
        }

        // Decision Explanation Reason Box
        if (request.decisionReason.isNotEmpty()) {
          Spacer(modifier = Modifier.height(10.dp))
          Text(
            text = request.decisionReason,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 15.sp
          )
        }

        // Manual Override Action Buttons
        if (request.status == RideStatus.MANUAL_REVIEW || request.status == RideStatus.PENDING) {
          Spacer(modifier = Modifier.height(14.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Box(
              modifier = Modifier
                .weight(1f)
                .height(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF2E1010))
                .border(1.dp, CoralReject, RoundedCornerShape(10.dp))
                .clickable { onManualReject(request.id) }
                .testTag("manual_reject_button"),
              contentAlignment = Alignment.Center
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Default.Close,
                  contentDescription = "Reject",
                  tint = CoralReject,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "SKIP RIDE",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  color = CoralReject
                )
              }
            }

            Box(
              modifier = Modifier
                .weight(1f)
                .height(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(NeonGreenPrimary)
                .clickable { onManualAccept(request.id) }
                .testTag("manual_accept_button"),
              contentAlignment = Alignment.Center
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Default.CheckCircle,
                  contentDescription = "Accept",
                  tint = Color(0xFF041C0C),
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "ACCEPT NOW",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFF041C0C)
                )
              }
            }
          }
        }
      }
    }
  }
}
