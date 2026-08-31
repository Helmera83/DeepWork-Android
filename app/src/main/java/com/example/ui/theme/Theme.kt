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
    primary = HyperCyan, // 0xFF00EAEA
    onPrimary = Color(0xFF002020),
    primaryContainer = BrandSecondary, // 0xFF2D6767
    onPrimaryContainer = BrandSecondaryContainer, // 0xFFB0EAEA
    secondary = BrandSecondaryContainer, // 0xFFB0EAEA
    onSecondary = Color(0xFF002020),
    secondaryContainer = BrandSecondary,
    onSecondaryContainer = Color(0xFFEAF3F1),
    tertiary = BrandTertiaryContainer, // 0xFFD3CDFF
    onTertiary = Color(0xFF1A163D),
    tertiaryContainer = BrandTertiary,
    onTertiaryContainer = Color(0xFFE4DFFF),
    background = DarkBackground, // 0xFF101818
    onBackground = DarkTextPrimary, // 0xFFF2FBFA
    surface = DarkSurface, // 0xFF151D1D
    onSurface = DarkTextPrimary, // 0xFFF2FBFA
    surfaceVariant = DarkSurfaceContainerHigh,
    onSurfaceVariant = DarkTextSecondary,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceContainerHighest = Color(0xFF2E3B3A),
    surfaceContainerLow = Color(0xFF121919),
    outline = DarkBorderOutline,
    outlineVariant = DarkBorderOutlineVariant,
    error = ExpressiveCoralDark,
    onError = Color(0xFF450A0A)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = BrandPrimary, // 0xFF006A6A
    onPrimary = Color.White,
    primaryContainer = BrandPrimaryContainer, // 0xFF00EAEA
    onPrimaryContainer = BrandOnPrimaryContainer, // 0xFF006565
    secondary = BrandSecondary, // 0xFF2D6767
    onSecondary = Color.White,
    secondaryContainer = BrandSecondaryContainer, // 0xFFB0EAEA
    onSecondaryContainer = BrandOnSecondaryContainer, // 0xFF316B6C
    tertiary = BrandTertiary, // 0xFF5E5984
    onTertiary = Color.White,
    tertiaryContainer = BrandTertiaryContainer, // 0xFFD3CDFF
    onTertiaryContainer = BrandOnTertiaryContainer, // 0xFF595580
    background = SurfaceBase, // 0xFFF2FBFA
    onBackground = TextPrimary, // 0xFF151D1D
    surface = SurfaceBase, // 0xFFF2FBFA
    onSurface = TextPrimary, // 0xFF151D1D
    surfaceVariant = SurfaceContainerHighest, // 0xFFDBE4E3
    onSurfaceVariant = TextSecondary, // 0xFF3B4A49
    surfaceContainer = SurfaceContainerDefault, // 0xFFE7F0EE
    surfaceContainerHigh = SurfaceContainerHigh, // 0xFFE1EAE9
    surfaceContainerHighest = SurfaceContainerHighest, // 0xFFDBE4E3
    surfaceContainerLow = SurfaceContainerLow, // 0xFFECF5F4
    outline = BorderOutline, // 0xFF6A7A7A
    outlineVariant = BorderOutlineVariant, // 0xFFB9CAC9
    error = ExpressiveCoral,
    onError = Color.White
  )

@Composable
fun TaskBreakTheme(
  themeMode: AppThemeMode = AppThemeMode.LIGHT,
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



