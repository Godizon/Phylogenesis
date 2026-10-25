package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.model.AppScreen
import com.example.model.SimulationState
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewport.SimulationViewport
import com.example.viewmodel.PhylogenesisViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhylogenesisScreen(
    viewModel: PhylogenesisViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val settings by viewModel.gameSettings.collectAsState()
    val tutorialState by viewModel.tutorialState.collectAsState()
    val showSettingsModal by viewModel.showSettingsModal.collectAsState()

    var showLeftTelemetry by remember { mutableStateOf(false) }
    var showRightClades by remember { mutableStateOf(false) }

    // System Back Handler for closing drawers/modals correctly
    BackHandler(
        enabled = showLeftTelemetry || showRightClades || state.showCladogramModal ||
                state.showSpecimenModal || state.showGeneEditorModal || state.showGodToolsSheet ||
                showSettingsModal || tutorialState.isActive
    ) {
        when {
            tutorialState.isActive -> viewModel.skipTutorial()
            showSettingsModal -> viewModel.setShowSettingsModal(false)
            state.showGodToolsSheet -> viewModel.setGodToolsSheetVisible(false)
            state.showGeneEditorModal -> viewModel.setShowGeneEditorModal(false)
            state.showSpecimenModal -> viewModel.setSpecimenModalVisible(false)
            state.showCladogramModal -> viewModel.setCladogramModalVisible(false)
            showLeftTelemetry -> showLeftTelemetry = false
            showRightClades -> showRightClades = false
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("phylogenesis_main_screen"),
        containerColor = PixelVoid,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Master HUD
                TopHudBar(
                    state = state,
                    onTogglePlayPause = { viewModel.togglePlayPause() },
                    onSetSpeed = { viewModel.setTimeSpeed(it) },
                    onToggleMode = { viewModel.toggleMode() },
                    onToggleAudio = { viewModel.toggleAudio() },
                    onOpenCladogram = { viewModel.setCladogramModalVisible(true) },
                    onOpenSettings = { viewModel.setShowSettingsModal(true) }
                )

                // Middle Viewport and Drawer Layer
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    // Central Interactive Viewport
                    SimulationViewport(
                        state = state,
                        onZoomIn = { viewModel.zoomIn() },
                        onZoomOut = { viewModel.zoomOut() },
                        onSelectSpecimen = { viewModel.selectSpecimen(it) },
                        modifier = Modifier.fillMaxSize()
                    )

                    // On-Screen Visual Indicators (Full-screen immersion, non-overlapping spacing)
                    // Top: Climate Alert Ticker
                    if (settings.showClimateTicker) {
                        ClimateAlertTicker(
                            state = state,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(horizontal = 48.dp, vertical = 2.dp)
                        )
                    }

                    // Top Left: Active Tool Indicator
                    if (settings.showActiveToolPill) {
                        ActiveToolIndicator(
                            mode = state.mode,
                            activeCataclysm = state.activeCataclysm,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(top = 44.dp, start = 12.dp)
                        )
                    }

                    // Bottom Left: On-Screen Pixel Radar Mini-Map
                    if (settings.showMiniMap) {
                        PixelMiniMap(
                            planetary = state.planetary,
                            activeCataclysm = state.activeCataclysm,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(start = 12.dp, bottom = 10.dp)
                        )
                    }

                    // Quick Weather & Day-Night Layer Toggles (Floating right-center bar)
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Weather Cloud Toggle
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (state.overlays.showWeatherClouds) PixelPanelLight else PixelPanel)
                                .border(2.dp, if (state.overlays.showWeatherClouds) PixelCyan else MutedText, RoundedCornerShape(4.dp))
                                .clickable { viewModel.toggleWeatherClouds() }
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cloud,
                                contentDescription = "Toggle Drifting Clouds",
                                tint = if (state.overlays.showWeatherClouds) PixelCyan else MutedText,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Day-Night Terminator Toggle
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (state.overlays.showDayNightCycle) PixelPanelLight else PixelPanel)
                                .border(2.dp, if (state.overlays.showDayNightCycle) PixelGold else MutedText, RoundedCornerShape(4.dp))
                                .clickable { viewModel.toggleDayNightCycle() }
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.WbSunny,
                                contentDescription = "Toggle Day-Night Shadow",
                                tint = if (state.overlays.showDayNightCycle) PixelGold else MutedText,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Left Telemetry Drawer Overlay
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showLeftTelemetry,
                        enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
                        exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(),
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        PlanetaryDrawer(
                            planetary = state.planetary,
                            onToggleVolcanism = { viewModel.toggleVolcanism() }
                        )
                    }

                    // Right Clade & Ecosystem Drawer Overlay
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showRightClades,
                        enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                        exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        CladeDrawer(
                            state = state,
                            onSelectClade = { viewModel.selectClade(it) },
                            onInspectSpecimen = { viewModel.setSpecimenModalVisible(true) },
                            onToggleBiomassHeatmap = { viewModel.toggleBiomassHeatmap() },
                            onToggleTrophicFlow = { viewModel.toggleTrophicFlow() },
                            onToggleSpeciationHotspots = { viewModel.toggleSpeciationHotspots() }
                        )
                    }

                    // Floating Drawer Toggle Buttons
                    // Left drawer toggle (Planetary Telemetry)
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = if (showLeftTelemetry) 286.dp else 8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(PixelPanelLight)
                            .border(2.dp, PixelCyan, RoundedCornerShape(4.dp))
                            .clickable { showLeftTelemetry = !showLeftTelemetry }
                            .padding(7.dp)
                            .testTag("toggle_left_telemetry")
                    ) {
                        Icon(
                            imageVector = if (showLeftTelemetry) Icons.Default.ChevronLeft else Icons.Default.Public,
                            contentDescription = "Toggle Planetary Telemetry",
                            tint = PixelCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Right drawer toggle (Clade & Ecosystem)
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = if (showRightClades) 286.dp else 8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(PixelPanelLight)
                            .border(2.dp, PixelEmerald, RoundedCornerShape(4.dp))
                            .clickable { showRightClades = !showRightClades }
                            .padding(7.dp)
                            .testTag("toggle_right_clades")
                    ) {
                        Icon(
                            imageVector = if (showRightClades) Icons.Default.ChevronRight else Icons.Default.Hub,
                            contentDescription = "Toggle Clade & Ecosystem",
                            tint = PixelEmerald,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // System Alert Banner (Pixel style)
                    state.systemAlertMessage?.let { alert ->
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 40.dp)
                                .fillMaxWidth(0.92f)
                                .clip(RoundedCornerShape(4.dp))
                                .background(PixelCrimson)
                                .border(2.dp, PixelGold, RoundedCornerShape(4.dp))
                                .clickable { viewModel.dismissAlertMessage() }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag("system_alert_banner")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = alert,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Bottom Timeline & Tool Matrix Dock
                TimelineBar(
                    state = state,
                    onWarpToEpoch = { viewModel.warpToEpoch(it) },
                    onSelectScale = { viewModel.setScale(it) },
                    onOpenGodTools = { viewModel.setGodToolsSheetVisible(true) },
                    onOpenCladogram = { viewModel.setCladogramModalVisible(true) },
                    onOpenGeneEditor = { viewModel.setShowGeneEditorModal(true) }
                )
            }

            // Cladogram Phylogenetic Tree Modal
            if (state.showCladogramModal) {
                CladogramModal(
                    onDismiss = { viewModel.setCladogramModalVisible(false) }
                )
            }

            // Specimen Anatomical X-Ray Modal
            if (state.showSpecimenModal) {
                SpecimenInspector(
                    specimen = state.selectedSpecimen,
                    onDismiss = { viewModel.setSpecimenModalVisible(false) },
                    onMutate = { viewModel.mutateSelectedSpecimen() },
                    onOpenGeneEditor = { viewModel.setShowGeneEditorModal(true) }
                )
            }

            // Interactive vDNA Gene Lab Modal
            if (state.showGeneEditorModal) {
                GeneEditorModal(
                    planetary = state.planetary,
                    initialSpecimen = state.selectedSpecimen,
                    onDismiss = { viewModel.setShowGeneEditorModal(false) },
                    onInjectOrganism = { viewModel.injectEngineeredOrganism(it) }
                )
            }

            // God-Tools Demiurge Bottom Sheet
            if (state.showGodToolsSheet) {
                GodToolPanel(
                    state = state,
                    onDismiss = { viewModel.setGodToolsSheetVisible(false) },
                    onUpdateOrbit = { viewModel.updateOrbitalDistance(it) },
                    onUpdateTilt = { viewModel.updateAxialTilt(it) },
                    onUpdateLuminosity = { viewModel.updateStellarLuminosity(it) },
                    onInjectGas = { co2, o2, ch4 -> viewModel.updateAtmosphere(co2, o2, ch4) },
                    onToggleVolcanism = { viewModel.toggleVolcanism() },
                    onMutateSpecimen = { viewModel.mutateSelectedSpecimen() },
                    onTriggerCataclysm = { viewModel.triggerCataclysm(it) }
                )
            }

            // Settings Modal
            if (showSettingsModal) {
                SettingsDialog(
                    settings = settings,
                    onUpdateSettings = { viewModel.updateSettings(it) },
                    onRestartTutorial = {
                        viewModel.setShowSettingsModal(false)
                        viewModel.startTutorial()
                    },
                    onReturnToTitle = {
                        viewModel.setShowSettingsModal(false)
                        viewModel.navigateToScreen(AppScreen.START_MENU)
                    },
                    onDismiss = { viewModel.setShowSettingsModal(false) }
                )
            }

            // Interactive Spotlight Tutorial Overlay
            if (tutorialState.isActive) {
                TutorialSpotlightOverlay(
                    tutorialState = tutorialState,
                    onNextStep = { viewModel.nextTutorialStep() },
                    onPrevStep = { viewModel.prevTutorialStep() },
                    onSkipTutorial = { viewModel.skipTutorial() }
                )
            }
        }
    }
}
