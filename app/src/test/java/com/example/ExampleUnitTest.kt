package com.example

import com.example.engine.SimulationEngine
import com.example.model.*
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun initialState_isValidAndPopulated() {
        val state = SimulationEngine.createInitialState()
        assertNotNull(state)
        assertEquals(0.525f, state.currentTimeGa, 0.001f)
        assertEquals(GeologicalEpoch.PALEOZOIC, state.currentEpoch)
        assertTrue(state.clades.isNotEmpty())
        assertNotNull(state.selectedSpecimen)
        assertTrue(state.selectedSpecimen!!.activeCodons.isNotEmpty())
    }

    @Test
    fun tick_progressesTimeAndCalculatesClimate() {
        val state = SimulationEngine.createInitialState()
        val next = SimulationEngine.tick(state, 1.0f)

        // Time should decrease towards present (0.0 Ga)
        assertTrue(next.currentTimeGa < state.currentTimeGa)
        assertTrue(next.planetary.surfaceTempCelsius != 0f)
        assertTrue(next.totalBiomassTons > 0.0)
    }

    @Test
    fun triggerCataclysm_inflictsCasualtiesAndShiftsClimate() {
        val state = SimulationEngine.createInitialState()
        val postBolide = SimulationEngine.triggerCataclysm(state, CataclysmType.CHICXULUB_BOLIDE)

        assertNotNull(postBolide.activeCataclysm)
        assertEquals(CataclysmType.CHICXULUB_BOLIDE, postBolide.activeCataclysm!!.type)
        // Bolide causes dust cooling
        assertTrue(postBolide.planetary.surfaceTempCelsius < state.planetary.surfaceTempCelsius)
        // Biomass drops
        assertTrue(postBolide.totalBiomassTons < state.totalBiomassTons)
    }

    @Test
    fun mutateSpecimen_updatesCodonsAndBiomechanics() {
        val state = SimulationEngine.createInitialState()
        val specimen = state.selectedSpecimen!!
        val mutated = SimulationEngine.mutateSpecimen(specimen)

        assertNotNull(mutated)
        assertTrue(mutated.bodyLengthMeters >= specimen.bodyLengthMeters)
        // Square-cube law: mass scales with L^3
        assertTrue(mutated.massKg >= specimen.massKg)
        assertTrue(mutated.segments.isNotEmpty())
    }

    @Test
    fun cladogramTree_containsEvolutionaryNodes() {
        val tree = SimulationEngine.buildCladogramTree()
        assertTrue(tree.size >= 8)
        assertTrue(tree.any { it.name.contains("Common Ancestor", ignoreCase = true) || it.id.contains("luca", ignoreCase = true) })
        assertTrue(tree.any { it.isExtinct })
    }

    @Test
    fun tutorialSteps_progressCorrectly() {
        val steps = TutorialStep.values()
        assertEquals(5, steps.size)
        assertTrue(steps.first().isFirst)
        assertTrue(steps.last().isLast)
        assertEquals(SpotlightTarget.TOP_HUD, steps.first().spotlightTarget)
        assertEquals(SpotlightTarget.CLADOGRAM_BTN, steps.last().spotlightTarget)
    }

    @Test
    fun gameSettings_defaultsAreValid() {
        val settings = GameSettings()
        assertTrue(settings.audioEnabled)
        assertTrue(settings.showMiniMap)
        assertTrue(settings.showActiveToolPill)
        assertTrue(settings.showClimateTicker)
        assertEquals(14f, settings.pixelTileSize, 0.01f)
    }

    @Test
    fun planetaryArchetypes_eyeballAndSuperEarth_areValid() {
        val eyeball = PlanetaryState(archetype = PlanetaryArchetype.TIDALLY_LOCKED_EYEBALL)
        assertTrue(eyeball.isEyeballWorld)
        assertFalse(eyeball.isSuperEarth)

        val superEarth = PlanetaryState(archetype = PlanetaryArchetype.SUPER_EARTH, gravityG = 2.5f)
        assertTrue(superEarth.isSuperEarth)
        assertEquals(2.5f, superEarth.gravityG, 0.01f)
    }

    @Test
    fun weatherEngine_initializesAndDrifts() {
        val planetary = PlanetaryState()
        val weather = com.example.engine.WeatherEngine.createInitialWeather(planetary)
        assertTrue(weather.clouds.isNotEmpty())
        assertTrue(weather.globalHumidityPercent > 0f)

        val stepped = com.example.engine.WeatherEngine.stepWeather(weather, planetary, 0.1f)
        assertNotNull(stepped)
        assertEquals(weather.clouds.size, stepped.clouds.size)
    }

    @Test
    fun synthesizeOrganism_scalesBiomechanicalStressWithGravity() {
        val codons = listOf(CodonType.CHITIN_SYNTHASE, CodonType.CYTOCHROME_C)
        val earthOrganism = SimulationEngine.synthesizeOrganism(
            name = "Testus earthii",
            commonName = "Earth Crawler",
            codons = codons,
            cephalicSegments = 1,
            thoracicSegments = 4,
            abdominalSegments = 2,
            limbType = LimbType.JOINTED_WALKER,
            gravityG = 1.0f
        )

        val heavyOrganism = SimulationEngine.synthesizeOrganism(
            name = "Testus gravitas",
            commonName = "Super-Earth Crawler",
            codons = codons,
            cephalicSegments = 1,
            thoracicSegments = 4,
            abdominalSegments = 2,
            limbType = LimbType.JOINTED_WALKER,
            gravityG = 2.5f
        )

        // Under 2.5g, skeletal yield stress must be higher
        assertTrue(heavyOrganism.biomechanics.skeletalYieldStressPercent > earthOrganism.biomechanics.skeletalYieldStressPercent)
        // Metabolic cost scales with gravity load
        assertTrue(heavyOrganism.biomechanics.dailyMetabolicKj > earthOrganism.biomechanics.dailyMetabolicKj)
    }

    @Test
    fun livingOrganisms_areInitializedWithCanonicalSpecies() {
        val state = SimulationEngine.createInitialState()
        assertTrue("Living organisms must not be empty", state.livingOrganisms.isNotEmpty())
        assertTrue("Must contain Trilobite", state.livingOrganisms.any { it.cladeId == "clad_trilobita" })
        assertTrue("Must contain Anomalocaris", state.livingOrganisms.any { it.cladeId == "clad_anomalocaris" })
        assertTrue("Must contain Stem-Chordata", state.livingOrganisms.any { it.cladeId == "clad_chordata" })
        assertTrue("Must contain Stromatolite Reef", state.livingOrganisms.any { it.cladeId == "clad_cyanobiota" })
    }

    @Test
    fun tick_whenPaused_strictlyFreezesSimulation() {
        val state = SimulationEngine.createInitialState().copy(isPaused = true)
        val next = SimulationEngine.tick(state, 1.0f)

        // Time must NOT advance when paused
        assertEquals(state.currentTimeGa, next.currentTimeGa, 0.00001f)
        // Organisms must be completely frozen in place
        assertEquals(state.livingOrganisms, next.livingOrganisms)
    }

    @Test
    fun tick_whenRunning_movesOrganisms() {
        val state = SimulationEngine.createInitialState().copy(isPaused = false)
        val next = SimulationEngine.tick(state, 1.0f)

        // Time advances
        assertTrue(next.currentTimeGa < state.currentTimeGa)
        // Mobile organisms should have updated positions or swim phases
        val mobileBefore = state.livingOrganisms.first { it.speed > 0f }
        val mobileAfter = next.livingOrganisms.first { it.id == mobileBefore.id }
        assertTrue(mobileBefore.swimPhase != mobileAfter.swimPhase)
    }

    @Test
    fun hyperoxia_triggersAdaptiveGigantism() {
        val state = SimulationEngine.createInitialState()
        val hyperoxicState = state.copy(
            isPaused = false,
            planetary = state.planetary.copy(
                atmosphere = state.planetary.atmosphere.copy(o2Percent = 32.0f)
            )
        )
        val stepped = SimulationEngine.tick(hyperoxicState, 1.0f)

        // Organisms should exhibit Gigantism adaptation note and spiracle trachea
        val gigantismOrg = stepped.livingOrganisms.first { it.speed > 0f }
        assertTrue(
            "Must exhibit Gigantism adaptation: ${gigantismOrg.adaptationNote}",
            gigantismOrg.adaptationNote.contains("Gigantism", ignoreCase = true)
        )
        assertTrue(
            "Must evolve spiracle trachea under high O2",
            gigantismOrg.phenotype.activeCodons.contains(CodonType.SPIRACLE_TRACHEA)
        )
    }

    @Test
    fun hypoxia_triggersDwarfismAndBranchialGills() {
        val state = SimulationEngine.createInitialState()
        val hypoxicState = state.copy(
            isPaused = false,
            planetary = state.planetary.copy(
                atmosphere = state.planetary.atmosphere.copy(o2Percent = 3.0f)
            )
        )
        val stepped = SimulationEngine.tick(hypoxicState, 1.0f)

        val hypoxicOrg = stepped.livingOrganisms.first { it.speed > 0f }
        assertTrue(
            "Must exhibit Hypoxic adaptation: ${hypoxicOrg.adaptationNote}",
            hypoxicOrg.adaptationNote.contains("Hypoxic", ignoreCase = true)
        )
        assertTrue(
            "Must develop branchial gills across segments",
            hypoxicOrg.phenotype.segments.any { it.hasBranchialGill }
        )
    }

    @Test
    fun triggerCataclysm_inflictsDirectImpactOnLivingOrganisms() {
        val state = SimulationEngine.createInitialState()
        val postCataclysm = SimulationEngine.triggerCataclysm(state, CataclysmType.CHICXULUB_BOLIDE)

        assertTrue("Organisms must be stressed after cataclysm", postCataclysm.livingOrganisms.all { it.isStressed })
        val apex = postCataclysm.livingOrganisms.first { it.cladeId == "clad_anomalocaris" }
        // Apex predator should suffer trophic health collapse (< 0.5f)
        assertTrue("Apex predator health must drop under cataclysm", apex.health < 0.5f)
        assertEquals(OrganismBehavior.FLEEING_CATACLYSM, apex.behavior)
    }
}

