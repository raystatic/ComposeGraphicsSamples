package com.example.graphicspoc.captions

import com.example.graphicspoc.captions.model.Word
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CaptionChunkerTest {

    private fun words(vararg spec: Pair<String, LongRange>) =
        spec.map { (t, r) -> Word(t, r.first, r.last) }

    @Test
    fun splitsByMaxWords() {
        val w = words("a" to 0L..100L, "b" to 100L..200L, "c" to 200L..300L, "d" to 300L..400L)
        val lines = CaptionChunker.chunk(w, maxWords = 3)
        assertEquals(listOf("a b c", "d"), lines.map { it.text })
    }

    @Test
    fun splitsOnPausesAndSentenceEnds() {
        val w = listOf(
            Word("ek", 0, 200), Word("do", 200, 400, endsSentence = true),
            Word("teen", 450, 600), Word("chaar", 2000, 2200),
        )
        val lines = CaptionChunker.chunk(w, maxWords = 10)
        assertEquals(listOf("ek do", "teen", "chaar"), lines.map { it.text })
    }

    @Test
    fun linesNeverOverlapAndLinger() {
        val w = words("a" to 0L..100L, "b" to 150L..250L)
        val lines = CaptionChunker.chunk(w, maxWords = 1, lingerMs = 400)
        assertEquals(150L, lines[0].endMs)      // clipped at next line start
        assertEquals(650L, lines[1].endMs)      // lingers after last word
    }

    @Test
    fun lineLookup() {
        val w = words("a" to 0L..100L, "b" to 150L..250L, "c" to 3000L..3100L)
        val lines = CaptionChunker.chunk(w, maxWords = 1)
        assertEquals("a", CaptionChunker.lineAt(lines, 50)!!.text)
        assertEquals("b", CaptionChunker.lineAt(lines, 150)!!.text)
        assertNull(CaptionChunker.lineAt(lines, 1500))
        assertEquals("c", CaptionChunker.lineAt(lines, 3050)!!.text)
    }

    @Test
    fun editKeepsTimingWhenWordCountMatches() {
        val w = words("mai" to 0L..100L, "ghar" to 100L..300L, "ja" to 300L..400L)
        val edited = CaptionChunker.editRange(w, 0, 400, "main ghar jaa")
        assertEquals(listOf("main", "ghar", "jaa"), edited.map { it.text })
        assertEquals(w.map { it.startMs }, edited.map { it.startMs })
    }

    @Test
    fun editRedistributesTimingWhenWordCountChanges() {
        val w = words("x" to 0L..100L, "ab" to 1000L..1400L, "cd" to 1400L..1800L, "y" to 5000L..5100L)
        val edited = CaptionChunker.editRange(w, 1000, 1800, "ab cd ef gh")
        assertEquals(listOf("x", "ab", "cd", "ef", "gh", "y"), edited.map { it.text })
        assertEquals(1000L, edited[1].startMs)
        assertEquals(1800L, edited[4].endMs)
        assertEquals(5000L, edited[5].startMs)
    }
}
