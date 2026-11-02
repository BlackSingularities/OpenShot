// Drobne elementy ekranu aparatu: ekspozycja, plakietki, licznik nagrania, przyciski.

package com.blacksingularities.registrer.ui

import com.blacksingularities.registrer.i18n.tr
import com.blacksingularities.registrer.ui.theme.activeContent
import com.blacksingularities.registrer.ui.theme.PickerOption
import androidx.compose.material.icons.rounded.TimerOff
import com.blacksingularities.registrer.ui.theme.LocalHintOnTap
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.ShutterSpeed
import androidx.compose.material.icons.rounded.Camera
import androidx.compose.runtime.CompositionLocalProvider
import com.blacksingularities.registrer.ui.theme.BlockShape
import com.blacksingularities.registrer.ui.theme.GlassValueButton
import androidx.compose.material.icons.rounded.Exposure
import com.blacksingularities.registrer.ui.theme.glass
import com.blacksingularities.registrer.ui.theme.GlassIconButton
import com.blacksingularities.registrer.ui.theme.GlassChip
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Image
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CenterFocusWeak
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Gradient
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.NoiseControlOff
import androidx.compose.material.icons.rounded.NoiseAware
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Headset
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blacksingularities.registrer.camera.CaptureMode
import com.blacksingularities.registrer.data.GridType
import com.blacksingularities.registrer.ui.components.formatClock
import com.blacksingularities.registrer.ui.components.formatShutter
import com.blacksingularities.registrer.ui.theme.MonoDigits
import com.blacksingularities.registrer.ui.theme.Palette
import com.blacksingularities.registrer.ui.theme.rotatedLayout
import com.blacksingularities.registrer.update.UpdateState
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

internal val TIMER_STEPS = listOf(0, 3, 5, 10)

internal fun resolutionGlyph(r: com.blacksingularities.registrer.data.VideoResolution): Pair<String, String> = when (r) {
    com.blacksingularities.registrer.data.VideoResolution.UHD -> "4K" to "2160"
    com.blacksingularities.registrer.data.VideoResolution.FHD -> "FHD" to "1080"
    com.blacksingularities.registrer.data.VideoResolution.HD -> "HD" to "720"
}

