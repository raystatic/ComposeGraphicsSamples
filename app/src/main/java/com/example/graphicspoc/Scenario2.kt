package com.example.graphicspoc

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
fun Scenario2Screen(userName: String = "Rahul") {
    val context = LocalContext.current

    // ── 1. ROBIN LUMINOUS DRAWABLE INTEGRATION (Bottom wash) ──
    val luminousDrawable = remember(context) {
        LuminousBackgroundDrawable(context, isTopDown = false)
    }

    Box(
        Modifier
            .fillMaxSize()
            .drawBehind {
                luminousDrawable.setBounds(0, 0, size.width.toInt(), size.height.toInt())
                luminousDrawable.draw(drawContext.canvas.nativeCanvas)
            }
    ) {
        // ── 2. FOREGROUND CONTENT (Hardcoded UI structure from doc) ──
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top Bar
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Menu, "Menu")
                Spacer(Modifier.width(16.dp))
                Text("Gemini ", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Text("Flash", fontSize = 20.sp, color = Color(0xFF444746))
                Icon(Icons.Default.ExpandMore, null)
                Spacer(Modifier.weight(1f))
                Icon(Icons.Default.Edit, "New chat")
            }

            // Center Greeting
            Column(
                Modifier.weight(1f).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    "What's next, $userName?",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF1F1F1F),
                )
            }

            // Suggestion Rows
            val suggestions = listOf(
                "Format a data table",
                "Pitch to brands for collaboration",
                "Find a budget smartphone",
            )
            suggestions.forEach { text ->
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.SubdirectoryArrowRight,
                        null,
                        tint = Color(0xFF444746),
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(text, fontSize = 16.sp, color = Color(0xFF1F1F1F))
                }
            }

            Spacer(Modifier.height(12.dp))

            // Bottom "Ask Gemini" Pill
            Surface(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                shape = RoundedCornerShape(32.dp),
                color = Color.White,
                shadowElevation = 3.dp,
            ) {
                Row(
                    Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Add, "Add", Modifier.size(26.dp))
                    Spacer(Modifier.width(14.dp))
                    Text("Ask Gemini", fontSize = 17.sp, color = Color(0xFF7A7A7A))
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Default.Mic, "Voice input", Modifier.size(24.dp))
                    Spacer(Modifier.width(16.dp))
                    FilledIconButton(
                        onClick = {},
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color(0xFFE9EEF6)
                        )
                    ) {
                        Icon(Icons.Default.GraphicEq, "Live", tint = Color(0xFF1B66C9))
                    }
                }
            }
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}
