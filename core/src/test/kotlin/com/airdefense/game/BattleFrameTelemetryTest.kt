package com.airdefense.game

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BattleFrameTelemetryTest {
    @Test
    fun `snapshot reports rolling frame stats with stable formatting`() {
        val telemetry = BattleFrameTelemetry(windowSize = 4, logIntervalSeconds = 99f)
        val input = summaryInput()

        telemetry.onFrame(0.016f, input)
        telemetry.onFrame(0.018f, input)
        telemetry.onFrame(0.020f, input)
        telemetry.onFrame(1.146f, input)

        assertEquals(
            "LIVE 1s // FPS 13.2 // FT 18.0/20.0/250.0 // Q PERFORMANCE // FX5 // T2 I1",
            telemetry.snapshot(input),
        )
    }

    @Test
    fun `on frame only emits a log summary after the configured interval`() {
        val telemetry = BattleFrameTelemetry(windowSize = 4, logIntervalSeconds = 0.04f)
        val input = summaryInput()

        assertNull(telemetry.onFrame(0.02f, input))

        assertEquals(
            "LIVE 0s // FPS 40.0 // FT 20.0/20.0/30.0 // Q PERFORMANCE // FX5 // T2 I1",
            telemetry.onFrame(0.03f, input),
        )
    }

    @Test
    fun `snapshot handles empty telemetry state`() {
        val telemetry = BattleFrameTelemetry()

        assertEquals(
            "LIVE 0s // FPS 0.0 // FT 0.0/0.0/0.0 // Q PERFORMANCE // FX5 // T2 I1",
            telemetry.snapshot(summaryInput()),
        )
    }

    private fun summaryInput(): BattleFrameSummaryInput =
        BattleFrameSummaryInput(
            qualityLabel = "PERFORMANCE",
            effectCount = 5,
            threatCount = 2,
            interceptorCount = 1,
        )
}
