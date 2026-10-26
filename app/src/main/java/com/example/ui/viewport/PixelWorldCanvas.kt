package com.example.ui.viewport

import androidx.compose.runtime.withFrameNanos
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.model.*
import com.example.ui.theme.*
import kotlin.math.*

/**
 * WorldBox-style 16-bit pixel art terrain and organism sandbox renderer.
 * Features distinct, highly legible organisms with autonomous swimming,
 * clear species name badges, precise tap-to-inspect hit testing,
 * animated targeting reticles, and pause-synchronized physics/animations.
 */
@Composable
fun PixelWorldCanvas(
    planetary: PlanetaryState,
    clades: List<Clade>,
    livingOrganisms: List<LivingOrganism>,
    selectedOrganismId: String?,
    isPaused: Boolean,
    overlays: ViewportOverlays,
    activeCataclysm: ActiveCataclysm?,
    weather: WeatherState = WeatherState(),
    onSelectOrganism: (LivingOrganism) -> Unit,
    onTapBackground: (Offset) -> Unit,
    modifier: Modifier = Modifier
) {
    // Simulation animation tick that STRICTLY freezes when isPaused is true
    var simAnimTick by remember { mutableFloatStateOf(0f) }

    // Water ripple effect on empty taps
    var rippleCenter by remember { mutableStateOf<Offset?>(null) }
    var rippleTimer by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isPaused) {
        var lastNano = System.nanoTime()
        while (true) {
            withFrameNanos { nowNano ->
                val dt = ((nowNano - lastNano) / 1_000_000_000f).coerceIn(0f, 0.05f)
                lastNano = nowNano
                if (!isPaused) {
                    simAnimTick += dt
                    if (rippleCenter != null) {
                        rippleTimer += dt
                        if (rippleTimer > 1.2f) {
                            rippleCenter = null
                            rippleTimer = 0f
                        }
                    }
                }
            }
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(livingOrganisms, isPaused) {
                detectTapGestures { tapOffset ->
                    val w = size.width.toFloat()
                    val h = size.height.toFloat()
                    var closest: LivingOrganism? = null
                    var closestDist = Float.MAX_VALUE
                    val touchThreshold = 54.dp.toPx()

                    for (org in livingOrganisms) {
                        val ox = org.xRatio * w
                        val oy = org.yRatio * h
                        val dist = hypot(ox - tapOffset.x, oy - tapOffset.y)
                        if (dist <= touchThreshold && dist < closestDist) {
                            closest = org
                            closestDist = dist
                        }
                    }

                    if (closest != null) {
                        onSelectOrganism(closest)
                    } else {
                        rippleCenter = tapOffset
                        rippleTimer = 0f
                        onTapBackground(tapOffset)
                    }
                }
            }
    ) {
        val w = size.width
        val h = size.height

        val pixelSize = 14f
        val cols = ceil(w / pixelSize).toInt()
        val rows = ceil(h / pixelSize).toInt()

        // 1. Draw Procedural WorldBox Terrain Grid
        drawPixelTerrain(
            cols = cols,
            rows = rows,
            pixelSize = pixelSize,
            planetary = planetary,
            animTick = simAnimTick
        )

        // 2. Draw Pixelated Viewport Overlays
        if (overlays.showBiomassHeatmap) {
            drawPixelBiomassHeatmap(cols, rows, pixelSize, simAnimTick)
        }
        if (overlays.showTrophicFlowVectors) {
            drawPixelTrophicFlow(w, h, pixelSize, simAnimTick)
        }
        if (overlays.showSpeciationHotspots) {
            drawPixelSpeciationHotspots(w, h, pixelSize, simAnimTick)
        }

        // 3. Draw Water Ripple on background taps
        rippleCenter?.let { rc ->
            val progress = (rippleTimer / 1.2f).coerceIn(0f, 1f)
            val rippleRadius = progress * 70.dp.toPx()
            val alpha = (1f - progress) * 0.75f
            drawCircle(
                color = PixelCyan.copy(alpha = alpha),
                radius = rippleRadius,
                center = rc,
                style = Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = Color.White.copy(alpha = alpha * 0.5f),
                radius = rippleRadius * 0.55f,
                center = rc,
                style = Stroke(width = 1.5.dp.toPx())
            )
        }

        // 4. Draw Revamped Autonomous Living Organisms
        drawLivingOrganismsLayer(
            w = w,
            h = h,
            organisms = livingOrganisms,
            selectedId = selectedOrganismId,
            animTick = simAnimTick,
            isPaused = isPaused
        )

        // 5. Draw Dynamic Weather Layer (Drifting 16-bit clouds & rain)
        if (overlays.showWeatherClouds) {
            drawPixelWeather(
                w = w,
                h = h,
                pixelSize = pixelSize,
                weather = weather,
                animTick = simAnimTick,
                planetary = planetary
            )
        }

        // 6. Day-Night Lighting Terminator
        if (overlays.showDayNightCycle) {
            drawDayNightTerminator(
                w = w,
                h = h,
                pixelSize = pixelSize,
                weather = weather,
                planetary = planetary
            )
        }

        // 7. Draw Pixel Cataclysm Effects
        if (activeCataclysm != null) {
            drawPixelCataclysm(
                w = w,
                h = h,
                pixelSize = pixelSize,
                cataclysm = activeCataclysm,
                animTick = simAnimTick
            )
        }

        // 8. Subtle Icy Frozen Vignette Border when Paused
        if (isPaused) {
            drawRect(
                color = PixelCyan.copy(alpha = 0.08f),
                topLeft = Offset.Zero,
                size = Size(w, h)
            )
            drawRect(
                color = PixelCyan.copy(alpha = 0.45f),
                topLeft = Offset.Zero,
                size = Size(w, h),
                style = Stroke(width = 3.dp.toPx())
            )
        }
    }
}

/**
 * Draws the discrete pixel tilemap representing continents, oceans, biomes, glaciers, and lava.
 */
private fun DrawScope.drawPixelTerrain(
    cols: Int,
    rows: Int,
    pixelSize: Float,
    planetary: PlanetaryState,
    animTick: Float
) {
    val tempC = planetary.surfaceTempCelsius
    val iceCoverage = planetary.iceAlbedoCoverage
    val isEyeball = planetary.isEyeballWorld
    val volcanism = planetary.volcanicActivityIndex

    val deepOceanColor = Color(0xFF041E42)
    val shallowOceanColor = Color(0xFF0077B6)
    val shelfColor = Color(0xFF00B4D8)
    val shoreSandColor = Color(0xFFE9D8A6)
    val lushBiomeColor = Color(0xFF2D6A4F)
    val mountainColor = Color(0xFF5C677D)
    val glacierColor = Color(0xFFE0F2FE)
    val lavaGlow = if ((animTick * 3f).toInt() % 2 == 0) PixelLava else PixelFire

    for (r in 0 until rows) {
        val yNorm = r.toFloat() / rows.toFloat()
        for (c in 0 until cols) {
            val xNorm = c.toFloat() / cols.toFloat()

            // Polar glacier threshold
            val polarDist = abs(yNorm - 0.5f) * 2f
            val isGlacier = if (isEyeball) {
                xNorm > 0.65f || polarDist > 0.85f
            } else {
                polarDist > (1f - iceCoverage).coerceAtLeast(0.15f)
            }

            if (isGlacier) {
                drawRect(
                    color = glacierColor,
                    topLeft = Offset(c * pixelSize, r * pixelSize),
                    size = Size(pixelSize, pixelSize)
                )
                continue
            }

            // Continental landmass noise
            val continentNoise = sin(xNorm * 7.5 + sin(yNorm * 5.0)) * 0.5f +
                    cos(yNorm * 6.0 + cos(xNorm * 4.0)) * 0.5f
            val isLand = continentNoise > 0.18f

            if (isLand) {
                val isMountain = continentNoise > 0.65f
                val isVolcano = volcanism > 0.4f && continentNoise > 0.82f
                val tileColor = when {
                    isVolcano -> lavaGlow
                    isMountain -> mountainColor
                    tempC < 2f -> Color(0xFF94D2BD)
                    tempC > 40f -> Color(0xFFCA6702)
                    else -> lushBiomeColor
                }
                drawRect(
                    color = tileColor,
                    topLeft = Offset(c * pixelSize, r * pixelSize),
                    size = Size(pixelSize, pixelSize)
                )
            } else {
                // Oceanic water depth
                val depthFactor = (0.18f - continentNoise)
                val isWaveFoam = (c + r + (animTick * 2.5f).toInt()) % 19 == 0
                val waterColor = when {
                    isWaveFoam -> Color(0xFF90E0EF)
                    depthFactor < 0.08f -> shoreSandColor
                    depthFactor < 0.22f -> shelfColor
                    depthFactor < 0.45f -> shallowOceanColor
                    else -> deepOceanColor
                }
                drawRect(
                    color = waterColor,
                    topLeft = Offset(c * pixelSize, r * pixelSize),
                    size = Size(pixelSize, pixelSize)
                )
            }
        }
    }
}

