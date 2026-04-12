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

private data class LayoutPlacement(
    val modelName: String,
    val x: Float,
    val z: Float,
    val yaw: Float = 0f,
)

private const val BUILDING_ID_PADDING = 3
private const val GROUND_LEVEL_Y = 0f
private const val RADAR_HORIZONTAL_PADDING_RATIO = 0.08f
private const val RADAR_VERTICAL_PADDING_RATIO = 0.08f
private const val RADAR_MARKER_X_RATIO = 0.5f
private const val RADAR_MARKER_Y_RATIO = 0.1f
private const val RADAR_SWEEP_START_RATIO = 0.08f
private const val RADAR_SWEEP_SPAN_RATIO = 0.84f
private const val RADAR_NORMALIZED_CENTER = 0.5f
private const val RADAR_NORMALIZED_MIN = 0f
private const val RADAR_NORMALIZED_MAX = 1f

private fun worldPoint(
    x: Float,
    y: Float,
    z: Float,
): Vector3 = Vector3(x, y, z)

private fun metrics(
    baseHeight: Float,
    width: Float,
    depth: Float,
): BuildingMetrics =
    BuildingMetrics(
        baseHeight = baseHeight,
        width = width,
        depth = depth,
    )

private fun placement(
    modelName: String,
    x: Float,
    z: Float,
    yaw: Float = 0f,
): LayoutPlacement =
    LayoutPlacement(
        modelName = modelName,
        x = x,
        z = z,
        yaw = yaw,
    )

private val launcherPads =
    listOf(
        worldPoint(x = -160f, y = 5f, z = 260f),
        worldPoint(x = 210f, y = 5f, z = 220f),
    )

private val defenseOriginPoint = worldPoint(x = 25f, y = 0f, z = 240f)
private val radarOriginPoint = worldPoint(x = 24f, y = 5f, z = 120f)

private val defaultBuildingMetrics =
    metrics(
        baseHeight = 140f,
        width = 100f,
        depth = 80f,
    )

private val buildingMetricsByModel =
    mapOf(
        "tower_a" to metrics(baseHeight = 280f, width = 58f, depth = 58f),
        "tower_b" to metrics(baseHeight = 210f, width = 84f, depth = 84f),
        "tower_c" to metrics(baseHeight = 130f, width = 120f, depth = 90f),
        "tower_d" to metrics(baseHeight = 360f, width = 96f, depth = 74f),
        "tower_e" to metrics(baseHeight = 178f, width = 146f, depth = 112f),
        "podium" to metrics(baseHeight = 78f, width = 180f, depth = 120f),
        "hotel" to metrics(baseHeight = 118f, width = 132f, depth = 72f),
        "coastal_slab" to metrics(baseHeight = 96f, width = 228f, depth = 56f),
        "office_slab" to metrics(baseHeight = 152f, width = 168f, depth = 92f),
        "needle_tower" to metrics(baseHeight = 420f, width = 44f, depth = 44f),
        "setback_tower" to metrics(baseHeight = 304f, width = 118f, depth = 92f),
    )

private val waterfrontPlacements =
    listOf(
        placement(modelName = "coastal_slab", x = -1420f, z = -1450f, yaw = 4f),
        placement(modelName = "hotel", x = -980f, z = -1490f, yaw = -4f),
        placement(modelName = "coastal_slab", x = -360f, z = -1430f, yaw = 4f),
        placement(modelName = "hotel", x = 220f, z = -1480f, yaw = -4f),
        placement(modelName = "coastal_slab", x = 860f, z = -1420f, yaw = 4f),
        placement(modelName = "hotel", x = 1500f, z = -1490f, yaw = -4f),
        placement(modelName = "coastal_slab", x = 2180f, z = -1440f, yaw = 4f),
        placement(modelName = "hotel", x = 2860f, z = -1490f, yaw = -4f),
        placement(modelName = "coastal_slab", x = 3520f, z = -1430f, yaw = 4f),
    )

private val skylinePlacements =
    listOf(
        placement(modelName = "setback_tower", x = -540f, z = -1040f, yaw = 8f),
        placement(modelName = "tower_d", x = -40f, z = -960f, yaw = -7f),
        placement(modelName = "needle_tower", x = 360f, z = -900f, yaw = -7f),
        placement(modelName = "tower_a", x = 760f, z = -980f, yaw = 8f),
        placement(modelName = "tower_e", x = 1180f, z = -840f, yaw = -7f),
        placement(modelName = "setback_tower", x = 1660f, z = -940f, yaw = -7f),
        placement(modelName = "tower_d", x = 2140f, z = -780f, yaw = 8f),
        placement(modelName = "needle_tower", x = 2520f, z = -960f, yaw = -7f),
        placement(modelName = "tower_a", x = 2920f, z = -840f, yaw = -7f),
        placement(modelName = "setback_tower", x = 3400f, z = -930f, yaw = 8f),
    )

