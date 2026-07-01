package com.sleepadvisor.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.sleepadvisor.agent.SleepAgent
import com.sleepadvisor.data.Preferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun Dashboard(
    onNavigate: (String) -> Unit,
    revision: Int
) {
    val context = LocalContext.current
    val gson = Gson()
    
    // Parse stats
    var totalLogs by remember { mutableStateOf(0) }
    var avgEfficiency by remember { mutableStateOf(0.0) }
    var avgDurationMins by remember { mutableStateOf(0) }
    var avgLatency by remember { mutableStateOf(0) }
    
    // Checklist state
    var diaryDoneToday by remember { mutableStateOf(false) }
    var relaxationDoneToday by remember { mutableStateOf(false) }
    
    // CBT Week
    var cbtWeek by remember { mutableStateOf(-1) }

    LaunchedEffect(revision) {
        // Calculate sleep stats
        val statsString = SleepAgent.calculateSleepStats(context)
        try {
            val statsObj = gson.fromJson(statsString, JsonObject::class.java)
            totalLogs = statsObj.get("total_logs")?.asInt ?: 0
            avgEfficiency = statsObj.get("average_efficiency")?.asDouble ?: 0.0
            avgDurationMins = statsObj.get("average_duration_mins")?.asInt ?: 0
            avgLatency = statsObj.get("average_latency_mins")?.asInt ?: 0
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // CBT Week
        cbtWeek = try {
            Preferences.getString(context, Preferences.KEY_CBT_WEEK, "-1").toInt()
        } catch (e: Exception) {
            -1
        }

        // Today strings
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        
        // Check diary
        val diariesJson = Preferences.getString(context, Preferences.KEY_DIARIES, "[]")
        val diariesArray = gson.fromJson(diariesJson, JsonArray::class.java) ?: JsonArray()
        var diaryDone = false
        for (i in 0 until diariesArray.size()) {
            val d = diariesArray.get(i).asJsonObject
            if (d.get("date")?.asString == todayStr) {
                diaryDone = true
                break
            }
        }
        diaryDoneToday = diaryDone

        // Check relaxation
        relaxationDoneToday = Preferences.getString(context, "relaxation_done_$todayStr", "false") == "true"
    }

    val weekName = when (cbtWeek) {
        -1 -> "Initial Interview"
        0 -> "Baseline Logging"
        1 -> "Week 1: Changing Thoughts"
        2 -> "Week 2: Establishing Habits"
        3 -> "Week 3: Lifestyle & Environment"
        4 -> "Week 4: Relaxation Response"
        5 -> "Week 5: Thinking Away Stress"
        6 -> "Week 6: Developing Attitudes"
        else -> "CBT Maintenance"
    }

    val weekInstructions = when (cbtWeek) {
        -1 -> "Engage in the voice chat to complete your initial sleep profile. Let's outline your goals."
        0 -> "Log your sleep diary every morning. We need 7 days of logs to calculate your average sleep duration and efficiency."
        1 -> "Read about sleep physiology. Identify your Negative Sleep Thoughts (NSTs) and replace them with Positive Sleep Thoughts (PSTs). Remember: you don't need 8 hours!"
        2 -> "Enforce sleep restriction! Limit your time in bed to your average sleep duration. Rise at the exact same time every day. No naps, and get out of bed if awake for 20 minutes."
        3 -> "Expose yourself to morning bright light. Exercise for 20-30 minutes in the late afternoon. Complete caffeine cutoff by 12:00 PM, and avoid alcohol/nicotine."
        4 -> "Practice the Relaxation Response. Use the breathing exercise tool in the Relaxation Room for 15-20 minutes every afternoon or before bed."
        5 -> "Challenge stressful daytime thinking. Reframe your daytime negative self-talk and write down worries in a journal long before bedtime."
        6 -> "Adopt sleep-resilient attitudes. Maintain consistency, commit to your sleep restriction windows, and view challenges as opportunities."
        else -> "Maintain your consistent rise times, sleep-promoting habits, and relaxation techniques."
    }

    val progressPercent = ((cbtWeek + 1).toDouble() / 7.0 * 100.0).coerceIn(0.0, 100.0).toInt()

    fun formatMinsToHours(totalMins: Int): String {
        if (totalMins <= 0) return "0 hrs"
        val hrs = totalMins / 60
        val mins = totalMins % 60
        return if (mins > 0) "${hrs}h ${mins}m" else "${hrs}h"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // CBT-I Status Card
        GlassCard(glow = true) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Surface(
                        color = Purple.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.border(1.dp, PurpleGlow, RoundedCornerShape(20.dp))
                    ) {
                        Text(
                            text = weekName,
                            color = Color(0xFFC0A9FF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "CBT-I Program Status",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Text(
                    text = if (cbtWeek == -1) "Interview" else if (cbtWeek == 0) "Baseline" else "Week $cbtWeek/6",
                    color = Gold,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = weekInstructions,
                color = TextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
            
            if (cbtWeek == -1) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { onNavigate("chat") },
                    colors = ButtonDefaults.buttonColors(containerColor = Gold),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Mic, contentDescription = null, tint = BgDark)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Start Voice Assessment", color = BgDark, fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "Start", color = TextMuted, fontSize = 11.sp)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progressPercent / 100f)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(Purple, Gold)
                                )
                            )
                    )
                }
                Text(
                    text = "$progressPercent% Complete",
                    color = Gold,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Stats Grid
        Text(
            text = "Sleep Statistics Summary",
            color = TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Efficiency Card
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(PanelBg)
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                    .padding(14.dp)
            ) {
                Icon(Icons.Default.Speed, contentDescription = null, tint = Gold, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "$avgEfficiency%", color = Gold, fontSize = 26.sp, fontWeight = FontWeight.Black)
                Text(text = "Sleep Efficiency", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                Text(text = "Target: > 85%", color = TextMuted, fontSize = 9.sp)
            }

            // Duration Card
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(PanelBg)
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                    .padding(14.dp)
            ) {
                Icon(Icons.Default.AccessTime, contentDescription = null, tint = Purple, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = formatMinsToHours(avgDurationMins), color = Purple, fontSize = 26.sp, fontWeight = FontWeight.Black)
                Text(text = "Avg Sleep Time", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                Text(text = "Actual duration", color = TextMuted, fontSize = 9.sp)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Latency Card
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(PanelBg)
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                    .padding(14.dp)
            ) {
                Icon(Icons.Default.HourglassEmpty, contentDescription = null, tint = Cyan, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "${avgLatency}m", color = Cyan, fontSize = 26.sp, fontWeight = FontWeight.Black)
                Text(text = "Avg Latency", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                Text(text = "Time to fall asleep", color = TextMuted, fontSize = 9.sp)
            }

            // Logs Card
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(PanelBg)
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                    .padding(14.dp)
            ) {
                Icon(Icons.Default.MenuBook, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "$totalLogs / 7", color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Black)
                Text(text = "Logged Diaries", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                Text(text = "Logs in history", color = TextMuted, fontSize = 9.sp)
            }
        }

        // Today's Checklist
        GlassCard {
            Text(
                text = "Today's Checklist",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Maintain consistency in your daily CBT-I exercises.",
                color = TextSecondary,
                fontSize = 12.sp
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // Item 1: Complete Sleep Diary
            ChecklistItem(
                title = "Complete Sleep Diary",
                subtitle = "Report your sleep parameters from last night.",
                isChecked = diaryDoneToday,
                onClick = { onNavigate("diary") }
            )

            // Item 2: Practice Relaxation Response
            ChecklistItem(
                title = "Practice Relaxation Response",
                subtitle = "Perform 15 minutes of guided box breathing.",
                isChecked = relaxationDoneToday,
                onClick = { onNavigate("relaxation") }
            )

            // Item 3: Daily Advisor Check-in
            ChecklistItem(
                title = "Daily Advisor Check-in",
                subtitle = "Speak with your sleep advisor to receive tips and restructure thoughts.",
                isChecked = true,
                onClick = { onNavigate("chat") }
            )
        }

        // Quick Tip Panel
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Gold.copy(alpha = 0.05f))
                .border(1.dp, Gold.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Gold, modifier = Modifier.size(24.dp))
            Text(
                text = "CBT-I Fact: The \"8-hour sleep myth\" causes excessive anxiety. Most adults sleep perfectly healthy on 6.5 to 7.5 hours, and research shows 7-hour sleepers actually live longer!",
                color = Color(0xFFFFEAA3),
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun ChecklistItem(
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (isChecked) Cyan else Color.Transparent)
                .border(2.dp, if (isChecked) Cyan else Color.White.copy(alpha = 0.12f), RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (isChecked) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = BgDark,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        
        Column {
            Text(
                text = title,
                color = if (isChecked) Cyan else TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 11.sp
            )
        }
    }
}
