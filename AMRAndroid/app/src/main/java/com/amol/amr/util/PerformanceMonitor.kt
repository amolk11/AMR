package com.amol.amr.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PerformanceMetrics(
    val cameraFps: Float = 0.0f,
    val inferenceLatencyMs: Float = 0.0f,
    val navigationLatencyMs: Float = 0.0f,
    val totalPipelineLatencyMs: Float = 0.0f,
    val droppedFramesCount: Long = 0L,
    val memoryUsageMb: Float = 0.0f
)

class PerformanceMonitor(private val timeProvider: TimeProvider = SystemTimeProvider()) {
    private val _metrics = MutableStateFlow(PerformanceMetrics())
    val metrics: StateFlow<PerformanceMetrics> = _metrics.asStateFlow()

    private var frameCount = 0
    private var lastFpsUpdateTime = timeProvider.currentTimeSeconds()
    private var currentFps = 0.0f
    private var droppedFrames = 0L

    fun onFrameCaptured() {
        frameCount++
        val now = timeProvider.currentTimeSeconds()
        val elapsed = now - lastFpsUpdateTime
        if (elapsed >= 1.0) {
            currentFps = (frameCount / elapsed).toFloat()
            frameCount = 0
            lastFpsUpdateTime = now
            updateMetrics()
        }
    }

    fun onFrameDropped() {
        droppedFrames++
        updateMetrics()
    }

    fun updateLatencies(inferenceMs: Float, navigationMs: Float, totalMs: Float) {
        val runtime = Runtime.getRuntime()
        val usedMemoryMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024f * 1024f)

        _metrics.value = PerformanceMetrics(
            cameraFps = currentFps,
            inferenceLatencyMs = inferenceMs,
            navigationLatencyMs = navigationMs,
            totalPipelineLatencyMs = totalMs,
            droppedFramesCount = droppedFrames,
            memoryUsageMb = usedMemoryMb
        )
    }

    private fun updateMetrics() {
        _metrics.value = _metrics.value.copy(
            cameraFps = currentFps,
            droppedFramesCount = droppedFrames
        )
    }
}
