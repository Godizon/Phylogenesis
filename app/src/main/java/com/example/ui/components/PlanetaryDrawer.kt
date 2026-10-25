package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.example.model.PlanetaryState
import com.example.ui.theme.*

@Composable
fun PlanetaryDrawer(
    planetary: PlanetaryState,
    onToggleVolcanism: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .width(280.dp)
            .fillMaxHeight()
            .testTag("planetary_telemetry_drawer"),
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
                        .background(PixelCyan)
                )
                Text(
                    text = "PLANETARY TELEMETRY",
                    color = PixelGold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }

            // Star & Orbit Card
            PixelTelemetryCard(title = "STAR & ORBITAL MECHANICS") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    MetricRow(label = "Host Star", value = "${planetary.starType.label} (${planetary.starType.spectralClass})")
                    MetricRow(label = "Stellar Luminosity", value = String.format("%.2f L☉", planetary.stellarLuminosity))
                    MetricRow(label = "Orbital Distance", value = String.format("%.2f AU (e=%.2f)", planetary.orbitalDistanceAU, planetary.orbitalEccentricity))
                    MetricRow(label = "Axial Obliquity", value = String.format("%.1f°", planetary.axialTiltDeg))
                    MetricRow(label = "Effective Insolation", value = String.format("%.0f W/m²", planetary.insolationWm2))
                }
            }

            // Surface & Climate Card
            PixelTelemetryCard(title = "CLIMATOLOGY & SURFACE") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val tempColor = when {
                        planetary.surfaceTempCelsius < -10f -> IceBlue
                        planetary.surfaceTempCelsius > 35f -> PixelCrimson
                        else -> PixelEmerald
                    }
                    MetricRow(
                        label = "Surface Temp [AVG]",
                        value = String.format("%.1f °C", planetary.surfaceTempCelsius),
                        valueColor = tempColor
                    )
                    MetricRow(label = "Surface Pressure", value = String.format("%.2f bar", planetary.surfacePressureBar))
                    MetricRow(label = "Hydrosphere", value = String.format("%.1f %% H₂O", planetary.hydrospherePercent))
                    MetricRow(label = "Ocean pH", value = String.format("%.2f (%s)", planetary.oceanPh, if (planetary.oceanPh < 7.2f) "Acidic" else "Alkaline"))
                    MetricRow(
                        label = "Ice Albedo Coverage",
                        value = String.format("%.0f %%", planetary.iceAlbedoCoverage * 100f),
                        valueColor = if (planetary.isSnowballEarth) PixelSnow else PaleText
                    )
                }
            }

            // Atmospheric Chromatograph Card (Pixel style)
            PixelTelemetryCard(title = "GAS CHROMATOGRAPHY") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Chromatograph segmented pixel bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .border(1.dp, PixelBorderLight)
                    ) {
                        Box(modifier = Modifier.weight(planetary.atmosphere.n2Percent.coerceAtLeast(1f)).fillMaxHeight().background(Color(0xFF1E88E5)))
                        Box(modifier = Modifier.weight(planetary.atmosphere.o2Percent.coerceAtLeast(0.5f)).fillMaxHeight().background(PixelEmerald))
                        Box(modifier = Modifier.weight((planetary.atmosphere.co2Ppm / 1000f).coerceAtLeast(0.5f)).fillMaxHeight().background(PixelGold))
                        Box(modifier = Modifier.weight((planetary.atmosphere.ch4Ppm / 500f).coerceAtLeast(0.5f)).fillMaxHeight().background(PixelPurple))
                        Box(modifier = Modifier.weight(planetary.atmosphere.h2oPercent.coerceAtLeast(0.5f)).fillMaxHeight().background(PixelCyan))
                    }

                    // Legend
                    GasRow(name = "N₂ (Nitrogen)", value = String.format("%.1f%%", planetary.atmosphere.n2Percent), color = Color(0xFF1E88E5))
                    GasRow(name = "O₂ (Oxygen)", value = String.format("%.1f%%", planetary.atmosphere.o2Percent), color = PixelEmerald)
                    GasRow(name = "CO₂ (Carbon Dioxide)", value = String.format("%.0f ppm", planetary.atmosphere.co2Ppm), color = PixelGold)
                    GasRow(name = "CH₄ (Methane)", value = String.format("%.0f ppm", planetary.atmosphere.ch4Ppm), color = PixelPurple)
                    GasRow(name = "H₂O (Vapor)", value = String.format("%.1f%%", planetary.atmosphere.h2oPercent), color = PixelCyan)
                    GasRow(name = "SO₂ (Sulfur Dioxide)", value = String.format("%.0f ppm", planetary.atmosphere.so2Ppm), color = PixelLava)
                }
            }

            // Geodynamics & Magnetosphere Card
            PixelTelemetryCard(title = "GEODYNAMICS & DYNAMO") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    MetricRow(
                        label = "Geomagnetic Dynamo",
                        value = "${planetary.magnetosphereMicroTesla.toInt()} μT",
                        valueColor = if (planetary.isMagnetosphereShielded) PixelCyan else PixelCrimson
                    )
                    MetricRow(
                        label = "Solar Wind Stripping",
                        value = if (planetary.solarWindStripping) "ALERT: STRIPPING" else "SHIELDED",
                        valueColor = if (planetary.solarWindStripping) PixelCrimson else PixelEmerald
                    )
                    MetricRow(
                        label = "Mantle Convection",
                        value = if (planetary.mantleConvectionActive) "Active" else "Frozen"
                    )

                    Button(
                        onClick = onToggleVolcanism,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(34.dp)
                            .testTag("btn_toggle_volcanism"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (planetary.volcanicActivityIndex > 0.5f) PixelLava else PixelPanelLight
                        ),
                        shape = RoundedCornerShape(3.dp),
                        border = BorderStroke(1.dp, if (planetary.volcanicActivityIndex > 0.5f) PixelGold else PixelBorderLight),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (planetary.volcanicActivityIndex > 0.5f) "MANTLE PLUMES: HIGH" else "MANTLE PLUMES: DORMANT",
                            fontSize = 10.sp,
                            color = if (planetary.volcanicActivityIndex > 0.5f) PixelVoid else PaleText,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PixelTelemetryCard(
    title: String,
    content: @Composable () -> Unit
) {
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
                text = title,
                color = PixelCyan,
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
private fun MetricRow(
    label: String,
    value: String,
    valueColor: Color = PaleText
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = MutedText, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        Text(
            text = value,
            color = valueColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun GasRow(
    name: String,
    value: String,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
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
                    .background(color)
            )
            Text(text = name, color = PaleText, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        }
        Text(
            text = value,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}
