# YOLO + Customer-Aware AMR Hardware Integration Report

**Auditor / Integration Engineer**: Senior Robotics, Embedded & Computer Vision QA  
**Baseline System**: Physically Working Autonomous Mobile Robot (AMR)  
**Target Milestone**: Vision + Ultrasonic + IMU + Motor Control Hardware Integration  

---

## 1. Existing AMR Architecture (Baseline Hardware)
The existing physical robot is a differential-drive AMR equipped with:
* **Microcontroller**: ESP32-S3.
* **Motor Driver**: L298N Dual H-Bridge connected to Left Motor (`IN1=14, IN2=15, ENA=16`) and Right Motor (`IN3=17, IN4=18, ENB=4`).
* **Close-Range Obstacle Detection**: HC-SR04 Ultrasonic sensor (`TRIG=21, ECHO=47`).
* **Servo Scanner**: SG90/MG995 servo on `SERVO_PIN=13`.
* **Inertial Measurement Unit**: MPU6050 6-DOF IMU on I²C (`SDA=8, SCL=9, ADDR=0x68`).

---

## 2. YOLO Vision Architecture (Added Layer)
The vision subsystem runs natively on an Android smartphone:
* **Camera Input**: CameraX `ImageAnalysis` capturing $640 \times 480$ frames with `STRATEGY_KEEP_ONLY_LATEST`.
* **Inference Runtime**: Mobile YOLOv8 Pose / Detection model with GPU/NNAPI acceleration.
* **Customer Tracking & Kinematics**: IoU tracking, $\Delta t$-normalized speed estimation ($px/s$), and constant-velocity trajectory projection over $0.8s$.
* **Navigation Planner**: Soft-margin zone partitioning ($1/3, 2/3$), steering deadband ($\pm 2.5^\circ$), distance-decay speed scaling, and acceleration/deceleration ramping.

---

## 3. Integration Architecture
The system integrates without breaking or replacing existing hardware sensors:
* **Android Smartphone**: Serves as the high-level perception and supervisory navigation planner.
* **ESP32-S3**: Serves as the embedded safety arbiter, sensor reader, and low-level motor driver.
* **Physical Link**: Direct USB Type-C to Type-C OTG cable transmitting ASCII / CSV control packets.

---

## 4. Existing Sensor Logic Preserved

| Subsystem | Baseline Hardware Pins | Preserved Behavior |
| :--- | :--- | :--- |
| **Ultrasonic HC-SR04** | `TRIG=21`, `ECHO=47` | 25ms timeout reading, $<20\text{ cm}$ safety threshold preserved. |
| **Scanning Servo** | `SERVO_PIN=13` | Centered at $90^\circ$ for forward monitoring; scanning capability intact. |
| **MPU6050 IMU** | `SDA=8`, `SCL=9` | $400\text{ kHz}$ I²C bus reading accelerometer/gyro, $>25^\circ$ tilt safety trip. |
| **L298N Driver** | `14, 15, 17, 18, 16, 4` | $5\text{ kHz}$ 8-bit PWM channels 0 & 1 with forward/reverse direction lines. |

---

## 5. YOLO Data Flow
$$\text{CameraX Frame} \to \text{YOLOv8} \to \text{Tracker} \to \text{Speed Tracker} \to \text{Motion Predictor} \to \text{NavigationEngine} \to \text{SafetyController} \to \text{UsbSerialManager}$$

---

## 6. Android $\to$ ESP32 Protocol
* **Compact ASCII**: `STEER:<angle_deg> SPEED:<scale_0_to_1>\n` and `STOP\n`.
* **Structured CSV**: `CMD,<DIRECTION>,<PWM_0_255>,<STEER_DEG>,<STOP_FLAG>,<SEQ>\n`.
* **Framing**: Newline `\n` terminated, 115200 baud / native USB-CDC.

---

## 7. Safety & Command Priority Hierarchy

```text
[Highest Priority]
1. EMERGENCY STOP (Touch UI / E-Stop Command / Manual Override)
2. Ultrasonic Hardware Safety Bumper (Front Distance < 20 cm)
3. Communication Watchdog (No packet received for > 500 ms)
4. MPU6050 IMU Tilt / Collision Fault (Pitch / Roll > 25°)
5. Android YOLO Customer-Aware Navigation Commands (Steering & Speed Scale)
6. Normal Differential Drive Motor Movement
[Lowest Priority]
```
> **Safety Guarantee**: A YOLO `"FORWARD"` command **NEVER** overrides a physical ultrasonic obstacle or IMU tilt cutoff.

---

## 8. Safety & Failure Modes Matrix

| Failure Mode | Detection | System Action | Result |
| :--- | :--- | :--- | :--- |
| **USB Cable Disconnected** | Hardware D+/D- loss | ESP32-S3 500ms Watchdog halts PWM to 0 | Safe Stop |
| **Android App Freezes** | Serial packet silence | ESP32-S3 500ms Watchdog halts PWM to 0 | Safe Stop |
| **Physical Obstacle in Path** | HC-SR04 $<20\text{ cm}$ | Hardware safety interlock cuts motor power | Safe Stop |
| **Camera Obstructed / Frozen** | `SafetyController.kt` ($>0.5s$) | Overrides command to `STOP\n` | Safe Stop |
| **Robot Tip / Severe Tilt** | MPU6050 tilt $>25^\circ$ | Hardware safety interlock cuts motor power | Safe Stop |

---

## 9. Sensor Interaction & Fusion
* **High-Level Vision (Android)**: Detects customers at $1.0\text{ m} - 5.0\text{ m}$ range, predicts movement vectors, modulates robot speed, and chooses clear supermarket aisles.
* **Low-Level Hardware (ESP32-S3)**: Acts as an independent hard-real-time safety shield. If a blind-spot object or customer suddenly steps within $20\text{ cm}$, the ultrasonic sensor overrides the Android command and halts the robot.

---

## 10. Test Results

* **Python Automated Tests**: **17 / 17 Passed (100%)** — Unit, scenario, rate-limiting, and timing invariance.
* **Kotlin Parity Tests**: **10 / 10 Verified** — Identical navigation outputs and state transitions.
* **Model Export**: **VERIFIED** — `yolov8n_pose.onnx` exported and packaged (12.9 MB).
* **Firmware Validation**: **VERIFIED IN CODE** — Multi-tier priority and pin matching.
* **Physical Hardware Tests**: **NOT EXECUTED** — Pending bench connection of physical robot.

---

## 11. Remaining Issues & Calibration
1. **Camera Tilt Angle**: Calibrate vertical FOV mounting angle on physical chassis to align camera ground plane with physical distance.
2. **ECHO Pin Voltage**: Ensure level shifter / resistor divider is fitted on HC-SR04 ECHO pin to protect ESP32-S3 GPIO 47.

---

## 12. Physical Testing Procedure

Follow the strict 4-stage hardware bring-up in [PHYSICAL_VALIDATION_PROCEDURE.md](file:///c:/Users/kadam/Desktop/customer-aware-amr-demo/docs/PHYSICAL_VALIDATION_PROCEDURE.md):
* **Stage A**: Sensors & Microcontroller without motors (Serial Monitor verification).
* **Stage B**: Android USB OTG communication without motors (Command & E-Stop flow).
* **Stage C**: Wheels elevated off ground (Direction, differential turning & watchdog check).
* **Stage D**: Controlled floor navigation (Live customer-aware restocking test).
