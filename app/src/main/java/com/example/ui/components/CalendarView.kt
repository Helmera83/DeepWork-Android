package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.calendar.CalendarIntegrationManager
import com.example.data.model.Priority
import com.example.data.model.SubTask
import com.example.data.model.Task
import com.example.ui.theme.BaselinePrimary
import com.example.ui.theme.ExpressiveEmerald
import com.example.ui.theme.PriorityHighColor
import com.example.ui.theme.PriorityLowColor
import com.example.ui.theme.PriorityMediumColor
import com.example.ui.theme.PriorityUrgentColor
import com.example.ui.theme.ToneSlateSecondary
import com.example.ui.theme.ToneTealTertiary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun CalendarView(
  tasks: List<Task>,
  calendarManager: CalendarIntegrationManager,
  onToggleSubtask: (subtask: SubTask, parentTask: Task) -> Unit,
  onStartTimer: (subtask: SubTask, parentTask: Task) -> Unit,
  onAddNewTask: (deadlineMillis: Long?) -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var currentMonthCalendar by remember { mutableStateOf(Calendar.getInstance()) }
  var selectedDateCalendar by remember { mutableStateOf(Calendar.getInstance()) }
  var showExportDialogForTask by remember { mutableStateOf<Task?>(null) }

  val monthYearFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
  val dayDateFormat = remember { SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()) }

  // Extract all tasks with deadlines or created dates mapped to calendar day keys
  fun getDayKey(timeMillis: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = timeMillis }
    return "${cal.get(Calendar.YEAR)}_${cal.get(Calendar.MONTH)}_${cal.get(Calendar.DAY_OF_MONTH)}"
  }

  val selectedDayKey = "${selectedDateCalendar.get(Calendar.YEAR)}_${selectedDateCalendar.get(Calendar.MONTH)}_${selectedDateCalendar.get(Calendar.DAY_OF_MONTH)}"

  // Map of date key to tasks
  val tasksByDate = remember(tasks) {
    val map = mutableMapOf<String, MutableList<Task>>()
    for (task in tasks) {
      if (task.deadlineTimestamp != null && task.deadlineTimestamp > 0L) {
        val key = getDayKey(task.deadlineTimestamp)
        map.getOrPut(key) { mutableListOf() }.add(task)
      } else {
        // Also map to creation day if no deadline
        val key = getDayKey(task.createdAt)
        map.getOrPut(key) { mutableListOf() }.add(task)
      }
    }
    map
  }

  // Tasks for selected date
  val tasksOnSelectedDate = tasksByDate[selectedDayKey] ?: emptyList()

  // All upcoming tasks with deadlines
  val upcomingTasks = remember(tasks) {
    tasks.filter { it.deadlineTimestamp != null && it.deadlineTimestamp > 0L }
      .sortedBy { it.deadlineTimestamp }
  }

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // 1. Calendar Navigation Header
    item {
      Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          // Month Navigator Bar
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            IconButton(
              onClick = {
                val newCal = currentMonthCalendar.clone() as Calendar
                newCal.add(Calendar.MONTH, -1)
                currentMonthCalendar = newCal
              }
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Previous Month",
                tint = MaterialTheme.colorScheme.onSurface
              )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                text = monthYearFormat.format(currentMonthCalendar.time),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "Tasks & Scheduled Milestones",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
              // Today Button
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                modifier = Modifier
                  .clickable {
                    val now = Calendar.getInstance()
                    currentMonthCalendar = now
                    selectedDateCalendar = now
                  }
                  .padding(end = 4.dp)
              ) {
                Text(
                  text = "Today",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                )
              }

              IconButton(
                onClick = {
                  val newCal = currentMonthCalendar.clone() as Calendar
                  newCal.add(Calendar.MONTH, 1)
                  currentMonthCalendar = newCal
                }
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                  contentDescription = "Next Month",
                  tint = MaterialTheme.colorScheme.onSurface
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Days of Week Header
          val daysOfWeek = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
          ) {
            daysOfWeek.forEach { dayName ->
              Text(
                text = dayName,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Calendar Grid for Month
          CalendarMonthGrid(
            calendar = currentMonthCalendar,
            selectedCalendar = selectedDateCalendar,
            tasksByDate = tasksByDate,
            onSelectDate = { selectedDateCalendar = it }
          )
        }
      }
    }

    // 2. Selected Date Header & Action Bar
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = dayDateFormat.format(selectedDateCalendar.time),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "${tasksOnSelectedDate.size} task${if (tasksOnSelectedDate.size == 1) "" else "s"} scheduled",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        OutlinedButton(
          onClick = { onAddNewTask(selectedDateCalendar.timeInMillis) },
          shape = RoundedCornerShape(12.dp),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
        ) {
          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Add Task", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
      }
    }

    // 3. Tasks on Selected Date
    if (tasksOnSelectedDate.isNotEmpty()) {
      items(tasksOnSelectedDate, key = { "cal_task_${it.id}" }) { task ->
        CalendarTaskCard(
          task = task,
          onToggleSubtask = { sub -> onToggleSubtask(sub, task) },
          onStartTimer = { sub -> onStartTimer(sub, task) },
          onExportCalendar = { showExportDialogForTask = task }
        )
      }
    } else {
      item {
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
          border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Icon(
              imageVector = Icons.Default.DateRange,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
              modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "No tasks due on this date",
              fontSize = 14.sp,
              fontWeight = FontWeight.Medium,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "Tap 'Add Task' to schedule a goal or pick another day.",
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center
            )
          }
        }
      }
    }

    // 4. Upcoming Deadlines Section
    if (upcomingTasks.isNotEmpty()) {
      item {
        Spacer(modifier = Modifier.height(10.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
          Text(
            text = "All Scheduled Deadlines (${upcomingTasks.size})",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
        }
      }

      items(upcomingTasks, key = { "upcoming_${it.id}" }) { task ->
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surface,
          border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
          modifier = Modifier
            .fillMaxWidth()
            .clickable {
              task.deadlineTimestamp?.let {
                val cal = Calendar.getInstance().apply { timeInMillis = it }
                selectedDateCalendar = cal
                currentMonthCalendar = cal
              }
            }
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = task.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 4.dp)
              ) {
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = MaterialTheme.colorScheme.primaryContainer
                ) {
                  Text(
                    text = task.category.label,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
                Text(
                  text = "Due ${task.formattedDeadline() ?: "Soon"}",
                  fontSize = 11.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            IconButton(
              onClick = {
                val intent = calendarManager.createCalendarInsertIntent(task)
                context.startActivity(intent)
              }
            ) {
              Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = "Sync to Google Calendar",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
              )
            }
          }
        }
      }
    }
  }

  // Export Sheet if clicked
  showExportDialogForTask?.let { task ->
    CalendarExportDialog(
      task = task,
      calendarManager = calendarManager,
      onDismiss = { showExportDialogForTask = null }
    )
  }
}

