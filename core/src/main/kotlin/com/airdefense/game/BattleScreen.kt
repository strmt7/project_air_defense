package com.airdefense.game

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
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.BoxShapeBuilder
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.ConeShapeBuilder
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.CylinderShapeBuilder
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.SphereShapeBuilder
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
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

class BattleScreen(private val game: AirDefenseGame) : ScreenAdapter() {
    private val environment = Environment()
    private val impactLight = PointLight()
    private val modelBatch = ModelBatch(NightShaderProvider(impactLight))
    private val camera = PerspectiveCamera(55f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
    private val stage = Stage(ScreenViewport())
    private val settings = DefenseSettings()
    private val textures = Array<Texture>()
    private val skin = createSkin()
    private val whiteRegion by lazy { skin.get("white_region", com.badlogic.gdx.graphics.g2d.TextureRegion::class.java) }
    private var battleSkyTexture: Texture? = null

    private val models = ObjectMap<String, Model>()
    private val instances = Array<ModelInstance>()
    private val launchers = Array<ModelInstance>()
    private val cityBlocks = Array<BuildingEntity>()
    private val threats = Array<ThreatEntity>()
    private val interceptors = Array<InterceptorEntity>()
    private val effects = Array<VisualEffect>()
    private val debris = Array<DebrisEntity>()
    private val sounds = ObjectMap<String, Sound>()

    private val statusLabel = Label("AIR DEFENSE NETWORK ONLINE", skin, "status")
    private val creditsLabel = Label("", skin, "status")
    private lateinit var waveButton: TextButton

    private val gravity = Vector3(0f, -55f, 0f)
    private val cameraBase = Vector3(0f, 260f, 720f)
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
    private var radarScanAngle = 0f
    private var shakeTime = 0f
    private var shakeIntensity = 0f
    private var initializationStep = 0
    private var initialized = false
    private var loadingMessage = "Initializing battle systems..."

    private companion object {
        private const val WORLD_RADIUS = 9000f
        private const val THREAT_TRAIL_INTERVAL = 0.06f
        private const val INTERCEPTOR_TRAIL_INTERVAL = 0.025f
        private const val THREAT_SCALE = 12f
        private const val INTERCEPTOR_SCALE = 16f
    }

    init {
        stage.addListener(object : InputAdapter(), com.badlogic.gdx.scenes.scene2d.EventListener {
            override fun handle(event: com.badlogic.gdx.scenes.scene2d.Event?): Boolean {
                if (isGameOver && event is InputEvent && event.type == InputEvent.Type.touchDown) {
                    game.screen = StartScreen(game)
                    return true
                }
                return false
            }
        })
        Gdx.input.inputProcessor = stage
    }

    private fun createSkin(): Skin {
        val s = Skin()
        val uiScale = Gdx.graphics.height / 1080f
        val font = BitmapFont().apply { data.setScale(1.28f * uiScale) }
        val titleFont = BitmapFont().apply { data.setScale(2.02f * uiScale) }
        val microFont = BitmapFont().apply { data.setScale(1.02f * uiScale) }
        s.add("default", font, BitmapFont::class.java)
        s.add("title", titleFont, BitmapFont::class.java)
        s.add("micro", microFont, BitmapFont::class.java)

        val whitePix = Pixmap(2, 2, Pixmap.Format.RGBA8888)
        whitePix.setColor(Color.WHITE)
        whitePix.fill()
        val whiteTex = Texture(whitePix)
        textures.add(whiteTex)
        s.add("white", TextureRegionDrawable(com.badlogic.gdx.graphics.g2d.TextureRegion(whiteTex)), Drawable::class.java)
        s.add("white_region", com.badlogic.gdx.graphics.g2d.TextureRegion(whiteTex))

        fun addButton(name: String, top: Color, bottom: Color, stroke: Color, glow: Color) {
            val p = Pixmap(280, 100, Pixmap.Format.RGBA8888)
            for (y in 0 until p.height) {
                val t = y / (p.height - 1f)
                p.setColor(
                    MathUtils.lerp(top.r, bottom.r, t),
                    MathUtils.lerp(top.g, bottom.g, t),
                    MathUtils.lerp(top.b, bottom.b, t),
                    MathUtils.lerp(top.a, bottom.a, t)
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
            s.add(name, TextureRegionDrawable(com.badlogic.gdx.graphics.g2d.TextureRegion(tex)), Drawable::class.java)
            p.dispose()
        }

        addButton("btn_up", Color(0.03f, 0.08f, 0.16f, 0.96f), Color(0.01f, 0.14f, 0.25f, 0.98f), Color(0.28f, 0.82f, 1f, 1f), Color(0.24f, 0.62f, 0.82f, 0.2f))
        addButton("btn_over", Color(0.07f, 0.2f, 0.33f, 0.98f), Color(0.03f, 0.28f, 0.45f, 0.98f), Color(0.78f, 0.96f, 1f, 1f), Color(0.48f, 0.84f, 1f, 0.28f))
        addButton("btn_down", Color(0f, 0.34f, 0.52f, 0.98f), Color(0.0f, 0.2f, 0.34f, 1f), Color(0.86f, 0.98f, 1f, 1f), Color(0.66f, 0.94f, 1f, 0.2f))
        addButton("btn_disabled", Color(0.07f, 0.1f, 0.14f, 0.88f), Color(0.03f, 0.06f, 0.1f, 0.9f), Color(0.18f, 0.24f, 0.3f, 1f), Color(0f, 0f, 0f, 0f))

        s.add("default", TextButton.TextButtonStyle().apply {
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
        })
        s.add("default", Label.LabelStyle(font, Color.WHITE))
        s.add("status", Label.LabelStyle(microFont, Color(0.7f, 0.96f, 1f, 1f)))
        s.add("warning", Label.LabelStyle(font, Color(1f, 0.84f, 0.4f, 1f)))
        s.add("critical", Label.LabelStyle(font, Color(1f, 0.42f, 0.42f, 1f)))
        s.add("title", Label.LabelStyle(titleFont, Color.WHITE))

        val sliderBack = Pixmap(240, 20, Pixmap.Format.RGBA8888).apply {
            for (y in 0 until height) {
                val t = y / (height - 1f)
                setColor(
                    MathUtils.lerp(0.05f, 0.1f, t),
                    MathUtils.lerp(0.14f, 0.2f, t),
                    MathUtils.lerp(0.2f, 0.28f, t),
                    1f
                )
                drawLine(0, y, width - 1, y)
            }
            setColor(0.28f, 0.78f, 0.94f, 1f)
            drawRectangle(0, 0, width, height)
            drawRectangle(1, 1, width - 2, height - 2)
        }
        val sliderKnob = Pixmap(40, 48, Pixmap.Format.RGBA8888).apply {
            for (y in 0 until height) {
                val t = y / (height - 1f)
                setColor(
                    MathUtils.lerp(0.28f, 0.68f, t),
                    MathUtils.lerp(0.82f, 0.96f, t),
                    1f,
                    1f
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
        s.add("default-horizontal", Slider.SliderStyle(
            TextureRegionDrawable(com.badlogic.gdx.graphics.g2d.TextureRegion(sliderBackTex)),
            TextureRegionDrawable(com.badlogic.gdx.graphics.g2d.TextureRegion(sliderKnobTex))
        ))
        whitePix.dispose()
        sliderBack.dispose()
        sliderKnob.dispose()
        return s
    }

    private fun setupEnvironment() {
        environment.set(ColorAttribute(ColorAttribute.AmbientLight, 0.22f, 0.24f, 0.3f, 1f))
        environment.add(DirectionalLight().set(Color(0.38f, 0.44f, 0.55f, 1f), -0.4f, -1f, -0.18f))
        environment.add(DirectionalLight().set(Color(0.16f, 0.15f, 0.22f, 1f), 0.35f, -0.22f, 0.4f))
        impactLight.set(Color.BLACK, Vector3.Zero, 0f)
        environment.add(impactLight)
    }

    private fun setupCamera() {
        camera.near = 1f
        camera.far = WORLD_RADIUS * 1.5f
        camera.position.set(cameraBase)
        camera.lookAt(0f, 100f, -240f)
        camera.update()
    }

    private fun loadTexture(path: String, fallbackColor: Color): Texture {
        val file = Gdx.files.internal(path)
        if (file.exists()) {
            return registerTexture(Texture(file))
        }
        val pixmap = Pixmap(8, 8, Pixmap.Format.RGBA8888)
        pixmap.setColor(fallbackColor)
        pixmap.fill()
        return registerTexture(Texture(pixmap)).also { pixmap.dispose() }
    }

    private fun registerTexture(texture: Texture, repeat: Boolean = false): Texture {
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        if (repeat) texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat)
        textures.add(texture)
        return texture
    }

    private fun createTextureSet(diffuse: Pixmap, roughness: Pixmap, repeat: Boolean = true): SurfaceTextureSet {
        val diffuseTexture = registerTexture(Texture(diffuse), repeat)
        val roughnessTexture = registerTexture(Texture(roughness), repeat)
        diffuse.dispose()
        roughness.dispose()
        return SurfaceTextureSet(diffuseTexture, roughnessTexture)
    }

    private fun createSolidTextureSet(color: Color, roughnessValue: Float): SurfaceTextureSet {
        val diffuse = Pixmap(4, 4, Pixmap.Format.RGBA8888).apply { setColor(color); fill() }
        val roughness = Pixmap(4, 4, Pixmap.Format.RGBA8888).apply {
            setColor(roughnessValue, roughnessValue, roughnessValue, 1f)
            fill()
        }
        return createTextureSet(diffuse, roughness, repeat = false)
    }

    private fun createFacadeTextureSet(width: Int, height: Int, base: Color, lit: Color): SurfaceTextureSet {
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
                    1f
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
        val diffuse = Pixmap(512, 512, Pixmap.Format.RGBA8888)
        val roughness = Pixmap(512, 512, Pixmap.Format.RGBA8888)
        for (x in 0 until 512) {
            for (y in 0 until 512) {
                val noise = MathUtils.random(-0.03f, 0.03f)
                val striping = if ((x / 32 + y / 32) % 2 == 0) 0.02f else -0.01f
                diffuse.setColor(
                    (0.07f + noise + striping).coerceIn(0f, 1f),
                    (0.075f + noise).coerceIn(0f, 1f),
                    (0.08f + noise * 0.7f).coerceIn(0f, 1f),
                    1f
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
        val diffuse = Pixmap(512, 512, Pixmap.Format.RGBA8888)
        val roughness = Pixmap(512, 512, Pixmap.Format.RGBA8888)
        for (x in 0 until 512) {
            for (y in 0 until 512) {
                val wave = kotlin.math.sin((x + y) * 0.045f) * 0.03f
                val shimmer = kotlin.math.cos(y * 0.08f) * 0.02f
                val noise = MathUtils.random(-0.018f, 0.018f)
                diffuse.setColor(
                    (0.02f + wave + noise).coerceIn(0f, 1f),
                    (0.12f + shimmer + noise).coerceIn(0f, 1f),
                    (0.22f + wave + shimmer + noise * 1.2f).coerceIn(0f, 1f),
                    1f
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
        val diffuse = Pixmap(512, 512, Pixmap.Format.RGBA8888)
        val roughness = Pixmap(512, 512, Pixmap.Format.RGBA8888)
        for (x in 0 until 512) {
            for (y in 0 until 512) {
                val dune = kotlin.math.sin(x * 0.03f) * 0.04f + kotlin.math.cos(y * 0.024f) * 0.025f
                val noise = MathUtils.random(-0.03f, 0.03f)
                diffuse.setColor(
                    (0.58f + dune + noise).coerceIn(0f, 1f),
                    (0.5f + dune * 0.7f + noise).coerceIn(0f, 1f),
                    (0.34f + noise * 0.6f).coerceIn(0f, 1f),
                    1f
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
        val diffuse = Pixmap(512, 512, Pixmap.Format.RGBA8888)
        val roughness = Pixmap(512, 512, Pixmap.Format.RGBA8888)
        for (x in 0 until 512) {
            for (y in 0 until 512) {
                val strip = if ((x / 28 + y / 36) % 2 == 0) 0.03f else -0.02f
                val noise = MathUtils.random(-0.035f, 0.035f)
                diffuse.setColor(
                    (0.06f + strip + noise).coerceIn(0f, 1f),
                    (0.18f + strip + noise).coerceIn(0f, 1f),
                    (0.08f + noise).coerceIn(0f, 1f),
                    1f
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
        val diffuse = Pixmap(512, 512, Pixmap.Format.RGBA8888)
        val roughness = Pixmap(512, 512, Pixmap.Format.RGBA8888)
        for (x in 0 until 512) {
            for (y in 0 until 512) {
                val tile = if ((x / 48 + y / 48) % 2 == 0) 0.04f else -0.015f
                val noise = MathUtils.random(-0.02f, 0.02f)
                diffuse.setColor(
                    (0.22f + tile + noise).coerceIn(0f, 1f),
                    (0.2f + tile + noise).coerceIn(0f, 1f),
                    (0.18f + tile * 0.7f + noise).coerceIn(0f, 1f),
                    1f
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
        val pixmap = Pixmap(1024, 512, Pixmap.Format.RGBA8888)
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
                    1f
                )
                pixmap.drawPixel(x, y)
            }
        }

        repeat(320) {
            val x = MathUtils.random(0, pixmap.width - 1)
            val y = MathUtils.random((pixmap.height * 0.35f).toInt(), pixmap.height - 1)
            val brightness = MathUtils.random(0.55f, 1f)
            pixmap.setColor(brightness, brightness, brightness, MathUtils.random(0.45f, 0.95f))
            pixmap.fillCircle(x, y, MathUtils.random(1, 2))
        }

        val texture = registerTexture(Texture(pixmap))
        pixmap.dispose()
        return texture
    }

    private fun createRoadTextureSet(): SurfaceTextureSet {
        val diffuse = Pixmap(512, 512, Pixmap.Format.RGBA8888)
        val roughness = Pixmap(512, 512, Pixmap.Format.RGBA8888)
        for (x in 0 until 512) {
            for (y in 0 until 512) {
                val noise = MathUtils.random(-0.025f, 0.025f)
                diffuse.setColor(
                    (0.09f + noise).coerceIn(0f, 1f),
                    (0.09f + noise).coerceIn(0f, 1f),
                    (0.1f + noise).coerceIn(0f, 1f),
                    1f
                )
                diffuse.drawPixel(x, y)
                val r = (0.74f + MathUtils.random(-0.08f, 0.06f)).coerceIn(0.1f, 1f)
                roughness.setColor(r, r, r, 1f)
                roughness.drawPixel(x, y)
            }
        }
        for (x in 246..266) {
            diffuse.setColor(0.65f, 0.6f, 0.36f, 1f)
            diffuse.fillRectangle(x, 0, 2, 512)
            roughness.setColor(0.42f, 0.42f, 0.42f, 1f)
            roughness.fillRectangle(x, 0, 2, 512)
        }
        return createTextureSet(diffuse, roughness)
    }

    private fun createMetalTextureSet(base: Color): SurfaceTextureSet {
        val diffuse = Pixmap(256, 256, Pixmap.Format.RGBA8888)
        val roughness = Pixmap(256, 256, Pixmap.Format.RGBA8888)
        for (x in 0 until 256) {
            for (y in 0 until 256) {
                val streak = ((x % 24) / 24f) * 0.05f
                val noise = MathUtils.random(-0.025f, 0.025f)
                diffuse.setColor(
                    (base.r + streak + noise).coerceIn(0f, 1f),
                    (base.g + streak + noise).coerceIn(0f, 1f),
                    (base.b + streak + noise).coerceIn(0f, 1f),
                    1f
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
        val diffuse = Pixmap(256, 256, Pixmap.Format.RGBA8888)
        val roughness = Pixmap(256, 256, Pixmap.Format.RGBA8888)
        for (x in 0 until 256) {
            for (y in 0 until 256) {
                val noise = MathUtils.random(-0.05f, 0.05f)
                diffuse.setColor(
                    (base.r + noise).coerceIn(0f, 1f),
                    (base.g + noise).coerceIn(0f, 1f),
                    (base.b + noise).coerceIn(0f, 1f),
                    1f
                )
                diffuse.drawPixel(x, y)
                val r = (0.88f + MathUtils.random(-0.06f, 0.04f)).coerceIn(0.1f, 1f)
                roughness.setColor(r, r, r, 1f)
                roughness.drawPixel(x, y)
            }
        }
        return createTextureSet(diffuse, roughness)
    }

    private fun generateWorldModels() {
        val attr = (
            VertexAttributes.Usage.Position or
                VertexAttributes.Usage.Normal or
                VertexAttributes.Usage.TextureCoordinates
            ).toLong()
        val mb = ModelBuilder()
        battleSkyTexture = createSkyTexture()
        val facadeA = createFacadeTextureSet(256, 512, Color(0.08f, 0.1f, 0.14f, 1f), Color(1f, 0.82f, 0.48f, 1f))
        val facadeB = createFacadeTextureSet(256, 512, Color(0.05f, 0.07f, 0.11f, 1f), Color(0.7f, 0.88f, 1f, 1f))
        val facadeC = createFacadeTextureSet(256, 512, Color(0.1f, 0.08f, 0.09f, 1f), Color(1f, 0.62f, 0.3f, 1f))
        val facadeD = createFacadeTextureSet(256, 512, Color(0.07f, 0.09f, 0.12f, 1f), Color(0.56f, 0.96f, 0.96f, 1f))
        val facadeE = createFacadeTextureSet(256, 512, Color(0.12f, 0.1f, 0.08f, 1f), Color(1f, 0.9f, 0.62f, 1f))
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
        val inlandGround = mb.part(
            "inland_ground",
            GL20.GL_TRIANGLES,
            attr,
            Material(
                TextureAttribute.createDiffuse(groundSet.diffuse),
                TextureAttribute.createSpecular(groundSet.roughness),
                ColorAttribute.createDiffuse(Color(0.95f, 0.98f, 1f, 1f)),
                ColorAttribute.createSpecular(Color(0.18f, 0.2f, 0.24f, 1f)),
                FloatAttribute.createShininess(12f)
            )
        )
        BoxShapeBuilder.build(inlandGround, 7600f, 3f, 11000f, 1350f, -2f, -300f)
        val sea = mb.part(
            "sea",
            GL20.GL_TRIANGLES,
            attr,
            Material(
                TextureAttribute.createDiffuse(seaSet.diffuse),
                TextureAttribute.createSpecular(seaSet.roughness),
                ColorAttribute.createDiffuse(Color.WHITE),
                ColorAttribute.createSpecular(Color(0.45f, 0.62f, 0.78f, 1f)),
                FloatAttribute.createShininess(46f)
            )
        )
        BoxShapeBuilder.build(sea, 4200f, 2f, 11000f, -3550f, -3f, -300f)
        val beach = mb.part(
            "beach",
            GL20.GL_TRIANGLES,
            attr,
            Material(
                TextureAttribute.createDiffuse(beachSet.diffuse),
                TextureAttribute.createSpecular(beachSet.roughness),
                ColorAttribute.createDiffuse(Color.WHITE),
                ColorAttribute.createSpecular(Color(0.2f, 0.18f, 0.12f, 1f)),
                FloatAttribute.createShininess(8f)
            )
        )
        BoxShapeBuilder.build(beach, 900f, 4f, 9800f, -2150f, -1f, -340f)
        val promenade = mb.part(
            "promenade",
            GL20.GL_TRIANGLES,
            attr,
            Material(
                TextureAttribute.createDiffuse(promenadeSet.diffuse),
                TextureAttribute.createSpecular(promenadeSet.roughness),
                ColorAttribute.createDiffuse(Color.WHITE),
                ColorAttribute.createSpecular(Color(0.22f, 0.22f, 0.24f, 1f)),
                FloatAttribute.createShininess(18f)
            )
        )
        BoxShapeBuilder.build(promenade, 260f, 2f, 9300f, -1500f, 0f, -360f)
        val park = mb.part(
            "park",
            GL20.GL_TRIANGLES,
            attr,
            Material(
                TextureAttribute.createDiffuse(parkSet.diffuse),
                TextureAttribute.createSpecular(parkSet.roughness),
                ColorAttribute.createDiffuse(Color.WHITE),
                ColorAttribute.createSpecular(Color(0.12f, 0.18f, 0.1f, 1f)),
                FloatAttribute.createShininess(6f)
            )
        )
        BoxShapeBuilder.build(park, 920f, 2f, 4200f, 980f, -1f, -1120f)
        val road = mb.part(
            "road",
            GL20.GL_TRIANGLES,
            attr,
            Material(
                TextureAttribute.createDiffuse(roadSet.diffuse),
                TextureAttribute.createSpecular(roadSet.roughness),
                ColorAttribute.createDiffuse(Color.WHITE),
                ColorAttribute.createSpecular(Color(0.28f, 0.28f, 0.3f, 1f)),
                FloatAttribute.createShininess(18f)
            )
        )
        BoxShapeBuilder.build(road, 7200f, 1f, 42f, 600f, 0f, 210f)
        BoxShapeBuilder.build(road, 5600f, 1f, 38f, 220f, 0f, -1160f)
        BoxShapeBuilder.build(road, 40f, 1f, 8800f, -1160f, 0f, -280f)
        BoxShapeBuilder.build(road, 44f, 1f, 7600f, 840f, 0f, -480f)
        BoxShapeBuilder.build(road, 38f, 1f, 6000f, 1780f, 0f, -580f)
        val glowBand = mb.part(
            "glow",
            GL20.GL_TRIANGLES,
            attr,
            Material(
                ColorAttribute.createDiffuse(Color(0.15f, 0.22f, 0.35f, 1f)),
                BlendingAttribute(0.18f)
            )
        )
        BoxShapeBuilder.build(glowBand, 7200f, 0.5f, 1100f, 760f, 6f, -420f)
        BoxShapeBuilder.build(glowBand, 1800f, 0.5f, 8600f, -1450f, 4f, -320f)
        models.put("ground", mb.end())

        fun createBuildingModel(name: String, width: Float, height: Float, depth: Float, texture: SurfaceTextureSet, tint: Color) {
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
                        FloatAttribute.createShininess(8f)
                    ),
                    attr
                )
            )
        }

        createBuildingModel("tower_a", 58f, 280f, 58f, facadeA, Color(0.9f, 0.95f, 1f, 1f))
        createBuildingModel("tower_b", 84f, 210f, 84f, facadeB, Color(0.82f, 0.9f, 1f, 1f))
        createBuildingModel("tower_c", 120f, 130f, 90f, facadeC, Color(1f, 0.95f, 0.9f, 1f))
        createBuildingModel("tower_d", 96f, 360f, 74f, facadeD, Color(0.84f, 0.98f, 1f, 1f))
        createBuildingModel("tower_e", 146f, 178f, 112f, facadeE, Color(1f, 0.94f, 0.86f, 1f))
        createBuildingModel("podium", 180f, 78f, 120f, facadeB, Color(0.9f, 0.94f, 1f, 1f))
        createBuildingModel("hotel", 132f, 118f, 72f, facadeC, Color(1f, 0.96f, 0.9f, 1f))

        val launcherMaterial = Material(
            TextureAttribute.createDiffuse(launcherSet.diffuse),
            TextureAttribute.createSpecular(launcherSet.roughness),
            ColorAttribute.createDiffuse(Color.WHITE),
            ColorAttribute.createSpecular(Color(0.5f, 0.56f, 0.52f, 1f)),
            FloatAttribute.createShininess(40f)
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
        val radarMaterial = Material(
            TextureAttribute.createDiffuse(radarSet.diffuse),
            TextureAttribute.createSpecular(radarSet.roughness),
            ColorAttribute.createDiffuse(Color.WHITE),
            ColorAttribute.createSpecular(Color(0.55f, 0.64f, 0.6f, 1f)),
            FloatAttribute.createShininess(44f)
        )
        mb.part("base", GL20.GL_TRIANGLES, attr, radarMaterial).apply {
            BoxShapeBuilder.build(this, 42f, 8f, 42f)
        }
        mb.part("face", GL20.GL_TRIANGLES, attr, radarMaterial).apply {
            BoxShapeBuilder.build(this, 54f, 34f, 6f, 0f, 28f, -8f)
        }
        models.put("radar", mb.end())

        mb.begin()
        val threatMaterial = Material(
            TextureAttribute.createDiffuse(threatSet.diffuse),
            TextureAttribute.createSpecular(threatSet.roughness),
            ColorAttribute.createDiffuse(Color(1f, 0.78f, 0.42f, 1f)),
            ColorAttribute.createSpecular(Color(1f, 0.88f, 0.66f, 1f)),
            ColorAttribute.createEmissive(Color(0.18f, 0.08f, 0.03f, 1f)),
            FloatAttribute.createShininess(78f)
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
        val interceptorMaterial = Material(
            TextureAttribute.createDiffuse(interceptorSet.diffuse),
            TextureAttribute.createSpecular(interceptorSet.roughness),
            ColorAttribute.createDiffuse(Color(0.82f, 0.96f, 1f, 1f)),
            ColorAttribute.createSpecular(Color(0.94f, 0.98f, 1f, 1f)),
            ColorAttribute.createEmissive(Color(0.05f, 0.12f, 0.18f, 1f)),
            FloatAttribute.createShininess(92f)
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
                    BlendingAttribute(0.92f)
                ),
                attr
            )
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
                    BlendingAttribute(0.55f)
                ),
                attr
            )
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
                    FloatAttribute.createShininess(10f)
                ),
                attr
            )
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
                    ColorAttribute.createEmissive(Color(0.18f, 0.2f, 0.24f, 1f))
                ),
                attr
            )
        )
    }

    private fun createWorldInstances() {
        instances.add(ModelInstance(models.get("ground")).apply { transform.setToTranslation(0f, -2f, 0f) })
        instances.add(ModelInstance(models.get("moon")).apply { transform.setToTranslation(1400f, 1450f, -4200f) })

        val leftLauncher = ModelInstance(models.get("launcher")).apply { transform.setToTranslation(-80f, 4f, 260f) }
        val rightLauncher = ModelInstance(models.get("launcher")).apply { transform.setToTranslation(140f, 4f, 220f) }
        launchers.add(leftLauncher)
        launchers.add(rightLauncher)
        instances.add(leftLauncher)
        instances.add(rightLauncher)
        instances.add(ModelInstance(models.get("radar")).apply { transform.setToTranslation(20f, 4f, 120f) })

        fun addBuilding(modelName: String, x: Float, z: Float, yaw: Float = 0f) {
            val (baseHeight, width, depth) = when (modelName) {
                "tower_a" -> Triple(280f, 58f, 58f)
                "tower_b" -> Triple(210f, 84f, 84f)
                "tower_c" -> Triple(130f, 120f, 90f)
                "tower_d" -> Triple(360f, 96f, 74f)
                "tower_e" -> Triple(178f, 146f, 112f)
                "podium" -> Triple(78f, 180f, 120f)
                "hotel" -> Triple(118f, 132f, 72f)
                else -> Triple(140f, 100f, 80f)
            }
            val entity = BuildingEntity(
                instance = ModelInstance(models.get(modelName)),
                modelName = modelName,
                position = Vector3(x, 0f, z),
                yaw = yaw,
                baseHeight = baseHeight,
                width = width,
                depth = depth,
                integrity = 100f
            )
            syncBuildingTransform(entity)
            cityBlocks.add(entity)
        }

        val waterfrontStrip = listOf(
            Triple("hotel", -860f, -2820f),
            Triple("hotel", -820f, -2220f),
            Triple("podium", -760f, -1540f),
            Triple("hotel", -790f, -860f),
            Triple("hotel", -840f, -180f),
            Triple("podium", -760f, 620f)
        )
        waterfrontStrip.forEachIndexed { index, (model, x, z) ->
            addBuilding(model, x, z, yaw = if (index % 2 == 0) 6f else -6f)
        }

        val skylineCore = listOf(
            Triple("tower_d", -180f, -2560f),
            Triple("tower_a", 220f, -2480f),
            Triple("tower_b", 520f, -2380f),
            Triple("tower_d", 840f, -2220f),
            Triple("tower_e", 1120f, -2360f),
            Triple("tower_b", 1480f, -2140f),
            Triple("tower_a", 1760f, -2400f),
            Triple("tower_d", 2140f, -2260f),
            Triple("tower_b", 2420f, -1980f),
            Triple("tower_e", 2760f, -2140f)
        )
        skylineCore.forEachIndexed { index, (model, x, z) ->
            addBuilding(model, x, z, yaw = if (index % 3 == 0) 12f else -8f)
        }

        val innerCity = listOf(
            Triple("tower_c", -120f, -1660f),
            Triple("tower_b", 260f, -1500f),
            Triple("tower_c", 620f, -1460f),
            Triple("tower_e", 980f, -1380f),
            Triple("tower_b", 1380f, -1280f),
            Triple("podium", 1820f, -1420f),
            Triple("tower_c", 2220f, -1260f),
            Triple("tower_b", 2600f, -1180f),
            Triple("tower_c", 2960f, -1260f),
            Triple("tower_b", 340f, -760f),
            Triple("tower_c", 760f, -700f),
            Triple("tower_b", 1190f, -640f),
            Triple("tower_c", 1650f, -680f),
            Triple("tower_e", 2120f, -720f),
            Triple("tower_b", 2520f, -620f),
            Triple("tower_c", 2940f, -740f)
        )
        innerCity.forEachIndexed { index, (model, x, z) ->
            addBuilding(model, x, z, yaw = if (index % 2 == 0) -10f else 10f)
        }

        val inlandDistrict = listOf(
            Triple("tower_c", 420f, 180f),
            Triple("tower_b", 880f, 140f),
            Triple("podium", 1320f, 180f),
            Triple("tower_c", 1780f, 60f),
            Triple("tower_b", 2260f, 80f),
            Triple("tower_c", 2720f, 40f),
            Triple("tower_e", 3200f, -40f),
            Triple("tower_c", 980f, 640f),
            Triple("tower_b", 1480f, 620f),
            Triple("tower_c", 1980f, 580f),
            Triple("tower_b", 2480f, 520f)
        )
        inlandDistrict.forEachIndexed { index, (model, x, z) ->
            addBuilding(model, x, z, yaw = if (index % 2 == 0) 7f else -9f)
        }
    }

    private fun setupHud() {
        stage.clear()
        val uiScale = Gdx.graphics.height / 1080f
        val root = Table().apply { setFillParent(true) }

        val top = Table().apply { background = this@BattleScreen.skin.newDrawable("white", Color(0f, 0.04f, 0.08f, 0.82f)) }
        top.add(statusLabel).expandX().left().pad(20f * uiScale)
        top.add(creditsLabel).right().pad(20f * uiScale)
        root.add(top).expandX().fillX().top().row()

        val side = Table().apply {
            background = this@BattleScreen.skin.newDrawable("white", Color(0.02f, 0.06f, 0.1f, 0.82f))
            defaults().pad(14f * uiScale).width(420f * uiScale)
        }
        side.add(Label("ENGAGEMENT CONTROL", skin, "title")).padTop(10f * uiScale).padBottom(30f * uiScale).row()
        side.add(Label("AUTO ENGAGEMENT RANGE", skin)).left().padTop(4f * uiScale).row()
        side.add(Slider(400f, 2600f, 25f, false, skin).apply {
            value = settings.engagementRange
            addListener(object : ChangeListener() {
                override fun changed(event: ChangeEvent?, actor: Actor?) {
                    settings.engagementRange = value
                }
            })
        }).fillX().height(58f * uiScale).row()
        side.add(Label("INTERCEPTOR SPEED", skin)).left().padTop(6f * uiScale).row()
        side.add(Slider(280f, 620f, 10f, false, skin).apply {
            value = settings.interceptorSpeed
            addListener(object : ChangeListener() {
                override fun changed(event: ChangeEvent?, actor: Actor?) {
                    settings.interceptorSpeed = value
                }
            })
        }).fillX().height(58f * uiScale).row()
        waveButton = TextButton("START NEXT WAVE", skin).apply {
            addListener(object : ChangeListener() {
                override fun changed(event: ChangeEvent?, actor: Actor?) {
                    if (!waveInProgress && !isGameOver) startNewWave()
                }
            })
        }
        side.add(waveButton).height(116f * uiScale).fillX().padTop(22f * uiScale).padBottom(8f * uiScale).row()
        root.add(side).expand().right().pad(22f * uiScale)

        stage.addActor(root)
        updateHud()
        refreshWaveButton()
    }

    private fun loadAudio() {
        fun loadSound(key: String, path: String) {
            val file = Gdx.files.internal(path)
            if (file.exists()) {
                sounds.put(key, Gdx.audio.newSound(file))
            }
        }
        loadSound("launch", "sfx/launch.mp3")
        loadSound("detonate", "sfx/detonate.mp3")
        loadSound("impact", "sfx/impact.mp3")
    }

    private fun playSfx(name: String, volume: Float) {
        sounds[name]?.play(volume)
    }

    private fun startNewWave() {
        waveInProgress = true
        threatsRemainingInWave = BattleBalance.threatsForWave(wave)
        spawnTimer = 0.4f
        timeSinceLastLaunch = settings.launchCooldown
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

        ScreenUtils.clear(0.01f, 0.02f, 0.04f, 1f, true)
        battleSkyTexture?.let { sky ->
            val batch = stage.batch
            stage.viewport.apply()
            batch.projectionMatrix = stage.camera.combined
            batch.begin()
            batch.color = Color.WHITE
            batch.draw(sky, 0f, 0f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
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

        stage.act(delta)
        stage.draw()
        renderOverlay()
    }

    private fun runInitializationStep() {
        when (initializationStep) {
            0 -> {
                loadingMessage = "Bringing command network online..."
                setupEnvironment()
                setupCamera()
            }
            1 -> {
                loadingMessage = "Generating city geometry..."
                generateWorldModels()
            }
            2 -> {
                loadingMessage = "Deploying launchers and radar..."
                createWorldInstances()
            }
            3 -> {
                loadingMessage = "Calibrating engagement controls..."
                setupHud()
                loadAudio()
                startNewWave()
                initialized = true
            }
        }
        initializationStep++
    }

    private fun renderLoading() {
        ScreenUtils.clear(0.01f, 0.02f, 0.04f, 1f, true)
        val batch = stage.batch
        val font = skin.getFont("default")
        val titleFont = skin.getFont("title")
        val titleLayout = GlyphLayout(titleFont, "INITIALIZING BATTLESPACE")
        val statusLayout = GlyphLayout(font, loadingMessage)
        val progress = (initializationStep.coerceAtMost(4)) / 4f
        val barWidth = Gdx.graphics.width * 0.34f
        val barHeight = 16f
        val barX = (Gdx.graphics.width - barWidth) * 0.5f
        val barY = Gdx.graphics.height * 0.42f

        batch.begin()
        batch.color = Color.WHITE
        titleFont.draw(batch, titleLayout, (Gdx.graphics.width - titleLayout.width) * 0.5f, Gdx.graphics.height * 0.58f)
        font.draw(batch, statusLayout, (Gdx.graphics.width - statusLayout.width) * 0.5f, Gdx.graphics.height * 0.5f)
        batch.color = Color(0.04f, 0.1f, 0.16f, 0.95f)
        batch.draw(whiteRegion, barX, barY, barWidth, barHeight)
        batch.color = Color(0.2f, 0.84f, 1f, 1f)
        batch.draw(whiteRegion, barX, barY, barWidth * progress, barHeight)
        batch.color = Color.WHITE
        batch.end()
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

        val cx = Gdx.graphics.width * 0.5f
        val cy = Gdx.graphics.height * 0.5f
        val cross = 44f * uiScale
        batch.color = Color(0.2f, 0.92f, 1f, 0.28f)
        batch.draw(whiteRegion, cx - cross, cy, cross * 0.45f, 2f)
        batch.draw(whiteRegion, cx + cross * 0.55f, cy, cross * 0.45f, 2f)
        batch.draw(whiteRegion, cx, cy - cross, 2f, cross * 0.45f)
        batch.draw(whiteRegion, cx, cy + cross * 0.55f, 2f, cross * 0.45f)

        val radarSize = 220f * uiScale
        val radarX = Gdx.graphics.width - radarSize - 26f * uiScale
        val radarY = 24f * uiScale
        batch.color = Color(0.02f, 0.07f, 0.12f, 0.72f)
        batch.draw(whiteRegion, radarX, radarY, radarSize, radarSize)
        batch.color = Color(0.14f, 0.8f, 1f, 0.3f)
        val originX = radarX + radarSize * 0.5f
        val originY = radarY + radarSize * 0.5f
        val sweepX = originX + MathUtils.cosDeg(radarScanAngle) * radarSize * 0.48f
        val sweepY = originY + MathUtils.sinDeg(radarScanAngle) * radarSize * 0.48f
        batch.draw(whiteRegion, originX, originY, sweepX - originX, sweepY - originY)

        threats.forEach { threat ->
            val tx = radarX + radarSize * 0.5f + threat.position.x / 4500f * radarSize * 0.5f
            val tz = radarY + radarSize * 0.5f + threat.position.z / 4500f * radarSize * 0.5f
            if (tx in radarX..(radarX + radarSize) && tz in radarY..(radarY + radarSize)) {
                batch.color = if (threat.isTracked) Color.RED else Color.YELLOW
                batch.draw(whiteRegion, tx - 2f, tz - 2f, 4f, 4f)
            }
        }

        threats.forEach { threat ->
            if (!threat.isTracked) return@forEach
            tempProject.set(threat.position)
            camera.project(tempProject)
            if (tempProject.z in 0f..1f) {
                batch.color = Color(1f, 0.35f, 0.35f, 0.9f)
                batch.draw(whiteRegion, tempProject.x - 24f, tempProject.y + 30f, 48f, 2f)
                font.draw(batch, "${threat.id}  ALT ${(threat.position.y * 3.5f).toInt()} m", tempProject.x + 28f, tempProject.y + 42f)
            }
        }

        batch.color = Color.WHITE
        batch.end()
    }

    private fun updateLogic(dt: Float) {
        radarScanAngle = (radarScanAngle + dt * 110f) % 360f
        updateCameraShake(dt)

        if (waveInProgress) {
            spawnTimer -= dt
            if (spawnTimer <= 0f && threatsRemainingInWave > 0) {
                spawnThreat()
                threatsRemainingInWave--
                spawnTimer = BattleBalance.spawnIntervalForWave(wave)
            }
            if (threatsRemainingInWave == 0 && threats.isEmpty) {
                waveInProgress = false
                wave++
                credits += 1800
                statusLabel.style = skin.get("status", Label.LabelStyle::class.java)
                statusLabel.setText("SKY CLEAR // PREPARE NEXT WAVE")
                refreshWaveButton()
            }
        }

        updateThreats(dt)
        updateInterceptors(dt)
        updateEffects(dt)
        updateDebris(dt)
        updateBuildings(dt)
        updateHud()

        if (cityIntegrity <= 0f) {
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
                MathUtils.random(-amount, amount)
            )
        } else {
            camera.position.set(cameraBase)
        }
        camera.lookAt(0f, 110f, -260f)
        camera.update()
    }

    private fun spawnThreat() {
        val launch = ThreatFactory.createThreatLaunch(wave, cityBlocks.random().position)
        val instance = ModelInstance(models.get("threat"))
        val threat = ThreatEntity(
            instance = instance,
            position = launch.start,
            velocity = launch.velocity,
            id = "T-${MathUtils.random(1000, 9999)}",
            trailCooldown = MathUtils.random(0f, THREAT_TRAIL_INTERVAL)
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

            if (threat.position.y <= 12f || threat.position.z >= 420f) {
                impactAt(threat.position, 85f, true)
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

            val aimPoint = InterceptionMath.predictInterceptPoint(interceptor.position, target.position, target.velocity, settings.interceptorSpeed)
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

            if (interceptor.position.dst(target.position) <= settings.blastRadius) {
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

    private fun isHigherPriorityThreat(candidate: ThreatEntity, incumbent: ThreatEntity): Boolean {
        return FireControl.hasPriorityOver(candidate.position, incumbent.position)
    }

    private fun launchInterceptor(target: ThreatEntity) {
        val launcher = launchers.minByOrNull { launcher ->
            launcher.transform.getTranslation(tempA)
            tempA.dst2(target.position)
        } ?: return

        launcher.transform.getTranslation(tempA)
        tempA.y += 18f
        val aimPoint = InterceptionMath.predictInterceptPoint(tempA, target.position, target.velocity, settings.interceptorSpeed)
        tempB.set(aimPoint).sub(tempA).nor().scl(settings.interceptorSpeed)
        launcher.setRotationToward(tempB)

        val instance = ModelInstance(models.get("interceptor"))
        syncProjectileTransform(instance, tempA, tempB, INTERCEPTOR_SCALE)
        interceptors.add(
            InterceptorEntity(
                instance = instance,
                position = tempA.cpy(),
                velocity = tempB.cpy(),
                target = target,
                trailCooldown = 0f
            )
        )
        spawnBlast(tempA, 14f)
        playSfx("launch", 0.35f)
    }

    private fun impactAt(position: Vector3, radius: Float, hostile: Boolean) {
        spawnBlast(position, radius * 1.2f)
        spawnDebris(position, if (hostile) 24 else 12, Color(0.4f, 0.38f, 0.36f, 1f))
        triggerShake(if (hostile) 26f else 12f, 0.55f)
        playSfx("impact", if (hostile) 0.8f else 0.45f)

        cityBlocks.forEach { building ->
            val damage = DamageModel.computeBuildingDamage(
                buildingPosition = building.position,
                buildingWidth = building.width,
                buildingDepth = building.depth,
                impactPosition = position,
                blastRadius = radius,
                hostile = hostile
            )
            if (damage > 0f) {
                applyBuildingDamage(building, damage.coerceAtLeast(8f), position)
            }
        }

        if (hostile) {
            cityIntegrity = max(0f, cityIntegrity - DamageModel.cityIntegrityLoss(hostile = true, destroyedBuilding = false))
        }
    }

    private fun applyBuildingDamage(building: BuildingEntity, damage: Float, epicenter: Vector3) {
        if (building.integrity <= 0f) return
        building.integrity = max(0f, building.integrity - damage)
        building.collapseVelocity += damage * 0.0025f
        building.leanTarget = MathUtils.clamp(
            building.leanTarget + (building.position.x - epicenter.x).sign() * damage * 0.05f,
            -16f,
            16f
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

    private fun spawnBlast(position: Vector3, size: Float) {
        val instance = ModelInstance(models.get("blast"))
        instance.transform.setToScaling(0.1f, 0.1f, 0.1f).trn(position)
        effects.add(VisualEffect(instance, position.cpy(), 0.75f, 0.75f, EffectType.BLAST, size))
        impactLight.set(Color(1f, 0.82f, 0.5f, 1f), position, size * 16f)
    }

    private fun spawnTrail(position: Vector3, hostile: Boolean) {
        val instance = ModelInstance(models.get("trail"))
        val initialScale = if (hostile) 1.35f else 1.05f
        instance.transform.setToScaling(initialScale, initialScale, initialScale).trn(position)
        val effect = VisualEffect(
            instance,
            position.cpy(),
            if (hostile) 0.95f else 0.72f,
            if (hostile) 0.95f else 0.72f,
            EffectType.TRAIL,
            if (hostile) 5.2f else 4.1f
        )
        val material = effect.instance.materials.first()
        material.set(
            ColorAttribute.createDiffuse(
                if (hostile) Color(1f, 0.42f, 0.08f, 1f) else Color(0.46f, 0.9f, 1f, 1f)
            )
        )
        material.set(
            ColorAttribute.createEmissive(
                if (hostile) Color(0.45f, 0.12f, 0.02f, 1f) else Color(0.08f, 0.28f, 0.36f, 1f)
            )
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
            if (effect.type == EffectType.BLAST) {
                val scale = effect.maxScale * (1.2f - progress * progress)
                effect.instance.transform.setToScaling(scale, scale, scale).trn(effect.position)
                blend?.opacity = (progress * progress).coerceIn(0f, 1f)
                val intensity = effect.maxScale * progress * 18f
                if (intensity > strongest) {
                    strongest = intensity
                    strongestPos.set(effect.position)
                }
            } else {
                val scale = 1.2f + (1f - progress) * effect.maxScale
                effect.instance.transform.setToScaling(scale, scale, scale).trn(effect.position)
                blend?.opacity = 0.5f * progress
            }
            if (effect.life <= 0f) iterator.remove()
        }

        if (strongest > 0f) {
            impactLight.set(Color(1f, 0.8f, 0.42f, 1f), strongestPos, strongest)
        } else {
            impactLight.intensity = max(0f, impactLight.intensity - dt * 260f)
        }
    }

    private fun spawnDebris(position: Vector3, count: Int, color: Color) {
        repeat(count) {
            val velocity = Vector3(
                MathUtils.random(-1f, 1f),
                MathUtils.random(0.3f, 1.5f),
                MathUtils.random(-1f, 1f)
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

    private fun syncProjectileTransform(instance: ModelInstance, position: Vector3, velocity: Vector3, scale: Float) {
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
        val waveState = when {
            isGameOver -> "STATUS LOST"
            waveInProgress -> "WAVE $wave LIVE // ${threats.size + threatsRemainingInWave} HOSTILES"
            else -> "WAVE $wave READY"
        }
        creditsLabel.setText(
            "SCORE $score   CREDITS $credits   CITY ${(cityIntegrity.coerceAtLeast(0f)).toInt()}%   $waveState"
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

    private fun triggerShake(intensity: Float, duration: Float) {
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

    override fun resize(width: Int, height: Int) {
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

private fun Float.sign(): Float = when {
    this > 0f -> 1f
    this < 0f -> -1f
    else -> 0f
}

data class DefenseSettings(
    var engagementRange: Float = 1350f,
    var interceptorSpeed: Float = 420f,
    var launchCooldown: Float = 0.8f,
    var blastRadius: Float = 42f
)

enum class EffectType { BLAST, TRAIL }

data class BuildingEntity(
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
    var collapseVelocity: Float = 0f
)

data class ThreatEntity(
    val instance: ModelInstance,
    val position: Vector3,
    val velocity: Vector3,
    val id: String,
    var isTracked: Boolean = false,
    var trailCooldown: Float = 0f
)

data class InterceptorEntity(
    val instance: ModelInstance,
    val position: Vector3,
    val velocity: Vector3,
    var target: ThreatEntity?,
    var trailCooldown: Float = 0f
)

data class VisualEffect(
    val instance: ModelInstance,
    val position: Vector3,
    var life: Float,
    val initialLife: Float,
    val type: EffectType,
    val maxScale: Float
)

data class DebrisEntity(
    val instance: ModelInstance,
    val position: Vector3,
    val velocity: Vector3,
    var life: Float,
    val scale: Float,
    val rotation: Vector3 = Vector3()
)

data class SurfaceTextureSet(
    val diffuse: Texture,
    val roughness: Texture
)
