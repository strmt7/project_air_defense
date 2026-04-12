package com.airdefense.game

import com.badlogic.gdx.math.Vector3
import kotlin.test.Test
import kotlin.test.assertEquals
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
        assertTrue(
            simulation.totalThreatsIntercepted + simulation.totalHostileImpacts <= simulation.totalThreatsSpawned,
        )
        assertTrue(simulation.cityIntegrity in 0f..100f)
    }

    @Test
    fun `batch runner shows bounded but non-zero threat leakage`() {
        val results =
            BattleMonteCarloRunner.run(
                BattleMonteCarloConfig(
                    runs = 48,
                    waves = 1,
                    maxSecondsPerWave = 48f,
                    stepSeconds = 0.05f,
                    baseSeed = 9000L,
                ),
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
                BattleMonteCarloConfig(
                    runs = 48,
                    waves = 1,
                    maxSecondsPerWave = 48f,
                    stepSeconds = 0.05f,
                    baseSeed = 12000L,
                    settings = DefenseSettings(doctrine = DefenseDoctrine.DISCIPLINED),
                ),
            )
        val shieldWall =
            BattleMonteCarloRunner.run(
                BattleMonteCarloConfig(
                    runs = 48,
                    waves = 1,
                    maxSecondsPerWave = 48f,
                    stepSeconds = 0.05f,
                    baseSeed = 12000L,
                    settings = DefenseSettings(doctrine = DefenseDoctrine.SHIELD_WALL),
                ),
            )

        val disciplinedInterceptRate = disciplined.map { it.interceptRate }.average()
        val shieldWallInterceptRate = shieldWall.map { it.interceptRate }.average()
        val disciplinedIntegrity = disciplined.map { it.cityIntegrity }.average()
        val shieldWallIntegrity = shieldWall.map { it.cityIntegrity }.average()

        assertTrue(
            shieldWallInterceptRate > disciplinedInterceptRate,
            (
                "shield wall should raise intercept rate: disciplined=$disciplinedInterceptRate " +
                    "shieldWall=$shieldWallInterceptRate"
            ),
        )
        assertTrue(
            shieldWallIntegrity >= disciplinedIntegrity,
            (
                "shield wall should not reduce average city integrity: disciplined=$disciplinedIntegrity " +
                    "shieldWall=$shieldWallIntegrity"
            ),
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

    @Test
    fun `seeded single run snapshot stays stable`() {
        val results =
            BattleMonteCarloRunner.run(
                BattleMonteCarloConfig(
                    runs = 1,
                    waves = 1,
                    maxSecondsPerWave = 48f,
                    stepSeconds = 0.05f,
                    baseSeed = 4242L,
                ),
            )
        val result = results.single()

        assertEquals(9, result.threatsSpawned)
        assertEquals(8, result.threatsIntercepted)
        assertEquals(1, result.hostileImpacts)
        assertEquals(0, result.destroyedBuildings)
        assertEquals(1200, result.score)
        assertEquals(88f, result.cityIntegrity, 0.0001f)
        assertEquals(8f / 9f, result.interceptRate, 0.0001f)
        assertTrue(!result.gameOver)
    }

    @Test
    fun `step removes orphan interceptor without affecting city state`() {
        val simulation =
            BattleSimulation(
                buildingDefinitions = BattleWorldLayout.buildingDefinitions(),
                launcherPositions = BattleWorldLayout.launcherPositions(),
                settings = DefenseSettings(),
                random = SeededRandomSource(77L),
            )
        simulation.interceptors +=
            SimulationInterceptor(
                id = "I-TEST",
                position = Vector3(0f, 200f, 0f),
                velocity = Vector3(0f, 1f, 0f),
                targetId = "missing-threat",
                launcherIndex = 0,
            )

        val events = simulation.step(0.05f)

        assertTrue("I-TEST" in events.removedInterceptorIds)
        assertTrue(simulation.interceptors.isEmpty())
        assertEquals(100f, simulation.cityIntegrity, 0.0001f)
    }

    @Test
    fun `step resolves hostile impact when a threat reaches its target`() {
        val simulation =
            BattleSimulation(
                buildingDefinitions = BattleWorldLayout.buildingDefinitions(),
                launcherPositions = BattleWorldLayout.launcherPositions(),
                settings = DefenseSettings(),
                random = SeededRandomSource(88L),
            )
        val targetBuilding = simulation.buildings.first()
        simulation.threats +=
            SimulationThreat(
                id = "T-IMPACT",
                position = targetBuilding.position.cpy().add(0f, 80f, 0f),
                targetPosition = targetBuilding.position.cpy(),
                velocity = Vector3.Zero.cpy(),
            )

        val events = simulation.step(0.05f)

        assertTrue("T-IMPACT" in events.removedThreatIds)
        assertTrue(events.blastEvents.any { it.kind == BlastKind.HOSTILE_IMPACT })
        assertTrue(events.buildingDamageEvents.any { it.buildingId == targetBuilding.id })
        assertEquals(1, simulation.totalHostileImpacts)
        assertTrue(simulation.cityIntegrity < 100f)
    }
}
