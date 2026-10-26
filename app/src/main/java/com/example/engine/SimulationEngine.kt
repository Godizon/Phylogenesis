package com.example.engine

import com.example.model.*
import kotlin.math.*

object SimulationEngine {

    fun createInitialState(): SimulationState {
        val initialClades = buildCanonicalEvolutionaryTree()
        val defaultClade = initialClades.firstOrNull { it.id == "clad_trilobita" } ?: initialClades.first()
        val initialPlanetary = PlanetaryState(
            surfaceTempCelsius = 19.8f,
            surfacePressureBar = 1.15f,
            iceAlbedoCoverage = 0.14f,
            magnetosphereMicroTesla = 48.0f,
            atmosphere = AtmosphericComposition(
                n2Percent = 74.2f,
                o2Percent = 14.5f,
                co2Ppm = 4500f,
                ch4Ppm = 250f,
                h2oPercent = 2.1f,
                arPercent = 0.9f
            )
        )
        val initialOrganisms = createInitialOrganisms(initialClades)
        val defaultOrganism = initialOrganisms.firstOrNull()

        return SimulationState(
            currentTimeGa = 0.525f, // 525 Ma (Early Cambrian Explosion)
            timeSpeedMaPerSec = 10.0f,
            isPaused = false,
            mode = PlayerInterventionMode.NATURALIST,
            currentScale = CosmicScale.COSMIC,
            planetary = initialPlanetary,
            weather = WeatherEngine.createInitialWeather(initialPlanetary),
            clades = initialClades,
            livingOrganisms = initialOrganisms,
            selectedCladeId = defaultClade.id,
            selectedOrganismId = defaultOrganism?.id,
            selectedSpecimen = defaultClade.dominantMorphotype,
            totalBiomassTons = 4.2e11,
            globalTaxonDiversityCount = 286
        )
    }

    /**
     * Executes one simulation time-step.
     * @param deltaSeconds Real-world frame time in seconds (e.g. 0.016s)
     */
    fun tick(currentState: SimulationState, deltaSeconds: Float): SimulationState {
        if (currentState.isPaused) return currentState

        val timeStepMa = (currentState.timeSpeedMaPerSec * deltaSeconds)
        val timeStepGa = timeStepMa / 1000f

        // Progress time towards Present (0.0 Ga)
        var newTimeGa = currentState.currentTimeGa - timeStepGa
        if (newTimeGa < 0f) newTimeGa = 4.00f // Wrap around to deep time when reaching present

        var planetary = currentState.planetary
        var activeCataclysm = currentState.activeCataclysm
        var totalBiomass = currentState.totalBiomassTons
        val currentClades = currentState.clades.toMutableList()

        // 1. Process active Cataclysm dynamics
        if (activeCataclysm != null) {
            val newElapsed = activeCataclysm.elapsedSeconds + deltaSeconds
            if (newElapsed >= activeCataclysm.durationSeconds) {
                activeCataclysm = null
            } else {
                val progress = newElapsed / activeCataclysm.durationSeconds
                val radius = sin(progress * Math.PI.toFloat())
                activeCataclysm = activeCataclysm.copy(
                    elapsedSeconds = newElapsed,
                    shockwaveRadiusPercent = radius
                )
            }
        }

        // 2. Thermodynamic & Climate calculation
        // Insolation calculation
        val effectiveInsolation = (1361f * planetary.stellarLuminosity / (planetary.orbitalDistanceAU * planetary.orbitalDistanceAU)) *
                (1f - planetary.iceAlbedoCoverage) * cos(Math.toRadians(planetary.axialTiltDeg.toDouble())).toFloat()

        // Greenhouse warming effect from CO2 and CH4
        val greenhouseCo2 = 4.2f * ln(max(100f, planetary.atmosphere.co2Ppm) / 280f)
        val greenhouseCh4 = 1.8f * (planetary.atmosphere.ch4Ppm / 1000f)
        val baseTemp = -18f + (effectiveInsolation / 1361f) * 33f + greenhouseCo2 + greenhouseCh4

        // Ice-Albedo positive feedback loop
        var iceAlbedo = if (baseTemp < -10f) {
            min(0.85f, planetary.iceAlbedoCoverage + 0.005f * deltaSeconds)
        } else if (baseTemp > 10f) {
            max(0.04f, planetary.iceAlbedoCoverage - 0.005f * deltaSeconds)
        } else {
            planetary.iceAlbedoCoverage
        }

        // Magnetosphere decay or maintenance
        var magnetosphere = planetary.magnetosphereMicroTesla
        if (!planetary.mantleConvectionActive) {
            magnetosphere = max(0f, magnetosphere - 0.2f * deltaSeconds)
        }

        // Volcanic degassing adds CO2 and SO2
        val volcanicCo2Delta = planetary.volcanicActivityIndex * 50f * deltaSeconds
        val updatedCo2 = min(60000f, max(50f, planetary.atmosphere.co2Ppm + volcanicCo2Delta))

        // Ocean pH depends on CO2 dissolution (carbonic acid)
        val oceanPh = 8.2f - (log10(max(100f, updatedCo2) / 280f) * 0.35f)

        planetary = planetary.copy(
            surfaceTempCelsius = (baseTemp * 0.95f + planetary.surfaceTempCelsius * 0.05f),
            iceAlbedoCoverage = iceAlbedo,
            magnetosphereMicroTesla = magnetosphere,
            oceanPh = oceanPh,
            atmosphere = planetary.atmosphere.copy(co2Ppm = updatedCo2)
        )

        // 3. Update ecosystem carrying capacity and clade viability
        var liveBiomass = 0.0
        val updatedClades = currentClades.map { clade ->
            val tempOk = planetary.surfaceTempCelsius in clade.thermalToleranceMinC..clade.thermalToleranceMaxC
            val o2Ok = planetary.atmosphere.o2Percent >= clade.oxygenRequirementPercent
            val phOk = planetary.oceanPh >= 7.1f

            val viabilityFactor = if (tempOk && o2Ok && phOk) 1.02f else 0.96f
            val cataclysmPenalty = if (activeCataclysm != null) 0.85f else 1.0f

            val newBiomass = max(1e4, clade.biomassTons * viabilityFactor * cataclysmPenalty)
            if (!clade.isExtinct) {
                liveBiomass += newBiomass
            }
            clade.copy(
                biomassTons = newBiomass,
                meanFitness = if (tempOk && o2Ok) min(0.99f, clade.meanFitness + 0.001f) else max(0.15f, clade.meanFitness - 0.005f)
            )
        }

        totalBiomass = max(1e9, liveBiomass)

        val updatedWeather = WeatherEngine.stepWeather(currentState.weather, planetary, deltaSeconds)
        val updatedLivingOrganisms = stepLivingOrganisms(
            organisms = currentState.livingOrganisms,
            planetary = planetary,
            activeCataclysm = activeCataclysm,
            deltaSeconds = deltaSeconds
        )

        // Environmental tipping point alerts
        var alertMsg = currentState.systemAlertMessage
        val hasGigantism = alertMsg?.contains("Gigantism") == true
        val hasHypoxia = alertMsg?.contains("Hypoxia") == true
        val hasCryo = alertMsg?.contains("Cryo-Torpor") == true
        val hasHyperthermia = alertMsg?.contains("Hyperthermia") == true

        if (planetary.atmosphere.o2Percent >= 24f && !hasGigantism) {
            alertMsg = "🦕 EVOLUTION: Atmospheric O₂ (${"%.1f".format(planetary.atmosphere.o2Percent)}%) triggering widespread Megafaunal Gigantism!"
        } else if (planetary.atmosphere.o2Percent <= 7f && !hasHypoxia) {
            alertMsg = "🫧 ADAPTATION: Hypoxic oceans (${"%.1f".format(planetary.atmosphere.o2Percent)}% O₂) driving Dwarfism and Gill Expansion!"
        } else if (planetary.surfaceTempCelsius <= 0f && !hasCryo) {
            alertMsg = "❄️ CLIMATE: Freezing oceans (${"%.1f".format(planetary.surfaceTempCelsius)}°C) inducing Cryo-Torpor and Antifreeze synthesis!"
        } else if (planetary.surfaceTempCelsius >= 40f && !hasHyperthermia) {
            alertMsg = "🔥 CLIMATE CRISIS: Ocean Hyperthermia (${"%.1f".format(planetary.surfaceTempCelsius)}°C) forcing Thermal Miniaturization!"
        }

        return currentState.copy(
            currentTimeGa = newTimeGa,
            planetary = planetary,
            weather = updatedWeather,
            activeCataclysm = activeCataclysm,
            clades = updatedClades,
            livingOrganisms = updatedLivingOrganisms,
            totalBiomassTons = totalBiomass,
            systemAlertMessage = alertMsg
        )
    }

