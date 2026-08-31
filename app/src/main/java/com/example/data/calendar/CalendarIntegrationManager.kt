package com.example.data.calendar

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import androidx.core.content.FileProvider
import com.example.data.model.Task
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class CalendarIntegrationManager(private val context: Context) {

  fun createCalendarInsertIntent(task: Task, startTimeMillis: Long = System.currentTimeMillis()): Intent {
    val totalMinutes = if (task.totalEstimatedMinutes > 0) task.totalEstimatedMinutes else 60
    val endTimeMillis = startTimeMillis + (totalMinutes * 60 * 1000L)

    val descriptionBuilder = StringBuilder()
    descriptionBuilder.append("📋 Task Breakdown by TaskBreak AI:\n\n")
    if (task.description.isNotBlank()) {
      descriptionBuilder.append("Notes: ${task.description}\n\n")
    }
    task.subtasks.forEachIndexed { index, sub ->
      descriptionBuilder.append("${index + 1}. [ ] ${sub.title} (~${sub.estimatedMinutes}m)\n")
      if (sub.actionableNotes.isNotBlank()) {
        descriptionBuilder.append("   • ${sub.actionableNotes}\n")
      }
    }
    descriptionBuilder.append("\nTotal Estimated Duration: ${task.totalEstimatedMinutes} mins")

    return Intent(Intent.ACTION_INSERT).apply {
      data = CalendarContract.Events.CONTENT_URI
      putExtra(CalendarContract.Events.TITLE, task.title)
      putExtra(CalendarContract.Events.DESCRIPTION, descriptionBuilder.toString())
      putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startTimeMillis)
      putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endTimeMillis)
      putExtra(CalendarContract.Events.EVENT_LOCATION, task.category.label)
      putExtra(CalendarContract.Events.ACCESS_LEVEL, CalendarContract.Events.ACCESS_PRIVATE)
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
  }

  fun generateIcsContent(task: Task, startEpochMillis: Long = System.currentTimeMillis()): String {
    val dateFormat = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US).apply {
      timeZone = TimeZone.getTimeZone("UTC")
    }

    val createdTimestamp = dateFormat.format(Date(task.createdAt))
    var currentSlotStart = startEpochMillis

    val ics = StringBuilder()
    ics.appendLine("BEGIN:VCALENDAR")
    ics.appendLine("VERSION:2.0")
    ics.appendLine("PRODID:-//TaskBreak AI//Android//EN")
    ics.appendLine("CALSCALE:GREGORIAN")
    ics.appendLine("METHOD:PUBLISH")

    // Master Event
    val masterEnd = startEpochMillis + ((task.totalEstimatedMinutes.coerceAtLeast(30)) * 60 * 1000L)
    ics.appendLine("BEGIN:VEVENT")
    ics.appendLine("UID:task-${task.id}@taskbreak.ai")
    ics.appendLine("DTSTAMP:$createdTimestamp")
    ics.appendLine("DTSTART:${dateFormat.format(Date(startEpochMillis))}")
    ics.appendLine("DTEND:${dateFormat.format(Date(masterEnd))}")
    ics.appendLine("SUMMARY:${escapeIcsText(task.title)}")
    ics.appendLine("DESCRIPTION:${escapeIcsText("Category: ${task.category.label} | Est: ${task.totalEstimatedMinutes}m\\n${task.description}")}")
    ics.appendLine("STATUS:CONFIRMED")
    ics.appendLine("CATEGORIES:${escapeIcsText(task.category.label)}")
    ics.appendLine("END:VEVENT")

    // Individual Subtask Timeblocks
    task.subtasks.forEachIndexed { index, sub ->
      val subDurationMillis = (sub.estimatedMinutes.coerceAtLeast(15)) * 60 * 1000L
      val subStart = sub.scheduledStartTime ?: currentSlotStart
      val subEnd = sub.scheduledEndTime ?: sub.dueDateTimestamp ?: (subStart + subDurationMillis)
      ics.appendLine("BEGIN:VEVENT")
      ics.appendLine("UID:subtask-${sub.id}@taskbreak.ai")
      ics.appendLine("DTSTAMP:$createdTimestamp")
      ics.appendLine("DTSTART:${dateFormat.format(Date(subStart))}")
      ics.appendLine("DTEND:${dateFormat.format(Date(subEnd))}")
      ics.appendLine("SUMMARY:${escapeIcsText("Step ${index + 1}: ${sub.title}")}")
      ics.appendLine("DESCRIPTION:${escapeIcsText("Part of ${task.title}\\n${sub.actionableNotes}")}")
      ics.appendLine("STATUS:${if (sub.isCompleted) "COMPLETED" else "CONFIRMED"}")
      ics.appendLine("END:VEVENT")

      // 5 min buffer between subtask blocks
      currentSlotStart = subEnd + (5 * 60 * 1000L)
    }

    ics.appendLine("END:VCALENDAR")
    return ics.toString()
  }

  fun createShareIcsIntent(task: Task, startEpochMillis: Long = System.currentTimeMillis()): Intent {
    val icsText = generateIcsContent(task, startEpochMillis)
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
      type = "text/calendar"
      putExtra(Intent.EXTRA_SUBJECT, "Calendar Schedule: ${task.title}")
      putExtra(Intent.EXTRA_TEXT, icsText)
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    return Intent.createChooser(sendIntent, "Export schedule to Calendar / ICS").apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
  }

  private fun escapeIcsText(text: String): String {
    return text.replace("\\", "\\\\")
      .replace(";", "\\;")
      .replace(",", "\\,")
      .replace("\n", "\\n")
  }
}
