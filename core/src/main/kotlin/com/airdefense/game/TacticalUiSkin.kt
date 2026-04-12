package com.airdefense.game

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.NinePatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Slider
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Array
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

enum class TacticalUiDensity {
    MENU,
    BATTLE,
}

private data class FontScaleProfile(
    val base: Float,
    val title: Float,
    val status: Float,
    val headline: Float,
    val tag: Float,
    val display: Float,
)

private data class FontSet(
    val default: BitmapFont,
    val title: BitmapFont,
    val status: BitmapFont,
    val headline: BitmapFont,
    val tag: BitmapFont,
    val display: BitmapFont,
)

private data class ColorStops(
    val top: Color,
    val bottom: Color,
)

private data class RectSpec(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val radius: Int,
)

private data class GradientFillSpec(
    val rect: RectSpec,
    val colors: ColorStops,
)

private data class ShadowSpec(
    val rect: RectSpec,
    val color: Color,
    val spread: Int,
)

private data class PanelDrawableSpec(
    val name: String,
    val width: Int,
    val height: Int,
    val radius: Int,
    val patch: Int,
    val top: Color,
    val bottom: Color,
    val border: Color,
    val accent: Color,
)

private data class ButtonDrawableSpec(
    val name: String,
    val width: Int,
    val height: Int,
    val radius: Int,
    val patch: Int,
    val top: Color,
    val bottom: Color,
    val border: Color,
    val highlight: Color,
)

private data class SliderTrackSpec(
    val width: Int,
    val height: Int,
    val outerFill: GradientFillSpec,
    val innerFill: GradientFillSpec,
    val accentFill: GradientFillSpec,
)

private data class SliderKnobSpec(
    val width: Int,
    val height: Int,
    val shadow: ShadowSpec,
    val outerFill: GradientFillSpec,
    val innerFill: GradientFillSpec,
    val shineFill: GradientFillSpec,
)

private const val BASE_UI_HEIGHT = 1080f
private const val MIN_UI_SCALE = 0.96f
private const val WHITE_PIXMAP_SIZE = 4

private const val PANEL_SHADOW_OFFSET_X = 6
private const val PANEL_SHADOW_OFFSET_Y = 6
private const val PANEL_SHADOW_HORIZONTAL_INSET = 12
private const val PANEL_SHADOW_VERTICAL_INSET = 12
private const val PANEL_SHADOW_SPREAD = 6
private const val PANEL_INNER_INSET = 2
private const val PANEL_RADIUS_INSET = 2
private const val PANEL_MIN_INNER_RADIUS = 6
private const val PANEL_ACCENT_HORIZONTAL_INSET = 14
private const val PANEL_ACCENT_BOTTOM_INSET = 12
private const val PANEL_ACCENT_MIN_HEIGHT = 12
private const val PANEL_ACCENT_HEIGHT_RATIO = 0.16f
private const val PANEL_ACCENT_BOTTOM_ALPHA_SCALE = 0.16f

private const val BUTTON_SHADOW_OFFSET_X = 8
private const val BUTTON_SHADOW_OFFSET_Y = 10
private const val BUTTON_SHADOW_HORIZONTAL_INSET = 16
private const val BUTTON_SHADOW_VERTICAL_INSET = 18
private const val BUTTON_SHADOW_SPREAD = 8
private const val BUTTON_INNER_INSET = 2
private const val BUTTON_RADIUS_INSET = 2
private const val BUTTON_MIN_INNER_RADIUS = 6
private const val BUTTON_HIGHLIGHT_INSET_X = 12
private const val BUTTON_HIGHLIGHT_BOTTOM_OFFSET = 40
private const val BUTTON_HIGHLIGHT_HORIZONTAL_INSET = 24
private const val BUTTON_HIGHLIGHT_HEIGHT = 24
private const val BUTTON_HIGHLIGHT_RADIUS = 12
private const val BUTTON_HIGHLIGHT_BOTTOM_ALPHA = 0.02f
private const val BUTTON_GLOSS_INSET_X = 14
private const val BUTTON_GLOSS_INSET_Y = 14
private const val BUTTON_GLOSS_HORIZONTAL_INSET = 28
private const val BUTTON_GLOSS_HEIGHT = 18
private const val BUTTON_GLOSS_RADIUS = 10