    fun createInitialOrganisms(clades: List<Clade>): List<LivingOrganism> {
        val triloClade = clades.firstOrNull { it.id == "clad_trilobita" } ?: clades.first()
        val anomaloClade = clades.firstOrNull { it.id == "clad_anomalocaris" } ?: clades.first()
        val chordClade = clades.firstOrNull { it.id == "clad_chordata" } ?: clades.first()
        val cyanoClade = clades.firstOrNull { it.id == "clad_cyanobiota" } ?: clades.first()
        val chemoClade = clades.firstOrNull { it.id == "clad_chemoliths" } ?: cyanoClade
        val virusClade = clades.firstOrNull { it.id == "clad_virus" } ?: triloClade
        val placodermClade = clades.firstOrNull { it.id == "clad_placoderm" } ?: chordClade
        val tetrapodClade = clades.firstOrNull { it.id == "clad_tetrapod" } ?: chordClade
        val dinoClade = clades.firstOrNull { it.id == "clad_dinosaur" } ?: chordClade
        val birdClade = clades.firstOrNull { it.id == "clad_bird" } ?: chordClade
        val mammalClade = clades.firstOrNull { it.id == "clad_mammal" } ?: chordClade

        return listOf(
            // 1. Capsid Bacteriophage / Microscopic Virus
            LivingOrganism(
                id = "org_virus_1",
                cladeId = virusClade.id,
                commonName = "Icosahedral Bacteriophage",
                binomialName = virusClade.dominantMorphotype.binomialName,
                phylum = virusClade.phylum.label,
                trophicLevel = virusClade.trophicLevel,
                emoji = "🦠",
                phenotype = virusClade.dominantMorphotype,
                xRatio = 0.22f,
                yRatio = 0.32f,
                headingRad = 0.65f,
                speed = 0.038f,
                spriteScale = 0.95f,
                behavior = OrganismBehavior.HUNTING,
                behaviorTimer = 2.5f
            ),

            // 2. Trilobite (Arthropod seabed crawler)
            LivingOrganism(
                id = "org_trilo_1",
                cladeId = triloClade.id,
                commonName = "Shield-Headed Trilobite",
                binomialName = triloClade.dominantMorphotype.binomialName,
                phylum = triloClade.phylum.label,
                trophicLevel = triloClade.trophicLevel,
                emoji = "🛡️",
                phenotype = triloClade.dominantMorphotype,
                xRatio = 0.30f,
                yRatio = 0.72f,
                headingRad = 0.35f,
                speed = 0.024f,
                spriteScale = 1.30f,
                behavior = OrganismBehavior.GRAZING,
                behaviorTimer = 4.0f
            ),
            LivingOrganism(
                id = "org_trilo_2",
                cladeId = triloClade.id,
                commonName = "Shield-Headed Trilobite",
                binomialName = triloClade.dominantMorphotype.binomialName,
                phylum = triloClade.phylum.label,
                trophicLevel = triloClade.trophicLevel,
                emoji = "🛡️",
                phenotype = triloClade.dominantMorphotype,
                xRatio = 0.76f,
                yRatio = 0.78f,
                headingRad = 3.2f,
                speed = 0.022f,
                spriteScale = 1.25f,
                behavior = OrganismBehavior.CRUISING,
                behaviorTimer = 3.2f
            ),

            // 3. Anomalocarid (Radiodonta Apex Swimmer)
            LivingOrganism(
                id = "org_anomalo_1",
                cladeId = anomaloClade.id,
                commonName = "Titanopteryx (Apex Swimmer)",
                binomialName = anomaloClade.dominantMorphotype.binomialName,
                phylum = anomaloClade.phylum.label,
                trophicLevel = anomaloClade.trophicLevel,
                emoji = "🦞",
                phenotype = anomaloClade.dominantMorphotype,
                xRatio = 0.40f,
                yRatio = 0.38f,
                headingRad = 0.25f,
                speed = 0.042f,
                spriteScale = 1.60f,
                behavior = OrganismBehavior.HUNTING,
                behaviorTimer = 6.0f
            ),

            // 4. Stem-Chordate (Notochord ancestral fish)
            LivingOrganism(
                id = "org_chord_1",
                cladeId = chordClade.id,
                commonName = "Flexible-Rod Pre-Chordate",
                binomialName = chordClade.dominantMorphotype.binomialName,
                phylum = chordClade.phylum.label,
                trophicLevel = chordClade.trophicLevel,
                emoji = "🐟",
                phenotype = chordClade.dominantMorphotype,
                xRatio = 0.50f,
                yRatio = 0.52f,
                headingRad = 1.10f,
                speed = 0.050f,
                spriteScale = 1.20f,
                behavior = OrganismBehavior.SCHOOLING,
                behaviorTimer = 3.0f
            ),

            // 5. Armored Placoderm Super-Predator (Dunkleosteus)
            LivingOrganism(
                id = "org_placoderm_1",
                cladeId = placodermClade.id,
                commonName = "Armored Placoderm Super-Predator",
                binomialName = placodermClade.dominantMorphotype.binomialName,
                phylum = placodermClade.phylum.label,
                trophicLevel = placodermClade.trophicLevel,
                emoji = "🦈",
                phenotype = placodermClade.dominantMorphotype,
                xRatio = 0.65f,
                yRatio = 0.44f,
                headingRad = 2.80f,
                speed = 0.038f,
                spriteScale = 1.55f,
                behavior = OrganismBehavior.CRUISING,
                behaviorTimer = 5.0f
            ),

            // 6. Sarcopterygian Tetrapod Walker (Tiktaalik)
            LivingOrganism(
                id = "org_tetrapod_1",
                cladeId = tetrapodClade.id,
                commonName = "Shallow-Shelf Tetrapod Walker",
                binomialName = tetrapodClade.dominantMorphotype.binomialName,
                phylum = tetrapodClade.phylum.label,
                trophicLevel = tetrapodClade.trophicLevel,
                emoji = "🦎",
                phenotype = tetrapodClade.dominantMorphotype,
                xRatio = 0.35f,
                yRatio = 0.65f,
                headingRad = 0.95f,
                speed = 0.020f,
                spriteScale = 1.30f,
                behavior = OrganismBehavior.GRAZING,
                behaviorTimer = 4.5f
            ),

            // 7. Bipedal Theropod Dinosaur (Archeoraptor)
            LivingOrganism(
                id = "org_dino_1",
                cladeId = dinoClade.id,
                commonName = "Agile Bipedal Theropod",
                binomialName = dinoClade.dominantMorphotype.binomialName,
                phylum = dinoClade.phylum.label,
                trophicLevel = dinoClade.trophicLevel,
                emoji = "🦖",
                phenotype = dinoClade.dominantMorphotype,
                xRatio = 0.60f,
                yRatio = 0.70f,
                headingRad = 3.10f,
                speed = 0.034f,
                spriteScale = 1.50f,
                behavior = OrganismBehavior.HUNTING,
                behaviorTimer = 4.0f
            ),

            // 8. Feathered Avian Flyer (Protoavis)
            LivingOrganism(
                id = "org_bird_1",
                cladeId = birdClade.id,
                commonName = "Feathered Aerodynamic Aviator",
                binomialName = birdClade.dominantMorphotype.binomialName,
                phylum = birdClade.phylum.label,
                trophicLevel = birdClade.trophicLevel,
                emoji = "🦅",
                phenotype = birdClade.dominantMorphotype,
                xRatio = 0.45f,
                yRatio = 0.26f,
                headingRad = 0.15f,
                speed = 0.048f,
                spriteScale = 1.25f,
                behavior = OrganismBehavior.CRUISING,
                behaviorTimer = 3.5f
            ),

            // 9. Endothermic Mammal (Morganucodon)
            LivingOrganism(
                id = "org_mammal_1",
                cladeId = mammalClade.id,
                commonName = "Endothermic Night Forager",
                binomialName = mammalClade.dominantMorphotype.binomialName,
                phylum = mammalClade.phylum.label,
                trophicLevel = mammalClade.trophicLevel,
                emoji = "🦔",
                phenotype = mammalClade.dominantMorphotype,
                xRatio = 0.78f,
                yRatio = 0.62f,
                headingRad = 1.85f,
                speed = 0.028f,
                spriteScale = 1.10f,
                behavior = OrganismBehavior.GRAZING,
                behaviorTimer = 3.8f
            ),

            // 10. Cyanobacteria Stromatolite Reef
            LivingOrganism(
                id = "org_cyano_1",
                cladeId = cyanoClade.id,
                commonName = "Stromatolite Reef Colony",
                binomialName = cyanoClade.dominantMorphotype.binomialName,
                phylum = cyanoClade.phylum.label,
                trophicLevel = cyanoClade.trophicLevel,
                emoji = "🌿",
                phenotype = cyanoClade.dominantMorphotype,
                xRatio = 0.16f,
                yRatio = 0.80f,
                headingRad = 0f,
                speed = 0f,
                spriteScale = 1.45f,
                behavior = OrganismBehavior.GRAZING,
                behaviorTimer = 999f
            ),

            // 11. Hydrothermal Abyssal Vent Chimney
            LivingOrganism(
                id = "org_chemo_1",
                cladeId = chemoClade.id,
                commonName = "Abyssal Vent Chimney Chemolith",
                binomialName = chemoClade.dominantMorphotype.binomialName,
                phylum = chemoClade.phylum.label,
                trophicLevel = chemoClade.trophicLevel,
                emoji = "🌋",
                phenotype = chemoClade.dominantMorphotype,
                xRatio = 0.86f,
                yRatio = 0.82f,
                headingRad = 0f,
                speed = 0f,
                spriteScale = 1.50f,
                behavior = OrganismBehavior.GRAZING,
                behaviorTimer = 999f
            )
        )
    }

    fun stepLivingOrganisms(organisms: List<LivingOrganism>, deltaSeconds: Float): List<LivingOrganism> {
        return stepLivingOrganisms(organisms, PlanetaryState(), null, deltaSeconds)
    }

