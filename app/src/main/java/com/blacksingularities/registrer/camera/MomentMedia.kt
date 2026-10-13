package com.blacksingularities.registrer.camera

import com.blacksingularities.registrer.i18n.tr
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Zdjęcia ruchome (Motion Photo – JPEG z wbudowanym klipem MP4, rozpoznawany przez Zdjęcia
 * Google i galerie zgodne z formatem). Źródłem jest nagranie w tle, z którego wycinany jest
 * fragment wokół naciśnięcia migawki.
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
            check(videoTrack >= 0) { tr("Brak ścieżki wideo", "No video track") }
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
                MediaSaver.tagGps(exif)
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
        require(segLength <= 0xFFFF) { tr("XMP za duże", "XMP too large") }
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
}
