package com.airdefense.game

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

data class DefenseDoctrineProfile(
    val launchCadenceMultiplier: Float,
    val launcherReloadMultiplier: Float,
    val engagementRangeBias: Float,
    val fuseRadiusBias: Float,
    val maxAssignmentsPerThreat: Int,
    val recommitWindowSeconds: Float,
    val turnRate: Float,
)

private data class LinearGrowth(
    val base: Float,
    val stepPerWave: Float,
)

private data class ScalarClamp(
    val min: Float,
    val max: Float,
)

private data class ThreatSpeedBand(
    val minimum: Float,
    val maximum: Float,
)

data class BuildingFootprint(
    val position: Vector3,
    val width: Float,
    val depth: Float,
)

data class BlastImpact(
    val position: Vector3,
    val radius: Float,
    val hostile: Boolean,
)

data class MotionSample(
    val position: Vector3,
    val velocity: Vector3,
)

private data class ThreatAssessmentWeights(
    val timeUrgency: Float,
    val normalizedDistance: Float,
    val centerlineBias: Float,
    val altitudeUrgency: Float,
    val terminalBonus: Float,
    val assignmentPenalty: Float,
)

private object DefenseDoctrineProfiles {
    val disciplined =
        DefenseDoctrineProfile(
            launchCadenceMultiplier = 1.18f,
            launcherReloadMultiplier = 1.14f,
            engagementRangeBias = -120f,
            fuseRadiusBias = -6f,
            maxAssignmentsPerThreat = 1,
            recommitWindowSeconds = 3.2f,
            turnRate = 3.8f,
        )

    val adaptive =
        DefenseDoctrineProfile(
            launchCadenceMultiplier = 1f,
            launcherReloadMultiplier = 1f,
            engagementRangeBias = 0f,
            fuseRadiusBias = 0f,
            maxAssignmentsPerThreat = 1,
            recommitWindowSeconds = 4.5f,
            turnRate = 4.4f,
        )

    val shieldWall =
        DefenseDoctrineProfile(
            launchCadenceMultiplier = 0.82f,
            launcherReloadMultiplier = 0.84f,
            engagementRangeBias = 180f,
            fuseRadiusBias = 8f,
            maxAssignmentsPerThreat = 2,
            recommitWindowSeconds = 6.2f,
            turnRate = 4.9f,
        )
}

private object BattleBalanceTuning {
    const val BASE_THREAT_COUNT = 6
    const val ADDED_THREATS_PER_WAVE = 3

    const val MINIMUM_SPAWN_INTERVAL = 0.9f
    private val SPAWN_INTERVAL_GROWTH =
        LinearGrowth(
            base = 2.4f,
            stepPerWave = -0.08f,
        )

    private val THREAT_SPEED_MINIMUM_GROWTH =
        LinearGrowth(
            base = 220f,
            stepPerWave = 8f,
        )
    private val THREAT_SPEED_MAXIMUM_GROWTH =
        LinearGrowth(
            base = 285f,
            stepPerWave = 10f,
        )

    fun spawnIntervalForWave(wave: Int): Float = max(MINIMUM_SPAWN_INTERVAL, SPAWN_INTERVAL_GROWTH.valueAt(wave))

    fun threatSpeedBandForWave(wave: Int): ThreatSpeedBand =
        ThreatSpeedBand(
            minimum = THREAT_SPEED_MINIMUM_GROWTH.valueAt(wave),
            maximum = THREAT_SPEED_MAXIMUM_GROWTH.valueAt(wave),
        )
}

private object DefenseTuningLimits {
    val ENGAGEMENT_RANGE =
        ScalarClamp(
            min = 1200f,
            max = 3400f,
        )
    val LAUNCH_COOLDOWN =
        ScalarClamp(
            min = 0.16f,
            max = 1.4f,
        )
    val LAUNCHER_RELOAD =
        ScalarClamp(
            min = 0.2f,
            max = 1.3f,
        )
    val BLAST_RADIUS =
        ScalarClamp(
            min = 56f,
            max = 128f,
        )

    const val BASE_LAUNCHER_RELOAD = 0.44f
}

