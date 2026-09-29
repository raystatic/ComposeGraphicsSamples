package com.example.graphicspoc.captions.transcribe

import com.example.graphicspoc.captions.hinglish.HinglishTransliterator
import com.example.graphicspoc.captions.model.Word
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.DataOutputStream
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/**
 * Sends the extracted audio to an OpenAI-compatible `/audio/transcriptions` endpoint
 * (Whisper), asks for word-level timestamps, then converts every word to Hinglish.
 *
 * `temperature=0` keeps Whisper deterministic: it decodes the audio instead of paraphrasing.
 */
class WhisperTranscriber(private val settings: SttSettings) : Transcriber {

    override suspend fun transcribe(audioWav: File): List<Word> = withContext(Dispatchers.IO) {
        require(settings.apiKey.isNotBlank()) { "Add your ${settings.provider.label} API key in Settings" }
        if (audioWav.length() > MAX_UPLOAD_BYTES) {
            throw IOException("Audio is too long for one request (max ~13 min). Trim the video first.")
        }

        val boundary = "----HinglishCaptions${UUID.randomUUID()}"
        val conn = (URL("${settings.provider.baseUrl}/audio/transcriptions").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 30_000
            readTimeout = 180_000
            setRequestProperty("Authorization", "Bearer ${settings.apiKey.trim()}")
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            setChunkedStreamingMode(64 * 1024)
        }

        try {
            DataOutputStream(conn.outputStream).use { out ->
                fun field(name: String, value: String) {
                    out.writeBytes("--$boundary\r\n")
                    out.writeBytes("Content-Disposition: form-data; name=\"$name\"\r\n\r\n")
                    out.write(value.toByteArray(Charsets.UTF_8))
                    out.writeBytes("\r\n")
                }
                field("model", settings.provider.model)
                field("response_format", "verbose_json")
                field("timestamp_granularities[]", "word")
                field("timestamp_granularities[]", "segment")
                field("temperature", "0")
                if (settings.language.isNotBlank()) field("language", settings.language)
                if (settings.vocabularyHint.isNotBlank()) field("prompt", settings.vocabularyHint)

                out.writeBytes("--$boundary\r\n")
                out.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"audio.wav\"\r\n")
                out.writeBytes("Content-Type: audio/wav\r\n\r\n")
                audioWav.inputStream().use { it.copyTo(out) }
                out.writeBytes("\r\n--$boundary--\r\n")
            }

            val code = conn.responseCode
            val body = (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) throw IOException("Transcription failed ($code): ${errorMessage(body)}")

            WhisperResponseParser.parse(body).map { w ->
                w.copy(text = HinglishTransliterator.transliterateWord(w.original))
            }
        } finally {
            conn.disconnect()
        }
    }

    private fun errorMessage(body: String): String = runCatching {
        JSONObject(body).getJSONObject("error").getString("message")
    }.getOrDefault(body.take(300))

    companion object {
        private const val MAX_UPLOAD_BYTES = 25L * 1024 * 1024
    }
}
