package com.airdefense.game

internal data class BattleRuntimeSnapshot(
    val credits: Int,
    val wave: Int,
    val score: Int,
    val cityIntegrity: Float,
    val waveInProgress: Boolean,
    val isGameOver: Boolean,
    val threatsRemainingInWave: Int,
)

internal fun battleRuntimeSnapshot(simulation: BattleSimulation): BattleRuntimeSnapshot =
    BattleRuntimeSnapshot(
        credits = simulation.credits,
        wave = simulation.wave,
        score = simulation.score,
        cityIntegrity = simulation.cityIntegrity,
        waveInProgress = simulation.waveInProgress,
        isGameOver = simulation.isGameOver,
        threatsRemainingInWave = simulation.threatsRemainingInWave,
    )

internal fun buildBattleHudSnapshot(
    snapshot: BattleRuntimeSnapshot,
    visibleThreats: Int,
    settings: DefenseSettings,
): BattleHudSnapshot =
    BattleHudSnapshot(
        cityIntegrity = snapshot.cityIntegrity,
        score = snapshot.score,
        credits = snapshot.credits,
        wave = snapshot.wave,
        waveInProgress = snapshot.waveInProgress,
        isGameOver = snapshot.isGameOver,
        visibleThreats = visibleThreats,
        remainingThreatsInWave = snapshot.threatsRemainingInWave,
        effectiveRangeMeters = DefenseTuning.engagementRange(settings),
        effectiveFuseMeters = DefenseTuning.blastRadius(settings),
        doctrineLabel = settings.doctrine.label,
        doctrineSummary = settings.doctrine.summary,
    )

internal fun buildBattleFrameSummaryInput(
    qualityLabel: String,
    effectCount: Int,
    threatCount: Int,
    interceptorCount: Int,
): BattleFrameSummaryInput =
    BattleFrameSummaryInput(
        qualityLabel = qualityLabel,
        effectCount = effectCount,
        threatCount = threatCount,
        interceptorCount = interceptorCount,
    )
