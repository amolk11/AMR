"""
Platform-Independent Navigation Engine for Customer-Aware AMR.

Decoupled from camera hardware, GUI/OpenCV, and motor interfaces.
Accepts structured detection inputs and outputs deterministic NavigationCommands
with smooth steering, rate-limited acceleration/deceleration, crowd awareness,
and proactive multi-person collision avoidance.
"""

import time
import math
from dataclasses import dataclass, field
from typing import List, Dict, Tuple, Optional, Any

from config import NavigationConfig
from crowd_analysis import CrowdDensity, analyze_crowd_density
from person_speed import PersonSpeedTracker
from motion_prediction import MotionPredictor
from navigation_zones import compute_zone_occupancies_from_boxes
from navigation_planner import navigation_planner, steering_from_zones


@dataclass
class PersonDetection:
    """Standardized representation of a detected person."""
    x1: float
    y1: float
    x2: float
    y2: float
    track_id: Optional[int] = None
    conf: float = 1.0
    keypoints: Optional[Any] = None

    @property
    def center(self) -> Tuple[float, float]:
        return ((self.x1 + self.x2) / 2.0, (self.y1 + self.y2) / 2.0)

    @property
    def width(self) -> float:
        return max(0.0, self.x2 - self.x1)

    @property
    def height(self) -> float:
        return max(0.0, self.y2 - self.y1)

    @property
    def area(self) -> float:
        return self.width * self.height


@dataclass
class PersonState:
    """Processed state for an individual person in the scene."""
    track_id: int
    cx: float
    cy: float
    vx: float
    vy: float
    speed: float
    pred_x: int
    pred_y: int
    bottom_y_norm: float
    area_ratio: float
    is_in_center_corridor: bool
    is_predicted_in_path: bool
    proximity_risk: float


@dataclass
class NavigationCommand:
    """Deterministic output command for robot actuation."""
    steering: float                 # Steering angle in degrees (-45.0 to +45.0)
    speed: float                    # Speed scale [0.0 to 1.0]
    emergency_stop: bool            # True if immediate halt required
    reason: str                     # Human-readable explanation of decision
    diagnostics: Dict[str, Any] = field(default_factory=dict)

    def to_serial_string(self) -> str:
        """Formats command for serial / micro-controller communication."""
        if self.emergency_stop or self.speed <= 0.0:
            return "STOP\n"
        return f"STEER:{int(round(self.steering))} SPEED:{self.speed:.2f}\n"


