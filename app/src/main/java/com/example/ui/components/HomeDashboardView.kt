package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.calendar.CalendarIntegrationManager
import com.example.data.model.Priority
import com.example.data.model.SubTask
import com.example.data.model.Task
import com.example.data.model.TaskCategory
import com.example.ui.theme.BrandSecondary
import com.example.ui.theme.BrandSecondaryContainer
import com.example.ui.theme.BrandTertiary
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricGreen
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.HyperCyan
import com.example.ui.theme.PriorityHighColor
import com.example.ui.theme.PriorityLowColor
import com.example.ui.theme.PriorityMediumColor
import com.example.ui.theme.PriorityUrgentColor
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class DashboardViewMode(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
  OVERVIEW("Overview", Icons.Default.ViewAgenda),
  ALL_TASKS("All Tasks", Icons.Default.FormatListBulleted),
  FILTERED("Filtered", Icons.Default.FilterList),
  TIMELINE("Timeline", Icons.Default.Timeline),
  CALENDAR("Calendar", Icons.Default.CalendarMonth)
}

enum class StatusFilterOption(val label: String) {
  ALL("All"),
  ACTIVE("Active"),
  COMPLETED("Completed")
}

@Composable
fun HomeDashboardView(
  tasks: List<Task>,
  calendarManager: CalendarIntegrationManager,
  onNavigateToBreakdown: () -> Unit,
  onNavigateToAssistant: () -> Unit,
  onNavigateToSync: () -> Unit,
  onToggleSubtask: (SubTask, Task) -> Unit,
  onDeleteSubtask: (SubTask, Task) -> Unit,
  onEditTask: (Task) -> Unit,
  onDeleteTask: (Task) -> Unit,
  onExportCalendar: (Task) -> Unit,
  onAddSubtask: (Task) -> Unit,
  onGenerateAiSubtasks: (Task) -> Unit,
  onCompleteTask: (Task) -> Unit = {},
  onPostponeTask: (Task) -> Unit = {},
  onAddNewTask: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  var selectedViewMode by remember { mutableStateOf(DashboardViewMode.OVERVIEW) }

  // Filter state for Filtered view & search
  var searchQuery by remember { mutableStateOf("") }
  var statusFilter by remember { mutableStateOf(StatusFilterOption.ALL) }
  var categoryFilter by remember { mutableStateOf<TaskCategory?>(null) }
  var priorityFilter by remember { mutableStateOf<Priority?>(null) }

  // Calendar mode state
  var currentMonthCalendar by remember { mutableStateOf(Calendar.getInstance()) }
  var selectedDateCalendar by remember { mutableStateOf(Calendar.getInstance()) }

  val todayFormatted = remember {
    SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())
  }
  val monthYearFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
  val dayDateFormat = remember { SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()) }

  // Active focus task (first incomplete task)
  val activeTask = tasks.firstOrNull { !it.isFullyCompleted }

  // Metrics computation
  val totalTasksCount = tasks.size
  val completedTasksCount = tasks.count { it.isFullyCompleted }
  val activeTasksCount = tasks.count { !it.isFullyCompleted }

  val allSubtasks = remember(tasks) { tasks.flatMap { it.subtasks } }
  val totalSubtasksCount = allSubtasks.size
  val completedSubtasksCount = remember(allSubtasks) { allSubtasks.count { it.isCompleted } }
  val pendingStepsCount = remember(allSubtasks) { allSubtasks.count { !it.isCompleted } }

  val totalActualMinutes = remember(allSubtasks) { allSubtasks.sumOf { it.actualMinutes } }
  val deepWorkHours = remember(totalActualMinutes) {
    String.format(Locale.getDefault(), "%.1f", (totalActualMinutes.coerceAtLeast(45)) / 60.0)
  }
  val remainingEstimatedMinutes = remember(allSubtasks) {
    allSubtasks.filter { !it.isCompleted }.sumOf { it.estimatedMinutes }
  }
  val remainingHours = remember(remainingEstimatedMinutes) {
    String.format(Locale.getDefault(), "%.1f", remainingEstimatedMinutes / 60.0)
  }

  val taskCompletionPercentage = if (totalTasksCount == 0) 100 else ((completedTasksCount.toFloat() / totalTasksCount.toFloat()) * 100).toInt()
  val subtaskCompletionPercentage = if (totalSubtasksCount == 0) 100 else ((completedSubtasksCount.toFloat() / totalSubtasksCount.toFloat()) * 100).toInt()
  val overallProgressFraction = if (totalSubtasksCount > 0) completedSubtasksCount.toFloat() / totalSubtasksCount.toFloat() else if (totalTasksCount > 0) completedTasksCount.toFloat() / totalTasksCount.toFloat() else 1f

  val overdueTasksCount = tasks.count { it.isOverdue() }
  val overdueSubtasksCount = allSubtasks.count { it.isOverdue() }
  val totalOverdueCount = overdueTasksCount + overdueSubtasksCount

  val dueTodayTasksCount = tasks.count { !it.isFullyCompleted && it.formattedDueDate() == "Due Today" }
  val dueTodaySubtasksCount = allSubtasks.count { !it.isCompleted && it.formattedDueDate() == "Due Today" }
  val totalDueTodayCount = dueTodayTasksCount + dueTodaySubtasksCount

  val urgentTasksCount = tasks.count { !it.isFullyCompleted && it.priority == Priority.URGENT }
  val highTasksCount = tasks.count { !it.isFullyCompleted && it.priority == Priority.HIGH }
  val mediumTasksCount = tasks.count { !it.isFullyCompleted && it.priority == Priority.MEDIUM }
  val lowTasksCount = tasks.count { !it.isFullyCompleted && it.priority == Priority.LOW }

  val totalMilestonesCount = remember(tasks) { tasks.sumOf { it.milestones.size } }
  val completedMilestonesCount = remember(tasks, allSubtasks) {
    tasks.flatMap { it.milestones }.count { milestone ->
      val mSubs = allSubtasks.filter { it.milestoneId == milestone.id }
      mSubs.isNotEmpty() && mSubs.all { it.isCompleted }
    }
  }

  val efficiencyScore = "$subtaskCompletionPercentage%"

  // Filtered tasks calculation
  val filteredTasks = remember(tasks, searchQuery, statusFilter, categoryFilter, priorityFilter) {
    tasks.filter { task ->
      val matchesSearch = searchQuery.isBlank() ||
          task.title.contains(searchQuery, ignoreCase = true) ||
          task.description.contains(searchQuery, ignoreCase = true) ||
          task.subtasks.any { it.title.contains(searchQuery, ignoreCase = true) }

      val matchesStatus = when (statusFilter) {
        StatusFilterOption.ALL -> true
        StatusFilterOption.ACTIVE -> !task.isFullyCompleted
        StatusFilterOption.COMPLETED -> task.isFullyCompleted
      }

      val matchesCategory = categoryFilter == null || task.category == categoryFilter
      val matchesPriority = priorityFilter == null || task.priority == priorityFilter

      matchesSearch && matchesStatus && matchesCategory && matchesPriority
    }
  }

  // Group tasks by date for calendar view
  fun getDayKey(timeMillis: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = timeMillis }
    return "${cal.get(Calendar.YEAR)}_${cal.get(Calendar.MONTH)}_${cal.get(Calendar.DAY_OF_MONTH)}"
  }

  val tasksByDate = remember(tasks) {
    val map = mutableMapOf<String, MutableList<Task>>()
    for (task in tasks) {
      if (task.deadlineTimestamp != null && task.deadlineTimestamp > 0L) {
        val key = getDayKey(task.deadlineTimestamp)
        map.getOrPut(key) { mutableListOf() }.add(task)
      } else {
        val key = getDayKey(task.createdAt)
        map.getOrPut(key) { mutableListOf() }.add(task)
      }
    }
    map
  }

  val activeCategories = remember(tasks) {
    TaskCategory.values().mapNotNull { cat ->
      val catTasks = tasks.filter { it.category == cat }
      if (catTasks.isEmpty()) null
      else {
        val totalCatSubs = catTasks.flatMap { it.subtasks }.size
        val doneCatSubs = catTasks.flatMap { it.subtasks }.count { it.isCompleted }
        Triple(cat, catTasks.size, if (totalCatSubs > 0) doneCatSubs.toFloat() / totalCatSubs.toFloat() else if (catTasks.all { it.isFullyCompleted }) 1f else 0f)
      }
    }
  }

  val selectedDayKey = "${selectedDateCalendar.get(Calendar.YEAR)}_${selectedDateCalendar.get(Calendar.MONTH)}_${selectedDateCalendar.get(Calendar.DAY_OF_MONTH)}"
  val tasksOnSelectedDate = tasksByDate[selectedDayKey] ?: emptyList()

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .testTag("home_dashboard_view"),
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 96.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // 1. Greeting & Date Banner
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = todayFormatted,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            text = "Welcome, Alex",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp,
            color = MaterialTheme.colorScheme.onSurface
          )
        }

        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.primaryContainer,
          modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onNavigateToAssistant() }
            .testTag("dashboard_ai_coach_shortcut")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Icon(
              imageVector = Icons.Default.SmartToy,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(16.dp)
            )
            Text(
              text = "Ask AI",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary
            )
          }
        }
      }
    }

    // 2. View Mode Switcher Segmented Bar (Overview | All Tasks | Filtered | Timeline | Calendar)
    item {
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          DashboardViewMode.values().forEach { mode ->
            val isSelected = mode == selectedViewMode
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
              shadowElevation = if (isSelected) 1.dp else 0.dp,
              modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable { selectedViewMode = mode }
                .testTag("dashboard_mode_tab_${mode.name.lowercase()}")
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
              ) {
                Icon(
                  imageVector = mode.icon,
                  contentDescription = null,
                  tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(14.dp)
                )
                Text(
                  text = mode.label,
                  fontSize = 12.sp,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                  color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }
        }
      }
    }

    // --- VIEW MODE 1: OVERVIEW (Rich Metrics & High-Level Summary) ---
    if (selectedViewMode == DashboardViewMode.OVERVIEW) {
      // 1. Executive Progress Hero Card
      item {
        ExecutiveProgressHeroCard(
          overallProgressFraction = overallProgressFraction,
          completionPercentage = subtaskCompletionPercentage,
          completedTasksCount = completedTasksCount,
          totalTasksCount = totalTasksCount,
          completedSubtasksCount = completedSubtasksCount,
          totalSubtasksCount = totalSubtasksCount,
          activeTasksCount = activeTasksCount,
          totalOverdueCount = totalOverdueCount
        )
      }

      // 2. Daily Velocity Grid (4 Key Output Metrics)
      item {
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Daily Velocity",
              fontSize = 17.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.TrendingUp,
                  contentDescription = null,
                  tint = EmeraldSuccess,
                  modifier = Modifier.size(13.dp)
                )
                Text(
                  text = "Efficiency $efficiencyScore",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            VelocityMetricCard(
              title = "TASKS DONE",
              value = "$completedTasksCount",
              unit = "/ $totalTasksCount",
              valueColor = BrandTertiary,
              icon = Icons.Default.AssignmentTurnedIn,
              modifier = Modifier.weight(1f)
            )

            VelocityMetricCard(
              title = "SUBTASKS",
              value = "$completedSubtasksCount",
              unit = "/ $totalSubtasksCount",
              valueColor = EmeraldSuccess,
              icon = Icons.Default.TaskAlt,
              modifier = Modifier.weight(1f)
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            VelocityMetricCard(
              title = "DEEP WORK",
              value = deepWorkHours,
              unit = "hrs",
              valueColor = BrandSecondary,
              icon = Icons.Default.HourglassEmpty,
              modifier = Modifier.weight(1f)
            )

            VelocityMetricCard(
              title = "EST. REMAINING",
              value = remainingHours,
              unit = "hrs",
              valueColor = Color(0xFF00897B),
              icon = Icons.Default.Schedule,
              modifier = Modifier.weight(1f)
            )
          }
        }
      }

      // 3. Deadlines & Milestones Health Row
      item {
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Text(
            text = "Deadlines & Milestones",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            // Due Today Card
            DashboardStatusBadgeCard(
              title = "DUE TODAY",
              count = "$totalDueTodayCount",
              subtitle = if (totalDueTodayCount == 0) "All clear" else "Action needed",
              icon = Icons.Default.CalendarToday,
              accentColor = if (totalDueTodayCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.weight(1f)
            )

            // Overdue Card
            DashboardStatusBadgeCard(
              title = "OVERDUE",
              count = "$totalOverdueCount",
              subtitle = if (totalOverdueCount == 0) "On track" else "Behind schedule",
              icon = Icons.Default.WarningAmber,
              accentColor = if (totalOverdueCount > 0) PriorityUrgentColor else EmeraldSuccess,
              modifier = Modifier.weight(1f)
            )

            // Milestones Card
            DashboardStatusBadgeCard(
              title = "MILESTONES",
              count = "$completedMilestonesCount",
              subtitle = "of $totalMilestonesCount hit",
              icon = Icons.Default.Flag,
              accentColor = BrandSecondary,
              modifier = Modifier.weight(1f)
            )
          }
        }
      }

      // 4. Workload by Priority Matrix
      item {
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Text(
            text = "Active Workload by Priority",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )

          Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
              horizontalArrangement = Arrangement.SpaceEvenly,
              verticalAlignment = Alignment.CenterVertically
            ) {
              PriorityMetricPill(
                label = "Urgent",
                count = urgentTasksCount,
                color = PriorityUrgentColor
              )
              PriorityMetricPill(
                label = "High",
                count = highTasksCount,
                color = PriorityHighColor
              )
              PriorityMetricPill(
                label = "Medium",
                count = mediumTasksCount,
                color = PriorityMediumColor
              )
              PriorityMetricPill(
                label = "Low",
                count = lowTasksCount,
                color = PriorityLowColor
              )
            }
          }
        }
      }

      // 5. Category Breakdown & Velocity
      if (activeCategories.isNotEmpty()) {
        item {
          Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Text(
              text = "Category Progress",
              fontSize = 17.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )

            Surface(
              shape = RoundedCornerShape(14.dp),
              color = MaterialTheme.colorScheme.surfaceContainer,
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                activeCategories.forEach { (cat, count, progress) ->
                  CategoryProgressRow(
                    category = cat,
                    taskCount = count,
                    progress = progress
                  )
                }
              }
            }
          }
        }
      }

      // Quick Tasks List in Overview
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Active Tasks (${tasks.count { !it.isFullyCompleted }})",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )

          Text(
            text = "View All",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
              .clickable { selectedViewMode = DashboardViewMode.ALL_TASKS }
              .padding(4.dp)
          )
        }
      }

      items(tasks.take(3), key = { "overview_task_${it.id}" }) { task ->
        TaskCard(
          task = task,
          onToggleSubtask = { sub -> onToggleSubtask(sub, task) },
          onStartTimer = { /* handled in subtask */ },
          onDeleteSubtask = { sub -> onDeleteSubtask(sub, task) },
          onDeleteTask = { onDeleteTask(task) },
          onEditTask = { onEditTask(task) },
          onExportCalendar = { onExportCalendar(task) },
          onAddSubtask = { onAddSubtask(task) },
          onGenerateAiSubtasks = { onGenerateAiSubtasks(task) }
        )
      }
    }

    // --- VIEW MODE 2: ALL TASKS ---
    if (selectedViewMode == DashboardViewMode.ALL_TASKS) {
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "All Tasks (${tasks.size})",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )

          Button(
            onClick = onAddNewTask,
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier.testTag("all_tasks_add_btn")
          ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("New Task", fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
        }
      }

      // Search Bar
      item {
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = { Text("Search all tasks...", fontSize = 13.sp) },
          leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
          trailingIcon = {
            if (searchQuery.isNotEmpty()) {
              IconButton(onClick = { searchQuery = "" }) {
                Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
              }
            }
          },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("all_tasks_search_input"),
          shape = RoundedCornerShape(12.dp),
          singleLine = true,
          colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
          )
        )
      }

      val displayTasks = if (searchQuery.isBlank()) tasks else tasks.filter {
        it.title.contains(searchQuery, ignoreCase = true) ||
            it.description.contains(searchQuery, ignoreCase = true) ||
            it.subtasks.any { s -> s.title.contains(searchQuery, ignoreCase = true) }
      }

      if (displayTasks.isEmpty()) {
        item {
          EmptyStateBanner(
            title = if (searchQuery.isBlank()) "No tasks in workspace yet" else "No matching tasks found",
            subtitle = "Tap 'New Task' or use Breakdown Lab to create structured goals."
          )
        }
      } else {
        items(displayTasks, key = { "all_task_${it.id}" }) { task ->
          TaskCard(
            task = task,
            onToggleSubtask = { sub -> onToggleSubtask(sub, task) },
            onStartTimer = { },
            onDeleteSubtask = { sub -> onDeleteSubtask(sub, task) },
            onDeleteTask = { onDeleteTask(task) },
            onEditTask = { onEditTask(task) },
            onExportCalendar = { onExportCalendar(task) },
            onAddSubtask = { onAddSubtask(task) },
            onGenerateAiSubtasks = { onGenerateAiSubtasks(task) }
          )
        }
      }
    }

    // --- VIEW MODE 3: FILTERED VIEW ---
    if (selectedViewMode == DashboardViewMode.FILTERED) {
      item {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Filtered View (${filteredTasks.size})",
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )

            if (statusFilter != StatusFilterOption.ALL || categoryFilter != null || priorityFilter != null || searchQuery.isNotBlank()) {
              TextButton(
                onClick = {
                  statusFilter = StatusFilterOption.ALL
                  categoryFilter = null
                  priorityFilter = null
                  searchQuery = ""
                }
              ) {
                Text("Reset Filters", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
              }
            }
          }

          // Search Field
          OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Filter by keyword...", fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
          )

          // Status Filter Row
          Text("STATUS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            StatusFilterOption.values().forEach { opt ->
              val isSelected = opt == statusFilter
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                  .clip(RoundedCornerShape(8.dp))
                  .clickable { statusFilter = opt }
              ) {
                Text(
                  text = opt.label,
                  fontSize = 11.sp,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                  color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
              }
            }
          }

          // Priority Filter Row
          Text("PRIORITY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = if (priorityFilter == null) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
              modifier = Modifier.clickable { priorityFilter = null }
            ) {
              Text(
                text = "All Priorities",
                fontSize = 11.sp,
                fontWeight = if (priorityFilter == null) FontWeight.Bold else FontWeight.Medium,
                color = if (priorityFilter == null) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
              )
            }

            Priority.values().forEach { prio ->
              val isSelected = prio == priorityFilter
              val prioColor = when (prio) {
                Priority.LOW -> PriorityLowColor
                Priority.MEDIUM -> PriorityMediumColor
                Priority.HIGH -> PriorityHighColor
                Priority.URGENT -> PriorityUrgentColor
              }
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) prioColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.clickable { priorityFilter = if (isSelected) null else prio }
              ) {
                Text(
                  text = prio.label,
                  fontSize = 11.sp,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                  color = if (isSelected) prioColor else MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
              }
            }
          }

          // Category Filter Row
          Text("CATEGORY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = if (categoryFilter == null) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
              modifier = Modifier.clickable { categoryFilter = null }
            ) {
              Text(
                text = "All Categories",
                fontSize = 11.sp,
                fontWeight = if (categoryFilter == null) FontWeight.Bold else FontWeight.Medium,
                color = if (categoryFilter == null) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
              )
            }

            TaskCategory.values().forEach { cat ->
              val isSelected = cat == categoryFilter
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.clickable { categoryFilter = if (isSelected) null else cat }
              ) {
                Text(
                  text = cat.label,
                  fontSize = 11.sp,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                  color = if (isSelected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
              }
            }
          }
        }
      }

      if (filteredTasks.isEmpty()) {
        item {
          EmptyStateBanner(
            title = "No tasks match the active filters",
            subtitle = "Try adjusting your priority, category, or search filters above."
          )
        }
      } else {
        items(filteredTasks, key = { "filtered_task_${it.id}" }) { task ->
          TaskCard(
            task = task,
            onToggleSubtask = { sub -> onToggleSubtask(sub, task) },
            onStartTimer = { },
            onDeleteSubtask = { sub -> onDeleteSubtask(sub, task) },
            onDeleteTask = { onDeleteTask(task) },
            onEditTask = { onEditTask(task) },
            onExportCalendar = { onExportCalendar(task) },
            onAddSubtask = { onAddSubtask(task) },
            onGenerateAiSubtasks = { onGenerateAiSubtasks(task) }
          )
        }
      }
    }

    // --- VIEW MODE 4: TIMELINE VIEW ---
    if (selectedViewMode == DashboardViewMode.TIMELINE) {
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Timeline Schedule",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )

          IconButton(
            onClick = onNavigateToSync,
            modifier = Modifier.size(28.dp).testTag("timeline_sync_btn")
          ) {
            Icon(
              imageVector = Icons.Default.Sync,
              contentDescription = "Sync Timeline",
              tint = BrandSecondary,
              modifier = Modifier.size(18.dp)
            )
          }
        }
      }

      // Schedule Event Blocks
      item {
        Surface(
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .testTag("timeline_container_card"),
          color = MaterialTheme.colorScheme.surfaceContainer,
          shape = RoundedCornerShape(16.dp)
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            TimelineEventRow(
              time = "09:00",
              ampm = "AM",
              title = "Daily Standup & Sync",
              location = "Google Meet",
              barColor = BrandSecondary,
              icon = Icons.Default.Videocam
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), thickness = 1.dp)

            TimelineEventRow(
              time = "10:30",
              ampm = "AM",
              title = activeTask?.title ?: "Deep Work Execution Sprint",
              location = "Workspace Focus Block",
              barColor = HyperCyan,
              icon = Icons.Default.Schedule,
              isHighlighted = true
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), thickness = 1.dp)

            TimelineEventRow(
              time = "01:00",
              ampm = "PM",
              title = "Lunch & Recovery",
              location = "Personal",
              barColor = BrandTertiary,
              icon = Icons.Default.Restaurant
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), thickness = 1.dp)

            TimelineEventRow(
              time = "03:30",
              ampm = "PM",
              title = "Stakeholder Review & Decompose",
              location = "Conference Rm B",
              barColor = MaterialTheme.colorScheme.outlineVariant,
              icon = Icons.Default.Groups
            )
          }
        }
      }

      // Upcoming Milestones Section
      item {
        Text(
          text = "Scheduled Task Milestones",
          fontSize = 15.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.padding(top = 8.dp)
        )
      }

      val allSubtasks = tasks.flatMap { t -> t.subtasks.map { sub -> Pair(t, sub) } }
      if (allSubtasks.isEmpty()) {
        item {
          EmptyStateBanner(
            title = "No milestones scheduled",
            subtitle = "Add tasks with milestone breakdowns to see them scheduled chronologically."
          )
        }
      } else {
        items(allSubtasks.take(6), key = { "timeline_sub_${it.second.id}" }) { (parentTask, subtask) ->
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
              ) {
                Box(
                  modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (subtask.isCompleted) EmeraldSuccess else MaterialTheme.colorScheme.primary)
                )

                Column {
                  Text(
                    text = subtask.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                  Text(
                    text = "${parentTask.title} • ${subtask.estimatedMinutes}m",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }

              Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
              ) {
                Text(
                  text = subtask.priority.label,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
            }
          }
        }
      }
    }

    // --- VIEW MODE 5: CALENDAR VIEW ---
    if (selectedViewMode == DashboardViewMode.CALENDAR) {
      item {
        Surface(
          shape = RoundedCornerShape(20.dp),
          color = MaterialTheme.colorScheme.surface,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            // Month Header
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
                Icon(Icons.Default.Schedule, contentDescription = "Prev Month")
              }

              Text(
                text = monthYearFormat.format(currentMonthCalendar.time),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )

              IconButton(
                onClick = {
                  val newCal = currentMonthCalendar.clone() as Calendar
                  newCal.add(Calendar.MONTH, 1)
                  currentMonthCalendar = newCal
                }
              ) {
                Icon(Icons.Default.DateRange, contentDescription = "Next Month")
              }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Days of week
            val daysOfWeek = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceAround
            ) {
              daysOfWeek.forEach { dayName ->
                Text(
                  text = dayName,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  textAlign = TextAlign.Center,
                  modifier = Modifier.weight(1f)
                )
              }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Month Grid
            DashboardMonthGrid(
              calendar = currentMonthCalendar,
              selectedCalendar = selectedDateCalendar,
              tasksByDate = tasksByDate,
              onSelectDate = { selectedDateCalendar = it }
            )
          }
        }
      }

      // Selected Date Tasks List Header
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
              text = "${tasksOnSelectedDate.size} scheduled for this day",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          OutlinedButton(
            onClick = onAddNewTask,
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
          ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Add Task", fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }
        }
      }

      if (tasksOnSelectedDate.isEmpty()) {
        item {
          EmptyStateBanner(
            title = "No tasks due on this date",
            subtitle = "Tap 'Add Task' to schedule a task or milestone for this day."
          )
        }
      } else {
        items(tasksOnSelectedDate, key = { "cal_day_task_${it.id}" }) { task ->
          TaskCard(
            task = task,
            onToggleSubtask = { sub -> onToggleSubtask(sub, task) },
            onStartTimer = { },
            onDeleteSubtask = { sub -> onDeleteSubtask(sub, task) },
            onDeleteTask = { onDeleteTask(task) },
            onEditTask = { onEditTask(task) },
            onExportCalendar = { onExportCalendar(task) },
            onAddSubtask = { onAddSubtask(task) },
            onGenerateAiSubtasks = { onGenerateAiSubtasks(task) }
          )
        }
      }
    }
  }
}

