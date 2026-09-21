def navigation_planner(zones):

    left_cost = zones["left"]

    center_cost = zones["center"]

    right_cost = zones["right"]

    # prefer forward motion

    center_cost *= 0.8

    costs = {

        "LEFT": left_cost,

        "FORWARD": center_cost,

        "RIGHT": right_cost
    }

    best_direction = min(costs, key=costs.get)

    return best_direction, costs


# ======================================================
# STEERING COMPUTATION
# ======================================================

def steering_from_zones(zones):

    left = zones["left"]

    center = zones["center"]

    right = zones["right"]

    steering = 0

    # more crowd on left -> steer right
    # more crowd on right -> steer left

    steering += (right - left) * 120

    # limit steering

    steering = max(-45, min(45, steering))

    return steering