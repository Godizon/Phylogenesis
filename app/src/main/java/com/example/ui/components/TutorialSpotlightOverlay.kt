package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SpotlightTarget
import com.example.model.TutorialState
import com.example.model.TutorialStep
import com.example.ui.theme.*

private data class SpotlightBounds(
    val left: Dp,
    val top: Dp,
    val right: Dp,
    val bottom: Dp,
    val label: String,
    val indicatorBelow: Boolean,
    val cardAlignment: Alignment,
    val cardPaddingTop: Dp = 0.dp,
    val cardPaddingBottom: Dp = 0.dp
)

@Composable
fun TutorialSpotlightOverlay(
    tutorialState: TutorialState,
    onNextStep: () -> Unit,
    onPrevStep: () -> Unit,
    onSkipTutorial: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!tutorialState.isActive) return

    val infiniteTransition = rememberInfiniteTransition(label = "spotlight_pulse")
    val pulseBorder by infiniteTransition.animateFloat(
        initialValue = 2.5f,
        targetValue = 5.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "spotlight_pulse"
    )

    val step = tutorialState.currentStep

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag("tutorial_spotlight_overlay")
    ) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight

        // Calculate dynamic cutout coordinates and dialog position based on current step
        val bounds = when (step.spotlightTarget) {
            SpotlightTarget.TOP_HUD -> SpotlightBounds(
                left = 4.dp,
                top = 2.dp,
                right = screenWidth - 4.dp,
                bottom = 98.dp,
                label = "GEOLOGICAL DEEP TIME & CLIMATE TELEMETRY",
                indicatorBelow = true,
                cardAlignment = Alignment.BottomCenter,
                cardPaddingBottom = 34.dp
            )
            SpotlightTarget.ZOOM_DOCK -> SpotlightBounds(
                left = 4.dp,
                top = screenHeight - 110.dp,
                right = screenWidth - 4.dp,
                bottom = screenHeight - 4.dp,
                label = "POWERS OF TEN COSMIC ZOOM DOCK",
                indicatorBelow = false,
                cardAlignment = Alignment.TopCenter,
                cardPaddingTop = 106.dp
            )
            SpotlightTarget.GOD_TOOLS_BTN -> SpotlightBounds(
                left = (screenWidth - 152.dp).coerceAtLeast(8.dp),
                top = screenHeight - 104.dp,
                right = screenWidth - 8.dp,
                bottom = screenHeight - 62.dp,
                label = "DEMIURGE GOD-TOOLS & MASS EXTINCTIONS",
                indicatorBelow = false,
                cardAlignment = Alignment.TopCenter,
                cardPaddingTop = 106.dp
            )
            SpotlightTarget.SPECIMEN_DRAWER -> {
                val specW = minOf(screenWidth - 32.dp, 330.dp)
                val specH = 220.dp
                val left = (screenWidth - specW) / 2
                val top = (screenHeight * 0.22f)
                SpotlightBounds(
                    left = left,
                    top = top,
                    right = left + specW,
                    bottom = top + specH,
                    label = "LIVING SPECIMEN & ANATOMICAL OBSERVATION",
                    indicatorBelow = true,
                    cardAlignment = Alignment.BottomCenter,
                    cardPaddingBottom = 22.dp
                )
            }
            SpotlightTarget.CLADOGRAM_BTN -> SpotlightBounds(
                left = (screenWidth - 138.dp).coerceAtLeast(8.dp),
                top = 6.dp,
                right = (screenWidth - 86.dp).coerceAtLeast(50.dp),
                bottom = 54.dp,
                label = "TREE OF LIFE (PHYLOGENETIC CLADOGRAM)",
                indicatorBelow = true,
                cardAlignment = Alignment.BottomCenter,
                cardPaddingBottom = 34.dp
            )
        }

        // Canvas drawing the transparent cutout hole in the scrim + glowing gold border & brackets
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    // Prevent accidental clicks passing through dimmed area
                }
        ) {
            val leftPx = bounds.left.toPx()
            val topPx = bounds.top.toPx()
            val rightPx = bounds.right.toPx()
            val bottomPx = bounds.bottom.toPx()
            val widthPx = rightPx - leftPx
            val heightPx = bottomPx - topPx

            // 1. Cutout path with EvenOdd fill: leaves the spotlight window 100% transparent and clear!
            val scrimPath = Path().apply {
                fillType = PathFillType.EvenOdd
                // Outer bounds
                addRect(Rect(0f, 0f, size.width, size.height))
                // Inner cutout window
                addRoundRect(
                    RoundRect(
                        rect = Rect(leftPx, topPx, rightPx, bottomPx),
                        cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                    )
                )
            }
            drawPath(path = scrimPath, color = Color.Black.copy(alpha = 0.76f))

            // 2. Soft outer aura glow
            drawRoundRect(
                color = PixelGold.copy(alpha = 0.35f),
                topLeft = Offset(leftPx - 2.dp.toPx(), topPx - 2.dp.toPx()),
                size = Size(widthPx + 4.dp.toPx(), heightPx + 4.dp.toPx()),
                cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx()),
                style = Stroke(width = (pulseBorder + 2f).dp.toPx())
            )

            // 3. Sharp pulsing primary gold border
            drawRoundRect(
                color = PixelGold,
                topLeft = Offset(leftPx, topPx),
                size = Size(widthPx, heightPx),
                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                style = Stroke(width = pulseBorder.dp.toPx())
            )

            // 4. 16-bit retro corner brackets
            val bracketLen = 14.dp.toPx()
            val bracketStroke = 3.5.dp.toPx()

            // Top-Left
            drawLine(PixelGold, Offset(leftPx, topPx), Offset(leftPx + bracketLen, topPx), bracketStroke)
            drawLine(PixelGold, Offset(leftPx, topPx), Offset(leftPx, topPx + bracketLen), bracketStroke)

            // Top-Right
            drawLine(PixelGold, Offset(rightPx, topPx), Offset(rightPx - bracketLen, topPx), bracketStroke)
            drawLine(PixelGold, Offset(rightPx, topPx), Offset(rightPx, topPx + bracketLen), bracketStroke)

            // Bottom-Left
            drawLine(PixelGold, Offset(leftPx, bottomPx), Offset(leftPx + bracketLen, bottomPx), bracketStroke)
            drawLine(PixelGold, Offset(leftPx, bottomPx), Offset(leftPx, bottomPx - bracketLen), bracketStroke)

            // Bottom-Right
            drawLine(PixelGold, Offset(rightPx, bottomPx), Offset(rightPx - bracketLen, bottomPx), bracketStroke)
            drawLine(PixelGold, Offset(rightPx, bottomPx), Offset(rightPx, bottomPx - bracketLen), bracketStroke)
        }

        // Tappable spotlight window hotspot (tapping the highlighted UI element advances tutorial)
        Box(
            modifier = Modifier
                .offset(x = bounds.left, y = bounds.top)
                .size(width = bounds.right - bounds.left, height = bounds.bottom - bounds.top)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    onNextStep()
                }
        )

        // Retro Floating Target Indicator Badge
        val indicatorY = if (bounds.indicatorBelow) bounds.bottom + 8.dp else bounds.top - 32.dp
        Box(
            modifier = Modifier
                .offset(y = indicatorY)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(PixelVoid)
                    .border(1.5.dp, PixelGold, RoundedCornerShape(4.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (bounds.indicatorBelow) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = PixelGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = bounds.label,
                        color = PixelGold,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )
                    Icon(
                        imageVector = if (bounds.indicatorBelow) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = PixelGold,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Floating Instructional Dialogue Card (Safely positioned away from spotlight cutout)
        Card(
            modifier = Modifier
                .align(bounds.cardAlignment)
                .padding(
                    top = bounds.cardPaddingTop,
                    bottom = bounds.cardPaddingBottom,
                    start = 16.dp,
                    end = 16.dp
                )
                .fillMaxWidth(0.94f)
                .testTag("tutorial_dialog_card"),
            colors = CardDefaults.cardColors(containerColor = PixelPanel),
            shape = RoundedCornerShape(4.dp),
            border = BorderStroke(3.dp, PixelGold),
            elevation = CardDefaults.cardElevation(14.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header: Step index badge & Skip button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(PixelGold)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "STEP ${step.stepIndex} OF 5",
                            color = PixelVoid,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Text(
                        text = "SKIP TUTORIAL",
                        color = MutedText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .clickable { onSkipTutorial() }
                            .padding(8.dp)
                            .testTag("btn_skip_tutorial")
                    )
                }

                // Step Title
                Text(
                    text = step.title,
                    color = PixelCyan,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace
                )

                // Step Description
                Text(
                    text = step.description,
                    color = PaleText,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    fontFamily = FontFamily.Monospace
                )

                Divider(color = PixelBorderLight, thickness = 1.dp)

                // Bottom Navigation Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!step.isFirst) {
                        Button(
                            onClick = onPrevStep,
                            colors = ButtonDefaults.buttonColors(containerColor = PixelPanelLight),
                            shape = RoundedCornerShape(3.dp),
                            border = BorderStroke(1.dp, PixelBorderLight),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("btn_tutorial_prev")
                        ) {
                            Text(
                                text = "◄ PREVIOUS",
                                fontSize = 11.sp,
                                color = PaleText,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Spacer(Modifier.width(1.dp))
                    }

                    Button(
                        onClick = onNextStep,
                        colors = ButtonDefaults.buttonColors(containerColor = PixelCyan),
                        shape = RoundedCornerShape(3.dp),
                        border = BorderStroke(1.dp, PixelGold),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("btn_tutorial_next")
                    ) {
                        Text(
                            text = if (step.isLast) "FINISH & PLAY ►" else "NEXT ►",
                            fontSize = 11.sp,
                            color = OnPrimary,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}
