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
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

object AdvisoryAgent {
    private val gson = Gson()

    private const val SYSTEM_PROMPT_TEMPLATE = """You are a CBT-I Certified Sleep Advisor, based on Gregg Jacobs' book "Say Good Night to Insomnia". Your role is to act as a long-term sleep advisor, guiding the user through their cognitive behavioral therapy for insomnia.

The user is 53 years old. You will guide them through these phases:
1. **Initial Assessment (CBT Week -1)**: Conduct a comprehensive initial interview to establish their age, sleep history (latency, awakenings, duration), lifestyle (exercise, caffeine, alcohol), and goals. Use `update_human_memory` to save these details. If the user reports any specific nights of sleep during this assessment (such as last night's sleep), immediately call `save_sleep_diary` to log it so they can see and edit it in their Diary tab. Tell them they need to complete a daily 60-Second Sleep Diary for 7 days to establish their baseline.
2. **Baseline Week (CBT Week 0)**: The user is in their baseline logging phase. Ask them about their sleep, record diaries using the `save_sleep_diary` tool, and keep encouraging them. After they have logged 7 diaries, move them to Week 1.
3. **Week 1: Changing Thoughts (CBT Week 1)**: Teach cognitive restructuring. Challenge Negative Sleep Thoughts (NSTs) like the "8-hour sleep myth", explaining that 7 hours is average and healthier, and that sleep needs vary. Replace NSTs with Positive Sleep Thoughts (PSTs).
4. **Week 2: Sleep-Promoting Habits (CBT Week 2)**: Enforce sleep restriction (limiting time-in-bed to average baseline sleep duration, min 5 hours) and stimulus control rules (rising at same time daily, only bed when sleepy, get out of bed if awake 20+ mins, no naps).
5. **Week 3: Stimulus Control (CBT Week 3)**: Review compliance, reinforce rules (strict rise times, no bed unless sleepy), and adjust the sleep window based on weekly efficiency.
6. **Week 4: Relaxation (CBT Week 4)**: Introduce the relaxation response. Discuss diaphragmatic breathing, progressive muscle relaxation, or mindfulness, and reinforce daily practice.
7. **Week 5: Stress Reduction (CBT Week 5)**: Address daytime stress. Teach cognitive restructuring for daily worries, distinguishing productive vs unproductive worry, and establishing "worry time".
8. **Week 6: Sleep-Enhancing Attitudes (CBT Week 6)**: Focus on maintaining long-term gains, preventing relapse, and handling temporary sleep disruptions without anxiety.

=== MEMORY BLOCK ===
{human_mem}

=== PERSONA GUIDELINES ===
{persona_mem}

=== SLEEP STATS SUMMARY ===
{sleep_stats}

=== COGNITIVE INSTRUCTIONS ===
1. You are a Letta-style memory agent. You have the ability to read and write to your long-term memory.
2. When you learn standalone long-term facts or specific events (e.g., "User started a new job on 2026-06-29"), save them to Archival Memory using `insert_archival_memory`.
3. If you need sleep science details, search Gregg Jacobs' book using the `search_book` tool.
4. The Extraction Agent has already processed and saved any daily sleep logs the user reported in this turn. You do NOT need to extract or log sleep data yourself. Focus purely on coaching, encouragement, and CBT-I guidance. If the user reported sleep data, acknowledge it by saying "I have logged this in your diary" and provide coaching feedback or prompt for the next step.
5. Always ensure the user's current CBT program week is updated using `update_cbt_week` when they transition to the next week.
6. CRITICAL: When conducting assessments, diagnostic interviews, or obtaining sleep/lifestyle details, ask exactly ONE question at a time. Wait for the user's answer before asking the next question.

=== VOICE CHAT STYLE ===
- Keep your responses concise, warm, caring, and conversational.
- Do not repeat long instructions. Keep it engaging for text-to-speech.
"""

