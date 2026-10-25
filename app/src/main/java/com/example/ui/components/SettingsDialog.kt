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
import androidx.compose.ui.window.Dialog
import com.example.model.GameSettings
import com.example.model.TemperatureUnit
import com.example.ui.theme.*

@Composable
fun SettingsDialog(
    settings: GameSettings,
    onUpdateSettings: (GameSettings) -> Unit,
    onRestartTutorial: () -> Unit,
    onReturnToTitle: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .testTag("settings_dialog"),
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                .size(10.dp)
                                .background(PixelGold)
                        )
                        Text(
                            text = "SYSTEM SETTINGS (16-BIT)",
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

                // Audio Section
                SettingsCategoryCard(title = "AUDIO & SOUNDSCAPE") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Master Audio Volume", color = PaleText, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            Text("${(settings.masterVolume * 100).toInt()}%", color = PixelCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                        Slider(
                            value = settings.masterVolume,
                            onValueChange = { onUpdateSettings(settings.copy(masterVolume = it)) },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = PixelCyan,
                                activeTrackColor = PixelCyan,
                                inactiveTrackColor = PixelBorderLight
                            ),
                            modifier = Modifier.testTag("slider_volume")
                        )

                        SettingsToggleRow(
                            label = "Procedural Audio Synthesizer",
                            checked = settings.audioEnabled,
                            onToggle = { onUpdateSettings(settings.copy(audioEnabled = !settings.audioEnabled)) }
                        )
                    }
                }

                // Graphics & Visual Indicators
                SettingsCategoryCard(title = "GRAPHICS & FULL-SCREEN HUD") {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SettingsToggleRow(
                            label = "On-Screen Pixel Mini-Map Radar",
                            checked = settings.showMiniMap,
                            onToggle = { onUpdateSettings(settings.copy(showMiniMap = !settings.showMiniMap)) }
                        )

                        SettingsToggleRow(
                            label = "Active Tool Status Indicator",
                            checked = settings.showActiveToolPill,
                            onToggle = { onUpdateSettings(settings.copy(showActiveToolPill = !settings.showActiveToolPill)) }
                        )

                        SettingsToggleRow(
                            label = "Climate News Alert Ticker",
                            checked = settings.showClimateTicker,
                            onToggle = { onUpdateSettings(settings.copy(showClimateTicker = !settings.showClimateTicker)) }
                        )

                        SettingsToggleRow(
                            label = "Coastal Wave Foam Animation",
                            checked = settings.coastalFoamAnimation,
                            onToggle = { onUpdateSettings(settings.copy(coastalFoamAnimation = !settings.coastalFoamAnimation)) }
                        )

                        // Chunky vs Detailed Pixel Tile Size
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val newSize = if (settings.pixelTileSize > 10f) 8f else 14f
                                    onUpdateSettings(settings.copy(pixelTileSize = newSize))
                                }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Pixel Grid Resolution", color = PaleText, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            Text(
                                text = if (settings.pixelTileSize > 10f) "CHUNKY (14px)" else "DETAILED (8px)",
                                color = PixelGold,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Gameplay & Simulation
                SettingsCategoryCard(title = "GAMEPLAY & SIMULATION") {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SettingsToggleRow(
                            label = "Auto-Pause on Cataclysms",
                            checked = settings.autoPauseOnCataclysm,
                            onToggle = { onUpdateSettings(settings.copy(autoPauseOnCataclysm = !settings.autoPauseOnCataclysm)) }
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val newUnit = if (settings.temperatureUnit == TemperatureUnit.CELSIUS) TemperatureUnit.KELVIN else TemperatureUnit.CELSIUS
                                    onUpdateSettings(settings.copy(temperatureUnit = newUnit))
                                }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Temperature Unit", color = PaleText, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            Text(
                                text = settings.temperatureUnit.symbol,
                                color = PixelCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Actions: Restart Tutorial & Return to Title
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            onDismiss()
                            onRestartTutorial()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PixelGold),
                        shape = RoundedCornerShape(3.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                            .testTag("btn_restart_tutorial")
                    ) {
                        Icon(Icons.Default.School, contentDescription = null, tint = PixelVoid, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("RESTART INTERACTIVE TUTORIAL", color = PixelVoid, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
                    }

                    Button(
                        onClick = {
                            onDismiss()
                            onReturnToTitle()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PixelPanelLight),
                        shape = RoundedCornerShape(3.dp),
                        border = BorderStroke(1.dp, PixelBorderLight),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                            .testTag("btn_return_title")
                    ) {
                        Icon(Icons.Default.Home, contentDescription = null, tint = PaleText, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("RETURN TO START MENU", color = PaleText, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsCategoryCard(
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
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                color = PixelCyan,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace
            )
            content()
        }
    }
}

@Composable
private fun SettingsToggleRow(
    label: String,
    checked: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = PaleText, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        Checkbox(
            checked = checked,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = PixelCyan,
                uncheckedColor = SubduedText
            ),
            modifier = Modifier.size(20.dp)
        )
    }
}