class NavigationEngine:
    """
    Core navigation decision engine. Maintains temporal state, filters,
    and produces rate-limited commands.
    """
    def __init__(self, config: Optional[NavigationConfig] = None):
        self.config = config or NavigationConfig()
        self.speed_tracker = PersonSpeedTracker(
            ema_alpha=self.config.velocity_ema_alpha,
            max_ttl_s=self.config.max_track_history_s
        )
        self.motion_predictor = MotionPredictor(
            horizon_s=self.config.prediction_horizon_s,
            ema_alpha=self.config.velocity_ema_alpha,
            max_ttl_s=self.config.max_track_history_s
        )

        # Temporal filter states
        self.current_steering: float = 0.0
        self.current_speed: float = 0.0
        self.last_update_time: Optional[float] = None

        # Stop state hysteresis
        self.is_emergency_stopped: bool = False
        self.clear_path_start_time: Optional[float] = None

    def reset(self):
        """Resets all internal filters, trackers, and state."""
        self.speed_tracker.reset()
        self.motion_predictor.reset()
        self.current_steering = 0.0
        self.current_speed = 0.0
        self.last_update_time = None
        self.is_emergency_stopped = False
        self.clear_path_start_time = None

    def process(
        self,
        detections: List[PersonDetection],
        frame_width: int,
        frame_height: int,
        current_time: Optional[float] = None
    ) -> NavigationCommand:
        """
        Executes one full navigation planning cycle.
        """
        if current_time is None:
            current_time = time.time()

        # Compute dt
        if self.last_update_time is None:
            dt = 0.033  # Default nominal ~30 FPS on first frame
        else:
            dt = max(1e-4, min(1.0, current_time - self.last_update_time))
        self.last_update_time = current_time

        # Cleanup expired track histories
        self.speed_tracker.cleanup_stale_tracks(current_time)
        self.motion_predictor.cleanup_stale_tracks(current_time)

        # 1. Spatial Occupancy Analysis
        boxes = [(d.x1, d.y1, d.x2, d.y2) for d in detections]
        zones = compute_zone_occupancies_from_boxes(
            boxes=boxes,
            frame_width=float(frame_width),
            frame_height=float(frame_height),
            left_ratio=self.config.left_boundary_ratio,
            right_ratio=self.config.right_boundary_ratio,
            soft_margin_ratio=self.config.zone_soft_margin_ratio
        )

        direction_choice, zone_costs = navigation_planner(zones)
        raw_target_steering = steering_from_zones(
            zones=zones,
            steering_gain=self.config.steering_gain,
            max_steering_angle=self.config.max_steering_angle,
            deadband=self.config.steering_deadband
        )

        # 2. Crowd Density Analysis
        people_count = len(detections)
        crowd_density = analyze_crowd_density(people_count)

        if crowd_density == CrowdDensity.EMPTY.value:
            crowd_speed_scale = self.config.speed_scale_empty
        elif crowd_density == CrowdDensity.LOW.value:
            crowd_speed_scale = self.config.speed_scale_low
        elif crowd_density == CrowdDensity.MEDIUM.value:
            crowd_speed_scale = self.config.speed_scale_medium
        else:
            crowd_speed_scale = self.config.speed_scale_high

        # 3. Multi-Person State Tracking & Risk Evaluation
        total_frame_area = max(1.0, float(frame_width * frame_height))
        center_x1 = frame_width * self.config.left_boundary_ratio
        center_x2 = frame_width * self.config.right_boundary_ratio

        person_states: List[PersonState] = []
        immediate_stop_triggered = False
        stop_reasons = []
        total_collision_risk = 0.0

        for idx, det in enumerate(detections):
            tid = det.track_id if det.track_id is not None else (10000 + idx)
            cx, cy = det.center
            speed, vx, vy = self.speed_tracker.update(tid, (cx, cy), current_time)
            pred_x, pred_y = self.motion_predictor.predict(tid, (cx, cy), current_time, vx, vy)

            bottom_y_norm = det.y2 / float(frame_height)
            area_ratio = det.area / total_frame_area
            in_center = (center_x1 <= cx <= center_x2)
            predicted_in_path = (center_x1 <= pred_x <= center_x2) and (pred_y > frame_height * self.config.caution_bottom_y)

            # Proximity Risk Evaluation
            person_risk = 0.0

            # Direct frontal close proximity (Critical Safety Check)
            if in_center:
                if bottom_y_norm >= self.config.emergency_stop_bottom_y:
                    immediate_stop_triggered = True
                    stop_reasons.append(f"Person {tid} directly in front (dist close: y={bottom_y_norm:.2f})")
                elif area_ratio >= self.config.emergency_stop_area_ratio:
                    immediate_stop_triggered = True
                    stop_reasons.append(f"Person {tid} large area in center (area={area_ratio:.2f})")
                else:
                    # Intermediate proximity in center
                    proximity_factor = max(0.0, (bottom_y_norm - self.config.caution_bottom_y) / (1.0 - self.config.caution_bottom_y))
                    person_risk += proximity_factor * self.config.center_proximity_weight

            # Lateral trajectory crossing into robot path
            if predicted_in_path:
                person_risk += self.config.trajectory_crossing_weight
                total_collision_risk += 1.0

            if not in_center and bottom_y_norm >= self.config.emergency_stop_bottom_y:
                # Close on side
                person_risk += self.config.lateral_proximity_weight

            p_state = PersonState(
                track_id=tid,
                cx=cx,
                cy=cy,
                vx=vx,
                vy=vy,
                speed=speed,
                pred_x=pred_x,
                pred_y=pred_y,
                bottom_y_norm=bottom_y_norm,
                area_ratio=area_ratio,
                is_in_center_corridor=in_center,
                is_predicted_in_path=predicted_in_path,
                proximity_risk=person_risk
            )
            person_states.append(p_state)

        # 4. Emergency Stop and Recovery Hysteresis
        if immediate_stop_triggered or total_collision_risk >= 3.0:
            self.is_emergency_stopped = True
            self.clear_path_start_time = None
        else:
            if self.is_emergency_stopped:
                # In recovery mode: wait for clear path duration
                if self.clear_path_start_time is None:
                    self.clear_path_start_time = current_time
                elif (current_time - self.clear_path_start_time) >= self.config.stop_recovery_cooldown_s:
                    self.is_emergency_stopped = False
                    self.clear_path_start_time = None

        # 5. Speed Planning with Distance Scaling & Rate Limiting
        if self.is_emergency_stopped:
            target_speed = 0.0
            primary_reason = stop_reasons[0] if stop_reasons else "COLLISION_RISK_HIGH"
        else:
            # Base speed scaled by crowd
            target_speed = self.config.base_speed * crowd_speed_scale

            # Slow down smoothly according to collision risk
            if total_collision_risk >= 2.0:
                target_speed *= 0.50
            elif total_collision_risk >= 1.0:
                target_speed *= 0.75

            # Distance-based slowdown from most threatening center obstacle
            max_center_risk = max([p.proximity_risk for p in person_states if p.is_in_center_corridor], default=0.0)
            if max_center_risk > 0.0:
                risk_decay = max(0.2, 1.0 - (max_center_risk / (self.config.center_proximity_weight * 1.5)))
                target_speed *= risk_decay

            # Ensure minimum moving speed if not stopped
            if target_speed > 0.0:
                target_speed = max(self.config.min_moving_speed, target_speed)

            primary_reason = f"{direction_choice} (Crowd: {crowd_density})"

        # Apply acceleration / deceleration limits
        if target_speed > self.current_speed:
            max_increase = self.config.max_accel_rate * dt
            self.current_speed = min(target_speed, self.current_speed + max_increase)
        else:
            max_decrease = self.config.max_decel_rate * dt
            # If emergency stop, apply immediate deceleration
            if self.is_emergency_stopped:
                self.current_speed = 0.0
            else:
                self.current_speed = max(target_speed, self.current_speed - max_decrease)

        # 6. Steering Smoothing & Deadband
        # Exponential moving average filter on steering angle
        alpha = self.config.steering_ema_alpha
        self.current_steering = alpha * raw_target_steering + (1.0 - alpha) * self.current_steering

        if abs(self.current_steering) < (self.config.steering_deadband / 2.0):
            self.current_steering = 0.0

        # Build diagnostic telemetry
        diagnostics = {
            "people_count": people_count,
            "crowd_density": crowd_density,
            "collision_risk_count": total_collision_risk,
            "zones": zones,
            "direction_choice": direction_choice,
            "raw_steering": raw_target_steering,
            "target_speed": target_speed,
            "dt": dt,
            "person_states": person_states
        }

        return NavigationCommand(
            steering=float(self.current_steering),
            speed=float(self.current_speed),
            emergency_stop=self.is_emergency_stopped,
            reason=primary_reason,
            diagnostics=diagnostics
        )
