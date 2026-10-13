package com.blacksingularities.registrer.ui

import androidx.compose.foundation.gestures.detectDragGestures
import com.blacksingularities.registrer.ui.components.QuickControl
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Exposure
import androidx.compose.material.icons.rounded.BlurOn
import dev.chrisbanes.haze.rememberHazeState
import dev.chrisbanes.haze.hazeSource
import com.blacksingularities.registrer.ui.theme.reactive
import com.blacksingularities.registrer.ui.theme.glass
import com.blacksingularities.registrer.ui.theme.LocalHaze
import com.blacksingularities.registrer.ui.theme.GlassScope
import com.blacksingularities.registrer.ui.theme.GlassLevel
import com.blacksingularities.registrer.ui.theme.GlassIconButton
import com.blacksingularities.registrer.ui.theme.GlassChip
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.RawOn
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.HdrOn
import androidx.compose.material.icons.rounded.HdrOff
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.spring
import androidx.activity.compose.BackHandler
import android.content.ActivityNotFoundException
import android.content.Intent
import android.view.OrientationEventListener
import android.view.Surface
import androidx.camera.core.CameraSelector
import androidx.camera.extensions.ExtensionMode
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cameraswitch
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FlashAuto
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.FlipToBack
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.blacksingularities.registrer.camera.CaptureMode
import com.blacksingularities.registrer.camera.FlashSetting
import com.blacksingularities.registrer.data.GridType
import com.blacksingularities.registrer.data.PhotoFormat
import com.blacksingularities.registrer.ui.components.AudioStudio
import com.blacksingularities.registrer.ui.components.FocusRing
import com.blacksingularities.registrer.ui.components.GridOverlay
import com.blacksingularities.registrer.ui.components.HistogramView
import com.blacksingularities.registrer.ui.components.LevelOverlay
import com.blacksingularities.registrer.ui.components.ModeDial
import com.blacksingularities.registrer.ui.components.ProPanel
import com.blacksingularities.registrer.ui.components.SettingsSheet
import com.blacksingularities.registrer.ui.components.ShutterButton
import com.blacksingularities.registrer.ui.components.ZoomControls
import com.blacksingularities.registrer.ui.components.formatClock
import com.blacksingularities.registrer.ui.components.formatShutter
import com.blacksingularities.registrer.ui.components.imageRect
import com.blacksingularities.registrer.ui.theme.MonoDigits
import com.blacksingularities.registrer.ui.theme.Palette
import com.blacksingularities.registrer.update.UpdateState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.roundToInt

private val BAR = 56.dp
private val SIDE_RAIL = 76.dp
private val SHUTTER_RAIL = 132.dp

/**
 * Ekran aparatu. Okno nigdy się nie obraca – interfejs dopasowuje się do orientacji, w której
 * trzymasz telefon: ikony obracają się płynnie, a informacje w kadrze (plakietki, komunikaty,
 * odliczanie, aktualizacje) układają się „do góry” względem patrzącego. Układ zależy od kształtu
 * okna, więc działa także na zewnętrznym i wewnętrznym ekranie folda.
 */
