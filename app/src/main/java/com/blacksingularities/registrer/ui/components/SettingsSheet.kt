package com.blacksingularities.registrer.ui.components

import com.blacksingularities.registrer.ui.theme.BlockShape
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Brightness4
import androidx.compose.material.icons.rounded.BurstMode
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DataUsage
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Flip
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.HdrOn
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Portrait
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SlowMotionVideo
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material.icons.rounded.AvTimer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.rounded.Close
import androidx.compose.ui.input.pointer.pointerInput
import com.blacksingularities.registrer.ui.theme.GlassIconButton
import com.blacksingularities.registrer.ui.theme.GlassLevel
import com.blacksingularities.registrer.ui.theme.glass
import com.blacksingularities.registrer.ui.theme.reactive
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blacksingularities.registrer.camera.CameraCaps
import com.blacksingularities.registrer.camera.CaptureMode
import com.blacksingularities.registrer.data.AspectRatioMode
import com.blacksingularities.registrer.data.AudioPreset
import com.blacksingularities.registrer.data.AudioSourceType
import com.blacksingularities.registrer.data.CapturePriority
import com.blacksingularities.registrer.data.GlobalSettings
import com.blacksingularities.registrer.data.GridType
import com.blacksingularities.registrer.data.ModeSettings
import com.blacksingularities.registrer.data.PhotoFormat
import com.blacksingularities.registrer.data.PortraitEffect
import com.blacksingularities.registrer.data.VideoBitrate
import com.blacksingularities.registrer.data.VideoFps
import com.blacksingularities.registrer.data.VolumeAction
import com.blacksingularities.registrer.data.ScreenRotation
import com.blacksingularities.registrer.ui.theme.Palette
import com.blacksingularities.registrer.update.UpdateManager
import com.blacksingularities.registrer.update.UpdateState
import java.util.Locale
import kotlin.math.roundToInt

fun modeIcon(mode: CaptureMode): ImageVector = when (mode) {
    CaptureMode.NIGHT -> Icons.Rounded.NightsStay
    CaptureMode.PORTRAIT -> Icons.Rounded.Portrait
    CaptureMode.PHOTO -> Icons.Rounded.PhotoCamera
    CaptureMode.HDR -> Icons.Rounded.HdrOn
    CaptureMode.PRO -> Icons.Rounded.Tune
    CaptureMode.VIDEO -> Icons.Rounded.Videocam
    CaptureMode.SLOWMO -> Icons.Rounded.SlowMotionVideo
    CaptureMode.AUDIO -> Icons.Rounded.Mic
}

