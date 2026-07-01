package com.sleepadvisor.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.sleepadvisor.domain.model.ChatMessage
import com.sleepadvisor.domain.model.CbtWeek
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

class MemoryRepository(private val context: Context) {
    private val gson = Gson()

    fun getHumanMemory(): String {
        return Preferences.getString(context, Preferences.KEY_HUMAN_MEMORY, "{}")
    }

    fun updateHumanMemory(json: String) {
        Preferences.putString(context, Preferences.KEY_HUMAN_MEMORY, json)
    }

    fun getPersonaMemory(): String {
        return Preferences.getString(context, Preferences.KEY_PERSONA_MEMORY, "{}")
    }

    fun updatePersonaMemory(json: String) {
        Preferences.putString(context, Preferences.KEY_PERSONA_MEMORY, json)
    }

    fun getArchivalMemories(): List<JsonObject> {
        val archJson = Preferences.getString(context, Preferences.KEY_ARCHIVAL_MEMORIES, "[]")
        return try {
            val arr = gson.fromJson(archJson, JsonArray::class.java) ?: JsonArray()
            val list = mutableListOf<JsonObject>()
            for (i in 0 until arr.size()) {
                list.add(arr.get(i).asJsonObject)
            }
            list
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun insertArchival(content: String) {
        val list = getArchivalMemories().toMutableList()
        val item = JsonObject().apply {
            addProperty("content", content)
            val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
            addProperty("timestamp", dateStr)
        }
        list.add(item)
        saveArchivalList(list)
    }

    fun deleteArchival(index: Int) {
        val list = getArchivalMemories().toMutableList()
        if (index in 0 until list.size) {
            list.removeAt(index)
            saveArchivalList(list)
        }
    }

    fun searchArchival(query: String): List<JsonObject> {
        val q = query.lowercase(Locale.US)
        return getArchivalMemories().filter {
            val content = it.get("content")?.asString ?: ""
            content.lowercase(Locale.US).contains(q)
        }
    }

    private fun saveArchivalList(list: List<JsonObject>) {
        val arr = JsonArray()
        list.forEach { arr.add(it) }
        Preferences.putString(context, Preferences.KEY_ARCHIVAL_MEMORIES, gson.toJson(arr))
    }

    fun getChatHistory(): List<ChatMessage> {
        val historyJson = Preferences.getString(context, Preferences.KEY_CHAT_HISTORY, "[]")
        return try {
            val arr = gson.fromJson(historyJson, JsonArray::class.java) ?: JsonArray()
            val list = mutableListOf<ChatMessage>()
            for (i in 0 until arr.size()) {
                list.add(ChatMessage.fromJson(arr.get(i).asJsonObject))
            }
            list
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun appendMessage(msg: ChatMessage) {
        val list = getChatHistory().toMutableList()
        list.add(msg)
        saveChatHistory(list)
    }

    fun getRecentHistory(count: Int): List<ChatMessage> {
        val list = getChatHistory()
        val startIndex = (list.size - count).coerceAtLeast(0)
        return list.subList(startIndex, list.size)
    }

    fun clearChatHistory() {
        Preferences.putString(context, Preferences.KEY_CHAT_HISTORY, "[]")
    }

    private fun saveChatHistory(list: List<ChatMessage>) {
        val arr = JsonArray()
        list.forEach { arr.add(ChatMessage.toJson(it)) }
        Preferences.putString(context, Preferences.KEY_CHAT_HISTORY, gson.toJson(arr))
    }

    fun getCbtWeek(): Int {
        val weekStr = Preferences.getString(context, Preferences.KEY_CBT_WEEK, "-1")
        return weekStr.toIntOrNull() ?: -1
    }

    fun getCbtWeekEnum(): CbtWeek {
        return CbtWeek.fromWeekNum(getCbtWeek())
    }

    fun updateCbtWeek(week: Int) {
        Preferences.putString(context, Preferences.KEY_CBT_WEEK, week.toString())
        
        val humanMemJson = getHumanMemory()
        try {
            val obj = JsonParser.parseString(humanMemJson).asJsonObject
            val cbtWeekEnum = CbtWeek.fromWeekNum(week)
            
            val progress = if (obj.has("cbt_progress") && !obj.get("cbt_progress").isJsonNull) {
                obj.get("cbt_progress").asJsonObject
            } else {
                JsonObject()
            }
            progress.addProperty("current_week", week)
            progress.addProperty("current_week_description", cbtWeekEnum.displayName)
            obj.add("cbt_progress", progress)
            
            updateHumanMemory(gson.toJson(obj))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
