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

private const val EFFECT_PRESSURE_INTERCEPTOR_WEIGHT = 0.75f
private const val EFFECT_PRESSURE_DIVISOR = 18f
private const val EFFECT_PRESSURE_HEAVY = 18f
private const val EFFECT_PRESSURE_MEDIUM = 12f
private const val EFFECT_PRESSURE_LIGHT = 8f
private const val EFFECT_PRESSURE_HEAVY_SCALE = 0.62f
private const val EFFECT_PRESSURE_MEDIUM_SCALE = 0.78f
private const val EFFECT_PRESSURE_LIGHT_SCALE = 0.9f
private const val EFFECT_PRESSURE_FULL_SCALE = 1f
private const val TRAIL_PRESSURE_EFFECT_DIVISOR = 20
private const val TRAIL_PRESSURE_HEAVY = 22
private const val TRAIL_PRESSURE_MEDIUM = 14
private const val TRAIL_PRESSURE_MEDIUM_BONUS = 1
private const val TRAIL_PRESSURE_HEAVY_BONUS = 2
private const val TRAIL_EFFECT_HEADROOM = 80
private const val BLAST_CORE_BASE_SCALE = 0.1f
private const val BLAST_CORE_LIFE = 0.7f
private const val BLAST_CORE_SIZE_MULTIPLIER = 1.18f
private const val BLAST_CORE_FALLOFF = 1.25f
private const val BLAST_LIGHT_INTENSITY_MULTIPLIER = 20f
private const val SHOCKWAVE_Y_SCALE = 0.03f
private const val SHOCKWAVE_Y_OFFSET = 6f
private const val SHOCKWAVE_LIFE = 0.56f
private const val SHOCKWAVE_SIZE_MULTIPLIER = 2.8f
private const val SHOCKWAVE_SCALE_FALLOFF = 1.18f
private const val SHOCKWAVE_HEIGHT_MULTIPLIER = 0.08f
private const val SHOCKWAVE_ALPHA_MULTIPLIER = 0.72f
private const val SPARK_SCALE = 0.3f
private const val SPARK_LIFE_MIN = 0.26f
private const val SPARK_LIFE_MAX = 0.46f
private const val SPARK_SIZE_MIN = 0.22f
private const val SPARK_SIZE_MAX = 0.42f
private const val SPARK_VELOCITY_XY = 160f
private const val SPARK_VELOCITY_UP_MIN = 40f
private const val SPARK_VELOCITY_UP_MAX = 180f
private const val SPARK_SCALE_BASE = 0.18f
private const val SPARK_SCALE_MULTIPLIER = 0.12f
private const val SPARK_DRAG = 1.8f
private const val SPARK_MIN_DRAG_SCALE = 0.2f
private const val SPARK_GRAVITY = 140f
private const val SPARK_ALPHA_MULTIPLIER = 0.72f
private const val SPARK_ALPHA_MAX = 0.85f
private const val SMOKE_SCALE = 0.4f
private const val SMOKE_LIFE_MIN = 1.1f
private const val SMOKE_LIFE_MAX = 1.65f
private const val SMOKE_SIZE_MIN = 0.7f
private const val SMOKE_SIZE_MAX = 1.15f
private const val SMOKE_DRIFT_X = 28f
private const val SMOKE_DRIFT_Z = 20f
private const val SMOKE_RISE_MIN = 22f
private const val SMOKE_RISE_MAX = 48f
private const val SMOKE_RISE_ACCELERATION = 10f
private const val SMOKE_SCALE_BASE = 0.6f
private const val SMOKE_ALPHA_MULTIPLIER = 0.26f
private const val SMOKE_ALPHA_MAX = 0.3f
private const val TRAIL_HOSTILE_INITIAL_SCALE = 0.86f
private const val TRAIL_INTERCEPTOR_INITIAL_SCALE = 0.56f
private const val TRAIL_HOSTILE_LIFE = 0.56f
private const val TRAIL_INTERCEPTOR_LIFE = 0.34f
private const val TRAIL_HOSTILE_SIZE = 2.25f
private const val TRAIL_INTERCEPTOR_SIZE = 1.45f
private const val TRAIL_HOSTILE_LIFT = 8f
private const val TRAIL_INTERCEPTOR_LIFT = 4f
private const val TRAIL_SCALE_BASE = 0.45f
private const val TRAIL_ALPHA_MULTIPLIER = 0.42f
private const val IMPACT_LIGHT_RADIUS_MULTIPLIER = 18f
private const val IMPACT_LIGHT_DECAY = 260f
private const val IMPACT_LIGHT_DEBRIS_SPEED_MIN = 40f
private const val IMPACT_LIGHT_DEBRIS_SPEED_MAX = 220f
private const val DEBRIS_DIRECTION_XZ = 1f
private const val DEBRIS_DIRECTION_UP_MIN = 0.3f
private const val DEBRIS_DIRECTION_UP_MAX = 1.5f
private const val DEBRIS_SCALE_MIN = 0.35f
private const val DEBRIS_SCALE_MAX = 1.5f
private const val DEBRIS_LIFE_MIN = 1.1f
private const val DEBRIS_LIFE_MAX = 3.2f
private const val DEBRIS_GRAVITY = 120f
private const val DEBRIS_ROTATION_X = 1.5f
private const val DEBRIS_ROTATION_Y = 0.9f
private const val DEBRIS_ROTATION_Z = 1.2f
private const val DEBRIS_ROTATION_SPEED = 120f
private val BLAST_DIFFUSE = Color(1f, 0.96f, 0.86f, 1f)
private val BLAST_EMISSIVE = Color(1f, 0.74f, 0.28f, 1f)
private val SHOCKWAVE_DIFFUSE = Color(1f, 0.72f, 0.34f, 1f)
private val SHOCKWAVE_EMISSIVE = Color(0.72f, 0.28f, 0.08f, 1f)
private val SPARK_DIFFUSE = Color(1f, 0.72f, 0.28f, 1f)
private val SPARK_EMISSIVE = Color(0.95f, 0.42f, 0.08f, 1f)
private val SMOKE_DIFFUSE = Color(0.22f, 0.22f, 0.24f, 1f)
private val SMOKE_EMISSIVE = Color(0.06f, 0.06f, 0.07f, 1f)
private val TRAIL_HOSTILE_DIFFUSE = Color(1f, 0.42f, 0.08f, 1f)
private val TRAIL_INTERCEPTOR_DIFFUSE = Color(0.46f, 0.9f, 1f, 1f)
private val TRAIL_HOSTILE_EMISSIVE = Color(0.62f, 0.18f, 0.04f, 1f)
private val TRAIL_INTERCEPTOR_EMISSIVE = Color(0.12f, 0.38f, 0.48f, 1f)
private val IMPACT_LIGHT_COLOR = Color(1f, 0.82f, 0.5f, 1f)
private val EFFECT_LIGHT_FALLOFF_COLOR = Color(1f, 0.8f, 0.42f, 1f)
private val DEBRIS_ROTATION_AXIS = Vector3(DEBRIS_ROTATION_X, DEBRIS_ROTATION_Y, DEBRIS_ROTATION_Z)

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
            battleAdjustedEffectCount(qualityProfile.baseSparkCount, minimum = 2, load = load, baseScale = qualityProfile.effectBudgetScale)
        val smokeCount =
            battleAdjustedEffectCount(qualityProfile.baseSmokeCount, minimum = 1, load = load, baseScale = qualityProfile.effectBudgetScale)
        visualEffects.add(createBlastCoreEffect(models, position, size))
        visualEffects.add(createShockwaveEffect(models, position, size))
        repeat(sparkCount) {
            visualEffects.add(createSparkEffect(models, position, size))
        }
        repeat(smokeCount) {
            visualEffects.add(createSmokeEffect(models, position, size))
        }
        impactLight.set(IMPACT_LIGHT_COLOR, position, size * IMPACT_LIGHT_RADIUS_MULTIPLIER * qualityProfile.lightIntensityScale)
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

