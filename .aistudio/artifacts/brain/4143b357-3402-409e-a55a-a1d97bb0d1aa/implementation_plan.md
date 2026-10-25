# UI & Layout Overhaul: Immersive Sandbox & Clutter Reduction

## User Review & Critical Decisions

> [!IMPORTANT]
> This plan addresses clutter, overlapping UI overlays, continuous moving element controls, comprehensive tooltips, and viewport sizing as requested in feedback.

- **Confirmed Decision 1**: Full-screen immersive canvas with retractable HUD and floating toolbars.
- **Confirmed Decision 2**: Pause/speed toggles, layer visibility switches, and slow/pause controls for weather clouds and day-night shadows.
- **Confirmed Decision 3**: Tap-to-inspect popovers with full descriptions and working system back handlers.

---

### 1. Overview & Core Concept
- **What It Does**: Reorganizes the entire HUD and overlay hierarchy in `PhylogenesisScreen` and viewport widgets to maximize game viewing area, eliminate overlapping elements, provide clear descriptive tooltips for every control, and add intuitive pause/speed/layer toggles for animated weather and diurnal cycles.
- **Key Improvements**:
  1. **Expanded Viewport Area**: Move top HUD and bottom toolbars into collapsible/retractable header and footer sheets or slim pill bars, giving the central pixel world canvas 85%+ of the screen.
  2. **Clutter Elimination**: Prevent overlapping badges and tickers by combining climate alerts, active tool indicators, and mini-map into a clean, unified bottom-right floating dock and top-left status pill.
  3. **Weather & Motion Controls**: Add dedicated controls in the settings/telemetry to pause, slow down, or toggle layer visibility for drifting pixel clouds and day-night shadow bands.
  4. **Tap-to-Inspect Tooltips**: Implement interactive tooltips and inspect popovers on all tools, biomes, clades, and celestial widgets explaining what they do in plain English.
  5. **Robust Back Handlers**: Ensure `BackHandler` correctly closes open drawers, modals, and toolbars before exiting screens.

### 2. User Experience & Visual Design
- **Layout Structure**:
  - **Top Bar**: Minimalist retro-futuristic header with pause/play, epoch indicator, and hamburger menu toggle (reducing vertical height).
  - **Main Canvas**: Expanded immersive full-screen viewport with zero wasted padding.
  - **Bottom Dock**: Compact toolbar for zoom levels and primary God tools with clear iconography and text labels.
  - **Inspect Popovers**: Clean floating cards above tapped items with detailed tooltips and stats.

### 3. Technical Architecture & Data Strategy
```
┌────────────────────────────────────────────────────────┐
│ Top Bar (Compact Header: Epoch, Play/Pause, Menu)      │
├────────────────────────────────────────────────────────┤
│                                                        │
│                  Simulation Viewport                   │
│         (Maximized Canvas, Cloud & Day/Night Toggles)  │
│                                                        │
│   [Top-Left Pill]                 [Top-Right Tooltip]  │
│   [Bottom-Left MiniMap]           [Bottom-Right Tools] │
│                                                        │
├────────────────────────────────────────────────────────┤
│ Bottom Bar (Compact Zoom & Epoch Selector / Tray)      │
└────────────────────────────────────────────────────────┘
```
- **State Management**: Update `PhylogenesisViewModel` and UI state to support layer visibility (`showClouds`, `showDayNight`, `showMiniMap`) and tooltips.
