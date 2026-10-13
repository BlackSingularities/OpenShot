package com.blacksingularities.registrer.camera

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.net.Uri
import android.os.Build
import android.util.Range
import android.util.Size
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.DynamicRange
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.MeteringPoint
import androidx.camera.core.MirrorMode
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.extensions.ExtensionMode
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.HighSpeedVideoSessionConfig
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.concurrent.futures.await
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.blacksingularities.registrer.data.AspectRatioMode
import com.blacksingularities.registrer.data.CapturePriority
import com.blacksingularities.registrer.data.PhotoFormat
import com.blacksingularities.registrer.data.PortraitEffect
import com.blacksingularities.registrer.data.VideoBitrate
import com.blacksingularities.registrer.data.VideoFps
import com.blacksingularities.registrer.data.VideoResolution
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** Wynik zapisu zdjęcia: Uri pliku głównego (JPEG lub DNG) i opcjonalnie drugiego (RAW+JPEG). */
data class PhotoResult(val uri: Uri?, val secondary: Uri? = null, val error: String? = null)

/**
 * Warstwa sprzętowa: sonduje możliwości obiektywu, wiąże przypadki użycia CameraX
 * (zdjęcia JPEG / Ultra HDR / RAW, wideo, slow-motion), obsługuje rozszerzenia producenta
 * i ręczne sterowanie sensorem przez Camera2 Interop.
 */
@OptIn(ExperimentalCamera2Interop::class)
class CameraEngine(private val context: Context) {
    private val main = ContextCompat.getMainExecutor(context)
    private val analysisExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    private var provider: ProcessCameraProvider? = null
    private var extensions: ExtensionsManager? = null

    var camera: Camera? = null
    private var preview: Preview? = null
    private var displayRotation: Int = android.view.Surface.ROTATION_0
        private set
    var session = ActiveSession()
        private set
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private var lastHud: Hud? = null
    private var hudFrame = 0
    private var baseOptions: CaptureRequestOptions? = null

    var onHud: ((Hud) -> Unit)? = null
    var onHistogram: ((IntArray) -> Unit)? = null

    val isRecording get() = recording != null

    private val hudCallback = object : CameraCaptureSession.CaptureCallback() {
        override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {
            if (hudFrame++ % 6 != 0) return
            val iso = result.get(CaptureResult.SENSOR_SENSITIVITY) ?: return
            val exposure = result.get(CaptureResult.SENSOR_EXPOSURE_TIME) ?: return
            val hud = Hud(iso, exposure, result.get(CaptureResult.LENS_FOCUS_DISTANCE), result.get(CaptureResult.LENS_APERTURE))
            lastHud = hud
            main.execute { onHud?.invoke(hud) }
        }
    }

    private suspend fun ensureProvider(): ProcessCameraProvider {
        provider?.let { return it }
        val p = ProcessCameraProvider.getInstance(context).await()
        provider = p
        extensions = runCatching { ExtensionsManager.getInstanceAsync(context, p).await() }.getOrNull()
        return p
    }

    private fun hasLens(p: ProcessCameraProvider, facing: Int) = runCatching {
        p.hasCamera(CameraSelector.Builder().requireLensFacing(facing).build())
    }.getOrDefault(false)

    private fun cameraIdOf(info: CameraInfo): String? = runCatching { Camera2CameraInfo.from(info).cameraId }.getOrNull()

    private fun selectorFor(facing: Int, cameraId: String? = null, physical: Boolean = false): CameraSelector {
        if (cameraId != null && physical) {
            return CameraSelector.Builder().requireLensFacing(facing).setPhysicalCameraId(cameraId).build()
        }
        if (cameraId != null) {
            val match = runCatching { provider?.availableCameraInfos }.getOrNull()?.firstOrNull { cameraIdOf(it) == cameraId }
            if (match != null) return match.cameraSelector
        }
        return CameraSelector.Builder().requireLensFacing(facing).build()
    }

    private fun zoomRange(info: CameraInfo): Pair<Float, Float> {
        val range = if (Build.VERSION.SDK_INT >= 30) {
            runCatching { Camera2CameraInfo.from(info).getCameraCharacteristic(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE) }.getOrNull()
        } else {
            null
        }
        val zoom = info.zoomState.value
        return (range?.lower ?: zoom?.minZoomRatio ?: 1f) to (range?.upper ?: zoom?.maxZoomRatio ?: 1f)
    }

    private fun roundLens(ratio: Float): Float =
        if (ratio >= 1.5f && abs(ratio - ratio.roundToInt()) < 0.2f) ratio.roundToInt().toFloat() else (ratio * 10f).roundToInt() / 10f

    private fun photoFormatsOf(info: CameraInfo): Set<PhotoFormat> {
        val formats = runCatching {
            ImageCapture.getImageCaptureCapabilities(info).supportedOutputFormats
        }.getOrDefault(emptySet())
        return buildSet {
            add(PhotoFormat.JPEG)
            if (ImageCapture.OUTPUT_FORMAT_JPEG_ULTRA_HDR in formats) add(PhotoFormat.ULTRA_HDR)
            if (ImageCapture.OUTPUT_FORMAT_RAW in formats) add(PhotoFormat.RAW)
            if (ImageCapture.OUTPUT_FORMAT_RAW_JPEG in formats) add(PhotoFormat.RAW_JPEG)
        }
    }

    private fun outputFormatOf(format: PhotoFormat): Int = when (format) {
        PhotoFormat.JPEG -> ImageCapture.OUTPUT_FORMAT_JPEG
        PhotoFormat.ULTRA_HDR -> ImageCapture.OUTPUT_FORMAT_JPEG_ULTRA_HDR
        PhotoFormat.RAW -> ImageCapture.OUTPUT_FORMAT_RAW
        PhotoFormat.RAW_JPEG -> ImageCapture.OUTPUT_FORMAT_RAW_JPEG
    }

