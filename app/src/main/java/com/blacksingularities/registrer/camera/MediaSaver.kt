package com.blacksingularities.registrer.camera

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import androidx.camera.core.ImageCapture
import androidx.camera.video.MediaStoreOutputOptions
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Zapis zdjęć i filmów do galerii (DCIM/Registrer) przez MediaStore. */
object MediaSaver {
    const val MEDIA_DIR = "DCIM/Registrer"

    private fun stamp(): String = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())

    private fun photoValues(tag: String, mime: String = "image/jpeg") = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, "REG_${tag}_${stamp()}")
        put(MediaStore.MediaColumns.MIME_TYPE, mime)
        put(MediaStore.MediaColumns.RELATIVE_PATH, MEDIA_DIR)
    }

    fun photoOutputOptions(context: Context, mirror: Boolean, tag: String, mime: String): ImageCapture.OutputFileOptions =
        ImageCapture.OutputFileOptions.Builder(
            context.contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            photoValues(tag, mime),
        ).setMetadata(ImageCapture.Metadata().apply { isReversedHorizontal = mirror }).build()

    /** Surowy obraz z sensora zapisywany jako DNG. */
    fun rawOutputOptions(context: Context, tag: String): ImageCapture.OutputFileOptions =
        ImageCapture.OutputFileOptions.Builder(
            context.contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            photoValues(tag, "image/x-adobe-dng"),
        ).build()

    fun videoOutputOptions(context: Context): MediaStoreOutputOptions {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "REG_VID_${stamp()}")
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            put(MediaStore.MediaColumns.RELATIVE_PATH, MEDIA_DIR)
        }
        return MediaStoreOutputOptions.Builder(context.contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(values)
            .build()
    }

    /** Zapisuje przetworzoną bitmapę (w orientacji sensora) z poprawnym tagiem EXIF orientacji. */
    fun saveBitmap(context: Context, bitmap: Bitmap, rotation: Int, quality: Int, software: String, tag: String): Uri? {
        val tmp = File(context.cacheDir, "proc_${System.nanoTime()}.jpg")
        try {
            FileOutputStream(tmp).use { bitmap.compress(Bitmap.CompressFormat.JPEG, quality, it) }
            writeExif(tmp, rotation, software)
            return insertFile(context, tmp, tag)
        } finally {
            tmp.delete()
        }
    }

    /** Zapisuje surową klatkę JPEG (awaryjnie, gdy przetwarzanie się nie powiedzie). */
    fun saveJpeg(context: Context, bytes: ByteArray, rotation: Int, software: String, tag: String): Uri? {
        val tmp = File(context.cacheDir, "raw_${System.nanoTime()}.jpg")
        try {
            tmp.writeBytes(bytes)
            writeExif(tmp, rotation, software)
            return insertFile(context, tmp, tag)
        } finally {
            tmp.delete()
        }
    }

    private fun writeExif(file: File, rotation: Int, software: String) {
        runCatching {
            val exif = ExifInterface(file.absolutePath)
            val orientation = when ((rotation % 360 + 360) % 360) {
                90 -> ExifInterface.ORIENTATION_ROTATE_90
                180 -> ExifInterface.ORIENTATION_ROTATE_180
                270 -> ExifInterface.ORIENTATION_ROTATE_270
                else -> ExifInterface.ORIENTATION_NORMAL
            }
            exif.setAttribute(ExifInterface.TAG_ORIENTATION, orientation.toString())
            exif.setAttribute(ExifInterface.TAG_SOFTWARE, software)
            val now = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).format(Date())
            exif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, now)
            exif.setAttribute(ExifInterface.TAG_DATETIME, now)
            exif.saveAttributes()
        }
    }

    private fun insertFile(context: Context, file: File, tag: String): Uri? {
        val resolver = context.contentResolver
        val values = photoValues(tag).apply { put(MediaStore.MediaColumns.IS_PENDING, 1) }
        val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val uri = resolver.insert(collection, values) ?: return null
        try {
            val out = resolver.openOutputStream(uri) ?: error("Brak dostępu do pliku")
            out.use { os -> file.inputStream().use { it.copyTo(os) } }
            resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
            return uri
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }
    }
}
