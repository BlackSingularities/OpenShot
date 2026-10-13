package com.blacksingularities.registrer.audio

import com.blacksingularities.registrer.i18n.tr
import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.MediaRecorder
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.os.Process
import android.os.SystemClock
import android.provider.MediaStore
import com.blacksingularities.registrer.data.AudioInput
import com.blacksingularities.registrer.data.AudioPreset
import com.blacksingularities.registrer.data.AudioSourceType
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileDescriptor
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.math.tanh

data class AudioItem(
    val id: Long,
    val uri: Uri,
    val name: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val dateAddedSec: Long,
    val mime: String,
    /** Znaczniki dodane podczas nagrywania (ms od początku pliku). */
    val markers: List<Long> = emptyList(),
    /** Zarys głośności 0–1 zapisany przy nagrywaniu; pusty dla starszych nagrań. */
    val peaks: List<Float> = emptyList(),
) {
    val title: String get() = name.substringBeforeLast('.')
    val format: String
        get() = when {
            mime.contains("wav") -> "WAV"
            mime.contains("mp4") || mime.contains("m4a") || mime.contains("aac") -> "M4A"
            else -> name.substringAfterLast('.', "").uppercase(Locale.ROOT)
        }
}

enum class RecorderStatus { IDLE, RECORDING, PAUSED, FINISHING }

/**
 * Dyktafon na AudioRecord: własne kodowanie AAC (MediaCodec + MediaMuxer) albo bezstratny WAV,
 * cyfrowe wzmocnienie z miękkim ogranicznikiem, pomiar szczytów i przesterowań, pomijanie ciszy,
 * znaczniki i zarys fali zapisywane obok nagrania. Jedna instancja na proces – nagrywanie trwa
 * w usłudze pierwszoplanowej także wtedy, gdy ekran aparatu został zamknięty.
 */
class AudioEngine private constructor(private val context: Context) {

    data class Config(
        val preset: AudioPreset,
        val source: AudioSourceType,
        val input: AudioInput,
        val gainDb: Int,
        val noiseSuppression: Boolean,
        val agc: Boolean,
        val skipSilence: Boolean,
        /** 0 = bez limitu. */
        val maxDurationMs: Long,
    )

    data class Finished(val uri: Uri?, val durationMs: Long, val autoStopped: Boolean, val error: String?)

    interface Listener {
        fun onStateChanged() {}
        fun onFinished(result: Finished) {}
    }

    private val main = Handler(Looper.getMainLooper())
    private val listeners = CopyOnWriteArrayList<Listener>()

    @Volatile var status = RecorderStatus.IDLE
        private set
    @Volatile private var paused = false
    @Volatile private var stopRequested = false
    @Volatile private var framesWritten = 0L
    @Volatile private var sampleRate = 48_000

    /** Szczyt lewego / prawego kanału ostatnich 20 ms (0–1, po wzmocnieniu). */
    @Volatile var peakL = 0f
        private set
    @Volatile var peakR = 0f
        private set
    /** Chwila ostatniego przesterowania wejścia (elapsedRealtime). */
    @Volatile var lastClipAt = 0L
        private set
    /** Trwa pomijana cisza. */
    @Volatile var silent = false
        private set
    /** Łączny czas pominiętej ciszy. */
    @Volatile var skippedMs = 0L
        private set
    @Volatile var deviceName: String? = null
        private set
    @Volatile var formatLabel: String = ""
        private set

    private val markers = CopyOnWriteArrayList<Long>()
    private var thread: Thread? = null

    val recordedMs: Long get() = framesWritten * 1000 / sampleRate.coerceAtLeast(1)
    val markerCount: Int get() = markers.size
    val isActive: Boolean get() = status != RecorderStatus.IDLE

    fun addListener(l: Listener) {
        listeners.addIfAbsent(l)
    }

    fun removeListener(l: Listener) {
        listeners.remove(l)
    }

    private fun notifyState() = main.post { listeners.forEach { it.onStateChanged() } }

