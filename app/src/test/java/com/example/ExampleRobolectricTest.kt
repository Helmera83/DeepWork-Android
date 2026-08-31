package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.calendar.CalendarIntegrationManager
import com.example.data.crypto.CryptoManager
import com.example.data.model.Priority
import com.example.data.model.SubTask
import com.example.data.model.Task
import com.example.data.model.TaskCategory
import com.example.data.ai.GeminiBreakdownService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("TaskLogic AI", appName)
  }

  @Test
  fun `test end to end encryption roundtrip`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val crypto = CryptoManager(context)

    val masterPass = "SecretPass123"
    val setOk = crypto.setMasterPassphrase(masterPass)
    assertTrue(setOk)
    assertTrue(crypto.isVaultUnlocked.value)

    val originalText = "Deploy backend database to production"
    val encrypted = crypto.encryptText(originalText)
    val decrypted = crypto.decryptText(encrypted)

    assertEquals(originalText, decrypted)
  }

  @Test
  fun `test calendar ics generation`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val calManager = CalendarIntegrationManager(context)

    val task = Task(
      id = "task-1",
      title = "Launch Product",
      description = "Prepare market strategy",
      category = TaskCategory.WORK,
      priority = Priority.HIGH,
      deadlineTimestamp = System.currentTimeMillis() + 86400000L,
      createdAt = System.currentTimeMillis(),
      updatedAt = System.currentTimeMillis(),
      subtasks = listOf(
        SubTask(
          id = "sub-1",
          taskId = "task-1",
          title = "Write Press Release",
          estimatedMinutes = 30,
          orderIndex = 0
        ),
        SubTask(
          id = "sub-2",
          taskId = "task-1",
          title = "Send Newsletter",
          estimatedMinutes = 20,
          orderIndex = 1
        )
      )
    )

    val ics = calManager.generateIcsContent(task)
    assertTrue(ics.contains("BEGIN:VCALENDAR"))
    assertTrue(ics.contains("SUMMARY:Launch Product"))
    assertTrue(ics.contains("Step 1: Write Press Release"))
    assertTrue(ics.contains("END:VCALENDAR"))
  }

  @Test
  fun `test task breakdown generator creates structured subtasks`() = runBlocking {
    val service = GeminiBreakdownService()
    val breakdown = service.breakdownTask(
      taskId = "test-task-123",
      taskTitle = "Build Authentication Feature",
      taskDescription = "User sign up and login with email and OAuth",
      deadlineTimestamp = System.currentTimeMillis() + 86400000L * 3
    )

    assertTrue(breakdown.subtasks.isNotEmpty())
    assertTrue(breakdown.subtasks.all { it.estimatedMinutes > 0 })
    assertTrue(breakdown.subtasks.all { it.title.isNotBlank() })
  }

  @Test
  fun `test room database persists milestones and subtasks correctly`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val database = com.example.data.local.AppDatabase.getDatabase(context)
    val dao = database.taskDao()

    val taskId = "test-task-persisted"
    val m1Id = "ms_test_1"
    val m2Id = "ms_test_2"

    val taskEntity = com.example.data.local.TaskEntity(
      id = taskId,
      title = "Develop Feature Architecture",
      description = "Full stack planning",
      category = "CODING",
      priority = "HIGH",
      deadlineTimestamp = System.currentTimeMillis() + 86400000L,
      createdAt = System.currentTimeMillis(),
      updatedAt = System.currentTimeMillis(),
      syncStatus = "PENDING_SYNC",
      isEncrypted = false,
      encryptedData = null
    )

    val milestones = listOf(
      com.example.data.local.MilestoneEntity(id = m1Id, taskId = taskId, title = "Milestone 1: Design", orderIndex = 0),
      com.example.data.local.MilestoneEntity(id = m2Id, taskId = taskId, title = "Milestone 2: Execution", orderIndex = 1)
    )

    val subtasks = listOf(
      com.example.data.local.SubTaskEntity(
        id = "sub-1",
        taskId = taskId,
        milestoneId = m1Id,
        title = "Create API Specs",
        estimatedMinutes = 30,
        actualMinutes = 0,
        isCompleted = false,
        orderIndex = 0,
        actionableNotes = "Draft OpenAPI spec",
        milestoneTitle = "Milestone 1: Design"
      ),
      com.example.data.local.SubTaskEntity(
        id = "sub-2",
        taskId = taskId,
        milestoneId = m2Id,
        title = "Implement Database Models",
        estimatedMinutes = 45,
        actualMinutes = 0,
        isCompleted = false,
        orderIndex = 1,
        actionableNotes = "Implement Room Entities",
        milestoneTitle = "Milestone 2: Execution"
      )
    )

    dao.insertTask(taskEntity)
    dao.insertMilestones(milestones)
    dao.insertSubtasks(subtasks)

    val flowResult = dao.getAllTasksWithMilestonesAndSubtasks().first()
    val loadedTask = flowResult.firstOrNull { it.task.id == taskId }

    org.junit.Assert.assertNotNull(loadedTask)
    assertEquals(2, loadedTask!!.milestones.size)
    assertEquals(2, loadedTask.subtasks.size)
    assertEquals("Milestone 1: Design", loadedTask.milestones[0].title)
    assertEquals(m1Id, loadedTask.subtasks.first { it.id == "sub-1" }.milestoneId)
  }

  @Test
  fun `test AI subtask due dates are assigned and formatted`() = runBlocking {
    val service = GeminiBreakdownService()
    val fourDaysAhead = System.currentTimeMillis() + (86400000L * 4)
    val breakdown = service.breakdownTask(
      taskId = "task-with-deadline",
      taskTitle = "Publish Android Release to Play Store",
      taskDescription = "Prepare app bundle, test on emulators, create release notes",
      deadlineTimestamp = fourDaysAhead
    )

    assertTrue(breakdown.subtasks.isNotEmpty())
    assertTrue("All subtasks should receive assigned due dates", breakdown.subtasks.all { it.dueDateTimestamp != null })
    val firstSub = breakdown.subtasks.first()
    org.junit.Assert.assertNotNull(firstSub.formattedDueDate())
  }
}


