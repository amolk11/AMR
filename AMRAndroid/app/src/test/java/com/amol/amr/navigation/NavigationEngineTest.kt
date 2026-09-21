package com.amol.amr.navigation

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class NavigationEngineTest {

    private lateinit var config: NavigationConfig
    private lateinit var engine: NavigationEngine
    private val frameW = 640
    private val frameH = 480

    @Before
    fun setUp() {
        config = NavigationConfig()
        engine = NavigationEngine(config)
    }

    @Test
    fun testScenario1_noPerson() {
        val t0 = 100.0
        var cmd: NavigationCommand? = null
        for (i in 0 until 5) {
            cmd = engine.process(emptyList(), frameW, frameH, t0 + i * 0.1)
        }

        assertNotNull(cmd)
        assertFalse(cmd!!.emergencyStop)
        assertEquals(0.0f, cmd.steering, 0.5f)
        assertTrue(cmd.speed > 0.0f)
        assertEquals("EMPTY", cmd.diagnostics["crowd_density"])
        assertEquals("FORWARD", cmd.diagnostics["direction_choice"])
    }

    @Test
    fun testScenario2_personFarAway() {
        val det = PersonDetection(x1 = 300f, y1 = 40f, x2 = 340f, y2 = 100f, trackId = 1)
        val cmd = engine.process(listOf(det), frameW, frameH, 100.0)

        assertFalse(cmd.emergencyStop)
        assertEquals(0.0f, cmd.diagnostics["collision_risk_count"] as Float, 0.01f)
    }

    @Test
    fun testScenario3_personApproachingPath() {
        val t0 = 100.0
        val det1 = PersonDetection(x1 = 50f, y1 = 260f, x2 = 110f, y2 = 380f, trackId = 10)
        engine.process(listOf(det1), frameW, frameH, t0)

        val det2 = PersonDetection(x1 = 120f, y1 = 260f, x2 = 180f, y2 = 380f, trackId = 10)
        val cmd2 = engine.process(listOf(det2), frameW, frameH, t0 + 0.1)

        @Suppress("UNCHECKED_CAST")
        val personStates = cmd2.diagnostics["person_states"] as List<PersonState>
        val pState = personStates[0]

        assertTrue(pState.vx > 0.0f)
        assertTrue(pState.isPredictedInPath)
        assertTrue((cmd2.diagnostics["collision_risk_count"] as Float) > 0.0f)
    }

    @Test
    fun testScenario4_personDirectlyInFront_emergencyStop() {
        val det = PersonDetection(x1 = 260f, y1 = 150f, x2 = 380f, y2 = 420f, trackId = 5)
        val cmd = engine.process(listOf(det), frameW, frameH, 100.0)

        assertTrue(cmd.emergencyStop)
        assertEquals(0.0f, cmd.speed, 0.001f)
        assertEquals("STOP\n", cmd.toSerialString())
    }

    @Test
    fun testScenario5_personLeft_steerRight() {
        val det = PersonDetection(x1 = 10f, y1 = 200f, x2 = 150f, y2 = 400f, trackId = 20)
        val cmd = engine.process(listOf(det), frameW, frameH, 100.0)

        assertTrue("Steering should be negative to steer RIGHT away from LEFT obstacle", cmd.steering < 0.0f)
    }

    @Test
    fun testScenario6_personRight_steerLeft() {
        val det = PersonDetection(x1 = 490f, y1 = 200f, x2 = 630f, y2 = 400f, trackId = 21)
        val cmd = engine.process(listOf(det), frameW, frameH, 100.0)

        assertTrue("Steering should be positive to steer LEFT away from RIGHT obstacle", cmd.steering > 0.0f)
    }

    @Test
    fun testScenario7_multiplePeople() {
        val dets = listOf(
            PersonDetection(x1 = 50f, y1 = 200f, x2 = 120f, y2 = 350f, trackId = 1),
            PersonDetection(x1 = 500f, y1 = 200f, x2 = 570f, y2 = 350f, trackId = 2),
            PersonDetection(x1 = 280f, y1 = 100f, x2 = 340f, y2 = 220f, trackId = 3)
        )
        val cmd = engine.process(dets, frameW, frameH, 100.0)

        assertEquals("MEDIUM", cmd.diagnostics["crowd_density"])
        assertEquals(0.0f, cmd.steering, 5.0f)
    }

    @Test
    fun testScenario8_stationaryPerson() {
        val t0 = 100.0
        val det = PersonDetection(x1 = 280f, y1 = 150f, x2 = 360f, y2 = 300f, trackId = 7)

        var cmd: NavigationCommand? = null
        for (i in 0 until 10) {
            cmd = engine.process(listOf(det), frameW, frameH, t0 + i * 0.033)
        }

        @Suppress("UNCHECKED_CAST")
        val personStates = cmd!!.diagnostics["person_states"] as List<PersonState>
        val pState = personStates[0]

        assertEquals(0.0f, pState.speed, 0.5f)
        assertEquals(Math.round(pState.cx), pState.predX)
        assertEquals(Math.round(pState.cy), pState.predY)
    }

    @Test
    fun testScenario9_recoveryHysteresis() {
        val t0 = 100.0
        val detClose = PersonDetection(x1 = 260f, y1 = 150f, x2 = 380f, y2 = 420f, trackId = 5)
        val cmdStop = engine.process(listOf(detClose), frameW, frameH, t0)
        assertTrue(cmdStop.emergencyStop)

        // Path clears immediately (0.1s later) -> Should still be stopped due to cooldown (0.4s)
        val cmdCooldown = engine.process(emptyList(), frameW, frameH, t0 + 0.1)
        assertTrue(cmdCooldown.emergencyStop)

        // After cooldown (0.5s later) -> Should resume
        val cmdResumed = engine.process(emptyList(), frameW, frameH, t0 + 0.5)
        assertFalse(cmdResumed.emergencyStop)
    }

    @Test
    fun testScenario10_highCrowdDensity() {
        val dets = (0 until 5).map { i ->
            PersonDetection(x1 = i * 100f, y1 = 100f, x2 = i * 100f + 50f, y2 = 200f, trackId = i)
        }
        val cmd = engine.process(dets, frameW, frameH, 100.0)

        assertEquals("HIGH", cmd.diagnostics["crowd_density"])
        assertTrue((cmd.diagnostics["target_speed"] as Float) <= config.speedScaleHigh)
    }
}