    private val relativeDir: String
        get() = if (Build.VERSION.SDK_INT >= 31) {
            Environment.DIRECTORY_RECORDINGS + "/OpenShot"
        } else {
            Environment.DIRECTORY_MUSIC + "/OpenShot"
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

    private fun inputDevices(): List<AudioDeviceInfo> = runCatching {
        (context.getSystemService(Context.AUDIO_SERVICE) as AudioManager).getDevices(AudioManager.GET_DEVICES_INPUTS).toList()
    }.getOrDefault(emptyList())

    /** Podłączony mikrofon zewnętrzny (przewodowy, USB, Bluetooth LE) albo null. */
    fun externalInput(): AudioDeviceInfo? = inputDevices().firstOrNull { it.type in EXTERNAL_TYPES }

    fun externalInputName(): String? = externalInput()?.let { describe(it) }

    private fun preferredDevice(input: AudioInput): AudioDeviceInfo? = when (input) {
        AudioInput.AUTO -> null
        AudioInput.BUILTIN -> inputDevices().firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_MIC }
        AudioInput.EXTERNAL -> externalInput()
    }

    @SuppressLint("MissingPermission")
    private fun createRecord(source: Int, channels: Int, rate: Int): AudioRecord? {
        val mask = if (channels == 2) AudioFormat.CHANNEL_IN_STEREO else AudioFormat.CHANNEL_IN_MONO
        val min = AudioRecord.getMinBufferSize(rate, mask, AudioFormat.ENCODING_PCM_16BIT)
        if (min <= 0) return null
        return try {
            val r = AudioRecord.Builder()
                .setAudioSource(source)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(rate)
                        .setChannelMask(mask)
                        .build(),
                )
                .setBufferSizeInBytes(max(min * 4, rate * channels * 2 / 2))
                .build()
            if (r.state == AudioRecord.STATE_INITIALIZED) r else {
                r.release()
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /** Rozpoczyna nagrywanie; zwraca null przy powodzeniu albo opis błędu. */
    fun start(cfg: Config): String? {
        if (status != RecorderStatus.IDLE) return tr("Nagrywanie już trwa", "Recording is already running")
        val preset = cfg.preset
        val source = sourceFor(cfg.source)
        val attempts = listOf(
            Triple(source, preset.channels, preset.sampleRate),
            Triple(source, 1, preset.sampleRate),
            Triple(MediaRecorder.AudioSource.MIC, preset.channels, preset.sampleRate),
            Triple(MediaRecorder.AudioSource.MIC, 1, 44_100),
        ).distinct()
        var record: AudioRecord? = null
        for ((s, ch, rate) in attempts) {
            record = createRecord(s, ch, rate)
            if (record != null) break
        }
        val rec = record ?: return tr("mikrofon jest zajęty albo niedostępny", "the microphone is busy or unavailable")
        val rate = rec.sampleRate
        val channels = rec.channelCount

        preferredDevice(cfg.input)?.let { runCatching { rec.setPreferredDevice(it) } }

        val resolver = context.contentResolver
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val ext = if (preset.lossless) "wav" else "m4a"
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "OPENSHOT_AUD_$stamp.$ext")
            put(MediaStore.MediaColumns.MIME_TYPE, if (preset.lossless) MIME_WAV else MIME_M4A)
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativeDir)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val uri = runCatching { resolver.insert(collection, values) }.getOrNull()
        if (uri == null) {
            rec.release()
            return tr("nie można utworzyć pliku", "cannot create the file")
        }
        val pfd = runCatching { resolver.openFileDescriptor(uri, "rw") }.getOrNull()
        if (pfd == null) {
            rec.release()
            runCatching { resolver.delete(uri, null, null) }
            return tr("nie można otworzyć pliku", "cannot open the file")
        }
        val sink: Sink = try {
            if (preset.lossless) WavSink(pfd.fileDescriptor, rate, channels) else AacSink.create(pfd.fileDescriptor, rate, channels, preset.bitRate)
        } catch (e: Exception) {
            rec.release()
            runCatching { pfd.close() }
            runCatching { resolver.delete(uri, null, null) }
            return tr("koder dźwięku niedostępny (${e.message})", "audio encoder unavailable (${e.message})")
        }

        val effects = mutableListOf<android.media.audiofx.AudioEffect>()
        if (cfg.noiseSuppression && NoiseSuppressor.isAvailable()) {
            runCatching { NoiseSuppressor.create(rec.audioSessionId)?.also { it.setEnabled(true); effects += it } }
        }
        if (cfg.agc && AutomaticGainControl.isAvailable()) {
            runCatching { AutomaticGainControl.create(rec.audioSessionId)?.also { it.setEnabled(true); effects += it } }
        }

        try {
            rec.startRecording()
        } catch (e: Exception) {
            // obsłużone niżej
        }
        if (rec.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
            effects.forEach { runCatching { it.release() } }
            rec.release()
            runCatching { sink.close() }
            runCatching { pfd.close() }
            runCatching { resolver.delete(uri, null, null) }
            return tr("mikrofon jest używany przez inną aplikację", "the microphone is used by another app")
        }

        sampleRate = rate
        framesWritten = 0
        skippedMs = 0
        peakL = 0f
        peakR = 0f
        lastClipAt = 0L
        silent = false
        paused = false
        stopRequested = false
        markers.clear()
        val chLabel = if (channels == 2) "stereo" else "mono"
        val khz = String.format(com.blacksingularities.registrer.i18n.Lang.locale, "%.1f", rate / 1000f).removeSuffix(",0").removeSuffix(".0")
        formatLabel = if (preset.lossless) "WAV 16-bit · $chLabel · $khz kHz" else "AAC ${preset.bitRate / 1000} kb/s · $chLabel · $khz kHz"
        deviceName = (runCatching { rec.routedDevice }.getOrNull() ?: preferredDevice(cfg.input))?.let { describe(it) }
        status = RecorderStatus.RECORDING

        val t = Thread({ loop(rec, sink, pfd, uri, effects, cfg, rate, channels) }, "OpenShot-audio")
        thread = t
        t.start()
        notifyState()
        // Urządzenie bywa znane dopiero po pierwszych próbkach.
        main.postDelayed({
            if (status != RecorderStatus.IDLE) {
                runCatching { rec.routedDevice }.getOrNull()?.let { deviceName = describe(it); notifyState() }
            }
        }, 600)
        return null
    }

