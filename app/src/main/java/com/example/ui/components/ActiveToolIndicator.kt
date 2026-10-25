package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ActiveCataclysm
import com.example.model.PlayerInterventionMode
import com.example.ui.theme.*

/**
 * On-screen indicator pill showing currently active god power or observation mode.
 */
@Composable
fun ActiveToolIndicator(
    mode: PlayerInterventionMode,
    activeCataclysm: ActiveCataclysm?,
    modifier: Modifier = Modifier
) {
    val isArchitect = mode == PlayerInterventionMode.ARCHITECT
    val borderColor = if (activeCataclysm != null) PixelCrimson else if (isArchitect) PixelPurple else PixelEmerald
    val bgColor = PixelVoid.copy(alpha = 0.88f)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .border(2.dp, borderColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag("active_tool_indicator")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = if (activeCataclysm != null) Icons.Default.Warning
                else if (isArchitect) Icons.Default.Bolt
                else Icons.Default.Visibility,
                contentDescription = null,
                tint = borderColor,
                modifier = Modifier.size(14.dp)
            )

            Text(
                text = if (activeCataclysm != null) "CATACLYSM: ${activeCataclysm.type.title.uppercase()}"
                else if (isArchitect) "ARCHITECT: DEMIURGE ARMED"
                else "NATURALIST: WATCHING EMERGENCE",
                color = PaleText,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
