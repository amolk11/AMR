package com.amol.amr.navigation

import kotlin.math.hypot

/**
 * Velocity and speed tracker with timestamp normalization and EMA smoothing.
 */
class PersonSpeedTracker(
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

    data class VelocityResult(
        val speed: Float,
        val vx: Float,
        val vy: Float
    )

    private val tracks = mutableMapOf<Int, TrackRecord>()

    fun update(trackId: Int, cx: Float, cy: Float, currentTime: Double): VelocityResult {
        val record = tracks[trackId]

        if (record == null) {
            tracks[trackId] = TrackRecord(cx, cy, currentTime, 0.0f, 0.0f)
            return VelocityResult(0.0f, 0.0f, 0.0f)
        }

        val dt = currentTime - record.lastTimestamp

        if (dt <= 1e-4) {
            val speed = hypot(record.vx, record.vy)
            return VelocityResult(speed, record.vx, record.vy)
        }

        if (dt > maxTtlS) {
            record.lastX = cx
            record.lastY = cy
            record.lastTimestamp = currentTime
            record.vx = 0.0f
            record.vy = 0.0f
            return VelocityResult(0.0f, 0.0f, 0.0f)
        }

        val rawVx = ((cx - record.lastX) / dt).toFloat()
        val rawVy = ((cy - record.lastY) / dt).toFloat()

        val vx = emaAlpha * rawVx + (1.0f - emaAlpha) * record.vx
        val vy = emaAlpha * rawVy + (1.0f - emaAlpha) * record.vy
        val speed = hypot(vx, vy)

        record.lastX = cx
        record.lastY = cy
        record.lastTimestamp = currentTime
        record.vx = vx
        record.vy = vy

        return VelocityResult(speed, vx, vy)
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
