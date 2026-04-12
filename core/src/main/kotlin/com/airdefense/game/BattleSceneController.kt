package com.airdefense.game

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.graphics.g3d.environment.PointLight
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

internal const val BATTLE_CAMERA_FOV_DEGREES = 55f

private const val CAMERA_NEAR = 1f
private const val CAMERA_FAR_MULTIPLIER = 1.5f
private const val CAMERA_BASE_X = 280f
private const val CAMERA_BASE_Y = 380f
private const val CAMERA_BASE_Z = 1760f
private const val CAMERA_LOOK_AT_X = 980f
private const val CAMERA_LOOK_AT_Y = 120f
private const val CAMERA_LOOK_AT_Z = -2550f
private const val SHAKE_REFERENCE_DURATION = 0.5f
private const val LAUNCHER_PULSE_MAX = 1f
private const val LAUNCHER_PULSE_DECAY = 2.3f
private const val THREAT_PRESSURE_WEIGHT = 0.3f
private const val BASE_CITY_PULSE = 0.86f
private const val CITY_PULSE_SWAY_SPEED = 0.45f
private const val CITY_PULSE_SWAY_AMOUNT = 0.08f
private const val CITY_PULSE_THREAT_CAP = 0.3f
private const val CITY_PULSE_THREAT_SCALE = 0.018f
private const val CITY_GLOW_BASE_INTENSITY = 3200f
private const val CITY_GLOW_DRIFT_SPEED = 0.12f
private const val CITY_GLOW_DRIFT_DISTANCE = 90f
private const val LAUNCHER_IDLE_INTENSITY = 320f
private const val LAUNCHER_PULSE_INTENSITY = 780f
private const val LAUNCHER_LIGHT_RADIUS = 420f
private const val COLOR_BLEND_START_RED = 0.24f
private const val COLOR_BLEND_END_RED = 0.95f
private const val COLOR_BLEND_START_GREEN = 0.82f
private const val COLOR_BLEND_END_GREEN = 0.76f
private const val COLOR_BLEND_START_BLUE = 1f
private const val COLOR_BLEND_END_BLUE = 0.54f
private const val CITY_GLOW_BASE_X = 1180f
private const val CITY_GLOW_BASE_Y = 120f
private const val CITY_GLOW_BASE_Z = -1880f
private const val LEFT_LAUNCHER_LIGHT_X = -160f
private const val LEFT_LAUNCHER_LIGHT_Y = 18f
private const val LEFT_LAUNCHER_LIGHT_Z = 260f
private const val RIGHT_LAUNCHER_LIGHT_X = 210f
private const val RIGHT_LAUNCHER_LIGHT_Y = 18f
private const val RIGHT_LAUNCHER_LIGHT_Z = 220f
private const val PRIMARY_DIRECTION_X = -0.45f
private const val PRIMARY_DIRECTION_Y = -1f
private const val PRIMARY_DIRECTION_Z = -0.12f
private const val SECONDARY_DIRECTION_X = 0.3f
private const val SECONDARY_DIRECTION_Y = -0.16f
private const val SECONDARY_DIRECTION_Z = 0.4f

private val AMBIENT_LIGHT_COLOR = Color.valueOf("474D5CFF")
private val PRIMARY_DIRECTIONAL_COLOR = Color.valueOf("6B7A93FF")
private val SECONDARY_DIRECTIONAL_COLOR = Color.valueOf("2E3D57FF")
private val CITY_GLOW_BASE_COLOR = Color.valueOf("CC8A42FF")
private val LAUNCHER_IDLE_COLOR = Color.valueOf("3DD1FFFF")
private val CAMERA_BASE = Vector3(CAMERA_BASE_X, CAMERA_BASE_Y, CAMERA_BASE_Z)
private val CAMERA_LOOK_AT = Vector3(CAMERA_LOOK_AT_X, CAMERA_LOOK_AT_Y, CAMERA_LOOK_AT_Z)
private val CITY_GLOW_BASE_POSITION = Vector3(CITY_GLOW_BASE_X, CITY_GLOW_BASE_Y, CITY_GLOW_BASE_Z)
private val LEFT_LAUNCHER_LIGHT_POSITION =
    Vector3(
        LEFT_LAUNCHER_LIGHT_X,
        LEFT_LAUNCHER_LIGHT_Y,
        LEFT_LAUNCHER_LIGHT_Z,
    )
