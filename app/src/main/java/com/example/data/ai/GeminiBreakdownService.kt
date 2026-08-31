package com.example.data.ai

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.Priority
import com.example.data.model.SubTask
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
        appendLine("CRITICAL SCHEDULING CONSTRAINT: Structure all milestones and subtasks so the total estimated effort and sequencing fit comfortably and realistically within this $diffDays-day allotted window.")
      } else {
        appendLine("Determine the optimal number of milestones and subtasks for complete, successful execution.")
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
    val subtasks = filtered.take(count.coerceIn(1, filtered.size)).mapIndexed { idx, step ->
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

    return BreakdownResult(
      subtasks = subtasks,
      aiExplanation = "Added ${subtasks.size} supplemental steps to reinforce your task execution plan.",
      sourceModel = "Local Smart Planner"
    )
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
                categoryTag = categoryTag
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
              categoryTag = categoryTag
            )
          )
        }
      }

      if (list.isEmpty()) {
        generateSmartLocalBreakdown(taskId, taskTitle, taskDescription, deadlineTimestamp)
      } else {
        BreakdownResult(
          subtasks = list,
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

    return BreakdownResult(
      subtasks = subtasks,
      aiExplanation = advice,
      sourceModel = "Local Smart Milestone Planner",
      determinedCategory = determinedCategory,
      determinedPriority = determinedPriority
    )
  }
}
