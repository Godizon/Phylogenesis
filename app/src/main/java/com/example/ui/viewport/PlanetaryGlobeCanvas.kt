package com.example.ui.viewport

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.example.model.ActiveCataclysm
import com.example.model.CataclysmType
import com.example.model.PlanetaryState
import kotlin.math.*

@Composable
fun PlanetaryGlobeCanvas(
    planetary: PlanetaryState,
    activeCataclysm: ActiveCataclysm?,
    onTapGlobe: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Rotation animation
    val infiniteTransition = rememberInfiniteTransition(label = "planetary_rotation")
    val rotationDeg by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(24000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures {
                    onTapGlobe()
                }
            }
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = min(size.width, size.height) * 0.38f

        // 1. Draw Star background field & glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF0D1D3A), Color.Transparent),
                center = Offset(cx, cy),
                radius = radius * 1.8f
            ),
            radius = radius * 1.8f,
            center = Offset(cx, cy)
        )

        // 2. Auroral Magnetosphere Ring if shielded
        if (planetary.isMagnetosphereShielded) {
            val auraRadius = radius * 1.18f * pulseGlow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, Color(0x3300F0FF), Color(0x11A855F7), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = auraRadius
                ),
                radius = auraRadius,
                center = Offset(cx, cy)
            )
        }

        // 3. Ocean / Base Planetary Sphere
        val oceanColor = if (planetary.isSnowballEarth) {
            Color(0xFFE2E8F0) // Frozen white/ice
        } else if (planetary.oceanPh < 7.2f) {
            Color(0xFF1E3A5F) // Acidic dark teal
        } else {
            Color(0xFF0F3B66) // Vibrant deep ocean
        }

        drawCircle(
            color = oceanColor,
            radius = radius,
            center = Offset(cx, cy)
        )

        // 4. Continents & Landmasses with rotation
        drawTectonicContinents(cx, cy, radius, rotationDeg, planetary)

        // 5. Polar Ice Caps (expands with iceAlbedoCoverage)
        val iceCapHeight = radius * (planetary.iceAlbedoCoverage * 0.85f).coerceIn(0.08f, 0.95f)
        // North Pole Cap
        drawOval(
            color = Color(0xFFF1F5F9).copy(alpha = 0.9f),
            topLeft = Offset(cx - radius * 0.7f, cy - radius),
            size = androidx.compose.ui.geometry.Size(radius * 1.4f, iceCapHeight)
        )
        // South Pole Cap
        drawOval(
            color = Color(0xFFF1F5F9).copy(alpha = 0.9f),
            topLeft = Offset(cx - radius * 0.7f, cy + radius - iceCapHeight),
            size = androidx.compose.ui.geometry.Size(radius * 1.4f, iceCapHeight)
        )

        // 6. Atmospheric Cloud Vortices & Wind Bands
        drawAtmosphericClouds(cx, cy, radius, rotationDeg * 1.3f)

        // 7. Day / Night Terminator & Spherical Shading (3D effect)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.Transparent, Color(0x33000000), Color(0xAA070B14), Color(0xEE070B14)),
                center = Offset(cx - radius * 0.35f, cy - radius * 0.35f),
                radius = radius * 1.25f
            ),
            radius = radius,
            center = Offset(cx, cy)
        )

        // Atmosphere Rim Glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.Transparent, Color(0x2200F0FF), Color(0x7700F0FF), Color.Transparent),
                center = Offset(cx, cy),
                radius = radius * 1.05f
            ),
            radius = radius * 1.05f,
            center = Offset(cx, cy),
            style = Stroke(width = 6f)
        )

        // 8. Cataclysm Shockwave / Plumes
        if (activeCataclysm != null) {
            drawCataclysmOverlay(cx, cy, radius, activeCataclysm)
        }
    }
}

