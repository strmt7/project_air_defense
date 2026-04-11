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

object TacticalUiSkin {
    fun create(
        textures: Array<Texture>,
        density: TacticalUiDensity,
    ): Skin {
        val skin = Skin()
        val uiScale = max(0.96f, Gdx.graphics.height / 1080f)
        val fontScale =
            when (density) {
                TacticalUiDensity.MENU -> {
                    FontScaleProfile(
                        base = 1.5f,
                        title = 3.62f,
                        status = 1.18f,
                        headline = 1.74f,
                        tag = 1.02f,
                        display = 1.58f,
                    )
                }

                TacticalUiDensity.BATTLE -> {
                    FontScaleProfile(
                        base = 1.24f,
                        title = 1.74f,
                        status = 1.06f,
                        headline = 1.36f,
                        tag = 0.94f,
                        display = 1.48f,
                    )
                }
            }

        val defaultFont = BitmapFont().apply { data.setScale(fontScale.base * uiScale) }
        val titleFont = BitmapFont().apply { data.setScale(fontScale.title * uiScale) }
        val statusFont = BitmapFont().apply { data.setScale(fontScale.status * uiScale) }
        val headlineFont = BitmapFont().apply { data.setScale(fontScale.headline * uiScale) }
        val tagFont = BitmapFont().apply { data.setScale(fontScale.tag * uiScale) }
        val displayFont = BitmapFont().apply { data.setScale(fontScale.display * uiScale) }
        skin.add("default", defaultFont, BitmapFont::class.java)
        skin.add("title-font", titleFont, BitmapFont::class.java)
        skin.add("status-font", statusFont, BitmapFont::class.java)
        skin.add("headline-font", headlineFont, BitmapFont::class.java)
        skin.add("tag-font", tagFont, BitmapFont::class.java)
        skin.add("display-font", displayFont, BitmapFont::class.java)

        val whitePixmap =
            Pixmap(4, 4, Pixmap.Format.RGBA8888).apply {
                setColor(Color.WHITE)
                fill()
            }
        val whiteTexture = Texture(whitePixmap).also { it.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear) }
        textures.add(whiteTexture)
        val whiteRegion = TextureRegion(whiteTexture)
        skin.add("white", whiteRegion, TextureRegion::class.java)
        skin.add("white_region", whiteRegion, TextureRegion::class.java)
        skin.add("white", TextureRegionDrawable(whiteRegion), Drawable::class.java)
        whitePixmap.dispose()

        addPatchDrawable(
            skin = skin,
            textures = textures,
            name = "panel_strong",
            pixmap =
                createPanelPixmap(
                    width = 224,
                    height = 148,
                    radius = 28,
                    top = Color(0.03f, 0.09f, 0.16f, 0.92f),
                    bottom = Color(0.01f, 0.05f, 0.1f, 0.94f),
                    border = Color(0.36f, 0.86f, 1f, 0.98f),
                    accent = Color(0.1f, 0.58f, 0.82f, 0.4f),
                ),
            patch = 34,
        )
        addPatchDrawable(
            skin = skin,
            textures = textures,
            name = "panel_soft",
            pixmap =
                createPanelPixmap(
                    width = 224,
                    height = 148,
                    radius = 28,
                    top = Color(0.02f, 0.07f, 0.14f, 0.72f),
                    bottom = Color(0.01f, 0.04f, 0.08f, 0.76f),
                    border = Color(0.18f, 0.42f, 0.54f, 0.92f),
                    accent = Color(0.06f, 0.3f, 0.42f, 0.28f),
                ),
            patch = 34,
        )
        addPatchDrawable(
            skin = skin,
            textures = textures,
            name = "hud_panel",
            pixmap =
                createPanelPixmap(
                    width = 240,
                    height = 168,
                    radius = 30,
                    top = Color(0.02f, 0.08f, 0.14f, 0.84f),
                    bottom = Color(0.01f, 0.05f, 0.1f, 0.88f),
                    border = Color(0.3f, 0.78f, 0.94f, 0.94f),
                    accent = Color(0.14f, 0.62f, 0.84f, 0.32f),
                ),
            patch = 36,
        )
        addPatchDrawable(
            skin = skin,
            textures = textures,
            name = "hud_soft",
            pixmap =
                createPanelPixmap(
                    width = 224,
                    height = 152,
                    radius = 28,
                    top = Color(0.015f, 0.05f, 0.1f, 0.62f),
                    bottom = Color(0.01f, 0.03f, 0.07f, 0.68f),
                    border = Color(0.16f, 0.34f, 0.46f, 0.9f),
                    accent = Color(0.08f, 0.24f, 0.34f, 0.24f),
                ),
            patch = 34,
        )

