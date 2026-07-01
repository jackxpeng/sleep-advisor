package com.sleepadvisor.domain.model

import com.google.gson.JsonObject

data class ChatMessage(
    val sender: String,
    val message: String,
    val timestamp: String,
    val toolCalls: String? = null
) {

    companion object {

        fun fromJson(json: JsonObject): ChatMessage {
            fun str(key: String, default: String): String =
                if (json.has(key) && !json.get(key).isJsonNull) json.get(key).asString else default

            fun strOrNull(key: String): String? =
                if (json.has(key) && !json.get(key).isJsonNull) json.get(key).asString else null

            return ChatMessage(
                sender = str("sender", "assistant"),
                message = str("message", ""),
                timestamp = str("timestamp", ""),
                toolCalls = strOrNull("toolCalls")
            )
        }

        fun toJson(msg: ChatMessage): JsonObject = JsonObject().apply {
            addProperty("sender", msg.sender)
            addProperty("message", msg.message)
            addProperty("timestamp", msg.timestamp)
            if (msg.toolCalls != null) {
                addProperty("toolCalls", msg.toolCalls)
            }
        }
    }
}
