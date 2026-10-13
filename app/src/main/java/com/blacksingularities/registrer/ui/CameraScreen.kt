package com.blacksingularities.registrer.ui

import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import com.blacksingularities.registrer.ui.theme.activeContent
import com.blacksingularities.registrer.ui.theme.LocalNoBackground
import androidx.compose.ui.draw.clipToBounds
import com.blacksingularities.registrer.ui.theme.LocalPickerHorizontal
import com.blacksingularities.registrer.ui.theme.PickerOption
import androidx.compose.material.icons.rounded.TimerOff
import com.blacksingularities.registrer.ui.components.CompassTape
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.layout.wrapContentSize
import com.blacksingularities.registrer.ui.theme.LocalHintOnTap
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.ShutterSpeed
import androidx.compose.material.icons.rounded.Iso
import androidx.compose.material.icons.rounded.Camera
import com.blacksingularities.registrer.ui.theme.LocalBlur
import androidx.compose.runtime.CompositionLocalProvider
import com.blacksingularities.registrer.ui.theme.BlockShape
import com.blacksingularities.registrer.ui.theme.GlassValueButton
import com.blacksingularities.registrer.ui.theme.GlassToolbar
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import com.blacksingularities.registrer.ui.components.QuickControl
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Exposure
import androidx.compose.material.icons.rounded.BlurOn
import com.blacksingularities.registrer.ui.theme.reactive
import com.blacksingularities.registrer.ui.theme.glass
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
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.VideoStable
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.blacksingularities.registrer.ui.theme.rotatedLayout
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
            // SurfaceView – najwydajniejsza ścieżka podglądu.
            implementationMode = PreviewView.ImplementationMode.PERFORMANCE
            scaleType = PreviewView.ScaleType.FIT_CENTER
        }
    }

    var freezeFrame by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    val bindKey = vm.bindKey
    LaunchedEffect(bindKey, lifecycleOwner) {
        // Ostatnia klatka zostaje na ekranie podczas przełączania obiektywu / trybu
        // i płynnie przenika w nowy obraz – bez czarnego mignięcia.
        freezeFrame = runCatching { previewView.bitmap?.asImageBitmap() }.getOrNull()
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
    // W trybie „Obracaj ekran” obraca się cały ekran – elementy nie obracają się dodatkowo
    // (inaczej obracałyby się dwa razy, a nakładki przeskakiwały). Wyjątek: blokada podczas nagrywania.
    val screenFollows = vm.global.screenRotation == com.blacksingularities.registrer.data.ScreenRotation.AUTO && !vm.holdOrientation
    val relativeRotation = if (screenFollows) 0 else (vm.deviceRotation - (360 - displayDegrees) % 360 + 360) % 360

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

    var controlsTop by remember { mutableFloatStateOf(Float.MAX_VALUE) }
    val ui = Ui(
        vm, iconRotation, uprightTurn, displayDegrees, haptics,
        onSettings = { showSettings = true },
        onFold = onToggleRearDisplay,
        freeze = freezeFrame,
        controlsTop = controlsTop,
        onControlsTop = { controlsTop = it },
    )

    BackHandler(enabled = showSettings) { showSettings = false }
    GlassScope(haptics = haptics) {
        // Główny interfejs: bez teł, napisy z lekkim cieniem dla czytelności na jasnym obrazie.
        CompositionLocalProvider(
            LocalNoBackground provides true,
            androidx.compose.material3.LocalTextStyle provides androidx.compose.material3.LocalTextStyle.current.merge(
                androidx.compose.ui.text.TextStyle(
                    shadow = androidx.compose.ui.graphics.Shadow(Color.Black.copy(alpha = 0.7f), Offset(0f, 1f), 5f),
                ),
            ),
        ) {
            BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black)) {
                val wide = maxWidth > maxHeight * 1.05f
                if (wide) WideLayout(ui, previewView) else TallLayout(ui, previewView)
            }
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
            enter = slideInVertically(spring(dampingRatio = 0.9f, stiffness = 500f)) { it / 6 } + fadeIn(tween(160)),
            exit = slideOutVertically(tween(180)) { it / 6 } + fadeOut(tween(160)),
            modifier = Modifier.fillMaxSize(),
        ) {
            SettingsSheet(
            initialMode = vm.mode,
            modes = vm.availableModes,
            profiles = vm.profiles,
            global = vm.global,
            caps = vm.caps,
            locked = vm.busy,
            update = vm.update,
            landscape = vm.deviceRotation % 180 != 0,
            rotation = iconRotation,
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
    val freeze: androidx.compose.ui.graphics.ImageBitmap? = null,
    /** Górna krawędź dolnego sterowania w układzie okna – nakładki pływające jej nie zasłaniają. */
    val controlsTop: Float = Float.MAX_VALUE,
    val onControlsTop: (Float) -> Unit = {},
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
            if (isVideoRecording(vm)) RecordingClock(vm, ui.iconRotation) else CompositionLocalProvider(LocalBlur provides true) { TopControls(ui, vertical = false) }
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, bottom = insets.calculateBottomPadding() + 6.dp)
                .onGloballyPositioned { ui.onControlsTop(it.boundsInRoot().top) }
                .glass(RoundedCornerShape(28.dp), GlassLevel.Faint)
                .padding(bottom = 4.dp, top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CaptureTools(ui)
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 30.dp, vertical = 2.dp),
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
            if (isVideoRecording(vm)) RecordingClock(vm, ui.iconRotation) else CompositionLocalProvider(LocalBlur provides true) { TopControls(ui, vertical = true) }
        }
        Box(Modifier.weight(1f).fillMaxHeight()) {
            PreviewArea(ui, previewView, centered = true, ratio = 1f / vm.effectiveAspect.ratio, modifier = Modifier.fillMaxSize())
            if (vm.mode == CaptureMode.AUDIO) AudioPane(vm, PaddingValues(top = 16.dp, bottom = 120.dp))
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .onGloballyPositioned { ui.onControlsTop(it.boundsInRoot().top) }
                    .glass(RoundedCornerShape(28.dp), GlassLevel.Faint)
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

    // Zmiana proporcji (i każde inne ponowne wiązanie kamery) na chwilę chowa część sterowania
    // (EV, zoom, panel PRO). Gdy kamera nie jest gotowa, granica może się tylko podnieść –
    // dzięki temu nakładki pływające nie skaczą w dół i z powrotem.
    var settledControlsTop by remember { mutableFloatStateOf(ui.controlsTop) }
    LaunchedEffect(ui.controlsTop, vm.cameraReady) {
        settledControlsTop = if (vm.cameraReady) ui.controlsTop else minOf(settledControlsTop, ui.controlsTop)
    }

    var areaTop by remember { mutableFloatStateOf(0f) }
    BoxWithConstraints(modifier.onGloballyPositioned { areaTop = it.boundsInRoot().top }) {
        val containerSize = with(density) { Size(maxWidth.toPx(), maxHeight.toPx()) }
        // Nakładki pływające (histogram, pasek parametrów, kompas) tylko nad dolnym sterowaniem.
        // Obszar nie zależy od proporcji kadru, więc zmiana 4:3 ↔ 16:9 nie przesuwa nakładek.
        val overlayArea = Rect(
            Offset.Zero,
            Size(containerSize.width, (settledControlsTop - areaTop).coerceIn(containerSize.height * 0.3f, containerSize.height)),
        )
        val rect = imageRect(containerSize, ratio, centered)

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

        // Przytrzymanie pozwala przeciągnąć nakładkę; kosz usuwa ją z kadru.
        var overlayDragging by remember { mutableStateOf(false) }
        var overlayOverTrash by remember { mutableStateOf(false) }
        val dragState: (Boolean, Boolean) -> Unit = { dragging, overTrash ->
            overlayDragging = dragging
            overlayOverTrash = overTrash
        }
        val bins = vm.histogram

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
                start = Offset(vm.global.zoomNavigatorX, vm.global.zoomNavigatorY),
                turn = ui.uprightTurn,
                rotation = ui.iconRotation,
                haptics = ui.haptics,
                deletable = false,
                onCommit = { p -> vm.updateGlobal { it.copy(zoomNavigatorX = p.x, zoomNavigatorY = p.y) } },
                onDragState = { _, _ -> },
                onDelete = {},
            ) {
                ZoomNavigator(overview, vm.displayZoom, vm.zoomNavigatorBase)
            }
        }

        if (bins != null && vm.cfg.histogram && vm.mode.isPhoto) {
            DraggableOverlay(
                rect = overlayArea,
                start = Offset(vm.global.histogramX, vm.global.histogramY),
                turn = ui.uprightTurn,
                rotation = ui.iconRotation,
                haptics = ui.haptics,
                onCommit = { p -> vm.updateGlobal { it.copy(histogramX = p.x, histogramY = p.y) } },
                onDragState = dragState,
                onDelete = { vm.updateProfile { it.copy(histogram = false) } },
            ) {
                // Dotknięcie przełącza tryb: jasność ↔ RGB.
                HistogramView(
                    bins,
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
                start = Offset(vm.global.stripX, vm.global.stripY),
                turn = ui.uprightTurn,
                rotation = ui.iconRotation,
                haptics = ui.haptics,
                onCommit = { p -> vm.updateGlobal { it.copy(stripX = p.x, stripY = p.y) } },
                onDragState = dragState,
                onDelete = { vm.updateGlobal { it.copy(exposureInfo = false) } },
            ) { ExposureStrip(vm) }
        }
        if (vm.global.compass && vm.mode != CaptureMode.AUDIO) {
            DraggableOverlay(
                rect = overlayArea,
                start = Offset(vm.global.compassX, vm.global.compassY),
                turn = ui.uprightTurn,
                rotation = ui.iconRotation,
                haptics = ui.haptics,
                onCommit = { p -> vm.updateGlobal { it.copy(compassX = p.x, compassY = p.y) } },
                onDragState = dragState,
                onDelete = { vm.updateGlobal { it.copy(compass = false) } },
            ) { CompassTape() }
        }

        AnimatedVisibility(
            visible = overlayDragging,
            enter = fadeIn(tween(100)) + scaleIn(initialScale = 0.8f),
            exit = fadeOut(tween(120)) + scaleOut(targetScale = 0.8f),
            modifier = Modifier.offset {
                IntOffset(
                    (overlayArea.center.x - with(density) { 27.dp.toPx() }).roundToInt(),
                    (overlayArea.bottom - with(density) { 66.dp.toPx() }).roundToInt(),
                )
            },
        ) {
            Box(
                Modifier
                    .size(54.dp)
                    .graphicsLayer {
                        rotationZ = ui.iconRotation
                        scaleX = if (overlayOverTrash) 1.16f else 1f
                        scaleY = if (overlayOverTrash) 1.16f else 1f
                    }
                    .glass(CircleShape, active = overlayOverTrash),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Delete, "Usuń nakładkę", tint = if (overlayOverTrash) Palette.Ink else Color.White)
            }
        }

        Box(Modifier.fillMaxSize().graphicsLayer { alpha = flashAlpha.value }.background(Color.Black))
    }
}

