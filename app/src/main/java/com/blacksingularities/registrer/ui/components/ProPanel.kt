package com.blacksingularities.registrer.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.blacksingularities.registrer.camera.CameraCaps
import com.blacksingularities.registrer.camera.Hud
import com.blacksingularities.registrer.camera.ProSettings
import com.blacksingularities.registrer.camera.WhiteBalance
import com.blacksingularities.registrer.ui.theme.Palette
import com.blacksingularities.registrer.ui.theme.glass
import com.blacksingularities.registrer.ui.theme.GlassLevel
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private enum class ProParam(val title: String) { ISO("ISO"), SHUTTER("S"), FOCUS("F"), WB("WB"), EV("EV") }

private val ISO_STEPS = listOf(50, 64, 80, 100, 125, 160, 200, 250, 320, 400, 500, 640, 800, 1000, 1250, 1600, 2000, 2500, 3200, 4000, 5000, 6400, 8000, 12800)
private val SHUTTER_STEPS: List<Long> = listOf(
    1_000_000_000L / 8000, 1_000_000_000L / 4000, 1_000_000_000L / 2000, 1_000_000_000L / 1000,
    1_000_000_000L / 500, 1_000_000_000L / 250, 1_000_000_000L / 125, 1_000_000_000L / 60,
    1_000_000_000L / 30, 1_000_000_000L / 15, 1_000_000_000L / 8, 1_000_000_000L / 4,
    1_000_000_000L / 2, 1_000_000_000L, 2_000_000_000L, 4_000_000_000L, 8_000_000_000L,
    15_000_000_000L, 30_000_000_000L,
)

fun formatShutter(ns: Long): String {
    val s = ns / 1e9
    return if (s >= 0.5) {
        if (abs(s - s.roundToInt()) < 0.05) "${s.roundToInt()}\"" else String.format(Locale.US, "%.1f\"", s)
    } else {
        "1/${(1 / s).roundToInt()}"
    }
}

fun formatFocus(diopters: Float): String = when {
    diopters <= 0.01f -> "∞"
    1f / diopters >= 1f -> String.format(Locale.US, "%.1f m", 1f / diopters)
    else -> "${(100f / diopters).roundToInt()} cm"
}

