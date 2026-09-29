package com.example.graphicspoc.captions.transcribe

import com.example.graphicspoc.captions.model.Word
import org.json.JSONObject
import kotlin.math.roundToLong

/**
 * Parses an OpenAI-compatible `verbose_json` transcription response into timed words.
 *
 * Prefers the word-level timestamps. If the server only returned segments, each segment's
 * duration is spread across its words proportionally to their length.
 */
object WhisperResponseParser {

    private data class Segment(val text: String, val startMs: Long, val endMs: Long)

    fun parse(json: String): List<Word> {
        val root = JSONObject(json)
        val segments = root.optJSONArray("segments")?.let { arr ->
            (0 until arr.length()).map { i ->
                val s = arr.getJSONObject(i)
                Segment(s.optString("text").trim(), s.optDouble("start").toMs(), s.optDouble("end").toMs())
            }
        }.orEmpty()

        val wordsArr = root.optJSONArray("words")
        if (wordsArr != null && wordsArr.length() > 0) {
            val words = (0 until wordsArr.length()).mapNotNull { i ->
                val w = wordsArr.getJSONObject(i)
                val text = w.optString("word").trim()
                if (text.isEmpty()) null else Word(text, w.optDouble("start").toMs(), w.optDouble("end").toMs())
            }
            return markSentenceEnds(words, segments)
        }

        if (segments.isNotEmpty()) {
            return segments.flatMap { seg -> spread(seg) }
        }

        // Last resort: plain text with no timing at all.
        val text = root.optString("text").trim()
        return if (text.isEmpty()) emptyList() else spread(Segment(text, 0, text.split(' ').size * 400L))
    }

    private fun Double.toMs(): Long = if (isNaN()) 0 else (this * 1000).roundToLong()

    private fun spread(seg: Segment): List<Word> {
        val tokens = seg.text.split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return emptyList()
        val total = tokens.sumOf { it.length }.coerceAtLeast(1)
        var cursor = seg.startMs
        return tokens.mapIndexed { i, t ->
            val end = if (i == tokens.lastIndex) seg.endMs else cursor + (seg.endMs - seg.startMs) * t.length / total
            Word(t, cursor, end, endsSentence = i == tokens.lastIndex).also { cursor = end }
        }
    }

    /** A word ends a sentence if it is the last word inside a recogniser segment. */
    private fun markSentenceEnds(words: List<Word>, segments: List<Segment>): List<Word> {
        if (segments.isEmpty()) return words
        val segmentEnds = segments.map { it.endMs }
        return words.mapIndexed { i, w ->
            val next = words.getOrNull(i + 1) ?: return@mapIndexed w.copy(endsSentence = true)
            // The segment boundary falls between this word and the next one.
            val closes = segmentEnds.any { end -> end in (w.startMs + 1)..next.startMs }
            if (closes) w.copy(endsSentence = true) else w
        }
    }
}
