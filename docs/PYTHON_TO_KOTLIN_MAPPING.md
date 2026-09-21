# Python to Kotlin Architecture & API Mapping

This document provides a line-by-line and class-by-class mapping of the reference Python navigation engine to the target Kotlin Android implementation.

## 1. Class & File Mapping

| Python Module (`src/`) | Kotlin Class (`com.amol.amr.navigation`) | Responsibilities & Mappings |
| :--- | :--- | :--- |
| [`src/config.py`](file:///c:/Users/kadam/Desktop/customer-aware-amr-demo/src/config.py) (`NavigationConfig`) | `NavigationConfig.kt` (data class) | Direct 1:1 parameter mapping with default parameters. |
| [`src/crowd_analysis.py`](file:///c:/Users/kadam/Desktop/customer-aware-amr-demo/src/crowd_analysis.py) (`CrowdDensity`, `analyze_crowd_density`) | `CrowdAnalyzer.kt` | Enum `CrowdDensity` and companion function `analyze(peopleCount: Int): CrowdDensity`. |
| [`src/person_speed.py`](file:///c:/Users/kadam/Desktop/customer-aware-amr-demo/src/person_speed.py) (`PersonSpeedTracker`) | `PersonSpeedTracker.kt` | Object with `MutableMap<Int, TrackData>`, $\Delta t$ normalization, EMA filtering, and `cleanupStaleTracks(currentTime)`. |
| [`src/motion_prediction.py`](file:///c:/Users/kadam/Desktop/customer-aware-amr-demo/src/motion_prediction.py) (`MotionPredictor`) | `MotionPredictor.kt` | Projects $(p_x, p_y)$ over $T_{pred} = 0.8s$ horizon using smoothed velocity vectors. |
| [`src/navigation_zones.py`](file:///c:/Users/kadam/Desktop/customer-aware-amr-demo/src/navigation_zones.py) (`compute_soft_zone_weights`, `compute_zone_occupancies_from_boxes`) | `NavigationZones.kt` | Soft 5% transition margin calculations across LEFT, CENTER, RIGHT. |
| [`src/navigation_planner.py`](file:///c:/Users/kadam/Desktop/customer-aware-amr-demo/src/navigation_planner.py) (`navigation_planner`, `steering_from_zones`) | `NavigationPlanner.kt` | Cost evaluation, deadband enforcement ($\pm 2.5^\circ$), angle clamping ($\pm 45^\circ$), forward priority ($-1\mu$). |
| [`src/navigation_engine.py`](file:///c:/Users/kadam/Desktop/customer-aware-amr-demo/src/navigation_engine.py) (`NavigationEngine`, `PersonDetection`, `NavigationCommand`) | `NavigationEngine.kt` | High-level orchestrator; evaluates multi-person collision risk, manages acceleration ramping, and enforces stop-recovery hysteresis. |

---

## 2. API Signature Equivalence

### A. Navigation Engine Interface
```python
# Python
class NavigationEngine:
    def process(
        self,
        detections: List[PersonDetection],
        frame_width: int,
        frame_height: int,
        current_time: Optional[float] = None
    ) -> NavigationCommand:
        ...
```

```kotlin
// Kotlin
class NavigationEngine(
    val config: NavigationConfig = NavigationConfig()
) {
    fun process(
        detections: List<PersonDetection>,
        frameWidth: Int,
        frameHeight: Int,
        currentTimeSeconds: Double = System.currentTimeMillis() / 1000.0
    ): NavigationCommand {
        ...
    }
}
```

### B. Speed Tracking
```python
# Python
class PersonSpeedTracker:
    def update(self, track_id: int, center: Tuple[float, float], current_time: float) -> Tuple[float, float, float]:
        # returns (speed, vx, vy)
```

```kotlin
// Kotlin
class PersonSpeedTracker(
    private val emaAlpha: Float = 0.4f,
    private val maxTtlSeconds: Double = 2.0
) {
    data class VelocityResult(val speed: Float, val vx: Float, val vy: Float)

    fun update(trackId: Int, cx: Float, cy: Float, currentTimeSeconds: Double): VelocityResult {
        ...
    }
}
```

### C. Motion Prediction
```python
# Python
class MotionPredictor:
    def predict(self, track_id: int, center: Tuple[float, float], current_time: float, vx: float, vy: float) -> Tuple[int, int]:
        # returns (pred_x, pred_y)
```

```kotlin
// Kotlin
class MotionPredictor(
    private val horizonSeconds: Float = 0.8f,
    private val emaAlpha: Float = 0.4f,
    private val maxTtlSeconds: Double = 2.0
) {
    fun predict(trackId: Int, cx: Float, cy: Float, currentTimeSeconds: Double, vx: Float, vy: Float): Pair<Int, Int> {
        ...
    }
}
```

---

## 3. Numerical Precision & Platform Considerations

1. **Floating Point Consistency**: Both Python 3 `float` (64-bit IEEE 754) and Kotlin `Double` maintain exact numerical precision for timestamps and coordinates. Kotlin `Float` (32-bit) is used for frame bounding box calculations without loss of precision.
2. **Time Sources**:
   * Python uses `time.time()` (Unix epoch seconds).
   * Kotlin uses `SystemClock.elapsedRealtimeNanos() / 1e9` or `System.currentTimeMillis() / 1000.0` for monotonic elapsed seconds, immune to wall-clock time shifts.
3. **Memory Management**:
   * Stale track IDs in Kotlin are purged in `cleanupStaleTracks()` via `Iterator.remove()` to prevent `ConcurrentModificationException` and allocation overhead.
