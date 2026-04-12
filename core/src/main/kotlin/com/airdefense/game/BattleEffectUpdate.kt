package com.airdefense.game

import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute
import com.badlogic.gdx.graphics.g3d.environment.PointLight
import com.badlogic.gdx.math.Vector3
import kotlin.math.max
import kotlin.math.sqrt

internal fun updateVisualEffect(
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

internal fun updateImpactLight(
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
