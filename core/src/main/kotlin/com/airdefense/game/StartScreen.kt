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

private const val START_UI_BASE_HEIGHT = 1080f
private const val HERO_PANEL_PAD = 34f
private const val HERO_HEADLINE_PAD_TOP = 14f
private const val HERO_COPY_PAD_TOP = 18f
private const val HERO_COPY_WIDTH = 760f
private const val HERO_TAG_GAP = 14f
private const val HERO_TAG_PAD_TOP = 20f
private const val MENU_PANEL_PAD = 26f
private const val MENU_BUTTON_WIDTH = 560f
private const val MENU_BUTTON_HEIGHT = 132f
private const val MENU_BUTTON_PAD = 12f
private const val OPS_PANEL_PAD = 28f
private const val OPS_COPY_WIDTH = 540f
private const val OPS_MENU_PAD_TOP = 14f
private const val OPS_TAG_PAD_TOP = 4f
private const val HERO_SECTION_WIDTH = 860f
private const val SCREEN_SECTION_GAP = 24f
private const val OPS_SECTION_WIDTH = 620f
private const val ROOT_TOP_PAD = 84f
private const val ROOT_LEFT_PAD = 34f
private const val FOOTER_LABEL_PAD = 14f
private const val FOOTER_MARGIN = 18f
private const val FALLBACK_TEXTURE_SIZE = 4
private const val SCAN_SPEED = 260f
private const val CITY_DRAW_X = 0.16f
private const val CITY_DRAW_WIDTH = 0.84f
private const val CITY_DRAW_HEIGHT = 0.62f
private const val LOWER_OVERLAY_HEIGHT = 0.64f
private const val HORIZON_Y = 0.38f
private const val HORIZON_HEIGHT = 0.08f
private const val SECOND_SCAN_OFFSET = 180f
private const val PRIMARY_SCAN_HEIGHT = 3f
private const val SECONDARY_SCAN_HEIGHT = 1.5f
private const val TOP_VIGNETTE_HEIGHT = 160f
private const val BOTTOM_VIGNETTE_HEIGHT = 92f

private val START_SKY_FALLBACK_COLOR = Color.valueOf("03050DFF")
private val START_CITY_FALLBACK_COLOR = Color.valueOf("14141FFF")
private val HERO_PANEL_TINT = Color.valueOf("FFFFFFFA")
private val MENU_PANEL_TINT = Color.valueOf("FFFFFFF2")
private val OPS_PANEL_TINT = Color.valueOf("FFFFFFF0")
private val FOOTER_PANEL_TINT = Color.valueOf("FFFFFFEB")
private val START_CLEAR_COLOR = Color.valueOf("03050AFF")
private val CITY_LAYER_COLOR = Color.valueOf("FFFFFFC7")
private val LOWER_OVERLAY_COLOR = Color.valueOf("002E5238")
private val HORIZON_STRIP_COLOR = Color.valueOf("14669E1F")
private val PRIMARY_SCAN_COLOR = Color.valueOf("26EBFF14")
private val VIGNETTE_COLOR = Color.valueOf("00000047")