@Composable
fun CameraScreen(vm: CameraViewModel, onToggleRearDisplay: () -> Unit = {}) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptics = vm.global.haptics

    val previewView = remember {
        PreviewView(context).apply {
            // TextureView – tylko jego obraz można rozmyć pod szklanymi panelami.
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FIT_CENTER
        }
    }

    val bindKey = vm.bindKey
    LaunchedEffect(bindKey, lifecycleOwner) {
        vm.bind(lifecycleOwner, previewView.surfaceProvider)
    }

    // Płynny zoom: interpolacja w przestrzeni logarytmicznej, zależna od czasu klatki.
    LaunchedEffect(Unit) {
        var current = vm.zoomCurrent
        var lastFrame: Long
        while (true) {
            snapshotFlow { vm.zoomTarget to vm.zoomCurrent }.first { (t, c) -> abs(ln(t / c)) > 0.0015f || abs(current - c) > 0.001f }
            current = vm.zoomCurrent
            lastFrame = 0L
            while (abs(ln(vm.zoomTarget / current)) > 0.0015f) {
                val now = withFrameNanos { it }
                val dt = if (lastFrame == 0L) 1f / 60f else ((now - lastFrame) / 1e9f).coerceIn(0f, 0.1f)
                lastFrame = now
                val alpha = 1f - exp(-dt * vm.zoomSpeed)
                current = exp(ln(current) + (ln(vm.zoomTarget) - ln(current)) * alpha)
                vm.applyZoomFrame(current)
            }
            current = vm.zoomTarget
            vm.applyZoomFrame(current)
        }
    }

    // Orientacja fizyczna telefonu (z histerezą, by nie migotało na granicy 45°).
    DisposableEffect(Unit) {
        val listener = object : OrientationEventListener(context) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN) return
                val current = vm.deviceRotation
                var diff = abs(orientation - current)
                if (diff > 180) diff = 360 - diff
                if (diff < 55) return
                vm.deviceRotation = ((orientation + 45) / 90 * 90) % 360
            }
        }
        listener.enable()
        onDispose { listener.disable() }
    }
    // Obrót wyświetlacza (≠ 0 tylko w trybie „Obracaj ekran”). Interfejs obraca się o różnicę
    // między fizyczną orientacją telefonu a orientacją ekranu.
    val configuration = LocalConfiguration.current
    val view = LocalView.current
    val displaySurfaceRotation = remember(configuration) { view.display?.rotation ?: Surface.ROTATION_0 }
    val displayDegrees = when (displaySurfaceRotation) {
        Surface.ROTATION_90 -> 90
        Surface.ROTATION_180 -> 180
        Surface.ROTATION_270 -> 270
        else -> 0
    }
    LaunchedEffect(displaySurfaceRotation, vm.holdOrientation) {
        if (vm.displayRotation != displaySurfaceRotation && !vm.holdOrientation) vm.displayRotation = displaySurfaceRotation
    }
    val relativeRotation = (vm.deviceRotation - (360 - displayDegrees) % 360 + 360) % 360

    var rotationAccum by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(relativeRotation) {
        val target = -relativeRotation.toFloat()
        var delta = (target - rotationAccum) % 360f
        if (delta > 180f) delta -= 360f
        if (delta < -180f) delta += 360f
        rotationAccum += delta
    }
    val iconRotation by animateFloatAsState(rotationAccum, tween(320), label = "iconRotation")
    val uprightTurn = (360 - relativeRotation) % 360

    // Powrót do aplikacji: przywrócenie przerwanej instalacji / okresowe sprawdzenie wersji.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) vm.onForeground()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var showSettings by remember { mutableStateOf(false) }
    LaunchedEffect(vm.message) {
        if (vm.message != null) {
            delay(3200)
            vm.clearMessage()
        }
    }

    val ui = Ui(vm, iconRotation, uprightTurn, displayDegrees, haptics, onSettings = { showSettings = true }, onFold = onToggleRearDisplay)

    val hazeState = rememberHazeState()
    BackHandler(enabled = showSettings) { showSettings = false }
    GlassScope(haze = hazeState, haptics = haptics) {
        BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black)) {
            val wide = maxWidth > maxHeight * 1.05f
            if (wide) WideLayout(ui, previewView) else TallLayout(ui, previewView)
        }

        AnimatedVisibility(
            visible = showSettings,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(220)),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.18f))
                    .pointerInput(Unit) { detectTapGestures { showSettings = false } },
            )
        }
        AnimatedVisibility(
            visible = showSettings,
            enter = slideInVertically(spring(dampingRatio = 0.86f, stiffness = 420f)) { it } + fadeIn(),
            exit = slideOutVertically(tween(220)) { it } + fadeOut(tween(200)),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            SettingsSheet(
            initialMode = vm.mode,
            modes = vm.availableModes,
            profiles = vm.profiles,
            global = vm.global,
            caps = vm.caps,
            locked = vm.busy,
            update = vm.update,
            onProfile = { m, t -> vm.updateProfile(m, t) },
            onGlobal = vm::updateGlobal,
            onReset = vm::resetProfile,
            onCheckUpdate = { vm.checkForUpdates(manual = true) },
            onDownloadUpdate = vm::downloadUpdate,
            onInstallUpdate = { vm.installUpdate() },
            onDismiss = { showSettings = false },
            )
        }
    }
}

/** Wspólny kontekst dla obu układów. */
private class Ui(
    val vm: CameraViewModel,
    val iconRotation: Float,
    val uprightTurn: Int,
    val displayDegrees: Int,
    val haptics: Boolean,
    val onSettings: () -> Unit,
    val onFold: () -> Unit,
)

// ==================================================================== układy

/** Okno wyższe niż szersze: telefon, ekran zewnętrzny folda. */
@Composable
private fun TallLayout(ui: Ui, previewView: PreviewView) {
    val vm = ui.vm
    val insets = WindowInsets.safeDrawing.asPaddingValues()
    val top = insets.calculateTopPadding() + BAR
    Box(Modifier.fillMaxSize()) {
        PreviewArea(ui, previewView, centered = false, ratio = vm.effectiveAspect.ratio, modifier = Modifier.fillMaxSize().padding(top = top))

        if (vm.mode == CaptureMode.AUDIO) AudioPane(vm, PaddingValues(top = top, bottom = 250.dp))

        Box(
            Modifier
                .align(Alignment.TopCenter)
                .padding(top = insets.calculateTopPadding())
                .fillMaxWidth()
                .height(BAR)
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (isVideoRecording(vm)) RecordingClock(vm, ui.iconRotation) else TopControls(ui, vertical = false)
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, bottom = insets.calculateBottomPadding() + 6.dp)
                .glass(RoundedCornerShape(30.dp), GlassLevel.Regular)
                .padding(bottom = 8.dp, top = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CaptureTools(ui)
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                ShutterCluster(ui)
            }
        }
    }
}

