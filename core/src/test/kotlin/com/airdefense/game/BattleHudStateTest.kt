package com.airdefense.game

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BattleHudStateTest {
    @Test
    fun `wave state shows live hostile count during active wave`() {
        val snapshot = snapshot(waveInProgress = true, visibleThreats = 6, remainingThreatsInWave = 4)

        assertEquals("WAVE 3 LIVE / 10 HOSTILES", snapshot.waveStateText())
        assertEquals("WAVE 3 ACTIVE", snapshot.waveButtonState().text)
        assertTrue(snapshot.waveButtonState().disabled)
    }

    @Test
    fun `wave state shows ready when no wave is active`() {
        val snapshot = snapshot(waveInProgress = false, visibleThreats = 0, remainingThreatsInWave = 11)

        assertEquals("WAVE 3 READY", snapshot.waveStateText())
        assertEquals("START NEXT WAVE", snapshot.waveButtonState().text)
        assertFalse(snapshot.waveButtonState().disabled)
    }

    @Test
    fun `game over state dominates wave presentation`() {
        val snapshot = snapshot(isGameOver = true, waveInProgress = true, visibleThreats = 9, remainingThreatsInWave = 2)

        assertEquals("STATUS LOST", snapshot.waveStateText())
        assertEquals("DEFENSE FAILED", snapshot.waveButtonState().text)
        assertTrue(snapshot.waveButtonState().disabled)
    }

    @Test
    fun `summary text includes core command metrics`() {
        val snapshot = snapshot(cityIntegrity = 87.6f, score = 2400, credits = 10820, visibleThreats = 3, remainingThreatsInWave = 1)

        assertEquals("2825 M", snapshot.rangeText())
        assertEquals("82 M", snapshot.fuseText())
        assertEquals(
            "CITY 87%   SCORE 2400   CR 10820   WAVE 3 READY",
            snapshot.summaryText(),
        )
    }

    private fun snapshot(
        cityIntegrity: Float = 100f,
        score: Int = 900,
        credits: Int = 10200,
        wave: Int = 3,
        waveInProgress: Boolean = false,
        isGameOver: Boolean = false,
        visibleThreats: Int = 0,
        remainingThreatsInWave: Int = 0,
        effectiveRangeMeters: Float = 2825f,
        effectiveFuseMeters: Float = 82f,
        doctrineLabel: String = DefenseDoctrine.SHIELD_WALL.label,
        doctrineSummary: String = DefenseDoctrine.SHIELD_WALL.summary,
    ): BattleHudSnapshot =
        BattleHudSnapshot(
            cityIntegrity = cityIntegrity,
            score = score,
            credits = credits,
            wave = wave,
            waveInProgress = waveInProgress,
            isGameOver = isGameOver,
            visibleThreats = visibleThreats,
            remainingThreatsInWave = remainingThreatsInWave,
            effectiveRangeMeters = effectiveRangeMeters,
            effectiveFuseMeters = effectiveFuseMeters,
            doctrineLabel = doctrineLabel,
            doctrineSummary = doctrineSummary,
        )
}