/**
 * Nakładka do przeciągnięcia. Pozycja zapisywana jest tak, jak widzi ją trzymający telefon
 * (0–1 w poziomie i pionie „do góry”), więc po obróceniu telefonu element zostaje np. na górze.
 * Po upuszczeniu przyciąga się do krawędzi, rogów lub środka. Gest działa przed obrotem treści.
 */
@Composable
private fun DraggableOverlay(
    rect: Rect,
    start: Offset,
    turn: Int,
    rotation: Float,
    haptics: Boolean,
    deletable: Boolean = true,
    onCommit: (Offset) -> Unit,
    onDragState: (Boolean, Boolean) -> Unit,
    onDelete: () -> Unit,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    var contentSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
    val quarter = (((rotation / 90f).roundToInt() % 4) + 4) % 4
    val swap = quarter % 2 == 1
    val boxW = (if (swap) contentSize.height else contentSize.width).toFloat()
    val boxH = (if (swap) contentSize.width else contentSize.height).toFloat()
    val margin = with(density) { 8.dp.toPx() }
    val availW by rememberUpdatedState((rect.width - boxW - 2 * margin).coerceAtLeast(1f))
    val availH by rememberUpdatedState((rect.height - boxH - 2 * margin).coerceAtLeast(1f))
    val commit by rememberUpdatedState(onCommit)
    val dragState by rememberUpdatedState(onDragState)
    val delete by rememberUpdatedState(onDelete)
    val turnNow by rememberUpdatedState(turn)
    var upright by remember { mutableStateOf(start) }
    var dragPos by remember { mutableStateOf<Offset?>(null) }
    val target = dragPos ?: uprightToScreen(upright, turn)
    val shown by androidx.compose.animation.core.animateOffsetAsState(
        target,
        if (dragPos != null) androidx.compose.animation.core.snap() else spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "overlayPos",
    )
    val scale by animateFloatAsState(if (dragPos != null) 1.06f else 1f, spring(dampingRatio = 0.55f, stiffness = 600f), label = "dragScale")
    Box(
        Modifier
            .offset {
                IntOffset(
                    (rect.left + margin + shown.x * availW).roundToInt(),
                    (rect.top + margin + shown.y * availH).roundToInt(),
                )
            }
            .size(with(density) { boxW.toDp() }, with(density) { boxH.toDp() })
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        dragPos = uprightToScreen(upright, turnNow)
                        dragState(deletable, false)
                        if (haptics) haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    },
                    onDragEnd = {
                        val p = dragPos ?: return@detectDragGesturesAfterLongPress
                        if (deletable && isOverOverlayTrash(p, rect, margin, availW, availH, boxW, boxH, density)) {
                            dragPos = null
                            dragState(false, false)
                            if (haptics) haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            delete()
                            return@detectDragGesturesAfterLongPress
                        }
                        val snapped = snapUpright(screenToUpright(p, turnNow))
                        upright = snapped
                        dragPos = null
                        dragState(false, false)
                        if (haptics) haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        commit(snapped)
                    },
                    onDragCancel = {
                        dragPos = null
                        dragState(false, false)
                    },
                ) { change, drag ->
                    change.consume()
                    val p = dragPos ?: return@detectDragGesturesAfterLongPress
                    dragPos = Offset(
                        (p.x + drag.x / availW).coerceIn(0f, 1f),
                        (p.y + drag.y / availH).coerceIn(0f, 1f),
                    )
                    dragState(
                        deletable,
                        deletable && isOverOverlayTrash(dragPos!!, rect, margin, availW, availH, boxW, boxH, density),
                    )
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .wrapContentSize(unbounded = true)
                .onSizeChanged { contentSize = it }
                .graphicsLayer {
                    rotationZ = rotation
                    scaleX = scale
                    scaleY = scale
                },
        ) { content() }
    }
}

