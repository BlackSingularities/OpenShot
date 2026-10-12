package com.blacksingularities.registrer.camera

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy

/** Liczy 64-przedziałowy histogram luminancji z płaszczyzny Y podglądu. */
class HistogramAnalyzer(private val onResult: (IntArray) -> Unit) : ImageAnalysis.Analyzer {
    private var frame = 0

    override fun analyze(image: ImageProxy) {
        try {
            if (frame++ % 3 != 0) return
            val plane = image.planes[0]
            val buffer = plane.buffer
            val rowStride = plane.rowStride
            val pixelStride = plane.pixelStride
            val w = image.width
            val h = image.height
            val bins = IntArray(64)
            var y = 0
            while (y < h) {
                val row = y * rowStride
                var x = 0
                while (x < w) {
                    val v = buffer.get(row + x * pixelStride).toInt() and 0xFF
                    bins[v shr 2]++
                    x += 2
                }
                y += 2
            }
            onResult(bins)
        } finally {
            image.close()
        }
    }
}
