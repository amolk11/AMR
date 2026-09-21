# ESP32-S3 Serial Communication & Safety Protocol Specification

## 1. Physical Layer
* **Connection**: USB Type-C to Type-C (Android Phone in USB Host OTG mode $\to$ ESP32-S3 Native USB-CDC or UART bridge).
* **Baud Rate**: 115200 bps (standard UART) or Native Full-Speed USB-CDC (12 Mbps).
* **Framing**: 8 Data bits, 1 Stop bit, No Parity, Newline delimiter (`\n` or `\r\n`).

---

## 2. Command Packet Format

### A. Compact ASCII Format (Backward Compatible)
```text
STEER:<angle> SPEED:<speed_scale>\n
STOP\n
```
* **Examples**:
  * `STEER:15 SPEED:0.75\n` $\rightarrow$ Steer left $15^\circ$ at 75% max PWM.
  * `STEER:-20 SPEED:0.50\n` $\rightarrow$ Steer right $20^\circ$ at 50% max PWM.
  * `STOP\n` $\rightarrow$ Emergency Stop (0% PWM, all H-Bridge outputs LOW).

### B. Robust Structured Telemetry CSV Packet
```text
CMD,<DIRECTION>,<PWM_SPEED>,<STEER_ANGLE>,<EMERGENCY_FLAG>,<SEQUENCE_NUM>\n
```
* **Fields**:
  1. `CMD`: Header identifier.
  2. `<DIRECTION>`: `FORWARD`, `LEFT`, `RIGHT`, `BACKWARD`, `STOP`.
  3. `<PWM_SPEED>`: Integer `0 .. 255`.
  4. `<STEER_ANGLE>`: Integer `-45 .. +45` degrees.
  5. `<EMERGENCY_FLAG>`: `0` (Normal) or `1` (Emergency Stop).
  6. `<SEQUENCE_NUM>`: Monotonically increasing sequence number for dropped packet detection.

---

## 3. ESP32-S3 Hardware Safety & Watchdog Contract

1. **Communication Watchdog**:
   * ESP32-S3 must run a hardware/software timer (e.g. `500ms` timeout).
   * If no valid command packet is received within `500ms`, ESP32-S3 must automatically set PWM to 0 and halt motors (`FAILSAFE_STOP`).
2. **Emergency Stop Pin & Priority**:
   * If `<EMERGENCY_FLAG> == 1` or packet is `"STOP"`, firmware immediately drops `ENA` and `ENB` to 0V before parsing any other telemetry.
3. **Differential Drive Mixing**:
   * For differential 2-wheel AMR:
     * $PWM_{Left} = BaseSpeed \times \left(1.0 + \frac{Steering}{45.0} \times Gain_{turn}\right)$
     * $PWM_{Right} = BaseSpeed \times \left(1.0 - \frac{Steering}{45.0} \times Gain_{turn}\right)$
     * Clamped to $[0, 255]$.
