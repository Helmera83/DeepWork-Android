package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Milestone
import com.example.data.model.Priority
import com.example.data.model.SubTask
import com.example.data.model.Task
import com.example.data.model.TaskCategory
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.ExpressiveAmber
import com.example.ui.theme.PriorityHighColor
import com.example.ui.theme.PriorityLowColor
import com.example.ui.theme.PriorityMediumColor
import com.example.ui.theme.PriorityUrgentColor

@Composable
fun TaskCard(
  task: Task,
  onToggleSubtask: (SubTask) -> Unit,
  onStartTimer: (SubTask) -> Unit,
  onDeleteSubtask: (SubTask) -> Unit,
  onDeleteTask: () -> Unit,
  onExportCalendar: () -> Unit,
  onAddSubtask: () -> Unit,
  onGenerateAiSubtasks: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  var isExpanded by remember { mutableStateOf(true) }
  val progress = task.completionProgress
  val animatedProgress by animateFloatAsState(targetValue = progress, label = "task_progress")

  // State to track expansion of individual milestones
  val milestoneExpandedMap = remember(task.id) {
    mutableStateMapOf<String, Boolean>().apply {
      task.milestones.forEach { put(it.id, true) }
    }
  }

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .testTag("task_card_${task.id}"),
    shape = RoundedCornerShape(22.dp),
    color = MaterialTheme.colorScheme.surface,
    border = androidx.compose.foundation.BorderStroke(
      1.dp,
      MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    ),
    tonalElevation = 2.dp,
    shadowElevation = 3.dp
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    ) {
      // Header: L1 Root Goal Badge, Category, Priority, Vault Badge, and Delete
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          // Level 1 Root Goal Badge
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primaryContainer
          ) {
            Text(
              text = "L1 ROOT GOAL",
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.5.sp,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
            )
          }

          CategoryBadge(category = task.category)
          PriorityBadge(priority = task.priority)

          if (task.isEncrypted) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f))
                .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Encrypted",
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(11.dp)
              )
              Spacer(modifier = Modifier.width(3.dp))
              Text(
                text = "Vault",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.tertiary
              )
            }
          }
        }

        IconButton(
          onClick = onDeleteTask,
          modifier = Modifier.size(28.dp)
        ) {
          Icon(
            imageVector = Icons.Default.DeleteOutline,
            contentDescription = "Delete Task",
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(16.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Task Title
      Text(
        text = task.title,
        fontSize = 17.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.clickable { isExpanded = !isExpanded }
      )

      if (task.description.isNotBlank()) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = task.description,
          fontSize = 13.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = if (isExpanded) 3 else 1,
          overflow = TextOverflow.Ellipsis
        )
      }

      // Deadline (if set)
      task.formattedDeadline()?.let { deadline ->
        Spacer(modifier = Modifier.height(6.dp))
        Row(
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.Event,
            contentDescription = "Due Date",
            tint = ExpressiveAmber,
            modifier = Modifier.size(13.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "Target Deadline: $deadline",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = ExpressiveAmber
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Overall Progress bar & Rollup Across All Milestones
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = if (task.isFullyCompleted) EmeraldSuccess else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(14.dp)
          )
          Spacer(modifier = Modifier.width(5.dp))
          Text(
            text = "${task.completedSubtasksCount} of ${task.totalSubtasksCount} sub-tasks (${(progress * 100).toInt()}%)",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
          )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Schedule,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(13.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "${task.totalEstimatedMinutes}m est. total",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      LinearProgressIndicator(
        progress = { animatedProgress },
        modifier = Modifier
          .fillMaxWidth()
          .height(6.dp)
          .clip(RoundedCornerShape(3.dp)),
        color = if (task.isFullyCompleted) EmeraldSuccess else MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.surfaceVariant
      )

      Spacer(modifier = Modifier.height(10.dp))

      // Expand / Collapse Bar & Quick Calendar Sync Action
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { isExpanded = !isExpanded }
            .padding(vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = if (isExpanded) "Hide Milestones (${task.milestones.size})" else "View Milestones (${task.milestones.size})",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
          )
          Icon(
            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
          )
        }

        FilledTonalButton(
          onClick = onExportCalendar,
          modifier = Modifier.height(30.dp),
          contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp),
          shape = RoundedCornerShape(10.dp),
          colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
          )
        ) {
          Icon(
            imageVector = Icons.Default.CalendarMonth,
            contentDescription = "Sync to Calendar",
            modifier = Modifier.size(13.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text("Calendar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
      }

      // Milestones Breakdown List (Each Milestone is a Toggled Heading with Subtasks & Progress Rollup)
      AnimatedVisibility(visible = isExpanded) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          val milestones = task.milestones
          if (milestones.isEmpty()) {
            Surface(
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ) {
              Text(
                text = "No subtasks yet. Click below to generate AI milestones or add steps manually.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(14.dp)
              )
            }
          } else {
            milestones.forEachIndexed { milestoneIndex, milestone ->
              val isMilestoneExpanded = milestoneExpandedMap[milestone.id] ?: true
              MilestoneSection(
                milestone = milestone,
                milestoneIndex = milestoneIndex,
                totalMilestones = milestones.size,
                isExpanded = isMilestoneExpanded,
                onToggleExpand = {
                  milestoneExpandedMap[milestone.id] = !isMilestoneExpanded
                },
                onToggleSubtask = onToggleSubtask,
                onStartTimer = onStartTimer,
                onDeleteSubtask = onDeleteSubtask
              )
            }
          }

          // Subtask addition action row (AI + Manual)
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            // AI Generate More Button
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.primaryContainer,
              border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
              ),
              modifier = Modifier
                .weight(1.2f)
                .height(36.dp)
                .clickable(onClick = onGenerateAiSubtasks)
            ) {
              Row(
                modifier = Modifier
                  .fillMaxSize()
                  .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
              ) {
                Icon(
                  imageVector = Icons.Default.AutoAwesome,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onPrimaryContainer,
                  modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                  text = "+ AI Sub-tasks",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onPrimaryContainer
                )
              }
            }

            // Manual Add Step Button
            OutlinedButton(
              onClick = onAddSubtask,
              modifier = Modifier
                .weight(1f)
                .height(36.dp),
              shape = RoundedCornerShape(12.dp),
              contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
            ) {
              Text("+ Step", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
          }
        }
      }
    }
  }
}

