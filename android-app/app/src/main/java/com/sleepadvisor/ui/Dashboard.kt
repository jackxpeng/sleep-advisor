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
import com.sleepadvisor.data.Preferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun Dashboard(
    onNavigate: (String) -> Unit,
    revision: Int
) {
    val viewModel: com.sleepadvisor.ui.viewmodel.DashboardViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    
    val sleepStats by viewModel.sleepStats.collectAsState()
    val cbtWeek by viewModel.cbtWeek.collectAsState()
    val diaryDoneToday by viewModel.isDiaryDoneToday.collectAsState()
    val relaxationDoneToday by viewModel.isBreathingDoneToday.collectAsState()
    val progressPercentState by viewModel.progressPercent.collectAsState()

    LaunchedEffect(revision) {
        viewModel.reloadDashboard()
    }

    val totalLogs = sleepStats.totalLogs
    val avgEfficiency = sleepStats.avgEfficiency
    val avgDurationMins = sleepStats.avgDurationMins.toInt()
    val avgLatency = sleepStats.avgLatencyMins.toInt()

    val weekName = cbtWeek.displayName
    val weekInstructions = cbtWeek.instructions

    // checklist progress is 0, 50, or 100
    val checklistProgressPercent = (progressPercentState * 100).toInt()

    // overall week progress is (weekNum + 1) / 8 * 100
    val progressPercent = ((cbtWeek.weekNum + 1).toDouble() / 8.0 * 100.0).coerceIn(0.0, 100.0).toInt()

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
                    text = if (cbtWeek.weekNum == -1) "Interview" else if (cbtWeek.weekNum == 0) "Baseline" else "Week ${cbtWeek.weekNum}/6",
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
            
            if (cbtWeek.weekNum == -1) {
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
