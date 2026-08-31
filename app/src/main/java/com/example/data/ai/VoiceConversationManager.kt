package com.example.data.ai

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.BuildConfig
import com.example.data.model.Priority
import com.example.data.model.SubTask
import com.example.data.model.TaskCategory
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

enum class VoiceState {
  IDLE,
  LISTENING,
  PROCESSING,
  SPEAKING,
  ERROR
}

enum class MessageSender {
  USER, AI
}

data class VoiceChatMessage(
  val id: String = UUID.randomUUID().toString(),
  val sender: MessageSender,
  val text: String,
  val timestamp: Long = System.currentTimeMillis(),
  val breakdown: BreakdownResult? = null,
  val taskTitle: String? = null,
  val taskDescription: String? = null
)

class VoiceConversationManager(
  private val context: Context,
  private val scope: CoroutineScope
) {
  private val client = OkHttpClient.Builder()
    .connectTimeout(60, TimeUnit.SECONDS)
    .readTimeout(60, TimeUnit.SECONDS)
    .writeTimeout(60, TimeUnit.SECONDS)
    .build()

  private val modelName = "gemini-3.7-flash"
  private val fallbackModelName = "gemini-3.5-flash"

  private val mainHandler = Handler(Looper.getMainLooper())

  private var speechRecognizer: SpeechRecognizer? = null
  private var textToSpeech: TextToSpeech? = null
  private var isTtsInitialized = false

  private val _voiceState = MutableStateFlow(VoiceState.IDLE)
  val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

  private val _audioRmsLevel = MutableStateFlow(0f)
  val audioRmsLevel: StateFlow<Float> = _audioRmsLevel.asStateFlow()

  private val _isTtsMuted = MutableStateFlow(false)
  val isTtsMuted: StateFlow<Boolean> = _isTtsMuted.asStateFlow()

  private val _messages = MutableStateFlow<List<VoiceChatMessage>>(emptyList())
  val messages: StateFlow<List<VoiceChatMessage>> = _messages.asStateFlow()

  private val _currentListeningHypothesis = MutableStateFlow("")
  val currentListeningHypothesis: StateFlow<String> = _currentListeningHypothesis.asStateFlow()

  private val _errorMessage = MutableStateFlow<String?>(null)
  val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

  init {
    initTts()
    addInitialGreeting()
  }

  private fun initTts() {
    try {
      textToSpeech = TextToSpeech(context) { status ->
        if (status == TextToSpeech.SUCCESS) {
          textToSpeech?.let { tts ->
            val result = tts.setLanguage(Locale.getDefault())
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
              tts.setLanguage(Locale.US)
            }
            tts.setSpeechRate(1.0f)
            tts.setPitch(1.02f)
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
              override fun onStart(utteranceId: String?) {
                _voiceState.value = VoiceState.SPEAKING
              }

              override fun onDone(utteranceId: String?) {
                if (_voiceState.value == VoiceState.SPEAKING) {
                  _voiceState.value = VoiceState.IDLE
                }
              }

              @Deprecated("Deprecated in Java")
              override fun onError(utteranceId: String?) {
                if (_voiceState.value == VoiceState.SPEAKING) {
                  _voiceState.value = VoiceState.IDLE
                }
              }
            })
            isTtsInitialized = true
          }
        }
      }
    } catch (e: Exception) {
      Log.e("VoiceManager", "Error initializing TTS", e)
    }
  }

  private fun addInitialGreeting() {
    if (_messages.value.isEmpty()) {
      val greetingText = "Hi! I'm your AI Task Coach. Tell me what project or goal you're working on, and I'll break it down into sequential subtasks for you."
      val greeting = VoiceChatMessage(
        sender = MessageSender.AI,
        text = greetingText
      )
      _messages.value = listOf(greeting)
    }
  }

  fun toggleTtsMute() {
    _isTtsMuted.value = !_isTtsMuted.value
    if (_isTtsMuted.value) {
      stopSpeaking()
    }
  }

  fun startListening() {
    mainHandler.post {
      try {
        stopSpeaking()
        _errorMessage.value = null
        _currentListeningHypothesis.value = ""

        if (speechRecognizer == null) {
          if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _errorMessage.value = "Speech recognition is not available on this device."
            _voiceState.value = VoiceState.ERROR
            return@post
          }
          speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        }

        val recognizer = speechRecognizer ?: return@post
        recognizer.setRecognitionListener(object : RecognitionListener {
          override fun onReadyForSpeech(params: Bundle?) {
            _voiceState.value = VoiceState.LISTENING
          }

          override fun onBeginningOfSpeech() {
            _voiceState.value = VoiceState.LISTENING
          }

          override fun onRmsChanged(rmsdB: Float) {
            // Normalize RMS dB (-2 to 10 typical) to 0.0 .. 1.0 for visualizer
            val normalized = ((rmsdB + 2f) / 12f).coerceIn(0.05f, 1f)
            _audioRmsLevel.value = normalized
          }

          override fun onBufferReceived(buffer: ByteArray?) {}

          override fun onEndOfSpeech() {
            _voiceState.value = VoiceState.PROCESSING
            _audioRmsLevel.value = 0f
          }

          override fun onError(error: Int) {
            val errStr = when (error) {
              SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
              SpeechRecognizer.ERROR_CLIENT -> "Client error"
              SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Audio permission required"
              SpeechRecognizer.ERROR_NETWORK -> "Network connection error"
              SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
              SpeechRecognizer.ERROR_NO_MATCH -> "Didn't catch that, please try again"
              SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer is busy"
              SpeechRecognizer.ERROR_SERVER -> "Server error"
              SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected"
              else -> "Speech recognition error ($error)"
            }
            Log.w("VoiceManager", "Speech recognition error: $errStr ($error)")
            _voiceState.value = VoiceState.IDLE
            _audioRmsLevel.value = 0f
            if (error != SpeechRecognizer.ERROR_NO_MATCH && error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
              _errorMessage.value = errStr
            }
          }

          override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val spokenText = matches?.firstOrNull() ?: ""
            _voiceState.value = VoiceState.IDLE
            _audioRmsLevel.value = 0f
            _currentListeningHypothesis.value = ""

            if (spokenText.isNotBlank()) {
              processUserSpokenInput(spokenText)
            }
          }

          override fun onPartialResults(partialResults: Bundle?) {
            val partials = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = partials?.firstOrNull() ?: ""
            if (text.isNotBlank()) {
              _currentListeningHypothesis.value = text
            }
          }

          override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
          putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
          putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
          putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
          putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        recognizer.startListening(intent)
        _voiceState.value = VoiceState.LISTENING
      } catch (e: Exception) {
        Log.e("VoiceManager", "Failed to start listening", e)
        _errorMessage.value = "Unable to start speech recognizer: ${e.message}"
        _voiceState.value = VoiceState.IDLE
      }
    }
  }

  fun stopListening() {
    mainHandler.post {
      try {
        speechRecognizer?.stopListening()
      } catch (e: Exception) {
        Log.e("VoiceManager", "Error stopping listening", e)
      }
      _voiceState.value = VoiceState.IDLE
      _audioRmsLevel.value = 0f
    }
  }

  fun cancelListening() {
    mainHandler.post {
      try {
        speechRecognizer?.cancel()
      } catch (e: Exception) {
        Log.e("VoiceManager", "Error canceling listening", e)
      }
      _voiceState.value = VoiceState.IDLE
      _audioRmsLevel.value = 0f
      _currentListeningHypothesis.value = ""
    }
  }

  fun processUserSpokenInput(text: String) {
    if (text.isBlank()) return

    val userMessage = VoiceChatMessage(
      sender = MessageSender.USER,
      text = text.trim()
    )
    _messages.value = _messages.value + userMessage
    _voiceState.value = VoiceState.PROCESSING

    scope.launch {
      sendVoicePromptToGemini(text.trim())
    }
  }

  private suspend fun sendVoicePromptToGemini(userInput: String) = withContext(Dispatchers.IO) {
    val apiKey = try {
      BuildConfig.GEMINI_API_KEY
    } catch (e: Throwable) {
      ""
    }

    if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
      Log.d("VoiceManager", "No API key, using smart local voice coach")
      handleLocalVoiceResponse(userInput)
      return@withContext
    }

    val conversationHistory = _messages.value.takeLast(6).map { msg ->
      val role = if (msg.sender == MessageSender.USER) "user" else "model"
      JSONObject().apply {
        put("role", role)
        put("parts", JSONArray().apply {
          put(JSONObject().put("text", msg.text))
        })
      }
    }

    val systemInstruction = buildString {
      appendLine("You are an expert Agile project decomposition and productivity coach named BreakDown Coach.")
      appendLine("You are engaged in a real-time voice conversation with the user to help them plan, execute, and break down their tasks into milestones and actionable subtasks.")
      appendLine()
      appendLine("Follow these instructions strictly:")
      appendLine("1. 'spokenResponse': A concise, articulate, friendly spoken response (2-3 sentences max) directly answering the user or coaching them. Do not use markdown or bullet points in 'spokenResponse' because it will be spoken aloud via TTS.")
      appendLine("2. 'hasBreakdown': Set to true whenever the user describes a project, feature, goal, study plan, or task to decompose. If the user is just asking a brief conversational question or greeting, set to false.")
      appendLine("3. If 'hasBreakdown' is true, provide:")
      appendLine("   - 'taskTitle': Concise action-oriented overall task title.")
      appendLine("   - 'taskDescription': 1-sentence summary of the scope/context.")
      appendLine("   - 'category': One of ['Work', 'Coding', 'Study', 'Personal', 'Health', 'Project'].")
      appendLine("   - 'priority': One of ['Urgent', 'High', 'Medium', 'Low'].")
      appendLine("   - 'milestones': An array of 2 to 3 sequential Milestones, each with 2 to 4 actionable subtasks with 'title', 'estimatedMinutes' (10-90), 'priority', 'categoryTag', and 'actionableNotes'.")
      appendLine()
      appendLine("Respond with valid raw JSON:")
      appendLine("""
        {
          "spokenResponse": "I've structured your mobile app redesign into 3 key milestones starting with user flows.",
          "hasBreakdown": true,
          "taskTitle": "Mobile App UI Redesign",
          "taskDescription": "Complete overhaul of the user onboarding and checkout UX",
          "category": "Coding",
          "priority": "High",
          "milestones": [
            {
              "title": "Milestone 1: Research & Wireframing",
              "subtasks": [
                {
                  "title": "Audit existing checkout drop-off rates",
                  "estimatedMinutes": 30,
                  "priority": "High",
                  "categoryTag": "Research",
                  "actionableNotes": "Check analytics funnels and heatmap recordings"
                }
              ]
            }
          ]
        }
      """.trimIndent())
    }

    val requestJson = JSONObject().apply {
      val contentsArray = JSONArray()
      conversationHistory.forEach { contentsArray.put(it) }
      put("contents", contentsArray)

      put("systemInstruction", JSONObject().apply {
        put("parts", JSONArray().apply {
          put(JSONObject().put("text", systemInstruction))
        })
      })

      val generationConfig = JSONObject().apply {
        put("temperature", 0.4)
        put("responseMimeType", "application/json")
      }
      put("generationConfig", generationConfig)
    }

    var success = false
    val modelsToTry = listOf(modelName, fallbackModelName)

    for (model in modelsToTry) {
      if (success) break
      val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
      try {
        val body = requestJson.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
          .url(url)
          .post(body)
          .build()

        val response = client.newCall(request).execute()
        val responseString = response.body?.string()

        if (response.isSuccessful && !responseString.isNullOrBlank()) {
          val json = JSONObject(responseString)
          val candidates = json.optJSONArray("candidates")
          val firstCandidate = candidates?.optJSONObject(0)
          val contentObj = firstCandidate?.optJSONObject("content")
          val parts = contentObj?.optJSONArray("parts")
          val rawText = parts?.optJSONObject(0)?.optString("text") ?: ""

          parseAndApplyVoiceResponse(rawText, model)
          success = true
        } else {
          Log.w("VoiceManager", "Model $model returned code ${response.code}: $responseString")
        }
      } catch (e: Exception) {
        Log.e("VoiceManager", "Error calling $model", e)
      }
    }

    if (!success) {
      handleLocalVoiceResponse(userInput)
    }
  }

  private suspend fun parseAndApplyVoiceResponse(rawJsonText: String, model: String) = withContext(Dispatchers.Main) {
    try {
      val cleanJson = rawJsonText.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
      val obj = JSONObject(cleanJson)

      val spokenResponse = obj.optString("spokenResponse", "Here is the breakdown for your project.")
      val hasBreakdown = obj.optBoolean("hasBreakdown", false)

      var breakdownResult: BreakdownResult? = null
      var taskTitle: String? = null
      var taskDescription: String? = null

      if (hasBreakdown) {
        taskTitle = obj.optString("taskTitle", "New Project Goal")
        taskDescription = obj.optString("taskDescription", "")
        val categoryStr = obj.optString("category", "Work")
        val priorityStr = obj.optString("priority", "Medium")
        val category = TaskCategory.fromString(categoryStr)
        val priority = Priority.fromString(priorityStr)

        val subtasks = mutableListOf<SubTask>()
        val milestonesArray = obj.optJSONArray("milestones")
        val tempTaskId = UUID.randomUUID().toString()

        if (milestonesArray != null && milestonesArray.length() > 0) {
          var orderIdx = 0
          for (m in 0 until milestonesArray.length()) {
            val milestoneObj = milestonesArray.getJSONObject(m)
            val milestoneTitle = milestoneObj.optString("title", "Milestone ${m + 1}")
            val subArray = milestoneObj.optJSONArray("subtasks") ?: JSONArray()
            for (s in 0 until subArray.length()) {
              val subObj = subArray.getJSONObject(s)
              val title = subObj.optString("title", "Subtask ${s + 1}")
              val minutes = subObj.optInt("estimatedMinutes", 30).coerceIn(5, 240)
              val subPrioStr = subObj.optString("priority", "Medium")
              val tag = subObj.optString("categoryTag", category.label)
              val notes = subObj.optString("actionableNotes", "")

              subtasks.add(
                SubTask(
                  id = UUID.randomUUID().toString(),
                  taskId = tempTaskId,
                  title = title,
                  estimatedMinutes = minutes,
                  actualMinutes = 0,
                  isCompleted = false,
                  orderIndex = orderIdx++,
                  actionableNotes = notes,
                  milestoneTitle = milestoneTitle,
                  priority = Priority.fromString(subPrioStr),
                  categoryTag = tag
                )
              )
            }
          }
        }

        if (subtasks.isNotEmpty()) {
          breakdownResult = BreakdownResult(
            subtasks = subtasks,
            aiExplanation = spokenResponse,
            sourceModel = model,
            determinedCategory = category,
            determinedPriority = priority
          )
        }
      }

      val aiMessage = VoiceChatMessage(
        sender = MessageSender.AI,
        text = spokenResponse,
        breakdown = breakdownResult,
        taskTitle = taskTitle,
        taskDescription = taskDescription
      )

      _messages.value = _messages.value + aiMessage
      speakText(spokenResponse)
    } catch (e: Exception) {
      Log.e("VoiceManager", "Error parsing voice response JSON", e)
      handleLocalVoiceResponse(rawJsonText)
    }
  }

  private suspend fun handleLocalVoiceResponse(userInput: String) = withContext(Dispatchers.Main) {
    val service = GeminiBreakdownService()
    val tempTaskId = UUID.randomUUID().toString()
    val localBreakdown = service.generateSmartLocalBreakdown(
      taskId = tempTaskId,
      taskTitle = userInput
    )

    val spokenText = "I've broken down '$userInput' into ${localBreakdown.subtasks.size} sequential subtasks across ${localBreakdown.subtasks.map { it.milestoneTitle }.distinct().size} milestones."

    val aiMessage = VoiceChatMessage(
      sender = MessageSender.AI,
      text = spokenText,
      breakdown = localBreakdown,
      taskTitle = userInput,
      taskDescription = "Created via Voice AI Coach"
    )

    _messages.value = _messages.value + aiMessage
    speakText(spokenText)
  }

  fun speakText(text: String) {
    if (_isTtsMuted.value || !isTtsInitialized) {
      _voiceState.value = VoiceState.IDLE
      return
    }

    try {
      val cleanText = text.replace(Regex("[*#_`>]"), "").trim()
      val params = Bundle().apply {
        putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, UUID.randomUUID().toString())
      }
      _voiceState.value = VoiceState.SPEAKING
      textToSpeech?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, params, params.getString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID))
    } catch (e: Exception) {
      Log.e("VoiceManager", "Error speaking text", e)
      _voiceState.value = VoiceState.IDLE
    }
  }

  fun stopSpeaking() {
    try {
      textToSpeech?.stop()
    } catch (e: Exception) {
      Log.e("VoiceManager", "Error stopping TTS", e)
    }
    if (_voiceState.value == VoiceState.SPEAKING) {
      _voiceState.value = VoiceState.IDLE
    }
  }

  fun clearConversation() {
    stopSpeaking()
    stopListening()
    _messages.value = emptyList()
    addInitialGreeting()
  }

  fun destroy() {
    try {
      speechRecognizer?.destroy()
      speechRecognizer = null
      textToSpeech?.stop()
      textToSpeech?.shutdown()
      textToSpeech = null
    } catch (e: Exception) {
      Log.e("VoiceManager", "Error destroying VoiceManager", e)
    }
  }
}
