package com.airdefense.game

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.environment.PointLight
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.viewport.ScreenViewport
import kotlin.math.min

class BattleScreen(
    private val game: AirDefenseGame,
) : ScreenAdapter() {
    private val qualityProfile =
        GraphicsQualityProfiles.resolve(
            requestedMode = game.launchConfig.graphicsQualityMode,
            deviceClass = game.launchConfig.devicePerformanceClass,
        )
    private val environment = Environment()
    private val impactLight = PointLight()
    private val cityGlowLight = PointLight()
    private val launcherLeftLight = PointLight()
    private val launcherRightLight = PointLight()
    private val useSafeAndroidRenderer = Gdx.app.type == Application.ApplicationType.Android
    private val useImportedLandmarks = Gdx.app.type != Application.ApplicationType.Android
    private val modelBatch = if (useSafeAndroidRenderer) ModelBatch() else ModelBatch(NightShaderProvider(impactLight))
    private val camera =
        PerspectiveCamera(
            BATTLE_CAMERA_FOV_DEGREES,
            Gdx.graphics.width.toFloat(),
            Gdx.graphics.height.toFloat(),
        )
    private val stage = Stage(ScreenViewport())
    private val settings = DefenseSettings()
    private val textures = Array<Texture>()
    private val skin = TacticalUiSkin.create(textures, TacticalUiDensity.BATTLE)
    private val whiteRegion by lazy {
        skin.get("white_region", com.badlogic.gdx.graphics.g2d.TextureRegion::class.java)
    }
    private val sceneRenderer by lazy { BattleSceneRenderer(stage, skin, whiteRegion) }

    private val models = ObjectMap<String, Model>()
    private val instances = Array<ModelInstance>()
    private val launchers = Array<ModelInstance>()
    private val cityBlocks = Array<BuildingEntity>()
    private val cityBlocksById = ObjectMap<String, BuildingEntity>()
    private val threats = Array<ThreatEntity>()
    private val threatsById = ObjectMap<String, ThreatEntity>()
    private val interceptors = Array<InterceptorEntity>()
    private val interceptorsById = ObjectMap<String, InterceptorEntity>()
    private lateinit var simulation: BattleSimulation
    private val effectsController by lazy {
        BattleEffectsController(
            models = models,
            qualityProfile = qualityProfile,
            impactLight = impactLight,
        )
    }
    private val sceneController by lazy {
        BattleSceneController(
            qualityProfile = qualityProfile,
            sceneRig =
                BattleSceneRig(
                    environment = environment,
                    camera = camera,
                    impactLight = impactLight,
                    cityGlowLight = cityGlowLight,
                    launcherLeftLight = launcherLeftLight,
                    launcherRightLight = launcherRightLight,
                ),
        )
    }
    private val projectileVisualController by lazy {
        BattleProjectileVisualController(
            launchers = launchers,
            threatsById = threatsById,
            interceptorsById = interceptorsById,
            threatScale = THREAT_SCALE,
            interceptorScale = INTERCEPTOR_SCALE,
            pulseLauncherLight = sceneController::pulseLauncher,
        )
    }
    private val buildingVisualController by lazy {
        BattleBuildingVisualController(
            effectsController = effectsController,
            threatCountProvider = { threats.size },
            interceptorCountProvider = { interceptors.size },
        )
    }
    private val worldBootstrapper by lazy {
        BattleWorldBootstrapper(
            collections =
                BattleWorldBootstrapCollections(
                    models = models,
                    instances = instances,
                    launchers = launchers,
                    cityBlocks = cityBlocks,
                    cityBlocksById = cityBlocksById,
                ),
            config =
                BattleWorldBootstrapConfig(
                    settings = settings,
                    benchmarkSeed = game.launchConfig.benchmarkSeed,
                    useImportedLandmarks = useImportedLandmarks,
                ),
        )
    }
    private val assetBootstrapper by lazy {
        BattleAssetBootstrapper(
            qualityProfile = qualityProfile,
            textures = textures,
            models = models,
            sceneRenderer = sceneRenderer,
            worldBootstrapper = worldBootstrapper,
        )
    }
    private val audioController = BattleAudioController()
    private val hudController by lazy {
        BattleHudController(
            stage = stage,
            uiSkin = skin,
            settings = settings,
            onSettingsChanged = ::refreshHud,
            onStartWaveRequested = ::startNewWave,
        )
    }
    private val simulationStepApplier by lazy {
        BattleSimulationStepApplier(
            simulationProvider = { simulation },
            renderCollections =
                BattleRenderCollections(
                    models = models,
                    launchers = launchers,
                    cityBlocksById = cityBlocksById,
                    threats = threats,
                    threatsById = threatsById,
                    interceptors = interceptors,
                    interceptorsById = interceptorsById,
                ),
            callbacks =
                BattleStepCallbacks(
                    canSpawnTrail = { hostile ->
                        effectsController.canSpawnTrail(hostile, threats.size, interceptors.size)
                    },
                    spawnTrail = effectsController::spawnTrail,
                    syncProjectileTransform = projectileVisualController::syncProjectileTransform,
                    syncRenderEntitiesFromSimulation = {
                        projectileVisualController.syncRenderEntitiesFromSimulation(simulation)
                    },
                    syncBattleStateFromSimulation = {
                        applyRuntimeSnapshot(battleRuntimeSnapshot(simulation))
                    },
                    pulseLauncher = projectileVisualController::pulseLauncher,
                    spawnBlast = { position, size ->
                        effectsController.spawnBlast(position, size, threats.size, interceptors.size)
                    },
                    spawnDebris = { position, count, color ->
                        effectsController.spawnDebris(position, count, color, threats.size, interceptors.size)
                    },
                    triggerShake = sceneController::triggerShake,
                    playSfx = audioController::play,
                    applyBuildingDamageVisual = buildingVisualController::applyDamageVisual,
                    setStatus = hudController::setStatus,
                    syncHud = ::refreshHud,
                ),
            threatScale = THREAT_SCALE,
            interceptorScale = INTERCEPTOR_SCALE,
        )
    }
    private val initializationController by lazy {
        BattleInitializationController(
            tasks =
                listOf(
                    InitializationTask("Bringing command network online...") {
                        sceneController.setupEnvironment()
                        sceneController.setupCamera(WORLD_RADIUS)
                    },
                    InitializationTask("Synthesizing coast and district materials...") {
                        assetBootstrapper.loadTerrain()
                    },
                    InitializationTask("Constructing skyline architecture...") {
                        assetBootstrapper.loadBuildings()
                    },
                    InitializationTask("Deploying launchers and radar...") {
                        assetBootstrapper.loadDefenseAssets()
                    },
                    InitializationTask("Arming missiles and effects...") {
                        assetBootstrapper.loadProjectileAssets()
                    },
                    InitializationTask("Loading imported landmark geometry...") {
                        assetBootstrapper.loadImportedLandmarks()
                    },
                    InitializationTask("Positioning launch sites and city blocks...") {
                        simulation = assetBootstrapper.createWorld(buildingVisualController::syncTransform)
                        applyRuntimeSnapshot(battleRuntimeSnapshot(simulation))
                    },
                    InitializationTask("Calibrating engagement controls...") {
                        hudController.build()
                        audioController.loadDefaults()
                        startNewWave()
                    },
                ),
            qualityLabel = qualityProfile.label,
        )
    }

    private var credits = INITIAL_CREDITS
    private var wave = 1
    private var score = 0
    private var cityIntegrity = INITIAL_CITY_INTEGRITY
    private var waveInProgress = false
    private var isGameOver = false
    private var threatsRemainingInWave = 0
    private var radarScanProgress = 0f
    private val frameTelemetry = BattleFrameTelemetry()

    private companion object {
        private const val WORLD_RADIUS = 9000f
        private const val THREAT_SCALE = 3.2f
        private const val INTERCEPTOR_SCALE = 3.4f
        private const val INITIAL_CREDITS = 10_000
        private const val INITIAL_CITY_INTEGRITY = 100f
        private const val MAX_SIMULATION_STEP_SECONDS = 1f / 30f
        private const val RADAR_SCAN_SPEED = 0.42f
        private const val MIN_VISIBLE_BUILDING_RENDER_HEIGHT = 1f
    }

    init {
        Gdx.graphics.isContinuousRendering = true
        Gdx.app.log(
            "BattleQuality",
            "requested=${game.launchConfig.graphicsQualityMode} " +
                "effective=${qualityProfile.label} " +
                "deviceClass=${game.launchConfig.devicePerformanceClass}",
        )
        stage.addListener(
            object : InputAdapter(), com.badlogic.gdx.scenes.scene2d.EventListener {
                override fun handle(event: com.badlogic.gdx.scenes.scene2d.Event?): Boolean {
                    if (isGameOver && event is InputEvent && event.type == InputEvent.Type.touchDown) {
                        game.screen = StartScreen(game)
                        return true
                    }
                    return false
                }
            },
        )
        Gdx.input.inputProcessor = stage
    }

    private fun startNewWave() {
        if (!simulation.startNewWave()) return
        applyRuntimeSnapshot(battleRuntimeSnapshot(simulation))
        hudController.setStatus("critical", "MULTIPLE INBOUND TRACKS // WAVE $wave")
        refreshHud()
    }

    override fun render(delta: Float) {
        if (!initializationController.isComplete) {
            val frame = initializationController.advance()
            sceneRenderer.renderLoading(
                frame.message,
                frame.diagnosticsLine,
                frame.progress,
            )
            return
        }

        if (isGameOver) {
            sceneRenderer.renderGameOver(score)
            return
        }

        updateLogic(min(delta, MAX_SIMULATION_STEP_SECONDS))
        frameTelemetry
            .onFrame(
                delta,
                buildBattleFrameSummaryInput(
                    qualityLabel = qualityProfile.label,
                    effectCount = effectsController.effects.size,
                    threatCount = threats.size,
                    interceptorCount = interceptors.size,
                ),
            )?.let { summary ->
                Gdx.app.log("BattleFrame", summary)
            }
        sceneRenderer.renderBackdrop(
            battleLiveTime = frameTelemetry.liveTimeSeconds,
            impactLightIntensity = impactLight.intensity,
        )
        BattleWorldRenderPass(
            modelBatch = modelBatch,
            camera = camera,
            environment = environment,
            instances = instances,
            cityBlocks = cityBlocks,
            threats = threats,
            interceptors = interceptors,
            debris = effectsController.debris,
            effects = effectsController.effects,
            minimumVisibleBuildingHeight = MIN_VISIBLE_BUILDING_RENDER_HEIGHT,
        ).render()
        sceneRenderer.renderAtmosphere()

        stage.act(delta)
        stage.draw()
        sceneRenderer.renderOverlay(threats, interceptors, camera, radarScanProgress)
    }

    private fun updateLogic(dt: Float) {
        radarScanProgress = (radarScanProgress + dt * RADAR_SCAN_SPEED) % 1f
        val step = simulation.step(dt)
        simulationStepApplier.apply(step)
        effectsController.update(dt)
        buildingVisualController.update(cityBlocks, dt)
        applyRuntimeSnapshot(battleRuntimeSnapshot(simulation))
        sceneController.update(
            dt = dt,
            sceneState =
                BattleSceneState(
                    threatCount = threats.size,
                    remainingThreatsInWave = threatsRemainingInWave,
                    liveTimeSeconds = frameTelemetry.liveTimeSeconds,
                ),
        )
        refreshHud()

        if (isGameOver) {
            isGameOver = true
            hudController.setStatus("critical", "DEFENSE FAILED")
            refreshHud()
        }
    }

    private fun applyRuntimeSnapshot(snapshot: BattleRuntimeSnapshot) {
        credits = snapshot.credits
        wave = snapshot.wave
        score = snapshot.score
        cityIntegrity = snapshot.cityIntegrity
        waveInProgress = snapshot.waveInProgress
        isGameOver = snapshot.isGameOver
        threatsRemainingInWave = snapshot.threatsRemainingInWave
    }

    private fun refreshHud() {
        hudController.update(
            buildBattleHudSnapshot(
                snapshot =
                    BattleRuntimeSnapshot(
                        credits = credits,
                        wave = wave,
                        score = score,
                        cityIntegrity = cityIntegrity,
                        waveInProgress = waveInProgress,
                        isGameOver = isGameOver,
                        threatsRemainingInWave = threatsRemainingInWave,
                    ),
                visibleThreats = threats.size,
                settings = settings,
            ),
        )
    }

    override fun resize(
        width: Int,
        height: Int,
    ) {
        stage.viewport.update(width, height, true)
        camera.viewportWidth = width.toFloat()
        camera.viewportHeight = height.toFloat()
        camera.update()
        hudController.build()
        refreshHud()
    }

    override fun dispose() {
        modelBatch.dispose()
        stage.dispose()
        skin.dispose()
        assetBootstrapper.dispose()
        models.values().forEach { it.dispose() }
        audioController.dispose()
    }
}

