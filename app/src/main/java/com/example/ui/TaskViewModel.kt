package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.BreakdownResult
import com.example.data.ai.GeminiBreakdownService
import com.example.data.ai.VoiceConversationManager
import com.example.data.calendar.CalendarIntegrationManager
import com.example.data.crypto.CryptoManager
import com.example.data.local.AppDatabase
import com.example.data.model.Priority
import com.example.data.model.SubTask
import com.example.data.model.Task
import com.example.data.model.TaskCategory
import com.example.data.notification.NotificationProgressHelper
import com.example.data.repository.TaskRepository
import com.example.data.sync.SyncManager
import com.example.ui.theme.AppThemeMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ActiveTimerState(
  val isRunning: Boolean = false,
  val isPaused: Boolean = false,
  val task: Task? = null,
  val subtask: SubTask? = null,
  val totalSeconds: Int = 25 * 60,
  val remainingSeconds: Int = 25 * 60
)

data class UiFeedback(
  val message: String,
  val isError: Boolean = false
)

class TaskViewModel(application: Application) : AndroidViewModel(application) {

  private val database = AppDatabase.getDatabase(application)
  val cryptoManager = CryptoManager(application)
  private val geminiService = GeminiBreakdownService()
  val notificationHelper = NotificationProgressHelper(application)
  val calendarManager = CalendarIntegrationManager(application)
  val syncManager = SyncManager(application, cryptoManager)
  val voiceManager = VoiceConversationManager(application, viewModelScope)

  val repository = TaskRepository(
    taskDao = database.taskDao(),
    geminiService = geminiService,
    notificationHelper = notificationHelper
  )

  // Preferences
  private val themePrefs = application.getSharedPreferences("taskbreak_theme_prefs", Context.MODE_PRIVATE)

  private val _themeMode = MutableStateFlow(
    when (themePrefs.getString("theme_mode", "SYSTEM")) {
      "DARK" -> AppThemeMode.DARK
      "LIGHT" -> AppThemeMode.LIGHT
      else -> AppThemeMode.SYSTEM
    }
  )
  val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

  // Filter & Search
  private val _searchQuery = MutableStateFlow("")
  val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

  private val _selectedCategoryFilter = MutableStateFlow<TaskCategory?>(null)
  val selectedCategoryFilter: StateFlow<TaskCategory?> = _selectedCategoryFilter.asStateFlow()

  private val _selectedPriorityFilter = MutableStateFlow<Priority?>(null)
  val selectedPriorityFilter: StateFlow<Priority?> = _selectedPriorityFilter.asStateFlow()

  // Selected / Active Task
  private val _selectedTask = MutableStateFlow<Task?>(null)
  val selectedTask: StateFlow<Task?> = _selectedTask.asStateFlow()

  // AI Breakdown Loading
  private val _isGeneratingAiBreakdown = MutableStateFlow(false)
  val isGeneratingAiBreakdown: StateFlow<Boolean> = _isGeneratingAiBreakdown.asStateFlow()

  private val _lastAiExplanation = MutableStateFlow<String?>(null)
  val lastAiExplanation: StateFlow<String?> = _lastAiExplanation.asStateFlow()

  // UI Feedback Toast/Snackbar
  private val _uiFeedback = MutableStateFlow<UiFeedback?>(null)
  val uiFeedback: StateFlow<UiFeedback?> = _uiFeedback.asStateFlow()

  // Focus Timer
  private val _timerState = MutableStateFlow(ActiveTimerState())
  val timerState: StateFlow<ActiveTimerState> = _timerState.asStateFlow()
  private var timerJob: Job? = null

