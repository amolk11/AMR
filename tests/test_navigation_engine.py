"""
Comprehensive Unit and Scenario Test Suite for Customer-Aware AMR Navigation Logic.
Validates all 10 operational robotics scenarios, boundary conditions,
rate limiting, smoothing, and temporal robustness.
"""

import sys
import os
import unittest
import math

# Add src to python path for testing
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "src")))

from config import NavigationConfig
from crowd_analysis import CrowdDensity, analyze_crowd_density
from person_speed import PersonSpeedTracker, compute_speed
from motion_prediction import MotionPredictor, predict_next_position
from navigation_zones import compute_soft_zone_weights, compute_zone_occupancies_from_boxes
from navigation_planner import navigation_planner, steering_from_zones
from navigation_engine import NavigationEngine, PersonDetection, NavigationCommand


class TestNavigationEngine(unittest.TestCase):

    def setUp(self):
        self.config = NavigationConfig()
        self.engine = NavigationEngine(self.config)
        self.w = 640
        self.h = 480

    # -------------------------------------------------------------
    # SCENARIO 1: No Person Detected
    # -------------------------------------------------------------
    def test_scenario_1_no_person(self):
        """Scenario 1: No person detected -> Straight forward, accelerates towards max speed."""
        t0 = 100.0
        cmd = None
        for i in range(5):
            cmd = self.engine.process(
                detections=[],
                frame_width=self.w,
                frame_height=self.h,
                current_time=t0 + i * 0.1
            )

        self.assertFalse(cmd.emergency_stop)
        self.assertAlmostEqual(cmd.steering, 0.0, places=1)
        self.assertGreater(cmd.speed, 0.0)
        self.assertEqual(cmd.diagnostics["crowd_density"], CrowdDensity.EMPTY.value)
        self.assertEqual(cmd.diagnostics["direction_choice"], "FORWARD")

    # -------------------------------------------------------------
    # SCENARIO 2: Person Far Away
    # -------------------------------------------------------------
    def test_scenario_2_person_far_away(self):
        """Scenario 2: Person far away (small bbox at top of screen) -> Normal navigation."""
        det = PersonDetection(x1=300, y1=40, x2=340, y2=100, track_id=1)
        cmd = self.engine.process([det], self.w, self.h, current_time=100.0)

        self.assertFalse(cmd.emergency_stop)
        self.assertEqual(cmd.diagnostics["collision_risk_count"], 0)

    # -------------------------------------------------------------
    # SCENARIO 3: Person Approaching Robot Path (Crossing)
    # -------------------------------------------------------------
    def test_scenario_3_person_approaching_path(self):
        """Scenario 3: Person moving laterally from left into center corridor -> Anticipatory slowdown."""
        t0 = 100.0
        det1 = PersonDetection(x1=50, y1=260, x2=110, y2=380, track_id=10)
        self.engine.process([det1], self.w, self.h, current_time=t0)

        det2 = PersonDetection(x1=120, y1=260, x2=180, y2=380, track_id=10)
        cmd2 = self.engine.process([det2], self.w, self.h, current_time=t0 + 0.1)

        p_state = cmd2.diagnostics["person_states"][0]
        self.assertGreater(p_state.vx, 0.0)
        self.assertTrue(p_state.is_predicted_in_path)
        self.assertGreater(cmd2.diagnostics["collision_risk_count"], 0)

    # -------------------------------------------------------------
    # SCENARIO 4: Person Directly in Front (Emergency Stop)
    # -------------------------------------------------------------
    def test_scenario_4_person_directly_in_front_emergency_stop(self):
        """Scenario 4: Single person standing close in front -> Immediate emergency stop."""
        det = PersonDetection(x1=260, y1=150, x2=380, y2=420, track_id=5)
        cmd = self.engine.process([det], self.w, self.h, current_time=100.0)

        self.assertTrue(cmd.emergency_stop)
        self.assertEqual(cmd.speed, 0.0)
        self.assertEqual(cmd.to_serial_string(), "STOP\n")

    # -------------------------------------------------------------
    # SCENARIO 5: Person Moves Left -> Center (Steer Right away)
    # -------------------------------------------------------------
    def test_scenario_5_person_moves_left_to_center(self):
        """Scenario 5: Obstacle on Left -> Steer Right (negative angle)."""
        det = PersonDetection(x1=10, y1=200, x2=150, y2=400, track_id=20)
        cmd = self.engine.process([det], self.w, self.h, current_time=100.0)

        self.assertLess(cmd.steering, 0.0)
        self.assertLessEqual(cmd.steering, -self.config.steering_deadband / 2.0)

    # -------------------------------------------------------------
    # SCENARIO 6: Person Moves Center -> Right (Steer Left away)
    # -------------------------------------------------------------
    def test_scenario_6_person_moves_center_to_right(self):
        """Scenario 6: Obstacle on Right -> Steer Left (positive angle)."""
        det = PersonDetection(x1=490, y1=200, x2=630, y2=400, track_id=21)
        cmd = self.engine.process([det], self.w, self.h, current_time=100.0)

        self.assertGreater(cmd.steering, 0.0)
        self.assertGreaterEqual(cmd.steering, self.config.steering_deadband / 2.0)

    # -------------------------------------------------------------
    # SCENARIO 7: Multiple People Distributed
    # -------------------------------------------------------------
    def test_scenario_7_multiple_people(self):
        """Scenario 7: Multiple people -> Planner evaluates combined risk and balanced steering."""
        dets = [
            PersonDetection(x1=50, y1=200, x2=120, y2=350, track_id=1),
            PersonDetection(x1=500, y1=200, x2=570, y2=350, track_id=2),
            PersonDetection(x1=280, y1=100, x2=340, y2=220, track_id=3)
        ]
        cmd = self.engine.process(dets, self.w, self.h, current_time=100.0)

        self.assertEqual(len(cmd.diagnostics["person_states"]), 3)
        self.assertEqual(cmd.diagnostics["crowd_density"], CrowdDensity.MEDIUM.value)
        self.assertAlmostEqual(cmd.steering, 0.0, delta=5.0)

    # -------------------------------------------------------------
    # SCENARIO 8: Stationary Person
    # -------------------------------------------------------------
    def test_scenario_8_stationary_person(self):
        """Scenario 8: Stationary person -> Velocity is zero and prediction does not drift."""
        t0 = 100.0
        det = PersonDetection(x1=280, y1=150, x2=360, y2=300, track_id=7)

        cmd = None
        for i in range(10):
            cmd = self.engine.process([det], self.w, self.h, current_time=t0 + i * 0.033)

        p_state = cmd.diagnostics["person_states"][0]
        self.assertAlmostEqual(p_state.speed, 0.0, places=1)
        self.assertAlmostEqual(p_state.pred_x, int(p_state.cx), delta=1)
        self.assertAlmostEqual(p_state.pred_y, int(p_state.cy), delta=1)

    # -------------------------------------------------------------
    # SCENARIO 9: Temporary Detection Loss & Recovery Hysteresis
    # -------------------------------------------------------------
    def test_scenario_9_detection_loss_and_hysteresis(self):
        """Scenario 9: After emergency stop, system requires clear path cooldown before resuming."""
        t0 = 100.0
        det_close = PersonDetection(x1=260, y1=150, x2=380, y2=420, track_id=5)
        cmd_stop = self.engine.process([det_close], self.w, self.h, current_time=t0)
        self.assertTrue(cmd_stop.emergency_stop)

        cmd_cooldown = self.engine.process([], self.w, self.h, current_time=t0 + 0.1)
        self.assertTrue(cmd_cooldown.emergency_stop)

        cmd_resumed = self.engine.process([], self.w, self.h, current_time=t0 + 0.5)
        self.assertFalse(cmd_resumed.emergency_stop)

    # -------------------------------------------------------------
    # SCENARIO 10: High Crowd Density
    # -------------------------------------------------------------
    def test_scenario_10_high_crowd_density(self):
        """Scenario 10: 5+ people detected -> Crowd density is HIGH, max target speed scale reduced."""
        dets = [
            PersonDetection(x1=i*100, y1=100, x2=i*100+50, y2=200, track_id=i)
            for i in range(5)
        ]
        cmd = self.engine.process(dets, self.w, self.h, current_time=100.0)

        self.assertEqual(cmd.diagnostics["crowd_density"], CrowdDensity.HIGH.value)
        self.assertLessEqual(cmd.diagnostics["target_speed"], self.config.speed_scale_high)

    # -------------------------------------------------------------
    # REGRESSION & BOUNDARY TESTS
    # -------------------------------------------------------------
    def test_speed_acceleration_ramping(self):
        """Verify speed ramping respects max acceleration per second."""
        t0 = 100.0
        # dt = 0.1s, max accel = 1.0/s -> max speed increase per step = 0.10
        cmd1 = self.engine.process([], self.w, self.h, current_time=t0)
        cmd2 = self.engine.process([], self.w, self.h, current_time=t0 + 0.1)

        speed_increase = cmd2.speed - cmd1.speed
        self.assertLessEqual(speed_increase, self.config.max_accel_rate * 0.1 + 1e-4)

    def test_serial_string_formatting(self):
        """Verify serial strings match expected micro-controller format."""
        cmd_move = NavigationCommand(steering=15.4, speed=0.654, emergency_stop=False, reason="CLEAR")
        self.assertEqual(cmd_move.to_serial_string(), "STEER:15 SPEED:0.65\n")

        cmd_stop = NavigationCommand(steering=0.0, speed=0.0, emergency_stop=True, reason="STOP")
        self.assertEqual(cmd_stop.to_serial_string(), "STOP\n")

    def test_soft_zone_continuous_transition(self):
        """Verify soft-margin transition prevents step discontinuities."""
        width = 600.0
        w_l1, w_c1, _ = compute_soft_zone_weights(160, width, 1/3, 2/3, 0.05)
        w_l2, w_c2, _ = compute_soft_zone_weights(200, width, 1/3, 2/3, 0.05)
        w_l3, w_c3, _ = compute_soft_zone_weights(240, width, 1/3, 2/3, 0.05)

        self.assertEqual((w_l1, w_c1), (1.0, 0.0))
        self.assertAlmostEqual(w_l2, 0.5, places=2)
        self.assertAlmostEqual(w_c2, 0.5, places=2)
        self.assertEqual((w_l3, w_c3), (0.0, 1.0))

    def test_timing_robustness_velocity(self):
        """Verify velocity is identical at 10 FPS (dt=0.1) vs 30 FPS (dt=0.0333) for same physical movement."""
        tracker_30fps = PersonSpeedTracker(ema_alpha=1.0)
        tracker_10fps = PersonSpeedTracker(ema_alpha=1.0)

        tracker_30fps.update(1, (100, 100), current_time=0.0)
        s30, _, _ = tracker_30fps.update(1, (130, 100), current_time=0.03333333)

        tracker_10fps.update(1, (100, 100), current_time=0.0)
        s10, _, _ = tracker_10fps.update(1, (190, 100), current_time=0.1)

        self.assertAlmostEqual(s30, 900.0, delta=2.0)
        self.assertAlmostEqual(s10, 900.0, delta=2.0)

    def test_steering_deadband_and_limits(self):
        """Verify steering deadband rejects micro-angles and clamps to limits."""
        steer_micro = steering_from_zones({"left": 0.01, "right": 0.015}, steering_gain=120.0, deadband=2.5)
        self.assertEqual(steer_micro, 0.0)

        steer_max = steering_from_zones({"left": 0.0, "right": 0.8}, steering_gain=120.0, max_steering_angle=45.0)
        self.assertEqual(steer_max, 45.0)

    def test_stale_track_cleanup(self):
        """Verify stale track IDs are automatically purged after max_ttl_s."""
        tracker = PersonSpeedTracker(max_ttl_s=1.0)
        tracker.update(99, (50, 50), current_time=1.0)
        self.assertIn(99, tracker.tracks)

        tracker.cleanup_stale_tracks(current_time=3.0)
        self.assertNotIn(99, tracker.tracks)

    def test_boundary_extreme_detections(self):
        """Verify engine handles empty, zero area, and out-of-bounds coordinates without crashing."""
        dets = [
            PersonDetection(x1=-50, y1=-20, x2=100, y2=150, track_id=101),  # Negative / partially clipped
            PersonDetection(x1=200, y1=200, x2=200, y2=200, track_id=102),  # Zero area
        ]
        cmd = self.engine.process(dets, self.w, self.h, current_time=100.0)
        self.assertIsInstance(cmd, NavigationCommand)
        self.assertFalse(math.isnan(cmd.steering))
        self.assertFalse(math.isnan(cmd.speed))


if __name__ == "__main__":
    unittest.main()
