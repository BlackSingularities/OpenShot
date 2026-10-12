package com.blacksingularities.registrer.ui

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cameraswitch
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
            implementationMode = PreviewView.ImplementationMode.PERFORMANCE
            scaleType = PreviewView.ScaleType.FIT_START
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
    LaunchedEffect(displaySurfaceRotation) { vm.engine.setDisplayRotation(displaySurfaceRotation) }
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

    BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black)) {
        val wide = maxWidth > maxHeight * 1.05f
        LaunchedEffect(wide) {
            previewView.scaleType = if (wide) PreviewView.ScaleType.FIT_CENTER else PreviewView.ScaleType.FIT_START
        }
        if (wide) WideLayout(ui, previewView) else TallLayout(ui, previewView)
    }

    if (showSettings) {
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
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f), Color.Black.copy(alpha = 0.85f))))
                .padding(bottom = insets.calculateBottomPadding() + 12.dp, top = 12.dp),
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
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))))
                    .padding(top = 16.dp, bottom = 8.dp),
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

        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        val streaming = streamState == PreviewView.StreamState.STREAMING && vm.cameraReady
        val curtain by animateFloatAsState(if (streaming) 0f else 1f, tween(if (streaming) 380 else 120), label = "curtain")
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = curtain }.background(Color.Black))

        // Szczypanie = zoom, dotknięcie = ostrość, przytrzymanie = blokada AE/AF, podwójne = obiektyw.
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoom, _ ->
                        if (zoom != 1f) vm.setZoomTarget(vm.zoomTarget * zoom, 22f)
                    }
                }
                .pointerInput(rect) {
                    fun meter(o: Offset, lock: Boolean) {
                        if (!rect.contains(o) || vm.mode == CaptureMode.AUDIO) return
                        vm.focus(previewView.meteringPointFactory.createPoint(o.x, o.y), lock)
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

        GridOverlay(vm.cfg.grid, ratio, Modifier.fillMaxSize(), centered)
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

        Box(Modifier.fillMaxSize().graphicsLayer { alpha = flashAlpha.value }.background(Color.Black))
    }
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
                modeBadge(vm)?.let { Badge(it) }
                val hud = vm.hud
                if (hud != null && (vm.mode == CaptureMode.PRO || vm.mode == CaptureMode.NIGHT)) {
                    Badge("ISO ${hud.iso} · ${formatShutter(hud.exposureNs)}", mono = true)
                }
                if (vm.focusLocked) Badge("AE/AF · zablokowane  ✕", color = Palette.Amber, onClick = vm::unlockFocus)
                if (vm.intervalRunning) {
                    val total = vm.cfg.intervalCount
                    Badge("Interwał · ${vm.intervalShot}${if (total > 0) "/$total" else ""}", color = Palette.Amber)
                }
                if (vm.rearDisplayActive) Badge("Selfie głównym aparatem", color = Palette.Amber)
            }
            val bins = vm.histogram
            if (bins != null && vm.cfg.histogram && vm.mode.isPhoto) HistogramView(bins)
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
                    .clip(RoundedCornerShape(16.dp))
                    .background(Palette.PanelHigh.copy(alpha = 0.95f))
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
                .clip(RoundedCornerShape(50))
                .background(Palette.GlassStrong)
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

