package com.example.graphicspoc

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.MeshGradientPainter

/** Rotates a base hue backwards by [phase]
 *  keeping it light & pastel like the reference. */
private fun pastel(baseHue: Float, phase: Float, sat: Float, value: Float): Color {
    val h = ((baseHue - phase) % 360f + 360f) % 360f
    return Color.hsv(h, sat, value)
}

@Composable
fun AnimatedMeshGradient(
    modifier: Modifier = Modifier,
    isDark: Boolean = isSystemInDarkTheme(),
    periodMillis: Int = 5000,
) {
    val transition = rememberInfiniteTransition(label = "meshHue")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(periodMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phase",
    )

    val surfaceColor = if (isDark) GeminiGradientsColors.DarkBackground else GeminiGradientsColors.LightBackground
    val valScale = if (isDark) 0.65f else 0.96f
    val satScale = if (isDark) 0.60f else 0.40f

    val painter = remember(isDark) {
        MeshGradientPainter(rows = 3, columns = 3) {
            // Row 0 — top edge
            setVertex(0, 0, Offset(0.00f, 0f), pastel(210f, phase, satScale, valScale))
            setVertex(0, 1, Offset(0.33f, 0f), pastel(226f, phase, satScale * 0.9f, valScale))
            setVertex(0, 2, Offset(0.66f, 0f), pastel(198f, phase, satScale * 0.95f, valScale))
            setVertex(0, 3, Offset(1.00f, 0f), pastel(214f, phase, satScale * 0.85f, valScale))

            // Row 1 — softer blend
            setVertex(1, 0, Offset(0.00f, 0.22f), pastel(206f, phase, satScale * 0.55f, valScale * 0.9f))
            setVertex(1, 1, Offset(0.33f, 0.24f), pastel(222f, phase, satScale * 0.50f, valScale * 0.9f))
            setVertex(1, 2, Offset(0.66f, 0.20f), pastel(196f, phase, satScale * 0.55f, valScale * 0.9f))
            setVertex(1, 3, Offset(1.00f, 0.22f), pastel(212f, phase, satScale * 0.45f, valScale * 0.9f))

            // Row 2 — desaturating edge
            setVertex(2, 0, Offset(0.00f, 0.45f), pastel(206f, phase, satScale * 0.15f, valScale * 0.5f))
            setVertex(2, 1, Offset(0.33f, 0.45f), pastel(222f, phase, satScale * 0.12f, valScale * 0.5f))
            setVertex(2, 2, Offset(0.66f, 0.45f), pastel(196f, phase, satScale * 0.15f, valScale * 0.5f))
            setVertex(2, 3, Offset(1.00f, 0.45f), pastel(212f, phase, satScale * 0.12f, valScale * 0.5f))

            // Row 3 — fades into bottom surface
            setVertex(3, 0, Offset(0.00f, 1f), surfaceColor)
            setVertex(3, 1, Offset(0.33f, 1f), surfaceColor)
            setVertex(3, 2, Offset(0.66f, 1f), surfaceColor)
            setVertex(3, 3, Offset(1.00f, 1f), surfaceColor)
        }
    }

    Box(modifier.paint(painter))
}



@Composable
fun Modifier.bottomUpLuminousMesh(
    isDark: Boolean = isSystemInDarkTheme(),
    backgroundColor: Color = if (isDark) GeminiGradientsColors.DarkBackground else GeminiGradientsColors.LightBackground,
    accentColor: Color = if (isDark) GeminiGradientsColors.DarkLuminousAccent else GeminiGradientsColors.LightLuminousAccent,
): Modifier {
    val painter = remember(backgroundColor, accentColor) {
        MeshGradientPainter(
            rows = 2,
            columns = 2,
            hasBicubicColor = true
        ) {
            setVertex(0, 0, Offset(0.0f, 0.0f), backgroundColor)
            setVertex(0, 1, Offset(0.5f, 0.0f), backgroundColor)
            setVertex(0, 2, Offset(1.0f, 0.0f), backgroundColor)

            setVertex(1, 0, Offset(0.0f, 0.6f), backgroundColor)
            setVertex(1, 1, Offset(0.5f, 0.75f), backgroundColor)
            setVertex(1, 2, Offset(1.0f, 0.6f), backgroundColor)

            setVertex(2, 0, Offset(0.0f, 1.0f), accentColor)
            setVertex(2, 1, Offset(0.5f, 1.0f), accentColor)
            setVertex(2, 2, Offset(1.0f, 1.0f), accentColor)
        }
    }
    return this.paint(painter)
}



object GeminiGradientsColors {
    // ── Light Theme Palette ──
    val LightBackground = Color(0xFFFFFFFF)
    val LightSurfaceCard = Color(0xFFFFFFFF)
    val LightTextPrimary = Color(0xFF1F1F1F)
    val LightTextSecondary = Color(0xFF444746)
    val LightIconTint = Color(0xFF444746)
    val LightPillContainer = Color(0xFFE9EEF6)
    val LightLuminousAccent = Color(0xFF8DC5FD) // Sky Blue
    // ── Dark Theme Palette ──
    val DarkBackground = Color(0xFF131314)       // Gemini dark surface
    val DarkSurfaceCard = Color(0xFF1E1F20)      // Gemini elevated dark card
    val DarkTextPrimary = Color(0xFFE3E3E3)
    val DarkTextSecondary = Color(0xFFC4C7C5)
    val DarkIconTint = Color(0xFFC4C7C5)
    val DarkPillContainer = Color(0xFF282A2C)
    val DarkLuminousAccent = Color(0xFF1B3B5A)   // Deep Atmospheric Blue wash
}
