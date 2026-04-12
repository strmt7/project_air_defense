package com.airdefense.game

import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.utils.Array

internal data class BattleWorldRenderPass(
    val modelBatch: ModelBatch,
    val camera: PerspectiveCamera,
    val environment: Environment,
    val instances: Array<ModelInstance>,
    val cityBlocks: Array<BuildingEntity>,
    val threats: Array<ThreatEntity>,
    val interceptors: Array<InterceptorEntity>,
    val debris: Array<DebrisEntity>,
    val effects: Array<VisualEffect>,
    val minimumVisibleBuildingHeight: Float,
) {
    fun render() {
        modelBatch.begin(camera)
        instances.forEach { modelBatch.render(it, environment) }
        cityBlocks.forEach { building ->
            if (building.visibleHeight > minimumVisibleBuildingHeight) {
                modelBatch.render(building.instance, environment)
            }
        }
        threats.forEach { modelBatch.render(it.instance, environment) }
        interceptors.forEach { modelBatch.render(it.instance, environment) }
        debris.forEach { modelBatch.render(it.instance, environment) }
        effects.forEach { modelBatch.render(it.instance, environment) }
        modelBatch.end()
    }
}
