package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.model.Clade
import com.example.model.SimulationState
import com.example.ui.theme.*

@Composable
fun CladeDrawer(
    state: SimulationState,
    onSelectClade: (String) -> Unit,
    onInspectSpecimen: () -> Unit,
    onToggleBiomassHeatmap: () -> Unit,
    onToggleTrophicFlow: () -> Unit,
    onToggleSpeciationHotspots: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .width(280.dp)
            .fillMaxHeight()
            .testTag("clade_ecosystem_drawer"),
        color = PixelVoid.copy(alpha = 0.96f),
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .border(2.dp, PixelBorderDark)
                .background(PixelPanel)
                .verticalScroll(rememberScrollState())
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(PixelEmerald)
                )
                Text(
                    text = "CLADE & ECOSYSTEM",
                    color = PixelGold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }

            // Selected Clade Card
            val selectedClade = state.selectedClade
            if (selectedClade != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = PixelPanelLight),
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(2.dp, Color(selectedClade.trophicLevel.colorHex))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedClade.name,
                                color = PaleText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color(selectedClade.trophicLevel.colorHex).copy(alpha = 0.25f))
                                    .border(1.dp, Color(selectedClade.trophicLevel.colorHex), RoundedCornerShape(3.dp))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (selectedClade.isExtinct) "EXTINCT" else "EXTANT",
                                    color = if (selectedClade.isExtinct) PixelCrimson else Color(selectedClade.trophicLevel.colorHex),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Text(
                            text = selectedClade.trophicLevel.label,
                            color = Color(selectedClade.trophicLevel.colorHex),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )

                        Divider(color = PixelBorderLight, thickness = 1.dp)

                        CladeMetricRow("Biomass", String.format("%.2e t", selectedClade.biomassTons))
                        CladeMetricRow("Avg Lifespan", "${selectedClade.averageLifespanYears} years")
                        CladeMetricRow("Origin", "${selectedClade.speciationDateGa} Ga")
                        CladeMetricRow("Thermal Band", "${selectedClade.thermalToleranceMinC.toInt()}°C to ${selectedClade.thermalToleranceMaxC.toInt()}°C")

                        // Fitness Bar
                        Column(modifier = Modifier.fillMaxWidth().padding(top = 2.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Fitness Index", color = MutedText, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                Text(String.format("%.2f", selectedClade.meanFitness), color = PixelCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                            LinearProgressIndicator(
                                progress = { selectedClade.meanFitness.coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = PixelCyan,
                                trackColor = PixelBorderLight,
                            )
                        }

                        // Inspect Organism Button (WorldBox style)
                        Button(
                            onClick = onInspectSpecimen,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                                .height(34.dp)
                                .testTag("btn_inspect_specimen"),
                            colors = ButtonDefaults.buttonColors(containerColor = PixelCyan),
                            shape = RoundedCornerShape(3.dp),
                            border = BorderStroke(1.dp, PixelGold),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Biotech,
                                contentDescription = null,
                                tint = OnPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "ANATOMICAL X-RAY",
                                color = OnPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // Dominant Phyla List
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = PixelPanelLight),
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(2.dp, PixelBorderLight)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = "DOMINANT LINEAGES",
                        color = PixelCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )

                    state.clades.forEach { clade ->
                        val isSelected = clade.id == state.selectedCladeId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (isSelected) PixelCyan.copy(alpha = 0.2f) else Color.Transparent)
                                .border(
                                    1.dp,
                                    if (isSelected) PixelGold else Color.Transparent,
                                    RoundedCornerShape(3.dp)
                                )
                                .clickable { onSelectClade(clade.id) }
                                .padding(horizontal = 6.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(Color(clade.phylum.colorHex))
                                )
                                Column {
                                    Text(
                                        text = clade.name,
                                        color = if (clade.isExtinct) SubduedText else PaleText,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = clade.phylum.label,
                                        color = MutedText,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                            Text(
                                text = String.format("%.1e t", clade.biomassTons),
                                color = PixelEmerald,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // Viewport Overlays Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = PixelPanelLight),
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(2.dp, PixelBorderLight)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "VIEWPORT OVERLAYS",
                        color = PixelCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )

                    PixelOverlayToggleRow(
                        title = "Biomass Heatmap",
                        checked = state.overlays.showBiomassHeatmap,
                        color = PixelEmerald,
                        onToggle = onToggleBiomassHeatmap,
                        testTag = "toggle_heatmap"
                    )

                    PixelOverlayToggleRow(
                        title = "Trophic Flow Vectors",
                        checked = state.overlays.showTrophicFlowVectors,
                        color = PixelGold,
                        onToggle = onToggleTrophicFlow,
                        testTag = "toggle_trophic"
                    )

                    PixelOverlayToggleRow(
                        title = "Speciation Hotspots",
                        checked = state.overlays.showSpeciationHotspots,
                        color = PixelPurple,
                        onToggle = onToggleSpeciationHotspots,
                        testTag = "toggle_speciation"
                    )
                }
            }
        }
    }
}

@Composable
private fun CladeMetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = MutedText, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        Text(text = value, color = PaleText, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PixelOverlayToggleRow(
    title: String,
    checked: Boolean,
    color: Color,
    onToggle: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 3.dp)
            .testTag(testTag),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(if (checked) color else SubduedText)
            )
            Text(text = title, color = if (checked) PaleText else MutedText, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        }
        Checkbox(
            checked = checked,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = color,
                uncheckedColor = SubduedText
            ),
            modifier = Modifier.size(20.dp)
        )
    }
}
