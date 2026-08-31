package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

  @Transaction
  @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
  fun getAllTasksWithMilestonesAndSubtasks(): Flow<List<TaskWithMilestonesAndSubtasksRelation>>

  @Transaction
  @Query("SELECT * FROM tasks WHERE id = :taskId LIMIT 1")
  fun getTaskWithMilestonesAndSubtasksById(taskId: String): Flow<TaskWithMilestonesAndSubtasksRelation?>

  @Transaction
  @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
  fun getAllTasksWithSubtasks(): Flow<List<TaskWithSubtasksRelation>>

  @Transaction
  @Query("SELECT * FROM tasks WHERE id = :taskId LIMIT 1")
  fun getTaskWithSubtasksById(taskId: String): Flow<TaskWithSubtasksRelation?>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTask(task: TaskEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertMilestones(milestones: List<MilestoneEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertMilestone(milestone: MilestoneEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertSubtasks(subtasks: List<SubTaskEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertSubtask(subtask: SubTaskEntity)

  @Update
  suspend fun updateTask(task: TaskEntity)

  @Update
  suspend fun updateMilestone(milestone: MilestoneEntity)

  @Update
  suspend fun updateSubtask(subtask: SubTaskEntity)

  @Delete
  suspend fun deleteTask(task: TaskEntity)

  @Query("DELETE FROM tasks WHERE id = :taskId")
  suspend fun deleteTaskById(taskId: String)

  @Query("DELETE FROM milestones WHERE id = :milestoneId")
  suspend fun deleteMilestoneById(milestoneId: String)

  @Query("DELETE FROM milestones WHERE taskId = :taskId")
  suspend fun deleteAllMilestonesForTask(taskId: String)

  @Query("SELECT * FROM milestones WHERE taskId = :taskId ORDER BY orderIndex ASC")
  fun getMilestonesForTask(taskId: String): Flow<List<MilestoneEntity>>

  @Query("DELETE FROM subtasks WHERE id = :subtaskId")
  suspend fun deleteSubtaskById(subtaskId: String)

  @Query("DELETE FROM subtasks WHERE taskId = :taskId")
  suspend fun deleteAllSubtasksForTask(taskId: String)

  @Query("UPDATE subtasks SET isCompleted = :isCompleted WHERE id = :subtaskId")
  suspend fun setSubtaskCompleted(subtaskId: String, isCompleted: Boolean)

  @Query("UPDATE subtasks SET actualMinutes = actualMinutes + :addedMinutes WHERE id = :subtaskId")
  suspend fun addActualMinutesToSubtask(subtaskId: String, addedMinutes: Int)

  @Query("UPDATE subtasks SET milestoneId = :milestoneId, milestoneTitle = :milestoneTitle WHERE id = :subtaskId")
  suspend fun updateSubtaskMilestone(subtaskId: String, milestoneId: String?, milestoneTitle: String)

  @Query("UPDATE tasks SET syncStatus = :syncStatus, updatedAt = :updatedAt WHERE id = :taskId")
  suspend fun updateTaskSyncStatus(taskId: String, syncStatus: String, updatedAt: Long)

  @Query("SELECT COUNT(*) FROM tasks")
  suspend fun getTaskCount(): Int
}