        addPatchDrawable(
            skin = skin,
            textures = textures,
            name = "btn_up",
            pixmap =
                createButtonPixmap(
                    width = 240,
                    height = 112,
                    radius = 34,
                    top = Color(0.06f, 0.2f, 0.34f, 0.98f),
                    bottom = Color(0.02f, 0.11f, 0.2f, 0.99f),
                    border = Color(0.66f, 0.94f, 1f, 1f),
                    highlight = Color(0.52f, 0.9f, 1f, 0.26f),
                ),
            patch = 40,
        )
        addPatchDrawable(
            skin = skin,
            textures = textures,
            name = "btn_over",
            pixmap =
                createButtonPixmap(
                    width = 240,
                    height = 112,
                    radius = 34,
                    top = Color(0.1f, 0.3f, 0.46f, 1f),
                    bottom = Color(0.04f, 0.18f, 0.31f, 1f),
                    border = Color(0.86f, 0.99f, 1f, 1f),
                    highlight = Color(0.62f, 0.94f, 1f, 0.34f),
                ),
            patch = 40,
        )
        addPatchDrawable(
            skin = skin,
            textures = textures,
            name = "btn_down",
            pixmap =
                createButtonPixmap(
                    width = 240,
                    height = 112,
                    radius = 34,
                    top = Color(0.02f, 0.16f, 0.28f, 1f),
                    bottom = Color(0.0f, 0.09f, 0.18f, 1f),
                    border = Color(0.88f, 0.99f, 1f, 1f),
                    highlight = Color(0.22f, 0.66f, 0.9f, 0.18f),
                ),
            patch = 40,
        )
        addPatchDrawable(
            skin = skin,
            textures = textures,
            name = "btn_disabled",
            pixmap =
                createButtonPixmap(
                    width = 240,
                    height = 112,
                    radius = 34,
                    top = Color(0.08f, 0.1f, 0.14f, 0.9f),
                    bottom = Color(0.04f, 0.06f, 0.1f, 0.92f),
                    border = Color(0.18f, 0.24f, 0.3f, 1f),
                    highlight = Color(0f, 0f, 0f, 0f),
                ),
            patch = 40,
        )

        val sliderTrackTexture =
            textureFromPixmap(
                textures = textures,
                pixmap = createSliderTrackPixmap(),
            )
        val sliderKnobTexture =
            textureFromPixmap(
                textures = textures,
                pixmap = createSliderKnobPixmap(),
            )
        skin.add(
            "default-horizontal",
            Slider.SliderStyle(
                NinePatchDrawable(NinePatch(TextureRegion(sliderTrackTexture), 20, 20, 10, 10)),
                TextureRegionDrawable(TextureRegion(sliderKnobTexture)),
            ),
        )

