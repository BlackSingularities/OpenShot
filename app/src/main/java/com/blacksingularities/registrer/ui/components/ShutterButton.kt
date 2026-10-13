package com.blacksingularities.registrer.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.blacksingularities.registrer.camera.CaptureMode
import com.blacksingularities.registrer.ui.theme.Palette

/**
 * Migawka morfująca kształt i kolor zależnie od trybu: krąg (foto), księżyc (noc),
 * czerwony punkt → zaokrąglony kwadrat (nagrywanie), bursztyn (audio). Pierścień postępu
 * pokazuje przetwarzanie wieloklatkowe.
 */
@Composable
fun ShutterButton(
    mode: CaptureMode,
    active: Boolean,
    progress: Float?,
    processing: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    counter: Int? = null,
    onLongPress: () -> Unit = {},
    onRelease: () -> Unit = {},
    /** Tryb nagrywa wideo (także tryb ręczny w wariancie wideo) – czerwony środek. */
    video: Boolean = false,
) {
    var pressed by remember { mutableStateOf(false) }
    val click by rememberUpdatedState(onClick)
    val longPress by rememberUpdatedState(onLongPress)
    val release by rememberUpdatedState(onRelease)
    val measurer = rememberTextMeasurer()
    val scale by animateFloatAsState(if (pressed) 0.88f else 1f, spring(dampingRatio = 0.5f, stiffness = 600f), label = "scale")
    val inner by animateColorAsState(
        when {
            video -> Palette.Record
            mode == CaptureMode.VIDEO || mode == CaptureMode.SLOWMO || mode == CaptureMode.TIMELAPSE -> Palette.Record
            mode == CaptureMode.AUDIO -> Palette.Amber
            mode == CaptureMode.NIGHT -> Palette.Moon
            mode == CaptureMode.PORTRAIT -> Color(0xFFFFE2C4)
            else -> Color.White
        },
        tween(350),
        label = "inner",
    )
    val corner by animateFloatAsState(if (active) 0.32f else 1f, spring(dampingRatio = 0.7f, stiffness = 300f), label = "corner")
    val innerSize by animateFloatAsState(if (active) 0.42f else 0.8f, spring(dampingRatio = 0.6f, stiffness = 300f), label = "size")
    val crescent by animateFloatAsState(if (mode == CaptureMode.NIGHT && !active) 1f else 0f, tween(400), label = "moon")
    val spin = rememberInfiniteTransition(label = "spin")
    val sweepStart by spin.animateFloat(0f, 360f, infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart), label = "start")

    Canvas(
        modifier
            .size(74.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (enabled) 1f else 0.45f }
            .semantics { contentDescription = if (active) "Zatrzymaj" else "Wyzwól migawkę" }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onPress = {
                        pressed = true
                        tryAwaitRelease()
                        pressed = false
                        release()
                    },
                    onLongPress = { longPress() },
                    onTap = { click() },
                )
            },
    ) {
        val ringWidth = 3.dp.toPx()
        val radius = size.minDimension / 2f - ringWidth
        drawCircle(Color.White.copy(alpha = 0.92f), radius = radius, style = Stroke(ringWidth))

        val s = size.minDimension * innerSize
        val tl = Offset((size.width - s) / 2f, (size.height - s) / 2f)
        drawRoundRect(inner, tl, Size(s, s), CornerRadius(s / 2f * corner))
        if (crescent > 0.01f) {
            val r = s / 2f
            drawCircle(
                Palette.Ink.copy(alpha = 0.92f * crescent),
                radius = r * 0.78f,
                center = Offset(center.x + r * 0.42f, center.y - r * 0.3f),
            )
        }

        val arcInset = ringWidth / 2f
        val arcSize = Size(size.width - 2 * arcInset, size.height - 2 * arcInset)
        if (counter != null) {
            val layout = measurer.measure(
                counter.toString(),
                TextStyle(color = Palette.Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold),
            )
            drawText(layout, topLeft = Offset(center.x - layout.size.width / 2f, center.y - layout.size.height / 2f))
        }
        if (progress != null) {
            drawArc(
                Palette.Amber, -90f, 360f * progress.coerceIn(0f, 1f), false,
                topLeft = Offset(arcInset, arcInset), size = arcSize,
                style = Stroke(ringWidth, cap = StrokeCap.Round),
            )
        } else if (processing) {
            drawArc(
                Palette.Amber, sweepStart, 90f, false,
                topLeft = Offset(arcInset, arcInset), size = arcSize,
                style = Stroke(ringWidth, cap = StrokeCap.Round),
            )
        }
    }
}
