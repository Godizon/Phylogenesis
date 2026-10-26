package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.window.DialogProperties
import com.example.model.CelestialScenario
import com.example.model.PlanetaryState
import com.example.ui.theme.*

/**
 * Full-screen celestial scenario picker allowing one-tap switching between
 * authentic astrophysical environments (Earth, Mars, Europa, Titan, Epsilon Eridani).
 */
@Composable
fun CelestialObservatoryModal(
    currentPlanetary: PlanetaryState,
    onDismiss: () -> Unit,
    onSelectScenario: (CelestialScenario) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onDismiss() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier
                .fillMaxSize()
                .padding(12.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(2.dp, PixelGold, RoundedCornerShape(16.dp))
                .testTag("celestial_observatory_modal"),
            color = PixelPanel
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🌌 CELESTIAL OBSERVATORY",
                            color = PixelGold,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Simulate authentic astrophysical worlds & extreme biological adaptations",
                            color = MutedText,
                            fontSize = 12.sp
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = PaleText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(CelestialScenario.values()) { scenario ->
                        val isCurrent = (scenario.title.startsWith("Earth") && currentPlanetary.gravityG == 1.0f && currentPlanetary.surfaceTempCelsius > 0f && currentPlanetary.hydrospherePercent > 50f) ||
                                (scenario == CelestialScenario.MARS_DESERT && currentPlanetary.surfacePressureBar < 0.1f) ||
                                (scenario == CelestialScenario.EUROPA_ABYSS && currentPlanetary.surfaceTempCelsius < -100f && currentPlanetary.hydrospherePercent > 90f) ||
                                (scenario == CelestialScenario.TITAN_CRYOMETHANE && currentPlanetary.surfaceTempCelsius < -150f && currentPlanetary.atmosphere.ch4Ppm > 10000f) ||
                                (scenario == CelestialScenario.ERIDANI_EYEBALL && currentPlanetary.isEyeballWorld)

                        ScenarioCard(
                            scenario = scenario,
                            isCurrent = isCurrent,
                            onSelect = { onSelectScenario(scenario) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScenarioCard(
    scenario: CelestialScenario,
    isCurrent: Boolean,
    onSelect: () -> Unit
) {
    val borderColor = if (isCurrent) PixelEmerald else PixelBorderLight
    val bgColor = if (isCurrent) PixelEmerald.copy(alpha = 0.08f) else PixelPanelLight

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        border = BorderStroke(if (isCurrent) 2.dp else 1.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Title & Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(scenario.iconEmoji, fontSize = 28.sp)
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = scenario.title,
                                color = PaleText,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (isCurrent) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = PixelEmerald
                                ) {
                                    Text(
                                        text = "ACTIVE WORLD",
                                        color = Color.Black,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = scenario.subtitle,
                            color = PixelCyan,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Button(
                    onClick = onSelect,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCurrent) PixelEmerald else PixelCyan
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (isCurrent) "Inhabited" else "Inhabit World ▶",
                        color = Color.Black,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Description
            Text(
                text = scenario.description,
                color = PaleText.copy(alpha = 0.85f),
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            // Environmental Metrics Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MetricChip("🌡️", "${scenario.surfaceTempC}°C")
                MetricChip("⚖️", "${scenario.gravityG}g")
                MetricChip("🫧", "${scenario.atmosphere.o2Percent}% O₂")
                MetricChip("🛡️", "${scenario.magnetosphereMicroTesla} µT")
            }

            // Evolutionary Challenge Box
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = PixelVoid.copy(alpha = 0.7f),
                border = BorderStroke(1.dp, PixelGold.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("🧬", fontSize = 12.sp)
                    Text(
                        text = "Challenge: ${scenario.evolutionaryChallenge}",
                        color = PixelGold,
                        fontSize = 11.sp,
                        lineHeight = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricChip(icon: String, text: String) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = PixelVoid,
        border = BorderStroke(1.dp, PixelBorderLight)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(icon, fontSize = 10.sp)
            Text(
                text = text,
                color = PaleText,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