@Composable
private fun CalendarMonthGrid(
  calendar: Calendar,
  selectedCalendar: Calendar,
  tasksByDate: Map<String, List<Task>>,
  onSelectDate: (Calendar) -> Unit
) {
  val cal = calendar.clone() as Calendar
  cal.set(Calendar.DAY_OF_MONTH, 1)

  val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1 // 0 = Sunday
  val maxDaysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

  val todayCal = Calendar.getInstance()
  val todayDay = todayCal.get(Calendar.DAY_OF_MONTH)
  val todayMonth = todayCal.get(Calendar.MONTH)
  val todayYear = todayCal.get(Calendar.YEAR)

  val curMonth = calendar.get(Calendar.MONTH)
  val curYear = calendar.get(Calendar.YEAR)

  val selDay = selectedCalendar.get(Calendar.DAY_OF_MONTH)
  val selMonth = selectedCalendar.get(Calendar.MONTH)
  val selYear = selectedCalendar.get(Calendar.YEAR)

  val totalSlots = ((firstDayOfWeek + maxDaysInMonth + 6) / 7) * 7

  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    for (row in 0 until (totalSlots / 7)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround
      ) {
        for (col in 0 until 7) {
          val slotIndex = row * 7 + col
          val dayNumber = slotIndex - firstDayOfWeek + 1

          if (dayNumber in 1..maxDaysInMonth) {
            val dateKey = "${curYear}_${curMonth}_$dayNumber"
            val tasksForDay = tasksByDate[dateKey] ?: emptyList()
            val isSelected = (dayNumber == selDay && curMonth == selMonth && curYear == selYear)
            val isToday = (dayNumber == todayDay && curMonth == todayMonth && curYear == todayYear)

            Box(
              modifier = Modifier
                .weight(1f)
                .height(42.dp)
                .padding(2.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                  when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                    else -> Color.Transparent
                  }
                )
                .clickable {
                  val newDate = Calendar.getInstance().apply {
                    set(Calendar.YEAR, curYear)
                    set(Calendar.MONTH, curMonth)
                    set(Calendar.DAY_OF_MONTH, dayNumber)
                  }
                  onSelectDate(newDate)
                },
              contentAlignment = Alignment.Center
            ) {
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
              ) {
                Text(
                  text = dayNumber.toString(),
                  fontSize = 13.sp,
                  fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                  color = when {
                    isSelected -> MaterialTheme.colorScheme.onPrimary
                    isToday -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface
                  }
                )

                // Task Indicator Dot
                if (tasksForDay.isNotEmpty()) {
                  Box(
                    modifier = Modifier
                      .padding(top = 2.dp)
                      .size(5.dp)
                      .clip(CircleShape)
                      .background(if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary)
                  )
                } else {
                  Spacer(modifier = Modifier.height(5.dp))
                }
              }
            }
          } else {
            // Empty filler slot
            Spacer(modifier = Modifier.weight(1f))
          }
        }
      }
    }
  }
}

