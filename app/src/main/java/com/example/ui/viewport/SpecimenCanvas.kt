package com.example.ui.viewport

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.model.LimbType
import com.example.model.OrganismPhenotype
import com.example.model.SegmentType
import com.example.ui.theme.*
import kotlin.math.*

/**
 * WorldBox 16-bit retro pixelated anatomical X-Ray canvas.
 */
@Composable
fun SpecimenCanvas(
    specimen: OrganismPhenotype,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pixel_specimen_anim")

    // Metachronal gait phase
    val gaitPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pixel_gait"
    )

    // Pulsing 8-bit heart beat
    val heartBeat by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pixel_heart"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f

        val p = 6f // Chunky pixel unit size
        val segments = specimen.segments
        val segCount = segments.size.coerceAtLeast(1)
        val bodySpan = w * 0.70f
        val segSpacing = bodySpan / (segCount + 1)
        val startX = cx - bodySpan / 2f

        // 1. Draw Stepped Pixel Appendages / Legs
        for (i in segments.indices) {
            val seg = segments[i]
            val segX = (startX + (i + 1) * segSpacing)
            val qx = (segX / p).toInt() * p
            val qy = (cy / p).toInt() * p
            val legSwingSteps = (sin(gaitPhase + i * 0.5f) * 3).toInt()

            if (seg.limbPairCount > 0) {
                when (seg.limbType) {
                    LimbType.JOINTED_WALKER -> {
                        val legColor = PixelCyan
                        // Upper leg: stepped pixels going up
                        drawRect(legColor, Offset(qx, qy - 4 * p), Size(p, 2 * p))
                        drawRect(legColor, Offset(qx + legSwingSteps * p, qy - 6 * p), Size(p, 2 * p))
                        drawRect(Color(0xFF0096C7), Offset(qx + (legSwingSteps + 1) * p, qy - 8 * p), Size(p, 2 * p))

                        // Lower leg: stepped pixels going down
                        drawRect(legColor, Offset(qx, qy + 4 * p), Size(p, 2 * p))
                        drawRect(legColor, Offset(qx - legSwingSteps * p, qy + 6 * p), Size(p, 2 * p))
                        drawRect(Color(0xFF0096C7), Offset(qx - (legSwingSteps + 1) * p, qy + 8 * p), Size(p, 2 * p))
                    }
                    LimbType.HYDROFOIL_FIN -> {
                        val finColor = PixelEmerald
                        val flapY = if (legSwingSteps > 0) p else -p
                        drawRect(finColor, Offset(qx - 2 * p, qy - 6 * p + flapY), Size(3 * p, 2 * p))
                        drawRect(finColor, Offset(qx - 2 * p, qy + 6 * p - flapY), Size(3 * p, 2 * p))
                    }
                    else -> {}
                }
            }

            // Gills / Spiracles breathing pixel blocks
            if (seg.hasBranchialGill || seg.hasSpiracle) {
                val breathGlow = if ((gaitPhase * 2 + i).toInt() % 2 == 0) PixelGold else Color(0xFFB58308)
                drawRect(breathGlow, Offset(qx, qy - 3 * p), Size(p, p))
            }
        }

        // 2. Draw Pixelated Metamere Carapace Plates
        for (i in segments.indices) {
            val seg = segments[i]
            val segX = startX + (i + 1) * segSpacing
            val qx = (segX / p).toInt() * p
            val qy = (cy / p).toInt() * p
            val halfWidthP = max(2, (seg.widthFraction * 3).toInt())
            val halfHeightP = max(2, (seg.widthFraction * 3).toInt())

            val plateColor = when (seg.type) {
                SegmentType.CEPHALIC -> PixelCyan
                SegmentType.THORACIC -> Color(0xFF2B3A55)
                SegmentType.ABDOMINAL -> Color(0xFF1E2A3E)
                SegmentType.CAUDAL -> Color(0xFF141D2C)
            }

            // Outer plate pixel block
            drawRect(
                color = plateColor,
                topLeft = Offset(qx - halfWidthP * p, qy - halfHeightP * p),
                size = Size(halfWidthP * 2 * p, halfHeightP * 2 * p)
            )

            // Pixel border outline
            drawPixelBorder(
                x = qx - halfWidthP * p,
                y = qy - halfHeightP * p,
                w = halfWidthP * 2 * p,
                h = halfHeightP * 2 * p,
                color = PixelBorderLight,
                p = p
            )

            // Cephalic eyes & sensory antenna
            if (seg.type == SegmentType.CEPHALIC) {
                // 8-bit golden compound eye
                drawRect(PixelGold, Offset(qx + halfWidthP * p - p, qy - p), Size(p, p))
                // Stepped pixel antenna
                drawRect(PixelCyan, Offset(qx + halfWidthP * p, qy - 2 * p), Size(p, p))
                drawRect(PixelCyan, Offset(qx + (halfWidthP + 1) * p, qy - 4 * p), Size(p, 2 * p))
            }
        }

        // 3. Draw 8-Bit Beating Pixel Heart
        val heartX = (startX + segSpacing * 1.6f / p).toInt() * p
        val heartY = (cy / p).toInt() * p
        draw8BitPixelHeart(heartX, heartY, p, heartBeat > 0.5f)

        // Dorsal vascular conduit pixel line
        val lineStartX = (startX + segSpacing * 0.8f / p).toInt() * p
        val lineEndX = (startX + segSpacing * (segCount - 0.2f) / p).toInt() * p
        drawRect(
            color = PixelCrimson.copy(alpha = 0.8f),
            topLeft = Offset(lineStartX.toFloat(), cy - p * 0.5f),
            size = Size((lineEndX - lineStartX).toFloat(), p)
        )
    }
}

private fun DrawScope.drawPixelBorder(x: Float, y: Float, w: Float, h: Float, color: Color, p: Float) {
    drawRect(color, Offset(x, y), Size(w, p)) // top
    drawRect(color, Offset(x, y + h - p), Size(w, p)) // bottom
    drawRect(color, Offset(x, y), Size(p, h)) // left
    drawRect(color, Offset(x + w - p, y), Size(p, h)) // right
}

/**
 * 8-bit heart sprite drawn using discrete pixel blocks.
 */
private fun DrawScope.draw8BitPixelHeart(x: Float, y: Float, p: Float, beat: Boolean) {
    val heartColor = if (beat) PixelCrimson else Color(0xFF9E2A2B)
    val s = if (beat) 1.2f else 1.0f

    drawRect(heartColor, Offset(x - p * s, y - p * s), Size(p * s, p * s))
    drawRect(heartColor, Offset(x + p * s, y - p * s), Size(p * s, p * s))
    drawRect(heartColor, Offset(x - 2 * p * s, y), Size(3 * p * s, p * s))
    drawRect(heartColor, Offset(x - p * s, y + p * s), Size(2 * p * s, p * s))
    drawRect(heartColor, Offset(x, y + 2 * p * s), Size(p * s, p * s))
}
