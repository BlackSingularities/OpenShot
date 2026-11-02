// Elementy sterujące aparatu: panel audio, narzędzia, tryby, spust i górny pasek.

package com.blacksingularities.registrer.ui

import com.blacksingularities.registrer.i18n.tr
import com.blacksingularities.registrer.ui.theme.LocalPickerHorizontal
import com.blacksingularities.registrer.ui.theme.PickerOption
import androidx.compose.material.icons.rounded.TimerOff
import com.blacksingularities.registrer.ui.theme.LocalHintOnTap
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.ShutterSpeed
import androidx.compose.runtime.CompositionLocalProvider
import com.blacksingularities.registrer.ui.theme.GlassValueButton
import com.blacksingularities.registrer.ui.theme.GlassToolbar
import androidx.compose.foundation.verticalScroll
import com.blacksingularities.registrer.ui.components.QuickControl
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Exposure
import androidx.compose.material.icons.rounded.BlurOn
import com.blacksingularities.registrer.ui.theme.reactive
import androidx.compose.material.icons.rounded.RawOn
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.HdrOn
import androidx.compose.material.icons.rounded.HdrOff
import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.camera.core.CameraSelector
import androidx.camera.extensions.ExtensionMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cameraswitch
import androidx.compose.material.icons.rounded.FlashAuto
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.FlipToBack
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.PresentToAll
import androidx.compose.material.icons.rounded.Timelapse
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.material.icons.rounded.Flare
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.VideoStable
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.blacksingularities.registrer.camera.CaptureMode
import com.blacksingularities.registrer.camera.FlashSetting
import com.blacksingularities.registrer.data.PhotoFormat
import com.blacksingularities.registrer.ui.components.AudioLive
import com.blacksingularities.registrer.ui.components.AudioPlayback
import com.blacksingularities.registrer.ui.components.AudioStudio
import com.blacksingularities.registrer.ui.components.ModeDial
import com.blacksingularities.registrer.ui.components.ProPanel
import com.blacksingularities.registrer.ui.components.ShutterButton
import com.blacksingularities.registrer.ui.components.ZoomControls
import com.blacksingularities.registrer.ui.theme.Palette
import kotlinx.coroutines.flow.first
import kotlin.math.abs

@Composable
internal fun AudioPane(vm: CameraViewModel, padding: PaddingValues) {
    AudioStudio(
        live = AudioLive(
            state = vm.audioState,
            finishing = vm.audioFinishing,
            millis = vm.audioMillis,
            levelL = vm.audioLevelL,
            levelR = vm.audioLevelR,
            peakDb = vm.audioPeakDb,
            clipping = vm.audioClipping,
            silent = vm.audioSilent,
            skippedMs = vm.audioSkippedMs,
            markers = vm.audioMarkers,
            device = vm.audioDevice,
            format = vm.audioFormat,
        ),
        amplitudes = vm.amplitudes,
        settings = vm.cfg,
        externalMic = vm.externalMic,
        recordings = vm.recordings,
        playback = AudioPlayback(vm.playingId, vm.playPaused, vm.playSpeed, vm::playbackPosition),
        onPlay = vm::togglePlay,
        onSeek = vm::seekPlayback,
        onSkip = vm::skipPlayback,
        onSpeed = vm::cyclePlaybackSpeed,
        onNextMarker = vm::nextMarker,
        onDelete = vm::deleteRecording,
        onRename = vm::renameRecording,
        contentPadding = padding,
    )
}

// ==================================================================== sterowanie

/** Panel Pro / szybkie ustawienie trybu, obiektywy z przełącznikiem zoomu oraz pokrętło trybów. */
@Composable
internal fun CaptureTools(ui: Ui) {
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
            evLabel = String.format(java.util.Locale.US, "%+.1f EV", vm.evIndex * vm.evStep).let { if (com.blacksingularities.registrer.i18n.Lang.polish) it.replace('.', ',') else it },
            onToggleEv = vm::toggleRockerEv,
            onEvRate = vm::nudgeEv,
        )
    }
    Box(Modifier.fillMaxWidth()) {
        ModeDial(
            modes = vm.availableModes,
            selected = vm.mode,
            enabled = !vm.busy,
            haptics = ui.haptics,
            onSelect = vm::selectMode,
        )
        if (vm.mode == CaptureMode.PRO) {
            ProCaptureSwitch(
                vm = vm,
                rotation = ui.iconRotation,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 5.dp, end = 8.dp),
            )
        }
    }
}