/** Okno szersze niż wyższe: tablet lub wewnętrzny ekran folda o naturalnej orientacji poziomej. */
@Composable
private fun WideLayout(ui: Ui, previewView: PreviewView) {
    val vm = ui.vm
    val insets = WindowInsets.safeDrawing.asPaddingValues()
    Row(
        Modifier
            .fillMaxSize()
            .padding(top = insets.calculateTopPadding(), bottom = insets.calculateBottomPadding()),
    ) {
        Column(
            Modifier
                .width(SIDE_RAIL)
                .fillMaxHeight()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (isVideoRecording(vm)) RecordingClock(vm, ui.iconRotation) else TopControls(ui, vertical = true)
        }
        Box(Modifier.weight(1f).fillMaxHeight()) {
            PreviewArea(ui, previewView, centered = true, ratio = 1f / vm.effectiveAspect.ratio, modifier = Modifier.fillMaxSize())
            if (vm.mode == CaptureMode.AUDIO) AudioPane(vm, PaddingValues(top = 16.dp, bottom = 120.dp))
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .glass(RoundedCornerShape(28.dp), GlassLevel.Regular)
                    .padding(top = 10.dp, bottom = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CaptureTools(ui)
            }
        }
        Column(
            Modifier
                .width(SHUTTER_RAIL)
                .fillMaxHeight()
                .padding(vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            ShutterCluster(ui)
        }
    }
}

// ==================================================================== podgląd

@Composable
private fun PreviewArea(ui: Ui, previewView: PreviewView, centered: Boolean, ratio: Float, modifier: Modifier) {
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

    BoxWithConstraints(modifier) {
        val containerSize = with(density) { Size(maxWidth.toPx(), maxHeight.toPx()) }
        val rect = imageRect(containerSize, ratio, centered)

        // Widok kamery zajmuje dokładnie prostokąt kadru – obraz jest wyśrodkowany.
        val haze = LocalHaze.current
        AndroidView(
            factory = { previewView },
            modifier = Modifier
                .offset { IntOffset(rect.left.roundToInt(), rect.top.roundToInt()) }
                .size(with(density) { rect.width.toDp() }, with(density) { rect.height.toDp() })
                .then(if (haze != null) Modifier.hazeSource(haze) else Modifier),
        )

        val streaming = streamState == PreviewView.StreamState.STREAMING && vm.cameraReady
        val curtain by animateFloatAsState(if (streaming) 0f else 1f, tween(if (streaming) 380 else 120), label = "curtain")
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = curtain }.background(Color.Black))

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
                        vm.focus(previewView.meteringPointFactory.createPoint(o.x - rect.left, o.y - rect.top), lock)
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

        GridOverlay(vm.cfg.grids, ratio, Modifier.fillMaxSize(), centered)
        if (vm.cfg.level && vm.mode != CaptureMode.AUDIO) LevelOverlay(ratio, ui.haptics, Modifier.fillMaxSize(), centered, ui.displayDegrees)

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

        val bins = vm.histogram
        if (bins != null && vm.cfg.histogram && vm.mode.isPhoto) {
            DraggableHistogram(vm, bins, rect, ui.iconRotation)
        }

        Box(Modifier.fillMaxSize().graphicsLayer { alpha = flashAlpha.value }.background(Color.Black))
    }
}

/**
 * Histogram, który można przeciągnąć w dowolne miejsce kadru; pozycja jest zapamiętywana
 * (jako ułamek kadru, więc pasuje też po zmianie proporcji czy ekranu folda).
 */
@Composable
private fun DraggableHistogram(vm: CameraViewModel, bins: IntArray, rect: Rect, rotation: Float) {
    val density = LocalDensity.current
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val w = with(density) { 112.dp.toPx() }
    val h = with(density) { 52.dp.toPx() }
    val margin = with(density) { 8.dp.toPx() }
    val availW = (rect.width - w - 2 * margin).coerceAtLeast(1f)
    val availH = (rect.height - h - 2 * margin).coerceAtLeast(1f)
    var pos by remember { mutableStateOf(Offset(vm.global.histogramX, vm.global.histogramY)) }
    var dragging by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (dragging) 1.12f else 1f, spring(dampingRatio = 0.55f, stiffness = 600f), label = "histScale")
    HistogramView(
        bins,
        Modifier
            .offset {
                IntOffset(
                    (rect.left + margin + pos.x * availW).roundToInt(),
                    (rect.top + margin + pos.y * availH).roundToInt(),
                )
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                rotationZ = rotation
                shadowElevation = if (dragging) 12f else 0f
            }
            .pointerInput(rect) {
                detectDragGestures(
                    onDragStart = {
                        dragging = true
                        if (vm.global.haptics) haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    },
                    onDragEnd = {
                        dragging = false
                        vm.updateGlobal { it.copy(histogramX = pos.x, histogramY = pos.y) }
                    },
                    onDragCancel = { dragging = false },
                ) { change, drag ->
                    change.consume()
                    pos = Offset(
                        (pos.x + drag.x / availW).coerceIn(0f, 1f),
                        (pos.y + drag.y / availH).coerceIn(0f, 1f),
                    )
                }
            },
    )
}

/**
 * Warstwa ułożona w prostokącie kadru, obrócona o [turn] stopni. Przy obrocie o 90°/270°
 * szerokość i wysokość zamieniają się, więc treść wypełnia kadr tak, jak widzi go użytkownik.
 * Zmiana orientacji to krótkie przenikanie zamiast „pływającego” tekstu.
 */
