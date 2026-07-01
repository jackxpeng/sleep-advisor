package com.sleepadvisor.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sleepadvisor.data.DiaryRepository
import com.sleepadvisor.domain.model.SleepDiary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

class DiaryViewModel(application: Application) : AndroidViewModel(application) {
    private val diaryRepo = DiaryRepository(application)

    private val _diaries = MutableStateFlow<List<SleepDiary>>(emptyList())
    val diaries: StateFlow<List<SleepDiary>> = _diaries.asStateFlow()

    // Form fields state
    val date = MutableStateFlow("")
    val bedTime = MutableStateFlow("23:00")
    val lightOutTime = MutableStateFlow("23:15")
    val latencyMins = MutableStateFlow("30")
    val awakenings = MutableStateFlow("1")
    val awakeMins = MutableStateFlow("15")
    val wakeTime = MutableStateFlow("06:30")
    val outOfBedTime = MutableStateFlow("06:45")
    val quality = MutableStateFlow(3)
    val alertness = MutableStateFlow(3)
    val medications = MutableStateFlow("None")
    val notes = MutableStateFlow("")

    private val _efficiency = MutableStateFlow(0.0)
    val efficiency: StateFlow<Double> = _efficiency.asStateFlow()

    private val _durationMins = MutableStateFlow(0)
    val durationMins: StateFlow<Int> = _durationMins.asStateFlow()

    private val _tibMins = MutableStateFlow(0)
    val tibMins: StateFlow<Int> = _tibMins.asStateFlow()

    init {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        date.value = todayStr
        loadDiaries()
        prefillToday()
    }

    fun loadDiaries() {
        _diaries.value = diaryRepo.getAllDiaries()
    }

    fun prefillToday() {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val entry = diaryRepo.getDiaryForDate(todayStr)
        if (entry != null) {
            selectEntry(entry)
        }
    }

    fun selectEntry(diary: SleepDiary) {
        date.value = diary.date
        bedTime.value = diary.bedTime
        lightOutTime.value = diary.lightOutTime
        latencyMins.value = diary.latencyMins.toString()
        awakenings.value = diary.awakenings.toString()
        awakeMins.value = diary.awakeMins.toString()
        wakeTime.value = diary.wakeTime
        outOfBedTime.value = diary.outOfBedTime
        quality.value = diary.quality
        alertness.value = diary.alertness
        medications.value = diary.medications
        notes.value = diary.notes
        recalculate()
    }

    fun setField(field: String, value: Any) {
        when (field) {
            "date" -> date.value = value as String
            "bedTime" -> bedTime.value = value as String
            "lightOutTime" -> lightOutTime.value = value as String
            "latencyMins" -> latencyMins.value = value as String
            "awakenings" -> awakenings.value = value as String
            "awakeMins" -> awakeMins.value = value as String
            "wakeTime" -> wakeTime.value = value as String
            "outOfBedTime" -> outOfBedTime.value = value as String
            "quality" -> quality.value = value as Int
            "alertness" -> alertness.value = value as Int
            "medications" -> medications.value = value as String
            "notes" -> notes.value = value as String
        }
        recalculate()
    }

    fun recalculate() {
        val dummyDiary = SleepDiary(
            date = date.value,
            bedTime = bedTime.value,
            lightOutTime = lightOutTime.value,
            latencyMins = latencyMins.value.toIntOrNull() ?: 0,
            awakenings = awakenings.value.toIntOrNull() ?: 0,
            awakeMins = awakeMins.value.toIntOrNull() ?: 0,
            wakeTime = wakeTime.value,
            outOfBedTime = outOfBedTime.value,
            quality = quality.value,
            alertness = alertness.value,
            medications = medications.value,
            notes = notes.value
        )
        _efficiency.value = dummyDiary.sleepEfficiency()
        _durationMins.value = dummyDiary.sleepDurationMins()
        _tibMins.value = dummyDiary.timeInBedMins()
    }

    fun handleSubmit(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val diary = SleepDiary(
                date = date.value,
                bedTime = bedTime.value,
                lightOutTime = lightOutTime.value,
                latencyMins = latencyMins.value.toIntOrNull() ?: 0,
                awakenings = awakenings.value.toIntOrNull() ?: 0,
                awakeMins = awakeMins.value.toIntOrNull() ?: 0,
                wakeTime = wakeTime.value,
                outOfBedTime = outOfBedTime.value,
                quality = quality.value,
                alertness = alertness.value,
                medications = medications.value,
                notes = notes.value
            )
            diaryRepo.upsertDiary(diary)
            loadDiaries()
            onSuccess()
        }
    }
}
