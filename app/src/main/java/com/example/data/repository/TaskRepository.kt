package com.example.data.repository

import com.example.data.ai.BreakdownResult
import com.example.data.ai.GeminiBreakdownService
import com.example.data.local.SubTaskEntity
import com.example.data.local.TaskDao
import com.example.data.local.TaskEntity
import com.example.data.model.Priority
import com.example.data.model.SubTask
import com.example.data.model.SyncState
import com.example.data.model.Task
import com.example.data.model.TaskCategory
import com.example.data.notification.NotificationProgressHelper
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TaskRepository(
  private val taskDao: TaskDao,
  private val geminiService: GeminiBreakdownService,
  private val notificationHelper: NotificationProgressHelper
) {

  val allTasks: Flow<List<Task>> = taskDao.getAllTasksWithSubtasks().map { relations ->
    relations.map { rel ->
      val subtasks = rel.subtasks.sortedBy { it.orderIndex }.map { sub ->
        SubTask(
          id = sub.id,
          taskId = sub.taskId,
          title = sub.title,
          estimatedMinutes = sub.estimatedMinutes,
          actualMinutes = sub.actualMinutes,
          isCompleted = sub.isCompleted,
          orderIndex = sub.orderIndex,
          actionableNotes = sub.actionableNotes,
          milestoneTitle = sub.milestoneTitle,
          priority = Priority.fromString(sub.priority),
          categoryTag = sub.categoryTag,
          scheduledStartTime = sub.scheduledStartTime,
          scheduledEndTime = sub.scheduledEndTime,
          calendarEventId = sub.calendarEventId
        )
      }

      Task(
        id = rel.task.id,
        title = rel.task.title,
        description = rel.task.description,
        category = TaskCategory.fromString(rel.task.category),
        priority = Priority.fromString(rel.task.priority),
        deadlineTimestamp = rel.task.deadlineTimestamp,
        createdAt = rel.task.createdAt,
        updatedAt = rel.task.updatedAt,
        syncState = when (rel.task.syncStatus) {
          "SYNCED" -> SyncState.SYNCED
          "OFFLINE" -> SyncState.OFFLINE_LOCAL
          else -> SyncState.PENDING_SYNC
        },
        isEncrypted = false,
        subtasks = subtasks
      )
    }
  }

  suspend fun createTaskWithAiBreakdown(
    title: String,
    description: String = "",
    deadLineTimestamp: Long? = null
  ): Pair<Task, BreakdownResult> {
    val taskId = UUID.randomUUID().toString()
    val now = System.currentTimeMillis()

    // Break down with AI into structured milestones and subtasks calibrated to deadline
    val breakdown = geminiService.breakdownTask(
      taskId = taskId,
      taskTitle = title,
      taskDescription = description,
      deadlineTimestamp = deadLineTimestamp
    )

    val finalCategory = breakdown.determinedCategory
    val finalPriority = breakdown.determinedPriority

    val taskEntity = TaskEntity(
      id = taskId,
      title = title,
      description = description,
      category = finalCategory.name,
      priority = finalPriority.name,
      deadlineTimestamp = deadLineTimestamp,
      createdAt = now,
      updatedAt = now,
      syncStatus = "PENDING_SYNC",
      isEncrypted = false,
      encryptedData = null
    )

    val subEntities = breakdown.subtasks.mapIndexed { idx, sub ->
      SubTaskEntity(
        id = sub.id,
        taskId = taskId,
        title = sub.title,
        estimatedMinutes = sub.estimatedMinutes,
        actualMinutes = 0,
        isCompleted = false,
        orderIndex = idx,
        actionableNotes = sub.actionableNotes,
        milestoneTitle = sub.milestoneTitle,
        priority = sub.priority.name,
        categoryTag = sub.categoryTag,
        scheduledStartTime = null,
        scheduledEndTime = null,
        calendarEventId = null
      )
    }

    taskDao.insertTask(taskEntity)
    taskDao.insertSubtasks(subEntities)

    val task = Task(
      id = taskId,
      title = title,
      description = description,
      category = finalCategory,
      priority = finalPriority,
      deadlineTimestamp = deadLineTimestamp,
      createdAt = now,
      updatedAt = now,
      syncState = SyncState.PENDING_SYNC,
      isEncrypted = false,
      subtasks = breakdown.subtasks
    )

    return Pair(task, breakdown)
  }

  suspend fun saveTaskWithPrecomputedBreakdown(
    title: String,
    description: String = "",
    breakdown: BreakdownResult,
    deadLineTimestamp: Long? = null
  ): Task {
    val taskId = UUID.randomUUID().toString()
    val now = System.currentTimeMillis()

    val finalCategory = breakdown.determinedCategory
    val finalPriority = breakdown.determinedPriority

    val taskEntity = TaskEntity(
      id = taskId,
      title = title,
      description = description,
      category = finalCategory.name,
      priority = finalPriority.name,
      deadlineTimestamp = deadLineTimestamp,
      createdAt = now,
      updatedAt = now,
      syncStatus = "PENDING_SYNC",
      isEncrypted = false,
      encryptedData = null
    )

    val remappedSubtasks = breakdown.subtasks.mapIndexed { idx, sub ->
      sub.copy(
        id = UUID.randomUUID().toString(),
        taskId = taskId,
        orderIndex = idx
      )
    }

    val subEntities = remappedSubtasks.map { sub ->
      SubTaskEntity(
        id = sub.id,
        taskId = taskId,
        title = sub.title,
        estimatedMinutes = sub.estimatedMinutes,
        actualMinutes = 0,
        isCompleted = false,
        orderIndex = sub.orderIndex,
        actionableNotes = sub.actionableNotes,
        milestoneTitle = sub.milestoneTitle,
        priority = sub.priority.name,
        categoryTag = sub.categoryTag,
        scheduledStartTime = null,
        scheduledEndTime = null,
        calendarEventId = null
      )
    }

    taskDao.insertTask(taskEntity)
    taskDao.insertSubtasks(subEntities)

    return Task(
      id = taskId,
      title = title,
      description = description,
      category = finalCategory,
      priority = finalPriority,
      deadlineTimestamp = deadLineTimestamp,
      createdAt = now,
      updatedAt = now,
      syncState = SyncState.PENDING_SYNC,
      isEncrypted = false,
      subtasks = remappedSubtasks
    )
  }

  suspend fun toggleSubtaskCompletion(subtask: SubTask, task: Task) {
    val newStatus = !subtask.isCompleted
    taskDao.setSubtaskCompleted(subtask.id, newStatus)
    taskDao.updateTaskSyncStatus(task.id, "PENDING_SYNC", System.currentTimeMillis())

    // Progress Notifications
    val completedCount = task.subtasks.count { if (it.id == subtask.id) newStatus else it.isCompleted }
    val totalCount = task.subtasks.size

    if (newStatus) {
      if (completedCount == totalCount && totalCount > 0) {
        notificationHelper.showTaskAllDoneNotification(task.title, task.totalEstimatedMinutes)
      } else {
        notificationHelper.showSubtaskCompletedNotification(task.title, subtask.title, completedCount, totalCount)
      }
    }
  }

  suspend fun addSubtask(
    taskId: String,
    title: String,
    estimatedMinutes: Int,
    notes: String = "",
    milestoneTitle: String = "Additional Steps",
    priority: Priority = Priority.MEDIUM,
    categoryTag: String = "General"
  ) {
    val subId = UUID.randomUUID().toString()

    val entity = SubTaskEntity(
      id = subId,
      taskId = taskId,
      title = title,
      estimatedMinutes = estimatedMinutes,
      actualMinutes = 0,
      isCompleted = false,
      orderIndex = 99,
      actionableNotes = notes,
      milestoneTitle = milestoneTitle,
      priority = priority.name,
      categoryTag = categoryTag,
      scheduledStartTime = null,
      scheduledEndTime = null,
      calendarEventId = null
    )
    taskDao.insertSubtasks(listOf(entity))
    taskDao.updateTaskSyncStatus(taskId, "PENDING_SYNC", System.currentTimeMillis())
  }

  suspend fun generateAndAppendAiSubtasks(
    task: Task,
    userInstructions: String,
    count: Int
  ): BreakdownResult {
    val result = geminiService.generateAdditionalSubtasks(
      task = task,
      additionalInstructions = userInstructions,
      count = count
    )

    val startOrder = task.subtasks.size
    val entities = result.subtasks.mapIndexed { idx, sub ->
      SubTaskEntity(
        id = sub.id,
        taskId = task.id,
        title = sub.title,
        estimatedMinutes = sub.estimatedMinutes,
        actualMinutes = 0,
        isCompleted = false,
        orderIndex = startOrder + idx,
        actionableNotes = sub.actionableNotes,
        milestoneTitle = sub.milestoneTitle,
        priority = sub.priority.name,
        categoryTag = sub.categoryTag,
        scheduledStartTime = null,
        scheduledEndTime = null,
        calendarEventId = null
      )
    }

    taskDao.insertSubtasks(entities)
    taskDao.updateTaskSyncStatus(task.id, "PENDING_SYNC", System.currentTimeMillis())
    return result
  }

  suspend fun logSubtaskFocusTime(subtaskId: String, taskId: String, minutesSpent: Int) {
    taskDao.addActualMinutesToSubtask(subtaskId, minutesSpent)
    taskDao.updateTaskSyncStatus(taskId, "PENDING_SYNC", System.currentTimeMillis())
  }

  suspend fun deleteSubtask(subtaskId: String, taskId: String) {
    taskDao.deleteSubtaskById(subtaskId)
    taskDao.updateTaskSyncStatus(taskId, "PENDING_SYNC", System.currentTimeMillis())
  }

  suspend fun deleteTask(task: Task) {
    taskDao.deleteTaskById(task.id)
  }

  suspend fun importTasks(tasks: List<Task>) {
    for (task in tasks) {
      val taskEntity = TaskEntity(
        id = task.id,
        title = task.title,
        description = task.description,
        category = task.category.name,
        priority = task.priority.name,
        deadlineTimestamp = task.deadlineTimestamp,
        createdAt = task.createdAt,
        updatedAt = task.updatedAt,
        syncStatus = "SYNCED",
        isEncrypted = false,
        encryptedData = null
      )
      taskDao.insertTask(taskEntity)

      val subEntities = task.subtasks.mapIndexed { idx, sub ->
        SubTaskEntity(
          id = sub.id,
          taskId = task.id,
          title = sub.title,
          estimatedMinutes = sub.estimatedMinutes,
          actualMinutes = sub.actualMinutes,
          isCompleted = sub.isCompleted,
          orderIndex = sub.orderIndex.takeIf { it >= 0 } ?: idx,
          actionableNotes = sub.actionableNotes,
          milestoneTitle = sub.milestoneTitle,
          priority = sub.priority.name,
          categoryTag = sub.categoryTag,
          scheduledStartTime = sub.scheduledStartTime,
          scheduledEndTime = sub.scheduledEndTime,
          calendarEventId = sub.calendarEventId
        )
      }
      taskDao.insertSubtasks(subEntities)
    }
  }

  suspend fun seedSampleTasksIfEmpty() {
    if (taskDao.getTaskCount() == 0) {
      createTaskWithAiBreakdown(
        title = "Launch Mobile App Beta on Google Play",
        description = "Prepare store assets, finalize release build, and invite 20 beta testers.",
        deadLineTimestamp = System.currentTimeMillis() + 86400000L * 3
      )
      createTaskWithAiBreakdown(
        title = "Prepare Q3 Financial Review Presentation",
        description = "Synthesize quarterly revenue metrics and design executive slide deck.",
        deadLineTimestamp = System.currentTimeMillis() + 86400000L * 5
      )
    }
  }
}
