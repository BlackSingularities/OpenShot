package com.blacksingularities.registrer.ui.components

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blacksingularities.registrer.ui.theme.BlockShape
import com.blacksingularities.registrer.ui.theme.Palette
import com.blacksingularities.registrer.ui.theme.glass
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.roundToInt
import kotlin.math.sqrt

private const val RAD = 180f / PI.toFloat()

/**
 * Poziomica.
 *
 * Telefon trzymany pionowo/poziomo: linia horyzontu (z przerwą w środku) obraca się
 * zgodnie z grawitacją względem stałych znaczników ekranu, obok odczyt odchylenia w stopniach,
 * a punkt w środku pokazuje pochylenie przód/tył. Zrównanie (±1°) – bursztyn i haptyka.
 *
 * Telefon płasko (zdjęcie z góry): libella – pęcherzyk w kole, wypoziomowanie w obu osiach.
 */
@Composable
fun LevelOverlay(
    ratio: Float,
    haptics: Boolean,
    modifier: Modifier = Modifier,
    centered: Boolean = false,
    displayDegrees: Int = 0,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val measurer = rememberTextMeasurer()
    var gx by remember { mutableFloatStateOf(0f) }
    var gy by remember { mutableFloatStateOf(9.8f) }
    var gz by remember { mutableFloatStateOf(0f) }
    var wasLevel by remember { mutableStateOf(false) }
    val displayOffset by rememberUpdatedState(displayDegrees.toFloat())

    DisposableEffect(Unit) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sm.getDefaultSensor(Sensor.TYPE_GRAVITY) ?: sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                // Filtr dolnoprzepustowy – spokojna linia bez drgań.
                gx += (e.values[0] - gx) * 0.18f
                gy += (e.values[1] - gy) * 0.18f
                gz += (e.values[2] - gz) * 0.18f
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        if (sensor != null) sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sm.unregisterListener(listener) }
    }

    val g = sqrt(gx * gx + gy * gy + gz * gz).coerceAtLeast(0.1f)
    val flat = abs(gz) / g > 0.88f
    // Obrót wokół osi aparatu (w układzie ekranu) i pochylenie przód/tył.
    val roll = atan2(gx, gy) * RAD - displayOffset
    val snapped = (roll / 90f).roundToInt() * 90f
    val deviation = roll - snapped
    val pitch = asin((gz / g).coerceIn(-1f, 1f)) * RAD
    val tiltX = asin((gx / g).coerceIn(-1f, 1f)) * RAD
    val tiltY = asin((gy / g).coerceIn(-1f, 1f)) * RAD
    val level = if (flat) abs(tiltX) < 1f && abs(tiltY) < 1f else abs(deviation) < 1f

    LaunchedEffect(level) {
        if (level && !wasLevel && haptics) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        wasLevel = level
    }
    if (!flat && abs(deviation) > 30f) return

    Canvas(modifier) {
        val r = imageRect(size, ratio, centered)
        val c = r.center
        val accent = if (level) Palette.Amber else Color.White
        val shadow = Color.Black.copy(alpha = 0.35f)
        val sw = 1.5f.dp.toPx()

        if (flat) {
            // Libella: stałe koło + pęcherzyk przesunięty o pochylenie w obu osiach.
            val ring = 34.dp.toPx()
            val maxShift = ring * 1.6f
            val bx = (-tiltX / 15f).coerceIn(-1f, 1f) * maxShift
            val by = (tiltY / 15f).coerceIn(-1f, 1f) * maxShift
            rotate(-displayOffset, c) {
                drawCircle(shadow, ring, c, style = Stroke(sw * 2))
                drawCircle(Color.White.copy(alpha = 0.8f), ring, c, style = Stroke(sw))
                drawLine(Color.White.copy(alpha = 0.5f), Offset(c.x - ring - 10.dp.toPx(), c.y), Offset(c.x - ring + 6.dp.toPx(), c.y), sw)
                drawLine(Color.White.copy(alpha = 0.5f), Offset(c.x + ring - 6.dp.toPx(), c.y), Offset(c.x + ring + 10.dp.toPx(), c.y), sw)
                drawLine(Color.White.copy(alpha = 0.5f), Offset(c.x, c.y - ring - 10.dp.toPx()), Offset(c.x, c.y - ring + 6.dp.toPx()), sw)
                drawLine(Color.White.copy(alpha = 0.5f), Offset(c.x, c.y + ring - 6.dp.toPx()), Offset(c.x, c.y + ring + 10.dp.toPx()), sw)
                val bubble = Offset(c.x + bx, c.y + by)
                drawCircle(accent.copy(alpha = if (level) 0.9f else 0.55f), 12.dp.toPx(), bubble)
                drawCircle(accent, 12.dp.toPx(), bubble, style = Stroke(sw))
            }
            return@Canvas
        }

        val half = minOf(r.width, r.height) * 0.30f
        val gap = 18.dp.toPx()
        // Stałe znaczniki ekranu (poziom docelowy).
        rotate(snapped, c) {
            for (sign in listOf(-1f, 1f)) {
                val a = Offset(c.x + sign * (half + 8.dp.toPx()), c.y)
                val b = Offset(c.x + sign * (half + 22.dp.toPx()), c.y)
                drawLine(shadow, a + Offset(0f, 1.dp.toPx()), b + Offset(0f, 1.dp.toPx()), sw * 2)
                drawLine(Color.White.copy(alpha = 0.7f), a, b, sw)
            }
        }
        // Linia horyzontu z przerwą w środku.
        rotate(roll, c) {
            val w = if (level) 2.5f.dp.toPx() else sw
            for (sign in listOf(-1f, 1f)) {
                val a = Offset(c.x + sign * gap, c.y)
                val b = Offset(c.x + sign * half, c.y)
                drawLine(shadow, a + Offset(0f, 1.dp.toPx()), b + Offset(0f, 1.dp.toPx()), w + sw)
                drawLine(accent, a, b, w)
            }
        }
        // Pochylenie przód/tył: punkt przesuwa się wzdłuż osi „góra–dół” ekranu.
        rotate(snapped, c) {
            val shift = (pitch * 2.dp.toPx()).coerceIn(-40.dp.toPx(), 40.dp.toPx())
            val p = Offset(c.x, c.y - shift)
            val pitchLevel = abs(pitch) < 1.5f
            drawCircle(shadow, 4.dp.toPx(), p + Offset(0f, 1.dp.toPx()))
            drawCircle(if (pitchLevel) Palette.Amber else Color.White, 3.5f.dp.toPx(), p)
            // Odczyt odchylenia w stopniach, czytelny w orientacji trzymania.
            val text = String.format(Locale.US, "%.1f°", abs(deviation)).replace('.', ',')
            val layout = measurer.measure(
                text,
                TextStyle(color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
            )
            drawText(layout, topLeft = Offset(c.x - layout.size.width / 2f, c.y + 10.dp.toPx()))
        }
    }
}

