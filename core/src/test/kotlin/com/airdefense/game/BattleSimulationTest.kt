package com.airdefense.game

import com.badlogic.gdx.math.Vector3
import kotlin.test.Test
import kotlin.test.assertTrue

class BattleSimulationTest {
    @Test
    fun `simulation accounting stays internally consistent`() {
        val simulation =
            BattleSimulation(
                buildingDefinitions = BattleWorldLayout.buildingDefinitions(),
                launcherPositions = BattleWorldLayout.launcherPositions(),
                settings = DefenseSettings(),
                random = SeededRandomSource(42L),
            )

        assertTrue(simulation.startNewWave())

        repeat(700) {
            simulation.step(0.05f)
            if (!simulation.waveInProgress && simulation.threats.isEmpty()) return@repeat
        }

        assertTrue(simulation.totalThreatsSpawned > 0)
        assertTrue(simulation.totalInterceptorsLaunched >= simulation.totalThreatsIntercepted)
        assertTrue(simulation.totalThreatsIntercepted + simulation.totalHostileImpacts <= simulation.totalThreatsSpawned)
        assertTrue(simulation.cityIntegrity in 0f..100f)
    }

    @Test
    fun `batch runner shows bounded but non-zero threat leakage`() {
        val results =
            BattleMonteCarloRunner.run(
                runs = 48,
                waves = 1,
                maxSecondsPerWave = 48f,
                stepSeconds = 0.05f,
                baseSeed = 9000L,
            )

        val averageInterceptRate = results.map { it.interceptRate }.average()

        assertTrue(results.any { it.hostileImpacts > 0 }, "expected at least one run with a leak")
        assertTrue(results.any { it.cityIntegrity >= 70f }, "expected some runs to hold the city together")
        assertTrue(averageInterceptRate > 0.45, "average intercept rate dropped too low: $averageInterceptRate")
        assertTrue(averageInterceptRate < 0.98, "average intercept rate is too perfect: $averageInterceptRate")
    }

    @Test
    fun `shield wall doctrine improves intercept performance over disciplined doctrine`() {
        val disciplined =
            BattleMonteCarloRunner.run(
                runs = 48,
                waves = 1,
                maxSecondsPerWave = 48f,
                stepSeconds = 0.05f,
                baseSeed = 12000L,
                settings = DefenseSettings(doctrine = DefenseDoctrine.DISCIPLINED),
            )
        val shieldWall =
            BattleMonteCarloRunner.run(
                runs = 48,
                waves = 1,
                maxSecondsPerWave = 48f,
                stepSeconds = 0.05f,
                baseSeed = 12000L,
                settings = DefenseSettings(doctrine = DefenseDoctrine.SHIELD_WALL),
            )

        val disciplinedInterceptRate = disciplined.map { it.interceptRate }.average()
        val shieldWallInterceptRate = shieldWall.map { it.interceptRate }.average()
        val disciplinedIntegrity = disciplined.map { it.cityIntegrity }.average()
        val shieldWallIntegrity = shieldWall.map { it.cityIntegrity }.average()

        assertTrue(
            shieldWallInterceptRate > disciplinedInterceptRate,
            "shield wall should raise intercept rate: disciplined=$disciplinedInterceptRate shieldWall=$shieldWallInterceptRate",
        )
        assertTrue(
            shieldWallIntegrity >= disciplinedIntegrity,
            "shield wall should not reduce average city integrity: disciplined=$disciplinedIntegrity shieldWall=$shieldWallIntegrity",
        )
    }

    @Test
    fun `headless simulation targets real building coordinates`() {
        val simulation =
            BattleSimulation(
                buildingDefinitions = BattleWorldLayout.buildingDefinitions(),
                launcherPositions = BattleWorldLayout.launcherPositions(),
                settings = DefenseSettings(),
                random =
                    object : RandomSource {
                        override fun range(
                            min: Float,
                            max: Float,
                        ): Float = (min + max) * 0.5f
                    },
            )

        simulation.startNewWave()
        simulation.step(0.4f)

        val threat = simulation.threats.first()
        val nearestBuildingDistance =
            simulation.buildings.minOf { building ->
                building.position.dst(Vector3(threat.targetPosition.x, 0f, threat.targetPosition.z))
            }

        assertTrue(nearestBuildingDistance < 140f)
    }
}
