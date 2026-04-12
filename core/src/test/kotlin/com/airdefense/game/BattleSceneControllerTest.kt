package com.airdefense.game

import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BattleSceneControllerTest {
    @Test
    fun `city pulse rises with bounded threat pressure and scene sway`() {
        val state =
            BattleSceneState(
                threatCount = 4,
                remainingThreatsInWave = 2,
                liveTimeSeconds = 10f,
            )

        val pulse = battleCityPulse(state)

        assertEquals(0.86462086f, pulse, 0.0001f)
        assertEquals(1180f + sin(10f * 0.12f) * 90f, battleCityGlowX(10f), 0.0001f)
    }

    @Test
    fun `launcher light state reflects pulse decay and brighter launch flash`() {
        val pulse = battleLauncherPulse(currentPulse = 1f, dt = 0.1f)
        val lightState = battleLauncherLightState(pulse = pulse, lightIntensityScale = 1f)
        val idleLightState = battleLauncherLightState(pulse = 0f, lightIntensityScale = 1f)

        assertEquals(0.77f, pulse, 0.0001f)
        assertEquals(920.6f, lightState.intensity, 0.0001f)
        assertEquals(320f, idleLightState.intensity, 0.0001f)
        assertTrue(lightState.color.r > idleLightState.color.r)
        assertTrue(lightState.color.b < idleLightState.color.b)
    }

    @Test
    fun `shake amount clamps to zero after the reference window`() {
        assertEquals(6.4f, battleShakeAmount(shakeIntensity = 8f, remainingShakeTime = 0.4f), 0.0001f)
        assertEquals(0f, battleShakeAmount(shakeIntensity = 8f, remainingShakeTime = -0.1f), 0.0001f)
    }
}
