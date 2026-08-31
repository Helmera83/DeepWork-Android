package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ==========================================
// TaskLogic AI - Productive Bold Light Palette
// ==========================================

// Core Brand Tones
val BrandPrimary = Color(0xFF006A6A)
val BrandPrimaryContainer = Color(0xFF00EAEA)
val BrandOnPrimaryContainer = Color(0xFF006565)
val BrandSecondary = Color(0xFF2D6767)
val BrandSecondaryContainer = Color(0xFFB0EAEA)
val BrandOnSecondaryContainer = Color(0xFF316B6C)

// Tertiary (Soft Lavender & Violet Accents)
val BrandTertiary = Color(0xFF5E5984)
val BrandTertiaryContainer = Color(0xFFD3CDFF)
val BrandOnTertiaryContainer = Color(0xFF595580)

// Electric Accents & AI Glows
val HyperCyan = Color(0xFF00EAEA)
val ElectricCyan = Color(0xFF00DBE9)
val ElectricGreen = Color(0xFF4EDEA3)
val SoftLavender = Color(0xFFD3CDFF)
val DeepViolet = Color(0xFF5E5984)

// Surface & Neutral Layers (Light Foundation)
val SurfaceBase = Color(0xFFF2FBFA)
val SurfaceContainerLowest = Color(0xFFFFFFFF)
val SurfaceContainerLow = Color(0xFFECF5F4)
val SurfaceContainerDefault = Color(0xFFE7F0EE)
val SurfaceContainerHigh = Color(0xFFE1EAE9)
val SurfaceContainerHighest = Color(0xFFDBE4E3)

// Text & Borders
val TextPrimary = Color(0xFF151D1D)
val TextSecondary = Color(0xFF3B4A49)
val TextMuted = Color(0xFF6A7A7A)
val BorderOutline = Color(0xFF6A7A7A)
val BorderOutlineVariant = Color(0xFFB9CAC9)

// Dark Theme Variants
val DarkBackground = Color(0xFF101818)
val DarkSurface = Color(0xFF151D1D)
val DarkSurfaceContainer = Color(0xFF1D2626)
val DarkSurfaceContainerHigh = Color(0xFF242E2E)
val DarkBorderOutline = Color(0xFF3B4A49)
val DarkBorderOutlineVariant = Color(0xFF2A3635)
val DarkTextPrimary = Color(0xFFF2FBFA)
val DarkTextSecondary = Color(0xFFB9CAC9)

// Status & Semantic Colors
val ExpressiveCoral = Color(0xFFBA1A1A)
val ExpressiveCoralDark = Color(0xFFFFB4AB)
val ExpressiveEmerald = Color(0xFF006A6A)
val EmeraldSuccess = Color(0xFF10B981)
val ExpressiveAmber = Color(0xFFD97706)
val PriorityLowColor = Color(0xFF10B981)
val PriorityMediumColor = Color(0xFF006A6A)
val PriorityHighColor = Color(0xFF5E5984)
val PriorityUrgentColor = Color(0xFFBA1A1A)

// Gradients & AI Progress Brushes
val ProgressGradientBrush = Brush.horizontalGradient(
  listOf(ElectricCyan, ElectricGreen)
)

val CardGlowBorderBrush = Brush.horizontalGradient(
  listOf(HyperCyan.copy(alpha = 0.6f), ElectricGreen.copy(alpha = 0.6f))
)

// Legacy Aliases
val BaselinePrimary = BrandPrimary
val BaselinePrimaryLight = BrandPrimary
val BaselinePrimaryDark = HyperCyan
val BaselinePrimaryContainerLight = BrandPrimaryContainer
val BaselinePrimaryContainerDark = BrandSecondary
val ToneSlateSecondary = BrandSecondary
val ToneSlateSecondaryDark = BrandSecondaryContainer
val ToneTealTertiary = BrandTertiary
val ToneTealTertiaryDark = BrandTertiaryContainer
val SleekLavenderPrimary = BrandTertiary
val SleekCyanAccent = HyperCyan
val IndigoPrimary = BrandSecondary
val CyanAccent = HyperCyan
val VioletAccent = BrandTertiary
val RoseAccent = ExpressiveCoral
val SleekPurpleContainer = BrandTertiaryContainer






