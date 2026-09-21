"""
Navigation Planner and Steering Decision for Customer-Aware AMR.
Computes direction costs, obstacle avoidance steering angles,
and steering commands with deadbands and limits.
"""

from typing import Dict, Tuple

def navigation_planner(zones: Dict[str, float]) -> Tuple[str, Dict[str, float]]:
    """
    Evaluates costs for candidate directions (LEFT, FORWARD, RIGHT).
    Applies forward-bias to favor straight-line motion when clear or tied.
    """
    left_cost = zones.get("left", 0.0)
    center_cost = zones.get("center", 0.0)
    right_cost = zones.get("right", 0.0)

    # Forward cost incorporates forward bias
    # If all zones are 0 (empty), forward bias ensures FORWARD is chosen.
    discounted_center = float(center_cost * 0.8) - 1e-6

    costs = {
        "FORWARD": discounted_center,
        "LEFT": float(left_cost),
        "RIGHT": float(right_cost)
    }

    best_direction = min(costs, key=costs.get)
    return best_direction, costs


def steering_from_zones(
    zones: Dict[str, float],
    steering_gain: float = 120.0,
    max_steering_angle: float = 45.0,
    deadband: float = 2.5
) -> float:
    """
    Computes steering angle from zone occupancies.
    Sign convention:
      + Angle = Steer LEFT (away from obstacle on RIGHT)
      - Angle = Steer RIGHT (away from obstacle on LEFT)
    """
    left = zones.get("left", 0.0)
    right = zones.get("right", 0.0)

    # Crowd on right -> positive steering (steer left)
    # Crowd on left -> negative steering (steer right)
    raw_steering = (right - left) * steering_gain

    # Deadband check to eliminate jitter around zero
    if abs(raw_steering) < deadband:
        return 0.0

    # Clamp to max steering limits
    return max(-max_steering_angle, min(max_steering_angle, raw_steering))