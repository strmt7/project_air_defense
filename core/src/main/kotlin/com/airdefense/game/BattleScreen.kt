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
import com.badlogic.gdx.graphics.g3d.environment.PointLight
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
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Slider
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.viewport.ScreenViewport
import kotlin.math.max

class BattleScreen : ScreenAdapter() {
    private val modelBatch = ModelBatch()
    private val environment = Environment().apply {
        set(ColorAttribute(ColorAttribute.AmbientLight, 0.46f, 0.43f, 0.38f, 1f))
        add(DirectionalLight().set(Color(0.98f, 0.92f, 0.84f, 1f), -0.8f, -1f, -0.25f))
        add(DirectionalLight().set(Color(0.18f, 0.2f, 0.28f, 1f), 0.4f, -0.35f, -0.85f))
        add(PointLight().set(Color(1f, 0.64f, 0.42f, 1f), 0f, 14f, 0f, 46f))
    }

    private val camera = PerspectiveCamera(58f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat()).apply {
        near = 0.1f
        far = 1500f
        position.set(0f, 92f, 235f)
        lookAt(0f, 24f, 0f)
        update()
    }

    private val settings = DefenseSettings()
    private val stage = Stage(ScreenViewport())
    private val skin = Skin(Gdx.files.classpath("com/badlogic/gdx/scenes/scene2d/ui/skin/uiskin.json"))
    private val statusLabel = Label("", skin)
    private val doctrineLabel = Label("", skin)

    private val gravity = Vector3(0f, -9.81f, 0f)
    private val collisionConfig = btDefaultCollisionConfiguration()
    private val dispatcher = btCollisionDispatcher(collisionConfig)
    private val broadphase = btDbvtBroadphase()
    private val solver = btSequentialImpulseConstraintSolver()
    private val world = btDiscreteDynamicsWorld(dispatcher, broadphase, solver, collisionConfig).apply {
        setGravity(gravity)
    }

    private val skyModel: Model
    private val groundModel: Model
    private val roadModel: Model
    private val buildingModel: Model
    private val mountainModel: Model
    private val treeTrunkModel: Model
    private val treeTopModel: Model

    private val launcherTrailerModel: Model
    private val launcherCanisterModel: Model
    private val radarBaseModel: Model
    private val radarFaceModel: Model
    private val ecsModel: Model
    private val mastModel: Model
    private val powerUnitModel: Model

    private val incomingBallisticModel: Model
    private val incomingCruiseModel: Model
    private val antiRadiationModel: Model
    private val decoyModel: Model
    private val interceptorModel: Model
    private val trailModel: Model
    private val blastModel: Model

    private val skyInstance: ModelInstance
    private val groundInstance: ModelInstance
    private val roadInstances = Array<ModelInstance>()
    private val cityBlocks = Array<BuildingEntity>()
    private val mountainInstances = Array<ModelInstance>()
    private val treeTrunks = Array<ModelInstance>()
    private val treeTops = Array<ModelInstance>()

    private val launcherTrailerInstance: ModelInstance
    private val launcherCanisters = Array<ModelInstance>()
    private val radarBaseInstance: ModelInstance
    private val radarFaceInstance: ModelInstance
    private val ecsInstance: ModelInstance
    private val mastInstance: ModelInstance
    private val powerUnitInstance: ModelInstance

    private val incomingMissiles = Array<ThreatEntity>()
    private val trackTable = Array<TrackContact>()
    private val interceptors = Array<InterceptorEntity>()
    private val trails = Array<TrailParticle>()
    private val blastEffects = Array<BlastEffect>()

    private var wave = 1
    private var neutralized = 0
    private var leaks = 0

    private var launchCooldown = 0f
    private var spawnTimer = 0f
    private var radarSweepAngle = 0f
    private var radarTrackRefreshTimer = 0f
    private var autoFireTimer = 0f

    private var radarHealth = 100f
    private var launcherHealth = 100f
    private var ecsHealth = 100f
    private var radarOfflineTimer = 0f