private fun createBlastCoreEffect(
    models: ObjectMap<String, Model>,
    position: Vector3,
    size: Float,
): VisualEffect {
    val instance = ModelInstance(models.get("blast"))
    instance.transform.setToScaling(BLAST_CORE_BASE_SCALE, BLAST_CORE_BASE_SCALE, BLAST_CORE_BASE_SCALE).trn(position)
    instance.materials.first().set(ColorAttribute.createDiffuse(BLAST_DIFFUSE))
    instance.materials.first().set(ColorAttribute.createEmissive(BLAST_EMISSIVE))
    return VisualEffect(
        instance = instance,
        position = position.cpy(),
        life = BLAST_CORE_LIFE,
        initialLife = BLAST_CORE_LIFE,
        type = EffectType.BLAST,
        maxScale = size * BLAST_CORE_SIZE_MULTIPLIER,
    )
}

private fun createShockwaveEffect(
    models: ObjectMap<String, Model>,
    position: Vector3,
    size: Float,
): VisualEffect {
    val instance = ModelInstance(models.get("blast"))
    instance.transform
        .setToScaling(BLAST_CORE_BASE_SCALE, SHOCKWAVE_Y_SCALE, BLAST_CORE_BASE_SCALE)
        .trn(position.x, position.y + SHOCKWAVE_Y_OFFSET, position.z)
    instance.materials.first().set(ColorAttribute.createDiffuse(SHOCKWAVE_DIFFUSE))
    instance.materials.first().set(ColorAttribute.createEmissive(SHOCKWAVE_EMISSIVE))
    return VisualEffect(
        instance = instance,
        position = Vector3(position.x, position.y + SHOCKWAVE_Y_OFFSET, position.z),
        life = SHOCKWAVE_LIFE,
        initialLife = SHOCKWAVE_LIFE,
        type = EffectType.SHOCKWAVE,
        maxScale = size * SHOCKWAVE_SIZE_MULTIPLIER,
    )
}

