package com.example.ui.components

import androidx.activity.compose.BackHandler
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
    BackHandler { onDismiss() }

    var selectedTab by remember { mutableIntStateOf(0) }
    var speciesName by remember { mutableStateOf(initialSpecimen?.binomialName ?: "Synthetica exotica") }
    var commonName by remember { mutableStateOf(initialSpecimen?.commonName ?: "Engineered Morphotype") }
    var cephalicCount by remember { mutableStateOf(initialSpecimen?.segments?.count { it.type == SegmentType.CEPHALIC }?.coerceIn(1, 3) ?: 1) }
    var thoracicCount by remember { mutableStateOf(initialSpecimen?.segments?.count { it.type == SegmentType.THORACIC }?.coerceIn(1, 8) ?: 4) }
    var abdominalCount by remember { mutableStateOf(initialSpecimen?.segments?.count { it.type == SegmentType.ABDOMINAL || it.type == SegmentType.CAUDAL }?.coerceIn(1, 6) ?: 3) }
    var selectedLimb by remember { mutableStateOf(initialSpecimen?.segments?.firstOrNull { it.limbType != LimbType.NONE }?.limbType ?: LimbType.JOINTED_WALKER) }
    var activeCodons by remember {
        mutableStateOf(
            initialSpecimen?.activeCodons?.toSet() ?: setOf(
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
                                text = "🧪 CREATURE DESIGNER",
                                color = PixelCyan,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "GRAVITY ON THIS PLANET: ${planetary.gravityG}g (${planetary.archetype.title.uppercase()})",
                                color = if (planetary.gravityG > 1.5f) PixelCrimson else PixelGold,
                                fontSize = 12.sp,
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
                                    text = "BODY STATS",
                                    color = PixelCyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "Length: ${String.format("%.2f m", previewOrganism.bodyLengthMeters)}",
                                    color = PaleText,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "Mass: ${String.format("%.2f kg", previewOrganism.massKg)}",
                                    color = PaleText,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                val stress = previewOrganism.biomechanics.skeletalYieldStressPercent
                                Text(
                                    text = "Bone Strain: ${String.format("%.0f%%", stress)}",
                                    color = if (stress < 70f) PixelEmerald else if (stress < 100f) PixelGold else PixelCrimson,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "Food Needed: ${String.format("%.0f kJ/day", previewOrganism.biomechanics.dailyMetabolicKj)}",
                                    color = PaleText,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    // Evo-Devo Workbench Tab Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val tabs = listOf(
                            0 to "🦴 Morphology",
                            1 to "🧬 Codons",
                            2 to "⚖️ Biomechanics"
                        )
                        tabs.forEach { (idx, title) ->
                            val isSelected = selectedTab == idx
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (isSelected) PixelCyan else PixelVoid,
                                border = BorderStroke(1.5.dp, if (isSelected) PixelGold else PixelBorderDark),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedTab = idx }
                            ) {
                                Text(
                                    text = title,
                                    color = if (isSelected) OnPrimary else PaleText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 7.dp)
                                )
                            }
                        }
                    }

                    // TAB 0: Morphology & Hox Segments
                    if (selectedTab == 0) {
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
                                    text = "HOX METAMERE SEGMENTS",
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
                                    SegmentCounter(label = "Thoracic (Body/Limbs)", count = thoracicCount, min = 1, max = 8) {
                                        thoracicCount = it
                                    }
                                    SegmentCounter(label = "Abdominal / Tail", count = abdominalCount, min = 1, max = 6) {
                                        abdominalCount = it
                                    }
                                }
                            }
                        }

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
                                    text = "LOCOMOTIVE APPENDAGE KINEMATICS",
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
                                    LimbType.values().forEach { limb ->
                                        val isSelected = selectedLimb == limb
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(if (isSelected) PixelCyan else PixelVoid)
                                                .border(1.5.dp, if (isSelected) PixelGold else PixelBorderDark, RoundedCornerShape(3.dp))
                                                .clickable { selectedLimb = limb }
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = limb.label,
                                                color = if (isSelected) OnPrimary else PaleText,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // TAB 1: vDNA Codons & Biological Traits
                    if (selectedTab == 1) {
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
                                    text = "VIRTUAL DNA CODONS (Select active gene expressions)",
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
                                    val allCodons = listOf(
                                        CodonType.CHITIN_SYNTHASE to "🛡️ Chitin Armor",
                                        CodonType.CALCIFICATION_CA3PO4 to "🦴 Bone Calcium",
                                        CodonType.COLLAGEN_ELASTIN to "🧬 Collagen Matrix",
                                        CodonType.PNEUMATIC_BONES to "🪶 Hollow Air Bones",
                                        CodonType.KERATIN_FEATHERS to "🦅 Keratin Feathers",
                                        CodonType.CYTOCHROME_C to "⚡ Cytochrome-c (Mito)",
                                        CodonType.RUBISCO to "🌿 Rubisco Photosynthesis",
                                        CodonType.CATALASE to "🧪 Peroxide Catalase",
                                        CodonType.AZOTOSOME_LIPID to "🪐 Cryo Azotosome",
                                        CodonType.VIRAL_CAPSID_INJECT to "🦠 Capsid Injector",
                                        CodonType.VASCULAR_PUMP to "🫀 Multi-Chamber Heart",
                                        CodonType.SPIRACLE_TRACHEA to "🫁 Tracheal Canals",
                                        CodonType.ENDOTHERMIC_THERMO to "🔥 Warm-Blooded Endothermy",
                                        CodonType.NEOCORTEX_HOX to "🧠 Neocortex Cephalization"
                                    )

                                    allCodons.forEach { (codon, label) ->
                                        val isChecked = activeCodons.contains(codon)
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(if (isChecked) PixelPurple else PixelVoid)
                                                .border(1.5.dp, if (isChecked) PixelGold else PixelBorderLight, RoundedCornerShape(3.dp))
                                                .clickable {
                                                    activeCodons = if (isChecked) activeCodons - codon else activeCodons + codon
                                                }
                                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = label,
                                                color = if (isChecked) Color.White else PaleText,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // TAB 2: Biomechanics & Physical Laws
                    if (selectedTab == 2) {
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
                                    text = "BIOMECHANICAL & PHYSIOLOGICAL METER",
                                    color = PixelGold,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace
                                )

                                val metrics = previewOrganism.biomechanics

                                // Square-Cube Stress Bar
                                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Square-Cube Skeletal Yield Stress:",
                                            color = PaleText,
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            text = "${metrics.skeletalYieldStressPercent.toInt()}% (${if (metrics.skeletalYieldStressPercent < 70f) "Structural Reserve" else if (metrics.skeletalYieldStressPercent < 100f) "Yield Limit" else "COLLAPSE RISK!"})",
                                            color = if (metrics.skeletalYieldStressPercent < 70f) PixelEmerald else if (metrics.skeletalYieldStressPercent < 100f) PixelGold else PixelCrimson,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = { (metrics.skeletalYieldStressPercent / 120f).coerceIn(0f, 1f) },
                                        color = if (metrics.skeletalYieldStressPercent < 70f) PixelEmerald else if (metrics.skeletalYieldStressPercent < 100f) PixelGold else PixelCrimson,
                                        trackColor = PixelVoid,
                                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(2.dp))
                                    )
                                }

                                Divider(color = PixelBorderDark, thickness = 1.dp)

                                // Fick's Law & Respiration
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Fick's Diffusion Safety Margin:", color = PaleText, fontSize = 11.sp)
                                    Text(
                                        "${String.format("%.2f", metrics.vascularDiffusionSafety)}x",
                                        color = if (metrics.vascularDiffusionSafety >= 1.0f) PixelEmerald else PixelCrimson,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Respiration Organ:", color = PaleText, fontSize = 11.sp)
                                    Text(previewOrganism.respirationSystem, color = PixelCyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Circulation System:", color = PaleText, fontSize = 11.sp)
                                    Text(previewOrganism.circulationSystem, color = PixelCyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                }

                                Divider(color = PixelBorderDark, thickness = 1.dp)

                                // Reynolds Number & Kleiber Metabolic
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Reynolds Number (Re):", color = PaleText, fontSize = 11.sp)
                                    Text(
                                        "${metrics.reynoldsNumber.toInt()} (${if (metrics.reynoldsNumber < 10f) "Stokes Viscous" else "Inertial Turbulent"})",
                                        color = PaleText,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Kleiber Basal Metabolic Cost:", color = PaleText, fontSize = 11.sp)
                                    Text(
                                        "${metrics.dailyMetabolicKj.toInt()} kJ / day",
                                        color = PixelGold,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
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
                            text = "CANCEL",
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
                            text = if (isViable) "🌍 RELEASE INTO THE WORLD ►" else "💥 TOO HEAVY! IT WOULD COLLAPSE",
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
            fontSize = 11.sp,
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
