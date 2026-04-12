package com.airdefense.game

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Slider
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener

private const val HUD_REFERENCE_HEIGHT = 1080f
private const val TOP_BAR_PAD = 12f
private const val CONTROL_DOCK_PAD = 14f
private const val CONTROL_DOCK_CELL_PAD = 8f
private const val CONTROL_ROW_PAD = 6f
private const val STATUS_PAD_TOP = 4f
private const val SUMMARY_PAD_TOP = 8f
private const val DETAIL_PAD_TOP = 6f
private const val CARD_CONTROL_PAD_TOP = 10f
private const val CARD_BUTTON_PAD_TOP = 12f
private const val DOCK_BOTTOM_PAD = 14f
private const val DOCK_TOP_PAD = 12f
private const val DOCTRINE_ROW_TOP_PAD = 2f
private const val CREDITS_PAD_LEFT = 18f
private const val CONTROL_LABEL_PAD_LEFT = 6f
private const val CARD_WIDTH = 364f
private const val CARD_CONTROL_WIDTH = 340f
private const val CARD_CONTROL_HEIGHT = 56f
private const val CARD_BUTTON_HEIGHT = 96f
private const val RANGE_MIN = 1200f
private const val RANGE_MAX = 3200f
private const val RANGE_STEP = 25f
private const val FUSE_MIN = 56f
private const val FUSE_MAX = 120f
private const val FUSE_STEP = 2f

