package com.airdefense.game

import com.badlogic.gdx.math.Vector3
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InterceptionMathTest {
    @Test
    fun `predict intercept leads a moving target`() {
        val interceptor = Vector3(0f, 0f, 0f)
        val target = Vector3(100f, 0f, 0f)
        val velocity = Vector3(10f, 0f, 0f)

        val intercept = InterceptionMath.predictInterceptPoint(interceptor, target, velocity, 50f)

        assertTrue(intercept.x > target.x)
        assertTrue(intercept.y == 0f)
    }

    @Test
    fun `predict intercept falls back to current position when impossible`() {
        val interceptor = Vector3(0f, 0f, 0f)
        val target = Vector3(0f, 0f, 100f)
        val velocity = Vector3(0f, 0f, 400f)

        val intercept = InterceptionMath.predictInterceptPoint(interceptor, target, velocity, 50f)

        assertTrue(intercept.dst(target) < 0.001f)
    }

    @Test
    fun `proximity fuse detects a crossing intercept window`() {
        val detonates = EngagementPhysics.closesWithinFuse(
            interceptorPos = Vector3(0f, 0f, 0f),
            interceptorVel = Vector3(100f, 0f, 0f),
            targetPos = Vector3(50f, 20f, 0f),
            targetVel = Vector3(-100f, -5f, 0f),
            dt = 0.5f,
            fuseRadius = 30f
        )

        assertTrue(detonates)
    }

    @Test
    fun `proximity fuse ignores distant crossing paths`() {
        val detonates = EngagementPhysics.closesWithinFuse(
            interceptorPos = Vector3(0f, 0f, 0f),
            interceptorVel = Vector3(40f, 0f, 0f),
            targetPos = Vector3(400f, 180f, 0f),
            targetVel = Vector3(-20f, 0f, 0f),
            dt = 0.5f,
            fuseRadius = 25f
        )

        assertFalse(detonates)
    }
}
