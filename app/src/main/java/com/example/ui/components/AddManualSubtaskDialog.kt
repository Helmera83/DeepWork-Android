package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Priority
import com.example.ui.theme.IndigoPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddManualSubtaskDialog(
  taskTitle: String,
  onAddSubtask: (title: String, estimatedMinutes: Int, notes: String, priority: Priority, categoryTag: String) -> Unit,
  onDismiss: () -> Unit
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var title by remember { mutableStateOf("") }
  var notes by remember { mutableStateOf("") }
  var estimatedMinutes by remember { mutableFloatStateOf(25f) }
  var selectedPriority by remember { mutableStateOf(Priority.MEDIUM) }
  var categoryTag by remember { mutableStateOf("Development") }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp)
        .padding(bottom = 32.dp)
        .verticalScroll(rememberScrollState())
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "Add Step to Breakdown",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "For: $taskTitle",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        IconButton(onClick = onDismiss) {
          Icon(Icons.Default.Close, contentDescription = "Close")
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      OutlinedTextField(
        value = title,
        onValueChange = { title = it },
        label = { Text("Sub-task action / title") },
        placeholder = { Text("e.g. Conduct usability testing on checkout") },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = IndigoPrimary,
          unfocusedBorderColor = MaterialTheme.colorScheme.outline
        )
      )

      Spacer(modifier = Modifier.height(12.dp))

      OutlinedTextField(
        value = notes,
        onValueChange = { notes = it },
        label = { Text("Actionable Notes or Guidance (Optional)") },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        maxLines = 2,
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = IndigoPrimary,
          unfocusedBorderColor = MaterialTheme.colorScheme.outline
        )
      )

      Spacer(modifier = Modifier.height(12.dp))

      // Priority Selector
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Flag, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(15.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Sub-task Priority", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
      }
      Spacer(modifier = Modifier.height(6.dp))
      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Priority.entries.forEach { prio ->
          val selected = selectedPriority == prio
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            border = if (selected) androidx.compose.foundation.BorderStroke(1.5.dp, IndigoPrimary) else null,
            modifier = Modifier
              .weight(1f)
              .clickable { selectedPriority = prio }
          ) {
            Text(
              text = prio.label,
              fontSize = 11.sp,
              fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
              color = if (selected) IndigoPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(vertical = 6.dp),
              textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Category Tag Chips
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.LocalOffer, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(15.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Category Tag", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
      }
      Spacer(modifier = Modifier.height(6.dp))
      val defaultTags = listOf("Architecture", "Design", "Development", "Testing", "Review", "DevOps", "Research")
      Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        defaultTags.take(4).forEach { tag ->
          val isSelected = categoryTag.equals(tag, ignoreCase = true)
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (isSelected) IndigoPrimary else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
              .weight(1f)
              .clickable { categoryTag = tag }
          ) {
            Text(
              text = tag,
              fontSize = 10.sp,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
              color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(vertical = 6.dp),
              textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("Estimated Duration", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Text("${estimatedMinutes.toInt()} mins", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = IndigoPrimary)
      }

      Slider(
        value = estimatedMinutes,
        onValueChange = { estimatedMinutes = it },
        valueRange = 5f..120f,
        steps = 22,
        colors = SliderDefaults.colors(
          thumbColor = IndigoPrimary,
          activeTrackColor = IndigoPrimary
        )
      )

      Spacer(modifier = Modifier.height(20.dp))

      Button(
        onClick = {
          if (title.isNotBlank()) {
            onAddSubtask(title, estimatedMinutes.toInt(), notes, selectedPriority, categoryTag)
            onDismiss()
          }
        },
        enabled = title.isNotBlank(),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = IndigoPrimary,
          contentColor = Color.White
        ),
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
      ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Add Sub-task", fontSize = 14.sp, fontWeight = FontWeight.Bold)
      }
    }
  }
}
