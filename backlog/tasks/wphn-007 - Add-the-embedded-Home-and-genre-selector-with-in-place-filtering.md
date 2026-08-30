---
id: wphn-007
title: Add the embedded Home and genre selector with in-place filtering
status: Done
assignee:
  - '@codex'
created_date: '2026-08-29'
updated_date: '2026-08-30 12:56'
labels:
  - android
  - library-hub
  - focus
  - rows
dependencies:
  - wphn-006
modified_files:
  - app/src/main/java/com/github/damontecres/wholphin/ui/library/LibraryHub.kt
  - >-
    app/src/main/java/com/github/damontecres/wholphin/ui/components/CollectionFolderView.kt
  - >-
    app/src/main/java/com/github/damontecres/wholphin/ui/components/CollectionFolderGrid.kt
  - >-
    app/src/test/java/com/github/damontecres/wholphin/ui/library/LibraryHubTest.kt
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Replace the current genre-card page transition with a text selector that keeps filtered browsing inside the library hub.

## Goal

Implement the exact Home/genre state machine without making genre focus trigger queries or taking the user to a separate destination.

## Files to inspect

- `ui/components/GenreCardGrid.kt`
- `data/model/BaseItem.kt` genre destination/filter builder
- `ui/components/CollectionFolderView.kt`
- `ui/library/LibraryHub*` from wphn-006
- current focus-restorer/bring-into-view helpers

## Implementation

1. Add a horizontal `GenreSelectorRow`:
   - first item `Home`;
   - remaining items from the same Jellyfin genre query currently used by `GenreCardGrid`;
   - text only;
   - subtle selected/focused pill/background;
   - proper ellipsis and TV-sized focus target.
2. Reset selected genre to Home whenever a new library hub key is created. Do not share active genre between hubs.
3. Left/right changes focus only. Do not use a focus dwell for genres.
4. On OK for a genre:
   - keep focus on the pill;
   - cancel any previous filter load;
   - construct the same library/genre/include-type filter used by the current filtered collection route;
   - replace normal hub rows with an embedded `CollectionFolderView`/provider grid.
5. Modify the shared collection-folder content at the real focus-event point to expose an optional focused-item callback. Keep the callback default no-op so existing callers do not change behaviour.
6. Use the first non-null filtered item as spotlight fallback before a grid card receives focus.
7. Down enters the first result; Up from the first grid row returns to the selected pill.
8. On OK for Home:
   - cancel genre load;
   - restore configured/fallback rows;
   - clear old filtered focus/spotlight;
   - keep focus on Home.
9. Install a Back handler active only in Genre mode. First Back performs the same Home reset and returns selector focus; it must not pop the hub.
10. Ensure drawer D-pad right still uses the first-media focus requester and never chooses the selector.
11. If the filtered result is empty or errors, retain selector focus and show the existing clear empty/error state below it.
12. Do not mutate the full library browser’s saved filter/display settings. Use the existing genres-specific display-info ID convention or an equivalent isolated key.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Home is initially selected for every newly previewed hub.
- [x] #2 Focus alone never changes genre or sends a query.
- [x] #3 OK activates a genre in place and keeps selector focus.
- [x] #4 Spotlight uses first filtered result, then follows grid focus.
- [x] #5 Down enters grid; Up returns selected pill.
- [x] #6 First Back in Genre mode restores Home and is consumed.
- [x] #7 Switching libraries resets to Home.
- [x] #8 Full browser filter state is unaffected.
- [x] #9 Existing collection-folder grid implementation is reused.
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Extend LibraryHubViewModel with hub-keyed Home/genre state, reuse the existing genre query and createGenreDestination filter contract, and expose deterministic selector actions. 2. Render a text-only selector above existing rows and embed CollectionFolderView for active genres while retaining the existing first-media drawer requester and isolated genres display-info ID. 3. Add one defaulted focused-item/focus-boundary hook at the shared collection grid card-focus path so existing callers remain unchanged. 4. Add the smallest focused state/filter regression check required by WPHN-007, run that check plus :app:compileDefaultDebugKotlin, inspect/review the diff, then record evidence and move to In Review.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Implemented WPHN-007 only.

Files:
- app/src/main/java/com/github/damontecres/wholphin/ui/library/LibraryHub.kt
- app/src/main/java/com/github/damontecres/wholphin/ui/components/CollectionFolderView.kt
- app/src/main/java/com/github/damontecres/wholphin/ui/components/CollectionFolderGrid.kt
- app/src/test/java/com/github/damontecres/wholphin/ui/library/LibraryHubTest.kt

Implementation:
- Added a Home-first text genre selector to the existing LibraryHub.
- Reused the current Jellyfin genre query, createGenreDestination filter contract, CollectionFolderView, grid, display-info persistence path, and existing TabRow focus targets.
- Genre selection is hub-keyed, activates only on OK, swaps rows for a keyed/cancellable embedded filtered view, keeps selector focus, and drives spotlight from the first result then focused grid cards.
- Down/Up bridge selector and first grid row; Back restores Home without popping; drawer Right still enters first Home media.
- CollectionFolderView gained defaulted focused-item/first-row-up hooks and now honors LocalContentTakeFocus for loading placeholders. Existing callers retain defaults.

Verification:
- PASS: rtk .\gradlew.bat :app:testDefaultDebugUnitTest --tests com.github.damontecres.wholphin.ui.library.LibraryHubTest (BUILD SUCCESSFUL).
- PASS: rtk .\gradlew.bat :app:compileDefaultDebugKotlin (BUILD SUCCESSFUL; existing deprecation/KSP warnings only).
- PASS: rtk .\gradlew.bat :app:assembleDefaultDebug (BUILD SUCCESSFUL; existing deprecation/KSP warnings only).
- PASS: rtk git diff --check.
- PASS: independent review, no P0-P2 findings.
- PASS on Google TV Streamer using the final 13:29:19 APK: drawer preview Right entered first media; selector L/R did not query; Action and Adventure activated in place with pill focus retained; spotlights/results updated; Back restored Home pill and rows; switching Movies -> Shows -> Movies reset each newly previewed hub to Home. Evidence under build/wphn-007-tvqa/latest-action.png, latest-adventure.png, latest-back.png, cross-shows-home.png, and cross-movies-home.png.
- Empty/error rendering reuses existing CollectionFolderView states and focus fallback. Error injection was not performed against the live server.
- Full LibraryBrowse state was not mutated during QA; isolation is enforced by the genres-specific viewModelKey/display-info ID and the existing full-browser key was left unchanged.

Deviations:
- The task says In Review; this repository's configured equivalent status is Human Review.
- No unrelated pre-existing projectmem working-tree files were staged.
<!-- SECTION:NOTES:END -->

## Comments

<!-- COMMENTS:BEGIN -->
author: @codex
created: 2026-08-30 12:56
---
Accepted by the user; approved for push.
---
<!-- COMMENTS:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Added the embedded Home/genre selector and in-place filtered CollectionFolderView to LibraryHub, preserving hub-local state, selector/grid focus, spotlight updates, Back behavior, drawer entry, and full-browser persistence isolation. Focused unit test, Kotlin compilation, debug APK assembly, diff check, independent review, and final Google TV Streamer journeys passed.
<!-- SECTION:FINAL_SUMMARY:END -->

## Verification

```powershell
.\gradlew.bat :app:assembleDefaultDebug
```

Run all Genres scenarios from `architecture/verification-matrix.md`.

## Out of scope

- Genre focus activation.
- Genre artwork.
- A View All genre screen.
