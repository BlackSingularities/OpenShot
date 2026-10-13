package com.blacksingularities.registrer.ui.components

import android.content.Intent
import android.text.format.DateUtils
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blacksingularities.registrer.audio.AudioItem
import com.blacksingularities.registrer.data.AudioInput
import com.blacksingularities.registrer.data.ModeSettings
import com.blacksingularities.registrer.ui.RecState
import com.blacksingularities.registrer.ui.theme.MonoDigits
import com.blacksingularities.registrer.ui.theme.Palette
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

fun formatClock(ms: Long, tenths: Boolean = true): String {
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    val base = if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s) else String.format(Locale.US, "%02d:%02d", m, s)
    return if (tenths) base + "." + (ms % 1000) / 100 else base
}

private val PL = Locale("pl", "PL")

/** Bieżący stan nagrywania przekazywany do studia. */
data class AudioLive(
    val state: RecState,
    val finishing: Boolean,
    val millis: Long,
    val levelL: Float,
    val levelR: Float,
    val peakDb: Float,
    val clipping: Boolean,
    val silent: Boolean,
    val skippedMs: Long,
    val markers: Int,
    val device: String?,
    val format: String,
)

/** Odtwarzanie w bibliotece. */
data class AudioPlayback(
    val playingId: Long?,
    val paused: Boolean,
    val speed: Float,
    val position: () -> Float,
)

/**
 * Studio dźwięku: „orbitalny” wizualizator, mierniki szczytów z ostrzeżeniem o przesterowaniu,
 * stan nagrania (urządzenie, format, znaczniki, pomijana cisza) oraz biblioteka nagrań pogrupowana
 * dniami – z zarysem fali, przewijaniem, prędkością, znacznikami, zmianą nazwy i udostępnianiem.
 */
