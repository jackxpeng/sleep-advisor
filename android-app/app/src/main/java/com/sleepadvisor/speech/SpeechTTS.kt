package com.sleepadvisor.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class SpeechTTS(
    private val context: Context,
    private val onStart: () -> Unit = {},
    private val onDone: () -> Unit = {},
    private val onError: (String) -> Unit = {}
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context, this)
    private var isInitialized = false

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                onError("US English language is not supported or missing data")
            } else {
                isInitialized = true
                setupProgressListener()
            }
        } else {
            onError("Initialization of TTS failed")
        }
    }

    private fun setupProgressListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                onStart()
            }

            override fun onDone(utteranceId: String?) {
                onDone()
            }

            override fun onError(utteranceId: String?) {
                onError("TTS playback error")
            }
        })
    }

    private fun cleanMarkdown(text: String): String {
        var clean = text
        // Replace bullet points at the beginning of lines/paragraphs with spaces
        clean = clean.replace(Regex("(?m)^\\s*[-*+]\\s+"), " ")
        // Remove bold/italic markup symbols
        clean = clean.replace("**", "")
        clean = clean.replace("__", "")
        clean = clean.replace("*", "")
        clean = clean.replace("_", "")
        // Remove header markup symbols at the beginning of lines
        clean = clean.replace(Regex("(?m)^\\s*#+\\s+"), "")
        // Replace multiple newlines/spaces with a single space
        clean = clean.replace(Regex("\\s+"), " ")
        return clean.trim()
    }

    fun speak(text: String, rate: Float = 1.0f, pitch: Float = 1.0f) {
        if (!isInitialized) {
            onError("TTS engine not ready yet")
            return
        }

        tts?.apply {
            setSpeechRate(rate)
            setPitch(pitch)
            
            // Try to set a male voice if available
            val voices = voices
            if (!voices.isNullOrEmpty()) {
                val maleVoice = voices.find { voice ->
                    val name = voice.name.lowercase()
                    val isEnglish = voice.locale.language == "en"
                    isEnglish && (
                        name.contains("male") ||
                        name.contains("x-iom") || // US Male
                        name.contains("x-iol") || // US Male
                        name.contains("x-tpf") || // US Male
                        name.contains("x-rjs") || // UK Male
                        name.contains("x-gdo") || // UK Male
                        name.contains("x-kdf") || // UK Male
                        name.contains("x-ere") || // India Male
                        name.contains("x-djd")    // Australia Male
                    )
                }
                if (maleVoice != null) {
                    voice = maleVoice
                } else {
                    // Fallback to any English voice that has "male" in name
                    val fallbackMale = voices.find { voice ->
                        voice.locale.language == "en" && voice.name.lowercase().contains("male")
                    }
                    if (fallbackMale != null) {
                        voice = fallbackMale
                    }
                }
            }

            val params = android.os.Bundle()
            params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "advisor_speech")
            speak(cleanMarkdown(text), TextToSpeech.QUEUE_FLUSH, params, "advisor_speech")
        }
    }

    fun stop() {
        if (isInitialized) {
            tts?.stop()
        }
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}
