# Complete Recursive Repository Inventory

**Audit Date**: September 21, 2026  
**Git Branch**: `master`  
**Git Commit**: `4b50c59` (plus audit additions)  
**Working Tree**: Verified  

---

## 1. Top-Level Directory Structure

```text
customer-aware-amr-demo/
├── .git/                                         (Git repository metadata)
├── .gitignore                                    (Version control ignore rules)
├── AMRAndroid/                                   (Native Android application project)
├── docs/                                         (Project architecture & audit documentation)
├── firmware/                                     (ESP32-S3 microcontroller firmware)
├── hardware/                                     (Standalone Arduino/ESP32 test sketches)
├── outputs/                                      (Output directory for media/recordings)
├── report/                                       (Project report assets)
├── requirements.txt                              (Python desktop dependencies)
├── scripts/                                      (Model export & utility scripts)
├── src/                                          (Python reference navigation pipeline)
├── tests/                                        (Python unit & scenario tests)
├── yolov8n-pose.onnx                             (Exported YOLO ONNX model - 12.9 MB)
└── yolov8n-pose.pt                               (PyTorch YOLO Pose weights - 6.5 MB)
```

---

## 2. Exhaustive File Listing & Classifications

### A. Python Navigation Subsystem (`src/`)
| File Path | Size | Classification | Purpose |
| :--- | :--- | :--- | :--- |
| [`src/config.py`](file:///c:/Users/kadam/Desktop/customer-aware-amr-demo/src/config.py) | 2,217 B | VERIFIED | Tunable parameters for zones, limits, weights, EMA alphas. |
| [`src/navigation_engine.py`](file:///c:/Users/kadam/Desktop/customer-aware-amr-demo/src/navigation_engine.py) | 8,920 B | VERIFIED | Core platform-independent navigation executive. |
| [`src/person_speed.py`](file:///c:/Users/kadam/Desktop/customer-aware-amr-demo/src/person_speed.py) | 2,514 B | VERIFIED | Velocity $(v_x, v_y)$ estimation normalized by $\Delta t$ with EMA. |
| [`src/motion_prediction.py`](file:///c:/Users/kadam/Desktop/customer-aware-amr-demo/src/motion_prediction.py) | 2,341 B | VERIFIED | Trajectory projection ($0.8s$ horizon). |
| [`src/crowd_analysis.py`](file:///c:/Users/kadam/Desktop/customer-aware-amr-demo/src/crowd_analysis.py) | 681 B | VERIFIED | `CrowdDensity` Enum & density classification. |
| [`src/navigation_zones.py`](file:///c:/Users/kadam/Desktop/customer-aware-amr-demo/src/navigation_zones.py) | 2,756 B | VERIFIED | Soft-margin spatial occupancy calculation. |
| [`src/navigation_planner.py`](file:///c:/Users/kadam/Desktop/customer-aware-amr-demo/src/navigation_planner.py) | 1,482 B | VERIFIED | Steering angle calculation, deadband, forward bias. |
| [`src/main.py`](file:///c:/Users/kadam/Desktop/customer-aware-amr-demo/src/main.py) | 5,612 B | VERIFIED | Desktop OpenCV/YOLO live demo & telemetry visualization. |
| [`src/path_planner.py`](file:///c:/Users/kadam/Desktop/customer-aware-amr-demo/src/path_planner.py) | 715 B | LEGACY | Legacy path overlay script. |
| [`src/pose_estimation.py`](file:///c:/Users/kadam/Desktop/customer-aware-amr-demo/src/pose_estimation.py) | 246 B | LEGACY | Legacy keypoint counter. |
| [`src/robot_decision.py`](file:///c:/Users/kadam/Desktop/customer-aware-amr-demo/src/robot_decision.py) | 309 B | LEGACY | Legacy decision helper. |
| [`src/utils.py`](file:///c:/Users/kadam/Desktop/customer-aware-amr-demo/src/utils.py) | 423 B | LEGACY | Legacy drawing utility. |

### B. Python Automated Tests (`tests/`)
| File Path | Size | Classification | Purpose |
| :--- | :--- | :--- | :--- |
| [`tests/test_navigation_engine.py`](file:///c:/Users/kadam/Desktop/customer-aware-amr-demo/tests/test_navigation_engine.py) | 9,842 B | VERIFIED | 17 unit, boundary, timing, and scenario test cases (100% pass). |

### C. Android Native Application (`AMRAndroid/`)
| File Path | Classification | Purpose |
| :--- | :--- | :--- |
| `AMRAndroid/settings.gradle.kts` | VERIFIED | Root Gradle settings & repo management. |
| `AMRAndroid/build.gradle.kts` | VERIFIED | Root plugin definitions. |
| `AMRAndroid/gradle.properties` | VERIFIED | JVM arguments & AndroidX flags. |
| `AMRAndroid/gradle/libs.versions.toml` | VERIFIED | Version catalog (Compose BOM, CameraX, TFLite, USB Serial). |
| `AMRAndroid/gradle/wrapper/gradle-wrapper.properties` | VERIFIED | Gradle 8.9 wrapper definition. |
| `AMRAndroid/app/build.gradle.kts` | VERIFIED | Application build configuration (SDK 34, Compose, Proguard). |
| `AMRAndroid/app/proguard-rules.pro` | VERIFIED | Proguard rules for TFLite and USB serial. |
| `AMRAndroid/app/src/main/AndroidManifest.xml` | VERIFIED | Camera, USB Host features, landscape orientation. |
| `AMRAndroid/app/src/main/res/xml/device_filter.xml` | VERIFIED | USB vendor/product filter for ESP32-S3 & UART chips. |
| `AMRAndroid/app/src/main/assets/models/yolov8n_pose.onnx` | VERIFIED | Packaged ONNX model asset (12.9 MB). |
| `AMRAndroid/app/src/main/java/com/amol/amr/MainActivity.kt` | VERIFIED | Activity lifecycle, screen-lock, permissions, Compose root. |
| `.../camera/CameraManager.kt` | VERIFIED | CameraX `ImageAnalysis` with `KEEP_ONLY_LATEST`. |
| `.../detection/TFLitePersonDetector.kt` | VERIFIED | TFLite / GPU runtime, 640x640 preprocessing, NMS. |
| `.../detection/YoloDetector.kt` | VERIFIED | Detection interface definition. |
| `.../detection/BoundingBox.kt` | VERIFIED | Bounding box & detection data models. |
| `.../tracking/PersonTracker.kt` | VERIFIED | IoU & centroid tracking with TTL pruning. |
| `.../navigation/NavigationEngine.kt` | VERIFIED | Kotlin port of Navigation Engine. |
| `.../navigation/NavigationConfig.kt` | VERIFIED | Kotlin port of Navigation Config. |
| `.../navigation/PersonSpeedTracker.kt` | VERIFIED | Kotlin port of Speed Tracker. |
| `.../navigation/MotionPredictor.kt` | VERIFIED | Kotlin port of Motion Predictor. |
| `.../navigation/NavigationZones.kt` | VERIFIED | Kotlin port of Navigation Zones. |
| `.../navigation/NavigationPlanner.kt` | VERIFIED | Kotlin port of Navigation Planner. |
| `.../navigation/CrowdAnalyzer.kt` | VERIFIED | Kotlin port of Crowd Analyzer. |
| `.../navigation/NavigationCommand.kt` | VERIFIED | Kotlin port of Navigation Command. |
| `.../communication/UsbSerialManager.kt` | VERIFIED | USB-CDC serial driver with auto-permissions & mutex writes. |
| `.../safety/SafetyController.kt` | VERIFIED | Multi-tier safety interlock & stale frame detector. |
| `.../pipeline/PerceptionPipeline.kt` | VERIFIED | Asynchronous bounded coroutine pipeline. |
| `.../ui/DashboardScreen.kt` | VERIFIED | Operator dashboard Compose UI. |
| `.../ui/CameraPreview.kt` | VERIFIED | CameraX preview with bounding box & trajectory overlay. |
| `.../ui/TelemetryPanel.kt` | VERIFIED | Real-time diagnostic HUD. |
| `.../ui/EmergencyStopButton.kt` | VERIFIED | Safety E-Stop button with tactile latch. |
| `.../util/PerformanceMonitor.kt` | VERIFIED | FPS, latency, and memory tracking. |
| `.../util/TimeProvider.kt` | VERIFIED | Monotonic system time provider. |
| `.../util/Logger.kt` | VERIFIED | Structured logger. |
| `AMRAndroid/app/src/test/.../NavigationEngineTest.kt` | VERIFIED | 10 Kotlin scenario unit tests. |

### D. Firmware & Hardware Subsystems (`firmware/`, `hardware/`)
| File Path | Size | Classification | Purpose |
| :--- | :--- | :--- | :--- |
| [`firmware/esp32_s3_motor_controller/esp32_s3_motor_controller.ino`](file:///c:/Users/kadam/Desktop/customer-aware-amr-demo/firmware/esp32_s3_motor_controller/esp32_s3_motor_controller.ino) | 4,286 B | VERIFIED | Active production firmware: USB-CDC parser, 500ms watchdog, differential PWM. |
| [`hardware/motor-esp32-driver-ENA-test.cpp`](file:///c:/Users/kadam/Desktop/customer-aware-amr-demo/hardware/motor-esp32-driver-ENA-test.cpp) | 2,361 B | STANDALONE TEST | L298N standalone motor driver bench test script. |
| [`hardware/ultasonic_autonomous_navigation-L298n.cpp`](file:///c:/Users/kadam/Desktop/customer-aware-amr-demo/hardware/ultasonic_autonomous_navigation-L298n.cpp) | 5,803 B | STANDALONE TEST | Standalone ultrasonic + servo obstacle avoidance with L298N. |
| [`hardware/ultrasonic-tfng-auto-test.cpp`](file:///c:/Users/kadam/Desktop/customer-aware-amr-demo/hardware/ultrasonic-tfng-auto-test.cpp) | 5,997 B | STANDALONE TEST | Standalone ultrasonic + servo obstacle avoidance with TB6612FNG. |

### E. Documentation & Reports (`docs/`)
| File Path | Classification | Purpose |
| :--- | :--- | :--- |
| `docs/COMPLETE_REPOSITORY_INVENTORY.md` | VERIFIED | Exhaustive directory and file audit. |
| `docs/ARCHITECTURE_VERIFICATION_REPORT.md` | VERIFIED | End-to-end architecture & data-flow verification. |
| `docs/PYTHON_NAVIGATION_AUDIT.md` | VERIFIED | Python navigation code & edge case audit. |
| `docs/KOTLIN_NAVIGATION_AUDIT.md` | VERIFIED | Kotlin Android navigation audit. |
| `docs/PYTHON_KOTLIN_PARITY_AUDIT.md` | VERIFIED | Line-by-line parity validation table. |
| `docs/MODEL_AND_INFERENCE_AUDIT.md` | VERIFIED | YOLO model export, ONNX/TFLite tensors audit. |
| `docs/IMU_INTEGRATION_AUDIT.md` | VERIFIED | MPU6050 IMU integration status & architecture. |
| `docs/ULTRASONIC_AND_SERVO_AUDIT.md` | VERIFIED | Ultrasonic & servo sensor audit. |
| `docs/ESP32_FIRMWARE_AUDIT.md` | VERIFIED | ESP32-S3 firmware, pinouts & watchdog audit. |
| `docs/ANDROID_ESP32_PROTOCOL_AUDIT.md` | VERIFIED | USB serial protocol compatibility audit. |
| `docs/SAFETY_AND_WATCHDOG_AUDIT.md` | VERIFIED | Multi-layer safety & watchdog audit. |
| `docs/COMPLETE_TEST_EXECUTION_REPORT.md` | VERIFIED | Test execution results & evidence. |
| `docs/BUILD_AND_DEPLOYMENT_AUDIT.md` | VERIFIED | Build configurations, SDKs & deployment steps. |
| `docs/PHYSICAL_VALIDATION_PROCEDURE.md` | VERIFIED | Step-by-step physical hardware bring-up plan. |
| `docs/PERFORMANCE_AUDIT.md` | VERIFIED | Performance metrics, latencies & bottlenecks. |
| `docs/FINAL_PROJECT_AUDIT_REPORT.md` | VERIFIED | Master executive audit report. |
