package com.airdefense.game

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Array
import kotlin.math.max
import kotlin.math.min

private const val BUILDING_COLLAPSE_VELOCITY_FACTOR = 0.0025f
private const val BUILDING_LEAN_DAMAGE_FACTOR = 0.05f
private const val BUILDING_MAX_LEAN = 16f
private const val BUILDING_TINT_BASE = 0.28f
private const val BUILDING_TINT_BLUE_BIAS = 0.06f
private const val BUILDING_TINT_INTEGRITY_DIVISOR = 140f
private const val BUILDING_DESTROYED_HEIGHT_RATIO = 0.12f
private const val BUILDING_DESTROYED_MIN_COLLAPSE_VELOCITY = 60f
private const val BUILDING_DESTROYED_DEBRIS_HEIGHT_RATIO = 0.35f
private const val BUILDING_DAMAGED_DEBRIS_HEIGHT_RATIO = 0.45f
private const val BUILDING_MIN_VISIBLE_HEIGHT_RATIO = 0.08f
private const val BUILDING_DAMAGED_HEIGHT_BASE_RATIO = 0.35f
private const val BUILDING_DAMAGED_HEIGHT_RANGE_RATIO = 0.65f
private const val BUILDING_HEIGHT_RESPONSE = 3f
private const val BUILDING_LEAN_RESPONSE = 1.7f
private const val BUILDING_MIN_HEIGHT_SCALE = 0.05f
private const val BUILDING_CENTER_HEIGHT_RATIO = 0.5f
private const val BUILDING_FULL_INTEGRITY = 100f
private const val OPAQUE_ALPHA = 1f

private const val BUILDING_DESTROYED_DEBRIS_COUNT = 28
private const val BUILDING_DAMAGED_DEBRIS_COUNT = 6

private val BUILDING_DESTROYED_DEBRIS_COLOR = Color.valueOf("404047FF")
private val BUILDING_DAMAGED_DEBRIS_COLOR = Color.valueOf("4D4D57FF")

private fun battleAxisSign(value: Float): Float =
    when {
        value > 0f -> 1f
        value < 0f -> -1f
        else -> 0f
    }

internal enum class BattleBuildingDebrisProfile {
    DESTROYED,
    DAMAGED,
}

internal data class BattleBuildingDebrisPlan(
    val profile: BattleBuildingDebrisProfile,
    val position: Vector3,
)

internal data class BattleBuildingDamageVisualUpdate(
    val integrity: Float,
    val collapseVelocity: Float,
    val leanTarget: Float,
    val tint: Color,
    val visibleHeightOverride: Float? = null,
    val debrisPlan: BattleBuildingDebrisPlan? = null,
)

internal data class BattleBuildingAnimationState(
    val visibleHeight: Float,
    val lean: Float,
)

internal data class BattleBuildingDamageInput(
    val previousIntegrity: Float,
    val newIntegrity: Float,
    val currentCollapseVelocity: Float,
    val currentLeanTarget: Float,
    val position: Vector3,
    val baseHeight: Float,
    val epicenter: Vector3,
)

internal data class BattleBuildingAnimationInput(
    val baseHeight: Float,
    val integrity: Float,
    val visibleHeight: Float,
    val collapseVelocity: Float,
    val lean: Float,
    val leanTarget: Float,
    val dt: Float,
)

internal fun battleBuildingDamageVisualUpdate(input: BattleBuildingDamageInput): BattleBuildingDamageVisualUpdate? {
    val damage = (input.previousIntegrity - input.newIntegrity).coerceAtLeast(0f)
    val collapseVelocity = input.currentCollapseVelocity + damage * BUILDING_COLLAPSE_VELOCITY_FACTOR
    val leanTarget =
        MathUtils.clamp(
            input.currentLeanTarget +
                battleAxisSign(input.position.x - input.epicenter.x) * damage * BUILDING_LEAN_DAMAGE_FACTOR,
            -BUILDING_MAX_LEAN,
            BUILDING_MAX_LEAN,
        )
    val tint = BUILDING_TINT_BASE + input.newIntegrity / BUILDING_TINT_INTEGRITY_DIVISOR
    val tintColor = Color(tint, tint, tint + BUILDING_TINT_BLUE_BIAS, OPAQUE_ALPHA)

    return when {
        input.previousIntegrity <= 0f && input.newIntegrity <= 0f -> {
            null
        }

        damage <= 0f -> {
            null
        }

        input.previousIntegrity > 0f && input.newIntegrity <= 0f -> {
            BattleBuildingDamageVisualUpdate(
                integrity = input.newIntegrity,
                collapseVelocity = max(collapseVelocity, BUILDING_DESTROYED_MIN_COLLAPSE_VELOCITY),
                leanTarget = leanTarget,
                tint = tintColor,
                visibleHeightOverride = input.baseHeight * BUILDING_DESTROYED_HEIGHT_RATIO,
                debrisPlan =
                    BattleBuildingDebrisPlan(
                        profile = BattleBuildingDebrisProfile.DESTROYED,
                        position =
                            input.position.cpy().add(
                                0f,
                                input.baseHeight * BUILDING_DESTROYED_DEBRIS_HEIGHT_RATIO,
                                0f,
                            ),
                    ),
            )
        }

        else -> {
            BattleBuildingDamageVisualUpdate(
                integrity = input.newIntegrity,
                collapseVelocity = collapseVelocity,
                leanTarget = leanTarget,
                tint = tintColor,
                debrisPlan =
                    BattleBuildingDebrisPlan(
                        profile = BattleBuildingDebrisProfile.DAMAGED,
                        position =
                            input.position.cpy().add(
                                0f,
                                input.baseHeight * BUILDING_DAMAGED_DEBRIS_HEIGHT_RATIO,
                                0f,
                            ),
                    ),
            )
        }
    }
}