@Composable
private fun DashboardMonthGrid(
  calendar: Calendar,
  selectedCalendar: Calendar,
  tasksByDate: Map<String, List<Task>>,
  onSelectDate: (Calendar) -> Unit
) {
  val cal = calendar.clone() as Calendar
  cal.set(Calendar.DAY_OF_MONTH, 1)

  val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1
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
                .height(38.dp)
                .padding(2.dp)
                .clip(RoundedCornerShape(8.dp))
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
                  fontSize = 12.sp,
                  fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                  color = when {
                    isSelected -> MaterialTheme.colorScheme.onPrimary
                    isToday -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface
                  }
                )

                if (tasksForDay.isNotEmpty()) {
                  Box(
                    modifier = Modifier
                      .padding(top = 2.dp)
                      .size(4.dp)
                      .clip(CircleShape)
                      .background(if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary)
                  )
                } else {
                  Spacer(modifier = Modifier.height(4.dp))
                }
              }
            }
          } else {
            Spacer(modifier = Modifier.weight(1f))
          }
        }
      }
    }
  }
}

@Composable
private fun EmptyStateBanner(title: String, subtitle: String) {
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
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = subtitle,
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
      )
    }
  }
}

@Composable
private fun ExecutiveProgressHeroCard(
  overallProgressFraction: Float,
  completionPercentage: Int,
  completedTasksCount: Int,
  totalTasksCount: Int,
  completedSubtasksCount: Int,
  totalSubtasksCount: Int,
  activeTasksCount: Int,
  totalOverdueCount: Int,
  modifier: Modifier = Modifier
) {
  Surface(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
      .testTag("executive_progress_hero_card"),
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    shape = RoundedCornerShape(16.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
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
              .size(32.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Analytics,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(18.dp)
            )
          }
          Column {
            Text(
              text = "Overall Execution",
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = if (totalTasksCount == 0) "No tasks yet" else "$completedTasksCount of $totalTasksCount goals achieved",
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        // Status Badge
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = when {
            totalOverdueCount > 0 -> PriorityUrgentColor.copy(alpha = 0.15f)
            totalTasksCount > 0 && activeTasksCount == 0 -> EmeraldSuccess.copy(alpha = 0.15f)
            else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
          }
        ) {
          Text(
            text = when {
              totalOverdueCount > 0 -> "$totalOverdueCount Overdue"
              totalTasksCount > 0 && activeTasksCount == 0 -> "All Completed"
              else -> "$activeTasksCount Active"
            },
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = when {
              totalOverdueCount > 0 -> PriorityUrgentColor
              totalTasksCount > 0 && activeTasksCount == 0 -> EmeraldSuccess
              else -> MaterialTheme.colorScheme.primary
            },
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }
      }

      // Progress Bar & Percentage
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.Bottom
        ) {
          Text(
            text = "$completionPercentage% Subtask Completion",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "$completedSubtasksCount / $totalSubtasksCount steps",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
        ) {
          Box(
            modifier = Modifier
              .fillMaxWidth(overallProgressFraction.coerceIn(0f, 1f))
              .height(8.dp)
              .clip(RoundedCornerShape(4.dp))
              .background(
                Brush.horizontalGradient(
                  listOf(
                    BrandSecondary,
                    ElectricCyan,
                    EmeraldSuccess
                  )
                )
              )
          )
        }
      }
    }
  }
}

