package com.airdefense.game

import com.badlogic.gdx.Gdx

internal data class BattleInitializationFrame(
    val message: String,
    val diagnosticsLine: String,
    val progress: Float,
)

internal data class InitializationTask(
    val message: String,
    val action: () -> Unit,
)

internal class BattleInitializationController(
    private val tasks: List<InitializationTask>,
    private val qualityLabel: String,
    private val defaultMessage: String = "Initializing battle systems...",
) {
    private var nextStepIndex = 0
    private var lastCompletedDurationMs = 0L
    private var lastMessage = defaultMessage

    val isComplete: Boolean
        get() = nextStepIndex >= tasks.size

    fun advance(): BattleInitializationFrame {
        if (isComplete) {
            return currentFrame()
        }

        val task = tasks[nextStepIndex]
        lastMessage = task.message
        val startedAt = System.currentTimeMillis()
        task.action()
        lastCompletedDurationMs = System.currentTimeMillis() - startedAt
        Gdx.app.log(
            "BattleInit",
            "${nextStepIndex + 1}/${tasks.size} ${task.message} took ${lastCompletedDurationMs}ms",
        )
        nextStepIndex += 1
        return currentFrame()
    }

    fun currentFrame(): BattleInitializationFrame {
        val totalSteps = tasks.size.coerceAtLeast(1)
        val currentStep = (nextStepIndex + 1).coerceAtMost(totalSteps)
        return BattleInitializationFrame(
            message = lastMessage,
            diagnosticsLine =
                "STEP $currentStep/$totalSteps // LAST ${lastCompletedDurationMs}ms // $qualityLabel",
            progress = nextStepIndex.toFloat() / totalSteps,
        )
    }
}
