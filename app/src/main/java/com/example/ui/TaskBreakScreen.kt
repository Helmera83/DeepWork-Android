package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.Priority
import com.example.data.model.Task
import com.example.data.model.TaskCategory
import com.example.ui.components.AddManualSubtaskDialog
import com.example.ui.components.AiBreakdownDialog
import com.example.ui.components.AiVoiceConversationDialog
import com.example.ui.components.BreakdownLabView
import com.example.ui.components.CalendarExportDialog
import com.example.ui.components.EditTaskDialog
import com.example.ui.components.FocusTimerDialog
import com.example.ui.components.GenerateMoreSubtasksDialog
import com.example.ui.components.HomeDashboardView
import com.example.ui.components.SettingsDialog
import com.example.ui.components.SyncAccountsDialog
import com.example.ui.components.TaskAiChatbotView
import com.example.ui.components.TaskLogicTopBar
import com.example.ui.components.VaultSecurityDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskBreakScreen(
  viewModel: TaskViewModel = viewModel(),
  modifier: Modifier = Modifier
) {
  val tasks by viewModel.tasks.collectAsState()
  val syncAccounts by viewModel.syncAccounts.collectAsState()
  val themeMode by viewModel.themeMode.collectAsState()
  val isGeneratingAi by viewModel.isGeneratingAiBreakdown.collectAsState()
  val uiFeedback by viewModel.uiFeedback.collectAsState()
  val timerState by viewModel.timerState.collectAsState()
  val chatMessages by viewModel.chatMessages.collectAsState()
  val isChatThinking by viewModel.isChatThinking.collectAsState()
  val syncStatus by viewModel.syncManager.syncStatus.collectAsState()
  val lastSyncTime by viewModel.syncManager.lastSyncTimestamp.collectAsState()
  val isOfflineMode by viewModel.syncManager.isOfflineMode.collectAsState()
  val isVaultUnlocked by viewModel.cryptoManager.isVaultUnlocked.collectAsState()

  val snackbarHostState = remember { SnackbarHostState() }

  // Dialog States
  var showAiBreakdownDialog by remember { mutableStateOf(false) }
  var showVoiceCoachDialog by remember { mutableStateOf(false) }
  var showSettingsDialog by remember { mutableStateOf(false) }
  var showVaultSecurityDialog by remember { mutableStateOf(false) }
  var defaultDeadlineForNewTask by remember { mutableStateOf<Long?>(null) }
  var showSyncDialog by remember { mutableStateOf(false) }
  var taskForCalendarExport by remember { mutableStateOf<Task?>(null) }
  var taskForAddSubtask by remember { mutableStateOf<Task?>(null) }
  var taskForGenerateMoreAi by remember { mutableStateOf<Task?>(null) }
  var taskForEdit by remember { mutableStateOf<Task?>(null) }

  // Navigation tab state: 0 = Dashboard, 1 = Tasks (Decompose), 2 = AI Coach (Chatbot)
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
      TaskLogicTopBar(
        title = "TaskLogic AI",
        hasNotifications = true,
        onAvatarClick = { showSettingsDialog = true },
        onNotificationClick = { showSyncDialog = true }
      )
    },
    bottomBar = {
      NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        windowInsets = WindowInsets.navigationBars
      ) {
        // Tab 0: Dashboard (Overview, All Tasks, Filtered, Timeline, Calendar)
        NavigationBarItem(
          selected = currentNavTab == 0,
          onClick = { currentNavTab = 0 },
          icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
          label = { Text("Dashboard", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
          colors = NavigationBarItemDefaults.colors(
            indicatorColor = Color(0xFFB0EAEA),
            selectedIconColor = Color(0xFF2D6767),
            selectedTextColor = Color(0xFF2D6767),
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
          ),
          modifier = Modifier.testTag("nav_tab_dashboard")
        )

        // Tab 1: Tasks (Decomposition Lab & Workspace)
        NavigationBarItem(
          selected = currentNavTab == 1,
          onClick = { currentNavTab = 1 },
          icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "Tasks") },
          label = { Text("Tasks", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
          colors = NavigationBarItemDefaults.colors(
            indicatorColor = Color(0xFFB0EAEA),
            selectedIconColor = Color(0xFF2D6767),
            selectedTextColor = Color(0xFF2D6767),
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
          ),
          modifier = Modifier.testTag("nav_tab_breakdown")
        )

        // Tab 2: AI Coach (Task Analysis & Support Chatbot)
        NavigationBarItem(
          selected = currentNavTab == 2,
          onClick = { currentNavTab = 2 },
          icon = { Icon(Icons.Default.SmartToy, contentDescription = "AI Coach") },
          label = { Text("AI Coach", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
          colors = NavigationBarItemDefaults.colors(
            indicatorColor = Color(0xFFB0EAEA),
            selectedIconColor = Color(0xFF2D6767),
            selectedTextColor = Color(0xFF2D6767),
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
          ),
          modifier = Modifier.testTag("nav_tab_coach")
        )
      }
    }
  ) { paddingValues ->
    when (currentNavTab) {
      // Tab 0: Home Dashboard (Overview, All Tasks, Filtered, Timeline, Calendar)
      0 -> {
        HomeDashboardView(
          tasks = tasks,
          calendarManager = viewModel.calendarManager,
          onNavigateToBreakdown = { currentNavTab = 1 },
          onNavigateToAssistant = { currentNavTab = 2 },
          onNavigateToSync = { showSyncDialog = true },
          onToggleSubtask = { sub, parent -> viewModel.toggleSubtask(sub, parent) },
          onDeleteSubtask = { sub, parent -> viewModel.deleteSubtask(sub.id, parent.id) },
          onEditTask = { task -> taskForEdit = task },
          onDeleteTask = { task -> viewModel.deleteTask(task) },
          onExportCalendar = { task -> taskForCalendarExport = task },
          onAddSubtask = { task -> taskForAddSubtask = task },
          onGenerateAiSubtasks = { task -> taskForGenerateMoreAi = task },
          onCompleteTask = { task ->
            task.subtasks.forEach { sub ->
              if (!sub.isCompleted) viewModel.toggleSubtask(sub, task)
            }
          },
          onPostponeTask = { task ->
            viewModel.updateTaskDetails(
              task = task,
              title = task.title,
              description = task.description,
              deadlineTimestamp = (task.deadlineTimestamp ?: System.currentTimeMillis()) + 86400000L
            )
          },
          onAddNewTask = { showAiBreakdownDialog = true },
          modifier = Modifier.padding(paddingValues)
        )
      }

      // Tab 1: Tasks / Breakdown Lab (with Date Picker & Edit/Delete actions)
      1 -> {
        BreakdownLabView(
          tasks = tasks,
          isGenerating = isGeneratingAi,
          onDecomposeGoal = { title, cat, prio, desc, deadline ->
            viewModel.generateAiTaskBreakdown(
              title = title,
              userCategory = cat,
              userPriority = prio,
              description = desc.ifEmpty { "Decomposed into strategic milestones and actionable steps" },
              deadlineTimestamp = deadline
            )
          },
          onToggleSubTask = { taskId, subTaskId ->
            val parentTask = tasks.firstOrNull { it.id == taskId }
            val sub = parentTask?.subtasks?.firstOrNull { it.id == subTaskId }
            if (parentTask != null && sub != null) {
              viewModel.toggleSubtask(sub, parentTask)
            }
          },
          onEditTask = { task -> taskForEdit = task },
          onDeleteTask = { task -> viewModel.deleteTask(task) },
          onAddSubtask = { task -> taskForAddSubtask = task },
          onGenerateMoreAi = { task -> taskForGenerateMoreAi = task },
          onExportCalendar = { task -> taskForCalendarExport = task },
          onOpenVoiceAssistant = { showVoiceCoachDialog = true },
          onOpenManualAdd = { showAiBreakdownDialog = true },
          modifier = Modifier.padding(paddingValues)
        )
      }

      // Tab 2: AI Task Coach Chatbot (analyzes tasks and provides completion support)
      else -> {
        TaskAiChatbotView(
          tasks = tasks,
          chatMessages = chatMessages,
          isThinking = isChatThinking,
          onSendMessage = { prompt -> viewModel.sendChatMessage(prompt) },
          onClearChat = { viewModel.clearChat() },
          onOpenVoiceAssistant = { showVoiceCoachDialog = true },
          modifier = Modifier.padding(paddingValues)
        )
      }
    }
  }

  // --- Modal Dialogs ---

  // Edit Task Dialog
  taskForEdit?.let { taskToEdit ->
    EditTaskDialog(
      task = taskToEdit,
      onDismiss = { taskForEdit = null },
      onSave = { newTitle, newDesc, newCat, newPrio, newDeadline ->
        val updated = taskToEdit.copy(
          title = newTitle,
          description = newDesc,
          category = newCat,
          priority = newPrio,
          deadlineTimestamp = newDeadline,
          updatedAt = System.currentTimeMillis()
        )
        viewModel.updateTask(updated)
        taskForEdit = null
      }
    )
  }

  // Settings & Appearance Dialog
  if (showSettingsDialog) {
    SettingsDialog(
      themeMode = themeMode,
      onThemeModeChange = { viewModel.setThemeMode(it) },
      isOfflineMode = isOfflineMode,
      onToggleOfflineMode = { viewModel.syncManager.setOfflineMode(it) },
      isVaultUnlocked = isVaultUnlocked,
      onOpenVaultSecurity = { showVaultSecurityDialog = true },
      syncAccountsCount = syncAccounts.size,
      syncStatus = syncStatus,
      onOpenSyncAccounts = { showSyncDialog = true },
      onDismiss = { showSettingsDialog = false }
    )
  }

  // AI Live Voice Conversation Dialog
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

  // AI Task Breakdown Dialog
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

  // Focus Timer Dialog (if active)
  if (timerState.isRunning) {
    FocusTimerDialog(
      timerState = timerState,
      onPauseToggle = { viewModel.pauseTimer() },
      onAddFiveMinutes = { viewModel.addMinutesToTimer(5) },
      onFinishAndLog = { viewModel.finishTimer() },
      onCancel = { viewModel.cancelTimer() }
    )
  }

  // Calendar Export Dialog
  taskForCalendarExport?.let { taskToExport ->
    CalendarExportDialog(
      task = taskToExport,
      calendarManager = viewModel.calendarManager,
      onDismiss = { taskForCalendarExport = null }
    )
  }

  // Add Manual Subtask Dialog
  taskForAddSubtask?.let { parentTask ->
    AddManualSubtaskDialog(
      taskTitle = parentTask.title,
      onAddSubtask = { title, time, notes, prio, tag ->
        viewModel.addCustomSubtask(parentTask.id, title, time, notes, prio, tag)
      },
      onDismiss = { taskForAddSubtask = null }
    )
  }

  // Generate More AI Subtasks Dialog
  taskForGenerateMoreAi?.let { parentTask ->
    GenerateMoreSubtasksDialog(
      task = parentTask,
      onGenerate = { instructions, count ->
        viewModel.generateMoreAiSubtasks(parentTask, instructions, count)
      },
      isLoading = isGeneratingAi,
      onDismiss = { taskForGenerateMoreAi = null }
    )
  }

  // Sync / Backup / Export Dialog
  if (showSyncDialog) {
    SyncAccountsDialog(
      syncManager = viewModel.syncManager,
      syncStatus = syncStatus,
      lastSyncTime = lastSyncTime ?: 0L,
      isOfflineMode = isOfflineMode,
      syncAccounts = syncAccounts,
      tasks = tasks,
      onTriggerSyncAll = { viewModel.triggerCloudSync() },
      onSyncAccount = { accountId -> viewModel.syncAccount(accountId) },
      onAddAccount = { account -> viewModel.addSyncAccount(account) },
      onUpdateAccount = { account -> viewModel.updateSyncAccount(account) },
      onDeleteAccount = { accountId, name -> viewModel.deleteSyncAccount(accountId, name) },
      onToggleAutoSync = { accountId, enabled -> viewModel.toggleAccountAutoSync(accountId, enabled) },
      onImportBackup = { json, wasEncrypted ->
        viewModel.importBackup(json, wasEncrypted)
      },
      onDismiss = { showSyncDialog = false }
    )
  }

  // Security Vault PIN Dialog
  val hasCustomPassphrase by viewModel.cryptoManager.hasCustomPassphrase.collectAsState()
  if (showVaultSecurityDialog) {
    VaultSecurityDialog(
      cryptoManager = viewModel.cryptoManager,
      isVaultUnlocked = isVaultUnlocked,
      hasCustomPassphrase = hasCustomPassphrase,
      onUnlock = { pin -> viewModel.cryptoManager.unlockVault(pin) },
      onSetPassphrase = { pin -> viewModel.cryptoManager.setMasterPassphrase(pin) },
      onLock = { viewModel.cryptoManager.lockVault() },
      onDismiss = { showVaultSecurityDialog = false }
    )
  }
}
