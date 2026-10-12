package com.blacksingularities.registrer.camera

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.net.Uri
import android.util.Range
import android.util.Size
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera
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
import com.blacksingularities.registrer.data.VideoResolution
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Warstwa sprzętowa: wiąże przypadki użycia CameraX, zarządza rozszerzeniami producenta
 * (Noc / Bokeh / HDR), ręcznym sterowaniem sensorem (Camera2 Interop), zdjęciami, burstem
 * do pamięci i nagrywaniem wideo.
 */
@OptIn(ExperimentalCamera2Interop::class)
class CameraEngine(private val context: Context) {
    private val main = ContextCompat.getMainExecutor(context)
    private val analysisExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    private var provider: ProcessCameraProvider? = null
    private var extensions: ExtensionsManager? = null

    var camera: Camera? = null
        private set
    var activeExtension: Int? = null
        private set
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private var lastHud: Hud? = null
    private var hudFrame = 0
    private var nightPreviewOptions: CaptureRequestOptions? = null

    var onHud: ((Hud) -> Unit)? = null
    var onHistogram: ((IntArray) -> Unit)? = null

    val isRecording get() = recording != null

    private val hudCallback = object : CameraCaptureSession.CaptureCallback() {
        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult,
        ) {
            if (hudFrame++ % 6 != 0) return
            val iso = result.get(CaptureResult.SENSOR_SENSITIVITY) ?: return
            val exposure = result.get(CaptureResult.SENSOR_EXPOSURE_TIME) ?: return
            val focus = result.get(CaptureResult.LENS_FOCUS_DISTANCE)
            val hud = Hud(iso, exposure, focus)
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

    fun unbind() {
        provider?.unbindAll()
        camera = null
        imageCapture = null
        videoCapture = null
        activeExtension = null
    }

    suspend fun bind(owner: LifecycleOwner, surfaceProvider: Preview.SurfaceProvider, req: BindRequest): CameraCaps {
        val p = ensureProvider()
        p.unbindAll()
        camera = null
        imageCapture = null
        videoCapture = null
        activeExtension = null
        nightPreviewOptions = null
        lastHud = null

        val hasBack = hasLens(p, CameraSelector.LENS_FACING_BACK)
        val hasFront = hasLens(p, CameraSelector.LENS_FACING_FRONT)
        val facing = when {
            req.lensFacing == CameraSelector.LENS_FACING_FRONT && hasFront -> CameraSelector.LENS_FACING_FRONT
            hasBack -> CameraSelector.LENS_FACING_BACK
            else -> CameraSelector.LENS_FACING_FRONT
        }
        val base = CameraSelector.Builder().requireLensFacing(facing).build()
        val ext = extensions
        fun extAvailable(mode: Int) = ext != null && runCatching { ext.isExtensionAvailable(base, mode) }.getOrDefault(false)

        var caps = CameraCaps(
            hasFront = hasFront,
            hasBack = hasBack,
            nightExtension = extAvailable(ExtensionMode.NIGHT),
            bokehExtension = extAvailable(ExtensionMode.BOKEH),
            hdrExtension = extAvailable(ExtensionMode.HDR),
        )
        if (req.mode == CaptureMode.AUDIO) return caps

        val wantedExtension = when (req.mode) {
            CaptureMode.NIGHT -> ExtensionMode.NIGHT.takeIf { caps.nightExtension }
            CaptureMode.PORTRAIT -> ExtensionMode.BOKEH.takeIf { caps.bokehExtension }
            CaptureMode.HDR -> ExtensionMode.HDR.takeIf { caps.hdrExtension }
            else -> null
        }
        val selector = if (wantedExtension != null && ext != null) {
            ext.getExtensionEnabledCameraSelector(base, wantedExtension)
        } else {
            base
        }

        val isVideo = req.mode == CaptureMode.VIDEO
        val aspect = if (isVideo) AspectRatioMode.R16_9 else req.settings.aspect
        val aspectStrategy = if (aspect == AspectRatioMode.R16_9) {
            AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY
        } else {
            AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY
        }

        fun buildPreview(): Preview {
            val builder = Preview.Builder()
                .setResolutionSelector(ResolutionSelector.Builder().setAspectRatioStrategy(aspectStrategy).build())
            if (wantedExtension == null) {
                Camera2Interop.Extender(builder).setSessionCaptureCallback(hudCallback)
            }
            return builder.build().also { it.setSurfaceProvider(surfaceProvider) }
        }

        var bound: Camera? = null
        var videoHdr = false
        var stabilization = false

        if (isVideo) {
            val quality = when (req.settings.videoResolution) {
                VideoResolution.UHD -> Quality.UHD
                VideoResolution.FHD -> Quality.FHD
                VideoResolution.HD -> Quality.HD
            }
            // Próbujemy od najbogatszej konfiguracji, schodząc w dół, gdy urządzenie jej nie obsługuje.
            val attempts = listOf(
                req.settings.videoHdr to req.settings.stabilization,
                false to req.settings.stabilization,
                false to false,
            ).distinct()
            var lastError: Exception? = null
            for ((hdr, stab) in attempts) {
                try {
                    p.unbindAll()
                    val recorder = Recorder.Builder()
                        .setQualitySelector(
                            QualitySelector.from(quality, FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)),
                        )
                        .build()
                    val builder = VideoCapture.Builder(recorder)
                        .setMirrorMode(MirrorMode.MIRROR_MODE_ON_FRONT_ONLY)
                    if (stab) builder.setVideoStabilizationEnabled(true)
                    if (hdr) builder.setDynamicRange(DynamicRange.HLG_10_BIT)
                    val vc = builder.build()
                    bound = p.bindToLifecycle(owner, selector, buildPreview(), vc)
                    videoCapture = vc
                    videoHdr = hdr
                    stabilization = stab
                    break
                } catch (e: Exception) {
                    lastError = e
                }
            }
            if (bound == null) throw lastError ?: IllegalStateException("Nie udało się uruchomić wideo")
        } else {
            val quality = req.settings.maxQuality ||
                req.mode == CaptureMode.NIGHT || req.mode == CaptureMode.HDR || req.mode == CaptureMode.PRO
            val icBuilder = ImageCapture.Builder()
                .setResolutionSelector(
                    ResolutionSelector.Builder()
                        .setAspectRatioStrategy(aspectStrategy)
                        .setResolutionStrategy(ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY)
                        .build(),
                )
                .setCaptureMode(
                    if (quality) ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY else ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY,
                )
                .setJpegQuality(req.settings.jpegQuality.coerceIn(1, 100))
                .setFlashMode(flashModeOf(req.flash))
            if (wantedExtension == null && (req.mode == CaptureMode.NIGHT || req.mode == CaptureMode.HDR)) {
                Camera2Interop.Extender(icBuilder)
                    .setCaptureRequestOption(CaptureRequest.NOISE_REDUCTION_MODE, CaptureRequest.NOISE_REDUCTION_MODE_HIGH_QUALITY)
                    .setCaptureRequestOption(CaptureRequest.EDGE_MODE, CaptureRequest.EDGE_MODE_HIGH_QUALITY)
            }
            val ic = icBuilder.build()

            val analysis = if (wantedExtension == null && req.settings.histogram) {
                ImageAnalysis.Builder()
                    .setResolutionSelector(
                        ResolutionSelector.Builder()
                            .setResolutionStrategy(
                                ResolutionStrategy(Size(320, 240), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER),
                            )
                            .build(),
                    )
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { a ->
                        a.setAnalyzer(analysisExecutor, HistogramAnalyzer { h -> main.execute { onHistogram?.invoke(h) } })
                    }
            } else {
                null
            }

            val preview = buildPreview()
            val useCases = listOfNotNull<UseCase>(preview, ic, analysis)
            bound = try {
                p.bindToLifecycle(owner, selector, *useCases.toTypedArray())
            } catch (e: Exception) {
                if (analysis == null) throw e
                p.unbindAll()
                p.bindToLifecycle(owner, selector, preview, ic)
            }
            imageCapture = ic
        }

        val cam = bound!!
        camera = cam
        activeExtension = wantedExtension

        val info = Camera2CameraInfo.from(cam.cameraInfo)
        val capabilities = runCatching { info.getCameraCharacteristic(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES) }.getOrNull()
        val manual = capabilities?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR) == true
        val isoRange = runCatching { info.getCameraCharacteristic(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE) }.getOrNull()
        val expRange = runCatching { info.getCameraCharacteristic(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE) }.getOrNull()
        val minFocus = runCatching { info.getCameraCharacteristic(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE) }.getOrNull()

