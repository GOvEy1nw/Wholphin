---
id: APP-006
title: Build the reusable video library hub
status: To Do
assignee: []
created_date: '2026-08-29'
labels:
  - android
  - library-hub
  - focus
  - rows
dependencies: 
  - APP-002
  - APP-005
priority: high
---

## Description

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
4. Keep Genres in the configuration list, then partition it from media rows for the fixed selector slot. `APP-007` supplies the selector; use a non-interactive placeholder or hidden slot until then.
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

## Acceptance criteria

- [ ] Fallback rows match the approved order for general and TV libraries.
- [ ] TV fallback contains Next Up and no Studios.
- [ ] Spotlight tracks focused item and otherwise the first available active item.
- [ ] Spotlight never retains media from a previous library.
- [ ] Drawer-to-hub entry targets first media, not selector/header.
- [ ] Empty rows do not trap focus.
- [ ] Existing row cards and context actions are reused.
- [ ] No parallel row renderer/fetch service is introduced.

## Verification

```powershell
.\gradlew.bat :app:assembleDefaultDebug
```

Manually verify fallback row order, first-media focus, spotlight fallback, empty rows, and Back from a temporary browse route.

## Out of scope

- Final genre selector/filtering.
- Managed layouts.
- Visual branding/polish.
