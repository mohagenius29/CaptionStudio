package com.mohart.captionstudio

object StyleLibrary {

    val categories = listOf("Viral", "Clean", "Neon & Fun", "Cinematic", "Arabic")

    private fun c(v: Long): Int = v.toInt()

    val all: List<CaptionStyle> = listOf(

        // ------------------------------------------------------------ Viral
        CaptionStyle(
            "viral_pop", "Viral Pop", "Viral", "montserrat",
            sizeFrac = 0.052f, highlightColor = c(0xFFFFE600), strokeEm = 0.28f,
            shadowColor = c(0x99000000), shadowDyEm = 0.06f, shadowBlurEm = 0.12f,
            uppercase = true, yFrac = 0.6f, maxWidthFrac = 0.8f,
            animIn = Anim.WORD_POP, animOut = Anim.NONE, karaoke = Karaoke.COLOR
        ),
        CaptionStyle(
            "beast", "Big Yellow", "Viral", "luckiest",
            sizeFrac = 0.058f, textColor = c(0xFFFFE600), strokeEm = 0.3f,
            shadowColor = BLACK, shadowDyEm = 0.08f, uppercase = true, yFrac = 0.62f,
            maxWidthFrac = 0.84f, animIn = Anim.POP, animMs = 220L
        ),
        CaptionStyle(
            "sticker", "Sticker", "Viral", "lilita",
            sizeFrac = 0.056f, highlightColor = c(0xFFFFD60A), strokeEm = 0.35f,
            shadowColor = BLACK, shadowDyEm = 0.1f, yFrac = 0.62f, maxWidthFrac = 0.82f,
            animIn = Anim.WORD_POP, animOut = Anim.NONE, karaoke = Karaoke.SCALE
        ),
        CaptionStyle(
            "karaoke_green", "Karaoke Green", "Viral", "poppins",
            sizeFrac = 0.046f, highlightColor = c(0xFF39FF14), strokeEm = 0.22f,
            shadowColor = c(0x99000000), shadowBlurEm = 0.1f, shadowDyEm = 0.05f,
            uppercase = true, yFrac = 0.66f, maxWidthFrac = 0.82f,
            animIn = Anim.WORD_FADE, animOut = Anim.NONE, karaoke = Karaoke.COLOR
        ),
        CaptionStyle(
            "highlight_box", "Highlight Box", "Viral", "poppins",
            sizeFrac = 0.044f, highlightColor = c(0xFF7C3AED),
            shadowColor = c(0x80000000), shadowBlurEm = 0.12f, shadowDyEm = 0.05f,
            boxPadEm = 0.22f, boxRadiusEm = 0.3f, yFrac = 0.7f, maxWidthFrac = 0.82f,
            animIn = Anim.FADE, karaoke = Karaoke.BOX
        ),
        CaptionStyle(
            "boxed_yellow", "Yellow Label", "Viral", "bebas",
            sizeFrac = 0.06f, textColor = BLACK, boxMode = BoxMode.BLOCK,
            boxColor = c(0xFFFFE600), boxPadEm = 0.22f, boxRadiusEm = 0.15f,
            uppercase = true, yFrac = 0.75f, maxWidthFrac = 0.8f, animIn = Anim.SLIDE_UP
        ),

        // ------------------------------------------------------------ Clean
        CaptionStyle(
            "clean_bold", "Clean Bold", "Clean", "poppins",
            sizeFrac = 0.044f, shadowColor = c(0xB3000000), shadowBlurEm = 0.18f,
            shadowDyEm = 0.06f, yFrac = 0.8f, animIn = Anim.FADE
        ),
        CaptionStyle(
            "minimal_lower", "Minimal", "Clean", "montserratm",
            sizeFrac = 0.036f, textColor = c(0xF2FFFFFF), shadowColor = c(0xCC000000),
            shadowBlurEm = 0.2f, shadowDyEm = 0.05f, yFrac = 0.86f, maxWidthFrac = 0.8f,
            animIn = Anim.FADE, animMs = 350L
        ),
        CaptionStyle(
            "news_bar", "News Bar", "Clean", "poppinsb",
            sizeFrac = 0.038f, boxMode = BoxMode.LINE, boxColor = c(0xE6102A5C),
            boxPadEm = 0.3f, boxRadiusEm = 0.08f, yFrac = 0.82f, maxWidthFrac = 0.8f,
            animIn = Anim.SLIDE_UP
        ),
        CaptionStyle(
            "top_banner", "Top Banner", "Clean", "archivo",
            sizeFrac = 0.04f, boxMode = BoxMode.BLOCK, boxColor = c(0xFFD62828),
            boxPadEm = 0.28f, boxRadiusEm = 0.1f, uppercase = true, yFrac = 0.14f,
            maxWidthFrac = 0.8f, animIn = Anim.SLIDE_DOWN
        ),
        CaptionStyle(
            "bungee_block", "Block Letters", "Clean", "bungee",
            sizeFrac = 0.04f, boxMode = BoxMode.BLOCK, boxColor = c(0xFF111111),
            boxPadEm = 0.3f, boxRadiusEm = 0.1f, yFrac = 0.74f, maxWidthFrac = 0.8f,
            animIn = Anim.SLIDE_LEFT
        ),

        // ------------------------------------------------------------ Neon & Fun
        CaptionStyle(
            "neon_cyan", "Neon Cyan", "Neon & Fun", "righteous",
            sizeFrac = 0.054f, textColor = c(0xFFE0FFFF), glowColor = c(0xFF00E5FF),
            glowEm = 0.35f, letterSpacingEm = 0.02f, yFrac = 0.62f,
            animIn = Anim.FADE, animMs = 400L
        ),
        CaptionStyle(
            "retro_neon", "Retro Neon", "Neon & Fun", "monoton",
            sizeFrac = 0.05f, textColor = c(0xFFFF4FD8), glowColor = c(0xFFFF2BD6),
            glowEm = 0.4f, yFrac = 0.6f, animIn = Anim.FADE, animMs = 400L
        ),
        CaptionStyle(
            "comic", "Comic", "Neon & Fun", "bangers",
            sizeFrac = 0.062f, strokeEm = 0.3f, shadowColor = BLACK,
            shadowDxEm = 0.06f, shadowDyEm = 0.08f, uppercase = true,
            letterSpacingEm = 0.02f, yFrac = 0.62f, animIn = Anim.BOUNCE
        ),
        CaptionStyle(
            "marker_yellow", "Marker", "Neon & Fun", "marker",
            sizeFrac = 0.054f, textColor = c(0xFFFFEB3B), strokeEm = 0.22f,
            yFrac = 0.64f, animIn = Anim.ROTATE_IN
        ),
        CaptionStyle(
            "pixel_arcade", "Pixel Arcade", "Neon & Fun", "pixel",
            sizeFrac = 0.03f, textColor = c(0xFF39FF14), boxMode = BoxMode.BLOCK,
            boxColor = c(0xD9000000), boxPadEm = 0.5f, boxRadiusEm = 0.1f,
            lineSpacing = 1.5f, yFrac = 0.78f, maxWidthFrac = 0.8f,
            animIn = Anim.TYPEWRITER, animOut = Anim.NONE
        ),
        CaptionStyle(
            "lobster_pink", "Lobster Pink", "Neon & Fun", "lobster",
            sizeFrac = 0.062f, strokeColor = c(0xFFFF3D81), strokeEm = 0.3f,
            shadowColor = c(0x99000000), shadowBlurEm = 0.15f, shadowDyEm = 0.05f,
            yFrac = 0.66f, animIn = Anim.POP
        ),
        CaptionStyle(
            "pacifico_sunset", "Sunset Script", "Neon & Fun", "pacifico",
            sizeFrac = 0.058f, gradTop = c(0xFFFFD166), gradBottom = c(0xFFFF4D8D),
            shadowColor = c(0x99000000), shadowBlurEm = 0.15f, shadowDyEm = 0.05f,
            yFrac = 0.64f, animIn = Anim.SLIDE_UP
        ),
        CaptionStyle(
            "retro_3d", "Retro 3D", "Neon & Fun", "shrikhand",
            sizeFrac = 0.056f, textColor = c(0xFFFFE066), strokeEm = 0.16f,
            shadowColor = c(0xFFE63946), shadowDxEm = 0.06f, shadowDyEm = 0.08f,
            yFrac = 0.64f, animIn = Anim.POP
        ),
        CaptionStyle(
            "glitch", "Glitch", "Neon & Fun", "blackops",
            sizeFrac = 0.05f, shadowColor = c(0x99000000), shadowBlurEm = 0.1f,
            uppercase = true, yFrac = 0.6f, animIn = Anim.GLITCH, animMs = 500L
        ),
        CaptionStyle(
            "horror", "Horror", "Neon & Fun", "creepster",
            sizeFrac = 0.066f, textColor = c(0xFFE00000), shadowColor = BLACK,
            shadowBlurEm = 0.2f, shadowDyEm = 0.05f, letterSpacingEm = 0.02f,
            yFrac = 0.62f, animIn = Anim.ZOOM_IN, animMs = 600L
        ),

        // ------------------------------------------------------------ Cinematic
        CaptionStyle(
            "cine_serif", "Cinema Serif", "Cinematic", "playfair",
            sizeFrac = 0.044f, shadowColor = c(0xB3000000), shadowBlurEm = 0.2f,
            shadowDyEm = 0.05f, letterSpacingEm = 0.02f, yFrac = 0.84f, maxWidthFrac = 0.8f,
            animIn = Anim.TRACKING, animMs = 700L
        ),
        CaptionStyle(
            "gold_cinzel", "Gold Title", "Cinematic", "cinzel",
            sizeFrac = 0.05f, gradTop = c(0xFFFFF1B8), gradBottom = c(0xFFC9972B),
            shadowColor = c(0xB3000000), shadowBlurEm = 0.2f, shadowDyEm = 0.05f,
            uppercase = true, letterSpacingEm = 0.06f, yFrac = 0.8f,
            animIn = Anim.TRACKING, animMs = 700L
        ),
        CaptionStyle(
            "script_elegant", "Elegant Script", "Cinematic", "greatvibes",
            sizeFrac = 0.075f, shadowColor = c(0xB3000000), shadowBlurEm = 0.15f,
            shadowDyEm = 0.04f, yFrac = 0.78f, animIn = Anim.FADE, animMs = 500L
        ),
        CaptionStyle(
            "satisfy_warm", "Warm Script", "Cinematic", "satisfy",
            sizeFrac = 0.066f, textColor = c(0xFFFFE8C2), shadowColor = c(0xB3000000),
            shadowBlurEm = 0.15f, shadowDyEm = 0.05f, yFrac = 0.75f, animIn = Anim.SLIDE_UP
        ),
        CaptionStyle(
            "abril_title", "Title Card", "Cinematic", "abril",
            sizeFrac = 0.062f, shadowColor = c(0xB3000000), shadowBlurEm = 0.15f,
            shadowDyEm = 0.05f, yFrac = 0.5f, animIn = Anim.ZOOM_IN, animMs = 500L
        ),

        // ------------------------------------------------------------ Arabic
        CaptionStyle(
            "naskh_classic", "Naskh Classic", "Arabic", "amirib",
            sizeFrac = 0.056f, shadowColor = c(0xB3000000), shadowBlurEm = 0.2f,
            shadowDyEm = 0.06f, lineSpacing = 1.3f, yFrac = 0.8f, animIn = Anim.FADE
        ),
        CaptionStyle(
            "ruqaa_gold", "Ruqaa Gold", "Arabic", "arefruqaa",
            sizeFrac = 0.062f, gradTop = c(0xFFFFF2B0), gradBottom = c(0xFFD4A017),
            glowColor = c(0xFFFFB300), glowEm = 0.18f, lineSpacing = 1.35f,
            yFrac = 0.72f, animIn = Anim.ZOOM_IN, animMs = 450L
        ),
        CaptionStyle(
            "kufi_emerald", "Kufi Emerald", "Arabic", "reemkufi",
            sizeFrac = 0.05f, boxMode = BoxMode.BLOCK, boxColor = c(0xFF0B6B4F),
            boxPadEm = 0.3f, boxRadiusEm = 0.3f, yFrac = 0.76f, animIn = Anim.POP
        ),
        CaptionStyle(
            "quran_scheherazade", "Quran Verse", "Arabic", "scheherazade",
            sizeFrac = 0.06f, shadowColor = c(0xCC000000), shadowBlurEm = 0.18f,
            shadowDyEm = 0.05f, lineSpacing = 1.55f, yFrac = 0.5f, maxWidthFrac = 0.86f,
            animIn = Anim.WORD_FADE
        ),
        CaptionStyle(
            "tajawal_pop", "Tajawal Pop", "Arabic", "tajawalb",
            sizeFrac = 0.056f, highlightColor = c(0xFFFFC700), strokeEm = 0.22f,
            shadowColor = c(0x99000000), shadowBlurEm = 0.1f, shadowDyEm = 0.05f,
            yFrac = 0.64f, animIn = Anim.WORD_POP, animOut = Anim.NONE, karaoke = Karaoke.COLOR
        ),
        CaptionStyle(
            "rakkas_royal", "Royal Gold", "Arabic", "rakkas",
            sizeFrac = 0.058f, textColor = c(0xFFFFD166), strokeEm = 0.18f,
            shadowColor = c(0x99000000), shadowBlurEm = 0.12f, shadowDyEm = 0.05f,
            yFrac = 0.7f, animIn = Anim.SLIDE_UP
        ),
        CaptionStyle(
            "lalezar_punch", "Punchy", "Arabic", "lalezar",
            sizeFrac = 0.066f, strokeEm = 0.3f, shadowColor = BLACK,
            shadowDyEm = 0.08f, yFrac = 0.64f, animIn = Anim.BOUNCE
        ),
        CaptionStyle(
            "harmattan_soft", "Soft Glow", "Arabic", "harmattan",
            sizeFrac = 0.06f, glowColor = c(0xFF4ADE80), glowEm = 0.22f,
            yFrac = 0.75f, animIn = Anim.FADE
        ),
        CaptionStyle(
            "katibeh_callig", "Calligraphy", "Arabic", "katibeh",
            sizeFrac = 0.08f, textColor = c(0xFFFFE8B0), shadowColor = c(0xB3000000),
            shadowBlurEm = 0.15f, shadowDyEm = 0.04f, yFrac = 0.72f,
            animIn = Anim.FADE, animMs = 500L
        )
    )

    fun byId(id: String): CaptionStyle = all.firstOrNull { it.id == id } ?: all.first()
}
