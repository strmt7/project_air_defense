package com.airdefense.game

import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import kotlin.math.abs

private const val ROTATION_ZERO_TOLERANCE = 0.0001f
private const val ROTATION_PARALLEL_THRESHOLD = 0.98f

internal class BattleProjectileVisualController(
    private val launchers: Array<ModelInstance>,
    private val threatsById: ObjectMap<String, ThreatEntity>,
    private val interceptorsById: ObjectMap<String, InterceptorEntity>,
    private val threatScale: Float,
    private val interceptorScale: Float,
    private val pulseLauncherLight: (Int) -> Unit,
) {
    private val tempA = Vector3()
    private val tempB = Vector3()
    private val tempC = Vector3()
    private val tempD = Vector3()

    fun pulseLauncher(
        launch: InterceptorLaunchEvent,
        launchVelocity: Vector3,
    ) {
        if (launch.launcherIndex in 0 until launchers.size) {
            launchers[launch.launcherIndex].setRotationToward(launchVelocity)
        }
        pulseLauncherLight(launch.launcherIndex)
    }

    fun syncRenderEntitiesFromSimulation(simulation: BattleSimulation) {
        simulation.threats.forEach { state ->
            val renderThreat = threatsById[state.id] ?: return@forEach
            renderThreat.position.set(state.position)
            renderThreat.velocity.set(state.velocity)
            renderThreat.targetPosition.set(state.targetPosition)
            renderThreat.isTracked = state.isTracked
            renderThreat.trailCooldown = state.trailCooldown
            syncProjectileTransform(
                renderThreat.instance,
                renderThreat.position,
                renderThreat.velocity,
                threatScale,
            )
        }

        simulation.interceptors.forEach { state ->
            val renderInterceptor = interceptorsById[state.id] ?: return@forEach
            renderInterceptor.position.set(state.position)
            renderInterceptor.velocity.set(state.velocity)
            renderInterceptor.target = state.targetId?.let { threatsById[it] }
            renderInterceptor.trailCooldown = state.trailCooldown
            syncProjectileTransform(
                renderInterceptor.instance,
                renderInterceptor.position,
                renderInterceptor.velocity,
                interceptorScale,
            )
        }
    }

    fun syncProjectileTransform(
        instance: ModelInstance,
        position: Vector3,
        velocity: Vector3,
        scale: Float,
    ) {
        instance.transform.setToScaling(scale, scale, scale).trn(position)
        instance.setRotationToward(velocity)
    }

    private fun ModelInstance.setRotationToward(direction: Vector3) {
        if (direction.isZero(ROTATION_ZERO_TOLERANCE)) return
        transform.getTranslation(tempC)
        tempA.set(direction).nor()
        tempB.set(Vector3.Y)
        if (abs(tempA.dot(tempB)) > ROTATION_PARALLEL_THRESHOLD) {
            tempB.set(Vector3.Z)
        }
        tempD.set(tempB).crs(tempA).nor()
        tempB.set(tempA).crs(tempD).nor()
        transform.set(tempD, tempA, tempB, tempC)
    }
}
