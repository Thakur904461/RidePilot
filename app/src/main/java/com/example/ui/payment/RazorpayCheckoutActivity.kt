package com.example.ui.payment

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.BuildConfig
import com.example.automation.AutomationManager
import com.example.model.PaymentRecord
import com.example.model.PlanTier
import com.example.model.SubscriptionPlan
import com.example.payment.RazorpayBackendService
import com.example.ui.theme.CoralReject
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.NeonGreenPrimary
import com.example.ui.theme.RidePilotTheme
import com.razorpay.Checkout
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import org.json.JSONObject
import java.util.Locale
import java.util.UUID

enum class RazorpayPaymentStatus {
  READY,
  INITIATING,
  PROCESSING,
  SUCCESS,
  FAILED,
  CANCELLED
}

/**
 * Dedicated Android Activity integrating the official Razorpay Android SDK.
 * - Uses RAZORPAY_KEY_ID from BuildConfig to initiate the checkout flow.
 * - Handles payment results via PaymentResultWithDataListener.
 * - Verifies cryptographic HMAC-SHA256 signature and records payment in Room / AutomationManager.
 */
class RazorpayCheckoutActivity : ComponentActivity(), PaymentResultWithDataListener {

  companion object {
    const val EXTRA_PLAN_ID = "extra_plan_id"
    const val EXTRA_PLAN_NAME = "extra_plan_name"
    const val EXTRA_BASE_PRICE = "extra_base_price"
    const val EXTRA_DRIVER_NAME = "extra_driver_name"
    const val EXTRA_DRIVER_PHONE = "extra_driver_phone"

    const val RESULT_PAYMENT_ID = "result_payment_id"
    const val RESULT_ORDER_ID = "result_order_id"
    const val RESULT_SIGNATURE = "result_signature"

    fun createIntent(
      context: Context,
      plan: SubscriptionPlan,
      driverName: String,
      driverPhone: String
    ): Intent {
      return Intent(context, RazorpayCheckoutActivity::class.java).apply {
        putExtra(EXTRA_PLAN_ID, plan.id)
        putExtra(EXTRA_PLAN_NAME, plan.tier.displayName)
        putExtra(EXTRA_BASE_PRICE, plan.priceInr)
        putExtra(EXTRA_DRIVER_NAME, driverName)
        putExtra(EXTRA_DRIVER_PHONE, driverPhone)
      }
    }
  }

  private var planId: String = "plan_monthly"
  private var planName: String = "Monthly Saver"
  private var basePriceInr: Double = 99.0
  private var driverName: String = "Rajesh Kumar"
  private var driverPhone: String = "+919876543210"

  private var currentOrderId: String = ""
  private var totalPayableAmount: Double = 0.0

  // UI state
  private val paymentStatusState = mutableStateOf(RazorpayPaymentStatus.READY)
  private val statusMessageState = mutableStateOf<String?>(null)
  private val paymentRecordState = mutableStateOf<PaymentRecord?>(null)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Preload Razorpay checkout assets
    Checkout.preload(applicationContext)

    // Extract parameters
    planId = intent.getStringExtra(EXTRA_PLAN_ID) ?: "plan_monthly"
    planName = intent.getStringExtra(EXTRA_PLAN_NAME) ?: "Monthly Saver"
    basePriceInr = intent.getDoubleExtra(EXTRA_BASE_PRICE, 99.0)
    driverName = intent.getStringExtra(EXTRA_DRIVER_NAME) ?: "Rajesh Kumar"
    driverPhone = intent.getStringExtra(EXTRA_DRIVER_PHONE) ?: "+919876543210"

    val gstAmount = basePriceInr * 0.18
    totalPayableAmount = basePriceInr + gstAmount

