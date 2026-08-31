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
  fun getAllTasksWithSubtasks(): Flow<List<TaskWithSubtasksRelation>>

  @Transaction
  @Query("SELECT * FROM tasks WHERE id = :taskId LIMIT 1")
  fun getTaskWithSubtasksById(taskId: String): Flow<TaskWithSubtasksRelation?>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTask(task: TaskEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertSubtasks(subtasks: List<SubTaskEntity>)

  @Update
  suspend fun updateTask(task: TaskEntity)

  @Update
  suspend fun updateSubtask(subtask: SubTaskEntity)

  @Delete
  suspend fun deleteTask(task: TaskEntity)

  @Query("DELETE FROM tasks WHERE id = :taskId")
  suspend fun deleteTaskById(taskId: String)

  @Query("DELETE FROM subtasks WHERE id = :subtaskId")
  suspend fun deleteSubtaskById(subtaskId: String)

  @Query("DELETE FROM subtasks WHERE taskId = :taskId")
  suspend fun deleteAllSubtasksForTask(taskId: String)

  @Query("UPDATE subtasks SET isCompleted = :isCompleted WHERE id = :subtaskId")
  suspend fun setSubtaskCompleted(subtaskId: String, isCompleted: Boolean)

  @Query("UPDATE subtasks SET actualMinutes = actualMinutes + :addedMinutes WHERE id = :subtaskId")
  suspend fun addActualMinutesToSubtask(subtaskId: String, addedMinutes: Int)

  @Query("UPDATE tasks SET syncStatus = :syncStatus, updatedAt = :updatedAt WHERE id = :taskId")
  suspend fun updateTaskSyncStatus(taskId: String, syncStatus: String, updatedAt: Long)

  @Query("SELECT COUNT(*) FROM tasks")
  suspend fun getTaskCount(): Int
}