@Composable
private fun UprightFrame(rect: Rect, turn: Int, content: @Composable BoxScope.() -> Unit) {
    val density = LocalDensity.current
    if (rect.width <= 0f) return
    val alpha = remember { Animatable(1f) }
    var shown by remember { mutableIntStateOf(turn) }
    LaunchedEffect(turn) {
        if (turn == shown) return@LaunchedEffect
        alpha.animateTo(0f, tween(110))
        shown = turn
        alpha.animateTo(1f, tween(200))
    }
    val swap = shown % 180 != 0
    val w = with(density) { (if (swap) rect.height else rect.width).toDp() }
    val h = with(density) { (if (swap) rect.width else rect.height).toDp() }
    Box(
        Modifier
            .offset { IntOffset(rect.left.roundToInt(), rect.top.roundToInt()) }
            .size(with(density) { rect.width.toDp() }, with(density) { rect.height.toDp() }),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .requiredSize(w, h)
                .graphicsLayer {
                    rotationZ = shown.toFloat()
                    this.alpha = alpha.value
                },
            content = content,
        )
    }
}

@Composable
private fun BoxScope.FrameInfo(ui: Ui) {
    val vm = ui.vm
    Column(
        Modifier
            .align(Alignment.TopCenter)
            .fillMaxWidth()
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (vm.global.exposureInfo && vm.mode != CaptureMode.AUDIO) ExposureStrip(vm, Modifier.align(Alignment.CenterHorizontally))
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (vm.focusLocked) Badge("AE/AF · zablokowane  ✕", color = Palette.Amber, onClick = vm::unlockFocus)
                if (vm.intervalRunning) {
                    val total = vm.cfg.intervalCount
                    Badge("Interwał · ${vm.intervalShot}${if (total > 0) "/$total" else ""}", color = Palette.Amber)
                }
                if (vm.rearDisplayActive) Badge("Selfie głównym aparatem", color = Palette.Amber)
            }
        }
        UpdateBanner(
            state = vm.update,
            visible = !vm.busy,
            onDownload = vm::downloadUpdate,
            onInstall = { vm.installUpdate() },
            onDismiss = vm::dismissUpdate,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        AnimatedVisibility(
            visible = vm.message != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
            Text(
                vm.message ?: "",
                color = Color.White,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .glass(RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }
    }

    AnimatedVisibility(
        visible = vm.countdown != null,
        enter = fadeIn() + scaleIn(initialScale = 1.6f),
        exit = fadeOut() + scaleOut(targetScale = 0.6f),
        modifier = Modifier.align(Alignment.Center),
    ) {
        Text("${vm.countdown ?: ""}", fontSize = 120.sp, fontWeight = FontWeight.Thin, color = Color.White)
    }
    vm.processing?.let { p ->
        Text(
            p.label,
            color = Palette.Amber,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 14.dp)
                .glass(RoundedCornerShape(50))
                .padding(horizontal = 14.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun AudioPane(vm: CameraViewModel, padding: PaddingValues) {
    AudioStudio(
        state = vm.audioState,
        millis = vm.audioMillis,
        amplitudes = vm.amplitudes,
        settings = vm.cfg,
        recordings = vm.recordings,
        playingId = vm.playingId,
        playbackPosition = vm::playbackPosition,
        onPlay = vm::togglePlay,
        onDelete = vm::deleteRecording,
        contentPadding = padding,
    )
}

// ==================================================================== sterowanie

/** Panel Pro / szybkie ustawienie trybu, obiektywy z przełącznikiem zoomu oraz pokrętło trybów. */
@Composable
private fun CaptureTools(ui: Ui) {
    val vm = ui.vm
    if (vm.mode != CaptureMode.PRO) ModeQuickControl(ui, Modifier.padding(bottom = 8.dp))
    if (vm.mode == CaptureMode.PRO && vm.cameraReady) {
        ProPanel(
            caps = vm.caps,
            pro = vm.pro,
            hud = vm.hud,
            evIndex = vm.evIndex,
            evMin = vm.evMin,
            evMax = vm.evMax,
            evStep = vm.evStep,
            haptics = ui.haptics,
            onPro = vm::updatePro,
            onEv = vm::setEv,
        )
    }
    if (vm.mode != CaptureMode.AUDIO && (vm.cameraReady || vm.caps.lensZooms.isNotEmpty())) {
        ZoomControls(
            presets = vm.zoomPresets,
            current = vm.displayZoom,
            min = vm.displayZoomMin,
            max = vm.displayZoomMax,
            iconRotation = ui.iconRotation,
            haptics = ui.haptics,
            onPreset = vm::selectZoomPreset,
            onRate = vm::nudgeZoom,
            modifier = Modifier.padding(bottom = 4.dp),
            focusAvailable = vm.focusAvailable,
            focusMode = vm.rockerFocus,
            focusLabel = vm.pro.focusDiopters?.let { com.blacksingularities.registrer.ui.components.formatFocus(it) },
            onToggleFocus = vm::toggleRockerFocus,
            onFocusRate = vm::nudgeFocus,
        )
    }
    ModeDial(
        modes = vm.availableModes,
        selected = vm.mode,
        enabled = !vm.busy,
        haptics = ui.haptics,
        onSelect = vm::selectMode,
    )
}

/**
 * Najważniejsze ustawienie bieżącego trybu, zawsze na ekranie: rozmycie w Portrecie, liczba
 * klatek w Nocy, rozpiętość w HDR, jakość w Audio, a w pozostałych trybach kompensacja EV.
 * Gdy działa rozszerzenie sprzętowe producenta (bez regulacji) – kompensacja EV.
 */
@Composable
private fun ModeQuickControl(ui: Ui, modifier: Modifier = Modifier) {
    val vm = ui.vm
    val cfg = vm.cfg
    val rot = ui.iconRotation
    val ext = vm.session.extension

    @Composable
    fun ev() {
        if (vm.evMax <= vm.evMin || !vm.cameraReady) return
        val values = (vm.evMin..vm.evMax).toList()
        QuickControl(
            icon = Icons.Rounded.Exposure,
            label = "Kompensacja ekspozycji",
            options = values.map { String.format(java.util.Locale.US, "%+.1f EV", it * vm.evStep) },
            selected = values.indexOf(vm.evIndex).coerceAtLeast(0),
            defaultIndex = values.indexOf(0).takeIf { it >= 0 },
            rotation = rot,
            modifier = modifier,
        ) { i -> vm.setEv(values[i]) }
    }

    when (vm.mode) {
        CaptureMode.PORTRAIT -> if (ext == ExtensionMode.BOKEH || ext == ExtensionMode.FACE_RETOUCH) {
            ev()
        } else {
            val values = (0..10).map { it * 10 }
            QuickControl(
                icon = Icons.Rounded.BlurOn,
                label = "Rozmycie tła",
                options = values.map { "$it%" },
                selected = values.indexOfFirst { it >= cfg.portraitBlur }.let { if (it < 0) values.lastIndex else it },
                defaultIndex = values.indexOf(60),
                rotation = rot,
                modifier = modifier,
            ) { i -> vm.updateProfile { it.copy(portraitBlur = values[i]) } }
        }
        CaptureMode.NIGHT -> if (ext == ExtensionMode.NIGHT) {
            ev()
        } else {
            val values = listOf(0, 4, 6, 8, 10, 12, 16)
            QuickControl(
                icon = Icons.Rounded.Layers,
                label = "Liczba klatek nocnych",
                options = values.map { if (it == 0) "Auto ${vm.autoNightFrames()}" else "×$it" },
                selected = values.indexOf(cfg.nightFrames).coerceAtLeast(0),
                defaultIndex = 0,
                rotation = rot,
                modifier = modifier,
            ) { i -> vm.updateProfile { it.copy(nightFrames = values[i]) } }
        }
        CaptureMode.HDR -> if (ext == ExtensionMode.HDR) {
            ev()
        } else {
            val values = listOf(1.0f, 1.3f, 1.7f, 2.0f, 2.5f)
            QuickControl(
                icon = Icons.Rounded.HdrOn,
                label = "Rozpiętość ekspozycji HDR",
                options = values.map { String.format(java.util.Locale.US, "±%.1f EV", it) },
                selected = values.indexOfFirst { abs(it - cfg.hdrStops) < 0.05f }.let { if (it < 0) 2 else it },
                defaultIndex = 2,
                rotation = rot,
                modifier = modifier,
            ) { i -> vm.updateProfile { it.copy(hdrStops = values[i]) } }
        }
        CaptureMode.AUDIO -> {
            val values = com.blacksingularities.registrer.data.AudioPreset.entries
            QuickControl(
                icon = Icons.Rounded.GraphicEq,
                label = "Jakość nagrania: ${cfg.audioPreset.description}",
                options = values.map { it.label },
                selected = values.indexOf(cfg.audioPreset),
                defaultIndex = values.indexOf(com.blacksingularities.registrer.data.AudioPreset.MUSIC),
                rotation = rot,
                enabled = vm.audioState == RecState.IDLE,
                modifier = modifier,
            ) { i -> vm.updateProfile { it.copy(audioPreset = values[i]) } }
        }
        CaptureMode.PRO -> Unit
        else -> ev()
    }
}

/** Miniatura / kadr z wideo, migawka, przełącznik obiektywu / pauza. */
@Composable
private fun ShutterCluster(ui: Ui) {
    val vm = ui.vm
    val context = LocalContext.current
    val isAudio = vm.mode == CaptureMode.AUDIO
    val rot = ui.iconRotation

    Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
        val thumb = vm.thumbnail
        if (vm.mode.isVideo && vm.recState != RecState.IDLE && vm.session.snapshotInVideo) {
            RoundIcon(Icons.Rounded.PhotoCamera, "Zdjęcie z wideo", rot, size = 56.dp, onClick = vm::snapshot)
        } else if (!isAudio && thumb != null) {
            Image(
                bitmap = thumb.asImageBitmap(),
                contentDescription = "Ostatnie ujęcie",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(52.dp)
                    .graphicsLayer { rotationZ = rot }
                    .clip(CircleShape)
                    .border(1.5.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.9f), Color.White.copy(alpha = 0.3f))), CircleShape)
                    .reactive("Galeria") {
                        val uri = vm.lastUri ?: return@reactive
                        val type = if (vm.lastIsVideo) "video/*" else "image/*"
                        val review = Intent("android.provider.action.REVIEW").setDataAndType(uri, type)
                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        try {
                            context.startActivity(review)
                        } catch (_: ActivityNotFoundException) {
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW).setDataAndType(uri, type).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
                                )
                            }
                        }
                    },
            )
        }
    }

    val active = when {
        vm.mode.isVideo -> vm.recState != RecState.IDLE
        isAudio -> vm.audioState != RecState.IDLE
        else -> vm.countdown != null || vm.intervalRunning || vm.bursting
    }
    ShutterButton(
        mode = vm.mode,
        active = active,
        progress = vm.processing?.progress,
        processing = vm.processing != null,
        enabled = vm.processing == null && (vm.cameraReady || isAudio),
        onClick = vm::onShutter,
        counter = if (vm.bursting) vm.burstCount else null,
        onLongPress = vm::onShutterLongPress,
        onRelease = vm::onShutterRelease,
        modifier = Modifier.graphicsLayer { rotationZ = rot },
    )

    val recording = (vm.mode.isVideo && vm.recState != RecState.IDLE) || (isAudio && vm.audioState != RecState.IDLE)
    val paused = vm.recState == RecState.PAUSED || vm.audioState == RecState.PAUSED
    when {
        recording -> RoundIcon(
            if (paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
            if (paused) "Wznów" else "Pauza",
            rot,
            size = 56.dp,
            onClick = vm::togglePause,
        )
        !isAudio && vm.caps.hasFront && vm.caps.hasBack && !vm.rearDisplayActive -> {
            val spin by animateFloatAsState(
                if (vm.lensFacing == CameraSelector.LENS_FACING_FRONT) 180f else 0f,
                tween(420),
                label = "switch",
            )
            RoundIcon(Icons.Rounded.Cameraswitch, "Zmień obiektyw", rot + spin, size = 56.dp, enabled = !vm.busy, onClick = vm::switchLens)
        }
        else -> Spacer(Modifier.size(56.dp))
    }
}

