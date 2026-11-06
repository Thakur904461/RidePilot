package com.example.model

data class DriverProfile(
  val id: String = "drv_in_9044",
  val name: String = "Rajesh Kumar",
  val phone: String = "+91 98765 43210",
  val city: String = "New Delhi / NCR",
  val vehicleType: String = "Bike Taxi & Auto",
  val vehicleRegistration: String = "DL 01 AB 4421",
  val rating: Float = 4.88f,
  val deviceId: String = "SM-G998B-IN-AUTH-091",
  val isSingleDeviceAuthorized: Boolean = true,
  val memberSince: String = "March 2024"
)

enum class PlanTier(val displayName: String, val billingLabel: String) {
  FREE_TRIAL("Free 3-Day Trial", "3 Days Free"),
  DAILY("Daily Pass", "₹5 / Day"),
  MONTHLY("Monthly Saver", "₹99 / Month"),
  HALF_YEARLY("Half-Yearly Pro", "₹499 / 6 Months"),
  YEARLY("Annual Super Saver", "₹699 / Year")
}

data class SubscriptionPlan(
  val id: String,
  val tier: PlanTier,
  val priceInr: Double,
  val originalPriceInr: Double,
  val durationDays: Int,
  val isPopular: Boolean = false,
  val features: List<String>
) {
  companion object {
    fun defaultPlans(): List<SubscriptionPlan> = listOf(
      SubscriptionPlan(
        id = "plan_trial",
        tier = PlanTier.FREE_TRIAL,
        priceInr = 0.0,
        originalPriceInr = 99.0,
        durationDays = 3,
        features = listOf(
          "Full auto-accept engine for 3 days",
          "Ola, Uber & Rapido support",
          "Voice announcements in Hindi & English",
          "Floating overlay widget"
        )
      ),
      SubscriptionPlan(
        id = "plan_daily",
        tier = PlanTier.DAILY,
        priceInr = 5.0,
        originalPriceInr = 15.0,
        durationDays = 1,
        features = listOf(
          "24-Hour full automation pass",
          "All 4 platforms (Ola, Uber, Rapido, Porter)",
          "Zone and Fare per km filters",
          "Draggable Floating Widget"
        )
      ),
      SubscriptionPlan(
        id = "plan_monthly",
        tier = PlanTier.MONTHLY,
        priceInr = 99.0,
        originalPriceInr = 199.0,
        durationDays = 30,
        isPopular = true,
        features = listOf(
          "Most popular among full-time drivers",
          "Only ₹3.30/day full auto-pilot",
          "Saves ~4 hours of screen staring daily",
          "Boosts daily income by ₹400-₹800",
          "All present & future platform integrations"
        )
      ),
      SubscriptionPlan(
        id = "plan_half_yearly",
        tier = PlanTier.HALF_YEARLY,
        priceInr = 499.0,
        originalPriceInr = 899.0,
        durationDays = 180,
        features = listOf(
          "6 Months uninterrupted auto-acceptance",
          "Save over 15% vs monthly pricing",
          "Priority customer support on WhatsApp",
          "High-earning preset & voice assistant",
          "Daily earnings analytics export"
        )
      ),
      SubscriptionPlan(
        id = "plan_yearly",
        tier = PlanTier.YEARLY,
        priceInr = 699.0,
        originalPriceInr = 1499.0,
        durationDays = 365,
        features = listOf(
          "Ultimate value — only ₹1.90/day",
          "Full 365 days coverage",
          "VIP 24/7 dedicated support",
          "Custom route rule automation",
          "Tax invoice for business expense deduction"
        )
      )
    )
  }
}

data class PaymentRecord(
  val paymentId: String,
  val orderId: String,
  val planName: String,
  val amountInr: Double,
  val timestamp: Long,
  val status: String = "SUCCESS",
  val method: String = "UPI / PhonePe / GPay",
  val invoiceNumber: String
)