/**
 * Draws all autonomous living organisms with high-clarity 16-bit sprites,
 * heading-aligned rotation, animated targeting reticle, and species badges.
 */
private fun DrawScope.drawLivingOrganismsLayer(
    w: Float,
    h: Float,
    organisms: List<LivingOrganism>,
    selectedId: String?,
    animTick: Float,
    isPaused: Boolean
) {
    if (organisms.isEmpty()) return

    // Draw organisms in layer order: sessile reefs first, then creatures, then targeting reticles/labels
    organisms.forEach { org ->
        val ox = org.xRatio * w
        val oy = org.yRatio * h
        val isSelected = (org.id == selectedId)
        val headingDeg = Math.toDegrees(org.headingRad.toDouble()).toFloat()

        // 1. Draw Drop-Shadow beneath creature in water
        drawOval(
            color = Color.Black.copy(alpha = 0.28f),
            topLeft = Offset(ox - 18.dp.toPx() * org.spriteScale, oy + 8.dp.toPx() * org.spriteScale),
            size = Size(36.dp.toPx() * org.spriteScale, 12.dp.toPx() * org.spriteScale)
        )

        // 2. Draw Organism Sprite (oriented towards swimming heading)
        rotate(degrees = headingDeg, pivot = Offset(ox, oy)) {
            when (org.cladeId) {
                "clad_virus" -> {
                    drawDetailedVirus(ox, oy, org.spriteScale, org.swimPhase, isSelected)
                }
                "clad_placoderm" -> {
                    drawDetailedPlacoderm(ox, oy, org.spriteScale, org.swimPhase, isSelected)
                }
                "clad_tetrapod" -> {
                    drawDetailedTetrapod(ox, oy, org.spriteScale, org.swimPhase, isSelected)
                }
                "clad_dinosaur" -> {
                    drawDetailedDinosaur(ox, oy, org.spriteScale, org.swimPhase, isSelected)
                }
                "clad_bird" -> {
                    drawDetailedBird(ox, oy, org.spriteScale, org.swimPhase, isSelected)
                }
                "clad_mammal" -> {
                    drawDetailedMammal(ox, oy, org.spriteScale, org.swimPhase, isSelected)
                }
                "clad_chemoliths" -> {
                    drawDetailedChemolithVent(ox, oy, org.spriteScale, org.swimPhase, isSelected, animTick)
                }
                "clad_trilobita" -> {
                    drawDetailedTrilobite(ox, oy, org.spriteScale, org.swimPhase, isSelected)
                }
                "clad_anomalocaris" -> {
                    drawDetailedAnomalocaris(ox, oy, org.spriteScale, org.swimPhase, isSelected)
                }
                "clad_chordata" -> {
                    drawDetailedPreChordate(ox, oy, org.spriteScale, org.swimPhase, isSelected)
                }
                "clad_cyanobiota" -> {
                    drawDetailedStromatoliteReef(ox, oy, org.spriteScale, org.swimPhase, isSelected, animTick)
                }
                else -> {
                    // Custom / Engineered / Emergent Organism
                    drawDetailedEngineeredOrganism(ox, oy, org, org.spriteScale, org.swimPhase, isSelected)
                }
            }
        }

        // 3. Selection Reticle & Target Brackets
        if (isSelected) {
            drawSelectionReticle(ox, oy, org.spriteScale, animTick)
        }

        // 4. Species Name & Status Tag above creature
        drawOrganismNameTag(
            ox = ox,
            oy = oy - (24.dp.toPx() * org.spriteScale),
            org = org,
            isSelected = isSelected
        )
    }
}

/**
 * High-definition 16-bit Trilobite (Olenoides calcipteryx) sprite.
 * Distinct crescent cephalon, compound amber eyes, articulated pleura, and jointed legs.
 */
private fun DrawScope.drawDetailedTrilobite(
    cx: Float,
    cy: Float,
    scale: Float,
    swimPhase: Float,
    isSelected: Boolean
) {
    val u = 3.2f * scale // Chunky pixel unit

    // Stepped leg paddling
    val legCycle = sin(swimPhase) * u * 1.5f

    // 1. Jointed Walking Legs (amber gold)
    val legColor = PixelGold
    for (i in -2..2) {
        val ly = cy + i * (u * 2.8f)
        val legOffset = if (i % 2 == 0) legCycle else -legCycle
        // Left legs
        drawRect(legColor, Offset(cx - u * 5f, ly + legOffset), Size(u * 1.6f, u))
        drawRect(Color(0xFFB45309), Offset(cx - u * 6.5f, ly + legOffset + u), Size(u * 1.5f, u))
        // Right legs
        drawRect(legColor, Offset(cx + u * 3.5f, ly - legOffset), Size(u * 1.6f, u))
        drawRect(Color(0xFFB45309), Offset(cx + u * 5.0f, ly - legOffset + u), Size(u * 1.5f, u))
    }

    // 2. Cephalon (Head Shield) - Broad crescent shape facing forward (+X)
    val shellMain = if (isSelected) Color(0xFFFDE047) else Color(0xFFD4A373)
    val shellDark = Color(0xFF8B5A2B)
    val shellHighlight = Color(0xFFFAEDCD)

    // Cephalic plate
    drawRect(shellDark, Offset(cx + u * 2f, cy - u * 4.5f), Size(u * 3f, u * 9f))
    drawRect(shellMain, Offset(cx + u * 2.5f, cy - u * 3.8f), Size(u * 2f, u * 7.6f))
    drawRect(shellHighlight, Offset(cx + u * 3.5f, cy - u * 2.5f), Size(u * 0.8f, u * 5f))

    // Prominent Compound Eyes (Crescent Amber)
    drawRect(Color(0xFFFEF08A), Offset(cx + u * 2.8f, cy - u * 3.5f), Size(u * 1.2f, u * 1.2f))
    drawRect(Color(0xFF1E293B), Offset(cx + u * 3.2f, cy - u * 3.2f), Size(u * 0.6f, u * 0.6f))
    drawRect(Color(0xFFFEF08A), Offset(cx + u * 2.8f, cy + u * 2.3f), Size(u * 1.2f, u * 1.2f))
    drawRect(Color(0xFF1E293B), Offset(cx + u * 3.2f, cy + u * 2.6f), Size(u * 0.6f, u * 0.6f))

    // Sensory Antennae reaching forward
    drawRect(Color(0xFFFDE68A), Offset(cx + u * 5f, cy - u * 2f), Size(u * 3f, u * 0.6f))
    drawRect(Color(0xFFFDE68A), Offset(cx + u * 5f, cy + u * 1.4f), Size(u * 3f, u * 0.6f))

    // 3. Thoracic Segments (Pleural spines & Axial lobe)
    for (seg in 0..3) {
        val segX = cx - seg * (u * 2.2f)
        val segW = (4f - seg * 0.4f) * u
        drawRect(shellDark, Offset(segX - u * 1.8f, cy - segW), Size(u * 2f, segW * 2f))
        drawRect(shellMain, Offset(segX - u * 1.4f, cy - (segW - u * 0.6f)), Size(u * 1.4f, (segW - u * 0.6f) * 2f))
        // Central axial ridge
        drawRect(Color(0xFF9C6644), Offset(segX - u * 1.2f, cy - u * 1.2f), Size(u * 1.2f, u * 2.4f))
    }

    // 4. Pygidium (Tail shield)
    drawRect(shellDark, Offset(cx - u * 8f, cy - u * 2f), Size(u * 2f, u * 4f))
    drawRect(shellMain, Offset(cx - u * 7.5f, cy - u * 1.4f), Size(u * 1.2f, u * 2.8f))
}

/**
 * High-definition 16-bit Anomalocaris (Titanopteryx carnifex) Apex Predator.
 * Long segmented hydrodynamic body, great spiny raptorial appendages, stalked eyes,
 * and 6 pairs of undulating swimming lobes.
 */
