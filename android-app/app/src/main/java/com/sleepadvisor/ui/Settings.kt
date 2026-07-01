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

    val viewModel: com.sleepadvisor.ui.viewmodel.SettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()

    val initialApiKey by viewModel.apiKey.collectAsState()
    val initialVoiceRate by viewModel.voiceRate.collectAsState()
    val initialVoicePitch by viewModel.voicePitch.collectAsState()

    var apiKey by remember { mutableStateOf("") }
    var showKey by remember { mutableStateOf(false) }
    var voiceRate by remember { mutableStateOf(1.0f) }
    var voicePitch by remember { mutableStateOf(0.9f) }

    // Sync from ViewModel
    LaunchedEffect(initialApiKey, initialVoiceRate, initialVoicePitch) {
        apiKey = initialApiKey
        voiceRate = initialVoiceRate
        voicePitch = initialVoicePitch
    }

    // TTS engine for testing
    var ttsTest by remember { mutableStateOf<SpeechTTS?>(null) }

    DisposableEffect(Unit) {
        ttsTest = SpeechTTS(context)
        onDispose {
            ttsTest?.shutdown()
        }
    }

    val handleSaveKey = {
        viewModel.setApiKey(apiKey.trim())
        Toast.makeText(context, "API Key updated successfully.", Toast.LENGTH_SHORT).show()
        onUpdate()
    }

    val handleSaveVoiceSettings = {
        viewModel.setVoiceRate(voiceRate)
        viewModel.setVoicePitch(voicePitch)
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
        viewModel.resetAllData()
        Toast.makeText(context, "All application data has been reset.", Toast.LENGTH_SHORT).show()
        onUpdate()
    }

    val handleLoadMockData = {
        viewModel.loadMockData {
            Toast.makeText(context, "Mock baseline data successfully generated!", Toast.LENGTH_SHORT).show()
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