@Composable
private fun ZoomNavigator(bitmap: ImageBitmap, zoom: Float, baseZoom: Float) {
    val cropFraction = (baseZoom / zoom).coerceIn(0.08f, 1f)
    Box(
        Modifier
            .size(120.dp, 82.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black)
            .border(1.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(8.dp)),
    ) {
        Image(
            bitmap = bitmap,
            contentDescription = "Nawigator dużego zoomu",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width * cropFraction
            val h = size.height * cropFraction
            val left = (size.width - w) / 2f
            val top = (size.height - h) / 2f
            val shade = Color.Black.copy(alpha = 0.34f)
            drawRect(shade, topLeft = Offset.Zero, size = Size(size.width, top))
            drawRect(shade, topLeft = Offset(0f, top + h), size = Size(size.width, size.height - top - h))
            drawRect(shade, topLeft = Offset(0f, top), size = Size(left, h))
            drawRect(shade, topLeft = Offset(left + w, top), size = Size(size.width - left - w, h))
            drawRect(
                color = Palette.Amber,
                topLeft = Offset(left, top),
                size = Size(w, h),
                style = Stroke(width = 2.dp.toPx()),
            )
            val cross = 5.dp.toPx()
            drawLine(Palette.Amber, Offset(center.x - cross, center.y), Offset(center.x + cross, center.y), 1.dp.toPx())
            drawLine(Palette.Amber, Offset(center.x, center.y - cross), Offset(center.x, center.y + cross), 1.dp.toPx())
        }
        Text(
            "${com.blacksingularities.registrer.ui.components.formatZoom(zoom)}×",
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.TopStart)
                .background(Color.Black.copy(alpha = 0.72f), RoundedCornerShape(bottomEnd = 6.dp))
                .padding(horizontal = 5.dp, vertical = 2.dp),
        )
    }
}