        caps = caps.copy(
            manualSensor = manual && wantedExtension == null,
            isoRange = isoRange?.let { it.lower..it.upper },
            exposureRange = expRange?.let { it.lower..it.upper },
            minFocusDistance = minFocus ?: 0f,
            hasFlash = cam.cameraInfo.hasFlashUnit(),
            videoHdrActive = videoHdr,
            stabilizationActive = stabilization,
        )

        if (wantedExtension == null && req.mode == CaptureMode.NIGHT) {
            // Brak sprzętowego trybu nocnego: pozwalamy AE wydłużyć czas naświetlania w podglądzie.
            val ranges = runCatching { info.getCameraCharacteristic(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES) }.getOrNull()
            val slowest = ranges?.minWithOrNull(compareBy<Range<Int>>({ it.lower }, { it.upper }))
            if (slowest != null) {
                nightPreviewOptions = CaptureRequestOptions.Builder()
                    .setCaptureRequestOption(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, slowest)
                    .build()
            }
        }

        if (req.mode == CaptureMode.PRO) applyPro(req.pro) else applyBaseOptions()
        applyFlash(req.flash, req.mode)
        return caps
    }

    private fun applyBaseOptions() {
        val cam = camera ?: return
        if (activeExtension != null) return
        val options = nightPreviewOptions ?: CaptureRequestOptions.Builder().build()
        runCatching { Camera2CameraControl.from(cam.cameraControl).setCaptureRequestOptions(options) }
    }

    fun applyPro(pro: ProSettings) {
        val cam = camera ?: return
        if (activeExtension != null) return
        val b = CaptureRequestOptions.Builder()
        if (pro.manualExposure) {
            val iso = pro.iso ?: lastHud?.iso ?: 400
            val exposure = pro.exposureNs ?: lastHud?.exposureNs ?: 16_666_666L
            b.setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
            b.setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY, iso)
            b.setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME, exposure)
            b.setCaptureRequestOption(CaptureRequest.SENSOR_FRAME_DURATION, max(exposure, 33_333_333L))
        }
        if (pro.focusDiopters != null) {
            b.setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
            b.setCaptureRequestOption(CaptureRequest.LENS_FOCUS_DISTANCE, pro.focusDiopters)
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
        imageCapture?.flashMode = flashModeOf(f)
        val cam = camera ?: return
        if (cam.cameraInfo.hasFlashUnit()) {
            val torch = f == FlashSetting.TORCH || (mode == CaptureMode.VIDEO && f == FlashSetting.ON)
            cam.cameraControl.enableTorch(torch)
        }
    }

    fun setZoom(ratio: Float) {
        camera?.cameraControl?.setZoomRatio(ratio)
    }

    fun focus(point: MeteringPoint, autoFocus: Boolean) {
        val cam = camera ?: return
        val flags = if (autoFocus) {
            FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE or FocusMeteringAction.FLAG_AWB
        } else {
            FocusMeteringAction.FLAG_AE
        }
        val action = FocusMeteringAction.Builder(point, flags)
            .setAutoCancelDuration(5, TimeUnit.SECONDS)
            .build()
        if (cam.cameraInfo.isFocusMeteringSupported(action)) cam.cameraControl.startFocusAndMetering(action)
    }

    fun setExposureIndex(index: Int) {
        camera?.cameraControl?.setExposureCompensationIndex(index)
    }

    fun setTargetRotation(rotation: Int) {
        imageCapture?.targetRotation = rotation
        if (recording == null) videoCapture?.targetRotation = rotation
    }

    fun takePhoto(rotation: Int, mirror: Boolean, tag: String, onResult: (Uri?, String?) -> Unit) {
        val ic = imageCapture ?: return onResult(null, "Kamera nie jest gotowa")
        ic.targetRotation = rotation
        ic.takePicture(
            MediaSaver.photoOutputOptions(context, mirror, tag),
            main,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) = onResult(output.savedUri, null)
                override fun onError(exception: ImageCaptureException) = onResult(null, exception.message)
            },
        )
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
                                bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 98, os)
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

    fun release() {
        recording?.stop()
        analysisExecutor.shutdown()
    }
}
