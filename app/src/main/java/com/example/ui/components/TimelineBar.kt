package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import com.example.model.GeologicalEpoch
import com.example.model.SimulationState
import com.example.ui.theme.*

@Composable
fun TimelineBar(
    state: SimulationState,
    onWarpToEpoch: (GeologicalEpoch) -> Unit,
    onSelectScale: (CosmicScale) -> Unit,
    onOpenGodTools: () -> Unit,
    onOpenCladogram: () -> Unit,
    onOpenGeneEditor: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("timeline_and_tool_dock"),
        color = PixelVoid,
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, PixelBorderDark)
                .background(PixelPanel)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Row 1: Cosmic Zoom Scale Selector (Powers of Ten - WorldBox style)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "ZOOM:",
                        color = PixelCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )

                    CosmicScale.values().forEach { scale ->
                        val isSelected = state.currentScale == scale
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (isSelected) PixelCyan else PixelVoid)
                                .border(
                                    2.dp,
                                    if (isSelected) PixelGold else PixelBorderLight,
                                    RoundedCornerShape(3.dp)
                                )
                                .clickable { onSelectScale(scale) }
                                .padding(horizontal = 7.dp, vertical = 4.dp)
                                .testTag("scale_${scale.name.lowercase()}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = scale.meterScale,
                                    color = if (isSelected) OnPrimary else PaleText,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = scale.name,
                                    color = if (isSelected) OnPrimary else MutedText,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // vDNA Gene Lab Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(PixelCyan)
                            .border(2.dp, PixelGold, RoundedCornerShape(4.dp))
                            .clickable { onOpenGeneEditor() }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .testTag("btn_open_gene_editor")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Biotech,
                                contentDescription = null,
                                tint = OnPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "GENE LAB",
                                color = OnPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // WorldBox-style God-Tools Trigger Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(PixelPurple)
                            .border(2.dp, PixelGold, RoundedCornerShape(4.dp))
                            .clickable { onOpenGodTools() }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .testTag("btn_open_god_tools")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoFixHigh,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "GOD-TOOLS",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // Row 2: Retro Deep-Time Timeline Scroller
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TimelineNode(
                    epoch = GeologicalEpoch.HADEAN,
                    label = "4.0 Ga",
                    milestone = "LUCA Vents",
                    isCurrent = state.currentEpoch == GeologicalEpoch.HADEAN,
                    onWarp = { onWarpToEpoch(GeologicalEpoch.HADEAN) }
                )

                TimelineNode(
                    epoch = GeologicalEpoch.ARCHEAN,
                    label = "3.8 Ga",
                    milestone = "Chemoliths",
                    isCurrent = state.currentEpoch == GeologicalEpoch.ARCHEAN,
                    onWarp = { onWarpToEpoch(GeologicalEpoch.ARCHEAN) }
                )

                TimelineNode(
                    epoch = GeologicalEpoch.PROTEROZOIC,
                    label = "2.5 Ga",
                    milestone = "Photosynthesis",
                    isCurrent = state.currentEpoch == GeologicalEpoch.PROTEROZOIC,
                    onWarp = { onWarpToEpoch(GeologicalEpoch.PROTEROZOIC) }
                )

                TimelineNode(
                    epoch = GeologicalEpoch.PALEOZOIC,
                    label = "541 Ma",
                    milestone = "Cambrian Explosion",
                    isCurrent = state.currentEpoch == GeologicalEpoch.PALEOZOIC,
                    onWarp = { onWarpToEpoch(GeologicalEpoch.PALEOZOIC) }
                )

                TimelineNode(
                    epoch = GeologicalEpoch.MESOZOIC,
                    label = "252 Ma",
                    milestone = "Mesozoic Fauna",
                    isCurrent = state.currentEpoch == GeologicalEpoch.MESOZOIC,
                    onWarp = { onWarpToEpoch(GeologicalEpoch.MESOZOIC) }
                )

                TimelineNode(
                    epoch = GeologicalEpoch.CENOZOIC,
                    label = "Present",
                    milestone = "Modern Biome",
                    isCurrent = state.currentEpoch == GeologicalEpoch.CENOZOIC,
                    onWarp = { onWarpToEpoch(GeologicalEpoch.CENOZOIC) }
                )
            }
        }
    }
}

@Composable
private fun TimelineNode(
    epoch: GeologicalEpoch,
    label: String,
    milestone: String,
    isCurrent: Boolean,
    onWarp: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .background(if (isCurrent) PixelPanelLight else PixelVoid)
            .border(
                2.dp,
                if (isCurrent) PixelGold else PixelBorderLight,
                RoundedCornerShape(3.dp)
            )
            .clickable { onWarp() }
            .padding(horizontal = 8.dp, vertical = 5.dp)
            .testTag("timeline_node_${epoch.name.lowercase()}")
    ) {
        Column(horizontalAlignment = Alignment.Start) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(if (isCurrent) PixelGold else MutedText)
                )
                Text(
                    text = label,
                    color = if (isCurrent) PixelGold else PaleText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
            Text(
                text = milestone,
                color = if (isCurrent) PixelCyan else MutedText,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
