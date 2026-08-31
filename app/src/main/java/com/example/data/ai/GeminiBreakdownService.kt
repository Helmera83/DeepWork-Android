package com.example.data.ai

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.ChatMessage
import com.example.data.model.ChatSender
import com.example.data.model.Priority
import com.example.data.model.SubTask
import com.example.data.model.Task
import com.example.data.model.TaskCategory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

data class BreakdownResult(
  val subtasks: List<SubTask>,
  val aiExplanation: String = "",
  val sourceModel: String = "",
  val determinedCategory: TaskCategory = TaskCategory.WORK,
  val determinedPriority: Priority = Priority.MEDIUM
)

class GeminiBreakdownService {

  private val client = OkHttpClient.Builder()
    .connectTimeout(60, TimeUnit.SECONDS)
    .readTimeout(60, TimeUnit.SECONDS)
    .writeTimeout(60, TimeUnit.SECONDS)
    .build()

  private val defaultModel = "gemini-3.5-flash"

  suspend fun breakdownTask(
    taskId: String,
    taskTitle: String,
    taskDescription: String = "",
    deadlineTimestamp: Long? = null
  ): BreakdownResult = withContext(Dispatchers.IO) {
    val apiKey = try {
      BuildConfig.GEMINI_API_KEY
    } catch (e: Throwable) {
      ""
    }

    if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
      Log.d("GeminiBreakdown", "No active API key found, generating smart local fallback")
      return@withContext generateSmartLocalBreakdown(taskId, taskTitle, taskDescription, deadlineTimestamp)
    }

    val url = "https://generativelanguage.googleapis.com/v1beta/models/$defaultModel:generateContent?key=$apiKey"

    val prompt = buildString {
      appendLine("You are an expert productivity coach and project decomposition specialist.")
      appendLine("Analyze the following user goal, automatically detect its appropriate high-level category and priority level, and break it down into sequential Milestones (optimal number, typically 2 to 4 milestones), where each Milestone contains an optimal number of concise, highly actionable sequential sub-tasks.")
      appendLine()
      appendLine("1. Intelligently determine the OVERALL TASK CATEGORY from: ['Work', 'Coding', 'Study', 'Personal', 'Health', 'Project'].")
      appendLine("2. Intelligently determine the OVERALL TASK PRIORITY from: ['Urgent', 'High', 'Medium', 'Low'] based on timeline, importance, and technical/scope depth.")
      appendLine("3. For EACH sub-task:")
      appendLine("   - Assign a realistic completion time estimate in minutes (e.g. 15, 25, 45, 60 mins).")
      appendLine("   - Assign an individual sub-task PRIORITY ('Urgent', 'High', 'Medium', or 'Low') based on dependencies and criticality.")
      appendLine("   - Assign a concise CATEGORY TAG (e.g. 'Research', 'Architecture', 'Design', 'Development', 'Testing', 'DevOps', 'Documentation', 'Review', 'Planning').")
      appendLine("   - Assign a RECOMMENDED DUE DATE OFFSET ('dueDayOffset' as an integer representing number of days from today, e.g. 0 for today, 1 for tomorrow, 2 for 2 days out) to pace sub-tasks sequentially.")
      appendLine("   - Provide actionable notes/guidance.")
      appendLine()
      appendLine("Task Title: $taskTitle")
      if (taskDescription.isNotBlank()) appendLine("Context / Details: $taskDescription")

      if (deadlineTimestamp != null && deadlineTimestamp > System.currentTimeMillis()) {
        val diffMillis = deadlineTimestamp - System.currentTimeMillis()
        val diffDays = (diffMillis / (1000 * 60 * 60 * 24)).coerceAtLeast(1)
        val sdf = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
        val formattedDate = sdf.format(Date(deadlineTimestamp))
        appendLine()
        appendLine("TARGET COMPLETION DEADLINE: $formattedDate (Allotted timeframe: $diffDays days)")
        appendLine("CRITICAL SCHEDULING CONSTRAINT: Structure all milestones and subtasks with sequential due date offsets ('dueDayOffset': 0 to $diffDays) so each subtask has a dedicated deadline that fits comfortably and realistically within this $diffDays-day allotted window.")
      } else {
        appendLine("Determine the optimal number of milestones and subtasks with sequential due date offsets ('dueDayOffset') for complete, successful execution.")
      }

      appendLine()
      appendLine("Respond with valid raw JSON matching this schema:")
      appendLine("""
        {
          "category": "Coding",
          "priority": "High",
          "explanation": "Brief 1-sentence strategic advice and timeframe pacing for this task",
          "milestones": [
            {
              "title": "Milestone 1: Clear phase or goal title",
              "subtasks": [
                {
                  "title": "Clear action verb + subtask name",
                  "estimatedMinutes": 30,
                  "priority": "High",
                  "categoryTag": "Development",
                  "dueDayOffset": 0,
                  "actionableNotes": "Concrete execution guidance or key deliverable"
                }
              ]
            }
          ]
        }
      """.trimIndent())
    }

    val requestJson = JSONObject().apply {
      val contentsArray = JSONArray().apply {
        put(JSONObject().apply {
          put("parts", JSONArray().apply {
            put(JSONObject().put("text", prompt))
          })
        })
      }
      put("contents", contentsArray)

      val generationConfig = JSONObject().apply {
        put("temperature", 0.3)
        put("responseMimeType", "application/json")
      }
      put("generationConfig", generationConfig)
    }

