package com.amol.amr.detection

import android.graphics.RectF

data class BoundingBox(
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float
) {
    val width: Float get() = maxOf(0.0f, x2 - x1)
    val height: Float get() = maxOf(0.0f, y2 - y1)
    val centerX: Float get() = (x1 + x2) / 2.0f
    val centerY: Float get() = (y1 + y2) / 2.0f
    val area: Float get() = width * height

    fun toRectF(): RectF = RectF(x1, y1, x2, y2)

    fun scale(scaleX: Float, scaleY: Float): BoundingBox {
        return BoundingBox(x1 * scaleX, y1 * scaleY, x2 * scaleX, y2 * scaleY)
    }
}

data class Detection(
    val classId: Int,
    val label: String,
    val confidence: Float,
    val boundingBox: BoundingBox,
    val keypoints: FloatArray? = null
)

data class DetectionFrame(
    val detections: List<Detection>,
    val inferenceTimeMs: Float,
    val frameWidth: Int,
    val frameHeight: Int,
    val timestampMs: Long
)
