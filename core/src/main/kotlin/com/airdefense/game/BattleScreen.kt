package com.airdefense.game

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.graphics.g3d.environment.PointLight
import com.badlogic.gdx.graphics.g3d.loader.ObjLoader
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.viewport.ScreenViewport
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
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
    private val camera = PerspectiveCamera(55f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
    private val stage = Stage(ScreenViewport())
    private val settings = DefenseSettings()
    private val textures = Array<Texture>()
    private val skin = TacticalUiSkin.create(textures, TacticalUiDensity.BATTLE)
    private val whiteRegion by lazy { skin.get("white_region", com.badlogic.gdx.graphics.g2d.TextureRegion::class.java) }
    private val sceneRenderer by lazy { BattleSceneRenderer(stage, skin, whiteRegion) }
    private val textureFactory = BattleTextureFactory(qualityProfile, ::registerTexture)
    private val surfaceTextures by lazy { textureFactory.surfaces }
    private val backdropTextures by lazy { textureFactory.backdrop }
    private val terrainAssetFactory by lazy { BattleTerrainAssetFactory(qualityProfile, surfaceTextures, backdropTextures) }
    private val buildingAssetFactory by lazy { BattleBuildingAssetFactory(surfaceTextures) }
    private val defenseAssetFactory by lazy { BattleDefenseAssetFactory(surfaceTextures) }
    private val projectileAssetFactory by lazy { BattleProjectileAssetFactory(qualityProfile, surfaceTextures) }
    private var battleSkyTexture: Texture? = null
    private var battleSkyRegion: TextureRegion? = null
    private var battleHorizonTexture: Texture? = null
    private var battleFogTexture: Texture? = null
    private var battleGlowTexture: Texture? = null
    private var battleReflectionTexture: Texture? = null

    private val models = ObjectMap<String, Model>()
    private val instances = Array<ModelInstance>()
    private val launchers = Array<ModelInstance>()
    private val cityBlocks = Array<BuildingEntity>()
    private val cityBlocksById = ObjectMap<String, BuildingEntity>()
    private val threats = Array<ThreatEntity>()
    private val threatsById = ObjectMap<String, ThreatEntity>()
    private val interceptors = Array<InterceptorEntity>()
    private val interceptorsById = ObjectMap<String, InterceptorEntity>()
    private val effects = Array<VisualEffect>()
    private val debris = Array<DebrisEntity>()
    private val sounds = ObjectMap<String, Sound>()
    private lateinit var simulation: BattleSimulation
    private val hudController by lazy {
        BattleHudController(
            stage = stage,
            uiSkin = skin,
            settings = settings,
            onSettingsChanged = ::syncHud,
            onStartWaveRequested = ::handleStartWaveRequest,
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
                    canSpawnTrail = ::canSpawnTrail,
                    spawnTrail = ::spawnTrail,
                    syncProjectileTransform = ::syncProjectileTransform,
                    syncRenderEntitiesFromSimulation = ::syncRenderEntitiesFromSimulation,
                    syncBattleStateFromSimulation = ::syncBattleStateFromSimulation,
                    pulseLauncher = ::pulseLauncher,
                    spawnBlast = ::spawnBlast,
                    spawnDebris = ::spawnDebris,
                    triggerShake = ::triggerShake,
                    playSfx = ::playSfx,
                    applyBuildingDamageVisual = ::applyBuildingDamageVisual,
                    setStatus = ::setStatus,
                    syncHud = ::syncHud,
                ),
            threatScale = THREAT_SCALE,
            interceptorScale = INTERCEPTOR_SCALE,
        )
    }

    private val cameraBase = Vector3(280f, 380f, 1760f)
    private val cameraLookAt = Vector3(980f, 120f, -2550f)
    private val tempA = Vector3()
    private val tempB = Vector3()
    private val tempC = Vector3()
    private val tempD = Vector3()

    private var credits = 10000
    private var wave = 1
    private var score = 0
    private var cityIntegrity = 100f
    private var waveInProgress = false
    private var isGameOver = false
    private var threatsRemainingInWave = 0
    private var radarScanProgress = 0f
    private var shakeTime = 0f
    private var shakeIntensity = 0f
    private var initializationStep = 0
    private var initialized = false
    private var loadingMessage = "Initializing battle systems..."
    private var lastInitializationDurationMs = 0L
    private var battleLiveTime = 0f
    private var launcherLeftPulse = 0f
    private var launcherRightPulse = 0f
    private var hostileTrailSamples = 0
    private var interceptorTrailSamples = 0
    private val frameTimeWindowMs = FloatArray(180)
    private var frameTimeWindowCursor = 0
    private var frameTimeWindowSize = 0
    private var battleFrameLogTimer = 0f

    private val initializationTasks by lazy {
        listOf(
            InitializationTask("Bringing command network online...") {
                setupEnvironment()
                setupCamera()
            },
            InitializationTask("Synthesizing coast and district materials...") {
                generateTerrainModels()
            },
            InitializationTask("Constructing skyline architecture...") {
                generateBuildingModels()
            },
            InitializationTask("Deploying launchers and radar...") {
                generateDefenseModels()
            },
            InitializationTask("Arming missiles and effects...") {
                generateProjectileModels()
            },
            InitializationTask("Loading imported landmark geometry...") {
                loadImportedModels()
            },
            InitializationTask("Positioning launch sites and city blocks...") {
                createWorldInstances()
            },
            InitializationTask("Calibrating engagement controls...") {
                setupHud()
                loadAudio()
                startNewWave()
                initialized = true
            },
        )
    }

    private companion object {
        private const val WORLD_RADIUS = 9000f
        private const val THREAT_SCALE = 3.2f
        private const val INTERCEPTOR_SCALE = 3.4f
    }

    private fun pulseLauncher(
        launch: InterceptorLaunchEvent,
        launchVelocity: Vector3,
    ) {
        if (launch.launcherIndex in 0 until launchers.size) {
            launchers[launch.launcherIndex].setRotationToward(launchVelocity)
        }
        when (launch.launcherIndex) {
            0 -> launcherLeftPulse = 1f
            1 -> launcherRightPulse = 1f
        }
    }

    private fun currentEffectBudgetScale(): Float {
        val scenePressure = threats.size + interceptors.size * 0.75f + effects.size / 18f
        val pressureScale =
            when {
                scenePressure >= 18f -> 0.62f
                scenePressure >= 12f -> 0.78f
                scenePressure >= 8f -> 0.9f
                else -> 1f
            }
        return qualityProfile.effectBudgetScale * pressureScale
    }

    private fun activeTrailEffectCount(): Int {
        var count = 0
        effects.forEach { effect ->
            if (effect.type == EffectType.TRAIL) count++
        }
        return count
    }

    private fun adjustedEffectCount(
        baseCount: Int,
        minimum: Int,
    ): Int {
        val scaled = (baseCount * currentEffectBudgetScale()).toInt()
        return max(minimum, min(baseCount, scaled))
    }

    private fun adjustedTrailStride(baseStride: Int): Int {
        val scenePressure = threats.size + interceptors.size + effects.size / 20
        val extraStride =
            when {
                scenePressure >= 22 -> 2
                scenePressure >= 14 -> 1
                else -> 0
            }
        return (baseStride + extraStride).coerceAtLeast(1)
    }

    private fun canSpawnTrail(hostile: Boolean): Boolean {
        val activeTrailEffects = activeTrailEffectCount()
        if (activeTrailEffects >= qualityProfile.maxTrailEffects) return false

        val stride =
            adjustedTrailStride(
                if (hostile) qualityProfile.hostileTrailStride else qualityProfile.interceptorTrailStride,
            )
        return stride <= 1 || nextTrailSampleIndex(hostile) % stride == 1
    }

    private fun nextTrailSampleIndex(hostile: Boolean): Int =
        if (hostile) {
            hostileTrailSamples += 1
            hostileTrailSamples
        } else {
            interceptorTrailSamples += 1
            interceptorTrailSamples
        }

    init {
        Gdx.graphics.isContinuousRendering = true
        Gdx.app.log(
            "BattleQuality",
            "requested=${game.launchConfig.graphicsQualityMode} effective=${qualityProfile.label} deviceClass=${game.launchConfig.devicePerformanceClass}",
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

    private fun setupEnvironment() {
        environment.set(ColorAttribute(ColorAttribute.AmbientLight, 0.28f, 0.3f, 0.36f, 1f))
        environment.add(DirectionalLight().set(Color(0.42f, 0.48f, 0.58f, 1f), -0.45f, -1f, -0.12f))
        environment.add(DirectionalLight().set(Color(0.18f, 0.24f, 0.34f, 1f), 0.3f, -0.16f, 0.4f))
        impactLight.set(Color.BLACK, Vector3.Zero, 0f)
        cityGlowLight.set(Color(0.8f, 0.54f, 0.26f, 1f), Vector3(1180f, 120f, -1880f), 4200f)
        launcherLeftLight.set(Color(0.24f, 0.82f, 1f, 1f), Vector3(-160f, 18f, 260f), 420f)
        launcherRightLight.set(Color(0.24f, 0.82f, 1f, 1f), Vector3(210f, 18f, 220f), 420f)
        environment.add(impactLight)
        environment.add(cityGlowLight)
        environment.add(launcherLeftLight)
        environment.add(launcherRightLight)
    }

    private fun setupCamera() {
        camera.near = 1f
        camera.far = WORLD_RADIUS * 1.5f
        camera.position.set(cameraBase)
        camera.lookAt(cameraLookAt)
        camera.update()
    }

    private fun registerTexture(
        texture: Texture,
        repeat: Boolean = false,
    ): Texture {
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        if (repeat) texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat)
        textures.add(texture)
        return texture
    }

    private fun generateTerrainModels() {
        val terrainAssets = terrainAssetFactory.build()
        battleSkyTexture = terrainAssets.skyTexture
        battleSkyRegion = TextureRegion(terrainAssets.skyTexture)
        battleHorizonTexture = terrainAssets.horizonTexture
        battleFogTexture = terrainAssets.fogTexture
        battleGlowTexture = terrainAssets.glowTexture
        battleReflectionTexture = terrainAssets.reflectionTexture
        models.put("ground", terrainAssets.groundModel)
        sceneRenderer.updateBackdropTextures(
            skyRegion = battleSkyRegion,
            horizonTexture = battleHorizonTexture,
            glowTexture = battleGlowTexture,
            reflectionTexture = battleReflectionTexture,
            fogTexture = battleFogTexture,
        )
    }

    private fun generateBuildingModels() {
        buildingAssetFactory.build().forEach { namedModel ->
            models.put(namedModel.key, namedModel.model)
        }
    }

    private fun generateDefenseModels() {
        defenseAssetFactory.build().forEach { namedModel ->
            models.put(namedModel.key, namedModel.model)
        }
    }

    private fun generateProjectileModels() {
        projectileAssetFactory.build().forEach { namedModel ->
            models.put(namedModel.key, namedModel.model)
        }
    }

    private fun loadImportedModels() {
        if (!useImportedLandmarks) return
        val engelHouseFile = Gdx.files.internal("models/engel_house.obj")
        if (engelHouseFile.exists()) {
            val engelHouse = ObjLoader().loadModel(engelHouseFile)
            engelHouse.materials.forEach { material ->
                material.set(ColorAttribute.createDiffuse(Color(0.94f, 0.92f, 0.88f, 1f)))
                material.set(ColorAttribute.createSpecular(Color(0.18f, 0.18f, 0.2f, 1f)))
                material.set(FloatAttribute.createShininess(14f))
            }
            models.put("engel_house", engelHouse)
        }
    }

    private fun createWorldInstances() {
        instances.add(ModelInstance(models.get("ground")).apply { transform.setToTranslation(0f, -2f, 0f) })
        if (qualityProfile.showMoon) {
            instances.add(ModelInstance(models.get("moon")).apply { transform.setToTranslation(1400f, 1450f, -4200f) })
        }

        val launcherPositions = BattleWorldLayout.launcherPositions()
        launcherPositions.forEach { position ->
            val launcher =
                ModelInstance(models.get("launcher")).apply {
                    transform.setToTranslation(position)
                }
            launchers.add(launcher)
            instances.add(launcher)
        }

        val radarPosition = BattleWorldLayout.radarPosition()
        instances.add(ModelInstance(models.get("radar")).apply { transform.setToTranslation(radarPosition) })

        val buildingDefinitions = BattleWorldLayout.buildingDefinitions()
        buildingDefinitions.forEach { definition ->
            val entity =
                BuildingEntity(
                    id = definition.id,
                    instance = ModelInstance(models.get(definition.modelName)),
                    modelName = definition.modelName,
                    position = definition.position.cpy(),
                    yaw = definition.yaw,
                    baseHeight = definition.metrics.baseHeight,
                    width = definition.metrics.width,
                    depth = definition.metrics.depth,
                    integrity = 100f,
                )
            syncBuildingTransform(entity)
            cityBlocks.add(entity)
            cityBlocksById.put(entity.id, entity)
        }

        simulation =
            BattleSimulation(
                buildingDefinitions = buildingDefinitions,
                launcherPositions = launcherPositions,
                settings = settings,
                random = game.launchConfig.benchmarkSeed?.let(::SeededRandomSource) ?: DefaultRandomSource,
            )

        fun addImportedLandmark(
            key: String,
            x: Float,
            z: Float,
            yaw: Float,
            targetHeight: Float,
        ) {
            val model = models.get(key) ?: return
            val bounds = BoundingBox()
            val center = Vector3()
            val dimensions = Vector3()
            model.calculateBoundingBox(bounds)
            bounds.getCenter(center)
            bounds.getDimensions(dimensions)
            val height = dimensions.y.coerceAtLeast(1f)
            val scale = targetHeight / height
            instances.add(
                ModelInstance(model).apply {
                    transform.idt()
                    transform.translate(x, 0f, z)
                    transform.rotate(Vector3.Y, yaw)
                    transform.scale(scale, scale, scale)
                    transform.translate(-center.x, -bounds.min.y, -center.z)
                },
            )
        }

        addImportedLandmark("engel_house", -1320f, -1340f, 90f, 108f)
        addImportedLandmark("engel_house", -760f, -1320f, 90f, 108f)

        syncBattleStateFromSimulation()
    }

    private fun syncBattleStateFromSimulation() {
        wave = simulation.wave
        score = simulation.score
        credits = simulation.credits
        cityIntegrity = simulation.cityIntegrity
        waveInProgress = simulation.waveInProgress
        isGameOver = simulation.isGameOver
        threatsRemainingInWave = simulation.threatsRemainingInWave
    }

    private fun setStatus(
        styleName: String,
        text: String,
    ) {
        hudController.setStatus(styleName, text)
    }

    private fun applySimulationStep(step: BattleStepEvents) {
        simulationStepApplier.apply(step)
    }

    private fun syncRenderEntitiesFromSimulation() {
        simulation.threats.forEach { state ->
            val renderThreat = threatsById[state.id] ?: return@forEach
            renderThreat.position.set(state.position)
            renderThreat.velocity.set(state.velocity)
            renderThreat.targetPosition.set(state.targetPosition)
            renderThreat.isTracked = state.isTracked
            renderThreat.trailCooldown = state.trailCooldown
            syncProjectileTransform(renderThreat.instance, renderThreat.position, renderThreat.velocity, THREAT_SCALE)
        }

        simulation.interceptors.forEach { state ->
            val renderInterceptor = interceptorsById[state.id] ?: return@forEach
            renderInterceptor.position.set(state.position)
            renderInterceptor.velocity.set(state.velocity)
            renderInterceptor.target = state.targetId?.let { threatsById[it] }
            renderInterceptor.trailCooldown = state.trailCooldown
            syncProjectileTransform(renderInterceptor.instance, renderInterceptor.position, renderInterceptor.velocity, INTERCEPTOR_SCALE)
        }
    }

    private fun applyBuildingDamageVisual(
        building: BuildingEntity,
        newIntegrity: Float,
        epicenter: Vector3,
    ) {
        if (building.integrity <= 0f && newIntegrity <= 0f) return
        val previousIntegrity = building.integrity
        val damage = (previousIntegrity - newIntegrity).coerceAtLeast(0f)
        if (damage <= 0f) return

        building.integrity = newIntegrity
        building.collapseVelocity += damage * 0.0025f
        building.leanTarget =
            MathUtils.clamp(
                building.leanTarget + (building.position.x - epicenter.x).sign() * damage * 0.05f,
                -16f,
                16f,
            )

        val material = building.instance.materials.first()
        val tint = 0.28f + building.integrity / 140f
        material.set(ColorAttribute.createDiffuse(Color(tint, tint, tint + 0.06f, 1f)))

        if (previousIntegrity > 0f && building.integrity <= 0f) {
            building.visibleHeight = building.baseHeight * 0.12f
            building.collapseVelocity = max(building.collapseVelocity, 60f)
            spawnDebris(building.position.cpy().add(0f, building.baseHeight * 0.35f, 0f), 28, Color(0.25f, 0.25f, 0.28f, 1f))
        } else {
            spawnDebris(building.position.cpy().add(0f, building.baseHeight * 0.45f, 0f), 6, Color(0.3f, 0.3f, 0.34f, 1f))
        }
    }

    private fun setupHud() {
        hudController.build()
        syncHud()
    }

    private fun loadAudio() {
        fun loadSound(
            key: String,
            path: String,
        ) {
            val file = Gdx.files.internal(path)
            if (file.exists()) {
                sounds.put(key, Gdx.audio.newSound(file))
            }
        }
        loadSound("launch", "sfx/launch.mp3")
        loadSound("detonate", "sfx/detonate.mp3")
        loadSound("impact", "sfx/impact.mp3")
    }

    private fun playSfx(
        name: String,
        volume: Float,
    ) {
        sounds[name]?.play(volume)
    }

    private fun startNewWave() {
        if (!simulation.startNewWave()) return
        syncBattleStateFromSimulation()
        setStatus("critical", "MULTIPLE INBOUND TRACKS // WAVE $wave")
        syncHud()
    }

    private fun handleStartWaveRequest() {
        startNewWave()
    }

    override fun render(delta: Float) {
        if (!initialized) {
            runInitializationStep()
            sceneRenderer.renderLoading(loadingMessage, buildDiagnosticsLine(), initializationProgress())
            return
        }

        if (isGameOver) {
            sceneRenderer.renderGameOver(score)
            return
        }

        updateLogic(min(delta, 1f / 30f))
        battleLiveTime += delta
        recordFrameTelemetry(delta)
        sceneRenderer.renderBackdrop(battleLiveTime = battleLiveTime, impactLightIntensity = impactLight.intensity)
        renderWorldModels()
        sceneRenderer.renderAtmosphere()

        stage.act(delta)
        stage.draw()
        sceneRenderer.renderOverlay(threats, interceptors, camera, radarScanProgress)
    }

    private fun renderWorldModels() {
        modelBatch.begin(camera)
        instances.forEach { modelBatch.render(it, environment) }
        cityBlocks.forEach { if (it.visibleHeight > 1f) modelBatch.render(it.instance, environment) }
        threats.forEach { modelBatch.render(it.instance, environment) }
        interceptors.forEach { modelBatch.render(it.instance, environment) }
        debris.forEach { modelBatch.render(it.instance, environment) }
        effects.forEach { modelBatch.render(it.instance, environment) }
        modelBatch.end()
    }

    private fun runInitializationStep() {
        if (initialized || initializationStep >= initializationTasks.size) return
        val task = initializationTasks[initializationStep]
        loadingMessage = task.message
        val startedAt = System.currentTimeMillis()
        task.action()
        lastInitializationDurationMs = System.currentTimeMillis() - startedAt
        Gdx.app.log(
            "BattleInit",
            "${initializationStep + 1}/${initializationTasks.size} ${task.message} took ${lastInitializationDurationMs}ms",
        )
        initializationStep++
    }

    private fun buildDiagnosticsLine(): String {
        val currentStep = (initializationStep + 1).coerceAtMost(initializationTasks.size)
        return "STEP $currentStep/${initializationTasks.size} // LAST ${lastInitializationDurationMs}ms // ${qualityProfile.label}"
    }

    private fun initializationProgress(): Float = initializationStep.toFloat() / initializationTasks.size.coerceAtLeast(1)

    private fun recordFrameTelemetry(delta: Float) {
        val frameTimeMs = delta.coerceIn(0f, 0.25f) * 1000f
        frameTimeWindowMs[frameTimeWindowCursor] = frameTimeMs
        frameTimeWindowCursor = (frameTimeWindowCursor + 1) % frameTimeWindowMs.size
        frameTimeWindowSize = min(frameTimeWindowSize + 1, frameTimeWindowMs.size)
        battleFrameLogTimer += delta
        if (battleFrameLogTimer >= 3f && frameTimeWindowSize > 0) {
            Gdx.app.log("BattleFrame", battleHealthSummary())
            battleFrameLogTimer = 0f
        }
    }

    private fun battleHealthSummary(): String {
        if (battleLiveTime <= 0f || frameTimeWindowSize == 0) {
            return buildString {
                append("LIVE 0s // FPS 0.0 // FT 0.0/0.0/0.0")
                append(" // Q ${qualityProfile.label}")
                append(" // FX${effects.size}")
                append(" // T${threats.size} I${interceptors.size}")
            }
        }
        val samples = copyFrameTimeWindow()
        samples.sort()
        val averageFrameMs = samples.sum() / samples.size
        val averageFps = if (averageFrameMs > 0f) 1000f / averageFrameMs else 0f
        val p50 = percentile(samples, 0.5f)
        val p95 = percentile(samples, 0.95f)
        val maxFrameMs = samples.last()
        return buildString {
            append("LIVE ${battleLiveTime.toInt()}s // FPS ${"%.1f".format(Locale.US, averageFps)}")
            append(
                " // FT ${"%.1f".format(Locale.US, p50)}/${"%.1f".format(Locale.US, p95)}/${"%.1f".format(Locale.US, maxFrameMs)}",
            )
            append(" // Q ${qualityProfile.label}")
            append(" // FX${effects.size}")
            append(" // T${threats.size} I${interceptors.size}")
        }
    }

    private fun copyFrameTimeWindow(): FloatArray {
        val copy = FloatArray(frameTimeWindowSize)
        val start =
            if (frameTimeWindowSize == frameTimeWindowMs.size) {
                frameTimeWindowCursor
            } else {
                0
            }
        for (index in 0 until frameTimeWindowSize) {
            copy[index] = frameTimeWindowMs[(start + index) % frameTimeWindowMs.size]
        }
        return copy
    }

    private fun percentile(
        sortedSamples: FloatArray,
        percentile: Float,
    ): Float {
        if (sortedSamples.isEmpty()) return 0f
        val index = ((sortedSamples.size - 1) * percentile).toInt().coerceIn(0, sortedSamples.lastIndex)
        return sortedSamples[index]
    }

    private fun updateLogic(dt: Float) {
        radarScanProgress = (radarScanProgress + dt * 0.42f) % 1f
        updateCameraShake(dt)
        val step = simulation.step(dt)
        applySimulationStep(step)
        updateEffects(dt)
        updateDebris(dt)
        updateBuildings(dt)
        syncBattleStateFromSimulation()
        updateSceneLights(dt)
        syncHud()

        if (isGameOver) {
            isGameOver = true
            setStatus("critical", "DEFENSE FAILED")
            syncHud()
        }
    }

    private fun updateCameraShake(dt: Float) {
        if (shakeTime > 0f) {
            shakeTime -= dt
            val amount = shakeIntensity * (shakeTime / 0.5f).coerceIn(0f, 1f)
            camera.position.set(cameraBase).add(
                MathUtils.random(-amount, amount),
                MathUtils.random(-amount, amount),
                MathUtils.random(-amount, amount),
            )
        } else {
            camera.position.set(cameraBase)
        }
        camera.lookAt(cameraLookAt)
        camera.update()
    }

    private fun updateSceneLights(dt: Float) {
        launcherLeftPulse = max(0f, launcherLeftPulse - dt * 2.3f)
        launcherRightPulse = max(0f, launcherRightPulse - dt * 2.3f)

        val wavePressure = (threats.size + threatsRemainingInWave * 0.3f).coerceAtLeast(0f)
        val cityPulse = 0.86f + kotlin.math.sin(battleLiveTime * 0.45f) * 0.08f + min(0.3f, wavePressure * 0.018f)
        cityGlowLight.intensity = 3200f * cityPulse * qualityProfile.lightIntensityScale
        cityGlowLight.position.set(1180f + kotlin.math.sin(battleLiveTime * 0.12f) * 90f, 120f, -1880f)

        launcherLeftLight.intensity = (320f + launcherLeftPulse * 780f) * qualityProfile.lightIntensityScale
        launcherRightLight.intensity = (320f + launcherRightPulse * 780f) * qualityProfile.lightIntensityScale
        launcherLeftLight.color.set(
            MathUtils.lerp(0.24f, 0.95f, launcherLeftPulse),
            MathUtils.lerp(0.82f, 0.76f, launcherLeftPulse),
            MathUtils.lerp(1f, 0.54f, launcherLeftPulse),
            1f,
        )
        launcherRightLight.color.set(
            MathUtils.lerp(0.24f, 0.95f, launcherRightPulse),
            MathUtils.lerp(0.82f, 0.76f, launcherRightPulse),
            MathUtils.lerp(1f, 0.54f, launcherRightPulse),
            1f,
        )
    }

    private fun updateBuildings(dt: Float) {
        cityBlocks.forEach { building ->
            if (building.integrity <= 0f) {
                building.visibleHeight = max(building.baseHeight * 0.08f, building.visibleHeight - building.collapseVelocity * dt)
            } else if (building.integrity < 100f) {
                val targetHeight = building.baseHeight * (0.35f + building.integrity / 100f * 0.65f)
                building.visibleHeight += (targetHeight - building.visibleHeight) * min(1f, dt * 3f)
            }
            building.lean += (building.leanTarget - building.lean) * min(1f, dt * 1.7f)
            syncBuildingTransform(building)
        }
    }

    private fun spawnBlast(
        position: Vector3,
        size: Float,
    ) {
        val sparkCount = adjustedEffectCount(qualityProfile.baseSparkCount, minimum = 2)
        val smokeCount = adjustedEffectCount(qualityProfile.baseSmokeCount, minimum = 1)
        addBlastCoreEffect(position, size)
        addShockwaveEffect(position, size)
        spawnSparkEffects(position, size, sparkCount)
        spawnSmokeEffects(position, size, smokeCount)
        brightenImpactLight(position, size)
    }

    private fun addBlastCoreEffect(
        position: Vector3,
        size: Float,
    ) {
        val core = ModelInstance(models.get("blast"))
        core.transform.setToScaling(0.1f, 0.1f, 0.1f).trn(position)
        core.materials.first().set(ColorAttribute.createDiffuse(Color(1f, 0.96f, 0.86f, 1f)))
        core.materials.first().set(ColorAttribute.createEmissive(Color(1f, 0.74f, 0.28f, 1f)))
        effects.add(
            VisualEffect(
                instance = core,
                position = position.cpy(),
                life = 0.7f,
                initialLife = 0.7f,
                type = EffectType.BLAST,
                maxScale = size * 1.18f,
            ),
        )
    }

    private fun addShockwaveEffect(
        position: Vector3,
        size: Float,
    ) {
        val shockwave = ModelInstance(models.get("blast"))
        shockwave.transform.setToScaling(0.1f, 0.03f, 0.1f).trn(position.x, position.y + 6f, position.z)
        shockwave.materials.first().set(ColorAttribute.createDiffuse(Color(1f, 0.72f, 0.34f, 1f)))
        shockwave.materials.first().set(ColorAttribute.createEmissive(Color(0.72f, 0.28f, 0.08f, 1f)))
        effects.add(
            VisualEffect(
                instance = shockwave,
                position = Vector3(position.x, position.y + 6f, position.z),
                life = 0.56f,
                initialLife = 0.56f,
                type = EffectType.SHOCKWAVE,
                maxScale = size * 2.8f,
            ),
        )
    }

    private fun spawnSparkEffects(
        position: Vector3,
        size: Float,
        sparkCount: Int,
    ) {
        repeat(sparkCount) {
            val sparkLife = MathUtils.random(0.26f, 0.46f)
            val spark = ModelInstance(models.get("trail"))
            spark.transform.setToScaling(0.3f, 0.3f, 0.3f).trn(position)
            spark.materials.first().set(ColorAttribute.createDiffuse(Color(1f, 0.72f, 0.28f, 1f)))
            spark.materials.first().set(ColorAttribute.createEmissive(Color(0.95f, 0.42f, 0.08f, 1f)))
            effects.add(
                VisualEffect(
                    instance = spark,
                    position = position.cpy(),
                    life = sparkLife,
                    initialLife = sparkLife,
                    type = EffectType.SPARK,
                    maxScale = size * MathUtils.random(0.22f, 0.42f),
                    velocity =
                        Vector3(
                            MathUtils.random(-160f, 160f),
                            MathUtils.random(40f, 180f),
                            MathUtils.random(-160f, 160f),
                        ),
                ),
            )
        }
    }

    private fun spawnSmokeEffects(
        position: Vector3,
        size: Float,
        smokeCount: Int,
    ) {
        repeat(smokeCount) {
            val smokeLife = MathUtils.random(1.1f, 1.65f)
            val smoke = ModelInstance(models.get("trail"))
            smoke.transform.setToScaling(0.4f, 0.4f, 0.4f).trn(position)
            smoke.materials.first().set(ColorAttribute.createDiffuse(Color(0.22f, 0.22f, 0.24f, 1f)))
            smoke.materials.first().set(ColorAttribute.createEmissive(Color(0.06f, 0.06f, 0.07f, 1f)))
            effects.add(
                VisualEffect(
                    instance = smoke,
                    position = position.cpy(),
                    life = smokeLife,
                    initialLife = smokeLife,
                    type = EffectType.SMOKE,
                    maxScale = size * MathUtils.random(0.7f, 1.15f),
                    velocity =
                        Vector3(
                            MathUtils.random(-28f, 28f),
                            MathUtils.random(22f, 48f),
                            MathUtils.random(-20f, 20f),
                        ),
                ),
            )
        }
    }

    private fun brightenImpactLight(
        position: Vector3,
        size: Float,
    ) {
        impactLight.set(Color(1f, 0.82f, 0.5f, 1f), position, size * 18f * qualityProfile.lightIntensityScale)
    }

    private fun spawnTrail(
        position: Vector3,
        hostile: Boolean,
    ) {
        if (effects.size >= qualityProfile.maxTrailEffects + 80) return
        val instance = ModelInstance(models.get("trail"))
        val initialScale = if (hostile) 0.86f else 0.56f
        instance.transform.setToScaling(initialScale, initialScale, initialScale).trn(position)
        val effect =
            VisualEffect(
                instance,
                position.cpy(),
                if (hostile) 0.56f else 0.34f,
                if (hostile) 0.56f else 0.34f,
                EffectType.TRAIL,
                if (hostile) 2.25f else 1.45f,
                Vector3(0f, if (hostile) 8f else 4f, 0f),
            )
        val material = effect.instance.materials.first()
        material.set(
            ColorAttribute.createDiffuse(
                if (hostile) Color(1f, 0.42f, 0.08f, 1f) else Color(0.46f, 0.9f, 1f, 1f),
            ),
        )
        material.set(
            ColorAttribute.createEmissive(
                if (hostile) Color(0.62f, 0.18f, 0.04f, 1f) else Color(0.12f, 0.38f, 0.48f, 1f),
            ),
        )
        effects.add(effect)
    }

    private fun updateEffects(dt: Float) {
        val iterator = effects.iterator()
        var strongest = 0f
        val strongestPos = tempA.setZero()
        while (iterator.hasNext()) {
            val effect = iterator.next()
            strongest =
                updateEffect(
                    effect = effect,
                    dt = dt,
                    strongest = strongest,
                    strongestPos = strongestPos,
                )
            if (effect.life <= 0f) {
                iterator.remove()
            }
        }
        updateImpactLightFromEffects(dt, strongest, strongestPos)
    }

    private fun updateEffect(
        effect: VisualEffect,
        dt: Float,
        strongest: Float,
        strongestPos: Vector3,
    ): Float {
        effect.life -= dt
        val progress = (effect.life / effect.initialLife).coerceIn(0f, 1f)
        val blend =
            effect.instance.materials
                .first()
                .get(BlendingAttribute.Type) as? BlendingAttribute
        return when (effect.type) {
            EffectType.BLAST -> {
                updateBlastEffect(effect, progress, blend, strongest, strongestPos)
            }

            EffectType.SHOCKWAVE -> {
                updateShockwaveEffect(effect, progress, blend)
                strongest
            }

            EffectType.SMOKE -> {
                updateSmokeEffect(effect, dt, progress, blend)
                strongest
            }

            EffectType.SPARK -> {
                updateSparkEffect(effect, dt, progress, blend)
                strongest
            }

            EffectType.TRAIL -> {
                updateTrailEffect(effect, dt, progress, blend)
                strongest
            }
        }
    }

    private fun updateBlastEffect(
        effect: VisualEffect,
        progress: Float,
        blend: BlendingAttribute?,
        strongest: Float,
        strongestPos: Vector3,
    ): Float {
        val scale = effect.maxScale * (1.25f - progress * progress)
        effect.instance.transform
            .setToScaling(scale, scale, scale)
            .trn(effect.position)
        blend?.opacity = (progress * progress).coerceIn(0f, 1f)
        val intensity = effect.maxScale * progress * 20f
        if (intensity > strongest) {
            strongestPos.set(effect.position)
            return intensity
        }
        return strongest
    }

    private fun updateShockwaveEffect(
        effect: VisualEffect,
        progress: Float,
        blend: BlendingAttribute?,
    ) {
        val scale = effect.maxScale * (1.18f - progress)
        effect.instance.transform
            .setToScaling(scale, scale * 0.08f, scale)
            .trn(effect.position)
        blend?.opacity = (progress * 0.72f).coerceIn(0f, 1f)
    }

    private fun updateSmokeEffect(
        effect: VisualEffect,
        dt: Float,
        progress: Float,
        blend: BlendingAttribute?,
    ) {
        effect.position.mulAdd(effect.velocity, dt)
        effect.velocity.y += dt * 10f
        val scale = 0.6f + (1f - progress) * effect.maxScale
        effect.instance.transform
            .setToScaling(scale, scale, scale)
            .trn(effect.position)
        blend?.opacity = (0.26f * kotlin.math.sqrt(progress)).coerceIn(0f, 0.3f)
    }

    private fun updateSparkEffect(
        effect: VisualEffect,
        dt: Float,
        progress: Float,
        blend: BlendingAttribute?,
    ) {
        effect.position.mulAdd(effect.velocity, dt)
        effect.velocity.scl((1f - dt * 1.8f).coerceAtLeast(0.2f))
        effect.velocity.y -= dt * 140f
        val scale = 0.18f + progress * effect.maxScale * 0.12f
        effect.instance.transform
            .setToScaling(scale, scale, scale)
            .trn(effect.position)
        blend?.opacity = (0.72f * progress).coerceIn(0f, 0.85f)
    }

    private fun updateTrailEffect(
        effect: VisualEffect,
        dt: Float,
        progress: Float,
        blend: BlendingAttribute?,
    ) {
        effect.position.mulAdd(effect.velocity, dt)
        val scale = 0.45f + (1f - progress) * effect.maxScale
        effect.instance.transform
            .setToScaling(scale, scale, scale)
            .trn(effect.position)
        blend?.opacity = 0.42f * progress
    }

    private fun updateImpactLightFromEffects(
        dt: Float,
        strongest: Float,
        strongestPos: Vector3,
    ) {
        if (strongest > 0f) {
            impactLight.set(Color(1f, 0.8f, 0.42f, 1f), strongestPos, strongest)
            return
        }
        impactLight.intensity = max(0f, impactLight.intensity - dt * 260f)
    }

    private fun spawnDebris(
        position: Vector3,
        count: Int,
        color: Color,
    ) {
        val debrisCount = adjustedEffectCount(min(count, qualityProfile.maxDebrisPieces), minimum = 1)
        repeat(debrisCount) {
            val velocity =
                Vector3(
                    MathUtils.random(-1f, 1f),
                    MathUtils.random(0.3f, 1.5f),
                    MathUtils.random(-1f, 1f),
                ).nor().scl(MathUtils.random(40f, 220f))
            val instance = ModelInstance(models.get("debris"))
            instance.materials.first().set(ColorAttribute.createDiffuse(color))
            val size = MathUtils.random(0.35f, 1.5f)
            instance.transform.setToScaling(size, size, size).trn(position)
            debris.add(DebrisEntity(instance, position.cpy(), velocity, MathUtils.random(1.1f, 3.2f), size))
        }
    }

    private fun updateDebris(dt: Float) {
        val iterator = debris.iterator()
        while (iterator.hasNext()) {
            val piece = iterator.next()
            piece.life -= dt
            piece.velocity.y -= 120f * dt
            piece.position.mulAdd(piece.velocity, dt)
            piece.rotation.mulAdd(Vector3(1.5f, 0.9f, 1.2f), dt * 120f)
            piece.instance.transform
                .setToScaling(piece.scale, piece.scale, piece.scale)
                .rotate(Vector3.X, piece.rotation.x)
                .rotate(Vector3.Y, piece.rotation.y)
                .rotate(Vector3.Z, piece.rotation.z)
                .trn(piece.position)
            if (piece.position.y <= 0f || piece.life <= 0f) iterator.remove()
        }
    }

    private fun syncProjectileTransform(
        instance: ModelInstance,
        position: Vector3,
        velocity: Vector3,
        scale: Float,
    ) {
        instance.transform.setToScaling(scale, scale, scale).trn(position)
        instance.setRotationToward(velocity)
    }

    private fun syncBuildingTransform(building: BuildingEntity) {
        val heightScale = (building.visibleHeight / building.baseHeight).coerceAtLeast(0.05f)
        val y = building.visibleHeight * 0.5f
        building.instance.transform.idt()
        building.instance.transform.translate(building.position.x, y, building.position.z)
        building.instance.transform.rotate(Vector3.Y, building.yaw)
        building.instance.transform.rotate(Vector3.Z, building.lean)
        building.instance.transform.scale(1f, heightScale, 1f)
    }

    private fun currentHudSnapshot(): BattleHudSnapshot =
        BattleHudSnapshot(
            cityIntegrity = cityIntegrity,
            score = score,
            credits = credits,
            wave = wave,
            waveInProgress = waveInProgress,
            isGameOver = isGameOver,
            visibleThreats = threats.size,
            remainingThreatsInWave = threatsRemainingInWave,
            effectiveRangeMeters = DefenseTuning.engagementRange(settings),
            effectiveFuseMeters = DefenseTuning.blastRadius(settings),
            doctrineLabel = settings.doctrine.label,
            doctrineSummary = settings.doctrine.summary,
        )

    private fun syncHud() {
        hudController.update(currentHudSnapshot())
    }

    private fun triggerShake(
        intensity: Float,
        duration: Float,
    ) {
        shakeIntensity = max(shakeIntensity, intensity)
        shakeTime = max(shakeTime, duration)
    }

    private fun ModelInstance.setRotationToward(direction: Vector3) {
        if (direction.isZero(0.0001f)) return
        transform.getTranslation(tempC)
        tempA.set(direction).nor()
        tempB.set(Vector3.Y)
        if (abs(tempA.dot(tempB)) > 0.98f) tempB.set(Vector3.Z)
        tempD.set(tempB).crs(tempA).nor()
        tempB.set(tempA).crs(tempD).nor()
        transform.set(tempD, tempA, tempB, tempC)
    }

    override fun resize(
        width: Int,
        height: Int,
    ) {
        stage.viewport.update(width, height, true)
        camera.viewportWidth = width.toFloat()
        camera.viewportHeight = height.toFloat()
        camera.update()
        setupHud()
    }

    override fun dispose() {
        modelBatch.dispose()
        stage.dispose()
        skin.dispose()
        textures.forEach { it.dispose() }
        models.values().forEach { it.dispose() }
        sounds.values().forEach { it.dispose() }
    }
}

private fun Float.sign(): Float =
    when {
        this > 0f -> 1f
        this < 0f -> -1f
        else -> 0f
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

data class InitializationTask(
    val message: String,
    val action: () -> Unit,
)
