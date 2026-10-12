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
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Fuzja ekspozycji (HDR) z serii klatek o różnym EV. Wagi „dobrej ekspozycji” liczone są
 * na rozmytych miniaturach (lokalne mapowanie tonów, bez artefaktów blokowych) i mnożone
 * przez wagę piksela, aby zachować ostre krawędzie.
 */
object ExposureFusion {

    suspend fun fuse(frames: List<ByteArray>, refIndex: Int, onProgress: (Float) -> Unit): Bitmap =
        withContext(Dispatchers.Default) {
            require(frames.isNotEmpty())
            val decoders = frames.map { FrameMath.regionDecoder(it) }
            try {
                val w = decoders[refIndex].width
                val h = decoders[refIndex].height
                val sample = FrameMath.sampleFor(w, 320)
                val smalls = frames.map { FrameMath.smallLuma(it, sample) }
                val ref = smalls[refIndex]
                val sw = ref.w
                val sh = ref.h
                val scale = w.toFloat() / sw

                val count = frames.size
                val usable = BooleanArray(count) { i -> decoders[i].width == w && decoders[i].height == h && smalls[i].w == sw && smalls[i].h == sh }
                val smallShift = Array(count) { IntArray(2) }
                val shifts = Array(count) { IntArray(2) }
                for (i in 0 until count) {
                    if (i == refIndex || !usable[i]) continue
                    val s = FrameMath.estimateShift(ref, smalls[i], 10, true)
                    smallShift[i] = s
                    shifts[i][0] = (s[0] * scale).roundToInt()
                    shifts[i][1] = (s[1] * scale).roundToInt()
                }
                onProgress(0.15f)

                // Mapy wag w układzie klatki referencyjnej.
                val maps = Array(count) { i ->
                    val m = FloatArray(sw * sh)
                    if (usable[i]) {
                        val luma = smalls[i]
                        val dx = smallShift[i][0]
                        val dy = smallShift[i][1]
                        for (y in 0 until sh) for (x in 0 until sw) {
                            m[y * sw + x] = wellExposed(luma.at(x + dx, y + dy) / 255f) + 1e-3f
                        }
                        boxBlur(m, sw, sh, 3)
                        boxBlur(m, sw, sh, 3)
                    }
                    m
                }
                onProgress(0.25f)

                val pixelWeight = FloatArray(256) { 0.35f + wellExposed(it / 255f) }
                val x0 = IntArray(w)
                val x1 = IntArray(w)
                val fx = FloatArray(w)
                for (x in 0 until w) {
                    val sx = ((x + 0.5f) / scale - 0.5f).coerceIn(0f, (sw - 1).toFloat())
                    x0[x] = sx.toInt()
                    x1[x] = min(sw - 1, x0[x] + 1)
                    fx[x] = sx - x0[x]
                }

                val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                val stripH = max(192, h / 14)
                val strips = (h + stripH - 1) / stripH
                val done = AtomicInteger(0)
                val workers = min(4, max(1, Runtime.getRuntime().availableProcessors() - 1))

                coroutineScope {
                    (0 until workers).map { worker ->
                        async {
                            val opts = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
                            val size = w * stripH
                            val bufs = Array(count) { IntArray(size) }
                            val sy0s = IntArray(count)
                            val sy1s = IntArray(count)
                            val outRow = IntArray(size)
                            var s = worker
                            while (s < strips) {
                                ensureActive()
                                val y0 = s * stripH
                                val y1 = min(h, y0 + stripH)
                                val rows = y1 - y0
                                for (i in 0 until count) {
                                    if (!usable[i]) continue
                                    val dy = shifts[i][1]
                                    sy0s[i] = (y0 + dy).coerceIn(0, h)
                                    sy1s[i] = (y1 + dy).coerceIn(0, h)
                                    if (sy1s[i] > sy0s[i]) FrameMath.readRows(decoders[i], w, sy0s[i], sy1s[i], opts, bufs[i])
                                }
                                for (r in 0 until rows) {
                                    val y = y0 + r
                                    val syf = ((y + 0.5f) / scale - 0.5f).coerceIn(0f, (sh - 1).toFloat())
                                    val yA = syf.toInt()
                                    val yB = min(sh - 1, yA + 1)
                                    val fy = syf - yA
                                    for (x in 0 until w) {
                                        var wr = 0f
                                        var wg = 0f
                                        var wb = 0f
                                        var ws = 0f
                                        for (i in 0 until count) {
                                            if (!usable[i]) continue
                                            val sy = y + shifts[i][1]
                                            val sx = x + shifts[i][0]
                                            if (sy < sy0s[i] || sy >= sy1s[i] || sx < 0 || sx >= w) continue
                                            val p = bufs[i][(sy - sy0s[i]) * w + sx]
                                            val m = maps[i]
                                            val top = m[yA * sw + x0[x]] * (1 - fx[x]) + m[yA * sw + x1[x]] * fx[x]
                                            val bot = m[yB * sw + x0[x]] * (1 - fx[x]) + m[yB * sw + x1[x]] * fx[x]
                                            val wt = (top * (1 - fy) + bot * fy) * pixelWeight[FrameMath.luma(p)]
                                            wr += wt * ((p shr 16) and 0xFF)
                                            wg += wt * ((p shr 8) and 0xFF)
                                            wb += wt * (p and 0xFF)
                                            ws += wt
                                        }
                                        val inv = if (ws > 0f) 1f / ws else 0f
                                        val rr = (wr * inv + 0.5f).toInt().coerceIn(0, 255)
                                        val gg = (wg * inv + 0.5f).toInt().coerceIn(0, 255)
                                        val bb = (wb * inv + 0.5f).toInt().coerceIn(0, 255)
                                        outRow[r * w + x] = (0xFF shl 24) or (rr shl 16) or (gg shl 8) or bb
                                    }
                                }
                                synchronized(out) { out.setPixels(outRow, 0, w, 0, y0, w, rows) }
                                onProgress(0.25f + 0.75f * done.incrementAndGet() / strips)
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

    private fun wellExposed(l: Float): Float {
        val d = l - 0.5f
        return exp(-(d * d) / (2f * 0.2f * 0.2f))
    }

    private fun boxBlur(m: FloatArray, w: Int, h: Int, r: Int) {
        val tmp = FloatArray(m.size)
        for (y in 0 until h) {
            for (x in 0 until w) {
                var s = 0f
                var c = 0
                for (k in max(0, x - r)..min(w - 1, x + r)) {
                    s += m[y * w + k]; c++
                }
                tmp[y * w + x] = s / c
            }
        }
        for (y in 0 until h) {
            for (x in 0 until w) {
                var s = 0f
                var c = 0
                for (k in max(0, y - r)..min(h - 1, y + r)) {
                    s += tmp[k * w + x]; c++
                }
                m[y * w + x] = s / c
            }
        }
    }
}
