# YOLO + Ultrasonic + IMU AMR Integration Data Flow

## 1. Complete End-to-End Perception & Control Pipeline

```
[Android Smartphone Camera]
       ↓ (CameraX ImageAnalysis @ 30 FPS / KEEP_ONLY_LATEST)
[TFLite / ONNX YOLOv8 Person Detector] (GPU / NNAPI accelerated)
       ↓ (Detections: BoundingBox, Confidence)
[IoU + Centroid Person Tracker] (Stable IDs & TTL Pruning)
       ↓
[Time-Normalized Speed Tracker] (Δt normalized, EMA smoothed)
       ↓
[Motion Trajectory Predictor] (0.8s Constant Velocity Horizon)
       ↓
[Soft-Zone & Customer Proximity Evaluator]
       ↓
[Platform-Independent Navigation Engine] (Rate-limited speed & smoothed steering)
       ↓
[Android Safety Controller] (Stale-Frame timeout 0.5s, E-Stop latch)
       ↓
[Navigation Command Packet] ("STEER:X SPEED:Y\n" or "CMD,DIR,PWM,STEER,STOP,SEQ\n")
       ↓
[USB-C OTG Serial Link] (115200 Baud / Full-Speed USB-CDC)
       ↓
┌────────────────────────────────────────────────────────────────────────┐
│                   ESP32-S3 EMBEDDED SAFETY ARBITER                     │
│                                                                        │
│  [Hardware Safety Priority Hierarchy]                                 │
│                                                                        │
│  [Tier 1] EMERGENCY STOP REQUESTED? ──────────────────────► [HALT]     │
│             │ (No)                                                     │
│             ▼                                                          │
│  [Tier 2] ULTRASONIC CLOSE OBSTACLE? (Distance < 20cm) ──► [HALT]     │
│             │ (No)                                                     │
│             ▼                                                          │
│  [Tier 3] WATCHDOG TIMEOUT EXCEEDED? (Age > 500ms) ──────► [HALT]     │
│             │ (No)                                                     │
│             ▼                                                          │
│  [Tier 4] MPU6050 TILT FAULT? (Pitch/Roll > 25°) ────────► [HALT]     │
│             │ (No)                                                     │
│             ▼                                                          │
│  [Tier 5] EXECUTE ANDROID YOLO NAVIGATION COMMAND                      │
│             │ (Differential Drive PWM Mixing)                          │
│             ▼                                                          │
│  [Tier 6] L298N DUAL H-BRIDGE MOTOR DRIVER                             │
└──────────────────────────────────┬─────────────────────────────────────┘
                                   │ (PWM Signals ENA, ENB, IN1-IN4)
                                   ▼
                   [LEFT & RIGHT DC DRIVE MOTORS]
```

---

## 2. Data Contract & Hardware Mapping

| Stage | Input | Output | Hardware/Software Interface |
| :--- | :--- | :--- | :--- |
| **Vision Perception** | Camera Frame ($640 \times 480$) | `PersonDetection(x1, y1, x2, y2, trackId, conf)` | CameraX $\to$ YOLOv8 $\to$ Android CPU/GPU |
| **Kinematics & Risk** | Tracked Bounding Boxes | `PersonState(vx, vy, speed, predX, predY, risk)` | `PersonSpeedTracker.kt` $\to$ `MotionPredictor.kt` |
| **Navigation Decision** | Customer positions & velocities | `NavigationCommand(steering, speed, emergencyStop)` | `NavigationEngine.kt` |
| **Serial Transport** | Formatted command string | Serial byte stream | `UsbSerialManager.kt` via Type-C OTG |
| **Hardware Arbiter** | Serial byte stream + HC-SR04 + MPU6050 | Mixed PWM duty cycles ($0..255$) | `esp32_s3_motor_controller.ino` |
| **Physical Actuation** | GPIO 14, 15, 17, 18, 16, 4 | Wheel rotation | L298N Driver $\to$ DC Motors |
