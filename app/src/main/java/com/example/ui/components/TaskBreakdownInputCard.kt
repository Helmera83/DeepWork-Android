package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Priority
import com.example.data.model.TaskCategory
import com.example.ui.theme.BaselinePrimary
import com.example.ui.theme.ExpressiveAmber
import com.example.ui.theme.ToneTealTertiary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Task Breakdown Input Card:
 * Provides an input box, interactive Date Picker, voice breakdown trigger,
 * priority/category selectors, and an AI breakdown action button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskBreakdownInputCard(
  modifier: Modifier = Modifier,
  isLoading: Boolean = false,
  targetDateMillis: Long? = null,
  targetDateFormatted: String? = null,
  onInitiateBreakdown: (
    title: String,
    category: TaskCategory,
    priority: Priority,
    description: String,
    deadlineMillis: Long?
  ) -> Unit,
  onOpenVoiceCoach: (() -> Unit)? = null
) {
  var taskDescriptionText by remember { mutableStateOf("") }
  var additionalDetailsText by remember { mutableStateOf("") }
  var isExpandedOptions by remember { mutableStateOf(false) }
  var selectedCategory by remember { mutableStateOf(TaskCategory.WORK) }
  var selectedPriority by remember { mutableStateOf(Priority.MEDIUM) }
  var selectedDeadlineMillis by remember(targetDateMillis) { mutableStateOf<Long?>(targetDateMillis) }
  var showDatePickerDialog by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  val focusManager = LocalFocusManager.current
  val dateFormat = remember { SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()) }

  val infiniteTransition = rememberInfiniteTransition(label = "sparkle_anim")
  val sparkleScale by infiniteTransition.animateFloat(
    initialValue = 0.92f,
    targetValue = 1.08f,
    animationSpec = infiniteRepeatable(
      animation = tween(1200, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "sparkle_scale"
  )

  fun submitBreakdown() {
    val trimmed = taskDescriptionText.trim()
    if (trimmed.isBlank()) {
      errorMessage = "Please enter a task name or description"
      return
    }
    errorMessage = null
    focusManager.clearFocus()
    onInitiateBreakdown(
      trimmed,
      selectedCategory,
      selectedPriority,
      additionalDetailsText.trim(),
      selectedDeadlineMillis
    )
    taskDescriptionText = ""
    additionalDetailsText = ""
    isExpandedOptions = false
  }

  // Material 3 Date Picker Dialog
  if (showDatePickerDialog) {
    val datePickerState = rememberDatePickerState(
      initialSelectedDateMillis = selectedDeadlineMillis ?: System.currentTimeMillis()
    )
    DatePickerDialog(
      onDismissRequest = { showDatePickerDialog = false },
      confirmButton = {
        TextButton(
          onClick = {
            selectedDeadlineMillis = datePickerState.selectedDateMillis
            showDatePickerDialog = false
          },
          modifier = Modifier.testTag("date_picker_confirm_btn")
        ) {
          Text("Set Date", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showDatePickerDialog = false }) {
          Text("Cancel")
        }
      }
    ) {
      DatePicker(state = datePickerState)
    }
  }

  Surface(
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 2.dp,
    modifier = modifier
      .fillMaxWidth()
      .border(
        width = 1.dp,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
        shape = RoundedCornerShape(20.dp)
      )
      .testTag("task_breakdown_input_card")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    ) {
      // --- Header Row with AI & Voice Controls ---
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Box(
            modifier = Modifier
              .size(28.dp)
              .clip(CircleShape)
              .background(
                Brush.linearGradient(
                  colors = listOf(BaselinePrimary, ToneTealTertiary)
                )
              ),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.AutoAwesome,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier
                .size(15.dp)
                .scale(sparkleScale)
            )
          }

          Column {
            Text(
              text = "Input & Break Down Task",
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "Add manually or breakdown with voice & AI",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          if (onOpenVoiceCoach != null) {
            IconButton(
              onClick = onOpenVoiceCoach,
              modifier = Modifier
                .size(34.dp)
                .testTag("input_card_voice_btn")
            ) {
              Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "Voice Input / Coach",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
              )
            }
          }

          IconButton(
            onClick = { isExpandedOptions = !isExpandedOptions },
            modifier = Modifier.size(34.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Tune,
              contentDescription = "Options",
              tint = if (isExpandedOptions) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(18.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // --- Main Task Input Box ---
      OutlinedTextField(
        value = taskDescriptionText,
        onValueChange = {
          taskDescriptionText = it
          if (errorMessage != null) errorMessage = null
        },
        placeholder = {
          Text(
            text = "Enter task title or goal (e.g. 'Launch new project landing page')",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
          )
        },
        modifier = Modifier
          .fillMaxWidth()
          .testTag("main_task_description_input"),
        shape = RoundedCornerShape(14.dp),
        minLines = 2,
        maxLines = 4,
        isError = errorMessage != null,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { submitBreakdown() }),
        trailingIcon = {
          if (taskDescriptionText.isNotEmpty()) {
            IconButton(onClick = { taskDescriptionText = "" }) {
              Icon(
                imageVector = Icons.Default.Clear,
                contentDescription = "Clear input",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
              )
            }
          }
        },
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = MaterialTheme.colorScheme.primary,
          unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
          errorBorderColor = MaterialTheme.colorScheme.error
        )
      )

      if (errorMessage != null) {
        Text(
          text = errorMessage!!,
          fontSize = 12.sp,
          color = MaterialTheme.colorScheme.error,
          modifier = Modifier.padding(start = 4.dp, top = 4.dp)
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      // --- Interactive Date Picker Row ---
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = if (selectedDeadlineMillis != null) {
            MaterialTheme.colorScheme.primaryContainer
          } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
          },
          modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { showDatePickerDialog = true }
            .testTag("date_picker_trigger_btn")
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
          ) {
            Icon(
              imageVector = Icons.Default.CalendarMonth,
              contentDescription = "Pick Due Date",
              tint = if (selectedDeadlineMillis != null) {
                MaterialTheme.colorScheme.primary
              } else {
                MaterialTheme.colorScheme.onSurfaceVariant
              },
              modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (selectedDeadlineMillis != null) {
                "Due: ${dateFormat.format(Date(selectedDeadlineMillis!!))}"
              } else {
                "Select Due Date"
              },
              fontSize = 12.sp,
              fontWeight = FontWeight.Medium,
              color = if (selectedDeadlineMillis != null) {
                MaterialTheme.colorScheme.onPrimaryContainer
              } else {
                MaterialTheme.colorScheme.onSurfaceVariant
              }
            )
          }
        }

        if (selectedDeadlineMillis != null) {
          TextButton(
            onClick = { selectedDeadlineMillis = null },
            modifier = Modifier.height(32.dp)
          ) {
            Text("Clear Date", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
          }
        }
      }

      // --- Expandable Options: Category, Priority, Extra Constraints ---
      AnimatedVisibility(
        visible = isExpandedOptions,
        enter = fadeIn(),
        exit = fadeOut()
      ) {
        Column(modifier = Modifier.padding(top = 10.dp)) {
          OutlinedTextField(
            value = additionalDetailsText,
            onValueChange = { additionalDetailsText = it },
            placeholder = {
              Text(
                text = "Optional context (e.g. 'Target 3 milestones with 25-min steps')",
                fontSize = 12.sp
              )
            },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("task_extra_context_input"),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = MaterialTheme.colorScheme.primary,
              unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
          )

          Spacer(modifier = Modifier.height(10.dp))

          // Category Selector Pills
          Text(
            text = "Category",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.height(4.dp))
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            TaskCategory.values().forEach { cat ->
              val isSelected = cat == selectedCategory
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                  .clip(RoundedCornerShape(12.dp))
                  .clickable { selectedCategory = cat }
              ) {
                Text(
                  text = cat.label,
                  fontSize = 11.sp,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                  color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Priority Selector Pills
          Text(
            text = "Priority",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.height(4.dp))
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Priority.values().forEach { prio ->
              val isSelected = prio == selectedPriority
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                  .clip(RoundedCornerShape(12.dp))
                  .clickable { selectedPriority = prio }
              ) {
                Text(
                  text = prio.label,
                  fontSize = 11.sp,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                  color = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // --- Quick Starter Suggestions ---
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = Icons.Default.Lightbulb,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
          modifier = Modifier.size(14.dp)
        )
        listOf(
          "Prepare quarterly pitch deck",
          "Deploy Kotlin Room database",
          "Organize home office setup",
          "Study for machine learning exam"
        ).forEach { suggestion ->
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier
              .clip(RoundedCornerShape(12.dp))
              .clickable {
                taskDescriptionText = suggestion
                errorMessage = null
              }
          ) {
            Text(
              text = suggestion,
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // --- Breakdown Button ---
      Button(
        onClick = { submitBreakdown() },
        enabled = !isLoading,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
          .testTag("initiate_ai_breakdown_btn")
      ) {
        if (isLoading) {
          CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.onPrimary
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Decomposing Task...",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
          )
        } else {
          Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Break Down Task",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }
  }
}

