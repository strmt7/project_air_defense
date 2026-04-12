package com.airdefense.game

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g3d.Model

data class SurfaceTextureSet(
    val diffuse: Texture,
    val roughness: Texture,
)

data class BattleTerrainAssetBundle(
    val groundModel: Model,
    val skyTexture: Texture,
    val horizonTexture: Texture,
    val fogTexture: Texture?,
    val glowTexture: Texture?,
    val reflectionTexture: Texture?,
)

data class NamedModel(
    val key: String,
    val model: Model,
)
