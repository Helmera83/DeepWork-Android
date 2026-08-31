package com.example.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "tasks")
data class TaskEntity(
  @PrimaryKey val id: String,
  val title: String,
  val description: String,
  val category: String,
  val priority: String,
  val deadlineTimestamp: Long?,
  val createdAt: Long,
  val updatedAt: Long,
  val syncStatus: String,
  val isEncrypted: Boolean,
  val encryptedData: String? = null // AES-GCM base64 ciphertext envelope for E2EE storage
)

@Entity(
  tableName = "milestones",
  foreignKeys = [
    ForeignKey(
      entity = TaskEntity::class,
      parentColumns = ["id"],
      childColumns = ["taskId"],
      onDelete = ForeignKey.CASCADE
    )
  ],
  indices = [Index(value = ["taskId"])]
)
data class MilestoneEntity(
  @PrimaryKey val id: String,
  val taskId: String,
  val title: String,
  val orderIndex: Int
)

@Entity(
  tableName = "subtasks",
  foreignKeys = [
    ForeignKey(
      entity = TaskEntity::class,
      parentColumns = ["id"],
      childColumns = ["taskId"],
      onDelete = ForeignKey.CASCADE
    )
  ],
  indices = [
    Index(value = ["taskId"]),
    Index(value = ["milestoneId"])
  ]
)
data class SubTaskEntity(
  @PrimaryKey val id: String,
  val taskId: String,
  val milestoneId: String? = null,
  val title: String,
  val estimatedMinutes: Int,
  val actualMinutes: Int,
  val isCompleted: Boolean,
  val orderIndex: Int,
  val actionableNotes: String,
  val milestoneTitle: String = "",
  val priority: String = "MEDIUM",
  val categoryTag: String = "",
  val scheduledStartTime: Long? = null,
  val scheduledEndTime: Long? = null,
  val dueDateTimestamp: Long? = null,
  val calendarEventId: String? = null
)

data class TaskWithMilestonesAndSubtasksRelation(
  @Embedded val task: TaskEntity,
  @Relation(
    parentColumn = "id",
    entityColumn = "taskId"
  )
  val milestones: List<MilestoneEntity>,
  @Relation(
    parentColumn = "id",
    entityColumn = "taskId"
  )
  val subtasks: List<SubTaskEntity>
)

data class TaskWithSubtasksRelation(
  @Embedded val task: TaskEntity,
  @Relation(
    parentColumn = "id",
    entityColumn = "taskId"
  )
  val subtasks: List<SubTaskEntity>
)

