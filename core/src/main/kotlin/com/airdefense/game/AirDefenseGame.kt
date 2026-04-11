package com.airdefense.game

import com.badlogic.gdx.Game

class AirDefenseGame(
    val launchConfig: GameLaunchConfig = GameLaunchConfig(),
) : Game() {
    override fun create() {
        when (launchConfig.launchTarget) {
            LaunchTarget.MENU -> setScreen(StartScreen(this))
            LaunchTarget.BATTLE -> setScreen(BattleScreen(this))
        }
    }
}
