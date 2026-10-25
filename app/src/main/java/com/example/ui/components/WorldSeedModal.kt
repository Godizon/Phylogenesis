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
import androidx.compose.ui.window.Dialog
import com.example.model.StarType
import com.example.ui.theme.*
import kotlin.random.Random

@Composable
fun WorldSeedModal(
    onDismiss: () -> Unit,
    onGenerateWorld: (seed: String, starType: StarType, preset: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var seed by remember { mutableStateOf("HADAL-${Random.nextInt(1000, 9999)}") }
    var selectedStar by remember { mutableStateOf(StarType.G_DWARF) }
    var selectedPreset by remember { mutableStateOf("PRIMORDIAL_BROTH") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .testTag("world_seed_modal"),
            shape = RoundedCornerShape(4.dp),
            color = PixelPanel,
            border = BorderStroke(3.dp, PixelGold),
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
                            text = "🌍 CREATE A NEW WORLD",
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

                // Seed Input with Dice Randomizer
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("WORLD CODE (same code = same world)", color = PixelCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = seed,
                            onValueChange = { seed = it },
                            modifier = Modifier.weight(1f).testTag("input_world_seed"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PixelCyan,
                                unfocusedBorderColor = PixelBorderLight,
                                focusedTextColor = PaleText,
                                unfocusedTextColor = PaleText
                            ),
                            shape = RoundedCornerShape(3.dp),
                            singleLine = true
                        )

                        Button(
                            onClick = { seed = "PHYLO-${Random.nextInt(1000, 9999)}" },
                            colors = ButtonDefaults.buttonColors(containerColor = PixelPanelLight),
                            shape = RoundedCornerShape(3.dp),
                            border = BorderStroke(1.dp, PixelGold),
                            modifier = Modifier.height(52.dp).testTag("btn_randomize_seed")
                        ) {
                            Icon(Icons.Default.Casino, contentDescription = "Randomize", tint = PixelGold)
                        }
                    }
                }

                // Star Type Selection
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("WHAT KIND OF SUN?", color = PixelCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        StarType.values().forEach { star ->
                            val isSelected = selectedStar == star
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(if (isSelected) PixelVoid else PixelPanelLight)
                                    .border(
                                        2.dp,
                                        if (isSelected) PixelGold else PixelBorderLight,
                                        RoundedCornerShape(3.dp)
                                    )
                                    .clickable { selectedStar = star }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(Color(star.colorHex))
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = star.spectralClass,
                                        color = if (isSelected) PixelGold else PaleText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }

                // Starting Climate Preset
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("HOW SHOULD LIFE START?", color = PixelCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)

                    PresetOption(
                        title = "🦠 Primordial Soup",
                        subtitle = "Hot, gassy oceans with volcanic vents. Start from scratch.",
                        isSelected = selectedPreset == "PRIMORDIAL_BROTH",
                        onSelect = { selectedPreset = "PRIMORDIAL_BROTH" }
                    )

                    PresetOption(
                        title = "🐚 Oxygen Seas",
                        subtitle = "Oxygen-rich oceans, ready for animals to burst into new shapes.",
                        isSelected = selectedPreset == "OXYGENATED_SEAS",
                        onSelect = { selectedPreset = "OXYGENATED_SEAS" }
                    )

                    PresetOption(
                        title = "👁️ Eyeball World",
                        subtitle = "One side always faces its sun. Life survives in the twilight ring.",
                        isSelected = selectedPreset == "EYEBALL_WORLD",
                        onSelect = {
                            selectedPreset = "EYEBALL_WORLD"
                            selectedStar = StarType.M_RED
                        }
                    )

                    PresetOption(
                        title = "🪨 Heavy Giant Planet",
                        subtitle = "Crushing gravity and thick air. Creatures stay low and flat.",
                        isSelected = selectedPreset == "SUPER_EARTH",
                        onSelect = { selectedPreset = "SUPER_EARTH" }
                    )

                    PresetOption(
                        title = "❄️ Snowball World",
                        subtitle = "Mostly ice. Life clings to warm volcanic oases.",
                        isSelected = selectedPreset == "SNOWBALL_WORLD",
                        onSelect = { selectedPreset = "SNOWBALL_WORLD" }
                    )
                }

                Spacer(Modifier.height(4.dp))

                // Action: Spawn World
                Button(
                    onClick = {
                        onGenerateWorld(seed, selectedStar, selectedPreset)
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("btn_spawn_world"),
                    colors = ButtonDefaults.buttonColors(containerColor = PixelCyan),
                    shape = RoundedCornerShape(3.dp),
                    border = BorderStroke(2.dp, PixelGold)
                ) {
                    Icon(Icons.Default.Public, contentDescription = null, tint = OnPrimary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "🚀 START THIS WORLD",
                        color = OnPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun PresetOption(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(3.dp))
            .background(if (isSelected) PixelVoid else PixelPanelLight)
            .border(
                2.dp,
                if (isSelected) PixelGold else PixelBorderLight,
                RoundedCornerShape(3.dp)
            )
            .clickable { onSelect() }
            .padding(8.dp)
    ) {
        Column {
            Text(
                text = title,
                color = if (isSelected) PixelGold else PaleText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = subtitle,
                color = MutedText,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