    init {
        Bullet.init()
        val builder = ModelBuilder()
        val attrs = (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong()

        skyModel = builder.createSphere(1250f, 1000f, 1250f, 32, 24, Material(ColorAttribute.createDiffuse(Color(0.54f, 0.67f, 0.82f, 1f))), attrs)
        groundModel = builder.createBox(640f, 1f, 640f, Material(ColorAttribute.createDiffuse(Color(0.57f, 0.52f, 0.39f, 1f))), attrs)
        roadModel = builder.createBox(18f, 0.2f, 120f, Material(ColorAttribute.createDiffuse(Color(0.27f, 0.27f, 0.29f, 1f))), attrs)
        buildingModel = builder.createBox(20f, 24f, 20f, Material(ColorAttribute.createDiffuse(Color(0.58f, 0.57f, 0.55f, 1f))), attrs)
        mountainModel = builder.createCone(45f, 60f, 45f, 20, Material(ColorAttribute.createDiffuse(Color(0.42f, 0.38f, 0.31f, 1f))), attrs)
        treeTrunkModel = builder.createCylinder(1.1f, 5f, 1.1f, 12, Material(ColorAttribute.createDiffuse(Color(0.42f, 0.28f, 0.12f, 1f))), attrs)
        treeTopModel = builder.createSphere(5.2f, 4.8f, 5.2f, 14, 12, Material(ColorAttribute.createDiffuse(Color(0.28f, 0.43f, 0.22f, 1f))), attrs)

        launcherTrailerModel = builder.createBox(16f, 3f, 8f, Material(ColorAttribute.createDiffuse(Color(0.33f, 0.37f, 0.39f, 1f))), attrs)
        launcherCanisterModel = builder.createBox(2.3f, 9f, 2.3f, Material(ColorAttribute.createDiffuse(Color(0.24f, 0.63f, 0.68f, 1f))), attrs)
        radarBaseModel = builder.createCylinder(9f, 5f, 9f, 24, Material(ColorAttribute.createDiffuse(Color(0.35f, 0.39f, 0.44f, 1f))), attrs)
        radarFaceModel = builder.createBox(21f, 1f, 7f, Material(ColorAttribute.createDiffuse(Color(0.71f, 0.77f, 0.84f, 1f))), attrs)
        ecsModel = builder.createBox(11f, 4f, 5f, Material(ColorAttribute.createDiffuse(Color(0.31f, 0.35f, 0.36f, 1f))), attrs)
        mastModel = builder.createCylinder(0.8f, 20f, 0.8f, 10, Material(ColorAttribute.createDiffuse(Color(0.72f, 0.72f, 0.72f, 1f))), attrs)
        powerUnitModel = builder.createBox(8f, 3f, 4f, Material(ColorAttribute.createDiffuse(Color(0.36f, 0.32f, 0.29f, 1f))), attrs)

        incomingBallisticModel = builder.createCapsule(1.5f, 7f, 20, Material(ColorAttribute.createDiffuse(Color(0.82f, 0.2f, 0.18f, 1f))), attrs)
        incomingCruiseModel = builder.createBox(6f, 1.2f, 1.6f, Material(ColorAttribute.createDiffuse(Color(0.88f, 0.63f, 0.16f, 1f))), attrs)
        antiRadiationModel = builder.createCapsule(1.2f, 5f, 18, Material(ColorAttribute.createDiffuse(Color(0.97f, 0.82f, 0.25f, 1f))), attrs)
        decoyModel = builder.createSphere(1.7f, 1.7f, 1.7f, 16, 16, Material(ColorAttribute.createDiffuse(Color(0.75f, 0.72f, 0.3f, 1f))), attrs)
        interceptorModel = builder.createCapsule(1f, 6f, 20, Material(ColorAttribute.createDiffuse(Color(0.17f, 0.75f, 0.98f, 1f))), attrs)
        trailModel = builder.createSphere(0.5f, 0.5f, 0.5f, 10, 10, Material(ColorAttribute.createDiffuse(Color(0.95f, 0.95f, 0.95f, 1f))), attrs)
        blastModel = builder.createSphere(2.5f, 2.5f, 2.5f, 16, 16, Material(ColorAttribute.createDiffuse(Color(1f, 0.49f, 0.14f, 1f))), attrs)

        skyInstance = ModelInstance(skyModel)
        groundInstance = ModelInstance(groundModel).apply { transform.setToTranslation(0f, -0.5f, 0f) }

        launcherTrailerInstance = ModelInstance(launcherTrailerModel).apply { transform.setToTranslation(0f, 2f, -2f) }
        radarBaseInstance = ModelInstance(radarBaseModel).apply { transform.setToTranslation(0f, 3f, 11f) }
        radarFaceInstance = ModelInstance(radarFaceModel).apply { transform.setToTranslation(0f, 8f, 11f) }
        ecsInstance = ModelInstance(ecsModel).apply { transform.setToTranslation(-16f, 2f, 4f) }
        mastInstance = ModelInstance(mastModel).apply { transform.setToTranslation(-20f, 10f, 11f) }
        powerUnitInstance = ModelInstance(powerUnitModel).apply { transform.setToTranslation(14f, 1.7f, 8f) }

        buildEnvironment()
        buildPatriotLikeBattery()
        setupHud()
        Gdx.input.inputProcessor = stage
    }

    private fun buildEnvironment() {
        for (i in -4..4) {
            val road = ModelInstance(roadModel)
            road.transform.setToTranslation(i * 22f, 0.1f, 130f)
            roadInstances.add(road)
        }

        for (x in -5..5) {
            for (z in 2..7) {
                if (MathUtils.randomBoolean(0.65f)) {
                    val block = ModelInstance(buildingModel)
                    val bx = x * 24f + MathUtils.random(-6f, 6f)
                    val bz = z * 28f + MathUtils.random(-5f, 5f)
                    val height = MathUtils.random(12f, 30f)
                    block.transform.setToScaling(1f, height / 24f, 1f).trn(bx, height * 0.5f, bz)
                    cityBlocks.add(BuildingEntity(block, Vector3(bx, 0f, bz), 100f))
                }
            }
        }

        repeat(16) {
            val mountain = ModelInstance(mountainModel)
            val mx = MathUtils.randomSign() * MathUtils.random(180f, 290f)
            val mz = MathUtils.random(-300f, -80f)
            mountain.transform.setToTranslation(mx, 29f, mz)
            mountainInstances.add(mountain)
        }

        repeat(52) {
            val x = MathUtils.random(-220f, 220f)
            val z = MathUtils.random(-10f, 240f)
            if (Vector3(x, 0f, z).dst2(0f, 0f, 0f) < 900f) return@repeat

            val trunk = ModelInstance(treeTrunkModel)
            val top = ModelInstance(treeTopModel)
            trunk.transform.setToTranslation(x, 2.5f, z)
            top.transform.setToTranslation(x, 6f, z)
            treeTrunks.add(trunk)
            treeTops.add(top)
        }
    }

    private fun buildPatriotLikeBattery() {
        for (row in 0 until 2) {
            for (col in 0 until 4) {
                val canister = ModelInstance(launcherCanisterModel)
                val x = -4.2f + col * 2.8f
                val y = 6.8f + row * 2.6f
                val z = -2.8f + row * 2f
                canister.transform.setToTranslation(x, y, z)
                launcherCanisters.add(canister)
            }
        }
    }

    private fun setupHud() {
        val root = Table().apply {
            setFillParent(true)
            defaults().pad(4f)
        }

        val fireButton = TextButton("Fire Salvo", skin).apply {
            addListener(object : ChangeListener() {
                override fun changed(event: ChangeEvent?, actor: com.badlogic.gdx.scenes.scene2d.Actor?) = fireControlledSalvo()
            })
        }
        val left = TextButton("<", skin).apply {
            addListener(object : ChangeListener() {
                override fun changed(event: ChangeEvent?, actor: com.badlogic.gdx.scenes.scene2d.Actor?) {
                    camera.rotateAround(Vector3.Zero, Vector3.Y, 10f)
                    camera.lookAt(0f, 18f, 0f)
                    camera.update()
                }
            })
        }
        val right = TextButton(">", skin).apply {
            addListener(object : ChangeListener() {
                override fun changed(event: ChangeEvent?, actor: com.badlogic.gdx.scenes.scene2d.Actor?) {
                    camera.rotateAround(Vector3.Zero, Vector3.Y, -10f)
                    camera.lookAt(0f, 18f, 0f)
                    camera.update()
                }
            })
        }
        val auto = CheckBox("Auto Fire", skin).apply {
            isChecked = settings.autoFire
            addListener(object : ChangeListener() {
                override fun changed(event: ChangeEvent?, actor: com.badlogic.gdx.scenes.scene2d.Actor?) {
                    settings.autoFire = isChecked
                }
            })
        }

        val rangeSlider = makeSlider("Engage Range", 80f, 280f, settings.engagementRange) { settings.engagementRange = it }
        val speedSlider = makeSlider("Interceptor Speed", 100f, 235f, settings.interceptorSpeed) { settings.interceptorSpeed = it }
        val cooldownSlider = makeSlider("Launch Cooldown", 0.3f, 2.4f, settings.launchCooldown) { settings.launchCooldown = it }
        val radarSlider = makeSlider("Radar Refresh", 0.1f, 1.9f, settings.radarRefreshSeconds) { settings.radarRefreshSeconds = it }
        val blastSlider = makeSlider("Blast Radius", 4f, 20f, settings.blastRadius) { settings.blastRadius = it }
        val salvoSlider = makeSlider("Salvo Size", 1f, 6f, settings.salvoSize.toFloat(), 1f) { settings.salvoSize = it.toInt() }

        root.top().left()
        root.add(statusLabel).left().colspan(2).expandX()
        root.row()
        root.add(doctrineLabel).left().colspan(2)
        root.row()
        root.add(left).width(56f)
        root.add(right).width(56f).left()
        root.row()
        root.add(fireButton).colspan(2).fillX().width(220f)
        root.row()
        root.add(auto).left().colspan(2)
        root.row()
        root.add(rangeSlider).left().colspan(2)
        root.row()
        root.add(speedSlider).left().colspan(2)
        root.row()
        root.add(cooldownSlider).left().colspan(2)
        root.row()
        root.add(radarSlider).left().colspan(2)
        root.row()
        root.add(blastSlider).left().colspan(2)
        root.row()
        root.add(salvoSlider).left().colspan(2)

        stage.addActor(root)
    }

    private fun makeSlider(
        title: String,
        min: Float,
        max: Float,
        initial: Float,
        step: Float = 0.05f,
        onChange: (Float) -> Unit,
    ): Table {
        val label = Label("$title: ${"%.2f".format(initial)}", skin)
        val slider = Slider(min, max, step, false, skin).apply {
            value = initial
            addListener(object : ChangeListener() {
                override fun changed(event: ChangeEvent?, actor: com.badlogic.gdx.scenes.scene2d.Actor?) {
                    val adjusted = if (step >= 1f) value.toInt().toFloat() else value
                    label.setText("$title: ${"%.2f".format(adjusted)}")
                    onChange(adjusted)
                }
            })
        }

        return Table().apply {
            add(label).left().padBottom(2f)
            row()
            add(slider).width(260f).left()
        }
    }

    override fun render(delta: Float) {
        val dt = max(0.001f, delta)
        simulate(dt)

        ScreenUtils.clear(0.58f, 0.67f, 0.77f, 1f)
        Gdx.gl.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST)

        modelBatch.begin(camera)
        modelBatch.render(skyInstance, environment)
        modelBatch.render(groundInstance, environment)
        roadInstances.forEach { modelBatch.render(it, environment) }
        cityBlocks.forEach { modelBatch.render(it.instance, environment) }
        mountainInstances.forEach { modelBatch.render(it, environment) }
        treeTrunks.forEach { modelBatch.render(it, environment) }
        treeTops.forEach { modelBatch.render(it, environment) }

        modelBatch.render(launcherTrailerInstance, environment)
        launcherCanisters.forEach { modelBatch.render(it, environment) }
        modelBatch.render(radarBaseInstance, environment)
        modelBatch.render(radarFaceInstance, environment)
        modelBatch.render(ecsInstance, environment)
        modelBatch.render(mastInstance, environment)
        modelBatch.render(powerUnitInstance, environment)

        incomingMissiles.forEach { modelBatch.render(it.instance, environment) }
        interceptors.forEach { modelBatch.render(it.instance, environment) }
        trails.forEach { modelBatch.render(it.instance, environment) }
        blastEffects.forEach { modelBatch.render(it.instance, environment) }
        modelBatch.end()

        statusLabel.setText(
            "Wave $wave | Kills $neutralized | Leaks $leaks | Radar ${radarHealth.toInt()}% | Launcher ${launcherHealth.toInt()}% | ECS ${ecsHealth.toInt()}%"
        )
        doctrineLabel.setText(
            "Scan->Track->Engage. Radar ${if (radarOfflineTimer > 0f) "JAMMED ${"%.1f".format(radarOfflineTimer)}s" else "ONLINE"} | Sweep ${"%.0f".format(radarSweepAngle)}° | Tracks ${trackTable.size}"
        )

        stage.act(dt)
        stage.draw()
    }