class StartScreen(
    private val game: AirDefenseGame,
) : ScreenAdapter() {
    private val isAndroid = Gdx.app.type == Application.ApplicationType.Android
    private val stage = Stage(ScreenViewport())
    private val textures = Array<Texture>()
    private val skin = TacticalUiSkin.create(textures, TacticalUiDensity.MENU)
    private val skyTexture = loadTexture("textures/sky_panorama_2k.jpg", START_SKY_FALLBACK_COLOR)
    private val cityTexture = loadTexture("textures/city_backdrop_telaviv.jpg", START_CITY_FALLBACK_COLOR)

    private var scanLineY = 0f
    private var launchRequested = false

    init {
        Gdx.graphics.isContinuousRendering = true
        val uiScale = Gdx.graphics.height / START_UI_BASE_HEIGHT
        val root = Table().apply { setFillParent(true) }
        val body = Table().apply { defaults().top() }
        val hero =
            Table().apply {
                background = this@StartScreen.skin.newDrawable("panel_strong", HERO_PANEL_TINT)
                pad(HERO_PANEL_PAD * uiScale)
                defaults().left()
            }
        hero.add(Label("PROJECT AIR DEFENSE", skin, "title")).row()
        hero.add(Label("COASTAL SHIELD GRID", skin, "headline")).padTop(HERO_HEADLINE_PAD_TOP * uiScale).row()
        hero
            .add(
                Label(
                    "Intercept inbound missiles with clearer tracking and larger phone-first controls.",
                    skin,
                    "status",
                ),
            ).width(HERO_COPY_WIDTH * uiScale)
            .padTop(HERO_COPY_PAD_TOP * uiScale)
            .row()

        val statRow = Table().apply { defaults().padRight(HERO_TAG_GAP * uiScale).padTop(HERO_TAG_PAD_TOP * uiScale) }
        statRow.add(Label("FAST TRACKING", skin, "tag"))
        statRow.add(Label("CLEAR HUD", skin, "tag"))
        statRow.add(Label("PHONE-SIZE CONTROLS", skin, "tag"))
        hero.add(statRow).left().row()

        val menu =
            Table().apply {
                background = this@StartScreen.skin.newDrawable("panel_soft", MENU_PANEL_TINT)
                pad(MENU_PANEL_PAD * uiScale)
                defaults()
                    .width(MENU_BUTTON_WIDTH * uiScale)
                    .height(MENU_BUTTON_HEIGHT * uiScale)
                    .pad(MENU_BUTTON_PAD * uiScale)
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
                background = this@StartScreen.skin.newDrawable("panel_soft", OPS_PANEL_TINT)
                pad(OPS_PANEL_PAD * uiScale)
                defaults().left().padBottom(MENU_BUTTON_PAD * uiScale)
            }
        ops.add(Label("DEPLOYMENT", skin, "headline")).row()
        ops
            .add(
                Label("BALLISTIC THREATS // NIGHT SHIELD BATTLE", skin, "status"),
            ).width(OPS_COPY_WIDTH * uiScale)
            .row()
        ops.add(menu).padTop(OPS_MENU_PAD_TOP * uiScale).row()
        ops.add(Label("PRIMARY ACTION", skin, "tag")).padTop(OPS_TAG_PAD_TOP * uiScale).row()

        body
            .add(hero)
            .width(HERO_SECTION_WIDTH * uiScale)
            .left()
            .padRight(SCREEN_SECTION_GAP * uiScale)
        body.add(ops).width(OPS_SECTION_WIDTH * uiScale).top()
        root
            .add(body)
            .expand()
            .top()
            .left()
            .padTop(ROOT_TOP_PAD * uiScale)
            .padLeft(ROOT_LEFT_PAD * uiScale)
            .row()

        val footer = Table().apply { background = this@StartScreen.skin.newDrawable("panel_soft", FOOTER_PANEL_TINT) }
        val footerCopy =
            if (isAndroid) {
                "TAP ENTER AIRSPACE"
            } else {
                "PRESS ENTER AIRSPACE TO DEPLOY"
            }
        footer.add(Label(footerCopy, skin, "status")).pad(FOOTER_LABEL_PAD * uiScale)
        root
            .add(footer)
            .expandX()
            .fillX()
            .bottom()
            .pad(FOOTER_MARGIN * uiScale)

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
        val pixmap = Pixmap(FALLBACK_TEXTURE_SIZE, FALLBACK_TEXTURE_SIZE, Pixmap.Format.RGBA8888)
        pixmap.setColor(fallbackColor)
        pixmap.fill()
        return Texture(pixmap).also {
            it.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
            textures.add(it)
            pixmap.dispose()
        }
    }

    override fun render(delta: Float) {
        ScreenUtils.clear(START_CLEAR_COLOR)
        stage.act(delta)
        stage.viewport.apply()

        val batch = stage.batch
        val width = Gdx.graphics.width.toFloat()
        val height = Gdx.graphics.height.toFloat()
        scanLineY = (scanLineY + delta * SCAN_SPEED) % height

        batch.begin()
        batch.color = Color.WHITE
        batch.draw(skyTexture, 0f, 0f, width, height)
        batch.color = CITY_LAYER_COLOR
        batch.draw(cityTexture, width * CITY_DRAW_X, 0f, width * CITY_DRAW_WIDTH, height * CITY_DRAW_HEIGHT)
        batch.color = LOWER_OVERLAY_COLOR
        batch.draw(skin.getRegion("white"), 0f, 0f, width, height * LOWER_OVERLAY_HEIGHT)
        batch.color = HORIZON_STRIP_COLOR
        batch.draw(skin.getRegion("white"), 0f, height * HORIZON_Y, width, height * HORIZON_HEIGHT)
        batch.color = PRIMARY_SCAN_COLOR
        batch.draw(skin.getRegion("white"), 0f, scanLineY, width, PRIMARY_SCAN_HEIGHT)
        batch.draw(skin.getRegion("white"), 0f, (scanLineY + SECOND_SCAN_OFFSET) % height, width, SECONDARY_SCAN_HEIGHT)
        batch.color = VIGNETTE_COLOR
        batch.draw(skin.getRegion("white"), 0f, height - TOP_VIGNETTE_HEIGHT, width, TOP_VIGNETTE_HEIGHT)
        batch.draw(skin.getRegion("white"), 0f, 0f, width, BOTTOM_VIGNETTE_HEIGHT)
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
