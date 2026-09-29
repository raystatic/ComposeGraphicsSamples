package com.example.graphicspoc.captions.hinglish

import java.text.Normalizer

/**
 * Deterministic Devanagari -> Hinglish (casual Roman script) converter.
 *
 * This is NOT a language model: it never invents words. It only changes the *script* of the
 * words the speech recogniser actually heard, so "मैं आज वीडियो बना रहा हूँ" becomes
 * "main aaj video bana raha hoon".
 *
 * Pipeline per word:
 *  1. Latin words (English spoken as English) pass through untouched.
 *  2. Common words / English loanwords written in Devanagari are looked up in
 *     [HinglishLexicon] so they get the spelling creators actually use ("nahi", "video").
 *  3. Everything else is romanised phonetically with Hindi schwa deletion
 *     ("समझना" -> "samajhna", "लड़की" -> "ladki").
 */
object HinglishTransliterator {

    /** Converts a full sentence / caption line. */
    fun transliterate(text: String): String =
        text.split(WHITESPACE).filter { it.isNotEmpty() }.joinToString(" ") { transliterateWord(it) }

    /** Converts a single whitespace-free token, keeping any surrounding punctuation. */
    fun transliterateWord(token: String): String {
        val normalized = normalize(token)
        if (normalized.none(::isDevanagari)) return normalized

        val sb = StringBuilder()
        var i = 0
        while (i < normalized.length) {
            val start = i
            if (isDevanagariLetter(normalized[i])) {
                while (i < normalized.length && isDevanagariLetter(normalized[i])) i++
                sb.append(romanizeWord(normalized.substring(start, i)))
            } else {
                while (i < normalized.length && !isDevanagariLetter(normalized[i])) i++
                sb.append(mapNonLetters(normalized.substring(start, i)))
            }
        }
        return sb.toString()
    }

    /** True if [text] contains any Devanagari characters. */
    fun containsDevanagari(text: String): Boolean = text.any(::isDevanagari)

    // ---------------------------------------------------------------------------------------

    private val WHITESPACE = Regex("\\s+")

    internal fun normalize(s: String): String =
        Normalizer.normalize(s, Normalizer.Form.NFC)
            .replace("‍", "") // ZWJ
            .replace("‌", "") // ZWNJ

    private fun isDevanagari(c: Char) = c in 'ऀ'..'ॿ'

    /** Devanagari characters that form words (i.e. not danda / digits). */
    private fun isDevanagariLetter(c: Char) =
        isDevanagari(c) && c != '।' && c != '॥' && c !in '०'..'९' && c != '॰'

    private fun mapNonLetters(s: String): String = buildString {
        for (c in s) {
            when (c) {
                '।', '॥' -> append('.')
                in '०'..'९' -> append('0' + (c - '०'))
                '॰' -> append('.')
                else -> append(c)
            }
        }
    }

    // --- Phonetic romanisation -------------------------------------------------------------

    private const val NUKTA = '़'
    private const val HALANT = '्'
    private const val ANUSVARA = 'ं'
    private const val CHANDRABINDU = 'ँ'
    private const val VISARGA = 'ः'

    private enum class VowelKind { INHERENT, MATRA, NONE }

    private class Akshara(
        /** Roman consonant, or null for an independent vowel. */
        var consonant: String?,
        /** The vowel sign / independent vowel character, if any. */
        val vowelChar: Char?,
        var kind: VowelKind,
        var nasal: Char? = null,
        var visarga: Boolean = false,
        var schwaDeleted: Boolean = false,
    ) {
        val isIndependentVowel get() = consonant == null
        val hasVowel: Boolean
            get() = isIndependentVowel || kind == VowelKind.MATRA ||
                (kind == VowelKind.INHERENT && !schwaDeleted)
    }

