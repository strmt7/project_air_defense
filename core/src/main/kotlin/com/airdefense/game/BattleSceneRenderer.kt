package com.airdefense.game

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ScreenUtils
import kotlin.math.sin

private val BATTLE_CLEAR_COLOR = Color.valueOf("03050AFF")
private val BACKDROP_MID_BAND_COLOR = Color.valueOf("05122470")
private val BACKDROP_WARM_BAND_COLOR = Color.valueOf("C79E6629")
private val BACKDROP_HOT_BAND_COLOR = Color.valueOf("F5A83D24")
private val BACKDROP_REFLECTION_GLOW_COLOR = Color.valueOf("FFC7612E")
private val BACKDROP_FOOTING_COLOR = Color.valueOf("0A101F1F")
private val RADAR_PANEL_COLOR = Color.valueOf("05121A99")
private val RADAR_GRID_COLOR = Color.valueOf("145C7A47")
private val RADAR_SWEEP_COLOR = Color.valueOf("24CCFF2E")
private val RADAR_LAUNCHER_COLOR = Color.valueOf("47EBFFE6")
private val OVERLAY_THREAT_LABEL_COLOR = Color.valueOf("FF5959E6")
private val LOADING_TRACK_COLOR = Color.valueOf("0A1A29F2")
private val LOADING_FILL_COLOR = Color.valueOf("33D6FFFF")
private const val LOADING_TITLE_Y = 0.58f
private const val LOADING_STATUS_Y = 0.5f
private const val LOADING_DIAGNOSTICS_Y = 0.46f
private const val LOADING_BAR_WIDTH_FRACTION = 0.34f
private const val LOADING_BAR_HEIGHT = 16f
private const val LOADING_BAR_Y = 0.42f
private const val GAME_OVER_TITLE_Y = 0.58f
private const val GAME_OVER_SCORE_Y = 0.48f
private const val BACKDROP_MID_BAND_Y = 0.22f
private const val BACKDROP_MID_BAND_HEIGHT = 0.11f
private const val BACKDROP_WARM_BAND_Y = 0.205f
private const val BACKDROP_WARM_BAND_HEIGHT = 0.028f
private const val BACKDROP_HOT_BAND_Y = 0.238f
private const val BACKDROP_HOT_BAND_HEIGHT = 0.012f
private const val BACKDROP_REFLECTION_DRIFT = 0.36f
private const val BACKDROP_REFLECTION_OFFSET = 0.015f
private const val BACKDROP_REFLECTION_Y = 0.11f
private const val BACKDROP_REFLECTION_WIDTH = 1.05f
private const val BACKDROP_REFLECTION_HEIGHT = 0.14f
private const val BACKDROP_REFLECTION_FLASH_DIVISOR = 1200f
private const val BACKDROP_REFLECTION_BASE_ALPHA = 0.3f
private const val BACKDROP_REFLECTION_FLASH_ALPHA = 0.14f
private const val BACKDROP_REFLECTION_BAND_Y = 0.145f
private const val BACKDROP_REFLECTION_BAND_HEIGHT = 0.028f
private const val BACKDROP_HORIZON_Y = 0.19f
private const val BACKDROP_HORIZON_HEIGHT = 0.24f
private const val BACKDROP_HORIZON_ALPHA = 0.82f
private const val BACKDROP_GLOW_Y = 0.12f
private const val BACKDROP_GLOW_HEIGHT = 0.2f
private const val BACKDROP_GLOW_ALPHA = 0.92f
private const val BACKDROP_FOOTING_HEIGHT = 0.12f
private const val ATMOSPHERE_FRONT_Y = 0.2f
private const val ATMOSPHERE_FRONT_HEIGHT = 0.16f
private const val ATMOSPHERE_FRONT_ALPHA = 0.78f
private const val ATMOSPHERE_REAR_X = -0.08f
private const val ATMOSPHERE_REAR_Y = 0.1f
private const val ATMOSPHERE_REAR_WIDTH = 1.16f
private const val ATMOSPHERE_REAR_HEIGHT = 0.11f
private const val ATMOSPHERE_REAR_ALPHA = 0.54f
private const val RADAR_REFERENCE_HEIGHT = 1080f
private const val RADAR_BASE_SIZE = 182f
private const val RADAR_MARGIN = 20f
private const val RADAR_GRID_INSET = 0.08f
private const val RADAR_SWEEP_HEIGHT = 3.2f
private const val RADAR_GRID_THICKNESS = 1.5f
private const val RADAR_MARKER_SIZE = 10f
private const val RADAR_THREAT_MARKER_SIZE = 4.8f
private const val RADAR_INTERCEPTOR_MARKER_SIZE = 3.6f
private const val RADAR_THREAT_MARKER_OFFSET = 2.4f
private const val RADAR_INTERCEPTOR_MARKER_OFFSET = 1.8f
private const val OVERLAY_THREAT_LINE_X = 20f
private const val OVERLAY_THREAT_LINE_Y = 24f
private const val OVERLAY_THREAT_LINE_WIDTH = 34f
private const val OVERLAY_THREAT_LINE_HEIGHT = 2f
private const val OVERLAY_THREAT_TEXT_X = 20f
private const val OVERLAY_THREAT_TEXT_Y = 34f
private const val WORLD_ALTITUDE_METERS_SCALE = 3.5f
private const val RADAR_GRID_MIDLINE = 0.5f
private const val RADAR_GRID_TOPLINE = 0.92f
private const val HALF_SCALE = 0.5f

