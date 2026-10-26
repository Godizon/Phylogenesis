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

    companion object {
        fun fromScenario(scenario: CelestialScenario): PlanetaryState {
            val archetype = when (scenario) {
                CelestialScenario.ERIDANI_EYEBALL -> PlanetaryArchetype.TIDALLY_LOCKED_EYEBALL
                CelestialScenario.MARS_DESERT -> PlanetaryArchetype.STANDARD_TERRESTRIAL
                else -> PlanetaryArchetype.STANDARD_TERRESTRIAL
            }
            return PlanetaryState(
                archetype = archetype,
                starType = scenario.hostStar,
                orbitalDistanceAU = scenario.orbitalAU,
                gravityG = scenario.gravityG,
                surfacePressureBar = scenario.surfacePressureBar,
                surfaceTempCelsius = scenario.surfaceTempC,
                hydrospherePercent = scenario.hydrospherePercent,
                oceanSalinityPpt = scenario.oceanSalinityPpt,
                oceanPh = scenario.oceanPh,
                magnetosphereMicroTesla = scenario.magnetosphereMicroTesla,
                volcanicActivityIndex = scenario.volcanicIndex,
                atmosphere = scenario.atmosphere,
                iceAlbedoCoverage = when {
                    scenario == CelestialScenario.EUROPA_ABYSS -> 0.85f
                    scenario == CelestialScenario.TITAN_CRYOMETHANE -> 0.70f
                    scenario.surfaceTempC < 0f -> 0.65f
                    else -> 0.12f
                }
            )
        }
    }
}

/**
 * Authentic astrophysical scenarios requested by the user.
 */
enum class CelestialScenario(
    val title: String,
    val subtitle: String,
    val iconEmoji: String,
    val hostStar: StarType,
    val orbitalAU: Float,
    val gravityG: Float,
    val surfacePressureBar: Float,
    val surfaceTempC: Float,
    val hydrospherePercent: Float,
    val oceanSalinityPpt: Float,
    val oceanPh: Float,
    val magnetosphereMicroTesla: Float,
    val volcanicIndex: Float,
    val atmosphere: AtmosphericComposition,
    val description: String,
    val evolutionaryChallenge: String
) {
    EARTH_PRIME(
        title = "Earth (Terra)",
        subtitle = "Temperate Biostrome",
        iconEmoji = "🌍",
        hostStar = StarType.G_DWARF,
        orbitalAU = 1.00f,
        gravityG = 1.00f,
        surfacePressureBar = 1.01f,
        surfaceTempC = 15.5f,
        hydrospherePercent = 71.0f,
        oceanSalinityPpt = 34.5f,
        oceanPh = 8.15f,
        magnetosphereMicroTesla = 48.0f,
        volcanicIndex = 0.35f,
        atmosphere = AtmosphericComposition(n2Percent = 78.1f, o2Percent = 20.9f, co2Ppm = 420f, ch4Ppm = 18f, h2oPercent = 1.2f),
        description = "Balanced terrestrial world with protective geodynamo, plate tectonics, and rich aerobic biosphere.",
        evolutionaryChallenge = "Balance trophic food webs and survive cyclical mass extinctions across deep time."
    ),
    MARS_DESERT(
        title = "Mars (Noachian/Current)",
        subtitle = "Desiccated Rust Planet",
        iconEmoji = "🔴",
        hostStar = StarType.G_DWARF,
        orbitalAU = 1.52f,
        gravityG = 0.38f,
        surfacePressureBar = 0.006f,
        surfaceTempC = -63.0f,
        hydrospherePercent = 2.0f,
        oceanSalinityPpt = 85.0f,
        oceanPh = 6.4f,
        magnetosphereMicroTesla = 2.0f,
        volcanicIndex = 0.05f,
        atmosphere = AtmosphericComposition(n2Percent = 2.6f, o2Percent = 0.13f, co2Ppm = 95300f, ch4Ppm = 0f, h2oPercent = 0.03f, so2Ppm = 10f),
        description = "Cold, low-gravity arid desert with unshielded solar radiation and ultra-thin carbon dioxide atmosphere.",
        evolutionaryChallenge = "Evolve heavy desiccation-resistant cuticles, endolithic burrowing, and radiation catalase."
    ),
    EUROPA_ABYSS(
        title = "Europa (Jovian Moon)",
        subtitle = "Subsurface Ice Ocean",
        iconEmoji = "🧊",
        hostStar = StarType.G_DWARF,
        orbitalAU = 5.20f,
        gravityG = 0.134f,
        surfacePressureBar = 0.00001f,
        surfaceTempC = -160.0f,
        hydrospherePercent = 100.0f,
        oceanSalinityPpt = 60.0f,
        oceanPh = 7.6f,
        magnetosphereMicroTesla = 25.0f,
        volcanicIndex = 0.70f,
        atmosphere = AtmosphericComposition(n2Percent = 10.0f, o2Percent = 0.01f, co2Ppm = 800f, ch4Ppm = 450f, h2oPercent = 89.0f),
        description = "Sunless global ocean trapped under 20km ice crust. Tidally heated hydrothermal vents power cold abyss life.",
        evolutionaryChallenge = "Develop vent chemolithoautotrophy, bioluminescence, and high-pressure cellular fluidics."
    ),
    TITAN_CRYOMETHANE(
        title = "Titan (Saturnian Moon)",
        subtitle = "Cryogenic Smog World",
        iconEmoji = "🪐",
        hostStar = StarType.G_DWARF,
        orbitalAU = 9.58f,
        gravityG = 0.14f,
        surfacePressureBar = 1.45f,
        surfaceTempC = -179.0f,
        hydrospherePercent = 18.0f,
        oceanSalinityPpt = 0.0f,
        oceanPh = 7.0f,
        magnetosphereMicroTesla = 0.0f,
        volcanicIndex = 0.40f,
        atmosphere = AtmosphericComposition(n2Percent = 94.2f, o2Percent = 0.0f, co2Ppm = 10f, ch4Ppm = 56000f, h2oPercent = 0.0f, arPercent = 0.3f),
        description = "Dense nitrogen atmosphere with orange tholin haze and frigid seas of liquid methane and ethane.",
        evolutionaryChallenge = "Synthesize non-aqueous azotosome cell membranes and cryogenic metabolism at -180°C."
    ),
    ERIDANI_EYEBALL(
        title = "Epsilon Eridani b",
        subtitle = "Tidally Locked Eyeball",
        iconEmoji = "👁️",
        hostStar = StarType.K_ORANGE,
        orbitalAU = 0.054f,
        gravityG = 1.25f,
        surfacePressureBar = 2.10f,
        surfaceTempC = 24.5f,
        hydrospherePercent = 55.0f,
        oceanSalinityPpt = 42.0f,
        oceanPh = 7.7f,
        magnetosphereMicroTesla = 65.0f,
        volcanicIndex = 0.60f,
        atmosphere = AtmosphericComposition(n2Percent = 71.0f, o2Percent = 14.5f, co2Ppm = 12000f, ch4Ppm = 1200f, h2oPercent = 3.5f),
        description = "Tidally locked super-Earth orbiting an active star. Permanent daytime ocean bordered by permanent night glaciers.",
        evolutionaryChallenge = "Survive extreme Coriolis wind jets in the twilight ribbon and endure coronal mass ejections."
    )
}
