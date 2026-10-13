package com.blacksingularities.registrer.ui.components

import com.blacksingularities.registrer.ui.theme.BlockShape
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import com.blacksingularities.registrer.ui.theme.activeContent
import com.blacksingularities.registrer.ui.theme.glass
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
    // Gesty żyją dłużej niż jedna kompozycja – bieżący tryb i lista muszą być czytane przez stan,
    // inaczej porównanie odbywa się z trybem sprzed przełączenia (pokrętło pokazuje inny tryb niż aktywny).
    val currentSelected by rememberUpdatedState(selected)
    val currentModes by rememberUpdatedState(modes)
    val selectedIndex = modes.indexOf(selected).coerceAtLeast(0)
    val position = remember { Animatable(selectedIndex.toFloat()) }
    var dragging by remember { mutableStateOf(false) }
    val anglePerItem = 0.19f
    val radiusPx = with(density) { 330.dp.toPx() }

    // Po każdej zmianie trybu i po puszczeniu pokrętła – zawsze na rzeczywiście aktywnym trybie
    // (także gdy przełączenie zostało odrzucone, np. w trakcie nagrywania).
    LaunchedEffect(selectedIndex, dragging) {
        if (!dragging) position.animateTo(selectedIndex.toFloat(), spring(dampingRatio = 0.78f, stiffness = 260f))
    }

    fun commit(index: Int) {
        val list = currentModes
        val i = index.coerceIn(0, list.lastIndex)
        scope.launch { position.animateTo(i.toFloat(), spring(dampingRatio = 0.78f, stiffness = 260f)) }
        if (list[i] != currentSelected) currentSelect(list[i])
    }

    Box(
        modifier
            .fillMaxWidth()
            .height(82.dp)
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
            // Znacznik aktualnego trybu.
            drawRect(Palette.Amber, Offset(size.width / 2f - 6.dp.toPx(), 2.dp.toPx()), Size(12.dp.toPx(), 2.dp.toPx()))
        }
        modes.forEachIndexed { i, m ->
            val d = i - position.value
            val angle = d * anglePerItem
            val dist = min(abs(d), 4.5f)
            val isSel = abs(d) < 0.5f
            val focus = (1f - abs(d)).coerceIn(0f, 1f)
            val depth = (1f - dist / 5f).coerceIn(0.18f, 1f)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .graphicsLayer {
                        translationX = radiusPx * sin(angle)
                        translationY = radiusPx * (1f - cos(angle)) + 7.dp.toPx()
                        rotationZ = Math.toDegrees(angle.toDouble()).toFloat() * 0.42f
                        rotationY = -Math.toDegrees(angle.toDouble()).toFloat() * 0.72f
                        cameraDistance = 18f * density.density
                        alpha = (0.28f + depth * 0.72f).coerceIn(0f, 1f)
                        val s = 0.7f + depth * 0.3f
                        scaleX = s
                        scaleY = s
                    },
            ) {
                Box(
                    Modifier
                        .size(36.dp)
                        .graphicsLayer { scaleX = 0.92f + 0.34f * focus; scaleY = 0.92f + 0.34f * focus }
                        .glass(BlockShape, active = isSel, border = isSel),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        modeIcon(m),
                        m.label,
                        tint = if (isSel) activeContent() else Color.White.copy(alpha = 0.58f + depth * 0.3f),
                        modifier = Modifier.size(20.dp),
                    )
                }
                Text(
                    m.label.uppercase(),
                    color = if (isSel) Palette.Amber else Color.White.copy(alpha = 0.52f + depth * 0.24f),
                    fontSize = (8.2f + focus * 1.8f).sp,
                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                    letterSpacing = (0.7f + focus * 0.7f).sp,
                    modifier = Modifier.padding(top = (2f + focus * 5f).dp),
                )
            }
        }
    }
}
