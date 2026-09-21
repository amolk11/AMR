package com.amol.amr.navigation

import java.util.Locale

/**
 * Deterministic output command for robot actuation.
 */
data class NavigationCommand(
    val steering: Float,
    val speed: Float,
    val emergencyStop: Boolean,
    val reason: String,
    val diagnostics: Map<String, Any> = emptyMap()
) {
    /** Formats command as a string for ESP32-S3 serial communication */
    fun toSerialString(): String {
        return if (emergencyStop || speed <= 0.0f) {
            "STOP\n"
        } else {
            val steerRounded = Math.round(steering)
            String.format(Locale.US, "STEER:%d SPEED:%.2f\n", steerRounded, speed)
        }
    }

    /** Formats structured protocol string for robust packet transmission */
    fun toProtocolString(sequenceNumber: Long): String {
        val direction = when {
            emergencyStop || speed <= 0.0f -> "STOP"
            steering > 2.5f -> "LEFT"
            steering < -2.5f -> "RIGHT"
            else -> "FORWARD"
        }
        val speedPwm = Math.round(speed * 255).coerceIn(0, 255)
        val steerDeg = Math.round(steering).coerceIn(-45, 45)
        val stopFlag = if (emergencyStop) 1 else 0

        return "CMD,$direction,$speedPwm,$steerDeg,$stopFlag,$sequenceNumber\n"
    }
}
