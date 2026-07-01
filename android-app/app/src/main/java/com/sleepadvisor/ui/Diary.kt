package com.sleepadvisor.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.sleepadvisor.data.Preferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SleepDiaryForm(
    onUpdate: () -> Unit,
    revision: Int
) {
    val context = LocalContext.current
    val gson = Gson()
    
    val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    
    var date by remember { mutableStateOf(todayStr) }
    var bedTime by remember { mutableStateOf("23:00") }
    var lightOutTime by remember { mutableStateOf("23:15") }
    var latencyMins by remember { mutableStateOf("30") }
    var awakenings by remember { mutableStateOf("1") }
    var awakeMins by remember { mutableStateOf("15") }
    var wakeTime by remember { mutableStateOf("06:30") }
    var outOfBedTime by remember { mutableStateOf("06:45") }
    var quality by remember { mutableStateOf(3) }
    var alertness by remember { mutableStateOf(3) }
    var medications by remember { mutableStateOf("None") }
    var notes by remember { mutableStateOf("") }
    
    var diaries by remember { mutableStateOf(listOf<JsonObject>()) }
    
    // Live calculations
    var tibMins by remember { mutableStateOf(0) }
    var actualMins by remember { mutableStateOf(0) }
    var efficiency by remember { mutableStateOf(0.0) }

    fun loadDiaries() {
        val logsJson = Preferences.getString(context, Preferences.KEY_DIARIES, "[]")
        try {
            val arr = gson.fromJson(logsJson, JsonArray::class.java) ?: JsonArray()
            val list = mutableListOf<JsonObject>()
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            var cleanNeeded = false
            val cleanArray = JsonArray()
            
            for (i in 0 until arr.size()) {
                val item = arr.get(i).asJsonObject
                val dateStr = item.get("date")?.asString ?: ""
                // Filter out invalid future dates
                if (dateStr > todayStr && dateStr.length == 10) {
                    cleanNeeded = true
                } else {
                    list.add(item)
                    cleanArray.add(item)
                }
            }
            
            if (cleanNeeded) {
                Preferences.putString(context, Preferences.KEY_DIARIES, gson.toJson(cleanArray))
            }
            
            // Sort by date descending
            list.sortByDescending { it.get("date").asString }
            diaries = list

            // Auto-prefill the form with today's log if it exists in the database
            val todayEntry = list.find { it.get("date")?.asString == todayStr }
            if (todayEntry != null) {
                date = todayStr
                bedTime = todayEntry.get("bed_time")?.let { if (it.isJsonNull) "23:00" else it.asString } ?: "23:00"
                lightOutTime = todayEntry.get("light_out_time")?.let { if (it.isJsonNull) "23:15" else it.asString } ?: "23:15"
                latencyMins = (todayEntry.get("latency_mins")?.let { if (it.isJsonNull) 30 else it.asInt } ?: 30).toString()
                awakenings = (todayEntry.get("awakenings")?.let { if (it.isJsonNull) 1 else it.asInt } ?: 1).toString()
                awakeMins = (todayEntry.get("awake_mins")?.let { if (it.isJsonNull) 15 else it.asInt } ?: 15).toString()
                wakeTime = todayEntry.get("wake_time")?.let { if (it.isJsonNull) "06:30" else it.asString } ?: "06:30"
                outOfBedTime = todayEntry.get("out_of_bed_time")?.let { if (it.isJsonNull) "06:45" else it.asString } ?: "06:45"
                quality = todayEntry.get("quality")?.let { if (it.isJsonNull) 3 else it.asInt } ?: 3
                alertness = todayEntry.get("alertness")?.let { if (it.isJsonNull) 3 else it.asInt } ?: 3
                medications = todayEntry.get("medications")?.let { if (it.isJsonNull) "None" else it.asString } ?: "None"
                notes = todayEntry.get("notes")?.let { if (it.isJsonNull) "" else it.asString } ?: ""
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    LaunchedEffect(revision) {
        loadDiaries()
    }

    // Run live calculations
    LaunchedEffect(bedTime, lightOutTime, latencyMins, awakeMins, wakeTime, outOfBedTime) {
        fun timeDiff(t1: String, t2: String): Int {
            return try {
                val parts1 = t1.split(":").map { it.toInt() }
                val parts2 = t2.split(":").map { it.toInt() }
                var mins = (parts2[0] * 60 + parts2[1]) - (parts1[0] * 60 + parts1[1])
                if (mins < 0) mins += 24 * 60
                mins
            } catch (e: Exception) {
                0
            }
        }

        val start = if (lightOutTime.isNotEmpty()) lightOutTime else bedTime
        val tib = timeDiff(start, outOfBedTime)
        val lat = latencyMins.toIntOrNull() ?: 0
        val awk = awakeMins.toIntOrNull() ?: 0
        val act = (tib - lat - awk).coerceAtLeast(0)
        val eff = if (tib > 0) Math.round((act.toDouble() / tib) * 100 * 10) / 10.0 else 0.0
        
        tibMins = tib
        actualMins = act
        efficiency = eff
    }

    fun formatMins(totalMins: Int): String {
        val hrs = totalMins / 60
        val remainder = totalMins % 60
        return if (remainder > 0) "${hrs}h ${remainder}m" else "${hrs}h"
    }

    val handleSubmit = {
        val newDiary = JsonObject().apply {
            addProperty("date", date)
            addProperty("bed_time", bedTime)
            addProperty("light_out_time", lightOutTime)
            addProperty("latency_mins", latencyMins.toIntOrNull() ?: 0)
            addProperty("awakenings", awakenings.toIntOrNull() ?: 0)
            addProperty("awake_mins", awakeMins.toIntOrNull() ?: 0)
            addProperty("wake_time", wakeTime)
            addProperty("out_of_bed_time", outOfBedTime)
            addProperty("quality", quality)
            addProperty("alertness", alertness)
            addProperty("medications", medications)
            addProperty("notes", notes)
        }

        val logsJson = Preferences.getString(context, Preferences.KEY_DIARIES, "[]")
        val logsArray = gson.fromJson(logsJson, JsonArray::class.java) ?: JsonArray()
        
        var existingIndex = -1
        for (i in 0 until logsArray.size()) {
            if (logsArray.get(i).asJsonObject.get("date").asString == date) {
                existingIndex = i
                break
            }
        }

        if (existingIndex != -1) {
            logsArray.set(existingIndex, newDiary)
        } else {
            logsArray.add(newDiary)
        }

        Preferences.putString(context, Preferences.KEY_DIARIES, gson.toJson(logsArray))
        Toast.makeText(context, "Sleep diary saved successfully.", Toast.LENGTH_SHORT).show()
        
        notes = ""
        loadDiaries()
        onUpdate()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Real-time calculations feedback card
        GlassCard(glow = true) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Time In Bed", color = TextSecondary, fontSize = 11.sp)
                    Text(text = formatMins(tibMins), color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Actual Sleep", color = TextSecondary, fontSize = 11.sp)
                    Text(text = formatMins(actualMins), color = Purple, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Efficiency", color = TextSecondary, fontSize = 11.sp)
                    Text(
                        text = "$efficiency%",
                        color = if (efficiency >= 85) Cyan else Gold,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        // Form Card
        GlassCard {
            Text(text = "60-Second Sleep Diary", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(text = "Complete this diary every morning to track your CBT-I progress.", color = TextSecondary, fontSize = 12.sp)
            
            Spacer(modifier = Modifier.height(16.dp))

            // Form inputs
            OutlinedTextField(
                value = date,
                onValueChange = { date = it },
                label = { Text("Diary Date (YYYY-MM-DD)") },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Purple, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = medications,
                onValueChange = { medications = it },
                label = { Text("Medications Taken") },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Purple, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = bedTime,
                    onValueChange = { bedTime = it },
                    label = { Text("Time to Bed (HH:MM)") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Purple, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = lightOutTime,
                    onValueChange = { lightOutTime = it },
                    label = { Text("Lights Out Time") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Purple, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = latencyMins,
                    onValueChange = { latencyMins = it },
                    label = { Text("Latency (Mins)") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Purple, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = awakenings,
                    onValueChange = { awakenings = it },
                    label = { Text("Awakenings Count") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Purple, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = awakeMins,
                    onValueChange = { awakeMins = it },
                    label = { Text("Awake in Night (Mins)") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Purple, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = wakeTime,
                    onValueChange = { wakeTime = it },
                    label = { Text("Final Wake Time") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Purple, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = outOfBedTime,
                onValueChange = { outOfBedTime = it },
                label = { Text("Time Out of Bed (HH:MM)") },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Purple, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Rating Selector for Quality
            Text(text = "Sleep Quality (1 - 5)", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            RatingSelector(rating = quality, onRatingSelected = { quality = it })

            Spacer(modifier = Modifier.height(12.dp))

            // Rating Selector for Alertness
            Text(text = "Daytime Alertness (1 - 5)", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            RatingSelector(rating = alertness, onRatingSelected = { alertness = it })

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Negative Sleep Thoughts (NSTs) / Notes") },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Purple, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { handleSubmit() },
                colors = ButtonDefaults.buttonColors(containerColor = Purple),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Default.AddCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Save Sleep Diary", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        // History Card
        GlassCard {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(imageVector = Icons.Default.TableChart, contentDescription = null, tint = Purple)
                Text(text = "Past Sleep Records", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Text(text = "Your historical logs saved on this device.", color = TextSecondary, fontSize = 12.sp)
            
            Spacer(modifier = Modifier.height(16.dp))

            if (diaries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "No logs recorded yet. Create your first log above.", color = TextMuted, fontSize = 12.sp)
                }
            } else {
                diaries.forEach { d ->
                    val dateVal = d.get("date")?.let { if (it.isJsonNull) "" else it.asString } ?: ""
                    val bedVal = d.get("bed_time")?.let { if (it.isJsonNull) "23:00" else it.asString } ?: "23:00"
                    val outVal = d.get("out_of_bed_time")?.let { if (it.isJsonNull) "06:45" else it.asString } ?: "06:45"
                    val lightVal = d.get("light_out_time")?.let { if (it.isJsonNull) "" else it.asString } ?: ""
                    val latencyVal = d.get("latency_mins")?.let { if (it.isJsonNull) 0 else it.asInt } ?: 0
                    val awakeVal = d.get("awake_mins")?.let { if (it.isJsonNull) 0 else it.asInt } ?: 0
                    val awakeningsVal = d.get("awakenings")?.let { if (it.isJsonNull) 0 else it.asInt } ?: 0
                    val qualityVal = d.get("quality")?.let { if (it.isJsonNull) 3 else it.asInt } ?: 3
                    val wakeVal = d.get("wake_time")?.let { if (it.isJsonNull) "" else it.asString } ?: ""
                    val alertnessVal = d.get("alertness")?.let { if (it.isJsonNull) 3 else it.asInt } ?: 3
                    val medicationsVal = d.get("medications")?.let { if (it.isJsonNull) "" else it.asString } ?: ""
                    val notesVal = d.get("notes")?.let { if (it.isJsonNull) "" else it.asString } ?: ""

                    // Calculate efficiency row
                    fun calculateRowEff(): Int {
                        fun timeDiff(t1: String, t2: String): Int {
                            return try {
                                val parts1 = t1.split(":").map { it.toInt() }
                                val parts2 = t2.split(":").map { it.toInt() }
                                var mins = (parts2[0] * 60 + parts2[1]) - (parts1[0] * 60 + parts1[1])
                                if (mins < 0) mins += 24 * 60
                                mins
                            } catch (e: Exception) {
                                0
                            }
                        }
                        val start = if (lightVal.isNotEmpty()) lightVal else bedVal
                        val tib = timeDiff(start, outVal)
                        val act = (tib - latencyVal - awakeVal).coerceAtLeast(0)
                        return if (tib > 0) Math.round((act.toDouble() / tib) * 100).toInt() else 0
                    }

                    val rowEff = calculateRowEff()

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .background(Color.White.copy(alpha = 0.02f))
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                            .clickable {
                                date = dateVal
                                bedTime = bedVal
                                lightOutTime = lightVal
                                latencyMins = latencyVal.toString()
                                awakenings = awakeningsVal.toString()
                                awakeMins = awakeVal.toString()
                                wakeTime = wakeVal
                                outOfBedTime = outVal
                                quality = qualityVal
                                alertness = alertnessVal
                                medications = medicationsVal
                                notes = notesVal
                                Toast.makeText(context, "Loaded diary entry for $dateVal to edit", Toast.LENGTH_SHORT).show()
                            }
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = dateVal, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(
                                text = "Eff: $rowEff%",
                                color = if (rowEff >= 85) Cyan else Gold,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Bed Window: $bedVal - $outVal", color = TextSecondary, fontSize = 11.sp)
                            Text(text = "Quality: $qualityVal/5", color = TextSecondary, fontSize = 11.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Latency: ${latencyVal}m | Awakenings: $awakeningsVal | Awake time: ${awakeVal}m",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RatingSelector(rating: Int, onRatingSelected: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (i in 1..5) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (rating == i) Purple else Color.White.copy(alpha = 0.05f))
                    .border(
                        1.dp,
                        if (rating == i) Purple else Color.White.copy(alpha = 0.08f),
                        RoundedCornerShape(10.dp)
                    )
                    .clickable { onRatingSelected(i) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = i.toString(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}
