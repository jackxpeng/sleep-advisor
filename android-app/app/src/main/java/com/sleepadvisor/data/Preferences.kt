package com.sleepadvisor.data

import android.content.Context
import android.content.SharedPreferences

object Preferences {
    private const val PREFS_NAME = "sleep_advisor_prefs"
    
    const val KEY_API_KEY = "deepseek_api_key"
    const val KEY_HUMAN_MEMORY = "core_memory_human"
    const val KEY_PERSONA_MEMORY = "core_memory_persona"
    const val KEY_ARCHIVAL_MEMORIES = "archival_memories"
    const val KEY_DIARIES = "sleep_diaries"
    const val KEY_CHAT_HISTORY = "chat_history"
    const val KEY_CBT_WEEK = "cbt_week"
    const val KEY_VOICE_RATE = "voice_rate"
    const val KEY_VOICE_PITCH = "voice_pitch"

    private const val DEFAULT_HUMAN = """{
  "age": 53,
  "insomnia_duration": "Unknown",
  "symptoms": "Insomnia",
  "medications": "Unknown",
  "sleep_goals": "Improve sleep quality, reduce nighttime awakenings, establish consistency",
  "lifestyle": {
    "exercise": "Unknown",
    "caffeine": "Unknown",
    "alcohol": "Unknown"
  },
  "cbt_progress": {
    "current_week": -1,
    "current_week_description": "Initial Assessment Interview",
    "sleep_window": "Not set",
    "average_sleep_duration": 0,
    "average_sleep_efficiency": 0.0
  }
}"""

    private const val DEFAULT_PERSONA = """{
  "name": "Sleep Advisor",
  "role": "CBT-I Specialist",
  "style": "deep, caring, confident, structured, encouraging, empathetic",
  "rules": [
    "Guide the user through the 6-week CBT-I program based on Gregg Jacobs' book.",
    "Ensure the user maintains a consistent wake-up time.",
    "Implement and enforce sleep restriction (time-in-bed limit) starting from Week 2.",
    "Help the user identify and challenge negative sleep thoughts (NSTs).",
    "Frequently reference relevant concepts from the book like 'core sleep', '8-hour myth', 'relaxation response'.",
    "Actively use memory tools (update_human_memory, insert_archival_memory) to persist important details.",
    "Keep responses highly engaging, concise, and conversational for voice playback."
  ]
}"""

    private val BUNDLED_API_KEY = ApiKey.DEEPSEEK_API_KEY

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun initialize(context: Context) {
        val prefs = getPrefs(context)
        val editor = prefs.edit()
        
        if (!prefs.contains(KEY_API_KEY)) {
            editor.putString(KEY_API_KEY, BUNDLED_API_KEY)
        }
        if (!prefs.contains(KEY_HUMAN_MEMORY)) {
            editor.putString(KEY_HUMAN_MEMORY, DEFAULT_HUMAN)
        }
        if (!prefs.contains(KEY_PERSONA_MEMORY)) {
            editor.putString(KEY_PERSONA_MEMORY, DEFAULT_PERSONA)
        }
        if (!prefs.contains(KEY_ARCHIVAL_MEMORIES)) {
            editor.putString(KEY_ARCHIVAL_MEMORIES, "[]")
        }
        if (!prefs.contains(KEY_DIARIES)) {
            editor.putString(KEY_DIARIES, "[]")
        }
        if (!prefs.contains(KEY_CHAT_HISTORY)) {
            editor.putString(KEY_CHAT_HISTORY, "[]")
        }
        if (!prefs.contains(KEY_CBT_WEEK)) {
            editor.putString(KEY_CBT_WEEK, "-1")
        }
        if (!prefs.contains(KEY_VOICE_RATE)) {
            editor.putString(KEY_VOICE_RATE, "1.0")
        }
        if (!prefs.contains(KEY_VOICE_PITCH)) {
            editor.putString(KEY_VOICE_PITCH, "1.0")
        }
        editor.apply()
    }

    fun getString(context: Context, key: String, default: String = ""): String {
        return getPrefs(context).getString(key, default) ?: default
    }

    fun putString(context: Context, key: String, value: String) {
        getPrefs(context).edit().putString(key, value).apply()
    }

    fun clearChatHistory(context: Context) {
        putString(context, KEY_CHAT_HISTORY, "[]")
    }
}