private fun createSparkEffect(
    models: ObjectMap<String, Model>,
    position: Vector3,
    size: Float,
): VisualEffect {
    val sparkLife = MathUtils.random(SPARK_LIFE_MIN, SPARK_LIFE_MAX)
    val instance = ModelInstance(models.get("trail"))
    instance.transform.setToScaling(SPARK_SCALE, SPARK_SCALE, SPARK_SCALE).trn(position)
    instance.materials.first().set(ColorAttribute.createDiffuse(SPARK_DIFFUSE))
    instance.materials.first().set(ColorAttribute.createEmissive(SPARK_EMISSIVE))
    return VisualEffect(
        instance = instance,
        position = position.cpy(),
        life = sparkLife,
        initialLife = sparkLife,
        type = EffectType.SPARK,
        maxScale = size * MathUtils.random(SPARK_SIZE_MIN, SPARK_SIZE_MAX),
        velocity =
            Vector3(
                MathUtils.random(-SPARK_VELOCITY_XY, SPARK_VELOCITY_XY),
                MathUtils.random(SPARK_VELOCITY_UP_MIN, SPARK_VELOCITY_UP_MAX),
                MathUtils.random(-SPARK_VELOCITY_XY, SPARK_VELOCITY_XY),
            ),
    )
}

private fun createSmokeEffect(
    models: ObjectMap<String, Model>,
    position: Vector3,
    size: Float,
): VisualEffect {
    val smokeLife = MathUtils.random(SMOKE_LIFE_MIN, SMOKE_LIFE_MAX)
    val instance = ModelInstance(models.get("trail"))
    instance.transform.setToScaling(SMOKE_SCALE, SMOKE_SCALE, SMOKE_SCALE).trn(position)
    instance.materials.first().set(ColorAttribute.createDiffuse(SMOKE_DIFFUSE))
    instance.materials.first().set(ColorAttribute.createEmissive(SMOKE_EMISSIVE))
    return VisualEffect(
        instance = instance,
        position = position.cpy(),
        life = smokeLife,
        initialLife = smokeLife,
        type = EffectType.SMOKE,
        maxScale = size * MathUtils.random(SMOKE_SIZE_MIN, SMOKE_SIZE_MAX),
        velocity =
            Vector3(
                MathUtils.random(-SMOKE_DRIFT_X, SMOKE_DRIFT_X),
                MathUtils.random(SMOKE_RISE_MIN, SMOKE_RISE_MAX),
                MathUtils.random(-SMOKE_DRIFT_Z, SMOKE_DRIFT_Z),
            ),
    )
}