    fun pause(): Boolean {
        if (status != RecorderStatus.RECORDING) return false
        paused = true
        status = RecorderStatus.PAUSED
        notifyState()
        return true
    }

    fun resume(): Boolean {
        if (status != RecorderStatus.PAUSED) return false
        paused = false
        status = RecorderStatus.RECORDING
        notifyState()
        return true
    }

    /** Kończy nagranie asynchronicznie – wynik trafia do [Listener.onFinished]. */
    fun stop() {
        if (status == RecorderStatus.IDLE || status == RecorderStatus.FINISHING) return
        stopRequested = true
        status = RecorderStatus.FINISHING
        notifyState()
    }

    /** Znacznik w bieżącym miejscu nagrania; zwraca jego czas albo null. */
    fun addMarker(): Long? {
        if (status != RecorderStatus.RECORDING && status != RecorderStatus.PAUSED) return null
        val at = recordedMs
        if (markers.lastOrNull()?.let { at - it < 500 } == true) return null
        markers += at
        notifyState()
        return at
    }

    private fun loop(
        rec: AudioRecord,
        sink: Sink,
        pfd: ParcelFileDescriptor,
        uri: Uri,
        effects: List<android.media.audiofx.AudioEffect>,
        cfg: Config,
        rate: Int,
        ch: Int,
    ) {
        runCatching { Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO) }
        val chunk = ShortArray(rate / 50 * ch)
        val gain = 10.0.pow(cfg.gainDb / 20.0).toFloat()
        var maxFrames = if (cfg.maxDurationMs > 0) cfg.maxDurationMs * rate / 1000 else Long.MAX_VALUE
        if (sink is WavSink) maxFrames = min(maxFrames, WavSink.MAX_DATA_BYTES / (2L * ch))
        val preroll = ArrayDeque<ShortArray>()
        val quietLimit = rate * 3L / 2
        var quietFrames = 0L
        val bucketFrames = rate / 10
        var bucketPeak = 0f
        var bucketCount = 0
        val peaks = ByteArrayOutputStream()
        var autoStopped = false
        var error: String? = null

