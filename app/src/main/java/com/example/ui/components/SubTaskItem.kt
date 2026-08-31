package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
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
  var showLevelBreakdown by remember { mutableStateOf(false) }

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .testTag("subtask_item_${subtask.id}"),
    shape = RoundedCornerShape(14.dp),
    color = if (subtask.isCompleted) {
      MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    } else {
      MaterialTheme.colorScheme.surface
    },
    border = androidx.compose.foundation.BorderStroke(
      1.dp,
      if (subtask.isCompleted) {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
      } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
      }
    ),
    tonalElevation = 1.dp,
    shadowElevation = if (subtask.isCompleted) 0.5.dp else 1.5.dp
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Custom Checkbox for 2nd Level Subtask
        Box(
          modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(
              if (subtask.isCompleted) EmeraldSuccess else Color.Transparent
            )
            .clickable(onClick = onToggle)
            .testTag("subtask_checkbox_${subtask.id}"),
          contentAlignment = Alignment.Center
        ) {
          if (subtask.isCompleted) {
            Icon(
              imageVector = Icons.Default.Check,
              contentDescription = "Completed",
              tint = Color.White,
              modifier = Modifier.size(15.dp)
            )
          } else {
            Box(
              modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
            ) {
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

        // Title, Badges, and Hierarchy Details
        Column(
          modifier = Modifier
            .weight(1f)
            .clickable { showLevelBreakdown = !showLevelBreakdown }
        ) {
          // Top line: Level badge + Title
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            // Task Hierarchy Level 2 Indicator Badge
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
            ) {
              Text(
                text = "L2.${stepIndex + 1}",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
              )
            }

            Text(
              text = subtask.title,
              fontSize = 13.sp,
              fontWeight = if (subtask.isCompleted) FontWeight.Normal else FontWeight.Medium,
              color = if (subtask.isCompleted) {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
              } else {
                MaterialTheme.colorScheme.onSurface
              },
              textDecoration = if (subtask.isCompleted) TextDecoration.LineThrough else TextDecoration.None
            )
          }

          Spacer(modifier = Modifier.height(5.dp))

          // Subtask Meta & AI-assigned Priority + Category Tag Row
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            // Step Sequence Pill
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = MaterialTheme.colorScheme.surfaceVariant
            ) {
              Text(
                text = "Step ${stepIndex + 1} of $totalSteps",
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
              )
            }

            // Estimated Time Badge
            Row(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f))
                .padding(horizontal = 6.dp, vertical = 2.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(10.dp)
              )
              Spacer(modifier = Modifier.width(3.dp))
              Text(
                text = "${subtask.estimatedMinutes}m",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
              )
            }

            // AI-Assigned Subtask Priority Badge
            val prioColor = when (subtask.priority) {
              Priority.URGENT -> PriorityUrgentColor
              Priority.HIGH -> PriorityHighColor
              Priority.MEDIUM -> PriorityMediumColor
              Priority.LOW -> PriorityLowColor
            }

            Surface(
              shape = RoundedCornerShape(6.dp),
              color = prioColor.copy(alpha = 0.15f)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = Icons.Default.Flag,
                  contentDescription = null,
                  tint = prioColor,
                  modifier = Modifier.size(9.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                  text = subtask.priority.label,
                  fontSize = 9.sp,
                  fontWeight = FontWeight.Bold,
                  color = prioColor
                )
              }
            }

            // AI-Assigned Category Tag Chip
            if (subtask.categoryTag.isNotBlank()) {
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(
                    imageVector = Icons.Default.LocalOffer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(9.dp)
                  )
                  Spacer(modifier = Modifier.width(2.dp))
                  Text(
                    text = subtask.categoryTag,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                  )
                }
              }
            }

            if (subtask.actualMinutes > 0) {
              Row(
                modifier = Modifier
                  .clip(RoundedCornerShape(6.dp))
                  .background(EmeraldSuccess.copy(alpha = 0.15f))
                  .padding(horizontal = 5.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "${subtask.actualMinutes}m logged",
                  fontSize = 9.sp,
                  fontWeight = FontWeight.Bold,
                  color = EmeraldSuccess
                )
              }
            }

            Icon(
              imageVector = if (showLevelBreakdown) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
              contentDescription = "Toggle hierarchy details",
              tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
              modifier = Modifier
                .size(14.dp)
                .align(Alignment.CenterVertically)
            )
          }
        }

        // Focus Timer Play Button
        if (!subtask.isCompleted) {
          IconButton(
            onClick = onStartTimer,
            modifier = Modifier
              .size(32.dp)
              .testTag("start_timer_btn_${subtask.id}")
          ) {
            Box(
              modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Start Timer",
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(16.dp)
              )
            }
          }
        }

        IconButton(
          onClick = onDelete,
          modifier = Modifier.size(28.dp)
        ) {
          Icon(
            imageVector = Icons.Default.DeleteOutline,
            contentDescription = "Delete Subtask",
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(15.dp)
          )
        }
      }

      // Subsequent Task Levels for Visual Hierarchy (Level 2 & 3 breakdowns and guidance notes)
      AnimatedVisibility(
        visible = showLevelBreakdown
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, start = 20.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(10.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Layers,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(13.dp)
            )
            Text(
              text = "TASK HIERARCHY BREAKDOWN",
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.8.sp,
              color = MaterialTheme.colorScheme.primary
            )
          }

          if (subtask.actionableNotes.isNotBlank()) {
            Text(
              text = "Actionable Guidance: ${subtask.actionableNotes}",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurface,
              fontWeight = FontWeight.Normal
            )
          }

          // Level 2.1: Execution Phase
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Box(
              modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
            )
            Column {
              Text(
                text = "Level 2.${stepIndex + 1}a • Focus Execution Phase",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "Allocated session: ${subtask.estimatedMinutes}m • Priority: ${subtask.priority.label} • Tag: ${subtask.categoryTag.ifBlank { "General" }}",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          // Level 2.2: Verification Stage
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Box(
              modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(if (subtask.isCompleted) EmeraldSuccess else MaterialTheme.colorScheme.outline)
            )
            Column {
              Text(
                text = "Level 2.${stepIndex + 1}b • Verification & Completion Stage",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (subtask.isCompleted) EmeraldSuccess else MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = if (subtask.isCompleted) "Status: Completed and verified" else "Status: Pending checkoff",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }
    }
  }
}