private object InterceptionTuning {
    const val NEAR_ZERO_EPSILON = 0.001f
    const val INTERCEPT_FALLBACK_SECONDS = 0f
    const val MAX_INTERCEPT_LEAD_SECONDS = 8f
    const val QUADRATIC_FACTOR = 2f
    const val DISCRIMINANT_FACTOR = 4f
    const val MINIMUM_VALID_INTERCEPT_TIME = 0f
}

private object DamageTuning {
    const val HOSTILE_DAMAGE_SCALE = 72f
    const val DEFENSIVE_DAMAGE_SCALE = 36f
    const val MINIMUM_DAMAGE = 8f
    const val HOSTILE_CITY_INTEGRITY_LOSS = 12f
    const val COLLAPSE_INTEGRITY_LOSS = 6f
    const val GROUND_PLANE_Y = 0f
}

private object ThreatLaunchTuning {
    const val SPAWN_DEPTH = -4200f
    const val MINIMUM_TRAVEL_TIME = 7.5f
    const val MAXIMUM_TRAVEL_TIME = 13f
    const val MINIMUM_HORIZONTAL_DISTANCE = 1f
    const val HORIZONTAL_VELOCITY_SCALE = 1f
    const val GRAVITY_COMPENSATION_FACTOR = 0.5f
    const val CITY_TARGET_OFFSET_Y = 0f

    val SPAWN_X_RANGE =
        ScalarClamp(
            min = -1800f,
            max = 1800f,
        )
    val SPAWN_ALTITUDE_RANGE =
        ScalarClamp(
            min = 1500f,
            max = 2100f,
        )
    val CITY_TARGET_OFFSET_RANGE =
        ScalarClamp(
            min = -80f,
            max = 80f,
        )
}

private object EngagementTuning {
    const val RELATIVE_SPEED_EPSILON = 0.0001f
    const val STATIONARY_SAMPLE_TIME = 0f
}

private object ThreatAssessmentTuning {
    private val WEIGHTS =
        ThreatAssessmentWeights(
            timeUrgency = 5.4f,
            normalizedDistance = 2.6f,
            centerlineBias = 1.6f,
            altitudeUrgency = 1.2f,
            terminalBonus = 1.4f,
            assignmentPenalty = 3.4f,
        )

    const val MINIMUM_NORMALIZED_THREAT_SCORE = 0f
    const val MAXIMUM_NORMALIZED_THREAT_SCORE = 1f
    const val ALTITUDE_REFERENCE = 2400f
    const val TERMINAL_TRACK_THRESHOLD_Z = -260f
    const val TERMINAL_TRACK_BONUS = 0f
    const val TIME_URGENCY_REFERENCE = 10f
    const val MINIMUM_TIME_URGENCY = -1f
    const val MAXIMUM_TIME_URGENCY = 10f
    const val ZERO_VELOCITY_EPSILON = 0.001f
    const val FALLBACK_VERTICAL_WEIGHT = 0.35f
    const val FALLBACK_CLOSING_SPEED = 280f
    const val MINIMUM_FALLBACK_TIME_TO_IMPACT = 0.4f
    const val MINIMUM_TRACK_TIME_TO_IMPACT = 0.25f
    const val MAXIMUM_TRACK_TIME_TO_IMPACT = 30f
    const val MINIMUM_THREAT_DISTANCE = 1f