@Composable
fun AudioStudio(
    live: AudioLive,
    amplitudes: List<Float>,
    settings: ModeSettings,
    externalMic: String?,
    recordings: List<AudioItem>,
    playback: AudioPlayback,
    onPlay: (AudioItem) -> Unit,
    onSeek: (AudioItem, Float) -> Unit,
    onSkip: (Int) -> Unit,
    onSpeed: () -> Unit,
    onNextMarker: (AudioItem) -> Unit,
    onDelete: (AudioItem) -> Unit,
    onRename: (AudioItem, String) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var pendingDelete by remember { mutableStateOf<AudioItem?>(null) }
    var pendingRename by remember { mutableStateOf<AudioItem?>(null) }
    var expandedId by remember { mutableStateOf<Long?>(null) }
    var searching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val recording = live.state != RecState.IDLE
    val orbit by animateDpAsState(if (recording) 210.dp else 150.dp, tween(380), label = "orbitSize")

    Column(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF161320), Palette.Ink, Palette.Ink)))
            .padding(contentPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        OrbitVisualizer(live.state, amplitudes, Modifier.size(orbit))
        Text(
            formatClock(live.millis),
            style = MonoDigits.copy(fontSize = if (recording) 40.sp else 32.sp, fontWeight = FontWeight.Light),
            color = if (live.state == RecState.PAUSED) Palette.TextDim else Color.White,
        )
        Text(
            when {
                live.finishing -> "ZAPISYWANIE"
                live.state == RecState.RECORDING && live.silent -> "CISZA · POMIJANA"
                live.state == RecState.RECORDING -> "NAGRYWANIE"
                live.state == RecState.PAUSED -> "PAUZA"
                else -> settings.audioPreset.description.uppercase(PL)
            },
            color = if (live.state == RecState.RECORDING && !live.silent) Palette.Record else Palette.TextDim,
            fontSize = 12.sp,
            letterSpacing = 1.4.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(10.dp))
        if (recording) {
            LevelMeter(live, Modifier.widthIn(max = 420.dp).fillMaxWidth().padding(horizontal = 24.dp))
            Spacer(Modifier.height(8.dp))
            Pills(
                buildList {
                    add(live.format to false)
                    live.device?.let { add(it to false) }
                    if (live.markers > 0) add("Znaczniki: ${live.markers}" to true)
                    if (live.skippedMs >= 1000) add("Pominięto ${formatClock(live.skippedMs, false)}" to false)
                },
            )
        } else {
            Pills(idleSummary(settings, externalMic))
        }
        Spacer(Modifier.height(12.dp))

        val filtered = remember(recordings, query) {
            val q = query.trim()
            if (q.isEmpty()) recordings else recordings.filter { it.title.contains(q, ignoreCase = true) }
        }
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Nagrania", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                if (recordings.isNotEmpty()) {
                    val total = recordings.sumOf { it.durationMs }
                    val size = recordings.sumOf { it.sizeBytes }
                    Text(
                        "${recordings.size} · ${formatClock(total, false)} · ${formatSize(size)}",
                        color = Palette.TextDim,
                        fontSize = 12.sp,
                    )
                }
            }
            if (recordings.isNotEmpty()) {
                IconButton(onClick = {
                    searching = !searching
                    if (!searching) query = ""
                }) {
                    Icon(if (searching) Icons.Rounded.Close else Icons.Rounded.Search, if (searching) "Zamknij wyszukiwanie" else "Szukaj", tint = Palette.TextDim)
                }
            }
        }
        AnimatedVisibility(searching, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                placeholder = { Text("Szukaj w nazwach") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp),
            )
        }
        if (recordings.isEmpty()) {
            Text(
                "Brak nagrań. Naciśnij migawkę, aby zacząć – nagrywanie trwa także przy zgaszonym ekranie.",
                color = Palette.TextFaint,
                fontSize = 13.sp,
                modifier = Modifier.padding(24.dp),
            )
        } else if (filtered.isEmpty()) {
            Text("Brak nagrań pasujących do „$query”", color = Palette.TextFaint, fontSize = 13.sp, modifier = Modifier.padding(24.dp))
        }
        val entries = remember(filtered) { groupByDay(filtered) }
        LazyColumn(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(entries, key = { it.key }) { entry ->
                when (entry) {
                    is Entry.Header -> Text(
                        entry.label,
                        color = Palette.TextDim,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.6.sp,
                        modifier = Modifier.padding(start = 6.dp, top = 6.dp),
                    )
                    is Entry.Item -> {
                        val item = entry.item
                        val active = playback.playingId == item.id
                        RecordingCard(
                            item = item,
                            active = active,
                            paused = active && playback.paused,
                            expanded = active || expandedId == item.id,
                            speed = playback.speed,
                            position = playback.position,
                            onToggleExpand = { expandedId = if (expandedId == item.id) null else item.id },
                            onPlay = { onPlay(item) },
                            onSeek = { f -> onSeek(item, f) },
                            onSkip = onSkip,
                            onSpeed = onSpeed,
                            onNextMarker = { onNextMarker(item) },
                            onShare = {
                                val send = Intent(Intent.ACTION_SEND).apply {
                                    type = item.mime.ifBlank { "audio/*" }
                                    putExtra(Intent.EXTRA_STREAM, item.uri)
                                    putExtra(Intent.EXTRA_TITLE, item.title)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                runCatching { context.startActivity(Intent.createChooser(send, "Udostępnij nagranie")) }
                            },
                            onRename = { pendingRename = item },
                            onDelete = { pendingDelete = item },
                        )
                    }
                }
            }
        }
    }

    pendingDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Usunąć nagranie?") },
            text = { Text(item.title) },
            confirmButton = {
                TextButton(onClick = { onDelete(item); pendingDelete = null }) { Text("Usuń", color = Palette.Record) }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Anuluj") } },
            containerColor = Palette.PanelHigh,
        )
    }

    pendingRename?.let { item ->
        var name by remember(item.id) { mutableStateOf(item.title) }
        AlertDialog(
            onDismissRequest = { pendingRename = null },
            title = { Text("Zmień nazwę") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(80) },
                    singleLine = true,
                    supportingText = { Text("Rozszerzenie .${item.name.substringAfterLast('.', "m4a")} zostanie zachowane") },
                )
            },
            confirmButton = {
                TextButton(
                    enabled = name.isNotBlank(),
                    onClick = { onRename(item, name); pendingRename = null },
                ) { Text("Zapisz") }
            },
            dismissButton = { TextButton(onClick = { pendingRename = null }) { Text("Anuluj") } },
            containerColor = Palette.PanelHigh,
        )
    }
}

