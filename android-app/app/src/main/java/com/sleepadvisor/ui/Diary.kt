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
    
    val viewModel: com.sleepadvisor.ui.viewmodel.DiaryViewModel = androidx.lifecycle.viewmodel.compose.viewModel()

    val diaries by viewModel.diaries.collectAsState()
    val date by viewModel.date.collectAsState()
    val bedTime by viewModel.bedTime.collectAsState()
    val lightOutTime by viewModel.lightOutTime.collectAsState()
    val latencyMins by viewModel.latencyMins.collectAsState()
    val awakenings by viewModel.awakenings.collectAsState()
    val awakeMins by viewModel.awakeMins.collectAsState()
    val wakeTime by viewModel.wakeTime.collectAsState()
    val outOfBedTime by viewModel.outOfBedTime.collectAsState()
    val quality by viewModel.quality.collectAsState()
    val alertness by viewModel.alertness.collectAsState()
    val medications by viewModel.medications.collectAsState()
    val notes by viewModel.notes.collectAsState()

    val efficiency by viewModel.efficiency.collectAsState()
    val durationMins by viewModel.durationMins.collectAsState()
    val tibMins by viewModel.tibMins.collectAsState()

    LaunchedEffect(revision) {
        viewModel.loadDiaries()
        viewModel.prefillToday()
    }

    fun formatMins(totalMins: Int): String {
        val hrs = totalMins / 60
        val remainder = totalMins % 60
        return if (remainder > 0) "${hrs}h ${remainder}m" else "${hrs}h"
    }

    val handleSubmit = {
        viewModel.handleSubmit {
            Toast.makeText(context, "Sleep diary saved successfully.", Toast.LENGTH_SHORT).show()
            onUpdate()
        }
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
                    Text(text = formatMins(durationMins), color = Purple, fontSize = 18.sp, fontWeight = FontWeight.Bold)
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
                onValueChange = { viewModel.setField("date", it) },
                label = { Text("Diary Date (YYYY-MM-DD)") },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Purple, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = medications,
                onValueChange = { viewModel.setField("medications", it) },
                label = { Text("Medications Taken") },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Purple, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = bedTime,
                    onValueChange = { viewModel.setField("bedTime", it) },
                    label = { Text("Time to Bed (HH:MM)") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Purple, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = lightOutTime,
                    onValueChange = { viewModel.setField("lightOutTime", it) },
                    label = { Text("Lights Out Time") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Purple, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = latencyMins,
                    onValueChange = { viewModel.setField("latencyMins", it) },
                    label = { Text("Latency (Mins)") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Purple, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = awakenings,
                    onValueChange = { viewModel.setField("awakenings", it) },
                    label = { Text("Awakenings Count") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Purple, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = awakeMins,
                    onValueChange = { viewModel.setField("awakeMins", it) },
                    label = { Text("Awake in Night (Mins)") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Purple, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = wakeTime,
                    onValueChange = { viewModel.setField("wakeTime", it) },
                    label = { Text("Final Wake Time") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Purple, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = outOfBedTime,
                onValueChange = { viewModel.setField("outOfBedTime", it) },
                label = { Text("Time Out of Bed (HH:MM)") },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Purple, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Rating Selector for Quality
            Text(text = "Sleep Quality (1 - 5)", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            RatingSelector(rating = quality, onRatingSelected = { viewModel.setField("quality", it) })

            Spacer(modifier = Modifier.height(12.dp))

            // Rating Selector for Alertness
            Text(text = "Daytime Alertness (1 - 5)", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            RatingSelector(rating = alertness, onRatingSelected = { viewModel.setField("alertness", it) })

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = notes,
                onValueChange = { viewModel.setField("notes", it) },
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
                    val rowEff = d.sleepEfficiency().toInt()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .background(Color.White.copy(alpha = 0.02f))
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                            .clickable {
                                viewModel.selectEntry(d)
                                Toast.makeText(context, "Loaded diary entry for ${d.date} to edit", Toast.LENGTH_SHORT).show()
                            }
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = d.date, fontWeight = FontWeight.Bold, color = TextPrimary)
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
                            Text(text = "Bed Window: ${d.bedTime} - ${d.outOfBedTime}", color = TextSecondary, fontSize = 11.sp)
                            Text(text = "Quality: ${d.quality}/5", color = TextSecondary, fontSize = 11.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Latency: ${d.latencyMins}m | Awakenings: ${d.awakenings} | Awake time: ${d.awakeMins}m",
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
