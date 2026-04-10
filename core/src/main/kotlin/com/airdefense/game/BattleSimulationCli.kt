package com.airdefense.game

import kotlin.math.roundToInt

object BattleMonteCarloRunner {
    fun run(
        runs: Int,
        waves: Int,
        maxSecondsPerWave: Float,
        stepSeconds: Float,
        baseSeed: Long
    ): List<BattleRunSummary> {
        return (0 until runs).map { runIndex ->
            val simulation = BattleSimulation(
                buildingDefinitions = BattleWorldLayout.buildingDefinitions(),
                launcherPositions = BattleWorldLayout.launcherPositions(),
                settings = DefenseSettings(),
                random = SeededRandomSource(baseSeed + runIndex)
            )

            var simulatedSeconds = 0f
            repeat(waves) {
                if (!simulation.startNewWave()) return@repeat
                var elapsedThisWave = 0f
                while (!simulation.isGameOver && elapsedThisWave < maxSecondsPerWave) {
                    simulation.step(stepSeconds)
                    simulatedSeconds += stepSeconds
                    elapsedThisWave += stepSeconds
                    if (!simulation.waveInProgress && simulation.threats.isEmpty()) break
                }
            }

            simulation.snapshot(runIndex + 1, simulatedSeconds)
        }
    }
}

fun main(args: Array<String>) {
    val runs = args.getOrNull(0)?.toIntOrNull() ?: 300
    val waves = args.getOrNull(1)?.toIntOrNull() ?: 1
    val maxSeconds = args.getOrNull(2)?.toFloatOrNull() ?: 48f
    val stepSeconds = args.getOrNull(3)?.toFloatOrNull() ?: 0.05f
    val baseSeed = args.getOrNull(4)?.toLongOrNull() ?: 20260411L

    val results = BattleMonteCarloRunner.run(
        runs = runs,
        waves = waves,
        maxSecondsPerWave = maxSeconds,
        stepSeconds = stepSeconds,
        baseSeed = baseSeed
    )

    val averageIntegrity = results.map { it.cityIntegrity }.average()
    val averageInterceptRate = results.map { it.interceptRate }.average()
    val averageScore = results.map { it.score }.average()
    val averageHostileImpacts = results.map { it.hostileImpacts }.average()
    val averageDestroyedBuildings = results.map { it.destroyedBuildings }.average()

    println("[battle-monte-carlo] runs=$runs waves=$waves maxSecondsPerWave=$maxSeconds stepSeconds=$stepSeconds seed=$baseSeed")
    println("[battle-monte-carlo] avgCityIntegrity=${averageIntegrity.roundToInt()}% avgInterceptRate=${(averageInterceptRate * 100).roundToInt()}% avgScore=${averageScore.roundToInt()}")
    println("[battle-monte-carlo] avgHostileImpacts=${"%.2f".format(averageHostileImpacts)} avgDestroyedBuildings=${"%.2f".format(averageDestroyedBuildings)}")
    println("[battle-monte-carlo] worstRun=${results.minByOrNull { it.cityIntegrity }?.runIndex} bestRun=${results.maxByOrNull { it.cityIntegrity }?.runIndex}")
}