/** Lampa, samowyzwalacz, proporcje, format, siatki, fold, ustawienia – ikony; nazwy po przytrzymaniu. */
@Composable
private fun TopControls(ui: Ui, vertical: Boolean) {
    val vm = ui.vm
    val rot = ui.iconRotation
    val items: @Composable () -> Unit = {
        if (vm.mode != CaptureMode.AUDIO) {
            val cfg = vm.cfg
            val (flashIcon, flashName) = when (cfg.flash) {
                FlashSetting.OFF -> Icons.Rounded.FlashOff to "Lampa wyłączona"
                FlashSetting.AUTO -> Icons.Rounded.FlashAuto to "Lampa automatyczna"
                FlashSetting.ON -> Icons.Rounded.FlashOn to "Lampa włączona"
                FlashSetting.TORCH -> Icons.Rounded.FlashlightOn to "Latarka"
            }
            RoundIcon(
                flashIcon,
                if (vm.caps.hasFlash) flashName else "Brak lampy",
                rot,
                active = cfg.flash != FlashSetting.OFF,
                enabled = vm.caps.hasFlash,
                onClick = vm::cycleFlash,
            )
            if (vm.mode.isPhoto) {
                RoundIcon(
                    Icons.Rounded.Timer,
                    if (cfg.timerSeconds == 0) "Samowyzwalacz wyłączony" else "Samowyzwalacz ${cfg.timerSeconds} s",
                    rot,
                    active = cfg.timerSeconds > 0,
                    badge = cfg.timerSeconds.takeIf { it > 0 }?.toString(),
                    onClick = vm::cycleTimer,
                )
                val frame = remember(cfg.aspect) { frameIcon(cfg.aspect) }
                RoundIcon(
                    frame,
                    "Proporcje ${cfg.aspect.label}",
                    rot,
                    enabled = !vm.busy,
                    onClick = vm::cycleAspect,
                )
                if ((vm.mode == CaptureMode.PHOTO || vm.mode == CaptureMode.PRO) && vm.caps.photoFormats.size > 1) {
                    val (icon, name) = when (cfg.photoFormat) {
                        PhotoFormat.JPEG -> Icons.Rounded.Image to "Format JPEG"
                        PhotoFormat.ULTRA_HDR -> Icons.Rounded.HdrOn to "Format Ultra HDR"
                        PhotoFormat.RAW_JPEG -> Icons.Rounded.RawOn to "Format RAW + JPEG"
                        PhotoFormat.RAW -> Icons.Rounded.RawOn to "Format RAW (DNG)"
                    }
                    RoundIcon(
                        icon,
                        name,
                        rot,
                        active = cfg.photoFormat != PhotoFormat.JPEG,
                        badge = if (cfg.photoFormat == PhotoFormat.RAW_JPEG) "+" else null,
                        enabled = !vm.busy,
                        onClick = vm::cyclePhotoFormat,
                    )
                }
            } else {
                if (vm.mode == CaptureMode.VIDEO) {
                    TextChip(
                        cfg.videoResolution.label,
                        rot,
                        label = "Rozdzielczość ${cfg.videoResolution.label}",
                        enabled = !vm.busy && vm.caps.videoResolutions.size > 1,
                        onClick = vm::cycleVideoResolution,
                    )
                }
                val fpsChoices = if (vm.mode == CaptureMode.SLOWMO) vm.caps.slowMoRanges.size else vm.videoFpsOptions().size
                TextChip(
                    vm.fpsLabel.removeSuffix(" kl/s"),
                    rot,
                    label = "Klatkaż ${vm.fpsLabel}",
                    icon = Icons.Rounded.Speed,
                    enabled = !vm.busy && fpsChoices > 1,
                    active = vm.mode == CaptureMode.VIDEO && cfg.videoFps != com.blacksingularities.registrer.data.VideoFps.AUTO,
                    onClick = vm::cycleVideoFps,
                )
                if (vm.mode == CaptureMode.VIDEO && vm.caps.videoHlg) {
                    RoundIcon(
                        if (cfg.videoHdr) Icons.Rounded.HdrOn else Icons.Rounded.HdrOff,
                        if (cfg.videoHdr) "Wideo HDR (HLG)" else "Wideo SDR",
                        rot,
                        active = cfg.videoHdr,
                        enabled = !vm.busy,
                        onClick = { vm.updateProfile { it.copy(videoHdr = !it.videoHdr) } },
                    )
                }
            }
            GridButton(vm, rot)
            if (vm.rearDisplayAvailable || vm.rearDisplayActive) {
                RoundIcon(
                    Icons.Rounded.FlipToBack,
                    if (vm.rearDisplayActive) "Wróć na ekran wewnętrzny" else "Selfie głównym aparatem",
                    rot,
                    active = vm.rearDisplayActive,
                    enabled = !vm.busy,
                    onClick = ui.onFold,
                )
            }
        } else {
            RoundIcon(Icons.Rounded.Mic, "Dyktafon", rot, active = true, onClick = {})
        }
        RoundIcon(Icons.Rounded.Settings, "Ustawienia", rot, onClick = ui.onSettings)
    }
    if (vertical) {
        Column(
            Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) { items() }
    } else {
        // Równo rozłożone, a gdy przycisków jest więcej niż miejsca (wąski ekran) – przewijane.
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            Row(
                Modifier
                    .horizontalScroll(rememberScrollState())
                    .widthIn(min = maxWidth),
                horizontalArrangement = SpreadOrPack,
                verticalAlignment = Alignment.CenterVertically,
            ) { items() }
        }
    }
}

