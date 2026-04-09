package com.airdefense.game

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BattleBalanceTest {
    @Test
    fun `wave threat count scales linearly`() {
        assertEquals(11, BattleBalance.threatsForWave(1))
        assertEquals(27, BattleBalance.threatsForWave(5))
    }

    @Test
    fun `spawn interval never drops below floor`() {
        assertEquals(2.15f, BattleBalance.spawnIntervalForWave(1))
        assertEquals(0.65f, BattleBalance.spawnIntervalForWave(40))
    }

    @Test
    fun `threat speed range expands with wave`() {
        val early = BattleBalance.threatSpeedRangeForWave(1)
        val late = BattleBalance.threatSpeedRangeForWave(8)
        assertTrue(late.start > early.start)
        assertTrue(late.endInclusive > early.endInclusive)
    }
}
