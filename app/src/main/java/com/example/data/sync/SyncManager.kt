package com.example.data.sync

import android.content.Context
import com.example.data.crypto.CryptoManager
import com.example.data.model.Priority
import com.example.data.model.SubTask
import com.example.data.model.SyncState
import com.example.data.model.Task
import com.example.data.model.TaskCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

enum class CloudSyncStatus {
  IDLE,
  SYNCING,
  SYNC_SUCCESS,
  OFFLINE,
  ERROR
}

data class SyncReport(
  val tasksCount: Int,
  val subtasksCount: Int,
  val lastSyncedTime: Long,
  val isEncrypted: Boolean
)

class SyncManager(
  private val context: Context,
  private val cryptoManager: CryptoManager
) {

  private val prefs = context.getSharedPreferences("taskbreak_sync_prefs", Context.MODE_PRIVATE)

  private val _syncStatus = MutableStateFlow(CloudSyncStatus.IDLE)
  val syncStatus: StateFlow<CloudSyncStatus> = _syncStatus.asStateFlow()

  private val _lastSyncTimestamp = MutableStateFlow(prefs.getLong(KEY_LAST_SYNC_TIME, 0L))
  val lastSyncTimestamp: StateFlow<Long> = _lastSyncTimestamp.asStateFlow()

  private val _isOfflineMode = MutableStateFlow(prefs.getBoolean(KEY_OFFLINE_MODE, false))
  val isOfflineMode: StateFlow<Boolean> = _isOfflineMode.asStateFlow()

  private val _syncAccounts = MutableStateFlow<List<com.example.data.model.SyncAccount>>(loadSyncAccounts())
  val syncAccounts: StateFlow<List<com.example.data.model.SyncAccount>> = _syncAccounts.asStateFlow()

  fun setOfflineMode(enabled: Boolean) {
    _isOfflineMode.value = enabled
    prefs.edit().putBoolean(KEY_OFFLINE_MODE, enabled).apply()
  }

  fun addSyncAccount(account: com.example.data.model.SyncAccount) {
    val current = _syncAccounts.value.toMutableList()
    current.add(account)
    saveSyncAccounts(current)
  }

  fun updateSyncAccount(account: com.example.data.model.SyncAccount) {
    val current = _syncAccounts.value.map {
      if (it.id == account.id) account else it
    }
    saveSyncAccounts(current)
  }

  fun deleteSyncAccount(accountId: String) {
    val current = _syncAccounts.value.filterNot { it.id == accountId }
    saveSyncAccounts(current)
  }

  fun toggleAutoSync(accountId: String, enabled: Boolean) {
    val current = _syncAccounts.value.map {
      if (it.id == accountId) it.copy(isAutoSyncEnabled = enabled) else it
    }
    saveSyncAccounts(current)
  }

  private fun loadSyncAccounts(): List<com.example.data.model.SyncAccount> {
    val jsonStr = prefs.getString(KEY_SYNC_ACCOUNTS, null)
    if (jsonStr.isNullOrBlank()) {
      // Return default sample configured accounts
      val initial = listOf(
        com.example.data.model.SyncAccount(
          id = "acc_gcal_default",
          accountName = "Google Calendar Primary",
          type = com.example.data.model.SyncAccountType.GOOGLE_CALENDAR,
          serverUrl = "calendar.google.com",
          usernameOrEmail = "user@gmail.com",
          isAutoSyncEnabled = true,
          syncIntervalMinutes = 15,
          lastSyncTimestamp = System.currentTimeMillis() - 1000 * 60 * 35,
          isConnected = true,
          isPrimary = true
        )
      )
      persistAccountsToPrefs(initial)
      return initial
    }

    val list = mutableListOf<com.example.data.model.SyncAccount>()
    try {
      val array = JSONArray(jsonStr)
      for (i in 0 until array.length()) {
        val obj = array.getJSONObject(i)
        list.add(
          com.example.data.model.SyncAccount(
            id = obj.getString("id"),
            accountName = obj.getString("accountName"),
            type = try {
              com.example.data.model.SyncAccountType.valueOf(obj.getString("type"))
            } catch (e: Exception) {
              com.example.data.model.SyncAccountType.GOOGLE_CALENDAR
            },
            serverUrl = obj.optString("serverUrl", ""),
            usernameOrEmail = obj.optString("usernameOrEmail", ""),
            authTokenOrPassword = obj.optString("authTokenOrPassword", ""),
            isAutoSyncEnabled = obj.optBoolean("isAutoSyncEnabled", true),
            syncIntervalMinutes = obj.optInt("syncIntervalMinutes", 15),
            lastSyncTimestamp = obj.optLong("lastSyncTimestamp", 0L),
            isConnected = obj.optBoolean("isConnected", true),
            isPrimary = obj.optBoolean("isPrimary", false)
          )
        )
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
    return list
  }

  private fun persistAccountsToPrefs(accounts: List<com.example.data.model.SyncAccount>) {
    val array = JSONArray()
    for (acc in accounts) {
      val obj = JSONObject().apply {
        put("id", acc.id)
        put("accountName", acc.accountName)
        put("type", acc.type.name)
        put("serverUrl", acc.serverUrl)
        put("usernameOrEmail", acc.usernameOrEmail)
        put("authTokenOrPassword", acc.authTokenOrPassword)
        put("isAutoSyncEnabled", acc.isAutoSyncEnabled)
        put("syncIntervalMinutes", acc.syncIntervalMinutes)
        put("lastSyncTimestamp", acc.lastSyncTimestamp)
        put("isConnected", acc.isConnected)
        put("isPrimary", acc.isPrimary)
      }
      array.put(obj)
    }
    prefs.edit().putString(KEY_SYNC_ACCOUNTS, array.toString()).apply()
  }

  private fun saveSyncAccounts(accounts: List<com.example.data.model.SyncAccount>) {
    _syncAccounts.value = accounts
    persistAccountsToPrefs(accounts)
  }

  suspend fun syncAccount(accountId: String, tasks: List<Task>): Result<SyncReport> = withContext(Dispatchers.IO) {
    if (_isOfflineMode.value) {
      _syncStatus.value = CloudSyncStatus.OFFLINE
      return@withContext Result.failure(Exception("App is running in Offline Mode."))
    }

    _syncStatus.value = CloudSyncStatus.SYNCING
    try {
      delay(900)
      val now = System.currentTimeMillis()
      val updatedAccounts = _syncAccounts.value.map {
        if (it.id == accountId) it.copy(lastSyncTimestamp = now, isConnected = true) else it
      }
      saveSyncAccounts(updatedAccounts)
      _lastSyncTimestamp.value = now
      _syncStatus.value = CloudSyncStatus.SYNC_SUCCESS

      val totalSubtasks = tasks.sumOf { it.subtasks.size }
      val isEnc = cryptoManager.isVaultUnlocked.value || tasks.any { it.isEncrypted }

      Result.success(
        SyncReport(
          tasksCount = tasks.size,
          subtasksCount = totalSubtasks,
          lastSyncedTime = now,
          isEncrypted = isEnc
        )
      )
    } catch (e: Exception) {
      _syncStatus.value = CloudSyncStatus.ERROR
      Result.failure(e)
    }
  }

  suspend fun performCloudSync(tasks: List<Task>): Result<SyncReport> = withContext(Dispatchers.IO) {
    if (_isOfflineMode.value) {
      _syncStatus.value = CloudSyncStatus.OFFLINE
      return@withContext Result.failure(Exception("App is running in Offline Mode."))
    }

    _syncStatus.value = CloudSyncStatus.SYNCING
    try {
      // Simulate real cloud sync handshake with encrypted transport payload
      delay(1200)

      val now = System.currentTimeMillis()
      prefs.edit().putLong(KEY_LAST_SYNC_TIME, now).apply()
      _lastSyncTimestamp.value = now
      _syncStatus.value = CloudSyncStatus.SYNC_SUCCESS

      val totalSubtasks = tasks.sumOf { it.subtasks.size }
      val isEnc = cryptoManager.isVaultUnlocked.value || tasks.any { it.isEncrypted }

      Result.success(
        SyncReport(
          tasksCount = tasks.size,
          subtasksCount = totalSubtasks,
          lastSyncedTime = now,
          isEncrypted = isEnc
        )
      )
    } catch (e: Exception) {
      _syncStatus.value = CloudSyncStatus.ERROR
      Result.failure(e)
    }
  }

  fun exportSyncPayload(tasks: List<Task>, encryptWithVault: Boolean): String {
    val root = JSONObject()
    root.put("version", 1)
    root.put("exportedAt", System.currentTimeMillis())
    root.put("app", "TaskBreak AI")
    root.put("isEncrypted", encryptWithVault)

    val tasksArray = JSONArray()
    for (task in tasks) {
      val taskObj = JSONObject().apply {
        put("id", task.id)
        if (encryptWithVault) {
          put("title", cryptoManager.encryptText(task.title))
          put("description", cryptoManager.encryptText(task.description))
        } else {
          put("title", task.title)
          put("description", task.description)
        }
        put("category", task.category.name)
        put("priority", task.priority.name)
        put("deadlineTimestamp", task.deadlineTimestamp ?: 0L)
        put("createdAt", task.createdAt)
        put("updatedAt", task.updatedAt)

        val subtasksArray = JSONArray()
        for (sub in task.subtasks) {
          val subObj = JSONObject().apply {
            put("id", sub.id)
            put("taskId", sub.taskId)
            if (encryptWithVault) {
              put("title", cryptoManager.encryptText(sub.title))
              put("actionableNotes", cryptoManager.encryptText(sub.actionableNotes))
            } else {
              put("title", sub.title)
              put("actionableNotes", sub.actionableNotes)
            }
            put("estimatedMinutes", sub.estimatedMinutes)
            put("actualMinutes", sub.actualMinutes)
            put("isCompleted", sub.isCompleted)
            put("orderIndex", sub.orderIndex)
          }
          subtasksArray.put(subObj)
        }
        put("subtasks", subtasksArray)
      }
      tasksArray.put(taskObj)
    }
    root.put("tasks", tasksArray)

    return root.toString(2)
  }

  fun importSyncPayload(jsonString: String, wasEncrypted: Boolean): List<Task> {
    val tasks = mutableListOf<Task>()
    try {
      val root = JSONObject(jsonString)
      val tasksArray = root.optJSONArray("tasks") ?: JSONArray()
      val isEncryptedInPayload = root.optBoolean("isEncrypted", false) || wasEncrypted

      for (i in 0 until tasksArray.length()) {
        val taskObj = tasksArray.getJSONObject(i)
        val rawTitle = taskObj.getString("title")
        val rawDesc = taskObj.optString("description", "")

        val title = if (isEncryptedInPayload) cryptoManager.decryptText(rawTitle) else rawTitle
        val description = if (isEncryptedInPayload) cryptoManager.decryptText(rawDesc) else rawDesc

        val subtasksList = mutableListOf<SubTask>()
        val subtasksArray = taskObj.optJSONArray("subtasks") ?: JSONArray()
        for (j in 0 until subtasksArray.length()) {
          val subObj = subtasksArray.getJSONObject(j)
          val subRawTitle = subObj.getString("title")
          val subRawNotes = subObj.optString("actionableNotes", "")

          val subTitle = if (isEncryptedInPayload) cryptoManager.decryptText(subRawTitle) else subRawTitle
          val subNotes = if (isEncryptedInPayload) cryptoManager.decryptText(subRawNotes) else subRawNotes

          subtasksList.add(
            SubTask(
              id = subObj.getString("id"),
              taskId = subObj.getString("taskId"),
              title = subTitle,
              estimatedMinutes = subObj.optInt("estimatedMinutes", 30),
              actualMinutes = subObj.optInt("actualMinutes", 0),
              isCompleted = subObj.optBoolean("isCompleted", false),
              orderIndex = subObj.optInt("orderIndex", j),
              actionableNotes = subNotes
            )
          )
        }

        tasks.add(
          Task(
            id = taskObj.getString("id"),
            title = title,
            description = description,
            category = TaskCategory.fromString(taskObj.optString("category", "WORK")),
            priority = Priority.fromString(taskObj.optString("priority", "MEDIUM")),
            deadlineTimestamp = taskObj.optLong("deadlineTimestamp", 0L).let { if (it == 0L) null else it },
            createdAt = taskObj.optLong("createdAt", System.currentTimeMillis()),
            updatedAt = taskObj.optLong("updatedAt", System.currentTimeMillis()),
            syncState = SyncState.SYNCED,
            isEncrypted = isEncryptedInPayload,
            subtasks = subtasksList
          )
        )
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
    return tasks
  }

  companion object {
    private const val KEY_LAST_SYNC_TIME = "key_last_sync_time"
    private const val KEY_OFFLINE_MODE = "key_offline_mode"
    private const val KEY_SYNC_ACCOUNTS = "key_sync_accounts"
  }
}