    /**
     * Wszystkie aparaty po danej stronie. Wielu producentów udostępnia szeroki kąt (i tele) jako
     * osobny aparat zamiast zoomu < 1× aparatu głównego – takie zwracamy jako [AuxLens].
     */
    private fun auxLenses(p: ProcessCameraProvider, primary: CameraInfo, facing: Int): Pair<List<AuxLens>, String> {
        val primaryId = cameraIdOf(primary)
        val primaryFocal = equivalentFocal(primary)
        val physicalIds = runCatching { primary.physicalCameraInfos.mapNotNull { cameraIdOf(it) } }.getOrDefault(emptyList()).toSet()
        val all = runCatching { p.availableCameraInfos }.getOrDefault(emptyList())
            .filter { runCatching { it.lensFacing }.getOrNull() == facing }
        val aux = mutableListOf<AuxLens>()
        val summary = StringBuilder()
        for (o in all) {
            val id = cameraIdOf(o) ?: continue
            val c2 = Camera2CameraInfo.from(o)
            val focal = equivalentFocal(o)
            val pixels = runCatching { c2.getCameraCharacteristic(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE) }.getOrNull()
            val mp = pixels?.let { it.width.toFloat() * it.height / 1_000_000f } ?: 0f
            val logical = runCatching {
                c2.getCameraCharacteristic(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
                    ?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA) == true
            }.getOrDefault(false)
            val (minZ, _) = zoomRange(o)
            if (summary.isNotEmpty()) summary.append("\n")
            summary.append("#").append(id).append(": ")
            summary.append(focal?.let { "${it.roundToInt()} mm" } ?: "? mm")
            summary.append(" · ").append(mp.roundToInt()).append(" MP")
            summary.append(" · od ").append(String.format(java.util.Locale.US, "%.2f", minZ)).append("×")
            if (logical) summary.append(" · logiczny")
            if (id == primaryId) summary.append(" · główny")

            if (id == primaryId || id in physicalIds || focal == null || primaryFocal == null) continue
            if (mp in 0.1f..4.9f) continue // makro / głębia
            val factor = roundLens(focal / primaryFocal)
            if (factor in 0.86f..1.3f) continue
            if (aux.any { abs(it.factor - factor) / factor < 0.1f }) continue
            aux += AuxLens(id, factor)
        }
        // Sensory fizyczne wewnątrz aparatu logicznego (np. szeroki kąt bez zoomu < 1×).
        val logicalSupported = runCatching { primary.isLogicalMultiCameraSupported }.getOrDefault(false)
        if (logicalSupported && primaryFocal != null) {
            for (phys in runCatching { primary.physicalCameraInfos }.getOrDefault(emptySet())) {
                val id = cameraIdOf(phys) ?: continue
                val focal = equivalentFocal(phys) ?: continue
                summary.append("\n  └ fizyczny #").append(id).append(": ").append(focal.roundToInt()).append(" mm")
                val factor = roundLens(focal / primaryFocal)
                if (factor in 0.86f..1.3f) continue
                // Rejestrujemy zawsze – nawet gdy opis aparatu deklaruje taki zoom, po związaniu
                // bywa niedostępny i wtedy jedyną drogą jest bezpośredni sensor.
                if (aux.any { abs(it.factor - factor) / factor < 0.1f }) continue
                aux += AuxLens(id, factor, physical = true)
            }
        }
        // Wszystkie identyfikatory zgłaszane przez system – także te, których CameraX nie widzi.
        runCatching {
            val cm = context.getSystemService(android.hardware.camera2.CameraManager::class.java)
            val listed = cm.cameraIdList.toSet()
            val known = all.mapNotNull { cameraIdOf(it) }.toSet()
            summary.append("\nCamera2: ").append(listed.sorted().joinToString(", ") { "#$it" })
            val hidden = (0..15).map { it.toString() }.filter { it !in listed }.filter { id ->
                runCatching { cm.getCameraCharacteristics(id) }.isSuccess
            }
            if (hidden.isNotEmpty()) summary.append(" · ukryte: ").append(hidden.joinToString(", ") { "#$it" })
            val unusable = listed - known
            if (unusable.isNotEmpty()) summary.append(" · niedostępne w CameraX: ").append(unusable.joinToString(", ") { "#$it" })
        }
        return aux.sortedBy { it.factor } to summary.toString()
    }

    // ------------------------------------------------------------------ sondowanie

    /** Rozstrzyga, który obiektyw jest faktycznie dostępny. */
    suspend fun resolveFacing(requested: Int): Int {
        val p = ensureProvider()
        return when {
            requested == CameraSelector.LENS_FACING_FRONT && hasLens(p, CameraSelector.LENS_FACING_FRONT) -> requested
            hasLens(p, CameraSelector.LENS_FACING_BACK) -> CameraSelector.LENS_FACING_BACK
            else -> CameraSelector.LENS_FACING_FRONT
        }
    }

