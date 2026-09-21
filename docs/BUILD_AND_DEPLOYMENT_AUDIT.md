# Build and Deployment Audit

## 1. Build Environment & Toolchain Audit

| Component | Target Version | Status on Build Host | Build / Deployment Guidance |
| :--- | :--- | :--- | :--- |
| **Python** | 3.12.0 | **AVAILABLE** | Python tests run natively via `unittest` |
| **PyTorch / Ultralytics** | 2.10.0+cpu / 8.4.21 | **AVAILABLE** | Model inference and ONNX export verified |
| **Android SDK / JDK** | Android 34 / JDK 17 | **STANDALONE IN ANDROID STUDIO** | Not in host system PATH; open in Android Studio directly |
| **Gradle / AGP** | Gradle 8.9 / AGP 8.6.0 | **CONFIGURED** | `gradle-wrapper.properties` and version catalog configured |
| **Arduino / ESP-IDF** | ESP32 Board Core v2.0+ | **FIRMWARE READY** | Open `esp32_s3_motor_controller.ino` in Arduino IDE |

---

## 2. Step-by-Step Deployment Instructions

1. **Android Application**:
   * Open `AMRAndroid` directory in Android Studio.
   * Allow Gradle to sync dependencies from Google / MavenCentral.
   * Connect Android Smartphone via USB Debugging.
   * Click **Run 'app'** (`Shift + F10`) to compile and install APK.
2. **ESP32-S3 Firmware**:
   * Open `firmware/esp32_s3_motor_controller/esp32_s3_motor_controller.ino` in Arduino IDE.
   * Select Board: **ESP32S3 Dev Module** (USB CDC on Boot: Enabled).
   * Click **Upload**.
3. **Physical Interconnect**:
   * Connect Android phone to ESP32-S3 with a USB Type-C to Type-C OTG cable.
   * Accept USB permission prompt on Android screen.