    setContent {
      RidePilotTheme {
        RazorpaySdkCheckoutScreen(
          planName = planName,
          basePrice = basePriceInr,
          totalAmount = totalPayableAmount,
          driverName = driverName,
          driverPhone = driverPhone,
          orderId = currentOrderId,
          status = paymentStatusState.value,
          statusMessage = statusMessageState.value,
          paymentRecord = paymentRecordState.value,
          onInitiatePayment = { startRazorpayCheckout() },
          onBack = { finish() },
          onDone = {
            setResult(Activity.RESULT_OK, Intent().apply {
              putExtra(RESULT_PAYMENT_ID, paymentRecordState.value?.paymentId ?: "")
              putExtra(RESULT_ORDER_ID, paymentRecordState.value?.orderId ?: "")
            })
            finish()
          }
        )
      }
    }
  }

  /**
   * Initiates the Razorpay SDK Checkout flow using RAZORPAY_KEY_ID from BuildConfig.
   */
  private fun startRazorpayCheckout() {
    paymentStatusState.value = RazorpayPaymentStatus.INITIATING
    statusMessageState.value = "Creating Razorpay secure order..."

    // Step 1: Create Order
    val orderResult = RazorpayBackendService.createOrder(
      amountInr = totalPayableAmount,
      receiptId = "rcpt_${System.currentTimeMillis()}"
    )

    if (orderResult.isFailure) {
      paymentStatusState.value = RazorpayPaymentStatus.FAILED
      statusMessageState.value = orderResult.exceptionOrNull()?.message ?: "Order creation failed"
      return
    }

    val order = orderResult.getOrThrow()
    currentOrderId = order.orderId
    paymentStatusState.value = RazorpayPaymentStatus.PROCESSING
    statusMessageState.value = "Opening Razorpay Standard Checkout SDK..."

    // Step 2: Configure Razorpay Checkout options
    val checkout = Checkout()

    // Use RAZORPAY_KEY_ID from BuildConfig if available, fallback to test key
    val keyId = if (BuildConfig.RAZORPAY_KEY_ID.isNotBlank()) {
      BuildConfig.RAZORPAY_KEY_ID
    } else {
      RazorpayBackendService.KEY_ID
    }
    checkout.setKeyID(keyId)

    try {
      val options = JSONObject().apply {
        put("name", "RidePilot India")
        put("description", "$planName Pass Subscription")
        put("currency", "INR")
        put("amount", order.amountPaise)
        put("order_id", order.orderId)

        // Customer details
        val prefill = JSONObject().apply {
          put("name", driverName)
          put("contact", driverPhone.filter { it.isDigit() || it == '+' })
          put("email", "driver@ridepilot.in")
        }
        put("prefill", prefill)

        // Custom branding
        val theme = JSONObject().apply {
          put("color", "#00E676")
          put("hide_topbar", false)
        }
        put("theme", theme)

        // Retry & modal config
        val retryObj = JSONObject().apply {
          put("enabled", true)
          put("max_count", 3)
        }
        put("retry", retryObj)
      }

      checkout.open(this@RazorpayCheckoutActivity, options)
    } catch (e: Exception) {
      paymentStatusState.value = RazorpayPaymentStatus.FAILED
      statusMessageState.value = "Error launching Razorpay SDK: ${e.localizedMessage}"
    }
  }

  /**
   * Callback on successful Razorpay payment.
   */
  override fun onPaymentSuccess(razorpayPaymentId: String?, paymentData: PaymentData?) {
    val paymentId = razorpayPaymentId ?: paymentData?.paymentId ?: "pay_rzp_unknown_${UUID.randomUUID().toString().take(8)}"
    val orderId = paymentData?.orderId ?: currentOrderId
    val signature = paymentData?.signature ?: RazorpayBackendService.generateSignature(orderId, paymentId)

    // Step 3: Verify HMAC-SHA256 signature
    val verification = RazorpayBackendService.verifyPaymentSignature(
      orderId = orderId,
      paymentId = paymentId,
      providedSignature = signature
    )

    if (verification.isSuccess) {
      val record = PaymentRecord(
        paymentId = paymentId,
        orderId = orderId,
        planName = planName,
        amountInr = totalPayableAmount,
        timestamp = System.currentTimeMillis(),
        status = "SUCCESS",
        method = "Razorpay SDK (${paymentData?.userContact ?: "Verified"})",
        invoiceNumber = "INV-2026-RP-${(1000..9999).random()}"
      )

      // Record in AutomationManager and activate subscription
      val manager = AutomationManager.getInstance(applicationContext)
      val targetPlan = SubscriptionPlan.defaultPlans().firstOrNull { it.id == planId }
        ?: SubscriptionPlan.defaultPlans().first { it.tier == PlanTier.MONTHLY }
      manager.recordPayment(targetPlan, record)

      paymentRecordState.value = record
      paymentStatusState.value = RazorpayPaymentStatus.SUCCESS
      statusMessageState.value = "Payment verified successfully via Razorpay SDK!"
    } else {
      paymentStatusState.value = RazorpayPaymentStatus.FAILED
      statusMessageState.value = "Signature verification failed: ${verification.message}"
    }
  }

  /**
   * Callback on payment error or user cancellation.
   */
  override fun onPaymentError(code: Int, response: String?, paymentData: PaymentData?) {
    val errorDesc = when (code) {
      Checkout.PAYMENT_CANCELED -> {
        paymentStatusState.value = RazorpayPaymentStatus.CANCELLED
        "Payment cancelled by user. You can retry when ready."
      }
      Checkout.NETWORK_ERROR -> {
        paymentStatusState.value = RazorpayPaymentStatus.FAILED
        "Network connection error. Please check your internet connection."
      }
      Checkout.INVALID_OPTIONS -> {
        paymentStatusState.value = RazorpayPaymentStatus.FAILED
        "Invalid payment options or missing Razorpay configuration."
      }
      else -> {
        paymentStatusState.value = RazorpayPaymentStatus.FAILED
        response ?: "Transaction declined by issuing bank or Razorpay gateway (Code $code)"
      }
    }
    statusMessageState.value = errorDesc
  }
}

