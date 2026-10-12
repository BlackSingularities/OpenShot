package com.blacksingularities.registrer.camera

import android.hardware.camera2.CameraMetadata
import com.blacksingularities.registrer.data.AppSettings

enum class CaptureMode(val label: String, val isPhoto: Boolean) {
    NIGHT("Noc", true),
    PORTRAIT("Portret", true),
    PHOTO("Foto", true),
    HDR("HDR", true),
    PRO("Pro", true),
    VIDEO("Wideo", false),
    AUDIO("Audio", false),
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

data class CameraCaps(
    val hasFront: Boolean = false,
    val hasBack: Boolean = true,
    val nightExtension: Boolean = false,
    val bokehExtension: Boolean = false,
    val hdrExtension: Boolean = false,
    val manualSensor: Boolean = false,
    val isoRange: IntRange? = null,
    val exposureRange: LongRange? = null,
    val minFocusDistance: Float = 0f,
    val hasFlash: Boolean = false,
    val videoHdrActive: Boolean = false,
    val stabilizationActive: Boolean = false,
)

/** Bieżące parametry ekspozycji odczytane z sensora (HUD). */
data class Hud(val iso: Int, val exposureNs: Long, val focusDiopters: Float?)

data class BindRequest(
    val lensFacing: Int,
    val mode: CaptureMode,
    val settings: AppSettings,
    val pro: ProSettings,
    val flash: FlashSetting,
)

class CapturedFrame(val bytes: ByteArray, val rotation: Int)