    private fun simulate(dt: Float) {
        launchCooldown -= dt
        spawnTimer -= dt
        radarTrackRefreshTimer -= dt
        autoFireTimer -= dt
        radarOfflineTimer -= dt

        if (spawnTimer <= 0f) {
            spawnThreatPackage()
            spawnTimer = MathUtils.random(0.95f, 2.0f) / (1f + wave * 0.1f)
        }

        world.stepSimulation(dt, 5, 1f / 60f)

        radarSweepAngle = (radarSweepAngle + settings.radarSweepRpm * 6f * dt) % 360f
        radarFaceInstance.transform.setToRotation(Vector3.Y, radarSweepAngle).trn(0f, 8f, 11f)

        if (radarTrackRefreshTimer <= 0f) {
            if (radarOfflineTimer <= 0f && radarHealth > 0f) {
                updateTrackTable()
            } else {
                trackTable.clear()
            }
            radarTrackRefreshTimer = settings.radarRefreshSeconds
        }

        advanceThreats(dt)
        advanceInterceptors(dt)
        advanceTrails(dt)
        advanceBlasts(dt)
        detectInterceptions()

        if (settings.autoFire && autoFireTimer <= 0f && trackTable.any { it.hostileConfidence > 0.42f }) {
            fireControlledSalvo()
            autoFireTimer = 0.22f
        }

        if (neutralized > 0 && neutralized % 14 == 0) {
            wave = 1 + neutralized / 14
        }
    }

