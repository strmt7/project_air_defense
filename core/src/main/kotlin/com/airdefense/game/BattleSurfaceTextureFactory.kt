package com.airdefense.game

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
import com.badlogic.gdx.math.MathUtils
import kotlin.math.cos
import kotlin.math.sin

private const val FACADE_BASE_ROUGHNESS = 0.84f
private const val FACADE_VERTICAL_BAND_WIDTH = 14
private const val FACADE_HORIZONTAL_BAND_HEIGHT = 18
private const val FACADE_VERTICAL_BAND_DELTA = 0.03f
private const val FACADE_HORIZONTAL_BAND_DELTA = 0.02f
private const val FACADE_NOISE_MIN = -0.015f
private const val FACADE_NOISE_MAX = 0.015f
private const val WINDOW_START_X = 8
private const val WINDOW_START_Y = 10
private const val WINDOW_STEP_X = 20
private const val WINDOW_STEP_Y = 22
private const val WINDOW_WIDTH = 10
private const val WINDOW_HEIGHT = 12
private const val WINDOW_GLOW_PROBABILITY = 0.7f
private const val WINDOW_LIT_ROUGHNESS = 0.15f
private const val WINDOW_DARK_ROUGHNESS = 0.88f
private val WINDOW_DARK_COLOR = Color.valueOf("080D14FF")
private const val GROUND_TEXTURE_BASE = 256
private const val GROUND_TEXTURE_MINIMUM = 128
private const val GROUND_STRIP_TILE = 32
private const val GROUND_STRIP_HIGH = 0.02f
private const val GROUND_STRIP_LOW = -0.01f
private const val GROUND_NOISE_MIN = -0.03f
private const val GROUND_NOISE_MAX = 0.03f
private const val GROUND_ROUGHNESS_BASE = 0.82f
private const val GROUND_ROUGHNESS_MIN = -0.1f
private const val GROUND_ROUGHNESS_MAX = 0.08f
private const val STANDARD_ROUGHNESS_FLOOR = 0.1f
private const val METAL_ROUGHNESS_FLOOR = 0.08f
private val GROUND_BASE_COLOR = Color.valueOf("121314FF")
private const val GROUND_BLUE_NOISE_SCALE = 0.7f
private const val SEA_WAVE_SCALE = 0.045f
private const val SEA_WAVE_INTENSITY = 0.03f
private const val SEA_SHIMMER_SCALE = 0.08f
private const val SEA_SHIMMER_INTENSITY = 0.02f
private const val SEA_NOISE_MIN = -0.018f
private const val SEA_NOISE_MAX = 0.018f
private const val SEA_NOISE_BLUE_SCALE = 1.2f
private const val SEA_ROUGHNESS_BASE = 0.32f
private const val SEA_ROUGHNESS_MIN = -0.06f
private const val SEA_ROUGHNESS_MAX = 0.08f
private val SEA_BASE_COLOR = Color.valueOf("051E38FF")
private const val BEACH_DUNE_X_SCALE = 0.03f
private const val BEACH_DUNE_Y_SCALE = 0.024f
private const val BEACH_DUNE_X_INTENSITY = 0.04f
private const val BEACH_DUNE_Y_INTENSITY = 0.025f
private const val BEACH_NOISE_MIN = -0.03f
private const val BEACH_NOISE_MAX = 0.03f
private const val BEACH_GREEN_DUNE_SCALE = 0.7f
private const val BEACH_BLUE_NOISE_SCALE = 0.6f
private const val BEACH_ROUGHNESS_BASE = 0.9f
private const val BEACH_ROUGHNESS_MIN = -0.04f
private const val BEACH_ROUGHNESS_MAX = 0.05f
private val BEACH_BASE_COLOR = Color.valueOf("948057FF")
private const val PARK_STRIP_X_TILE = 28
private const val PARK_STRIP_Y_TILE = 36
private const val PARK_STRIP_HIGH = 0.03f
private const val PARK_STRIP_LOW = -0.02f
private const val PARK_NOISE_MIN = -0.035f
private const val PARK_NOISE_MAX = 0.035f
private const val PARK_ROUGHNESS_BASE = 0.86f
private const val PARK_ROUGHNESS_MIN = -0.06f
private const val PARK_ROUGHNESS_MAX = 0.05f
private val PARK_BASE_COLOR = Color.valueOf("0F2E14FF")
private const val PROMENADE_TILE_SIZE = 48
private const val PROMENADE_TILE_HIGH = 0.04f
private const val PROMENADE_TILE_LOW = -0.015f
private const val PROMENADE_NOISE_MIN = -0.02f
private const val PROMENADE_NOISE_MAX = 0.02f
private const val PROMENADE_BLUE_TILE_SCALE = 0.7f
private const val PROMENADE_ROUGHNESS_BASE = 0.72f
private const val PROMENADE_ROUGHNESS_MIN = -0.06f
private const val PROMENADE_ROUGHNESS_MAX = 0.04f
private val PROMENADE_BASE_COLOR = Color.valueOf("38332EFF")
private const val ROAD_NOISE_MIN = -0.025f
private const val ROAD_NOISE_MAX = 0.025f
private const val ROAD_ROUGHNESS_BASE = 0.74f
private const val ROAD_ROUGHNESS_MIN = -0.08f
private const val ROAD_ROUGHNESS_MAX = 0.06f
private val ROAD_BASE_COLOR = Color.valueOf("17171AFF")
private const val ROAD_MARKING_HALF_WIDTH = 10
private const val ROAD_MARKING_WIDTH = 2
private val ROAD_MARKING_COLOR = Color.valueOf("A6995CFF")
private const val ROAD_MARKING_ROUGHNESS = 0.42f
private const val METAL_TEXTURE_BASE = 128
private const val METAL_TEXTURE_MINIMUM = 64
private const val METAL_STREAK_PERIOD = 24
private const val METAL_STREAK_DELTA = 0.05f
private const val METAL_NOISE_MIN = -0.025f
private const val METAL_NOISE_MAX = 0.025f
private const val METAL_ROUGHNESS_BASE = 0.42f
private const val METAL_ROUGHNESS_MIN = -0.1f
private const val METAL_ROUGHNESS_MAX = 0.08f
private const val CONCRETE_NOISE_MIN = -0.05f
private const val CONCRETE_NOISE_MAX = 0.05f
private const val CONCRETE_ROUGHNESS_BASE = 0.88f
private const val CONCRETE_ROUGHNESS_MIN = -0.06f
private const val CONCRETE_ROUGHNESS_MAX = 0.04f

