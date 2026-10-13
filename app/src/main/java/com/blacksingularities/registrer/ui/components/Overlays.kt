package com.blacksingularities.registrer.ui.components

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.blacksingularities.registrer.data.GridType
import com.blacksingularities.registrer.ui.theme.Palette
import com.blacksingularities.registrer.ui.theme.glass
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Prostokąt obrazu przy skalowaniu FIT_START (wyrównanie do góry, wyśrodkowanie w poziomie)
 * albo FIT_CENTER, gdy [centered].
 */
fun imageRect(container: Size, ratio: Float, centered: Boolean = false): Rect {
    if (container.width <= 0f || container.height <= 0f) return Rect.Zero
    var w = container.width
    var h = w / ratio
    if (h > container.height) {
        h = container.height
        w = h * ratio
    }
    val left = (container.width - w) / 2f
    val top = if (centered) (container.height - h) / 2f else 0f
    return Rect(left, top, left + w, top + h)
}

/**
 * Siatki kompozycyjne nakładane jednocześnie. Każda ma własny styl, by dało się je rozróżnić:
 * 3×3 – biała linia, złoty podział – bursztynowa przerywana, kwadrat – ramka z krzyżykiem,
 * przekątne – delikatne. Pod każdą linią cień, by była widoczna także na jasnym niebie.
 */
@Composable
fun GridOverlay(types: Set<GridType>, ratio: Float, modifier: Modifier = Modifier, centered: Boolean = false) {
    if (types.isEmpty()) return
    Canvas(modifier) {
        val r = imageRect(size, ratio, centered)
        val sw = 1.dp.toPx()
        val shadow = Color.Black.copy(alpha = 0.28f)
        fun line(a: Offset, b: Offset, color: Color, effect: PathEffect? = null) {
            drawLine(shadow, a + Offset(0f, sw), b + Offset(0f, sw), sw * 1.6f, pathEffect = effect)
            drawLine(color, a, b, sw, pathEffect = effect)
        }
        fun v(fx: Float, c: Color, e: PathEffect? = null) = line(Offset(r.left + r.width * fx, r.top), Offset(r.left + r.width * fx, r.bottom), c, e)
        fun h(fy: Float, c: Color, e: PathEffect? = null) = line(Offset(r.left, r.top + r.height * fy), Offset(r.right, r.top + r.height * fy), c, e)

        if (GridType.DIAGONAL in types) {
            val c = Color.White.copy(alpha = 0.22f)
            line(r.topLeft, r.bottomRight, c)
            line(Offset(r.right, r.top), Offset(r.left, r.bottom), c)
        }
        if (GridType.THIRDS in types) {
            val c = Color.White.copy(alpha = 0.42f)
            v(1 / 3f, c); v(2 / 3f, c); h(1 / 3f, c); h(2 / 3f, c)
        }
        if (GridType.GOLDEN in types) {
            val c = Palette.Amber.copy(alpha = 0.75f)
            val dash = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))
            val g = 0.382f
            v(g, c, dash); v(1 - g, c, dash); h(g, c, dash); h(1 - g, c, dash)
        }
        if (GridType.SQUARE in types) {
            val c = Color.White.copy(alpha = 0.6f)
            val side = minOf(r.width, r.height)
            val top = r.top + (r.height - side) / 2f
            val left = r.left + (r.width - side) / 2f
            drawRect(shadow, Offset(left, top + sw), Size(side, side), style = Stroke(sw * 1.6f))
            drawRect(c, Offset(left, top), Size(side, side), style = Stroke(sw))
            val cx = left + side / 2
            val cy = top + side / 2
            val arm = 10.dp.toPx()
            line(Offset(cx, cy - arm), Offset(cx, cy + arm), c)
            line(Offset(cx - arm, cy), Offset(cx + arm, cy), c)
        }
    }
}

