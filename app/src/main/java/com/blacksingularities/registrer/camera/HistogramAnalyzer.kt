package com.blacksingularities.registrer.camera

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy

/**
 * Histogramy z podglądu (YUV): 4 × 64 przedziały – jasność, R, G, B
 * (wynik: [0..63] Y, [64..127] R, [128..191] G, [192..255] B).
 */
class HistogramAnalyzer(private val onResult: (IntArray) -> Unit) : ImageAnalysis.Analyzer {
    private var frame = 0

    override fun analyze(image: ImageProxy) {
        try {
            if (frame++ % 3 != 0) return
            val yp = image.planes[0]
            val up = image.planes[1]
            val vp = image.planes[2]
            val yb = yp.buffer
            val ub = up.buffer
            val vb = vp.buffer
            val w = image.width
            val h = image.height
            val bins = IntArray(256)
            var y = 0
            while (y < h) {
                val yRow = y * yp.rowStride
                val uvY = y / 2
                var x = 0
                while (x < w) {
                    val lum = yb.get(yRow + x * yp.pixelStride).toInt() and 0xFF
                    val uvX = x / 2
                    val u = (ub.get(uvY * up.rowStride + uvX * up.pixelStride).toInt() and 0xFF) - 128
                    val v = (vb.get(uvY * vp.rowStride + uvX * vp.pixelStride).toInt() and 0xFF) - 128
                    val r = (lum + 1.402f * v).toInt().coerceIn(0, 255)
                    val g = (lum - 0.344f * u - 0.714f * v).toInt().coerceIn(0, 255)
                    val b = (lum + 1.772f * u).toInt().coerceIn(0, 255)
                    bins[lum shr 2]++
                    bins[64 + (r shr 2)]++
                    bins[128 + (g shr 2)]++
                    bins[192 + (b shr 2)]++
                    x += 4
                }
                y += 4
            }
            onResult(bins)
        } finally {
            image.close()
        }
    }
}
