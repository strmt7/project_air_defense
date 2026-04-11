package com.airdefense.game

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.viewport.ScreenViewport

class StartScreen(
    private val game: AirDefenseGame,
) : ScreenAdapter() {
    private val isAndroid = Gdx.app.type == Application.ApplicationType.Android
    private val stage = Stage(ScreenViewport())
    private val textures = Array<Texture>()
    private val skin = TacticalUiSkin.create(textures, TacticalUiDensity.MENU)
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
                background = this@StartScreen.skin.newDrawable("panel_strong", Color(1f, 1f, 1f, 0.98f))
                pad(34f * uiScale)
                defaults().left()
            }
        hero.add(Label("PROJECT AIR DEFENSE", skin, "title")).row()
        hero.add(Label("COASTAL SHIELD GRID", skin, "headline")).padTop(14f * uiScale).row()
        hero
            .add(
                Label(
                    "Intercept inbound missiles with clearer tracking and larger phone-first controls.",
                    skin,
                    "status",
                ),
            ).width(760f * uiScale)
            .padTop(18f * uiScale)
            .row()

        val statRow = Table().apply { defaults().padRight(14f * uiScale).padTop(20f * uiScale) }
        statRow.add(Label("FAST TRACKING", skin, "tag"))
        statRow.add(Label("CLEAR HUD", skin, "tag"))
        statRow.add(Label("PHONE-SIZE CONTROLS", skin, "tag"))
        hero.add(statRow).left().row()

        val menu =
            Table().apply {
                background = this@StartScreen.skin.newDrawable("panel_soft", Color(1f, 1f, 1f, 0.95f))
                pad(26f * uiScale)
                defaults().width(560f * uiScale).height(132f * uiScale).pad(12f * uiScale)
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
                background = this@StartScreen.skin.newDrawable("panel_soft", Color(1f, 1f, 1f, 0.94f))
                pad(28f * uiScale)
                defaults().left().padBottom(12f * uiScale)
            }
        ops.add(Label("DEPLOYMENT", skin, "headline")).row()
        ops
            .add(
                Label("BALLISTIC THREATS // NIGHT SHIELD BATTLE", skin, "status"),
            ).width(540f * uiScale)
            .row()
        ops.add(menu).padTop(14f * uiScale).row()
        ops.add(Label("PRIMARY ACTION", skin, "tag")).padTop(4f * uiScale).row()

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

        val footer = Table().apply { background = this@StartScreen.skin.newDrawable("panel_soft", Color(1f, 1f, 1f, 0.92f)) }
        val footerCopy =
            if (isAndroid) {
                "TAP ENTER AIRSPACE"
            } else {
                "PRESS ENTER AIRSPACE TO DEPLOY"
            }
        footer.add(Label(footerCopy, skin, "status")).pad(14f * uiScale)
        root
            .add(footer)
            .expandX()
            .fillX()
            .bottom()
            .pad(18f * uiScale)

        stage.addActor(root)
        Gdx.input.inputProcessor = stage
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
