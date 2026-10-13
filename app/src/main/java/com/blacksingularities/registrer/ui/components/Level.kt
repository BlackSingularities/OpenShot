package com.blacksingularities.registrer.ui.components

import com.blacksingularities.registrer.i18n.tr
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blacksingularities.registrer.data.LevelStyle
import com.blacksingularities.registrer.ui.theme.Palette
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.roundToInt
import kotlin.math.sqrt

private const val RAD = 180f / PI.toFloat()

/** Stan grawitacji odczytany z czujnika (po filtrze i kalibracji). */
class Gravity {
    var x by mutableFloatStateOf(0f)
    var y by mutableFloatStateOf(9.8f)
    var z by mutableFloatStateOf(0f)
}

/**
 * Nasłuch czujnika grawitacji z filtrem dolnoprzepustowym. [calibration] – stałe przesunięcie
 * czujnika (m/s²) zmierzone na płaskiej powierzchni, odejmowane od każdego odczytu.
 */
@Composable
fun rememberGravity(calibration: FloatArray = FloatArray(3), smoothing: Float = 0.18f): Gravity {
    val context = LocalContext.current
    val g = remember { Gravity() }
    val cal by rememberUpdatedState(calibration)
    DisposableEffect(Unit) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sm.getDefaultSensor(Sensor.TYPE_GRAVITY) ?: sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                g.x += (e.values[0] - cal[0] - g.x) * smoothing
                g.y += (e.values[1] - cal[1] - g.y) * smoothing
                g.z += (e.values[2] - cal[2] - g.z) * smoothing
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        if (sensor != null) sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sm.unregisterListener(listener) }
    }
    return g
}

/**
 * Poziomica w czterech stylach ([LevelStyle]):
 * - Horyzont – linia z przerwą obracana grawitacją, znaczniki ekranu i punkt pochylenia,
 * - Lotniczy – sztuczny horyzont z drabinką pochylenia co 5° i łukiem przechyłu,
 * - Libelle – dwie poziomice rurkowe: przechył na dole, pochylenie z boku,
 * - Krzyż – obracany celownik z odczytem obu kątów.
 * Telefon płasko (zdjęcie z góry) – zawsze libella okrągła w dwóch osiach.
 * Wypoziomowanie w granicy [tolerance] stopni podświetla poziomicę i daje sygnał haptyczny;
 * opcjonalnie poziomica przygasa, gdy kadr jest prosty ([fadeWhenLevel]).
 */
