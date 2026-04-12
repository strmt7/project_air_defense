package com.airdefense.game

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap

internal data class BattleRenderCollections(
    val models: ObjectMap<String, Model>,
    val launchers: Array<ModelInstance>,
    val cityBlocksById: ObjectMap<String, BuildingEntity>,
    val threats: Array<ThreatEntity>,
    val threatsById: ObjectMap<String, ThreatEntity>,
    val interceptors: Array<InterceptorEntity>,
    val interceptorsById: ObjectMap<String, InterceptorEntity>,
)

internal data class BattleStepCallbacks(
    val canSpawnTrail: (Boolean) -> Boolean,
    val spawnTrail: (Vector3, Boolean) -> Unit,
    val syncProjectileTransform: (ModelInstance, Vector3, Vector3, Float) -> Unit,
    val syncRenderEntitiesFromSimulation: () -> Unit,
    val syncBattleStateFromSimulation: () -> Unit,
    val pulseLauncher: (InterceptorLaunchEvent, Vector3) -> Unit,
    val spawnBlast: (Vector3, Float) -> Unit,
    val spawnDebris: (Vector3, Int, Color) -> Unit,
    val triggerShake: (Float, Float) -> Unit,
    val playSfx: (String, Float) -> Unit,
    val applyBuildingDamageVisual: (BuildingEntity, Float, Vector3) -> Unit,
    val setStatus: (String, String) -> Unit,
    val syncHud: () -> Unit,
)

