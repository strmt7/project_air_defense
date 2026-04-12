package com.airdefense.game

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BattleEffectsControllerTest {
    @Test
    fun `effect budget scale drops under heavy scene pressure`() {
        val load = BattleEffectLoad(threatCount = 16, interceptorCount = 6, effectCount = 40)

        val scale = battleEffectBudgetScale(load, baseScale = 0.9f)

        assertEquals(0.558f, scale, 0.0001f)
    }

    @Test
    fun `adjusted effect count stays within configured bounds`() {
        val load = BattleEffectLoad(threatCount = 10, interceptorCount = 8, effectCount = 24)

        val count =
            battleAdjustedEffectCount(
                baseCount = 12,
                minimum = 2,
                load = load,
                baseScale = 0.85f,
            )

        assertEquals(7, count)
    }

    @Test
    fun `adjusted effect count respects minimum under extreme pressure`() {
        val load = BattleEffectLoad(threatCount = 20, interceptorCount = 20, effectCount = 140)

        val count =
            battleAdjustedEffectCount(
                baseCount = 5,
                minimum = 2,
                load = load,
                baseScale = 0.62f,
            )

        assertEquals(2, count)
    }

    @Test
    fun `trail stride increases when scene pressure rises`() {
        val lowLoad = BattleEffectLoad(threatCount = 2, interceptorCount = 1, effectCount = 4)
        val mediumLoad = BattleEffectLoad(threatCount = 9, interceptorCount = 4, effectCount = 26)
        val heavyLoad = BattleEffectLoad(threatCount = 16, interceptorCount = 9, effectCount = 50)

        val baseStride = 2

        assertEquals(baseStride, battleAdjustedTrailStride(baseStride, lowLoad))
        assertEquals(baseStride + 1, battleAdjustedTrailStride(baseStride, mediumLoad))
        assertEquals(baseStride + 2, battleAdjustedTrailStride(baseStride, heavyLoad))
    }

    @Test
    fun `trail stride never drops below one`() {
        val load = BattleEffectLoad(threatCount = 0, interceptorCount = 0, effectCount = 0)

        assertTrue(battleAdjustedTrailStride(baseStride = 0, load = load) >= 1)
    }
}
