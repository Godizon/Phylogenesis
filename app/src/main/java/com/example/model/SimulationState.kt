package com.example.model

/**
 * Geological Epochs spanning the 4.0-billion-year planetary history.
 */
enum class GeologicalEpoch(
    val title: String,
    val rangeStr: String,
    val startGa: Float,
    val endGa: Float,
    val majorMilestone: String
) {
    HADEAN("Hadean / Eoarchean", "4.00 – 3.80 Ga", 4.00f, 3.80f, "Hydrothermal Abiogenesis & LUCA"),
    ARCHEAN("Archean Eon", "3.80 – 2.50 Ga", 3.80f, 2.50f, "Chemolithoautotrophy & Microbial Mats"),
    PROTEROZOIC("Proterozoic Eon", "2.50 – 0.54 Ga", 2.50f, 0.54f, "Great Oxidation Event & Eukaryogenesis"),
    PALEOZOIC("Paleozoic Era", "541 – 252 Ma", 0.541f, 0.252f, "Cambrian Explosion & Land Conquest"),
    MESOZOIC("Mesozoic Era", "252 – 66 Ma", 0.252f, 0.066f, "Reptilian Diversification & Gymnosperms"),
    CENOZOIC("Cenozoic Era", "66 – Present", 0.066f, 0.000f, "Macrofauna Radiations & Grasslands");

    companion object {
        fun fromTime(timeGa: Float): GeologicalEpoch {
            return when {
                timeGa >= 3.80f -> HADEAN
                timeGa >= 2.50f -> ARCHEAN
                timeGa >= 0.541f -> PROTEROZOIC
                timeGa >= 0.252f -> PALEOZOIC
                timeGa >= 0.066f -> MESOZOIC
                else -> CENOZOIC
            }
        }
    }
}

/**
 * Player role mode.
 */
enum class PlayerInterventionMode(val label: String, val description: String) {
    NATURALIST("Naturalist Mode", "Passive observation: witness emergent thermodynamic evolution across millions of years"),
    ARCHITECT("Architect Mode", "Active Demiurge: manipulate planetary dials, mutate genomes, and unleash cataclysms")
}

/**
 * Active Viewport Overlays.
 */
data class ViewportOverlays(
    val showBiomassHeatmap: Boolean = true,
    val showTrophicFlowVectors: Boolean = true,
    val showSpeciationHotspots: Boolean = true,
    val showAtmosphericVortices: Boolean = true,
    val showWeatherClouds: Boolean = true,
    val showDayNightCycle: Boolean = true
)

/**
 * Cataclysm visual status on the active viewport.
 */
data class ActiveCataclysm(
    val type: CataclysmType,
    val startedAtTimeGa: Float,
    val durationSeconds: Float = 6.0f,
    val elapsedSeconds: Float = 0f,
    val shockwaveRadiusPercent: Float = 0f,
    val casualtiesTons: Double = 0.0
)


enum class OrganismBehavior(val label: String, val emoji: String) {
    CRUISING("Cruising & Patrolling", "🏊"),
    GRAZING("Benthic Grazing", "🌿"),
    HUNTING("Stalking Prey", "🎯"),
    RESTING("Resting on Seafloor", "💤"),
    SCHOOLING("Schooling Wave", "🐟"),
    FLEEING_CATACLYSM("Fleeing Cataclysm", "⚡"),
    THERMAL_STRESS("Thermal Stress Shock", "🔥"),
    HYPOXIA_GASP("Hypoxic Gasping", "🫧"),
    RADIATION_MUTATING("Cosmic Mutating", "🧬"),
    BURROWING_SHELTER("Burrowing for Safety", "🛡️"),
    OXYGEN_GIGANTISM("Oxygenic Gigantism", "🦕")
}

data class LivingOrganism(
    val id: String,
    val cladeId: String,
    val commonName: String,
    val binomialName: String,
    val phylum: String,
    val trophicLevel: TrophicLevel,
    val emoji: String,
    val phenotype: OrganismPhenotype,
    val xRatio: Float, // Normalized X position (0.05 to 0.95)
    val yRatio: Float, // Normalized Y position (0.15 to 0.85)
    val headingRad: Float = 0f, // Movement heading angle in radians
    val speed: Float = 0.04f, // Movement speed ratio per second
    val spriteScale: Float = 1.0f, // Visual sprite scale multiplier
    val behavior: OrganismBehavior = OrganismBehavior.CRUISING,
    val behaviorTimer: Float = 3.5f,
    val health: Float = 1.0f,
    val energy: Float = 0.9f,
    val swimPhase: Float = 0f,
    val adaptationNote: String = "Normal Baseline",
    val isStressed: Boolean = false,
    val stressReason: String? = null
)

/**
 * Global game state for Phylogenesis.
 */
data class SimulationState(
    val currentTimeGa: Float = 3.824f, // Geological time in Giga-annum (Ga)
    val timeSpeedMaPerSec: Float = 10.0f, // Mega-annum (Ma) per second
    val isPaused: Boolean = false,
    val mode: PlayerInterventionMode = PlayerInterventionMode.NATURALIST,
    val currentScale: CosmicScale = CosmicScale.COSMIC,
    val planetary: PlanetaryState = PlanetaryState(),
    val weather: WeatherState = WeatherState(),
    val clades: List<Clade> = emptyList(),
    val livingOrganisms: List<LivingOrganism> = emptyList(),
    val selectedCladeId: String? = null,
    val selectedOrganismId: String? = null,
    val selectedSpecimen: OrganismPhenotype? = null,
    val overlays: ViewportOverlays = ViewportOverlays(),
    val activeCataclysm: ActiveCataclysm? = null,
    val audioEnabled: Boolean = true,
    val showCladogramModal: Boolean = false,
    val showSpecimenModal: Boolean = false,
    val showGodToolsSheet: Boolean = false,
    val showGeneEditorModal: Boolean = false,
    val showCelestialModal: Boolean = false,
    val totalBiomassTons: Double = 1.42e12,
    val globalTaxonDiversityCount: Int = 142,
    val systemAlertMessage: String? = null
) {
    val currentEpoch: GeologicalEpoch get() = GeologicalEpoch.fromTime(currentTimeGa)

    val selectedClade: Clade?
        get() = clades.firstOrNull { it.id == selectedCladeId } ?: clades.firstOrNull()

    val selectedOrganism: LivingOrganism?
        get() = livingOrganisms.firstOrNull { it.id == selectedOrganismId }

    val formattedTime: String
        get() = if (currentTimeGa >= 1.0f) {
            String.format("%.3f Ga", currentTimeGa)
        } else {
            String.format("%.1f Ma", currentTimeGa * 1000f)
        }

    val formattedBiomass: String
        get() = String.format("%.2e t", totalBiomassTons)
}
