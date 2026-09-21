package com.amol.amr.navigation

import kotlin.math.abs

object NavigationPlanner {

    data class PlannerResult(
        val bestDirection: String,
        val costs: Map<String, Float>
    )

    fun planDirection(zones: NavigationZones.ZoneOccupancies): PlannerResult {
        val discountedCenter = (zones.center * 0.8f) - 1e-6f

        val costs = linkedMapOf(
            "FORWARD" to discountedCenter,
            "LEFT" to zones.left,
            "RIGHT" to zones.right
        )

        var minDirection = "FORWARD"
        var minCost = Float.MAX_VALUE

        for ((dir, cost) in costs) {
            if (cost < minCost) {
                minCost = cost
                minDirection = dir
            }
        }

        return PlannerResult(minDirection, costs)
    }

    fun computeSteering(
        zones: NavigationZones.ZoneOccupancies,
        steeringGain: Float = 120.0f,
        maxSteeringAngle: Float = 45.0f,
        deadband: Float = 2.5f
    ): Float {
        // Crowd on right -> positive steering (steer left)
        // Crowd on left -> negative steering (steer right)
        val rawSteering = (zones.right - zones.left) * steeringGain

        if (abs(rawSteering) < deadband) {
            return 0.0f
        }

        return rawSteering.coerceIn(-maxSteeringAngle, maxSteeringAngle)
    }
}
