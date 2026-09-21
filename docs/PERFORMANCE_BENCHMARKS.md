# Mobile Performance & Latency Benchmarks

## 1. Pipeline Component Latencies (Target Android Hardware)

| Pipeline Stage | Target Latency | Optimization Mechanism |
| :--- | :--- | :--- |
| **CameraX Frame Capture** | $33.3\text{ ms}$ ($30\text{ FPS}$) | `STRATEGY_KEEP_ONLY_LATEST`, zero-copy `ImageProxy` |
| **Preprocessing & Resizing** | $2.5\text{ ms}$ | Reusable `DirectByteBuffer`, scaled memory mapping |
| **YOLOv8n Mobile Inference** | $25.0 - 38.0\text{ ms}$ | GPU Delegate / NNAPI, INT8 or Float16 quantization |
| **NMS & Tracking** | $1.2\text{ ms}$ | IoU + Euclidean centroid association, $O(N)$ matching |
| **Navigation Engine** | $0.4\text{ ms}$ | Pure mathematical matrix & kinematic projections |
| **Safety Controller & Watchdog** | $0.1\text{ ms}$ | State validation & monotonic timer comparison |
| **USB-C OTG Transmission** | $0.8\text{ ms}$ | Async IO (`Dispatchers.IO`), non-blocking bulk transfer |
| **Total End-to-End Latency** | **$30 - 45\text{ ms}$** | Real-time response ($20 - 30\text{ Hz}$) |

---

## 2. Resource Consumption Estimates

* **RAM Footprint**: $\approx 110 - 145\text{ MB}$ (inclusive of CameraX surface buffers & TFLite tensors).
* **CPU Utilization**: $\approx 18 - 25\%$ on modern 8-core ARM SoC (with GPU delegate enabled).
* **Thermal Profile**: Steady-state operation at $\approx 36^\circ\text{C} - 41^\circ\text{C}$.
* **Battery Consumption**: $\approx 12 - 15\%\text{ per hour}$ active autonomous runtime.
