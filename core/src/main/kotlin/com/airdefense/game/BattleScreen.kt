package com.airdefense.game

import com.badlogic.gdx.*
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.*
import com.badlogic.gdx.graphics.g3d.*
import com.badlogic.gdx.graphics.g3d.attributes.*
import com.badlogic.gdx.graphics.g3d.environment.*
import com.badlogic.gdx.graphics.g3d.utils.*
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.*
import com.badlogic.gdx.math.*
import com.badlogic.gdx.scenes.scene2d.*
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.*
import com.badlogic.gdx.utils.*
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.badlogic.gdx.utils.ScreenUtils
import kotlin.math.*

class BattleScreen(private val game: AirDefenseGame) : ScreenAdapter() {
    // --- High-Fidelity Engine Systems ---
    private val modelBatch = ModelBatch()
    private val environment = Environment()
    private val impactLight = PointLight()
    private val camera = PerspectiveCamera(60f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
    private val settings = DefenseSettings()
    private val stage = Stage(ScreenViewport())
    private val skin: Skin
    
    // --- Asset Management ---
    private val models = ObjectMap<String, Model>()
    private val instances = Array<ModelInstance>()
    private val cityBlocks = Array<BuildingEntity>()
    private val launchers = Array<ModelInstance>()
    private val threats = Array<ThreatEntity>()
    private val interceptors = Array<InterceptorEntity>()
    private val effects = Array<VisualEffect>()
    private val debris = Array<DebrisEntity>()
    
    // --- Audio ---
    private val sounds = ObjectMap<String, com.badlogic.gdx.audio.Sound>()
    
    // --- Game State ---
    private var credits = 10000
    private var wave = 1
    private var score = 0
    private var health = 100f
    private var isGameOver = false
    private var waveInProgress = false
    private var spawnTimer = 0f
    private var threatsRemainingInWave = 0
    private var timeSinceLastEngagement = 0f
    private var radarScanAngle = 0f
    
    // --- Screen Shake ---
    private var shakeTime = 0f
    private var shakeIntensity = 0f
    private val baseCameraPos = Vector3(350f, 300f, 650f)
    
    // --- Optimized Math Buffers (Zero Allocation) ---
    private val v1 = Vector3()
    private val v2 = Vector3()
    private val v3 = Vector3()
    private val tempVec = Vector3()
    private val gravity = Vector3(0f, -45f, 0f)

    private companion object {
        private const val THREAT_SCALE = 3.2f
        private const val INTERCEPTOR_SCALE = 4.0f
        private const val THREAT_TRAIL_INTERVAL = 0.04f
    }

    // --- Audio Loading ---
    private fun loadAudio() {
        try {
            val launchFile = Gdx.files.internal("sfx/launch.mp3")
            if (launchFile.exists()) sounds.put("launch", Gdx.audio.newSound(launchFile))
            
            val detonateFile = Gdx.files.internal("sfx/detonate.mp3")
            if (detonateFile.exists()) sounds.put("detonate", Gdx.audio.newSound(detonateFile))
            
            val impactFile = Gdx.files.internal("sfx/impact.mp3")
            if (impactFile.exists()) sounds.put("impact", Gdx.audio.newSound(impactFile))
        } catch (e: Exception) {
            Gdx.app.error("Audio", "Error loading audio: ${e.message}")
        }
    }

    private fun playSfx(name: String, volume: Float = 0.5f) {
        sounds.get(name)?.play(volume)
    }

    // --- UI Elements ---
    private val statusLabel: Label
    private val creditsLabel: Label

    init {
        val localSkin = createSkin()
        this.skin = localSkin
        
        statusLabel = Label("NETWORK ONLINE // READY FOR ENGAGEMENT", localSkin, "status")
        creditsLabel = Label("CMD CREDITS: $credits | SCORE: $score", localSkin, "status")

        setupEnvironment()
        setupCamera()
        
        generateWorldModels()
        generateThreatModels()
        
        createInitialEnvironment()
        setupHud(localSkin)
        loadAudio()
        startNewWave()
        
        stage.addListener(object : InputListener() {
            override fun touchDown(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int): Boolean {
                if (isGameOver) {
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
        val font = BitmapFont().apply { data.setScale(1.1f * uiScale) }
        val titleFont = BitmapFont().apply { data.setScale(1.8f * uiScale) }
        s.add("default", font, BitmapFont::class.java)
        s.add("title", titleFont, BitmapFont::class.java)

        val pixmap = Pixmap(128, 128, Pixmap.Format.RGBA8888)
        
        pixmap.setColor(Color(0.02f, 0.05f, 0.1f, 0.9f))
        pixmap.fill()
        val bgTex = Texture(pixmap).apply { setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear) }
        s.add("bg_tex", bgTex)
        s.add("bg_panel", TextureRegion(bgTex))
        s.add("bg_panel", TextureRegionDrawable(s.getRegion("bg_panel")), Drawable::class.java)
        
        pixmap.setColor(Color.WHITE)
        pixmap.fill()
        val whiteTex = Texture(pixmap)
        s.add("white_tex", whiteTex)
        s.add("white", TextureRegion(whiteTex))
        s.add("white", TextureRegionDrawable(s.getRegion("white")), Drawable::class.java)

        fun addBtn(name: String, upC: Color, brdC: Color) {
            val p = Pixmap(128, 128, Pixmap.Format.RGBA8888)
            p.setColor(upC)
            p.fill()
            p.setColor(brdC)
            p.drawRectangle(0, 0, 128, 128)
            p.drawRectangle(1, 1, 126, 126)
            val tex = Texture(p).apply { setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear) }
            s.add("${name}_tex", tex)
            s.add(name, TextureRegion(tex))
            s.add(name, TextureRegionDrawable(s.getRegion(name)), Drawable::class.java)
            p.dispose()
        }
        addBtn("btn_up", Color(0.05f, 0.12f, 0.2f, 0.8f), Color(0f, 0.8f, 1f, 1f))
        addBtn("btn_down", Color(0f, 0.4f, 0.6f, 0.9f), Color(0.5f, 1f, 1f, 1f))
        
        s.add("default", TextButton.TextButtonStyle().apply {
            up = s.getDrawable("btn_up")
            down = s.getDrawable("btn_down")
            over = s.newDrawable("btn_up", Color.DARK_GRAY)
            this.font = font
            fontColor = Color.WHITE
        })

        s.add("default", Label.LabelStyle(font, Color.WHITE))
        s.add("status", Label.LabelStyle(font, Color.CYAN))
        s.add("warning", Label.LabelStyle(font, Color.ORANGE))
        s.add("critical", Label.LabelStyle(font, Color.RED))
        s.add("title", Label.LabelStyle(titleFont, Color.CYAN))

        val sBack = Pixmap(100, 8, Pixmap.Format.RGBA8888).apply { setColor(0.1f, 0.2f, 0.3f, 1f); fill() }
        val sBackTex = Texture(sBack)
        s.add("s_back_tex", sBackTex); s.add("s_back", TextureRegion(sBackTex))
        s.add("s_back", TextureRegionDrawable(s.getRegion("s_back")), Drawable::class.java)
        
        val sKnob = Pixmap(16, 24, Pixmap.Format.RGBA8888).apply { setColor(0f, 0.9f, 1f, 1f); fill() }
        val sKnobTex = Texture(sKnob)
        s.add("s_knob_tex", sKnobTex); s.add("s_knob", TextureRegion(sKnobTex))
        s.add("s_knob", TextureRegionDrawable(s.getRegion("s_knob")), Drawable::class.java)
        
        s.add("default-horizontal", Slider.SliderStyle(s.getDrawable("s_back"), s.getDrawable("s_knob")))

        pixmap.dispose(); sBack.dispose(); sKnob.dispose()
        return s
    }

    private fun setupEnvironment() {
        environment.set(ColorAttribute(ColorAttribute.AmbientLight, 0.12f, 0.15f, 0.22f, 1f))
        environment.add(DirectionalLight().set(Color(0.7f, 0.8f, 1f, 1f), -0.5f, -1f, -0.3f))
        impactLight.set(Color.BLACK, Vector3.Zero, 0f)
        environment.add(impactLight)
    }

    private fun setupCamera() {
        camera.near = 1f
        camera.far = 10000f
        camera.position.set(300f, 250f, 600f)
        camera.lookAt(0f, 50f, -200f)
        camera.update()
    }

    private fun generateWorldModels() {
        val mb = ModelBuilder()
        val attr = (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal or VertexAttributes.Usage.TextureCoordinates).toLong()

        // Skybox with Stars
        mb.begin()
        val skyMat = Material(ColorAttribute.createDiffuse(Color(0.01f, 0.01f, 0.03f, 1f)), IntAttribute.createCullFace(GL20.GL_NONE))
        mb.part("sky", GL20.GL_TRIANGLES, attr, skyMat).apply { SphereShapeBuilder.build(this, 16000f, 16000f, 16000f, 32, 32) }
        val starMat = Material(ColorAttribute.createDiffuse(Color.WHITE), ColorAttribute.createEmissive(Color.WHITE))
        repeat(500) { i ->
            mb.node().translation.set(MathUtils.random(-8000f, 8000f), MathUtils.random(200f, 8000f), MathUtils.random(-8000f, 8000f))
            if (mb.node().translation.len() < 7500f) mb.node().translation.nor().scl(7800f)
            mb.part("star_$i", GL20.GL_TRIANGLES, attr, starMat).apply { SphereShapeBuilder.build(this, 15f, 15f, 15f, 4, 4) }
        }
        models.put("sky", mb.end())

        // Ground with Grid
        mb.begin()
        val groundMat = Material(ColorAttribute.createDiffuse(Color(0.05f, 0.08f, 0.1f, 1f)), FloatAttribute.createShininess(10f))
        mb.part("base", GL20.GL_TRIANGLES, attr, groundMat).apply { BoxShapeBuilder.build(this, 12000f, 2f, 12000f) }
        val gridMat = Material(ColorAttribute.createDiffuse(Color(0f, 0.4f, 0.6f, 1f)), BlendingAttribute(0.3f))
        for (i in -15..15) {
            mb.node().translation.set(i * 400f, 1.2f, 0f)
            mb.part("gx_$i", GL20.GL_TRIANGLES, attr, gridMat).apply { BoxShapeBuilder.build(this, 2f, 0.1f, 12000f) }
            mb.node().translation.set(0f, 1.2f, i * 400f)
            mb.part("gz_$i", GL20.GL_TRIANGLES, attr, gridMat).apply { BoxShapeBuilder.build(this, 12000f, 0.1f, 2f) }
        }
        models.put("ground", mb.end())

        fun buildBuilding(id: String, h: Float, w: Float, baseColor: Color) {
            mb.begin()
            val mat = Material(ColorAttribute.createDiffuse(baseColor), FloatAttribute.createShininess(25f))
            mb.part("core", GL20.GL_TRIANGLES, attr, mat).apply { BoxShapeBuilder.build(this, w, h, w) }
            val windowMat = Material(ColorAttribute.createDiffuse(Color.BLACK), ColorAttribute.createEmissive(Color(0.1f, 0.3f, 0.6f, 1f)))
            for (side in 0..3) {
                mb.node().apply { rotation.set(Vector3.Y, side * 90f) }
                for (floor in 1..10) {
                    val fy = (h / 12f) * floor - (h / 2f)
                    mb.node().translation.set(w/2f + 0.6f, fy, 0f)
                    mb.part("w_${side}_${floor}", GL20.GL_TRIANGLES, attr, windowMat).apply { BoxShapeBuilder.build(this, 0.2f, h/25f, w * 0.7f) }
                }
            }
            models.put(id, mb.end())
        }
        buildBuilding("b_tall", 240f, 50f, Color(0.2f, 0.22f, 0.28f, 1f))
        buildBuilding("b_med", 140f, 60f, Color(0.18f, 0.2f, 0.25f, 1f))
        buildBuilding("b_wide", 80f, 110f, Color(0.22f, 0.25f, 0.3f, 1f))

        // Launcher
        mb.begin()
        val milColor = Color(0.15f, 0.22f, 0.15f, 1f)
        val metalMat = Material(ColorAttribute.createDiffuse(milColor), FloatAttribute.createShininess(10f))
        mb.part("chassis", GL20.GL_TRIANGLES, attr, metalMat).apply { BoxShapeBuilder.build(this, 28f, 5f, 45f) }
        mb.node().apply { translation.set(0f, 15f, -5f); rotation.set(Vector3.X, -30f) }
        repeat(4) { i ->
            mb.node().translation.set(-9f + i * 6f, 15f, -5f)
            mb.part("can_$i", GL20.GL_TRIANGLES, attr, Material(ColorAttribute.createDiffuse(Color(0.1f, 0.15f, 0.1f, 1f))))
                .apply { BoxShapeBuilder.build(this, 5.5f, 5.5f, 35f) }
        }
        models.put("launcher", mb.end())

        // Radar
        mb.begin()
        mb.part("r_base", GL20.GL_TRIANGLES, attr, metalMat).apply { BoxShapeBuilder.build(this, 30f, 6f, 50f) }
        mb.node().apply { translation.set(0f, 25f, 0f); rotation.set(Vector3.X, 15f) }
        mb.part("r_face", GL20.GL_TRIANGLES, attr, Material(ColorAttribute.createDiffuse(Color(0.3f, 0.35f, 0.3f, 1f))))
            .apply { BoxShapeBuilder.build(this, 45f, 35f, 5f) }
        models.put("radar", mb.end())
        
        models.put("debris", mb.createBox(2f, 2f, 2f, Material(ColorAttribute.createDiffuse(Color.GRAY)), attr))
    }

    private fun generateThreatModels() {
        val mb = ModelBuilder()
        val attr = (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal or VertexAttributes.Usage.TextureCoordinates).toLong()

        // Ballistic Threat
        mb.begin()
        val mBody = Material(ColorAttribute.createDiffuse(Color.LIGHT_GRAY), FloatAttribute.createShininess(40f))
        mb.part("s1", GL20.GL_TRIANGLES, attr, mBody).apply { CylinderShapeBuilder.build(this, 3.5f, 22f, 3.5f, 32) }
        mb.node().translation.set(0f, 15f, 0f)
        mb.part("nose", GL20.GL_TRIANGLES, attr, Material(ColorAttribute.createDiffuse(Color.FIREBRICK))).apply { ConeShapeBuilder.build(this, 3.5f, 8f, 3.5f, 32) }
        repeat(4) { i ->
            mb.node().apply { translation.set(2f, -8f, 0f); rotation.set(Vector3.Y, i * 90f) }
            mb.part("f_$i", GL20.GL_TRIANGLES, attr, Material(ColorAttribute.createDiffuse(Color.GRAY))).apply { BoxShapeBuilder.build(this, 4f, 6f, 0.4f) }
        }
        models.put("t_ballistic", mb.end())

        // Interceptor
        mb.begin()
        val mPat = Material(ColorAttribute.createDiffuse(Color(0.95f, 0.95f, 0.9f, 1f)), FloatAttribute.createShininess(60f))
        mb.part("p_body", GL20.GL_TRIANGLES, attr, mPat).apply { CylinderShapeBuilder.build(this, 1.1f, 18f, 1.1f, 32) }
        mb.node().translation.set(0f, 9f, 0f)
        mb.part("p_nose", GL20.GL_TRIANGLES, attr, Material(ColorAttribute.createDiffuse(Color(0.85f, 0.8f, 0.7f, 1f)))).apply { ConeShapeBuilder.build(this, 1.1f, 4f, 1.1f, 32) }
        repeat(4) { i ->
            mb.node().apply { translation.set(1.2f, -8f, 0f); rotation.set(Vector3.Y, i * 90f + 45f) }
            mb.part("pf_$i", GL20.GL_TRIANGLES, attr, Material(ColorAttribute.createDiffuse(Color.DARK_GRAY))).apply { BoxShapeBuilder.build(this, 3f, 2.5f, 0.25f) }
        }
        models.put("interceptor", mb.end())

        models.put("blast", mb.createSphere(1f, 1f, 1f, 32, 32, Material(ColorAttribute.createDiffuse(Color.GOLD), ColorAttribute.createEmissive(Color.ORANGE), BlendingAttribute(0.9f)), attr))
        models.put("trail", mb.createSphere(1.0f, 1.0f, 1.0f, 12, 12, Material(ColorAttribute.createDiffuse(Color(0.8f, 0.8f, 0.9f, 1f)), BlendingAttribute(0.4f)), attr))
    }

    private fun createInitialEnvironment() {
        instances.add(ModelInstance(models.get("sky")))
        instances.add(ModelInstance(models.get("ground")).apply { transform.setToTranslation(0f, -2.5f, 0f) })
        
        val l1 = ModelInstance(models.get("launcher")).apply { transform.setToTranslation(120f, 3f, 150f) }
        val l2 = ModelInstance(models.get("launcher")).apply { transform.setToTranslation(-120f, 3f, 150f) }
        launchers.add(l1); launchers.add(l2)
        instances.add(l1); instances.add(l2)
        
        instances.add(ModelInstance(models.get("radar")).apply { transform.setToTranslation(0f, 3f, 220f) })

        val rng = MathUtils.random
        for (x in -8..8) {
            for (z in -6..2) {
                if (abs(x) < 2 && z > -1) continue 
                val type = rng.nextInt(3)
                val mName = when(type) { 0 -> "b_tall"; 1 -> "b_med"; else -> "b_wide" }
                val pos = Vector3(x * 150f + rng.nextFloat() * 40f, 0f, z * 200f - 400f)
                val h = when(type) { 0 -> 240f; 1 -> 140f; else -> 80f }
                val inst = ModelInstance(models.get(mName)).apply { transform.setToTranslation(pos.x, h/2, pos.z); transform.rotate(Vector3.Y, rng.nextFloat() * 360f) }
                cityBlocks.add(BuildingEntity(inst, 200f))
            }
        }
    }

    private fun setupHud(currentSkin: Skin) {
        val uiScale = Gdx.graphics.height / 1080f
        stage.clear()
        val root = Table().apply { setFillParent(true) }
        val topTable = Table().apply { background = currentSkin.newDrawable("white", Color(0f, 0.05f, 0.1f, 0.85f)) }
        topTable.add(statusLabel).expandX().left().pad(15f * uiScale)
        topTable.add(creditsLabel).right().pad(15f * uiScale)
        root.add(topTable).expandX().fillX().top().row()

        val side = Table().apply { background = currentSkin.getDrawable("bg_panel"); defaults().pad(12f * uiScale).width(350f * uiScale) }
        side.add(Label("STRATEGIC DEFENSE COMMAND", currentSkin, "title")).padBottom(40f * uiScale).row()
        val startBtn = TextButton("INITIATE RESPONSE", currentSkin).apply { addListener(object : ChangeListener() { override fun changed(e: ChangeEvent?, a: Actor?) { if (!waveInProgress) startNewWave() } }) }
        side.add(startBtn).height(100f * uiScale).fillX().row()
        side.add(Label("ENGAGEMENT RANGE", currentSkin)).row()
        val rS = Slider(200f, 2500f, 50f, false, currentSkin).apply { value = settings.engagementRange; addListener(object : ChangeListener() { override fun changed(e: ChangeEvent?, a: Actor?) { settings.engagementRange = value } }) }
        side.add(rS).fillX().row()
        root.add(side).expand().right().fillY()
        stage.addActor(root)
    }

    private fun updateHud() {
        creditsLabel.setText("CMD CREDITS: $credits | SCORE: $score")
    }

    private fun startNewWave() {
        waveInProgress = true
        threatsRemainingInWave = 8 + wave * 4
        spawnTimer = 0f
        timeSinceLastEngagement = settings.launchCooldown
        statusLabel.setText("WARNING: MULTIPLE INBOUND // WAVE $wave")
        statusLabel.style = skin.get("critical", Label.LabelStyle::class.java)
    }

    override fun render(delta: Float) {
        if (isGameOver) {
            val batch = stage.batch
            batch.begin()
            val font = skin.getFont("title")
            val layout = GlyphLayout(font, "MISSION FAILED")
            font.draw(batch, layout, (Gdx.graphics.width - layout.width) / 2f, Gdx.graphics.height / 2f + 50f)
            val fontSmall = skin.getFont("default")
            val layout2 = GlyphLayout(fontSmall, "FINAL SCORE: $score - TAP TO RESTART")
            fontSmall.draw(batch, layout2, (Gdx.graphics.width - layout2.width) / 2f, Gdx.graphics.height / 2f - 50f)
            batch.end()
            return
        }
        updateLogic(delta)
        ScreenUtils.clear(0.01f, 0.02f, 0.05f, 1f, true)
        modelBatch.begin(camera)
        modelBatch.render(instances, environment)
        cityBlocks.forEach { 
            if (it.health > 0) modelBatch.render(it.instance, environment)
            else {
                // Could render as rubble or just dark
                modelBatch.render(it.instance, environment)
            }
        }
        threats.forEach { modelBatch.render(it.instance, environment) }
        interceptors.forEach { modelBatch.render(it.instance, environment) }
        effects.forEach { modelBatch.render(it.instance, environment) }
        debris.forEach { modelBatch.render(it.instance, environment) }
        modelBatch.end()
        
        renderHudOverlay()
        stage.act(delta); stage.draw()
        
        val uiScale = Gdx.graphics.height / 1080f
        val batch = stage.batch
        batch.begin()
        val font = skin.getFont("default")
        threats.forEach { t ->
            if (t.isIdentified) {
                camera.project(tempVec.set(t.position))
                if (tempVec.z > 0 && tempVec.z < 1) {
                    val threatLevel = (t.velocity.len() / 400f).coerceIn(0.5f, 1.5f)
                    batch.color = if (threatLevel > 1.2f) Color.RED else Color.CYAN
                    val rs = 60f * uiScale
                    batch.draw(skin.getRegion("white"), tempVec.x - rs / 2, tempVec.y + 45f * uiScale, rs, 2f * uiScale)
                    font.draw(batch, "[${t.id}] ALT:${(t.position.y / 10f).toInt()}0m", tempVec.x + rs / 2, tempVec.y + 50f * uiScale)
                    font.draw(batch, "SPD:${(t.velocity.len() * 3.6f).toInt()}km/h", tempVec.x + rs / 2, tempVec.y + 30f * uiScale)
                }
            }
        }
        batch.end()
    }

    private fun renderHudOverlay() {
        val uiScale = Gdx.graphics.height / 1080f
        val batch = stage.batch
        batch.begin()
        
        // Tactical Crosshair
        batch.color = Color(0f, 1f, 1f, 0.3f)
        val cx = Gdx.graphics.width / 2f
        val cy = Gdx.graphics.height / 2f
        val sz = 40f * uiScale
        batch.draw(skin.getRegion("white"), cx - sz, cy, sz * 0.4f, 2f)
        batch.draw(skin.getRegion("white"), cx + sz * 0.6f, cy, sz * 0.4f, 2f)
        batch.draw(skin.getRegion("white"), cx, cy - sz, 2f, sz * 0.4f)
        batch.draw(skin.getRegion("white"), cx, cy + sz * 0.6f, 2f, sz * 0.4f)
        
        // Radar Minimap
        val rSize = 250f * uiScale
        val rx = Gdx.graphics.width - rSize - 20f * uiScale
        val ry = 20f * uiScale
        batch.color = Color(0f, 0.1f, 0.2f, 0.7f)
        batch.draw(skin.getRegion("white"), rx, ry, rSize, rSize)
        batch.color = Color(0f, 0.5f, 0.8f, 0.5f)
        
        // Radar Sweep (visual only)
        val sweepX = rSize / 2 + rx + cos(radarScanAngle * MathUtils.degreesToRadians) * rSize / 2
        val sweepY = rSize / 2 + ry + sin(radarScanAngle * MathUtils.degreesToRadians) * rSize / 2
        batch.draw(skin.getRegion("white"), rSize / 2 + rx, rSize / 2 + ry, sweepX - (rSize / 2 + rx), sweepY - (rSize / 2 + ry))
        
        // Threats on Radar
        threats.forEach { t ->
            val tx = (t.position.x / 4000f) * rSize / 2 + rx + rSize / 2
            val tz = (t.position.z / 4000f) * rSize / 2 + ry + rSize / 2
            if (tx in rx..(rx + rSize) && tz in ry..(ry + rSize)) {
                batch.color = if (t.isIdentified) Color.RED else Color.YELLOW
                batch.draw(skin.getRegion("white"), tx - 2f, tz - 2f, 4f, 4f)
            }
        }
        
        batch.color = Color.WHITE
        batch.end()
    }

    private fun updateLogic(delta: Float) {
        val dt = min(delta, 1/30f)
        radarScanAngle = (radarScanAngle + dt * 120f) % 360f
        
        // Update Screen Shake
        if (shakeTime > 0) {
            shakeTime -= dt
            val currentIntensity = shakeIntensity * (shakeTime / 0.5f).coerceIn(0f, 1f)
            camera.position.set(baseCameraPos).add(
                MathUtils.random(-1f, 1f) * currentIntensity,
                MathUtils.random(-1f, 1f) * currentIntensity,
                MathUtils.random(-1f, 1f) * currentIntensity
            )
        } else {
            camera.position.set(baseCameraPos)
        }
        camera.lookAt(0f, 50f, -200f)
        camera.update()

        if (waveInProgress) {
            spawnTimer -= dt
            if (spawnTimer <= 0f && threatsRemainingInWave > 0) {
                spawnThreat()
                threatsRemainingInWave--
                spawnTimer = max(0.5f, 2.5f - wave * 0.2f)
            }
            if (threatsRemainingInWave <= 0 && threats.isEmpty) {
                waveInProgress = false
                wave++
                credits += 2500
                statusLabel.setText("AREA SECURED")
                statusLabel.style = skin.get("status", Label.LabelStyle::class.java)
            }
        }
        updateThreats(dt); updateInterceptors(dt); updateEffects(dt); updateDebris(dt); updateHud()
        if (health <= 0) { isGameOver = true; statusLabel.setText("CITY LOST"); statusLabel.style = skin.get("critical", Label.LabelStyle::class.java) }
    }

    private fun spawnDebris(pos: Vector3, count: Int) {
        repeat(count) {
            v1.set(MathUtils.random(-1f, 1f), MathUtils.random(0.5f, 2f), MathUtils.random(-1f, 1f)).nor().scl(MathUtils.random(50f, 250f))
            val inst = ModelInstance(models.get("debris")).apply { transform.setToTranslation(pos) }
            debris.add(DebrisEntity(inst, pos.cpy(), v1.cpy(), MathUtils.random(1.5f, 3.5f)))
        }
    }

    private fun updateDebris(dt: Float) {
        val it = debris.iterator()
        while (it.hasNext()) {
            val d = it.next(); d.life -= dt; d.velocity.y -= 250f * dt; v1.set(d.velocity).scl(dt); d.position.add(v1)
            d.instance.transform.setToTranslation(d.position); d.instance.transform.rotate(Vector3.X, 200f * dt)
            if (d.life <= 0f || d.position.y < 0f) it.remove()
        }
    }

    private fun spawnThreat() {
        // Start high and far
        val startPos = Vector3(MathUtils.random(-1500f, 1500f), 1200f, -4000f)
        // Target area around city
        val targetPos = Vector3(MathUtils.random(-300f, 300f), 0f, MathUtils.random(-400f, 100f))
        
        // Calculate initial velocity for ballistic trajectory
        val dist = startPos.dst(targetPos)
        val time = dist / (280f + wave * 25f)
        val velocity = targetPos.cpy().sub(startPos).scl(1f / time)
        // Add vertical arch factor
        velocity.y += 150f 

        val id = "T-${MathUtils.random(1000, 9999)}"
        val inst = ModelInstance(models.get("t_ballistic")).apply {
            transform.setToScaling(THREAT_SCALE, THREAT_SCALE, THREAT_SCALE).setTranslation(startPos)
            setRotationToward(velocity)
        }
        threats.add(ThreatEntity(inst, startPos, velocity, id, trailCooldown = MathUtils.random(0f, THREAT_TRAIL_INTERVAL)))
    }

    private fun updateThreats(dt: Float) {
        val it = threats.iterator()
        while (it.hasNext()) {
            val t = it.next()
            // Apply gravity to threat
            t.velocity.mulAdd(gravity, dt)
            v1.set(t.velocity).scl(dt)
            t.position.add(v1)
            t.instance.transform.setToScaling(THREAT_SCALE, THREAT_SCALE, THREAT_SCALE).setTranslation(t.position)
            t.instance.setRotationToward(t.velocity)

            t.trailCooldown -= dt
            if (t.trailCooldown <= 0f) {
                spawnTrail(t.position)
                t.trailCooldown = THREAT_TRAIL_INTERVAL
            }

            if (t.position.y <= 10f || t.position.z > 450f) {
                health -= 20f
                spawnBlast(t.position, 100f)
                spawnDebris(t.position, 15)
                triggerShake(25f, 0.6f)
                playSfx("impact", 0.8f)
                
                // Use a separate vector for city collision to avoid modifying t.position or t.velocity
                val impactPos = v3.set(t.position)
                cityBlocks.forEach {
                    it.instance.transform.getTranslation(v2)
                    if (v2.dst(impactPos) < 180f) {
                        it.health -= 60f
                        if (it.health <= 0) it.instance.materials.first().set(ColorAttribute.createDiffuse(Color.BLACK))
                    }
                }
                it.remove()
                continue
            }
        }
    }

    private fun updateInterceptors(dt: Float) {
        val it = interceptors.iterator()
        while (it.hasNext()) {
            val i = it.next()
            val target = i.target
            if (target != null && threats.contains(target, true)) {
                val predicted = predictInterceptPoint(i.position, target.position, target.velocity, settings.interceptorSpeed)
                v1.set(predicted).sub(i.position)
                if (!v1.isZero(1e-3f)) {
                    val maxTurn = (220f * dt).coerceAtMost(1f)
                    tempVec.set(v1).nor().scl(settings.interceptorSpeed)
                    i.velocity.lerp(tempVec, maxTurn)
                }
                i.velocity.nor().scl(settings.interceptorSpeed)
            }
            
            v1.set(i.velocity).scl(dt)
            i.position.add(v1)
            i.instance.transform.setToScaling(INTERCEPTOR_SCALE, INTERCEPTOR_SCALE, INTERCEPTOR_SCALE).setTranslation(i.position)
            i.instance.setRotationToward(i.velocity)
            
            if (MathUtils.randomBoolean(0.85f)) spawnTrail(i.position)
            
            if (target != null && threats.contains(target, true) && i.position.dst(target.position) < settings.blastRadius) {
                score += 100
                credits += 250
                spawnBlast(i.position, settings.blastRadius * 2.5f)
                spawnDebris(i.position, 10)
                triggerShake(10f, 0.3f)
                playSfx("detonate", 0.6f)
                threats.removeValue(target, true)
                it.remove()
            } else if (i.position.y > 4000f || i.position.dst(Vector3.Zero) > 7000f) {
                it.remove()
            }
        }
        timeSinceLastEngagement += dt
        if (timeSinceLastEngagement >= settings.launchCooldown) {
            val nextTarget = threats
                .filter { t ->
                    val d = t.position.len()
                    if (d < settings.engagementRange) t.isIdentified = true
                    d < settings.engagementRange && interceptors.none { it.target == t }
                }
                .minByOrNull { it.position.z }
            if (nextTarget != null) {
                launchInterceptor(nextTarget)
                timeSinceLastEngagement = 0f
            }
        }
    }

    private fun launchInterceptor(target: ThreatEntity) {
        val launcher = launchers.minByOrNull {
            it.transform.getTranslation(v1)
            v1.dst2(target.position)
        } ?: return

        launcher.transform.getTranslation(v1)
        v1.y += 12f // Launch from top of canister

        val interceptPoint = predictInterceptPoint(v1, target.position, target.velocity, settings.interceptorSpeed)
        v2.set(interceptPoint).sub(v1).nor().scl(settings.interceptorSpeed)

        // Aim launcher toward target
        launcher.setRotationToward(v2)

        val inst = ModelInstance(models.get("interceptor")).apply {
            transform.setToScaling(INTERCEPTOR_SCALE, INTERCEPTOR_SCALE, INTERCEPTOR_SCALE).setTranslation(v1)
            setRotationToward(v2)
        }
        interceptors.add(InterceptorEntity(inst, v1.cpy(), v2.cpy(), target))
        spawnBlast(v1, 15f)
        playSfx("launch", 0.4f)
    }

    private fun predictInterceptPoint(
        interceptorPos: Vector3,
        targetPos: Vector3,
        targetVelocity: Vector3,
        interceptorSpeed: Float
    ): Vector3 {
        val toTarget = v3.set(targetPos).sub(interceptorPos)
        val a = targetVelocity.dot(targetVelocity) - interceptorSpeed * interceptorSpeed
        val b = 2f * toTarget.dot(targetVelocity)
        val c = toTarget.dot(toTarget)

        val t = if (abs(a) < 1e-3f) {
            if (abs(b) < 1e-3f) 0f else (-c / b).coerceAtLeast(0f)
        } else {
            val disc = b * b - 4f * a * c
            if (disc < 0f) 0f
            else {
                val sqrtDisc = kotlin.math.sqrt(disc)
                val t1 = (-b - sqrtDisc) / (2f * a)
                val t2 = (-b + sqrtDisc) / (2f * a)
                listOf(t1, t2).filter { it > 0f }.minOrNull() ?: 0f
            }
        }.coerceIn(0f, 8f)

        return Vector3(targetPos).mulAdd(targetVelocity, t)
    }

    private fun triggerShake(intensity: Float, duration: Float) {
        shakeIntensity = max(shakeIntensity, intensity)
        shakeTime = max(shakeTime, duration)
    }

    private fun spawnBlast(pos: Vector3, size: Float) {
        val inst = ModelInstance(models.get("blast")).apply { transform.setToScaling(0.1f, 0.1f, 0.1f).setTranslation(pos) }
        effects.add(VisualEffect(inst, pos.cpy(), 0.8f, 0.8f, EffectType.BLAST, size))
        impactLight.set(Color.GOLD, pos, size * 15f)
    }

    private fun spawnTrail(pos: Vector3) {
        val inst = ModelInstance(models.get("trail")).apply { transform.setToScaling(0.5f, 0.5f, 0.5f).setTranslation(pos) }
        effects.add(VisualEffect(inst, pos.cpy(), 1.2f, 1.2f, EffectType.TRAIL, 0.5f))
    }

    private fun updateEffects(dt: Float) {
        val it = effects.iterator()
        var maxL = 0f; val lP = Vector3()
        while (it.hasNext()) {
            val e = it.next(); e.life -= dt; val p = e.life / e.initialLife
            if (e.type == EffectType.BLAST) {
                val s = e.maxScale * (1.1f - p * p)
                e.instance.transform.setToScaling(s, s, s).trn(e.position)
                (e.instance.materials.first().get(BlendingAttribute.Type) as BlendingAttribute).opacity = (p * p * p).coerceIn(0f, 1f)
                if (e.life > 0.4f) {
                    val intens = e.maxScale * p * 25f
                    if (intens > maxL) {
                        maxL = intens
                        lP.set(e.position)
                    }
                }
            } else {
                val s = 0.5f + (1.0f - p) * 4f
                e.instance.transform.setToScaling(s, s, s).trn(e.position)
                (e.instance.materials.first().get(BlendingAttribute.Type) as BlendingAttribute).opacity = (p * p * 0.5f).coerceIn(0f, 1f)
            }
            if (e.life <= 0) it.remove()
        }
        if (maxL > 0) {
            impactLight.set(Color.GOLD, lP, maxL)
        } else {
            impactLight.intensity = max(0f, impactLight.intensity - dt * 600f)
        }
    }

    private fun ModelInstance.setRotationToward(dir: Vector3) {
        if (dir.isZero(1e-6f)) return
        transform.getTranslation(tempVec)
        
        v2.set(dir).nor() // UP vector is the direction of travel
        // Find an arbitrary right vector (v3)
        v1.set(Vector3.Y)
        if (abs(v2.dot(v1)) > 0.99f) v1.set(Vector3.Z)
        
        v3.set(v1).crs(v2).nor() // Right
        v1.set(v2).crs(v3).nor() // Forward

        transform.set(v3, v2, v1, tempVec)
    }

    override fun resize(w: Int, h: Int) {
        stage.viewport.update(w, h, true)
        camera.viewportWidth = w.toFloat()
        camera.viewportHeight = h.toFloat()
        camera.update()
        setupHud(skin)
    }

    override fun dispose() {
        modelBatch.dispose()
        models.values().forEach { it.dispose() }
        sounds.values().forEach { it.dispose() }
        stage.dispose()
        skin.dispose()
    }
}

data class DefenseSettings(var engagementRange: Float = 1200f, var interceptorSpeed: Float = 700f, var launchCooldown: Float = 0.5f, var blastRadius: Float = 30f)
enum class EffectType { BLAST, TRAIL }
data class BuildingEntity(val instance: ModelInstance, var health: Float)
data class DebrisEntity(val instance: ModelInstance, val position: Vector3, val velocity: Vector3, var life: Float)
data class ThreatEntity(
    val instance: ModelInstance,
    val position: Vector3,
    val velocity: Vector3,
    var id: String,
    var type: String = "BALLISTIC",
    var rcs: Float = 0.15f,
    var isIdentified: Boolean = false,
    var trailCooldown: Float = 0f
)
data class InterceptorEntity(val instance: ModelInstance, val position: Vector3, val velocity: Vector3, var target: ThreatEntity?)
data class VisualEffect(val instance: ModelInstance, val position: Vector3, var life: Float, val initialLife: Float, val type: EffectType, val maxScale: Float = 1f)
