package com.example.model

/**
 * Host star classification.
 */
enum class StarType(val label: String, val spectralClass: String, val baseTempK: Int, val colorHex: Long) {
    G_DWARF("Yellow Dwarf", "G2V", 5778, 0xFFFFF3B0),
    K_ORANGE("Orange Dwarf", "K5V", 4400, 0xFFFFB703),
    M_RED("Red Dwarf", "M3V", 3200, 0xFFFF758F),
    F_WHITE("White-Yellow", "F5V", 6500, 0xFFE0F2FE)
}

/**
 * Planetary system architecture archetype.
 */
enum class PlanetaryArchetype(
    val title: String,
    val description: String,
    val defaultGravityG: Float,
    val defaultPressureBar: Float,
    val isTidallyLocked: Boolean
) {
    STANDARD_TERRESTRIAL(
        "Standard Terrestrial",
        "Earth-like diurnal rotation with balanced seasons and 1.0g gravity",
        1.0f,
        1.01f,
        false
    ),
    TIDALLY_LOCKED_EYEBALL(
        "Tidally Locked Eyeball",
        "1:1 spin-orbit resonance around red dwarf with habitable twilight ribbon",
        0.88f,
        1.35f,
        true
    ),
    SUPER_EARTH(
        "Super-Earth (2.5g)",
        "Massive rocky planet with crushing gravity and dense 3.2 bar atmosphere",
        2.5f,
        3.20f,
        false
    )
}

/**
 * Atmospheric gas breakdown.
 */
data class AtmosphericComposition(
    val n2Percent: Float = 75.4f,
    val o2Percent: Float = 0.8f,
    val co2Ppm: Float = 4200f,
    val ch4Ppm: Float = 850f,
    val h2oPercent: Float = 1.9f,
    val arPercent: Float = 0.9f,
    val so2Ppm: Float = 120f
) {
    val isOxidizing: Boolean get() = o2Percent > 10.0f
    val isHyperthermal: Boolean get() = co2Ppm > 10000f || ch4Ppm > 2000f
}

/**
 * Thermodynamic and geodynamic planetary state.
 */
data class PlanetaryState(
    val archetype: PlanetaryArchetype = PlanetaryArchetype.STANDARD_TERRESTRIAL,
    val starType: StarType = StarType.G_DWARF,
    val stellarLuminosity: Float = 1.02f, // Solar luminosities (L_sun)
    val orbitalDistanceAU: Float = 1.04f, // AU
    val orbitalEccentricity: Float = 0.016f,
    val axialTiltDeg: Float = 23.4f, // Degrees (0 to 90)
    val gravityG: Float = 1.0f, // Surface gravity in g
    val surfacePressureBar: Float = 1.12f,
    val surfaceTempCelsius: Float = 18.4f,
    val hydrospherePercent: Float = 71.0f,
    val oceanSalinityPpt: Float = 34.5f,
    val oceanPh: Float = 7.85f,
    val iceAlbedoCoverage: Float = 0.12f, // Fraction 0.0 to 1.0 (Snowball if > 0.60)
    val magnetosphereMicroTesla: Float = 46.2f, // Core magnetic dynamo field strength
    val mantleConvectionActive: Boolean = true,
    val volcanicActivityIndex: Float = 0.45f, // 0.0 to 1.0
    val atmosphere: AtmosphericComposition = AtmosphericComposition(),
    val solarWindStripping: Boolean = false,
    val substellarX: Float = 0.5f, // Center of daytime eyeball ocean
    val substellarY: Float = 0.5f
) {
    // Effective Solar Insolation S (W/m^2)
    val insolationWm2: Float
        get() {
            val baseSolarConstant = 1361f
            val distanceFactor = baseSolarConstant * stellarLuminosity / (orbitalDistanceAU * orbitalDistanceAU)
            return distanceFactor * (1f - iceAlbedoCoverage)
        }

    val isSnowballEarth: Boolean get() = iceAlbedoCoverage >= 0.58f || surfaceTempCelsius < -15f
    val isMagnetosphereShielded: Boolean get() = magnetosphereMicroTesla >= 18f
    val isEyeballWorld: Boolean get() = archetype == PlanetaryArchetype.TIDALLY_LOCKED_EYEBALL
    val isSuperEarth: Boolean get() = archetype == PlanetaryArchetype.SUPER_EARTH
}