    fun stepLivingOrganisms(
        organisms: List<LivingOrganism>,
        planetary: PlanetaryState,
        activeCataclysm: ActiveCataclysm?,
        deltaSeconds: Float
    ): List<LivingOrganism> {
        val o2 = planetary.atmosphere.o2Percent
        val tempC = planetary.surfaceTempCelsius
        val oceanPh = planetary.oceanPh
        val gravity = planetary.gravityG

        // 1. Oxygen scaling (Gigantism vs Hypoxic Dwarfism)
        val o2ScaleFactor = when {
            o2 >= 22f -> 1.0f + ((o2 - 21f) / 18f).coerceIn(0f, 0.85f) // Gigantism up to 1.85x
            o2 <= 8f -> (0.60f + (o2 / 20f)).coerceIn(0.55f, 0.95f) // Dwarfism down to 0.55x
            else -> 1.0f
        }

        // 2. Temperature scaling (Bergmann's rule / Heat-shock dwarfism)
        val tempScaleFactor = when {
            tempC > 38f -> (1.0f - ((tempC - 38f) / 60f)).coerceIn(0.75f, 1.0f) // Lilliput effect
            tempC < 4f -> (1.0f + ((4f - tempC) / 80f)).coerceIn(1.0f, 1.15f) // Cold heat conservation
            else -> 1.0f
        }

        val combinedScaleMultiplier = o2ScaleFactor * tempScaleFactor

        // Temperature kinetics speed multiplier (Q10 enzyme kinetics)
        val tempSpeedMultiplier = when {
            planetary.isSnowballEarth || tempC < -10f -> 0.22f
            tempC < 4f -> (0.35f + (tempC + 10f) / 40f).coerceIn(0.25f, 0.85f)
            tempC > 42f -> 1.40f // Frenzied heat stress
            else -> 1.0f
        }

        return organisms.map { org ->
            if (org.speed <= 0.0001f) {
                // Sessile reef / Cyanobiota colony
                val isCyanobacteriaStressed = tempC > 48f || oceanPh < 6.8f
                val reefHealth = if (isCyanobacteriaStressed) max(0.2f, org.health - 0.01f * deltaSeconds) else min(1.0f, org.health + 0.005f * deltaSeconds)
                val reefAdaptation = when {
                    tempC > 40f -> "Hyperthermal Chemotrophy"
                    o2 >= 22f -> "Maximum Oxygenic Photosynthesis"
                    planetary.isSnowballEarth -> "Sub-Ice Hydrothermal Symbiosis"
                    else -> "Oxygenic Biostrome"
                }
                org.copy(
                    swimPhase = (org.swimPhase + deltaSeconds * 2.5f) % (2 * Math.PI.toFloat()),
                    health = reefHealth,
                    isStressed = isCyanobacteriaStressed,
                    stressReason = if (isCyanobacteriaStressed) "Ocean Thermal Bleaching" else null,
                    adaptationNote = reefAdaptation
                )
            } else {
                var newTimer = org.behaviorTimer - deltaSeconds
                var newHeading = org.headingRad
                var newBehavior = org.behavior
                var baseSpeed = org.speed
                var isStressed = false
                var stressReason: String? = null
                var adaptation = org.adaptationNote
                var currentPhenotype = org.phenotype

                // --- A. Environmental Stress Evaluation ---
                val hasHypoxia = o2 < 8.0f
                val hasSevereHypoxia = o2 < 3.5f
                val hasHyperthermia = tempC > 38.0f
                val hasGlaciation = tempC < 2.0f
                val hasAcidification = oceanPh < 7.3f
                val hasCataclysm = activeCataclysm != null

                if (hasSevereHypoxia) {
                    isStressed = true
                    stressReason = "Severe Hypoxia (${"%.1f".format(o2)}% O₂)"
                } else if (hasHyperthermia) {
                    isStressed = true
                    stressReason = "Hyperthermia (${"%.1f".format(tempC)}°C)"
                } else if (hasGlaciation && planetary.isSnowballEarth) {
                    isStressed = true
                    stressReason = "Snowball Earth Glaciation"
                } else if (hasAcidification) {
                    isStressed = true
                    stressReason = "Acid Ocean (pH ${"%.2f".format(oceanPh)})"
                }

                // --- B. Adaptive Evolution Shifts ---
                if (o2 >= 22f) {
                    adaptation = "Oxygenic Gigantism (+${((combinedScaleMultiplier - 1f) * 100).toInt()}%)"
                    if (!currentPhenotype.activeCodons.contains(CodonType.SPIRACLE_TRACHEA)) {
                        currentPhenotype = currentPhenotype.copy(
                            activeCodons = currentPhenotype.activeCodons + CodonType.SPIRACLE_TRACHEA
                        )
                    }
                } else if (hasHypoxia) {
                    adaptation = "Hypoxic Dwarfism & Gill Expansion"
                    val segmentsWithGills = currentPhenotype.segments.map { it.copy(hasBranchialGill = true) }
                    val updatedCodons = if (!currentPhenotype.activeCodons.contains(CodonType.CYTOCHROME_C)) {
                        currentPhenotype.activeCodons + CodonType.CYTOCHROME_C
                    } else currentPhenotype.activeCodons
                    currentPhenotype = currentPhenotype.copy(
                        segments = segmentsWithGills,
                        activeCodons = updatedCodons
                    )
                } else if (hasGlaciation) {
                    adaptation = "Cryo-Torpor & Antifreeze Enzymes"
                    if (!currentPhenotype.activeCodons.contains(CodonType.CATALASE)) {
                        currentPhenotype = currentPhenotype.copy(
                            activeCodons = currentPhenotype.activeCodons + CodonType.CATALASE
                        )
                    }
                } else if (hasHyperthermia) {
                    adaptation = "Heat-Shock Miniaturization"
                } else if (gravity > 1.8f) {
                    adaptation = "Super-Earth Chitin Exoskeleton"
                    if (!currentPhenotype.activeCodons.contains(CodonType.CHITIN_SYNTHASE)) {
                        currentPhenotype = currentPhenotype.copy(
                            activeCodons = currentPhenotype.activeCodons + CodonType.CHITIN_SYNTHASE
                        )
                    }
                } else {
                    adaptation = "Stabilized Phenotype"
                }

                // --- C. Cataclysm Panic & Steering ---
                if (hasCataclysm) {
                    isStressed = true
                    stressReason = "${activeCataclysm!!.type.title} Shockwave!"
                    adaptation = "Disaster Evasion"
                    newBehavior = OrganismBehavior.FLEEING_CATACLYSM

                    val epiX = 0.45f
                    val epiY = 0.50f
                    val dx = org.xRatio - epiX
                    val dy = org.yRatio - epiY
                    val fleeAngle = atan2(dy, dx)
                    newHeading = fleeAngle + (((org.swimPhase * 10f).toInt() % 3) - 1) * 0.2f
                    baseSpeed *= 1.6f
                } else if (newTimer <= 0f) {
                    newTimer = 2.5f + (abs(org.id.hashCode()) % 30) / 10f
                    val angleOffset = (((org.swimPhase * 10f).toInt() % 5) - 2) * 0.18f
                    newHeading = (newHeading + angleOffset) % (2 * Math.PI.toFloat())

                    newBehavior = when {
                        hasHypoxia -> OrganismBehavior.HYPOXIA_GASP
                        hasHyperthermia -> OrganismBehavior.THERMAL_STRESS
                        hasGlaciation && (org.cladeId == "clad_trilobita" || org.cladeId == "clad_mammal") -> OrganismBehavior.BURROWING_SHELTER
                        o2 >= 24f -> OrganismBehavior.OXYGEN_GIGANTISM
                        org.cladeId == "clad_anomalocaris" || org.cladeId == "clad_placoderm" || org.cladeId == "clad_dinosaur" -> if ((newTimer * 10).toInt() % 2 == 0) OrganismBehavior.HUNTING else OrganismBehavior.CRUISING
                        org.cladeId == "clad_virus" -> if ((newTimer * 10).toInt() % 2 == 0) OrganismBehavior.HUNTING else OrganismBehavior.RESTING
                        org.cladeId == "clad_bird" -> if ((newTimer * 10).toInt() % 3 == 0) OrganismBehavior.SCHOOLING else OrganismBehavior.CRUISING
                        org.cladeId == "clad_tetrapod" || org.cladeId == "clad_mammal" -> when ((newTimer * 10).toInt() % 3) {
                            0 -> OrganismBehavior.GRAZING
                            1 -> OrganismBehavior.RESTING
                            else -> OrganismBehavior.CRUISING
                        }
                        org.cladeId == "clad_trilobita" -> when ((newTimer * 10).toInt() % 3) {
                            0 -> OrganismBehavior.GRAZING
                            1 -> OrganismBehavior.RESTING
                            else -> OrganismBehavior.CRUISING
                        }
                        org.cladeId == "clad_chordata" -> OrganismBehavior.SCHOOLING
                        else -> OrganismBehavior.CRUISING
                    }
                }

                var effectiveSpeed = baseSpeed * tempSpeedMultiplier
                if (newBehavior == OrganismBehavior.RESTING || newBehavior == OrganismBehavior.BURROWING_SHELTER) {
                    effectiveSpeed *= 0.25f
                }

                // Health update
                val healthDelta = when {
                    hasCataclysm -> -0.06f * deltaSeconds
                    hasSevereHypoxia -> -0.03f * deltaSeconds
                    hasHyperthermia -> -0.02f * deltaSeconds
                    isStressed -> -0.008f * deltaSeconds
                    else -> 0.015f * deltaSeconds
                }
                val newHealth = (org.health + healthDelta).coerceIn(0.12f, 1.0f)

                // Smooth boundary steering
                if (org.xRatio < 0.12f && cos(newHeading) < 0f) {
                    newHeading = 0f + (((org.yRatio * 10).toInt() % 3) - 1) * 0.35f
                } else if (org.xRatio > 0.88f && cos(newHeading) > 0f) {
                    newHeading = Math.PI.toFloat() + (((org.yRatio * 10).toInt() % 3) - 1) * 0.35f
                }
                if (org.yRatio < 0.22f && sin(newHeading) < 0f) {
                    newHeading = (Math.PI.toFloat() * 0.5f)
                } else if (org.yRatio > 0.82f && sin(newHeading) > 0f) {
                    newHeading = (-Math.PI.toFloat() * 0.5f)
                }

                val moveX = cos(newHeading) * effectiveSpeed * deltaSeconds
                val moveY = sin(newHeading) * effectiveSpeed * deltaSeconds

                val newX = (org.xRatio + moveX).coerceIn(0.08f, 0.92f)
                val newY = (org.yRatio + moveY).coerceIn(0.18f, 0.84f)
                val swimFrequency = 4.5f * tempSpeedMultiplier
                val newSwim = (org.swimPhase + deltaSeconds * swimFrequency) % (2 * Math.PI.toFloat())

                // Dynamic sprite scale: smoothly interpolate toward target scale
                val defaultCladeScale = when (org.cladeId) {
                    "clad_anomalocaris" -> 1.60f
                    "clad_placoderm" -> 1.55f
                    "clad_dinosaur" -> 1.50f
                    "clad_trilobita" -> 1.30f
                    "clad_tetrapod" -> 1.30f
                    "clad_bird" -> 1.25f
                    "clad_chordata" -> 1.20f
                    "clad_mammal" -> 1.10f
                    "clad_virus" -> 0.95f
                    else -> 1.30f
                }
                val targetScale = (defaultCladeScale * combinedScaleMultiplier).coerceIn(0.70f, 2.50f)
                val lerpedScale = org.spriteScale + (targetScale - org.spriteScale) * (deltaSeconds * 2.5f).coerceIn(0.05f, 0.25f)

                // Update physical body length and mass
                val updatedPhenotype = currentPhenotype.copy(
                    bodyLengthMeters = currentPhenotype.bodyLengthMeters * (targetScale / defaultCladeScale)
                )

                org.copy(
                    xRatio = newX,
                    yRatio = newY,
                    headingRad = newHeading,
                    speed = org.speed,
                    spriteScale = lerpedScale,
                    behavior = newBehavior,
                    behaviorTimer = newTimer,
                    health = newHealth,
                    swimPhase = newSwim,
                    adaptationNote = adaptation,
                    isStressed = isStressed,
                    stressReason = stressReason,
                    phenotype = updatedPhenotype
                )
            }
        }
    }

