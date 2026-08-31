package com.example.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class Priority(val label: String, val level: Int) {
  LOW("Low", 1),
  MEDIUM("Medium", 2),
  HIGH("High", 3),
  URGENT("Urgent", 4);

  companion object {
    fun fromString(value: String): Priority {
      return entries.firstOrNull { it.name.equals(value, ignoreCase = true) || it.label.equals(value, ignoreCase = true) }
        ?: MEDIUM
    }
  }
}

enum class TaskCategory(val label: String, val iconName: String) {
  WORK("Work", "work"),
  CODING("Coding", "code"),
  STUDY("Study", "school"),
  LIFE("Personal", "person"),
  HEALTH("Health", "fitness_center"),
  PROJECT("Project", "rocket_launch");

  companion object {
    fun fromString(value: String): TaskCategory {
      return entries.firstOrNull { it.name.equals(value, ignoreCase = true) || it.label.equals(value, ignoreCase = true) }
        ?: WORK
    }
  }
}

enum class SyncState {
  SYNCED,
  PENDING_SYNC,
  OFFLINE_LOCAL
}

enum class SyncAccountType(val label: String, val defaultServer: String) {
  GOOGLE_CALENDAR("Google Calendar", "calendar.google.com"),
  CALDAV("CalDAV / WebDAV", "https://caldav.example.com"),
  NEXTCLOUD("Nextcloud / ownCloud", "https://cloud.example.com/remote.php/dav"),
  CUSTOM_SERVER("Custom REST API", "https://api.example.com/sync")
}

data class SyncAccount(
  val id: String,
  val accountName: String,
  val type: SyncAccountType,
  val serverUrl: String,
  val usernameOrEmail: String,
  val authTokenOrPassword: String = "",
  val isAutoSyncEnabled: Boolean = true,
  val syncIntervalMinutes: Int = 15,
  val lastSyncTimestamp: Long = 0L,
  val isConnected: Boolean = true,
  val isPrimary: Boolean = false
)

data class SubTask(
  val id: String,
  val taskId: String,
  val title: String,
  val estimatedMinutes: Int,
  val actualMinutes: Int = 0,
  val isCompleted: Boolean = false,
  val orderIndex: Int = 0,
  val actionableNotes: String = "",
  val milestoneTitle: String = "",
  val priority: Priority = Priority.MEDIUM,
  val categoryTag: String = "",
  val scheduledStartTime: Long? = null,
  val scheduledEndTime: Long? = null,
  val calendarEventId: String? = null
)

data class Milestone(
  val id: String,
  val title: String,
  val orderIndex: Int,
  val subtasks: List<SubTask> = emptyList()
) {
  val completedSubtasksCount: Int
    get() = subtasks.count { it.isCompleted }

  val totalSubtasksCount: Int
    get() = subtasks.size

  val totalEstimatedMinutes: Int
    get() = subtasks.sumOf { it.estimatedMinutes }

  val totalActualMinutes: Int
    get() = subtasks.sumOf { it.actualMinutes }

  val completionProgress: Float
    get() = if (subtasks.isEmpty()) 0f else completedSubtasksCount.toFloat() / totalSubtasksCount.toFloat()

  val isCompleted: Boolean
    get() = subtasks.isNotEmpty() && subtasks.all { it.isCompleted }
}

data class Task(
  val id: String,
  val title: String,
  val description: String = "",
  val category: TaskCategory = TaskCategory.WORK,
  val priority: Priority = Priority.MEDIUM,
  val deadlineTimestamp: Long? = null,
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis(),
  val syncState: SyncState = SyncState.PENDING_SYNC,
  val isEncrypted: Boolean = false,
  val subtasks: List<SubTask> = emptyList()
) {
  val totalEstimatedMinutes: Int
    get() = subtasks.sumOf { it.estimatedMinutes }

  val totalActualMinutes: Int
    get() = subtasks.sumOf { it.actualMinutes }

  val completedSubtasksCount: Int
    get() = subtasks.count { it.isCompleted }

  val totalSubtasksCount: Int
    get() = subtasks.size

  val completionProgress: Float
    get() = if (subtasks.isEmpty()) 0f else completedSubtasksCount.toFloat() / totalSubtasksCount.toFloat()

  val isFullyCompleted: Boolean
    get() = subtasks.isNotEmpty() && subtasks.all { it.isCompleted }

  val milestones: List<Milestone>
    get() {
      if (subtasks.isEmpty()) return emptyList()
      val hasExplicitMilestones = subtasks.any { it.milestoneTitle.isNotBlank() }
      if (hasExplicitMilestones) {
        // Group while preserving order of first appearance
        val groups = LinkedHashMap<String, MutableList<SubTask>>()
        for (sub in subtasks) {
          val key = if (sub.milestoneTitle.isNotBlank()) sub.milestoneTitle else "Milestone 1: General Tasks"
          groups.getOrPut(key) { mutableListOf() }.add(sub)
        }
        return groups.entries.mapIndexed { index, entry ->
          Milestone(
            id = "ms_${id}_$index",
            title = entry.key,
            orderIndex = index,
            subtasks = entry.value
          )
        }
      } else {
        // Chunk into 2-3 structured milestones if no explicit milestone title
        val chunkSize = if (subtasks.size <= 3) subtasks.size else ((subtasks.size + 1) / 2).coerceAtLeast(2)
        val chunks = subtasks.chunked(chunkSize)
        return chunks.mapIndexed { index, chunkList ->
          val phaseName = when (index) {
            0 -> "Milestone 1: Planning & Scope"
            1 -> "Milestone 2: Core Execution"
            2 -> "Milestone 3: Verification & Review"
            else -> "Milestone ${index + 1}: Follow-up Actions"
          }
          Milestone(
            id = "ms_${id}_$index",
            title = phaseName,
            orderIndex = index,
            subtasks = chunkList
          )
        }
      }
    }

  fun formattedDeadline(): String? {
    if (deadlineTimestamp == null || deadlineTimestamp == 0L) return null
    val sdf = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
    return sdf.format(Date(deadlineTimestamp))
  }
}
