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

    val viewModel: com.sleepadvisor.ui.viewmodel.VoiceChatViewModel = androidx.lifecycle.viewmodel.compose.viewModel()

    val messages by viewModel.messages.collectAsState()
    val status by viewModel.status.collectAsState()
    val isVoiceModeActive by viewModel.isVoiceModeActive.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val rmsDb by viewModel.rmsDb.collectAsState()

    var showKeyboard by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current

    // STT & TTS Helpers
    var speechTTS by remember { mutableStateOf<SpeechTTS?>(null) }
    var speechSTT by remember { mutableStateOf<SpeechSTT?>(null) }

    // Initialize TTS
    DisposableEffect(Unit) {
        speechTTS = SpeechTTS(
            context = context,
            onStart = { viewModel.setStatus("speaking") },
            onDone = {
                viewModel.setStatus("idle")
                if (isVoiceModeActive) {
                    coroutineScope.launch {
                        speechSTT?.startListening()
                    }
                }
            },
            onError = { err ->
                viewModel.setStatus("idle")
                println("TTS Error: $err")
            }
        )

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
            if (speechTTS?.isSpeaking() == true) {
                speechTTS?.stopAndNotify()
            }
            keyboardController?.hide()
            viewModel.sendMessage(text) { reply ->
                speakText(reply)
            }
        }
    }

    // Initialize STT and enable barge-in
    LaunchedEffect(isVoiceModeActive) {
        if (isVoiceModeActive) {
            speechSTT = SpeechSTT(
                context = context,
                onStart = {
                    viewModel.setStatus("listening")
                },
                onEnd = {
                    if (viewModel.status.value == "listening") {
                        viewModel.setStatus("idle")
                    }
                },
                onResult = { result ->
                    if (result.trim().isNotEmpty()) {
                        handleSendMsg(result)
                    }
                },
                onPartial = { partial ->
                    // Barge-in: if the user interrupts while TTS is speaking, immediately stop TTS
                    if (speechTTS?.isSpeaking() == true) {
                        speechTTS?.stopAndNotify()
                    }
                    viewModel.setInputText(partial)
                },
                onError = { err ->
                    viewModel.setStatus("idle")
                    println("STT Error: $err")
                },
                onRmsChanged = { rms ->
                    viewModel.setRmsDb(rms)
                }
            )
            speechSTT?.startListening()
        } else {
            speechSTT?.stopListening()
            speechSTT = null
            viewModel.setRmsDb(0f)
            if (viewModel.status.value == "listening") {
                viewModel.setStatus("idle")
            }
        }
    }

    // Permissions check & launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.setVoiceModeActive(true)
        } else {
            viewModel.setVoiceModeActive(false)
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
                viewModel.setVoiceModeActive(false)
            } else {
                speechTTS?.stop()
                viewModel.setVoiceModeActive(true)
            }
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val handleClearHistory = {
        viewModel.clearHistory()
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

                    val glowColors = when (status) {
                        "listening" -> listOf(Color(0xFFFFD700), Color(0xFFFF8F00), BgDark)
                        "thinking" -> listOf(Color(0xFF8A4FFF), PurpleGlow, BgDark)
                        "speaking" -> listOf(Color(0xFFB5A6FF), Purple, BgDark)
                        else -> listOf(Color(0xFFB388FF), Color(0xFF6200EA), BgDark)
                    }

                    val ringColor = when (status) {
                        "listening" -> Gold
                        "thinking" -> Purple
                        "speaking" -> Purple
                        else -> Color.White.copy(alpha = 0.15f)
                    }

                    // Pulsing Orb Drawing
                    Box(
                        modifier = Modifier
                            .size(orbSize)
                            .offset(y = floatAnim.dp)
                            .clip(CircleShape)
                            .clickable { toggleMic() }
                            .background(
                                Brush.radialGradient(
                                    colors = glowColors
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // Outer glow canvas ring
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(
                                color = ringColor,
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
                    AnimatedWaveform(rmsDb = rmsDb, status = status)

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
                    onValueChange = { viewModel.setInputText(it) },
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
@Composable
fun ChatBubble(msg: com.sleepadvisor.domain.model.ChatMessage, onSpeak: () -> Unit) {
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
fun AnimatedWaveform(rmsDb: Float, status: String) {
    val bars = remember { mutableStateListOf(8f, 8f, 8f, 8f, 8f, 8f, 8f, 8f, 8f) }
    
    LaunchedEffect(rmsDb, status) {
        if (status == "listening") {
            val height = (rmsDb + 2f).coerceAtLeast(0f) * 4f + 8f
            bars.removeAt(0)
            bars.add(height.coerceIn(8f, 50f))
        } else if (status == "speaking") {
            // Give a soft speaking animation wave
            for (i in 0 until bars.size) {
                bars[i] = (12f + Math.sin((System.currentTimeMillis() / 150.0) + i).toFloat() * 10f).coerceAtLeast(8f)
            }
        } else {
            for (i in 0 until bars.size) {
                bars[i] = 8f
            }
        }
    }

    Row(
        modifier = Modifier
            .height(55.dp)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        bars.forEach { h ->
            val animatedHeight by animateFloatAsState(
                targetValue = h,
                animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f)
            )
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height(animatedHeight.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (status == "listening") Gold else Purple)
            )
        }
    }
}
