package com.airdefense.game

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute
import com.badlogic.gdx.graphics.g3d.loader.ObjLoader
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap

private const val IMPORTED_LANDMARK_MODEL_KEY = "engel_house"
private const val IMPORTED_LANDMARK_SHININESS = 14f
private val IMPORTED_LANDMARK_DIFFUSE = Color.valueOf("F0EBE0FF")
private val IMPORTED_LANDMARK_SPECULAR = Color.valueOf("2E2E33FF")

internal data class BattleWorldBootstrapCollections(
    val models: ObjectMap<String, Model>,
    val instances: Array<ModelInstance>,
    val launchers: Array<ModelInstance>,
    val cityBlocks: Array<BuildingEntity>,
    val cityBlocksById: ObjectMap<String, BuildingEntity>,
)

internal data class BattleWorldBootstrapConfig(
    val settings: DefenseSettings,
    val benchmarkSeed: Long?,
    val useImportedLandmarks: Boolean,
)

internal class BattleWorldBootstrapper(
    private val collections: BattleWorldBootstrapCollections,
    private val config: BattleWorldBootstrapConfig,
) {
    fun loadImportedModels() {
        if (!config.useImportedLandmarks) return
        val engelHouseFile = Gdx.files.internal("models/engel_house.obj")
        if (!engelHouseFile.exists()) return
        val engelHouse = ObjLoader().loadModel(engelHouseFile)
        engelHouse.materials.forEach { material ->
            material.set(ColorAttribute.createDiffuse(IMPORTED_LANDMARK_DIFFUSE))
            material.set(ColorAttribute.createSpecular(IMPORTED_LANDMARK_SPECULAR))
            material.set(FloatAttribute.createShininess(IMPORTED_LANDMARK_SHININESS))
        }
        collections.models.put(IMPORTED_LANDMARK_MODEL_KEY, engelHouse)
    }

    fun createWorld(syncBuildingTransform: (BuildingEntity) -> Unit): BattleSimulation {
        addBaseInstances()
        val launcherPositions = addLaunchers()
        addRadar()
        val buildingDefinitions = addCityBlocks(syncBuildingTransform)
        val simulation = createSimulation(buildingDefinitions, launcherPositions)
        addImportedLandmarks()
        return simulation
    }

    private fun addBaseInstances() {
        collections.instances.add(
            ModelInstance(collections.models.get("ground")).apply {
                transform.setToTranslation(BattleWorldLayout.groundPosition())
            },
        )
        if (collections.models.containsKey("moon")) {
            collections.instances.add(
                ModelInstance(collections.models.get("moon")).apply {
                    transform.setToTranslation(BattleWorldLayout.moonPosition())
                },
            )
        }
    }

    private fun addLaunchers(): List<Vector3> {
        val launcherPositions = BattleWorldLayout.launcherPositions()
        launcherPositions.forEach { position ->
            val launcher =
                ModelInstance(collections.models.get("launcher")).apply {
                    transform.setToTranslation(position)
                }
            collections.launchers.add(launcher)
            collections.instances.add(launcher)
        }
        return launcherPositions
    }

    private fun addRadar() {
        val radarPosition = BattleWorldLayout.radarPosition()
        collections.instances.add(
            ModelInstance(collections.models.get("radar")).apply {
                transform.setToTranslation(radarPosition)
            },
        )
    }

    private fun addCityBlocks(syncBuildingTransform: (BuildingEntity) -> Unit): List<BattleBuildingDefinition> {
        val buildingDefinitions = BattleWorldLayout.buildingDefinitions()
        buildingDefinitions.forEach { definition ->
            val entity =
                BuildingEntity(
                    id = definition.id,
                    instance = ModelInstance(collections.models.get(definition.modelName)),
                    modelName = definition.modelName,
                    position = definition.position.cpy(),
                    yaw = definition.yaw,
                    baseHeight = definition.metrics.baseHeight,
                    width = definition.metrics.width,
                    depth = definition.metrics.depth,
                    integrity = 100f,
                )
            syncBuildingTransform(entity)
            collections.cityBlocks.add(entity)
            collections.cityBlocksById.put(entity.id, entity)
        }
        return buildingDefinitions
    }

    private fun createSimulation(
        buildingDefinitions: List<BattleBuildingDefinition>,
        launcherPositions: List<Vector3>,
    ): BattleSimulation =
        BattleSimulation(
            buildingDefinitions = buildingDefinitions,
            launcherPositions = launcherPositions,
            settings = config.settings,
            random = config.benchmarkSeed?.let(::SeededRandomSource) ?: DefaultRandomSource,
        )

    private fun addImportedLandmarks() {
        if (!config.useImportedLandmarks) return
        BattleWorldLayout.importedLandmarks().forEach { placement ->
            val instance = importedLandmarkInstance(placement) ?: return@forEach
            collections.instances.add(instance)
        }
    }

    private fun importedLandmarkInstance(placement: ImportedLandmarkPlacement): ModelInstance? {
        val model = collections.models.get(placement.key) ?: return null
        val bounds = BoundingBox()
        val center = Vector3()
        val dimensions = Vector3()
        model.calculateBoundingBox(bounds)
        bounds.getCenter(center)
        bounds.getDimensions(dimensions)
        val height = dimensions.y.coerceAtLeast(1f)
        val scale = placement.targetHeight / height
        return ModelInstance(model).apply {
            transform.idt()
            transform.translate(placement.position)
            transform.rotate(Vector3.Y, placement.yaw)
            transform.scale(scale, scale, scale)
            transform.translate(-center.x, -bounds.min.y, -center.z)
        }
    }
}