        fun write(data: ShortArray, n: Int) {
            sink.write(data, n)
            framesWritten += n / ch
            for (i in 0 until n) {
                val a = abs(data[i].toInt()) / 32768f
                if (a > bucketPeak) bucketPeak = a
            }
            bucketCount += n / ch
            if (bucketCount >= bucketFrames) {
                peaks.write((bucketPeak * 255f).roundToInt().coerceIn(0, 255))
                bucketPeak = 0f
                bucketCount = 0
            }
        }

        try {
            while (!stopRequested) {
                val n = rec.read(chunk, 0, chunk.size)
                if (n < 0) {
                    error = tr("błąd odczytu mikrofonu ($n)", "microphone read error ($n)")
                    break
                }
                if (n == 0) continue
                var pl = 0f
                var pr = 0f
                var sum = 0.0
                var clipped = false
                for (i in 0 until n) {
                    var x = chunk[i] / 32768f
                    if (gain != 1f) x *= gain
                    val a = abs(x)
                    if (a >= 0.995f) clipped = true
                    if (a > LIMIT_KNEE) {
                        val limited = LIMIT_KNEE + (1f - LIMIT_KNEE) * tanh((a - LIMIT_KNEE) / (1f - LIMIT_KNEE))
                        x = if (x < 0) -limited else limited
                    }
                    chunk[i] = (x * 32767f).roundToInt().coerceIn(-32768, 32767).toShort()
                    val y = abs(x)
                    if (ch == 2 && i % 2 == 1) {
                        if (y > pr) pr = y
                    } else if (y > pl) {
                        pl = y
                    }
                    sum += (x * x).toDouble()
                }
                peakL = pl
                peakR = if (ch == 2) pr else pl
                if (clipped) lastClipAt = SystemClock.elapsedRealtime()
                if (paused) continue

                val frames = n / ch
                if (cfg.skipSilence) {
                    val db = 20f * log10(sqrt(sum / n).toFloat().coerceAtLeast(1e-6f))
                    quietFrames = if (db < SILENCE_DB) quietFrames + frames else 0
                    val nowSilent = if (silent) db < RESUME_DB else quietFrames > quietLimit
                    if (nowSilent) {
                        if (!silent) {
                            silent = true
                            notifyState()
                        }
                        preroll.addLast(chunk.copyOf(n))
                        if (preroll.size > PREROLL_CHUNKS) {
                            preroll.removeFirst()
                            skippedMs += frames * 1000L / rate
                        }
                        continue
                    }
                    if (silent) {
                        silent = false
                        quietFrames = 0
                        while (preroll.isNotEmpty()) {
                            val p = preroll.removeFirst()
                            write(p, p.size)
                        }
                        notifyState()
                    }
                }
                write(chunk, n)
                if (framesWritten >= maxFrames) {
                    autoStopped = true
                    break
                }
            }
        } catch (e: Exception) {
            error = e.message ?: e.javaClass.simpleName
        }

