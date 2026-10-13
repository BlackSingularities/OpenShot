package com.blacksingularities.registrer.data

import android.content.Context
import android.content.SharedPreferences
import com.blacksingularities.registrer.camera.CaptureMode
import com.blacksingularities.registrer.camera.FlashSetting
import com.blacksingularities.registrer.i18n.AppLanguage
import com.blacksingularities.registrer.i18n.tr

/** Proporcje kadru; [ratio] to szerokość / wysokość w orientacji pionowej. */
enum class AspectRatioMode(val label: String, val ratio: Float) {
    R4_3("4:3", 3f / 4f),
    R16_9("16:9", 9f / 16f),
}

/** Siatki kompozycyjne – można włączyć dowolny zestaw naraz. */
enum class GridType(private val pl: String, private val en: String) {
    THIRDS("3×3", "3×3"),
    GOLDEN("Złoty podział", "Golden ratio"),
    SQUARE("Kwadrat", "Square"),
    DIAGONAL("Przekątne", "Diagonals"),
    CENTER("Celownik", "Crosshair"),
    ;

    val label: String get() = tr(pl, en)
}

enum class HistogramMode(private val pl: String, private val en: String) {
    LUMA("Jasność", "Luma"),
    RGB("RGB", "RGB"),
    ;

    val label: String get() = tr(pl, en)
}

enum class HistogramSize(private val pl: String, private val en: String, val widthDp: Int, val heightDp: Int) {
    SMALL("Mały", "Small", 92, 42),
    MEDIUM("Średni", "Medium", 120, 56),
    LARGE("Duży", "Large", 164, 76),
    ;

    val label: String get() = tr(pl, en)
}

enum class PhotoFormat(val label: String) {
    JPEG("JPEG"),
    ULTRA_HDR("Ultra HDR"),
    RAW_JPEG("RAW+JPEG"),
    RAW("RAW"),
}

enum class CapturePriority(private val pl: String, private val en: String) {
    SPEED("Szybkość", "Speed"),
    BALANCED("Balans", "Balanced"),
    QUALITY("Jakość", "Quality"),
    ;

    val label: String get() = tr(pl, en)
}

enum class VideoResolution(val label: String) {
    UHD("4K"),
    FHD("1080p"),
    HD("720p"),
}

enum class VideoFps(private val pl: String, private val en: String, val fps: Int) {
    AUTO("Auto", "Auto", 0),
    F24("24", "24", 24),
    F30("30", "30", 30),
    F60("60", "60", 60),
    ;

    val label: String get() = tr(pl, en)
}

enum class VideoBitrate(private val pl: String, private val en: String) {
    AUTO("Auto", "Auto"),
    HIGH("Wysoki", "High"),
    MAX("Maks.", "Max"),
    ;

    val label: String get() = tr(pl, en)
}

enum class TimelapseResolution(val label: String, val width: Int, val height: Int) {
    FHD("1080p", 1920, 1080),
    UHD("4K", 3840, 2160),
    UHD8K("8K", 7680, 4320),
}

/** Gotowe przyspieszenia z podpowiedzią sceny (nazwy w bieżącym języku). */
val TIMELAPSE_SPEEDS: List<Pair<Int, String>>
    get() = listOf(
        15 to tr("Tłum", "Crowd"),
        30 to tr("Ulica", "Street"),
        60 to tr("Ruch", "Traffic"),
        120 to tr("Chmury", "Clouds"),
        300 to tr("Zachód", "Sunset"),
        600 to tr("Gwiazdy", "Stars"),
        1800 to tr("Budowa", "Building"),
    )

enum class LevelStyle(private val pl: String, private val en: String) {
    HORIZON("Horyzont", "Horizon"),
    AVIATION("Lotniczy", "Aviation"),
    TUBES("Libelle", "Vials"),
    CROSS("Krzyż", "Cross"),
    ;

    val label: String get() = tr(pl, en)
}

