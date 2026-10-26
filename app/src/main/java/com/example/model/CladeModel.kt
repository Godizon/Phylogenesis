package com.example.model

enum class TrophicLevel(val label: String, val colorHex: Long) {
    PRIMARY_PRODUCER("Autotroph (Primary Producer)", 0xFF10B981),
    PRIMARY_CONSUMER("Herbivore / Filter Feeder", 0xFF06B6D4),
    SECONDARY_CONSUMER("Mesopredator", 0xFFF59E0B),
    APEX_PREDATOR("Apex Predator", 0xFFEF4444),
    DETRITIVORE("Benthic Detritivore / Scavenger", 0xFFA855F7)
}

enum class PhylumCategory(val label: String, val eraOrigin: String, val colorHex: Long) {
    VIRUS_CAPSID("Capsid Viruses & Bacteriophages", "3.90 Ga", 0xFFA855F7),
    PREBIOTIC_PROTOCELLS("Prebiotic Protobiota (LUCA)", "4.00 Ga", 0xFF94A3B8),
    CHEMOLITHOAUTOTROPHS("Hydrothermal Chemoliths", "3.80 Ga", 0xFFE2E8F0),
    CYANOBACTERIA_ANALOG("Oxygenic Cyanobiota", "2.70 Ga", 0xFF34D399),
    EUKARYOTA_ANALOG("Complex Eukaryota", "1.80 Ga", 0xFF38BDF8),
    RADIATA_PORIFERA("Sponges & Cnidaria-Equiv", "650 Ma", 0xFFF472B6),
    ARTHROPODA_EQUIV("Segmented Exopoda (Arthropod)", "540 Ma", 0xFFFBBF24),
    CHORDATA_EQUIV("Notochord Chordata-Equiv", "525 Ma", 0xFF60A5FA),
    PLACODERMI_FISH("Armored Jawed Fishes", "420 Ma", 0xFF06B6D4),
    CHLOROPHYTA_LAND("Embryophyta (Flora)", "470 Ma", 0xFF4ADE80),
    TETRAPODA_EQUIV("Limb-Bearing Tetrapoda", "375 Ma", 0xFFFB923C),
    DINOSAURIA_REPTILE("Archosauria & Dinosaurs", "230 Ma", 0xFFEF4444),
    AVES_FLIGHT("Feathered Avian Lineages", "150 Ma", 0xFFA78BFA),
    MAMMALIA_THERIA("Endothermic Mammalia", "210 Ma", 0xFFF472B6)
}

/**
 * A taxonomic clade residing within the planetary ecosystem.
 */
data class Clade(
    val id: String,
    val name: String,
    val phylum: PhylumCategory,
    val trophicLevel: TrophicLevel,
    val speciationDateGa: Float, // e.g. 3.8 Ga or 0.54 Ga
    val extinctionDateGa: Float? = null, // null = thriving / extant
    val biomassTons: Double,
    val populationCount: Double,
    val averageLifespanYears: Float,
    val meanFitness: Float, // 0.0 to 1.0
    val thermalToleranceMinC: Float,
    val thermalToleranceMaxC: Float,
    val oxygenRequirementPercent: Float,
    val dominantMorphotype: OrganismPhenotype,
    val isExtinct: Boolean = false,
    val ancestralCladeId: String? = null
)

/**
 * Node for rendering the dynamic Phylogenetic Tree of Life (Cladogram).
 */
data class CladogramNode(
    val id: String,
    val name: String,
    val category: PhylumCategory,
    val timeGa: Float,
    val isExtinct: Boolean,
    val parentId: String?,
    val childrenIds: List<String>,
    val depth: Int,
    val description: String,
    val anatomicalInnovation: String
)
