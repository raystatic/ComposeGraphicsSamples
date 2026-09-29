package com.example.graphicspoc.captions

import com.example.graphicspoc.captions.transcribe.WhisperResponseParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WhisperResponseParserTest {

    @Test
    fun parsesWordTimestampsAndMarksSegmentEnds() {
        val json = """
            {"text":"नमस्ते दोस्तों आज हम","segments":[
              {"start":0.0,"end":1.2,"text":" नमस्ते दोस्तों"},
              {"start":1.4,"end":2.4,"text":" आज हम"}],
             "words":[
              {"word":" नमस्ते","start":0.0,"end":0.6},
              {"word":" दोस्तों","start":0.6,"end":1.2},
              {"word":" आज","start":1.4,"end":1.8},
              {"word":" हम","start":1.8,"end":2.4}]}
        """.trimIndent()
        val words = WhisperResponseParser.parse(json)
        assertEquals(listOf("नमस्ते", "दोस्तों", "आज", "हम"), words.map { it.text })
        assertEquals(listOf(0L, 600L, 1400L, 1800L), words.map { it.startMs })
        assertFalse(words[0].endsSentence)
        assertTrue(words[1].endsSentence)
        assertTrue(words[3].endsSentence)
    }

    @Test
    fun fallsBackToSegmentsWithoutWords() {
        val json = """{"segments":[{"start":1.0,"end":2.0,"text":"ab cd"}]}"""
        val words = WhisperResponseParser.parse(json)
        assertEquals(listOf("ab", "cd"), words.map { it.text })
        assertEquals(1000L, words[0].startMs)
        assertEquals(1500L, words[1].startMs)
        assertEquals(2000L, words[1].endMs)
    }
}