/** Ikona ramki o proporcjach kadru (pionowo: 3:4 albo 9:16). */
private fun frameIcon(aspect: com.blacksingularities.registrer.data.AspectRatioMode): ImageVector {
    val h = 19f
    val w = h * aspect.ratio
    val l = 12f - w / 2f
    val t = 12f - h / 2f
    val r = 2.5f
    return ImageVector.Builder("frame_${aspect.name}", 24.dp, 24.dp, 24f, 24f).apply {
        path(
            fill = null,
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2f,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(l + r, t)
            lineTo(l + w - r, t)
            quadTo(l + w, t, l + w, t + r)
            lineTo(l + w, t + h - r)
            quadTo(l + w, t + h, l + w - r, t + h)
            lineTo(l + r, t + h)
            quadTo(l, t + h, l, t + h - r)
            lineTo(l, t + r)
            quadTo(l, t, l + r, t)
            close()
        }
    }.build()
}

/**
 * Pasek parametrów na samej górze kadru: ISO, czas naświetlania, przysłona, kompensacja EV
 * i zoom. Wartości ustawione ręcznie (tryb Pro) są wyróżnione bursztynem.
 */
@Composable
private fun ExposureStrip(vm: CameraViewModel, modifier: Modifier = Modifier) {
    val hud = vm.hud
    val pro = if (vm.mode == CaptureMode.PRO) vm.pro else null
    val items = buildList {
        val iso = pro?.iso ?: hud?.iso
        add(Triple("ISO", iso?.toString() ?: "—", pro?.iso != null))
        val shutter = pro?.exposureNs ?: hud?.exposureNs
        add(Triple("S", shutter?.let { formatShutter(it) } ?: "—", pro?.exposureNs != null))
        hud?.aperture?.let { add(Triple("f/", String.format(java.util.Locale.US, "%.1f", it), false)) }
        val ev = vm.evIndex * vm.evStep
        add(Triple("EV", String.format(java.util.Locale.US, "%+.1f", ev), vm.evIndex != 0))
        add(Triple("", com.blacksingularities.registrer.ui.components.formatZoom(vm.displayZoom) + "×", false))
    }
    Row(
        modifier
            .glass(RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEach { (label, value, manual) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (label.isNotEmpty()) {
                    Text(label, color = Palette.TextFaint, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(3.dp))
                }
                Text(value, style = MonoDigits, fontSize = 12.sp, color = if (manual) Palette.Amber else Color.White)
            }
        }
    }
}

