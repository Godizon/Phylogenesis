package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CataclysmType
import com.example.model.GeologicalEpoch
import com.example.ui.theme.*

/**
 * WorldBox-style power bar. Big, labelled, one-tap buttons.
 * - Tap a disaster button: it fires immediately (with a bounce) and a toast explains what it does.
 * - Long-press any button: shows its explanation without firing it.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun PowersDock(
    onTriggerCataclysm: (CataclysmType) -> Unit,
    onOpenGeneLab: () -> Unit,
    onOpenTree: () -> Unit,
    onOpenTimeTravel: () -> Unit,
    onOpenPlanetControls: () -> Unit,
    onOpenCelestialModal: () -> Unit = {},
    onShowInfo: (InfoEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("timeline_and_tool_dock"),
        color = PixelPanel.copy(alpha = 0.95f)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Create / explore
                PowerButton("🌌", "Worlds", PixelGold, onOpenCelestialModal) {
                    onShowInfo(InfoEntry("🌌", "Celestial Worlds", "Switch between authentic astrophysical scenarios: Earth, Mars, Europa, Titan, and Epsilon Eridani."))
                }
                PowerButton("🧪", "Create Life", PixelCyan, onOpenGeneLab) { onShowInfo(Glossary.geneLab) }
                PowerButton("🌳", "Tree of Life", PixelEmerald, onOpenTree) { onShowInfo(Glossary.tree) }
                PowerButton("🕰️", "Time Travel", PixelGold, onOpenTimeTravel) { onShowInfo(Glossary.timeTravel) }

                // Divider
                Box(Modifier.width(2.dp).height(58.dp).background(PixelBorderLight))

                // Disasters, straight from the game's cataclysm list
                CataclysmType.values().forEach { type ->
                    val f = type.friendly
                    PowerButton(
                        emoji = f.emoji,
                        label = f.name,
                        accent = Color(type.colorHex),
                        onClick = { onTriggerCataclysm(type) },
                        onLongClick = {
                            onShowInfo(InfoEntry(f.emoji, f.name, f.blurb))
                        }
                    )
                }

                Box(Modifier.width(2.dp).height(58.dp).background(PixelBorderLight))

                PowerButton("🪐", "Planet", PixelPurple, onOpenPlanetControls) { onShowInfo(Glossary.moreTools) }
            }
            Text(
                "Tap to use  •  Hold a button to learn what it does",
                color = SubduedText,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun PowerButton(
    emoji: String,
    label: String,
    accent: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 1.18f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        finishedListener = { pressed = false },
        label = "power_bounce"
    )
    Column(
        modifier = Modifier
            .scale(scale)
            .width(68.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(accent.copy(alpha = 0.18f))
            .border(2.dp, accent, RoundedCornerShape(14.dp))
            .combinedClickable(
                onClick = {
                    pressed = true
                    onClick()
                },
                onLongClick = onLongClick
            )
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(emoji, fontSize = 26.sp)
        Text(
            label,
            color = PaleText,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

/** Pulsing "what do I do?" bubble. Disappears after the first creature tap or a few seconds. */
@Composable
fun CoachHint(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "coach_pulse")
    val a by t.animateFloat(
        initialValue = 0.65f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "coach_alpha"
    )
    Text(
        "👆 Tap a creature to inspect it",
        color = OnPrimary,
        fontSize = 14.sp,
        fontWeight = FontWeight.ExtraBold,
        modifier = modifier
            .scale(0.97f + 0.03f * a)
            .clip(RoundedCornerShape(20.dp))
            .background(PixelGold.copy(alpha = a))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

/** Jump to a chapter of Earth's history. Plain names, plain blurbs. */
@Composable
fun TimeTravelDialog(
    current: GeologicalEpoch,
    onPick: (GeologicalEpoch) -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(PixelPanel)
                .border(2.dp, PixelGold, RoundedCornerShape(18.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("🕰️ Time Travel", color = PixelGold, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            Text("Jump to a moment in history", color = MutedText, fontSize = 13.sp)
            GeologicalEpoch.values().forEach { e ->
                val isNow = e == current
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isNow) PixelPanelLight else PixelVoid)
                        .border(1.5.dp, if (isNow) PixelGold else PixelBorderLight, RoundedCornerShape(12.dp))
                        .clickable { onPick(e); onDismiss() }
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(e.emoji, fontSize = 28.sp)
                    Column(Modifier.weight(1f)) {
                        Text(e.friendlyName, color = PaleText, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                        Text(e.friendlyBlurb, color = MutedText, fontSize = 12.sp, lineHeight = 16.sp)
                    }
                }
            }
        }
    }
}