private data class ScreenFrame(
    val width: Float,
    val height: Float,
)

private data class RadarLayout(
    val x: Float,
    val y: Float,
    val size: Float,
    val gridStart: Float,
    val gridSpan: Float,
    val uiScale: Float,
)

class BattleSceneRenderer(
    private val stage: Stage,
    private val skin: Skin,
    private val whiteRegion: TextureRegion,
) {
    private val projectedThreatPoint = Vector3()
    private var skyRegion: TextureRegion? = null
    private var horizonTexture: Texture? = null
    private var glowTexture: Texture? = null
    private var reflectionTexture: Texture? = null
    private var fogTexture: Texture? = null

    fun updateBackdropTextures(
        skyRegion: TextureRegion?,
        horizonTexture: Texture?,
        glowTexture: Texture?,
        reflectionTexture: Texture?,
        fogTexture: Texture?,
    ) {
        this.skyRegion = skyRegion
        this.horizonTexture = horizonTexture
        this.glowTexture = glowTexture
        this.reflectionTexture = reflectionTexture
        this.fogTexture = fogTexture
    }

    fun renderBackdrop(
        battleLiveTime: Float,
        impactLightIntensity: Float,
    ) {
        clearFrame()
        val skyTexture = skyRegion ?: return

        val batch = stage.batch
        val screenFrame = currentScreenFrame()

        stage.viewport.apply()
        batch.projectionMatrix = stage.camera.combined
        batch.begin()
        batch.color = Color.WHITE
        batch.draw(skyTexture, 0f, 0f, screenFrame.width, screenFrame.height)
        drawBackdropBands(batch, screenFrame)
        reflectionTexture?.let { drawReflectionLayer(batch, it, screenFrame, battleLiveTime, impactLightIntensity) }
        horizonTexture?.let { drawHorizonLayer(batch, it, screenFrame) }
        batch.color = Color.WHITE
        batch.end()
    }

    fun renderAtmosphere() {
        val fog = fogTexture ?: return
        val batch = stage.batch
        val screenFrame = currentScreenFrame()

        stage.viewport.apply()
        batch.projectionMatrix = stage.camera.combined
        batch.begin()
        batch.setColor(1f, 1f, 1f, ATMOSPHERE_FRONT_ALPHA)
        batch.draw(
            fog,
            0f,
            screenFrame.height * ATMOSPHERE_FRONT_Y,
            screenFrame.width,
            screenFrame.height * ATMOSPHERE_FRONT_HEIGHT,
        )
        batch.setColor(1f, 1f, 1f, ATMOSPHERE_REAR_ALPHA)
        batch.draw(
            fog,
            screenFrame.width * ATMOSPHERE_REAR_X,
            screenFrame.height * ATMOSPHERE_REAR_Y,
            screenFrame.width * ATMOSPHERE_REAR_WIDTH,
            screenFrame.height * ATMOSPHERE_REAR_HEIGHT,
        )
        batch.color = Color.WHITE
        batch.end()
    }

    fun renderLoading(
        loadingMessage: String,
        diagnosticsLine: String,
        progress: Float,
    ) {
        clearFrame()
        val batch = stage.batch
        val screenFrame = currentScreenFrame()
        val font = skin.getFont("default")
        val titleFont = skin.getFont("title-font")
        val titleLayout = GlyphLayout(titleFont, "INITIALIZING BATTLESPACE")
        val statusLayout = GlyphLayout(font, loadingMessage)
        val diagnosticsLayout = GlyphLayout(font, diagnosticsLine)
        val barWidth = screenFrame.width * LOADING_BAR_WIDTH_FRACTION
        val barX = (screenFrame.width - barWidth) * HALF_SCALE
        val barY = screenFrame.height * LOADING_BAR_Y

        batch.begin()
        batch.color = Color.WHITE
        titleFont.draw(batch, titleLayout, (screenFrame.width - titleLayout.width) * HALF_SCALE, screenFrame.height * LOADING_TITLE_Y)
        font.draw(batch, statusLayout, (screenFrame.width - statusLayout.width) * HALF_SCALE, screenFrame.height * LOADING_STATUS_Y)
        font.draw(
            batch,
            diagnosticsLayout,
            (screenFrame.width - diagnosticsLayout.width) * HALF_SCALE,
            screenFrame.height * LOADING_DIAGNOSTICS_Y,
        )
        batch.color = LOADING_TRACK_COLOR
        batch.draw(whiteRegion, barX, barY, barWidth, LOADING_BAR_HEIGHT)
        batch.color = LOADING_FILL_COLOR
        batch.draw(whiteRegion, barX, barY, barWidth * progress.coerceIn(0f, 1f), LOADING_BAR_HEIGHT)
        batch.color = Color.WHITE
        batch.end()
    }

    fun renderGameOver(score: Int) {
        clearFrame()
        val batch = stage.batch
        val screenFrame = currentScreenFrame()
        val titleFont = skin.getFont("title-font")
        val normalFont = skin.getFont("default")
        val titleLayout = GlyphLayout(titleFont, "CITY LOST")
        val scoreLayout = GlyphLayout(normalFont, "FINAL SCORE: $score    TAP TO RETURN")

        batch.begin()
        titleFont.draw(batch, titleLayout, (screenFrame.width - titleLayout.width) * HALF_SCALE, screenFrame.height * GAME_OVER_TITLE_Y)
        normalFont.draw(batch, scoreLayout, (screenFrame.width - scoreLayout.width) * HALF_SCALE, screenFrame.height * GAME_OVER_SCORE_Y)
        batch.end()
    }

    fun renderOverlay(
        threats: Array<ThreatEntity>,
        interceptors: Array<InterceptorEntity>,
        camera: PerspectiveCamera,
        radarScanProgress: Float,
    ) {
        val batch = stage.batch
        val radarLayout = createRadarLayout()
        val font = skin.getFont("default")

        batch.begin()
        drawRadarPanel(batch, radarLayout, radarScanProgress)
        drawRadarContacts(batch, radarLayout, threats, interceptors)
        drawThreatLabels(batch, font, threats, camera, radarLayout.uiScale)
        batch.color = Color.WHITE
        batch.end()
    }

    private fun clearFrame() {
        ScreenUtils.clear(
            BATTLE_CLEAR_COLOR.r,
            BATTLE_CLEAR_COLOR.g,
            BATTLE_CLEAR_COLOR.b,
            BATTLE_CLEAR_COLOR.a,
            true,
        )
    }

    private fun currentScreenFrame(): ScreenFrame = ScreenFrame(Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())

    private fun createRadarLayout(): RadarLayout {
        val uiScale = Gdx.graphics.height / RADAR_REFERENCE_HEIGHT
        val radarSize = RADAR_BASE_SIZE * uiScale
        return RadarLayout(
            x = Gdx.graphics.width - radarSize - RADAR_MARGIN * uiScale,
            y = RADAR_MARGIN * uiScale,
            size = radarSize,
            gridStart = radarSize * RADAR_GRID_INSET,
            gridSpan = radarSize * (1f - RADAR_GRID_INSET * 2f),
            uiScale = uiScale,
        )
    }

    private fun drawBackdropBands(
        batch: com.badlogic.gdx.graphics.g2d.Batch,
        screenFrame: ScreenFrame,
    ) {
        batch.color = BACKDROP_MID_BAND_COLOR
        batch.draw(
            whiteRegion,
            0f,
            screenFrame.height * BACKDROP_MID_BAND_Y,
            screenFrame.width,
            screenFrame.height * BACKDROP_MID_BAND_HEIGHT,
        )
        batch.color = BACKDROP_WARM_BAND_COLOR
        batch.draw(
            whiteRegion,
            0f,
            screenFrame.height * BACKDROP_WARM_BAND_Y,
            screenFrame.width,
            screenFrame.height * BACKDROP_WARM_BAND_HEIGHT,
        )
        batch.color = BACKDROP_HOT_BAND_COLOR
        batch.draw(
            whiteRegion,
            0f,
            screenFrame.height * BACKDROP_HOT_BAND_Y,
            screenFrame.width,
            screenFrame.height * BACKDROP_HOT_BAND_HEIGHT,
        )
    }

    private fun drawReflectionLayer(
        batch: com.badlogic.gdx.graphics.g2d.Batch,
        reflectionTexture: Texture,
        screenFrame: ScreenFrame,
        battleLiveTime: Float,
        impactLightIntensity: Float,
    ) {
        val drift = sin(battleLiveTime * BACKDROP_REFLECTION_DRIFT) * screenFrame.width * BACKDROP_REFLECTION_OFFSET
        val impactFlash = (impactLightIntensity / BACKDROP_REFLECTION_FLASH_DIVISOR).coerceIn(0f, 1f)
        batch.setColor(1f, 1f, 1f, BACKDROP_REFLECTION_BASE_ALPHA + impactFlash * BACKDROP_REFLECTION_FLASH_ALPHA)
        batch.draw(
            reflectionTexture,
            -drift,
            screenFrame.height * BACKDROP_REFLECTION_Y,
            screenFrame.width * BACKDROP_REFLECTION_WIDTH,
            screenFrame.height * BACKDROP_REFLECTION_HEIGHT,
        )
        batch.setColor(
            BACKDROP_REFLECTION_GLOW_COLOR.r,
            BACKDROP_REFLECTION_GLOW_COLOR.g,
            BACKDROP_REFLECTION_GLOW_COLOR.b,
            impactFlash * BACKDROP_REFLECTION_GLOW_COLOR.a,
        )
        batch.draw(
            whiteRegion,
            0f,
            screenFrame.height * BACKDROP_REFLECTION_BAND_Y,
            screenFrame.width,
            screenFrame.height * BACKDROP_REFLECTION_BAND_HEIGHT,
        )
    }

    private fun drawHorizonLayer(
        batch: com.badlogic.gdx.graphics.g2d.Batch,
        horizonTexture: Texture,
        screenFrame: ScreenFrame,
    ) {
        batch.setColor(1f, 1f, 1f, BACKDROP_HORIZON_ALPHA)
        batch.draw(
            horizonTexture,
            0f,
            screenFrame.height * BACKDROP_HORIZON_Y,
            screenFrame.width,
            screenFrame.height * BACKDROP_HORIZON_HEIGHT,
        )
        glowTexture?.let {
            batch.setColor(1f, 1f, 1f, BACKDROP_GLOW_ALPHA)
            batch.draw(it, 0f, screenFrame.height * BACKDROP_GLOW_Y, screenFrame.width, screenFrame.height * BACKDROP_GLOW_HEIGHT)
        }
        batch.color = BACKDROP_FOOTING_COLOR
        batch.draw(whiteRegion, 0f, 0f, screenFrame.width, screenFrame.height * BACKDROP_FOOTING_HEIGHT)
    }

    private fun drawRadarPanel(
        batch: com.badlogic.gdx.graphics.g2d.Batch,
        radarLayout: RadarLayout,
        radarScanProgress: Float,
    ) {
        val sweepY = RadarProjection.sweepY(radarLayout.y, radarLayout.size, radarScanProgress)
        val launcherMarker = RadarProjection.launcherMarker(radarLayout.x, radarLayout.y, radarLayout.size, radarLayout.size)
        batch.color = RADAR_PANEL_COLOR
        batch.draw(whiteRegion, radarLayout.x, radarLayout.y, radarLayout.size, radarLayout.size)
        batch.color = RADAR_GRID_COLOR
        drawRadarGridLine(batch, radarLayout, RADAR_GRID_INSET)
        drawRadarGridLine(batch, radarLayout, RADAR_GRID_MIDLINE)
        drawRadarGridLine(batch, radarLayout, RADAR_GRID_TOPLINE)
        batch.color = RADAR_SWEEP_COLOR
        batch.draw(
            whiteRegion,
            radarLayout.x + radarLayout.gridStart,
            sweepY,
            radarLayout.gridSpan,
            RADAR_SWEEP_HEIGHT * radarLayout.uiScale,
        )
        batch.color = RADAR_LAUNCHER_COLOR
        batch.draw(
            whiteRegion,
            launcherMarker.x - RADAR_MARKER_SIZE * HALF_SCALE * radarLayout.uiScale,
            launcherMarker.y - RADAR_MARKER_SIZE * HALF_SCALE * radarLayout.uiScale,
            RADAR_MARKER_SIZE * radarLayout.uiScale,
            RADAR_MARKER_SIZE * radarLayout.uiScale,
        )
    }

    private fun drawRadarContacts(
        batch: com.badlogic.gdx.graphics.g2d.Batch,
        radarLayout: RadarLayout,
        threats: Array<ThreatEntity>,
        interceptors: Array<InterceptorEntity>,
    ) {
        threats.forEach { threat ->
            val plot = RadarProjection.project(threat.position, radarLayout.x, radarLayout.y, radarLayout.size, radarLayout.size)
            batch.color = if (threat.isTracked) Color.RED else Color.YELLOW
            batch.draw(
                whiteRegion,
                plot.x - RADAR_THREAT_MARKER_OFFSET * radarLayout.uiScale,
                plot.y - RADAR_THREAT_MARKER_OFFSET * radarLayout.uiScale,
                RADAR_THREAT_MARKER_SIZE * radarLayout.uiScale,
                RADAR_THREAT_MARKER_SIZE * radarLayout.uiScale,
            )
        }

        interceptors.forEach { interceptor ->
            val plot =
                RadarProjection.project(interceptor.position, radarLayout.x, radarLayout.y, radarLayout.size, radarLayout.size)
            batch.color = RADAR_LAUNCHER_COLOR
            batch.draw(
                whiteRegion,
                plot.x - RADAR_INTERCEPTOR_MARKER_OFFSET * radarLayout.uiScale,
                plot.y - RADAR_INTERCEPTOR_MARKER_OFFSET * radarLayout.uiScale,
                RADAR_INTERCEPTOR_MARKER_SIZE * radarLayout.uiScale,
                RADAR_INTERCEPTOR_MARKER_SIZE * radarLayout.uiScale,
            )
        }
    }

    private fun drawThreatLabels(
        batch: com.badlogic.gdx.graphics.g2d.Batch,
        font: com.badlogic.gdx.graphics.g2d.BitmapFont,
        threats: Array<ThreatEntity>,
        camera: PerspectiveCamera,
        uiScale: Float,
    ) {
        threats.forEach { threat ->
            if (!threat.isTracked) return@forEach
            projectedThreatPoint.set(threat.position)
            camera.project(projectedThreatPoint)
            if (projectedThreatPoint.z in 0f..1f) {
                batch.color = OVERLAY_THREAT_LABEL_COLOR
                batch.draw(
                    whiteRegion,
                    projectedThreatPoint.x - OVERLAY_THREAT_LINE_X * uiScale,
                    projectedThreatPoint.y + OVERLAY_THREAT_LINE_Y * uiScale,
                    OVERLAY_THREAT_LINE_WIDTH * uiScale,
                    OVERLAY_THREAT_LINE_HEIGHT * uiScale,
                )
                font.draw(
                    batch,
                    "${threat.id}  ALT ${(threat.position.y * WORLD_ALTITUDE_METERS_SCALE).toInt()} m",
                    projectedThreatPoint.x + OVERLAY_THREAT_TEXT_X * uiScale,
                    projectedThreatPoint.y + OVERLAY_THREAT_TEXT_Y * uiScale,
                )
            }
        }
    }

    private fun drawRadarGridLine(
        batch: com.badlogic.gdx.graphics.g2d.Batch,
        radarLayout: RadarLayout,
        positionFraction: Float,
    ) {
        batch.draw(
            whiteRegion,
            radarLayout.x + radarLayout.gridStart,
            radarLayout.y + positionFraction * radarLayout.size,
            radarLayout.gridSpan,
            RADAR_GRID_THICKNESS * radarLayout.uiScale,
        )
    }
}