        status = RecorderStatus.FINISHING
        notifyState()
        runCatching { rec.stop() }
        rec.release()
        effects.forEach { runCatching { it.release() } }
        val closed = runCatching { sink.close() }.getOrDefault(false)
        runCatching { pfd.close() }
        val duration = recordedMs
        val resolver = context.contentResolver
        val ok = closed && duration >= 300
        val published = if (ok) {
            runCatching {
                resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
            }.isSuccess
        } else {
            false
        }
        if (published) {
            val raw = peaks.toByteArray()
            runCatching { AudioMeta.save(context, ContentUris.parseId(uri), markers.toList(), downsample(raw, 160)) }
        } else {
            runCatching { resolver.delete(uri, null, null) }
        }
        val result = Finished(
            uri = if (published) uri else null,
            durationMs = duration,
            autoStopped = autoStopped,
            error = error ?: if (!closed && duration >= 300) tr("nie udało się zamknąć pliku", "could not close the file") else null,
        )
        paused = false
        silent = false
        peakL = 0f
        peakR = 0f
        thread = null
        status = RecorderStatus.IDLE
        main.post { listeners.forEach { it.onFinished(result) } }
    }

    // ------------------------------------------------------------ biblioteka

    fun list(): List<AudioItem> {
        val items = mutableListOf<AudioItem>()
        val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.MIME_TYPE,
        )
        runCatching {
            context.contentResolver.query(
                collection,
                projection,
                "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ? OR ${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?",
                // Zachowaj widoczność nagrań wykonanych przed zmianą nazwy aplikacji.
                arrayOf("%OpenShot%", "%Registrer%"),
                "${MediaStore.Audio.Media.DATE_ADDED} DESC",
            )?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val nameCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val durCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val sizeCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val dateCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val mimeCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
                while (c.moveToNext()) {
                    val id = c.getLong(idCol)
                    val meta = AudioMeta.load(context, id)
                    items += AudioItem(
                        id = id,
                        uri = ContentUris.withAppendedId(collection, id),
                        name = c.getString(nameCol) ?: tr("Nagranie", "Recording"),
                        durationMs = c.getLong(durCol),
                        sizeBytes = c.getLong(sizeCol),
                        dateAddedSec = c.getLong(dateCol),
                        mime = c.getString(mimeCol) ?: "",
                        markers = meta.markers,
                        peaks = meta.peaks,
                    )
                }
            }
        }
        return items
    }

    fun delete(item: AudioItem): Boolean {
        val ok = runCatching { context.contentResolver.delete(item.uri, null, null) > 0 }.getOrDefault(false)
        if (ok) AudioMeta.delete(context, item.id)
        return ok
    }

    /** Zmienia nazwę pliku (rozszerzenie zostaje). */
    fun rename(item: AudioItem, title: String): Boolean {
        val clean = title.trim().replace(Regex("[\\\\/:*?\"<>|]"), "_").take(80)
        if (clean.isEmpty()) return false
        val ext = item.name.substringAfterLast('.', if (item.format == "WAV") "wav" else "m4a")
        return runCatching {
            context.contentResolver.update(
                item.uri,
                ContentValues().apply { put(MediaStore.MediaColumns.DISPLAY_NAME, "$clean.$ext") },
                null,
                null,
            ) > 0
        }.getOrDefault(false)
    }

    companion object {
        @Volatile private var instance: AudioEngine? = null

        fun get(context: Context): AudioEngine = instance ?: synchronized(this) {
            instance ?: AudioEngine(context.applicationContext).also { instance = it }
        }

        const val MIME_WAV = "audio/x-wav"
        const val MIME_M4A = "audio/mp4"

        private const val LIMIT_KNEE = 0.8f
        private const val SILENCE_DB = -50f
        private const val RESUME_DB = -44f
        private const val PREROLL_CHUNKS = 15 // 300 ms przed powrotem dźwięku

        val noiseSuppressionAvailable: Boolean get() = runCatching { NoiseSuppressor.isAvailable() }.getOrDefault(false)
        val agcAvailable: Boolean get() = runCatching { AutomaticGainControl.isAvailable() }.getOrDefault(false)

        private val EXTERNAL_TYPES = setOf(
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_USB_DEVICE,
            AudioDeviceInfo.TYPE_USB_HEADSET,
            AudioDeviceInfo.TYPE_USB_ACCESSORY,
            AudioDeviceInfo.TYPE_LINE_ANALOG,
            AudioDeviceInfo.TYPE_LINE_DIGITAL,
            AudioDeviceInfo.TYPE_BLE_HEADSET,
        )

        fun describe(d: AudioDeviceInfo): String {
            val name = d.productName?.toString()?.takeIf { it.isNotBlank() && it != Build.MODEL }
            return when (d.type) {
                AudioDeviceInfo.TYPE_BUILTIN_MIC -> tr("Mikrofon telefonu", "Phone microphone")
                AudioDeviceInfo.TYPE_WIRED_HEADSET -> tr("Mikrofon przewodowy", "Wired microphone")
                AudioDeviceInfo.TYPE_USB_DEVICE, AudioDeviceInfo.TYPE_USB_HEADSET, AudioDeviceInfo.TYPE_USB_ACCESSORY ->
                    "USB" + (name?.let { ": $it" } ?: "")
                AudioDeviceInfo.TYPE_BLE_HEADSET, AudioDeviceInfo.TYPE_BLUETOOTH_SCO ->
                    "Bluetooth" + (name?.let { ": $it" } ?: "")
                AudioDeviceInfo.TYPE_LINE_ANALOG, AudioDeviceInfo.TYPE_LINE_DIGITAL -> tr("Wejście liniowe", "Line input")
                else -> name ?: tr("Mikrofon", "Microphone")
            }
        }

        /** Maksima w [n] przedziałach, przeskalowane do 0–1 względem najgłośniejszego fragmentu. */
        fun downsample(raw: ByteArray, n: Int): List<Float> {
            if (raw.isEmpty()) return emptyList()
            val values = if (raw.size <= n) {
                raw.map { (it.toInt() and 0xFF) / 255f }
            } else {
                List(n) { i ->
                    val from = (i.toLong() * raw.size / n).toInt()
                    val to = ((i + 1L) * raw.size / n).toInt().coerceAtLeast(from + 1)
                    var m = 0
                    for (j in from until to) m = max(m, raw[j].toInt() and 0xFF)
                    m / 255f
                }
            }
            val top = values.maxOrNull()?.takeIf { it > 0.01f } ?: return values
            return values.map { (it / top).coerceIn(0f, 1f) }
        }
    }
}