enum class LongBlend(private val pl: String, private val en: String, private val plDesc: String, private val enDesc: String) {
    MOTION(
        "Rozmycie ruchu", "Motion blur",
        "Średnia klatek: gładka woda, chmury, znikający przechodnie",
        "Frame average: silky water, streaking clouds, vanishing passers-by",
    ),
    TRAILS(
        "Smugi światła", "Light trails",
        "Najjaśniejsze piksele: światła aut, gwiazdy, ogień",
        "Brightest pixels: car lights, stars, fire",
    ),
    ;

    val label: String get() = tr(pl, en)
    val description: String get() = tr(plDesc, enDesc)
}

enum class PortraitEffect(private val pl: String, private val en: String) {
    BOKEH("Rozmycie tła", "Background blur"),
    RETOUCH("Retusz", "Retouch"),
    ;

    val label: String get() = tr(pl, en)
}

enum class VolumeAction(private val pl: String, private val en: String) {
    SHUTTER("Migawka", "Shutter"),
    ZOOM("Zoom", "Zoom"),
    NONE("Głośność", "Volume"),
    ;

    val label: String get() = tr(pl, en)
}

enum class ScreenRotation(private val pl: String, private val en: String, private val plDesc: String, private val enDesc: String) {
    FIXED("Stały", "Fixed", "Ekran stoi, obracają się ikony i informacje", "The screen stays put; icons and info rotate"),
    AUTO(
        "Obracaj", "Rotate",
        "Cały interfejs obraca się z telefonem, także przy systemowej blokadzie obrotu",
        "The whole interface rotates with the phone, even with system rotation lock on",
    ),
    ;

    val label: String get() = tr(pl, en)
    val description: String get() = tr(plDesc, enDesc)
}

enum class AudioPreset(
    private val pl: String,
    private val en: String,
    private val plDesc: String,
    private val enDesc: String,
    val sampleRate: Int,
    val bitRate: Int,
    val channels: Int,
    /** Bezstratny WAV zamiast AAC. */
    val lossless: Boolean = false,
) {
    VOICE("Mowa", "Speech", "AAC 96 kb/s · mono · 44,1 kHz", "AAC 96 kb/s · mono · 44.1 kHz", 44_100, 96_000, 1),
    MUSIC("Muzyka", "Music", "AAC 256 kb/s · stereo · 48 kHz", "AAC 256 kb/s · stereo · 48 kHz", 48_000, 256_000, 2),
    STUDIO("Studyjny", "Studio", "AAC 320 kb/s · stereo · 48 kHz", "AAC 320 kb/s · stereo · 48 kHz", 48_000, 320_000, 2),
    LOSSLESS(
        "Bezstratny", "Lossless",
        "WAV 16-bit · stereo · 48 kHz · ok. 11 MB/min", "WAV 16-bit · stereo · 48 kHz · approx. 11 MB/min",
        48_000, 0, 2, lossless = true,
    ),
    ;

    val label: String get() = tr(pl, en)
    val description: String get() = tr(plDesc, enDesc)
}

enum class AudioInput(private val pl: String, private val en: String, private val plDesc: String, private val enDesc: String) {
    AUTO("Auto", "Auto", "System wybiera mikrofon", "The system picks the microphone"),
    BUILTIN("Telefon", "Phone", "Zawsze wbudowany mikrofon", "Always the built-in microphone"),
    EXTERNAL("Zewnętrzny", "External", "Przewodowy, USB lub Bluetooth LE, gdy jest podłączony", "Wired, USB or Bluetooth LE when connected"),
    ;

    val label: String get() = tr(pl, en)
    val description: String get() = tr(plDesc, enDesc)
}

enum class AudioSourceType(private val pl: String, private val en: String, private val plDesc: String, private val enDesc: String) {
    STANDARD("Zwykłe", "Standard", "Domyślne przetwarzanie mikrofonu", "Default microphone processing"),
    RAW("Surowy", "Raw", "Bez filtrów – najwierniejszy dźwięk", "No filters – the most faithful sound"),
    VOICE("Głos", "Voice", "Redukcja szumów i echa", "Noise and echo reduction"),
    CAMCORDER("Kierunkowy", "Directional", "Mikrofon skierowany jak w kamerze", "Microphone aimed like a camcorder's"),
    ;

