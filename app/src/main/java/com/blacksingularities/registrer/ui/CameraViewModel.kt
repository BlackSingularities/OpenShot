package com.blacksingularities.registrer.ui

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.AudioManager
import android.media.MediaActionSound
import android.media.MediaPlayer
import android.media.ToneGenerator
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
import com.blacksingularities.registrer.camera.ActiveSession
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
import com.blacksingularities.registrer.computational.PortraitBokeh
import com.blacksingularities.registrer.data.AspectRatioMode
import com.blacksingularities.registrer.data.GlobalSettings
import com.blacksingularities.registrer.data.ModeSettings
import com.blacksingularities.registrer.data.PhotoFormat
import com.blacksingularities.registrer.data.SettingsStore
import com.blacksingularities.registrer.data.VolumeAction
import com.blacksingularities.registrer.update.UpdateManager
import com.blacksingularities.registrer.update.UpdateState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.exp
import kotlin.math.log10

enum class RecState { IDLE, RECORDING, PAUSED }

data class Processing(val label: String, val progress: Float?)

/** Klucz konfiguracji – jego zmiana wymusza ponowne związanie kamery. */
data class BindKey(val mode: CaptureMode, val lens: Int, val settings: ModeSettings)

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
    private val tone = runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 70) }.getOrNull()

    // ---------------------------------------------------------------- ustawienia
    var global by mutableStateOf(store.loadGlobal()); private set
    var profiles by mutableStateOf(store.loadAll()); private set
    var mode by mutableStateOf(global.lastMode.takeIf { it != CaptureMode.SLOWMO } ?: CaptureMode.PHOTO); private set
    val cfg: ModeSettings get() = profiles.getValue(mode)

    var lensFacing by mutableIntStateOf(CameraSelector.LENS_FACING_BACK); private set
    var pro by mutableStateOf(ProSettings()); private set
    var caps by mutableStateOf(CameraCaps()); private set
    var session by mutableStateOf(ActiveSession()); private set
    var cameraReady by mutableStateOf(false); private set
    private val capsCache = mutableMapOf<Int, CameraCaps>()

    var hud by mutableStateOf<Hud?>(null); private set
    var histogram by mutableStateOf<IntArray?>(null); private set

    // ---------------------------------------------------------------- zoom / ekspozycja
    var zoomMin by mutableFloatStateOf(1f); private set
    var zoomMax by mutableFloatStateOf(1f); private set
    var zoomTarget by mutableFloatStateOf(1f); private set
    var zoomCurrent by mutableFloatStateOf(1f); private set
    var zoomSpeed by mutableFloatStateOf(14f); private set
    var evIndex by mutableIntStateOf(0); private set
    var evMin by mutableIntStateOf(0); private set
    var evMax by mutableIntStateOf(0); private set
    var evStep by mutableFloatStateOf(0f); private set
    var focusLocked by mutableStateOf(false); private set

    // ---------------------------------------------------------------- przechwytywanie
    var thumbnail by mutableStateOf<Bitmap?>(null); private set
    var lastUri by mutableStateOf<Uri?>(null); private set
    var lastIsVideo by mutableStateOf(false); private set
    var processing by mutableStateOf<Processing?>(null); private set
    var countdown by mutableStateOf<Int?>(null); private set
    var shutterTick by mutableIntStateOf(0); private set
    var message by mutableStateOf<String?>(null); private set
    var burstCount by mutableIntStateOf(0); private set
    var bursting by mutableStateOf(false); private set
    var intervalShot by mutableIntStateOf(0); private set
    var intervalRunning by mutableStateOf(false); private set

    var recState by mutableStateOf(RecState.IDLE); private set
    var recNanos by mutableLongStateOf(0L); private set
    var recMuted by mutableStateOf(false); private set
    var deviceRotation by mutableIntStateOf(0)

    // ---------------------------------------------------------------- audio
    var audioState by mutableStateOf(RecState.IDLE); private set
    var audioMillis by mutableLongStateOf(0L); private set
    val amplitudes = mutableStateListOf<Float>()
    var audioPeakDb by mutableFloatStateOf(-90f); private set
    var recordings by mutableStateOf<List<AudioItem>>(emptyList()); private set
    var playingId by mutableStateOf<Long?>(null); private set
    private var player: MediaPlayer? = null

    // ---------------------------------------------------------------- aktualizacje
    var update by mutableStateOf<UpdateState>(UpdateState.Idle); private set
    private var updateJob: Job? = null

    private var audioJob: Job? = null
    private var timerJob: Job? = null
    private var burstJob: Job? = null
    private var intervalJob: Job? = null

    val bindKey: BindKey get() = BindKey(mode, lensFacing, cfg.bindKey())

    val effectiveAspect: AspectRatioMode
        get() = if (mode.isVideo) AspectRatioMode.R16_9 else cfg.aspect

    val availableModes: List<CaptureMode>
        get() = CaptureMode.entries.filter {
            when (it) {
                CaptureMode.SLOWMO -> caps.slowMo
                CaptureMode.PORTRAIT -> true // bez rozszerzenia producenta działa programowy bokeh
                else -> true
            }
        }

    /** Presety zoomu odpowiadające obiektywom (+ cyfrowe 2× i 10×, jeśli brak tele). */
    val zoomPresets: List<Float>
        get() {
            val lenses = caps.lensZooms.filter { it in (zoomMin - 0.01f)..(zoomMax + 0.01f) }.ifEmpty {
                buildList {
                    if (zoomMin < 0.95f) add(zoomMin)
                    add(1f)
                }
            }.toMutableList()
            if (lenses.none { it > 1.4f } && zoomMax >= 2f) lenses += 2f
            if (lenses.none { it >= 4.5f } && zoomMax >= 10f) lenses += 10f
            return lenses.distinct().sorted()
        }

    val busy
        get() = processing != null || recState != RecState.IDLE || audioState != RecState.IDLE ||
            bursting || intervalRunning

    init {
        engine.onHud = { hud = it }
        engine.onHistogram = { histogram = it }
        refreshRecordings()
        if (global.autoCheckUpdates) {
            val last = store.getLong("update.lastCheck")
            if (System.currentTimeMillis() - last > 3 * 3600_000L) checkForUpdates(manual = false)
        }
    }

    // ================================================================ wiązanie kamery

    suspend fun bind(owner: LifecycleOwner, surfaceProvider: Preview.SurfaceProvider) {
        cameraReady = false
        hud = null
        histogram = null
        focusLocked = false
        try {
            val facing = engine.resolveFacing(lensFacing)
            if (facing != lensFacing) {
                lensFacing = facing
                return
            }
            val c = capsCache[facing] ?: engine.probe(facing).also { capsCache[facing] = it }
            caps = c
            if (mode !in availableModes) {
                mode = if (mode == CaptureMode.SLOWMO) CaptureMode.VIDEO else CaptureMode.PHOTO
                return
            }
            session = engine.bind(owner, surfaceProvider, BindRequest(lensFacing, mode, cfg, pro), c)
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

    // ================================================================ ustawienia

    fun updateProfile(target: CaptureMode = mode, transform: (ModeSettings) -> ModeSettings) {
        val next = transform(profiles.getValue(target))
        profiles = profiles + (target to next)
        store.save(target, next)
        if (target == mode) engine.applyFlash(next.flash, mode)
    }

    fun resetProfile(target: CaptureMode) = updateProfile(target) { ModeSettings.defaultsFor(target) }

    fun updateGlobal(transform: (GlobalSettings) -> GlobalSettings) {
        global = transform(global)
        store.saveGlobal(global)
    }

    fun selectMode(m: CaptureMode) {
        if (m == mode || busy) return
        stopPlayback()
        timerJob?.cancel()
        countdown = null
        mode = m
        if (m == CaptureMode.AUDIO) refreshRecordings()
        updateGlobal { it.copy(lastMode = m) }
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
            !caps.hasFlash -> listOf(FlashSetting.OFF)
            mode.isVideo || mode == CaptureMode.NIGHT -> listOf(FlashSetting.OFF, FlashSetting.TORCH)
            else -> FlashSetting.entries
        }
        val next = options[(options.indexOf(cfg.flash) + 1).mod(options.size)]
        updateProfile { it.copy(flash = next) }
    }

    fun cycleTimer() {
        val options = listOf(0, 3, 5, 10)
        updateProfile { it.copy(timerSeconds = options[(options.indexOf(it.timerSeconds) + 1).mod(options.size)]) }
    }

    fun cycleAspect() {
        if (busy) return
        updateProfile { it.copy(aspect = if (it.aspect == AspectRatioMode.R4_3) AspectRatioMode.R16_9 else AspectRatioMode.R4_3) }
    }

    fun cycleVideoResolution() {
        if (busy) return
        val all = caps.videoResolutions
        updateProfile { s -> s.copy(videoResolution = all[(all.indexOf(s.videoResolution) + 1).mod(all.size)]) }
    }

    fun cyclePhotoFormat() {
        if (busy) return
        val all = PhotoFormat.entries.filter { it in caps.photoFormats }
        updateProfile { s -> s.copy(photoFormat = all[(all.indexOf(s.photoFormat) + 1).mod(all.size)]) }
    }

    fun updatePro(p: ProSettings) {
        pro = p
        engine.applyPro(p)
    }

    // ================================================================ zoom / ostrość / EV

    fun setZoomTarget(value: Float, speed: Float = 14f) {
        zoomTarget = value.coerceIn(zoomMin, zoomMax)
        zoomSpeed = if (mode.isVideo && cfg.cinematicZoom) minOf(speed, 3.2f) else speed
    }

    /** Przełącznik zoomu: [rate] w jednostkach ln(zoom)/s (+ przybliża, − oddala). */
    fun nudgeZoom(rate: Float, dtSeconds: Float) {
        if (rate == 0f) return
        zoomTarget = (zoomTarget * exp(rate * dtSeconds)).coerceIn(zoomMin, zoomMax)
        zoomSpeed = 30f
    }

    fun applyZoomFrame(value: Float) {
        zoomCurrent = value
        engine.setZoom(value)
    }

    fun focus(point: MeteringPoint, lock: Boolean) {
        engine.focus(point, autoFocus = !(mode == CaptureMode.PRO && pro.focusDiopters != null), lock = lock)
        focusLocked = lock
    }

    fun unlockFocus() {
        if (!focusLocked) return
        engine.cancelFocusLock()
        focusLocked = false
    }

    fun setEv(index: Int) {
        val i = index.coerceIn(evMin, evMax)
        if (i == evIndex) return
        evIndex = i
        engine.setExposureIndex(i)
    }

    // ================================================================ migawka

    fun onShutter() {
        when (mode) {
            CaptureMode.VIDEO, CaptureMode.SLOWMO -> toggleVideo()
            CaptureMode.AUDIO -> toggleAudio()
            else -> {
                if (countdown != null) {
                    timerJob?.cancel()
                    countdown = null
                    return
                }
                if (intervalRunning) {
                    stopInterval()
                    return
                }
                if (processing != null || !cameraReady || bursting) return
                timerJob = viewModelScope.launch {
                    runCountdown()
                    if ((mode == CaptureMode.PHOTO || mode == CaptureMode.PRO) && cfg.intervalSeconds > 0) startInterval() else capturePhoto()
                }
            }
        }
    }

    private suspend fun runCountdown() {
        for (i in cfg.timerSeconds downTo 1) {
            countdown = i
            if (global.timerBeep) tone?.startTone(if (i <= 2) ToneGenerator.TONE_PROP_BEEP2 else ToneGenerator.TONE_PROP_BEEP, 120)
            delay(1000)
        }
        countdown = null
    }

    /** Długie przytrzymanie migawki w trybie Foto: seria zdjęć aż do puszczenia. */
    fun onShutterLongPress() {
        if (mode != CaptureMode.PHOTO || !cfg.burst || !cameraReady || busy) {
            onShutter()
            return
        }
        if (session.photoFormat != PhotoFormat.JPEG) {
            message = "Seria działa tylko w formacie JPEG"
            return
        }
        bursting = true
        burstCount = 0
        burstJob = viewModelScope.launch {
            val rotation = surfaceRotation()
            engine.setTargetRotation(rotation)
            while (isActive && bursting && burstCount < 200) {
                playShutter(quiet = burstCount > 0)
                val r = engine.takePhoto(rotation, mirror(), "BURST")
                if (r.uri != null) {
                    burstCount++
                    lastUri = r.uri
                    lastIsVideo = false
                } else {
                    break
                }
            }
            bursting = false
            lastUri?.let { onSaved(it, false) }
            if (burstCount > 0) message = "Seria: $burstCount zdjęć"
        }
    }

    fun onShutterRelease() {
        if (bursting) bursting = false
    }

    private fun startInterval() {
        intervalRunning = true
        intervalShot = 0
        intervalJob = viewModelScope.launch {
            val total = cfg.intervalCount
            while (isActive && (total == 0 || intervalShot < total)) {
                val started = SystemClock.elapsedRealtime()
                capturePhoto()
                intervalShot++
                val wait = cfg.intervalSeconds * 1000L - (SystemClock.elapsedRealtime() - started)
                if (wait > 0) delay(wait)
            }
            intervalRunning = false
            message = "Interwałometr: $intervalShot zdjęć"
        }
    }

    private fun stopInterval() {
        intervalJob?.cancel()
        intervalRunning = false
        message = "Interwałometr zatrzymany po $intervalShot zdjęciach"
    }

    fun onVolumeKey(up: Boolean, repeat: Int): Boolean = when (global.volumeAction) {
        VolumeAction.NONE -> false
        VolumeAction.SHUTTER -> {
            if (repeat == 0) onShutter()
            true
        }
        VolumeAction.ZOOM -> {
            if (mode != CaptureMode.AUDIO) setZoomTarget(zoomTarget * if (up) 1.15f else 1f / 1.15f, 6f)
            true
        }
    }

    private fun surfaceRotation(): Int = when (deviceRotation) {
        90 -> Surface.ROTATION_270
        180 -> Surface.ROTATION_180
        270 -> Surface.ROTATION_90
        else -> Surface.ROTATION_0
    }

    private fun mirror() = lensFacing == CameraSelector.LENS_FACING_FRONT && cfg.mirrorFront

    private fun playShutter(quiet: Boolean = false) {
        shutterTick++
        if (global.shutterSound && !quiet) sound.play(MediaActionSound.SHUTTER_CLICK)
    }

    /** Automatyczna liczba klatek nocnych: im ciemniejsza scena (histogram, ISO), tym więcej klatek. */
    fun autoNightFrames(): Int {
        val bins = histogram
        val mean = if (bins != null && bins.sum() > 0) {
            bins.indices.sumOf { it.toLong() * bins[it] }.toFloat() / bins.sum() / 63f
        } else {
            null
        }
        val iso = hud?.iso ?: 0
        return when {
            (mean != null && mean < 0.06f) || iso >= 3200 -> 12
            (mean != null && mean < 0.12f) || iso >= 1600 -> 10
            (mean != null && mean < 0.20f) || iso >= 800 -> 8
            mean != null && mean < 0.32f -> 6
            mean == null -> 8
            else -> 4
        }
    }

    private suspend fun capturePhoto() {
        val rotation = surfaceRotation()
        engine.setTargetRotation(rotation)
        when {
            mode == CaptureMode.NIGHT && session.extension == null -> nightCapture()
            mode == CaptureMode.HDR && session.extension == null -> hdrCapture()
            mode == CaptureMode.PORTRAIT && session.extension == null -> portraitCapture()
            else -> {
                val hardware = session.extension != null
                if (hardware) processing = Processing("Przetwarzanie sprzętowe", null)
                playShutter()
                val tag = when (mode) {
                    CaptureMode.NIGHT -> "NIGHT"
                    CaptureMode.PORTRAIT -> "PORTRAIT"
                    CaptureMode.HDR -> "HDR"
                    CaptureMode.PRO -> "PRO"
                    else -> "IMG"
                }
                val result = engine.takePhoto(
                    rotation,
                    mirror(),
                    tag,
                    onProgress = { p -> processing = Processing("Przetwarzanie sprzętowe", p / 100f) },
                    onPostview = { bmp -> thumbnail = bmp },
                )
                processing = null
                val uri = result.uri
                if (uri != null) {
                    onSaved(uri, false, result.secondary)
                } else {
                    message = "Nie zapisano zdjęcia: ${result.error}"
                }
            }
        }
    }

    private fun progress(label: String, value: Float) {
        viewModelScope.launch(Dispatchers.Main) { processing = Processing(label, value) }
    }

    private suspend fun nightCapture() {
        val n = (if (cfg.nightFrames == 0) autoNightFrames() else cfg.nightFrames).coerceIn(2, 16)
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
            val q = cfg.jpegQuality
            val uri = withContext(Dispatchers.IO) {
                try {
                    MediaSaver.saveBitmap(ctx, bitmap, frames[0].rotation, q, "Registrer Noc ×$n", "NIGHT")
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
            val stops = cfg.hdrStops
            frames = engine.captureBracket(listOf(-stops, 0f, stops))
            if (frames.size < 2) {
                saveFallback(frames, "HDR")
                return
            }
            val refIndex = frames.size / 2
            val bitmap = ExposureFusion.fuse(frames.map { it.bytes }, refIndex) { p -> progress("Fuzja HDR", 0.3f + 0.65f * p) }
            processing = Processing("Zapisywanie", 0.96f)
            val q = cfg.jpegQuality
            val uri = withContext(Dispatchers.IO) {
                try {
                    MediaSaver.saveBitmap(ctx, bitmap, frames[refIndex].rotation, q, "Registrer HDR fusion", "HDR")
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

    private suspend fun portraitCapture() {
        processing = Processing("Portret", 0f)
        playShutter()
        var frame: CapturedFrame? = null
        try {
            frame = engine.captureFrame()
            val bitmap = PortraitBokeh.render(frame.bytes, frame.rotation, mirror(), cfg.portraitBlur / 100f) { p ->
                progress("Rozmywanie tła", p)
            }
            processing = Processing("Zapisywanie", 0.97f)
            val q = cfg.jpegQuality
            val uri = withContext(Dispatchers.IO) {
                try {
                    MediaSaver.saveBitmap(ctx, bitmap, 0, q, "Registrer Portret", "PORTRAIT")
                } finally {
                    bitmap.recycle()
                }
            }
            if (uri != null) onSaved(uri, false)
        } catch (oom: OutOfMemoryError) {
            frame?.let { saveFallback(listOf(it), "PORTRAIT") }
        } catch (e: Exception) {
            if (frame != null) saveFallback(listOf(frame), "PORTRAIT") else message = "Błąd portretu: ${e.message}"
        } finally {
            processing = null
        }
    }

    // ================================================================ fold: selfie głównym aparatem

    /** Czy system pozwala przenieść aplikację na ekran zewnętrzny (rozłożony fold). */
    var rearDisplayAvailable by mutableStateOf(false)
    var rearDisplayActive by mutableStateOf(false)
        private set

    fun onRearDisplayChanged(active: Boolean) {
        rearDisplayActive = active
        if (active) {
            // Ekran zewnętrzny patrzy w stronę głównych aparatów.
            if (lensFacing != CameraSelector.LENS_FACING_BACK && caps.hasBack) {
                lensFacing = CameraSelector.LENS_FACING_BACK
                zoomTarget = 1f
                pro = ProSettings()
            }
            message = "Selfie głównym aparatem – złóż telefon lub dotknij ikony, by wrócić"
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

    private fun onSaved(uri: Uri, video: Boolean, alternative: Uri? = null) {
        lastUri = uri
        lastIsVideo = video
        viewModelScope.launch {
            val thumb = withContext(Dispatchers.IO) {
                listOfNotNull(alternative, uri).firstNotNullOfOrNull { u ->
                    runCatching { ctx.contentResolver.loadThumbnail(u, Size(256, 256), null) }.getOrNull()
                }
            }
            if (thumb != null) thumbnail = thumb
        }
    }

    // ================================================================ wideo

    private fun hasAudioPermission() =
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    private fun toggleVideo() {
        if (!cameraReady) return
        if (recState == RecState.IDLE) {
            val withAudio = cfg.videoAudio && mode == CaptureMode.VIDEO
            if (withAudio && !hasAudioPermission()) message = "Brak uprawnienia do mikrofonu – wideo bez dźwięku"
            val started = engine.startRecording(surfaceRotation(), withAudio) { onVideoEvent(it) }
            if (started) {
                recState = RecState.RECORDING
                recNanos = 0
                recMuted = false
                if (global.shutterSound) sound.play(MediaActionSound.START_VIDEO_RECORDING)
            }
        } else {
            engine.stopRecording()
        }
    }

    fun togglePause() {
        when {
            mode.isVideo -> when (recState) {
                RecState.RECORDING -> engine.pauseRecording()
                RecState.PAUSED -> engine.resumeRecording()
                RecState.IDLE -> Unit
            }
            mode == CaptureMode.AUDIO -> when (audioState) {
                RecState.RECORDING -> if (audio.pause()) audioState = RecState.PAUSED
                RecState.PAUSED -> if (audio.resume()) audioState = RecState.RECORDING
                RecState.IDLE -> Unit
            }
        }
    }

    fun toggleMute() {
        if (recState == RecState.IDLE) return
        recMuted = !recMuted
        engine.muteRecording(recMuted)
    }

    /** Zdjęcie w trakcie nagrywania wideo. */
    fun snapshot() {
        if (!session.snapshotInVideo || recState == RecState.IDLE) return
        viewModelScope.launch {
            playShutter(quiet = true)
            val r = engine.takePhoto(surfaceRotation(), false, "VIDSNAP")
            if (r.uri != null) message = "Zapisano kadr z wideo"
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
                if (global.shutterSound) sound.play(MediaActionSound.STOP_VIDEO_RECORDING)
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

    // ================================================================ audio

    private fun toggleAudio() {
        if (audioState == RecState.IDLE) startAudio() else stopAudio()
    }

    private fun startAudio() {
        if (!hasAudioPermission()) {
            message = "Zezwól na dostęp do mikrofonu w ustawieniach systemu"
            return
        }
        stopPlayback()
        if (!audio.start(cfg.audioPreset, cfg.audioSource)) {
            message = "Nie udało się uruchomić mikrofonu"
            return
        }
        amplitudes.clear()
        audioMillis = 0
        audioPeakDb = -90f
        audioState = RecState.RECORDING
        if (global.shutterSound) sound.play(MediaActionSound.START_VIDEO_RECORDING)
        audioJob = viewModelScope.launch {
            var last = SystemClock.elapsedRealtime()
            while (isActive) {
                delay(40)
                val now = SystemClock.elapsedRealtime()
                if (audioState == RecState.RECORDING) {
                    audioMillis += now - last
                    val amp = audio.amplitude().coerceAtLeast(1)
                    val db = 20f * log10(amp / 32767f)
                    audioPeakDb = maxOf(db, audioPeakDb - 0.6f)
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
        if (global.shutterSound) sound.play(MediaActionSound.STOP_VIDEO_RECORDING)
        message = if (uri == null) "Nagranie było zbyt krótkie lub wystąpił błąd" else "Zapisano nagranie"
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

    // ================================================================ aktualizacje

    fun checkForUpdates(manual: Boolean) {
        if (updateJob?.isActive == true) return
        updateJob = viewModelScope.launch {
            update = UpdateState.Checking
            try {
                val info = UpdateManager.fetchLatest()
                store.putLong("update.lastCheck", System.currentTimeMillis())
                if (UpdateManager.isNewer(info.version)) {
                    update = UpdateState.Available(info)
                    if (!manual && global.autoDownloadUpdates) downloadUpdateInternal(info)
                } else {
                    update = UpdateState.UpToDate
                    if (manual) message = "Masz najnowszą wersję (${UpdateManager.currentVersion})"
                }
            } catch (e: Exception) {
                update = if (manual) UpdateState.Failed(e.message ?: "Brak połączenia") else UpdateState.Idle
            }
        }
    }

    fun downloadUpdate() {
        val info = when (val s = update) {
            is UpdateState.Available -> s.info
            is UpdateState.Failed -> return checkForUpdates(manual = true)
            else -> return
        }
        updateJob = viewModelScope.launch { downloadUpdateInternal(info) }
    }

    private suspend fun downloadUpdateInternal(info: com.blacksingularities.registrer.update.ReleaseInfo) {
        try {
            update = UpdateState.Downloading(info, 0f)
            val file = UpdateManager.download(ctx, info) { p ->
                viewModelScope.launch(Dispatchers.Main) {
                    if (update is UpdateState.Downloading) update = UpdateState.Downloading(info, p)
                }
            }
            update = UpdateState.Ready(info, file)
        } catch (e: Exception) {
            update = UpdateState.Failed("Pobieranie nieudane: ${e.message}")
        }
    }

    /** Zwraca false, gdy brakuje zgody na instalowanie – UI otwiera wtedy ustawienia systemu. */
    fun installUpdate(): Boolean {
        val ready = update as? UpdateState.Ready ?: return true
        if (!UpdateManager.canInstall(ctx)) {
            UpdateManager.openInstallPermissionSettings(ctx)
            message = "Zezwól aplikacji Registrer na instalowanie aktualizacji i spróbuj ponownie"
            return false
        }
        update = UpdateState.Installing(ready.info)
        viewModelScope.launch {
            try {
                UpdateManager.install(ctx, ready.file)
            } catch (e: Exception) {
                update = UpdateState.Failed("Instalacja nieudana: ${e.message}")
            }
        }
        return true
    }

    fun dismissUpdate() {
        if (update !is UpdateState.Downloading && update !is UpdateState.Installing) update = UpdateState.Idle
    }

    // ================================================================ cykl życia

    fun onBackground() {
        if (audioState != RecState.IDLE) stopAudio()
        if (recState != RecState.IDLE) engine.stopRecording()
        bursting = false
        if (intervalRunning) stopInterval()
        stopPlayback()
        timerJob?.cancel()
        countdown = null
        // Pobrana aktualizacja instaluje się po wyjściu z aplikacji, by nie przerywać zdjęć.
        val ready = update
        if (ready is UpdateState.Ready && global.autoDownloadUpdates && UpdateManager.canInstallSilently(ctx)) {
            update = UpdateState.Installing(ready.info)
            viewModelScope.launch { runCatching { UpdateManager.install(ctx, ready.file) } }
        }
    }

    fun onForeground() {
        val installing = update
        if (installing is UpdateState.Installing) {
            // Instalacja przerwana albo odrzucona – plik nadal czeka w pamięci podręcznej.
            val file = java.io.File(ctx.cacheDir, "updates/Registrer-${installing.info.version}.apk")
            update = if (file.exists()) UpdateState.Ready(installing.info, file) else UpdateState.Idle
        }
        if (global.autoCheckUpdates && update is UpdateState.Idle) {
            val last = store.getLong("update.lastCheck")
            if (System.currentTimeMillis() - last > 3 * 3600_000L) checkForUpdates(manual = false)
        }
    }

    fun clearMessage() {
        message = null
    }

    override fun onCleared() {
        audio.release()
        stopPlayback()
        engine.release()
        sound.release()
        tone?.release()
    }
}
