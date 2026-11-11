package com.mohart.captionstudio

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

class EditorViewModel(app: Application) : AndroidViewModel(app) {

    private val ctx: Application = app

    val fonts = FontLibrary(app)

    // ---- video + captions
    var videoUri by mutableStateOf<Uri?>(null)
    var videoInfo by mutableStateOf<VideoInfo?>(null)
    val captions = mutableStateListOf<Caption>()
    var timingVersion by mutableIntStateOf(0)
    var analyzing by mutableStateOf(false)
    var transcribing by mutableStateOf(false)
    var notice by mutableStateOf<String?>(null)
    private var analysis: SpeechMap? = null
    private var nextId = 1L

    // ---- music / audio track
    var musicUri by mutableStateOf<Uri?>(null)
    var musicVolume by mutableFloatStateOf(0.55f)
    var originalVolume by mutableFloatStateOf(1.0f)
    var musicLoop by mutableStateOf(true)

    // ---- style choice + tweaks
    var styleId by mutableStateOf(StyleLibrary.all.first().id)
    var fontOverride by mutableStateOf<String?>(null)
    var sizeScale by mutableFloatStateOf(1f)
    var yOverride by mutableStateOf<Float?>(null)
    var animInOverride by mutableStateOf<Anim?>(null)
    var animOutOverride by mutableStateOf<Anim?>(null)
    var colorOverride by mutableStateOf<Int?>(null)
    var highlightOverride by mutableStateOf<Int?>(null)

    // ---- export
    var quality by mutableIntStateOf(720)
    var aspect by mutableStateOf(Aspect.ORIGINAL)
    var exporting by mutableStateOf(false)
    var exportProgress by mutableFloatStateOf(0f)
    var exportMessage by mutableStateOf<String?>(null)
    var savedUri by mutableStateOf<Uri?>(null)
    private var exporter: Exporter? = null

    fun baseStyle(): CaptionStyle = StyleLibrary.byId(styleId)

    fun resolvedStyle(): CaptionStyle {
        val b = baseStyle()
        return b.copy(
            fontKey = fontOverride ?: b.fontKey,
            sizeFrac = b.sizeFrac * sizeScale,
            yFrac = yOverride ?: b.yFrac,
            animIn = animInOverride ?: b.animIn,
            animOut = animOutOverride ?: b.animOut,
            textColor = colorOverride ?: b.textColor,
            highlightColor = highlightOverride ?: b.highlightColor,
            // a colour pick replaces any gradient so the pick is actually visible
            gradTop = if (colorOverride != null) 0 else b.gradTop,
            gradBottom = if (colorOverride != null) 0 else b.gradBottom
        )
    }

    fun selectStyle(id: String) {
        styleId = id
        resetTweaks()
    }

    fun resetTweaks() {
        fontOverride = null
        sizeScale = 1f
        yOverride = null
        animInOverride = null
        animOutOverride = null
        colorOverride = null
        highlightOverride = null
    }

    // ---------------------------------------------------------------- video

    fun loadVideo(uri: Uri) {
        val info = VideoProbe.probe(ctx, uri)
        if (info == null) {
            notice = "Could not read that video."
            return
        }
        videoUri = uri
        videoInfo = info
        analysis = null
        savedUri = null
        exportMessage = null
        // keep music if already set
    }

    fun loadMusic(uri: Uri) {
        musicUri = uri
        notice = "Music track added."
    }

    fun clearMusic() {
        musicUri = null
        notice = "Music track removed."
    }

    // ---------------------------------------------------------------- captions

    fun importSrt(uri: Uri) {
        val text = try {
            ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        } catch (t: Throwable) {
            null
        }
        if (text == null) {
            notice = "Could not read that file."
            return
        }
        val parsed = Srt.parse(text, nextId)
        if (parsed.isEmpty()) {
            notice = "No captions found in that file."
            return
        }
        nextId += parsed.size + 1
        captions.clear()
        captions.addAll(parsed)
        timingVersion++
        notice = "Imported ${parsed.size} captions."
    }

