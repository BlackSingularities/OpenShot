package com.blacksingularities.registrer.ui

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaActionSound
import android.media.MediaPlayer
import android.net.Uri
import android.os.SystemClock
import android.util.Size
import android.view.Surface
import androidx.camera.core.CameraSelector
import androidx.camera.core.MeteringPoint
import androidx.camera.core.Preview
import androidx.camera.video.VideoRecordEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import com.blacksingularities.registrer.audio.AudioEngine
import com.blacksingularities.registrer.audio.AudioItem
import com.blacksingularities.registrer.camera.BindRequest
import com.blacksingularities.registrer.camera.CameraCaps
import com.blacksingularities.registrer.camera.CameraEngine
import com.blacksingularities.registrer.camera.CaptureMode
import com.blacksingularities.registrer.camera.CapturedFrame
import com.blacksingularities.registrer.camera.FlashSetting
import com.blacksingularities.registrer.camera.Hud
import com.blacksingularities.registrer.camera.MediaSaver
import com.blacksingularities.registrer.camera.ProSettings
import com.blacksingularities.registrer.computational.ExposureFusion
import com.blacksingularities.registrer.computational.NightStacker
import com.blacksingularities.registrer.data.AppSettings
import com.blacksingularities.registrer.data.AspectRatioMode
import com.blacksingularities.registrer.data.SettingsStore
import com.blacksingularities.registrer.data.VideoResolution
import com.blacksingularities.registrer.data.VolumeAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.log10

enum class RecState { IDLE, RECORDING, PAUSED }

data class Processing(val label: String, val progress: Float?)

/** Klucz konfiguracji – jego zmiana oznacza konieczność ponownego związania kamery. */
data class BindKey(
    val mode: CaptureMode,
    val lens: Int,
    val aspect: AspectRatioMode,
    val video: VideoResolution,
    val hdr: Boolean,
    val stabilization: Boolean,
    val histogram: Boolean,
    val maxQuality: Boolean,
    val jpeg: Int,
)

class CameraViewModel(app: Application) : AndroidViewModel(app) {
    private val ctx get() = getApplication<Application>()
    private val store = SettingsStore(app)
    val engine = CameraEngine(app)
    private val audio = AudioEngine(app)
    private val sound = MediaActionSound().apply {
        load(MediaActionSound.SHUTTER_CLICK)
        load(MediaActionSound.START_VIDEO_RECORDING)
        load(MediaActionSound.STOP_VIDEO_RECORDING)
    }

    var settings by mutableStateOf(store.load()); private set
    var mode by mutableStateOf(CaptureMode.PHOTO); private set
    var lensFacing by mutableIntStateOf(CameraSelector.LENS_FACING_BACK); private set
    var flash by mutableStateOf(FlashSetting.OFF); private set
    var pro by mutableStateOf(ProSettings()); private set
    var caps by mutableStateOf(CameraCaps()); private set
    var cameraReady by mutableStateOf(false); private set
    var activeExtension by mutableStateOf<Int?>(null); private set

    var hud by mutableStateOf<Hud?>(null); private set
    var histogram by mutableStateOf<IntArray?>(null); private set

    var zoomMin by mutableFloatStateOf(1f); private set
    var zoomMax by mutableFloatStateOf(1f); private set
    var zoomTarget by mutableFloatStateOf(1f); private set
    var zoomCurrent by mutableFloatStateOf(1f); private set
    /** Szybkość wygładzania zoomu (1/s); mniejsza = bardziej „kinowo”. */
    var zoomSpeed by mutableFloatStateOf(14f); private set

    var evIndex by mutableIntStateOf(0); private set
    var evMin by mutableIntStateOf(0); private set
    var evMax by mutableIntStateOf(0); private set
    var evStep by mutableFloatStateOf(0f); private set

    var thumbnail by mutableStateOf<Bitmap?>(null); private set
    var lastUri by mutableStateOf<Uri?>(null); private set
    var lastIsVideo by mutableStateOf(false); private set

    var processing by mutableStateOf<Processing?>(null); private set
    var countdown by mutableStateOf<Int?>(null); private set
    var shutterTick by mutableIntStateOf(0); private set
    var message by mutableStateOf<String?>(null); private set

    var recState by mutableStateOf(RecState.IDLE); private set
    var recNanos by mutableLongStateOf(0L); private set

