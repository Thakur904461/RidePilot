package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
  primary = NeonGreenPrimary,
  onPrimary = Color(0xFF041C0C),
  primaryContainer = Color(0xFF00391A),
  onPrimaryContainer = Color(0xFF6BFF9E),
  secondary = ElectricCyanSecondary,
  onSecondary = Color(0xFF002F3A),
  secondaryContainer = Color(0xFF004E5F),
  onSecondaryContainer = Color(0xFF7DEFFF),
  tertiary = AmberWarning,
  onTertiary = Color(0xFF332000),
  background = DarkBackground,
  onBackground = DarkTextPrimary,
  surface = DarkSurface,
  onSurface = DarkTextPrimary,
  surfaceVariant = DarkSurfaceElevated,
  onSurfaceVariant = DarkTextSecondary,
  outline = DarkSurfaceBorder,
  error = CoralReject,
  onError = Color.White
)

private val LightColorScheme = lightColorScheme(
  primary = NeonGreenVariant,
  onPrimary = Color.White,
  primaryContainer = Color(0xFFC7FCE0),
  onPrimaryContainer = Color(0xFF00210C),
  secondary = ElectricCyanVariant,
  onSecondary = Color.White,
  secondaryContainer = Color(0xFFC4EFFF),
  onSecondaryContainer = Color(0xFF001F29),
  tertiary = AmberWarning,
  onTertiary = Color.White,
  background = LightBackground,
  onBackground = LightTextPrimary,
  surface = LightSurface,
  onSurface = LightTextPrimary,
  surfaceVariant = LightSurfaceElevated,
  onSurfaceVariant = LightTextSecondary,
  outline = LightSurfaceBorder,
  error = CoralReject,
  onError = Color.White
)

@Composable
fun RidePilotTheme(
  darkTheme: Boolean = true, // Default to dark fintech cockpit theme
  dynamicColor: Boolean = false, // Keep high-contrast custom brand palette
  content: @Composable () -> Unit,
) {
  val colorScheme = when {
    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      val context = LocalContext.current
      if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    darkTheme -> DarkColorScheme
    else -> LightColorScheme
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