@Composable
private fun CalendarTaskCard(
  task: Task,
  onToggleSubtask: (SubTask) -> Unit,
  onStartTimer: (SubTask) -> Unit,
  onExportCalendar: () -> Unit
) {
  val context = LocalContext.current

  Surface(
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surface,
    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
    shadowElevation = 1.dp,
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      // Top Task Info
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = MaterialTheme.colorScheme.primaryContainer
            ) {
              Text(
                text = task.category.label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }

            val prioColor = when (task.priority) {
              Priority.LOW -> PriorityLowColor
              Priority.MEDIUM -> PriorityMediumColor
              Priority.HIGH -> PriorityHighColor
              Priority.URGENT -> PriorityUrgentColor
            }
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = prioColor.copy(alpha = 0.15f)
            ) {
              Text(
                text = task.priority.label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = prioColor,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = task.title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          if (task.description.isNotBlank()) {
            Text(
              text = task.description,
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis
            )
          }
        }

        IconButton(onClick = onExportCalendar) {
          Icon(
            imageVector = Icons.Default.CalendarMonth,
            contentDescription = "Export to Calendar",
            tint = MaterialTheme.colorScheme.primary
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Milestone Breakdown List
      Text(
        text = "Sub-tasks & Milestones (${task.completedSubtasksCount}/${task.totalSubtasksCount}):",
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      Spacer(modifier = Modifier.height(6.dp))

      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        task.subtasks.forEach { sub ->
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (sub.isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                  .weight(1f)
                  .clickable { onToggleSubtask(sub) }
              ) {
                Icon(
                  imageVector = if (sub.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                  contentDescription = if (sub.isCompleted) "Completed" else "Incomplete",
                  tint = if (sub.isCompleted) ExpressiveEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                  Text(
                    text = sub.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (sub.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (sub.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                  )
                  if (sub.categoryTag.isNotBlank()) {
                    Text(
                      text = "${sub.categoryTag} • ${sub.estimatedMinutes} mins",
                      fontSize = 11.sp,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                }
              }

              if (!sub.isCompleted) {
                IconButton(
                  onClick = { onStartTimer(sub) },
                  modifier = Modifier.size(30.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Focus Timer",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}