    private val TOOLS_JSON = """[
      {
        "type": "function",
        "function": {
          "name": "search_book",
          "description": "Search the CBT-I sleep guide book 'Say Good Night to Insomnia' for relevant clinical passages.",
          "parameters": {
            "type": "object",
            "properties": {
              "query": { "type": "string", "description": "Key phrases or words to search in the book." }
            },
            "required": ["query"]
          }
        }
      },
      {
        "type": "function",
        "function": {
          "name": "search_archival_memory",
          "description": "Search archival memory logs for previous user history milestones.",
          "parameters": {
            "type": "object",
            "properties": {
              "query": { "type": "string", "description": "Search substring." }
            },
            "required": ["query"]
          }
        }
      },
      {
        "type": "function",
        "function": {
          "name": "insert_archival_memory",
          "description": "Insert a new permanent fact or event log into Archival Memory.",
          "parameters": {
            "type": "object",
            "properties": {
              "content": { "type": "string", "description": "The exact fact or log to store." }
            },
            "required": ["content"]
          }
        }
      },
      {
        "type": "function",
        "function": {
          "name": "get_sleep_stats",
          "description": "Retrieve user's rolling historical sleep stats (efficiency, duration, quality averages).",
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
          "description": "Advance or change the user's active CBT-I program week.",
          "parameters": {
            "type": "object",
            "properties": {
              "week_num": { "type": "integer", "description": "The target week number (-1 to 6)." }
            },
            "required": ["week_num"]
          }
        }
      }
    ]"""

    fun runAdvisory(
        context: Context,
        userMessage: String,
        recentHistory: List<ChatMessage>,
        memoryRepo: MemoryRepository,
        diaryRepo: DiaryRepository
    ): String {
        val settingsRepo = SettingsRepository(context)
        val apiKey = settingsRepo.getApiKey()
        if (apiKey.isEmpty()) return "Please enter your DeepSeek API key in Settings."

        try {
            val humanMem = memoryRepo.getHumanMemory()
            val personaMem = memoryRepo.getPersonaMemory()
            val sleepStats = diaryRepo.calculateStats().toJsonString()
            val systemPrompt = SYSTEM_PROMPT_TEMPLATE
                .replace("{human_mem}", humanMem)
                .replace("{persona_mem}", personaMem)
                .replace("{sleep_stats}", sleepStats)

            val messages = JsonArray().apply {
                add(JsonObject().apply {
                    addProperty("role", "system")
                    addProperty("content", systemPrompt)
                })
                // Add last 20 messages for context
                for (m in recentHistory) {
                    add(JsonObject().apply {
                        addProperty("role", if (m.sender == "user") "user" else "assistant")
                        addProperty("content", m.message)
                    })
                }
                // Add current user message
                add(JsonObject().apply {
                    addProperty("role", "user")
                    addProperty("content", userMessage)
                })
            }

            val tools = JsonParser.parseString(TOOLS_JSON).asJsonArray
            var loopCount = 0
            var finalResponse = "I'm sorry, I encountered an issue processing your request."

            while (loopCount < 5) {
                val response = DeepSeekClient.callChatCompletion(apiKey, messages, tools, temperature = 1.0)
                val choice = response.getAsJsonArray("choices").get(0).asJsonObject
                val messageObj = choice.getAsJsonObject("message")
                val content = if (messageObj.has("content") && !messageObj.get("content").isJsonNull) messageObj.get("content").asString else ""

                if (content.isNotEmpty()) {
                    finalResponse = content
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
                            "search_book" -> {
                                val query = toolArgs.get("query").asString
                                val chunks = RagSearch.searchBook(context, query)
                                toolResult = "Found book passages:\n" + chunks.joinToString("\n---\n")
                            }
                            "search_archival_memory" -> {
                                val query = toolArgs.get("query").asString
                                val matches = memoryRepo.searchArchival(query)
                                toolResult = "Search results in Archival Memory:\n" + matches.joinToString("\n") { it.toString() }
                            }
                            "insert_archival_memory" -> {
                                val contentArg = toolArgs.get("content").asString
                                memoryRepo.insertArchival(contentArg)
                                toolResult = "Successfully inserted into Archival Memory."
                            }
                            "get_sleep_stats" -> {
                                toolResult = diaryRepo.calculateStats().toJsonString()
                            }
                            "update_cbt_week" -> {
                                val weekNum = toolArgs.get("week_num").asInt
                                memoryRepo.updateCbtWeek(weekNum)
                                toolResult = "Successfully updated CBT week to $weekNum."
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

            return finalResponse
        } catch (e: Exception) {
            e.printStackTrace()
            return "Connection error: ${e.message}. Please check your connection and API key."
        }
    }
}
