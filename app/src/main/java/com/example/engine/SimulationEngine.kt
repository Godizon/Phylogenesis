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
        return SimulationState(
            currentTimeGa = 0.525f, // 525 Ma (Early Cambrian Explosion)
            timeSpeedMaPerSec = 10.0f,
            isPaused = false,
            mode = PlayerInterventionMode.NATURALIST,
            currentScale = CosmicScale.COSMIC,
            planetary = initialPlanetary,
            weather = WeatherEngine.createInitialWeather(initialPlanetary),
            clades = initialClades,
            selectedCladeId = defaultClade.id,
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

        return currentState.copy(
            currentTimeGa = newTimeGa,
            planetary = planetary,
            weather = updatedWeather,
            activeCataclysm = activeCataclysm,
            clades = updatedClades,
            totalBiomassTons = totalBiomass
        )
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
        val bodyLength = max(0.01f, totalSegments * 0.035f)
        val hasMineralization = codons.contains(CodonType.CALCIFICATION_CA3PO4) || codons.contains(CodonType.CHITIN_SYNTHASE)
        val densityFactor = if (hasMineralization) 1150f else 980f
        val bodyRadius = bodyLength * 0.16f
        val volume = Math.PI.toFloat() * bodyRadius.pow(2) * bodyLength
        val massKg = max(0.001f, volume * densityFactor)

        val crossSectionArea = Math.PI.toFloat() * bodyRadius.pow(2)
        val materialStrength = if (codons.contains(CodonType.CALCIFICATION_CA3PO4)) 35000f else if (codons.contains(CodonType.CHITIN_SYNTHASE)) 22000f else 11000f
        val stressPercent = min(120f, (massKg * 9.81f * gravityG / (crossSectionArea * materialStrength)) * 100f)

        val hasOxygenTransport = codons.contains(CodonType.CYTOCHROME_C) || codons.contains(CodonType.VASCULAR_PUMP)
        val metabolicCost = massKg * (if (hasOxygenTransport) 45f else 18f) * (if (gravityG > 1.5f) 1.8f else 1.0f)

        val reynolds = (1000f * 0.25f * bodyLength) / 0.001f

        val biomechanics = BiomechanicalMetrics(
            bodyLengthMeters = bodyLength,
            massKg = massKg,
            skeletalYieldStressPercent = stressPercent,
            dailyMetabolicKj = metabolicCost,
            reynoldsNumber = reynolds,
            vascularDiffusionSafety = if (hasOxygenTransport) 3.5f else (0.05f / bodyLength).coerceIn(0.2f, 2.0f)
        )

        return OrganismPhenotype(
            id = "synth_" + (name.hashCode().absoluteValue % 100000),
            binomialName = name,
            commonName = commonName,
            phylum = if (limbType == LimbType.JOINTED_WALKER) "Synthetic Arthropoda" else "Engineered Chordata",
            activeCodons = codons,
            bodyLengthMeters = bodyLength,
            massKg = massKg,
            respirationSystem = if (codons.contains(CodonType.SPIRACLE_TRACHEA)) "Branching Tracheoles" else "Dermal Gill Diffusion",
            circulationSystem = if (codons.contains(CodonType.VASCULAR_PUMP)) "Multi-Chambered Vascular Heart" else "Open Hemolymph Sinus",
            gaitDescription = "${limbType.label} gait (${gravityG}g load)",
            segments = segments,
            biomechanics = biomechanics,
            fitnessScore = if (stressPercent < 85f) 0.88f else 0.42f,
            mutationRatePerGeneration = 0.045f
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