    val label: String get() = tr(pl, en)
    val description: String get() = tr(plDesc, enDesc)
}

/**
 * Grupy ustawień wspólne dla wszystkich trybów. Tryb może odłączyć grupę (pole [ModeSettings.custom])
 * i ustawiać ją tylko dla siebie.
 */
enum class SharedGroup(private val pl: String, private val en: String) {
    FRAMING("Kadr", "Framing"),
    PHOTO("Zdjęcia", "Photos"),
    VIDEO("Wideo", "Video"),
    ;

    val label: String get() = tr(pl, en)
}

/** Kopiuje wartości grupy [group] z [src]. */
fun ModeSettings.withGroup(group: SharedGroup, src: ModeSettings): ModeSettings = when (group) {
    SharedGroup.FRAMING -> copy(grids = src.grids, level = src.level, histogram = src.histogram, timerSeconds = src.timerSeconds)
    SharedGroup.PHOTO -> copy(jpegQuality = src.jpegQuality, mirrorFront = src.mirrorFront, burst = src.burst)
    SharedGroup.VIDEO -> copy(videoAudio = src.videoAudio, stabilization = src.stabilization, bitrate = src.bitrate, cinematicZoom = src.cinematicZoom)
}

/**
 * Profil jednego trybu. Każdy tryb (Foto, Noc, Portret, HDR, Pro, Wideo, Slow-mo, Audio)
 * ma własną kopię – ekran ustawień pokazuje tylko pola istotne dla danego trybu.
 */
