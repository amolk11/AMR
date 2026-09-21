"""
Navigation Zone Analysis for Customer-Aware AMR.
Computes soft occupancy costs for LEFT, CENTER, and RIGHT FOV sectors.
"""

from typing import Dict, List, Tuple, Any, Optional

def compute_soft_zone_weights(
    cx: float,
    width: float,
    left_ratio: float = 1.0 / 3.0,
    right_ratio: float = 2.0 / 3.0,
    soft_margin_ratio: float = 0.05
) -> Tuple[float, float, float]:
    """
    Computes soft partition weights (w_left, w_center, w_right) that sum to 1.0.
    Prevents step-discontinuities when a person crosses zone boundaries.
    """
    left_x = width * left_ratio
    right_x = width * right_ratio
    margin = width * soft_margin_ratio

    # Distance to left boundary
    if cx < (left_x - margin):
        return 1.0, 0.0, 0.0
    elif cx < (left_x + margin):
        # Blend left and center
        t = (cx - (left_x - margin)) / (2.0 * margin)
        return (1.0 - t), t, 0.0
    elif cx < (right_x - margin):
        return 0.0, 1.0, 0.0
    elif cx < (right_x + margin):
        # Blend center and right
        t = (cx - (right_x - margin)) / (2.0 * margin)
        return 0.0, (1.0 - t), t
    else:
        return 0.0, 0.0, 1.0


def compute_zone_occupancies_from_boxes(
    boxes: List[Tuple[float, float, float, float]],
    frame_width: float,
    frame_height: float,
    left_ratio: float = 1.0 / 3.0,
    right_ratio: float = 2.0 / 3.0,
    soft_margin_ratio: float = 0.05
) -> Dict[str, float]:
    """
    Computes continuous zone occupancies from a list of bounding boxes (x1, y1, x2, y2).
    Pure numerical calculation independent of OpenCV / YOLO GUI.
    """
    left_zone = 0.0
    center_zone = 0.0
    right_zone = 0.0

    total_frame_area = frame_width * frame_height
    if total_frame_area <= 0:
        return {"left": 0.0, "center": 0.0, "right": 0.0}

    for (x1, y1, x2, y2) in boxes:
        cx = (x1 + x2) / 2.0
        area = max(0.0, (x2 - x1)) * max(0.0, (y2 - y1))
        occupancy = area / total_frame_area

        w_l, w_c, w_r = compute_soft_zone_weights(
            cx, frame_width, left_ratio, right_ratio, soft_margin_ratio
        )

        left_zone += occupancy * w_l
        center_zone += occupancy * w_c
        right_zone += occupancy * w_r

    return {
        "left": left_zone,
        "center": center_zone,
        "right": right_zone
    }


def analyze_navigation_zones(frame: Any, results: Any) -> Dict[str, float]:
    """
    Backward-compatible wrapper for YOLO results and cv2 frame.
    """
    h, w = frame.shape[:2]
    boxes = []

    if results is not None and len(results) > 0 and results[0].boxes is not None:
        for box in results[0].boxes:
            if int(box.cls) != 0:
                continue
            x1, y1, x2, y2 = map(float, box.xyxy[0])
            boxes.append((x1, y1, x2, y2))

    return compute_zone_occupancies_from_boxes(boxes, float(w), float(h))