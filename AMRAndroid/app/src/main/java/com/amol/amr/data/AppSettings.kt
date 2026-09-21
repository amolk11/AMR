package com.amol.amr.data

data class AppSettings(
    val confidenceThreshold: Float = 0.45f,
    val iouThreshold: Float = 0.50f,
    val isUsbEnabled: Boolean = true,
    val isDebugOverlayVisible: Boolean = true,
    val modelInputSize: Int = 640,
    val cameraResolutionWidth: Int = 640,
    val cameraResolutionHeight: Int = 480,
    val targetInferenceFps: Int = 20
)
