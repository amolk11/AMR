import cv2

def draw_info(frame, people_count, decision):

    cv2.putText(
        frame,
        f"Customers: {people_count}",
        (20,40),
        cv2.FONT_HERSHEY_SIMPLEX,
        0.9,
        (0,255,0),
        2
    )

    cv2.putText(
        frame,
        decision,
        (20,80),
        cv2.FONT_HERSHEY_SIMPLEX,
        0.7,
        (255,255,255),
        2
    )

    return frame