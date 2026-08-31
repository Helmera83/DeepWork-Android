package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.Task
import com.example.data.model.TaskCategory
import com.example.ui.components.AddManualSubtaskDialog
import com.example.ui.components.AiBreakdownDialog
import com.example.ui.components.AiVoiceConversationDialog
import com.example.ui.components.CalendarExportDialog
import com.example.ui.components.CalendarView
import com.example.ui.components.FocusTimerDialog
import com.example.ui.components.GenerateMoreSubtasksDialog
import com.example.ui.components.ProgressHero
import com.example.ui.components.SyncAccountsView
import com.example.ui.components.SyncBackupDialog
import com.example.ui.components.TaskCard
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.EmeraldSuccess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskBreakScreen(
  viewModel: TaskViewModel = viewModel(),
  modifier: Modifier = Modifier
) {
  val tasks by viewModel.tasks.collectAsState()
  val syncAccounts by viewModel.syncAccounts.collectAsState()
  val themeMode by viewModel.themeMode.collectAsState()
  val searchQuery by viewModel.searchQuery.collectAsState()
  val selectedCategory by viewModel.selectedCategoryFilter.collectAsState()
  val isGeneratingAi by viewModel.isGeneratingAiBreakdown.collectAsState()
  val uiFeedback by viewModel.uiFeedback.collectAsState()
  val timerState by viewModel.timerState.collectAsState()
  val syncStatus by viewModel.syncManager.syncStatus.collectAsState()
  val lastSyncTime by viewModel.syncManager.lastSyncTimestamp.collectAsState()
  val isOfflineMode by viewModel.syncManager.isOfflineMode.collectAsState()
  val isVaultUnlocked by viewModel.cryptoManager.isVaultUnlocked.collectAsState()

  val snackbarHostState = remember { SnackbarHostState() }

  // Dialog States
  var showAiBreakdownDialog by remember { mutableStateOf(false) }
  var showVoiceCoachDialog by remember { mutableStateOf(false) }
  var defaultDeadlineForNewTask by remember { mutableStateOf<Long?>(null) }
  var showSyncDialog by remember { mutableStateOf(false) }
  var taskForCalendarExport by remember { mutableStateOf<Task?>(null) }
  var taskForAddSubtask by remember { mutableStateOf<Task?>(null) }
  var taskForGenerateMoreAi by remember { mutableStateOf<Task?>(null) }
  var isSearchExpanded by remember { mutableStateOf(false) }
  var currentNavTab by remember { mutableIntStateOf(0) }

  // Handle feedback snackbars
  LaunchedEffect(uiFeedback) {
    uiFeedback?.let { feedback ->
      snackbarHostState.showSnackbar(feedback.message)
      viewModel.clearFeedback()
    }
  }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.background,
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      TopAppBar(
        title = {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Box(
              modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = when (currentNavTab) {
                  1 -> Icons.Default.CalendarMonth
                  2 -> Icons.Default.CloudSync
                  else -> Icons.Default.AutoAwesome
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
              )
            }
            Text(
              text = when (currentNavTab) {
                1 -> "Calendar & Schedule"
                2 -> "Sync & Accounts"
                else -> "Tasks"
              },
              fontSize = 20.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
          }
        },
        actions = {
          if (currentNavTab == 0) {
            // Live Voice AI Coach Quick Button
            IconButton(
              onClick = { showVoiceCoachDialog = true },
              modifier = Modifier.testTag("topbar_voice_coach_btn")
            ) {
              Icon(
                imageVector = Icons.Default.RecordVoiceOver,
                contentDescription = "Voice Task Coach",
                tint = MaterialTheme.colorScheme.primary
              )
            }

            // Search Toggle for Tasks tab
            IconButton(
              onClick = {
                isSearchExpanded = !isSearchExpanded
                if (!isSearchExpanded) viewModel.setSearchQuery("")
              }
            ) {
              Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = if (isSearchExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          // Overflow Settings & Quick Actions Menu
          var showMenu by remember { mutableStateOf(false) }
          IconButton(onClick = { showMenu = true }) {
            Icon(
              imageVector = Icons.Default.MoreVert,
              contentDescription = "More Options",
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier.clip(RoundedCornerShape(14.dp))
          ) {
            DropdownMenuItem(
              text = {
                Text(
                  text = "Sync Now",
                  fontWeight = FontWeight.Medium
                )
              },
              leadingIcon = {
                Icon(
                  imageVector = Icons.Default.CloudSync,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary
                )
              },
              onClick = {
                showMenu = false
                viewModel.triggerCloudSync()
              }
            )

            DropdownMenuItem(
              text = {
                Text(
                  text = when (themeMode) {
                    AppThemeMode.DARK -> "Theme: Dark"
                    AppThemeMode.LIGHT -> "Theme: Light"
                    AppThemeMode.SYSTEM -> "Theme: System"
                  },
                  fontWeight = FontWeight.Medium
                )
              },
              leadingIcon = {
                Icon(
                  imageVector = when (themeMode) {
                    AppThemeMode.DARK -> Icons.Default.DarkMode
                    AppThemeMode.LIGHT -> Icons.Default.LightMode
                    AppThemeMode.SYSTEM -> Icons.Default.SettingsBrightness
                  },
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
              },
              onClick = {
                val next = when (themeMode) {
                  AppThemeMode.SYSTEM -> AppThemeMode.DARK
                  AppThemeMode.DARK -> AppThemeMode.LIGHT
                  AppThemeMode.LIGHT -> AppThemeMode.SYSTEM
                }
                viewModel.setThemeMode(next)
                showMenu = false
              }
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.background,
          scrolledContainerColor = MaterialTheme.colorScheme.surface
        )
      )
    },
    bottomBar = {
      NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        windowInsets = WindowInsets.navigationBars
      ) {
        NavigationBarItem(
          selected = currentNavTab == 0,
          onClick = { currentNavTab = 0 },
          icon = { Icon(Icons.Default.TaskAlt, contentDescription = "Tasks") },
          label = { Text("Tasks", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
          colors = NavigationBarItemDefaults.colors(
            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
            selectedIconColor = MaterialTheme.colorScheme.primary,
            selectedTextColor = MaterialTheme.colorScheme.primary
          )
        )

        NavigationBarItem(
          selected = currentNavTab == 1,
          onClick = { currentNavTab = 1 },
          icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "Calendar") },
          label = { Text("Calendar", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
          colors = NavigationBarItemDefaults.colors(
            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
            selectedIconColor = MaterialTheme.colorScheme.primary,
            selectedTextColor = MaterialTheme.colorScheme.primary
          )
        )

        NavigationBarItem(
          selected = currentNavTab == 2,
          onClick = { currentNavTab = 2 },
          icon = { Icon(Icons.Default.CloudSync, contentDescription = "Sync") },
          label = { Text("Sync", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
          colors = NavigationBarItemDefaults.colors(
            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
            selectedIconColor = MaterialTheme.colorScheme.primary,
            selectedTextColor = MaterialTheme.colorScheme.primary
          )
        )
      }
    },
    floatingActionButton = {
      if (currentNavTab != 2) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Voice Conversation Assistant FAB
          FloatingActionButton(
            onClick = { showVoiceCoachDialog = true },
            modifier = Modifier.testTag("voice_coach_fab"),
            shape = RoundedCornerShape(18.dp),
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Icon(
                imageVector = Icons.Default.RecordVoiceOver,
                contentDescription = "Voice Task Coach",
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(22.dp)
              )
              Text(
                text = "Voice Coach",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
              )
            }
          }

          // Structured AI Breakdown FAB
          FloatingActionButton(
            onClick = {
              defaultDeadlineForNewTask = if (currentNavTab == 1) System.currentTimeMillis() + 86400000L else null
              showAiBreakdownDialog = true
            },
            modifier = Modifier.testTag("ai_breakdown_fab"),
            shape = RoundedCornerShape(18.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
          ) {
            Icon(
              imageVector = Icons.Default.AutoAwesome,
              contentDescription = "Break Down Task with AI",
              tint = MaterialTheme.colorScheme.onPrimary,
              modifier = Modifier.size(24.dp)
            )
          }
        }
      }
    }
  ) { paddingValues ->
    when (currentNavTab) {
      // Tab 0: Main Tasks View
      0 -> {
        LazyColumn(
          modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
          contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 88.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          // Search Box (collapsible)
          if (isSearchExpanded) {
            item {
              OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search tasks or sub-tasks...") },
                leadingIcon = {
                  Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                },
                trailingIcon = {
                  if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                      Icon(Icons.Default.Clear, contentDescription = "Clear search")
                    }
                  }
                },
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("search_input"),
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = MaterialTheme.colorScheme.primary,
                  unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                )
              )
            }
          }

          // Minimalist Progress Summary
          item {
            ProgressHero(
              tasks = tasks,
              syncStatus = syncStatus,
              isOfflineMode = isOfflineMode,
              isVaultUnlocked = isVaultUnlocked
            )
          }

          // Expressive Category Filter Pills
          item {
            CategoryFilterRow(
              selectedCategory = selectedCategory,
              onSelectCategory = { viewModel.setCategoryFilter(it) }
            )
          }

          // Tasks List Header
          item {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = if (selectedCategory != null) "${selectedCategory!!.label} Tasks" else "All Tasks (${tasks.size})",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
            }
          }

          // Task Items or Minimalist Empty State
          if (tasks.isEmpty()) {
            item {
              EmptyTasksState(
                onQuickBreakdown = { title ->
                  viewModel.createTaskWithAi(
                    title = title,
                    description = "Automated productivity breakdown",
                    deadlineTimestamp = System.currentTimeMillis() + 86400000L * 3
                  )
                },
                onOpenVoiceCoach = { showVoiceCoachDialog = true }
              )
            }
          } else {
            items(tasks, key = { it.id }) { task ->
              TaskCard(
                task = task,
                onToggleSubtask = { sub -> viewModel.toggleSubtask(sub, task) },
                onStartTimer = { sub -> viewModel.startFocusTimer(task, sub) },
                onDeleteSubtask = { sub -> viewModel.deleteSubtask(sub.id, task.id) },
                onDeleteTask = { viewModel.deleteTask(task) },
                onExportCalendar = { taskForCalendarExport = task },
                onAddSubtask = { taskForAddSubtask = task },
                onGenerateAiSubtasks = { taskForGenerateMoreAi = task }
              )
            }
          }
        }
      }

      // Tab 1: Interactive Calendar View
      1 -> {
        CalendarView(
          tasks = tasks,
          calendarManager = viewModel.calendarManager,
          onToggleSubtask = { sub, parent -> viewModel.toggleSubtask(sub, parent) },
          onStartTimer = { sub, parent -> viewModel.startFocusTimer(parent, sub) },
          onAddNewTask = { deadline ->
            defaultDeadlineForNewTask = deadline
            showAiBreakdownDialog = true
          },
          modifier = Modifier.padding(paddingValues)
        )
      }

      // Tab 2: Sync & Accounts View
      2 -> {
        SyncAccountsView(
          syncManager = viewModel.syncManager,
          syncStatus = syncStatus,
          lastSyncTime = lastSyncTime,
          isOfflineMode = isOfflineMode,
          syncAccounts = syncAccounts,
          tasks = tasks,
          onTriggerSyncAll = { viewModel.triggerCloudSync() },
          onSyncAccount = { accountId -> viewModel.syncAccount(accountId) },
          onAddAccount = { account -> viewModel.addSyncAccount(account) },
          onUpdateAccount = { account -> viewModel.updateSyncAccount(account) },
          onDeleteAccount = { accountId, name -> viewModel.deleteSyncAccount(accountId, name) },
          onToggleAutoSync = { accountId, enabled -> viewModel.toggleAccountAutoSync(accountId, enabled) },
          onImportBackup = { json, wasEncrypted -> viewModel.importBackup(json, wasEncrypted) },
          modifier = Modifier.padding(paddingValues)
        )
      }
    }
  }

  // --- Modal Dialogs ---

  // 0. AI Live Voice Conversation Dialog
  if (showVoiceCoachDialog) {
    AiVoiceConversationDialog(
      voiceManager = viewModel.voiceManager,
      onApplyBreakdown = { title, desc, breakdown, deadline ->
        viewModel.applyVoiceBreakdownToWorkspace(
          taskTitle = title,
          taskDescription = desc,
          breakdown = breakdown,
          deadlineTimestamp = deadline
        )
      },
      onDismiss = { showVoiceCoachDialog = false }
    )
  }

  // 1. AI Task Breakdown Dialog
  if (showAiBreakdownDialog) {
    AiBreakdownDialog(
      onDismiss = {
        showAiBreakdownDialog = false
        defaultDeadlineForNewTask = null
      },
      onCreateTask = { title, desc, deadline ->
        viewModel.createTaskWithAi(
          title = title,
          description = desc,
          deadlineTimestamp = deadline ?: defaultDeadlineForNewTask
        )
      },
      isLoading = isGeneratingAi,
      onOpenVoiceCoach = {
        showAiBreakdownDialog = false
        showVoiceCoachDialog = true
      }
    )
  }

  // 2. Focus Timer Sheet
  if (timerState.isRunning) {
    FocusTimerDialog(
      timerState = timerState,
      onPauseToggle = { viewModel.pauseTimer() },
      onAddFiveMinutes = { viewModel.addMinutesToTimer(5) },
      onFinishAndLog = { viewModel.finishTimer() },
      onCancel = { viewModel.cancelTimer() }
    )
  }

  // 3. Calendar Export Sheet
  taskForCalendarExport?.let { task ->
    CalendarExportDialog(
      task = task,
      calendarManager = viewModel.calendarManager,
      onDismiss = { taskForCalendarExport = null }
    )
  }

  // 4. Add Manual Subtask Dialog
  taskForAddSubtask?.let { task ->
    AddManualSubtaskDialog(
      taskTitle = task.title,
      onAddSubtask = { title, time, notes, prio, tag ->
        viewModel.addCustomSubtask(task.id, title, time, notes, prio, tag)
      },
      onDismiss = { taskForAddSubtask = null }
    )
  }

  // 5. Add More AI Subtasks Dialog
  taskForGenerateMoreAi?.let { task ->
    GenerateMoreSubtasksDialog(
      task = task,
      onGenerate = { instructions, count ->
        viewModel.generateMoreAiSubtasks(task, instructions, count)
      },
      isLoading = isGeneratingAi,
      onDismiss = { taskForGenerateMoreAi = null }
    )
  }
}

@Composable
private fun CategoryFilterRow(
  selectedCategory: TaskCategory?,
  onSelectCategory: (TaskCategory?) -> Unit
) {
  val scrollState = rememberScrollState()

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .horizontalScroll(scrollState),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    FilterPill(
      label = "All",
      isSelected = selectedCategory == null,
      onClick = { onSelectCategory(null) }
    )

    TaskCategory.entries.forEach { cat ->
      FilterPill(
        label = cat.label,
        isSelected = selectedCategory == cat,
        onClick = { onSelectCategory(if (selectedCategory == cat) null else cat) }
      )
    }
  }
}