private fun isOverOverlayTrash(
    p: Offset,
    rect: Rect,
    margin: Float,
    availW: Float,
    availH: Float,
    boxW: Float,
    boxH: Float,
    density: androidx.compose.ui.unit.Density,
): Boolean {
    val center = Offset(rect.left + margin + p.x * availW + boxW / 2f, rect.top + margin + p.y * availH + boxH / 2f)
    val trash = Offset(rect.center.x, rect.bottom - with(density) { 39.dp.toPx() })
    return (center - trash).getDistance() <= with(density) { 48.dp.toPx() }
}

/** Obrót punktu (0–1) wokół środka o wielokrotność 90° (zgodnie z ruchem wskazówek). */
private fun rotateUnit(p: Offset, degrees: Int): Offset {
    val dx = p.x - 0.5f
    val dy = p.y - 0.5f
    return when (((degrees % 360) + 360) % 360) {
        90 -> Offset(0.5f - dy, 0.5f + dx)
        180 -> Offset(0.5f - dx, 0.5f - dy)
        270 -> Offset(0.5f + dy, 0.5f - dx)
        else -> p
    }
}

private fun uprightToScreen(p: Offset, turn: Int) = rotateUnit(p, turn)
private fun screenToUpright(p: Offset, turn: Int) = rotateUnit(p, -turn)