    suspend fun probe(facing: Int): CameraCaps {
        val p = ensureProvider()
        val base = selectorFor(facing)
        val info = runCatching { p.getCameraInfo(base) }.getOrNull()
            ?: return CameraCaps(hasFront = hasLens(p, CameraSelector.LENS_FACING_FRONT), hasBack = hasLens(p, CameraSelector.LENS_FACING_BACK))
        val ext = extensions
        fun extAvailable(mode: Int) = ext != null && runCatching { ext.isExtensionAvailable(base, mode) }.getOrDefault(false)

        val c2 = Camera2CameraInfo.from(info)
        fun <T> ch(key: CameraCharacteristics.Key<T>): T? = runCatching { c2.getCameraCharacteristic(key) }.getOrNull()

        val formats = runCatching { ImageCapture.getImageCaptureCapabilities(info).supportedOutputFormats }.getOrDefault(emptySet())
        val primaryFormats = photoFormatsOf(info)

        // Część telefonów zgłasza RAW dopiero dla fizycznego sensora aparatu logicznego albo
        // dla osobnego CameraId. Szukamy takiej trasy i preferujemy sensor o ogniskowej 1×.
        data class RawRoute(val id: String, val physical: Boolean, val factor: Float, val formats: Set<PhotoFormat>)
        val primaryId = cameraIdOf(info)
        val primaryFocal = equivalentFocal(info)
        val physicalIds = runCatching { info.physicalCameraInfos.mapNotNull { cameraIdOf(it) } }.getOrDefault(emptyList()).toSet()
        val rawRoutes = mutableListOf<RawRoute>()
        runCatching { info.physicalCameraInfos }.getOrDefault(emptySet()).forEach { candidate ->
            val id = cameraIdOf(candidate) ?: return@forEach
            val candidateFormats = photoFormatsOf(candidate)
            if (PhotoFormat.RAW !in candidateFormats) return@forEach
            val factor = if (primaryFocal != null) equivalentFocal(candidate)?.div(primaryFocal) ?: 1f else 1f
            rawRoutes += RawRoute(id, true, roundLens(factor), candidateFormats)
        }
        runCatching { p.availableCameraInfos }.getOrDefault(emptyList()).forEach { candidate ->
            val id = cameraIdOf(candidate) ?: return@forEach
            if (id == primaryId || id in physicalIds || runCatching { candidate.lensFacing }.getOrNull() != facing) return@forEach
            val candidateFormats = photoFormatsOf(candidate)
            if (PhotoFormat.RAW !in candidateFormats) return@forEach
            val factor = if (primaryFocal != null) equivalentFocal(candidate)?.div(primaryFocal) ?: 1f else 1f
            rawRoutes += RawRoute(id, false, roundLens(factor), candidateFormats)
        }
        val rawRoute = if (PhotoFormat.RAW in primaryFormats) null else rawRoutes.minByOrNull { abs(it.factor - 1f) }
        val photoFormats = primaryFormats + (rawRoute?.formats ?: emptySet())

        val videoCaps = runCatching { Recorder.getVideoCapabilities(info) }.getOrNull()
        val sdrQualities = runCatching { videoCaps?.getSupportedQualities(DynamicRange.SDR) }.getOrNull().orEmpty()
        val resolutions = buildList {
            if (Quality.UHD in sdrQualities) add(VideoResolution.UHD)
            if (Quality.FHD in sdrQualities) add(VideoResolution.FHD)
            if (Quality.HD in sdrQualities) add(VideoResolution.HD)
        }.ifEmpty { listOf(VideoResolution.FHD, VideoResolution.HD) }
        val hlg = runCatching { videoCaps?.supportedDynamicRanges?.contains(DynamicRange.HLG_10_BIT) == true }.getOrDefault(false)
        val stab = runCatching { videoCaps?.isStabilizationSupported == true }.getOrDefault(false)
        val fpsRanges = runCatching { info.supportedFrameRateRanges }.getOrDefault(emptySet())

        val slowRanges = runCatching {
            if (Recorder.getHighSpeedVideoCapabilities(info) == null) {
                emptyList()
            } else {
                val probeVc = VideoCapture.withOutput(Recorder.Builder().build())
                val cfg = HighSpeedVideoSessionConfig(probeVc, Preview.Builder().build())
                info.getSupportedFrameRateRanges(cfg)
                    .filter { it.lower == it.upper && it.upper >= 120 }
                    .distinctBy { it.upper }
                    .sortedBy { it.upper }
            }
        }.getOrDefault(emptyList())

        val formatSummary = runCatching { formatDiagnostics(info, formats) }.getOrDefault("") +
            (rawRoute?.let { "\nTrasa RAW CameraX: #${it.id}${if (it.physical) " (sensor fizyczny)" else ""} · ${String.format(java.util.Locale.US, "%.1f", it.factor)}×" } ?: "")
        val presets = lensZooms(info)
        val aux = runCatching { auxLenses(p, info, facing) }.getOrDefault(emptyList<AuxLens>() to "")

        val capabilities = ch(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
        val isoRange = ch(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
        val expRange = ch(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
        // Wielu producentów (Samsung na aparacie logicznym, Huawei, foldy) nie zgłasza flagi
        // MANUAL_SENSOR aplikacjom innych firm, choć ręczne ISO i czas działają. Wystarczą zakresy.
        val manual = capabilities?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR) == true ||
            (isoRange != null && expRange != null) ||
            runCatching { info.physicalCameraInfos }.getOrDefault(emptySet()).any { phys ->
                runCatching {
                    Camera2CameraInfo.from(phys).getCameraCharacteristic(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
                        ?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR) == true
                }.getOrDefault(false)
            }

        return CameraCaps(
            hasFront = hasLens(p, CameraSelector.LENS_FACING_FRONT),
            hasBack = hasLens(p, CameraSelector.LENS_FACING_BACK),
            nightExtension = extAvailable(ExtensionMode.NIGHT),
            bokehExtension = extAvailable(ExtensionMode.BOKEH),
            hdrExtension = extAvailable(ExtensionMode.HDR),
            retouchExtension = extAvailable(ExtensionMode.FACE_RETOUCH),
            photoFormats = photoFormats,
            zsl = runCatching { info.isZslSupported }.getOrDefault(false),
            manualSensor = manual,
            isoRange = isoRange?.let { it.lower..it.upper },
            exposureRange = expRange?.let { it.lower..it.upper },
            minFocusDistance = ch(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE) ?: 0f,
            hasFlash = info.hasFlashUnit(),
            videoResolutions = resolutions,
            videoHlg = hlg,
            videoStabilization = stab,
            fps60 = fpsRanges.any { it.upper >= 60 },
            fps24 = fpsRanges.any { it.lower <= 24 && it.upper >= 24 },
            slowMoRanges = slowRanges,
            lowLightBoost = runCatching { info.isLowLightBoostSupported }.getOrDefault(false),
            lensZooms = presets,
            maxZoom = zoomRange(info).second,
            auxLenses = aux.first,
            cameraSummary = aux.second,
            formatSummary = formatSummary,
            rawCameraId = rawRoute?.id,
            rawCameraPhysical = rawRoute?.physical == true,
            rawCameraFactor = rawRoute?.factor ?: 1f,
        )
    }

    /**
     * Diagnostyka formatów prosto z Camera2 (aparat główny i jego sensory fizyczne):
     * zdolność RAW, strumienie RAW_SENSOR / RAW10 / RAW12, JPEG_R (Ultra HDR, Android 14+)
     * i poziom sprzętowy – zestawione z tym, co faktycznie udostępnia CameraX.
     */
    private fun formatDiagnostics(info: CameraInfo, cameraXFormats: Set<Int>): String {
        fun describe(c2: Camera2CameraInfo): String {
            val caps = runCatching { c2.getCameraCharacteristic(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES) }.getOrNull()
            val rawCap = caps?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW) == true
            val map = runCatching { c2.getCameraCharacteristic(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP) }.getOrNull()
            val out = runCatching { map?.outputFormats?.toSet() }.getOrNull().orEmpty()
            val raws = buildList {
                if (ImageFormat.RAW_SENSOR in out) add("RAW_SENSOR")
                if (ImageFormat.RAW10 in out) add("RAW10")
                if (ImageFormat.RAW12 in out) add("RAW12")
                if (ImageFormat.RAW_PRIVATE in out) add("RAW_PRIVATE")
            }
            val jpegR = Build.VERSION.SDK_INT >= 34 && ImageFormat.JPEG_R in out
            val level = when (runCatching { c2.getCameraCharacteristic(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL) }.getOrNull()) {
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> "LEGACY"
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> "LIMITED"
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> "FULL"
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> "LEVEL_3"
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL -> "EXTERNAL"
                else -> "?"
            }
            return "poziom $level · zdolność RAW: ${if (rawCap) "tak" else "nie"} · strumienie RAW: " +
                (raws.joinToString("/").ifEmpty { "brak" }) + " · JPEG_R: " + (if (jpegR) "tak" else "nie")
        }
        val sb = StringBuilder()
        val main = Camera2CameraInfo.from(info)
        sb.append("#").append(main.cameraId).append(": ").append(describe(main))
        for (phys in runCatching { info.physicalCameraInfos }.getOrDefault(emptySet())) {
            val c2 = runCatching { Camera2CameraInfo.from(phys) }.getOrNull() ?: continue
            sb.append("\n  └ #").append(c2.cameraId).append(": ").append(describe(c2))
        }
        sb.append("\nCameraX: RAW ").append(if (ImageCapture.OUTPUT_FORMAT_RAW in cameraXFormats) "tak" else "nie")
        sb.append(" · Ultra HDR ").append(if (ImageCapture.OUTPUT_FORMAT_JPEG_ULTRA_HDR in cameraXFormats) "tak" else "nie")
        if (Build.VERSION.SDK_INT < 34) sb.append(" (Ultra HDR wymaga Androida 14, ten ma API ").append(Build.VERSION.SDK_INT).append(")")
        return sb.toString()
    }

    /** Ekwiwalent ogniskowej 35 mm dla danego obiektywu. */
    private fun equivalentFocal(info: CameraInfo): Float? {
        val c2 = Camera2CameraInfo.from(info)
        val focal = runCatching { c2.getCameraCharacteristic(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS) }
            .getOrNull()?.minOrNull() ?: return null
        val sensor = runCatching { c2.getCameraCharacteristic(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE) }.getOrNull()
            ?: return null
        val diagonal = sqrt(sensor.width * sensor.width + sensor.height * sensor.height)
        if (diagonal <= 0f) return null
        return focal * 43.27f / diagonal
    }

    /**
     * Krotności odpowiadające fizycznym obiektywom (szeroki kąt < 1×, główny 1×, tele > 1×).
     * Szeroki kąt nie jest „1”, tylko np. 0,6× – zgodnie z minimalnym zoomem urządzenia.
     */
    private fun lensZooms(info: CameraInfo): List<Float> {
        val (minZoom, maxZoom) = zoomRange(info)
        val result = mutableListOf(1f)
        if (minZoom < 0.95f) result += minZoom
        val base = equivalentFocal(info)
        if (base != null) {
            runCatching { info.physicalCameraInfos }.getOrDefault(emptySet()).forEach { phys ->
                val f = equivalentFocal(phys) ?: return@forEach
                var ratio = f / base
                if (ratio < 1f) ratio = max(ratio, minZoom)
                if (ratio > maxZoom + 0.01f) return@forEach
                result += roundLens(ratio)
            }
        }
        val sorted = result.filter { it in (minZoom - 0.01f)..(maxZoom + 0.01f) }.sorted()
        val deduped = mutableListOf<Float>()
        for (z in sorted) if (deduped.none { abs(it - z) / z < 0.08f }) deduped += z
        return deduped
    }

    // ------------------------------------------------------------------ wiązanie

    fun unbind() {
        provider?.unbindAll()
        camera = null
        preview = null
        imageCapture = null
        videoCapture = null
        session = ActiveSession()
    }

    suspend fun bind(
        owner: LifecycleOwner,
        surfaceProvider: Preview.SurfaceProvider,
        req: BindRequest,
        caps: CameraCaps,
    ): ActiveSession {
        val p = ensureProvider()
        unbind()
        baseOptions = null
        lastHud = null
        if (req.mode == CaptureMode.AUDIO) return session

        val s = req.settings
        val wantsRaw = req.mode in setOf(CaptureMode.PHOTO, CaptureMode.PRO) &&
            s.photoFormat in setOf(PhotoFormat.RAW, PhotoFormat.RAW_JPEG)
        val rawRoute = wantsRaw && req.cameraId == null && caps.rawCameraId != null
        val auxId = (if (rawRoute) caps.rawCameraId else req.cameraId).takeIf { req.mode != CaptureMode.SLOWMO }
        val auxPhysical = if (rawRoute) caps.rawCameraPhysical else caps.auxLenses.firstOrNull { it.cameraId == auxId }?.physical == true
        val base = selectorFor(req.lensFacing, auxId, auxPhysical)
        val selectedFormats = if (rawRoute) {
            caps.photoFormats.filterTo(mutableSetOf(PhotoFormat.JPEG)) { it == PhotoFormat.RAW || it == PhotoFormat.RAW_JPEG }
        } else {
            runCatching { photoFormatsOf(p.getCameraInfo(base)) }.getOrDefault(setOf(PhotoFormat.JPEG))
        }
        val ext = extensions
        val wantedExtension = if (auxId != null) null else when (req.mode) {
            CaptureMode.NIGHT -> ExtensionMode.NIGHT.takeIf { caps.nightExtension }
            CaptureMode.PORTRAIT -> when {
                s.portraitEffect == PortraitEffect.RETOUCH && caps.retouchExtension -> ExtensionMode.FACE_RETOUCH
                caps.bokehExtension -> ExtensionMode.BOKEH
                caps.retouchExtension -> ExtensionMode.FACE_RETOUCH
                else -> null
            }
            CaptureMode.HDR -> ExtensionMode.HDR.takeIf { caps.hdrExtension }
            else -> null
        }
        val selector = if (wantedExtension != null && ext != null) ext.getExtensionEnabledCameraSelector(base, wantedExtension) else base

        val aspect = if (req.mode.isVideo) AspectRatioMode.R16_9 else s.aspect
        val aspectStrategy = if (aspect == AspectRatioMode.R16_9) {
            AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY
        } else {
            AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY
        }

        fun buildPreview(withSelector: Boolean = true): Preview {
            val builder = Preview.Builder()
            if (withSelector) builder.setResolutionSelector(ResolutionSelector.Builder().setAspectRatioStrategy(aspectStrategy).build())
            if (wantedExtension == null) Camera2Interop.Extender(builder).setSessionCaptureCallback(hudCallback)
            return builder.build().also {
                it.targetRotation = displayRotation
                it.setSurfaceProvider(surfaceProvider)
                this@CameraEngine.preview = it
            }
        }

        var active: ActiveSession
        val bound: Camera

        when (req.mode) {
            CaptureMode.SLOWMO -> {
                val range = caps.slowMoRanges.firstOrNull { it.upper == s.slowMoFps } ?: caps.slowMoRanges.lastOrNull()
                    ?: error("Urządzenie nie obsługuje zwolnionego tempa")
                val info = p.getCameraInfo(base)
                val hsQuality = Recorder.getHighSpeedVideoCapabilities(info)?.getSupportedQualities(DynamicRange.SDR)?.firstOrNull()
                val recorder = Recorder.Builder().apply {
                    if (hsQuality != null) setQualitySelector(QualitySelector.from(hsQuality))
                }.build()
                val vc = VideoCapture.withOutput(recorder)
                val preview = Preview.Builder().setTargetRotation(displayRotation).build().also {
                    it.setSurfaceProvider(surfaceProvider)
                    this@CameraEngine.preview = it
                }
                val cfg = HighSpeedVideoSessionConfig(vc, preview, range, isSlowMotionEnabled = true)
                bound = p.bindToLifecycle(owner, base, cfg)
                videoCapture = vc
                active = ActiveSession(fps = range.upper)
            }

            CaptureMode.VIDEO -> {
                val quality = when (s.videoResolution.takeIf { it in caps.videoResolutions } ?: caps.videoResolutions.first()) {
                    VideoResolution.UHD -> Quality.UHD
                    VideoResolution.FHD -> Quality.FHD
                    VideoResolution.HD -> Quality.HD
                }
                val fps = when (s.videoFps) {
                    VideoFps.F60 -> if (caps.fps60) 60 else 0
                    VideoFps.F24 -> if (caps.fps24) 24 else 0
                    VideoFps.F30 -> 30
                    VideoFps.AUTO -> 0
                }
                val bitrate = bitrateFor(quality, s.bitrate, fps)
                data class Attempt(val hdr: Boolean, val stab: Boolean, val fps: Int, val snapshot: Boolean)
                val attempts = listOf(
                    Attempt(s.videoHdr && caps.videoHlg, s.stabilization && caps.videoStabilization, fps, true),
                    Attempt(s.videoHdr && caps.videoHlg, s.stabilization && caps.videoStabilization, fps, false),
                    Attempt(false, s.stabilization && caps.videoStabilization, fps, false),
                    Attempt(false, false, fps, false),
                    Attempt(false, false, 0, false),
                ).distinct()
                var result: Camera? = null
                var lastError: Exception? = null
                active = ActiveSession()
                for (a in attempts) {
                    try {
                        p.unbindAll()
                        val recorder = Recorder.Builder()
                            .setQualitySelector(QualitySelector.from(quality, FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)))
                            .apply { if (bitrate > 0) setTargetVideoEncodingBitRate(bitrate) }
                            .build()
                        val builder = VideoCapture.Builder(recorder).setMirrorMode(MirrorMode.MIRROR_MODE_ON_FRONT_ONLY)
                        if (a.stab) builder.setVideoStabilizationEnabled(true)
                        if (a.hdr) builder.setDynamicRange(DynamicRange.HLG_10_BIT)
                        if (a.fps > 0) builder.setTargetFrameRate(Range(a.fps, a.fps))
                        val vc = builder.build()
                        val useCases = mutableListOf<UseCase>(buildPreview(), vc)
                        var ic: ImageCapture? = null
                        if (a.snapshot) {
                            ic = ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                .setResolutionSelector(ResolutionSelector.Builder().setAspectRatioStrategy(aspectStrategy).build())
                                .build()
                            useCases += ic
                        }
                        result = p.bindToLifecycle(owner, base, *useCases.toTypedArray())
                        videoCapture = vc
                        imageCapture = ic
                        active = ActiveSession(videoHdr = a.hdr, stabilization = a.stab, fps = a.fps, snapshotInVideo = ic != null)
                        break
                    } catch (e: Exception) {
                        lastError = e
                    }
                }
                bound = result ?: throw (lastError ?: IllegalStateException("Nie udało się uruchomić wideo"))
            }

            CaptureMode.MOTION, CaptureMode.CLIP -> {
                // Podgląd + nagrywanie w tle (bufor chwil) + zdjęcie (dla Motion Photo).
                val recorder = Recorder.Builder()
                    .setQualitySelector(QualitySelector.from(Quality.FHD, FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)))
                    .setAspectRatio(if (aspect == AspectRatioMode.R16_9) androidx.camera.core.AspectRatio.RATIO_16_9 else androidx.camera.core.AspectRatio.RATIO_4_3)
                    .build()
                val vc = VideoCapture.Builder(recorder).setMirrorMode(MirrorMode.MIRROR_MODE_ON_FRONT_ONLY).build()
                var ic: ImageCapture? = null
                if (req.mode == CaptureMode.MOTION) {
                    ic = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .setResolutionSelector(ResolutionSelector.Builder().setAspectRatioStrategy(aspectStrategy).build())
                        .setJpegQuality(s.jpegQuality.coerceIn(1, 100))
                        .build()
                }
                bound = try {
                    p.bindToLifecycle(owner, base, *listOfNotNull<UseCase>(buildPreview(), vc, ic).toTypedArray())
                } catch (e: Exception) {
                    if (ic == null) throw e
                    ic = null
                    p.unbindAll()
                    p.bindToLifecycle(owner, base, buildPreview(), vc)
                }
                videoCapture = vc
                imageCapture = ic
                active = ActiveSession(snapshotInVideo = ic != null)
            }

            else -> {
                val format = when {
                    wantedExtension != null -> PhotoFormat.JPEG
                    req.mode != CaptureMode.PHOTO && req.mode != CaptureMode.PRO -> PhotoFormat.JPEG
                    s.photoFormat in selectedFormats -> s.photoFormat
                    else -> PhotoFormat.JPEG
                }
                val captureMode = when {
                    req.mode == CaptureMode.NIGHT || req.mode == CaptureMode.HDR -> ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
                    s.priority == CapturePriority.QUALITY -> ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
                    s.priority == CapturePriority.SPEED && caps.zsl && wantedExtension == null && format == PhotoFormat.JPEG ->
                        ImageCapture.CAPTURE_MODE_ZERO_SHUTTER_LAG
                    else -> ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
                }
                val postview = wantedExtension != null && runCatching {
                    ext?.let { ImageCapture.getImageCaptureCapabilities(p.getCameraInfo(selector)).isPostviewSupported }
                }.getOrNull() == true
                val icBuilder = ImageCapture.Builder()
                    .setResolutionSelector(
                        ResolutionSelector.Builder()
                            .setAspectRatioStrategy(aspectStrategy)
                            .setResolutionStrategy(ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY)
                            .build(),
                    )
                    .setCaptureMode(captureMode)
                    .setJpegQuality(s.jpegQuality.coerceIn(1, 100))
                    .setFlashMode(flashModeOf(s.flash))
                    .setOutputFormat(outputFormatOf(format))
                if (postview) icBuilder.setPostviewEnabled(true)
                if (wantedExtension == null && (req.mode == CaptureMode.NIGHT || req.mode == CaptureMode.HDR)) {
                    Camera2Interop.Extender(icBuilder)
                        .setCaptureRequestOption(CaptureRequest.NOISE_REDUCTION_MODE, CaptureRequest.NOISE_REDUCTION_MODE_HIGH_QUALITY)
                        .setCaptureRequestOption(CaptureRequest.EDGE_MODE, CaptureRequest.EDGE_MODE_HIGH_QUALITY)
                }
                val ic = icBuilder.build()

                // Analiza potrzebna do histogramu oraz do automatycznego doboru liczby klatek nocnych.
                val wantAnalysis = wantedExtension == null && (s.histogram || req.mode == CaptureMode.NIGHT)
                val analysis = if (wantAnalysis) {
                    ImageAnalysis.Builder()
                        .setResolutionSelector(
                            ResolutionSelector.Builder()
                                .setResolutionStrategy(ResolutionStrategy(Size(320, 240), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER))
                                .build(),
                        )
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { a -> a.setAnalyzer(analysisExecutor, HistogramAnalyzer { h -> main.execute { onHistogram?.invoke(h) } }) }
                } else {
                    null
                }
                val preview = buildPreview()
                bound = try {
                    p.bindToLifecycle(owner, selector, *listOfNotNull<UseCase>(preview, ic, analysis).toTypedArray())
                } catch (e: Exception) {
                    if (analysis == null) throw e
                    p.unbindAll()
                    p.bindToLifecycle(owner, selector, preview, ic)
                }
                imageCapture = ic
                active = ActiveSession(extension = wantedExtension, photoFormat = format, postview = postview)
            }
        }

