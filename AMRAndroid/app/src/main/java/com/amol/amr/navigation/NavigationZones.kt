package com.amol.amr.navigation

/**
 * Soft zone spatial partitioning and occupancy analysis.
 */
object NavigationZones {

    data class ZoneWeights(val left: Float, val center: Float, val right: Float)
    data class ZoneOccupancies(val left: Float, val center: Float, val right: Float)

    fun computeSoftZoneWeights(
        cx: Float,
        width: Float,
        leftRatio: Float = 1.0f / 3.0f,
        rightRatio: Float = 2.0f / 3.0f,
        softMarginRatio: Float = 0.05f
    ): ZoneWeights {
        val leftX = width * leftRatio
        val rightX = width * rightRatio
        val margin = width * softMarginRatio

        return when {
            cx < (leftX - margin) -> ZoneWeights(1.0f, 0.0f, 0.0f)
            cx < (leftX + margin) -> {
                val t = (cx - (leftX - margin)) / (2.0f * margin)
                ZoneWeights(1.0f - t, t, 0.0f)
            }
            cx < (rightX - margin) -> ZoneWeights(0.0f, 1.0f, 0.0f)
            cx < (rightX + margin) -> {
                val t = (cx - (rightX - margin)) / (2.0f * margin)
                ZoneWeights(0.0f, 1.0f - t, t)
            }
            else -> ZoneWeights(0.0f, 0.0f, 1.0f)
        }
    }

    fun computeZoneOccupancies(
        detections: List<PersonDetection>,
        frameWidth: Float,
        frameHeight: Float,
        leftRatio: Float = 1.0f / 3.0f,
        rightRatio: Float = 2.0f / 3.0f,
        softMarginRatio: Float = 0.05f
    ): ZoneOccupancies {
        val totalArea = frameWidth * frameHeight
        if (totalArea <= 0.0f) return ZoneOccupancies(0.0f, 0.0f, 0.0f)

        var left = 0.0f
        var center = 0.0f
        var right = 0.0f

        for (d in detections) {
            val cx = (d.x1 + d.x2) / 2.0f
            val area = maxOf(0.0f, d.x2 - d.x1) * maxOf(0.0f, d.y2 - d.y1)
            val occupancy = area / totalArea

            val weights = computeSoftZoneWeights(cx, frameWidth, leftRatio, rightRatio, softMarginRatio)
            left += occupancy * weights.left
            center += occupancy * weights.center
            right += occupancy * weights.right
        }

        return ZoneOccupancies(left, center, right)
    }
}
