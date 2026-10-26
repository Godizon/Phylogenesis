package com.example.ui.viewport

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
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
import com.example.model.CosmicScale
import com.example.model.LivingOrganism
import com.example.model.OrganismPhenotype
import com.example.model.SimulationState
import com.example.ui.components.friendly
import com.example.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun SimulationViewport(
    state: SimulationState,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onSelectOrganism: (LivingOrganism) -> Unit,
    onClearSelectedOrganism: () -> Unit,
    onInspectSpecimenModal: (OrganismPhenotype) -> Unit,
    onMutateSpecimen: () -> Unit,
    onOpenGeneLab: () -> Unit,
    onTogglePlayPause: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PixelVoid)
            .testTag("simulation_viewport")
    ) {
        // Main Pixel Canvas Routed to Active Cosmic Zoom Scale
        AnimatedContent(
            targetState = state.currentScale,
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            },
            label = "viewport_pixel_scale_transition"
        ) { scale ->
            when (scale) {
                CosmicScale.COSMIC, CosmicScale.ECOSYSTEM -> {
                    // WorldBox-style pixel sandbox world canvas with revamped living organisms
                    PixelWorldCanvas(
                        planetary = state.planetary,
                        clades = state.clades,
                        livingOrganisms = state.livingOrganisms,
                        selectedOrganismId = state.selectedOrganismId,
                        isPaused = state.isPaused,
                        overlays = state.overlays,
                        activeCataclysm = state.activeCataclysm,
                        weather = state.weather,
                        onSelectOrganism = onSelectOrganism,
                        onTapBackground = { onClearSelectedOrganism() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                CosmicScale.SPECIMEN -> {
                    val specimen = state.selectedSpecimen ?: state.clades.firstOrNull()?.dominantMorphotype
                    if (specimen != null) {
                        SpecimenCanvas(
                            specimen = specimen,
                            isPaused = state.isPaused,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                CosmicScale.CELLULAR -> {
                    CellularCanvas(
                        isPaused = state.isPaused,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // Top-Center: Prominent Pause Banner when paused
        AnimatedVisibility(
            visible = state.isPaused,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 74.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = PixelPanel.copy(alpha = 0.95f),
                border = BorderStroke(1.5.dp, PixelCyan),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "⏸ SIMULATION FROZEN",
                        color = PixelCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "• Tap any creature to inspect",
                        color = PaleText,
                        fontSize = 11.sp
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(PixelEmerald)
                            .clickable { onTogglePlayPause() }
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Resume ▶",
                            color = Color.Black,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Bottom right: friendly view badge (plain names instead of powers-of-ten jargon)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(10.dp)
                .padding(bottom = 108.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(PixelPanel.copy(alpha = 0.9f))
                .border(1.5.dp, PixelBorderLight, RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            val f = state.currentScale.friendly
            Column {
                Text(
                    text = "${f.emoji} ${f.name}",
                    color = PaleText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(text = f.blurb, color = MutedText, fontSize = 11.sp)
            }
        }

        // Top Right: Retro Square Pixel Zoom Buttons
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
                .padding(top = 108.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PixelSquareButton(
                icon = Icons.Default.ZoomIn,
                contentDescription = "Zoom In",
                tint = PixelCyan,
                onClick = onZoomIn,
                testTag = "btn_zoom_in"
            )

            PixelSquareButton(
                icon = Icons.Default.ZoomOut,
                contentDescription = "Zoom Out",
                tint = PixelGold,
                onClick = onZoomOut,
                testTag = "btn_zoom_out"
            )
        }

        // Floating In-Viewport Specimen Quick-Card on selection
        AnimatedVisibility(
            visible = (state.selectedOrganism != null),
            enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 126.dp)
                .padding(horizontal = 14.dp)
        ) {
            val org = state.selectedOrganism ?: return@AnimatedVisibility
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = PixelPanel.copy(alpha = 0.96f),
                border = BorderStroke(2.dp, PixelGold),
                shadowElevation = 10.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp)
                    .testTag("specimen_quick_card")
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Header: Emoji, Common Name, Binomial, and Close (X)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(org.emoji, fontSize = 28.sp)
                            Column {
                                Text(
                                    text = org.commonName,
                                    color = PaleText,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1
                                )
                                Text(
                                    text = org.binomialName,
                                    color = PixelGold,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1
                                )
                            }
                        }

                        IconButton(
                            onClick = onClearSelectedOrganism,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MutedText,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Status / Telemetry Badges
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Trophic Role
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(org.trophicLevel.colorHex).copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, Color(org.trophicLevel.colorHex))
                        ) {
                            Text(
                                text = org.trophicLevel.label.split("(").first().trim(),
                                color = Color(org.trophicLevel.colorHex),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }

                        // Current Behavior
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = PixelPanelLight,
                            border = BorderStroke(1.dp, PixelBorderLight)
                        ) {
                            Text(
                                text = "${org.behavior.emoji} ${org.behavior.label}",
                                color = PaleText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }

                        // Physical Size
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = PixelPanelLight,
                            border = BorderStroke(1.dp, PixelBorderLight)
                        ) {
                            val lenCm = (org.phenotype.bodyLengthMeters * 100f).roundToInt()
                            Text(
                                text = "📏 ${lenCm} cm",
                                color = PixelCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Environmental Adaptation & Stress Status Banner
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (org.isStressed) PixelCrimson.copy(alpha = 0.18f) else PixelPanelLight,
                        border = BorderStroke(1.dp, if (org.isStressed) PixelCrimson else PixelEmerald.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = if (org.isStressed) "⚠️" else "🧬",
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = if (org.isStressed && org.stressReason != null) org.stressReason!! else org.adaptationNote,
                                    color = if (org.isStressed) Color(0xFFFF8B8B) else PixelEmerald,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                            Text(
                                text = "🌡️ ${"%.1f".format(state.planetary.surfaceTempCelsius)}°C | 🫧 ${"%.1f".format(state.planetary.atmosphere.o2Percent)}%",
                                color = MutedText,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Action Buttons: X-Ray, Mutate, Gene Lab
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = { onInspectSpecimenModal(org.phenotype) },
                            colors = ButtonDefaults.buttonColors(containerColor = PixelCyan),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1.1f)
                                .height(34.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Text(
                                text = "🦴 Inside (X-Ray)",
                                color = Color.Black,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = onMutateSpecimen,
                            colors = ButtonDefaults.buttonColors(containerColor = PixelPurple),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1.0f)
                                .height(34.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Text(
                                text = "🧬 Mutate",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = onOpenGeneLab,
                            colors = ButtonDefaults.buttonColors(containerColor = PixelPanelLight),
                            border = BorderStroke(1.dp, PixelEmerald),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(34.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text(
                                text = "🔬 Lab",
                                color = PixelEmerald,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PixelSquareButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(PixelPanelLight)
            .border(2.dp, PixelBorderLight, RoundedCornerShape(4.dp))
            .clickable { onClick() }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}
