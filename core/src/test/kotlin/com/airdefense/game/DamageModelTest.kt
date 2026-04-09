package com.airdefense.game

import com.badlogic.gdx.math.Vector3
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DamageModelTest {
    @Test
    fun `damage is high near impact and zero outside radius`() {
        val centerDamage = DamageModel.computeBuildingDamage(
            buildingPosition = Vector3(0f, 0f, 0f),
            buildingWidth = 60f,
            buildingDepth = 60f,
            impactPosition = Vector3(0f, 0f, 0f),
            blastRadius = 85f,
            hostile = true
        )
        val distantDamage = DamageModel.computeBuildingDamage(
            buildingPosition = Vector3(400f, 0f, 400f),
            buildingWidth = 60f,
            buildingDepth = 60f,
            impactPosition = Vector3(0f, 0f, 0f),
            blastRadius = 85f,
            hostile = true
        )

        assertTrue(centerDamage > 70f)
        assertEquals(0f, distantDamage)
    }

    @Test
    fun `city integrity loss reflects hostile impacts and collapses`() {
        assertEquals(12f, DamageModel.cityIntegrityLoss(hostile = true, destroyedBuilding = false))
        assertEquals(18f, DamageModel.cityIntegrityLoss(hostile = true, destroyedBuilding = true))
        assertEquals(6f, DamageModel.cityIntegrityLoss(hostile = false, destroyedBuilding = true))
    }
}
