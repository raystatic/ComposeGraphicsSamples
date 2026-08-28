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
import androidx.compose.ui.graphics.lerp
import kotlin.math.cos
import kotlin.math.sin

/** Rotates a base hue backwards by [phase]
 *  keeping it light & pastel like the reference. */
private fun pastel(baseHue: Float, phase: Float, sat: Float, value: Float): Color {
    val h = ((baseHue - phase) % 360f + 360f) % 360f
    return Color.hsv(h, sat, value)
}

private fun sampleRobinRibbon(t: Float, stops: List<Pair<Float, Color>>): Color {
    val normT = ((t % 1f) + 1f) % 1f
    for (i in 0 until stops.size - 1) {
        val (pos1, col1) = stops[i]
        val (pos2, col2) = stops[i + 1]
        if (normT in pos1..pos2) {
            val fraction = (normT - pos1) / (pos2 - pos1)
            return lerp(col1, col2, fraction)
        }
    }
    return stops.first().second
}

@Composable
fun AnimatedMeshGradient(
    modifier: Modifier = Modifier,
    isDark: Boolean = isSystemInDarkTheme(),
    periodMillis: Int = 10000, // 10s smooth drift matching Robin's scrollSpeed = 0.206
) {
    val transition = rememberInfiniteTransition(label = "auroraFlow")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(periodMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )
    val stops = if (isDark) DARK_STOPS else LIGHT_STOPS
    val surfaceColor = if (isDark) Color(0xFF131314) else Color.White
    val angle = progress * 2f * Math.PI.toFloat()
    val painter = remember(isDark, progress) {
        MeshGradientPainter(rows = 3, columns = 3, hasBicubicColor = true) {
            // ── ZOOMED-IN SAMPLING (Matching Robin's gradientZoom = 6.85f) ──
            // Total span across width is ~0.14 (14%), not 0.82 (82%)
            val span = 0.14f
            val c0 = sampleRobinRibbon(progress + 0.00f * span, stops)
            val c1 = sampleRobinRibbon(progress + 0.33f * span, stops)
            val c2 = sampleRobinRibbon(progress + 0.66f * span, stops)
            val c3 = sampleRobinRibbon(progress + 1.00f * span, stops)
            // Organic wave offsets
            val w1 = sin(angle) * 0.03f
            val w2 = cos(angle * 1.3f) * 0.02f
            val w3 = sin(angle * 0.7f) * 0.03f
            // ── Row 0: Top Edge (y = 0.00f) ──
            setVertex(0, 0, Offset(0.00f, 0f), c0)
            setVertex(0, 1, Offset(0.33f + w1, 0f), c1)
            setVertex(0, 2, Offset(0.66f + w2, 0f), c2)
            setVertex(0, 3, Offset(1.00f, 0f), c3)
            // ── Row 1: Upper Middle Dome (y ≈ 0.15–0.22f) ──
            val fade1 = if (isDark) 0.85f else 0.90f
            setVertex(1, 0, Offset(0.00f, 0.14f + w3), lerp(surfaceColor, c0, fade1))
            setVertex(1, 1, Offset(0.33f + w2, 0.22f + w1), lerp(surfaceColor, c1, fade1)) // arched center
            setVertex(1, 2, Offset(0.66f + w1, 0.20f + w2), lerp(surfaceColor, c2, fade1)) // arched center
            setVertex(1, 3, Offset(1.00f, 0.14f + w3), lerp(surfaceColor, c3, fade1))
            // ── Row 2: Lower Dome Boundary (y ≈ 0.26–0.35f) ──
            val fade2 = if (isDark) 0.25f else 0.30f
            setVertex(2, 0, Offset(0.00f, 0.26f), lerp(surfaceColor, c0, fade2))
            setVertex(2, 1, Offset(0.33f, 0.36f + w2), lerp(surfaceColor, c1, fade2)) // deep center glow
            setVertex(2, 2, Offset(0.66f, 0.34f + w1), lerp(surfaceColor, c2, fade2)) // deep center glow
            setVertex(2, 3, Offset(1.00f, 0.26f), lerp(surfaceColor, c3, fade2))
            // ── Row 3: Pure Surface Color ──
            setVertex(3, 0, Offset(0.00f, 0.44f), surfaceColor)
            setVertex(3, 1, Offset(0.33f, 0.48f), surfaceColor)
            setVertex(3, 2, Offset(0.66f, 0.48f), surfaceColor)
            setVertex(3, 3, Offset(1.00f, 0.44f), surfaceColor)
        }
    }
    Box(modifier.paint(painter))
}


// ── Light Theme Ribbon ──
val LIGHT_STOPS = listOf(
    0.0000f to Color(0xFF3C90FF), // Google Blue
    0.1476f to Color(0xFF3C90FF), // Google Blue hold
    0.4009f to Color(0xFFF96BD6), // Gemini Spark Pink / Magenta
    0.4844f to Color(0xFFFFCF03), // Warm Gold / Amber
    0.5769f to Color(0xFFFFE921), // Bright Yellow
    0.6344f to Color(0xFF88DE42), // Electric Lime
    0.6875f to Color(0xFF60D673), // Fresh Green
    0.7734f to Color(0xFF60D673), // Fresh Green hold
    0.8211f to Color(0xFF00BDD2), // Cyan / Teal
    0.8713f to Color(0xFF4FA0FF), // Sky Blue
    0.9600f to Color(0xFF3C90FF), // Google Blue
    1.0000f to Color(0xFF3C90FF), // Google Blue (loops seamlessly)
)

// ── Dark Theme Ribbon ──
val DARK_STOPS = listOf(
    0.0000f to Color(0xFF336EF3), // Deep Vivid Blue
    0.3388f to Color(0xFF336EF3), // Deep Vivid Blue hold
    0.4356f to Color(0xFFF63BB3), // Rich Magenta / Neon Pink
    0.4844f to Color(0xFFFEC700), // Deep Gold
    0.5769f to Color(0xFFFFDB0F), // Rich Warm Yellow
    0.6344f to Color(0xFF57C200), // Vibrant Lime
    0.6875f to Color(0xFF00AF57), // Deep Emerald Green
    0.7734f to Color(0xFF00AF57), // Deep Emerald Green hold
    0.8211f to Color(0xFF009AAA), // Deep Cyan
    0.8713f to Color(0xFF3279F9), // Azure Blue
    0.9600f to Color(0xFF336EF3), // Deep Vivid Blue
    1.0000f to Color(0xFF336EF3), // Deep Vivid Blue (loops seamlessly)
)





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
