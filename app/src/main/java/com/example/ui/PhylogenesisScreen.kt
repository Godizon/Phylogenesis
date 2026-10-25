package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
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
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import com.example.model.CataclysmType
import kotlin.random.Random

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
    var infoEntry by remember { mutableStateOf<InfoEntry?>(null) }
    var showTimeTravel by remember { mutableStateOf(false) }
    var coachDismissed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(20_000); coachDismissed = true }

    // ---- Disaster effects: screen shake, color flash, and a "what just happened" result card ----
    var fxType by remember { mutableStateOf<CataclysmType?>(null) }
    var fxBefore by remember { mutableStateOf(0.0) }
    var fxResult by remember { mutableStateOf<String?>(null) }
    var shakeX by remember { mutableStateOf(0f) }
    var shakeY by remember { mutableStateOf(0f) }
    val latestState by rememberUpdatedState(state)
    val flashAlpha by animateFloatAsState(
        targetValue = if (fxType != null) 0.38f else 0f,
        animationSpec = androidx.compose.animation.core.tween(350),
        label = "disaster_flash"
    )
    val fireDisaster: (CataclysmType) -> Unit = { type ->
        fxBefore = state.totalBiomassTons
        fxResult = null
        fxType = type
        viewModel.triggerCataclysm(type)
    }
    LaunchedEffect(fxType) {
        val type = fxType ?: return@LaunchedEffect
        val steps = 40
        for (i in 0 until steps) {
            val strength = 26f * (1f - i / steps.toFloat())
            shakeX = (Random.nextFloat() - 0.5f) * strength
            shakeY = (Random.nextFloat() - 0.5f) * strength
            delay(35)
        }
        shakeX = 0f; shakeY = 0f
        val before = fxBefore
        val after = latestState.totalBiomassTons
        val lost = if (before > 0.0) ((before - after) / before * 100.0).coerceIn(0.0, 100.0) else 0.0
        val f = type.friendly
        fxType = null
        fxResult = "${f.emoji} ${f.name}!  About ${lost.toInt()}% of all life was wiped out.\n\n${type.evolutionaryConsequence}"
    }

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
        var immersiveHideHud by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .graphicsLayer { translationX = shakeX; translationY = shakeY }
        ) {
            // 1. Full-Screen Immersive Simulation Viewport (Maximum viewing area)
            SimulationViewport(
                state = state,
                onZoomIn = { viewModel.zoomIn() },
                onZoomOut = { viewModel.zoomOut() },
                onSelectSpecimen = { coachDismissed = true; viewModel.selectSpecimen(it) },
                modifier = Modifier.fillMaxSize()
            )

            // 2. Floating Overlays (Minimap, Weather Toggles, Active Tool, Climate Ticker)
            // Climate Alert Ticker
            if (!immersiveHideHud && settings.showClimateTicker) {
                ClimateAlertTicker(
                    state = state,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(start = 56.dp, end = 56.dp, top = 118.dp)
                )
            }

            // Bottom Left: On-Screen Pixel Radar Mini-Map
            if (!immersiveHideHud && settings.showMiniMap) {
                PixelMiniMap(
                    planetary = state.planetary,
                    activeCataclysm = state.activeCataclysm,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 12.dp, bottom = 124.dp)
                )
            }

            // Quick Weather & Day-Night Layer Toggles & Immersive Toggle (Floating right-center bar)
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(y = 84.dp)
                    .padding(end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Immersive Mode / Fullscreen HUD Hide Toggle
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (immersiveHideHud) PixelPanelLight else PixelPanel)
                        .border(2.dp, if (immersiveHideHud) PixelGold else PixelBorderLight, RoundedCornerShape(4.dp))
                        .clickable { immersiveHideHud = !immersiveHideHud }
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (immersiveHideHud) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Toggle Immersive Viewport (Hide/Show HUD)",
                        tint = if (immersiveHideHud) PixelGold else PixelCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }

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
                modifier = Modifier.align(Alignment.CenterStart).padding(top = 60.dp, bottom = 60.dp)
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
                modifier = Modifier.align(Alignment.CenterEnd).padding(top = 60.dp, bottom = 60.dp)
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
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (showLeftTelemetry) "◀" else "🌍", fontSize = 20.sp, color = PixelCyan)
                    if (!showLeftTelemetry) Text("Planet", color = PixelCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(y = (-40).dp)
                    .padding(end = if (showRightClades) 286.dp else 8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(PixelPanelLight)
                    .border(2.dp, PixelEmerald, RoundedCornerShape(4.dp))
                    .clickable { showRightClades = !showRightClades }
                    .padding(7.dp)
                    .testTag("toggle_right_clades")
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (showRightClades) "▶" else "🐾", fontSize = 20.sp, color = PixelEmerald)
                    if (!showRightClades) Text("Life", color = PixelEmerald, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            // System Alert Banner
            state.systemAlertMessage?.let { alert ->
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 118.dp)
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

            // 3. Floating Top Master HUD (if not hidden)
            androidx.compose.animation.AnimatedVisibility(
                visible = !immersiveHideHud,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                TopHudBar(
                    state = state,
                    onTogglePlayPause = { viewModel.togglePlayPause() },
                    onSetSpeed = { viewModel.setTimeSpeed(it) },
                    onToggleAudio = { viewModel.toggleAudio() },
                    onOpenSettings = { viewModel.setShowSettingsModal(true) },
                    onShowInfo = { infoEntry = it }
                )
            }

            // 4. Floating Bottom Timeline & Tool Matrix Dock (if not hidden)
            androidx.compose.animation.AnimatedVisibility(
                visible = !immersiveHideHud,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                PowersDock(
                    onTriggerCataclysm = fireDisaster,
                    onOpenGeneLab = { viewModel.setShowGeneEditorModal(true) },
                    onOpenTree = { viewModel.setCladogramModalVisible(true) },
                    onOpenTimeTravel = { showTimeTravel = true },
                    onOpenPlanetControls = { viewModel.setGodToolsSheetVisible(true) },
                    onShowInfo = { infoEntry = it }
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
                    onTriggerCataclysm = fireDisaster
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

            // Disaster flash + big emoji
            if (flashAlpha > 0.01f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background((fxType?.let { Color(it.colorHex) } ?: Color.White).copy(alpha = flashAlpha)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(fxType?.friendly?.emoji ?: "", fontSize = 110.sp)
                }
            }

            // What-just-happened card
            fxResult?.let { msg ->
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 24.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(PixelPanel)
                        .border(2.dp, PixelCrimson, RoundedCornerShape(18.dp))
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(msg, color = PaleText, fontSize = 15.sp, lineHeight = 21.sp)
                    Text(
                        if (state.isPaused) "▶ Keep going" else "OK",
                        color = OnPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(PixelCyan)
                            .clickable {
                                fxResult = null
                                if (state.isPaused) viewModel.togglePlayPause()
                            }
                            .padding(vertical = 10.dp)
                    )
                }
            }

            // First-time coach hint
            if (!immersiveHideHud && !coachDismissed && !tutorialState.isActive) {
                CoachHint(
                    onClick = { coachDismissed = true; infoEntry = Glossary.creature },
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 128.dp)
                )
            }

            if (showTimeTravel) {
                TimeTravelDialog(
                    current = state.currentEpoch,
                    onPick = { viewModel.warpToEpoch(it) },
                    onDismiss = { showTimeTravel = false }
                )
            }

            infoEntry?.let { InfoDialog(entry = it, onDismiss = { infoEntry = null }) }

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
