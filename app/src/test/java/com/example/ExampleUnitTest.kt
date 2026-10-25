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
}
