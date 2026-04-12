package com.airdefense.game

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder

private val FACADE_A_BASE = Color.valueOf("141A24FF")
private val FACADE_A_LIT = Color.valueOf("FFD17AFF")
private val FACADE_B_BASE = Color.valueOf("0D121CFF")
private val FACADE_B_LIT = Color.valueOf("B3E0FFFF")
private val FACADE_C_BASE = Color.valueOf("1A1417FF")
private val FACADE_C_LIT = Color.valueOf("FF9E4DFF")
private val FACADE_D_BASE = Color.valueOf("12171FFF")
private val FACADE_D_LIT = Color.valueOf("8FF5F5FF")
private val FACADE_E_BASE = Color.valueOf("1F1A14FF")
private val FACADE_E_LIT = Color.valueOf("FFE69EFF")
private val BUILDING_SPECULAR_COLOR = Color.valueOf("1F242EFF")
private const val BUILDING_GLOW_RED_GAIN = 0.13f
private const val BUILDING_GLOW_GREEN_GAIN = 0.13f
private const val BUILDING_GLOW_BLUE_GAIN = 0.16f
private const val BUILDING_GLOW_RED_MAX = 0.22f
private const val BUILDING_GLOW_GREEN_MAX = 0.22f
private const val BUILDING_GLOW_BLUE_MAX = 0.28f
private const val BUILDING_SHININESS = 12f
private const val FACADE_TEXTURE_WIDTH = 192
private const val FACADE_TEXTURE_HEIGHT = 384
private const val TOWER_A_WIDTH = 58f
private const val TOWER_A_HEIGHT = 280f
private const val TOWER_A_DEPTH = 58f
private const val TOWER_B_WIDTH = 84f
private const val TOWER_B_HEIGHT = 210f
private const val TOWER_B_DEPTH = 84f
private const val TOWER_C_WIDTH = 120f
private const val TOWER_C_HEIGHT = 130f
private const val TOWER_C_DEPTH = 90f
private const val TOWER_D_WIDTH = 96f
private const val TOWER_D_HEIGHT = 360f
private const val TOWER_D_DEPTH = 74f
private const val TOWER_E_WIDTH = 146f
private const val TOWER_E_HEIGHT = 178f
private const val TOWER_E_DEPTH = 112f
private const val PODIUM_WIDTH = 180f
private const val PODIUM_HEIGHT = 78f
private const val PODIUM_DEPTH = 120f
private const val HOTEL_WIDTH = 132f
private const val HOTEL_HEIGHT = 118f
private const val HOTEL_DEPTH = 72f
private const val COASTAL_SLAB_WIDTH = 228f
private const val COASTAL_SLAB_HEIGHT = 96f
private const val COASTAL_SLAB_DEPTH = 56f
private const val OFFICE_SLAB_WIDTH = 168f
private const val OFFICE_SLAB_HEIGHT = 152f
private const val OFFICE_SLAB_DEPTH = 92f
private const val NEEDLE_TOWER_WIDTH = 44f
private const val NEEDLE_TOWER_HEIGHT = 420f
private const val NEEDLE_TOWER_DEPTH = 44f
private const val SETBACK_TOWER_WIDTH = 118f
private const val SETBACK_TOWER_HEIGHT = 304f
private const val SETBACK_TOWER_DEPTH = 92f
private val TOWER_A_TINT = Color.valueOf("E6F2FFFF")
private val TOWER_B_TINT = Color.valueOf("D1E6FFFF")
private val TOWER_C_TINT = Color.valueOf("FFF2E6FF")
private val TOWER_D_TINT = Color.valueOf("D6FAFFFF")
private val TOWER_E_TINT = Color.valueOf("FFF0DBFF")
private val PODIUM_TINT = Color.valueOf("E6F0FFFF")
private val HOTEL_TINT = Color.valueOf("FFF5E6FF")
private val COASTAL_SLAB_TINT = Color.valueOf("FFF7E8FF")
private val OFFICE_SLAB_TINT = Color.valueOf("DBEBFFFF")
private val NEEDLE_TOWER_TINT = Color.valueOf("D1F5FFFF")
private val SETBACK_TOWER_TINT = Color.valueOf("E6F5FFFF")

private data class FacadePalette(
    val key: String,
    val base: Color,
    val lit: Color,
)

private data class BuildingSpec(
    val key: String,
    val width: Float,
    val height: Float,
    val depth: Float,
    val facadeKey: String,
    val tint: Color,
)

private val FACADE_PALETTES =
    listOf(
        FacadePalette("a", FACADE_A_BASE, FACADE_A_LIT),
        FacadePalette("b", FACADE_B_BASE, FACADE_B_LIT),
        FacadePalette("c", FACADE_C_BASE, FACADE_C_LIT),
        FacadePalette("d", FACADE_D_BASE, FACADE_D_LIT),
        FacadePalette("e", FACADE_E_BASE, FACADE_E_LIT),
    )

