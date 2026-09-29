package com.example.graphicspoc.captions

import com.example.graphicspoc.captions.hinglish.HinglishTransliterator
import org.junit.Assert.assertEquals
import org.junit.Test

class HinglishTransliteratorTest {

    private fun t(s: String) = HinglishTransliterator.transliterate(s)

    @Test
    fun typicalCreatorSentence() {
        assertEquals(
            "namaste doston, aaj main aapko ek bahut mast trick batane wala hoon.",
            t("नमस्ते दोस्तों, आज मैं आपको एक बहुत मस्त ट्रिक बताने वाला हूँ।"),
        )
    }

    @Test
    fun englishWordsWrittenInDevanagariBecomeEnglish() {
        assertEquals("is video ko like karo aur channel ko subscribe karo", t("इस वीडियो को लाइक करो और चैनल को सब्सक्राइब करो"))
    }

    @Test
    fun latinWordsPassThroughUntouched() {
        assertEquals("Hello guys aaj ka topic hai money", t("Hello guys आज का topic है money"))
    }

    @Test
    fun schwaDeletion() {
        assertEquals("samajhna", t("समझना"))
        assertEquals("karna", t("करना"))
        assertEquals("ladki", t("लड़की"))
        assertEquals("apna", t("अपना"))
        assertEquals("kamal", t("कमल"))
        assertEquals("samajh", t("समझ"))
        assertEquals("rahna", t("रहना"))
    }

    @Test
    fun keepsFinalSchwaAfterClusters() {
        assertEquals("mitra", t("मित्र"))
        assertEquals("satya", t("सत्य"))
    }

    @Test
    fun nasalsAndSpecialClusters() {
        assertEquals("hindi", t("हिंदी"))
        assertEquals("sambandh", t("संबंध"))
        assertEquals("batein", t("बातें"))
        assertEquals("gyaan", t("ज्ञान"))
        assertEquals("bacche", t("बच्चे"))
        assertEquals("swagat", t("स्वागत"))
    }

    @Test
    fun nuktaFormsBothPrecomposedAndDecomposed() {
        assertEquals("zindagi", t("ज़िंदगी"))   // precomposed ज़
        assertEquals("zindagi", t("ज़िंदगी"))  // ज + nukta
        assertEquals("film", t("फ़िल्म"))
        assertEquals("khush", t("ख़ुश"))
    }

    @Test
    fun vowelLengthMatchesCasualSpelling() {
        assertEquals("baat", t("बात"))
        assertEquals("kaam", t("काम"))
        assertEquals("hamara", t("हमारा"))
        assertEquals("seekhna", t("सीखना"))
        assertEquals("theek", t("ठीक"))
    }

    @Test
    fun digitsAndDanda() {
        assertEquals("2024 mein.", t("२०२४ में।"))
    }
}
