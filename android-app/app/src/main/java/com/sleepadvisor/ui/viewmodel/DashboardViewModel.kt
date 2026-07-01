package com.sleepadvisor.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.sleepadvisor.data.DiaryRepository
import com.sleepadvisor.data.MemoryRepository
import com.sleepadvisor.data.Preferences
import com.sleepadvisor.domain.model.CbtWeek
import com.sleepadvisor.domain.model.SleepStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val diaryRepo = DiaryRepository(application)
    private val memoryRepo = MemoryRepository(application)

    private val _sleepStats = MutableStateFlow(SleepStats())
    val sleepStats: StateFlow<SleepStats> = _sleepStats.asStateFlow()

    private val _cbtWeek = MutableStateFlow(CbtWeek.INITIAL_ASSESSMENT)
    val cbtWeek: StateFlow<CbtWeek> = _cbtWeek.asStateFlow()

    private val _isDiaryDoneToday = MutableStateFlow(false)
    val isDiaryDoneToday: StateFlow<Boolean> = _isDiaryDoneToday.asStateFlow()

    private val _isBreathingDoneToday = MutableStateFlow(false)
    val isBreathingDoneToday: StateFlow<Boolean> = _isBreathingDoneToday.asStateFlow()

    private val _progressPercent = MutableStateFlow(0f)
    val progressPercent: StateFlow<Float> = _progressPercent.asStateFlow()

    init {
        reloadDashboard()
    }

    fun reloadDashboard() {
        _sleepStats.value = diaryRepo.calculateStats()
        _cbtWeek.value = memoryRepo.getCbtWeekEnum()

        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val todayDiary = diaryRepo.getDiaryForDate(todayStr)
        _isDiaryDoneToday.value = todayDiary != null

        val breathingDone = Preferences.getString(getApplication(), "relaxation_done_$todayStr", "false") == "true"
        _isBreathingDoneToday.value = breathingDone

        var completedTasks = 0f
        if (todayDiary != null) completedTasks += 0.5f
        if (breathingDone) completedTasks += 0.5f
        _progressPercent.value = completedTasks
    }
}