    var deviceRotation by mutableIntStateOf(0)

    // --- Audio ---
    var audioState by mutableStateOf(RecState.IDLE); private set
    var audioMillis by mutableLongStateOf(0L); private set
    val amplitudes = mutableStateListOf<Float>()
    var recordings by mutableStateOf<List<AudioItem>>(emptyList()); private set
    var playingId by mutableStateOf<Long?>(null); private set
    private var player: MediaPlayer? = null
    private var audioJob: Job? = null
    private var timerJob: Job? = null

    val bindKey: BindKey
        get() = BindKey(
            mode = mode,
            lens = lensFacing,
            aspect = settings.aspect,
            video = settings.videoResolution,
            hdr = settings.videoHdr,
            stabilization = settings.stabilization,
            histogram = settings.histogram,
            maxQuality = settings.maxQuality,
            jpeg = settings.jpegQuality,
        )

    val effectiveAspect: AspectRatioMode
        get() = if (mode == CaptureMode.VIDEO) AspectRatioMode.R16_9 else settings.aspect

    val busy get() = processing != null || recState != RecState.IDLE || audioState != RecState.IDLE

    init {
        engine.onHud = { hud = it }
        engine.onHistogram = { histogram = it }
        refreshRecordings()
    }

    // ---------------------------------------------------------------- wiązanie kamery

    suspend fun bind(owner: LifecycleOwner, surfaceProvider: Preview.SurfaceProvider) {
        cameraReady = false
        hud = null
        histogram = null
        try {
            val result = engine.bind(owner, surfaceProvider, BindRequest(lensFacing, mode, settings, pro, flash))
            caps = result
            activeExtension = engine.activeExtension
            if (mode == CaptureMode.AUDIO) return
            val cam = engine.camera ?: return
            val zs = cam.cameraInfo.zoomState.value
            zoomMin = zs?.minZoomRatio ?: 1f
            zoomMax = zs?.maxZoomRatio ?: 1f
            zoomTarget = zoomTarget.coerceIn(zoomMin, zoomMax)
            zoomCurrent = zoomTarget
            engine.setZoom(zoomCurrent)
            val es = cam.cameraInfo.exposureState
            evMin = es.exposureCompensationRange.lower
            evMax = es.exposureCompensationRange.upper
            evStep = es.exposureCompensationStep.toFloat()
            evIndex = 0
            engine.setExposureIndex(0)
            cameraReady = true
        } catch (e: Exception) {
            message = "Kamera niedostępna: ${e.message ?: e.javaClass.simpleName}"
        }
    }

