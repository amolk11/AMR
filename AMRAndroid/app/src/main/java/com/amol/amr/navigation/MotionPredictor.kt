package com.amol.amr.navigation

/**
 * Trajectory predictor using constant velocity kinematics over a calibrated horizon.
 */
class MotionPredictor(
    private val horizonS: Float = 0.8f,
    private val emaAlpha: Float = 0.40f,
    private val maxTtlS: Double = 2.0
) {
    private data class TrackRecord(
        var lastX: Float,
        var lastY: Float,
        var lastTimestamp: Double,
        var vx: Float,
        var vy: Float
    )

    private val tracks = mutableMapOf<Int, TrackRecord>()

    fun predict(
        trackId: Int,
        cx: Float,
        cy: Float,
        currentTime: Double,
        inputVx: Float? = null,
        inputVy: Float? = null
    ): Pair<Int, Int> {
        val vx: Float
        val vy: Float

        if (inputVx != null && inputVy != null) {
            vx = inputVx
            vy = inputVy
        } else {
            val record = tracks[trackId]
            if (record == null) {
                tracks[trackId] = TrackRecord(cx, cy, currentTime, 0.0f, 0.0f)
                return Pair(Math.round(cx), Math.round(cy))
            }

            val dt = currentTime - record.lastTimestamp
            if (dt <= 1e-4) {
                vx = record.vx
                vy = record.vy
            } else if (dt > maxTtlS) {
                record.lastX = cx
                record.lastY = cy
                record.lastTimestamp = currentTime
                record.vx = 0.0f
                record.vy = 0.0f
                return Pair(Math.round(cx), Math.round(cy))
            } else {
                val rawVx = ((cx - record.lastX) / dt).toFloat()
                val rawVy = ((cy - record.lastY) / dt).toFloat()
                vx = emaAlpha * rawVx + (1.0f - emaAlpha) * record.vx
                vy = emaAlpha * rawVy + (1.0f - emaAlpha) * record.vy
                record.vx = vx
                record.vy = vy
            }
            record.lastX = cx
            record.lastY = cy
            record.lastTimestamp = currentTime
        }

        val predX = cx + vx * horizonS
        val predY = cy + vy * horizonS

        return Pair(Math.round(predX), Math.round(predY))
    }

    fun cleanupStaleTracks(currentTime: Double) {
        val iterator = tracks.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (currentTime - entry.value.lastTimestamp > maxTtlS) {
                iterator.remove()
            }
        }
    }

    fun reset() {
        tracks.clear()
    }
}