private const val SLIDER_TRACK_WIDTH = 320
private const val SLIDER_TRACK_HEIGHT = 36
private const val SLIDER_TRACK_OUTER_RADIUS = 18
private const val SLIDER_TRACK_INNER_INSET = 2
private const val SLIDER_TRACK_INNER_RADIUS = 16
private const val SLIDER_TRACK_ACCENT_INSET_X = 10
private const val SLIDER_TRACK_ACCENT_INSET_Y = 8
private const val SLIDER_TRACK_ACCENT_HEIGHT = 10
private const val SLIDER_TRACK_ACCENT_RADIUS = 5

private const val SLIDER_KNOB_SIZE = 72
private const val SLIDER_KNOB_SHADOW_X = 10
private const val SLIDER_KNOB_SHADOW_Y = 8
private const val SLIDER_KNOB_SHADOW_SIZE = 52
private const val SLIDER_KNOB_SHADOW_RADIUS = 26
private const val SLIDER_KNOB_SHADOW_SPREAD = 6
private const val SLIDER_KNOB_OUTER_X = 8
private const val SLIDER_KNOB_OUTER_Y = 10
private const val SLIDER_KNOB_OUTER_SIZE = 56
private const val SLIDER_KNOB_OUTER_RADIUS = 28
private const val SLIDER_KNOB_INNER_X = 10
private const val SLIDER_KNOB_INNER_Y = 12
private const val SLIDER_KNOB_INNER_SIZE = 52
private const val SLIDER_KNOB_INNER_RADIUS = 26
private const val SLIDER_KNOB_SHINE_X = 18
private const val SLIDER_KNOB_SHINE_Y = 40
private const val SLIDER_KNOB_SHINE_WIDTH = 36
private const val SLIDER_KNOB_SHINE_HEIGHT = 10
private const val SLIDER_KNOB_SHINE_RADIUS = 5
private const val SHADOW_ALPHA_FALLOFF = 0.45f
private const val SHADOW_BOTTOM_ALPHA_SCALE = 0.7f
private const val SHADOW_MIN_RADIUS = 4
private const val SHADOW_X_SHIFT_DIVISOR = 2
private const val SHADOW_Y_SHIFT_DIVISOR = 3

private const val STANDARD_PANEL_WIDTH = 224
private const val STANDARD_PANEL_HEIGHT = 148
private const val STANDARD_PANEL_RADIUS = 28
private const val STANDARD_PANEL_PATCH = 34
private const val HUD_PANEL_WIDTH = 240
private const val HUD_PANEL_HEIGHT = 168
private const val HUD_PANEL_RADIUS = 30
private const val HUD_PANEL_PATCH = 36
private const val HUD_SOFT_HEIGHT = 152

private const val BUTTON_WIDTH = 240
private const val BUTTON_HEIGHT = 112
private const val BUTTON_RADIUS = 34
private const val BUTTON_PATCH = 40

private const val SLIDER_TRACK_PATCH_HORIZONTAL = 20
private const val SLIDER_TRACK_PATCH_VERTICAL = 10

private val MENU_FONT_SCALES =
    FontScaleProfile(
        base = 1.5f,
        title = 3.62f,
        status = 1.18f,
        headline = 1.74f,
        tag = 1.02f,
        display = 1.58f,
    )

private val BATTLE_FONT_SCALES =
    FontScaleProfile(
        base = 1.24f,
        title = 1.74f,
        status = 1.06f,
        headline = 1.36f,
        tag = 0.94f,
        display = 1.48f,
    )

private val PANEL_SHADOW_COLOR = Color.valueOf("00000038")
private val BUTTON_SHADOW_COLOR = Color.valueOf("0000004C")
private val BUTTON_GLOSS_TOP_COLOR = Color.valueOf("0000001F")
private val BUTTON_GLOSS_BOTTOM_COLOR = Color.valueOf("00000000")

private val PANEL_STRONG_TOP_COLOR = Color.valueOf("081729EB")
private val PANEL_STRONG_BOTTOM_COLOR = Color.valueOf("030D1AF0")
private val PANEL_STRONG_BORDER_COLOR = Color.valueOf("5CDBFFFA")
private val PANEL_STRONG_ACCENT_COLOR = Color.valueOf("1A94D166")

private val PANEL_SOFT_TOP_COLOR = Color.valueOf("051224B8")
private val PANEL_SOFT_BOTTOM_COLOR = Color.valueOf("030A14C2")
private val PANEL_SOFT_BORDER_COLOR = Color.valueOf("2E6B8AEB")
private val PANEL_SOFT_ACCENT_COLOR = Color.valueOf("0F4C6B47")