private fun idleSummary(s: ModeSettings, externalMic: String?): List<Pair<String, Boolean>> = buildList {
    add(
        when (s.audioInput) {
            AudioInput.AUTO -> (externalMic?.let { "Auto · $it" } ?: "Mikrofon dobierany samoczynnie") to false
            AudioInput.BUILTIN -> "Mikrofon telefonu" to true
            AudioInput.EXTERNAL -> (externalMic ?: "Brak mikrofonu zewnętrznego") to (externalMic != null)
        },
    )
    add(s.audioSource.label to (s.audioSource != com.blacksingularities.registrer.data.AudioSourceType.STANDARD))
    if (s.audioGainDb > 0) add("+${s.audioGainDb} dB" to true)
    if (s.audioNoiseSuppression) add("Redukcja szumów" to true)
    if (s.audioAgc) add("Samoczynna głośność" to true)
    if (s.audioSkipSilence) add("Pomijanie ciszy" to true)
    if (s.audioAutoStopMin > 0) add("Stop po ${autoStopLabel(s.audioAutoStopMin)}" to true)
}

fun autoStopLabel(min: Int): String = if (min < 60) "$min min" else "${min / 60} h"

fun formatSize(bytes: Long): String = when {
    bytes >= 1_073_741_824L -> String.format(PL, "%.2f GB", bytes / 1_073_741_824f)
    bytes >= 1_048_576L -> String.format(PL, "%.1f MB", bytes / 1_048_576f)
    else -> String.format(PL, "%d kB", (bytes / 1024).coerceAtLeast(1))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Pills(items: List<Pair<String, Boolean>>) {
    FlowRow(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items.forEach { (text, highlighted) ->
            Text(
                text,
                fontSize = 11.sp,
                color = if (highlighted) Palette.Ink else Palette.TextDim,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (highlighted) Color.White else Color.White.copy(alpha = 0.08f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
    }
}

/** Mierniki szczytów L/P w skali −60…0 dBFS ze strefą przesterowania i wskaźnikiem szczytu. */
@Composable
private fun LevelMeter(live: AudioLive, modifier: Modifier = Modifier) {
    val stereo = live.format.contains("stereo")
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Canvas(Modifier.weight(1f).height(if (stereo) 20.dp else 9.dp)) {
                fun fraction(linear: Float): Float {
                    val db = 20f * kotlin.math.log10(linear.coerceAtLeast(1e-5f))
                    return ((db + 60f) / 60f).coerceIn(0f, 1f)
                }
                val hot = (60f - 3f) / 60f
                val bar = if (stereo) (size.height - 2.dp.toPx()) / 2f else size.height
                val r = CornerRadius(bar / 2f)
                val levels = if (stereo) listOf(live.levelL, live.levelR) else listOf(live.levelL)
                levels.forEachIndexed { i, level ->
                    val top = i * (bar + 2.dp.toPx())
                    drawRoundRect(Color.White.copy(alpha = 0.1f), Offset(0f, top), Size(size.width, bar), r)
                    val f = fraction(level)
                    val safe = minOf(f, hot)
                    if (safe > 0f) drawRoundRect(Color.White, Offset(0f, top), Size(size.width * safe, bar), r)
                    if (f > hot) drawRoundRect(Palette.Record, Offset(size.width * hot, top), Size(size.width * (f - hot), bar), r)
                }
                val peak = ((live.peakDb + 60f) / 60f).coerceIn(0f, 1f)
                if (peak > 0f) {
                    drawLine(
                        if (peak > hot) Palette.Record else Color.White,
                        Offset(size.width * peak, 0f),
                        Offset(size.width * peak, size.height),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Text(
                if (live.clipping) "PRZESTER" else if (live.peakDb <= -59f) "−∞ dB" else String.format(PL, "%.0f dB", live.peakDb),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (live.clipping) Color.White else Palette.TextDim,
                modifier = Modifier
                    .width(74.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (live.clipping) Palette.Record else Color.Transparent)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                maxLines = 1,
            )
        }
        Row(Modifier.fillMaxWidth().padding(end = 84.dp, top = 2.dp)) {
            listOf("−60", "−40", "−20", "−6", "0").forEachIndexed { i, label ->
                if (i > 0) Spacer(Modifier.weight(if (i == 3) 1.4f else if (i == 4) 0.6f else 1f))
                Text(label, fontSize = 9.sp, color = Palette.TextFaint)
            }
        }
    }
}

private sealed class Entry(val key: String) {
    class Header(val label: String) : Entry("h_$label")
    class Item(val item: AudioItem) : Entry("i_${item.id}")
}

private fun groupByDay(items: List<AudioItem>): List<Entry> {
    val out = mutableListOf<Entry>()
    var last: String? = null
    val year = SimpleDateFormat("yyyy", PL).format(Date())
    for (item in items) {
        val ms = item.dateAddedSec * 1000
        val label = when {
            DateUtils.isToday(ms) -> "DZIŚ"
            DateUtils.isToday(ms + DateUtils.DAY_IN_MILLIS) -> "WCZORAJ"
            SimpleDateFormat("yyyy", PL).format(Date(ms)) == year -> SimpleDateFormat("EEEE, d MMMM", PL).format(Date(ms)).uppercase(PL)
            else -> SimpleDateFormat("d MMMM yyyy", PL).format(Date(ms)).uppercase(PL)
        }
        if (label != last) {
            out += Entry.Header(label)
            last = label
        }
        out += Entry.Item(item)
    }
    return out
}

@Composable
private fun RecordingCard(
    item: AudioItem,
    active: Boolean,
    paused: Boolean,
    expanded: Boolean,
    speed: Float,
    position: () -> Float,
    onToggleExpand: () -> Unit,
    onPlay: () -> Unit,
    onSeek: (Float) -> Unit,
    onSkip: (Int) -> Unit,
    onSpeed: () -> Unit,
    onNextMarker: () -> Unit,
    onShare: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    var progress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(active) {
        if (!active) progress = 0f
        while (active) {
            withFrameNanos { }
            progress = position()
        }
    }
    val playing = active && !paused
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (expanded) Palette.PanelHigh else Palette.Panel)
            .clickable(onClick = onToggleExpand)
            .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = if (expanded) 4.dp else 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(if (active) Color.White else Color.White.copy(alpha = 0.08f))
                    .clickable(onClick = onPlay),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (playing) "Pauza" else "Odtwórz",
                    tint = if (active) Palette.Ink else Color.White,
                )
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(item.title, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val time = SimpleDateFormat("HH:mm", PL).format(Date(item.dateAddedSec * 1000))
                val markers = if (item.markers.isNotEmpty()) " · znaczniki: ${item.markers.size}" else ""
                Text(
                    "${formatClock(item.durationMs, false)} · ${item.format} · ${formatSize(item.sizeBytes)} · $time$markers",
                    color = Palette.TextDim,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (item.peaks.isNotEmpty() || expanded) {
            Waveform(
                peaks = item.peaks,
                progress = if (active) progress else 0f,
                markers = if (item.durationMs > 0) item.markers.map { it.toFloat() / item.durationMs } else emptyList(),
                onSeek = if (expanded) onSeek else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, start = 4.dp, end = 4.dp)
                    .height(if (expanded) 40.dp else 22.dp),
            )
        }
        AnimatedVisibility(expanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Column {
                Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp)) {
                    Text(formatClock((item.durationMs * progress).toLong(), false), fontSize = 11.sp, color = Palette.TextDim, style = MonoDigits)
                    Spacer(Modifier.weight(1f))
                    Text(formatClock(item.durationMs, false), fontSize = 11.sp, color = Palette.TextDim, style = MonoDigits)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onSkip(-10_000) }, enabled = active) {
                        Icon(Icons.Rounded.Replay10, "10 s wstecz", tint = if (active) Color.White else Palette.TextFaint)
                    }
                    IconButton(onClick = { onSkip(10_000) }, enabled = active) {
                        Icon(Icons.Rounded.Forward10, "10 s naprzód", tint = if (active) Color.White else Palette.TextFaint)
                    }
                    Text(
                        String.format(PL, "%.2f", speed).trimEnd('0').trimEnd(',') + "×",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (speed != 1f) Palette.Ink else Color.White,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (speed != 1f) Color.White else Color.White.copy(alpha = 0.08f))
                            .clickable(onClick = onSpeed)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                    if (item.markers.isNotEmpty()) {
                        IconButton(onClick = onNextMarker) { Icon(Icons.Rounded.Flag, "Następny znacznik", tint = Color.White) }
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onRename) { Icon(Icons.Rounded.Edit, "Zmień nazwę", tint = Palette.TextDim) }
                    IconButton(onClick = onShare) { Icon(Icons.Rounded.Share, "Udostępnij", tint = Palette.TextDim) }
                    IconButton(onClick = onDelete) { Icon(Icons.Rounded.Delete, "Usuń", tint = Palette.TextDim) }
                }
            }
        }
    }
}