/** Dyskretny przełącznik zdjęcie/wideo przy pokrętle trybów (zablokowany podczas nagrywania). */
@Composable
internal fun ProCaptureSwitch(vm: CameraViewModel, rotation: Float, modifier: Modifier = Modifier) {
    val video = vm.cfg.proVideo
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(Color.Black.copy(alpha = 0.28f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(50))
            .padding(2.dp),
    ) {
        listOf(false, true).forEach { isVideo ->
            val selected = video == isVideo
            Box(
                Modifier
                    .size(27.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (selected) {
                            if (isVideo) Palette.Record.copy(alpha = 0.82f) else Color.White.copy(alpha = 0.9f)
                        } else Color.Transparent,
                    )
                    .clickable(enabled = !vm.busy && !selected) { vm.setProVideo(isVideo) }
                    .graphicsLayer { alpha = if (vm.busy) 0.38f else 1f },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (isVideo) Icons.Rounded.Videocam else Icons.Rounded.PhotoCamera,
                    if (isVideo) tr("Tryb ręczny: wideo", "Manual mode: video") else tr("Tryb ręczny: zdjęcia", "Manual mode: photos"),
                    tint = if (selected) (if (isVideo) Color.White else Palette.Ink) else Palette.TextDim,
                    modifier = Modifier.size(14.dp).graphicsLayer { rotationZ = rotation },
                )
            }
        }
    }
}

/**
 * Najważniejsze ustawienie bieżącego trybu, zawsze na ekranie: rozmycie w Portrecie, liczba
 * klatek w Nocy, rozpiętość w HDR, jakość w Audio, a w pozostałych trybach kompensacja EV.
 * Gdy działa rozszerzenie sprzętowe producenta (bez regulacji) – kompensacja EV.
 */
