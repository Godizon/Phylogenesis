package com.example.model

/**
 * Steps for the first-time "How to play" walkthrough. Plain language only.
 */
enum class TutorialStep(
    val stepIndex: Int,
    val emoji: String,
    val title: String,
    val description: String,
    val spotlightTarget: SpotlightTarget
) {
    PLANETARY_TIME(
        stepIndex = 1,
        emoji = "⏳",
        title = "Time flies here",
        description = "The top bar shows which chapter of history you're in. Tap ⏩ to make time go faster. Tap Life, Species or Shield to learn what each one means.",
        spotlightTarget = SpotlightTarget.TOP_HUD
    ),
    COSMIC_ZOOM(
        stepIndex = 2,
        emoji = "🔍",
        title = "Zoom in and out",
        description = "Use + and − on the right side to zoom from the whole planet, to one habitat, to a single creature, all the way down to its cells.",
        spotlightTarget = SpotlightTarget.ZOOM_DOCK
    ),
    GOD_TOOLS(
        stepIndex = 3,
        emoji = "💥",
        title = "Play god",
        description = "The bar at the bottom is your toolbox. Drop a meteor, freeze the planet or erupt volcanoes, then watch who survives. Hold any button to read what it does first.",
        spotlightTarget = SpotlightTarget.GOD_TOOLS_BTN
    ),
    EVODEVO_INSPECTION(
        stepIndex = 4,
        emoji = "👆",
        title = "Meet your creatures",
        description = "Tap any creature on the map to look inside it and see how healthy it is. You can mutate it or redesign it in the Creature Designer.",
        spotlightTarget = SpotlightTarget.SPECIMEN_DRAWER
    ),
    CLADOGRAM_TREE(
        stepIndex = 5,
        emoji = "🌳",
        title = "The Tree of Life",
        description = "Tap 🌳 to see a family tree of every species. Gray branches are extinct. Every disaster and mutation you cause changes this tree!",
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
