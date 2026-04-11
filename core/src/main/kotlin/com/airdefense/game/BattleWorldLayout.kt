package com.airdefense.game

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3

data class BuildingMetrics(
    val baseHeight: Float,
    val width: Float,
    val depth: Float,
)

data class BattleBuildingDefinition(
    val id: String,
    val modelName: String,
    val position: Vector3,
    val yaw: Float,
    val metrics: BuildingMetrics,
)

data class RadarPlotPoint(
    val x: Float,
    val y: Float,
)

object BattleWorldLayout {
    const val WATERFRONT_SAFE_Z = -1480f
    const val RADAR_HALF_WIDTH = 4500f
    const val RADAR_NEAR_Z = 720f
    const val RADAR_FAR_Z = -4500f

    private val launcherPads =
        listOf(
            Vector3(-160f, 5f, 260f),
            Vector3(210f, 5f, 220f),
        )

    val defenseOrigin: Vector3 = Vector3(25f, 0f, 240f)

    fun launcherPositions(): List<Vector3> = launcherPads.map { it.cpy() }

    fun radarPosition(): Vector3 = Vector3(24f, 5f, 120f)

    fun buildingMetrics(modelName: String): BuildingMetrics =
        when (modelName) {
            "tower_a" -> BuildingMetrics(280f, 58f, 58f)
            "tower_b" -> BuildingMetrics(210f, 84f, 84f)
            "tower_c" -> BuildingMetrics(130f, 120f, 90f)
            "tower_d" -> BuildingMetrics(360f, 96f, 74f)
            "tower_e" -> BuildingMetrics(178f, 146f, 112f)
            "podium" -> BuildingMetrics(78f, 180f, 120f)
            "hotel" -> BuildingMetrics(118f, 132f, 72f)
            "coastal_slab" -> BuildingMetrics(96f, 228f, 56f)
            "office_slab" -> BuildingMetrics(152f, 168f, 92f)
            "needle_tower" -> BuildingMetrics(420f, 44f, 44f)
            "setback_tower" -> BuildingMetrics(304f, 118f, 92f)
            else -> BuildingMetrics(140f, 100f, 80f)
        }

    fun buildingDefinitions(): List<BattleBuildingDefinition> {
        val definitions = mutableListOf<BattleBuildingDefinition>()
        var nextId = 1

        fun addBuilding(
            modelName: String,
            x: Float,
            z: Float,
            yaw: Float = 0f,
        ) {
            val metrics = buildingMetrics(modelName)
            val correctedZ = maxOf(z, WATERFRONT_SAFE_Z)
            definitions +=
                BattleBuildingDefinition(
                    id = "B-${nextId++.toString().padStart(3, '0')}",
                    modelName = modelName,
                    position = Vector3(x, 0f, correctedZ),
                    yaw = yaw,
                    metrics = metrics,
                )
        }

        listOf(
            Triple("coastal_slab", -1420f, -1450f),
            Triple("hotel", -980f, -1490f),
            Triple("coastal_slab", -360f, -1430f),
            Triple("hotel", 220f, -1480f),
            Triple("coastal_slab", 860f, -1420f),
            Triple("hotel", 1500f, -1490f),
            Triple("coastal_slab", 2180f, -1440f),
            Triple("hotel", 2860f, -1490f),
            Triple("coastal_slab", 3520f, -1430f),
        ).forEachIndexed { index, (model, x, z) ->
            addBuilding(model, x, z, yaw = if (index % 2 == 0) 4f else -4f)
        }

        listOf(
            Triple("setback_tower", -540f, -1040f),
            Triple("tower_d", -40f, -960f),
            Triple("needle_tower", 360f, -900f),
            Triple("tower_a", 760f, -980f),
            Triple("tower_e", 1180f, -840f),
            Triple("setback_tower", 1660f, -940f),
            Triple("tower_d", 2140f, -780f),
            Triple("needle_tower", 2520f, -960f),
            Triple("tower_a", 2920f, -840f),
            Triple("setback_tower", 3400f, -930f),
        ).forEachIndexed { index, (model, x, z) ->
            addBuilding(model, x, z, yaw = if (index % 3 == 0) 8f else -7f)
        }

        listOf(
            Triple("office_slab", -1020f, -620f),
            Triple("tower_b", -440f, -480f),
            Triple("tower_c", 100f, -320f),
            Triple("office_slab", 760f, -220f),
            Triple("tower_b", 1420f, -120f),
            Triple("tower_c", 2040f, -60f),
            Triple("office_slab", 2700f, -180f),
            Triple("tower_b", 3340f, -240f),
            Triple("tower_c", 480f, 220f),
            Triple("tower_b", 1220f, 320f),
            Triple("tower_e", 1960f, 240f),
            Triple("office_slab", 2680f, 120f),
        ).forEachIndexed { index, (model, x, z) ->
            addBuilding(model, x, z, yaw = if (index % 2 == 0) -9f else 7f)
        }

        listOf(
            Triple("podium", -1040f, 420f),
            Triple("hotel", -1380f, 580f),
            Triple("podium", 3440f, 360f),
            Triple("hotel", 3900f, 560f),
        ).forEachIndexed { index, (model, x, z) ->
            addBuilding(model, x, z, yaw = if (index % 2 == 0) -5f else 5f)
        }

        listOf(
            Triple("tower_c", -180f, 820f),
            Triple("tower_b", 620f, 980f),
            Triple("podium", 1380f, 860f),
            Triple("tower_c", 2120f, 760f),
            Triple("tower_b", 2880f, 820f),
            Triple("tower_c", 3620f, 720f),
        ).forEachIndexed { index, (model, x, z) ->
            addBuilding(model, x, z, yaw = if (index % 2 == 0) 5f else -7f)
        }

        return definitions
    }
}

object RadarProjection {
    fun project(
        position: Vector3,
        radarX: Float,
        radarY: Float,
        radarWidth: Float,
        radarHeight: Float,
    ): RadarPlotPoint {
        val horizontalPadding = radarWidth * 0.08f
        val verticalPadding = radarHeight * 0.08f
        val normalizedX = ((position.x / BattleWorldLayout.RADAR_HALF_WIDTH) * 0.5f + 0.5f).coerceIn(0f, 1f)
        val normalizedDepth =
            (
                (BattleWorldLayout.RADAR_NEAR_Z - position.z) /
                    (BattleWorldLayout.RADAR_NEAR_Z - BattleWorldLayout.RADAR_FAR_Z)
            ).coerceIn(0f, 1f)

        return RadarPlotPoint(
            x = radarX + horizontalPadding + normalizedX * (radarWidth - horizontalPadding * 2f),
            y = radarY + verticalPadding + normalizedDepth * (radarHeight - verticalPadding * 2f),
        )
    }

    fun launcherMarker(
        radarX: Float,
        radarY: Float,
        radarWidth: Float,
        radarHeight: Float,
    ): Vector2 {
        val markerY = radarY + radarHeight * 0.1f
        return Vector2(radarX + radarWidth * 0.5f, markerY)
    }

    fun sweepY(
        radarY: Float,
        radarHeight: Float,
        phase: Float,
    ): Float {
        val clampedPhase = phase.coerceIn(0f, 1f)
        return radarY + radarHeight * 0.08f + clampedPhase * radarHeight * 0.84f
    }
}
