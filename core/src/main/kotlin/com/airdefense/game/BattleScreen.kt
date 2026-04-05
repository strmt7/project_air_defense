package com.airdefense.game

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.physics.bullet.Bullet
import com.badlogic.gdx.physics.bullet.collision.btCollisionDispatcher
import com.badlogic.gdx.physics.bullet.collision.btDbvtBroadphase
import com.badlogic.gdx.physics.bullet.collision.btDefaultCollisionConfiguration
import com.badlogic.gdx.physics.bullet.dynamics.btDiscreteDynamicsWorld
import com.badlogic.gdx.physics.bullet.dynamics.btSequentialImpulseConstraintSolver
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.viewport.ScreenViewport
import kotlin.math.max

class BattleScreen : ScreenAdapter() {
    private val modelBatch = ModelBatch()
    private val environment = Environment().apply {
        set(ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.45f, 1f))
        add(DirectionalLight().set(Color.WHITE, -1f, -0.8f, -0.2f))
    }

    private val camera = PerspectiveCamera(62f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat()).apply {
        near = 0.1f
        far = 900f
        position.set(0f, 55f, 145f)
        lookAt(0f, 18f, 0f)
        update()
    }

    private val groundModel: Model
    private val launcherModel: Model
    private val incomingModel: Model
    private val interceptorModel: Model

    private val groundInstance: ModelInstance
    private val launcherInstance: ModelInstance

    private val incomingMissiles = Array<MissileEntity>()
    private val interceptors = Array<MissileEntity>()

    private val stage = Stage(ScreenViewport())
    private val skin = Skin(Gdx.files.classpath("com/badlogic/gdx/scenes/scene2d/ui/skin/uiskin.json"))
    private val statusLabel = Label("", skin)

    private var interceptCooldown = 0f
    private var spawnTimer = 0f
    private var wave = 1
    private var threatsNeutralized = 0

    private val gravity = Vector3(0f, -9.81f, 0f)

    private val collisionConfig = btDefaultCollisionConfiguration()
    private val dispatcher = btCollisionDispatcher(collisionConfig)
    private val broadphase = btDbvtBroadphase()
    private val solver = btSequentialImpulseConstraintSolver()
    private val world = btDiscreteDynamicsWorld(dispatcher, broadphase, solver, collisionConfig).apply {
        setGravity(gravity)
    }

    init {
        Bullet.init()
        val modelBuilder = ModelBuilder()

        groundModel = modelBuilder.createBox(
            340f,
            1f,
            260f,
            Material(ColorAttribute.createDiffuse(Color(0.15f, 0.28f, 0.16f, 1f))),
            (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong(),
        )

        launcherModel = modelBuilder.createCylinder(
            8f,
            5f,
            8f,
            20,
            Material(ColorAttribute.createDiffuse(Color(0.32f, 0.35f, 0.4f, 1f))),
            (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong(),
        )

        incomingModel = modelBuilder.createSphere(
            2.5f,
            2.5f,
            10f,
            24,
            24,
            Material(ColorAttribute.createDiffuse(Color(0.72f, 0.16f, 0.16f, 1f))),
            (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong(),
        )

        interceptorModel = modelBuilder.createCapsule(
            0.9f,
            6.2f,
            20,
            Material(ColorAttribute.createDiffuse(Color(0.2f, 0.65f, 0.92f, 1f))),
            (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong(),
        )

        groundInstance = ModelInstance(groundModel).apply { transform.setToTranslation(0f, -0.5f, 0f) }
        launcherInstance = ModelInstance(launcherModel).apply { transform.setToTranslation(0f, 2.5f, 0f) }

        setupHud()
        Gdx.input.inputProcessor = stage
    }

    private fun setupHud() {
        val layout = Table().apply {
            setFillParent(true)
            defaults().pad(8f)
        }

        val fireButton = TextButton("Launch Interceptor", skin).apply {
            label.setFontScale(1.3f)
            addListener { event ->
                if (isPressed) {
                    launchInterceptor()
                }
                false
            }
        }

        val rotateLeft = TextButton("< Radar", skin).apply {
            addListener {
                if (isPressed) {
                    camera.rotateAround(Vector3.Zero, Vector3.Y, 35f * Gdx.graphics.deltaTime)
                    camera.lookAt(0f, 15f, 0f)
                    camera.update()
                }
                false
            }
        }

        val rotateRight = TextButton("Radar >", skin).apply {
            addListener {
                if (isPressed) {
                    camera.rotateAround(Vector3.Zero, Vector3.Y, -35f * Gdx.graphics.deltaTime)
                    camera.lookAt(0f, 15f, 0f)
                    camera.update()
                }
                false
            }
        }

        layout.top().left()
        layout.add(statusLabel).colspan(3).left().expandX()
        layout.row()
        layout.add(rotateLeft)
        layout.add(fireButton).expandX().fillX()
        layout.add(rotateRight)
        layout.bottom()

        stage.addActor(layout)
    }

    override fun render(delta: Float) {
        val dt = max(0.001f, delta)
        stepSimulation(dt)

        ScreenUtils.clear(0.02f, 0.03f, 0.08f, 1f)
        Gdx.gl.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST)

        modelBatch.begin(camera)
        modelBatch.render(groundInstance, environment)
        modelBatch.render(launcherInstance, environment)
        incomingMissiles.forEach { modelBatch.render(it.instance, environment) }
        interceptors.forEach { modelBatch.render(it.instance, environment) }
        modelBatch.end()

        statusLabel.setText(
            "Wave $wave   Threats neutralized: $threatsNeutralized   Incoming: ${incomingMissiles.size}   Ready in: ${"%.1f".format(interceptCooldown.coerceAtLeast(0f))}s"
        )

        stage.act(dt)
        stage.draw()
    }

    private fun stepSimulation(dt: Float) {
        interceptCooldown -= dt
        spawnTimer -= dt

        if (spawnTimer <= 0f) {
            spawnIncomingMissile()
            spawnTimer = MathUtils.random(1.4f, 2.2f) / (1f + wave * 0.09f)
        }

        world.stepSimulation(dt, 5, 1f / 60f)

        advanceMissiles(incomingMissiles, dt)
        advanceMissiles(interceptors, dt)
        detectInterceptions()

        if (threatsNeutralized > 0 && threatsNeutralized % 10 == 0) {
            wave = 1 + threatsNeutralized / 10
        }
    }

    private fun advanceMissiles(missiles: Array<MissileEntity>, dt: Float) {
        val toRemove = Array<MissileEntity>()
        missiles.forEach { missile ->
            missile.velocity.mulAdd(gravity, dt)
            missile.position.mulAdd(missile.velocity, dt)
            missile.instance.transform.setToTranslation(missile.position)

            if (missile.position.y < 0f || missile.position.len() > 600f) {
                toRemove.add(missile)
            }
        }
        toRemove.forEach { missiles.removeValue(it, true) }
    }

    private fun launchInterceptor() {
        if (interceptCooldown > 0f || incomingMissiles.isEmpty) return

        val target = incomingMissiles.minBy { it.position.dst2(0f, 0f, 0f) }
        val launchPos = Vector3(0f, 4.5f, 0f)
        val toTarget = target.position.cpy().sub(launchPos)

        // Predictive lead for ballistic targets.
        val estimatedTime = toTarget.len() / 120f
        val predicted = target.position.cpy().mulAdd(target.velocity, estimatedTime)
        val direction = predicted.sub(launchPos).nor()

        val interceptor = MissileEntity(
            instance = ModelInstance(interceptorModel),
            position = launchPos,
            velocity = direction.scl(145f),
            blastRadius = 9f,
            isInterceptor = true,
        )

        interceptor.instance.transform.setToTranslation(launchPos)
        interceptors.add(interceptor)
        interceptCooldown = 1.2f
    }

    private fun spawnIncomingMissile() {
        val spawnX = MathUtils.random(-140f, 140f)
        val spawnZ = -180f
        val start = Vector3(spawnX, MathUtils.random(120f, 180f), spawnZ)
        val target = Vector3(MathUtils.random(-40f, 40f), 0f, MathUtils.random(90f, 130f))
        val arc = target.cpy().sub(start).nor()

        val speed = MathUtils.random(38f, 52f) + wave * 1.6f
        val incoming = MissileEntity(
            instance = ModelInstance(incomingModel),
            position = start,
            velocity = arc.scl(speed),
            blastRadius = 0f,
            isInterceptor = false,
        )

        incoming.instance.transform.setToTranslation(start)
        incomingMissiles.add(incoming)
    }

    private fun detectInterceptions() {
        val destroyedIncoming = Array<MissileEntity>()
        val spentInterceptors = Array<MissileEntity>()

        interceptors.forEach { interceptor ->
            incomingMissiles.forEach { threat ->
                if (interceptor.position.dst2(threat.position) <= interceptor.blastRadius * interceptor.blastRadius) {
                    destroyedIncoming.add(threat)
                    spentInterceptors.add(interceptor)
                    threatsNeutralized++
                }
            }
        }

        destroyedIncoming.distinct().forEach { incomingMissiles.removeValue(it, true) }
        spentInterceptors.distinct().forEach { interceptors.removeValue(it, true) }
    }

    override fun resize(width: Int, height: Int) {
        camera.viewportWidth = width.toFloat()
        camera.viewportHeight = height.toFloat()
        camera.update()
        stage.viewport.update(width, height, true)
    }

    override fun dispose() {
        stage.dispose()
        skin.dispose()
        modelBatch.dispose()

        groundModel.dispose()
        launcherModel.dispose()
        incomingModel.dispose()
        interceptorModel.dispose()

        world.dispose()
        solver.dispose()
        broadphase.dispose()
        dispatcher.dispose()
        collisionConfig.dispose()
    }
}

data class MissileEntity(
    val instance: ModelInstance,
    val position: Vector3,
    val velocity: Vector3,
    val blastRadius: Float,
    val isInterceptor: Boolean,
)
