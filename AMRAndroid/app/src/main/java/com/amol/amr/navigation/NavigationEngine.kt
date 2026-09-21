package com.amol.amr.navigation

import kotlin.math.abs

/**
 * Platform-independent Kotlin Navigation Engine.
 * Decoupled from camera hardware, UI, and motor interfaces.
 * Produces deterministic, rate-limited NavigationCommands.
 */
class NavigationEngine(
    val config: NavigationConfig = NavigationConfig()
) {
    val speedTracker = PersonSpeedTracker(
        emaAlpha = config.velocityEmaAlpha,
        maxTtlS = config.maxTrackHistoryS
    )
    val motionPredictor = MotionPredictor(
        horizonS = config.predictionHorizonS,
        emaAlpha = config.velocityEmaAlpha,
        maxTtlS = config.maxTrackHistoryS
    )

    // Temporal state filters
    var currentSteering: Float = 0.0f
        private set
    var currentSpeed: Float = 0.0f
        private set
    private var lastUpdateTime: Double? = null

    // Stop state hysteresis
    var isEmergencyStopped: Boolean = false
        private set
    private var clearPathStartTime: Double? = null

    fun reset() {
        speedTracker.reset()
        motionPredictor.reset()
        currentSteering = 0.0f
        currentSpeed = 0.0f
        lastUpdateTime = null
        isEmergencyStopped = false
        clearPathStartTime = null
    }

    fun process(
        detections: List<PersonDetection>,
        frameWidth: Int,
        frameHeight: Int,
        currentTime: Double = System.currentTimeMillis() / 1000.0
    ): NavigationCommand {
        val dt: Double = when (val last = lastUpdateTime) {
            null -> 0.033
            else -> (currentTime - last).coerceIn(1e-4, 1.0)
        }
        lastUpdateTime = currentTime

        // Stale tracking garbage collection
        speedTracker.cleanupStaleTracks(currentTime)
        motionPredictor.cleanupStaleTracks(currentTime)

        // 1. Spatial Occupancy Analysis
        val zones = NavigationZones.computeZoneOccupancies(
            detections = detections,
            frameWidth = frameWidth.toFloat(),
            frameHeight = frameHeight.toFloat(),
            leftRatio = config.leftBoundaryRatio,
            rightRatio = config.rightBoundaryRatio,
            softMarginRatio = config.zoneSoftMarginRatio
        )

        val plan = NavigationPlanner.planDirection(zones)
        val rawTargetSteering = NavigationPlanner.computeSteering(
            zones = zones,
            steeringGain = config.steeringGain,
            maxSteeringAngle = config.maxSteeringAngle,
            deadband = config.steeringDeadband
        )

        // 2. Crowd Density Analysis
        val peopleCount = detections.size
        val crowdDensity = CrowdAnalyzer.analyzeCrowdDensity(peopleCount)
        val crowdSpeedScale = when (crowdDensity) {
            CrowdDensity.EMPTY -> config.speedScaleEmpty
            CrowdDensity.LOW -> config.speedScaleLow
            CrowdDensity.MEDIUM -> config.speedScaleMedium
            CrowdDensity.HIGH -> config.speedScaleHigh
        }

        // 3. Multi-Person State Tracking & Risk Evaluation
        val totalFrameArea = maxOf(1.0f, (frameWidth * frameHeight).toFloat())
        val centerX1 = frameWidth * config.leftBoundaryRatio
        val centerX2 = frameWidth * config.rightBoundaryRatio

        val personStates = mutableListOf<PersonState>()
        var immediateStopTriggered = false
        val stopReasons = mutableListOf<String>()
        var totalCollisionRisk = 0.0f

        for ((idx, det) in detections.withIndex()) {
            val tid = det.trackId ?: (10000 + idx)
            val cx = det.centerX
            val cy = det.centerY

            val vel = speedTracker.update(tid, cx, cy, currentTime)
            val (predX, predY) = motionPredictor.predict(tid, cx, cy, currentTime, vel.vx, vel.vy)

            val bottomYNorm = det.y2 / frameHeight.toFloat()
            val areaRatio = det.area / totalFrameArea
            val inCenter = (cx in centerX1..centerX2)
            val predictedInPath = (predX.toFloat() in centerX1..centerX2) &&
                    (predY > frameHeight * config.cautionBottomY)

            var personRisk = 0.0f

            // Direct Frontal Close Proximity (Critical Safety Check)
            if (inCenter) {
                if (bottomYNorm >= config.emergencyStopBottomY) {
                    immediateStopTriggered = true
                    stopReasons.add("Person $tid directly in front (dist close: y=${String.format(java.util.Locale.US, "%.2f", bottomYNorm)})")
                } else if (areaRatio >= config.emergencyStopAreaRatio) {
                    immediateStopTriggered = true
                    stopReasons.add("Person $tid large area in center (area=${String.format(java.util.Locale.US, "%.2f", areaRatio)})")
                } else {
                    val proximityFactor = maxOf(0.0f, (bottomYNorm - config.cautionBottomY) / (1.0f - config.cautionBottomY))
                    personRisk += proximityFactor * config.centerProximityWeight
                }
            }

            // Lateral trajectory crossing
            if (predictedInPath) {
                personRisk += config.trajectoryCrossingWeight
                totalCollisionRisk += 1.0f
            }

            if (!inCenter && bottomYNorm >= config.emergencyStopBottomY) {
                personRisk += config.lateralProximityWeight
            }

            val pState = PersonState(
                trackId = tid,
                cx = cx,
                cy = cy,
                vx = vel.vx,
                vy = vel.vy,
                speed = vel.speed,
                predX = predX,
                predY = predY,
                bottomYNorm = bottomYNorm,
                areaRatio = areaRatio,
                isInCenterCorridor = inCenter,
                isPredictedInPath = predictedInPath,
                proximityRisk = personRisk
            )
            personStates.add(pState)
        }

        // 4. Emergency Stop and Recovery Hysteresis
        if (immediateStopTriggered || totalCollisionRisk >= 3.0f) {
            isEmergencyStopped = true
            clearPathStartTime = null
        } else {
            if (isEmergencyStopped) {
                if (clearPathStartTime == null) {
                    clearPathStartTime = currentTime
                } else if ((currentTime - clearPathStartTime!!) >= config.stopRecoveryCooldownS) {
                    isEmergencyStopped = false
                    clearPathStartTime = null
                }
            }
        }

        // 5. Speed Planning with Distance Scaling & Rate Limiting
        var targetSpeed: Float
        val primaryReason: String

        if (isEmergencyStopped) {
            targetSpeed = 0.0f
            primaryReason = if (stopReasons.isNotEmpty()) stopReasons[0] else "COLLISION_RISK_HIGH"
        } else {
            targetSpeed = config.baseSpeed * crowdSpeedScale

            if (totalCollisionRisk >= 2.0f) {
                targetSpeed *= 0.50f
            } else if (totalCollisionRisk >= 1.0f) {
                targetSpeed *= 0.75f
            }

            var maxCenterRisk = 0.0f
            for (p in personStates) {
                if (p.isInCenterCorridor && p.proximityRisk > maxCenterRisk) {
                    maxCenterRisk = p.proximityRisk
                }
            }

            if (maxCenterRisk > 0.0f) {
                val riskDecay = maxOf(0.2f, 1.0f - (maxCenterRisk / (config.centerProximityWeight * 1.5f)))
                targetSpeed *= riskDecay
            }

            if (targetSpeed > 0.0f) {
                targetSpeed = maxOf(config.minMovingSpeed, targetSpeed)
            }

            primaryReason = "${plan.bestDirection} (Crowd: ${crowdDensity.value})"
        }

        // Apply acceleration / deceleration limits
        if (targetSpeed > currentSpeed) {
            val maxIncrease = (config.maxAccelRate * dt).toFloat()
            currentSpeed = minOf(targetSpeed, currentSpeed + maxIncrease)
        } else {
            val maxDecrease = (config.maxDecelRate * dt).toFloat()
            if (isEmergencyStopped) {
                currentSpeed = 0.0f
            } else {
                currentSpeed = maxOf(targetSpeed, currentSpeed - maxDecrease)
            }
        }

        // 6. Steering Smoothing & Deadband
        val alpha = config.steeringEmaAlpha
        currentSteering = alpha * rawTargetSteering + (1.0f - alpha) * currentSteering

        if (abs(currentSteering) < (config.steeringDeadband / 2.0f)) {
            currentSteering = 0.0f
        }

        val diagnostics = mapOf<String, Any>(
            "people_count" to peopleCount,
            "crowd_density" to crowdDensity.value,
            "collision_risk_count" to totalCollisionRisk,
            "zones" to zones,
            "direction_choice" to plan.bestDirection,
            "raw_steering" to rawTargetSteering,
            "target_speed" to targetSpeed,
            "dt" to dt,
            "person_states" to personStates
        )

        return NavigationCommand(
            steering = currentSteering,
            speed = currentSpeed,
            emergencyStop = isEmergencyStopped,
            reason = primaryReason,
            diagnostics = diagnostics
        )
    }
}
