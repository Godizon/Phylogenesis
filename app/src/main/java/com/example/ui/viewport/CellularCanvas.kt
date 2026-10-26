package com.example.ui.viewport

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.*
import kotlin.math.*

/**
 * WorldBox-style 16-bit retro pixelated cellular viewport.
 */
@Composable
fun CellularCanvas(
    isPaused: Boolean = false,
    modifier: Modifier = Modifier
) {
    var animTime by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isPaused) {
        var lastNano = System.nanoTime()
        while (true) {
            withFrameNanos { nowNano ->
                val dt = ((nowNano - lastNano) / 1_000_000_000f).coerceIn(0f, 0.05f)
                lastNano = nowNano
                if (!isPaused) {
                    animTime += dt
                }
            }
        }
    }

    val waveStep = (animTime * 4.4f) % 8f
    val rotorStep = ((animTime * 8f) % 4f).toInt()
    val atpSpark = (sin(animTime * 10f) * 0.5f + 0.5f)

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f

        val p = 8f // Pixel unit
        val r = (min(w, h) * 0.35f / p).toInt() * p

        // 1. Draw Extracellular Nutrient Fluid (Deep Blue pixel tiles)
        val cols = ceil(w / p).toInt()
        val rows = ceil(h / p).toInt()
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val px = col * p
                val py = row * p
                val d = hypot(px - cx, py - cy)
                if (d < r) {
                    // Cytoplasm pixel interior
                    val cytoColor = if ((col + row) % 2 == 0) Color(0xFF0F3B57) else Color(0xFF0A2B40)
                    drawRect(cytoColor, Offset(px, py), Size(p, p))
                } else {
                    // Extracellular fluid
                    val soupColor = if ((col + row) % 3 == 0) PixelDeepOcean else Color(0xFF081A30)
                    drawRect(soupColor, Offset(px, py), Size(p, p))
                }
            }
        }

        // 2. Draw Phospholipid Bilayer Membrane (Chunky pixel ring)
        val steps = 40
        for (i in 0 until steps) {
            val angle = i * (2 * Math.PI / steps)
            val wobble = ((sin(angle * 4 + waveStep) * 2).toInt()) * p
            val outerR = r + wobble
            val innerR = r - 2 * p + wobble

            val ox = (cx + cos(angle) * outerR) / p
            val oy = (cy + sin(angle) * outerR) / p
            val ix = (cx + cos(angle) * innerR) / p
            val iy = (cy + sin(angle) * innerR) / p

            drawRect(PixelCyan, Offset(ox.toInt() * p, oy.toInt() * p), Size(p, p))
            drawRect(Color(0xFF0096C7), Offset(ix.toInt() * p, iy.toInt() * p), Size(p, p))
        }

        // 3. Draw 8-Bit Chromatin Nucleus
        val nR = (r * 0.4f / p).toInt() * p
        for (dy in -nR.toInt()..nR.toInt() step p.toInt()) {
            for (dx in -nR.toInt()..nR.toInt() step p.toInt()) {
                if (hypot(dx.toFloat(), dy.toFloat()) < nR) {
                    val nColor = if (((dx / p).toInt() + (dy / p).toInt()) % 2 == 0) PixelPurple else Color(0xFF7209B7)
                    drawRect(nColor, Offset(cx + dx, cy + dy), Size(p, p))
                }
            }
        }
        // Golden DNA codons inside nucleus
        drawRect(PixelGold, Offset(cx - 2 * p, cy - p), Size(4 * p, p))
        drawRect(PixelGold, Offset(cx - 3 * p, cy + p), Size(5 * p, p))

        // 4. Draw Pixelated Mitochondria with Cristae
        for (m in 0 until 3) {
            val mAngle = m * (2 * Math.PI / 3) + 0.6
            val mx = (cx + cos(mAngle) * (r * 0.65f) / p).toInt() * p
            val my = (cy + sin(mAngle) * (r * 0.65f) / p).toInt() * p

            // Green capsule pixels
            drawRect(PixelEmerald, Offset(mx.toFloat() - 2 * p, my.toFloat() - p), Size(5 * p, 3 * p))
            // Inner cristae slits
            drawRect(Color(0xFFB7E4C7), Offset(mx.toFloat() - p, my.toFloat()), Size(p, p))
            drawRect(Color(0xFFB7E4C7), Offset(mx.toFloat() + p, my.toFloat()), Size(p, p))
        }

        // 5. Draw 8-Bit Flagellar Motor with Stepped Rotor
        val motorX = (cx + r + p * 2) / p
        val motorY = cy / p
        val qmx = motorX.toInt() * p
        val qmy = motorY.toInt() * p

        // Stator base
        drawRect(PixelGold, Offset(qmx, qmy - p), Size(2 * p, 3 * p))

        // Rotating rotor spokes
        when (rotorStep % 4) {
            0 -> drawRect(PixelCyan, Offset(qmx + 2 * p, qmy), Size(2 * p, p))
            1 -> drawRect(PixelCyan, Offset(qmx + 2 * p, qmy - p), Size(2 * p, 2 * p))
            2 -> drawRect(PixelCyan, Offset(qmx + 2 * p, qmy + p), Size(2 * p, p))
            else -> drawRect(PixelCyan, Offset(qmx + p, qmy), Size(3 * p, p))
        }

        // Stepped pixel flagellum tail
        for (seg in 0 until 6) {
            val fx = qmx + 4 * p + seg * 2 * p
            val fy = qmy + ((sin(seg * 0.8f + waveStep) * 2).toInt()) * p
            drawRect(PixelCyan, Offset(fx, fy), Size(2 * p, p))
        }

        // 6. Glowing ATP Synthase Energy Sparks
        val atpColor = if (atpSpark > 0.5f) PixelGold else PixelLavaGlow
        drawRect(atpColor, Offset(cx - r * 0.45f, cy + r * 0.45f), Size(2 * p, 2 * p))
        drawRect(Color.White, Offset(cx - r * 0.45f + p * 0.5f, cy + r * 0.45f + p * 0.5f), Size(p, p))
    }
}