    private val CONSONANTS = mapOf(
        'क' to "k", 'ख' to "kh", 'ग' to "g", 'घ' to "gh", 'ङ' to "n",
        'च' to "ch", 'छ' to "chh", 'ज' to "j", 'झ' to "jh", 'ञ' to "n",
        'ट' to "t", 'ठ' to "th", 'ड' to "d", 'ढ' to "dh", 'ण' to "n",
        'त' to "t", 'थ' to "th", 'द' to "d", 'ध' to "dh", 'न' to "n",
        'प' to "p", 'फ' to "ph", 'ब' to "b", 'भ' to "bh", 'म' to "m",
        'य' to "y", 'र' to "r", 'ल' to "l", 'ळ' to "l", 'व' to "v",
        'श' to "sh", 'ष' to "sh", 'स' to "s", 'ह' to "h",
    )

    /** Consonant + nukta (Urdu / Persian sounds). */
    private val NUKTA_CONSONANTS = mapOf(
        'क' to "q", 'ख' to "kh", 'ग' to "gh", 'ज' to "z", 'ड' to "d",
        'ढ' to "dh", 'फ' to "f", 'य' to "y", 'र' to "r", 'ल' to "l", 'न' to "n",
    )

    private val INDEPENDENT_VOWELS = setOf(
        'अ', 'आ', 'इ', 'ई', 'उ', 'ऊ', 'ऋ', 'ए', 'ऐ', 'ओ', 'औ', 'ऑ', 'ऍ', 'ऎ', 'ऒ',
    )

    private val MATRAS = setOf(
        'ा', 'ि', 'ी', 'ु', 'ू', 'ृ', 'े', 'ै', 'ो', 'ौ', 'ॉ', 'ॅ', 'ॆ', 'ॊ',
    )

    private fun parse(word: String): List<Akshara> {
        val units = mutableListOf<Akshara>()
        var i = 0
        while (i < word.length) {
            val c = word[i]
            when {
                c in CONSONANTS -> {
                    var roman = CONSONANTS.getValue(c)
                    i++
                    if (i < word.length && word[i] == NUKTA) {
                        roman = NUKTA_CONSONANTS[c] ?: roman
                        i++
                    }
                    // ज्ञ is pronounced "gy" in Hindi (ज्ञान -> gyaan).
                    if (c == 'ज' && i + 1 < word.length && word[i] == HALANT && word[i + 1] == 'ञ') {
                        roman = "gy"
                        i += 2
                    }
                    val unit = when {
                        i < word.length && word[i] in MATRAS -> Akshara(roman, word[i++], VowelKind.MATRA)
                        i < word.length && word[i] == HALANT -> {
                            i++
                            Akshara(roman, null, VowelKind.NONE)
                        }
                        else -> Akshara(roman, null, VowelKind.INHERENT)
                    }
                    units += unit
                }
                c in INDEPENDENT_VOWELS -> {
                    units += Akshara(null, c, VowelKind.MATRA)
                    i++
                }
                c == ANUSVARA || c == CHANDRABINDU -> {
                    units.lastOrNull()?.nasal = c
                    i++
                }
                c == VISARGA -> {
                    units.lastOrNull()?.visarga = true
                    i++
                }
                else -> i++ // avagraha, stray nukta/halant, unknown marks
            }
        }
        return units
    }

    private fun applySchwaDeletion(units: List<Akshara>) {
        if (units.isEmpty()) return
        val last = units.last()
        // Word-final schwa: "कमल" -> kamal, but keep it for one-letter words ("न" -> na)
        // and after y/r/v clusters ("मित्र" -> mitra, "सत्य" -> satya).
        if (!last.isIndependentVowel && last.kind == VowelKind.INHERENT && last.nasal == null && !last.visarga &&
            units.size > 1
        ) {
            val prev = units[units.size - 2]
            val keep = prev.kind == VowelKind.NONE && last.consonant in setOf("y", "r", "v")
            if (!keep) last.schwaDeleted = true
        }
        // Medial schwa: V C[a] C V -> V C C V, processed right to left ("समझना" -> samajhna).
        for (i in units.size - 2 downTo 1) {
            val u = units[i]
            if (u.isIndependentVowel || u.kind != VowelKind.INHERENT || u.schwaDeleted) continue
            if (u.nasal != null || u.visarga) continue
            val next = units[i + 1]
            val prev = units[i - 1]
            if (next.isIndependentVowel || !next.hasVowel) continue
            if (!prev.hasVowel) continue
            u.schwaDeleted = true
        }
    }

