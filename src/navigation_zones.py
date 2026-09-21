import cv2

def analyze_navigation_zones(frame, results):

    h, w = frame.shape[:2]

    left_zone = 0
    center_zone = 0
    right_zone = 0

    # zone boundaries
    left_x = w // 3
    right_x = 2 * w // 3

    overlay = frame.copy()

    # draw zones
    cv2.line(overlay, (left_x, 0), (left_x, h), (255,255,0), 2)
    cv2.line(overlay, (right_x, 0), (right_x, h), (255,255,0), 2)

    if results[0].boxes is not None:

        for box in results[0].boxes:

            if int(box.cls) != 0:
                continue

            x1, y1, x2, y2 = map(int, box.xyxy[0])

            cx = int((x1+x2)/2)

            area = (x2-x1)*(y2-y1)

            occupancy = area / (w*h)

            if cx < left_x:
                left_zone += occupancy

            elif cx < right_x:
                center_zone += occupancy

            else:
                right_zone += occupancy

    return {
        "left": left_zone,
        "center": center_zone,
        "right": right_zone
    }