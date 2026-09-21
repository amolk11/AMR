"""
Customer-Aware Autonomous Mobile Robot (AMR) Navigation Pipeline.
Integrates YOLO pose tracking with the reference NavigationEngine.
"""

import time
import cv2
from ultralytics import YOLO

from config import NavigationConfig
from navigation_engine import NavigationEngine, PersonDetection

# =========================================================
# INITIALIZATION
# =========================================================

# Load YOLO Model
model = YOLO("yolov8n-pose.pt")

# Initialize Reference Navigation Engine
config = NavigationConfig()
nav_engine = NavigationEngine(config)

# Initialize Camera
cap = cv2.VideoCapture(0, cv2.CAP_DSHOW)
cap.set(cv2.CAP_PROP_FRAME_WIDTH, 640)
cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 480)

prev_time = time.time()

print("======================================")
print(" CUSTOMER-AWARE AI NAVIGATION SYSTEM ")
print("======================================")

# =========================================================
# MAIN LOOP
# =========================================================

while True:
    ret, frame = cap.read()
    if not ret:
        break

    current_timestamp = time.time()
    frame_h, frame_w = frame.shape[:2]

    # =====================================================
    # YOLO TRACKING
    # =====================================================
    results = model.track(
        frame,
        persist=True,
        verbose=False
    )

    annotated_frame = results[0].plot()

    # =====================================================
    # EXTRACT STRUCTURED DETECTIONS
    # =====================================================
    detections = []

    if results[0].boxes is not None:
        for box in results[0].boxes:
            # Filter for human class (cls == 0)
            if int(box.cls) != 0:
                continue

            x1, y1, x2, y2 = map(float, box.xyxy[0])
            track_id = int(box.id) if box.id is not None else None
            conf = float(box.conf[0]) if box.conf is not None else 1.0

            detections.append(PersonDetection(
                x1=x1, y1=y1, x2=x2, y2=y2,
                track_id=track_id, conf=conf
            ))

    # =====================================================
    # RUN NAVIGATION ENGINE
    # =====================================================
    cmd = nav_engine.process(
        detections=detections,
        frame_width=frame_w,
        frame_height=frame_h,
        current_time=current_timestamp
    )

    diag = cmd.diagnostics
    person_states = diag.get("person_states", [])
    zones = diag.get("zones", {"left": 0.0, "center": 0.0, "right": 0.0})

    # =====================================================
    # VISUALIZATION: PERSON VELOCITIES & PREDICTIONS
    # =====================================================
    for p in person_states:
        cx, cy = int(p.cx), int(p.cy)
        pred_x, pred_y = p.pred_x, p.pred_y

        # Speed label
        cv2.putText(
            annotated_frame,
            f"V:{p.speed:.1f}px/s",
            (cx - 40, int(cy - 20)),
            cv2.FONT_HERSHEY_SIMPLEX,
            0.55,
            (0, 255, 0),
            2
        )

        # Movement vector
        cv2.line(
            annotated_frame,
            (cx, cy),
            (pred_x, pred_y),
            (0, 0, 255) if p.is_predicted_in_path else (255, 200, 0),
            2
        )

        # Predicted point
        cv2.circle(
            annotated_frame,
            (pred_x, pred_y),
            5,
            (0, 0, 255) if p.is_predicted_in_path else (255, 200, 0),
            -1
        )

        # Center point
        cv2.circle(
            annotated_frame,
            (cx, cy),
            4,
            (255, 255, 0),
            -1
        )

    # =====================================================
    # VISUALIZATION: ZONE BOUNDARIES
    # =====================================================
    left_x = int(frame_w * config.left_boundary_ratio)
    right_x = int(frame_w * config.right_boundary_ratio)

    cv2.line(annotated_frame, (left_x, 0), (left_x, frame_h), (255, 255, 0), 2)
    cv2.line(annotated_frame, (right_x, 0), (right_x, frame_h), (255, 255, 0), 2)

    # =====================================================
    # HUD TELEMETRY PANEL
    # =====================================================
    serial_cmd = cmd.to_serial_string().strip()
    status_color = (0, 0, 255) if cmd.emergency_stop else (0, 255, 0)

    cv2.putText(annotated_frame, f"Customers: {len(detections)}", (20, 35),
                cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 255, 0), 2)
    cv2.putText(annotated_frame, f"Density: {diag.get('crowd_density', 'EMPTY')}", (20, 70),
                cv2.FONT_HERSHEY_SIMPLEX, 0.7, (255, 255, 0), 2)
    cv2.putText(annotated_frame, f"Risk Count: {int(diag.get('collision_risk_count', 0))}", (20, 105),
                cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 140, 255), 2)
    cv2.putText(annotated_frame, f"Plan: {diag.get('direction_choice', 'FORWARD')}", (20, 140),
                cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 255, 255), 2)
    cv2.putText(annotated_frame, f"Steer: {int(round(cmd.steering))} deg", (20, 175),
                cv2.FONT_HERSHEY_SIMPLEX, 0.7, (255, 0, 255), 2)
    cv2.putText(annotated_frame, f"Speed Scale: {cmd.speed:.2f}", (20, 210),
                cv2.FONT_HERSHEY_SIMPLEX, 0.7, (255, 255, 255), 2)
    cv2.putText(annotated_frame, f"Command: {serial_cmd}", (20, 245),
                cv2.FONT_HERSHEY_SIMPLEX, 0.75, status_color, 2)
    cv2.putText(annotated_frame, f"Reason: {cmd.reason}", (20, 280),
                cv2.FONT_HERSHEY_SIMPLEX, 0.55, (200, 200, 200), 1)

    # Zone Occupancies
    cv2.putText(annotated_frame, f"L:{zones['left']:.2f} C:{zones['center']:.2f} R:{zones['right']:.2f}",
                (20, 315), cv2.FONT_HERSHEY_SIMPLEX, 0.65, (220, 220, 220), 2)

    # =====================================================
    # FPS
    # =====================================================
    fps = 1.0 / max(1e-4, (current_timestamp - prev_time))
    prev_time = current_timestamp

    cv2.putText(annotated_frame, f"FPS: {int(round(fps))}", (20, 450),
                cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 255, 200), 2)

    # =====================================================
    # DISPLAY
    # =====================================================
    cv2.imshow("Customer-Aware AI Navigation System", annotated_frame)

    if cv2.waitKey(1) == 27:
        break

# Cleanup
cap.release()
cv2.destroyAllWindows()