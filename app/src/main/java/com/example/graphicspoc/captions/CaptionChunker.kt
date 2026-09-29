package com.example.graphicspoc.captions

import com.example.graphicspoc.captions.model.CaptionLine
import com.example.graphicspoc.captions.model.Word

/**
 * Groups timed words into on-screen caption lines.
 *
 * Short-form captions read best in small bursts, so a line is closed when any of these happen:
 * it reaches [maxWords] or [maxChars], the speaker pauses for [pauseBreakMs], or the recogniser
 * ended a sentence.
 */
object CaptionChunker {

    fun chunk(
        words: List<Word>,
        maxWords: Int,
        maxChars: Int = 28,
        pauseBreakMs: Long = 600,
        lingerMs: Long = 400,
    ): List<CaptionLine> {
        val groups = mutableListOf<MutableList<Word>>()
        var current = mutableListOf<Word>()
        var chars = 0
        for (word in words) {
            if (word.text.isBlank()) continue
            val prev = current.lastOrNull()
            val wouldBeChars = chars + word.text.length + if (current.isEmpty()) 0 else 1
            val breakHere = prev != null && (
                current.size >= maxWords ||
                    (wouldBeChars > maxChars && current.isNotEmpty()) ||
                    word.startMs - prev.endMs >= pauseBreakMs ||
                    prev.endsSentence
                )
            if (breakHere) {
                groups += current
                current = mutableListOf()
                chars = 0
            }
            chars += word.text.length + if (current.isEmpty()) 0 else 1
            current += word
        }
        if (current.isNotEmpty()) groups += current

        return groups.mapIndexed { i, group ->
            val start = group.first().startMs
            val spokenEnd = group.last().endMs
            val nextStart = groups.getOrNull(i + 1)?.first()?.startMs
            // Keep the line up a little after the last word, but never overlap the next line.
            val end = if (nextStart != null) minOf(spokenEnd + lingerMs, nextStart) else spokenEnd + lingerMs
            CaptionLine(group, start, maxOf(end, spokenEnd))
        }
    }

    /** The line visible at [timeMs], if any. Lines are sorted and non-overlapping. */
    fun lineAt(lines: List<CaptionLine>, timeMs: Long): CaptionLine? = lines.getOrNull(indexAt(lines, timeMs))

    /** Index of the line visible at [timeMs], or -1. */
    fun indexAt(lines: List<CaptionLine>, timeMs: Long): Int {
        var lo = 0
        var hi = lines.lastIndex
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            val line = lines[mid]
            when {
                timeMs < line.startMs -> hi = mid - 1
                timeMs >= line.endMs -> lo = mid + 1
                else -> return mid
            }
        }
        return -1
    }

    /**
     * Replaces the text of the words in [startMs, endMs) with [newText], keeping the timing.
     * If the word count changed, the original time span is spread across the new words in
     * proportion to their length, so highlighting still lines up with speech.
     */
    fun editRange(words: List<Word>, startMs: Long, endMs: Long, newText: String): List<Word> {
        val first = words.indexOfFirst { it.startMs >= startMs }
        if (first == -1) return words
        var last = first
        while (last + 1 < words.size && words[last + 1].startMs < endMs) last++
        val old = words.subList(first, last + 1)
        val tokens = newText.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }

        val replacement: List<Word> = when {
            tokens.isEmpty() -> emptyList()
            tokens.size == old.size -> old.zip(tokens) { w, t -> w.copy(text = t) }
            else -> {
                val spanStart = old.first().startMs
                val spanEnd = old.last().endMs
                val totalChars = tokens.sumOf { it.length }.coerceAtLeast(1)
                var cursor = spanStart
                tokens.mapIndexed { i, t ->
                    val dur = (spanEnd - spanStart) * t.length / totalChars
                    val wStart = cursor
                    val wEnd = if (i == tokens.lastIndex) spanEnd else cursor + dur
                    cursor = wEnd
                    Word(
                        text = t, startMs = wStart, endMs = wEnd, original = t,
                        endsSentence = i == tokens.lastIndex && old.last().endsSentence,
                    )
                }
            }
        }
        return words.subList(0, first) + replacement + words.subList(last + 1, words.size)
    }
}
