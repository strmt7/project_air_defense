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
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.ConeShapeBuilder
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.CylinderShapeBuilder
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.SphereShapeBuilder
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3

private val THREAT_METAL_BASE = Color.valueOf("52595FFF")
private val INTERCEPTOR_METAL_BASE = Color.valueOf("E6EBF0FF")
private val DEBRIS_CONCRETE_BASE = Color.valueOf("383D47FF")
private val BLAST_BASE = Color.valueOf("FFD159FF")
private val TRAIL_BASE = Color.valueOf("EBE6D6FF")
private val MOON_BASE = Color.valueOf("E0E6FFFF")
private val THREAT_DIFFUSE_COLOR = Color.valueOf("FFC76BFF")
private val THREAT_SPECULAR_COLOR = Color.valueOf("FFE0A8FF")
private val THREAT_EMISSIVE_COLOR = Color.valueOf("2E1408FF")
private val INTERCEPTOR_DIFFUSE_COLOR = Color.valueOf("D1F5FFFF")
private val INTERCEPTOR_SPECULAR_COLOR = Color.valueOf("F0FAFFFF")
private val INTERCEPTOR_EMISSIVE_COLOR = Color.valueOf("0D1F2EFF")
private val BLAST_EMISSIVE_COLOR = Color.valueOf("FFA633FF")
private val TRAIL_EMISSIVE_COLOR = Color.valueOf("52616FFF")
private val DEBRIS_SPECULAR_COLOR = Color.valueOf("2E2E33FF")
private val MOON_EMISSIVE_COLOR = Color.valueOf("2E333DFF")
private const val THREAT_BODY_DIAMETER = 8f
private const val THREAT_BODY_HEIGHT = 34f
private const val THREAT_NOSE_DIAMETER = 8.8f
private const val THREAT_NOSE_HEIGHT = 13f
private const val THREAT_NOSE_OFFSET_Y = 22f
private const val THREAT_FIN_WIDTH = 8f
private const val THREAT_FIN_HEIGHT = 10f
private const val THREAT_FIN_DEPTH = 0.8f
private const val THREAT_FIN_OFFSET_Y = -10f
private const val THREAT_SHININESS = 78f
private const val THREAT_PART_DIVISIONS = 20
private const val INTERCEPTOR_BODY_DIAMETER = 5.4f
private const val INTERCEPTOR_BODY_HEIGHT = 32f
private const val INTERCEPTOR_NOSE_DIAMETER = 5.8f
private const val INTERCEPTOR_NOSE_HEIGHT = 11f
private const val INTERCEPTOR_NOSE_OFFSET_Y = 21f
private const val INTERCEPTOR_FIN_WIDTH = 5f
private const val INTERCEPTOR_FIN_HEIGHT = 6f
private const val INTERCEPTOR_FIN_DEPTH = 0.55f
private const val INTERCEPTOR_FIN_OFFSET_Y = -10f
private const val INTERCEPTOR_FIN_YAW_OFFSET = 45f
private const val INTERCEPTOR_SHININESS = 92f
private const val BLAST_SIZE = 1f
private const val BLAST_DIVISIONS_U = 16
private const val BLAST_DIVISIONS_V = 12
private const val BLAST_BLEND_ALPHA = 0.92f
private const val TRAIL_SIZE = 1f
private const val TRAIL_DIVISIONS_U = 10
private const val TRAIL_DIVISIONS_V = 8
private const val TRAIL_BLEND_ALPHA = 0.55f
private const val DEBRIS_SIZE = 6f
private const val DEBRIS_SHININESS = 10f
private const val MOON_SIZE = 80f
private const val MOON_DIVISIONS_U = 18
private const val MOON_DIVISIONS_V = 18
private const val SOLID_TRAIL_ROUGHNESS = 0.8f
private const val SOLID_BLAST_ROUGHNESS = 0.12f
private const val SOLID_MOON_ROUGHNESS = 0.65f
private const val QUARTER_ROTATION_DEGREES = 90f
private const val FIN_COUNT = 4

