package com.example.graphicspoc.captions.style

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import com.example.graphicspoc.captions.CaptionChunker
import com.example.graphicspoc.captions.model.CaptionLine
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min

/**
 * Draws the caption for a given moment onto an [android.graphics.Canvas].
 *
 * The same class paints the Compose preview (via `drawIntoCanvas { it.nativeCanvas }`) and the
 * frames of the exported video (via a Media3 `CanvasOverlay`), so both always match.
 * Not thread-safe: use one instance per drawing thread.
 */
class CaptionRenderer {

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    private val box = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()

    private class PlacedWord(
        val text: String,
        val index: Int,
        var x: Float = 0f,
        var width: Float = 0f,
        var row: Int = 0,
    )

    fun draw(
        canvas: Canvas,
        width: Float,
        height: Float,
        timeMs: Long,
        lines: List<CaptionLine>,
        style: CaptionStyle,
        layout: CaptionLayout,
    ) {
        val lineIndex = CaptionChunker.indexAt(lines, timeMs)
        if (lineIndex < 0) return
        drawLine(canvas, width, height, timeMs, lines[lineIndex], lineIndex, style, layout)
    }

    fun drawLine(
        canvas: Canvas,
        width: Float,
        height: Float,
        timeMs: Long,
        line: CaptionLine,
        lineIndex: Int,
        style: CaptionStyle,
        layout: CaptionLayout,
    ) {
        val textSize = min(width, height) * style.sizeRatio * layout.sizeScale
        configurePaints(style, textSize)

        val activeIndex = line.words.indexOfLast { it.startMs <= timeMs }
        val placed = line.words.mapIndexedNotNull { i, w ->
            val t = displayText(w.text, style)
            if (t.isEmpty()) null else PlacedWord(t, i)
        }
        if (placed.isEmpty()) return

        // --- Word-wrap into centred rows ------------------------------------------------
        val space = fill.measureText(" ")
        val maxRowWidth = width * 0.86f
        val rowWidths = mutableListOf(0f)
        for (p in placed) {
            p.width = fill.measureText(p.text)
            val row = rowWidths.lastIndex
            val needed = if (rowWidths[row] == 0f) p.width else rowWidths[row] + space + p.width
            if (needed > maxRowWidth && rowWidths[row] > 0f) {
                rowWidths += p.width
            } else {
                rowWidths[row] = needed
            }
            p.row = rowWidths.lastIndex
        }
        val rowCursor = FloatArray(rowWidths.size) { (width - rowWidths[it]) / 2f }
        for (p in placed) {
            p.x = rowCursor[p.row]
            rowCursor[p.row] += p.width + space
        }

        val fm = fill.fontMetrics
        val rowHeight = (fm.descent - fm.ascent) * 1.08f
        val blockHeight = rowHeight * rowWidths.size
        val margin = height * 0.05f
        val top = (height * layout.verticalBias - blockHeight / 2f)
            .coerceIn(margin, (height - margin - blockHeight).coerceAtLeast(margin))
        fun baseline(row: Int) = top + row * rowHeight - fm.ascent + (rowHeight - (fm.descent - fm.ascent)) / 2f

        // --- Line entry bounce -----------------------------------------------------------
        canvas.save()
        if (style.entryBounce) {
            val s = 0.8f + 0.2f * easeOutBack(((timeMs - line.startMs) / 160f).coerceIn(0f, 1f))
            canvas.scale(s, s, width / 2f, top + blockHeight / 2f)
        }

        // --- Row boxes -------------------------------------------------------------------
        style.boxColor?.let { color ->
            box.color = color
            val padH = textSize * 0.35f
            val padV = textSize * 0.12f
            val radius = textSize * 0.25f
            for (row in rowWidths.indices) {
                val visible = placed.filter { it.row == row && isVisible(it.index, activeIndex, style) }
                if (visible.isEmpty()) continue
                val left = visible.first().x
                val right = visible.last().x + visible.last().width
                val y = top + row * rowHeight
                rect.set(left - padH, y - padV, right + padH, y + rowHeight + padV)
                canvas.drawRoundRect(rect, radius, radius, box)
            }
        }

        // --- Words -------------------------------------------------------------------------
        val baseColor = style.boxTextColor ?: style.textColor
        for (p in placed) {
            if (!isVisible(p.index, activeIndex, style)) continue
            val word = line.words[p.index]
            val isActive = p.index == activeIndex && style.highlightMode != HighlightMode.NONE ||
                (style.maxWords == 1 && p.index == activeIndex)
            val y = baseline(p.row)
            val centerX = p.x + p.width / 2f
            val centerY = y + (fm.ascent + fm.descent) / 2f

            canvas.save()
            if (p.index == activeIndex && style.popScale != 1f) {
                val k = easeOutBack(((timeMs - word.startMs) / 140f).coerceIn(0f, 1f))
                val s = 1f + (style.popScale - 1f) * k
                canvas.scale(s, s, centerX, centerY)
            }

            if (isActive && style.highlightMode == HighlightMode.PILL) {
                val padH = textSize * 0.18f
                val padV = textSize * 0.06f
                rect.set(p.x - padH, y + fm.ascent - padV, p.x + p.width + padH, y + fm.descent + padV)
                box.color = style.highlightColor
                canvas.drawRoundRect(rect, textSize * 0.22f, textSize * 0.22f, box)
            }

            val color = when {
                style.cycleColors.isNotEmpty() -> style.cycleColors[lineIndex % style.cycleColors.size]
                isActive && style.highlightMode != HighlightMode.PILL -> style.highlightColor
                else -> baseColor
            }
            val alpha = if (p.index > activeIndex) style.upcomingAlpha else 1f

            if (style.strokeColor != null && style.strokeRatio > 0f) {
                stroke.color = style.strokeColor
                stroke.alpha = (255 * alpha).toInt()
                canvas.drawText(p.text, p.x, y, stroke)
            }

            fill.shader = null
            if (style.gradient.size >= 2 && !isActive && style.cycleColors.isEmpty()) {
                fill.color = baseColor
                fill.shader = LinearGradient(
                    0f, y + fm.ascent, 0f, y + fm.descent,
                    style.gradient.toIntArray(), null, Shader.TileMode.CLAMP,
                )
            } else {
                fill.color = color
            }
            fill.alpha = (255 * alpha).toInt()
            if (style.highlightMode == HighlightMode.GLOW) {
                val glow = if (isActive) style.highlightColor else style.shadowColor
                fill.setShadowLayer(textSize * style.shadowRatio * (if (isActive) 1.4f else 1f), 0f, 0f, glow)
            }
            canvas.drawText(p.text, p.x, y, fill)
            canvas.restore()
        }
        canvas.restore()
    }

