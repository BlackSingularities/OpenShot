package com.blacksingularities.registrer.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.view.OrientationEventListener
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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
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
import androidx.compose.material.icons.rounded.Cameraswitch
import androidx.compose.material.icons.rounded.FlashAuto
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.blacksingularities.registrer.camera.CaptureMode
import com.blacksingularities.registrer.camera.FlashSetting
import com.blacksingularities.registrer.ui.components.AudioStudio
import com.blacksingularities.registrer.ui.components.FocusRing
import com.blacksingularities.registrer.ui.components.GridOverlay
import com.blacksingularities.registrer.ui.components.HistogramView
import com.blacksingularities.registrer.ui.components.LevelOverlay
import com.blacksingularities.registrer.ui.components.ModeDial
import com.blacksingularities.registrer.ui.components.ProPanel
import com.blacksingularities.registrer.ui.components.SettingsSheet
import com.blacksingularities.registrer.ui.components.ShutterButton
import com.blacksingularities.registrer.ui.components.ZoomBar
import com.blacksingularities.registrer.ui.components.formatClock
import com.blacksingularities.registrer.ui.components.formatShutter
import com.blacksingularities.registrer.ui.components.imageRect
import com.blacksingularities.registrer.ui.theme.MonoDigits
import com.blacksingularities.registrer.ui.theme.Palette
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.roundToInt

private val TOP_BAR_HEIGHT = 56.dp