data class ModeSettings(
    // kadr i nakładki
    val aspect: AspectRatioMode = AspectRatioMode.R4_3,
    val grids: Set<GridType> = setOf(GridType.THIRDS),
    val level: Boolean = true,
    val histogram: Boolean = true,
    val timerSeconds: Int = 0,
    val flash: FlashSetting = FlashSetting.OFF,
    // zdjęcia
    val photoFormat: PhotoFormat = PhotoFormat.JPEG,
    val jpegQuality: Int = 95,
    val priority: CapturePriority = CapturePriority.BALANCED,
    val mirrorFront: Boolean = true,
    val burst: Boolean = true,
    val intervalSeconds: Int = 0,
    val intervalCount: Int = 0,
    // tryby specjalne
    val nightFrames: Int = 0, // 0 = automatycznie
    val lowLightBoost: Boolean = true,
    val portraitEffect: PortraitEffect = PortraitEffect.BOKEH,
    val portraitBlur: Int = 60, // siła rozmycia programowego bokeh, 0–100
    val hdrStops: Float = 1.7f,
    /** Długi czas naświetlania: czas zbierania klatek w sekundach. */
    val longSeconds: Int = 2,
    val longBlend: LongBlend = LongBlend.MOTION,
    /** Wyrównywanie klatek (z ręki); na statywie można wyłączyć. */
    val longAlign: Boolean = true,
    // wideo
    val videoResolution: VideoResolution = VideoResolution.FHD,
    val videoFps: VideoFps = VideoFps.AUTO,
    val videoHdr: Boolean = false,
    val stabilization: Boolean = true,
    val videoAudio: Boolean = true,
    val bitrate: VideoBitrate = VideoBitrate.AUTO,
    val cinematicZoom: Boolean = true,
    val slowMoResolution: VideoResolution = VideoResolution.HD,
    val slowMoFps: Int = 0, // 0 = najniższy dostępny high-speed (najstabilniejszy podgląd)
    // film poklatkowy
    /** Przyspieszenie: ×N względem czasu rzeczywistego (odstęp = N / klatkaż). */
    val tlSpeed: Int = 120,
    val tlFps: Int = 30,
    val tlResolution: TimelapseResolution = TimelapseResolution.UHD,
    /** Zakończ po tylu minutach; 0 = ręcznie. */
    val tlStopMin: Int = 0,
    val tlDeflicker: Boolean = true,
    /** Blokada ekspozycji i balansu bieli na czas nagrania. */
    val tlLock: Boolean = false,
    /** Braketowanie w trybie ręcznym: liczba zdjęć (0 = wyłączone) i krok w EV. */
    val bracketCount: Int = 0,
    val bracketStep: Float = 1f,
    /** Płaski profil (LOG) w wideo trybu ręcznego. */
    val flatProfile: Boolean = false,
    /** Tryb ręczny nagrywa wideo zamiast zdjęć. */
    val proVideo: Boolean = false,
    // audio
    val audioPreset: AudioPreset = AudioPreset.MUSIC,
    val audioSource: AudioSourceType = AudioSourceType.STANDARD,
    val audioInput: AudioInput = AudioInput.AUTO,
    /** Cyfrowe wzmocnienie z miękkim ogranicznikiem, 0–24 dB. */
    val audioGainDb: Int = 0,
    val audioNoiseSuppression: Boolean = false,
    val audioAgc: Boolean = false,
    /** Pauzy dłuższe niż 1,5 s nie trafiają do pliku. */
    val audioSkipSilence: Boolean = false,
    /** Automatyczne zatrzymanie po tylu minutach; 0 = wyłączone. */
    val audioAutoStopMin: Int = 0,
    /** Grupy odłączone od ustawień wspólnych (ustawiane tylko dla tego trybu). */
    val custom: Set<SharedGroup> = emptySet(),
) {
    /** Pola wymagające ponownego związania kamery – reszta działa „na żywo”. */
    fun bindKey(): ModeSettings = ModeSettings(
        aspect = aspect,
        histogram = histogram,
        photoFormat = photoFormat,
        jpegQuality = jpegQuality,
        priority = priority,
        portraitEffect = portraitEffect,
        videoResolution = videoResolution,
        videoFps = videoFps,
        videoHdr = videoHdr,
        stabilization = stabilization,
        bitrate = bitrate,
        slowMoResolution = slowMoResolution,
        slowMoFps = slowMoFps,
        proVideo = proVideo,
        tlResolution = tlResolution,
        flatProfile = flatProfile,
        lowLightBoost = lowLightBoost,
    )

    companion object {
        fun defaultsFor(mode: CaptureMode): ModeSettings = when (mode) {
            CaptureMode.NIGHT -> ModeSettings(priority = CapturePriority.QUALITY, histogram = false, grids = emptySet())
            CaptureMode.PORTRAIT -> ModeSettings(grids = emptySet(), histogram = false, priority = CapturePriority.QUALITY)
            CaptureMode.HDR -> ModeSettings(priority = CapturePriority.QUALITY)
            CaptureMode.PRO -> ModeSettings(priority = CapturePriority.QUALITY, grids = setOf(GridType.THIRDS))
            CaptureMode.VIDEO -> ModeSettings(aspect = AspectRatioMode.R16_9, histogram = false)
            CaptureMode.SLOWMO -> ModeSettings(aspect = AspectRatioMode.R16_9, histogram = false, level = false)
            CaptureMode.AUDIO -> ModeSettings(histogram = false, level = false, grids = emptySet())
            CaptureMode.LONG -> ModeSettings(histogram = false)
            CaptureMode.TIMELAPSE -> ModeSettings(aspect = AspectRatioMode.R16_9, histogram = false)
            CaptureMode.PHOTO -> ModeSettings()
            CaptureMode.MOTION -> ModeSettings()
        }
    }
}

