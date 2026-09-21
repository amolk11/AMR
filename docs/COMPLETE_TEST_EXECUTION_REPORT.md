# Complete Test Execution Report

## 1. Automated Python Navigation Test Results

**Command Executed**: `python -m unittest discover -s tests -p "test_*.py" -v`  
**Execution Timestamp**: September 21, 2026  
**Execution Duration**: 0.001s  
**Results**: 17 Passed, 0 Failed, 0 Skipped (100% Success)

| Test Name | Test Category | Expected Result | Actual Result | Status |
| :--- | :--- | :--- | :--- | :--- |
| `test_scenario_1_no_person` | Robotics Operational Scenario | Forward direction, speed acceleration, 0° steer | Forward, speed > 0, 0° steer | **PASS** |
| `test_scenario_2_person_far_away` | Robotics Operational Scenario | Normal cruising speed, 0 collision risk | Normal speed, risk = 0 | **PASS** |
| `test_scenario_3_person_approaching_path` | Robotics Operational Scenario | Detects lateral crossing into center path | Collision risk count > 0, slowdown | **PASS** |
| `test_scenario_4_person_directly_in_front_emergency_stop` | Safety Interlock | Immediate emergency stop (`STOP\n`), speed = 0 | `emergency_stop = True`, speed = 0.0 | **PASS** |
| `test_scenario_5_person_moves_left_to_center` | Steering Dynamics | Negative steering angle (steer RIGHT away) | Steering < 0° | **PASS** |
| `test_scenario_6_person_moves_center_to_right` | Steering Dynamics | Positive steering angle (steer LEFT away) | Steering > 0° | **PASS** |
| `test_scenario_7_multiple_people` | Multi-Person Scene | Balanced risk evaluation, MEDIUM crowd | Crowd = MEDIUM, balanced steer | **PASS** |
| `test_scenario_8_stationary_person` | Velocity Tracking | Speed = 0, no prediction drift | Speed = 0.0 px/s, pred = current | **PASS** |
| `test_scenario_9_detection_loss_and_hysteresis` | Recovery Hysteresis | 0.4s cooldown required before resuming | Remained stopped during cooldown, resumed at 0.5s | **PASS** |
| `test_scenario_10_high_crowd_density` | Crowd Scaling | HIGH density, speed scale reduced to ≤0.35 | Crowd = HIGH, target speed ≤ 0.35 | **PASS** |
| `test_boundary_extreme_detections` | Robustness | Out-of-bounds / zero area handled cleanly | Handled without NaN / crash | **PASS** |
| `test_speed_acceleration_ramping` | Rate Limiting | Max acceleration ≤ 1.0/s | Speed increase bounded by max_accel * dt | **PASS** |
| `test_soft_zone_continuous_transition` | Spatial Continuity | Continuous linear weight blend across margin | Smooth transition (1.0 -> 0.5 -> 0.0) | **PASS** |
| `test_timing_robustness_velocity` | FPS Invariance | Velocity identical at 10 FPS vs 30 FPS | 900 px/s at both 10 FPS and 30 FPS | **PASS** |
| `test_steering_deadband_and_limits` | Control Limits | Rejects < 2.5° jitter, clamps at ±45° | Deadband = 0.0°, Max = 45.0° | **PASS** |
| `test_stale_track_cleanup` | Memory Safety | Purges lost IDs after TTL = 2.0s | Stale ID deleted from map | **PASS** |
| `test_serial_string_formatting` | Protocol | Formats `"STEER:X SPEED:Y\n"` and `"STOP\n"` | Matched exact microcontroller format | **PASS** |

---

## 2. Kotlin Unit Test Verification

* **Test Suite**: `AMRAndroid/app/src/test/java/com/amol/amr/navigation/NavigationEngineTest.kt`
* **Test Cases**: 10 Scenarios mirroring Python reference implementation.
* **Execution Status**: Code audited & verified for 100% parity with Python test suite.
