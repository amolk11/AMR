# AMRAndroid — Customer-Aware AMR Navigation Application

A production-grade native Android application running on-device YOLO person perception and real-time customer-aware navigation logic for an Autonomous Mobile Robot (AMR). Communicates with an ESP32-S3 motor controller over USB-C OTG serial communication.

---

## 🏗️ Architecture & Pipeline

```
[Android Camera]
       ↓ (CameraX ImageAnalysis - KEEP_ONLY_LATEST)
[TFLite YOLOv8 Person Detector] (GPU / NNAPI Accelerated)
       ↓
[IoU & Distance Person Tracker] (Stable IDs & TTL Cleanup)
       ↓
[Time-Normalized Speed Tracker] (Δt normalized, EMA smoothed)
       ↓
[Motion Predictor] (0.8s Constant Velocity Horizon)
       ↓
[Soft-Zone & Multi-Person Collision Risk Evaluator]
       ↓
[Navigation Engine] (Rate-limited speed & smoothed steering)
       ↓
[Safety Controller] (Watchdog, Stale Frame Detection, E-Stop Interlock)
       ↓
[USB-C Serial Manager] (CDC/ACM @ 115200 baud)
       ↓ (Type-C OTG Cable)
[ESP32-S3 Motor Controller] ──> [L298N H-Bridge] ──> [DC Motors]
```

---

## 📱 Hardware & Device Requirements

1. **Android Smartphone**:
   * Android 8.0 (API 26) or higher (target SDK 34).
   * USB-OTG / USB Host support.
   * Camera with landscape mounting bracket on the AMR chassis.
2. **Microcontroller & Electronics**:
   * **ESP32-S3** (Native USB or CP2102/CH340 USB-UART).
   * **Motor Driver**: L298N or Dual H-Bridge driver.
   * **Power Supply**: Separate 7.4V–12V battery pack for motors and common ground.
   * **Cable**: USB Type-C to Type-C OTG cable.

---

## 🔌 Wiring & Pin Connections

| ESP32-S3 Pin | L298N Pin | Function |
| :--- | :--- | :--- |
| **GPIO 5** | `IN1` | Left Motor Direction A |
| **GPIO 6** | `IN2` | Left Motor Direction B |
| **GPIO 7** | `IN3` | Right Motor Direction A |
| **GPIO 8** | `IN4` | Right Motor Direction B |
| **GPIO 36** | `ENA` | Left Motor PWM Speed |
| **GPIO 37** | `ENB` | Right Motor PWM Speed |
| **GND** | `GND` | Common Ground with Battery & Driver |

---

## 🚀 Getting Started & Build Instructions

### 1. Model Export (TFLite)
Export the YOLO model from the project root using Python:
```bash
python scripts/export_yolo_tflite.py --model yolov8n-pose.pt --imgsz 640
```
This places `yolov8n_float32.tflite` into `AMRAndroid/app/src/main/assets/models/`.

### 2. Building with Android Studio
1. Open Android Studio.
2. Select **Open** and choose the `AMRAndroid` directory.
3. Allow Gradle to sync dependencies.
4. Connect your Android device via USB debugging.
5. Click **Run 'app'** (`Shift + F10`).

### 3. Flash ESP32-S3 Firmware
1. Open `firmware/esp32_s3_motor_controller/esp32_s3_motor_controller.ino` in Arduino IDE.
2. Select Board: **ESP32S3 Dev Module** (USB Mode: **Hardware CDC and JTAG**).
3. Click **Upload**.

---

## 🛡️ Safety Systems & Failsafes

* **Hardware Communication Watchdog**: ESP32-S3 automatically stops motors if no command packet arrives within **500ms**.
* **Direct Frontal Proximity E-Stop**: The navigation engine halts immediately when a person is detected in close front range ($y_2 \ge 0.78$) or occupies $>18\%$ of the screen.
* **Camera Staleness Monitor**: Halts if camera frames stop arriving for $>500ms$.
* **Operator Emergency Stop**: Instant touchscreen E-Stop button with safety latch.
