package com.example.automation

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import com.example.model.DecisionType
import com.example.model.RideRequest
import java.util.Locale

class VoiceAssistant(context: Context) {

  private var tts: TextToSpeech? = null
  private var isInitialized = false
  private var isHindiAvailable = false

  init {
    try {
      tts = TextToSpeech(context.applicationContext) { status ->
        if (status == TextToSpeech.SUCCESS) {
          isInitialized = true
          val hiResult = tts?.setLanguage(Locale("hi", "IN"))
          isHindiAvailable = hiResult != TextToSpeech.LANG_MISSING_DATA && hiResult != TextToSpeech.LANG_NOT_SUPPORTED
          if (!isHindiAvailable) {
            tts?.setLanguage(Locale.ENGLISH)
          }
          tts?.setSpeechRate(1.05f)
        }
      }
    } catch (e: Exception) {
      Log.e("VoiceAssistant", "Failed to init TTS", e)
    }
  }

  fun speak(text: String, isHindiPreferred: Boolean = true) {
    if (!isInitialized || tts == null) return
    try {
      if (isHindiPreferred && isHindiAvailable) {
        tts?.setLanguage(Locale("hi", "IN"))
      } else {
        tts?.setLanguage(Locale.ENGLISH)
      }
      tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ride_pilot_voice_${System.currentTimeMillis()}")
    } catch (e: Exception) {
      Log.e("VoiceAssistant", "Error speaking text", e)
    }
  }

  fun announceRideDecision(
    request: RideRequest,
    decision: DecisionType,
    isHindi: Boolean = true
  ) {
    val platformName = request.platform.displayName
    val fareInt = request.fare.toInt()

    val text = if (isHindi) {
      when (decision) {
        DecisionType.ACCEPT -> "$platformName par ₹$fareInt ki ride automatically accept ho gayi hai."
        DecisionType.REJECT -> "$platformName par ₹$fareInt ki ride skip ki gayi. ${request.decisionReason}"
        DecisionType.MANUAL_REVIEW -> "$platformName par ₹$fareInt ki nayi ride aayi hai, kripya check karein."
        DecisionType.UNKNOWN -> "$platformName par nayi ride aayi hai."
      }
    } else {
      when (decision) {
        DecisionType.ACCEPT -> "Ride on $platformName for ₹$fareInt automatically accepted."
        DecisionType.REJECT -> "Ride on $platformName for ₹$fareInt skipped. ${request.decisionReason}"
        DecisionType.MANUAL_REVIEW -> "New ride on $platformName for ₹$fareInt needs your review."
        DecisionType.UNKNOWN -> "New ride request detected on $platformName."
      }
    }

    speak(text, isHindi)
  }

  fun shutdown() {
    try {
      tts?.stop()
      tts?.shutdown()
    } catch (_: Exception) {}
  }
}
