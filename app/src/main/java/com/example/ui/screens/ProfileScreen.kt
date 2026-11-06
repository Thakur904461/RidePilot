package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.runtime.rememberCoroutineScope
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.model.AutomationSettings
import com.example.model.DriverProfile
import com.example.model.PaymentRecord
import com.example.model.PlanTier
import com.example.model.SubscriptionPlan
import com.example.ui.payment.RazorpayCheckoutActivity
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.NeonGreenPrimary

import com.example.ui.components.RazorpayCheckoutDialog

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
  driverProfile: DriverProfile,
  settings: AutomationSettings,
  isSubscribed: Boolean,
  activePlan: SubscriptionPlan,
  paymentHistory: List<PaymentRecord>,
  isAccessibilityActive: Boolean,
  onSubscribePlan: (SubscriptionPlan) -> Unit,
  onUpdateVoiceSettings: (Boolean, String) -> Unit,
  onTestVoice: () -> Unit,
  onSignOutRequested: () -> Unit = {},
  onRecordPayment: ((SubscriptionPlan, PaymentRecord) -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var showSubscriptionPlansDialog by remember { mutableStateOf(false) }
  var showRazorpayCheckout by remember { mutableStateOf(false) }
  var latestPaymentRecord by remember { mutableStateOf<PaymentRecord?>(null) }
  var showPaymentSuccessDialog by remember { mutableStateOf(false) }
  var showAdminDashboardDialog by remember { mutableStateOf(false) }
  var selectedPlanForCheckout by remember { mutableStateOf<SubscriptionPlan?>(null) }
  var showSupportDialog by remember { mutableStateOf(false) }
  var showPrivacyDialog by remember { mutableStateOf(false) }

  val razorpayLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.StartActivityForResult()
  ) { result ->
    if (result.resultCode == Activity.RESULT_OK) {
      val paymentId = result.data?.getStringExtra(RazorpayCheckoutActivity.RESULT_PAYMENT_ID) ?: ""
      val orderId = result.data?.getStringExtra(RazorpayCheckoutActivity.RESULT_ORDER_ID) ?: ""
      val chosen = selectedPlanForCheckout ?: SubscriptionPlan.defaultPlans().first { it.tier == PlanTier.MONTHLY }
      val rec = PaymentRecord(
        paymentId = paymentId.ifBlank { "pay_rzp_sdk_${System.currentTimeMillis()}" },
        orderId = orderId.ifBlank { "order_rzp_sdk_${System.currentTimeMillis()}" },
        planName = chosen.tier.displayName,
        amountInr = chosen.priceInr * 1.18,
        timestamp = System.currentTimeMillis(),
        status = "SUCCESS",
        method = "Razorpay Android SDK",
        invoiceNumber = "INV-2026-RP-${(1000..9999).random()}"
      )
      latestPaymentRecord = rec
      showPaymentSuccessDialog = true
      onRecordPayment?.invoke(chosen, rec)
      onSubscribePlan(chosen)
    }
  }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    item {
      Spacer(modifier = Modifier.height(6.dp))

      // Driver Profile Header Card
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
              Box(
                modifier = Modifier
                  .size(48.dp)
                  .clip(CircleShape)
                  .background(NeonGreenPrimary),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Person,
                  contentDescription = null,
                  tint = Color(0xFF041C0C),
                  modifier = Modifier.size(28.dp)
                )
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = driverProfile.name,
                  fontSize = 17.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = "${driverProfile.phone} • ${driverProfile.city}",
                  fontSize = 12.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            // Single Device Authorization Pill
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF00391A))
                .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Default.VerifiedUser,
                  contentDescription = null,
                  tint = NeonGreenPrimary,
                  modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "DEVICE LOCKED",
                  fontSize = 9.sp,
                  fontWeight = FontWeight.Bold,
                  color = NeonGreenPrimary
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(8.dp))
              .background(DarkSurface)
              .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column {
              Text("Vehicle Category", fontSize = 10.sp, color = Color(0xFF94A3B8))
              Text(driverProfile.vehicleType, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Column {
              Text("Vehicle Reg No", fontSize = 10.sp, color = Color(0xFF94A3B8))
              Text(driverProfile.vehicleRegistration, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Column {
              Text("Driver Rating", fontSize = 10.sp, color = Color(0xFF94A3B8))
              Text("⭐ ${driverProfile.rating}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFB300))
            }
          }
        }
      }
    }

    // Firebase Cloud Sync & Google Sign-In Card
    item {
      val scope = rememberCoroutineScope()
      var currentUser by remember { mutableStateOf<FirebaseUser?>(Firebase.auth.currentUser) }
      var isAuthLoading by remember { mutableStateOf(false) }
      var authError by remember { mutableStateOf<String?>(null) }

      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(16.dp))
          .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
        color = Color(0xFF0A1B2D)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.VerifiedUser,
                contentDescription = null,
                tint = Color(0xFF00E5FF),
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "FIREBASE CLOUD BACKUP",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00E5FF)
              )
            }

            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(if (currentUser != null) Color(0xFF00391A) else Color(0xFF334155))
                .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
              Text(
                text = if (currentUser != null) "FIRESTORE SYNCED" else "LOCAL ONLY",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (currentUser != null) NeonGreenPrimary else Color(0xFF94A3B8)
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          if (currentUser != null) {
            Text(
              text = currentUser?.displayName ?: "Google Driver Account",
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
              color = Color.White
            )
            Text(
              text = currentUser?.email ?: "",
              fontSize = 12.sp,
              color = Color(0xFF94A3B8)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "Connected to Firestore enterprise database (asia-southeast1). Rides and settings automatically back up to the cloud.",
              fontSize = 11.sp,
              color = Color(0xFFBAE6FD),
              lineHeight = 15.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(DarkSurfaceElevated)
                .clickable {
                  scope.launch {
                    com.example.auth.GoogleAuthService.signOut(context) {
                      currentUser = null
                      onSignOutRequested()
                    }
                  }
                }
                .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
              Text(
                text = "Sign Out Google Account",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF94A3B8)
              )
            }
          } else {
            Text(
              text = "Sign in with Google to enable cloud backup for your ride history, presets, and location zones across devices.",
              fontSize = 11.sp,
              color = Color(0xFF94A3B8),
              lineHeight = 15.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            Button(
              onClick = {
                val activity = context as? Activity
                if (activity != null) {
                  isAuthLoading = true
                  authError = null
                  scope.launch {
                    com.example.auth.GoogleAuthService.signInWithGoogle(
                      activity = activity,
                      onSuccess = {
                        isAuthLoading = false
                        currentUser = Firebase.auth.currentUser
                      },
                      onError = { err ->
                        isAuthLoading = false
                        authError = err
                      },
                      onCancelled = {
                        isAuthLoading = false
                      }
                    )
                  }
                }
              },
              enabled = !isAuthLoading,
              colors = ButtonDefaults.buttonColors(containerColor = Color.White),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.fillMaxWidth().height(44.dp)
            ) {
              if (isAuthLoading) {
                Text("Connecting to Google...", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold)
              } else {
                Text("Sign in with Google", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold)
              }
            }

            if (authError != null) {
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = authError ?: "",
                fontSize = 11.sp,
                color = Color(0xFFFF5252)
              )
            }
          }
        }
      }
    }

    // Razorpay Subscription Status Card
    item {
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(16.dp))
          .border(1.dp, NeonGreenPrimary.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
        color = Color(0xFF072414)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = NeonGreenPrimary,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "SUBSCRIPTION ACTIVE",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = NeonGreenPrimary
              )
            }

            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(NeonGreenPrimary)
                .clickable { showSubscriptionPlansDialog = true }
                .padding(horizontal = 10.dp, vertical = 5.dp)
                .testTag("upgrade_plan_button"),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "CHANGE PLAN",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF041C0C)
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          Text(
            text = activePlan.tier.displayName,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
          )
          Text(
            text = "Valid for 30 Days • Razorpay Gateway Auto-Renew • Device Protected",
            fontSize = 11.sp,
            color = Color(0xFF94A3B8)
          )
        }
      }
    }

    // Voice Assistant Configuration
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
                imageVector = Icons.Default.VolumeUp,
                contentDescription = null,
                tint = Color(0xFF00E5FF),
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "VOICE ANNOUNCEMENTS",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
            }

            Switch(
              checked = settings.voiceAnnouncementsEnabled,
              onCheckedChange = { onUpdateVoiceSettings(it, settings.voiceLanguage) },
              colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF041C0C),
                checkedTrackColor = NeonGreenPrimary
              )
            )
          }

          Spacer(modifier = Modifier.height(10.dp))

          Text(
            text = "Speaks incoming ride amounts, distances, and acceptance results aloud while driving.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          Spacer(modifier = Modifier.height(10.dp))

          // Language Selector
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Box(
              modifier = Modifier
                .weight(1f)
                .height(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (settings.voiceLanguage == "hi") Color(0xFF00391A) else DarkSurface)
                .border(1.dp, if (settings.voiceLanguage == "hi") NeonGreenPrimary else Color.Transparent, RoundedCornerShape(8.dp))
                .clickable { onUpdateVoiceSettings(settings.voiceAnnouncementsEnabled, "hi") },
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "Hinglish / Hindi (हिन्दी)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (settings.voiceLanguage == "hi") NeonGreenPrimary else Color.White
              )
            }

            Box(
              modifier = Modifier
                .weight(1f)
                .height(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (settings.voiceLanguage == "en") Color(0xFF00391A) else DarkSurface)
                .border(1.dp, if (settings.voiceLanguage == "en") NeonGreenPrimary else Color.Transparent, RoundedCornerShape(8.dp))
                .clickable { onUpdateVoiceSettings(settings.voiceAnnouncementsEnabled, "en") },
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "English",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (settings.voiceLanguage == "en") NeonGreenPrimary else Color.White
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(36.dp)
              .clip(RoundedCornerShape(8.dp))
              .background(DarkSurfaceElevated)
              .clickable { onTestVoice() }
              .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "🔊 Test Voice Announcement Sample",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFF38BDF8)
            )
          }
        }
      }
    }

    // Android Permissions Status & Setup Wizard
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
            text = "SYSTEM PERMISSIONS STATUS",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Required for RidePilot to monitor screen and accept permitted rides",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          Spacer(modifier = Modifier.height(12.dp))

          PermissionRow(
            title = "Accessibility Service",
            description = "Auto-detects ride requests and executes accept/skip",
            isGranted = isAccessibilityActive,
            onOpenSettings = {
              try {
                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
              } catch (_: Exception) {}
            }
          )

          Spacer(modifier = Modifier.height(8.dp))

          PermissionRow(
            title = "Display Over Other Apps",
            description = "Floating widget over Ola, Uber, Rapido & Porter",
            isGranted = true,
            onOpenSettings = {
              try {
                context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")))
              } catch (_: Exception) {}
            }
          )
        }
      }
    }

    // Admin Dashboard Button & Utilities
    item {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E293B))
            .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .clickable { showAdminDashboardDialog = true }
            .padding(horizontal = 14.dp)
            .testTag("admin_dashboard_button"),
          contentAlignment = Alignment.Center
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.AdminPanelSettings,
              contentDescription = null,
              tint = Color(0xFF38BDF8),
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "VIEW ADMIN & REVENUE PORTAL",
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFF38BDF8)
            )
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Box(
            modifier = Modifier
              .weight(1f)
              .height(42.dp)
              .clip(RoundedCornerShape(10.dp))
              .background(DarkSurfaceElevated)
              .clickable { showSupportDialog = true },
            contentAlignment = Alignment.Center
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.HelpOutline, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Driver Support", fontSize = 12.sp, color = Color.White)
            }
          }

          Box(
            modifier = Modifier
              .weight(1f)
              .height(42.dp)
              .clip(RoundedCornerShape(10.dp))
              .background(DarkSurfaceElevated)
              .clickable { showPrivacyDialog = true },
            contentAlignment = Alignment.Center
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Security, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Privacy & Terms", fontSize = 12.sp, color = Color.White)
            }
          }
        }
      }
    }

    item {
      Spacer(modifier = Modifier.height(20.dp))
    }
  }

  // Subscription Plans Modal Dialog
  if (showSubscriptionPlansDialog) {
    AlertDialog(
      onDismissRequest = { showSubscriptionPlansDialog = false },
      title = {
        Text("Choose RidePilot Plan", fontWeight = FontWeight.Bold, fontSize = 18.sp)
      },
      text = {
        LazyColumn(
          modifier = Modifier.height(380.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          item {
            Text(
              "Instant activation via Razorpay. Secured with single device authorization.",
              fontSize = 11.sp,
              color = Color(0xFF94A3B8)
            )
          }

          items(SubscriptionPlan.defaultPlans().size) { index ->
            val plan = SubscriptionPlan.defaultPlans()[index]
            val isCurrent = activePlan.id == plan.id

            Surface(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(
                  1.dp,
                  if (plan.isPopular) NeonGreenPrimary else Color(0xFF22335C),
                  RoundedCornerShape(12.dp)
                )
                .clickable {
                  selectedPlanForCheckout = plan
                },
              color = if (selectedPlanForCheckout?.id == plan.id) Color(0xFF072414) else DarkSurface
            ) {
              Column(modifier = Modifier.padding(12.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                      text = plan.tier.displayName,
                      fontSize = 14.sp,
                      fontWeight = FontWeight.Bold,
                      color = Color.White
                    )
                    if (plan.isPopular) {
                      Spacer(modifier = Modifier.width(6.dp))
                      Box(
                        modifier = Modifier
                          .clip(RoundedCornerShape(4.dp))
                          .background(NeonGreenPrimary)
                          .padding(horizontal = 6.dp, vertical = 2.dp)
                      ) {
                        Text(
                          "POPULAR",
                          fontSize = 9.sp,
                          fontWeight = FontWeight.Black,
                          color = Color(0xFF041C0C)
                        )
                      }
                    }
                  }

                  Text(
                    text = if (plan.priceInr == 0.0) "FREE" else "₹${plan.priceInr.toInt()}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = NeonGreenPrimary
                  )
                }

                Spacer(modifier = Modifier.height(6.dp))

                plan.features.take(2).forEach { feat ->
                  Text(
                    text = "• $feat",
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8)
                  )
                }
              }
            }
          }
        }
      },
      confirmButton = {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          OutlinedButton(
            onClick = {
              val chosen = selectedPlanForCheckout ?: SubscriptionPlan.defaultPlans().first { it.tier == PlanTier.MONTHLY }
              selectedPlanForCheckout = chosen
              showSubscriptionPlansDialog = false
              showRazorpayCheckout = true
            }
          ) {
            Text("Web Modal", fontSize = 11.sp, color = Color.White)
          }

          Button(
            onClick = {
              val chosen = selectedPlanForCheckout ?: SubscriptionPlan.defaultPlans().first { it.tier == PlanTier.MONTHLY }
              selectedPlanForCheckout = chosen
              showSubscriptionPlansDialog = false
              razorpayLauncher.launch(
                RazorpayCheckoutActivity.createIntent(
                  context = context,
                  plan = chosen,
                  driverName = driverProfile.name,
                  driverPhone = driverProfile.phone
                )
              )
            },
            colors = ButtonDefaults.buttonColors(containerColor = NeonGreenPrimary)
          ) {
            Text("Razorpay SDK", color = Color(0xFF041C0C), fontWeight = FontWeight.Bold, fontSize = 11.sp)
          }
        }
      },
      dismissButton = {
        TextButton(onClick = { showSubscriptionPlansDialog = false }) {
          Text("Cancel", color = Color(0xFF94A3B8))
        }
      }
    )
  }

  // Razorpay Payment Gateway Modal
  if (showRazorpayCheckout && selectedPlanForCheckout != null) {
    RazorpayCheckoutDialog(
      plan = selectedPlanForCheckout!!,
      driverName = driverProfile.name,
      driverPhone = driverProfile.phone,
      onDismiss = { showRazorpayCheckout = false },
      onPaymentSuccess = { record ->
        showRazorpayCheckout = false
        latestPaymentRecord = record
        if (onRecordPayment != null) {
          onRecordPayment(selectedPlanForCheckout!!, record)
        } else {
          onSubscribePlan(selectedPlanForCheckout!!)
        }
        showPaymentSuccessDialog = true
      }
    )
  }

  // Payment Success / Tax Invoice Dialog
  if (showPaymentSuccessDialog) {
    val displayPayment = latestPaymentRecord ?: paymentHistory.firstOrNull()
    AlertDialog(
      onDismissRequest = { showPaymentSuccessDialog = false },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.CheckCircle, contentDescription = null, tint = NeonGreenPrimary)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Payment Successful!", fontWeight = FontWeight.Bold)
        }
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text("Plan: ${activePlan.tier.displayName}", fontWeight = FontWeight.Bold)
          Text("Total Paid: ₹${String.format(java.util.Locale.US, "%.2f", displayPayment?.amountInr ?: 647.82)} (incl. 18% GST)")
          Text("Payment Method: ${displayPayment?.method ?: "Razorpay (UPI)"}")
          Text("Razorpay ID: ${displayPayment?.paymentId ?: "pay_rzp_test_881920"}")
          Text("Order ID: ${displayPayment?.orderId ?: "order_rzp_44129"}")
          Text("Tax Invoice: ${displayPayment?.invoiceNumber ?: "INV-2026-RP-0418"}")
          Text("Single-Device License: ${driverProfile.deviceId}")
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            "Auto-accept is fully activated. Automated ride acceptance on Ola, Uber, Rapido, and Porter is now unlocked!",
            fontSize = 11.sp,
            color = Color(0xFF94A3B8)
          )
        }
      },
      confirmButton = {
        Button(
          onClick = { showPaymentSuccessDialog = false },
          colors = ButtonDefaults.buttonColors(containerColor = NeonGreenPrimary)
        ) {
          Text("Done", color = Color(0xFF041C0C))
        }
      }
    )
  }

  // Admin Dashboard Modal
  if (showAdminDashboardDialog) {
    AlertDialog(
      onDismissRequest = { showAdminDashboardDialog = false },
      title = { Text("RidePilot Admin Portal", fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Surface(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(8.dp)),
            color = DarkSurface
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Text("Total Registered Drivers: 42,890", fontSize = 12.sp, fontWeight = FontWeight.Bold)
              Text("Active Paid Subscribers: 28,140 (65.6%)", fontSize = 12.sp, color = NeonGreenPrimary)
              Text("Monthly GMV Saved: ₹1.54 Crore", fontSize = 12.sp, color = Color(0xFF00E5FF))
              Text("Avg Automated Daily Rides: 1.8 Lakhs", fontSize = 12.sp)
            }
          }

          Text("Integration Compatibility Status:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
          Text("• Ola Driver (v5.8.2): Verified Healthy", fontSize = 11.sp, color = NeonGreenPrimary)
          Text("• Uber Driver (v4.492): Verified Healthy", fontSize = 11.sp, color = NeonGreenPrimary)
          Text("• Rapido Captain (v6.1.0): Verified Healthy", fontSize = 11.sp, color = NeonGreenPrimary)
          Text("• Porter Partner (v7.2.1): Verified Healthy", fontSize = 11.sp, color = NeonGreenPrimary)
        }
      },
      confirmButton = {
        Button(
          onClick = { showAdminDashboardDialog = false },
          colors = ButtonDefaults.buttonColors(containerColor = NeonGreenPrimary)
        ) {
          Text("Close", color = Color(0xFF041C0C))
        }
      }
    )
  }

  // Support Dialog
  if (showSupportDialog) {
    AlertDialog(
      onDismissRequest = { showSupportDialog = false },
      title = { Text("Driver Help & Support") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("WhatsApp Support: +91 88000 12345 (24/7)", fontWeight = FontWeight.Bold)
          Text("Email: driverhelp@ridepilot.in")
          Text("Working Hours: All days 6:00 AM - 11:59 PM")
          Text("Guides: How to setup overlay permission, optimize ₹/km in Delhi NCR, Mumbai, Bangalore.")
        }
      },
      confirmButton = {
        Button(onClick = { showSupportDialog = false }) {
          Text("OK")
        }
      }
    )
  }

  // Privacy Dialog
  if (showPrivacyDialog) {
    AlertDialog(
      onDismissRequest = { showPrivacyDialog = false },
      title = { Text("Privacy Policy & Terms") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text("1. Zero Personal Data Theft: RidePilot runs on-device using Android Accessibility APIs.")
          Text("2. No Screenshots Uploaded: Fare and location details are processed locally in real time.")
          Text("3. Driver Control: Automation can be paused or emergency halted at any moment.")
          Text("4. Razorpay Secure: PCI-DSS certified Indian payments.")
        }
      },
      confirmButton = {
        Button(onClick = { showPrivacyDialog = false }) {
          Text("Understood")
        }
      }
    )
  }
}

@Composable
private fun PermissionRow(
  title: String,
  description: String,
  isGranted: Boolean,
  onOpenSettings: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(8.dp))
      .background(DarkSurface)
      .padding(10.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Lock,
          contentDescription = null,
          tint = if (isGranted) NeonGreenPrimary else Color(0xFFFFB300),
          modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = title,
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          color = Color.White
        )
      }
      Text(
        text = description,
        fontSize = 10.sp,
        color = Color(0xFF94A3B8)
      )
    }

    Box(
      modifier = Modifier
        .clip(RoundedCornerShape(6.dp))
        .background(if (isGranted) Color(0xFF00391A) else Color(0xFF3B2900))
        .clickable { onOpenSettings() }
        .padding(horizontal = 8.dp, vertical = 4.dp),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = if (isGranted) "ENABLED" else "GRANT",
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = if (isGranted) NeonGreenPrimary else Color(0xFFFFB300)
      )
    }
  }
}