    fun scoreThreat(
        threat: ThreatSnapshot,
        engagementRange: Float,
        timeToImpact: Float,
        assignedCount: Int,
    ): Float {
        val normalizedDistance =
            1f -
                (threat.position.len() / engagementRange).coerceIn(
                    MINIMUM_NORMALIZED_THREAT_SCORE,
                    MAXIMUM_NORMALIZED_THREAT_SCORE,
                )
        val centerlineBias =
            1f -
                (abs(threat.position.x) / engagementRange).coerceIn(
                    MINIMUM_NORMALIZED_THREAT_SCORE,
                    MAXIMUM_NORMALIZED_THREAT_SCORE,
                )
        val altitudeUrgency =
            1f -
                (threat.position.y / ALTITUDE_REFERENCE).coerceIn(
                    MINIMUM_NORMALIZED_THREAT_SCORE,
                    MAXIMUM_NORMALIZED_THREAT_SCORE,
                )
        val timeUrgency =
            (TIME_URGENCY_REFERENCE - timeToImpact).coerceIn(
                MINIMUM_TIME_URGENCY,
                MAXIMUM_TIME_URGENCY,
            )
        val terminalBonus =
            if (threat.position.z >= TERMINAL_TRACK_THRESHOLD_Z) {
                WEIGHTS.terminalBonus
            } else {
                TERMINAL_TRACK_BONUS
            }

        return timeUrgency * WEIGHTS.timeUrgency +
            normalizedDistance * WEIGHTS.normalizedDistance +
            centerlineBias * WEIGHTS.centerlineBias +
            altitudeUrgency * WEIGHTS.altitudeUrgency +
            terminalBonus -
            assignedCount * WEIGHTS.assignmentPenalty
    }

    fun estimateTimeToImpact(threat: ThreatSnapshot): Float {
        val toTarget = threat.targetPosition.cpy().sub(threat.position)
        val distance = toTarget.len().coerceAtLeast(MINIMUM_THREAT_DISTANCE)
        val closingSpeed =
            if (threat.velocity.isZero(ZERO_VELOCITY_EPSILON)) {
                0f
            } else {
                max(0f, threat.velocity.dot(toTarget.nor()))
            }
        if (closingSpeed <= ZERO_VELOCITY_EPSILON) {
            val fallbackDistance =
                max(0f, abs(threat.position.z)) +
                    max(0f, threat.position.y * FALLBACK_VERTICAL_WEIGHT)
            return (fallbackDistance / FALLBACK_CLOSING_SPEED).coerceIn(
                MINIMUM_FALLBACK_TIME_TO_IMPACT,
                MAXIMUM_TRACK_TIME_TO_IMPACT,
            )
        }
        return (distance / closingSpeed).coerceIn(
            MINIMUM_TRACK_TIME_TO_IMPACT,
            MAXIMUM_TRACK_TIME_TO_IMPACT,
        )
    }
}

private fun LinearGrowth.valueAt(wave: Int): Float = base + wave * stepPerWave

private fun ScalarClamp.coerce(value: Float): Float = value.coerceIn(min, max)

private fun groundPoint(position: Vector3): Vector3 = Vector3(position.x, DamageTuning.GROUND_PLANE_Y, position.z)

private fun RandomSource.sample(range: ScalarClamp): Float = range(range.min, range.max)

