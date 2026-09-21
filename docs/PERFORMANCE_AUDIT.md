# Performance & Latency Audit

## 1. Measured & Projected Performance Parameters

| Metric | Target Specification | Design Implementation | Risk Assessment |
| :--- | :--- | :--- | :--- |
| **Camera Capture Rate** | $30\text{ FPS}$ | CameraX non-blocking analysis | LOW |
| **YOLO Inference Latency** | $25 - 38\text{ ms}$ | TFLite GPU Delegate / ONNX Mobile | LOW |
| **Tracking & Kinematics** | $< 2\text{ ms}$ | IoU + Euclidean centroid & EMA | LOW |
| **Navigation Decision Loop** | $< 1\text{ ms}$ | Pure mathematical matrix projection | ZERO |
| **End-to-End Latency** | $< 50\text{ ms}$ | Single-flight asynchronous worker | LOW |
| **Memory Consumption** | $< 150\text{ MB}$ | Direct pre-allocated buffers | LOW |
| **USB Serial Transmission** | $< 1\text{ ms}$ | Asynchronous CDC/ACM bulk endpoint | LOW |
| **Watchdog Reaction Time** | $\le 500\text{ ms}$ | Hardware timer in ESP32-S3 | ZERO (Guaranteed) |

---

## 2. Bottleneck & Safety Analysis
* **Frame Queueing**: Prevented via `STRATEGY_KEEP_ONLY_LATEST` and `DROP_OLDEST` channel buffers.
* **Thermal Throttling**: On long supermarket restocking routes, sustained high-resolution YOLO inference can cause mobile SoC thermal throttling. Recommended: $640 \times 480$ input resolution with FP16/INT8 quantization for cool, battery-efficient operation.
