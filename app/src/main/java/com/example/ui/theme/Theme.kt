package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = EmeraldGreen,
    secondary = ElectricCyan,
    tertiary = WarningAmber,
    background = SlateDarkBg,
    surface = SolidDarkSurface,
    onPrimary = PureWhiteSurface,
    onSecondary = PureWhiteSurface,
    onBackground = SoftLightBg,
    onSurface = SoftLightBg,
    outline = DesignOutlineDark,
    surfaceVariant = SolidDarkSurface,
    onSurfaceVariant = TextSlateMuted
  )

private val LightColorScheme =
  lightColorScheme(
    primary = EmeraldGreen,
    secondary = ElectricCyan,
    tertiary = WarningAmber,
    background = SoftLightBg, // Professional pristine light off-white background
    surface = PureWhiteSurface, // Pure professional white surface
    onPrimary = PureWhiteSurface,
    onSecondary = PureWhiteSurface,
    onBackground = TextSlatePrimary, // Deep slate elegant dark text
    onSurface = TextSlatePrimary,
    outline = DesignOutlineLight, // Soft grey boundary line
    surfaceVariant = SoftLightBg,
    onSurfaceVariant = TextSlateSecondary // Professional gray secondary text
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = false, // Always default to the pristine, professional white theme
  // Disable dynamic color by default to guarantee our pristine white-and-emerald curated experience
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
