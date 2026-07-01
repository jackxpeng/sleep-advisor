package com.sleepadvisor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sleepadvisor.data.Preferences
import com.sleepadvisor.ui.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize local SharedPreferences schema
        Preferences.initialize(applicationContext)

        setContent {
            SleepAdvisorTheme {
                var currentTab by remember { mutableStateOf("dashboard") }
                var revisionTrigger by remember { mutableStateOf(0) }
                
                // Helper to trigger re-renders on sibling screens
                val onUpdate: () -> Unit = {
                    revisionTrigger++
                }

                // Get CBT week text for header badge
                var cbtWeekText by remember { mutableStateOf("Initial Interview") }
                
                LaunchedEffect(revisionTrigger) {
                    val weekVal = Preferences.getString(applicationContext, Preferences.KEY_CBT_WEEK, "-1")
                    cbtWeekText = when (weekVal) {
                        "-1" -> "Initial Interview"
                        "0" -> "Baseline Logging"
                        else -> "Week $weekVal Focus"
                    }
                }

                Scaffold(
                    topBar = {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(BgDark.copy(alpha = 0.4f))
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Custom Logo with Glow
                                    Image(
                                        painter = painterResource(id = R.drawable.sleep_logo),
                                        contentDescription = "Logo",
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .border(1.dp, Gold.copy(alpha = 0.3f), CircleShape)
                                    )
                                    
                                    Text(
                                        text = "Sleep Advisor",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = TextPrimary
                                    )
                                }

                                // CBT badge
                                Surface(
                                    color = Purple.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier.border(1.dp, PurpleGlow, RoundedCornerShape(20.dp))
                                ) {
                                    Text(
                                        text = cbtWeekText,
                                        color = Color(0xFFC0A9FF),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                            
                            // Fine border divider
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(Color.White.copy(alpha = 0.08f))
                            )
                        }
                    },
                    bottomBar = {
                        Column {
                            // Border divider
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(Color.White.copy(alpha = 0.08f))
                            )
                            
                            NavigationBar(
                                containerColor = Color(0xFF0A0E28).copy(alpha = 0.85f),
                                modifier = Modifier.height(72.dp)
                            ) {
                                // Dashboard
                                NavigationBarItem(
                                    selected = currentTab == "dashboard",
                                    onClick = { currentTab = "dashboard" },
                                    label = { Text("Dashboard", fontSize = 10.sp) },
                                    icon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Gold,
                                        selectedTextColor = Gold,
                                        unselectedIconColor = TextMuted,
                                        unselectedTextColor = TextMuted,
                                        indicatorColor = Color.Transparent
                                    )
                                )

                                // Advisor (Voice Chat)
                                NavigationBarItem(
                                    selected = currentTab == "chat",
                                    onClick = { currentTab = "chat" },
                                    label = { Text("Advisor", fontSize = 10.sp) },
                                    icon = { Icon(Icons.Default.Mic, contentDescription = null) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Gold,
                                        selectedTextColor = Gold,
                                        unselectedIconColor = TextMuted,
                                        unselectedTextColor = TextMuted,
                                        indicatorColor = Color.Transparent
                                    )
                                )

                                // Diary (Form)
                                NavigationBarItem(
                                    selected = currentTab == "diary",
                                    onClick = { currentTab = "diary" },
                                    label = { Text("Diary", fontSize = 10.sp) },
                                    icon = { Icon(Icons.Default.AddCircle, contentDescription = null) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Gold,
                                        selectedTextColor = Gold,
                                        unselectedIconColor = TextMuted,
                                        unselectedTextColor = TextMuted,
                                        indicatorColor = Color.Transparent
                                    )
                                )

                                // Relaxation (Breathe)
                                NavigationBarItem(
                                    selected = currentTab == "relaxation",
                                    onClick = { currentTab = "relaxation" },
                                    label = { Text("Breathe", fontSize = 10.sp) },
                                    icon = { Icon(Icons.Default.Spa, contentDescription = null) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Gold,
                                        selectedTextColor = Gold,
                                        unselectedIconColor = TextMuted,
                                        unselectedTextColor = TextMuted,
                                        indicatorColor = Color.Transparent
                                    )
                                )

                                // Memory (Letta memory)
                                NavigationBarItem(
                                    selected = currentTab == "memory",
                                    onClick = { currentTab = "memory" },
                                    label = { Text("Memory", fontSize = 10.sp) },
                                    icon = { Icon(Icons.Default.Psychology, contentDescription = null) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Gold,
                                        selectedTextColor = Gold,
                                        unselectedIconColor = TextMuted,
                                        unselectedTextColor = TextMuted,
                                        indicatorColor = Color.Transparent
                                    )
                                )

                                // Settings
                                NavigationBarItem(
                                    selected = currentTab == "settings",
                                    onClick = { currentTab = "settings" },
                                    label = { Text("Settings", fontSize = 10.sp) },
                                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Gold,
                                        selectedTextColor = Gold,
                                        unselectedIconColor = TextMuted,
                                        unselectedTextColor = TextMuted,
                                        indicatorColor = Color.Transparent
                                    )
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    // Radial gradient background
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color(0xFF161A3F), BgDark)
                                )
                            )
                            .padding(innerPadding)
                    ) {
                        when (currentTab) {
                            "dashboard" -> Dashboard(
                                onNavigate = { currentTab = it },
                                revision = revisionTrigger
                            )
                            "chat" -> VoiceChat(
                                onStateChange = onUpdate,
                                revision = revisionTrigger
                            )
                            "diary" -> SleepDiaryForm(
                                onUpdate = onUpdate,
                                revision = revisionTrigger
                            )
                            "relaxation" -> RelaxationRoom(
                                onUpdate = onUpdate,
                                revision = revisionTrigger
                            )
                            "memory" -> MemoryInspector(
                                onUpdate = onUpdate,
                                revision = revisionTrigger
                            )
                            "settings" -> Settings(
                                onUpdate = onUpdate,
                                revision = revisionTrigger
                            )
                        }
                    }
                }
            }
        }
    }
}
