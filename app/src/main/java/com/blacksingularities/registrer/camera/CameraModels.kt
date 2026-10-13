package com.blacksingularities.registrer.camera

import android.hardware.camera2.CameraMetadata
import android.util.Range
import com.blacksingularities.registrer.data.ModeSettings
import com.blacksingularities.registrer.data.PhotoFormat
import com.blacksingularities.registrer.data.VideoResolution

enum class CaptureMode(val label: String, val isPhoto: Boolean, val isVideo: Boolean) {
    HDR("HDR", true, false),
    PORTRAIT("Portret", true, false),
    NIGHT("Noc", true, false),
    PHOTO("Foto", true, false),
    VIDEO("Wideo", false, true),
    SLOWMO("Slow-mo", false, true),
    /** Klip HEVC: każde naciśnięcie migawki zapisuje ~3 s film (1,5 s przed i 1,5 s po). */
    CLIP("Klip", false, false),
    /** Odpowiednik Live Photo: zdjęcie + ~3 s ruchu wokół naciśnięcia migawki. */
    MOTION("Live", true, false),
    AUDIO("Audio", false, false),
    /** Ręczne ISO, czas, ostrość i balans bieli dla fotografii. */
    PRO("Pro Foto", true, false),
}

enum class FlashSetting { OFF, AUTO, ON, TORCH }

enum class WhiteBalance(val label: String, val awbMode: Int) {
    AUTO("Auto", CameraMetadata.CONTROL_AWB_MODE_AUTO),
    DAYLIGHT("Słońce", CameraMetadata.CONTROL_AWB_MODE_DAYLIGHT),
    CLOUDY("Chmury", CameraMetadata.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT),
    SHADE("Cień", CameraMetadata.CONTROL_AWB_MODE_SHADE),
    TWILIGHT("Zmierzch", CameraMetadata.CONTROL_AWB_MODE_TWILIGHT),
    INCANDESCENT("Żarówka", CameraMetadata.CONTROL_AWB_MODE_INCANDESCENT),
    FLUORESCENT("Świetlówka", CameraMetadata.CONTROL_AWB_MODE_FLUORESCENT),
}

/** Ręczne parametry trybu Pro; `null` oznacza automat. */
data class ProSettings(
    val iso: Int? = null,
    val exposureNs: Long? = null,
    val focusDiopters: Float? = null,
    val whiteBalance: WhiteBalance = WhiteBalance.AUTO,
) {
    val manualExposure get() = iso != null || exposureNs != null
}

/**
 * Możliwości bieżącego obiektywu odczytane przed związaniem kamery – na ich podstawie
 * interfejs pokazuje wyłącznie opcje, które urządzenie naprawdę obsługuje.
 */
data class CameraCaps(
    val hasFront: Boolean = false,
    val hasBack: Boolean = true,
    val nightExtension: Boolean = false,
    val bokehExtension: Boolean = false,
    val hdrExtension: Boolean = false,
    val retouchExtension: Boolean = false,
    val photoFormats: Set<PhotoFormat> = setOf(PhotoFormat.JPEG),
    val zsl: Boolean = false,
    val manualSensor: Boolean = false,
    val isoRange: IntRange? = null,
    val exposureRange: LongRange? = null,
    val minFocusDistance: Float = 0f,
    val hasFlash: Boolean = false,
    val videoResolutions: List<VideoResolution> = listOf(VideoResolution.FHD, VideoResolution.HD),
    val videoHlg: Boolean = false,
    val videoStabilization: Boolean = false,
    val fps60: Boolean = false,
    val fps24: Boolean = false,
    val slowMoRanges: List<Range<Int>> = emptyList(),
    /** Wszystkie kombinacje rozdzielczości i klatkażu szybkiego nagrywania głównego aparatu. */
    val slowMoProfiles: List<SlowMoProfile> = emptyList(),
    val lowLightBoost: Boolean = false,
    /** Krotności odpowiadające fizycznym obiektywom (np. 0.6 szeroki kąt, 1, 3 tele). */
    val lensZooms: List<Float> = emptyList(),
    /** Górna granica zoomu głównego (logicznego) aparatu. */
    val maxZoom: Float = 1f,
    /** Osobne aparaty (np. szeroki kąt z własnym id), do których przełączamy się przyciskiem. */
    val auxLenses: List<AuxLens> = emptyList(),
    /** Opis wszystkich wykrytych aparatów – do diagnostyki w ustawieniach. */
    val cameraSummary: String = "",
    /** Alternatywny sensor, który CameraX udostępnia w RAW, gdy aparat logiczny pokazuje tylko JPEG. */
    val rawCameraId: String? = null,
    val rawCameraPhysical: Boolean = false,
    val rawCameraFactor: Float = 1f,
) {
    val slowMo get() = slowMoRanges.isNotEmpty()
    val portrait get() = bokehExtension || retouchExtension
}

data class SlowMoProfile(
    val resolution: VideoResolution,
    val frameRate: Range<Int>,
) {
    val fps: Int get() = frameRate.upper
    val slowdown: Int get() = (fps / 30).coerceAtLeast(1)
}

/**
 * Osobny aparat po tej samej stronie; [factor] to krotność względem 1× głównego aparatu.
 * [physical] – sensor wewnątrz aparatu logicznego, otwierany przez setPhysicalCameraId.
 */
data class AuxLens(val cameraId: String, val factor: Float, val physical: Boolean = false)

/** Stan rzeczywiście aktywny po związaniu (może być skromniejszy niż żądany). */
data class ActiveSession(
    val extension: Int? = null,
    val videoHdr: Boolean = false,
    val stabilization: Boolean = false,
    val fps: Int = 0,
    val photoFormat: PhotoFormat = PhotoFormat.JPEG,
    val snapshotInVideo: Boolean = false,
    val postview: Boolean = false,
    /** Rzeczywista rozdzielczość zapisywanego materiału (zdjęcia albo wideo), jeśli znana. */
    val outputSize: android.util.Size? = null,
)

/** Bieżące parametry ekspozycji odczytane z sensora (HUD). */
data class Hud(val iso: Int, val exposureNs: Long, val focusDiopters: Float?, val aperture: Float? = null)

data class BindRequest(
    val lensFacing: Int,
    val mode: CaptureMode,
    val settings: ModeSettings,
    val pro: ProSettings,
    /** Id osobnego aparatu (szeroki kąt / tele) albo null dla głównego. */
    val cameraId: String? = null,
)

class CapturedFrame(val bytes: ByteArray, val rotation: Int)

/** Chwile nagrywane z bufora w tle. */
val CaptureMode.isMoment: Boolean get() = this == CaptureMode.MOTION || this == CaptureMode.CLIP