    // ---------------------------------------------------------------- ustawienia

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        settings = transform(settings)
        store.save(settings)
    }

    fun selectMode(m: CaptureMode) {
        if (m == mode || busy) return
        stopPlayback()
        timerJob?.cancel()
        countdown = null
        mode = m
        if (m == CaptureMode.VIDEO && (flash == FlashSetting.AUTO || flash == FlashSetting.ON)) flash = FlashSetting.OFF
        if (m == CaptureMode.NIGHT) flash = FlashSetting.OFF
        if (m == CaptureMode.AUDIO) refreshRecordings()
    }

    fun switchLens() {
        if (busy) return
        val next = if (lensFacing == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
        if (next == CameraSelector.LENS_FACING_FRONT && !caps.hasFront) return
        if (next == CameraSelector.LENS_FACING_BACK && !caps.hasBack) return
        lensFacing = next
        zoomTarget = 1f
        pro = ProSettings()
    }

    fun cycleFlash() {
        val options = when {
            !caps.hasFlash && lensFacing == CameraSelector.LENS_FACING_BACK -> listOf(FlashSetting.OFF)
            mode == CaptureMode.VIDEO -> listOf(FlashSetting.OFF, FlashSetting.TORCH)
            mode == CaptureMode.NIGHT -> listOf(FlashSetting.OFF, FlashSetting.TORCH)
            else -> FlashSetting.entries
        }
        flash = options[(options.indexOf(flash) + 1).mod(options.size)]
        engine.applyFlash(flash, mode)
    }

    fun cycleTimer() {
        val options = listOf(0, 3, 10)
        val next = options[(options.indexOf(settings.timerSeconds) + 1).mod(options.size)]
        updateSettings { it.copy(timerSeconds = next) }
    }

    fun cycleAspect() {
        if (busy) return
        updateSettings { it.copy(aspect = if (it.aspect == AspectRatioMode.R4_3) AspectRatioMode.R16_9 else AspectRatioMode.R4_3) }
    }

    fun cycleVideoResolution() {
        if (busy) return
        val all = VideoResolution.entries
        updateSettings { it.copy(videoResolution = all[(all.indexOf(it.videoResolution) + 1).mod(all.size)]) }
    }

    fun updatePro(p: ProSettings) {
        pro = p
        engine.applyPro(p)
    }

    // ---------------------------------------------------------------- zoom / ostrość / EV

    fun setZoomTarget(value: Float, speed: Float = 14f) {
        zoomTarget = value.coerceIn(zoomMin, zoomMax)
        zoomSpeed = if (mode == CaptureMode.VIDEO && settings.cinematicZoom) minOf(speed, 3.2f) else speed
    }

    /** Wywoływane co klatkę ekranu przez pętlę wygładzania zoomu w UI. */
    fun applyZoomFrame(value: Float) {
        zoomCurrent = value
        engine.setZoom(value)
    }

    fun focus(point: MeteringPoint) {
        engine.focus(point, autoFocus = !(mode == CaptureMode.PRO && pro.focusDiopters != null))
    }

    fun setEv(index: Int) {
        val i = index.coerceIn(evMin, evMax)
        if (i == evIndex) return
        evIndex = i
        engine.setExposureIndex(i)
    }

    // ---------------------------------------------------------------- migawka

    fun onShutter() {
        when (mode) {
            CaptureMode.VIDEO -> toggleVideo()
            CaptureMode.AUDIO -> toggleAudio()
            else -> {
                if (countdown != null) {
                    timerJob?.cancel()
                    countdown = null
                    return
                }
                if (processing != null || !cameraReady) return
                timerJob = viewModelScope.launch {
                    val t = settings.timerSeconds
                    for (i in t downTo 1) {
                        countdown = i
                        delay(1000)
                    }
                    countdown = null
                    capturePhoto()
                }
            }
        }
    }

    fun onVolumeKey(up: Boolean, repeat: Int): Boolean = when (settings.volumeAction) {
        VolumeAction.NONE -> false
        VolumeAction.SHUTTER -> {
            if (repeat == 0) onShutter()
            true
        }
        VolumeAction.ZOOM -> {
            if (mode != CaptureMode.AUDIO) setZoomTarget(zoomTarget * if (up) 1.18f else 1f / 1.18f, 6f)
            true
        }
    }

    private fun surfaceRotation(): Int = when (deviceRotation) {
        90 -> Surface.ROTATION_270
        180 -> Surface.ROTATION_180
        270 -> Surface.ROTATION_90
        else -> Surface.ROTATION_0
    }

    private fun playShutter() {
        shutterTick++
        if (settings.shutterSound) sound.play(MediaActionSound.SHUTTER_CLICK)
    }

    private suspend fun capturePhoto() {
        val rotation = surfaceRotation()
        engine.setTargetRotation(rotation)
        when {
            mode == CaptureMode.NIGHT && activeExtension == null -> nightCapture()
            mode == CaptureMode.HDR && activeExtension == null -> hdrCapture()
            else -> {
                val slow = activeExtension != null
                if (slow) processing = Processing("Przetwarzanie sprzętowe…", null)
                playShutter()
                val tag = when (mode) {
                    CaptureMode.NIGHT -> "NIGHT"
                    CaptureMode.PORTRAIT -> "PORTRAIT"
                    CaptureMode.HDR -> "HDR"
                    CaptureMode.PRO -> "PRO"
                    else -> "IMG"
                }
                engine.takePhoto(rotation, lensFacing == CameraSelector.LENS_FACING_FRONT, tag) { uri, error ->
                    processing = null
                    if (uri != null) onSaved(uri, false) else message = "Nie zapisano zdjęcia: $error"
                }
            }
        }
    }

    private fun progress(label: String, value: Float) {
        viewModelScope.launch(Dispatchers.Main) { processing = Processing(label, value) }
    }

    private suspend fun nightCapture() {
        val n = settings.nightFrames.coerceIn(2, 16)
        val frames = mutableListOf<CapturedFrame>()
        processing = Processing("Nie ruszaj telefonem", 0f)
        playShutter()
        try {
            repeat(n) { i ->
                frames += engine.captureFrame()
                processing = Processing("Nie ruszaj telefonem · ${i + 1}/$n", 0.35f * (i + 1) / n)
            }
            val bitmap = NightStacker.stack(frames.map { it.bytes }) { p -> progress("Składanie $n klatek", 0.35f + 0.55f * p) }
            processing = Processing("Zapisywanie", 0.95f)
            val uri = withContext(Dispatchers.IO) {
                try {
                    MediaSaver.saveBitmap(ctx, bitmap, frames[0].rotation, settings.jpegQuality, "Registrer Noc ×$n", "NIGHT")
                } finally {
                    bitmap.recycle()
                }
            }
            if (uri != null) onSaved(uri, false)
        } catch (oom: OutOfMemoryError) {
            saveFallback(frames, "NIGHT")
        } catch (e: Exception) {
            if (frames.isNotEmpty()) saveFallback(frames, "NIGHT") else message = "Błąd trybu nocnego: ${e.message}"
        } finally {
            processing = null
        }
    }

    private suspend fun hdrCapture() {
        processing = Processing("Seria ekspozycji", 0f)
        playShutter()
        var frames: List<CapturedFrame> = emptyList()
        try {
            frames = engine.captureBracket(listOf(-1.7f, 0f, 1.7f))
            if (frames.size < 2) {
                saveFallback(frames, "HDR")
                return
            }
            val refIndex = frames.size / 2
            val bitmap = ExposureFusion.fuse(frames.map { it.bytes }, refIndex) { p -> progress("Fuzja HDR", 0.3f + 0.65f * p) }
            processing = Processing("Zapisywanie", 0.96f)
            val uri = withContext(Dispatchers.IO) {
                try {
                    MediaSaver.saveBitmap(ctx, bitmap, frames[refIndex].rotation, settings.jpegQuality, "Registrer HDR fusion", "HDR")
                } finally {
                    bitmap.recycle()
                }
            }
            if (uri != null) onSaved(uri, false)
        } catch (oom: OutOfMemoryError) {
            saveFallback(frames, "HDR")
        } catch (e: Exception) {
            if (frames.isNotEmpty()) saveFallback(frames, "HDR") else message = "Błąd HDR: ${e.message}"
        } finally {
            processing = null
        }
    }

    private suspend fun saveFallback(frames: List<CapturedFrame>, tag: String) {
        val frame = frames.getOrNull(frames.size / 2) ?: return
        val uri = withContext(Dispatchers.IO) {
            runCatching { MediaSaver.saveJpeg(ctx, frame.bytes, frame.rotation, "Registrer", tag) }.getOrNull()
        }
        if (uri != null) {
            onSaved(uri, false)
            message = "Zapisano pojedynczą klatkę"
        }
    }

    private fun onSaved(uri: Uri, video: Boolean) {
        lastUri = uri
        lastIsVideo = video
        viewModelScope.launch {
            val thumb = withContext(Dispatchers.IO) {
                runCatching { ctx.contentResolver.loadThumbnail(uri, Size(256, 256), null) }.getOrNull()
            }
            if (thumb != null) thumbnail = thumb
        }
    }

    // ---------------------------------------------------------------- wideo

    private fun hasAudioPermission() =
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    private fun toggleVideo() {
        if (!cameraReady) return
        if (recState == RecState.IDLE) {
            if (settings.videoAudio && !hasAudioPermission()) message = "Brak uprawnienia do mikrofonu – wideo bez dźwięku"
            val started = engine.startRecording(surfaceRotation(), settings.videoAudio) { onVideoEvent(it) }
            if (started) {
                recState = RecState.RECORDING
                recNanos = 0
                if (settings.shutterSound) sound.play(MediaActionSound.START_VIDEO_RECORDING)
            }
        } else {
            engine.stopRecording()
        }
    }

    fun togglePause() {
        when (mode) {
            CaptureMode.VIDEO -> when (recState) {
                RecState.RECORDING -> engine.pauseRecording()
                RecState.PAUSED -> engine.resumeRecording()
                RecState.IDLE -> Unit
            }
            CaptureMode.AUDIO -> when (audioState) {
                RecState.RECORDING -> if (audio.pause()) audioState = RecState.PAUSED
                RecState.PAUSED -> if (audio.resume()) audioState = RecState.RECORDING
                RecState.IDLE -> Unit
            }
            else -> Unit
        }
    }

    private fun onVideoEvent(event: VideoRecordEvent) {
        recNanos = event.recordingStats.recordedDurationNanos
        when (event) {
            is VideoRecordEvent.Pause -> recState = RecState.PAUSED
            is VideoRecordEvent.Resume -> recState = RecState.RECORDING
            is VideoRecordEvent.Finalize -> {
                recState = RecState.IDLE
                recNanos = 0
                if (settings.shutterSound) sound.play(MediaActionSound.STOP_VIDEO_RECORDING)
                val uri = event.outputResults.outputUri
                if (uri != Uri.EMPTY) {
                    onSaved(uri, true)
                    if (event.hasError() && event.error != VideoRecordEvent.Finalize.ERROR_NONE) {
                        message = "Nagranie zakończone z ostrzeżeniem (${event.error})"
                    }
                } else {
                    message = "Nie udało się zapisać wideo (${event.error})"
                }
            }
            else -> Unit
        }
    }

    // ---------------------------------------------------------------- audio

    private fun toggleAudio() {
        if (audioState == RecState.IDLE) startAudio() else stopAudio()
    }

    private fun startAudio() {
        if (!hasAudioPermission()) {
            message = "Zezwól na dostęp do mikrofonu w ustawieniach systemu"
            return
        }
        stopPlayback()
        if (!audio.start(settings.audioPreset, settings.audioSource)) {
            message = "Nie udało się uruchomić mikrofonu"
            return
        }
        amplitudes.clear()
        audioMillis = 0
        audioState = RecState.RECORDING
        if (settings.shutterSound) sound.play(MediaActionSound.START_VIDEO_RECORDING)
        audioJob = viewModelScope.launch {
            var last = SystemClock.elapsedRealtime()
            while (isActive) {
                delay(40)
                val now = SystemClock.elapsedRealtime()
                if (audioState == RecState.RECORDING) {
                    audioMillis += now - last
                    val amp = audio.amplitude().coerceAtLeast(1)
                    val db = 20f * log10(amp / 32767f)
                    amplitudes.add(((db + 60f) / 60f).coerceIn(0f, 1f))
                    if (amplitudes.size > 160) amplitudes.removeAt(0)
                }
                last = now
            }
        }
    }

    private fun stopAudio() {
        audioJob?.cancel()
        audioJob = null
        val uri = audio.stop()
        audioState = RecState.IDLE
        if (settings.shutterSound) sound.play(MediaActionSound.STOP_VIDEO_RECORDING)
        if (uri == null) message = "Nagranie było zbyt krótkie lub wystąpił błąd" else message = "Zapisano nagranie"
        refreshRecordings()
    }

    fun refreshRecordings() {
        viewModelScope.launch {
            recordings = withContext(Dispatchers.IO) { audio.list() }
        }
    }

    fun togglePlay(item: AudioItem) {
        if (playingId == item.id) {
            stopPlayback()
            return
        }
        stopPlayback()
        try {
            player = MediaPlayer().apply {
                setDataSource(ctx, item.uri)
                setOnCompletionListener { stopPlayback() }
                prepare()
                start()
            }
            playingId = item.id
        } catch (e: Exception) {
            message = "Nie można odtworzyć: ${e.message}"
            stopPlayback()
        }
    }

    fun playbackPosition(): Float {
        val p = player ?: return 0f
        return runCatching { p.currentPosition.toFloat() / p.duration.coerceAtLeast(1) }.getOrDefault(0f)
    }

    private fun stopPlayback() {
        player?.let { runCatching { it.stop() }; it.release() }
        player = null
        playingId = null
    }

    fun deleteRecording(item: AudioItem) {
        if (playingId == item.id) stopPlayback()
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) { audio.delete(item) }
            if (!ok) message = "Nie udało się usunąć nagrania"
            refreshRecordings()
        }
    }

    // ---------------------------------------------------------------- cykl życia

    fun onBackground() {
        if (audioState != RecState.IDLE) stopAudio()
        if (recState != RecState.IDLE) engine.stopRecording()
        stopPlayback()
        timerJob?.cancel()
        countdown = null
    }

    fun clearMessage() {
        message = null
    }

    override fun onCleared() {
        audio.release()
        stopPlayback()
        engine.release()
        sound.release()
    }
}
