package com.example.graphicspoc.captions.audio

import java.io.OutputStream

/**
 * Streaming down-mixer + resampler: interleaved PCM of any rate/channel count in,
 * 16-bit mono little-endian PCM at [outRate] out. Speech recognisers expect 16 kHz mono,
 * which also keeps a 60s clip under 2 MB for upload.
 */
class PcmResampler(
    private val inRate: Int,
    private val channels: Int,
    private val out: OutputStream,
    private val outRate: Int = 16_000,
) {
    private val ratio = inRate.toDouble() / outRate
    // Simple box low-pass before decimation so 44.1/48 kHz input doesn't alias.
    private val window = maxOf(1, ratio.toInt())
    private val history = FloatArray(window)
    private var historyPos = 0
    private var historySum = 0f

    private var inIndex = 0L
    private var nextOutPos = 0.0
    private var prev = 0f
    private val buffer = ByteArray(4096)
    private var bufferLen = 0

    var samplesWritten = 0L
        private set

    /** Feeds interleaved frames, samples normalised to [-1, 1]. */
    fun feed(samples: FloatArray, count: Int = samples.size) {
        var i = 0
        while (i + channels <= count) {
            var mono = 0f
            for (c in 0 until channels) mono += samples[i + c]
            mono /= channels
            i += channels
            pushMono(mono)
        }
    }

    /** Feeds interleaved 16-bit samples. */
    fun feed(samples: ShortArray, count: Int = samples.size) {
        val floats = FloatArray(count) { samples[it] / 32768f }
        feed(floats, count)
    }

    private fun pushMono(raw: Float) {
        historySum += raw - history[historyPos]
        history[historyPos] = raw
        historyPos = (historyPos + 1) % window
        val s = historySum / window

        while (nextOutPos <= inIndex) {
            val frac = (nextOutPos - (inIndex - 1)).toFloat().coerceIn(0f, 1f)
            writeSample(prev + (s - prev) * frac)
            nextOutPos += ratio
        }
        prev = s
        inIndex++
    }

    private fun writeSample(v: Float) {
        val s = (v.coerceIn(-1f, 1f) * 32767f).toInt()
        buffer[bufferLen++] = (s and 0xFF).toByte()
        buffer[bufferLen++] = ((s shr 8) and 0xFF).toByte()
        samplesWritten++
        if (bufferLen == buffer.size) flush()
    }

    fun flush() {
        if (bufferLen > 0) out.write(buffer, 0, bufferLen)
        bufferLen = 0
    }

    companion object {
        /** 44-byte canonical WAV header for 16-bit mono PCM. */
        fun wavHeader(sampleRate: Int, dataBytes: Long): ByteArray {
            val h = ByteArray(44)
            fun str(off: Int, s: String) = s.forEachIndexed { i, c -> h[off + i] = c.code.toByte() }
            fun int(off: Int, v: Long) { for (i in 0..3) h[off + i] = ((v shr (8 * i)) and 0xFF).toByte() }
            fun short(off: Int, v: Int) { h[off] = (v and 0xFF).toByte(); h[off + 1] = ((v shr 8) and 0xFF).toByte() }
            str(0, "RIFF"); int(4, 36 + dataBytes); str(8, "WAVE")
            str(12, "fmt "); int(16, 16); short(20, 1); short(22, 1)
            int(24, sampleRate.toLong()); int(28, sampleRate * 2L); short(32, 2); short(34, 16)
            str(36, "data"); int(40, dataBytes)
            return h
        }
    }
}
