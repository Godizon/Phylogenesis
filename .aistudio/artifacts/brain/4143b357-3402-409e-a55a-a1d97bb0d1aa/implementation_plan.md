# Universal Biosphere & Evo-Devo Engine: Procedural Life, Real Celestial Worlds & Advanced Mutation Workbench

A comprehensive expansion of Phylogenesis into a deep-time biological simulator. This plan introduces procedural taxonomy spanning all major life forms (microbes, viruses, fish, dinosaurs, birds, and mammals), a realistic celestial scenario catalog (Earth, Mars, Europa, Titan, and Eridani), an advanced Evo-Devo mutation laboratory tracking real physical and biological laws, and a non-overlapping docked UI architecture that stays clean and responsive indefinitely.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following directions were confirmed during interactive clarification and form the foundation of this implementation:
> - **Creature Diversity & Speciation**: Full procedural taxonomy spanning microbes, viruses, armored fishes, land tetrapods, dinosaurs, birds, and mammals across all geological epochs.
> - **Planetary Scenarios**: Realistic celestial catalog featuring Earth (Archean to Present), Mars (Noachian to Desiccated), Europa (Subsurface Ice Ocean), Titan (Cryogenic Methane Seas), and Epsilon Eridani b (Tidally Locked M-Dwarf Eyeball World).
> - **Mutation Lab Architecture**: Advanced Evo-Devo workbench with comprehensive biological telemetry (metabolism, thermodynamics, gas diffusion, skeletal yield stress, Reynolds number) and non-overlapping docked UI.

---

## 1. Overview & Core Concept

Phylogenesis will transform from a Cambrian-focused prototype into a full universal biosphere simulator:
1. **Procedural Taxonomy & Infinite Evolutionary Radiations**:
   - Spans 6 major biological domains:
     - **Microbial & Viral**: LUCA Protocells (*Protobion hadalis*), Giant Bacteriophages (*Phagus archaicus*), Oxygenic Stromatolite Reefs (*Collenia aerophila*).
     - **Marine Metazoans & Paleozoic Exopods**: Cambrian Trilobites (*Olenoides calcipteryx*), Apex Radiodonts (*Titanopteryx carnifex*), Stem-Chordates (*Notochordia gracilis*).
     - **Jawed Fishes & Land Conquest**: Devonian Armored Placoderms (*Dunkleosteus armoris*), Sarcopterygian Walkers (*Tiktaalik amphibium*).
     - **Giant Arthropods & Mesozoic Dinosaurs**: Carboniferous Meganeura (*Meganeura gigas*), Bipedal Carnosaurs (*Archeoraptor velox*), Giant Sauropods (*Titanosaurus ingens*).
     - **Avian & Mammalian Radiations**: Paleogene Proto-Birds (*Protoavis plumata*), Endothermic Mammals (*Morganucodon vivus*).
   - **Continuous Procedural Speciation**: Organisms accumulate micro-mutations over geological time. Environmental pressures trigger adaptive radiations, generating new procedural binomial names, colors, and limb configurations.

2. **Astrophysically Grounded Celestial Scenarios**:
   - **Earth**: Standard 1.0g terrestrial environment with active geodynamo, plate tectonics, and nitrogen/oxygen atmosphere.
   - **Mars**: 0.38g gravity, 0.006 bar thin CO₂ atmosphere, -63°C cold, zero intrinsic magnetic shielding. Requires desiccation resistance, UV-protective pigments, and subterranean burrowing.
   - **Europa**: Subsurface global ocean trapped beneath 20km ice sheet. 0.13g microgravity, zero sunlight, extreme hydrostatic pressure. Life depends on hydrothermal vent chemolithoautotrophy and bioluminescence.
   - **Titan**: 0.14g gravity, dense 1.45 bar nitrogen/methane atmosphere, -179°C cryogenic surface with liquid hydrocarbon seas. Life evolves azotosome cell membranes and cryogenic biochemistry.
   - **Epsilon Eridani b**: 1.25g super-Earth in close orbit around an active flare star. Tidally locked with a permanent day-night division and violent stellar winds.