private val HUD_PANEL_TOP_COLOR = Color.valueOf("051424D6")
private val HUD_PANEL_BOTTOM_COLOR = Color.valueOf("030D1AE0")
private val HUD_PANEL_BORDER_COLOR = Color.valueOf("4CC7F0F0")
private val HUD_PANEL_ACCENT_COLOR = Color.valueOf("249ED652")

private val HUD_SOFT_TOP_COLOR = Color.valueOf("040D1A9E")
private val HUD_SOFT_BOTTOM_COLOR = Color.valueOf("030812AD")
private val HUD_SOFT_BORDER_COLOR = Color.valueOf("295775E6")
private val HUD_SOFT_ACCENT_COLOR = Color.valueOf("143D573D")

private val BUTTON_UP_TOP_COLOR = Color.valueOf("0F3357FA")
private val BUTTON_UP_BOTTOM_COLOR = Color.valueOf("051C33FC")
private val BUTTON_UP_BORDER_COLOR = Color.valueOf("A8F0FFFF")
private val BUTTON_UP_HIGHLIGHT_COLOR = Color.valueOf("85E6FF42")

private val BUTTON_OVER_TOP_COLOR = Color.valueOf("1A4C75FF")
private val BUTTON_OVER_BOTTOM_COLOR = Color.valueOf("0A2E4FFF")
private val BUTTON_OVER_BORDER_COLOR = Color.valueOf("DBFCFFFF")
private val BUTTON_OVER_HIGHLIGHT_COLOR = Color.valueOf("9EF0FF57")

private val BUTTON_DOWN_TOP_COLOR = Color.valueOf("052947FF")
private val BUTTON_DOWN_BOTTOM_COLOR = Color.valueOf("00172EFF")
private val BUTTON_DOWN_BORDER_COLOR = Color.valueOf("E0FCFFFF")
private val BUTTON_DOWN_HIGHLIGHT_COLOR = Color.valueOf("38A8E62E")

private val BUTTON_DISABLED_TOP_COLOR = Color.valueOf("141A24E6")
private val BUTTON_DISABLED_BOTTOM_COLOR = Color.valueOf("0A0F1AEB")
private val BUTTON_DISABLED_BORDER_COLOR = Color.valueOf("2E3D4CFF")

private val SLIDER_TRACK_OUTER_COLOR = Color.valueOf("47C7F0FF")
private val SLIDER_TRACK_INNER_TOP_COLOR = Color.valueOf("0A1F33FF")
private val SLIDER_TRACK_INNER_BOTTOM_COLOR = Color.valueOf("051424FF")
private val SLIDER_TRACK_ACCENT_TOP_COLOR = Color.valueOf("33B8E042")
private val SLIDER_TRACK_ACCENT_BOTTOM_COLOR = Color.valueOf("1F5C8514")

private val SLIDER_KNOB_SHADOW_COLOR = Color.valueOf("00000047")
private val SLIDER_KNOB_OUTER_COLOR = Color.valueOf("F0FCFFFF")
private val SLIDER_KNOB_INNER_TOP_COLOR = Color.valueOf("B2F0FFFF")
private val SLIDER_KNOB_INNER_BOTTOM_COLOR = Color.valueOf("47B8EBFF")
private val SLIDER_KNOB_SHINE_TOP_COLOR = Color.valueOf("FFFFFF61")

private val PANEL_DRAWABLE_SPECS =
    listOf(
        PanelDrawableSpec(
            name = "panel_strong",
            width = STANDARD_PANEL_WIDTH,
            height = STANDARD_PANEL_HEIGHT,
            radius = STANDARD_PANEL_RADIUS,
            patch = STANDARD_PANEL_PATCH,
            top = PANEL_STRONG_TOP_COLOR,
            bottom = PANEL_STRONG_BOTTOM_COLOR,
            border = PANEL_STRONG_BORDER_COLOR,
            accent = PANEL_STRONG_ACCENT_COLOR,
        ),
        PanelDrawableSpec(
            name = "panel_soft",
            width = STANDARD_PANEL_WIDTH,
            height = STANDARD_PANEL_HEIGHT,
            radius = STANDARD_PANEL_RADIUS,
            patch = STANDARD_PANEL_PATCH,
            top = PANEL_SOFT_TOP_COLOR,
            bottom = PANEL_SOFT_BOTTOM_COLOR,
            border = PANEL_SOFT_BORDER_COLOR,
            accent = PANEL_SOFT_ACCENT_COLOR,
        ),
        PanelDrawableSpec(
            name = "hud_panel",
            width = HUD_PANEL_WIDTH,
            height = HUD_PANEL_HEIGHT,
            radius = HUD_PANEL_RADIUS,
            patch = HUD_PANEL_PATCH,
            top = HUD_PANEL_TOP_COLOR,
            bottom = HUD_PANEL_BOTTOM_COLOR,
            border = HUD_PANEL_BORDER_COLOR,
            accent = HUD_PANEL_ACCENT_COLOR,
        ),
        PanelDrawableSpec(
            name = "hud_soft",
            width = STANDARD_PANEL_WIDTH,
            height = HUD_SOFT_HEIGHT,
            radius = STANDARD_PANEL_RADIUS,
            patch = STANDARD_PANEL_PATCH,
            top = HUD_SOFT_TOP_COLOR,
            bottom = HUD_SOFT_BOTTOM_COLOR,
            border = HUD_SOFT_BORDER_COLOR,
            accent = HUD_SOFT_ACCENT_COLOR,
        ),
    )

