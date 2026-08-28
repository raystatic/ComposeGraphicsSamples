package com.example.graphicspoc

import android.os.Build
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Scenario1Screen(isDark: Boolean = isSystemInDarkTheme()) {
    val bg = if (isDark) GeminiGradientsColors.DarkBackground else GeminiGradientsColors.LightBackground
    val cardBg = if (isDark) GeminiGradientsColors.DarkSurfaceCard else GeminiGradientsColors.LightSurfaceCard
    val textPrimary = if (isDark) GeminiGradientsColors.DarkTextPrimary else GeminiGradientsColors.LightTextPrimary
    val textSecondary = if (isDark) GeminiGradientsColors.DarkTextSecondary else GeminiGradientsColors.LightTextSecondary
    val chipBg = if (isDark) Color(0x33FFFFFF) else Color.White.copy(alpha = 0.55f)

    Box(Modifier.fillMaxSize().background(bg)) {
        // Gradient layer (automatically uses Dark Mode if isDark = true)
        AnimatedMeshGradient(Modifier.fillMaxSize(), isDark = isDark)

        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            // Top Bar
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(44.dp).background(chipBg, RoundedCornerShape(50)),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Default.Menu, "Menu", tint = textPrimary) }
                Spacer(Modifier.width(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Gemini ", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = textPrimary)
                    Text("Flash", fontSize = 20.sp, color = textSecondary)
                    Icon(Icons.Default.ExpandMore, null, tint = textSecondary, modifier = Modifier.padding(start = 2.dp))
                }
                Spacer(Modifier.weight(1f))
                Row(
                    Modifier.background(chipBg, RoundedCornerShape(28.dp)).padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Edit, "New chat", tint = textPrimary)
                    Spacer(Modifier.width(18.dp))
                    Icon(Icons.Default.MoreVert, "More", tint = textPrimary)
                }
            }

            Spacer(Modifier.height(24.dp))

            // User prompt bubble
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.End) {
                Text(
                    "Plan a 5-day Kerala itinerary",
                    Modifier.widthIn(max = 260.dp).background(chipBg, RoundedCornerShape(24.dp)).padding(horizontal = 20.dp, vertical = 14.dp),
                    fontSize = 16.sp, color = textPrimary,
                )
            }

            Spacer(Modifier.weight(1f))

            // Bottom Input Card
            Surface(
                Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(28.dp),
                color = cardBg,
                shadowElevation = 3.dp,
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        "Design a scenic 5-day travel itinerary for Kerala covering houseboats and hill stations. Start with a brief travel tip…",
                        fontSize = 16.sp, color = textPrimary,
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Add, "Add", tint = textSecondary, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.weight(1f))
                        FilledIconButton(
                            onClick = {},
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = if (isDark) GeminiGradientsColors.DarkPillContainer else GeminiGradientsColors.LightPillContainer
                            )
                        ) {
                            Icon(Icons.Default.Stop, "Stop", tint = textSecondary)
                        }
                    }
                }
            }
        }
    }
}

