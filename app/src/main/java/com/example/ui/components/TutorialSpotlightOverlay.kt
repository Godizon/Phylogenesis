package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SpotlightTarget
import com.example.model.TutorialState
import com.example.model.TutorialStep
import com.example.ui.theme.*
import kotlin.math.roundToInt

/**
 * "How to play" walkthrough. A friendly card with a bouncing arrow pointing at the part of the screen
 * being explained. (The old version hard-coded pixel positions that broke whenever the layout changed.)
 */
@Composable
fun TutorialSpotlightOverlay(
    tutorialState: TutorialState,
    onNextStep: () -> Unit,
    onPrevStep: () -> Unit,
    onSkipTutorial: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!tutorialState.isActive) return
    val step = tutorialState.currentStep

    val t = rememberInfiniteTransition(label = "tutorial_arrow")
    val bounce by t.animateFloat(
        initialValue = 0f, targetValue = 14f,
        animationSpec = infiniteRepeatable(tween(500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "arrow_bounce"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            .testTag("tutorial_spotlight_overlay")
    ) {
        // Bouncing arrow toward the thing being explained
        when (step.spotlightTarget) {
            SpotlightTarget.TOP_HUD -> Arrow("⬆️", Modifier.align(Alignment.TopCenter).padding(top = 112.dp).offset { IntOffset(0, -bounce.roundToInt()) })
            SpotlightTarget.ZOOM_DOCK -> Arrow("➡️", Modifier.align(Alignment.TopEnd).padding(top = 130.dp, end = 62.dp).offset { IntOffset(bounce.roundToInt(), 0) })
            SpotlightTarget.GOD_TOOLS_BTN, SpotlightTarget.CLADOGRAM_BTN ->
                Arrow("⬇️", Modifier.align(Alignment.BottomCenter).padding(bottom = 118.dp).offset { IntOffset(0, bounce.roundToInt()) })
            SpotlightTarget.SPECIMEN_DRAWER -> Unit
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(PixelPanel)
                .border(2.dp, PixelGold, RoundedCornerShape(20.dp))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(step.emoji, fontSize = 44.sp)
            Text(step.title, color = PixelGold, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
            Text(step.description, color = PaleText, fontSize = 16.sp, lineHeight = 22.sp, textAlign = TextAlign.Center)

            // Progress dots
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TutorialStep.values().forEach {
                    Box(
                        Modifier
                            .size(if (it == step) 10.dp else 8.dp)
                            .clip(CircleShape)
                            .background(if (it == step) PixelGold else PixelBorderLight)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Skip",
                    color = MutedText,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onSkipTutorial() }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .testTag("tutorial_skip")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!step.isFirst) {
                        Text(
                            "Back",
                            color = PaleText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(PixelPanelLight)
                                .clickable { onPrevStep() }
                                .padding(horizontal = 18.dp, vertical = 10.dp)
                                .testTag("tutorial_back")
                        )
                    }
                    Text(
                        if (step.isLast) "Let's play! 🎉" else "Next",
                        color = OnPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(PixelCyan)
                            .clickable { onNextStep() }
                            .padding(horizontal = 18.dp, vertical = 10.dp)
                            .testTag("tutorial_next")
                    )
                }
            }
        }
    }
}

@Composable
private fun Arrow(symbol: String, modifier: Modifier) {
    Text(symbol, fontSize = 40.sp, modifier = modifier)
}
