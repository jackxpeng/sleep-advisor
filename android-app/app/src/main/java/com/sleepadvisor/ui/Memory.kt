package com.sleepadvisor.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.sleepadvisor.data.Preferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MemoryInspector(
    onUpdate: () -> Unit,
    revision: Int
) {
    val context = LocalContext.current
    val gson = Gson()

    val memoryRepo = remember { com.sleepadvisor.data.MemoryRepository(context) }
    
    var humanMemory by remember { mutableStateOf("{}") }
    var personaMemory by remember { mutableStateOf("{}") }
    var archivalMemories by remember { mutableStateOf(listOf<JsonObject>()) }
    var newFact by remember { mutableStateOf("") }

    fun loadMemory() {
        humanMemory = memoryRepo.getHumanMemory()
        personaMemory = memoryRepo.getPersonaMemory()
        archivalMemories = memoryRepo.getArchivalMemories()
    }

    LaunchedEffect(revision) {
        loadMemory()
    }

    val handleAddFact = {
        if (newFact.trim().isNotEmpty()) {
            memoryRepo.insertArchival(newFact.trim())
            newFact = ""
            loadMemory()
            onUpdate()
            Toast.makeText(context, "Fact inserted successfully.", Toast.LENGTH_SHORT).show()
        }
    }

    val handleDeleteFact: (Int) -> Unit = { index ->
        memoryRepo.deleteArchival(index)
        loadMemory()
        onUpdate()
        Toast.makeText(context, "Fact deleted.", Toast.LENGTH_SHORT).show()
    }

    fun formatTimestamp(isoStr: String): String {
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            val dateObj = parser.parse(isoStr) ?: Date()
            val formatter = SimpleDateFormat("MM/dd/yyyy hh:mm a", Locale.US)
            formatter.format(dateObj)
        } catch (e: Exception) {
            try {
                val parser2 = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                val dateObj = parser2.parse(isoStr) ?: Date()
                val formatter = SimpleDateFormat("MM/dd/yyyy hh:mm a", Locale.US)
                formatter.format(dateObj)
            } catch (e2: Exception) {
                isoStr
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Letta explanation header
        GlassCard(glow = true) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Psychology, contentDescription = null, tint = Purple, modifier = Modifier.size(24.dp))
                Text(
                    text = "Letta Cognitive Architecture: The Sleep Advisor uses a Core Memory loop. As you speak to it, it dynamically updates its human profile (Core Memory) and writes permanent facts about your sleep habits (Archival Memory) to maintain deep long-term context.",
                    color = Color(0xFFDED3FF),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }

        // Core memory block row / grid (stacked on portrait mobile)
        GlassCard {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = Gold, modifier = Modifier.size(18.dp))
                Text(text = "Core Memory - Human Profile", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            Text(text = "What the advisor currently remembers about your sleep status.", color = TextSecondary, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = humanMemory,
                        color = Color(0xFFA9B7C6),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        GlassCard {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(imageVector = Icons.Default.SmartButton, contentDescription = null, tint = Purple, modifier = Modifier.size(18.dp))
                Text(text = "Core Memory - Assistant Persona", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            Text(text = "The advisor's current behavioral rules and parameters.", color = TextSecondary, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = personaMemory,
                        color = Color(0xFFA9B7C6),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Archival Memory Block
        GlassCard {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(imageVector = Icons.Default.Storage, contentDescription = null, tint = Cyan, modifier = Modifier.size(18.dp))
                Text(text = "Archival Memory Database", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Text(text = "A semantic storage log of permanent facts and events compiled over time.", color = TextSecondary, fontSize = 12.sp)
            
            Spacer(modifier = Modifier.height(16.dp))

            // Add manual fact input row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newFact,
                    onValueChange = { newFact = it },
                    placeholder = { Text(text = "Add a new permanent fact about yourself...", color = TextSecondary, fontSize = 13.sp) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Purple, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                    modifier = Modifier.weight(1f)
                )

                Button(
                    onClick = { handleAddFact() },
                    colors = ButtonDefaults.buttonColors(containerColor = Purple),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // List of archival memories
            if (archivalMemories.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "No archival facts recorded yet. The advisor will insert them automatically during conversation.", color = TextMuted, fontSize = 12.sp)
                }
            } else {
                archivalMemories.forEachIndexed { i, fact ->
                    val timestamp = fact.get("created_at")?.let { if (it.isJsonNull) "" else it.asString } ?: ""
                    val contentStr = fact.get("content")?.let { if (it.isJsonNull) "" else it.asString } ?: ""

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .background(Color.White.copy(alpha = 0.02f))
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = formatTimestamp(timestamp), color = TextMuted, fontSize = 10.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = contentStr, color = TextPrimary, fontSize = 13.sp)
                        }
                        IconButton(onClick = { handleDeleteFact(i) }, modifier = Modifier.size(32.dp)) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Red, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}
