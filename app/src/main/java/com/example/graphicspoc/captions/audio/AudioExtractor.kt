package com.example.graphicspoc.captions.audio

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteOrder
import kotlin.coroutines.coroutineContext

/**
 * Pulls the audio track out of a video and decodes it to a 16 kHz mono 16-bit WAV file,
 * ready to upload to a speech-to-text API.
 */
class AudioExtractor(private val context: Context) {

    class NoAudioTrackException : Exception("This video has no audio track")

    suspend fun extractToWav(
        videoUri: Uri,
        outFile: File,
        onProgress: (Float) -> Unit = {},
    ): File = withContext(Dispatchers.IO) {
        val extractor = MediaExtractor()
        extractor.setDataSource(context, videoUri, null)
        val trackIndex = (0 until extractor.trackCount).firstOrNull { i ->
            extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
        } ?: run {
            extractor.release()
            throw NoAudioTrackException()
        }
        extractor.selectTrack(trackIndex)
        val inputFormat = extractor.getTrackFormat(trackIndex)
        val durationUs = if (inputFormat.containsKey(MediaFormat.KEY_DURATION)) {
            inputFormat.getLong(MediaFormat.KEY_DURATION)
        } else {
            0L
        }

        val decoder = MediaCodec.createDecoderByType(inputFormat.getString(MediaFormat.KEY_MIME)!!)
        decoder.configure(inputFormat, null, null, 0)
        decoder.start()

        val fileOut = FileOutputStream(outFile)
        val out = BufferedOutputStream(fileOut)
        out.write(ByteArray(44)) // header placeholder, patched below

        var resampler: PcmResampler? = null
        var isFloat = false
        var priorSamples = 0L
        fun makeResampler(format: MediaFormat) {
            val rate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            isFloat = format.containsKey(MediaFormat.KEY_PCM_ENCODING) &&
                format.getInteger(MediaFormat.KEY_PCM_ENCODING) == AudioFormat.ENCODING_PCM_FLOAT
            resampler?.let {
                it.flush()
                priorSamples += it.samplesWritten
            }
            resampler = PcmResampler(rate, channels, out)
        }

        val info = MediaCodec.BufferInfo()
        var inputDone = false
        var outputDone = false
        var floatScratch = FloatArray(0)
        var shortScratch = ShortArray(0)
        var totalSamples = 0L

        try {
            while (!outputDone) {
                coroutineContext.ensureActive()
                if (!inputDone) {
                    val inIndex = decoder.dequeueInputBuffer(10_000)
                    if (inIndex >= 0) {
                        val buf = decoder.getInputBuffer(inIndex)!!
                        val size = extractor.readSampleData(buf, 0)
                        if (size < 0) {
                            decoder.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            decoder.queueInputBuffer(inIndex, 0, size, extractor.sampleTime, 0)
                            if (durationUs > 0) onProgress((extractor.sampleTime.toFloat() / durationUs).coerceIn(0f, 1f))
                            extractor.advance()
                        }
                    }
                }

                val outIndex = decoder.dequeueOutputBuffer(info, 10_000)
                when {
                    outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> makeResampler(decoder.outputFormat)
                    outIndex >= 0 -> {
                        if (resampler == null) makeResampler(decoder.outputFormat)
                        val buf = decoder.getOutputBuffer(outIndex)!!
                        buf.position(info.offset)
                        buf.limit(info.offset + info.size)
                        val ordered = buf.order(ByteOrder.nativeOrder())
                        if (isFloat) {
                            val n = info.size / 4
                            if (floatScratch.size < n) floatScratch = FloatArray(n)
                            ordered.asFloatBuffer().get(floatScratch, 0, n)
                            resampler!!.feed(floatScratch, n)
                        } else {
                            val n = info.size / 2
                            if (shortScratch.size < n) shortScratch = ShortArray(n)
                            ordered.asShortBuffer().get(shortScratch, 0, n)
                            resampler!!.feed(shortScratch, n)
                        }
                        decoder.releaseOutputBuffer(outIndex, false)
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
                    }
                }
            }
            resampler?.flush()
            totalSamples = priorSamples + (resampler?.samplesWritten ?: 0L)
            out.flush()
        } finally {
            out.close()
            decoder.stop()
            decoder.release()
            extractor.release()
        }

        RandomAccessFile(outFile, "rw").use { raf ->
            raf.seek(0)
            raf.write(PcmResampler.wavHeader(16_000, totalSamples * 2))
        }
        onProgress(1f)
        outFile
    }
}
