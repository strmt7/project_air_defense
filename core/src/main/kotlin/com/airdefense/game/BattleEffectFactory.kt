package com.airdefense.game

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.ObjectMap

internal fun createBlastCoreEffect(
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

internal fun createShockwaveEffect(
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

internal fun createSparkEffect(
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

internal fun createSmokeEffect(
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

internal fun createTrailEffect(
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
            life = if (hostile) TRAIL_HOSTILE_LIFE else TRAIL_INTERCEPTOR_LIFE,
            initialLife = if (hostile) TRAIL_HOSTILE_LIFE else TRAIL_INTERCEPTOR_LIFE,
            type = EffectType.TRAIL,
            maxScale = if (hostile) TRAIL_HOSTILE_SIZE else TRAIL_INTERCEPTOR_SIZE,
            velocity = Vector3(0f, if (hostile) TRAIL_HOSTILE_LIFT else TRAIL_INTERCEPTOR_LIFT, 0f),
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

internal fun createDebrisPiece(
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
