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
  primary = ElectricBlueLight,
  onPrimary = Color.White,
  primaryContainer = DeepNavyCardDark,
  onPrimaryContainer = Color(0xFFD6E4FF),
  secondary = ElectricCyanAccent,
  onSecondary = Color(0xFF003544),
  secondaryContainer = DeepNavyBorderDark,
  onSecondaryContainer = Color(0xFFC7F3FF),
  tertiary = StatusPending,
  background = DeepNavyBackgroundDark,
  onBackground = TextPrimaryDark,
  surface = DeepNavySurfaceDark,
  onSurface = TextPrimaryDark,
  surfaceVariant = DeepNavyCardDark,
  onSurfaceVariant = TextSecondaryDark,
  outline = DeepNavyBorderDark
)

private val LightColorScheme = lightColorScheme(
  primary = ElectricBluePrimary,
  onPrimary = Color.White,
  primaryContainer = Color(0xFFE5EFFF),
  onPrimaryContainer = Color(0xFF001F52),
  secondary = Color(0xFF0A192F),
  onSecondary = Color.White,
  secondaryContainer = Color(0xFFEEF2F6),
  onSecondaryContainer = Color(0xFF0A192F),
  tertiary = ElectricCyanAccent,
  background = SoftWhiteBackgroundLight,
  onBackground = TextPrimaryLight,
  surface = CardWhiteLight,
  onSurface = TextPrimaryLight,
  surfaceVariant = SurfaceVariantLight,
  onSurfaceVariant = TextSecondaryLight,
  outline = BorderLight
)

@Composable
fun MeetMindTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Set false by default to preserve the brand Navy & Electric Blue identity
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

// Backward compatibility alias
@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  MeetMindTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}
