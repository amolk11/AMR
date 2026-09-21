"""
Script to export YOLOv8 pose or detection model to TensorFlow Lite (.tflite) for Android.

Usage:
    python scripts/export_yolo_tflite.py --model yolov8n.pt --imgsz 640 --int8
"""

import argparse
import os
import shutil
from ultralytics import YOLO

def export_model(model_path: str, imgsz: int = 640, int8: bool = False):
    print(f"Loading YOLO model: {model_path}...")
    model = YOLO(model_path)

    print(f"Exporting to TFLite (imgsz={imgsz}, int8={int8})...")
    export_path = model.export(
        format="tflite",
        imgsz=imgsz,
        int8=int8
    )

    print(f"Model successfully exported to: {export_path}")

    # Destination in Android assets
    dest_dir = os.path.join("AMRAndroid", "app", "src", "main", "assets", "models")
    os.makedirs(dest_dir, exist_ok=True)
    dest_path = os.path.join(dest_dir, "yolov8n_float32.tflite")

    if os.path.exists(export_path):
        shutil.copyfile(export_path, dest_path)
        print(f"Copied exported model to Android assets: {dest_path}")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Export YOLO model to TFLite for Android AMR")
    parser.add_argument("--model", type=str, default="yolov8n-pose.pt", help="Path to .pt model")
    parser.add_argument("--imgsz", type=int, default=640, help="Model input size (default 640)")
    parser.add_argument("--int8", action="store_true", help="Enable INT8 quantization")
    args = parser.parse_args()

    export_model(args.model, args.imgsz, args.int8)
