package com.airdefense.game

import android.app.ActivityManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration

class AndroidLauncher : AndroidApplication() {
    companion object {
        const val EXTRA_LAUNCH_TARGET = "com.airdefense.game.extra.LAUNCH_TARGET"
        const val EXTRA_BENCHMARK_SEED = "com.airdefense.game.extra.BENCHMARK_SEED"
        const val EXTRA_QUALITY_MODE = "com.airdefense.game.extra.QUALITY_MODE"

        private const val TARGET_BATTLE = "battle"
        private const val DEFAULT_BENCHMARK_SEED = 20260411L
        private const val DEFAULT_HEAP_CLASS_MB = 192
        private const val MID_HEAP_CLASS_MB = 256
        private const val HIGH_HEAP_CLASS_MB = 384
        private const val DEVICE_PROFILE_TAG = "DeviceProfile"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val launchTarget =
            if (intent?.getStringExtra(EXTRA_LAUNCH_TARGET) == TARGET_BATTLE) {
                LaunchTarget.BATTLE
            } else {
                LaunchTarget.MENU
            }
        val benchmarkSeed =
            if (intent?.hasExtra(EXTRA_BENCHMARK_SEED) == true) {
                intent.getLongExtra(EXTRA_BENCHMARK_SEED, DEFAULT_BENCHMARK_SEED)
            } else {
                null
            }
        val deviceClass = detectDevicePerformanceClass()
        val requestedQualityMode =
            GraphicsQualityMode.from(intent?.getStringExtra(EXTRA_QUALITY_MODE))
                ?: GraphicsQualityMode.AUTO
        val effectiveQualityMode = GraphicsQualityProfiles.effectiveMode(requestedQualityMode, deviceClass)

        val config =
            AndroidApplicationConfiguration().apply {
                useImmersiveMode = true
                useGyroscope = false
                useAccelerometer = false
                numSamples = if (effectiveQualityMode == GraphicsQualityMode.PERFORMANCE) 0 else 2
                useGL30 = false // Request GLES 2.0 for wider device compatibility
            }

        initialize(
            AirDefenseGame(
                GameLaunchConfig(
                    launchTarget = launchTarget,
                    benchmarkSeed = benchmarkSeed,
                    graphicsQualityMode = requestedQualityMode,
                    devicePerformanceClass = deviceClass,
                ),
            ),
            config,
        )
    }

    private fun detectDevicePerformanceClass(): DevicePerformanceClass {
        val activityManager = getSystemService(ACTIVITY_SERVICE) as? ActivityManager
        val heapClassMb = resolveHeapClassMb(activityManager)
        val isLowRamDevice = activityManager?.isLowRamDevice == true
        val resolved =
            resolveDevicePerformanceClass(
                isLowRamDevice = isLowRamDevice,
                heapClassMb = heapClassMb,
                highHeapClassMb = HIGH_HEAP_CLASS_MB,
                midHeapClassMb = MID_HEAP_CLASS_MB,
            )
        Log.i(
            DEVICE_PROFILE_TAG,
            "emulator=${isProbablyEmulator()} lowRam=$isLowRamDevice heapClassMb=$heapClassMb resolved=$resolved",
        )
        return resolved
    }

    private fun resolveHeapClassMb(activityManager: ActivityManager?): Int =
        activityManager?.largeMemoryClass ?: activityManager?.memoryClass ?: DEFAULT_HEAP_CLASS_MB

    private fun isProbablyEmulator(): Boolean =
        Build.FINGERPRINT.contains("generic", ignoreCase = true) ||
            Build.MODEL.contains("Emulator", ignoreCase = true) ||
            Build.MODEL.contains("Android SDK built for", ignoreCase = true) ||
            Build.HARDWARE.contains("ranchu", ignoreCase = true) ||
            Build.MANUFACTURER.contains("Genymotion", ignoreCase = true) ||
            Build.PRODUCT.contains("sdk", ignoreCase = true)
}

internal fun resolveDevicePerformanceClass(
    isLowRamDevice: Boolean,
    heapClassMb: Int,
    highHeapClassMb: Int,
    midHeapClassMb: Int,
): DevicePerformanceClass =
    when {
        isLowRamDevice -> DevicePerformanceClass.LOW
        heapClassMb >= highHeapClassMb -> DevicePerformanceClass.HIGH
        heapClassMb >= midHeapClassMb -> DevicePerformanceClass.MID
        else -> DevicePerformanceClass.LOW
    }
