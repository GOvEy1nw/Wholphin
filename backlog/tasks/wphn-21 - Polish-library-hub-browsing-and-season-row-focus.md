---
id: WPHN-21
title: Polish library hub browsing and season-row focus
status: Human Review
assignee:
  - '@codex'
created_date: '2026-08-31 14:43'
updated_date: '2026-08-31 15:35'
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
1. Attach no-animation Navigation 3 metadata only to drawer-wrapped destinations; preserve full-screen transitions.
2. Keep video-library drawer activation on LibraryHub, and model Home, All, and Genre as explicit hub states.
3. Reuse createGenreDestination for genre queries, restrict TV hub results to SERIES, and give embedded All/Genre views captionless non-persisted poster options.
4. Make useSavedLibraryDisplayInfo=false bypass saved display-info reads so existing preferences cannot override embedded hub defaults.
5. Preserve the vertical scroll position across the episode-to-season focus handoff after focus relocation.
6. Verify with focused state/view-model tests, default-debug compilation and assembly, physical Google TV interaction, diff inspection, and fresh review.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Architecture evidence: NavDisplay currently transitions entire NavDrawer-wrapped entries, explaining the screenshot overlap/dimming. Existing createGenreDestination already is the canonical Library > Genres query contract; WPHN-21 will reuse it rather than invent another genre endpoint. CollectionType.TVSHOWS.baseItemKinds includes SERIES, SEASON, and EPISODE, so the hub needs a series-only override.

Implemented the requested drawer, hub, genre, card, TV-series, and season-focus behavior across the existing navigation and collection-view paths. A first physical-TV check exposed that retaining the episode spotlight alone did not prevent the season-row scroll; the final fix restores the captured scroll position on the frame after season focus succeeds.

Fresh review found an existing-user edge case: saved LibraryDisplayInfo view options could re-enable captions despite the hub opt-out. CollectionFolderViewModel now skips saved display-info lookup when useSavedLibraryDisplayInfo is false, covered by a focused stored-preference regression test. Second fresh review verdict: ship.

Verification: `rtk .\gradlew.bat :app:testDefaultDebugUnitTest --tests com.github.damontecres.wholphin.ui.library.LibraryHubTest --tests com.github.damontecres.wholphin.ui.components.CollectionFolderViewModelTest` -> BUILD SUCCESSFUL in 52s; `rtk .\gradlew.bat :app:compileDefaultDebugKotlin --max-workers=1` -> BUILD SUCCESSFUL in 20s; final `rtk .\gradlew.bat :app:assembleDefaultDebug --max-workers=1` -> BUILD SUCCESSFUL in 29s; `git diff --check` -> exit 0 with line-ending warnings only.

Physical Google TV Streamer validation used package `com.github.damontecres.wholphin.debug` and the matching `armeabi-v7a` APK. Screenshots confirmed Home/All/Action ordering, drawer activation remaining on the hub, captionless series cards in Shows All and Action, spotlight updates, and identical vertical row coordinates before/after DPAD_UP from episodes to seasons. UIAutomator could not reach idle, so adb screencap PNG evidence was used.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
## Summary
- Removed cross-fade transitions from drawer-wrapped destinations so selected navigation items remain visually stable.
- Kept library activation on the hub and added explicit Home, All, and canonical Genre modes.
- Made Shows All/Genre queries series-only and embedded grids captionless without reading or mutating saved display preferences.
- Preserved series-detail vertical scroll when focus moves from episodes to the season selector.

## Verification
- Focused LibraryHub and CollectionFolderViewModel unit tests passed.
- Default-debug Kotlin compilation and APK assembly passed.
- Physical Google TV Streamer journey passed for drawer activation, All/genre presentation, series-only results, spotlight behavior, and season-row focus stability.
- Final diff check passed and fresh reviewer verdict was ship.

## Notes
- UIAutomator could not idle on the device; adb screencap comparisons supplied the interaction evidence instead.
<!-- SECTION:FINAL_SUMMARY:END -->
