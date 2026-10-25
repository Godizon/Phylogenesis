package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.engine.AudioSynthesizer
import com.example.engine.SimulationEngine
import com.example.engine.WeatherEngine
import com.example.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PhylogenesisViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(SimulationEngine.createInitialState())
    val uiState: StateFlow<SimulationState> = _uiState.asStateFlow()

    private val _currentScreen = MutableStateFlow(AppScreen.START_MENU)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _gameSettings = MutableStateFlow(GameSettings())
    val gameSettings: StateFlow<GameSettings> = _gameSettings.asStateFlow()

    private val _tutorialState = MutableStateFlow(TutorialState())
    val tutorialState: StateFlow<TutorialState> = _tutorialState.asStateFlow()

    private val _showSettingsModal = MutableStateFlow(false)
    val showSettingsModal: StateFlow<Boolean> = _showSettingsModal.asStateFlow()

    private val audioSynthesizer = AudioSynthesizer()

    init {
        // Start simulation loop
        viewModelScope.launch {
            var lastTimeNanos = System.nanoTime()
            while (true) {
                val nowNanos = System.nanoTime()
                val deltaSec = ((nowNanos - lastTimeNanos) / 1_000_000_000f).coerceIn(0.010f, 0.050f)
                lastTimeNanos = nowNanos

                _uiState.update { currentState ->
                    SimulationEngine.tick(currentState, deltaSec)
                }

                delay(30) // ~33 FPS tick loop for silky smooth performance
            }
        }

        // Initialize audio synthesizer
        audioSynthesizer.start()
    }

    fun navigateToScreen(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun updateSettings(newSettings: GameSettings) {
        _gameSettings.value = newSettings
        audioSynthesizer.isEnabled = newSettings.audioEnabled
    }

    fun setShowSettingsModal(show: Boolean) {
        _showSettingsModal.value = show
    }

    // Interactive Tutorial Controller
    fun startTutorial() {
        _currentScreen.value = AppScreen.SIMULATION
        _tutorialState.update {
            it.copy(
                isActive = true,
                currentStep = TutorialStep.PLANETARY_TIME
            )
        }
    }

    fun nextTutorialStep() {
        _tutorialState.update { current ->
            if (current.currentStep.isLast) {
                current.copy(isActive = false, hasCompletedTutorial = true)
            } else {
                val nextStep = TutorialStep.values()[current.currentStep.ordinal + 1]
                current.copy(currentStep = nextStep)
            }
        }
    }

    fun prevTutorialStep() {
        _tutorialState.update { current ->
            if (current.currentStep.isFirst) {
                current
            } else {
                val prevStep = TutorialStep.values()[current.currentStep.ordinal - 1]
                current.copy(currentStep = prevStep)
            }
        }
    }

    fun skipTutorial() {
        _tutorialState.update { it.copy(isActive = false, hasCompletedTutorial = true) }
    }

    // World Seed Generator
    fun generateNewWorld(seed: String, starType: StarType, preset: String) {
        val baseState = SimulationEngine.createInitialState()
        val customPlanetary = when (preset) {
            "PRIMORDIAL_BROTH" -> baseState.planetary.copy(
                archetype = PlanetaryArchetype.STANDARD_TERRESTRIAL,
                starType = starType,
                surfaceTempCelsius = 42.0f,
                surfacePressureBar = 1.45f,
                iceAlbedoCoverage = 0.04f,
                atmosphere = AtmosphericComposition(
                    n2Percent = 76.0f,
                    o2Percent = 0.2f,
                    co2Ppm = 22000f,
                    ch4Ppm = 3800f
                )
            )
            "OXYGENATED_SEAS" -> baseState.planetary.copy(
                archetype = PlanetaryArchetype.STANDARD_TERRESTRIAL,
                starType = starType,
                surfaceTempCelsius = 19.5f,
                surfacePressureBar = 1.10f,
                iceAlbedoCoverage = 0.12f,
                atmosphere = AtmosphericComposition(
                    n2Percent = 74.0f,
                    o2Percent = 18.5f,
                    co2Ppm = 1200f,
                    ch4Ppm = 100f
                )
            )
            "SNOWBALL_WORLD" -> baseState.planetary.copy(
                archetype = PlanetaryArchetype.STANDARD_TERRESTRIAL,
                starType = starType,
                surfaceTempCelsius = -38.0f,
                surfacePressureBar = 0.95f,
                iceAlbedoCoverage = 0.75f,
                atmosphere = AtmosphericComposition(
                    n2Percent = 78.0f,
                    o2Percent = 4.0f,
                    co2Ppm = 300f,
                    ch4Ppm = 20f
                )
            )
            "EYEBALL_WORLD" -> baseState.planetary.copy(
                archetype = PlanetaryArchetype.TIDALLY_LOCKED_EYEBALL,
                starType = StarType.M_RED,
                orbitalDistanceAU = 0.14f,
                surfaceTempCelsius = 21.0f, // Twilight ribbon temperature
                surfacePressureBar = 1.35f,
                gravityG = 0.88f,
                iceAlbedoCoverage = 0.44f, // Frozen antistellar side
                atmosphere = AtmosphericComposition(
                    n2Percent = 77.0f,
                    o2Percent = 11.5f,
                    co2Ppm = 6200f,
                    ch4Ppm = 450f
                )
            )
            "SUPER_EARTH" -> baseState.planetary.copy(
                archetype = PlanetaryArchetype.SUPER_EARTH,
                starType = starType,
                surfaceTempCelsius = 27.0f,
                surfacePressureBar = 3.20f,
                gravityG = 2.5f,
                iceAlbedoCoverage = 0.08f,
                atmosphere = AtmosphericComposition(
                    n2Percent = 82.0f,
                    o2Percent = 21.0f,
                    co2Ppm = 2800f,
                    ch4Ppm = 120f
                )
            )
            else -> baseState.planetary.copy(starType = starType)
        }

        _uiState.update {
            baseState.copy(
                currentTimeGa = if (preset == "PRIMORDIAL_BROTH") 3.80f else 0.54f,
                planetary = customPlanetary,
                weather = WeatherEngine.createInitialWeather(customPlanetary),
                systemAlertMessage = "NEW SEED GENERATED: $seed (${customPlanetary.archetype.title})!"
            )
        }
    }

    fun setScale(scale: CosmicScale) {
        audioSynthesizer.currentScale = scale
        _uiState.update { it.copy(currentScale = scale) }
    }

    fun zoomIn() {
        val current = _uiState.value.currentScale
        val next = current.nextScale
        if (next != current) {
            setScale(next)
        }
    }

    fun zoomOut() {
        val current = _uiState.value.currentScale
        val prev = current.prevScale
        if (prev != current) {
            setScale(prev)
        }
    }

    fun togglePlayPause() {
        _uiState.update { it.copy(isPaused = !it.isPaused) }
    }

    fun setTimeSpeed(speedMaPerSec: Float) {
        _uiState.update {
            it.copy(
                timeSpeedMaPerSec = speedMaPerSec,
                isPaused = false
            )
        }
    }

    fun toggleMode() {
        _uiState.update {
            val newMode = if (it.mode == PlayerInterventionMode.NATURALIST) {
                PlayerInterventionMode.ARCHITECT
            } else {
                PlayerInterventionMode.NATURALIST
            }
            it.copy(mode = newMode)
        }
    }

    fun toggleBiomassHeatmap() {
        _uiState.update { it.copy(overlays = it.overlays.copy(showBiomassHeatmap = !it.overlays.showBiomassHeatmap)) }
    }

    fun toggleTrophicFlow() {
        _uiState.update { it.copy(overlays = it.overlays.copy(showTrophicFlowVectors = !it.overlays.showTrophicFlowVectors)) }
    }

    fun toggleSpeciationHotspots() {
        _uiState.update { it.copy(overlays = it.overlays.copy(showSpeciationHotspots = !it.overlays.showSpeciationHotspots)) }
    }

    fun toggleAudio() {
        _uiState.update {
            val newAudio = !it.audioEnabled
            audioSynthesizer.isEnabled = newAudio
            it.copy(audioEnabled = newAudio)
        }
    }

    fun selectClade(cladeId: String) {
        _uiState.update { state ->
            val clade = state.clades.firstOrNull { it.id == cladeId }
            state.copy(
                selectedCladeId = cladeId,
                selectedSpecimen = clade?.dominantMorphotype ?: state.selectedSpecimen
            )
        }
    }

    fun selectSpecimen(specimen: OrganismPhenotype) {
        _uiState.update {
            it.copy(
                selectedSpecimen = specimen,
                showSpecimenModal = true
            )
        }
    }

    fun triggerCataclysm(type: CataclysmType) {
        triggerHapticImpact()
        audioSynthesizer.playCataclysmStinger(type)

        _uiState.update { currentState ->
            val nextState = SimulationEngine.triggerCataclysm(currentState, type)
            if (_gameSettings.value.autoPauseOnCataclysm) {
                nextState.copy(isPaused = true)
            } else {
                nextState
            }
        }
    }

    fun mutateSelectedSpecimen() {
        val currentSpecimen = _uiState.value.selectedSpecimen ?: return
        val mutated = SimulationEngine.mutateSpecimen(currentSpecimen)
        _uiState.update { state ->
            val updatedClades = state.clades.map { clade ->
                if (clade.id == state.selectedCladeId) {
                    clade.copy(dominantMorphotype = mutated)
                } else clade
            }
            state.copy(
                selectedSpecimen = mutated,
                clades = updatedClades,
                systemAlertMessage = "NEO-FUNCTIONALIZATION: Mutagen applied to ${mutated.binomialName}!"
            )
        }
    }

    fun updateOrbitalDistance(newAU: Float) {
        _uiState.update { state ->
            state.copy(planetary = state.planetary.copy(orbitalDistanceAU = newAU))
        }
    }

    fun updateAxialTilt(newDeg: Float) {
        _uiState.update { state ->
            state.copy(planetary = state.planetary.copy(axialTiltDeg = newDeg))
        }
    }

    fun updateStellarLuminosity(newLuminosity: Float) {
        _uiState.update { state ->
            state.copy(planetary = state.planetary.copy(stellarLuminosity = newLuminosity))
        }
    }

    fun updateAtmosphere(co2Ppm: Float? = null, o2Percent: Float? = null, ch4Ppm: Float? = null) {
        _uiState.update { state ->
            val currAtmo = state.planetary.atmosphere
            val newAtmo = currAtmo.copy(
                co2Ppm = co2Ppm ?: currAtmo.co2Ppm,
                o2Percent = o2Percent ?: currAtmo.o2Percent,
                ch4Ppm = ch4Ppm ?: currAtmo.ch4Ppm
            )
            state.copy(planetary = state.planetary.copy(atmosphere = newAtmo))
        }
    }

    fun toggleVolcanism() {
        _uiState.update { state ->
            val newVolcano = if (state.planetary.volcanicActivityIndex > 0.6f) 0.2f else 0.85f
            state.copy(
                planetary = state.planetary.copy(volcanicActivityIndex = newVolcano),
                systemAlertMessage = if (newVolcano > 0.5f) "Mantle plumes breached crust! Outgassing CO₂ & SO₂" else "Volcanic activity stabilized."
            )
        }
    }

    fun warpToEpoch(epoch: GeologicalEpoch) {
        _uiState.update { state ->
            state.copy(
                currentTimeGa = (epoch.startGa + epoch.endGa) / 2f,
                systemAlertMessage = "Warped to ${epoch.title} (${epoch.majorMilestone})"
            )
        }
    }

    fun setCladogramModalVisible(visible: Boolean) {
        _uiState.update { it.copy(showCladogramModal = visible) }
    }

    fun setSpecimenModalVisible(visible: Boolean) {
        _uiState.update { it.copy(showSpecimenModal = visible) }
    }

    fun setGodToolsSheetVisible(visible: Boolean) {
        _uiState.update { it.copy(showGodToolsSheet = visible) }
    }

    fun toggleWeatherClouds() {
        _uiState.update { state ->
            val newOverlays = state.overlays.copy(showWeatherClouds = !state.overlays.showWeatherClouds)
            state.copy(
                overlays = newOverlays,
                systemAlertMessage = if (newOverlays.showWeatherClouds) "Weather Clouds Layer: Enabled" else "Weather Clouds Layer: Hidden"
            )
        }
    }

    fun toggleDayNightCycle() {
        _uiState.update { state ->
            val newOverlays = state.overlays.copy(showDayNightCycle = !state.overlays.showDayNightCycle)
            state.copy(
                overlays = newOverlays,
                systemAlertMessage = if (newOverlays.showDayNightCycle) "Day-Night Terminator: Enabled" else "Day-Night Terminator: Hidden"
            )
        }
    }

    fun setShowGeneEditorModal(visible: Boolean) {
        _uiState.update { it.copy(showGeneEditorModal = visible) }
    }

    fun injectEngineeredOrganism(organism: OrganismPhenotype) {
        _uiState.update { state ->
            val engineeredClade = Clade(
                id = "clade_" + organism.id,
                name = organism.binomialName,
                phylum = if (organism.segments.any { it.limbType == LimbType.JOINTED_WALKER }) PhylumCategory.ARTHROPODA_EQUIV else PhylumCategory.CHORDATA_EQUIV,
                trophicLevel = if (organism.activeCodons.contains(CodonType.RUBISCO)) TrophicLevel.PRIMARY_PRODUCER else TrophicLevel.PRIMARY_CONSUMER,
                speciationDateGa = state.currentTimeGa,
                biomassTons = 1.2e8,
                populationCount = 4.5e7,
                averageLifespanYears = 4.2f,
                meanFitness = 0.88f,
                thermalToleranceMinC = -5f,
                thermalToleranceMaxC = 45f,
                oxygenRequirementPercent = 6.0f,
                dominantMorphotype = organism
            )
            val updatedClades = state.clades.toMutableList().apply {
                add(0, engineeredClade)
            }
            state.copy(
                clades = updatedClades,
                selectedCladeId = engineeredClade.id,
                selectedSpecimen = organism,
                showGeneEditorModal = false,
                systemAlertMessage = "ORGANISM SYNTHESIZED: ${organism.binomialName} released into active biosphere!"
            )
        }
    }

    fun dismissAlertMessage() {
        _uiState.update { it.copy(systemAlertMessage = null) }
    }

    private fun triggerHapticImpact() {
        try {
            val context = getApplication<Application>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(300)
                }
            }
        } catch (_: Exception) {}
    }

    override fun onCleared() {
        super.onCleared()
        audioSynthesizer.stop()
    }
}