data class GlobalSettings(
    val haptics: Boolean = true,
    val shutterSound: Boolean = true,
    val timerBeep: Boolean = true,
    val volumeAction: VolumeAction = VolumeAction.SHUTTER,
    val screenRotation: ScreenRotation = ScreenRotation.FIXED,
    val exposureInfo: Boolean = true,
    /** Pozycja histogramu w kadrze (0–1 w poziomie i pionie). */
    val histogramX: Float = 1f,
    val histogramY: Float = 0.12f,
    /** Pozycja paska parametrów i kompasu w kadrze (0–1). */
    val stripX: Float = 0.5f,
    val stripY: Float = 0f,
    val compass: Boolean = false,
    val histogramMode: HistogramMode = HistogramMode.LUMA,
    val histogramSize: HistogramSize = HistogramSize.MEDIUM,
    val compassX: Float = 0.5f,
    val compassY: Float = 0.1f,
    /** Obrys ostrości (focus peaking) we wszystkich trybach z obrazem. */
    val peaking: Boolean = false,
    /** Zebra – paski na prześwietlonych obszarach. */
    val zebra: Boolean = false,
    val zebraPercent: Int = 95,
    val levelStyle: LevelStyle = LevelStyle.HORIZON,
    /** Granica „prosto” w stopniach. */
    val levelTolerance: Float = 1f,
    val levelPitch: Boolean = true,
    val levelFade: Boolean = false,
    /** Przesunięcie czujnika grawitacji (m/s²) zmierzone na płaskiej powierzchni. */
    val levelCalX: Float = 0f,
    val levelCalY: Float = 0f,
    val levelCalZ: Float = 0f,
    /** Pozycja automatycznego nawigatora zoomu >10×. */
    val zoomNavigatorX: Float = 0f,
    val zoomNavigatorY: Float = 0.18f,
    /** Podgląd dla fotografowanego jak w lustrze. */
    val subjectMirror: Boolean = true,
    /** Pływający wskaźnik poziomu dźwięku w trybie Wideo. */
    val audioMeter: Boolean = false,
    val audioMeterX: Float = 0f,
    val audioMeterY: Float = 1f,
    val autoCheckUpdates: Boolean = true,
    val autoDownloadUpdates: Boolean = true,
    /** Zapis współrzędnych GPS w zdjęciach i filmach. */
    val geotag: Boolean = true,
    val lastMode: CaptureMode = CaptureMode.PHOTO,
    /** Język interfejsu. */
    val language: AppLanguage = AppLanguage.SYSTEM,
)

