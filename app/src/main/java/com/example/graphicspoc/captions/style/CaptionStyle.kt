package com.example.graphicspoc.captions.style

enum class FontChoice { BLACK, BOLD, CONDENSED, MONO, SERIF }

enum class HighlightMode {
    /** No per-word highlight, classic subtitle. */
    NONE,

    /** The word being spoken changes colour. */
    COLOR,

    /** The word being spoken sits on a coloured rounded "pill". */
    PILL,

    /** The word being spoken glows. */
    GLOW,
}

/**
 * A caption look. Sizes are ratios of the video's shorter side so the preview and the
 * exported video look identical at any resolution.
 */
data class CaptionStyle(
    val id: String,
    val name: String,
    val tagline: String,
    val font: FontChoice,
    val uppercase: Boolean,
    val textColor: Int,
    val sizeRatio: Float,
    val maxWords: Int,
    val maxChars: Int = 24,
    val highlightMode: HighlightMode = HighlightMode.COLOR,
    val highlightColor: Int = 0xFFFFE14D.toInt(),
    /** Colours cycled per line instead of a single highlight colour (one-word styles). */
    val cycleColors: List<Int> = emptyList(),
    val strokeColor: Int? = null,
    val strokeRatio: Float = 0f,
    val shadowColor: Int = 0x99000000.toInt(),
    val shadowRatio: Float = 0.06f,
    /** Vertical gradient fill (top -> bottom) for non-highlighted words. */
    val gradient: List<Int> = emptyList(),
    /** Rounded box behind each row. */
    val boxColor: Int? = null,
    val boxTextColor: Int? = null,
    /** Scale the active word pops to. */
    val popScale: Float = 1f,
    /** Alpha of words not yet spoken (1 = fully visible). */
    val upcomingAlpha: Float = 1f,
    /** Typewriter: only show words that have already been spoken. */
    val progressiveReveal: Boolean = false,
    /** Line pops in with a small bounce when it appears. */
    val entryBounce: Boolean = true,
    val stripPunctuation: Boolean = true,
    val letterSpacing: Float = 0f,
)

object CaptionStyles {
    private const val WHITE = 0xFFFFFFFF.toInt()
    private const val BLACK = 0xFF000000.toInt()

    val POP = CaptionStyle(
        id = "pop", name = "Pop", tagline = "Bold, yellow word pop",
        font = FontChoice.BLACK, uppercase = true, textColor = WHITE, sizeRatio = 0.085f,
        maxWords = 3, maxChars = 18, highlightColor = 0xFFFFE14D.toInt(),
        strokeColor = BLACK, strokeRatio = 0.16f, popScale = 1.15f,
    )

    val KARAOKE = CaptionStyle(
        id = "karaoke", name = "Karaoke", tagline = "Purple pill follows the voice",
        font = FontChoice.BOLD, uppercase = false, textColor = WHITE, sizeRatio = 0.07f,
        maxWords = 4, maxChars = 24, highlightMode = HighlightMode.PILL, highlightColor = 0xFF7C4DFF.toInt(),
        shadowRatio = 0.08f, popScale = 1.05f,
    )

    val NEON = CaptionStyle(
        id = "neon", name = "Neon", tagline = "Night-club glow",
        font = FontChoice.CONDENSED, uppercase = true, textColor = 0xFFE6FFFF.toInt(), sizeRatio = 0.08f,
        maxWords = 4, maxChars = 22, highlightMode = HighlightMode.GLOW, highlightColor = 0xFFFF3CAC.toInt(),
        shadowColor = 0xFF00E5FF.toInt(), shadowRatio = 0.12f, upcomingAlpha = 0.55f, popScale = 1.08f,
        letterSpacing = 0.04f,
    )

    val MASALA = CaptionStyle(
        id = "masala", name = "Masala", tagline = "Saffron-pink desi gradient",
        font = FontChoice.BLACK, uppercase = true, textColor = WHITE, sizeRatio = 0.085f,
        maxWords = 3, maxChars = 18, highlightColor = 0xFF19F5AA.toInt(),
        gradient = listOf(0xFFFFB347.toInt(), 0xFFFF3D77.toInt()),
        strokeColor = 0xFF2B0A3D.toInt(), strokeRatio = 0.16f, popScale = 1.12f,
    )

    val CLEAN = CaptionStyle(
        id = "clean", name = "Clean", tagline = "Classic subtitle box",
        font = FontChoice.BOLD, uppercase = false, textColor = WHITE, sizeRatio = 0.052f,
        maxWords = 7, maxChars = 40, highlightMode = HighlightMode.NONE,
        boxColor = 0xB3000000.toInt(), shadowRatio = 0f, entryBounce = false, stripPunctuation = false,
    )

    val PUNCH = CaptionStyle(
        id = "punch", name = "Punch", tagline = "One big word at a time",
        font = FontChoice.BLACK, uppercase = true, textColor = WHITE, sizeRatio = 0.13f,
        maxWords = 1, maxChars = 14,
        cycleColors = listOf(WHITE, 0xFFFFE14D.toInt(), 0xFF4DF0FF.toInt(), 0xFFFF5C8A.toInt()),
        strokeColor = BLACK, strokeRatio = 0.14f, popScale = 1.2f,
    )

    val TYPEWRITER = CaptionStyle(
        id = "typewriter", name = "Typewriter", tagline = "Words type in on a sticky note",
        font = FontChoice.MONO, uppercase = false, textColor = BLACK, sizeRatio = 0.06f,
        maxWords = 5, maxChars = 26, highlightMode = HighlightMode.NONE,
        boxColor = 0xFFFFE14D.toInt(), boxTextColor = BLACK, shadowRatio = 0f,
        progressiveReveal = true, entryBounce = false,
    )

    val ALL = listOf(POP, KARAOKE, MASALA, NEON, PUNCH, TYPEWRITER, CLEAN)

    fun byId(id: String) = ALL.firstOrNull { it.id == id } ?: POP
}

/** User adjustments on top of a style. */
data class CaptionLayout(
    /** Vertical centre of the caption block, 0 = top, 1 = bottom. */
    val verticalBias: Float = 0.72f,
    val sizeScale: Float = 1f,
)
