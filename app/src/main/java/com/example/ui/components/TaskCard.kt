package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
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

/**
 * A streamlined, high-contrast task card featuring clean typography,
 * progress tracking, collapsible milestones, and subtask management.
 */
@Composable
fun TaskCard(
  task: Task,
  onToggleSubtask: (SubTask) -> Unit,
  onStartTimer: (SubTask) -> Unit,
  onDeleteSubtask: (SubTask) -> Unit,
  onDeleteTask: () -> Unit,
  onEditTask: (Task) -> Unit = {},
  onExportCalendar: () -> Unit,
  onAddSubtask: () -> Unit,
  onGenerateAiSubtasks: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  var isExpanded by remember { mutableStateOf(true) }
  val progress = task.completionProgress
  val animatedProgress by animateFloatAsState(targetValue = progress, label = "task_progress")

  val milestoneExpandedMap = remember(task.id) {
    mutableStateMapOf<String, Boolean>().apply {
      task.milestones.forEach { put(it.id, true) }
    }
  }

  OutlinedCard(
    modifier = modifier
      .fillMaxWidth()
      .testTag("task_card_${task.id}"),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.outlinedCardColors(
      containerColor = MaterialTheme.colorScheme.surface
    ),
    border = BorderStroke(
      width = 1.5.dp,
      color = MaterialTheme.colorScheme.outlineVariant
    )
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    ) {
      // Header: Category, Priority, Vault Badge & Actions (Edit & Delete)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          CategoryBadge(category = task.category)
          PriorityBadge(priority = task.priority)

          if (task.isEncrypted) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f))
                .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Encrypted",
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(11.dp)
              )
              Spacer(modifier = Modifier.width(3.dp))
              Text(
                text = "Encrypted",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.tertiary
              )
            }
          }
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          IconButton(
            onClick = { onEditTask(task) },
            modifier = Modifier
              .size(32.dp)
              .testTag("edit_task_btn_${task.id}")
          ) {
            Icon(
              imageVector = Icons.Default.Edit,
              contentDescription = "Edit Task",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(18.dp)
            )
          }

          IconButton(
            onClick = onDeleteTask,
            modifier = Modifier
              .size(32.dp)
              .testTag("delete_task_btn_${task.id}")
          ) {
            Icon(
              imageVector = Icons.Default.DeleteOutline,
              contentDescription = "Delete Task",
              tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
              modifier = Modifier.size(18.dp)
            )
          }
        }
      }


      Spacer(modifier = Modifier.height(8.dp))

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
          overflow = TextOverflow.Ellipsis,
          lineHeight = 18.sp
        )
      }

      // Deadline (if present)
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
            text = "Due $deadline",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = ExpressiveAmber
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Overall Progress Bar & Meta Summary
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
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "${task.completedSubtasksCount} of ${task.totalSubtasksCount} completed (${(progress * 100).toInt()}%)",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
          )
        }

        Text(
          text = "${task.totalEstimatedMinutes}m total",
          fontSize = 11.sp,
          fontWeight = FontWeight.Medium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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

      // Milestones Toggle & Calendar Export Action
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
            text = if (isExpanded) "Hide Steps (${task.subtasks.size})" else "Show Steps (${task.subtasks.size})",
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
          modifier = Modifier.height(32.dp),
          contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp),
          shape = RoundedCornerShape(10.dp),
          colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
          )
        ) {
          Icon(
            imageVector = Icons.Default.CalendarMonth,
            contentDescription = "Export to Calendar",
            modifier = Modifier.size(13.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text("Calendar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
      }

      // Milestones & Subtasks Section
      AnimatedVisibility(visible = isExpanded) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          val milestones = task.milestones
          if (milestones.isEmpty()) {
            Surface(
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
              Text(
                text = "No subtasks yet. Click below to add steps or generate AI milestones.",
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

          // Action Buttons: AI More Subtasks & Manual Add
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
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

  // Tone-based container colors for filled milestone cards
  val toneBackgroundColor = when {
    milestone.isCompleted -> EmeraldSuccess.copy(alpha = 0.12f)
    milestoneIndex % 4 == 0 -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    milestoneIndex % 4 == 1 -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
    milestoneIndex % 4 == 2 -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
    else -> MaterialTheme.colorScheme.surfaceVariant
  }

  val toneBadgeColor = when {
    milestone.isCompleted -> EmeraldSuccess
    milestoneIndex % 4 == 0 -> MaterialTheme.colorScheme.primary
    milestoneIndex % 4 == 1 -> MaterialTheme.colorScheme.secondary
    milestoneIndex % 4 == 2 -> MaterialTheme.colorScheme.tertiary
    else -> MaterialTheme.colorScheme.onSurfaceVariant
  }

  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("milestone_card_${milestone.id}"),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(
      containerColor = toneBackgroundColor
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp)
    ) {
      // Milestone Heading
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(8.dp))
          .clickable(onClick = onToggleExpand)
          .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = toneBadgeColor
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
              if (milestone.isCompleted) {
                Icon(
                  imageVector = Icons.Default.Check,
                  contentDescription = null,
                  tint = Color.White,
                  modifier = Modifier.size(10.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
              }
              Text(
                text = "Phase ${milestoneIndex + 1}",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
              )
            }
          }

          Spacer(modifier = Modifier.width(8.dp))

          Column {
            Text(
              text = milestone.title,
              fontSize = 13.sp,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )

            Text(
              text = "${milestone.completedSubtasksCount}/${milestone.totalSubtasksCount} steps completed • ${milestone.totalEstimatedMinutes}m",
              fontSize = 10.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        IconButton(
          onClick = onToggleExpand,
          modifier = Modifier.size(28.dp)
        ) {
          Icon(
            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = if (isExpanded) "Collapse" else "Expand",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(4.dp))
      LinearProgressIndicator(
        progress = { animatedMilestoneProgress },
        modifier = Modifier
          .fillMaxWidth()
          .height(3.dp)
          .clip(RoundedCornerShape(2.dp)),
        color = if (milestone.isCompleted) EmeraldSuccess else toneBadgeColor,
        trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
      )

      AnimatedVisibility(visible = isExpanded) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
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
internal fun CategoryBadge(category: TaskCategory) {
  Surface(
    shape = RoundedCornerShape(6.dp),
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
internal fun PriorityBadge(priority: Priority) {
  val (color, label) = when (priority) {
    Priority.LOW -> Pair(PriorityLowColor, "Low")
    Priority.MEDIUM -> Pair(PriorityMediumColor, "Med")
    Priority.HIGH -> Pair(PriorityHighColor, "High")
    Priority.URGENT -> Pair(PriorityUrgentColor, "Urgent")
  }

  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
      .clip(RoundedCornerShape(6.dp))
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
