package com.airdefense.game

import java.io.File
import kotlin.math.roundToInt

object BattleMonteCarloRunner {
    fun run(
        runs: Int,
        waves: Int,
        maxSecondsPerWave: Float,
        stepSeconds: Float,
        baseSeed: Long,
        settings: DefenseSettings = DefenseSettings(),
    ): List<BattleRunSummary> {
        return (0 until runs).map { runIndex ->
            val simulation =
                BattleSimulation(
                    buildingDefinitions = BattleWorldLayout.buildingDefinitions(),
                    launcherPositions = BattleWorldLayout.launcherPositions(),
                    settings = settings.copy(),
                    random = SeededRandomSource(baseSeed + runIndex),
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

private data class ScalarSummary(
    val average: Double,
    val min: Double,
    val p50: Double,
    val p90: Double,
    val max: Double,
)

private fun List<Double>.toScalarSummary(): ScalarSummary {
    val sorted = sorted()

    fun percentile(fraction: Double): Double {
        if (sorted.isEmpty()) return 0.0
        val index = ((sorted.size - 1) * fraction).toInt().coerceIn(0, sorted.lastIndex)
        return sorted[index]
    }

    return ScalarSummary(
        average = average(),
        min = sorted.firstOrNull() ?: 0.0,
        p50 = percentile(0.5),
        p90 = percentile(0.9),
        max = sorted.lastOrNull() ?: 0.0,
    )
}

private fun buildJsonReport(
    runs: Int,
    waves: Int,
    maxSeconds: Float,
    stepSeconds: Float,
    baseSeed: Long,
    results: List<BattleRunSummary>,
    settings: DefenseSettings,
): String {
    val integrity = results.map { it.cityIntegrity.toDouble() }.toScalarSummary()
    val interceptRate = results.map { it.interceptRate * 100.0 }.toScalarSummary()
    val score = results.map { it.score.toDouble() }.toScalarSummary()
    val hostileImpacts = results.map { it.hostileImpacts.toDouble() }.toScalarSummary()
    val destroyedBuildings = results.map { it.destroyedBuildings.toDouble() }.toScalarSummary()
    val gameOverRate = results.count { it.gameOver }.toDouble() / results.size.coerceAtLeast(1) * 100.0

    fun summaryJson(
        name: String,
        summary: ScalarSummary,
    ): String =
        """
        "$name": {
          "average": ${"%.3f".format(summary.average)},
          "min": ${"%.3f".format(summary.min)},
          "p50": ${"%.3f".format(summary.p50)},
          "p90": ${"%.3f".format(summary.p90)},
          "max": ${"%.3f".format(summary.max)}
        }
        """.trimIndent()

    return """
        {
          "runs": $runs,
          "waves": $waves,
          "maxSecondsPerWave": $maxSeconds,
          "stepSeconds": $stepSeconds,
          "seed": $baseSeed,
          "doctrine": {
            "mode": "${settings.doctrine.name}",
            "label": "${settings.doctrine.label}",
            "engagementRange": ${settings.engagementRange},
            "interceptorSpeed": ${settings.interceptorSpeed},
            "launchCooldown": ${settings.launchCooldown},
            "blastRadius": ${settings.blastRadius}
          },
          "gameOverRatePercent": ${"%.3f".format(gameOverRate)},
          "metrics": {
            ${summaryJson("cityIntegrityPercent", integrity)},
            ${summaryJson("interceptRatePercent", interceptRate)},
            ${summaryJson("score", score)},
            ${summaryJson("hostileImpacts", hostileImpacts)},
            ${summaryJson("destroyedBuildings", destroyedBuildings)}
          }
        }
        """.trimIndent()
}

private fun isLikelyReportPath(candidate: String?): Boolean {
    if (candidate.isNullOrBlank()) return false
    return candidate.endsWith(".json", ignoreCase = true) ||
        candidate.contains('\\') ||
        candidate.contains('/')
}

fun main(args: Array<String>) {
    val defaultSettings = DefenseSettings()
    val runs = args.getOrNull(0)?.toIntOrNull() ?: 300
    val waves = args.getOrNull(1)?.toIntOrNull() ?: 1
    val maxSeconds = args.getOrNull(2)?.toFloatOrNull() ?: 48f
    val stepSeconds = args.getOrNull(3)?.toFloatOrNull() ?: 0.05f
    val baseSeed = args.getOrNull(4)?.toLongOrNull() ?: 20260411L
    val possibleReportPath = args.getOrNull(5)
    val hasExplicitReportPath = isLikelyReportPath(possibleReportPath)
    val reportPath = if (hasExplicitReportPath) possibleReportPath else null
    val doctrineOffset = if (hasExplicitReportPath) 6 else 5
    val settings =
        DefenseSettings(
            engagementRange = args.getOrNull(doctrineOffset)?.toFloatOrNull() ?: defaultSettings.engagementRange,
            interceptorSpeed = args.getOrNull(doctrineOffset + 1)?.toFloatOrNull() ?: defaultSettings.interceptorSpeed,
            launchCooldown = args.getOrNull(doctrineOffset + 2)?.toFloatOrNull() ?: defaultSettings.launchCooldown,
            blastRadius = args.getOrNull(doctrineOffset + 3)?.toFloatOrNull() ?: defaultSettings.blastRadius,
            doctrine =
                args
                    .getOrNull(doctrineOffset + 4)
                    ?.uppercase()
                    ?.let { requested -> DefenseDoctrine.entries.firstOrNull { it.name == requested } } ?: defaultSettings.doctrine,
        )

    val results =
        BattleMonteCarloRunner.run(
            runs = runs,
            waves = waves,
            maxSecondsPerWave = maxSeconds,
            stepSeconds = stepSeconds,
            baseSeed = baseSeed,
            settings = settings,
        )

    val averageIntegrity = results.map { it.cityIntegrity }.average()
    val averageInterceptRate = results.map { it.interceptRate }.average()
    val averageScore = results.map { it.score }.average()
    val averageHostileImpacts = results.map { it.hostileImpacts }.average()
    val averageDestroyedBuildings = results.map { it.destroyedBuildings }.average()

    println("[battle-monte-carlo] runs=$runs waves=$waves maxSecondsPerWave=$maxSeconds stepSeconds=$stepSeconds seed=$baseSeed")
    val doctrineLine =
        "[battle-monte-carlo] doctrine " +
            "range=${settings.engagementRange.roundToInt()} " +
            "speed=${settings.interceptorSpeed.roundToInt()} " +
            "cooldown=${"%.2f".format(settings.launchCooldown)} " +
            "blast=${settings.blastRadius.roundToInt()} " +
            "mode=${settings.doctrine.name}"
    println(
        doctrineLine,
    )
    println(
        "[battle-monte-carlo] avgCityIntegrity=${averageIntegrity.roundToInt()}% avgInterceptRate=${(averageInterceptRate * 100).roundToInt()}% avgScore=${averageScore.roundToInt()}",
    )
    println(
        "[battle-monte-carlo] avgHostileImpacts=${"%.2f".format(
            averageHostileImpacts,
        )} avgDestroyedBuildings=${"%.2f".format(averageDestroyedBuildings)}",
    )
    println(
        "[battle-monte-carlo] worstRun=${results.minByOrNull {
            it.cityIntegrity
        }?.runIndex} bestRun=${results.maxByOrNull { it.cityIntegrity }?.runIndex}",
    )

    reportPath?.let { path ->
        val reportFile = File(path)
        reportFile.parentFile?.mkdirs()
        reportFile.writeText(buildJsonReport(runs, waves, maxSeconds, stepSeconds, baseSeed, results, settings))
        println("[battle-monte-carlo] wroteJsonReport=${reportFile.absolutePath}")
    }
}
