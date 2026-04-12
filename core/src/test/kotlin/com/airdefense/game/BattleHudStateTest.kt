package com.airdefense.game

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BattleHudStateTest {
    @Test
    fun `wave state shows live hostile count during active wave`() {
        val snapshot =
            snapshot(
                HudSnapshotFixture(
                    waveInProgress = true,
                    visibleThreats = 6,
                    remainingThreatsInWave = 4,
                ),
            )

        assertEquals("WAVE 3 LIVE / 10 HOSTILES", snapshot.waveStateText())
        assertEquals("WAVE 3 ACTIVE", snapshot.waveButtonState().text)
        assertTrue(snapshot.waveButtonState().disabled)
    }

    @Test
    fun `wave state shows ready when no wave is active`() {
        val snapshot =
            snapshot(
                HudSnapshotFixture(
                    waveInProgress = false,
                    visibleThreats = 0,
                    remainingThreatsInWave = 11,
                ),
            )

        assertEquals("WAVE 3 READY", snapshot.waveStateText())
        assertEquals("START NEXT WAVE", snapshot.waveButtonState().text)
        assertFalse(snapshot.waveButtonState().disabled)
    }

    @Test
    fun `game over state dominates wave presentation`() {
        val snapshot =
            snapshot(
                HudSnapshotFixture(
                    isGameOver = true,
                    waveInProgress = true,
                    visibleThreats = 9,
                    remainingThreatsInWave = 2,
                ),
            )

        assertEquals("STATUS LOST", snapshot.waveStateText())
        assertEquals("DEFENSE FAILED", snapshot.waveButtonState().text)
        assertTrue(snapshot.waveButtonState().disabled)
    }

    @Test
    fun `summary text includes core command metrics`() {
        val snapshot =
            snapshot(
                HudSnapshotFixture(
                    cityIntegrity = 87.6f,
                    score = 2400,
                    credits = 10820,
                    visibleThreats = 3,
                    remainingThreatsInWave = 1,
                ),
            )

        assertEquals("2825 M", snapshot.rangeText())
        assertEquals("82 M", snapshot.fuseText())
        assertEquals(
            "CITY 87%   SCORE 2400   CR 10820   WAVE 3 READY",
            snapshot.summaryText(),
        )
    }

    private fun snapshot(fixture: HudSnapshotFixture = HudSnapshotFixture()): BattleHudSnapshot =
        BattleHudSnapshot(
            cityIntegrity = fixture.cityIntegrity,
            score = fixture.score,
            credits = fixture.credits,
            wave = fixture.wave,
            waveInProgress = fixture.waveInProgress,
            isGameOver = fixture.isGameOver,
            visibleThreats = fixture.visibleThreats,
            remainingThreatsInWave = fixture.remainingThreatsInWave,
            effectiveRangeMeters = fixture.effectiveRangeMeters,
            effectiveFuseMeters = fixture.effectiveFuseMeters,
            doctrineLabel = fixture.doctrineLabel,
            doctrineSummary = fixture.doctrineSummary,
        )

    private data class HudSnapshotFixture(
        val cityIntegrity: Float = 100f,
        val score: Int = 900,
        val credits: Int = 10200,
        val wave: Int = 3,
        val waveInProgress: Boolean = false,
        val isGameOver: Boolean = false,
        val visibleThreats: Int = 0,
        val remainingThreatsInWave: Int = 0,
        val effectiveRangeMeters: Float = 2825f,
        val effectiveFuseMeters: Float = 82f,
        val doctrineLabel: String = DefenseDoctrine.SHIELD_WALL.label,
        val doctrineSummary: String = DefenseDoctrine.SHIELD_WALL.summary,
    )
}