/** Ujście próbek PCM 16-bit (przeplatane kanały). */
private interface Sink {
    fun write(pcm: ShortArray, n: Int)

    /** Zamyka plik; false = plik nie nadaje się do zapisania. */
    fun close(): Boolean
}

/** Bezstratny WAV: nagłówek uzupełniany przy zamknięciu. */
private class WavSink(fd: FileDescriptor, private val rate: Int, private val ch: Int) : Sink {
    private val out = FileOutputStream(fd)
    private val channel = out.channel
    private var buffer = ByteBuffer.allocate(16 * 1024).order(ByteOrder.LITTLE_ENDIAN)
    private var dataBytes = 0L

    init {
        val h = header(0)
        while (h.hasRemaining()) channel.write(h)
    }

    override fun write(pcm: ShortArray, n: Int) {
        if (n * 2 > buffer.capacity()) buffer = ByteBuffer.allocate(n * 2).order(ByteOrder.LITTLE_ENDIAN)
        buffer.clear()
        buffer.asShortBuffer().put(pcm, 0, n)
        buffer.position(0)
        buffer.limit(n * 2)
        while (buffer.hasRemaining()) channel.write(buffer)
        dataBytes += n * 2L
    }

    override fun close(): Boolean = try {
        channel.position(0)
        val h = header(dataBytes)
        while (h.hasRemaining()) channel.write(h)
        dataBytes > 0
    } catch (e: Exception) {
        false
    } finally {
        runCatching { out.close() }
    }

    private fun header(data: Long): ByteBuffer {
        val d = data.coerceAtMost(MAX_DATA_BYTES)
        return ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray(Charsets.US_ASCII))
            putInt((36 + d).toInt())
            put("WAVE".toByteArray(Charsets.US_ASCII))
            put("fmt ".toByteArray(Charsets.US_ASCII))
            putInt(16)
            putShort(1)
            putShort(ch.toShort())
            putInt(rate)
            putInt(rate * ch * 2)
            putShort((ch * 2).toShort())
            putShort(16)
            put("data".toByteArray(Charsets.US_ASCII))
            putInt(d.toInt())
            flip()
        }
    }

    companion object {
        /** Granica formatu RIFF (4 GB) z zapasem na nagłówek. */
        const val MAX_DATA_BYTES = 0xFFFF_FFFFL - 64
    }
}