    private fun updateTrackTable() {
        trackTable.clear()
        val sweep = Vector3(0f, 0f, -1f).rotate(Vector3.Y, radarSweepAngle)

        incomingMissiles.forEach { threat ->
            val toThreat = threat.position.cpy().nor()
            val inBeam = sweep.dot(toThreat) > 0.53f
            val distance = threat.position.len()
            val quality = when {
                inBeam -> 0.96f
                distance < 130f -> 0.66f
                else -> 0.32f
            }

            val confidence = when (threat.type) {
                ThreatType.BALLISTIC -> 0.9f
                ThreatType.CRUISE -> 0.7f
                ThreatType.ANTI_RADIATION -> 0.97f
                ThreatType.DECOY -> 0.22f
            } * quality

            trackTable.add(
                TrackContact(
                    threat = threat,
                    hostileConfidence = confidence,
                    timeToImpact = threat.position.dst(threat.aimPoint) / max(1f, threat.velocity.len()),
                )
            )
        }
    }

    private fun fireControlledSalvo() {
        if (launchCooldown > 0f || launcherHealth <= 0f || trackTable.isEmpty) return

        val ordered = trackTable
            .sortedBy { it.timeToImpact - (it.hostileConfidence * 2f) }
            .filter { it.hostileConfidence > 0.35f && it.threat.position.len() < settings.engagementRange }

        if (ordered.isEmpty()) return
        val canistersAvailable = max(1, (launcherCanisters.size * (launcherHealth / 100f)).toInt())
        val salvoCount = minOf(settings.salvoSize, canistersAvailable)

        for (i in 0 until salvoCount) {
            val track = ordered.getOrNull(i % ordered.size) ?: continue
            launchInterceptor(track.threat, i)
        }

        launchCooldown = settings.launchCooldown
    }

