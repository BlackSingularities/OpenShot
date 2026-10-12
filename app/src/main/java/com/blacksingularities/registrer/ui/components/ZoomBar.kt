package com.blacksingularities.registrer.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blacksingularities.registrer.ui.theme.Palette
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign

/** „0,6”, „1”, „2,4”, „10” – zapis z polskim przecinkiem dziesiętnym. */
fun formatZoom(z: Float): String {
    val text = when {
        z >= 10f -> String.format(Locale.US, "%.0f", z)
        abs(z - z.roundToInt()) < 0.05f && z >= 1f -> String.format(Locale.US, "%.0f", z)
        else -> String.format(Locale.US, "%.1f", z)
    }
    return text.replace('.', ',')
}

/**
 * Przyciski obiektywów: każdy odpowiada rzeczywistej krotności (szeroki kąt np. 0,6×, główny 1×,
 * tele np. 3×). Aktywny pokazuje bieżący zoom, gdy jest pomiędzy obiektywami.
 */
@Composable
fun LensSelector(
    presets: List<Float>,
    current: Float,
    iconRotation: Float,
    haptics: Boolean,
    onSelect: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (presets.size < 2) return
    val haptic = LocalHapticFeedback.current
    val active = presets.lastOrNull { it <= current + 0.02f } ?: presets.first()
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(Palette.GlassStrong)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        presets.forEach { p ->
            val isActive = p == active
            val scale by animateFloatAsState(if (isActive) 1f else 0.84f, label = "lens")
            Box(
                Modifier
                    .size(40.dp)
                    .graphicsLayer { scaleX = scale; scaleY = scale; rotationZ = iconRotation }
                    .clip(CircleShape)
                    .background(if (isActive) Color.White.copy(alpha = 0.16f) else Color.Transparent)
                    .clickable {
                        if (haptics) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelect(p)
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (isActive) formatZoom(current) + "×" else formatZoom(p),
                    color = if (isActive) Palette.Amber else Color.White,
                    fontSize = if (isActive) 12.sp else 11.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

/**
 * Przełącznik zoomu jak w kamerze: suwak spoczywa na środku, wychylenie w prawo przybliża,
 * w lewo oddala, a im większe wychylenie, tym szybciej. Po puszczeniu wraca sprężyście
 * na środek. [onRate] dostaje prędkość w jednostkach ln(zoom)/s oraz czas klatki.
 */
@Composable
fun ZoomRocker(
    enabled: Boolean,
    haptics: Boolean,
    onRate: (rate: Float, dt: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val deflection = remember { Animatable(0f) }
    var dragging by remember { mutableStateOf(false) }
    val rateCallback by rememberUpdatedState(onRate)
    val maxRate = 1.9f

    // Pętla klatek: dopóki suwak jest wychylony, zoom zmienia się z proporcjonalną prędkością.
    LaunchedEffect(dragging) {
        if (!dragging) return@LaunchedEffect
        var last = 0L
        while (dragging) {
            val now = withFrameNanos { it }
            if (last != 0L) {
                val dt = ((now - last) / 1e9f).coerceIn(0f, 0.1f)
                val d = deflection.value
                val rate = sign(d) * d * d * maxRate
                if (abs(d) > 0.04f) rateCallback(rate, dt)
            }
            last = now
        }
    }

    Row(
        modifier.graphicsLayer { alpha = if (enabled) 1f else 0.4f },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Rounded.Remove, "Oddal", tint = Palette.TextDim, modifier = Modifier.size(18.dp))
        Canvas(
            Modifier
                .width(220.dp)
                .height(30.dp)
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    var crossedEdge = false
                    detectHorizontalDragGestures(
                        onDragStart = {
                            dragging = true
                            crossedEdge = false
                        },
                        onDragEnd = {
                            dragging = false
                            scope.launch { deflection.animateTo(0f, spring(dampingRatio = 0.55f, stiffness = 500f)) }
                        },
                        onDragCancel = {
                            dragging = false
                            scope.launch { deflection.animateTo(0f, spring(dampingRatio = 0.55f, stiffness = 500f)) }
                        },
                    ) { change, dx ->
                        change.consume()
                        val half = size.width / 2f - size.height / 2f
                        val next = (deflection.value + dx / half).coerceIn(-1f, 1f)
                        scope.launch { deflection.snapTo(next) }
                        val atEdge = abs(next) >= 0.999f
                        if (atEdge && !crossedEdge && haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        crossedEdge = atEdge
                    }
                },
        ) {
            val h = size.height
            val trackH = 6.dp.toPx()
            val cy = h / 2f
            val cx = size.width / 2f
            val half = size.width / 2f - h / 2f
            val d = deflection.value
            // tor
            drawRoundRect(
                Color.White.copy(alpha = 0.14f),
                Offset(h / 2f - trackH / 2f, cy - trackH / 2f),
                Size(size.width - h + trackH, trackH),
                CornerRadius(trackH / 2f),
            )
            // znaczniki prędkości
            for (k in -4..4) {
                if (k == 0) continue
                val x = cx + half * k / 4f
                drawLine(Color.White.copy(alpha = 0.22f), Offset(x, cy - 5.dp.toPx()), Offset(x, cy + 5.dp.toPx()), 1.dp.toPx())
            }
            // wypełnienie od środka do kciuka
            val thumbX = cx + half * d
            if (abs(d) > 0.01f) {
                val left = minOf(cx, thumbX)
                drawRoundRect(
                    Brush.horizontalGradient(
                        if (d > 0) listOf(Palette.Amber.copy(alpha = 0.3f), Palette.Amber) else listOf(Palette.Amber, Palette.Amber.copy(alpha = 0.3f)),
                        startX = left,
                        endX = left + abs(thumbX - cx),
                    ),
                    Offset(left, cy - trackH / 2f),
                    Size(abs(thumbX - cx), trackH),
                    CornerRadius(trackH / 2f),
                )
            }
            // środek
            drawCircle(Color.White.copy(alpha = 0.5f), 2.dp.toPx(), Offset(cx, cy))
            // kciuk
            drawCircle(if (dragging) Palette.Amber else Color.White, h / 2f - 4.dp.toPx(), Offset(thumbX, cy))
            drawCircle(Palette.Ink.copy(alpha = 0.6f), 3.dp.toPx(), Offset(thumbX, cy))
        }
        Icon(Icons.Rounded.Add, "Przybliż", tint = Palette.TextDim, modifier = Modifier.size(18.dp))
    }
}

/** Kompletny blok zoomu: obiektywy + przełącznik prędkości. */
@Composable
fun ZoomControls(
    presets: List<Float>,
    current: Float,
    min: Float,
    max: Float,
    iconRotation: Float,
    haptics: Boolean,
    onPreset: (Float) -> Unit,
    onRate: (rate: Float, dt: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        LensSelector(presets, current, iconRotation, haptics, onPreset)
        if (max - min > 0.05f) {
            ZoomRocker(enabled = true, haptics = haptics, onRate = onRate, modifier = Modifier.padding(top = 6.dp))
        }
    }
}
