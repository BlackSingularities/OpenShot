package com.blacksingularities.registrer.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blacksingularities.registrer.camera.CaptureMode
import com.blacksingularities.registrer.ui.theme.Palette
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Łukowe pokrętło trybów – etykiety leżą na fragmencie okręgu jak skala na pierścieniu
 * obiektywu. Przeciągnięcie obraca pierścień z wyczuwalnymi „zapadkami” (haptyka),
 * puszczenie dociąga do najbliższego trybu sprężyną.
 */
@Composable
fun ModeDial(
    modes: List<CaptureMode>,
    selected: CaptureMode,
    enabled: Boolean,
    haptics: Boolean,
    onSelect: (CaptureMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val currentSelect by rememberUpdatedState(onSelect)
    val selectedIndex = modes.indexOf(selected).coerceAtLeast(0)
    val position = remember { Animatable(selectedIndex.toFloat()) }
    var dragging by remember { mutableStateOf(false) }
    val anglePerItem = 0.30f
    val radiusPx = with(density) { 300.dp.toPx() }

    LaunchedEffect(selectedIndex) {
        if (!dragging) position.animateTo(selectedIndex.toFloat(), spring(dampingRatio = 0.78f, stiffness = 260f))
    }

    fun commit(index: Int) {
        val i = index.coerceIn(0, modes.lastIndex)
        scope.launch { position.animateTo(i.toFloat(), spring(dampingRatio = 0.78f, stiffness = 260f)) }
        if (modes[i] != selected) currentSelect(modes[i])
    }

    Box(
        modifier
            .fillMaxWidth()
            .height(58.dp)
            .pointerInput(enabled, modes) {
                if (!enabled) return@pointerInput
                var lastTick = selectedIndex
                detectHorizontalDragGestures(
                    onDragStart = { dragging = true; lastTick = position.value.roundToInt() },
                    onDragEnd = { dragging = false; commit(position.value.roundToInt()) },
                    onDragCancel = { dragging = false; commit(position.value.roundToInt()) },
                ) { change, dx ->
                    change.consume()
                    val next = (position.value - dx / (radiusPx * anglePerItem)).coerceIn(-0.45f, modes.lastIndex + 0.45f)
                    scope.launch { position.snapTo(next) }
                    val tick = next.roundToInt()
                    if (tick != lastTick) {
                        lastTick = tick
                        if (haptics) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                }
            }
            .pointerInput(enabled, modes) {
                if (!enabled) return@pointerInput
                detectTapGestures { tap ->
                    val rel = ((tap.x - size.width / 2f) / radiusPx).coerceIn(-0.99f, 0.99f)
                    val d = asin(rel) / anglePerItem
                    commit((position.value + d).roundToInt())
                }
            },
    ) {
        Canvas(Modifier.matchParentSize()) {
            // Delikatny łuk-prowadnica oraz znacznik aktualnego trybu.
            val top = 30.dp.toPx()
            drawArc(
                color = Color.White.copy(alpha = 0.07f),
                startAngle = 245f,
                sweepAngle = 50f,
                useCenter = false,
                topLeft = Offset(size.width / 2f - radiusPx, top),
                size = Size(radiusPx * 2, radiusPx * 2),
                style = Stroke(1.dp.toPx()),
            )
            drawCircle(Palette.Amber, radius = 2.5f.dp.toPx(), center = Offset(size.width / 2f, 4.dp.toPx()))
        }
        modes.forEachIndexed { i, m ->
            val d = i - position.value
            val angle = d * anglePerItem
            val dist = min(abs(d), 3f)
            val isSel = abs(d) < 0.5f
            Text(
                text = m.label.uppercase(),
                color = if (isSel) Palette.Amber else Color.White,
                fontSize = 13.sp,
                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                letterSpacing = 1.6.sp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .graphicsLayer {
                        translationX = radiusPx * sin(angle)
                        translationY = radiusPx * (1f - cos(angle)) + 14.dp.toPx()
                        rotationZ = Math.toDegrees(angle.toDouble()).toFloat()
                        alpha = (1f - dist * 0.3f).coerceIn(0f, 1f)
                        val s = 1f - dist * 0.08f
                        scaleX = s
                        scaleY = s
                    },
            )
        }
    }
}