    fun pasteText(text: String) {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) return
        val dur = videoInfo?.durationMs ?: (lines.size * 2500L)
        val times = AutoTimer.times(lines, analysis ?: SpeechMap(0L, dur, emptyList()), dur)
        captions.clear()
        for ((i, l) in lines.withIndex()) {
            captions.add(Caption(nextId++, times[i][0], times[i][1], l))
        }
        timingVersion++
        retimeToSpeech()
    }

    fun retimeToSpeech() {
        val uri = videoUri ?: return
        val dur = videoInfo?.durationMs ?: return
        if (captions.isEmpty() || analyzing) return
        viewModelScope.launch {
            analyzing = true
            val map = analysis ?: withContext(Dispatchers.Default) {
                AudioAnalyzer.analyze(ctx, uri, dur)
            }
            analysis = map
            val times = AutoTimer.times(captions.map { it.text }, map, dur)
            if (times.size == captions.size) {
                for (i in captions.indices) {
                    captions[i] = captions[i].copy(startMs = times[i][0], endMs = times[i][1])
                }
                timingVersion++
                notice = if (map.gaps.isEmpty() && map.startMs == 0L)
                    "No clear speech found, so captions were spread evenly."
                else "Captions timed to the speech. Fine-tune any line if needed."
            }
            analyzing = false
        }
    }

    /**
     * Scaffold for transcription.
     * Detects speech regions from the energy map and creates editable caption slots.
     * Real on-device STT (Whisper / ML Kit / Vosk) can later fill the text of these slots.
     * For now it creates timed placeholders so you can type the words (or lyrics) quickly.
     */
    fun transcribeAudio() {
        val uri = videoUri ?: return
        val dur = videoInfo?.durationMs ?: return
        if (transcribing || analyzing) return
        viewModelScope.launch {
            transcribing = true
            notice = "Analyzing audio for speech regions…"
            val map = analysis ?: withContext(Dispatchers.Default) {
                AudioAnalyzer.analyze(ctx, uri, dur)
            }
            analysis = map

            // Build caption slots from speech segments (gaps are the pauses)
            val segments = buildSpeechSegments(map, dur)
            if (segments.isEmpty()) {
                notice = "No clear speech regions found. Try pasting text or importing an SRT."
                transcribing = false
                return@launch
            }

            captions.clear()
            for ((idx, seg) in segments.withIndex()) {
                val placeholder = if (segments.size == 1) "Type the spoken words or lyrics here"
                else "Caption ${idx + 1} — type the words here"
                captions.add(Caption(nextId++, seg.first, seg.second, placeholder))
            }
            timingVersion++
            notice = "Created ${segments.size} timed caption slots from the audio. " +
                    "Replace the placeholder text with the actual words or lyrics. " +
                    "Full automatic speech-to-text can be added later with an on-device model."
            transcribing = false
        }
    }

    private fun buildSpeechSegments(map: SpeechMap, durationMs: Long): List<Pair<Long, Long>> {
        val speechStart = max(0L, map.startMs)
        val speechEnd = min(durationMs, map.endMs).coerceAtLeast(speechStart + 500)
        if (map.gaps.isEmpty()) {
            // No clear pauses → one big segment or a few evenly split
            val n = 4
            val len = (speechEnd - speechStart) / n
            return (0 until n).map { i ->
                val s = speechStart + i * len
                val e = if (i == n - 1) speechEnd else s + len
                s to e
            }
        }
        val segments = ArrayList<Pair<Long, Long>>()
        var cursor = speechStart
        for (g in map.gaps) {
            if (g.first > cursor + 400) {
                segments.add(cursor to g.first)
            }
            cursor = max(cursor, g.last)
        }
        if (speechEnd > cursor + 400) {
            segments.add(cursor to speechEnd)
        }
        // Merge very short ones and limit count
        val merged = ArrayList<Pair<Long, Long>>()
        for (seg in segments) {
            if (merged.isNotEmpty() && seg.first - merged.last().second < 300) {
                val last = merged.removeAt(merged.lastIndex)
                merged.add(last.first to seg.second)
            } else if (seg.second - seg.first >= 400) {
                merged.add(seg)
            }
        }
        return merged.take(40) // safety
    }

    fun updateText(id: Long, text: String) {
        val i = captions.indexOfFirst { it.id == id }
        if (i >= 0) captions[i] = captions[i].copy(text = text)
    }

    fun setTimes(id: Long, start: Long? = null, end: Long? = null) {
        val i = captions.indexOfFirst { it.id == id }
        if (i < 0) return
        val c = captions[i]
        captions[i] = c.copy(
            startMs = max(0L, start ?: c.startMs),
            endMs = max(0L, end ?: c.endMs)
        )
    }

    fun addAt(ms: Long) {
        val dur = videoInfo?.durationMs ?: (ms + 2000L)
        val s = min(ms, max(0L, dur - 500L))
        val e = min(dur, s + 2000L).coerceAtLeast(s + 500L)
        captions.add(Caption(nextId++, s, e, "New caption"))
        val sorted = captions.sortedBy { it.startMs }
        captions.clear()
        captions.addAll(sorted)
        timingVersion++
    }

    fun delete(id: Long) {
        val i = captions.indexOfFirst { it.id == id }
        if (i >= 0) captions.removeAt(i)
    }

    // ---------------------------------------------------------------- export

    fun startExport() {
        val uri = videoUri ?: return
        val info = videoInfo ?: return
        if (captions.isEmpty() || exporting) return
        exporting = true
        exportProgress = 0f
        exportMessage = null
        savedUri = null
        val ex = Exporter(ctx)
        exporter = ex
        val music = if (musicUri != null) {
            MusicSettings(musicUri, musicVolume, originalVolume, musicLoop)
        } else null
        ex.start(uri, info, captions.toList(), resolvedStyle(), aspect, quality, fonts, music,
            object : Exporter.Callback {
                override fun onProgress(p: Float) {
                    exportProgress = p
                }

                override fun onDone(uri: Uri?, message: String) {
                    exporting = false
                    exportProgress = 1f
                    savedUri = uri
                    exportMessage = message
                }

                override fun onError(message: String) {
                    exporting = false
                    exportMessage = "Export failed: $message"
                }
            })
    }

    fun cancelExport() {
        exporter?.cancel()
        exporting = false
        exportMessage = "Export cancelled."
    }
}