/** Przyciąganie do krawędzi (0, 1) i środka (0,5), gdy blisko. */
private fun snapUpright(p: Offset): Offset {
    fun snap(v: Float) = listOf(0f, 0.5f, 1f).firstOrNull { kotlin.math.abs(it - v) < 0.1f } ?: v
    return Offset(snap(p.x), snap(p.y))
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
                    .glass(RoundedCornerShape(50))
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
                .glass(BlockShape)
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
    if (vm.mode != CaptureMode.PRO) ModeQuickControl(ui, Modifier.padding(bottom = 4.dp))
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
            evAvailable = vm.mode == CaptureMode.PRO && vm.evMax > vm.evMin,
            evMode = vm.rockerEv,
            evLabel = String.format(java.util.Locale.US, "%+.1f EV", vm.evIndex * vm.evStep).replace('.', ','),
            onToggleEv = vm::toggleRockerEv,
            onEvRate = vm::nudgeEv,
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

    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
        val thumb = vm.thumbnail
        if (vm.mode.isVideo && vm.recState != RecState.IDLE && vm.session.snapshotInVideo) {
            RoundIcon(Icons.Rounded.PhotoCamera, "Zdjęcie z wideo", rot, size = 48.dp, onClick = vm::snapshot)
        } else if (!isAudio && thumb != null) {
            Image(
                bitmap = thumb.asImageBitmap(),
                contentDescription = "Ostatnie ujęcie",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(44.dp)
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
            size = 48.dp,
            onClick = vm::togglePause,
        )
        !isAudio && vm.mode != CaptureMode.SLOWMO && vm.caps.hasFront && vm.caps.hasBack && !vm.rearDisplayActive -> {
            val spin by animateFloatAsState(
                if (vm.lensFacing == CameraSelector.LENS_FACING_FRONT) 180f else 0f,
                tween(420),
                label = "switch",
            )
            RoundIcon(Icons.Rounded.Cameraswitch, "Zmień obiektyw", rot + spin, size = 48.dp, enabled = !vm.busy, onClick = vm::switchLens)
        }
        else -> Spacer(Modifier.size(48.dp))
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
                options = vm.flashOptions().map { f ->
                    when (f) {
                        FlashSetting.OFF -> PickerOption("Lampa wyłączona", Icons.Rounded.FlashOff, caption = "Wył.")
                        FlashSetting.AUTO -> PickerOption("Lampa automatyczna", Icons.Rounded.FlashAuto, caption = "Auto")
                        FlashSetting.ON -> PickerOption("Lampa włączona", Icons.Rounded.FlashOn, caption = "Wł.")
                        FlashSetting.TORCH -> PickerOption("Latarka", Icons.Rounded.FlashlightOn, caption = "Latarka")
                    }
                },
                selected = vm.flashOptions().indexOf(cfg.flash),
                onPick = { i -> vm.setFlash(vm.flashOptions()[i]) },
                onClick = vm::cycleFlash,
            )
            if (vm.mode.isPhoto) {
                RoundIcon(
                    Icons.Rounded.Timer,
                    if (cfg.timerSeconds == 0) "Samowyzwalacz wyłączony" else "Samowyzwalacz ${cfg.timerSeconds} s",
                    rot,
                    active = cfg.timerSeconds > 0,
                    badge = cfg.timerSeconds.takeIf { it > 0 }?.toString(),
                    options = TIMER_STEPS.map { t ->
                        if (t == 0) PickerOption("Samowyzwalacz wyłączony", Icons.Rounded.TimerOff, caption = "Wył.")
                        else PickerOption("Samowyzwalacz $t s", value = "$t", caption = "s")
                    },
                    selected = TIMER_STEPS.indexOf(cfg.timerSeconds),
                    onPick = { i -> vm.setTimer(TIMER_STEPS[i]) },
                    onClick = vm::cycleTimer,
                )
                val frame = remember(cfg.aspect) { frameIcon(cfg.aspect) }
                RoundIcon(
                    frame,
                    "Proporcje ${cfg.aspect.label}",
                    rot,
                    enabled = !vm.busy,
                    options = com.blacksingularities.registrer.data.AspectRatioMode.entries.map { a ->
                        PickerOption("Proporcje ${a.label}", frameIcon(a), caption = a.label)
                    },
                    selected = cfg.aspect.ordinal,
                    onPick = { i -> vm.setAspect(com.blacksingularities.registrer.data.AspectRatioMode.entries[i]) },
                    onClick = vm::cycleAspect,
                )
                val formats = PhotoFormat.entries.filter { it in vm.caps.photoFormats }
                if ((vm.mode == CaptureMode.PHOTO || vm.mode == CaptureMode.PRO) && formats.size > 1) {
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
                        options = formats.map { f ->
                            when (f) {
                                PhotoFormat.JPEG -> PickerOption("Format JPEG", Icons.Rounded.Image, caption = "JPEG")
                                PhotoFormat.ULTRA_HDR -> PickerOption("Format Ultra HDR", Icons.Rounded.HdrOn, caption = "UHDR")
                                PhotoFormat.RAW_JPEG -> PickerOption("Format RAW + JPEG", Icons.Rounded.RawOn, caption = "RAW+JPG")
                                PhotoFormat.RAW -> PickerOption("Format RAW (DNG)", Icons.Rounded.RawOn, caption = "DNG")
                            }
                        },
                        selected = formats.indexOf(cfg.photoFormat),
                        onPick = { i -> vm.setPhotoFormat(formats[i]) },
                        onClick = vm::cyclePhotoFormat,
                    )
                }
            } else if (vm.mode.isVideo) {
                if (vm.mode == CaptureMode.VIDEO) {
                    val (resValue, resUnit) = resolutionGlyph(cfg.videoResolution)
                    GlassValueButton(
                        resValue,
                        resUnit,
                        "Rozdzielczość ${cfg.videoResolution.label}",
                        rot,
                        active = cfg.videoResolution == com.blacksingularities.registrer.data.VideoResolution.UHD,
                        enabled = !vm.busy && vm.caps.videoResolutions.size > 1,
                        options = vm.caps.videoResolutions.map { r ->
                            val (v, u) = resolutionGlyph(r)
                            PickerOption("Rozdzielczość ${r.label}", value = v, caption = u)
                        },
                        selected = vm.caps.videoResolutions.indexOf(cfg.videoResolution),
                        onPick = { i -> vm.setVideoResolution(vm.caps.videoResolutions[i]) },
                        onClick = vm::cycleVideoResolution,
                    )
                } else {
                    val resolutions = vm.slowMoResolutions()
                    val current = vm.effectiveSlowMoResolution
                    val (resValue, resUnit) = resolutionGlyph(current)
                    GlassValueButton(
                        resValue,
                        resUnit,
                        "Slow-motion ${current.label}",
                        rot,
                        active = true,
                        enabled = !vm.busy && resolutions.size > 1,
                        options = resolutions.map { r ->
                            val (v, u) = resolutionGlyph(r)
                            PickerOption("Slow-motion ${r.label}", value = v, caption = u)
                        },
                        selected = resolutions.indexOf(current),
                        onPick = { i -> vm.setSlowMoResolution(resolutions[i]) },
                        onClick = vm::cycleVideoResolution,
                    )
                }
                val slowFps = vm.slowMoFpsOptions()
                val fpsChoices = if (vm.mode == CaptureMode.SLOWMO) slowFps.size else vm.videoFpsOptions().size
                val autoFps = vm.mode == CaptureMode.VIDEO && cfg.videoFps == com.blacksingularities.registrer.data.VideoFps.AUTO
                val fpsNumber = vm.fpsLabel.removeSuffix(" kl/s").removePrefix("Auto").trim()
                GlassValueButton(
                    if (autoFps) (vm.session.fps.takeIf { it > 0 }?.toString() ?: "A") else fpsNumber,
                    if (autoFps) "auto" else "kl/s",
                    "Klatkaż ${vm.fpsLabel}",
                    rot,
                    active = !autoFps,
                    enabled = !vm.busy && fpsChoices > 1,
                    options = if (vm.mode == CaptureMode.SLOWMO) {
                        slowFps.map { f -> PickerOption("Zwolnione tempo $f kl/s", value = "$f", caption = "kl/s") }
                    } else {
                        vm.videoFpsOptions().map { f ->
                            if (f == com.blacksingularities.registrer.data.VideoFps.AUTO) PickerOption("Klatkaż automatyczny", value = "A", caption = "auto")
                            else PickerOption("Klatkaż ${f.fps} kl/s", value = "${f.fps}", caption = "kl/s")
                        }
                    },
                    selected = if (vm.mode == CaptureMode.SLOWMO) slowFps.indexOf(cfg.slowMoFps).coerceAtLeast(0)
                        else vm.videoFpsOptions().indexOf(cfg.videoFps),
                    onPick = { i ->
                        if (vm.mode == CaptureMode.SLOWMO) vm.setSlowMoFps(slowFps[i]) else vm.setVideoFps(vm.videoFpsOptions()[i])
                    },
                    onClick = vm::cycleVideoFps,
                )
                if (vm.mode == CaptureMode.VIDEO && vm.caps.videoHlg) {
                    RoundIcon(
                        if (cfg.videoHdr) Icons.Rounded.HdrOn else Icons.Rounded.HdrOff,
                        if (cfg.videoHdr) "Wideo HDR (HLG)" else "Wideo SDR",
                        rot,
                        active = cfg.videoHdr,
                        enabled = !vm.busy,
                        options = listOf(
                            PickerOption("Wideo SDR", Icons.Rounded.HdrOff, caption = "SDR"),
                            PickerOption("Wideo HDR (HLG)", Icons.Rounded.HdrOn, caption = "HLG"),
                        ),
                        selected = if (cfg.videoHdr) 1 else 0,
                        onPick = { i -> vm.setVideoHdr(i == 1) },
                        onClick = { vm.updateProfile { it.copy(videoHdr = !it.videoHdr) } },
                    )
                }
                if (vm.mode == CaptureMode.VIDEO && vm.caps.videoStabilization) {
                    RoundIcon(
                        Icons.Rounded.VideoStable,
                        if (cfg.stabilization) "Stabilizacja włączona" else "Stabilizacja wyłączona",
                        rot,
                        active = cfg.stabilization,
                        enabled = !vm.busy,
                        options = listOf(
                            PickerOption("Stabilizacja wyłączona", Icons.Rounded.VideoStable, caption = "Wył."),
                            PickerOption("Stabilizacja włączona", Icons.Rounded.VideoStable, caption = "Wł."),
                        ),
                        selected = if (cfg.stabilization) 1 else 0,
                        onPick = { i -> vm.setStabilization(i == 1) },
                        onClick = { vm.setStabilization(!cfg.stabilization) },
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
    }
    val settings: @Composable () -> Unit = {
        RoundIcon(Icons.Rounded.Settings, "Ustawienia", rot, onClick = ui.onSettings)
    }
    // Jeden szklany pasek z przyciskami + osobne kółko ustawień. Gdy przycisków jest więcej,
    // niż mieści ekran (wąski ekran zewnętrzny folda), pasek się przewija.
    if (vertical) {
        Column(
            Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.weight(1f, fill = false)) {
                GlassToolbar(vertical = true, modifier = Modifier.verticalScroll(rememberScrollState())) {
                    CompositionLocalProvider(LocalHintOnTap provides true, LocalPickerHorizontal provides true) { items() }
                }
            }
            settings()
        }
    } else {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BoxWithConstraints(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                GlassToolbar(
                    vertical = false,
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .widthIn(min = maxWidth),
                ) {
                    CompositionLocalProvider(LocalHintOnTap provides true) { items() }
                }
            }
            settings()
        }
    }
}

