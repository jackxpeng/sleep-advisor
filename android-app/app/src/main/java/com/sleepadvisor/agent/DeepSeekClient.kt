package com.sleepadvisor.agent

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

object DeepSeekClient {
    private val gson = Gson()
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    fun callChatCompletion(
        apiKey: String,
        messages: JsonArray,
        tools: JsonArray?,
        temperature: Double = 1.0
    ): JsonObject {
        val requestBodyJson = JsonObject().apply {
            addProperty("model", "deepseek-chat")
            add("messages", messages)
            if (tools != null && tools.size() > 0) {
                add("tools", tools)
                addProperty("tool_choice", "auto")
            }
            addProperty("temperature", temperature)
        }

        val requestBody = gson.toJson(requestBodyJson).toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url("https://api.deepseek.com/chat/completions")
            .post(requestBody)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorMsg = response.body?.string() ?: "Unknown error"
                throw IOException("API call failed with code ${response.code}: $errorMsg")
            }
            val bodyString = response.body?.string() ?: throw IOException("Empty response body")
            return gson.fromJson(bodyString, JsonObject::class.java)
        }
    }
}
