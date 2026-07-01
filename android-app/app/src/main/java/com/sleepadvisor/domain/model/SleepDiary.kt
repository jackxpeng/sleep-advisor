package com.sleepadvisor.domain.model

import com.google.gson.JsonObject

data class SleepDiary(
    val date: String,
    val bedTime: String = "23:00",
    val lightOutTime: String = "23:15",
    val latencyMins: Int = 0,
    val awakenings: Int = 0,
    val awakeMins: Int = 0,
    val wakeTime: String = "06:30",
    val outOfBedTime: String = "06:45",
    val quality: Int = 3,
    val alertness: Int = 3,
    val medications: String = "None",
    val notes: String = ""
) {

    fun timeInBedMins(): Int = timeDiffMins(bedTime, outOfBedTime)

    fun sleepDurationMins(): Int = maxOf(0, timeInBedMins() - latencyMins - awakeMins)

    fun sleepEfficiency(): Double {
        val tib = timeInBedMins()
        return if (tib > 0) (sleepDurationMins().toDouble() / tib) * 100.0 else 0.0
    }

    companion object {

        fun fromJson(json: JsonObject): SleepDiary {
            fun str(key: String, default: String): String =
                if (json.has(key) && !json.get(key).isJsonNull) json.get(key).asString else default

            fun int(key: String, default: Int): Int =
                if (json.has(key) && !json.get(key).isJsonNull) json.get(key).asInt else default

            return SleepDiary(
                date = str("date", ""),
                bedTime = str("bedTime", "23:00"),
                lightOutTime = str("lightOutTime", "23:15"),
                latencyMins = int("latencyMins", 0),
                awakenings = int("awakenings", 0),
                awakeMins = int("awakeMins", 0),
                wakeTime = str("wakeTime", "06:30"),
                outOfBedTime = str("outOfBedTime", "06:45"),
                quality = int("quality", 3),
                alertness = int("alertness", 3),
                medications = str("medications", "None"),
                notes = str("notes", "")
            )
        }

        fun toJson(diary: SleepDiary): JsonObject = JsonObject().apply {
            addProperty("date", diary.date)
            addProperty("bedTime", diary.bedTime)
            addProperty("lightOutTime", diary.lightOutTime)
            addProperty("latencyMins", diary.latencyMins)
            addProperty("awakenings", diary.awakenings)
            addProperty("awakeMins", diary.awakeMins)
            addProperty("wakeTime", diary.wakeTime)
            addProperty("outOfBedTime", diary.outOfBedTime)
            addProperty("quality", diary.quality)
            addProperty("alertness", diary.alertness)
            addProperty("medications", diary.medications)
            addProperty("notes", diary.notes)
        }
    }
}
private fun timeDiffMins(start: String, end: String): Int {
    try {
        val startParts = start.split(":")
        val endParts = end.split(":")
        if (startParts.size != 2 || endParts.size != 2) return 0
        
        val sh = startParts[0].trim().toIntOrNull() ?: return 0
        val sm = startParts[1].trim().toIntOrNull() ?: return 0
        val eh = endParts[0].trim().toIntOrNull() ?: return 0
        val em = endParts[1].trim().toIntOrNull() ?: return 0
        
        val startMins = sh * 60 + sm
        val endMins = eh * 60 + em
        val diff = endMins - startMins
        return if (diff < 0) diff + 24 * 60 else diff
    } catch (e: Exception) {
        return 0
    }
}
