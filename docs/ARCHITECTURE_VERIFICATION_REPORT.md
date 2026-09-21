# Architecture Verification Report

## 1. System Architecture Diagram

```mermaid
graph TD
    subgraph Android_Phone [Android Smartphone / Edge-AI Perception]
        CAM[CameraX 640x480 Frame Capture] -->|STRATEGY_KEEP_ONLY_LATEST| PRE[Image Preprocessing 640x640]
        PRE --> YOLO[YOLOv8 Pose/Detection Engine]
        YOLO -->|Detections Class 0| NMS[NMS & Bounding Box Scaling]
        NMS --> TRK[PersonTracker IoU + Centroid]
        TRK --> VEL[PersonSpeedTracker dt-Normalized]
        VEL --> PRED[MotionPredictor 0.8s Constant Velocity]
        PRED --> ZON[NavigationZones Soft 5% Margin]
        ZON --> CROWD[CrowdAnalyzer EMPTY/LOW/MED/HIGH]
        CROWD --> NAV[NavigationEngine Executive]
        NAV -->|Raw Command| SAFE[SafetyController & Stale-Frame Check]
        SAFE -->|Validated Command| USB_TX[UsbSerialManager Async Write]
        SAFE --> UI[Compose Dashboard & Telemetry HUD]
    end

    subgraph Communication_Link [Physical USB Link]
        USB_TX -->|Type-C to Type-C OTG 115200 Baud / CDC| USB_RX[ESP32-S3 USB CDC/UART Receiver]
    end

    subgraph Microcontroller [ESP32-S3 Embedded Controller]
        USB_RX --> PARSER[Serial Command Parser]
        PARSER --> WD_RESET[Watchdog Timer Reset]
        PARSER --> ESTOP_CHK{Emergency Stop?}
        ESTOP_CHK -->|Yes| HALT[Immediate PWM 0 & Direction LOW]
        ESTOP_CHK -->|No| MIX[Differential Drive PWM Mixer]
        MIX --> MOTOR_OUT[GPIO Outputs ENA/ENB/IN1-IN4]
        
        WD[500ms Watchdog Timer] -->|Timeout >500ms| HALT
    end

    subgraph Actuation [AMR Chassis Hardware]
        MOTOR_OUT --> L298N[L298N Motor Driver]
        L298N --> MOTORS[Left & Right DC Motors]
    end
```

---

## 2. Architecture Conformance Verification

| Requirement | Design Specification | Actual Implementation | Conformance Status |
| :--- | :--- | :--- | :--- |
| **Perception Allocation** | On Android smartphone | Android CameraX + TFLite/ONNX in `AMRAndroid/` | **VERIFIED** |
| **Navigation Executive** | On Android smartphone | `NavigationEngine.kt` in Kotlin | **VERIFIED** |
| **Motor & Safety Failsafe** | On ESP32-S3 | `esp32_s3_motor_controller.ino` with 500ms Watchdog | **VERIFIED** |
| **Transport Layer** | USB-C OTG Serial | `UsbSerialManager.kt` via `usb-serial-for-android` | **VERIFIED** |
| **Offline Independence** | Operates without laptop | Phone directly drives ESP32-S3 via Type-C | **VERIFIED** |
