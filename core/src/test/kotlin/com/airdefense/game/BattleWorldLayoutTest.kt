package com.airdefense.game

import com.badlogic.gdx.math.Vector3
import kotlin.test.Test
import kotlin.test.assertTrue

class BattleWorldLayoutTest {
    @Test
    fun `waterfront buildings stay inland of the shoreline safety band`() {
        BattleWorldLayout.buildingDefinitions().forEach { definition ->
            assertTrue(
                definition.position.z >= BattleWorldLayout.WATERFRONT_SAFE_Z,
                "building ${definition.id} sits too far into the waterfront at z=${definition.position.z}"
            )
        }
    }

    @Test
    fun `radar projection matches incoming direction from the battle camera`() {
        val farThreat = RadarProjection.project(Vector3(0f, 0f, -4200f), 0f, 0f, 200f, 200f)
        val nearThreat = RadarProjection.project(Vector3(0f, 0f, 400f), 0f, 0f, 200f, 200f)
        val leftThreat = RadarProjection.project(Vector3(-3200f, 0f, -2000f), 0f, 0f, 200f, 200f)
        val rightThreat = RadarProjection.project(Vector3(3200f, 0f, -2000f), 0f, 0f, 200f, 200f)

        assertTrue(farThreat.y > nearThreat.y, "far incoming threats should plot higher on the radar")
        assertTrue(leftThreat.x < rightThreat.x, "left/right world positions should remain left/right on the radar")
    }
}