        camera = bound
        val output = runCatching {
            if (req.mode.isVideo) videoCapture?.resolutionInfo?.resolution else imageCapture?.resolutionInfo?.resolution
        }.getOrNull()
        active = active.copy(outputSize = output)
        session = active

        if (wantedExtension == null && req.mode == CaptureMode.NIGHT) {
            val ranges = runCatching {
                Camera2CameraInfo.from(bound.cameraInfo).getCameraCharacteristic(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
            }.getOrNull()
            val slowest = ranges?.minWithOrNull(compareBy<Range<Int>>({ it.lower }, { it.upper }))
            if (slowest != null) {
                baseOptions = CaptureRequestOptions.Builder()
                    .setCaptureRequestOption(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, slowest)
                    .build()
            }
        }
        if (caps.lowLightBoost && s.lowLightBoost && (req.mode == CaptureMode.NIGHT || req.mode == CaptureMode.VIDEO) && wantedExtension == null) {
            runCatching { bound.cameraControl.enableLowLightBoostAsync(true) }
        }

        if (req.mode == CaptureMode.PRO) applyPro(req.pro) else applyBaseOptions()
        applyFlash(s.flash, req.mode)
        return active
    }

    private fun bitrateFor(quality: Quality, mode: VideoBitrate, fps: Int): Int {
        if (mode == VideoBitrate.AUTO) return 0
        val baseMbps = when (quality) {
            Quality.UHD -> 48
            Quality.FHD -> 20
            else -> 10
        }
        val fpsFactor = if (fps >= 60) 1.6f else 1f
        val factor = if (mode == VideoBitrate.HIGH) 1.5f else 2.6f
        return (baseMbps * fpsFactor * factor * 1_000_000).toInt()
    }

