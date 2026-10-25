package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.window.Dialog
import com.example.model.CodonType
import com.example.model.OrganismPhenotype
import com.example.ui.theme.*
import com.example.ui.viewport.SpecimenCanvas

@Composable
fun SpecimenInspector(
    specimen: OrganismPhenotype?,
    onDismiss: () -> Unit,
    onMutate: () -> Unit,
    onOpenGeneEditor: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (specimen == null) return

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .testTag("specimen_inspector_modal"),
            shape = RoundedCornerShape(4.dp),
            color = PixelPanel,
            border = BorderStroke(3.dp, PixelBorderLight),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Modal Title Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "ANATOMICAL X-RAY (16-BIT)",
                            color = PixelGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = specimen.binomialName,
                            color = PaleText,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "${specimen.commonName} • ${specimen.phylum}",
                            color = PixelCyan,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = MutedText)
                    }
                }

                // Center: 16-Bit Specimen Articulation Canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(PixelVoid)
                        .border(2.dp, PixelBorderLight, RoundedCornerShape(4.dp))
                ) {
                    SpecimenCanvas(
                        specimen = specimen,
                        modifier = Modifier.fillMaxSize()
                    )

                    Text(
                        text = "LIVE PIXEL IK & VASCULAR MONITOR",
                        color = PixelCyan.copy(alpha = 0.6f),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(6.dp)
                    )
                }

                // Biomechanical & Physiological Telemetry Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = PixelPanelLight),
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(2.dp, PixelBorderLight)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = "PHYSIOLOGY & BIOMECHANICAL STRESS",
                            color = PixelGold,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace
                        )

                        DiagnosticMetric("Respiration System", specimen.respirationSystem)
                        DiagnosticMetric("Vascular Circulation", specimen.circulationSystem)
                        DiagnosticMetric("Locomotion Kinematics", specimen.gaitDescription)
                        DiagnosticMetric("Length & Wet Mass", "${String.format("%.3f", specimen.bodyLengthMeters)} m (${String.format("%.3f", specimen.massKg)} kg)")
                        DiagnosticMetric("Reynolds Number (Re)", String.format("%.0f (%s)", specimen.biomechanics.reynoldsNumber, if (specimen.biomechanics.reynoldsNumber < 10) "Creeping Flow" else "Inertial Flow"))
                        DiagnosticMetric("Thermodynamic Cost", "${String.format("%.2f", specimen.biomechanics.dailyMetabolicKj)} kJ/day (ΔG)")

                        // Skeletal Stress % Progress
                        Column(modifier = Modifier.padding(top = 4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Skeletal Yield Stress (Square-Cube Limit)", color = MutedText, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                Text(
                                    "${specimen.biomechanics.skeletalYieldStressPercent.toInt()}%",
                                    color = if (specimen.biomechanics.skeletalYieldStressPercent > 80f) PixelCrimson else PixelEmerald,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            LinearProgressIndicator(
                                progress = { (specimen.biomechanics.skeletalYieldStressPercent / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = if (specimen.biomechanics.skeletalYieldStressPercent > 80f) PixelCrimson else PixelCyan,
                                trackColor = PixelBorderLight
                            )
                        }
                    }
                }

                // Virtual DNA Genome & GRN Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = PixelPanelLight),
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(2.dp, PixelBorderLight)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "EXPRESSED vDNA CODONS",
                                color = PixelPurple,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "${specimen.activeCodons.size} CODONS",
                                color = PixelGold,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(specimen.activeCodons) { codon ->
                                CodonPill(codon = codon)
                            }
                        }

                        Text(
                            text = "Hox Metameres: ${specimen.segments.size} Segments (${specimen.segments.count { it.limbPairCount > 0 }} with Locomotive Appendages)",
                            color = MutedText,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Gene Lab Studio Button
                Button(
                    onClick = {
                        onDismiss()
                        onOpenGeneEditor()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("btn_open_gene_lab"),
                    colors = ButtonDefaults.buttonColors(containerColor = PixelCyan),
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(2.dp, PixelGold)
                ) {
                    Icon(
                        imageVector = Icons.Default.Biotech,
                        contentDescription = null,
                        tint = OnPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "OPEN vDNA GENE LAB (MORPHOGENESIS)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = OnPrimary,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Bottom Action: Mutate Genome
                Button(
                    onClick = onMutate,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("btn_mutate_in_modal"),
                    colors = ButtonDefaults.buttonColors(containerColor = PixelPurple),
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(2.dp, PixelGold)
                ) {
                    Icon(
                        imageVector = Icons.Default.Science,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "TRIGGER RETROVIRAL MUTATION (NEO-FUNCTIONALIZATION)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun DiagnosticMetric(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = MutedText, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        Text(text = value, color = PaleText, fontSize = 9.sp, fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun CodonPill(codon: CodonType) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .background(PixelVoid)
            .border(1.dp, PixelPurple, RoundedCornerShape(3.dp))
            .padding(horizontal = 7.dp, vertical = 4.dp)
    ) {
        Column {
            Text(
                text = codon.code,
                color = PixelPurple,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = codon.name.replace("_", " "),
                color = PaleText,
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