private val RIGHT_LAUNCHER_LIGHT_POSITION =
    Vector3(
        RIGHT_LAUNCHER_LIGHT_X,
        RIGHT_LAUNCHER_LIGHT_Y,
        RIGHT_LAUNCHER_LIGHT_Z,
    )

internal data class BattleSceneState(
    val threatCount: Int,
    val remainingThreatsInWave: Int,
    val liveTimeSeconds: Float,
)

internal data class BattleLauncherLightState(
    val intensity: Float,
    val color: Color,
)

internal data class BattleSceneRig(
    val environment: Environment,
    val camera: PerspectiveCamera,
    val impactLight: PointLight,
    val cityGlowLight: PointLight,
    val launcherLeftLight: PointLight,
    val launcherRightLight: PointLight,
)

internal fun battleLauncherPulse(
    currentPulse: Float,
    dt: Float,
): Float = max(0f, currentPulse - dt * LAUNCHER_PULSE_DECAY)

internal fun battleCityPulse(sceneState: BattleSceneState): Float {
    val wavePressure =
        (sceneState.threatCount + sceneState.remainingThreatsInWave * THREAT_PRESSURE_WEIGHT)
            .coerceAtLeast(0f)
    return BASE_CITY_PULSE +
        sin(sceneState.liveTimeSeconds * CITY_PULSE_SWAY_SPEED) * CITY_PULSE_SWAY_AMOUNT +
        min(CITY_PULSE_THREAT_CAP, wavePressure * CITY_PULSE_THREAT_SCALE)
}

internal fun battleCityGlowX(liveTimeSeconds: Float): Float =
    CITY_GLOW_BASE_POSITION.x + sin(liveTimeSeconds * CITY_GLOW_DRIFT_SPEED) * CITY_GLOW_DRIFT_DISTANCE

internal fun battleLauncherLightState(
    pulse: Float,
    lightIntensityScale: Float,
): BattleLauncherLightState =
    BattleLauncherLightState(
        intensity = (LAUNCHER_IDLE_INTENSITY + pulse * LAUNCHER_PULSE_INTENSITY) * lightIntensityScale,
        color =
            Color(
                MathUtils.lerp(COLOR_BLEND_START_RED, COLOR_BLEND_END_RED, pulse),
                MathUtils.lerp(COLOR_BLEND_START_GREEN, COLOR_BLEND_END_GREEN, pulse),
                MathUtils.lerp(COLOR_BLEND_START_BLUE, COLOR_BLEND_END_BLUE, pulse),
                1f,
            ),
    )

internal fun battleShakeAmount(
    shakeIntensity: Float,
    remainingShakeTime: Float,
): Float = shakeIntensity * (remainingShakeTime / SHAKE_REFERENCE_DURATION).coerceIn(0f, 1f)

