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

enum class AppThemeMode {
  SYSTEM,
  DARK,
  LIGHT
}

private val DarkColorScheme =
  darkColorScheme(
    primary = BaselinePrimaryDark,
    onPrimary = Color(0xFF0D243B),
    primaryContainer = BaselinePrimaryContainerDark, // #263a52
    onPrimaryContainer = Color(0xFFD6E3F3),
    secondary = ToneSlateSecondaryDark,
    onSecondary = Color(0xFF14202F),
    secondaryContainer = ToneSlateSecondaryContainerDark,
    onSecondaryContainer = Color(0xFFDCE8F6),
    tertiary = ToneTealTertiaryDark,
    onTertiary = Color(0xFF082631),
    tertiaryContainer = ToneTealTertiaryContainerDark,
    onTertiaryContainer = Color(0xFFD5EDF5),
    background = ToneDarkBackground,
    onBackground = ToneDarkTextPrimary,
    surface = ToneDarkSurface,
    onSurface = ToneDarkTextPrimary,
    surfaceVariant = ToneDarkSurfaceVariant,
    onSurfaceVariant = ToneDarkTextSecondary,
    surfaceContainer = ToneDarkSurfaceContainer,
    surfaceContainerHigh = ToneDarkSurfaceContainerHigh,
    outline = ToneDarkBorder,
    outlineVariant = Color(0xFF2C3746),
    error = ExpressiveCoralDark,
    onError = Color(0xFF4C0519)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = BaselinePrimaryLight, // #263a52
    onPrimary = Color.White,
    primaryContainer = BaselinePrimaryContainerLight,
    onPrimaryContainer = Color(0xFF0F1E2E),
    secondary = ToneSlateSecondary,
    onSecondary = Color.White,
    secondaryContainer = ToneSlateSecondaryContainer,
    onSecondaryContainer = Color(0xFF14202F),
    tertiary = ToneTealTertiary,
    onTertiary = Color.White,
    tertiaryContainer = ToneTealTertiaryContainer,
    onTertiaryContainer = Color(0xFF082631),
    background = ToneLightBackground,
    onBackground = ToneLightTextPrimary,
    surface = ToneLightSurface,
    onSurface = ToneLightTextPrimary,
    surfaceVariant = ToneLightSurfaceVariant,
    onSurfaceVariant = ToneLightTextSecondary,
    surfaceContainer = ToneLightSurfaceContainer,
    surfaceContainerHigh = ToneLightSurfaceContainerHigh,
    outline = ToneLightBorder,
    outlineVariant = Color(0xFFE0E7EE),
    error = ExpressiveCoral,
    onError = Color.White
  )

@Composable
fun TaskBreakTheme(
  themeMode: AppThemeMode = AppThemeMode.SYSTEM,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val systemDark = isSystemInDarkTheme()
  val darkTheme = when (themeMode) {
    AppThemeMode.SYSTEM -> systemDark
    AppThemeMode.DARK -> true
    AppThemeMode.LIGHT -> false
  }

  val colorScheme =
    when {
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

