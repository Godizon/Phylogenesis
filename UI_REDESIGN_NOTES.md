# Phylogenesis UI redesign notes

Goal: players should never have to Google anything, and the game should feel like a toy (WorldBox-style).

## New files (app/src/main/java/com/example/ui/components)
- Glossary.kt: plain-language names/explanations for every science term (epochs, scales, disasters, stats).
  Add new "what is this?" entries here.
- InfoDialog.kt: InfoDialog (tap-to-learn popup) and StatChip (tappable stat tile).
- PowersDock.kt: bottom power bar (Create Life, Tree of Life, Time Travel, 4 disasters, Planet),
  CoachHint (first-run pulsing hint) and TimeTravelDialog.

## Rewritten
- TopHudBar.kt: slim 2-row HUD. Naturalist/Architect toggle removed (it did nothing).
- TutorialSpotlightOverlay.kt + model/TutorialModel.kt: card walkthrough with bouncing arrows, no hard-coded pixel positions.

## Edited
- PhylogenesisScreen.kt: new layout, disaster screen-shake/flash/result card, labeled drawer buttons, fixed overlapping right-side controls.
- SimulationViewport.kt, ClimateAlertTicker.kt, CladeDrawer.kt, PlanetaryDrawer.kt, SpecimenInspector.kt,
  GeneEditorModal.kt, CladogramModal.kt, GodToolPanel.kt, WorldSeedModal.kt, SettingsDialog.kt, StartMenuScreen.kt,
  model/CataclysmType.kt: jargon replaced with plain language; tiny fonts bumped.
- GameSettings.kt: autoPauseOnCataclysm now defaults to false so disasters play out live.

## Unused now (safe to delete)
- TimelineBar.kt, ActiveToolIndicator.kt

NOTE: written without access to an Android toolchain. Run `./gradlew assembleDebug` and fix any compile errors first.
