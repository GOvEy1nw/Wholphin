---
id: WPHN-21
title: Polish library hub browsing and season-row focus
status: Done
assignee:
  - '@codex'
created_date: '2026-08-31 14:43'
updated_date: '2026-09-01 12:46'
labels:
  - android
  - library-hub
  - series
  - focus
dependencies:
  - WPHN-007
  - WPHN-008
  - WPHN-010
references:
  - 'C:/Users/rais/Desktop/Screenshot_1788186159.png'
  - 'C:/Users/rais/Desktop/Screenshot_1788253109.png'
  - 'C:/Users/rais/Desktop/Screenshot_1788252975.png'
documentation:
  - docs/rais-stream/plan/README.md
modified_files:
  - >-
    app/src/main/java/com/github/damontecres/wholphin/services/NavDrawerService.kt
  - >-
    app/src/main/java/com/github/damontecres/wholphin/ui/components/CollectionFolderView.kt
  - >-
    app/src/main/java/com/github/damontecres/wholphin/ui/detail/series/SeriesOverviewContent.kt
  - app/src/main/java/com/github/damontecres/wholphin/ui/library/LibraryHub.kt
  - >-
    app/src/main/java/com/github/damontecres/wholphin/ui/nav/ApplicationContent.kt
  - >-
    app/src/test/java/com/github/damontecres/wholphin/ui/components/CollectionFolderViewModelTest.kt
  - >-
    app/src/test/java/com/github/damontecres/wholphin/ui/library/LibraryHubTest.kt
priority: high
type: bug
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Correct the Rais Stream library hub and series-detail regressions reported during review: remove drawer-item transition dimming, expose full-library browsing as an in-hub All option, use Jellyfin's genre-specific browsing semantics, simplify media-card presentation, preserve series-level TV results, and stop season-row focus from shifting the page.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Switching focus between navigation drawer items does not visibly flash or dim the newly selected item during the content transition.
- [x] #2 The library hub selector orders Home, All, then genres; activating All displays the full selected library, and activating the library drawer item no longer serves as the All-items shortcut.
- [x] #3 Activating a genre uses the same Jellyfin genre-specific view/query semantics as Library > Genres > that genre, rather than applying a genre filter to the All-items view.
- [x] #4 All and genre grids omit titles and secondary text beneath posters while continuing to drive the spotlight from focused media.
- [x] #5 TV Shows All and genre grids contain series, not episodes.
- [x] #6 Moving focus up to the season selector does not scroll the series detail page downward.
- [x] #7 Focused automated checks and default-debug Kotlin compilation pass, with representative TV interaction evidence recorded when the local device/emulator is available.
- [x] #8 When focus changes the library-hub spotlight item, its backdrop artwork updates to the same item across Home, All, and genre modes.
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [x] #1 Acceptance criteria are satisfied
- [x] #2 Relevant automated tests pass
- [x] #3 Lint, type-check, and build checks pass where applicable
- [x] #4 Documentation is updated where required
- [x] #5 Implementation summary and verification evidence are recorded
- [x] #6 No unrelated changes are included
<!-- DOD:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Preserve the existing Home / All / genre selector, stable navbar focus, captionless browse cards, series-only TV queries, season-row scroll behavior, and saved-display isolation.
2. Present selected genres with the official genre filter controls and generated genre/library heading while keeping the embedded hub navigation model.
3. Give LibraryHub sole ownership of embedded spotlight artwork: guard callbacks by monotonic activation and mode, serialize cancellable backdrop mutations, keep keyed loaders alive across hidden tabs, and disable the collection child writer only for hub embedding.
4. Verify focused library/collection tests and default-debug assembly, install the matching armeabi-v7a APK, exercise normal and rapid Home/All/genre transitions, inspect the complete diff, and obtain a fresh ship review.
5. Move WPHN-21 to Human Review and commit only the two source files plus Backlog task metadata; do not push.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Architecture evidence: NavDisplay currently transitions entire NavDrawer-wrapped entries, explaining the screenshot overlap/dimming. Existing createGenreDestination already is the canonical Library > Genres query contract; WPHN-21 will reuse it rather than invent another genre endpoint. CollectionType.TVSHOWS.baseItemKinds includes SERIES, SEASON, and EPISODE, so the hub needs a series-only override.

Implemented the requested drawer, hub, genre, card, TV-series, and season-focus behavior across the existing navigation and collection-view paths. A first physical-TV check exposed that retaining the episode spotlight alone did not prevent the season-row scroll; the final fix restores the captured scroll position on the frame after season focus succeeds.

Fresh review found an existing-user edge case: saved LibraryDisplayInfo view options could re-enable captions despite the hub opt-out. CollectionFolderViewModel now skips saved display-info lookup when useSavedLibraryDisplayInfo is false, covered by a focused stored-preference regression test. Second fresh review verdict: ship.

