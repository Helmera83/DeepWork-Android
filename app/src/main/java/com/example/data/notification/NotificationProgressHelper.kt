package com.example.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

class NotificationProgressHelper(private val context: Context) {

  private val notificationManager =
    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

  init {
    createNotificationChannels()
  }

  private fun createNotificationChannels() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val progressChannel = NotificationChannel(
        CHANNEL_PROGRESS_ID,
        "Progress & Focus Tracking",
        NotificationManager.IMPORTANCE_LOW
      ).apply {
        description = "Live updates on active subtasks, focus timers, and completion milestones."
      }

      val reminderChannel = NotificationChannel(
        CHANNEL_REMINDER_ID,
        "Task Reminders & Deadlines",
        NotificationManager.IMPORTANCE_DEFAULT
      ).apply {
        description = "Alerts for upcoming subtasks and task deadlines."
      }

      notificationManager.createNotificationChannel(progressChannel)
      notificationManager.createNotificationChannel(reminderChannel)
    }
  }

  fun showSubtaskCompletedNotification(
    taskTitle: String,
    subtaskTitle: String,
    completedCount: Int,
    totalCount: Int
  ) {
    if (!hasNotificationPermission()) return

    val progressPercent = if (totalCount > 0) (completedCount * 100) / totalCount else 100
    val notification = NotificationCompat.Builder(context, CHANNEL_PROGRESS_ID)
      .setSmallIcon(android.R.drawable.checkbox_on_background)
      .setContentTitle("Step Completed: $subtaskTitle")
      .setContentText("$taskTitle • $completedCount/$totalCount steps done ($progressPercent%)")
      .setProgress(totalCount, completedCount, false)
      .setPriority(NotificationCompat.PRIORITY_LOW)
      .setAutoCancel(true)
      .build()

    notificationManager.notify(NOTIFICATION_ID_MILESTONE, notification)
  }

  fun showTaskAllDoneNotification(taskTitle: String, totalMinutes: Int) {
    if (!hasNotificationPermission()) return

    val notification = NotificationCompat.Builder(context, CHANNEL_PROGRESS_ID)
      .setSmallIcon(android.R.drawable.star_on)
      .setContentTitle("🎉 Task Complete!")
      .setContentText("You finished all steps for \"$taskTitle\" (~${totalMinutes}m logged)!")
      .setPriority(NotificationCompat.PRIORITY_DEFAULT)
      .setAutoCancel(true)
      .build()

    notificationManager.notify(NOTIFICATION_ID_ALL_DONE, notification)
  }

  fun showFocusTimerNotification(
    subtaskTitle: String,
    remainingSeconds: Int,
    totalSeconds: Int,
    isPaused: Boolean
  ) {
    if (!hasNotificationPermission()) return

    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)
    val stateText = if (isPaused) "Paused" else "In Progress"

    val notification = NotificationCompat.Builder(context, CHANNEL_PROGRESS_ID)
      .setSmallIcon(android.R.drawable.ic_media_play)
      .setContentTitle("Focusing on: $subtaskTitle")
      .setContentText("$timeFormatted remaining • $stateText")
      .setProgress(totalSeconds, totalSeconds - remainingSeconds, false)
      .setOngoing(!isPaused && remainingSeconds > 0)
      .setOnlyAlertOnce(true)
      .build()

    notificationManager.notify(NOTIFICATION_ID_TIMER, notification)
  }

  fun cancelTimerNotification() {
    notificationManager.cancel(NOTIFICATION_ID_TIMER)
  }

  private fun hasNotificationPermission(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.POST_NOTIFICATIONS
      ) == PackageManager.PERMISSION_GRANTED
    } else {
      true
    }
  }

  companion object {
    const val CHANNEL_PROGRESS_ID = "taskbreak_progress_channel"
    const val CHANNEL_REMINDER_ID = "taskbreak_reminder_channel"

    private const val NOTIFICATION_ID_MILESTONE = 1001
    private const val NOTIFICATION_ID_ALL_DONE = 1002
    private const val NOTIFICATION_ID_TIMER = 1003
  }
}
