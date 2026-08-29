---
id: APP-007
title: Add the embedded Home and genre selector with in-place filtering
status: To Do
assignee: []
created_date: '2026-08-29'
labels:
  - android
  - library-hub
  - focus
  - rows
dependencies: 
  - APP-006
priority: high
---

## Description

Replace the current genre-card page transition with a text selector that keeps filtered browsing inside the library hub.

## Goal

Implement the exact Home/genre state machine without making genre focus trigger queries or taking the user to a separate destination.

## Files to inspect

- `ui/components/GenreCardGrid.kt`
- `data/model/BaseItem.kt` genre destination/filter builder
- `ui/components/CollectionFolderView.kt`
- `ui/library/LibraryHub*` from APP-006
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

## Acceptance criteria

- [ ] Home is initially selected for every newly previewed hub.
- [ ] Focus alone never changes genre or sends a query.
- [ ] OK activates a genre in place and keeps selector focus.
- [ ] Spotlight uses first filtered result, then follows grid focus.
- [ ] Down enters grid; Up returns selected pill.
- [ ] First Back in Genre mode restores Home and is consumed.
- [ ] Switching libraries resets to Home.
- [ ] Full browser filter state is unaffected.
- [ ] Existing collection-folder grid implementation is reused.

## Verification

```powershell
.\gradlew.bat :app:assembleDefaultDebug
```

Run all Genres scenarios from `architecture/verification-matrix.md`.

## Out of scope

- Genre focus activation.
- Genre artwork.
- A View All genre screen.
