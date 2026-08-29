---
id: APP-013
title: Rename Favourites to Watch List and fix its drawer position
status: To Do
assignee: []
created_date: '2026-08-29'
labels:
  - android
  - navigation
  - settings
dependencies: 
  - APP-001
priority: medium
---

## Description

Rename the visible favourites concept without changing its Jellyfin data semantics, and render the fixed Watch List item immediately above Settings.

## Goal

Create a family-friendly Watch List while avoiding a second watch-later store or migration.

## Files to inspect

- `services/NavDrawerService.kt`
- `ui/nav/NavDrawer.kt`
- `ui/detail/FavoritesPage.kt`
- all `R.string.*favorite*` usages in visible actions/pages
- pin/customise-drawer UI and persistence

## Implementation

1. Keep internal Jellyfin favourite flags, API calls, and existing `Destination.Favorites` unless an internal rename clearly reduces code without increasing merge cost.
2. Change all user-visible English labels in this private fork to:
   - Watch List
   - Add to Watch List
   - Remove from Watch List
   - No Watch List items, or equivalent natural empty state.
3. Stop mixing the built-in favourite item into the ordinary pinned/library order.
4. Render the fixed Watch List item after libraries/expanded More content and directly before Settings.
5. Keep it click-only; it does not participate in 300 ms drawer previews.
6. Remove/hide Watch List from drawer pin/reorder controls if those controls would imply it can move away from its fixed position.
7. Existing favourited Movies, Series, people, music, and other supported types must appear automatically on the existing page.
8. Do not create a Room table, playlist, collection, tag, or separate server state.

## Acceptance criteria

- [ ] Drawer shows Watch List directly above Settings.
- [ ] Watch List cannot be reordered away from that position.
- [ ] Existing Jellyfin favourites appear without migration.
- [ ] Visible add/remove/page/empty labels use Watch List terminology.
- [ ] The internal API still uses Jellyfin favourite state.
- [ ] Watch List remains click-only.

## Verification

```powershell
.\gradlew.bat :app:assembleDefaultDebug
```

Manually favourite/unfavourite one Movie and one Series and confirm the page updates.

## Out of scope

- Separate watch-later semantics.
- Synchronising with Seerr watchlists.
