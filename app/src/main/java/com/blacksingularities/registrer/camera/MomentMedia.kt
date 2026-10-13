package com.blacksingularities.registrer.camera

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.os.SystemClock
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Chwile: zdjęcia ruchome (Motion Photo – JPEG z wbudowanym klipem MP4, rozpoznawany przez
 * Zdjęcia Google i galerie zgodne z formatem) oraz klipy HEVC. Źródłem jest nagranie w tle,
 * z którego wycinany jest fragment wokół naciśnięcia migawki.
 */
object MomentMedia {
    private fun stamp(): String = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())

    /**
     * Kopiuje fragment [fromUs, toUs] (od klatki kluczowej ≤ fromUs) do nowego MP4.
     * Zwraca czas (w µs nagrania źródłowego), od którego zaczyna się wynik.
     */
    fun trim(src: File, dst: File, fromUs: Long, toUs: Long): Long {
        val ex = MediaExtractor()
        ex.setDataSource(src.path)
        val muxer = MediaMuxer(dst.path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        try {
            val map = IntArray(ex.trackCount) { -1 }
            var videoTrack = -1
            for (i in 0 until ex.trackCount) {
                val f = ex.getTrackFormat(i)
                val mime = f.getString(MediaFormat.KEY_MIME) ?: continue
                if (!mime.startsWith("video/") && !mime.startsWith("audio/")) continue
                if (mime.startsWith("video/")) {
                    videoTrack = i
                    if (f.containsKey(MediaFormat.KEY_ROTATION)) muxer.setOrientationHint(f.getInteger(MediaFormat.KEY_ROTATION))
                }
                map[i] = muxer.addTrack(f)
                ex.selectTrack(i)
            }
            check(videoTrack >= 0) { "Brak ścieżki wideo" }
            muxer.start()
            ex.seekTo(fromUs.coerceAtLeast(0), MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
            val buffer = ByteBuffer.allocate(8 * 1024 * 1024)
            val info = MediaCodec.BufferInfo()
            var base = -1L
            while (true) {
                val t = ex.sampleTime
                if (t < 0) break
                val track = ex.sampleTrackIndex
                if (track == videoTrack && t > toUs) break
                val size = ex.readSampleData(buffer, 0)
                if (size < 0) break
                if (base < 0) base = t
                if (t >= base && map[track] >= 0) {
                    val flags = if (ex.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0
                    info.set(0, size, t - base, flags)
                    muxer.writeSampleData(map[track], buffer, info)
                }
                ex.advance()
            }
            muxer.stop()
            return base.coerceAtLeast(0)
        } finally {
            runCatching { muxer.release() }
            ex.release()
        }
    }

    fun hevcEncoderAvailable(width: Int, height: Int): Boolean = runCatching {
        MediaCodecList(MediaCodecList.REGULAR_CODECS)
            .findEncoderForFormat(MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_HEVC, width, height)) != null
    }.getOrDefault(false)

    /**
     * Przekodowanie klipu do HEVC sprzętowym koderem: dekoder rysuje bezpośrednio na
     * powierzchnię wejściową kodera, dźwięk jest kopiowany bez zmian. false = brak HEVC / błąd.
     */
    fun transcodeHevc(src: File, dst: File): Boolean {
        val ex = MediaExtractor()
        ex.setDataSource(src.path)
        var videoIndex = -1
        var audioFormat: MediaFormat? = null
        var audioIndex = -1
        for (i in 0 until ex.trackCount) {
            val f = ex.getTrackFormat(i)
            val mime = f.getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("video/") && videoIndex < 0) videoIndex = i
            if (mime.startsWith("audio/") && audioIndex < 0) {
                audioIndex = i
                audioFormat = f
            }
        }
        if (videoIndex < 0) {
            ex.release()
            return false
        }
        val inFormat = ex.getTrackFormat(videoIndex)
        val width = inFormat.getInteger(MediaFormat.KEY_WIDTH)
        val height = inFormat.getInteger(MediaFormat.KEY_HEIGHT)
        if (!hevcEncoderAvailable(width, height)) {
            ex.release()
            return false
        }
        val rotation = if (inFormat.containsKey(MediaFormat.KEY_ROTATION)) inFormat.getInteger(MediaFormat.KEY_ROTATION) else 0
        val fps = if (inFormat.containsKey(MediaFormat.KEY_FRAME_RATE)) inFormat.getInteger(MediaFormat.KEY_FRAME_RATE) else 30

        var encoder: MediaCodec? = null
        var decoder: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var ok = false
        try {
            val outFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_HEVC, width, height).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, (width.toLong() * height * fps / 6).toInt().coerceIn(4_000_000, 40_000_000))
                setInteger(MediaFormat.KEY_FRAME_RATE, fps)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }
            encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_HEVC)
            encoder.configure(outFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            val surface = encoder.createInputSurface()
            encoder.start()
            decoder = MediaCodec.createDecoderByType(inFormat.getString(MediaFormat.KEY_MIME)!!)
            decoder.configure(inFormat, surface, null, 0)
            decoder.start()

            muxer = MediaMuxer(dst.path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            muxer.setOrientationHint(rotation)
            var muxVideo = -1
            var muxAudio = -1
            var started = false

            ex.selectTrack(videoIndex)
            val info = MediaCodec.BufferInfo()
            val encInfo = MediaCodec.BufferInfo()
            var inputDone = false
            var decoderDone = false
            var encoderDone = false
            val deadline = SystemClock.elapsedRealtime() + 30_000
            while (!encoderDone && SystemClock.elapsedRealtime() < deadline) {
                if (!inputDone) {
                    val idx = decoder.dequeueInputBuffer(5_000)
                    if (idx >= 0) {
                        val buf = decoder.getInputBuffer(idx)!!
                        val size = ex.readSampleData(buf, 0)
                        if (size < 0) {
                            decoder.queueInputBuffer(idx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            decoder.queueInputBuffer(idx, 0, size, ex.sampleTime, 0)
                            ex.advance()
                        }
                    }
                }
                if (!decoderDone) {
                    val idx = decoder.dequeueOutputBuffer(info, 5_000)
                    if (idx >= 0) {
                        val render = info.size > 0
                        if (render) decoder.releaseOutputBuffer(idx, info.presentationTimeUs * 1000) else decoder.releaseOutputBuffer(idx, false)
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            encoder.signalEndOfInputStream()
                            decoderDone = true
                        }
                    }
                }
                val out = encoder.dequeueOutputBuffer(encInfo, 5_000)
                when {
                    out == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        muxVideo = muxer.addTrack(encoder.outputFormat)
                        audioFormat?.let { muxAudio = muxer.addTrack(it) }
                        muxer.start()
                        started = true
                    }
                    out >= 0 -> {
                        val buf = encoder.getOutputBuffer(out)!!
                        if (encInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) encInfo.size = 0
                        if (encInfo.size > 0 && started) muxer.writeSampleData(muxVideo, buf, encInfo)
                        encoder.releaseOutputBuffer(out, false)
                        if (encInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) encoderDone = true
                    }
                }
            }
            if (!encoderDone || !started) return false

            // Dźwięk bez zmian.
            if (audioIndex >= 0 && muxAudio >= 0) {
                val ax = MediaExtractor()
                ax.setDataSource(src.path)
                ax.selectTrack(audioIndex)
                val buffer = ByteBuffer.allocate(1024 * 1024)
                val ai = MediaCodec.BufferInfo()
                while (true) {
                    val size = ax.readSampleData(buffer, 0)
                    if (size < 0) break
                    ai.set(0, size, ax.sampleTime, 0)
                    muxer.writeSampleData(muxAudio, buffer, ai)
                    ax.advance()
                }
                ax.release()
            }
            muxer.stop()
            ok = true
            return true
        } catch (e: Exception) {
            return false
        } finally {
            runCatching { decoder?.stop() }
            runCatching { decoder?.release() }
            runCatching { encoder?.stop() }
            runCatching { encoder?.release() }
            runCatching { muxer?.release() }
            ex.release()
            if (!ok) dst.delete()
        }
    }

    /** JPEG z ustawioną orientacją EXIF. */
    fun jpegWithOrientation(context: Context, bytes: ByteArray, rotation: Int): ByteArray {
        val tmp = File(context.cacheDir, "mp_${System.nanoTime()}.jpg")
        try {
            tmp.writeBytes(bytes)
            runCatching {
                val exif = ExifInterface(tmp.absolutePath)
                val orientation = when ((rotation % 360 + 360) % 360) {
                    90 -> ExifInterface.ORIENTATION_ROTATE_90
                    180 -> ExifInterface.ORIENTATION_ROTATE_180
                    270 -> ExifInterface.ORIENTATION_ROTATE_270
                    else -> ExifInterface.ORIENTATION_NORMAL
                }
                exif.setAttribute(ExifInterface.TAG_ORIENTATION, orientation.toString())
                exif.setAttribute(ExifInterface.TAG_SOFTWARE, "OpenShot Motion Photo")
                exif.saveAttributes()
            }
            return tmp.readBytes()
        } finally {
            tmp.delete()
        }
    }

    /** Klatka z nagrania (gdy aparat nie pozwolił na zdjęcie równolegle z nagrywaniem). */
    fun frameAt(src: File, timeUs: Long): ByteArray? {
        val r = MediaMetadataRetriever()
        return try {
            r.setDataSource(src.path)
            val bmp = r.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST) ?: return null
            ByteArrayOutputStream().use { os ->
                bmp.compress(Bitmap.CompressFormat.JPEG, 95, os)
                bmp.recycle()
                os.toByteArray()
            }
        } catch (e: Exception) {
            null
        } finally {
            runCatching { r.release() }
        }
    }

    /**
     * Motion Photo (format Google v1 + zgodność z MicroVideo): metadane XMP w JPEG
     * i plik MP4 dołączony na końcu.
     */
    fun buildMotionPhoto(jpeg: ByteArray, mp4: ByteArray, presentationUs: Long): ByteArray {
        val xmp = """<x:xmpmeta xmlns:x="adobe:ns:meta/" x:xmptk="OpenShot">
<rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
<rdf:Description rdf:about=""
 xmlns:GCamera="http://ns.google.com/photos/1.0/camera/"
 xmlns:Container="http://ns.google.com/photos/1.0/container/"
 xmlns:Item="http://ns.google.com/photos/1.0/container/item/"
 GCamera:MotionPhoto="1"
 GCamera:MotionPhotoVersion="1"
 GCamera:MotionPhotoPresentationTimestampUs="$presentationUs"
 GCamera:MicroVideo="1"
 GCamera:MicroVideoVersion="1"
 GCamera:MicroVideoOffset="${mp4.size}"
 GCamera:MicroVideoPresentationTimestampUs="$presentationUs">
<Container:Directory>
<rdf:Seq>
<rdf:li rdf:parseType="Resource"><Container:Item Item:Mime="image/jpeg" Item:Semantic="Primary" Item:Length="0" Item:Padding="0"/></rdf:li>
<rdf:li rdf:parseType="Resource"><Container:Item Item:Mime="video/mp4" Item:Semantic="MotionPhoto" Item:Length="${mp4.size}" Item:Padding="0"/></rdf:li>
</rdf:Seq>
</Container:Directory>
</rdf:Description>
</rdf:RDF>
</x:xmpmeta>""".toByteArray(Charsets.UTF_8)
        val header = "http://ns.adobe.com/xap/1.0/".toByteArray(Charsets.US_ASCII) + byteArrayOf(0)
        val segLength = 2 + header.size + xmp.size
        require(segLength <= 0xFFFF) { "XMP za duże" }
        // XMP za segmentem EXIF (APP1), jeśli jest – inaczej tuż po SOI.
        var insertAt = 2
        if (jpeg.size > 6 && (jpeg[2].toInt() and 0xFF) == 0xFF && (jpeg[3].toInt() and 0xFF) == 0xE1) {
            val len = ((jpeg[4].toInt() and 0xFF) shl 8) or (jpeg[5].toInt() and 0xFF)
            val isExif = jpeg.size > 10 && String(jpeg, 6, 4, Charsets.US_ASCII) == "Exif"
            if (isExif) insertAt = 4 + len
        } else if (jpeg.size > 6 && (jpeg[2].toInt() and 0xFF) == 0xFF && (jpeg[3].toInt() and 0xFF) == 0xE0) {
            val len = ((jpeg[4].toInt() and 0xFF) shl 8) or (jpeg[5].toInt() and 0xFF)
            insertAt = 4 + len
        }
        val out = ByteArrayOutputStream(jpeg.size + segLength + 2 + mp4.size)
        out.write(jpeg, 0, insertAt)
        out.write(0xFF)
        out.write(0xE1)
        out.write((segLength shr 8) and 0xFF)
        out.write(segLength and 0xFF)
        out.write(header)
        out.write(xmp)
        out.write(jpeg, insertAt, jpeg.size - insertAt)
        out.write(mp4)
        return out.toByteArray()
    }

    fun saveImage(context: Context, bytes: ByteArray): Uri? {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "MVIMG_${stamp()}.jpg")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, MediaSaver.MEDIA_DIR)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), values) ?: return null
        return try {
            resolver.openOutputStream(uri)!!.use { it.write(bytes) }
            resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
            uri
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            null
        }
    }

    fun saveVideo(context: Context, file: File, hevc: Boolean): Uri? {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "OPENSHOT_CLIP${if (hevc) "_HEVC" else ""}_${stamp()}.mp4")
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            put(MediaStore.MediaColumns.RELATIVE_PATH, MediaSaver.MEDIA_DIR)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), values) ?: return null
        return try {
            resolver.openOutputStream(uri)!!.use { os -> file.inputStream().use { it.copyTo(os) } }
            resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
            uri
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            null
        }
    }
}