private fun DrawScope.drawDetailedAnomalocaris(
    cx: Float,
    cy: Float,
    scale: Float,
    swimPhase: Float,
    isSelected: Boolean
) {
    val u = 3.5f * scale

    // 1. Undulating Lateral Swimming Lobes (Turquoise / Aquamarine hydrofoils)
    val lobeColor = Color(0xFFA8DADC)
    val lobeDark = Color(0xFF457B9D)
    for (i in 0 until 6) {
        val segX = cx - i * (u * 2.6f)
        val flapWave = sin(swimPhase + i * 0.7f) * (u * 2f)
        val lobeW = (3.5f - i * 0.35f) * u
        // Upper flap
        drawRect(lobeDark, Offset(segX, cy - u * 3.5f - lobeW + flapWave), Size(u * 2.2f, lobeW))
        drawRect(lobeColor, Offset(segX + u * 0.4f, cy - u * 3.5f - lobeW + flapWave + u * 0.5f), Size(u * 1.4f, lobeW - u))
        // Lower flap
        drawRect(lobeDark, Offset(segX, cy + u * 3.5f - flapWave), Size(u * 2.2f, lobeW))
        drawRect(lobeColor, Offset(segX + u * 0.4f, cy + u * 3.5f + u * 0.5f - flapWave), Size(u * 1.4f, lobeW - u))
    }

    // 2. Central Hydrodynamic Body (Fierce Coral Red / Crimson Chitin)
    val bodyMain = if (isSelected) Color(0xFFFF6B6B) else Color(0xFFE63946)
    val bodyDark = Color(0xFF9B2226)
    val bodyHighlight = Color(0xFFFFA5AB)

    for (i in 0 until 7) {
        val segX = cx - i * (u * 2.5f)
        val segH = (4.2f - i * 0.45f) * u
        drawRect(bodyDark, Offset(segX - u * 1.2f, cy - segH), Size(u * 2.4f, segH * 2f))
        drawRect(bodyMain, Offset(segX - u * 0.8f, cy - segH + u * 0.6f), Size(u * 1.6f, (segH - u * 0.6f) * 2f))
        drawRect(bodyHighlight, Offset(segX, cy - u * 0.8f), Size(u * 0.8f, u * 1.6f))
    }

    // 3. Stalked Bulbous Compound Eyes protruding laterally
    val eyeColor = Color(0xFFF1FAEE)
    val pupilColor = Color(0xFF0F172A)
    // Top eye stalk & eye
    drawRect(bodyDark, Offset(cx + u * 1.5f, cy - u * 4.5f), Size(u * 1.2f, u * 2f))
    drawRect(eyeColor, Offset(cx + u * 1.2f, cy - u * 5.8f), Size(u * 2.2f, u * 2.2f))
    drawRect(pupilColor, Offset(cx + u * 2.2f, cy - u * 5.4f), Size(u * 1.0f, u * 1.4f))
    // Bottom eye stalk & eye
    drawRect(bodyDark, Offset(cx + u * 1.5f, cy + u * 2.5f), Size(u * 1.2f, u * 2f))
    drawRect(eyeColor, Offset(cx + u * 1.2f, cy + u * 3.6f), Size(u * 2.2f, u * 2.2f))
    drawRect(pupilColor, Offset(cx + u * 2.2f, cy + u * 4.0f), Size(u * 1.0f, u * 1.4f))

    // 4. Great Spiny Raptorial Appendages reaching forward
    val raptorColor = Color(0xFFF4A261)
    val raptorSpine = Color(0xFFE76F51)
    // Upper grasping arm
    drawRect(raptorColor, Offset(cx + u * 3.2f, cy - u * 2.5f), Size(u * 3.5f, u * 1.4f))
    drawRect(raptorColor, Offset(cx + u * 6.0f, cy - u * 1.5f), Size(u * 2.0f, u * 2.0f))
    drawRect(raptorSpine, Offset(cx + u * 5.5f, cy), Size(u * 0.8f, u * 1.8f))
    // Lower grasping arm
    drawRect(raptorColor, Offset(cx + u * 3.2f, cy + u * 1.1f), Size(u * 3.5f, u * 1.4f))
    drawRect(raptorColor, Offset(cx + u * 6.0f, cy - u * 0.5f), Size(u * 2.0f, u * 2.0f))
    drawRect(raptorSpine, Offset(cx + u * 5.5f, cy - u * 1.8f), Size(u * 0.8f, u * 1.8f))

    // 5. Tail Fan (3 steering blades)
    val tailX = cx - u * 17.5f
    drawRect(lobeDark, Offset(tailX, cy - u * 4.5f), Size(u * 3.2f, u * 2f))
    drawRect(lobeColor, Offset(tailX + u * 0.5f, cy - u * 4.0f), Size(u * 2.2f, u * 1.2f))
    drawRect(lobeDark, Offset(tailX - u * 1.5f, cy - u * 1.2f), Size(u * 3.5f, u * 2.4f))
    drawRect(lobeColor, Offset(tailX - u * 1.0f, cy - u * 0.6f), Size(u * 2.5f, u * 1.2f))
    drawRect(lobeDark, Offset(tailX, cy + u * 2.5f), Size(u * 3.2f, u * 2f))
    drawRect(lobeColor, Offset(tailX + u * 0.5f, cy + u * 2.8f), Size(u * 2.2f, u * 1.2f))
}

/**
 * High-definition 16-bit Stem-Chordate (Notochordia gracilis / Pikaia analog).
 * Sinuous translucent eel/fish ancestor with glowing turquoise dorsal notochord rod.
 */
private fun DrawScope.drawDetailedPreChordate(
    cx: Float,
    cy: Float,
    scale: Float,
    swimPhase: Float,
    isSelected: Boolean
) {
    val u = 3.0f * scale
    val segCount = 9

    val bodyColor = if (isSelected) Color(0xFF90E0EF) else Color(0xFF48CAE4)
    val notochordGlow = Color(0xFF00F5D4)
    val myomereColor = Color(0xFF0077B6)

    // Undulating sinuous spine curve
    for (i in 0 until segCount) {
        val waveY = sin(swimPhase + i * 0.65f) * (u * 1.8f)
        val segX = cx - i * (u * 2.4f)
        val segY = cy + waveY
        val segH = (3.2f - (i * 0.28f)) * u

        // Translucent Body Fin Folds (Dorsal & Ventral)
        drawRect(Color(0xFFCAF0F8).copy(alpha = 0.6f), Offset(segX - u, segY - segH - u * 1.2f), Size(u * 2f, u * 1.2f))
        drawRect(Color(0xFFCAF0F8).copy(alpha = 0.6f), Offset(segX - u, segY + segH), Size(u * 2f, u * 1.2f))

        // Main Body Segment
        drawRect(bodyColor, Offset(segX - u, segY - segH), Size(u * 2f, segH * 2f))

        // V-Shaped Chevron Myomere muscle blocks
        drawRect(myomereColor, Offset(segX - u * 0.5f, segY - segH * 0.5f), Size(u * 0.8f, segH))

        // LUMINOUS NOTOCHORD ROD - Core evolutionary feature
        drawRect(notochordGlow, Offset(segX - u * 0.8f, segY - u * 0.4f), Size(u * 1.8f, u * 0.9f))
    }

    // Snout / Cephalon with small sensory barbel tentacles
    drawRect(bodyColor, Offset(cx + u * 1.2f, cy - u * 1.2f), Size(u * 2f, u * 2.4f))
    drawRect(Color(0xFFCAF0F8), Offset(cx + u * 2.8f, cy - u * 0.8f), Size(u * 1.8f, u * 0.6f))
    drawRect(Color(0xFFCAF0F8), Offset(cx + u * 2.8f, cy + u * 0.3f), Size(u * 1.8f, u * 0.6f))

    // Caudal fin ribbon (tail)
    val tailX = cx - segCount * (u * 2.4f)
    val tailY = cy + sin(swimPhase + segCount * 0.65f) * (u * 1.8f)
    drawRect(notochordGlow.copy(alpha = 0.75f), Offset(tailX - u * 2.5f, tailY - u * 2f), Size(u * 3f, u * 4f))
}

/**
 * High-definition 16-bit Stromatolite Reef Colony.
 * Domed mineralized microbial mounds with vibrant emerald-cyan photosynthetic biofilms
 * and gentle rising translucent oxygen bubbles (O2).
 */
