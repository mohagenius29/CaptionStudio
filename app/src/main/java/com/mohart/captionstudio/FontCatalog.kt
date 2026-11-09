package com.mohart.captionstudio

data class FontEntry(val key: String, val label: String, val file: String, val weight: Int, val arabic: Boolean)

object FontCatalog {
    val entries: List<FontEntry> = listOf(
        FontEntry("bebas", "Bebas Neue", "bebas.ttf", 0, false),
        FontEntry("anton", "Anton", "anton.ttf", 0, false),
        FontEntry("bangers", "Bangers", "bangers.ttf", 0, false),
        FontEntry("lobster", "Lobster", "lobster.ttf", 0, false),
        FontEntry("pacifico", "Pacifico", "pacifico.ttf", 0, false),
        FontEntry("marker", "Permanent Marker", "marker.ttf", 0, false),
        FontEntry("righteous", "Righteous", "righteous.ttf", 0, false),
        FontEntry("bungee", "Bungee", "bungee.ttf", 0, false),
        FontEntry("pixel", "Press Start 2P", "pixel.ttf", 0, false),
        FontEntry("poppins", "Poppins ExtraBold", "poppins.ttf", 0, false),
        FontEntry("poppinsb", "Poppins Bold", "poppinsb.ttf", 0, false),
        FontEntry("abril", "Abril Fatface", "abril.ttf", 0, false),
        FontEntry("luckiest", "Luckiest Guy", "luckiest.ttf", 0, false),
        FontEntry("blackops", "Black Ops One", "blackops.ttf", 0, false),
        FontEntry("creepster", "Creepster", "creepster.ttf", 0, false),
        FontEntry("monoton", "Monoton", "monoton.ttf", 0, false),
        FontEntry("russo", "Russo One", "russo.ttf", 0, false),
        FontEntry("archivo", "Archivo Black", "archivo.ttf", 0, false),
        FontEntry("shrikhand", "Shrikhand", "shrikhand.ttf", 0, false),
        FontEntry("lilita", "Lilita One", "lilita.ttf", 0, false),
        FontEntry("greatvibes", "Great Vibes", "greatvibes.ttf", 0, false),
        FontEntry("satisfy", "Satisfy", "satisfy.ttf", 0, false),
        FontEntry("playfair", "Playfair Display", "playfair.ttf", 700, false),
        FontEntry("montserrat", "Montserrat ExtraBold", "montserrat.ttf", 800, false),
        FontEntry("montserratm", "Montserrat Medium", "montserrat.ttf", 600, false),
        FontEntry("cinzel", "Cinzel", "cinzel.ttf", 700, false),
        FontEntry("amiri", "Amiri", "amiri.ttf", 0, true),
        FontEntry("amirib", "Amiri Bold", "amirib.ttf", 0, true),
        FontEntry("arefruqaa", "Aref Ruqaa", "arefruqaa.ttf", 0, true),
        FontEntry("reemkufi", "Reem Kufi", "reemkufi.ttf", 700, true),
        FontEntry("tajawal", "Tajawal ExtraBold", "tajawal.ttf", 0, true),
        FontEntry("tajawalb", "Tajawal Black", "tajawalb.ttf", 0, true),
        FontEntry("scheherazade", "Scheherazade New", "scheherazade.ttf", 0, true),
        FontEntry("lateef", "Lateef", "lateef.ttf", 0, true),
        FontEntry("harmattan", "Harmattan", "harmattan.ttf", 0, true),
        FontEntry("rakkas", "Rakkas", "rakkas.ttf", 0, true),
        FontEntry("lalezar", "Lalezar", "lalezar.ttf", 0, true),
        FontEntry("katibeh", "Katibeh", "katibeh.ttf", 0, true),
        FontEntry("almarai", "Almarai ExtraBold", "almarai.ttf", 0, true),
        FontEntry("mirza", "Mirza Bold", "mirza.ttf", 0, true),
        FontEntry("jomhuria", "Jomhuria", "jomhuria.ttf", 0, true)
    )
}
