package com.airdefense.game

enum class GraphicsQualityMode {
    AUTO,
    PERFORMANCE,
    BALANCED,
    HIGH,
    ;

    companion object {
        fun from(value: String?): GraphicsQualityMode? =
            entries.firstOrNull { mode ->
                mode.name.equals(value, ignoreCase = true)
            }
    }
}

enum class DevicePerformanceClass {
    EMULATOR,
    LOW,
    MID,
    HIGH,
}

data class GraphicsQualityProfile(
    val effectiveMode: GraphicsQualityMode,
    val label: String,
    val sceneTextureScale: Float,
    val facadeTextureScale: Float,
    val showAtmosphereLayers: Boolean,
    val showGlowLayer: Boolean,
    val showReflectionLayer: Boolean,
    val showMoon: Boolean,
    val effectBudgetScale: Float,
    val baseSparkCount: Int,
    val baseSmokeCount: Int,
    val maxDebrisPieces: Int,
    val maxTrailEffects: Int,
    val hostileTrailStride: Int,
    val interceptorTrailStride: Int,
    val lightIntensityScale: Float,
)

private val performanceProfile =
    GraphicsQualityProfile(
        effectiveMode = GraphicsQualityMode.PERFORMANCE,
        label = "PERFORMANCE",
        sceneTextureScale = 0.5f,
        facadeTextureScale = 0.5f,
        showAtmosphereLayers = true,
        showGlowLayer = true,
        showReflectionLayer = false,
        showMoon = false,
        effectBudgetScale = 0.62f,
        baseSparkCount = 3,
        baseSmokeCount = 2,
        maxDebrisPieces = 10,
        maxTrailEffects = 120,
        hostileTrailStride = 1,
        interceptorTrailStride = 2,
        lightIntensityScale = 0.8f,
    )

private val balancedProfile =
    GraphicsQualityProfile(
        effectiveMode = GraphicsQualityMode.BALANCED,
        label = "BALANCED",
        sceneTextureScale = 0.75f,
        facadeTextureScale = 0.75f,
        showAtmosphereLayers = true,
        showGlowLayer = true,
        showReflectionLayer = true,
        showMoon = true,
        effectBudgetScale = 0.86f,
        baseSparkCount = 5,
        baseSmokeCount = 3,
        maxDebrisPieces = 18,
        maxTrailEffects = 180,
        hostileTrailStride = 1,
        interceptorTrailStride = 1,
        lightIntensityScale = 0.92f,
    )

private val highProfile =
    GraphicsQualityProfile(
        effectiveMode = GraphicsQualityMode.HIGH,
        label = "HIGH",
        sceneTextureScale = 1f,
        facadeTextureScale = 1f,
        showAtmosphereLayers = true,
        showGlowLayer = true,
        showReflectionLayer = true,
        showMoon = true,
        effectBudgetScale = 1f,
        baseSparkCount = 6,
        baseSmokeCount = 4,
        maxDebrisPieces = 26,
        maxTrailEffects = 240,
        hostileTrailStride = 1,
        interceptorTrailStride = 1,
        lightIntensityScale = 1f,
    )

private val profilesByMode =
    mapOf(
        GraphicsQualityMode.PERFORMANCE to performanceProfile,
        GraphicsQualityMode.BALANCED to balancedProfile,
        GraphicsQualityMode.HIGH to highProfile,
    )

object GraphicsQualityProfiles {
    fun effectiveMode(
        requestedMode: GraphicsQualityMode,
        deviceClass: DevicePerformanceClass,
    ): GraphicsQualityMode =
        when (requestedMode) {
            GraphicsQualityMode.AUTO -> autoModeFor(deviceClass)
            else -> requestedMode
        }

    fun resolve(
        requestedMode: GraphicsQualityMode,
        deviceClass: DevicePerformanceClass,
    ): GraphicsQualityProfile = profilesByMode.getValue(effectiveMode(requestedMode, deviceClass))

    private fun autoModeFor(deviceClass: DevicePerformanceClass): GraphicsQualityMode =
        when (deviceClass) {
            DevicePerformanceClass.EMULATOR,
            DevicePerformanceClass.LOW,
            -> GraphicsQualityMode.PERFORMANCE

            DevicePerformanceClass.MID,
            DevicePerformanceClass.HIGH,
            -> GraphicsQualityMode.BALANCED
        }
}
