package com.blacksingularities.registrer.ui.components

import com.blacksingularities.registrer.ui.theme.BlockShape
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.material.icons.rounded.Explore
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
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.NoiseAware
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Headset
import androidx.compose.material.icons.rounded.MotionPhotosOn
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.ui.input.pointer.pointerInput
import com.blacksingularities.registrer.ui.theme.GlassIconButton
import com.blacksingularities.registrer.ui.theme.GlassLevel
import com.blacksingularities.registrer.ui.theme.glass
import com.blacksingularities.registrer.ui.theme.reactive
import com.blacksingularities.registrer.ui.theme.rotatedLayout
import androidx.compose.material.icons.rounded.Link
import com.blacksingularities.registrer.data.SharedGroup
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.rounded.ScreenLockRotation
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blacksingularities.registrer.camera.CameraCaps
import com.blacksingularities.registrer.camera.CaptureMode
import com.blacksingularities.registrer.data.AspectRatioMode
import com.blacksingularities.registrer.audio.AudioEngine
import com.blacksingularities.registrer.data.AudioInput
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
    CaptureMode.MOTION -> Icons.Rounded.MotionPhotosOn
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
    landscape: Boolean,
    rotation: Float,
    onProfile: (CaptureMode, (ModeSettings) -> ModeSettings) -> Unit,
    onGlobal: ((GlobalSettings) -> GlobalSettings) -> Unit,
    onReset: (CaptureMode) -> Unit,
    onCheckUpdate: () -> Unit,
    onDownloadUpdate: () -> Unit,
    onInstallUpdate: () -> Unit,
    onDismiss: () -> Unit,
) {
    var tab by remember { mutableStateOf<CaptureMode?>(initialMode) }

    @Composable
    fun Tabs() {
        val options = listOf<CaptureMode?>(null) + modes
        val selectedIndex = options.indexOf(tab).coerceAtLeast(0)
        val state = rememberLazyListState()
        LaunchedEffect(selectedIndex) {
            state.animateScrollToItem(selectedIndex)
        }
        LazyRow(
            Modifier.fillMaxWidth(),
            state = state,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(options, key = { it?.name ?: "GENERAL" }) { mode ->
                TabChip(
                    if (mode == null) Icons.Rounded.Settings else modeIcon(mode),
                    mode?.title ?: "Ogólne",
                    tab == mode,
                ) { tab = mode }
            }
        }
    }

    @Composable
    fun Body(modifier: Modifier) {
        AnimatedContent(
            modifier = modifier,
            targetState = tab,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "settingsTab",
        ) { current ->
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (locked) Text("Część opcji jest zablokowana podczas nagrywania lub przetwarzania.", color = Palette.Amber, fontSize = 12.sp)
                if (current == null) {
                    GeneralTab(global, caps, update, onGlobal, onCheckUpdate, onDownloadUpdate, onInstallUpdate)
                } else {
                    ModeTab(current, profiles.getValue(current), caps, !locked) { t -> onProfile(current, t) }
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).clickable { onReset(current) }.padding(12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.RestartAlt, null, tint = Palette.TextDim, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Przywróć domyślne: ${current.title}", color = Palette.TextDim, fontSize = 13.sp)
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }

    // Odstępy od pasków systemowych liczone dla obróconego układu: po obrocie o 90° pasek stanu
    // leży przy bocznej krawędzi treści, a nie przy jej górze.
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    val safe = WindowInsets.safeDrawing
    val physical = with(density) {
        listOf(
            safe.getTop(density).toDp(),
            safe.getRight(density, direction).toDp(),
            safe.getBottom(density).toDp(),
            safe.getLeft(density, direction).toDp(),
        )
    }
    // Krawędzie treści (góra, prawo, dół, lewo) po obrocie o kolejne ćwiartki zgodnie z ruchem wskazówek.
    val quarter = (((rotation / 90f).roundToInt() % 4) + 4) % 4
    fun edge(contentSide: Int) = physical[(contentSide + quarter) % 4]
    val insets = PaddingValues(top = edge(0), end = edge(1), bottom = edge(2), start = edge(3))

    Box(
        Modifier.fillMaxSize().rotatedLayout(rotation).background(GlassLevel.Thick.tint).pointerInput(Unit) { detectTapGestures { } },
    ) {
        Column(Modifier.fillMaxSize().padding(insets)) {
            Row(Modifier.padding(start = 20.dp, end = 12.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Ustawienia", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                GlassIconButton(Icons.Rounded.Close, "Zamknij", 0f, size = 38.dp, onClick = onDismiss)
            }
            Spacer(Modifier.height(if (landscape) 6.dp else 12.dp))
            Tabs()
            Spacer(Modifier.height(if (landscape) 6.dp else 10.dp))
            Body(Modifier.weight(1f))
        }
    }
}

@Composable
private fun ModeTab(mode: CaptureMode, s: ModeSettings, caps: CameraCaps, enabled: Boolean, change: ((ModeSettings) -> ModeSettings) -> Unit) {
    val isPhoto = mode.isPhoto
    @Composable
    fun shared(group: SharedGroup) = SharedSwitch(group, s, change)
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
            Group("Dźwięk i zbliżanie") {
                shared(SharedGroup.VIDEO)
                SwitchRow(Icons.Rounded.Mic, "Nagrywaj dźwięk", null, s.videoAudio, enabled) { v -> change { it.copy(videoAudio = v) } }
                SwitchRow(Icons.Rounded.ZoomIn, "Płynne zbliżanie", "Wolny, równy najazd podczas nagrywania", s.cinematicZoom) { v -> change { it.copy(cinematicZoom = v) } }
            }
            FramingGroup(s, change, showAspect = false, showHistogram = false, showTimer = false)
        }

        CaptureMode.SLOWMO -> {
            Group("Zwolnione tempo") {
                if (caps.slowMoProfiles.isEmpty()) {
                    InfoRow(Icons.Rounded.Info, "Ten obiektyw nie obsługuje nagrywania szybkoklatkowego")
                } else {
                    InfoRow(Icons.Rounded.CameraAlt, "Tylko tylny aparat główny · 1×")
                    val resolutions = caps.slowMoProfiles.map { it.resolution }.distinct().sortedBy { it.ordinal }
                    val resolution = s.slowMoResolution.takeIf { it in resolutions } ?: resolutions.first()
                    Chips(Icons.Rounded.HighQuality, "Rozdzielczość", resolutions, resolution, { it.label }, enabled) { selectedResolution ->
                        val fps = caps.slowMoProfiles.filter { it.resolution == selectedResolution }.map { it.fps }
                        change { current -> current.copy(slowMoResolution = selectedResolution, slowMoFps = current.slowMoFps.takeIf { it in fps } ?: fps.min()) }
                    }
                    val options = caps.slowMoProfiles.filter { it.resolution == resolution }.map { it.fps }.distinct().sorted()
                    val selected = s.slowMoFps.takeIf { it in options } ?: options.first()
                    Chips(Icons.Rounded.SlowMotionVideo, "Prędkość nagrywania", options, selected, { "$it kl/s · 1/${it / 30}×" }, enabled) { selectedFps ->
                        change { it.copy(slowMoFps = selectedFps) }
                    }
                }
                SwitchRow(Icons.Rounded.ZoomIn, "Płynne zbliżanie", null, s.cinematicZoom) { v -> change { it.copy(cinematicZoom = v) } }
            }
            FramingGroup(s, change, showAspect = false, showHistogram = false, showTimer = false)
        }

        CaptureMode.AUDIO -> {
            Group("Jakość") {
                Chips(Icons.Rounded.GraphicEq, "Format", AudioPreset.entries, s.audioPreset, { it.label }, enabled, hint = s.audioPreset.description) { v ->
                    change { it.copy(audioPreset = v) }
                }
            }
            Group("Mikrofon") {
                Chips(Icons.Rounded.Headset, "Wejście", AudioInput.entries, s.audioInput, { it.label }, enabled, hint = s.audioInput.description) { v ->
                    change { it.copy(audioInput = v) }
                }
                Chips(Icons.Rounded.Mic, "Przetwarzanie", AudioSourceType.entries, s.audioSource, { it.label }, enabled, hint = s.audioSource.description) { v ->
                    change { it.copy(audioSource = v) }
                }
                Chips(Icons.Rounded.Equalizer, "Wzmocnienie", listOf(0, 6, 12, 18, 24), s.audioGainDb, { if (it == 0) "0 dB" else "+$it dB" }, enabled, hint = "Cyfrowe, z miękkim ogranicznikiem – dla cichych źródeł i wykładów") { v ->
                    change { it.copy(audioGainDb = v) }
                }
                if (AudioEngine.noiseSuppressionAvailable) {
                    SwitchRow(Icons.Rounded.NoiseAware, "Redukcja szumów", "Filtr systemowy: czystsza mowa, gorzej dla muzyki", s.audioNoiseSuppression, enabled) { v ->
                        change { it.copy(audioNoiseSuppression = v) }
                    }
                }
                if (AudioEngine.agcAvailable) {
                    SwitchRow(Icons.Rounded.GraphicEq, "Samoczynna głośność", "Wyrównuje cichych i głośnych mówców", s.audioAgc, enabled) { v ->
                        change { it.copy(audioAgc = v) }
                    }
                }
            }
            Group("Nagrywanie") {
                SwitchRow(Icons.Rounded.FastForward, "Pomijaj ciszę", "Przerwy dłuższe niż 1,5 s nie trafiają do pliku", s.audioSkipSilence, enabled) { v ->
                    change { it.copy(audioSkipSilence = v) }
                }
                Chips(Icons.Rounded.Timer, "Automatyczne zatrzymanie", listOf(0, 15, 30, 60, 120), s.audioAutoStopMin, { if (it == 0) "Wył." else autoStopLabel(it) }, enabled) { v ->
                    change { it.copy(audioAutoStopMin = v) }
                }
                InfoRow(Icons.Rounded.Info, "Nagrywanie trwa w tle i przy zgaszonym ekranie – pauza, znacznik i stop są w powiadomieniu")
            }
        }

        else -> {
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
                        InfoRow(Icons.Rounded.Face, "Programowe rozmycie tła – wykrywanie osoby (tylny i przedni aparat)")
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
            Group("Zdjęcie", collapsible = true, initiallyExpanded = false) {
                shared(SharedGroup.PHOTO)
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
                    Chips(Icons.Rounded.Bolt, "Priorytet migawki", priorities, s.priority, { if (it == CapturePriority.SPEED && caps.zsl) "Szybkość (bez opóźnienia)" else it.label }, enabled) { v ->
                        change { it.copy(priority = v) }
                    }
                }
                if (caps.hasFront) {
                    SwitchRow(Icons.Rounded.Flip, "Lustrzane odbicie autoportretu", "Zapis jak w podglądzie przedniego aparatu", s.mirrorFront) { v -> change { it.copy(mirrorFront = v) } }
                }
            }
            FramingGroup(s, change, showAspect = true, showHistogram = true, showTimer = true, enabled = enabled)
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
    Group("Kadr", collapsible = true, initiallyExpanded = false) {
        SharedSwitch(SharedGroup.FRAMING, s, change)
        if (showAspect) Chips(Icons.Rounded.AspectRatio, "Proporcje", AspectRatioMode.entries, s.aspect, { it.label }, enabled) { v -> change { it.copy(aspect = v) } }
        MultiChips(Icons.Rounded.GridOn, "Siatki", GridType.entries, s.grids, { it.label }) { v ->
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
    Group(null) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            IconToggle(Icons.Rounded.Vibration, "Wibracje", g.haptics, Modifier.weight(1f)) { v -> onGlobal { it.copy(haptics = v) } }
            IconToggle(Icons.Rounded.CameraAlt, "Migawka", g.shutterSound, Modifier.weight(1f)) { v -> onGlobal { it.copy(shutterSound = v) } }
            IconToggle(Icons.Rounded.Alarm, "Odliczanie", g.timerBeep, Modifier.weight(1f)) { v -> onGlobal { it.copy(timerBeep = v) } }
            IconToggle(Icons.Rounded.Speed, "Parametry", g.exposureInfo, Modifier.weight(1f)) { v -> onGlobal { it.copy(exposureInfo = v) } }
            IconToggle(Icons.Rounded.Explore, "Kompas", g.compass, Modifier.weight(1f)) { v -> onGlobal { it.copy(compass = v) } }
        }
        Text(
            "Dźwięk migawki, sygnał samowyzwalacza i nakładki w kadrze. Nakładki przeciągasz po kadrze, a upuszczone na koszu znikają.",
            fontSize = 10.sp,
            lineHeight = 13.sp,
            color = Palette.TextDim,
            modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 6.dp),
        )
    }
    Group("Obsługa") {
        CompactChoice(
            Icons.Rounded.ScreenRotation,
            "Obrót ekranu",
            ScreenRotation.entries,
            g.screenRotation,
            label = { it.label },
            icon = { if (it == ScreenRotation.FIXED) Icons.Rounded.ScreenLockRotation else Icons.Rounded.ScreenRotation },
            hint = g.screenRotation.description,
        ) { v -> onGlobal { it.copy(screenRotation = v) } }
        CompactChoice(
            Icons.Rounded.VolumeUp,
            "Klawisze głośności",
            VolumeAction.entries,
            g.volumeAction,
            label = { it.label },
            icon = {
                when (it) {
                    VolumeAction.SHUTTER -> Icons.Rounded.CameraAlt
                    VolumeAction.ZOOM -> Icons.Rounded.ZoomIn
                    VolumeAction.NONE -> Icons.Rounded.VolumeUp
                }
            },
        ) { v -> onGlobal { it.copy(volumeAction = v) } }
        CompactChoice(
            Icons.Rounded.BarChart,
            "Histogram",
            com.blacksingularities.registrer.data.HistogramMode.entries,
            g.histogramMode,
            label = { it.label },
            hint = "Dotknięcie histogramu w kadrze też przełącza",
        ) { v -> onGlobal { it.copy(histogramMode = v) } }
        CompactChoice(
            Icons.Rounded.AspectRatio,
            "Wielkość histogramu",
            com.blacksingularities.registrer.data.HistogramSize.entries,
            g.histogramSize,
            label = { it.label },
            short = { it.label.take(1) },
        ) { v -> onGlobal { it.copy(histogramSize = v) } }
    }
    Group("Informacje o sprzęcie", collapsible = true, initiallyExpanded = false) {
        val lenses = (caps.lensZooms + caps.auxLenses.map { it.factor }).distinct().sorted()
            .joinToString(" · ") { formatZoom(it) + "×" }.ifEmpty { "1×" }
        InfoRow(Icons.Rounded.ZoomIn, "Obiektywy: $lenses")
        if (caps.cameraSummary.isNotEmpty()) ExpandableInfoRow(Icons.Rounded.CameraAlt, "Wykryte aparaty", caps.cameraSummary)
        InfoRow(Icons.Rounded.Image, "Formaty: " + caps.photoFormats.sortedBy { it.ordinal }.joinToString { it.label })
        InfoRow(Icons.Rounded.Movie, "Wideo: " + caps.videoResolutions.joinToString { it.label } + (if (caps.fps60) " · 60 kl/s" else "") + (if (caps.videoHlg) " · HLG" else ""))
        if (caps.slowMo) InfoRow(Icons.Rounded.SlowMotionVideo, "Zwolnione tempo: " + caps.slowMoProfiles.joinToString { "${it.resolution.label} ${it.fps} kl/s" })
        InfoRow(Icons.Rounded.Tune, if (caps.manualSensor) "Pełne sterowanie ręczne sensora" else "Ograniczone sterowanie ręczne")
        InfoRow(Icons.Rounded.Info, "Pliki: DCIM/OpenShot · Recordings/OpenShot")
    }
    Group("Aktualizacje") {
        UpdateRow(update, onCheck, onDownload, onInstall)
        Row(
            Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            PillToggle(Icons.Rounded.Autorenew, "Sprawdzaj sam", g.autoCheckUpdates, Modifier.weight(1f)) { v ->
                onGlobal { it.copy(autoCheckUpdates = v) }
            }
            PillToggle(Icons.Rounded.Download, "Instaluj sam", g.autoDownloadUpdates, Modifier.weight(1f)) { v ->
                onGlobal { it.copy(autoDownloadUpdates = v) }
            }
        }
        Credits()
        Spacer(Modifier.height(6.dp))
    }
}

/** Kafelek-przełącznik: sama ikona z krótkim podpisem; włączony – biały. */
@Composable
private fun IconToggle(icon: ImageVector, label: String, value: Boolean, modifier: Modifier = Modifier, onChange: (Boolean) -> Unit) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (value) Color.White else Color.White.copy(alpha = 0.06f))
            .toggleable(value = value, role = Role.Switch, onValueChange = onChange)
            .padding(vertical = 8.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, null, tint = if (value) Palette.Ink else Palette.TextDim, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(3.dp))
        Text(
            label,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = if (value) Palette.Ink else Palette.TextDim,
            fontWeight = if (value) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

/** Przełącznik w formie pigułki: ikona i etykieta w jednej linii. */
@Composable
private fun PillToggle(icon: ImageVector, label: String, value: Boolean, modifier: Modifier = Modifier, onChange: (Boolean) -> Unit) {
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(if (value) Color.White else Color.White.copy(alpha = 0.06f))
            .toggleable(value = value, role = Role.Switch, onValueChange = onChange)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = if (value) Palette.Ink else Palette.TextDim, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 12.sp, maxLines = 1, color = if (value) Palette.Ink else Palette.TextDim)
    }
}

