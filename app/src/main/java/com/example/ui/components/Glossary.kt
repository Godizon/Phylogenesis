package com.example.ui.components

import com.example.model.CataclysmType
import com.example.model.CosmicScale
import com.example.model.GeologicalEpoch
import java.util.Locale

/**
 * One plain-language explanation shown when the player taps a "?"-able thing.
 * Rule: no jargon in [plain]. Real scientific terms can appear in [funFact].
 */
data class InfoEntry(
    val emoji: String,
    val title: String,
    val plain: String,
    val funFact: String? = null
)

object Glossary {
    val era = InfoEntry(
        "⏳", "Time Period",
        "The planet's story is split into big chapters called eras. This is the chapter you're in right now. Each one has its own kind of life.",
        "Earth's real history is about 4.5 billion years long. You can jump between chapters with the Time Travel button."
    )
    val speed = InfoEntry(
        "⏩", "Game Speed",
        "Tap to make time pass faster or slower. At the fastest speed, millions of years fly by every few seconds, so evolution happens quickly.",
        "1x = calm, 10x = fast, 100x = turbo."
    )
    val life = InfoEntry(
        "🌱", "Life Amount",
        "How much living stuff exists on your planet in total: every plant, microbe and animal added together.",
        "Scientists call this biomass. When it crashes, something bad happened. When it climbs, life is thriving."
    )
    val species = InfoEntry(
        "🧬", "Species Count",
        "How many different kinds of living things exist right now. New ones appear when a group splits and slowly changes over time.",
        "Scientists call a family of related species a clade. Open the Tree of Life to see how they connect."
    )
    val shield = InfoEntry(
        "🛡️", "Planet Shield",
        "A magnetic bubble around your planet that blocks deadly space radiation. With the shield up, life is safe. Without it, delicate life struggles.",
        "On Earth this is the magnetosphere, made by the churning molten core."
    )
    val tree = InfoEntry(
        "🌳", "Tree of Life",
        "A family tree of every species that has ever lived on your planet. Branches that stop are extinct. Tap a branch to learn about it.",
        "Biologists call this a phylogenetic tree. That's where the game's name comes from!"
    )
    val geneLab = InfoEntry(
        "🧪", "Gene Lab",
        "Design your own creature. Pick its body, diet and traits, then release it into the world and watch whether it survives.",
        "Tougher worlds (more gravity, less oxygen) make some designs impossible."
    )
    val timeTravel = InfoEntry(
        "🕰️", "Time Travel",
        "Jump straight to a famous moment in the history of life, like the first photosynthesis or the Cambrian explosion."
    )
    val moreTools = InfoEntry(
        "🪐", "Planet Controls",
        "Change the planet itself: move it closer to or further from its sun, tilt it, or change the air. Small changes can transform everything that lives there."
    )
    val zoom = InfoEntry(
        "🔍", "Zoom",
        "Zoom in from the whole planet, down to a habitat, to a single creature, all the way to its cells. Tap + and − on the right.",
        "Tap any creature while zoomed out to inspect it."
    )
    val creature = InfoEntry(
        "👆", "Tap a creature!",
        "Tap any living thing on the map to see what it eats, how it moves, and how well it's surviving. You can even mutate it."
    )
}

// ---------- Friendly names for scientific enums ----------

val GeologicalEpoch.friendlyName: String
    get() = when (this) {
        GeologicalEpoch.HADEAN -> "Molten World"
        GeologicalEpoch.ARCHEAN -> "Age of Microbes"
        GeologicalEpoch.PROTEROZOIC -> "Age of Oxygen"
        GeologicalEpoch.PALEOZOIC -> "Explosion of Life"
        GeologicalEpoch.MESOZOIC -> "Age of Reptiles"
        GeologicalEpoch.CENOZOIC -> "Age of Mammals"
    }

val GeologicalEpoch.emoji: String
    get() = when (this) {
        GeologicalEpoch.HADEAN -> "🌋"
        GeologicalEpoch.ARCHEAN -> "🦠"
        GeologicalEpoch.PROTEROZOIC -> "🫧"
        GeologicalEpoch.PALEOZOIC -> "🐚"
        GeologicalEpoch.MESOZOIC -> "🦖"
        GeologicalEpoch.CENOZOIC -> "🐘"
    }

val GeologicalEpoch.friendlyBlurb: String
    get() = when (this) {
        GeologicalEpoch.HADEAN -> "Hot, rocky and hostile. The very first life sparks near underwater vents."
        GeologicalEpoch.ARCHEAN -> "Single-celled microbes spread through the oceans and form slimy mats."
        GeologicalEpoch.PROTEROZOIC -> "Tiny organisms start making oxygen from sunlight and change the air forever."
        GeologicalEpoch.PALEOZOIC -> "Animals suddenly burst into thousands of shapes, then crawl onto land."
        GeologicalEpoch.MESOZOIC -> "Reptiles rule the land, sea and sky, and flowering plants appear."
        GeologicalEpoch.CENOZOIC -> "Mammals and birds spread out. Grasslands and modern habitats appear."
    }

data class FriendlyScale(val emoji: String, val name: String, val blurb: String)

val CosmicScale.friendly: FriendlyScale
    get() = when (this) {
        CosmicScale.COSMIC -> FriendlyScale("🌍", "Planet View", "See continents, oceans, weather and clouds")
        CosmicScale.ECOSYSTEM -> FriendlyScale("🌿", "Habitat View", "See who eats whom in one region")
        CosmicScale.SPECIMEN -> FriendlyScale("🦎", "Creature View", "See inside a single animal's body")
        CosmicScale.CELLULAR -> FriendlyScale("🔬", "Cell View", "See the tiny machinery inside one cell")
    }

data class FriendlyPower(val emoji: String, val name: String, val blurb: String)

val CataclysmType.friendly: FriendlyPower
    get() = when (this) {
        CataclysmType.CHICXULUB_BOLIDE -> FriendlyPower(
            "☄️", "Meteor",
            "Smash a giant asteroid into the planet. Big animals usually die. Small, hardy ones survive and take over."
        )
        CataclysmType.LARGE_IGNEOUS_PROVINCE -> FriendlyPower(
            "🌋", "Eruption",
            "Split the crust open and flood the world with lava and gas. The oceans lose oxygen and sea life suffers most."
        )
        CataclysmType.SNOWBALL_RUNAWAY -> FriendlyPower(
            "❄️", "Ice Age",
            "Freeze the planet from the poles to the equator. Only life hiding in warm deep-sea vents survives."
        )
        CataclysmType.GAMMA_RAY_BURST -> FriendlyPower(
            "☢️", "Radiation",
            "Blast the planet with radiation from an exploding star. Surface life dies. Life deep underwater is protected."
        )
    }

/** "3.47 billion years ago" / "541 million years ago" / "Today" */
fun formatYearsAgo(timeGa: Float): String = when {
    timeGa >= 1f -> String.format(Locale.US, "%.2f billion years ago", timeGa)
    timeGa >= 0.001f -> String.format(Locale.US, "%.0f million years ago", timeGa * 1000f)
    else -> "Today"
}
