package com.airdefense.game

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.viewport.ScreenViewport

class StartScreen(
    private val game: AirDefenseGame,
) : ScreenAdapter() {
    private val isAndroid = Gdx.app.type == Application.ApplicationType.Android
    private val stage = Stage(ScreenViewport())
    private val textures = Array<Texture>()
    private val skin = createSkin()
    private val skyTexture = loadTexture("textures/sky_panorama_2k.jpg", Color(0.01f, 0.02f, 0.05f, 1f))
    private val cityTexture = loadTexture("textures/city_backdrop_telaviv.jpg", Color(0.08f, 0.08f, 0.12f, 1f))

    private var scanLineY = 0f
    private var launchRequested = false

    init {
        Gdx.graphics.isContinuousRendering = true
        val uiScale = Gdx.graphics.height / 1080f
        val root = Table().apply { setFillParent(true) }
        val body = Table().apply { defaults().top() }
        val hero =
            Table().apply {
                background = this@StartScreen.skin.newDrawable("panel_strong", Color(1f, 1f, 1f, 0.96f))
                pad(34f * uiScale)
                defaults().left()
            }
        hero.add(Label("PROJECT AIR DEFENSE", skin, "title")).row()
        hero.add(Label("COASTAL SHIELD GRID // LIVE INTERCEPTION COMMAND", skin, "headline")).padTop(14f * uiScale).row()
        hero
            .add(
                Label(
                    "Defend the skyline with faster launches, clearer tracking, and a modernized tactical interface built for phone play.",
                    skin,
                    "status",
                ),
            ).width(760f * uiScale)
            .padTop(22f * uiScale)
            .row()

        val statRow = Table().apply { defaults().padRight(14f * uiScale).padTop(20f * uiScale) }
        statRow.add(Label("REAL-TIME THREAT TRACKING", skin, "tag"))
        statRow.add(Label("COASTAL CITYSCAPE", skin, "tag"))
        statRow.add(Label("ANDROID SAFE RENDER PATH", skin, "tag"))
        hero.add(statRow).left().row()

        val menu =
            Table().apply {
                background = this@StartScreen.skin.newDrawable("panel_soft", Color(1f, 1f, 1f, 0.92f))
                pad(24f * uiScale)
                defaults().width(540f * uiScale).height(126f * uiScale).pad(10f * uiScale)
            }
        val startButton = TextButton("ENTER AIRSPACE", skin)
        startButton.addListener(
            object : ChangeListener() {
                override fun changed(
                    event: ChangeEvent?,
                    actor: Actor?,
                ) {
                    Gdx.app.log("StartScreen", "ENTER AIRSPACE pressed")
                    launchRequested = true
                    startButton.isDisabled = true
                }
            },
        )
        menu.add(startButton).row()
        if (!isAndroid) {
            val exitButton = TextButton("EXIT", skin)
            exitButton.addListener(
                object : ChangeListener() {
                    override fun changed(
                        event: ChangeEvent?,
                        actor: Actor?,
                    ) {
                        Gdx.app.log("StartScreen", "EXIT pressed")
                        Gdx.app.exit()
                    }
                },
            )
            menu.add(exitButton).row()
        }
        val ops =
            Table().apply {
                background = this@StartScreen.skin.newDrawable("panel_soft", Color(1f, 1f, 1f, 0.9f))
                pad(24f * uiScale)
                defaults().left().padBottom(12f * uiScale)
            }
        ops.add(Label("DEPLOYMENT OVERVIEW", skin, "headline")).row()
        ops
            .add(
                Label("BALLISTIC THREATS  //  PROXIMITY-FUZE INTERCEPTS  //  TOUCH-FIRST COMMAND UI", skin, "status"),
            ).width(540f * uiScale)
            .row()
        ops.add(menu).padTop(12f * uiScale).row()
        ops.add(Label("PRIMARY DEPLOY CONTROL", skin, "tag")).padTop(4f * uiScale).row()

        body
            .add(hero)
            .width(860f * uiScale)
            .left()
            .padRight(24f * uiScale)
        body.add(ops).width(620f * uiScale).top()
        root
            .add(body)
            .expand()
            .top()
            .left()
            .padTop(84f * uiScale)
            .padLeft(34f * uiScale)
            .row()

        val footer =
            Table().apply {
                background = this@StartScreen.skin.newDrawable("panel_soft", Color(1f, 1f, 1f, 0.9f))
            }
        val footerCopy =
            if (isAndroid) {
                "TAP ENTER AIRSPACE TO DEPLOY // USE SYSTEM BACK TO EXIT"
            } else {
                "TEL AVIV NIGHT REFERENCE + CC0 SKY PANORAMA INTEGRATED INTO THE SCENE"
            }
        footer.add(Label(footerCopy, skin, "tag")).pad(10f * uiScale)
        root
            .add(footer)
            .expandX()
            .fillX()
            .bottom()
            .pad(18f * uiScale)

        stage.addActor(root)
        Gdx.input.inputProcessor = stage
    }

    private fun createSkin(): Skin {
        val s = Skin()
        val uiScale = Gdx.graphics.height / 1080f
        val font = BitmapFont().apply { data.setScale(1.45f * uiScale) }
        val titleFont = BitmapFont().apply { data.setScale(3.65f * uiScale) }
        val statusFont = BitmapFont().apply { data.setScale(1.18f * uiScale) }
        val headlineFont = BitmapFont().apply { data.setScale(1.62f * uiScale) }
        val tagFont = BitmapFont().apply { data.setScale(0.92f * uiScale) }
        s.add("default", font, BitmapFont::class.java)
        s.add("title", titleFont, BitmapFont::class.java)
        s.add("status-font", statusFont, BitmapFont::class.java)
        s.add("headline-font", headlineFont, BitmapFont::class.java)
        s.add("tag-font", tagFont, BitmapFont::class.java)

        val white = Pixmap(2, 2, Pixmap.Format.RGBA8888)
        white.setColor(Color.WHITE)
        white.fill()
        val whiteTexture = Texture(white)
        textures.add(whiteTexture)
        val whiteRegion = TextureRegion(whiteTexture)
        s.add("white", whiteRegion, TextureRegion::class.java)
        s.add("white", TextureRegionDrawable(whiteRegion), Drawable::class.java)
        white.dispose()

        fun addPanel(
            name: String,
            fill: Color,
            stroke: Color,
            accent: Color,
        ) {
            val pixmap = Pixmap(320, 180, Pixmap.Format.RGBA8888)
            pixmap.setColor(fill)
            pixmap.fill()
            pixmap.setColor(accent)
            pixmap.fillRectangle(0, 0, pixmap.width, 18)
            pixmap.setColor(stroke)
            pixmap.drawRectangle(0, 0, pixmap.width, pixmap.height)
            pixmap.drawRectangle(1, 1, pixmap.width - 2, pixmap.height - 2)
            val texture = Texture(pixmap)
            textures.add(texture)
            s.add(name, TextureRegionDrawable(TextureRegion(texture)), Drawable::class.java)
            pixmap.dispose()
        }

        fun addButton(
            name: String,
            top: Color,
            bottom: Color,
            border: Color,
            glow: Color,
        ) {
            val pixmap = Pixmap(280, 104, Pixmap.Format.RGBA8888)
            for (y in 0 until pixmap.height) {
                val t = y / (pixmap.height - 1f)
                pixmap.setColor(
                    MathUtils.lerp(top.r, bottom.r, t),
                    MathUtils.lerp(top.g, bottom.g, t),
                    MathUtils.lerp(top.b, bottom.b, t),
                    MathUtils.lerp(top.a, bottom.a, t),
                )
                pixmap.drawLine(0, y, pixmap.width - 1, y)
            }
            pixmap.setColor(glow)
            pixmap.fillRectangle(6, 6, pixmap.width - 12, 18)
            pixmap.setColor(border)
            pixmap.drawRectangle(0, 0, pixmap.width, pixmap.height)
            pixmap.drawRectangle(1, 1, pixmap.width - 2, pixmap.height - 2)
            pixmap.drawRectangle(2, 2, pixmap.width - 4, pixmap.height - 4)
            val texture = Texture(pixmap)
            textures.add(texture)
            s.add(name, TextureRegion(texture), TextureRegion::class.java)
            s.add(name, TextureRegionDrawable(TextureRegion(texture)), Drawable::class.java)
            pixmap.dispose()
        }

        addButton(
            "btn_up",
            Color(0.03f, 0.08f, 0.16f, 0.96f),
            Color(0.01f, 0.14f, 0.28f, 0.98f),
            Color(0.36f, 0.88f, 1f, 1f),
            Color(0.2f, 0.62f, 0.82f, 0.22f),
        )
        addButton(
            "btn_over",
            Color(0.07f, 0.18f, 0.3f, 0.98f),
            Color(0.02f, 0.28f, 0.46f, 0.98f),
            Color(0.72f, 0.95f, 1f, 1f),
            Color(0.42f, 0.82f, 1f, 0.32f),
        )
        addButton(
            "btn_down",
            Color(0.01f, 0.34f, 0.5f, 0.98f),
            Color(0.0f, 0.22f, 0.38f, 1f),
            Color(0.84f, 0.98f, 1f, 1f),
            Color(0.72f, 0.95f, 1f, 0.22f),
        )
        addButton(
            "btn_disabled",
            Color(0.08f, 0.1f, 0.13f, 0.9f),
            Color(0.05f, 0.07f, 0.1f, 0.92f),
            Color(0.2f, 0.26f, 0.3f, 1f),
            Color(0f, 0f, 0f, 0f),
        )
        addPanel("panel_strong", Color(0.02f, 0.06f, 0.11f, 0.88f), Color(0.24f, 0.78f, 0.94f, 1f), Color(0.12f, 0.54f, 0.72f, 0.95f))
        addPanel("panel_soft", Color(0.02f, 0.05f, 0.1f, 0.7f), Color(0.16f, 0.36f, 0.46f, 1f), Color(0.0f, 0.16f, 0.24f, 0.92f))

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
                downFontColor = Color(0.88f, 0.98f, 1f, 1f)
                overFontColor = Color.WHITE
                checkedFontColor = Color.WHITE
                disabledFontColor = Color(0.58f, 0.66f, 0.72f, 1f)
            },
        )
        s.add("default", Label.LabelStyle(font, Color.WHITE))
        s.add("status", Label.LabelStyle(statusFont, Color(0.68f, 0.95f, 1f, 1f)))
        s.add("headline", Label.LabelStyle(headlineFont, Color(0.94f, 0.98f, 1f, 1f)))
        s.add("tag", Label.LabelStyle(tagFont, Color(0.76f, 0.92f, 1f, 1f)))
        s.add("title", Label.LabelStyle(titleFont, Color.WHITE))
        return s
    }

    private fun loadTexture(
        path: String,
        fallbackColor: Color,
    ): Texture {
        val file = Gdx.files.internal(path)
        if (file.exists()) {
            return Texture(file).also {
                it.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
                textures.add(it)
            }
        }
        val pixmap = Pixmap(4, 4, Pixmap.Format.RGBA8888)
        pixmap.setColor(fallbackColor)
        pixmap.fill()
        return Texture(pixmap).also {
            it.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
            textures.add(it)
            pixmap.dispose()
        }
    }

    override fun render(delta: Float) {
        ScreenUtils.clear(0.01f, 0.02f, 0.04f, 1f)
        stage.act(delta)
        stage.viewport.apply()

        val batch = stage.batch
        val width = Gdx.graphics.width.toFloat()
        val height = Gdx.graphics.height.toFloat()
        scanLineY = (scanLineY + delta * 260f) % height

        batch.begin()
        batch.color = Color.WHITE
        batch.draw(skyTexture, 0f, 0f, width, height)
        batch.setColor(1f, 1f, 1f, 0.78f)
        batch.draw(cityTexture, width * 0.16f, 0f, width * 0.84f, height * 0.62f)
        batch.setColor(0f, 0.18f, 0.32f, 0.22f)
        batch.draw(skin.getRegion("white"), 0f, 0f, width, height * 0.64f)
        batch.setColor(0.08f, 0.4f, 0.62f, 0.12f)
        batch.draw(skin.getRegion("white"), 0f, height * 0.38f, width, height * 0.08f)
        batch.setColor(0.15f, 0.92f, 1f, 0.08f)
        batch.draw(skin.getRegion("white"), 0f, scanLineY, width, 3f)
        batch.draw(skin.getRegion("white"), 0f, (scanLineY + 180f) % height, width, 1.5f)
        batch.setColor(0f, 0f, 0f, 0.28f)
        batch.draw(skin.getRegion("white"), 0f, height - 160f, width, 160f)
        batch.draw(skin.getRegion("white"), 0f, 0f, width, 92f)
        batch.color = Color.WHITE
        batch.end()

        stage.draw()

        if (launchRequested) {
            launchRequested = false
            Gdx.app.log("StartScreen", "Switching to BattleScreen")
            game.screen = BattleScreen(game)
        }
    }

    override fun resize(
        width: Int,
        height: Int,
    ) {
        stage.viewport.update(width, height, true)
    }

    override fun dispose() {
        stage.dispose()
        skin.dispose()
        textures.forEach { it.dispose() }
    }
}
