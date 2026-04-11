package com.airdefense.game

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.graphics.g3d.environment.PointLight
import com.badlogic.gdx.graphics.g3d.loader.ObjLoader
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.BoxShapeBuilder
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.ConeShapeBuilder
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.CylinderShapeBuilder
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.SphereShapeBuilder
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Slider
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.viewport.ScreenViewport
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
    private val skin = createSkin()
    private val whiteRegion by lazy { skin.get("white_region", com.badlogic.gdx.graphics.g2d.TextureRegion::class.java) }
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

    private val statusLabel = Label("AIR DEFENSE NETWORK ONLINE", skin, "status")
    private val creditsLabel = Label("", skin, "status")
    private lateinit var waveButton: TextButton
    private lateinit var rangeValueLabel: Label
    private lateinit var speedValueLabel: Label

    private val gravity = Vector3(0f, PhysicsModel.THREAT_GRAVITY_Y, 0f)
    private val cameraBase = Vector3(280f, 380f, 1760f)
    private val cameraLookAt = Vector3(980f, 120f, -2550f)
    private val tempA = Vector3()
    private val tempB = Vector3()
    private val tempC = Vector3()
    private val tempD = Vector3()
    private val tempProject = Vector3()
    private val tmpMatrix = Matrix4()

    private var credits = 10000
    private var wave = 1
    private var score = 0
    private var cityIntegrity = 100f
    private var waveInProgress = false
    private var isGameOver = false
    private var threatsRemainingInWave = 0
    private var spawnTimer = 0f
    private var timeSinceLastLaunch = 0f
    private var radarScanProgress = 0f
    private var shakeTime = 0f
    private var shakeIntensity = 0f
    private var initializationStep = 0
    private var initialized = false
    private var loadingMessage = "Initializing battle systems..."
    private var lastInitializationDurationMs = 0L
    private var battleLiveTime = 0f
    private var renderedBattleFrames = 0
    private var launcherLeftPulse = 0f
    private var launcherRightPulse = 0f
    private var hostileTrailSamples = 0
    private var interceptorTrailSamples = 0

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
        private const val THREAT_TRAIL_INTERVAL = 0.14f
        private const val INTERCEPTOR_TRAIL_INTERVAL = 0.05f
        private const val THREAT_SCALE = 3.2f
        private const val INTERCEPTOR_SCALE = 3.4f
    }

    private fun scaledSceneTextureSize(
        base: Int,
        minimum: Int = 64,
    ): Int = max(minimum, (base * qualityProfile.sceneTextureScale).toInt())

    private fun scaledFacadeSize(
        base: Int,
        minimum: Int,
    ): Int = max(minimum, (base * qualityProfile.facadeTextureScale).toInt())

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
        if (stride <= 1) return true

        val sampleIndex =
            if (hostile) {
                hostileTrailSamples += 1
                hostileTrailSamples
            } else {
                interceptorTrailSamples += 1
                interceptorTrailSamples
            }
        return sampleIndex % stride == 1
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

    private fun createSkin(): Skin {
        val s = Skin()
        val uiScale = Gdx.graphics.height / 1080f
        val font = BitmapFont().apply { data.setScale(1.28f * uiScale) }
        val titleFont = BitmapFont().apply { data.setScale(1.72f * uiScale) }
        val microFont = BitmapFont().apply { data.setScale(1.02f * uiScale) }
        val displayFont = BitmapFont().apply { data.setScale(1.42f * uiScale) }
        s.add("default", font, BitmapFont::class.java)
        s.add("title", titleFont, BitmapFont::class.java)
        s.add("micro", microFont, BitmapFont::class.java)
        s.add("display", displayFont, BitmapFont::class.java)

        val whitePix = Pixmap(2, 2, Pixmap.Format.RGBA8888)
        whitePix.setColor(Color.WHITE)
        whitePix.fill()
        val whiteTex = Texture(whitePix)
        textures.add(whiteTex)
        s.add(
            "white",
            TextureRegionDrawable(
                com.badlogic.gdx.graphics.g2d
                    .TextureRegion(whiteTex),
            ),
            Drawable::class.java,
        )
        s.add(
            "white_region",
            com.badlogic.gdx.graphics.g2d
                .TextureRegion(whiteTex),
        )

        fun addPanel(
            name: String,
            fill: Color,
            stroke: Color,
            accent: Color,
        ) {
            val p = Pixmap(360, 220, Pixmap.Format.RGBA8888)
            p.setColor(fill)
            p.fill()
            p.setColor(accent)
            p.fillRectangle(0, 0, p.width, 16)
            p.setColor(stroke)
            p.drawRectangle(0, 0, p.width, p.height)
            p.drawRectangle(1, 1, p.width - 2, p.height - 2)
            val tex = Texture(p)
            textures.add(tex)
            s.add(
                name,
                TextureRegionDrawable(
                    com.badlogic.gdx.graphics.g2d
                        .TextureRegion(tex),
                ),
                Drawable::class.java,
            )
            p.dispose()
        }

        fun addButton(
            name: String,
            top: Color,
            bottom: Color,
            stroke: Color,
            glow: Color,
        ) {
            val p = Pixmap(360, 120, Pixmap.Format.RGBA8888)
            for (y in 0 until p.height) {
                val t = y / (p.height - 1f)
                p.setColor(
                    MathUtils.lerp(top.r, bottom.r, t),
                    MathUtils.lerp(top.g, bottom.g, t),
                    MathUtils.lerp(top.b, bottom.b, t),
                    MathUtils.lerp(top.a, bottom.a, t),
                )
                p.drawLine(0, y, p.width - 1, y)
            }
            p.setColor(glow)
            p.fillRectangle(8, 6, p.width - 16, 18)
            p.setColor(stroke)
            p.drawRectangle(0, 0, p.width, p.height)
            p.drawRectangle(1, 1, p.width - 2, p.height - 2)
            p.drawRectangle(2, 2, p.width - 4, p.height - 4)
            val tex = Texture(p)
            textures.add(tex)
            s.add(
                name,
                TextureRegionDrawable(
                    com.badlogic.gdx.graphics.g2d
                        .TextureRegion(tex),
                ),
                Drawable::class.java,
            )
            p.dispose()
        }

        addButton(
            "btn_up",
            Color(0.03f, 0.08f, 0.16f, 0.96f),
            Color(0.01f, 0.14f, 0.25f, 0.98f),
            Color(0.28f, 0.82f, 1f, 1f),
            Color(0.24f, 0.62f, 0.82f, 0.2f),
        )
        addButton(
            "btn_over",
            Color(0.07f, 0.2f, 0.33f, 0.98f),
            Color(0.03f, 0.28f, 0.45f, 0.98f),
            Color(0.78f, 0.96f, 1f, 1f),
            Color(0.48f, 0.84f, 1f, 0.28f),
        )
        addButton(
            "btn_down",
            Color(0f, 0.34f, 0.52f, 0.98f),
            Color(0.0f, 0.2f, 0.34f, 1f),
            Color(0.86f, 0.98f, 1f, 1f),
            Color(0.66f, 0.94f, 1f, 0.2f),
        )
        addButton(
            "btn_disabled",
            Color(0.07f, 0.1f, 0.14f, 0.88f),
            Color(0.03f, 0.06f, 0.1f, 0.9f),
            Color(0.18f, 0.24f, 0.3f, 1f),
            Color(0f, 0f, 0f, 0f),
        )
        addPanel("hud_panel", Color(0.01f, 0.04f, 0.08f, 0.58f), Color(0.18f, 0.42f, 0.54f, 0.95f), Color(0.12f, 0.52f, 0.7f, 0.8f))
        addPanel("hud_soft", Color(0.01f, 0.04f, 0.08f, 0.42f), Color(0.12f, 0.24f, 0.3f, 0.88f), Color(0.03f, 0.14f, 0.2f, 0.72f))

        s.add(
            "default",
            TextButton.TextButtonStyle().apply {
                up = s.getDrawable("btn_up")
                checked = s.getDrawable("btn_down")
                down = s.getDrawable("btn_down")
                over = s.getDrawable("btn_over")
                disabled = s.getDrawable("btn_disabled")
                this.font = font
                fontColor = Color.WHITE
                downFontColor = Color(0.92f, 0.98f, 1f, 1f)
                overFontColor = Color.WHITE
                checkedFontColor = Color.WHITE
                disabledFontColor = Color(0.55f, 0.64f, 0.7f, 1f)
            },
        )
        s.add("default", Label.LabelStyle(font, Color.WHITE))
        s.add("display", Label.LabelStyle(displayFont, Color(0.92f, 0.98f, 1f, 1f)))
        s.add("status", Label.LabelStyle(microFont, Color(0.7f, 0.96f, 1f, 1f)))
        s.add("warning", Label.LabelStyle(font, Color(1f, 0.84f, 0.4f, 1f)))
        s.add("critical", Label.LabelStyle(font, Color(1f, 0.42f, 0.42f, 1f)))
        s.add("title", Label.LabelStyle(titleFont, Color.WHITE))

        val sliderBack =
            Pixmap(320, 24, Pixmap.Format.RGBA8888).apply {
                for (y in 0 until height) {
                    val t = y / (height - 1f)
                    setColor(
                        MathUtils.lerp(0.05f, 0.1f, t),
                        MathUtils.lerp(0.14f, 0.2f, t),
                        MathUtils.lerp(0.2f, 0.28f, t),
                        1f,
                    )
                    drawLine(0, y, width - 1, y)
                }
                setColor(0.28f, 0.78f, 0.94f, 1f)
                drawRectangle(0, 0, width, height)
                drawRectangle(1, 1, width - 2, height - 2)
            }
        val sliderKnob =
            Pixmap(52, 64, Pixmap.Format.RGBA8888).apply {
                for (y in 0 until height) {
                    val t = y / (height - 1f)
                    setColor(
                        MathUtils.lerp(0.28f, 0.68f, t),
                        MathUtils.lerp(0.82f, 0.96f, t),
                        1f,
                        1f,
                    )
                    drawLine(0, y, width - 1, y)
                }
                setColor(0.88f, 0.98f, 1f, 1f)
                drawRectangle(0, 0, width, height)
                drawRectangle(1, 1, width - 2, height - 2)
            }
        val sliderBackTex = Texture(sliderBack)
        val sliderKnobTex = Texture(sliderKnob)
        textures.add(sliderBackTex)
        textures.add(sliderKnobTex)
        s.add(
            "default-horizontal",
            Slider.SliderStyle(
                TextureRegionDrawable(
                    com.badlogic.gdx.graphics.g2d
                        .TextureRegion(sliderBackTex),
                ),
                TextureRegionDrawable(
                    com.badlogic.gdx.graphics.g2d
                        .TextureRegion(sliderKnobTex),
                ),
            ),
        )
        whitePix.dispose()
        sliderBack.dispose()
        sliderKnob.dispose()
        return s
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

    private fun loadTexture(
        path: String,
        fallbackColor: Color,
    ): Texture {
        val file = Gdx.files.internal(path)
        if (file.exists()) {
            return registerTexture(Texture(file))
        }
        val pixmap = Pixmap(8, 8, Pixmap.Format.RGBA8888)
        pixmap.setColor(fallbackColor)
        pixmap.fill()
        return registerTexture(Texture(pixmap)).also { pixmap.dispose() }
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

    private fun createTiledAttribute(
        texture: Texture,
        scaleU: Float,
        scaleV: Float,
    ): TextureAttribute =
        TextureAttribute.createDiffuse(texture).apply {
            this.scaleU = scaleU
            this.scaleV = scaleV
        }

    private fun createTextureSet(
        diffuse: Pixmap,
        roughness: Pixmap,
        repeat: Boolean = true,
    ): SurfaceTextureSet {
        val diffuseTexture = registerTexture(Texture(diffuse), repeat)
        val roughnessTexture = registerTexture(Texture(roughness), repeat)
        diffuse.dispose()
        roughness.dispose()
        return SurfaceTextureSet(diffuseTexture, roughnessTexture)
    }

    private fun createSolidTextureSet(
        color: Color,
        roughnessValue: Float,
    ): SurfaceTextureSet {
        val diffuse =
            Pixmap(4, 4, Pixmap.Format.RGBA8888).apply {
                setColor(color)
                fill()
            }
        val roughness =
            Pixmap(4, 4, Pixmap.Format.RGBA8888).apply {
                setColor(roughnessValue, roughnessValue, roughnessValue, 1f)
                fill()
            }
        return createTextureSet(diffuse, roughness, repeat = false)
    }

    private fun createFacadeTextureSet(
        width: Int,
        height: Int,
        base: Color,
        lit: Color,
    ): SurfaceTextureSet {
        val diffuse = Pixmap(width, height, Pixmap.Format.RGBA8888)
        val roughness = Pixmap(width, height, Pixmap.Format.RGBA8888)
        diffuse.setColor(base)
        diffuse.fill()
        roughness.setColor(0.84f, 0.84f, 0.84f, 1f)
        roughness.fill()

        for (x in 0 until width) {
            for (y in 0 until height) {
                val verticalBand = ((x / 14) % 2) * 0.03f
                val horizontalBand = ((y / 18) % 2) * 0.02f
                val noise = MathUtils.random(-0.015f, 0.015f)
                diffuse.setColor(
                    (base.r + verticalBand + noise).coerceIn(0f, 1f),
                    (base.g + horizontalBand + noise).coerceIn(0f, 1f),
                    (base.b + noise).coerceIn(0f, 1f),
                    1f,
                )
                diffuse.drawPixel(x, y)
            }
        }

        for (x in 8 until width step 20) {
            for (y in 10 until height step 22) {
                val glow = MathUtils.randomBoolean(0.7f)
                diffuse.setColor(if (glow) lit else Color(0.03f, 0.05f, 0.08f, 1f))
                diffuse.fillRectangle(x, y, 10, 12)
                roughness.setColor(if (glow) 0.15f else 0.88f, if (glow) 0.15f else 0.88f, if (glow) 0.15f else 0.88f, 1f)
                roughness.fillRectangle(x, y, 10, 12)
            }
        }

        return createTextureSet(diffuse, roughness)
    }

    private fun createGroundTextureSet(): SurfaceTextureSet {
        val textureSize = scaledSceneTextureSize(256, minimum = 128)
        val diffuse = Pixmap(textureSize, textureSize, Pixmap.Format.RGBA8888)
        val roughness = Pixmap(textureSize, textureSize, Pixmap.Format.RGBA8888)
        for (x in 0 until textureSize) {
            for (y in 0 until textureSize) {
                val noise = MathUtils.random(-0.03f, 0.03f)
                val striping = if ((x / 32 + y / 32) % 2 == 0) 0.02f else -0.01f
                diffuse.setColor(
                    (0.07f + noise + striping).coerceIn(0f, 1f),
                    (0.075f + noise).coerceIn(0f, 1f),
                    (0.08f + noise * 0.7f).coerceIn(0f, 1f),
                    1f,
                )
                diffuse.drawPixel(x, y)
                val r = (0.82f + MathUtils.random(-0.1f, 0.08f)).coerceIn(0.1f, 1f)
                roughness.setColor(r, r, r, 1f)
                roughness.drawPixel(x, y)
            }
        }
        return createTextureSet(diffuse, roughness)
    }

    private fun createSeaTextureSet(): SurfaceTextureSet {
        val textureSize = scaledSceneTextureSize(256, minimum = 128)
        val diffuse = Pixmap(textureSize, textureSize, Pixmap.Format.RGBA8888)
        val roughness = Pixmap(textureSize, textureSize, Pixmap.Format.RGBA8888)
        for (x in 0 until textureSize) {
            for (y in 0 until textureSize) {
                val wave = kotlin.math.sin((x + y) * 0.045f) * 0.03f
                val shimmer = kotlin.math.cos(y * 0.08f) * 0.02f
                val noise = MathUtils.random(-0.018f, 0.018f)
                diffuse.setColor(
                    (0.02f + wave + noise).coerceIn(0f, 1f),
                    (0.12f + shimmer + noise).coerceIn(0f, 1f),
                    (0.22f + wave + shimmer + noise * 1.2f).coerceIn(0f, 1f),
                    1f,
                )
                diffuse.drawPixel(x, y)
                val r = (0.32f + MathUtils.random(-0.06f, 0.08f)).coerceIn(0.08f, 1f)
                roughness.setColor(r, r, r, 1f)
                roughness.drawPixel(x, y)
            }
        }
        return createTextureSet(diffuse, roughness)
    }

    private fun createBeachTextureSet(): SurfaceTextureSet {
        val textureSize = scaledSceneTextureSize(256, minimum = 128)
        val diffuse = Pixmap(textureSize, textureSize, Pixmap.Format.RGBA8888)
        val roughness = Pixmap(textureSize, textureSize, Pixmap.Format.RGBA8888)
        for (x in 0 until textureSize) {
            for (y in 0 until textureSize) {
                val dune = kotlin.math.sin(x * 0.03f) * 0.04f + kotlin.math.cos(y * 0.024f) * 0.025f
                val noise = MathUtils.random(-0.03f, 0.03f)
                diffuse.setColor(
                    (0.58f + dune + noise).coerceIn(0f, 1f),
                    (0.5f + dune * 0.7f + noise).coerceIn(0f, 1f),
                    (0.34f + noise * 0.6f).coerceIn(0f, 1f),
                    1f,
                )
                diffuse.drawPixel(x, y)
                val r = (0.9f + MathUtils.random(-0.04f, 0.05f)).coerceIn(0.1f, 1f)
                roughness.setColor(r, r, r, 1f)
                roughness.drawPixel(x, y)
            }
        }
        return createTextureSet(diffuse, roughness)
    }

    private fun createParkTextureSet(): SurfaceTextureSet {
        val textureSize = scaledSceneTextureSize(256, minimum = 128)
        val diffuse = Pixmap(textureSize, textureSize, Pixmap.Format.RGBA8888)
        val roughness = Pixmap(textureSize, textureSize, Pixmap.Format.RGBA8888)
        for (x in 0 until textureSize) {
            for (y in 0 until textureSize) {
                val strip = if ((x / 28 + y / 36) % 2 == 0) 0.03f else -0.02f
                val noise = MathUtils.random(-0.035f, 0.035f)
                diffuse.setColor(
                    (0.06f + strip + noise).coerceIn(0f, 1f),
                    (0.18f + strip + noise).coerceIn(0f, 1f),
                    (0.08f + noise).coerceIn(0f, 1f),
                    1f,
                )
                diffuse.drawPixel(x, y)
                val r = (0.86f + MathUtils.random(-0.06f, 0.05f)).coerceIn(0.1f, 1f)
                roughness.setColor(r, r, r, 1f)
                roughness.drawPixel(x, y)
            }
        }
        return createTextureSet(diffuse, roughness)
    }

    private fun createPromenadeTextureSet(): SurfaceTextureSet {
        val textureSize = scaledSceneTextureSize(256, minimum = 128)
        val diffuse = Pixmap(textureSize, textureSize, Pixmap.Format.RGBA8888)
        val roughness = Pixmap(textureSize, textureSize, Pixmap.Format.RGBA8888)
        for (x in 0 until textureSize) {
            for (y in 0 until textureSize) {
                val tile = if ((x / 48 + y / 48) % 2 == 0) 0.04f else -0.015f
                val noise = MathUtils.random(-0.02f, 0.02f)
                diffuse.setColor(
                    (0.22f + tile + noise).coerceIn(0f, 1f),
                    (0.2f + tile + noise).coerceIn(0f, 1f),
                    (0.18f + tile * 0.7f + noise).coerceIn(0f, 1f),
                    1f,
                )
                diffuse.drawPixel(x, y)
                val r = (0.72f + MathUtils.random(-0.06f, 0.04f)).coerceIn(0.1f, 1f)
                roughness.setColor(r, r, r, 1f)
                roughness.drawPixel(x, y)
            }
        }
        return createTextureSet(diffuse, roughness)
    }

    private fun createSkyTexture(): Texture {
        val skyFile = Gdx.files.internal("textures/rooftop_night_horizon.jpg")
        if (skyFile.exists()) {
            val source = Pixmap(skyFile)
            val cropY = (source.height * 0.16f).toInt()
            val cropHeight = (source.height * 0.56f).toInt().coerceAtLeast(1)
            val cropped = Pixmap(source.width, cropHeight, Pixmap.Format.RGBA8888)
            cropped.drawPixmap(
                source,
                0,
                0,
                0,
                cropY,
                source.width,
                cropHeight,
            )
            source.dispose()
            val texture = registerTexture(Texture(cropped))
            cropped.dispose()
            return texture
        }
        val pixmap =
            Pixmap(
                scaledSceneTextureSize(1024, minimum = 512),
                scaledSceneTextureSize(512, minimum = 256),
                Pixmap.Format.RGBA8888,
            )
        for (x in 0 until pixmap.width) {
            for (y in 0 until pixmap.height) {
                val v = y / (pixmap.height - 1f)
                val horizonGlow = (1f - kotlin.math.abs(v - 0.35f) * 2.2f).coerceIn(0f, 1f)
                val baseR = 0.01f + horizonGlow * 0.06f
                val baseG = 0.02f + horizonGlow * 0.08f
                val baseB = 0.05f + horizonGlow * 0.16f
                val noise = MathUtils.random(-0.012f, 0.012f)
                pixmap.setColor(
                    (baseR + noise).coerceIn(0f, 1f),
                    (baseG + noise).coerceIn(0f, 1f),
                    (baseB + noise * 1.4f).coerceIn(0f, 1f),
                    1f,
                )
                pixmap.drawPixel(x, y)
            }
        }

        repeat(96) {
            val x = MathUtils.random(0, pixmap.width - 1)
            val y = MathUtils.random((pixmap.height * 0.52f).toInt(), pixmap.height - 1)
            val brightness = MathUtils.random(0.45f, 0.82f)
            pixmap.setColor(brightness, brightness, brightness, MathUtils.random(0.28f, 0.62f))
            pixmap.fillCircle(x, y, MathUtils.random(0, 1))
        }

        val texture = registerTexture(Texture(pixmap))
        pixmap.dispose()
        return texture
    }

    private fun createFogTexture(): Texture {
        val pixmap =
            Pixmap(
                scaledSceneTextureSize(512, minimum = 256),
                scaledSceneTextureSize(160, minimum = 96),
                Pixmap.Format.RGBA8888,
            )
        for (x in 0 until pixmap.width) {
            for (y in 0 until pixmap.height) {
                val vertical = y / (pixmap.height - 1f)
                val density = (1f - kotlin.math.abs(vertical - 0.44f) * 2.05f).coerceIn(0f, 1f)
                val noise = MathUtils.random(0.9f, 1.08f)
                val warmMix = (1f - vertical).coerceIn(0f, 1f)
                val alpha = (density * 0.36f * noise).coerceIn(0f, 0.38f)
                pixmap.setColor(
                    MathUtils.lerp(0.34f, 0.18f, vertical),
                    MathUtils.lerp(0.28f, 0.3f, vertical),
                    MathUtils.lerp(0.18f, 0.42f, vertical),
                    alpha * MathUtils.lerp(1.08f, 0.86f, warmMix),
                )
                pixmap.drawPixel(x, y)
            }
        }
        val texture = registerTexture(Texture(pixmap))
        pixmap.dispose()
        return texture
    }

    private fun createGlowTexture(): Texture {
        val pixmap =
            Pixmap(
                scaledSceneTextureSize(512, minimum = 256),
                scaledSceneTextureSize(220, minimum = 128),
                Pixmap.Format.RGBA8888,
            )
        for (x in 0 until pixmap.width) {
            for (y in 0 until pixmap.height) {
                val vertical = y / (pixmap.height - 1f)
                val horizonBand = (1f - kotlin.math.abs(vertical - 0.24f) * 4f).coerceIn(0f, 1f)
                val upperBand = (1f - kotlin.math.abs(vertical - 0.58f) * 3.4f).coerceIn(0f, 1f)
                val sideFade = (1f - kotlin.math.abs((x / (pixmap.width - 1f)) - 0.5f) * 1.55f).coerceIn(0.45f, 1f)
                val warmAlpha = horizonBand * 0.34f * sideFade
                val coolAlpha = upperBand * 0.14f
                val r = 0.12f + warmAlpha * 1.8f + coolAlpha * 0.2f
                val g = 0.12f + warmAlpha * 1.1f + coolAlpha * 0.55f
                val b = 0.18f + warmAlpha * 0.28f + coolAlpha * 1.35f
                val alpha = (warmAlpha + coolAlpha).coerceIn(0f, 0.42f)
                pixmap.setColor(r.coerceIn(0f, 1f), g.coerceIn(0f, 1f), b.coerceIn(0f, 1f), alpha)
                pixmap.drawPixel(x, y)
            }
        }
        val texture = registerTexture(Texture(pixmap))
        pixmap.dispose()
        return texture
    }

    private fun createReflectionTexture(): Texture {
        val pixmap =
            Pixmap(
                scaledSceneTextureSize(768, minimum = 256),
                scaledSceneTextureSize(256, minimum = 128),
                Pixmap.Format.RGBA8888,
            )
        for (x in 0 until pixmap.width) {
            val u = x / (pixmap.width - 1f)
            val streakSeed = kotlin.math.sin(u * 26f) * 0.5f + kotlin.math.sin(u * 63f) * 0.24f
            val warmWindow = (1f - kotlin.math.abs(u - 0.5f) * 1.6f).coerceIn(0f, 1f)
            for (y in 0 until pixmap.height) {
                val v = y / (pixmap.height - 1f)
                val fade = (1f - v).coerceIn(0f, 1f)
                val wave = kotlin.math.sin((u * 44f) + (v * 18f)) * 0.04f
                val glow = (streakSeed * fade + warmWindow * 0.45f - v * 0.22f + wave).coerceIn(0f, 1f)
                val cool = (0.08f + fade * 0.22f + glow * 0.16f).coerceIn(0f, 1f)
                val warm = (glow * 0.48f).coerceIn(0f, 1f)
                pixmap.setColor(
                    (0.08f + warm * 1.1f).coerceIn(0f, 1f),
                    (0.12f + warm * 0.72f + cool * 0.28f).coerceIn(0f, 1f),
                    (0.2f + cool * 0.95f).coerceIn(0f, 1f),
                    (fade * glow * 0.42f).coerceIn(0f, 0.42f),
                )
                pixmap.drawPixel(x, y)
            }
        }
        val texture = registerTexture(Texture(pixmap))
        pixmap.dispose()
        return texture
    }

    private fun createRoadTextureSet(): SurfaceTextureSet {
        val textureSize = scaledSceneTextureSize(256, minimum = 128)
        val diffuse = Pixmap(textureSize, textureSize, Pixmap.Format.RGBA8888)
        val roughness = Pixmap(textureSize, textureSize, Pixmap.Format.RGBA8888)
        for (x in 0 until textureSize) {
            for (y in 0 until textureSize) {
                val noise = MathUtils.random(-0.025f, 0.025f)
                diffuse.setColor(
                    (0.09f + noise).coerceIn(0f, 1f),
                    (0.09f + noise).coerceIn(0f, 1f),
                    (0.1f + noise).coerceIn(0f, 1f),
                    1f,
                )
                diffuse.drawPixel(x, y)
                val r = (0.74f + MathUtils.random(-0.08f, 0.06f)).coerceIn(0.1f, 1f)
                roughness.setColor(r, r, r, 1f)
                roughness.drawPixel(x, y)
            }
        }
        for (x in (textureSize / 2 - 10)..(textureSize / 2 + 10)) {
            diffuse.setColor(0.65f, 0.6f, 0.36f, 1f)
            diffuse.fillRectangle(x, 0, 2, textureSize)
            roughness.setColor(0.42f, 0.42f, 0.42f, 1f)
            roughness.fillRectangle(x, 0, 2, textureSize)
        }
        return createTextureSet(diffuse, roughness)
    }

    private fun createMetalTextureSet(base: Color): SurfaceTextureSet {
        val textureSize = scaledSceneTextureSize(128, minimum = 64)
        val diffuse = Pixmap(textureSize, textureSize, Pixmap.Format.RGBA8888)
        val roughness = Pixmap(textureSize, textureSize, Pixmap.Format.RGBA8888)
        for (x in 0 until textureSize) {
            for (y in 0 until textureSize) {
                val streak = ((x % 24) / 24f) * 0.05f
                val noise = MathUtils.random(-0.025f, 0.025f)
                diffuse.setColor(
                    (base.r + streak + noise).coerceIn(0f, 1f),
                    (base.g + streak + noise).coerceIn(0f, 1f),
                    (base.b + streak + noise).coerceIn(0f, 1f),
                    1f,
                )
                diffuse.drawPixel(x, y)
                val r = (0.42f + MathUtils.random(-0.1f, 0.08f)).coerceIn(0.08f, 1f)
                roughness.setColor(r, r, r, 1f)
                roughness.drawPixel(x, y)
            }
        }
        return createTextureSet(diffuse, roughness)
    }

    private fun createConcreteTextureSet(base: Color): SurfaceTextureSet {
        val textureSize = scaledSceneTextureSize(128, minimum = 64)
        val diffuse = Pixmap(textureSize, textureSize, Pixmap.Format.RGBA8888)
        val roughness = Pixmap(textureSize, textureSize, Pixmap.Format.RGBA8888)
        for (x in 0 until textureSize) {
            for (y in 0 until textureSize) {
                val noise = MathUtils.random(-0.05f, 0.05f)
                diffuse.setColor(
                    (base.r + noise).coerceIn(0f, 1f),
                    (base.g + noise).coerceIn(0f, 1f),
                    (base.b + noise).coerceIn(0f, 1f),
                    1f,
                )
                diffuse.drawPixel(x, y)
                val r = (0.88f + MathUtils.random(-0.06f, 0.04f)).coerceIn(0.1f, 1f)
                roughness.setColor(r, r, r, 1f)
                roughness.drawPixel(x, y)
            }
        }
        return createTextureSet(diffuse, roughness)
    }

    private fun defaultVertexAttributes(): Long =
        (
            VertexAttributes.Usage.Position or
                VertexAttributes.Usage.Normal or
                VertexAttributes.Usage.TextureCoordinates
        ).toLong()

    private fun generateTerrainModels() {
        val attr = defaultVertexAttributes()
        val mb = ModelBuilder()
        battleSkyTexture = createSkyTexture()
        battleSkyRegion = battleSkyTexture?.let { TextureRegion(it) }
        val groundSet = createGroundTextureSet()
        val seaSet = createSeaTextureSet()
        val beachSet = createBeachTextureSet()
        val parkSet = createParkTextureSet()
        val promenadeSet = createPromenadeTextureSet()
        val roadSet = createRoadTextureSet()
        battleHorizonTexture = loadTexture("textures/city_backdrop_telaviv.jpg", Color(0.08f, 0.09f, 0.12f, 1f))
        battleFogTexture = if (qualityProfile.showAtmosphereLayers) createFogTexture() else null
        battleGlowTexture = if (qualityProfile.showGlowLayer) createGlowTexture() else null
        battleReflectionTexture = if (qualityProfile.showReflectionLayer) createReflectionTexture() else null

        mb.begin()
        val inlandGround =
            mb.part(
                "inland_ground",
                GL20.GL_TRIANGLES,
                attr,
                Material(
                    createTiledAttribute(groundSet.diffuse, 12f, 12f),
                    TextureAttribute.createSpecular(groundSet.roughness),
                    ColorAttribute.createDiffuse(Color(0.3f, 0.34f, 0.4f, 1f)),
                    ColorAttribute.createSpecular(Color(0.12f, 0.14f, 0.18f, 1f)),
                    FloatAttribute.createShininess(18f),
                ),
            )
        BoxShapeBuilder.build(inlandGround, 9600f, 3f, 9800f, 1180f, -2f, 220f)
        val sea =
            mb.part(
                "sea",
                GL20.GL_TRIANGLES,
                attr,
                Material(
                    TextureAttribute.createDiffuse(seaSet.diffuse),
                    TextureAttribute.createSpecular(seaSet.roughness),
                    ColorAttribute.createDiffuse(Color.WHITE),
                    ColorAttribute.createSpecular(Color(0.44f, 0.66f, 0.84f, 1f)),
                    FloatAttribute.createShininess(56f),
                ),
            )
        BoxShapeBuilder.build(sea, 9600f, 2f, 3600f, 1180f, -4f, -4580f)
        BoxShapeBuilder.build(sea, 8200f, 1f, 1280f, 1180f, -4f, -3170f)
        val beach =
            mb.part(
                "beach",
                GL20.GL_TRIANGLES,
                attr,
                Material(
                    TextureAttribute.createDiffuse(beachSet.diffuse),
                    TextureAttribute.createSpecular(beachSet.roughness),
                    ColorAttribute.createDiffuse(Color(1f, 0.98f, 0.92f, 1f)),
                    ColorAttribute.createSpecular(Color(0.2f, 0.18f, 0.12f, 1f)),
                    FloatAttribute.createShininess(8f),
                ),
            )
        BoxShapeBuilder.build(beach, 8600f, 4f, 760f, 1180f, -1f, -2550f)
        val promenade =
            mb.part(
                "promenade",
                GL20.GL_TRIANGLES,
                attr,
                Material(
                    createTiledAttribute(promenadeSet.diffuse, 18f, 2.6f),
                    TextureAttribute.createSpecular(promenadeSet.roughness),
                    ColorAttribute.createDiffuse(Color(0.6f, 0.62f, 0.66f, 1f)),
                    ColorAttribute.createSpecular(Color(0.22f, 0.24f, 0.28f, 1f)),
                    FloatAttribute.createShininess(28f),
                ),
            )
        BoxShapeBuilder.build(promenade, 8400f, 2f, 180f, 1180f, 0f, -2100f)
        val park =
            mb.part(
                "park",
                GL20.GL_TRIANGLES,
                attr,
                Material(
                    TextureAttribute.createDiffuse(parkSet.diffuse),
                    TextureAttribute.createSpecular(parkSet.roughness),
                    ColorAttribute.createDiffuse(Color.WHITE),
                    ColorAttribute.createSpecular(Color(0.12f, 0.18f, 0.1f, 1f)),
                    FloatAttribute.createShininess(6f),
                ),
            )
        BoxShapeBuilder.build(park, 1560f, 2f, 3600f, 880f, -1f, -820f)
        val road =
            mb.part(
                "road",
                GL20.GL_TRIANGLES,
                attr,
                Material(
                    createTiledAttribute(roadSet.diffuse, 18f, 3.2f),
                    TextureAttribute.createSpecular(roadSet.roughness),
                    ColorAttribute.createDiffuse(Color(0.34f, 0.36f, 0.4f, 1f)),
                    ColorAttribute.createSpecular(Color(0.18f, 0.18f, 0.2f, 1f)),
                    FloatAttribute.createShininess(22f),
                ),
            )
        BoxShapeBuilder.build(road, 8600f, 1f, 120f, 1180f, 0f, -1910f)
        BoxShapeBuilder.build(road, 7600f, 1f, 48f, 1180f, 0f, -640f)
        BoxShapeBuilder.build(road, 6200f, 1f, 42f, 1480f, 0f, 180f)
        BoxShapeBuilder.build(road, 50f, 1f, 4200f, -260f, 0f, -620f)
        BoxShapeBuilder.build(road, 52f, 1f, 4700f, 920f, 0f, -320f)
        BoxShapeBuilder.build(road, 48f, 1f, 4400f, 2140f, 0f, -460f)
        val glowBand =
            mb.part(
                "glow",
                GL20.GL_TRIANGLES,
                attr,
                Material(
                    ColorAttribute.createDiffuse(Color(0.74f, 0.46f, 0.18f, 1f)),
                    BlendingAttribute(0.2f),
                ),
            )
        BoxShapeBuilder.build(glowBand, 8400f, 0.5f, 260f, 1180f, 5f, -2070f)
        BoxShapeBuilder.build(glowBand, 8200f, 0.5f, 620f, 1180f, 4f, -1820f)
        val defensePad =
            mb.part(
                "defense_pad",
                GL20.GL_TRIANGLES,
                attr,
                Material(
                    createTiledAttribute(groundSet.diffuse, 4f, 2f),
                    TextureAttribute.createSpecular(groundSet.roughness),
                    ColorAttribute.createDiffuse(Color(0.48f, 0.5f, 0.54f, 1f)),
                    ColorAttribute.createSpecular(Color(0.18f, 0.18f, 0.2f, 1f)),
                    FloatAttribute.createShininess(26f),
                ),
            )
        BoxShapeBuilder.build(defensePad, 2520f, 14f, 620f, 520f, 5f, 310f)
        models.put("ground", mb.end())
    }

    private fun generateBuildingModels() {
        val attr = defaultVertexAttributes()
        val mb = ModelBuilder()
        val facadeA = createFacadeTextureSet(192, 384, Color(0.08f, 0.1f, 0.14f, 1f), Color(1f, 0.82f, 0.48f, 1f))
        val facadeB = createFacadeTextureSet(192, 384, Color(0.05f, 0.07f, 0.11f, 1f), Color(0.7f, 0.88f, 1f, 1f))
        val facadeC = createFacadeTextureSet(192, 384, Color(0.1f, 0.08f, 0.09f, 1f), Color(1f, 0.62f, 0.3f, 1f))
        val facadeD = createFacadeTextureSet(192, 384, Color(0.07f, 0.09f, 0.12f, 1f), Color(0.56f, 0.96f, 0.96f, 1f))
        val facadeE = createFacadeTextureSet(192, 384, Color(0.12f, 0.1f, 0.08f, 1f), Color(1f, 0.9f, 0.62f, 1f))

        fun createBuildingModel(
            name: String,
            width: Float,
            height: Float,
            depth: Float,
            texture: SurfaceTextureSet,
            tint: Color,
        ) {
            val glow =
                Color(
                    (tint.r * 0.13f).coerceIn(0f, 0.22f),
                    (tint.g * 0.13f).coerceIn(0f, 0.22f),
                    (tint.b * 0.16f).coerceIn(0f, 0.28f),
                    1f,
                )
            models.put(
                name,
                mb.createBox(
                    width,
                    height,
                    depth,
                    Material(
                        TextureAttribute.createDiffuse(texture.diffuse),
                        TextureAttribute.createSpecular(texture.roughness),
                        ColorAttribute.createDiffuse(tint),
                        ColorAttribute.createEmissive(glow),
                        ColorAttribute.createSpecular(Color(0.12f, 0.14f, 0.18f, 1f)),
                        FloatAttribute.createShininess(12f),
                    ),
                    attr,
                ),
            )
        }

        createBuildingModel("tower_a", 58f, 280f, 58f, facadeA, Color(0.9f, 0.95f, 1f, 1f))
        createBuildingModel("tower_b", 84f, 210f, 84f, facadeB, Color(0.82f, 0.9f, 1f, 1f))
        createBuildingModel("tower_c", 120f, 130f, 90f, facadeC, Color(1f, 0.95f, 0.9f, 1f))
        createBuildingModel("tower_d", 96f, 360f, 74f, facadeD, Color(0.84f, 0.98f, 1f, 1f))
        createBuildingModel("tower_e", 146f, 178f, 112f, facadeE, Color(1f, 0.94f, 0.86f, 1f))
        createBuildingModel("podium", 180f, 78f, 120f, facadeB, Color(0.9f, 0.94f, 1f, 1f))
        createBuildingModel("hotel", 132f, 118f, 72f, facadeC, Color(1f, 0.96f, 0.9f, 1f))
        createBuildingModel("coastal_slab", 228f, 96f, 56f, facadeE, Color(1f, 0.97f, 0.91f, 1f))
        createBuildingModel("office_slab", 168f, 152f, 92f, facadeA, Color(0.86f, 0.92f, 1f, 1f))
        createBuildingModel("needle_tower", 44f, 420f, 44f, facadeD, Color(0.82f, 0.96f, 1f, 1f))
        createBuildingModel("setback_tower", 118f, 304f, 92f, facadeB, Color(0.9f, 0.96f, 1f, 1f))
    }

    private fun generateDefenseModels() {
        val attr = defaultVertexAttributes()
        val mb = ModelBuilder()
        val launcherSet = createMetalTextureSet(Color(0.18f, 0.24f, 0.19f, 1f))
        val radarSet = createMetalTextureSet(Color(0.22f, 0.32f, 0.24f, 1f))
        val launcherMaterial =
            Material(
                TextureAttribute.createDiffuse(launcherSet.diffuse),
                TextureAttribute.createSpecular(launcherSet.roughness),
                ColorAttribute.createDiffuse(Color.WHITE),
                ColorAttribute.createSpecular(Color(0.5f, 0.56f, 0.52f, 1f)),
                FloatAttribute.createShininess(40f),
            )
        mb.begin()
        mb.part("chassis", GL20.GL_TRIANGLES, attr, launcherMaterial).apply {
            BoxShapeBuilder.build(this, 48f, 7f, 72f)
        }
        mb.part("cab", GL20.GL_TRIANGLES, attr, launcherMaterial).apply {
            BoxShapeBuilder.build(this, 26f, 14f, 24f, 0f, 10f, 24f)
        }
        repeat(4) { index ->
            mb.part("tube_$index", GL20.GL_TRIANGLES, attr, launcherMaterial).apply {
                BoxShapeBuilder.build(this, 6f, 6f, 44f, -12f + index * 8f, 20f, -10f)
            }
        }
        models.put("launcher", mb.end())

        mb.begin()
        val radarMaterial =
            Material(
                TextureAttribute.createDiffuse(radarSet.diffuse),
                TextureAttribute.createSpecular(radarSet.roughness),
                ColorAttribute.createDiffuse(Color.WHITE),
                ColorAttribute.createSpecular(Color(0.55f, 0.64f, 0.6f, 1f)),
                FloatAttribute.createShininess(44f),
            )
        mb.part("base", GL20.GL_TRIANGLES, attr, radarMaterial).apply {
            BoxShapeBuilder.build(this, 56f, 10f, 56f)
        }
        mb.part("face", GL20.GL_TRIANGLES, attr, radarMaterial).apply {
            BoxShapeBuilder.build(this, 82f, 46f, 8f, 0f, 36f, -10f)
        }
        models.put("radar", mb.end())
    }

    private fun generateProjectileModels() {
        val attr = defaultVertexAttributes()
        val mb = ModelBuilder()
        val threatSet = createMetalTextureSet(Color(0.32f, 0.35f, 0.38f, 1f))
        val interceptorSet = createMetalTextureSet(Color(0.9f, 0.92f, 0.94f, 1f))
        val debrisSet = createConcreteTextureSet(Color(0.22f, 0.24f, 0.28f, 1f))
        val blastSet = createSolidTextureSet(Color(1f, 0.82f, 0.35f, 1f), 0.12f)
        val trailSet = createSolidTextureSet(Color(0.92f, 0.9f, 0.84f, 1f), 0.8f)
        val moonSet = createSolidTextureSet(Color(0.88f, 0.9f, 1f, 1f), 0.65f)

        mb.begin()
        val threatMaterial =
            Material(
                TextureAttribute.createDiffuse(threatSet.diffuse),
                TextureAttribute.createSpecular(threatSet.roughness),
                ColorAttribute.createDiffuse(Color(1f, 0.78f, 0.42f, 1f)),
                ColorAttribute.createSpecular(Color(1f, 0.88f, 0.66f, 1f)),
                ColorAttribute.createEmissive(Color(0.18f, 0.08f, 0.03f, 1f)),
                FloatAttribute.createShininess(78f),
            )
        mb.part("body", GL20.GL_TRIANGLES, attr, threatMaterial).apply {
            CylinderShapeBuilder.build(this, 8f, 34f, 8f, 20)
        }
        mb.part("nose", GL20.GL_TRIANGLES, attr, threatMaterial).apply {
            tmpMatrix.idt().translate(0f, 22f, 0f)
            setVertexTransform(tmpMatrix)
            ConeShapeBuilder.build(this, 8.8f, 13f, 8.8f, 20)
        }
        repeat(4) { index ->
            mb.part("fin_$index", GL20.GL_TRIANGLES, attr, threatMaterial).apply {
                val yaw = index * 90f
                tmpMatrix.idt().rotate(Vector3.Y, yaw)
                tmpMatrix.translate(0f, -10f, 0f)
                setVertexTransform(tmpMatrix)
                BoxShapeBuilder.build(this, 8f, 10f, 0.8f)
            }
        }
        models.put("threat", mb.end())

        mb.begin()
        val interceptorMaterial =
            Material(
                TextureAttribute.createDiffuse(interceptorSet.diffuse),
                TextureAttribute.createSpecular(interceptorSet.roughness),
                ColorAttribute.createDiffuse(Color(0.82f, 0.96f, 1f, 1f)),
                ColorAttribute.createSpecular(Color(0.94f, 0.98f, 1f, 1f)),
                ColorAttribute.createEmissive(Color(0.05f, 0.12f, 0.18f, 1f)),
                FloatAttribute.createShininess(92f),
            )
        mb.part("body", GL20.GL_TRIANGLES, attr, interceptorMaterial).apply {
            CylinderShapeBuilder.build(this, 5.4f, 32f, 5.4f, 20)
        }
        mb.part("nose", GL20.GL_TRIANGLES, attr, interceptorMaterial).apply {
            tmpMatrix.idt().translate(0f, 21f, 0f)
            setVertexTransform(tmpMatrix)
            ConeShapeBuilder.build(this, 5.8f, 11f, 5.8f, 20)
        }
        repeat(4) { index ->
            mb.part("fin_$index", GL20.GL_TRIANGLES, attr, interceptorMaterial).apply {
                val yaw = index * 90f + 45f
                tmpMatrix.idt().rotate(Vector3.Y, yaw)
                tmpMatrix.translate(0f, -10f, 0f)
                setVertexTransform(tmpMatrix)
                BoxShapeBuilder.build(this, 5f, 6f, 0.55f)
            }
        }
        models.put("interceptor", mb.end())
        models.put(
            "blast",
            mb.createSphere(
                1f,
                1f,
                1f,
                16,
                12,
                Material(
                    TextureAttribute.createDiffuse(blastSet.diffuse),
                    TextureAttribute.createSpecular(blastSet.roughness),
                    ColorAttribute.createDiffuse(Color.WHITE),
                    ColorAttribute.createSpecular(Color.WHITE),
                    ColorAttribute.createEmissive(Color(1f, 0.65f, 0.2f, 1f)),
                    BlendingAttribute(0.92f),
                ),
                attr,
            ),
        )
        models.put(
            "trail",
            mb.createSphere(
                1f,
                1f,
                1f,
                10,
                8,
                Material(
                    TextureAttribute.createDiffuse(trailSet.diffuse),
                    TextureAttribute.createSpecular(trailSet.roughness),
                    ColorAttribute.createDiffuse(Color.WHITE),
                    ColorAttribute.createEmissive(Color(0.32f, 0.38f, 0.44f, 1f)),
                    BlendingAttribute(0.55f),
                ),
                attr,
            ),
        )
        models.put(
            "debris",
            mb.createBox(
                6f,
                6f,
                6f,
                Material(
                    TextureAttribute.createDiffuse(debrisSet.diffuse),
                    TextureAttribute.createSpecular(debrisSet.roughness),
                    ColorAttribute.createDiffuse(Color.WHITE),
                    ColorAttribute.createSpecular(Color(0.18f, 0.18f, 0.2f, 1f)),
                    FloatAttribute.createShininess(10f),
                ),
                attr,
            ),
        )
        models.put(
            "moon",
            mb.createSphere(
                80f,
                80f,
                80f,
                18,
                18,
                Material(
                    TextureAttribute.createDiffuse(moonSet.diffuse),
                    TextureAttribute.createSpecular(moonSet.roughness),
                    ColorAttribute.createDiffuse(Color.WHITE),
                    ColorAttribute.createEmissive(Color(0.18f, 0.2f, 0.24f, 1f)),
                ),
                attr,
            ),
        )
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

    private fun generateWorldModels() {
        val attr =
            (
                VertexAttributes.Usage.Position or
                    VertexAttributes.Usage.Normal or
                    VertexAttributes.Usage.TextureCoordinates
            ).toLong()
        val mb = ModelBuilder()
        battleSkyTexture = createSkyTexture()
        val facadeWidth = scaledFacadeSize(256, minimum = 128)
        val facadeHeight = scaledFacadeSize(512, minimum = 256)
        val facadeA = createFacadeTextureSet(facadeWidth, facadeHeight, Color(0.08f, 0.1f, 0.14f, 1f), Color(1f, 0.82f, 0.48f, 1f))
        val facadeB = createFacadeTextureSet(facadeWidth, facadeHeight, Color(0.05f, 0.07f, 0.11f, 1f), Color(0.7f, 0.88f, 1f, 1f))
        val facadeC = createFacadeTextureSet(facadeWidth, facadeHeight, Color(0.1f, 0.08f, 0.09f, 1f), Color(1f, 0.62f, 0.3f, 1f))
        val facadeD = createFacadeTextureSet(facadeWidth, facadeHeight, Color(0.07f, 0.09f, 0.12f, 1f), Color(0.56f, 0.96f, 0.96f, 1f))
        val facadeE = createFacadeTextureSet(facadeWidth, facadeHeight, Color(0.12f, 0.1f, 0.08f, 1f), Color(1f, 0.9f, 0.62f, 1f))
        val groundSet = createGroundTextureSet()
        val seaSet = createSeaTextureSet()
        val beachSet = createBeachTextureSet()
        val parkSet = createParkTextureSet()
        val promenadeSet = createPromenadeTextureSet()
        val roadSet = createRoadTextureSet()
        val launcherSet = createMetalTextureSet(Color(0.18f, 0.24f, 0.19f, 1f))
        val radarSet = createMetalTextureSet(Color(0.22f, 0.32f, 0.24f, 1f))
        val threatSet = createMetalTextureSet(Color(0.32f, 0.35f, 0.38f, 1f))
        val interceptorSet = createMetalTextureSet(Color(0.9f, 0.92f, 0.94f, 1f))
        val debrisSet = createConcreteTextureSet(Color(0.22f, 0.24f, 0.28f, 1f))
        val blastSet = createSolidTextureSet(Color(1f, 0.82f, 0.35f, 1f), 0.12f)
        val trailSet = createSolidTextureSet(Color(0.92f, 0.9f, 0.84f, 1f), 0.8f)
        val moonSet = createSolidTextureSet(Color(0.88f, 0.9f, 1f, 1f), 0.65f)

        mb.begin()
        val inlandGround =
            mb.part(
                "inland_ground",
                GL20.GL_TRIANGLES,
                attr,
                Material(
                    TextureAttribute.createDiffuse(groundSet.diffuse),
                    TextureAttribute.createSpecular(groundSet.roughness),
                    ColorAttribute.createDiffuse(Color(0.95f, 0.98f, 1f, 1f)),
                    ColorAttribute.createSpecular(Color(0.18f, 0.2f, 0.24f, 1f)),
                    FloatAttribute.createShininess(12f),
                ),
            )
        BoxShapeBuilder.build(inlandGround, 7600f, 3f, 11000f, 1350f, -2f, -300f)
        val sea =
            mb.part(
                "sea",
                GL20.GL_TRIANGLES,
                attr,
                Material(
                    TextureAttribute.createDiffuse(seaSet.diffuse),
                    TextureAttribute.createSpecular(seaSet.roughness),
                    ColorAttribute.createDiffuse(Color.WHITE),
                    ColorAttribute.createSpecular(Color(0.45f, 0.62f, 0.78f, 1f)),
                    FloatAttribute.createShininess(46f),
                ),
            )
        BoxShapeBuilder.build(sea, 4200f, 2f, 11000f, -3550f, -3f, -300f)
        val beach =
            mb.part(
                "beach",
                GL20.GL_TRIANGLES,
                attr,
                Material(
                    TextureAttribute.createDiffuse(beachSet.diffuse),
                    TextureAttribute.createSpecular(beachSet.roughness),
                    ColorAttribute.createDiffuse(Color.WHITE),
                    ColorAttribute.createSpecular(Color(0.2f, 0.18f, 0.12f, 1f)),
                    FloatAttribute.createShininess(8f),
                ),
            )
        BoxShapeBuilder.build(beach, 900f, 4f, 9800f, -2150f, -1f, -340f)
        val promenade =
            mb.part(
                "promenade",
                GL20.GL_TRIANGLES,
                attr,
                Material(
                    TextureAttribute.createDiffuse(promenadeSet.diffuse),
                    TextureAttribute.createSpecular(promenadeSet.roughness),
                    ColorAttribute.createDiffuse(Color.WHITE),
                    ColorAttribute.createSpecular(Color(0.22f, 0.22f, 0.24f, 1f)),
                    FloatAttribute.createShininess(18f),
                ),
            )
        BoxShapeBuilder.build(promenade, 260f, 2f, 9300f, -1500f, 0f, -360f)
        val park =
            mb.part(
                "park",
                GL20.GL_TRIANGLES,
                attr,
                Material(
                    TextureAttribute.createDiffuse(parkSet.diffuse),
                    TextureAttribute.createSpecular(parkSet.roughness),
                    ColorAttribute.createDiffuse(Color.WHITE),
                    ColorAttribute.createSpecular(Color(0.12f, 0.18f, 0.1f, 1f)),
                    FloatAttribute.createShininess(6f),
                ),
            )
        BoxShapeBuilder.build(park, 920f, 2f, 4200f, 980f, -1f, -1120f)
        val road =
            mb.part(
                "road",
                GL20.GL_TRIANGLES,
                attr,
                Material(
                    TextureAttribute.createDiffuse(roadSet.diffuse),
                    TextureAttribute.createSpecular(roadSet.roughness),
                    ColorAttribute.createDiffuse(Color.WHITE),
                    ColorAttribute.createSpecular(Color(0.28f, 0.28f, 0.3f, 1f)),
                    FloatAttribute.createShininess(18f),
                ),
            )
        BoxShapeBuilder.build(road, 7200f, 1f, 42f, 600f, 0f, 210f)
        BoxShapeBuilder.build(road, 5600f, 1f, 38f, 220f, 0f, -1160f)
        BoxShapeBuilder.build(road, 40f, 1f, 8800f, -1160f, 0f, -280f)
        BoxShapeBuilder.build(road, 44f, 1f, 7600f, 840f, 0f, -480f)
        BoxShapeBuilder.build(road, 38f, 1f, 6000f, 1780f, 0f, -580f)
        val glowBand =
            mb.part(
                "glow",
                GL20.GL_TRIANGLES,
                attr,
                Material(
                    ColorAttribute.createDiffuse(Color(0.15f, 0.22f, 0.35f, 1f)),
                    BlendingAttribute(0.18f),
                ),
            )
        BoxShapeBuilder.build(glowBand, 7200f, 0.5f, 1100f, 760f, 6f, -420f)
        BoxShapeBuilder.build(glowBand, 1800f, 0.5f, 8600f, -1450f, 4f, -320f)
        models.put("ground", mb.end())

        fun createBuildingModel(
            name: String,
            width: Float,
            height: Float,
            depth: Float,
            texture: SurfaceTextureSet,
            tint: Color,
        ) {
            models.put(
                name,
                mb.createBox(
                    width,
                    height,
                    depth,
                    Material(
                        TextureAttribute.createDiffuse(texture.diffuse),
                        TextureAttribute.createSpecular(texture.roughness),
                        ColorAttribute.createDiffuse(tint),
                        ColorAttribute.createSpecular(Color(0.12f, 0.14f, 0.18f, 1f)),
                        FloatAttribute.createShininess(8f),
                    ),
                    attr,
                ),
            )
        }

        createBuildingModel("tower_a", 58f, 280f, 58f, facadeA, Color(0.9f, 0.95f, 1f, 1f))
        createBuildingModel("tower_b", 84f, 210f, 84f, facadeB, Color(0.82f, 0.9f, 1f, 1f))
        createBuildingModel("tower_c", 120f, 130f, 90f, facadeC, Color(1f, 0.95f, 0.9f, 1f))
        createBuildingModel("tower_d", 96f, 360f, 74f, facadeD, Color(0.84f, 0.98f, 1f, 1f))
        createBuildingModel("tower_e", 146f, 178f, 112f, facadeE, Color(1f, 0.94f, 0.86f, 1f))
        createBuildingModel("podium", 180f, 78f, 120f, facadeB, Color(0.9f, 0.94f, 1f, 1f))
        createBuildingModel("hotel", 132f, 118f, 72f, facadeC, Color(1f, 0.96f, 0.9f, 1f))

        val launcherMaterial =
            Material(
                TextureAttribute.createDiffuse(launcherSet.diffuse),
                TextureAttribute.createSpecular(launcherSet.roughness),
                ColorAttribute.createDiffuse(Color.WHITE),
                ColorAttribute.createSpecular(Color(0.5f, 0.56f, 0.52f, 1f)),
                FloatAttribute.createShininess(40f),
            )
        mb.begin()
        mb.part("chassis", GL20.GL_TRIANGLES, attr, launcherMaterial).apply {
            BoxShapeBuilder.build(this, 34f, 5f, 54f)
        }
        mb.part("cab", GL20.GL_TRIANGLES, attr, launcherMaterial).apply {
            BoxShapeBuilder.build(this, 18f, 10f, 16f, 0f, 7f, 18f)
        }
        repeat(4) { index ->
            mb.part("tube_$index", GL20.GL_TRIANGLES, attr, launcherMaterial).apply {
                BoxShapeBuilder.build(this, 5f, 5f, 34f, -9f + index * 6f, 15f, -6f)
            }
        }
        models.put("launcher", mb.end())

        mb.begin()
        val radarMaterial =
            Material(
                TextureAttribute.createDiffuse(radarSet.diffuse),
                TextureAttribute.createSpecular(radarSet.roughness),
                ColorAttribute.createDiffuse(Color.WHITE),
                ColorAttribute.createSpecular(Color(0.55f, 0.64f, 0.6f, 1f)),
                FloatAttribute.createShininess(44f),
            )
        mb.part("base", GL20.GL_TRIANGLES, attr, radarMaterial).apply {
            BoxShapeBuilder.build(this, 42f, 8f, 42f)
        }
        mb.part("face", GL20.GL_TRIANGLES, attr, radarMaterial).apply {
            BoxShapeBuilder.build(this, 54f, 34f, 6f, 0f, 28f, -8f)
        }
        models.put("radar", mb.end())

        mb.begin()
        val threatMaterial =
            Material(
                TextureAttribute.createDiffuse(threatSet.diffuse),
                TextureAttribute.createSpecular(threatSet.roughness),
                ColorAttribute.createDiffuse(Color(1f, 0.78f, 0.42f, 1f)),
                ColorAttribute.createSpecular(Color(1f, 0.88f, 0.66f, 1f)),
                ColorAttribute.createEmissive(Color(0.18f, 0.08f, 0.03f, 1f)),
                FloatAttribute.createShininess(78f),
            )
        mb.part("body", GL20.GL_TRIANGLES, attr, threatMaterial).apply {
            CylinderShapeBuilder.build(this, 8f, 34f, 8f, 20)
        }
        mb.part("nose", GL20.GL_TRIANGLES, attr, threatMaterial).apply {
            tmpMatrix.idt().translate(0f, 22f, 0f)
            setVertexTransform(tmpMatrix)
            ConeShapeBuilder.build(this, 8.8f, 13f, 8.8f, 20)
        }
        repeat(4) { index ->
            mb.part("fin_$index", GL20.GL_TRIANGLES, attr, threatMaterial).apply {
                val yaw = index * 90f
                tmpMatrix.idt().rotate(Vector3.Y, yaw)
                tmpMatrix.translate(0f, -10f, 0f)
                setVertexTransform(tmpMatrix)
                BoxShapeBuilder.build(this, 8f, 10f, 0.8f)
            }
        }
        models.put("threat", mb.end())

        mb.begin()
        val interceptorMaterial =
            Material(
                TextureAttribute.createDiffuse(interceptorSet.diffuse),
                TextureAttribute.createSpecular(interceptorSet.roughness),
                ColorAttribute.createDiffuse(Color(0.82f, 0.96f, 1f, 1f)),
                ColorAttribute.createSpecular(Color(0.94f, 0.98f, 1f, 1f)),
                ColorAttribute.createEmissive(Color(0.05f, 0.12f, 0.18f, 1f)),
                FloatAttribute.createShininess(92f),
            )
        mb.part("body", GL20.GL_TRIANGLES, attr, interceptorMaterial).apply {
            CylinderShapeBuilder.build(this, 5.4f, 32f, 5.4f, 20)
        }
        mb.part("nose", GL20.GL_TRIANGLES, attr, interceptorMaterial).apply {
            tmpMatrix.idt().translate(0f, 21f, 0f)
            setVertexTransform(tmpMatrix)
            ConeShapeBuilder.build(this, 5.8f, 11f, 5.8f, 20)
        }
        repeat(4) { index ->
            mb.part("fin_$index", GL20.GL_TRIANGLES, attr, interceptorMaterial).apply {
                val yaw = index * 90f + 45f
                tmpMatrix.idt().rotate(Vector3.Y, yaw)
                tmpMatrix.translate(0f, -10f, 0f)
                setVertexTransform(tmpMatrix)
                BoxShapeBuilder.build(this, 5f, 6f, 0.55f)
            }
        }
        models.put("interceptor", mb.end())

        models.put(
            "blast",
            mb.createSphere(
                1f,
                1f,
                1f,
                16,
                12,
                Material(
                    TextureAttribute.createDiffuse(blastSet.diffuse),
                    TextureAttribute.createSpecular(blastSet.roughness),
                    ColorAttribute.createDiffuse(Color.WHITE),
                    ColorAttribute.createSpecular(Color.WHITE),
                    ColorAttribute.createEmissive(Color(1f, 0.65f, 0.2f, 1f)),
                    BlendingAttribute(0.92f),
                ),
                attr,
            ),
        )
        models.put(
            "trail",
            mb.createSphere(
                1f,
                1f,
                1f,
                10,
                8,
                Material(
                    TextureAttribute.createDiffuse(trailSet.diffuse),
                    TextureAttribute.createSpecular(trailSet.roughness),
                    ColorAttribute.createDiffuse(Color.WHITE),
                    ColorAttribute.createEmissive(Color(0.32f, 0.38f, 0.44f, 1f)),
                    BlendingAttribute(0.55f),
                ),
                attr,
            ),
        )
        models.put(
            "debris",
            mb.createBox(
                6f,
                6f,
                6f,
                Material(
                    TextureAttribute.createDiffuse(debrisSet.diffuse),
                    TextureAttribute.createSpecular(debrisSet.roughness),
                    ColorAttribute.createDiffuse(Color.WHITE),
                    ColorAttribute.createSpecular(Color(0.18f, 0.18f, 0.2f, 1f)),
                    FloatAttribute.createShininess(10f),
                ),
                attr,
            ),
        )
        models.put(
            "moon",
            mb.createSphere(
                80f,
                80f,
                80f,
                18,
                18,
                Material(
                    TextureAttribute.createDiffuse(moonSet.diffuse),
                    TextureAttribute.createSpecular(moonSet.roughness),
                    ColorAttribute.createDiffuse(Color.WHITE),
                    ColorAttribute.createEmissive(Color(0.18f, 0.2f, 0.24f, 1f)),
                ),
                attr,
            ),
        )

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

    private fun applySimulationStep(step: BattleStepEvents) {
        step.spawnedThreatIds.forEach { id ->
            simulation.findThreat(id)?.let { threat ->
                val instance = ModelInstance(models.get("threat"))
                val renderThreat =
                    ThreatEntity(
                        instance = instance,
                        position = threat.position.cpy(),
                        targetPosition = threat.targetPosition.cpy(),
                        velocity = threat.velocity.cpy(),
                        id = threat.id,
                        isTracked = threat.isTracked,
                        trailCooldown = threat.trailCooldown,
                    )
                syncProjectileTransform(renderThreat.instance, renderThreat.position, renderThreat.velocity, THREAT_SCALE)
                threats.add(renderThreat)
                threatsById.put(id, renderThreat)
            }
        }

        step.launchedInterceptors.forEach { launch ->
            val interceptorState = simulation.findInterceptor(launch.interceptorId) ?: return@forEach
            val instance = ModelInstance(models.get("interceptor"))
            val renderInterceptor =
                InterceptorEntity(
                    id = interceptorState.id,
                    instance = instance,
                    position = interceptorState.position.cpy(),
                    velocity = interceptorState.velocity.cpy(),
                    target = interceptorState.targetId?.let { threatsById[it] },
                    trailCooldown = interceptorState.trailCooldown,
                )
            syncProjectileTransform(renderInterceptor.instance, renderInterceptor.position, renderInterceptor.velocity, INTERCEPTOR_SCALE)
            interceptors.add(renderInterceptor)
            interceptorsById.put(renderInterceptor.id, renderInterceptor)
            if (launch.launcherIndex in 0 until launchers.size) {
                launchers[launch.launcherIndex].setRotationToward(interceptorState.velocity)
            }
            spawnBlast(launch.launcherPosition, 14f)
            when (launch.launcherIndex) {
                0 -> launcherLeftPulse = 1f
                1 -> launcherRightPulse = 1f
            }
            playSfx("launch", 0.35f)
        }

        step.trailEvents.forEach { trail ->
            if (canSpawnTrail(trail.hostile)) {
                spawnTrail(trail.position, trail.hostile)
            }
        }

        step.blastEvents.forEach { blast ->
            when (blast.kind) {
                BlastKind.HOSTILE_IMPACT -> {
                    spawnBlast(blast.position, blast.radius)
                    spawnDebris(blast.position, 24, Color(0.4f, 0.38f, 0.36f, 1f))
                    triggerShake(26f, 0.55f)
                    playSfx("impact", 0.8f)
                }

                BlastKind.INTERCEPT -> {
                    spawnBlast(blast.position, blast.radius)
                    spawnDebris(blast.position, 10, Color(0.7f, 0.7f, 0.74f, 1f))
                    triggerShake(10f, 0.3f)
                    playSfx("detonate", 0.5f)
                }
            }
        }

        step.buildingDamageEvents.forEach { damage ->
            cityBlocksById[damage.buildingId]?.let { building ->
                applyBuildingDamageVisual(building, damage.integrity, damage.epicenter)
            }
        }

        step.removedThreatIds.forEach { id ->
            threatsById.remove(id)?.let { threats.removeValue(it, true) }
        }

        step.removedInterceptorIds.forEach { id ->
            interceptorsById.remove(id)?.let { interceptors.removeValue(it, true) }
        }

        syncRenderEntitiesFromSimulation()
        syncBattleStateFromSimulation()

        if (step.waveCleared) {
            statusLabel.style = skin.get("status", Label.LabelStyle::class.java)
            statusLabel.setText("SKY CLEAR // PREPARE NEXT WAVE")
            refreshWaveButton()
        }
        if (step.gameOver) {
            statusLabel.style = skin.get("critical", Label.LabelStyle::class.java)
            statusLabel.setText("DEFENSE FAILED")
            refreshWaveButton()
        }
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
        stage.clear()
        val uiScale = Gdx.graphics.height / 1080f
        val root = Table().apply { setFillParent(true) }

        val top =
            Table().apply {
                background = this@BattleScreen.skin.getDrawable("hud_soft")
                pad(8f * uiScale)
            }
        val missionBlock = Table().apply { defaults().left() }
        missionBlock.add(Label("BATTLESPACE", skin, "title")).row()
        missionBlock.add(statusLabel).padTop(4f * uiScale).row()
        top.add(missionBlock).expandX().left()
        top.add(creditsLabel).right().padLeft(18f * uiScale)
        root
            .add(top)
            .expandX()
            .fillX()
            .top()
            .pad(12f * uiScale)
            .row()

        root.add().expand().row()

        val dock =
            Table().apply {
                background = this@BattleScreen.skin.getDrawable("hud_soft")
                pad(10f * uiScale)
                defaults().pad(6f * uiScale)
            }

        val rangeCard =
            Table().apply {
                background = this@BattleScreen.skin.getDrawable("hud_soft")
                pad(12f * uiScale)
                defaults().left()
            }
        rangeValueLabel = Label("", skin, "display")
        rangeCard.add(Label("AUTO ENGAGEMENT RANGE", skin, "status")).row()
        rangeCard.add(rangeValueLabel).padTop(8f * uiScale).row()
        rangeCard
            .add(
                Slider(800f, 2800f, 25f, false, skin).apply {
                    value = settings.engagementRange
                    addListener(
                        object : ChangeListener() {
                            override fun changed(
                                event: ChangeEvent?,
                                actor: Actor?,
                            ) {
                                settings.engagementRange = value
                                updateHud()
                            }
                        },
                    )
                },
            ).width(320f * uiScale)
            .fillX()
            .height(48f * uiScale)
            .padTop(10f * uiScale)

        val speedCard =
            Table().apply {
                background = this@BattleScreen.skin.getDrawable("hud_soft")
                pad(12f * uiScale)
                defaults().left()
            }
        speedValueLabel = Label("", skin, "display")
        speedCard.add(Label("INTERCEPTOR SPEED", skin, "status")).row()
        speedCard.add(speedValueLabel).padTop(8f * uiScale).row()
        speedCard
            .add(
                Slider(320f, 700f, 10f, false, skin).apply {
                    value = settings.interceptorSpeed
                    addListener(
                        object : ChangeListener() {
                            override fun changed(
                                event: ChangeEvent?,
                                actor: Actor?,
                            ) {
                                settings.interceptorSpeed = value
                                updateHud()
                            }
                        },
                    )
                },
            ).width(320f * uiScale)
            .fillX()
            .height(48f * uiScale)
            .padTop(10f * uiScale)

        val actionCard =
            Table().apply {
                background = this@BattleScreen.skin.getDrawable("hud_soft")
                pad(12f * uiScale)
                defaults().left()
            }
        actionCard.add(Label("WAVE COMMAND", skin, "status")).row()
        waveButton =
            TextButton("START NEXT WAVE", skin).apply {
                addListener(
                    object : ChangeListener() {
                        override fun changed(
                            event: ChangeEvent?,
                            actor: Actor?,
                        ) {
                            if (!waveInProgress && !isGameOver) startNewWave()
                        }
                    },
                )
            }
        actionCard
            .add(waveButton)
            .width(320f * uiScale)
            .height(96f * uiScale)
            .fillX()
            .padTop(12f * uiScale)

        dock.add(rangeCard).width(342f * uiScale).fillY()
        dock.add(speedCard).width(342f * uiScale).fillY()
        dock.add(actionCard).width(342f * uiScale).fillY()
        root
            .add(dock)
            .expandX()
            .fillX()
            .bottom()
            .pad(12f * uiScale)

        stage.addActor(root)
        updateHud()
        refreshWaveButton()
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
        statusLabel.style = skin.get("critical", Label.LabelStyle::class.java)
        statusLabel.setText("MULTIPLE INBOUND TRACKS // WAVE $wave")
        refreshWaveButton()
    }

    override fun render(delta: Float) {
        if (!initialized) {
            runInitializationStep()
            renderLoading()
            return
        }

        if (isGameOver) {
            renderGameOver()
            return
        }

        updateLogic(min(delta, 1f / 30f))
        battleLiveTime += delta
        renderedBattleFrames++
        if (renderedBattleFrames % 180 == 0) {
            Gdx.app.log("BattleFrame", battleHealthSummary())
        }

        ScreenUtils.clear(0.01f, 0.02f, 0.04f, 1f, true)
        battleSkyRegion?.let { sky ->
            val batch = stage.batch
            stage.viewport.apply()
            batch.projectionMatrix = stage.camera.combined
            batch.begin()
            batch.color = Color.WHITE
            batch.draw(sky, 0f, 0f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
            batch.setColor(0.02f, 0.07f, 0.14f, 0.44f)
            batch.draw(
                whiteRegion,
                0f,
                Gdx.graphics.height * 0.22f,
                Gdx.graphics.width.toFloat(),
                Gdx.graphics.height * 0.11f,
            )
            batch.setColor(0.78f, 0.62f, 0.4f, 0.16f)
            batch.draw(
                whiteRegion,
                0f,
                Gdx.graphics.height * 0.205f,
                Gdx.graphics.width.toFloat(),
                Gdx.graphics.height * 0.028f,
            )
            batch.setColor(0.96f, 0.66f, 0.24f, 0.14f)
            batch.draw(
                whiteRegion,
                0f,
                Gdx.graphics.height * 0.238f,
                Gdx.graphics.width.toFloat(),
                Gdx.graphics.height * 0.012f,
            )
            battleReflectionTexture?.let { reflection ->
                val drift = kotlin.math.sin(battleLiveTime * 0.36f) * Gdx.graphics.width * 0.015f
                val impactFlash = (impactLight.intensity / 1200f).coerceIn(0f, 1f)
                batch.setColor(1f, 1f, 1f, 0.3f + impactFlash * 0.14f)
                batch.draw(
                    reflection,
                    -drift,
                    Gdx.graphics.height * 0.11f,
                    Gdx.graphics.width * 1.05f,
                    Gdx.graphics.height * 0.14f,
                )
                batch.setColor(1f, 0.78f, 0.38f, impactFlash * 0.18f)
                batch.draw(
                    whiteRegion,
                    0f,
                    Gdx.graphics.height * 0.145f,
                    Gdx.graphics.width.toFloat(),
                    Gdx.graphics.height * 0.028f,
                )
            }
            battleHorizonTexture?.let { horizon ->
                batch.setColor(1f, 1f, 1f, 0.82f)
                batch.draw(
                    horizon,
                    0f,
                    Gdx.graphics.height * 0.19f,
                    Gdx.graphics.width.toFloat(),
                    Gdx.graphics.height * 0.24f,
                )
                battleGlowTexture?.let { glow ->
                    batch.setColor(1f, 1f, 1f, 0.92f)
                    batch.draw(
                        glow,
                        0f,
                        Gdx.graphics.height * 0.12f,
                        Gdx.graphics.width.toFloat(),
                        Gdx.graphics.height * 0.2f,
                    )
                }
                batch.setColor(0.04f, 0.06f, 0.1f, 0.12f)
                batch.draw(
                    whiteRegion,
                    0f,
                    0f,
                    Gdx.graphics.width.toFloat(),
                    Gdx.graphics.height * 0.12f,
                )
            }
            batch.color = Color.WHITE
            batch.end()
        }
        modelBatch.begin(camera)
        instances.forEach { modelBatch.render(it, environment) }
        cityBlocks.forEach { if (it.visibleHeight > 1f) modelBatch.render(it.instance, environment) }
        threats.forEach { modelBatch.render(it.instance, environment) }
        interceptors.forEach { modelBatch.render(it.instance, environment) }
        debris.forEach { modelBatch.render(it.instance, environment) }
        effects.forEach { modelBatch.render(it.instance, environment) }
        modelBatch.end()

        renderAtmosphere()

        stage.act(delta)
        stage.draw()
        renderOverlay()
    }

    private fun renderAtmosphere() {
        val batch = stage.batch
        stage.viewport.apply()
        batch.projectionMatrix = stage.camera.combined
        batch.begin()
        battleFogTexture?.let { fog ->
            batch.setColor(1f, 1f, 1f, 0.78f)
            batch.draw(
                fog,
                0f,
                Gdx.graphics.height * 0.2f,
                Gdx.graphics.width.toFloat(),
                Gdx.graphics.height * 0.16f,
            )
            batch.setColor(1f, 1f, 1f, 0.54f)
            batch.draw(
                fog,
                -Gdx.graphics.width * 0.08f,
                Gdx.graphics.height * 0.1f,
                Gdx.graphics.width * 1.16f,
                Gdx.graphics.height * 0.11f,
            )
        }
        batch.color = Color.WHITE
        batch.end()
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

    private fun renderLoading() {
        ScreenUtils.clear(0.01f, 0.02f, 0.04f, 1f, true)
        val batch = stage.batch
        val font = skin.getFont("default")
        val titleFont = skin.getFont("title")
        val titleLayout = GlyphLayout(titleFont, "INITIALIZING BATTLESPACE")
        val statusLayout = GlyphLayout(font, loadingMessage)
        val diagnosticsLayout = GlyphLayout(font, buildDiagnosticsLine())
        val progress = initializationStep.toFloat() / initializationTasks.size.coerceAtLeast(1)
        val barWidth = Gdx.graphics.width * 0.34f
        val barHeight = 16f
        val barX = (Gdx.graphics.width - barWidth) * 0.5f
        val barY = Gdx.graphics.height * 0.42f

        batch.begin()
        batch.color = Color.WHITE
        titleFont.draw(batch, titleLayout, (Gdx.graphics.width - titleLayout.width) * 0.5f, Gdx.graphics.height * 0.58f)
        font.draw(batch, statusLayout, (Gdx.graphics.width - statusLayout.width) * 0.5f, Gdx.graphics.height * 0.5f)
        font.draw(batch, diagnosticsLayout, (Gdx.graphics.width - diagnosticsLayout.width) * 0.5f, Gdx.graphics.height * 0.46f)
        batch.color = Color(0.04f, 0.1f, 0.16f, 0.95f)
        batch.draw(whiteRegion, barX, barY, barWidth, barHeight)
        batch.color = Color(0.2f, 0.84f, 1f, 1f)
        batch.draw(whiteRegion, barX, barY, barWidth * progress, barHeight)
        batch.color = Color.WHITE
        batch.end()
    }

    private fun buildDiagnosticsLine(): String {
        val currentStep = (initializationStep + 1).coerceAtMost(initializationTasks.size)
        return "STEP $currentStep/${initializationTasks.size} // LAST ${lastInitializationDurationMs}ms // ${qualityProfile.label}"
    }

    private fun renderGameOver() {
        ScreenUtils.clear(0.01f, 0.02f, 0.04f, 1f, true)
        val batch = stage.batch
        batch.begin()
        val titleFont = skin.getFont("title")
        val normalFont = skin.getFont("default")
        val titleLayout = GlyphLayout(titleFont, "CITY LOST")
        val scoreLayout = GlyphLayout(normalFont, "FINAL SCORE: $score    TAP TO RETURN")
        titleFont.draw(batch, titleLayout, (Gdx.graphics.width - titleLayout.width) * 0.5f, Gdx.graphics.height * 0.58f)
        normalFont.draw(batch, scoreLayout, (Gdx.graphics.width - scoreLayout.width) * 0.5f, Gdx.graphics.height * 0.48f)
        batch.end()
    }

    private fun renderOverlay() {
        val batch = stage.batch
        val uiScale = Gdx.graphics.height / 1080f
        val font = skin.getFont("default")
        batch.begin()

        val radarSize = 182f * uiScale
        val radarX = Gdx.graphics.width - radarSize - 20f * uiScale
        val radarY = 20f * uiScale
        batch.color = Color(0.02f, 0.07f, 0.12f, 0.6f)
        batch.draw(whiteRegion, radarX, radarY, radarSize, radarSize)
        val sweepY = RadarProjection.sweepY(radarY, radarSize, radarScanProgress)
        val launcherMarker = RadarProjection.launcherMarker(radarX, radarY, radarSize, radarSize)
        batch.color = Color(0.08f, 0.36f, 0.48f, 0.28f)
        batch.draw(whiteRegion, radarX + radarSize * 0.08f, radarY + radarSize * 0.08f, radarSize * 0.84f, 1.5f * uiScale)
        batch.draw(whiteRegion, radarX + radarSize * 0.08f, radarY + radarSize * 0.5f, radarSize * 0.84f, 1.5f * uiScale)
        batch.draw(whiteRegion, radarX + radarSize * 0.08f, radarY + radarSize * 0.92f, radarSize * 0.84f, 1.5f * uiScale)
        batch.color = Color(0.14f, 0.8f, 1f, 0.18f)
        batch.draw(whiteRegion, radarX + radarSize * 0.08f, sweepY, radarSize * 0.84f, 3.2f * uiScale)
        batch.color = Color(0.28f, 0.92f, 1f, 0.9f)
        batch.draw(whiteRegion, launcherMarker.x - 5f * uiScale, launcherMarker.y - 5f * uiScale, 10f * uiScale, 10f * uiScale)

        threats.forEach { threat ->
            val plot = RadarProjection.project(threat.position, radarX, radarY, radarSize, radarSize)
            batch.color = if (threat.isTracked) Color.RED else Color.YELLOW
            batch.draw(whiteRegion, plot.x - 2.4f * uiScale, plot.y - 2.4f * uiScale, 4.8f * uiScale, 4.8f * uiScale)
        }

        interceptors.forEach { interceptor ->
            val plot = RadarProjection.project(interceptor.position, radarX, radarY, radarSize, radarSize)
            batch.color = Color(0.48f, 0.92f, 1f, 0.92f)
            batch.draw(whiteRegion, plot.x - 1.8f * uiScale, plot.y - 1.8f * uiScale, 3.6f * uiScale, 3.6f * uiScale)
        }

        threats.forEach { threat ->
            if (!threat.isTracked) return@forEach
            tempProject.set(threat.position)
            camera.project(tempProject)
            if (tempProject.z in 0f..1f) {
                batch.color = Color(1f, 0.35f, 0.35f, 0.9f)
                batch.draw(whiteRegion, tempProject.x - 20f * uiScale, tempProject.y + 24f * uiScale, 34f * uiScale, 2f * uiScale)
                font.draw(
                    batch,
                    "${threat.id}  ALT ${(threat.position.y * 3.5f).toInt()} m",
                    tempProject.x + 20f * uiScale,
                    tempProject.y + 34f * uiScale,
                )
            }
        }

        batch.color = Color.WHITE
        batch.end()
    }

    private fun battleHealthSummary(): String {
        if (battleLiveTime <= 0f) return "LIVE 0s // 0 FPS // T0 I0"
        val averageFps = (renderedBattleFrames / battleLiveTime).toInt()
        return buildString {
            append("LIVE ${battleLiveTime.toInt()}s // $averageFps FPS")
            append(" // Q ${qualityProfile.label}")
            append(" // FX${effects.size}")
            append(" // T${threats.size} I${interceptors.size}")
        }
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
        updateHud()

        if (isGameOver) {
            isGameOver = true
            statusLabel.style = skin.get("critical", Label.LabelStyle::class.java)
            statusLabel.setText("DEFENSE FAILED")
            refreshWaveButton()
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

    private fun spawnThreat() {
        val launch = ThreatFactory.createThreatLaunch(wave, cityBlocks.random().position)
        val instance = ModelInstance(models.get("threat"))
        val threat =
            ThreatEntity(
                instance = instance,
                position = launch.start,
                targetPosition = launch.target,
                velocity = launch.velocity,
                id = "T-${MathUtils.random(1000, 9999)}",
                trailCooldown = MathUtils.random(0f, THREAT_TRAIL_INTERVAL),
            )
        syncProjectileTransform(instance, launch.start, launch.velocity, THREAT_SCALE)
        threats.add(threat)
    }

    private fun updateThreats(dt: Float) {
        val iterator = threats.iterator()
        while (iterator.hasNext()) {
            val threat = iterator.next()
            threat.velocity.mulAdd(gravity, dt)
            threat.position.mulAdd(threat.velocity, dt)
            syncProjectileTransform(threat.instance, threat.position, threat.velocity, THREAT_SCALE)

            threat.trailCooldown -= dt
            if (threat.trailCooldown <= 0f) {
                spawnTrail(threat.position, true)
                threat.trailCooldown = THREAT_TRAIL_INTERVAL
            }

            if (threat.position.dst2(Vector3.Zero) < settings.engagementRange * settings.engagementRange) {
                threat.isTracked = true
            }

            val reachedTarget =
                threat.position.dst2(threat.targetPosition) <= PhysicsModel.THREAT_IMPACT_RADIUS * PhysicsModel.THREAT_IMPACT_RADIUS
            val hitGround = threat.position.y <= 0f
            val leftBattlespace = threat.position.z >= PhysicsModel.THREAT_FAILSAFE_Z
            if ((reachedTarget && threat.position.y <= 90f) || hitGround || leftBattlespace) {
                val impactPoint =
                    when {
                        reachedTarget -> threat.targetPosition
                        hitGround -> Vector3(threat.position.x, 0f, threat.position.z)
                        else -> Vector3(threat.position.x, threat.position.y.coerceAtLeast(0f), threat.position.z)
                    }
                impactAt(impactPoint, 85f, true)
                iterator.remove()
            }
        }
    }

    private fun updateInterceptors(dt: Float) {
        val iterator = interceptors.iterator()
        while (iterator.hasNext()) {
            val interceptor = iterator.next()
            val target = interceptor.target
            if (target == null || !threats.contains(target, true)) {
                iterator.remove()
                continue
            }

            val aimPoint =
                InterceptionMath.predictInterceptPoint(
                    interceptor.position,
                    target.position,
                    target.velocity,
                    settings.interceptorSpeed,
                )
            tempA.set(aimPoint).sub(interceptor.position)
            if (!tempA.isZero(0.001f)) {
                tempA.nor().scl(settings.interceptorSpeed)
                interceptor.velocity.lerp(tempA, (dt * 4.2f).coerceAtMost(1f))
            }
            interceptor.velocity.nor().scl(settings.interceptorSpeed)
            interceptor.position.mulAdd(interceptor.velocity, dt)
            syncProjectileTransform(interceptor.instance, interceptor.position, interceptor.velocity, INTERCEPTOR_SCALE)

            interceptor.trailCooldown -= dt
            if (interceptor.trailCooldown <= 0f) {
                spawnTrail(interceptor.position, false)
                interceptor.trailCooldown = INTERCEPTOR_TRAIL_INTERVAL
            }

            if (EngagementPhysics.closesWithinFuse(
                    interceptorPos = interceptor.position,
                    interceptorVel = interceptor.velocity,
                    targetPos = target.position,
                    targetVel = target.velocity,
                    dt = dt,
                    fuseRadius = settings.blastRadius,
                )
            ) {
                score += 150
                credits += 180
                spawnBlast(interceptor.position, settings.blastRadius * 2.2f)
                spawnDebris(interceptor.position, 10, Color(0.7f, 0.7f, 0.74f, 1f))
                triggerShake(10f, 0.3f)
                playSfx("detonate", 0.5f)
                threats.removeValue(target, true)
                iterator.remove()
                continue
            }

            if (interceptor.position.y > 4400f || interceptor.position.dst2(Vector3.Zero) > 7000f * 7000f) {
                iterator.remove()
            }
        }

        timeSinceLastLaunch += dt
        if (timeSinceLastLaunch >= settings.launchCooldown) {
            val nextTarget = findNextLaunchTarget()
            if (nextTarget != null) {
                launchInterceptor(nextTarget)
                timeSinceLastLaunch = 0f
            }
        }
    }

    private fun findNextLaunchTarget(): ThreatEntity? {
        val engagementRangeSquared = settings.engagementRange * settings.engagementRange
        var bestThreat: ThreatEntity? = null
        threats.forEach { threat ->
            val inRange = threat.position.dst2(Vector3.Zero) < engagementRangeSquared
            if (inRange) {
                threat.isTracked = true
            }
            if (!inRange || interceptors.any { it.target == threat }) return@forEach

            val incumbent = bestThreat
            if (incumbent == null || isHigherPriorityThreat(threat, incumbent)) {
                bestThreat = threat
            }
        }
        return bestThreat
    }

    private fun isHigherPriorityThreat(
        candidate: ThreatEntity,
        incumbent: ThreatEntity,
    ): Boolean = FireControl.hasPriorityOver(candidate.position, incumbent.position)

    private fun launchInterceptor(target: ThreatEntity) {
        val launcher =
            launchers.minByOrNull { launcher ->
                launcher.transform.getTranslation(tempA)
                tempA.dst2(target.position)
            } ?: return

        launcher.transform.getTranslation(tempA)
        tempA.y += 18f
        val aimPoint = InterceptionMath.predictInterceptPoint(tempA, target.position, target.velocity, settings.interceptorSpeed)
        tempB
            .set(aimPoint)
            .sub(tempA)
            .nor()
            .scl(settings.interceptorSpeed)
        launcher.setRotationToward(tempB)

        val instance = ModelInstance(models.get("interceptor"))
        syncProjectileTransform(instance, tempA, tempB, INTERCEPTOR_SCALE)
        interceptors.add(
            InterceptorEntity(
                id = "LEGACY-I-${MathUtils.random(1000, 9999)}",
                instance = instance,
                position = tempA.cpy(),
                velocity = tempB.cpy(),
                target = target,
                trailCooldown = 0f,
            ),
        )
        spawnBlast(tempA, 14f)
        if (launchers.size > 1) {
            when (launchers.indexOf(launcher, true)) {
                0 -> launcherLeftPulse = 1f
                1 -> launcherRightPulse = 1f
            }
        }
        playSfx("launch", 0.35f)
    }

    private fun impactAt(
        position: Vector3,
        radius: Float,
        hostile: Boolean,
    ) {
        spawnBlast(position, radius * 1.2f)
        spawnDebris(position, if (hostile) 24 else 12, Color(0.4f, 0.38f, 0.36f, 1f))
        triggerShake(if (hostile) 26f else 12f, 0.55f)
        playSfx("impact", if (hostile) 0.8f else 0.45f)

        cityBlocks.forEach { building ->
            val damage =
                DamageModel.computeBuildingDamage(
                    buildingPosition = building.position,
                    buildingWidth = building.width,
                    buildingDepth = building.depth,
                    impactPosition = position,
                    blastRadius = radius,
                    hostile = hostile,
                )
            if (damage > 0f) {
                applyBuildingDamage(building, damage.coerceAtLeast(8f), position)
            }
        }

        if (hostile) {
            cityIntegrity = max(0f, cityIntegrity - DamageModel.cityIntegrityLoss(hostile = true, destroyedBuilding = false))
        }
    }

    private fun applyBuildingDamage(
        building: BuildingEntity,
        damage: Float,
        epicenter: Vector3,
    ) {
        if (building.integrity <= 0f) return
        building.integrity = max(0f, building.integrity - damage)
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

        if (building.integrity <= 0f) {
            building.visibleHeight = building.baseHeight * 0.12f
            building.collapseVelocity = max(building.collapseVelocity, 60f)
            spawnDebris(building.position.cpy().add(0f, building.baseHeight * 0.35f, 0f), 28, Color(0.25f, 0.25f, 0.28f, 1f))
            cityIntegrity = max(0f, cityIntegrity - DamageModel.cityIntegrityLoss(hostile = false, destroyedBuilding = true))
        } else {
            spawnDebris(building.position.cpy().add(0f, building.baseHeight * 0.45f, 0f), 6, Color(0.3f, 0.3f, 0.34f, 1f))
        }
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
            effect.life -= dt
            val progress = (effect.life / effect.initialLife).coerceIn(0f, 1f)
            val material = effect.instance.materials.first()
            val blend = material.get(BlendingAttribute.Type) as? BlendingAttribute
            when (effect.type) {
                EffectType.BLAST -> {
                    val scale = effect.maxScale * (1.25f - progress * progress)
                    effect.instance.transform
                        .setToScaling(scale, scale, scale)
                        .trn(effect.position)
                    blend?.opacity = (progress * progress).coerceIn(0f, 1f)
                    val intensity = effect.maxScale * progress * 20f
                    if (intensity > strongest) {
                        strongest = intensity
                        strongestPos.set(effect.position)
                    }
                }

                EffectType.SHOCKWAVE -> {
                    val scale = effect.maxScale * (1.18f - progress)
                    effect.instance.transform
                        .setToScaling(scale, scale * 0.08f, scale)
                        .trn(effect.position)
                    blend?.opacity = (progress * 0.72f).coerceIn(0f, 1f)
                }

                EffectType.SMOKE -> {
                    effect.position.mulAdd(effect.velocity, dt)
                    effect.velocity.y += dt * 10f
                    val scale = 0.6f + (1f - progress) * effect.maxScale
                    effect.instance.transform
                        .setToScaling(scale, scale, scale)
                        .trn(effect.position)
                    blend?.opacity = (0.26f * kotlin.math.sqrt(progress)).coerceIn(0f, 0.3f)
                }

                EffectType.SPARK -> {
                    effect.position.mulAdd(effect.velocity, dt)
                    effect.velocity.scl((1f - dt * 1.8f).coerceAtLeast(0.2f))
                    effect.velocity.y -= dt * 140f
                    val scale = 0.18f + progress * effect.maxScale * 0.12f
                    effect.instance.transform
                        .setToScaling(scale, scale, scale)
                        .trn(effect.position)
                    blend?.opacity = (0.72f * progress).coerceIn(0f, 0.85f)
                }

                EffectType.TRAIL -> {
                    effect.position.mulAdd(effect.velocity, dt)
                    val scale = 0.45f + (1f - progress) * effect.maxScale
                    effect.instance.transform
                        .setToScaling(scale, scale, scale)
                        .trn(effect.position)
                    blend?.opacity = 0.42f * progress
                }
            }
            if (effect.life <= 0f) iterator.remove()
        }

        if (strongest > 0f) {
            impactLight.set(Color(1f, 0.8f, 0.42f, 1f), strongestPos, strongest)
        } else {
            impactLight.intensity = max(0f, impactLight.intensity - dt * 260f)
        }
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

    private fun updateHud() {
        val waveState =
            when {
                isGameOver -> "STATUS LOST"
                waveInProgress -> "WAVE $wave LIVE // ${threats.size + threatsRemainingInWave} HOSTILES"
                else -> "WAVE $wave READY"
            }
        if (::rangeValueLabel.isInitialized) {
            rangeValueLabel.setText("${settings.engagementRange.toInt()} M")
        }
        if (::speedValueLabel.isInitialized) {
            speedValueLabel.setText("${settings.interceptorSpeed.toInt()} M/S")
        }
        creditsLabel.setText(
            "SCORE $score   CREDITS $credits   CITY ${(cityIntegrity.coerceAtLeast(0f)).toInt()}%   $waveState",
        )
    }

    private fun refreshWaveButton() {
        if (!::waveButton.isInitialized) return
        when {
            isGameOver -> {
                waveButton.setText("DEFENSE FAILED")
                waveButton.isDisabled = true
            }

            waveInProgress -> {
                waveButton.setText("WAVE $wave ACTIVE")
                waveButton.isDisabled = true
            }

            else -> {
                waveButton.setText("START NEXT WAVE")
                waveButton.isDisabled = false
            }
        }
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

data class SurfaceTextureSet(
    val diffuse: Texture,
    val roughness: Texture,
)

data class InitializationTask(
    val message: String,
    val action: () -> Unit,
)
