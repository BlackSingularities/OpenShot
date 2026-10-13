package com.blacksingularities.registrer.ui.components

import com.blacksingularities.registrer.i18n.tr
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.CropFree
import androidx.compose.material.icons.rounded.PresentToAll
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Exposure
import androidx.compose.material.icons.rounded.Timelapse
import androidx.compose.material.icons.rounded.Gradient
import androidx.compose.material.icons.rounded.CenterFocusWeak
import androidx.compose.material.icons.rounded.Opacity
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.LinearScale
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.HorizontalRule
import com.blacksingularities.registrer.data.LevelStyle
import androidx.compose.material.icons.rounded.ShutterSpeed
import androidx.compose.material.icons.rounded.LocationOn
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
import com.blacksingularities.registrer.ui.theme.hairline
import com.blacksingularities.registrer.ui.theme.amoledFill
import com.blacksingularities.registrer.update.UpdateManager
import com.blacksingularities.registrer.update.UpdateState
import java.util.Locale
import kotlin.math.roundToInt

val LONG_SECONDS = listOf(1, 2, 4, 8, 15)

/** „Zdjęcie co X s · 1 min nagrania = Y s filmu”. */
fun timelapseHint(speed: Int, fps: Int): String {
    val interval = speed.toFloat() / fps
    val perMinute = 60f / speed
    val pl = com.blacksingularities.registrer.i18n.Lang.locale
    return tr("Zdjęcie co ", "Photo every ") + String.format(pl, "%.1f", interval).removeSuffix(",0").removeSuffix(".0") + tr(" s · 1 min nagrania = ", " s · 1 min of shooting = ") +
        String.format(pl, "%.1f", perMinute).removeSuffix(",0").removeSuffix(".0") + tr(" s filmu", " s of video")
}

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
    CaptureMode.LONG -> Icons.Rounded.ShutterSpeed
    CaptureMode.TIMELAPSE -> Icons.Rounded.Timelapse
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
    displaysInfo: String = "",
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
                    mode?.title ?: tr("Ogólne", "General"),
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
                if (locked) Text(tr("Część opcji jest zablokowana podczas nagrywania lub przetwarzania.", "Some options are locked while recording or processing."), color = Palette.Amber, fontSize = 12.sp)
                if (current == null) {
                    GeneralTab(global, caps, update, onGlobal, onCheckUpdate, onDownloadUpdate, onInstallUpdate, displaysInfo)
                } else {
                    ModeTab(current, profiles.getValue(current), caps, !locked) { t -> onProfile(current, t) }
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).clickable { onReset(current) }.padding(12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.RestartAlt, null, tint = Palette.TextDim, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(tr("Przywróć domyślne: ${current.title}", "Restore defaults: ${current.title}"), color = Palette.TextDim, fontSize = 13.sp)
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
                Text(tr("Ustawienia", "Settings"), fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                GlassIconButton(Icons.Rounded.Close, tr("Zamknij", "Close"), 0f, size = 38.dp, onClick = onDismiss)
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
    if (mode == CaptureMode.PRO) {
        Group(tr("Tryb ręczny", "Manual mode")) {
            Chips(Icons.Rounded.Tune, tr("Nagrywanie", "Capture"), listOf(false, true), s.proVideo, { if (it) tr("Wideo", "Video") else tr("Zdjęcia", "Photos") }, enabled, hint = tr("Ręczne ISO, czas, ostrość i balans bieli działają w obu wariantach", "Manual ISO, shutter, focus and white balance work in both")) { v ->
                change { it.copy(proVideo = v) }
            }
            if (s.proVideo) {
                if (caps.flatProfile) {
                    SwitchRow(
                        Icons.Rounded.Gradient,
                        tr("Płaski profil (LOG)", "Flat profile (LOG)"),
                        tr("Miękka krzywa i bez wyostrzania – pod korekcję kolorów; nie łączy się z HDR", "Soft curve, no sharpening – for colour grading; not combined with HDR"),
                        s.flatProfile,
                        enabled,
                    ) { v -> change { it.copy(flatProfile = v) } }
                } else {
                    InfoRow(Icons.Rounded.Gradient, tr("Ten aparat nie pozwala na płaski profil (LOG)", "This camera does not allow a flat (LOG) profile"))
                }
            }
        }
        // Wariant wideo ma te same ustawienia co tryb Wideo (zapisywane w profilu trybu ręcznego).
        if (s.proVideo) {
            ModeTab(CaptureMode.VIDEO, s, caps, enabled, change)
            return
        }
    }
    when (mode) {
        CaptureMode.VIDEO -> {
            Group(tr("Obraz", "Image")) {
                Chips(Icons.Rounded.HighQuality, tr("Rozdzielczość", "Resolution"), caps.videoResolutions, s.videoResolution, { it.label }, enabled) { v -> change { it.copy(videoResolution = v) } }
                val fpsOptions = VideoFps.entries.filter {
                    when (it) {
                        VideoFps.F60 -> caps.fps60
                        VideoFps.F24 -> caps.fps24
                        else -> true
                    }
                }
                Chips(Icons.Rounded.Speed, tr("Klatki na sekundę", "Frames per second"), fpsOptions, s.videoFps, { it.label }, enabled) { v -> change { it.copy(videoFps = v) } }
                Chips(Icons.Rounded.DataUsage, tr("Przepływność", "Bitrate"), VideoBitrate.entries, s.bitrate, { it.label }, enabled) { v -> change { it.copy(bitrate = v) } }
                if (caps.videoHlg) {
                    SwitchRow(Icons.Rounded.HdrOn, "HDR 10-bit (HLG)", tr("Szerszy zakres tonalny, większe pliki", "Wider tonal range, larger files"), s.videoHdr, enabled) { v -> change { it.copy(videoHdr = v) } }
                }
                if (caps.videoStabilization) {
                    SwitchRow(Icons.Rounded.CenterFocusStrong, tr("Stabilizacja", "Stabilization"), tr("Mniej drgań, lekko węższy kadr", "Less shake, slightly tighter frame"), s.stabilization, enabled) { v -> change { it.copy(stabilization = v) } }
                }
                if (caps.lowLightBoost) {
                    SwitchRow(Icons.Rounded.Brightness4, tr("Wzmocnienie w ciemności", "Low-light boost"), tr("Jaśniejszy obraz przy słabym świetle", "Brighter image in dim light"), s.lowLightBoost, enabled) { v -> change { it.copy(lowLightBoost = v) } }
                }
            }
            Group(tr("Dźwięk i zbliżanie", "Sound and zoom")) {
                shared(SharedGroup.VIDEO)
                SwitchRow(Icons.Rounded.Mic, tr("Nagrywaj dźwięk", "Record sound"), null, s.videoAudio, enabled) { v -> change { it.copy(videoAudio = v) } }
                SwitchRow(Icons.Rounded.ZoomIn, tr("Płynne zbliżanie", "Smooth zoom"), tr("Wolny, równy najazd podczas nagrywania", "Slow, even zoom while recording"), s.cinematicZoom) { v -> change { it.copy(cinematicZoom = v) } }
            }
            FramingGroup(s, change, showAspect = false, showHistogram = false, showTimer = false)
        }

        CaptureMode.SLOWMO -> {
            Group(tr("Zwolnione tempo", "Slow motion")) {
                if (caps.slowMoProfiles.isEmpty()) {
                    InfoRow(Icons.Rounded.Info, tr("Ten obiektyw nie obsługuje nagrywania szybkoklatkowego", "This lens does not support high-frame-rate recording"))
                } else {
                    InfoRow(Icons.Rounded.CameraAlt, tr("Tylko tylny aparat główny · 1×", "Main rear camera only · 1×"))
                    val resolutions = caps.slowMoProfiles.map { it.resolution }.distinct().sortedBy { it.ordinal }
                    val resolution = s.slowMoResolution.takeIf { it in resolutions } ?: resolutions.first()
                    Chips(Icons.Rounded.HighQuality, tr("Rozdzielczość", "Resolution"), resolutions, resolution, { it.label }, enabled) { selectedResolution ->
                        val fps = caps.slowMoProfiles.filter { it.resolution == selectedResolution }.map { it.fps }
                        change { current -> current.copy(slowMoResolution = selectedResolution, slowMoFps = current.slowMoFps.takeIf { it in fps } ?: fps.min()) }
                    }
                    val options = caps.slowMoProfiles.filter { it.resolution == resolution }.map { it.fps }.distinct().sorted()
                    val selected = s.slowMoFps.takeIf { it in options } ?: options.first()
                    Chips(Icons.Rounded.SlowMotionVideo, tr("Prędkość nagrywania", "Capture rate"), options, selected, { tr("$it kl/s · 1/${it / 30}×", "$it fps · 1/${it / 30}×") }, enabled) { selectedFps ->
                        change { it.copy(slowMoFps = selectedFps) }
                    }
                }
                SwitchRow(Icons.Rounded.ZoomIn, tr("Płynne zbliżanie", "Smooth zoom"), null, s.cinematicZoom) { v -> change { it.copy(cinematicZoom = v) } }
            }
            FramingGroup(s, change, showAspect = false, showHistogram = false, showTimer = false)
        }

        CaptureMode.TIMELAPSE -> {
            Group(tr("Film poklatkowy", "Time-lapse")) {
                val intervalS = s.tlSpeed.toFloat() / s.tlFps
                Chips(
                    Icons.Rounded.Speed,
                    tr("Przyspieszenie", "Speed-up"),
                    com.blacksingularities.registrer.data.TIMELAPSE_SPEEDS.map { it.first },
                    s.tlSpeed,
                    { sp -> "×$sp " + (com.blacksingularities.registrer.data.TIMELAPSE_SPEEDS.firstOrNull { it.first == sp }?.second ?: "") },
                    enabled,
                    hint = timelapseHint(s.tlSpeed, s.tlFps),
                ) { v -> change { it.copy(tlSpeed = v) } }
                Chips(Icons.Rounded.SlowMotionVideo, tr("Klatkaż filmu", "Video frame rate"), listOf(24, 30, 60), s.tlFps, { tr("$it kl/s", "$it fps") }, enabled,
                    hint = tr("Zdjęcie co ", "Photo every ") + String.format(com.blacksingularities.registrer.i18n.Lang.locale, "%.1f", intervalS).removeSuffix(",0").removeSuffix(".0") + " s") { v ->
                    change { it.copy(tlFps = v) }
                }
                val resolutions = com.blacksingularities.registrer.data.TimelapseResolution.entries.filter {
                    com.blacksingularities.registrer.camera.TimelapseWriter.pickMime(it.width, it.height) != null
                }
                Chips(Icons.Rounded.HighQuality, tr("Rozdzielczość", "Resolution"), resolutions, s.tlResolution, { it.label }, enabled,
                    hint = tr("8K wymaga aparatu 33 MP+ i kodera 8K; gdy brak – film powstanie w 4K", "8K needs a 33 MP+ camera and an 8K encoder; otherwise the video is made in 4K")) { v ->
                    change { it.copy(tlResolution = v) }
                }
                Chips(Icons.Rounded.Timer, tr("Zakończ po", "Stop after"), listOf(0, 5, 15, 30, 60, 120), s.tlStopMin, { if (it == 0) tr("Ręcznie", "Manually") else autoStopLabel(it) }, enabled) { v ->
                    change { it.copy(tlStopMin = v) }
                }
                SwitchRow(Icons.Rounded.Exposure, tr("Wyrównywanie jasności", "Deflicker"), tr("Tłumi migotanie między klatkami", "Smooths brightness flicker between frames"), s.tlDeflicker, enabled) { v ->
                    change { it.copy(tlDeflicker = v) }
                }
                SwitchRow(Icons.Rounded.Lock, tr("Blokada ekspozycji", "Exposure lock"), tr("Stała jasność i barwa – wyłącz przy zachodzie słońca", "Constant brightness and colour – turn off for sunsets"), s.tlLock, enabled) { v ->
                    change { it.copy(tlLock = v) }
                }
            }
            FramingGroup(s, change, showAspect = false, showHistogram = false, showTimer = false)
        }

        CaptureMode.AUDIO -> {
            Group(tr("Jakość", "Quality")) {
                Chips(Icons.Rounded.GraphicEq, "Format", AudioPreset.entries, s.audioPreset, { it.label }, enabled, hint = s.audioPreset.description) { v ->
                    change { it.copy(audioPreset = v) }
                }
            }
            Group(tr("Mikrofon", "Microphone")) {
                Chips(Icons.Rounded.Headset, tr("Wejście", "Input"), AudioInput.entries, s.audioInput, { it.label }, enabled, hint = s.audioInput.description) { v ->
                    change { it.copy(audioInput = v) }
                }
                Chips(Icons.Rounded.Mic, tr("Przetwarzanie", "Processing"), AudioSourceType.entries, s.audioSource, { it.label }, enabled, hint = s.audioSource.description) { v ->
                    change { it.copy(audioSource = v) }
                }
                Chips(Icons.Rounded.Equalizer, tr("Wzmocnienie", "Gain"), listOf(0, 6, 12, 18, 24), s.audioGainDb, { if (it == 0) "0 dB" else "+$it dB" }, enabled, hint = tr("Cyfrowe, z miękkim ogranicznikiem – dla cichych źródeł i wykładów", "Digital, with a soft limiter – for quiet sources and lectures")) { v ->
                    change { it.copy(audioGainDb = v) }
                }
                if (AudioEngine.noiseSuppressionAvailable) {
                    SwitchRow(Icons.Rounded.NoiseAware, tr("Redukcja szumów", "Noise reduction"), tr("Filtr systemowy: czystsza mowa, gorzej dla muzyki", "System filter: cleaner speech, worse for music"), s.audioNoiseSuppression, enabled) { v ->
                        change { it.copy(audioNoiseSuppression = v) }
                    }
                }
                if (AudioEngine.agcAvailable) {
                    SwitchRow(Icons.Rounded.GraphicEq, tr("Samoczynna głośność", "Auto gain"), tr("Wyrównuje cichych i głośnych mówców", "Evens out quiet and loud speakers"), s.audioAgc, enabled) { v ->
                        change { it.copy(audioAgc = v) }
                    }
                }
            }
            Group(tr("Nagrywanie", "Capture")) {
                SwitchRow(Icons.Rounded.FastForward, tr("Pomijaj ciszę", "Skip silence"), tr("Przerwy dłuższe niż 1,5 s nie trafiają do pliku", "Pauses longer than 1.5 s are left out of the file"), s.audioSkipSilence, enabled) { v ->
                    change { it.copy(audioSkipSilence = v) }
                }
                Chips(Icons.Rounded.Timer, tr("Automatyczne zatrzymanie", "Auto stop"), listOf(0, 15, 30, 60, 120), s.audioAutoStopMin, { if (it == 0) tr("Wył.", "Off") else autoStopLabel(it) }, enabled) { v ->
                    change { it.copy(audioAutoStopMin = v) }
                }
                InfoRow(Icons.Rounded.Info, tr("Nagrywanie trwa w tle i przy zgaszonym ekranie – pauza, znacznik i stop są w powiadomieniu", "Recording continues in the background and with the screen off – pause, marker and stop are in the notification"))
            }
        }

        else -> {
            when (mode) {
                CaptureMode.NIGHT -> Group(tr("Tryb nocny", "Night mode")) {
                    if (caps.nightExtension) {
                        InfoRow(Icons.Rounded.NightsStay, tr("Aktywny sprzętowy tryb nocny producenta – liczbę klatek dobiera telefon", "Manufacturer hardware night mode active – the phone picks the frame count"))
                    } else {
                        Chips(Icons.Rounded.Layers, tr("Liczba klatek", "Frame count"), listOf(0, 4, 6, 8, 12), s.nightFrames, { if (it == 0) "Auto" else "×$it" }, enabled, hint = tr("Auto dobiera 4–12 klatek do jasności sceny", "Auto picks 4–12 frames for the scene brightness")) { v ->
                            change { it.copy(nightFrames = v) }
                        }
                    }
                    if (caps.lowLightBoost && !caps.nightExtension) {
                        SwitchRow(Icons.Rounded.Brightness4, tr("Jaśniejszy podgląd", "Brighter preview"), tr("Wzmocnienie przy słabym świetle", "Boost in dim light"), s.lowLightBoost, enabled) { v -> change { it.copy(lowLightBoost = v) } }
                    }
                }
                CaptureMode.PORTRAIT -> Group(tr("Portret", "Portrait")) {
                    val effects = PortraitEffect.entries.filter {
                        (it == PortraitEffect.BOKEH && caps.bokehExtension) || (it == PortraitEffect.RETOUCH && caps.retouchExtension)
                    }
                    if (effects.size > 1) {
                        Chips(Icons.Rounded.Face, tr("Efekt", "Effect"), effects, s.portraitEffect, { it.label }, enabled) { v -> change { it.copy(portraitEffect = v) } }
                    } else if (effects.isNotEmpty()) {
                        InfoRow(Icons.Rounded.Face, tr("Sprzętowy efekt producenta: ${effects.first().label}", "Manufacturer hardware effect: ${effects.first().label}"))
                    } else {
                        InfoRow(Icons.Rounded.Face, tr("Programowe rozmycie tła – wykrywanie osoby (tylny i przedni aparat)", "Software background blur – person detection (rear and front camera)"))
                    }
                    if (!caps.bokehExtension) {
                        Slider(Icons.Rounded.Face, tr("Siła rozmycia tła", "Background blur strength"), s.portraitBlur, 0..100, true) { v -> change { it.copy(portraitBlur = v) } }
                    }
                }
                CaptureMode.LONG -> Group(tr("Długi czas", "Long exposure")) {
                    Chips(Icons.Rounded.ShutterSpeed, tr("Czas naświetlania", "Exposure time"), LONG_SECONDS, s.longSeconds, { "$it s" }, enabled, hint = tr("Dłużej = mocniejsze rozmycie; najwyżej 32 klatki", "Longer = stronger blur; at most 32 frames")) { v ->
                        change { it.copy(longSeconds = v) }
                    }
                    Chips(Icons.Rounded.Layers, tr("Łączenie klatek", "Frame blending"), com.blacksingularities.registrer.data.LongBlend.entries, s.longBlend, { it.label }, enabled, hint = s.longBlend.description) { v ->
                        change { it.copy(longBlend = v) }
                    }
                    SwitchRow(Icons.Rounded.CenterFocusStrong, tr("Wyrównuj klatki", "Align frames"), tr("Do zdjęć z ręki; na statywie wyłącz – tło będzie ostrzejsze", "For handheld shots; turn off on a tripod for a sharper background"), s.longAlign, enabled) { v ->
                        change { it.copy(longAlign = v) }
                    }
                }
                CaptureMode.HDR -> Group("HDR") {
                    if (caps.hdrExtension) {
                        InfoRow(Icons.Rounded.HdrOn, tr("Aktywny sprzętowy HDR producenta", "Manufacturer hardware HDR active"))
                    } else {
                        Chips(Icons.Rounded.Tune, tr("Rozpiętość ekspozycji", "Exposure spread"), listOf(1f, 1.7f, 2.3f), s.hdrStops, { "±" + String.format(Locale.US, "%.1f", it).let { if (com.blacksingularities.registrer.i18n.Lang.polish) it.replace('.', ',') else it } + " EV" }, enabled) { v ->
                            change { it.copy(hdrStops = v) }
                        }
                    }
                }
                else -> Unit
            }
            if (mode == CaptureMode.PRO) {
                Group(tr("Braketowanie", "Bracketing")) {
                    Chips(
                        Icons.Rounded.Exposure,
                        tr("Liczba zdjęć", "Shots"),
                        listOf(0, 3, 5, 7),
                        s.bracketCount,
                        { if (it == 0) tr("Wył.", "Off") else "$it" },
                        enabled,
                        hint = tr(
                            "Seria o różnej ekspozycji, zapisana osobno – do późniejszej obróbki HDR",
                            "A series with different exposures, saved separately – for later HDR editing",
                        ),
                    ) { v -> change { it.copy(bracketCount = v) } }
                    if (s.bracketCount > 0) {
                        Chips(
                            Icons.Rounded.Tune,
                            tr("Krok", "Step"),
                            listOf(0.7f, 1f, 2f),
                            s.bracketStep,
                            { "±" + String.format(com.blacksingularities.registrer.i18n.Lang.locale, "%.1f", it) + " EV" },
                            enabled,
                        ) { v -> change { it.copy(bracketStep = v) } }
                    }
                }
            }
            if (mode == CaptureMode.PHOTO || mode == CaptureMode.PRO) {
                Group(tr("Seria i interwał", "Burst and interval")) {
                    if (mode == CaptureMode.PHOTO) {
                        SwitchRow(Icons.Rounded.BurstMode, tr("Seria po przytrzymaniu", "Burst on hold"), tr("Zdjęcia seryjne, dopóki trzymasz migawkę", "Burst shots while you hold the shutter"), s.burst) { v -> change { it.copy(burst = v) } }
                    }
                    Chips(Icons.Rounded.AvTimer, tr("Interwałometr", "Intervalometer"), listOf(0, 2, 5, 10, 30, 60), s.intervalSeconds, { if (it == 0) tr("Wył.", "Off") else "${it}s" }) { v ->
                        change { it.copy(intervalSeconds = v) }
                    }
                    if (s.intervalSeconds > 0) {
                        Chips(Icons.Rounded.Layers, tr("Liczba zdjęć", "Number of photos"), listOf(0, 10, 30, 100, 300), s.intervalCount, { if (it == 0) "∞" else "$it" }) { v ->
                            change { it.copy(intervalCount = v) }
                        }
                    }
                }
            }
            Group(tr("Zdjęcie", "Photo"), collapsible = true, initiallyExpanded = false) {
                shared(SharedGroup.PHOTO)
                if (mode == CaptureMode.PHOTO || mode == CaptureMode.PRO) {
                    val formats = PhotoFormat.entries.filter { it in caps.photoFormats }
                    if (formats.size > 1) {
                        Chips(Icons.Rounded.Image, "Format", formats, s.photoFormat, { it.label }, enabled, hint = tr("RAW zapisuje plik DNG prosto z sensora", "RAW saves a DNG file straight from the sensor")) { v ->
                            change { it.copy(photoFormat = v) }
                        }
                    } else {
                        InfoRow(Icons.Rounded.Image, tr("RAW i Ultra HDR nie są dostępne dla tego obiektywu", "RAW and Ultra HDR are not available for this lens"))
                    }
                }
                Slider(Icons.Rounded.HighQuality, tr("Jakość JPEG", "JPEG quality"), s.jpegQuality, 70..100, enabled) { v -> change { it.copy(jpegQuality = v) } }
                if (mode != CaptureMode.NIGHT && mode != CaptureMode.HDR && mode != CaptureMode.LONG) {
                    val priorities = CapturePriority.entries
                    Chips(Icons.Rounded.Bolt, tr("Priorytet migawki", "Shutter priority"), priorities, s.priority, { if (it == CapturePriority.SPEED && caps.zsl) tr("Szybkość (bez opóźnienia)", "Speed (zero lag)") else it.label }, enabled) { v ->
                        change { it.copy(priority = v) }
                    }
                }
                if (caps.hasFront) {
                    SwitchRow(Icons.Rounded.Flip, tr("Lustrzane odbicie autoportretu", "Mirror selfies"), tr("Zapis jak w podglądzie przedniego aparatu", "Save as seen in the front camera preview"), s.mirrorFront) { v -> change { it.copy(mirrorFront = v) } }
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
    Group(tr("Kadr", "Framing"), collapsible = true, initiallyExpanded = false) {
        SharedSwitch(SharedGroup.FRAMING, s, change)
        if (showAspect) Chips(Icons.Rounded.AspectRatio, tr("Proporcje", "Aspect ratio"), AspectRatioMode.entries, s.aspect, { it.label }, enabled) { v -> change { it.copy(aspect = v) } }
        MultiChips(Icons.Rounded.GridOn, tr("Siatki", "Grids"), GridType.entries, s.grids, { it.label }) { v ->
            change { it.copy(grids = if (v in it.grids) it.grids - v else it.grids + v) }
        }
        if (showTimer) Chips(Icons.Rounded.Timer, tr("Samowyzwalacz", "Self-timer"), listOf(0, 3, 5, 10), s.timerSeconds, { if (it == 0) tr("Wył.", "Off") else "$it s" }) { v -> change { it.copy(timerSeconds = v) } }
        SwitchRow(Icons.Rounded.Straighten, tr("Poziomica", "Level"), null, s.level) { v -> change { it.copy(level = v) } }
        if (showHistogram) SwitchRow(Icons.Rounded.BarChart, tr("Histogram na żywo", "Live histogram"), null, s.histogram, enabled) { v -> change { it.copy(histogram = v) } }
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
    displaysInfo: String = "",
) {
    // Szybkie przełączniki: dźwięki, wibracje i zapis miejsca.
    Group(null) {
        TileGrid(
            listOf(
                Tile(Icons.Rounded.Vibration, tr("Wibracje", "Vibration"), g.haptics) { v -> onGlobal { it.copy(haptics = v) } },
                Tile(Icons.Rounded.CameraAlt, tr("Migawka", "Shutter"), g.shutterSound) { v -> onGlobal { it.copy(shutterSound = v) } },
                Tile(Icons.Rounded.Alarm, tr("Odliczanie", "Countdown"), g.timerBeep) { v -> onGlobal { it.copy(timerBeep = v) } },
                Tile(Icons.Rounded.LocationOn, tr("Lokalizacja", "Location"), g.geotag) { v -> onGlobal { it.copy(geotag = v) } },
            ),
        )
        Hint(tr("Dźwięk migawki, sygnał samowyzwalacza i zapis miejsca w zdjęciach oraz filmach.", "Shutter sound, self-timer beep and saving the location in photos and videos."))
    }

    Group(tr("Na ekranie", "On screen")) {
        TileGrid(
            listOf(
                Tile(Icons.Rounded.Speed, tr("Parametry", "Exposure"), g.exposureInfo) { v -> onGlobal { it.copy(exposureInfo = v) } },
                Tile(Icons.Rounded.Explore, tr("Kompas", "Compass"), g.compass) { v -> onGlobal { it.copy(compass = v) } },
                Tile(Icons.Rounded.CenterFocusWeak, tr("Ostrość", "Focus"), g.peaking) { v -> onGlobal { it.copy(peaking = v) } },
                Tile(Icons.Rounded.Gradient, "Zebra", g.zebra) { v -> onGlobal { it.copy(zebra = v) } },
                Tile(Icons.Rounded.GraphicEq, tr("Dźwięk", "Audio"), g.audioMeter) { v -> onGlobal { it.copy(audioMeter = v) } },
            ),
        )
        if (g.zebra) {
            CompactChoice(
                Icons.Rounded.Gradient,
                tr("Próg zebry", "Zebra threshold"),
                listOf(85, 90, 95, 98),
                g.zebraPercent,
                label = { tr("$it% jasności", "$it% brightness") },
                short = { "$it" },
            ) { v -> onGlobal { it.copy(zebraPercent = v) } }
        }
        CompactChoice(
            Icons.Rounded.BarChart,
            "Histogram",
            com.blacksingularities.registrer.data.HistogramMode.entries,
            g.histogramMode,
            label = { it.label },
            hint = tr("włączasz go w ustawieniach trybu", "turn it on in the mode settings"),
        ) { v -> onGlobal { it.copy(histogramMode = v) } }
        CompactChoice(
            Icons.Rounded.AspectRatio,
            tr("Wielkość histogramu", "Histogram size"),
            com.blacksingularities.registrer.data.HistogramSize.entries,
            g.histogramSize,
            label = { it.label },
            short = { it.label.take(1) },
        ) { v -> onGlobal { it.copy(histogramSize = v) } }
        Hint(
            tr("Ostrość – czerwony obrys najostrzejszych krawędzi; Zebra – paski na prześwietleniach; ", "Focus – red outline on the sharpest edges; Zebra – stripes on overexposed areas; ") +
                tr("Dźwięk – poziom w trybie Wideo. Nakładki przeciągasz po kadrze, upuszczone na koszu znikają.", "Audio – level in Video mode. Drag overlays around the frame; drop them on the bin to hide them."),
        )
    }

    Group(tr("Sterowanie", "Controls")) {
        CompactChoice(
            Icons.Rounded.ScreenRotation,
            tr("Obrót ekranu", "Screen rotation"),
            ScreenRotation.entries,
            g.screenRotation,
            label = { it.label },
            icon = { if (it == ScreenRotation.FIXED) Icons.Rounded.ScreenLockRotation else Icons.Rounded.ScreenRotation },
            hint = g.screenRotation.description,
        ) { v -> onGlobal { it.copy(screenRotation = v) } }
        DefaultCameraRow()
        CompactChoice(
            Icons.Rounded.VolumeUp,
            tr("Klawisze głośności", "Volume keys"),
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
            Icons.Rounded.PresentToAll,
            tr("Podgląd na drugim ekranie", "Preview on second screen"),
            listOf(true, false),
            g.subjectMirror,
            label = { if (it) tr("Jak w lustrze", "Mirrored") else tr("Bez odbicia", "Not mirrored") },
            icon = { if (it) Icons.Rounded.Flip else Icons.Rounded.CropFree },
        ) { v -> onGlobal { it.copy(subjectMirror = v) } }
    }

    Group(tr("Język", "Language")) {
        CompactChoice(
            Icons.Rounded.Translate,
            tr("Język aplikacji", "App language"),
            com.blacksingularities.registrer.i18n.AppLanguage.entries,
            g.language,
            label = { it.label },
            short = {
                when (it) {
                    com.blacksingularities.registrer.i18n.AppLanguage.SYSTEM -> "Auto"
                    com.blacksingularities.registrer.i18n.AppLanguage.PL -> "PL"
                    com.blacksingularities.registrer.i18n.AppLanguage.EN -> "EN"
                }
            },
            hint = tr("Auto – zgodnie z językiem systemu", "Auto – follows the system language"),
        ) { v -> onGlobal { it.copy(language = v) } }
    }
    Group(tr("Poziomica", "Level"), collapsible = true, initiallyExpanded = false) {
        CompactChoice(
            Icons.Rounded.Straighten,
            tr("Wygląd", "Style"),
            LevelStyle.entries,
            g.levelStyle,
            label = { it.label },
            icon = {
                when (it) {
                    LevelStyle.HORIZON -> Icons.Rounded.HorizontalRule
                    LevelStyle.AVIATION -> Icons.Rounded.Flight
                    LevelStyle.TUBES -> Icons.Rounded.LinearScale
                    LevelStyle.CROSS -> Icons.Rounded.CenterFocusStrong
                }
            },
            hint = tr("włączasz ją w ustawieniach trybu", "turn it on in the mode settings"),
        ) { v -> onGlobal { it.copy(levelStyle = v) } }
        CompactChoice(
            Icons.Rounded.Tune,
            tr("Dokładność", "Precision"),
            listOf(0.5f, 1f, 2f),
            g.levelTolerance,
            label = { "±" + (if (it < 1f) "0,5" else it.toInt().toString()) + "°" },
            short = { if (it < 1f) "0,5°" else "${it.toInt()}°" },
        ) { v -> onGlobal { it.copy(levelTolerance = v) } }
        Row(
            Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 2.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            PillToggle(Icons.Rounded.SwapVert, tr("Pochylenie", "Pitch"), g.levelPitch, Modifier.weight(1f)) { v -> onGlobal { it.copy(levelPitch = v) } }
            PillToggle(Icons.Rounded.Opacity, tr("Przygaszaj", "Dim when level"), g.levelFade, Modifier.weight(1f)) { v -> onGlobal { it.copy(levelFade = v) } }
        }
        LevelCalibrationRow(
            calibrated = g.levelCalX != 0f || g.levelCalY != 0f || g.levelCalZ != 0f,
            onCalibrate = { x, y, z -> onGlobal { it.copy(levelCalX = x, levelCalY = y, levelCalZ = z) } },
            onReset = { onGlobal { it.copy(levelCalX = 0f, levelCalY = 0f, levelCalZ = 0f) } },
        )
    }

    Group(tr("Informacje o sprzęcie", "Hardware info"), collapsible = true, initiallyExpanded = false) {
        val lenses = (caps.lensZooms + caps.auxLenses.map { it.factor }).distinct().sorted()
            .joinToString(" · ") { formatZoom(it) + "×" }.ifEmpty { "1×" }
        InfoRow(Icons.Rounded.ZoomIn, tr("Obiektywy: $lenses", "Lenses: $lenses"))
        if (caps.cameraSummary.isNotEmpty()) ExpandableInfoRow(Icons.Rounded.CameraAlt, tr("Wykryte aparaty", "Detected cameras"), caps.cameraSummary)
        if (displaysInfo.isNotEmpty()) ExpandableInfoRow(Icons.Rounded.PresentToAll, tr("Wykryte ekrany", "Detected displays"), displaysInfo)
        InfoRow(Icons.Rounded.Image, tr("Formaty: ", "Formats: ") + caps.photoFormats.sortedBy { it.ordinal }.joinToString { it.label })
        InfoRow(Icons.Rounded.Movie, tr("Wideo: ", "Video: ") + caps.videoResolutions.joinToString { it.label } + (if (caps.fps60) tr(" · 60 kl/s", " · 60 fps") else "") + (if (caps.videoHlg) " · HLG" else ""))
        if (caps.slowMo) InfoRow(Icons.Rounded.SlowMotionVideo, tr("Zwolnione tempo: ", "Slow motion: ") + caps.slowMoProfiles.joinToString { tr("${it.resolution.label} ${it.fps} kl/s", "${it.resolution.label} ${it.fps} fps") })
        InfoRow(Icons.Rounded.Tune, if (caps.manualSensor) tr("Pełne sterowanie ręczne sensora", "Full manual sensor control") else tr("Ograniczone sterowanie ręczne", "Limited manual control"))
        InfoRow(Icons.Rounded.Info, tr("Pliki: DCIM/OpenShot · Recordings/OpenShot", "Files: DCIM/OpenShot · Recordings/OpenShot"))
    }

    Group(tr("Aplikacja", "App")) {
        UpdateRow(update, onCheck, onDownload, onInstall)
        Row(
            Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            PillToggle(Icons.Rounded.Autorenew, tr("Sprawdzaj sam", "Auto check"), g.autoCheckUpdates, Modifier.weight(1f)) { v ->
                onGlobal { it.copy(autoCheckUpdates = v) }
            }
            PillToggle(Icons.Rounded.Download, tr("Instaluj sam", "Auto install"), g.autoDownloadUpdates, Modifier.weight(1f)) { v ->
                onGlobal { it.copy(autoDownloadUpdates = v) }
            }
        }
        Credits()
        Spacer(Modifier.height(6.dp))
    }
}

private class Tile(val icon: ImageVector, val label: String, val value: Boolean, val onChange: (Boolean) -> Unit)

/** Siatka kafelków-przełączników po cztery w rzędzie (krótsze rzędy wyrównane do lewej). */
@Composable
private fun TileGrid(tiles: List<Tile>) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        tiles.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { t -> IconToggle(t.icon, t.label, t.value, Modifier.weight(1f), t.onChange) }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(
        text,
        fontSize = 10.sp,
        lineHeight = 13.sp,
        color = Palette.TextDim,
        modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 8.dp),
    )
}

/** Status domyślnego aparatu i przycisk ustawienia (odświeżany po powrocie z ustawień systemu). */
@Composable
private fun DefaultCameraRow() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val owner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var tick by remember { mutableStateOf(0) }
    androidx.compose.runtime.DisposableEffect(owner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, e ->
            if (e == androidx.lifecycle.Lifecycle.Event.ON_RESUME) tick++
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    val status = remember(tick) { com.blacksingularities.registrer.DefaultCamera.status(context) }
    val ours = status is com.blacksingularities.registrer.DefaultCamera.Status.Ours
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.CameraAlt, null, tint = Palette.TextDim, modifier = Modifier.size(18.dp))
        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
            Text(tr("Domyślny aparat", "Default camera"), fontSize = 13.sp)
            Text(
                when (status) {
                    is com.blacksingularities.registrer.DefaultCamera.Status.Ours ->
                        tr("OpenShot – przycisk aparatu i inne aplikacje otwierają OpenShot", "OpenShot – the camera button and other apps open OpenShot")
                    is com.blacksingularities.registrer.DefaultCamera.Status.None ->
                        tr("Nie wybrano – dotknij „Ustaw” i wybierz OpenShot → Zawsze", "Not set – tap “Set” and choose OpenShot → Always")
                    is com.blacksingularities.registrer.DefaultCamera.Status.Other ->
                        tr(
                            "Teraz: ${status.label} – w jej ustawieniach wyczyść „Otwieraj domyślnie”",
                            "Now: ${status.label} – clear “Open by default” in its settings",
                        )
                },
                fontSize = 10.sp,
                lineHeight = 12.sp,
                color = Palette.TextDim,
                maxLines = 3,
            )
        }
        if (!ours) {
            Text(
                tr("Ustaw", "Set"),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Palette.Ink,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.White)
                    .clickable { com.blacksingularities.registrer.DefaultCamera.request(context) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        } else {
            Icon(Icons.Rounded.CheckCircle, tr("Domyślny", "Default"), tint = Color.White, modifier = Modifier.size(20.dp))
        }
    }
}

/**
 * Kalibracja poziomicy: telefon leżący ekranem do góry na płaskiej powierzchni powinien
 * wskazywać 0° w obu osiach; zmierzona różnica jest zapisywana i odejmowana od odczytów.
 */
@Composable
private fun LevelCalibrationRow(calibrated: Boolean, onCalibrate: (Float, Float, Float) -> Unit, onReset: () -> Unit) {
    val gravity = rememberGravity(smoothing = 0.08f)
    var note by remember { mutableStateOf<String?>(null) }
    val tiltX = Math.toDegrees(kotlin.math.asin((gravity.x / 9.80665f).coerceIn(-1f, 1f)).toDouble()).toFloat()
    val tiltY = Math.toDegrees(kotlin.math.asin((gravity.y / 9.80665f).coerceIn(-1f, 1f)).toDouble()).toFloat()
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Straighten, null, tint = Palette.TextDim, modifier = Modifier.size(18.dp))
        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
            Text(tr("Kalibracja", "Calibration"), fontSize = 13.sp)
            Text(
                note ?: String.format(com.blacksingularities.registrer.i18n.Lang.locale, tr("%s · teraz %.1f° / %.1f°", "%s · now %.1f° / %.1f°"), if (calibrated) tr("skalibrowana", "calibrated") else tr("połóż telefon płasko ekranem do góry", "lay the phone flat, screen up"), tiltX, tiltY),
                fontSize = 10.sp,
                lineHeight = 12.sp,
                color = Palette.TextDim,
                maxLines = 2,
            )
        }
        if (calibrated) {
            Text(
                tr("Zeruj", "Reset"),
                fontSize = 12.sp,
                color = Palette.TextDim,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable { onReset(); note = tr("Kalibracja usunięta", "Calibration removed") }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
        Text(
            tr("Kalibruj", "Calibrate"),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Palette.Ink,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Color.White)
                .clickable {
                    val ok = gravity.z > 8.5f && kotlin.math.abs(gravity.x) < 1.6f && kotlin.math.abs(gravity.y) < 1.6f
                    note = if (ok) {
                        onCalibrate(gravity.x, gravity.y, gravity.z - 9.80665f)
                        tr("Zapisano – teraz telefon na tej powierzchni pokazuje 0°", "Saved – the phone now reads 0° on this surface")
                    } else {
                        tr("Połóż telefon ekranem do góry na płaskim stole i spróbuj ponownie", "Lay the phone screen up on a flat table and try again")
                    }
                }
                .padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

/** Kafelek-przełącznik: sama ikona z krótkim podpisem; włączony – biały. */
@Composable
private fun IconToggle(icon: ImageVector, label: String, value: Boolean, modifier: Modifier = Modifier, onChange: (Boolean) -> Unit) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .amoledFill(value, RoundedCornerShape(14.dp), Color.White)
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
            .amoledFill(value, RoundedCornerShape(50), Color.White)
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
                .hairline(RoundedCornerShape(50))
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
                append(tr("Pomysł i wykonanie: ", "Idea and development: "))
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
        UpdateState.Idle -> Triple(tr("Wersja ${UpdateManager.currentVersion}", "Version ${UpdateManager.currentVersion}"), tr("Sprawdź", "Check"), onCheck)
        UpdateState.Checking -> Triple(tr("Sprawdzanie…", "Checking…"), null, onCheck)
        UpdateState.UpToDate -> Triple(tr("Wersja ${UpdateManager.currentVersion} jest najnowsza", "Version ${UpdateManager.currentVersion} is the latest"), tr("Sprawdź", "Check"), onCheck)
        is UpdateState.Available -> Triple(tr("Dostępna wersja ${update.info.version}", "Version ${update.info.version} available"), tr("Pobierz", "Download"), onDownload)
        is UpdateState.Downloading -> Triple(tr("Pobieranie ${update.info.version} · ${(update.progress * 100).roundToInt()}%", "Downloading ${update.info.version} · ${(update.progress * 100).roundToInt()}%"), null, onCheck)
        is UpdateState.Ready -> Triple(tr("Wersja ${update.info.version} gotowa", "Version ${update.info.version} ready"), tr("Zainstaluj", "Install"), onInstall)
        is UpdateState.Installing -> Triple(tr("Instalowanie ${update.info.version}…", "Installing ${update.info.version}…"), null, onCheck)
        is UpdateState.Failed -> Triple(update.message, tr("Ponów", "Retry"), onCheck)
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
                    if (expanded) tr("Zwiń sekcję $title", "Collapse section $title") else tr("Rozwiń sekcję $title", "Expand section $title"),
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
                    .hairline(RoundedCornerShape(16.dp))
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
            .hairline(CircleShape),
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
        tr("Wspólne dla wszystkich trybów", "Shared by all modes"),
        if (isShared) tr("${group.label}: zmiany obowiązują we wszystkich trybach", "${group.label}: changes apply to all modes") else tr("${group.label}: ustawienia tylko dla tego trybu", "${group.label}: settings for this mode only"),
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
                        .amoledFill(sel, RoundedCornerShape(50), Palette.Amber)
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
                        .amoledFill(sel, RoundedCornerShape(50), Palette.Amber)
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
                if (expanded) tr("Zwiń", "Collapse") else tr("Rozwiń", "Expand"),
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
