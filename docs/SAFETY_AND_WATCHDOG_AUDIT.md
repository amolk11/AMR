# Safety Systems & Watchdog Audit

## 1. Safety Failure Modes & Failsafe Table

| Failure Scenario | Detection Mechanism | System Response | Max Delay | Safe? | Evidence |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **USB Cable Unplugged** | Hardware USB D+/D- disconnect / Serial timeout | ESP32-S3 Watchdog triggers `stopMotors()` | $\le 500\text{ ms}$ | **YES** | `esp32_s3_motor_controller.ino` L49 |
| **Android App Crash / Freeze** | Silence on USB Serial stream | ESP32-S3 Watchdog triggers `stopMotors()` | $\le 500\text{ ms}$ | **YES** | `WATCHDOG_TIMEOUT_MS = 500` |
| **Camera Frame Drop / Freeze** | `SafetyController.kt` timestamp monitor | Overrides command to `EMERGENCY_STOP` | $\le 500\text{ ms}$ | **YES** | `maxFrameStalenessSeconds = 0.50` |
| **Repeated Inference Failures** | `SafetyController.kt` consecutive failure counter | Overrides command to `EMERGENCY_STOP` | $3\text{ frames}$ ($\approx 100\text{ ms}$) | **YES** | `maxInferenceFailureCount = 3` |
| **Close Customer Proximity** | `NavigationEngine.kt` ($y_2 \ge 0.78$ in center) | Immediate `STOP\n` packet sent | Instant ($1\text{ frame}$) | **YES** | `emergency_stop_bottom_y = 0.78` |
| **Operator Touch E-Stop** | UI Emergency Stop button pressed | Immediate `STOP\n` packet & software latch | Instant | **YES** | `DashboardScreen.kt` |
| **Microcontroller Boot / Reset** | Firmware `setup()` routine | Initial `stopMotors()` called before loop | $0\text{ ms}$ | **YES** | `esp32_s3_motor_controller.ino` L37 |

---

## 2. Safety Audit Conclusion

**Overall Safety Status**: **VERIFIED IN SOFTWARE & FIRMWARE ARCHITECTURE**.  
The dual-watchdog architecture guarantees that failure in either Android software, USB transport, or microcontroller loop results in an automatic, deterministic motor halt.
