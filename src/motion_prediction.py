previous_positions = {}

def predict_next_position(track_id, center):

    if track_id not in previous_positions:
        previous_positions[track_id] = center
        return center

    prev_x, prev_y = previous_positions[track_id]
    x, y = center

    dx = x - prev_x
    dy = y - prev_y

    predicted_x = int(x + dx * 3)
    predicted_y = int(y + dy * 3)

    previous_positions[track_id] = center

    return (predicted_x, predicted_y)