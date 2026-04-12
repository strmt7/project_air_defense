package com.airdefense.game

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap

internal class BattleAssetBootstrapper(
    private val qualityProfile: GraphicsQualityProfile,
    private val textures: Array<Texture>,
    private val models: ObjectMap<String, Model>,
    private val sceneRenderer: BattleSceneRenderer,
    private val worldBootstrapper: BattleWorldBootstrapper,
) {
    private val textureFactory = BattleTextureFactory(qualityProfile, ::registerTexture)
    private val surfaceTextures by lazy { textureFactory.surfaces }
    private val backdropTextures by lazy { textureFactory.backdrop }
    private val terrainAssetFactory by lazy {
        BattleTerrainAssetFactory(qualityProfile, surfaceTextures, backdropTextures)
    }
    private val buildingAssetFactory by lazy { BattleBuildingAssetFactory(surfaceTextures) }
    private val defenseAssetFactory by lazy { BattleDefenseAssetFactory(surfaceTextures) }
    private val projectileAssetFactory by lazy {
        BattleProjectileAssetFactory(qualityProfile, surfaceTextures)
    }

    fun loadTerrain() {
        val terrainAssets = terrainAssetFactory.build()
        models.put("ground", terrainAssets.groundModel)
        sceneRenderer.updateBackdropTextures(
            skyRegion = TextureRegion(terrainAssets.skyTexture),
            horizonTexture = terrainAssets.horizonTexture,
            glowTexture = terrainAssets.glowTexture,
            reflectionTexture = terrainAssets.reflectionTexture,
            fogTexture = terrainAssets.fogTexture,
        )
    }

    fun loadBuildings() {
        buildingAssetFactory.build().registerAll()
    }

    fun loadDefenseAssets() {
        defenseAssetFactory.build().registerAll()
    }

    fun loadProjectileAssets() {
        projectileAssetFactory.build().registerAll()
    }

    fun loadImportedLandmarks() {
        worldBootstrapper.loadImportedModels()
    }

    fun createWorld(syncBuildingTransform: (BuildingEntity) -> Unit): BattleSimulation =
        worldBootstrapper.createWorld(syncBuildingTransform)

    fun dispose() {
        textures.forEach { it.dispose() }
    }

    private fun registerTexture(
        texture: Texture,
        repeat: Boolean,
    ): Texture {
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        if (repeat) {
            texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat)
        }
        textures.add(texture)
        return texture
    }

    private fun List<NamedModel>.registerAll() {
        forEach { namedModel ->
            models.put(namedModel.key, namedModel.model)
        }
    }
}