/** Panel Pro, obiektywy z przełącznikiem zoomu oraz pokrętło trybów. */
@Composable
private fun CaptureTools(ui: Ui) {
    val vm = ui.vm
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
    if (vm.mode != CaptureMode.AUDIO && vm.cameraReady) {
        ZoomControls(
            presets = vm.zoomPresets,
            current = vm.zoomCurrent,
            min = vm.zoomMin,
            max = vm.zoomMax,
            iconRotation = ui.iconRotation,
            haptics = ui.haptics,
            onPreset = { vm.setZoomTarget(it, if (vm.mode.isVideo) 4f else 14f) },
            onRate = vm::nudgeZoom,
            modifier = Modifier.padding(bottom = 4.dp),
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
                    .border(2.dp, Color.White.copy(alpha = 0.85f), CircleShape)
                    .clickable {
                        val uri = vm.lastUri ?: return@clickable
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

/** Lampa, samowyzwalacz, proporcje, format, siatka, fold, ustawienia – w rzędzie albo w kolumnie. */
@Composable
private fun TopControls(ui: Ui, vertical: Boolean) {
    val vm = ui.vm
    val rot = ui.iconRotation
    val items: @Composable () -> Unit = {
        if (vm.mode != CaptureMode.AUDIO) {
            val cfg = vm.cfg
            val flashIcon = when (cfg.flash) {
                FlashSetting.OFF -> Icons.Rounded.FlashOff
                FlashSetting.AUTO -> Icons.Rounded.FlashAuto
                FlashSetting.ON -> Icons.Rounded.FlashOn
                FlashSetting.TORCH -> Icons.Rounded.FlashlightOn
            }
            RoundIcon(
                flashIcon,
                "Lampa",
                rot,
                tint = if (cfg.flash == FlashSetting.OFF) Color.White else Palette.Amber,
                enabled = vm.caps.hasFlash,
                onClick = vm::cycleFlash,
            )
            if (vm.mode.isPhoto) {
                LabeledIcon(
                    Icons.Rounded.Timer,
                    if (cfg.timerSeconds == 0) null else "${cfg.timerSeconds}s",
                    rot,
                    active = cfg.timerSeconds > 0,
                    onClick = vm::cycleTimer,
                )
                TextChip(cfg.aspect.label, rot, enabled = !vm.busy, onClick = vm::cycleAspect)
                if ((vm.mode == CaptureMode.PHOTO || vm.mode == CaptureMode.PRO) && vm.caps.photoFormats.size > 1) {
                    TextChip(
                        when (cfg.photoFormat) {
                            PhotoFormat.JPEG -> "JPG"
                            PhotoFormat.ULTRA_HDR -> "UHDR"
                            PhotoFormat.RAW_JPEG -> "RAW+"
                            PhotoFormat.RAW -> "RAW"
                        },
                        rot,
                        enabled = !vm.busy,
                        active = cfg.photoFormat != PhotoFormat.JPEG,
                        onClick = vm::cyclePhotoFormat,
                    )
                } else {
                    GridButton(vm, rot)
                }
            } else {
                TextChip(cfg.videoResolution.label, rot, enabled = !vm.busy && vm.caps.videoResolutions.size > 1, onClick = vm::cycleVideoResolution)
                if (vm.mode == CaptureMode.VIDEO && vm.caps.videoHlg) {
                    TextChip(
                        if (cfg.videoHdr) "HDR" else "SDR",
                        rot,
                        enabled = !vm.busy,
                        active = cfg.videoHdr,
                        onClick = { vm.updateProfile { it.copy(videoHdr = !it.videoHdr) } },
                    )
                }
                GridButton(vm, rot)
            }
            if (vm.rearDisplayAvailable || vm.rearDisplayActive) {
                RoundIcon(
                    Icons.Rounded.FlipToBack,
                    if (vm.rearDisplayActive) "Wróć na ekran wewnętrzny" else "Selfie głównym aparatem",
                    rot,
                    tint = if (vm.rearDisplayActive) Palette.Ink else Color.White,
                    background = if (vm.rearDisplayActive) Palette.Amber else Palette.Glass,
                    enabled = !vm.busy,
                    onClick = ui.onFold,
                )
            }
        } else {
            Text(
                "Dyktafon",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.padding(6.dp).graphicsLayer { rotationZ = if (vertical) -90f else 0f },
            )
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
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) { items() }
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
            .clip(RoundedCornerShape(50))
            .background(Palette.GlassStrong)
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

private fun modeBadge(vm: CameraViewModel): String? {
    val ext = vm.session.extension
    return when (vm.mode) {
        CaptureMode.NIGHT -> when {
            ext == ExtensionMode.NIGHT -> "NOC · sprzętowa"
            vm.cfg.nightFrames == 0 -> "NOC · auto ${vm.autoNightFrames()} klatek"
            else -> "NOC · ${vm.cfg.nightFrames} klatek"
        }
        CaptureMode.HDR -> if (ext == ExtensionMode.HDR) "HDR · sprzętowy" else "HDR · fuzja 3 ekspozycji"
        CaptureMode.PORTRAIT -> when (ext) {
            ExtensionMode.BOKEH -> "PORTRET · bokeh sprzętowy"
            ExtensionMode.FACE_RETOUCH -> "PORTRET · retusz"
            else -> "PORTRET · bokeh ${vm.cfg.portraitBlur}%"
        }
        CaptureMode.PHOTO, CaptureMode.PRO -> vm.session.photoFormat.takeIf { it != PhotoFormat.JPEG }?.label
        CaptureMode.VIDEO, CaptureMode.SLOWMO -> buildString {
            append(vm.cfg.videoResolution.label)
            if (vm.session.fps > 0) append(" · ${vm.session.fps} kl./s")
            if (vm.session.videoHdr) append(" · HDR")
            if (vm.session.stabilization) append(" · STAB")
        }
        CaptureMode.AUDIO -> null
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
            .clip(RoundedCornerShape(50))
            .background(Palette.GlassStrong)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

@Composable
private fun GridButton(vm: CameraViewModel, rotation: Float) {
    val grid = vm.cfg.grid
    RoundIcon(
        Icons.Rounded.GridOn,
        "Siatka: ${grid.label}",
        rotation,
        tint = if (grid == GridType.NONE) Palette.TextFaint else Color.White,
        onClick = {
            val all = GridType.entries
            vm.updateProfile { it.copy(grid = all[(all.indexOf(it.grid) + 1) % all.size]) }
        },
    )
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
                .clip(RoundedCornerShape(50))
                .background(Palette.PanelHigh.copy(alpha = 0.96f))
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
    size: Dp = 42.dp,
    tint: Color = Color.White,
    background: Color = Palette.Glass,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(background)
            .clickable(enabled = enabled, onClick = onClick)
            .graphicsLayer { alpha = if (enabled) 1f else 0.4f },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, description, tint = tint, modifier = Modifier.graphicsLayer { rotationZ = rotation })
    }
}

@Composable
private fun LabeledIcon(icon: ImageVector, label: String?, rotation: Float, active: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(Palette.Glass)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.graphicsLayer { rotationZ = rotation }, contentAlignment = Alignment.Center) {
            if (label == null) {
                Icon(icon, "Samowyzwalacz", tint = Color.White)
            } else {
                Text(label, color = if (active) Palette.Amber else Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun TextChip(text: String, rotation: Float, enabled: Boolean = true, active: Boolean = false, onClick: () -> Unit) {
    Box(
        Modifier
            .height(42.dp)
            .clip(RoundedCornerShape(50))
            .background(Palette.Glass)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = if (active) Palette.Amber else if (enabled) Color.White else Palette.TextFaint,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.graphicsLayer { rotationZ = rotation },
        )
    }
}