private val BUTTON_DRAWABLE_SPECS =
    listOf(
        ButtonDrawableSpec(
            name = "btn_up",
            width = BUTTON_WIDTH,
            height = BUTTON_HEIGHT,
            radius = BUTTON_RADIUS,
            patch = BUTTON_PATCH,
            top = BUTTON_UP_TOP_COLOR,
            bottom = BUTTON_UP_BOTTOM_COLOR,
            border = BUTTON_UP_BORDER_COLOR,
            highlight = BUTTON_UP_HIGHLIGHT_COLOR,
        ),
        ButtonDrawableSpec(
            name = "btn_over",
            width = BUTTON_WIDTH,
            height = BUTTON_HEIGHT,
            radius = BUTTON_RADIUS,
            patch = BUTTON_PATCH,
            top = BUTTON_OVER_TOP_COLOR,
            bottom = BUTTON_OVER_BOTTOM_COLOR,
            border = BUTTON_OVER_BORDER_COLOR,
            highlight = BUTTON_OVER_HIGHLIGHT_COLOR,
        ),
        ButtonDrawableSpec(
            name = "btn_down",
            width = BUTTON_WIDTH,
            height = BUTTON_HEIGHT,
            radius = BUTTON_RADIUS,
            patch = BUTTON_PATCH,
            top = BUTTON_DOWN_TOP_COLOR,
            bottom = BUTTON_DOWN_BOTTOM_COLOR,
            border = BUTTON_DOWN_BORDER_COLOR,
            highlight = BUTTON_DOWN_HIGHLIGHT_COLOR,
        ),
        ButtonDrawableSpec(
            name = "btn_disabled",
            width = BUTTON_WIDTH,
            height = BUTTON_HEIGHT,
            radius = BUTTON_RADIUS,
            patch = BUTTON_PATCH,
            top = BUTTON_DISABLED_TOP_COLOR,
            bottom = BUTTON_DISABLED_BOTTOM_COLOR,
            border = BUTTON_DISABLED_BORDER_COLOR,
            highlight = BUTTON_GLOSS_BOTTOM_COLOR,
        ),
    )

private val SLIDER_TRACK_SPEC =
    SliderTrackSpec(
        width = SLIDER_TRACK_WIDTH,
        height = SLIDER_TRACK_HEIGHT,
        outerFill =
            GradientFillSpec(
                rect = RectSpec(0, 0, SLIDER_TRACK_WIDTH, SLIDER_TRACK_HEIGHT, SLIDER_TRACK_OUTER_RADIUS),
                colors = ColorStops(SLIDER_TRACK_OUTER_COLOR, SLIDER_TRACK_OUTER_COLOR),
            ),
        innerFill =
            GradientFillSpec(
                rect =
                    RectSpec(
                        SLIDER_TRACK_INNER_INSET,
                        SLIDER_TRACK_INNER_INSET,
                        SLIDER_TRACK_WIDTH - SLIDER_TRACK_INNER_INSET * 2,
                        SLIDER_TRACK_HEIGHT - SLIDER_TRACK_INNER_INSET * 2,
                        SLIDER_TRACK_INNER_RADIUS,
                    ),
                colors = ColorStops(SLIDER_TRACK_INNER_TOP_COLOR, SLIDER_TRACK_INNER_BOTTOM_COLOR),
            ),
        accentFill =
            GradientFillSpec(
                rect =
                    RectSpec(
                        SLIDER_TRACK_ACCENT_INSET_X,
                        SLIDER_TRACK_ACCENT_INSET_Y,
                        SLIDER_TRACK_WIDTH - SLIDER_TRACK_ACCENT_INSET_X * 2,
                        SLIDER_TRACK_ACCENT_HEIGHT,
                        SLIDER_TRACK_ACCENT_RADIUS,
                    ),
                colors = ColorStops(SLIDER_TRACK_ACCENT_TOP_COLOR, SLIDER_TRACK_ACCENT_BOTTOM_COLOR),
            ),
    )