    /**
     * Synthesizes an organism with custom vDNA codons and segment configuration.
     */
    fun synthesizeOrganism(
        name: String,
        commonName: String,
        codons: List<CodonType>,
        cephalicSegments: Int,
        thoracicSegments: Int,
        abdominalSegments: Int,
        limbType: LimbType,
        gravityG: Float = 1.0f
    ): OrganismPhenotype {
        val segments = mutableListOf<SegmentUnit>()
        var idx = 0
        for (i in 0 until cephalicSegments.coerceIn(1, 4)) {
            segments.add(SegmentUnit(idx++, SegmentType.CEPHALIC, 0.9f, if (i == 0) 1 else 0, LimbType.NONE, hasBranchialGill = true))
        }
        for (i in 0 until thoracicSegments.coerceIn(1, 8)) {
            segments.add(SegmentUnit(idx++, SegmentType.THORACIC, 1.0f, 1, limbType, hasSpiracle = true))
        }
        for (i in 0 until abdominalSegments.coerceIn(1, 6)) {
            segments.add(SegmentUnit(idx++, SegmentType.ABDOMINAL, max(0.4f, 0.8f - (i * 0.08f)), 0, LimbType.NONE))
        }

        val totalSegments = segments.size
        val bodyLength = max(0.005f, totalSegments * 0.045f)
        val hasMineralization = codons.contains(CodonType.CALCIFICATION_CA3PO4) || codons.contains(CodonType.CHITIN_SYNTHASE)
        val hasPneumaticBones = codons.contains(CodonType.PNEUMATIC_BONES)
        val densityFactor = when {
            hasPneumaticBones -> 620f // Trabecular hollow avian/dinosaur bone structure
            hasMineralization -> 1150f
            else -> 980f
        }
        val bodyRadius = bodyLength * 0.16f
        val volume = Math.PI.toFloat() * bodyRadius.pow(2) * bodyLength
        val massKg = max(0.0001f, volume * densityFactor)

        val crossSectionArea = Math.PI.toFloat() * bodyRadius.pow(2)
        val materialStrength = when {
            codons.contains(CodonType.PNEUMATIC_BONES) -> 48000f // High strength-to-weight ratio
            codons.contains(CodonType.CALCIFICATION_CA3PO4) -> 35000f
            codons.contains(CodonType.CHITIN_SYNTHASE) -> 22000f
            else -> 11000f
        }
        val stressPercent = min(120f, (massKg * 9.81f * gravityG / (crossSectionArea * materialStrength)) * 100f)

        val hasOxygenTransport = codons.contains(CodonType.CYTOCHROME_C) || codons.contains(CodonType.VASCULAR_PUMP)
        val hasEndothermy = codons.contains(CodonType.ENDOTHERMIC_THERMO)
        val hasNeocortex = codons.contains(CodonType.NEOCORTEX_HOX)
        val metabolicCost = massKg * (if (hasEndothermy) 180f else if (hasOxygenTransport) 45f else 18f) *
                (if (hasNeocortex) 1.35f else 1.0f) * (if (gravityG > 1.5f) 1.8f else 1.0f)

        val reynolds = (1000f * 0.25f * bodyLength) / 0.001f

        val biomechanics = BiomechanicalMetrics(
            bodyLengthMeters = bodyLength,
            massKg = massKg,
            skeletalYieldStressPercent = stressPercent,
            dailyMetabolicKj = metabolicCost,
            reynoldsNumber = reynolds,
            vascularDiffusionSafety = if (hasOxygenTransport) 3.5f else (0.05f / bodyLength).coerceIn(0.2f, 2.0f)
        )

        val phylumName = when (limbType) {
            LimbType.PNEUMATIC_WING -> "Feathered Avian Lineages"
            LimbType.BIPEDAL_RAPTOR_LEG -> "Archosauria & Dinosaurs"
            LimbType.AERODYNAMIC_WING -> "Segmented Exopoda (Arthropod)"
            LimbType.JOINTED_WALKER -> if (hasEndothermy) "Endothermic Mammalia" else "Synthetic Arthropoda"
            LimbType.HYDROFOIL_FIN -> "Armored Jawed Fishes"
            LimbType.CILIA_FLAGELLA -> if (codons.contains(CodonType.VIRAL_CAPSID_INJECT)) "Capsid Viruses & Bacteriophages" else "Prebiotic Protobiota (LUCA)"
            else -> "Notochord Chordata-Equiv"
        }

        val respDesc = when {
            codons.contains(CodonType.KERATIN_FEATHERS) -> "Unidirectional Crosscurrent Avian Air Sacs"
            codons.contains(CodonType.SPIRACLE_TRACHEA) -> "High-Pressure Tracheal Micro-Canals"
            codons.contains(CodonType.VASCULAR_PUMP) -> "Closed Double-Loop Pulmonary Alveoli"
            else -> "Dermal & Branchial Slit Gas Diffusion"
        }

        return OrganismPhenotype(
            id = "synth_" + (name.hashCode().absoluteValue % 100000),
            binomialName = name,
            commonName = commonName,
            phylum = phylumName,
            activeCodons = codons,
            bodyLengthMeters = bodyLength,
            massKg = massKg,
            respirationSystem = respDesc,
            circulationSystem = if (codons.contains(CodonType.VASCULAR_PUMP)) "Multi-Chambered Vascular Heart" else "Open Hemolymph Sinus",
            gaitDescription = "${limbType.label} locomotive kinematics (${gravityG}g load)",
            segments = segments,
            biomechanics = biomechanics,
            fitnessScore = if (stressPercent < 85f) 0.90f else 0.40f,
            mutationRatePerGeneration = 0.035f
        )
    }

