package com.example.graphicspoc

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.SubdirectoryArrowRight
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Scenario2Screen(
    userName: String = "Rahul",
    isDark: Boolean = isSystemInDarkTheme(),
) {
    val bg = if (isDark) GeminiGradientsColors.DarkBackground else GeminiGradientsColors.LightBackground
    val cardBg = if (isDark) GeminiGradientsColors.DarkSurfaceCard else GeminiGradientsColors.LightSurfaceCard
    val textPrimary = if (isDark) GeminiGradientsColors.DarkTextPrimary else GeminiGradientsColors.LightTextPrimary
    val textSecondary = if (isDark) GeminiGradientsColors.DarkTextSecondary else GeminiGradientsColors.LightTextSecondary
    val searchHintColor = if (isDark) Color(0xFF8E918F) else Color(0xFF7A7A7A)
    Box(
        Modifier
            .fillMaxSize()
            .background(bg)
    ) {
        // ── 1. BOTTOM-UP MESH GRADIENT (Dark theme aware) ──
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .bottomUpLuminousMesh(isDark = isDark)
        ) {
            // ── Top Bar ──
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Menu, "Menu", tint = textPrimary)
                Spacer(Modifier.width(16.dp))
                Text("Gemini ", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = textPrimary)
                Text("Flash", fontSize = 20.sp, color = textSecondary)
                Icon(Icons.Default.ExpandMore, null, tint = textSecondary)
                Spacer(Modifier.weight(1f))
                Icon(Icons.Default.Edit, "New chat", tint = textPrimary)
            }
            // ── Center Greeting ──
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    "What's next, $userName?",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Normal,
                    color = textPrimary,
                )
            }
            // ── Suggestion Rows ──
            val suggestions = listOf(
                "Format a data table",
                "Pitch to brands for collaboration",
                "Find a budget smartphone",
            )
            suggestions.forEach { text ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.SubdirectoryArrowRight,
                        contentDescription = null,
                        tint = textSecondary,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(text, fontSize = 16.sp, color = textPrimary)
                }
            }
            Spacer(Modifier.height(12.dp))
            // ── Bottom "Ask Gemini" Pill Card ──
            Surface(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                shape = RoundedCornerShape(32.dp),
                color = cardBg,
                shadowElevation = if (isDark) 1.dp else 3.dp,
            ) {
                Row(
                    Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Add, "Add", tint = textSecondary, modifier = Modifier.size(26.dp))
                    Spacer(Modifier.width(14.dp))
                    Text("Ask Gemini", fontSize = 17.sp, color = searchHintColor)
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Default.Mic, "Voice input", tint = textSecondary, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(16.dp))
                    FilledIconButton(
                        onClick = {},
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (isDark) Color(0xFF004A77) else Color(0xFFE9EEF6)
                        )
                    ) {
                        Icon(
                            Icons.Default.GraphicEq,
                            contentDescription = "Live",
                            tint = if (isDark) Color(0xFFA8C7FA) else Color(0xFF1B66C9),
                        )
                    }
                }
            }
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}
