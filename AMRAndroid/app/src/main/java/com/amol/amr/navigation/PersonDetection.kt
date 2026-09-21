package com.amol.amr.navigation

/**
 * Standardized representation of a detected person.
 */
data class PersonDetection(
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
    val trackId: Int? = null,
    val conf: Float = 1.0f,
    val keypoints: FloatArray? = null
) {
    val centerX: Float get() = (x1 + x2) / 2.0f
    val centerY: Float get() = (y1 + y2) / 2.0f
    val width: Float get() = maxOf(0.0f, x2 - x1)
    val height: Float get() = maxOf(0.0f, y2 - y1)
    val area: Float get() = width * height
}

/**
 * Processed state for an individual person in the scene.
 */
data class PersonState(
    val trackId: Int,
    val cx: Float,
    val cy: Float,
    val vx: Float,
    val vy: Float,
    val speed: Float,
    val predX: Int,
    val predY: Int,
    val bottomYNorm: Float,
    val areaRatio: Float,
    val isInCenterCorridor: Boolean,
    val isPredictedInPath: Boolean,
    val proximityRisk: Float
)
