# Audit of Existing Customer-Aware AMR Navigation System

## 1. System Overview & Core Components
The navigation system is a deterministic, perception-driven mobile robotics controller designed for supermarket restocking. It processes real-time detections of humans (customers) and yields smooth steering, rate-limited speeds, and proactive emergency stops.

| Module | Purpose | Key Responsibilities |
| :--- | :--- | :--- |
| [`src/config.py`](file:///c:/Users/kadam/Desktop/customer-aware-amr-demo/src/config.py) | Parameter Centralization | Stores geometric ratios, gains, deadbands, speed caps, risk thresholds, and EMA smoothing alphas. |
| [`src/navigation_engine.py`](file:///c:/Users/kadam/Desktop/customer-aware-amr-demo/src/navigation_engine.py) | Central Executive | Coordinates perception pipeline, evaluates multi-person risks, manages acceleration limits, and enforces recovery hysteresis. |
| [`src/person_speed.py`](file:///c:/Users/kadam/Desktop/customer-aware-amr-demo/src/person_speed.py) | Velocity Tracking | Calculates time-normalized velocities $(v_x, v_y)$ in $px/s$ using $\Delta t$, applies EMA filter ($\alpha=0.4$), and purges stale IDs ($TTL=2.0s$). |
| [`src/motion_prediction.py`](file:///c:/Users/kadam/Desktop/customer-aware-amr-demo/src/motion_prediction.py) | Trajectory Prediction | Projects future coordinates $(p_x, p_y)$ over horizon $T_{pred}=0.8s$ using constant-velocity kinematics. |
| [`src/crowd_analysis.py`](file:///c:/Users/kadam/Desktop/customer-aware-amr-demo/src/crowd_analysis.py) | Density Estimation | Classifies customer count into `EMPTY` (0), `LOW` (1–2), `MEDIUM` (3–4), `HIGH` (5+). |
| [`src/navigation_zones.py`](file:///c:/Users/kadam/Desktop/customer-aware-amr-demo/src/navigation_zones.py) | Spatial Partitioning | Computes continuous soft-occupancy costs for `LEFT` ($0..1/3$), `CENTER` ($1/3..2/3$), and `RIGHT` ($2/3..1.0$) with 5% soft boundary blending. |
| [`src/navigation_planner.py`](file:///c:/Users/kadam/Desktop/customer-aware-amr-demo/src/navigation_planner.py) | Steering & Direction | Computes obstacle avoidance steering angle $(R - L) \times 120^\circ$, enforces $\pm 2.5^\circ$ deadband, clamps to $\pm 45^\circ$, and favors forward motion via $-1\mu$ tie-breaker. |

---

## 2. Mathematical Formulations & Thresholds

### A. Coordinate System & Geometry
* **Frame Coordinates**: Normalized $[0.0, 1.0]$ or pixel $[0..W, 0..H]$ where $(0,0)$ is top-left, $+X$ is right, $+Y$ is downwards.
* **Corridor Partitions**:
  * Left Boundary: $X_{left} = W \times \frac{1}{3} \approx 0.333 \cdot W$
  * Right Boundary: $X_{right} = W \times \frac{2}{3} \approx 0.667 \cdot W$
  * Soft Margin: $M = W \times 0.05$ (5% blend on boundaries)
* **Steering Sign Convention**:
  * Positive ($+$) Steering $\rightarrow$ Turn **LEFT** (counter-clockwise) away from right-side crowd.
  * Negative ($-$) Steering $\rightarrow$ Turn **RIGHT** (clockwise) away from left-side crowd.
  * Formula: $\theta_{raw} = (Cost_{right} - Cost_{left}) \times 120.0^\circ$
  * Deadband: If $|\theta_{raw}| < 2.5^\circ \implies \theta_{raw} = 0.0^\circ$
  * Limits: $\theta_{clamped} \in [-45.0^\circ, +45.0^\circ]$
  * Temporal EMA: $\theta_t = 0.35 \cdot \theta_{raw} + 0.65 \cdot \theta_{t-1}$

### B. Velocity & Trajectory Prediction
* **Time Delta**: $\Delta t = t_{curr} - t_{prev}$ (clamped $10^{-4} \le \Delta t \le 1.0s$).
* **Velocity Filtering**:
  $$v_{x, raw} = \frac{x_t - x_{t-1}}{\Delta t}, \quad v_{y, raw} = \frac{y_t - y_{t-1}}{\Delta t}$$
  $$v_x = 0.4 \cdot v_{x, raw} + 0.6 \cdot v_{x, prev}, \quad v_y = 0.4 \cdot v_{y, raw} + 0.6 \cdot v_{y, prev}$$
  $$Speed = \sqrt{v_x^2 + v_y^2}$$
* **Predicted Position**:
  $$p_x = x + v_x \times 0.8s, \quad p_y = y + v_y \times 0.8s$$

### C. Proximity, Risk & Emergency Stop Logic
* **Direct Frontal Proximity Stop**:
  * Condition 1: Person in Center Corridor ($X_{left} \le c_x \le X_{right}$) AND normalized bottom $y_2 / H \ge 0.78$ (critical close range).
  * Condition 2: Person in Center Corridor AND bounding box area ratio $Area / (W \times H) \ge 0.18$ (covers $>18\%$ of FOV).
  * Action: Immediate `emergency_stop = True`, target speed = $0.0$, command = `"STOP\n"`.
* **Path-Crossing Trajectory Collision**:
  * Condition: Predicted point $(p_x, p_y)$ falls within center corridor ($X_{left} \le p_x \le X_{right}$) and $p_y > H \times 0.50$.
  * Action: Adds $2.0$ to collision risk score $\implies$ triggers anticipatory deceleration.
* **Emergency Stop Recovery Hysteresis**:
  * Requires $0.4s$ continuous clear path before clearing emergency stop state to prevent stop-start stuttering.

### D. Speed Scaling & Acceleration Limits
* **Base & Crowd Scaling**:
  * `EMPTY`: $1.00$
  * `LOW`: $0.85$
  * `MEDIUM`: $0.65$
  * `HIGH`: $0.35$
* **Collision Risk Decay**:
  * Risk $\ge 2.0 \implies \times 0.50$
  * Risk $\ge 1.0 \implies \times 0.75$
  * Frontal Proximity Decay: $V_{target} = V_{target} \times \max\left(0.2, 1.0 - \frac{Risk_{center}}{4.5}\right)$
* **Rate Limits**:
  * Max Acceleration: $+1.0 / s$ ($0 \to 1.0$ speed in $1.0s$).
  * Max Deceleration: $-2.5 / s$ (rapid braking in $0.4s$).
  * Minimum Moving Speed: $0.20$ (prevents motor stall when moving).

---

## 3. Findings & Porting Readiness
1. The Python implementation is completely modular and pure math/data logic, making it 100% portable to Kotlin without external Python dependencies.
2. All inputs to `NavigationEngine.process()` are structured data (`PersonDetection(x1, y1, x2, y2, track_id, conf)`), making it straightforward to bridge from Android CameraX/YOLO inference results.
3. The output `NavigationCommand` produces standard strings (`"STEER:15 SPEED:0.65\n"` or `"STOP\n"`), which map directly to our USB serial protocol.