private val SLIDER_KNOB_SPEC =
    SliderKnobSpec(
        width = SLIDER_KNOB_SIZE,
        height = SLIDER_KNOB_SIZE,
        shadow =
            ShadowSpec(
                rect =
                    RectSpec(
                        SLIDER_KNOB_SHADOW_X,
                        SLIDER_KNOB_SHADOW_Y,
                        SLIDER_KNOB_SHADOW_SIZE,
                        SLIDER_KNOB_SHADOW_SIZE,
                        SLIDER_KNOB_SHADOW_RADIUS,
                    ),
                color = SLIDER_KNOB_SHADOW_COLOR,
                spread = SLIDER_KNOB_SHADOW_SPREAD,
            ),
        outerFill =
            GradientFillSpec(
                rect =
                    RectSpec(
                        SLIDER_KNOB_OUTER_X,
                        SLIDER_KNOB_OUTER_Y,
                        SLIDER_KNOB_OUTER_SIZE,
                        SLIDER_KNOB_OUTER_SIZE,
                        SLIDER_KNOB_OUTER_RADIUS,
                    ),
                colors = ColorStops(SLIDER_KNOB_OUTER_COLOR, SLIDER_KNOB_OUTER_COLOR),
            ),
        innerFill =
            GradientFillSpec(
                rect =
                    RectSpec(
                        SLIDER_KNOB_INNER_X,
                        SLIDER_KNOB_INNER_Y,
                        SLIDER_KNOB_INNER_SIZE,
                        SLIDER_KNOB_INNER_SIZE,
                        SLIDER_KNOB_INNER_RADIUS,
                    ),
                colors = ColorStops(SLIDER_KNOB_INNER_TOP_COLOR, SLIDER_KNOB_INNER_BOTTOM_COLOR),
            ),
        shineFill =
            GradientFillSpec(
                rect =
                    RectSpec(
                        SLIDER_KNOB_SHINE_X,
                        SLIDER_KNOB_SHINE_Y,
                        SLIDER_KNOB_SHINE_WIDTH,
                        SLIDER_KNOB_SHINE_HEIGHT,
                        SLIDER_KNOB_SHINE_RADIUS,
                    ),
                colors =
                    ColorStops(
                        SLIDER_KNOB_SHINE_TOP_COLOR,
                        Color.valueOf("FFFFFF0F"),
                    ),
            ),
    )

private val BUTTON_DOWN_FONT_COLOR = Color.valueOf("EBFCFFFF")
private val BUTTON_DISABLED_FONT_COLOR = Color.valueOf("8A9EB2FF")
private val DISPLAY_LABEL_COLOR = Color.valueOf("F0FAFFFF")
private val STATUS_LABEL_COLOR = Color.valueOf("B2F5FFFF")
private val TAG_LABEL_COLOR = Color.valueOf("CCF0FFFF")
private val WARNING_LABEL_COLOR = Color.valueOf("FFD66BFF")
private val CRITICAL_LABEL_COLOR = Color.valueOf("FF6B6BFF")

object TacticalUiSkin {
    fun create(
        textures: Array<Texture>,
        density: TacticalUiDensity,
    ): Skin {
        val skin = Skin()
        val registerPatchDrawable =
            { name: String, pixmap: Pixmap, patch: Int ->
                val texture = textureFromPixmap(textures, pixmap)
                val ninePatch = NinePatch(TextureRegion(texture), patch, patch, patch, patch)
                skin.add(name, NinePatchDrawable(ninePatch), Drawable::class.java)
            }
        val uiScale = max(MIN_UI_SCALE, Gdx.graphics.height / BASE_UI_HEIGHT)
        val fontProfile =
            when (density) {
                TacticalUiDensity.MENU -> MENU_FONT_SCALES
                TacticalUiDensity.BATTLE -> BATTLE_FONT_SCALES
            }
        val fonts = registerFonts(skin, uiScale, fontProfile)
        registerWhiteDrawables(skin, textures)
        PANEL_DRAWABLE_SPECS.forEach { spec ->
            registerPatchDrawable(spec.name, createPanelPixmap(spec), spec.patch)
        }
        BUTTON_DRAWABLE_SPECS.forEach { spec ->
            registerPatchDrawable(spec.name, createButtonPixmap(spec), spec.patch)
        }
        registerSliderStyle(skin, textures)
        registerTextStyles(skin, fonts)
        return skin
    }
}