@Composable
fun CameraScreen(vm: CameraViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val density = LocalDensity.current
    val insets = WindowInsets.safeDrawing.asPaddingValues()
    val haptics = vm.settings.haptics

    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.PERFORMANCE
            scaleType = PreviewView.ScaleType.FIT_START
        }
    }
    val streamState by previewView.previewStreamState.observeAsState()

    // (Re)wiązanie kamery przy każdej zmianie konfiguracji.
    val bindKey = vm.bindKey
    LaunchedEffect(bindKey, lifecycleOwner) {
        vm.bind(lifecycleOwner, previewView.surfaceProvider)
    }

    // Płynny zoom: interpolacja w przestrzeni logarytmicznej, zależna od czasu klatki.
    LaunchedEffect(Unit) {
        var current = vm.zoomCurrent
        var lastFrame = 0L
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

    // Orientacja urządzenia – ikony obracają się, ekran pozostaje pionowy.
    DisposableEffect(Unit) {
        val listener = object : OrientationEventListener(context) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN) return
                vm.deviceRotation = when (orientation) {
                    in 45..134 -> 90
                    in 135..224 -> 180
                    in 225..314 -> 270
                    else -> 0
                }
            }
        }
        listener.enable()
        onDispose { listener.disable() }
    }
    var rotationAccum by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(vm.deviceRotation) {
        val target = -vm.deviceRotation.toFloat()
        var delta = (target - rotationAccum) % 360f
        if (delta > 180f) delta -= 360f
        if (delta < -180f) delta += 360f
        rotationAccum += delta
    }
    val iconRotation by animateFloatAsState(rotationAccum, tween(320), label = "iconRotation")

    var showSettings by remember { mutableStateOf(false) }
    var focusPoint by remember { mutableStateOf<Offset?>(null) }
    var focusTrigger by remember { mutableIntStateOf(0) }
    LaunchedEffect(focusTrigger, vm.evIndex) {
        if (focusPoint != null) {
            delay(3500)
            focusPoint = null
        }
    }
    LaunchedEffect(bindKey) { focusPoint = null }

    val flashAlpha = remember { Animatable(0f) }
    LaunchedEffect(vm.shutterTick) {
        if (vm.shutterTick > 0) {
            flashAlpha.snapTo(0.85f)
            flashAlpha.animateTo(0f, tween(260))
        }
    }

    LaunchedEffect(vm.message) {
        if (vm.message != null) {
            delay(2800)
            vm.clearMessage()
        }
    }

    val isAudio = vm.mode == CaptureMode.AUDIO
    val ratio = vm.effectiveAspect.ratio
    val topOffset = insets.calculateTopPadding() + TOP_BAR_HEIGHT

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        // ------------------------------------------------------------- podgląd
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .padding(top = topOffset),
        ) {
            val containerSize = with(density) { Size(maxWidth.toPx(), maxHeight.toPx()) }
            val rect = imageRect(containerSize, ratio)

            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

            val streaming = streamState == PreviewView.StreamState.STREAMING && vm.cameraReady
            val curtain by animateFloatAsState(if (streaming) 0f else 1f, tween(if (streaming) 380 else 120), label = "curtain")
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = curtain }
                    .background(Color.Black),
            )

            // Gesty: szczypanie = zoom, dotknięcie = ostrość/pomiar, podwójne = zmiana obiektywu.
            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, _, zoom, _ ->
                            if (zoom != 1f) vm.setZoomTarget(vm.zoomTarget * zoom, 22f)
                        }
                    }
                    .pointerInput(rect) {
                        detectTapGestures(
                            onTap = { o ->
                                if (rect.contains(o)) {
                                    val point = previewView.meteringPointFactory.createPoint(o.x, o.y)
                                    vm.focus(point)
                                    focusPoint = o
                                    focusTrigger++
                                }
                            },
                            onDoubleTap = { vm.switchLens() },
                        )
                    },
            )

            GridOverlay(vm.settings.grid, ratio, Modifier.fillMaxSize())
            if (vm.settings.level) {
                LevelOverlay(ratio, haptics, Modifier.fillMaxSize())
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

            // Plakietki stanu u góry kadru.
            Column(
                Modifier
                    .offset { IntOffset(rect.left.roundToInt(), rect.top.roundToInt()) }
                    .width(with(density) { rect.width.toDp() })
                    .padding(10.dp),
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        modeBadge(vm)?.let { Badge(it, iconRotation) }
                        val hud = vm.hud
                        if (hud != null && (vm.mode == CaptureMode.PRO || vm.mode == CaptureMode.NIGHT)) {
                            Badge("ISO ${hud.iso} · ${formatShutter(hud.exposureNs)}", iconRotation, mono = true)
                        }
                    }
                    val bins = vm.histogram
                    if (bins != null && vm.settings.histogram && vm.mode.isPhoto) {
                        HistogramView(bins, Modifier.graphicsLayer { alpha = 0.95f })
                    }
                }
            }

            // Odliczanie samowyzwalacza.
            AnimatedVisibility(
                visible = vm.countdown != null,
                enter = fadeIn() + scaleIn(initialScale = 1.6f),
                exit = fadeOut() + scaleOut(targetScale = 0.6f),
                modifier = Modifier
                    .offset { IntOffset(0, (rect.center.y - containerSize.height / 2f).roundToInt()) }
                    .align(Alignment.Center),
            ) {
                Text(
                    "${vm.countdown ?: ""}",
                    fontSize = 120.sp,
                    fontWeight = FontWeight.Thin,
                    color = Color.White,
                    modifier = Modifier.graphicsLayer { rotationZ = iconRotation },
                )
            }

            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = flashAlpha.value }
                    .background(Color.Black),
            )
        }

        if (isAudio) {
            AudioStudio(
                state = vm.audioState,
                millis = vm.audioMillis,
                amplitudes = vm.amplitudes,
                settings = vm.settings,
                recordings = vm.recordings,
                playingId = vm.playingId,
                playbackPosition = vm::playbackPosition,
                onPlay = vm::togglePlay,
                onDelete = vm::deleteRecording,
                contentPadding = PaddingValues(top = topOffset, bottom = 250.dp),
            )
        }

        // ------------------------------------------------------------- górny pasek
        TopBar(
            vm = vm,
            rotation = iconRotation,
            onSettings = { showSettings = true },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = insets.calculateTopPadding()),
        )

        // ------------------------------------------------------------- dolne sterowanie
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f), Color.Black.copy(alpha = 0.85f))))
                .padding(bottom = insets.calculateBottomPadding() + 12.dp, top = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            vm.processing?.let { p ->
                Text(
                    p.label,
                    color = Palette.Amber,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Palette.GlassStrong)
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                )
            }
            if (vm.mode == CaptureMode.PRO && vm.cameraReady) {
                ProPanel(
                    caps = vm.caps,
                    pro = vm.pro,
                    hud = vm.hud,
                    evIndex = vm.evIndex,
                    evMin = vm.evMin,
                    evMax = vm.evMax,
                    evStep = vm.evStep,
                    haptics = haptics,
                    onPro = vm::updatePro,
                    onEv = vm::setEv,
                )
            }
            if (!isAudio) {
                ZoomBar(
                    current = vm.zoomCurrent,
                    target = vm.zoomTarget,
                    min = vm.zoomMin,
                    max = vm.zoomMax,
                    iconRotation = iconRotation,
                    haptics = haptics,
                    onTarget = { v, speed -> vm.setZoomTarget(v, speed) },
                )
            }
            ModeDial(
                modes = CaptureMode.entries,
                selected = vm.mode,
                enabled = !vm.busy,
                haptics = haptics,
                onSelect = vm::selectMode,
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // Lewa strona: miniatura ostatniego ujęcia.
                Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                    val thumb = vm.thumbnail
                    if (!isAudio && thumb != null) {
                        Image(
                            bitmap = thumb.asImageBitmap(),
                            contentDescription = "Ostatnie ujęcie",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(52.dp)
                                .graphicsLayer { rotationZ = iconRotation }
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
                                                Intent(Intent.ACTION_VIEW).setDataAndType(uri, type)
                                                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
                                            )
                                        }
                                    }
                                },
                        )
                    }
                }

                val active = when (vm.mode) {
                    CaptureMode.VIDEO -> vm.recState != RecState.IDLE
                    CaptureMode.AUDIO -> vm.audioState != RecState.IDLE
                    else -> vm.countdown != null
                }
                ShutterButton(
                    mode = vm.mode,
                    active = active,
                    progress = vm.processing?.progress,
                    processing = vm.processing != null,
                    enabled = vm.processing == null && (vm.cameraReady || isAudio),
                    onClick = vm::onShutter,
                )

                // Prawa strona: pauza podczas nagrywania albo przełącznik obiektywu.
                val recording = (vm.mode == CaptureMode.VIDEO && vm.recState != RecState.IDLE) ||
                    (isAudio && vm.audioState != RecState.IDLE)
                val paused = vm.recState == RecState.PAUSED || vm.audioState == RecState.PAUSED
                when {
                    recording -> RoundIcon(
                        if (paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                        if (paused) "Wznów" else "Pauza",
                        iconRotation,
                        size = 56.dp,
                        onClick = vm::togglePause,
                    )
                    !isAudio && vm.caps.hasFront && vm.caps.hasBack -> {
                        val spin by animateFloatAsState(
                            if (vm.lensFacing == CameraSelector.LENS_FACING_FRONT) 180f else 0f,
                            tween(420),
                            label = "switch",
                        )
                        RoundIcon(Icons.Rounded.Cameraswitch, "Zmień obiektyw", iconRotation + spin, size = 56.dp, enabled = !vm.busy, onClick = vm::switchLens)
                    }
                    else -> Spacer(Modifier.size(56.dp))
                }
            }
        }

        // ------------------------------------------------------------- komunikaty
        AnimatedVisibility(
            visible = vm.message != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = topOffset + 64.dp),
        ) {
            Text(
                vm.message ?: "",
                color = Color.White,
                fontSize = 13.sp,
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Palette.PanelHigh.copy(alpha = 0.95f))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }
    }

    if (showSettings) {
        SettingsSheet(
            settings = vm.settings,
            locked = vm.busy,
            onChange = vm::updateSettings,
            onDismiss = { showSettings = false },
        )
    }
}

