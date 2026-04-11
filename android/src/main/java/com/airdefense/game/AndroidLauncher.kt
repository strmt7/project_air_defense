package com.airdefense.game

import android.os.Bundle
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration

class AndroidLauncher : AndroidApplication() {
    companion object {
        const val EXTRA_LAUNCH_TARGET = "com.airdefense.game.extra.LAUNCH_TARGET"
        const val EXTRA_BENCHMARK_SEED = "com.airdefense.game.extra.BENCHMARK_SEED"
        private const val TARGET_BATTLE = "battle"
        private const val DEFAULT_BENCHMARK_SEED = 20260411L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val launchTarget = if (intent?.getStringExtra(EXTRA_LAUNCH_TARGET) == TARGET_BATTLE) {
            LaunchTarget.BATTLE
        } else {
            LaunchTarget.MENU
        }
        val benchmarkSeed = if (intent?.hasExtra(EXTRA_BENCHMARK_SEED) == true) {
            intent.getLongExtra(EXTRA_BENCHMARK_SEED, DEFAULT_BENCHMARK_SEED)
        } else {
            null
        }

        val config = AndroidApplicationConfiguration().apply {
            useImmersiveMode = true
            useGyroscope = false
            useAccelerometer = false
            numSamples = 2 // Multisampling for smoother edges
            useGL30 = false // Request GLES 2.0 for wider device compatibility
        }

        initialize(
            AirDefenseGame(GameLaunchConfig(launchTarget, benchmarkSeed)),
            config
        )
    }
}