private fun createTrailEffect(
    models: ObjectMap<String, Model>,
    position: Vector3,
    hostile: Boolean,
): VisualEffect {
    val instance = ModelInstance(models.get("trail"))
    val initialScale =
        if (hostile) {
            TRAIL_HOSTILE_INITIAL_SCALE
        } else {
            TRAIL_INTERCEPTOR_INITIAL_SCALE
        }
    instance.transform.setToScaling(initialScale, initialScale, initialScale).trn(position)
    val effect =
        VisualEffect(
            instance = instance,
            position = position.cpy(),
            life =
                if (hostile) {
                    TRAIL_HOSTILE_LIFE
                } else {
                    TRAIL_INTERCEPTOR_LIFE
                },
            initialLife =
                if (hostile) {
                    TRAIL_HOSTILE_LIFE
                } else {
                    TRAIL_INTERCEPTOR_LIFE
                },
            type = EffectType.TRAIL,
            maxScale =
                if (hostile) {
                    TRAIL_HOSTILE_SIZE
                } else {
                    TRAIL_INTERCEPTOR_SIZE
                },
            velocity =
                Vector3(
                    0f,
                    if (hostile) {
                        TRAIL_HOSTILE_LIFT
                    } else {
                        TRAIL_INTERCEPTOR_LIFT
                    },
                    0f,
                ),
        )
    effect.instance.materials.first().set(
        ColorAttribute.createDiffuse(
            if (hostile) TRAIL_HOSTILE_DIFFUSE else TRAIL_INTERCEPTOR_DIFFUSE,
        ),
    )
    effect.instance.materials.first().set(
        ColorAttribute.createEmissive(
            if (hostile) TRAIL_HOSTILE_EMISSIVE else TRAIL_INTERCEPTOR_EMISSIVE,
        ),
    )
    return effect
}

private fun createDebrisPiece(
    models: ObjectMap<String, Model>,
    position: Vector3,
    color: Color,
): DebrisEntity {
    val velocity =
        Vector3(
            MathUtils.random(-DEBRIS_DIRECTION_XZ, DEBRIS_DIRECTION_XZ),
            MathUtils.random(DEBRIS_DIRECTION_UP_MIN, DEBRIS_DIRECTION_UP_MAX),
            MathUtils.random(-DEBRIS_DIRECTION_XZ, DEBRIS_DIRECTION_XZ),
        ).nor().scl(MathUtils.random(IMPACT_LIGHT_DEBRIS_SPEED_MIN, IMPACT_LIGHT_DEBRIS_SPEED_MAX))
    val instance = ModelInstance(models.get("debris"))
    instance.materials.first().set(ColorAttribute.createDiffuse(color))
    val size = MathUtils.random(DEBRIS_SCALE_MIN, DEBRIS_SCALE_MAX)
    instance.transform.setToScaling(size, size, size).trn(position)
    return DebrisEntity(
        instance = instance,
        position = position.cpy(),
        velocity = velocity,
        life = MathUtils.random(DEBRIS_LIFE_MIN, DEBRIS_LIFE_MAX),
        scale = size,
    )
}

private fun updateVisualEffect(
    effect: VisualEffect,
    dt: Float,
    strongestIntensity: Float,
    strongestPosition: Vector3,
): Float {
    effect.life -= dt
    val progress = (effect.life / effect.initialLife).coerceIn(0f, 1f)
    val blend =
        effect.instance.materials
            .first()
            .get(BlendingAttribute.Type) as? BlendingAttribute
    return when (effect.type) {
        EffectType.BLAST -> {
            updateBlastEffect(effect, progress, blend, strongestIntensity, strongestPosition)
        }

        EffectType.SHOCKWAVE -> {
            updateShockwaveEffect(effect, progress, blend)
            strongestIntensity
        }

        EffectType.SMOKE -> {
            updateSmokeEffect(effect, dt, progress, blend)
            strongestIntensity
        }

        EffectType.SPARK -> {
            updateSparkEffect(effect, dt, progress, blend)
            strongestIntensity
        }

        EffectType.TRAIL -> {
            updateTrailEffect(effect, dt, progress, blend)
            strongestIntensity
        }
    }
}

