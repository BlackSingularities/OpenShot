package com.blacksingularities.registrer.data

import android.content.Context

/** Proporcje kadru; [ratio] to szerokość / wysokość w orientacji pionowej. */
enum class AspectRatioMode(val label: String, val ratio: Float) {
    R4_3("4:3", 3f / 4f),
    R16_9("16:9", 9f / 16f),
}

enum class GridType(val label: String) {
    NONE("Brak"),
    THIRDS("Trójpodział"),
    GOLDEN("Złoty"),
    SQUARE("Kwadrat"),
    DIAGONAL("Przekątne"),
}

enum class VideoResolution(val label: String) {
    UHD("4K"),
    FHD("1080p"),
    HD("720p"),
}

enum class VolumeAction(val label: String) {
    SHUTTER("Migawka"),
    ZOOM("Zoom"),
    NONE("Głośność"),
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

data class AppSettings(
    val aspect: AspectRatioMode = AspectRatioMode.R4_3,
    val grid: GridType = GridType.THIRDS,
    val timerSeconds: Int = 0,
    val jpegQuality: Int = 95,
    val maxQuality: Boolean = true,
    val nightFrames: Int = 8,
    val level: Boolean = true,
    val histogram: Boolean = true,
    val videoResolution: VideoResolution = VideoResolution.FHD,
    val videoHdr: Boolean = false,
    val stabilization: Boolean = true,
    val videoAudio: Boolean = true,
    val cinematicZoom: Boolean = true,
    val audioPreset: AudioPreset = AudioPreset.MUSIC,
    val audioSource: AudioSourceType = AudioSourceType.STANDARD,
    val shutterSound: Boolean = true,
    val haptics: Boolean = true,
    val volumeAction: VolumeAction = VolumeAction.SHUTTER,
)

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("registrer_settings", Context.MODE_PRIVATE)

    fun load(): AppSettings {
        val d = AppSettings()
        return AppSettings(
            aspect = enumOr(prefs.getString("aspect", null), d.aspect),
            grid = enumOr(prefs.getString("grid", null), d.grid),
            timerSeconds = prefs.getInt("timer", d.timerSeconds),
            jpegQuality = prefs.getInt("jpeg", d.jpegQuality),
            maxQuality = prefs.getBoolean("maxQuality", d.maxQuality),
            nightFrames = prefs.getInt("nightFrames", d.nightFrames),
            level = prefs.getBoolean("level", d.level),
            histogram = prefs.getBoolean("histogram", d.histogram),
            videoResolution = enumOr(prefs.getString("videoRes", null), d.videoResolution),
            videoHdr = prefs.getBoolean("videoHdr", d.videoHdr),
            stabilization = prefs.getBoolean("stab", d.stabilization),
            videoAudio = prefs.getBoolean("videoAudio", d.videoAudio),
            cinematicZoom = prefs.getBoolean("cineZoom", d.cinematicZoom),
            audioPreset = enumOr(prefs.getString("audioPreset", null), d.audioPreset),
            audioSource = enumOr(prefs.getString("audioSource", null), d.audioSource),
            shutterSound = prefs.getBoolean("shutterSound", d.shutterSound),
            haptics = prefs.getBoolean("haptics", d.haptics),
            volumeAction = enumOr(prefs.getString("volume", null), d.volumeAction),
        )
    }

    fun save(s: AppSettings) {
        prefs.edit()
            .putString("aspect", s.aspect.name)
            .putString("grid", s.grid.name)
            .putInt("timer", s.timerSeconds)
            .putInt("jpeg", s.jpegQuality)
            .putBoolean("maxQuality", s.maxQuality)
            .putInt("nightFrames", s.nightFrames)
            .putBoolean("level", s.level)
            .putBoolean("histogram", s.histogram)
            .putString("videoRes", s.videoResolution.name)
            .putBoolean("videoHdr", s.videoHdr)
            .putBoolean("stab", s.stabilization)
            .putBoolean("videoAudio", s.videoAudio)
            .putBoolean("cineZoom", s.cinematicZoom)
            .putString("audioPreset", s.audioPreset.name)
            .putString("audioSource", s.audioSource.name)
            .putBoolean("shutterSound", s.shutterSound)
            .putBoolean("haptics", s.haptics)
            .putString("volume", s.volumeAction.name)
            .apply()
    }

    private inline fun <reified T : Enum<T>> enumOr(name: String?, default: T): T =
        name?.let { n -> enumValues<T>().firstOrNull { it.name == n } } ?: default
}
