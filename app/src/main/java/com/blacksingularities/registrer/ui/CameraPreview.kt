// Podgląd kamery: kadr z gestami i nakładka asystenta.

package com.blacksingularities.registrer.ui

import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.draw.clipToBounds
import com.blacksingularities.registrer.ui.components.CompassTape
import androidx.compose.material.icons.rounded.Image
import androidx.camera.core.CameraSelector
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.blacksingularities.registrer.camera.CaptureMode
import com.blacksingularities.registrer.ui.components.FocusRing
import com.blacksingularities.registrer.ui.components.GridOverlay
import com.blacksingularities.registrer.ui.components.HistogramView
import com.blacksingularities.registrer.ui.components.LevelOverlay
import com.blacksingularities.registrer.ui.components.imageRect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@Composable
internal fun PreviewArea(ui: Ui, previewView: PreviewView, centered: Boolean, ratio: Float, modifier: Modifier) {
    val vm = ui.vm
    val density = LocalDensity.current
    val streamState by previewView.previewStreamState.observeAsState()
    var focusPoint by remember { mutableStateOf<Offset?>(null) }
    var focusTrigger by remember { mutableIntStateOf(0) }
    LaunchedEffect(focusTrigger, vm.evIndex, vm.focusLocked) {
        if (focusPoint != null && !vm.focusLocked) {
            delay(3500)
            focusPoint = null
        }
    }
    LaunchedEffect(vm.bindKey) { focusPoint = null }

    val flashAlpha = remember { Animatable(0f) }
    LaunchedEffect(vm.shutterTick) {
        if (vm.shutterTick > 0) {
            flashAlpha.snapTo(0.85f)
            flashAlpha.animateTo(0f, tween(260))
        }
    }

    // Zmiana proporcji (i każde inne ponowne wiązanie kamery) na chwilę chowa część sterowania
    // (EV, zoom, panel PRO). Gdy kamera nie jest gotowa, granica może się tylko podnieść –
    // dzięki temu nakładki pływające nie skaczą w dół i z powrotem.
    var settledControlsTop by remember { mutableFloatStateOf(ui.controlsTop) }
    LaunchedEffect(ui.controlsTop, vm.cameraReady) {
        settledControlsTop = if (vm.cameraReady) ui.controlsTop else minOf(settledControlsTop, ui.controlsTop)
    }

    var areaOrigin by remember { mutableStateOf(Offset.Zero) }
    val areaTop = areaOrigin.y
    BoxWithConstraints(modifier.onGloballyPositioned { areaOrigin = it.boundsInRoot().topLeft }) {
        val containerSize = with(density) { Size(maxWidth.toPx(), maxHeight.toPx()) }
        // Nakładki pływające (histogram, pasek parametrów, kompas) tylko nad dolnym sterowaniem.
        // Obszar nie zależy od proporcji kadru, więc zmiana 4:3 ↔ 16:9 nie przesuwa nakładek.
        val overlayArea = Rect(
            Offset.Zero,
            // Nakładki mogą zejść tuż do dolnego sterowania (lekko na jego przezroczysty margines).
            Size(containerSize.width, (settledControlsTop - areaTop + with(density) { 14.dp.toPx() }).coerceIn(containerSize.height * 0.3f, containerSize.height)),
        )
        val rect = imageRect(containerSize, ratio, centered)
        // Kosz na nakładki: pod obszarem nakładek (nad dolnym sterowaniem), z lewej strony –
        // poza miejscami, w które przeciąga się paski, więc nie da się go trafić przypadkiem.
        val trashCenter = with(density) {
            Offset(
                overlayArea.left + 46.dp.toPx(),
                minOf(overlayArea.bottom + 30.dp.toPx(), containerSize.height - 34.dp.toPx()),
            )
        }
        // Klucz ekranu niezależny od obrotu (krótszy × dłuższy bok, co 40 dp) – fold złożony i rozłożony
        // mają osobne pozycje nakładek.
        val screenKey = remember(maxWidth, maxHeight) {
            val shortSide = minOf(maxWidth.value, maxHeight.value)
            val longSide = maxOf(maxWidth.value, maxHeight.value)
            "${(shortSide / 40f).roundToInt()}x${(longSide / 40f).roundToInt()}"
        }

        // Widok kamery zajmuje dokładnie prostokąt kadru – obraz jest wyśrodkowany.
        // Zoom cyfrowy ponad maksimum aparatu: podgląd powiększany w obrębie kadru.
        val crop by animateFloatAsState(vm.cropZoom, tween(90), label = "crop")
        Box(
            Modifier
                .offset { IntOffset(rect.left.roundToInt(), rect.top.roundToInt()) }
                .size(with(density) { rect.width.toDp() }, with(density) { rect.height.toDp() })
                .clipToBounds(),
        ) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = crop
                        scaleY = crop
                    },
            )
        }

        val streaming = streamState == PreviewView.StreamState.STREAMING && vm.cameraReady
        val curtain by animateFloatAsState(if (streaming) 0f else 1f, tween(if (streaming) 380 else 120), label = "curtain")
        val freeze = ui.freeze
        if (freeze != null) {
            Image(
                bitmap = freeze,
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .offset { IntOffset(rect.left.roundToInt(), rect.top.roundToInt()) }
                    .size(with(density) { rect.width.toDp() }, with(density) { rect.height.toDp() })
                    .graphicsLayer { alpha = curtain },
            )
        } else {
            Box(Modifier.fillMaxSize().graphicsLayer { alpha = curtain }.background(Color.Black))
        }

        // Szczypanie = zoom, dotknięcie = ostrość, przytrzymanie = blokada AE/AF, podwójne = obiektyw.
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoom, _ ->
                        if (zoom != 1f) vm.zoomBy(zoom, 22f)
                    }
                }
                .pointerInput(rect) {
                    fun meter(o: Offset, lock: Boolean) {
                        if (!rect.contains(o) || vm.mode == CaptureMode.AUDIO) return
                        vm.focus(previewView.meteringPointFactory.createPoint(
                            rect.width / 2f + (o.x - rect.center.x) / vm.cropZoom,
                            rect.height / 2f + (o.y - rect.center.y) / vm.cropZoom,
                        ), lock)
                        focusPoint = o
                        focusTrigger++
                    }
                    detectTapGestures(
                        onTap = { meter(it, lock = false) },
                        onLongPress = { meter(it, lock = true) },
                        onDoubleTap = { vm.switchLens() },
                    )
                },
        )

        // Sesja bez osobnej analizy (wideo, zwolnione tempo, rozszerzenia): histogram i obrys
        // liczone z obrazu podglądu – działają w każdym trybie.
        val sampleConfig = vm.assistConfig
        val sampling = vm.cameraReady && !vm.session.analysis && sampleConfig.active && streamState == PreviewView.StreamState.STREAMING
        LaunchedEffect(sampling, sampleConfig, previewView) {
            if (!sampling) return@LaunchedEffect
            val sampler = com.blacksingularities.registrer.camera.PreviewSampler()
            while (true) {
                val bmp = runCatching { previewView.bitmap }.getOrNull()
                if (bmp != null) {
                    val (bins, frame) = withContext(Dispatchers.Default) { sampler.process(bmp, sampleConfig) }
                    vm.onPreviewSample(bins, frame)
                }
                delay(if (sampleConfig.any) 110 else 220)
            }
        }

        if (vm.mode != CaptureMode.AUDIO) GridOverlay(vm.global.grids, ratio, Modifier.fillMaxSize(), centered)
        AssistOverlay({ vm.assistFrame }, rect, crop, vm.lensFacing == CameraSelector.LENS_FACING_FRONT)
        if (vm.global.level && vm.mode != CaptureMode.AUDIO) {
            val gl = vm.global
            val calibration = remember(gl.levelCalX, gl.levelCalY, gl.levelCalZ) { floatArrayOf(gl.levelCalX, gl.levelCalY, gl.levelCalZ) }
            LevelOverlay(
                ratio,
                ui.haptics,
                Modifier.fillMaxSize(),
                centered,
                ui.displayDegrees,
                style = gl.levelStyle,
                tolerance = gl.levelTolerance,
                showPitch = gl.levelPitch,
                fadeWhenLevel = gl.levelFade,
                calibration = calibration,
            )
        }

        focusPoint?.let { p ->
            FocusRing(
                point = p,
                trigger = focusTrigger,
                evIndex = vm.evIndex,
                evMin = vm.evMin,
                evMax = vm.evMax,
                onEv = { vm.setEv(it) },
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Informacje w kadrze zawsze „do góry” względem trzymającego telefon.
        UprightFrame(rect, ui.uprightTurn) {
            FrameInfo(ui)
        }

        // Przytrzymanie pozwala przeciągnąć nakładkę; kosz usuwa ją z kadru.
        var overlayDragging by remember { mutableStateOf(false) }
        // Linie pomocnicze przyciągania (środek i krawędzie zdjęcia) podczas przeciągania nakładki.
        var guideX by remember { mutableStateOf<Float?>(null) }
        var guideY by remember { mutableStateOf<Float?>(null) }
        val guides: (Float?, Float?) -> Unit = { gx, gy ->
            guideX = gx
            guideY = gy
        }
        var overlayOverTrash by remember { mutableStateOf(false) }
        val dragState: (Boolean, Boolean) -> Unit = { dragging, overTrash ->
            overlayDragging = dragging
            overlayOverTrash = overTrash
        }
        val hasHistogram by remember { derivedStateOf { vm.histogram != null } }

        // Powyżej 10× główny podgląd jest kadrowany programowo. Osobna miniatura zachowuje
        // szerszy obraz bazowy, dzięki czemu łatwo odnaleźć obiekt i zobaczyć aktualny wycinek.
        val showZoomNavigator = vm.cropZoom > 1.001f && vm.displayZoom > ZOOM_NAVIGATOR_THRESHOLD
        var zoomOverview by remember { mutableStateOf<ImageBitmap?>(null) }
        LaunchedEffect(showZoomNavigator, previewView) {
            if (!showZoomNavigator) {
                zoomOverview = null
                return@LaunchedEffect
            }
            while (true) {
                runCatching { previewView.bitmap?.asImageBitmap() }.getOrNull()?.let { zoomOverview = it }
                delay(200)
            }
        }
        zoomOverview?.takeIf { showZoomNavigator }?.let { overview ->
            DraggableOverlay(
                rect = overlayArea,
                photo = rect,
                trash = trashCenter,
                onGuides = guides,
                start = vm.overlayPos("navigator", screenKey, Offset(vm.global.zoomNavigatorX, vm.global.zoomNavigatorY)),
                turn = ui.uprightTurn,
                rotation = ui.iconRotation,
                haptics = ui.haptics,
                deletable = false,
                onCommit = { p -> vm.saveOverlayPos("navigator", screenKey, p) },
                onDragState = { _, _ -> },
                onDelete = {},
            ) {
                ZoomNavigator(overview, vm.displayZoom, vm.zoomNavigatorBase)
            }
        }

        if (hasHistogram && vm.global.histogram && vm.mode != CaptureMode.AUDIO) {
            DraggableOverlay(
                rect = overlayArea,
                photo = rect,
                trash = trashCenter,
                onGuides = guides,
                start = vm.overlayPos("histogram", screenKey, Offset(vm.global.histogramX, vm.global.histogramY)),
                turn = ui.uprightTurn,
                rotation = ui.iconRotation,
                haptics = ui.haptics,
                onCommit = { p -> vm.saveOverlayPos("histogram", screenKey, p) },
                onDragState = dragState,
                onDelete = { vm.updateGlobal { it.copy(histogram = false) } },
            ) {
                // Dotknięcie przełącza tryb: jasność ↔ RGB. Dane czytane tu – nowe przedziały
                // przerysowują tylko histogram, nie cały kadr.
                HistogramView(
                    vm.histogram ?: IntArray(256),
                    Modifier.pointerInput(Unit) {
                        detectTapGestures {
                            vm.updateGlobal {
                                val all = com.blacksingularities.registrer.data.HistogramMode.entries
                                it.copy(histogramMode = all[(it.histogramMode.ordinal + 1) % all.size])
                            }
                        }
                    },
                    mode = vm.global.histogramMode,
                    histogramSize = vm.global.histogramSize,
                )
            }
        }
        if (vm.global.exposureInfo && vm.mode != CaptureMode.AUDIO) {
            DraggableOverlay(
                rect = overlayArea,
                photo = rect,
                trash = trashCenter,
                onGuides = guides,
                start = vm.overlayPos("strip", screenKey, Offset(vm.global.stripX, vm.global.stripY)),
                turn = ui.uprightTurn,
                rotation = ui.iconRotation,
                haptics = ui.haptics,
                onCommit = { p -> vm.saveOverlayPos("strip", screenKey, p) },
                onDragState = dragState,
                onDelete = { vm.updateGlobal { it.copy(exposureInfo = false) } },
            ) { ExposureStrip(vm) }
        }
        if (vm.global.audioMeter && vm.videoMode && vm.mode != CaptureMode.SLOWMO && vm.cfg.videoAudio) {
            DraggableOverlay(
                rect = overlayArea,
                photo = rect,
                trash = trashCenter,
                onGuides = guides,
                start = vm.overlayPos("audioMeter", screenKey, Offset(vm.global.audioMeterX, vm.global.audioMeterY)),
                turn = ui.uprightTurn,
                rotation = ui.iconRotation,
                haptics = ui.haptics,
                onCommit = { p -> vm.saveOverlayPos("audioMeter", screenKey, p) },
                onDragState = dragState,
                onDelete = { vm.updateGlobal { it.copy(audioMeter = false) } },
            ) {
                com.blacksingularities.registrer.ui.components.VideoAudioMeter(
                    level = { vm.videoLevel },
                    peakDb = { vm.videoPeakDb },
                    note = vm.videoAudioNote,
                    recording = vm.recState != RecState.IDLE,
                    problem = vm.recState != RecState.IDLE && vm.videoAudioProblem,
                )
            }
        }
        if (vm.global.compass && vm.mode != CaptureMode.AUDIO) {
            DraggableOverlay(
                rect = overlayArea,
                photo = rect,
                trash = trashCenter,
                onGuides = guides,
                start = vm.overlayPos("compass", screenKey, Offset(vm.global.compassX, vm.global.compassY)),
                turn = ui.uprightTurn,
                rotation = ui.iconRotation,
                haptics = ui.haptics,
                onCommit = { p -> vm.saveOverlayPos("compass", screenKey, p) },
                onDragState = dragState,
                onDelete = { vm.updateGlobal { it.copy(compass = false) } },
            ) { CompassTape() }
        }

        if (overlayDragging && (guideX != null || guideY != null)) {
            Canvas(Modifier.fillMaxSize()) {
                val line = Color.White.copy(alpha = 0.55f)
                val w = 1.dp.toPx()
                guideX?.let { x -> drawLine(line, Offset(x, rect.top), Offset(x, rect.bottom), w) }
                guideY?.let { y -> drawLine(line, Offset(rect.left, y), Offset(rect.right, y), w) }
            }
        }

        // Kosz rysuje ekran główny – nad dolnym sterowaniem, poza obszarem nakładek.
        LaunchedEffect(overlayDragging, overlayOverTrash, trashCenter, areaOrigin) {
            ui.onTrash(if (overlayDragging) trashCenter + areaOrigin else null, overlayOverTrash)
        }

        // Baner aktualizacji na środku kadru i nad nakładkami – żadna z nich nie przechwyci dotknięcia.
        UprightFrame(rect, ui.uprightTurn) {
            UpdateBanner(
                state = vm.update,
                visible = !vm.busy,
                onDownload = vm::downloadUpdate,
                onInstall = { vm.installUpdate() },
                onDismiss = vm::dismissUpdate,
                modifier = Modifier.align(Alignment.Center).padding(horizontal = 16.dp),
            )
        }

        Box(Modifier.fillMaxSize().graphicsLayer { alpha = flashAlpha.value }.background(Color.Black))
    }
}

