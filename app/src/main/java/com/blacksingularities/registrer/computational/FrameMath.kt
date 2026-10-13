package com.blacksingularities.registrer.computational

import com.blacksingularities.registrer.i18n.tr
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect
import android.os.Build
import kotlin.math.abs
import kotlin.math.max

/** Obraz luminancji w niskiej rozdzielczości – używany do wyrównywania klatek i map wag. */
class Luma(val w: Int, val h: Int, val px: ByteArray) {
    fun mean(): Float {
        var sum = 0L
        for (b in px) sum += (b.toInt() and 0xFF)
        return sum.toFloat() / max(1, px.size)
    }

    fun at(x: Int, y: Int): Int = px[y.coerceIn(0, h - 1) * w + x.coerceIn(0, w - 1)].toInt() and 0xFF
}

object FrameMath {
    fun luma(c: Int): Int =
        (((c shr 16) and 0xFF) * 77 + ((c shr 8) and 0xFF) * 150 + (c and 0xFF) * 29) shr 8

    fun sampleFor(width: Int, target: Int): Int {
        var s = 1
        while (width / (s * 2) >= target) s *= 2
        return s
    }

    fun smallLuma(bytes: ByteArray, sample: Int): Luma {
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts) ?: error(tr("Nie można zdekodować klatki", "Cannot decode the frame"))
        val w = bmp.width
        val h = bmp.height
        val px = IntArray(w * h)
        bmp.getPixels(px, 0, w, 0, 0, w, h)
        bmp.recycle()
        val out = ByteArray(w * h)
        for (i in px.indices) out[i] = luma(px[i]).toByte()
        return Luma(w, h, out)
    }

    /**
     * Szuka przesunięcia (dx, dy) minimalizującego sumę różnic bezwzględnych: piksel (x, y)
     * klatki referencyjnej odpowiada pikselowi (x + dx, y + dy) klatki [img].
     * [matchBrightness] wyrównuje średnią jasność – potrzebne przy klatkach o różnej ekspozycji.
     */
    fun estimateShift(ref: Luma, img: Luma, radius: Int, matchBrightness: Boolean): IntArray {
        if (ref.w != img.w || ref.h != img.h) return intArrayOf(0, 0)
        val gain = if (matchBrightness) (ref.mean() / max(1f, img.mean())).coerceIn(0.1f, 10f) else 1f
        val g = (gain * 256).toInt()
        val mx = radius + ref.w / 8
        val my = radius + ref.h / 8
        if (ref.w - 2 * mx <= 4 || ref.h - 2 * my <= 4) return intArrayOf(0, 0)
        var best = Long.MAX_VALUE
        var bx = 0
        var by = 0
        // Wyszukiwanie zgrubne co 2 px, potem doprecyzowanie wokół minimum.
        fun cost(dx: Int, dy: Int): Long {
            var sad = 0L
            var y = my
            while (y < ref.h - my) {
                val ro = y * ref.w
                val io = (y + dy) * img.w
                var x = mx
                while (x < ref.w - mx) {
                    val a = ref.px[ro + x].toInt() and 0xFF
                    var b = ((img.px[io + x + dx].toInt() and 0xFF) * g) shr 8
                    if (b > 255) b = 255
                    sad += abs(a - b)
                    x += 2
                }
                y += 2
            }
            return sad
        }
        var dy = -radius
        while (dy <= radius) {
            var dx = -radius
            while (dx <= radius) {
                val c = cost(dx, dy)
                if (c < best) {
                    best = c; bx = dx; by = dy
                }
                dx += 2
            }
            dy += 2
        }
        val cx = bx
        val cy = by
        for (fy in cy - 1..cy + 1) for (fx in cx - 1..cx + 1) {
            if (abs(fx) > radius || abs(fy) > radius) continue
            val c = cost(fx, fy)
            if (c < best) {
                best = c; bx = fx; by = fy
            }
        }
        return intArrayOf(bx, by)
    }

    @Suppress("DEPRECATION")
    fun regionDecoder(bytes: ByteArray): BitmapRegionDecoder =
        if (Build.VERSION.SDK_INT >= 31) {
            BitmapRegionDecoder.newInstance(bytes, 0, bytes.size)
        } else {
            BitmapRegionDecoder.newInstance(bytes, 0, bytes.size, false)
        } ?: error(tr("Nie można otworzyć klatki", "Cannot open the frame"))

    /** Dekoduje wiersze [y0, y1) pełnej szerokości do [out]. */
    fun readRows(decoder: BitmapRegionDecoder, w: Int, y0: Int, y1: Int, opts: BitmapFactory.Options, out: IntArray) {
        val bmp = decoder.decodeRegion(Rect(0, y0, w, y1), opts) ?: error(tr("Błąd dekodowania pasa", "Strip decoding error"))
        bmp.getPixels(out, 0, w, 0, 0, w, y1 - y0)
        bmp.recycle()
    }
}
