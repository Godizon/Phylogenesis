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
        state.activeCataclysm != null -> "EXTINCTION IN PROGRESS: ${state.activeCataclysm.type.title.uppercase()}!"
        state.planetary.isSnowballEarth -> "CRYOSPHERE RUNAWAY: Global ice coverage breaches 58% equator threshold!"
        state.planetary.atmosphere.isOxidizing -> "GREAT OXIDATION EVENT: Oxygen photolysis transforming planetary chemistry."
        state.planetary.surfaceTempCelsius > 35f -> "HYPERTHERMAL GREENHOUSE: Elevated atmospheric CO₂/CH₄ driving ocean warming."
        else -> "ECOSYSTEM STEADY: ${state.globalTaxonDiversityCount} active taxa thriving in ${state.currentEpoch.title}."
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
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = "TICKER: $message",
                color = PaleText,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace,
                maxLines = 1
            )
        }
    }
}