    private fun launchInterceptor(target: ThreatEntity, index: Int) {
        val canister = launcherCanisters[index % launcherCanisters.size]
        val origin = canister.transform.getTranslation(Vector3())
        val lead = target.position.cpy().mulAdd(target.velocity, 0.55f)
        val direction = lead.sub(origin).nor()

        val interceptor = InterceptorEntity(
            instance = ModelInstance(interceptorModel),
            position = origin,
            velocity = direction.scl(settings.interceptorSpeed),
            blastRadius = settings.blastRadius,
            target = target,
        )
        interceptor.instance.transform.setToTranslation(origin)
        interceptors.add(interceptor)
        spawnTrail(origin)
    }

    private fun spawnThreatPackage() {
        val count = MathUtils.random(1, if (wave >= 5) 3 else 2)
        repeat(count) {
            val isAntiRadiation = MathUtils.random(1, 20) == 1
            val type = when {
                isAntiRadiation -> ThreatType.ANTI_RADIATION
                wave > 3 && MathUtils.randomBoolean(0.18f) -> ThreatType.DECOY
                MathUtils.randomBoolean(0.32f) -> ThreatType.CRUISE
                else -> ThreatType.BALLISTIC
            }

            val start = Vector3(MathUtils.random(-260f, 260f), MathUtils.random(90f, 230f), MathUtils.random(-340f, -180f))
            val aim = when (type) {
                ThreatType.ANTI_RADIATION -> Vector3(0f, 7f, 11f)
                else -> Vector3(MathUtils.random(-80f, 80f), 0f, MathUtils.random(85f, 195f))
            }
            val direction = aim.cpy().sub(start).nor()

            val velocity = when (type) {
                ThreatType.BALLISTIC -> direction.scl(MathUtils.random(48f, 68f) + wave)
                ThreatType.CRUISE -> direction.scl(MathUtils.random(33f, 45f) + wave * 0.6f).set(direction.x * 30f, -4.5f, direction.z * 30f)
                ThreatType.ANTI_RADIATION -> direction.scl(MathUtils.random(56f, 75f) + wave * 0.7f)
                ThreatType.DECOY -> direction.scl(MathUtils.random(20f, 36f))
            }

            val model = when (type) {
                ThreatType.BALLISTIC -> incomingBallisticModel
                ThreatType.CRUISE -> incomingCruiseModel
                ThreatType.ANTI_RADIATION -> antiRadiationModel
                ThreatType.DECOY -> decoyModel
            }

            incomingMissiles.add(
                ThreatEntity(
                    instance = ModelInstance(model),
                    position = start,
                    velocity = velocity,
                    type = type,
                    aimPoint = aim,
                ).also { it.instance.transform.setToTranslation(start) }
            )
        }
    }

