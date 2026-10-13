package com.blacksingularities.registrer.data

import android.content.Context
import android.content.SharedPreferences
import com.blacksingularities.registrer.camera.CaptureMode
import com.blacksingularities.registrer.camera.FlashSetting

/** Proporcje kadru; [ratio] to szerokość / wysokość w orientacji pionowej. */
enum class AspectRatioMode(val label: String, val ratio: Float) {
    R4_3("4:3", 3f / 4f),
    R16_9("16:9", 9f / 16f),
}

/** Siatki kompozycyjne – można włączyć dowolny zestaw naraz. */
enum class GridType(val label: String) {
    THIRDS("3×3"),
    GOLDEN("Złoty podział"),
    SQUARE("Kwadrat"),
    DIAGONAL("Przekątne"),
}

enum class PhotoFormat(val label: String) {
    JPEG("JPEG"),
    ULTRA_HDR("Ultra HDR"),
    RAW_JPEG("RAW+JPEG"),
    RAW("RAW"),
}

enum class CapturePriority(val label: String) {
    SPEED("Szybkość"),
    BALANCED("Balans"),
    QUALITY("Jakość"),
}

enum class VideoResolution(val label: String) {
    UHD("4K"),
    FHD("1080p"),
    HD("720p"),
}

enum class VideoFps(val label: String, val fps: Int) {
    AUTO("Auto", 0),
    F24("24", 24),
    F30("30", 30),
    F60("60", 60),
}

enum class VideoBitrate(val label: String) {
    AUTO("Auto"),
    HIGH("Wysoki"),
    MAX("Maks."),
}

enum class PortraitEffect(val label: String) {
    BOKEH("Bokeh"),
    RETOUCH("Retusz"),
}

enum class VolumeAction(val label: String) {
    SHUTTER("Migawka"),
    ZOOM("Zoom"),
    NONE("Głośność"),
}

enum class ScreenRotation(val label: String, val description: String) {
    FIXED("Stały", "Ekran stoi, obracają się ikony i informacje"),
    AUTO("Obracaj", "Cały interfejs obraca się z telefonem, także przy systemowej blokadzie obrotu"),
}

enum class AudioPreset(
    val label: String,
    val description: String,
    val sampleRate: Int,
    val bitRate: Int,
    val channels: Int,
) {
    VOICE("Mowa", "AAC 96 kb/s · mono · 44,1 kHz", 44_100, 96_000, 1),
    MUSIC("Muzyka", "AAC 256 kb/s · stereo · 48 kHz", 48_000, 256_000, 2),
    STUDIO("Studio", "AAC 320 kb/s · stereo · 48 kHz", 48_000, 320_000, 2),
}

enum class AudioSourceType(val label: String, val description: String) {
    STANDARD("Standard", "Domyślne przetwarzanie mikrofonu"),
    RAW("Surowy", "Bez filtrów – najwierniejszy dźwięk"),
    VOICE("Głos", "Redukcja szumów i echa"),
    CAMCORDER("Kierunkowy", "Mikrofon skierowany jak w kamerze"),
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
    // wideo
    val videoResolution: VideoResolution = VideoResolution.FHD,
    val videoFps: VideoFps = VideoFps.AUTO,
    val videoHdr: Boolean = false,
    val stabilization: Boolean = true,
    val videoAudio: Boolean = true,
    val bitrate: VideoBitrate = VideoBitrate.AUTO,
    val cinematicZoom: Boolean = true,
    val slowMoFps: Int = 0, // 0 = najwyższy dostępny
    // audio
    val audioPreset: AudioPreset = AudioPreset.MUSIC,
    val audioSource: AudioSourceType = AudioSourceType.STANDARD,
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
        slowMoFps = slowMoFps,
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
            CaptureMode.PHOTO -> ModeSettings()
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
    val compassX: Float = 0.5f,
    val compassY: Float = 0.1f,
    val autoCheckUpdates: Boolean = true,
    val autoDownloadUpdates: Boolean = true,
    val lastMode: CaptureMode = CaptureMode.PHOTO,
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
            compassX = prefs.getFloat("g.compassX", d.compassX),
            compassY = prefs.getFloat("g.compassY", d.compassY),
            autoCheckUpdates = prefs.getBoolean("g.autoCheck", d.autoCheckUpdates),
            autoDownloadUpdates = prefs.getBoolean("g.autoDownload", d.autoDownloadUpdates),
            lastMode = enumOr(prefs.getString("g.lastMode", null), d.lastMode),
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
            .putFloat("g.compassX", g.compassX)
            .putFloat("g.compassY", g.compassY)
            .putBoolean("g.autoCheck", g.autoCheckUpdates)
            .putBoolean("g.autoDownload", g.autoDownloadUpdates)
            .putString("g.lastMode", g.lastMode.name)
            .apply()
    }

    fun loadAll(): Map<CaptureMode, ModeSettings> = CaptureMode.entries.associateWith { load(it) }

    fun load(mode: CaptureMode): ModeSettings {
        val d = ModeSettings.defaultsFor(mode)
        val k = mode.name + "."
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
            videoResolution = enumOr(prefs.getString(k + "videoRes", null), d.videoResolution),
            videoFps = enumOr(prefs.getString(k + "fps", null), d.videoFps),
            videoHdr = prefs.getBoolean(k + "videoHdr", d.videoHdr),
            stabilization = prefs.getBoolean(k + "stab", d.stabilization),
            videoAudio = prefs.getBoolean(k + "videoAudio", d.videoAudio),
            bitrate = enumOr(prefs.getString(k + "bitrate", null), d.bitrate),
            cinematicZoom = prefs.getBoolean(k + "cineZoom", d.cinematicZoom),
            slowMoFps = prefs.getInt(k + "slowFps", d.slowMoFps),
            audioPreset = enumOr(prefs.getString(k + "audioPreset", null), d.audioPreset),
            audioSource = enumOr(prefs.getString(k + "audioSource", null), d.audioSource),
        )
    }

    fun save(mode: CaptureMode, s: ModeSettings) {
        val k = mode.name + "."
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
            .putString(k + "videoRes", s.videoResolution.name)
            .putString(k + "fps", s.videoFps.name)
            .putBoolean(k + "videoHdr", s.videoHdr)
            .putBoolean(k + "stab", s.stabilization)
            .putBoolean(k + "videoAudio", s.videoAudio)
            .putString(k + "bitrate", s.bitrate.name)
            .putBoolean(k + "cineZoom", s.cinematicZoom)
            .putInt(k + "slowFps", s.slowMoFps)
            .putString(k + "audioPreset", s.audioPreset.name)
            .putString(k + "audioSource", s.audioSource.name)
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