    try {
      val body = requestJson.toString().toRequestBody("application/json".toMediaType())
      val request = Request.Builder()
        .url(url)
        .post(body)
        .build()

      val response = client.newCall(request).execute()
      val responseString = response.body?.string()

      if (!response.isSuccessful || responseString.isNullOrBlank()) {
        Log.w("GeminiBreakdown", "API call failed code ${response.code}: $responseString")
        return@withContext generateSmartLocalBreakdown(taskId, taskTitle, taskDescription, deadlineTimestamp)
      }

      val json = JSONObject(responseString)
      val candidates = json.optJSONArray("candidates")
      val firstCandidate = candidates?.optJSONObject(0)
      val contentObj = firstCandidate?.optJSONObject("content")
      val parts = contentObj?.optJSONArray("parts")
      val rawText = parts?.optJSONObject(0)?.optString("text") ?: ""

      parseJsonBreakdown(taskId, rawText, defaultModel, taskTitle, taskDescription, deadlineTimestamp)
    } catch (e: Exception) {
      Log.e("GeminiBreakdown", "Error in Gemini breakdown", e)
      generateSmartLocalBreakdown(taskId, taskTitle, taskDescription, deadlineTimestamp)
    }
  }

  suspend fun generateAdditionalSubtasks(
    task: com.example.data.model.Task,
    additionalInstructions: String = "",
    count: Int = 3
  ): BreakdownResult = withContext(Dispatchers.IO) {
    val apiKey = try {
      BuildConfig.GEMINI_API_KEY
    } catch (e: Throwable) {
      ""
    }

    val existingTitles = task.subtasks.map { it.title }

    if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
      Log.d("GeminiBreakdown", "Generating smart local additional subtasks")
      return@withContext generateSmartLocalAdditionalSubtasks(task, existingTitles, additionalInstructions, count)
    }

    val url = "https://generativelanguage.googleapis.com/v1beta/models/$defaultModel:generateContent?key=$apiKey"

    val prompt = buildString {
      appendLine("You are an expert productivity coach and task decomposition specialist.")
      appendLine("The user is working on the task: '${task.title}'")
      if (task.description.isNotBlank()) appendLine("Task Context: ${task.description}")
      appendLine("Category: ${task.category.label}")
      appendLine()
      appendLine("The task ALREADY has the following ${task.subtasks.size} sub-tasks:")
      existingTitles.forEachIndexed { i, t -> appendLine("  ${i + 1}. $t") }
      appendLine()
      if (additionalInstructions.isNotBlank()) {
        appendLine("Specific user focus / requirements for new steps: $additionalInstructions")
      }
      appendLine("Generate exactly $count NEW, non-duplicate, highly actionable additional sub-tasks that build upon or fill gaps in the existing plan.")
      appendLine("For EACH new sub-task:")
      appendLine("- provide a realistic estimated time in minutes (5 to 120 mins)")
      appendLine("- assign sub-task priority ('Urgent', 'High', 'Medium', or 'Low')")
      appendLine("- assign a concise category tag (e.g. 'Testing', 'Refactor', 'Documentation', 'Review')")
      appendLine("- assign a recommended due date offset ('dueDayOffset': integer representing days from today, e.g. 1, 2, 3)")
      appendLine()
      appendLine("Respond with valid raw JSON matching this schema:")
      appendLine("""
        {
          "explanation": "Brief 1-sentence explanation of why these steps were added",
          "subtasks": [
            {
              "title": "Action verb + specific subtask name",
              "estimatedMinutes": 25,
              "priority": "Medium",
              "categoryTag": "Testing",
              "dueDayOffset": 1,
              "actionableNotes": "Concrete tips or key deliverables",
              "milestoneTitle": "Milestone name"
            }
          ]
        }
      """.trimIndent())
    }

    val requestJson = JSONObject().apply {
      val contentsArray = JSONArray().apply {
        put(JSONObject().apply {
          put("parts", JSONArray().apply {
            put(JSONObject().put("text", prompt))
          })
        })
      }
      put("contents", contentsArray)

      val generationConfig = JSONObject().apply {
        put("temperature", 0.4)
        put("responseMimeType", "application/json")
      }
      put("generationConfig", generationConfig)
    }

    try {
      val body = requestJson.toString().toRequestBody("application/json".toMediaType())
      val request = Request.Builder()
        .url(url)
        .post(body)
        .build()

      val response = client.newCall(request).execute()
      val responseString = response.body?.string()

      if (!response.isSuccessful || responseString.isNullOrBlank()) {
        return@withContext generateSmartLocalAdditionalSubtasks(task, existingTitles, additionalInstructions, count)
      }

      val json = JSONObject(responseString)
      val candidates = json.optJSONArray("candidates")
      val firstCandidate = candidates?.optJSONObject(0)
      val contentObj = firstCandidate?.optJSONObject("content")
      val parts = contentObj?.optJSONArray("parts")
      val rawText = parts?.optJSONObject(0)?.optString("text") ?: ""

      parseJsonBreakdown(task.id, rawText, defaultModel, task.title, task.description, task.deadlineTimestamp)
    } catch (e: Exception) {
      Log.e("GeminiBreakdown", "Error in Gemini additional subtasks", e)
      generateSmartLocalAdditionalSubtasks(task, existingTitles, additionalInstructions, count)
    }
  }

  private fun generateSmartLocalAdditionalSubtasks(
    task: com.example.data.model.Task,
    existingTitles: List<String>,
    userInstructions: String,
    count: Int
  ): BreakdownResult {
    data class LocalStep(val title: String, val minutes: Int, val priority: Priority, val categoryTag: String)

    val pool = mutableListOf<LocalStep>()
    val titleLower = task.title.lowercase()

    if (titleLower.contains("code") || titleLower.contains("app") || task.category == TaskCategory.CODING) {
      pool.add(LocalStep("Write automated integration & regression test suite", 35, Priority.HIGH, "Testing"))
      pool.add(LocalStep("Profile memory performance and optimize rendering bottlenecks", 30, Priority.MEDIUM, "Performance"))
      pool.add(LocalStep("Generate API documentation and developer setup guide", 25, Priority.LOW, "Documentation"))
      pool.add(LocalStep("Conduct security audit and input sanitization check", 25, Priority.HIGH, "Security"))
      pool.add(LocalStep("Set up CI/CD build automated verification pipeline", 30, Priority.MEDIUM, "DevOps"))
    } else if (titleLower.contains("write") || titleLower.contains("article") || titleLower.contains("report")) {
      pool.add(LocalStep("Fact-check citations, statistics and reference links", 20, Priority.HIGH, "Research"))
      pool.add(LocalStep("Format layout, headings, and high-impact visual callouts", 25, Priority.MEDIUM, "Design"))
      pool.add(LocalStep("Generate executive summary / key takeaways", 15, Priority.MEDIUM, "Writing"))
      pool.add(LocalStep("Solicit peer feedback and perform targeted revisions", 30, Priority.HIGH, "Review"))
    } else if (task.category == TaskCategory.HEALTH || titleLower.contains("workout")) {
      pool.add(LocalStep("Log workout performance metrics & heart rate recovery", 15, Priority.MEDIUM, "Tracking"))
      pool.add(LocalStep("Prepare post-activity hydration & nutritional meal", 20, Priority.HIGH, "Nutrition"))
      pool.add(LocalStep("Conduct 10-minute mobility and cool-down stretching", 10, Priority.LOW, "Recovery"))
    } else {
      pool.add(LocalStep("Conduct comprehensive milestone checkpoint review", 20, Priority.HIGH, "Review"))
      pool.add(LocalStep("Organize documentation and backup working deliverables", 15, Priority.LOW, "Documentation"))
      pool.add(LocalStep("Gather stakeholder feedback on completed outputs", 25, Priority.MEDIUM, "Feedback"))
      pool.add(LocalStep("Define post-launch retrospective & next iteration roadmap", 30, Priority.LOW, "Planning"))
    }

    // Filter out already existing titles
    val filtered = pool.filter { step ->
      existingTitles.none { existing -> existing.contains(step.title, ignoreCase = true) || step.title.contains(existing, ignoreCase = true) }
    }.ifEmpty { pool }

    val startOrder = task.subtasks.size
    val lastMilestone = task.subtasks.lastOrNull()?.milestoneTitle?.ifBlank { null } ?: "Milestone 2: Follow-up Actions"
    val rawSubtasks = filtered.take(count.coerceIn(1, filtered.size)).mapIndexed { idx, step ->
      val customTitle = if (userInstructions.isNotBlank() && idx == 0) "${step.title} ($userInstructions)" else step.title
      SubTask(
        id = UUID.randomUUID().toString(),
        taskId = task.id,
        title = customTitle,
        estimatedMinutes = step.minutes,
        orderIndex = startOrder + idx,
        milestoneTitle = lastMilestone,
        priority = step.priority,
        categoryTag = step.categoryTag,
        actionableNotes = "AI suggested addition to accelerate completion."
      )
    }

    val scheduledSubtasks = computeSubtaskDueDates(rawSubtasks, task.deadlineTimestamp)

    return BreakdownResult(
      subtasks = scheduledSubtasks,
      aiExplanation = "Added ${scheduledSubtasks.size} supplemental steps to reinforce your task execution plan with targeted due dates.",
      sourceModel = "Local Smart Planner"
    )
  }

  fun computeSubtaskDueDates(
    subtasks: List<SubTask>,
    deadlineTimestamp: Long?,
    now: Long = System.currentTimeMillis()
  ): List<SubTask> {
    if (subtasks.isEmpty()) return emptyList()

    val effectiveDeadline = if (deadlineTimestamp != null && deadlineTimestamp > now) {
      deadlineTimestamp
    } else {
      val daysSpan = when {
        subtasks.size <= 2 -> 1L
        subtasks.size <= 4 -> 2L
        subtasks.size <= 6 -> 3L
        else -> (subtasks.size / 2L).coerceIn(3L, 7L)
      }
      now + (daysSpan * 86_400_000L)
    }

    val totalSpanMillis = (effectiveDeadline - now).coerceAtLeast(3_600_000L)
    val n = subtasks.size

    return subtasks.mapIndexed { index, sub ->
      if (sub.dueDateTimestamp != null && sub.dueDateTimestamp > 0L) {
        sub
      } else {
        val fraction = ((index + 1).toDouble() / n.toDouble()).coerceIn(0.1, 1.0)
        val targetTimestamp = now + (totalSpanMillis * fraction).toLong()

        val cal = java.util.Calendar.getInstance().apply {
          timeInMillis = targetTimestamp
          set(java.util.Calendar.HOUR_OF_DAY, 17)
          set(java.util.Calendar.MINUTE, 0)
          set(java.util.Calendar.SECOND, 0)
          set(java.util.Calendar.MILLISECOND, 0)
        }

        val calculatedDueDate = cal.timeInMillis.coerceAtLeast(now + 1_800_000L)
        sub.copy(
          dueDateTimestamp = calculatedDueDate,
          scheduledStartTime = calculatedDueDate - (sub.estimatedMinutes * 60_000L),
          scheduledEndTime = calculatedDueDate
        )
      }
    }
  }

  private fun parseJsonBreakdown(
    taskId: String,
    rawJsonText: String,
    modelName: String,
    taskTitle: String,
    taskDescription: String,
    deadlineTimestamp: Long?
  ): BreakdownResult {
    return try {
      val cleanJson = rawJsonText.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
      val obj = JSONObject(cleanJson)
      val explanation = obj.optString("explanation", "Task decomposed into optimal milestones and sequential execution subtasks.")
      val categoryStr = obj.optString("category", "")
      val priorityStr = obj.optString("priority", "")
      val determinedCategory = if (categoryStr.isNotBlank()) TaskCategory.fromString(categoryStr) else inferTaskCategory(taskTitle, taskDescription)
      val determinedPriority = if (priorityStr.isNotBlank()) Priority.fromString(priorityStr) else inferTaskPriority(taskTitle, taskDescription, deadlineTimestamp)

      val list = mutableListOf<SubTask>()
      val now = System.currentTimeMillis()

      val milestonesArray = obj.optJSONArray("milestones")
      if (milestonesArray != null && milestonesArray.length() > 0) {
        var globalIndex = 0
        for (m in 0 until milestonesArray.length()) {
          val milestoneObj = milestonesArray.getJSONObject(m)
          val milestoneTitle = milestoneObj.optString("title", "Milestone ${m + 1}: Execution Phase")
          val subtasksArray = milestoneObj.optJSONArray("subtasks") ?: JSONArray()
          for (s in 0 until subtasksArray.length()) {
            val subObj = subtasksArray.getJSONObject(s)
            val title = subObj.optString("title", "Subtask ${s + 1}")
            val estMin = subObj.optInt("estimatedMinutes", 30).coerceIn(5, 480)
            val notes = subObj.optString("actionableNotes", "")
            val subPriorityStr = subObj.optString("priority", "Medium")
            val subPriority = Priority.fromString(subPriorityStr)
            val categoryTag = subObj.optString("categoryTag", inferCategoryTag(title, determinedCategory))
            val dueDayOffset = subObj.optInt("dueDayOffset", -1)
            val explicitDueDate = if (dueDayOffset >= 0) {
              val cal = java.util.Calendar.getInstance().apply {
                timeInMillis = now + (dueDayOffset * 86_400_000L)
                set(java.util.Calendar.HOUR_OF_DAY, 17)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
              }
              cal.timeInMillis.coerceAtLeast(now + 1_800_000L)
            } else null

            list.add(
              SubTask(
                id = UUID.randomUUID().toString(),
                taskId = taskId,
                title = title,
                estimatedMinutes = estMin,
                actualMinutes = 0,
                isCompleted = false,
                orderIndex = globalIndex++,
                actionableNotes = notes,
                milestoneTitle = milestoneTitle,
                priority = subPriority,
                categoryTag = categoryTag,
                dueDateTimestamp = explicitDueDate,
                scheduledStartTime = explicitDueDate?.let { it - (estMin * 60_000L) },
                scheduledEndTime = explicitDueDate
              )
            )
          }
        }
      } else {
        // Fallback: check flat subtasks array
        val subtasksArray = obj.optJSONArray("subtasks") ?: JSONArray()
        for (i in 0 until subtasksArray.length()) {
          val subObj = subtasksArray.getJSONObject(i)
          val title = subObj.optString("title", "Step ${i + 1}")
          val estMin = subObj.optInt("estimatedMinutes", 30).coerceIn(5, 480)
          val notes = subObj.optString("actionableNotes", "")
          val milestoneTitle = subObj.optString("milestoneTitle", if (i < 2) "Milestone 1: Planning & Scope" else "Milestone 2: Execution & Delivery")
          val subPriorityStr = subObj.optString("priority", "Medium")
          val subPriority = Priority.fromString(subPriorityStr)
          val categoryTag = subObj.optString("categoryTag", inferCategoryTag(title, determinedCategory))
          val dueDayOffset = subObj.optInt("dueDayOffset", -1)
          val explicitDueDate = if (dueDayOffset >= 0) {
            val cal = java.util.Calendar.getInstance().apply {
              timeInMillis = now + (dueDayOffset * 86_400_000L)
              set(java.util.Calendar.HOUR_OF_DAY, 17)
              set(java.util.Calendar.MINUTE, 0)
              set(java.util.Calendar.SECOND, 0)
              set(java.util.Calendar.MILLISECOND, 0)
            }
            cal.timeInMillis.coerceAtLeast(now + 1_800_000L)
          } else null

          list.add(
            SubTask(
              id = UUID.randomUUID().toString(),
              taskId = taskId,
              title = title,
              estimatedMinutes = estMin,
              actualMinutes = 0,
              isCompleted = false,
              orderIndex = i,
              actionableNotes = notes,
              milestoneTitle = milestoneTitle,
              priority = subPriority,
              categoryTag = categoryTag,
              dueDateTimestamp = explicitDueDate,
              scheduledStartTime = explicitDueDate?.let { it - (estMin * 60_000L) },
              scheduledEndTime = explicitDueDate
            )
          )
        }
      }

      if (list.isEmpty()) {
        generateSmartLocalBreakdown(taskId, taskTitle, taskDescription, deadlineTimestamp)
      } else {
        val fullyScheduled = computeSubtaskDueDates(list, deadlineTimestamp)
        BreakdownResult(
          subtasks = fullyScheduled,
          aiExplanation = explanation,
          sourceModel = modelName,
          determinedCategory = determinedCategory,
          determinedPriority = determinedPriority
        )
      }
    } catch (e: Exception) {
      Log.e("GeminiBreakdown", "Failed parsing JSON output", e)
      generateSmartLocalBreakdown(taskId, taskTitle, taskDescription, deadlineTimestamp)
    }
  }

  private fun inferTaskCategory(title: String, description: String): TaskCategory {
    val text = "$title $description".lowercase()
    return when {
      text.contains("code") || text.contains("app") || text.contains("bug") || text.contains("feature") ||
          text.contains("backend") || text.contains("api") || text.contains("git") || text.contains("ui") ||
          text.contains("kotlin") || text.contains("react") || text.contains("compose") -> TaskCategory.CODING
      text.contains("study") || text.contains("exam") || text.contains("learn") || text.contains("course") ||
          text.contains("homework") || text.contains("syllabus") || text.contains("research") -> TaskCategory.STUDY
      text.contains("workout") || text.contains("gym") || text.contains("run") || text.contains("marathon") ||
          text.contains("health") || text.contains("diet") || text.contains("fitness") || text.contains("doctor") -> TaskCategory.HEALTH
      text.contains("tax") || text.contains("budget") || text.contains("invest") || text.contains("finance") ||
          text.contains("bank") || text.contains("invoice") || text.contains("expense") -> TaskCategory.PROJECT
      text.contains("home") || text.contains("family") || text.contains("vacation") || text.contains("trip") ||
          text.contains("personal") || text.contains("habit") -> TaskCategory.LIFE
      else -> TaskCategory.WORK
    }
  }

  private fun inferTaskPriority(title: String, description: String, deadlineTimestamp: Long?): Priority {
    val text = "$title $description".lowercase()
    if (text.contains("urgent") || text.contains("asap") || text.contains("critical") || text.contains("emergency") || text.contains("blocking")) {
      return Priority.URGENT
    }
    if (deadlineTimestamp != null && deadlineTimestamp > System.currentTimeMillis()) {
      val diffHours = (deadlineTimestamp - System.currentTimeMillis()) / (1000 * 60 * 60)
      if (diffHours <= 24) return Priority.URGENT
      if (diffHours <= 72) return Priority.HIGH
    }
    if (text.contains("launch") || text.contains("deploy") || text.contains("exam") || text.contains("interview") || text.contains("presentation")) {
      return Priority.HIGH
    }
    return Priority.MEDIUM
  }

  private fun inferCategoryTag(title: String, parentCategory: TaskCategory): String {
    val t = title.lowercase()
    return when {
      t.contains("test") || t.contains("qa") || t.contains("verify") -> "Testing"
      t.contains("design") || t.contains("ui") || t.contains("layout") -> "Design"
      t.contains("code") || t.contains("implement") || t.contains("build") || t.contains("develop") -> "Development"
      t.contains("research") || t.contains("investigate") || t.contains("explore") -> "Research"
      t.contains("review") || t.contains("audit") || t.contains("check") -> "Review"
      t.contains("plan") || t.contains("scope") || t.contains("outline") -> "Planning"
      t.contains("deploy") || t.contains("ci/cd") || t.contains("release") -> "DevOps"
      t.contains("doc") || t.contains("write") || t.contains("guide") -> "Documentation"
      else -> parentCategory.label
    }
  }

  fun generateSmartLocalBreakdown(
    taskId: String,
    taskTitle: String,
    taskDescription: String = "",
    deadlineTimestamp: Long? = null
  ): BreakdownResult {
    data class LocalTaskDef(
      val title: String,
      val minutes: Int,
      val subPriority: Priority,
      val categoryTag: String,
      val notes: String
    )

    val determinedCategory = inferTaskCategory(taskTitle, taskDescription)
    val determinedPriority = inferTaskPriority(taskTitle, taskDescription, deadlineTimestamp)

    val titleLower = taskTitle.lowercase()
    val subtasks = mutableListOf<SubTask>()
    val deadlineNote = if (deadlineTimestamp != null && deadlineTimestamp > System.currentTimeMillis()) {
      val diffDays = ((deadlineTimestamp - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)).coerceAtLeast(1)
      " Calibrated to complete comfortably within $diffDays days allotted."
    } else ""
    var advice = "Structured into optimal sequential milestones for steady momentum.$deadlineNote"

    var globalOrder = 0

    when {
      titleLower.contains("code") || titleLower.contains("app") || titleLower.contains("feature") || titleLower.contains("bug") || determinedCategory == TaskCategory.CODING -> {
        advice = "Follow test-driven design across 3 core milestones: Architecture, Implementation, and QA.$deadlineNote"
        val milestone1 = "Milestone 1: Architecture & Setup" to listOf(
          LocalTaskDef("Define technical scope & architectural requirements", 25, Priority.HIGH, "Architecture", "Establish boundaries and contract specs."),
          LocalTaskDef("Set up branch, data models & core dependencies", 30, Priority.MEDIUM, "Setup", "Prepare local workspace & libraries.")
        )
        val milestone2 = "Milestone 2: Feature Implementation" to listOf(
          LocalTaskDef("Implement business logic and state management", 45, Priority.URGENT, "Development", "Focus on core functional flows first."),
          LocalTaskDef("Build polished UI components and error states", 40, Priority.HIGH, "Design", "Follow Material 3 design and accessibility.")
        )
        val milestone3 = "Milestone 3: Quality Verification & Release" to listOf(
          LocalTaskDef("Run unit tests, verify edge cases & code review", 30, Priority.HIGH, "Testing", "Verify regression safety and performance."),
          LocalTaskDef("Deploy or merge changes and verify in staging", 20, Priority.MEDIUM, "DevOps", "Verify deployment smoke tests.")
        )
        listOf(milestone1, milestone2, milestone3).forEach { (mTitle, steps) ->
          steps.forEach { step ->
            subtasks.add(
              SubTask(
                id = UUID.randomUUID().toString(),
                taskId = taskId,
                title = step.title,
                estimatedMinutes = step.minutes,
                orderIndex = globalOrder++,
                milestoneTitle = mTitle,
                priority = step.subPriority,
                categoryTag = step.categoryTag,
                actionableNotes = step.notes
              )
            )
          }
        }
      }
      titleLower.contains("write") || titleLower.contains("article") || titleLower.contains("report") || titleLower.contains("blog") -> {
        advice = "Organized into Research, Draft, and Revision milestones for clarity and flow.$deadlineNote"
        val milestone1 = "Milestone 1: Research & Outline" to listOf(
          LocalTaskDef("Gather research, citations & key talking points", 30, Priority.HIGH, "Research", "Compile credible source links."),
          LocalTaskDef("Create detailed section outline and flow", 20, Priority.MEDIUM, "Planning", "Structure narrative arc and main headers.")
        )
        val milestone2 = "Milestone 2: First Draft" to listOf(
          LocalTaskDef("Write first uninterrupted draft", 50, Priority.URGENT, "Writing", "Draft quickly without premature editing.")
        )
        val milestone3 = "Milestone 3: Polish & Publish" to listOf(
          LocalTaskDef("Review structure, tone, and readability", 30, Priority.HIGH, "Review", "Trim filler words and polish transitions."),
          LocalTaskDef("Final proofread, formatting and publishing", 20, Priority.MEDIUM, "Publishing", "Add visuals and publish final copy.")
        )
        listOf(milestone1, milestone2, milestone3).forEach { (mTitle, steps) ->
          steps.forEach { step ->
            subtasks.add(
              SubTask(
                id = UUID.randomUUID().toString(),
                taskId = taskId,
                title = step.title,
                estimatedMinutes = step.minutes,
                orderIndex = globalOrder++,
                milestoneTitle = mTitle,
                priority = step.subPriority,
                categoryTag = step.categoryTag,
                actionableNotes = step.notes
              )
            )
          }
        }
      }
      titleLower.contains("study") || titleLower.contains("exam") || titleLower.contains("learn") || determinedCategory == TaskCategory.STUDY -> {
        advice = "Phased into Discovery, Active Recall, and Self-Testing milestones for deep retention.$deadlineNote"
        val milestone1 = "Milestone 1: Foundations & Scope" to listOf(
          LocalTaskDef("Review syllabus and identify core knowledge gaps", 20, Priority.HIGH, "Planning", "Pinpoint weakest concept areas."),
          LocalTaskDef("Deep-dive study session into foundational concepts", 45, Priority.URGENT, "Study", "Read core theory with Feynman technique.")
        )
        val milestone2 = "Milestone 2: Active Recall Practice" to listOf(
          LocalTaskDef("Active recall practice and flashcard problem solving", 40, Priority.HIGH, "Practice", "Solve questions closed-book."),
          LocalTaskDef("Synthesize notes into one-page reference cheat sheet", 25, Priority.MEDIUM, "Summary", "Condense key formulas and mental models.")
        )
        val milestone3 = "Milestone 3: Mock Testing & Mastery" to listOf(
          LocalTaskDef("Self-assessment quiz and final consolidation", 30, Priority.HIGH, "Testing", "Simulate test conditions and review errors.")
        )
        listOf(milestone1, milestone2, milestone3).forEach { (mTitle, steps) ->
          steps.forEach { step ->
            subtasks.add(
              SubTask(
                id = UUID.randomUUID().toString(),
                taskId = taskId,
                title = step.title,
                estimatedMinutes = step.minutes,
                orderIndex = globalOrder++,
                milestoneTitle = mTitle,
                priority = step.subPriority,
                categoryTag = step.categoryTag,
                actionableNotes = step.notes
              )
            )
          }
        }
      }
      else -> {
        advice = "Sequenced into Preparation, Core Execution, and Review milestones.$deadlineNote"
        val milestone1 = "Milestone 1: Preparation & Planning" to listOf(
          LocalTaskDef("Define objective, milestones & gather prerequisites", 20, Priority.HIGH, "Planning", "Clear potential roadblocks."),
          LocalTaskDef("Execute initial foundation & setup", 30, Priority.MEDIUM, "Setup", "Prepare tools and materials.")
        )
        val milestone2 = "Milestone 2: Core Execution" to listOf(
          LocalTaskDef("Perform primary execution phase", 50, Priority.URGENT, "Execution", "Dedicate deep focus to main deliverable."),
          LocalTaskDef("Evaluate progress and resolve blockers", 25, Priority.HIGH, "Review", "Test output against success criteria.")
        )
        val milestone3 = "Milestone 3: Final Delivery & Review" to listOf(
          LocalTaskDef("Conduct quality review and mark finalized deliverables", 20, Priority.MEDIUM, "Quality", "Document completion and share outcomes.")
        )
        listOf(milestone1, milestone2, milestone3).forEach { (mTitle, steps) ->
          steps.forEach { step ->
            subtasks.add(
              SubTask(
                id = UUID.randomUUID().toString(),
                taskId = taskId,
                title = step.title,
                estimatedMinutes = step.minutes,
                orderIndex = globalOrder++,
                milestoneTitle = mTitle,
                priority = step.subPriority,
                categoryTag = step.categoryTag,
                actionableNotes = step.notes
              )
            )
          }
        }
      }
    }

    val scheduledSubtasks = computeSubtaskDueDates(subtasks, deadlineTimestamp)

    return BreakdownResult(
      subtasks = scheduledSubtasks,
      aiExplanation = advice,
      sourceModel = "Local Smart Milestone Planner",
      determinedCategory = determinedCategory,
      determinedPriority = determinedPriority
    )
  }

  suspend fun chatWithTaskCoach(
    userMessage: String,
    tasks: List<Task>,
    conversationHistory: List<ChatMessage>
  ): String = withContext(Dispatchers.IO) {
    val apiKey = try {
      BuildConfig.GEMINI_API_KEY
    } catch (e: Throwable) {
      ""
    }

    if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
      Log.d("GeminiBreakdown", "No active API key found, generating smart local task coach response")
      return@withContext generateSmartLocalChatResponse(userMessage, tasks)
    }

    val url = "https://generativelanguage.googleapis.com/v1beta/models/$defaultModel:generateContent?key=$apiKey"

    val prompt = buildString {
      appendLine("You are TaskLogic AI, an elite productivity strategist, task execution coach, and supportive accountability partner.")
      appendLine("Your purpose is to help the user conquer their tasks, defeat procrastination, structure time effectively, and maintain peak momentum.")
      appendLine()
      appendLine("=== CURRENT USER TASKS & PROGRESS ===")
      if (tasks.isEmpty()) {
        appendLine("No tasks currently recorded in the workspace.")
      } else {
        val completedCount = tasks.count { it.isFullyCompleted }
        val totalCount = tasks.size
        val pendingSubtasks = tasks.flatMap { it.subtasks }.count { !it.isCompleted }
        appendLine("Overview: $completedCount of $totalCount tasks fully completed. $pendingSubtasks sub-tasks pending execution.")
        appendLine()
        tasks.forEachIndexed { i, task ->
          val status = if (task.isFullyCompleted) "DONE" else "${task.completedSubtasksCount}/${task.totalSubtasksCount} steps completed"
          val deadlineStr = task.formattedDeadline()?.let { " [Due: $it]" } ?: ""
          appendLine("${i + 1}. [${task.priority.name}] [${task.category.label}] \"${task.title}\" ($status)$deadlineStr")
          if (task.description.isNotBlank()) appendLine("   Context: ${task.description}")
          task.subtasks.forEach { sub ->
            val check = if (sub.isCompleted) "[X]" else "[ ]"
            appendLine("   - $check ${sub.title} (${sub.estimatedMinutes}m • ${sub.priority.name})")
          }
        }
      }
      appendLine("=====================================")
      appendLine()
      appendLine("Guidelines for your response:")
      appendLine("- Be concise, actionable, and encouraging. Use bullet points or numbered lists where helpful.")
      appendLine("- Ground your advice in the user's ACTUAL tasks, deadlines, and priorities above.")
      appendLine("- Highlight specific next sub-tasks to focus on right now.")
      appendLine("- If the user asks for scheduling, suggest time blocks or a step-by-step roadmap.")
      appendLine("- If the user expresses fatigue or overwhelm, provide high-impact micro-steps to restart momentum.")
      appendLine()
      if (conversationHistory.isNotEmpty()) {
        appendLine("Recent Conversation:")
        conversationHistory.takeLast(6).forEach { msg ->
          val role = if (msg.sender == ChatSender.USER) "User" else "Assistant"
          appendLine("$role: ${msg.content}")
        }
        appendLine()
      }
      appendLine("User's latest message: $userMessage")
    }

    val requestJson = JSONObject().apply {
      val contentsArray = JSONArray().apply {
        put(JSONObject().apply {
          put("parts", JSONArray().apply {
            put(JSONObject().put("text", prompt))
          })
        })
      }
      put("contents", contentsArray)

      val generationConfig = JSONObject().apply {
        put("temperature", 0.7)
      }
      put("generationConfig", generationConfig)
    }

    try {
      val body = requestJson.toString().toRequestBody("application/json".toMediaType())
      val request = Request.Builder()
        .url(url)
        .post(body)
        .build()

      val response = client.newCall(request).execute()
      val responseString = response.body?.string()

      if (!response.isSuccessful || responseString.isNullOrBlank()) {
        Log.w("GeminiBreakdown", "Chatbot API call failed code ${response.code}: $responseString")
        return@withContext generateSmartLocalChatResponse(userMessage, tasks)
      }

      val json = JSONObject(responseString)
      val candidates = json.optJSONArray("candidates")
      val firstCandidate = candidates?.optJSONObject(0)
      val contentObj = firstCandidate?.optJSONObject("content")
      val parts = contentObj?.optJSONArray("parts")
      val rawText = parts?.optJSONObject(0)?.optString("text") ?: ""

      if (rawText.isNotBlank()) rawText.trim() else generateSmartLocalChatResponse(userMessage, tasks)
    } catch (e: Exception) {
      Log.e("GeminiBreakdown", "Error in Gemini Chatbot", e)
      generateSmartLocalChatResponse(userMessage, tasks)
    }
  }

  private fun generateSmartLocalChatResponse(userMessage: String, tasks: List<Task>): String {
    val q = userMessage.lowercase()
    val incompleteTasks = tasks.filter { !it.isFullyCompleted }
    val urgentTasks = incompleteTasks.filter { it.priority == Priority.URGENT || it.priority == Priority.HIGH }
    val nextIncompleteSubtasks = incompleteTasks.flatMap { t -> t.subtasks.filter { !it.isCompleted }.map { sub -> Pair(t, sub) } }

    if (q.contains("prioritize") || q.contains("next") || q.contains("what should i") || q.contains("focus")) {
      return if (nextIncompleteSubtasks.isNotEmpty()) {
        val top = nextIncompleteSubtasks.first()
        buildString {
          appendLine("🎯 **Immediate Next Action:**")
          appendLine("Start with **\"${top.second.title}\"** (${top.second.estimatedMinutes} mins) under *${top.first.title}*.")
          appendLine()
          appendLine("💡 **Why this first?** It's tagged as **${top.second.priority.name}** priority in **${top.first.category.label}**.")
          if (nextIncompleteSubtasks.size > 1) {
            val second = nextIncompleteSubtasks[1]
            appendLine()
            appendLine("📋 **On deck right after:**")
            appendLine("• ${second.second.title} (${second.second.estimatedMinutes}m)")
          }
          appendLine()
          appendLine("Ready? Set a timer and dedicate 20 minutes of distraction-free execution!")
        }
      } else {
        "🎉 You currently have all your sub-tasks marked as completed! Create a new goal or breakdown in the Breakdown tab to get started."
      }
    }

    if (q.contains("analyze") || q.contains("workload") || q.contains("status") || q.contains("summary") || q.contains("bottleneck")) {
      val totalEstMin = incompleteTasks.sumOf { it.totalEstimatedMinutes }
      val totalHours = String.format(Locale.getDefault(), "%.1f", totalEstMin / 60f)
      return buildString {
        appendLine("📊 **Workload Analysis & Insights:**")
        appendLine("• **Active Tasks:** ${incompleteTasks.size} (${tasks.count { it.isFullyCompleted }} completed)")
        appendLine("• **Pending Subtasks:** ${nextIncompleteSubtasks.size} steps")
        appendLine("• **Estimated Remaining Effort:** ~$totalHours hours total ($totalEstMin minutes)")
        appendLine()
        if (urgentTasks.isNotEmpty()) {
          appendLine("⚠️ **High-Priority Watchlist:**")
          urgentTasks.forEach { t ->
            val due = t.formattedDeadline()?.let { " (Due: $it)" } ?: ""
            appendLine("• **${t.title}** [${t.priority.name}]$due — ${t.completedSubtasksCount}/${t.totalSubtasksCount} steps completed")
          }
          appendLine()
        }
        appendLine("💡 **Recommendation:** Cluster similar category tasks into uninterrupted 45-minute focus sprints to avoid context switching.")
      }
    }

    if (q.contains("motivat") || q.contains("procrastinat") || q.contains("stuck") || q.contains("tired") || q.contains("overwhelm")) {
      return buildString {
        appendLine("🔥 **Momentum Blueprint:**")
        appendLine("When resistance feels high, remember: *action creates motivation, not the other way around.*")
        appendLine()
        appendLine("1. **The 2-Minute Rule:** Pick just ONE tiny step and commit to 2 minutes only.")
        if (nextIncompleteSubtasks.isNotEmpty()) {
          appendLine("2. **Your easiest micro-step:** \"${nextIncompleteSubtasks.first().second.title}\"")
        }
        appendLine("3. **Remove friction:** Silence notifications and open your primary tool.")
        appendLine()
        appendLine("You've got this! Start with step 1 right now.")
      }
    }

    if (q.contains("schedule") || q.contains("today") || q.contains("plan")) {
      return buildString {
        appendLine("🗓️ **Suggested Time-Blocked Schedule for Today:**")
        var currentSlotMinutes = 0
        nextIncompleteSubtasks.take(4).forEachIndexed { index, (task, sub) ->
          val block = when (index) {
            0 -> "Block 1 (Morning Focus)"
            1 -> "Block 2 (Deep Work)"
            2 -> "Block 3 (Mid-Day Push)"
            else -> "Block 4 (Wrap Up)"
          }
          appendLine("• **$block:** ${sub.title} (~${sub.estimatedMinutes}m • *${task.title}*)")
          currentSlotMinutes += sub.estimatedMinutes
        }
        appendLine()
        appendLine("⏱️ Total scheduled execution time: ~$currentSlotMinutes mins. Take 5-10 min breaks between blocks!")
      }
    }

    return buildString {
      appendLine("Hello! I am your **TaskLogic AI Coach**.")
      appendLine("I have analyzed your workspace with **${incompleteTasks.size} active tasks** and **${nextIncompleteSubtasks.size} pending steps**.")
      appendLine()
      appendLine("Here is how I can assist you today:")
      appendLine("• ⚡ **Prioritize:** Tell me what you want to achieve today")
      appendLine("• 📊 **Analyze:** Ask for a workload breakdown and bottleneck scan")
      appendLine("• 🎯 **Break Down:** Ask for strategy advice on any specific task")
      appendLine("• ⏰ **Schedule:** Ask to create an optimal day schedule")
      appendLine()
      appendLine("How can I support your productivity right now?")
    }
  }
}

