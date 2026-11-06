package com.example.ui.screens

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.auth.GoogleAuthService
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.NeonGreenPrimary
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
  onAuthSuccess: () -> Unit,
  onExploreDemo: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  Surface(
    modifier = modifier.fillMaxSize(),
    color = MaterialTheme.colorScheme.background
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            colors = listOf(
              Color(0xFF061826),
              MaterialTheme.colorScheme.background,
              Color(0xFF040B16)
            )
          )
        )
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 24.dp, vertical = 20.dp)
          .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Spacer(modifier = Modifier.height(28.dp))

          // Brand Logo Emblem with Radar Glow
          Box(
            modifier = Modifier
              .size(76.dp)
              .clip(RoundedCornerShape(20.dp))
              .background(
                Brush.radialGradient(
                  colors = listOf(NeonGreenPrimary, Color(0xFF00A844))
                )
              )
              .border(2.dp, Color(0xFF80FFBA), RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.DirectionsCar,
              contentDescription = "RidePilot Emblem",
              tint = Color(0xFF041C0C),
              modifier = Modifier.size(46.dp)
            )
          }

          Spacer(modifier = Modifier.height(18.dp))

          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = "RidePilot",
              fontSize = 32.sp,
              fontWeight = FontWeight.Black,
              color = Color.White,
              letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF00391A))
                .border(1.dp, NeonGreenPrimary.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
              Text(
                text = "INDIA",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = NeonGreenPrimary
              )
            }
          }

          Spacer(modifier = Modifier.height(6.dp))

          Text(
            text = "Smart Auto Ride Acceptor & Trip Filter",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF38BDF8),
            textAlign = TextAlign.Center
          )

          Text(
            text = "For Ola, Uber, Rapido, and Porter Drivers",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
          )

          Spacer(modifier = Modifier.height(28.dp))

          // Value Propositions Cards
          Surface(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(16.dp))
              .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
            color = DarkSurface
          ) {
            Column(
              modifier = Modifier.padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
              FeatureRow(
                icon = Icons.Default.Speed,
                iconColor = NeonGreenPrimary,
                title = "Smart Auto-Accept Engine",
                desc = "Accepts rides meeting your ₹/km and distance filters instantly."
              )
              FeatureRow(
                icon = Icons.Default.CloudSync,
                iconColor = Color(0xFF00E5FF),
                title = "Firebase Cloud Sync",
                desc = "Sync your earnings, presets, and location zones across devices."
              )
              FeatureRow(
                icon = Icons.Default.VolumeUp,
                iconColor = Color(0xFFFFB300),
                title = "Voice Alerts in Hindi & English",
                desc = "Speaks ride amounts and acceptance decisions while driving."
              )
              FeatureRow(
                icon = Icons.Default.Lock,
                iconColor = Color(0xFF818CF8),
                title = "Single Device Authorization",
                desc = "Secured binding protects your subscription and preferences."
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Actions & Sign-In Section
        Column(
          modifier = Modifier.fillMaxWidth(),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          // Error Message Display
          AnimatedVisibility(visible = errorMessage != null) {
            Surface(
              modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .clip(RoundedCornerShape(8.dp)),
              color = Color(0xFF3B0B0B)
            ) {
              Text(
                text = errorMessage ?: "",
                fontSize = 12.sp,
                color = Color(0xFFFF8080),
                modifier = Modifier.padding(10.dp),
                textAlign = TextAlign.Center
              )
            }
          }

          // Primary Google Sign-In Button
          Button(
            onClick = {
              val activity = context as? Activity
              if (activity != null) {
                isLoading = true
                errorMessage = null
                scope.launch {
                  GoogleAuthService.signInWithGoogle(
                    activity = activity,
                    onSuccess = {
                      isLoading = false
                      onAuthSuccess()
                    },
                    onError = { err ->
                      isLoading = false
                      errorMessage = err
                    },
                    onCancelled = {
                      isLoading = false
                    }
                  )
                }
              }
            },
            enabled = !isLoading,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = Color.White,
              contentColor = Color(0xFF1F1F1F)
            ),
            modifier = Modifier
              .fillMaxWidth()
              .height(52.dp)
              .testTag("google_sign_in_button")
          ) {
            if (isLoading) {
              CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = Color(0xFF041C0C),
                strokeWidth = 2.5.dp
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = "Connecting to Google...",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F1F1F)
              )
            } else {
              GoogleLogoIcon(modifier = Modifier.size(20.dp))
              Spacer(modifier = Modifier.width(12.dp))
              Text(
                text = "Sign in with Google",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F1F1F)
              )
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Explore Sandbox Demo Button
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(46.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(DarkSurfaceElevated)
              .border(1.dp, Color(0xFF22335C), RoundedCornerShape(12.dp))
              .clickable { onExploreDemo() }
              .testTag("explore_demo_button"),
            contentAlignment = Alignment.Center
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = Color(0xFF00E5FF),
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Explore Sandbox / Offline Mode",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE2E8F0)
              )
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          // Security & Compliance Footer
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
          ) {
            Icon(
              imageVector = Icons.Default.Security,
              contentDescription = null,
              tint = Color(0xFF64748B),
              modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Secured with Firebase Auth & Jetpack Credential Manager",
              fontSize = 10.sp,
              color = Color(0xFF64748B),
              textAlign = TextAlign.Center
            )
          }

          Spacer(modifier = Modifier.height(12.dp))
        }
      }
    }
  }
}

@Composable
private fun FeatureRow(
  icon: ImageVector,
  iconColor: Color,
  title: String,
  desc: String
) {
  Row(
    verticalAlignment = Alignment.Top,
    modifier = Modifier.fillMaxWidth()
  ) {
    Box(
      modifier = Modifier
        .size(32.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(iconColor.copy(alpha = 0.15f)),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = iconColor,
        modifier = Modifier.size(18.dp)
      )
    }

    Spacer(modifier = Modifier.width(12.dp))

    Column {
      Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White
      )
      Text(
        text = desc,
        fontSize = 11.sp,
        color = Color(0xFF94A3B8),
        lineHeight = 15.sp
      )
    }
  }
}

@Composable
private fun GoogleLogoIcon(modifier: Modifier = Modifier) {
  // Simple multi-color Google 'G' logo vector representation
  Box(
    modifier = modifier
      .size(20.dp)
      .clip(CircleShape)
      .background(Color.White),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = "G",
      fontSize = 14.sp,
      fontWeight = FontWeight.Black,
      color = Color(0xFF4285F4)
    )
  }
}