/** AAC-LC w kontenerze MP4 (M4A); znaczniki czasu liczone z liczby próbek, więc pauza nie zostawia dziury. */
private class AacSink private constructor(
    private val codec: MediaCodec,
    private val muxer: MediaMuxer,
    private val rate: Int,
    private val ch: Int,
) : Sink {
    private val info = MediaCodec.BufferInfo()
    private var track = -1
    private var started = false
    private var frames = 0L
    private var samples = 0

    override fun write(pcm: ShortArray, n: Int) {
        var off = 0
        while (off < n) {
            val idx = codec.dequeueInputBuffer(10_000)
            if (idx < 0) {
                drain(false)
                continue
            }
            val buf = codec.getInputBuffer(idx) ?: error("Brak bufora kodera")
            buf.clear()
            val room = buf.capacity() / 2 / ch * ch
            check(room > 0) { "Za mały bufor kodera" }
            val count = min(n - off, room)
            buf.order(ByteOrder.nativeOrder()).asShortBuffer().put(pcm, off, count)
            codec.queueInputBuffer(idx, 0, count * 2, frames * 1_000_000L / rate, 0)
            frames += count / ch
            off += count
            drain(false)
        }
    }

    private fun drain(endOfStream: Boolean) {
        var idle = 0
        while (true) {
            val idx = codec.dequeueOutputBuffer(info, if (endOfStream) 10_000 else 0)
            when {
                idx == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                    if (!endOfStream || ++idle > 300) return
                }
                idx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    if (!started) {
                        track = muxer.addTrack(codec.outputFormat)
                        muxer.start()
                        started = true
                    }
                }
                idx >= 0 -> {
                    val out = codec.getOutputBuffer(idx)
                    if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) info.size = 0
                    if (out != null && info.size > 0 && started) {
                        out.position(info.offset)
                        out.limit(info.offset + info.size)
                        muxer.writeSampleData(track, out, info)
                        samples++
                    }
                    codec.releaseOutputBuffer(idx, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) return
                }
            }
        }
    }

    override fun close(): Boolean {
        var ok = false
        try {
            val idx = codec.dequeueInputBuffer(100_000)
            if (idx >= 0) codec.queueInputBuffer(idx, 0, 0, frames * 1_000_000L / rate, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            drain(true)
            if (started && samples > 0) {
                muxer.stop()
                ok = true
            }
        } catch (e: Exception) {
            ok = false
        }
        runCatching { codec.stop() }
        runCatching { codec.release() }
        runCatching { muxer.release() }
        return ok
    }

    companion object {
        fun create(fd: FileDescriptor, rate: Int, ch: Int, bitRate: Int): AacSink {
            val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, rate, ch).apply {
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16 * 1024 * ch)
            }
            val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
            try {
                codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                codec.start()
            } catch (e: Exception) {
                codec.release()
                throw e
            }
            val muxer = try {
                MediaMuxer(fd, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            } catch (e: Exception) {
                runCatching { codec.stop() }
                codec.release()
                throw e
            }
            return AacSink(codec, muxer, rate, ch)
        }
    }
}

/** Znaczniki i zarys fali nagrania – mały plik JSON w pamięci aplikacji, kluczem jest id MediaStore. */
object AudioMeta {
    data class Meta(val markers: List<Long>, val peaks: List<Float>)

    private fun file(context: Context, id: Long) = File(File(context.filesDir, "audio_meta").apply { mkdirs() }, "$id.json")

    fun save(context: Context, id: Long, markers: List<Long>, peaks: List<Float>) {
        val json = org.json.JSONObject()
        json.put("markers", org.json.JSONArray(markers))
        json.put("peaks", org.json.JSONArray(peaks.map { (it * 100).roundToInt() }))
        file(context, id).writeText(json.toString())
    }

    fun load(context: Context, id: Long): Meta {
        val f = file(context, id)
        if (!f.exists()) return Meta(emptyList(), emptyList())
        return runCatching {
            val json = org.json.JSONObject(f.readText())
            val m = json.optJSONArray("markers")
            val p = json.optJSONArray("peaks")
            Meta(
                markers = if (m == null) emptyList() else List(m.length()) { m.getLong(it) },
                peaks = if (p == null) emptyList() else List(p.length()) { p.getInt(it) / 100f },
            )
        }.getOrDefault(Meta(emptyList(), emptyList()))
    }

    fun delete(context: Context, id: Long) {
        runCatching { file(context, id).delete() }
    }
}
