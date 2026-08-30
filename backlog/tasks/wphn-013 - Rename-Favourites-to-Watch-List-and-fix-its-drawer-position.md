---
id: wphn-013
title: Rename Favourites to Watch List and fix its drawer position
status: Human Review
assignee:
  - '@codex'
created_date: '2026-08-29'
updated_date: '2026-08-30 22:24'
labels:
  - android
  - navigation
  - settings
dependencies:
  - wphn-001
modified_files:
  - >-
    app/src/main/java/com/github/damontecres/wholphin/services/NavDrawerService.kt
  - app/src/main/java/com/github/damontecres/wholphin/ui/nav/NavDrawer.kt
  - >-
    app/src/main/java/com/github/damontecres/wholphin/ui/detail/FavoritesViewModel.kt
  - app/src/main/res/values/strings.xml
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
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
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Drawer shows Watch List directly above Settings.
- [x] #2 Watch List cannot be reordered away from that position.
- [x] #3 Existing Jellyfin favourites appear without migration.
- [x] #4 Visible add/remove/page/empty labels use Watch List terminology.
- [x] #5 The internal API still uses Jellyfin favourite state.
- [x] #6 Watch List remains click-only.
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Remove Favorites from NavDrawerService's reorderable built-in list so stored pin/order data no longer controls it.
2. Render the existing NavDrawerItem.Favorites as a fixed click-only item after all libraries/expanded More content and immediately before Settings, preserving Destination.Favorites selection/navigation.
3. Rename all visible English favorite labels to Watch List terminology, then run focused static checks and :app:compileDefaultDebugKotlin.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Implemented:
- Removed NavDrawerItem.Favorites from NavDrawerService's persisted/reorderable built-in items.
- Rendered the existing Favorites item as fixed Watch List after libraries/expanded More items and directly before Settings, preserving Destination.Favorites and click-only focus behavior.
- Updated visible English favorite labels to Watch List terminology.
- Corrected FavoritesViewModel.TypedProvider.setFavorite to call FavoriteWatchManager.setFavorite so Watch List refreshes after add/remove.

Files:
- app/src/main/java/com/github/damontecres/wholphin/services/NavDrawerService.kt
- app/src/main/java/com/github/damontecres/wholphin/ui/nav/NavDrawer.kt
- app/src/main/java/com/github/damontecres/wholphin/ui/detail/FavoritesViewModel.kt
- app/src/main/res/values/strings.xml

Verification:
- Dependency gate: git merge-base --is-ancestor 0ed62c2b HEAD (exit 0; WPHN-001 merged).
- Static contract check: Favorites excluded from reorder state; fixed item before Settings after all service items; existing navigation handler retained; preview focus cleared; all six English labels updated.
- .\gradlew.bat :app:compileDefaultDebugKotlin --console=plain — BUILD SUCCESSFUL (31 tasks).
- .\gradlew.bat :app:assembleDefaultDebug --console=plain — BUILD SUCCESSFUL after final source edit (73 tasks).
- Installed armeabi-v7a Default debug APK on emulator-5554.
- Manual Series: added 2 Broke Girls, confirmed it appeared under TV Shows, removed it, confirmed it disappeared immediately.
- Manual Movie: added "Wuthering Heights", confirmed it appeared under Movies, removed it, confirmed it disappeared immediately.
- Independent final review: ship, no findings.

Deviations:
- The task's manual refresh check exposed a pre-existing shared-path bug: TypedProvider.setFavorite called setWatched. Fixed surgically in the same Watch List path because acceptance criterion requires add/remove updates.
- No new automated test was added; the changed branch is a one-line API delegation and the task-prescribed device journeys plus compilation/assemble provide direct evidence.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Renamed the visible Favorites experience to Watch List, fixed it directly above Settings outside drawer ordering controls, preserved Jellyfin favorite state and existing navigation, and repaired immediate Watch List refresh after add/remove. Compilation, final APK assembly, independent review, and emulator Movie/Series journeys passed.
<!-- SECTION:FINAL_SUMMARY:END -->

## Verification

```powershell
.\gradlew.bat :app:assembleDefaultDebug
```

Manually favourite/unfavourite one Movie and one Series and confirm the page updates.

## Out of scope

- Separate watch-later semantics.
- Synchronising with Seerr watchlists.
