package com.airdefense.game

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector3
import kotlin.math.max

data class DefenseSettings(
    var engagementRange: Float = 2825f,
    var interceptorSpeed: Float = 700f,
    var launchCooldown: Float = 0.3f,
    var blastRadius: Float = 82f,
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
    }

    private val gravity = Vector3(0f, PhysicsModel.THREAT_GRAVITY_Y, 0f)
    private val launcherPads = launcherPositions.map { it.cpy() }
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

    var credits = 10000
        private set
    var wave = 1
        private set
    var score = 0
        private set
    var cityIntegrity = 100f
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
        spawnTimer = 0.4f
        timeSinceLastLaunch = settings.launchCooldown
        return true
    }

    fun step(dt: Float): BattleStepEvents {
        if (isGameOver) return BattleStepEvents(gameOver = true)

        val spawnedThreatIds = mutableListOf<String>()
        val removedThreatIds = mutableListOf<String>()
        val launchedInterceptors = mutableListOf<InterceptorLaunchEvent>()
        val removedInterceptorIds = mutableListOf<String>()
        val trailEvents = mutableListOf<TrailEvent>()
        val blastEvents = mutableListOf<BlastEvent>()
        val buildingDamageEvents = mutableListOf<BuildingDamageEvent>()
        var waveCleared = false
        var gameOverTriggered = false

        if (waveInProgress) {
            spawnTimer -= dt
            if (spawnTimer <= 0f && threatsRemainingInWave > 0) {
                val threat = spawnThreat()
                spawnedThreatIds += threat.id
                threatsRemainingInWave--
                spawnTimer = BattleBalance.spawnIntervalForWave(wave)
            }
            if (threatsRemainingInWave == 0 && threats.isEmpty()) {
                waveInProgress = false
                wave++
                credits += 1800
                waveCleared = true
            }
        }

        val threatsIterator = threats.iterator()
        while (threatsIterator.hasNext()) {
            val threat = threatsIterator.next()
            threat.velocity.mulAdd(gravity, dt)
            threat.position.mulAdd(threat.velocity, dt)

            threat.trailCooldown -= dt
            if (threat.trailCooldown <= 0f) {
                trailEvents += TrailEvent(threat.position.cpy(), hostile = true)
                threat.trailCooldown = THREAT_TRAIL_INTERVAL
            }

            threat.isTracked = threat.position.dst2(defenseOrigin) < settings.engagementRange * settings.engagementRange

            val reachedTarget =
                threat.position.dst2(threat.targetPosition) <= PhysicsModel.THREAT_IMPACT_RADIUS * PhysicsModel.THREAT_IMPACT_RADIUS
            val hitGround = threat.position.y <= 0f
            val leftBattlespace = threat.position.z >= PhysicsModel.THREAT_FAILSAFE_Z
            if ((reachedTarget && threat.position.y <= 90f) || hitGround || leftBattlespace) {
                val impactPoint =
                    when {
                        reachedTarget -> threat.targetPosition.cpy()
                        hitGround -> Vector3(threat.position.x, 0f, threat.position.z)
                        else -> Vector3(threat.position.x, threat.position.y.coerceAtLeast(0f), threat.position.z)
                    }
                resolveImpact(
                    position = impactPoint,
                    radius = 85f,
                    hostile = true,
                    blastEvents = blastEvents,
                    buildingDamageEvents = buildingDamageEvents,
                )
                totalHostileImpacts++
                removedThreatIds += threat.id
                threatsIterator.remove()
            }
        }

        val interceptorIterator = interceptors.iterator()
        while (interceptorIterator.hasNext()) {
            val interceptor = interceptorIterator.next()
            val target = interceptor.targetId?.let(::findThreat)
            if (target == null) {
                removedInterceptorIds += interceptor.id
                interceptorIterator.remove()
                continue
            }

            val aimPoint =
                InterceptionMath.predictInterceptPoint(
                    interceptorPos = interceptor.position,
                    targetPos = target.position,
                    targetVelocity = target.velocity,
                    interceptorSpeed = settings.interceptorSpeed,
                )
            val desiredVelocity = aimPoint.cpy().sub(interceptor.position)
            if (!desiredVelocity.isZero(0.001f)) {
                desiredVelocity.nor().scl(settings.interceptorSpeed)
                interceptor.velocity.lerp(desiredVelocity, (dt * 4.2f).coerceAtMost(1f))
            }
            interceptor.velocity.nor().scl(settings.interceptorSpeed)
            interceptor.position.mulAdd(interceptor.velocity, dt)

            interceptor.trailCooldown -= dt
            if (interceptor.trailCooldown <= 0f) {
                trailEvents += TrailEvent(interceptor.position.cpy(), hostile = false)
                interceptor.trailCooldown = INTERCEPTOR_TRAIL_INTERVAL
            }

            if (
                EngagementPhysics.closesWithinFuse(
                    interceptorPos = interceptor.position,
                    interceptorVel = interceptor.velocity,
                    targetPos = target.position,
                    targetVel = target.velocity,
                    dt = dt,
                    fuseRadius = settings.blastRadius,
                )
            ) {
                score += 150
                credits += 180
                totalThreatsIntercepted++
                blastEvents += BlastEvent(interceptor.position.cpy(), settings.blastRadius * 2.2f, BlastKind.INTERCEPT)
                removedThreatIds += target.id
                removedInterceptorIds += interceptor.id
                threats.remove(target)
                interceptorIterator.remove()
                continue
            }

            if (interceptor.position.y > 4400f || interceptor.position.dst2(defenseOrigin) > 7000f * 7000f) {
                removedInterceptorIds += interceptor.id
                interceptorIterator.remove()
            }
        }

        timeSinceLastLaunch += dt
        if (timeSinceLastLaunch >= settings.launchCooldown) {
            val nextTarget = selectNextThreat()
            if (nextTarget != null) {
                val launch = launchInterceptor(nextTarget)
                launchedInterceptors += launch
                timeSinceLastLaunch = 0f
            }
        }

        if (cityIntegrity <= 0f) {
            isGameOver = true
            gameOverTriggered = true
        }

        return BattleStepEvents(
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

    fun findThreat(id: String): SimulationThreat? = threats.firstOrNull { it.id == id }

    fun findInterceptor(id: String): SimulationInterceptor? = interceptors.firstOrNull { it.id == id }

    fun findBuilding(id: String): SimulationBuilding? = buildings.firstOrNull { it.id == id }

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
                id = "T-${nextThreatOrdinal++.toString().padStart(4, '0')}",
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
        val assignedThreatIds = interceptors.mapNotNull { it.targetId }.toSet()
        val selected =
            FireControl.selectNextThreat(
                threats = threats.map { ThreatSnapshot(it.id, it.position.cpy()) },
                engagementRange = settings.engagementRange,
                assignedThreatIds = assignedThreatIds,
            ) ?: return null
        return findThreat(selected)
    }

    private fun launchInterceptor(target: SimulationThreat): InterceptorLaunchEvent {
        val launcherIndex =
            launcherPads.indices.minByOrNull { index ->
                launcherPads[index].dst2(target.position)
            } ?: 0
        val launchPosition = launcherPads[launcherIndex].cpy().add(0f, 18f, 0f)
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
                id = "I-${nextInterceptorOrdinal++.toString().padStart(4, '0')}",
                position = launchPosition.cpy(),
                velocity = velocity.cpy(),
                targetId = target.id,
                launcherIndex = launcherIndex,
                trailCooldown = 0f,
            )
        interceptors += interceptor
        totalInterceptorsLaunched++
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
        blastEvents += BlastEvent(position.cpy(), radius * 1.2f, if (hostile) BlastKind.HOSTILE_IMPACT else BlastKind.INTERCEPT)

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
            if (damage > 0f) {
                val before = building.integrity
                building.integrity = max(0f, building.integrity - damage.coerceAtLeast(8f))
                if (building.integrity != before) {
                    buildingDamageEvents += BuildingDamageEvent(building.id, building.integrity, position.cpy())
                    if (before > 0f && building.integrity <= 0f) {
                        totalDestroyedBuildings++
                        cityIntegrity = max(0f, cityIntegrity - DamageModel.cityIntegrityLoss(hostile = false, destroyedBuilding = true))
                    }
                }
            }
        }

        if (hostile) {
            cityIntegrity = max(0f, cityIntegrity - DamageModel.cityIntegrityLoss(hostile = true, destroyedBuilding = false))
        }
    }
}