private val innerCityPlacements =
    listOf(
        placement(modelName = "office_slab", x = -1020f, z = -620f, yaw = -9f),
        placement(modelName = "tower_b", x = -440f, z = -480f, yaw = 7f),
        placement(modelName = "tower_c", x = 100f, z = -320f, yaw = -9f),
        placement(modelName = "office_slab", x = 760f, z = -220f, yaw = 7f),
        placement(modelName = "tower_b", x = 1420f, z = -120f, yaw = -9f),
        placement(modelName = "tower_c", x = 2040f, z = -60f, yaw = 7f),
        placement(modelName = "office_slab", x = 2700f, z = -180f, yaw = -9f),
        placement(modelName = "tower_b", x = 3340f, z = -240f, yaw = 7f),
        placement(modelName = "tower_c", x = 480f, z = 220f, yaw = -9f),
        placement(modelName = "tower_b", x = 1220f, z = 320f, yaw = 7f),
        placement(modelName = "tower_e", x = 1960f, z = 240f, yaw = -9f),
        placement(modelName = "office_slab", x = 2680f, z = 120f, yaw = 7f),
    )

private val promenadePlacements =
    listOf(
        placement(modelName = "podium", x = -1040f, z = 420f, yaw = -5f),
        placement(modelName = "hotel", x = -1380f, z = 580f, yaw = 5f),
        placement(modelName = "podium", x = 3440f, z = 360f, yaw = -5f),
        placement(modelName = "hotel", x = 3900f, z = 560f, yaw = 5f),
    )

private val inlandPlacements =
    listOf(
        placement(modelName = "tower_c", x = -180f, z = 820f, yaw = 5f),
        placement(modelName = "tower_b", x = 620f, z = 980f, yaw = -7f),
        placement(modelName = "podium", x = 1380f, z = 860f, yaw = 5f),
        placement(modelName = "tower_c", x = 2120f, z = 760f, yaw = -7f),
        placement(modelName = "tower_b", x = 2880f, z = 820f, yaw = 5f),
        placement(modelName = "tower_c", x = 3620f, z = 720f, yaw = -7f),
    )

private val layoutPlacements =
    waterfrontPlacements +
        skylinePlacements +
        innerCityPlacements +
        promenadePlacements +
        inlandPlacements

object BattleWorldLayout {
    const val WATERFRONT_SAFE_Z = -1480f
    const val RADAR_HALF_WIDTH = 4500f
    const val RADAR_NEAR_Z = 720f
    const val RADAR_FAR_Z = -4500f

    fun launcherPositions(): List<Vector3> = launcherPads.map(Vector3::cpy)

    fun defenseOrigin(): Vector3 = defenseOriginPoint.cpy()

    fun radarPosition(): Vector3 = radarOriginPoint.cpy()

    fun buildingMetrics(modelName: String) = buildingMetricsByModel[modelName] ?: defaultBuildingMetrics

    fun buildingDefinitions(): List<BattleBuildingDefinition> =
        layoutPlacements
            .mapIndexed { index, placement ->
                placement.toDefinition(index + 1)
            }

    private fun LayoutPlacement.toDefinition(order: Int): BattleBuildingDefinition {
        val metrics = buildingMetrics(modelName)
        val correctedZ = maxOf(z, WATERFRONT_SAFE_Z)
        return BattleBuildingDefinition(
            id = buildingId(order),
            modelName = modelName,
            position = worldPoint(x = x, y = GROUND_LEVEL_Y, z = correctedZ),
            yaw = yaw,
            metrics = metrics,
        )
    }

    private fun buildingId(order: Int): String = "B-${order.toString().padStart(BUILDING_ID_PADDING, '0')}"
}

object RadarProjection {
    fun project(
        position: Vector3,
        radarX: Float,
        radarY: Float,
        radarWidth: Float,
        radarHeight: Float,
    ): RadarPlotPoint {
        val horizontalPadding = radarWidth * RADAR_HORIZONTAL_PADDING_RATIO
        val verticalPadding = radarHeight * RADAR_VERTICAL_PADDING_RATIO
        val normalizedX =
            (
                (position.x / BattleWorldLayout.RADAR_HALF_WIDTH) * RADAR_NORMALIZED_CENTER +
                    RADAR_NORMALIZED_CENTER
            ).coerceIn(
                RADAR_NORMALIZED_MIN,
                RADAR_NORMALIZED_MAX,
            )
        val normalizedDepth =
            (
                (BattleWorldLayout.RADAR_NEAR_Z - position.z) /
                    (BattleWorldLayout.RADAR_NEAR_Z - BattleWorldLayout.RADAR_FAR_Z)
            ).coerceIn(
                RADAR_NORMALIZED_MIN,
                RADAR_NORMALIZED_MAX,
            )

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
        val markerX = radarX + radarWidth * RADAR_MARKER_X_RATIO
        val markerY = radarY + radarHeight * RADAR_MARKER_Y_RATIO
        return Vector2(markerX, markerY)
    }

    fun sweepY(
        radarY: Float,
        radarHeight: Float,
        phase: Float,
    ): Float {
        val clampedPhase = phase.coerceIn(0f, 1f)
        return radarY + radarHeight * RADAR_SWEEP_START_RATIO + clampedPhase * radarHeight * RADAR_SWEEP_SPAN_RATIO
    }
}