@Composable
private fun FilterPill(
  label: String,
  isSelected: Boolean,
  onClick: () -> Unit
) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
    border = androidx.compose.foundation.BorderStroke(
      1.dp,
      if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    ),
    modifier = Modifier.clickable(onClick = onClick)
  ) {
    Text(
      text = label,
      fontSize = 12.sp,
      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
      color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
    )
  }
}

@Composable
private fun EmptyTasksState(
  onQuickBreakdown: (title: String) -> Unit,
  onOpenVoiceCoach: () -> Unit
) {
  Surface(
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.surface,
    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
    tonalElevation = 2.dp,
    shadowElevation = 3.dp,
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Box(
        modifier = Modifier
          .size(52.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.AutoAwesome,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(26.dp)
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      Text(
        text = "No Tasks Scheduled",
        fontSize = 17.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = "Talk with your AI Voice Coach or try one of these quick starters:",
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(16.dp))

      // Voice Coach Card inside Empty State
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
        modifier = Modifier
          .fillMaxWidth()
          .clickable { onOpenVoiceCoach() }
          .testTag("empty_state_voice_coach_card")
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Icon(
              imageVector = Icons.Default.RecordVoiceOver,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.secondary,
              modifier = Modifier.size(22.dp)
            )
            Column {
              Text(
                text = "Talk to AI Voice Coach (Live)",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
              )
              Text(
                text = "Explain your goal out loud and get instant subtasks",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
              )
            }
          }
          Text(
            text = "Speak →",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        QuickStarterItem(
          title = "Build & Deploy REST API Backend",
          onClick = { onQuickBreakdown("Build & Deploy REST API Backend") }
        )
        QuickStarterItem(
          title = "Complete 5k Marathon Training Plan",
          onClick = { onQuickBreakdown("Complete 5k Marathon Training Plan") }
        )
        QuickStarterItem(
          title = "Study for Machine Learning Exam",
          onClick = { onQuickBreakdown("Study for Machine Learning Exam") }
        )
      }
    }
  }
}

@Composable
private fun QuickStarterItem(
  title: String,
  onClick: () -> Unit
) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurface
      )
      Text(
        text = "+ Start",
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
      )
    }
  }
}


