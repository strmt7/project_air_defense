package com.airdefense.game

import java.io.File
import kotlin.math.roundToInt

private const val DEFAULT_RUNS = 300
private const val DEFAULT_WAVES = 1
private const val DEFAULT_MAX_SECONDS_PER_WAVE = 48f
private const val DEFAULT_STEP_SECONDS = 0.05f
private const val DEFAULT_BASE_SEED = 20260411L

private const val RUNS_ARG_INDEX = 0
private const val WAVES_ARG_INDEX = 1
private const val MAX_SECONDS_ARG_INDEX = 2
private const val STEP_SECONDS_ARG_INDEX = 3
private const val BASE_SEED_ARG_INDEX = 4
private const val REPORT_PATH_ARG_INDEX = 5
private const val SETTINGS_OFFSET_WITHOUT_REPORT = 5
private const val SETTINGS_OFFSET_WITH_REPORT = 6
private const val SETTINGS_ENGAGEMENT_RANGE_OFFSET = 0
private const val SETTINGS_INTERCEPTOR_SPEED_OFFSET = 1
private const val SETTINGS_LAUNCH_COOLDOWN_OFFSET = 2
private const val SETTINGS_BLAST_RADIUS_OFFSET = 3
private const val SETTINGS_DOCTRINE_OFFSET = 4

private const val P50_FRACTION = 0.5
private const val P90_FRACTION = 0.9
private const val MINIMUM_RESULT_COUNT = 1
private const val PERCENT_MULTIPLIER = 100.0

private data class BattleSimulationCliRequest(
    val simulation: BattleMonteCarloConfig,
    val reportPath: String? = null,
)

private data class MonteCarloReportPayload(
    val request: BattleSimulationCliRequest,
    val results: List<BattleRunSummary>,
)

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
        p50 = percentile(P50_FRACTION),
        p90 = percentile(P90_FRACTION),
        max = sorted.lastOrNull() ?: 0.0,
    )
}

