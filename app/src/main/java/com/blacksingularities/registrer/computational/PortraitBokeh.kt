package com.blacksingularities.registrer.computational

import com.blacksingularities.registrer.i18n.tr
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Programowy tryb portretowy dla urządzeń bez rozszerzenia producenta (np. foldy Huawei bez
 * usług Google): segmentacja osoby modelem wbudowanym w aplikację, rozmycie tła i miękkie
 * złożenie z oryginałem. Działa z tylnym i przednim aparatem.
 */
object PortraitBokeh {
    private const val MAX_SIDE = 4096
    private const val MASK_SIDE = 640

    /** Zwraca bitmapę już obróconą do pionu (EXIF = 0) i ewentualnie odbitą lustrzanie. */
    suspend fun render(
        jpeg: ByteArray,
        rotation: Int,
        mirror: Boolean,
        strength: Float,
        onProgress: (Float) -> Unit,
    ): Bitmap = withContext(Dispatchers.Default) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size, bounds)
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / sample > MAX_SIDE) sample *= 2
        val decoded = BitmapFactory.decodeByteArray(
            jpeg, 0, jpeg.size,
            BitmapFactory.Options().apply {
                inSampleSize = sample
                inMutable = true
                inPreferredConfig = Bitmap.Config.ARGB_8888
            },
        ) ?: error(tr("Nie można zdekodować klatki", "Cannot decode the frame"))
        val photo = if (rotation != 0 || mirror) {
            val m = Matrix().apply {
                postRotate(rotation.toFloat())
                if (mirror) postScale(-1f, 1f)
            }
            Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, m, true)
                .copy(Bitmap.Config.ARGB_8888, true)
                .also { decoded.recycle() }
        } else {
            decoded
        }
        onProgress(0.15f)

        val w = photo.width
        val h = photo.height
        val maskScale = MASK_SIDE.toFloat() / max(w, h)
        val mw = max(1, (w * maskScale).roundToInt())
        val mh = max(1, (h * maskScale).roundToInt())
        val mask = segment(Bitmap.createScaledBitmap(photo, mw, mh, true))
        onProgress(0.45f)

        // Miękka krawędź maski: lekkie rozmycie i wyostrzenie przejścia.
        boxBlurGray(mask, mw, mh, 2, 2)
        for (i in mask.indices) mask[i] = smoothstep(0.30f, 0.70f, mask[i])
        val maskBitmap = Bitmap.createBitmap(mw, mh, Bitmap.Config.ARGB_8888)
        val maskPixels = IntArray(mw * mh) { i ->
            val v = (mask[i] * 255f).roundToInt().coerceIn(0, 255)
            Color.argb(255, v, v, v)
        }
        maskBitmap.setPixels(maskPixels, 0, mw, 0, 0, mw, mh)
        val maskFull = Bitmap.createScaledBitmap(maskBitmap, w, h, true)
        maskBitmap.recycle()
        onProgress(0.55f)

        // Rozmycie tła na zmniejszonej kopii (szybkie i gładkie po powiększeniu).
        val factor = (6f + 6f * strength.coerceIn(0f, 1f))
        val bw = max(8, (w / factor).roundToInt())
        val bh = max(8, (h / factor).roundToInt())
        val small = Bitmap.createScaledBitmap(photo, bw, bh, true)
        val sp = IntArray(bw * bh)
        small.getPixels(sp, 0, bw, 0, 0, bw, bh)
        val radius = (2 + 4 * strength.coerceIn(0f, 1f)).roundToInt()
        boxBlurRgb(sp, bw, bh, radius, 3)
        small.setPixels(sp, 0, bw, 0, 0, bw, bh)
        val blurred = Bitmap.createScaledBitmap(small, w, h, true)
        small.recycle()
        onProgress(0.75f)

        // Złożenie wiersz po wierszu: osoba z oryginału, tło z rozmycia.
        val rowO = IntArray(w)
        val rowB = IntArray(w)
        val rowM = IntArray(w)
        for (y in 0 until h) {
            photo.getPixels(rowO, 0, w, 0, y, w, 1)
            blurred.getPixels(rowB, 0, w, 0, y, w, 1)
            maskFull.getPixels(rowM, 0, w, 0, y, w, 1)
            for (x in 0 until w) {
                val a = rowM[x] and 0xFF
                if (a == 255) continue
                val o = rowO[x]
                val b = rowB[x]
                val ia = 255 - a
                val r = (((o shr 16) and 0xFF) * a + ((b shr 16) and 0xFF) * ia) / 255
                val g = (((o shr 8) and 0xFF) * a + ((b shr 8) and 0xFF) * ia) / 255
                val bl = ((o and 0xFF) * a + (b and 0xFF) * ia) / 255
                rowO[x] = (0xFF shl 24) or (r shl 16) or (g shl 8) or bl
            }
            photo.setPixels(rowO, 0, w, 0, y, w, 1)
            if (y % 256 == 0) onProgress(0.75f + 0.2f * y / h)
        }
        blurred.recycle()
        maskFull.recycle()
        photo
    }

    private fun segment(input: Bitmap): FloatArray {
        val options = SelfieSegmenterOptions.Builder()
            .setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE)
            .build()
        val segmenter = Segmentation.getClient(options)
        try {
            val result = Tasks.await(segmenter.process(InputImage.fromBitmap(input, 0)))
            val buffer = result.buffer.order(ByteOrder.nativeOrder())
            buffer.rewind()
            val out = FloatArray(input.width * input.height)
            val mw = result.width
            val mh = result.height
            if (mw == input.width && mh == input.height) {
                buffer.asFloatBuffer().get(out)
            } else {
                // Maska w innym rozmiarze – próbkowanie najbliższego sąsiada.
                val raw = FloatArray(mw * mh)
                buffer.asFloatBuffer().get(raw)
                for (y in 0 until input.height) {
                    val sy = (y * mh / input.height).coerceIn(0, mh - 1)
                    for (x in 0 until input.width) {
                        val sx = (x * mw / input.width).coerceIn(0, mw - 1)
                        out[y * input.width + x] = raw[sy * mw + sx]
                    }
                }
            }
            return out
        } finally {
            segmenter.close()
            input.recycle()
        }
    }

    private fun smoothstep(e0: Float, e1: Float, v: Float): Float {
        val t = ((v - e0) / (e1 - e0)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    private fun boxBlurGray(data: FloatArray, w: Int, h: Int, radius: Int, passes: Int) {
        val tmp = FloatArray(data.size)
        repeat(passes) {
            for (y in 0 until h) {
                for (x in 0 until w) {
                    var s = 0f
                    var n = 0
                    for (k in -radius..radius) {
                        val xx = x + k
                        if (xx in 0 until w) { s += data[y * w + xx]; n++ }
                    }
                    tmp[y * w + x] = s / n
                }
            }
            for (y in 0 until h) {
                for (x in 0 until w) {
                    var s = 0f
                    var n = 0
                    for (k in -radius..radius) {
                        val yy = y + k
                        if (yy in 0 until h) { s += tmp[yy * w + x]; n++ }
                    }
                    data[y * w + x] = s / n
                }
            }
        }
    }

    private fun boxBlurRgb(px: IntArray, w: Int, h: Int, radius: Int, passes: Int) {
        val tmp = IntArray(px.size)
        fun pass(src: IntArray, dst: IntArray, horizontal: Boolean) {
            val outer = if (horizontal) h else w
            val inner = if (horizontal) w else h
            for (o in 0 until outer) {
                for (i in 0 until inner) {
                    var r = 0
                    var g = 0
                    var b = 0
                    var n = 0
                    for (k in -radius..radius) {
                        val j = i + k
                        if (j < 0 || j >= inner) continue
                        val c = if (horizontal) src[o * w + j] else src[j * w + o]
                        r += (c shr 16) and 0xFF
                        g += (c shr 8) and 0xFF
                        b += c and 0xFF
                        n++
                    }
                    val idx = if (horizontal) o * w + i else i * w + o
                    dst[idx] = (0xFF shl 24) or ((r / n) shl 16) or ((g / n) shl 8) or (b / n)
                }
            }
        }
        repeat(passes) {
            pass(px, tmp, true)
            pass(tmp, px, false)
        }
    }
}