    private fun advanceThreats(dt: Float) {
        val remove = Array<ThreatEntity>()

        incomingMissiles.forEach { threat ->
            when (threat.type) {
                ThreatType.BALLISTIC -> threat.velocity.mulAdd(gravity, dt * 0.35f)
                ThreatType.CRUISE -> threat.velocity.rotate(Vector3.Y, MathUtils.random(-9f, 9f) * dt)
                ThreatType.ANTI_RADIATION -> {
                    val home = Vector3(0f, 7f, 11f).sub(threat.position).nor().scl(68f)
                    threat.velocity.lerp(home, dt * 0.9f)
                }
                ThreatType.DECOY -> {
                    threat.velocity.rotate(Vector3.Y, MathUtils.random(-22f, 22f) * dt)
                    threat.velocity.y -= dt * 2f
                }
            }

            threat.position.mulAdd(threat.velocity, dt)
            threat.instance.transform.setToTranslation(threat.position)
            spawnTrail(threat.position)

            val hitGround = threat.position.y <= 0f || threat.position.dst2(threat.aimPoint) < 45f
            if (hitGround) {
                resolveImpact(threat)
                remove.add(threat)
            } else if (threat.position.len() > 1100f || threat.position.y < -30f) {
                remove.add(threat)
            }
        }

        remove.forEach { incomingMissiles.removeValue(it, true) }
    }