enum class DefenseDoctrine(
    val label: String,
    val summary: String,
    val profile: DefenseDoctrineProfile,
) {
    DISCIPLINED(
        label = "DISCIPLINED",
        summary = "Single-shot coverage with more conservative battery spacing.",
        profile = DefenseDoctrineProfiles.disciplined,
    ),
    ADAPTIVE(
        label = "ADAPTIVE",
        summary = "Balanced coverage with selective late-track reinforcement.",
        profile = DefenseDoctrineProfiles.adaptive,
    ),
    SHIELD_WALL(
        label = "SHIELD WALL",
        summary = "Aggressive layered fire with critical-track double taps.",
        profile = DefenseDoctrineProfiles.shieldWall,
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
    fun threatsForWave(wave: Int): Int =
        BattleBalanceTuning.BASE_THREAT_COUNT +
            wave * BattleBalanceTuning.ADDED_THREATS_PER_WAVE

    fun spawnIntervalForWave(wave: Int): Float = BattleBalanceTuning.spawnIntervalForWave(wave)

    fun threatSpeedRangeForWave(wave: Int): ClosedFloatingPointRange<Float> {
        val band = BattleBalanceTuning.threatSpeedBandForWave(wave)
        return band.minimum..band.maximum
    }
}

object DefenseTuning {
    fun engagementRange(settings: DefenseSettings): Float {
        val doctrine = settings.doctrine.profile
        return DefenseTuningLimits.ENGAGEMENT_RANGE.coerce(settings.engagementRange + doctrine.engagementRangeBias)
    }

    fun launchCooldown(settings: DefenseSettings): Float {
        val doctrine = settings.doctrine.profile
        return DefenseTuningLimits.LAUNCH_COOLDOWN.coerce(settings.launchCooldown * doctrine.launchCadenceMultiplier)
    }

    fun launcherReload(settings: DefenseSettings): Float {
        val doctrine = settings.doctrine.profile
        val scaledReload = DefenseTuningLimits.BASE_LAUNCHER_RELOAD * doctrine.launcherReloadMultiplier
        return DefenseTuningLimits.LAUNCHER_RELOAD.coerce(scaledReload)
    }

    fun blastRadius(settings: DefenseSettings): Float {
        val doctrine = settings.doctrine.profile
        return DefenseTuningLimits.BLAST_RADIUS.coerce(settings.blastRadius + doctrine.fuseRadiusBias)
    }

    fun turnRate(settings: DefenseSettings): Float = settings.doctrine.profile.turnRate
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
        val b = InterceptionTuning.QUADRATIC_FACTOR * toTarget.dot(targetVelocity)
        val c = toTarget.dot(toTarget)
        val time =
            if (abs(a) < InterceptionTuning.NEAR_ZERO_EPSILON) {
                linearInterceptTime(
                    b = b,
                    c = c,
                )
            } else {
                quadraticInterceptTime(
                    a = a,
                    b = b,
                    c = c,
                )
            }.coerceIn(
                InterceptionTuning.MINIMUM_VALID_INTERCEPT_TIME,
                InterceptionTuning.MAX_INTERCEPT_LEAD_SECONDS,
            )
        return targetPos.cpy().mulAdd(targetVelocity, time)
    }

    private fun linearInterceptTime(
        b: Float,
        c: Float,
    ): Float {
        if (abs(b) < InterceptionTuning.NEAR_ZERO_EPSILON) {
            return InterceptionTuning.INTERCEPT_FALLBACK_SECONDS
        }
        return (-c / b).coerceAtLeast(InterceptionTuning.MINIMUM_VALID_INTERCEPT_TIME)
    }

    private fun quadraticInterceptTime(
        a: Float,
        b: Float,
        c: Float,
    ): Float {
        val discriminant = b * b - InterceptionTuning.DISCRIMINANT_FACTOR * a * c
        if (discriminant <= 0f) {
            return InterceptionTuning.INTERCEPT_FALLBACK_SECONDS
        }
        val sqrtDiscriminant = sqrt(discriminant)
        val t1 = (-b - sqrtDiscriminant) / (2f * a)
        val t2 = (-b + sqrtDiscriminant) / (2f * a)
        return listOf(t1, t2).filter { it > 0f }.minOrNull() ?: InterceptionTuning.INTERCEPT_FALLBACK_SECONDS
    }
}

object DamageModel {
    fun computeBuildingDamage(
        building: BuildingFootprint,
        impact: BlastImpact,
    ): Float {
        val distance = groundPoint(building.position).dst(groundPoint(impact.position))
        val effectiveRadius = impact.radius + max(building.width, building.depth)
        if (distance >= effectiveRadius) return 0f

        val scale = if (impact.hostile) DamageTuning.HOSTILE_DAMAGE_SCALE else DamageTuning.DEFENSIVE_DAMAGE_SCALE
        return (((effectiveRadius - distance) / effectiveRadius) * scale).coerceAtLeast(DamageTuning.MINIMUM_DAMAGE)
    }

