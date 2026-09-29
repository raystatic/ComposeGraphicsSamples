package com.example.graphicspoc.ui

import android.app.Application
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.graphicspoc.captions.CaptionChunker
import com.example.graphicspoc.captions.audio.AudioExtractor
import com.example.graphicspoc.captions.export.CaptionExporter
import com.example.graphicspoc.captions.hinglish.HinglishTransliterator
import com.example.graphicspoc.captions.model.CaptionLine
import com.example.graphicspoc.captions.model.Word
import com.example.graphicspoc.captions.style.CaptionLayout
import com.example.graphicspoc.captions.style.CaptionStyle
import com.example.graphicspoc.captions.style.CaptionStyles
import com.example.graphicspoc.captions.transcribe.SttProvider
import com.example.graphicspoc.captions.transcribe.SttSettings
import com.example.graphicspoc.captions.transcribe.WhisperTranscriber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

sealed interface Stage {
    data object Idle : Stage
    data class ExtractingAudio(val progress: Float) : Stage
    data object Transcribing : Stage
    data class Exporting(val progress: Float) : Stage
    data class Exported(val uri: Uri) : Stage
    data class Error(val message: String) : Stage

    val isBusy get() = this is ExtractingAudio || this is Transcribing || this is Exporting
}

data class CaptionUiState(
    val videoUri: Uri? = null,
    /** Display aspect ratio (width / height) after applying rotation. */
    val videoAspect: Float = 9f / 16f,
    val durationMs: Long = 0,
    val words: List<Word> = emptyList(),
    val style: CaptionStyle = CaptionStyles.POP,
    val layout: CaptionLayout = CaptionLayout(),
    val lines: List<CaptionLine> = emptyList(),
    val stage: Stage = Stage.Idle,
    val settings: SttSettings = SttSettings(),
)

class CaptionViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences("captions", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(CaptionUiState(settings = loadSettings()))
    val state: StateFlow<CaptionUiState> = _state.asStateFlow()
    private var job: Job? = null

    fun onVideoPicked(uri: Uri) {
        job?.cancel()
        viewModelScope.launch {
            val (aspect, duration) = withContext(Dispatchers.IO) { readVideoInfo(uri) }
            _state.update {
                it.copy(
                    videoUri = uri, videoAspect = aspect, durationMs = duration,
                    words = emptyList(), lines = emptyList(), stage = Stage.Idle,
                )
            }
        }
    }

    /** Extract audio -> speech-to-text with word timings -> Hinglish. */
    fun generateCaptions() {
        val uri = _state.value.videoUri ?: return
        if (_state.value.stage.isBusy) return
        job = viewModelScope.launch {
            val app = getApplication<Application>()
            val wav = File(app.cacheDir, "audio_${System.currentTimeMillis()}.wav")
            try {
                _state.update { it.copy(stage = Stage.ExtractingAudio(0f)) }
                AudioExtractor(app).extractToWav(uri, wav) { p ->
                    _state.update { s -> if (s.stage is Stage.ExtractingAudio) s.copy(stage = Stage.ExtractingAudio(p)) else s }
                }
                _state.update { it.copy(stage = Stage.Transcribing) }
                val words = WhisperTranscriber(_state.value.settings).transcribe(wav)
                if (words.isEmpty()) error("No speech was detected in this video")
                setWords(words)
                _state.update { it.copy(stage = Stage.Idle) }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Throwable) {
                _state.update { it.copy(stage = Stage.Error(e.message ?: e.javaClass.simpleName)) }
            } finally {
                wav.delete()
            }
        }
    }

    fun selectStyle(style: CaptionStyle) {
        _state.update { it.copy(style = style, lines = chunk(it.words, style)) }
    }

    fun updateLayout(layout: CaptionLayout) = _state.update { it.copy(layout = layout) }

    /** Manual correction of one caption line. Devanagari typed here is converted too. */
    fun editLine(line: CaptionLine, newText: String) {
        val cleaned = HinglishTransliterator.transliterate(newText)
        val words = CaptionChunker.editRange(_state.value.words, line.startMs, line.endMs, cleaned)
        setWords(words)
    }

    fun export() {
        val s = _state.value
        val uri = s.videoUri ?: return
        if (s.lines.isEmpty() || s.stage.isBusy) return
        job = viewModelScope.launch {
            try {
                _state.update { it.copy(stage = Stage.Exporting(0f)) }
                val out = CaptionExporter(getApplication()).export(uri, s.lines, s.style, s.layout) { p ->
                    _state.update { st -> if (st.stage is Stage.Exporting) st.copy(stage = Stage.Exporting(p)) else st }
                }
                _state.update { it.copy(stage = Stage.Exported(out)) }
            } catch (e: kotlinx.coroutines.CancellationException) {
                _state.update { it.copy(stage = Stage.Idle) }
                throw e
            } catch (e: Throwable) {
                _state.update { it.copy(stage = Stage.Error("Export failed: ${e.message ?: e.javaClass.simpleName}")) }
            }
        }
    }

    fun cancel() {
        job?.cancel()
        _state.update { it.copy(stage = Stage.Idle) }
    }

    fun dismissStage() = _state.update { it.copy(stage = Stage.Idle) }

    fun saveSettings(settings: SttSettings) {
        prefs.edit()
            .putString("provider", settings.provider.name)
            .putString("key_${settings.provider.name}", settings.apiKey)
            .putString("language", settings.language)
            .putString("vocab", settings.vocabularyHint)
            .apply()
        _state.update { it.copy(settings = settings) }
    }

    fun apiKeyFor(provider: SttProvider): String = prefs.getString("key_${provider.name}", "").orEmpty()

    // ---------------------------------------------------------------------------------------

    private fun setWords(words: List<Word>) =
        _state.update { it.copy(words = words, lines = chunk(words, it.style)) }

    private fun chunk(words: List<Word>, style: CaptionStyle) =
        CaptionChunker.chunk(words, maxWords = style.maxWords, maxChars = style.maxChars)

    private fun loadSettings(): SttSettings {
        val provider = runCatching { SttProvider.valueOf(prefs.getString("provider", null)!!) }
            .getOrDefault(SttProvider.GROQ)
        return SttSettings(
            provider = provider,
            apiKey = apiKeyFor(provider),
            language = prefs.getString("language", "hi")!!,
            vocabularyHint = prefs.getString("vocab", "").orEmpty(),
        )
    }

    private fun readVideoInfo(uri: Uri): Pair<Float, Long> {
        val r = MediaMetadataRetriever()
        return try {
            r.setDataSource(getApplication(), uri)
            val w = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toFloatOrNull() ?: 9f
            val h = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toFloatOrNull() ?: 16f
            val rotation = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            val duration = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0
            val aspect = if (rotation % 180 != 0) h / w else w / h
            aspect to duration
        } catch (_: Exception) {
            9f / 16f to 0L
        } finally {
            r.release()
        }
    }
}