/**
 * Wybór w jednej linii: ikona, nazwa z bieżącą wartością pod spodem i przełącznik segmentowy
 * po prawej – ikonami ([icon]) albo krótkim tekstem ([short]).
 */
@Composable
private fun <T> CompactChoice(
    rowIcon: ImageVector,
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    icon: ((T) -> ImageVector)? = null,
    short: (T) -> String = label,
    hint: String? = null,
    onSelect: (T) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(rowIcon, null, tint = Palette.TextDim, modifier = Modifier.size(18.dp))
        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
            Text(title, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                hint?.let { "${label(selected)} · $it" } ?: label(selected),
                fontSize = 10.sp,
                lineHeight = 12.sp,
                color = Palette.TextDim,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(
            Modifier
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.06f))
                .padding(2.dp),
        ) {
            options.forEach { o ->
                val sel = o == selected
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (sel) Color.White else Color.Transparent)
                        .selectable(selected = sel, role = Role.RadioButton, onClick = { onSelect(o) })
                        .padding(horizontal = if (icon != null) 10.dp else 9.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (icon != null) {
                        Icon(icon(o), label(o), tint = if (sel) Palette.Ink else Palette.TextDim, modifier = Modifier.size(16.dp))
                    } else {
                        Text(
                            short(o),
                            fontSize = 11.sp,
                            fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium,
                            color = if (sel) Palette.Ink else Palette.TextDim,
                        )
                    }
                }
            }
        }
    }
}