@Composable
internal fun ModeQuickControl(ui: Ui, modifier: Modifier = Modifier) {
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
            label = tr("Kompensacja ekspozycji", "Exposure compensation"),
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
                label = tr("Rozmycie tła", "Background blur"),
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
                label = tr("Liczba klatek nocnych", "Night frame count"),
                options = values.map { if (it == 0) "Auto ${vm.autoNightFrames()}" else "×$it" },
                selected = values.indexOf(cfg.nightFrames).coerceAtLeast(0),
                defaultIndex = 0,
                rotation = rot,
                modifier = modifier,
            ) { i -> vm.updateProfile { it.copy(nightFrames = values[i]) } }
        }
        CaptureMode.TIMELAPSE -> {
            val speeds = com.blacksingularities.registrer.data.TIMELAPSE_SPEEDS
            QuickControl(
                icon = Icons.Rounded.Timelapse,
                label = com.blacksingularities.registrer.ui.components.timelapseHint(cfg.tlSpeed, cfg.tlFps),
                options = speeds.map { "${it.second} ×${it.first}" },
                selected = speeds.indexOfFirst { it.first == cfg.tlSpeed }.coerceAtLeast(0),
                defaultIndex = speeds.indexOfFirst { it.first == 120 },
                rotation = rot,
                enabled = !vm.tlRunning,
                modifier = modifier,
            ) { i -> vm.updateProfile { it.copy(tlSpeed = speeds[i].first) } }
        }
        CaptureMode.LONG -> {
            val values = com.blacksingularities.registrer.ui.components.LONG_SECONDS
            QuickControl(
                icon = Icons.Rounded.ShutterSpeed,
                label = tr("Czas naświetlania · ${cfg.longBlend.label}", "Exposure time · ${cfg.longBlend.label}"),
                options = values.map { "$it s" },
                selected = values.indexOf(cfg.longSeconds).coerceAtLeast(0),
                defaultIndex = values.indexOf(2),
                rotation = rot,
                modifier = modifier,
            ) { i -> vm.updateProfile { it.copy(longSeconds = values[i]) } }
        }
        CaptureMode.HDR -> if (ext == ExtensionMode.HDR) {
            ev()
        } else {
            val values = listOf(1.0f, 1.3f, 1.7f, 2.0f, 2.5f)
            QuickControl(
                icon = Icons.Rounded.HdrOn,
                label = tr("Rozpiętość ekspozycji HDR", "HDR exposure spread"),
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
                label = tr("Jakość nagrania: ${cfg.audioPreset.description}", "Recording quality: ${cfg.audioPreset.description}"),
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
internal fun ShutterCluster(ui: Ui) {
    val vm = ui.vm
    val context = LocalContext.current
    val isAudio = vm.mode == CaptureMode.AUDIO
    val rot = ui.iconRotation

    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
        val thumb = vm.thumbnail
        if (vm.videoMode && vm.recState != RecState.IDLE && vm.session.snapshotInVideo) {
            RoundIcon(Icons.Rounded.PhotoCamera, tr("Zdjęcie z wideo", "Photo from video"), rot, size = 48.dp, onClick = vm::snapshot)
        } else if (isAudio && vm.audioState != RecState.IDLE) {
            RoundIcon(
                Icons.Rounded.Flag,
                tr("Dodaj znacznik", "Add marker"),
                rot,
                size = 48.dp,
                active = vm.audioMarkers > 0,
                badge = vm.audioMarkers.takeIf { it > 0 }?.toString(),
                enabled = !vm.audioFinishing,
                onClick = vm::addAudioMarker,
            )
        } else if (!isAudio && thumb != null) {
            Image(
                bitmap = thumb.asImageBitmap(),
                contentDescription = tr("Ostatnie ujęcie", "Last shot"),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(44.dp)
                    .graphicsLayer { rotationZ = rot }
                    .clip(CircleShape)
                    .border(1.5.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.9f), Color.White.copy(alpha = 0.3f))), CircleShape)
                    .reactive(tr("Galeria", "Gallery")) {
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
        vm.videoMode -> vm.recState != RecState.IDLE
        isAudio -> vm.audioState != RecState.IDLE
        else -> vm.countdown != null || vm.intervalRunning || vm.bursting || vm.tlRunning
    }
    ShutterButton(
        mode = vm.mode,
        video = vm.videoMode,
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

    val recording = (vm.videoMode && vm.recState != RecState.IDLE) || (isAudio && vm.audioState != RecState.IDLE)
    val paused = vm.recState == RecState.PAUSED || vm.audioState == RecState.PAUSED
    when {
        recording -> RoundIcon(
            if (paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
            if (paused) tr("Wznów", "Resume") else tr("Pauza", "Pause"),
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
            RoundIcon(Icons.Rounded.Cameraswitch, tr("Zmień obiektyw", "Switch camera"), rot + spin, size = 48.dp, enabled = !vm.busy, onClick = vm::switchLens)
        }
        else -> Spacer(Modifier.size(48.dp))
    }
}

/** Lampa, samowyzwalacz, proporcje, format, siatki, fold, ustawienia – ikony; nazwy po przytrzymaniu. */
@Composable
internal fun TopControls(ui: Ui, vertical: Boolean) {
    val vm = ui.vm
    val rot = ui.iconRotation
    val items: @Composable () -> Unit = {
        if (vm.mode != CaptureMode.AUDIO) {
            val cfg = vm.cfg
            val (flashIcon, flashName) = when (cfg.flash) {
                FlashSetting.OFF -> Icons.Rounded.FlashOff to tr("Lampa wyłączona", "Flash off")
                FlashSetting.AUTO -> Icons.Rounded.FlashAuto to tr("Lampa automatyczna", "Auto flash")
                FlashSetting.ON -> Icons.Rounded.FlashOn to tr("Lampa włączona", "Flash on")
                FlashSetting.TORCH -> Icons.Rounded.FlashlightOn to tr("Latarka", "Torch")
            }
            RoundIcon(
                flashIcon,
                if (vm.caps.hasFlash) flashName else tr("Brak lampy", "No flash"),
                rot,
                active = cfg.flash != FlashSetting.OFF,
                enabled = vm.caps.hasFlash,
                options = vm.flashOptions().map { f ->
                    when (f) {
                        FlashSetting.OFF -> PickerOption(tr("Lampa wyłączona", "Flash off"), Icons.Rounded.FlashOff, caption = tr("Wył.", "Off"))
                        FlashSetting.AUTO -> PickerOption(tr("Lampa automatyczna", "Auto flash"), Icons.Rounded.FlashAuto, caption = "Auto")
                        FlashSetting.ON -> PickerOption(tr("Lampa włączona", "Flash on"), Icons.Rounded.FlashOn, caption = tr("Wł.", "On"))
                        FlashSetting.TORCH -> PickerOption(tr("Latarka", "Torch"), Icons.Rounded.FlashlightOn, caption = tr("Latarka", "Torch"))
                    }
                },
                selected = vm.flashOptions().indexOf(cfg.flash),
                onPick = { i -> vm.setFlash(vm.flashOptions()[i]) },
                onClick = vm::cycleFlash,
            )
            if (vm.mode == CaptureMode.TIMELAPSE) {
                val resolutions = vm.timelapseResolutions()
                GlassValueButton(
                    cfg.tlResolution.label.removeSuffix("p"),
                    if (cfg.tlResolution.label.endsWith("p")) "p" else null,
                    tr("Film poklatkowy ${cfg.tlResolution.label}", "Time-lapse ${cfg.tlResolution.label}"),
                    rot,
                    active = cfg.tlResolution != com.blacksingularities.registrer.data.TimelapseResolution.FHD,
                    enabled = !vm.busy && resolutions.size > 1,
                    options = resolutions.map { r -> PickerOption(tr("Film ${r.label}", "Video ${r.label}"), value = r.label.removeSuffix("p"), caption = if (r.label.endsWith("p")) "p" else null) },
                    selected = resolutions.indexOf(cfg.tlResolution),
                    onPick = { i -> vm.updateProfile { it.copy(tlResolution = resolutions[i]) } },
                    onClick = { vm.updateProfile { it.copy(tlResolution = resolutions[(resolutions.indexOf(it.tlResolution) + 1).mod(resolutions.size)]) } },
                )
                val fpsOptions = listOf(24, 30, 60)
                GlassValueButton(
                    "${cfg.tlFps}",
                    tr("kl/s", "fps"),
                    tr("Klatkaż filmu ${cfg.tlFps} kl/s", "Video frame rate ${cfg.tlFps} fps"),
                    rot,
                    enabled = !vm.busy,
                    options = fpsOptions.map { f -> PickerOption(tr("Klatkaż filmu $f kl/s", "Video frame rate $f fps"), value = "$f", caption = tr("kl/s", "fps")) },
                    selected = fpsOptions.indexOf(cfg.tlFps),
                    onPick = { i -> vm.updateProfile { it.copy(tlFps = fpsOptions[i]) } },
                    onClick = { vm.updateProfile { it.copy(tlFps = fpsOptions[(fpsOptions.indexOf(it.tlFps) + 1).mod(fpsOptions.size)]) } },
                )
                val stops = listOf(0, 5, 15, 30, 60, 120)
                RoundIcon(
                    if (cfg.tlStopMin > 0) Icons.Rounded.Timer else Icons.Rounded.TimerOff,
                    if (cfg.tlStopMin > 0) tr("Zakończ po ${com.blacksingularities.registrer.ui.components.autoStopLabel(cfg.tlStopMin)}", "Stop after ${com.blacksingularities.registrer.ui.components.autoStopLabel(cfg.tlStopMin)}") else tr("Zakończenie ręczne", "Stop manually"),
                    rot,
                    active = cfg.tlStopMin > 0,
                    badge = cfg.tlStopMin.takeIf { it > 0 }?.let { if (it < 60) "$it" else "${it / 60}h" },
                    enabled = !vm.busy,
                    options = stops.map { m ->
                        if (m == 0) PickerOption(tr("Zakończenie ręczne", "Stop manually"), Icons.Rounded.TimerOff, caption = tr("Ręcznie", "Manual"))
                        else PickerOption(tr("Zakończ po ${com.blacksingularities.registrer.ui.components.autoStopLabel(m)}", "Stop after ${com.blacksingularities.registrer.ui.components.autoStopLabel(m)}"), value = if (m < 60) "$m" else "${m / 60}", caption = if (m < 60) "min" else "h")
                    },
                    selected = stops.indexOf(cfg.tlStopMin).coerceAtLeast(0),
                    onPick = { i -> vm.updateProfile { it.copy(tlStopMin = stops[i]) } },
                    onClick = { vm.updateProfile { it.copy(tlStopMin = stops[(stops.indexOf(it.tlStopMin).coerceAtLeast(0) + 1) % stops.size]) } },
                )
            }
            if (vm.mode == CaptureMode.PRO && cfg.proVideo && vm.caps.flatProfile) {
                GlassValueButton(
                    "LOG",
                    null,
                    if (cfg.flatProfile) tr("Płaski profil (LOG) włączony", "Flat profile (LOG) on") else tr("Płaski profil (LOG) wyłączony", "Flat profile (LOG) off"),
                    rot,
                    active = cfg.flatProfile,
                    enabled = !vm.busy,
                    onClick = { vm.updateProfile { it.copy(flatProfile = !it.flatProfile) } },
                )
            }
            if (vm.mode == CaptureMode.PRO && !cfg.proVideo) {
                val counts = listOf(0, 3, 5, 7)
                RoundIcon(
                    Icons.Rounded.Exposure,
                    if (cfg.bracketCount == 0) tr("Braketowanie wyłączone", "Bracketing off") else tr("Braketowanie: ${cfg.bracketCount} zdjęć", "Bracketing: ${cfg.bracketCount} shots"),
                    rot,
                    active = cfg.bracketCount > 0,
                    badge = cfg.bracketCount.takeIf { it > 0 }?.toString(),
                    enabled = !vm.busy,
                    options = counts.map { c ->
                        if (c == 0) {
                            PickerOption(tr("Braketowanie wyłączone", "Bracketing off"), Icons.Rounded.Exposure, caption = tr("Wył.", "Off"))
                        } else {
                            PickerOption(tr("Braketowanie: $c zdjęć", "Bracketing: $c shots"), value = "$c", caption = "BRK")
                        }
                    },
                    selected = counts.indexOf(cfg.bracketCount).coerceAtLeast(0),
                    onPick = { i -> vm.updateProfile { it.copy(bracketCount = counts[i]) } },
                    onClick = { vm.updateProfile { it.copy(bracketCount = counts[(counts.indexOf(it.bracketCount).coerceAtLeast(0) + 1) % counts.size]) } },
                )
            }
            if (vm.mode == CaptureMode.LONG) {
                val blends = com.blacksingularities.registrer.data.LongBlend.entries
                fun blendIcon(b: com.blacksingularities.registrer.data.LongBlend) =
                    if (b == com.blacksingularities.registrer.data.LongBlend.TRAILS) Icons.Rounded.Flare else Icons.Rounded.Waves
                RoundIcon(
                    blendIcon(cfg.longBlend),
                    cfg.longBlend.label,
                    rot,
                    active = cfg.longBlend == com.blacksingularities.registrer.data.LongBlend.TRAILS,
                    enabled = !vm.busy,
                    options = blends.map { PickerOption(it.label, blendIcon(it), caption = if (it == com.blacksingularities.registrer.data.LongBlend.TRAILS) tr("Smugi", "Trails") else tr("Ruch", "Motion")) },
                    selected = blends.indexOf(cfg.longBlend),
                    onPick = { i -> vm.updateProfile { it.copy(longBlend = blends[i]) } },
                    onClick = { vm.updateProfile { it.copy(longBlend = blends[(blends.indexOf(it.longBlend) + 1) % blends.size]) } },
                )
            }
            if (vm.mode.isPhoto && !vm.videoMode) {
                RoundIcon(
                    Icons.Rounded.Timer,
                    if (cfg.timerSeconds == 0) tr("Samowyzwalacz wyłączony", "Self-timer off") else tr("Samowyzwalacz ${cfg.timerSeconds} s", "Self-timer ${cfg.timerSeconds} s"),
                    rot,
                    active = cfg.timerSeconds > 0,
                    badge = cfg.timerSeconds.takeIf { it > 0 }?.toString(),
                    options = TIMER_STEPS.map { t ->
                        if (t == 0) PickerOption(tr("Samowyzwalacz wyłączony", "Self-timer off"), Icons.Rounded.TimerOff, caption = tr("Wył.", "Off"))
                        else PickerOption(tr("Samowyzwalacz $t s", "Self-timer $t s"), value = "$t", caption = "s")
                    },
                    selected = TIMER_STEPS.indexOf(cfg.timerSeconds),
                    onPick = { i -> vm.setTimer(TIMER_STEPS[i]) },
                    onClick = vm::cycleTimer,
                )
                val frame = remember(cfg.aspect) { frameIcon(cfg.aspect) }
                RoundIcon(
                    frame,
                    tr("Proporcje ${cfg.aspect.label}", "Aspect ratio ${cfg.aspect.label}"),
                    rot,
                    enabled = !vm.busy,
                    options = com.blacksingularities.registrer.data.AspectRatioMode.entries.map { a ->
                        PickerOption(tr("Proporcje ${a.label}", "Aspect ratio ${a.label}"), frameIcon(a), caption = a.label)
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
            } else if (vm.videoMode) {
                if (vm.mode != CaptureMode.SLOWMO) {
                    val (resValue, resUnit) = resolutionGlyph(cfg.videoResolution)
                    GlassValueButton(
                        resValue,
                        resUnit,
                        tr("Rozdzielczość ${cfg.videoResolution.label}", "Resolution ${cfg.videoResolution.label}"),
                        rot,
                        active = cfg.videoResolution == com.blacksingularities.registrer.data.VideoResolution.UHD,
                        enabled = !vm.busy && vm.caps.videoResolutions.size > 1,
                        options = vm.caps.videoResolutions.map { r ->
                            val (v, u) = resolutionGlyph(r)
                            PickerOption(tr("Rozdzielczość ${r.label}", "Resolution ${r.label}"), value = v, caption = u)
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
                        tr("Zwolnione tempo ${current.label}", "Slow motion ${current.label}"),
                        rot,
                        active = true,
                        enabled = !vm.busy && resolutions.size > 1,
                        options = resolutions.map { r ->
                            val (v, u) = resolutionGlyph(r)
                            PickerOption(tr("Zwolnione tempo ${r.label}", "Slow motion ${r.label}"), value = v, caption = u)
                        },
                        selected = resolutions.indexOf(current),
                        onPick = { i -> vm.setSlowMoResolution(resolutions[i]) },
                        onClick = vm::cycleVideoResolution,
                    )
                }
                val slowFps = vm.slowMoFpsOptions()
                val fpsChoices = if (vm.mode == CaptureMode.SLOWMO) slowFps.size else vm.videoFpsOptions().size
                val autoFps = vm.mode != CaptureMode.SLOWMO && cfg.videoFps == com.blacksingularities.registrer.data.VideoFps.AUTO
                val fpsNumber = vm.fpsLabel.removeSuffix(tr(" kl/s", " fps")).removePrefix("Auto").trim()
                GlassValueButton(
                    if (autoFps) (vm.session.fps.takeIf { it > 0 }?.toString() ?: "A") else fpsNumber,
                    if (autoFps) "auto" else tr("kl/s", "fps"),
                    tr("Klatkaż ${vm.fpsLabel}", "Frame rate ${vm.fpsLabel}"),
                    rot,
                    active = !autoFps,
                    enabled = !vm.busy && fpsChoices > 1,
                    options = if (vm.mode == CaptureMode.SLOWMO) {
                        slowFps.map { f -> PickerOption(tr("Zwolnione tempo $f kl/s", "Slow motion $f fps"), value = "$f", caption = tr("kl/s", "fps")) }
                    } else {
                        vm.videoFpsOptions().map { f ->
                            if (f == com.blacksingularities.registrer.data.VideoFps.AUTO) PickerOption(tr("Klatkaż automatyczny", "Auto frame rate"), value = "A", caption = "auto")
                            else PickerOption(tr("Klatkaż ${f.fps} kl/s", "Frame rate ${f.fps} fps"), value = "${f.fps}", caption = tr("kl/s", "fps"))
                        }
                    },
                    selected = if (vm.mode == CaptureMode.SLOWMO) slowFps.indexOf(cfg.slowMoFps).coerceAtLeast(0)
                        else vm.videoFpsOptions().indexOf(cfg.videoFps),
                    onPick = { i ->
                        if (vm.mode == CaptureMode.SLOWMO) vm.setSlowMoFps(slowFps[i]) else vm.setVideoFps(vm.videoFpsOptions()[i])
                    },
                    onClick = vm::cycleVideoFps,
                )
                if (vm.mode != CaptureMode.SLOWMO && vm.caps.videoHlg) {
                    RoundIcon(
                        if (cfg.videoHdr) Icons.Rounded.HdrOn else Icons.Rounded.HdrOff,
                        if (cfg.videoHdr) tr("Wideo HDR (HLG)", "HDR video (HLG)") else tr("Wideo SDR", "SDR video"),
                        rot,
                        active = cfg.videoHdr,
                        enabled = !vm.busy,
                        options = listOf(
                            PickerOption(tr("Wideo SDR", "SDR video"), Icons.Rounded.HdrOff, caption = "SDR"),
                            PickerOption(tr("Wideo HDR (HLG)", "HDR video (HLG)"), Icons.Rounded.HdrOn, caption = "HLG"),
                        ),
                        selected = if (cfg.videoHdr) 1 else 0,
                        onPick = { i -> vm.setVideoHdr(i == 1) },
                        onClick = { vm.updateProfile { it.copy(videoHdr = !it.videoHdr) } },
                    )
                }
                if (vm.mode != CaptureMode.SLOWMO && vm.caps.videoStabilization) {
                    RoundIcon(
                        Icons.Rounded.VideoStable,
                        if (cfg.stabilization) tr("Stabilizacja włączona", "Stabilization on") else tr("Stabilizacja wyłączona", "Stabilization off"),
                        rot,
                        active = cfg.stabilization,
                        enabled = !vm.busy,
                        options = listOf(
                            PickerOption(tr("Stabilizacja wyłączona", "Stabilization off"), Icons.Rounded.VideoStable, caption = tr("Wył.", "Off")),
                            PickerOption(tr("Stabilizacja włączona", "Stabilization on"), Icons.Rounded.VideoStable, caption = tr("Wł.", "On")),
                        ),
                        selected = if (cfg.stabilization) 1 else 0,
                        onPick = { i -> vm.setStabilization(i == 1) },
                        onClick = { vm.setStabilization(!cfg.stabilization) },
                    )
                }
            }
            GridButton(vm, rot)
            if (vm.subjectPreviewAvailable || vm.subjectPreviewActive) {
                RoundIcon(
                    Icons.Rounded.PresentToAll,
                    if (vm.subjectPreviewActive) tr("Wyłącz podgląd dla fotografowanego", "Turn off subject preview") else tr("Podgląd dla fotografowanego na drugim ekranie", "Subject preview on the second screen"),
                    rot,
                    active = vm.subjectPreviewActive,
                    onClick = ui.onSubjectPreview,
                )
            }
            if (vm.rearDisplayAvailable || vm.rearDisplayActive) {
                RoundIcon(
                    Icons.Rounded.FlipToBack,
                    if (vm.rearDisplayActive) tr("Wróć na ekran wewnętrzny", "Back to the inner screen") else tr("Autoportret głównym aparatem", "Selfie with the main camera"),
                    rot,
                    active = vm.rearDisplayActive,
                    enabled = !vm.busy,
                    onClick = ui.onFold,
                )
            }
        } else {
            AudioTopControls(vm, rot)
        }
    }
    val settings: @Composable () -> Unit = {
        RoundIcon(Icons.Rounded.Settings, tr("Ustawienia", "Settings"), rot, onClick = ui.onSettings)
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
