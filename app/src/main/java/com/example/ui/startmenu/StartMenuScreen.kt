package com.example.ui.startmenu

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.example.model.GameSettings
import com.example.model.StarType
import com.example.ui.components.SettingsDialog
import com.example.ui.components.WorldSeedModal
import com.example.ui.theme.*
import kotlin.math.sin

@Composable
fun StartMenuScreen(
    onContinue: () -> Unit,
    onStartTutorial: () -> Unit,
    onGenerateWorld: (seed: String, starType: StarType, preset: String) -> Unit,
    settings: GameSettings,
    onUpdateSettings: (GameSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSeedModal by remember { mutableStateOf(false) }
    var showSettingsModal by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "title_anim")
    val animTick by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "title_tick"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PixelVoid)
            .testTag("start_menu_screen")
    ) {
        // Animated 16-Bit Pixel Background with Floating Spores & Continental Tiles
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val p = 16f

            // Drifting continental pixel blocks
            for (i in 0 until 18) {
                val bx = ((i * 80f + animTick * 15f) % (w + 100f)) - 50f
                val by = (h * 0.15f + sin(animTick * 0.1f + i) * (h * 0.7f))
                val qx = (bx / p).toInt() * p
                val qy = (by / p).toInt() * p
                val color = if (i % 3 == 0) PixelForest else if (i % 2 == 0) PixelGrass else PixelShallowOcean

                drawRect(color.copy(alpha = 0.45f), Offset(qx, qy), Size(p * 2, p * 2))
            }

            // Floating luminous bioluminescent pixel spores
            for (j in 0 until 24) {
                val sx = ((j * 45f + animTick * 8f) % w)
                val sy = ((j * 32f + sin(animTick * 0.15f + j) * 40f) % h)
                drawRect(PixelCyan.copy(alpha = 0.6f), Offset(sx, sy), Size(4f, 4f))
            }
        }

        // Center: Retro Title & Main Action Buttons
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(24.dp)
                .widthIn(max = 380.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title Banner
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Pixelated Top Icon Badge
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(PixelPanel)
                        .border(2.dp, PixelGold, RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Public,
                        contentDescription = null,
                        tint = PixelCyan,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "PHYLOGENESIS",
                    color = PixelGold,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp
                )

                Text(
                    text = "A DETERMINISTIC EVOLUTIONARY SANDBOX",
                    color = PixelCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )

                Text(
                    text = "16-BIT TILE WORLD • EVO-DEVO GRN • THERMODYNAMICS",
                    color = MutedText,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(Modifier.height(8.dp))

            // Action Buttons (WorldBox Chunky Pixel Frames)
            PixelMenuButton(
                text = "CONTINUE SIMULATION",
                icon = Icons.Default.PlayArrow,
                primaryColor = PixelEmerald,
                onClick = onContinue,
                testTag = "btn_menu_continue"
            )

            PixelMenuButton(
                text = "NEW SIMULATION / SEED",
                icon = Icons.Default.Casino,
                primaryColor = PixelGold,
                onClick = { showSeedModal = true },
                testTag = "btn_menu_new_seed"
            )

            PixelMenuButton(
                text = "INTERACTIVE TUTORIAL",
                icon = Icons.Default.School,
                primaryColor = PixelCyan,
                onClick = onStartTutorial,
                testTag = "btn_menu_tutorial"
            )

            PixelMenuButton(
                text = "SETTINGS & DISPLAY",
                icon = Icons.Default.Settings,
                primaryColor = PixelPurple,
                onClick = { showSettingsModal = true },
                testTag = "btn_menu_settings"
            )
        }

        // Bottom Footer
        Text(
            text = "PHYLOGENESIS v2.0 • BUILT WITH JETPACK COMPOSE",
            color = SubduedText,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
        )

        // World Seed Config Modal
        if (showSeedModal) {
            WorldSeedModal(
                onDismiss = { showSeedModal = false },
                onGenerateWorld = { seed, star, preset ->
                    onGenerateWorld(seed, star, preset)
                    onContinue()
                }
            )
        }

        // Settings Dialog
        if (showSettingsModal) {
            SettingsDialog(
                settings = settings,
                onUpdateSettings = onUpdateSettings,
                onRestartTutorial = {
                    showSettingsModal = false
                    onStartTutorial()
                },
                onReturnToTitle = { showSettingsModal = false },
                onDismiss = { showSettingsModal = false }
            )
        }
    }
}

@Composable
private fun PixelMenuButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    primaryColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag(testTag),
        colors = ButtonDefaults.buttonColors(containerColor = PixelPanel),
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(2.dp, primaryColor)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = primaryColor,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = text,
                color = PaleText,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.5.sp
            )
        }
    }
}