/** Podpis w sekcji Aktualizacje: wersja i autor (adres otwiera się po dotknięciu). */
@Composable
private fun Credits() {
    val uriHandler = LocalUriHandler.current
    Column(
        Modifier.fillMaxWidth().padding(top = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("OpenShot ${UpdateManager.currentVersion}", fontSize = 11.sp, color = Palette.TextDim)
        Text(
            buildAnnotatedString {
                append("Pomysł i wykonanie: ")
                withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.SemiBold, textDecoration = TextDecoration.Underline)) {
                    append("redzikowski.com")
                }
            },
            fontSize = 12.sp,
            color = Palette.TextDim,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { runCatching { uriHandler.openUri("https://redzikowski.com") } }
                .padding(horizontal = 8.dp, vertical = 4.dp),
        )
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
                Text("OpenShot ${UpdateManager.currentVersion}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(status, fontSize = 12.sp, color = if (update is UpdateState.Available || update is UpdateState.Ready) Palette.Amber else Palette.TextDim, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            if (action != null) {
                Text(
                    action,
                    color = Palette.Ink,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
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
            Text(
                notes,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                textAlign = TextAlign.Justify,
                color = Palette.TextDim,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, start = 44.dp, end = 4.dp),
            )
        }
    }
}

// ------------------------------------------------------------------ cegiełki

