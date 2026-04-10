package com.airdefense.game

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3
import kotlin.math.abs
import kotlin.math.max

object BattleBalance {
    fun threatsForWave(wave: Int): Int = 7 + wave * 4

    fun spawnIntervalForWave(wave: Int): Float = max(0.65f, 2.25f - wave * 0.1f)

    fun threatSpeedRangeForWave(wave: Int): ClosedFloatingPointRange<Float> =
        (260f + wave * 10f)..(330f + wave * 14f)
}

object InterceptionMath {
    fun predictInterceptPoint(
        interceptorPos: Vector3,
        targetPos: Vector3,
        targetVelocity: Vector3,
        interceptorSpeed: Float
    ): Vector3 {
        val toTarget = targetPos.cpy().sub(interceptorPos)
        val a = targetVelocity.dot(targetVelocity) - interceptorSpeed * interceptorSpeed
        val b = 2f * toTarget.dot(targetVelocity)
        val c = toTarget.dot(toTarget)
        val time = if (abs(a) < 0.001f) {
            if (abs(b) < 0.001f) 0f else (-c / b).coerceAtLeast(0f)
        } else {
            val discriminant = b * b - 4f * a * c
            if (discriminant <= 0f) 0f else {
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
        hostile: Boolean
    ): Float {
        val impactPoint = Vector3(impactPosition.x, 0f, impactPosition.z)
        val buildingPoint = Vector3(buildingPosition.x, 0f, buildingPosition.z)
        val distance = buildingPoint.dst(impactPoint)
        val effectiveRadius = blastRadius + max(buildingWidth, buildingDepth)
        if (distance >= effectiveRadius) return 0f
        val scale = if (hostile) 72f else 36f
        return (((effectiveRadius - distance) / effectiveRadius) * scale).coerceAtLeast(8f)
    }

    fun cityIntegrityLoss(hostile: Boolean, destroyedBuilding: Boolean): Float {
        var loss = if (hostile) 12f else 0f
        if (destroyedBuilding) loss += 6f
        return loss
    }
}

object ThreatFactory {
    fun createThreatLaunch(
        wave: Int,
        cityTarget: Vector3,
        random: RandomSource = DefaultRandomSource
    ): ThreatLaunch {
        val start = Vector3(
            random.range(-1800f, 1800f),
            random.range(1500f, 2100f),
            -4200f
        )
        val target = cityTarget.cpy().add(random.range(-80f, 80f), 0f, random.range(-80f, 80f))
        val speed = random.range(
            BattleBalance.threatSpeedRangeForWave(wave).start,
            BattleBalance.threatSpeedRangeForWave(wave).endInclusive
        )
        val travelTime = start.dst(target) / speed
        val velocity = target.cpy().sub(start).scl(1f / travelTime)
        velocity.y += random.range(210f, 290f)
        return ThreatLaunch(start, target, velocity)
    }
}

object FireControl {
    fun selectNextThreat(
        threats: List<ThreatSnapshot>,
        engagementRange: Float,
        assignedThreatIds: Set<String>
    ): String? {
        val engagementRangeSquared = engagementRange * engagementRange
        var bestThreat: ThreatSnapshot? = null
        threats.asSequence()
            .filter { it.id !in assignedThreatIds }
            .filter { it.position.len2() <= engagementRangeSquared }
            .forEach { threat ->
                val incumbent = bestThreat
                if (incumbent == null || hasPriorityOver(threat.position, incumbent.position)) {
                    bestThreat = threat
                }
            }
        return bestThreat?.id
    }

    fun hasPriorityOver(candidate: Vector3, incumbent: Vector3): Boolean {
        if (candidate.z != incumbent.z) return candidate.z > incumbent.z
        if (candidate.y != incumbent.y) return candidate.y < incumbent.y
        return abs(candidate.x) < abs(incumbent.x)
    }
}

data class ThreatLaunch(
    val start: Vector3,
    val target: Vector3,
    val velocity: Vector3
)

data class ThreatSnapshot(
    val id: String,
    val position: Vector3
)

interface RandomSource {
    fun range(min: Float, max: Float): Float
}

object DefaultRandomSource : RandomSource {
    override fun range(min: Float, max: Float): Float = MathUtils.random(min, max)
}
