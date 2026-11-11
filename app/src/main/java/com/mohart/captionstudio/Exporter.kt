package com.mohart.captionstudio

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.Presentation
import androidx.media3.effect.TextureOverlay
import androidx.media3.transformer.Composition
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.VideoEncoderSettings
import com.google.common.collect.ImmutableList
import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object OutputSize {
    /** quality = size of the short side (720 or 1080). */
    fun compute(info: VideoInfo?, aspect: Aspect, quality: Int): Pair<Int, Int> {
        val videoRatio = if (info != null) info.width.toFloat() / info.height else 9f / 16f
        val ratio = aspect.ratio ?: videoRatio
        val w: Int
        val h: Int
        if (ratio >= 1f) {
            w = (quality * ratio).roundToInt(); h = quality
        } else {
            w = quality; h = (quality / ratio).roundToInt()
        }
        return Pair(w and 1.inv(), h and 1.inv())
    }
}

/** Supplies one transparent caption bitmap per video frame. */
private class CaptionOverlay(
    private val renderer: CaptionRenderer,
    private val captions: List<Caption>,
    private val style: CaptionStyle,
    private val w: Int,
    private val h: Int
) : BitmapOverlay() {

    private val empty: Bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    private val pool: Array<Bitmap> = Array(2) { Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888) }
    private val canvases: Array<Canvas> = Array(2) { Canvas(pool[it]) }
    private var poolIdx = 0
    private var lastKey = Long.MIN_VALUE
    private var last: Bitmap = empty

    override fun getBitmap(presentationTimeUs: Long): Bitmap {
        val tMs = presentationTimeUs / 1000L
        val key = renderer.frameKey(captions, tMs, style, w, h)
        if (key == lastKey) return last
        lastKey = key
        if (key == -1L) {
            last = empty
            return empty
        }
        poolIdx = (poolIdx + 1) % 2
        val bmp = pool[poolIdx]
        bmp.eraseColor(Color.TRANSPARENT)
        renderer.draw(canvases[poolIdx], w, h, captions, tMs, style)
        last = bmp
        return bmp
    }
}

class Exporter(private val context: Context) {

    interface Callback {
        fun onProgress(p: Float)
        fun onDone(uri: Uri?, message: String)
        fun onError(message: String)
    }

    private val handler = Handler(Looper.getMainLooper())
    private var transformer: Transformer? = null
    private var active = false

    fun start(
        src: Uri,
        info: VideoInfo,
        captions: List<Caption>,
        style: CaptionStyle,
        aspect: Aspect,
        quality: Int,
        fonts: FontLibrary,
        music: MusicSettings?,
        cb: Callback
    ) {
        val (w, h) = OutputSize.compute(info, aspect, quality)
        val renderer = CaptionRenderer(fonts)
        val overlay = CaptionOverlay(renderer, captions, style, w, h)

        val videoEffects = ArrayList<Effect>()
        videoEffects.add(Presentation.createForWidthAndHeight(w, h, Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP))
        videoEffects.add(OverlayEffect(ImmutableList.of<TextureOverlay>(overlay)))

        val removeOriginalAudio = music != null && music.originalVolume <= 0.01f

        val videoItem = EditedMediaItem.Builder(MediaItem.fromUri(src))
            .setEffects(Effects(emptyList(), videoEffects))
            .setRemoveAudio(removeOriginalAudio)
            .build()

        val videoSequence = EditedMediaItemSequence.withAudioAndVideoFrom(listOf(videoItem))

        val composition: Composition = if (music?.uri != null) {
            val musicItem = EditedMediaItem.Builder(MediaItem.fromUri(music.uri)).build()
            val musicSeqBuilder = EditedMediaItemSequence.withAudioFrom(listOf(musicItem)).buildUpon()
            if (music.loop) {
                musicSeqBuilder.setIsLooping(true)
            }
            val musicSequence = musicSeqBuilder.build()
            Composition.Builder(videoSequence, musicSequence).build()
        } else {
            Composition.Builder(videoSequence).build()
        }

        val bitrate = min(16_000_000, max(3_000_000, (w.toLong() * h * 30 * 0.15).toInt()))
        val encoderFactory = DefaultEncoderFactory.Builder(context)
            .setRequestedVideoEncoderSettings(VideoEncoderSettings.Builder().setBitrate(bitrate).build())
            .setEnableFallback(true)
            .build()

        val outFile = File(context.cacheDir, "cs_${System.currentTimeMillis()}.mp4")
        if (outFile.exists()) outFile.delete()

        val t = Transformer.Builder(context)
            .setVideoMimeType(MimeTypes.VIDEO_H264)
            .setAudioMimeType(MimeTypes.AUDIO_AAC)
            .setEncoderFactory(encoderFactory)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    active = false
                    cb.onProgress(1f)
                    Thread {
                        val name = "CaptionStudio_${System.currentTimeMillis()}.mp4"
                        val result = try {
                            saveToMovies(outFile, name)
                        } catch (e: Throwable) {
                            Pair<Uri?, String>(null, "Export finished but saving failed: ${e.message}")
                        }
                        outFile.delete()
                        handler.post { cb.onDone(result.first, result.second) }
                    }.start()
                }

                override fun onError(
                    composition: Composition, exportResult: ExportResult, exportException: ExportException
                ) {
                    active = false
                    outFile.delete()
                    cb.onError("${exportException.errorCodeName}: ${exportException.message ?: "unknown error"}")
                }
            })
            .build()
        transformer = t
        active = true

        val holder = ProgressHolder()
        val poll = object : Runnable {
            override fun run() {
                if (!active) return
                val state = t.getProgress(holder)
                if (state == Transformer.PROGRESS_STATE_AVAILABLE) cb.onProgress(holder.progress / 100f)
                handler.postDelayed(this, 300)
            }
        }
        try {
            t.start(composition, outFile.absolutePath)
            handler.postDelayed(poll, 300)
        } catch (e: Throwable) {
            active = false
            cb.onError(e.message ?: "could not start")
        }
    }

    fun cancel() {
        active = false
        try { transformer?.cancel() } catch (_: Throwable) {}
    }

    private fun saveToMovies(file: File, name: String): Pair<Uri?, String> {
        if (Build.VERSION.SDK_INT >= 29) {
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, name)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CaptionStudio")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                ?: return Pair(null, "Could not create the output file.")
            resolver.openOutputStream(uri)?.use { out ->
                file.inputStream().use { it.copyTo(out) }
            }
            values.clear()
            values.put(MediaStore.Video.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            return Pair(uri, "Saved to Movies/CaptionStudio/$name")
        } else {
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir
            dir.mkdirs()
            val dst = File(dir, name)
            file.copyTo(dst, true)
            return Pair(null, "Saved to ${dst.absolutePath}")
        }
    }
}