private fun updateBlastEffect(
    effect: VisualEffect,
    progress: Float,
    blend: BlendingAttribute?,
    strongestIntensity: Float,
    strongestPosition: Vector3,
): Float {
    val scale = effect.maxScale * (BLAST_CORE_FALLOFF - progress * progress)
    effect.instance.transform
        .setToScaling(scale, scale, scale)
        .trn(effect.position)
    blend?.opacity = (progress * progress).coerceIn(0f, 1f)
    val intensity = effect.maxScale * progress * BLAST_LIGHT_INTENSITY_MULTIPLIER
    if (intensity > strongestIntensity) {
        strongestPosition.set(effect.position)
        return intensity
    }
    return strongestIntensity
}

private fun updateShockwaveEffect(
    effect: VisualEffect,
    progress: Float,
    blend: BlendingAttribute?,
) {
    val scale = effect.maxScale * (SHOCKWAVE_SCALE_FALLOFF - progress)
    effect.instance.transform
        .setToScaling(scale, scale * SHOCKWAVE_HEIGHT_MULTIPLIER, scale)
        .trn(effect.position)
    blend?.opacity = (progress * SHOCKWAVE_ALPHA_MULTIPLIER).coerceIn(0f, 1f)
}

private fun updateSmokeEffect(
    effect: VisualEffect,
    dt: Float,
    progress: Float,
    blend: BlendingAttribute?,
) {
    effect.position.mulAdd(effect.velocity, dt)
    effect.velocity.y += dt * SMOKE_RISE_ACCELERATION
    val scale = SMOKE_SCALE_BASE + (1f - progress) * effect.maxScale
    effect.instance.transform
        .setToScaling(scale, scale, scale)
        .trn(effect.position)
    blend?.opacity = (SMOKE_ALPHA_MULTIPLIER * sqrt(progress)).coerceIn(0f, SMOKE_ALPHA_MAX)
}

private fun updateSparkEffect(
    effect: VisualEffect,
    dt: Float,
    progress: Float,
    blend: BlendingAttribute?,
) {
    effect.position.mulAdd(effect.velocity, dt)
    effect.velocity.scl((1f - dt * SPARK_DRAG).coerceAtLeast(SPARK_MIN_DRAG_SCALE))
    effect.velocity.y -= dt * SPARK_GRAVITY
    val scale = SPARK_SCALE_BASE + progress * effect.maxScale * SPARK_SCALE_MULTIPLIER
    effect.instance.transform
        .setToScaling(scale, scale, scale)
        .trn(effect.position)
    blend?.opacity = (SPARK_ALPHA_MULTIPLIER * progress).coerceIn(0f, SPARK_ALPHA_MAX)
}

private fun updateTrailEffect(
    effect: VisualEffect,
    dt: Float,
    progress: Float,
    blend: BlendingAttribute?,
) {
    effect.position.mulAdd(effect.velocity, dt)
    val scale = TRAIL_SCALE_BASE + (1f - progress) * effect.maxScale
    effect.instance.transform
        .setToScaling(scale, scale, scale)
        .trn(effect.position)
    blend?.opacity = TRAIL_ALPHA_MULTIPLIER * progress
}

private fun updateImpactLight(
    dt: Float,
    strongestIntensity: Float,
    strongestPosition: Vector3,
    impactLight: PointLight,
) {
    if (strongestIntensity > 0f) {
        impactLight.set(EFFECT_LIGHT_FALLOFF_COLOR, strongestPosition, strongestIntensity)
        return
    }
    impactLight.intensity = max(0f, impactLight.intensity - dt * IMPACT_LIGHT_DECAY)
}