internal fun createTiledDiffuseAttribute(
    texture: Texture,
    scaleU: Float,
    scaleV: Float,
): TextureAttribute =
    TextureAttribute.createDiffuse(texture).apply {
        this.scaleU = scaleU
        this.scaleV = scaleV
    }

internal class BattleSurfaceTextureFactory(
    private val context: BattleTextureContext,
) {
    fun createSolidTextureSet(
        color: Color,
        roughnessValue: Float,
    ): SurfaceTextureSet = context.createSolidTextureSet(color, roughnessValue)

    fun createFacadeTextureSet(
        width: Int,
        height: Int,
        base: Color,
        lit: Color,
    ): SurfaceTextureSet {
        val diffuse = Pixmap(width, height, Pixmap.Format.RGBA8888)
        val roughness = Pixmap(width, height, Pixmap.Format.RGBA8888)
        diffuse.setColor(base)
        diffuse.fill()
        roughness.setColor(FACADE_BASE_ROUGHNESS, FACADE_BASE_ROUGHNESS, FACADE_BASE_ROUGHNESS, 1f)
        roughness.fill()

        for (x in 0 until width) {
            for (y in 0 until height) {
                val verticalBand = ((x / FACADE_VERTICAL_BAND_WIDTH) % 2) * FACADE_VERTICAL_BAND_DELTA
                val horizontalBand = ((y / FACADE_HORIZONTAL_BAND_HEIGHT) % 2) * FACADE_HORIZONTAL_BAND_DELTA
                val noise = MathUtils.random(FACADE_NOISE_MIN, FACADE_NOISE_MAX)
                diffuse.setColor(
                    (base.r + verticalBand + noise).coerceIn(0f, 1f),
                    (base.g + horizontalBand + noise).coerceIn(0f, 1f),
                    (base.b + noise).coerceIn(0f, 1f),
                    1f,
                )
                diffuse.drawPixel(x, y)
            }
        }

        for (x in WINDOW_START_X until width step WINDOW_STEP_X) {
            for (y in WINDOW_START_Y until height step WINDOW_STEP_Y) {
                val glow = MathUtils.randomBoolean(WINDOW_GLOW_PROBABILITY)
                diffuse.setColor(if (glow) lit else WINDOW_DARK_COLOR)
                diffuse.fillRectangle(x, y, WINDOW_WIDTH, WINDOW_HEIGHT)
                val roughnessValue = if (glow) WINDOW_LIT_ROUGHNESS else WINDOW_DARK_ROUGHNESS
                roughness.setColor(roughnessValue, roughnessValue, roughnessValue, 1f)
                roughness.fillRectangle(x, y, WINDOW_WIDTH, WINDOW_HEIGHT)
            }
        }

        return context.createTextureSet(diffuse, roughness)
    }

    fun createGroundTextureSet(): SurfaceTextureSet =
        context.buildSquareTextureSet(
            GROUND_TEXTURE_BASE,
            GROUND_TEXTURE_MINIMUM,
        ) { diffuse, roughness, textureSize ->
            for (x in 0 until textureSize) {
                for (y in 0 until textureSize) {
                    val noise = MathUtils.random(GROUND_NOISE_MIN, GROUND_NOISE_MAX)
                    val striping =
                        if ((x / GROUND_STRIP_TILE + y / GROUND_STRIP_TILE) % 2 == 0) {
                            GROUND_STRIP_HIGH
                        } else {
                            GROUND_STRIP_LOW
                        }
                    diffuse.setColor(
                        (GROUND_BASE_COLOR.r + noise + striping).coerceIn(0f, 1f),
                        (GROUND_BASE_COLOR.g + noise).coerceIn(0f, 1f),
                        (GROUND_BASE_COLOR.b + noise * GROUND_BLUE_NOISE_SCALE).coerceIn(0f, 1f),
                        1f,
                    )
                    diffuse.drawPixel(x, y)
                    val roughnessValue =
                        (
                            GROUND_ROUGHNESS_BASE +
                                MathUtils.random(GROUND_ROUGHNESS_MIN, GROUND_ROUGHNESS_MAX)
                        ).coerceIn(STANDARD_ROUGHNESS_FLOOR, 1f)
                    roughness.setColor(roughnessValue, roughnessValue, roughnessValue, 1f)
                    roughness.drawPixel(x, y)
                }
            }
        }

    fun createSeaTextureSet(): SurfaceTextureSet =
        context.buildSquareTextureSet(GROUND_TEXTURE_BASE, GROUND_TEXTURE_MINIMUM) { diffuse, roughness, textureSize ->
            for (x in 0 until textureSize) {
                for (y in 0 until textureSize) {
                    val wave = sin((x + y) * SEA_WAVE_SCALE) * SEA_WAVE_INTENSITY
                    val shimmer = cos(y * SEA_SHIMMER_SCALE) * SEA_SHIMMER_INTENSITY
                    val noise = MathUtils.random(SEA_NOISE_MIN, SEA_NOISE_MAX)
                    diffuse.setColor(
                        (SEA_BASE_COLOR.r + wave + noise).coerceIn(0f, 1f),
                        (SEA_BASE_COLOR.g + shimmer + noise).coerceIn(0f, 1f),
                        (SEA_BASE_COLOR.b + wave + shimmer + noise * SEA_NOISE_BLUE_SCALE).coerceIn(0f, 1f),
                        1f,
                    )
                    diffuse.drawPixel(x, y)
                    val roughnessValue =
                        (
                            SEA_ROUGHNESS_BASE +
                                MathUtils.random(SEA_ROUGHNESS_MIN, SEA_ROUGHNESS_MAX)
                        ).coerceIn(METAL_ROUGHNESS_FLOOR, 1f)
                    roughness.setColor(roughnessValue, roughnessValue, roughnessValue, 1f)
                    roughness.drawPixel(x, y)
                }
            }
        }

    fun createBeachTextureSet(): SurfaceTextureSet =
        context.buildSquareTextureSet(GROUND_TEXTURE_BASE, GROUND_TEXTURE_MINIMUM) { diffuse, roughness, textureSize ->
            for (x in 0 until textureSize) {
                for (y in 0 until textureSize) {
                    val dune =
                        sin(x * BEACH_DUNE_X_SCALE) * BEACH_DUNE_X_INTENSITY +
                            cos(y * BEACH_DUNE_Y_SCALE) * BEACH_DUNE_Y_INTENSITY
                    val noise = MathUtils.random(BEACH_NOISE_MIN, BEACH_NOISE_MAX)
                    diffuse.setColor(
                        (BEACH_BASE_COLOR.r + dune + noise).coerceIn(0f, 1f),
                        (BEACH_BASE_COLOR.g + dune * BEACH_GREEN_DUNE_SCALE + noise).coerceIn(0f, 1f),
                        (BEACH_BASE_COLOR.b + noise * BEACH_BLUE_NOISE_SCALE).coerceIn(0f, 1f),
                        1f,
                    )
                    diffuse.drawPixel(x, y)
                    val roughnessValue =
                        (
                            BEACH_ROUGHNESS_BASE +
                                MathUtils.random(BEACH_ROUGHNESS_MIN, BEACH_ROUGHNESS_MAX)
                        ).coerceIn(STANDARD_ROUGHNESS_FLOOR, 1f)
                    roughness.setColor(roughnessValue, roughnessValue, roughnessValue, 1f)
                    roughness.drawPixel(x, y)
                }
            }
        }

    fun createParkTextureSet(): SurfaceTextureSet =
        context.buildSquareTextureSet(GROUND_TEXTURE_BASE, GROUND_TEXTURE_MINIMUM) { diffuse, roughness, textureSize ->
            for (x in 0 until textureSize) {
                for (y in 0 until textureSize) {
                    val strip =
                        if ((x / PARK_STRIP_X_TILE + y / PARK_STRIP_Y_TILE) % 2 == 0) {
                            PARK_STRIP_HIGH
                        } else {
                            PARK_STRIP_LOW
                        }
                    val noise = MathUtils.random(PARK_NOISE_MIN, PARK_NOISE_MAX)
                    diffuse.setColor(
                        (PARK_BASE_COLOR.r + strip + noise).coerceIn(0f, 1f),
                        (PARK_BASE_COLOR.g + strip + noise).coerceIn(0f, 1f),
                        (PARK_BASE_COLOR.b + noise).coerceIn(0f, 1f),
                        1f,
                    )
                    diffuse.drawPixel(x, y)
                    val roughnessValue =
                        (
                            PARK_ROUGHNESS_BASE +
                                MathUtils.random(PARK_ROUGHNESS_MIN, PARK_ROUGHNESS_MAX)
                        ).coerceIn(STANDARD_ROUGHNESS_FLOOR, 1f)
                    roughness.setColor(roughnessValue, roughnessValue, roughnessValue, 1f)
                    roughness.drawPixel(x, y)
                }
            }
        }

    fun createPromenadeTextureSet(): SurfaceTextureSet =
        context.buildSquareTextureSet(GROUND_TEXTURE_BASE, GROUND_TEXTURE_MINIMUM) { diffuse, roughness, textureSize ->
            for (x in 0 until textureSize) {
                for (y in 0 until textureSize) {
                    val tile =
                        if ((x / PROMENADE_TILE_SIZE + y / PROMENADE_TILE_SIZE) % 2 == 0) {
                            PROMENADE_TILE_HIGH
                        } else {
                            PROMENADE_TILE_LOW
                        }
                    val noise = MathUtils.random(PROMENADE_NOISE_MIN, PROMENADE_NOISE_MAX)
                    diffuse.setColor(
                        (PROMENADE_BASE_COLOR.r + tile + noise).coerceIn(0f, 1f),
                        (PROMENADE_BASE_COLOR.g + tile + noise).coerceIn(0f, 1f),
                        (PROMENADE_BASE_COLOR.b + tile * PROMENADE_BLUE_TILE_SCALE + noise).coerceIn(0f, 1f),
                        1f,
                    )
                    diffuse.drawPixel(x, y)
                    val roughnessValue =
                        (
                            PROMENADE_ROUGHNESS_BASE +
                                MathUtils.random(PROMENADE_ROUGHNESS_MIN, PROMENADE_ROUGHNESS_MAX)
                        ).coerceIn(STANDARD_ROUGHNESS_FLOOR, 1f)
                    roughness.setColor(roughnessValue, roughnessValue, roughnessValue, 1f)
                    roughness.drawPixel(x, y)
                }
            }
        }

    fun createRoadTextureSet(): SurfaceTextureSet =
        context.buildSquareTextureSet(GROUND_TEXTURE_BASE, GROUND_TEXTURE_MINIMUM) { diffuse, roughness, textureSize ->
            for (x in 0 until textureSize) {
                for (y in 0 until textureSize) {
                    val noise = MathUtils.random(ROAD_NOISE_MIN, ROAD_NOISE_MAX)
                    diffuse.setColor(
                        (ROAD_BASE_COLOR.r + noise).coerceIn(0f, 1f),
                        (ROAD_BASE_COLOR.g + noise).coerceIn(0f, 1f),
                        (ROAD_BASE_COLOR.b + noise).coerceIn(0f, 1f),
                        1f,
                    )
                    diffuse.drawPixel(x, y)
                    val roughnessValue =
                        (
                            ROAD_ROUGHNESS_BASE +
                                MathUtils.random(ROAD_ROUGHNESS_MIN, ROAD_ROUGHNESS_MAX)
                        ).coerceIn(STANDARD_ROUGHNESS_FLOOR, 1f)
                    roughness.setColor(roughnessValue, roughnessValue, roughnessValue, 1f)
                    roughness.drawPixel(x, y)
                }
            }

            val markingStart = textureSize / 2 - ROAD_MARKING_HALF_WIDTH
            val markingEnd = textureSize / 2 + ROAD_MARKING_HALF_WIDTH
            for (x in markingStart..markingEnd) {
                diffuse.setColor(ROAD_MARKING_COLOR)
                diffuse.fillRectangle(x, 0, ROAD_MARKING_WIDTH, textureSize)
                roughness.setColor(ROAD_MARKING_ROUGHNESS, ROAD_MARKING_ROUGHNESS, ROAD_MARKING_ROUGHNESS, 1f)
                roughness.fillRectangle(x, 0, ROAD_MARKING_WIDTH, textureSize)
            }
        }

    fun createMetalTextureSet(base: Color): SurfaceTextureSet =
        context.buildSquareTextureSet(METAL_TEXTURE_BASE, METAL_TEXTURE_MINIMUM) { diffuse, roughness, textureSize ->
            for (x in 0 until textureSize) {
                for (y in 0 until textureSize) {
                    val streak = ((x % METAL_STREAK_PERIOD) / METAL_STREAK_PERIOD.toFloat()) * METAL_STREAK_DELTA
                    val noise = MathUtils.random(METAL_NOISE_MIN, METAL_NOISE_MAX)
                    diffuse.setColor(
                        (base.r + streak + noise).coerceIn(0f, 1f),
                        (base.g + streak + noise).coerceIn(0f, 1f),
                        (base.b + streak + noise).coerceIn(0f, 1f),
                        1f,
                    )
                    diffuse.drawPixel(x, y)
                    val roughnessValue =
                        (
                            METAL_ROUGHNESS_BASE +
                                MathUtils.random(METAL_ROUGHNESS_MIN, METAL_ROUGHNESS_MAX)
                        ).coerceIn(METAL_ROUGHNESS_FLOOR, 1f)
                    roughness.setColor(roughnessValue, roughnessValue, roughnessValue, 1f)
                    roughness.drawPixel(x, y)
                }
            }
        }

    fun createConcreteTextureSet(base: Color): SurfaceTextureSet =
        context.buildSquareTextureSet(METAL_TEXTURE_BASE, METAL_TEXTURE_MINIMUM) { diffuse, roughness, textureSize ->
            for (x in 0 until textureSize) {
                for (y in 0 until textureSize) {
                    val noise = MathUtils.random(CONCRETE_NOISE_MIN, CONCRETE_NOISE_MAX)
                    diffuse.setColor(
                        (base.r + noise).coerceIn(0f, 1f),
                        (base.g + noise).coerceIn(0f, 1f),
                        (base.b + noise).coerceIn(0f, 1f),
                        1f,
                    )
                    diffuse.drawPixel(x, y)
                    val roughnessValue =
                        (
                            CONCRETE_ROUGHNESS_BASE +
                                MathUtils.random(CONCRETE_ROUGHNESS_MIN, CONCRETE_ROUGHNESS_MAX)
                        ).coerceIn(STANDARD_ROUGHNESS_FLOOR, 1f)
                    roughness.setColor(roughnessValue, roughnessValue, roughnessValue, 1f)
                    roughness.drawPixel(x, y)
                }
            }
        }
}
