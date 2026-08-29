---
id: wphn-006
title: Build the reusable video library hub
status: Done
assignee:
  - '@codex'
created_date: '2026-08-29'
updated_date: '2026-08-30 11:12'
labels:
  - android
  - library-hub
  - focus
  - rows
dependencies:
  - wphn-002
  - wphn-005
modified_files:
  - app/src/main/java/com/github/damontecres/wholphin/ui/library/LibraryHub.kt
  - app/src/main/java/com/github/damontecres/wholphin/ui/main/HomePage.kt
  - >-
    app/src/main/java/com/github/damontecres/wholphin/ui/nav/DestinationContent.kt
  - app/src/main/java/com/github/damontecres/wholphin/ui/nav/NavDrawer.kt
  - >-
    app/src/main/java/com/github/damontecres/wholphin/ui/nav/NavigationDrawerAndroid.kt
  - >-
    app/src/test/java/com/github/damontecres/wholphin/ui/library/LibraryHubTest.kt
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Create the hub screen and ViewModel by reusing `HomePageContent`, `HomePageHeader`, `HomeSettingsService`, existing context actions, and current row-card rendering.

## Goal

Replace the conceptual Recommended tab with one proper library landing page whose spotlight always describes the focused or first available media item.

## Files to inspect

- `ui/main/HomePage.kt`
- `ui/main/HomeViewModel.kt`
- `ui/components/RecommendedContent.kt`
- `services/HomeSettingsService.kt`
- `ui/detail/CollectionFolderMovie.kt`
- `ui/detail/CollectionFolderTv.kt`
- current context-menu/action helpers

## Implementation

1. Add `LibraryHub` and a ViewModel keyed by server, user, and library ID.
2. Resolve the library metadata from `NavDrawerService`’s current library list or the existing item endpoint. Do not add it to the navigation key solely to avoid one existing lookup.
3. Add one fallback builder returning existing `HomeRowConfig` values.

   General video fallback:

   ```text
   Genres
   ContinueWatching(parentId)
   RecentlyReleased(parentId)
   RecentlyAdded(parentId)
   TopUnwatched(parentId)
   Suggestions(parentId)
   Collections(parentId)
   ```

   TV fallback inserts `NextUp(parentId)` after Continue Watching.

4. Keep Genres in the configuration list, then partition it from media rows for the fixed selector slot. `wphn-007` supplies the selector; use a non-interactive placeholder or hidden slot until then.
5. Fetch rows concurrently using the same bounded-concurrency pattern as Home. Do not create a second request-handler hierarchy.
6. Store:

   ```text
   active content rows
   first available media item
   currently focused media item
   spotlight item = focused ?: first available
   current row/card position
   loading/refresh state
   ```

7. Render the header through the existing `HomePageHeader`/logo preference path.
8. Render media rows through `HomePageContent`. Add only the minimal optional focus hooks required to:
   - expose a first-media-row `FocusRequester`;
   - bypass the genre row when entering from the drawer;
   - report focused media to the hub.
9. On entry from the drawer, request the first item of the first non-empty media row. Preserve a retained hub state when returning from its full browser, but a newly previewed library destination starts at first media.
10. Reuse existing click, play, watched, favourite/Watch List, playlist, and allowed context actions.
11. Treat empty/403/404 rows as unavailable and skip them in initial focus.
12. Do not add a standalone Spotlight query or configuration row.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Fallback rows match the approved order for general and TV libraries.
- [x] #2 TV fallback contains Next Up and no Studios.
- [x] #3 Spotlight tracks focused item and otherwise the first available active item.
- [x] #4 Spotlight never retains media from a previous library.
- [x] #5 Drawer-to-hub entry targets first media, not selector/header.
- [x] #6 Empty rows do not trap focus.
- [x] #7 Existing row cards and context actions are reused.
- [x] #8 No parallel row renderer/fetch service is introduced.
- [x] #9 Library drawer preview dwell is 150 ms.
- [x] #10 A loading library hub does not move focus to the profile/header controls.
- [x] #11 Opening the sidebar from a library hub selects that current library instead of Home.
- [x] #12 Library dwell preview keeps focus on the selected sidebar item until the user presses D-pad Right.
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Extend the existing library hub and shared navigation paths only, preserving established services, models, composables, and navigation state.
2. Implement the approved reusable hub rows, spotlight behavior, browse destinations, empty-row omission, loading/error handling, and drawer-to-hub focus handoff.
3. Apply manual-review corrections through the shared drawer path: use a 150 ms preview dwell, keep focus on the selected library through preview/loading, restore that library when reopening the drawer, and transfer focus to hub content only on D-pad Right.
4. Run the task's focused LibraryHub test plus touched-module Kotlin compilation/assembly, review the exact diff, and validate the focus flows on the target TV.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Implemented a reusable, destination-scoped LibraryHubViewModel and LibraryHub screen. The hub reuses HomeSettingsService row loading, HomePageContent/HomePageHeader rendering, existing card/context-menu actions, existing navigation/playback paths, and the Nav3 destination key scope.

