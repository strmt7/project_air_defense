package com.airdefense.game

import com.badlogic.gdx.Game

class AirDefenseGame : Game() {
    override fun create() {
        setScreen(StartScreen(this))
    }
}
