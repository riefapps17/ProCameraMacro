package com.example.camera

import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.min

/**
 * Focus Stacking Engine for Macro Photography
 * Stacks multiple macro images taken at incremental focal planes across the depth of field.
 * Uses a 3x3 Neighborhood Laplacian Contrast Energy Operator to identify truly sharp focal planes
 * while rejecting single-pixel sensor noise. Also protects memory on high-MP modes before
 * upscaling back to final target resolution.
 */
object FocusStackEngine {

    private const val MAX_STACK_PROCESSING_DIMENSION = 2560 // Safe multi-slice RAM threshold

    suspend fun stackImages(bitmaps: List<Bitmap>): Bitmap = withContext(Dispatchers.Default) {
        if (bitmaps.isEmpty()) throw IllegalArgumentException("No bitmaps provided for focus stacking")
        if (bitmaps.size == 1) return@withContext bitmaps.first()

        val origWidth = bitmaps.first().width
        val origHeight = bitmaps.first().height

        // Scale down during multi-slice Laplacian fusion if slices exceed safe memory bounds (e.g. 48MP x 10 slices)
        val maxOrigDim = maxOf(origWidth, origHeight)
        val scaleFactor = if (maxOrigDim > MAX_STACK_PROCESSING_DIMENSION) {
            MAX_STACK_PROCESSING_DIMENSION.toFloat() / maxOrigDim.toFloat()
        } else {
            1.0f
        }

        val width = (origWidth * scaleFactor).toInt().coerceAtLeast(100)
        val height = (origHeight * scaleFactor).toInt().coerceAtLeast(100)

        val numImages = bitmaps.size

        // Extract RGB pixel buffers and precalculate Luminance + 1-pixel Laplacian energy per slice
        val pixelArrays = Array(numImages) { imgIdx ->
            val bmp = bitmaps[imgIdx]
            val workingBmp = if (bmp.width != width || bmp.height != height) {
                Bitmap.createScaledBitmap(bmp, width, height, true)
            } else {
                bmp
            }
            val arr = IntArray(width * height)
            workingBmp.getPixels(arr, 0, width, 0, 0, width, height)
            arr
        }

        // Precompute per-pixel Laplacian magnitude for each slice
        val energyArrays = Array(numImages) { imgIdx ->
            val arr = pixelArrays[imgIdx]
            val lum = IntArray(width * height)
            for (i in arr.indices) {
                val c = arr[i]
                lum[i] = ((c shr 16 and 0xFF) * 77 + (c shr 8 and 0xFF) * 150 + (c and 0xFF) * 29) shr 8
            }

            val energy = IntArray(width * height)
            for (y in 1 until height - 1) {
                val yOff = y * width
                val yPrev = (y - 1) * width
                val yNext = (y + 1) * width
                for (x in 1 until width - 1) {
                    val idx = yOff + x
                    val lap = abs(4 * lum[idx] - lum[idx - 1] - lum[idx + 1] - lum[yPrev + x] - lum[yNext + x])
                    energy[idx] = min(lap, 255)
                }
            }
            energy
        }

        val resultPixels = IntArray(width * height)

        // Fuse slices using 3x3 neighborhood local contrast energy (prevents salt-and-pepper noise artifacts)
        for (y in 0 until height) {
            val yOff = y * width
            val y0 = (y - 1).coerceAtLeast(0) * width
            val y1 = yOff
            val y2 = (y + 1).coerceAtMost(height - 1) * width

            for (x in 0 until width) {
                val x0 = (x - 1).coerceAtLeast(0)
                val x1 = x
                val x2 = (x + 1).coerceAtMost(width - 1)

                var maxRegionEnergy = -1
                var bestSliceIdx = 0

                for (imgIdx in 0 until numImages) {
                    val e = energyArrays[imgIdx]
                    // 3x3 neighborhood contrast sum (center weighted x2)
                    val regionEnergy =
                        e[y0 + x0] + e[y0 + x1] + e[y0 + x2] +
                        e[y1 + x0] + (e[y1 + x1] shl 1) + e[y1 + x2] +
                        e[y2 + x0] + e[y2 + x1] + e[y2 + x2]

                    if (regionEnergy > maxRegionEnergy) {
                        maxRegionEnergy = regionEnergy
                        bestSliceIdx = imgIdx
                    }
                }

                resultPixels[yOff + x] = pixelArrays[bestSliceIdx][yOff + x]
            }
        }

        val stackedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        stackedBitmap.setPixels(resultPixels, 0, width, 0, 0, width, height)

        if (width != origWidth || height != origHeight) {
            Bitmap.createScaledBitmap(stackedBitmap, origWidth, origHeight, true)
        } else {
            stackedBitmap
        }
    }
}