    private fun resolveImpact(threat: ThreatEntity) {
        leaks++
        spawnBlast(threat.position.cpy())

        if (threat.type == ThreatType.ANTI_RADIATION && threat.position.dst2(0f, 7f, 11f) < 400f) {
            radarHealth = (radarHealth - 30f).coerceAtLeast(0f)
            ecsHealth = (ecsHealth - 10f).coerceAtLeast(0f)
            radarOfflineTimer = 8f
            tintDamage(radarFaceInstance, 1f - radarHealth / 100f)
            tintDamage(ecsInstance, 1f - ecsHealth / 100f)
        }

        if (threat.position.dst2(0f, 0f, 0f) < 900f) {
            launcherHealth = (launcherHealth - 18f).coerceAtLeast(0f)
            tintDamage(launcherTrailerInstance, 1f - launcherHealth / 100f)
        }

        cityBlocks.forEach { building ->
            val distance = building.anchor.dst(threat.position)
            if (distance < 34f && building.integrity > 0f) {
                val damage = (36f - distance).coerceAtLeast(4f) * if (threat.type == ThreatType.BALLISTIC) 1.1f else 0.85f
                building.integrity = (building.integrity - damage).coerceAtLeast(0f)
                tintDamage(building.instance, 1f - building.integrity / 100f)
                if (building.integrity <= 0f) {
                    building.instance.transform.trn(0f, -5f, 0f)
                    spawnBlast(building.anchor.cpy().add(0f, 2f, 0f))
                }
            }
        }
    }

    private fun tintDamage(instance: ModelInstance, level: Float) {
        val diffuse = Color(
            MathUtils.lerp(0.65f, 0.19f, level),
            MathUtils.lerp(0.65f, 0.15f, level),
            MathUtils.lerp(0.65f, 0.14f, level),
            1f,
        )
        instance.materials.firstOrNull()?.set(ColorAttribute.createDiffuse(diffuse))
    }

    private fun advanceInterceptors(dt: Float) {
        val spent = Array<InterceptorEntity>()
        interceptors.forEach { interceptor ->
            if (interceptor.target !in incomingMissiles && incomingMissiles.isNotEmpty) {
                interceptor.target = incomingMissiles[MathUtils.random(0, incomingMissiles.size - 1)]
            }

            val predicted = interceptor.target.position.cpy().mulAdd(interceptor.target.velocity, 0.18f)
            val desiredVelocity = predicted.sub(interceptor.position).nor().scl(settings.interceptorSpeed)
            interceptor.velocity.lerp(desiredVelocity, dt * settings.guidanceResponsiveness)
            interceptor.position.mulAdd(interceptor.velocity, dt)
            interceptor.instance.transform.setToTranslation(interceptor.position)
            spawnTrail(interceptor.position)

            if (interceptor.position.y < 0f || interceptor.position.len() > 1200f) spent.add(interceptor)
        }
        spent.forEach { interceptors.removeValue(it, true) }
    }