private fun registerFonts(
    skin: Skin,
    uiScale: Float,
    fontScale: FontScaleProfile,
): FontSet {
    val fontSet =
        FontSet(
            default = BitmapFont().apply { data.setScale(fontScale.base * uiScale) },
            title = BitmapFont().apply { data.setScale(fontScale.title * uiScale) },
            status = BitmapFont().apply { data.setScale(fontScale.status * uiScale) },
            headline = BitmapFont().apply { data.setScale(fontScale.headline * uiScale) },
            tag = BitmapFont().apply { data.setScale(fontScale.tag * uiScale) },
            display = BitmapFont().apply { data.setScale(fontScale.display * uiScale) },
        )
    skin.add("default", fontSet.default, BitmapFont::class.java)
    skin.add("title-font", fontSet.title, BitmapFont::class.java)
    skin.add("status-font", fontSet.status, BitmapFont::class.java)
    skin.add("headline-font", fontSet.headline, BitmapFont::class.java)
    skin.add("tag-font", fontSet.tag, BitmapFont::class.java)
    skin.add("display-font", fontSet.display, BitmapFont::class.java)
    return fontSet
}

private fun registerWhiteDrawables(
    skin: Skin,
    textures: Array<Texture>,
) {
    val whitePixmap =
        Pixmap(WHITE_PIXMAP_SIZE, WHITE_PIXMAP_SIZE, Pixmap.Format.RGBA8888).apply {
            setColor(Color.WHITE)
            fill()
        }
    val whiteTexture =
        Texture(whitePixmap).also { texture ->
            texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        }
    textures.add(whiteTexture)
    val whiteRegion = TextureRegion(whiteTexture)
    skin.add("white", whiteRegion, TextureRegion::class.java)
    skin.add("white_region", whiteRegion, TextureRegion::class.java)
    skin.add("white", TextureRegionDrawable(whiteRegion), Drawable::class.java)
    whitePixmap.dispose()
}

private fun registerSliderStyle(
    skin: Skin,
    textures: Array<Texture>,
) {
    val sliderTrackTexture =
        textureFromPixmap(
            textures,
            Pixmap(SLIDER_TRACK_SPEC.width, SLIDER_TRACK_SPEC.height, Pixmap.Format.RGBA8888).apply {
                fillRoundedGradient(this, SLIDER_TRACK_SPEC.outerFill)
                fillRoundedGradient(this, SLIDER_TRACK_SPEC.innerFill)
                fillRoundedGradient(this, SLIDER_TRACK_SPEC.accentFill)
            },
        )
    val sliderKnobTexture =
        textureFromPixmap(
            textures,
            Pixmap(SLIDER_KNOB_SPEC.width, SLIDER_KNOB_SPEC.height, Pixmap.Format.RGBA8888).apply {
                drawRoundedShadow(this, SLIDER_KNOB_SPEC.shadow)
                fillRoundedGradient(this, SLIDER_KNOB_SPEC.outerFill)
                fillRoundedGradient(this, SLIDER_KNOB_SPEC.innerFill)
                fillRoundedGradient(this, SLIDER_KNOB_SPEC.shineFill)
            },
        )
    skin.add(
        "default-horizontal",
        Slider.SliderStyle(
            NinePatchDrawable(
                NinePatch(
                    TextureRegion(sliderTrackTexture),
                    SLIDER_TRACK_PATCH_HORIZONTAL,
                    SLIDER_TRACK_PATCH_HORIZONTAL,
                    SLIDER_TRACK_PATCH_VERTICAL,
                    SLIDER_TRACK_PATCH_VERTICAL,
                ),
            ),
            TextureRegionDrawable(TextureRegion(sliderKnobTexture)),
        ),
    )
}

