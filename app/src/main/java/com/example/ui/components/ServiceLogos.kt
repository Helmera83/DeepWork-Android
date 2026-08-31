package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * High-fidelity vector logos for integrations matching the UI mockups
 */
@Composable
fun ServiceLogoIcon(
  serviceName: String,
  size: Dp = 48.dp,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .size(size)
      .clip(RoundedCornerShape(12.dp))
      .background(Color.White)
      .padding(8.dp),
    contentAlignment = Alignment.Center
  ) {
    when (serviceName.lowercase()) {
      "notion" -> NotionLogoCanvas(modifier = Modifier.fillMaxSize())
      "google calendar", "google" -> GoogleCalendarLogoCanvas(modifier = Modifier.fillMaxSize())
      "outlook", "microsoft" -> OutlookLogoCanvas(modifier = Modifier.fillMaxSize())
      "slack" -> SlackLogoCanvas(modifier = Modifier.fillMaxSize())
      else -> {
        Icon(
          imageVector = Icons.Default.Description,
          contentDescription = serviceName,
          tint = Color(0xFF2D6767),
          modifier = Modifier.fillMaxSize()
        )
      }
    }
  }
}

@Composable
fun NotionLogoCanvas(modifier: Modifier = Modifier) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    
    // Notion distinct stylized N
    val path = Path().apply {
      moveTo(w * 0.22f, h * 0.18f)
      lineTo(w * 0.38f, h * 0.15f)
      lineTo(w * 0.78f, h * 0.78f)
      lineTo(w * 0.78f, h * 0.22f)
      lineTo(w * 0.90f, h * 0.20f)
      lineTo(w * 0.90f, h * 0.85f)
      lineTo(w * 0.74f, h * 0.88f)
      lineTo(w * 0.34f, h * 0.25f)
      lineTo(w * 0.34f, h * 0.82f)
      lineTo(w * 0.22f, h * 0.85f)
      close()
    }
    drawPath(path = path, color = Color(0xFF0F172A), style = Fill)
    
    // Top serif accent
    drawRect(
      color = Color(0xFF0F172A),
      topLeft = Offset(w * 0.18f, h * 0.15f),
      size = Size(w * 0.12f, h * 0.08f)
    )
  }
}

@Composable
fun GoogleCalendarLogoCanvas(modifier: Modifier = Modifier) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    
    // Google Calendar 4-color grid square
    val blue = Color(0xFF4285F4)
    val red = Color(0xFFEA4335)
    val yellow = Color(0xFFFBBC05)
    val green = Color(0xFF34A853)
    
    val corner = 4.dp.toPx()
    
    // Base calendar background
    drawRoundRect(
      color = Color.White,
      topLeft = Offset(0f, 0f),
      size = Size(w, h),
      cornerRadius = CornerRadius(corner, corner)
    )
    
    // Top blue bar
    drawRoundRect(
      color = blue,
      topLeft = Offset(w * 0.15f, h * 0.15f),
      size = Size(w * 0.70f, h * 0.25f),
      cornerRadius = CornerRadius(corner / 2, corner / 2)
    )
    
    // Left blue side
    drawRect(
      color = blue,
      topLeft = Offset(w * 0.15f, h * 0.35f),
      size = Size(w * 0.18f, h * 0.50f)
    )
    // Bottom green
    drawRect(
      color = green,
      topLeft = Offset(w * 0.15f, h * 0.72f),
      size = Size(w * 0.70f, h * 0.15f)
    )
    // Right yellow & red
    drawRect(
      color = yellow,
      topLeft = Offset(w * 0.67f, h * 0.45f),
      size = Size(w * 0.18f, h * 0.40f)
    )
    drawRect(
      color = red,
      topLeft = Offset(w * 0.67f, h * 0.30f),
      size = Size(w * 0.18f, h * 0.20f)
    )
    
    // Center square (31 / date pip)
    drawRoundRect(
      color = blue,
      topLeft = Offset(w * 0.42f, h * 0.45f),
      size = Size(w * 0.16f, h * 0.18f),
      cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
    )
  }
}

@Composable
fun OutlookLogoCanvas(modifier: Modifier = Modifier) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    
    val outlookBlue = Color(0xFF0078D4)
    val darkBlue = Color(0xFF106EBE)
    val lightBlue = Color(0xFF2886DE)
    
    // Right envelope sheet
    drawRoundRect(
      color = outlookBlue,
      topLeft = Offset(w * 0.25f, h * 0.15f),
      size = Size(w * 0.65f, h * 0.70f),
      cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
    )
    
    // Inner envelope flap
    val flapPath = Path().apply {
      moveTo(w * 0.25f, h * 0.20f)
      lineTo(w * 0.58f, h * 0.50f)
      lineTo(w * 0.90f, h * 0.20f)
      close()
    }
    drawPath(flapPath, color = lightBlue)
    
    // Left 'O' disc
    drawRoundRect(
      color = darkBlue,
      topLeft = Offset(w * 0.08f, h * 0.25f),
      size = Size(w * 0.45f, h * 0.50f),
      cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
    )
    
    // White 'O' letter center
    drawRoundRect(
      color = Color.White,
      topLeft = Offset(w * 0.18f, h * 0.35f),
      size = Size(w * 0.25f, h * 0.30f),
      cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
    )
    drawRoundRect(
      color = darkBlue,
      topLeft = Offset(w * 0.23f, h * 0.41f),
      size = Size(w * 0.15f, h * 0.18f),
      cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
    )
  }
}

@Composable
fun SlackLogoCanvas(modifier: Modifier = Modifier) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    
    val red = Color(0xFFE01E5A)
    val green = Color(0xFF2EB67D)
    val blue = Color(0xFF36C5F0)
    val yellow = Color(0xFFECB22E)
    
    val pillW = w * 0.14f
    val pillH = h * 0.32f
    val dotR = w * 0.07f
    val rad = CornerRadius(pillW / 2, pillW / 2)
    
    // Top-left Red pill + dot
    drawRoundRect(color = red, topLeft = Offset(w * 0.38f, h * 0.10f), size = Size(pillW, pillH), cornerRadius = rad)
    drawCircle(color = red, radius = dotR, center = Offset(w * 0.22f, h * 0.22f))
    
    // Top-right Green pill + dot
    drawRoundRect(color = green, topLeft = Offset(w * 0.58f, h * 0.38f), size = Size(pillH, pillW), cornerRadius = rad)
    drawCircle(color = green, radius = dotR, center = Offset(w * 0.78f, h * 0.22f))
    
    // Bottom-right Yellow pill + dot
    drawRoundRect(color = yellow, topLeft = Offset(w * 0.48f, h * 0.58f), size = Size(pillW, pillH), cornerRadius = rad)
    drawCircle(color = yellow, radius = dotR, center = Offset(w * 0.78f, h * 0.78f))
    
    // Bottom-left Blue pill + dot
    drawRoundRect(color = blue, topLeft = Offset(w * 0.10f, h * 0.48f), size = Size(pillH, pillW), cornerRadius = rad)
    drawCircle(color = blue, radius = dotR, center = Offset(w * 0.22f, h * 0.78f))
  }
}
