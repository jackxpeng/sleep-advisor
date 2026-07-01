package com.sleepadvisor.agent

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader

object RagSearch {
    private var bookChunks: List<String>? = null

    private fun loadChunks(context: Context): List<String> {
        if (bookChunks != null) return bookChunks!!
        
        return try {
            val assetManager = context.assets
            val inputStream = assetManager.open("book_chunks.json")
            val reader = InputStreamReader(inputStream, "UTF-8")
            val type = object : TypeToken<List<String>>() {}.type
            val chunks: List<String> = Gson().fromJson(reader, type)
            reader.close()
            bookChunks = chunks
            chunks
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun searchBook(context: Context, query: String, limit: Int = 4): List<String> {
        val chunks = loadChunks(context)
        if (chunks.isEmpty()) return emptyList()

        val cleanQuery = query.lowercase().trim()
        if (cleanQuery.isEmpty()) return emptyList()

        // Split query into words larger than 2 characters
        val words = cleanQuery.split(Regex("\\s+")).filter { it.length > 2 }
        
        if (words.isEmpty()) {
            return chunks.filter { it.lowercase().contains(cleanQuery) }.take(limit)
        }

        data class ScoredChunk(val chunk: String, val score: Int, val index: Int)

        val scored = chunks.mapIndexed { index, chunk ->
            val chunkLower = chunk.lowercase()
            var score = 0

            for (word in words) {
                if (chunkLower.contains(word)) {
                    score += 10
                    // Count occurrences
                    val occurrences = chunkLower.split(word).size - 1
                    score += occurrences.coerceAtMost(5)
                }
            }

            if (chunkLower.contains(cleanQuery)) {
                score += 50
            }

            ScoredChunk(chunk, score, index)
        }

        return scored
            .filter { it.score > 0 }
            .sortedWith(compareByDescending<ScoredChunk> { it.score }.thenBy { it.index })
            .take(limit)
            .map { it.chunk }
    }
}