enum class EffectType { BLAST, SHOCKWAVE, SMOKE, SPARK, TRAIL }

data class BuildingEntity(
    val id: String,
    val instance: ModelInstance,
    val modelName: String,
    val position: Vector3,
    val yaw: Float,
    val baseHeight: Float,
    val width: Float,
    val depth: Float,
    var integrity: Float,
    var visibleHeight: Float = baseHeight,
    var lean: Float = 0f,
    var leanTarget: Float = 0f,
    var collapseVelocity: Float = 0f,
)

data class ThreatEntity(
    val instance: ModelInstance,
    val position: Vector3,
    val targetPosition: Vector3,
    val velocity: Vector3,
    val id: String,
    var isTracked: Boolean = false,
    var trailCooldown: Float = 0f,
)

data class InterceptorEntity(
    val id: String,
    val instance: ModelInstance,
    val position: Vector3,
    val velocity: Vector3,
    var target: ThreatEntity?,
    var trailCooldown: Float = 0f,
)

data class VisualEffect(
    val instance: ModelInstance,
    val position: Vector3,
    var life: Float,
    val initialLife: Float,
    val type: EffectType,
    val maxScale: Float,
    val velocity: Vector3 = Vector3(),
)

data class DebrisEntity(
    val instance: ModelInstance,
    val position: Vector3,
    val velocity: Vector3,
    var life: Float,
    val scale: Float,
    val rotation: Vector3 = Vector3(),
)
