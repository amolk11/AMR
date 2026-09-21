# Android-to-ESP32 Communication & Protocol Audit

## 1. Protocol Compatibility Matrix

| Android Transmitter (`UsbSerialManager.kt`) | ESP32 Receiver (`esp32_s3_motor_controller.ino`) | Compatible? | Evidence |
| :--- | :--- | :--- | :--- |
| `"STOP\n"` | `if (cmd == "STOP") stopMotors();` | **YES** | Exact string match |
| `"STEER:15 SPEED:0.75\n"` | `cmd.startsWith("STEER:")` parses angle and speed scale | **YES** | Exact token match |
| `"CMD,FORWARD,180,15,0,102\n"` | `cmd.startsWith("CMD,")` parses CSV fields | **YES** | Exact CSV match |
| Framing & Delimiter | Newline `\n` | `Serial.readStringUntil('\n')` | **YES** | Exact delimiter match |
| Baud Rate | 115200 bps | `Serial.begin(115200)` | **YES** | Matched baud rate |

---

## 2. Transport & Error Recovery Analysis

* **USB Host Auto-Permission**: Handled in Android via `ACTION_USB_PERMISSION` broadcast receiver.
* **Write Mutex**: Android serial output uses `writeMutex.withLock` to prevent interleaved packets.
* **Timeout on Write**: Android enforces a 200ms write timeout.
* **Failsafe Stop**: ESP32-S3 halts within 500ms if packets stop arriving.
* **Overall Status**: **VERIFIED IN CODE**.