private fun registerTextStyles(
    skin: Skin,
    fonts: FontSet,
) {
    skin.add(
        "default",
        TextButton.TextButtonStyle().apply {
            up = skin.getDrawable("btn_up")
            checked = skin.getDrawable("btn_down")
            down = skin.getDrawable("btn_down")
            over = skin.getDrawable("btn_over")
            disabled = skin.getDrawable("btn_disabled")
            font = fonts.default
            fontColor = Color.WHITE.cpy()
            overFontColor = Color.WHITE.cpy()
            downFontColor = BUTTON_DOWN_FONT_COLOR.cpy()
            checkedFontColor = Color.WHITE.cpy()
            disabledFontColor = BUTTON_DISABLED_FONT_COLOR.cpy()
        },
    )
    skin.add("default", Label.LabelStyle(fonts.default, Color.WHITE.cpy()))
    skin.add("title", Label.LabelStyle(fonts.title, Color.WHITE.cpy()))
    skin.add("display", Label.LabelStyle(fonts.display, DISPLAY_LABEL_COLOR.cpy()))
    skin.add("status", Label.LabelStyle(fonts.status, STATUS_LABEL_COLOR.cpy()))
    skin.add("headline", Label.LabelStyle(fonts.headline, DISPLAY_LABEL_COLOR.cpy()))
    skin.add("tag", Label.LabelStyle(fonts.tag, TAG_LABEL_COLOR.cpy()))
    skin.add("warning", Label.LabelStyle(fonts.headline, WARNING_LABEL_COLOR.cpy()))
    skin.add("critical", Label.LabelStyle(fonts.headline, CRITICAL_LABEL_COLOR.cpy()))
}

private fun textureFromPixmap(
    textures: Array<Texture>,
    pixmap: Pixmap,
): Texture =
    Texture(pixmap).also { texture ->
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        textures.add(texture)
        pixmap.dispose()
    }

private fun createPanelPixmap(spec: PanelDrawableSpec): Pixmap {
    val pixmap = Pixmap(spec.width, spec.height, Pixmap.Format.RGBA8888)
    val shadowRect =
        RectSpec(
            PANEL_SHADOW_OFFSET_X,
            PANEL_SHADOW_OFFSET_Y,
            spec.width - PANEL_SHADOW_HORIZONTAL_INSET,
            spec.height - PANEL_SHADOW_VERTICAL_INSET,
            spec.radius,
        )
    val innerRect =
        RectSpec(
            PANEL_INNER_INSET,
            PANEL_INNER_INSET,
            spec.width - PANEL_INNER_INSET * 2,
            spec.height - PANEL_INNER_INSET * 2,
            max(PANEL_MIN_INNER_RADIUS, spec.radius - PANEL_RADIUS_INSET),
        )
    drawRoundedShadow(pixmap, ShadowSpec(shadowRect, PANEL_SHADOW_COLOR, PANEL_SHADOW_SPREAD))
    fillRoundedGradient(
        pixmap,
        GradientFillSpec(
            RectSpec(0, 0, spec.width, spec.height, spec.radius),
            ColorStops(spec.border, spec.border),
        ),
    )
    fillRoundedGradient(pixmap, GradientFillSpec(innerRect, ColorStops(spec.top, spec.bottom)))
    val accentHeight = max(PANEL_ACCENT_MIN_HEIGHT, (spec.height * PANEL_ACCENT_HEIGHT_RATIO).roundToInt())
    val accentRect =
        RectSpec(
            PANEL_ACCENT_HORIZONTAL_INSET,
            spec.height - accentHeight - PANEL_ACCENT_BOTTOM_INSET,
            spec.width - PANEL_ACCENT_HORIZONTAL_INSET * 2,
            accentHeight,
            min(spec.radius / 2, accentHeight / 2),
        )
    val accentBottom = spec.accent.cpy().apply { a *= PANEL_ACCENT_BOTTOM_ALPHA_SCALE }
    fillRoundedGradient(pixmap, GradientFillSpec(accentRect, ColorStops(spec.accent, accentBottom)))
    return pixmap
}