Verification: `rtk .\gradlew.bat :app:testDefaultDebugUnitTest --tests com.github.damontecres.wholphin.ui.library.LibraryHubTest --tests com.github.damontecres.wholphin.ui.components.CollectionFolderViewModelTest` -> BUILD SUCCESSFUL in 52s; `rtk .\gradlew.bat :app:compileDefaultDebugKotlin --max-workers=1` -> BUILD SUCCESSFUL in 20s; final `rtk .\gradlew.bat :app:assembleDefaultDebug --max-workers=1` -> BUILD SUCCESSFUL in 29s; `git diff --check` -> exit 0 with line-ending warnings only.

Physical Google TV Streamer validation used package `com.github.damontecres.wholphin.debug` and the matching `armeabi-v7a` APK. Screenshots confirmed Home/All/Action ordering, drawer activation remaining on the hub, captionless series cards in Shows All and Action, spotlight updates, and identical vertical row coordinates before/after DPAD_UP from episodes to seasons. UIAutomator could not reach idle, so adb screencap PNG evidence was used.

Human review feedback (2026-09-01): fork screenshot shows Action as a counted filter on the embedded All grid, while official Wholphin v1.0.7 opens a dedicated titled `Action Movies` genre collection with no genre filter badge. Reopened WPHN-21 and AC #3 for correction; previous captionless All/Genre requirement remains in force unless explicitly superseded.

Additional review feedback (2026-09-01): spotlight metadata changes correctly while the artwork backdrop remains stale when switching hubs/genres or focused media. Added AC #8 and kept WPHN-21 In Progress for a shared backdrop-ownership correction.

Corrective implementation: genre mode now passes `DefaultForGenresFilterOptions` and exposes the preserved `genre + library` title while All stays titleless. LibraryHub owns embedded backdrop updates with an activation generation, active-mode guard, cancellable mutex-serialized writes, and explicit clearing on mode activation. Keyed collection loads remain alive across hidden tabs so rapid same-tab returns can reuse completed work. `CollectionFolderView.manageBackdrop` defaults to true for all existing callers and is false only in LibraryHub, preventing the Show Backdrop option from creating a second writer.

Final verification: `./gradlew.bat :app:testDefaultDebugUnitTest --tests com.github.damontecres.wholphin.ui.library.LibraryHubTest --tests com.github.damontecres.wholphin.ui.components.CollectionFolderViewModelTest :app:assembleDefaultDebug --max-workers=1` -> BUILD SUCCESSFUL in 1m 7s. `git diff --check` -> exit 0 with line-ending warnings only. Installed the generated `armeabi-v7a` default-debug APK on `emulator-5554` (device supports x86/armeabi-v7a; installed package primary ABI is armeabi-v7a). Sequential Action and Adventure checks showed matching spotlight/artwork; rapid Home-All-Action-Adventure ended on Adventure Movies with matching The 5th Wave metadata/art; rapid All-Action-All before load completion recovered the All grid with matching Wuthering Heights metadata/art. User explicitly confirmed the genre presentation works. Final fresh reviewer verdict: ship, no findings.
<!-- SECTION:NOTES:END -->

## Comments

<!-- COMMENTS:BEGIN -->
created: 2026-09-01 12:46
---
User accepted the completed implementation and explicitly requested publication on 2026-09-01.
---
<!-- COMMENTS:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
## Summary
- Made embedded genre tabs match official Wholphin collection presentation: titled genre/library heading and no genre-as-user-filter badge, while retaining captionless posters and series-only TV queries.
- Synchronized spotlight metadata and artwork through one LibraryHub-owned backdrop path.
- Protected rapid switching with per-activation callback identity, serialized cancellable backdrop updates, retained keyed loading, and suppression of the embedded collection view's optional secondary backdrop writer.
- Preserved all earlier WPHN-21 drawer-transition, All-tab, saved-display, and season-row focus corrections.

## Verification
- Focused LibraryHub and CollectionFolderViewModel tests passed.
- Default-debug APK assembly passed and `git diff --check` reported no errors.
- Latest armeabi-v7a debug APK installed successfully on emulator-5554.
- Normal and rapid genre journeys produced matching spotlight text/artwork; rapid return to an incompletely loaded All view recovered correctly.
- Fresh release review verdict: ship, no findings.

## Notes
- No broad suite was run because the two focused tests, assembly, device journeys, and independent review cover the changed collection/hub contracts.
- UI startup was occasionally slow during reinstall; validation sequences were rerun after the activity finished rendering.
<!-- SECTION:FINAL_SUMMARY:END -->
