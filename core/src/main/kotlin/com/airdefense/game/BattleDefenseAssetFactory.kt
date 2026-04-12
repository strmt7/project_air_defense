package com.airdefense.game

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.BoxShapeBuilder

private val LAUNCHER_METAL_BASE = Color.valueOf("2E3D30FF")
private val RADAR_METAL_BASE = Color.valueOf("38523DFF")
private val LAUNCHER_SPECULAR_COLOR = Color.valueOf("80908AFF")
private val RADAR_SPECULAR_COLOR = Color.valueOf("8CA399FF")
private const val LAUNCHER_SHININESS = 40f
private const val RADAR_SHININESS = 44f
private const val LAUNCHER_CHASSIS_WIDTH = 48f
private const val LAUNCHER_CHASSIS_HEIGHT = 7f
private const val LAUNCHER_CHASSIS_DEPTH = 72f
private const val LAUNCHER_CAB_WIDTH = 26f
private const val LAUNCHER_CAB_HEIGHT = 14f
private const val LAUNCHER_CAB_DEPTH = 24f
private const val LAUNCHER_CAB_OFFSET_Y = 10f
private const val LAUNCHER_CAB_OFFSET_Z = 24f
private const val LAUNCHER_TUBE_WIDTH = 6f
private const val LAUNCHER_TUBE_HEIGHT = 6f
private const val LAUNCHER_TUBE_DEPTH = 44f
private const val LAUNCHER_TUBE_START_X = -12f
private const val LAUNCHER_TUBE_SPACING_X = 8f
private const val LAUNCHER_TUBE_OFFSET_Y = 20f
private const val LAUNCHER_TUBE_OFFSET_Z = -10f
private const val LAUNCHER_TUBE_COUNT = 4
private const val RADAR_BASE_SIZE = 56f
private const val RADAR_BASE_HEIGHT = 10f
private const val RADAR_FACE_WIDTH = 82f
private const val RADAR_FACE_HEIGHT = 46f
private const val RADAR_FACE_DEPTH = 8f
private const val RADAR_FACE_OFFSET_Y = 36f
private const val RADAR_FACE_OFFSET_Z = -10f

internal class BattleDefenseAssetFactory(
    private val surfaceTextures: BattleSurfaceTextureFactory,
) {
    fun build(): List<NamedModel> {
        val attr = defaultVertexAttributes()
        val launcherMaterial = createMetalMaterial(LAUNCHER_METAL_BASE, LAUNCHER_SPECULAR_COLOR, LAUNCHER_SHININESS)
        val radarMaterial = createMetalMaterial(RADAR_METAL_BASE, RADAR_SPECULAR_COLOR, RADAR_SHININESS)
        return listOf(
            NamedModel("launcher", createLauncherModel(attr, launcherMaterial)),
            NamedModel("radar", createRadarModel(attr, radarMaterial)),
        )
    }

    private fun createLauncherModel(
        attr: Long,
        material: Material,
    ): Model {
        val modelBuilder = ModelBuilder()
        modelBuilder.begin()
        modelBuilder.part("chassis", GL20.GL_TRIANGLES, attr, material).apply {
            BoxShapeBuilder.build(this, LAUNCHER_CHASSIS_WIDTH, LAUNCHER_CHASSIS_HEIGHT, LAUNCHER_CHASSIS_DEPTH)
        }
        modelBuilder.part("cab", GL20.GL_TRIANGLES, attr, material).apply {
            BoxShapeBuilder.build(
                this,
                LAUNCHER_CAB_WIDTH,
                LAUNCHER_CAB_HEIGHT,
                LAUNCHER_CAB_DEPTH,
                0f,
                LAUNCHER_CAB_OFFSET_Y,
                LAUNCHER_CAB_OFFSET_Z,
            )
        }
        repeat(LAUNCHER_TUBE_COUNT) { index ->
            modelBuilder.part("tube_$index", GL20.GL_TRIANGLES, attr, material).apply {
                BoxShapeBuilder.build(
                    this,
                    LAUNCHER_TUBE_WIDTH,
                    LAUNCHER_TUBE_HEIGHT,
                    LAUNCHER_TUBE_DEPTH,
                    LAUNCHER_TUBE_START_X + index * LAUNCHER_TUBE_SPACING_X,
                    LAUNCHER_TUBE_OFFSET_Y,
                    LAUNCHER_TUBE_OFFSET_Z,
                )
            }
        }
        return modelBuilder.end()
    }

    private fun createRadarModel(
        attr: Long,
        material: Material,
    ): Model {
        val modelBuilder = ModelBuilder()
        modelBuilder.begin()
        modelBuilder.part("base", GL20.GL_TRIANGLES, attr, material).apply {
            BoxShapeBuilder.build(this, RADAR_BASE_SIZE, RADAR_BASE_HEIGHT, RADAR_BASE_SIZE)
        }
        modelBuilder.part("face", GL20.GL_TRIANGLES, attr, material).apply {
            BoxShapeBuilder.build(
                this,
                RADAR_FACE_WIDTH,
                RADAR_FACE_HEIGHT,
                RADAR_FACE_DEPTH,
                0f,
                RADAR_FACE_OFFSET_Y,
                RADAR_FACE_OFFSET_Z,
            )
        }
        return modelBuilder.end()
    }

    private fun createMetalMaterial(
        base: Color,
        specular: Color,
        shininess: Float,
    ): Material {
        val textureSet = surfaceTextures.createMetalTextureSet(base)
        return Material(
            TextureAttribute.createDiffuse(textureSet.diffuse),
            TextureAttribute.createSpecular(textureSet.roughness),
            ColorAttribute.createDiffuse(Color.WHITE),
            ColorAttribute.createSpecular(specular),
            FloatAttribute.createShininess(shininess),
        )
    }

    private fun defaultVertexAttributes(): Long =
        (
            VertexAttributes.Usage.Position or
                VertexAttributes.Usage.Normal or
                VertexAttributes.Usage.TextureCoordinates
        ).toLong()
}
