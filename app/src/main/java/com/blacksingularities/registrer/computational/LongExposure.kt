package com.blacksingularities.registrer.computational

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Długi czas naświetlania z serii zwykłych klatek – jak filtr ND, ale bez statywu i filtra:
 * - uśrednianie: ruch (woda, chmury, przechodnie) rozmywa się, nieruchome tło zostaje ostre,
 * - rozjaśnianie (maksimum): światła zostawiają smugi (samochody, gwiazdy, ogień).
 * Klatki mogą być wyrównane do pierwszej (zdjęcie z ręki). Przetwarzanie pasami, jak w trybie nocnym.
 */
object LongExposure {

    suspend fun blend(
        frames: List<ByteArray>,
        lighten: Boolean,
        align: Boolean,
        onProgress: (Float) -> Unit,
    ): Bitmap = withContext(Dispatchers.Default) {
        require(frames.isNotEmpty())
        val decoders = frames.map { FrameMath.regionDecoder(it) }
        try {
            val w = decoders[0].width
            val h = decoders[0].height
            val shifts = Array(frames.size) { IntArray(2) }
            val usable = BooleanArray(frames.size) { i -> decoders[i].width == w && decoders[i].height == h }
            if (align && frames.size > 1) {
                val sample = FrameMath.sampleFor(w, 480)
                val refSmall = FrameMath.smallLuma(frames[0], sample)
                val scale = w.toFloat() / refSmall.w
                for (i in 1 until frames.size) {
                    ensureActive()
                    if (!usable[i]) continue
                    val s = FrameMath.estimateShift(refSmall, FrameMath.smallLuma(frames[i], sample), 16, false)
                    shifts[i][0] = (s[0] * scale).roundToInt()
                    shifts[i][1] = (s[1] * scale).roundToInt()
                    onProgress(0.2f * i / frames.size)
                }
            }

            val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val stripH = max(192, h / 14)
            val strips = (h + stripH - 1) / stripH
            val done = AtomicInteger(0)
            val workers = min(4, max(1, Runtime.getRuntime().availableProcessors() - 1))
            val base = if (align) 0.2f else 0f

            coroutineScope {
                (0 until workers).map { worker ->
                    async {
                        val opts = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
                        val size = w * stripH
                        val pixels = IntArray(size)
                        val buf = IntArray(size)
                        val sr = IntArray(size)
                        val sg = IntArray(size)
                        val sb = IntArray(size)
                        val cnt = IntArray(size)
                        var s = worker
                        while (s < strips) {
                            ensureActive()
                            val y0 = s * stripH
                            val y1 = min(h, y0 + stripH)
                            val rows = y1 - y0
                            val n = rows * w
                            FrameMath.readRows(decoders[0], w, y0, y1, opts, pixels)
                            for (i in 0 until n) {
                                val c = pixels[i]
                                sr[i] = (c shr 16) and 0xFF
                                sg[i] = (c shr 8) and 0xFF
                                sb[i] = c and 0xFF
                                cnt[i] = 1
                            }
                            for (f in 1 until frames.size) {
                                if (!usable[f]) continue
                                val dx = shifts[f][0]
                                val dy = shifts[f][1]
                                val sy0 = (y0 + dy).coerceIn(0, h)
                                val sy1 = (y1 + dy).coerceIn(0, h)
                                if (sy1 - sy0 <= 0) continue
                                FrameMath.readRows(decoders[f], w, sy0, sy1, opts, buf)
                                val xs = max(0, -dx)
                                val xe = min(w, w - dx)
                                for (r in 0 until rows) {
                                    val sy = y0 + r + dy
                                    if (sy < sy0 || sy >= sy1) continue
                                    val bo = (sy - sy0) * w + dx
                                    val ro = r * w
                                    for (x in xs until xe) {
                                        val p = buf[bo + x]
                                        val k = ro + x
                                        val pr = (p shr 16) and 0xFF
                                        val pg = (p shr 8) and 0xFF
                                        val pb = p and 0xFF
                                        if (lighten) {
                                            if (pr > sr[k]) sr[k] = pr
                                            if (pg > sg[k]) sg[k] = pg
                                            if (pb > sb[k]) sb[k] = pb
                                        } else {
                                            sr[k] += pr
                                            sg[k] += pg
                                            sb[k] += pb
                                            cnt[k]++
                                        }
                                    }
                                }
                            }
                            for (i in 0 until n) {
                                val c = if (lighten) 1 else cnt[i]
                                val r = (sr[i] + c / 2) / c
                                val g = (sg[i] + c / 2) / c
                                val b = (sb[i] + c / 2) / c
                                pixels[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
                            }
                            synchronized(out) { out.setPixels(pixels, 0, w, 0, y0, w, rows) }
                            onProgress(base + (1f - base) * done.incrementAndGet() / strips)
                            s += workers
                        }
                    }
                }.awaitAll()
            }
            out
        } finally {
            decoders.forEach { it.recycle() }
        }
    }
}
