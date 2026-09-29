package com.example.graphicspoc.captions.transcribe

import com.example.graphicspoc.captions.model.Word
import java.io.File

/** Speech-to-text over the real audio track. Implementations must return word timings. */
interface Transcriber {
    suspend fun transcribe(audioWav: File): List<Word>
}

/** Where the Whisper model is hosted. Both expose the same OpenAI-compatible endpoint. */
enum class SttProvider(val label: String, val baseUrl: String, val model: String, val keyHint: String) {
    GROQ("Groq · whisper-large-v3", "https://api.groq.com/openai/v1", "whisper-large-v3", "gsk_…"),
    OPENAI("OpenAI · whisper-1", "https://api.openai.com/v1", "whisper-1", "sk-…"),
}

data class SttSettings(
    val provider: SttProvider = SttProvider.GROQ,
    val apiKey: String = "",
    /**
     * "hi" makes Whisper write Hindi in Devanagari (never Urdu script), which our
     * transliterator then turns into Hinglish. English words usually stay in Latin script.
     */
    val language: String = "hi",
    /** Optional spelling hints: names, brands, slang the creator uses. Not a caption source. */
    val vocabularyHint: String = "",
)