/** Zarys fali z postępem odtwarzania i znacznikami; dotknięcie lub przeciągnięcie przewija. */
@Composable
private fun Waveform(
    peaks: List<Float>,
    progress: Float,
    markers: List<Float>,
    onSeek: ((Float) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val seek by rememberUpdatedState(onSeek)
    var scrub by remember { mutableStateOf<Float?>(null) }
    val gestures = if (onSeek == null) Modifier else Modifier
        .pointerInput(Unit) {
            detectTapGestures { o -> seek?.invoke((o.x / size.width).coerceIn(0f, 1f)) }
        }
        .pointerInput(Unit) {
            detectHorizontalDragGestures(
                onDragStart = { o -> scrub = (o.x / size.width).coerceIn(0f, 1f) },
                onDragEnd = {
                    scrub?.let { f -> seek?.invoke(f) }
                    scrub = null
                },
                onDragCancel = { scrub = null },
            ) { change, _ ->
                change.consume()
                scrub = (change.position.x / size.width).coerceIn(0f, 1f)
            }
        }
    Canvas(modifier.then(gestures)) {
        val shown = scrub ?: progress
        val mid = size.height / 2f
        val count = if (peaks.isEmpty()) 80 else peaks.size
        val step = size.width / count
        val barW = (step * 0.62f).coerceAtLeast(1.2f)
        for (i in 0 until count) {
            val v = if (peaks.isEmpty()) 0.06f else peaks[i].coerceIn(0.04f, 1f)
            val h = (size.height * 0.92f * v).coerceAtLeast(barW)
            val x = i * step + step / 2f
            val played = (i + 0.5f) / count <= shown
            drawLine(
                if (played) Color.White else Color.White.copy(alpha = 0.26f),
                Offset(x, mid - h / 2f),
                Offset(x, mid + h / 2f),
                strokeWidth = barW,
                cap = StrokeCap.Round,
            )
        }
        markers.forEach { f ->
            val x = size.width * f.coerceIn(0f, 1f)
            drawCircle(Palette.Record, radius = 2.6.dp.toPx(), center = Offset(x, 2.6.dp.toPx()))
            drawCircle(Palette.Record, radius = 2.6.dp.toPx(), center = Offset(x, size.height - 2.6.dp.toPx()))
        }
        if (shown > 0f) {
            val x = size.width * shown
            drawCircle(Color.White, radius = 3.dp.toPx(), center = Offset(x, mid))
        }
    }
}

@Composable
private fun OrbitVisualizer(state: RecState, amplitudes: List<Float>, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "orbit")
    val breath by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(2600), RepeatMode.Reverse), label = "breath")
    val spin by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(24000, easing = LinearEasing)), label = "spin")
    val recording = state == RecState.RECORDING
    val level = if (recording) amplitudes.lastOrNull() ?: 0f else 0f

    Canvas(modifier.padding(16.dp)) {
        val c = center
        val core = size.minDimension * 0.18f
        val ringR = size.minDimension * 0.30f
        val maxBar = size.minDimension * 0.19f
        val bars = 120

        // Delikatna poświata rdzenia
        drawCircle(
            Brush.radialGradient(
                listOf(Palette.Amber.copy(alpha = 0.28f + 0.4f * level), Color.Transparent),
                center = c,
                radius = core * (2.4f + level),
            ),
            radius = core * (2.4f + level),
            center = c,
        )
        drawCircle(Color.White.copy(alpha = 0.06f), ringR, c, style = Stroke(1.dp.toPx()))

        for (i in 0 until bars) {
            val idx = amplitudes.size - bars + i
            val amp = if (idx >= 0) amplitudes[idx] else 0f
            val idle = if (!recording) 0.04f + 0.03f * sin((i / bars.toFloat()) * 2f * PI.toFloat() * 3f + breath * 6f) else 0f
            val v = (amp * amp + idle).coerceIn(0f, 1f)
            val a = Math.toRadians((i * 360.0 / bars) + spin).toFloat()
            val dir = Offset(cos(a), sin(a))
            val start = c + dir * ringR
            val end = c + dir * (ringR + 2.dp.toPx() + v * maxBar)
            val t = i / bars.toFloat()
            drawLine(
                androidx.compose.ui.graphics.lerp(Palette.Amber, Palette.Coral, t).copy(alpha = 0.35f + 0.65f * (if (idx >= 0) 1f else 0.4f)),
                start,
                end,
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
        val coreR = core * (0.92f + 0.08f * breath + 0.35f * level)
        drawCircle(if (recording) Palette.Record else if (state == RecState.PAUSED) Palette.TextDim else Palette.Amber, coreR, c)
        drawCircle(Palette.Ink, coreR * 0.38f, c)
    }
}