private fun DrawScope.drawDetailedStromatoliteReef(
    cx: Float,
    cy: Float,
    scale: Float,
    swimPhase: Float,
    isSelected: Boolean,
    animTick: Float
) {
    val u = 3.6f * scale

    val rockDark = Color(0xFF1B4332)
    val rockLayer = Color(0xFF2D6A4F)
    val bioFilm = if (isSelected) Color(0xFF74C69D) else Color(0xFF40916C)
    val microbialGlow = Color(0xFF52B788)

    // Domed Reef Mound 1 (Large Main Dome)
    drawRect(rockDark, Offset(cx - u * 6f, cy - u * 2f), Size(u * 12f, u * 7f))
    drawRect(rockLayer, Offset(cx - u * 5f, cy - u * 3.5f), Size(u * 10f, u * 6f))
    drawRect(bioFilm, Offset(cx - u * 4f, cy - u * 4.8f), Size(u * 8f, u * 3.5f))
    drawRect(microbialGlow, Offset(cx - u * 2.5f, cy - u * 5.6f), Size(u * 5f, u * 1.8f))

    // Domed Reef Mound 2 (Adjacent Satellite Dome)
    drawRect(rockDark, Offset(cx + u * 4f, cy - u * 1f), Size(u * 6f, u * 5f))
    drawRect(bioFilm, Offset(cx + u * 4.5f, cy - u * 2.2f), Size(u * 5f, u * 3f))
    drawRect(microbialGlow, Offset(cx + u * 5.0f, cy - u * 3.0f), Size(u * 3.5f, u * 1.4f))

    // RISING OXYGEN BUBBLES (O2) - Oxygenating the primordial seas
    for (b in 0..4) {
        val bubblePhase = (swimPhase * 0.8f + b * 1.4f) % 6f
        val bubbleY = cy - u * 6f - bubblePhase * (u * 4.5f)
        val bubbleX = cx + sin(bubblePhase * 2f + b * 1.1f) * (u * 3f)
        val bubbleSize = (1.5f + (b % 3) * 0.8f) * u * 0.5f

        drawCircle(
            color = Color(0xFF99F6E4).copy(alpha = 0.75f),
            radius = bubbleSize,
            center = Offset(bubbleX, bubbleY)
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.9f),
            radius = bubbleSize * 0.35f,
            center = Offset(bubbleX - bubbleSize * 0.3f, bubbleY - bubbleSize * 0.3f)
        )
    }
}

/**
 * Custom / Engineered synthetic organism with articulated bioluminescent styling.
 */
private fun DrawScope.drawDetailedEngineeredOrganism(
    cx: Float,
    cy: Float,
    org: LivingOrganism,
    scale: Float,
    swimPhase: Float,
    isSelected: Boolean
) {
    val u = 3.2f * scale
    val mainCol = if (isSelected) Color(0xFFF72585) else Color(0xFF7209B7)
    val accentCol = Color(0xFF4CC9F0)

    val segments = org.phenotype.segments.ifEmpty { listOf(SegmentUnit(0, SegmentType.THORACIC, 1f, 2, LimbType.HYDROFOIL_FIN)) }

    for (i in segments.indices) {
        val seg = segments[i]
        val segX = cx - i * (u * 2.5f)
        val waveY = sin(swimPhase + i * 0.8f) * (u * 1.5f)
        val segY = cy + waveY
        val segW = seg.widthFraction * (u * 3.5f)

        // Limbs/Fins
        if (seg.limbPairCount > 0) {
            val legColor = if (seg.limbType == LimbType.JOINTED_WALKER) PixelGold else accentCol
            drawRect(legColor, Offset(segX, segY - segW - u * 2f), Size(u * 2f, u * 2f))
            drawRect(legColor, Offset(segX, segY + segW), Size(u * 2f, u * 2f))
        }

        // Segment body
        drawRect(mainCol, Offset(segX - u, segY - segW), Size(u * 2.2f, segW * 2f))
        drawRect(accentCol, Offset(segX - u * 0.4f, segY - u * 0.5f), Size(u * 1f, u * 1f))
    }

    // Cephalic Head
    drawRect(mainCol, Offset(cx + u * 1.5f, cy - u * 2.5f), Size(u * 2.8f, u * 5f))
    drawRect(Color(0xFF4CC9F0), Offset(cx + u * 2.5f, cy - u * 1.5f), Size(u * 1.2f, u * 1.2f))
    drawRect(Color(0xFF4CC9F0), Offset(cx + u * 2.5f, cy + u * 0.3f), Size(u * 1.2f, u * 1.2f))
}

/**
 * Animated High-Contrast Targeting Reticle framing the selected organism.
 */
private fun DrawScope.drawSelectionReticle(
    cx: Float,
    cy: Float,
    scale: Float,
    animTick: Float
) {
    val r = 32.dp.toPx() * scale
    val pulse = sin(animTick * 4f) * 2.dp.toPx()
    val outerR = r + pulse

    // Glowing Circular Field
    drawCircle(
        color = PixelGold.copy(alpha = 0.22f),
        radius = outerR,
        center = Offset(cx, cy)
    )
    drawCircle(
        color = PixelGold.copy(alpha = 0.85f),
        radius = outerR,
        center = Offset(cx, cy),
        style = Stroke(width = 2.dp.toPx())
    )

    // Corner Target Brackets [ ⌜ ⌝ ⌞ ⌟ ]
    val bracketLen = 10.dp.toPx()
    val bDist = outerR + 4.dp.toPx()

    // Top-Left ⌜
    drawLine(PixelGold, Offset(cx - bDist, cy - bDist), Offset(cx - bDist + bracketLen, cy - bDist), strokeWidth = 3.dp.toPx())
    drawLine(PixelGold, Offset(cx - bDist, cy - bDist), Offset(cx - bDist, cy - bDist + bracketLen), strokeWidth = 3.dp.toPx())

    // Top-Right ⌝
    drawLine(PixelGold, Offset(cx + bDist, cy - bDist), Offset(cx + bDist - bracketLen, cy - bDist), strokeWidth = 3.dp.toPx())
    drawLine(PixelGold, Offset(cx + bDist, cy - bDist), Offset(cx + bDist, cy - bDist + bracketLen), strokeWidth = 3.dp.toPx())

    // Bottom-Left ⌞
    drawLine(PixelGold, Offset(cx - bDist, cy + bDist), Offset(cx - bDist + bracketLen, cy + bDist), strokeWidth = 3.dp.toPx())
    drawLine(PixelGold, Offset(cx - bDist, cy + bDist), Offset(cx - bDist, cy + bDist - bracketLen), strokeWidth = 3.dp.toPx())

    // Bottom-Right ⌟
    drawLine(PixelGold, Offset(cx + bDist, cy + bDist), Offset(cx + bDist - bracketLen, cy + bDist), strokeWidth = 3.dp.toPx())
    drawLine(PixelGold, Offset(cx + bDist, cy + bDist), Offset(cx + bDist, cy + bDist - bracketLen), strokeWidth = 3.dp.toPx())
}

/**
 * Floating species badge and behavior tag drawn cleanly above each organism.
 */
private fun DrawScope.drawOrganismNameTag(
    ox: Float,
    oy: Float,
    org: LivingOrganism,
    isSelected: Boolean
) {
    val textPaint = android.graphics.Paint().apply {
        isAntiAlias = true
        textSize = if (isSelected) 12.dp.toPx() else 10.dp.toPx()
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        color = if (isSelected) android.graphics.Color.YELLOW else if (org.isStressed) android.graphics.Color.rgb(255, 120, 120) else android.graphics.Color.WHITE
        textAlign = android.graphics.Paint.Align.CENTER
    }

    val subPaint = android.graphics.Paint().apply {
        isAntiAlias = true
        textSize = 9.dp.toPx()
        typeface = android.graphics.Typeface.MONOSPACE
        color = if (org.isStressed) android.graphics.Color.rgb(255, 180, 180) else android.graphics.Color.LTGRAY
        textAlign = android.graphics.Paint.Align.CENTER
    }

    val label = if (org.isStressed) "⚠️ ${org.commonName.split("(").first().trim()}" else "${org.emoji} ${org.commonName.split("(").first().trim()}"
    val statusText = if (org.isStressed && org.stressReason != null) {
        "⚠️ ${org.stressReason}"
    } else {
        "${org.behavior.emoji} ${org.adaptationNote}"
    }

    val textWidth = max(textPaint.measureText(label), if (isSelected) subPaint.measureText(statusText) else 0f)
    val pillW = max(textWidth + 18.dp.toPx(), 72.dp.toPx())
    val pillH = if (isSelected) 30.dp.toPx() else 18.dp.toPx()

    // Background pill
    val bgCol = if (isSelected) PixelPanel.copy(alpha = 0.95f) else Color(0xFF0F172A).copy(alpha = 0.82f)
    val borderCol = if (isSelected) PixelGold else if (org.isStressed) PixelCrimson else PixelBorderLight

    drawRoundRect(
        color = bgCol,
        topLeft = Offset(ox - pillW / 2f, oy - pillH / 2f),
        size = Size(pillW, pillH),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx())
    )
    drawRoundRect(
        color = borderCol,
        topLeft = Offset(ox - pillW / 2f, oy - pillH / 2f),
        size = Size(pillW, pillH),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx()),
        style = Stroke(width = if (isSelected || org.isStressed) 1.8.dp.toPx() else 1.dp.toPx())
    )

    // Draw text using native Canvas
    drawContext.canvas.nativeCanvas.drawText(
        label,
        ox,
        oy + (if (isSelected) -3.dp.toPx() else 4.dp.toPx()),
        textPaint
    )

    if (isSelected) {
        drawContext.canvas.nativeCanvas.drawText(
            statusText,
            ox,
            oy + 10.dp.toPx(),
            subPaint
        )
    }
}

