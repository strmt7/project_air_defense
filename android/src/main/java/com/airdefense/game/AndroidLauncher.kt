package com.airdefense.game

import android.os.Bundle
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration

class AndroidLauncher : AndroidApplication() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initialize(AirDefenseGame(), AndroidApplicationConfiguration().apply {
            useImmersiveMode = true
            useGyroscope = false
            useAccelerometer = false
            numSamples = 2 // Multisampling for smoother edges
            useGL30 = false // Request GLES 2.0 for wider device compatibility
        })
    }
}