  // All Tasks Flow
  val tasks: StateFlow<List<Task>> = combine(
    repository.allTasks,
    _searchQuery,
    _selectedCategoryFilter,
    _selectedPriorityFilter
  ) { list, query, category, priority ->
    list.filter { task ->
      val matchesQuery = query.isBlank() ||
          task.title.contains(query, ignoreCase = true) ||
          task.description.contains(query, ignoreCase = true) ||
          task.subtasks.any { it.title.contains(query, ignoreCase = true) || it.categoryTag.contains(query, ignoreCase = true) }
      val matchesCategory = category == null || task.category == category
      val matchesPriority = priority == null || task.priority == priority
      matchesQuery && matchesCategory && matchesPriority
    }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  init {
    viewModelScope.launch {
      repository.seedSampleTasksIfEmpty()
    }
  }

  fun setThemeMode(mode: AppThemeMode) {
    _themeMode.value = mode
    themePrefs.edit().putString("theme_mode", mode.name).apply()
  }

  fun setSearchQuery(query: String) {
    _searchQuery.value = query
  }

  fun setCategoryFilter(category: TaskCategory?) {
    _selectedCategoryFilter.value = category
  }

  fun setPriorityFilter(priority: Priority?) {
    _selectedPriorityFilter.value = priority
  }

  fun selectTask(task: Task?) {
    _selectedTask.value = task
  }

  fun clearFeedback() {
    _uiFeedback.value = null
  }

  fun createTaskWithAi(
    title: String,
    description: String = "",
    deadlineTimestamp: Long? = null
  ) {
    if (title.isBlank()) {
      _uiFeedback.value = UiFeedback("Please enter a task title", isError = true)
      return
    }

    viewModelScope.launch {
      _isGeneratingAiBreakdown.value = true
      try {
        val (createdTask, breakdownResult) = repository.createTaskWithAiBreakdown(
          title = title,
          description = description,
          deadLineTimestamp = deadlineTimestamp
        )
        _lastAiExplanation.value = breakdownResult.aiExplanation
        _selectedTask.value = createdTask
        _uiFeedback.value = UiFeedback("AI created task as ${createdTask.category.label} (${createdTask.priority.label} priority) with ${breakdownResult.subtasks.size} milestones & sub-tasks!")
      } catch (e: Exception) {
        _uiFeedback.value = UiFeedback("Error breaking down task: ${e.message}", isError = true)
      } finally {
        _isGeneratingAiBreakdown.value = false
      }
    }
  }

  fun toggleSubtask(subtask: SubTask, task: Task) {
    viewModelScope.launch {
      repository.toggleSubtaskCompletion(subtask, task)
      // Update selected task in memory if open
      if (_selectedTask.value?.id == task.id) {
        val updatedSubtasks = task.subtasks.map {
          if (it.id == subtask.id) it.copy(isCompleted = !it.isCompleted) else it
        }
        _selectedTask.value = task.copy(subtasks = updatedSubtasks)
      }
    }
  }

  fun addCustomSubtask(
    taskId: String,
    title: String,
    estimatedMinutes: Int,
    notes: String,
    priority: Priority = Priority.MEDIUM,
    categoryTag: String = "General"
  ) {
    if (title.isBlank()) return
    viewModelScope.launch {
      repository.addSubtask(
        taskId = taskId,
        title = title,
        estimatedMinutes = estimatedMinutes,
        notes = notes,
        priority = priority,
        categoryTag = categoryTag
      )
      _uiFeedback.value = UiFeedback("Sub-task added")
    }
  }

  fun generateMoreAiSubtasks(
    task: Task,
    userInstructions: String,
    count: Int
  ) {
    viewModelScope.launch {
      _isGeneratingAiBreakdown.value = true
      try {
        val result = repository.generateAndAppendAiSubtasks(
          task = task,
          userInstructions = userInstructions,
          count = count
        )
        _lastAiExplanation.value = result.aiExplanation
        _uiFeedback.value = UiFeedback("Generated ${result.subtasks.size} additional AI sub-tasks!")
      } catch (e: Exception) {
        _uiFeedback.value = UiFeedback("Error generating more sub-tasks: ${e.message}", isError = true)
      } finally {
        _isGeneratingAiBreakdown.value = false
      }
    }
  }

  fun deleteSubtask(subtaskId: String, taskId: String) {
    viewModelScope.launch {
      repository.deleteSubtask(subtaskId, taskId)
    }
  }

  fun deleteTask(task: Task) {
    viewModelScope.launch {
      repository.deleteTask(task)
      if (_selectedTask.value?.id == task.id) {
        _selectedTask.value = null
      }
      _uiFeedback.value = UiFeedback("Task deleted")
    }
  }

  // --- Focus Timer ---
  fun startFocusTimer(task: Task, subtask: SubTask, durationMinutes: Int = subtask.estimatedMinutes.coerceAtLeast(5)) {
    timerJob?.cancel()
    val totalSecs = durationMinutes * 60
    _timerState.value = ActiveTimerState(
      isRunning = true,
      isPaused = false,
      task = task,
      subtask = subtask,
      totalSeconds = totalSecs,
      remainingSeconds = totalSecs
    )

    notificationHelper.showFocusTimerNotification(subtask.title, totalSecs, totalSecs, false)

    timerJob = viewModelScope.launch {
      while (_timerState.value.remainingSeconds > 0 && _timerState.value.isRunning) {
        delay(1000)
        if (!_timerState.value.isPaused) {
          val remaining = _timerState.value.remainingSeconds - 1
          _timerState.value = _timerState.value.copy(remainingSeconds = remaining)

          if (remaining % 15 == 0 || remaining <= 10) {
            notificationHelper.showFocusTimerNotification(
              subtask.title,
              remaining,
              _timerState.value.totalSeconds,
              _timerState.value.isPaused
            )
          }

          if (remaining <= 0) {
            finishTimer(logMinutes = durationMinutes)
            break
          }
        }
      }
    }
  }

  fun pauseTimer() {
    val current = _timerState.value
    _timerState.value = current.copy(isPaused = !current.isPaused)
    current.subtask?.let { sub ->
      notificationHelper.showFocusTimerNotification(
        sub.title,
        current.remainingSeconds,
        current.totalSeconds,
        !current.isPaused
      )
    }
  }

  fun addMinutesToTimer(minutes: Int) {
    val current = _timerState.value
    val addedSec = minutes * 60
    _timerState.value = current.copy(
      totalSeconds = current.totalSeconds + addedSec,
      remainingSeconds = current.remainingSeconds + addedSec
    )
  }

  fun finishTimer(logMinutes: Int? = null) {
    timerJob?.cancel()
    val current = _timerState.value
    val sub = current.subtask
    val task = current.task

    if (sub != null && task != null) {
      val spentSec = current.totalSeconds - current.remainingSeconds
      val minutesSpent = logMinutes ?: (spentSec / 60).coerceAtLeast(1)

      viewModelScope.launch {
        repository.logSubtaskFocusTime(sub.id, task.id, minutesSpent)
        _uiFeedback.value = UiFeedback("Logged $minutesSpent mins focus time for \"${sub.title}\"")
      }
    }

    _timerState.value = ActiveTimerState()
    notificationHelper.cancelTimerNotification()
  }

  fun cancelTimer() {
    timerJob?.cancel()
    _timerState.value = ActiveTimerState()
    notificationHelper.cancelTimerNotification()
  }

  // --- Cloud Sync & Accounts ---
  val syncAccounts = syncManager.syncAccounts

  fun triggerCloudSync() {
    viewModelScope.launch {
      val reportResult = syncManager.performCloudSync(tasks.value)
      reportResult.onSuccess { report ->
        _uiFeedback.value = UiFeedback("Cloud Sync Complete: ${report.tasksCount} tasks synced")
      }.onFailure { err ->
        _uiFeedback.value = UiFeedback("Sync Notice: ${err.message}", isError = true)
      }
    }
  }

  fun syncAccount(accountId: String) {
    viewModelScope.launch {
      val reportResult = syncManager.syncAccount(accountId, tasks.value)
      reportResult.onSuccess { report ->
        _uiFeedback.value = UiFeedback("Account Synced (${report.tasksCount} tasks updated)")
      }.onFailure { err ->
        _uiFeedback.value = UiFeedback("Sync error: ${err.message}", isError = true)
      }
    }
  }

  fun addSyncAccount(account: com.example.data.model.SyncAccount) {
    syncManager.addSyncAccount(account)
    _uiFeedback.value = UiFeedback("Added sync account \"${account.accountName}\"")
  }

  fun updateSyncAccount(account: com.example.data.model.SyncAccount) {
    syncManager.updateSyncAccount(account)
    _uiFeedback.value = UiFeedback("Updated sync account \"${account.accountName}\"")
  }

  fun deleteSyncAccount(accountId: String, accountName: String) {
    syncManager.deleteSyncAccount(accountId)
    _uiFeedback.value = UiFeedback("Removed sync account \"$accountName\"")
  }

  fun toggleAccountAutoSync(accountId: String, enabled: Boolean) {
    syncManager.toggleAutoSync(accountId, enabled)
  }

  fun importBackup(jsonString: String, wasEncrypted: Boolean) {
    viewModelScope.launch {
      val imported = syncManager.importSyncPayload(jsonString, wasEncrypted)
      if (imported.isNotEmpty()) {
        repository.importTasks(imported)
        _uiFeedback.value = UiFeedback("Successfully imported ${imported.size} tasks!")
      } else {
        _uiFeedback.value = UiFeedback("Failed to parse backup payload", isError = true)
      }
    }
  }

  // --- Vault Helpers ---
  fun unlockVault(passphrase: String): Boolean {
    val success = cryptoManager.unlockVault(passphrase)
    if (success) {
      _uiFeedback.value = UiFeedback("E2EE Vault Unlocked")
    } else {
      _uiFeedback.value = UiFeedback("Incorrect Passphrase", isError = true)
    }
    return success
  }

  fun setMasterPassphrase(passphrase: String): Boolean {
    val success = cryptoManager.setMasterPassphrase(passphrase)
    if (success) {
      _uiFeedback.value = UiFeedback("Master Passphrase Configured")
    } else {
      _uiFeedback.value = UiFeedback("Passphrase too short (min 4 chars)", isError = true)
    }
    return success
  }

  fun lockVault() {
    cryptoManager.lockVault()
    _uiFeedback.value = UiFeedback("Vault Locked")
  }

  // --- Voice AI Breakdown Actions ---
  fun startVoiceListening() {
    voiceManager.startListening()
  }

  fun stopVoiceListening() {
    voiceManager.stopListening()
  }

  fun cancelVoiceListening() {
    voiceManager.cancelListening()
  }

  fun sendVoiceMessage(text: String) {
    voiceManager.processUserSpokenInput(text)
  }

  fun toggleVoiceTtsMute() {
    voiceManager.toggleTtsMute()
  }

  fun clearVoiceConversation() {
    voiceManager.clearConversation()
  }

  fun applyVoiceBreakdownToWorkspace(
    taskTitle: String,
    taskDescription: String,
    breakdown: BreakdownResult,
    deadlineTimestamp: Long? = null
  ) {
    viewModelScope.launch {
      try {
        val savedTask = repository.saveTaskWithPrecomputedBreakdown(
          title = taskTitle.ifBlank { "Voice Breakdown Task" },
          description = taskDescription,
          breakdown = breakdown,
          deadLineTimestamp = deadlineTimestamp
        )
        _selectedTask.value = savedTask
        _uiFeedback.value = UiFeedback("Added \"${savedTask.title}\" with ${savedTask.subtasks.size} sub-tasks to workspace!")
      } catch (e: Exception) {
        _uiFeedback.value = UiFeedback("Error saving voice breakdown: ${e.message}", isError = true)
      }
    }
  }

  override fun onCleared() {
    super.onCleared()
    voiceManager.destroy()
  }
}