        skin.add(
            "default",
            TextButton.TextButtonStyle().apply {
                up = skin.getDrawable("btn_up")
                checked = skin.getDrawable("btn_down")
                down = skin.getDrawable("btn_down")
                over = skin.getDrawable("btn_over")
                disabled = skin.getDrawable("btn_disabled")
                font = defaultFont
                fontColor = Color.WHITE
                overFontColor = Color.WHITE
                downFontColor = Color(0.92f, 0.99f, 1f, 1f)
                checkedFontColor = Color.WHITE
                disabledFontColor = Color(0.54f, 0.62f, 0.7f, 1f)
            },
        )
        skin.add("default", Label.LabelStyle(defaultFont, Color.WHITE))
        skin.add("title", Label.LabelStyle(titleFont, Color.WHITE))
        skin.add("display", Label.LabelStyle(displayFont, Color(0.94f, 0.98f, 1f, 1f)))
        skin.add("status", Label.LabelStyle(statusFont, Color(0.7f, 0.96f, 1f, 1f)))
        skin.add("headline", Label.LabelStyle(headlineFont, Color(0.94f, 0.98f, 1f, 1f)))
        skin.add("tag", Label.LabelStyle(tagFont, Color(0.8f, 0.94f, 1f, 1f)))
        skin.add("warning", Label.LabelStyle(headlineFont, Color(1f, 0.84f, 0.42f, 1f)))
        skin.add("critical", Label.LabelStyle(headlineFont, Color(1f, 0.42f, 0.42f, 1f)))
        return skin
    }

    private data class FontScaleProfile(
        val base: Float,
        val title: Float,
        val status: Float,
        val headline: Float,
        val tag: Float,
        val display: Float,
    )

    private fun addPatchDrawable(
        skin: Skin,
        textures: Array<Texture>,
        name: String,
        pixmap: Pixmap,
        patch: Int,
    ) {
        val texture = textureFromPixmap(textures, pixmap)
        val ninePatch = NinePatch(TextureRegion(texture), patch, patch, patch, patch)
        skin.add(name, NinePatchDrawable(ninePatch), Drawable::class.java)
    }

    private fun textureFromPixmap(
        textures: Array<Texture>,
        pixmap: Pixmap,
    ): Texture =
        Texture(pixmap).also {
            it.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
            textures.add(it)
            pixmap.dispose()
        }

    private fun createPanelPixmap(
        width: Int,
        height: Int,
        radius: Int,
        top: Color,
        bottom: Color,
        border: Color,
        accent: Color,
    ): Pixmap {
        val pixmap = Pixmap(width, height, Pixmap.Format.RGBA8888)
        drawRoundedShadow(pixmap, 6, 6, width - 12, height - 12, radius, Color(0f, 0f, 0f, 0.22f), 6)
        fillRoundedGradient(pixmap, 0, 0, width, height, radius, border, border)
        fillRoundedGradient(pixmap, 2, 2, width - 4, height - 4, max(6, radius - 2), top, bottom)
        val accentHeight = max(12, (height * 0.16f).roundToInt())
        fillRoundedGradient(
            pixmap = pixmap,
            x = 14,
            y = height - accentHeight - 12,
            width = width - 28,
            height = accentHeight,
            radius = min(radius / 2, accentHeight / 2),
            top = accent,
            bottom = Color(accent.r, accent.g, accent.b, accent.a * 0.16f),
        )
        return pixmap
    }

    private fun createButtonPixmap(
        width: Int,
        height: Int,
        radius: Int,
        top: Color,
        bottom: Color,
        border: Color,
        highlight: Color,
    ): Pixmap {
        val pixmap = Pixmap(width, height, Pixmap.Format.RGBA8888)
        drawRoundedShadow(pixmap, 8, 10, width - 16, height - 18, radius, Color(0f, 0f, 0f, 0.3f), 8)
        fillRoundedGradient(pixmap, 0, 0, width, height, radius, border, border)
        fillRoundedGradient(pixmap, 2, 2, width - 4, height - 4, max(6, radius - 2), top, bottom)
        fillRoundedGradient(
            pixmap = pixmap,
            x = 12,
            y = height - 40,
            width = width - 24,
            height = 24,
            radius = 12,
            top = highlight,
            bottom = Color(highlight.r, highlight.g, highlight.b, 0.02f),
        )
        fillRoundedGradient(
            pixmap = pixmap,
            x = 14,
            y = 14,
            width = width - 28,
            height = 18,
            radius = 10,
            top = Color(0f, 0f, 0f, 0.12f),
            bottom = Color(0f, 0f, 0f, 0f),
        )
        return pixmap
    }

    private fun createSliderTrackPixmap(): Pixmap {
        val pixmap = Pixmap(320, 36, Pixmap.Format.RGBA8888)
        fillRoundedGradient(
            pixmap = pixmap,
            x = 0,
            y = 0,
            width = pixmap.width,
            height = pixmap.height,
            radius = 18,
            top = Color(0.28f, 0.78f, 0.94f, 1f),
            bottom = Color(0.28f, 0.78f, 0.94f, 1f),
        )
        fillRoundedGradient(
            pixmap = pixmap,
            x = 2,
            y = 2,
            width = pixmap.width - 4,
            height = pixmap.height - 4,
            radius = 16,
            top = Color(0.04f, 0.12f, 0.2f, 1f),
            bottom = Color(0.02f, 0.08f, 0.14f, 1f),
        )
        fillRoundedGradient(
            pixmap = pixmap,
            x = 10,
            y = 8,
            width = pixmap.width - 20,
            height = 10,
            radius = 5,
            top = Color(0.2f, 0.72f, 0.88f, 0.26f),
            bottom = Color(0.12f, 0.36f, 0.52f, 0.08f),
        )
        return pixmap
    }

    private fun createSliderKnobPixmap(): Pixmap {
        val pixmap = Pixmap(72, 72, Pixmap.Format.RGBA8888)
        drawRoundedShadow(pixmap, 10, 8, 52, 52, 26, Color(0f, 0f, 0f, 0.28f), 6)
        fillRoundedGradient(
            pixmap = pixmap,
            x = 8,
            y = 10,
            width = 56,
            height = 56,
            radius = 28,
            top = Color(0.94f, 0.99f, 1f, 1f),
            bottom = Color(0.94f, 0.99f, 1f, 1f),
        )
        fillRoundedGradient(
            pixmap = pixmap,
            x = 10,
            y = 12,
            width = 52,
            height = 52,
            radius = 26,
            top = Color(0.7f, 0.94f, 1f, 1f),
            bottom = Color(0.28f, 0.72f, 0.92f, 1f),
        )
        fillRoundedGradient(
            pixmap = pixmap,
            x = 18,
            y = 40,
            width = 36,
            height = 10,
            radius = 5,
            top = Color(1f, 1f, 1f, 0.38f),
            bottom = Color(1f, 1f, 1f, 0.06f),
        )
        return pixmap
    }

    private fun drawRoundedShadow(
        pixmap: Pixmap,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        radius: Int,
        color: Color,
        spread: Int,
    ) {
        for (offset in spread downTo 1) {
            val alpha = color.a * (offset.toFloat() / spread.toFloat()) * 0.45f
            fillRoundedGradient(
                pixmap = pixmap,
                x = x + offset / 2,
                y = y - offset / 3,
                width = width,
                height = height,
                radius = max(4, radius - offset / 2),
                top = Color(color.r, color.g, color.b, alpha),
                bottom = Color(color.r, color.g, color.b, alpha * 0.7f),
            )
        }
    }

    private fun fillRoundedGradient(
        pixmap: Pixmap,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        radius: Int,
        top: Color,
        bottom: Color,
    ) {
        for (row in 0 until height) {
            val mix = row / (height - 1f).coerceAtLeast(1f)
            val color =
                Color(
                    MathUtils.lerp(top.r, bottom.r, mix),
                    MathUtils.lerp(top.g, bottom.g, mix),
                    MathUtils.lerp(top.b, bottom.b, mix),
                    MathUtils.lerp(top.a, bottom.a, mix),
                )
            pixmap.setColor(color)
            val inset = roundedInset(row, height, radius)
            val lineX = x + inset
            val lineWidth = width - inset * 2
            if (lineWidth > 0) {
                pixmap.drawLine(lineX, y + row, lineX + lineWidth - 1, y + row)
            }
        }
    }

    private fun roundedInset(
        row: Int,
        height: Int,
        radius: Int,
    ): Int {
        val distanceFromTop = height - 1 - row
        return when {
            row < radius -> circleInset(radius, radius - row - 1)
            distanceFromTop < radius -> circleInset(radius, radius - distanceFromTop - 1)
            else -> 0
        }
    }

    private fun circleInset(
        radius: Int,
        edgeDistance: Int,
    ): Int {
        if (radius <= 0) return 0
        val vertical = edgeDistance.coerceIn(0, radius)
        val horizontal = sqrt((radius * radius - vertical * vertical).toDouble()).toInt()
        return max(0, radius - horizontal)
    }
}
