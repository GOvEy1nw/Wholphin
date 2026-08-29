---
id: wphn-005
title: Extend the shared row engine for library hubs
status: Ready
assignee: []
created_date: "2026-08-29"
labels:
  - android
  - rows
  - library-hub
  - tests
dependencies:
  - wphn-001
priority: high
---

## Description

Add only the row vocabulary and request scoping that the approved fallback and plugin-managed library hubs require. Keep all data fetching in `HomeSettingsService` and existing request services.

## Goal

Allow Home and library hubs to share one row engine without custom per-hub queries.

## Files to inspect

- `data/model/HomeRowConfig.kt`
- `services/HomeSettingsService.kt`
- `services/LatestNextUpService.kt`
- `ui/components/RecommendedMovie.kt`
- `ui/components/RecommendedTvShow.kt`
- current request handlers and `HomeItemFields`

## Implementation

1. Add an optional `parentId` to:
   - `ContinueWatching`
   - `NextUp`
   - `ContinueWatchingCombined`

   Existing serialized rows omit/null it and retain global Home behaviour.

2. Thread optional `parentId` through `LatestNextUpService.getResume()` and `getNextUp()` to the underlying Jellyfin requests. Update every caller once at the shared function boundary.
3. Add serializable row types:

   ```kotlin
   HomeRowConfig.TopUnwatched(parentId, viewOptions)
   HomeRowConfig.Collections(parentId, viewOptions)
   ```

4. Implement `TopUnwatched` with the same media type, recursive scope, rating sort, user data, and useful fields currently used by Recommended Movie/TV. Infer the item type from the known library record; do not duplicate a second `kind` field unless the real request cannot be resolved safely.
5. Implement `Collections` by first reusing the same Jellyfin 12 query shape as the existing Movie Collections tab:
   - parent library ID;
   - recursive;
   - include `BOX_SET`;
   - user-visible results;
   - existing card fields.
6. Add a focused integration/manual fixture against Jellyfin 12.0.0-rc6 proving that the scoped query:
   - returns a TV collection containing a series from the current TV library;
   - keeps a mixed collection when it has a qualifying series;
   - excludes a collection unrelated to that library.
7. Only if the RC6 query fails that acceptance test, add the smallest membership-filter fallback. Do not pre-emptively add an N+1 collection scanner.
8. Add resolution titles and View More handling through the existing `HomeRowLoadingState.Success` path.
9. Keep `Studios` model support for old/local settings, but do not add it to any new fallback or managed profile UI.
10. Keep local `HomePageSettings.version` unchanged. The managed envelope has its own version boundary.

## Acceptance criteria

- [ ] Existing global Home rows behave exactly as before when `parentId` is null.
- [ ] Hub watching rows are scoped to their library.
- [ ] Top Unwatched uses the current library and correct media kind.
- [ ] Movie and TV Collections return only qualifying collections on Jellyfin 12.0.0-rc6.
- [ ] Mixed TV collections remain when qualifying.
- [ ] All row types use the existing fetch/resolve pipeline.
- [ ] No second row service or arbitrary query abstraction is added.

## Verification

```powershell
.\gradlew.bat :app:testDefaultDebugUnitTest
.\gradlew.bat :app:assembleDefaultDebug
```

Run the RC6 collection query check documented in the task notes.

## Out of scope

- Hub screen.
- Plugin contract wrapper.
- Smart collection generation.
