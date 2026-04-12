package com.airdefense.game

import com.badlogic.gdx.math.Vector3
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class BattleBuildingVisualControllerTest {
    @Test
    fun `damage update marks collapsed buildings with collapse debris plan`() {
        val update =
            battleBuildingDamageVisualUpdate(
                BattleBuildingDamageInput(
                    previousIntegrity = 20f,
                    newIntegrity = 0f,
                    currentCollapseVelocity = 10f,
                    currentLeanTarget = 1f,
                    position = Vector3(100f, 0f, -50f),
                    baseHeight = 200f,
                    epicenter = Vector3.Zero,
                ),
            )

        assertNotNull(update)
        val collapsedHeight = update.visibleHeightOverride
        val collapseDebris = update.debrisPlan
        assertNotNull(collapsedHeight)
        assertNotNull(collapseDebris)
        assertEquals(0f, update.integrity, 0.0001f)
        assertEquals(60f, update.collapseVelocity, 0.0001f)
        assertEquals(2f, update.leanTarget, 0.0001f)
        assertEquals(24f, collapsedHeight, 0.0001f)
        assertEquals(0.28f, update.tint.r, 0.0001f)
        assertEquals(BattleBuildingDebrisProfile.DESTROYED, collapseDebris.profile)
        assertEquals(70f, collapseDebris.position.y, 0.0001f)
    }

    @Test
    fun `damage update keeps partial hit in damaged state`() {
        val update =
            battleBuildingDamageVisualUpdate(
                BattleBuildingDamageInput(
                    previousIntegrity = 100f,
                    newIntegrity = 80f,
                    currentCollapseVelocity = 2f,
                    currentLeanTarget = 0f,
                    position = Vector3(-50f, 0f, 0f),
                    baseHeight = 100f,
                    epicenter = Vector3(100f, 0f, 0f),
                ),
            )

        assertNotNull(update)
        val damageDebris = update.debrisPlan
        assertNotNull(damageDebris)
        assertEquals(80f, update.integrity, 0.0001f)
        assertEquals(2.05f, update.collapseVelocity, 0.0001f)
        assertEquals(-1f, update.leanTarget, 0.0001f)
        assertNull(update.visibleHeightOverride)
        assertEquals(0.85142857f, update.tint.r, 0.0001f)
        assertEquals(BattleBuildingDebrisProfile.DAMAGED, damageDebris.profile)
        assertEquals(45f, damageDebris.position.y, 0.0001f)
    }

    @Test
    fun `animation state clamps destroyed buildings to minimum visible height`() {
        val state =
            battleBuildingAnimationState(
                BattleBuildingAnimationInput(
                    baseHeight = 200f,
                    integrity = 0f,
                    visibleHeight = 30f,
                    collapseVelocity = 50f,
                    lean = 0f,
                    leanTarget = 10f,
                    dt = 0.5f,
                ),
            )

        assertEquals(16f, state.visibleHeight, 0.0001f)
        assertEquals(8.5f, state.lean, 0.0001f)
    }

    @Test
    fun `animation state eases damaged buildings toward their target silhouette`() {
        val state =
            battleBuildingAnimationState(
                BattleBuildingAnimationInput(
                    baseHeight = 200f,
                    integrity = 80f,
                    visibleHeight = 200f,
                    collapseVelocity = 0f,
                    lean = 2f,
                    leanTarget = 6f,
                    dt = 0.2f,
                ),
            )

        assertEquals(184.4f, state.visibleHeight, 0.0001f)
        assertEquals(3.36f, state.lean, 0.0001f)
    }
}
