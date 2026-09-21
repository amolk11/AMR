# ESP32-S3 Firmware Audit

**Audit Target**: [`firmware/esp32_s3_motor_controller/esp32_s3_motor_controller.ino`](file:///c:/Users/kadam/Desktop/customer-aware-amr-demo/firmware/esp32_s3_motor_controller/esp32_s3_motor_controller.ino)  

---

## 1. Pin Assignments & Hardware Interfaces

| Signal | GPIO Pin | Function | Electrical / Direction |
| :--- | :--- | :--- | :--- |
| `PIN_IN1` | GPIO 5 | Left Motor Direction A | Output, 3.3V Logic |
| `PIN_IN2` | GPIO 6 | Left Motor Direction B | Output, 3.3V Logic |
| `PIN_IN3` | GPIO 7 | Right Motor Direction A | Output, 3.3V Logic |
| `PIN_IN4` | GPIO 8 | Right Motor Direction B | Output, 3.3V Logic |
| `PIN_ENA` | GPIO 36 | Left Motor PWM Speed | Output, PWM (0..255) |
| `PIN_ENB` | GPIO 37 | Right Motor PWM Speed | Output, PWM (0..255) |
| `USB-CDC` | GPIO 19/20 | Native USB D- / D+ | USB OTG Full-Speed (12 Mbps) |

---

## 2. Firmware Subsystems & Logic Verification

### A. Watchdog Timer Failsafe
* **Threshold**: `WATCHDOG_TIMEOUT_MS = 500`.
* **Mechanism**: If `(millis() - lastCommandTime > 500)`, calls `stopMotors()` and asserts `isFailsafeActive = true`.
* **Status**: **VERIFIED IN CODE**. Guarantees that if the USB cable unplugs or the Android app freezes, motors halt within half a second.

### B. Command Parser
* **Dual Parsing**:
  1. Compact ASCII format: `STEER:<angle> SPEED:<scale>` and `STOP`.
  2. Telemetry CSV format: `CMD,<DIRECTION>,<PWM>,<STEER>,<STOP_FLAG>,<SEQ>`.
* **Status**: **VERIFIED IN CODE**.

### C. Differential Drive Mixing
* **Formula**:
  $$PWM_{Left} = BasePWM \times \left(1.0 - \frac{Steering}{45.0} \times 0.5\right)$$
  $$PWM_{Right} = BasePWM \times \left(1.0 + \frac{Steering}{45.0} \times 0.5\right)$$
  Clamped to $[0, 255]$.
* **Status**: **VERIFIED IN CODE**. Positive steering turns LEFT by reducing left wheel power and boosting right wheel power.