private val DIRECTIONS = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")

fun directionName(deg: Float): String = DIRECTIONS[(((deg + 22.5f) % 360f + 360f) % 360f / 45f).toInt() % 8]

/**
 * Kompas: kierunek, w którym patrzy aparat (oś obiektywu), niezależnie od trzymania telefonu.
 * Gdy aparat celuje prosto w dół lub w górę – kierunek górnej krawędzi telefonu.
 * Północ magnetyczna (bez korekty deklinacji – wymagałaby lokalizacji).
 */
@Composable
fun CompassTape(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val measurer = rememberTextMeasurer()
    var heading by remember { mutableFloatStateOf(Float.NaN) }
    var unreliable by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: sm.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR)
        val rm = FloatArray(9)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rm, e.values)
                // Oś aparatu = −Z urządzenia; kolumny macierzy to osie urządzenia w układzie świata
                // (X – wschód, Y – północ, Z – góra).
                var fx = -rm[2]
                var fy = -rm[5]
                if (sqrt(fx * fx + fy * fy) < 0.25f) {
                    fx = rm[1]
                    fy = rm[4]
                }
                val h = ((atan2(fx, fy) * RAD) + 360f) % 360f
                heading = if (heading.isNaN()) {
                    h
                } else {
                    var d = h - heading
                    if (d > 180f) d -= 360f
                    if (d < -180f) d += 360f
                    (heading + d * 0.15f + 360f) % 360f
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                unreliable = accuracy <= SensorManager.SENSOR_STATUS_ACCURACY_LOW
            }
        }
        if (sensor != null) sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        onDispose { sm.unregisterListener(listener) }
    }
    if (heading.isNaN()) return

    Canvas(
        modifier
            .width(240.dp)
            .height(46.dp)
            .glass(BlockShape),
    ) {
        val cx = size.width / 2f
        val pxPerDeg = size.width / 90f // widoczne ±45°
        val base = size.height - 6.dp.toPx()
        val start = (heading - 50f).toInt() / 5 * 5
        for (deg in start..(heading + 50f).toInt() step 5) {
            val x = cx + (deg - heading) * pxPerDeg
            if (x < 0f || x > size.width) continue
            val d = ((deg % 360) + 360) % 360
            val major = d % 45 == 0
            val fade = 1f - (abs(x - cx) / cx).coerceIn(0f, 1f) * 0.8f
            drawLine(
                Color.White.copy(alpha = (if (major) 0.9f else 0.4f) * fade),
                Offset(x, base),
                Offset(x, base - (if (major) 9 else 5).dp.toPx()),
                1.dp.toPx(),
            )
            if (major) {
                val name = DIRECTIONS[d / 45]
                val layout = measurer.measure(
                    name,
                    TextStyle(
                        color = (if (d == 0) Palette.Amber else Color.White).copy(alpha = fade),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                drawText(layout, topLeft = Offset(x - layout.size.width / 2f, base - 11.dp.toPx() - layout.size.height))
            }
        }
        // Wskaźnik środka i odczyt.
        drawLine(Palette.Amber, Offset(cx, base + 3.dp.toPx()), Offset(cx, base - 12.dp.toPx()), 2.dp.toPx())
        val label = "${heading.roundToInt() % 360}° ${directionName(heading)}" + if (unreliable) "  ⚠" else ""
        val layout = measurer.measure(
            label,
            TextStyle(color = Palette.Amber, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
        )
        drawRect(Color.Black.copy(alpha = 0.55f), Offset(cx - layout.size.width / 2f - 4.dp.toPx(), 2.dp.toPx()), Size(layout.size.width + 8.dp.toPx(), layout.size.height.toFloat()))
        drawText(layout, topLeft = Offset(cx - layout.size.width / 2f, 2.dp.toPx()))
    }
}