    private fun detectInterceptions() {
        val destroyed = Array<ThreatEntity>()
        val spent = Array<InterceptorEntity>()

        interceptors.forEach { interceptor ->
            incomingMissiles.forEach { threat ->
                val resistance = when (threat.type) {
                    ThreatType.BALLISTIC -> 1f
                    ThreatType.CRUISE -> 1.18f
                    ThreatType.ANTI_RADIATION -> 1.08f
                    ThreatType.DECOY -> 0.5f
                }
                val radius = interceptor.blastRadius / resistance
                if (interceptor.position.dst2(threat.position) <= radius * radius) {
                    destroyed.add(threat)
                    spent.add(interceptor)
                    neutralized++
                    spawnBlast(threat.position.cpy())
                }
            }
        }

        destroyed.distinct().forEach { incomingMissiles.removeValue(it, true) }
        spent.distinct().forEach { interceptors.removeValue(it, true) }
    }

    private fun spawnTrail(position: Vector3) {
        if (!MathUtils.randomBoolean(0.34f)) return
        trails.add(TrailParticle(ModelInstance(trailModel), position.cpy(), MathUtils.random(0.24f, 0.6f)).also {
            it.instance.transform.setToTranslation(it.position)
        })
    }

    private fun advanceTrails(dt: Float) {
        val expired = Array<TrailParticle>()
        trails.forEach {
            it.life -= dt
            it.position.y += dt * 0.75f
            it.instance.transform.setToTranslation(it.position)
            if (it.life <= 0f) expired.add(it)
        }
        expired.forEach { trails.removeValue(it, true) }
    }

    private fun spawnBlast(position: Vector3) {
        blastEffects.add(BlastEffect(ModelInstance(blastModel), position, 0.65f).also {
            it.instance.transform.setToTranslation(position)
        })
    }

    private fun advanceBlasts(dt: Float) {
        val expired = Array<BlastEffect>()
        blastEffects.forEach {
            it.life -= dt
            it.scale += dt * 4.2f
            it.instance.transform.setToScaling(it.scale, it.scale, it.scale).trn(it.position)
            if (it.life <= 0f) expired.add(it)
        }
        expired.forEach { blastEffects.removeValue(it, true) }
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

        skyModel.dispose()
        groundModel.dispose()
        roadModel.dispose()
        buildingModel.dispose()
        mountainModel.dispose()
        treeTrunkModel.dispose()
        treeTopModel.dispose()

        launcherTrailerModel.dispose()
        launcherCanisterModel.dispose()
        radarBaseModel.dispose()
        radarFaceModel.dispose()
        ecsModel.dispose()
        mastModel.dispose()
        powerUnitModel.dispose()

        incomingBallisticModel.dispose()
        incomingCruiseModel.dispose()
        antiRadiationModel.dispose()
        decoyModel.dispose()
        interceptorModel.dispose()
        trailModel.dispose()
        blastModel.dispose()

        world.dispose()
        solver.dispose()
        broadphase.dispose()
        dispatcher.dispose()
        collisionConfig.dispose()
    }
}

data class DefenseSettings(
    var engagementRange: Float = 185f,
    var interceptorSpeed: Float = 152f,
    var launchCooldown: Float = 1.1f,
    var radarRefreshSeconds: Float = 0.45f,
    var radarSweepRpm: Float = 11f,
    var blastRadius: Float = 9f,
    var salvoSize: Int = 2,
    var guidanceResponsiveness: Float = 2.95f,
    var autoFire: Boolean = true,
)

enum class ThreatType {
    BALLISTIC,
    CRUISE,
    ANTI_RADIATION,
    DECOY,
}

data class ThreatEntity(
    val instance: ModelInstance,
    val position: Vector3,
    val velocity: Vector3,
    val type: ThreatType,
    val aimPoint: Vector3,
)

data class TrackContact(
    val threat: ThreatEntity,
    val hostileConfidence: Float,
    val timeToImpact: Float,
)

data class InterceptorEntity(
    val instance: ModelInstance,
    val position: Vector3,
    val velocity: Vector3,
    val blastRadius: Float,
    var target: ThreatEntity,
)

data class TrailParticle(
    val instance: ModelInstance,
    val position: Vector3,
    var life: Float,
)

data class BlastEffect(
    val instance: ModelInstance,
    val position: Vector3,
    var life: Float,
    var scale: Float = 1f,
)

data class BuildingEntity(
    val instance: ModelInstance,
    val anchor: Vector3,
    var integrity: Float,
)
