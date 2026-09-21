import cv2

def draw_robot_path(frame, predicted_positions):

    h, w, _ = frame.shape

    robot_x = w // 2
    robot_y = h - 40

    offset = 0

    for px, py in predicted_positions:

        if abs(px - robot_x) < 120:
            offset = 200

    next_point = (robot_x + offset, h // 2)

    goal_point = (w // 2, 50)

    cv2.line(frame,(robot_x,robot_y),next_point,(0,255,0),4)
    cv2.line(frame,next_point,goal_point,(0,255,0),4)

    cv2.circle(frame,(robot_x,robot_y),15,(255,0,0),-1)

    cv2.putText(
        frame,
        "ROBOT",
        (robot_x-30,robot_y+30),
        cv2.FONT_HERSHEY_SIMPLEX,
        0.5,
        (255,0,0),
        2
    )

    return frame