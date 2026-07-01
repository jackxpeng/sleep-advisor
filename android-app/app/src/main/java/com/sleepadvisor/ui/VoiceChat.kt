package com.sleepadvisor.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.sleepadvisor.agent.SleepAgent
import com.sleepadvisor.data.Preferences
import com.sleepadvisor.speech.SpeechSTT
import com.sleepadvisor.speech.SpeechTTS
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun VoiceChat(
    onStateChange: () -> Unit,
    revision: Int
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val gson = Gson()

    var isListening by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("idle") } // "idle", "listening", "thinking", "speaking"
    var messages by remember { mutableStateOf(listOf<ChatMsg>()) }
    var inputText by remember { mutableStateOf("") }
    var showKeyboard by remember { mutableStateOf(false) }
    var isVoiceModeActive by remember { mutableStateOf(false) }
    
    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current

    // STT & TTS Helpers
    var speechTTS by remember { mutableStateOf<SpeechTTS?>(null) }
    var speechSTT by remember { mutableStateOf<SpeechSTT?>(null) }

    // Helper to reload messages
    fun reloadHistory() {
        val historyJson = Preferences.getString(context, Preferences.KEY_CHAT_HISTORY, "[]")
        try {
            val arr = gson.fromJson(historyJson, JsonArray::class.java) ?: JsonArray()
            val list = mutableListOf<ChatMsg>()
            for (i in 0 until arr.size()) {
                val item = arr.get(i).asJsonObject
                list.add(
                    ChatMsg(
                        sender = item.get("sender").asString,
                        message = item.get("message").asString
                    )
                )
            }
            messages = list
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Initialize TTS
    DisposableEffect(Unit) {
        speechTTS = SpeechTTS(
            context = context,
            onStart = { status = "speaking" },
            onDone = {
                status = "idle"
                if (isVoiceModeActive) {
                    coroutineScope.launch {
                        speechSTT?.startListening()
                    }
                }
            },
            onError = { err ->
                status = "idle"
                println("TTS Error: $err")
            }
        )
        reloadHistory()

        onDispose {
            speechTTS?.shutdown()
            speechSTT?.stopListening()
        }
    }

    // Scroll transcript to bottom on new messages
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Speak helper
    val speakText: (String) -> Unit = { text ->
        val rate = Preferences.getString(context, Preferences.KEY_VOICE_RATE, "1.0").toFloatOrNull() ?: 1.0f
        val pitch = Preferences.getString(context, Preferences.KEY_VOICE_PITCH, "1.0").toFloatOrNull() ?: 1.0f
        speechTTS?.speak(text, rate, pitch)
    }

    // Run Agent logic in Background
    val handleSendMsg: (String) -> Unit = { text ->
        if (text.trim().isNotEmpty()) {
            if (status == "speaking") {
                speechTTS?.stop()
            }
            status = "thinking"
            inputText = ""
            keyboardController?.hide()
            
            // Add user message optimistically
            val userMsg = ChatMsg("user", text)
            messages = messages + userMsg

            coroutineScope.launch(Dispatchers.IO) {
                val reply = SleepAgent.runAgentTurn(context, text) {
                    coroutineScope.launch(Dispatchers.Main) {
                        onStateChange()
                    }
                }
                
                withContext(Dispatchers.Main) {
                    reloadHistory()
                    speakText(reply)
                }
            }
        }
    }

    // Initialize STT
    LaunchedEffect(isVoiceModeActive) {
        speechSTT = SpeechSTT(
            context = context,
            onStart = {
                isListening = true
                status = "listening"
            },
            onEnd = {
                isListening = false
                if (status == "listening") {
                    status = "idle"
                }
            },
            onResult = { result ->
                if (result.trim().isNotEmpty()) {
                    handleSendMsg(result)
                }
            },
            onPartial = { partial ->
                inputText = partial
                showKeyboard = true
            },
            onError = { err ->
                isListening = false
                status = "idle"
                println("STT Error: $err")
            }
        )
    }

    // Permissions check & launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            isVoiceModeActive = true
            speechSTT?.startListening()
        } else {
            isVoiceModeActive = false
            println("Microphone permission denied")
        }
    }

    val toggleMic = {
        val permissionCheck = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        )

        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            if (isVoiceModeActive) {
                isVoiceModeActive = false
                speechSTT?.stopListening()
                isListening = false
                status = "idle"
            } else {
                isVoiceModeActive = true
                speechTTS?.stop()
                speechSTT?.startListening()
            }
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val handleClearHistory = {
        Preferences.clearChatHistory(context)
        reloadHistory()
        onStateChange()
    }

    // Animations for Orb
    val infiniteTransition = rememberInfiniteTransition(label = "orb")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (status == "listening") 1.08f else if (status == "thinking") 1.05f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (status == "listening") 900 else 600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    val floatAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (status == "speaking") -8f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Sleep Coach voice session",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Speak or type your observations. I am listening.",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Toggle keyboard button
                IconButton(
                    onClick = { showKeyboard = !showKeyboard },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (showKeyboard) Gold else Color.White.copy(alpha = 0.05f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Keyboard,
                        contentDescription = "Toggle Keyboard",
                        tint = if (showKeyboard) BgDark else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Clear history button
                IconButton(
                    onClick = { handleClearHistory() },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.05f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Message,
                        contentDescription = "Clear History",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Coaching Orb Visualizer (Glass Panel)
        GlassCard(
            modifier = Modifier.weight(1f),
            glow = status == "listening" || status == "speaking"
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!showKeyboard) {
                    val orbSize = if (messages.isEmpty()) 140.dp else 90.dp
                    val iconSize = if (messages.isEmpty()) 38.dp else 26.dp

                    // Pulsing Orb Drawing
                    Box(
                        modifier = Modifier
                            .size(orbSize)
                            .offset(y = floatAnim.dp)
                            .clip(CircleShape)
                            .clickable { toggleMic() }
                            .background(
                                Brush.radialGradient(
                                    colors = when (status) {
                                        "listening" -> listOf(Color(0xFFFFD700), Color(0xFFFF8F00), BgDark)
                                        "thinking" -> listOf(Color(0xFF8A4FFF), PurpleGlow, BgDark)
                                        "speaking" -> listOf(Color(0xFFB5A6FF), Purple, BgDark)
                                        else -> listOf(Color(0xFFB388FF), Color(0xFF6200EA), BgDark)
                                    }
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // Outer glow canvas ring
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(
                                color = when (status) {
                                    "listening" -> Gold
                                    "thinking" -> Purple
                                    "speaking" -> Purple
                                    else -> Color.White.copy(alpha = 0.15f)
                                },
                                radius = (size.minDimension / 2f) * pulseScale,
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }

                        Icon(
                            imageVector = when (status) {
                                "listening" -> Icons.Default.Mic
                                "thinking" -> Icons.Default.Autorenew
                                "speaking" -> Icons.Default.VolumeUp
                                else -> Icons.Default.MicOff
                            },
                            contentDescription = "Orb Icon",
                            tint = if (status == "listening") BgDark else Color.White,
                            modifier = Modifier.size(iconSize)
                        )
                    }

                    // Waveform animations
                    AnimatedWaveform(isActive = status == "speaking" || status == "listening")

                    Text(
                        text = when (status) {
                            "listening" -> "Listening... Speak now."
                            "thinking" -> "Thinking... analyzing book guidelines..."
                            "speaking" -> "Speaking sleep advice..."
                            else -> "Tap the orb to start voice check-in"
                        },
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Scrollable Chat Transcript Box
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.2f))
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                        .padding(12.dp)
                ) {
                    if (messages.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "No conversation logs yet. Tap the microphone orb to report your sleep or ask a question.",
                                color = TextMuted,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(messages) { msg ->
                                ChatBubble(msg = msg, onSpeak = { speakText(msg.message) })
                            }
                        }
                    }
                }
            }
        }

        // Typing fall-back input
        if (showKeyboard) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text(text = "Type your message here...", color = TextSecondary, fontSize = 14.sp) },
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = Color(0x990A0E28),
                        unfocusedContainerColor = Color(0x990A0E28),
                        focusedIndicatorColor = Purple,
                        unfocusedIndicatorColor = Color.White.copy(alpha = 0.08f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp)),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        handleSendMsg(inputText)
                    })
                )

                Button(
                    onClick = { handleSendMsg(inputText) },
                    colors = ButtonDefaults.buttonColors(containerColor = Purple),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(54.dp)
                ) {
                    Icon(imageVector = Icons.Default.Send, contentDescription = "Send", tint = Color.White)
                }
            }
        }
    }
}

