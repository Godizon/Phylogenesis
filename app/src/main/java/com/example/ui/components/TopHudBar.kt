package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SimulationState
import com.example.ui.theme.*

/**
 * Slim, plain-language HUD.
 * Row 1: what time period is it + play/speed/sound/settings.
 * Row 2: three tappable stat tiles (Life, Species, Shield). Tap any of them to learn what it means.
 */
@Composable
fun TopHudBar(
    state: SimulationState,
    onTogglePlayPause: () -> Unit,
    onSetSpeed: (Float) -> Unit,
    onToggleAudio: () -> Unit,
    onOpenSettings: () -> Unit,
    onShowInfo: (InfoEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    val epoch = state.currentEpoch
    val speed = state.timeSpeedMaPerSec
    val speedLabel = when {
        speed >= 50f -> "100x"
        speed >= 5f -> "10x"
        else -> "1x"
    }
    val nextSpeed = when {
        speed >= 50f -> 1f
        speed >= 5f -> 100f
        else -> 10f
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("top_hud_bar"),
        color = PixelPanel.copy(alpha = 0.94f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Era + how long ago
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onShowInfo(Glossary.era) }
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(epoch.emoji, fontSize = 26.sp)
                    Column {
                        Text(
                            epoch.friendlyName,
                            color = PixelGold,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1
                        )
                        Text(
                            formatYearsAgo(state.currentTimeGa),
                            color = MutedText,
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HudButton(
                        label = if (state.isPaused) "▶" else "⏸",
                        accent = if (state.isPaused) PixelGold else PixelCyan,
                        onClick = onTogglePlayPause,
                        tag = "btn_play_pause"
                    )
                    HudButton(
                        label = "⏩ $speedLabel",
                        accent = PixelCyan,
                        width = 66.dp,
                        onClick = { onSetSpeed(nextSpeed) },
                        tag = "btn_speed_cycle"
                    )
                    HudButton(
                        label = if (state.audioEnabled) "🔊" else "🔇",
                        accent = PixelBorderLight,
                        onClick = onToggleAudio,
                        tag = "toggle_audio_button"
                    )
                    HudButton(
                        label = "⚙️",
                        accent = PixelBorderLight,
                        onClick = onOpenSettings,
                        tag = "btn_open_settings"
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StatChip(
                    emoji = "🌱", label = "Life", value = state.formattedBiomass,
                    accent = PixelEmerald,
                    onClick = { onShowInfo(Glossary.life) },
                    modifier = Modifier.weight(1f)
                )
                StatChip(
                    emoji = "🧬", label = "Species", value = "${state.globalTaxonDiversityCount}",
                    accent = PixelCyan,
                    onClick = { onShowInfo(Glossary.species) },
                    modifier = Modifier.weight(1f)
                )
                StatChip(
                    emoji = "🛡️", label = "Shield",
                    value = if (state.planetary.isMagnetosphereShielded) "Strong" else "Weak ⚠",
                    accent = if (state.planetary.isMagnetosphereShielded) PixelGold else PixelCrimson,
                    onClick = { onShowInfo(Glossary.shield) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun HudButton(
    label: String,
    accent: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    tag: String,
    width: Dp = 40.dp
) {
    Box(
        modifier = Modifier
            .size(width = width, height = 40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(PixelVoid)
            .border(1.5.dp, accent, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = PaleText, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}
