package com.example.graphicspoc.captions

import com.example.graphicspoc.captions.audio.PcmResampler
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayOutputStream
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

class PcmResamplerTest {

    @Test
    fun downsamples48kStereoTo16kMono() {
        val out = ByteArrayOutputStream()
        val r = PcmResampler(48_000, 2, out)
        val frames = 48_000 // one second
        val samples = FloatArray(frames * 2) { i ->
            val v = (0.5 * sin(2 * PI * 440 * (i / 2) / 48_000.0)).toFloat()
            v
        }
        r.feed(samples)
        r.flush()
        assertEquals(16_000L, r.samplesWritten)
        assertEquals(32_000, out.size())
    }

    @Test
    fun passthroughAt16kKeepsSignal() {
        val out = ByteArrayOutputStream()
        val r = PcmResampler(16_000, 1, out)
        r.feed(shortArrayOf(0, 16384, -16384, 32767))
        r.flush()
        val b = out.toByteArray()
        val s = (0 until b.size / 2).map { ((b[2 * it + 1].toInt() shl 8) or (b[2 * it].toInt() and 0xFF)).toShort() }
        assertEquals(4, s.size)
        assertEquals(16383, s[1].toInt(), 2)
        assertEquals(-16383, s[2].toInt(), 2)
    }

    @Test
    fun wavHeaderIsWellFormed() {
        val h = PcmResampler.wavHeader(16_000, 32_000)
        assertEquals("RIFF", String(h, 0, 4))
        assertEquals("WAVE", String(h, 8, 4))
        assertEquals("data", String(h, 36, 4))
        val dataLen = (h[40].toInt() and 0xFF) or ((h[41].toInt() and 0xFF) shl 8) or ((h[42].toInt() and 0xFF) shl 16)
        assertEquals(32_000, dataLen)
    }

    private fun assertEquals(expected: Int, actual: Int, tolerance: Int) {
        if (abs(expected - actual) > tolerance) throw AssertionError("expected $expected±$tolerance but was $actual")
    }
}
