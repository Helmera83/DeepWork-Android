package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Priority
import com.example.data.model.SubTask
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.PriorityHighColor
import com.example.ui.theme.PriorityLowColor
import com.example.ui.theme.PriorityMediumColor
import com.example.ui.theme.PriorityUrgentColor

/**
 * A clean, spacious, uncluttered SubTask item with high-contrast text,
 * accessible touch targets, and clear status indicators.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SubTaskItem(
  subtask: SubTask,
  stepIndex: Int = 0,
  totalSteps: Int = 1,
  onToggle: () -> Unit,
  onStartTimer: () -> Unit,
  onDelete: () -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    modifier = modifier
      .fillMaxWidth()
      .testTag("subtask_item_${subtask.id}"),
    shape = RoundedCornerShape(12.dp),
    color = if (subtask.isCompleted) {
      MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
    } else {
      MaterialTheme.colorScheme.surface
    },
    tonalElevation = if (subtask.isCompleted) 0.dp else 1.dp
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Accessible Checkbox Touch Target
      Box(
        modifier = Modifier
          .size(36.dp)
          .clickable(onClick = onToggle)
          .testTag("subtask_checkbox_${subtask.id}"),
        contentAlignment = Alignment.Center
      ) {
        Box(
          modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(
              if (subtask.isCompleted) EmeraldSuccess else Color.Transparent
            )
            .then(
              if (!subtask.isCompleted) {
                Modifier.background(
                  color = Color.Transparent,
                  shape = CircleShape
                )
              } else Modifier
            ),
          contentAlignment = Alignment.Center
        ) {
          if (subtask.isCompleted) {
            Icon(
              imageVector = Icons.Default.Check,
              contentDescription = "Completed",
              tint = Color.White,
              modifier = Modifier.size(16.dp)
            )
          } else {
            Surface(
              shape = CircleShape,
              color = Color.Transparent,
              border = androidx.compose.foundation.BorderStroke(
                2.dp,
                MaterialTheme.colorScheme.outline
              ),
              modifier = Modifier.size(22.dp)
            ) {}
          }
        }
      }

      Spacer(modifier = Modifier.width(10.dp))

      // Main Content: Step number, Title, and concise Metadata
      Column(
        modifier = Modifier
          .weight(1f)
          .padding(vertical = 2.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Text(
            text = "${stepIndex + 1}.",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (subtask.isCompleted) {
              MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            } else {
              MaterialTheme.colorScheme.primary
            }
          )

          Text(
            text = subtask.title,
            fontSize = 14.sp,
            fontWeight = if (subtask.isCompleted) FontWeight.Normal else FontWeight.SemiBold,
            color = if (subtask.isCompleted) {
              MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            } else {
              MaterialTheme.colorScheme.onSurface
            },
            textDecoration = if (subtask.isCompleted) TextDecoration.LineThrough else TextDecoration.None
          )
        }

        // Actionable guidance notes (if any)
        if (subtask.actionableNotes.isNotBlank() && !subtask.isCompleted) {
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = subtask.actionableNotes,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 16.sp
          )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Concise Metadata Pills (Time & Priority)
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          // Estimated Time Badge
          Row(
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
              .padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.Schedule,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.size(11.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
              text = "${subtask.estimatedMinutes}m",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onPrimaryContainer
            )
          }

          // Priority Badge
          val prioColor = when (subtask.priority) {
            Priority.URGENT -> PriorityUrgentColor
            Priority.HIGH -> PriorityHighColor
            Priority.MEDIUM -> PriorityMediumColor
            Priority.LOW -> PriorityLowColor
          }

          Surface(
            shape = RoundedCornerShape(6.dp),
            color = prioColor.copy(alpha = 0.12f)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.Flag,
                contentDescription = null,
                tint = prioColor,
                modifier = Modifier.size(10.dp)
              )
              Spacer(modifier = Modifier.width(3.dp))
              Text(
                text = subtask.priority.label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = prioColor
              )
            }
          }

          // Due Date Badge (if scheduled)
          val dueDateStr = subtask.formattedDueDate()
          if (dueDateStr != null) {
            val isOverdue = subtask.isOverdue()
            val badgeBg = when {
              isOverdue -> PriorityUrgentColor.copy(alpha = 0.14f)
              dueDateStr == "Due Today" -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
              else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            }
            val badgeTextColor = when {
              isOverdue -> PriorityUrgentColor
              dueDateStr == "Due Today" -> MaterialTheme.colorScheme.primary
              else -> MaterialTheme.colorScheme.onSurfaceVariant
            }

            Surface(
              shape = RoundedCornerShape(6.dp),
              color = badgeBg
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = Icons.Default.CalendarToday,
                  contentDescription = "Due Date",
                  tint = badgeTextColor,
                  modifier = Modifier.size(10.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                  text = if (isOverdue) "Overdue ($dueDateStr)" else dueDateStr,
                  fontSize = 10.sp,
                  fontWeight = if (isOverdue || dueDateStr == "Due Today") FontWeight.Bold else FontWeight.Medium,
                  color = badgeTextColor
                )
              }
            }
          }

          // Category Tag (if provided)
          if (subtask.categoryTag.isNotBlank()) {
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = MaterialTheme.colorScheme.surfaceVariant
            ) {
              Text(
                text = subtask.categoryTag,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }

          if (subtask.actualMinutes > 0) {
            Text(
              text = "${subtask.actualMinutes}m logged",
              fontSize = 10.sp,
              fontWeight = FontWeight.SemiBold,
              color = EmeraldSuccess,
              modifier = Modifier.padding(vertical = 2.dp)
            )
          }
        }
      }

      // Action Buttons: Focus Timer & Delete
      if (!subtask.isCompleted) {
        IconButton(
          onClick = onStartTimer,
          modifier = Modifier
            .size(36.dp)
            .testTag("start_timer_btn_${subtask.id}")
        ) {
          Box(
            modifier = Modifier
              .size(30.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.PlayArrow,
              contentDescription = "Start Timer",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(18.dp)
            )
          }
        }
      }

      IconButton(
        onClick = onDelete,
        modifier = Modifier.size(36.dp)
      ) {
        Icon(
          imageVector = Icons.Default.DeleteOutline,
          contentDescription = "Delete Subtask",
          tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
          modifier = Modifier.size(18.dp)
        )
      }
    }
  }
}