private val TIMER_STEPS = listOf(0, 3, 5, 10)

private fun resolutionGlyph(r: com.blacksingularities.registrer.data.VideoResolution): Pair<String, String> = when (r) {
    com.blacksingularities.registrer.data.VideoResolution.UHD -> "4K" to "2160"
    com.blacksingularities.registrer.data.VideoResolution.FHD -> "FHD" to "1080"
    com.blacksingularities.registrer.data.VideoResolution.HD -> "HD" to "720"
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
 * Pasek parametrów na samej górze kadru (ikony zamiast nazw): rozdzielczość materiału, ISO,
 * czas naświetlania, przysłona, kompensacja EV i zoom. Wartości ręczne (Pro) – bursztynem.
 */
@Composable
private fun ExposureStrip(vm: CameraViewModel, modifier: Modifier = Modifier) {
    val hud = vm.hud
    val pro = if (vm.mode == CaptureMode.PRO) vm.pro else null
    data class Item(val icon: ImageVector?, val name: String, val value: String, val manual: Boolean = false)
    val items = buildList {
        add(Item(if (vm.mode.isVideo) Icons.Rounded.Videocam else Icons.Rounded.Image, "Rozdzielczość", resolutionText(vm)))
        val iso = pro?.iso ?: hud?.iso
        add(Item(null, "ISO", iso?.toString() ?: "—", pro?.iso != null))
        val shutter = pro?.exposureNs ?: hud?.exposureNs
        add(Item(Icons.Rounded.ShutterSpeed, "Czas naświetlania", shutter?.let { formatShutter(it) } ?: "—", pro?.exposureNs != null))
        hud?.aperture?.let { add(Item(Icons.Rounded.Camera, "Przysłona", String.format(java.util.Locale.US, "f/%.1f", it))) }
        add(Item(Icons.Rounded.Exposure, "Kompensacja EV", String.format(java.util.Locale.US, "%+.1f", vm.evIndex * vm.evStep), vm.evIndex != 0))
        add(Item(Icons.Rounded.ZoomIn, "Zoom", com.blacksingularities.registrer.ui.components.formatZoom(vm.displayZoom) + "×"))
    }
    Row(
        modifier
            .glass(BlockShape)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEach { item ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                val tint = if (item.manual) Palette.Amber else Palette.TextDim
                if (item.icon != null) {
                    Icon(item.icon, item.name, tint = tint, modifier = Modifier.size(13.dp))
                } else {
                    // „ISO” w ramce – czytelniejsze niż ikona ekspozycji.
                    Text(
                        item.name,
                        color = tint,
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.3.sp,
                        style = androidx.compose.ui.text.TextStyle(lineHeight = 8.sp),
                        modifier = Modifier
                            .border(1.dp, tint, RoundedCornerShape(2.dp))
                            .padding(horizontal = 2.dp, vertical = 1.dp),
                    )
                }
                Spacer(Modifier.width(3.dp))
                Text(item.value, style = MonoDigits, fontSize = 12.sp, color = if (item.manual) Palette.Amber else Color.White, maxLines = 1)
            }
        }
    }
}