/**
 * Drifting 16-bit pixel cloud clusters and falling rain droplets (honors animTick).
 */
private fun DrawScope.drawPixelWeather(
    w: Float,
    h: Float,
    pixelSize: Float,
    weather: WeatherState,
    animTick: Float,
    planetary: PlanetaryState
) {
    // 1. Drifting 16-bit Cloud Clusters
    weather.clouds.forEach { cloud ->
        val cx = cloud.xRatio * w
        val cy = cloud.yRatio * h
        val qx = (cx / pixelSize).toInt() * pixelSize
        val qy = (cy / pixelSize).toInt() * pixelSize

        val cloudColor = Color.White.copy(alpha = cloud.density.coerceIn(0.4f, 0.85f))
        val shadowColor = Color.Black.copy(alpha = 0.22f)

        // Draw cloud drop-shadow
        for (dy in 0 until cloud.heightTiles) {
            for (dx in 0 until cloud.widthTiles) {
                if ((dx == 0 || dx == cloud.widthTiles - 1) && (dy == 0 || dy == cloud.heightTiles - 1)) {
                    continue // Rounded pixel corners
                }
                drawRect(
                    color = shadowColor,
                    topLeft = Offset(qx + dx * pixelSize + 4.dp.toPx(), qy + dy * pixelSize + 6.dp.toPx()),
                    size = Size(pixelSize, pixelSize)
                )
            }
        }

        // Draw cloud body
        for (dy in 0 until cloud.heightTiles) {
            for (dx in 0 until cloud.widthTiles) {
                if ((dx == 0 || dx == cloud.widthTiles - 1) && (dy == 0 || dy == cloud.heightTiles - 1)) {
                    continue
                }
                val isShaded = dy == cloud.heightTiles - 1
                val col = if (isShaded) Color(0xFFE2E8F0).copy(alpha = cloud.density) else cloudColor
                drawRect(
                    color = col,
                    topLeft = Offset(qx + dx * pixelSize, qy + dy * pixelSize),
                    size = Size(pixelSize, pixelSize)
                )
            }
        }
    }

    // 2. Animated Rain Particles
    weather.rainZones.forEach { zone ->
        val rX = zone.centerTileX * pixelSize
        val rY = zone.centerTileY * pixelSize
        val radiusPx = zone.radiusTiles * pixelSize

        for (i in 0 until 12) {
            val dropX = rX + sin(animTick * 3f + i * 1.7f) * radiusPx
            val dropY = (rY + (animTick * 80f + i * 20f) % (radiusPx * 2f))
            drawRect(
                color = Color(0xFF7DD3FC),
                topLeft = Offset(dropX, dropY),
                size = Size(2.dp.toPx(), 5.dp.toPx())
            )
        }
    }
}

/**
 * Sweeps day-night terminator shadow or renders fixed radial twilight for Eyeball worlds.
 */
private fun DrawScope.drawDayNightTerminator(
    w: Float,
    h: Float,
    pixelSize: Float,
    weather: WeatherState,
    planetary: PlanetaryState
) {
    if (planetary.isEyeballWorld) {
        val cx = w * 0.5f
        val cy = h * 0.5f
        val stepCount = 6

        for (s in stepCount downTo 1) {
            val ratio = s.toFloat() / stepCount
            if (ratio > 0.65f) {
                val shadowAlpha = ((ratio - 0.65f) / 0.35f) * 0.65f
                drawRect(
                    color = Color(0xFF030712).copy(alpha = shadowAlpha),
                    topLeft = Offset.Zero,
                    size = Size(w, h)
                )
            }
        }
    } else {
        val progress = weather.dayNightCycleProgress
        val nightStartX = (progress * w) % w
        val nightWidth = w * 0.48f

        val cols = ceil(w / pixelSize).toInt()
        val startCol = (nightStartX / pixelSize).toInt()
        val nightColSpan = (nightWidth / pixelSize).toInt()

        for (c in 0 until cols) {
            val isNight = if (nightStartX + nightWidth <= w) {
                c in startCol until (startCol + nightColSpan)
            } else {
                c >= startCol || c < ((startCol + nightColSpan) % cols)
            }

            if (isNight) {
                drawRect(
                    color = Color(0xFF020617).copy(alpha = 0.52f),
                    topLeft = Offset(c * pixelSize, 0f),
                    size = Size(pixelSize, h)
                )
            }
        }
    }
}

private fun DrawScope.drawPixelBiomassHeatmap(cols: Int, rows: Int, pixelSize: Float, animTick: Float) {
    val cx = cols / 2f
    val cy = rows / 2f
    for (r in 0 until rows step 3) {
        for (c in 0 until cols step 3) {
            val dist = hypot(c - cx, r - cy)
            if (dist in 4f..18f) {
                val intensity = (sin(dist * 0.4f + animTick * 0.8f) * 0.5f + 0.5f) * 0.35f
                drawRect(
                    color = PixelEmerald.copy(alpha = intensity),
                    topLeft = Offset(c * pixelSize, r * pixelSize),
                    size = Size(pixelSize * 3, pixelSize * 3)
                )
            }
        }
    }
}

private fun DrawScope.drawPixelTrophicFlow(w: Float, h: Float, pixelSize: Float, animTick: Float) {
    val arrowCount = 5
    for (i in 0 until arrowCount) {
        val y = h * 0.35f + i * (h * 0.08f)
        val x = ((animTick * 30f + i * (w / arrowCount)) % w)
        drawRect(PixelCyan.copy(alpha = 0.5f), Offset(x, y), Size(pixelSize * 2, pixelSize * 0.5f))
        drawRect(PixelCyan.copy(alpha = 0.7f), Offset(x + pixelSize * 2, y - pixelSize * 0.25f), Size(pixelSize * 0.5f, pixelSize))
    }
}

private fun DrawScope.drawPixelSpeciationHotspots(w: Float, h: Float, pixelSize: Float, animTick: Float) {
    val pulse = (sin(animTick * 2f) * 0.5f + 0.5f) * pixelSize * 4
    val hx = w * 0.4f
    val hy = h * 0.55f
    drawRect(PixelPurple.copy(alpha = 0.4f), Offset(hx - pulse, hy - pulse), Size(pulse * 2, pulse * 2))
}