@Composable
fun LevelOverlay(
    ratio: Float,
    haptics: Boolean,
    modifier: Modifier = Modifier,
    centered: Boolean = false,
    displayDegrees: Int = 0,
    style: LevelStyle = LevelStyle.HORIZON,
    tolerance: Float = 1f,
    showPitch: Boolean = true,
    fadeWhenLevel: Boolean = false,
    calibration: FloatArray = FloatArray(3),
) {
    val haptic = LocalHapticFeedback.current
    val measurer = rememberTextMeasurer()
    val grav = rememberGravity(calibration)
    var wasLevel by remember { mutableStateOf(false) }
    var settled by remember { mutableStateOf(false) }
    val displayOffset by rememberUpdatedState(displayDegrees.toFloat())

    val gx = grav.x
    val gy = grav.y
    val gz = grav.z
    val g = sqrt(gx * gx + gy * gy + gz * gz).coerceAtLeast(0.1f)
    val flat = abs(gz) / g > 0.88f
    val roll = atan2(gx, gy) * RAD - displayOffset
    val snapped = (roll / 90f).roundToInt() * 90f
    val deviation = roll - snapped
    val pitch = asin((gz / g).coerceIn(-1f, 1f)) * RAD
    val tiltX = asin((gx / g).coerceIn(-1f, 1f)) * RAD
    val tiltY = asin((gy / g).coerceIn(-1f, 1f)) * RAD
    val rollLevel = abs(deviation) < tolerance
    val pitchLevel = abs(pitch) < tolerance * 1.5f
    val level = if (flat) abs(tiltX) < tolerance && abs(tiltY) < tolerance else rollLevel

    LaunchedEffect(level) {
        if (level && !wasLevel && haptics) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        wasLevel = level
        settled = false
        if (level && fadeWhenLevel) {
            delay(1500)
            settled = true
        }
    }
    val fade by animateFloatAsState(if (settled && fadeWhenLevel) 0.22f else 1f, tween(400), label = "levelFade")
    val limit = if (style == LevelStyle.AVIATION) 60f else 30f
    if (!flat && abs(deviation) > limit) return

    Canvas(modifier.alpha(fade)) {
        val r = imageRect(size, ratio, centered)
        val c = r.center
        val accent = if (level) Palette.Amber else Color.White
        val shadow = Color.Black.copy(alpha = 0.35f)
        val sw = 1.5f.dp.toPx()

        if (flat) {
            drawBullseye(c, tiltX, tiltY, level, accent, shadow, sw, displayOffset, measurer)
            return@Canvas
        }
        when (style) {
            LevelStyle.HORIZON -> drawHorizon(r.width, r.height, c, roll, snapped, deviation, pitch, showPitch, pitchLevel, level, accent, shadow, sw, measurer)
            LevelStyle.AVIATION -> drawAviation(r, c, roll, snapped, deviation, pitch, showPitch, level, accent, shadow, sw, measurer)
            LevelStyle.TUBES -> drawTubes(r.width, r.height, c, snapped, deviation, pitch, showPitch, rollLevel, pitchLevel, shadow, sw, measurer)
            LevelStyle.CROSS -> drawCross(r.width, r.height, c, roll, snapped, deviation, pitch, showPitch, pitchLevel, level, accent, shadow, sw, measurer)
        }
    }
}

private fun degrees(v: Float, signed: Boolean = false): String {
    val text = String.format(Locale.US, if (signed) "%+.1f°" else "%.1f°", if (signed) v else abs(v))
    return text.let { if (com.blacksingularities.registrer.i18n.Lang.polish) it.replace('.', ',') else it }.replace('-', '−')
}

private fun DrawScope.label(measurer: TextMeasurer, text: String, color: Color, at: Offset, size: Int = 11, center: Boolean = true) {
    val layout = measurer.measure(text, TextStyle(color = color, fontSize = size.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace))
    val x = if (center) at.x - layout.size.width / 2f else at.x
    drawRoundRect(
        Color.Black.copy(alpha = 0.45f),
        Offset(x - 4.dp.toPx(), at.y - 1.dp.toPx()),
        Size(layout.size.width + 8.dp.toPx(), layout.size.height + 2.dp.toPx()),
        CornerRadius(4.dp.toPx()),
    )
    drawText(layout, topLeft = Offset(x, at.y))
}

private fun DrawScope.shadowLine(a: Offset, b: Offset, color: Color, width: Float, shadow: Color) {
    drawLine(shadow, a + Offset(0f, 1.dp.toPx()), b + Offset(0f, 1.dp.toPx()), width + 1.5f.dp.toPx())
    drawLine(color, a, b, width)
}