internal class BattleProjectileAssetFactory(
    private val qualityProfile: GraphicsQualityProfile,
    private val surfaceTextures: BattleSurfaceTextureFactory,
) {
    fun build(): List<NamedModel> {
        val attr = defaultVertexAttributes()
        val modelBuilder = ModelBuilder()
        val transform = Matrix4()
        val threatSet = surfaceTextures.createMetalTextureSet(THREAT_METAL_BASE)
        val interceptorSet = surfaceTextures.createMetalTextureSet(INTERCEPTOR_METAL_BASE)
        val debrisSet = surfaceTextures.createConcreteTextureSet(DEBRIS_CONCRETE_BASE)
        val blastSet = surfaceTextures.createSolidTextureSet(BLAST_BASE, SOLID_BLAST_ROUGHNESS)
        val trailSet = surfaceTextures.createSolidTextureSet(TRAIL_BASE, SOLID_TRAIL_ROUGHNESS)
        val moonSet = surfaceTextures.createSolidTextureSet(MOON_BASE, SOLID_MOON_ROUGHNESS)

        val models =
            mutableListOf(
                createThreatModel(modelBuilder, attr, transform, threatSet),
                createInterceptorModel(modelBuilder, attr, transform, interceptorSet),
                createBlastModel(modelBuilder, attr, blastSet),
                createTrailModel(modelBuilder, attr, trailSet),
                createDebrisModel(modelBuilder, attr, debrisSet),
            )
        if (qualityProfile.showMoon) {
            models += createMoonModel(modelBuilder, attr, moonSet)
        }
        return models
    }

    private fun createThreatModel(
        modelBuilder: ModelBuilder,
        attr: Long,
        transform: Matrix4,
        threatSet: SurfaceTextureSet,
    ): NamedModel {
        modelBuilder.begin()
        val threatMaterial =
            Material(
                TextureAttribute.createDiffuse(threatSet.diffuse),
                TextureAttribute.createSpecular(threatSet.roughness),
                ColorAttribute.createDiffuse(THREAT_DIFFUSE_COLOR),
                ColorAttribute.createSpecular(THREAT_SPECULAR_COLOR),
                ColorAttribute.createEmissive(THREAT_EMISSIVE_COLOR),
                FloatAttribute.createShininess(THREAT_SHININESS),
            )
        modelBuilder.part("body", GL20.GL_TRIANGLES, attr, threatMaterial).apply {
            CylinderShapeBuilder.build(this, THREAT_BODY_DIAMETER, THREAT_BODY_HEIGHT, THREAT_BODY_DIAMETER, THREAT_PART_DIVISIONS)
        }
        modelBuilder.part("nose", GL20.GL_TRIANGLES, attr, threatMaterial).apply {
            transform.idt().translate(0f, THREAT_NOSE_OFFSET_Y, 0f)
            setVertexTransform(transform)
            ConeShapeBuilder.build(this, THREAT_NOSE_DIAMETER, THREAT_NOSE_HEIGHT, THREAT_NOSE_DIAMETER, THREAT_PART_DIVISIONS)
        }
        repeat(FIN_COUNT) { index ->
            modelBuilder.part("fin_$index", GL20.GL_TRIANGLES, attr, threatMaterial).apply {
                val yaw = index * QUARTER_ROTATION_DEGREES
                transform.idt().rotate(Vector3.Y, yaw)
                transform.translate(0f, THREAT_FIN_OFFSET_Y, 0f)
                setVertexTransform(transform)
                BoxShapeBuilder.build(this, THREAT_FIN_WIDTH, THREAT_FIN_HEIGHT, THREAT_FIN_DEPTH)
            }
        }
        return NamedModel("threat", modelBuilder.end())
    }

    private fun createInterceptorModel(
        modelBuilder: ModelBuilder,
        attr: Long,
        transform: Matrix4,
        interceptorSet: SurfaceTextureSet,
    ): NamedModel {
        modelBuilder.begin()
        val interceptorMaterial =
            Material(
                TextureAttribute.createDiffuse(interceptorSet.diffuse),
                TextureAttribute.createSpecular(interceptorSet.roughness),
                ColorAttribute.createDiffuse(INTERCEPTOR_DIFFUSE_COLOR),
                ColorAttribute.createSpecular(INTERCEPTOR_SPECULAR_COLOR),
                ColorAttribute.createEmissive(INTERCEPTOR_EMISSIVE_COLOR),
                FloatAttribute.createShininess(INTERCEPTOR_SHININESS),
            )
        modelBuilder.part("body", GL20.GL_TRIANGLES, attr, interceptorMaterial).apply {
            CylinderShapeBuilder.build(
                this,
                INTERCEPTOR_BODY_DIAMETER,
                INTERCEPTOR_BODY_HEIGHT,
                INTERCEPTOR_BODY_DIAMETER,
                THREAT_PART_DIVISIONS,
            )
        }
        modelBuilder.part("nose", GL20.GL_TRIANGLES, attr, interceptorMaterial).apply {
            transform.idt().translate(0f, INTERCEPTOR_NOSE_OFFSET_Y, 0f)
            setVertexTransform(transform)
            ConeShapeBuilder.build(
                this,
                INTERCEPTOR_NOSE_DIAMETER,
                INTERCEPTOR_NOSE_HEIGHT,
                INTERCEPTOR_NOSE_DIAMETER,
                THREAT_PART_DIVISIONS,
            )
        }
        repeat(FIN_COUNT) { index ->
            modelBuilder.part("fin_$index", GL20.GL_TRIANGLES, attr, interceptorMaterial).apply {
                val yaw = index * QUARTER_ROTATION_DEGREES + INTERCEPTOR_FIN_YAW_OFFSET
                transform.idt().rotate(Vector3.Y, yaw)
                transform.translate(0f, INTERCEPTOR_FIN_OFFSET_Y, 0f)
                setVertexTransform(transform)
                BoxShapeBuilder.build(this, INTERCEPTOR_FIN_WIDTH, INTERCEPTOR_FIN_HEIGHT, INTERCEPTOR_FIN_DEPTH)
            }
        }
        return NamedModel("interceptor", modelBuilder.end())
    }

    private fun createBlastModel(
        modelBuilder: ModelBuilder,
        attr: Long,
        blastSet: SurfaceTextureSet,
    ): NamedModel =
        NamedModel(
            "blast",
            modelBuilder.createSphere(
                BLAST_SIZE,
                BLAST_SIZE,
                BLAST_SIZE,
                BLAST_DIVISIONS_U,
                BLAST_DIVISIONS_V,
                Material(
                    TextureAttribute.createDiffuse(blastSet.diffuse),
                    TextureAttribute.createSpecular(blastSet.roughness),
                    ColorAttribute.createDiffuse(Color.WHITE),
                    ColorAttribute.createSpecular(Color.WHITE),
                    ColorAttribute.createEmissive(BLAST_EMISSIVE_COLOR),
                    BlendingAttribute(BLAST_BLEND_ALPHA),
                ),
                attr,
            ),
        )

    private fun createTrailModel(
        modelBuilder: ModelBuilder,
        attr: Long,
        trailSet: SurfaceTextureSet,
    ): NamedModel =
        NamedModel(
            "trail",
            modelBuilder.createSphere(
                TRAIL_SIZE,
                TRAIL_SIZE,
                TRAIL_SIZE,
                TRAIL_DIVISIONS_U,
                TRAIL_DIVISIONS_V,
                Material(
                    TextureAttribute.createDiffuse(trailSet.diffuse),
                    TextureAttribute.createSpecular(trailSet.roughness),
                    ColorAttribute.createDiffuse(Color.WHITE),
                    ColorAttribute.createEmissive(TRAIL_EMISSIVE_COLOR),
                    BlendingAttribute(TRAIL_BLEND_ALPHA),
                ),
                attr,
            ),
        )

    private fun createDebrisModel(
        modelBuilder: ModelBuilder,
        attr: Long,
        debrisSet: SurfaceTextureSet,
    ): NamedModel =
        NamedModel(
            "debris",
            modelBuilder.createBox(
                DEBRIS_SIZE,
                DEBRIS_SIZE,
                DEBRIS_SIZE,
                Material(
                    TextureAttribute.createDiffuse(debrisSet.diffuse),
                    TextureAttribute.createSpecular(debrisSet.roughness),
                    ColorAttribute.createDiffuse(Color.WHITE),
                    ColorAttribute.createSpecular(DEBRIS_SPECULAR_COLOR),
                    FloatAttribute.createShininess(DEBRIS_SHININESS),
                ),
                attr,
            ),
        )

    private fun createMoonModel(
        modelBuilder: ModelBuilder,
        attr: Long,
        moonSet: SurfaceTextureSet,
    ): NamedModel =
        NamedModel(
            "moon",
            modelBuilder.createSphere(
                MOON_SIZE,
                MOON_SIZE,
                MOON_SIZE,
                MOON_DIVISIONS_U,
                MOON_DIVISIONS_V,
                Material(
                    TextureAttribute.createDiffuse(moonSet.diffuse),
                    TextureAttribute.createSpecular(moonSet.roughness),
                    ColorAttribute.createDiffuse(Color.WHITE),
                    ColorAttribute.createEmissive(MOON_EMISSIVE_COLOR),
                ),
                attr,
            ),
        )

    private fun defaultVertexAttributes(): Long =
        (
            VertexAttributes.Usage.Position or
                VertexAttributes.Usage.Normal or
                VertexAttributes.Usage.TextureCoordinates
        ).toLong()
}
