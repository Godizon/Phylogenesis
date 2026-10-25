package com.example.model

/**
 * Logarithmic "Powers of Ten" Cosmic Zoom scales.
 */
enum class CosmicScale(
    val exponent: Int,
    val title: String,
    val meterScale: String,
    val description: String,
    val subtitle: String
) {
    COSMIC(
        exponent = 7,
        title = "Cosmic Scale",
        meterScale = "10⁷ m",
        description = "Planetary Spheroid & Tectonics",
        subtitle = "Global atmospheric circulation, mantle convection & magnetosphere"
    ),
    ECOSYSTEM(
        exponent = 2,
        title = "Ecosystem Scale",
        meterScale = "10² m",
        description = "Regional Biome & Trophic Webs",
        subtitle = "Biomass density, energy vectors, species interactions & speciation zones"
    ),
    SPECIMEN(
        exponent = 0,
        title = "Organism Scale",
        meterScale = "10⁰ m",
        description = "Anatomical X-Ray & Biomechanics",
        subtitle = "Musculoskeletal IK kinematics, Fick's vascular diffusion & segment Hox genes"
    ),
    CELLULAR(
        exponent = -6,
        title = "Cellular Scale",
        meterScale = "10⁻⁶ m",
        description = "Biochemical Nano-Machines",
        subtitle = "Lipid bilayer membrane, ATP synthase, organelle endosymbiosis & GRN transcription"
    );

    val nextScale: CosmicScale
        get() = when (this) {
            COSMIC -> ECOSYSTEM
            ECOSYSTEM -> SPECIMEN
            SPECIMEN -> CELLULAR
            CELLULAR -> CELLULAR
        }

    val prevScale: CosmicScale
        get() = when (this) {
            CELLULAR -> SPECIMEN
            SPECIMEN -> ECOSYSTEM
            ECOSYSTEM -> COSMIC
            COSMIC -> COSMIC
        }
}
