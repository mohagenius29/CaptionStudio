package com.mohart.captionstudio

val WHITE: Int = 0xFFFFFFFF.toInt()
val BLACK: Int = 0xFF000000.toInt()
val YELLOW: Int = 0xFFFFE600.toInt()

fun argb(v: Long): Int = v.toInt()

data class Caption(val id: Long, val startMs: Long, val endMs: Long, val text: String)

data class VideoInfo(val durationMs: Long, val width: Int, val height: Int)

/** Background music / audio track settings. */
data class MusicSettings(
    val uri: android.net.Uri? = null,
    val volume: Float = 0.55f,          // music volume 0..1
    val originalVolume: Float = 1.0f,   // original video audio volume 0..1
    val loop: Boolean = true
)

enum class Anim(val label: String) {
    NONE("None"),
    FADE("Fade"),
    POP("Pop"),
    BOUNCE("Bounce"),
    SLIDE_UP("Slide up"),
    SLIDE_DOWN("Slide down"),
    SLIDE_LEFT("Slide left"),
    SLIDE_RIGHT("Slide right"),
    ZOOM_IN("Zoom in"),
    ROTATE_IN("Twist in"),
    TRACKING("Spread"),
    TYPEWRITER("Typewriter"),
    WORD_POP("Word pop"),
    WORD_FADE("Word fade"),
    WORD_RISE("Word rise"),
    GLITCH("Glitch");

    val isWordAnim: Boolean
        get() = this == WORD_POP || this == WORD_FADE || this == WORD_RISE
}

enum class Karaoke { NONE, COLOR, SCALE, BOX }

enum class BoxMode { NONE, BLOCK, LINE }

enum class Aspect(val label: String, val ratio: Float?) {
    ORIGINAL("Original", null),
    R9_16("9:16", 9f / 16f),
    R16_9("16:9", 16f / 9f),
    R1_1("1:1", 1f)
}

/**
 * One caption look. All sizes are fractions so the same style works at any resolution:
 * sizeFrac = font size / video height, *Em values = multiples of the font size.
 */
data class CaptionStyle(
    val id: String,
    val name: String,
    val category: String,
    val fontKey: String,
    val sizeFrac: Float = 0.05f,
    val textColor: Int = WHITE,
    val highlightColor: Int = YELLOW,
    val strokeColor: Int = BLACK,
    val strokeEm: Float = 0f,
    val shadowColor: Int = 0,
    val shadowDxEm: Float = 0f,
    val shadowDyEm: Float = 0f,
    val shadowBlurEm: Float = 0f,
    val glowColor: Int = 0,
    val glowEm: Float = 0f,
    val gradTop: Int = 0,
    val gradBottom: Int = 0,
    val boxMode: BoxMode = BoxMode.NONE,
    val boxColor: Int = 0xCC000000.toInt(),
    val boxPadEm: Float = 0.3f,
    val boxRadiusEm: Float = 0.25f,
    val uppercase: Boolean = false,
    val yFrac: Float = 0.78f,
    val maxWidthFrac: Float = 0.86f,
    val lineSpacing: Float = 1.05f,
    val letterSpacingEm: Float = 0f,
    val animIn: Anim = Anim.FADE,
    val animOut: Anim = Anim.FADE,
    val animMs: Long = 280L,
    val karaoke: Karaoke = Karaoke.NONE
)