class SettingsStore(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("registrer_v2", Context.MODE_PRIVATE)

    fun loadGlobal(): GlobalSettings {
        val d = GlobalSettings()
        return GlobalSettings(
            haptics = prefs.getBoolean("g.haptics", d.haptics),
            shutterSound = prefs.getBoolean("g.sound", d.shutterSound),
            timerBeep = prefs.getBoolean("g.beep", d.timerBeep),
            volumeAction = enumOr(prefs.getString("g.volume", null), d.volumeAction),
            screenRotation = enumOr(prefs.getString("g.rotation", null), d.screenRotation),
            exposureInfo = prefs.getBoolean("g.exposureInfo", d.exposureInfo),
            histogramX = prefs.getFloat("g.histX", d.histogramX),
            histogramY = prefs.getFloat("g.histY", d.histogramY),
            stripX = prefs.getFloat("g.stripX", d.stripX),
            stripY = prefs.getFloat("g.stripY", d.stripY),
            compass = prefs.getBoolean("g.compass", d.compass),
            histogramMode = enumOr(prefs.getString("g.histMode", null), d.histogramMode),
            histogramSize = enumOr(prefs.getString("g.histSize", null), d.histogramSize),
            compassX = prefs.getFloat("g.compassX", d.compassX),
            compassY = prefs.getFloat("g.compassY", d.compassY),
            peaking = prefs.getBoolean("g.peaking", d.peaking),
            zebra = prefs.getBoolean("g.zebra", d.zebra),
            zebraPercent = prefs.getInt("g.zebraPct", d.zebraPercent),
            levelStyle = enumOr(prefs.getString("g.levelStyle", null), d.levelStyle),
            levelTolerance = prefs.getFloat("g.levelTol", d.levelTolerance),
            levelPitch = prefs.getBoolean("g.levelPitch", d.levelPitch),
            levelFade = prefs.getBoolean("g.levelFade", d.levelFade),
            levelCalX = prefs.getFloat("g.levelCalX", d.levelCalX),
            levelCalY = prefs.getFloat("g.levelCalY", d.levelCalY),
            levelCalZ = prefs.getFloat("g.levelCalZ", d.levelCalZ),
            zoomNavigatorX = prefs.getFloat("g.zoomNavX", d.zoomNavigatorX),
            zoomNavigatorY = prefs.getFloat("g.zoomNavY", d.zoomNavigatorY),
            subjectMirror = prefs.getBoolean("g.subjectMirror", d.subjectMirror),
            audioMeter = prefs.getBoolean("g.audioMeter", d.audioMeter),
            audioMeterX = prefs.getFloat("g.audioMeterX", d.audioMeterX),
            audioMeterY = prefs.getFloat("g.audioMeterY", d.audioMeterY),
            autoCheckUpdates = prefs.getBoolean("g.autoCheck", d.autoCheckUpdates),
            autoDownloadUpdates = prefs.getBoolean("g.autoDownload", d.autoDownloadUpdates),
            geotag = prefs.getBoolean("g.geotag", d.geotag),
            lastMode = enumOr(prefs.getString("g.lastMode", null), d.lastMode),
            language = enumOr(prefs.getString("g.language", null), d.language),
        )
    }

    fun saveGlobal(g: GlobalSettings) {
        prefs.edit()
            .putBoolean("g.haptics", g.haptics)
            .putBoolean("g.sound", g.shutterSound)
            .putBoolean("g.beep", g.timerBeep)
            .putString("g.volume", g.volumeAction.name)
            .putString("g.rotation", g.screenRotation.name)
            .putBoolean("g.exposureInfo", g.exposureInfo)
            .putFloat("g.histX", g.histogramX)
            .putFloat("g.histY", g.histogramY)
            .putFloat("g.stripX", g.stripX)
            .putFloat("g.stripY", g.stripY)
            .putBoolean("g.compass", g.compass)
            .putString("g.histMode", g.histogramMode.name)
            .putString("g.histSize", g.histogramSize.name)
            .putFloat("g.compassX", g.compassX)
            .putFloat("g.compassY", g.compassY)
            .putBoolean("g.peaking", g.peaking)
            .putBoolean("g.zebra", g.zebra)
            .putInt("g.zebraPct", g.zebraPercent)
            .putString("g.levelStyle", g.levelStyle.name)
            .putFloat("g.levelTol", g.levelTolerance)
            .putBoolean("g.levelPitch", g.levelPitch)
            .putBoolean("g.levelFade", g.levelFade)
            .putFloat("g.levelCalX", g.levelCalX)
            .putFloat("g.levelCalY", g.levelCalY)
            .putFloat("g.levelCalZ", g.levelCalZ)
            .putFloat("g.zoomNavX", g.zoomNavigatorX)
            .putFloat("g.zoomNavY", g.zoomNavigatorY)
            .putBoolean("g.subjectMirror", g.subjectMirror)
            .putBoolean("g.audioMeter", g.audioMeter)
            .putFloat("g.audioMeterX", g.audioMeterX)
            .putFloat("g.audioMeterY", g.audioMeterY)
            .putBoolean("g.autoCheck", g.autoCheckUpdates)
            .putBoolean("g.autoDownload", g.autoDownloadUpdates)
            .putBoolean("g.geotag", g.geotag)
            .putString("g.lastMode", g.lastMode.name)
            .putString("g.language", g.language.name)
            .apply()
    }

    fun loadAll(): Map<CaptureMode, ModeSettings> = CaptureMode.entries.associateWith { load(it) }

    fun load(mode: CaptureMode): ModeSettings = loadPrefix(mode.name + ".", ModeSettings.defaultsFor(mode))

    /** Ustawienia wspólne; przy pierwszym uruchomieniu – przejęte z trybu Foto. */
    fun loadShared(): ModeSettings =
        if (prefs.contains("SHARED.level")) loadPrefix("SHARED.", ModeSettings()) else load(CaptureMode.PHOTO).copy(custom = emptySet())

    fun saveShared(s: ModeSettings) = savePrefix("SHARED.", s)

    fun save(mode: CaptureMode, s: ModeSettings) = savePrefix(mode.name + ".", s)

    private fun loadPrefix(k: String, d: ModeSettings): ModeSettings {
        return ModeSettings(
            aspect = enumOr(prefs.getString(k + "aspect", null), d.aspect),
            grids = loadGrids(prefs.getString(k + "grids", null), prefs.getString(k + "grid", null), d.grids),
            level = prefs.getBoolean(k + "level", d.level),
            histogram = prefs.getBoolean(k + "histogram", d.histogram),
            timerSeconds = prefs.getInt(k + "timer", d.timerSeconds),
            flash = enumOr(prefs.getString(k + "flash", null), d.flash),
            photoFormat = enumOr(prefs.getString(k + "format", null), d.photoFormat),
            jpegQuality = prefs.getInt(k + "jpeg", d.jpegQuality),
            priority = enumOr(prefs.getString(k + "priority", null), d.priority),
            mirrorFront = prefs.getBoolean(k + "mirror", d.mirrorFront),
            burst = prefs.getBoolean(k + "burst", d.burst),
            intervalSeconds = prefs.getInt(k + "interval", d.intervalSeconds),
            intervalCount = prefs.getInt(k + "intervalCount", d.intervalCount),
            nightFrames = prefs.getInt(k + "nightFrames", d.nightFrames),
            lowLightBoost = prefs.getBoolean(k + "lowLight", d.lowLightBoost),
            portraitEffect = enumOr(prefs.getString(k + "portrait", null), d.portraitEffect),
            portraitBlur = prefs.getInt(k + "portraitBlur", d.portraitBlur),
            hdrStops = prefs.getFloat(k + "hdrStops", d.hdrStops),
            longSeconds = prefs.getInt(k + "longSec", d.longSeconds),
            longBlend = enumOr(prefs.getString(k + "longBlend", null), d.longBlend),
            longAlign = prefs.getBoolean(k + "longAlign", d.longAlign),
            videoResolution = enumOr(prefs.getString(k + "videoRes", null), d.videoResolution),
            videoFps = enumOr(prefs.getString(k + "fps", null), d.videoFps),
            videoHdr = prefs.getBoolean(k + "videoHdr", d.videoHdr),
            stabilization = prefs.getBoolean(k + "stab", d.stabilization),
            videoAudio = prefs.getBoolean(k + "videoAudio", d.videoAudio),
            bitrate = enumOr(prefs.getString(k + "bitrate", null), d.bitrate),
            cinematicZoom = prefs.getBoolean(k + "cineZoom", d.cinematicZoom),
            slowMoResolution = enumOr(prefs.getString(k + "slowRes", null), d.slowMoResolution),
            slowMoFps = prefs.getInt(k + "slowFps", d.slowMoFps),
            proVideo = prefs.getBoolean(k + "proVideo", d.proVideo),
            bracketCount = prefs.getInt(k + "bracketCount", d.bracketCount),
            bracketStep = prefs.getFloat(k + "bracketStep", d.bracketStep),
            flatProfile = prefs.getBoolean(k + "flatProfile", d.flatProfile),
            tlSpeed = prefs.getInt(k + "tlSpeed", d.tlSpeed),
            tlFps = prefs.getInt(k + "tlFps", d.tlFps),
            tlResolution = enumOr(prefs.getString(k + "tlRes", null), d.tlResolution),
            tlStopMin = prefs.getInt(k + "tlStop", d.tlStopMin),
            tlDeflicker = prefs.getBoolean(k + "tlDeflicker", d.tlDeflicker),
            tlLock = prefs.getBoolean(k + "tlLock", d.tlLock),
            audioPreset = enumOr(prefs.getString(k + "audioPreset", null), d.audioPreset),
            audioSource = enumOr(prefs.getString(k + "audioSource", null), d.audioSource),
            audioInput = enumOr(prefs.getString(k + "audioInput", null), d.audioInput),
            audioGainDb = prefs.getInt(k + "audioGain", d.audioGainDb),
            audioNoiseSuppression = prefs.getBoolean(k + "audioNs", d.audioNoiseSuppression),
            audioAgc = prefs.getBoolean(k + "audioAgc", d.audioAgc),
            audioSkipSilence = prefs.getBoolean(k + "audioSkipSilence", d.audioSkipSilence),
            audioAutoStopMin = prefs.getInt(k + "audioAutoStop", d.audioAutoStopMin),
            custom = prefs.getString(k + "custom", null)
                ?.split(',')?.mapNotNull { n -> SharedGroup.entries.firstOrNull { it.name == n } }?.toSet()
                ?: d.custom,
        )
    }

    private fun savePrefix(k: String, s: ModeSettings) {
        prefs.edit()
            .putString(k + "aspect", s.aspect.name)
            .putString(k + "grids", s.grids.joinToString(",") { it.name })
            .putBoolean(k + "level", s.level)
            .putBoolean(k + "histogram", s.histogram)
            .putInt(k + "timer", s.timerSeconds)
            .putString(k + "flash", s.flash.name)
            .putString(k + "format", s.photoFormat.name)
            .putInt(k + "jpeg", s.jpegQuality)
            .putString(k + "priority", s.priority.name)
            .putBoolean(k + "mirror", s.mirrorFront)
            .putBoolean(k + "burst", s.burst)
            .putInt(k + "interval", s.intervalSeconds)
            .putInt(k + "intervalCount", s.intervalCount)
            .putInt(k + "nightFrames", s.nightFrames)
            .putBoolean(k + "lowLight", s.lowLightBoost)
            .putString(k + "portrait", s.portraitEffect.name)
            .putInt(k + "portraitBlur", s.portraitBlur)
            .putFloat(k + "hdrStops", s.hdrStops)
            .putInt(k + "longSec", s.longSeconds)
            .putString(k + "longBlend", s.longBlend.name)
            .putBoolean(k + "longAlign", s.longAlign)
            .putString(k + "videoRes", s.videoResolution.name)
            .putString(k + "fps", s.videoFps.name)
            .putBoolean(k + "videoHdr", s.videoHdr)
            .putBoolean(k + "stab", s.stabilization)
            .putBoolean(k + "videoAudio", s.videoAudio)
            .putString(k + "bitrate", s.bitrate.name)
            .putBoolean(k + "cineZoom", s.cinematicZoom)
            .putString(k + "slowRes", s.slowMoResolution.name)
            .putInt(k + "slowFps", s.slowMoFps)
            .putBoolean(k + "proVideo", s.proVideo)
            .putInt(k + "bracketCount", s.bracketCount)
            .putFloat(k + "bracketStep", s.bracketStep)
            .putBoolean(k + "flatProfile", s.flatProfile)
            .putInt(k + "tlSpeed", s.tlSpeed)
            .putInt(k + "tlFps", s.tlFps)
            .putString(k + "tlRes", s.tlResolution.name)
            .putInt(k + "tlStop", s.tlStopMin)
            .putBoolean(k + "tlDeflicker", s.tlDeflicker)
            .putBoolean(k + "tlLock", s.tlLock)
            .putString(k + "audioPreset", s.audioPreset.name)
            .putString(k + "audioSource", s.audioSource.name)
            .putString(k + "audioInput", s.audioInput.name)
            .putInt(k + "audioGain", s.audioGainDb)
            .putBoolean(k + "audioNs", s.audioNoiseSuppression)
            .putBoolean(k + "audioAgc", s.audioAgc)
            .putBoolean(k + "audioSkipSilence", s.audioSkipSilence)
            .putInt(k + "audioAutoStop", s.audioAutoStopMin)
            .putString(k + "custom", s.custom.joinToString(",") { it.name })
            .apply()
    }

    fun getLong(key: String) = prefs.getLong(key, 0L)
    fun putLong(key: String, value: Long) = prefs.edit().putLong(key, value).apply()

    /** Nowy zapis „THIRDS,GOLDEN”; starsza wersja trzymała jedną siatkę pod kluczem „grid”. */
    private fun loadGrids(list: String?, legacy: String?, default: Set<GridType>): Set<GridType> = when {
        list != null -> list.split(',').mapNotNull { n -> GridType.entries.firstOrNull { it.name == n } }.toSet()
        legacy == "NONE" -> emptySet()
        legacy != null -> GridType.entries.firstOrNull { it.name == legacy }?.let { setOf(it) } ?: default
        else -> default
    }

    private inline fun <reified T : Enum<T>> enumOr(name: String?, default: T): T =
        name?.let { n -> enumValues<T>().firstOrNull { it.name == n } } ?: default
}