    /**
     * Applies a Cataclysm scenario directly to the planetary ecosystem.
     */
    fun triggerCataclysm(state: SimulationState, type: CataclysmType): SimulationState {
        val casualtyFraction = when (type) {
            CataclysmType.CHICXULUB_BOLIDE -> 0.76
            CataclysmType.LARGE_IGNEOUS_PROVINCE -> 0.88
            CataclysmType.SNOWBALL_RUNAWAY -> 0.82
            CataclysmType.GAMMA_RAY_BURST -> 0.65
        }

        val updatedPlanetary = when (type) {
            CataclysmType.CHICXULUB_BOLIDE -> state.planetary.copy(
                surfaceTempCelsius = state.planetary.surfaceTempCelsius - 16f,
                iceAlbedoCoverage = min(0.65f, state.planetary.iceAlbedoCoverage + 0.35f),
                atmosphere = state.planetary.atmosphere.copy(
                    o2Percent = max(2f, state.planetary.atmosphere.o2Percent - 4f)
                )
            )
            CataclysmType.LARGE_IGNEOUS_PROVINCE -> state.planetary.copy(
                surfaceTempCelsius = state.planetary.surfaceTempCelsius + 14f,
                volcanicActivityIndex = 0.95f,
                atmosphere = state.planetary.atmosphere.copy(
                    co2Ppm = state.planetary.atmosphere.co2Ppm + 15000f,
                    so2Ppm = 1800f
                ),
                oceanPh = 6.9f
            )
            CataclysmType.SNOWBALL_RUNAWAY -> state.planetary.copy(
                surfaceTempCelsius = -42f,
                iceAlbedoCoverage = 0.82f
            )
            CataclysmType.GAMMA_RAY_BURST -> state.planetary.copy(
                magnetosphereMicroTesla = max(5f, state.planetary.magnetosphereMicroTesla - 30f),
                solarWindStripping = true,
                atmosphere = state.planetary.atmosphere.copy(
                    o2Percent = max(4f, state.planetary.atmosphere.o2Percent - 6f)
                )
            )
        }

        val casualties = state.totalBiomassTons * casualtyFraction
        val newBiomass = state.totalBiomassTons - casualties

        val updatedClades = state.clades.map { clade ->
            if (clade.isExtinct) return@map clade
            val survivalChance = when (clade.trophicLevel) {
                TrophicLevel.DETRITIVORE -> 0.70 // Detritivores survive better during collapse
                TrophicLevel.APEX_PREDATOR -> 0.10 // Apex predators collapse first
                TrophicLevel.PRIMARY_PRODUCER -> 0.25
                else -> 0.35
            }
            val willExtinct = survivalChance < 0.20 && type == CataclysmType.LARGE_IGNEOUS_PROVINCE
            clade.copy(
                biomassTons = clade.biomassTons * survivalChance,
                isExtinct = willExtinct,
                extinctionDateGa = if (willExtinct) state.currentTimeGa else clade.extinctionDateGa
            )
        }

        // Propagate cataclysm shockwave and mortality directly to living organisms
        val updatedLivingOrganisms = state.livingOrganisms.map { org ->
            val healthMultiplier = when (org.trophicLevel) {
                TrophicLevel.DETRITIVORE -> 0.85f // Hardy benthic detritivores survive best
                TrophicLevel.PRIMARY_PRODUCER -> 0.75f // Stromatolite biostromes weather shock
                TrophicLevel.PRIMARY_CONSUMER -> 0.55f
                TrophicLevel.APEX_PREDATOR -> 0.35f // Apex predators face trophic collapse
                else -> 0.50f
            }
            val mutatedPhenotype = if (type == CataclysmType.GAMMA_RAY_BURST) {
                mutateSpecimen(org.phenotype)
            } else org.phenotype

            val epiX = 0.45f
            val epiY = 0.50f
            val fleeHeading = atan2(org.yRatio - epiY, org.xRatio - epiX)

            org.copy(
                health = (org.health * healthMultiplier).coerceIn(0.15f, 1.0f),
                behavior = if (type == CataclysmType.GAMMA_RAY_BURST) OrganismBehavior.RADIATION_MUTATING else OrganismBehavior.FLEEING_CATACLYSM,
                headingRad = fleeHeading,
                speed = if (type == CataclysmType.SNOWBALL_RUNAWAY) 0.008f else org.speed * 1.6f,
                phenotype = mutatedPhenotype,
                isStressed = true,
                stressReason = "${type.title}: Impact & Extinction Wave!",
                adaptationNote = if (type == CataclysmType.GAMMA_RAY_BURST) "Cosmic Radiation Mutated" else "Disaster Evasion"
            )
        }

        val activeCataclysm = ActiveCataclysm(
            type = type,
            startedAtTimeGa = state.currentTimeGa,
            durationSeconds = 7.0f,
            casualtiesTons = casualties
        )

        return state.copy(
            planetary = updatedPlanetary,
            activeCataclysm = activeCataclysm,
            clades = updatedClades,
            livingOrganisms = updatedLivingOrganisms,
            totalBiomassTons = newBiomass,
            systemAlertMessage = "CRITICAL ALERT: ${type.title} triggered! ${type.primaryPhysics}"
        )
    }

    /**
     * Applies genetic mutation to a specimen via retroviral or cosmic vector.
     */
    fun mutateSpecimen(phenotype: OrganismPhenotype): OrganismPhenotype {
        val allCodons = CodonType.values().toList()
        val currentCodons = phenotype.activeCodons.toMutableList()
        val unusedCodons = allCodons.filter { !currentCodons.contains(it) }

        if (unusedCodons.isNotEmpty()) {
            currentCodons.add(unusedCodons.random())
        } else if (currentCodons.size > 3) {
            currentCodons.shuffle()
        }

        // Morphological shift: duplicate or expand thoracic metameres
        val newSegments = phenotype.segments.toMutableList()
        if (newSegments.size < 12) {
            val lastThorax = newSegments.indexOfLast { it.type == SegmentType.THORACIC }
            val insertIndex = if (lastThorax >= 0) lastThorax + 1 else newSegments.size / 2
            newSegments.add(
                insertIndex,
                SegmentUnit(
                    segmentIndex = newSegments.size,
                    type = SegmentType.THORACIC,
                    widthFraction = 0.85f,
                    limbPairCount = 1,
                    limbType = LimbType.JOINTED_WALKER,
                    hasSpiracle = true
                )
            )
        }

        val newLength = phenotype.bodyLengthMeters * 1.15f
        val newMass = phenotype.massKg * (1.15f.pow(3)) // Square-Cube Law: Mass scales with L^3
        val crossSectionalArea = (newLength * 0.25f).pow(2) // Area scales with L^2
        val stressPercent = min(95f, (newMass * 9.81f / (crossSectionalArea * 25000f)) * 100f)

        val updatedBiomechanics = BiomechanicalMetrics(
            bodyLengthMeters = newLength,
            massKg = newMass,
            skeletalYieldStressPercent = stressPercent,
            dailyMetabolicKj = phenotype.biomechanics.dailyMetabolicKj * 1.25f,
            reynoldsNumber = phenotype.biomechanics.reynoldsNumber * 1.3f,
            vascularDiffusionSafety = if (newLength > 0.05f) 2.4f else 0.9f
        )

        return phenotype.copy(
            activeCodons = currentCodons,
            bodyLengthMeters = newLength,
            massKg = newMass,
            segments = newSegments,
            biomechanics = updatedBiomechanics,
            fitnessScore = min(0.98f, phenotype.fitnessScore + 0.08f)
        )
    }