private fun modeBadge(vm: CameraViewModel): String? = when (vm.mode) {
    CaptureMode.NIGHT -> if (vm.activeExtension == ExtensionMode.NIGHT) "NOC · sprzętowa" else "NOC · ${vm.settings.nightFrames} klatek"
    CaptureMode.HDR -> if (vm.activeExtension == ExtensionMode.HDR) "HDR · sprzętowy" else "HDR · fuzja 3 ekspozycji"
    CaptureMode.PORTRAIT -> if (vm.activeExtension == ExtensionMode.BOKEH) "BOKEH · sprzętowy" else "PORTRET · brak rozszerzenia"
    CaptureMode.VIDEO -> buildString {
        append(vm.settings.videoResolution.label)
        if (vm.caps.videoHdrActive) append(" · HDR")
        if (vm.caps.stabilizationActive) append(" · STAB")
    }
    else -> null
}

@Composable
private fun Badge(text: String, rotation: Float, mono: Boolean = false) {
    Text(
        text,
        color = Color.White,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.8.sp,
        style = if (mono) MonoDigits else androidx.compose.ui.text.TextStyle.Default,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Palette.GlassStrong)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

@Composable
private fun TopBar(vm: CameraViewModel, rotation: Float, onSettings: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(TOP_BAR_HEIGHT)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        val videoRecording = vm.mode == CaptureMode.VIDEO && vm.recState != RecState.IDLE
        if (videoRecording) {
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
            }
        } else Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (vm.mode != CaptureMode.AUDIO) {
                val flashIcon = when (vm.flash) {
                    FlashSetting.OFF -> Icons.Rounded.FlashOff
                    FlashSetting.AUTO -> Icons.Rounded.FlashAuto
                    FlashSetting.ON -> Icons.Rounded.FlashOn
                    FlashSetting.TORCH -> Icons.Rounded.FlashlightOn
                }
                RoundIcon(flashIcon, "Lampa", rotation, tint = if (vm.flash == FlashSetting.OFF) Color.White else Palette.Amber, onClick = vm::cycleFlash)
                if (vm.mode.isPhoto) {
                    LabeledIcon(
                        Icons.Rounded.Timer,
                        if (vm.settings.timerSeconds == 0) null else "${vm.settings.timerSeconds}s",
                        rotation,
                        active = vm.settings.timerSeconds > 0,
                        onClick = vm::cycleTimer,
                    )
                    TextChip(vm.settings.aspect.label, rotation, enabled = !vm.busy, onClick = vm::cycleAspect)
                } else {
                    TextChip(vm.settings.videoResolution.label, rotation, enabled = !vm.busy, onClick = vm::cycleVideoResolution)
                    TextChip(
                        if (vm.settings.videoHdr) "HDR" else "SDR",
                        rotation,
                        enabled = !vm.busy,
                        active = vm.settings.videoHdr,
                        onClick = { vm.updateSettings { it.copy(videoHdr = !it.videoHdr) } },
                    )
                }
                RoundIcon(
                    Icons.Rounded.GridOn,
                    "Siatka: ${vm.settings.grid.label}",
                    rotation,
                    tint = if (vm.settings.grid == com.blacksingularities.registrer.data.GridType.NONE) Palette.TextFaint else Color.White,
                    onClick = {
                        val all = com.blacksingularities.registrer.data.GridType.entries
                        vm.updateSettings { it.copy(grid = all[(all.indexOf(it.grid) + 1) % all.size]) }
                    },
                )
            } else {
                Text("Dyktafon", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(start = 6.dp))
            }
            RoundIcon(Icons.Rounded.Settings, "Ustawienia", rotation, onClick = onSettings)
        }
    }
}

@Composable
private fun RoundIcon(
    icon: ImageVector,
    description: String,
    rotation: Float,
    size: androidx.compose.ui.unit.Dp = 42.dp,
    tint: Color = Color.White,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(Palette.Glass)
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
            .padding(horizontal = 14.dp),
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
