# Physical Hardware Validation Procedure

This document provides a strict, staged, safety-first commissioning procedure for bringing up the physical Customer-Aware AMR.

---

## Stage A — Sensors & Microcontroller (Motors Disconnected)
* **Goal**: Validate power, USB serial communications, and sensor telemetry without risk of uncommanded motion.
1. Disconnect motor battery power from the L298N driver.
2. Power the ESP32-S3 via USB-C.
3. Open Arduino Serial Monitor at 115200 baud.
4. Verify initial startup string `"STATUS:INITIALIZED"`.
5. Send `"STEER:0 SPEED:0.50\n"` manually $\rightarrow$ verify parse.
6. Stop sending commands $\rightarrow$ verify that after 500ms `"STATUS:FAILSAFE_WATCHDOG_TRIGGERED"` is reported.

---

## Stage B — Android USB OTG Communication (Motors Disconnected)
* **Goal**: Validate Android-to-ESP32-S3 serial command flow.
1. Connect Android phone to ESP32-S3 via Type-C OTG cable.
2. Launch `AMR Customer-Aware` app on Android.
3. Tap **Connect ESP32 USB** $\rightarrow$ accept OS permission popup.
4. Verify USB Status changes to `CONNECTED` (Green) on HUD.
5. Tap **START AUTONOMOUS** $\rightarrow$ verify live commands streaming on Serial Monitor.
6. Tap **🛑 EMERGENCY STOP** $\rightarrow$ verify immediate `"STOP"` packet received.
7. Unplug USB-C cable $\rightarrow$ verify ESP32 watchdog halts within 500ms.

---

## Stage C — Motors Elevated (Chassis on Stand)
* **Goal**: Validate differential wheel spinning, speed limits, and steering direction without ground contact.
1. Place AMR chassis securely on a stand so all drive wheels rotate freely in the air.
2. Connect motor power battery to L298N driver.
3. Keep an emergency battery disconnect switch within immediate reach.
4. Set speed limit in app to low value (e.g. 0.30).
5. Verify:
   * **FORWARD command**: Both left and right wheels spin forward in unison.
   * **STEER LEFT (+ angle)**: Right wheel spins faster than left wheel.
   * **STEER RIGHT (- angle)**: Left wheel spins faster than right wheel.
   * **STOP command / E-STOP**: Both wheels stop spinning immediately.
   * **Cable disconnect**: Both wheels stop within 500ms.

---

## Stage D — Controlled Floor Testing
* **Goal**: Validate autonomous customer-aware navigation in an open, obstacle-free test area.
1. Place AMR on a flat, level floor in a wide, clear corridor.
2. Stand 4 meters in front of the robot.
3. Start autonomous mode:
   * **Clear corridor**: Robot advances forward smoothly.
   * **Step toward left side of FOV**: Robot gently steers right.
   * **Step toward right side of FOV**: Robot gently steers left.
   * **Walk directly into close frontal path ($<1.0\text{ m}$)**: Robot executes immediate emergency stop.
   * **Step aside**: Robot waits 0.4s cooldown, then resumes smooth motion.
