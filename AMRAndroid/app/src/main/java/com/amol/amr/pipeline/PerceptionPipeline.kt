package com.amol.amr.pipeline

import android.content.Context
import android.os.SystemClock
import com.amol.amr.camera.CameraFrame
import com.amol.amr.communication.UsbConnectionState
import com.amol.amr.communication.UsbSerialManager
import com.amol.amr.detection.DetectionFrame
import com.amol.amr.detection.TFLitePersonDetector
import com.amol.amr.detection.YoloDetector
import com.amol.amr.navigation.NavigationCommand
import com.amol.amr.navigation.NavigationConfig
import com.amol.amr.navigation.NavigationEngine
import com.amol.amr.navigation.PersonDetection
import com.amol.amr.safety.SafetyController
import com.amol.amr.safety.SafetyState
import com.amol.amr.tracking.PersonTracker
import com.amol.amr.util.Logger
import com.amol.amr.util.PerformanceMonitor
import com.amol.amr.util.SystemTimeProvider
import com.amol.amr.util.TimeProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class PipelineUiState(
    val latestCommand: NavigationCommand? = null,
    val latestDetections: List<PersonDetection> = emptyMap<String, Any>().let { emptyList() },
    val safetyState: SafetyState = SafetyState(),
    val usbConnectionState: UsbConnectionState = UsbConnectionState.DISCONNECTED,
    val isAutonomousActive: Boolean = false
)

class PerceptionPipeline(
    private val context: Context,
    private val timeProvider: TimeProvider = SystemTimeProvider(),
    private val config: NavigationConfig = NavigationConfig()
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val performanceMonitor = PerformanceMonitor(timeProvider)
    val safetyController = SafetyController(timeProvider)
    val usbManager = UsbSerialManager(context)

    private val tracker = PersonTracker()
    private val navigationEngine = NavigationEngine(config)
    private var detector: YoloDetector? = null

    private val _uiState = MutableStateFlow(PipelineUiState())
    val uiState: StateFlow<PipelineUiState> = _uiState.asStateFlow()

    // Bounded channel to prevent queue buildup (drops oldest if inference is busy)
    private val frameChannel = MutableSharedFlow<CameraFrame>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private val inferenceMutex = Mutex()
    private var isAutonomousRunning = false

    init {
        initializeDetector()
        startPipelineWorker()
        observeUsbState()
    }

    private fun initializeDetector() {
        try {
            detector = TFLitePersonDetector(context)
        } catch (e: Exception) {
            Logger.e("PerceptionPipeline", "Failed to initialize detector", e)
        }
    }

    private fun observeUsbState() {
        scope.launch {
            usbManager.connectionState.collect { state ->
                safetyController.onUsbConnectionChanged(state == UsbConnectionState.CONNECTED)
                _uiState.value = _uiState.value.copy(usbConnectionState = state)
            }
        }
    }

    fun onNewCameraFrame(frame: CameraFrame) {
        performanceMonitor.onFrameCaptured()
        safetyController.onFrameReceived()
        val emitted = frameChannel.tryEmit(frame)
        if (!emitted) {
            performanceMonitor.onFrameDropped()
        }
    }

    private fun startPipelineWorker() {
        scope.launch {
            frameChannel.collect { frame ->
                processSingleFrame(frame)
            }
        }
    }

    private suspend fun processSingleFrame(frame: CameraFrame) {
        if (!inferenceMutex.tryLock()) {
            performanceMonitor.onFrameDropped()
            return
        }

        try {
            val startTime = SystemClock.elapsedRealtime()

            // 1. Inference
            val det: YoloDetector = detector ?: return
            val detectionFrame: DetectionFrame = try {
                det.detect(frame.bitmap, frame.rotationDegrees)
            } catch (e: Exception) {
                safetyController.onInferenceFailure(e)
                return
            }
            safetyController.onInferenceSuccess()

            // 2. Tracking
            val trackedDetections = tracker.update(detectionFrame.detections)

            // 3. Navigation Engine
            val navStartTime = SystemClock.elapsedRealtime()
            val nowSeconds = timeProvider.currentTimeSeconds()
            val rawCommand = navigationEngine.process(
                detections = trackedDetections,
                frameWidth = detectionFrame.frameWidth,
                frameHeight = detectionFrame.frameHeight,
                currentTime = nowSeconds
            )
            val navDuration = (SystemClock.elapsedRealtime() - navStartTime).toFloat()

            // 4. Safety Validation
            val safeCommand = safetyController.validateAndEnforceSafety(rawCommand)
            val totalDuration = (SystemClock.elapsedRealtime() - startTime).toFloat()

            // 5. Update Metrics
            performanceMonitor.updateLatencies(
                inferenceMs = detectionFrame.inferenceTimeMs,
                navigationMs = navDuration,
                totalMs = totalDuration
            )

            // 6. Transmit Command over USB Serial if Autonomous is Enabled
            if (isAutonomousRunning) {
                usbManager.sendCommand(safeCommand)
            }

            // 7. Update UI State
            _uiState.value = _uiState.value.copy(
                latestCommand = safeCommand,
                latestDetections = trackedDetections,
                safetyState = safetyController.safetyState.value,
                isAutonomousActive = isAutonomousRunning
            )

        } finally {
            inferenceMutex.unlock()
        }
    }

    fun setAutonomousMode(enabled: Boolean) {
        isAutonomousRunning = enabled
        _uiState.value = _uiState.value.copy(isAutonomousActive = enabled)
        if (!enabled) {
            scope.launch {
                usbManager.sendCommand(
                    NavigationCommand(0.0f, 0.0f, true, "AUTONOMOUS_DISABLED")
                )
            }
        }
    }

    fun triggerEmergencyStop(active: Boolean) {
        safetyController.setUserEmergencyStop(active)
        if (active) {
            scope.launch {
                usbManager.sendCommand(
                    NavigationCommand(0.0f, 0.0f, true, "MANUAL_EMERGENCY_STOP")
                )
            }
        }
    }

    fun connectUsb(): Boolean = usbManager.connect()

    fun release() {
        setAutonomousMode(false)
        usbManager.release()
        detector?.close()
    }
}
