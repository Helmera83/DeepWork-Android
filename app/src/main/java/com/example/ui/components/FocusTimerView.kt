package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Task
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.BrandSecondary
import com.example.ui.theme.BrandTertiary
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricGreen
import com.example.ui.theme.HyperCyan
import kotlinx.coroutines.delay

@Composable
fun FocusTimerView(
  tasks: List<Task>,
  onCompleteTask: (Task) -> Unit = {},
  modifier: Modifier = Modifier
) {
  var selectedTask by remember { mutableStateOf(tasks.firstOrNull { !it.isFullyCompleted }) }
  var totalDurationSeconds by remember { mutableIntStateOf(45 * 60) }
  var remainingSeconds by remember { mutableIntStateOf(45 * 60) }
  var isRunning by remember { mutableStateOf(false) }
  var selectedModeIndex by remember { mutableIntStateOf(0) } // 0: 45m Focus, 1: 25m Pomodoro, 2: 5m Break

  // Live timer tick
  LaunchedEffect(isRunning, remainingSeconds) {
    if (isRunning && remainingSeconds > 0) {
      delay(1000L)
      remainingSeconds--
    } else if (remainingSeconds <= 0 && isRunning) {
      isRunning = false
    }
  }

  val minutes = remainingSeconds / 60
  val seconds = remainingSeconds % 60
  val timeString = String.format("%02d:%02d", minutes, seconds)
  val progress = if (totalDurationSeconds > 0) {
    (totalDurationSeconds - remainingSeconds).toFloat() / totalDurationSeconds.toFloat()
  } else 0f

  val animatedProgress by animateFloatAsState(targetValue = progress, label = "timerProgress")

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .testTag("focus_timer_view"),
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(20.dp)
  ) {
    // 1. Header
    item {
      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(
          text = "Focus Mode",
          fontSize = 28.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = (-0.5).sp,
          color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "Eliminate distractions and enter a flow state.",
          fontSize = 14.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }

    // 2. Mode Pills (45m Focus, 25m Pomodoro, 5m Break)
    item {
      Row(
        modifier = Modifier
          .clip(RoundedCornerShape(20.dp))
          .background(MaterialTheme.colorScheme.surfaceContainer)
          .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        FocusModePill(
          label = "45m Deep",
          isSelected = selectedModeIndex == 0,
          onClick = {
            selectedModeIndex = 0
            totalDurationSeconds = 45 * 60
            remainingSeconds = 45 * 60
            isRunning = false
          }
        )
        FocusModePill(
          label = "25m Sprint",
          isSelected = selectedModeIndex == 1,
          onClick = {
            selectedModeIndex = 1
            totalDurationSeconds = 25 * 60
            remainingSeconds = 25 * 60
            isRunning = false
          }
        )
        FocusModePill(
          label = "5m Break",
          isSelected = selectedModeIndex == 2,
          onClick = {
            selectedModeIndex = 2
            totalDurationSeconds = 5 * 60
            remainingSeconds = 5 * 60
            isRunning = false
          }
        )
      }
    }

    // 3. Active Task Target Card
    item {
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(16.dp)
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Box(
            modifier = Modifier
              .size(38.dp)
              .clip(CircleShape)
              .background(BrandSecondary),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Timer,
              contentDescription = null,
              tint = HyperCyan,
              modifier = Modifier.size(20.dp)
            )
          }

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "TARGET TASK",
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.5.sp,
              color = BrandSecondary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = selectedTask?.title ?: "Finalize Q4 Strategy Deck",
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
          }
        }
      }
    }

    // 4. Large Circular Countdown Timer Ring
    item {
      Box(
        modifier = Modifier
          .size(260.dp)
          .padding(16.dp),
        contentAlignment = Alignment.Center
      ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
          val strokeWidth = 14.dp.toPx()
          val radius = (size.minDimension - strokeWidth) / 2
          
          // Background track
          drawCircle(
            color = Color(0xFFDBE4E3),
            radius = radius,
            style = Stroke(width = strokeWidth)
          )

          // Glowing active gradient arc
          drawArc(
            brush = Brush.sweepGradient(
              listOf(ElectricCyan, ElectricGreen, ElectricCyan)
            ),
            startAngle = -90f,
            sweepAngle = animatedProgress * 360f,
            useCenter = false,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
          )
        }

        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
        ) {
          Text(
            text = timeString,
            fontSize = 44.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-1).sp,
            color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = if (isRunning) "REMAINING" else "PAUSED",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = if (isRunning) BrandSecondary else MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }

    // 5. Timer Action Controls (Play/Pause, Reset, Complete)
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Reset Button
        IconButton(
          onClick = {
            isRunning = false
            remainingSeconds = totalDurationSeconds
          },
          modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .testTag("timer_reset_btn")
        ) {
          Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = "Reset Timer",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
          )
        }

        Spacer(modifier = Modifier.width(20.dp))

        // Play/Pause Main Button
        Button(
          onClick = { isRunning = !isRunning },
          modifier = Modifier
            .size(72.dp)
            .shadow(10.dp, CircleShape, ambientColor = HyperCyan.copy(alpha = 0.4f))
            .testTag("timer_play_pause_btn"),
          shape = CircleShape,
          colors = ButtonDefaults.buttonColors(
            containerColor = BrandSecondary,
            contentColor = HyperCyan
          )
        ) {
          Icon(
            imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = if (isRunning) "Pause" else "Start",
            tint = HyperCyan,
            modifier = Modifier.size(36.dp)
          )
        }

        Spacer(modifier = Modifier.width(20.dp))

        // Complete Task Button
        IconButton(
          onClick = {
            selectedTask?.let { onCompleteTask(it) }
            isRunning = false
          },
          modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .testTag("timer_finish_task_btn")
        ) {
          Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Complete Task",
            tint = BrandTertiary,
            modifier = Modifier.size(24.dp)
          )
        }
      }
    }
  }
}

@Composable
private fun FocusModePill(
  label: String,
  isSelected: Boolean,
  onClick: () -> Unit
) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(16.dp))
      .background(if (isSelected) BrandSecondary else Color.Transparent)
      .clickable(onClick = onClick)
      .padding(horizontal = 16.dp, vertical = 8.dp),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = label,
      fontSize = 12.sp,
      fontWeight = FontWeight.Bold,
      color = if (isSelected) HyperCyan else MaterialTheme.colorScheme.onSurfaceVariant
    )
  }
}