/**
 * Milestone Section Component:
 * - Rendered as a toggled/collapsible heading.
 * - Displays progress rollup (completed count, percentage, progress bar, estimated time) from its child subtasks.
 * - Checkboxes are exclusively on subtasks (Level 2 and subsequent levels), not on the milestone header itself.
 */
@Composable
private fun MilestoneSection(
  milestone: Milestone,
  milestoneIndex: Int,
  totalMilestones: Int,
  isExpanded: Boolean,
  onToggleExpand: () -> Unit,
  onToggleSubtask: (SubTask) -> Unit,
  onStartTimer: (SubTask) -> Unit,
  onDeleteSubtask: (SubTask) -> Unit,
  modifier: Modifier = Modifier
) {
  val animatedMilestoneProgress by animateFloatAsState(
    targetValue = milestone.completionProgress,
    label = "milestone_progress_${milestone.id}"
  )

  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
    border = androidx.compose.foundation.BorderStroke(
      1.dp,
      if (milestone.isCompleted) {
        EmeraldSuccess.copy(alpha = 0.4f)
      } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
      }
    ),
    tonalElevation = 1.dp
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp)
    ) {
      // Milestone Toggled Heading
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(10.dp))
          .clickable(onClick = onToggleExpand)
          .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          // 1st Level Milestone Badge (NO checkbox on milestone heading itself!)
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (milestone.isCompleted) EmeraldSuccess else MaterialTheme.colorScheme.secondaryContainer
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
              if (milestone.isCompleted) {
                Icon(
                  imageVector = Icons.Default.Check,
                  contentDescription = "Milestone Completed",
                  tint = Color.White,
                  modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
              } else {
                Icon(
                  imageVector = Icons.Default.Flag,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onSecondaryContainer,
                  modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
              }
              Text(
                text = "M${milestoneIndex + 1}",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (milestone.isCompleted) Color.White else MaterialTheme.colorScheme.onSecondaryContainer
              )
            }
          }

          Spacer(modifier = Modifier.width(8.dp))

          Column {
            Text(
              text = milestone.title,
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )

            // Progress Rollup on the 1st Level Milestone Heading
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Text(
                text = "${milestone.completedSubtasksCount}/${milestone.totalSubtasksCount} completed • ${(milestone.completionProgress * 100).toInt()}%",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (milestone.isCompleted) EmeraldSuccess else MaterialTheme.colorScheme.primary
              )
              Text(
                text = "•",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Text(
                text = "${milestone.totalEstimatedMinutes}m",
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }

        IconButton(
          onClick = onToggleExpand,
          modifier = Modifier.size(24.dp)
        ) {
          Icon(
            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = if (isExpanded) "Collapse Milestone" else "Expand Milestone",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
          )
        }
      }

      // Milestone Mini Progress Rollup Bar
      Spacer(modifier = Modifier.height(6.dp))
      LinearProgressIndicator(
        progress = { animatedMilestoneProgress },
        modifier = Modifier
          .fillMaxWidth()
          .height(4.dp)
          .clip(RoundedCornerShape(2.dp)),
        color = if (milestone.isCompleted) EmeraldSuccess else MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.surfaceVariant
      )

      // Milestone Subtasks List (Checkboxes at Level 2 and subsequent levels)
      AnimatedVisibility(visible = isExpanded) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          milestone.subtasks.forEachIndexed { subIndex, subtask ->
            SubTaskItem(
              subtask = subtask,
              stepIndex = subIndex,
              totalSteps = milestone.subtasks.size,
              onToggle = { onToggleSubtask(subtask) },
              onStartTimer = { onStartTimer(subtask) },
              onDelete = { onDeleteSubtask(subtask) }
            )
          }
        }
      }
    }
  }
}

@Composable
private fun CategoryBadge(category: TaskCategory) {
  Surface(
    shape = RoundedCornerShape(8.dp),
    color = MaterialTheme.colorScheme.surfaceVariant
  ) {
    Text(
      text = category.label,
      fontSize = 10.sp,
      fontWeight = FontWeight.SemiBold,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
    )
  }
}

@Composable
private fun PriorityBadge(priority: Priority) {
  val (color, label) = when (priority) {
    Priority.LOW -> Pair(PriorityLowColor, "Low")
    Priority.MEDIUM -> Pair(PriorityMediumColor, "Med")
    Priority.HIGH -> Pair(PriorityHighColor, "High")
    Priority.URGENT -> Pair(PriorityUrgentColor, "Urgent")
  }

  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
      .clip(RoundedCornerShape(8.dp))
      .background(color.copy(alpha = 0.12f))
      .padding(horizontal = 7.dp, vertical = 3.dp)
  ) {
    Box(
      modifier = Modifier
        .size(6.dp)
        .clip(CircleShape)
        .background(color)
    )
    Spacer(modifier = Modifier.width(4.dp))
    Text(
      text = label,
      fontSize = 10.sp,
      fontWeight = FontWeight.Bold,
      color = color
    )
  }
}
