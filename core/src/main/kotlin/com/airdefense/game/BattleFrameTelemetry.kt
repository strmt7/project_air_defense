package com.airdefense.game

import java.util.Locale

private const val FRAME_TIME_MAX_DELTA_SECONDS = 0.25f
private const val MILLISECONDS_PER_SECOND = 1000f
private const val BATTLE_FRAME_LOG_INTERVAL_SECONDS = 3f
private const val EMPTY_BATTLE_FPS = "0.0"
private const val EMPTY_BATTLE_FRAME_TIMING = "0.0/0.0/0.0"
private const val MEDIAN_PERCENTILE = 0.5f
private const val HIGH_PERCENTILE = 0.95f

internal data class BattleFrameSummaryInput(
    val qualityLabel: String,
    val effectCount: Int,
    val threatCount: Int,
    val interceptorCount: Int,
)

internal class BattleFrameTelemetry(
    windowSize: Int = 180,
    private val logIntervalSeconds: Float = BATTLE_FRAME_LOG_INTERVAL_SECONDS,
) {
    private val frameTimeWindowMs = FloatArray(windowSize)
    private var frameTimeWindowCursor = 0
    private var frameTimeWindowSize = 0
    private var battleFrameLogTimer = 0f

    var liveTimeSeconds = 0f
        private set

    fun onFrame(
        deltaSeconds: Float,
        summaryInput: BattleFrameSummaryInput,
    ): String? {
        liveTimeSeconds += deltaSeconds

        val frameTimeMs =
            deltaSeconds.coerceIn(0f, FRAME_TIME_MAX_DELTA_SECONDS) * MILLISECONDS_PER_SECOND
        frameTimeWindowMs[frameTimeWindowCursor] = frameTimeMs
        frameTimeWindowCursor = (frameTimeWindowCursor + 1) % frameTimeWindowMs.size
        frameTimeWindowSize = minOf(frameTimeWindowSize + 1, frameTimeWindowMs.size)

        battleFrameLogTimer += deltaSeconds
        if (battleFrameLogTimer < logIntervalSeconds || frameTimeWindowSize == 0) {
            return null
        }

        battleFrameLogTimer = 0f
        return snapshot(summaryInput)
    }

    fun snapshot(summaryInput: BattleFrameSummaryInput): String {
        if (liveTimeSeconds <= 0f || frameTimeWindowSize == 0) {
            return buildString {
                append("LIVE 0s // FPS $EMPTY_BATTLE_FPS // FT $EMPTY_BATTLE_FRAME_TIMING")
                append(" // Q ${summaryInput.qualityLabel}")
                append(" // FX${summaryInput.effectCount}")
                append(" // T${summaryInput.threatCount} I${summaryInput.interceptorCount}")
            }
        }

        val samples = copyFrameTimeWindow()
        samples.sort()
        val averageFrameMs = samples.sum() / samples.size
        val averageFps =
            if (averageFrameMs > 0f) {
                MILLISECONDS_PER_SECOND / averageFrameMs
            } else {
                0f
            }
        val p50 = percentile(samples, MEDIAN_PERCENTILE)
        val p95 = percentile(samples, HIGH_PERCENTILE)
        val maxFrameMs = samples.last()

        return buildString {
            append(
                "LIVE ${liveTimeSeconds.toInt()}s // FPS ${formatFrameValue(averageFps)}",
            )
            append(
                " // FT ${formatFrameValue(p50)}/${formatFrameValue(p95)}/${formatFrameValue(maxFrameMs)}",
            )
            append(" // Q ${summaryInput.qualityLabel}")
            append(" // FX${summaryInput.effectCount}")
            append(" // T${summaryInput.threatCount} I${summaryInput.interceptorCount}")
        }
    }

    private fun copyFrameTimeWindow(): FloatArray {
        val copy = FloatArray(frameTimeWindowSize)
        val start =
            if (frameTimeWindowSize == frameTimeWindowMs.size) {
                frameTimeWindowCursor
            } else {
                0
            }

        for (index in 0 until frameTimeWindowSize) {
            copy[index] = frameTimeWindowMs[(start + index) % frameTimeWindowMs.size]
        }
        return copy
    }

    private fun percentile(
        sortedSamples: FloatArray,
        percentile: Float,
    ): Float {
        if (sortedSamples.isEmpty()) return 0f
        val index =
            ((sortedSamples.size - 1) * percentile)
                .toInt()
                .coerceIn(0, sortedSamples.lastIndex)
        return sortedSamples[index]
    }

    private fun formatFrameValue(value: Float): String = "%.1f".format(Locale.US, value)
}
