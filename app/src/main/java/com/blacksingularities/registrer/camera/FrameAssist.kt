package com.blacksingularities.registrer.camera

import android.graphics.Bitmap
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import kotlin.math.abs
import kotlin.math.max

/** Pomoce kadru liczone z podglądu: obrys ostrości (focus peaking) i zebra prześwietleń. */
data class AssistConfig(
    val peaking: Boolean = false,
    val zebra: Boolean = false,
    /** Próg zebry w procentach jasności. */
    val zebraPercent: Int = 95,
) {
    val any: Boolean get() = peaking || zebra
}

/** Maska nakładana na podgląd; [rotation] – obrót klatki analizy do pozycji ekranu. */
class AssistFrame(val bitmap: Bitmap, val rotation: Int, val stamp: Long)

/**
 * Analiza klatek podglądu: histogram (co 3. klatka) i maska pomocy kadru (co 2. klatka).
 * Maska ma połowę rozdzielczości analizy; dwie bitmapy na zmianę, by nie nadpisywać rysowanej.
 */
class FrameAnalyzer(
    private val histogram: Boolean,
    private val config: () -> AssistConfig,
    private val onHistogram: (IntArray) -> Unit,
    private val onAssist: (AssistFrame?) -> Unit,
) : ImageAnalysis.Analyzer {
    private var frame = 0
    private var buffers = arrayOfNulls<Bitmap>(2)
    private var flip = 0
    private var pixels = IntArray(0)
    private var mags = IntArray(0)
    private var maskShown = false

    override fun analyze(image: ImageProxy) {
        try {
            val n = frame++
            if (histogram && n % 3 == 0) onHistogram(HistogramAnalyzer.compute(image))
            val cfg = config()
            if (!cfg.any) {
                if (maskShown) {
                    maskShown = false
                    onAssist(null)
                }
                return
            }
            if (n % 2 == 0) {
                onAssist(mask(image, cfg))
                maskShown = true
            }
        } finally {
            image.close()
        }
    }

    private fun mask(image: ImageProxy, cfg: AssistConfig): AssistFrame {
        val yp = image.planes[0]
        val yb = yp.buffer
        val rs = yp.rowStride
        val ps = yp.pixelStride
        val step = if (image.width > 400) 2 else 1
        val w = image.width / step
        val h = image.height / step
        if (pixels.size != w * h) {
            pixels = IntArray(w * h)
            mags = IntArray(w * h)
            buffers = arrayOfNulls(2)
        }
        fun lum(x: Int, y: Int): Int = yb.get(y * step * rs + x * step * ps).toInt() and 0xFF

        var maxMag = 0
        if (cfg.peaking) {
            for (y in 1 until h - 1) {
                for (x in 1 until w - 1) {
                    val gx = lum(x + 1, y) - lum(x - 1, y)
                    val gy = lum(x, y + 1) - lum(x, y - 1)
                    val m = abs(gx) + abs(gy)
                    mags[y * w + x] = m
                    if (m > maxMag) maxMag = m
                }
            }
        }
        // Próg względny: zaznaczane są najostrzejsze krawędzie kadru, nie szum.
        val threshold = max(46, (maxMag * 0.5f).toInt())
        val zebraLevel = (cfg.zebraPercent * 255 / 100).coerceIn(200, 254)
        for (y in 0 until h) {
            for (x in 0 until w) {
                val i = y * w + x
                var c = 0
                if (cfg.zebra && lum(x, y) >= zebraLevel) {
                    c = if (((x + y) / 3) % 2 == 0) ZEBRA_LIGHT else ZEBRA_DARK
                }
                if (cfg.peaking && x > 0 && y > 0 && x < w - 1 && y < h - 1 && mags[i] >= threshold) c = PEAK
                pixels[i] = c
            }
        }
        val bmp = buffers[flip] ?: Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also { buffers[flip] = it }
        bmp.setPixels(pixels, 0, w, 0, 0, w, h)
        flip = 1 - flip
        return AssistFrame(bmp, image.imageInfo.rotationDegrees, System.nanoTime())
    }

    companion object {
        private const val PEAK = 0xFFFF3B47.toInt()
        private const val ZEBRA_LIGHT = 0xC8FFFFFF.toInt()
        private const val ZEBRA_DARK = 0x80000000.toInt()
    }
}
