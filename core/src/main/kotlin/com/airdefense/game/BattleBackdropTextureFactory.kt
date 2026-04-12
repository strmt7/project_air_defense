package com.airdefense.game

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.math.MathUtils
import kotlin.math.abs
import kotlin.math.sin

private const val SKY_CROP_Y_RATIO = 0.16f
private const val SKY_CROP_HEIGHT_RATIO = 0.56f
private const val SKY_TEXTURE_WIDTH = 1024
private const val SKY_TEXTURE_HEIGHT = 512
private const val SKY_TEXTURE_MINIMUM_WIDTH = 512
private const val SKY_TEXTURE_MINIMUM_HEIGHT = 256
private const val SKY_HORIZON_CENTER = 0.35f
private const val SKY_HORIZON_FALLOFF = 2.2f
private const val SKY_NOISE_MIN = -0.012f
private const val SKY_NOISE_MAX = 0.012f
private const val SKY_NOISE_BLUE_SCALE = 1.4f
private const val SKY_STAR_COUNT = 96
private const val SKY_STAR_START_RATIO = 0.52f
private const val SKY_STAR_MIN_RADIUS = 0
private const val SKY_STAR_MAX_RADIUS = 1
private const val SKY_STAR_ALPHA_MIN = 0.28f
private const val SKY_STAR_ALPHA_MAX = 0.62f
private const val SKY_STAR_BRIGHTNESS_MIN = 0.45f
private const val SKY_STAR_BRIGHTNESS_MAX = 0.82f
private val SKY_BASE_COLOR = Color.valueOf("03050DFF")
private const val SKY_HORIZON_RED_GAIN = 0.06f
private const val SKY_HORIZON_GREEN_GAIN = 0.08f
private const val SKY_HORIZON_BLUE_GAIN = 0.16f
private const val FOG_TEXTURE_WIDTH = 512
private const val FOG_TEXTURE_WIDTH_MINIMUM = 256
private const val FOG_TEXTURE_HEIGHT = 160
private const val FOG_TEXTURE_HEIGHT_MINIMUM = 96
private const val FOG_DENSITY_CENTER = 0.44f
private const val FOG_DENSITY_FALLOFF = 2.05f
private const val FOG_NOISE_MIN = 0.9f
private const val FOG_NOISE_MAX = 1.08f
private const val FOG_ALPHA_SCALE = 0.36f
private const val FOG_ALPHA_MAX = 0.38f
private const val FOG_ALPHA_WARM_NEAR = 1.08f
private const val FOG_ALPHA_WARM_FAR = 0.86f
private val FOG_NEAR_COLOR = Color.valueOf("574730FF")
private val FOG_FAR_COLOR = Color.valueOf("2E4D6BFF")
private const val GLOW_TEXTURE_WIDTH = 512
private const val GLOW_TEXTURE_WIDTH_MINIMUM = 256
private const val GLOW_TEXTURE_HEIGHT = 220
private const val GLOW_TEXTURE_HEIGHT_MINIMUM = 128
private const val GLOW_HORIZON_CENTER = 0.24f
private const val GLOW_HORIZON_FALLOFF = 4f
private const val GLOW_UPPER_CENTER = 0.58f
private const val GLOW_UPPER_FALLOFF = 3.4f
private const val GLOW_SIDE_FALLOFF = 1.55f
private const val GLOW_SIDE_MIN = 0.45f
private const val GLOW_WARM_ALPHA = 0.34f
private const val GLOW_COOL_ALPHA = 0.14f
private const val GLOW_ALPHA_MAX = 0.42f
private const val GLOW_RED_BASE = 0.12f
private const val GLOW_GREEN_BASE = 0.12f
private const val GLOW_BLUE_BASE = 0.18f
private const val GLOW_WARM_RED_GAIN = 1.8f
private const val GLOW_WARM_GREEN_GAIN = 1.1f
private const val GLOW_WARM_BLUE_GAIN = 0.28f
private const val GLOW_COOL_RED_GAIN = 0.2f
private const val GLOW_COOL_GREEN_GAIN = 0.55f
private const val GLOW_COOL_BLUE_GAIN = 1.35f
private const val REFLECTION_TEXTURE_WIDTH = 768
private const val REFLECTION_TEXTURE_WIDTH_MINIMUM = 256
private const val REFLECTION_TEXTURE_HEIGHT = 256
private const val REFLECTION_TEXTURE_HEIGHT_MINIMUM = 128
private const val REFLECTION_STREAK_FREQ_A = 26f
private const val REFLECTION_STREAK_FREQ_B = 63f
private const val REFLECTION_STREAK_GAIN_A = 0.5f
private const val REFLECTION_STREAK_GAIN_B = 0.24f
private const val REFLECTION_WARM_CENTER = 0.5f
private const val REFLECTION_WARM_FALLOFF = 1.6f
private const val REFLECTION_WARM_WINDOW_GAIN = 0.45f
private const val REFLECTION_VERTICAL_FADE = 0.22f
private const val REFLECTION_WAVE_U = 44f
private const val REFLECTION_WAVE_V = 18f
private const val REFLECTION_WAVE_GAIN = 0.04f
private const val REFLECTION_COOL_BASE = 0.08f
private const val REFLECTION_COOL_FADE_GAIN = 0.22f
private const val REFLECTION_COOL_GLOW_GAIN = 0.16f
private const val REFLECTION_WARM_COLOR_GAIN = 0.48f
private const val REFLECTION_RED_BASE = 0.08f
private const val REFLECTION_GREEN_BASE = 0.12f
private const val REFLECTION_BLUE_BASE = 0.2f
private const val REFLECTION_RED_WARM_GAIN = 1.1f
private const val REFLECTION_GREEN_WARM_GAIN = 0.72f
private const val REFLECTION_GREEN_COOL_GAIN = 0.28f
private const val REFLECTION_BLUE_COOL_GAIN = 0.95f
private const val REFLECTION_ALPHA_GAIN = 0.42f

