package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.*

/** Friendly "what is this?" popup. Used everywhere instead of making players Google things. */
@Composable
fun InfoDialog(entry: InfoEntry, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(PixelPanel)
                .border(2.dp, PixelGold, RoundedCornerShape(18.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(entry.emoji, fontSize = 34.sp)
                Text(entry.title, color = PixelGold, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            }
            Text(entry.plain, color = PaleText, fontSize = 15.sp, lineHeight = 21.sp)
            entry.funFact?.let {
                Text(
                    "💡 $it",
                    color = MutedText,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(PixelVoid)
                        .padding(10.dp)
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.End)
                    .clip(RoundedCornerShape(12.dp))
                    .background(PixelCyan)
                    .clickable { onDismiss() }
                    .padding(horizontal = 22.dp, vertical = 10.dp)
            ) {
                Text("Got it!", color = OnPrimary, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

/** A readable, tappable stat tile: emoji + small label + big value. Tap = explain. */
@Composable
fun StatChip(
    emoji: String,
    label: String,
    value: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(PixelVoid.copy(alpha = 0.85f))
            .border(1.5.dp, accent.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(emoji, fontSize = 18.sp)
        Column {
            Text(label, color = MutedText, fontSize = 10.sp, maxLines = 1)
            Text(value, color = accent, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
        }
    }
}
