# Navigation Data Contract & Interface Specification

## 1. Input Data Contracts

### A. Raw Camera Frame Contract
* **Source**: Android CameraX `ImageAnalysis` analyzer.
* **Resolution**: Recommending $640 \times 480$ or $640 \times 640$ (YOLO standard).
* **Pixel Format**: YUV_420_888 converted/rotated to RGB Bitmap or direct direct FloatBuffer.
* **Coordinate System**: Image space $[0, W] \times [0, H]$ with origin $(0,0)$ at top-left.

### B. `PersonDetection` Contract
```kotlin
data class PersonDetection(
    val x1: Float,                  // Left bound (pixels)
    val y1: Float,                  // Top bound (pixels)
    val x2: Float,                  // Right bound (pixels)
    val y2: Float,                  // Bottom bound (pixels)
    val trackId: Int? = null,       // Tracking ID assigned by tracker (e.g. ByteTrack / Sort / Kalman)
    val confidence: Float = 1.0f,   // Detection confidence [0.0 .. 1.0]
    val keypoints: FloatArray? = null // Optional pose keypoints
) {
    val centerX: Float get() = (x1 + x2) / 2.0f
    val centerY: Float get() = (y1 + y2) / 2.0f
    val width: Float get() = maxOf(0.0f, x2 - x1)
    val height: Float get() = maxOf(0.0f, y2 - y1)
    val area: Float get() = width * height
}
```

---

## 2. Internal State Contracts

### A. `PersonState` Contract
```kotlin
data class PersonState(
    val trackId: Int,
    val cx: Float,
    val cy: Float,
    val vx: Float,                  // Horizontal velocity (pixels/sec)
    val vy: Float,                  // Vertical velocity (pixels/sec)
    val speed: Float,               // Magnitude (pixels/sec)
    val predX: Int,                 // Projected X at T_pred = 0.8s
    val predY: Int,                 // Projected Y at T_pred = 0.8s
    val bottomYNorm: Float,         // y2 / frameHeight [0.0 .. 1.0]
    val areaRatio: Float,           // area / (frameWidth * frameHeight)
    val isInCenterCorridor: Boolean,
    val isPredictedInPath: Boolean,
    val proximityRisk: Float
)
```

### B. `CrowdDensity` Enum
```kotlin
enum class CrowdDensity(val value: String) {
    EMPTY("EMPTY"),
    LOW("LOW"),
    MEDIUM("MEDIUM"),
    HIGH("HIGH")
}
```

---

## 3. Output Command Contract

### A. `NavigationCommand` Contract
```kotlin
data class NavigationCommand(
    val steering: Float,            // Steering angle in degrees [-45.0 .. +45.0] (+ = Left, - = Right)
    val speed: Float,               // Normalized speed scale [0.0 .. 1.0]
    val emergencyStop: Boolean,     // True if robot must halt immediately
    val reason: String,             // Human-readable rationale for telemetry & logging
    val timestampSeconds: Double,   // Monotonic timestamp when decision was computed
    val diagnostics: Map<String, Any> = emptyMap()
) {
    /** Formats command as a newline-delimited ASCII string for ESP32-S3 serial stream */
    fun toSerialString(): String {
        return if (emergencyStop || speed <= 0.0f) {
            "STOP\n"
        } else {
            "STEER:${Math.round(steering)} SPEED:${String.format(java.util.Locale.US, "%.2f", speed)}\n"
        }
    }

    /** Formats command as structured CSV for firmware parsing */
    fun toProtocolPacket(sequenceNumber: Long): String {
        val dirStr = when {
            emergencyStop || speed <= 0.0f -> "STOP"
            steering > 5.0f -> "LEFT"
            steering < -5.0f -> "RIGHT"
            else -> "FORWARD"
        }
        val speedInt = Math.round(speed * 255)
        val steerInt = Math.round(steering)
        val stopFlag = if (emergencyStop) 1 else 0
        return "CMD,$dirStr,$speedInt,$steerInt,$stopFlag,$sequenceNumber\n"
    }
}
```
