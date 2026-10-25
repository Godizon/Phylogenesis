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
        title = "Meteor Strike",
        subtitle = "A giant asteroid hits the planet",
        triggerName = "Drop the Asteroid",
        primaryPhysics = "A huge blast, wildfires everywhere, and dust that blocks the sun for years.",
        evolutionaryConsequence = "Plants die first, then big animals starve. Small, hardy creatures inherit the world.",
        colorHex = 0xFFEF4444,
        iconName = "ic_meteor"
    ),
    LARGE_IGNEOUS_PROVINCE(
        title = "Mega Eruption",
        subtitle = "The crust splits and floods the world with lava",
        triggerName = "Split the Crust",
        primaryPhysics = "Lava and poison gas pour out for thousands of years. The planet overheats and the oceans run low on oxygen.",
        evolutionaryConsequence = "Most sea life dies, reefs collapse, and only tough all-rounders survive.",
        colorHex = 0xFFF97316,
        iconName = "ic_volcano"
    ),
    SNOWBALL_RUNAWAY(
        title = "Ice Age",
        subtitle = "Ice spreads from the poles to the equator",
        triggerName = "Freeze the Planet",
        primaryPhysics = "White ice reflects sunlight, which makes the planet colder, which makes more ice, until the whole planet is frozen.",
        evolutionaryConsequence = "Land life vanishes. Survivors hide near warm deep-sea vents under the ice.",
        colorHex = 0xFF38BDF8,
        iconName = "ic_ice"
    ),
    GAMMA_RAY_BURST(
        title = "Radiation Blast",
        subtitle = "A star explodes and aims a beam at your planet",
        triggerName = "Fire the Beam",
        primaryPhysics = "The beam destroys most of the ozone layer, so deadly sunlight reaches the surface.",
        evolutionaryConsequence = "Surface and shallow-water life die off. Deep-sea life is shielded by the water above it.",
        colorHex = 0xFFA855F7,
        iconName = "ic_radiation"
    )
}
