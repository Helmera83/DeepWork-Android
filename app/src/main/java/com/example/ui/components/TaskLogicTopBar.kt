package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BrandSecondary
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.HyperCyan

@Composable
fun TaskLogicTopBar(
  title: String = "TaskLogic AI",
  hasNotifications: Boolean = true,
  onAvatarClick: () -> Unit = {},
  onNotificationClick: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  Surface(
    modifier = modifier
      .fillMaxWidth()
      .height(64.dp)
      .testTag("task_logic_top_bar"),
    color = MaterialTheme.colorScheme.background,
    shadowElevation = 0.dp
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Left: Circular Avatar
      Box(
        modifier = Modifier
          .size(36.dp)
          .clip(CircleShape)
          .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
          .background(MaterialTheme.colorScheme.surfaceContainerHigh)
          .clickable(onClick = onAvatarClick)
          .testTag("topbar_avatar_btn"),
        contentAlignment = Alignment.Center
      ) {
        // High-tech avatar silhouette with cyan gradient
        Canvas(modifier = Modifier.size(36.dp)) {
          val w = size.width
          val h = size.height
          
          // Background subtle gradient
          drawCircle(
            brush = Brush.radialGradient(
              colors = listOf(Color(0xFF2D6767), Color(0xFF151D1D))
            )
          )
          
          // Head silhouette
          drawCircle(
            color = Color(0xFFEAF3F1),
            radius = w * 0.22f,
            center = Offset(w * 0.5f, h * 0.38f)
          )
          // Shoulder / suit silhouette
          drawArc(
            color = Color(0xFFEAF3F1),
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(w * 0.18f, h * 0.55f),
            size = androidx.compose.ui.geometry.Size(w * 0.64f, h * 0.64f)
          )
        }
      }

      Spacer(modifier = Modifier.width(12.dp))

      // Center/Left: App Title
      Text(
        text = title,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.2).sp,
        color = BrandSecondary,
        modifier = Modifier.weight(1f)
      )

      // Right: Notifications Button
      IconButton(
        onClick = onNotificationClick,
        modifier = Modifier.testTag("topbar_notification_btn")
      ) {
        BadgedBox(
          badge = {
            if (hasNotifications) {
              Badge(
                containerColor = Color(0xFF00EAEA),
                contentColor = Color(0xFF002020),
                modifier = Modifier.size(6.dp)
              )
            }
          }
        ) {
          Icon(
            imageVector = Icons.Default.Notifications,
            contentDescription = "Notifications",
            tint = BrandSecondary,
            modifier = Modifier.size(24.dp)
          )
        }
      }
    }
  }
}
