# Kotlin and Android Navigation Audit

**Audit Target**: `AMRAndroid/app/src/main/java/com/amol/amr/navigation/` and `AMRAndroid/app/src/main/java/com/amol/amr/pipeline/`  
**Test Target**: `AMRAndroid/app/src/test/java/com/amol/amr/navigation/NavigationEngineTest.kt`  

---

## 1. Subsystem Implementation Verification

### A. CameraX Pipeline (`CameraManager.kt`)
* **Backpressure**: Configured with `ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST`.
* **Resource Safety**: Uses `try ... finally { imageProxy.close() }` to guarantee no buffer pool starvation.
* **Format**: Converts frames via `ImageProxy.toBitmap()` with rotation degree normalization.

### B. Inference Engine (`TFLitePersonDetector.kt`)
* **Hardware Acceleration**: Automatic GPU Delegate with fallback to NNAPI or multi-threaded CPU.
* **Memory Management**: Zero-allocation per-frame DirectByteBuffer (`1 * 640 * 640 * 3 * 4` bytes).
* **NMS**: Implemented IoU filtering with `iouThreshold = 0.50f` and `confThreshold = 0.45f`.

### C. Tracker & Velocity Tracking (`PersonTracker.kt`, `PersonSpeedTracker.kt`)
* **Track Association**: Hybrid IoU + Centroid Euclidean distance score.
* **Velocity Estimation**: Exact time normalization $\Delta t$ and EMA low-pass filtering ($\alpha = 0.40$).
* **Memory Safety**: `Iterator.remove()` TTL cleanup preventing `ConcurrentModificationException`.

### D. Navigation Core (`NavigationEngine.kt`)
* **Parity**: Identical mathematics for soft zone occupancies, collision risk scoring, distance-decay speed scaling, acceleration/deceleration ramping, and recovery hysteresis.

---

## 2. Kotlin Audit Findings

| Subsystem | Audit Item | Result |
| :--- | :--- | :--- |
| **Concurrency** | Bounded channel buffer with `DROP_OLDEST` | **VERIFIED** |
| **Thread Safety** | Serial writes protected by `Mutex.withLock` | **VERIFIED** |
| **Exception Handling** | Camera & USB disconnects caught gracefully | **VERIFIED** |
| **Lifecycle Safety** | UI binds to ViewModel / CoroutineScope | **VERIFIED** |
