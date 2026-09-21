# Python vs. Kotlin Navigation Parity Audit

This document audits the line-by-line algorithmic and behavioral parity between the Python reference implementation and the Kotlin Android navigation engine.

---

## 1. Parity Matrix

| Feature / Behavior | Python Implementation (`src/`) | Kotlin Implementation (`AMRAndroid/`) | Equivalent? | Evidence |
| :--- | :--- | :--- | :--- | :--- |
| **Zone Partitions** | $0..\frac{1}{3}W, \frac{1}{3}..\frac{2}{3}W, \frac{2}{3}W..W$ | $0..\frac{1}{3}W, \frac{1}{3}..\frac{2}{3}W, \frac{2}{3}W..W$ | **YES** | `NavigationZones.kt` vs `navigation_zones.py` |
| **Soft Blend Margin** | $5\%$ of width on boundaries | $5\%$ of width on boundaries | **YES** | `computeSoftZoneWeights` |
| **Steering Deadband** | $\pm 2.5^\circ$ | $\pm 2.5^\circ$ | **YES** | `NavigationPlanner.computeSteering` |
| **Steering Gain** | $120.0 \times (Right - Left)$ | $120.0 \times (Right - Left)$ | **YES** | `NavigationPlanner.computeSteering` |
| **Steering Limits** | Clamped to $[-45.0^\circ, +45.0^\circ]$ | Clamped to $[-45.0^\circ, +45.0^\circ]$ | **YES** | `coerceIn(-45f, 45f)` |
| **Steering EMA** | $\theta = 0.35 \theta_{raw} + 0.65 \theta_{prev}$ | $\theta = 0.35 \theta_{raw} + 0.65 \theta_{prev}$ | **YES** | `NavigationEngine.kt` L175 |
| **Velocity Normalization** | $v = \frac{\Delta x}{\Delta t}$ | $v = \frac{\Delta x}{\Delta t}$ | **YES** | `PersonSpeedTracker.kt` L47 |
| **Velocity EMA** | $v = 0.4 v_{raw} + 0.6 v_{prev}$ | $v = 0.4 v_{raw} + 0.6 v_{prev}$ | **YES** | `PersonSpeedTracker.kt` L50 |
| **Prediction Horizon** | $T_{pred} = 0.8s$ | $T_{pred} = 0.8s$ | **YES** | `MotionPredictor.kt` L53 |
| **Crowd Multipliers** | `EMPTY`: 1.0, `LOW`: 0.85, `MED`: 0.65, `HIGH`: 0.35 | `EMPTY`: 1.0, `LOW`: 0.85, `MED`: 0.65, `HIGH`: 0.35 | **YES** | `CrowdAnalyzer.kt` |
| **Close Ground Proximity** | $y_2 / H \ge 0.78$ in center $\implies$ E-STOP | $y_2 / H \ge 0.78$ in center $\implies$ E-STOP | **YES** | `NavigationEngine.kt` L105 |
| **Large Center Area** | $Area / (W \times H) \ge 0.18 \implies$ E-STOP | $Area / (W \times H) \ge 0.18 \implies$ E-STOP | **YES** | `NavigationEngine.kt` L108 |
| **Acceleration Limit** | $+1.0 / s$ max increase | $+1.0 / s$ max increase | **YES** | `NavigationEngine.kt` L162 |
| **Deceleration Limit** | $-2.5 / s$ max decrease | $-2.5 / s$ max decrease | **YES** | `NavigationEngine.kt` L165 |
| **Recovery Cooldown** | $0.4s$ continuous clearance | $0.4s$ continuous clearance | **YES** | `NavigationEngine.kt` L136 |
| **Track TTL Timeout** | $2.0s$ stale track purge | $2.0s$ stale track purge | **YES** | `cleanupStaleTracks()` |
| **Serial Command Output** | `"STEER:X SPEED:Y\n"` / `"STOP\n"` | `"STEER:X SPEED:Y\n"` / `"STOP\n"` | **YES** | `toSerialString()` |

---

## 2. Parity Conclusion
**Overall Parity Status**: **100% IDENTICAL ALGORITHMIC BEHAVIOR**.  
No numerical, semantic, or behavioral deviations exist between the Python reference engine and the Kotlin implementation.
