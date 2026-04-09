package com.airdefense.game

import com.badlogic.gdx.math.Vector3
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
}
