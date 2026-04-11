package com.airdefense.game

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3
import kotlin.math.abs
import kotlin.math.max

enum class DefenseDoctrine(
    val label: String,
    val summary: String,
    val launchCadenceMultiplier: Float,
    val launcherReloadMultiplier: Float,
    val engagementRangeBias: Float,
    val fuseRadiusBias: Float,
    val maxAssignmentsPerThreat: Int,
    val recommitWindowSeconds: Float,
) {
    DISCIPLINED(
        label = "DISCIPLINED",
        summary = "Single-shot coverage with more conservative battery spacing.",
        launchCadenceMultiplier = 1.18f,
        launcherReloadMultiplier = 1.14f,
        engagementRangeBias = -120f,
        fuseRadiusBias = -6f,
        maxAssignmentsPerThreat = 1,
        recommitWindowSeconds = 3.2f,
    ),
    ADAPTIVE(
        label = "ADAPTIVE",
        summary = "Balanced coverage with selective late-track reinforcement.",
        launchCadenceMultiplier = 1f,
        launcherReloadMultiplier = 1f,
        engagementRangeBias = 0f,
        fuseRadiusBias = 0f,
        maxAssignmentsPerThreat = 1,
        recommitWindowSeconds = 4.5f,
    ),
    SHIELD_WALL(
        label = "SHIELD WALL",
        summary = "Aggressive layered fire with critical-track double taps.",
        launchCadenceMultiplier = 0.82f,
        launcherReloadMultiplier = 0.84f,
        engagementRangeBias = 180f,
        fuseRadiusBias = 8f,
        maxAssignmentsPerThreat = 2,
        recommitWindowSeconds = 6.2f,
    ),
    ;

    fun next(): DefenseDoctrine = entries[(ordinal + 1) % entries.size]
}

object PhysicsModel {
    const val THREAT_GRAVITY_Y = -55f
    const val THREAT_IMPACT_RADIUS = 110f
    const val THREAT_FAILSAFE_Z = 1800f
}

object BattleBalance {
    fun threatsForWave(wave: Int): Int = 6 + wave * 3

    fun spawnIntervalForWave(wave: Int): Float = max(0.9f, 2.4f - wave * 0.08f)

    fun threatSpeedRangeForWave(wave: Int): ClosedFloatingPointRange<Float> = (220f + wave * 8f)..(285f + wave * 10f)
}

object DefenseTuning {
    fun engagementRange(settings: DefenseSettings): Float =
        (settings.engagementRange + settings.doctrine.engagementRangeBias).coerceIn(1200f, 3400f)

    fun launchCooldown(settings: DefenseSettings): Float =
        (settings.launchCooldown * settings.doctrine.launchCadenceMultiplier).coerceIn(0.16f, 1.4f)

    fun launcherReload(settings: DefenseSettings): Float = (0.44f * settings.doctrine.launcherReloadMultiplier).coerceIn(0.2f, 1.3f)

    fun blastRadius(settings: DefenseSettings): Float = (settings.blastRadius + settings.doctrine.fuseRadiusBias).coerceIn(56f, 128f)

    fun turnRate(settings: DefenseSettings): Float =
        when (settings.doctrine) {
            DefenseDoctrine.DISCIPLINED -> 3.8f
            DefenseDoctrine.ADAPTIVE -> 4.4f
            DefenseDoctrine.SHIELD_WALL -> 4.9f
        }
}

object InterceptionMath {
    fun predictInterceptPoint(
        interceptorPos: Vector3,
        targetPos: Vector3,
        targetVelocity: Vector3,
        interceptorSpeed: Float,
    ): Vector3 {
        val toTarget = targetPos.cpy().sub(interceptorPos)
        val a = targetVelocity.dot(targetVelocity) - interceptorSpeed * interceptorSpeed
        val b = 2f * toTarget.dot(targetVelocity)
        val c = toTarget.dot(toTarget)
        val time =
            if (abs(a) < 0.001f) {
                if (abs(b) < 0.001f) 0f else (-c / b).coerceAtLeast(0f)
            } else {
                val discriminant = b * b - 4f * a * c
                if (discriminant <= 0f) {
                    0f
                } else {
                    val sqrtDisc = kotlin.math.sqrt(discriminant)
                    val t1 = (-b - sqrtDisc) / (2f * a)
                    val t2 = (-b + sqrtDisc) / (2f * a)
                    listOf(t1, t2).filter { it > 0f }.minOrNull() ?: 0f
                }
            }.coerceIn(0f, 8f)
        return targetPos.cpy().mulAdd(targetVelocity, time)
    }
}

