package com.blacksingularities.registrer.ui.components

import android.content.Intent
import android.text.format.DateUtils
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blacksingularities.registrer.audio.AudioItem
import com.blacksingularities.registrer.data.ModeSettings
import com.blacksingularities.registrer.ui.RecState
import com.blacksingularities.registrer.ui.theme.MonoDigits
import com.blacksingularities.registrer.ui.theme.Palette
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

/**
 * Studio dźwięku: „orbitalny” wizualizator – historia głośności rozpięta promieniście
 * wokół pulsującego rdzenia – oraz biblioteka nagrań z odtwarzaniem i udostępnianiem.
 */
@Composable
fun AudioStudio(
    state: RecState,
    millis: Long,
    amplitudes: List<Float>,
    settings: ModeSettings,
    recordings: List<AudioItem>,
    playingId: Long?,
    playbackPosition: () -> Float,
    onPlay: (AudioItem) -> Unit,
    onDelete: (AudioItem) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var pendingDelete by remember { mutableStateOf<AudioItem?>(null) }

    Column(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF161320), Palette.Ink, Palette.Ink)))
            .padding(contentPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        OrbitVisualizer(state, amplitudes, Modifier.size(250.dp))
        Text(
            formatClock(millis),
            style = MonoDigits.copy(fontSize = 40.sp, fontWeight = FontWeight.Light),
            color = if (state == RecState.PAUSED) Palette.TextDim else Color.White,
        )
        Text(
            when (state) {
                RecState.RECORDING -> "NAGRYWANIE"
                RecState.PAUSED -> "PAUZA"
                RecState.IDLE -> "${settings.audioPreset.label} · ${settings.audioSource.label}"
            },
            color = if (state == RecState.RECORDING) Palette.Record else Palette.TextDim,
            fontSize = 12.sp,
            letterSpacing = 1.4.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Nagrania", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
            Text("${recordings.size}", color = Palette.TextDim, fontSize = 13.sp)
        }
        if (recordings.isEmpty()) {
            Text(
                "Brak nagrań. Naciśnij migawkę, aby zacząć.",
                color = Palette.TextFaint,
                fontSize = 13.sp,
                modifier = Modifier.padding(24.dp),
            )
        }
        LazyColumn(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(recordings, key = { it.id }) { item ->
                RecordingRow(
                    item = item,
                    playing = playingId == item.id,
                    playbackPosition = playbackPosition,
                    onPlay = { onPlay(item) },
                    onShare = {
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "audio/mp4"
                            putExtra(Intent.EXTRA_STREAM, item.uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        runCatching { context.startActivity(Intent.createChooser(send, "Udostępnij nagranie")) }
                    },
                    onDelete = { pendingDelete = item },
                )
            }
        }
    }

    pendingDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Usunąć nagranie?") },
            text = { Text(item.name) },
            confirmButton = {
                TextButton(onClick = { onDelete(item); pendingDelete = null }) { Text("Usuń", color = Palette.Coral) }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Anuluj") } },
            containerColor = Palette.PanelHigh,
        )
    }
}

@Composable
private fun RecordingRow(
    item: AudioItem,
    playing: Boolean,
    playbackPosition: () -> Float,
    onPlay: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
) {
    var progress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(playing) {
        progress = 0f
        while (playing) {
            withFrameNanos { }
            progress = playbackPosition()
        }
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(if (playing) Palette.PanelHigh else Palette.Panel)
            .clickable(onClick = onPlay)
            .padding(start = 8.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (playing) Palette.Amber else Color.White.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (playing) "Zatrzymaj" else "Odtwórz",
                    tint = if (playing) Palette.Ink else Color.White,
                )
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(item.name.removeSuffix(".m4a"), fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val date = DateUtils.getRelativeTimeSpanString(item.dateAddedSec * 1000).toString()
                val size = String.format(Locale.US, "%.1f MB", item.sizeBytes / 1_048_576f)
                Text("${formatClock(item.durationMs, false)} · $size · $date", color = Palette.TextDim, fontSize = 12.sp)
            }
            IconButton(onClick = onShare) { Icon(Icons.Rounded.Share, "Udostępnij", tint = Palette.TextDim) }
            IconButton(onClick = onDelete) { Icon(Icons.Rounded.Delete, "Usuń", tint = Palette.TextDim) }
        }
        if (playing) {
            Canvas(Modifier.fillMaxWidth().height(3.dp).padding(horizontal = 6.dp)) {
                drawLine(Color.White.copy(alpha = 0.12f), Offset(0f, size.height / 2), Offset(size.width, size.height / 2), size.height, StrokeCap.Round)
                drawLine(Palette.Amber, Offset(0f, size.height / 2), Offset(size.width * progress, size.height / 2), size.height, StrokeCap.Round)
            }
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
                lerp(Palette.Amber, Palette.Coral, t).copy(alpha = 0.35f + 0.65f * (if (idx >= 0) 1f else 0.4f)),
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
