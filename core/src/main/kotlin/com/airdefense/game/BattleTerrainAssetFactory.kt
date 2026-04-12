package com.airdefense.game

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.BoxShapeBuilder

private val HORIZON_FALLBACK_COLOR = Color.valueOf("14171FFF")
private val INLAND_GROUND_DIFFUSE_COLOR = Color.valueOf("4D5766FF")
private val INLAND_GROUND_SPECULAR_COLOR = Color.valueOf("1F242EFF")
private val SEA_SPECULAR_COLOR = Color.valueOf("70A8D6FF")
private val BEACH_DIFFUSE_COLOR = Color.valueOf("FFF9EBFF")
private val BEACH_SPECULAR_COLOR = Color.valueOf("332E1FFF")
private val PROMENADE_DIFFUSE_COLOR = Color.valueOf("999EA8FF")
private val PROMENADE_SPECULAR_COLOR = Color.valueOf("383D47FF")
private val PARK_SPECULAR_COLOR = Color.valueOf("1F2E19FF")
private val ROAD_DIFFUSE_COLOR = Color.valueOf("575C66FF")
private val ROAD_SPECULAR_COLOR = Color.valueOf("2E2E33FF")
private val GLOW_BAND_COLOR = Color.valueOf("BD752EFF")
private val DEFENSE_PAD_DIFFUSE_COLOR = Color.valueOf("7A808AFF")
private val DEFENSE_PAD_SPECULAR_COLOR = Color.valueOf("2E2E33FF")

private const val DEFAULT_WORLD_MESH_WIDTH = 9600f
private const val INLAND_GROUND_HEIGHT = 3f
private const val INLAND_GROUND_DEPTH = 9800f
private const val INLAND_GROUND_CENTER_X = 1180f
private const val INLAND_GROUND_CENTER_Y = -2f
private const val INLAND_GROUND_CENTER_Z = 220f
private const val INLAND_GROUND_SHININESS = 18f
private const val GROUND_TILE_SCALE = 12f
private const val SEA_DEPTH = 3600f
private const val SEA_HEIGHT = 2f
private const val SEA_CENTER_Y = -4f
private const val SEA_PRIMARY_CENTER_Z = -4580f
private const val SEA_SHELF_WIDTH = 8200f
private const val SEA_SHELF_DEPTH = 1280f
private const val SEA_SHELF_CENTER_Z = -3170f
private const val SEA_SHININESS = 56f
private const val BEACH_WIDTH = 8600f
private const val BEACH_HEIGHT = 4f
private const val BEACH_DEPTH = 760f
private const val BEACH_CENTER_Y = -1f
private const val BEACH_CENTER_Z = -2550f
private const val BEACH_SHININESS = 8f
private const val PROMENADE_WIDTH = 8400f
private const val PROMENADE_HEIGHT = 2f
private const val PROMENADE_DEPTH = 180f
private const val PROMENADE_CENTER_Y = 0f
private const val PROMENADE_CENTER_Z = -2100f
private const val PROMENADE_TILE_SCALE_U = 18f
private const val PROMENADE_TILE_SCALE_V = 2.6f
private const val PROMENADE_SHININESS = 28f
private const val PARK_WIDTH = 1560f
private const val PARK_HEIGHT = 2f
private const val PARK_DEPTH = 3600f
private const val PARK_CENTER_X = 880f
private const val PARK_CENTER_Z = -820f
private const val PARK_SHININESS = 6f
private const val ROAD_WIDTH = 8600f
private const val ROAD_HEIGHT = 1f
private const val ROAD_COASTAL_DEPTH = 120f
private const val ROAD_COASTAL_CENTER_Z = -1910f
private const val ROAD_COASTAL_TILE_SCALE_U = 18f
private const val ROAD_COASTAL_TILE_SCALE_V = 3.2f
private const val ROAD_SECONDARY_WIDTH = 7600f
private const val ROAD_SECONDARY_DEPTH = 48f
private const val ROAD_SECONDARY_CENTER_Z = -640f
private const val ROAD_TERTIARY_WIDTH = 6200f
private const val ROAD_TERTIARY_DEPTH = 42f
private const val ROAD_TERTIARY_CENTER_X = 1480f
private const val ROAD_TERTIARY_CENTER_Z = 180f
private const val VERTICAL_ROAD_ONE_WIDTH = 50f
private const val VERTICAL_ROAD_ONE_DEPTH = 4200f
private const val VERTICAL_ROAD_ONE_CENTER_X = -260f
private const val VERTICAL_ROAD_ONE_CENTER_Z = -620f
private const val VERTICAL_ROAD_TWO_WIDTH = 52f
private const val VERTICAL_ROAD_TWO_DEPTH = 4700f
private const val VERTICAL_ROAD_TWO_CENTER_X = 920f
private const val VERTICAL_ROAD_TWO_CENTER_Z = -320f
private const val VERTICAL_ROAD_THREE_WIDTH = 48f
private const val VERTICAL_ROAD_THREE_DEPTH = 4400f
private const val VERTICAL_ROAD_THREE_CENTER_X = 2140f
private const val VERTICAL_ROAD_THREE_CENTER_Z = -460f
private const val ROAD_SHININESS = 22f
private const val GLOW_BAND_HEIGHT = 0.5f
private const val GLOW_BAND_PRIMARY_DEPTH = 260f
private const val GLOW_BAND_PRIMARY_CENTER_Y = 5f
private const val GLOW_BAND_PRIMARY_CENTER_Z = -2070f
private const val GLOW_BAND_SECONDARY_DEPTH = 620f
private const val GLOW_BAND_SECONDARY_CENTER_Y = 4f
private const val GLOW_BAND_SECONDARY_CENTER_Z = -1820f
private const val GLOW_BAND_ALPHA = 0.2f
private const val DEFENSE_PAD_WIDTH = 2520f
private const val DEFENSE_PAD_HEIGHT = 14f
private const val DEFENSE_PAD_DEPTH = 620f
private const val DEFENSE_PAD_CENTER_X = 520f
private const val DEFENSE_PAD_CENTER_Y = 5f
private const val DEFENSE_PAD_CENTER_Z = 310f
private const val DEFENSE_PAD_TILE_SCALE_U = 4f
private const val DEFENSE_PAD_TILE_SCALE_V = 2f
private const val DEFENSE_PAD_SHININESS = 26f