3. **Advanced Scientific Evo-Devo Gene Lab**:
   - Tracks actual physiological and biomechanical metrics:
     - **Square-Cube Law**: Mass ($L^3$) vs Skeletal Load Area ($L^2$) yield stress.
     - **Fick's Law of Diffusion**: $t \approx x^2 / (2D)$ governing tracheal vs vascular circulatory systems.
     - **Fluid Reynolds Number**: $Re = \rho v L / \mu$ determining creeping viscous flow vs inertial hydrodynamics.
     - **Thermodynamic Entropy & Metabolic Scope**: Basal metabolic rate (BMR) and daily energy cost $\Delta G$.
   - Interactive multi-tab workbench: Genetic Codon Sequencer, Metamere & Organ System Builder, Biomechanical Stress Analyzer, and Live Planetary Injection.

4. **Non-Overlapping Component Coordination & Long-Term Stability**:
   - Resolves all component collisions (e.g., overlapping alerts at `118.dp`, drawer toggles clashing with floating layer tools, and specimen card overlap with bottom docks).
   - Enforces strict memory and entity pooling to ensure smooth 60 FPS performance even after running for hours.

---

## 2. User Experience & Visual Design

### Screen Layout & Docking Architecture (Eliminating All Overlaps)

```
┌────────────────────────────────────────────────────────────────────────┐
│ TOP HUD BAR: Planet Name, Geological Epoch, Game Speed, Master Buttons │
├────────────────────────────────────────────────────────────────────────┤
│ UNIFIED ALERT DOCK (Single-slot: Climate Alerts or Disaster Shockwave) │
├────────────────────────────────────────────────────────────────────────┤
│                                                        ┌──────────────┐│
│                                                        │ TOOL PALETTE ││
│                                                        │ • Immersive  ││
│  FULL-SCREEN PIXEL SIMULATION CANVAS                   │ • Clouds     ││
│  (Unobstructed living world with 16-bit sprites)       │ • Day/Night  ││
│                                                        │ • Minimap    ││
│                                                        │ • Radar Heat ││
│                                                        └──────────────┘│
│                                                                        │
│                      ┌─────────────────────────┐                       │
│                      │ SPECIMEN QUICK CARD     │                       │
│                      │ (Floating above dock)   │                       │
│                      └─────────────────────────┘                       │
├────────────────────────────────────────────────────────────────────────┤
│ TIMELINE BAR & POWERS DOCK (Edge-to-edge with navigationBarsPadding)   │
└────────────────────────────────────────────────────────────────────────┘
```

1. **Non-Overlapping Layout Rules**:
   - **Top Zone**: `TopHudBar` at `Alignment.TopCenter`. A single, unified `AlertNotificationSlot` is positioned directly below the HUD (`top = 80.dp`). Climate ticker and system alert messages share this single container, eliminating the previous duplicate overlay collision at `118.dp`.
   - **Side Panels**: `PlanetaryDrawer` (left) and `CladeDrawer` (right) are mutually exclusive. Opening one smoothly closes the other.
   - **Floating Quick Toggles**: Consolidated into a single compact vertical palette at `Alignment.TopEnd` (below the HUD, clear of any drawer zones).
   - **Specimen Quick Card**: Positioned with dynamic clearance (`bottom = 100.dp`), docking cleanly above the bottom navigation bar without overlapping the radar minimap or timeline.

2. **Celestial Scenario Selection Flow**:
   - A dedicated **Celestial Observatory Modal** accessible from the Top HUD.
   - High-contrast pixel cards for Earth, Mars, Europa, Titan, and Eridani with real astronomical telemetry: Host star spectral class, orbital AU, gravity in $g$, surface pressure in bar, and atmospheric gas breakdown.
   - Seamless one-tap scenario switching with instant climate and terrain generation.

