package com.airdefense.game

import com.badlogic.gdx.math.Vector3
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.math.abs

class ThreatFactoryTest {
    @Test
    fun `threat factory produces downward-moving attack profile toward city`() {
        val values = ArrayDeque(
            listOf(
                0f,
                1800f,
                0f,
                0f,
                300f,
                250f
            )
        )
        val random = object : RandomSource {
            override fun range(min: Float, max: Float): Float = values.removeFirst()
        }

        val launch = ThreatFactory.createThreatLaunch(4, Vector3(50f, 0f, -200f), random)

        assertEquals(-4200f, launch.start.z)
        assertTrue(launch.target.z < 0f)
        assertTrue(launch.velocity.z > 0f)
        assertTrue(launch.velocity.y > 0f)
    }

    @Test
    fun `ballistic launch reaches intended target under gravity`() {
        val launch = ThreatFactory.createThreatLaunch(
            wave = 2,
            cityTarget = Vector3(120f, 0f, -180f),
            random = object : RandomSource {
                override fun range(min: Float, max: Float): Float = (min + max) * 0.5f
            }
        )

        val position = launch.start.cpy()
        val velocity = launch.velocity.cpy()
        var steps = 0
        while (steps < 320 && position.y > 0f) {
            velocity.y += PhysicsModel.THREAT_GRAVITY_Y * 0.05f
            position.mulAdd(velocity, 0.05f)
            steps++
        }

        assertTrue(abs(position.x - launch.target.x) < 160f)
        assertTrue(abs(position.z - launch.target.z) < 260f)
        assertTrue(position.y < 120f)
    }
}