private fun DrawScope.drawPixelCataclysm(w: Float, h: Float, pixelSize: Float, cataclysm: ActiveCataclysm, animTick: Float) {
    val progress = (cataclysm.elapsedSeconds / cataclysm.durationSeconds).coerceIn(0f, 1f)
    when (cataclysm.type) {
        CataclysmType.CHICXULUB_BOLIDE -> {
            val targetX = (w * 0.45f / pixelSize).toInt() * pixelSize
            val targetY = (h * 0.50f / pixelSize).toInt() * pixelSize

            if (progress < 0.35f) {
                val meteorX = targetX - (1f - progress / 0.35f) * 200f
                val meteorY = targetY - (1f - progress / 0.35f) * 200f
                drawRect(Color(0xFF2B2D42), Offset(meteorX, meteorY), Size(pixelSize * 2, pixelSize * 2))
                drawRect(PixelFire, Offset(meteorX - pixelSize * 2, meteorY - pixelSize * 2), Size(pixelSize * 2, pixelSize * 2))
            } else {
                val shockRadius = ((progress - 0.35f) / 0.65f) * 160f
                val sR = (shockRadius / pixelSize).toInt() * pixelSize
                val shockColor = PixelCrimson.copy(alpha = (1f - progress).coerceIn(0.1f, 0.9f))
                drawRect(PixelLava, Offset(targetX - pixelSize * 2, targetY - pixelSize * 2), Size(pixelSize * 4, pixelSize * 4))
                drawRect(shockColor, Offset(targetX - sR, targetY - sR), Size(sR * 2, pixelSize))
                drawRect(shockColor, Offset(targetX - sR, targetY + sR), Size(sR * 2, pixelSize))
            }
        }
        CataclysmType.LARGE_IGNEOUS_PROVINCE -> {
            val plumeX = (w * 0.5f / pixelSize).toInt() * pixelSize
            val plumeY = (h * 0.5f / pixelSize).toInt() * pixelSize
            val radiusTiles = (cataclysm.shockwaveRadiusPercent * 8).toInt()
            for (dr in -radiusTiles..radiusTiles) {
                for (dc in -radiusTiles..radiusTiles) {
                    if (abs(dr) + abs(dc) <= radiusTiles) {
                        val color = if ((dr + dc) % 2 == 0) PixelLava else PixelFire
                        drawRect(color.copy(alpha = 0.85f), Offset(plumeX + dc * pixelSize, plumeY + dr * pixelSize), Size(pixelSize, pixelSize))
                    }
                }
            }
        }
        CataclysmType.SNOWBALL_RUNAWAY -> {
            val frostColor = PixelSnow.copy(alpha = (0.6f * cataclysm.shockwaveRadiusPercent).coerceIn(0f, 0.8f))
            drawRect(frostColor, Offset.Zero, Size(w, h))
        }
        CataclysmType.GAMMA_RAY_BURST -> {
            val beamX = (w * 0.5f / pixelSize).toInt() * pixelSize
            drawRect(PixelPurple, Offset(beamX - pixelSize * 2, 0f), Size(pixelSize * 4, h))
            drawRect(Color(0xFFE0AAFF), Offset(beamX - pixelSize, 0f), Size(pixelSize * 2, h))
        }
    }
}

/**
 * 16-bit Icosahedral Capsid Bacteriophage / Microscopic Virus.
 * Geometric crystalline protein shell with glowing viral genome and Brownian injector pins.
 */
private fun DrawScope.drawDetailedVirus(
    cx: Float,
    cy: Float,
    scale: Float,
    swimPhase: Float,
    isSelected: Boolean
) {
    val u = 2.8f * scale
    val capsidColor = if (isSelected) Color(0xFFF72585) else Color(0xFFB5179E)
    val capsidFacet = Color(0xFF7209B7)
    val rnaGlow = Color(0xFF4CC9F0)
    val legColor = Color(0xFF4895EF)

    // Jitter Brownian oscillation
    val jitterX = sin(swimPhase * 8f) * (u * 0.6f)
    val jitterY = cos(swimPhase * 9f) * (u * 0.6f)
    val jcx = cx + jitterX
    val jcy = cy + jitterY

    // 1. Icosahedral 20-faced capsid head (Geometric faceted diamond)
    drawRect(capsidFacet, Offset(jcx - u * 3.5f, jcy - u * 3.5f), Size(u * 7f, u * 7f))
    drawRect(capsidColor, Offset(jcx - u * 2.5f, jcy - u * 2.5f), Size(u * 5f, u * 5f))
    drawRect(Color(0xFFF72585), Offset(jcx - u * 1.5f, jcy - u * 1.5f), Size(u * 3f, u * 3f))

    // Glowing core nucleic acid (vDNA / vRNA)
    drawRect(rnaGlow, Offset(jcx - u * 0.8f, jcy - u * 0.8f), Size(u * 1.6f, u * 1.6f))

    // 2. Contractive Neck & Sheath tube
    drawRect(capsidFacet, Offset(jcx - u * 5.5f, jcy - u * 0.8f), Size(u * 2.5f, u * 1.6f))
    drawRect(legColor, Offset(jcx - u * 6.5f, jcy - u * 1.2f), Size(u * 1.2f, u * 2.4f))

    // 3. Spider-like flexible Tail Fibers / Injector pins
    for (i in -1..1) {
        val fPhase = sin(swimPhase * 6f + i * 1.5f) * (u * 1.2f)
        val ly = jcy + i * (u * 2.2f)
        drawRect(legColor, Offset(jcx - u * 8.5f, ly + fPhase), Size(u * 2.5f, u * 0.6f))
        drawRect(capsidColor, Offset(jcx - u * 10f, ly + fPhase * 1.4f), Size(u * 1.8f, u * 0.6f))
    }
}

/**
 * 16-bit Armored Placoderm Super-Predator (Dunkleosteus armoris).
 * Heavy dermal bone cranial plates, razor-sharp shearing gnathal jaw, and muscular shark caudal body.
 */
private fun DrawScope.drawDetailedPlacoderm(
    cx: Float,
    cy: Float,
    scale: Float,
    swimPhase: Float,
    isSelected: Boolean
) {
    val u = 3.6f * scale
    val armorPlate = if (isSelected) Color(0xFF64748B) else Color(0xFF334155)
    val armorEdge = Color(0xFFB45309)
    val sharkBody = Color(0xFF1E293B)
    val eyeColor = Color(0xFFFACC15)
    val bladeColor = Color(0xFFF8FAFC)

    // Undulating shark caudal thrust
    val tailWave1 = sin(swimPhase) * (u * 2.2f)
    val tailWave2 = sin(swimPhase + 0.8f) * (u * 3.4f)

    // 1. Muscular Torpedo Body & Caudal Peduncle
    drawRect(sharkBody, Offset(cx - u * 6f, cy - u * 3.5f + tailWave1), Size(u * 6f, u * 7f))
    drawRect(sharkBody, Offset(cx - u * 12f, cy - u * 2.2f + tailWave2), Size(u * 6f, u * 4.4f))

    // 2. Heterocercal Asymmetric Caudal Tail Fin
    val finX = cx - u * 16f
    val finY = cy + tailWave2
    drawRect(armorPlate, Offset(finX - u * 4f, finY - u * 6f), Size(u * 4f, u * 4f))
    drawRect(armorPlate, Offset(finX - u * 2f, finY + u * 1.5f), Size(u * 2.5f, u * 3f))

    // 3. Cephalic & Thoracic Heavy Dermal Armor Shield (Cranial Bony Vault)
    drawRect(armorEdge, Offset(cx - u * 1.5f, cy - u * 5f), Size(u * 6.5f, u * 10f))
    drawRect(armorPlate, Offset(cx - u * 1f, cy - u * 4.5f), Size(u * 5.5f, u * 9f))
    drawRect(Color(0xFF475569), Offset(cx + u * 0.5f, cy - u * 3.2f), Size(u * 3.5f, u * 6.4f))

    // 4. Piercing Eye with Sclerotic Ring
    drawRect(Color.Black, Offset(cx + u * 2.5f, cy - u * 3f), Size(u * 1.8f, u * 1.8f))
    drawRect(eyeColor, Offset(cx + u * 2.8f, cy - u * 2.8f), Size(u * 1.2f, u * 1.2f))
    drawRect(Color.Black, Offset(cx + u * 3.2f, cy - u * 2.6f), Size(u * 0.6f, u * 0.8f))

    // 5. Gnathal Shearing Jaw Blades (Bone Shears, not teeth!)
    val jawSnap = abs(sin(swimPhase * 2.5f)) * (u * 1.2f)
    drawRect(bladeColor, Offset(cx + u * 4.5f, cy - u * 1.8f - jawSnap), Size(u * 2.2f, u * 1.4f))
    drawRect(bladeColor, Offset(cx + u * 4.2f, cy + u * 0.5f + jawSnap), Size(u * 2.4f, u * 1.4f))

    // 6. Broad Pectoral Hydrofoil Stabilizers
    drawRect(armorPlate, Offset(cx - u * 2f, cy - u * 7f), Size(u * 3f, u * 2.5f))
    drawRect(armorPlate, Offset(cx - u * 2f, cy + u * 4.5f), Size(u * 3f, u * 2.5f))
}

/**
 * 16-bit Sarcopterygian Tetrapod Walker (Tiktaalik amphibium).
 * Sprawling amphibious skull, robust weight-bearing pectoral limbs, and dappled riverbed scales.
 */
