"""
Motion Trajectory Prediction for Customer-Aware AMR.
Projects pedestrian future positions using constant-velocity model
over a physical prediction horizon (seconds).
"""

from typing import Tuple, Dict, Optional
import time

class MotionPredictor:
    def __init__(self, horizon_s: float = 0.8, ema_alpha: float = 0.4, max_ttl_s: float = 2.0):
        self.horizon_s = horizon_s
        self.ema_alpha = ema_alpha
        self.max_ttl_s = max_ttl_s
        # track_id -> (last_x, last_y, last_t, vx, vy)
        self.tracks: Dict[int, Tuple[float, float, float, float, float]] = {}

    def predict(
        self,
        track_id: int,
        center: Tuple[float, float],
        current_time: Optional[float] = None,
        vx: Optional[float] = None,
        vy: Optional[float] = None
    ) -> Tuple[int, int]:
        """
        Projects future position (px, py) at (current_time + horizon_s).
        """
        if current_time is None:
            current_time = time.time()

        cx, cy = center

        if vx is None or vy is None:
            # Estimate velocity internally
            if track_id not in self.tracks:
                self.tracks[track_id] = (cx, cy, current_time, 0.0, 0.0)
                return (int(cx), int(cy))

            prev_x, prev_y, prev_t, prev_vx, prev_vy = self.tracks[track_id]
            dt = current_time - prev_t

            if dt <= 1e-4:
                vx, vy = prev_vx, prev_vy
            elif dt > self.max_ttl_s:
                self.tracks[track_id] = (cx, cy, current_time, 0.0, 0.0)
                return (int(cx), int(cy))
            else:
                raw_vx = (cx - prev_x) / dt
                raw_vy = (cy - prev_y) / dt
                vx = self.ema_alpha * raw_vx + (1.0 - self.ema_alpha) * prev_vx
                vy = self.ema_alpha * raw_vy + (1.0 - self.ema_alpha) * prev_vy

            self.tracks[track_id] = (cx, cy, current_time, vx, vy)

        pred_x = cx + vx * self.horizon_s
        pred_y = cy + vy * self.horizon_s

        return (int(round(pred_x)), int(round(pred_y)))

    def cleanup_stale_tracks(self, current_time: float):
        stale_ids = [tid for tid, data in self.tracks.items() if (current_time - data[2]) > self.max_ttl_s]
        for tid in stale_ids:
            del self.tracks[tid]

    def reset(self):
        self.tracks.clear()


# Global instance for backward compatibility
_default_predictor = MotionPredictor()

def predict_next_position(track_id: int, center: Tuple[int, int]) -> Tuple[int, int]:
    """
    Backward-compatible prediction function.
    """
    return _default_predictor.predict(track_id, (float(center[0]), float(center[1])))