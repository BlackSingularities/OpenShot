package com.blacksingularities.registrer.audio

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.media.AudioManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import com.blacksingularities.registrer.data.AudioPreset
import com.blacksingularities.registrer.data.AudioSourceType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AudioItem(
    val id: Long,
    val uri: Uri,
    val name: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val dateAddedSec: Long,
)

/** Dyktafon wysokiej jakości zapisujący AAC/M4A bezpośrednio do MediaStore. */
class AudioEngine(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var descriptor: ParcelFileDescriptor? = null
    private var currentUri: Uri? = null

    val isRecording get() = recorder != null

    private val relativeDir: String
        get() = if (Build.VERSION.SDK_INT >= 31) {
            Environment.DIRECTORY_RECORDINGS + "/Registrer"
        } else {
            Environment.DIRECTORY_MUSIC + "/Registrer"
        }

    private fun sourceFor(type: AudioSourceType): Int = when (type) {
        AudioSourceType.STANDARD -> MediaRecorder.AudioSource.MIC
        AudioSourceType.VOICE -> MediaRecorder.AudioSource.VOICE_COMMUNICATION
        AudioSourceType.CAMCORDER -> MediaRecorder.AudioSource.CAMCORDER
        AudioSourceType.RAW -> {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            if (am.getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED) == "true") {
                MediaRecorder.AudioSource.UNPROCESSED
            } else {
                MediaRecorder.AudioSource.VOICE_RECOGNITION
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun start(preset: AudioPreset, source: AudioSourceType): Boolean {
        if (recorder != null) return false
        val resolver = context.contentResolver
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "REG_AUD_$stamp.m4a")
            put(MediaStore.MediaColumns.MIME_TYPE, "audio/mp4")
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativeDir)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val uri = resolver.insert(collection, values) ?: return false
        val pfd = resolver.openFileDescriptor(uri, "w")
        if (pfd == null) {
            resolver.delete(uri, null, null)
            return false
        }

        fun create(audioSource: Int, channels: Int): MediaRecorder {
            val r = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()
            try {
                r.setAudioSource(audioSource)
                r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                r.setAudioSamplingRate(preset.sampleRate)
                r.setAudioEncodingBitRate(preset.bitRate)
                r.setAudioChannels(channels)
                r.setOutputFile(pfd.fileDescriptor)
                r.prepare()
                r.start()
                return r
            } catch (e: Exception) {
                r.release()
                throw e
            }
        }

        val rec = try {
            create(sourceFor(source), preset.channels)
        } catch (_: Exception) {
            try {
                create(MediaRecorder.AudioSource.MIC, 1)
            } catch (_: Exception) {
                null
            }
        }
        if (rec == null) {
            runCatching { pfd.close() }
            resolver.delete(uri, null, null)
            return false
        }
        recorder = rec
        descriptor = pfd
        currentUri = uri
        return true
    }

    fun amplitude(): Int = runCatching { recorder?.maxAmplitude ?: 0 }.getOrDefault(0)

    fun pause() = runCatching { recorder?.pause() }.isSuccess
    fun resume() = runCatching { recorder?.resume() }.isSuccess

    /** Kończy nagranie i publikuje plik; zwraca jego Uri albo null przy błędzie. */
    fun stop(): Uri? {
        val r = recorder ?: return null
        val uri = currentUri
        val ok = runCatching { r.stop() }.isSuccess
        r.release()
        runCatching { descriptor?.close() }
        recorder = null
        descriptor = null
        currentUri = null
        if (uri == null) return null
        val resolver = context.contentResolver
        return if (ok) {
            resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
            uri
        } else {
            resolver.delete(uri, null, null)
            null
        }
    }

    fun list(): List<AudioItem> {
        val items = mutableListOf<AudioItem>()
        val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_ADDED,
        )
        runCatching {
            context.contentResolver.query(
                collection,
                projection,
                "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?",
                arrayOf("%Registrer%"),
                "${MediaStore.Audio.Media.DATE_ADDED} DESC",
            )?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val nameCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val durCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val sizeCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val dateCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                while (c.moveToNext()) {
                    val id = c.getLong(idCol)
                    items += AudioItem(
                        id = id,
                        uri = ContentUris.withAppendedId(collection, id),
                        name = c.getString(nameCol) ?: "Nagranie",
                        durationMs = c.getLong(durCol),
                        sizeBytes = c.getLong(sizeCol),
                        dateAddedSec = c.getLong(dateCol),
                    )
                }
            }
        }
        return items
    }

    fun delete(item: AudioItem): Boolean =
        runCatching { context.contentResolver.delete(item.uri, null, null) > 0 }.getOrDefault(false)

    fun release() {
        if (recorder != null) stop()
    }
}
