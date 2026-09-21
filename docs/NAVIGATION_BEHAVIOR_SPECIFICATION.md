# AMR Navigation Behavior Specification

## 1. Safety Priorities & Hierarchy

The navigation decision matrix follows a strict safety hierarchy:
1. **Critical Safety Halt (Level 0)**: Frontal close obstacle ($y_2 \ge 0.78$ or area $\ge 18\%$ in center corridor).
   * *Action*: Immediate Emergency Stop (`STOP\n`), target speed = 0.
2. **High Collision Risk Deceleration (Level 1)**: Lateral trajectory intersection ($p_y \ge 0.50$ in center) or multi-person convergence ($Risk \ge 2.0$).
   * *Action*: Cut speed by $50\%$ and steer away.
3. **Crowd Density Modulation (Level 2)**: Overall scene customer count.
   * *Action*: Scale max speed: `EMPTY` = 1.0, `LOW` = 0.85, `MEDIUM` = 0.65, `HIGH` = 0.35.
4. **Soft Obstacle Avoidance (Level 3)**: Zone occupancy imbalance between Left and Right sectors.
   * *Action*: Smooth steering deflection $(R - L) \times 120.0^\circ$ with $\pm 2.5^\circ$ deadband.
5. **Clear Path Progression (Level 4)**: Open space ahead.
   * *Action*: Steer straight ($0^\circ$), accelerate at $+1.0/s$ to base speed.

---

## 2. Dynamic Rate Limiting & Ramping

### A. Speed Ramping Formula
Given target speed $V_{target}$, current speed $V_{current}$, and elapsed time $\Delta t$:
$$\Delta V_{max\_up} = A_{max} \cdot \Delta t = 1.0 \cdot \Delta t$$
$$\Delta V_{max\_down} = D_{max} \cdot \Delta t = 2.5 \cdot \Delta t$$
* If $V_{target} > V_{current}$: $V_{new} = \min(V_{target}, V_{current} + \Delta V_{max\_up})$
* If $V_{target} < V_{current}$: $V_{new} = \max(V_{target}, V_{current} - \Delta V_{max\_down})$
* Exception: In Emergency Stop, $V_{new} = 0.0$ immediately.

### B. Steering Smoothing Formula
Given raw steering command $\theta_{raw}$ and previous steering $\theta_{prev}$:
$$\theta_{new} = 0.35 \cdot \theta_{raw} + 0.65 \cdot \theta_{prev}$$
If $|\theta_{new}| < 1.25^\circ \implies \theta_{new} = 0.0^\circ$.

---

## 3. Recovery Hysteresis State Machine

```
              ┌───────────────────────────┐
              │      NORMAL RUNNING       │
              └─────────────┬─────────────┘
                            │
               Close Obstacle / High Risk
                            │
                            ▼
              ┌───────────────────────────┐
              │      EMERGENCY STOP       │◄──────┐
              └─────────────┬─────────────┘       │
                            │                     │
                    Path Becomes Clear            │ Obstacle Reappears
                            │                     │ During Cooldown
                            ▼                     │
              ┌───────────────────────────┐       │
              │  RECOVERY COOLDOWN (0.4s) ├───────┘
              └─────────────┬─────────────┘
                            │
                   Timer >= 0.4s Clear
                            │
                            ▼
              ┌───────────────────────────┐
              │      RESUME MOVEMENT      │
              └───────────────────────────┘
```