    /**
     * Builds the phylogenetic tree covering the 4-billion-year evolutionary span.
     */
    private fun buildCanonicalEvolutionaryTree(): List<Clade> {
        // Specimen 1: Protocellular Chemolithoautotroph (Archean)
        val protoChemo = OrganismPhenotype(
            id = "org_chemo_luca",
            binomialName = "Protobion hadalis",
            commonName = "Hadal Vent Chemolith",
            phylum = "Hydrothermal Chemoliths",
            activeCodons = listOf(CodonType.CATALASE, CodonType.CYTOCHROME_C),
            bodyLengthMeters = 0.000003f, // 3 microns
            massKg = 1e-14f,
            respirationSystem = "Membrane Anaerobic Sulfate Reduction",
            circulationSystem = "Brownian Micro-Diffusion",
            gaitDescription = "Thermal fluid convection drift & pili tethering",
            segments = listOf(
                SegmentUnit(0, SegmentType.CEPHALIC, 1.0f, 1, LimbType.CILIA_FLAGELLA)
            ),
            biomechanics = BiomechanicalMetrics(
                bodyLengthMeters = 0.000003f,
                massKg = 1e-14f,
                skeletalYieldStressPercent = 2.1f,
                dailyMetabolicKj = 0.00001f,
                reynoldsNumber = 0.0001f,
                vascularDiffusionSafety = 10.0f
            ),
            fitnessScore = 0.72f,
            mutationRatePerGeneration = 0.045f
        )

        // Specimen 2: Cyanobacteria Analog (Stromatolite builder)
        val cyanoStroma = OrganismPhenotype(
            id = "org_cyano_stroma",
            binomialName = "Chroococcus phycobilis",
            commonName = "Bioluminescent Cyanobion",
            phylum = "Oxygenic Cyanobiota",
            activeCodons = listOf(CodonType.CHLOROPHYLL_A, CodonType.RUBISCO, CodonType.CATALASE),
            bodyLengthMeters = 0.000015f,
            massKg = 8e-13f,
            respirationSystem = "Oxygenic Photosynthesis (H₂O Photolysis)",
            circulationSystem = "Intracellular Cytoplasmic Streaming",
            gaitDescription = "Mucilaginous gliding motility across carbonate mats",
            segments = listOf(
                SegmentUnit(0, SegmentType.CEPHALIC, 0.9f, 0, LimbType.NONE),
                SegmentUnit(1, SegmentType.THORACIC, 1.0f, 0, LimbType.NONE)
            ),
            biomechanics = BiomechanicalMetrics(
                bodyLengthMeters = 0.000015f,
                massKg = 8e-13f,
                skeletalYieldStressPercent = 4.2f,
                dailyMetabolicKj = 0.0002f,
                reynoldsNumber = 0.0005f,
                vascularDiffusionSafety = 8.5f
            ),
            fitnessScore = 0.85f,
            mutationRatePerGeneration = 0.025f
        )

        // Specimen 3: Trilobite-Analog (Cambrian Exopod)
        val trilobite = OrganismPhenotype(
            id = "org_trilobita_primus",
            binomialName = "Olenoides calcipteryx",
            commonName = "Shield-Headed Trilobite",
            phylum = "Segmented Exopoda (Arthropod)",
            activeCodons = listOf(
                CodonType.CHITIN_SYNTHASE,
                CodonType.CALCIFICATION_CA3PO4,
                CodonType.HOX_CEPHALIC,
                CodonType.HOX_THORACIC,
                CodonType.HOX_ABDOMINAL,
                CodonType.SPIRACLE_TRACHEA,
                CodonType.VASCULAR_PUMP
            ),
            bodyLengthMeters = 0.085f, // 8.5 cm
            massKg = 0.042f, // 42 grams
            respirationSystem = "Branchial Book-Gills on Coxal Appendages",
            circulationSystem = "Open Hemocoel with Dorsal Pulsatile Vessel",
            gaitDescription = "Metachronal 12-legged walking wave (phase delay: 0.18s)",
            segments = listOf(
                SegmentUnit(0, SegmentType.CEPHALIC, 1.0f, 1, LimbType.CILIA_FLAGELLA), // Cephalon with antennae
                SegmentUnit(1, SegmentType.THORACIC, 0.95f, 2, LimbType.JOINTED_WALKER, hasBranchialGill = true),
                SegmentUnit(2, SegmentType.THORACIC, 0.90f, 2, LimbType.JOINTED_WALKER, hasBranchialGill = true),
                SegmentUnit(3, SegmentType.THORACIC, 0.85f, 2, LimbType.JOINTED_WALKER, hasBranchialGill = true),
                SegmentUnit(4, SegmentType.THORACIC, 0.80f, 2, LimbType.JOINTED_WALKER, hasBranchialGill = true),
                SegmentUnit(5, SegmentType.ABDOMINAL, 0.70f, 1, LimbType.JOINTED_WALKER),
                SegmentUnit(6, SegmentType.CAUDAL, 0.45f, 0, LimbType.NONE) // Pygidium
            ),
            biomechanics = BiomechanicalMetrics(
                bodyLengthMeters = 0.085f,
                massKg = 0.042f,
                skeletalYieldStressPercent = 38.4f,
                dailyMetabolicKj = 2.4f,
                reynoldsNumber = 1240f,
                vascularDiffusionSafety = 2.8f
            ),
            fitnessScore = 0.91f,
            mutationRatePerGeneration = 0.012f
        )

        // Specimen 4: Anomalocarid Apex Predator
        val anomalocaris = OrganismPhenotype(
            id = "org_anomalo_apex",
            binomialName = "Titanopteryx carnifex",
            commonName = "Radian-Mouthed Apex Swimmer",
            phylum = "Segmented Exopoda (Arthropod)",
            activeCodons = listOf(
                CodonType.COLLAGEN_ELASTIN,
                CodonType.CHITIN_SYNTHASE,
                CodonType.HOX_CEPHALIC,
                CodonType.HOX_THORACIC,
                CodonType.CYTOCHROME_C,
                CodonType.VASCULAR_PUMP
            ),
            bodyLengthMeters = 0.65f, // 65 cm
            massKg = 2.8f,
            respirationSystem = "Extensive Dorsal Swimming Flap Gills",
            circulationSystem = "Multi-Chambered Contractile Heart",
            gaitDescription = "Synchronized metachronal flap undulation (Reynolds Re > 80,000)",
            segments = listOf(
                SegmentUnit(0, SegmentType.CEPHALIC, 0.9f, 2, LimbType.JOINTED_WALKER), // Great raptorial appendages
                SegmentUnit(1, SegmentType.THORACIC, 1.1f, 2, LimbType.HYDROFOIL_FIN),
                SegmentUnit(2, SegmentType.THORACIC, 1.15f, 2, LimbType.HYDROFOIL_FIN),
                SegmentUnit(3, SegmentType.THORACIC, 1.1f, 2, LimbType.HYDROFOIL_FIN),
                SegmentUnit(4, SegmentType.THORACIC, 0.95f, 2, LimbType.HYDROFOIL_FIN),
                SegmentUnit(5, SegmentType.CAUDAL, 0.60f, 1, LimbType.HYDROFOIL_FIN) // Tail fan
            ),
            biomechanics = BiomechanicalMetrics(
                bodyLengthMeters = 0.65f,
                massKg = 2.8f,
                skeletalYieldStressPercent = 46.2f,
                dailyMetabolicKj = 48.5f,
                reynoldsNumber = 84000f,
                vascularDiffusionSafety = 3.6f
            ),
            fitnessScore = 0.94f,
            mutationRatePerGeneration = 0.008f
        )

        // Specimen 5: Early Notochord Pre-Chordate (Pikaia analog)
        val pikaia = OrganismPhenotype(
            id = "org_chordata_pikaia",
            binomialName = "Notochordia gracilis",
            commonName = "Flexible-Rod Pre-Chordate",
            phylum = "Notochord Chordata-Equiv",
            activeCodons = listOf(
                CodonType.COLLAGEN_ELASTIN,
                CodonType.CALCIFICATION_CA3PO4,
                CodonType.CYTOCHROME_C,
                CodonType.VASCULAR_PUMP
            ),
            bodyLengthMeters = 0.052f, // 5.2 cm
            massKg = 0.008f,
            respirationSystem = "Pharyngeal Slits & Thin Integument",
            circulationSystem = "Ventral Aortic Arch & Segmental Loops",
            gaitDescription = "Sinuous anguilliform axial body undulation",
            segments = listOf(
                SegmentUnit(0, SegmentType.CEPHALIC, 0.7f, 1, LimbType.CILIA_FLAGELLA),
                SegmentUnit(1, SegmentType.THORACIC, 0.85f, 0, LimbType.NONE),
                SegmentUnit(2, SegmentType.THORACIC, 0.95f, 0, LimbType.NONE),
                SegmentUnit(3, SegmentType.THORACIC, 0.90f, 0, LimbType.NONE),
                SegmentUnit(4, SegmentType.CAUDAL, 0.50f, 1, LimbType.HYDROFOIL_FIN)
            ),
            biomechanics = BiomechanicalMetrics(
                bodyLengthMeters = 0.052f,
                massKg = 0.008f,
                skeletalYieldStressPercent = 18.2f,
                dailyMetabolicKj = 1.1f,
                reynoldsNumber = 3200f,
                vascularDiffusionSafety = 2.2f
            ),
            fitnessScore = 0.89f,
            mutationRatePerGeneration = 0.015f
        )

        // Specimen 6: Acellular Capsid Bacteriophage
        val phagus = OrganismPhenotype(
            id = "org_virus_phagus",
            binomialName = "Phagus archaicus",
            commonName = "Icosahedral Capsid Bacteriophage",
            phylum = "Capsid Viruses & Bacteriophages",
            activeCodons = listOf(CodonType.VIRAL_CAPSID_INJECT, CodonType.CATALASE),
            bodyLengthMeters = 0.0000002f, // 200 nanometers
            massKg = 1e-19f,
            respirationSystem = "Acellular Structural Stability",
            circulationSystem = "Brownian Micro-Diffusion",
            gaitDescription = "Brownian drift & contractile tail sheath",
            segments = listOf(SegmentUnit(0, SegmentType.CEPHALIC, 0.4f, 1, LimbType.CILIA_FLAGELLA)),
            biomechanics = BiomechanicalMetrics(0.0000002f, 1e-19f, 0.05f, 0.00001f, 0.0001f, 9.9f),
            fitnessScore = 0.86f,
            mutationRatePerGeneration = 0.12f
        )

        // Specimen 7: Armored Placoderm Super-Predator
        val placoderm = OrganismPhenotype(
            id = "org_placoderm_dunkle",
            binomialName = "Dunkleosteus armoris",
            commonName = "Armored Placoderm Super-Predator",
            phylum = "Armored Jawed Fishes",
            activeCodons = listOf(CodonType.CALCIFICATION_CA3PO4, CodonType.CYTOCHROME_C, CodonType.VASCULAR_PUMP, CodonType.COLLAGEN_ELASTIN),
            bodyLengthMeters = 1.2f,
            massKg = 45.0f,
            respirationSystem = "Opercular Branchial Gill Chambers",
            circulationSystem = "Two-Chambered Contractile Branchial Heart",
            gaitDescription = "Carangiform caudal thrust & hydrofoil stabilization",
            segments = listOf(
                SegmentUnit(0, SegmentType.CEPHALIC, 1.2f, 2, LimbType.HYDROFOIL_FIN),
                SegmentUnit(1, SegmentType.THORACIC, 1.3f, 2, LimbType.HYDROFOIL_FIN),
                SegmentUnit(2, SegmentType.THORACIC, 1.1f, 0, LimbType.NONE),
                SegmentUnit(3, SegmentType.CAUDAL, 0.7f, 1, LimbType.HYDROFOIL_FIN)
            ),
            biomechanics = BiomechanicalMetrics(1.2f, 45.0f, 42f, 210f, 180000f, 4.2f),
            fitnessScore = 0.93f,
            mutationRatePerGeneration = 0.007f
        )

        // Specimen 8: Sarcopterygian Land Walker
        val tetrapod = OrganismPhenotype(
            id = "org_tetrapod_tiktaalik",
            binomialName = "Tiktaalik amphibium",
            commonName = "Shallow-Shelf Sarcopterygian Walker",
            phylum = "Limb-Bearing Tetrapoda",
            activeCodons = listOf(CodonType.CALCIFICATION_CA3PO4, CodonType.CYTOCHROME_C, CodonType.VASCULAR_PUMP, CodonType.COLLAGEN_ELASTIN),
            bodyLengthMeters = 0.75f,
            massKg = 6.5f,
            respirationSystem = "Dual Spiracular Lungs & Dermal Mucosa",
            circulationSystem = "Three-Chambered Amphibious System",
            gaitDescription = "Sprawling quadrupedal benthic crawl & pectoral push-up",
            segments = listOf(
                SegmentUnit(0, SegmentType.CEPHALIC, 1.0f, 0, LimbType.NONE),
                SegmentUnit(1, SegmentType.THORACIC, 1.1f, 2, LimbType.JOINTED_WALKER),
                SegmentUnit(2, SegmentType.THORACIC, 1.0f, 2, LimbType.JOINTED_WALKER),
                SegmentUnit(3, SegmentType.CAUDAL, 0.6f, 1, LimbType.HYDROFOIL_FIN)
            ),
            biomechanics = BiomechanicalMetrics(0.75f, 6.5f, 38f, 55f, 45000f, 3.4f),
            fitnessScore = 0.91f,
            mutationRatePerGeneration = 0.010f
        )

        // Specimen 9: Bipedal Theropod Dinosaur
        val dinosaur = OrganismPhenotype(
            id = "org_dino_archeo",
            binomialName = "Archeoraptor velox",
            commonName = "Agile Bipedal Theropod",
            phylum = "Archosauria & Dinosaurs",
            activeCodons = listOf(CodonType.PNEUMATIC_BONES, CodonType.CALCIFICATION_CA3PO4, CodonType.ENDOTHERMIC_THERMO, CodonType.VASCULAR_PUMP, CodonType.NEOCORTEX_HOX),
            bodyLengthMeters = 1.8f,
            massKg = 28.0f,
            respirationSystem = "Unidirectional Flow-Through Lung Air Sacs",
            circulationSystem = "Four-Chambered High-Pressure Avian-Style Heart",
            gaitDescription = "Digitigrade bipedal cursorial sprint (Froude Fr > 2.5)",
            segments = listOf(
                SegmentUnit(0, SegmentType.CEPHALIC, 0.8f, 1, LimbType.JOINTED_WALKER),
                SegmentUnit(1, SegmentType.THORACIC, 1.0f, 2, LimbType.BIPEDAL_RAPTOR_LEG),
                SegmentUnit(2, SegmentType.THORACIC, 0.9f, 0, LimbType.NONE),
                SegmentUnit(3, SegmentType.CAUDAL, 0.5f, 0, LimbType.NONE)
            ),
            biomechanics = BiomechanicalMetrics(1.8f, 28.0f, 48f, 380f, 250000f, 4.8f),
            fitnessScore = 0.95f,
            mutationRatePerGeneration = 0.009f
        )

        // Specimen 10: Feathered Proto-Bird
        val protoBird = OrganismPhenotype(
            id = "org_bird_protoavis",
            binomialName = "Protoavis plumata",
            commonName = "Feathered Aerodynamic Aviator",
            phylum = "Feathered Avian Lineages",
            activeCodons = listOf(CodonType.KERATIN_FEATHERS, CodonType.PNEUMATIC_BONES, CodonType.ENDOTHERMIC_THERMO, CodonType.VASCULAR_PUMP, CodonType.NEOCORTEX_HOX),
            bodyLengthMeters = 0.42f,
            massKg = 0.85f,
            respirationSystem = "Cervical & Abdominal Respiration Air Sacs",
            circulationSystem = "Four-Chambered Complete Septum Heart",
            gaitDescription = "Flapping aerodynamic vortex lift & bipedal perching",
            segments = listOf(
                SegmentUnit(0, SegmentType.CEPHALIC, 0.6f, 0, LimbType.NONE),
                SegmentUnit(1, SegmentType.THORACIC, 0.9f, 2, LimbType.PNEUMATIC_WING),
                SegmentUnit(2, SegmentType.THORACIC, 0.8f, 2, LimbType.BIPEDAL_RAPTOR_LEG),
                SegmentUnit(3, SegmentType.CAUDAL, 0.4f, 1, LimbType.PNEUMATIC_WING)
            ),
            biomechanics = BiomechanicalMetrics(0.42f, 0.85f, 29f, 160f, 65000f, 5.2f),
            fitnessScore = 0.92f,
            mutationRatePerGeneration = 0.014f
        )

        // Specimen 11: Endothermic Mammal
        val mammal = OrganismPhenotype(
            id = "org_mammal_morganu",
            binomialName = "Morganucodon vivus",
            commonName = "Endothermic Night Forager",
            phylum = "Endothermic Mammalia",
            activeCodons = listOf(CodonType.ENDOTHERMIC_THERMO, CodonType.NEOCORTEX_HOX, CodonType.VASCULAR_PUMP, CodonType.COLLAGEN_ELASTIN, CodonType.CALCIFICATION_CA3PO4),
            bodyLengthMeters = 0.12f,
            massKg = 0.045f,
            respirationSystem = "Diaphragmatic Alveolar Pulmonary System",
            circulationSystem = "Four-Chambered System with Left Aortic Arch",
            gaitDescription = "Parasagittal quadrupedal flexed-spine bounding",
            segments = listOf(
                SegmentUnit(0, SegmentType.CEPHALIC, 0.7f, 1, LimbType.NONE),
                SegmentUnit(1, SegmentType.THORACIC, 0.85f, 2, LimbType.JOINTED_WALKER),
                SegmentUnit(2, SegmentType.THORACIC, 0.80f, 2, LimbType.JOINTED_WALKER),
                SegmentUnit(3, SegmentType.CAUDAL, 0.35f, 0, LimbType.NONE)
            ),
            biomechanics = BiomechanicalMetrics(0.12f, 0.045f, 22f, 32f, 8500f, 3.8f),
            fitnessScore = 0.93f,
            mutationRatePerGeneration = 0.011f
        )

        return listOf(
            Clade(
                id = "clad_trilobita",
                name = "Trilobita-Analog (Exopoda)",
                phylum = PhylumCategory.ARTHROPODA_EQUIV,
                trophicLevel = TrophicLevel.PRIMARY_CONSUMER,
                speciationDateGa = 0.540f,
                biomassTons = 1.8e11,
                populationCount = 4.2e13,
                averageLifespanYears = 6.5f,
                meanFitness = 0.91f,
                thermalToleranceMinC = 5f,
                thermalToleranceMaxC = 38f,
                oxygenRequirementPercent = 10f,
                dominantMorphotype = trilobite
            ),
            Clade(
                id = "clad_anomalocaris",
                name = "Radiodonta (Apex Swimmers)",
                phylum = PhylumCategory.ARTHROPODA_EQUIV,
                trophicLevel = TrophicLevel.APEX_PREDATOR,
                speciationDateGa = 0.535f,
                biomassTons = 2.4e10,
                populationCount = 8.5e10,
                averageLifespanYears = 14.0f,
                meanFitness = 0.94f,
                thermalToleranceMinC = 8f,
                thermalToleranceMaxC = 32f,
                oxygenRequirementPercent = 12f,
                dominantMorphotype = anomalocaris
            ),
            Clade(
                id = "clad_chordata",
                name = "Stem-Chordata (Notochords)",
                phylum = PhylumCategory.CHORDATA_EQUIV,
                trophicLevel = TrophicLevel.PRIMARY_CONSUMER,
                speciationDateGa = 0.528f,
                biomassTons = 6.2e10,
                populationCount = 7.7e12,
                averageLifespanYears = 4.2f,
                meanFitness = 0.89f,
                thermalToleranceMinC = 4f,
                thermalToleranceMaxC = 36f,
                oxygenRequirementPercent = 9f,
                dominantMorphotype = pikaia
            ),
            Clade(
                id = "clad_cyanobiota",
                name = "Oxygenic Stromatoliths",
                phylum = PhylumCategory.CYANOBACTERIA_ANALOG,
                trophicLevel = TrophicLevel.PRIMARY_PRODUCER,
                speciationDateGa = 2.700f,
                biomassTons = 1.5e11,
                populationCount = 9.8e18,
                averageLifespanYears = 0.05f,
                meanFitness = 0.96f,
                thermalToleranceMinC = -5f,
                thermalToleranceMaxC = 60f,
                oxygenRequirementPercent = 0.1f,
                dominantMorphotype = cyanoStroma
            ),
            Clade(
                id = "clad_chemoliths",
                name = "Abyssal Vent Chemoliths",
                phylum = PhylumCategory.CHEMOLITHOAUTOTROPHS,
                trophicLevel = TrophicLevel.DETRITIVORE,
                speciationDateGa = 3.820f,
                biomassTons = 4.8e10,
                populationCount = 2.4e17,
                averageLifespanYears = 0.02f,
                meanFitness = 0.82f,
                thermalToleranceMinC = 20f,
                thermalToleranceMaxC = 115f,
                oxygenRequirementPercent = 0.0f,
                dominantMorphotype = protoChemo
            ),
            Clade(
                id = "clad_virus",
                name = "Archaic Bacteriophages",
                phylum = PhylumCategory.VIRUS_CAPSID,
                trophicLevel = TrophicLevel.DETRITIVORE,
                speciationDateGa = 3.900f,
                biomassTons = 8.5e8,
                populationCount = 1.2e22,
                averageLifespanYears = 0.001f,
                meanFitness = 0.86f,
                thermalToleranceMinC = -20f,
                thermalToleranceMaxC = 95f,
                oxygenRequirementPercent = 0.0f,
                dominantMorphotype = phagus
            ),
            Clade(
                id = "clad_placoderm",
                name = "Armored Jawed Fishes",
                phylum = PhylumCategory.PLACODERMI_FISH,
                trophicLevel = TrophicLevel.APEX_PREDATOR,
                speciationDateGa = 0.420f,
                biomassTons = 4.5e10,
                populationCount = 6.2e9,
                averageLifespanYears = 22.0f,
                meanFitness = 0.93f,
                thermalToleranceMinC = 6f,
                thermalToleranceMaxC = 34f,
                oxygenRequirementPercent = 14f,
                dominantMorphotype = placoderm
            ),
            Clade(
                id = "clad_tetrapod",
                name = "Sarcopterygian Walkers",
                phylum = PhylumCategory.TETRAPODA_EQUIV,
                trophicLevel = TrophicLevel.SECONDARY_CONSUMER,
                speciationDateGa = 0.375f,
                biomassTons = 3.2e10,
                populationCount = 4.8e10,
                averageLifespanYears = 12.0f,
                meanFitness = 0.91f,
                thermalToleranceMinC = 8f,
                thermalToleranceMaxC = 36f,
                oxygenRequirementPercent = 16f,
                dominantMorphotype = tetrapod
            ),
            Clade(
                id = "clad_dinosaur",
                name = "Theropoda & Carnosaurs",
                phylum = PhylumCategory.DINOSAURIA_REPTILE,
                trophicLevel = TrophicLevel.APEX_PREDATOR,
                speciationDateGa = 0.230f,
                biomassTons = 6.8e10,
                populationCount = 1.8e9,
                averageLifespanYears = 35.0f,
                meanFitness = 0.95f,
                thermalToleranceMinC = 10f,
                thermalToleranceMaxC = 42f,
                oxygenRequirementPercent = 18f,
                dominantMorphotype = dinosaur
            ),
            Clade(
                id = "clad_bird",
                name = "Avian Theropoda",
                phylum = PhylumCategory.AVES_FLIGHT,
                trophicLevel = TrophicLevel.SECONDARY_CONSUMER,
                speciationDateGa = 0.150f,
                biomassTons = 2.1e10,
                populationCount = 3.4e10,
                averageLifespanYears = 8.5f,
                meanFitness = 0.92f,
                thermalToleranceMinC = -15f,
                thermalToleranceMaxC = 44f,
                oxygenRequirementPercent = 19f,
                dominantMorphotype = protoBird
            ),
            Clade(
                id = "clad_mammal",
                name = "Endothermic Mammalia",
                phylum = PhylumCategory.MAMMALIA_THERIA,
                trophicLevel = TrophicLevel.PRIMARY_CONSUMER,
                speciationDateGa = 0.210f,
                biomassTons = 3.9e10,
                populationCount = 9.2e11,
                averageLifespanYears = 5.0f,
                meanFitness = 0.93f,
                thermalToleranceMinC = -25f,
                thermalToleranceMaxC = 45f,
                oxygenRequirementPercent = 18f,
                dominantMorphotype = mammal
            )
        )
    }