/**
 * Maska obrysu ostrości i zebry nad podglądem: klatka analizy obracana do pozycji ekranu,
 * odbijana dla przedniego aparatu i powiększana jak podgląd przy zoomie cyfrowym.
 * Stan czytany dopiero przy rysowaniu – nowa maska nie przebudowuje całego kadru.
 */
@Composable
internal fun AssistOverlay(frame: () -> com.blacksingularities.registrer.camera.AssistFrame?, rect: Rect, crop: Float, mirror: Boolean) {
    Canvas(Modifier.fillMaxSize()) {
        val f = frame() ?: return@Canvas
        val bmp = f.bitmap
        // Maska z obrazu podglądu jest już odbita jak na ekranie.
        val flip = mirror && f.sensor
        if (bmp.isRecycled || rect.width <= 0f) return@Canvas
        val swap = f.rotation % 180 != 0
        val dstW = if (swap) rect.height else rect.width
        val dstH = if (swap) rect.width else rect.height
        clipRect(rect.left, rect.top, rect.right, rect.bottom) {
            withTransform({
                translate(rect.center.x, rect.center.y)
                scale(if (flip) -crop else crop, crop, pivot = Offset.Zero)
                rotate(f.rotation.toFloat(), pivot = Offset.Zero)
            }) {
                drawImage(
                    bmp.asImageBitmap(),
                    dstOffset = IntOffset((-dstW / 2f).roundToInt(), (-dstH / 2f).roundToInt()),
                    dstSize = androidx.compose.ui.unit.IntSize(dstW.roundToInt(), dstH.roundToInt()),
                )
            }
        }
    }
}
