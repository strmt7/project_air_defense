package com.airdefense.game

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.environment.PointLight
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

internal const val EFFECT_PRESSURE_INTERCEPTOR_WEIGHT = 0.75f
internal const val EFFECT_PRESSURE_DIVISOR = 18f
internal const val EFFECT_PRESSURE_HEAVY = 18f
internal const val EFFECT_PRESSURE_MEDIUM = 12f
internal const val EFFECT_PRESSURE_LIGHT = 8f
internal const val EFFECT_PRESSURE_HEAVY_SCALE = 0.62f
internal const val EFFECT_PRESSURE_MEDIUM_SCALE = 0.78f
internal const val EFFECT_PRESSURE_LIGHT_SCALE = 0.9f
internal const val EFFECT_PRESSURE_FULL_SCALE = 1f
internal const val TRAIL_PRESSURE_EFFECT_DIVISOR = 20
internal const val TRAIL_PRESSURE_HEAVY = 22
internal const val TRAIL_PRESSURE_MEDIUM = 14
internal const val TRAIL_PRESSURE_MEDIUM_BONUS = 1
internal const val TRAIL_PRESSURE_HEAVY_BONUS = 2
internal const val TRAIL_EFFECT_HEADROOM = 80
internal const val BLAST_CORE_BASE_SCALE = 0.1f
internal const val BLAST_CORE_LIFE = 0.7f
internal const val BLAST_CORE_SIZE_MULTIPLIER = 1.18f
internal const val BLAST_CORE_FALLOFF = 1.25f
internal const val BLAST_LIGHT_INTENSITY_MULTIPLIER = 20f
internal const val SHOCKWAVE_Y_SCALE = 0.03f
internal const val SHOCKWAVE_Y_OFFSET = 6f
internal const val SHOCKWAVE_LIFE = 0.56f
internal const val SHOCKWAVE_SIZE_MULTIPLIER = 2.8f
internal const val SHOCKWAVE_SCALE_FALLOFF = 1.18f
internal const val SHOCKWAVE_HEIGHT_MULTIPLIER = 0.08f
internal const val SHOCKWAVE_ALPHA_MULTIPLIER = 0.72f
internal const val SPARK_SCALE = 0.3f
internal const val SPARK_LIFE_MIN = 0.26f
internal const val SPARK_LIFE_MAX = 0.46f
internal const val SPARK_SIZE_MIN = 0.22f
internal const val SPARK_SIZE_MAX = 0.42f
internal const val SPARK_VELOCITY_XY = 160f
internal const val SPARK_VELOCITY_UP_MIN = 40f
internal const val SPARK_VELOCITY_UP_MAX = 180f
internal const val SPARK_SCALE_BASE = 0.18f
internal const val SPARK_SCALE_MULTIPLIER = 0.12f
internal const val SPARK_DRAG = 1.8f
internal const val SPARK_MIN_DRAG_SCALE = 0.2f
internal const val SPARK_GRAVITY = 140f
internal const val SPARK_ALPHA_MULTIPLIER = 0.72f
internal const val SPARK_ALPHA_MAX = 0.85f
internal const val SMOKE_SCALE = 0.4f
internal const val SMOKE_LIFE_MIN = 1.1f
internal const val SMOKE_LIFE_MAX = 1.65f
internal const val SMOKE_SIZE_MIN = 0.7f
internal const val SMOKE_SIZE_MAX = 1.15f
internal const val SMOKE_DRIFT_X = 28f
internal const val SMOKE_DRIFT_Z = 20f
internal const val SMOKE_RISE_MIN = 22f
internal const val SMOKE_RISE_MAX = 48f
internal const val SMOKE_RISE_ACCELERATION = 10f
internal const val SMOKE_SCALE_BASE = 0.6f
internal const val SMOKE_ALPHA_MULTIPLIER = 0.26f
internal const val SMOKE_ALPHA_MAX = 0.3f
internal const val TRAIL_HOSTILE_INITIAL_SCALE = 0.86f
internal const val TRAIL_INTERCEPTOR_INITIAL_SCALE = 0.56f
internal const val TRAIL_HOSTILE_LIFE = 0.56f
internal const val TRAIL_INTERCEPTOR_LIFE = 0.34f
internal const val TRAIL_HOSTILE_SIZE = 2.25f
internal const val TRAIL_INTERCEPTOR_SIZE = 1.45f
internal const val TRAIL_HOSTILE_LIFT = 8f
internal const val TRAIL_INTERCEPTOR_LIFT = 4f
internal const val TRAIL_SCALE_BASE = 0.45f
internal const val TRAIL_ALPHA_MULTIPLIER = 0.42f
internal const val IMPACT_LIGHT_RADIUS_MULTIPLIER = 18f
internal const val IMPACT_LIGHT_DECAY = 260f
internal const val IMPACT_LIGHT_DEBRIS_SPEED_MIN = 40f
internal const val IMPACT_LIGHT_DEBRIS_SPEED_MAX = 220f
internal const val DEBRIS_DIRECTION_XZ = 1f
internal const val DEBRIS_DIRECTION_UP_MIN = 0.3f
internal const val DEBRIS_DIRECTION_UP_MAX = 1.5f
internal const val DEBRIS_SCALE_MIN = 0.35f
internal const val DEBRIS_SCALE_MAX = 1.5f
internal const val DEBRIS_LIFE_MIN = 1.1f
internal const val DEBRIS_LIFE_MAX = 3.2f
internal const val DEBRIS_GRAVITY = 120f
internal const val DEBRIS_ROTATION_X = 1.5f
internal const val DEBRIS_ROTATION_Y = 0.9f
internal const val DEBRIS_ROTATION_Z = 1.2f
internal const val DEBRIS_ROTATION_SPEED = 120f
internal val BLAST_DIFFUSE = Color.valueOf("FFF5DBFF")
internal val BLAST_EMISSIVE = Color.valueOf("FFBD47FF")
internal val SHOCKWAVE_DIFFUSE = Color.valueOf("FFB857FF")
internal val SHOCKWAVE_EMISSIVE = Color.valueOf("B84714FF")
internal val SPARK_DIFFUSE = Color.valueOf("FFB857FF")
internal val SPARK_EMISSIVE = Color.valueOf("F26B14FF")
internal val SMOKE_DIFFUSE = Color.valueOf("38383DFF")
internal val SMOKE_EMISSIVE = Color.valueOf("0F0F12FF")
internal val TRAIL_HOSTILE_DIFFUSE = Color.valueOf("FF6B14FF")
internal val TRAIL_INTERCEPTOR_DIFFUSE = Color.valueOf("75E6FFFF")
internal val TRAIL_HOSTILE_EMISSIVE = Color.valueOf("9E2E0AFF")
internal val TRAIL_INTERCEPTOR_EMISSIVE = Color.valueOf("1F617AFF")
internal val IMPACT_LIGHT_COLOR = Color.valueOf("FFD180FF")
internal val EFFECT_LIGHT_FALLOFF_COLOR = Color.valueOf("FFCC6BFF")
internal val DEBRIS_ROTATION_AXIS = Vector3(DEBRIS_ROTATION_X, DEBRIS_ROTATION_Y, DEBRIS_ROTATION_Z)