/** Ikona ramki o proporcjach kadru (pionowo: 3:4 albo 9:16). */
internal fun frameIcon(aspect: com.blacksingularities.registrer.data.AspectRatioMode): ImageVector {
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
internal fun ExposureStrip(vm: CameraViewModel, modifier: Modifier = Modifier) {
    val hud = vm.hud
    val pro = if (vm.mode == CaptureMode.PRO) vm.pro else null
    data class Item(val icon: ImageVector?, val name: String, val value: String, val manual: Boolean = false)
    val items = buildList {
        add(Item(if (vm.videoMode) Icons.Rounded.Videocam else Icons.Rounded.Image, tr("Rozdzielczość", "Resolution"), resolutionText(vm)))
        val iso = pro?.iso ?: hud?.iso
        add(Item(null, "ISO", iso?.toString() ?: "—", pro?.iso != null))
        val shutter = pro?.exposureNs ?: hud?.exposureNs
        add(Item(Icons.Rounded.ShutterSpeed, tr("Czas naświetlania", "Shutter"), shutter?.let { formatShutter(it) } ?: "—", pro?.exposureNs != null))
        hud?.aperture?.let { add(Item(Icons.Rounded.Camera, tr("Przysłona", "Aperture"), String.format(java.util.Locale.US, "f/%.1f", it))) }
        add(Item(Icons.Rounded.Exposure, tr("Kompensacja EV", "EV compensation"), String.format(java.util.Locale.US, "%+.1f", vm.evIndex * vm.evStep), vm.evIndex != 0))
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
internal fun resolutionText(vm: CameraViewModel): String {
    val size = vm.session.outputSize
    return if (vm.videoMode) {
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

internal fun isVideoRecording(vm: CameraViewModel) = vm.videoMode && vm.recState != RecState.IDLE

@Composable
internal fun RecordingClock(vm: CameraViewModel, rotation: Float) {
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
        if (vm.recMuted) Text(tr("  ·  bez dźwięku", "  ·  no sound"), fontSize = 11.sp, color = Palette.TextDim)
    }
}

// ==================================================================== drobne elementy

@Composable
internal fun Badge(text: String, mono: Boolean = false, color: Color = Color.White, onClick: (() -> Unit)? = null) {
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

/**
 * Pomoce kadru: menu z zaznaczaniem – siatki (kilka naraz), poziomica, obrys ostrości i zebra.
 * Ustawienia ogólne, jednakowe we wszystkich trybach z obrazem. Histogram jest w ustawieniach.
 */
@Composable
internal fun GridButton(vm: CameraViewModel, rotation: Float) {
    var open by remember { mutableStateOf(false) }
    val g = vm.global
    val grids = g.grids
    val anyOn = grids.isNotEmpty() || g.peaking || g.zebra || g.level
    Box {
        // Menu otwiera się dotknięciem – bez dymka.
        CompositionLocalProvider(LocalHintOnTap provides false) {
            RoundIcon(
                Icons.Rounded.GridOn,
                tr("Pomoce kadru", "Framing aids"),
                rotation,
                tint = if (anyOn) Color.White else Palette.TextFaint,
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
            @Composable
            fun Item(label: String, on: Boolean, icon: ImageVector?, onClick: () -> Unit) {
                val check: @Composable () -> Unit = {
                    Icon(if (on) Icons.Rounded.CheckBox else Icons.Rounded.CheckBoxOutlineBlank, null, tint = if (on) Palette.Amber else Palette.TextDim)
                }
                DropdownMenuItem(
                    modifier = Modifier.padding(vertical = 1.dp).clip(RoundedCornerShape(14.dp)).background(Color.Black.copy(alpha = 0.58f)),
                    text = { Text(label, color = if (on) Palette.Amber else Color.White, fontWeight = if (on) FontWeight.Bold else FontWeight.Normal) },
                    // Siatki: samo pole wyboru; pozostałe: ikona funkcji i pole wyboru z prawej.
                    leadingIcon = {
                        if (icon != null) Icon(icon, null, tint = if (on) Palette.Amber else Palette.TextDim) else check()
                    },
                    trailingIcon = if (icon != null) check else null,
                    onClick = onClick,
                )
            }
            GridType.entries.forEach { grid ->
                Item(grid.label, grid in grids, null) {
                    vm.updateGlobal { it.copy(grids = if (grid in it.grids) it.grids - grid else it.grids + grid) }
                }
            }
            Item(tr("Poziomica", "Level"), g.level, Icons.Rounded.Straighten) { vm.updateGlobal { it.copy(level = !it.level) } }
            Item(tr("Obrys ostrości", "Focus peaking"), g.peaking, Icons.Rounded.CenterFocusWeak) { vm.updateGlobal { it.copy(peaking = !it.peaking) } }
            Item(tr("Zebra", "Zebra"), g.zebra, Icons.Rounded.Gradient) { vm.updateGlobal { it.copy(zebra = !it.zebra) } }
            if (anyOn) {
                DropdownMenuItem(
                    modifier = Modifier.padding(vertical = 1.dp).clip(RoundedCornerShape(14.dp)).background(Color.Black.copy(alpha = 0.58f)),
                    text = { Text(tr("Wyłącz wszystkie", "Turn all off"), color = Palette.TextDim) },
                    leadingIcon = { Icon(Icons.Rounded.Close, null, tint = Palette.TextDim) },
                    onClick = {
                        vm.updateGlobal { it.copy(grids = emptySet(), level = false, peaking = false, zebra = false) }
                        open = false
                    },
                )
            }
        }
    }
}

/** Postęp filmu poklatkowego: liczba klatek, długość filmu i czas nagrywania. */
@Composable
internal fun TimelapseBadge(vm: CameraViewModel) {
    var now by remember { mutableLongStateOf(android.os.SystemClock.elapsedRealtime()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = android.os.SystemClock.elapsedRealtime()
            delay(500)
        }
    }
    val elapsed = (now - vm.tlStartedAt).coerceAtLeast(0)
    val film = vm.tlFrames.toFloat() / vm.cfg.tlFps.coerceAtLeast(1)
    val size = vm.tlSize?.let { "$it · " } ?: ""
    Badge(
        tr("● Poklatkowy · $size${vm.tlFrames} kl. · film ", "● Time-lapse · $size${vm.tlFrames} fr. · video ") +
            String.format(com.blacksingularities.registrer.i18n.Lang.locale, "%.1f", film) + " s · " +
            com.blacksingularities.registrer.ui.components.formatClock(elapsed, false),
        color = Palette.Record,
    )
}

/** Informacja o nowej wersji – sprawdzanej automatycznie przy starcie i po powrocie do aplikacji. */
@Composable
internal fun UpdateBanner(
    state: UpdateState,
    visible: Boolean,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val content: Triple<String, String?, (() -> Unit)?>? = when (state) {
        is UpdateState.Available -> Triple(tr("Nowa wersja ${state.info.version}", "New version ${state.info.version}"), tr("Pobierz", "Download"), onDownload)
        is UpdateState.Downloading -> Triple(tr("Pobieranie ${state.info.version} · ${(state.progress * 100).roundToInt()}%", "Downloading ${state.info.version} · ${(state.progress * 100).roundToInt()}%"), null, null)
        is UpdateState.Ready -> Triple(tr("Wersja ${state.info.version} gotowa", "Version ${state.info.version} ready"), tr("Zainstaluj", "Install"), onInstall)
        is UpdateState.Installing -> Triple(tr("Instalowanie ${state.info.version}…", "Installing ${state.info.version}…"), null, null)
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
                    Icon(Icons.Rounded.Close, tr("Zamknij", "Close"), tint = Palette.TextDim, modifier = Modifier.size(16.dp))
                }
            } else {
                Spacer(Modifier.width(10.dp))
            }
        }
    }
}

/** Górny pasek dyktafonu: wejście, wzmocnienie, redukcja szumów, pomijanie ciszy, automatyczny stop. */
@Composable
internal fun AudioTopControls(vm: CameraViewModel, rot: Float) {
    val cfg = vm.cfg
    val idle = vm.audioState == RecState.IDLE
    val inputs = com.blacksingularities.registrer.data.AudioInput.entries
    fun inputIcon(i: com.blacksingularities.registrer.data.AudioInput) = when (i) {
        com.blacksingularities.registrer.data.AudioInput.AUTO -> Icons.Rounded.Mic
        com.blacksingularities.registrer.data.AudioInput.BUILTIN -> Icons.Rounded.Smartphone
        com.blacksingularities.registrer.data.AudioInput.EXTERNAL -> Icons.Rounded.Headset
    }
    RoundIcon(
        inputIcon(cfg.audioInput),
        tr("Mikrofon: ${cfg.audioInput.label}", "Microphone: ${cfg.audioInput.label}") + (vm.externalMic?.let { tr(" · podłączony $it", " · connected $it") } ?: ""),
        rot,
        active = cfg.audioInput != com.blacksingularities.registrer.data.AudioInput.AUTO || vm.externalMic != null,
        enabled = idle,
        options = inputs.map { PickerOption(tr("Mikrofon: ${it.label}", "Microphone: ${it.label}"), inputIcon(it), caption = it.label) },
        selected = inputs.indexOf(cfg.audioInput),
        onPick = { i -> vm.updateProfile { it.copy(audioInput = inputs[i]) } },
        onClick = { vm.updateProfile { it.copy(audioInput = inputs[(inputs.indexOf(it.audioInput) + 1) % inputs.size]) } },
    )
    GlassValueButton(
        if (cfg.audioGainDb == 0) "0" else "+${cfg.audioGainDb}",
        "dB",
        tr("Wzmocnienie ${if (cfg.audioGainDb == 0) "wyłączone" else "+${cfg.audioGainDb} dB"}", "Gain ${if (cfg.audioGainDb == 0) "off" else "+${cfg.audioGainDb} dB"}"),
        rot,
        active = cfg.audioGainDb > 0,
        enabled = idle,
        options = AUDIO_GAIN_STEPS.map { g -> PickerOption(if (g == 0) tr("Bez wzmocnienia", "No gain") else tr("Wzmocnienie +$g dB", "Gain +$g dB"), value = if (g == 0) "0" else "+$g", caption = "dB") },
        selected = AUDIO_GAIN_STEPS.indexOf(cfg.audioGainDb).coerceAtLeast(0),
        onPick = { i -> vm.updateProfile { it.copy(audioGainDb = AUDIO_GAIN_STEPS[i]) } },
        onClick = {
            vm.updateProfile {
                val i = AUDIO_GAIN_STEPS.indexOf(it.audioGainDb).coerceAtLeast(0)
                it.copy(audioGainDb = AUDIO_GAIN_STEPS[(i + 1) % AUDIO_GAIN_STEPS.size])
            }
        },
    )
    if (com.blacksingularities.registrer.audio.AudioEngine.noiseSuppressionAvailable) {
        RoundIcon(
            if (cfg.audioNoiseSuppression) Icons.Rounded.NoiseAware else Icons.Rounded.NoiseControlOff,
            if (cfg.audioNoiseSuppression) tr("Redukcja szumów włączona", "Noise reduction on") else tr("Redukcja szumów wyłączona", "Noise reduction off"),
            rot,
            active = cfg.audioNoiseSuppression,
            enabled = idle,
            onClick = { vm.updateProfile { it.copy(audioNoiseSuppression = !it.audioNoiseSuppression) } },
        )
    }
    RoundIcon(
        Icons.Rounded.FastForward,
        if (cfg.audioSkipSilence) tr("Pomijanie ciszy włączone", "Skip silence on") else tr("Pomijanie ciszy wyłączone", "Skip silence off"),
        rot,
        active = cfg.audioSkipSilence,
        enabled = idle,
        onClick = { vm.updateProfile { it.copy(audioSkipSilence = !it.audioSkipSilence) } },
    )
    RoundIcon(
        if (cfg.audioAutoStopMin > 0) Icons.Rounded.Timer else Icons.Rounded.TimerOff,
        if (cfg.audioAutoStopMin > 0) tr("Automatyczny stop po ${com.blacksingularities.registrer.ui.components.autoStopLabel(cfg.audioAutoStopMin)}", "Auto stop after ${com.blacksingularities.registrer.ui.components.autoStopLabel(cfg.audioAutoStopMin)}") else tr("Bez automatycznego stopu", "No auto stop"),
        rot,
        active = cfg.audioAutoStopMin > 0,
        badge = cfg.audioAutoStopMin.takeIf { it > 0 }?.let { if (it < 60) "$it" else "${it / 60}h" },
        enabled = idle,
        options = AUDIO_AUTOSTOP_STEPS.map { m ->
            if (m == 0) PickerOption(tr("Bez automatycznego stopu", "No auto stop"), Icons.Rounded.TimerOff, caption = tr("Wył.", "Off"))
            else PickerOption(tr("Stop po ${com.blacksingularities.registrer.ui.components.autoStopLabel(m)}", "Stop after ${com.blacksingularities.registrer.ui.components.autoStopLabel(m)}"), value = if (m < 60) "$m" else "${m / 60}", caption = if (m < 60) "min" else "h")
        },
        selected = AUDIO_AUTOSTOP_STEPS.indexOf(cfg.audioAutoStopMin).coerceAtLeast(0),
        onPick = { i -> vm.updateProfile { it.copy(audioAutoStopMin = AUDIO_AUTOSTOP_STEPS[i]) } },
        onClick = {
            vm.updateProfile {
                val i = AUDIO_AUTOSTOP_STEPS.indexOf(it.audioAutoStopMin).coerceAtLeast(0)
                it.copy(audioAutoStopMin = AUDIO_AUTOSTOP_STEPS[(i + 1) % AUDIO_AUTOSTOP_STEPS.size])
            }
        },
    )
}

val AUDIO_GAIN_STEPS = listOf(0, 6, 12, 18, 24)
val AUDIO_AUTOSTOP_STEPS = listOf(0, 15, 30, 60, 120)

@Composable
internal fun RoundIcon(
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
internal fun TextChip(
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
