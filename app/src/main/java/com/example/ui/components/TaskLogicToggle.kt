package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Custom Checkmark Toggle Switch matching the Productive Bold Light mockup design
 */
@Composable
fun TaskLogicToggle(
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  testTag: String = "task_logic_toggle"
) {
  val interactionSource = remember { MutableInteractionSource() }
  
  val trackColor by animateColorAsState(
    targetValue = if (checked) Color(0xFF00EAEA) else Color(0xFFB9CAC9),
    label = "trackColor"
  )
  
  val thumbOffset by animateFloatAsState(
    targetValue = if (checked) 24f else 3f,
    label = "thumbOffset"
  )

  Box(
    modifier = modifier
      .size(width = 54.dp, height = 30.dp)
      .clip(RoundedCornerShape(16.dp))
      .background(trackColor)
      .clickable(
        interactionSource = interactionSource,
        indication = null,
        enabled = enabled,
        onClick = { onCheckedChange(!checked) }
      )
      .testTag(testTag),
    contentAlignment = Alignment.CenterStart
  ) {
    // White Thumb circle
    Box(
      modifier = Modifier
        .offset { IntOffset(x = thumbOffset.dp.roundToPx(), y = 0) }
        .size(24.dp)
        .clip(CircleShape)
        .background(if (checked) Color(0xFF0072F5) else Color.White),
      contentAlignment = Alignment.Center
    ) {
      if (checked) {
        Icon(
          imageVector = Icons.Default.Check,
          contentDescription = null,
          tint = Color.White,
          modifier = Modifier.size(16.dp)
        )
      }
    }
  }
}
