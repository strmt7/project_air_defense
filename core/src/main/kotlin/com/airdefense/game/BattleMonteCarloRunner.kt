package com.airdefense.game

data class BattleMonteCarloConfig(
    val runs: Int,
    val waves: Int,
    val maxSecondsPerWave: Float,
    val stepSeconds: Float,
    val baseSeed: Long,
    val settings: DefenseSettings = DefenseSettings(),
)

object BattleMonteCarloRunner {
    fun run(config: BattleMonteCarloConfig): List<BattleRunSummary> {
        return (0 until config.runs).map { runIndex ->
            val simulation =
                BattleSimulation(
                    buildingDefinitions = BattleWorldLayout.buildingDefinitions(),
                    launcherPositions = BattleWorldLayout.launcherPositions(),
                    settings = config.settings.copy(),
                    random = SeededRandomSource(config.baseSeed + runIndex),
                )

            var simulatedSeconds = 0f
            repeat(config.waves) {
                if (!simulation.startNewWave()) return@repeat

                var elapsedThisWave = 0f
                while (!simulation.isGameOver && elapsedThisWave < config.maxSecondsPerWave) {
                    simulation.step(config.stepSeconds)
                    simulatedSeconds += config.stepSeconds
                    elapsedThisWave += config.stepSeconds
                    if (!simulation.waveInProgress && simulation.threats.isEmpty()) break
                }
            }

            simulation.snapshot(runIndex = runIndex + 1, simulatedSeconds = simulatedSeconds)
        }
    }
}