/** Libella okrągła dla zdjęć z góry. */
private fun DrawScope.drawBullseye(
    c: Offset, tiltX: Float, tiltY: Float, level: Boolean, accent: Color, shadow: Color, sw: Float,
    displayOffset: Float, measurer: TextMeasurer,
) {
    val ring = 34.dp.toPx()
    val maxShift = ring * 1.6f
    val bx = (-tiltX / 15f).coerceIn(-1f, 1f) * maxShift
    val by = (tiltY / 15f).coerceIn(-1f, 1f) * maxShift
    rotate(-displayOffset, c) {
        drawCircle(shadow, ring, c, style = Stroke(sw * 2))
        drawCircle(Color.White.copy(alpha = 0.8f), ring, c, style = Stroke(sw))
        drawCircle(Color.White.copy(alpha = 0.35f), ring * 2f, c, style = Stroke(sw))
        val t = 10.dp.toPx()
        drawLine(Color.White.copy(alpha = 0.5f), Offset(c.x - ring * 2f - t, c.y), Offset(c.x - ring + t * 0.6f, c.y), sw)
        drawLine(Color.White.copy(alpha = 0.5f), Offset(c.x + ring - t * 0.6f, c.y), Offset(c.x + ring * 2f + t, c.y), sw)
        drawLine(Color.White.copy(alpha = 0.5f), Offset(c.x, c.y - ring * 2f - t), Offset(c.x, c.y - ring + t * 0.6f), sw)
        drawLine(Color.White.copy(alpha = 0.5f), Offset(c.x, c.y + ring - t * 0.6f), Offset(c.x, c.y + ring * 2f + t), sw)
        val bubble = Offset(c.x + bx, c.y + by)
        drawCircle(accent.copy(alpha = if (level) 0.9f else 0.55f), 12.dp.toPx(), bubble)
        drawCircle(accent, 12.dp.toPx(), bubble, style = Stroke(sw))
        label(measurer, "${degrees(tiltX)} · ${degrees(tiltY)}", accent, Offset(c.x, c.y + ring * 2f + 14.dp.toPx()))
    }
}

/** Linia horyzontu z przerwą, znaczniki ekranu i punkt pochylenia. */
private fun DrawScope.drawHorizon(
    w: Float, h: Float, c: Offset, roll: Float, snapped: Float, deviation: Float, pitch: Float, showPitch: Boolean,
    pitchLevel: Boolean, level: Boolean, accent: Color, shadow: Color, sw: Float, measurer: TextMeasurer,
) {
    val half = minOf(w, h) * 0.30f
    val gap = 18.dp.toPx()
    rotate(snapped, c) {
        for (sign in listOf(-1f, 1f)) {
            shadowLine(Offset(c.x + sign * (half + 8.dp.toPx()), c.y), Offset(c.x + sign * (half + 22.dp.toPx()), c.y), Color.White.copy(alpha = 0.7f), sw, shadow)
        }
    }
    rotate(roll, c) {
        val lw = if (level) 2.5f.dp.toPx() else sw
        for (sign in listOf(-1f, 1f)) shadowLine(Offset(c.x + sign * gap, c.y), Offset(c.x + sign * half, c.y), accent, lw, shadow)
    }
    rotate(snapped, c) {
        if (showPitch) {
            val shift = (pitch * 2.dp.toPx()).coerceIn(-40.dp.toPx(), 40.dp.toPx())
            val p = Offset(c.x, c.y - shift)
            drawCircle(shadow, 4.dp.toPx(), p + Offset(0f, 1.dp.toPx()))
            drawCircle(if (pitchLevel) Palette.Amber else Color.White, 3.5f.dp.toPx(), p)
        }
        val text = if (showPitch) "${degrees(deviation)} · ${degrees(pitch, true)}" else degrees(deviation)
        label(measurer, text, accent, Offset(c.x, c.y + 12.dp.toPx()))
    }
}