    /**
     * Builds the complete tree nodes for the Cladogram view.
     */
    fun buildCladogramTree(): List<CladogramNode> {
        return listOf(
            CladogramNode(
                id = "node_luca",
                name = "L.U.C.A. (Last Universal Common Ancestor)",
                category = PhylumCategory.PREBIOTIC_PROTOCELLS,
                timeGa = 4.00f,
                isExtinct = false,
                parentId = null,
                childrenIds = listOf("node_chemo", "node_photo"),
                depth = 0,
                description = "Prebiotic catalytic protocell driven by deep-sea iron-sulfur redox gradient.",
                anatomicalInnovation = "Lipid vesicle encapsulation & primitive proton ATPase"
            ),
            CladogramNode(
                id = "node_chemo",
                name = "Chemolithoautotrophy",
                category = PhylumCategory.CHEMOLITHOAUTOTROPHS,
                timeGa = 3.80f,
                isExtinct = false,
                parentId = "node_luca",
                childrenIds = emptyList(),
                depth = 1,
                description = "Methanogenesis & sulfur reduction in hydrothermal chimney chimneys.",
                anatomicalInnovation = "Enzymatic Fe-S cluster electron conduits"
            ),
            CladogramNode(
                id = "node_photo",
                name = "Oxygenic Photosynthesis",
                category = PhylumCategory.CYANOBACTERIA_ANALOG,
                timeGa = 2.70f,
                isExtinct = false,
                parentId = "node_luca",
                childrenIds = listOf("node_eukarya"),
                depth = 1,
                description = "Photolysis of water generating O₂ byproduct; triggers Great Oxidation Event.",
                anatomicalInnovation = "Manganese water-splitting complex (Photosystem II)"
            ),
            CladogramNode(
                id = "node_eukarya",
                name = "Eukaryogenesis & Endosymbiosis",
                category = PhylumCategory.EUKARYOTA_ANALOG,
                timeGa = 1.80f,
                isExtinct = false,
                parentId = "node_photo",
                childrenIds = listOf("node_radiata", "node_bilateria"),
                depth = 2,
                description = "Engulfment of alpha-proteobacteria yields mitochondria, increasing ATP budget 18-fold.",
                anatomicalInnovation = "Double-membrane nucleus, linear chromosomes & endosymbionts"
            ),
            CladogramNode(
                id = "node_radiata",
                name = "Radial Diploblasts (Cnidaria/Porifera)",
                category = PhylumCategory.RADIATA_PORIFERA,
                timeGa = 0.650f,
                isExtinct = false,
                parentId = "node_eukarya",
                childrenIds = emptyList(),
                depth = 3,
                description = "Multicellular sessile filters and radial symmetry with simple nerve net.",
                anatomicalInnovation = "Extracellular collagen sponge scaffolding & cnidocytes"
            ),
            CladogramNode(
                id = "node_bilateria",
                name = "Triploblastic Bilateria",
                category = PhylumCategory.ARTHROPODA_EQUIV,
                timeGa = 0.570f,
                isExtinct = false,
                parentId = "node_eukarya",
                childrenIds = listOf("node_arthropoda", "node_chordata"),
                depth = 3,
                description = "Cephalization: Anterior-posterior gradient and three germ tissue layers (Ecto/Meso/Endo).",
                anatomicalInnovation = "Homeobox Hox clusters directing directional locomotion"
            ),
            CladogramNode(
                id = "node_arthropoda",
                name = "Segmented Exopoda (Arthropoda)",
                category = PhylumCategory.ARTHROPODA_EQUIV,
                timeGa = 0.540f,
                isExtinct = false,
                parentId = "node_bilateria",
                childrenIds = listOf("node_trilobite_fossil"),
                depth = 4,
                description = "Chitinous exoskeleton plates with articulated jointed appendages and compound vision.",
                anatomicalInnovation = "Chitin-protein biocomposite plates & metachronal gait"
            ),
            CladogramNode(
                id = "node_trilobite_fossil",
                name = "Olenellus (Extinct Clade)",
                category = PhylumCategory.ARTHROPODA_EQUIV,
                timeGa = 0.252f,
                isExtinct = true,
                parentId = "node_arthropoda",
                childrenIds = emptyList(),
                depth = 5,
                description = "Wiped out during End-Permian Great Dying supervolcanic catastrophe.",
                anatomicalInnovation = "Calcite crystal holochroal optical lenses"
            ),
            CladogramNode(
                id = "node_chordata",
                name = "Notochord Chordata-Equiv",
                category = PhylumCategory.CHORDATA_EQUIV,
                timeGa = 0.525f,
                isExtinct = false,
                parentId = "node_bilateria",
                childrenIds = listOf("node_tetrapoda"),
                depth = 4,
                description = "Flexible dorsal rod (notochord), hollow dorsal nerve cord, and post-anal tail.",
                anatomicalInnovation = "Internal hydro-elastic skeletal rod allowing powerful axial undulating stroke"
            ),
            CladogramNode(
                id = "node_tetrapoda",
                name = "Limbed Tetrapoda (Land Colonizers)",
                category = PhylumCategory.TETRAPODA_EQUIV,
                timeGa = 0.375f,
                isExtinct = false,
                parentId = "node_chordata",
                childrenIds = emptyList(),
                depth = 5,
                description = "Transition to terrestrial environment: weight-bearing robust dactylic limbs.",
                anatomicalInnovation = "Pectoral girdle detachment from skull & sacral pelvis fusion"
            )
        )
    }
}