3. **Evo-Devo Gene Lab Workbench**:
   - Full-screen modal with 3 specialized tabs:
     - **🧬 Genome & Codons**: Categorized list (Enzymatic, Structural, Respiration, Metabolism, Sensory, Developmental HOX genes).
     - **🦴 Metameres & Organs**: Interactive cephalic, thoracic, abdominal, and caudal segment builders with limb selection (Cilia, Jointed Walkers, Hydrofoil Fins, Chitinous Wings, Dinosaur Bipedal Legs).
     - **📊 Biomechanics & Laws**: Real-time meters for Skeletal Stress, Aerobic Safety, Reynolds Number, and Daily Metabolic kJ.
   - Direct field test button: Immediately deploys the customized specimen into the live planetary ocean or atmosphere.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Procedural Taxonomy vs. Static Species Catalog**
  - *Chosen Approach*: Hybrid foundational tree + procedural speciation generator. 10 foundational clades representing major evolutionary milestones (microbes, viruses, trilobites, anomalocarids, jawed fish, early tetrapods, giant insects, dinosaurs, birds, mammals) combined with procedural morphological variations (color shifts, scale, limb counts, and procedural binomial naming) whenever species mutate or adapt.
  - *Why*: Gives players recognizable canonical evolutionary stepping stones while providing infinite emergent diversity as the planet changes.
  - *Alternatives Considered*: Pure random procedural generation (often produces nonsensical creatures without biological grounding) or fixed catalog only (limits replayability).

