package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Task
import com.example.data.sync.CloudSyncStatus
import com.example.data.sync.SyncManager
import com.example.ui.theme.ExpressiveCoral
import com.example.ui.theme.ExpressiveEmerald
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncBackupDialog(
  syncManager: SyncManager,
  syncStatus: CloudSyncStatus,
  lastSyncTime: Long,
  isOfflineMode: Boolean,
  tasks: List<Task>,
  onTriggerSync: () -> Unit,
  onImportBackup: (json: String, wasEncrypted: Boolean) -> Unit,
  onDismiss: () -> Unit
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val context = LocalContext.current

  var importJsonInput by remember { mutableStateOf("") }
  var importIsEncrypted by remember { mutableStateOf(false) }
  var copiedExportNotice by remember { mutableStateOf<String?>(null) }
  var showImportField by remember { mutableStateOf(false) }

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
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(42.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.CloudSync,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(22.dp)
            )
          }
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text(
              text = "Cross-Platform Sync & Backup",
              fontSize = 17.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "Seamless offline caching and cross-device sync",
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        IconButton(onClick = onDismiss) {
          Icon(Icons.Default.Close, contentDescription = "Close")
        }
      }

      Spacer(modifier = Modifier.height(18.dp))

      // Offline Mode Toggle
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
              imageVector = if (isOfflineMode) Icons.Default.WifiOff else Icons.Default.CloudDone,
              contentDescription = null,
              tint = if (isOfflineMode) ExpressiveCoral else ExpressiveEmerald,
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
                text = if (isOfflineMode) "Queues updates locally until connected" else "Auto-syncs task updates",
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

      Spacer(modifier = Modifier.height(14.dp))

      // Sync Trigger Button
      Button(
        onClick = onTriggerSync,
        enabled = !isOfflineMode && syncStatus != CloudSyncStatus.SYNCING,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
      ) {
        if (syncStatus == CloudSyncStatus.SYNCING) {
          CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Syncing with Cloud...", fontSize = 14.sp, fontWeight = FontWeight.Bold)
        } else {
          Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Sync Now (${tasks.size} Tasks)", fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
      }

      if (lastSyncTime > 0) {
        val sdf = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = "Last synced: ${sdf.format(Date(lastSyncTime))}",
          fontSize = 11.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.align(Alignment.CenterHorizontally)
        )
      }

      Spacer(modifier = Modifier.height(20.dp))
      HorizontalDivider(thickness = 0.8.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
      Spacer(modifier = Modifier.height(16.dp))

      // Cross-Platform JSON Export / Import
      Text(
        text = "Universal Cross-Platform Backup",
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
      )
      Spacer(modifier = Modifier.height(8.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // Export JSON button
        OutlinedButton(
          onClick = {
            val exportJson = syncManager.exportSyncPayload(tasks, encryptWithVault = false)
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("TaskBreak AI Backup", exportJson)
            clipboard.setPrimaryClip(clip)
            copiedExportNotice = "Plaintext Backup (${tasks.size} tasks) copied to clipboard!"
          },
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.weight(1f)
        ) {
          Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Export JSON", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        // Export Encrypted button
        OutlinedButton(
          onClick = {
            val exportJson = syncManager.exportSyncPayload(tasks, encryptWithVault = true)
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("TaskBreak AI Encrypted Backup", exportJson)
            clipboard.setPrimaryClip(clip)
            copiedExportNotice = "E2EE Encrypted Backup copied to clipboard!"
          },
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.weight(1f)
        ) {
          Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.tertiary)
          Spacer(modifier = Modifier.width(4.dp))
          Text("Export E2EE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
        }
      }

      copiedExportNotice?.let { msg ->
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
            Text(text = msg, fontSize = 11.sp, color = ExpressiveEmerald, fontWeight = FontWeight.Medium)
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Import Backup Toggle
      OutlinedButton(
        onClick = { showImportField = !showImportField },
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(if (showImportField) "Hide Import Tool" else "Import Tasks from JSON Backup", fontSize = 13.sp, fontWeight = FontWeight.Bold)
      }

      if (showImportField) {
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
          value = importJsonInput,
          onValueChange = { importJsonInput = it },
          label = { Text("Paste JSON Backup payload") },
          placeholder = { Text("{\n  \"version\": 1,\n  \"tasks\": [...]\n}") },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(12.dp),
          maxLines = 4,
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
          )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth()
        ) {
          Switch(
            checked = importIsEncrypted,
            onCheckedChange = { importIsEncrypted = it }
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text("Payload was encrypted with E2EE", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
          onClick = {
            if (importJsonInput.isNotBlank()) {
              onImportBackup(importJsonInput, importIsEncrypted)
              importJsonInput = ""
              showImportField = false
            }
          },
          enabled = importJsonInput.isNotBlank(),
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
          ),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text("Restore / Merge Tasks", fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}