    private fun applyBaseOptions() {
        val cam = camera ?: return
        if (session.extension != null) return
        runCatching { Camera2CameraControl.from(cam.cameraControl).setCaptureRequestOptions(baseOptions ?: CaptureRequestOptions.Builder().build()) }
    }

    fun applyPro(pro: ProSettings) {
        val cam = camera ?: return
        if (session.extension != null) return
        val b = CaptureRequestOptions.Builder()
        if (pro.manualExposure) {
            val iso = pro.iso ?: lastHud?.iso ?: 400
            val exposure = pro.exposureNs ?: lastHud?.exposureNs ?: 16_666_666L
            b.setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
            b.setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY, iso)
            b.setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME, exposure)
            b.setCaptureRequestOption(CaptureRequest.SENSOR_FRAME_DURATION, max(exposure, 33_333_333L))
        }
        val focus = pro.focusDiopters
        if (focus != null) {
            b.setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
            b.setCaptureRequestOption(CaptureRequest.LENS_FOCUS_DISTANCE, focus)
        }
        if (pro.whiteBalance != WhiteBalance.AUTO) {
            b.setCaptureRequestOption(CaptureRequest.CONTROL_AWB_MODE, pro.whiteBalance.awbMode)
        }
        runCatching { Camera2CameraControl.from(cam.cameraControl).setCaptureRequestOptions(b.build()) }
    }

    private fun flashModeOf(f: FlashSetting) = when (f) {
        FlashSetting.AUTO -> ImageCapture.FLASH_MODE_AUTO
        FlashSetting.ON -> ImageCapture.FLASH_MODE_ON
        else -> ImageCapture.FLASH_MODE_OFF
    }

    fun applyFlash(f: FlashSetting, mode: CaptureMode) {
        imageCapture?.flashMode = if (mode.isVideo) ImageCapture.FLASH_MODE_OFF else flashModeOf(f)
        val cam = camera ?: return
        if (cam.cameraInfo.hasFlashUnit()) {
            val torch = f == FlashSetting.TORCH || (mode.isVideo && f == FlashSetting.ON)
            cam.cameraControl.enableTorch(torch)
        }
    }

    fun setZoom(ratio: Float) {
        camera?.cameraControl?.setZoomRatio(ratio)
    }

    fun focus(point: MeteringPoint, autoFocus: Boolean, lock: Boolean) {
        val cam = camera ?: return
        val flags = if (autoFocus) {
            FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE or FocusMeteringAction.FLAG_AWB
        } else {
            FocusMeteringAction.FLAG_AE
        }
        val builder = FocusMeteringAction.Builder(point, flags)
        if (lock) builder.disableAutoCancel() else builder.setAutoCancelDuration(5, TimeUnit.SECONDS)
        val action = builder.build()
        if (cam.cameraInfo.isFocusMeteringSupported(action)) cam.cameraControl.startFocusAndMetering(action)
    }

    fun cancelFocusLock() {
        camera?.cameraControl?.cancelFocusAndMetering()
    }

    fun setExposureIndex(index: Int) {
        camera?.cameraControl?.setExposureCompensationIndex(index)
    }

    /** Obrót wyświetlacza (Surface.ROTATION_*) – zmienia się tylko w trybie „Obracaj ekran”. */
    fun setDisplayRotation(rotation: Int) {
        displayRotation = rotation
        preview?.targetRotation = rotation
    }

    fun setTargetRotation(rotation: Int) {
        imageCapture?.targetRotation = rotation
        if (recording == null) videoCapture?.targetRotation = rotation
    }

    // ------------------------------------------------------------------ zdjęcia

    /**
     * Zdjęcie zapisane bezpośrednio do galerii. Obsługuje RAW (DNG), RAW+JPEG, Ultra HDR,
     * podgląd „postview” i postęp przetwarzania rozszerzeń producenta.
     */
    suspend fun takePhoto(
        rotation: Int,
        mirror: Boolean,
        tag: String,
        onProgress: (Int) -> Unit = {},
        onPostview: (Bitmap) -> Unit = {},
    ): PhotoResult {
        val ic = imageCapture ?: return PhotoResult(null, error = "Kamera nie jest gotowa")
        ic.targetRotation = rotation
        val format = session.photoFormat
        return suspendCancellableCoroutine { cont ->
            val expected = if (format == PhotoFormat.RAW_JPEG) 2 else 1
            var rawUri: Uri? = null
            var jpegUri: Uri? = null
            var finished = false
            val callback = object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    output.savedUri?.let { uri ->
                        if (output.imageFormat == ImageFormat.RAW_SENSOR || format == PhotoFormat.RAW) rawUri = uri else jpegUri = uri
                    }
                    val saved = listOfNotNull(rawUri, jpegUri).size
                    if (saved >= expected && !finished) {
                        finished = true
                        val result = when (format) {
                            PhotoFormat.RAW -> PhotoResult(rawUri)
                            PhotoFormat.RAW_JPEG -> PhotoResult(jpegUri ?: rawUri, rawUri)
                            else -> PhotoResult(jpegUri)
                        }
                        if (cont.isActive) cont.resume(result)
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    if (!finished) {
                        finished = true
                        if (cont.isActive) cont.resume(PhotoResult(jpegUri ?: rawUri, rawUri, exception.message))
                    }
                }

                override fun onCaptureProcessProgressed(progress: Int) = onProgress(progress)
                override fun onPostviewBitmapAvailable(bitmap: Bitmap) = onPostview(bitmap)
            }
            when (format) {
                PhotoFormat.RAW_JPEG -> ic.takePicture(
                    MediaSaver.rawOutputOptions(context, tag),
                    MediaSaver.photoOutputOptions(context, mirror, tag, "image/jpeg"),
                    main,
                    callback,
                )
                PhotoFormat.RAW -> ic.takePicture(MediaSaver.rawOutputOptions(context, tag), main, callback)
                else -> ic.takePicture(MediaSaver.photoOutputOptions(context, mirror, tag, "image/jpeg"), main, callback)
            }
        }
    }

    /** Pojedyncza klatka JPEG do pamięci (dla potoku nocnego i HDR). */
    suspend fun captureFrame(): CapturedFrame = suspendCancellableCoroutine { cont ->
        val ic = imageCapture
        if (ic == null) {
            cont.resumeWithException(IllegalStateException("Kamera nie jest gotowa"))
            return@suspendCancellableCoroutine
        }
        ic.takePicture(
            main,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    try {
                        val bytes = if (image.format == ImageFormat.JPEG) {
                            val buffer = image.planes[0].buffer
                            ByteArray(buffer.remaining()).also { buffer.get(it) }
                        } else {
                            val bmp = image.toBitmap()
                            ByteArrayOutputStream().use { os ->
                                bmp.compress(Bitmap.CompressFormat.JPEG, 98, os)
                                bmp.recycle()
                                os.toByteArray()
                            }
                        }
                        val frame = CapturedFrame(bytes, image.imageInfo.rotationDegrees)
                        image.close()
                        if (cont.isActive) cont.resume(frame)
                    } catch (t: Throwable) {
                        image.close()
                        if (cont.isActive) cont.resumeWithException(t)
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    if (cont.isActive) cont.resumeWithException(exception)
                }
            },
        )
    }

    /** Seria zdjęć z różną kompensacją ekspozycji (EV) – wejście do fuzji HDR. */
    suspend fun captureBracket(evStops: List<Float>): List<CapturedFrame> {
        val cam = camera ?: error("Kamera nie jest gotowa")
        val state = cam.cameraInfo.exposureState
        if (!state.isExposureCompensationSupported) return listOf(captureFrame())
        val step = state.exposureCompensationStep.toFloat().takeIf { it > 0f } ?: return listOf(captureFrame())
        val range = state.exposureCompensationRange
        val original = state.exposureCompensationIndex
        val frames = mutableListOf<CapturedFrame>()
        try {
            for (ev in evStops) {
                val index = (ev / step).roundToInt().coerceIn(range.lower, range.upper)
                runCatching { cam.cameraControl.setExposureCompensationIndex(index).await() }
                delay(160)
                frames += captureFrame()
            }
        } finally {
            cam.cameraControl.setExposureCompensationIndex(original)
        }
        return frames
    }

    // ------------------------------------------------------------------ bufor chwil

    /** Odcinek nagrania w tle; [startedAt] – SystemClock.elapsedRealtime() początku nagrania. */
    class Segment internal constructor(
        val file: java.io.File,
        val startedAt: Long,
        private val rec: Recording,
        private val done: kotlinx.coroutines.CompletableDeferred<Boolean>,
    ) {
        /** Kończy nagrywanie i czeka na zapis pliku; true = plik poprawny. */
        suspend fun finish(): Boolean {
            runCatching { rec.stop() }
            return kotlinx.coroutines.withTimeoutOrNull(5_000) { done.await() } ?: false
        }
    }

    private var segmentActive = false

    /** Rozpoczyna odcinek bufora do pliku; null, gdy nagrywanie nie ruszyło. */
    @SuppressLint("MissingPermission")
    suspend fun startSegment(file: java.io.File, rotation: Int, withAudio: Boolean): Segment? {
        val vc = videoCapture ?: return null
        if (recording != null || segmentActive) return null
        vc.targetRotation = rotation
        val started = kotlinx.coroutines.CompletableDeferred<Long>()
        val done = kotlinx.coroutines.CompletableDeferred<Boolean>()
        val pending = vc.output.prepareRecording(context, androidx.camera.video.FileOutputOptions.Builder(file).build())
        val audioGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (withAudio && audioGranted) pending.withAudioEnabled()
        segmentActive = true
        val rec = pending.start(main) { e ->
            when (e) {
                is VideoRecordEvent.Start -> started.complete(android.os.SystemClock.elapsedRealtime())
                is VideoRecordEvent.Finalize -> {
                    segmentActive = false
                    if (!started.isCompleted) started.complete(-1L)
                    done.complete(!e.hasError() || e.error == VideoRecordEvent.Finalize.ERROR_NONE)
                }
                else -> Unit
            }
        }
        val t = kotlinx.coroutines.withTimeoutOrNull(4_000) { started.await() } ?: -1L
        if (t < 0) {
            runCatching { rec.stop() }
            return null
        }
        return Segment(file, t, rec, done)
    }

    // ------------------------------------------------------------------ wideo

    @SuppressLint("MissingPermission")
    fun startRecording(rotation: Int, withAudio: Boolean, listener: (VideoRecordEvent) -> Unit): Boolean {
        val vc = videoCapture ?: return false
        if (recording != null) return false
        vc.targetRotation = rotation
        val pending = vc.output.prepareRecording(context, MediaSaver.videoOutputOptions(context))
        val audioGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (withAudio && audioGranted) pending.withAudioEnabled()
        recording = pending.start(main) { event ->
            if (event is VideoRecordEvent.Finalize) recording = null
            listener(event)
        }
        return true
    }

    fun pauseRecording() = recording?.pause()
    fun resumeRecording() = recording?.resume()
    fun stopRecording() = recording?.stop()
    fun muteRecording(muted: Boolean) = recording?.mute(muted)

    fun release() {
        recording?.stop()
        analysisExecutor.shutdown()
    }
}
