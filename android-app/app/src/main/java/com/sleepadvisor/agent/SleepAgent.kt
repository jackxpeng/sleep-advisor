package com.sleepadvisor.agent

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.sleepadvisor.data.Preferences
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object SleepAgent {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private const val SYSTEM_PROMPT_TEMPLATE = """You are a CBT-I Certified Sleep Advisor, based on Gregg Jacobs' book "Say Good Night to Insomnia". Your role is to act as a long-term sleep advisor, guiding the user through their cognitive behavioral therapy for insomnia.

The user is 53 years old. You will guide them through these phases:
1. **Initial Assessment (CBT Week -1)**: Conduct a comprehensive initial interview to establish their age, sleep history (latency, awakenings, duration), lifestyle (exercise, caffeine, alcohol), and goals. Use `update_human_memory` to save these details. If the user reports any specific nights of sleep during this assessment (such as last night's sleep), immediately call `save_sleep_diary` to log it so they can see and edit it in their Diary tab. Tell them they need to complete a daily 60-Second Sleep Diary for 7 days to establish their baseline.
2. **Baseline Week (CBT Week 0)**: The user is in their baseline logging phase. Ask them about their sleep, record diaries using the `save_sleep_diary` tool, and keep encouraging them. After they have logged 7 diaries, move them to Week 1.
3. **Week 1: Changing Thoughts (CBT Week 1)**: Teach cognitive restructuring. Challenge Negative Sleep Thoughts (NSTs) like the "8-hour sleep myth", explaining that 7 hours is average and healthier, and that sleep needs vary. Replace NSTs with Positive Sleep Thoughts (PSTs).
4. **Week 2: Sleep-Promoting Habits (CBT Week 2)**: Enforce sleep restriction (limiting time-in-bed to average baseline sleep duration, min 5 hours) and stimulus control rules (rising at same time daily, only bed when sleepy, get out of bed if awake 20+ mins, no naps).
5. **Week 3: Lifestyle & Environment (CBT Week 3)**: Review daily exercise (moderate, afternoon), light exposure (morning bright light), avoiding caffeine/alcohol, and sleep environment.
6. **Week 4: Relaxation Response (CBT Week 4)**: Introduce the Relaxation Response (abdominal breathing/breathing exercises, progressive muscle relaxation). Encourage them to use the app's Breathing Room.
7. **Week 5: Thinking Away Stress (CBT Week 5)**: Cognitive restructuring for daytime stress and negative self-talk.
8. **Week 6: Sleep-Enhancing Attitudes (CBT Week 6)**: Focus on optimism, commitment, control, challenge, and maintaining sleep habits long term.

=== LETTA CORE MEMORY ===
[CORE MEMORY - HUMAN PROFILE]
{human_mem}

[CORE MEMORY - ASSISTANT PERSONA]
{persona_mem}
=========================

=== RECALL MEMORY (RECENT HISTORY) ===
You have a memory of recent conversation below.

=== COGNITIVE INSTRUCTIONS ===
1. You are a Letta-style memory agent. You have the ability to read and write to your long-term memory.
2. When you learn new personal facts about the user (e.g. their age, sleep latency, awakenings, medications, exercise, caffeine, goals), you MUST immediately call `update_human_memory` to update the Human Profile JSON. Do not wait until the end of the conversation or assessment. Update the JSON incrementally after each user response so that no details are lost when the short-term chat window rolls over.
3. When you learn standalone long-term facts or specific events (e.g., "User started a new job on 2026-06-29"), save them to Archival Memory using `insert_archival_memory`.
4. If you need sleep science details, search Gregg Jacobs' book using the `search_book` tool.
5. If the user reports a night of sleep, use `save_sleep_diary` to log it. Make sure you get all details (bed time, light out time, latency, awakenings, awake mins, wake time, out of bed time, quality 1-5, alertness 1-5, medications, and negative thoughts/notes). You must explicitly ask the user for any missing details (such as negative sleep thoughts/NSTs or medications) before executing the log. Do not leave the notes or medications fields blank or default them without asking.
6. Always ensure the user's current CBT program week is updated using `update_cbt_week` when they transition to next week.
7. CRITICAL: When conducting assessments, diagnostic interviews, or obtaining sleep/lifestyle details, ask exactly ONE question at a time. Wait for the user's answer before asking the next question.

=== VOICE CHAT STYLE ===
The user is speaking to you via a Voice Chat interface. Respond in a warm, caring, confident tone.
IMPORTANT: Keep your verbal responses CONCISE, clear, and easy to listen to (avoid long bullet lists or code blocks in voice mode unless asked, keep paragraphs to 1-3 short sentences).
CRITICAL: Ask exactly ONE question at a time. Never bundle multiple questions together or ask for multiple metrics at once. Wait for the user to answer each question before asking the next one.
"""

    private val AGENT_TOOLS_JSON = """[
      {
        "type": "function",
        "function": {
          "name": "update_human_memory",
          "description": "Overwrite/update the core human profile memory JSON. Use this when the user reveals personal profile details like age, sleep patterns, lifestyle, or goals.",
          "parameters": {
            "type": "object",
            "properties": {
              "new_content": {
                "type": "string",
                "description": "The updated JSON string representing the human profile core memory."
              }
            },
            "required": ["new_content"]
          }
        }
      },
      {
        "type": "function",
        "function": {
          "name": "update_persona_memory",
          "description": "Update the advisor's internal persona guidelines or interaction rules in core memory.",
          "parameters": {
            "type": "object",
            "properties": {
              "new_content": {
                "type": "string",
                "description": "The updated persona memory text or JSON."
              }
            },
            "required": ["new_content"]
          }
        }
      },
      {
        "type": "function",
        "function": {
          "name": "insert_archival_memory",
          "description": "Insert a new long-term fact or notable event into the archival memory database.",
          "parameters": {
            "type": "object",
            "properties": {
              "content": {
                "type": "string",
                "description": "A single clear fact or memory statement about the user."
              }
            },
            "required": ["content"]
          }
        }
      },
      {
        "type": "function",
        "function": {
          "name": "search_archival_memory",
          "description": "Search the archival memory database for past facts or events about the user.",
          "parameters": {
            "type": "object",
            "properties": {
              "query": {
                "type": "string",
                "description": "The search keyword or topic to locate in long term memory."
              }
            },
            "required": ["query"]
          }
        }
      },
      {
        "type": "function",
        "function": {
          "name": "search_book",
          "description": "Search Dr. Gregg Jacobs' book 'Say Good Night to Insomnia' for scientifically proven CBT-I methods, facts, guidelines, or quotes.",
          "parameters": {
            "type": "object",
            "properties": {
              "query": {
                "type": "string",
                "description": "The concept, chapter keyword, or query to search inside the book (e.g. '8-hour myth', 'sleep restriction', 'caffeine')."
              }
            },
            "required": ["query"]
          }
        }
      },
      {
        "type": "function",
        "function": {
          "name": "save_sleep_diary",
          "description": "Log a completed 60-Second Sleep Diary entry for a specific date.",
          "parameters": {
            "type": "object",
            "properties": {
              "date": { "type": "string", "description": "The date of the sleep log in YYYY-MM-DD format (e.g., '2026-06-29')." },
              "bed_time": { "type": "string", "description": "Time the user got into bed (HH:MM standard 24hr format, e.g. '23:15')." },
              "light_out_time": { "type": "string", "description": "Time the user turned off the lights to sleep (HH:MM, e.g. '23:30')." },
              "latency_mins": { "type": "integer", "description": "Minutes it took to fall asleep (integer)." },
              "awakenings": { "type": "integer", "description": "Number of times the user woke up during the night (integer)." },
              "awake_mins": { "type": "integer", "description": "Total minutes spent awake during the night after initial sleep (integer)." },
              "wake_time": { "type": "string", "description": "Time of final awakening in the morning (HH:MM, e.g. '06:30')." },
              "out_of_bed_time": { "type": "string", "description": "Time the user got out of bed (HH:MM, e.g. '06:45')." },
              "quality": { "type": "integer", "description": "Subjective sleep quality score from 1 (poor) to 5 (excellent)." },
              "alertness": { "type": "integer", "description": "Subjective daytime alertness score from 1 (fatigued) to 5 (refreshed)." },
              "medications": { "type": "string", "description": "Name and dosage of any sleep medications taken, or 'None'." },
              "notes": { "type": "string", "description": "Any additional comments or Negative Sleep Thoughts logged." }
            },
            "required": ["date", "bed_time", "light_out_time", "latency_mins", "awakenings", "awake_mins", "wake_time", "out_of_bed_time", "quality", "alertness", "medications", "notes"]
          }
        }
      },
      {
        "type": "function",
        "function": {
          "name": "get_sleep_stats",
          "description": "Retrieve rolling averages and total count of sleep logs to calculate sleep efficiency.",
          "parameters": {
            "type": "object",
            "properties": {}
          }
        }
      },
      {
        "type": "function",
        "function": {
          "name": "update_cbt_week",
          "description": "Advance or change the user's active CBT-I program week (e.g. -1 for interview, 0 for baseline, 1 for Week 1... 6 for Week 6).",
          "parameters": {
            "type": "object",
            "properties": {
              "week_num": {
                "type": "integer",
                "description": "The week index number (-1, 0, 1, 2, 3, 4, 5, 6)."
              }
            },
            "required": ["week_num"]
          }
        }
      }
    ]"""

    fun runAgentTurn(context: Context, userMessage: String, onStateChange: () -> Unit = {}): String {
        val apiKey = Preferences.getString(context, Preferences.KEY_API_KEY)
        if (apiKey.isEmpty() || !apiKey.startsWith("sk-")) {
            return "Please enter your DeepSeek API key in the Settings tab to begin."
        }

        Preferences.initialize(context)

        // Save user message to history
        val historyJson = Preferences.getString(context, Preferences.KEY_CHAT_HISTORY, "[]")
        val historyArray = gson.fromJson(historyJson, JsonArray::class.java) ?: JsonArray()
        
        val userTurn = JsonObject().apply {
            addProperty("sender", "user")
            addProperty("message", userMessage)
            addProperty("timestamp", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date()))
        }
        historyArray.add(userTurn)
        Preferences.putString(context, Preferences.KEY_CHAT_HISTORY, gson.toJson(historyArray))

        // Build messages array for api
        val apiMessages = JsonArray()
        val humanMem = Preferences.getString(context, Preferences.KEY_HUMAN_MEMORY, "{}")
        val personaMem = Preferences.getString(context, Preferences.KEY_PERSONA_MEMORY, "{}")
        val systemPrompt = SYSTEM_PROMPT_TEMPLATE
            .replace("{human_mem}", humanMem)
            .replace("{persona_mem}", personaMem)

        apiMessages.add(JsonObject().apply {
            addProperty("role", "system")
            addProperty("content", systemPrompt)
        })

        // Map recent 20 messages of history (Recall Memory context)
        val listSize = historyArray.size()
        val startIndex = (listSize - 20).coerceAtLeast(0)
        for (i in startIndex until listSize) {
            val h = historyArray.get(i).asJsonObject
            val sender = h.get("sender").asString
            val message = h.get("message").asString
            apiMessages.add(JsonObject().apply {
                addProperty("role", if (sender == "user") "user" else "assistant")
                addProperty("content", message)
            })
        }

        val url = "https://api.deepseek.com/chat/completions"
        val toolsArray = gson.fromJson(AGENT_TOOLS_JSON, JsonArray::class.java)

        val maxLoops = 5
        var loopCount = 0

        while (loopCount < maxLoops) {
            loopCount++

            val requestBodyJson = JsonObject().apply {
                addProperty("model", "deepseek-chat")
                add("messages", apiMessages)
                add("tools", toolsArray)
                addProperty("tool_choice", "auto")
            }

            val request = Request.Builder()
                .url(url)
                .post(gson.toJson(requestBodyJson).toRequestBody(JSON_MEDIA_TYPE))
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer $apiKey")
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        val errBody = response.body?.string() ?: ""
                        System.err.println("DeepSeek API Error: ${response.code} $errBody")
                        return "I'm having trouble connecting to my sleep knowledge network. Please check your network and API key."
                    }

                    val resString = response.body?.string() ?: return "API returned empty response."
                    val resObj = gson.fromJson(resString, JsonObject::class.java)
                    val choices = resObj.getAsJsonArray("choices")
                    if (choices == null || choices.size() == 0) return "No response text found."
                    
                    val choiceObj = choices.get(0).asJsonObject
                    val messageObj = choiceObj.getAsJsonObject("message")
                    val assistantContent = messageObj.get("content")?.let { if (it.isJsonNull) "" else it.asString } ?: ""

                    val toolCalls = messageObj.getAsJsonArray("tool_calls")
                    if (toolCalls == null || toolCalls.size() == 0) {
                        // Success - standard text response
                        val finalHistoryJson = Preferences.getString(context, Preferences.KEY_CHAT_HISTORY, "[]")
                        val finalHistoryArray = gson.fromJson(finalHistoryJson, JsonArray::class.java) ?: JsonArray()
                        val assistantTurn = JsonObject().apply {
                            addProperty("sender", "advisor")
                            addProperty("message", assistantContent)
                            addProperty("timestamp", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date()))
                        }
                        finalHistoryArray.add(assistantTurn)
                        Preferences.putString(context, Preferences.KEY_CHAT_HISTORY, gson.toJson(finalHistoryArray))
                        onStateChange()
                        return assistantContent
                    }

                    // Handle tool calls
                    apiMessages.add(messageObj) // Add assistant's tool-call prompt

                    for (i in 0 until toolCalls.size()) {
                        val toolCall = toolCalls.get(i).asJsonObject
                        val toolId = toolCall.get("id").asString
                        val functionObj = toolCall.getAsJsonObject("function")
                        val toolName = functionObj.get("name").asString
                        val toolArgsString = functionObj.get("arguments").asString
                        val toolArgs = gson.fromJson(toolArgsString, JsonObject::class.java)

                        println("Executing tool in Kotlin: $toolName with arguments: $toolArgsString")
                        var toolResult = ""

                        try {
                            when (toolName) {
                                "update_human_memory" -> {
                                    val content = toolArgs.get("new_content").asString
                                    Preferences.putString(context, Preferences.KEY_HUMAN_MEMORY, content)
                                    toolResult = "Core Human Memory successfully updated."
                                }
                                "update_persona_memory" -> {
                                    val content = toolArgs.get("new_content").asString
                                    Preferences.putString(context, Preferences.KEY_PERSONA_MEMORY, content)
                                    toolResult = "Core Persona Memory successfully updated."
                                }
                                "insert_archival_memory" -> {
                                    val content = toolArgs.get("content").asString
                                    val archivalJson = Preferences.getString(context, Preferences.KEY_ARCHIVAL_MEMORIES, "[]")
                                    val archivalArray = gson.fromJson(archivalJson, JsonArray::class.java) ?: JsonArray()
                                    val newItem = JsonObject().apply {
                                        addProperty("content", content)
                                        addProperty("created_at", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date()))
                                    }
                                    archivalArray.add(newItem)
                                    Preferences.putString(context, Preferences.KEY_ARCHIVAL_MEMORIES, gson.toJson(archivalArray))
                                    toolResult = "Fact committed to Archival Memory: '$content'"
                                }
                                "search_archival_memory" -> {
                                    val q = toolArgs.get("query").asString.lowercase()
                                    val archivalJson = Preferences.getString(context, Preferences.KEY_ARCHIVAL_MEMORIES, "[]")
                                    val archivalArray = gson.fromJson(archivalJson, JsonArray::class.java) ?: JsonArray()
                                    val matches = mutableListOf<String>()
                                    for (j in 0 until archivalArray.size()) {
                                        val item = archivalArray.get(j).asJsonObject
                                        val contentVal = item.get("content").asString
                                        val dateVal = item.get("created_at").asString
                                        if (contentVal.lowercase().contains(q)) {
                                            matches.add("- [$dateVal]: $contentVal")
                                        }
                                    }
                                    toolResult = if (matches.isNotEmpty()) {
                                        matches.joinToString("\n")
                                    } else {
                                        "No matches found in long term memories."
                                    }
                                }
                                "search_book" -> {
                                    val q = toolArgs.get("query").asString
                                    val matches = RagSearch.searchBook(context, q)
                                    toolResult = if (matches.isNotEmpty()) gson.toJson(matches) else "No matches found in the book."
                                }
                                "save_sleep_diary" -> {
                                    val diariesJson = Preferences.getString(context, Preferences.KEY_DIARIES, "[]")
                                    val diariesArray = gson.fromJson(diariesJson, JsonArray::class.java) ?: JsonArray()
                                    val dateVal = toolArgs.get("date").asString
                                    
                                    // Remove old entry for same date if exists
                                    var existingIndex = -1
                                    for (j in 0 until diariesArray.size()) {
                                        if (diariesArray.get(j).asJsonObject.get("date").asString == dateVal) {
                                            existingIndex = j
                                            break
                                        }
                                    }
                                    if (existingIndex != -1) {
                                        diariesArray.set(existingIndex, toolArgs)
                                    } else {
                                        diariesArray.add(toolArgs)
                                    }
                                    Preferences.putString(context, Preferences.KEY_DIARIES, gson.toJson(diariesArray))
                                    toolResult = "Sleep diary logged successfully."
                                }
                                "get_sleep_stats" -> {
                                    toolResult = calculateSleepStats(context)
                                }
                                "update_cbt_week" -> {
                                    val weekNum = toolArgs.get("week_num").asInt
                                    Preferences.putString(context, Preferences.KEY_CBT_WEEK, weekNum.toString())
                                    
                                    val humanJson = Preferences.getString(context, Preferences.KEY_HUMAN_MEMORY, "{}")
                                    val humanObj = gson.fromJson(humanJson, JsonObject::class.java) ?: JsonObject()
                                    val cbtProg = humanObj.getAsJsonObject("cbt_progress") ?: JsonObject()
                                    cbtProg.addProperty("current_week", weekNum)
                                    
                                    val desc = when (weekNum) {
                                        -1 -> "Initial Assessment Interview"
                                        0 -> "Baseline Logging Week"
                                        1 -> "Week 1: Changing Thoughts"
                                        2 -> "Week 2: Sleep Habits"
                                        3 -> "Week 3: Lifestyle & Environment"
                                        4 -> "Week 4: Relaxation Response"
                                        5 -> "Week 5: Thinking Away Stress"
                                        6 -> "Week 6: Sleep Attitudes"
                                        else -> "Maintenance"
                                    }
                                    cbtProg.addProperty("current_week_description", desc)
                                    humanObj.add("cbt_progress", cbtProg)
                                    Preferences.putString(context, Preferences.KEY_HUMAN_MEMORY, gson.toJson(humanObj))
                                    
                                    toolResult = "CBT Program week updated to $weekNum."
                                }
                                else -> {
                                    toolResult = "Error: Tool '$toolName' not found."
                                }
                            }
                        } catch (err: Exception) {
                            toolResult = "Error executing tool: ${err.message}"
                        }

                        apiMessages.add(JsonObject().apply {
                            addProperty("role", "tool")
                            addProperty("tool_call_id", toolId)
                            addProperty("name", toolName)
                            addProperty("content", toolResult)
                        })
                    }

                    onStateChange()
                }
            } catch (e: IOException) {
                e.printStackTrace()
                return "My processing encountered an issue while communicating. Let's try again in a moment."
            }
        }

        return "I processed your request, but reached my tool-loop limit. How else can I support you tonight?"
    }

    fun calculateSleepStats(context: Context): String {
        val diariesJson = Preferences.getString(context, Preferences.KEY_DIARIES, "[]")
        val diariesArray = gson.fromJson(diariesJson, JsonArray::class.java) ?: JsonArray()
        
        if (diariesArray.size() == 0) {
            val emptyStats = JsonObject().apply {
                addProperty("total_logs", 0)
                addProperty("average_duration_mins", 0)
                addProperty("average_efficiency", 0.0)
                addProperty("average_latency_mins", 0)
                addProperty("average_awakenings", 0.0)
                addProperty("average_quality", 0.0)
                addProperty("average_alertness", 0.0)
            }
            return gson.toJson(emptyStats)
        }

        fun timeDiffMins(t1: String, t2: String): Int {
            return try {
                val parts1 = t1.split(":").map { it.toInt() }
                val parts2 = t2.split(":").map { it.toInt() }
                var mins = (parts2[0] * 60 + parts2[1]) - (parts1[0] * 60 + parts1[1])
                if (mins < 0) mins += 24 * 60 // Crossed midnight
                mins
            } catch (e: Exception) {
                0
            }
        }

        var totalDuration = 0
        var totalEfficiency = 0.0
        var totalLatency = 0
        var totalAwakenings = 0
        var totalQuality = 0.0
        var totalAlertness = 0.0
        var validDiaries = 0

        for (j in 0 until diariesArray.size()) {
            val d = diariesArray.get(j).asJsonObject
            val bed = d.get("light_out_time")?.let { if (it.isJsonNull) "" else it.asString } 
                ?: d.get("bed_time")?.let { if (it.isJsonNull) "" else it.asString } 
                ?: ""
            val out = d.get("out_of_bed_time")?.let { if (it.isJsonNull) "" else it.asString } ?: ""
            
            val tib = timeDiffMins(bed, out)
            if (tib <= 0) continue

            val latency = d.get("latency_mins")?.let { if (it.isJsonNull) 0 else it.asInt } ?: 0
            val awake = d.get("awake_mins")?.let { if (it.isJsonNull) 0 else it.asInt } ?: 0
            val actualSleep = tib - latency - awake
            val eff = if (tib > 0) (actualSleep.toDouble() / tib) * 100.0 else 0.0

            totalDuration += if (actualSleep >= 0) actualSleep else 0
            totalEfficiency += eff
            totalLatency += latency
            totalAwakenings += d.get("awakenings")?.let { if (it.isJsonNull) 0 else it.asInt } ?: 0
            totalQuality += d.get("quality")?.let { if (it.isJsonNull) 0.0 else it.asDouble } ?: 0.0
            totalAlertness += d.get("alertness")?.let { if (it.isJsonNull) 0.0 else it.asDouble } ?: 0.0
            validDiaries++
        }

        if (validDiaries == 0) {
            val emptyStats = JsonObject().apply {
                addProperty("total_logs", diariesArray.size())
                addProperty("average_duration_mins", 0)
                addProperty("average_efficiency", 0.0)
                addProperty("average_latency_mins", 0)
                addProperty("average_awakenings", 0.0)
                addProperty("average_quality", 0.0)
                addProperty("average_alertness", 0.0)
            }
            return gson.toJson(emptyStats)
        }

        val stats = JsonObject().apply {
            addProperty("total_logs", diariesArray.size())
            addProperty("average_duration_mins", Math.round(totalDuration.toDouble() / validDiaries).toInt())
            addProperty("average_efficiency", Math.round((totalEfficiency / validDiaries) * 10) / 10.0)
            addProperty("average_latency_mins", Math.round(totalLatency.toDouble() / validDiaries).toInt())
            addProperty("average_awakenings", Math.round((totalAwakenings.toDouble() / validDiaries) * 10) / 10.0)
            addProperty("average_quality", Math.round((totalQuality / validDiaries) * 10) / 10.0)
            addProperty("average_alertness", Math.round((totalAlertness / validDiaries) * 10) / 10.0)
        }
        return gson.toJson(stats)
    }
}
