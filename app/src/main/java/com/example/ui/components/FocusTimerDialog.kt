package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ActiveTimerState
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusTimerDialog(
  timerState: ActiveTimerState,
  onPauseToggle: () -> Unit,
  onAddFiveMinutes: () -> Unit,
  onFinishAndLog: () -> Unit,
  onCancel: () -> Unit
) {
  if (!timerState.isRunning || timerState.subtask == null) return

  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val subtask = timerState.subtask
  val task = timerState.task

  val progress = if (timerState.totalSeconds > 0) {
    (timerState.totalSeconds - timerState.remainingSeconds).toFloat() / timerState.totalSeconds.toFloat()
  } else 1f

  val animatedProgress by animateFloatAsState(targetValue = progress, label = "timer_progress")

  val minutes = timerState.remainingSeconds / 60
  val seconds = timerState.remainingSeconds % 60
  val timeFormatted = String.format("%02d:%02d", minutes, seconds)

  ModalBottomSheet(
    onDismissRequest = onCancel,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
    modifier = Modifier.testTag("focus_timer_sheet")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp)
        .padding(bottom = 36.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Timer,
            contentDescription = null,
            tint = CyanAccent,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Active Focus Session",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
        }
        IconButton(onClick = onCancel) {
          Icon(Icons.Default.Close, contentDescription = "Minimize")
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Subtask & Task info
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          if (task != null) {
            Text(
              text = task.title,
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
          }
          Text(
            text = subtask.title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          if (subtask.actionableNotes.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = subtask.actionableNotes,
              fontSize = 11.sp,
              color = CyanAccent
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(28.dp))

      // Large Countdown Radial Dial
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(190.dp)
      ) {
        CircularProgressIndicator(
          progress = { 1f },
          modifier = Modifier.size(190.dp),
          color = MaterialTheme.colorScheme.surfaceVariant,
          strokeWidth = 10.dp
        )
        CircularProgressIndicator(
          progress = { animatedProgress },
          modifier = Modifier.size(190.dp),
          color = if (timerState.isPaused) MaterialTheme.colorScheme.outline else CyanAccent,
          strokeWidth = 10.dp
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
            text = timeFormatted,
            fontSize = 38.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = if (timerState.isPaused) "PAUSED" else "FOCUSING",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = if (timerState.isPaused) MaterialTheme.colorScheme.onSurfaceVariant else CyanAccent
          )
        }
      }

      Spacer(modifier = Modifier.height(30.dp))

      // Control Buttons
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        OutlinedButton(
          onClick = onAddFiveMinutes,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.weight(1f)
        ) {
          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("+5 Min", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }

        Button(
          onClick = onPauseToggle,
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = if (timerState.isPaused) IndigoPrimary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (timerState.isPaused) Color.White else MaterialTheme.colorScheme.onSurface
          ),
          modifier = Modifier.weight(1f)
        ) {
          Icon(
            imageVector = if (timerState.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(if (timerState.isPaused) "Resume" else "Pause", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      Button(
        onClick = onFinishAndLog,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = EmeraldSuccess,
          contentColor = Color.White
        ),
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
      ) {
        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Complete & Log Time Spent", fontSize = 14.sp, fontWeight = FontWeight.Bold)
      }
    }
  }
}