/** Zakładka ustawień: profil trybu albo ustawienia ogólne (null). */
@Composable
fun SettingsSheet(
    initialMode: CaptureMode,
    modes: List<CaptureMode>,
    profiles: Map<CaptureMode, ModeSettings>,
    global: GlobalSettings,
    caps: CameraCaps,
    locked: Boolean,
    update: UpdateState,
    onProfile: (CaptureMode, (ModeSettings) -> ModeSettings) -> Unit,
    onGlobal: ((GlobalSettings) -> GlobalSettings) -> Unit,
    onReset: (CaptureMode) -> Unit,
    onCheckUpdate: () -> Unit,
    onDownloadUpdate: () -> Unit,
    onInstallUpdate: () -> Unit,
    onDismiss: () -> Unit,
) {
    var tab by remember { mutableStateOf<CaptureMode?>(initialMode) }

    // Arkusz w tym samym oknie co podgląd – dzięki temu tło jest rozmytym obrazem kamery.
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = maxHeight * 0.86f)
                .padding(horizontal = 6.dp)
                .glass(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp), GlassLevel.Thick)
                .pointerInput(Unit) { detectTapGestures { } }
                .navigationBarsPadding(),
        ) {
            Box(
                Modifier
                    .padding(top = 10.dp)
                    .align(Alignment.CenterHorizontally)
                    .size(width = 38.dp, height = 4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.35f)),
            )
            Row(Modifier.padding(start = 20.dp, end = 12.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Ustawienia", fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                GlassIconButton(Icons.Rounded.Close, "Zamknij", 0f, size = 38.dp, onClick = onDismiss)
            }
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                TabChip(Icons.Rounded.Settings, "Ogólne", tab == null) { tab = null }
                modes.forEach { m -> TabChip(modeIcon(m), m.label, tab == m) { tab = m } }
            }
            Spacer(Modifier.height(10.dp))
            AnimatedContent(
                modifier = Modifier.weight(1f, fill = false),
                targetState = tab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "settingsTab",
            ) { current ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (locked) {
                        Text("Część opcji jest zablokowana podczas nagrywania lub przetwarzania.", color = Palette.Amber, fontSize = 12.sp)
                    }
                    if (current == null) {
                        GeneralTab(global, caps, update, onGlobal, onCheckUpdate, onDownloadUpdate, onInstallUpdate)
                    } else {
                        ModeTab(current, profiles.getValue(current), caps, !locked) { t -> onProfile(current, t) }
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { onReset(current) }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Rounded.RestartAlt, null, tint = Palette.TextDim, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Przywróć domyślne dla trybu ${current.label}", color = Palette.TextDim, fontSize = 13.sp)
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
private fun ModeTab(mode: CaptureMode, s: ModeSettings, caps: CameraCaps, enabled: Boolean, change: ((ModeSettings) -> ModeSettings) -> Unit) {
    val isPhoto = mode.isPhoto
    when (mode) {
        CaptureMode.VIDEO -> {
            Group("Obraz") {
                Chips(Icons.Rounded.HighQuality, "Rozdzielczość", caps.videoResolutions, s.videoResolution, { it.label }, enabled) { v -> change { it.copy(videoResolution = v) } }
                val fpsOptions = VideoFps.entries.filter {
                    when (it) {
                        VideoFps.F60 -> caps.fps60
                        VideoFps.F24 -> caps.fps24
                        else -> true
                    }
                }
                Chips(Icons.Rounded.Speed, "Klatki na sekundę", fpsOptions, s.videoFps, { it.label }, enabled) { v -> change { it.copy(videoFps = v) } }
                Chips(Icons.Rounded.DataUsage, "Przepływność", VideoBitrate.entries, s.bitrate, { it.label }, enabled) { v -> change { it.copy(bitrate = v) } }
                if (caps.videoHlg) {
                    SwitchRow(Icons.Rounded.HdrOn, "HDR 10-bit (HLG)", "Szerszy zakres tonalny, większe pliki", s.videoHdr, enabled) { v -> change { it.copy(videoHdr = v) } }
                }
                if (caps.videoStabilization) {
                    SwitchRow(Icons.Rounded.CenterFocusStrong, "Stabilizacja", "Mniej drgań, lekko węższy kadr", s.stabilization, enabled) { v -> change { it.copy(stabilization = v) } }
                }
                if (caps.lowLightBoost) {
                    SwitchRow(Icons.Rounded.Brightness4, "Wzmocnienie w ciemności", "Jaśniejszy obraz przy słabym świetle", s.lowLightBoost, enabled) { v -> change { it.copy(lowLightBoost = v) } }
                }
            }
            Group("Dźwięk i zoom") {
                SwitchRow(Icons.Rounded.Mic, "Nagrywaj dźwięk", null, s.videoAudio, enabled) { v -> change { it.copy(videoAudio = v) } }
                SwitchRow(Icons.Rounded.ZoomIn, "Kinowy zoom", "Wolny, płynny najazd podczas nagrywania", s.cinematicZoom) { v -> change { it.copy(cinematicZoom = v) } }
            }
            FramingGroup(s, change, showAspect = false, showHistogram = false, showTimer = false)
        }

        CaptureMode.SLOWMO -> {
            Group("Zwolnione tempo") {
                if (caps.slowMoRanges.isEmpty()) {
                    InfoRow(Icons.Rounded.Info, "Ten obiektyw nie obsługuje nagrywania szybkoklatkowego")
                } else {
                    val options = caps.slowMoRanges.map { it.upper }
                    val selected = options.firstOrNull { it == s.slowMoFps } ?: options.last()
                    Chips(Icons.Rounded.SlowMotionVideo, "Prędkość nagrywania", options, selected, { "$it kl/s · 1/${it / 30}×" }, enabled) { v ->
                        change { it.copy(slowMoFps = v) }
                    }
                }
                SwitchRow(Icons.Rounded.ZoomIn, "Kinowy zoom", null, s.cinematicZoom) { v -> change { it.copy(cinematicZoom = v) } }
            }
            FramingGroup(s, change, showAspect = false, showHistogram = false, showTimer = false)
        }

        CaptureMode.AUDIO -> {
            Group("Nagrywanie") {
                Chips(Icons.Rounded.GraphicEq, "Jakość", AudioPreset.entries, s.audioPreset, { it.label }, enabled, hint = s.audioPreset.description) { v ->
                    change { it.copy(audioPreset = v) }
                }
                Chips(Icons.Rounded.Mic, "Źródło", AudioSourceType.entries, s.audioSource, { it.label }, enabled, hint = s.audioSource.description) { v ->
                    change { it.copy(audioSource = v) }
                }
            }
        }

        else -> {
            FramingGroup(s, change, showAspect = true, showHistogram = true, showTimer = true, enabled = enabled)
            when (mode) {
                CaptureMode.NIGHT -> Group("Tryb nocny") {
                    if (caps.nightExtension) {
                        InfoRow(Icons.Rounded.NightsStay, "Aktywny sprzętowy tryb nocny producenta – liczbę klatek dobiera telefon")
                    } else {
                        Chips(Icons.Rounded.Layers, "Liczba klatek", listOf(0, 4, 6, 8, 12), s.nightFrames, { if (it == 0) "Auto" else "×$it" }, enabled, hint = "Auto dobiera 4–12 klatek do jasności sceny") { v ->
                            change { it.copy(nightFrames = v) }
                        }
                    }
                    if (caps.lowLightBoost && !caps.nightExtension) {
                        SwitchRow(Icons.Rounded.Brightness4, "Jaśniejszy podgląd", "Wzmocnienie przy słabym świetle", s.lowLightBoost, enabled) { v -> change { it.copy(lowLightBoost = v) } }
                    }
                }
                CaptureMode.PORTRAIT -> Group("Portret") {
                    val effects = PortraitEffect.entries.filter {
                        (it == PortraitEffect.BOKEH && caps.bokehExtension) || (it == PortraitEffect.RETOUCH && caps.retouchExtension)
                    }
                    if (effects.size > 1) {
                        Chips(Icons.Rounded.Face, "Efekt", effects, s.portraitEffect, { it.label }, enabled) { v -> change { it.copy(portraitEffect = v) } }
                    } else if (effects.isNotEmpty()) {
                        InfoRow(Icons.Rounded.Face, "Sprzętowy efekt producenta: ${effects.first().label}")
                    } else {
                        InfoRow(Icons.Rounded.Face, "Programowy bokeh – wykrywanie osoby i rozmycie tła (tylny i przedni aparat)")
                    }
                    if (!caps.bokehExtension) {
                        Slider(Icons.Rounded.Face, "Siła rozmycia tła", s.portraitBlur, 0..100, true) { v -> change { it.copy(portraitBlur = v) } }
                    }
                }
                CaptureMode.HDR -> Group("HDR") {
                    if (caps.hdrExtension) {
                        InfoRow(Icons.Rounded.HdrOn, "Aktywny sprzętowy HDR producenta")
                    } else {
                        Chips(Icons.Rounded.Tune, "Rozpiętość ekspozycji", listOf(1f, 1.7f, 2.3f), s.hdrStops, { "±" + String.format(Locale.US, "%.1f", it).replace('.', ',') + " EV" }, enabled) { v ->
                            change { it.copy(hdrStops = v) }
                        }
                    }
                }
                else -> Unit
            }
            Group("Zdjęcie") {
                if (mode == CaptureMode.PHOTO || mode == CaptureMode.PRO) {
                    val formats = PhotoFormat.entries.filter { it in caps.photoFormats }
                    if (formats.size > 1) {
                        Chips(Icons.Rounded.Image, "Format", formats, s.photoFormat, { it.label }, enabled, hint = "RAW zapisuje plik DNG prosto z sensora") { v ->
                            change { it.copy(photoFormat = v) }
                        }
                    } else {
                        InfoRow(Icons.Rounded.Image, "RAW i Ultra HDR nie są dostępne dla tego obiektywu")
                    }
                }
                Slider(Icons.Rounded.HighQuality, "Jakość JPEG", s.jpegQuality, 70..100, enabled) { v -> change { it.copy(jpegQuality = v) } }
                if (mode != CaptureMode.NIGHT && mode != CaptureMode.HDR) {
                    val priorities = CapturePriority.entries
                    Chips(Icons.Rounded.Bolt, "Priorytet migawki", priorities, s.priority, { if (it == CapturePriority.SPEED && caps.zsl) "Szybkość (ZSL)" else it.label }, enabled) { v ->
                        change { it.copy(priority = v) }
                    }
                }
                if (caps.hasFront) {
                    SwitchRow(Icons.Rounded.Flip, "Lustrzane selfie", "Zapis jak w podglądzie przedniego aparatu", s.mirrorFront) { v -> change { it.copy(mirrorFront = v) } }
                }
            }
            if (mode == CaptureMode.PHOTO || mode == CaptureMode.PRO) {
                Group("Seria i interwał") {
                    if (mode == CaptureMode.PHOTO) {
                        SwitchRow(Icons.Rounded.BurstMode, "Seria po przytrzymaniu", "Zdjęcia seryjne, dopóki trzymasz migawkę", s.burst) { v -> change { it.copy(burst = v) } }
                    }
                    Chips(Icons.Rounded.AvTimer, "Interwałometr", listOf(0, 2, 5, 10, 30, 60), s.intervalSeconds, { if (it == 0) "Wył." else "${it}s" }) { v ->
                        change { it.copy(intervalSeconds = v) }
                    }
                    if (s.intervalSeconds > 0) {
                        Chips(Icons.Rounded.Layers, "Liczba zdjęć", listOf(0, 10, 30, 100, 300), s.intervalCount, { if (it == 0) "∞" else "$it" }) { v ->
                            change { it.copy(intervalCount = v) }
                        }
                    }
                }
            }
        }
    }
    if (!isPhoto && mode != CaptureMode.AUDIO && mode != CaptureMode.VIDEO && mode != CaptureMode.SLOWMO) Unit
}

@Composable
private fun FramingGroup(
    s: ModeSettings,
    change: ((ModeSettings) -> ModeSettings) -> Unit,
    showAspect: Boolean,
    showHistogram: Boolean,
    showTimer: Boolean,
    enabled: Boolean = true,
) {
    Group("Kadr") {
        if (showAspect) Chips(Icons.Rounded.AspectRatio, "Proporcje", AspectRatioMode.entries, s.aspect, { it.label }, enabled) { v -> change { it.copy(aspect = v) } }
        MultiChips(Icons.Rounded.GridOn, "Siatki (dowolne naraz)", GridType.entries, s.grids, { it.label }) { v ->
            change { it.copy(grids = if (v in it.grids) it.grids - v else it.grids + v) }
        }
        if (showTimer) Chips(Icons.Rounded.Timer, "Samowyzwalacz", listOf(0, 3, 5, 10), s.timerSeconds, { if (it == 0) "Wył." else "$it s" }) { v -> change { it.copy(timerSeconds = v) } }
        SwitchRow(Icons.Rounded.Straighten, "Poziomica", null, s.level) { v -> change { it.copy(level = v) } }
        if (showHistogram) SwitchRow(Icons.Rounded.BarChart, "Histogram na żywo", null, s.histogram, enabled) { v -> change { it.copy(histogram = v) } }
    }
}

@Composable
private fun GeneralTab(
    g: GlobalSettings,
    caps: CameraCaps,
    update: UpdateState,
    onGlobal: ((GlobalSettings) -> GlobalSettings) -> Unit,
    onCheck: () -> Unit,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
) {
    Group("Aktualizacje") {
        UpdateRow(update, onCheck, onDownload, onInstall)
        SwitchRow(Icons.Rounded.Autorenew, "Sprawdzaj automatycznie", "Przy uruchomieniu, co najwyżej co 3 godziny", g.autoCheckUpdates) { v ->
            onGlobal { it.copy(autoCheckUpdates = v) }
        }
        SwitchRow(Icons.Rounded.Download, "Pobieraj i instaluj automatycznie", "Instalacja po wyjściu z aplikacji", g.autoDownloadUpdates) { v ->
            onGlobal { it.copy(autoDownloadUpdates = v) }
        }
    }
    Group("Obsługa") {
        SwitchRow(Icons.Rounded.Vibration, "Wibracje", "Haptyka pokręteł, zoomu i poziomicy", g.haptics) { v -> onGlobal { it.copy(haptics = v) } }
        SwitchRow(Icons.Rounded.CameraAlt, "Dźwięk migawki", null, g.shutterSound) { v -> onGlobal { it.copy(shutterSound = v) } }
        SwitchRow(Icons.Rounded.Alarm, "Sygnał samowyzwalacza", null, g.timerBeep) { v -> onGlobal { it.copy(timerBeep = v) } }
        SwitchRow(Icons.Rounded.Speed, "Parametry ekspozycji", "Pasek ISO · czas · przysłona · EV u góry kadru", g.exposureInfo) { v ->
            onGlobal { it.copy(exposureInfo = v) }
        }
        Chips(
            Icons.Rounded.ScreenRotation,
            "Obrót ekranu",
            ScreenRotation.entries,
            g.screenRotation,
            { it.label },
            hint = g.screenRotation.description,
        ) { v -> onGlobal { it.copy(screenRotation = v) } }
        Chips(Icons.Rounded.VolumeUp, "Klawisze głośności", VolumeAction.entries, g.volumeAction, { it.label }) { v -> onGlobal { it.copy(volumeAction = v) } }
    }
    Group("Ten aparat") {
        val lenses = (caps.lensZooms + caps.auxLenses.map { it.factor }).distinct().sorted()
            .joinToString(" · ") { formatZoom(it) + "×" }.ifEmpty { "1×" }
        InfoRow(Icons.Rounded.ZoomIn, "Obiektywy: $lenses")
        if (caps.cameraSummary.isNotEmpty()) InfoRow(Icons.Rounded.CameraAlt, "Wykryte aparaty:\n" + caps.cameraSummary)
        InfoRow(Icons.Rounded.Image, "Formaty: " + caps.photoFormats.sortedBy { it.ordinal }.joinToString { it.label })
        InfoRow(Icons.Rounded.Movie, "Wideo: " + caps.videoResolutions.joinToString { it.label } + (if (caps.fps60) " · 60 kl/s" else "") + (if (caps.videoHlg) " · HLG" else ""))
        if (caps.slowMo) InfoRow(Icons.Rounded.SlowMotionVideo, "Slow-mo: " + caps.slowMoRanges.joinToString { "${it.upper} kl/s" })
        InfoRow(Icons.Rounded.Tune, if (caps.manualSensor) "Pełne sterowanie ręczne sensora" else "Ograniczone sterowanie ręczne")
        InfoRow(Icons.Rounded.Info, "Pliki: DCIM/Registrer · Recordings/Registrer")
    }
}

@Composable
private fun UpdateRow(update: UpdateState, onCheck: () -> Unit, onDownload: () -> Unit, onInstall: () -> Unit) {
    val (status, action, handler) = when (update) {
        UpdateState.Idle -> Triple("Wersja ${UpdateManager.currentVersion}", "Sprawdź", onCheck)
        UpdateState.Checking -> Triple("Sprawdzanie…", null, onCheck)
        UpdateState.UpToDate -> Triple("Wersja ${UpdateManager.currentVersion} jest najnowsza", "Sprawdź", onCheck)
        is UpdateState.Available -> Triple("Dostępna wersja ${update.info.version}", "Pobierz", onDownload)
        is UpdateState.Downloading -> Triple("Pobieranie ${update.info.version} · ${(update.progress * 100).roundToInt()}%", null, onCheck)
        is UpdateState.Ready -> Triple("Wersja ${update.info.version} gotowa", "Zainstaluj", onInstall)
        is UpdateState.Installing -> Triple("Instalowanie ${update.info.version}…", null, onCheck)
        is UpdateState.Failed -> Triple(update.message, "Ponów", onCheck)
    }
    Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RowIcon(Icons.Rounded.SystemUpdate)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text("Registrer ${UpdateManager.currentVersion}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(status, fontSize = 12.sp, color = if (update is UpdateState.Available || update is UpdateState.Ready) Palette.Amber else Palette.TextDim, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            if (action != null) {
                Text(
                    action,
                    color = Palette.Ink,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Palette.Amber)
                        .clickable(onClick = handler)
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                )
            }
        }
        if (update is UpdateState.Downloading) {
            LinearProgressIndicator(
                progress = { update.progress },
                color = Palette.Amber,
                trackColor = Color.White.copy(alpha = 0.1f),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
        }
        val notes = (update as? UpdateState.Available)?.info?.notes ?: (update as? UpdateState.Ready)?.info?.notes
        if (!notes.isNullOrBlank()) {
            Text(notes, fontSize = 12.sp, color = Palette.TextDim, maxLines = 4, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

// ------------------------------------------------------------------ cegiełki

@Composable
private fun TabChip(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .reactive(label, onClick = onClick)
            .glass(BlockShape, active = selected)
            .animateContentSize(spring(dampingRatio = 0.8f, stiffness = 500f))
            .padding(horizontal = 11.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = if (selected) Palette.Ink else Color.White, modifier = Modifier.size(18.dp))
        if (selected) {
            Spacer(Modifier.width(6.dp))
            Text(label, color = Palette.Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun Group(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            title.uppercase(),
            color = Palette.Amber,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.4.sp,
            modifier = Modifier.padding(start = 6.dp, bottom = 6.dp),
        )
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(Color.White.copy(alpha = 0.06f))
                .padding(vertical = 4.dp),
            content = content,
        )
    }
}

@Composable
private fun RowIcon(icon: ImageVector) {
    Box(
        Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(Palette.Amber.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = Palette.Amber, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun SwitchRow(icon: ImageVector, title: String, subtitle: String?, value: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onChange(!value) }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowIcon(icon)
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(title, fontSize = 14.sp, color = if (enabled) Color.White else Palette.TextFaint)
            if (subtitle != null) Text(subtitle, fontSize = 11.sp, color = Palette.TextDim, lineHeight = 13.sp)
        }
        Switch(
            checked = value,
            onCheckedChange = onChange,
            enabled = enabled,
            modifier = Modifier.scale(0.82f),
            colors = SwitchDefaults.colors(checkedTrackColor = Palette.Amber, checkedThumbColor = Palette.Ink),
        )
    }
}

/** Chipy wielokrotnego wyboru – każde dotknięcie włącza / wyłącza jedną opcję. */
@Composable
private fun <T> MultiChips(
    icon: ImageVector,
    title: String,
    options: List<T>,
    selected: Set<T>,
    label: (T) -> String,
    onToggle: (T) -> Unit,
) {
    Column(Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RowIcon(icon)
            Text(title, fontSize = 14.sp, color = Color.White, modifier = Modifier.padding(horizontal = 12.dp))
        }
        Row(
            Modifier
                .padding(start = 44.dp, top = 6.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            options.forEach { o ->
                val sel = o in selected
                Row(
                    Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (sel) Palette.Amber else Color.White.copy(alpha = 0.07f))
                        .clickable { onToggle(o) }
                        .padding(start = if (sel) 8.dp else 12.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (sel) {
                        Icon(Icons.Rounded.Check, null, tint = Palette.Ink, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        label(o),
                        color = if (sel) Palette.Ink else Color.White,
                        fontSize = 12.sp,
                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
private fun <T> Chips(
    icon: ImageVector,
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    enabled: Boolean = true,
    hint: String? = null,
    onSelect: (T) -> Unit,
) {
    Column(Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RowIcon(icon)
            Text(title, fontSize = 14.sp, color = if (enabled) Color.White else Palette.TextFaint, modifier = Modifier.padding(horizontal = 12.dp))
        }
        Row(
            Modifier
                .padding(start = 44.dp, top = 6.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            options.forEach { o ->
                val sel = o == selected
                Text(
                    label(o),
                    color = if (sel) Palette.Ink else if (enabled) Color.White else Palette.TextFaint,
                    fontSize = 12.sp,
                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (sel) Palette.Amber else Color.White.copy(alpha = 0.07f))
                        .clickable(enabled = enabled) { onSelect(o) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
        if (hint != null) Text(hint, fontSize = 11.sp, color = Palette.TextDim, modifier = Modifier.padding(start = 44.dp, top = 4.dp))
    }
}

@Composable
private fun Slider(icon: ImageVector, title: String, value: Int, range: IntRange, enabled: Boolean, onCommit: (Int) -> Unit) {
    var local by remember(value) { mutableFloatStateOf(value.toFloat()) }
    Column(Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RowIcon(icon)
            Text(title, fontSize = 14.sp, modifier = Modifier.weight(1f).padding(horizontal = 12.dp), color = if (enabled) Color.White else Palette.TextFaint)
            Text("${local.roundToInt()}%", color = Palette.Amber, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        Slider(
            value = local,
            onValueChange = { local = it },
            onValueChangeFinished = { onCommit(local.roundToInt()) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            enabled = enabled,
            modifier = Modifier.padding(start = 44.dp).height(30.dp),
            colors = SliderDefaults.colors(thumbColor = Palette.Amber, activeTrackColor = Palette.Amber),
        )
    }
}

@Composable
private fun InfoRow(icon: ImageVector, text: String) {
    Row(Modifier.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        RowIcon(icon)
        Text(text, fontSize = 13.sp, color = Palette.TextDim, modifier = Modifier.padding(horizontal = 12.dp))
    }
}