internal class BattleBackdropTextureFactory(
    private val context: BattleTextureContext,
) {
    fun loadTexture(
        path: String,
        fallbackColor: Color,
    ): Texture = context.loadTexture(path, fallbackColor)

    fun createSkyTexture(): Texture {
        val skyFile = Gdx.files.internal(BattleTerrainAssetFactory.HORIZON_TEXTURE_PATH)
        if (skyFile.exists()) {
            val source = Pixmap(skyFile)
            val cropY = (source.height * SKY_CROP_Y_RATIO).toInt()
            val cropHeight = (source.height * SKY_CROP_HEIGHT_RATIO).toInt().coerceAtLeast(1)
            val cropped = Pixmap(source.width, cropHeight, Pixmap.Format.RGBA8888)
            cropped.drawPixmap(source, 0, 0, 0, cropY, source.width, cropHeight)
            source.dispose()
            val texture = context.createManagedTexture(cropped)
            cropped.dispose()
            return texture
        }

        val pixmap =
            Pixmap(
                context.scaledSceneTextureSize(SKY_TEXTURE_WIDTH, SKY_TEXTURE_MINIMUM_WIDTH),
                context.scaledSceneTextureSize(SKY_TEXTURE_HEIGHT, SKY_TEXTURE_MINIMUM_HEIGHT),
                Pixmap.Format.RGBA8888,
            )
        for (x in 0 until pixmap.width) {
            for (y in 0 until pixmap.height) {
                val vertical = y / (pixmap.height - 1f)
                val horizonGlow = (1f - abs(vertical - SKY_HORIZON_CENTER) * SKY_HORIZON_FALLOFF).coerceIn(0f, 1f)
                val noise = MathUtils.random(SKY_NOISE_MIN, SKY_NOISE_MAX)
                pixmap.setColor(
                    (SKY_BASE_COLOR.r + horizonGlow * SKY_HORIZON_RED_GAIN + noise).coerceIn(0f, 1f),
                    (SKY_BASE_COLOR.g + horizonGlow * SKY_HORIZON_GREEN_GAIN + noise).coerceIn(0f, 1f),
                    (SKY_BASE_COLOR.b + horizonGlow * SKY_HORIZON_BLUE_GAIN + noise * SKY_NOISE_BLUE_SCALE).coerceIn(0f, 1f),
                    1f,
                )
                pixmap.drawPixel(x, y)
            }
        }

        repeat(SKY_STAR_COUNT) {
            val x = MathUtils.random(0, pixmap.width - 1)
            val y = MathUtils.random((pixmap.height * SKY_STAR_START_RATIO).toInt(), pixmap.height - 1)
            val brightness = MathUtils.random(SKY_STAR_BRIGHTNESS_MIN, SKY_STAR_BRIGHTNESS_MAX)
            pixmap.setColor(
                brightness,
                brightness,
                brightness,
                MathUtils.random(SKY_STAR_ALPHA_MIN, SKY_STAR_ALPHA_MAX),
            )
            pixmap.fillCircle(x, y, MathUtils.random(SKY_STAR_MIN_RADIUS, SKY_STAR_MAX_RADIUS))
        }
        return context.createManagedTexture(pixmap).also { pixmap.dispose() }
    }

    fun createFogTexture(): Texture {
        val pixmap =
            Pixmap(
                context.scaledSceneTextureSize(FOG_TEXTURE_WIDTH, FOG_TEXTURE_WIDTH_MINIMUM),
                context.scaledSceneTextureSize(FOG_TEXTURE_HEIGHT, FOG_TEXTURE_HEIGHT_MINIMUM),
                Pixmap.Format.RGBA8888,
            )
        for (x in 0 until pixmap.width) {
            for (y in 0 until pixmap.height) {
                val vertical = y / (pixmap.height - 1f)
                val density = (1f - abs(vertical - FOG_DENSITY_CENTER) * FOG_DENSITY_FALLOFF).coerceIn(0f, 1f)
                val noise = MathUtils.random(FOG_NOISE_MIN, FOG_NOISE_MAX)
                val warmMix = (1f - vertical).coerceIn(0f, 1f)
                val alpha = (density * FOG_ALPHA_SCALE * noise).coerceIn(0f, FOG_ALPHA_MAX)
                pixmap.setColor(
                    MathUtils.lerp(FOG_NEAR_COLOR.r, FOG_FAR_COLOR.r, vertical),
                    MathUtils.lerp(FOG_NEAR_COLOR.g, FOG_FAR_COLOR.g, vertical),
                    MathUtils.lerp(FOG_NEAR_COLOR.b, FOG_FAR_COLOR.b, vertical),
                    alpha * MathUtils.lerp(FOG_ALPHA_WARM_NEAR, FOG_ALPHA_WARM_FAR, warmMix),
                )
                pixmap.drawPixel(x, y)
            }
        }
        return context.createManagedTexture(pixmap).also { pixmap.dispose() }
    }

    fun createGlowTexture(): Texture {
        val pixmap =
            Pixmap(
                context.scaledSceneTextureSize(GLOW_TEXTURE_WIDTH, GLOW_TEXTURE_WIDTH_MINIMUM),
                context.scaledSceneTextureSize(GLOW_TEXTURE_HEIGHT, GLOW_TEXTURE_HEIGHT_MINIMUM),
                Pixmap.Format.RGBA8888,
            )
        for (x in 0 until pixmap.width) {
            for (y in 0 until pixmap.height) {
                val vertical = y / (pixmap.height - 1f)
                val horizonBand = (1f - abs(vertical - GLOW_HORIZON_CENTER) * GLOW_HORIZON_FALLOFF).coerceIn(0f, 1f)
                val upperBand = (1f - abs(vertical - GLOW_UPPER_CENTER) * GLOW_UPPER_FALLOFF).coerceIn(0f, 1f)
                val sideFade = (1f - abs((x / (pixmap.width - 1f)) - 0.5f) * GLOW_SIDE_FALLOFF).coerceIn(GLOW_SIDE_MIN, 1f)
                val warmAlpha = horizonBand * GLOW_WARM_ALPHA * sideFade
                val coolAlpha = upperBand * GLOW_COOL_ALPHA
                val red = GLOW_RED_BASE + warmAlpha * GLOW_WARM_RED_GAIN + coolAlpha * GLOW_COOL_RED_GAIN
                val green = GLOW_GREEN_BASE + warmAlpha * GLOW_WARM_GREEN_GAIN + coolAlpha * GLOW_COOL_GREEN_GAIN
                val blue = GLOW_BLUE_BASE + warmAlpha * GLOW_WARM_BLUE_GAIN + coolAlpha * GLOW_COOL_BLUE_GAIN
                val alpha = (warmAlpha + coolAlpha).coerceIn(0f, GLOW_ALPHA_MAX)
                pixmap.setColor(red.coerceIn(0f, 1f), green.coerceIn(0f, 1f), blue.coerceIn(0f, 1f), alpha)
                pixmap.drawPixel(x, y)
            }
        }
        return context.createManagedTexture(pixmap).also { pixmap.dispose() }
    }

    fun createReflectionTexture(): Texture {
        val pixmap =
            Pixmap(
                context.scaledSceneTextureSize(REFLECTION_TEXTURE_WIDTH, REFLECTION_TEXTURE_WIDTH_MINIMUM),
                context.scaledSceneTextureSize(REFLECTION_TEXTURE_HEIGHT, REFLECTION_TEXTURE_HEIGHT_MINIMUM),
                Pixmap.Format.RGBA8888,
            )
        for (x in 0 until pixmap.width) {
            val u = x / (pixmap.width - 1f)
            val streakSeed =
                sin(u * REFLECTION_STREAK_FREQ_A) * REFLECTION_STREAK_GAIN_A +
                    sin(u * REFLECTION_STREAK_FREQ_B) * REFLECTION_STREAK_GAIN_B
            val warmWindow = (1f - abs(u - REFLECTION_WARM_CENTER) * REFLECTION_WARM_FALLOFF).coerceIn(0f, 1f)
            for (y in 0 until pixmap.height) {
                val v = y / (pixmap.height - 1f)
                val fade = (1f - v).coerceIn(0f, 1f)
                val wave = sin((u * REFLECTION_WAVE_U) + (v * REFLECTION_WAVE_V)) * REFLECTION_WAVE_GAIN
                val glow =
                    (
                        streakSeed * fade +
                            warmWindow * REFLECTION_WARM_WINDOW_GAIN -
                            v * REFLECTION_VERTICAL_FADE +
                            wave
                    ).coerceIn(0f, 1f)
                val cool =
                    (
                        REFLECTION_COOL_BASE +
                            fade * REFLECTION_COOL_FADE_GAIN +
                            glow * REFLECTION_COOL_GLOW_GAIN
                    ).coerceIn(0f, 1f)
                val warm = (glow * REFLECTION_WARM_COLOR_GAIN).coerceIn(0f, 1f)
                pixmap.setColor(
                    (REFLECTION_RED_BASE + warm * REFLECTION_RED_WARM_GAIN).coerceIn(0f, 1f),
                    (
                        REFLECTION_GREEN_BASE +
                            warm * REFLECTION_GREEN_WARM_GAIN +
                            cool * REFLECTION_GREEN_COOL_GAIN
                    ).coerceIn(0f, 1f),
                    (REFLECTION_BLUE_BASE + cool * REFLECTION_BLUE_COOL_GAIN).coerceIn(0f, 1f),
                    (fade * glow * REFLECTION_ALPHA_GAIN).coerceIn(0f, REFLECTION_ALPHA_GAIN),
                )
                pixmap.drawPixel(x, y)
            }
        }
        return context.createManagedTexture(pixmap).also { pixmap.dispose() }
    }
}
