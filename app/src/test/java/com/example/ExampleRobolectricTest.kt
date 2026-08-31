package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.calendar.CalendarIntegrationManager
import com.example.data.crypto.CryptoManager
import com.example.data.model.Priority
import com.example.data.model.SubTask
import com.example.data.model.Task
import com.example.data.model.TaskCategory
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
    assertEquals("TaskBreak AI", appName)
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
}