internal fun battleBuildingAnimationState(input: BattleBuildingAnimationInput): BattleBuildingAnimationState {
    val nextVisibleHeight =
        when {
            input.integrity <= 0f -> {
                max(
                    input.baseHeight * BUILDING_MIN_VISIBLE_HEIGHT_RATIO,
                    input.visibleHeight - input.collapseVelocity * input.dt,
                )
            }

            input.integrity < BUILDING_FULL_INTEGRITY -> {
                val targetHeight =
                    input.baseHeight *
                        (
                            BUILDING_DAMAGED_HEIGHT_BASE_RATIO +
                                input.integrity / BUILDING_FULL_INTEGRITY * BUILDING_DAMAGED_HEIGHT_RANGE_RATIO
                        )
                input.visibleHeight +
                    (targetHeight - input.visibleHeight) * min(1f, input.dt * BUILDING_HEIGHT_RESPONSE)
            }

            else -> {
                input.visibleHeight
            }
        }
    val nextLean = input.lean + (input.leanTarget - input.lean) * min(1f, input.dt * BUILDING_LEAN_RESPONSE)
    return BattleBuildingAnimationState(visibleHeight = nextVisibleHeight, lean = nextLean)
}

internal class BattleBuildingVisualController(
    private val effectsController: BattleEffectsController,
    private val threatCountProvider: () -> Int,
    private val interceptorCountProvider: () -> Int,
) {
    fun applyDamageVisual(
        building: BuildingEntity,
        newIntegrity: Float,
        epicenter: Vector3,
    ) {
        val update =
            battleBuildingDamageVisualUpdate(
                BattleBuildingDamageInput(
                    previousIntegrity = building.integrity,
                    newIntegrity = newIntegrity,
                    currentCollapseVelocity = building.collapseVelocity,
                    currentLeanTarget = building.leanTarget,
                    position = building.position,
                    baseHeight = building.baseHeight,
                    epicenter = epicenter,
                ),
            ) ?: return

        building.integrity = update.integrity
        building.collapseVelocity = update.collapseVelocity
        building.leanTarget = update.leanTarget
        update.visibleHeightOverride?.let { building.visibleHeight = it }
        building.instance
            .materials
            .first()
            .set(ColorAttribute.createDiffuse(update.tint))
        update.debrisPlan?.let(::spawnDebris)
    }

    fun update(
        buildings: Array<BuildingEntity>,
        dt: Float,
    ) {
        buildings.forEach { building ->
            val nextState =
                battleBuildingAnimationState(
                    BattleBuildingAnimationInput(
                        baseHeight = building.baseHeight,
                        integrity = building.integrity,
                        visibleHeight = building.visibleHeight,
                        collapseVelocity = building.collapseVelocity,
                        lean = building.lean,
                        leanTarget = building.leanTarget,
                        dt = dt,
                    ),
                )
            building.visibleHeight = nextState.visibleHeight
            building.lean = nextState.lean
            syncTransform(building)
        }
    }

    fun syncTransform(building: BuildingEntity) {
        val heightScale = (building.visibleHeight / building.baseHeight).coerceAtLeast(BUILDING_MIN_HEIGHT_SCALE)
        val centerY = building.visibleHeight * BUILDING_CENTER_HEIGHT_RATIO
        building.instance.transform.idt()
        building.instance.transform
            .translate(building.position.x, centerY, building.position.z)
            .rotate(Vector3.Y, building.yaw)
            .rotate(Vector3.Z, building.lean)
            .scale(1f, heightScale, 1f)
    }

    private fun spawnDebris(plan: BattleBuildingDebrisPlan) {
        val count =
            when (plan.profile) {
                BattleBuildingDebrisProfile.DESTROYED -> BUILDING_DESTROYED_DEBRIS_COUNT
                BattleBuildingDebrisProfile.DAMAGED -> BUILDING_DAMAGED_DEBRIS_COUNT
            }
        val color =
            when (plan.profile) {
                BattleBuildingDebrisProfile.DESTROYED -> BUILDING_DESTROYED_DEBRIS_COLOR
                BattleBuildingDebrisProfile.DAMAGED -> BUILDING_DAMAGED_DEBRIS_COLOR
            }
        effectsController.spawnDebris(
            position = plan.position,
            count = count,
            color = color,
            threatCount = threatCountProvider(),
            interceptorCount = interceptorCountProvider(),
        )
    }
}
