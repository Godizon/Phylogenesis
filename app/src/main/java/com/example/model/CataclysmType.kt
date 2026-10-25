package com.example.model

/**
 * Mass extinction cataclysm scenarios from the game design doc.
 */
enum class CataclysmType(
    val title: String,
    val subtitle: String,
    val triggerName: String,
    val primaryPhysics: String,
    val evolutionaryConsequence: String,
    val colorHex: Long,
    val iconName: String
) {
    CHICXULUB_BOLIDE(
        title = "Chicxulub-Class Bolide",
        subtitle = "10-km Kinetic Hypervelocity Impactor",
        triggerName = "Deploy Kinetic Impactor",
        primaryPhysics = "Massive thermal radiation shockwave, global soot wildfire, dust plume cutting 95% insolation for 30 years.",
        evolutionaryConsequence = "Primary producers collapse; ectothermic macro-herbivores starve; benthic detritivores & small burrowers inherit niches.",
        colorHex = 0xFFEF4444,
        iconName = "ic_meteor"
    ),
    LARGE_IGNEOUS_PROVINCE(
        title = "Large Igneous Province (LIP)",
        subtitle = "Mantle Plume Flood Basalts & Ocean Anoxia",
        triggerName = "Rupture Mantle Plume",
        primaryPhysics = "Gigatons of CO₂ and SO₂ erupted over 100k years; runaway hyperwarming; catastrophic oceanic anoxia (OAE) & toxic H₂S degassing.",
        evolutionaryConsequence = "Marine mass mortality (>90%); coral and benthic reef collapse; survival restricted to broad thermal generalists.",
        colorHex = 0xFFF97316,
        iconName = "ic_volcano"
    ),
    SNOWBALL_RUNAWAY(
        title = "Snowball Runaway Glaciation",
        subtitle = "Albedo Feedback Ice-Cascade to Equator",
        triggerName = "Trigger Cryogenic Runaway",
        primaryPhysics = "Global ice sheets breach critical 30° latitude; runaway planetary albedo locks equatorial oceans beneath 1-km ice at -50°C.",
        evolutionaryConsequence = "Terrestrial biota extirpated; oceanic survivors bottlenecked into sub-ice geothermal hydrothermal vents.",
        colorHex = 0xFF38BDF8,
        iconName = "ic_ice"
    ),
    GAMMA_RAY_BURST(
        title = "Gamma-Ray Burst (GRB)",
        subtitle = "Relativistic Stellar Hypernova Beam",
        triggerName = "Irradiate with GRB Beam",
        primaryPhysics = "Relativistic ionizing jet strips 80% of planetary stratospheric ozone (O₃); surface showered in lethal solar UV-C flux.",
        evolutionaryConsequence = "Catastrophic epipelagic & terrestrial die-off; severe genetic fragmentation; deep-sea endobenthos protected by water column.",
        colorHex = 0xFFA855F7,
        iconName = "ic_radiation"
    )
}
