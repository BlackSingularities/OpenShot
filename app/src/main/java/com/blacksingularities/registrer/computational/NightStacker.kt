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
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Nocny potok wieloklatkowy:
 * 1. wyrównanie każdej klatki do referencji (globalne przesunięcie na miniaturach),
 * 2. uśrednianie z odrzucaniem „duchów” (piksele zbyt różne od referencji są pomijane),
 * 3. adaptacyjne mapowanie tonów (rozszerzony Reinhard) rozjaśniające cienie bez przepalania.
 * Przetwarzanie odbywa się pasami, równolegle na wielu rdzeniach – zużycie pamięci ≈ 1 klatka.
 */
object NightStacker {

    suspend fun stack(frames: List<ByteArray>, onProgress: (Float) -> Unit): Bitmap = withContext(Dispatchers.Default) {
        require(frames.isNotEmpty())
        val decoders = frames.map { FrameMath.regionDecoder(it) }
        try {
            val w = decoders[0].width
            val h = decoders[0].height
            val sample = FrameMath.sampleFor(w, 480)
            val refSmall = FrameMath.smallLuma(frames[0], sample)
            val scale = w.toFloat() / refSmall.w

            val shifts = Array(frames.size) { IntArray(2) }
            val usable = BooleanArray(frames.size) { it == 0 }
            for (i in 1 until frames.size) {
                ensureActive()
                if (decoders[i].width != w || decoders[i].height != h) continue
                val s = FrameMath.estimateShift(refSmall, FrameMath.smallLuma(frames[i], sample), 12, false)
                shifts[i][0] = (s[0] * scale).roundToInt()
                shifts[i][1] = (s[1] * scale).roundToInt()
                usable[i] = true
                onProgress(0.25f * i / frames.size)
            }

            val mean = refSmall.mean() / 255f
            val gain = (0.36f / max(mean, 0.02f)).coerceIn(1f, 4f)
            val lut = toneLut(gain)

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
                        val ref = IntArray(size)
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
                            FrameMath.readRows(decoders[0], w, y0, y1, opts, ref)
                            for (i in 0 until n) {
                                val c = ref[i]
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
                                        val lq = FrameMath.luma(ref[k])
                                        if (abs(FrameMath.luma(p) - lq) <= 28 + (lq shr 2)) {
                                            sr[k] += (p shr 16) and 0xFF
                                            sg[k] += (p shr 8) and 0xFF
                                            sb[k] += p and 0xFF
                                            cnt[k]++
                                        }
                                    }
                                }
                            }
                            for (i in 0 until n) {
                                val c = cnt[i]
                                val r = lut[(sr[i] shl 4) / c]
                                val g = lut[(sg[i] shl 4) / c]
                                val b = lut[(sb[i] shl 4) / c]
                                ref[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
                            }
                            synchronized(out) { out.setPixels(ref, 0, w, 0, y0, w, rows) }
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

    /** LUT dla wartości ×16 (0..4080): rozszerzony Reinhard z bielą = gain (gain = 1 → tożsamość). */
    private fun toneLut(gain: Float): IntArray = IntArray(255 * 16 + 1) { i ->
        val v = i / (255f * 16f)
        val x = v * gain
        val y = x * (1f + x / (gain * gain)) / (1f + x)
        (y.coerceIn(0f, 1f) * 255f + 0.5f).toInt()
    }
}
