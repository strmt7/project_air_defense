package com.airdefense.game

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture

private const val DEFAULT_MIN_TEXTURE_SIZE = 64
private const val SOLID_TEXTURE_SIZE = 4
private const val TEXTURE_FALLBACK_SIZE = 8

internal class BattleTextureFactory(
    qualityProfile: GraphicsQualityProfile,
    textureRegistrar: (Texture, Boolean) -> Texture,
) {
    private val context = BattleTextureContext(qualityProfile, textureRegistrar)

    val surfaces = BattleSurfaceTextureFactory(context)
    val backdrop = BattleBackdropTextureFactory(context)
}

internal class BattleTextureContext(
    private val qualityProfile: GraphicsQualityProfile,
    private val textureRegistrar: (Texture, Boolean) -> Texture,
) {
    fun loadTexture(
        path: String,
        fallbackColor: Color,
    ): Texture {
        val file = Gdx.files.internal(path)
        if (file.exists()) {
            return registerTexture(Texture(file))
        }
        val pixmap = Pixmap(TEXTURE_FALLBACK_SIZE, TEXTURE_FALLBACK_SIZE, Pixmap.Format.RGBA8888)
        pixmap.setColor(fallbackColor)
        pixmap.fill()
        return createManagedTexture(pixmap).also { pixmap.dispose() }
    }

    fun createSolidTextureSet(
        color: Color,
        roughnessValue: Float,
    ): SurfaceTextureSet {
        val diffuse =
            Pixmap(SOLID_TEXTURE_SIZE, SOLID_TEXTURE_SIZE, Pixmap.Format.RGBA8888).apply {
                setColor(color)
                fill()
            }
        val roughness =
            Pixmap(SOLID_TEXTURE_SIZE, SOLID_TEXTURE_SIZE, Pixmap.Format.RGBA8888).apply {
                setColor(roughnessValue, roughnessValue, roughnessValue, 1f)
                fill()
            }
        return createTextureSet(diffuse, roughness, repeat = false)
    }

    fun buildSquareTextureSet(
        base: Int,
        minimum: Int,
        repeat: Boolean = true,
        painter: (Pixmap, Pixmap, Int) -> Unit,
    ): SurfaceTextureSet {
        val textureSize = scaledSceneTextureSize(base, minimum)
        val diffuse = Pixmap(textureSize, textureSize, Pixmap.Format.RGBA8888)
        val roughness = Pixmap(textureSize, textureSize, Pixmap.Format.RGBA8888)
        painter(diffuse, roughness, textureSize)
        return createTextureSet(diffuse, roughness, repeat)
    }

    fun createTextureSet(
        diffuse: Pixmap,
        roughness: Pixmap,
        repeat: Boolean = true,
    ): SurfaceTextureSet {
        val diffuseTexture = createManagedTexture(diffuse, repeat)
        val roughnessTexture = createManagedTexture(roughness, repeat)
        diffuse.dispose()
        roughness.dispose()
        return SurfaceTextureSet(diffuseTexture, roughnessTexture)
    }

    fun createManagedTexture(
        pixmap: Pixmap,
        repeat: Boolean = false,
    ): Texture = registerTexture(Texture(pixmap), repeat)

    fun scaledSceneTextureSize(
        base: Int,
        minimum: Int = DEFAULT_MIN_TEXTURE_SIZE,
    ): Int = kotlin.math.max(minimum, (base * qualityProfile.sceneTextureScale).toInt())

    private fun registerTexture(
        texture: Texture,
        repeat: Boolean = false,
    ): Texture {
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        if (repeat) {
            texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat)
        }
        return textureRegistrar(texture, repeat)
    }
}
