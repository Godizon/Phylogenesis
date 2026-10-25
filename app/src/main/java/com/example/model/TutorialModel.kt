package com.example.model

/**
 * Steps for the interactive first-time tutorial.
 */
enum class TutorialStep(
    val stepIndex: Int,
    val title: String,
    val description: String,
    val spotlightTarget: SpotlightTarget
) {
    PLANETARY_TIME(
        stepIndex = 1,
        title = "1. Planetary Deep Time & Physics",
        description = "Observe geological time moving in Millions (Ma) and Billions (Ga) of years. The top bar tracks active epochs, simulation speed (1x to 100x), and global biomass.",
        spotlightTarget = SpotlightTarget.TOP_HUD
    ),
    COSMIC_ZOOM(
        stepIndex = 2,
        title = "2. 'Powers of Ten' Cosmic Zoom",
        description = "Seamlessly zoom across 13 orders of magnitude: from 10⁷m (Planetary tilemap) to 10²m (Ecosystem biome), 10⁰m (Organism X-ray), and 10⁻⁶m (Cellular nanomachines).",
        spotlightTarget = SpotlightTarget.ZOOM_DOCK
    ),
    GOD_TOOLS(
        stepIndex = 3,
        title = "3. God-Tools & Demiurge Powers",
        description = "Open the God-Tools drawer to shift planetary orbits, tilt axial obliquity, inject greenhouse gases, or trigger mass extinction cataclysms like asteroid impacts and mantle plumes.",
        spotlightTarget = SpotlightTarget.GOD_TOOLS_BTN
    ),
    EVODEVO_INSPECTION(
        stepIndex = 4,
        title = "4. Evo-Devo & Specimen Anatomy",
        description = "Tap any living creature in the world to open its 16-bit Anatomical X-Ray. Monitor its skeletal stress %, cardiovascular pulse, and inject retroviral mutations to alter its genome.",
        spotlightTarget = SpotlightTarget.SPECIMEN_DRAWER
    ),
    CLADOGRAM_TREE(
        stepIndex = 5,
        title = "5. The Tree of Life (Cladogram)",
        description = "Tap the tree icon anytime to explore 4.0 billion years of speciation branches, evolutionary innovations, and extinct fossil lineages from LUCA to macrofauna.",
        spotlightTarget = SpotlightTarget.CLADOGRAM_BTN
    );

    val isFirst: Boolean get() = stepIndex == 1
    val isLast: Boolean get() = stepIndex == values().size
}

enum class SpotlightTarget {
    TOP_HUD,
    ZOOM_DOCK,
    GOD_TOOLS_BTN,
    SPECIMEN_DRAWER,
    CLADOGRAM_BTN
}

data class TutorialState(
    val isActive: Boolean = false,
    val currentStep: TutorialStep = TutorialStep.PLANETARY_TIME,
    val hasCompletedTutorial: Boolean = false
)
