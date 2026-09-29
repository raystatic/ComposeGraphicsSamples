package com.example.graphicspoc.ui

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.graphicspoc.captions.model.CaptionLine
import com.example.graphicspoc.captions.style.CaptionLayout
import com.example.graphicspoc.captions.style.CaptionRenderer
import com.example.graphicspoc.captions.style.CaptionStyle

/**
 * Plays the video with the captions drawn live on top by [CaptionRenderer] – the exact same
 * drawing code the exporter burns into the file.
 */
@OptIn(UnstableApi::class)
@Composable
fun VideoPreview(
    uri: Uri,
    aspect: Float,
    lines: List<CaptionLine>,
    style: CaptionStyle,
    layout: CaptionLayout,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val player = remember(uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            repeatMode = Player.REPEAT_MODE_ALL
            prepare()
            playWhenReady = true
        }
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(player, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) player.pause()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            player.release()
        }
    }

    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var isPlaying by remember { mutableStateOf(true) }
    LaunchedEffect(player) {
        while (true) {
            withFrameMillis { }
            positionMs = player.currentPosition
            durationMs = player.duration.coerceAtLeast(0)
            isPlaying = player.isPlaying
        }
    }
    val renderer = remember { CaptionRenderer() }

    Box(modifier, contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .aspectRatio(aspect, matchHeightConstraintsFirst = aspect < 1f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                    player.playWhenReady = !player.playWhenReady
                },
        ) {
            AndroidView(
                factory = {
                    PlayerView(it).apply {
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        this.player = player
                    }
                },
                update = { it.player = player },
                modifier = Modifier.fillMaxSize(),
            )
            Canvas(Modifier.fillMaxSize()) {
                val t = positionMs // read here so only the draw phase invalidates each frame
                drawIntoCanvas { c ->
                    renderer.draw(c.nativeCanvas, size.width, size.height, t, lines, style, layout)
                }
            }
            if (!isPlaying) {
                Text(
                    "▶",
                    color = Color.White,
                    fontSize = 48.sp,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .background(Color(0x66000000), RoundedCornerShape(50))
                        .padding(horizontal = 22.dp, vertical = 6.dp),
                )
            }
            if (durationMs > 0) {
                LinearProgressIndicator(
                    progress = { positionMs.toFloat() / durationMs },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(3.dp),
                    color = Color.White,
                    trackColor = Color(0x33FFFFFF),
                )
            }
        }
    }
}
