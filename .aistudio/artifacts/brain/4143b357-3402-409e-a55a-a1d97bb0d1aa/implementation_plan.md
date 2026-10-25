# Phylogenesis: Exoplanetary Systems, vDNA Gene Editor & Dynamic Weather

Expand *Phylogenesis* with exotic exoplanetary scenarios (Tidally Locked Eyeball Worlds and 2.5g Super-Earths), an interactive vDNA Gene Editor with live morphology preview, and a dynamic 16-bit pixel weather and day/night illumination engine.

---

## 1. Scope & Core Features

### 1.1 Exotic Planetary Systems
1. **Tidally Locked "Eyeball World"**:
   - **Host Star:** M-Type Red Dwarf (Proxima-analog) with tight orbital resonance.
   - **Thermodynamic Grid:** 1:1 spin-orbit tidal lock.
     - *Substellar Point:* Perpetual noon, searing temperatures, boiling equatorial sea, and intense hyper-convective storms.
     - *Antistellar Hemisphere:* Perpetual midnight, kilometers-thick glacial ice sheet, frozen CO₂/nitrogen deposits.
     - *Twilight Ribbon (Terminator Zone):* The temperate boundary ring between day and night where liquid water persists, generating a unique ribbon biosphere with perpetual sunset lighting.
2. **Super-Earth System ($2.5g$)**:
   - **Gravitational & Atmospheric Physics:** $2.5\times$ surface gravity, $3.2\text{ bar}$ atmospheric density.
   - **Evo-Devo Biomechanics:** Square-cube law enforces heavy penalties on tall or fragile organisms. Organisms must evolve thick exoskeletons or dense bone mineralization, low-profile flattened bodies, and multi-jointed wide stances. High atmospheric pressure boosts oxygen diffusion efficiency but makes propulsion resistive.

### 1.2 Interactive vDNA Gene Editor & Live Morphology Laboratory
* **Dedicated Evo-Devo Studio:**
  * Openable from the specimen inspector or navigation dock.
  * **Hox Segment Regulation:** Adjust Cephalic, Thorax, and Abdomen segment counts (1 to 8), limb pairs, and specialized appendages (antennae, claws, paddles).
  * **Metabolic & Structural Codons:**
    - Mineralization: Chitin, Hydroxyapatite, Silica, or Flexible Collagen.
    - Respiration: Hemocyanin (copper, cold-water efficient) vs Hemoglobin (iron, high-capacity) vs Tracheal direct diffusion.
    - Energetics: Rubisco (autotroph), Catalase (radiation resistance), Glycolytic burst (predation sprint).
  * **Live 16-Bit Morphology Preview:** Real-time procedural pixel sprite rendering reflecting codon changes, calculating mass ($M \propto L^3$), skeletal yield strength, Reynolds number ($Re$), and daily metabolic consumption ($W$).
  * **Clone / Inject Button:** Release the modified organism back into the biosphere to observe whether it outcompetes native clades.

### 1.3 Pixel Weather Patterns & Day-Night Illumination Engine
* **Dynamic 16-Bit Cloud & Storm Simulation:**
  * Procedural wind vectors driving drifting pixel cloud clusters across the world canvas.
  * Precipitation zones: Rainstorms fill lakes and nourish coastal algal blooms; arid wind shadows create desertification.
  * Eyeball world weather: Massive cyclonic storm anchored over the substellar point pumping vapor towards the cold side.
* **Day-Night Lighting Terminator:**
  * Sweeping orbital shadow line simulating planetary rotation on standard worlds.
  * Fixed radial lighting gradient on Eyeball worlds (blazing solar zenith transitioning to amber twilight and deep blue-black night).

---

## 2. Technical Architecture & File Layout

```
/app/src/main/java/com/example/
├── model/
│   ├── PlanetaryModel.kt           # Add EyeballWorld and SuperEarth planetary configs
│   ├── WeatherModel.kt             # Cloud cells, wind vectors, precipitation, terminator line
│   └── EvoDevoModel.kt             # Extended Hox regulatory codons and morphological metrics
├── engine/
│   ├── SimulationEngine.kt         # Eyeball thermodynamics, Super-Earth gravity scaling, weather step
│   └── WeatherEngine.kt            # Procedural wind circulation, storm cells, and rain deposition
├── ui/
│   ├── components/
│   │   ├── GeneEditorModal.kt      # Interactive vDNA codon builder & live sprite preview
│   │   └── WorldSeedModal.kt       # Exoplanet scenario selector (Standard, Eyeball, Super-Earth)
│   └── viewport/
│       ├── PixelWorldCanvas.kt     # Integrate pixel weather layer, cloud sprites, and day-night lighting
│       └── SpecimenCanvas.kt       # Support live morphology rendering for custom gene edits
```

---

## 3. Implementation Steps

1. **Planetary Physics & Scenarios (`PlanetaryModel.kt`, `SimulationEngine.kt`)**:
   - Add `PlanetaryArchetype` (Standard Terrestrial, Tidally Locked Eyeball, Super-Earth 2.5g).
   - Implement latitude/longitude temperature differentials for Eyeball worlds (substellar heat hotspot to nightside cold sink).
   - Implement gravitational scale factor ($1.0g$ vs $2.5g$) directly modifying organism mass load and skeletal stress in `BiomechanicalMetrics`.

2. **Dynamic Weather & Day/Night System (`WeatherModel.kt`, `WeatherEngine.kt`, `PixelWorldCanvas.kt`)**:
   - Model low-overhead grid of atmospheric pressure and moisture vectors.
   - Render drifting pixelated cloud clusters and rain droplets with foam/ripple interaction on water tiles.
   - Render day-night terminator shadow mask with ambient lighting shifts.

3. **vDNA Gene Editor Modal (`GeneEditorModal.kt`)**:
   - Design retro 16-bit laboratory UI with codon sliders, segment spinners, and biomaterial selectors.
   - Render real-time pixel specimen preview updating immediately as genes are adjusted.
   - Add bio-energetic validation warnings (e.g., "Skeletal structural collapse under $2.5g$", "Metabolic demand exceeds photosynthetic area").
   - Add "Inject Into Biosphere" demiruge action.

4. **Start Menu & Seed Generator Integration (`WorldSeedModal.kt`, `StartMenuScreen.kt`)**:
   - Add preset cards for "Tidally Locked Eyeball" and "Heavy Gravity Super-Earth" in the world generator.

5. **Verification & Testing**:
   - Add unit tests for Eyeball world temperature distribution and Super-Earth gravitational scaling.
   - Verify 60 FPS Compose rendering performance with weather particles.
   - Compile and test across unit and Robolectric suites.
