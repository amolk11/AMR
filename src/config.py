"""
Centralized Configuration for Customer-Aware AMR Navigation.
Provides tunable parameters for zone sizing, steering, speed limits,
motion prediction, collision risk scoring, and temporal smoothing.
"""

from dataclasses import dataclass

@dataclass
class NavigationConfig:
    # -------------------------------------------------------------
    # Frame & Zone Geometry
    # -------------------------------------------------------------
    # Relative boundary split for FOV: [0..left_boundary], [left_boundary..right_boundary], [right_boundary..1.0]
    left_boundary_ratio: float = 1.0 / 3.0    # 0.333 of width
    right_boundary_ratio: float = 2.0 / 3.0   # 0.667 of width
    # Soft margin for zone transition to prevent step oscillations (ratio of frame width)
    zone_soft_margin_ratio: float = 0.05      # 5% soft blend margin

    # -------------------------------------------------------------
    # Steering Limits & Dynamics
    # -------------------------------------------------------------
    max_steering_angle: float = 45.0          # Max absolute steering angle in degrees
    steering_gain: float = 120.0              # Multiplier from zone occupancy imbalance to steering angle
    steering_deadband: float = 2.5            # Min angle delta (deg) required to change steering command
    steering_ema_alpha: float = 0.35          # Exponential Moving Average factor (0=freeze, 1=instant)

    # -------------------------------------------------------------
    # Speed Limits & Acceleration
    # -------------------------------------------------------------
    base_speed: float = 1.0                   # Max speed scale [0.0 .. 1.0]
    min_moving_speed: float = 0.20            # Min non-zero speed scale when creeping forward
    max_accel_rate: float = 1.0               # Max speed increase per second (e.g. 1.0 = 0 to 1 in 1s)
    max_decel_rate: float = 2.5               # Max speed decrease per second (faster braking)

    # Crowd Density Speed Multipliers
    speed_scale_empty: float = 1.00
    speed_scale_low: float = 0.85
    speed_scale_medium: float = 0.65
    speed_scale_high: float = 0.35

    # -------------------------------------------------------------
    # Proximity & Collision Risk Thresholds
    # -------------------------------------------------------------
    # Normalized bottom-y (ground contact) of person bounding box [0.0 top .. 1.0 bottom]
    emergency_stop_bottom_y: float = 0.78     # Person very close to robot front
    emergency_stop_area_ratio: float = 0.18   # Person occupies >18% of camera FOV
    caution_bottom_y: float = 0.50            # Person in intermediate distance range

    # Trajectory prediction collision horizon in seconds
    prediction_horizon_s: float = 0.8         # Project movement 0.8s into the future
    max_track_history_s: float = 2.0          # Max time to retain lost track IDs (TTL)
    velocity_ema_alpha: float = 0.40          # Velocity smoothing factor

    # Collision risk scoring weights
    center_proximity_weight: float = 3.0
    trajectory_crossing_weight: float = 2.0
    lateral_proximity_weight: float = 1.0

    # Emergency stop recovery hysteresis (frames of clear path before resuming motion)
    stop_recovery_cooldown_s: float = 0.4     # Must remain clear for 0.4s before resuming
