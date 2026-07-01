package com.sleepadvisor.agent

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.sleepadvisor.data.DiaryRepository
import com.sleepadvisor.data.MemoryRepository
import com.sleepadvisor.data.SettingsRepository
import com.sleepadvisor.domain.model.ChatMessage
import com.sleepadvisor.domain.model.SleepDiary
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

object ExtractionAgent {
    private val gson = Gson()

    private const val SYSTEM_PROMPT = """You are a silent data extraction assistant for a sleep tracking app. Your ONLY job is to analyze the user's messages and extract:
1. Any sleep diary data (bed time, wake time, sleep latency, awakenings, etc.)
2. Any personal facts about the user (age, medications, lifestyle, goals).

If you find sleep data, call the tool `save_sleep_diary`.
If you find personal facts, call `update_human_memory` with the COMPLETE updated JSON.

If the user's message contains no new extractable data, respond with just 'NO_DATA'. Do NOT converse with the user. Do NOT ask questions. Only extract and save.

Current Human Profile:
{human_profile}

Today's Date: {today_date}
"""

    private val TOOLS_JSON = """[
      {
        "type": "function",
        "function": {
          "name": "save_sleep_diary",
          "description": "Log a single daily sleep diary entry for a specific date.",
          "parameters": {
            "type": "object",
            "properties": {
              "date": { "type": "string", "description": "The date of the morning waking up, in yyyy-MM-dd format." },
              "bed_time": { "type": "string", "description": "Time the user got into bed, in HH:mm format." },
              "light_out_time": { "type": "string", "description": "Time lights were turned out to sleep, in HH:mm format." },
              "latency_mins": { "type": "integer", "description": "Minutes it took to fall asleep." },
              "awakenings": { "type": "integer", "description": "Number of nighttime awakenings." },
              "awake_mins": { "type": "integer", "description": "Total minutes spent awake during the night." },
              "wake_time": { "type": "string", "description": "Final wake up time, in HH:mm format." },
              "out_of_bed_time": { "type": "string", "description": "Time got out of bed, in HH:mm format." },
              "quality": { "type": "integer", "description": "Sleep quality rating (1-5, where 5 is excellent)." },
              "alertness": { "type": "integer", "description": "Daytime alertness rating (1-5, where 5 is fully alert)." },
              "medications": { "type": "string", "description": "Name and dosage of any sleep medications taken, or 'None'." },
              "notes": { "type": "string", "description": "Any additional comments or Negative Sleep Thoughts logged." }
            },
            "required": ["date"]
          }
        }
      },
      {
        "type": "function",
        "function": {
          "name": "update_human_memory",
          "description": "Update the Core Human Profile JSON to record persistent background details.",
          "parameters": {
            "type": "object",
            "properties": {
              "new_content": { "type": "string", "description": "The full, updated Human Profile JSON string containing age, insomnia_duration, symptoms, medications, sleep_goals, and lifestyle traits." }
            },
            "required": ["new_content"]
          }
        }
      }
    ]"""

    fun runExtraction(
        context: Context,
        userMessage: String,
        recentHistory: List<ChatMessage>,
        diaryRepo: DiaryRepository,
        memoryRepo: MemoryRepository
    ): String {
        val settingsRepo = SettingsRepository(context)
        val apiKey = settingsRepo.getApiKey()
        if (apiKey.isEmpty()) return "NO_DATA"

        try {
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val humanProfile = memoryRepo.getHumanMemory()
            val formattedSystemPrompt = SYSTEM_PROMPT
                .replace("{human_profile}", humanProfile)
                .replace("{today_date}", todayStr)

            val messages = JsonArray().apply {
                add(JsonObject().apply {
                    addProperty("role", "system")
                    addProperty("content", formattedSystemPrompt)
                })
                // Add last 10 messages for context
                val startIdx = (recentHistory.size - 10).coerceAtLeast(0)
                for (i in startIdx until recentHistory.size) {
                    val m = recentHistory[i]
                    add(JsonObject().apply {
                        addProperty("role", if (m.sender == "user") "user" else "assistant")
                        addProperty("content", m.message)
                    })
                }
                // Add current message
                add(JsonObject().apply {
                    addProperty("role", "user")
                    addProperty("content", userMessage)
                })
            }

            val tools = JsonParser.parseString(TOOLS_JSON).asJsonArray
            var loopCount = 0
            var finalResult = "NO_DATA"
            var parsedToolRuns = mutableListOf<String>()

            while (loopCount < 3) {
                val response = DeepSeekClient.callChatCompletion(apiKey, messages, tools, temperature = 0.0)
                val choice = response.getAsJsonArray("choices").get(0).asJsonObject
                val messageObj = choice.getAsJsonObject("message")
                val content = if (messageObj.has("content") && !messageObj.get("content").isJsonNull) messageObj.get("content").asString else ""

                if (content.isNotEmpty()) {
                    finalResult = content
                }

                if (messageObj.has("tool_calls") && !messageObj.get("tool_calls").isJsonNull) {
                    val toolCalls = messageObj.getAsJsonArray("tool_calls")
                    messages.add(messageObj) // append assistant message with tool calls

                    for (i in 0 until toolCalls.size()) {
                        val tc = toolCalls.get(i).asJsonObject
                        val callId = tc.get("id").asString
                        val functionObj = tc.getAsJsonObject("function")
                        val toolName = functionObj.get("name").asString
                        val toolArgsStr = functionObj.get("arguments").asString
                        val toolArgs = JsonParser.parseString(toolArgsStr).asJsonObject

                        var toolResult = ""
                        when (toolName) {
                            "save_sleep_diary" -> {
                                val diary = SleepDiary.fromJson(toolArgs)
                                diaryRepo.upsertDiary(diary)
                                toolResult = "Sleep diary logged successfully."
                                parsedToolRuns.add("save_sleep_diary:${diary.date}")
                            }
                            "update_human_memory" -> {
                                val newContent = toolArgs.get("new_content").asString
                                memoryRepo.updateHumanMemory(newContent)
                                toolResult = "Human memory updated."
                                parsedToolRuns.add("update_human_memory")
                            }
                        }

                        // Append tool result
                        messages.add(JsonObject().apply {
                            addProperty("role", "tool")
                            addProperty("tool_call_id", callId)
                            addProperty("name", toolName)
                            addProperty("content", toolResult)
                        })
                    }
                    loopCount++
                } else {
                    break
                }
            }

            return if (parsedToolRuns.isNotEmpty()) {
                "EXTRACTED:" + parsedToolRuns.joinToString(",")
            } else {
                finalResult
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return "ERROR"
        }
    }
}
