package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.engine.SimulationEngine
import com.example.model.*
import com.example.ui.theme.*
import kotlin.math.sin

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GeneEditorModal(
    planetary: PlanetaryState,
    initialSpecimen: OrganismPhenotype?,
    onDismiss: () -> Unit,
    onInjectOrganism: (OrganismPhenotype) -> Unit,
    modifier: Modifier = Modifier
) {
    var speciesName by remember { mutableStateOf("Synthetica exotica") }
    var commonName by remember { mutableStateOf("Chitinous Crawler") }
    var cephalicCount by remember { mutableStateOf(1) }
    var thoracicCount by remember { mutableStateOf(4) }
    var abdominalCount by remember { mutableStateOf(3) }
    var selectedLimb by remember { mutableStateOf(LimbType.JOINTED_WALKER) }
    var activeCodons by remember {
        mutableStateOf(
            setOf(
                CodonType.CHITIN_SYNTHASE,
                CodonType.CYTOCHROME_C,
                CodonType.VASCULAR_PUMP,
                CodonType.CATALASE
            )
        )
    }

    // Recompute synthesized organism and biomechanics
    val previewOrganism = remember(
        speciesName,
        commonName,
        cephalicCount,
        thoracicCount,
        abdominalCount,
        selectedLimb,
        activeCodons,
        planetary.gravityG
    ) {
        SimulationEngine.synthesizeOrganism(
            name = speciesName,
            commonName = commonName,
            codons = activeCodons.toList(),
            cephalicSegments = cephalicCount,
            thoracicSegments = thoracicCount,
            abdominalSegments = abdominalCount,
            limbType = selectedLimb,
            gravityG = planetary.gravityG
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_morph")
    val breathePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_morph"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .testTag("gene_editor_modal"),
            colors = CardDefaults.cardColors(containerColor = PixelPanel),
            shape = RoundedCornerShape(4.dp),
            border = BorderStroke(3.dp, PixelGold),
            elevation = CardDefaults.cardElevation(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Biotech,
                            contentDescription = null,
                            tint = PixelCyan,
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text(
                                text = "vDNA GENE LAB & EVO-DEVO STUDIO",
                                color = PixelCyan,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "PLANETARY GRAVITY LOAD: ${planetary.gravityG}g (${planetary.archetype.title.uppercase()})",
                                color = if (planetary.gravityG > 1.5f) PixelCrimson else PixelGold,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp).testTag("btn_close_gene_editor")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = PaleText
                        )
                    }
                }

                Divider(
                    color = PixelBorderDark,
                    thickness = 2.dp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                // Scrollable Body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Section 1: Live 16-Bit Morphological Sprite Preview & Diagnostics
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(PixelVoid)
                            .border(2.dp, PixelBorderLight, RoundedCornerShape(4.dp))
                    ) {
                        // Canvas drawing real-time pixel specimen
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            val centerX = w * 0.40f
                            val centerY = h * 0.50f
                            val pulseOffset = sin(breathePhase) * 2.5f

                            // Draw grid lines
                            val gridSpacing = 16.dp.toPx()
                            var gx = 0f
                            while (gx < w) {
                                drawLine(PixelBorderDark.copy(alpha = 0.35f), Offset(gx, 0f), Offset(gx, h), 1f)
                                gx += gridSpacing
                            }
                            var gy = 0f
                            while (gy < h) {
                                drawLine(PixelBorderDark.copy(alpha = 0.35f), Offset(0f, gy), Offset(w, gy), 1f)
                                gy += gridSpacing
                            }

                            // Draw specimen segments
                            val segCount = previewOrganism.segments.size
                            val segWidthPx = 14.dp.toPx()
                            val startX = centerX - (segCount * segWidthPx) / 2f

                            val isMineralized = previewOrganism.activeCodons.contains(CodonType.CALCIFICATION_CA3PO4)
                            val isChitin = previewOrganism.activeCodons.contains(CodonType.CHITIN_SYNTHASE)
                            val bodyColor = when {
                                isMineralized -> PixelBone
                                isChitin -> PixelChitin
                                else -> PixelFlesh
                            }

                            previewOrganism.segments.forEachIndexed { i, seg ->
                                val x = startX + i * segWidthPx
                                val segHeight = (28.dp.toPx() * seg.widthFraction) + (if (i % 2 == 0) pulseOffset else -pulseOffset)
                                val topY = centerY - segHeight / 2f

                                // Body Metamere tile
                                drawRect(
                                    color = bodyColor,
                                    topLeft = Offset(x, topY),
                                    size = Size(segWidthPx - 2.dp.toPx(), segHeight)
                                )

                                // Jointed Legs or Paddles
                                if (seg.limbType == LimbType.JOINTED_WALKER) {
                                    val legWave = sin(breathePhase + i * 0.8f) * 6.dp.toPx()
                                    // Upper leg
                                    drawLine(
                                        color = PixelGold,
                                        start = Offset(x + segWidthPx / 2f, topY),
                                        end = Offset(x + segWidthPx / 2f + 4.dp.toPx(), topY - 12.dp.toPx() + legWave),
                                        strokeWidth = 2.5.dp.toPx()
                                    )
                                    // Lower leg
                                    drawLine(
                                        color = PixelGold,
                                        start = Offset(x + segWidthPx / 2f, topY + segHeight),
                                        end = Offset(x + segWidthPx / 2f + 4.dp.toPx(), topY + segHeight + 12.dp.toPx() - legWave),
                                        strokeWidth = 2.5.dp.toPx()
                                    )
                                } else if (seg.limbType == LimbType.HYDROFOIL_FIN) {
                                    drawRect(
                                        color = PixelCyan.copy(alpha = 0.75f),
                                        topLeft = Offset(x, topY - 8.dp.toPx()),
                                        size = Size(segWidthPx, 6.dp.toPx())
                                    )
                                    drawRect(
                                        color = PixelCyan.copy(alpha = 0.75f),
                                        topLeft = Offset(x, topY + segHeight + 2.dp.toPx()),
                                        size = Size(segWidthPx, 6.dp.toPx())
                                    )
                                }

                                // Cephalic Ocelli (Eyes)
                                if (seg.type == SegmentType.CEPHALIC && i == 0) {
                                    drawRect(
                                        color = PixelGold,
                                        topLeft = Offset(x + 2.dp.toPx(), topY + 4.dp.toPx()),
                                        size = Size(4.dp.toPx(), 4.dp.toPx())
                                    )
                                }
                            }
                        }

                        // Right Diagnostics Card
                        Card(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 8.dp)
                                .width(170.dp),
                            colors = CardDefaults.cardColors(containerColor = PixelPanel.copy(alpha = 0.92f)),
                            shape = RoundedCornerShape(3.dp),
                            border = BorderStroke(1.dp, PixelBorderLight)
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "BIOMECHANICS",
                                    color = PixelCyan,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "Length: ${String.format("%.2f m", previewOrganism.bodyLengthMeters)}",
                                    color = PaleText,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "Mass: ${String.format("%.2f kg", previewOrganism.massKg)}",
                                    color = PaleText,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                val stress = previewOrganism.biomechanics.skeletalYieldStressPercent
                                Text(
                                    text = "Skel. Stress: ${String.format("%.0f%%", stress)}",
                                    color = if (stress < 70f) PixelEmerald else if (stress < 100f) PixelGold else PixelCrimson,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "Metab: ${String.format("%.0f kJ/d", previewOrganism.biomechanics.dailyMetabolicKj)}",
                                    color = PaleText,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    // Section 2: Hox Metamere Segment Regulators
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = PixelPanelLight),
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, PixelBorderLight)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "HOX SEGMENT REGULATORS (METAMERES)",
                                color = PixelGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SegmentCounter(label = "Cephalic (Head)", count = cephalicCount, min = 1, max = 3) {
                                    cephalicCount = it
                                }
                                SegmentCounter(label = "Thorax (Limbs)", count = thoracicCount, min = 1, max = 8) {
                                    thoracicCount = it
                                }
                                SegmentCounter(label = "Abdomen (Viscera)", count = abdominalCount, min = 1, max = 6) {
                                    abdominalCount = it
                                }
                            }
                        }
                    }

                    // Section 3: Locomotive Appendage Selector
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = PixelPanelLight),
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, PixelBorderLight)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "LOCOMOTIVE APPENDAGE",
                                color = PixelGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                LimbType.values().forEach { limb ->
                                    val isSelected = selectedLimb == limb
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(if (isSelected) PixelCyan else PixelVoid)
                                            .border(1.5.dp, if (isSelected) PixelGold else PixelBorderDark, RoundedCornerShape(3.dp))
                                            .clickable { selectedLimb = limb }
                                            .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = limb.label.split(" ").first(),
                                            color = if (isSelected) OnPrimary else PaleText,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Section 4: Virtual DNA Codon Matrix
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = PixelPanelLight),
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, PixelBorderLight)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "vDNA ENZYMATIC & STRUCTURAL CODONS",
                                color = PixelGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace
                            )

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val selectableCodons = listOf(
                                    CodonType.CHITIN_SYNTHASE to "Chitin Armor",
                                    CodonType.CALCIFICATION_CA3PO4 to "Bone Calcium",
                                    CodonType.COLLAGEN_ELASTIN to "Collagen Matrix",
                                    CodonType.CYTOCHROME_C to "Mito. Aerobic",
                                    CodonType.VASCULAR_PUMP to "Heart Pump",
                                    CodonType.RUBISCO to "Rubisco Fix",
                                    CodonType.CATALASE to "Antioxidant",
                                    CodonType.SPIRACLE_TRACHEA to "Tracheae"
                                )

                                selectableCodons.forEach { (codon, label) ->
                                    val isChecked = activeCodons.contains(codon)
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(if (isChecked) PixelPurple else PixelVoid)
                                            .border(1.5.dp, if (isChecked) PixelGold else PixelBorderLight, RoundedCornerShape(3.dp))
                                            .clickable {
                                                activeCodons = if (isChecked) activeCodons - codon else activeCodons + codon
                                            }
                                            .padding(horizontal = 8.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = label,
                                            color = if (isChecked) Color.White else PaleText,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Footer Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        border = BorderStroke(1.dp, PixelBorderLight),
                        shape = RoundedCornerShape(3.dp),
                        modifier = Modifier.testTag("btn_cancel_gene_edit")
                    ) {
                        Text(
                            text = "DISCARD",
                            color = PaleText,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    val isViable = previewOrganism.biomechanics.skeletalYieldStressPercent <= 110f
                    Button(
                        onClick = { onInjectOrganism(previewOrganism) },
                        enabled = isViable,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isViable) PixelCyan else PixelVoid,
                            disabledContainerColor = PixelVoid
                        ),
                        shape = RoundedCornerShape(3.dp),
                        border = BorderStroke(1.dp, if (isViable) PixelGold else PixelBorderDark),
                        modifier = Modifier.testTag("btn_inject_into_biosphere")
                    ) {
                        Text(
                            text = if (isViable) "RELEASE INTO BIOSPHERE ►" else "COLLAPSE UNDER GRAVITY",
                            color = if (isViable) OnPrimary else MutedText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SegmentCounter(
    label: String,
    count: Int,
    min: Int,
    max: Int,
    onChange: (Int) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            color = PaleText,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(PixelVoid)
                    .border(1.dp, PixelBorderLight, RoundedCornerShape(2.dp))
                    .clickable(enabled = count > min) { onChange(count - 1) },
                contentAlignment = Alignment.Center
            ) {
                Text("-", color = PaleText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Text(
                text = "$count",
                color = PixelGold,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.width(20.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(PixelVoid)
                    .border(1.dp, PixelBorderLight, RoundedCornerShape(2.dp))
                    .clickable(enabled = count < max) { onChange(count + 1) },
                contentAlignment = Alignment.Center
            ) {
                Text("+", color = PaleText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
