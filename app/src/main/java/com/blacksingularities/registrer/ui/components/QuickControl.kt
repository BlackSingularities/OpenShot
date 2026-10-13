package com.blacksingularities.registrer.ui.components

import com.blacksingularities.registrer.ui.theme.BlockShape
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blacksingularities.registrer.ui.theme.LocalHaptics
import com.blacksingularities.registrer.ui.theme.MonoDigits
import com.blacksingularities.registrer.ui.theme.Palette
import com.blacksingularities.registrer.ui.theme.glass
import com.blacksingularities.registrer.ui.theme.reactive
import kotlin.math.roundToInt

/**
 * Najważniejsze ustawienie bieżącego trybu – zawsze na ekranie. Szklana kapsuła z ikoną,
 * wartością i podziałką: przeciągnij lub dotknij podziałki, by wybrać wartość. Przytrzymanie
 * ikony pokazuje nazwę, dotknięcie ikony przywraca wartość domyślną ([defaultIndex]).
 */
@Composable
fun QuickControl(
    icon: ImageVector,
    label: String,
    options: List<String>,
    selected: Int,
    rotation: Float,
    modifier: Modifier = Modifier,
    defaultIndex: Int? = null,
    enabled: Boolean = true,
    onSelect: (Int) -> Unit,
) {
    if (options.isEmpty()) return
    val haptic = LocalHapticFeedback.current
    val haptics = LocalHaptics.current
    val select by rememberUpdatedState(onSelect)
    val current = selected.coerceIn(0, options.lastIndex)
    var dragging by remember { mutableStateOf(false) }
    val position by animateFloatAsState(
        current.toFloat(),
        spring(dampingRatio = 0.7f, stiffness = 600f),
        label = "quickPos",
    )
    val glow by animateFloatAsState(if (dragging) 1f else 0f, tween(160), label = "quickGlow")
    val isDefault = defaultIndex != null && current == defaultIndex

    Row(
        modifier
            .graphicsLayer { alpha = if (enabled) 1f else 0.4f }
            .glass(BlockShape)
            .padding(start = 4.dp, end = 14.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(34.dp)
                .reactive(label, enabled) { defaultIndex?.let { if (it != current) select(it) } }
                .glass(BlockShape, active = !isDefault),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                null,
                tint = if (isDefault) Color.White else Palette.Ink,
                modifier = Modifier.size(18.dp).graphicsLayer { rotationZ = rotation },
            )
        }
        AnimatedContent(
            targetState = current,
            transitionSpec = {
                val up = targetState > initialState
                (slideInVertically { if (up) it / 2 else -it / 2 } + fadeIn(tween(120))) togetherWith
                    (slideOutVertically { if (up) -it / 2 else it / 2 } + fadeOut(tween(90)))
            },
            label = "quickValue",
            modifier = Modifier
                .widthIn(min = 64.dp)
                .padding(horizontal = 8.dp)
                .graphicsLayer { rotationZ = rotation },
            contentAlignment = Alignment.Center,
        ) { i ->
            Text(options[i.coerceIn(0, options.lastIndex)], style = MonoDigits, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (isDefault) Color.White else Palette.Amber, maxLines = 1)
        }
        Canvas(
            Modifier
                .width(150.dp)
                .height(30.dp)
                .pointerInput(enabled, options.size) {
                    if (!enabled) return@pointerInput
                    fun indexAt(x: Float): Int {
                        val pad = 8.dp.toPx()
                        val f = ((x - pad) / (size.width - 2 * pad)).coerceIn(0f, 1f)
                        return (f * options.lastIndex).roundToInt()
                    }
                    var last = -1
                    fun pick(x: Float) {
                        val i = indexAt(x)
                        if (i != last) {
                            last = i
                            if (haptics) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            select(i)
                        }
                    }
                    detectHorizontalDragGestures(
                        onDragStart = { o -> dragging = true; last = -1; pick(o.x) },
                        onDragEnd = { dragging = false },
                        onDragCancel = { dragging = false },
                    ) { change, _ ->
                        change.consume()
                        pick(change.position.x)
                    }
                }
                .pointerInput(enabled, options.size) {
                    if (!enabled) return@pointerInput
                    detectTapGestures { o ->
                        val pad = 8.dp.toPx()
                        val f = ((o.x - pad) / (size.width - 2 * pad)).coerceIn(0f, 1f)
                        val i = (f * options.lastIndex).roundToInt()
                        if (i != current) {
                            if (haptics) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            select(i)
                        }
                    }
                },
        ) {
            val pad = 8.dp.toPx()
            val w = size.width - 2 * pad
            val cy = size.height / 2f
            val n = options.lastIndex.coerceAtLeast(1)
            val trackH = 2.dp.toPx()
            drawRoundRect(Color.White.copy(alpha = 0.16f), Offset(pad, cy - trackH / 2), Size(w, trackH), CornerRadius.Zero)
            val x = pad + w * position / n
            // wypełnienie od wartości domyślnej (albo od lewej) do bieżącej
            val from = pad + w * ((defaultIndex ?: 0).toFloat() / n)
            drawRoundRect(
                Palette.Amber.copy(alpha = 0.85f),
                Offset(minOf(from, x), cy - trackH / 2),
                Size(kotlin.math.abs(x - from), trackH),
                CornerRadius.Zero,
            )
            if (options.size <= 24) {
                for (i in 0..options.lastIndex) {
                    val tx = pad + w * i / n
                    val major = i == defaultIndex
                    drawLine(
                        Color.White.copy(alpha = if (major) 0.7f else 0.3f),
                        Offset(tx, cy + (if (major) 6 else 5).dp.toPx()),
                        Offset(tx, cy + (if (major) 10 else 8).dp.toPx()),
                        1.dp.toPx(),
                    )
                }
            }
            val th = (18f + 4f * glow).dp.toPx()
            val tw = 3.dp.toPx()
            drawRect(if (dragging) Palette.Amber else Color.White, Offset(x - tw / 2, cy - th / 2), Size(tw, th))
        }
    }
}
