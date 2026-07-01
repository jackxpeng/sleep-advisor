package com.sleepadvisor.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sleepadvisor.data.MemoryRepository
import com.sleepadvisor.data.SettingsRepository
import com.sleepadvisor.domain.model.ChatMessage
import com.sleepadvisor.agent.AgentRouter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

class VoiceChatViewModel(application: Application) : AndroidViewModel(application) {
    private val memoryRepo = MemoryRepository(application)
    private val settingsRepo = SettingsRepository(application)

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _status = MutableStateFlow("idle")
    val status: StateFlow<String> = _status.asStateFlow()

    private val _isVoiceModeActive = MutableStateFlow(false)
    val isVoiceModeActive: StateFlow<Boolean> = _isVoiceModeActive.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _rmsDb = MutableStateFlow(0f)
    val rmsDb: StateFlow<Float> = _rmsDb.asStateFlow()

    init {
        reloadHistory()
    }

    fun setInputText(text: String) {
        _inputText.value = text
    }

    fun setVoiceModeActive(active: Boolean) {
        _isVoiceModeActive.value = active
    }

    fun setStatus(newStatus: String) {
        _status.value = newStatus
    }

    fun setRmsDb(value: Float) {
        _rmsDb.value = value
    }

    fun reloadHistory() {
        _messages.value = memoryRepo.getChatHistory()
    }

    fun clearHistory() {
        memoryRepo.clearChatHistory()
        reloadHistory()
    }

    fun sendMessage(text: String, onReplyReceived: (String) -> Unit) {
        if (text.isBlank()) return
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val userMsg = ChatMessage(sender = "user", message = text, timestamp = dateStr)
        memoryRepo.appendMessage(userMsg)
        reloadHistory()

        _status.value = "thinking"
        _inputText.value = ""

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val reply = AgentRouter.runAgentTurn(getApplication(), text) {
                    viewModelScope.launch(Dispatchers.Main) {
                        reloadHistory()
                    }
                }
                viewModelScope.launch(Dispatchers.Main) {
                    reloadHistory()
                    onReplyReceived(reply)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                viewModelScope.launch(Dispatchers.Main) {
                    _status.value = "idle"
                }
            }
        }
    }
}
