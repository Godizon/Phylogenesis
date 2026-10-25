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
import com.example.engine.SimulationEngine
import com.example.model.CladogramNode
import com.example.ui.theme.*

@Composable
fun CladogramModal(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val treeNodes = remember { SimulationEngine.buildCladogramTree() }
    var selectedNode by remember { mutableStateOf(treeNodes.first()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .testTag("cladogram_modal"),
            shape = RoundedCornerShape(4.dp),
            color = PixelPanel,
            border = BorderStroke(3.dp, PixelBorderLight),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                .size(12.dp)
                                .background(PixelCyan)
                        )
                        Text(
                            text = "TREE OF LIFE (16-BIT CLADOGRAM)",
                            color = PixelGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = MutedText)
                    }
                }

                // Subtitle
                Text(
                    text = "Speciation branches spanning 4.0 Ga of planetary evolution. Extinct lineages fade to slate-gray.",
                    color = MutedText,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )

                // Scrollable Tree Lineages
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(PixelVoid)
                        .border(2.dp, PixelBorderLight, RoundedCornerShape(4.dp))
                        .padding(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        treeNodes.forEach { node ->
                            PixelCladogramNodeRow(
                                node = node,
                                isSelected = selectedNode.id == node.id,
                                onSelect = { selectedNode = node }
                            )
                        }
                    }
                }

                // Ancestral Morph Preview Card (Selected Node)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = PixelPanelLight),
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(2.dp, Color(selectedNode.category.colorHex))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedNode.name,
                                color = Color(selectedNode.category.colorHex),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = if (selectedNode.timeGa >= 1.0f) "${selectedNode.timeGa} Ga" else "${(selectedNode.timeGa * 1000).toInt()} Ma",
                                color = PixelGold,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = selectedNode.description,
                            color = PaleText,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "ANATOMICAL INNOVATION:",
                                color = PixelCyan,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = selectedNode.anatomicalInnovation,
                                color = PaleText,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PixelCladogramNodeRow(
    node: CladogramNode,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val indent = (node.depth * 16).dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = indent)
            .clip(RoundedCornerShape(3.dp))
            .background(if (isSelected) PixelCyan.copy(alpha = 0.25f) else Color.Transparent)
            .border(1.dp, if (isSelected) PixelGold else Color.Transparent, RoundedCornerShape(3.dp))
            .clickable { onSelect() }
            .padding(horizontal = 6.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(if (node.isExtinct) SubduedText else Color(node.category.colorHex))
            )
            Text(
                text = node.name,
                color = if (node.isExtinct) SubduedText else PaleText,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontFamily = FontFamily.Monospace
            )
        }

        if (node.isExtinct) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(2.dp))
                    .background(PixelCrimson.copy(alpha = 0.25f))
                    .border(1.dp, PixelCrimson, RoundedCornerShape(2.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(
                    text = "FOSSIL",
                    color = PixelCrimson,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
