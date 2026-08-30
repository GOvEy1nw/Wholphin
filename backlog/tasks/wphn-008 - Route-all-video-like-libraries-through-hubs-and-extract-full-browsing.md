---
id: wphn-008
title: Route all video-like libraries through hubs and extract full browsing
status: Done
assignee:
  - '@codex'
created_date: '2026-08-29'
updated_date: '2026-08-30 13:43'
labels:
  - android
  - navigation
  - library-hub
  - rows
dependencies:
  - wphn-004
  - wphn-006
  - wphn-007
modified_files:
  - >-
    app/src/main/java/com/github/damontecres/wholphin/ui/library/LibraryBrowse.kt
  - >-
    app/src/main/java/com/github/damontecres/wholphin/ui/nav/DestinationContent.kt
  - >-
    backlog/tasks/wphn-008 -
    Route-all-video-like-libraries-through-hubs-and-extract-full-browsing.md
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Complete the library redesign by routing supported video views to the new hub, extracting the former Library-tab configuration into a dedicated full browser, and removing Recommended/Library/Collections/Genres/Studios tabs from the hub path.

## Goal

Make the new navigation the only route for supported video libraries while preserving a powerful full browser on OK.

## Files to inspect

- `ui/nav/DestinationContent.kt`
- `ui/detail/CollectionFolderMovie.kt`
- `ui/detail/CollectionFolderTv.kt`
- generic collection-folder dispatch and browser composables
- current movie/TV sort, filter, view options and play flags
- every destination that opens a library/user view

## Implementation

1. Add `LibraryBrowse` composable that uses the exact former Library-tab configuration:
   - Movies: movie filter, movie sort, recursive, poster defaults, play enabled.
   - TV: series filter, TV filter options, series sort, recursive, poster defaults.
   - Home/mixed video: use the current generic video browser configuration and correct include types.
2. Route `Destination.LibraryHub` to the new hub and `Destination.LibraryBrowse` to this browser.
3. Route all supported drawer video libraries through these destinations.
4. Keep direct navigation to actual Movie, Series, Episode, Box Set, folder, playlist and other media details unchanged.
5. Stop presenting `TabbedPage` for the Movie/TV library landing flow. The old files may become thin compatibility delegates or be removed only if no callers remain.
6. Do not show Studios anywhere in the new TV hub.
7. Collections are a hub row for Movie and TV. Ensure the wphn-005 RC6 collection acceptance scenario is exercised from the real hub.
8. Preserve specialised Music, Photos, Live TV, recordings, playlists, Box Sets, and unsupported collection views.
9. Confirm library OK always opens browse even while the previewed hub is in Genre mode; the browser opens with its own normal saved filter state.
10. Confirm Back from browse returns to the matching hub rather than Home.
11. Remove only imports/functions made unused by this change. Do not delete reusable Genre/Studio components used elsewhere.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 All approved video-like drawer libraries preview a hub.
- [x] #2 OK opens a full sortable/filterable browser.
- [x] #3 Back returns to the matching hub.
- [x] #4 No Recommended/Library/Collections/Genres/Studios tab bar remains in hub flow.
- [x] #5 TV Collections work from the real hub and TV Studios are absent.
- [x] #6 Music, Photos, Live TV and other specialised flows are unchanged.
- [x] #7 Genre-mode state does not leak into full browser settings.
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Add ui/library/LibraryBrowse.kt as the single dedicated full-browser composable, reusing CollectionFolderView and the former Movie/TV Library-tab configurations while using the existing generic video configuration for other approved video-like libraries. 2. Route Destination.LibraryBrowse through the new composable and leave MediaItem specialised/direct detail dispatch unchanged; retain the existing LibraryHub and NavDrawerService classification/navigation paths. 3. Verify the complete focused diff, compile :app:compileDefaultDebugKotlin, assemble :app:assembleDefaultDebug, and manually exercise the task's listed library flows without repairing unrelated failures.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Implemented WPHN-008 only.

Files:
- app/src/main/java/com/github/damontecres/wholphin/ui/library/LibraryBrowse.kt
- app/src/main/java/com/github/damontecres/wholphin/ui/nav/DestinationContent.kt

Implementation:
- Added the dedicated LibraryBrowse composable and routed Destination.LibraryBrowse to it.
- Preserved the former Movie Library configuration and saved-state key, plus the former TV Library configuration and TV filter/sort contract.
- Reused CollectionFolderGeneric for other approved video-like libraries; Home Videos and Music Videos use precise video include types, while FOLDERS/UNKNOWN remain type-unfiltered so mixed folder libraries keep their current browser behavior.
- Left LibraryHub, drawer classification, navigation stack, direct media details, and specialised Music/Photos/Live TV/recording/playlist/Box Set dispatch unchanged.

Verification:
- PASS: .\gradlew.bat :app:compileDefaultDebugKotlin --console=plain (BUILD SUCCESSFUL in 53s; existing KSP/deprecation warnings only).
- PASS: .\gradlew.bat :app:assembleDefaultDebug --console=plain (BUILD SUCCESSFUL in 52s; existing warnings only).
- PASS: scoped git diff --check (normal LF-to-CRLF warning only).
- PASS: independent review verdict ship, no findings.
- PASS on Google TV Streamer 192.168.1.84 using the final armeabi-v7a debug APK: Movies and TV previewed hubs; OK opened no-tab sortable/filterable browsers; Back restored the matching hub; Movies Action genre state stayed isolated from the 3315-item browser; Docs opened the type-unfiltered three-folder browser; the TV Collections row opened Marvel Action collection details; TV hub showed no Studios.
- Manual limitation: this debug account exposed no Home Videos, Music, Photos, or Live TV libraries, so those four requested journeys could not be exercised on this fixture. Their existing specialised dispatch remains unchanged and compiled.

Deviations:
- The task requests In Review; this repository's configured equivalent is Human Review.
- No unrelated pre-existing projectmem files were staged or repaired.
<!-- SECTION:NOTES:END -->

## Comments

<!-- COMMENTS:BEGIN -->
author: @codex
created: 2026-08-30 13:43
---
User approved the implementation and requested publication on 2026-08-30.
---
<!-- COMMENTS:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Added a dedicated LibraryBrowse route that preserves the former Movie and TV Library-tab browser contracts, keeps mixed generic libraries type-unfiltered, and leaves specialised media flows unchanged. Focused Kotlin compilation and APK assembly passed; Movies, TV, Docs, Collections, Back navigation, no-tab flow, and genre-state isolation were verified on the Google TV Streamer. Home Videos, Music, Photos, and Live TV were unavailable on the connected account, so those source-preserved flows remain a fixture-limited manual evidence gap.
<!-- SECTION:FINAL_SUMMARY:END -->

## Verification

```powershell
.\gradlew.bat :app:assembleDefaultDebug
```

Manually exercise at least Movies, TV, Documentaries/general video, Home Videos, Music, Photos, and Live TV.

## Out of scope

- Managed plugin layouts.
- Final spacing/branding pass.
