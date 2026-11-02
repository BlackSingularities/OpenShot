package com.blacksingularities.registrer.ui

import com.blacksingularities.registrer.i18n.tr
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import com.blacksingularities.registrer.ui.theme.LocalNoBackground
import com.blacksingularities.registrer.ui.theme.LocalBlur
import androidx.compose.runtime.CompositionLocalProvider
import com.blacksingularities.registrer.ui.theme.glass
import com.blacksingularities.registrer.ui.theme.GlassScope
import com.blacksingularities.registrer.ui.theme.GlassLevel
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.spring
import androidx.activity.compose.BackHandler
import android.view.OrientationEventListener
import android.view.Surface
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.blacksingularities.registrer.camera.CaptureMode
import com.blacksingularities.registrer.ui.components.SettingsSheet
import com.blacksingularities.registrer.ui.theme.Palette
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.roundToInt

internal val BAR = 56.dp
internal val SIDE_RAIL = 76.dp
internal val SHUTTER_RAIL = 132.dp

/**
 * Ekran aparatu. Okno nigdy się nie obraca – interfejs dopasowuje się do orientacji, w której
 * trzymasz telefon: ikony obracają się płynnie, a informacje w kadrze (plakietki, komunikaty,
 * odliczanie, aktualizacje) układają się „do góry” względem patrzącego. Układ zależy od kształtu
 * okna, więc działa także na zewnętrznym i wewnętrznym ekranie folda.
 */
@Composable
fun CameraScreen(
    vm: CameraViewModel,
    onToggleRearDisplay: () -> Unit = {},
    onToggleSubjectPreview: (source: () -> android.graphics.Bitmap?) -> Unit = {},
) {
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
        // Strażnik: brak klatek po kilku sekundach = osobny obiektyw wraca do aparatu głównego,
        // a sesja z osobną analizą wiązana jest bez niej – zamiast zawieszonego obrazu.
        if (vm.mode != CaptureMode.AUDIO && vm.cameraReady) {
            delay(4000)
            val stalled = previewView.previewStreamState.value != PreviewView.StreamState.STREAMING &&
                lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
            if (stalled) {
                if (vm.activeCameraId != null) vm.onAuxLensStalled() else vm.onPreviewStalled()
            }
        }
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
    var trashCenter by remember { mutableStateOf(Offset.Zero) }
    var trashVisible by remember { mutableStateOf(false) }
    var trashOver by remember { mutableStateOf(false) }
    val ui = Ui(
        vm, iconRotation, uprightTurn, displayDegrees, haptics,
        onSettings = { showSettings = true },
        onFold = onToggleRearDisplay,
        onSubjectPreview = { onToggleSubjectPreview { runCatching { previewView.bitmap }.getOrNull() } },
        freeze = freezeFrame,
        controlsTop = controlsTop,
        onControlsTop = { controlsTop = it },
        onTrash = { c, over ->
            if (c != null) trashCenter = c
            trashVisible = c != null
            trashOver = over
        },
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
                OverlayTrash(trashVisible, trashOver, trashCenter, iconRotation)
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
            displaysInfo = vm.displaySummary,
            )
        }
    }
}

/** Kosz na nakładki – nad całym interfejsem, widoczny tylko podczas przeciągania. */
@Composable
internal fun OverlayTrash(visible: Boolean, over: Boolean, center: Offset, rotation: Float) {
    val density = LocalDensity.current
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(100)) + scaleIn(initialScale = 0.8f),
        exit = fadeOut(tween(120)) + scaleOut(targetScale = 0.8f),
        modifier = Modifier.offset {
            val half = with(density) { 27.dp.toPx() }
            IntOffset((center.x - half).roundToInt(), (center.y - half).roundToInt())
        },
    ) {
        Box(
            Modifier
                .size(54.dp)
                .graphicsLayer {
                    rotationZ = rotation
                    scaleX = if (over) 1.16f else 1f
                    scaleY = if (over) 1.16f else 1f
                }
                .background(Color.Black.copy(alpha = if (over) 0f else 0.55f), CircleShape)
                .glass(CircleShape, active = over),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Delete, tr("Usuń nakładkę", "Remove overlay"), tint = if (over) Palette.Ink else Color.White)
        }
    }
}

/** Wspólny kontekst dla obu układów. */
internal class Ui(
    val vm: CameraViewModel,
    val iconRotation: Float,
    val uprightTurn: Int,
    val displayDegrees: Int,
    val haptics: Boolean,
    val onSettings: () -> Unit,
    val onFold: () -> Unit,
    val onSubjectPreview: () -> Unit = {},
    val freeze: androidx.compose.ui.graphics.ImageBitmap? = null,
    /** Górna krawędź dolnego sterowania w układzie okna – nakładki pływające jej nie zasłaniają. */
    val controlsTop: Float = Float.MAX_VALUE,
    val onControlsTop: (Float) -> Unit = {},
    /** Kosz na nakładki: środek w układzie okna (null – ukryty) i czy palec jest nad nim. */
    val onTrash: (Offset?, Boolean) -> Unit = { _, _ -> },
)

// ==================================================================== układy

/** Okno wyższe niż szersze: telefon, ekran zewnętrzny folda. */
@Composable
internal fun TallLayout(ui: Ui, previewView: PreviewView) {
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
internal fun WideLayout(ui: Ui, previewView: PreviewView) {
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
