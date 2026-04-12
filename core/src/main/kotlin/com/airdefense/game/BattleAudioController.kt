package com.airdefense.game

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.utils.ObjectMap

internal class BattleAudioController {
    private val sounds = ObjectMap<String, Sound>()

    fun loadDefaults() {
        loadSound("launch", "sfx/launch.mp3")
        loadSound("detonate", "sfx/detonate.mp3")
        loadSound("impact", "sfx/impact.mp3")
    }

    fun play(
        name: String,
        volume: Float,
    ) {
        sounds[name]?.play(volume)
    }

    fun dispose() {
        sounds.values().forEach { it.dispose() }
    }

    private fun loadSound(
        key: String,
        path: String,
    ) {
        val file = Gdx.files.internal(path)
        if (file.exists()) {
            sounds.put(key, Gdx.audio.newSound(file))
        }
    }
}
