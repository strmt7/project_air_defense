package com.airdefense.game

import com.badlogic.gdx.math.Vector3
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

class BattleSkillBenchmarksTest {
    @Test
    fun `wave 1 sits inside the intended short-skill pressure envelope`() {
        val threats = BattleBalance.threatsForWave(1)
        val spawnInterval = BattleBalance.spawnIntervalForWave(1)
        val speedRange = BattleBalance.threatSpeedRangeForWave(1)

        assertTrue(threats in 8..10)
        assertTrue(spawnInterval in 2.1f..2.4f)
        assertTrue(speedRange.start >= 220f)
        assertTrue(speedRange.endInclusive <= 300f)
    }

    @Test
    fun `ballistic threat benchmark reaches the defended zone within the short horizon`() {
        val launch =
            ThreatFactory.createThreatLaunch(
                wave = 1,
                cityTarget = Vector3(220f, 0f, -260f),
                random =
                    object : RandomSource {
                        override fun range(
                            min: Float,
                            max: Float,
                        ): Float = (min + max) * 0.5f
                    },
            )

        val position = launch.start.cpy()
        val velocity = launch.velocity.cpy()
        var elapsed = 0f
        while (elapsed < 14f && position.y > 0f) {
            velocity.y += PhysicsModel.THREAT_GRAVITY_Y * 0.05f
            position.mulAdd(velocity, 0.05f)
            elapsed += 0.05f
        }

        assertTrue(elapsed <= 14f)
        assertTrue(abs(position.x - launch.target.x) < 170f)
        assertTrue(abs(position.z - launch.target.z) < 280f)
    }

    @Test
    fun `prox-fuze benchmark fires inside a single control window`() {
        val detonates =
            EngagementPhysics.closesWithinFuse(
                interceptorPos = Vector3(-40f, 0f, 0f),
                interceptorVel = Vector3(180f, 24f, -12f),
                targetPos = Vector3(55f, 18f, -8f),
                targetVel = Vector3(-135f, -8f, 4f),
                dt = 0.4f,
                fuseRadius = 34f,
            )

        assertTrue(detonates)
    }
}
