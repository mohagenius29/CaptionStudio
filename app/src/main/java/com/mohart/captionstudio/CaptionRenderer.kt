package com.mohart.captionstudio

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * Draws captions with Android's native Canvas. The exact same code feeds the live preview
 * and the exported video, so what you see is what you get.
 */
class CaptionRenderer(private val fonts: FontLibrary) {

    private class WordBox(
        val text: String, val x: Float, val w: Float, val line: Int,
        val startFrac: Float, val endFrac: Float
    )

    private class LineBox(
        val left: Float, val width: Float, val baseline: Float,
        val textTop: Float, val textBottom: Float
    )

    private class Layout(
        val words: List<WordBox>, val lines: List<LineBox>,
        val blockTop: Float, val blockBottom: Float, val rtl: Boolean
    )

    private class Fx {
        var alpha = 1f
        var scale = 1f
        var tx = 0f
        var ty = 0f
        var rot = 0f
        var track = 0f
        var reveal = 1f
        var glitch = 0f
        fun reset(): Fx {
            alpha = 1f; scale = 1f; tx = 0f; ty = 0f; rot = 0f; track = 0f; reveal = 1f; glitch = 0f
            return this
        }
    }

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    private val shadow = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeJoin = Paint.Join.ROUND }
    private val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val layerRect = RectF()
    private val fx = Fx()

    private val cache = object : LinkedHashMap<String, Layout>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Layout>?): Boolean = size > 64
    }

    // ---------------------------------------------------------------- public API

    fun activeCaption(captions: List<Caption>, tMs: Long): Caption? =
        captions.firstOrNull { tMs >= it.startMs && tMs < it.endMs }

    fun draw(canvas: Canvas, w: Int, h: Int, captions: List<Caption>, tMs: Long, style: CaptionStyle) {
        val c = activeCaption(captions, tMs) ?: return
        drawCaption(canvas, w, h, c, tMs, style)
    }

    /** Small static preview used by the style picker tiles. */
    fun drawSample(canvas: Canvas, w: Int, h: Int, base: CaptionStyle, text: String) {
        val s = base.copy(
            sizeFrac = 0.22f, yFrac = 0.5f, maxWidthFrac = 0.92f,
            animIn = Anim.NONE, animOut = Anim.NONE
        )
        draw(canvas, w, h, listOf(Caption(-1, 0, 4000, text)), 2000, s)
    }

    /** Changes only when the picture changes, so the exporter can reuse the previous bitmap. */
    fun frameKey(captions: List<Caption>, tMs: Long, style: CaptionStyle, w: Int, h: Int): Long {
        val c = activeCaption(captions, tMs) ?: return -1L
        val layout = layoutFor(c, w, h, style)
        val n = layout.words.size
        if (n == 0) return -1L
        val local = tMs - c.startMs
        val rem = c.endMs - tMs
        val inMs = introMs(c, style, n)
        val outLen = outMs(c, style)
        val animating = local < inMs || (outLen > 0 && rem < outLen)
        if (animating) return tMs * 2L + 1L
        val dur = max(1L, c.endMs - c.startMs)
        val wi = if (style.karaoke != Karaoke.NONE) activeWord(layout, local, dur) else 0
        return (c.id * 1000L + wi) * 2L
    }

    // ---------------------------------------------------------------- timing helpers

    private fun introMs(c: Caption, style: CaptionStyle, nWords: Int): Long {
        val dur = c.endMs - c.startMs
        return when {
            style.animIn == Anim.NONE -> 0L
            style.animIn.isWordAnim -> min((dur * 0.7).toLong(), nWords * 200L).coerceAtLeast(120L)
            style.animIn == Anim.TYPEWRITER ->
                min((dur * 0.7).toLong(), max(300L, c.text.length * 45L)).coerceAtLeast(150L)
            else -> min(style.animMs, dur / 2)
        }
    }

    private fun outMs(c: Caption, style: CaptionStyle): Long {
        if (style.animOut == Anim.NONE) return 0L
        return min(style.animMs, (c.endMs - c.startMs) / 3)
    }

    private fun safeOut(a: Anim): Anim = when (a) {
        Anim.TYPEWRITER, Anim.WORD_POP, Anim.WORD_FADE, Anim.WORD_RISE,
        Anim.TRACKING, Anim.GLITCH, Anim.ROTATE_IN -> Anim.FADE
        else -> a
    }

    private fun activeWord(layout: Layout, local: Long, dur: Long): Int {
        val frac = local.toFloat() / dur
        var idx = 0
        for ((i, wb) in layout.words.withIndex()) {
            if (wb.startFrac <= frac) idx = i
        }
        return idx
    }

    // ---------------------------------------------------------------- layout

    private fun isRtl(text: String): Boolean {
        var ar = 0
        var lat = 0
        for (ch in text) {
            val c = ch.code
            if (c in 0x0600..0x06FF || c in 0x0750..0x077F || c in 0x08A0..0x08FF ||
                c in 0xFB50..0xFDFF || c in 0xFE70..0xFEFF
            ) ar++
            else if (ch.isLetter() && c < 0x0600) lat++
        }
        return ar > 0 && ar >= lat
    }

    private fun configure(p: Paint, tf: Typeface, size: Float, spacing: Float) {
        p.typeface = tf
        p.textSize = size
        p.letterSpacing = spacing
    }

    private fun layoutFor(c: Caption, w: Int, h: Int, style: CaptionStyle): Layout {
        val key = "${c.id}|${c.text.hashCode()}|$w|$h|${style.fontKey}|${style.sizeFrac}|" +
            "${style.uppercase}|${style.maxWidthFrac}|${style.yFrac}|${style.lineSpacing}|" +
            "${style.letterSpacingEm}|${style.karaoke}"
        cache[key]?.let { return it }

        val size = style.sizeFrac * h
        val rtl = isRtl(c.text)
        configure(fill, fonts.typeface(style.fontKey), size, if (rtl) 0f else style.letterSpacingEm)
        fill.shader = null
        fill.style = Paint.Style.FILL

        val raw = if (style.uppercase && !rtl) c.text.uppercase(Locale.getDefault()) else c.text
        val maxW = style.maxWidthFrac * w
        val space = fill.measureText(" ")

        val visualLines = ArrayList<List<String>>()
        for (para in raw.split("\n")) {
            val ws = para.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
            if (ws.isEmpty()) continue
            var cur = ArrayList<String>()
            var curW = 0f
            for (word in ws) {
                val ww = fill.measureText(word)
                if (cur.isNotEmpty() && curW + space + ww > maxW) {
                    visualLines.add(cur)
                    cur = ArrayList()
                    curW = 0f
                }
                if (cur.isNotEmpty()) curW += space
                curW += ww
                cur.add(word)
            }
            if (cur.isNotEmpty()) visualLines.add(cur)
        }
        if (visualLines.isEmpty()) {
            val empty = Layout(emptyList(), emptyList(), 0f, 0f, rtl)
            cache[key] = empty
            return empty
        }

        val fm = fill.fontMetrics
        val glyphH = fm.descent - fm.ascent
        val lineH = glyphH * style.lineSpacing
        val blockH = lineH * visualLines.size
        var top = style.yFrac * h - blockH / 2f
        top = top.coerceIn(h * 0.03f, max(h * 0.03f, h * 0.97f - blockH))

        val totalWeight = visualLines.sumOf { l -> l.sumOf { it.length + 1 } }.toFloat()
        var cumWeight = 0f

        val words = ArrayList<WordBox>()
        val lines = ArrayList<LineBox>()
        for ((k, lw) in visualLines.withIndex()) {
            val widths = lw.map { fill.measureText(it) }
            val lineWidth = widths.sum() + space * (lw.size - 1)
            val left = (w - lineWidth) / 2f
            val lTop = top + k * lineH
            val baseline = lTop + (lineH - glyphH) / 2f - fm.ascent
            lines.add(LineBox(left, lineWidth, baseline, baseline + fm.ascent, baseline + fm.descent))
            var off = 0f
            for ((i, word) in lw.withIndex()) {
                val ww = widths[i]
                val x = if (rtl) left + lineWidth - off - ww else left + off
                val sf = cumWeight / totalWeight * 0.94f
                cumWeight += (word.length + 1)
                val ef = cumWeight / totalWeight * 0.94f
                words.add(WordBox(word, x, ww, k, sf, ef))
                off += ww + space
            }
        }
        val layout = Layout(words, lines, top, top + blockH, rtl)
        cache[key] = layout
        return layout
    }

    // ---------------------------------------------------------------- easing + effects

    private fun easeOutCubic(x: Float): Float = 1f - (1f - x) * (1f - x) * (1f - x)

    private fun easeOutBack(x: Float): Float {
        val c1 = 1.70158f
        val c3 = c1 + 1f
        val t = x - 1f
        return 1f + c3 * t * t * t + c1 * t * t
    }

    private fun bounceOut(x0: Float): Float {
        var x = x0
        val n1 = 7.5625f
        val d1 = 2.75f
        return when {
            x < 1f / d1 -> n1 * x * x
            x < 2f / d1 -> { x -= 1.5f / d1; n1 * x * x + 0.75f }
            x < 2.5f / d1 -> { x -= 2.25f / d1; n1 * x * x + 0.9375f }
            else -> { x -= 2.625f / d1; n1 * x * x + 0.984375f }
        }
    }

    /** p = visibility progress, 0 (hidden) .. 1 (fully shown). */
    private fun applyFx(anim: Anim, p: Float, w: Int, h: Int, f: Fx) {
        if (p >= 1f) return
        val e = easeOutCubic(p)
        when (anim) {
            Anim.FADE -> f.alpha *= p
            Anim.POP -> {
                f.scale *= easeOutBack(p).coerceAtLeast(0f)
                f.alpha *= min(1f, p * 4f)
            }
            Anim.BOUNCE -> {
                f.ty += -(1f - bounceOut(p)) * 0.14f * h
                f.alpha *= min(1f, p * 6f)
            }
            Anim.SLIDE_UP -> { f.ty += (1f - e) * 0.06f * h; f.alpha *= p }
            Anim.SLIDE_DOWN -> { f.ty -= (1f - e) * 0.06f * h; f.alpha *= p }
            Anim.SLIDE_LEFT -> { f.tx += (1f - e) * 0.18f * w; f.alpha *= p }
            Anim.SLIDE_RIGHT -> { f.tx -= (1f - e) * 0.18f * w; f.alpha *= p }
            Anim.ZOOM_IN -> { f.scale *= 2.4f - 1.4f * e; f.alpha *= p }
            Anim.ROTATE_IN -> {
                f.rot += (1f - e) * -14f
                f.scale *= (0.6f + 0.4f * easeOutBack(p)).coerceAtLeast(0f)
                f.alpha *= min(1f, p * 3f)
            }
            Anim.TRACKING -> { f.track += (1f - e) * 0.4f; f.alpha *= p }
            Anim.GLITCH -> {
                f.glitch = 1f - p
                f.alpha *= if ((p * 20f).toInt() % 2 == 0) 1f else 0.6f
            }
            else -> {}
        }
    }

    // ---------------------------------------------------------------- drawing

    private fun drawCaption(canvas: Canvas, w: Int, h: Int, c: Caption, tMs: Long, style: CaptionStyle) {
        val layout = layoutFor(c, w, h, style)
        val n = layout.words.size
        if (n == 0) return
        val dur = max(1L, c.endMs - c.startMs)
        val local = tMs - c.startMs
        val rem = c.endMs - tMs
        val inMs = introMs(c, style, n)
        val outLen = outMs(c, style)
        val inP = if (inMs > 0) (local.toFloat() / inMs).coerceIn(0f, 1f) else 1f
        val outP = if (outLen > 0 && rem < outLen) (rem.toFloat() / outLen).coerceIn(0f, 1f) else 1f

        val f = fx.reset()
        applyFx(style.animIn, inP, w, h, f)
        if (style.animIn == Anim.TYPEWRITER || style.animIn.isWordAnim) f.reveal = inP
        if (outP < 1f) applyFx(safeOut(style.animOut), outP, w, h, f)
        if (f.alpha <= 0.01f) return

        val size = style.sizeFrac * h
        val cx = w / 2f
        val cy = (layout.blockTop + layout.blockBottom) / 2f

        val saved: Int
        if (f.alpha < 0.999f) {
            layerRect.set(0f, 0f, w.toFloat(), h.toFloat())
            saved = canvas.saveLayerAlpha(layerRect, (f.alpha.coerceIn(0f, 1f) * 255f).toInt())
        } else {
            saved = canvas.save()
        }
        canvas.translate(f.tx, f.ty)
        if (f.rot != 0f) canvas.rotate(f.rot, cx, cy)
        if (f.scale != 1f) canvas.scale(f.scale, f.scale, cx, cy)

        val pad = style.boxPadEm * size
        val radius = style.boxRadiusEm * size

        // background boxes
        if (style.boxMode != BoxMode.NONE) {
            boxPaint.color = style.boxColor
            if (style.boxMode == BoxMode.BLOCK) {
                var l = Float.MAX_VALUE
                var r = 0f
                for (ln in layout.lines) {
                    l = min(l, ln.left)
                    r = max(r, ln.left + ln.width)
                }
                rect.set(
                    l - pad * 1.6f, layout.lines.first().textTop - pad,
                    r + pad * 1.6f, layout.lines.last().textBottom + pad
                )
                canvas.drawRoundRect(rect, radius, radius, boxPaint)
            } else {
                for (ln in layout.lines) {
                    rect.set(
                        ln.left - pad * 1.4f, ln.textTop - pad * 0.6f,
                        ln.left + ln.width + pad * 1.4f, ln.textBottom + pad * 0.6f
                    )
                    canvas.drawRoundRect(rect, radius, radius, boxPaint)
                }
            }
        }

        val activeIdx = if (style.karaoke != Karaoke.NONE) activeWord(layout, local, dur) else -1
        val totalChars = layout.words.sumOf { it.text.length }
        val visibleChars = if (style.animIn == Anim.TYPEWRITER && !layout.rtl)
            ceil(f.reveal * totalChars).toInt() else Int.MAX_VALUE
        var charsBefore = 0

        for ((i, wb) in layout.words.withIndex()) {
            val ln = layout.lines[wb.line]
            var text = wb.text
            var wAlpha = 1f
            var wScale = 1f
            var wDy = 0f

            val before = charsBefore
            charsBefore += wb.text.length

            if (style.animIn == Anim.TYPEWRITER) {
                if (layout.rtl) {
                    if (f.reveal * n <= i) continue
                } else {
                    val avail = visibleChars - before
                    if (avail <= 0) continue
                    if (avail < text.length) text = text.substring(0, avail)
                }
            } else if (style.animIn.isWordAnim) {
                val wp = (f.reveal * n - i).coerceIn(0f, 1f)
                if (wp <= 0f) continue
                when (style.animIn) {
                    Anim.WORD_POP -> {
                        wScale = easeOutBack(wp).coerceAtLeast(0f)
                        wAlpha = min(1f, wp * 3f)
                    }
                    Anim.WORD_FADE -> wAlpha = wp
                    Anim.WORD_RISE -> {
                        wDy = (1f - easeOutCubic(wp)) * 0.05f * h
                        wAlpha = wp
                    }
                    else -> {}
                }
            }

            val isActive = i == activeIdx
            var color = style.textColor
            if (isActive && (style.karaoke == Karaoke.COLOR || style.karaoke == Karaoke.SCALE)) {
                color = style.highlightColor
            }
            if (isActive && style.karaoke == Karaoke.SCALE) wScale *= 1.18f

            if (isActive && style.karaoke == Karaoke.BOX) {
                boxPaint.color = style.highlightColor
                rect.set(
                    wb.x - pad * 0.9f, ln.textTop - pad * 0.45f,
                    wb.x + wb.w + pad * 0.9f, ln.textBottom + pad * 0.45f
                )
                canvas.drawRoundRect(rect, radius, radius, boxPaint)
            }

            val wSaved: Int
            if (wAlpha < 0.999f) {
                layerRect.set(wb.x - size, ln.textTop - size, wb.x + wb.w + size, ln.textBottom + size)
                wSaved = canvas.saveLayerAlpha(layerRect, (wAlpha.coerceIn(0f, 1f) * 255f).toInt())
            } else {
                wSaved = canvas.save()
            }
            if (wScale != 1f || wDy != 0f) {
                canvas.translate(0f, wDy)
                canvas.scale(wScale, wScale, wb.x + wb.w / 2f, (ln.textTop + ln.textBottom) / 2f)
            }
            drawWord(canvas, text, wb.x, ln.baseline, ln.textTop, ln.textBottom, layout.rtl, size, color, style, f, tMs)
            canvas.restoreToCount(wSaved)
        }
        canvas.restoreToCount(saved)
    }

    private fun drawRun(canvas: Canvas, text: String, x: Float, y: Float, rtl: Boolean, p: Paint) {
        if (rtl) canvas.drawTextRun(text, 0, text.length, 0, text.length, x, y, true, p)
        else canvas.drawText(text, x, y, p)
    }

    private fun drawWord(
        canvas: Canvas, text: String, x: Float, baseline: Float, top: Float, bottom: Float,
        rtl: Boolean, size: Float, color: Int, style: CaptionStyle, f: Fx, tMs: Long
    ) {
        val tf = fonts.typeface(style.fontKey)
        val spacing = if (rtl) 0f else style.letterSpacingEm + f.track
        configure(fill, tf, size, spacing)
        configure(stroke, tf, size, spacing)
        configure(shadow, tf, size, spacing)
        val x0 = if (rtl) x else x - f.track * size * text.length / 2f
        fill.shader = null
        fill.style = Paint.Style.FILL

        // glitch ghosts
        if (f.glitch > 0f) {
            val rnd = java.util.Random(tMs / 40)
            val off = size * 0.12f * f.glitch * (rnd.nextFloat() + 0.3f)
            fill.color = 0xFFFF2D55.toInt()
            drawRun(canvas, text, x0 - off, baseline, rtl, fill)
            fill.color = 0xFF00E5FF.toInt()
            drawRun(canvas, text, x0 + off, baseline, rtl, fill)
        }

        // glow
        if (style.glowEm > 0f && style.glowColor != 0) {
            fill.color = color
            fill.setShadowLayer(style.glowEm * size, 0f, 0f, style.glowColor)
            drawRun(canvas, text, x0, baseline, rtl, fill)
            drawRun(canvas, text, x0, baseline, rtl, fill)
            fill.clearShadowLayer()
        }

        // shadow (soft or hard)
        if (style.shadowColor != 0) {
            shadow.color = style.shadowColor
            val sw = style.strokeEm * size
            if (sw > 0f) {
                shadow.style = Paint.Style.FILL_AND_STROKE
                shadow.strokeWidth = sw
            } else {
                shadow.style = Paint.Style.FILL
            }
            val soft = style.shadowBlurEm > 0f
            if (soft) {
                shadow.setShadowLayer(
                    style.shadowBlurEm * size, style.shadowDxEm * size, style.shadowDyEm * size, style.shadowColor
                )
            } else {
                shadow.clearShadowLayer()
            }
            val ox = if (soft) 0f else style.shadowDxEm * size
            val oy = if (soft) 0f else style.shadowDyEm * size
            drawRun(canvas, text, x0 + ox, baseline + oy, rtl, shadow)
            shadow.clearShadowLayer()
        }

        // outline
        if (style.strokeEm > 0f && style.strokeColor != 0) {
            stroke.color = style.strokeColor
            stroke.strokeWidth = style.strokeEm * size
            drawRun(canvas, text, x0, baseline, rtl, stroke)
        }

        // fill (solid or vertical gradient)
        fill.color = WHITE
        if (style.gradTop != 0 && style.gradBottom != 0 && color == style.textColor) {
            fill.shader = LinearGradient(0f, top, 0f, bottom, style.gradTop, style.gradBottom, Shader.TileMode.CLAMP)
        } else {
            fill.color = color
        }
        drawRun(canvas, text, x0, baseline, rtl, fill)
        fill.shader = null
    }
}