private fun DrawScope.drawTectonicContinents(
    cx: Float,
    cy: Float,
    radius: Float,
    rotationDeg: Float,
    planetary: PlanetaryState
) {
    val landColor = if (planetary.isSnowballEarth) {
        Color(0xFFCBD5E1)
    } else if (planetary.atmosphere.o2Percent > 12f) {
        Color(0xFF2E6B34) // Terrestrial green flora
    } else {
        Color(0xFF8B5A2B) // Pre-vegetation primordial red-brown continental crust
    }

    // Draw 3 simulated procedural tectonic continents rotating across the spheroid
    for (i in 0 until 3) {
        val angle = Math.toRadians((rotationDeg + i * 120.0))
        val normX = cos(angle).toFloat()
        val isFront = sin(angle) > -0.2f

        if (isFront) {
            val contX = cx + normX * radius * 0.65f
            val contY = cy + (if (i == 0) -radius * 0.2f else if (i == 1) radius * 0.25f else 0f)
            val contWidth = radius * 0.55f * (1f - abs(normX) * 0.4f)
            val contHeight = radius * 0.40f

            drawOval(
                color = landColor,
                topLeft = Offset(contX - contWidth / 2f, contY - contHeight / 2f),
                size = androidx.compose.ui.geometry.Size(contWidth, contHeight)
            )

            // Geothermal rift glowing vents if volcanism is high
            if (planetary.volcanicActivityIndex > 0.4f && i == 1) {
                drawCircle(
                    color = Color(0xFFFF5722),
                    radius = 4f,
                    center = Offset(contX, contY)
                )
            }
        }
    }
}

private fun DrawScope.drawAtmosphericClouds(
    cx: Float,
    cy: Float,
    radius: Float,
    cloudRotation: Float
) {
    val cloudColor = Color.White.copy(alpha = 0.22f)
    for (i in 0 until 4) {
        val yOffset = cy + (i - 1.5f) * radius * 0.35f
        val waveOffset = sin(Math.toRadians((cloudRotation + i * 70).toDouble())).toFloat() * 15f
        drawOval(
            color = cloudColor,
            topLeft = Offset(cx - radius * 0.75f + waveOffset, yOffset - 12f),
            size = androidx.compose.ui.geometry.Size(radius * 1.5f, 24f)
        )
    }
}

private fun DrawScope.drawCataclysmOverlay(
    cx: Float,
    cy: Float,
    radius: Float,
    cataclysm: ActiveCataclysm
) {
    val shockwaveR = radius * cataclysm.shockwaveRadiusPercent * 1.2f
    val shockColor = Color(cataclysm.type.colorHex)

    when (cataclysm.type) {
        CataclysmType.CHICXULUB_BOLIDE -> {
            // Impact epicenter fireball & expanding shockwave ring
            val impactX = cx - radius * 0.3f
            val impactY = cy - radius * 0.2f
            drawCircle(
                color = Color(0xFFFFB703),
                radius = 16f,
                center = Offset(impactX, impactY)
            )
            drawCircle(
                color = shockColor.copy(alpha = (1f - cataclysm.shockwaveRadiusPercent).coerceIn(0f, 1f)),
                radius = shockwaveR,
                center = Offset(impactX, impactY),
                style = Stroke(width = 8f)
            )
        }
        CataclysmType.LARGE_IGNEOUS_PROVINCE -> {
            // Massive mantle plume flood basalts
            drawCircle(
                color = shockColor.copy(alpha = 0.4f),
                radius = radius * 0.5f,
                center = Offset(cx, cy + radius * 0.1f)
            )
        }
        CataclysmType.SNOWBALL_RUNAWAY -> {
            // Cryogenic ice veil expanding to equator
            drawCircle(
                color = Color.White.copy(alpha = 0.5f * cataclysm.shockwaveRadiusPercent),
                radius = radius,
                center = Offset(cx, cy)
            )
        }
        CataclysmType.GAMMA_RAY_BURST -> {
            // Relativistic beam piercing atmosphere
            drawLine(
                color = Color(0xFFA855F7),
                start = Offset(0f, 0f),
                end = Offset(cx + radius, cy + radius),
                strokeWidth = 14f
            )
        }
    }
}
