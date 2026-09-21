import cv2
import time

from ultralytics import YOLO

from person_speed import compute_speed
from motion_prediction import predict_next_position
from crowd_analysis import analyze_crowd_density

from navigation_zones import analyze_navigation_zones
from navigation_planner import (
    navigation_planner,
    steering_from_zones
)

# =========================================================
# LOAD MODEL
# =========================================================

model = YOLO("yolov8n-pose.pt")

# =========================================================
# CAMERA
# =========================================================

cap = cv2.VideoCapture(0, cv2.CAP_DSHOW)

# lower resolution for better FPS

cap.set(cv2.CAP_PROP_FRAME_WIDTH, 640)
cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 480)

# =========================================================
# FPS
# =========================================================

prev_time = time.time()

# =========================================================
# START
# =========================================================

print("======================================")
print(" CUSTOMER-AWARE AI NAVIGATION SYSTEM ")
print("======================================")

# =========================================================
# MAIN LOOP
# =========================================================

while True:

    # =====================================================
    # READ CAMERA
    # =====================================================

    ret, frame = cap.read()

    if not ret:
        break

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
    # VARIABLES
    # =====================================================

    people_count = 0

    collision_risk = 0

    # =====================================================
    # PROCESS DETECTIONS
    # =====================================================

    if results[0].boxes is not None:

        for box in results[0].boxes:

            # only humans
            if int(box.cls) != 0:
                continue

            people_count += 1

            # skip if no tracking id
            if box.id is None:
                continue

            track_id = int(box.id)

            # =================================================
            # BOUNDING BOX
            # =================================================

            x1, y1, x2, y2 = map(int, box.xyxy[0])

            cx = int((x1 + x2) / 2)
            cy = int((y1 + y2) / 2)

            # =================================================
            # SPEED ESTIMATION
            # =================================================

            speed = compute_speed(track_id, (cx, cy))

            # =================================================
            # MOTION PREDICTION
            # =================================================

            predicted = predict_next_position(
                track_id,
                (cx, cy)
            )

            px, py = predicted

            # =================================================
            # COLLISION RISK
            # =================================================

            # robot forward zone

            center_x1 = frame_w // 3
            center_x2 = 2 * frame_w // 3

            # if predicted motion enters robot path

            if center_x1 < px < center_x2 and py > frame_h // 2:

                collision_risk += 1

            # =================================================
            # VISUALIZATION
            # =================================================

            # speed label

            cv2.putText(
                annotated_frame,
                f"Speed:{speed:.1f}",
                (cx - 40, y1 - 15),
                cv2.FONT_HERSHEY_SIMPLEX,
                0.6,
                (0, 255, 0),
                2
            )

            # movement vector

            cv2.line(
                annotated_frame,
                (cx, cy),
                predicted,
                (0, 0, 255),
                2
            )

            # predicted point

            cv2.circle(
                annotated_frame,
                predicted,
                6,
                (0, 0, 255),
                -1
            )

            # center point

            cv2.circle(
                annotated_frame,
                (cx, cy),
                4,
                (255, 255, 0),
                -1
            )

    # =====================================================
    # CROWD ANALYSIS
    # =====================================================

    density = analyze_crowd_density(people_count)

    # =====================================================
    # NAVIGATION ZONES
    # =====================================================

    zones = analyze_navigation_zones(
        frame,
        results
    )

    # =====================================================
    # NAVIGATION DECISION
    # =====================================================

    decision, costs = navigation_planner(zones)

    # =====================================================
    # STEERING COMPUTATION
    # =====================================================

    steering = steering_from_zones(zones)

    # =====================================================
    # SPEED CONTROL
    # =====================================================

    robot_speed = 1.0

    # reduce speed in crowd

    if density == "HIGH":
        robot_speed = 0.4

    elif density == "MEDIUM":
        robot_speed = 0.7

    # reduce speed for collision risk

    if collision_risk >= 2:
        robot_speed *= 0.5

    # =====================================================
    # EMERGENCY STOP
    # =====================================================

    emergency_stop = False

    if collision_risk >= 4:
        emergency_stop = True
        robot_speed = 0

    # =====================================================
    # NAVIGATION COMMAND
    # =====================================================

    if emergency_stop:

        nav_command = "STOP"

    else:

        nav_command = f"STEER:{int(steering)} SPEED:{robot_speed:.2f}"

    # =====================================================
    # ZONE VISUALIZATION
    # =====================================================

    left_x = frame_w // 3
    right_x = 2 * frame_w // 3

    cv2.line(
        annotated_frame,
        (left_x, 0),
        (left_x, frame_h),
        (255, 255, 0),
        2
    )

    cv2.line(
        annotated_frame,
        (right_x, 0),
        (right_x, frame_h),
        (255, 255, 0),
        2
    )

    # =====================================================
    # HUD PANEL
    # =====================================================

    cv2.putText(
        annotated_frame,
        f"Customers: {people_count}",
        (20, 40),
        cv2.FONT_HERSHEY_SIMPLEX,
        0.8,
        (0, 255, 0),
        2
    )

    cv2.putText(
        annotated_frame,
        f"Density: {density}",
        (20, 80),
        cv2.FONT_HERSHEY_SIMPLEX,
        0.8,
        (255, 255, 0),
        2
    )

    cv2.putText(
        annotated_frame,
        f"Collision Risk: {collision_risk}",
        (20, 120),
        cv2.FONT_HERSHEY_SIMPLEX,
        0.8,
        (0, 0, 255),
        2
    )

    cv2.putText(
        annotated_frame,
        f"Decision: {decision}",
        (20, 160),
        cv2.FONT_HERSHEY_SIMPLEX,
        0.8,
        (0, 255, 255),
        2
    )

    cv2.putText(
        annotated_frame,
        f"Steering: {int(steering)} deg",
        (20, 200),
        cv2.FONT_HERSHEY_SIMPLEX,
        0.8,
        (255, 0, 255),
        2
    )

    cv2.putText(
        annotated_frame,
        f"Speed Scale: {robot_speed:.2f}",
        (20, 240),
        cv2.FONT_HERSHEY_SIMPLEX,
        0.8,
        (255, 255, 255),
        2
    )

    cv2.putText(
        annotated_frame,
        f"Command: {nav_command}",
        (20, 280),
        cv2.FONT_HERSHEY_SIMPLEX,
        0.8,
        (0, 200, 255),
        2
    )

    # =====================================================
    # ZONE OCCUPANCY
    # =====================================================

    cv2.putText(
        annotated_frame,
        f"L:{zones['left']:.2f}",
        (20, 330),
        cv2.FONT_HERSHEY_SIMPLEX,
        0.7,
        (255,255,255),
        2
    )

    cv2.putText(
        annotated_frame,
        f"C:{zones['center']:.2f}",
        (20, 360),
        cv2.FONT_HERSHEY_SIMPLEX,
        0.7,
        (255,255,255),
        2
    )

    cv2.putText(
        annotated_frame,
        f"R:{zones['right']:.2f}",
        (20, 390),
        cv2.FONT_HERSHEY_SIMPLEX,
        0.7,
        (255,255,255),
        2
    )

    # =====================================================
    # FPS
    # =====================================================

    current_time = time.time()

    fps = 1 / (current_time - prev_time)

    prev_time = current_time

    cv2.putText(
        annotated_frame,
        f"FPS:{int(fps)}",
        (20, 430),
        cv2.FONT_HERSHEY_SIMPLEX,
        0.8,
        (255,0,0),
        2
    )

    # =====================================================
    # DISPLAY
    # =====================================================

    cv2.imshow(
        "Customer-Aware AI Navigation System",
        annotated_frame
    )

    # =====================================================
    # EXIT
    # =====================================================

    if cv2.waitKey(1) == 27:
        break

# =========================================================
# CLEANUP
# =========================================================

cap.release()

cv2.destroyAllWindows()