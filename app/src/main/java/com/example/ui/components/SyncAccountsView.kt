package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SyncAccount
import com.example.data.model.SyncAccountType
import com.example.data.model.Task
import com.example.data.sync.CloudSyncStatus
import com.example.data.sync.SyncManager
import com.example.ui.theme.BaselinePrimary
import com.example.ui.theme.ExpressiveAmber
import com.example.ui.theme.ExpressiveCoral
import com.example.ui.theme.ExpressiveEmerald
import com.example.ui.theme.ToneSlateSecondary
import com.example.ui.theme.ToneTealTertiary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncAccountsView(
  syncManager: SyncManager,
  syncStatus: CloudSyncStatus,
  lastSyncTime: Long,
  isOfflineMode: Boolean,
  syncAccounts: List<SyncAccount>,
  tasks: List<Task>,
  onTriggerSyncAll: () -> Unit,
  onSyncAccount: (accountId: String) -> Unit,
  onAddAccount: (SyncAccount) -> Unit,
  onUpdateAccount: (SyncAccount) -> Unit,
  onDeleteAccount: (accountId: String, accountName: String) -> Unit,
  onToggleAutoSync: (accountId: String, enabled: Boolean) -> Unit,
  onImportBackup: (json: String, wasEncrypted: Boolean) -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var showAddAccountDialog by remember { mutableStateOf(false) }
  var accountToEdit by remember { mutableStateOf<SyncAccount?>(null) }
  var accountToDelete by remember { mutableStateOf<SyncAccount?>(null) }

  // Manual Backup Import/Export states
  var showBackupSection by remember { mutableStateOf(false) }
  var importJsonInput by remember { mutableStateOf("") }
  var importIsEncrypted by remember { mutableStateOf(false) }
  var copiedExportNotice by remember { mutableStateOf<String?>(null) }

  val dateFormat = remember { SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()) }

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // 1. Master Sync Status & Health Overview Card
    item {
      Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(20.dp)) {
          // Top Status Row
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.weight(1f)
            ) {
              Box(
                modifier = Modifier
                  .size(46.dp)
                  .clip(RoundedCornerShape(14.dp))
                  .background(
                    if (isOfflineMode) MaterialTheme.colorScheme.surfaceVariant
                    else MaterialTheme.colorScheme.primaryContainer
                  ),
                contentAlignment = Alignment.Center
              ) {
                if (syncStatus == CloudSyncStatus.SYNCING) {
                  CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.5.dp
                  )
                } else {
                  Icon(
                    imageVector = when {
                      isOfflineMode -> Icons.Default.WifiOff
                      syncStatus == CloudSyncStatus.SYNC_SUCCESS -> Icons.Default.CloudDone
                      syncStatus == CloudSyncStatus.ERROR -> Icons.Default.CloudOff
                      else -> Icons.Default.CloudSync
                    },
                    contentDescription = null,
                    tint = when {
                      isOfflineMode -> MaterialTheme.colorScheme.onSurfaceVariant
                      syncStatus == CloudSyncStatus.SYNC_SUCCESS -> ExpressiveEmerald
                      syncStatus == CloudSyncStatus.ERROR -> ExpressiveCoral
                      else -> MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier.size(26.dp)
                  )
                }
              }

              Spacer(modifier = Modifier.width(14.dp))

              Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(
                    text = "Cloud Sync & Accounts",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = when {
                    isOfflineMode -> "Offline Mode Active • Local Only"
                    syncStatus == CloudSyncStatus.SYNCING -> "Syncing data in progress..."
                    lastSyncTime > 0 -> "Last synced ${dateFormat.format(Date(lastSyncTime))}"
                    else -> "Ready to synchronize"
                  },
                  fontSize = 12.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            Button(
              onClick = onTriggerSyncAll,
              enabled = !isOfflineMode && syncStatus != CloudSyncStatus.SYNCING,
              shape = RoundedCornerShape(14.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
              ),
              contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
            ) {
              Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Sync All", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          // Key Metrics Summary Chips
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            SyncMetricTile(
              icon = Icons.Default.Storage,
              label = "Local Tasks",
              value = "${tasks.size}",
              modifier = Modifier.weight(1f)
            )
            SyncMetricTile(
              icon = Icons.Default.CloudDone,
              label = "Connected",
              value = "${syncAccounts.count { it.isConnected }} of ${syncAccounts.size}",
              modifier = Modifier.weight(1f)
            )
            SyncMetricTile(
              icon = Icons.Default.Schedule,
              label = "Status",
              value = if (isOfflineMode) "Offline" else if (syncStatus == CloudSyncStatus.SYNCING) "Syncing" else "Active",
              badgeColor = if (isOfflineMode) MaterialTheme.colorScheme.onSurfaceVariant else ExpressiveEmerald,
              modifier = Modifier.weight(1f)
            )
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Offline Mode Toggle Bar
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
              ) {
                Icon(
                  imageVector = if (isOfflineMode) Icons.Default.WifiOff else Icons.Default.Cloud,
                  contentDescription = null,
                  tint = if (isOfflineMode) ExpressiveCoral else MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                  Text(
                    text = "Offline Mode",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  Text(
                    text = "Keep changes purely on-device without remote calls",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
              Switch(
                checked = isOfflineMode,
                onCheckedChange = { syncManager.setOfflineMode(it) }
              )
            }
          }
        }
      }
    }

    // 2. Section Header: Configured Sync Accounts
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "Configured Providers (${syncAccounts.size})",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "External calendars, CalDAV, and cloud endpoints",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        Button(
          onClick = { showAddAccountDialog = true },
          shape = RoundedCornerShape(12.dp),
          contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
        ) {
          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Add Account", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
      }
    }

    // 3. Accounts List
    if (syncAccounts.isEmpty()) {
      item {
        Surface(
          shape = RoundedCornerShape(18.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Box(
              modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.CloudSync,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
              )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
              text = "No Sync Accounts Configured",
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Connect your Google Calendar, Nextcloud, CalDAV, or custom task server for real-time synchronization.",
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center,
              modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
              onClick = { showAddAccountDialog = true },
              shape = RoundedCornerShape(12.dp)
            ) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Connect Provider", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
          }
        }
      }
    } else {
      items(syncAccounts, key = { it.id }) { account ->
        SyncAccountCard(
          account = account,
          onSync = { onSyncAccount(account.id) },
          onEdit = { accountToEdit = account },
          onDelete = { accountToDelete = account },
          onToggleAutoSync = { onToggleAutoSync(account.id, it) }
        )
      }
    }

    // 4. Data Backup & Universal Export/Import Card
    item {
      Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(18.dp)) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .clickable { showBackupSection = !showBackupSection }
              .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.weight(1f)
            ) {
              Box(
                modifier = Modifier
                  .size(38.dp)
                  .clip(RoundedCornerShape(10.dp))
                  .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.FileUpload,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(20.dp)
                )
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = "JSON Data Backup & Restore",
                  fontSize = 15.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = "Export offline snapshot or restore tasks from JSON",
                  fontSize = 11.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            Icon(
              imageVector = if (showBackupSection) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
              contentDescription = if (showBackupSection) "Collapse" else "Expand",
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          AnimatedVisibility(
            visible = showBackupSection,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
          ) {
            Column(modifier = Modifier.padding(top = 16.dp)) {
              HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
              )
              Spacer(modifier = Modifier.height(16.dp))

              // Export Section
              Text(
                text = "Export Tasks & Schedules",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = "Generates a full JSON document containing all ${tasks.size} tasks, subtasks, priorities, and deadlines.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Spacer(modifier = Modifier.height(10.dp))

              OutlinedButton(
                onClick = {
                  val payload = syncManager.exportSyncPayload(tasks, false)
                  val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                  clipboard.setPrimaryClip(ClipData.newPlainText("TaskBreak AI Backup", payload))
                  copiedExportNotice = "Copied JSON payload (${tasks.size} tasks) to clipboard!"
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
              ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Copy Full JSON Backup to Clipboard", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
              }

              copiedExportNotice?.let { notice ->
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = ExpressiveEmerald.copy(alpha = 0.12f),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ExpressiveEmerald, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = notice, fontSize = 11.sp, color = ExpressiveEmerald, fontWeight = FontWeight.Medium)
                  }
                }
              }

              Spacer(modifier = Modifier.height(18.dp))
              HorizontalDivider(thickness = 0.8.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
              Spacer(modifier = Modifier.height(16.dp))

              // Import Section
              Text(
                text = "Restore & Import Tasks",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = "Paste a TaskBreak AI JSON snapshot below to restore your tasks and milestones:",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Spacer(modifier = Modifier.height(8.dp))

              OutlinedTextField(
                value = importJsonInput,
                onValueChange = { importJsonInput = it },
                placeholder = { Text("Paste JSON payload here...", fontSize = 12.sp) },
                modifier = Modifier
                  .fillMaxWidth()
                  .height(115.dp),
                shape = RoundedCornerShape(12.dp),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = MaterialTheme.colorScheme.primary,
                  unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                )
              )

              Spacer(modifier = Modifier.height(10.dp))

              Button(
                onClick = {
                  if (importJsonInput.isNotBlank()) {
                    onImportBackup(importJsonInput, importIsEncrypted)
                    importJsonInput = ""
                    copiedExportNotice = "Successfully restored tasks from JSON payload!"
                  }
                },
                enabled = importJsonInput.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                  containerColor = MaterialTheme.colorScheme.primary,
                  contentColor = MaterialTheme.colorScheme.onPrimary
                )
              ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Restore & Import Tasks", fontSize = 13.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
    }
  }

  // --- Add Account Dialog ---
  if (showAddAccountDialog) {
    SyncAccountEditDialog(
      account = null,
      onDismiss = { showAddAccountDialog = false },
      onSave = { newAccount ->
        onAddAccount(newAccount)
        showAddAccountDialog = false
      }
    )
  }

  // --- Edit Account Dialog ---
  accountToEdit?.let { acc ->
    SyncAccountEditDialog(
      account = acc,
      onDismiss = { accountToEdit = null },
      onSave = { updatedAccount ->
        onUpdateAccount(updatedAccount)
        accountToEdit = null
      }
    )
  }

  // --- Delete Account Confirmation Dialog ---
  accountToDelete?.let { acc ->
    AlertDialog(
      onDismissRequest = { accountToDelete = null },
      shape = RoundedCornerShape(20.dp),
      title = { Text("Remove Sync Account?", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
      text = {
        Text(
          "Are you sure you want to remove \"${acc.accountName}\"? Scheduled events in local tasks will remain intact.",
          fontSize = 13.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      },
      confirmButton = {
        Button(
          onClick = {
            onDeleteAccount(acc.id, acc.accountName)
            accountToDelete = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = ExpressiveCoral),
          shape = RoundedCornerShape(10.dp)
        ) {
          Text("Remove", color = Color.White, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { accountToDelete = null }) {
          Text("Cancel", fontWeight = FontWeight.Medium)
        }
      }
    )
  }
}

@Composable
private fun SyncMetricTile(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  value: String,
  badgeColor: Color? = null,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
    modifier = modifier
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
      horizontalAlignment = Alignment.Start
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = badgeColor ?: MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(14.dp)
        )
        Text(
          text = label,
          fontSize = 10.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = value,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = badgeColor ?: MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

@Composable
private fun SyncAccountCard(
  account: SyncAccount,
  onSync: () -> Unit,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
  onToggleAutoSync: (Boolean) -> Unit
) {
  val dateFormat = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }

  Surface(
    shape = RoundedCornerShape(18.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
    shadowElevation = 1.dp,
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          Box(
            modifier = Modifier
              .size(42.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = when (account.type) {
                SyncAccountType.GOOGLE_CALENDAR -> Icons.Default.CalendarMonth
                SyncAccountType.CALDAV -> Icons.Default.CloudDone
                SyncAccountType.NEXTCLOUD -> Icons.Default.Cloud
                SyncAccountType.CUSTOM_SERVER -> Icons.Default.Dns
              },
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(22.dp)
            )
          }

          Spacer(modifier = Modifier.width(12.dp))

          Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = account.accountName,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              if (account.isPrimary) {
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = MaterialTheme.colorScheme.primaryContainer
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Icon(
                      imageVector = Icons.Default.Star,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                      text = "PRIMARY",
                      fontSize = 9.sp,
                      fontWeight = FontWeight.ExtraBold,
                      color = MaterialTheme.colorScheme.primary
                    )
                  }
                }
              }
            }

            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = if (account.usernameOrEmail.isNotBlank()) account.usernameOrEmail else account.serverUrl,
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }

        // Action Buttons Row
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
          IconButton(onClick = onSync, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.Sync, contentDescription = "Sync", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
          }
          IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
          }
          IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = ExpressiveCoral, modifier = Modifier.size(20.dp))
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))
      HorizontalDivider(thickness = 0.8.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
      Spacer(modifier = Modifier.height(10.dp))

      // Bottom Metadata & Auto-sync toggle
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
          ) {
            Text(
              text = account.type.label,
              fontSize = 10.sp,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
            )
          }

          Text(
            text = if (account.lastSyncTimestamp > 0) "Synced ${dateFormat.format(Date(account.lastSyncTimestamp))}" else "Never synced",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = "Auto-sync",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.width(6.dp))
          Switch(
            checked = account.isAutoSyncEnabled,
            onCheckedChange = onToggleAutoSync
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SyncAccountEditDialog(
  account: SyncAccount?,
  onDismiss: () -> Unit,
  onSave: (SyncAccount) -> Unit
) {
  val isEditing = account != null

  var accountName by remember { mutableStateOf(account?.accountName ?: "") }
  var selectedType by remember { mutableStateOf(account?.type ?: SyncAccountType.GOOGLE_CALENDAR) }
  var serverUrl by remember { mutableStateOf(account?.serverUrl ?: selectedType.defaultServer) }
  var usernameOrEmail by remember { mutableStateOf(account?.usernameOrEmail ?: "") }
  var authTokenOrPassword by remember { mutableStateOf(account?.authTokenOrPassword ?: "") }
  var isAutoSyncEnabled by remember { mutableStateOf(account?.isAutoSyncEnabled ?: true) }
  var isPrimary by remember { mutableStateOf(account?.isPrimary ?: false) }

  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp)
        .padding(bottom = 36.dp)
        .imePadding()
        .verticalScroll(rememberScrollState())
    ) {
      // Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = if (isEditing) "Edit Sync Account" else "Add Sync Account",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "Configure external provider connection and credentials",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        IconButton(onClick = onDismiss) {
          Icon(Icons.Default.Close, contentDescription = "Close")
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Provider Type Selector
      Text(
        text = "Account Type / Provider",
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface
      )
      Spacer(modifier = Modifier.height(8.dp))
      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        SyncAccountType.entries.forEach { type ->
          val selected = selectedType == type
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            border = if (selected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
            modifier = Modifier
              .weight(1f)
              .clickable {
                selectedType = type
                if (!isEditing || serverUrl.isBlank()) {
                  serverUrl = type.defaultServer
                }
              }
          ) {
            Column(
              modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Icon(
                imageVector = when (type) {
                  SyncAccountType.GOOGLE_CALENDAR -> Icons.Default.CalendarMonth
                  SyncAccountType.CALDAV -> Icons.Default.CloudDone
                  SyncAccountType.NEXTCLOUD -> Icons.Default.Cloud
                  SyncAccountType.CUSTOM_SERVER -> Icons.Default.Dns
                },
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = type.label,
                fontSize = 10.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Account Name Field
      OutlinedTextField(
        value = accountName,
        onValueChange = { accountName = it },
        label = { Text("Account Label") },
        placeholder = { Text("e.g. Work Google Calendar or Personal Nextcloud") },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = MaterialTheme.colorScheme.primary,
          unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        )
      )

      Spacer(modifier = Modifier.height(12.dp))

      // Server URL / Endpoint Field
      OutlinedTextField(
        value = serverUrl,
        onValueChange = { serverUrl = it },
        label = { Text("Server URL / Endpoint") },
        placeholder = { Text("e.g. calendar.google.com or https://caldav.server.com") },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = MaterialTheme.colorScheme.primary,
          unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        )
      )

      Spacer(modifier = Modifier.height(12.dp))

      // Email / Username Field
      OutlinedTextField(
        value = usernameOrEmail,
        onValueChange = { usernameOrEmail = it },
        label = { Text("Email or Username") },
        placeholder = { Text("user@example.com") },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = MaterialTheme.colorScheme.primary,
          unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        )
      )

      Spacer(modifier = Modifier.height(12.dp))

      // Auth Token / Password Field
      OutlinedTextField(
        value = authTokenOrPassword,
        onValueChange = { authTokenOrPassword = it },
        label = { Text("Access Token / Password (Optional)") },
        placeholder = { Text("OAuth Bearer Token or App-Specific Password") },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = MaterialTheme.colorScheme.primary,
          unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        )
      )

      Spacer(modifier = Modifier.height(16.dp))

      // Switches: Auto-sync and Primary
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text("Automatic Background Sync", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
              Text("Synchronize changes periodically", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = isAutoSyncEnabled, onCheckedChange = { isAutoSyncEnabled = it })
          }

          HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text("Set as Primary Account", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
              Text("Preferred provider for automatic exports", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = isPrimary, onCheckedChange = { isPrimary = it })
          }
        }
      }

      Spacer(modifier = Modifier.height(22.dp))

      // Save Button
      Button(
        onClick = {
          val finalName = if (accountName.isNotBlank()) accountName else "${selectedType.label} Account"
          val newOrUpdated = SyncAccount(
            id = account?.id ?: "acc_${UUID.randomUUID()}",
            accountName = finalName,
            type = selectedType,
            serverUrl = serverUrl,
            usernameOrEmail = usernameOrEmail,
            authTokenOrPassword = authTokenOrPassword,
            isAutoSyncEnabled = isAutoSyncEnabled,
            syncIntervalMinutes = account?.syncIntervalMinutes ?: 15,
            lastSyncTimestamp = account?.lastSyncTimestamp ?: 0L,
            isConnected = true,
            isPrimary = isPrimary
          )
          onSave(newOrUpdated)
        },
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary
        )
      ) {
        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = if (isEditing) "Save Changes" else "Add Sync Account",
          fontSize = 15.sp,
          fontWeight = FontWeight.Bold
        )
      }
    }
  }
}
