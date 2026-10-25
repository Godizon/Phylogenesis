package com.example.ui.viewport

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.model.*
import com.example.ui.theme.*
import kotlin.math.*

/**
 * WorldBox-style 16-bit pixel art terrain and entity sandbox renderer with dynamic weather.
 */
@Composable
fun PixelWorldCanvas(
    planetary: PlanetaryState,
    clades: List<Clade>,
    overlays: ViewportOverlays,
    activeCataclysm: ActiveCataclysm?,
    weather: WeatherState = WeatherState(),
    onSelectSpecimen: (OrganismPhenotype) -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pixel_world_anim")

    // Wave foam and creature locomotion tick
    val animTick by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 60f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "anim_tick"
    )

    // Pulsing lava flicker
    val lavaFlicker by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "lava"
    )

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(clades) {
                detectTapGestures {
                    val candidate = clades.firstOrNull { !it.isExtinct }?.dominantMorphotype
                        ?: clades.firstOrNull()?.dominantMorphotype
                    if (candidate != null) {
                        onSelectSpecimen(candidate)
                    }
                }
            }
    ) {
        val w = size.width
        val h = size.height

        // Calculate discrete pixel grid dimensions
        val pixelSize = 14f // Size of each chunky pixel tile
        val cols = ceil(w / pixelSize).toInt()
        val rows = ceil(h / pixelSize).toInt()

        // 1. Draw Procedural WorldBox Terrain Grid
        drawPixelTerrain(
            cols = cols,
            rows = rows,
            pixelSize = pixelSize,
            planetary = planetary,
            animTick = animTick,
            lavaFlicker = lavaFlicker
        )

        // 2. Draw Pixelated Viewport Overlays
        if (overlays.showBiomassHeatmap) {
            drawPixelBiomassHeatmap(cols, rows, pixelSize, animTick)
        }
        if (overlays.showTrophicFlowVectors) {
            drawPixelTrophicFlow(w, h, pixelSize, animTick)
        }
        if (overlays.showSpeciationHotspots) {
            drawPixelSpeciationHotspots(w, h, pixelSize, animTick)
        }

        // 3. Draw Animated Pixel Creature Sprites
        drawPixelCreatures(
            w = w,
            h = h,
            pixelSize = pixelSize,
            clades = clades,
            animTick = animTick
        )

        // 4. Draw Dynamic Weather Layer (Drifting 16-bit clouds & rain)
        if (overlays.showWeatherClouds) {
            drawPixelWeather(
                w = w,
                h = h,
                pixelSize = pixelSize,
                weather = weather,
                animTick = animTick,
                planetary = planetary
            )
        }

        // 5. Day-Night Lighting Terminator
        if (overlays.showDayNightCycle) {
            drawDayNightTerminator(
                w = w,
                h = h,
                pixelSize = pixelSize,
                weather = weather,
                planetary = planetary
            )
        }

        // 6. Draw Pixel Cataclysm Effects
        if (activeCataclysm != null) {
            drawPixelCataclysm(
                w = w,
                h = h,
                pixelSize = pixelSize,
                cataclysm = activeCataclysm,
                animTick = animTick
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
    animTick: Float,
    lavaFlicker: Float
) {
    val cx = cols / 2f
    val cy = rows / 2f

    val isSnowball = planetary.isSnowballEarth
    val iceCoverage = planetary.iceAlbedoCoverage.coerceIn(0.05f, 0.95f)
    val hasVegetation = planetary.atmosphere.o2Percent > 8.0f
    val isVolcanic = planetary.volcanicActivityIndex > 0.5f

    for (r in 0 until rows) {
        val distYFromEquator = abs(r - cy) / cy // 0 at equator, 1 at poles

        for (c in 0 until cols) {
            val px = c * pixelSize
            val py = r * pixelSize

            // Procedural landmass elevation formula using pseudo-harmonic trigonometric noise
            val nx = (c - cx) * 0.16f
            val ny = (r - cy) * 0.16f
            val rawHeight = sin(nx) * cos(ny) + 0.45f * sin(nx * 2.2f + 1.2f) * cos(ny * 2.2f) -
                    (distYFromEquator * 0.35f)

            val tileColor = when {
                // SPECIAL CASE 1: Tidally Locked Eyeball World
                planetary.isEyeballWorld -> {
                    val radialDist = hypot((c - cx) / cx, (r - cy) / cy)
                    when {
                        // Antistellar dark side: kilometers-thick perpetual glacier
                        radialDist > 0.72f -> if ((c + r) % 2 == 0) PixelSnow else Color(0xFFD8EEF8)
                        // Habitable Twilight Ribbon: lush moss, ferns, and shallow turquoise shores
                        radialDist in 0.44f..0.72f -> {
                            if (rawHeight > 0.1f) {
                                if (hasVegetation) PixelGrass else PixelMicrobialMat
                            } else if (rawHeight > -0.1f) {
                                PixelSand
                            } else {
                                PixelShallowOcean
                            }
                        }
                        // Substellar boiling noon cauldron
                        else -> if (rawHeight > 0.35f) PixelRock else PixelDeepOcean
                    }
                }

                // SPECIAL CASE 2: Super-Earth (2.5g)
                planetary.isSuperEarth -> {
                    when {
                        rawHeight > 0.50f -> PixelMountain
                        rawHeight > 0.20f -> if ((c + r) % 3 == 0) Color(0xFF7B2CBF) else PixelForest
                        rawHeight > -0.05f -> PixelSand
                        rawHeight > -0.30f -> PixelShallowOcean
                        else -> PixelDeepOcean
                    }
                }

                // STANDARD TERRESTRIAL WORLD:
                // Ice / Glacier tile
                isSnowball && (distYFromEquator > 0.15f || rawHeight > -0.2f) -> {
                    if ((c + r) % 2 == 0) PixelSnow else Color(0xFFD8EEF8)
                }
                !isSnowball && (distYFromEquator > (1.0f - iceCoverage * 0.85f) || (rawHeight > 0.65f && distYFromEquator > 0.45f)) -> {
                    if ((c + r) % 2 == 0) PixelSnow else Color(0xFFD8EEF8)
                }

                // Volcanic Lava / LIP fissure tile
                isVolcanic && rawHeight in 0.25f..0.45f && ((c * 7 + r * 13) % 29 < 3) -> {
                    if (lavaFlicker > 0.5f) PixelLava else PixelLavaGlow
                }

                // High Mountains & Peaks
                rawHeight > 0.55f -> if ((c + r) % 3 == 0) PixelMountain else PixelRock

                // Lowlands / Biome
                rawHeight > 0.12f -> {
                    if (hasVegetation) {
                        if (rawHeight > 0.35f) PixelForest else PixelGrass
                    } else {
                        if (rawHeight < 0.25f) PixelMicrobialMat else Color(0xFF8B5A2B)
                    }
                }

                // Coast / Beach
                rawHeight > -0.05f -> PixelSand

                // Shallow Water / Reefs
                rawHeight > -0.25f -> {
                    val foamWave = (sin(animTick * 0.8f + c * 0.5f) > 0.65f)
                    if (foamWave && rawHeight > -0.12f) Color(0xFFA6E3E9) else PixelShallowOcean
                }

                // Mid Ocean Shelf
                rawHeight > -0.55f -> PixelMidOcean

                // Abyssal Deep Ocean
                else -> PixelDeepOcean
            }

            drawRect(
                color = tileColor,
                topLeft = Offset(px, py),
                size = Size(pixelSize, pixelSize)
            )
        }
    }
}

/**
 * Draws drifting 16-bit pixel cloud clusters and falling rain droplets.
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
        // Eyeball World: Fixed radial illumination (Noon at center, midnight at sides)
        val cx = w * 0.5f
        val cy = h * 0.5f
        val maxR = hypot(cx, cy)
        val stepCount = 6

        for (s in stepCount downTo 1) {
            val ratio = s.toFloat() / stepCount
            if (ratio > 0.65f) {
                // Nightside deep cold shadow
                val shadowAlpha = ((ratio - 0.65f) / 0.35f) * 0.65f
                drawRect(
                    color = Color(0xFF030712).copy(alpha = shadowAlpha),
                    topLeft = Offset.Zero,
                    size = Size(w, h)
                )
            }
        }
    } else {
        // Standard / Super-Earth: Rotating Day-Night terminator line
        val progress = weather.dayNightCycleProgress
        val nightStartX = (progress * w) % w
        val nightWidth = w * 0.48f

        // Draw dithered nighttime shadow band
        val cols = ceil(w / pixelSize).toInt()
        val rows = ceil(h / pixelSize).toInt()

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

/**
 * Renders authentic animated pixel creature sprites across the world map.
 */
private fun DrawScope.drawPixelCreatures(
    w: Float,
    h: Float,
    pixelSize: Float,
    clades: List<Clade>,
    animTick: Float
) {
    if (clades.isEmpty()) return
    for (i in 0 until 14) {
        val clade = clades[i % clades.size]
        val speed = if (clade.id == "clad_anomalocaris") 48f else 24f
        val x = (i * (w / 14f) + (animTick * speed + i * 50f)) % w
        val y = (h * 0.22f + sin(animTick * 0.12f + i * 1.5f) * (h * 0.32f)).coerceIn(pixelSize * 3, h - pixelSize * 3)

        val qx = (x / pixelSize).toInt() * pixelSize
        val qy = (y / pixelSize).toInt() * pixelSize
        val stepFrame = ((animTick * 4f + i).toInt() % 2 == 0)

        when (clade.id) {
            "clad_trilobita" -> drawPixelTrilobite(qx, qy, pixelSize * 0.5f, stepFrame)
            "clad_anomalocaris" -> drawPixelAnomalocaris(qx, qy, pixelSize * 0.55f, stepFrame)
            "clad_chordata" -> drawPixelPreChordate(qx, qy, pixelSize * 0.5f, stepFrame)
            else -> drawPixelCyanobion(qx, qy, pixelSize * 0.45f, stepFrame)
        }
    }
}

private fun DrawScope.drawPixelTrilobite(x: Float, y: Float, s: Float, step: Boolean) {
    val shellColor = Color(0xFFD4A373)
    val carapaceDark = Color(0xFF8B5A2B)
    drawRect(carapaceDark, Offset(x + s, y), Size(s * 4, s * 2))
    drawRect(shellColor, Offset(x, y + s * 2), Size(s * 6, s * 3))
    drawRect(carapaceDark, Offset(x + s * 2, y + s * 5), Size(s * 2, s * 2))
    val legOffset = if (step) s else 0f
    drawRect(PixelGold, Offset(x - s, y + s * 2 + legOffset), Size(s, s))
    drawRect(PixelGold, Offset(x + s * 6, y + s * 2 + legOffset), Size(s, s))
}

private fun DrawScope.drawPixelAnomalocaris(x: Float, y: Float, s: Float, step: Boolean) {
    val bodyColor = Color(0xFFE63946)
    val finColor = Color(0xFFA8DADC)
    drawRect(bodyColor, Offset(x, y), Size(s * 8, s * 3))
    drawRect(PixelGold, Offset(x + s * 8, y - s), Size(s * 2, s * 2))
    drawRect(PixelGold, Offset(x + s * 8, y + s * 2), Size(s * 2, s * 2))
    val flap = if (step) s * 1.5f else 0f
    drawRect(finColor, Offset(x + s * 2, y - s * 2 + flap), Size(s * 4, s))
    drawRect(finColor, Offset(x + s * 2, y + s * 3 - flap), Size(s * 4, s))
}

private fun DrawScope.drawPixelPreChordate(x: Float, y: Float, s: Float, step: Boolean) {
    val chordColor = Color(0xFF48CAE4)
    val myomere = Color(0xFF0077B6)
    val wave = if (step) s else -s
    drawRect(chordColor, Offset(x, y), Size(s * 7, s * 2))
    drawRect(myomere, Offset(x + s, y + s * 0.5f), Size(s * 5, s))
    drawRect(chordColor, Offset(x + s * 7, y + wave), Size(s * 2, s * 2))
}

private fun DrawScope.drawPixelCyanobion(x: Float, y: Float, s: Float, step: Boolean) {
    val cyanCol = if (step) PixelCyan else PixelEmerald
    drawRect(cyanCol, Offset(x, y), Size(s * 3, s * 3))
    drawRect(Color.White.copy(alpha = 0.6f), Offset(x + s, y + s), Size(s, s))
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