private data class TerrainTextures(
    val ground: SurfaceTextureSet,
    val sea: SurfaceTextureSet,
    val beach: SurfaceTextureSet,
    val park: SurfaceTextureSet,
    val promenade: SurfaceTextureSet,
    val road: SurfaceTextureSet,
    val skyTexture: com.badlogic.gdx.graphics.Texture,
    val horizonTexture: com.badlogic.gdx.graphics.Texture,
    val fogTexture: com.badlogic.gdx.graphics.Texture?,
    val glowTexture: com.badlogic.gdx.graphics.Texture?,
    val reflectionTexture: com.badlogic.gdx.graphics.Texture?,
)

internal class BattleTerrainAssetFactory(
    private val qualityProfile: GraphicsQualityProfile,
    private val surfaceTextures: BattleSurfaceTextureFactory,
    private val backdropTextures: BattleBackdropTextureFactory,
) {
    fun build(): BattleTerrainAssetBundle {
        val textures = createTerrainTextures()
        val modelBuilder = ModelBuilder()
        val attr = defaultVertexAttributes()
        modelBuilder.begin()
        createInlandGround(modelBuilder, attr, textures)
        createSea(modelBuilder, attr, textures)
        createBeach(modelBuilder, attr, textures)
        createPromenade(modelBuilder, attr, textures)
        createPark(modelBuilder, attr, textures)
        createRoads(modelBuilder, attr, textures)
        createGlowBand(modelBuilder, attr)
        createDefensePad(modelBuilder, attr, textures)
        return BattleTerrainAssetBundle(
            groundModel = modelBuilder.end(),
            skyTexture = textures.skyTexture,
            horizonTexture = textures.horizonTexture,
            fogTexture = textures.fogTexture,
            glowTexture = textures.glowTexture,
            reflectionTexture = textures.reflectionTexture,
        )
    }

    private fun createTerrainTextures(): TerrainTextures =
        TerrainTextures(
            ground = surfaceTextures.createGroundTextureSet(),
            sea = surfaceTextures.createSeaTextureSet(),
            beach = surfaceTextures.createBeachTextureSet(),
            park = surfaceTextures.createParkTextureSet(),
            promenade = surfaceTextures.createPromenadeTextureSet(),
            road = surfaceTextures.createRoadTextureSet(),
            skyTexture = backdropTextures.createSkyTexture(),
            horizonTexture = backdropTextures.loadTexture(HORIZON_TEXTURE_PATH, HORIZON_FALLBACK_COLOR),
            fogTexture = if (qualityProfile.showAtmosphereLayers) backdropTextures.createFogTexture() else null,
            glowTexture = if (qualityProfile.showGlowLayer) backdropTextures.createGlowTexture() else null,
            reflectionTexture = if (qualityProfile.showReflectionLayer) backdropTextures.createReflectionTexture() else null,
        )

    private fun createInlandGround(
        modelBuilder: ModelBuilder,
        attr: Long,
        textures: TerrainTextures,
    ) {
        val part =
            modelBuilder.part(
                "inland_ground",
                GL20.GL_TRIANGLES,
                attr,
                Material(
                    surfaceTextures.createTiledAttribute(textures.ground.diffuse, GROUND_TILE_SCALE, GROUND_TILE_SCALE),
                    TextureAttribute.createSpecular(textures.ground.roughness),
                    ColorAttribute.createDiffuse(INLAND_GROUND_DIFFUSE_COLOR),
                    ColorAttribute.createSpecular(INLAND_GROUND_SPECULAR_COLOR),
                    FloatAttribute.createShininess(INLAND_GROUND_SHININESS),
                ),
            )
        BoxShapeBuilder.build(
            part,
            DEFAULT_WORLD_MESH_WIDTH,
            INLAND_GROUND_HEIGHT,
            INLAND_GROUND_DEPTH,
            INLAND_GROUND_CENTER_X,
            INLAND_GROUND_CENTER_Y,
            INLAND_GROUND_CENTER_Z,
        )
    }

    private fun createSea(
        modelBuilder: ModelBuilder,
        attr: Long,
        textures: TerrainTextures,
    ) {
        val part =
            modelBuilder.part(
                "sea",
                GL20.GL_TRIANGLES,
                attr,
                Material(
                    TextureAttribute.createDiffuse(textures.sea.diffuse),
                    TextureAttribute.createSpecular(textures.sea.roughness),
                    ColorAttribute.createDiffuse(Color.WHITE),
                    ColorAttribute.createSpecular(SEA_SPECULAR_COLOR),
                    FloatAttribute.createShininess(SEA_SHININESS),
                ),
            )
        BoxShapeBuilder.build(
            part,
            DEFAULT_WORLD_MESH_WIDTH,
            SEA_HEIGHT,
            SEA_DEPTH,
            INLAND_GROUND_CENTER_X,
            SEA_CENTER_Y,
            SEA_PRIMARY_CENTER_Z,
        )
        BoxShapeBuilder.build(
            part,
            SEA_SHELF_WIDTH,
            ROAD_HEIGHT,
            SEA_SHELF_DEPTH,
            INLAND_GROUND_CENTER_X,
            SEA_CENTER_Y,
            SEA_SHELF_CENTER_Z,
        )
    }

    private fun createBeach(
        modelBuilder: ModelBuilder,
        attr: Long,
        textures: TerrainTextures,
    ) {
        val part =
            modelBuilder.part(
                "beach",
                GL20.GL_TRIANGLES,
                attr,
                Material(
                    TextureAttribute.createDiffuse(textures.beach.diffuse),
                    TextureAttribute.createSpecular(textures.beach.roughness),
                    ColorAttribute.createDiffuse(BEACH_DIFFUSE_COLOR),
                    ColorAttribute.createSpecular(BEACH_SPECULAR_COLOR),
                    FloatAttribute.createShininess(BEACH_SHININESS),
                ),
            )
        BoxShapeBuilder.build(
            part,
            BEACH_WIDTH,
            BEACH_HEIGHT,
            BEACH_DEPTH,
            INLAND_GROUND_CENTER_X,
            BEACH_CENTER_Y,
            BEACH_CENTER_Z,
        )
    }

    private fun createPromenade(
        modelBuilder: ModelBuilder,
        attr: Long,
        textures: TerrainTextures,
    ) {
        val part =
            modelBuilder.part(
                "promenade",
                GL20.GL_TRIANGLES,
                attr,
                Material(
                    surfaceTextures.createTiledAttribute(
                        textures.promenade.diffuse,
                        PROMENADE_TILE_SCALE_U,
                        PROMENADE_TILE_SCALE_V,
                    ),
                    TextureAttribute.createSpecular(textures.promenade.roughness),
                    ColorAttribute.createDiffuse(PROMENADE_DIFFUSE_COLOR),
                    ColorAttribute.createSpecular(PROMENADE_SPECULAR_COLOR),
                    FloatAttribute.createShininess(PROMENADE_SHININESS),
                ),
            )
        BoxShapeBuilder.build(
            part,
            PROMENADE_WIDTH,
            PROMENADE_HEIGHT,
            PROMENADE_DEPTH,
            INLAND_GROUND_CENTER_X,
            PROMENADE_CENTER_Y,
            PROMENADE_CENTER_Z,
        )
    }

    private fun createPark(
        modelBuilder: ModelBuilder,
        attr: Long,
        textures: TerrainTextures,
    ) {
        val part =
            modelBuilder.part(
                "park",
                GL20.GL_TRIANGLES,
                attr,
                Material(
                    TextureAttribute.createDiffuse(textures.park.diffuse),
                    TextureAttribute.createSpecular(textures.park.roughness),
                    ColorAttribute.createDiffuse(Color.WHITE),
                    ColorAttribute.createSpecular(PARK_SPECULAR_COLOR),
                    FloatAttribute.createShininess(PARK_SHININESS),
                ),
            )
        BoxShapeBuilder.build(
            part,
            PARK_WIDTH,
            PARK_HEIGHT,
            PARK_DEPTH,
            PARK_CENTER_X,
            BEACH_CENTER_Y,
            PARK_CENTER_Z,
        )
    }

    private fun createRoads(
        modelBuilder: ModelBuilder,
        attr: Long,
        textures: TerrainTextures,
    ) {
        val part =
            modelBuilder.part(
                "road",
                GL20.GL_TRIANGLES,
                attr,
                Material(
                    surfaceTextures.createTiledAttribute(
                        textures.road.diffuse,
                        ROAD_COASTAL_TILE_SCALE_U,
                        ROAD_COASTAL_TILE_SCALE_V,
                    ),
                    TextureAttribute.createSpecular(textures.road.roughness),
                    ColorAttribute.createDiffuse(ROAD_DIFFUSE_COLOR),
                    ColorAttribute.createSpecular(ROAD_SPECULAR_COLOR),
                    FloatAttribute.createShininess(ROAD_SHININESS),
                ),
            )
        buildRoadBox(part, ROAD_WIDTH, ROAD_COASTAL_DEPTH, INLAND_GROUND_CENTER_X, ROAD_COASTAL_CENTER_Z)
        buildRoadBox(part, ROAD_SECONDARY_WIDTH, ROAD_SECONDARY_DEPTH, INLAND_GROUND_CENTER_X, ROAD_SECONDARY_CENTER_Z)
        buildRoadBox(part, ROAD_TERTIARY_WIDTH, ROAD_TERTIARY_DEPTH, ROAD_TERTIARY_CENTER_X, ROAD_TERTIARY_CENTER_Z)
        buildRoadBox(part, VERTICAL_ROAD_ONE_WIDTH, VERTICAL_ROAD_ONE_DEPTH, VERTICAL_ROAD_ONE_CENTER_X, VERTICAL_ROAD_ONE_CENTER_Z)
        buildRoadBox(part, VERTICAL_ROAD_TWO_WIDTH, VERTICAL_ROAD_TWO_DEPTH, VERTICAL_ROAD_TWO_CENTER_X, VERTICAL_ROAD_TWO_CENTER_Z)
        buildRoadBox(part, VERTICAL_ROAD_THREE_WIDTH, VERTICAL_ROAD_THREE_DEPTH, VERTICAL_ROAD_THREE_CENTER_X, VERTICAL_ROAD_THREE_CENTER_Z)
    }

    private fun createGlowBand(
        modelBuilder: ModelBuilder,
        attr: Long,
    ) {
        val part =
            modelBuilder.part(
                "glow",
                GL20.GL_TRIANGLES,
                attr,
                Material(
                    ColorAttribute.createDiffuse(GLOW_BAND_COLOR),
                    BlendingAttribute(GLOW_BAND_ALPHA),
                ),
            )
        BoxShapeBuilder.build(
            part,
            PROMENADE_WIDTH,
            GLOW_BAND_HEIGHT,
            GLOW_BAND_PRIMARY_DEPTH,
            INLAND_GROUND_CENTER_X,
            GLOW_BAND_PRIMARY_CENTER_Y,
            GLOW_BAND_PRIMARY_CENTER_Z,
        )
        BoxShapeBuilder.build(
            part,
            BEACH_WIDTH,
            GLOW_BAND_HEIGHT,
            GLOW_BAND_SECONDARY_DEPTH,
            INLAND_GROUND_CENTER_X,
            GLOW_BAND_SECONDARY_CENTER_Y,
            GLOW_BAND_SECONDARY_CENTER_Z,
        )
    }

    private fun createDefensePad(
        modelBuilder: ModelBuilder,
        attr: Long,
        textures: TerrainTextures,
    ) {
        val part =
            modelBuilder.part(
                "defense_pad",
                GL20.GL_TRIANGLES,
                attr,
                Material(
                    surfaceTextures.createTiledAttribute(
                        textures.ground.diffuse,
                        DEFENSE_PAD_TILE_SCALE_U,
                        DEFENSE_PAD_TILE_SCALE_V,
                    ),
                    TextureAttribute.createSpecular(textures.ground.roughness),
                    ColorAttribute.createDiffuse(DEFENSE_PAD_DIFFUSE_COLOR),
                    ColorAttribute.createSpecular(DEFENSE_PAD_SPECULAR_COLOR),
                    FloatAttribute.createShininess(DEFENSE_PAD_SHININESS),
                ),
            )
        BoxShapeBuilder.build(
            part,
            DEFENSE_PAD_WIDTH,
            DEFENSE_PAD_HEIGHT,
            DEFENSE_PAD_DEPTH,
            DEFENSE_PAD_CENTER_X,
            DEFENSE_PAD_CENTER_Y,
            DEFENSE_PAD_CENTER_Z,
        )
    }

    private fun buildRoadBox(
        part: com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder,
        width: Float,
        depth: Float,
        centerX: Float,
        centerZ: Float,
    ) {
        BoxShapeBuilder.build(part, width, ROAD_HEIGHT, depth, centerX, PROMENADE_CENTER_Y, centerZ)
    }

    private fun defaultVertexAttributes(): Long =
        (
            VertexAttributes.Usage.Position or
                VertexAttributes.Usage.Normal or
                VertexAttributes.Usage.TextureCoordinates
        ).toLong()

    companion object {
        const val HORIZON_TEXTURE_PATH = "textures/city_backdrop_telaviv.jpg"
    }
}
