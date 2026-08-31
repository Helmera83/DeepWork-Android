package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Priority
import com.example.data.model.SubTask
import com.example.data.model.Task
import com.example.data.model.TaskCategory
import com.example.ui.theme.BrandSecondary
import com.example.ui.theme.BrandTertiary
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricGreen
import com.example.ui.theme.HyperCyan

@Composable
fun BreakdownLabView(
  tasks: List<Task>,
  isGenerating: Boolean,
  onDecomposeGoal: (title: String, category: TaskCategory, priority: Priority, description: String, deadlineMillis: Long?) -> Unit,
  onToggleSubTask: (taskId: String, subTaskId: String) -> Unit,
  onEditTask: (Task) -> Unit,
  onDeleteTask: (Task) -> Unit,
  onAddSubtask: (Task) -> Unit,
  onGenerateMoreAi: (Task) -> Unit,
  onExportCalendar: (Task) -> Unit,
  onOpenVoiceAssistant: () -> Unit,
  onOpenManualAdd: () -> Unit,
  modifier: Modifier = Modifier
) {
  val totalEstimatedHours = remember(tasks) {
    val totalMinutes = tasks.flatMap { it.subtasks }.sumOf { it.estimatedMinutes }
    (totalMinutes / 60).coerceAtLeast(1)
  }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .testTag("breakdown_lab_view"),
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
    verticalArrangement = Arrangement.spacedBy(18.dp)
  ) {
    // 1. Header Section
    item {
      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(
          text = "Task Decomposition",
          fontSize = 28.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = (-0.5).sp,
          color = MaterialTheme.colorScheme.onSurface,
          textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "Break complex goals into manageable phases, time estimates, and actionable steps.",
          fontSize = 13.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
          lineHeight = 18.sp,
          modifier = Modifier.padding(horizontal = 12.dp)
        )
      }
    }

    // 2. Decomposition Input Card with Date Picker & AI Controls
    item {
      TaskBreakdownInputCard(
        isLoading = isGenerating,
        onInitiateBreakdown = { title, cat, prio, desc, deadline ->
          onDecomposeGoal(title, cat, prio, desc, deadline)
        },
        onOpenVoiceCoach = onOpenVoiceAssistant
      )
    }

    // 3. Complexity & Time Allocation Summary
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // Complexity Metric
        Surface(
          modifier = Modifier
            .weight(1f)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), RoundedCornerShape(14.dp)),
          color = MaterialTheme.colorScheme.surfaceContainer,
          shape = RoundedCornerShape(14.dp)
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Box(
              modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .border(2.dp, BrandSecondary, CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "${(tasks.size * 1.8).coerceIn(1.0, 9.9).toInt()}.${(tasks.size * 3) % 10}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = BrandSecondary
              )
            }
            Column {
              Text(
                text = "Complexity",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = if (tasks.size > 2) "Multi-Phase" else "Standard",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }

        // Time Allocation Metric
        Surface(
          modifier = Modifier
            .weight(1f)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), RoundedCornerShape(14.dp)),
          color = MaterialTheme.colorScheme.surfaceContainer,
          shape = RoundedCornerShape(14.dp)
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Box(
              modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(BrandTertiary.copy(alpha = 0.15f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = null,
                tint = BrandTertiary,
                modifier = Modifier.size(18.dp)
              )
            }
            Column {
              Text(
                text = "${totalEstimatedHours}h Allocated",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "${tasks.flatMap { it.subtasks }.size} Action Steps",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }
    }

    // 4. Section Title: Decomposed Tasks
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = null,
            tint = BrandSecondary,
            modifier = Modifier.size(18.dp)
          )
          Text(
            text = "Active Decomposed Tasks (${tasks.size})",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
        }

        Box(
          modifier = Modifier
            .clip(CircleShape)
            .background(BrandTertiary.copy(alpha = 0.12f))
            .border(1.dp, BrandTertiary.copy(alpha = 0.25f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 3.dp)
        ) {
          Text(
            text = "READY",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            color = BrandTertiary
          )
        }
      }
    }

    // 5. Tasks List with Edit & Delete Options
    if (tasks.isEmpty()) {
      item {
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Icon(
              imageVector = Icons.Default.FormatListBulleted,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
              modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "No tasks decomposed yet",
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Use the card above with a target due date to break down any goal!",
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center
            )
          }
        }
      }
    } else {
      items(tasks, key = { "breakdown_task_${it.id}" }) { task ->
        TaskCard(
          task = task,
          onToggleSubtask = { sub -> onToggleSubTask(task.id, sub.id) },
          onStartTimer = { },
          onDeleteSubtask = { sub -> onToggleSubTask(task.id, sub.id) },
          onDeleteTask = { onDeleteTask(task) },
          onEditTask = { onEditTask(task) },
          onExportCalendar = { onExportCalendar(task) },
          onAddSubtask = { onAddSubtask(task) },
          onGenerateAiSubtasks = { onGenerateMoreAi(task) }
        )
      }
    }
  }
}