@Composable
fun HistogramView(
    bins: IntArray,
    modifier: Modifier = Modifier,
    mode: com.blacksingularities.registrer.data.HistogramMode = com.blacksingularities.registrer.data.HistogramMode.LUMA,
    histogramSize: com.blacksingularities.registrer.data.HistogramSize = com.blacksingularities.registrer.data.HistogramSize.MEDIUM,
) {
    Canvas(
        modifier
            .size(width = histogramSize.widthDp.dp, height = histogramSize.heightDp.dp)
            .glass(RoundedCornerShape(12.dp)),
    ) {
        fun channel(offset: Int): IntArray =
            if (bins.size >= offset + 64) bins.copyOfRange(offset, offset + 64) else bins.copyOfRange(0, minOf(64, bins.size))
        fun curve(data: IntArray, color: Color) {
            val maxV = data.maxOrNull()?.coerceAtLeast(1) ?: 1
            val path = Path()
            val bw = size.width / data.size
            path.moveTo(0f, size.height)
            data.forEachIndexed { i, v ->
                val hgt = sqrt(v.toFloat() / maxV) * (size.height - 6.dp.toPx())
                path.lineTo(i * bw + bw / 2f, size.height - hgt)
            }
            path.lineTo(size.width, size.height)
            path.close()
            drawPath(path, color)
        }
        val luma = channel(0)
        if (mode == com.blacksingularities.registrer.data.HistogramMode.RGB && bins.size >= 256) {
            curve(channel(64), Color(0x99FF4D4D))
            curve(channel(128), Color(0x994DFF6A))
            curve(channel(192), Color(0x994D8BFF))
        } else {
            curve(luma, Color.White.copy(alpha = 0.6f))
        }
        // Prześwietlenia (prawa krawędź) i zgubione cienie (lewa).
        val total = luma.sum().coerceAtLeast(1)
        if (luma.last() + luma[luma.size - 2] > total * 0.04f) {
            drawRect(Color.White, Offset(size.width - 3.dp.toPx(), 0f), Size(3.dp.toPx(), size.height))
        }
        if (luma[0] + luma[1] > total * 0.08f) {
            drawRect(Color.Gray, Offset(0f, 0f), Size(3.dp.toPx(), size.height))
        }
    }
}

/**
 * Pierścień ostrości z wbudowanym suwakiem ekspozycji: przeciągnięcie w pionie tuż obok
 * pierścienia zmienia EV, słoneczko pokazuje wartość.
 */
@Composable
fun FocusRing(
    point: Offset,
    trigger: Int,
    evIndex: Int,
    evMin: Int,
    evMax: Int,
    onEv: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val anim = remember { Animatable(0f) }
    LaunchedEffect(trigger) {
        anim.snapTo(0f)
        anim.animateTo(1f, tween(380))
    }
    val ringSize = 76.dp
    val sliderH = 150.dp
    val ringPx = with(density) { ringSize.toPx() }
    val sliderHPx = with(density) { sliderH.toPx() }
    var dragAcc by remember { mutableFloatStateOf(0f) }
    val currentEv by rememberUpdatedState(evIndex)
    val evCallback by rememberUpdatedState(onEv)

    Box(modifier) {
        Canvas(
            Modifier
                .offset { IntOffset((point.x - ringPx / 2).roundToInt(), (point.y - ringPx / 2).roundToInt()) }
                .size(ringSize),
        ) {
            val s = 1.35f - 0.35f * anim.value
            val r = size.minDimension / 2f * s - 2.dp.toPx()
            drawCircle(Palette.Amber.copy(alpha = 0.95f), r, style = Stroke(1.6f.dp.toPx()))
            drawCircle(Palette.Amber, 2.dp.toPx())
        }
        if (evMax > evMin) {
            Canvas(
                Modifier
                    .offset {
                        IntOffset((point.x + ringPx / 2 + 6.dp.toPx()).roundToInt(), (point.y - sliderHPx / 2).roundToInt())
                    }
                    .size(width = 40.dp, height = sliderH)
                    .pointerInput(evMin, evMax) {
                        detectVerticalDragGestures(onDragStart = { dragAcc = 0f }) { change, dy ->
                            change.consume()
                            dragAcc -= dy
                            val stepPx = size.height / (evMax - evMin).toFloat()
                            if (abs(dragAcc) >= stepPx) {
                                val steps = (dragAcc / stepPx).toInt()
                                dragAcc -= steps * stepPx
                                evCallback(currentEv + steps)
                            }
                        }
                    },
            ) {
                val cx = size.width / 2f
                val frac = (evIndex - evMin).toFloat() / (evMax - evMin)
                val y = size.height * (1f - frac)
                drawLine(Color.White.copy(alpha = 0.35f), Offset(cx, 0f), Offset(cx, (y - 12.dp.toPx()).coerceAtLeast(0f)), 1.5f.dp.toPx())
                drawLine(Color.White.copy(alpha = 0.35f), Offset(cx, (y + 12.dp.toPx()).coerceAtMost(size.height)), Offset(cx, size.height), 1.5f.dp.toPx())
                drawCircle(Palette.Amber, 6.dp.toPx(), Offset(cx, y))
                for (k in 0 until 8) {
                    val a = k * (PI.toFloat() / 4f)
                    val inner = 8.5f.dp.toPx()
                    val outer = 11.5f.dp.toPx()
                    drawLine(
                        Palette.Amber,
                        Offset(cx + inner * kotlin.math.cos(a), y + inner * kotlin.math.sin(a)),
                        Offset(cx + outer * kotlin.math.cos(a), y + outer * kotlin.math.sin(a)),
                        1.5f.dp.toPx(),
                    )
                }
            }
        }
    }
}