/** „50 MP” dla zdjęć, „2160p30” dla wideo – z rzeczywistej rozdzielczości, a gdy nieznana, z ustawień. */
private fun resolutionText(vm: CameraViewModel): String {
    val size = vm.session.outputSize
    return if (vm.mode.isVideo) {
        val requested = if (vm.mode == CaptureMode.SLOWMO) vm.effectiveSlowMoResolution else vm.cfg.videoResolution
        val lines = size?.let { minOf(it.width, it.height) } ?: when (requested) {
            com.blacksingularities.registrer.data.VideoResolution.UHD -> 2160
            com.blacksingularities.registrer.data.VideoResolution.FHD -> 1080
            com.blacksingularities.registrer.data.VideoResolution.HD -> 720
        }
        val fps = vm.session.fps
        "${lines}p" + (if (fps > 0) "$fps" else "")
    } else if (size != null) {
        val mp = size.width.toLong() * size.height / 1_000_000f
        if (mp < 10f) String.format(java.util.Locale.US, "%.1f MP", mp) else "${mp.roundToInt()} MP"
    } else {
        "—"
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
            .glass(BlockShape)
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
        color = if (color == Palette.Amber) activeContent() else color,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.8.sp,
        style = if (mono) MonoDigits else TextStyle.Default,
        modifier = Modifier
            .glass(BlockShape, active = color == Palette.Amber)
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
        // Siatki otwierają menu – bez dymka przy dotknięciu.
        CompositionLocalProvider(LocalHintOnTap provides false) {
            RoundIcon(
                Icons.Rounded.GridOn,
                "Siatki",
                rotation,
                tint = if (grids.isEmpty()) Palette.TextFaint else Color.White,
                onClick = { open = true },
            )
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            modifier = Modifier.rotatedLayout(rotation),
            containerColor = Color.Transparent,
            shape = RoundedCornerShape(14.dp),
        ) {
            GridType.entries.forEach { g ->
                val on = g in grids
                DropdownMenuItem(
                    modifier = Modifier.padding(vertical = 1.dp).clip(RoundedCornerShape(14.dp)).background(Color.Black.copy(alpha = 0.58f)),
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
                    modifier = Modifier.padding(vertical = 1.dp).clip(RoundedCornerShape(14.dp)).background(Color.Black.copy(alpha = 0.58f)),
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
                .glass(BlockShape)
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
    options: List<PickerOption>? = null,
    selected: Int = -1,
    onPick: (Int) -> Unit = {},
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
        options = options,
        selected = selected,
        onPick = onPick,
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