private val BUILDING_SPECS =
    listOf(
        BuildingSpec("tower_a", TOWER_A_WIDTH, TOWER_A_HEIGHT, TOWER_A_DEPTH, "a", TOWER_A_TINT),
        BuildingSpec("tower_b", TOWER_B_WIDTH, TOWER_B_HEIGHT, TOWER_B_DEPTH, "b", TOWER_B_TINT),
        BuildingSpec("tower_c", TOWER_C_WIDTH, TOWER_C_HEIGHT, TOWER_C_DEPTH, "c", TOWER_C_TINT),
        BuildingSpec("tower_d", TOWER_D_WIDTH, TOWER_D_HEIGHT, TOWER_D_DEPTH, "d", TOWER_D_TINT),
        BuildingSpec("tower_e", TOWER_E_WIDTH, TOWER_E_HEIGHT, TOWER_E_DEPTH, "e", TOWER_E_TINT),
        BuildingSpec("podium", PODIUM_WIDTH, PODIUM_HEIGHT, PODIUM_DEPTH, "b", PODIUM_TINT),
        BuildingSpec("hotel", HOTEL_WIDTH, HOTEL_HEIGHT, HOTEL_DEPTH, "c", HOTEL_TINT),
        BuildingSpec("coastal_slab", COASTAL_SLAB_WIDTH, COASTAL_SLAB_HEIGHT, COASTAL_SLAB_DEPTH, "e", COASTAL_SLAB_TINT),
        BuildingSpec("office_slab", OFFICE_SLAB_WIDTH, OFFICE_SLAB_HEIGHT, OFFICE_SLAB_DEPTH, "a", OFFICE_SLAB_TINT),
        BuildingSpec("needle_tower", NEEDLE_TOWER_WIDTH, NEEDLE_TOWER_HEIGHT, NEEDLE_TOWER_DEPTH, "d", NEEDLE_TOWER_TINT),
        BuildingSpec("setback_tower", SETBACK_TOWER_WIDTH, SETBACK_TOWER_HEIGHT, SETBACK_TOWER_DEPTH, "b", SETBACK_TOWER_TINT),
    )

internal class BattleBuildingAssetFactory(
    private val surfaceTextures: BattleSurfaceTextureFactory,
) {
    fun build(): List<NamedModel> {
        val facades = createFacades()
        return BUILDING_SPECS.map { spec ->
            createBuildingModel(spec.copyFacade(facades))
        }
    }

    private fun createFacades(): Map<String, SurfaceTextureSet> =
        FACADE_PALETTES.associate { palette ->
            palette.key to
                surfaceTextures.createFacadeTextureSet(
                    FACADE_TEXTURE_WIDTH,
                    FACADE_TEXTURE_HEIGHT,
                    palette.base,
                    palette.lit,
                )
        }

    private fun createBuildingModel(spec: ResolvedBuildingSpec): NamedModel {
        val glow =
            Color(
                (spec.tint.r * BUILDING_GLOW_RED_GAIN).coerceIn(0f, BUILDING_GLOW_RED_MAX),
                (spec.tint.g * BUILDING_GLOW_GREEN_GAIN).coerceIn(0f, BUILDING_GLOW_GREEN_MAX),
                (spec.tint.b * BUILDING_GLOW_BLUE_GAIN).coerceIn(0f, BUILDING_GLOW_BLUE_MAX),
                1f,
            )
        val model =
            ModelBuilder().createBox(
                spec.width,
                spec.height,
                spec.depth,
                Material(
                    TextureAttribute.createDiffuse(spec.texture.diffuse),
                    TextureAttribute.createSpecular(spec.texture.roughness),
                    ColorAttribute.createDiffuse(spec.tint),
                    ColorAttribute.createEmissive(glow),
                    ColorAttribute.createSpecular(BUILDING_SPECULAR_COLOR),
                    FloatAttribute.createShininess(BUILDING_SHININESS),
                ),
                defaultVertexAttributes(),
            )
        return NamedModel(spec.key, model)
    }

    private fun defaultVertexAttributes(): Long =
        (
            VertexAttributes.Usage.Position or
                VertexAttributes.Usage.Normal or
                VertexAttributes.Usage.TextureCoordinates
        ).toLong()
}

private data class ResolvedBuildingSpec(
    val key: String,
    val width: Float,
    val height: Float,
    val depth: Float,
    val texture: SurfaceTextureSet,
    val tint: Color,
)

private fun BuildingSpec.copyFacade(facades: Map<String, SurfaceTextureSet>): ResolvedBuildingSpec =
    ResolvedBuildingSpec(
        key = key,
        width = width,
        height = height,
        depth = depth,
        texture = facades.getValue(facadeKey),
        tint = tint,
    )
