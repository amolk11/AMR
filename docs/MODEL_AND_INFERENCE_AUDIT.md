# YOLO Model and Inference Audit

## 1. Model Inventory & Properties

| Model Asset | Format | Size | Parameters | Input Dimensions | Output Shape | Location |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `yolov8n-pose.pt` | PyTorch | 6.5 MB | 3,289,964 | `(1, 3, 640, 640)` | `(1, 56, 8400)` | Repository root |
| `yolov8n-pose.onnx` | ONNX (opset 20) | 12.9 MB | 3,289,964 | `(1, 3, 640, 640)` | `(1, 56, 8400)` | Root & `AMRAndroid/app/src/main/assets/models/` |

---

## 2. Export Pipeline & Verification

### A. ONNX Export Verification
* **Execution Command**:
  ```bash
  python -c "from ultralytics import YOLO; model = YOLO('yolov8n-pose.pt'); model.export(format='onnx')"
  ```
* **Status**: **VERIFIED (SUCCESS)**
* **Output**: Successfully generated and slimmed `yolov8n-pose.onnx` (12.9 MB).

### B. TFLite Export & Compatibility Note
* **Desktop Python Dependency Note**: Desktop TensorFlow 2.17.0 on Python 3.12 requires specific `numpy` and `ml-dtypes` combinations for TFLite SavedModel translation.
* **Android Deployment Strategy**:
  1. The Android app includes `TFLitePersonDetector.kt` configured for standard TFLite float32 models (`yolov8n_float32.tflite`).
  2. The packaged `yolov8n_pose.onnx` can also be run natively on Android via ONNX Runtime Mobile (`onnxruntime-android`), providing multi-backend flexibility.
