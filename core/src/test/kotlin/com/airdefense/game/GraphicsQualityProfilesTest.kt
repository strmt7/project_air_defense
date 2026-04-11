package com.airdefense.game

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GraphicsQualityProfilesTest {
    @Test
    fun `auto quality maps emulator to performance`() {
        val effective =
            GraphicsQualityProfiles.effectiveMode(
                requestedMode = GraphicsQualityMode.AUTO,
                deviceClass = DevicePerformanceClass.EMULATOR,
            )

        assertEquals(GraphicsQualityMode.PERFORMANCE, effective)
    }

    @Test
    fun `auto quality maps higher-end devices to balanced`() {
        val effective =
            GraphicsQualityProfiles.effectiveMode(
                requestedMode = GraphicsQualityMode.AUTO,
                deviceClass = DevicePerformanceClass.HIGH,
            )

        assertEquals(GraphicsQualityMode.BALANCED, effective)
    }

    @Test
    fun `performance profile strips heaviest layers and reduces budgets`() {
        val performance =
            GraphicsQualityProfiles.resolve(
                requestedMode = GraphicsQualityMode.PERFORMANCE,
                deviceClass = DevicePerformanceClass.LOW,
            )
        val balanced =
            GraphicsQualityProfiles.resolve(
                requestedMode = GraphicsQualityMode.BALANCED,
                deviceClass = DevicePerformanceClass.HIGH,
            )

        assertFalse(performance.showReflectionLayer)
        assertFalse(performance.showMoon)
        assertTrue(balanced.showReflectionLayer)
        assertTrue(balanced.maxTrailEffects > performance.maxTrailEffects)
        assertTrue(balanced.maxDebrisPieces > performance.maxDebrisPieces)
        assertTrue(balanced.effectBudgetScale > performance.effectBudgetScale)
    }
}
