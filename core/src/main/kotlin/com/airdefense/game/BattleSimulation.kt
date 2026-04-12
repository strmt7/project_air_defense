package com.airdefense.game

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector3
import kotlin.math.max

data class DefenseSettings(
    var engagementRange: Float = 2825f,
    var interceptorSpeed: Float = 700f,
    var launchCooldown: Float = 0.3f,
    var blastRadius: Float = 82f,
    var doctrine: DefenseDoctrine = DefenseDoctrine.SHIELD_WALL,
)

data class SimulationBuilding(
    val id: String,
    val modelName: String,
    val position: Vector3,
    val yaw: Float,
    val baseHeight: Float,
    val width: Float,
    val depth: Float,
    var integrity: Float = 100f,
)

data class SimulationThreat(
    val id: String,
    val position: Vector3,
    val targetPosition: Vector3,
    val velocity: Vector3,
    var isTracked: Boolean = false,
    var trailCooldown: Float = 0f,
)

data class SimulationInterceptor(
    val id: String,
    val position: Vector3,
    val velocity: Vector3,
    var targetId: String?,
    val launcherIndex: Int,
    var trailCooldown: Float = 0f,
)

data class TrailEvent(
    val position: Vector3,
    val hostile: Boolean,
)

enum class BlastKind {
    HOSTILE_IMPACT,
    INTERCEPT,
}

data class BlastEvent(
    val position: Vector3,
    val radius: Float,
    val kind: BlastKind,
)

data class BuildingDamageEvent(
    val buildingId: String,
    val integrity: Float,
    val epicenter: Vector3,
)

data class InterceptorLaunchEvent(
    val interceptorId: String,
    val launcherIndex: Int,
    val launcherPosition: Vector3,
)

data class BattleStepEvents(
    val spawnedThreatIds: List<String> = emptyList(),
    val removedThreatIds: List<String> = emptyList(),
    val launchedInterceptors: List<InterceptorLaunchEvent> = emptyList(),
    val removedInterceptorIds: List<String> = emptyList(),
    val trailEvents: List<TrailEvent> = emptyList(),
    val blastEvents: List<BlastEvent> = emptyList(),
    val buildingDamageEvents: List<BuildingDamageEvent> = emptyList(),
    val waveCleared: Boolean = false,
    val gameOver: Boolean = false,
)

data class BattleRunSummary(
    val runIndex: Int,
    val wavesCompleted: Int,
    val cityIntegrity: Float,
    val gameOver: Boolean,
    val score: Int,
    val threatsSpawned: Int,
    val threatsIntercepted: Int,
    val hostileImpacts: Int,
    val interceptorsLaunched: Int,
    val destroyedBuildings: Int,
    val simulatedSeconds: Float,
) {
    val interceptRate: Float
        get() = if (threatsSpawned == 0) 0f else threatsIntercepted.toFloat() / threatsSpawned
}

