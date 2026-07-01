package com.sleepadvisor.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.sleepadvisor.data.DiaryRepository
import com.sleepadvisor.data.MemoryRepository
import com.sleepadvisor.data.SettingsRepository
import com.sleepadvisor.domain.model.SleepDiary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Calendar

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepo = SettingsRepository(application)
    private val diaryRepo = DiaryRepository(application)
    private val memoryRepo = MemoryRepository(application)

    private val _apiKey = MutableStateFlow("")
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    private val _voiceRate = MutableStateFlow(1.0f)
    val voiceRate: StateFlow<Float> = _voiceRate.asStateFlow()

    private val _voicePitch = MutableStateFlow(1.0f)
    val voicePitch: StateFlow<Float> = _voicePitch.asStateFlow()

    init {
        _apiKey.value = settingsRepo.getApiKey()
        _voiceRate.value = settingsRepo.getVoiceRate()
        _voicePitch.value = settingsRepo.getVoicePitch()
    }

    fun setApiKey(key: String) {
        _apiKey.value = key
        settingsRepo.setApiKey(key)
    }

    fun setVoiceRate(rate: Float) {
        _voiceRate.value = rate
        settingsRepo.setVoiceRate(rate)
    }

    fun setVoicePitch(pitch: Float) {
        _voicePitch.value = pitch
        settingsRepo.setVoicePitch(pitch)
    }

    fun resetAllData() {
        settingsRepo.resetAllData()
        // Reload values
        _apiKey.value = settingsRepo.getApiKey()
        _voiceRate.value = settingsRepo.getVoiceRate()
        _voicePitch.value = settingsRepo.getVoicePitch()
    }

    fun loadMockData(onSuccess: () -> Unit) {
        viewModelScope.launch {
            // Seed 7 diaries for the last 7 days
            val cal = Calendar.getInstance()
            val df = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            
            // Delete all existing first to prevent overlap
            val existing = diaryRepo.getAllDiaries()
            existing.forEach { diaryRepo.deleteDiary(it.date) }

            val mockLogs = listOf(
                // dateOffset, bedtime, lightsout, latency, awakenings, awakeMins, wake, outOfBed, quality, alertness
                Triple(7, Pair("23:00", "23:15"), Triple(30, 2, 20)),
                Triple(6, Pair("22:45", "23:00"), Triple(45, 3, 30)),
                Triple(5, Pair("23:15", "23:30"), Triple(15, 1, 10)),
                Triple(4, Pair("23:00", "23:00"), Triple(25, 2, 15)),
                Triple(3, Pair("23:30", "23:45"), Triple(20, 1, 10)),
                Triple(2, Pair("22:50", "23:00"), Triple(35, 2, 25)),
                Triple(1, Pair("23:10", "23:20"), Triple(10, 0, 0))
            )

            mockLogs.forEach { log ->
                val offset = log.first
                val times = log.second
                val metrics = log.third
                
                val logCal = cal.clone() as Calendar
                logCal.add(Calendar.DAY_OF_YEAR, -offset)
                val logDate = df.format(logCal.time)

                val wakeCal = logCal.clone() as Calendar
                // Let's assume wake up is roughly 7 hours later
                val diary = SleepDiary(
                    date = logDate,
                    bedTime = times.first,
                    lightOutTime = times.second,
                    latencyMins = metrics.first,
                    awakenings = metrics.second,
                    awakeMins = metrics.third,
                    wakeTime = "06:30",
                    outOfBedTime = "06:45",
                    quality = (3..5).random(),
                    alertness = (3..5).random(),
                    medications = "None",
                    notes = "Mock diary seeded for training baseline data."
                )
                diaryRepo.upsertDiary(diary)
            }

            // Seed Core Memory human profile
            val seededHuman = """{
  "age": 53,
  "insomnia_duration": "5 years",
  "symptoms": "Difficulty staying asleep, waking up tired",
  "medications": "None",
  "sleep_goals": "Establish consistent schedules, eliminate awakenings",
  "lifestyle": {
    "exercise": "Walking, 3 times a week",
    "caffeine": "1 cup in morning",
    "alcohol": "Occasional glass of wine"
  },
  "cbt_progress": {
    "current_week": 1,
    "current_week_description": "Week 1: Changing Thoughts",
    "sleep_window": "Not set",
    "average_sleep_duration": 395,
    "average_sleep_efficiency": 82.5
  }
}"""
            memoryRepo.updateHumanMemory(seededHuman)
            memoryRepo.updateCbtWeek(1)
            onSuccess()
        }
    }
}