internal data class BattleEffectLoad(
    val threatCount: Int,
    val interceptorCount: Int,
    val effectCount: Int,
)

internal fun battleEffectBudgetScale(
    load: BattleEffectLoad,
    baseScale: Float,
): Float {
    val scenePressure =
        load.threatCount +
            load.interceptorCount * EFFECT_PRESSURE_INTERCEPTOR_WEIGHT +
            load.effectCount / EFFECT_PRESSURE_DIVISOR
    val pressureScale =
        when {
            scenePressure >= EFFECT_PRESSURE_HEAVY -> EFFECT_PRESSURE_HEAVY_SCALE
            scenePressure >= EFFECT_PRESSURE_MEDIUM -> EFFECT_PRESSURE_MEDIUM_SCALE
            scenePressure >= EFFECT_PRESSURE_LIGHT -> EFFECT_PRESSURE_LIGHT_SCALE
            else -> EFFECT_PRESSURE_FULL_SCALE
        }
    return baseScale * pressureScale
}

internal fun battleAdjustedEffectCount(
    baseCount: Int,
    minimum: Int,
    load: BattleEffectLoad,
    baseScale: Float,
): Int {
    val scaled = (baseCount * battleEffectBudgetScale(load, baseScale)).toInt()
    return max(minimum, min(baseCount, scaled))
}

internal fun battleAdjustedTrailStride(
    baseStride: Int,
    load: BattleEffectLoad,
): Int {
    val scenePressure =
        load.threatCount +
            load.interceptorCount +
            load.effectCount / TRAIL_PRESSURE_EFFECT_DIVISOR
    val extraStride =
        when {
            scenePressure >= TRAIL_PRESSURE_HEAVY -> TRAIL_PRESSURE_HEAVY_BONUS
            scenePressure >= TRAIL_PRESSURE_MEDIUM -> TRAIL_PRESSURE_MEDIUM_BONUS
            else -> 0
        }
    return (baseStride + extraStride).coerceAtLeast(1)
}