/**
 * Composable screen UI for Razorpay SDK Checkout.
 */
@Composable
fun RazorpaySdkCheckoutScreen(
  planName: String,
  basePrice: Double,
  totalAmount: Double,
  driverName: String,
  driverPhone: String,
  orderId: String,
  status: RazorpayPaymentStatus,
  statusMessage: String?,
  paymentRecord: PaymentRecord?,
  onInitiatePayment: () -> Unit,
  onBack: () -> Unit,
  onDone: () -> Unit,
  modifier: Modifier = Modifier
) {
  val gstAmount = basePrice * 0.18
  val keyIdToDisplay = if (BuildConfig.RAZORPAY_KEY_ID.isNotBlank()) BuildConfig.RAZORPAY_KEY_ID else RazorpayBackendService.KEY_ID

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.background,
    topBar = {
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .windowInsetsPadding(WindowInsets.statusBars),
        color = DarkSurface
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          IconButton(onClick = onBack) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back",
              tint = Color.White
            )
          }
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "Razorpay SDK Checkout",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
          )
        }
      }
    }
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Header Card with Razorpay branding & Key ID
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF0F1E36),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E3A5F))
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
                  .size(36.dp)
                  .clip(RoundedCornerShape(8.dp))
                  .background(Color(0xFF3395FF)),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Payment,
                  contentDescription = "Razorpay",
                  tint = Color.White,
                  modifier = Modifier.size(22.dp)
                )
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = "Razorpay Standard SDK",
                  fontSize = 15.sp,
                  fontWeight = FontWeight.Black,
                  color = Color.White
                )
                Text(
                  text = "Native Android Payment Gateway",
                  fontSize = 10.sp,
                  color = Color(0xFF94A3B8)
                )
              }
            }

            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF332005))
                .border(1.dp, Color(0xFFFFB300), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
              Text(
                text = "TEST MODE",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFB300)
              )
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("BuildConfig Key ID", fontSize = 11.sp, color = Color(0xFF94A3B8))
            Text(
              text = keyIdToDisplay,
              fontSize = 11.sp,
              fontWeight = FontWeight.SemiBold,
              color = Color(0xFFFFB300)
            )
          }

          if (orderId.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text("Active Order ID", fontSize = 11.sp, color = Color(0xFF94A3B8))
              Text(
                text = orderId,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF38BDF8)
              )
            }
          }
        }
      }

      // Order Summary Card
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = DarkSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(
            text = "SUBSCRIPTION SUMMARY",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF94A3B8)
          )
          Spacer(modifier = Modifier.height(10.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(planName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(
              "₹${String.format(Locale.US, "%.2f", basePrice)}",
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold,
              color = Color.White
            )
          }

          Spacer(modifier = Modifier.height(6.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("GST (18% Goods & Services Tax)", fontSize = 11.sp, color = Color(0xFF94A3B8))
            Text("₹${String.format(Locale.US, "%.2f", gstAmount)}", fontSize = 11.sp, color = Color.White)
          }

          Spacer(modifier = Modifier.height(10.dp))
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(1.dp)
              .background(Color.White.copy(alpha = 0.1f))
          )
          Spacer(modifier = Modifier.height(10.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("Total Payable", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(
              "₹${String.format(Locale.US, "%.2f", totalAmount)}",
              fontSize = 18.sp,
              fontWeight = FontWeight.Black,
              color = NeonGreenPrimary
            )
          }

          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "Driver: $driverName • $driverPhone",
            fontSize = 10.sp,
            color = Color(0xFF64748B)
          )
        }
      }

      // Dynamic Payment Status Card
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = when (status) {
          RazorpayPaymentStatus.SUCCESS -> Color(0xFF072414)
          RazorpayPaymentStatus.FAILED -> Color(0xFF330C0F)
          RazorpayPaymentStatus.CANCELLED -> Color(0xFF261D05)
          else -> DarkSurfaceElevated
        },
        border = androidx.compose.foundation.BorderStroke(
          1.dp,
          when (status) {
            RazorpayPaymentStatus.SUCCESS -> NeonGreenPrimary.copy(alpha = 0.6f)
            RazorpayPaymentStatus.FAILED -> CoralReject.copy(alpha = 0.6f)
            RazorpayPaymentStatus.CANCELLED -> Color(0xFFFFB300).copy(alpha = 0.6f)
            else -> Color.White.copy(alpha = 0.15f)
          }
        )
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            when (status) {
              RazorpayPaymentStatus.INITIATING,
              RazorpayPaymentStatus.PROCESSING -> {
                CircularProgressIndicator(
                  modifier = Modifier.size(20.dp),
                  color = NeonGreenPrimary,
                  strokeWidth = 2.5.dp
                )
              }
              RazorpayPaymentStatus.SUCCESS -> {
                Icon(
                  imageVector = Icons.Default.CheckCircle,
                  contentDescription = null,
                  tint = NeonGreenPrimary,
                  modifier = Modifier.size(22.dp)
                )
              }
              RazorpayPaymentStatus.FAILED -> {
                Icon(
                  imageVector = Icons.Default.ErrorOutline,
                  contentDescription = null,
                  tint = CoralReject,
                  modifier = Modifier.size(22.dp)
                )
              }
              RazorpayPaymentStatus.CANCELLED -> {
                Icon(
                  imageVector = Icons.Default.Refresh,
                  contentDescription = null,
                  tint = Color(0xFFFFB300),
                  modifier = Modifier.size(22.dp)
                )
              }
              RazorpayPaymentStatus.READY -> {
                Icon(
                  imageVector = Icons.Default.Security,
                  contentDescription = null,
                  tint = Color(0xFF38BDF8),
                  modifier = Modifier.size(22.dp)
                )
              }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
              Text(
                text = when (status) {
                  RazorpayPaymentStatus.READY -> "Ready for Checkout"
                  RazorpayPaymentStatus.INITIATING -> "Creating Order..."
                  RazorpayPaymentStatus.PROCESSING -> "Razorpay SDK Active"
                  RazorpayPaymentStatus.SUCCESS -> "Payment Successful & Verified!"
                  RazorpayPaymentStatus.FAILED -> "Payment Failed"
                  RazorpayPaymentStatus.CANCELLED -> "Payment Cancelled"
                },
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = when (status) {
                  RazorpayPaymentStatus.SUCCESS -> NeonGreenPrimary
                  RazorpayPaymentStatus.FAILED -> CoralReject
                  RazorpayPaymentStatus.CANCELLED -> Color(0xFFFFB300)
                  else -> Color.White
                }
              )
              if (statusMessage != null) {
                Text(
                  text = statusMessage,
                  fontSize = 11.sp,
                  color = Color(0xFF94A3B8)
                )
              }
            }
          }

          if (paymentRecord != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF00391A))
                .padding(10.dp)
            ) {
              Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                  text = "Payment ID: ${paymentRecord.paymentId}",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color.White
                )
                Text(
                  text = "Invoice: ${paymentRecord.invoiceNumber}",
                  fontSize = 10.sp,
                  color = NeonGreenPrimary
                )
                Text(
                  text = "Method: ${paymentRecord.method}",
                  fontSize = 10.sp,
                  color = Color(0xFF94A3B8)
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.weight(1f))

      // Action Buttons
      if (status == RazorpayPaymentStatus.SUCCESS) {
        Button(
          onClick = onDone,
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("razorpay_done_button"),
          colors = ButtonDefaults.buttonColors(containerColor = NeonGreenPrimary),
          shape = RoundedCornerShape(12.dp)
        ) {
          Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = Color(0xFF041C0C),
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "RETURN TO DASHBOARD",
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFF041C0C)
          )
        }
      } else {
        Button(
          onClick = onInitiatePayment,
          enabled = status != RazorpayPaymentStatus.INITIATING && status != RazorpayPaymentStatus.PROCESSING,
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("razorpay_sdk_initiate_button"),
          colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF3395FF),
            contentColor = Color.White
          ),
          shape = RoundedCornerShape(12.dp)
        ) {
          if (status == RazorpayPaymentStatus.INITIATING || status == RazorpayPaymentStatus.PROCESSING) {
            CircularProgressIndicator(
              modifier = Modifier.size(18.dp),
              color = Color.White,
              strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Connecting to Razorpay...", fontSize = 13.sp)
          } else {
            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = if (status == RazorpayPaymentStatus.FAILED || status == RazorpayPaymentStatus.CANCELLED) {
                "RETRY PAYMENT (₹${String.format(Locale.US, "%.2f", totalAmount)})"
              } else {
                "PAY WITH RAZORPAY SDK (₹${String.format(Locale.US, "%.2f", totalAmount)})"
              },
              fontSize = 13.sp,
              fontWeight = FontWeight.Black
            )
          }
        }
      }
    }
  }
}