private fun DrawScope.drawDetailedTetrapod(
    cx: Float,
    cy: Float,
    scale: Float,
    swimPhase: Float,
    isSelected: Boolean
) {
    val u = 3.4f * scale
    val dermalMain = if (isSelected) Color(0xFF52B788) else Color(0xFF2D6A4F)
    val dermalDark = Color(0xFF1B4332)
    val bellyCream = Color(0xFFD8F3DC)
    val eyeGreen = Color(0xFFA3E635)

    // Crawling gait kinematics
    val limbCycleL = sin(swimPhase) * (u * 2.2f)
    val limbCycleR = -limbCycleL

    // 1. Sprawling Robust Jointed Limbs
    drawRect(dermalDark, Offset(cx + u * 1f, cy - u * 5.5f + limbCycleL), Size(u * 2.5f, u * 2f))
    drawRect(dermalMain, Offset(cx + u * 2.2f, cy - u * 7.5f + limbCycleL), Size(u * 2.2f, u * 2.4f))
    drawRect(dermalDark, Offset(cx + u * 1f, cy + u * 3.5f + limbCycleR), Size(u * 2.5f, u * 2f))
    drawRect(dermalMain, Offset(cx + u * 2.2f, cy + u * 5.5f + limbCycleR), Size(u * 2.2f, u * 2.4f))
    drawRect(dermalDark, Offset(cx - u * 6f, cy - u * 5.0f - limbCycleL), Size(u * 2.5f, u * 2f))
    drawRect(dermalMain, Offset(cx - u * 5.5f, cy - u * 7.0f - limbCycleL), Size(u * 2.2f, u * 2.2f))
    drawRect(dermalDark, Offset(cx - u * 6f, cy + u * 3.2f - limbCycleR), Size(u * 2.5f, u * 2f))
    drawRect(dermalMain, Offset(cx - u * 5.5f, cy + u * 5.0f - limbCycleR), Size(u * 2.2f, u * 2.2f))

    // 2. Torpedo Amphibious Trunk & Tail
    val tailUndulation = sin(swimPhase * 0.8f) * (u * 1.8f)
    drawRect(dermalDark, Offset(cx - u * 8f, cy - u * 2.8f + tailUndulation), Size(u * 8f, u * 5.6f))
    drawRect(dermalMain, Offset(cx - u * 7.5f, cy - u * 2.2f + tailUndulation), Size(u * 7f, u * 4.4f))
    drawRect(bellyCream, Offset(cx - u * 5f, cy - u * 0.8f + tailUndulation), Size(u * 4f, u * 1.6f))
    drawRect(dermalMain.copy(alpha = 0.8f), Offset(cx - u * 13f, cy - u * 1.8f + tailUndulation * 1.6f), Size(u * 5f, u * 3.6f))

    // 3. Flattened Crocodilian Skull
    drawRect(dermalDark, Offset(cx - u * 0.5f, cy - u * 3.6f), Size(u * 5.5f, u * 7.2f))
    drawRect(dermalMain, Offset(cx, cy - u * 3.0f), Size(u * 4.5f, u * 6.0f))

    // Raised dorsal eyes
    drawRect(Color.Black, Offset(cx + u * 1.5f, cy - u * 2.8f), Size(u * 1.6f, u * 1.6f))
    drawRect(eyeGreen, Offset(cx + u * 1.8f, cy - u * 2.6f), Size(u * 1.0f, u * 1.0f))
    drawRect(Color.Black, Offset(cx + u * 1.5f, cy + u * 1.2f), Size(u * 1.6f, u * 1.6f))
    drawRect(eyeGreen, Offset(cx + u * 1.8f, cy + u * 1.4f), Size(u * 1.0f, u * 1.0f))

    // Spiracular air breathing notch
    drawRect(Color(0xFF0F172A), Offset(cx + u * 0.2f, cy - u * 0.5f), Size(u * 0.8f, u * 1.0f))
}

/**
 * 16-bit Bipedal Theropod Dinosaur (Archeoraptor velox).
 * High-speed digitigrade raptor legs, sickle claw, S-curved predatory neck, and stiffened balance tail.
 */
private fun DrawScope.drawDetailedDinosaur(
    cx: Float,
    cy: Float,
    scale: Float,
    swimPhase: Float,
    isSelected: Boolean
) {
    val u = 3.6f * scale
    val dinoMain = if (isSelected) Color(0xFFF59E0B) else Color(0xFFD97706)
    val dinoStripe = Color(0xFF991B1B)
    val featherCrest = Color(0xFFFDE68A)
    val legMuscles = Color(0xFFB45309)

    // Cursorial sprint stride kinematics
    val legStrideL = sin(swimPhase * 1.4f) * (u * 3.2f)
    val legStrideR = -legStrideL

    // 1. Long Stiffened Counterbalancing Tail
    val tailWave = sin(swimPhase * 0.7f) * (u * 1.2f)
    drawRect(dinoMain, Offset(cx - u * 15f, cy - u * 1.2f + tailWave), Size(u * 9f, u * 2.4f))
    drawRect(dinoStripe, Offset(cx - u * 12f, cy - u * 1.4f + tailWave), Size(u * 2f, u * 2.8f))
    drawRect(featherCrest, Offset(cx - u * 18f, cy - u * 2f + tailWave), Size(u * 3.5f, u * 4f))

    // 2. Powerful Bipedal Running Legs & Sickle Claw
    drawRect(legMuscles, Offset(cx - u * 5f, cy - u * 5f + legStrideL), Size(u * 3f, u * 3.5f))
    drawRect(dinoMain, Offset(cx - u * 3.5f, cy - u * 7.5f + legStrideL), Size(u * 1.8f, u * 3f))
    drawRect(Color.Black, Offset(cx - u * 1.5f, cy - u * 8f + legStrideL), Size(u * 2.5f, u * 1.2f))
    drawRect(legMuscles, Offset(cx - u * 5f, cy + u * 1.5f + legStrideR), Size(u * 3f, u * 3.5f))
    drawRect(dinoMain, Offset(cx - u * 3.5f, cy + u * 4.5f + legStrideR), Size(u * 1.8f, u * 3f))
    drawRect(Color.Black, Offset(cx - u * 1.5f, cy + u * 7f + legStrideR), Size(u * 2.5f, u * 1.2f))

    // 3. Predatory Torso & Ribcage
    drawRect(dinoMain, Offset(cx - u * 6f, cy - u * 3f), Size(u * 6.5f, u * 6f))
    drawRect(dinoStripe, Offset(cx - u * 4f, cy - u * 3.2f), Size(u * 1.5f, u * 6.4f))
    drawRect(dinoStripe, Offset(cx - u * 1.8f, cy - u * 3.2f), Size(u * 1.5f, u * 6.4f))

    // 4. S-Curved Neck & Crested Skull
    drawRect(dinoMain, Offset(cx, cy - u * 2.5f), Size(u * 4f, u * 5f))
    drawRect(featherCrest, Offset(cx + u * 1f, cy - u * 4f), Size(u * 2.5f, u * 1.8f))

    // Amber Raptor Eye
    drawRect(Color.Black, Offset(cx + u * 2f, cy - u * 2f), Size(u * 1.4f, u * 1.4f))
    drawRect(Color(0xFFFEF08A), Offset(cx + u * 2.3f, cy - u * 1.8f), Size(u * 0.8f, u * 0.8f))

    // Serrated Ivory Jaw Snout
    val jawPulse = abs(sin(swimPhase)) * (u * 0.8f)
    drawRect(dinoMain, Offset(cx + u * 4f, cy - u * 1.5f - jawPulse), Size(u * 3.2f, u * 1.5f))
    drawRect(Color.White, Offset(cx + u * 4.5f, cy - u * 0.2f), Size(u * 2.2f, u * 0.5f))
    drawRect(dinoMain, Offset(cx + u * 4f, cy + u * 0.4f + jawPulse), Size(u * 3.0f, u * 1.2f))
}

/**
 * 16-bit Feathered Proto-Bird (Protoavis plumata).
 * Aerodynamic flight plumage, flapping wing primary feathers, and lightweight crosscurrent air sac body.
 */