internal class BattleSceneController(
    private val qualityProfile: GraphicsQualityProfile,
    private val sceneRig: BattleSceneRig,
    private val randomOffsetProvider: (Float) -> Float = { amount -> MathUtils.random(-amount, amount) },
) {
    private var shakeTime = 0f
    private var shakeIntensity = 0f
    private var launcherLeftPulse = 0f
    private var launcherRightPulse = 0f

    fun setupEnvironment() {
        sceneRig.environment.set(ColorAttribute(ColorAttribute.AmbientLight, AMBIENT_LIGHT_COLOR))
        sceneRig.environment.add(
            DirectionalLight().set(
                PRIMARY_DIRECTIONAL_COLOR,
                PRIMARY_DIRECTION_X,
                PRIMARY_DIRECTION_Y,
                PRIMARY_DIRECTION_Z,
            ),
        )
        sceneRig.environment.add(
            DirectionalLight().set(
                SECONDARY_DIRECTIONAL_COLOR,
                SECONDARY_DIRECTION_X,
                SECONDARY_DIRECTION_Y,
                SECONDARY_DIRECTION_Z,
            ),
        )
        sceneRig.impactLight.set(Color.BLACK, Vector3.Zero, 0f)
        sceneRig.cityGlowLight.set(CITY_GLOW_BASE_COLOR, CITY_GLOW_BASE_POSITION.cpy(), CITY_GLOW_BASE_INTENSITY)
        sceneRig.launcherLeftLight.set(LAUNCHER_IDLE_COLOR, LEFT_LAUNCHER_LIGHT_POSITION.cpy(), LAUNCHER_LIGHT_RADIUS)
        sceneRig.launcherRightLight.set(
            LAUNCHER_IDLE_COLOR,
            RIGHT_LAUNCHER_LIGHT_POSITION.cpy(),
            LAUNCHER_LIGHT_RADIUS,
        )
        sceneRig.environment.add(sceneRig.impactLight)
        sceneRig.environment.add(sceneRig.cityGlowLight)
        sceneRig.environment.add(sceneRig.launcherLeftLight)
        sceneRig.environment.add(sceneRig.launcherRightLight)
    }

    fun setupCamera(worldRadius: Float) {
        sceneRig.camera.near = CAMERA_NEAR
        sceneRig.camera.far = worldRadius * CAMERA_FAR_MULTIPLIER
        sceneRig.camera.position.set(CAMERA_BASE)
        sceneRig.camera.lookAt(CAMERA_LOOK_AT)
        sceneRig.camera.update()
    }

    fun pulseLauncher(launcherIndex: Int) {
        when (launcherIndex) {
            0 -> launcherLeftPulse = LAUNCHER_PULSE_MAX
            1 -> launcherRightPulse = LAUNCHER_PULSE_MAX
        }
    }

    fun triggerShake(
        intensity: Float,
        duration: Float,
    ) {
        shakeIntensity = max(shakeIntensity, intensity)
        shakeTime = max(shakeTime, duration)
    }

    fun update(
        dt: Float,
        sceneState: BattleSceneState,
    ) {
        updateCameraShake(dt)
        updateSceneLights(dt, sceneState)
    }

    private fun updateCameraShake(dt: Float) {
        if (shakeTime > 0f) {
            shakeTime -= dt
            val amount = battleShakeAmount(shakeIntensity, shakeTime)
            sceneRig.camera.position.set(CAMERA_BASE).add(
                randomOffsetProvider(amount),
                randomOffsetProvider(amount),
                randomOffsetProvider(amount),
            )
        } else {
            sceneRig.camera.position.set(CAMERA_BASE)
        }
        sceneRig.camera.lookAt(CAMERA_LOOK_AT)
        sceneRig.camera.update()
    }

    private fun updateSceneLights(
        dt: Float,
        sceneState: BattleSceneState,
    ) {
        launcherLeftPulse = battleLauncherPulse(launcherLeftPulse, dt)
        launcherRightPulse = battleLauncherPulse(launcherRightPulse, dt)

        val cityPulse = battleCityPulse(sceneState)
        sceneRig.cityGlowLight.intensity = CITY_GLOW_BASE_INTENSITY * cityPulse * qualityProfile.lightIntensityScale
        sceneRig.cityGlowLight.position.set(
            battleCityGlowX(sceneState.liveTimeSeconds),
            CITY_GLOW_BASE_POSITION.y,
            CITY_GLOW_BASE_POSITION.z,
        )

        val leftLightState = battleLauncherLightState(launcherLeftPulse, qualityProfile.lightIntensityScale)
        val rightLightState = battleLauncherLightState(launcherRightPulse, qualityProfile.lightIntensityScale)
        sceneRig.launcherLeftLight.intensity = leftLightState.intensity
        sceneRig.launcherRightLight.intensity = rightLightState.intensity
        sceneRig.launcherLeftLight.color.set(leftLightState.color)
        sceneRig.launcherRightLight.color.set(rightLightState.color)
    }
}