/** Rozkłada elementy na całą szerokość; gdy się nie mieszczą – ciasno, z odstępem 6 dp. */
private object SpreadOrPack : Arrangement.Horizontal {
    override val spacing = 6.dp

    override fun androidx.compose.ui.unit.Density.arrange(
        totalSize: Int,
        sizes: IntArray,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        outPositions: IntArray,
    ) {
        val minGap = spacing.roundToPx()
        val content = sizes.sum()
        val gap = if (sizes.size > 1) maxOf(minGap, (totalSize - content) / (sizes.size - 1)) else 0
        var x = 0
        sizes.forEachIndexed { i, w ->
            outPositions[i] = x
            x += w + gap
        }
    }
}

private fun isVideoRecording(vm: CameraViewModel) = vm.mode.isVideo && vm.recState != RecState.IDLE

@Composable
private fun RecordingClock(vm: CameraViewModel, rotation: Float) {
    val blink = remember { Animatable(1f) }
    LaunchedEffect(vm.recState) {
        while (vm.recState == RecState.RECORDING) {
            blink.animateTo(0.2f, tween(600))
            blink.animateTo(1f, tween(600))
        }
        blink.snapTo(1f)
    }
    Row(
        Modifier
            .graphicsLayer { rotationZ = rotation }
            .glass(RoundedCornerShape(50))
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(9.dp)
                .graphicsLayer { alpha = blink.value }
                .clip(CircleShape)
                .background(if (vm.recState == RecState.PAUSED) Palette.Amber else Palette.Record),
        )
        Spacer(Modifier.width(8.dp))
        Text(formatClock(vm.recNanos / 1_000_000, tenths = false), style = MonoDigits, color = Color.White)
        if (vm.recMuted) Text("  ·  bez dźwięku", fontSize = 11.sp, color = Palette.TextDim)
    }
}

