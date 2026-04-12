package com.airdefense.game

private const val ZERO_PERCENT = 0f

internal data class BattleHudSnapshot(
    val cityIntegrity: Float,
    val score: Int,
    val credits: Int,
    val wave: Int,
    val waveInProgress: Boolean,
    val isGameOver: Boolean,
    val visibleThreats: Int,
    val remainingThreatsInWave: Int,
    val effectiveRangeMeters: Float,
    val effectiveFuseMeters: Float,
    val doctrineLabel: String,
    val doctrineSummary: String,
)

internal data class BattleWaveButtonState(
    val text: String,
    val disabled: Boolean,
)

internal fun BattleHudSnapshot.rangeText(): String = "${effectiveRangeMeters.toInt()} M"

internal fun BattleHudSnapshot.fuseText(): String = "${effectiveFuseMeters.toInt()} M"

internal fun BattleHudSnapshot.waveStateText(): String =
    when {
        isGameOver -> "STATUS LOST"
        waveInProgress -> "WAVE $wave LIVE / ${visibleThreats + remainingThreatsInWave} HOSTILES"
        else -> "WAVE $wave READY"
    }

internal fun BattleHudSnapshot.summaryText(): String =
    buildString {
        append("CITY ${(cityIntegrity.coerceAtLeast(ZERO_PERCENT)).toInt()}%")
        append("   SCORE $score")
        append("   CR $credits")
        append("   ${waveStateText()}")
    }

internal fun BattleHudSnapshot.waveButtonState(): BattleWaveButtonState =
    when {
        isGameOver -> BattleWaveButtonState("DEFENSE FAILED", disabled = true)
        waveInProgress -> BattleWaveButtonState("WAVE $wave ACTIVE", disabled = true)
        else -> BattleWaveButtonState("START NEXT WAVE", disabled = false)
    }