private fun createButtonPixmap(spec: ButtonDrawableSpec): Pixmap {
    val pixmap = Pixmap(spec.width, spec.height, Pixmap.Format.RGBA8888)
    val shadowRect =
        RectSpec(
            BUTTON_SHADOW_OFFSET_X,
            BUTTON_SHADOW_OFFSET_Y,
            spec.width - BUTTON_SHADOW_HORIZONTAL_INSET,
            spec.height - BUTTON_SHADOW_VERTICAL_INSET,
            spec.radius,
        )
    val innerRect =
        RectSpec(
            BUTTON_INNER_INSET,
            BUTTON_INNER_INSET,
            spec.width - BUTTON_INNER_INSET * 2,
            spec.height - BUTTON_INNER_INSET * 2,
            max(BUTTON_MIN_INNER_RADIUS, spec.radius - BUTTON_RADIUS_INSET),
        )
    val highlightRect =
        RectSpec(
            BUTTON_HIGHLIGHT_INSET_X,
            spec.height - BUTTON_HIGHLIGHT_BOTTOM_OFFSET,
            spec.width - BUTTON_HIGHLIGHT_HORIZONTAL_INSET,
            BUTTON_HIGHLIGHT_HEIGHT,
            BUTTON_HIGHLIGHT_RADIUS,
        )
    val glossRect =
        RectSpec(
            BUTTON_GLOSS_INSET_X,
            BUTTON_GLOSS_INSET_Y,
            spec.width - BUTTON_GLOSS_HORIZONTAL_INSET,
            BUTTON_GLOSS_HEIGHT,
            BUTTON_GLOSS_RADIUS,
        )
    val highlightBottom = spec.highlight.cpy().apply { a = BUTTON_HIGHLIGHT_BOTTOM_ALPHA }
    drawRoundedShadow(pixmap, ShadowSpec(shadowRect, BUTTON_SHADOW_COLOR, BUTTON_SHADOW_SPREAD))
    fillRoundedGradient(
        pixmap,
        GradientFillSpec(
            RectSpec(0, 0, spec.width, spec.height, spec.radius),
            ColorStops(spec.border, spec.border),
        ),
    )
    fillRoundedGradient(pixmap, GradientFillSpec(innerRect, ColorStops(spec.top, spec.bottom)))
    fillRoundedGradient(pixmap, GradientFillSpec(highlightRect, ColorStops(spec.highlight, highlightBottom)))
    fillRoundedGradient(
        pixmap,
        GradientFillSpec(glossRect, ColorStops(BUTTON_GLOSS_TOP_COLOR, BUTTON_GLOSS_BOTTOM_COLOR)),
    )
    return pixmap
}

private fun drawRoundedShadow(
    pixmap: Pixmap,
    shadow: ShadowSpec,
) {
    for (offset in shadow.spread downTo 1) {
        val alpha = shadow.color.a * (offset.toFloat() / shadow.spread.toFloat()) * SHADOW_ALPHA_FALLOFF
        fillRoundedGradient(
            pixmap,
            GradientFillSpec(
                rect =
                    RectSpec(
                        x = shadow.rect.x + offset / SHADOW_X_SHIFT_DIVISOR,
                        y = shadow.rect.y - offset / SHADOW_Y_SHIFT_DIVISOR,
                        width = shadow.rect.width,
                        height = shadow.rect.height,
                        radius = max(SHADOW_MIN_RADIUS, shadow.rect.radius - offset / SHADOW_X_SHIFT_DIVISOR),
                    ),
                colors =
                    ColorStops(
                        Color(shadow.color.r, shadow.color.g, shadow.color.b, alpha),
                        Color(shadow.color.r, shadow.color.g, shadow.color.b, alpha * SHADOW_BOTTOM_ALPHA_SCALE),
                    ),
            ),
        )
    }
}

private fun fillRoundedGradient(
    pixmap: Pixmap,
    fill: GradientFillSpec,
) {
    for (row in 0 until fill.rect.height) {
        val mix = row / (fill.rect.height - 1f).coerceAtLeast(1f)
        val color =
            Color(
                MathUtils.lerp(fill.colors.top.r, fill.colors.bottom.r, mix),
                MathUtils.lerp(fill.colors.top.g, fill.colors.bottom.g, mix),
                MathUtils.lerp(fill.colors.top.b, fill.colors.bottom.b, mix),
                MathUtils.lerp(fill.colors.top.a, fill.colors.bottom.a, mix),
            )
        pixmap.setColor(color)
        val inset = roundedInset(row, fill.rect.height, fill.rect.radius)
        val lineX = fill.rect.x + inset
        val lineWidth = fill.rect.width - inset * 2
        if (lineWidth > 0) {
            pixmap.drawLine(lineX, fill.rect.y + row, lineX + lineWidth - 1, fill.rect.y + row)
        }
    }
}

private fun roundedInset(
    row: Int,
    height: Int,
    radius: Int,
): Int {
    val safeRadius = radius.coerceAtLeast(0)
    val distanceFromBottom = height - 1 - row
    val edgeDistance =
        when {
            safeRadius == 0 -> 0
            row < safeRadius -> safeRadius - row - 1
            distanceFromBottom < safeRadius -> safeRadius - distanceFromBottom - 1
            else -> 0
        }
    if (edgeDistance <= 0) return 0
    val vertical = edgeDistance.coerceIn(0, safeRadius)
    val horizontal = sqrt((safeRadius * safeRadius - vertical * vertical).toDouble()).toInt()
    return max(0, safeRadius - horizontal)
}