@Composable
private fun VelocityMetricCard(
  title: String,
  value: String,
  unit: String,
  valueColor: Color,
  icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
  modifier: Modifier = Modifier
) {
  Surface(
    modifier = modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
    color = MaterialTheme.colorScheme.surfaceContainer,
    shape = RoundedCornerShape(12.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = title,
          fontSize = 10.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 0.5.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (icon != null) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = valueColor.copy(alpha = 0.8f),
            modifier = Modifier.size(14.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
      ) {
        Text(
          text = value,
          fontSize = 22.sp,
          fontWeight = FontWeight.Bold,
          color = valueColor
        )
        if (unit.isNotEmpty()) {
          Text(
            text = unit,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 2.dp)
          )
        }
      }
    }
  }
}

@Composable
private fun DashboardStatusBadgeCard(
  title: String,
  count: String,
  subtitle: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  accentColor: Color,
  modifier: Modifier = Modifier
) {
  Surface(
    modifier = modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
    color = MaterialTheme.colorScheme.surfaceContainer,
    shape = RoundedCornerShape(12.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 10.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = title,
          fontSize = 9.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 0.5.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = accentColor,
          modifier = Modifier.size(13.dp)
        )
      }

      Text(
        text = count,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        color = accentColor
      )

      Text(
        text = subtitle,
        fontSize = 10.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

@Composable
private fun PriorityMetricPill(
  label: String,
  count: Int,
  color: Color,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(3.dp)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      Box(
        modifier = Modifier
          .size(7.dp)
          .clip(CircleShape)
          .background(color)
      )
      Text(
        text = label,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
    Text(
      text = "$count",
      fontSize = 16.sp,
      fontWeight = FontWeight.Bold,
      color = if (count > 0) color else MaterialTheme.colorScheme.onSurfaceVariant
    )
  }
}

@Composable
private fun getCategoryIcon(cat: TaskCategory): androidx.compose.ui.graphics.vector.ImageVector {
  return when (cat) {
    TaskCategory.WORK -> Icons.Default.Groups
    TaskCategory.CODING -> Icons.Default.Tune
    TaskCategory.STUDY -> Icons.Default.Timeline
    TaskCategory.LIFE -> Icons.Default.Restaurant
    TaskCategory.HEALTH -> Icons.Default.Speed
    TaskCategory.PROJECT -> Icons.Default.AutoAwesome
  }
}

@Composable
private fun CategoryProgressRow(
  category: TaskCategory,
  taskCount: Int,
  progress: Float,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(4.dp)
  ) {
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
          imageVector = getCategoryIcon(category),
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(14.dp)
        )
        Text(
          text = category.label,
          fontSize = 13.sp,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface
        )
      }

      Text(
        text = "$taskCount ${if (taskCount == 1) "task" else "tasks"} • ${(progress * 100).toInt()}%",
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(5.dp)
        .clip(RoundedCornerShape(3.dp))
        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth(progress.coerceIn(0f, 1f))
          .height(5.dp)
          .clip(RoundedCornerShape(3.dp))
          .background(
            if (progress >= 1f) EmeraldSuccess
            else MaterialTheme.colorScheme.primary
          )
      )
    }
  }
}

@Composable
private fun TimelineEventRow(
  time: String,
  ampm: String,
  title: String,
  location: String,
  barColor: Color,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  isHighlighted: Boolean = false,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(10.dp))
      .background(if (isHighlighted) BrandTertiary.copy(alpha = 0.08f) else Color.Transparent)
      .padding(8.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(42.dp)
      ) {
        Text(
          text = time,
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold,
          color = barColor
        )
        Text(
          text = ampm,
          fontSize = 9.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      Box(
        modifier = Modifier
          .width(3.dp)
          .height(34.dp)
          .clip(RoundedCornerShape(2.dp))
          .background(barColor)
      )

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(2.dp))
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(13.dp)
          )
          Text(
            text = location,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }
  }
}