    private fun isVisible(index: Int, activeIndex: Int, style: CaptionStyle) =
        !style.progressiveReveal || index <= maxOf(activeIndex, 0)

    private fun configurePaints(style: CaptionStyle, textSize: Float) {
        val typeface = typefaceFor(style.font)
        for (p in listOf(fill, stroke)) {
            p.typeface = typeface
            p.textSize = textSize
            p.letterSpacing = style.letterSpacing
        }
        stroke.strokeWidth = textSize * style.strokeRatio
        fill.shader = null
        if (style.shadowRatio > 0f) {
            val dy = if (style.highlightMode == HighlightMode.GLOW) 0f else textSize * 0.04f
            fill.setShadowLayer(textSize * style.shadowRatio, 0f, dy, style.shadowColor)
            stroke.setShadowLayer(textSize * style.shadowRatio, 0f, dy, style.shadowColor)
        } else {
            fill.clearShadowLayer()
            stroke.clearShadowLayer()
        }
    }

    companion object {
        private val PUNCTUATION = Regex("[.,!?;:।\"“”]+")

        fun displayText(text: String, style: CaptionStyle): String {
            var t = text
            if (style.stripPunctuation) t = t.replace(PUNCTUATION, "")
            if (style.uppercase) t = t.uppercase()
            return t.trim()
        }

        // Shared between the UI thread (preview) and the export GL thread.
        private val typefaces = ConcurrentHashMap<FontChoice, Typeface>()

        fun typefaceFor(font: FontChoice): Typeface = typefaces.getOrPut(font) {
            when (font) {
                FontChoice.BLACK -> Typeface.create("sans-serif-black", Typeface.NORMAL)
                FontChoice.BOLD -> Typeface.create("sans-serif", Typeface.BOLD)
                FontChoice.CONDENSED -> Typeface.create("sans-serif-condensed", Typeface.BOLD)
                FontChoice.MONO -> Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                FontChoice.SERIF -> Typeface.create(Typeface.SERIF, Typeface.BOLD)
            }
        }

        private fun easeOutBack(t: Float): Float {
            val c1 = 1.70158f
            val c3 = c1 + 1f
            val x = t - 1f
            return 1f + c3 * x * x * x + c1 * x * x
        }
    }
}