// ==================================================================== drobne elementy

@Composable
private fun Badge(text: String, mono: Boolean = false, color: Color = Color.White, onClick: (() -> Unit)? = null) {
    Text(
        text,
        color = color,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.8.sp,
        style = if (mono) MonoDigits else TextStyle.Default,
        modifier = Modifier
            .glass(RoundedCornerShape(50), active = color == Palette.Amber)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

/** Siatki: menu z zaznaczaniem – można włączyć kilka naraz. */
@Composable
private fun GridButton(vm: CameraViewModel, rotation: Float) {
    var open by remember { mutableStateOf(false) }
    val grids = vm.cfg.grids
    Box {
        RoundIcon(
            Icons.Rounded.GridOn,
            "Siatki",
            rotation,
            tint = if (grids.isEmpty()) Palette.TextFaint else Color.White,
            onClick = { open = true },
        )
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            containerColor = Palette.PanelHigh.copy(alpha = 0.9f),
            shape = RoundedCornerShape(18.dp),
        ) {
            GridType.entries.forEach { g ->
                val on = g in grids
                DropdownMenuItem(
                    text = { Text(g.label, color = if (on) Palette.Amber else Color.White, fontWeight = if (on) FontWeight.Bold else FontWeight.Normal) },
                    leadingIcon = {
                        Icon(
                            if (on) Icons.Rounded.CheckBox else Icons.Rounded.CheckBoxOutlineBlank,
                            null,
                            tint = if (on) Palette.Amber else Palette.TextDim,
                        )
                    },
                    onClick = { vm.updateProfile { it.copy(grids = if (g in it.grids) it.grids - g else it.grids + g) } },
                )
            }
            if (grids.isNotEmpty()) {
                DropdownMenuItem(
                    text = { Text("Wyłącz wszystkie", color = Palette.TextDim) },
                    leadingIcon = { Icon(Icons.Rounded.Close, null, tint = Palette.TextDim) },
                    onClick = {
                        vm.updateProfile { it.copy(grids = emptySet()) }
                        open = false
                    },
                )
            }
        }
    }
}

/** Informacja o nowej wersji – sprawdzanej automatycznie przy starcie i po powrocie do aplikacji. */
@Composable
private fun UpdateBanner(
    state: UpdateState,
    visible: Boolean,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val content: Triple<String, String?, (() -> Unit)?>? = when (state) {
        is UpdateState.Available -> Triple("Nowa wersja ${state.info.version}", "Pobierz", onDownload)
        is UpdateState.Downloading -> Triple("Pobieranie ${state.info.version} · ${(state.progress * 100).roundToInt()}%", null, null)
        is UpdateState.Ready -> Triple("Wersja ${state.info.version} gotowa", "Zainstaluj", onInstall)
        is UpdateState.Installing -> Triple("Instalowanie ${state.info.version}…", null, null)
        is UpdateState.Failed -> Triple(state.message, null, null)
        else -> null
    }
    AnimatedVisibility(visible = visible && content != null, enter = fadeIn(), exit = fadeOut(), modifier = modifier) {
        val (label, action, handler) = content ?: return@AnimatedVisibility
        Row(
            Modifier
                .glass(RoundedCornerShape(50))
                .padding(start = 14.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.SystemUpdate, null, tint = Palette.Amber, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(label, color = Color.White, fontSize = 13.sp, maxLines = 1)
            if (action != null && handler != null) {
                Spacer(Modifier.width(8.dp))
                Text(
                    action,
                    color = Palette.Ink,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Palette.Amber)
                        .clickable(onClick = handler)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
            if (state !is UpdateState.Downloading && state !is UpdateState.Installing) {
                Box(
                    Modifier.size(32.dp).clip(CircleShape).clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Close, "Zamknij", tint = Palette.TextDim, modifier = Modifier.size(16.dp))
                }
            } else {
                Spacer(Modifier.width(10.dp))
            }
        }
    }
}

@Composable
private fun RoundIcon(
    icon: ImageVector,
    description: String,
    rotation: Float,
    size: Dp = 44.dp,
    tint: Color? = null,
    active: Boolean = false,
    enabled: Boolean = true,
    badge: String? = null,
    onClick: () -> Unit,
) {
    GlassIconButton(
        icon = icon,
        label = description,
        rotation = rotation,
        size = size,
        active = active,
        enabled = enabled,
        tint = tint,
        badge = badge,
        onClick = onClick,
    )
}

@Composable
private fun TextChip(
    text: String,
    rotation: Float,
    label: String = text,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    active: Boolean = false,
    onClick: () -> Unit,
) {
    GlassChip(text = text, label = label, rotation = rotation, icon = icon, active = active, enabled = enabled, onClick = onClick)
}
