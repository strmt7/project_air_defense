package com.airdefense.game

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BattleBalanceTest {
    @Test
    fun `wave threat count scales linearly`() {
        assertEquals(9, BattleBalance.threatsForWave(1))
        assertEquals(21, BattleBalance.threatsForWave(5))
    }

    @Test
    fun `spawn interval never drops below floor`() {
        assertEquals(2.3200002f, BattleBalance.spawnIntervalForWave(1))
        assertEquals(0.9f, BattleBalance.spawnIntervalForWave(40))
    }

    @Test
    fun `threat speed range expands with wave`() {
        val early = BattleBalance.threatSpeedRangeForWave(1)
        val late = BattleBalance.threatSpeedRangeForWave(8)
        assertTrue(late.start > early.start)
        assertTrue(late.endInclusive > early.endInclusive)
    }
}
