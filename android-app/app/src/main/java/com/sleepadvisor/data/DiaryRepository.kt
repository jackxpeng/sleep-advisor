package com.sleepadvisor.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.sleepadvisor.domain.model.SleepDiary
import com.sleepadvisor.domain.model.SleepStats
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

class DiaryRepository(private val context: Context) {
    private val gson = Gson()

    fun getAllDiaries(): List<SleepDiary> {
        val diariesJson = Preferences.getString(context, Preferences.KEY_DIARIES, "[]")
        return try {
            val arr = gson.fromJson(diariesJson, JsonArray::class.java) ?: JsonArray()
            val list = mutableListOf<SleepDiary>()
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            
            for (i in 0 until arr.size()) {
                val item = arr.get(i).asJsonObject
                val diary = SleepDiary.fromJson(item)
                // Filter out invalid future dates
                if (diary.date <= todayStr && diary.date.length == 10) {
                    list.add(diary)
                }
            }
            // Sort by date descending
            list.sortByDescending { it.date }
            list
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun getDiaryForDate(date: String): SleepDiary? {
        return getAllDiaries().find { it.date == date }
    }

    fun upsertDiary(diary: SleepDiary) {
        val list = getAllDiaries().toMutableList()
        // Remove existing by date if exists
        list.removeAll { it.date == diary.date }
        list.add(diary)
        saveList(list)
    }

    fun deleteDiary(date: String) {
        val list = getAllDiaries().toMutableList()
        list.removeAll { it.date == date }
        saveList(list)
    }

    fun calculateStats(): SleepStats {
        return SleepStats.calculate(getAllDiaries())
    }

    private fun saveList(list: List<SleepDiary>) {
        val arr = JsonArray()
        list.forEach { diary ->
            arr.add(SleepDiary.toJson(diary))
        }
        Preferences.putString(context, Preferences.KEY_DIARIES, gson.toJson(arr))
    }
}
