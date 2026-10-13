package com.blacksingularities.registrer.ui

import com.blacksingularities.registrer.i18n.tr
import com.blacksingularities.registrer.data.withGroup
import com.blacksingularities.registrer.data.SharedGroup
import com.blacksingularities.registrer.camera.isMoment
import com.blacksingularities.registrer.camera.MomentMedia
import kotlinx.coroutines.sync.withLock
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
import com.blacksingularities.registrer.audio.AudioRecordService
import com.blacksingularities.registrer.audio.RecorderStatus
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
import com.blacksingularities.registrer.data.VideoFps
import com.blacksingularities.registrer.data.VolumeAction
import com.blacksingularities.registrer.update.UpdateManager
import com.blacksingularities.registrer.update.UpdateState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.log10
import kotlin.math.roundToInt

enum class RecState { IDLE, RECORDING, PAUSED }

data class Processing(val label: String, val progress: Float?)

/** Klucz konfiguracji – jego zmiana wymusza ponowne związanie kamery. */
data class BindKey(val mode: CaptureMode, val lens: Int, val cameraId: String?, val display: Int, val settings: ModeSettings, val assist: Boolean = false)

class CameraViewModel(app: Application) : AndroidViewModel(app) {
    private val ctx get() = getApplication<Application>()
    private val store = SettingsStore(app)
    val engine = CameraEngine(app)
    private val audio = AudioEngine.get(app)
    private val locationTracker = com.blacksingularities.registrer.camera.LocationTracker(app)
    private val sound = MediaActionSound().apply {
        load(MediaActionSound.SHUTTER_CLICK)
        load(MediaActionSound.START_VIDEO_RECORDING)
        load(MediaActionSound.STOP_VIDEO_RECORDING)
    }
    private val tone = runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 70) }.getOrNull()

    // ---------------------------------------------------------------- ustawienia
    var global by mutableStateOf(store.loadGlobal()); private set
    private var rawProfiles by mutableStateOf(store.loadAll())

    /** Ustawienia wspólne (grupy [SharedGroup]) – obowiązują w trybach, które ich nie odłączyły. */
    var shared by mutableStateOf(store.loadShared()); private set

    /** Profil trybu z nałożonymi ustawieniami wspólnymi. */
    fun effective(target: CaptureMode): ModeSettings {
        val p = rawProfiles.getValue(target)
        return SharedGroup.entries.fold(p) { acc, g -> if (g in p.custom) acc else acc.withGroup(g, shared) }
    }

    val profiles: Map<CaptureMode, ModeSettings> get() = CaptureMode.entries.associateWith { effective(it) }
    var mode by mutableStateOf(global.lastMode.takeIf { it != CaptureMode.SLOWMO } ?: CaptureMode.PHOTO); private set
    val cfg: ModeSettings get() = effective(mode)

    /** Bieżący tryb nagrywa wideo (Wideo, Zwolnione tempo albo tryb ręczny w wariancie wideo). */
    val videoMode: Boolean get() = mode.isVideo || (mode == CaptureMode.PRO && cfg.proVideo)

    var lensFacing by mutableIntStateOf(CameraSelector.LENS_FACING_BACK); private set
    var pro by mutableStateOf(ProSettings()); private set
    var caps by mutableStateOf(CameraCaps()); private set
    var session by mutableStateOf(ActiveSession()); private set
    var cameraReady by mutableStateOf(false); private set
    private val capsCache = mutableMapOf<Int, CameraCaps>()
    private val capsStore = com.blacksingularities.registrer.camera.CapsCache(app)
    private val capsRefreshed = mutableSetOf<Int>()

    /** Ponowne sondowanie po starcie – wykrywa zmiany (np. po aktualizacji sterowników aparatu). */
    private fun refreshCapsLater(facing: Int) {
        if (!capsRefreshed.add(facing)) return
        viewModelScope.launch {
            delay(2500)
            while (busy) delay(1000)
            val fresh = runCatching { engine.probe(facing) }.getOrNull() ?: return@launch
            capsStore.save(facing, fresh)
            if (fresh != capsCache[facing]) {
                capsCache[facing] = fresh
                if (lensFacing == facing) caps = fresh
            }
        }
    }

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
    var audioLevelL by mutableFloatStateOf(0f); private set
    var audioLevelR by mutableFloatStateOf(0f); private set
    var audioClipping by mutableStateOf(false); private set
    var audioSilent by mutableStateOf(false); private set
    var audioSkippedMs by mutableLongStateOf(0L); private set
    var audioMarkers by mutableIntStateOf(0); private set
    var audioDevice by mutableStateOf<String?>(null); private set
    var audioFormat by mutableStateOf(""); private set
    var audioFinishing by mutableStateOf(false); private set
    var recordings by mutableStateOf<List<AudioItem>>(emptyList()); private set
    var playingId by mutableStateOf<Long?>(null); private set
    var playPaused by mutableStateOf(false); private set
    var playSpeed by mutableFloatStateOf(1f); private set
    private var player: MediaPlayer? = null
    private var inForeground by mutableStateOf(true)

    // ---------------------------------------------------------------- poziom dźwięku w wideo
    private val levelMonitor = com.blacksingularities.registrer.audio.AudioLevelMonitor()
    private var levelJob: Job? = null
    /** Bieżący szczyt (0–1) i opadający wskaźnik szczytu w dBFS. */
    var videoLevel by mutableFloatStateOf(0f); private set
    var videoPeakDb by mutableFloatStateOf(-90f); private set
    /** Mikrofon albo stan dźwięku (wyciszony, zajęty). */
    var videoAudioNote by mutableStateOf<String?>(null); private set
    /** Dźwięk filmu ma problem (wyciszony, mikrofon zajęty, błąd). */
    var videoAudioProblem by mutableStateOf(false); private set

    private fun shouldMonitorLevel(): Boolean =
        global.audioMeter && videoMode && mode != CaptureMode.SLOWMO && cfg.videoAudio && recState == RecState.IDLE && inForeground

    private fun setVideoLevel(linear: Float) {
        videoLevel = linear.coerceIn(0f, 1f)
        val db = 20f * log10(videoLevel.coerceAtLeast(1e-5f))
        videoPeakDb = maxOf(db, videoPeakDb - 0.8f)
    }

    /** Nazwa podłączonego mikrofonu zewnętrznego (odświeżana przy podłączaniu urządzeń). */
    var externalMic by mutableStateOf<String?>(null); private set
    private val deviceCallback = object : android.media.AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out android.media.AudioDeviceInfo>?) {
            externalMic = audio.externalInputName()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out android.media.AudioDeviceInfo>?) {
            externalMic = audio.externalInputName()
        }
    }

    private val audioListener = object : AudioEngine.Listener {
        override fun onStateChanged() = syncAudio()
        override fun onFinished(result: AudioEngine.Finished) = onAudioFinished(result)
    }

    // ---------------------------------------------------------------- aktualizacje
    var update by mutableStateOf<UpdateState>(UpdateState.Idle); private set
    private var updateJob: Job? = null

    private var audioJob: Job? = null
    private var timerJob: Job? = null
    private var burstJob: Job? = null
    private var intervalJob: Job? = null

    val bindKey: BindKey get() = BindKey(mode, lensFacing, activeCameraId.takeIf { mode != CaptureMode.SLOWMO }, displayRotation, cfg.bindKey(), global.peaking || global.zebra)

    /** Maska obrysu ostrości / zebry dla bieżącej klatki podglądu. */
    var assistFrame by mutableStateOf<com.blacksingularities.registrer.camera.AssistFrame?>(null); private set

    private fun assistConfig() = com.blacksingularities.registrer.camera.AssistConfig(global.peaking, global.zebra, global.zebraPercent)

    /**
     * Obrót wyświetlacza (Surface.ROTATION_*), z którym związano kamerę. Zmiana (tryb „Obracaj
     * ekran”) wiąże kamerę od nowa – podgląd zawsze ma poprawny obrót i proporcje. Podczas
     * nagrywania orientacja ekranu jest zablokowana, więc nagranie nie zostanie przerwane.
     */
    var displayRotation by mutableIntStateOf(Surface.ROTATION_0)

    /** Nagrywanie lub przetwarzanie – ekran nie powinien się wtedy obracać. */
    val holdOrientation: Boolean
        get() = recState != RecState.IDLE || processing != null || bursting || intervalRunning || countdown != null || tlRunning

    /** Osobny aparat (szeroki kąt / tele) albo null, gdy aktywny jest aparat główny. */
    var activeCameraId by mutableStateOf<String?>(null); private set

    /** Obiektywy, które po przełączeniu nie dały obrazu – do końca sesji pomijane. */
    private var brokenLenses by mutableStateOf(emptySet<String>())

    /** Osobne obiektywy, z których da się korzystać. */
    private val usableAux get() = caps.auxLenses.filter { it.cameraId !in brokenLenses }

    /** Najmniejsza krotność osiągalna zoomem aparatu głównego (0,6× na telefonach z logicznym szerokim kątem). */
    private val primaryZoomMin: Float
        get() = if (activeCameraId == null) zoomMin else (caps.lensZooms.minOrNull() ?: 1f).coerceAtMost(1f)

    /** Osobny szeroki kąt tylko wtedy, gdy jest wyraźnie szerszy niż zoom aparatu głównego. */
    private val widerAux: com.blacksingularities.registrer.camera.AuxLens?
        get() = usableAux.filter { it.factor < 1f && it.factor < primaryZoomMin * 0.9f }.minByOrNull { it.factor }

    /** Krotność 1× aktywnego aparatu względem głównego (np. 0,6 dla szerokiego kąta). */
    val zoomScale: Float
        get() = caps.auxLenses.firstOrNull { it.cameraId == activeCameraId && mode != CaptureMode.SLOWMO }?.factor ?: 1f

    /** Zoom w jednostkach użytkownika (0,6× / 1× / 3×), niezależnie od aktywnego aparatu. */
    /**
     * Dodatkowy zoom cyfrowy ponad maksimum aparatu (tylko Foto i Pro): podgląd jest
     * powiększany programowo, a zapisane zdjęcie przycinane do widocznego kadru.
     */
    var cropZoom by mutableFloatStateOf(1f); private set
    private val cropAllowed get() = (mode == CaptureMode.PHOTO || (mode == CaptureMode.PRO && !cfg.proVideo)) && session.extension == null
    /** Powyżej 10× zatrzymujemy sensor na 10× i powiększamy programowo, zachowując szerszy obraz nawigatora. */
    val zoomNavigatorBase: Float get() = minOf(ZOOM_NAVIGATOR_THRESHOLD, zoomMax * zoomScale).coerceAtLeast(zoomMin * zoomScale)
    val displayZoom: Float get() = zoomCurrent * zoomScale * cropZoom
    val displayZoomMin: Float
        get() = if (mode == CaptureMode.SLOWMO) zoomMin else minOf(
            zoomMin * zoomScale,
            usableAux.minOfOrNull { it.factor } ?: Float.MAX_VALUE,
            caps.lensZooms.minOrNull() ?: 1f,
        )
    val displayZoomMax: Float get() = if (cropAllowed) MAX_DIGITAL_ZOOM else maxOf(zoomMax * zoomScale, caps.maxZoom)

    val effectiveAspect: AspectRatioMode
        get() = if (videoMode || mode == CaptureMode.TIMELAPSE) AspectRatioMode.R16_9 else cfg.aspect

    val availableModes: List<CaptureMode>
        get() = CaptureMode.entries.filter {
            when (it) {
                CaptureMode.SLOWMO -> capsCache[CameraSelector.LENS_FACING_BACK]?.slowMo
                    ?: (lensFacing == CameraSelector.LENS_FACING_BACK && caps.slowMo)
                CaptureMode.PORTRAIT -> true // bez rozszerzenia producenta działa programowy bokeh
                else -> true
            }
        }

    /** Presety zoomu odpowiadające obiektywom (+ cyfrowe 2× i 10×, jeśli brak tele). */
    val zoomPresets: List<Float>
        get() {
            val lenses = if (mode == CaptureMode.SLOWMO) mutableListOf(1f) else caps.lensZooms.ifEmpty { listOf(1f) }.toMutableList()
            if (mode != CaptureMode.SLOWMO) lenses += usableAux.map { it.factor }
            val max = caps.maxZoom
            if (lenses.none { it > 1.4f } && max >= 2f) lenses += 2f
            if (lenses.none { it >= 4.5f } && max >= 10f) lenses += 10f
            if (cropAllowed) lenses += MAX_DIGITAL_ZOOM
            // Scalenie bliskich wartości (np. 0,6 z zoomu aparatu i 0,55 z sensora fizycznego).
            // Zostaje ta osiągalna zoomem aparatu głównego – działa bez przełączania kamery.
            val reachable = primaryZoomMin - 0.01f
            val merged = mutableListOf<Float>()
            for (z in lenses.sorted()) {
                val i = merged.indexOfFirst { abs(it - z) / z < 0.12f }
                if (i < 0) merged += z else if (merged[i] < reachable && z >= reachable) merged[i] = z
            }
            return merged
        }

    val busy
        get() = processing != null || recState != RecState.IDLE || audioState != RecState.IDLE ||
            bursting || intervalRunning || tlRunning

    init {
        engine.onHud = { hud = it }
        engine.onHistogram = { histogram = it }
        // Podsłuch poziomu przed nagraniem – tylko gdy wskaźnik jest widoczny.
        viewModelScope.launch {
            androidx.compose.runtime.snapshotFlow { shouldMonitorLevel() }.collect { on ->
                if (on && hasAudioPermission()) {
                    levelMonitor.start()
                    levelJob?.cancel()
                    levelJob = launch {
                        while (isActive && levelMonitor.isRunning) {
                            delay(50)
                            setVideoLevel(levelMonitor.peak)
                            videoAudioNote = levelMonitor.deviceName
                        }
                    }
                } else {
                    levelJob?.cancel()
                    levelMonitor.stop()
                    if (recState == RecState.IDLE) setVideoLevel(0f)
                }
            }
        }
        engine.onAssist = { assistFrame = it }
        engine.assist = assistConfig()
        refreshRecordings()
        audio.addListener(audioListener)
        externalMic = audio.externalInputName()
        runCatching { ctx.getSystemService(AudioManager::class.java)?.registerAudioDeviceCallback(deviceCallback, null) }
        // Nagranie mogło trwać w tle, gdy ekran aparatu był zamknięty.
        if (audio.isActive) {
            mode = CaptureMode.AUDIO
            syncAudio()
            startAudioMeter()
        }
        if (global.autoCheckUpdates) {
            val last = store.getLong("update.lastCheck")
            if (System.currentTimeMillis() - last > 3 * 3600_000L) checkForUpdates(manual = false)
        }
    }

    // ================================================================ wiązanie kamery

    suspend fun bind(owner: LifecycleOwner, surfaceProvider: Preview.SurfaceProvider) {
        stopMomentBuffer()
        cameraReady = false
        session = ActiveSession()
        cropZoom = 1f
        hud = null
        histogram = null
        assistFrame = null
        focusLocked = false
        try {
            val facing = engine.resolveFacing(lensFacing)
            if (facing != lensFacing) {
                lensFacing = facing
                return
            }
            // Zapamiętane możliwości wiążą kamerę od razu; świeże sondowanie odbywa się w tle.
            val c = capsCache[facing]
                ?: capsStore.load(facing)?.also {
                    capsCache[facing] = it
                    refreshCapsLater(facing)
                }
                ?: engine.probe(facing).also {
                    capsCache[facing] = it
                    capsStore.save(facing, it)
                    capsRefreshed += facing
                }
            caps = c
            if (mode !in availableModes) {
                mode = if (mode == CaptureMode.SLOWMO) CaptureMode.VIDEO else CaptureMode.PHOTO
                return
            }
            if (activeCameraId != null && c.auxLenses.none { it.cameraId == activeCameraId }) {
                activeCameraId = null
                return
            }
            engine.setDisplayRotation(displayRotation)
            session = engine.bind(owner, surfaceProvider, BindRequest(lensFacing, mode, cfg, pro, activeCameraId), c)
            if (mode == CaptureMode.AUDIO) return
            if (cfg.photoFormat == PhotoFormat.RAW || cfg.photoFormat == PhotoFormat.RAW_JPEG) {
                message = when {
                    session.photoFormat != cfg.photoFormat ->
                        tr("${cfg.photoFormat.label} niedostępny dla wybranego obiektywu – użyto JPEG. Wybierz obiektyw 1× albo sprawdź diagnostykę RAW w ustawieniach.", "${cfg.photoFormat.label} is not available for the selected lens – JPEG was used. Pick the 1× lens or check RAW diagnostics in settings.")
                    c.rawCameraId != null && activeCameraId == null ->
                        tr("${cfg.photoFormat.label} aktywny · sensor RAW #${c.rawCameraId}", "${cfg.photoFormat.label} active · RAW sensor #${c.rawCameraId}")
                    else -> null
                }
            }
            val cam = engine.camera ?: return
            val zs = cam.cameraInfo.zoomState.value
            zoomMin = if (mode == CaptureMode.SLOWMO) maxOf(1f, zs?.minZoomRatio ?: 1f) else zs?.minZoomRatio ?: 1f
            zoomMax = zs?.maxZoomRatio ?: 1f
            zoomTarget = if (mode == CaptureMode.SLOWMO) 1f.coerceIn(zoomMin, zoomMax) else zoomTarget.coerceIn(zoomMin, zoomMax)
            zoomCurrent = zoomTarget
            engine.setZoom(zoomCurrent)
            val es = cam.cameraInfo.exposureState
            evMin = es.exposureCompensationRange.lower
            evMax = es.exposureCompensationRange.upper
            evStep = es.exposureCompensationStep.toFloat()
            evIndex = 0
            engine.setExposureIndex(0)
            cameraReady = true
            if (mode.isMoment) startMomentBuffer()
        } catch (e: Exception) {
            val reason = e.message ?: e.javaClass.simpleName
            if (activeCameraId != null) {
                val failedId = activeCameraId
                val failed = caps.auxLenses.firstOrNull { it.cameraId == failedId }
                if (failedId != null) brokenLenses = brokenLenses + failedId
                activeCameraId = null
                zoomTarget = if ((failed?.factor ?: 1f) < 1f) zoomMin.coerceAtMost(1f) else 1f
                message = tr("Nie udało się włączyć obiektywu ${failed?.factor?.let { formatRatio(it) + "×" } ?: "dodatkowego"}: $reason. Wracam do aparatu głównego.", "Could not start the ${failed?.factor?.let { formatRatio(it) + "×" } ?: "extra"} lens: $reason. Back to the main camera.")
            } else {
                message = tr("Kamera niedostępna: $reason", "Camera unavailable: $reason")
            }
        }
    }

    // ================================================================ ustawienia

    fun updateProfile(target: CaptureMode = mode, transform: (ModeSettings) -> ModeSettings) {
        val next = transform(effective(target))
        // Zmiana w grupie wspólnej obowiązuje we wszystkich trybach, które jej nie odłączyły.
        val nextShared = SharedGroup.entries.fold(shared) { acc, g -> if (g in next.custom) acc else acc.withGroup(g, next) }
        if (nextShared != shared) {
            shared = nextShared
            store.saveShared(nextShared)
        }
        rawProfiles = rawProfiles + (target to next)
        store.save(target, next)
        if (target == mode) engine.applyFlash(next.flash, mode.isVideo || (mode == CaptureMode.PRO && next.proVideo))
    }

    /** Domyślne dla trybu – grupy wspólne zostają nietknięte (reset nie zmienia innych trybów). */
    fun resetProfile(target: CaptureMode) = updateProfile(target) {
        SharedGroup.entries.fold(ModeSettings.defaultsFor(target)) { acc, g -> acc.withGroup(g, shared) }
    }

    fun updateGlobal(transform: (GlobalSettings) -> GlobalSettings) {
        val before = global.geotag
        val beforeLanguage = global.language
        global = transform(global)
        store.saveGlobal(global)
        if (global.language != beforeLanguage) com.blacksingularities.registrer.i18n.Lang.apply(global.language, ctx)
        engine.assist = assistConfig()
        if (!global.peaking && !global.zebra) assistFrame = null
        if (global.geotag != before) refreshLocation()
    }

    /** Oznaczanie lokalizacją: nasłuch tylko przy widocznej aplikacji i włączonej opcji. */
    fun refreshLocation() {
        when {
            !global.geotag -> locationTracker.disable()
            inForeground -> locationTracker.start()
            else -> locationTracker.stop()
        }
    }

    fun selectMode(m: CaptureMode) {
        if (m == mode || busy) return
        rockerFocus = false
        stopPlayback()
        timerJob?.cancel()
        countdown = null
        if (m == CaptureMode.SLOWMO) {
            lensFacing = CameraSelector.LENS_FACING_BACK
            activeCameraId = null
            zoomTarget = 1f
            cropZoom = 1f
        }
        mode = m
        if (m == CaptureMode.AUDIO) refreshRecordings()
        updateGlobal { it.copy(lastMode = m) }
    }

    /** Skróty z ikony aplikacji i kafelek dyktafonu. */
    fun handleShortcut(action: String?) {
        when (action) {
            com.blacksingularities.registrer.MainActivity.ACTION_RECORD_AUDIO -> {
                if (audio.isActive) {
                    mode = CaptureMode.AUDIO
                    return
                }
                if (busy) return
                stopPlayback()
                mode = CaptureMode.AUDIO
                updateGlobal { it.copy(lastMode = CaptureMode.AUDIO) }
                startAudio()
            }
            com.blacksingularities.registrer.MainActivity.ACTION_VIDEO -> selectMode(CaptureMode.VIDEO)
            com.blacksingularities.registrer.MainActivity.ACTION_SELFIE -> {
                selectMode(CaptureMode.PHOTO)
                if (!busy && lensFacing != CameraSelector.LENS_FACING_FRONT) {
                    lensFacing = CameraSelector.LENS_FACING_FRONT
                    activeCameraId = null
                    zoomTarget = 1f
                    pro = ProSettings()
                }
            }
        }
    }

    fun switchLens() {
        if (busy || mode == CaptureMode.SLOWMO) return
        val next = if (lensFacing == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
        if (next == CameraSelector.LENS_FACING_FRONT && !caps.hasFront) return
        if (next == CameraSelector.LENS_FACING_BACK && !caps.hasBack) return
        lensFacing = next
        activeCameraId = null
        zoomTarget = 1f
        pro = ProSettings()
    }

    fun flashOptions(): List<FlashSetting> = when {
        !caps.hasFlash -> listOf(FlashSetting.OFF)
        videoMode || mode == CaptureMode.NIGHT || mode == CaptureMode.LONG || mode == CaptureMode.TIMELAPSE -> listOf(FlashSetting.OFF, FlashSetting.TORCH)
        else -> FlashSetting.entries
    }

    fun cycleFlash() {
        val options = flashOptions()
        val next = options[(options.indexOf(cfg.flash) + 1).mod(options.size)]
        updateProfile { it.copy(flash = next) }
    }

    // Bezpośredni wybór z rozwijanych list górnego paska.
    fun setFlash(f: FlashSetting) = updateProfile { it.copy(flash = f) }
    fun setTimer(seconds: Int) = updateProfile { it.copy(timerSeconds = seconds) }
    fun setAspect(a: AspectRatioMode) {
        if (!busy) updateProfile { it.copy(aspect = a) }
    }
    fun setPhotoFormat(f: PhotoFormat) {
        if (!busy) updateProfile { it.copy(photoFormat = f) }
    }
    fun setVideoResolution(r: com.blacksingularities.registrer.data.VideoResolution) {
        if (!busy) updateProfile { it.copy(videoResolution = r) }
    }
    fun setVideoFps(f: VideoFps) {
        if (!busy) updateProfile { it.copy(videoFps = f) }
    }
    fun setSlowMoFps(fps: Int) {
        if (!busy && fps in slowMoFpsOptions()) updateProfile { it.copy(slowMoFps = fps) }
    }
    fun setSlowMoResolution(resolution: com.blacksingularities.registrer.data.VideoResolution) {
        if (busy || resolution !in slowMoResolutions()) return
        val fps = slowMoFpsOptions(resolution)
        updateProfile { current ->
            current.copy(
                slowMoResolution = resolution,
                slowMoFps = current.slowMoFps.takeIf { it in fps } ?: fps.minOrNull() ?: current.slowMoFps,
            )
        }
    }
    /** Tryb ręczny: przełączenie między zdjęciami a wideo (nowe wiązanie kamery). */
    fun setProVideo(on: Boolean) {
        if (busy || mode != CaptureMode.PRO) return
        cropZoom = 1f
        updateProfile { it.copy(proVideo = on) }
    }

    fun setVideoHdr(on: Boolean) {
        if (!busy) updateProfile { it.copy(videoHdr = on) }
    }
    fun setStabilization(on: Boolean) {
        if (!busy && videoMode && mode != CaptureMode.SLOWMO && caps.videoStabilization) {
            updateProfile { it.copy(stabilization = on) }
        }
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
        if (mode == CaptureMode.SLOWMO) {
            val all = slowMoResolutions()
            if (all.isEmpty()) return
            val current = effectiveSlowMoResolution
            setSlowMoResolution(all[(all.indexOf(current).coerceAtLeast(0) + 1).mod(all.size)])
        } else {
            val all = caps.videoResolutions
            updateProfile { s -> s.copy(videoResolution = all[(all.indexOf(s.videoResolution) + 1).mod(all.size)]) }
        }
    }

    /** Klatkaż: w Wideo Auto/24/30/60 (obsługiwane), w Slow-mo dostępne prędkości wysokoklatkowe. */
    fun cycleVideoFps() {
        if (busy) return
        if (mode == CaptureMode.SLOWMO) {
            val all = slowMoFpsOptions()
            if (all.isEmpty()) return
            updateProfile { s ->
                val current = if (s.slowMoFps in all) s.slowMoFps else all.first()
                s.copy(slowMoFps = all[(all.indexOf(current) + 1).mod(all.size)])
            }
        } else {
            val all = videoFpsOptions()
            updateProfile { s -> s.copy(videoFps = all[(all.indexOf(s.videoFps).coerceAtLeast(0) + 1).mod(all.size)]) }
        }
    }

    fun videoFpsOptions(): List<VideoFps> = VideoFps.entries.filter {
        when (it) {
            VideoFps.F60 -> caps.fps60
            VideoFps.F24 -> caps.fps24
            else -> true
        }
    }

    fun slowMoResolutions(): List<com.blacksingularities.registrer.data.VideoResolution> = caps.slowMoProfiles
        .map { it.resolution }
        .distinct()
        .sortedBy { it.ordinal }

    val effectiveSlowMoResolution: com.blacksingularities.registrer.data.VideoResolution
        get() = cfg.slowMoResolution.takeIf { it in slowMoResolutions() }
            ?: slowMoResolutions().firstOrNull()
            ?: cfg.slowMoResolution

    fun slowMoFpsOptions(resolution: com.blacksingularities.registrer.data.VideoResolution = effectiveSlowMoResolution): List<Int> =
        caps.slowMoProfiles.filter { it.resolution == resolution }.map { it.fps }.distinct().sorted()

    /** Etykieta klatkażu na pasku: rzeczywista wartość po związaniu albo wybór użytkownika. */
    val fpsLabel: String
        get() = when {
            mode == CaptureMode.SLOWMO -> {
                val all = slowMoFpsOptions()
                val fps = if (cfg.slowMoFps in all) cfg.slowMoFps else all.minOrNull() ?: 0
                tr("$fps kl/s", "$fps fps")
            }
            cfg.videoFps == VideoFps.AUTO -> if (session.fps > 0) tr("${session.fps} kl/s", "${session.fps} fps") else tr("Auto kl/s", "Auto fps")
            else -> tr("${cfg.videoFps.fps} kl/s", "${cfg.videoFps.fps} fps")
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

    /** Pro: suwak pod obiektywami steruje ręczną ostrością zamiast zoomem. */
    var rockerFocus by mutableStateOf(false); private set
    var rockerEv by mutableStateOf(false); private set
    private var rockerEvValue = 0f
    private var lastFocusApply = 0L

    val focusAvailable: Boolean get() = mode == CaptureMode.PRO && caps.minFocusDistance > 0f && session.extension == null

    fun toggleRockerFocus() {
        rockerFocus = !rockerFocus && focusAvailable
        if (rockerFocus) rockerEv = false
        if (rockerFocus && pro.focusDiopters == null) {
            // Start od bieżącej odległości ostrzenia z autofokusa, by obraz nie „skoczył”.
            val start = (hud?.focusDiopters ?: 0f).coerceIn(0f, caps.minFocusDistance)
            updatePro(pro.copy(focusDiopters = start))
        }
    }

    fun toggleRockerEv() {
        if (mode != CaptureMode.PRO || evMax <= evMin) return
        rockerEv = !rockerEv
        if (rockerEv) {
            rockerFocus = false
            rockerEvValue = evIndex.toFloat()
        }
    }

    fun nudgeEv(rate: Float, dtSeconds: Float) {
        if (rate == 0f || evMax <= evMin) return
        rockerEvValue = (rockerEvValue + rate * dtSeconds * (evMax - evMin) * 0.7f).coerceIn(evMin.toFloat(), evMax.toFloat())
        val target = rockerEvValue.roundToInt().coerceIn(evMin, evMax)
        if (target != evIndex) setEv(target)
    }

    /**
     * [rate] z suwaka: w prawo – dalej (mniej dioptrii, do ∞), w lewo – bliżej. Pełne wychylenie
     * przejeżdża cały zakres w ok. 1,5 s; zmiany wysyłane do kamery co najwyżej co 30 ms.
     */
    fun nudgeFocus(rate: Float, dtSeconds: Float) {
        if (!focusAvailable || rate == 0f) return
        val range = caps.minFocusDistance
        val current = pro.focusDiopters ?: hud?.focusDiopters ?: 0f
        val next = (current - rate / 1.9f * range * 0.65f * dtSeconds).coerceIn(0f, range)
        pro = pro.copy(focusDiopters = next)
        val now = SystemClock.elapsedRealtime()
        if (now - lastFocusApply >= 30) {
            lastFocusApply = now
            engine.applyPro(pro)
        }
    }

    // ================================================================ zoom / ostrość / EV

    fun setZoomTarget(value: Float, speed: Float = 14f) {
        val softwareBase = zoomNavigatorBase
        if (cropAllowed && value > softwareBase + 0.01f) {
            zoomTarget = softwareBase / zoomScale
            cropZoom = (value / softwareBase).coerceIn(1f, MAX_DIGITAL_ZOOM / softwareBase)
            zoomSpeed = speed
            return
        }
        cropZoom = 1f
        zoomTarget = (value / zoomScale).coerceIn(zoomMin, zoomMax)
        zoomSpeed = if (videoMode && cfg.cinematicZoom) minOf(speed, 3.2f) else speed
    }

    /** Przełącznik zoomu: [rate] w jednostkach ln(zoom)/s (+ przybliża, − oddala). */
    fun nudgeZoom(rate: Float, dtSeconds: Float) {
        if (rate == 0f || !cameraReady) return
        zoomDisplayTo(zoomTarget * zoomScale * cropZoom * exp(rate * dtSeconds), 30f, cinematic = false)
    }

    /** Szczypanie / klawisze głośności: względna zmiana zoomu. */
    fun zoomBy(factor: Float, speed: Float) {
        if (!cameraReady) return
        zoomDisplayTo(zoomTarget * zoomScale * cropZoom * factor, speed, cinematic = true)
    }

    /** Przycisk obiektywu: osobny aparat albo krotność aparatu głównego. */
    fun selectZoomPreset(value: Float) {
        val aux = usableAux.firstOrNull { abs(it.factor - value) / value < 0.1f }
        // Aparat główny osiąga tę krotność (lub prawie tę) zoomem – najpłynniej, bez przełączania kamery.
        val primaryReaches = value >= primaryZoomMin * 0.9f - 0.01f
        when {
            !canSwitchCamera() -> if (activeCameraId == null) setZoomTarget(value, if (videoMode) 4f else 14f)
            primaryReaches && activeCameraId == null -> setZoomTarget(maxOf(value, zoomMin), if (videoMode) 4f else 14f)
            primaryReaches && aux?.cameraId != activeCameraId -> switchCamera(null, maxOf(value, primaryZoomMin))
            aux != null && mode != CaptureMode.SLOWMO -> switchCamera(aux.cameraId, 1f)
            activeCameraId != null -> switchCamera(null, value)
            else -> {
                setZoomTarget(value, if (videoMode) 4f else 14f)
                if (value < zoomMin - 0.01f) {
                    message = tr("Szeroki kąt niedostępny: aparat pozwala od ${formatRatio(zoomMin)}×, a osobnego sensora system nie udostępnia", "Ultra-wide unavailable: the camera starts at ${formatRatio(zoomMin)}× and the system exposes no separate sensor")
                }
            }
        }
    }

    private fun formatRatio(v: Float) = String.format(java.util.Locale.US, "%.1f", v).let { if (com.blacksingularities.registrer.i18n.Lang.polish) it.replace('.', ',') else it }

    /**
     * Osobny obiektyw związał się, ale nie przesyła obrazu (częste przy sensorach fizycznych,
     * których CameraX nie obsługuje w pełni). Wracamy do aparatu głównego i pomijamy ten obiektyw.
     */
    fun onAuxLensStalled() {
        val id = activeCameraId ?: return
        val lens = caps.auxLenses.firstOrNull { it.cameraId == id }
        brokenLenses = brokenLenses + id
        activeCameraId = null
        zoomTarget = if ((lens?.factor ?: 1f) < 1f) primaryZoomMin else 1f
        message = tr("Obiektyw ${lens?.factor?.let { formatRatio(it) + "×" } ?: "dodatkowy"} nie przesyła obrazu – używam zoomu aparatu głównego", "The ${lens?.factor?.let { formatRatio(it) + "×" } ?: "extra"} lens sends no image – using the main camera zoom")
    }

    private fun canSwitchCamera() = recState == RecState.IDLE && !bursting && processing == null && !intervalRunning

    private fun switchCamera(id: String?, ratio: Float) {
        if (id == activeCameraId) {
            setZoomTarget(ratio)
            return
        }
        activeCameraId = id
        zoomTarget = ratio
        zoomSpeed = 14f
    }

    /**
     * Zoom w jednostkach użytkownika. Zejście poniżej minimum aparatu głównego włącza osobny
     * szeroki kąt, a powrót powyżej 1× (lub poniżej krotności tele) wraca do aparatu głównego.
     */
    /** Iloczyn kroków zoomu poniżej minimum aparatu głównego (przejście na szeroki kąt). */
    private var underZoom = 1f

    private fun zoomDisplayTo(display: Float, speed: Float, cinematic: Boolean) {
        val scale = zoomScale
        if (canSwitchCamera() && mode != CaptureMode.SLOWMO) {
            val wide = widerAux
            // Szczypanie podaje małe kroki (np. ×0,98), a zoom głównego aparatu zatrzymuje się na
            // minimum – kroki poniżej minimum sumujemy, inaczej nigdy nie przekroczyłyby progu.
            val belowMin = activeCameraId == null && display < zoomMin * 0.999f
            underZoom = if (belowMin) underZoom * (display / (zoomMin * scale)).coerceIn(0.5f, 1f) else 1f
            when {
                activeCameraId != null && scale < 1f && display >= maxOf(primaryZoomMin, scale * 1.05f) -> return switchCamera(null, display)
                activeCameraId != null && scale > 1f && display < scale * 0.98f -> return switchCamera(null, display)
                belowMin && wide != null && underZoom < 0.97f -> {
                    underZoom = 1f
                    // Start szerokiego kąta tuż pod 1× głównego – dalsze szczypanie płynnie schodzi do 0,6×.
                    return switchCamera(wide.cameraId, (zoomMin * 0.97f / wide.factor).coerceAtLeast(1f))
                }
                belowMin && wide == null && zoomMin > 0.95f && underZoom < 0.97f -> {
                    if (message == null) {
                        message = tr("Szeroki kąt niedostępny: aparat pozwala od ${formatRatio(zoomMin)}×, a osobnego sensora system nie udostępnia", "Ultra-wide unavailable: the camera starts at ${formatRatio(zoomMin)}× and the system exposes no separate sensor")
                    }
                }
            }
        }
        val softwareBase = zoomNavigatorBase
        if (cropAllowed && display > softwareBase) {
            zoomTarget = softwareBase / scale
            cropZoom = (display / softwareBase).coerceIn(1f, MAX_DIGITAL_ZOOM / softwareBase)
            zoomSpeed = speed
            return
        }
        val hw = display / scale
        cropZoom = 1f
        zoomTarget = hw.coerceIn(zoomMin, zoomMax)
        zoomSpeed = if (cinematic && videoMode && cfg.cinematicZoom) minOf(speed, 3.2f) else speed
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
        when {
            videoMode -> toggleVideo()
            mode == CaptureMode.AUDIO -> toggleAudio()
            mode == CaptureMode.TIMELAPSE -> toggleTimelapse()
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
            message = tr("Seria działa tylko w formacie JPEG", "Burst works only in JPEG")
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
            if (burstCount > 0) message = tr("Seria: $burstCount zdjęć", "Burst: $burstCount photos")
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
            message = tr("Interwałometr: $intervalShot zdjęć", "Intervalometer: $intervalShot photos")
        }
    }

    private fun stopInterval() {
        intervalJob?.cancel()
        intervalRunning = false
        message = tr("Interwałometr zatrzymany po $intervalShot zdjęciach", "Intervalometer stopped after $intervalShot photos")
    }

    fun onVolumeKey(up: Boolean, repeat: Int): Boolean = when (global.volumeAction) {
        VolumeAction.NONE -> false
        VolumeAction.SHUTTER -> {
            if (repeat == 0) onShutter()
            true
        }
        VolumeAction.ZOOM -> {
            if (mode != CaptureMode.AUDIO) zoomBy(if (up) 1.15f else 1f / 1.15f, 6f)
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
        val bins = histogram?.copyOfRange(0, 64)
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
            mode == CaptureMode.LONG -> longCapture()
            mode.isMoment -> momentCapture()
            else -> {
                val hardware = session.extension != null
                if (hardware) processing = Processing(tr("Przetwarzanie sprzętowe", "Hardware processing"), null)
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
                    onProgress = { p -> processing = Processing(tr("Przetwarzanie sprzętowe", "Hardware processing"), p / 100f) },
                    onPostview = { bmp -> thumbnail = bmp },
                )
                processing = null
                val uri = result.uri
                if (uri != null) {
                    val crop = cropZoom
                    if (crop > 1.01f && session.photoFormat == PhotoFormat.JPEG) {
                        processing = Processing(tr("Zoom cyfrowy ×${crop.toInt()}", "Digital zoom ×${crop.toInt()}"), null)
                        withContext(Dispatchers.IO) { runCatching { cropSavedPhoto(uri, crop) } }
                        processing = null
                    }
                    onSaved(uri, false, result.secondary)
                } else {
                    message = tr("Nie zapisano zdjęcia: ${result.error}", "Photo not saved: ${result.error}")
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
        processing = Processing(tr("Nie ruszaj telefonem", "Hold the phone still"), 0f)
        playShutter()
        try {
            repeat(n) { i ->
                frames += engine.captureFrame()
                processing = Processing(tr("Nie ruszaj telefonem · ${i + 1}/$n", "Hold the phone still · ${i + 1}/$n"), 0.35f * (i + 1) / n)
            }
            val bitmap = NightStacker.stack(frames.map { it.bytes }) { p -> progress(tr("Składanie $n klatek", "Merging $n frames"), 0.35f + 0.55f * p) }
            processing = Processing(tr("Zapisywanie", "Saving"), 0.95f)
            val q = cfg.jpegQuality
            val uri = withContext(Dispatchers.IO) {
                try {
                    MediaSaver.saveBitmap(ctx, bitmap, frames[0].rotation, q, "OpenShot Noc ×$n", "NIGHT")
                } finally {
                    bitmap.recycle()
                }
            }
            if (uri != null) onSaved(uri, false)
        } catch (oom: OutOfMemoryError) {
            saveFallback(frames, "NIGHT")
        } catch (e: Exception) {
            if (frames.isNotEmpty()) saveFallback(frames, "NIGHT") else message = tr("Błąd trybu nocnego: ${e.message}", "Night mode error: ${e.message}")
        } finally {
            processing = null
        }
    }

    private suspend fun hdrCapture() {
        processing = Processing(tr("Seria ekspozycji", "Exposure bracket"), 0f)
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
            val bitmap = ExposureFusion.fuse(frames.map { it.bytes }, refIndex) { p -> progress(tr("Fuzja HDR", "HDR fusion"), 0.3f + 0.65f * p) }
            processing = Processing(tr("Zapisywanie", "Saving"), 0.96f)
            val q = cfg.jpegQuality
            val uri = withContext(Dispatchers.IO) {
                try {
                    MediaSaver.saveBitmap(ctx, bitmap, frames[refIndex].rotation, q, "OpenShot HDR fusion", "HDR")
                } finally {
                    bitmap.recycle()
                }
            }
            if (uri != null) onSaved(uri, false)
        } catch (oom: OutOfMemoryError) {
            saveFallback(frames, "HDR")
        } catch (e: Exception) {
            if (frames.isNotEmpty()) saveFallback(frames, "HDR") else message = tr("Błąd HDR: ${e.message}", "HDR error: ${e.message}")
        } finally {
            processing = null
        }
    }

    /**
     * Długi czas: zbiera klatki przez zadany czas (najwyżej [LONG_MAX_FRAMES]) i łączy je
     * uśrednianiem (rozmyty ruch) albo maksimum jasności (smugi świateł).
     */
    private suspend fun longCapture() {
        val seconds = cfg.longSeconds.coerceIn(1, 30)
        val trails = cfg.longBlend == com.blacksingularities.registrer.data.LongBlend.TRAILS
        val frames = mutableListOf<CapturedFrame>()
        processing = Processing(tr("Naświetlanie – nie ruszaj telefonem", "Exposing – hold the phone still"), 0f)
        playShutter()
        val start = SystemClock.elapsedRealtime()
        try {
            while (frames.size < LONG_MAX_FRAMES) {
                frames += engine.captureFrame()
                val elapsed = SystemClock.elapsedRealtime() - start
                processing = Processing(tr("Naświetlanie · ${frames.size} kl.", "Exposing · ${frames.size} fr."), 0.4f * (elapsed / (seconds * 1000f)).coerceAtMost(1f))
                if (elapsed >= seconds * 1000L && frames.size >= 3) break
            }
            if (global.shutterSound) sound.play(MediaActionSound.SHUTTER_CLICK)
            val n = frames.size
            val bitmap = com.blacksingularities.registrer.computational.LongExposure.blend(frames.map { it.bytes }, trails, cfg.longAlign) { p ->
                progress(tr("Składanie $n klatek", "Merging $n frames"), 0.4f + 0.55f * p)
            }
            processing = Processing(tr("Zapisywanie", "Saving"), 0.96f)
            val q = cfg.jpegQuality
            val label = if (trails) "smugi" else "rozmycie"
            val uri = withContext(Dispatchers.IO) {
                try {
                    MediaSaver.saveBitmap(ctx, bitmap, frames[0].rotation, q, "OpenShot Długi czas $seconds s ×$n ($label)", "LONG")
                } finally {
                    bitmap.recycle()
                }
            }
            if (uri != null) onSaved(uri, false)
        } catch (oom: OutOfMemoryError) {
            saveFallback(frames, "LONG")
        } catch (e: Exception) {
            if (frames.isNotEmpty()) saveFallback(frames, "LONG") else message = tr("Błąd długiego czasu: ${e.message}", "Long exposure error: ${e.message}")
        } finally {
            processing = null
        }
    }

    private suspend fun portraitCapture() {
        processing = Processing(tr("Portret", "Portrait"), 0f)
        playShutter()
        var frame: CapturedFrame? = null
        try {
            frame = engine.captureFrame()
            val bitmap = PortraitBokeh.render(frame.bytes, frame.rotation, mirror(), cfg.portraitBlur / 100f) { p ->
                progress(tr("Rozmywanie tła", "Blurring the background"), p)
            }
            processing = Processing(tr("Zapisywanie", "Saving"), 0.97f)
            val q = cfg.jpegQuality
            val uri = withContext(Dispatchers.IO) {
                try {
                    MediaSaver.saveBitmap(ctx, bitmap, 0, q, "OpenShot Portret", "PORTRAIT")
                } finally {
                    bitmap.recycle()
                }
            }
            if (uri != null) onSaved(uri, false)
        } catch (oom: OutOfMemoryError) {
            frame?.let { saveFallback(listOf(it), "PORTRAIT") }
        } catch (e: Exception) {
            if (frame != null) saveFallback(listOf(frame), "PORTRAIT") else message = tr("Błąd portretu: ${e.message}", "Portrait error: ${e.message}")
        } finally {
            processing = null
        }
    }

    // ================================================================ podgląd dla fotografowanego

    /** Drugi ekran (tylny ekran folda, ekran zewnętrzny) może pokazać podgląd fotografowanemu. */
    var subjectPreviewAvailable by mutableStateOf(false)
    var subjectPreviewActive by mutableStateOf(false)
    /** Wykryte ekrany – do diagnostyki w ustawieniach. */
    var displaySummary by mutableStateOf("")

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
            message = tr("Autoportret głównym aparatem – złóż telefon lub dotknij ikony, by wrócić", "Selfie with the main camera – fold the phone or tap the icon to go back")
        }
    }

    /** Przycina zapisane zdjęcie do środka kadru (zoom cyfrowy ponad maksimum aparatu). */
    private fun cropSavedPhoto(uri: Uri, crop: Float) {
        val resolver = ctx.contentResolver
        val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return
        val src = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return
        val orientation = runCatching {
            androidx.exifinterface.media.ExifInterface(java.io.ByteArrayInputStream(bytes))
                .getAttribute(androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION)
        }.getOrNull()
        val w = (src.width / crop).toInt().coerceAtLeast(16)
        val h = (src.height / crop).toInt().coerceAtLeast(16)
        val cut = Bitmap.createBitmap(src, (src.width - w) / 2, (src.height - h) / 2, w, h)
        // Małe wycinki skalujemy do co najmniej 2048 px dłuższego boku.
        val scale = (2048f / maxOf(w, h)).coerceAtLeast(1f)
        val out = if (scale > 1f) Bitmap.createScaledBitmap(cut, (w * scale).toInt(), (h * scale).toInt(), true) else cut
        val tmp = java.io.File(ctx.cacheDir, "crop_${System.nanoTime()}.jpg")
        try {
            tmp.outputStream().use { out.compress(Bitmap.CompressFormat.JPEG, cfg.jpegQuality, it) }
            runCatching {
                val exif = androidx.exifinterface.media.ExifInterface(tmp.absolutePath)
                if (orientation != null) exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION, orientation)
                exif.setAttribute(androidx.exifinterface.media.ExifInterface.TAG_SOFTWARE, "OpenShot zoom cyfrowy ×${crop.toInt()}")
                MediaSaver.tagGps(exif)
                exif.saveAttributes()
            }
            resolver.openOutputStream(uri, "wt")?.use { os -> tmp.inputStream().use { it.copyTo(os) } }
        } finally {
            tmp.delete()
            src.recycle()
            if (cut !== src) cut.recycle()
            if (out !== cut) out.recycle()
        }
    }

    // ================================================================ film poklatkowy

    var tlRunning by mutableStateOf(false); private set
    var tlFrames by mutableIntStateOf(0); private set
    var tlStartedAt by mutableLongStateOf(0L); private set
    /** Rzeczywisty rozmiar kodowanego filmu (może być mniejszy niż wybrany). */
    var tlSize by mutableStateOf<String?>(null); private set
    private var tlJob: Job? = null

    /** Odstęp między zdjęciami w ms: przyspieszenie / klatkaż filmu. */
    val tlIntervalMs: Long get() = (cfg.tlSpeed * 1000L / cfg.tlFps.coerceAtLeast(1)).coerceAtLeast(200L)

    /** Rozdzielczości filmu, które obsługuje koder telefonu. */
    fun timelapseResolutions(): List<com.blacksingularities.registrer.data.TimelapseResolution> = tlResolutionsCache

    private val tlResolutionsCache by lazy {
        com.blacksingularities.registrer.data.TimelapseResolution.entries.filter {
            com.blacksingularities.registrer.camera.TimelapseWriter.pickMime(it.width, it.height) != null
        }.ifEmpty { listOf(com.blacksingularities.registrer.data.TimelapseResolution.FHD) }
    }

    private fun toggleTimelapse() {
        if (tlRunning) {
            tlJob?.cancel()
            return
        }
        if (!cameraReady || processing != null) return
        val c = cfg
        val res = c.tlResolution.takeIf { it in timelapseResolutions() } ?: timelapseResolutions().last()
        val fps = c.tlFps
        val interval = tlIntervalMs
        val limitMs = c.tlStopMin * 60_000L
        tlRunning = true
        tlFrames = 0
        tlSize = null
        tlStartedAt = SystemClock.elapsedRealtime()
        if (global.shutterSound) sound.play(MediaActionSound.START_VIDEO_RECORDING)
        if (c.tlLock) engine.setExposureLock(true)
        val out = java.io.File(ctx.cacheDir, "timelapse_${System.nanoTime()}.mp4")
        tlJob = viewModelScope.launch {
            var writer: com.blacksingularities.registrer.camera.TimelapseWriter? = null
            var failure: String? = null
            try {
                while (isActive) {
                    val started = SystemClock.elapsedRealtime()
                    if (limitMs > 0 && started - tlStartedAt >= limitMs) break
                    engine.setTargetRotation(surfaceRotation())
                    val frame = engine.captureFrame()
                    withContext(Dispatchers.Default) {
                        if (writer == null) {
                            // Rozmiar filmu: wybrany, ale nie większy niż klatki z aparatu.
                            val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                            android.graphics.BitmapFactory.decodeByteArray(frame.bytes, 0, frame.bytes.size, bounds)
                            val fit = com.blacksingularities.registrer.camera.TimelapseWriter.fitSize(
                                res.width, res.height, bounds.outWidth, bounds.outHeight,
                            ) ?: error(tr("Koder nie obsługuje żadnej rozdzielczości", "The encoder supports no resolution"))
                            writer = com.blacksingularities.registrer.camera.TimelapseWriter.create(
                                out, fit.first, fit.second, fps, frame.rotation, c.tlDeflicker,
                            ) ?: error(tr("Nie udało się uruchomić kodera wideo", "Could not start the video encoder"))
                            withContext(Dispatchers.Main) {
                                tlSize = when (fit.first) {
                                    7680 -> "8K"
                                    3840 -> "4K"
                                    1920 -> "1080p"
                                    else -> "${fit.second}p"
                                }
                                if (fit.first < res.width) message = tr("Film w ${tlSize} – aparat lub koder nie pozwala na ${res.label}", "Video in ${tlSize} – the camera or encoder does not allow ${res.label}")
                            }
                        }
                        writer?.addFrame(frame.bytes)
                    }
                    tlFrames = writer?.frames ?: tlFrames
                    val wait = interval - (SystemClock.elapsedRealtime() - started)
                    if (wait > 0) delay(wait)
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                // zatrzymanie przyciskiem
            } catch (e: Exception) {
                failure = e.message ?: e.javaClass.simpleName
            } finally {
                kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) { finishTimelapse(writer, out, failure) }
            }
        }
    }

    private suspend fun finishTimelapse(writer: com.blacksingularities.registrer.camera.TimelapseWriter?, out: java.io.File, error: String?) {
        if (cfg.tlLock) engine.setExposureLock(false)
        if (global.shutterSound && inForeground) sound.play(MediaActionSound.STOP_VIDEO_RECORDING)
        val frames = writer?.frames ?: 0
        if (writer == null || frames < 2) {
            writer?.let { w -> withContext(Dispatchers.Default) { w.finish() } }
            out.delete()
            tlRunning = false
            message = error?.let { tr("Film poklatkowy przerwany: $it", "Time-lapse interrupted: $it") } ?: tr("Za mało klatek na film (potrzeba co najmniej 2)", "Not enough frames for a video (at least 2 needed)")
            return
        }
        processing = Processing(tr("Zapisywanie filmu poklatkowego", "Saving the time-lapse"), null)
        val uri = withContext(Dispatchers.IO) {
            val ok = writer.finish()
            if (ok) runCatching { MediaSaver.saveVideoFile(ctx, out, "TL") }.getOrNull() else null
        }
        out.delete()
        processing = null
        tlRunning = false
        if (uri != null) {
            onSaved(uri, true)
            val seconds = frames.toFloat() / cfg.tlFps
            message = String.format(com.blacksingularities.registrer.i18n.Lang.locale, tr("Zapisano film poklatkowy: %d klatek · %.1f s", "Time-lapse saved: %d frames · %.1f s"), frames, seconds) +
                (error?.let { tr(" (przerwano: $it)", " (interrupted: $it)") } ?: "")
        } else {
            message = tr("Nie udało się zapisać filmu poklatkowego", "Could not save the time-lapse")
        }
    }

    // ================================================================ zdjęcia ruchome

    private var momentJob: Job? = null
    private var segment: CameraEngine.Segment? = null
    private val momentLock = kotlinx.coroutines.sync.Mutex()
    private var momentBusy = false

    /** Bufor w tle: odcinki nagrania po ok. 10 s, zastępowane kolejnymi. */
    private fun startMomentBuffer() {
        momentJob?.cancel()
        momentJob = viewModelScope.launch {
            while (isActive) {
                momentLock.withLock { if (segment == null && cameraReady && mode.isMoment) newSegment() }
                delay(1000)
                momentLock.withLock {
                    val s = segment ?: return@withLock
                    if (!momentBusy && SystemClock.elapsedRealtime() - s.startedAt > 10_000) {
                        segment = null
                        s.finish()
                        s.file.delete()
                        newSegment()
                    }
                }
            }
        }
    }

    private suspend fun newSegment(): CameraEngine.Segment? {
        val file = java.io.File(ctx.cacheDir, "moment_${System.nanoTime()}.mp4")
        val withAudio = cfg.videoAudio && hasAudioPermission()
        val s = engine.startSegment(file, surfaceRotation(), withAudio)
        segment = s
        if (s == null) file.delete()
        return s
    }

    private fun stopMomentBuffer() {
        momentJob?.cancel()
        momentJob = null
        val s = segment
        segment = null
        if (s != null) {
            viewModelScope.launch {
                s.finish()
                s.file.delete()
            }
        }
    }

    /** Migawka w trybie Live: zdjęcie (Motion Photo) + fragment bufora 1,5 s przed i 1,5 s po. */
    private suspend fun momentCapture() {
        val seg = segment
        if (seg == null) {
            message = tr("Bufor nagrania jeszcze się uruchamia – spróbuj za chwilę", "The recording buffer is still starting – try again in a moment")
            return
        }
        momentBusy = true
        try {
            playShutter()
            val pressMs = SystemClock.elapsedRealtime() - seg.startedAt
            val photo = if (session.snapshotInVideo) runCatching { engine.captureFrame() }.getOrNull() else null
            processing = Processing(tr("Zdjęcie ruchome", "Motion photo"), null)
            delay(1500)
            val ok = momentLock.withLock {
                if (segment === seg) segment = null
                val done = seg.finish()
                newSegment()
                done
            }
            if (!ok) {
                message = tr("Nie udało się zapisać fragmentu nagrania", "Could not save the recording clip")
                seg.file.delete()
                return
            }
            val clip = java.io.File(ctx.cacheDir, "clip_${System.nanoTime()}.mp4")
            val saved = withContext(Dispatchers.IO) {
                try {
                    val fromUs = ((pressMs - 1500).coerceAtLeast(0)) * 1000
                    val toUs = (pressMs + 1500) * 1000
                    val base = MomentMedia.trim(seg.file, clip, fromUs, toUs)
                    val presentationUs = (pressMs * 1000 - base).coerceAtLeast(0)
                    val still = photo?.let { MomentMedia.jpegWithOrientation(ctx, it.bytes, it.rotation) }
                        ?: MomentMedia.frameAt(seg.file, pressMs * 1000)
                        ?: error(tr("Brak klatki zdjęcia", "No photo frame"))
                    val file = MomentMedia.buildMotionPhoto(still, clip.readBytes(), presentationUs)
                    MomentMedia.saveImage(ctx, file)
                } catch (e: Exception) {
                    null
                } finally {
                    clip.delete()
                    seg.file.delete()
                }
            }
            if (saved != null) {
                onSaved(saved, false)
                message = tr("Zapisano zdjęcie ruchome", "Motion photo saved")
            } else {
                message = tr("Nie udało się zapisać chwili", "Could not save the moment")
            }
        } finally {
            processing = null
            momentBusy = false
        }
    }

    private suspend fun saveFallback(frames: List<CapturedFrame>, tag: String) {
        val frame = frames.getOrNull(frames.size / 2) ?: return
        val uri = withContext(Dispatchers.IO) {
            runCatching { MediaSaver.saveJpeg(ctx, frame.bytes, frame.rotation, "OpenShot", tag) }.getOrNull()
        }
        if (uri != null) {
            onSaved(uri, false)
            message = tr("Zapisano pojedynczą klatkę", "Saved a single frame")
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
            val withAudio = cfg.videoAudio && mode != CaptureMode.SLOWMO
            if (withAudio && !hasAudioPermission()) message = tr("Brak uprawnienia do mikrofonu – wideo bez dźwięku", "No microphone permission – video without sound")
            levelJob?.cancel()
            levelMonitor.stopAndWait()
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
            videoMode -> when (recState) {
                RecState.RECORDING -> engine.pauseRecording()
                RecState.PAUSED -> engine.resumeRecording()
                RecState.IDLE -> Unit
            }
            mode == CaptureMode.AUDIO -> when (audioState) {
                RecState.RECORDING -> audio.pause()
                RecState.PAUSED -> audio.resume()
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
            if (r.uri != null) message = tr("Zapisano kadr z wideo", "Saved a still from the video")
        }
    }

    private fun onVideoEvent(event: VideoRecordEvent) {
        recNanos = event.recordingStats.recordedDurationNanos
        if (global.audioMeter) {
            val stats = event.recordingStats.audioStats
            setVideoLevel(stats.audioAmplitude.toFloat())
            when (stats.audioState) {
                androidx.camera.video.AudioStats.AUDIO_STATE_MUTED -> videoAudioNote = tr("wyciszony", "muted")
                androidx.camera.video.AudioStats.AUDIO_STATE_SOURCE_SILENCED -> videoAudioNote = tr("mikrofon zajęty przez inną aplikację", "microphone in use by another app")
                androidx.camera.video.AudioStats.AUDIO_STATE_DISABLED -> videoAudioNote = tr("nagrywanie bez dźwięku", "recording without sound")
                androidx.camera.video.AudioStats.AUDIO_STATE_SOURCE_ERROR, androidx.camera.video.AudioStats.AUDIO_STATE_ENCODER_ERROR ->
                    videoAudioNote = tr("błąd dźwięku", "audio error")
                else -> if (videoAudioProblem) videoAudioNote = null
            }
            videoAudioProblem = stats.audioState != androidx.camera.video.AudioStats.AUDIO_STATE_ACTIVE
        }
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
                        message = tr("Nagranie zakończone z ostrzeżeniem (${event.error})", "Recording finished with a warning (${event.error})")
                    }
                } else {
                    message = tr("Nie udało się zapisać wideo (${event.error})", "Could not save the video (${event.error})")
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
            message = tr("Zezwól na dostęp do mikrofonu w ustawieniach systemu", "Allow microphone access in system settings")
            return
        }
        if (audio.isActive) return
        stopPlayback()
        val c = cfg
        val error = audio.start(
            AudioEngine.Config(
                preset = c.audioPreset,
                source = c.audioSource,
                input = c.audioInput,
                gainDb = c.audioGainDb,
                noiseSuppression = c.audioNoiseSuppression,
                agc = c.audioAgc,
                skipSilence = c.audioSkipSilence,
                maxDurationMs = c.audioAutoStopMin * 60_000L,
            ),
        )
        if (error != null) {
            message = tr("Nie udało się uruchomić nagrywania: $error", "Could not start recording: $error")
            return
        }
        AudioRecordService.start(ctx)
        if (c.audioInput == com.blacksingularities.registrer.data.AudioInput.EXTERNAL && audio.externalInput() == null) {
            message = tr("Brak mikrofonu zewnętrznego – nagrywam wbudowanym", "No external microphone – recording with the built-in one")
        }
        amplitudes.clear()
        audioPeakDb = -90f
        syncAudio()
        if (global.shutterSound) sound.play(MediaActionSound.START_VIDEO_RECORDING)
        startAudioMeter()
    }

    /** Odczyt mierników silnika ok. 25 razy na sekundę – tylko przy widocznym ekranie. */
    private fun startAudioMeter() {
        audioJob?.cancel()
        if (!inForeground) return
        audioJob = viewModelScope.launch {
            while (isActive && audio.isActive) {
                delay(40)
                audioMillis = audio.recordedMs
                val l = audio.peakL
                val r = audio.peakR
                audioLevelL = l
                audioLevelR = r
                val db = 20f * log10(maxOf(l, r).coerceAtLeast(1e-5f))
                audioPeakDb = maxOf(db, audioPeakDb - 0.6f)
                audioClipping = SystemClock.elapsedRealtime() - audio.lastClipAt < 1500
                audioSilent = audio.silent
                audioSkippedMs = audio.skippedMs
                if (audio.status == RecorderStatus.RECORDING && !audio.silent) {
                    amplitudes.add(((db + 60f) / 60f).coerceIn(0f, 1f))
                    if (amplitudes.size > 160) amplitudes.removeAt(0)
                }
            }
        }
    }

    private fun syncAudio() {
        audioState = when (audio.status) {
            RecorderStatus.IDLE -> RecState.IDLE
            RecorderStatus.PAUSED -> RecState.PAUSED
            RecorderStatus.RECORDING, RecorderStatus.FINISHING -> RecState.RECORDING
        }
        audioFinishing = audio.status == RecorderStatus.FINISHING
        audioMarkers = audio.markerCount
        audioDevice = audio.deviceName
        audioFormat = audio.formatLabel
        audioSilent = audio.silent
        audioMillis = audio.recordedMs
    }

    private fun onAudioFinished(result: AudioEngine.Finished) {
        audioJob?.cancel()
        audioJob = null
        syncAudio()
        audioLevelL = 0f
        audioLevelR = 0f
        audioClipping = false
        if (global.shutterSound && inForeground) sound.play(MediaActionSound.STOP_VIDEO_RECORDING)
        val length = com.blacksingularities.registrer.ui.components.formatClock(result.durationMs, false)
        message = when {
            result.uri == null && result.error != null -> tr("Nie zapisano nagrania: ${result.error}", "Recording not saved: ${result.error}")
            result.uri == null -> tr("Nagranie było zbyt krótkie", "The recording was too short")
            result.autoStopped -> tr("Automatyczne zatrzymanie – zapisano $length", "Auto stop – saved $length")
            else -> tr("Zapisano nagranie · $length", "Recording saved · $length")
        }
        refreshRecordings()
    }

    private fun stopAudio() {
        audio.stop()
    }

    /** Znacznik w bieżącym miejscu nagrania (widoczny potem na osi odtwarzania). */
    fun addAudioMarker() {
        if (audio.addMarker() == null) return
        audioMarkers = audio.markerCount
    }

    fun refreshRecordings() {
        viewModelScope.launch {
            recordings = withContext(Dispatchers.IO) { audio.list() }
        }
    }

    /** Dotknięcie przycisku odtwarzania: start, pauza albo wznowienie. */
    fun togglePlay(item: AudioItem) {
        val current = player
        if (playingId == item.id && current != null) {
            if (playPaused) {
                runCatching { current.start() }
                playPaused = false
                applySpeed()
            } else {
                runCatching { current.pause() }
                playPaused = true
            }
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
            playPaused = false
            applySpeed()
        } catch (e: Exception) {
            message = tr("Nie można odtworzyć: ${e.message}", "Cannot play: ${e.message}")
            stopPlayback()
        }
    }

    private fun applySpeed() {
        val p = player ?: return
        if (playPaused) return
        runCatching { p.playbackParams = p.playbackParams.setSpeed(playSpeed) }
    }

    fun cyclePlaybackSpeed() {
        val steps = listOf(1f, 1.25f, 1.5f, 2f, 0.75f)
        val i = steps.indexOfFirst { abs(it - playSpeed) < 0.01f }
        playSpeed = steps[(i + 1) % steps.size]
        applySpeed()
    }

    /** Przewija odtwarzane nagranie (lub rozpoczyna odtwarzanie innego) do ułamka długości. */
    fun seekPlayback(item: AudioItem, fraction: Float) {
        if (playingId != item.id) togglePlay(item)
        val p = player ?: return
        val d = runCatching { p.duration }.getOrDefault(0)
        if (d > 0) runCatching { p.seekTo((d * fraction.coerceIn(0f, 1f)).toLong(), MediaPlayer.SEEK_CLOSEST) }
    }

    fun skipPlayback(deltaMs: Int) {
        val p = player ?: return
        runCatching {
            val target = (p.currentPosition + deltaMs).coerceIn(0, p.duration)
            p.seekTo(target.toLong(), MediaPlayer.SEEK_CLOSEST)
        }
    }

    /** Skok do następnego znacznika (po ostatnim – do pierwszego). */
    fun nextMarker(item: AudioItem) {
        if (item.markers.isEmpty()) return
        if (playingId != item.id) togglePlay(item)
        val p = player ?: return
        val pos = runCatching { p.currentPosition.toLong() }.getOrDefault(0L)
        val next = item.markers.firstOrNull { it > pos + 800 } ?: item.markers.first()
        runCatching { p.seekTo(next, MediaPlayer.SEEK_CLOSEST) }
    }

    fun playbackPosition(): Float {
        val p = player ?: return 0f
        return runCatching { p.currentPosition.toFloat() / p.duration.coerceAtLeast(1) }.getOrDefault(0f)
    }

    private fun stopPlayback() {
        player?.let { runCatching { it.stop() }; it.release() }
        player = null
        playingId = null
        playPaused = false
    }

    fun deleteRecording(item: AudioItem) {
        if (playingId == item.id) stopPlayback()
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) { audio.delete(item) }
            if (!ok) message = tr("Nie udało się usunąć nagrania", "Could not delete the recording")
            refreshRecordings()
        }
    }

    fun renameRecording(item: AudioItem, title: String) {
        if (title.trim() == item.title) return
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) { audio.rename(item, title) }
            if (!ok) message = tr("Nie udało się zmienić nazwy", "Could not rename")
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
                    if (manual) message = tr("Masz najnowszą wersję (${UpdateManager.currentVersion})", "You have the latest version (${UpdateManager.currentVersion})")
                }
            } catch (e: Exception) {
                update = if (manual) UpdateState.Failed(e.message ?: tr("Brak połączenia", "No connection")) else UpdateState.Idle
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
            update = UpdateState.Failed(tr("Pobieranie nieudane: ${e.message}", "Download failed: ${e.message}"))
        }
    }

    /** Zwraca false, gdy brakuje zgody na instalowanie – UI otwiera wtedy ustawienia systemu. */
    fun installUpdate(): Boolean {
        val ready = update as? UpdateState.Ready ?: return true
        if (!UpdateManager.canInstall(ctx)) {
            UpdateManager.openInstallPermissionSettings(ctx)
            message = tr("Zezwól aplikacji OpenShot na instalowanie aktualizacji i spróbuj ponownie", "Allow OpenShot to install updates and try again")
            return false
        }
        update = UpdateState.Installing(ready.info)
        viewModelScope.launch {
            try {
                UpdateManager.install(ctx, ready.file)
            } catch (e: Exception) {
                update = UpdateState.Failed(tr("Instalacja nieudana: ${e.message}", "Installation failed: ${e.message}"))
            }
        }
        return true
    }

    fun dismissUpdate() {
        if (update !is UpdateState.Downloading && update !is UpdateState.Installing) update = UpdateState.Idle
    }

    // ================================================================ cykl życia

    fun onBackground() {
        stopMomentBuffer()
        // Nagrywanie dźwięku trwa dalej w usłudze pierwszoplanowej; mierniki usypiają.
        inForeground = false
        // Aparat zostaje zwolniony – film poklatkowy kończy się z dotychczasowymi klatkami.
        if (tlRunning) tlJob?.cancel()
        refreshLocation()
        audioJob?.cancel()
        audioJob = null
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
        inForeground = true
        refreshLocation()
        syncAudio()
        if (audio.isActive) startAudioMeter()
        refreshRecordings()
        if (mode.isMoment && cameraReady && momentJob == null) startMomentBuffer()
        val installing = update
        if (installing is UpdateState.Installing) {
            // Instalacja przerwana albo odrzucona – plik nadal czeka w pamięci podręcznej.
            val file = java.io.File(ctx.cacheDir, "updates/OpenShot-${installing.info.version}.apk")
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
        stopMomentBuffer()
        audio.removeListener(audioListener)
        levelMonitor.stop()
        locationTracker.stop()
        runCatching { ctx.getSystemService(AudioManager::class.java)?.unregisterAudioDeviceCallback(deviceCallback) }
        stopPlayback()
        engine.release()
        sound.release()
        tone?.release()
    }
}

/** Maksymalny zoom cyfrowy (z przycięciem zdjęcia) w trybach Foto i Pro. */
const val MAX_DIGITAL_ZOOM = 100f
const val ZOOM_NAVIGATOR_THRESHOLD = 10f
const val LONG_MAX_FRAMES = 32
