# Technical Open Questions & Android Integration Considerations

## 1. Mobile Inference Runtime Selection
* **Candidate Runtimes**:
  1. **TensorFlow Lite (TFLite / LiteRT)** with NNAPI / GPU Delegate: Most mature on Android, standard YOLOv8n TFLite float16/int8 exports available.
  2. **ONNX Runtime Mobile (ORT)**: High performance, direct export from PyTorch via `model.export(format="onnx")`.
* **Resolution**: Provide a modular `YoloDetector` interface and concrete `TFLitePersonDetector` and `OnnxPersonDetector` options, supporting model assets in `app/src/main/assets/models/`.

## 2. USB Serial Library & ESP32-S3 USB Mode
* **ESP32-S3 Capabilities**:
  * Native USB-CDC (direct Type-C connection via GPIO19/GPIO20 D+/D- pins) or external CP2102/CH340 USB-to-UART chip.
* **Android USB Serial Driver**:
  * `com.github.mik3y:usb-serial-for-android`: Standard production library supporting CDC/ACM, FTDI, CP210x, CH34x with USB Host OTG permission management.

## 3. Camera Orientation & Frame Transformations
* Mobile devices can be mounted in portrait, landscape, or reverse landscape.
* CameraX provides `ImageInfo.rotationDegrees` (0, 90, 180, 270). The detector pipeline must rotate/scale bounding boxes back to the standard frame coordinate space ($640 \times 480$) before feeding to `NavigationEngine`.