object DamageModel {
    fun computeBuildingDamage(
        buildingPosition: Vector3,
        buildingWidth: Float,
        buildingDepth: Float,
        impactPosition: Vector3,
        blastRadius: Float,
        hostile: Boolean,
    ): Float {
        val impactPoint = Vector3(impactPosition.x, 0f, impactPosition.z)
        val buildingPoint = Vector3(buildingPosition.x, 0f, buildingPosition.z)
        val distance = buildingPoint.dst(impactPoint)
        val effectiveRadius = blastRadius + max(buildingWidth, buildingDepth)
        if (distance >= effectiveRadius) return 0f
        val scale = if (hostile) 72f else 36f
        return (((effectiveRadius - distance) / effectiveRadius) * scale).coerceAtLeast(8f)
    }

    fun cityIntegrityLoss(
        hostile: Boolean,
        destroyedBuilding: Boolean,
    ): Float {
        var loss = if (hostile) 12f else 0f
        if (destroyedBuilding) loss += 6f
        return loss
    }
}

object ThreatFactory {
    fun createThreatLaunch(
        wave: Int,
        cityTarget: Vector3,
        random: RandomSource = DefaultRandomSource,
    ): ThreatLaunch {
        val start =
            Vector3(
                random.range(-1800f, 1800f),
                random.range(1500f, 2100f),
                -4200f,
            )
        val target = cityTarget.cpy().add(random.range(-80f, 80f), 0f, random.range(-80f, 80f))
        val horizontalSpeed =
            random.range(
                BattleBalance.threatSpeedRangeForWave(wave).start,
                BattleBalance.threatSpeedRangeForWave(wave).endInclusive,
            )
        val horizontalDistance = Vector3(target.x - start.x, 0f, target.z - start.z).len().coerceAtLeast(1f)
        val travelTime = (horizontalDistance / horizontalSpeed).coerceIn(7.5f, 13f)
        val displacement = target.cpy().sub(start)
        val velocity = displacement.scl(1f / travelTime)
        velocity.y -= 0.5f * PhysicsModel.THREAT_GRAVITY_Y * travelTime
        return ThreatLaunch(start, target, velocity)
    }
}

object EngagementPhysics {
    fun closesWithinFuse(
        interceptorPos: Vector3,
        interceptorVel: Vector3,
        targetPos: Vector3,
        targetVel: Vector3,
        dt: Float,
        fuseRadius: Float,
    ): Boolean {
        val relativePos = targetPos.cpy().sub(interceptorPos)
        val relativeVel = targetVel.cpy().sub(interceptorVel)
        val relativeSpeedSquared = relativeVel.len2()
        val sampleTime =
            if (relativeSpeedSquared <= 0.0001f) {
                0f
            } else {
                (-relativePos.dot(relativeVel) / relativeSpeedSquared).coerceIn(0f, dt)
            }
        return relativePos.mulAdd(relativeVel, sampleTime).len2() <= fuseRadius * fuseRadius
    }
}

