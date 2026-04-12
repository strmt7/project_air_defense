package com.airdefense.game

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Array

private val RADAR_PANEL_COLOR = Color.valueOf("05121A99")
private val RADAR_GRID_COLOR = Color.valueOf("145C7A47")
private val RADAR_SWEEP_COLOR = Color.valueOf("24CCFF2E")
private val RADAR_LAUNCHER_COLOR = Color.valueOf("47EBFFE6")
private val OVERLAY_THREAT_LABEL_COLOR = Color.valueOf("FF5959E6")
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

private data class RadarLayout(
    val x: Float,
    val y: Float,
    val size: Float,
    val gridStart: Float,
    val gridSpan: Float,
    val uiScale: Float,
)

internal data class BattleRadarOverlayState(
    val threats: Array<ThreatEntity>,
    val interceptors: Array<InterceptorEntity>,
    val camera: PerspectiveCamera,
    val radarScanProgress: Float,
)

internal class BattleRadarOverlayRenderer(
    private val whiteRegion: TextureRegion,
) {
    private val projectedThreatPoint = Vector3()

    internal fun render(
        batch: Batch,
        font: BitmapFont,
        overlayState: BattleRadarOverlayState,
    ) {
        val radarLayout = createRadarLayout()

        drawRadarPanel(batch, radarLayout, overlayState.radarScanProgress)
        drawRadarContacts(batch, radarLayout, overlayState.threats, overlayState.interceptors)
        drawThreatLabels(batch, font, overlayState.threats, overlayState.camera, radarLayout.uiScale)
        batch.color = Color.WHITE
    }

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

    private fun drawRadarPanel(
        batch: Batch,
        radarLayout: RadarLayout,
        radarScanProgress: Float,
    ) {
        val sweepY =
            RadarProjection.sweepY(
                radarLayout.y,
                radarLayout.size,
                radarScanProgress,
            )
        val launcherMarker =
            RadarProjection.launcherMarker(
                radarLayout.x,
                radarLayout.y,
                radarLayout.size,
                radarLayout.size,
            )

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
        batch: Batch,
        radarLayout: RadarLayout,
        threats: Array<ThreatEntity>,
        interceptors: Array<InterceptorEntity>,
    ) {
        threats.forEach { threat ->
            val plot =
                RadarProjection.project(
                    threat.position,
                    radarLayout.x,
                    radarLayout.y,
                    radarLayout.size,
                    radarLayout.size,
                )
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
                RadarProjection.project(
                    interceptor.position,
                    radarLayout.x,
                    radarLayout.y,
                    radarLayout.size,
                    radarLayout.size,
                )
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
        batch: Batch,
        font: BitmapFont,
        threats: Array<ThreatEntity>,
        camera: PerspectiveCamera,
        uiScale: Float,
    ) {
        threats.forEach { threat ->
            if (!threat.isTracked) return@forEach

            projectedThreatPoint.set(threat.position)
            camera.project(projectedThreatPoint)
            if (projectedThreatPoint.z !in 0f..1f) return@forEach

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

    private fun drawRadarGridLine(
        batch: Batch,
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