@Composable
private fun TabChip(icon: ImageVector, label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier
            .reactive(label, onClick = onClick)
            .glass(BlockShape, active = selected)
            .animateContentSize(spring(dampingRatio = 0.8f, stiffness = 500f))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = if (selected) Palette.Ink else Color.White, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(5.dp))
        Text(label, color = if (selected) Palette.Ink else Color.White, fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
    }
}

@Composable
private fun Group(
    /** null = sekcja bez nagłówka. */
    title: String?,
    collapsible: Boolean = false,
    initiallyExpanded: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    var expanded by remember(title) { mutableStateOf(initiallyExpanded) }
    Column {
        if (title != null) Row(
            Modifier
                .fillMaxWidth()
                .then(if (collapsible) Modifier.clickable { expanded = !expanded } else Modifier)
                .padding(start = 4.dp, end = 4.dp, bottom = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                title.uppercase(),
                color = Palette.Amber,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp,
                modifier = Modifier.weight(1f),
            )
            if (collapsible) {
                Icon(
                    Icons.Rounded.ExpandMore,
                    if (expanded) "Zwiń sekcję $title" else "Rozwiń sekcję $title",
                    tint = Palette.Amber,
                    modifier = Modifier.size(18.dp).rotate(if (expanded) 0f else -90f),
                )
            }
        }
        AnimatedVisibility(visible = !collapsible || expanded) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .padding(vertical = 2.dp),
                content = content,
            )
        }
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
            .padding(horizontal = 10.dp, vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowIcon(icon)
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(title, fontSize = 13.sp, color = if (enabled) Color.White else Palette.TextFaint)
            if (subtitle != null) Text(subtitle, fontSize = 10.sp, color = Palette.TextDim, lineHeight = 12.sp)
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

/**
 * „Wspólne dla wszystkich trybów”: włączone – zmiany tej grupy obowiązują wszędzie;
 * wyłączone – tryb zaczyna od bieżących wartości wspólnych i ustawia je tylko dla siebie.
 */
@Composable
private fun SharedSwitch(group: SharedGroup, s: ModeSettings, change: ((ModeSettings) -> ModeSettings) -> Unit) {
    val isShared = group !in s.custom
    SwitchRow(
        Icons.Rounded.Link,
        "Wspólne dla wszystkich trybów",
        if (isShared) "${group.label}: zmiany obowiązują we wszystkich trybach" else "${group.label}: ustawienia tylko dla tego trybu",
        isShared,
    ) { v -> change { it.copy(custom = if (v) it.custom - group else it.custom + group) } }
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
    Column(Modifier.padding(horizontal = 10.dp, vertical = 3.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RowIcon(icon)
            Text(title, fontSize = 14.sp, color = Color.White, modifier = Modifier.padding(horizontal = 12.dp))
        }
        Row(
            Modifier
                .padding(start = 40.dp, top = 3.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            options.forEach { o ->
                val sel = o in selected
                Row(
                    Modifier
                        .clip(RoundedCornerShape(50))
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
    Column(Modifier.padding(horizontal = 10.dp, vertical = 3.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RowIcon(icon)
            Text(title, fontSize = 14.sp, color = if (enabled) Color.White else Palette.TextFaint, modifier = Modifier.padding(horizontal = 12.dp))
        }
        Row(
            Modifier
                .padding(start = 40.dp, top = 3.dp)
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
                        .clip(RoundedCornerShape(50))
                        .background(if (sel) Palette.Amber else Color.White.copy(alpha = 0.07f))
                        .clickable(enabled = enabled) { onSelect(o) }
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }
        if (hint != null) Text(hint, fontSize = 11.sp, color = Palette.TextDim, modifier = Modifier.padding(start = 40.dp, top = 2.dp))
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
            modifier = Modifier.padding(start = 40.dp).height(26.dp),
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

@Composable
private fun ExpandableInfoRow(icon: ImageVector, title: String, body: String) {
    var expanded by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RowIcon(icon)
            Text(title, fontSize = 13.sp, modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
            Icon(
                Icons.Rounded.ExpandMore,
                if (expanded) "Zwiń" else "Rozwiń",
                tint = Palette.TextDim,
                modifier = Modifier.size(20.dp).rotate(if (expanded) 180f else 0f),
            )
        }
        AnimatedVisibility(expanded) {
            Text(
                body,
                color = Palette.TextDim,
                fontSize = 11.sp,
                modifier = Modifier.fillMaxWidth().padding(start = 56.dp, end = 12.dp, bottom = 12.dp),
            )
        }
    }
}
