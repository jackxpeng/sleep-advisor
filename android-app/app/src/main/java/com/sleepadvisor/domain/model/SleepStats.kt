package com.sleepadvisor.domain.model

data class SleepStats(
    val totalLogs: Int = 0,
    val avgDurationMins: Double = 0.0,
    val avgEfficiency: Double = 0.0,
    val avgLatencyMins: Double = 0.0,
    val avgAwakenings: Double = 0.0,
    val avgQuality: Double = 0.0,
    val avgAlertness: Double = 0.0
) {

    fun toJsonString(): String = buildString {
        append("{")
        append("\"totalLogs\":$totalLogs,")
        append("\"avgDurationMins\":%.1f,".format(avgDurationMins))
        append("\"avgEfficiency\":%.1f,".format(avgEfficiency))
        append("\"avgLatencyMins\":%.1f,".format(avgLatencyMins))
        append("\"avgAwakenings\":%.1f,".format(avgAwakenings))
        append("\"avgQuality\":%.1f,".format(avgQuality))
        append("\"avgAlertness\":%.1f".format(avgAlertness))
        append("}")
    }

    fun formatDuration(): String {
        val hours = (avgDurationMins / 60).toInt()
        val mins = (avgDurationMins % 60).toInt()
        return "${hours}h ${mins}m"
    }

    companion object {

        fun calculate(diaries: List<SleepDiary>): SleepStats {
            if (diaries.isEmpty()) return SleepStats()
            val n = diaries.size
            return SleepStats(
                totalLogs = n,
                avgDurationMins = diaries.sumOf { it.sleepDurationMins() }.toDouble() / n,
                avgEfficiency = diaries.sumOf { it.sleepEfficiency() } / n,
                avgLatencyMins = diaries.sumOf { it.latencyMins }.toDouble() / n,
                avgAwakenings = diaries.sumOf { it.awakenings }.toDouble() / n,
                avgQuality = diaries.sumOf { it.quality }.toDouble() / n,
                avgAlertness = diaries.sumOf { it.alertness }.toDouble() / n
            )
        }
    }
}
