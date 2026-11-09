package com.mohart.captionstudio

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

object Srt {
    private val timeRe = Regex("""(?:(\d+):)?(\d{1,2}):(\d{2})[,.](\d{1,3})""")
    private val tagRe = Regex("<[^>]+>")
    private val braceRe = Regex("\\{[^}]*\\}")

    private fun parseTime(s: String): Long? {
        val m = timeRe.find(s.trim()) ?: return null
        val h = m.groupValues[1].ifEmpty { "0" }.toLong()
        val mi = m.groupValues[2].toLong()
        val se = m.groupValues[3].toLong()
        val ms = m.groupValues[4].padEnd(3, '0').toLong()
        return ((h * 60 + mi) * 60 + se) * 1000 + ms
    }

    fun parse(raw: String, firstId: Long): List<Caption> {
        val text = raw.replace("\uFEFF", "").replace("\r\n", "\n").replace('\r', '\n')
        val out = ArrayList<Caption>()
        var id = firstId
        for (block in text.split(Regex("\n\\s*\n"))) {
            val lines = block.split("\n").map { it.trimEnd() }.filter { it.isNotBlank() }
            val ti = lines.indexOfFirst { it.contains("-->") }
            if (ti < 0) continue
            val parts = lines[ti].split("-->")
            if (parts.size < 2) continue
            val start = parseTime(parts[0]) ?: continue
            val end = parseTime(parts[1]) ?: continue
            val body = lines.drop(ti + 1).joinToString("\n")
                .replace(tagRe, "").replace(braceRe, "").trim()
            if (body.isEmpty()) continue
            out.add(Caption(id++, start, max(end, start + 200), body))
        }
        return out
    }
}

object VideoProbe {
    fun probe(ctx: Context, uri: Uri): VideoInfo? {
        val r = MediaMetadataRetriever()
        return try {
            r.setDataSource(ctx, uri)
            val dur = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            var w = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            var h = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            val rot = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            if (rot == 90 || rot == 270) {
                val t = w; w = h; h = t
            }
            if (w <= 0 || h <= 0 || dur <= 0) null else VideoInfo(dur, w, h)
        } catch (t: Throwable) {
            null
        } finally {
            try { r.release() } catch (_: Throwable) {}
        }
    }
}

/** Where speech starts/ends and where the natural pauses are. */
data class SpeechMap(val startMs: Long, val endMs: Long, val gaps: List<LongRange>)

object AudioAnalyzer {
    private const val WIN_MS = 50

    fun analyze(ctx: Context, uri: Uri, durationMs: Long): SpeechMap {
        val fallback = SpeechMap(0L, durationMs, emptyList())
        val ex = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            ex.setDataSource(ctx, uri, null)
            var track = -1
            var fmt: MediaFormat? = null
            for (i in 0 until ex.trackCount) {
                val f = ex.getTrackFormat(i)
                if (f.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                    track = i; fmt = f; break
                }
            }
            if (track < 0 || fmt == null) return fallback
            ex.selectTrack(track)
            val mime = fmt.getString(MediaFormat.KEY_MIME) ?: return fallback
            val dec = MediaCodec.createDecoderByType(mime)
            codec = dec
            dec.configure(fmt, null, null, 0)
            dec.start()

            var rate = if (fmt.containsKey(MediaFormat.KEY_SAMPLE_RATE)) fmt.getInteger(MediaFormat.KEY_SAMPLE_RATE) else 44100
            var ch = if (fmt.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) fmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT) else 2
            var winFrames = max(1, rate * WIN_MS / 1000)

            val rms = ArrayList<Float>()
            var sumSq = 0.0
            var cnt = 0
            val info = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false