class BattleSimulation(
    buildingDefinitions: List<BattleBuildingDefinition>,
    launcherPositions: List<Vector3>,
    val settings: DefenseSettings = DefenseSettings(),
    private val random: RandomSource = DefaultRandomSource,
) {
    private companion object {
        const val THREAT_TRAIL_INTERVAL = 0.14f
        const val INTERCEPTOR_TRAIL_INTERVAL = 0.05f
        const val STARTING_CREDITS = 10000
        const val STARTING_CITY_INTEGRITY = 100f
        const val WAVE_START_DELAY = 0.4f
        const val WAVE_CLEAR_CREDIT_BONUS = 1800
        const val THREAT_TERMINAL_ALTITUDE = 90f
        const val TRACKING_EPSILON = 0.001f
        const val INTERCEPT_SCORE_BONUS = 150
        const val INTERCEPT_CREDIT_BONUS = 180
        const val INTERCEPT_BLAST_SCALE = 2.2f
        const val INTERCEPTOR_MAX_ALTITUDE = 4400f
        const val INTERCEPTOR_MAX_RANGE = 7000f
        const val ENTITY_ID_PADDING = 4
        const val LAUNCHER_MUZZLE_HEIGHT = 18f
        const val BLAST_VISUAL_RADIUS_SCALE = 1.2f
        const val MIN_BUILDING_DAMAGE = 8f
    }

    private class StepAccumulator {
        val spawnedThreatIds = mutableListOf<String>()
        val removedThreatIds = mutableListOf<String>()
        val launchedInterceptors = mutableListOf<InterceptorLaunchEvent>()
        val removedInterceptorIds = mutableListOf<String>()
        val trailEvents = mutableListOf<TrailEvent>()
        val blastEvents = mutableListOf<BlastEvent>()
        val buildingDamageEvents = mutableListOf<BuildingDamageEvent>()
        var waveCleared = false
        var gameOverTriggered = false

        fun toEvents(): BattleStepEvents =
            BattleStepEvents(
                spawnedThreatIds = spawnedThreatIds,
                removedThreatIds = removedThreatIds.distinct(),
                launchedInterceptors = launchedInterceptors,
                removedInterceptorIds = removedInterceptorIds.distinct(),
                trailEvents = trailEvents,
                blastEvents = blastEvents,
                buildingDamageEvents = buildingDamageEvents,
                waveCleared = waveCleared,
                gameOver = gameOverTriggered,
            )
    }

    private data class InterceptorStepContext(
        val dt: Float,
        val effectiveBlastRadius: Float,
        val events: StepAccumulator,
    )

    private inner class StepFlowController {
        fun updateWaveState(
            dt: Float,
            events: StepAccumulator,
        ) {
            if (!waveInProgress) return

            spawnTimer -= dt
            if (spawnTimer <= 0f && threatsRemainingInWave > 0) {
                val threat = spawnThreat()
                events.spawnedThreatIds += threat.id
                threatsRemainingInWave--
                spawnTimer = BattleBalance.spawnIntervalForWave(wave)
            }
            if (threatsRemainingInWave == 0 && threats.isEmpty()) {
                waveInProgress = false
                wave++
                credits += WAVE_CLEAR_CREDIT_BONUS
                events.waveCleared = true
            }
        }

        fun updateLauncherReloads(dt: Float) {
            launcherReadyIn.indices.forEach { index ->
                launcherReadyIn[index] = max(0f, launcherReadyIn[index] - dt)
            }
        }

        fun launchInterceptorIfReady(
            dt: Float,
            events: StepAccumulator,
        ) {
            timeSinceLastLaunch += dt
            if (timeSinceLastLaunch >= DefenseTuning.launchCooldown(settings)) {
                val nextTarget = selectNextThreat()
                if (nextTarget != null) {
                    val launch = launchInterceptor(nextTarget)
                    if (launch != null) {
                        events.launchedInterceptors += launch
                        timeSinceLastLaunch = 0f
                    }
                }
            }
        }
    }

    private inner class ThreatStepper {
        fun update(
            dt: Float,
            events: StepAccumulator,
        ) {
            val engagementRangeSquared = DefenseTuning.engagementRange(settings).let { it * it }
            val threatsIterator = threats.iterator()
            while (threatsIterator.hasNext()) {
                val threat = threatsIterator.next()
                advance(threat, dt)
                emitTrailIfReady(threat, dt, events)
                threat.isTracked = isTracked(threat, engagementRangeSquared)

                val impactPoint = impactPointFor(threat) ?: continue
                resolveImpact(
                    position = impactPoint,
                    radius = PhysicsModel.THREAT_IMPACT_RADIUS,
                    hostile = true,
                    blastEvents = events.blastEvents,
                    buildingDamageEvents = events.buildingDamageEvents,
                )
                totalHostileImpacts++
                events.removedThreatIds += threat.id
                threatsIterator.remove()
            }
        }

        private fun advance(
            threat: SimulationThreat,
            dt: Float,
        ) {
            threat.velocity.mulAdd(gravity, dt)
            threat.position.mulAdd(threat.velocity, dt)
        }

        private fun emitTrailIfReady(
            threat: SimulationThreat,
            dt: Float,
            events: StepAccumulator,
        ) {
            threat.trailCooldown -= dt
            if (threat.trailCooldown > 0f) return
            events.trailEvents += TrailEvent(threat.position.cpy(), hostile = true)
            threat.trailCooldown = THREAT_TRAIL_INTERVAL
        }

        private fun isTracked(
            threat: SimulationThreat,
            engagementRangeSquared: Float,
        ): Boolean = threat.position.dst2(defenseOrigin) < engagementRangeSquared

        private fun impactPointFor(threat: SimulationThreat): Vector3? {
            val reachedTarget =
                threat.position.dst2(threat.targetPosition) <=
                    PhysicsModel.THREAT_IMPACT_RADIUS * PhysicsModel.THREAT_IMPACT_RADIUS
            return when {
                reachedTarget && threat.position.y <= THREAT_TERMINAL_ALTITUDE -> threat.targetPosition.cpy()
                threat.position.y <= 0f -> Vector3(threat.position.x, 0f, threat.position.z)
                threat.position.z >= PhysicsModel.THREAT_FAILSAFE_Z ->
                    Vector3(threat.position.x, threat.position.y.coerceAtLeast(0f), threat.position.z)
                else -> null
            }
        }
    }

    private inner class InterceptorStepper {
        fun update(
            dt: Float,
            effectiveBlastRadius: Float,
            events: StepAccumulator,
        ) {
            val context = InterceptorStepContext(dt, effectiveBlastRadius, events)
            val interceptorIterator = interceptors.iterator()
            while (interceptorIterator.hasNext()) {
                val interceptor = interceptorIterator.next()
                val target = interceptor.targetId?.let(::findThreat)

                when {
                    target == null -> remove(interceptorIterator, interceptor.id, context.events)
                    else -> processTrackedInterceptor(interceptorIterator, interceptor, target, context)
                }
            }
        }

        private fun processTrackedInterceptor(
            interceptorIterator: MutableIterator<SimulationInterceptor>,
            interceptor: SimulationInterceptor,
            target: SimulationThreat,
            context: InterceptorStepContext,
        ) {
            advance(interceptor, target, context.dt)
            emitTrailIfReady(interceptor, context.dt, context.events)

            val intercepted = tryResolveIntercept(interceptorIterator, interceptor, target, context)
            if (!intercepted && isOutOfBounds(interceptor)) {
                remove(interceptorIterator, interceptor.id, context.events)
            }
        }

        private fun advance(
            interceptor: SimulationInterceptor,
            target: SimulationThreat,
            dt: Float,
        ) {
            val aimPoint =
                InterceptionMath.predictInterceptPoint(
                    interceptorPos = interceptor.position,
                    targetPos = target.position,
                    targetVelocity = target.velocity,
                    interceptorSpeed = settings.interceptorSpeed,
                )
            val desiredVelocity = aimPoint.cpy().sub(interceptor.position)
            if (!desiredVelocity.isZero(TRACKING_EPSILON)) {
                desiredVelocity.nor().scl(settings.interceptorSpeed)
                interceptor.velocity.lerp(desiredVelocity, (dt * DefenseTuning.turnRate(settings)).coerceAtMost(1f))
            }
            interceptor.velocity.nor().scl(settings.interceptorSpeed)
            interceptor.position.mulAdd(interceptor.velocity, dt)
        }

        private fun emitTrailIfReady(
            interceptor: SimulationInterceptor,
            dt: Float,
            events: StepAccumulator,
        ) {
            interceptor.trailCooldown -= dt
            if (interceptor.trailCooldown > 0f) return
            events.trailEvents += TrailEvent(interceptor.position.cpy(), hostile = false)
            interceptor.trailCooldown = INTERCEPTOR_TRAIL_INTERVAL
        }

        private fun tryResolveIntercept(
            interceptorIterator: MutableIterator<SimulationInterceptor>,
            interceptor: SimulationInterceptor,
            target: SimulationThreat,
            context: InterceptorStepContext,
        ): Boolean {
            val closesWithinFuse =
                EngagementPhysics.closesWithinFuse(
                    interceptorPos = interceptor.position,
                    interceptorVel = interceptor.velocity,
                    targetPos = target.position,
                    targetVel = target.velocity,
                    dt = context.dt,
                    fuseRadius = context.effectiveBlastRadius,
                )
            if (!closesWithinFuse) return false

            score += INTERCEPT_SCORE_BONUS
            credits += INTERCEPT_CREDIT_BONUS
            totalThreatsIntercepted++
            context.events.blastEvents +=
                BlastEvent(
                    interceptor.position.cpy(),
                    context.effectiveBlastRadius * INTERCEPT_BLAST_SCALE,
                    BlastKind.INTERCEPT,
                )
            context.events.removedThreatIds += target.id
            context.events.removedInterceptorIds += interceptor.id
            threats.remove(target)
            interceptorIterator.remove()
            return true
        }

        private fun isOutOfBounds(interceptor: SimulationInterceptor): Boolean =
            interceptor.position.y > INTERCEPTOR_MAX_ALTITUDE ||
                interceptor.position.dst2(defenseOrigin) > INTERCEPTOR_MAX_RANGE * INTERCEPTOR_MAX_RANGE

        private fun remove(
            interceptorIterator: MutableIterator<SimulationInterceptor>,
            interceptorId: String,
            events: StepAccumulator,
        ) {
            events.removedInterceptorIds += interceptorId
            interceptorIterator.remove()
        }
    }

    private val gravity = Vector3(0f, PhysicsModel.THREAT_GRAVITY_Y, 0f)
    private val launcherPads = launcherPositions.map { it.cpy() }
    private val launcherReadyIn = MutableList(launcherPads.size.coerceAtLeast(1)) { 0f }
    private val stepFlowController = StepFlowController()
    private val threatStepper = ThreatStepper()
    private val interceptorStepper = InterceptorStepper()
    private val defenseOrigin =
        if (launcherPads.isEmpty()) {
            BattleWorldLayout.defenseOrigin.cpy()
        } else {
            Vector3().also { average ->
                launcherPads.forEach { average.add(it.x, 0f, it.z) }
                average.scl(1f / launcherPads.size)
            }
        }

    val buildings =
        buildingDefinitions
            .map {
                SimulationBuilding(
                    id = it.id,
                    modelName = it.modelName,
                    position = it.position.cpy(),
                    yaw = it.yaw,
                    baseHeight = it.metrics.baseHeight,
                    width = it.metrics.width,
                    depth = it.metrics.depth,
                )
            }.toMutableList()

    val threats = mutableListOf<SimulationThreat>()
    val interceptors = mutableListOf<SimulationInterceptor>()

    var credits = STARTING_CREDITS
        private set
    var wave = 1
        private set
    var score = 0
        private set
    var cityIntegrity = STARTING_CITY_INTEGRITY
        private set
    var waveInProgress = false
        private set
    var isGameOver = false
        private set
    var threatsRemainingInWave = 0
        private set

    var totalThreatsSpawned = 0
        private set
    var totalThreatsIntercepted = 0
        private set
    var totalHostileImpacts = 0
        private set
    var totalInterceptorsLaunched = 0
        private set
    var totalDestroyedBuildings = 0
        private set

    private var spawnTimer = 0f
    private var timeSinceLastLaunch = 0f
    private var nextThreatOrdinal = 1
    private var nextInterceptorOrdinal = 1

    fun startNewWave(): Boolean {
        if (waveInProgress || isGameOver) return false
        waveInProgress = true
        threatsRemainingInWave = BattleBalance.threatsForWave(wave)
        spawnTimer = WAVE_START_DELAY
        timeSinceLastLaunch = DefenseTuning.launchCooldown(settings)
        return true
    }

    fun step(dt: Float): BattleStepEvents {
        if (isGameOver) return BattleStepEvents(gameOver = true)

        val events = StepAccumulator()
        val effectiveBlastRadius = DefenseTuning.blastRadius(settings)

        stepFlowController.updateWaveState(dt, events)
        stepFlowController.updateLauncherReloads(dt)
        threatStepper.update(dt, events)
        interceptorStepper.update(dt, effectiveBlastRadius, events)
        stepFlowController.launchInterceptorIfReady(dt, events)

        if (cityIntegrity <= 0f) {
            isGameOver = true
            events.gameOverTriggered = true
        }

        return events.toEvents()
    }

    fun findThreat(id: String): SimulationThreat? = threats.firstOrNull { it.id == id }

    fun findInterceptor(id: String): SimulationInterceptor? = interceptors.firstOrNull { it.id == id }

    fun snapshot(
        runIndex: Int,
        simulatedSeconds: Float,
    ): BattleRunSummary =
        BattleRunSummary(
            runIndex = runIndex,
            wavesCompleted = if (waveInProgress) wave - 1 else wave - 1,
            cityIntegrity = cityIntegrity,
            gameOver = isGameOver,
            score = score,
            threatsSpawned = totalThreatsSpawned,
            threatsIntercepted = totalThreatsIntercepted,
            hostileImpacts = totalHostileImpacts,
            interceptorsLaunched = totalInterceptorsLaunched,
            destroyedBuildings = totalDestroyedBuildings,
            simulatedSeconds = simulatedSeconds,
        )

    private fun spawnThreat(): SimulationThreat {
        val viableTargets = buildings.filter { it.integrity > 0f }
        val targetBuilding = viableTargets[random.int(0, viableTargets.lastIndex)]
        val launch = ThreatFactory.createThreatLaunch(wave, targetBuilding.position, random)
        val threat =
            SimulationThreat(
                id = "T-${nextThreatOrdinal++.toString().padStart(ENTITY_ID_PADDING, '0')}",
                position = launch.start.cpy(),
                targetPosition = launch.target.cpy(),
                velocity = launch.velocity.cpy(),
                trailCooldown = random.range(0f, THREAT_TRAIL_INTERVAL),
            )
        threats += threat
        totalThreatsSpawned++
        return threat
    }

    private fun selectNextThreat(): SimulationThreat? {
        val assignmentCounts =
            interceptors
                .mapNotNull { it.targetId }
                .groupingBy { it }
                .eachCount()
        val selected =
            FireControl.selectNextThreat(
                threats =
                    threats.map {
                        ThreatSnapshot(
                            id = it.id,
                            position = it.position.cpy(),
                            velocity = it.velocity.cpy(),
                            targetPosition = it.targetPosition.cpy(),
                        )
                    },
                engagementRange = DefenseTuning.engagementRange(settings),
                assignmentCounts = assignmentCounts,
                doctrine = settings.doctrine,
            ) ?: return null
        return findThreat(selected)
    }

    private fun launchInterceptor(target: SimulationThreat): InterceptorLaunchEvent? {
        val availableLaunchers = launcherPads.indices.filter { launcherReadyIn.getOrElse(it) { 0f } <= 0f }
        if (availableLaunchers.isEmpty()) return null
        val launcherIndex =
            availableLaunchers.minByOrNull { index ->
                launcherPads[index].dst2(target.position)
            } ?: 0
        val launchPosition = launcherPads[launcherIndex].cpy().add(0f, LAUNCHER_MUZZLE_HEIGHT, 0f)
        val aimPoint =
            InterceptionMath.predictInterceptPoint(
                interceptorPos = launchPosition,
                targetPos = target.position,
                targetVelocity = target.velocity,
                interceptorSpeed = settings.interceptorSpeed,
            )
        val velocity = aimPoint.sub(launchPosition.cpy()).nor().scl(settings.interceptorSpeed)
        val interceptor =
            SimulationInterceptor(
                id = "I-${nextInterceptorOrdinal++.toString().padStart(ENTITY_ID_PADDING, '0')}",
                position = launchPosition.cpy(),
                velocity = velocity.cpy(),
                targetId = target.id,
                launcherIndex = launcherIndex,
                trailCooldown = 0f,
            )
        interceptors += interceptor
        totalInterceptorsLaunched++
        launcherReadyIn[launcherIndex] = DefenseTuning.launcherReload(settings)
        return InterceptorLaunchEvent(
            interceptorId = interceptor.id,
            launcherIndex = launcherIndex,
            launcherPosition = launchPosition,
        )
    }

    private fun resolveImpact(
        position: Vector3,
        radius: Float,
        hostile: Boolean,
        blastEvents: MutableList<BlastEvent>,
        buildingDamageEvents: MutableList<BuildingDamageEvent>,
    ) {
        blastEvents +=
            BlastEvent(
                position.cpy(),
                radius * BLAST_VISUAL_RADIUS_SCALE,
                if (hostile) BlastKind.HOSTILE_IMPACT else BlastKind.INTERCEPT,
            )

        buildings.forEach { building ->
            val damage =
                DamageModel.computeBuildingDamage(
                    buildingPosition = building.position,
                    buildingWidth = building.width,
                    buildingDepth = building.depth,
                    impactPosition = position,
                    blastRadius = radius,
                    hostile = hostile,
                )
            if (damage <= 0f) return@forEach
            applyBuildingImpact(building, damage, position, buildingDamageEvents)
        }

        if (hostile) {
            cityIntegrity =
                max(
                    0f,
                    cityIntegrity - DamageModel.cityIntegrityLoss(hostile = true, destroyedBuilding = false),
                )
        }
    }

    private fun applyBuildingImpact(
        building: SimulationBuilding,
        damage: Float,
        impactPosition: Vector3,
        buildingDamageEvents: MutableList<BuildingDamageEvent>,
    ) {
        val before = building.integrity
        building.integrity = max(0f, building.integrity - damage.coerceAtLeast(MIN_BUILDING_DAMAGE))
        if (building.integrity == before) return

        buildingDamageEvents += BuildingDamageEvent(building.id, building.integrity, impactPosition.cpy())
        if (before > 0f && building.integrity <= 0f) {
            totalDestroyedBuildings++
            cityIntegrity =
                max(
                    0f,
                    cityIntegrity - DamageModel.cityIntegrityLoss(hostile = false, destroyedBuilding = true),
                )
        }
    }
}