/** Sztuczny horyzont: drabinka pochylenia przesuwana i obracana z telefonem, łuk przechyłu u góry. */
private fun DrawScope.drawAviation(
    r: androidx.compose.ui.geometry.Rect, c: Offset, roll: Float, snapped: Float, deviation: Float, pitch: Float,
    showPitch: Boolean, level: Boolean, accent: Color, shadow: Color, sw: Float, measurer: TextMeasurer,
) {
    val pxPerDeg = 5.dp.toPx()
    val span = minOf(r.width, r.height) * 0.42f
    clipRect(r.left, r.top, r.right, r.bottom) {
        rotate(roll, c) {
            translate(0f, if (showPitch) pitch * pxPerDeg else 0f) {
                shadowLine(Offset(c.x - span * 1.6f, c.y), Offset(c.x - 28.dp.toPx(), c.y), accent, if (level) 2.5f.dp.toPx() else sw, shadow)
                shadowLine(Offset(c.x + 28.dp.toPx(), c.y), Offset(c.x + span * 1.6f, c.y), accent, if (level) 2.5f.dp.toPx() else sw, shadow)
                if (showPitch) {
                    for (step in listOf(-30, -20, -15, -10, -5, 5, 10, 15, 20, 30)) {
                        val y = c.y - step * pxPerDeg
                        val major = step % 10 == 0
                        val len = if (major) span * 0.45f else span * 0.22f
                        val col = Color.White.copy(alpha = if (major) 0.85f else 0.55f)
                        shadowLine(Offset(c.x - len, y), Offset(c.x - len * 0.25f, y), col, sw, shadow)
                        shadowLine(Offset(c.x + len * 0.25f, y), Offset(c.x + len, y), col, sw, shadow)
                        if (major) label(measurer, "${abs(step)}", col, Offset(c.x + len + 6.dp.toPx(), y - 7.dp.toPx()), 9, center = false)
                    }
                }
            }
        }
    }
    rotate(snapped, c) {
        // Stały symbol „samolotu” w środku.
        val wing = 26.dp.toPx()
        val drop = 7.dp.toPx()
        val path = Path().apply {
            moveTo(c.x - wing * 1.4f, c.y)
            lineTo(c.x - wing * 0.45f, c.y)
            lineTo(c.x, c.y + drop)
            lineTo(c.x + wing * 0.45f, c.y)
            lineTo(c.x + wing * 1.4f, c.y)
        }
        drawPath(path, shadow, style = Stroke(sw * 3))
        drawPath(path, Palette.Amber, style = Stroke(2.dp.toPx()))
        // Łuk przechyłu z podziałką i wskaźnikiem.
        val arcR = span * 0.95f
        drawArc(
            Color.White.copy(alpha = 0.5f), 210f, 120f, false,
            topLeft = Offset(c.x - arcR, c.y - arcR), size = Size(arcR * 2, arcR * 2), style = Stroke(sw),
        )
        for (t in listOf(-60, -45, -30, -20, -10, 0, 10, 20, 30, 45, 60)) {
            val len = if (t % 30 == 0) 12.dp.toPx() else if (t % 45 == 0) 10.dp.toPx() else 6.dp.toPx()
            rotate(t.toFloat(), c) {
                shadowLine(Offset(c.x, c.y - arcR), Offset(c.x, c.y - arcR + len), Color.White.copy(alpha = 0.8f), sw, shadow)
            }
        }
        rotate(deviation, c) {
            val tip = Offset(c.x, c.y - arcR + 14.dp.toPx())
            val tri = Path().apply {
                moveTo(tip.x, tip.y)
                lineTo(tip.x - 6.dp.toPx(), tip.y + 10.dp.toPx())
                lineTo(tip.x + 6.dp.toPx(), tip.y + 10.dp.toPx())
                close()
            }
            drawPath(tri, accent)
        }
        val text = if (showPitch) tr("P ${degrees(deviation)} · N ${degrees(pitch, true)}", "R ${degrees(deviation)} · P ${degrees(pitch, true)}") else tr("P ${degrees(deviation)}", "R ${degrees(deviation)}")
        label(measurer, text, accent, Offset(c.x, c.y + arcR * 0.45f))
    }
}

