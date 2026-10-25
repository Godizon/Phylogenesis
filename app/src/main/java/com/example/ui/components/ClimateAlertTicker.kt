package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import com.example.model.SimulationState
import com.example.ui.theme.*

/**
 * WorldBox-style retro ticker reporting biospheric events.
 */
@Composable
fun ClimateAlertTicker(
    state: SimulationState,
    modifier: Modifier = Modifier
) {
    val message = when {
        state.activeCataclysm != null -> {
            val f = state.activeCataclysm.type.friendly
            "${f.emoji} ${f.name.uppercase()}! Life is in danger!"
        }
        state.planetary.isSnowballEarth -> "❄️ The whole planet is freezing over!"
        state.planetary.atmosphere.isOxidizing -> "🫧 Oxygen is filling the air. Life is changing fast!"
        state.planetary.surfaceTempCelsius > 35f -> "🔥 The planet is overheating. Oceans are getting warm."
        else -> "🌿 All calm: ${state.globalTaxonDiversityCount} species living in the ${state.currentEpoch.friendlyName}."
    }

    val bannerColor = when {
        state.activeCataclysm != null -> PixelCrimson
        state.planetary.isSnowballEarth -> Color(0xFF1D3557)
        state.planetary.atmosphere.isOxidizing -> PixelEmerald
        else -> PixelPanelLight
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(3.dp))
            .background(PixelVoid.copy(alpha = 0.90f))
            .border(1.dp, bannerColor, RoundedCornerShape(3.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
            .testTag("climate_alert_ticker")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = PixelGold,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = message,
                color = PaleText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2
            )
        }
    }
}
