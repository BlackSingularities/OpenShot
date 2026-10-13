package com.blacksingularities.registrer.ui.components

import com.blacksingularities.registrer.i18n.tr
import com.blacksingularities.registrer.ui.theme.BlockShape
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
import com.blacksingularities.registrer.ui.theme.GlassIconButton
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.Landscape
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.foundation.layout.Spacer
import androidx.compose.animation.togetherWith
import androidx.compose.animation.scaleIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedContent
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Exposure
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
import androidx.compose.foundation.layout.heightIn
import com.blacksingularities.registrer.ui.theme.rotatedLayout
import com.blacksingularities.registrer.ui.theme.activeContent
import com.blacksingularities.registrer.ui.theme.glass
import com.blacksingularities.registrer.ui.theme.GlassLevel
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
    return text.let { if (com.blacksingularities.registrer.i18n.Lang.polish) it.replace('.', ',') else it }
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
            .glass(BlockShape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        presets.forEach { p ->
            val isActive = p == active
            val scale by animateFloatAsState(if (isActive) 1.15f else 0.9f, label = "lens")
            Box(
                Modifier
                    .size(36.dp)
                    .graphicsLayer { scaleX = scale; scaleY = scale; rotationZ = iconRotation }
                    .clip(RoundedCornerShape(50))
                    
                    .clickable {
                        if (haptics) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelect(p)
                    },
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isActive) formatZoom(current) + "×" else formatZoom(p),
                        color = if (isActive) Color.White else Color.White.copy(alpha = 0.6f),
                        fontSize = if (isActive) 13.sp else 11.sp,
                        fontWeight = if (isActive) FontWeight.Black else FontWeight.Medium,
                    )
                    if (isActive) {
                        Box(
                            Modifier
                                .padding(top = 1.dp)
                                .size(4.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color.White),
                        )
                    }
                }
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
    leftIcon: ImageVector = Icons.Rounded.Remove,
    leftLabel: String = tr("Oddal", "Zoom out"),
    rightIcon: ImageVector = Icons.Rounded.Add,
    rightLabel: String = tr("Przybliż", "Zoom in"),
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
        Icon(leftIcon, leftLabel, tint = Palette.TextDim, modifier = Modifier.size(18.dp))
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
            val trackH = 4.dp.toPx()
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
            drawCircle(if (dragging) Palette.Amber else Color.White, h / 2f - 5.dp.toPx(), Offset(thumbX, cy))
            drawCircle(Palette.Ink.copy(alpha = 0.55f), 2.5f.dp.toPx(), Offset(thumbX, cy))
        }
        Icon(rightIcon, rightLabel, tint = Palette.TextDim, modifier = Modifier.size(18.dp))
    }
}

/**
 * Kompletny blok zoomu: obiektywy + przełącznik prędkości. W trybie Pro przycisk obok
 * suwaka przełącza go na ręczną ostrość (lewo – bliżej, prawo – dalej, do ∞).
 */
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
    focusAvailable: Boolean = false,
    focusMode: Boolean = false,
    focusLabel: String? = null,
    onToggleFocus: () -> Unit = {},
    onFocusRate: (rate: Float, dt: Float) -> Unit = { _, _ -> },
    evAvailable: Boolean = false,
    evMode: Boolean = false,
    evLabel: String = "0,0 EV",
    onToggleEv: () -> Unit = {},
    onEvRate: (rate: Float, dt: Float) -> Unit = { _, _ -> },
) {
    val focusing = focusAvailable && focusMode
    val adjustingEv = evAvailable && evMode
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        AnimatedContent(
            targetState = when { focusing -> 1; adjustingEv -> 2; else -> 0 },
            transitionSpec = { (fadeIn(tween(160)) + scaleIn(initialScale = 0.9f)) togetherWith fadeOut(tween(120)) },
            label = "zoomOrFocus",
        ) { control ->
            if (control != 0) {
                Row(
                    Modifier
                        .heightIn(min = 48.dp)
                        .glass(BlockShape, active = true)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(if (control == 1) Icons.Rounded.CenterFocusStrong else Icons.Rounded.Exposure, null, tint = activeContent(), modifier = Modifier.size(18.dp).graphicsLayer { rotationZ = iconRotation })
                    Text(
                        if (control == 1) tr("  Ostrość  ", "  Focus  ") + (focusLabel ?: "AF") else tr("  Ekspozycja  $evLabel", "  Exposure  $evLabel"),
                        color = activeContent(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.rotatedLayout(iconRotation),
                    )
                }
            } else {
                LensSelector(presets, current, iconRotation, haptics, onPreset)
            }
        }
        if (max - min > 0.05f || focusAvailable || evAvailable) {
            Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                if (focusing) {
                    ZoomRocker(
                        enabled = true,
                        haptics = haptics,
                        onRate = onFocusRate,
                        leftIcon = Icons.Rounded.LocalFlorist,
                        leftLabel = tr("Bliżej", "Closer"),
                        rightIcon = Icons.Rounded.Landscape,
                        rightLabel = tr("Dalej", "Farther"),
                    )
                } else if (adjustingEv) {
                    ZoomRocker(
                        enabled = true,
                        haptics = haptics,
                        onRate = onEvRate,
                        leftLabel = tr("Ciemniej", "Darker"),
                        rightLabel = tr("Jaśniej", "Brighter"),
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Stały odczyt bieżącego zoomu nad suwakiem.
                        Text(
                            formatZoom(current) + "×",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.rotatedLayout(iconRotation),
                        )
                        ZoomRocker(enabled = max - min > 0.05f, haptics = haptics, onRate = onRate)
                    }
                }
                if (focusAvailable) {
                    Spacer(Modifier.width(8.dp))
                    GlassIconButton(
                        Icons.Rounded.CenterFocusStrong,
                        if (focusMode) tr("Suwak: ostrość – dotknij, by wrócić do zoomu", "Slider: focus – tap to go back to zoom") else tr("Suwak steruje ostrością", "Slider controls focus"),
                        iconRotation,
                        size = 36.dp,
                        active = focusMode,
                        onClick = onToggleFocus,
                    )
                }
                if (evAvailable) {
                    Spacer(Modifier.width(6.dp))
                    GlassIconButton(
                        Icons.Rounded.Exposure,
                        if (evMode) tr("Suwak EV – dotknij, by wrócić do zoomu", "EV slider – tap to go back to zoom") else tr("Suwak steruje EV", "Slider controls EV"),
                        iconRotation,
                        size = 36.dp,
                        active = evMode,
                        onClick = onToggleEv,
                    )
                }
            }
        }
    }
}
