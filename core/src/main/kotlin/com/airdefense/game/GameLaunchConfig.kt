package com.airdefense.game

enum class LaunchTarget {
    MENU,
    BATTLE
}

data class GameLaunchConfig(
    val launchTarget: LaunchTarget = LaunchTarget.MENU,
    val benchmarkSeed: Long? = null
)
