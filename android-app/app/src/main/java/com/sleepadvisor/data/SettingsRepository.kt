package com.sleepadvisor.data

import android.content.Context

class SettingsRepository(private val context: Context) {

    fun getApiKey(): String {
        return Preferences.getString(context, Preferences.KEY_API_KEY, "")
    }

    fun setApiKey(key: String) {
        Preferences.putString(context, Preferences.KEY_API_KEY, key)
    }

    fun getVoiceRate(): Float {
        val rateStr = Preferences.getString(context, Preferences.KEY_VOICE_RATE, "1.0")
        return rateStr.toFloatOrNull() ?: 1.0f
    }

    fun setVoiceRate(rate: Float) {
        Preferences.putString(context, Preferences.KEY_VOICE_RATE, rate.toString())
    }

    fun getVoicePitch(): Float {
        val pitchStr = Preferences.getString(context, Preferences.KEY_VOICE_PITCH, "1.0")
        return pitchStr.toFloatOrNull() ?: 1.0f
    }

    fun setVoicePitch(pitch: Float) {
        Preferences.putString(context, Preferences.KEY_VOICE_PITCH, pitch.toString())
    }

    fun resetAllData() {
        context.getSharedPreferences("sleep_advisor_prefs", Context.MODE_PRIVATE).edit().clear().apply()
        Preferences.initialize(context)
    }
}
