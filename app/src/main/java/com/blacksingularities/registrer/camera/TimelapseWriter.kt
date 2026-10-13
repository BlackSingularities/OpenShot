package com.blacksingularities.registrer.camera

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.SystemClock
import android.view.Surface
import com.blacksingularities.registrer.computational.FrameMath
import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Film poklatkowy kodowany na bieżąco: każda klatka (JPEG z aparatu) jest kadrowana do proporcji
 * filmu, skalowana i rysowana na powierzchnię kodera – na dysku nie zostają setki zdjęć, a film
 * jest gotowy zaraz po zatrzymaniu. Znaczniki czasu wynikają z numeru klatki i klatkażu.
 * Opcjonalne wyrównywanie jasności tłumi migotanie ekspozycji między klatkami.
 */
class TimelapseWriter private constructor(
    private val out: File,
    val width: Int,
    val height: Int,
    private val fps: Int,
    private val codec: MediaCodec,
    private val surface: Surface,
    private val muxer: MediaMuxer,
    private val deflicker: Boolean,
) {
    private val info = MediaCodec.BufferInfo()
    private var track = -1
    private var started = false
    private var written = 0
    var frames = 0
        private set
    private var smoothLuma = -1f
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG)

    /** Dodaje klatkę (wywoływać sekwencyjnie, poza wątkiem głównym). */
    fun addFrame(jpeg: ByteArray) {
        val bmp = decodeCropped(jpeg) ?: return
        try {
            paint.colorFilter = if (deflicker) flickerFilter(jpeg) else null
            val canvas = surface.lockHardwareCanvas()
            try {
                canvas.drawBitmap(bmp, null, Rect(0, 0, width, height), paint)
            } finally {
                surface.unlockCanvasAndPost(canvas)
            }
            frames++
        } finally {
            bmp.recycle()
        }
        // Koder nie może zostać zbyt daleko w tyle – inaczej kolejka powierzchni się zapcha.
        val deadline = SystemClock.elapsedRealtime() + 1500
        do {
            drain(false)
        } while (written < frames - 2 && SystemClock.elapsedRealtime() < deadline)
    }

    /** Kończy plik; true = film zapisany. */
    fun finish(): Boolean {
        var ok = false
        try {
            codec.signalEndOfInputStream()
            drain(true)
            if (started && written > 0) {
                muxer.stop()
                ok = true
            }
        } catch (e: Exception) {
            ok = false
        }
        runCatching { codec.stop() }
        runCatching { codec.release() }
        runCatching { surface.release() }
        runCatching { muxer.release() }
        if (!ok) out.delete()
        return ok
    }

    private fun drain(endOfStream: Boolean) {
        var idle = 0
        while (true) {
            val idx = codec.dequeueOutputBuffer(info, if (endOfStream) 20_000 else 2_000)
            when {
                idx == MediaCodec.INFO_TRY_AGAIN_LATER -> if (!endOfStream || ++idle > 250) return
                idx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> if (!started) {
                    track = muxer.addTrack(codec.outputFormat)
                    muxer.start()
                    started = true
                }
                idx >= 0 -> {
                    val buf = codec.getOutputBuffer(idx)
                    if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) info.size = 0
                    if (buf != null && info.size > 0 && started) {
                        info.presentationTimeUs = written * 1_000_000L / fps
                        buf.position(info.offset)
                        buf.limit(info.offset + info.size)
                        muxer.writeSampleData(track, buf, info)
                        written++
                    }
                    codec.releaseOutputBuffer(idx, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) return
                }
            }
        }
    }

    /** Środkowy wycinek o proporcjach filmu, zdekodowany z próbkowaniem nie mniejszym niż film. */
    private fun decodeCropped(jpeg: ByteArray): Bitmap? = runCatching {
        val decoder = FrameMath.regionDecoder(jpeg)
        try {
            val sw = decoder.width
            val sh = decoder.height
            val target = width.toFloat() / height
            var cw = sw
            var ch = (sw / target).roundToInt()
            if (ch > sh) {
                ch = sh
                cw = (sh * target).roundToInt()
            }
            val left = (sw - cw) / 2
            val top = (sh - ch) / 2
            var sample = 1
            while (cw / (sample * 2) >= width && ch / (sample * 2) >= height) sample *= 2
            val opts = BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            decoder.decodeRegion(Rect(left, top, left + cw, top + ch), opts)
        } finally {
            decoder.recycle()
        }
    }.getOrNull()

    /** Wzmocnienie wyrównujące średnią jasność do wygładzonej średniej poprzednich klatek. */
    private fun flickerFilter(jpeg: ByteArray): ColorMatrixColorFilter? {
        val mean = runCatching { FrameMath.smallLuma(jpeg, 16).mean() }.getOrNull() ?: return null
        if (mean < 2f) return null
        smoothLuma = if (smoothLuma < 0f) mean else smoothLuma + (mean - smoothLuma) * 0.2f
        val gain = (smoothLuma / mean).coerceIn(0.85f, 1.18f)
        if (kotlin.math.abs(gain - 1f) < 0.01f) return null
        return ColorMatrixColorFilter(ColorMatrix().apply { setScale(gain, gain, gain, 1f) })
    }

    companion object {
        /** Koder obsługujący dany rozmiar: najpierw HEVC (lepszy przy 4K/8K), potem H.264. */
        fun pickMime(w: Int, h: Int): String? {
            val list = runCatching { MediaCodecList(MediaCodecList.REGULAR_CODECS) }.getOrNull() ?: return null
            for (mime in listOf(MediaFormat.MIMETYPE_VIDEO_HEVC, MediaFormat.MIMETYPE_VIDEO_AVC)) {
                val ok = list.codecInfos.any { info ->
                    info.isEncoder && info.supportedTypes.any { it.equals(mime, ignoreCase = true) } && runCatching {
                        info.getCapabilitiesForType(mime).videoCapabilities?.isSizeSupported(w, h) == true
                    }.getOrDefault(false)
                }
                if (ok) return mime
            }
            return null
        }

        fun create(out: File, width: Int, height: Int, fps: Int, rotation: Int, deflicker: Boolean): TimelapseWriter? {
            val mime = pickMime(width, height) ?: return null
            val pixelsPerSecond = width.toLong() * height * fps
            val factor = if (mime == MediaFormat.MIMETYPE_VIDEO_HEVC) 0.11f else 0.18f
            val bitrate = (pixelsPerSecond * factor).toLong().coerceIn(8_000_000L, 120_000_000L).toInt()
            val format = MediaFormat.createVideoFormat(mime, width, height).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
                setInteger(MediaFormat.KEY_FRAME_RATE, fps)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
                setInteger(MediaFormat.KEY_MAX_B_FRAMES, 0)
            }
            val codec = runCatching { MediaCodec.createEncoderByType(mime) }.getOrNull() ?: return null
            return try {
                codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                val surface = codec.createInputSurface()
                codec.start()
                val muxer = MediaMuxer(out.path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
                muxer.setOrientationHint(((rotation % 360) + 360) % 360)
                TimelapseWriter(out, width, height, fps, codec, surface, muxer, deflicker)
            } catch (e: Exception) {
                runCatching { codec.release() }
                null
            }
        }

        /** Największy z rozmiarów ≤ [maxW]×[maxH], który obsługuje koder (8K → 4K → 1080p). */
        fun fitSize(wantW: Int, wantH: Int, maxW: Int, maxH: Int): Pair<Int, Int>? {
            val sizes = listOf(7680 to 4320, 3840 to 2160, 1920 to 1080, 1280 to 720)
            return sizes.firstOrNull { (w, h) ->
                w <= wantW && h <= wantH && w <= max(maxW, maxH) && h <= min(maxW, maxH) && pickMime(w, h) != null
            }
        }
    }
}
