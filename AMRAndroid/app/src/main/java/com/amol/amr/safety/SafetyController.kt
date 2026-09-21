package com.amol.amr.safety

import com.amol.amr.navigation.NavigationCommand
import com.amol.amr.util.Logger
import com.amol.amr.util.TimeProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SafetyState(
    val isUserEmergencyStopActive: Boolean = false,
    val isFrameStale: Boolean = false,
    val isInferenceFailed: Boolean = false,
    val isUsbDisconnected: Boolean = false,
    val activeFaultMessage: String? = null
) {
    val isSafeToMove: Boolean
        get() = !isUserEmergencyStopActive && !isFrameStale && !isInferenceFailed && !isUsbDisconnected
}

class SafetyController(
    private val timeProvider: TimeProvider,
    private val maxFrameStalenessSeconds: Double = 0.50,
    private val maxInferenceFailureCount: Int = 3
) {
    private val _safetyState = MutableStateFlow(SafetyState())
    val safetyState: StateFlow<SafetyState> = _safetyState.asStateFlow()

    private var lastValidFrameTime = timeProvider.currentTimeSeconds()
    private var consecutiveInferenceFailures = 0

    fun setUserEmergencyStop(active: Boolean) {
        _safetyState.value = _safetyState.value.copy(
            isUserEmergencyStopActive = active,
            activeFaultMessage = if (active) "MANUAL_EMERGENCY_STOP" else null
        )
    }

    fun onFrameReceived() {
        lastValidFrameTime = timeProvider.currentTimeSeconds()
        if (_safetyState.value.isFrameStale) {
            _safetyState.value = _safetyState.value.copy(isFrameStale = false)
        }
    }

    fun onInferenceSuccess() {
        consecutiveInferenceFailures = 0
        if (_safetyState.value.isInferenceFailed) {
            _safetyState.value = _safetyState.value.copy(isInferenceFailed = false)
        }
    }

    fun onInferenceFailure(error: Throwable) {
        consecutiveInferenceFailures++
        if (consecutiveInferenceFailures >= maxInferenceFailureCount) {
            Logger.e("SafetyController", "Multiple consecutive inference failures", error)
            _safetyState.value = _safetyState.value.copy(
                isInferenceFailed = true,
                activeFaultMessage = "INFERENCE_FAILURE"
            )
        }
    }

    fun onUsbConnectionChanged(isConnected: Boolean) {
        _safetyState.value = _safetyState.value.copy(
            isUsbDisconnected = !isConnected,
            activeFaultMessage = if (!isConnected) "USB_DISCONNECTED" else null
        )
    }

    fun validateAndEnforceSafety(command: NavigationCommand): NavigationCommand {
        val now = timeProvider.currentTimeSeconds()
        if (now - lastValidFrameTime > maxFrameStalenessSeconds) {
            _safetyState.value = _safetyState.value.copy(
                isFrameStale = true,
                activeFaultMessage = "CAMERA_FRAME_STALE"
            )
        }

        val state = _safetyState.value
        if (!state.isSafeToMove) {
            val reason = state.activeFaultMessage ?: "SAFETY_INTERLOCK_HALT"
            return NavigationCommand(
                steering = 0.0f,
                speed = 0.0f,
                emergencyStop = true,
                reason = "SAFETY_OVERRIDE: $reason",
                diagnostics = command.diagnostics
            )
        }

        return command
    }
}
