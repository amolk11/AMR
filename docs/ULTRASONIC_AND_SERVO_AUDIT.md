# Ultrasonic Sensor & Servo Subsystem Audit

## 1. Audit of Existing Standalone Sketches

In `hardware/`, two standalone Arduino sketches contain ultrasonic and servo scanning implementations:
1. [`hardware/ultasonic_autonomous_navigation-L298n.cpp`](file:///c:/Users/kadam/Desktop/customer-aware-amr-demo/hardware/ultasonic_autonomous_navigation-L298n.cpp) (L298N driver)
2. [`hardware/ultrasonic-tfng-auto-test.cpp`](file:///c:/Users/kadam/Desktop/customer-aware-amr-demo/hardware/ultrasonic-tfng-auto-test.cpp) (TB6612FNG driver)

### Pinout Analysis
* **TRIG Pin**: GPIO 21 (Output, 3.3V compatible).
* **ECHO Pin**: GPIO 47 (Input).
  * ⚠️ *Voltage Warning*: HC-SR04 echoes at 5V. ESP32-S3 GPIO pins are 3.3V logic. A resistor voltage divider ($1\text{k}\Omega / 2\text{k}\Omega$) or level shifter is **mandatory** on the ECHO pin to prevent damaging the ESP32-S3.
* **SERVO Pin**: GPIO 13 (PWM / `ESP32Servo` library).
  * Servo range: $30^\circ$ (Right), $90^\circ$ (Center), $150^\circ$ (Left).

---

## 2. Integration Status in Production Firmware

* **Current Status**: Ultrasonic scanning is implemented in standalone bench test scripts (`hardware/`), but **not active in `firmware/esp32_s3_motor_controller.ino`**.
* **Reason**: In the current Architecture A, high-level perception and customer avoidance are handled by the Android phone's camera + YOLO vision pipeline.
* **Recommended Next Step**: Integrate HC-SR04 on GPIO 21/47 into the ESP32-S3 main loop as a **hardware-level close-range safety bumper** ($d < 15\text{ cm} \implies$ immediate hardware motor cutoff).
