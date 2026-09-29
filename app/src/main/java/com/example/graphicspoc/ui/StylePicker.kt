package com.example.graphicspoc.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.graphicspoc.captions.CaptionChunker
import com.example.graphicspoc.captions.model.Word
import com.example.graphicspoc.captions.style.CaptionLayout
import com.example.graphicspoc.captions.style.CaptionRenderer
import com.example.graphicspoc.captions.style.CaptionStyle
import com.example.graphicspoc.captions.style.CaptionStyles

/** A looping sample sentence so every style card shows its animation. */
private val SAMPLE_WORDS: List<Word> = run {
    val text = "yeh caption ekdum mast lag raha hai"
    var t = 0L
    text.split(" ").map { w ->
        val d = 180L + w.length * 45L
        Word(w, t, t + d).also { t += d + 60 }
    }
}
private val SAMPLE_DURATION = SAMPLE_WORDS.last().endMs + 700

@Composable
fun StylePicker(
    selected: CaptionStyle,
    layout: CaptionLayout,
    onSelect: (CaptionStyle) -> Unit,
    onLayoutChange: (CaptionLayout) -> Unit,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "sample")
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = SAMPLE_DURATION.toFloat(),
        animationSpec = infiniteRepeatable(tween(SAMPLE_DURATION.toInt(), easing = LinearEasing), RepeatMode.Restart),
        label = "t",
    )

    Column(modifier) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(CaptionStyles.ALL, key = { it.id }) { style ->
                StyleCard(style, selected.id == style.id, { time }, onClick = { onSelect(style) })
            }
        }
        Spacer(Modifier.height(16.dp))
        LabeledSlider("Position", "Top", "Bottom", layout.verticalBias, 0.1f..0.9f) {
            onLayoutChange(layout.copy(verticalBias = it))
        }
        LabeledSlider("Size", "Small", "Big", layout.sizeScale, 0.6f..1.6f) {
            onLayoutChange(layout.copy(sizeScale = it))
        }
    }
}

@Composable
private fun StyleCard(style: CaptionStyle, isSelected: Boolean, time: () -> Float, onClick: () -> Unit) {
    val renderer = remember { CaptionRenderer() }
    val lines = remember(style) { CaptionChunker.chunk(SAMPLE_WORDS, style.maxWords, style.maxChars) }
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    Column(
        Modifier
            .width(112.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
    ) {
        Canvas(
            Modifier
                .size(112.dp, 150.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF3A3F58), Color(0xFF12131A))))
                .border(3.dp, borderColor, RoundedCornerShape(14.dp)),
        ) {
            val t = time().toLong()
            drawIntoCanvas {
                renderer.draw(it.nativeCanvas, size.width, size.height, t, lines, style, CaptionLayout(0.5f, 1f))
            }
        }
        Text(
            style.name,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(top = 6.dp, start = 4.dp),
        )
        Text(
            style.tagline,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    start: String,
    end: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row {
            Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            Text("$start ↔ $end", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(value = value, onValueChange = onChange, valueRange = range)
    }
}
