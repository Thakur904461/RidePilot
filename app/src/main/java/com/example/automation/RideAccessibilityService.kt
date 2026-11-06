package com.example.automation

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.model.AutomationState
import com.example.model.DecisionType
import java.util.concurrent.ConcurrentHashMap

class RideAccessibilityService : AccessibilityService() {

  private val TAG = "RideAccessibilitySvc"
  private val recentlyProcessedRides = ConcurrentHashMap<String, Long>()
  private var lastEventTime = 0L

  override fun onServiceConnected() {
    super.onServiceConnected()
    Log.d(TAG, "RidePilot Accessibility Service Connected")
    AutomationManager.getInstance(this).setAccessibilityConnected(true)
  }

  override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    if (event == null) return

    val currentTime = System.currentTimeMillis()
    if (currentTime - lastEventTime < 250) {
      // Debounce high frequency layout pulses
      return
    }
    lastEventTime = currentTime

    val rootNode = rootInActiveWindow ?: return
    val packageName = event.packageName?.toString() ?: ""

    val manager = AutomationManager.getInstance(this)
    val settings = manager.settings.value

    // If automation is stopped, skip processing
    if (settings.state == AutomationState.STOPPED || settings.emergencyStopActive) {
      return
    }

    // Inspect if the active window belongs to a supported ride hailing application
    val isSupportedApp = packageName.contains("olacabs") ||
      packageName.contains("ubercab") ||
      packageName.contains("rapido") ||
      packageName.contains("porter") ||
      packageName.contains("driver")

    if (!isSupportedApp) {
      return
    }

    val extractedTexts = mutableListOf<String>()
    var acceptNode: AccessibilityNodeInfo? = null
    var rejectNode: AccessibilityNodeInfo? = null

    // Traverse node hierarchy
    traverseNodeHierarchy(rootNode, extractedTexts) { node, textLower ->
      if (acceptNode == null && (textLower.contains("accept") || textLower.contains("swiped to accept") || textLower.contains("take ride"))) {
        acceptNode = node
      }
      if (rejectNode == null && (textLower.contains("decline") || textLower.contains("skip") || textLower.contains("reject") || textLower.contains("pass"))) {
        rejectNode = node
      }
    }

    if (extractedTexts.isEmpty()) return

    val platform = RideDataExtractor.identifyPlatform(packageName, extractedTexts.joinToString(" "))
    val request = RideDataExtractor.extractFromTexts(extractedTexts, platform, "") ?: return

    // Deduplication check: avoid re-processing identical ride within 15 seconds
    val dedupeKey = "${platform.id}_${request.fare}_${request.tripDistanceKm}"
    val lastProcessed = recentlyProcessedRides[dedupeKey] ?: 0L
    if (currentTime - lastProcessed < 15000) {
      return
    }
    recentlyProcessedRides[dedupeKey] = currentTime

    // Clean old deduplication entries
    if (recentlyProcessedRides.size > 50) {
      recentlyProcessedRides.entries.removeIf { currentTime - it.value > 60000 }
    }

    // Process ride through the decision engine
    manager.processRideRequest(request) { decision ->
      when (decision) {
        DecisionType.ACCEPT -> {
          if (settings.autoAcceptEnabled && acceptNode != null) {
            executeClick(acceptNode)
            Log.d(TAG, "Executed auto-accept click on ${platform.displayName} node")
          }
        }
        DecisionType.REJECT -> {
          if (settings.autoRejectEnabled && rejectNode != null) {
            executeClick(rejectNode)
            Log.d(TAG, "Executed auto-reject click on ${platform.displayName} node")
          }
        }
        else -> {
          // Manual review or unknown -> do not automatically interact
        }
      }
    }
  }

  private fun traverseNodeHierarchy(
    node: AccessibilityNodeInfo,
    texts: MutableList<String>,
    onNodeFound: (AccessibilityNodeInfo, String) -> Unit
  ) {
    val nodeText = node.text?.toString()?.trim()
    val contentDesc = node.contentDescription?.toString()?.trim()

    if (!nodeText.isNullOrEmpty()) {
      texts.add(nodeText)
      onNodeFound(node, nodeText.lowercase())
    }
    if (!contentDesc.isNullOrEmpty() && contentDesc != nodeText) {
      texts.add(contentDesc)
      onNodeFound(node, contentDesc.lowercase())
    }

    for (i in 0 until node.childCount) {
      val child = node.getChild(i) ?: continue
      traverseNodeHierarchy(child, texts, onNodeFound)
    }
  }

  private fun executeClick(node: AccessibilityNodeInfo?) {
    if (node == null) return
    var target: AccessibilityNodeInfo? = node
    while (target != null && !target.isClickable) {
      target = target.parent
    }
    if (target != null && target.isClickable) {
      target.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    } else {
      // Fallback click action
      node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }
  }

  override fun onInterrupt() {
    Log.d(TAG, "RidePilot Accessibility Service Interrupted")
  }

  override fun onDestroy() {
    super.onDestroy()
    AutomationManager.getInstance(this).setAccessibilityConnected(false)
  }
}
