package com.amol.amr.navigation

/**
 * Centralized configuration parameters for AMR navigation engine.
 * Exactly mirrors the reference Python NavigationConfig parameters.
 */
data class NavigationConfig(
    // Frame & Zone Geometry
    val leftBoundaryRatio: Float = 1.0f / 3.0f,
    val rightBoundaryRatio: Float = 2.0f / 3.0f,
    val zoneSoftMarginRatio: Float = 0.05f,

    // Steering Limits & Dynamics
    val maxSteeringAngle: Float = 45.0f,
    val steeringGain: Float = 120.0f,
    val steeringDeadband: Float = 2.5f,
    val steeringEmaAlpha: Float = 0.35f,

    // Speed Limits & Acceleration
    val baseSpeed: Float = 1.0f,
    val minMovingSpeed: Float = 0.20f,
    val maxAccelRate: Float = 1.0f,
    val maxDecelRate: Float = 2.5f,

    // Crowd Density Speed Multipliers
    val speedScaleEmpty: Float = 1.00f,
    val speedScaleLow: Float = 0.85f,
    val speedScaleMedium: Float = 0.65f,
    val speedScaleHigh: Float = 0.35f,

    // Proximity & Collision Risk Thresholds
    val emergencyStopBottomY: Float = 0.78f,
    val emergencyStopAreaRatio: Float = 0.18f,
    val cautionBottomY: Float = 0.50f,

    // Trajectory prediction collision horizon in seconds
    val predictionHorizonS: Float = 0.8f,
    val maxTrackHistoryS: Double = 2.0,
    val velocityEmaAlpha: Float = 0.40f,

    // Collision risk scoring weights
    val centerProximityWeight: Float = 3.0f,
    val trajectoryCrossingWeight: Float = 2.0f,
    val lateralProximityWeight: Float = 1.0f,

    // Emergency stop recovery hysteresis
    val stopRecoveryCooldownS: Double = 0.4
)