- **Decision 2: Realistic Astrophysics vs. Pure Fantasy Terraforming**
  - *Chosen Approach*: Ground each celestial scenario in real astronomical data (Mars' low gravity and thin CO₂, Europa's subsurface ocean, Titan's cryogenic methane).
  - *Why*: Makes the simulation genuinely educational, rewarding, and scientifically authentic as requested by the user.
  - *Alternatives Considered*: Arbitrary random sliders without celestial context.

- **Decision 3: Non-Overlapping Layout Architecture**
  - *Chosen Approach*: Coordinated single-slot notification controller with strict Z-index layering, mutually exclusive drawers, and container-relative padding.
  - *Why*: Resolves the user's specific complaint that "screens or components are overlapping on each other" and ensures the game viewing area remains uncluttered.

---

## 4. Technical Architecture & Data Strategy

### System Component Architecture

```
┌────────────────────────────────────────────────────────────────────────┐
│                        PhylogenesisViewModel                           │
│  • Manages SimulationState, GameSettings, and Viewport State          │
└───────────────────┬────────────────────────────────┬───────────────────┘
                    │                                │
                    ▼                                ▼
       ┌────────────────────────┐      ┌─────────────────────────┐
       │    SimulationEngine    │      │    EvoDevoEngine        │
       │  • Thermodynamic step  │      │  • Codon transcription  │
       │  • Procedural clade    │      │  • Square-cube scaling  │
       │    speciation & decay  │      │  • Biomechanical stress │
       │  • Organism physics    │      │  • Respiratory Fick law │
       └────────────┬───────────┘      └─────────────┬───────────┘
                    │                                │
                    ▼                                ▼
       ┌─────────────────────────────────────────────────────────┐
       │                  PixelWorldCanvas                       │
       │  • 16-bit discrete tilemap terrain (Oceans, Ice, Lava)  │
       │  • Living organisms layer (heading rotation & sprites)  │
       │  • Weather & Day/Night layers                           │
       │  • Dynamic selection reticle & non-overlapping badges   │
       └─────────────────────────────────────────────────────────┘
```

### Key Data Models to Add & Expand

1. **`CelestialScenario`** (in `PlanetaryModel.kt`):
   ```kotlin
   enum class CelestialScenario(
       val title: String,
       val subtitle: String,
       val hostStar: StarType,
       val gravityG: Float,
       val pressureBar: Float,
       val baseTempC: Float,
       val hydrospherePercent: Float,
       val atmosphere: AtmosphericComposition,
       val description: String,
       val evolutionaryChallenge: String
   ) {
       EARTH_PRIME("Earth", "Pale Blue Dot", StarType.G_DWARF, 1.0f, 1.01f, 15.0f, 71.0f, ...),
       MARS_RED("Mars", "Desiccated Rust Desert", StarType.G_DWARF, 0.38f, 0.006f, -63.0f, 0.0f, ...),
       EUROPA_OCEAN("Europa", "Subsurface Jovian Abyss", StarType.G_DWARF, 0.134f, 0.0f, -160.0f, 100.0f, ...),
       TITAN_METHANE("Titan", "Cryogenic Smog World", StarType.G_DWARF, 0.14f, 1.45f, -179.0f, 15.0f, ...),
       ERIDANI_EYEBALL("Epsilon Eridani b", "Tidally Locked Eyeball", StarType.M_RED, 1.25f, 2.10f, 22.0f, 55.0f, ...)
   }
   ```

2. **Expanded `PhylumCategory` & Procedural Morphotypes**:
   - `VIRUS_CAPSID` ("Capsid Viruses & Bacteriophages", "3.90 Ga", 0xFFA855F7)
   - `PROKARYOTA_CHEMO` ("Hydrothermal Chemolithotrophs", "3.80 Ga", 0xFF64748B)
   - `CYANOBACTERIA_ANALOG` ("Oxygenic Cyanobacteria", "2.70 Ga", 0xFF10B981)
   - `ARTHROPODA_EQUIV` ("Segmented Exopoda", "540 Ma", 0xFFF59E0B)
   - `CHORDATA_EQUIV` ("Stem-Chordates", "525 Ma", 0xFF38BDF8)
   - `PLACODERMI_FISH` ("Armored Jawed Fishes", "420 Ma", 0xFF06B6D4)
   - `TETRAPODA_LAND` ("Limb-Bearing Tetrapods", "375 Ma", 0xFFFB923C)
   - `DINOSAURIA_REPTILE` ("Archosauria & Dinosaurs", "230 Ma", 0xFFEF4444)
   - `AVES_FLIGHT` ("Feathered Avian Lineages", "150 Ma", 0xFFA78BFA)
   - `MAMMALIA_THERIA` ("Endothermic Mammals", "210 Ma", 0xFFF472B6)

3. **Expanded Evo-Devo Codon Palette**:
   - `ENDOTHERMIC_THERMO` ("END-03"): Internal metabolic temperature regulation (uncoupling protein 1 in brown adipose).
   - `PNEUMATIC_BONES` ("AIR-06"): Hollow trabecular bone structure enabling large land mass without skeletal yield failure.
   - `KERATIN_FEATHERS` ("KRT-09"): Aerodynamic lift generation and thermal insulation for high-altitude flight.
   - `NEOCORTEX_HOX` ("BRA-01"): Advanced sensory processing and behavioral plasticity.
   - `VIRAL_CAPSID_INJECT` ("VIR-02"): Horizontal gene transfer vectors bypassing standard generational limits.
   - `AZOTOSOME_LIPID` ("AZO-04"): Methane-soluble cell membranes stable at -180°C for cryogenic planets.

4. **Long-Term Simulation Stability**:
   - Fixed pool of active visual entities (`livingOrganisms` clamped to a healthy 12–20 specimen range per viewport).
   - When clades speciate or go extinct, dead entities are recycled through object pooling rather than infinitely leaking memory.
   - Geological time advancement is safely clamped with epoch wrap-around to prevent floating-point underflow/overflow over hours of play.

---

## 5. Verification & Test Plan

1. **Compilation & Static Checks**:
   - Run `compile_applet` to verify clean compilation across all new models, canvas renderers, and UI components.
2. **Automated JVM Unit Tests (`:app:testDebugUnitTest`)**:
   - Verify all 5 celestial scenarios initialize with correct astrophysics (gravity, pressure, gases, temperature).
   - Test procedural taxonomy speciation across all 10 clades (Microbes to Mammals).
   - Verify biomechanical scaling laws: confirm skeletal yield stress correctly flags impossibly heavy terrestrial creatures under Super-Earth gravity.
   - Verify long-term stability: run 50,000 tick iterations in a headless test to guarantee zero memory leaks or NaN coordinate drifts.
3. **UI Layout & Non-Overlap Verification**:
   - Inspect notification banner and climate ticker to verify mutual exclusion.
   - Verify side drawers cannot open simultaneously and close cleanly.
   - Verify specimen quick card never clips beneath navigation bars or over the bottom dock.
