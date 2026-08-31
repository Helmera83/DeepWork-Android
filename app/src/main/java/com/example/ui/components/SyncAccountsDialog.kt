package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.SyncAccount
import com.example.data.model.Task
import com.example.data.sync.CloudSyncStatus
import com.example.data.sync.SyncManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncAccountsDialog(
  syncManager: SyncManager,
  syncStatus: CloudSyncStatus,
  lastSyncTime: Long?,
  isOfflineMode: Boolean,
  syncAccounts: List<SyncAccount>,
  tasks: List<Task>,
  onTriggerSyncAll: () -> Unit,
  onSyncAccount: (String) -> Unit,
  onAddAccount: (SyncAccount) -> Unit,
  onUpdateAccount: (SyncAccount) -> Unit,
  onDeleteAccount: (String, String) -> Unit,
  onToggleAutoSync: (String, Boolean) -> Unit,
  onImportBackup: (String, Boolean) -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier
) {
  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(
      usePlatformDefaultWidth = false,
      dismissOnBackPress = true,
      dismissOnClickOutside = false
    )
  ) {
    Surface(
      modifier = modifier
        .fillMaxSize()
        .testTag("sync_accounts_dialog"),
      color = MaterialTheme.colorScheme.background
    ) {
      Scaffold(
        topBar = {
          TopAppBar(
            title = {
              Row(
                verticalAlignment = Alignment.CenterVertically
              ) {
                Box(
                  modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = Icons.Default.CloudSync,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                  )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                  Text(
                    text = "Sync & Accounts",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  Text(
                    text = "Cloud integrations & backups",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
            },
            actions = {
              IconButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("sync_dialog_close_btn")
              ) {
                Icon(
                  imageVector = Icons.Default.Close,
                  contentDescription = "Close",
                  tint = MaterialTheme.colorScheme.onSurface
                )
              }
            },
            colors = TopAppBarDefaults.topAppBarColors(
              containerColor = MaterialTheme.colorScheme.surface
            )
          )
        }
      ) { innerPadding ->
        SyncAccountsView(
          syncManager = syncManager,
          syncStatus = syncStatus,
          lastSyncTime = lastSyncTime ?: 0L,
          isOfflineMode = isOfflineMode,
          syncAccounts = syncAccounts,
          tasks = tasks,
          onTriggerSyncAll = onTriggerSyncAll,
          onSyncAccount = onSyncAccount,
          onAddAccount = onAddAccount,
          onUpdateAccount = onUpdateAccount,
          onDeleteAccount = onDeleteAccount,
          onToggleAutoSync = onToggleAutoSync,
          onImportBackup = onImportBackup,
          modifier = Modifier.padding(innerPadding)
        )
      }
    }
  }
}
