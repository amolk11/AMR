import math

previous_positions = {}

def compute_speed(track_id, center):

    speed = 0

    if track_id in previous_positions:

        prev_x, prev_y = previous_positions[track_id]
        x, y = center

        speed = math.sqrt((x-prev_x)**2 + (y-prev_y)**2)

    previous_positions[track_id] = center

    return speed