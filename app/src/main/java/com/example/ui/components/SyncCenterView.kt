package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SyncAccount
import com.example.data.model.SyncAccountType
import com.example.data.model.Task
import com.example.data.sync.CloudSyncStatus
import com.example.data.sync.SyncManager
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.BrandSecondary
import com.example.ui.theme.BrandTertiary
import com.example.ui.theme.HyperCyan
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SyncCenterView(
  syncManager: SyncManager,
  syncStatus: CloudSyncStatus,
  lastSyncTime: Long?,
  isOfflineMode: Boolean,
  syncAccounts: List<SyncAccount>,
  tasks: List<Task>,
  onTriggerSyncAll: () -> Unit,
  onOpenAddIntegrationDialog: () -> Unit,
  onToggleAccountSync: (String, Boolean) -> Unit,
  modifier: Modifier = Modifier
) {
  val formattedSyncTime = remember(lastSyncTime) {
    if (lastSyncTime != null && lastSyncTime > 0L) {
      SimpleDateFormat("MMM d, hh:mm a", Locale.getDefault()).format(Date(lastSyncTime))
    } else {
      "Today, 09:42 AM"
    }
  }

  // Local state for default connected states to match screenshot
  var notionSyncOn by remember { mutableStateOf(true) }
  var gcalSyncOn by remember { mutableStateOf(true) }
  var outlookSyncOn by remember { mutableStateOf(false) }
  var slackSyncOn by remember { mutableStateOf(false) }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .testTag("sync_center_view"),
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // 1. Sync Center Header & Last Synced
    item {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = "Sync Center",
              fontSize = 24.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = (-0.3).sp,
              color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
              )
              Text(
                text = "Last Synced: $formattedSyncTime",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          IconButton(
            onClick = onTriggerSyncAll,
            modifier = Modifier.testTag("sync_center_refresh_btn")
          ) {
            Icon(
              imageVector = Icons.Default.Sync,
              contentDescription = "Sync All",
              tint = BrandSecondary,
              modifier = Modifier.size(22.dp)
            )
          }
        }

        // Add Integration Action Button
        Button(
          onClick = onOpenAddIntegrationDialog,
          modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .shadow(6.dp, RoundedCornerShape(12.dp), ambientColor = Color.Black.copy(alpha = 0.2f))
            .testTag("add_integration_btn"),
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = BrandSecondary,
            contentColor = HyperCyan
          )
        ) {
          Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            tint = HyperCyan,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Add Integration",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = HyperCyan
          )
        }
      }
    }

    // 2. Integration Cards (Notion, Google Calendar, Outlook, Slack)

    // Card 1: Notion (Connected)
    item {
      IntegrationItemCard(
        serviceName = "Notion",
        isConnected = notionSyncOn,
        isSyncOn = notionSyncOn,
        onToggleSync = {
          notionSyncOn = it
          onTriggerSyncAll()
        },
        testTag = "integration_notion_card"
      )
    }

    // Card 2: Google Calendar (Connected with subtle glow)
    item {
      IntegrationItemCard(
        serviceName = "Google Calendar",
        isConnected = gcalSyncOn,
        isSyncOn = gcalSyncOn,
        onToggleSync = {
          gcalSyncOn = it
          onTriggerSyncAll()
        },
        testTag = "integration_gcal_card"
      )
    }

    // Card 3: Outlook (Disconnected)
    item {
      IntegrationItemCard(
        serviceName = "Outlook",
        isConnected = outlookSyncOn,
        isSyncOn = outlookSyncOn,
        onToggleSync = {
          outlookSyncOn = it
        },
        testTag = "integration_outlook_card"
      )
    }

    // Card 4: Slack (Disconnected)
    item {
      IntegrationItemCard(
        serviceName = "Slack",
        isConnected = slackSyncOn,
        isSyncOn = slackSyncOn,
        onToggleSync = {
          slackSyncOn = it
        },
        testTag = "integration_slack_card"
      )
    }

    // Dynamic Connected Accounts from syncManager
    items(syncAccounts, key = { it.id }) { account ->
      IntegrationItemCard(
        serviceName = account.accountName,
        isConnected = account.isConnected,
        isSyncOn = account.isAutoSyncEnabled,
        onToggleSync = { enabled ->
          onToggleAccountSync(account.id, enabled)
        },
        testTag = "integration_custom_${account.id}"
      )
    }
  }
}

@Composable
private fun IntegrationItemCard(
  serviceName: String,
  isConnected: Boolean,
  isSyncOn: Boolean,
  onToggleSync: (Boolean) -> Unit,
  testTag: String,
  modifier: Modifier = Modifier
) {
  Surface(
    modifier = modifier
      .fillMaxWidth()
      .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
      .testTag(testTag),
    color = MaterialTheme.colorScheme.surfaceContainer,
    shape = RoundedCornerShape(16.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        // Logo Container
        ServiceLogoIcon(
          serviceName = serviceName,
          size = 48.dp
        )

        // Title and Status
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = serviceName,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.height(2.dp))
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            if (isConnected) {
              Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = BrandTertiary,
                modifier = Modifier.size(15.dp)
              )
              Text(
                text = "Connected",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = BrandTertiary
              )
            } else {
              Icon(
                imageVector = Icons.Default.Cancel,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(15.dp)
              )
              Text(
                text = "Disconnected",
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }

      HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        thickness = 1.dp
      )

      // Sync Status & Toggle Switch
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Sync Status",
          fontSize = 14.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        TaskLogicToggle(
          checked = isSyncOn,
          onCheckedChange = onToggleSync,
          testTag = "${testTag}_toggle"
        )
      }
    }
  }
}
