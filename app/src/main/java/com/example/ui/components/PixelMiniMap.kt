package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ActiveCataclysm
import com.example.model.PlanetaryState
import com.example.ui.theme.*
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * WorldBox-style on-screen pixel radar mini-map.
 */
@Composable
fun PixelMiniMap(
    planetary: PlanetaryState,
    activeCataclysm: ActiveCataclysm?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(width = 86.dp, height = 66.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(PixelVoid)
            .border(2.dp, PixelBorderLight, RoundedCornerShape(3.dp))
            .testTag("pixel_mini_map")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val p = 4f // mini-pixel block size
            val cols = (w / p).toInt()
            val rows = (h / p).toInt()
            val cx = cols / 2f
            val cy = rows / 2f

            val iceLimit = planetary.iceAlbedoCoverage.coerceIn(0.1f, 0.9f)
            val isSnowball = planetary.isSnowballEarth

            for (r in 0 until rows) {
                val distY = abs(r - cy) / cy
                for (c in 0 until cols) {
                    val nx = (c - cx) * 0.25f
                    val ny = (r - cy) * 0.25f
                    val height = sin(nx) * cos(ny) + 0.4f * sin(nx * 2f)

                    val isIce = isSnowball || distY > (1f - iceLimit * 0.8f)
                    val color = when {
                        isIce -> PixelSnow
                        height > 0.35f -> PixelForest
                        height > 0.05f -> PixelGrass
                        height > -0.15f -> PixelSand
                        height > -0.4f -> PixelMidOcean
                        else -> PixelDeepOcean
                    }

                    drawRect(
                        color = color,
                        topLeft = Offset(c * p, r * p),
                        size = Size(p, p)
                    )
                }
            }

            // Blinking volcanic / cataclysm epicenter dot on mini-map
            if (activeCataclysm != null) {
                drawCircle(
                    color = PixelCrimson,
                    radius = 4f,
                    center = Offset(w * 0.45f, h * 0.5f)
                )
            } else if (planetary.volcanicActivityIndex > 0.5f) {
                drawCircle(
                    color = PixelLavaGlow,
                    radius = 3f,
                    center = Offset(w * 0.6f, h * 0.4f)
                )
            }
        }

        // Radar Label
        Text(
            text = "RADAR",
            color = PixelGold,
            fontSize = 8.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(2.dp)
        )
    }
}
