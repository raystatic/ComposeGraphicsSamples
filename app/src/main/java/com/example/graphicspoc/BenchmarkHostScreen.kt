package com.example.graphicspoc

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.android.awaitFrame

enum class BenchmarkMode {
    COMPOSE_MESH_GRADIENT,
    AGSL_NORTHERN_LIGHTS,
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun BenchmarkHostScreen() {
    var mode by remember { mutableStateOf(BenchmarkMode.COMPOSE_MESH_GRADIENT) }
    var fps by remember { mutableIntStateOf(60) }
    var frameTimeMs by remember { mutableFloatStateOf(16.6f) }

    // Real-time FPS & Frame Duration Monitor
    LaunchedEffect(Unit) {
        var frameCount = 0
        var prevTimeNanos = System.nanoTime()
        var lastFpsUpdate = System.nanoTime()

        while (true) {
            awaitFrame()
            val now = System.nanoTime()
            val dt = (now - prevTimeNanos) / 1_000_000f
            prevTimeNanos = now
            frameTimeMs = dt
            frameCount++

            if (now - lastFpsUpdate >= 1_000_000_000L) {
                fps = frameCount
                frameCount = 0
                lastFpsUpdate = now
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Render Active Mode Under Test
        when (mode) {
            BenchmarkMode.COMPOSE_MESH_GRADIENT -> {
                Scenario1Screen(isDark = false)
            }
            BenchmarkMode.AGSL_NORTHERN_LIGHTS -> {
                Box(Modifier.fillMaxSize()) {
                    NorthernLightsShaderEngine(isDark = false)
                }
            }
        }

        // Floating Benchmark Controller & Stats Card
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xEE1E1F20),
            shadowElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "FPS: $fps",
                            fontSize = 18.sp,
                            color = if (fps >= 58) Color(0xFF88DE42) else Color(0xFFFF5252)
                        )
                        Text(
                            text = "Frame Time: %.2f ms".format(frameTimeMs),
                            fontSize = 14.sp,
                            color = Color(0xFFC4C7C5)
                        )
                    }

                    // Mode Toggle Buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = mode == BenchmarkMode.COMPOSE_MESH_GRADIENT,
                            onClick = { mode = BenchmarkMode.COMPOSE_MESH_GRADIENT },
                            label = { Text("Mesh") }
                        )
                        FilterChip(
                            selected = mode == BenchmarkMode.AGSL_NORTHERN_LIGHTS,
                            onClick = { mode = BenchmarkMode.AGSL_NORTHERN_LIGHTS },
                            label = { Text("AGSL") }
                        )
                    }
                }
            }
        }
    }
}