    fun cityIntegrityLoss(
        hostile: Boolean,
        destroyedBuilding: Boolean,
    ): Float {
        var loss = if (hostile) DamageTuning.HOSTILE_CITY_INTEGRITY_LOSS else 0f
        if (destroyedBuilding) {
            loss += DamageTuning.COLLAPSE_INTEGRITY_LOSS
        }
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
                random.sample(ThreatLaunchTuning.SPAWN_X_RANGE),
                random.sample(ThreatLaunchTuning.SPAWN_ALTITUDE_RANGE),
                ThreatLaunchTuning.SPAWN_DEPTH,
            )
        val target =
            cityTarget.cpy().add(
                random.sample(ThreatLaunchTuning.CITY_TARGET_OFFSET_RANGE),
                ThreatLaunchTuning.CITY_TARGET_OFFSET_Y,
                random.sample(ThreatLaunchTuning.CITY_TARGET_OFFSET_RANGE),
            )
        val speedBand = BattleBalanceTuning.threatSpeedBandForWave(wave)
        val horizontalSpeed = random.range(speedBand.minimum, speedBand.maximum)
        val horizontalDistance =
            Vector3(target.x - start.x, 0f, target.z - start.z)
                .len()
                .coerceAtLeast(ThreatLaunchTuning.MINIMUM_HORIZONTAL_DISTANCE)
        val travelTime =
            (horizontalDistance / horizontalSpeed).coerceIn(
                ThreatLaunchTuning.MINIMUM_TRAVEL_TIME,
                ThreatLaunchTuning.MAXIMUM_TRAVEL_TIME,
            )
        val displacement = target.cpy().sub(start)
        val velocity = displacement.scl(ThreatLaunchTuning.HORIZONTAL_VELOCITY_SCALE / travelTime)
        velocity.y -= ThreatLaunchTuning.GRAVITY_COMPENSATION_FACTOR * PhysicsModel.THREAT_GRAVITY_Y * travelTime
        return ThreatLaunch(start, target, velocity)
    }
}

object EngagementPhysics {
    fun closesWithinFuse(
        interceptor: MotionSample,
        target: MotionSample,
        dt: Float,
        fuseRadius: Float,
    ): Boolean {
        val relativePos = target.position.cpy().sub(interceptor.position)
        val relativeVel = target.velocity.cpy().sub(interceptor.velocity)
        val relativeSpeedSquared = relativeVel.len2()
        val sampleTime =
            if (relativeSpeedSquared <= EngagementTuning.RELATIVE_SPEED_EPSILON) {
                EngagementTuning.STATIONARY_SAMPLE_TIME
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
                val assignedCount = assignedCountFor(threat.id, assignedThreatIds, assignmentCounts)
                if (shouldSkipThreat(assignedCount, doctrine)) return@forEach

                val timeToImpact = ThreatAssessmentTuning.estimateTimeToImpact(threat)
                if (shouldDelayRecommit(assignedCount, timeToImpact, doctrine)) return@forEach

                val score = ThreatAssessmentTuning.scoreThreat(threat, engagementRange, timeToImpact, assignedCount)
                if (shouldReplaceSelection(bestThreat, threat, score, bestScore)) {
                    bestThreat = threat
                    bestScore = score
                }
            }

        return bestThreat?.id
    }

    fun hasPriorityOver(
        candidate: Vector3,
        incumbent: Vector3,
    ): Boolean =
        when {
            candidate.z != incumbent.z -> candidate.z > incumbent.z
            candidate.y != incumbent.y -> candidate.y < incumbent.y
            else -> abs(candidate.x) < abs(incumbent.x)
        }

    private fun assignedCountFor(
        threatId: String,
        assignedThreatIds: Set<String>,
        assignmentCounts: Map<String, Int>,
    ): Int {
        val implicitAssignmentCount = if (threatId in assignedThreatIds) 1 else 0
        return max(assignmentCounts[threatId] ?: 0, implicitAssignmentCount)
    }

    private fun shouldSkipThreat(
        assignedCount: Int,
        doctrine: DefenseDoctrine,
    ): Boolean = assignedCount >= doctrine.profile.maxAssignmentsPerThreat

    private fun shouldDelayRecommit(
        assignedCount: Int,
        timeToImpact: Float,
        doctrine: DefenseDoctrine,
    ): Boolean = assignedCount > 0 && timeToImpact > doctrine.profile.recommitWindowSeconds

    private fun shouldReplaceSelection(
        incumbent: ThreatSnapshot?,
        candidate: ThreatSnapshot,
        candidateScore: Float,
        bestScore: Float,
    ): Boolean =
        when {
            incumbent == null -> true
            candidateScore > bestScore -> true
            candidateScore < bestScore -> false
            else -> hasPriorityOver(candidate.position, incumbent.position)
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