internal class BattleHudController(
    private val stage: Stage,
    private val uiSkin: Skin,
    private val settings: DefenseSettings,
    private val onSettingsChanged: () -> Unit,
    private val onStartWaveRequested: () -> Unit,
) {
    private val statusLabel = Label("AIR DEFENSE NETWORK ONLINE", uiSkin, "headline")
    private val creditsLabel = Label("", uiSkin, "headline")
    private lateinit var waveButton: TextButton
    private lateinit var rangeValueLabel: Label
    private lateinit var fuseValueLabel: Label
    private lateinit var doctrineValueLabel: Label
    private lateinit var doctrineDetailLabel: Label
    private var latestSnapshot: BattleHudSnapshot? = null

    fun build() {
        stage.clear()
        val uiScale = Gdx.graphics.height / HUD_REFERENCE_HEIGHT
        val root = Table().apply { setFillParent(true) }
        root
            .add(createTopBar(uiScale))
            .expandX()
            .fillX()
            .top()
            .pad(DOCK_TOP_PAD * uiScale)
            .row()
        root.add().expand().row()
        root
            .add(createControlDock(uiScale))
            .expand()
            .bottom()
            .left()
            .pad(DOCK_BOTTOM_PAD * uiScale)
        stage.addActor(root)
        latestSnapshot?.let(::update)
    }

    fun setStatus(
        styleName: String,
        text: String,
    ) {
        statusLabel.style = uiSkin.get(styleName, Label.LabelStyle::class.java)
        statusLabel.setText(text)
    }

    fun update(snapshot: BattleHudSnapshot) {
        latestSnapshot = snapshot
        if (!::waveButton.isInitialized) return
        rangeValueLabel.setText(snapshot.rangeText())
        fuseValueLabel.setText(snapshot.fuseText())
        doctrineValueLabel.setText(snapshot.doctrineLabel)
        doctrineDetailLabel.setText(snapshot.doctrineSummary)
        creditsLabel.setText(snapshot.summaryText())
        val waveButtonState = snapshot.waveButtonState()
        waveButton.setText(waveButtonState.text)
        waveButton.isDisabled = waveButtonState.disabled
    }

    private fun createTopBar(uiScale: Float): Table =
        Table().apply {
            background = uiSkin.getDrawable("hud_panel")
            pad(TOP_BAR_PAD * uiScale)
            add(
                Table().apply {
                    defaults().left()
                    add(Label("BATTLESPACE", uiSkin, "title")).row()
                    add(statusLabel).padTop(STATUS_PAD_TOP * uiScale).row()
                },
            ).expandX().left()
            add(creditsLabel).right().padLeft(CREDITS_PAD_LEFT * uiScale)
        }

    private fun createControlDock(uiScale: Float): Table =
        Table().apply {
            background = uiSkin.getDrawable("hud_panel")
            pad(CONTROL_DOCK_PAD * uiScale)
            defaults().pad(CONTROL_DOCK_CELL_PAD * uiScale)
            add(Label("CONTROL", uiSkin, "status")).left().padLeft(CONTROL_LABEL_PAD_LEFT * uiScale).row()
            add(createControlRow(uiScale, createRangeCard(uiScale), createFuseCard(uiScale))).left().row()
            add(
                createControlRow(uiScale, createDoctrineCard(uiScale), createActionCard(uiScale)),
            ).left().padTop(DOCTRINE_ROW_TOP_PAD * uiScale).row()
        }

    private fun createControlRow(
        uiScale: Float,
        leftCard: Table,
        rightCard: Table,
    ): Table =
        Table().apply {
            defaults().pad(CONTROL_ROW_PAD * uiScale)
            add(leftCard).width(CARD_WIDTH * uiScale).fillY()
            add(rightCard).width(CARD_WIDTH * uiScale).fillY()
        }

    private fun createRangeCard(uiScale: Float): Table =
        Table().apply {
            background = uiSkin.getDrawable("hud_soft")
            pad(CONTROL_DOCK_PAD * uiScale)
            defaults().left()
            rangeValueLabel = Label("", uiSkin, "display")
            add(Label("AUTO ENGAGE RANGE", uiSkin, "status")).row()
            add(rangeValueLabel).padTop(SUMMARY_PAD_TOP * uiScale).row()
            add(
                Slider(RANGE_MIN, RANGE_MAX, RANGE_STEP, false, uiSkin).apply {
                    value = settings.engagementRange
                    addListener(
                        object : ChangeListener() {
                            override fun changed(
                                event: ChangeEvent?,
                                actor: Actor?,
                            ) {
                                settings.engagementRange = value
                                onSettingsChanged()
                            }
                        },
                    )
                },
            ).width(CARD_CONTROL_WIDTH * uiScale)
                .fillX()
                .height(CARD_CONTROL_HEIGHT * uiScale)
                .padTop(CARD_CONTROL_PAD_TOP * uiScale)
        }

    private fun createFuseCard(uiScale: Float): Table =
        Table().apply {
            background = uiSkin.getDrawable("hud_soft")
            pad(CONTROL_DOCK_PAD * uiScale)
            defaults().left()
            fuseValueLabel = Label("", uiSkin, "display")
            add(Label("FUZE WINDOW", uiSkin, "status")).row()
            add(fuseValueLabel).padTop(SUMMARY_PAD_TOP * uiScale).row()
            add(
                Slider(FUSE_MIN, FUSE_MAX, FUSE_STEP, false, uiSkin).apply {
                    value = settings.blastRadius
                    addListener(
                        object : ChangeListener() {
                            override fun changed(
                                event: ChangeEvent?,
                                actor: Actor?,
                            ) {
                                settings.blastRadius = value
                                onSettingsChanged()
                            }
                        },
                    )
                },
            ).width(CARD_CONTROL_WIDTH * uiScale)
                .fillX()
                .height(CARD_CONTROL_HEIGHT * uiScale)
                .padTop(CARD_CONTROL_PAD_TOP * uiScale)
        }

    private fun createDoctrineCard(uiScale: Float): Table =
        Table().apply {
            background = uiSkin.getDrawable("hud_soft")
            pad(CONTROL_DOCK_PAD * uiScale)
            defaults().left()
            doctrineValueLabel = Label("", uiSkin, "display")
            doctrineDetailLabel = Label("", uiSkin, "status").apply { setWrap(true) }
            val doctrineButton =
                TextButton("CYCLE DOCTRINE", uiSkin).apply {
                    addListener(
                        object : ChangeListener() {
                            override fun changed(
                                event: ChangeEvent?,
                                actor: Actor?,
                            ) {
                                settings.doctrine = settings.doctrine.next()
                                onSettingsChanged()
                            }
                        },
                    )
                }
            add(Label("DOCTRINE", uiSkin, "status")).row()
            add(doctrineValueLabel).padTop(SUMMARY_PAD_TOP * uiScale).row()
            add(doctrineDetailLabel)
                .width(CARD_CONTROL_WIDTH * uiScale)
                .padTop(DETAIL_PAD_TOP * uiScale)
                .row()
            add(
                doctrineButton,
            ).width(CARD_CONTROL_WIDTH * uiScale)
                .height(CARD_BUTTON_HEIGHT * uiScale)
                .fillX()
                .padTop(CARD_BUTTON_PAD_TOP * uiScale)
        }

    private fun createActionCard(uiScale: Float): Table =
        Table().apply {
            background = uiSkin.getDrawable("hud_soft")
            pad(CONTROL_DOCK_PAD * uiScale)
            defaults().left()
            add(Label("WAVE", uiSkin, "status")).row()
            waveButton =
                TextButton("START NEXT WAVE", uiSkin).apply {
                    addListener(
                        object : ChangeListener() {
                            override fun changed(
                                event: ChangeEvent?,
                                actor: Actor?,
                            ) {
                                onStartWaveRequested()
                            }
                        },
                    )
                }
            add(
                waveButton,
            ).width(CARD_CONTROL_WIDTH * uiScale)
                .height(CARD_BUTTON_HEIGHT * uiScale)
                .fillX()
                .padTop(CARD_BUTTON_PAD_TOP * uiScale)
        }
}
