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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CataclysmType
import com.example.model.SimulationState
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GodToolPanel(
    state: SimulationState,
    onDismiss: () -> Unit,
    onUpdateOrbit: (Float) -> Unit,
    onUpdateTilt: (Float) -> Unit,
    onUpdateLuminosity: (Float) -> Unit,
    onInjectGas: (co2: Float?, o2: Float?, ch4: Float?) -> Unit,
    onToggleVolcanism: () -> Unit,
    onMutateSpecimen: () -> Unit,
    onTriggerCataclysm: (CataclysmType) -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = PixelPanel,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .width(48.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(PixelBorderLight)
            )
        },
        modifier = modifier.testTag("god_tools_modal")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(PixelPurple)
                    )
                    Text(
                        text = "THE GOD-TOOL CATALOG",
                        color = PixelGold,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = MutedText)
                }
            }

            // Section 1: TERRAFORM (Blue)
            PixelToolCategoryCard(
                categoryTitle = "1. ASTROPHYSICAL & TERRAFORM DIALS",
                categoryColor = PixelCyan
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Orbital Distance Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Orbital Semi-Major Axis", color = PaleText, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            Text(
                                String.format("%.2f AU", state.planetary.orbitalDistanceAU),
                                color = PixelCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Slider(
                            value = state.planetary.orbitalDistanceAU,
                            onValueChange = { onUpdateOrbit(it) },
                            valueRange = 0.70f..1.80f,
                            colors = SliderDefaults.colors(
                                thumbColor = PixelCyan,
                                activeTrackColor = PixelCyan,
                                inactiveTrackColor = PixelBorderLight
                            ),
                            modifier = Modifier.testTag("slider_orbit")
                        )
                    }

                    // Axial Tilt Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Axial Obliquity Tilt", color = PaleText, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            Text(
                                String.format("%.1f°", state.planetary.axialTiltDeg),
                                color = PixelCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Slider(
                            value = state.planetary.axialTiltDeg,
                            onValueChange = { onUpdateTilt(it) },
                            valueRange = 0f..90f,
                            colors = SliderDefaults.colors(
                                thumbColor = PixelCyan,
                                activeTrackColor = PixelCyan,
                                inactiveTrackColor = PixelBorderLight
                            ),
                            modifier = Modifier.testTag("slider_tilt")
                        )
                    }

                    // Atmospheric Gas Injections
                    Text(
                        text = "Geochemical Synthesis Injections",
                        color = PixelGold,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = { onInjectGas(state.planetary.atmosphere.co2Ppm + 3000f, null, null) },
                            colors = ButtonDefaults.buttonColors(containerColor = PixelPanelLight),
                            shape = RoundedCornerShape(3.dp),
                            border = BorderStroke(1.dp, PixelGold),
                            modifier = Modifier.weight(1f).testTag("inject_co2")
                        ) {
                            Text("+ CO₂", fontSize = 10.sp, color = PixelGold, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { onInjectGas(null, (state.planetary.atmosphere.o2Percent + 5f).coerceAtMost(35f), null) },
                            colors = ButtonDefaults.buttonColors(containerColor = PixelPanelLight),
                            shape = RoundedCornerShape(3.dp),
                            border = BorderStroke(1.dp, PixelEmerald),
                            modifier = Modifier.weight(1f).testTag("inject_o2")
                        ) {
                            Text("+ O₂", fontSize = 10.sp, color = PixelEmerald, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { onInjectGas(null, null, state.planetary.atmosphere.ch4Ppm + 1000f) },
                            colors = ButtonDefaults.buttonColors(containerColor = PixelPanelLight),
                            shape = RoundedCornerShape(3.dp),
                            border = BorderStroke(1.dp, PixelPurple),
                            modifier = Modifier.weight(1f).testTag("inject_ch4")
                        ) {
                            Text("+ CH₄", fontSize = 10.sp, color = PixelPurple, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Section 2: MUTAGENIC VECTORS (Purple)
            PixelToolCategoryCard(
                categoryTitle = "2. MUTAGENIC & INTERVENTIONIST VECTORS",
                categoryColor = PixelPurple
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Targeted Retroviral & Cosmic Ray Insertion into: ${state.selectedSpecimen?.binomialName ?: "Specimen"}",
                        color = MutedText,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    Button(
                        onClick = onMutateSpecimen,
                        colors = ButtonDefaults.buttonColors(containerColor = PixelPurple),
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(2.dp, PixelGold),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("btn_mutate_dna")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Science,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "INJECT RETROVIRAL MUTAGEN (vDNA EDIT)",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Section 3: CATACLYSMS & MASS EXTINCTIONS (Red)
            PixelToolCategoryCard(
                categoryTitle = "3. MASS EXTINCTION CATACLYSM SCENARIOS",
                categoryColor = PixelCrimson
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CataclysmType.values().forEach { cataclysm ->
                        PixelCataclysmTriggerCard(
                            cataclysm = cataclysm,
                            onTrigger = {
                                onTriggerCataclysm(cataclysm)
                                onDismiss()
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PixelToolCategoryCard(
    categoryTitle: String,
    categoryColor: Color,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PixelPanelLight),
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(2.dp, categoryColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = categoryTitle,
                color = categoryColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.5.sp
            )
            content()
        }
    }
}

@Composable
private fun PixelCataclysmTriggerCard(
    cataclysm: CataclysmType,
    onTrigger: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(3.dp))
            .background(PixelVoid)
            .border(2.dp, Color(cataclysm.colorHex), RoundedCornerShape(3.dp))
            .clickable { onTrigger() }
            .padding(8.dp)
            .testTag("cataclysm_${cataclysm.name.lowercase()}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = cataclysm.title,
                    color = Color(cataclysm.colorHex),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = cataclysm.subtitle,
                    color = PaleText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = cataclysm.primaryPhysics,
                    color = MutedText,
                    fontSize = 9.sp,
                    maxLines = 2,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = onTrigger,
                colors = ButtonDefaults.buttonColors(containerColor = Color(cataclysm.colorHex)),
                shape = RoundedCornerShape(3.dp),
                border = BorderStroke(1.dp, PixelGold),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.height(30.dp)
            ) {
                Text(
                    text = "UNLEASH",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
