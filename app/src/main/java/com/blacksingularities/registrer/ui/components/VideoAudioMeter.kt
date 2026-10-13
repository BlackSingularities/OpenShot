package com.blacksingularities.registrer.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blacksingularities.registrer.ui.theme.BlockShape
import com.blacksingularities.registrer.ui.theme.Palette
import com.blacksingularities.registrer.ui.theme.glass
import java.util.Locale
import kotlin.math.log10

/**
 * Pływający wskaźnik poziomu dźwięku dla wideo: szczyt w skali −60…0 dBFS, czerwona strefa
 * od −6 dB, znacznik opadającego szczytu i nazwa mikrofonu albo stan (wyciszony, zajęty).
 * Poziom czytany w fazie rysowania – wskaźnik nie przebudowuje reszty ekranu.
 */
@Composable
fun VideoAudioMeter(level: () -> Float, peakDb: () -> Float, note: String?, recording: Boolean) {
    val problem = note != null && (note.startsWith("wyciszony") || note.startsWith("mikrofon zajęty") || note.startsWith("błąd") || note.startsWith("nagrywanie bez"))
    Column(
        Modifier
            .width(176.dp)
            .glass(BlockShape)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (problem) Icons.Rounded.MicOff else Icons.Rounded.Mic,
                null,
                tint = if (recording && !problem) Palette.Record else Color.White,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(6.dp))
            Canvas(Modifier.weight(1f).height(8.dp)) {
                val hot = 54f / 60f
                val f = ((20f * log10(level().coerceAtLeast(1e-5f)) + 60f) / 60f).coerceIn(0f, 1f)
                val r = CornerRadius(size.height / 2f)
                drawRoundRect(Color.White.copy(alpha = 0.14f), Offset.Zero, size, r)
                val safe = minOf(f, hot)
                if (safe > 0f) drawRoundRect(Color.White, Offset.Zero, Size(size.width * safe, size.height), r)
                if (f > hot) drawRoundRect(Palette.Record, Offset(size.width * hot, 0f), Size(size.width * (f - hot), size.height), r)
                val p = ((peakDb() + 60f) / 60f).coerceIn(0f, 1f)
                if (p > 0f) drawLine(if (p > hot) Palette.Record else Color.White, Offset(size.width * p, -2f), Offset(size.width * p, size.height + 2f), 2.dp.toPx())
            }
            Spacer(Modifier.width(6.dp))
            val db = peakDb()
            Text(
                if (db <= -59f) "−∞" else String.format(Locale("pl", "PL"), "%.0f", db).replace('-', '−'),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = if (db > -6f) Palette.Record else Color.White,
                modifier = Modifier.width(26.dp),
            )
        }
        Text(
            note ?: if (recording) "Nagrywany dźwięk" else "Mikrofon",
            fontSize = 9.sp,
            color = if (problem) Palette.Record else Palette.TextDim,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}