internal class BattleEffectsController(
    private val models: ObjectMap<String, Model>,
    private val qualityProfile: GraphicsQualityProfile,
    private val impactLight: PointLight,
) {
    private val visualEffects = Array<VisualEffect>()
    private val debrisPieces = Array<DebrisEntity>()
    private val strongestEffectPosition = Vector3()
    private var hostileTrailSamples = 0
    private var interceptorTrailSamples = 0

    val effects: Array<VisualEffect>
        get() = visualEffects

    val debris: Array<DebrisEntity>
        get() = debrisPieces

    fun canSpawnTrail(
        hostile: Boolean,
        threatCount: Int,
        interceptorCount: Int,
    ): Boolean {
        if (activeTrailEffectCount(visualEffects) >= qualityProfile.maxTrailEffects) return false

        val stride =
            battleAdjustedTrailStride(
                baseStride =
                    if (hostile) {
                        qualityProfile.hostileTrailStride
                    } else {
                        qualityProfile.interceptorTrailStride
                    },
                load = effectLoad(threatCount, interceptorCount),
            )
        return stride <= 1 || nextTrailSampleIndex(hostile) % stride == 1
    }

    fun spawnBlast(
        position: Vector3,
        size: Float,
        threatCount: Int,
        interceptorCount: Int,
    ) {
        val load = effectLoad(threatCount, interceptorCount)
        val sparkCount =
            battleAdjustedEffectCount(
                baseCount = qualityProfile.baseSparkCount,
                minimum = 2,
                load = load,
                baseScale = qualityProfile.effectBudgetScale,
            )
        val smokeCount =
            battleAdjustedEffectCount(
                baseCount = qualityProfile.baseSmokeCount,
                minimum = 1,
                load = load,
                baseScale = qualityProfile.effectBudgetScale,
            )
        visualEffects.add(createBlastCoreEffect(models, position, size))
        visualEffects.add(createShockwaveEffect(models, position, size))
        repeat(sparkCount) {
            visualEffects.add(createSparkEffect(models, position, size))
        }
        repeat(smokeCount) {
            visualEffects.add(createSmokeEffect(models, position, size))
        }
        impactLight.set(
            IMPACT_LIGHT_COLOR,
            position,
            size * IMPACT_LIGHT_RADIUS_MULTIPLIER * qualityProfile.lightIntensityScale,
        )
    }

    fun spawnTrail(
        position: Vector3,
        hostile: Boolean,
    ) {
        if (visualEffects.size >= qualityProfile.maxTrailEffects + TRAIL_EFFECT_HEADROOM) return
        visualEffects.add(createTrailEffect(models, position, hostile))
    }

    fun spawnDebris(
        position: Vector3,
        count: Int,
        color: Color,
        threatCount: Int,
        interceptorCount: Int,
    ) {
        val debrisCount =
            battleAdjustedEffectCount(
                baseCount = min(count, qualityProfile.maxDebrisPieces),
                minimum = 1,
                load = effectLoad(threatCount, interceptorCount),
                baseScale = qualityProfile.effectBudgetScale,
            )
        repeat(debrisCount) {
            debrisPieces.add(createDebrisPiece(models, position, color))
        }
    }

    fun update(dt: Float) {
        strongestEffectPosition.setZero()
        updateVisualEffects(dt)
        updateDebrisPieces(dt)
    }

    private fun effectLoad(
        threatCount: Int,
        interceptorCount: Int,
    ): BattleEffectLoad = BattleEffectLoad(threatCount, interceptorCount, visualEffects.size)

    private fun nextTrailSampleIndex(hostile: Boolean): Int =
        if (hostile) {
            hostileTrailSamples += 1
            hostileTrailSamples
        } else {
            interceptorTrailSamples += 1
            interceptorTrailSamples
        }

    private fun updateVisualEffects(dt: Float) {
        val iterator = visualEffects.iterator()
        var strongestIntensity = 0f
        while (iterator.hasNext()) {
            val effect = iterator.next()
            strongestIntensity =
                updateVisualEffect(
                    effect = effect,
                    dt = dt,
                    strongestIntensity = strongestIntensity,
                    strongestPosition = strongestEffectPosition,
                )
            if (effect.life <= 0f) iterator.remove()
        }
        updateImpactLight(dt, strongestIntensity, strongestEffectPosition, impactLight)
    }

    private fun updateDebrisPieces(dt: Float) {
        val iterator = debrisPieces.iterator()
        while (iterator.hasNext()) {
            val piece = iterator.next()
            piece.life -= dt
            piece.velocity.y -= DEBRIS_GRAVITY * dt
            piece.position.mulAdd(piece.velocity, dt)
            piece.rotation.mulAdd(DEBRIS_ROTATION_AXIS, dt * DEBRIS_ROTATION_SPEED)
            piece.instance.transform
                .setToScaling(piece.scale, piece.scale, piece.scale)
                .rotate(Vector3.X, piece.rotation.x)
                .rotate(Vector3.Y, piece.rotation.y)
                .rotate(Vector3.Z, piece.rotation.z)
                .trn(piece.position)
            if (piece.position.y <= 0f || piece.life <= 0f) iterator.remove()
        }
    }
}

private fun activeTrailEffectCount(effects: Array<VisualEffect>): Int {
    var count = 0
    effects.forEach { effect ->
        if (effect.type == EffectType.TRAIL) count++
    }
    return count
}
