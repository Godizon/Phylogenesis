package com.example.ui.viewport

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.example.model.Clade
import com.example.model.OrganismPhenotype
import com.example.model.ViewportOverlays
import kotlin.math.*

@Composable
fun EcosystemCanvas(
    clades: List<Clade>,
    overlays: ViewportOverlays,
    onSelectSpecimen: (OrganismPhenotype) -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ecosystem_motion")
    val timeTick by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "tick"
    )

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(clades) {
                detectTapGestures {
                    // Tap chooses the first extant clade's morphotype for inspection
                    val selected = clades.firstOrNull { !it.isExtinct }?.dominantMorphotype ?: clades.firstOrNull()?.dominantMorphotype
                    if (selected != null) {
                        onSelectSpecimen(selected)
                    }
                }
            }
    ) {
        val w = size.width
        val h = size.height

        // 1. Primordial Oceanic Substrate Gradient
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF071B2F), Color(0xFF04101D), Color(0xFF0B1426)),
                startY = 0f,
                endY = h
            )
        )

        // 2. Biomass Density Heatmap Layer
        if (overlays.showBiomassHeatmap) {
            drawBiomassHeatmap(w, h, timeTick)
        }

        // 3. Trophic Flow Energy Vectors (Arrows showing Autotroph -> Consumer -> Apex)
        if (overlays.showTrophicFlowVectors) {
            drawTrophicFlowVectors(w, h, timeTick)
        }

        // 4. Speciation Hotspot Zones
        if (overlays.showSpeciationHotspots) {
            drawSpeciationHotspots(w, h, timeTick)
        }

        // 5. Living Autonomous Biota (Plankton, Trilobites, Apex Swimmers, Pre-Chordates)
        drawOrganismDemes(w, h, timeTick, clades)
    }
}

private fun DrawScope.drawBiomassHeatmap(w: Float, h: Float, tick: Float) {
    // False-color emerald/cyan nutrient bloom zones
    for (i in 0 until 4) {
        val cx = (w * 0.25f * i + 80f * sin(tick * 0.1f + i)).coerceIn(50f, w - 50f)
        val cy = (h * 0.45f + 60f * cos(tick * 0.12f + i)).coerceIn(50f, h - 50f)
        val r = 140f + 25f * sin(tick * 0.2f + i)

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x3310B981), Color(0x1800F0FF), Color.Transparent),
                center = Offset(cx, cy),
                radius = r
            ),
            radius = r,
            center = Offset(cx, cy)
        )
    }
}

private fun DrawScope.drawTrophicFlowVectors(w: Float, h: Float, tick: Float) {
    // Flowing vectors representing directional energy transfer
    val strokeColor = Color(0x44FFB703)
    for (i in 0 until 5) {
        val y = h * 0.2f * i + 40f
        val startX = (w * 0.1f + (tick * 15f + i * 80f) % (w * 0.8f))
        val arrowLen = 35f

        drawLine(
            color = strokeColor,
            start = Offset(startX, y),
            end = Offset(startX + arrowLen, y),
            strokeWidth = 2f
        )
        // Arrowhead
        drawLine(
            color = strokeColor,
            start = Offset(startX + arrowLen, y),
            end = Offset(startX + arrowLen - 6f, y - 4f),
            strokeWidth = 2f
        )
        drawLine(
            color = strokeColor,
            start = Offset(startX + arrowLen, y),
            end = Offset(startX + arrowLen - 6f, y + 4f),
            strokeWidth = 2f
        )
    }
}

private fun DrawScope.drawSpeciationHotspots(w: Float, h: Float, tick: Float) {
    val spotX = w * 0.72f
    val spotY = h * 0.35f
    val pulseR = 30f + (tick * 4f % 50f)

    drawCircle(
        color = Color(0xFFA855F7).copy(alpha = (1f - (pulseR / 80f)).coerceIn(0.1f, 0.6f)),
        radius = pulseR,
        center = Offset(spotX, spotY),
        style = Stroke(width = 2.5f)
    )
    drawCircle(
        color = Color(0xFFA855F7),
        radius = 5f,
        center = Offset(spotX, spotY)
    )
}

private fun DrawScope.drawOrganismDemes(
    w: Float,
    h: Float,
    tick: Float,
    clades: List<Clade>
) {
    // Draw 12 animated organisms moving through ecosystem
    for (i in 0 until 12) {
        val clade = clades[i % clades.size]
        val speed = if (clade.id == "clad_anomalocaris") 40f else 22f
        val x = (i * (w / 12f) + (tick * speed + i * 45f)) % w
        val y = (h * 0.25f + sin(tick * 0.08f + i) * (h * 0.35f)).coerceIn(40f, h - 40f)

        val color = Color(clade.trophicLevel.colorHex)

        when (clade.id) {
            "clad_anomalocaris" -> {
                // Apex swimmer with undulating side flaps
                drawOval(
                    color = color,
                    topLeft = Offset(x - 18f, y - 9f),
                    size = androidx.compose.ui.geometry.Size(36f, 18f)
                )
                // Flaps
                val flapOffset = sin(tick * 0.4f + i) * 6f
                drawLine(color = color, start = Offset(x - 6f, y - 9f), end = Offset(x - 6f, y - 18f + flapOffset), strokeWidth = 3f)
                drawLine(color = color, start = Offset(x + 6f, y - 9f), end = Offset(x + 6f, y - 18f - flapOffset), strokeWidth = 3f)
            }
            "clad_trilobita" -> {
                // Benthic crawler with segmented carapace
                drawOval(
                    color = color,
                    topLeft = Offset(x - 12f, y - 8f),
                    size = androidx.compose.ui.geometry.Size(24f, 16f)
                )
                // Antennae
                drawLine(color = color, start = Offset(x + 10f, y), end = Offset(x + 18f, y - 5f), strokeWidth = 1.5f)
                drawLine(color = color, start = Offset(x + 10f, y), end = Offset(x + 18f, y + 5f), strokeWidth = 1.5f)
            }
            "clad_chordata" -> {
                // Sinuous undulating pre-chordate
                val path = Path().apply {
                    moveTo(x, y)
                    quadraticBezierTo(
                        x - 10f, y + sin(tick * 0.3f + i) * 8f,
                        x - 22f, y + cos(tick * 0.3f + i) * 6f
                    )
                }
                drawPath(path = path, color = color, style = Stroke(width = 4f))
            }
            else -> {
                // Microscopic photosynthetic drifter / cyanobion
                drawCircle(
                    color = color.copy(alpha = 0.85f),
                    radius = 5f,
                    center = Offset(x, y)
                )
            }
        }
    }
}
