package com.example.graphicspoc.captions.model

/**
 * One spoken word with the timing reported by the speech recogniser.
 *
 * [original] is exactly what the recogniser returned (often Devanagari for Hindi speech),
 * [text] is what we show on screen (Hinglish / Roman script).
 */
data class Word(
    val text: String,
    val startMs: Long,
    val endMs: Long,
    val original: String = text,
    /** True when the recogniser closed a sentence/segment after this word. */
    val endsSentence: Boolean = false,
)

/** A group of words that is shown on screen together. */
data class CaptionLine(
    val words: List<Word>,
    val startMs: Long,
    val endMs: Long,
) {
    val text: String get() = words.joinToString(" ") { it.text }
}