internal class BattleSimulationStepApplier(
    private val simulationProvider: () -> BattleSimulation,
    private val renderCollections: BattleRenderCollections,
    private val callbacks: BattleStepCallbacks,
    private val threatScale: Float,
    private val interceptorScale: Float,
) {
    fun apply(step: BattleStepEvents) {
        applySpawnedThreats(step.spawnedThreatIds)
        applyLaunchedInterceptors(step.launchedInterceptors)
        applyTrailEvents(step.trailEvents)
        applyBlastEvents(step.blastEvents)
        applyBuildingDamageEvents(step.buildingDamageEvents)
        removeThreatEntities(step.removedThreatIds)
        removeInterceptorEntities(step.removedInterceptorIds)
        callbacks.syncRenderEntitiesFromSimulation()
        callbacks.syncBattleStateFromSimulation()
        applyStepStatus(step)
    }

    private fun applySpawnedThreats(ids: List<String>) {
        val simulation = simulationProvider()
        ids.forEach { id ->
            simulation.findThreat(id)?.let { threat ->
                val renderThreat =
                    ThreatEntity(
                        instance = ModelInstance(renderCollections.models.get("threat")),
                        position = threat.position.cpy(),
                        targetPosition = threat.targetPosition.cpy(),
                        velocity = threat.velocity.cpy(),
                        id = threat.id,
                        isTracked = threat.isTracked,
                        trailCooldown = threat.trailCooldown,
                    )
                callbacks.syncProjectileTransform(
                    renderThreat.instance,
                    renderThreat.position,
                    renderThreat.velocity,
                    threatScale,
                )
                renderCollections.threats.add(renderThreat)
                renderCollections.threatsById.put(id, renderThreat)
            }
        }
    }

    private fun applyLaunchedInterceptors(launches: List<InterceptorLaunchEvent>) {
        val simulation = simulationProvider()
        launches.forEach { launch ->
            val interceptorState = simulation.findInterceptor(launch.interceptorId) ?: return@forEach
            val renderInterceptor =
                InterceptorEntity(
                    id = interceptorState.id,
                    instance = ModelInstance(renderCollections.models.get("interceptor")),
                    position = interceptorState.position.cpy(),
                    velocity = interceptorState.velocity.cpy(),
                    target = interceptorState.targetId?.let { renderCollections.threatsById[it] },
                    trailCooldown = interceptorState.trailCooldown,
                )
            callbacks.syncProjectileTransform(
                renderInterceptor.instance,
                renderInterceptor.position,
                renderInterceptor.velocity,
                interceptorScale,
            )
            renderCollections.interceptors.add(renderInterceptor)
            renderCollections.interceptorsById.put(renderInterceptor.id, renderInterceptor)
            callbacks.pulseLauncher(launch, interceptorState.velocity)
            callbacks.spawnBlast(launch.launcherPosition, LAUNCH_BLAST_RADIUS)
            callbacks.playSfx("launch", LAUNCH_SFX_VOLUME)
        }
    }

    private fun applyTrailEvents(trails: List<TrailEvent>) {
        trails.forEach { trail ->
            if (callbacks.canSpawnTrail(trail.hostile)) {
                callbacks.spawnTrail(trail.position, trail.hostile)
            }
        }
    }

    private fun applyBlastEvents(blasts: List<BlastEvent>) {
        blasts.forEach { blast ->
            when (blast.kind) {
                BlastKind.HOSTILE_IMPACT -> {
                    callbacks.spawnBlast(blast.position, blast.radius)
                    callbacks.spawnDebris(blast.position, HOSTILE_IMPACT_DEBRIS_COUNT, HOSTILE_IMPACT_DEBRIS_COLOR)
                    callbacks.triggerShake(HOSTILE_IMPACT_SHAKE_INTENSITY, HOSTILE_IMPACT_SHAKE_DURATION)
                    callbacks.playSfx("impact", HOSTILE_IMPACT_SFX_VOLUME)
                }

                BlastKind.INTERCEPT -> {
                    callbacks.spawnBlast(blast.position, blast.radius)
                    callbacks.spawnDebris(blast.position, INTERCEPT_DEBRIS_COUNT, INTERCEPT_DEBRIS_COLOR)
                    callbacks.triggerShake(INTERCEPT_SHAKE_INTENSITY, INTERCEPT_SHAKE_DURATION)
                    callbacks.playSfx("detonate", INTERCEPT_SFX_VOLUME)
                }
            }
        }
    }

    private fun applyBuildingDamageEvents(damages: List<BuildingDamageEvent>) {
        damages.forEach { damage ->
            renderCollections.cityBlocksById[damage.buildingId]?.let { building ->
                callbacks.applyBuildingDamageVisual(building, damage.integrity, damage.epicenter)
            }
        }
    }

    private fun removeThreatEntities(ids: List<String>) {
        ids.forEach { id ->
            renderCollections.threatsById.remove(id)?.let { renderCollections.threats.removeValue(it, true) }
        }
    }

    private fun removeInterceptorEntities(ids: List<String>) {
        ids.forEach { id ->
            renderCollections.interceptorsById.remove(id)?.let { renderCollections.interceptors.removeValue(it, true) }
        }
    }

    private fun applyStepStatus(step: BattleStepEvents) {
        when {
            step.waveCleared -> {
                callbacks.setStatus("status", "SKY CLEAR // PREPARE NEXT WAVE")
                callbacks.syncHud()
            }

            step.gameOver -> {
                callbacks.setStatus("critical", "DEFENSE FAILED")
                callbacks.syncHud()
            }
        }
    }

    private companion object {
        private const val LAUNCH_BLAST_RADIUS = 14f
        private const val LAUNCH_SFX_VOLUME = 0.35f
        private const val HOSTILE_IMPACT_DEBRIS_COUNT = 24
        private val HOSTILE_IMPACT_DEBRIS_COLOR = Color(0.4f, 0.38f, 0.36f, 1f)
        private const val HOSTILE_IMPACT_SHAKE_INTENSITY = 26f
        private const val HOSTILE_IMPACT_SHAKE_DURATION = 0.55f
        private const val HOSTILE_IMPACT_SFX_VOLUME = 0.8f
        private const val INTERCEPT_DEBRIS_COUNT = 10
        private val INTERCEPT_DEBRIS_COLOR = Color(0.7f, 0.7f, 0.74f, 1f)
        private const val INTERCEPT_SHAKE_INTENSITY = 10f
        private const val INTERCEPT_SHAKE_DURATION = 0.3f
        private const val INTERCEPT_SFX_VOLUME = 0.5f
    }
}
