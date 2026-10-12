package com.blacksingularities.registrer.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blacksingularities.registrer.ui.theme.Palette
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln

fun formatZoom(z: Float): String = when {
    z < 0.995f -> String.format(Locale.US, "%.1f", z).trimStart('0')
    z >= 10f -> String.format(Locale.US, "%.0f", z)
    abs(z - Math.round(z)) < 0.05f -> String.format(Locale.US, "%.0f", z)
    else -> String.format(Locale.US, "%.1f", z)
}

/**
 * Pasek zoomu: szybkie skoki do obiektywów (0.5×/1×/2×/5×/10×) oraz – po przeciągnięciu –
 * rozwijana logarytmiczna linijka do płynnego, precyzyjnego zoomu z haptyką na progach.
 */
@Composable
fun ZoomBar(
    current: Float,
    target: Float,
    min: Float,
    max: Float,
    iconRotation: Float,
    haptics: Boolean,
    onTarget: (value: Float, speed: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (max - min < 0.05f) return
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val measurer = rememberTextMeasurer()
    val latestTarget by rememberUpdatedState(target)
    var rulerVisible by remember { mutableStateOf(false) }
    var releaseTick by remember { mutableIntStateOf(0) }
    val pxPerLn = with(density) { 150.dp.toPx() }

    val presets = remember(min, max) {
        buildList {
            if (min < 0.95f) add(min)
            add(1f)
            listOf(2f, 5f, 10f).filter { it <= max + 0.01f }.forEach { add(it) }
        }
    }
    val activePreset = presets.lastOrNull { it <= current + 0.02f } ?: presets.first()

    LaunchedEffect(releaseTick) {
        if (releaseTick > 0) {
            delay(1400)
            rulerVisible = false
        }
    }

    Column(
        modifier
            .fillMaxWidth()
            .pointerInput(min, max) {
                var accumulated = 0f
                detectHorizontalDragGestures(
                    onDragStart = { rulerVisible = true; accumulated = ln(latestTarget) },
                    onDragEnd = { releaseTick++ },
                    onDragCancel = { releaseTick++ },
                ) { change, dx ->
                    change.consume()
                    val before = exp(accumulated)
                    accumulated = (accumulated - dx / pxPerLn).coerceIn(ln(min), ln(max))
                    val after = exp(accumulated)
                    if (haptics && presets.any { p -> (before - p) * (after - p) < 0f }) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                    onTarget(after, 18f)
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AnimatedVisibility(rulerVisible, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(54.dp),
            ) {
                val cx = size.width / 2f
                val base = size.height - 8.dp.toPx()
                val lnCur = ln(current)
                fun xOf(v: Float) = cx + (ln(v) - lnCur) * pxPerLn
                var v = (Math.floor(min * 10.0) / 10.0).toFloat().coerceAtLeast(0.1f)
                while (v <= max + 0.001f) {
                    val x = xOf(v)
                    if (x in -20f..size.width + 20f) {
                        val isPreset = presets.any { abs(it - v) < 0.001f } || abs(v - Math.round(v)) < 0.001f && v <= 10f
                        val h = if (isPreset) 16.dp.toPx() else 7.dp.toPx()
                        val fade = 1f - (abs(x - cx) / (size.width / 2f)).coerceIn(0f, 1f)
                        drawLine(
                            Color.White.copy(alpha = 0.25f + 0.6f * fade),
                            Offset(x, base - h),
                            Offset(x, base),
                            strokeWidth = if (isPreset) 2.dp.toPx() else 1.dp.toPx(),
                        )
                        if (isPreset) {
                            val layout = measurer.measure(
                                formatZoom(v) + "×",
                                TextStyle(color = Color.White.copy(alpha = 0.4f + 0.6f * fade), fontSize = 10.sp),
                            )
                            drawText(layout, topLeft = Offset(x - layout.size.width / 2f, base - h - layout.size.height - 2.dp.toPx()))
                        }
                    }
                    v += when {
                        v < 2f -> 0.1f
                        v < 10f -> 0.5f
                        v < 30f -> 1f
                        else -> 5f
                    }
                }
                drawLine(Palette.Amber, Offset(cx, 0f), Offset(cx, base), strokeWidth = 2.5f.dp.toPx())
            }
        }
        Row(
            Modifier
                .padding(vertical = 6.dp)
                .clip(RoundedCornerShape(50))
                .background(Palette.GlassStrong)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            presets.forEach { p ->
                val active = p == activePreset
                val chipScale by animateFloatAsState(if (active) 1f else 0.82f, label = "chip")
                Box(
                    Modifier
                        .size(40.dp)
                        .graphicsLayer { scaleX = chipScale; scaleY = chipScale; rotationZ = iconRotation }
                        .clip(CircleShape)
                        .background(if (active) Color.White.copy(alpha = 0.16f) else Color.Transparent)
                        .clickable {
                            if (haptics) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onTarget(p, 7f)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (active) formatZoom(current) + "×" else formatZoom(p),
                        color = if (active) Palette.Amber else Color.White,
                        fontSize = if (active) 12.sp else 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}