/** Dwie poziomice rurkowe: przechył (pozioma, na dole) i pochylenie (pionowa, z boku). */
private fun DrawScope.drawTubes(
    w: Float, h: Float, c: Offset, snapped: Float, deviation: Float, pitch: Float, showPitch: Boolean,
    rollLevel: Boolean, pitchLevel: Boolean, shadow: Color, sw: Float, measurer: TextMeasurer,
) {
    val swap = abs(snapped % 180f) == 90f
    val vw = if (swap) h else w
    val vh = if (swap) w else h
    rotate(snapped, c) {
        fun tube(center: Offset, horizontal: Boolean, value: Float, ok: Boolean, text: String) {
            val len = (if (horizontal) vw else vh) * 0.42f
            val th = 20.dp.toPx()
            val size = if (horizontal) Size(len, th) else Size(th, len)
            val tl = Offset(center.x - size.width / 2f, center.y - size.height / 2f)
            drawRoundRect(Color.Black.copy(alpha = 0.4f), tl, size, CornerRadius(th / 2f))
            drawRoundRect(Color.White.copy(alpha = 0.75f), tl, size, CornerRadius(th / 2f), style = Stroke(sw))
            val mark = 12.dp.toPx()
            for (sign in listOf(-1f, 1f)) {
                if (horizontal) {
                    drawLine(Color.White.copy(alpha = 0.7f), Offset(center.x + sign * mark, tl.y), Offset(center.x + sign * mark, tl.y + th), sw)
                } else {
                    drawLine(Color.White.copy(alpha = 0.7f), Offset(tl.x, center.y + sign * mark), Offset(tl.x + th, center.y + sign * mark), sw)
                }
            }
            val travel = len / 2f - th / 2f
            val shift = (value / 10f).coerceIn(-1f, 1f) * travel
            val bubble = if (horizontal) Offset(center.x - shift, center.y) else Offset(center.x, center.y - shift)
            val col = if (ok) Palette.Amber else Color.White.copy(alpha = 0.6f)
            drawCircle(col.copy(alpha = 0.85f), th * 0.36f, bubble)
            drawCircle(Color.White, th * 0.36f, bubble, style = Stroke(sw))
            val at = if (horizontal) Offset(center.x, center.y + th / 2f + 4.dp.toPx()) else Offset(center.x - th / 2f - 4.dp.toPx(), center.y - 7.dp.toPx())
            label(measurer, text, if (ok) Palette.Amber else Color.White, at, 10, center = horizontal)
        }
        tube(Offset(c.x, c.y + vh * 0.32f), true, deviation, rollLevel, degrees(deviation))
        if (showPitch) tube(Offset(c.x + vw * 0.40f, c.y), false, pitch, pitchLevel, degrees(pitch, true))
    }
}

/** Celownik obracany z telefonem względem stałego krzyża ekranu. */
private fun DrawScope.drawCross(
    w: Float, h: Float, c: Offset, roll: Float, snapped: Float, deviation: Float, pitch: Float, showPitch: Boolean,
    pitchLevel: Boolean, level: Boolean, accent: Color, shadow: Color, sw: Float, measurer: TextMeasurer,
) {
    val arm = minOf(w, h) * 0.22f
    val gap = 10.dp.toPx()
    rotate(snapped, c) {
        drawCircle(Color.White.copy(alpha = 0.45f), arm * 0.55f, c, style = Stroke(sw))
    }
    rotate(roll, c) {
        val lw = if (level) 2.5f.dp.toPx() else sw
        shadowLine(Offset(c.x - arm, c.y), Offset(c.x - gap, c.y), accent, lw, shadow)
        shadowLine(Offset(c.x + gap, c.y), Offset(c.x + arm, c.y), accent, lw, shadow)
        shadowLine(Offset(c.x, c.y - arm * 0.6f), Offset(c.x, c.y - gap), accent, lw, shadow)
        shadowLine(Offset(c.x, c.y + gap), Offset(c.x, c.y + arm * 0.6f), accent, lw, shadow)
    }
    rotate(snapped, c) {
        if (showPitch) {
            val shift = (pitch * 2.dp.toPx()).coerceIn(-arm * 0.5f, arm * 0.5f)
            drawCircle(if (pitchLevel) Palette.Amber else Color.White, 4.dp.toPx(), Offset(c.x, c.y - shift), style = Stroke(2.dp.toPx()))
        }
        val text = if (showPitch) "${degrees(deviation)} · ${degrees(pitch, true)}" else degrees(deviation)
        label(measurer, text, accent, Offset(c.x, c.y + arm * 0.6f + 8.dp.toPx()))
    }
}
