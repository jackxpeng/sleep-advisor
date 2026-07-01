package com.sleepadvisor.agent

import android.content.Context
import com.sleepadvisor.data.DiaryRepository
import com.sleepadvisor.data.MemoryRepository
import com.sleepadvisor.data.SettingsRepository
import com.sleepadvisor.domain.model.ChatMessage
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

object AgentRouter {

    suspend fun runAgentTurn(
        context: Context,
        userMessage: String,
        onStateChange: () -> Unit
    ): String = coroutineScope {
        val settingsRepo = SettingsRepository(context)
        val apiKey = settingsRepo.getApiKey()
        if (apiKey.isEmpty() || !apiKey.startsWith("sk-")) {
            return@coroutineScope "Please enter a valid DeepSeek API key (starting with 'sk-') in the Settings tab."
        }

        val diaryRepo = DiaryRepository(context)
        val memoryRepo = MemoryRepository(context)

        // 1. Record User message in Recall Memory
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val userMsg = ChatMessage(sender = "user", message = userMessage, timestamp = dateStr)
        memoryRepo.appendMessage(userMsg)

        // 2. Fetch last 20 messages for context
        val recentHistory = memoryRepo.getRecentHistory(20)

        // 3. Run both Extraction Agent and Advisory Agent concurrently
        val extractionDeferred = async(Dispatchers.IO) {
            ExtractionAgent.runExtraction(context, userMessage, recentHistory, diaryRepo, memoryRepo)
        }
        val advisoryDeferred = async(Dispatchers.IO) {
            AdvisoryAgent.runAdvisory(context, userMessage, recentHistory, memoryRepo, diaryRepo)
        }

        val extractionResult = extractionDeferred.await()
        val advisoryResult = advisoryDeferred.await()

        // 4. Save Advisory Response to History (annotating toolCalls if extraction succeeded)
        val toolCallsMetadata = if (extractionResult.startsWith("EXTRACTED:")) {
            extractionResult.substringAfter("EXTRACTED:")
        } else {
            null
        }

        val assistantMsg = ChatMessage(
            sender = "assistant",
            message = advisoryResult,
            timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()),
            toolCalls = toolCallsMetadata
        )
        memoryRepo.appendMessage(assistantMsg)

        // 5. Trigger updates in sibling composables
        withContext(Dispatchers.Main) {
            onStateChange()
        }

        advisoryResult
    }
}
