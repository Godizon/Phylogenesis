package com.example.ui.viewport

import androidx.compose.animation.*
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
import com.example.model.OrganismPhenotype
import com.example.model.SimulationState
import com.example.ui.components.friendly
import com.example.ui.theme.*

@Composable
fun SimulationViewport(
    state: SimulationState,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onSelectSpecimen: (OrganismPhenotype) -> Unit,
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
                    // WorldBox-style pixel sandbox world canvas
                    PixelWorldCanvas(
                        planetary = state.planetary,
                        clades = state.clades,
                        overlays = state.overlays,
                        activeCataclysm = state.activeCataclysm,
                        weather = state.weather,
                        onSelectSpecimen = onSelectSpecimen,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                CosmicScale.SPECIMEN -> {
                    val specimen = state.selectedSpecimen ?: state.clades.firstOrNull()?.dominantMorphotype
                    if (specimen != null) {
                        SpecimenCanvas(
                            specimen = specimen,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                CosmicScale.CELLULAR -> {
                    CellularCanvas(
                        modifier = Modifier.fillMaxSize()
                    )
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
