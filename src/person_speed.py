"""
Person Velocity and Speed Tracking for Customer-Aware AMR.
Computes smooth velocity vectors (vx, vy) and scalar speed (pixels/sec)
normalized by time difference (dt), avoiding FPS-dependent distortions.
"""

import math
import time
from typing import Dict, Tuple, Optional

class PersonSpeedTracker:
    def __init__(self, ema_alpha: float = 0.4, max_ttl_s: float = 2.0):
        self.ema_alpha = ema_alpha
        self.max_ttl_s = max_ttl_s
        # track_id -> (last_x, last_y, last_timestamp, vx_filtered, vy_filtered)
        self.tracks: Dict[int, Tuple[float, float, float, float, float]] = {}

    def update(self, track_id: int, center: Tuple[float, float], current_time: Optional[float] = None) -> Tuple[float, float, float]:
        """
        Updates tracking for a person and returns (speed_magnitude, vx, vy) in units per second.
        """
        if current_time is None:
            current_time = time.time()

        cx, cy = center

        if track_id not in self.tracks:
            self.tracks[track_id] = (cx, cy, current_time, 0.0, 0.0)
            return 0.0, 0.0, 0.0

        prev_x, prev_y, prev_t, prev_vx, prev_vy = self.tracks[track_id]
        dt = current_time - prev_t

        # Avoid zero division or massive jumps on paused frames
        if dt <= 1e-4:
            speed = math.hypot(prev_vx, prev_vy)
            return speed, prev_vx, prev_vy

        if dt > self.max_ttl_s:
            # Re-initialize after long gap
            self.tracks[track_id] = (cx, cy, current_time, 0.0, 0.0)
            return 0.0, 0.0, 0.0

        # Raw instantaneous velocity per second
        raw_vx = (cx - prev_x) / dt
        raw_vy = (cy - prev_y) / dt

        # Exponential moving average filter
        vx = self.ema_alpha * raw_vx + (1.0 - self.ema_alpha) * prev_vx
        vy = self.ema_alpha * raw_vy + (1.0 - self.ema_alpha) * prev_vy

        speed = math.hypot(vx, vy)
        self.tracks[track_id] = (cx, cy, current_time, vx, vy)
        return speed, vx, vy

    def cleanup_stale_tracks(self, current_time: float):
        """Removes tracks that haven't been updated for more than max_ttl_s."""
        stale_ids = [tid for tid, data in self.tracks.items() if (current_time - data[2]) > self.max_ttl_s]
        for tid in stale_ids:
            del self.tracks[tid]

    def reset(self):
        self.tracks.clear()


# Global instance for backward compatibility
_default_tracker = PersonSpeedTracker()

def compute_speed(track_id: int, center: Tuple[int, int], current_time: Optional[float] = None) -> float:
    """
    Backward-compatible helper function. Returns scalar speed.
    """
    speed, _, _ = _default_tracker.update(track_id, (float(center[0]), float(center[1])), current_time)
    return speed