package com.blacksingularities.registrer.camera

import android.graphics.Bitmap
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import kotlin.math.abs
import kotlin.math.max

/**
 * Co liczyć z podglądu: histogram, obrys ostrości (focus peaking) i zebra prześwietleń.
 * Zmienia się na żywo – włączenie lub wyłączenie nie wymaga ponownego wiązania kamery.
 */
data class AssistConfig(
    val peaking: Boolean = false,
    val zebra: Boolean = false,
    /** Próg zebry w procentach jasności. */
    val zebraPercent: Int = 95,
    val histogram: Boolean = false,
) {
    /** Liczona jest maska nakładana na podgląd. */
    val any: Boolean get() = peaking || zebra

    /** Potrzebna jest jakakolwiek analiza obrazu. */
    val active: Boolean get() = any || histogram
}

/**
 * Maska nakładana na podgląd. [rotation] – obrót do pozycji ekranu; [sensor] – maska z klatki
 * analizy (wymaga odbicia dla przedniego aparatu), a nie z gotowego obrazu podglądu.
 */
class AssistFrame(val bitmap: Bitmap, val rotation: Int, val stamp: Long, val sensor: Boolean = true)

/**
 * Wspólne liczenie maski z tablicy jasności (0–255). Trzy bitmapy na zmianę – rysowana
 * w interfejsie nigdy nie jest nadpisywana w trakcie.
 */
class AssistRenderer {
    private var buffers = arrayOfNulls<Bitmap>(3)
    private var flip = 0
    private var pixels = IntArray(0)
    private var mags = IntArray(0)

    fun render(luma: IntArray, w: Int, h: Int, cfg: AssistConfig, rotation: Int, sensor: Boolean): AssistFrame {
        if (pixels.size != w * h) {
            pixels = IntArray(w * h)
            mags = IntArray(w * h)
            buffers = arrayOfNulls(3)
        }
        var maxMag = 0
        if (cfg.peaking) {
            for (y in 1 until h - 1) {
                val row = y * w
                for (x in 1 until w - 1) {
                    val i = row + x
                    val m = abs(luma[i + 1] - luma[i - 1]) + abs(luma[i + w] - luma[i - w])
                    mags[i] = m
                    if (m > maxMag) maxMag = m
                }
            }
        }
        // Próg względny: zaznaczane są najostrzejsze krawędzie kadru, nie szum.
        val threshold = max(46, (maxMag * 0.5f).toInt())
        val zebraLevel = (cfg.zebraPercent * 255 / 100).coerceIn(200, 254)
        for (y in 0 until h) {
            val edgeRow = y == 0 || y == h - 1
            for (x in 0 until w) {
                val i = y * w + x
                var c = 0
                if (cfg.zebra && luma[i] >= zebraLevel) {
                    c = if (((x + y) / 3) % 2 == 0) ZEBRA_LIGHT else ZEBRA_DARK
                }
                if (cfg.peaking && !edgeRow && x > 0 && x < w - 1 && mags[i] >= threshold) c = PEAK
                pixels[i] = c
            }
        }
        val bmp = buffers[flip] ?: Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also { buffers[flip] = it }
        bmp.setPixels(pixels, 0, w, 0, 0, w, h)
        flip = (flip + 1) % buffers.size
        return AssistFrame(bmp, rotation, System.nanoTime(), sensor)
    }

    companion object {
        private const val PEAK = 0xFFFF3B47.toInt()
        private const val ZEBRA_LIGHT = 0xC8FFFFFF.toInt()
        private const val ZEBRA_DARK = 0x80000000.toInt()
    }
}

/**
 * Analiza klatek podglądu (ImageAnalysis): histogram (co 3. klatka) i maska pomocy kadru
 * (co 2. klatka). Konfiguracja czytana przy każdej klatce – przełączanie działa od razu.
 */
class FrameAnalyzer(
    private val config: () -> AssistConfig,
    private val onHistogram: (IntArray) -> Unit,
    private val onAssist: (AssistFrame?) -> Unit,
) : ImageAnalysis.Analyzer {
    private var frame = 0
    private var luma = IntArray(0)
    private val renderer = AssistRenderer()
    private var maskShown = false

    override fun analyze(image: ImageProxy) {
        try {
            val n = frame++
            val cfg = config()
            if (cfg.histogram && n % 3 == 0) onHistogram(HistogramAnalyzer.compute(image))
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
        if (luma.size != w * h) luma = IntArray(w * h)
        for (y in 0 until h) {
            val src = y * step * rs
            val dst = y * w
            for (x in 0 until w) luma[dst + x] = yb.get(src + x * step * ps).toInt() and 0xFF
        }
        return renderer.render(luma, w, h, cfg, image.imageInfo.rotationDegrees, sensor = true)
    }
}

/**
 * Zapasowa analiza z gotowego obrazu podglądu – gdy sesja nie ma osobnej analizy (wideo,
 * zwolnione tempo, rozszerzenia producenta). Obraz jest już obrócony i odbity jak na ekranie.
 */
class PreviewSampler {
    private val renderer = AssistRenderer()
    private var argb = IntArray(0)
    private var luma = IntArray(0)

    /** Zwraca histogram (gdy potrzebny) i maskę (gdy potrzebna); [bitmap] jest zwalniana. */
    fun process(bitmap: Bitmap, cfg: AssistConfig): Pair<IntArray?, AssistFrame?> {
        val target = if (cfg.any) 320 else 160
        val scale = target.toFloat() / maxOf(bitmap.width, bitmap.height)
        val small = if (scale < 1f) {
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt().coerceAtLeast(1), (bitmap.height * scale).toInt().coerceAtLeast(1), true)
        } else {
            bitmap
        }
        val w = small.width
        val h = small.height
        if (argb.size != w * h) {
            argb = IntArray(w * h)
            luma = IntArray(w * h)
        }
        small.getPixels(argb, 0, w, 0, 0, w, h)
        if (small !== bitmap) small.recycle()
        bitmap.recycle()
        val bins = if (cfg.histogram) IntArray(256) else null
        for (i in argb.indices) {
            val c = argb[i]
            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            val b = c and 0xFF
            val y = (r * 77 + g * 150 + b * 29) shr 8
            luma[i] = y
            if (bins != null) {
                bins[y shr 2]++
                bins[64 + (r shr 2)]++
                bins[128 + (g shr 2)]++
                bins[192 + (b shr 2)]++
            }
        }
        val frame = if (cfg.any) renderer.render(luma, w, h, cfg, 0, sensor = false) else null
        return bins to frame
    }
}
