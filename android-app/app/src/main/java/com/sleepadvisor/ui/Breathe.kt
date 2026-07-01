package com.sleepadvisor.ui

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sleepadvisor.data.Preferences
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.exp
import kotlin.math.sin

@Composable
fun RelaxationRoom(
    onUpdate: () -> Unit,
    revision: Int
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isRunning by remember { mutableStateOf(false) }
    var breathPhase by remember { mutableStateOf("rest") } // "rest", "inhale", "hold", "exhale", "hold_empty"
    var secondsLeft by remember { mutableStateOf(4) }
    var totalTimer by remember { mutableStateOf(600) } // 10 minutes (600s) default
    var durationPreset by remember { mutableStateOf(600) }
    var soundEnabled by remember { mutableStateOf(true) }
    var technique by remember { mutableStateOf("4-4-4-4") } // "4-4-4-4", "4-7-8"
    var exerciseCompleted by remember { mutableStateOf(false) }

    // Audio synthesizer helper
    fun playChime(frequency: Double, durationSec: Double) {
        if (!soundEnabled) return
        Thread {
            try {
                val sampleRate = 44100
                val numSamples = (durationSec * sampleRate).toInt()
                val sample = DoubleArray(numSamples)
                val generatedSnd = ByteArray(2 * numSamples)

                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    val expDecay = exp(-5.0 * i / numSamples)
                    sample[i] = sin(2.0 * Math.PI * frequency * t) * expDecay * 0.15 // Volume 15%
                }

                var idx = 0
                for (dVal in sample) {
                    val valShort = (dVal * 32767).toInt().toShort()
                    generatedSnd[idx++] = (valShort.toInt() and 0x00ff).toByte()
                    generatedSnd[idx++] = ((valShort.toInt() and 0xff00) ushr 8).toByte()
                }

                val audioTrack = AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    generatedSnd.size,
                    AudioTrack.MODE_STATIC
                )
                audioTrack.write(generatedSnd, 0, generatedSnd.size)
                audioTrack.play()
                // Auto release track after playing
                Thread.sleep((durationSec * 1000).toLong() + 200)
                audioTrack.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    // Complete session helper
    fun completeExercise() {
        isRunning = false
        breathPhase = "rest"
        secondsLeft = 4
        exerciseCompleted = true
        playChime(659.25, 1.2) // E5 soothing final chime
        
        // Log in preferences
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        Preferences.putString(context, "relaxation_done_$todayStr", "true")
        onUpdate()
    }

    // Phase Loop Coroutine
    LaunchedEffect(isRunning, technique) {
        if (isRunning) {
            var phase = "inhale"
            while (isRunning && totalTimer > 0) {
                breathPhase = phase
                
                var duration = 4
                var nextPhase = "hold"
                var chimeFreq = 329.63 // E4

                if (technique == "4-4-4-4") {
                    // Box Breathing
                    when (phase) {
                        "inhale" -> {
                            duration = 4
                            nextPhase = "hold"
                            chimeFreq = 329.63 // E4
                        }
                        "hold" -> {
                            duration = 4
                            nextPhase = "exhale"
                            chimeFreq = 392.00 // G4
                        }
                        "exhale" -> {
                            duration = 4
                            nextPhase = "hold_empty"
                            chimeFreq = 261.63 // C4
                        }
                        "hold_empty" -> {
                            duration = 4
                            nextPhase = "inhale"
                            chimeFreq = 293.66 // D4
                        }
                    }
                } else {
                    // 4-7-8 Breathing
                    when (phase) {
                        "inhale" -> {
                            duration = 4
                            nextPhase = "hold"
                            chimeFreq = 329.63 // E4
                        }
                        "hold" -> {
                            duration = 7
                            nextPhase = "exhale"
                            chimeFreq = 392.00 // G4
                        }
                        "exhale" -> {
                            duration = 8
                            nextPhase = "inhale"
                            chimeFreq = 261.63 // C4
                        }
                    }
                }

                secondsLeft = duration
                playChime(chimeFreq, 0.4)

                // Countdown for this phase
                for (sec in duration downTo 1) {
                    if (!isRunning) break
                    secondsLeft = sec
                    delay(1000)
                }

                phase = nextPhase
            }
        }
    }

    // Total Session Countdown Timer Coroutine
    LaunchedEffect(isRunning) {
        if (isRunning) {
            while (isRunning && totalTimer > 0) {
                delay(1000)
                if (totalTimer <= 1) {
                    totalTimer = 0
                    completeExercise()
                } else {
                    totalTimer -= 1
                }
            }
        }
    }

    // Reset helper
    val handleReset = {
        isRunning = false
        breathPhase = "rest"
        secondsLeft = 4
        totalTimer = durationPreset
        exerciseCompleted = false
    }

    val changePreset: (Int) -> Unit = { sec ->
        isRunning = false
        durationPreset = sec
        totalTimer = sec
        exerciseCompleted = false
    }

    fun formatTotalTime(sec: Int): String {
        val m = sec / 60
        val s = sec % 60
        return String.format(Locale.US, "%d:%02d", m, s)
    }

    // Breathing pacer size scale
    val pacerScale by animateFloatAsState(
        targetValue = when (breathPhase) {
            "inhale" -> 2.4f
            "hold" -> 2.4f
            "exhale" -> 1.0f
            "hold_empty" -> 1.0f
            else -> 1.0f
        },
        animationSpec = tween(
            durationMillis = when (breathPhase) {
                "inhale" -> 4000
                "exhale" -> if (technique == "4-7-8") 8000 else 4000
                else -> 500
            },
            easing = LinearEasing
        ),
        label = "breathe_scale"
    )

    // Breathing circle background color transitions
    val circleColor by animateColorAsState(
        targetValue = when (breathPhase) {
            "hold" -> Gold
            "hold_empty" -> Color(0x66FFBE0B)
            else -> Cyan
        },
        animationSpec = tween(1000),
        label = "circle_color"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title block
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Relaxation Room",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Consciously elicit the Relaxation Response to slow brain waves.",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
            
            IconButton(
                onClick = { soundEnabled = !soundEnabled },
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.05f))
            ) {
                Icon(
                    imageVector = if (soundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                    contentDescription = "Mute",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Configuration Card
        GlassCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Technique Selector
                Column(modifier = Modifier.weight(1.2f)) {
                    Text(text = "Breathing Pattern", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    var dropdownExpanded by remember { mutableStateOf(false) }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x990A0E28))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                            .clickable(enabled = !isRunning) { dropdownExpanded = true }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = if (technique == "4-4-4-4") "Box (4-4-4-4)" else "Calming (4-7-8)",
                            color = TextPrimary,
                            fontSize = 13.sp
                        )
                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false },
                            modifier = Modifier.background(BgDeep)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Box Breathing (4-4-4-4)", color = TextPrimary) },
                                onClick = {
                                    technique = "4-4-4-4"
                                    handleReset()
                                    dropdownExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Calming Breath (4-7-8)", color = TextPrimary) },
                                onClick = {
                                    technique = "4-7-8"
                                    handleReset()
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Preset Selector
                Column(modifier = Modifier.weight(1.8f)) {
                    Text(text = "Duration Preset", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(120 to "2m", 300 to "5m", 600 to "10m", 900 to "15m").forEach { (sec, label) ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (durationPreset == sec) Purple else Color.White.copy(alpha = 0.05f))
                                    .border(
                                        1.dp,
                                        if (durationPreset == sec) Purple else Color.White.copy(alpha = 0.08f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable(enabled = !isRunning) { changePreset(sec) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Pacer Card Interface
        GlassCard(modifier = Modifier.weight(1f)) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Countdown text timer
                Text(
                    text = formatTotalTime(totalTimer),
                    color = Gold,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Expanding / Contracting Circle pacer
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .border(2.dp, Cyan.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size((80 * pacerScale).dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(circleColor, circleColor.copy(alpha = 0.6f))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isRunning) secondsLeft.toString() else "💨",
                            color = BgDark,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Text Prompts
                Text(
                    text = when {
                        !isRunning -> "Ready"
                        breathPhase == "inhale" -> "BREATHE IN"
                        breathPhase == "hold" -> "HOLD"
                        breathPhase == "exhale" -> "BREATHE OUT"
                        breathPhase == "hold_empty" -> "HOLD"
                        else -> "Ready"
                    },
                    color = Cyan,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = when {
                        !isRunning -> "Tap start to begin"
                        breathPhase == "inhale" -> "Expand your abdomen slowly"
                        breathPhase == "hold" -> "Rest in the fullness"
                        breathPhase == "exhale" -> "Release all air and tension"
                        breathPhase == "hold_empty" -> "Rest in the emptiness"
                        else -> ""
                    },
                    color = TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Control Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { isRunning = !isRunning },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) Red else Cyan
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1.5f)
                    .height(54.dp)
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = if (isRunning) Color.White else BgDark
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isRunning) "Pause" else "Start Session",
                    color = if (isRunning) Color.White else BgDark,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = { handleReset() },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.05f)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Reset", color = Color.White)
            }
        }

        // Completion status
        if (exerciseCompleted) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0x1A10B981))
                    .border(1.dp, Color(0x3310B981), RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Cyan, modifier = Modifier.size(24.dp))
                Text(
                    text = "Session Completed! You successfully practiced the Relaxation Response. Abdominal breathing slows your sympathetic nervous system and is logged on today's sleep checklist. Keep it up!",
                    color = Color(0xFFC4F4E7),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