private fun buildJsonReport(payload: MonteCarloReportPayload): String {
    val config = payload.request.simulation
    val results = payload.results
    val integrity = results.map { it.cityIntegrity.toDouble() }.toScalarSummary()
    val interceptRate = results.map { it.interceptRate * PERCENT_MULTIPLIER }.toScalarSummary()
    val score = results.map { it.score.toDouble() }.toScalarSummary()
    val hostileImpacts = results.map { it.hostileImpacts.toDouble() }.toScalarSummary()
    val destroyedBuildings = results.map { it.destroyedBuildings.toDouble() }.toScalarSummary()
    val gameOverRate =
        results.count { it.gameOver }.toDouble() /
            results.size.coerceAtLeast(MINIMUM_RESULT_COUNT) *
            PERCENT_MULTIPLIER

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
          "runs": ${config.runs},
          "waves": ${config.waves},
          "maxSecondsPerWave": ${config.maxSecondsPerWave},
          "stepSeconds": ${config.stepSeconds},
          "seed": ${config.baseSeed},
          "doctrine": {
            "mode": "${config.settings.doctrine.name}",
            "label": "${config.settings.doctrine.label}",
            "engagementRange": ${config.settings.engagementRange},
            "interceptorSpeed": ${config.settings.interceptorSpeed},
            "launchCooldown": ${config.settings.launchCooldown},
            "blastRadius": ${config.settings.blastRadius}
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

private fun parseCliRequest(args: Array<String>): BattleSimulationCliRequest {
    val defaultSettings = DefenseSettings()
    val possibleReportPath = args.getOrNull(REPORT_PATH_ARG_INDEX)
    val hasExplicitReportPath = isLikelyReportPath(possibleReportPath)
    val settingsOffset = if (hasExplicitReportPath) SETTINGS_OFFSET_WITH_REPORT else SETTINGS_OFFSET_WITHOUT_REPORT

    return BattleSimulationCliRequest(
        simulation =
            BattleMonteCarloConfig(
                runs = args.getOrNull(RUNS_ARG_INDEX)?.toIntOrNull() ?: DEFAULT_RUNS,
                waves = args.getOrNull(WAVES_ARG_INDEX)?.toIntOrNull() ?: DEFAULT_WAVES,
                maxSecondsPerWave =
                    args.getOrNull(MAX_SECONDS_ARG_INDEX)?.toFloatOrNull() ?: DEFAULT_MAX_SECONDS_PER_WAVE,
                stepSeconds = args.getOrNull(STEP_SECONDS_ARG_INDEX)?.toFloatOrNull() ?: DEFAULT_STEP_SECONDS,
                baseSeed = args.getOrNull(BASE_SEED_ARG_INDEX)?.toLongOrNull() ?: DEFAULT_BASE_SEED,
                settings =
                    DefenseSettings(
                        engagementRange =
                            args
                                .getOrNull(settingsOffset + SETTINGS_ENGAGEMENT_RANGE_OFFSET)
                                ?.toFloatOrNull() ?: defaultSettings.engagementRange,
                        interceptorSpeed =
                            args
                                .getOrNull(settingsOffset + SETTINGS_INTERCEPTOR_SPEED_OFFSET)
                                ?.toFloatOrNull() ?: defaultSettings.interceptorSpeed,
                        launchCooldown =
                            args
                                .getOrNull(settingsOffset + SETTINGS_LAUNCH_COOLDOWN_OFFSET)
                                ?.toFloatOrNull() ?: defaultSettings.launchCooldown,
                        blastRadius =
                            args
                                .getOrNull(settingsOffset + SETTINGS_BLAST_RADIUS_OFFSET)
                                ?.toFloatOrNull() ?: defaultSettings.blastRadius,
                        doctrine =
                            args
                                .getOrNull(settingsOffset + SETTINGS_DOCTRINE_OFFSET)
                                ?.uppercase()
                                ?.let { requested ->
                                    DefenseDoctrine.entries.firstOrNull { doctrine ->
                                        doctrine.name == requested
                                    }
                                } ?: defaultSettings.doctrine,
                    ),
            ),
        reportPath = if (hasExplicitReportPath) possibleReportPath else null,
    )
}

private fun printSummary(
    config: BattleMonteCarloConfig,
    results: List<BattleRunSummary>,
) {
    val averageIntegrity = results.map { it.cityIntegrity }.average()
    val averageInterceptRate = results.map { it.interceptRate }.average()
    val averageScore = results.map { it.score }.average()
    val averageHostileImpacts = results.map { it.hostileImpacts }.average()
    val averageDestroyedBuildings = results.map { it.destroyedBuildings }.average()

    println(
        "[battle-monte-carlo] runs=${config.runs} " +
            "waves=${config.waves} " +
            "maxSecondsPerWave=${config.maxSecondsPerWave} " +
            "stepSeconds=${config.stepSeconds} " +
            "seed=${config.baseSeed}",
    )
    println(
        "[battle-monte-carlo] doctrine " +
            "range=${config.settings.engagementRange.roundToInt()} " +
            "speed=${config.settings.interceptorSpeed.roundToInt()} " +
            "cooldown=${"%.2f".format(config.settings.launchCooldown)} " +
            "blast=${config.settings.blastRadius.roundToInt()} " +
            "mode=${config.settings.doctrine.name}",
    )
    println(
        "[battle-monte-carlo] avgCityIntegrity=${averageIntegrity.roundToInt()}% " +
            "avgInterceptRate=${(averageInterceptRate * PERCENT_MULTIPLIER).roundToInt()}% " +
            "avgScore=${averageScore.roundToInt()}",
    )
    println(
        "[battle-monte-carlo] avgHostileImpacts=${"%.2f".format(averageHostileImpacts)} " +
            "avgDestroyedBuildings=${"%.2f".format(averageDestroyedBuildings)}",
    )
    println(
        "[battle-monte-carlo] worstRun=${results.minByOrNull { it.cityIntegrity }?.runIndex} " +
            "bestRun=${results.maxByOrNull { it.cityIntegrity }?.runIndex}",
    )
}

private fun writeJsonReportIfRequested(
    request: BattleSimulationCliRequest,
    results: List<BattleRunSummary>,
) {
    val reportPath = request.reportPath ?: return
    val reportFile = File(reportPath)
    reportFile.parentFile?.mkdirs()
    reportFile.writeText(
        buildJsonReport(
            MonteCarloReportPayload(
                request = request,
                results = results,
            ),
        ),
    )
    println("[battle-monte-carlo] wroteJsonReport=${reportFile.absolutePath}")
}

fun main(args: Array<String>) {
    val request = parseCliRequest(args)
    val results = BattleMonteCarloRunner.run(request.simulation)
    printSummary(request.simulation, results)
    writeJsonReportIfRequested(request, results)
}