Added the specified general-video and TV fallback row order. TV includes Next Up and omits Studios. The Genres config is partitioned into the hidden WPHN-007 slot and is not fetched or rendered.

Extended HomePageContent with defaulted focus hooks so the first non-empty row receives drawer-entry focus, retained row positions are restored only when still valid, and empty rows fall back without trapping focus. Spotlight follows the focused item and otherwise uses the first available item.

Verification: `.\gradlew.bat :app:testDefaultDebugUnitTest --tests com.github.damontecres.wholphin.ui.library.LibraryHubTest` — BUILD SUCCESSFUL. The command also ran `:app:compileDefaultDebugKotlin` successfully. Existing KSP/deprecation warnings were unchanged.

Independent implementation review after the focus correction returned `VERDICT: ship` with no findings.

Deviation / remaining evidence: no live Android TV focus run was performed; drawer-entry and retained-focus interaction remain release-stage manual validation. No broader suite was run because the task requested only its focused check plus touched-module compilation.

Manual review on Google TV Streamer requested three WPHN-006 corrections: reduce library dwell from 300 ms to 150 ms; prevent profile/header focus while a hub is loading; restore the current library selection when reopening the drawer from its hub.

Final physical retest is waiting on ABI direction: `adb install` of the arm64-v8a APK failed with `INSTALL_FAILED_NO_MATCHING_ABIS`. The target reports `ro.product.cpu.abilist=armeabi-v7a,armeabi`, an empty `abilist64`, and the currently installed package uses `primaryCpuAbi=armeabi-v7a`. The focused test/compilation pass and independent review reports ship for the latest race correction.

Manual-review corrections completed. `NavDrawer.kt` now uses a 150 ms library preview delay, synchronizes the selected library index against preview/click destinations, preserves the selected drawer item's focus across destination replacement, and enables content focus only for D-pad Right. `NavigationDrawerAndroid.kt` extends the existing drawer implementation with a defaulted `closeOnFocusLost` switch so previews can retain drawer focus without changing other callers. `LibraryHub.kt` reuses `LoadingPage` with the existing `LocalContentTakeFocus` contract so loading does not steal focus during drawer previews.

Verification: `./gradlew.bat :app:testDefaultDebugUnitTest --tests com.github.damontecres.wholphin.ui.library.LibraryHubTest` passed (`BUILD SUCCESSFUL`, including `:app:compileDefaultDebugKotlin`). `./gradlew.bat :app:assembleDefaultDebug` passed. Existing KSP/deprecation warnings were recorded and not changed. `git diff --check` passed apart from repository CRLF warnings.

Physical validation on 192.168.1.84 passed with the armeabi-v7a debug APK (matching the official installed Wholphin ABI and the device's 32-bit Android userspace). After hovering Movies for more than four seconds, the loaded hub remained visible while focus stayed on the Movies drawer item; D-pad Right closed the drawer and focused the first media card; D-pad Left reopened the drawer with Movies focused instead of Home. The official app reports primaryCpuAbi=armeabi-v7a; an arm64-v8a APK was correctly rejected by Android with INSTALL_FAILED_NO_MATCHING_ABIS. The user approved the compatible armeabi-v7a install.

Independent review of the final diff returned `VERDICT: ship` with no findings. No broader suite was run because the task requires the focused test plus touched-module compilation.
<!-- SECTION:NOTES:END -->

## Comments

<!-- COMMENTS:BEGIN -->
created: 2026-08-30 11:12
---
Human acceptance recorded on 2026-08-30: user approved the implementation and requested publication.
---
<!-- COMMENTS:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Implemented WPHN-006 as one focused reusable-library-hub change. The existing hub/navigation stack now provides the approved row ordering, spotlight behavior, browse destinations, empty-row omission, and shared loading/error paths. Manual TV review refinements reduce library preview dwell to 150 ms, keep focus on the selected sidebar library while its hub previews and loads, transfer focus into hub content only on D-pad Right, and restore the current library when the drawer is reopened. Focused LibraryHub tests, touched-module Kotlin compilation, debug assembly, independent diff review, and physical validation on 192.168.1.84 passed. The compatible armeabi-v7a debug build was installed because both the official Wholphin app and the target Android userspace are 32-bit; arm64-v8a is unsupported by this device image.
<!-- SECTION:FINAL_SUMMARY:END -->

## Verification

```powershell
.\gradlew.bat :app:assembleDefaultDebug
```

Manually verify fallback row order, first-media focus, spotlight fallback, empty rows, and Back from a temporary browse route.

## Out of scope

- Final genre selector/filtering.
- Managed layouts.
- Visual branding/polish.
