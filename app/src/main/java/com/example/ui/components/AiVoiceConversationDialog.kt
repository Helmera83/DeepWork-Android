package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.ai.BreakdownResult
import com.example.data.ai.MessageSender
import com.example.data.ai.VoiceChatMessage
import com.example.data.ai.VoiceConversationManager
import com.example.data.ai.VoiceState
import com.example.ui.theme.BaselinePrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.ExpressiveAmber
import com.example.ui.theme.ToneTealTertiary
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiVoiceConversationDialog(
  voiceManager: VoiceConversationManager,
  onApplyBreakdown: (
    title: String,
    description: String,
    breakdown: BreakdownResult,
    deadLineTimestamp: Long?
  ) -> Unit,
  onDismiss: () -> Unit
) {
  val sheetState = rememberModalBottomSheetState(
    skipPartiallyExpanded = true
  )
  val context = LocalContext.current

  val voiceState by voiceManager.voiceState.collectAsState()
  val audioRmsLevel by voiceManager.audioRmsLevel.collectAsState()
  val isTtsMuted by voiceManager.isTtsMuted.collectAsState()
  val messages by voiceManager.messages.collectAsState()
  val currentHypothesis by voiceManager.currentListeningHypothesis.collectAsState()
  val errorMessage by voiceManager.errorMessage.collectAsState()

  var hasRecordAudioPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.RECORD_AUDIO
      ) == PackageManager.PERMISSION_GRANTED
    )
  }

  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    hasRecordAudioPermission = isGranted
    if (isGranted) {
      voiceManager.startListening()
    }
  }

  var textInput by remember { mutableStateOf("") }
  val addedMessageIds = remember { mutableStateListOf<String>() }
  val listState = rememberLazyListState()

  // Scroll to bottom on new messages
  LaunchedEffect(messages.size, currentHypothesis) {
    if (messages.isNotEmpty()) {
      listState.animateScrollToItem(messages.size - 1)
    }
  }

  ModalBottomSheet(
    onDismissRequest = {
      voiceManager.stopSpeaking()
      voiceManager.stopListening()
      onDismiss()
    },
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
    modifier = Modifier
      .fillMaxSize()
      .testTag("ai_voice_dialog")
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 20.dp)
        .padding(bottom = 24.dp)
    ) {
      // --- Top Bar ---
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Box(
            modifier = Modifier
              .size(38.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(
                Brush.linearGradient(
                  colors = listOf(BaselinePrimary, ToneTealTertiary)
                )
              ),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.RecordVoiceOver,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.size(20.dp)
            )
          }

          Column {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Text(
                text = "Voice Task Coach",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.primaryContainer
              ) {
                Text(
                  text = "gemini-3.7-flash (Live)",
                  fontSize = 9.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                )
              }
            }
            Text(
              text = "Speak your project goals to generate subtasks",
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(
            onClick = { voiceManager.toggleTtsMute() },
            modifier = Modifier.size(36.dp)
          ) {
            Icon(
              imageVector = if (isTtsMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
              contentDescription = if (isTtsMuted) "Unmute Voice" else "Mute Voice",
              tint = if (isTtsMuted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp)
            )
          }

          IconButton(
            onClick = { voiceManager.clearConversation() },
            modifier = Modifier.size(36.dp)
          ) {
            Icon(
              imageVector = Icons.Default.DeleteSweep,
              contentDescription = "Clear Conversation",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(20.dp)
            )
          }

          IconButton(
            onClick = {
              voiceManager.stopSpeaking()
              voiceManager.stopListening()
              onDismiss()
            },
            modifier = Modifier.size(36.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Close",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(20.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // --- Animated Audio Waveform & Status Banner ---
      VoiceWaveformVisualizer(
        voiceState = voiceState,
        audioRms = audioRmsLevel,
        currentHypothesis = currentHypothesis,
        errorMessage = errorMessage
      )

      Spacer(modifier = Modifier.height(10.dp))

      // --- Conversation Messages List ---
      LazyColumn(
        state = listState,
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 6.dp)
      ) {
        items(messages, key = { it.id }) { msg ->
          VoiceMessageBubble(
            message = msg,
            isAdded = addedMessageIds.contains(msg.id),
            onReplayAudio = { text -> voiceManager.speakText(text) },
            onAddBreakdownToTasks = {
              msg.breakdown?.let { breakdown ->
                addedMessageIds.add(msg.id)
                onApplyBreakdown(
                  msg.taskTitle ?: "Voice Planned Task",
                  msg.taskDescription ?: "Created from Voice AI conversation",
                  breakdown,
                  null
                )
              }
            }
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // --- Quick Spoken Prompts Carousel ---
      QuickVoicePromptsRow(
        onSelectPrompt = { prompt ->
          voiceManager.processUserSpokenInput(prompt)
        }
      )

      Spacer(modifier = Modifier.height(10.dp))

      // --- Voice Input Controls & Mic Action ---
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // Fallback text input
        OutlinedTextField(
          value = textInput,
          onValueChange = { textInput = it },
          placeholder = { Text("Or type project goal...", fontSize = 13.sp) },
          modifier = Modifier
            .weight(1f)
            .height(52.dp)
            .testTag("voice_text_fallback_input"),
          shape = RoundedCornerShape(26.dp),
          singleLine = true,
          trailingIcon = {
            if (textInput.isNotBlank()) {
              IconButton(
                onClick = {
                  val prompt = textInput.trim()
                  textInput = ""
                  voiceManager.processUserSpokenInput(prompt)
                }
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.Send,
                  contentDescription = "Send",
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(18.dp)
                )
              }
            }
          },
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
          )
        )

        // Main Live Microphone Button
        LiveMicrophoneButton(
          voiceState = voiceState,
          audioRms = audioRmsLevel,
          onClick = {
            if (!hasRecordAudioPermission) {
              permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            } else {
              when (voiceState) {
                VoiceState.LISTENING -> voiceManager.stopListening()
                VoiceState.SPEAKING -> voiceManager.stopSpeaking()
                VoiceState.PROCESSING -> voiceManager.cancelListening()
                else -> voiceManager.startListening()
              }
            }
          }
        )
      }
    }
  }
}

@Composable
private fun VoiceWaveformVisualizer(
  voiceState: VoiceState,
  audioRms: Float,
  currentHypothesis: String,
  errorMessage: String?
) {
  val infiniteTransition = rememberInfiniteTransition(label = "pulse")
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 0.95f,
    targetValue = 1.05f,
    animationSpec = infiniteRepeatable(
      animation = tween(600, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "scale"
  )

  Surface(
    shape = RoundedCornerShape(16.dp),
    color = when (voiceState) {
      VoiceState.LISTENING -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
      VoiceState.SPEAKING -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
      VoiceState.PROCESSING -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
      VoiceState.ERROR -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
      VoiceState.IDLE -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    },
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 12.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        when (voiceState) {
          VoiceState.LISTENING -> {
            Icon(
              imageVector = Icons.Default.Mic,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier
                .size(18.dp)
                .scale(pulseScale)
            )
            Text(
              text = if (currentHypothesis.isNotBlank()) "Hearing: \"$currentHypothesis\"" else "Listening... Speak your task or project goal",
              fontSize = 13.sp,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.primary
            )
          }
          VoiceState.SPEAKING -> {
            Icon(
              imageVector = Icons.Default.GraphicEq,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.secondary,
              modifier = Modifier.size(18.dp)
            )
            Text(
              text = "AI Coach Speaking...",
              fontSize = 13.sp,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.secondary
            )
          }
          VoiceState.PROCESSING -> {
            CircularProgressIndicator(
              modifier = Modifier.size(16.dp),
              strokeWidth = 2.dp,
              color = MaterialTheme.colorScheme.tertiary
            )
            Text(
              text = "Decomposing task with Gemini Live...",
              fontSize = 13.sp,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.tertiary
            )
          }
          VoiceState.ERROR -> {
            Text(
              text = errorMessage ?: "Voice recognition error. Tap mic to retry.",
              fontSize = 13.sp,
              fontWeight = FontWeight.Medium,
              color = MaterialTheme.colorScheme.error
            )
          }
          VoiceState.IDLE -> {
            Icon(
              imageVector = Icons.Default.RecordVoiceOver,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(16.dp)
            )
            Text(
              text = "Tap the microphone below to talk with AI Coach",
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }

      // 5-bar animated equalizer wave
      if (voiceState == VoiceState.LISTENING || voiceState == VoiceState.SPEAKING || voiceState == VoiceState.PROCESSING) {
        Spacer(modifier = Modifier.height(8.dp))
        Row(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          val baseColor = when (voiceState) {
            VoiceState.LISTENING -> MaterialTheme.colorScheme.primary
            VoiceState.SPEAKING -> MaterialTheme.colorScheme.secondary
            else -> MaterialTheme.colorScheme.tertiary
          }

          val bars = 9
          for (i in 0 until bars) {
            val offset = sin(System.currentTimeMillis() / 150.0 + i * 0.8).toFloat()
            val barHeight = when (voiceState) {
              VoiceState.LISTENING -> (8.dp + (32.dp * (audioRms * 0.8f + (offset * 0.2f).coerceAtLeast(0f))))
              VoiceState.SPEAKING -> (6.dp + (22.dp * (0.5f + offset * 0.5f).coerceIn(0.1f, 1f)))
              else -> (6.dp + (14.dp * (0.3f + offset * 0.3f).coerceIn(0.1f, 1f)))
            }

            Box(
              modifier = Modifier
                .width(4.dp)
                .height(barHeight)
                .clip(RoundedCornerShape(2.dp))
                .background(baseColor)
            )
          }
        }
      }
    }
  }
}

@Composable
private fun VoiceMessageBubble(
  message: VoiceChatMessage,
  isAdded: Boolean,
  onReplayAudio: (String) -> Unit,
  onAddBreakdownToTasks: () -> Unit
) {
  val isUser = message.sender == MessageSender.USER

  Column(
    modifier = Modifier.fillMaxWidth(),
    horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
  ) {
    Surface(
      shape = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomStart = if (isUser) 16.dp else 4.dp,
        bottomEnd = if (isUser) 4.dp else 16.dp
      ),
      color = if (isUser) {
        MaterialTheme.colorScheme.primaryContainer
      } else {
        MaterialTheme.colorScheme.surfaceVariant
      },
      modifier = Modifier
        .widthIn(max = 320.dp)
        .clip(
          RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp,
            bottomStart = if (isUser) 16.dp else 4.dp,
            bottomEnd = if (isUser) 4.dp else 16.dp
          )
        )
    ) {
      Column(modifier = Modifier.padding(12.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = if (isUser) "You" else "AI Coach",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
          )

          if (!isUser) {
            IconButton(
              onClick = { onReplayAudio(message.text) },
              modifier = Modifier.size(24.dp)
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = "Replay audio",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
          text = message.text,
          fontSize = 14.sp,
          color = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
          lineHeight = 20.sp
        )
      }
    }

    // --- Embedded Breakdown Card (if AI extracted milestones/subtasks) ---
    message.breakdown?.let { breakdown ->
      Spacer(modifier = Modifier.height(6.dp))
      VoiceBreakdownPreviewCard(
        taskTitle = message.taskTitle ?: "Project Goal",
        taskDescription = message.taskDescription ?: "",
        breakdown = breakdown,
        isAdded = isAdded,
        onAdd = onAddBreakdownToTasks
      )
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VoiceBreakdownPreviewCard(
  taskTitle: String,
  taskDescription: String,
  breakdown: BreakdownResult,
  isAdded: Boolean,
  onAdd: () -> Unit
) {
  val totalMins = breakdown.subtasks.sumOf { it.estimatedMinutes }
  val distinctMilestones = breakdown.subtasks.map { it.milestoneTitle }.distinct().filter { it.isNotBlank() }

  Surface(
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 2.dp,
    modifier = Modifier
      .fillMaxWidth()
      .border(
        width = 1.dp,
        color = if (isAdded) EmeraldSuccess.copy(alpha = 0.8f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
        shape = RoundedCornerShape(16.dp)
      )
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.primaryContainer
          ) {
            Text(
              text = breakdown.determinedCategory.label,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }

          Surface(
            shape = RoundedCornerShape(6.dp),
            color = when (breakdown.determinedPriority) {
              com.example.data.model.Priority.URGENT -> MaterialTheme.colorScheme.errorContainer
              com.example.data.model.Priority.HIGH -> ExpressiveAmber.copy(alpha = 0.2f)
              else -> MaterialTheme.colorScheme.surfaceVariant
            }
          ) {
            Text(
              text = "${breakdown.determinedPriority.label} Priority",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = when (breakdown.determinedPriority) {
                com.example.data.model.Priority.URGENT -> MaterialTheme.colorScheme.error
                com.example.data.model.Priority.HIGH -> ExpressiveAmber
                else -> MaterialTheme.colorScheme.onSurfaceVariant
              },
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
        }

        Text(
          text = "${breakdown.subtasks.size} subtasks • ${totalMins}m total",
          fontSize = 11.sp,
          fontWeight = FontWeight.Medium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = taskTitle,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
      )

      if (taskDescription.isNotBlank()) {
        Text(
          text = taskDescription,
          fontSize = 12.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(top = 2.dp)
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Subtasks Preview list
      Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        breakdown.subtasks.take(4).forEachIndexed { idx, sub ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(8.dp))
              .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
              .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.weight(1f)
            ) {
              Box(
                modifier = Modifier
                  .size(18.dp)
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = "${idx + 1}",
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary
                )
              }
              Text(
                text = sub.title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
              )
            }

            Text(
              text = "${sub.estimatedMinutes}m",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary
            )
          }
        }

        if (breakdown.subtasks.size > 4) {
          Text(
            text = "+ ${breakdown.subtasks.size - 4} more subtasks",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Add to Workspace Button
      Button(
        onClick = onAdd,
        enabled = !isAdded,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = if (isAdded) EmeraldSuccess else MaterialTheme.colorScheme.primary,
          disabledContainerColor = EmeraldSuccess.copy(alpha = 0.8f)
        ),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("add_voice_breakdown_btn")
      ) {
        Icon(
          imageVector = if (isAdded) Icons.Default.Check else Icons.Default.AutoAwesome,
          contentDescription = null,
          tint = Color.White,
          modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = if (isAdded) "Added to Workspace" else "Add Task to Workspace",
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold,
          color = Color.White
        )
      }
    }
  }
}

@Composable
private fun QuickVoicePromptsRow(
  onSelectPrompt: (String) -> Unit
) {
  val prompts = listOf(
    "Build mobile app user auth & profile",
    "Study for biology final exam",
    "Prepare client quarterly pitch deck",
    "Organize home office and workspace",
    "Plan 10K running training schedule"
  )

  val scrollState = rememberScrollState()

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .horizontalScroll(scrollState),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    prompts.forEach { p ->
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier.clickable { onSelectPrompt(p) }
      ) {
        Text(
          text = p,
          fontSize = 11.sp,
          fontWeight = FontWeight.Medium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
      }
    }
  }
}

@Composable
private fun LiveMicrophoneButton(
  voiceState: VoiceState,
  audioRms: Float,
  onClick: () -> Unit
) {
  val infiniteTransition = rememberInfiniteTransition(label = "mic_glow")
  val glowScale by infiniteTransition.animateFloat(
    initialValue = 1f,
    targetValue = 1.25f,
    animationSpec = infiniteRepeatable(
      animation = tween(800, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "glow"
  )

  val isListening = voiceState == VoiceState.LISTENING

  Box(
    contentAlignment = Alignment.Center,
    modifier = Modifier.size(56.dp)
  ) {
    if (isListening) {
      Box(
        modifier = Modifier
          .size(56.dp)
          .scale(glowScale + (audioRms * 0.2f))
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
      )
    }

    Surface(
      shape = CircleShape,
      color = when (voiceState) {
        VoiceState.LISTENING -> MaterialTheme.colorScheme.primary
        VoiceState.SPEAKING -> MaterialTheme.colorScheme.secondary
        VoiceState.PROCESSING -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
      },
      shadowElevation = 4.dp,
      modifier = Modifier
        .size(50.dp)
        .clip(CircleShape)
        .clickable { onClick() }
        .testTag("voice_mic_fab")
    ) {
      Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = when (voiceState) {
            VoiceState.LISTENING -> Icons.Default.Stop
            VoiceState.SPEAKING -> Icons.Default.GraphicEq
            VoiceState.PROCESSING -> Icons.Default.AutoAwesome
            else -> Icons.Default.Mic
          },
          contentDescription = "Voice Microphone",
          tint = Color.White,
          modifier = Modifier.size(24.dp)
        )
      }
    }
  }
}
