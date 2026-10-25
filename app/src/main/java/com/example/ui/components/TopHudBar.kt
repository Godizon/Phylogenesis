package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PlayerInterventionMode
import com.example.model.SimulationState
import com.example.ui.theme.*

@Composable
fun TopHudBar(
    state: SimulationState,
    onTogglePlayPause: () -> Unit,
    onSetSpeed: (Float) -> Unit,
    onToggleMode: () -> Unit,
    onToggleAudio: () -> Unit,
    onOpenCladogram: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("top_hud_bar"),
        color = PixelVoid,
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(width = 2.dp, color = PixelBorderDark)
                .background(PixelPanel)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Epoch and Deep Time (Pixel Badge)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(PixelVoid)
                            .border(2.dp, PixelCyan, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = state.currentEpoch.title.uppercase(),
                            color = PixelCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Column {
                        Text(
                            text = state.formattedTime,
                            color = PixelGold,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = state.currentEpoch.rangeStr,
                            color = MutedText,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Center: Retro Pixel Speed Controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(PixelVoid)
                        .border(2.dp, PixelBorderLight, RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    IconButton(
                        onClick = onTogglePlayPause,
                        modifier = Modifier
                            .size(30.dp)
                            .testTag("btn_play_pause")
                    ) {
                        Icon(
                            imageVector = if (state.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = if (state.isPaused) "Resume" else "Pause",
                            tint = if (state.isPaused) PixelGold else PixelCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    PixelSpeedPill(
                        label = "1x",
                        isSelected = !state.isPaused && state.timeSpeedMaPerSec == 1.0f,
                        onClick = { onSetSpeed(1.0f) },
                        testTag = "speed_1x"
                    )

                    PixelSpeedPill(
                        label = "10x",
                        isSelected = !state.isPaused && state.timeSpeedMaPerSec == 10.0f,
                        onClick = { onSetSpeed(10.0f) },
                        testTag = "speed_10x"
                    )

                    PixelSpeedPill(
                        label = "100x",
                        isSelected = !state.isPaused && state.timeSpeedMaPerSec == 100.0f,
                        onClick = { onSetSpeed(100.0f) },
                        testTag = "speed_100x"
                    )
                }

                // Right: Mode and Action Buttons (WorldBox square style)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Mode Toggle
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (state.mode == PlayerInterventionMode.ARCHITECT)
                                    PixelPurple.copy(alpha = 0.35f)
                                else
                                    PixelEmerald.copy(alpha = 0.25f)
                            )
                            .border(
                                2.dp,
                                if (state.mode == PlayerInterventionMode.ARCHITECT) PixelPurple else PixelEmerald,
                                RoundedCornerShape(4.dp)
                            )
                            .clickable { onToggleMode() }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                            .testTag("toggle_game_mode")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (state.mode == PlayerInterventionMode.ARCHITECT)
                                    Icons.Default.Bolt
                                else
                                    Icons.Default.Visibility,
                                contentDescription = null,
                                tint = if (state.mode == PlayerInterventionMode.ARCHITECT) PixelPurple else PixelEmerald,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (state.mode == PlayerInterventionMode.ARCHITECT) "ARCHITECT" else "NATURALIST",
                                color = PaleText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Tree of Life / Cladogram Button
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(PixelPanelLight)
                            .border(2.dp, PixelBorderLight, RoundedCornerShape(4.dp))
                            .clickable { onOpenCladogram() }
                            .testTag("open_cladogram_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountTree,
                            contentDescription = "Tree of Life Cladogram",
                            tint = PixelCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Audio Toggle Button
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(PixelPanelLight)
                            .border(2.dp, PixelBorderLight, RoundedCornerShape(4.dp))
                            .clickable { onToggleAudio() }
                            .testTag("toggle_audio_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (state.audioEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = if (state.audioEnabled) "Mute Audio" else "Unmute Audio",
                            tint = if (state.audioEnabled) PixelCyan else MutedText,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Settings Button
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(PixelPanelLight)
                            .border(2.dp, PixelBorderLight, RoundedCornerShape(4.dp))
                            .clickable { onOpenSettings() }
                            .testTag("btn_open_settings"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Game Settings",
                            tint = PixelGold,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Sub-row: Telemetry strip with pixel badges
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "BIOMASS: ${state.formattedBiomass}",
                        color = PixelEmerald,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "TAXA: ${state.globalTaxonDiversityCount} CLADES",
                        color = PixelCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "CORE: ${state.planetary.magnetosphereMicroTesla.toInt()} μT",
                        color = if (state.planetary.isMagnetosphereShielded) PixelGold else PixelCrimson,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                if (state.planetary.isSnowballEarth) {
                    Text(
                        text = "❄ SNOWBALL RUNAWAY",
                        color = PixelSnow,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun PixelSpeedPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .background(if (isSelected) PixelCyan.copy(alpha = 0.35f) else Color.Transparent)
            .border(
                1.dp,
                if (isSelected) PixelCyan else Color.Transparent,
                RoundedCornerShape(3.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 3.dp)
            .testTag(testTag)
    ) {
        Text(
            text = label,
            color = if (isSelected) PixelCyan else MutedText,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal,
            fontFamily = FontFamily.Monospace
        )
    }
}
