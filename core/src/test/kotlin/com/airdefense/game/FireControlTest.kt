package com.airdefense.game

import com.badlogic.gdx.math.Vector3
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FireControlTest {
    @Test
    fun `select next threat prefers the closest inbound target in range`() {
        val threats =
            listOf(
                ThreatSnapshot("rear", Vector3(0f, 200f, -900f)),
                ThreatSnapshot("front", Vector3(20f, 180f, -250f)),
                ThreatSnapshot("flank", Vector3(400f, 120f, -260f)),
            )

        val selected =
            FireControl.selectNextThreat(
                threats = threats,
                engagementRange = 1500f,
                assignedThreatIds = emptySet(),
            )

        assertEquals("front", selected)
    }

    @Test
    fun `select next threat skips already assigned tracks and out of range threats`() {
        val threats =
            listOf(
                ThreatSnapshot("assigned", Vector3(0f, 180f, -200f)),
                ThreatSnapshot("out_of_range", Vector3(0f, 180f, -200f).scl(20f)),
                ThreatSnapshot("available", Vector3(50f, 160f, -320f)),
            )

        val selected =
            FireControl.selectNextThreat(
                threats = threats,
                engagementRange = 1500f,
                assignedThreatIds = setOf("assigned"),
            )

        assertEquals("available", selected)
    }

    @Test
    fun `select next threat returns null when no target is available`() {
        val selected =
            FireControl.selectNextThreat(
                threats = emptyList(),
                engagementRange = 1500f,
                assignedThreatIds = emptySet(),
            )

        assertNull(selected)
    }

    @Test
    fun `select next threat breaks ties by altitude then centerline`() {
        val threats =
            listOf(
                ThreatSnapshot("off_axis", Vector3(260f, 220f, -280f)),
                ThreatSnapshot("centerline", Vector3(40f, 220f, -280f)),
            )

        val selected =
            FireControl.selectNextThreat(
                threats = threats,
                engagementRange = 1500f,
                assignedThreatIds = emptySet(),
            )

        assertEquals("centerline", selected)
    }

    @Test
    fun `adaptive doctrine refuses early recommit on already assigned track`() {
        val threats =
            listOf(
                ThreatSnapshot(
                    id = "assigned_far",
                    position = Vector3(0f, 220f, -920f),
                    velocity = Vector3(0f, -18f, 180f),
                    targetPosition = Vector3(0f, 0f, 0f),
                ),
                ThreatSnapshot(
                    id = "available_close",
                    position = Vector3(70f, 160f, -320f),
                    velocity = Vector3(0f, -12f, 200f),
                    targetPosition = Vector3(0f, 0f, 0f),
                ),
            )

        val selected =
            FireControl.selectNextThreat(
                threats = threats,
                engagementRange = 1800f,
                assignmentCounts = mapOf("assigned_far" to 1),
                doctrine = DefenseDoctrine.ADAPTIVE,
            )

        assertEquals("available_close", selected)
    }

    @Test
    fun `shield wall doctrine recommits on terminal tracks`() {
        val threats =
            listOf(
                ThreatSnapshot(
                    id = "assigned_terminal",
                    position = Vector3(20f, 150f, -120f),
                    velocity = Vector3(0f, -22f, 210f),
                    targetPosition = Vector3(0f, 0f, 0f),
                ),
                ThreatSnapshot(
                    id = "available_far",
                    position = Vector3(50f, 180f, -560f),
                    velocity = Vector3(0f, -15f, 180f),
                    targetPosition = Vector3(0f, 0f, 0f),
                ),
            )

        val selected =
            FireControl.selectNextThreat(
                threats = threats,
                engagementRange = 1800f,
                assignmentCounts = mapOf("assigned_terminal" to 1),
                doctrine = DefenseDoctrine.SHIELD_WALL,
            )

        assertEquals("assigned_terminal", selected)
    }
}
