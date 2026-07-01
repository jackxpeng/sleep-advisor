package com.sleepadvisor.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.sleepadvisor.data.Preferences
import com.sleepadvisor.speech.SpeechTTS
import java.util.Locale


@Composable
fun Settings(
    onUpdate: () -> Unit,
    revision: Int
) {
    val context = LocalContext.current
    val gson = Gson()

    var apiKey by remember { mutableStateOf("") }
    var showKey by remember { mutableStateOf(false) }
    var voiceRate by remember { mutableStateOf(1.0f) }
    var voicePitch by remember { mutableStateOf(0.9f) }

    // TTS engine for testing
    var ttsTest by remember { mutableStateOf<SpeechTTS?>(null) }

    DisposableEffect(Unit) {
        ttsTest = SpeechTTS(context)
        
        apiKey = Preferences.getString(context, Preferences.KEY_API_KEY)
        voiceRate = Preferences.getString(context, Preferences.KEY_VOICE_RATE, "1.0").toFloatOrNull() ?: 1.0f
        voicePitch = Preferences.getString(context, Preferences.KEY_VOICE_PITCH, "0.9").toFloatOrNull() ?: 0.9f

        onDispose {
            ttsTest?.shutdown()
        }
    }

    val handleSaveKey = {
        Preferences.putString(context, Preferences.KEY_API_KEY, apiKey.trim())
        Toast.makeText(context, "API Key updated successfully.", Toast.LENGTH_SHORT).show()
        onUpdate()
    }

    val handleSaveVoiceSettings = {
        Preferences.putString(context, Preferences.KEY_VOICE_RATE, voiceRate.toString())
        Preferences.putString(context, Preferences.KEY_VOICE_PITCH, voicePitch.toString())
        Toast.makeText(context, "Voice settings updated.", Toast.LENGTH_SHORT).show()
    }

    val handleTestVoice = {
        ttsTest?.speak(
            "Hello there. Can you hear my voice? I am your sleep advisor, ready to help you sleep.",
            voiceRate,
            voicePitch
        )
    }

    val handleResetData = {
        // Just clear the preferences values and re-initialize
        Preferences.putString(context, Preferences.KEY_API_KEY, "")
        Preferences.putString(context, Preferences.KEY_HUMAN_MEMORY, "")
        Preferences.putString(context, Preferences.KEY_PERSONA_MEMORY, "")
        Preferences.putString(context, Preferences.KEY_ARCHIVAL_MEMORIES, "[]")
        Preferences.putString(context, Preferences.KEY_DIARIES, "[]")
        Preferences.putString(context, Preferences.KEY_CHAT_HISTORY, "[]")
        Preferences.putString(context, Preferences.KEY_CBT_WEEK, "-1")
        Preferences.putString(context, Preferences.KEY_VOICE_RATE, "1.0")
        Preferences.putString(context, Preferences.KEY_VOICE_PITCH, "1.0")
        Preferences.initialize(context)

        apiKey = ""
        voiceRate = 1.0f
        voicePitch = 0.9f
        
        Toast.makeText(context, "All application data has been reset.", Toast.LENGTH_SHORT).show()
        onUpdate()
    }

    val handleLoadMockData = {
        val mockDiaries = """[
          {
            "date": "2026-06-22",
            "bed_time": "23:00",
            "light_out_time": "23:15",
            "latency_mins": 45,
            "awakenings": 2,
            "awake_mins": 40,
            "wake_time": "06:30",
            "out_of_bed_time": "06:45",
            "quality": 2,
            "alertness": 3,
            "medications": "None",
            "notes": "Had a hard time shutting my mind off. Woke up feeling tired."
          },
          {
            "date": "2026-06-23",
            "bed_time": "23:00",
            "light_out_time": "23:30",
            "latency_mins": 50,
            "awakenings": 3,
            "awake_mins": 35,
            "wake_time": "06:15",
            "out_of_bed_time": "06:30",
            "quality": 2,
            "alertness": 2,
            "medications": "None",
            "notes": "Felt frustrated lying in bed. NST: I will never get to sleep."
          },
          {
            "date": "2026-06-24",
            "bed_time": "22:45",
            "light_out_time": "23:00",
            "latency_mins": 30,
            "awakenings": 1,
            "awake_mins": 20,
            "wake_time": "06:00",
            "out_of_bed_time": "06:15",
            "quality": 3,
            "alertness": 4,
            "medications": "None",
            "notes": "Slightly better. Still woke up in the middle of the night."
          },
          {
            "date": "2026-06-25",
            "bed_time": "23:30",
            "light_out_time": "23:45",
            "latency_mins": 60,
            "awakenings": 2,
            "awake_mins": 50,
            "wake_time": "06:45",
            "out_of_bed_time": "07:00",
            "quality": 1,
            "alertness": 2,
            "medications": "None",
            "notes": "Awful night. Stressed about work tomorrow."
          },
          {
            "date": "2026-06-26",
            "bed_time": "23:00",
            "light_out_time": "23:15",
            "latency_mins": 40,
            "awakenings": 2,
            "awake_mins": 30,
            "wake_time": "06:30",
            "out_of_bed_time": "06:45",
            "quality": 3,
            "alertness": 3,
            "medications": "None",
            "notes": "Average sleep, felt a bit sleepy in afternoon."
          },
          {
            "date": "2026-06-27",
            "bed_time": "22:30",
            "light_out_time": "23:00",
            "latency_mins": 55,
            "awakenings": 3,
            "awake_mins": 45,
            "wake_time": "07:00",
            "out_of_bed_time": "07:30",
            "quality": 2,
            "alertness": 2,
            "medications": "None",
            "notes": "Stayed in bed too long trying to sleep. Felt groggy."
          },
          {
            "date": "2026-06-28",
            "bed_time": "23:00",
            "light_out_time": "23:15",
            "latency_mins": 35,
            "awakenings": 2,
            "awake_mins": 25,
            "wake_time": "06:30",
            "out_of_bed_time": "06:45",
            "quality": 3,
            "alertness": 3,
            "medications": "None",
            "notes": "Logging baseline complete. Sleep efficiency calculated around 79%."
          }
        ]"""

        Preferences.putString(context, Preferences.KEY_DIARIES, mockDiaries)
        Preferences.putString(context, Preferences.KEY_CBT_WEEK, "0")

        val humanJson = Preferences.getString(context, Preferences.KEY_HUMAN_MEMORY, "{}")
        val human = gson.fromJson(humanJson, JsonObject::class.java) ?: JsonObject()
        val mockProgress = JsonObject().apply {
            addProperty("current_week", 0)
            addProperty("current_week_description", "Baseline Logging completed (Ready for Week 1)")
            addProperty("sleep_window", "Not set")
            addProperty("average_sleep_duration", 320)
            addProperty("average_sleep_efficiency", 78.5)
        }
        human.add("cbt_progress", mockProgress)
        Preferences.putString(context, Preferences.KEY_HUMAN_MEMORY, gson.toJson(human))

        Toast.makeText(context, "Mock baseline data successfully generated!", Toast.LENGTH_SHORT).show()
        onUpdate()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = "Settings & Configuration",
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Manage your credentials, voice advisor preferences, and application storage.",
            color = TextSecondary,
            fontSize = 12.sp
        )

        // API Key Section
        GlassCard {
            Text(text = "DeepSeek API Key", color = Gold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("API Key (sk-...)") },
                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showKey = !showKey }) {
                        Icon(
                            imageVector = if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle key visibility"
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Purple, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "The key is stored only on your Pixel 10 secure local SharedPreferences.",
                color = TextMuted,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { handleSaveKey() },
                colors = ButtonDefaults.buttonColors(containerColor = Purple),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(imageVector = Icons.Default.Key, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Save API Key", color = Color.White)
            }
        }

        // Voice Section
        GlassCard {
            Text(text = "Voice Coach Settings", color = Gold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            // Speed rate
            Text(
                text = String.format(Locale.US, "Voice Speech Speed (Rate): %.2fx", voiceRate),
                color = TextSecondary,
                fontSize = 12.sp
            )
            Slider(
                value = voiceRate,
                onValueChange = { voiceRate = it },
                valueRange = 0.7f..1.3f,
                colors = SliderDefaults.colors(thumbColor = Purple, activeTrackColor = Purple)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Pitch
            Text(
                text = String.format(Locale.US, "Voice Pitch (Deepness): %.2f", voicePitch),
                color = TextSecondary,
                fontSize = 12.sp
            )
            Slider(
                value = voicePitch,
                onValueChange = { voicePitch = it },
                valueRange = 0.5f..1.2f,
                colors = SliderDefaults.colors(thumbColor = Purple, activeTrackColor = Purple)
            )
            Text(
                text = "Lower pitch yields a deeper, more confident male tone. Default is 0.9.",
                color = TextMuted,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { handleSaveVoiceSettings() },
                    colors = ButtonDefaults.buttonColors(containerColor = Purple),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.Hearing, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Save Settings", color = Color.White)
                }

                Button(
                    onClick = { handleTestVoice() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.05f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1.0f)
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                ) {
                    Text(text = "🔊 Test Voice", color = Color.White)
                }
            }
        }

        // Testing utilities Section
        GlassCard {
            Text(text = "CBT-I Testing Utilities", color = Gold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { handleLoadMockData() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.05f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                ) {
                    Icon(imageVector = Icons.Default.Storage, contentDescription = null, tint = Cyan)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Insert 7-Day Logs", color = Color.White, fontSize = 11.sp)
                }

                Button(
                    onClick = { handleResetData() },
                    colors = ButtonDefaults.buttonColors(containerColor = Red.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, Red.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = Red)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Reset App Data", color = Red, fontSize = 11.sp)
                }
            }
        }
    }
}