private fun DrawScope.drawDetailedBird(
    cx: Float,
    cy: Float,
    scale: Float,
    swimPhase: Float,
    isSelected: Boolean
) {
    val u = 3.2f * scale
    val wingCyan = if (isSelected) Color(0xFF67E8F9) else Color(0xFF06B6D4)
    val featherWhite = Color(0xFFF8FAFC)
    val wingDark = Color(0xFF0E7490)
    val beakGold = Color(0xFFFBBF24)

    val wingFlap = sin(swimPhase * 5f) * (u * 3.5f)

    // Primary Flight Wings
    drawRect(wingDark, Offset(cx - u * 3f, cy - u * 4f - wingFlap), Size(u * 5f, u * 4.5f))
    drawRect(wingCyan, Offset(cx - u * 2.5f, cy - u * 7.5f - wingFlap), Size(u * 4f, u * 4f))
    drawRect(featherWhite, Offset(cx - u * 2f, cy - u * 10f - wingFlap), Size(u * 3f, u * 3f))
    drawRect(wingDark, Offset(cx - u * 3f, cy + u * 2f + wingFlap), Size(u * 5f, u * 4.5f))
    drawRect(wingCyan, Offset(cx - u * 2.5f, cy + u * 4.5f + wingFlap), Size(u * 4f, u * 4f))
    drawRect(featherWhite, Offset(cx - u * 2f, cy + u * 7.5f + wingFlap), Size(u * 3f, u * 3f))

    // Streamlined Avian Body & Fan Tail
    drawRect(wingDark, Offset(cx - u * 5f, cy - u * 2.2f), Size(u * 6.5f, u * 4.4f))
    drawRect(wingCyan, Offset(cx - u * 4.5f, cy - u * 1.8f), Size(u * 5.5f, u * 3.6f))
    drawRect(featherWhite, Offset(cx - u * 3f, cy - u * 0.8f), Size(u * 3.5f, u * 1.6f))
    drawRect(wingCyan, Offset(cx - u * 11f, cy - u * 2.5f), Size(u * 6.5f, u * 5f))
    drawRect(featherWhite, Offset(cx - u * 13f, cy - u * 2f), Size(u * 3f, u * 4f))

    // Avian Head with Sharp Beak
    drawRect(wingCyan, Offset(cx + u * 1.5f, cy - u * 1.8f), Size(u * 2.8f, u * 3.6f))
    drawRect(Color.Black, Offset(cx + u * 2.2f, cy - u * 1.2f), Size(u * 1.2f, u * 1.2f))
    drawRect(Color.White, Offset(cx + u * 2.5f, cy - u * 1.0f), Size(u * 0.6f, u * 0.6f))
    drawRect(beakGold, Offset(cx + u * 4.2f, cy - u * 0.8f), Size(u * 2.6f, u * 1.6f))
}

/**
 * 16-bit Endothermic Mammal (Morganucodon vivus).
 * Dense insulating chestnut fur, alert sensory whiskers, erect bounding limbs, and warm-blooded neocortex.
 */
private fun DrawScope.drawDetailedMammal(
    cx: Float,
    cy: Float,
    scale: Float,
    swimPhase: Float,
    isSelected: Boolean
) {
    val u = 3.0f * scale
    val furDark = if (isSelected) Color(0xFF92400E) else Color(0xFF78350F)
    val furMain = if (isSelected) Color(0xFFD97706) else Color(0xFFB45309)
    val bellyCream = Color(0xFFFEF3C7)
    val whiskerGold = Color(0xFFFDE68A)
    val eyeBlack = Color(0xFF0F172A)

    val strideL = sin(swimPhase * 1.8f) * (u * 2.0f)
    val strideR = -strideL

    // Erect Parasagittal Paws
    drawRect(furDark, Offset(cx + u * 1f, cy - u * 4f + strideL), Size(u * 1.8f, u * 2.4f))
    drawRect(furDark, Offset(cx + u * 1f, cy + u * 1.8f + strideR), Size(u * 1.8f, u * 2.4f))
    drawRect(furDark, Offset(cx - u * 5f, cy - u * 4f - strideL), Size(u * 2f, u * 2.4f))
    drawRect(furDark, Offset(cx - u * 5f, cy + u * 1.8f - strideR), Size(u * 2f, u * 2.4f))

    // Insulated Fur Body
    drawRect(furDark, Offset(cx - u * 6f, cy - u * 2.6f), Size(u * 7.5f, u * 5.2f))
    drawRect(furMain, Offset(cx - u * 5.5f, cy - u * 2.2f), Size(u * 6.5f, u * 4.4f))
    drawRect(bellyCream, Offset(cx - u * 3.5f, cy - u * 1.0f), Size(u * 4f, u * 2.0f))

    val tailFlex = sin(swimPhase) * (u * 1.5f)
    drawRect(furMain, Offset(cx - u * 11f, cy - u * 0.8f + tailFlex), Size(u * 5.5f, u * 1.4f))

    // Cephalon with Large Auditory Pinnae Ears
    drawRect(furMain, Offset(cx + u * 1.5f, cy - u * 2.2f), Size(u * 3.5f, u * 4.4f))
    drawRect(Color(0xFFFBCFE8), Offset(cx + u * 1.5f, cy - u * 4.2f), Size(u * 1.8f, u * 2.2f))
    drawRect(Color(0xFFFBCFE8), Offset(cx + u * 1.5f, cy + u * 2.2f), Size(u * 1.8f, u * 2.2f))

    drawRect(eyeBlack, Offset(cx + u * 2.8f, cy - u * 1.5f), Size(u * 1.4f, u * 1.4f))
    drawRect(Color.White, Offset(cx + u * 3.2f, cy - u * 1.3f), Size(u * 0.5f, u * 0.5f))

    // Rhinarium and Whiskers
    drawRect(Color(0xFF18181B), Offset(cx + u * 4.8f, cy - u * 0.6f), Size(u * 1.2f, u * 1.2f))
    drawRect(whiskerGold, Offset(cx + u * 5f, cy - u * 2f), Size(u * 2.5f, u * 0.5f))
    drawRect(whiskerGold, Offset(cx + u * 5f, cy + u * 1.6f), Size(u * 2.5f, u * 0.5f))
}

/**
 * 16-bit Hydrothermal Vent Chimney Chemolith (Abyssal black smoker).
 * Mineralized sulfide chimney venting billowing mineral smoke plumes with thermophilic bacterial mats.
 */
private fun DrawScope.drawDetailedChemolithVent(
    cx: Float,
    cy: Float,
    scale: Float,
    swimPhase: Float,
    isSelected: Boolean,
    animTick: Float
) {
    val u = 3.6f * scale
    val basaltDark = Color(0xFF0F172A)
    val sulfideOre = Color(0xFF334155)
    val thermophileGlow = if (isSelected) Color(0xFFF97316) else Color(0xFFEA580C)
    val sulfurYellow = Color(0xFFFACC15)

    // 1. Heavy Mineralized Chimney Stack
    drawRect(basaltDark, Offset(cx - u * 4f, cy - u * 2f), Size(u * 8f, u * 8f))
    drawRect(sulfideOre, Offset(cx - u * 3f, cy - u * 4f), Size(u * 6f, u * 6f))
    drawRect(basaltDark, Offset(cx - u * 2f, cy - u * 6f), Size(u * 4f, u * 4f))

    // 2. Thermophilic Biofilm Mats
    drawRect(thermophileGlow, Offset(cx - u * 3.5f, cy - u * 1f), Size(u * 2.5f, u * 2f))
    drawRect(sulfurYellow, Offset(cx + u * 1.5f, cy - u * 2.5f), Size(u * 2.0f, u * 1.8f))
    drawRect(thermophileGlow, Offset(cx - u * 1.5f, cy - u * 5.5f), Size(u * 3f, u * 1.2f))

    // 3. Billowing Black Smoker Mineral Clouds
    for (p in 0..4) {
        val plumePhase = (swimPhase * 1.2f + p * 1.3f) % 5f
        val plumeY = cy - u * 7f - plumePhase * (u * 4.2f)
        val plumeX = cx + sin(plumePhase * 2.5f + p) * (u * 2.5f)
        val plumeSize = (2f + (p % 3) * 1.2f) * u * 0.6f

        drawCircle(
            color = Color(0xFF1E293B).copy(alpha = 0.8f - plumePhase * 0.14f),
            radius = plumeSize,
            center = Offset(plumeX, plumeY)
        )
        if (plumePhase < 2.0f) {
            drawCircle(
                color = PixelLava.copy(alpha = 0.9f - plumePhase * 0.4f),
                radius = plumeSize * 0.45f,
                center = Offset(plumeX, plumeY)
            )
        }
    }
}
