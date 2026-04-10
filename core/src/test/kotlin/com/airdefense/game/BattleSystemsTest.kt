package com.airdefense.game

import com.badlogic.gdx.math.Vector3
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BattleSystemsTest {
    @Test
    fun `predict intercept point stays ahead of inbound target`() {
        val interceptorPos = Vector3(0f, 0f, 0f)
        val targetPos = Vector3(0f, 200f, -600f)
        val targetVelocity = Vector3(0f, -20f, 180f)

        val intercept = InterceptionMath.predictInterceptPoint(
            interceptorPos = interceptorPos,
            targetPos = targetPos,
            targetVelocity = targetVelocity,
            interceptorSpeed = 420f
        )

        assertTrue(intercept.z > targetPos.z)
        assertTrue(intercept.y <= targetPos.y)
    }

    @Test
    fun `hostile impact does more damage than defensive blast at same distance`() {
        val building = Vector3(0f, 0f, 0f)
        val impact = Vector3(40f, 0f, 0f)

        val hostileDamage = DamageModel.computeBuildingDamage(
            buildingPosition = building,
            buildingWidth = 60f,
            buildingDepth = 60f,
            impactPosition = impact,
            blastRadius = 80f,
            hostile = true
        )
        val defensiveDamage = DamageModel.computeBuildingDamage(
            buildingPosition = building,
            buildingWidth = 60f,
            buildingDepth = 60f,
            impactPosition = impact,
            blastRadius = 80f,
            hostile = false
        )

        assertTrue(hostileDamage > defensiveDamage)
    }

    @Test
    fun `threat factory always launches from northern approach`() {
        val launch = ThreatFactory.createThreatLaunch(
            wave = 3,
            cityTarget = Vector3(0f, 0f, 0f),
            random = object : RandomSource {
                override fun range(min: Float, max: Float): Float = (min + max) * 0.5f
            }
        )

        assertEquals(-4200f, launch.start.z)
        assertTrue(launch.velocity.len() > 0f)
        assertTrue(launch.velocity.y > 0f)
    }
}
