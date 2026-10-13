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
            .width(220.dp)
            .height(32.dp)
            .glass(BlockShape),
    ) {
        val cx = size.width / 2f
        val pxPerDeg = size.width / 90f // widoczne ±45°
        val axis = size.height / 2f
        val start = (heading - 50f).toInt() / 5 * 5
        for (deg in start..(heading + 50f).toInt() step 5) {
            val x = cx + (deg - heading) * pxPerDeg
            if (x < 0f || x > size.width) continue
            val d = ((deg % 360) + 360) % 360
            val major = d % 45 == 0
            val fade = 1f - (abs(x - cx) / cx).coerceIn(0f, 1f) * 0.8f
            drawLine(
                Color.White.copy(alpha = (if (major) 0.9f else 0.4f) * fade),
                Offset(x, axis - (if (major) 8 else 4).dp.toPx()),
                Offset(x, axis + (if (major) 8 else 4).dp.toPx()),
                1.dp.toPx(),
            )
            if (major && abs(x - cx) > 30.dp.toPx()) {
                val name = DIRECTIONS[d / 45]
                val layout = measurer.measure(
                    name,
                    TextStyle(
                        color = (if (d == 0) Palette.Amber else Color.White).copy(alpha = fade),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                drawText(layout, topLeft = Offset(x - layout.size.width / 2f, axis - layout.size.height / 2f))
            }
        }
        // Odczyt leży dokładnie na osi kompasu, dzięki czemu cała kontrolka jest niższa.
        val label = "${heading.roundToInt() % 360}° ${directionName(heading)}" + if (unreliable) "  ⚠" else ""
        val layout = measurer.measure(
            label,
            TextStyle(color = Palette.Amber, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
        )
        val labelTop = axis - layout.size.height / 2f
        drawRect(
            Color.Black.copy(alpha = 0.72f),
            Offset(cx - layout.size.width / 2f - 5.dp.toPx(), labelTop - 2.dp.toPx()),
            Size(layout.size.width + 10.dp.toPx(), layout.size.height + 4.dp.toPx()),
        )
        drawLine(Palette.Amber, Offset(cx, 2.dp.toPx()), Offset(cx, labelTop - 2.dp.toPx()), 2.dp.toPx())
        drawLine(Palette.Amber, Offset(cx, labelTop + layout.size.height + 2.dp.toPx()), Offset(cx, size.height - 2.dp.toPx()), 2.dp.toPx())
        drawText(layout, topLeft = Offset(cx - layout.size.width / 2f, labelTop))
    }
}
