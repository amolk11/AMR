package com.amol.amr.detection

import android.graphics.Bitmap

/**
 * Interface for mobile person detection models.
 */
interface YoloDetector {
    suspend fun detect(bitmap: Bitmap, rotationDegrees: Int): DetectionFrame
    fun close()
}
