package com.airdefense.game

import com.badlogic.gdx.Game
import com.badlogic.gdx.physics.bullet.Bullet

class AirDefenseGame : Game() {
    override fun create() {
        Bullet.init()
        setScreen(StartScreen(this))
    }
}