    private fun romanizeWord(word: String): String {
        HinglishLexicon.lookup(word)?.let { return it }

        val units = parse(word)
        applySchwaDeletion(units)
        val syllables = units.count { it.hasVowel }
        val lastVowelIndex = units.indexOfLast { it.hasVowel }

        val sb = StringBuilder()
        units.forEachIndexed { index, u ->
            val prev = units.getOrNull(index - 1)
            val next = units.getOrNull(index + 1)
            val isFinalVowel = index == lastVowelIndex && index == units.lastIndex

            if (!u.isIndependentVowel) {
                var cons = u.consonant!!
                // "च्छ" / "च्च" -> "cch" (अच्छा -> accha, बच्चे -> bacche)
                if (cons == "ch" && u.kind == VowelKind.NONE && next?.consonant?.startsWith("ch") == true) cons = "c"
                if (cons == "chh" && prev?.consonant == "c") cons = "ch"
                // व after a half consonant sounds like "w" (स्वागत -> swagat, द्वारा -> dwara).
                if (cons == "v" && prev != null && !prev.isIndependentVowel && prev.kind == VowelKind.NONE) cons = "w"
                sb.append(cons)
            }

            val vowel = when {
                u.isIndependentVowel -> independentVowel(u.vowelChar!!, isFinalVowel)
                u.kind == VowelKind.MATRA -> matra(u.vowelChar!!, isFinalVowel, syllables)
                u.kind == VowelKind.INHERENT && !u.schwaDeleted -> "a"
                else -> ""
            }
            sb.append(vowel)

            u.nasal?.let { nasal ->
                sb.append(nasalAfter(u, nasal, next, isFinal = index == units.lastIndex))
            }
            if (u.visarga) sb.append('h')
        }
        return sb.toString()
    }

    private fun independentVowel(c: Char, isFinal: Boolean): String = when (c) {
        'अ' -> "a"
        'आ' -> "aa"
        'इ' -> "i"
        'ई' -> if (isFinal) "i" else "ee"
        'उ' -> "u"
        'ऊ' -> "oo"
        'ऋ' -> "ri"
        'ए', 'ऍ', 'ऎ' -> "e"
        'ऐ' -> "ai"
        'ओ', 'ऑ', 'ऒ' -> "o"
        'औ' -> "au"
        else -> ""
    }

    private fun matra(c: Char, isFinal: Boolean, syllables: Int): String = when (c) {
        // Casual Hinglish writes "baat", "kaam" but "hamara", "raha", "kya".
        'ा' -> if (syllables == 1) "aa" else "a"
        'ि' -> "i"
        'ी' -> if (isFinal) "i" else "ee"
        'ु' -> "u"
        'ू' -> if (isFinal) "u" else "oo"
        'ृ' -> "ri"
        'े', 'ॅ', 'ॆ' -> "e"
        'ै' -> "ai"
        'ो', 'ॉ', 'ॊ' -> "o"
        'ौ' -> "au"
        else -> ""
    }

    private fun nasalAfter(u: Akshara, nasal: Char, next: Akshara?, isFinal: Boolean): String {
        if (isFinal && u.kind == VowelKind.MATRA && !u.isIndependentVowel) {
            // बातें -> baatein, जाएं -> jaayein, हूँ -> hoon
            when (u.vowelChar) {
                'े' -> return "in"
                'ू' -> return "n"
            }
        }
        val labial = next?.consonant?.firstOrNull() in setOf('p', 'b', 'm')
        return if (nasal == ANUSVARA && labial) "m" else "n"
    }
}