data class ChatMsg(val sender: String, val message: String)

@Composable
fun ChatBubble(msg: ChatMsg, onSpeak: () -> Unit) {
    val isUser = msg.sender == "user"
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (!isUser) {
                IconButton(onClick = onSpeak, modifier = Modifier.size(28.dp)) {
                    Icon(imageVector = Icons.Default.VolumeUp, contentDescription = "Speak", tint = Color(0xFFB5A6FF), modifier = Modifier.size(16.dp))
                }
            }

            Box(
                modifier = Modifier
                    .widthIn(max = 240.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 12.dp,
                            topEnd = 12.dp,
                            bottomStart = if (isUser) 12.dp else 2.dp,
                            bottomEnd = if (isUser) 2.dp else 12.dp
                        )
                    )
                    .background(if (isUser) Color.White.copy(alpha = 0.06f) else Purple.copy(alpha = 0.12f))
                    .border(
                        1.dp,
                        if (isUser) Color.White.copy(alpha = 0.06f) else Purple.copy(alpha = 0.15f),
                        RoundedCornerShape(
                            topStart = 12.dp,
                            topEnd = 12.dp,
                            bottomStart = if (isUser) 12.dp else 2.dp,
                            bottomEnd = if (isUser) 2.dp else 12.dp
                        )
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = msg.message,
                    color = if (isUser) TextPrimary else Color(0xFFDFD7FF),
                    fontSize = 13.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun AnimatedWaveform(isActive: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val waveHeights = List(7) { index ->
        infiniteTransition.animateFloat(
            initialValue = 8f,
            targetValue = if (isActive) 35f else 8f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 600,
                    delayMillis = index * 100,
                    easing = FastOutSlowInEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bar_$index"
        )
    }

    Row(
        modifier = Modifier
            .height(40.dp)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        waveHeights.forEach { height ->
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(height.value.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (isActive) Gold else Purple)
            )
        }
    }
}
