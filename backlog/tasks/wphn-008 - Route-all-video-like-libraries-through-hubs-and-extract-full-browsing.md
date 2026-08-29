---
id: wphn-008
title: Route all video-like libraries through hubs and extract full browsing
status: Ready
assignee: []
created_date: "2026-08-29"
labels:
  - android
  - navigation
  - library-hub
  - rows
dependencies:
  - wphn-004
  - wphn-006
  - wphn-007
priority: high
---

## Description

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

## Acceptance criteria

- [ ] All approved video-like drawer libraries preview a hub.
- [ ] OK opens a full sortable/filterable browser.
- [ ] Back returns to the matching hub.
- [ ] No Recommended/Library/Collections/Genres/Studios tab bar remains in hub flow.
- [ ] TV Collections work from the real hub and TV Studios are absent.
- [ ] Music, Photos, Live TV and other specialised flows are unchanged.
- [ ] Genre-mode state does not leak into full browser settings.

## Verification

```powershell
.\gradlew.bat :app:assembleDefaultDebug
```

Manually exercise at least Movies, TV, Documentaries/general video, Home Videos, Music, Photos, and Live TV.

## Out of scope

- Managed plugin layouts.
- Final spacing/branding pass.
