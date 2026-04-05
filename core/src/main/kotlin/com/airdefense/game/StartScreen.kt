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
import kotlin.math.*

class StartScreen(private val game: AirDefenseGame) : ScreenAdapter() {
    private val stage = Stage(ScreenViewport())
    private val skin: Skin
    private val modelBatch = ModelBatch()
    private val camera = PerspectiveCamera(60f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
    private val environment = Environment()
    private val instances = Array<ModelInstance>()
    private val models = ObjectMap<String, Model>()
    
    private var scanLineY = 0f
    private val root = Table()

    init {
        skin = createSkin()
        setup3DBackground()
        
        root.setFillParent(true)
        
        val titleTable = Table()
        val titleLabel = Label("PROJECT: AIR DEFENSE", skin, "title")
        val subLabel = Label("STRATEGIC INTERCEPT COMMAND // VER 3.0.4", skin, "status")
        
        titleTable.add(titleLabel).row()
        titleTable.add(subLabel).padTop(10f).row()
        root.add(titleTable).padBottom(120f).row()

        val menuTable = Table()
        menuTable.defaults().width(450f).height(80f).pad(20f)
        
        val startBtn = TextButton("INITIALIZE DEFENSE NETWORK", skin)
        startBtn.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                Gdx.app.log("StartScreen", "Initializing Defense Network...")
                game.screen = BattleScreen(game)
            }
        })
        
        val exitBtn = TextButton("TERMINATE SESSION", skin)
        exitBtn.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                Gdx.app.exit()
            }
        })

        menuTable.add(startBtn).row()
        menuTable.add(exitBtn).row()
        root.add(menuTable).row()

        val footer = Table(skin).apply {
            background = skin.newDrawable("white", Color(0f, 0.1f, 0.2f, 0.5f))
            val footerText = Label("ESTABLISHING SECURE LINK... OK | RADAR ARRAY... ONLINE | BATTERY STATUS... READY", skin, "default")
            add(footerText).pad(10f)
        }
        root.add(footer).expandX().fillX().bottom().padTop(100f)

        stage.addActor(root)
        Gdx.input.inputProcessor = stage
    }

    private fun setup3DBackground() {
        camera.position.set(0f, 200f, 500f)
        camera.lookAt(0f, 0f, 0f)
        camera.near = 1f
        camera.far = 5000f
        camera.update()

        environment.set(ColorAttribute(ColorAttribute.AmbientLight, 0.3f, 0.4f, 0.6f, 1f))
        environment.add(DirectionalLight().set(Color.CYAN, -1f, -0.8f, -0.2f))
        environment.add(PointLight().set(Color.WHITE, Vector3(100f, 100f, 100f), 1000f))

        val mb = ModelBuilder()
        val attr = (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal or VertexAttributes.Usage.TextureCoordinates).toLong()

        // High-poly tactical globe
        val globeMat = Material(ColorAttribute.createDiffuse(Color(0f, 0.2f, 0.4f, 1f)), BlendingAttribute(0.6f))
        models.put("globe", mb.createSphere(400f, 400f, 400f, 64, 64, globeMat, attr))
        
        // Orbital Rings (High Poly)
        mb.begin()
        val ringMat = Material(ColorAttribute.createDiffuse(Color.CYAN))
        val p1 = mb.part("ring", GL20.GL_TRIANGLES, attr, ringMat)
        CylinderShapeBuilder.build(p1, 450f, 2f, 450f, 80)
        models.put("ring", mb.end())

        instances.add(ModelInstance(models.get("globe")))
        instances.add(ModelInstance(models.get("ring")).apply { transform.rotate(Vector3.X, 75f) })
        instances.add(ModelInstance(models.get("ring")).apply { transform.rotate(Vector3.Z, 45f) })
    }

    private fun createSkin(): Skin {
        val s = Skin()
        val uiScale = Gdx.graphics.height / 1080f
        val font = BitmapFont().apply { data.setScale(1.3f * uiScale) }
        val titleFont = BitmapFont().apply { data.setScale(3.5f * uiScale) }
        s.add("default", font, BitmapFont::class.java)
        s.add("title", titleFont, BitmapFont::class.java)

        val pix = Pixmap(1, 1, Pixmap.Format.RGBA8888)
        pix.setColor(Color.WHITE)
        pix.fill()
        val whiteTex = Texture(pix)
        val whiteRegion = TextureRegion(whiteTex)
        s.add("white", whiteRegion, TextureRegion::class.java)
        s.add("white", TextureRegionDrawable(whiteRegion), Drawable::class.java)

        // Fixed Button Registration using TextureRegion
        fun createBtnRegion(up: Boolean): TextureRegion {
            val w = (450 * uiScale).toInt()
            val h = (80 * uiScale).toInt()
            val p = Pixmap(w, h, Pixmap.Format.RGBA8888)
            if (up) {
                p.setColor(0.02f, 0.1f, 0.2f, 0.85f); p.fill()
                p.setColor(0f, 0.8f, 1f, 1f); p.drawRectangle(0, 0, w, h)
                p.drawRectangle(1, 1, w - 2, h - 2)
            } else {
                p.setColor(0f, 0.4f, 0.7f, 0.95f); p.fill()
                p.setColor(Color.WHITE); p.drawRectangle(0, 0, w, h)
            }
            val tex = Texture(p)
            p.dispose()
            return TextureRegion(tex)
        }

        val btnUp = createBtnRegion(true)
        val btnDown = createBtnRegion(false)
        s.add("btn_up", btnUp, TextureRegion::class.java)
        s.add("btn_up", TextureRegionDrawable(btnUp), Drawable::class.java)
        s.add("btn_down", btnDown, TextureRegion::class.java)
        s.add("btn_down", TextureRegionDrawable(btnDown), Drawable::class.java)

        s.add("default", TextButton.TextButtonStyle().apply {
            up = s.getDrawable("btn_up")
            down = s.getDrawable("btn_down")
            over = s.newDrawable("btn_up", Color(0.8f, 0.9f, 1f, 1f))
            this.font = font
            fontColor = Color.WHITE
        })

        s.add("default", Label.LabelStyle(font, Color.LIGHT_GRAY))
        s.add("status", Label.LabelStyle(font, Color.CYAN))
        s.add("title", Label.LabelStyle(titleFont, Color.WHITE))

        pix.dispose()
        return s
    }

    override fun render(delta: Float) {
        ScreenUtils.clear(0.01f, 0.02f, 0.05f, 1f, true)
        
        instances.forEach { it.transform.rotate(Vector3.Y, delta * 15f) }
        
        modelBatch.begin(camera)
        modelBatch.render(instances, environment)
        modelBatch.end()

        val uiScale = Gdx.graphics.height / 1080f
        scanLineY = (scanLineY + delta * 300f * uiScale) % Gdx.graphics.height
        
        stage.act(delta)
        stage.draw()
        
        val batch = stage.batch
        batch.begin()
        batch.setColor(0f, 1f, 1f, 0.05f)
        batch.draw(skin.getRegion("white"), 0f, scanLineY, Gdx.graphics.width.toFloat(), 3f * uiScale)
        batch.draw(skin.getRegion("white"), 0f, (scanLineY + 200 * uiScale) % Gdx.graphics.height, Gdx.graphics.width.toFloat(), 1f * uiScale)
        
        batch.setColor(0f, 0f, 0f, 0.4f)
        batch.draw(skin.getRegion("white"), 0f, Gdx.graphics.height - 100f * uiScale, Gdx.graphics.width.toFloat(), 100f * uiScale)
        batch.draw(skin.getRegion("white"), 0f, 0f, Gdx.graphics.width.toFloat(), 100f * uiScale)
        
        batch.setColor(Color.WHITE)
        batch.end()
    }

    override fun resize(width: Int, height: Int) {
        stage.viewport.update(width, height, true)
        camera.viewportWidth = width.toFloat()
        camera.viewportHeight = height.toFloat()
        camera.update()
        
        // Re-center UI root after scaling
        val uiScale = height / 1080f
        root.clear()
        
        val titleTable = Table()
        val titleLabel = Label("PROJECT: AIR DEFENSE", skin, "title")
        val subLabel = Label("STRATEGIC INTERCEPT COMMAND // VER 3.0.4", skin, "status")
        
        titleTable.add(titleLabel).row()
        titleTable.add(subLabel).padTop(10f * uiScale).row()
        root.add(titleTable).padBottom(120f * uiScale).row()

        val menuTable = Table()
        menuTable.defaults().width(450f * uiScale).height(80f * uiScale).pad(20f * uiScale)
        
        val startBtn = TextButton("INITIALIZE DEFENSE NETWORK", skin)
        startBtn.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                game.screen = BattleScreen(game)
            }
        })
        
        val exitBtn = TextButton("TERMINATE SESSION", skin)
        exitBtn.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                Gdx.app.exit()
            }
        })

        menuTable.add(startBtn).row()
        menuTable.add(exitBtn).row()
        root.add(menuTable).row()

        val footer = Table(skin).apply {
            background = skin.newDrawable("white", Color(0f, 0.1f, 0.2f, 0.5f))
            val footerText = Label("ESTABLISHING SECURE LINK... OK | RADAR ARRAY... ONLINE | BATTERY STATUS... READY", skin, "default")
            add(footerText).pad(10f * uiScale)
        }
        root.add(footer).expandX().fillX().bottom().padTop(100f * uiScale)
    }

    override fun dispose() {
        stage.dispose()
        skin.dispose()
        modelBatch.dispose()
        models.values().forEach { it.dispose() }
    }
}