object FireControl {
    fun selectNextThreat(
        threats: List<ThreatSnapshot>,
        engagementRange: Float,
        assignedThreatIds: Set<String> = emptySet(),
        assignmentCounts: Map<String, Int> = emptyMap(),
        doctrine: DefenseDoctrine = DefenseDoctrine.ADAPTIVE,
    ): String? {
        val engagementRangeSquared = engagementRange * engagementRange
        var bestThreat: ThreatSnapshot? = null
        var bestScore = Float.NEGATIVE_INFINITY
        threats
            .asSequence()
            .filter { it.position.len2() <= engagementRangeSquared }
            .forEach { threat ->
                val assignedCount = max(assignmentCounts[threat.id] ?: 0, if (threat.id in assignedThreatIds) 1 else 0)
                if (assignedCount >= doctrine.maxAssignmentsPerThreat) return@forEach

                val timeToImpact = estimateTimeToImpact(threat)
                if (assignedCount > 0 && timeToImpact > doctrine.recommitWindowSeconds) return@forEach

                val score = scoreThreat(threat, engagementRange, timeToImpact, assignedCount)
                val incumbent = bestThreat
                if (incumbent == null || score > bestScore ||
                    (score == bestScore && hasPriorityOver(threat.position, incumbent.position))
                ) {
                    bestThreat = threat
                    bestScore = score
                }
            }
        return bestThreat?.id
    }

    fun hasPriorityOver(
        candidate: Vector3,
        incumbent: Vector3,
    ): Boolean {
        if (candidate.z != incumbent.z) return candidate.z > incumbent.z
        if (candidate.y != incumbent.y) return candidate.y < incumbent.y
        return abs(candidate.x) < abs(incumbent.x)
    }

    private fun scoreThreat(
        threat: ThreatSnapshot,
        engagementRange: Float,
        timeToImpact: Float,
        assignedCount: Int,
    ): Float {
        val normalizedDistance = 1f - (threat.position.len() / engagementRange).coerceIn(0f, 1f)
        val centerlineBias = 1f - (abs(threat.position.x) / engagementRange).coerceIn(0f, 1f)
        val altitudeUrgency = 1f - (threat.position.y / 2400f).coerceIn(0f, 1f)
        val timeUrgency = (10f - timeToImpact).coerceIn(-1f, 10f)
        val terminalBonus = if (threat.position.z >= -260f) 1.4f else 0f
        return timeUrgency * 5.4f +
            normalizedDistance * 2.6f +
            centerlineBias * 1.6f +
            altitudeUrgency * 1.2f +
            terminalBonus -
            assignedCount * 3.4f
    }

    private fun estimateTimeToImpact(threat: ThreatSnapshot): Float {
        val toTarget = threat.targetPosition.cpy().sub(threat.position)
        val distance = toTarget.len().coerceAtLeast(1f)
        val closingSpeed =
            if (threat.velocity.isZero(0.001f)) {
                0f
            } else {
                max(0f, threat.velocity.dot(toTarget.nor()))
            }
        if (closingSpeed <= 0.001f) {
            val fallbackDistance = max(0f, abs(threat.position.z)) + max(0f, threat.position.y * 0.35f)
            return (fallbackDistance / 280f).coerceIn(0.4f, 30f)
        }
        return (distance / closingSpeed).coerceIn(0.25f, 30f)
    }
}

data class ThreatLaunch(
    val start: Vector3,
    val target: Vector3,
    val velocity: Vector3,
)

data class ThreatSnapshot(
    val id: String,
    val position: Vector3,
    val velocity: Vector3 = Vector3.Zero.cpy(),
    val targetPosition: Vector3 = Vector3.Zero.cpy(),
)

interface RandomSource {
    fun range(
        min: Float,
        max: Float,
    ): Float

    fun int(
        min: Int,
        max: Int,
    ): Int {
        if (max <= min) return min
        val sample = range(min.toFloat(), (max + 1).toFloat())
        return sample.toInt().coerceIn(min, max)
    }
}

object DefaultRandomSource : RandomSource {
    override fun range(
        min: Float,
        max: Float,
    ): Float = MathUtils.random(min, max)

    override fun int(
        min: Int,
        max: Int,
    ): Int = MathUtils.random(min, max)
}

class SeededRandomSource(
    seed: Long,
) : RandomSource {
    private val random = java.util.Random(seed)

    override fun range(
        min: Float,
        max: Float,
    ): Float {
        if (max <= min) return min
        return min + random.nextFloat() * (max - min)
    }

    override fun int(
        min: Int,
        max: Int,
    ): Int {
        if (max <= min) return min
        return min + random.nextInt(max - min + 1)
    }
}