            while (!outputDone) {
                if (!inputDone) {
                    val inIdx = dec.dequeueInputBuffer(10_000)
                    if (inIdx >= 0) {
                        val buf = dec.getInputBuffer(inIdx)!!
                        val n = ex.readSampleData(buf, 0)
                        if (n < 0) {
                            dec.queueInputBuffer(inIdx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            dec.queueInputBuffer(inIdx, 0, n, ex.sampleTime, 0)
                            ex.advance()
                        }
                    }
                }
                val outIdx = dec.dequeueOutputBuffer(info, 10_000)
                if (outIdx >= 0) {
                    val ob = dec.getOutputBuffer(outIdx)
                    if (ob != null && info.size > 0) {
                        ob.position(info.offset)
                        ob.limit(info.offset + info.size)
                        val sb = ob.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                        while (sb.remaining() >= ch) {
                            var s = 0
                            for (k in 0 until ch) s += sb.get().toInt()
                            val v = s / ch
                            sumSq += v.toDouble() * v
                            cnt++
                            if (cnt >= winFrames) {
                                rms.add(sqrt(sumSq / cnt).toFloat())
                                sumSq = 0.0
                                cnt = 0
                            }
                        }
                    }
                    dec.releaseOutputBuffer(outIdx, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
                } else if (outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    val of = dec.outputFormat
                    if (of.containsKey(MediaFormat.KEY_SAMPLE_RATE)) rate = of.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    if (of.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) ch = max(1, of.getInteger(MediaFormat.KEY_CHANNEL_COUNT))
                    winFrames = max(1, rate * WIN_MS / 1000)
                }
            }

            val n = rms.size
            if (n < 10) return fallback
            val sorted = rms.sorted()
            val floor = sorted[(n * 0.2).toInt().coerceIn(0, n - 1)]
            val peak = sorted[(n * 0.95).toInt().coerceIn(0, n - 1)]
            val thr = max(floor + 0.12f * (peak - floor), 200f)
            val speech = BooleanArray(n) { rms[it] > thr }
            val first = speech.indexOfFirst { it }
            val last = speech.indexOfLast { it }
            if (first < 0) return fallback

            val gaps = ArrayList<LongRange>()
            var i = first
            while (i <= last) {
                if (!speech[i]) {
                    var j = i
                    while (j <= last && !speech[j]) j++
                    if (j - i >= 6) gaps.add((i * WIN_MS).toLong() until (j * WIN_MS).toLong())
                    i = j
                } else i++
            }
            return SpeechMap(first * WIN_MS.toLong(), (last + 1) * WIN_MS.toLong(), gaps)
        } catch (t: Throwable) {
            return fallback
        } finally {
            try { codec?.stop() } catch (_: Throwable) {}
            try { codec?.release() } catch (_: Throwable) {}
            try { ex.release() } catch (_: Throwable) {}
        }
    }
}

object AutoTimer {
    /** Returns [start, end] in ms for each line, spread across the speech and snapped to pauses. */
    fun times(lines: List<String>, map: SpeechMap, durationMs: Long): List<LongArray> {
        val n = lines.size
        if (n == 0) return emptyList()
        val s = max(0L, map.startMs - 80)
        val e = min(durationMs, map.endMs + 250).coerceAtLeast(s + 500L * n)
        val weights = lines.map { (it.length + 6).toDouble() }
        val total = weights.sum()

        val cutEnd = LongArray(max(0, n - 1))
        val cutStart = LongArray(max(0, n - 1))
        val used = HashSet<Int>()
        var cum = 0.0
        var minCut = s
        for (i in 0 until n - 1) {
            cum += weights[i]
            val target = s + ((e - s) * cum / total).toLong()
            var best = -1
            var bestD = Long.MAX_VALUE
            for ((gi, g) in map.gaps.withIndex()) {
                if (gi in used) continue
                val center = (g.first + g.last) / 2
                if (center <= minCut + 500) continue
                val d = abs(center - target)
                if (d < bestD && d <= 1500) {
                    bestD = d; best = gi
                }
            }
            if (best >= 0) {
                used.add(best)
                val g = map.gaps[best]
                val center = (g.first + g.last) / 2
                cutEnd[i] = min(center, g.first + 150)
                cutStart[i] = max(center, g.last - 150)
                minCut = center
            } else {
                val t = max(target, minCut + 500)
                cutEnd[i] = t - 40
                cutStart[i] = t + 40
                minCut = t
            }
        }
        val out = ArrayList<LongArray>()
        for (i in 0 until n) {
            val st = if (i == 0) s else cutStart[i - 1]
            var en = if (i == n - 1) e else cutEnd[i]
            if (en - st < 300) en = st + 300
            out.add(longArrayOf(st, en))
        }
        return out
    }
}
