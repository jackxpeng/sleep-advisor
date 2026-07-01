package com.sleepadvisor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Design system colors matching App.css
val BgDark = Color(0xFF050716)
val BgDeep = Color(0xFF0A0E28)
val PanelBg = Color(0x730D1434) // 45% opacity
val PanelBgHover = Color(0x8C161F4A) // 55% opacity
val TextPrimary = Color(0xFFF2F5FC)
val TextSecondary = Color(0xFF8E9BB4)
val TextMuted = Color(0xFF5E6D8A)

// Custom fallback colors
private val CustomGold = Color(0xFFFFBE0B)
private val CustomPurple = Color(0xFF8A4FFF)
val Cyan = Color(0xFF06D6A0)
val Red = Color(0xFFEF476F)

// Theme-aware colors that adapt to system wallpaper theme
val Purple: Color
    @Composable
    get() = MaterialTheme.colorScheme.primary

val Gold: Color
    @Composable
    get() = MaterialTheme.colorScheme.secondary

val PurpleGlow: Color
    @Composable
    get() = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)

val GoldGlow: Color
    @Composable
    get() = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)

private val SleepColorScheme = darkColorScheme(
    primary = CustomPurple,
    onPrimary = Color.White,
    secondary = CustomGold,
    onSecondary = BgDark,
    background = BgDark,
    onBackground = TextPrimary,
    surface = PanelBg,
    onSurface = TextPrimary,
    error = Red,
    onError = Color.White
)

@Composable
fun SleepAdvisorTheme(
    content: @Composable () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val dynamicColor = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
    val colorScheme = if (dynamicColor) {
        androidx.compose.material3.dynamicDarkColorScheme(context)
    } else {
        SleepColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    glow: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val borderAlpha = if (glow) 0.35f else 0.12f
    val borderColor = if (glow) Purple else Color.White
    val bg = if (glow) PanelBgHover else PanelBg
    
    val baseModifier = modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(24.dp))
        .background(bg)
        .border(1.dp, borderColor.copy(alpha = borderAlpha), RoundedCornerShape(24.dp))
        .padding(16.dp)

    if (onClick != null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .clickable { onClick() }
        ) {
            Column(modifier = baseModifier) {
                content()
            }
        }
    } else {
        Column(modifier = baseModifier) {
            content()
        }
    }
}
