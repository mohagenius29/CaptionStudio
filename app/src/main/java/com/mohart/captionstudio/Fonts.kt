package com.mohart.captionstudio

import android.content.Context
import android.graphics.Typeface
import android.os.Build
import androidx.compose.ui.text.font.FontFamily

class FontLibrary(private val context: Context) {

    private val cache = HashMap<String, Typeface>()
    private val families = HashMap<String, FontFamily>()

    fun typeface(key: String): Typeface = cache.getOrPut(key) { load(key) }

    fun family(key: String): FontFamily = families.getOrPut(key) { FontFamily(typeface(key)) }

    private fun load(key: String): Typeface {
        val e = FontCatalog.entries.firstOrNull { it.key == key } ?: return Typeface.DEFAULT_BOLD
        return try {
            if (e.weight > 0 && Build.VERSION.SDK_INT >= 26) {
                val built = Typeface.Builder(context.assets, "fonts/${e.file}")
                    .setFontVariationSettings("'wght' ${e.weight}")
                    .build()
                built ?: Typeface.createFromAsset(context.assets, "fonts/${e.file}")
            } else {
                Typeface.createFromAsset(context.assets, "fonts/${e.file}")
            }
        } catch (t: Throwable) {
            Typeface.DEFAULT_BOLD
        }
    }
}