/** Panel trybu Pro: chipy parametrów + wspólna linijka wartości. */
@Composable
fun ProPanel(
    caps: CameraCaps,
    pro: ProSettings,
    hud: Hud?,
    evIndex: Int,
    evMin: Int,
    evMax: Int,
    evStep: Float,
    haptics: Boolean,
    onPro: (ProSettings) -> Unit,
    onEv: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var active by remember { mutableStateOf<ProParam?>(null) }
    val manual = caps.manualSensor

    val isoValues = remember(caps.isoRange) {
        val r = caps.isoRange
        ISO_STEPS.filter { r == null || it in r }.ifEmpty { ISO_STEPS }
    }
    val shutterValues = remember(caps.exposureRange) {
        val r = caps.exposureRange
        SHUTTER_STEPS.filter { r == null || it in r }.ifEmpty { SHUTTER_STEPS }
    }
    val focusValues = remember(caps.minFocusDistance) {
        val max = caps.minFocusDistance
        if (max <= 0f) emptyList() else List(31) { i -> max * i / 30f }
    }

    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        AnimatedVisibility(active != null, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
            when (active) {
                ProParam.ISO -> ValueRuler(
                    labels = listOf("A") + isoValues.map { it.toString() },
                    selected = pro.iso?.let { isoValues.indexOf(it) + 1 } ?: 0,
                    haptics = haptics,
                ) { i -> onPro(pro.copy(iso = if (i == 0) null else isoValues[i - 1])) }
                ProParam.SHUTTER -> ValueRuler(
                    labels = listOf("A") + shutterValues.map { formatShutter(it) },
                    selected = pro.exposureNs?.let { shutterValues.indexOf(it) + 1 } ?: 0,
                    haptics = haptics,
                ) { i -> onPro(pro.copy(exposureNs = if (i == 0) null else shutterValues[i - 1])) }
                ProParam.FOCUS -> ValueRuler(
                    labels = listOf("AF") + focusValues.map { formatFocus(it) },
                    selected = pro.focusDiopters?.let { d -> focusValues.indexOfFirst { abs(it - d) < 1e-4f } + 1 } ?: 0,
                    haptics = haptics,
                ) { i -> onPro(pro.copy(focusDiopters = if (i == 0) null else focusValues[i - 1])) }
                ProParam.WB -> ValueRuler(
                    labels = WhiteBalance.entries.map { it.label },
                    selected = pro.whiteBalance.ordinal,
                    haptics = haptics,
                ) { i -> onPro(pro.copy(whiteBalance = WhiteBalance.entries[i])) }
                ProParam.EV -> {
                    val values = (evMin..evMax).toList()
                    ValueRuler(
                        labels = values.map { String.format(Locale.US, "%+.1f", it * evStep) },
                        selected = values.indexOf(evIndex).coerceAtLeast(0),
                        haptics = haptics,
                    ) { i -> onEv(values[i]) }
                }
                null -> Unit
            }
        }
        Row(
            Modifier
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .glass(RoundedCornerShape(18.dp), GlassLevel.Regular)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            ProParam.entries.forEach { p ->
                val enabled = when (p) {
                    ProParam.ISO, ProParam.SHUTTER -> manual
                    ProParam.FOCUS -> focusValues.isNotEmpty()
                    ProParam.WB -> true
                    ProParam.EV -> evMax > evMin && !pro.manualExposure
                }
                val value = when (p) {
                    ProParam.ISO -> pro.iso?.toString() ?: hud?.iso?.let { "A·$it" } ?: "AUTO"
                    ProParam.SHUTTER -> pro.exposureNs?.let { formatShutter(it) } ?: hud?.exposureNs?.let { "A·" + formatShutter(it) } ?: "AUTO"
                    ProParam.FOCUS -> pro.focusDiopters?.let { formatFocus(it) } ?: "AF"
                    ProParam.WB -> pro.whiteBalance.label
                    ProParam.EV -> String.format(Locale.US, "%+.1f", evIndex * evStep)
                }
                val isManual = when (p) {
                    ProParam.ISO -> pro.iso != null
                    ProParam.SHUTTER -> pro.exposureNs != null
                    ProParam.FOCUS -> pro.focusDiopters != null
                    ProParam.WB -> pro.whiteBalance != WhiteBalance.AUTO
                    ProParam.EV -> evIndex != 0
                }
                Column(
                    Modifier
                        .width(66.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (active == p) Color.White.copy(alpha = 0.14f) else Color.Transparent)
                        .clickable(enabled = enabled) { active = if (active == p) null else p }
                        .padding(vertical = 6.dp)
                        .graphicsLayer { alpha = if (enabled) 1f else 0.35f },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(p.title, color = Palette.TextFaint, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text(
                        if (enabled) value else "—",
                        color = if (isManual) Palette.Amber else Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/** Pozioma linijka wartości z zapadkami i haptyką – przeciągnij, by wybrać. */
@Composable
fun ValueRuler(
    labels: List<String>,
    selected: Int,
    haptics: Boolean,
    onSelect: (Int) -> Unit,
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val measurer = rememberTextMeasurer()
    val stepPx = with(density) { 54.dp.toPx() }
    val callback by rememberUpdatedState(onSelect)
    var offset by remember(labels) { mutableFloatStateOf(selected.coerceAtLeast(0) * stepPx) }
    var dragging by remember { mutableStateOf(false) }

    LaunchedEffect(selected, labels) {
        if (!dragging) offset = selected.coerceAtLeast(0) * stepPx
    }

    Canvas(
        Modifier
            .fillMaxWidth()
            .height(58.dp)
            .pointerInput(labels) {
                var last = (offset / stepPx).roundToInt()
                detectHorizontalDragGestures(
                    onDragStart = { dragging = true; last = (offset / stepPx).roundToInt() },
                    onDragEnd = {
                        dragging = false
                        offset = (offset / stepPx).roundToInt() * stepPx
                    },
                    onDragCancel = { dragging = false },
                ) { change, dx ->
                    change.consume()
                    offset = (offset - dx).coerceIn(0f, (labels.size - 1) * stepPx)
                    val i = (offset / stepPx).roundToInt()
                    if (i != last) {
                        last = i
                        if (haptics) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        callback(i)
                    }
                }
            },
    ) {
        val cx = size.width / 2f
        val base = size.height - 6.dp.toPx()
        labels.forEachIndexed { i, label ->
            val x = cx + i * stepPx - offset
            if (x < -stepPx || x > size.width + stepPx) return@forEachIndexed
            val fade = 1f - (abs(x - cx) / (size.width / 2f)).coerceIn(0f, 1f)
            val center = abs(x - cx) < stepPx / 2
            drawLine(
                Color.White.copy(alpha = 0.2f + 0.6f * fade),
                Offset(x, base - 10.dp.toPx()), Offset(x, base), 1.5f.dp.toPx(),
            )
            for (k in 1..3) {
                val mx = x + k * stepPx / 4f
                if (i < labels.lastIndex) {
                    drawLine(Color.White.copy(alpha = 0.15f * fade + 0.05f), Offset(mx, base - 5.dp.toPx()), Offset(mx, base), 1.dp.toPx())
                }
            }
            val layout = measurer.measure(
                label,
                TextStyle(
                    color = if (center) Palette.Amber else Color.White.copy(alpha = 0.35f + 0.6f * fade),
                    fontSize = if (center) 14.sp else 11.sp,
                    fontWeight = if (center) FontWeight.Bold else FontWeight.Medium,
                ),
            )
            drawText(layout, topLeft = Offset(x - layout.size.width / 2f, base - 14.dp.toPx() - layout.size.height))
        }
        drawLine(Palette.Amber, Offset(cx, base - 16.dp.toPx()), Offset(cx, base + 4.dp.toPx()), 2.5f.dp.toPx())
    }
}
